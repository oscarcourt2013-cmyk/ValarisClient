package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.v1_21_11.screen.ValarisOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes every path into vanilla Options -- title screen, pause menu, keybind --
 * to the Valaris-styled screen, matching how the title and pause menus are
 * replaced.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    /** Whatever opened Options; our screen must return there on Done. */
    @Shadow
    @Final
    private Screen lastScreen;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$replaceOptions(CallbackInfo ci) {
        Minecraft.getInstance().setScreen(new ValarisOptionsScreen(this.lastScreen));
        ci.cancel();
    }
}
