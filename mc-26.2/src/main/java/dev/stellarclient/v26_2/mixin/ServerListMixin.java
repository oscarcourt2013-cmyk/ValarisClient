package dev.stellarclient.v26_2.mixin;

import dev.stellarclient.core.servers.PartnerServers;
import dev.stellarclient.v26_2.multiplayer.PartnerServerList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public abstract class ServerListMixin {

    @Inject(method = "load", at = @At("RETURN"))
    private void prime$injectPartners(CallbackInfo ci) {
        PartnerServerList.ensurePartners((ServerList) (Object) this);
    }

    /** Re-pin after user reorder / edit so partners stay at indices 0..n-1. */
    @Inject(method = {"swap", "replace", "add"}, at = @At("RETURN"))
    private void prime$repinPartners(CallbackInfo ci) {
        PartnerServerList.ensurePartners((ServerList) (Object) this);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void prime$blockPartnerRemove(ServerData data, CallbackInfo ci) {
        if (data != null && PartnerServers.isPartnerAddress(data.ip)) {
            ci.cancel();
        }
    }
}
