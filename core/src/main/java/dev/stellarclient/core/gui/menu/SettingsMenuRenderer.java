package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.cloud.CloudSyncManager;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.design.PrimeLogo;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.keybind.KeyNames;
import dev.stellarclient.core.keybind.Keybind;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.profile.ProfileManager;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Full settings hub with searchable categories. */
public final class SettingsMenuRenderer {

    private static final int ROW_HEIGHT = 16;
    private static final int KEY_BTN_W = 88;

    public enum Category {
        GENERAL("General"),
        APPEARANCE("Appearance"),
        PERFORMANCE("Performance"),
        CONTROLS("Controls"),
        ACCOUNT("Account"),
        PRIVACY("Privacy"),
        ABOUT("About");

        final String label;
        final String key;

        Category(String label) {
            this.label = label;
            this.key = "prime.gui.settings.tab." + name().toLowerCase();
        }

        String translated() {
            return PrimeLang.get(key, label);
        }
    }

    private Category active = Category.GENERAL;
    private final StringBuilder search = new StringBuilder();
    private int controlsScroll;
    private Keybind listeningFor;

    public Category active() {
        return active;
    }

    public boolean capturingKey() {
        return listeningFor != null;
    }

    public boolean captureKey(int glfwKey, KeybindManager keybinds) {
        if (listeningFor == null) {
            return false;
        }
        if (glfwKey == 256) {
            listeningFor = null;
            return true;
        }
        if (glfwKey == 259 || glfwKey == 261) {
            keybinds.rebind(listeningFor, Keybind.UNBOUND);
            listeningFor = null;
            return true;
        }
        keybinds.rebind(listeningFor, glfwKey);
        listeningFor = null;
        return true;
    }

    public boolean scroll(double delta) {
        if (active != Category.CONTROLS) {
            return false;
        }
        controlsScroll = Math.max(0, controlsScroll - (int) delta);
        return true;
    }

    /**
     * Preferred width, but never wider than the screen. The lower bound must stay
     * below Minecraft's smallest effective GUI canvas (320x240) or the panel is
     * forced off-screen at high GUI scales.
     */
    private static int settingsPanelW(int screenW) {
        int preferred = Math.min(420, Math.max(360, screenW - 80));
        return Math.min(preferred, Math.max(220, screenW - 12));
    }

    private static int settingsPanelH(int screenH) {
        int preferred = Math.min(300, Math.max(260, screenH - 60));
        return Math.min(preferred, Math.max(170, screenH - 12));
    }

