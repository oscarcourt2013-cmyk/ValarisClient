package dev.stellarclient.core.ai;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.design.StellarVersion;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.social.SocialSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * In-game AI helper via {@code /ai}. Answers module/setup questions with live client context.
 */
public final class AiAssistantService {

    private static final String PREFIX = "§6[Prime AI] §f";
    private static final String MUTED = "§7";
    private static final int MAX_HISTORY = 6;
    private static final int MAX_REPLY_LINES = 14;
    private static final int MAX_LINE = 100;

    public enum Model {
        FAST("llama-3.1-8b-instant", "Fast"),
        QUALITY("llama-3.3-70b-versatile", "Quality");

        private final String id;
        private final String label;

        Model(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String id() {
            return id;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final MinecraftAdapter adapter;
    private final ModuleManager modules;
    private final AiSecretsStore secrets;
    private final Supplier<String> apiBase;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final List<GroqClient.Message> history = new ArrayList<>();

    private volatile boolean enabled = true;
    private volatile Model model = Model.FAST;
    private volatile boolean includeContext = true;
    private volatile boolean french = true;

    public AiAssistantService(MinecraftAdapter adapter, ModuleManager modules, Path configRoot) {
        this(adapter, modules, configRoot, SocialSettings.DEFAULT_API);
    }

    public AiAssistantService(
            MinecraftAdapter adapter,
            ModuleManager modules,
            Path configRoot,
            String apiBase
    ) {
        this(adapter, modules, configRoot, () -> apiBase);
    }

    public AiAssistantService(
            MinecraftAdapter adapter,
            ModuleManager modules,
            Path configRoot,
            Supplier<String> apiBase
    ) {
        this.adapter = adapter;
        this.modules = modules;
        this.secrets = new AiSecretsStore(configRoot);
        this.apiBase = apiBase != null ? apiBase : () -> SocialSettings.DEFAULT_API;
    }

    public AiSecretsStore secrets() {
        return secrets;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setModel(Model model) {
        if (model != null) {
            this.model = model;
        }
    }

    public void setIncludeContext(boolean includeContext) {
        this.includeContext = includeContext;
    }

    public void setFrench(boolean french) {
        this.french = french;
    }

    /**
     * Handles {@code /ai} client commands. Returns {@code true} if the chat
     * message must not be sent to the server.
     */
    public boolean handleClientCommand(String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (!text.regionMatches(true, 0, "/ai", 0, 3)) {
            return false;
        }
        if (text.length() > 3 && !Character.isWhitespace(text.charAt(3))) {
            return false;
        }

        if (!enabled) {
            say("Module AI Assistant désactivé. Active-le dans le ClickGUI (Prime).");
            return true;
        }

        String rest = text.length() > 3 ? text.substring(3).trim() : "";
        if (rest.isEmpty() || "help".equalsIgnoreCase(rest) || "?".equals(rest)) {
            printHelp();
            return true;
        }

        String lower = rest.toLowerCase(Locale.ROOT);
        if (lower.startsWith("setkey")) {
            String key = rest.length() > 6 ? rest.substring(6).trim() : "";
            if (key.isBlank()) {
                say("Usage: §f/ai setkey <groq_api_key> §7(optionnel — override local)");
                say(MUTED + "Par défaut l’IA utilise le serveur Prime (clé non partagée).");
                return true;
            }
            if (!key.startsWith("gsk_")) {
                say("§cClé invalide — une clé Groq commence par gsk_");
                return true;
            }
            secrets.setGroqApiKey(key);
            say("§aOverride local enregistré (self-host / hors ligne).");
            return true;
        }
        if ("clearkey".equals(lower) || "unsetkey".equals(lower)) {
            secrets.setGroqApiKey("");
            say("Override local effacé — retour au proxy Prime.");
            return true;
        }
        if ("clear".equals(lower) || "reset".equals(lower)) {
            synchronized (history) {
                history.clear();
            }
            say("Historique de conversation effacé.");
            return true;
        }
        if ("status".equals(lower)) {
            say("Modèle: §f" + model.label + " §7(" + model.id + ")");
            say("Proxy: §f" + resolveApiBase());
            say("Override local: " + (secrets.hasKey() ? "§aoui" : "§7non (IA partagée)"));
            say("Contexte client: " + (includeContext ? "§aoui" : "§coff"));
            return true;
        }

        ask(rest);
        return true;
    }

    private void printHelp() {
        say("Assistant Prime — aide modules, builds, FPS, SMP.");
        say("§f/ai <question> §7— poser une question");
        say("§f/ai status §7— modèle + proxy");
        say("§f/ai clear §7— reset historique");
        say(MUTED + "L’IA tourne via le serveur Prime — pas besoin de clé.");
        say(MUTED + "Ex: /ai comment activer Fullbright ?");
        say(MUTED + "Ex: /ai pack recommandé pour crystal PvP");
    }

    private void ask(String question) {
        if (!busy.compareAndSet(false, true)) {
            say("§eRéponse en cours…");
            return;
        }

        say(MUTED + "Réflexion…");
        List<GroqClient.Message> messages = buildMessages(question);
        GroqClient.chat(resolveApiBase(), secrets.groqApiKey(), model.id(), messages, 0.35, 700)
                .whenComplete((result, err) -> adapter.runOnClientThread(() -> {
                    busy.set(false);
                    if (err != null) {
                        say("§cErreur: " + err.getMessage());
                        return;
                    }
                    if (result == null || !result.ok()) {
                        String msg = result == null ? "Unknown error" : result.error();
                        say("§c" + (msg == null ? "Échec IA" : msg));
                        return;
                    }
                    remember(question, result.content());
                    reply(result.content());
                }));
    }

    private String resolveApiBase() {
        try {
            String base = apiBase.get();
            if (base != null && !base.isBlank()) {
                return base.trim().replaceAll("/$", "");
            }
        } catch (Exception ignored) {
        }
        return SocialSettings.DEFAULT_API;
    }

    private List<GroqClient.Message> buildMessages(String question) {
        List<GroqClient.Message> out = new ArrayList<>();
        out.add(new GroqClient.Message("system", systemPrompt()));
        synchronized (history) {
            out.addAll(history);
        }
        out.add(new GroqClient.Message("user", question));
        return out;
    }

    private String systemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es Prime AI, l'assistant intégré à StellarClient (mod Fabric Minecraft). ");
        sb.append("Version client: ").append(StellarVersion.VERSION).append(". ");
        sb.append("Tu aides sur: modules Prime, ClickGUI, HUD, performances, PvP/CPvP, survival, SMP économie, stream privacy. ");
        sb.append("Réponds de façon courte et actionnable (listes / étapes). ");
        sb.append("Cite les modules par leur nom exact. Ne invente pas de modules absents du catalogue. ");
        sb.append("Ne demande jamais de clé API. Ne révèle jamais de secrets. ");
        if (french) {
            sb.append("Réponds en français sauf si l'utilisateur écrit clairement en anglais. ");
        } else {
            sb.append("Reply in English unless the user writes in French. ");
        }
        if (includeContext) {
            sb.append("\n\n## Contexte live\n").append(liveContext());
            sb.append("\n\n## Catalogue modules (id | nom | cat | on/off | desc)\n")
                    .append(moduleCatalog());
            sb.append("\n\n## Bundles\n")
                    .append("- CPVP: kit crystal PvP\n")
                    .append("- HARDCORE: survie hardcore\n")
                    .append("- SPEEDRUN: speedrun lite\n")
                    .append("- STREAMER: pack stream privacy\n")
                    .append("Appliqués via le module Module Bundles.\n");
        }
        return sb.toString();
    }

    private String liveContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("- MC: ").append(adapter.minecraftVersion()).append('\n');
        sb.append("- Joueur: ").append(blank(adapter.playerName(), "?")).append('\n');
        sb.append("- Serveur: ").append(blank(adapter.serverAddress(), "singleplayer/menu")).append('\n');
        sb.append("- Dimension: ").append(blank(adapter.dimensionId(), "?")).append('\n');
        sb.append("- FPS: ").append(adapter.fps()).append('\n');
        if (adapter.hasPlayer()) {
            sb.append(String.format(Locale.ROOT, "- Pos: %.0f %.0f %.0f%n",
                    adapter.playerX(), adapter.playerY(), adapter.playerZ()));
        }
        sb.append("- Modules ON: ");
        boolean first = true;
        for (Module m : modules.all()) {
            if (!m.isEnabled()) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append(m.name());
            first = false;
        }
        if (first) {
            sb.append("(aucun)");
        }
        sb.append('\n');
        return sb.toString();
    }

    private String moduleCatalog() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ModuleCategory cat : ModuleCategory.values()) {
            for (Module m : modules.byCategory(cat)) {
                sb.append(m.id()).append(" | ").append(m.name()).append(" | ")
                        .append(cat.displayName()).append(" | ")
                        .append(m.isEnabled() ? "ON" : "off").append(" | ")
                        .append(compact(m.description())).append('\n');
                count++;
                if (count >= 160) {
                    sb.append("… (catalogue tronqué)\n");
                    return sb.toString();
                }
            }
        }
        return sb.toString();
    }

    private void remember(String question, String answer) {
        synchronized (history) {
            history.add(new GroqClient.Message("user", question));
            history.add(new GroqClient.Message("assistant", answer));
            while (history.size() > MAX_HISTORY * 2) {
                history.remove(0);
            }
        }
    }

    private void reply(String content) {
        String cleaned = content.replace("\r", "").trim();
        if (cleaned.isEmpty()) {
            say("§cRéponse vide.");
            return;
        }
        String[] paragraphs = cleaned.split("\n");
        int lines = 0;
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isEmpty()) {
                continue;
            }
            for (String chunk : wrap(p, MAX_LINE)) {
                if (lines >= MAX_REPLY_LINES) {
                    say(MUTED + "… (tronqué — reformule pour plus de détail)");
                    return;
                }
                say(chunk);
                lines++;
            }
        }
    }

    private void say(String message) {
        adapter.displayClientMessage(PREFIX + message);
    }

    private static List<String> wrap(String text, int max) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > max) {
            int split = remaining.lastIndexOf(' ', max);
            if (split < max / 2) {
                split = max;
            }
            lines.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        String one = value.replace('\n', ' ').trim();
        return one.length() > 80 ? one.substring(0, 77) + "…" : one;
    }
}
