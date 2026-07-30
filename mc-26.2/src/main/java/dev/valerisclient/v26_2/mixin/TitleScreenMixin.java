package dev.valerisclient.v26_2.mixin;

import dev.valerisclient.core.gui.menu.TitleScreenGate;
import dev.valerisclient.v26_2.screen.ValerisTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$replaceTitleMenu(CallbackInfo ci) {
        if (TitleScreenGate.consumeAllowVanilla()) {
            return;
        }
        Minecraft.getInstance().gui.setScreen(new ValerisTitleScreen());
        ci.cancel();
    }
}
