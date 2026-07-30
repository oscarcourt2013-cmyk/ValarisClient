package dev.valerisclient.v1_21_11.screen;

/**
 * Marks a screen that already paints its own Valeris look and must therefore be
 * skipped by the global vanilla-skinning mixins.
 *
 * <p>The main menu, pause menu and ClickGUI are bespoke designs; the global skin
 * exists to brand the screens we have <em>not</em> rebuilt. Without this marker
 * the exclusion would only hold by accident -- these screens happen to override
 * {@code renderBackground} and to draw their own controls rather than using
 * vanilla widgets. Adding one vanilla button to any of them would otherwise let
 * the global skin bleed into a hand-designed layout.</p>
 */
public interface ValerisStyledScreen {
}
