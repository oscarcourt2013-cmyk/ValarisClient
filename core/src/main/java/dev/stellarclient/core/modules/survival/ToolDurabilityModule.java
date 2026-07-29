package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

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
            element.setText("Tool: â€”");
        } else {
            element.setText("Tool: " + percent + "%");
        }
    }
}
