package dev.stellarclient.core.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Refreshes a Microsoft account into a live Minecraft access token.
 *
 * <p>Supports both launcher auth stacks:
 * <ul>
 *   <li>Electron / msmc — Mojang client {@code 00000000402b5328} via {@code login.live.com}</li>
 *   <li>Tauri / Prism — Azure client {@code 1ce91f64-…} via {@code login.microsoftonline.com}</li>
 * </ul>
 */
public final class MicrosoftTokenRefresh {

    /** msmc / Electron default (Minecraft Launcher public client). */
    public static final String CLIENT_LIVE = "00000000402b5328";
    /** Prism / MultiMC family public client. */
    public static final String CLIENT_AZURE = "1ce91f64-568a-42b5-b1c3-4e6871f5b8c5";

    public record Result(String accessToken, String refreshToken, String username, String uuid, String authProvider) {
    }

    private record Endpoint(String name, String tokenUrl, String clientId) {
    }

    private MicrosoftTokenRefresh() {
    }

    /**
     * @param preferredProvider optional hint from accounts.json ({@code live} / {@code azure}); null = try live then azure
     */
    public static Result refresh(String refreshToken, String preferredProvider) throws IOException {
        List<Endpoint> order = new ArrayList<>(2);
        Endpoint live = new Endpoint(
                "live",
                "https://login.live.com/oauth20_token.srf",
                CLIENT_LIVE);
        Endpoint azure = new Endpoint(
                "azure",
                "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
                CLIENT_AZURE);

        if ("azure".equalsIgnoreCase(preferredProvider)) {
            order.add(azure);
            order.add(live);
        } else {
            // Default: Electron/msmc first (primary launcher), then Tauri/Prism.
            order.add(live);
            order.add(azure);
        }

        IOException last = null;
        for (Endpoint ep : order) {
            try {
                return refreshWith(refreshToken, ep);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null
                ? last
                : new IOException("Microsoft token refresh failed.");
    }

    /** @deprecated Prefer {@link #refresh(String, String)} with an auth-provider hint. */
    @Deprecated
    public static Result refresh(String refreshToken, String clientId, boolean unused) throws IOException {
        String preferred = CLIENT_AZURE.equals(clientId) ? "azure" : "live";
        return refresh(refreshToken, preferred);
    }

    private static Result refreshWith(String refreshToken, Endpoint endpoint) throws IOException {
        String form = "client_id=" + enc(endpoint.clientId)
                + "&grant_type=refresh_token"
                + "&refresh_token=" + enc(refreshToken);
        if ("azure".equals(endpoint.name)) {
            form += "&scope=" + enc("XboxLive.signin offline_access");
        }

        JsonObject token = postForm(endpoint.tokenUrl, form);
        String msAccess = req(token, "access_token");
        String newRefresh = token.has("refresh_token")
                ? token.get("refresh_token").getAsString()
                : refreshToken;

        JsonObject xboxBody = new JsonObject();
        JsonObject xboxProps = new JsonObject();
        xboxProps.addProperty("AuthMethod", "RPS");
        xboxProps.addProperty("SiteName", "user.auth.xboxlive.com");
        xboxProps.addProperty("RpsTicket", "d=" + msAccess);
        xboxBody.add("Properties", xboxProps);
        xboxBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xboxBody.addProperty("TokenType", "JWT");
        JsonObject xbox = postJson("https://user.auth.xboxlive.com/user/authenticate", xboxBody);
        String xboxToken = req(xbox, "Token");
        String uhs = xbox.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0).getAsJsonObject()
                .get("uhs").getAsString();

        JsonObject xstsBody = new JsonObject();
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken);
        xstsProps.add("UserTokens", userTokens);
        xstsBody.add("Properties", xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody);
        String xstsToken = req(xsts, "Token");

        JsonObject mcLogin = new JsonObject();
        mcLogin.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        JsonObject mc = postJson(
                "https://api.minecraftservices.com/authentication/login_with_xbox", mcLogin);
        String mcToken = req(mc, "access_token");

        JsonObject profile = getJson(
                "https://api.minecraftservices.com/minecraft/profile", mcToken);
        String rawId = req(profile, "id");
        String name = profile.has("name") ? profile.get("name").getAsString() : "Player";
        String dashed = dashUuid(rawId);
        return new Result(mcToken, newRefresh, name, dashed, endpoint.name);
    }

    private static String dashUuid(String uuid) {
        if (uuid.contains("-")) {
            return uuid;
        }
        if (uuid.length() != 32) {
            return uuid;
        }
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-"
                + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-"
                + uuid.substring(20, 32);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String req(JsonObject o, String key) throws IOException {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw new IOException("Missing field: " + key);
        }
        return o.get(key).getAsString();
    }

    private static JsonObject postForm(String url, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "StellarClient/1.2");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readJson(conn);
    }

    private static JsonObject postJson(String url, JsonObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "StellarClient/1.2");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        return readJson(conn);
    }

    private static JsonObject getJson(String url, String bearer) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Authorization", "Bearer " + bearer);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "StellarClient/1.2");
        return readJson(conn);
    }

    private static JsonObject readJson(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = "";
        if (stream != null) {
            body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (code >= 400) {
            throw new IOException("HTTP " + code + (body.isBlank() ? "" : ": " + body.lines().limit(2).collect(Collectors.joining(" "))));
        }
        return JsonParser.parseString(body).getAsJsonObject();
    }
}
