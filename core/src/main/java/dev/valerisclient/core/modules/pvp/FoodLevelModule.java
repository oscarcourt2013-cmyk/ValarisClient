package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Hunger level — sprint and regen depend on it in long fights. */
public final class FoodLevelModule extends Module {

    private final IntSetting warnBelow = addSetting(new IntSetting(
            "warn-below", "Warn below", "Highlight when food is at or below this", 8, 1, 19));

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public FoodLevelModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("food-level", "Food Level", "Hunger for sprint and regeneration", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "food-level", "Food Level", themes, HudAnchor.TOP_LEFT, 4, 20));
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
        int food = adapter.playerFoodLevel();
        String tag = food <= warnBelow.get() ? " LOW" : "";
        element.setText("Food: " + food + "/20" + tag);
    }
}
