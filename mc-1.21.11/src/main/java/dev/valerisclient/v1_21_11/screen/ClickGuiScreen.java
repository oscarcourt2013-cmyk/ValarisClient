package dev.valerisclient.v1_21_11.screen;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.clickgui.ClickGui;
import dev.valerisclient.v1_21_11.render.GuiRenderContext;
import dev.valerisclient.core.gui.BlurBackdrop;
import dev.valerisclient.v1_21_11.render.PanelBlur;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * ClickGUI screen for 1.21.11. Thin shell around the core {@link ClickGui}.
 */
public final class ClickGuiScreen extends Screen implements ValerisStyledScreen {

    private static final int DIM_COLOR = 0x60000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public ClickGuiScreen() {
        super(Component.literal("Valeris ClickGUI"));
    }

    @Override
    protected void init() {
        ValerisClient.get().clickGui().onOpen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ValerisClient.get().clickGui().tick(1f / 20f);
    }

    @Override
    public boolean panoramaShouldSpin() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null) {
            // In-world: dim overlay only — MC 1.21.11 allows one blur call per frame.
            BlurBackdrop.setActive(false);
        } else {
            renderPanorama(graphics, delta);
            BlurBackdrop.setActive(false);
        }
        renderContext.prepare(graphics);
        int dim = minecraft.level != null ? DIM_COLOR : 0x28000000;
        renderContext.fillRect(0, 0, width, height, dim);
        ValerisClient.get().clickGui().render(renderContext, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Skip default blurred background — we draw our own backdrop in render().
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (ValerisClient.get().clickGui().mousePressed(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        ValerisClient.get().clickGui().mouseDragged(event.x(), event.y(), width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ValerisClient.get().clickGui().mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ValerisClient.get().clickGui().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (ValerisClient.get().clickGui().charTyped((char) event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ValerisClient.get().clickGui().keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        PanelBlur.end(minecraft);
        ValerisClient.get().profiles().saveActive();
        super.onClose();
    }
}
