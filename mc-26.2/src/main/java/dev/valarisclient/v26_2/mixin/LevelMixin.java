package dev.valarisclient.v26_2.mixin;

import dev.valarisclient.core.hook.ValarisHooks;
import dev.valarisclient.core.state.AlwaysDayState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$noRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (ValarisHooks.noRainActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$noThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (ValarisHooks.noRainActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getDefaultClockTime", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$alwaysDayClock(CallbackInfoReturnable<Long> cir) {
        if (ValarisHooks.alwaysDayActive()) {
            cir.setReturnValue(AlwaysDayState.NOON_TICKS);
        }
    }

    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$alwaysDaySky(CallbackInfoReturnable<Integer> cir) {
        if (ValarisHooks.alwaysDayActive()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "isDarkOutside", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$alwaysBright(CallbackInfoReturnable<Boolean> cir) {
        if (ValarisHooks.alwaysDayActive()) {
            cir.setReturnValue(false);
        }
    }
}
