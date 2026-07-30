package dev.valarisclient.core.modules.valaris;

import dev.valarisclient.core.account.ValarisAccountService;
import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.event.WorldJoinEvent;
import dev.valarisclient.core.event.WorldLeaveEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;
import dev.valarisclient.core.util.SessionTracker;

/** HUD showing the player name and current session duration. */
public final class ValarisAccountModule extends Module {

    private final MinecraftAdapter adapter;
    private final ValarisAccountService account;
    private final SessionTracker sessionTracker = new SessionTracker();
    private final Element element;

    public ValarisAccountModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter,
                              ValarisAccountService account) {
        super("valaris-account", "Valaris Account", "Shows your name and session time", ModuleCategory.VALARIS);
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
        private final ValarisAccountService account;
        private final SessionTracker sessionTracker;

        private long lastSessionSecond = -1;
        private String lastName = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, ValarisAccountService account,
                SessionTracker sessionTracker) {
            super("valaris-account", "Valaris Account", HudAnchor.TOP_LEFT, 4, 52);
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
                String status = account.loggedIn() ? "Valaris" : "Guest";
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
