package dev.stellarclient.v26_2.render;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** {@link RenderContext} over 26.2's extract-based GUI pipeline. */
public final class GuiRenderContext implements RenderContext {

    private GuiGraphicsExtractor extractor;
    private Font font;
    private float drawOpacity = 1f;
    private int clipDepth;

    public void prepare(GuiGraphicsExtractor extractor) {
        this.extractor = extractor;
        this.font = Minecraft.getInstance().font;
        this.drawOpacity = 1f;
        this.clipDepth = 0;
    }

    @Override
    public int screenWidth() {
        return extractor.guiWidth();
    }

    @Override
    public int screenHeight() {
        return extractor.guiHeight();
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        extractor.fill(x, y, x + width, y + height, applyOpacity(argb));
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        extractor.text(font, text, x, y, applyOpacity(argb), shadow);
    }

    @Override
    public void drawUiText(String text, int x, int y, int argb) {
        extractor.text(font, text, x, y, applyOpacity(argb), false);
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
        extractor.fillGradient(x, y, x + width, y + height,
                applyOpacity(topArgb), applyOpacity(bottomArgb));
    }

    @Override
    public void pushTransform(float translateX, float translateY, float scale,
                              float rotationDegrees, float pivotLocalX, float pivotLocalY) {
        var pose = extractor.pose();
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
        extractor.pose().popMatrix();
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
        int sw = screenWidth();
        int sh = screenHeight();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(sw, x + width);
        int y1 = Math.min(sh, y + height);
        if (x1 > x0 && y1 > y0) {
            extractor.enableScissor(x0, y0, x1, y1);
            clipDepth++;
        }
    }

    @Override
    public void popClip() {
        if (clipDepth > 0) {
            extractor.disableScissor();
            clipDepth--;
        }
    }

    @Override
    public void drawTexture(String texturePath, int x, int y, int width, int height,
                              int textureWidth, int textureHeight, int tintArgb) {
        Identifier id = Identifier.fromNamespaceAndPath(StellarClient.MOD_ID, normalizeTexturePath(texturePath));
        extractor.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, width, height,
                textureWidth, textureHeight, textureTint(tintArgb));
    }

    @Override
    public void drawArmorItem(int slot, int x, int y) {
        ItemStack stack = armorStack(slot);
        if (stack.isEmpty()) {
            return;
        }
        extractor.item(stack, x, y);
    }

    @Override
    public void drawArmorGhost(int slot, int x, int y) {
        ItemStack ghost = armorGhost(slot);
        if (ghost.isEmpty()) {
            return;
        }
        extractor.fakeItem(ghost, x, y);
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

    private static String normalizeTexturePath(String texturePath) {
        return texturePath.replace('\\', '/');
    }

    private int applyOpacity(int argb) {
        return drawOpacity >= 0.999f ? argb : ColorUtil.withAlpha(argb, drawOpacity);
    }

    private int textureTint(int argb) {
        int tinted = applyOpacity(argb);
        if (((tinted >>> 24) & 0xFF) == 0) {
            tinted |= 0xFF000000;
        }
        return tinted;
    }
}
