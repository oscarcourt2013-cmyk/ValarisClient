package dev.valerisclient.core.modules.streamers;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.stream.StreamerPrivacyState;

import java.util.HashSet;
import java.util.Set;

/** Hides Prime branding HUD elements without touching the vanilla game HUD. */
public final class StreamBrandingModule extends Module {

    private final BooleanSetting hideWatermark =
            addSetting(new BooleanSetting(
                    "hide-watermark", "Hide watermark", "Hide the Valeris watermark", true));
    private final BooleanSetting hideValerisBranding =
            addSetting(new BooleanSetting(
                    "hide-prime-branding", "Hide Valeris branding", "Hide Valeris account branding", true));

    private final HudManager hud;
    private final Set<String> hiddenIds = new HashSet<>();
    private final Set<String> savedVisible = new HashSet<>();

    public StreamBrandingModule(HudManager hud) {
        super("stream-branding", "Stream Branding",
                "Hides Valeris branding for a cleaner stream overlay", ModuleCategory.STREAMERS);
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
        if (hideValerisBranding.get()) {
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
