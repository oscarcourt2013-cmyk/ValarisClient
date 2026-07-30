package dev.stellarclient.v26_2.mixin;

import dev.stellarclient.core.hook.PrimeHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Shadow @Final private Font font;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void StellarClient$streamSafeDebug(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!PrimeHooks.streamDebugShield()) {
            return;
        }
        ci.cancel();

        int y = 2;
        graphics.text(font, "FPS: " + minecraft.getFps(), 2, y, 0xFFE0E0E0, false);
        y += font.lineHeight;

        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMb = runtime.maxMemory() / (1024L * 1024L);
        graphics.text(font, "Mem: " + usedMb + "/" + maxMb + " MB", 2, y, 0xFFE0E0E0, false);
        y += font.lineHeight;
        graphics.text(font, "Stream Safe — coords hidden", 2, y, 0xFFE0E0E0, false);
    }
}
