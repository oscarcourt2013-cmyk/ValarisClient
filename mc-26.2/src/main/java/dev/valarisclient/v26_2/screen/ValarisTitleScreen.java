package dev.valarisclient.v26_2.screen;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.gui.menu.TitleMenu;
import dev.valarisclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** ValarisClient title screen — replaces vanilla {@code TitleScreen}. */
public final class ValarisTitleScreen extends Screen {

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
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        // Skip default backdrop — panorama is drawn in extractRenderState.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractPanorama(extractor, delta);
        renderContext.prepare(extractor);
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
