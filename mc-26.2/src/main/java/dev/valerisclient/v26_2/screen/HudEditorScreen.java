package dev.valerisclient.v26_2.screen;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.hud.editor.HudEditorState;
import dev.valerisclient.v26_2.render.GuiRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * HUD editor screen for 26.2. Flat dim backdrop — no live world/blur/vanilla GUI pass.
 */
public final class HudEditorScreen extends Screen {

    private static final int WORLD_DIM = 0xB0101010;
    private static final int MENU_DIM = 0xE0101010;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public HudEditorScreen() {
        super(Component.literal("Valeris HUD Editor"));
    }

    @Override
    protected void init() {
        HudEditorState.setActive(true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        // Skip blurred world pass — HUD editor only needs a flat backdrop.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        ValerisClient client = ValerisClient.get();
        renderContext.prepare(extractor);
        int dim = minecraft.level != null ? WORLD_DIM : MENU_DIM;
        renderContext.fillRect(0, 0, width, height, dim);
        // One layout pass (incl. hidden for hit-testing), then draw without re-measuring.
        client.hud().layout(renderContext, true);
        client.hud().render(renderContext, false);
        client.hudEditor().tickAutosave();
        client.hudEditor().renderOverlay(renderContext, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && ValerisClient.get().hudEditor().mousePressed(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0) {
            ValerisClient.get().hudEditor().mouseDragged(event.x(), event.y(), width, height);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ValerisClient.get().hudEditor().mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ValerisClient.get().hudEditor().mouseScrolled(
                mouseX, mouseY, verticalAmount, isShiftDown(), isControlDown())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ValerisClient.get().hudEditor().keyPressed(event.key(), event.hasShiftDown(), event.hasControlDown())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        HudEditorState.setActive(false);
        ValerisClient.get().hudEditor().flushAutosave();
        ValerisClient.get().profiles().saveActive();
        super.onClose();
    }

    private static boolean isShiftDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isControlDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
}
