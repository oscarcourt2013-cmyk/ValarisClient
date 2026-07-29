package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** Logs recent villager trades from chat parsing. */
public final class VillagerTradeLogModule extends Module {

    private static final int MAX_ENTRIES = 5;

    private final SimpleLineHud element;
    private final Deque<String> trades = new ArrayDeque<>();

    public VillagerTradeLogModule(HudManager hud, ThemeManager themes) {
        super("villager-trade-log", "Villager Trade Log", "Recent trades from chat", ModuleCategory.SURVIVAL);
        this.element = hud.register(new SimpleLineHud(
                "villager-trade-log", "Trade Log", themes, HudAnchor.BOTTOM_RIGHT, -4, -76));
        element.setVisible(false);
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        trades.clear();
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing()) {
            return;
        }
        String lower = event.text().toLowerCase(Locale.ROOT);
        if (lower.contains("traded") || lower.contains("villager") && lower.contains("emerald")) {
            push(event.text());
        }
    }

    private void push(String line) {
        if (trades.size() >= MAX_ENTRIES) {
            trades.removeLast();
        }
        trades.addFirst(line.length() > 40 ? line.substring(0, 37) + "..." : line);
        refresh();
    }

    private void refresh() {
        if (trades.isEmpty()) {
            element.setText("Trades: â€”");
            return;
        }
        element.setText("Trade: " + trades.peekFirst());
    }
}
