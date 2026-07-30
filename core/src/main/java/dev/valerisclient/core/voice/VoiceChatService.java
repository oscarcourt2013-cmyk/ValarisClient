package dev.valerisclient.core.voice;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prime Voice — proximity chat (48 blocks) + voice groups between ValerisClient users.
 *
 * <p>Like Simple Voice Chat: hear nearby Prime players, or join a group to talk at any distance.</p>
 */
public final class VoiceChatService {

    public enum State {
        OFF, CONNECTING, CONNECTED, ERROR
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("Valeris Voice");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoiceChatSettings settings = new VoiceChatSettings();
    private final VoiceRelayClient relay = new VoiceRelayClient();
    private final VoiceAudioCapture capture = new VoiceAudioCapture();
    private final VoiceAudioPlayback playback = new VoiceAudioPlayback();

    private final Map<String, VoiceParticipant> participants = new ConcurrentHashMap<>();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean transmitting = new AtomicBoolean(false);
    private final AtomicBoolean muted = new AtomicBoolean(false);
    private final AtomicBoolean deafened = new AtomicBoolean(false);

    private NotificationManager notifications;
    private MinecraftAdapter adapter;
    private State state = State.OFF;
    private String statusMessage = "";
    private int posTickCounter;

    public VoiceChatSettings settings() {
        return settings;
    }

