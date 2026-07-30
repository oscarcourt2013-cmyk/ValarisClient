package dev.valerisclient.core.crash;

import com.google.gson.JsonObject;
import dev.valerisclient.core.social.SocialSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** POSTs crash report text to the Prime social backend. */
public final class CrashReportUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger("Valeris Crash");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private CrashReportUploader() {
    }

    /**
     * @param bearerToken social session token; upload is skipped when null/blank
     */
    public static boolean upload(SocialSettings settings, String title, String text, String bearerToken) {
        if (settings == null || text == null || text.isBlank()) {
            return false;
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("title", title == null || title.isBlank() ? "crash" : title);
            body.addProperty("text", text);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(settings.apiBase() + "/v1/crash"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return res.statusCode() / 100 == 2;
        } catch (Exception e) {
            LOGGER.debug("Crash upload failed: {}", e.getMessage());
            return false;
        }
    }
}
