package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.core.ValarisClient;
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
    private static void ValarisClient$clientBrand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ValarisClient.NAME);
    }
}
