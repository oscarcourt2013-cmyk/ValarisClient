package dev.valerisclient.core.module;

import dev.valerisclient.core.i18n.ValerisLang;

/** The seven ValerisClient module families, in ClickGUI display order. */
public enum ModuleCategory {
    PVP("PvP", "P", 0xFFEF4444),
    SURVIVAL("Survival", "S", 0xFF84CC16),
    PERFORMANCE("Performance", "F", 0xFF22C55E),
    QOL("QoL", "Q", 0xFF3B82F6),
    STREAMERS("Streamers", "▶", 0xFF9146FF),
    CREATOR("Creator", "C", 0xFFA855F7),
    PRIME("Valeris", "★", 0xFFF59E0B);

    private final String displayName;
    private final String icon;
    private final int accent;

    ModuleCategory(String displayName, String icon, int accent) {
        this.displayName = displayName;
        this.icon = icon;
        this.accent = accent;
    }

    public String displayName() {
        return ValerisLang.category(this, displayName);
    }

    public String icon() {
        return icon;
    }

    public int accent() {
        return accent;
    }
}
