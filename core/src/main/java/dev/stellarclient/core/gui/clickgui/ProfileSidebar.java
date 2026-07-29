package dev.stellarclient.core.gui.clickgui;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.gui.component.TextInputField;
import dev.stellarclient.core.profile.ProfileManager;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

import java.util.List;

/** Left-hand named-profile list for the ClickGUI browse screen. */
final class ProfileSidebar {

    private static final int ROW_H = 20;
    private static final int PAD = 8;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_GAP = 4;

    private TextInputField newProfileField;
    private boolean naming;

    void render(RenderContext ctx, Theme theme, ClickGuiBrowseLayout layout, ProfileManager profiles,
               double mouseX, double mouseY) {
        int x = layout.sidebarX();
        int y = layout.sidebarY();
        int w = layout.sidebarW();
        int h = layout.sidebarH();
        if (w <= 0 || h <= 0) {
            return;
        }

        UiChrome.cardElevated(ctx, theme, x, y, w, h, false);

        List<String> names = profiles.listProfiles();
        int rowY = y + PAD;
        for (String name : names) {
            boolean active = name.equals(profiles.activeProfile());
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (active || hover) {
                ctx.fillRoundedRect(x + 4, rowY, w - 8, ROW_H, PrimeDesign.RADIUS_SM,
                        active ? theme.surfaceElevated() : ColorUtil.withAlpha(theme.surfaceElevated(), 0.5f));
            }
            if (active) {
                ctx.fillRect(x + 4, rowY + 4, 2, ROW_H - 8, theme.accent());
            }
            String label = GuiLayout.trimToWidth(ctx, name, w - PAD * 2 - 8);
            GuiLayout.label(ctx, label, x + PAD + 4, rowY + (ROW_H - ctx.uiFontHeight()) / 2 + 1,
                    active ? theme.foreground() : theme.foregroundMuted());
            rowY += ROW_H;
        }

        int buttonsY = y + h - PAD - BUTTON_H * 2 - BUTTON_GAP;
        if (naming && newProfileField != null) {
            newProfileField.render(ctx, theme, x + PAD, buttonsY, w - PAD * 2);
        } else {
            boolean saveHover = mouseX >= x + PAD && mouseX < x + w - PAD
                    && mouseY >= buttonsY && mouseY < buttonsY + BUTTON_H;
            UiChrome.button(ctx, theme, x + PAD, buttonsY, w - PAD * 2, BUTTON_H, saveHover, false);
            String saveLabel = GuiLayout.trimToWidth(ctx, "SAVE AS NEW", w - PAD * 2 - 6);
            int saveLabelW = GuiLayout.labelWidth(ctx, saveLabel);
            GuiLayout.label(ctx, saveLabel, x + PAD + (w - PAD * 2 - saveLabelW) / 2,
                    buttonsY + (BUTTON_H - ctx.uiFontHeight()) / 2 + 1,
                    saveHover ? theme.foreground() : theme.foregroundMuted());
        }

        int hudY = buttonsY + BUTTON_H + BUTTON_GAP;
        boolean hudHover = mouseX >= x + PAD && mouseX < x + w - PAD
                && mouseY >= hudY && mouseY < hudY + BUTTON_H;
        UiChrome.button(ctx, theme, x + PAD, hudY, w - PAD * 2, BUTTON_H, hudHover, true);
        String hudLabel = GuiLayout.trimToWidth(ctx, "EDIT HUD", w - PAD * 2 - 6);
        int hudLabelW = GuiLayout.labelWidth(ctx, hudLabel);
        GuiLayout.label(ctx, hudLabel, x + PAD + (w - PAD * 2 - hudLabelW) / 2,
                hudY + (BUTTON_H - ctx.uiFontHeight()) / 2 + 1, theme.foreground());
    }

    /** @return true if the press landed inside the sidebar (and was consumed). */
    boolean mousePressed(ClickGuiBrowseLayout layout, ProfileManager profiles, MinecraftAdapter adapter,
                        double mouseX, double mouseY, int button) {
        int x = layout.sidebarX();
        int y = layout.sidebarY();
        int w = layout.sidebarW();
        int h = layout.sidebarH();
        if (w <= 0 || h <= 0 || mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) {
            return false;
        }

        List<String> names = profiles.listProfiles();
        int rowY = y + PAD;
        for (String name : names) {
            if (mouseY >= rowY && mouseY < rowY + ROW_H) {
                profiles.switchTo(name);
                return true;
            }
            rowY += ROW_H;
        }

        int buttonsY = y + h - PAD - BUTTON_H * 2 - BUTTON_GAP;
        if (naming && newProfileField != null) {
            if (newProfileField.hit(mouseX, mouseY, x + PAD, buttonsY, w - PAD * 2)) {
                newProfileField.setFocused(true);
                return true;
            }
        } else if (mouseY >= buttonsY && mouseY < buttonsY + BUTTON_H) {
            naming = true;
            newProfileField = new TextInputField("", "profile name", 32);
            newProfileField.setFocused(true);
            return true;
        }

        int hudY = buttonsY + BUTTON_H + BUTTON_GAP;
        if (mouseY >= hudY && mouseY < hudY + BUTTON_H) {
            adapter.openHudEditor();
            return true;
        }
        return true;
    }

    boolean charTyped(char character) {
        if (naming && newProfileField != null) {
            return newProfileField.charTyped(character);
        }
        return false;
    }

    boolean keyPressed(int glfwKey, ProfileManager profiles) {
        if (!naming || newProfileField == null) {
            return false;
        }
        if (glfwKey == 257 || glfwKey == 335) {
            String name = newProfileField.text().trim();
            if (!name.isEmpty()) {
                try {
                    profiles.switchTo(name);
                } catch (IllegalArgumentException ignored) {
                    // Invalid characters in the name - drop out of naming mode without crashing.
                }
            }
            naming = false;
            newProfileField = null;
            return true;
        }
        if (glfwKey == 256) {
            naming = false;
            newProfileField = null;
            return true;
        }
        return newProfileField.keyPressed(glfwKey);
    }
}
