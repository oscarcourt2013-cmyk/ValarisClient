package dev.valerisclient.v26_2.mixin;

import dev.valerisclient.core.hook.PrimeHooks;
import dev.valerisclient.core.hud.editor.HudEditorState;
import dev.valerisclient.core.hud.vanilla.VanillaHudComponent;
import dev.valerisclient.core.hud.vanilla.VanillaHudMeasurements;
import dev.valerisclient.v26_2.hud.ScoreboardSidebarMetrics;
import dev.valerisclient.v26_2.hud.VanillaHudTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$deferHudWhileEditing(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HudEditorState.isActive() && !HudEditorState.isRenderingVanillaHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$hideVanillaCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PrimeHooks.hideVanillaCrosshair()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void ValerisClient$hotbarHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(extractor, VanillaHudComponent.HOTBAR);
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void ValerisClient$hotbarTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.HOTBAR);
    }

    @Inject(method = "extractEffects", at = @At("HEAD"))
    private void ValerisClient$effectsHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(extractor, VanillaHudComponent.STATUS_EFFECTS);
    }

    @Inject(method = "extractEffects", at = @At("RETURN"))
    private void ValerisClient$effectsTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.STATUS_EFFECTS);
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"))
    private void ValerisClient$bossHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(extractor, VanillaHudComponent.BOSSBAR);
    }

    @Inject(method = "extractBossOverlay", at = @At("RETURN"))
    private void ValerisClient$bossTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.BOSSBAR);
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"))
    private void ValerisClient$scoreboardHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        // Bounds from the previous frame stay valid so the transform matches the editor box.
        VanillaHudTransforms.push(extractor, VanillaHudComponent.SCOREBOARD);
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("RETURN"))
    private void ValerisClient$scoreboardTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.SCOREBOARD);
        if (!VanillaHudMeasurements.consumeScoreboardCaptureCommitted()) {
            // Sidebar drew nothing this frame: recompute (or clear) so bounds never go stale.
            Minecraft minecraft = Minecraft.getInstance();
            ScoreboardSidebarMetrics.update(
                    minecraft,
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
        }
    }
}
