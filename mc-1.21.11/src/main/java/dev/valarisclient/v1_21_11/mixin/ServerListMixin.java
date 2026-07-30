package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.core.servers.PartnerServers;
import dev.valarisclient.v1_21_11.multiplayer.PartnerServerList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public abstract class ServerListMixin {

    @Inject(method = "load", at = @At("RETURN"))
    private void valaris$injectPartners(CallbackInfo ci) {
        PartnerServerList.ensurePartners((ServerList) (Object) this);
    }

    /** Re-pin after user reorder / edit so partners stay at indices 0..n-1. */
    @Inject(method = {"swap", "replace", "add"}, at = @At("RETURN"))
    private void valaris$repinPartners(CallbackInfo ci) {
        PartnerServerList.ensurePartners((ServerList) (Object) this);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void valaris$blockPartnerRemove(ServerData data, CallbackInfo ci) {
        if (data != null && PartnerServers.isPartnerAddress(data.ip)) {
            ci.cancel();
        }
    }
}
