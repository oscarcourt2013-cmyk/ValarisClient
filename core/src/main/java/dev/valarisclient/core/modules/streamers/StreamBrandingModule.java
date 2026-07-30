package dev.valarisclient.core.modules.streamers;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.stream.StreamerPrivacyState;

import java.util.HashSet;
import java.util.Set;

/** Hides Valaris branding HUD elements without touching the vanilla game HUD. */
public final class StreamBrandingModule extends Module {

    private final BooleanSetting hideWatermark =
            addSetting(new BooleanSetting(
                    "hide-watermark", "Hide watermark", "Hide the Valaris watermark", true));
    private final BooleanSetting hideValarisBranding =
            addSetting(new BooleanSetting(
                    "hide-valaris-branding", "Hide Valaris branding", "Hide Valaris account branding", true));

    private final HudManager hud;
    private final Set<String> hiddenIds = new HashSet<>();
    private final Set<String> savedVisible = new HashSet<>();

    public StreamBrandingModule(HudManager hud) {
        super("stream-branding", "Stream Branding",
                "Hides Valaris branding for a cleaner stream overlay", ModuleCategory.STREAMERS);
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
        if (hideValarisBranding.get()) {
            hideElement("valaris-account");
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
