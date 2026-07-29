package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.theme.Theme;
public final class TitleMenu {

    private final TitleMenuRenderer renderer = new TitleMenuRenderer();
    private final MinecraftAdapter adapter;
    private float fade;

    public TitleMenu(MinecraftAdapter adapter) {
        this.adapter = adapter;
    }

    public void resetFade() {
        fade = 0f;
    }

    public void tick(float deltaSeconds) {
        renderer.tick(deltaSeconds);
        if (fade < 1f) {
            fade = Math.min(1f, fade + deltaSeconds * 1.8f);
        }
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        renderer.render(ctx, theme, mouseX, mouseY, adapter, fade);
    }

    public boolean mousePressed(double mouseX, double mouseY, int button,
                                int screenWidth, int screenHeight) {
        if (button != 0 || fade < 0.35f) {
            return false;
        }

        TitleMenuTopBar.Action top = TitleMenuTopBar.hitAction(
                mouseX, mouseY, screenWidth, adapter.playerName(), adapter.sessionAccountType());
        if (top != null) {
            switch (top) {
                case DISCORD -> adapter.openExternalLink(TitleMenuTopBar.discordUrl());
                case VANILLA -> adapter.openVanillaTitleScreen();
                case SETTINGS -> adapter.openPrimeSettings();
                case PROFILE -> adapter.openAccountSwitcher();
            }
            return true;
        }

        TitleMenuAction action = renderer.hitAction(mouseX, mouseY, screenWidth, screenHeight);
        if (action == null) {
            return false;
        }
        switch (action) {
            case SINGLEPLAYER -> adapter.openSingleplayer();
            case MULTIPLAYER -> adapter.openMultiplayer();
            case PRIME_CLIENT -> adapter.openClickGui();
            case OPTIONS -> adapter.openOptions();
            case QUIT -> adapter.quitGame();
        }
        return true;
    }
}
