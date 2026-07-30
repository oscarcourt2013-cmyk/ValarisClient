package dev.valerisclient.core.gui.menu;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.cloud.CloudClient;
import dev.valerisclient.core.cloud.CloudSyncManager;
import dev.valerisclient.core.gui.UiChrome;
import dev.valerisclient.core.i18n.PrimeLang;
import dev.valerisclient.core.profile.ProfileManager;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.util.ColorUtil;

import java.util.List;

/**
 * Local configuration backups — upload / download / restore.
 * Storage is on-disk under the game dir (not a remote cloud).
 */
public final class ConfigurationsMenuRenderer {

    // Kept within Minecraft's smallest effective GUI canvas (320x240).
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 224;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 6;

    private int selectedVersion;
    private String statusMessage = "";

    public void render(RenderContext ctx, Theme theme, CloudSyncManager cloud, ProfileManager profiles,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2;
        UiChrome.glassPanel(ctx, theme, x, y, PANEL_W, PANEL_H);

        ctx.drawText(PrimeLang.get("prime.gui.configurations.title", "Configurations"),
                x + 12, y + 12, theme.accent(), true);
        ctx.drawText(PrimeLang.get("prime.gui.configurations.local_only", "Local backups only — stored on this PC"),
                x + 12, y + 28, theme.foregroundMuted(), true);
        ctx.drawText(PrimeLang.get("prime.gui.configurations.profile", "Profile: %s", profiles.activeProfile()),
                x + 12, y + 44, theme.foreground(), true);

        int btnY = y + 64;
        int btnW = (PANEL_W - 24 - BTN_GAP * 2) / 3;
        drawButton(ctx, theme, x + 12, btnY, btnW, BTN_H,
                PrimeLang.get("prime.gui.configurations.upload", "Upload"), mouseX, mouseY, true);
        drawButton(ctx, theme, x + 12 + btnW + BTN_GAP, btnY, btnW, BTN_H,
                PrimeLang.get("prime.gui.configurations.download", "Download"), mouseX, mouseY, false);
        drawButton(ctx, theme, x + 12 + (btnW + BTN_GAP) * 2, btnY, btnW, BTN_H,
                PrimeLang.get("prime.gui.configurations.restore", "Restore"), mouseX, mouseY, false);

        List<CloudClient.VersionEntry> versions = cloud.listVersions(profiles.activeProfile());
        if (selectedVersion >= versions.size()) {
            selectedVersion = Math.max(0, versions.size() - 1);
        }

        int rowY = btnY + BTN_H + 14;
        ctx.drawText(PrimeLang.get("prime.gui.configurations.versions", "Versions (%d):", versions.size()),
                x + 12, rowY, theme.foreground(), true);
        rowY += 14;
        int shown = 0;
        for (int i = 0; i < versions.size() && shown < 5; i++) {
            CloudClient.VersionEntry entry = versions.get(i);
            boolean sel = i == selectedVersion;
            int rowH = 14;
            if (sel || hit(mouseX, mouseY, x + 12, rowY - 1, PANEL_W - 24, rowH + 2)) {
                ctx.fillRoundedRect(x + 12, rowY - 1, PANEL_W - 24, rowH + 2, 4,
                        ColorUtil.withAlpha(theme.accent(), sel ? 0.28f : 0.12f));
            }
            ctx.drawText((sel ? "› " : "  ") + trimLabel(entry.label(), 40),
                    x + 14, rowY + 1, sel ? theme.accent() : theme.foregroundMuted(), true);
            rowY += rowH + 2;
            shown++;
        }
        if (versions.isEmpty()) {
            ctx.drawText(PrimeLang.get("prime.gui.configurations.empty", "No backups yet — tap Upload"),
                    x + 16, rowY, theme.foregroundMuted(), true);
        }

        if (!statusMessage.isBlank()) {
            ctx.drawText(statusMessage, x + 12, y + PANEL_H - 28, theme.foregroundMuted(), true);
        }
        ctx.drawText(PrimeLang.get("prime.gui.configurations.hint", "Click a version, then Restore"),
                x + 12, y + PANEL_H - 14, theme.foregroundMuted(), true);
    }

