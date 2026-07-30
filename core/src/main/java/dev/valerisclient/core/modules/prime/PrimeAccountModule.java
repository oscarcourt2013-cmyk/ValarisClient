package dev.valerisclient.core.modules.prime;

import dev.valerisclient.core.account.PrimeAccountService;
import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.event.WorldJoinEvent;
import dev.valerisclient.core.event.WorldLeaveEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;
import dev.valerisclient.core.util.SessionTracker;

/** HUD showing the player name and current session duration. */
public final class PrimeAccountModule extends Module {

    private final MinecraftAdapter adapter;
    private final PrimeAccountService account;
    private final SessionTracker sessionTracker = new SessionTracker();
    private final Element element;

    public PrimeAccountModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter,
                              PrimeAccountService account) {
        super("prime-account", "Valeris Account", "Shows your name and session time", ModuleCategory.PRIME);
        this.adapter = adapter;
        this.account = account;
        this.element = hud.register(new Element(themes, adapter, account, sessionTracker));
        element.setVisible(false);

        listen(WorldJoinEvent.class, event -> sessionTracker.onJoin());
        listen(WorldLeaveEvent.class, event -> sessionTracker.onLeave());
    }

    @Override
    protected void onEnable() {
        if (!account.loggedIn() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final PrimeAccountService account;
        private final SessionTracker sessionTracker;

        private long lastSessionSecond = -1;
        private String lastName = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, PrimeAccountService account,
                SessionTracker sessionTracker) {
            super("prime-account", "Valeris Account", HudAnchor.TOP_LEFT, 4, 52);
            this.themes = themes;
            this.adapter = adapter;
            this.account = account;
            this.sessionTracker = sessionTracker;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            refresh();
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            String name = adapter.playerName();
            if (name.isEmpty()) {
                name = "Player";
            }
            long sessionSeconds = sessionTracker.sessionMillis() / 1000L;
            if (!name.equals(lastName) || sessionSeconds != lastSessionSecond) {
                lastName = name;
                lastSessionSecond = sessionSeconds;
                String status = account.loggedIn() ? "Valeris" : "Guest";
                text = name + " | " + status + " | " + formatDuration(sessionSeconds);
            }
        }

        private static String formatDuration(long totalSeconds) {
            long hours = totalSeconds / 3600L;
            long minutes = (totalSeconds % 3600L) / 60L;
            long seconds = totalSeconds % 60L;
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            }
            if (minutes > 0) {
                return minutes + "m " + seconds + "s";
            }
            return seconds + "s";
        }
    }
}
