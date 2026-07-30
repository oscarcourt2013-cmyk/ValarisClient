package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/**
 * Fuse countdown for nearby primed TNT.
 *
 * <p>Shows a placeholder until the version adapter can enumerate entities.</p>
 */
public final class TntTimerModule extends Module {

    private static final String UNAVAILABLE = "TNT: waiting for entities…";

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;
    private String lastText = "";

    public TntTimerModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("tnt-timer", "TNT Timer",
                "Fuse timer for nearby primed TNT (activates when entity access is available)",
                ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "tnt-timer", "TNT Timer", themes, HudAnchor.TOP_CENTER, 0, 36));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        int count = adapter.nearbyPrimedTntCount();
        float fuse = adapter.nearestPrimedTntFuseSeconds();
        String text;
        if (count < 0) {
            text = UNAVAILABLE;
        } else if (count == 0 || fuse < 0f) {
            text = "TNT: none nearby";
        } else if (count == 1) {
            text = String.format(java.util.Locale.ROOT, "TNT %.1fs", fuse);
        } else {
            text = String.format(java.util.Locale.ROOT, "TNT ×%d  %.1fs", count, fuse);
        }
        if (!text.equals(lastText)) {
            lastText = text;
            element.setText(text);
        }
    }
}
