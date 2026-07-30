package dev.valerisclient.v26_2.mixin;

import dev.valerisclient.core.ValerisClient;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public final class ClientBrandRetrieverMixin {

    private ClientBrandRetrieverMixin() {
    }

    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true)
    private static void ValerisClient$clientBrand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ValerisClient.NAME);
    }
}
