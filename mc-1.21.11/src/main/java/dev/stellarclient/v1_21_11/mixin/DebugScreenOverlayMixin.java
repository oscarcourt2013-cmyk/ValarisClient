package dev.stellarclient.v1_21_11.mixin;

import dev.stellarclient.core.hook.PrimeHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void StellarClient$streamSafeDebug(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!PrimeHooks.streamDebugShield()) {
            return;
        }
        ci.cancel();

        int y = 2;
        guiGraphics.drawString(font, "FPS: " + minecraft.getFps(), 2, y, 0xE0E0E0, false);
        y += font.lineHeight;

        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long maxMb = runtime.maxMemory() / (1024L * 1024L);
        guiGraphics.drawString(font, "Mem: " + usedMb + "/" + maxMb + " MB", 2, y, 0xE0E0E0, false);
        y += font.lineHeight;
        guiGraphics.drawString(font, "Â§7Stream Safe â€” coords hidden", 2, y, 0xE0E0E0, false);
    }
}
