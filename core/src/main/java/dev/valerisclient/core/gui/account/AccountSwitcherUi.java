package dev.valerisclient.core.gui.account;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.account.LauncherAccountStore;
import dev.valerisclient.core.account.LauncherAccountStore.AccountEntry;
import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.design.ValerisDesign;
import dev.valerisclient.core.gui.UiChrome;
import dev.valerisclient.core.i18n.ValerisLang;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** In-game account picker — title menu only (no world loaded). */
public final class AccountSwitcherUi {

    private static final int PANEL_W = 360;
    private static final int ROW_H = 36;
    private static final int PAD = 16;

    private final MinecraftAdapter adapter;
    private final List<AccountEntry> accounts = new ArrayList<>();
    private String draftName = "";
    private String status = "";
    private boolean statusError;
    private boolean busy;
    private final AtomicBoolean switching = new AtomicBoolean(false);
    private int scroll;

    public AccountSwitcherUi(MinecraftAdapter adapter) {
        this.adapter = adapter;
        reload();
    }

    public void reload() {
        accounts.clear();
        accounts.addAll(LauncherAccountStore.list());
        status = "";
        statusError = false;
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        int sw = ctx.screenWidth();
        int sh = ctx.screenHeight();
        int panelH = Math.min(460, sh - 36);
        int x = (sw - PANEL_W) / 2;
        int y = (sh - panelH) / 2;

        ctx.fillRect(0, 0, sw, sh, 0x88000000);
        UiChrome.glassPanel(ctx, theme, x, y, PANEL_W, panelH);

        ctx.drawSmoothText(
                ValerisLang.get("valeris.gui.account.title", "Switch Account"),
                x + PAD, y + 14, theme.foreground(), 1.12f);

        String current = adapter.playerName();
        if (current != null && !current.isBlank()) {
            String type = adapter.sessionAccountType();
            String chip = current + (type == null || type.isBlank() ? "" : " · " + typeLabel(type));
            int chipW = ctx.smoothTextWidth(chip, 0.78f) + 16;
            int chipX = x + PANEL_W - PAD - chipW;
            ctx.fillRoundedRect(chipX, y + 12, chipW, 18, ValerisDesign.RADIUS_SM,
                    ColorUtil.withAlpha(theme.accent(), 0.22f));
            ctx.drawSmoothText(chip, chipX + 8, y + 16, theme.accent(), 0.78f);
        }

        int listTop = y + 44;
        int listBottom = y + panelH - 96;
        int listH = listBottom - listTop;
        UiChrome.cardLite(ctx, theme, x + PAD, listTop, PANEL_W - PAD * 2, listH, false);

        Optional<String> activeId = LauncherAccountStore.activeAccountId();
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, accounts.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (accounts.isEmpty()) {
            ctx.drawSmoothText(
                    ValerisLang.get("valeris.gui.account.empty", "No launcher accounts found."),
                    x + PAD + 12, listTop + 16, theme.foreground(), 0.9f);
            ctx.drawSmoothText(
                    ValerisLang.get("valeris.gui.account.empty_hint",
                            "Sign in from the ValerisClient Launcher, or add an offline name below."),
                    x + PAD + 12, listTop + 34, theme.foregroundMuted(), 0.78f);
        } else {
            for (int i = scroll; i < accounts.size(); i++) {
                int rowY = listTop + (i - scroll) * ROW_H;
                if (rowY + ROW_H > listBottom) {
                    break;
                }
                AccountEntry a = accounts.get(i);
                boolean hover = mouseX >= x + PAD && mouseX < x + PANEL_W - PAD
                        && mouseY >= rowY && mouseY < rowY + ROW_H;
                boolean active = activeId.map(id -> id.equals(a.id())).orElse(false)
                        || a.username().equalsIgnoreCase(current);
                if (hover || active) {
                    ctx.fillRoundedRect(x + PAD + 4, rowY + 3, PANEL_W - PAD * 2 - 8, ROW_H - 6,
                            ValerisDesign.RADIUS_SM,
                            ColorUtil.withAlpha(active ? theme.accent() : theme.backgroundLight(),
                                    active ? 0.32f : 0.5f));
                }
                // Avatar stand-in
                int av = ColorUtil.withAlpha(a.microsoft() ? theme.accent() : theme.foregroundMuted(), 0.85f);
                ctx.fillRoundedRect(x + PAD + 12, rowY + 8, 20, 20, 6, av);
                ctx.drawSmoothText(
                        a.username().isEmpty() ? "?" : a.username().substring(0, 1).toUpperCase(Locale.ROOT),
                        x + PAD + 18, rowY + 13, 0xFFFFFFFF, 0.85f);

                ctx.drawSmoothText(a.username(), x + PAD + 40, rowY + 8, theme.foreground(), 0.9f);
                String meta = a.microsoft()
                        ? ValerisLang.get("valeris.gui.account.type.microsoft", "Microsoft")
                        : ValerisLang.get("valeris.gui.account.type.offline", "Offline");
                if (active) {
                    meta += " · " + ValerisLang.get("valeris.gui.account.active", "Active");
                }
                ctx.drawSmoothText(meta, x + PAD + 40, rowY + 20, theme.foregroundMuted(), 0.72f);

                if (hover && !busy) {
                    String action = ValerisLang.get("valeris.gui.account.use", "Use →");
                    int aw = ctx.smoothTextWidth(action, 0.78f);
                    ctx.drawSmoothText(action, x + PANEL_W - PAD - 12 - aw, rowY + 12, theme.accent(), 0.78f);
                }
            }
        }

        int fieldY = y + panelH - 78;
        ctx.fillRoundedRect(x + PAD, fieldY, PANEL_W - PAD * 2 - 86, 28, ValerisDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.background(), 0.55f));
        ctx.fillRoundedBorder(x + PAD, fieldY, PANEL_W - PAD * 2 - 86, 28, ValerisDesign.RADIUS_SM, 1,
                ColorUtil.withAlpha(theme.border(), 0.45f), ColorUtil.withAlpha(theme.background(), 0.55f));
        String placeholder = ValerisLang.get("valeris.gui.account.offline_hint", "Offline name…");
        String fieldText = draftName.isEmpty()
                ? placeholder
                : draftName + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        ctx.drawSmoothText(fieldText, x + PAD + 10, fieldY + 8,
                draftName.isEmpty() ? theme.foregroundMuted() : theme.foreground(), 0.88f);

