package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.theme.Theme;

/** In-game pause / game menu controller. */
public final class GameMenu {

    private final GameMenuRenderer renderer = new GameMenuRenderer();
    private final MinecraftAdapter adapter;
    private float fade;
    private float emberPhase;

    public GameMenu(MinecraftAdapter adapter) {
        this.adapter = adapter;
    }

    public void resetFade() {
        fade = 0f;
        emberPhase = 0f;
    }

    public void tick(float deltaSeconds) {
        emberPhase += deltaSeconds;
        if (fade < 1f) {
            fade = Math.min(1f, fade + deltaSeconds * 2.4f);
        }
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        renderer.render(ctx, theme, adapter, mouseX, mouseY, fade, emberPhase);
    }

    public boolean mousePressed(double mouseX, double mouseY, int button,
                                int screenWidth, int screenHeight) {
        if (button != 0 || fade < 0.25f) {
            return false;
        }
        GameMenuAction action = renderer.hitAction(mouseX, mouseY, screenWidth, screenHeight);
        if (action == null) {
            return false;
        }
        switch (action) {
            case BACK_TO_GAME -> adapter.closeCurrentScreen();
            case ADVANCEMENTS -> adapter.openAdvancements();
            case STATISTICS -> adapter.openStatistics();
            case GIVE_FEEDBACK -> adapter.openFeedbackLink();
            case REPORT_BUGS -> adapter.openBugReportLink();
            case OPTIONS -> adapter.openOptions();
            case OPEN_TO_LAN -> adapter.openShareToLan();
            case SAVE_AND_QUIT -> adapter.disconnectToTitle();
        }
        return true;
    }
}
