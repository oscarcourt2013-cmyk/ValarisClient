package dev.stellarclient.core.hud.elements;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.notification.Notification;
import dev.stellarclient.core.notification.NotificationManager;
import dev.stellarclient.core.notification.NotificationPreferences;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;
import dev.stellarclient.core.util.ColorUtil;
import dev.stellarclient.core.util.Easing;

import java.util.function.Consumer;

/** Premium notification stack with icons and slide-in animation. */
public final class NotificationsElement extends HudElement implements Consumer<Notification> {

    private static final int WIDTH = 168;
    private static final int ROW_HEIGHT = 26;
    private static final int GAP = 4;
    private static final int PADDING = 4;
    private static final int ACCENT_WIDTH = 3;

    private final NotificationManager notifications;
    private final ThemeManager themes;
    private final NotificationPreferences prefs;

    private RenderContext ctx;
    private long now;
    private int cursorY;

    public NotificationsElement(NotificationManager notifications, ThemeManager themes,
                                  NotificationPreferences prefs) {
        super("notifications", "Notifications", HudAnchor.TOP_RIGHT, -4, 4);
        this.notifications = notifications;
        this.themes = themes;
        this.prefs = prefs;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return WIDTH;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        int count = notifications.activeCount();
        return count == 0 ? 0 : count * ROW_HEIGHT + (count - 1) * GAP;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        this.ctx = ctx;
        this.now = nowMillis;
        this.cursorY = 0;
        notifications.forEachActive(nowMillis, this);
        this.ctx = null;
    }

    @Override
    public void accept(Notification notification) {
        Theme theme = themes.active();
        float progress = notification.progress(now);
        float slide = (1f - Easing.easeOutCubic(1f - progress)) * 12f * prefs.slideStrength();
        int y = cursorY + Math.round(slide);

        ctx.fillRect(0, y, WIDTH, ROW_HEIGHT, theme.background());
        ctx.fillRect(0, y, ACCENT_WIDTH, ROW_HEIGHT, levelColor(notification, theme));
        int textX = ACCENT_WIDTH + PADDING;
        if (prefs.showIcons()) {
            ctx.drawText(levelIcon(notification), textX, y + PADDING, levelColor(notification, theme), true);
            textX += 12;
        }
        ctx.drawText(notification.title(), textX, y + PADDING, theme.foreground(), true);
        ctx.drawText(notification.message(), textX, y + ROW_HEIGHT - PADDING - ctx.fontHeight(),
                theme.foregroundMuted(), true);
        int barWidth = Math.round(WIDTH * (1f - progress));
        ctx.fillRect(0, y + ROW_HEIGHT - 1, barWidth, 1, ColorUtil.withAlpha(theme.accent(), 0.8f));
        cursorY += ROW_HEIGHT + GAP;
    }

    private static int levelColor(Notification notification, Theme theme) {
        return switch (notification.level()) {
            case INFO -> theme.accent();
            case SUCCESS -> theme.success();
            case WARNING -> theme.warning();
            case ERROR -> theme.error();
        };
    }

    private static String levelIcon(Notification notification) {
        return switch (notification.level()) {
            case INFO -> "i";
            case SUCCESS -> "âœ“";
            case WARNING -> "!";
            case ERROR -> "Ã—";
        };
    }
}
