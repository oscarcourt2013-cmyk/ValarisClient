package dev.valerisclient.core.voice;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** WebSocket client for Prime Voice relay rooms (ValerisClient users only). */
final class VoiceRelayClient implements WebSocket.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger("Valeris Voice");
    private static final Gson GSON = new Gson();
    private static final int UUID_BYTES = 36;

    interface Listener {
        void onConnected();

        void onDisconnected(String reason);

        void onParticipants(Map<String, VoiceParticipant> participants);

        void onAudio(String senderId, byte[] pcm);

        void onError(String message);
    }

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private WebSocket socket;
    private Listener listener;
    private String selfId = "";
    private final StringBuilder textBuffer = new StringBuilder();
    private final Map<String, VoiceParticipant> participants = new ConcurrentHashMap<>();

    Map<String, VoiceParticipant> participants() {
        return participants;
    }

    void connect(String relayUrl, String roomId, String playerId, String playerName,
                 String groupId, Listener listener) {
        disconnect("reconnect");
        this.listener = listener;
        this.selfId = playerId;
        participants.clear();

        URI uri = URI.create(relayUrl);
        http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .buildAsync(uri, this)
                .whenComplete((ws, error) -> {
                    if (error != null) {
                        notifyError("Relay connect failed: " + error.getMessage());
                        return;
                    }
                    socket = ws;
                    JsonObject join = new JsonObject();
                    join.addProperty("t", "join");
                    join.addProperty("room", roomId);
                    join.addProperty("id", playerId);
                    join.addProperty("name", playerName);
                    join.addProperty("client", "prime");
                    if (groupId != null && !groupId.isBlank()) {
                        join.addProperty("group", groupId);
                    }
                    ws.sendText(join.toString(), true);
                });
    }

    void sendGroup(String groupId) {
        sendJson(json -> {
            json.addProperty("t", "group");
            json.addProperty("id", selfId);
            json.addProperty("group", groupId == null ? "" : groupId);
        });
    }

    void sendPosition(double x, double y, double z) {
        sendJson(json -> {
            json.addProperty("t", "pos");
            json.addProperty("id", selfId);
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("z", z);
        });
    }

    void sendAudio(byte[] pcm) {
        WebSocket ws = socket;
        if (ws == null || pcm == null || selfId.isBlank()) {
            return;
        }
        byte[] payload = new byte[UUID_BYTES + pcm.length];
        byte[] idBytes = selfId.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(idBytes, 0, payload, 0, Math.min(idBytes.length, UUID_BYTES));
        System.arraycopy(pcm, 0, payload, UUID_BYTES, pcm.length);
        ws.sendBinary(ByteBuffer.wrap(payload), true);
    }

    void disconnect(String reason) {
        WebSocket ws = socket;
        socket = null;
        if (ws != null) {
            try {
                JsonObject leave = new JsonObject();
                leave.addProperty("t", "leave");
                leave.addProperty("id", selfId);
                ws.sendText(leave.toString(), true);
            } catch (Exception ignored) {
            }
            ws.sendClose(WebSocket.NORMAL_CLOSURE, reason);
        }
        participants.clear();
        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }

    private void sendJson(Consumer<JsonObject> builder) {
        WebSocket ws = socket;
        if (ws == null) {
            return;
        }
        JsonObject json = new JsonObject();
        builder.accept(json);
        ws.sendText(json.toString(), true);
    }

    private void notifyError(String message) {
        LOGGER.warn(message);
        if (listener != null) {
            listener.onError(message);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
        if (listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            handleText(textBuffer.toString());
            textBuffer.setLength(0);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        if (bytes.length <= UUID_BYTES) {
            webSocket.request(1);
            return null;
        }
        String senderId = new String(bytes, 0, UUID_BYTES, StandardCharsets.US_ASCII).trim();
        byte[] pcm = new byte[bytes.length - UUID_BYTES];
        System.arraycopy(bytes, UUID_BYTES, pcm, 0, pcm.length);
        if (listener != null && !senderId.equals(selfId)) {
            listener.onAudio(senderId, pcm);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        socket = null;
        if (listener != null) {
            listener.onDisconnected(reason == null ? "closed" : reason);
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        notifyError(error.getMessage() == null ? "relay error" : error.getMessage());
    }

    private void handleText(String raw) {
        JsonObject json;
        try {
            json = GSON.fromJson(raw, JsonObject.class);
        } catch (RuntimeException e) {
            return;
        }
        if (json == null || !json.has("t")) {
            return;
        }
        String type = json.get("t").getAsString();
        if ("participants".equals(type) && json.has("list")) {
            participants.clear();
            JsonArray list = json.getAsJsonArray("list");
            for (JsonElement element : list) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject user = element.getAsJsonObject();
                if (!user.has("id") || !user.has("name")) {
                    continue;
                }
                String id = user.get("id").getAsString();
                if (id.equals(selfId)) {
                    continue;
                }
                VoiceParticipant participant = new VoiceParticipant(id, user.get("name").getAsString());
                if (user.has("x")) {
                    participant.setPosition(
                            user.get("x").getAsDouble(),
                            user.get("y").getAsDouble(),
                            user.get("z").getAsDouble());
                }
                if (user.has("group")) {
                    participant.setGroupId(user.get("group").getAsString());
                }
                participants.put(id, participant);
            }
            if (listener != null) {
                listener.onParticipants(Map.copyOf(participants));
            }
        } else if ("speaking".equals(type) && json.has("id")) {
            VoiceParticipant participant = participants.get(json.get("id").getAsString());
            if (participant != null && json.has("active")) {
                participant.setSpeaking(json.get("active").getAsBoolean());
            }
        } else if ("error".equals(type) && json.has("message")) {
            notifyError(json.get("message").getAsString());
        }
    }
}
