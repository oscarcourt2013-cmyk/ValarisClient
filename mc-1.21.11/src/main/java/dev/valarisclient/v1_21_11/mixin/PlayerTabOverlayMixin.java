package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.core.hook.ValarisHooks;
import dev.valarisclient.v1_21_11.render.ValarisTabBadgeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Unique
    private long ValarisClient$lastTabRenderMs;
    @Unique
    private long ValarisClient$tabOpenStartMs;
    @Unique
    private boolean ValarisClient$tabPosePushed;

    @Inject(method = "decorateName", at = @At("RETURN"), cancellable = true)
    private void ValarisClient$reserveBadgeSpace(PlayerInfo playerInfo, MutableComponent name,
                                               CallbackInfoReturnable<Component> cir) {
        if (!ValarisHooks.clientBadgeActive() || playerInfo == null || cir.getReturnValue() == null) {
            return;
        }
        if (!ValarisHooks.isValarisPlayer(playerInfo.getProfile().id())) {
            return;
        }
        int spaceWidth = Math.max(1, Minecraft.getInstance().font.width(" "));
        int spaces = Math.max(1, (ValarisTabBadgeRenderer.width() + 2 + spaceWidth - 1) / spaceWidth);
        cir.setReturnValue(Component.literal(" ".repeat(spaces)).append(cir.getReturnValue()));
    }

    @Inject(method = "renderPingIcon", at = @At("HEAD"))
    private void ValarisClient$renderBadge(GuiGraphics graphics, int width, int x, int y,
                                         PlayerInfo playerInfo, CallbackInfo ci) {
        if (!ValarisHooks.clientBadgeActive() || playerInfo == null) {
            return;
        }
        if (!ValarisHooks.isValarisPlayer(playerInfo.getProfile().id())) {
            return;
        }
        ValarisTabBadgeRenderer.draw(graphics, x + 9, y);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ValarisClient$tabAnimBegin(GuiGraphics graphics, int width, Scoreboard scoreboard,
                                          Objective objective, CallbackInfo ci) {
        ValarisClient$tabPosePushed = false;
        if (!ValarisHooks.tabAnimationActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - ValarisClient$lastTabRenderMs > 120L) {
            ValarisClient$tabOpenStartMs = now;
        }
        ValarisClient$lastTabRenderMs = now;
        int duration = Math.max(1, ValarisHooks.tabAnimationDurationMs());
        float t = Math.min(1f, (now - ValarisClient$tabOpenStartMs) / (float) duration);
        float ease = t * t * (3f - 2f * t);
        float slide = (1f - ease) * -ValarisHooks.tabAnimationSlidePixels();
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(0f, slide);
        ValarisClient$tabPosePushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ValarisClient$tabAnimEnd(GuiGraphics graphics, int width, Scoreboard scoreboard,
                                        Objective objective, CallbackInfo ci) {
        if (ValarisClient$tabPosePushed) {
            graphics.pose().popMatrix();
            ValarisClient$tabPosePushed = false;
        }
    }
}
