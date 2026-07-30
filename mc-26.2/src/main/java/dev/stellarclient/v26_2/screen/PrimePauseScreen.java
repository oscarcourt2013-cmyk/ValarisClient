package dev.stellarclient.v26_2.screen;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.gui.menu.GameMenu;
import dev.stellarclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** StellarClient pause / game menu — replaces vanilla {@code PauseScreen}. */
public final class PrimePauseScreen extends Screen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final GameMenu gameMenu = new GameMenu(StellarClient.get().adapter());

    public PrimePauseScreen() {
        super(Component.translatable("menu.game"));
    }

    @Override
    protected void init() {
        gameMenu.resetFade();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void tick() {
        gameMenu.tick(1f / 20f);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        // Keep the world visible; GameMenu draws its own dark wash + embers.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        renderContext.prepare(extractor);
        gameMenu.render(renderContext, StellarClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (gameMenu.mousePressed(event.x(), event.y(), event.button(), width, height)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
