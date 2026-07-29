package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.EnumSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.modules.qol.WaypointHud;
import dev.stellarclient.core.theme.ThemeManager;

/** Named structure waypoints with direction and distance. */
public final class StructureLogModule extends Module {

    private static final int KEY_SAVE = 78; // N

    public enum Slot {
        STRONGHOLD, NETHER, END, BASE, CUSTOM
    }

    private final EnumSetting<Slot> active =
            addSetting(new EnumSetting<>("active", "Active", "Selected structure", Slot.STRONGHOLD));
    private final StringSetting customName =
            addSetting(new StringSetting("custom-name", "Custom name", "Label for custom slot", "Custom"));
    private final DoubleSetting x =
            addSetting(new DoubleSetting("x", "X", "Structure X", 0, -30_000_000, 30_000_000));
    private final DoubleSetting y =
            addSetting(new DoubleSetting("y", "Y", "Structure Y", 64, -64, 320));
    private final DoubleSetting z =
            addSetting(new DoubleSetting("z", "Z", "Structure Z", 0, -30_000_000, 30_000_000));

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;
    private boolean saveKeyDown;

    public StructureLogModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("structure-log", "Structure Log", "Direction to saved structures", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "structure-log", "Structure Log", themes, HudAnchor.TOP_RIGHT, -4, 76));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        saveKeyDown = false;
    }

    private void onTick() {
        boolean down = adapter.isKeyDown(KEY_SAVE);
        if (down && !saveKeyDown && adapter.hasPlayer()) {
            x.set(adapter.playerX());
            y.set(adapter.playerY());
            z.set(adapter.playerZ());
        }
        saveKeyDown = down;
        refresh();
    }

    private void refresh() {
        String label = active.get() == Slot.CUSTOM ? customName.get() : active.get().name();
        double dx = x.get() - adapter.playerX();
        double dz = z.get() - adapter.playerZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        String dir = WaypointHud.directionTo(adapter, dx, dz);
        element.setText(label + ": " + dir + " " + Math.round(dist) + "m (N=save)");
    }
}
