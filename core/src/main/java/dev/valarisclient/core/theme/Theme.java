package dev.valarisclient.core.theme;

/**
 * A ValarisClient color theme. Colors are packed ARGB ints (0xAARRGGBB) —
 * the format every renderer consumes directly, no conversion in render paths.
 */
public record Theme(
        String id,
        String name,
        int accent,
        int accentSecondary,
        int background,
        int backgroundLight,
        int surfaceElevated,
        int foreground,
        int foregroundMuted,
        int border,
        int overlay,
        int gradientTop,
        int gradientBottom,
        int success,
        int warning,
        int error
) {
}
