package dev.valerisclient.v1_21_11.screen;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.menu.GameMenu;
import dev.valerisclient.v1_21_11.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** ValerisClient pause / game menu — replaces vanilla {@code PauseScreen}. */
public final class PrimePauseScreen extends Screen implements ValerisStyledScreen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final GameMenu gameMenu = new GameMenu(ValerisClient.get().adapter());

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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Keep the world visible; GameMenu draws its own dark wash + embers.
        if (minecraft != null && minecraft.level != null) {
            // Intentionally skip blur/panorama so the desaturated world stays readable.
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderContext.prepare(graphics);
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
