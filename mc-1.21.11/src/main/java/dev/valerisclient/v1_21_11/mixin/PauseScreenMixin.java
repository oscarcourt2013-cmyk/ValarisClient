package dev.valerisclient.v1_21_11.mixin;

import dev.valerisclient.v1_21_11.screen.PrimePauseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {

    @Shadow
    @Final
    private boolean showPauseMenu;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$replacePauseMenu(CallbackInfo ci) {
        if (!this.showPauseMenu) {
            return;
        }
        Minecraft.getInstance().setScreen(new PrimePauseScreen());
        ci.cancel();
    }
}
