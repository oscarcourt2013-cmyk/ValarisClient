package dev.valerisclient.v1_21_11.render;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** {@link RenderContext} over 1.21.11's immediate-mode {@link GuiGraphics}. */
public final class GuiRenderContext implements RenderContext {

    private GuiGraphics graphics;
    private Font font;
    private int screenWidth;
    private int screenHeight;
    private float drawOpacity = 1f;
    private int clipDepth;

    public void prepare(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        this.graphics = graphics;
        this.font = minecraft.font;
        this.screenWidth = minecraft.getWindow().getGuiScaledWidth();
        this.screenHeight = minecraft.getWindow().getGuiScaledHeight();
        this.drawOpacity = 1f;
        this.clipDepth = 0;
    }

    @Override
    public int screenWidth() {
        return screenWidth;
    }

    @Override
    public int screenHeight() {
        return screenHeight;
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, applyOpacity(argb));
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, applyOpacity(argb), shadow);
    }

    @Override
    public void drawUiText(String text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, applyOpacity(argb), false);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int uiTextWidth(String text) {
        return font.width(text);
    }

    @Override
    public int fontHeight() {
        return font.lineHeight;
    }

    @Override
    public int uiFontHeight() {
        return font.lineHeight;
    }

    @Override
    public void fillGradientVertical(int x, int y, int width, int height, int topArgb, int bottomArgb) {
        graphics.fillGradient(x, y, x + width, y + height, applyOpacity(topArgb), applyOpacity(bottomArgb));
    }

    @Override
    public void pushTransform(float translateX, float translateY, float scale,
                              float rotationDegrees, float pivotLocalX, float pivotLocalY) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(translateX, translateY);
        if (rotationDegrees != 0f) {
            pose.rotate((float) Math.toRadians(rotationDegrees));
        }
        pose.scale(scale, scale);
        if (pivotLocalX != 0f || pivotLocalY != 0f) {
            pose.translate(-pivotLocalX, -pivotLocalY);
        }
    }

    @Override
    public void popTransform() {
        graphics.pose().popMatrix();
    }

    @Override
    public void setDrawOpacity(float opacity) {
        this.drawOpacity = Math.clamp(opacity, 0f, 1f);
    }

    @Override
    public void pushClip(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(screenWidth, x + width);
        int y1 = Math.min(screenHeight, y + height);
        if (x1 > x0 && y1 > y0) {
            graphics.enableScissor(x0, y0, x1, y1);
            clipDepth++;
        }
    }

    @Override
    public void popClip() {
        if (clipDepth > 0) {
            graphics.disableScissor();
            clipDepth--;
        }
    }

    @Override
    public void drawTexture(String texturePath, int x, int y, int width, int height,
                              int textureWidth, int textureHeight, int tintArgb) {
        Identifier id = Identifier.fromNamespaceAndPath(ValerisClient.MOD_ID, normalizeTexturePath(texturePath));
        // 1.21.6+ treats blit tint as ARGB — always pass an explicit opaque color (no-color overload breaks alpha).
        graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, width, height,
                textureWidth, textureHeight, textureTint(tintArgb));
    }

    @Override
    public void drawArmorItem(int slot, int x, int y) {
        ItemStack stack = armorStack(slot);
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
    }

    @Override
    public void drawArmorGhost(int slot, int x, int y) {
        ItemStack ghost = armorGhost(slot);
        if (ghost.isEmpty()) {
            return;
        }
        graphics.renderFakeItem(ghost, x, y);
    }

    private static ItemStack armorStack(int slot) {
        EquipmentSlot equipment = equipmentSlot(slot);
        if (equipment == null) {
            return ItemStack.EMPTY;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(equipment);
    }

    private static final ItemStack GHOST_BOOTS = new ItemStack(Items.CHAINMAIL_BOOTS);
    private static final ItemStack GHOST_LEGGINGS = new ItemStack(Items.CHAINMAIL_LEGGINGS);
    private static final ItemStack GHOST_CHEST = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
    private static final ItemStack GHOST_HELMET = new ItemStack(Items.CHAINMAIL_HELMET);

    private static ItemStack armorGhost(int slot) {
        return switch (slot) {
            case 0 -> GHOST_BOOTS;
            case 1 -> GHOST_LEGGINGS;
            case 2 -> GHOST_CHEST;
            case 3 -> GHOST_HELMET;
            default -> ItemStack.EMPTY;
        };
    }

    private static EquipmentSlot equipmentSlot(int slot) {
        return switch (slot) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> null;
        };
    }

    /** Fabric/MC GUI textures use {@code textures/...} paths in {@link Identifier}s. */
    private static String normalizeTexturePath(String texturePath) {
        return texturePath.replace('\\', '/');
    }

    private int applyOpacity(int argb) {
        return drawOpacity >= 0.999f ? argb : ColorUtil.withAlpha(argb, drawOpacity);
    }

    /** 1.21.6+ blit tint is ARGB — RGB-only tints need an explicit alpha channel. */
    private int textureTint(int argb) {
        int tinted = applyOpacity(argb);
        if (((tinted >>> 24) & 0xFF) == 0) {
            tinted |= 0xFF000000;
        }
        return tinted;
    }
}
