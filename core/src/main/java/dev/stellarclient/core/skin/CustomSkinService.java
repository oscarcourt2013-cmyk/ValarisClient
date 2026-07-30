package dev.stellarclient.core.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.state.CustomSkinState;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persists and applies Prime-only custom skins ({@code /skin}).
 * Visible locally (including offline) and to other StellarClient peers.
 */
public final class CustomSkinService {

    public static final int MAX_BYTES = 128 * 1024;
    private static final String PREFIX = "§6[Prime Skin] §f";
    private static final String MUTED = "§7";
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final MinecraftAdapter adapter;
    private final Path skinFile;
    private final Path profileFile;
    private final ExecutorService downloadPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "prime-skin-download");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean downloading = new AtomicBoolean(false);
    private volatile boolean enabled;
    private long lastFileMtime = -1L;

    public CustomSkinService(MinecraftAdapter adapter, Path modRoot) {
        this.adapter = adapter;
        this.skinFile = modRoot.resolve("custom_skin.png");
        this.profileFile = modRoot.resolve("profiles").resolve("default.json");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Path skinFile() {
        return skinFile;
    }

    /** Load from disk / profile bridge if present. */
    public void loadFromDisk() {
        try {
            if (tryLoadProfileBase64()) {
                return;
            }
            if (Files.isRegularFile(skinFile)) {
                byte[] bytes = Files.readAllBytes(skinFile);
                if (isValidPng(bytes)) {
                    applyLocal(bytes, false);
                    lastFileMtime = Files.getLastModifiedTime(skinFile).toMillis();
                    return;
                }
            }
        } catch (IOException e) {
            StellarClient.LOGGER.warn("Failed to load custom skin: {}", e.toString());
        }
    }

    /** Cheap mtime poll so launcher bridge copies are picked up without restart. */
    public void pollBridgeFile() {
        if (!enabled) {
            return;
        }
        try {
            if (!Files.isRegularFile(skinFile)) {
                if (CustomSkinState.hasLocal() && lastFileMtime >= 0) {
                    clearLocal(false);
                    lastFileMtime = -1L;
                }
                return;
            }
            long mtime = Files.getLastModifiedTime(skinFile).toMillis();
            if (mtime == lastFileMtime) {
                return;
            }
            byte[] bytes = Files.readAllBytes(skinFile);
            if (!isValidPng(bytes)) {
                return;
            }
            String hash = CustomSkinState.hashOf(bytes);
            if (hash.equals(CustomSkinState.localHash())) {
                lastFileMtime = mtime;
                return;
            }
            applyLocal(bytes, false);
            lastFileMtime = mtime;
        } catch (IOException ignored) {
            // Bridge race — retry next poll.
        }
    }

    public void setLocal(byte[] bytes) {
        applyLocal(bytes, true);
    }

    public void clearLocal() {
        clearLocal(true);
    }

    public boolean handleClientCommand(String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (!text.regionMatches(true, 0, "/skin", 0, 5)) {
            return false;
        }
        if (text.length() > 5 && !Character.isWhitespace(text.charAt(5))) {
            return false;
        }

        if (!enabled) {
            say("Module Custom Skin désactivé. Active-le dans le ClickGUI (Prime).");
            return true;
        }

        String rest = text.length() > 5 ? text.substring(5).trim() : "";
        if (rest.isEmpty() || "help".equalsIgnoreCase(rest) || "?".equals(rest)) {
            printHelp();
            return true;
        }

        String lower = rest.toLowerCase(Locale.ROOT);
        if ("clear".equals(lower) || "reset".equals(lower) || "off".equals(lower)) {
            clearLocal(true);
            say("Skin custom effacé — retour au skin Mojang / défaut.");
            return true;
        }
        if (lower.startsWith("url ") || lower.startsWith("set ")) {
            String url = rest.substring(rest.indexOf(' ') + 1).trim();
            downloadFromUrl(url);
            return true;
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            downloadFromUrl(rest.trim());
            return true;
        }
        if ("status".equals(lower)) {
            if (CustomSkinState.hasLocal()) {
                say("Skin custom actif — hash §f" + shortHash(CustomSkinState.localHash()));
                say(MUTED + "Fichier: " + skinFile);
            } else {
                say("Aucun skin custom — utilise §f/skin url <png>§7.");
            }
            return true;
        }

        say("§cUsage inconnu. §7Tape §f/skin§7 pour l’aide.");
        return true;
    }

    private void printHelp() {
        say("Skin custom Prime — visible pour toi et les peers Prime (offline OK).");
        say("§f/skin url <https://...png> §7— télécharger un PNG (max 128 Ko)");
        say("§f/skin clear §7— retirer le skin custom");
        say("§f/skin status §7— hash + fichier");
        say(MUTED + "Le launcher peut aussi copier le skin actif au lancement.");
    }

    private void downloadFromUrl(String url) {
        if (url == null || url.isBlank()) {
            say("Usage: §f/skin url <http(s)://...png>");
            return;
        }
        String trimmed = url.trim();
        if (!trimmed.regionMatches(true, 0, "http://", 0, 7)
                && !trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            say("§cURL invalide — http(s) uniquement.");
            return;
        }
        if (!downloading.compareAndSet(false, true)) {
            say("§eTéléchargement déjà en cours…");
            return;
        }
        say(MUTED + "Téléchargement…");
        downloadPool.execute(() -> {
            try {
                byte[] bytes = fetchPng(trimmed);
                adapter.runOnClientThread(() -> {
                    try {
                        applyLocal(bytes, true);
                        say("§aSkin appliqué — visible localement et pour les peers Prime.");
                    } catch (RuntimeException e) {
                        say("§cÉchec: " + e.getMessage());
                    } finally {
                        downloading.set(false);
                    }
                });
            } catch (Exception e) {
                adapter.runOnClientThread(() -> {
                    say("§cTéléchargement échoué: " + e.getMessage());
                    downloading.set(false);
                });
            }
        });
    }

    private byte[] fetchPng(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "image/png,image/*,*/*")
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        byte[] bytes;
        try (InputStream in = response.body()) {
            bytes = in.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length > MAX_BYTES) {
            throw new IOException("PNG trop volumineux (max 128 Ko)");
        }
        if (!isValidPng(bytes)) {
            throw new IOException("Pas un PNG valide");
        }
        if (!contentType.isBlank()
                && !contentType.toLowerCase(Locale.ROOT).contains("png")
                && !contentType.toLowerCase(Locale.ROOT).contains("octet-stream")
                && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IOException("Content-Type attendu image/png (reçu " + contentType + ")");
        }
        return bytes;
    }

    private void applyLocal(byte[] bytes, boolean writeFile) {
        if (!isValidPng(bytes)) {
            throw new IllegalArgumentException("PNG invalide");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("PNG trop volumineux (max 128 Ko)");
        }
        CustomSkinState.setLocal(bytes);
        if (writeFile) {
            try {
                Files.createDirectories(skinFile.getParent());
                Files.write(skinFile, bytes);
                lastFileMtime = Files.getLastModifiedTime(skinFile).toMillis();
            } catch (IOException e) {
                StellarClient.LOGGER.warn("Could not persist custom skin: {}", e.toString());
            }
        }
        requestAnnounce();
    }

    private void clearLocal(boolean deleteFile) {
        CustomSkinState.clearLocal();
        if (deleteFile) {
            try {
                Files.deleteIfExists(skinFile);
            } catch (IOException ignored) {
            }
            lastFileMtime = -1L;
        }
        requestAnnounce();
    }

    private boolean tryLoadProfileBase64() {
        if (!Files.isRegularFile(profileFile)) {
            return false;
        }
        try {
            String raw = Files.readString(profileFile);
            JsonElement root = JsonParser.parseString(raw);
            if (!root.isJsonObject()) {
                return false;
            }
            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("customSkin") || obj.get("customSkin").isJsonNull()) {
                return false;
            }
            String b64 = obj.get("customSkin").getAsString();
            if (b64 == null || b64.isBlank()) {
                return false;
            }
            if (b64.startsWith("data:")) {
                int comma = b64.indexOf(',');
                if (comma < 0) {
                    return false;
                }
                b64 = b64.substring(comma + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(b64);
            if (!isValidPng(bytes)) {
                return false;
            }
            applyLocal(bytes, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void requestAnnounce() {
        try {
            StellarClient.get().presence().requestAnnounce();
        } catch (IllegalStateException ignored) {
            CustomSkinState.markAnnounceDirty();
        }
    }

    private void say(String message) {
        adapter.displayClientMessage(PREFIX + message);
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.length() < 8) {
            return hash == null ? "" : hash;
        }
        return hash.substring(0, 8);
    }

    public static boolean isValidPng(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_MAGIC.length || bytes.length > MAX_BYTES) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (bytes[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    public void shutdown() {
        downloadPool.shutdownNow();
    }
}