    public void render(RenderContext ctx, Theme theme, ThemeManager themes, ProfileManager profiles,
                       CloudSyncManager cloud, MinecraftAdapter adapter, KeybindManager keybinds,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int panelW = settingsPanelW(screenW);
        int panelH = settingsPanelH(screenH);
        int x = (screenW - panelW) / 2;
        int y = (screenH - panelH) / 2;
        UiChrome.glassPanel(ctx, theme, x, y, panelW, panelH);
        GuiLayout.label(ctx, PrimeLang.get("prime.gui.settings.title", "Settings"), x + 12, y + 10, theme.accent());

        int tabY = y + 28;
        int tabX = x + 8;
        int tabsInRow = 0;
        ctx.pushClip(x + 4, tabY, panelW - 8, 34);
        for (Category cat : Category.values()) {
            if (!matchesSearch(cat.translated())) {
                continue;
            }
            boolean sel = cat == active;
            int tw = GuiLayout.tabWidth(ctx, cat.translated(), 10);
            if (tabsInRow >= 4) {
                tabX = x + 8;
                tabY += 16;
                tabsInRow = 0;
            }
            ctx.fillRoundedRect(tabX, tabY, tw, 14, PrimeDesign.RADIUS_SM,
                    sel ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, cat.translated(), tabX + 5, tabY + 3, sel ? theme.accent() : theme.foregroundMuted());
            tabX += tw + 4;
            tabsInRow++;
        }
        ctx.popClip();

        int rowY = tabY + 22;
        int contentBottom = y + panelH - 28;
        ctx.pushClip(x + 4, rowY, panelW - 8, contentBottom - rowY);
        switch (active) {
            case GENERAL -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.profile", "Profile"), profiles.activeProfile());
                rowY += 16;
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.minecraft", "Minecraft"), adapter.minecraftVersion());
                rowY += 16;
                String sync = cloud.autoSync()
                        ? PrimeLang.get("prime.gui.settings.cloud_sync.enabled", "Enabled")
                        : PrimeLang.get("prime.gui.settings.cloud_sync.disabled", "Disabled");
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.cloud_sync", "Local backup"), sync);
            }
            case APPEARANCE -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.active_theme", "Active theme"), themes.active().name());
                rowY += 18;
                drawThemeChip(ctx, theme, themes, "prime-crimson",
                        PrimeLang.get("prime.gui.settings.theme.crimson", "Crimson"),
                        x + 12, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-midnight",
                        PrimeLang.get("prime.gui.settings.theme.midnight", "Midnight"),
                        x + 118, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-aurora",
                        PrimeLang.get("prime.gui.settings.theme.aurora", "Aurora"),
                        x + 224, rowY, 100);
                rowY += 20;
                drawThemeChip(ctx, theme, themes, "prime-obsidian",
                        PrimeLang.get("prime.gui.settings.theme.obsidian", "Obsidian"),
                        x + 12, rowY, 100);
                drawThemeChip(ctx, theme, themes, "prime-ember",
                        PrimeLang.get("prime.gui.settings.theme.ember", "Ember"),
                        x + 118, rowY, 100);
            }
            case PERFORMANCE -> row(ctx, theme, x + 12, rowY,
                    PrimeLang.get("prime.gui.settings.row.tip", "Tip"),
                    PrimeLang.get("prime.gui.settings.tip.performance", "Use Performance Profiles module"));
            case CONTROLS -> renderControls(ctx, theme, keybinds, x, rowY, panelW, contentBottom);
            case ACCOUNT -> {
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.player", "Player"), adapter.playerName());
                rowY += 16;
                String type = adapter.sessionAccountType();
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.row.session", "Session"),
                        type == null || type.isBlank() ? "â€”" : type);
                rowY += 22;
                boolean canSwitch = !adapter.isInGame();
                boolean hover = canSwitch && mouseX >= x + 12 && mouseX < x + 168
                        && mouseY >= rowY && mouseY < rowY + 20;
                UiChrome.button(ctx, theme, x + 12, rowY, 156, 20, hover, canSwitch);
                String btn = PrimeLang.get("prime.gui.settings.account.switch", "Switch accountâ€¦");
                GuiLayout.label(ctx, btn, x + 20, rowY + 6,
                        canSwitch ? theme.foreground() : theme.foregroundMuted());
                rowY += 26;
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx,
                                canSwitch
                                        ? PrimeLang.get("prime.gui.settings.account.switch_hint",
                                        "Uses accounts from the StellarClient Launcher")
                                        : PrimeLang.get("prime.gui.settings.account.in_world",
                                        "Return to the title screen to switch accounts"),
                                panelW - 40),
                        x + 12, rowY, theme.foregroundMuted());
            }
            case PRIVACY -> row(ctx, theme, x + 12, rowY,
                    PrimeLang.get("prime.gui.settings.row.data", "Data"),
                    PrimeLang.get("prime.gui.settings.privacy.hint", "Configs stored locally only"));
            case ABOUT -> {
                PrimeLogo.draw(ctx, x + 12, rowY, 14, 0xFFFFFFFF);
                row(ctx, theme, x + 12 + PrimeLogo.widthForHeight(14) + 6, rowY + 2,
                        PrimeLang.get("prime.gui.settings.about.client", "StellarClient"),
                        PrimeLang.get("prime.gui.settings.about.version", "v%s", PrimeDesign.VERSION));
                rowY += 20;
                row(ctx, theme, x + 12, rowY,
                        PrimeLang.get("prime.gui.settings.about.legitimate", "Legitimate client"),
                        PrimeLang.get("prime.gui.settings.about.tagline", "Visual & QoL only"));
            }
        }
        ctx.popClip();

        String footer = active == Category.CONTROLS
                ? PrimeLang.get("prime.gui.settings.keybinds.footer",
                "Click key to rebind Â· Backspace clears Â· Scroll for more")
                : (search.isEmpty()
                ? PrimeLang.get("prime.gui.settings.search.placeholder", "Search settings...")
                : search.toString());
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, footer, panelW - 24),
                x + 12, y + panelH - 18, theme.foregroundMuted());
    }

    private void renderControls(RenderContext ctx, Theme theme, KeybindManager keybinds,
                                int x, int rowY, int panelW, int contentBottom) {
        List<Keybind> binds = filteredKeybinds(keybinds);
        int maxScroll = Math.max(0, binds.size() - visibleControlRows(contentBottom - rowY));
        controlsScroll = Math.min(controlsScroll, maxScroll);

        int drawY = rowY;
        int skipped = 0;
        for (Keybind bind : binds) {
            if (skipped++ < controlsScroll) {
                continue;
            }
            if (drawY + ROW_HEIGHT > contentBottom) {
                break;
            }
            boolean listening = bind == listeningFor;
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, bind.displayName(), 130),
                    x + 12, drawY + 3, theme.foreground());
            int btnX = x + panelW - KEY_BTN_W - 20;
            ctx.fillRoundedRect(btnX, drawY, KEY_BTN_W, 14, PrimeDesign.RADIUS_SM,
                    listening ? theme.accent() : theme.backgroundLight());
            String label = listening
                    ? PrimeLang.get("prime.gui.settings.keybinds.listening", "Press a keyâ€¦")
                    : KeyNames.glfwName(bind.key());
            int textColor = listening ? theme.background() : theme.foreground();
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, label, KEY_BTN_W - 8),
                    btnX + 4, drawY + 3, textColor);
            drawY += ROW_HEIGHT;
        }
    }

    public boolean mousePressed(RenderContext ctx, double mx, double my, int screenW, int screenH,
                                ThemeManager themes, KeybindManager keybinds, MinecraftAdapter adapter) {
        int panelW = settingsPanelW(screenW);
        int panelH = settingsPanelH(screenH);
        int x = (screenW - panelW) / 2;
        int y = (screenH - panelH) / 2;
        int tabY = y + 28;
        int tabX = x + 8;
        int tabsInRow = 0;
        for (Category cat : Category.values()) {
            if (!matchesSearch(cat.translated())) {
                continue;
            }
            int tw = GuiLayout.tabWidth(ctx, cat.translated(), 10);
            if (tabsInRow >= 4) {
                tabX = x + 8;
                tabY += 16;
                tabsInRow = 0;
            }
            if (mx >= tabX && mx < tabX + tw && my >= tabY && my < tabY + 14) {
                active = cat;
                controlsScroll = 0;
                listeningFor = null;
                return true;
            }
            tabX += tw + 4;
            tabsInRow++;
        }
        if (active == Category.APPEARANCE) {
            int rowY = tabY + 40;
            if (mx >= x + 12 && mx < x + 112 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-crimson");
                return true;
            }
            if (mx >= x + 118 && mx < x + 218 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-midnight");
                return true;
            }
            if (mx >= x + 224 && mx < x + 324 && my >= rowY && my < rowY + 16) {
                themes.setActive("prime-aurora");
                return true;
            }
            int row2 = rowY + 20;
            if (mx >= x + 12 && mx < x + 112 && my >= row2 && my < row2 + 16) {
                themes.setActive("prime-obsidian");
                return true;
            }
            if (mx >= x + 118 && mx < x + 218 && my >= row2 && my < row2 + 16) {
                themes.setActive("prime-ember");
                return true;
            }
        }
        if (active == Category.ACCOUNT && adapter != null && !adapter.isInGame()) {
            int rowY = tabY + 22 + 16 + 22;
            if (mx >= x + 12 && mx < x + 168 && my >= rowY && my < rowY + 20) {
                adapter.openAccountSwitcher();
                return true;
            }
        }
        if (active == Category.CONTROLS) {
            int rowY = tabY + 22;
            int contentBottom = y + panelH - 28;
            List<Keybind> binds = filteredKeybinds(keybinds);
            int btnX = x + panelW - KEY_BTN_W - 20;
            int drawY = rowY;
            int skipped = 0;
            for (Keybind bind : binds) {
                if (skipped++ < controlsScroll) {
                    continue;
                }
                if (drawY + ROW_HEIGHT > contentBottom) {
                    break;
                }
                if (mx >= btnX && mx < btnX + KEY_BTN_W && my >= drawY && my < drawY + 14) {
                    listeningFor = bind;
                    return true;
                }
                drawY += ROW_HEIGHT;
            }
        }
        return mx >= x && mx < x + panelW && my >= y && my < y + panelH;
    }

    public boolean charTyped(char c) {
        if (capturingKey()) {
            return true;
        }
        if (c < ' ') {
            return false;
        }
        search.append(c);
        controlsScroll = 0;
        return true;
    }

    public boolean keyPressed(int key) {
        if (capturingKey()) {
            return true;
        }
        if (key == 259 && !search.isEmpty()) {
            search.setLength(search.length() - 1);
            controlsScroll = 0;
            return true;
        }
        return false;
    }

    private List<Keybind> filteredKeybinds(KeybindManager keybinds) {
        List<Keybind> list = new ArrayList<>(keybinds.all());
        list.sort(Comparator.comparing(Keybind::category).thenComparing(Keybind::displayName));
        if (search.isEmpty()) {
            return list;
        }
        String needle = search.toString().toLowerCase();
        return list.stream()
                .filter(bind -> bind.displayName().toLowerCase().contains(needle)
                        || bind.category().toLowerCase().contains(needle)
                        || KeyNames.glfwName(bind.key()).toLowerCase().contains(needle))
                .toList();
    }

    private static int visibleControlRows(int clipHeight) {
        return Math.max(1, clipHeight / ROW_HEIGHT);
    }

    private boolean matchesSearch(String label) {
        if (search.isEmpty()) {
            return true;
        }
        return label.toLowerCase().contains(search.toString().toLowerCase());
    }

    private static void row(RenderContext ctx, Theme theme, int x, int y, String k, String v) {
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, k, 110), x, y, theme.foreground());
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, v, 180), x + 120, y, theme.foregroundMuted());
    }

    private static void drawThemeChip(RenderContext ctx, Theme theme, ThemeManager themes,
                                      String themeId, String label, int x, int y, int w) {
        boolean selected = themeId.equals(themes.active().id());
        ctx.fillRoundedRect(x, y, w, 16, PrimeDesign.RADIUS_SM,
                selected ? theme.accent() : theme.backgroundLight());
        GuiLayout.label(ctx, label, x + 6, y + 4,
                selected ? theme.foreground() : theme.foregroundMuted());
    }
}
