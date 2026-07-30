package dev.stellarclient.core.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.config.ConfigBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of {@link Theme}s and holder of the active one.
 *
 * <p>{@link #active()} is a plain field read — safe to call every frame.</p>
 *
 * <p>Shared IDs with the launcher: {@code prime-crimson}, {@code prime-midnight},
 * {@code prime-aurora}, {@code prime-obsidian}, {@code prime-ember}.</p>
 */
public final class ThemeManager implements ConfigBinding {

    /** Brand red — signature Prime look (default). */
    public static final Theme PRIME_CRIMSON = new Theme(
            "prime-crimson", "Prime Crimson",
            0xFFE11D2E, // accent
            0xFF991B1B, // accentSecondary
            0xFF060608, // background
            0xF0111116, // backgroundLight
            0xF018181F, // surfaceElevated
            0xFFF8F9FC, // foreground
            0xFF8B8FA3, // foregroundMuted
            0x35FFFFFF, // border
            0xCC000000, // overlay
            0xFF060608, // gradientTop
            0xFF111116, // gradientBottom
            0xFF22C55E, // success
            0xFFF59E0B, // warning
            0xFFEF4444  // error
    );

    /** Deep navy with cool cyan accents. */
    public static final Theme PRIME_MIDNIGHT = new Theme(
            "prime-midnight", "Prime Midnight",
            0xFF38BDF8, // accent
            0xFF0284C7, // accentSecondary
            0xFF070B14, // background
            0xF00E1624, // backgroundLight
            0xF0141E2E, // surfaceElevated
            0xFFE8EEF8, // foreground
            0xFF7B8BA3, // foregroundMuted
            0x35A5C4E8, // border
            0xCC000000, // overlay
            0xFF070B14, // gradientTop
            0xFF0E1624, // gradientBottom
            0xFF34D399, // success
            0xFFFBBF24, // warning
            0xFFF87171  // error
    );

    /** Teal / violet aurora palette. */
    public static final Theme PRIME_AURORA = new Theme(
            "prime-aurora", "Prime Aurora",
            0xFF34D399, // accent
            0xFFA78BFA, // accentSecondary
            0xFF06120F, // background
            0xF00C1C18, // backgroundLight
            0xF0122820, // surfaceElevated
            0xFFF0FDF8, // foreground
            0xFF86A89A, // foregroundMuted
            0x3590E0C0, // border
            0xCC000000, // overlay
            0xFF06120F, // gradientTop
            0xFF0C1C18, // gradientBottom
            0xFF4ADE80, // success
            0xFFFBBF24, // warning
            0xFFFB7185  // error
    );

    /** Elevated — champagne gold on deep black. */
    public static final Theme PRIME_OBSIDIAN = new Theme(
            "prime-obsidian", "Prime Obsidian",
            0xFFF0D78C, // accent
            0xFFD4AF37, // accentSecondary
            0xFF030303, // background
            0xF00E0E0E, // backgroundLight
            0xF0161616, // surfaceElevated
            0xFFF7F1DF, // foreground
            0xFFA89B78, // foregroundMuted
            0x40F0D78C, // border
            0xCC000000, // overlay
            0xFF030303, // gradientTop
            0xFF0E0E0E, // gradientBottom
            0xFF4ADE80, // success
            0xFFFBBF24, // warning
            0xFFF87171  // error
    );

    /** Elevated — copper ember on charcoal. */
    public static final Theme PRIME_EMBER = new Theme(
            "prime-ember", "Prime Ember",
            0xFFFDBA74, // accent
            0xFFEA580C, // accentSecondary
            0xFF080504, // background
            0xF015100D, // backgroundLight
            0xF01E1612, // surfaceElevated
            0xFFFFF5ED, // foreground
            0xFFB89784, // foregroundMuted
            0x40FDBA74, // border
            0xCC000000, // overlay
            0xFF080504, // gradientTop
            0xFF15100D, // gradientBottom
            0xFF4ADE80, // success
            0xFFFBBF24, // warning
            0xFFF87171  // error
    );

    private final Map<String, Theme> themes = new LinkedHashMap<>();
    private Theme active;

    public ThemeManager() {
        register(PRIME_CRIMSON);
        register(PRIME_MIDNIGHT);
        register(PRIME_AURORA);
        register(PRIME_OBSIDIAN);
        register(PRIME_EMBER);
        this.active = PRIME_CRIMSON;
    }

    public void register(Theme theme) {
        Theme previous = themes.putIfAbsent(theme.id(), theme);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate theme id: " + theme.id());
        }
    }

    /** The active theme. Never {@code null}. */
    public Theme active() {
        return active;
    }

    /** @throws IllegalArgumentException if no theme has this id */
    public void setActive(String id) {
        Theme theme = themes.get(normalizeId(id));
        if (theme == null) {
            throw new IllegalArgumentException("Unknown theme: " + id);
        }
        this.active = theme;
    }

    /** Sets the theme if known; otherwise leaves the current theme unchanged. */
    public boolean trySetActive(String id) {
        Theme theme = themes.get(normalizeId(id));
        if (theme == null) {
            return false;
        }
        this.active = theme;
        return true;
    }

    public Collection<Theme> all() {
        return Collections.unmodifiableCollection(themes.values());
    }

    /**
     * Maps legacy IDs ({@code prime-dark}, {@code prime-light}) to the current set.
     */
    public static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return PRIME_CRIMSON.id();
        }
        return switch (id) {
            case "prime-dark" -> PRIME_CRIMSON.id();
            case "prime-light" -> PRIME_MIDNIGHT.id();
            default -> id;
        };
    }

    @Override
    public String configKey() {
        return "theme";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("active", active.id());
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonElement stored = element.getAsJsonObject().get("active");
        if (stored == null) {
            return;
        }
        trySetActive(stored.getAsString());
    }
}
