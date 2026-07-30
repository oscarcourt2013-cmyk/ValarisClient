package dev.valerisclient.v1_21_11.mixin;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.VanillaSkin;
import dev.valerisclient.v1_21_11.render.GuiRenderContext;
import dev.valerisclient.v1_21_11.screen.ValerisStyledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repaints every vanilla button with Valeris chrome.
 *
 * <p>{@code AbstractButton.renderWidget} is the single place Button, Checkbox and
 * friends draw their sprite, so one hook covers every un-rebuilt screen. Only
 * painting is replaced -- click handling, narration, tooltips and layout remain
 * vanilla.</p>
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {

    @Unique
    private final GuiRenderContext valerisClient$ctx = new GuiRenderContext();

    AbstractButtonMixin() {
        super(0, 0, 0, 0, null);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void valerisClient$skinButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                          CallbackInfo ci) {
        ValerisClient client = ValerisClient.getOrNull();
        if (client == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ValerisStyledScreen) {
            // Bespoke screens own their controls; never repaint widgets inside them.
            return;
        }
        var theme = client.themes().active();
        valerisClient$ctx.prepare(graphics);
        VanillaSkin.button(valerisClient$ctx, theme, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused(), this.active, this.alpha);

        // Vanilla drew the label inside renderWidget, so draw it here too.
        int color = VanillaSkin.buttonTextColor(theme, isHoveredOrFocused(), this.active);
        graphics.drawCenteredString(mc.font, getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                color);
        ci.cancel();
    }
}
