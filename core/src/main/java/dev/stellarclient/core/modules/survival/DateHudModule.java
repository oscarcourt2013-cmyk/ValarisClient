package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

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
