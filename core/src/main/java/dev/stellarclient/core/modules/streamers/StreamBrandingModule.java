package dev.stellarclient.core.modules.streamers;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.stream.StreamerPrivacyState;

import java.util.HashSet;
import java.util.Set;

/** Hides Prime branding HUD elements without touching the vanilla game HUD. */
public final class StreamBrandingModule extends Module {

    private final BooleanSetting hideWatermark =
            addSetting(new BooleanSetting(
                    "hide-watermark", "Hide watermark", "Hide the Prime watermark", true));
    private final BooleanSetting hidePrimeBranding =
            addSetting(new BooleanSetting(
                    "hide-prime-branding", "Hide Prime branding", "Hide Prime account branding", true));

    private final HudManager hud;
    private final Set<String> hiddenIds = new HashSet<>();
    private final Set<String> savedVisible = new HashSet<>();

    public StreamBrandingModule(HudManager hud) {
        super("stream-branding", "Stream Branding",
                "Hides Prime branding for a cleaner stream overlay", ModuleCategory.STREAMERS);
        this.hud = hud;
        listen(ClientTickEvent.class, event -> apply());
    }

    @Override
    protected void onEnable() {
        StreamerPrivacyState.setBrandingHide(true);
        apply();
    }

    @Override
    protected void onDisable() {
        restore();
        StreamerPrivacyState.setBrandingHide(false);
    }

    private void apply() {
        if (!isEnabled()) {
            return;
        }
        if (hideWatermark.get()) {
            hideElement("watermark");
        }
        if (hidePrimeBranding.get()) {
            hideElement("prime-account");
        }
    }

    private void hideElement(String id) {
        if (hiddenIds.contains(id)) {
            return;
        }
        HudElement element = hud.get(id);
        if (element != null && element.isVisible()) {
            savedVisible.add(id);
            element.setVisible(false);
        }
        hiddenIds.add(id);
    }

    private void restore() {
        for (String id : savedVisible) {
            HudElement element = hud.get(id);
            if (element != null) {
                element.setVisible(true);
            }
        }
        savedVisible.clear();
        hiddenIds.clear();
    }
}
