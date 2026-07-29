package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Growth stage of the crop block under the player. */
public final class CropGrowthHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public CropGrowthHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("crop-growth-hud", "Crop Growth HUD", "Shows crop growth under you", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "crop-growth-hud", "Crop Growth", themes, HudAnchor.TOP_LEFT, 4, 48));
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
        int stage = adapter.cropGrowthStage();
        if (stage < 0) {
            String block = adapter.blockUnderPlayerName();
            element.setText(block.isEmpty() ? "Crop: â€”" : "Block: " + block);
            return;
        }
        element.setText("Crop: " + stage + "/7");
    }
}
