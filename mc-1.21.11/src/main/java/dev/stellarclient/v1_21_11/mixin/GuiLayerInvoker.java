package dev.stellarclient.v1_21_11.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes individual {@link Gui} layers without a full {@code Gui.render} pass. */
@Mixin(Gui.class)
public interface GuiLayerInvoker {

    @Invoker("renderBossOverlay")
    void StellarClient$renderBossOverlay(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderEffects")
    void StellarClient$renderEffects(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderHotbarAndDecorations")
    void StellarClient$renderHotbarAndDecorations(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderScoreboardSidebar")
    void StellarClient$renderScoreboardSidebar(GuiGraphics graphics, DeltaTracker deltaTracker);
}
