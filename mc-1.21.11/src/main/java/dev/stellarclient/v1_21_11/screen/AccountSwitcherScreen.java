package dev.stellarclient.v1_21_11.screen;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.gui.account.AccountSwitcherUi;
import dev.stellarclient.v1_21_11.render.GuiRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Title-menu account switcher shell for 1.21.11. */
public final class AccountSwitcherScreen extends Screen {

    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final AccountSwitcherUi ui = new AccountSwitcherUi(StellarClient.get().adapter());
    private final Screen parent;

    public AccountSwitcherScreen(Screen parent) {
        super(Component.literal("Switch Account"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean panoramaShouldSpin() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderPanorama(graphics, delta);
        renderContext.prepare(graphics);
        ui.render(renderContext, StellarClient.get().themes().active(), mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
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
    public boolean charTyped(CharacterEvent event) {
        if (ui.charTyped((char) event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ui.keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent != null ? parent : new PrimeTitleScreen());
        }
    }
}
