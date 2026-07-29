package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.Locale;

/** Manual start/stop duel timer with optional chat triggers. */
public final class DuelTimerModule extends Module {

    private static final int KEY_TOGGLE = 84; // T

    private final BooleanSetting chatTriggers =
            addSetting(new BooleanSetting("chat-triggers", "Chat triggers", "Start/stop via duel chat", true));

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    private boolean running;
    private long startMillis;
    private boolean keyDown;

    public DuelTimerModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("duel-timer", "Duel Timer", "Manual duel timer HUD", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "duel-timer", "Duel Timer", themes, HudAnchor.TOP_CENTER, 0, 4));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> onTick());
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        running = false;
        element.setVisible(false);
        keyDown = false;
    }

    private void onTick() {
        boolean down = adapter.isKeyDown(KEY_TOGGLE);
        if (down && !keyDown && adapter.hasPlayer() && !adapter.isScreenOpen()) {
            toggleTimer();
        }
        keyDown = down;
        refresh();
    }

    private void onChat(ChatMessageEvent event) {
        if (!chatTriggers.get() || event.outgoing()) {
            return;
        }
        String lower = event.text().toLowerCase(Locale.ROOT);
        if (lower.contains("duel start") || lower.contains("fight!")) {
            startTimer();
        } else if (lower.contains("duel stop") || lower.contains("gg")) {
            stopTimer();
        }
    }

    private void toggleTimer() {
        if (running) {
            stopTimer();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        running = true;
        startMillis = System.currentTimeMillis();
        refresh();
    }

    private void stopTimer() {
        running = false;
        refresh();
    }

    private void refresh() {
        if (!running) {
            element.setText("Duel: stopped (T)");
            return;
        }
        long elapsed = System.currentTimeMillis() - startMillis;
        long seconds = elapsed / 1000L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        element.setText(String.format(Locale.ROOT, "Duel: %d:%02d", minutes, seconds));
    }
}
