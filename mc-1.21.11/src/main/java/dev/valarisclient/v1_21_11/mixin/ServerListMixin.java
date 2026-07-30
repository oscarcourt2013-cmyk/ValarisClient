package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.v1_21_11.multiplayer.PartnerServerList;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public abstract class ServerListMixin {

    /**
     * Offer new partners once and clear entries older builds pinned here.
     * Only hooks load: no re-pinning, and deletion is never blocked.
     */
    @Inject(method = "load", at = @At("RETURN"))
    private void valaris$syncPartners(CallbackInfo ci) {
        PartnerServerList.sync((ServerList) (Object) this);
    }
}
