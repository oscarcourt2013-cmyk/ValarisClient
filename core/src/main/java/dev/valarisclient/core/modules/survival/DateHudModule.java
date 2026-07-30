package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/** Real-world local calendar date on the HUD. */
public final class DateHudModule extends Module {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

    private final SimpleLineHud element;
    private String lastText = "";

    public DateHudModule(HudManager hud, ThemeManager themes) {
        super("date-hud", "Date HUD", "Shows today's real-world local date", ModuleCategory.SURVIVAL);
        this.element = hud.register(new SimpleLineHud(
                "date-hud", "Date HUD", themes, HudAnchor.TOP_LEFT, 4, 68));
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
        String text = FORMAT.format(LocalDate.now());
        if (!text.equals(lastText)) {
            lastText = text;
            element.setText(text);
        }
    }
}
