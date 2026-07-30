package dev.valerisclient.core.modules.prime;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.event.WorldJoinEvent;
import dev.valerisclient.core.event.WorldLeaveEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.keybind.Keybind;
import dev.valerisclient.core.keybind.KeybindManager;
import dev.valerisclient.core.keybind.KeyNames;
import dev.valerisclient.core.notification.NotificationManager;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;
import dev.valerisclient.core.voice.VoiceChatService;
import dev.valerisclient.core.voice.VoiceChatSettings;
import dev.valerisclient.core.voice.VoiceListenMode;
import dev.valerisclient.core.voice.VoiceParticipant;

/**
 * Prime Voice — proximity chat (48 blocks) + voice groups for ValerisClient players.
 *
 * <p>Push-to-talk key is configurable in Prime Keybinds (default B, not V).</p>
 */
public final class VoiceChatModule extends Module {

    /** Default PTT — B avoids conflict with Simple Voice Chat (often V). */
    private static final int DEFAULT_TALK_KEY = 66;

    private final StringSetting relayUrl =
            addSetting(new StringSetting("relay-url", "Relay URL", "Valeris Voice relay WebSocket",
                    VoiceChatSettings.DEFAULT_RELAY));
    private final BooleanSetting pushToTalk =
            addSetting(new BooleanSetting("push-to-talk", "Push to talk", "Hold V to transmit", true));
    private final IntSetting inputVolume =
            addSetting(new IntSetting("input-volume", "Mic volume", "Microphone gain %", 100, 0, 200));
    private final IntSetting outputVolume =
            addSetting(new IntSetting("output-volume", "Voice volume", "Other players volume %", 100, 0, 200));
    private final IntSetting proximityBlocks =
            addSetting(new IntSetting("proximity", "Proximity range", "Hear nearby Valeris players (blocks)",
                    VoiceChatSettings.DEFAULT_PROXIMITY, 8, 128));
    private final BooleanSetting proximityEnabled =
            addSetting(new BooleanSetting("proximity-enabled", "Proximity chat", "Hear Valeris players within range",
                    true));
    private final EnumSetting<VoiceListenMode> listenMode =
            addSetting(new EnumSetting<>("listen-mode", "Listen mode",
                    "Proximity, group, or both", VoiceListenMode.BOTH));
    private final StringSetting groupCode =
            addSetting(new StringSetting("group-code", "Group code", "Join a voice group (share this code with friends)",
                    ""));
    private final StringSetting newGroupName =
            addSetting(new StringSetting("new-group-name", "New group name", "Name when creating a voice group",
                    "Team"));
    private final BooleanSetting createGroup =
            addSetting(new BooleanSetting("create-group", "Create group", "Toggle ON to create a new voice group",
                    false));
    private final BooleanSetting leaveGroup =
            addSetting(new BooleanSetting("leave-group", "Leave group", "Toggle ON to leave current group", false));
    private final BooleanSetting showHud =
            addSetting(new BooleanSetting("show-hud", "Voice HUD", "Show Valeris Voice overlay", true));

    private final VoiceChatService voice;
    private final MinecraftAdapter adapter;
    private final Keybind talkKey;
    private final OverlayElement overlay;

    private String lastAppliedGroupCode = null;

    public VoiceChatModule(VoiceChatService voice, HudManager hud, ThemeManager themes,
                           MinecraftAdapter adapter, NotificationManager notifications,
                           KeybindManager keybinds) {
        super("prime-voice", "Valeris Voice",
                "Proximity + group voice chat (PTT key in Keybinds menu)", ModuleCategory.PRIME);
        this.voice = voice;
        this.adapter = adapter;
        voice.bindNotifications(notifications);
        this.talkKey = keybinds.register(new Keybind(
                "prime-voice-talk", "Valeris Voice Talk", "Valeris", DEFAULT_TALK_KEY));
        this.overlay = hud.register(new OverlayElement(themes));

        listen(ClientTickEvent.class, event -> onTick());
        listen(WorldJoinEvent.class, event -> onWorldJoin());
        listen(WorldLeaveEvent.class, event -> onWorldLeave());
    }

    @Override
    protected void onEnable() {
        if (groupCode.get().isBlank() && voice.settings().inGroup()) {
            groupCode.set(voice.settings().activeGroupId());
            lastAppliedGroupCode = voice.settings().activeGroupId();
        }
        syncSettings();
        applyGroupActions();
        overlay.setVisible(showHud.get());
        if (adapter.isMultiplayer()) {
            voice.start(adapter);
        }
    }

