package dev.valerisclient.v1_21_11.screen;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.social.SocialHubUi;
import dev.valerisclient.v1_21_11.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** In-game social hub shell for 1.21.11. */
public final class SocialHubScreen extends Screen implements ValerisStyledScreen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final SocialHubUi ui = new SocialHubUi(ValerisClient.get().social(), ValerisClient.get().adapter());
    private final Screen parent;

    public SocialHubScreen(Screen parent) {
        super(Component.literal("Social Hub"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ui.onOpen();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level != null) {
            super.renderBackground(graphics, mouseX, mouseY, delta);
        } else {
            renderPanorama(graphics, delta);
        }
        renderContext.prepare(graphics);
        ui.render(renderContext, ValerisClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (ui.mousePressed(event.x(), event.y(), event.button(), width, height)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ui.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ui.keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (ui.charTyped((char) event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
