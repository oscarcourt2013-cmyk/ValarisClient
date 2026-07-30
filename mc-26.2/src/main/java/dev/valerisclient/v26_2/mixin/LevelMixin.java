package dev.valerisclient.v26_2.mixin;

import dev.valerisclient.core.hook.PrimeHooks;
import dev.valerisclient.core.state.AlwaysDayState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$noRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.noRainActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$noThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (PrimeHooks.noRainActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getDefaultClockTime", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$alwaysDayClock(CallbackInfoReturnable<Long> cir) {
        if (PrimeHooks.alwaysDayActive()) {
            cir.setReturnValue(AlwaysDayState.NOON_TICKS);
        }
    }

    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$alwaysDaySky(CallbackInfoReturnable<Integer> cir) {
        if (PrimeHooks.alwaysDayActive()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "isDarkOutside", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$alwaysBright(CallbackInfoReturnable<Boolean> cir) {
        if (PrimeHooks.alwaysDayActive()) {
            cir.setReturnValue(false);
        }
    }
}
