package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.gui.VanillaSkin;
import dev.valarisclient.v1_21_11.render.GuiRenderContext;
import dev.valarisclient.v1_21_11.screen.ValarisStyledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Paints the Valaris backdrop behind every vanilla screen we have not rebuilt.
 *
 * <p>Only the background is replaced -- widgets, layout and behaviour stay
 * vanilla, so world creation, Realms, reporting and error screens keep working
 * exactly as they do without the mod.</p>
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Unique
    private final GuiRenderContext valarisClient$ctx = new GuiRenderContext();

    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"), cancellable = true)
    private void valarisClient$skinMenuBackground(GuiGraphics graphics, CallbackInfo ci) {
        ValarisClient client = ValarisClient.getOrNull();
        if (client == null) {
            // Screens can render before the entrypoint finishes; leave those vanilla.
            return;
        }
        if (this instanceof ValarisStyledScreen) {
            // Bespoke screens (main menu, pause menu, ClickGUI, ...) paint their own look.
            return;
        }
        valarisClient$ctx.prepare(graphics);
        VanillaSkin.background(valarisClient$ctx, client.themes().active(),
                Minecraft.getInstance().level != null);
        ci.cancel();
    }
}
