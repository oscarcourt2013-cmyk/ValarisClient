package dev.stellarclient.core.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chat completions via Prime backend proxy ({@code POST /v1/ai/chat}).
 * The Groq API key stays on the server â€” never shipped to clients.
 * Optional local key only for self-host / offline fallback.
 */
public final class GroqClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("Prime AI");
    private static final String GROQ_DIRECT = "https://api.groq.com/openai/v1/chat/completions";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record Message(String role, String content) {
    }

    public record Result(boolean ok, String content, String error) {
        public static Result success(String content) {
            return new Result(true, content, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }

    private GroqClient() {
    }

    /**
     * Prefer {@code apiBase} proxy; if blank or failing and {@code localApiKey} is set, call Groq directly.
     */
    public static CompletableFuture<Result> chat(
            String apiBase,
            String localApiKey,
            String model,
            List<Message> messages,
            double temperature,
            int maxTokens
    ) {
        if (model == null || model.isBlank()) {
            return CompletableFuture.completedFuture(Result.failure("Missing model"));
        }

        JsonArray msgs = new JsonArray();
        for (Message m : messages) {
            if (m == null || m.content() == null || m.content().isBlank()) {
                continue;
            }
            JsonObject row = new JsonObject();
            row.addProperty("role", m.role() == null ? "user" : m.role());
            row.addProperty("content", m.content());
            msgs.add(row);
        }
        if (msgs.isEmpty()) {
            return CompletableFuture.completedFuture(Result.failure("Empty prompt"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", msgs);
        body.addProperty("temperature", temperature);
        body.addProperty("max_tokens", Math.max(64, Math.min(maxTokens, 2048)));
        String jsonBody = body.toString();

        String base = apiBase == null ? "" : apiBase.trim().replaceAll("/$", "");
        if (!base.isBlank()) {
            return postJson(base + "/v1/ai/chat", null, jsonBody)
                    .thenCompose(proxyResult -> {
                        if (proxyResult.ok()) {
                            return CompletableFuture.completedFuture(proxyResult);
                        }
                        if (localApiKey != null && !localApiKey.isBlank()) {
                            LOGGER.debug("Proxy failed ({}), falling back to local key", proxyResult.error());
                            return postJson(GROQ_DIRECT, localApiKey.trim(), jsonBody);
                        }
                        return CompletableFuture.completedFuture(proxyResult);
                    });
        }
        if (localApiKey == null || localApiKey.isBlank()) {
            return CompletableFuture.completedFuture(Result.failure("AI unavailable"));
        }
        return postJson(GROQ_DIRECT, localApiKey.trim(), jsonBody);
    }

    private static CompletableFuture<Result> postJson(String url, String bearer, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
            if (bearer != null && !bearer.isBlank()) {
                builder.header("Authorization", "Bearer " + bearer);
            }
            return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(GroqClient::parseResponse)
                    .exceptionally(ex -> {
                        LOGGER.debug("AI request failed: {}", ex.getMessage());
                        return Result.failure("Network error: " + rootMessage(ex));
                    });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Result.failure(e.getMessage()));
        }
    }

    private static Result parseResponse(HttpResponse<String> res) {
        String raw = res.body() == null ? "" : res.body();
        if (res.statusCode() / 100 != 2) {
            String detail = extractError(raw);
            if (detail == null || detail.isBlank()) {
                detail = "HTTP " + res.statusCode();
            }
            return Result.failure(detail);
        }
        try {
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            // Proxy shape: { ok, message: { content } }
            if (json.has("message") && json.get("message").isJsonObject()) {
                JsonObject message = json.getAsJsonObject("message");
                if (message.has("content") && !message.get("content").isJsonNull()) {
                    String content = message.get("content").getAsString();
                    if (content != null && !content.isBlank()) {
                        return Result.success(content.trim());
                    }
                }
            }
            // Direct Groq shape: { choices: [ { message: { content } } ] }
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                    if (message != null && message.has("content")) {
                        String content = message.get("content").getAsString();
                        if (content != null && !content.isBlank()) {
                            return Result.success(content.trim());
                        }
                    }
                }
            }
            return Result.failure("Empty AI response");
        } catch (Exception e) {
            return Result.failure("Parse error: " + e.getMessage());
        }
    }

    private static String extractError(String raw) {
        try {
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("error")) {
                if (json.get("error").isJsonPrimitive()) {
                    return json.get("error").getAsString();
                }
                JsonObject err = json.getAsJsonObject("error");
                if (err.has("message")) {
                    return err.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return raw.length() > 180 ? raw.substring(0, 180) + "â€¦" : raw;
    }

    private static String rootMessage(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage() == null ? cur.getClass().getSimpleName() : cur.getMessage();
    }
}
