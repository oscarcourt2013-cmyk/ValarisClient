package dev.stellarclient.v26_2.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.stellarclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FOV zoom is handled by {@link CameraMixin} on 26.2 ({@code GameRenderer.getFov} was removed).
 * This mixin only suppresses boss-bar world darkening when fullbright is active.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(method = "bossOverlayWorldDarkening", at = @At("RETURN"))
    private float StellarClient$noWorldDarkening(float amount) {
        return PrimeHooks.fullbrightActive() ? 0.0F : amount;
    }
}