    public State state() {
        return state;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public Collection<VoiceParticipant> participants() {
        return participants.values();
    }

    public int participantCount() {
        return participants.size();
    }

    /** Prime players within proximity range right now. */
    public int nearbyCount() {
        if (adapter == null) {
            return 0;
        }
        int count = 0;
        for (VoiceParticipant participant : participants.values()) {
            if (proximityGain(participant) > 0f) {
                count++;
            }
        }
        return count;
    }

    /** Group members online (excluding self). */
    public int groupMemberCount() {
        if (!settings.inGroup()) {
            return 0;
        }
        int count = 0;
        for (VoiceParticipant participant : participants.values()) {
            if (participant.inGroup(settings.activeGroupId())) {
                count++;
            }
        }
        return count;
    }

    public boolean muted() {
        return muted.get();
    }

    public boolean deafened() {
        return deafened.get();
    }

    public void bindNotifications(NotificationManager notifications) {
        this.notifications = notifications;
    }

    public void setMuted(boolean value) {
        muted.set(value);
    }

    public void setDeafened(boolean value) {
        deafened.set(value);
        if (value) {
            muted.set(true);
        }
    }

    public void setTransmitting(boolean value) {
        transmitting.set(value);
    }

    /** Creates a new group and joins it. Returns the shareable group code. */
    public String createGroup(String displayName) {
        String name = displayName == null || displayName.isBlank() ? "Group" : displayName.trim();
        String code = randomGroupCode();
        settings.setActiveGroupId(code);
        settings.setActiveGroupName(name);
        relay.sendGroup(code);
        notify("Voice Group", "Created \"" + name + "\" — code: " + code);
        return code;
    }

    /** Join an existing group by code. */
    public void joinGroup(String code) {
        if (code == null || code.isBlank()) {
            leaveGroup();
            return;
        }
        settings.setActiveGroupId(code.trim());
        settings.setActiveGroupName("");
        relay.sendGroup(code.trim());
        notify("Voice Group", "Joined group " + code.trim());
    }

    /** Leave the current voice group (proximity still works). */
    public void leaveGroup() {
        settings.setActiveGroupId("");
        settings.setActiveGroupName("");
        relay.sendGroup("");
        notify("Voice Group", "Left voice group");
    }

    public void start(MinecraftAdapter adapter) {
        this.adapter = adapter;
        if (!adapter.isMultiplayer()) {
            state = State.OFF;
            statusMessage = "Singleplayer";
            return;
        }
        String playerId = adapter.playerUuid();
        if (playerId.isBlank()) {
            state = State.ERROR;
            statusMessage = "No player UUID";
            return;
        }
        active.set(true);
        state = State.CONNECTING;
        statusMessage = "Connecting…";
        String room = roomId(adapter.serverAddress());
        String group = settings.activeGroupId();
        relay.connect(settings.relayUrl(), room, playerId, adapter.playerName(), group,
                new VoiceRelayClient.Listener() {
                    @Override
                    public void onConnected() {
                        state = State.CONNECTED;
                        statusMessage = "Connected";
                        playback.start();
                        capture.start(pcm -> onCapturedFrame(pcm));
                        if (settings.inGroup()) {
                            relay.sendGroup(settings.activeGroupId());
                        }
                        LOGGER.info("Valeris Voice connected to room {}", room);
                    }

                    @Override
                    public void onDisconnected(String reason) {
                        if (active.get()) {
                            state = State.ERROR;
                            statusMessage = "Disconnected";
                        } else {
                            state = State.OFF;
                            statusMessage = "";
                        }
                    }

                    @Override
                    public void onParticipants(Map<String, VoiceParticipant> list) {
                        participants.clear();
                        participants.putAll(list);
                    }

                    @Override
                    public void onAudio(String senderId, byte[] pcm) {
                        if (deafened.get()) {
                            return;
                        }
                        VoiceParticipant participant = participants.get(senderId);
                        float gain = hearGain(participant);
                        if (gain <= 0.01f) {
                            return;
                        }
                        playback.play(senderId, pcm, volumeGain(settings.outputVolume()) * gain);
                        if (participant != null) {
                            participant.setSpeaking(true);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        state = State.ERROR;
                        statusMessage = message;
                    }
                });
    }

    public void stop() {
        active.set(false);
        capture.close();
        playback.close();
        relay.disconnect("stop");
        participants.clear();
        state = State.OFF;
        statusMessage = "";
    }

    public void tick(MinecraftAdapter adapter, boolean talkKeyDown) {
        if (!active.get() || state != State.CONNECTED) {
            return;
        }
        this.adapter = adapter;
        boolean shouldTalk = !muted.get() && (!settings.pushToTalk() || talkKeyDown);
        transmitting.set(shouldTalk);

        posTickCounter++;
        if (posTickCounter >= 10) {
            posTickCounter = 0;
            relay.sendPosition(adapter.playerX(), adapter.playerY(), adapter.playerZ());
            syncParticipantPositions(adapter);
        }

        for (VoiceParticipant participant : participants.values()) {
            if (participant.speaking()) {
                participant.setSpeaking(false);
            }
        }
    }

    public void shutdown() {
        stop();
    }

    private void onCapturedFrame(byte[] pcm) {
        if (!transmitting.get() || muted.get()) {
            return;
        }
        float gain = volumeGain(settings.inputVolume());
        if (gain <= 0.01f) {
            return;
        }
        relay.sendAudio(scale(pcm, gain));
    }

    private void syncParticipantPositions(MinecraftAdapter adapter) {
        for (VoiceParticipant participant : participants.values()) {
            double x = adapter.playerXForUuid(participant.id());
            double y = adapter.playerYForUuid(participant.id());
            double z = adapter.playerZForUuid(participant.id());
            if (Double.isFinite(x)) {
                participant.setPosition(x, y, z);
            }
        }
    }

    /** Combined gain from group (full) or proximity (distance falloff). */
    private float hearGain(VoiceParticipant participant) {
        if (participant == null || !canHear(participant)) {
            return 0f;
        }
        float groupGain = sameGroup(participant) ? 1f : 0f;
        float proxGain = settings.proximityEnabled() ? proximityGain(participant) : 0f;
        return Math.max(groupGain, proxGain);
    }

    private boolean canHear(VoiceParticipant participant) {
        boolean inGroup = sameGroup(participant);
        boolean inRange = settings.proximityEnabled() && proximityGain(participant) > 0f;
        return switch (settings.listenMode()) {
            case GROUP -> inGroup;
            case PROXIMITY -> inRange;
            case BOTH -> inGroup || inRange;
        };
    }

    private boolean sameGroup(VoiceParticipant participant) {
        return settings.inGroup() && participant.inGroup(settings.activeGroupId());
    }

    /** Linear falloff 0–range blocks; 0 beyond range (Simple Voice Chat style). */
    private float proximityGain(VoiceParticipant participant) {
        if (participant == null || adapter == null) {
            return 0f;
        }
        double distance = adapter.distanceToPlayer(participant.id());
        if (!Double.isFinite(distance)) {
            return 0f;
        }
        double range = settings.proximityBlocks();
        if (distance >= range) {
            return 0f;
        }
        return (float) (1.0 - (distance / range));
    }

    private static float volumeGain(int percent) {
        return Math.clamp(percent / 100f, 0f, 2f);
    }

    private static byte[] scale(byte[] pcm, float gain) {
        byte[] out = pcm.clone();
        for (int i = 0; i + 1 < out.length; i += 2) {
            short sample = (short) ((out[i + 1] << 8) | (out[i] & 0xff));
            sample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) (sample * gain)));
            out[i] = (byte) (sample & 0xff);
            out[i + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return out;
    }

    private void notify(String title, String message) {
        if (notifications != null) {
            notifications.info(title, message);
        }
    }

    private static String randomGroupCode() {
        char[] chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(chars[RANDOM.nextInt(chars.length)]);
        }
        return code.toString();
    }

    static String roomId(String serverAddress) {
        String normalized = serverAddress == null ? "unknown" : serverAddress.trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (Exception e) {
            return normalized.replace(':', '_');
        }
    }
}
