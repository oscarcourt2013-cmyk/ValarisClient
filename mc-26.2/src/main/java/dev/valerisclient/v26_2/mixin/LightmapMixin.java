package dev.valerisclient.v26_2.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.valerisclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {

    @ModifyReturnValue(method = "getBrightness", at = @At("RETURN"))
    private static float ValerisClient$fullbrightDimension(float original, DimensionType dimensionType, int level) {
        return PrimeHooks.fullbrightActive() ? 1.0F : original;
    }
}
