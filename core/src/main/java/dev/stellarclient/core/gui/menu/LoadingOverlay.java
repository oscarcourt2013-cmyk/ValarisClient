package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.design.PrimeLogo;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;
import dev.stellarclient.core.util.Easing;

/** Animated boot splash â€” plays once, then dismisses so gameplay is unobstructed. */
public final class LoadingOverlay {

    private enum Phase {
        ENTER, ACTIVE, HOLD, EXIT, DONE
    }

    private static final float ENTER_DURATION = 0.5f;
    private static final float HOLD_DURATION = 1.15f;
    private static final float EXIT_DURATION = 0.6f;

    private Phase phase = Phase.ENTER;
    private float phaseTime;
    private float enterProgress;
    private float exitProgress;
    private float displayProgress;
    private float targetProgress;
    private float pulsePhase;
    private float shimmerPhase;
    private boolean readyReached;
    private String stage = "Loading Core...";

    public void setStage(String stage, float progress) {
        this.stage = stage;
        this.targetProgress = Math.clamp(progress, 0f, 1f);
        if (targetProgress >= 1f) {
            readyReached = true;
        }
    }

    /** Skips hold and plays the exit animation (e.g. when entering a world). */
    public void requestDismiss() {
        if (phase == Phase.DONE || phase == Phase.EXIT) {
            return;
        }
        phase = Phase.EXIT;
        phaseTime = 0f;
    }

    public boolean visible() {
        return phase != Phase.DONE;
    }

    public boolean finished() {
        return phase == Phase.DONE;
    }

    public void tick(float deltaSeconds) {
        if (phase == Phase.DONE) {
            return;
        }

        pulsePhase += deltaSeconds * 2.8f;
        shimmerPhase += deltaSeconds * 3.2f;
        displayProgress = Easing.lerp(displayProgress, targetProgress, deltaSeconds * 4.5f);

        switch (phase) {
            case ENTER -> {
                phaseTime += deltaSeconds;
                enterProgress = Easing.easeOutBack(Math.min(1f, phaseTime / ENTER_DURATION));
                if (phaseTime >= ENTER_DURATION) {
                    phase = Phase.ACTIVE;
                    phaseTime = 0f;
                }
            }
            case ACTIVE -> {
                if (readyReached && displayProgress >= 0.985f) {
                    phase = Phase.HOLD;
                    phaseTime = 0f;
                }
            }
            case HOLD -> {
                phaseTime += deltaSeconds;
                if (phaseTime >= HOLD_DURATION) {
                    phase = Phase.EXIT;
                    phaseTime = 0f;
                }
            }
            case EXIT -> {
                phaseTime += deltaSeconds;
                exitProgress = Easing.easeInOutQuad(Math.min(1f, phaseTime / EXIT_DURATION));
                if (phaseTime >= EXIT_DURATION) {
                    phase = Phase.DONE;
                }
            }
            case DONE -> {
            }
        }
    }

    public void render(RenderContext ctx, Theme theme) {
        if (phase == Phase.DONE) {
            return;
        }

        float overlayAlpha = switch (phase) {
            case ENTER -> enterProgress;
            case ACTIVE, HOLD -> 1f;
            case EXIT -> 1f - exitProgress;
            case DONE -> 0f;
        };
        if (overlayAlpha <= 0.01f) {
            return;
        }

        int sw = ctx.screenWidth();
        int sh = ctx.screenHeight();
        int backdrop = ColorUtil.withAlpha(0xFF000000, 0.88f * overlayAlpha);
        ctx.fillRect(0, 0, sw, sh, backdrop);
        ctx.setDrawOpacity(overlayAlpha);
        ctx.fillGradientVertical(0, 0, sw, sh,
                ColorUtil.withAlpha(theme.gradientTop(), 0.35f),
                ColorUtil.withAlpha(theme.gradientBottom(), 0.55f));

        float logoScale = switch (phase) {
            case ENTER -> 0.65f + 0.35f * enterProgress;
            case HOLD -> 1f + 0.035f * (float) Math.sin(pulsePhase);
            case EXIT -> 1f + 0.12f * exitProgress;
            default -> 1f;
        };

        int baseLogoH = 40;
        int logoH = Math.round(baseLogoH * logoScale);
        int cy = sh / 2 - 34;
        int cx = sw / 2;

        if (phase == Phase.HOLD || phase == Phase.ACTIVE) {
            int glowSize = logoH + 24 + Math.round(6f * (float) Math.sin(pulsePhase));
            int glowX = cx - PrimeLogo.widthForHeight(glowSize) / 2 - 12;
            int glowY = cy - 8;
            ctx.fillRoundedRect(glowX, glowY,
                    PrimeLogo.widthForHeight(glowSize) + 24, glowSize + 16,
                    12, ColorUtil.withAlpha(theme.accent(), phase == Phase.HOLD ? 0.22f : 0.12f));
        }

        ctx.pushTransform(cx, cy + logoH / 2f, logoScale, 0f, 0f, -logoH / 2f);
        PrimeLogo.draw(ctx, -PrimeLogo.widthForHeight(baseLogoH) / 2, 0, baseLogoH, 0xFFFFFFFF);
        ctx.popTransform();

        float textAlpha = phase == Phase.ENTER
                ? Math.max(0f, (enterProgress - 0.35f) / 0.65f)
                : phase == Phase.EXIT ? 1f - exitProgress : 1f;
        ctx.setDrawOpacity(overlayAlpha * textAlpha);

        int stageW = ctx.textWidth(stage);
        ctx.drawText(stage, (sw - stageW) / 2, cy + logoH + 10, theme.foreground(), true);

        int barW = 200;
        int barH = 5;
        int barX = (sw - barW) / 2;
        int barY = cy + logoH + 28;
        int radius = barH / 2;
        ctx.fillRoundedRect(barX, barY, barW, barH, radius, ColorUtil.withAlpha(theme.backgroundLight(), 0.85f));

        int fillW = Math.max(radius * 2, Math.round(barW * displayProgress));
        if (fillW > 0) {
            ctx.fillRoundedRect(barX, barY, fillW, barH, radius, theme.accent());
            int shimmerW = 28;
            int shimmerX = barX + Math.round((barW - shimmerW) * ((shimmerPhase % 1.2f) / 1.2f));
            if (shimmerX + shimmerW > barX && shimmerX < barX + fillW) {
                int clipX = Math.max(barX, shimmerX);
                int clipW = Math.min(barX + fillW, shimmerX + shimmerW) - clipX;
                ctx.pushClip(clipX, barY, clipW, barH);
                ctx.fillGradientHorizontal(clipX, barY, clipW, barH,
                        ColorUtil.withAlpha(0xFFFFFFFF, 0f),
                        ColorUtil.withAlpha(0xFFFFFFFF, 0.35f));
                ctx.popClip();
            }
        }

        String version = PrimeLang.get("prime.gui.loading.version", "v%s", PrimeDesign.VERSION);
        String tagline = PrimeLang.get("prime.gui.loading.tagline", "Premium Minecraft Client");
        ctx.drawText(version, barX, barY + PrimeDesign.SPACE_MD + 2, theme.foregroundMuted(), true);
        ctx.drawText(tagline, barX, barY + PrimeDesign.SPACE_MD + 14, theme.foregroundMuted(), true);
        ctx.setDrawOpacity(1f);
    }
}
