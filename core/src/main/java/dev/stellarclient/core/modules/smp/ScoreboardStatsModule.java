package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows scoreboard lines matching economy keywords on a HUD overlay. */
public final class ScoreboardStatsModule extends Module {

    private final StringSetting lineKeywords = addSetting(new StringSetting(
            "line-keywords", "Line keywords", "Show lines containing these words",
            "money,balance,coin,rank,shard,gem,token"));
    private final IntSetting maxLines = addSetting(new IntSetting(
            "max-lines", "Max lines", "Maximum scoreboard lines shown", 5, 1, 10));

    private final Element element;
    private final MinecraftAdapter adapter;

    public ScoreboardStatsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("scoreboard-stats", "Scoreboard Stats", "Economy lines from the sidebar scoreboard", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        List<String> matched = new ArrayList<>();
        String keywords = lineKeywords.get().toLowerCase(Locale.ROOT);
        for (int i = 0; i < adapter.scoreboardLineCount() && matched.size() < maxLines.get(); i++) {
            String line = adapter.scoreboardLine(i);
            if (matchesKeyword(line, keywords)) {
                matched.add(line);
            }
        }
        if (matched.isEmpty()) {
            String title = adapter.scoreboardTitle();
            element.setLines(title.isEmpty() ? List.of("No economy lines") : List.of(title));
        } else {
            element.setLines(matched);
        }
    }

    private static boolean matchesKeyword(String line, String commaKeywords) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        for (String part : commaKeywords.split(",")) {
            String word = part.trim();
            if (!word.isEmpty() && lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;
        private static final int LINE_GAP = 2;

        private final ThemeManager themes;
        private List<String> lines = List.of("No economy lines");

        Element(ThemeManager themes) {
            super("scoreboard-stats", "Scoreboard Stats", HudAnchor.MIDDLE_RIGHT, -4, -40);
            this.themes = themes;
        }

        void setLines(List<String> lines) {
            this.lines = lines;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            int max = 0;
            for (String line : lines) {
                max = Math.max(max, ctx.textWidth(line));
            }
            return max + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            int lineHeight = ctx.fontHeight();
            return PADDING * 2 + lines.size() * lineHeight + Math.max(0, lines.size() - 1) * LINE_GAP;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            int width = measureWidth(ctx);
            int height = measureHeight(ctx);
            ctx.fillRect(0, 0, width, height, theme.background());
            int y = PADDING;
            int lineHeight = ctx.fontHeight();
            for (String line : lines) {
                ctx.drawText(line, PADDING, y, theme.foreground(), true);
                y += lineHeight + LINE_GAP;
            }
        }
    }
}