    @Override
    protected void onDisable() {
        voice.stop();
        overlay.setVisible(false);
    }

    private void onWorldJoin() {
        if (!isEnabled()) {
            return;
        }
        syncSettings();
        applyGroupActions();
        voice.start(adapter);
    }

    private void onWorldLeave() {
        voice.stop();
    }

    private void onTick() {
        syncSettings();
        applyGroupActions();
        overlay.setVisible(showHud.get() && isEnabled());
        if (!isEnabled()) {
            return;
        }
        voice.tick(adapter, talkKey.isPressed());
        overlay.refresh();
    }

    private void applyGroupActions() {
        if (leaveGroup.get()) {
            leaveGroup.set(false);
            voice.leaveGroup();
            groupCode.set("");
            lastAppliedGroupCode = "";
            return;
        }
        if (createGroup.get()) {
            createGroup.set(false);
            String code = voice.createGroup(newGroupName.get());
            groupCode.set(code);
            lastAppliedGroupCode = code;
            return;
        }
        String code = groupCode.get() == null ? "" : groupCode.get().trim();
        if (!code.equals(lastAppliedGroupCode)) {
            lastAppliedGroupCode = code;
            if (code.isBlank()) {
                if (voice.settings().inGroup()) {
                    voice.leaveGroup();
                }
            } else {
                voice.joinGroup(code);
            }
        }
    }

    private void syncSettings() {
        var settings = voice.settings();
        settings.setRelayUrl(relayUrl.get());
        settings.setPushToTalk(pushToTalk.get());
        settings.setInputVolume(inputVolume.get());
        settings.setOutputVolume(outputVolume.get());
        settings.setProximityBlocks(proximityBlocks.get());
        settings.setProximityEnabled(proximityEnabled.get());
        settings.setListenMode(listenMode.get());
        settings.setShowHud(showHud.get());
        if (!groupCode.get().isBlank()) {
            settings.setActiveGroupId(groupCode.get().trim());
        }
    }

    private final class OverlayElement extends HudElement {

        private final ThemeManager themes;
        private String line1 = "";
        private String line2 = "";
        private String line3 = "";

        OverlayElement(ThemeManager themes) {
            super("prime-voice", "Valeris Voice", HudAnchor.TOP_LEFT, 4, 68);
            this.themes = themes;
        }

        void refresh() {
            VoiceChatService.State state = voice.state();
            line1 = switch (state) {
                case CONNECTED -> "Voice · " + voice.nearbyCount() + " nearby · "
                        + voice.participantCount() + " Valeris";
                case CONNECTING -> "Voice · Connecting…";
                case ERROR -> "Voice · " + voice.statusMessage();
                default -> "Voice · Off";
            };
            if (voice.settings().inGroup()) {
                String name = voice.settings().activeGroupName();
                String label = name.isBlank() ? voice.settings().activeGroupId() : name;
                line2 = "Group: " + label + " (" + voice.groupMemberCount() + ")";
            } else if (voice.settings().proximityEnabled()) {
                line2 = "Proximity: " + voice.settings().proximityBlocks() + " blocks";
            } else {
                line2 = "No group";
            }
            if (voice.muted()) {
                line3 = "Muted";
            } else if (voice.deafened()) {
                line3 = "Deafened";
            } else if (voice.settings().pushToTalk()) {
                line3 = talkKey.isPressed()
                        ? "Transmitting (" + KeyNames.glfwName(talkKey.key()) + ")"
                        : "Hold " + KeyNames.glfwName(talkKey.key()) + " to talk";
            } else {
                line3 = "Open mic";
            }
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return Math.max(Math.max(ctx.textWidth(line1), ctx.textWidth(line2)), ctx.textWidth(line3)) + 8;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() * 3 + 14;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            int w = measureWidth(ctx);
            int h = measureHeight(ctx);
            ctx.fillRect(0, 0, w, h, theme.background());
            ctx.drawText(line1, 4, 4, theme.foreground(), true);
            ctx.drawText(line2, 4, 4 + ctx.fontHeight() + 2, theme.accent(), true);
            int color = theme.accent();
            for (VoiceParticipant participant : voice.participants()) {
                if (participant.speaking()) {
                    color = 0xFF22C55E;
                    break;
                }
            }
            ctx.drawText(line3, 4, 4 + (ctx.fontHeight() + 2) * 2, color, true);
        }
    }

}