    public boolean mousePressed(double mx, double my, int button,
                                CloudSyncManager cloud, ProfileManager profiles,
                                int screenW, int screenH) {
        if (button != 0) {
            return false;
        }
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2;
        if (mx < x || mx >= x + PANEL_W || my < y || my >= y + PANEL_H) {
            return false;
        }

        int btnY = y + 64;
        int btnW = (PANEL_W - 24 - BTN_GAP * 2) / 3;
        if (hit(mx, my, x + 12, btnY, btnW, BTN_H)) {
            profiles.saveActive();
            cloud.uploadNow(profiles.activeProfile());
            statusMessage = PrimeLang.get("prime.gui.configurations.status.uploaded", "Backup saved locally");
            return true;
        }
        if (hit(mx, my, x + 12 + btnW + BTN_GAP, btnY, btnW, BTN_H)) {
            if (cloud.downloadNow(profiles.activeProfile())) {
                profiles.saveActive();
                statusMessage = PrimeLang.get("prime.gui.configurations.status.downloaded", "Backup restored");
            } else {
                statusMessage = PrimeLang.get("prime.gui.configurations.status.none", "No backup found");
            }
            return true;
        }
        if (hit(mx, my, x + 12 + (btnW + BTN_GAP) * 2, btnY, btnW, BTN_H)) {
            List<CloudClient.VersionEntry> versions = cloud.listVersions(profiles.activeProfile());
            if (selectedVersion >= 0 && selectedVersion < versions.size()) {
                String id = versions.get(selectedVersion).id();
                if (cloud.restoreVersion(profiles.activeProfile(), id)) {
                    profiles.saveActive();
                    statusMessage = PrimeLang.get("prime.gui.configurations.status.restored", "Version restored");
                } else {
                    statusMessage = PrimeLang.get("prime.gui.configurations.status.failed", "Restore failed");
                }
            } else {
                statusMessage = PrimeLang.get("prime.gui.configurations.status.pick", "Select a version first");
            }
            return true;
        }

        List<CloudClient.VersionEntry> versions = cloud.listVersions(profiles.activeProfile());
        int rowY = btnY + BTN_H + 14 + 14;
        for (int i = 0; i < versions.size() && i < 5; i++) {
            if (hit(mx, my, x + 12, rowY - 1, PANEL_W - 24, 14)) {
                selectedVersion = i;
                return true;
            }
            rowY += 16;
        }
        return true;
    }

    public boolean keyPressed(int key, CloudSyncManager cloud, ProfileManager profiles) {
        if (key == 85) { // U
            profiles.saveActive();
            cloud.uploadNow(profiles.activeProfile());
            statusMessage = PrimeLang.get("prime.gui.configurations.status.uploaded", "Backup saved locally");
            return true;
        }
        if (key == 68) { // D
            if (cloud.downloadNow(profiles.activeProfile())) {
                profiles.saveActive();
                statusMessage = PrimeLang.get("prime.gui.configurations.status.downloaded", "Backup restored");
            } else {
                statusMessage = PrimeLang.get("prime.gui.configurations.status.none", "No backup found");
            }
            return true;
        }
        return false;
    }

    private static void drawButton(RenderContext ctx, Theme theme, int x, int y, int w, int h,
                                   String label, double mouseX, double mouseY, boolean primary) {
        boolean hover = hit(mouseX, mouseY, x, y, w, h);
        UiChrome.button(ctx, theme, x, y, w, h, hover, primary);
        int tw = ctx.textWidth(label);
        ctx.drawText(label, x + (w - tw) / 2, y + (h - ctx.fontHeight()) / 2 + 1,
                primary || hover ? 0xFFFFFFFF : theme.foregroundMuted(), false);
    }

    private static String trimLabel(String label, int max) {
        if (label.length() <= max) {
            return label;
        }
        return label.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
