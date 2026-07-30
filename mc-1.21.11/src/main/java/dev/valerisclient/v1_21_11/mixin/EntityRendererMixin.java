package dev.valerisclient.v1_21_11.mixin;

import dev.valerisclient.core.hook.PrimeHooks;
import dev.valerisclient.core.stream.StreamNameMask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void ValerisClient$maskPlayerNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (!PrimeHooks.streamNameMask() || cir.getReturnValue() == null || !(entity instanceof Player)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player && !PrimeHooks.streamNameMaskSelf()) {
            return;
        }
        String masked = StreamNameMask.maskPlayerName(cir.getReturnValue().getString());
        cir.setReturnValue(Component.literal(masked));
    }
}
