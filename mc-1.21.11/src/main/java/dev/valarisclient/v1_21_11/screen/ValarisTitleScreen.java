package dev.valarisclient.v1_21_11.screen;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.gui.menu.TitleMenu;
import dev.valarisclient.v1_21_11.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** ValarisClient title screen — replaces vanilla {@code TitleScreen}. */
public final class ValarisTitleScreen extends Screen implements ValarisStyledScreen {

    private static final int OVERLAY = 0x28000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final TitleMenu titleMenu = new TitleMenu(ValarisClient.get().adapter());

    public ValarisTitleScreen() {
        super(Component.literal("ValarisClient"));
    }

    @Override
    protected void init() {
        titleMenu.resetFade();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        titleMenu.tick(1f / 20f);
    }

    @Override
    public boolean panoramaShouldSpin() {
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Skip default menu backdrop — panorama is drawn in render() like AccountSwitcher.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderPanorama(graphics, delta);
        renderContext.prepare(graphics);
        renderContext.fillRect(0, 0, width, height, OVERLAY);
        titleMenu.render(renderContext, ValarisClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (titleMenu.mousePressed(event.x(), event.y(), event.button(), width, height)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
