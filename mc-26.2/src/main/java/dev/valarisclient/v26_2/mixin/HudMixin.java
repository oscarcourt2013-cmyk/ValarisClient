package dev.valarisclient.v26_2.mixin;

import dev.valarisclient.core.hook.ValarisHooks;
import dev.valarisclient.core.hud.editor.HudEditorState;
import dev.valarisclient.core.hud.vanilla.VanillaHudComponent;
import dev.valarisclient.core.hud.vanilla.VanillaHudMeasurements;
import dev.valarisclient.v26_2.hud.ScoreboardSidebarMetrics;
import dev.valarisclient.v26_2.hud.VanillaHudTransforms;
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
    private void ValarisClient$deferHudWhileEditing(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HudEditorState.isActive() && !HudEditorState.isRenderingVanillaHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$hideVanillaCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ValarisHooks.hideVanillaCrosshair()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
    private void ValarisClient$hotbarHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(extractor, VanillaHudComponent.HOTBAR);
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void ValarisClient$hotbarTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.HOTBAR);
    }

    @Inject(method = "extractEffects", at = @At("HEAD"))
    private void ValarisClient$effectsHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(extractor, VanillaHudComponent.STATUS_EFFECTS);
    }

    @Inject(method = "extractEffects", at = @At("RETURN"))
    private void ValarisClient$effectsTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.STATUS_EFFECTS);
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"))
    private void ValarisClient$bossHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(extractor, VanillaHudComponent.BOSSBAR);
    }

    @Inject(method = "extractBossOverlay", at = @At("RETURN"))
    private void ValarisClient$bossTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(extractor, VanillaHudComponent.BOSSBAR);
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"))
    private void ValarisClient$scoreboardHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        // Bounds from the previous frame stay valid so the transform matches the editor box.
        VanillaHudTransforms.push(extractor, VanillaHudComponent.SCOREBOARD);
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("RETURN"))
    private void ValarisClient$scoreboardTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
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
