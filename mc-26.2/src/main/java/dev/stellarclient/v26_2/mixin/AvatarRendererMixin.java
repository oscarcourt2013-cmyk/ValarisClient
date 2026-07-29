package dev.stellarclient.v26_2.mixin;

import dev.stellarclient.core.cosmetics.CapePhysics;
import dev.stellarclient.core.cosmetics.CosmeticLoadout;
import dev.stellarclient.core.state.CapePhysicsState;
import dev.stellarclient.core.state.CosmeticsState;
import dev.stellarclient.v26_2.render.PrimeCosmeticRenderData;
import dev.stellarclient.v26_2.render.PrimeWingsLayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void StellarClient$addWingsLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).StellarClient$addLayer(new PrimeWingsLayer(
                (AvatarRenderer<?>) (Object) this, context.getModelSet()));
    }

    @Inject(method = "extractCapeState", at = @At("RETURN"))
    private void StellarClient$primeCape(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        boolean local = entity instanceof LocalPlayer;
        CosmeticLoadout loadout = CosmeticsState.loadoutFor(entity.getUUID(), local);
        if (state instanceof PrimeCosmeticRenderData data) {
            data.StellarClient$setCapeId(loadout.capeId());
            data.StellarClient$setWingsId(loadout.wingsId());
        }
        if (loadout.hasCape()) {
            state.showCape = true;
            StellarClient$applyCapePhysics(entity, state);
        }
    }

    private static void StellarClient$applyCapePhysics(Avatar entity, AvatarRenderState state) {
        if (!CapePhysicsState.active()) {
            return;
        }
        Vec3 velocity = entity.getDeltaMovement();
        float horizontal = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        CapePhysics.Pose pose = CapePhysics.smooth(
                entity.getUUID(),
                state.capeFlap,
                state.capeLean,
                state.capeLean2,
                new CapePhysics.Motion(
                        horizontal,
                        (float) velocity.y,
                        entity.onGround(),
                        entity.isSprinting(),
                        state.ageInTicks),
                CapePhysicsState.intensity());
        state.capeFlap = pose.flap();
        state.capeLean = pose.lean();
        state.capeLean2 = pose.lean2();
    }
}
