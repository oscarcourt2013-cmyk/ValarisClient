package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

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
            element.setText(block.isEmpty() ? "Crop: —" : "Block: " + block);
            return;
        }
        element.setText("Crop: " + stage + "/7");
    }
}
