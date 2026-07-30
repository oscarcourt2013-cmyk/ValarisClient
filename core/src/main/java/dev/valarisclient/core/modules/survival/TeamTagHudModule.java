package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.StringSetting;
import dev.valarisclient.core.theme.ThemeManager;

import java.util.Arrays;

/** Local friend list with colored tags in HUD. */
public final class TeamTagHudModule extends Module {

    private final StringSetting friends =
            addSetting(new StringSetting("friends", "Friends", "Comma-separated names", ""));

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public TeamTagHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("team-tag-hud", "Team Tag HUD", "Shows online friends from your list", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "team-tag-hud", "Team Tags", themes, HudAnchor.TOP_LEFT, 4, 72));
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
        String raw = friends.get().trim();
        if (raw.isEmpty()) {
            element.setText("Team: (add friends)");
            return;
        }
        String[] names = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (names.length == 0) {
            element.setText("Team: (add friends)");
            return;
        }
        StringBuilder sb = new StringBuilder("Team: ");
        for (int i = 0; i < names.length && i < 4; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("§").append((char) ('a' + (i % 6))).append(names[i]);
        }
        element.setText(sb.toString());
    }
}
