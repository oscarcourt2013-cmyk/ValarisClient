package dev.valarisclient.v26_2.screen;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.gui.clickgui.ClickGui;
import dev.valarisclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * ClickGUI screen for 26.2. Thin shell around the core {@link ClickGui},
 * rendering through the extract-based screen pipeline of 26.x.
 */
public final class ClickGuiScreen extends Screen {

    private static final int DIM_COLOR = 0x60000000;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public ClickGuiScreen() {
        super(Component.literal("Valaris ClickGUI"));
    }

    @Override
    protected void init() {
        ValarisClient.get().clickGui().onOpen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ValarisClient.get().clickGui().tick(1f / 20f);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        renderContext.prepare(extractor);
        renderContext.fillRect(0, 0, width, height, DIM_COLOR);
        ValarisClient.get().clickGui().render(renderContext, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (ValarisClient.get().clickGui().mousePressed(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        ValarisClient.get().clickGui().mouseDragged(event.x(), event.y(), width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ValarisClient.get().clickGui().mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ValarisClient.get().clickGui().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (ValarisClient.get().clickGui().charTyped((char) event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ValarisClient.get().clickGui().keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ValarisClient.get().profiles().saveActive();
        super.onClose();
    }
}
