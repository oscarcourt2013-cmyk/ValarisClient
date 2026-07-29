package dev.stellarclient.core.i18n;

import dev.stellarclient.core.module.ModuleCategory;

import java.util.Locale;

/** Resolves StellarClient UI strings via the bound Minecraft language manager. */
public final class PrimeLang {

    @FunctionalInterface
    public interface Translator {
        String translate(String key, String fallback, Object... args);
    }

    private static Translator translator = (key, fallback, args) -> format(fallback, args);

    private PrimeLang() {
    }

    public static void bind(Translator next) {
        translator = next != null ? next : (key, fallback, args) -> format(fallback, args);
    }

    public static void unbind() {
        translator = (key, fallback, args) -> format(fallback, args);
    }

    public static String get(String key, String fallback) {
        return translator.translate(key, fallback);
    }

    public static String get(String key, String fallback, Object... args) {
        return translator.translate(key, fallback, args);
    }

    public static String module(String moduleId, String field, String fallback) {
        return get("prime.module." + idKey(moduleId) + "." + field, fallback);
    }

    public static String setting(String moduleId, String settingId, String field, String fallback) {
        return get("prime.module." + idKey(moduleId) + ".setting." + idKey(settingId) + "." + field, fallback);
    }

    public static String category(ModuleCategory category, String fallback) {
        return get("prime.category." + category.name().toLowerCase(Locale.ROOT), fallback);
    }

    public static String hud(String elementId, String fallback) {
        return get("prime.hud." + idKey(elementId), fallback);
    }

    public static String enumValue(Enum<?> value) {
        String enumKey = camelToSnake(value.getDeclaringClass().getSimpleName());
        String valueKey = value.name().toLowerCase(Locale.ROOT);
        return get("prime.enum." + enumKey + "." + valueKey, humanizeEnum(value.name()));
    }

    public static String cosmetic(String cosmeticId, String fallback) {
        return get("prime.cosmetic." + idKey(cosmeticId), fallback);
    }

    private static String idKey(String id) {
        return id.replace('-', '_');
    }

    private static String camelToSnake(String name) {
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                out.append('_');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    private static String humanizeEnum(String name) {
        if (name.contains("_")) {
            String[] parts = name.split("_");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                String p = parts[i].toLowerCase(Locale.ROOT);
                if (!p.isEmpty()) {
                    sb.append(Character.toUpperCase(p.charAt(0)));
                    if (p.length() > 1) {
                        sb.append(p.substring(1));
                    }
                }
            }
            return sb.toString();
        }
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String format(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (Exception ignored) {
            return template;
        }
    }
}
