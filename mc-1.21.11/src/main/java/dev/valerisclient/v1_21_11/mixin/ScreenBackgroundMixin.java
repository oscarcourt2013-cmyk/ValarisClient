package dev.valerisclient.v1_21_11.mixin;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.VanillaSkin;
import dev.valerisclient.v1_21_11.render.GuiRenderContext;
import dev.valerisclient.v1_21_11.screen.ValerisStyledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Paints the Valeris backdrop behind every vanilla screen we have not rebuilt.
 *
 * <p>Only the background is replaced -- widgets, layout and behaviour stay
 * vanilla, so world creation, Realms, reporting and error screens keep working
 * exactly as they do without the mod.</p>
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Unique
    private final GuiRenderContext valerisClient$ctx = new GuiRenderContext();

    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"), cancellable = true)
    private void valerisClient$skinMenuBackground(GuiGraphics graphics, CallbackInfo ci) {
        ValerisClient client = ValerisClient.getOrNull();
        if (client == null) {
            // Screens can render before the entrypoint finishes; leave those vanilla.
            return;
        }
        if (this instanceof ValerisStyledScreen) {
            // Bespoke screens (main menu, pause menu, ClickGUI, ...) paint their own look.
            return;
        }
        valerisClient$ctx.prepare(graphics);
        VanillaSkin.background(valerisClient$ctx, client.themes().active(),
                Minecraft.getInstance().level != null);
        ci.cancel();
    }
}