        boolean addHover = !busy && hit(mouseX, mouseY, x + PANEL_W - PAD - 78, fieldY, 78, 28);
        UiChrome.button(ctx, theme, x + PANEL_W - PAD - 78, fieldY, 78, 28, addHover, true);
        String addLabel = ValerisLang.get("valeris.gui.account.add", "Add");
        int tw = ctx.smoothTextWidth(addLabel, 0.88f);
        ctx.drawSmoothText(addLabel, x + PANEL_W - PAD - 78 + (78 - tw) / 2, fieldY + 8,
                0xFFFFFFFF, 0.88f);

        if (!status.isBlank()) {
            ctx.drawSmoothText(status, x + PAD, y + panelH - 28,
                    statusError ? 0xFFFF6B6B : theme.foregroundMuted(), 0.78f);
        } else if (busy) {
            ctx.drawSmoothText(
                    ValerisLang.get("valeris.gui.account.switching", "Switching…"),
                    x + PAD, y + panelH - 28, theme.accent(), 0.78f);
        } else {
            ctx.drawSmoothText(
                    ValerisLang.get("valeris.gui.account.hint", "Click an account · Esc to close"),
                    x + PAD, y + panelH - 28, theme.foregroundMuted(), 0.78f);
        }
    }

    public boolean mousePressed(double mouseX, double mouseY, int button, int screenW, int screenH) {
        if (button != 0 || busy) {
            return true;
        }
        int panelH = Math.min(460, screenH - 36);
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - panelH) / 2;
        int listTop = y + 44;
        int listBottom = y + panelH - 96;
        int fieldY = y + panelH - 78;

        if (hit(mouseX, mouseY, x + PANEL_W - PAD - 78, fieldY, 78, 28)) {
            addOffline();
            return true;
        }

        if (mouseX >= x + PAD && mouseX < x + PANEL_W - PAD
                && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + (int) ((mouseY - listTop) / ROW_H);
            if (index >= 0 && index < accounts.size()) {
                switchTo(accounts.get(index));
            }
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scroll = Math.max(0, scroll - 1);
        } else if (delta < 0) {
            scroll += 1;
        }
        return true;
    }

    public boolean charTyped(char c) {
        if (busy) {
            return true;
        }
        if (c >= 32 && c < 127 && draftName.length() < 16) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                draftName += c;
            }
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (key == 256) { // Escape
            adapter.closeAccountSwitcher();
            return true;
        }
        if (busy) {
            return true;
        }
        if (key == 259) { // Backspace
            if (!draftName.isEmpty()) {
                draftName = draftName.substring(0, draftName.length() - 1);
            }
            return true;
        }
        if (key == 257 || key == 335) { // Enter
            addOffline();
            return true;
        }
        return true;
    }

    private void addOffline() {
        if (busy || draftName.isBlank()) {
            return;
        }
        Optional<AccountEntry> created = LauncherAccountStore.addOffline(draftName);
        if (created.isEmpty()) {
            status = ValerisLang.get("valeris.gui.account.invalid", "Invalid or duplicate name.");
            statusError = true;
            return;
        }
        draftName = "";
        reload();
        switchTo(created.get());
    }

    private void switchTo(AccountEntry account) {
        if (!switching.compareAndSet(false, true)) {
            return;
        }
        busy = true;
        status = "";
        statusError = false;
        Thread worker = new Thread(() -> {
            try {
                var payload = LauncherAccountStore.resolveForSwitch(account);
                adapter.runOnClientThread(() -> {
                    boolean ok = adapter.applyMinecraftSession(
                            payload.username(),
                            payload.uuid(),
                            payload.accessToken(),
                            payload.microsoft());
                    if (ok) {
                        LauncherAccountStore.setActive(account.id());
                        reload();
                        status = ValerisLang.get("valeris.gui.account.switched", "Switched to")
                                + " " + payload.username();
                        statusError = false;
                        ValerisClient.get().notifications().success("Account", payload.username());
                        adapter.closeAccountSwitcher();
                    } else {
                        status = ValerisLang.get("valeris.gui.account.failed",
                                "Could not apply session (title screen only).");
                        statusError = true;
                    }
                    busy = false;
                    switching.set(false);
                });
            } catch (Exception e) {
                adapter.runOnClientThread(() -> {
                    status = friendlyError(e);
                    statusError = true;
                    busy = false;
                    switching.set(false);
                });
            }
        }, "prime-account-switch");
        worker.setDaemon(true);
        worker.start();
    }

    private static String friendlyError(Exception e) {
        String raw = e.getMessage() == null ? "" : e.getMessage();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("needs a launcher re-login") || lower.contains("refresh")) {
            return ValerisLang.get("valeris.gui.account.relogin",
                    "Microsoft session expired — sign in again in the launcher.");
        }
        if (lower.contains("http 400") || lower.contains("invalid_grant") || lower.contains("aadsts")) {
            return ValerisLang.get("valeris.gui.account.token_invalid",
                    "Microsoft token invalid — re-login in the ValerisClient Launcher.");
        }
        if (lower.contains("http 401") || lower.contains("unauthorized")) {
            return ValerisLang.get("valeris.gui.account.unauthorized",
                    "Xbox / Minecraft auth failed — check the account owns Java.");
        }
        if (raw.length() > 72) {
            return raw.substring(0, 69) + "…";
        }
        return raw.isBlank() ? ValerisLang.get("valeris.gui.account.failed", "Switch failed") : raw;
    }

    private static String typeLabel(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "microsoft" -> "MS";
            case "offline" -> "OFF";
            default -> type;
        };
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
