package dev.valerisclient.v1_21_11.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes individual {@link Gui} layers without a full {@code Gui.render} pass. */
@Mixin(Gui.class)
public interface GuiLayerInvoker {

    @Invoker("renderBossOverlay")
    void ValerisClient$renderBossOverlay(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderEffects")
    void ValerisClient$renderEffects(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderHotbarAndDecorations")
    void ValerisClient$renderHotbarAndDecorations(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderScoreboardSidebar")
    void ValerisClient$renderScoreboardSidebar(GuiGraphics graphics, DeltaTracker deltaTracker);
}
