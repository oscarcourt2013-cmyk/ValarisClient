package dev.valarisclient.v1_21_11.screen;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.hud.editor.HudEditorState;
import dev.valarisclient.v1_21_11.hud.VanillaHudLayerRenderer;
import dev.valarisclient.v1_21_11.render.GuiRenderContext;
import dev.valarisclient.v1_21_11.render.PanelBlur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD editor: flat dim backdrop (no world/blur) + lightweight vanilla HUD layers only.
 */
public final class HudEditorScreen extends Screen {

    /** Dim over flat backdrop — vanilla HUD draws on top. */
    private static final int WORLD_DIM = 0x68000000;
    /** Title / no level: same flat backdrop. */
    private static final int MENU_DIM = 0xE0101010;

    private final GuiRenderContext renderContext = new GuiRenderContext();

    public HudEditorScreen() {
        super(Component.literal("Valaris HUD Editor"));
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Intentionally empty — super renders the 3D world + blur every frame (~10 FPS on servers).
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        ValarisClient client = ValarisClient.get();
        renderContext.prepare(graphics);
        int dim = minecraft.level != null ? WORLD_DIM : MENU_DIM;
        renderContext.fillRect(0, 0, width, height, dim);

        if (minecraft.level != null && minecraft.player != null) {
            HudEditorState.runVanillaHudRender(
                    () -> VanillaHudLayerRenderer.renderVisibleLayers(graphics, minecraft.getDeltaTracker()));
        }

        // One layout pass (incl. hidden for hit-testing), then draw without re-measuring.
        client.hud().layout(renderContext, true);
        client.hud().render(renderContext, false);
        client.hudEditor().tickAutosave();
        client.hudEditor().renderOverlay(renderContext, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && ValarisClient.get().hudEditor().mousePressed(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0) {
            ValarisClient.get().hudEditor().mouseDragged(event.x(), event.y(), width, height);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        ValarisClient.get().hudEditor().mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ValarisClient.get().hudEditor().mouseScrolled(
                mouseX, mouseY, verticalAmount,
                Minecraft.getInstance().hasShiftDown(),
                Minecraft.getInstance().hasControlDown())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ValarisClient.get().hudEditor().keyPressed(event.key(), event.hasShiftDown(), event.hasControlDown())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        HudEditorState.setActive(false);
        PanelBlur.end(minecraft);
        ValarisClient.get().hudEditor().flushAutosave();
        ValarisClient.get().profiles().saveActive();
        super.onClose();
    }
}
