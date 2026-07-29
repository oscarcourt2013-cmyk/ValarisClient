package dev.stellarclient.v1_21_11.mixin;

import dev.stellarclient.core.hook.PrimeHooks;
import dev.stellarclient.core.hud.editor.HudEditorState;
import dev.stellarclient.core.hud.vanilla.VanillaHudComponent;
import dev.stellarclient.core.hud.vanilla.VanillaHudMeasurements;
import dev.stellarclient.v1_21_11.hud.ScoreboardSidebarMetrics;
import dev.stellarclient.v1_21_11.hud.VanillaHudTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void StellarClient$deferHudWhileEditing(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HudEditorState.isActive() && !HudEditorState.isRenderingVanillaHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void StellarClient$handShaderWash(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!PrimeHooks.handShaderActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.getCameraType().isFirstPerson()) {
            int w = minecraft.getWindow().getGuiScaledWidth();
            int h = minecraft.getWindow().getGuiScaledHeight();
            int color = PrimeHooks.handShaderOverlayArgb();
            // Soft wash over the lower HUD where first-person hands sit.
            graphics.fill(0, (int) (h * 0.62f), w, h, color);
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void StellarClient$hideVanillaCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PrimeHooks.hideVanillaCrosshair()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
    private void StellarClient$hotbarHead(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(graphics, VanillaHudComponent.HOTBAR);
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
    private void StellarClient$hotbarTail(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(graphics, VanillaHudComponent.HOTBAR);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void StellarClient$effectsHead(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(graphics, VanillaHudComponent.STATUS_EFFECTS);
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void StellarClient$effectsTail(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(graphics, VanillaHudComponent.STATUS_EFFECTS);
    }

    @Inject(method = "renderBossOverlay", at = @At("HEAD"))
    private void StellarClient$bossHead(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(graphics, VanillaHudComponent.BOSSBAR);
    }

    @Inject(method = "renderBossOverlay", at = @At("RETURN"))
    private void StellarClient$bossTail(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(graphics, VanillaHudComponent.BOSSBAR);
    }

    @Inject(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void StellarClient$xpHead(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.push(graphics, VanillaHudComponent.EXPERIENCE);
    }

    @Inject(
            method = "renderHotbarAndDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void StellarClient$xpTail(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(graphics, VanillaHudComponent.EXPERIENCE);
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"))
    private void StellarClient$scoreboardHead(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // Bounds from the previous frame stay valid so the transform matches the editor box.
        VanillaHudTransforms.push(graphics, VanillaHudComponent.SCOREBOARD);
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("RETURN"))
    private void StellarClient$scoreboardTail(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudTransforms.pop(graphics, VanillaHudComponent.SCOREBOARD);
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
