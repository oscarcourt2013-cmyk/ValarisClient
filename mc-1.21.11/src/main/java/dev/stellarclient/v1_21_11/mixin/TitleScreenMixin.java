package dev.stellarclient.v1_21_11.mixin;

import dev.stellarclient.core.gui.menu.TitleScreenGate;
import dev.stellarclient.v1_21_11.screen.PrimeTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void StellarClient$replaceTitleMenu(CallbackInfo ci) {
        if (TitleScreenGate.consumeAllowVanilla()) {
            return;
        }
        Minecraft.getInstance().setScreen(new PrimeTitleScreen());
        ci.cancel();
    }
}
