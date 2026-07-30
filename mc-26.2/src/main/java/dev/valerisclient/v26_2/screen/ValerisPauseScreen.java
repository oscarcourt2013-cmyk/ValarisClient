package dev.valerisclient.v26_2.screen;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.menu.GameMenu;
import dev.valerisclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** ValerisClient pause / game menu — replaces vanilla {@code PauseScreen}. */
public final class ValerisPauseScreen extends Screen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final GameMenu gameMenu = new GameMenu(ValerisClient.get().adapter());

    public ValerisPauseScreen() {
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
        gameMenu.render(renderContext, ValerisClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (gameMenu.mousePressed(event.x(), event.y(), event.button(), width, height)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
