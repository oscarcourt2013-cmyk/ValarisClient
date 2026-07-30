package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Held tool durability for mining and combat. */
public final class ToolDurabilityModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ToolDurabilityModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("tool-durability", "Tool Durability", "Durability of item in main hand", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "tool-durability", "Tool Durability", themes, HudAnchor.BOTTOM_RIGHT, -4, -116));
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
        int percent = adapter.heldItemDurabilityPercent();
        if (percent < 0) {
            element.setText("Tool: —");
        } else {
            element.setText("Tool: " + percent + "%");
        }
    }
}
