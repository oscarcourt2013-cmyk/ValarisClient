package dev.valerisclient.v1_21_11.screen;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.BlurBackdrop;
import dev.valerisclient.core.gui.options.OptionsMenuLayout;
import dev.valerisclient.core.gui.options.OptionsMenuRenderer;
import dev.valerisclient.core.gui.options.OptionsPage;
import dev.valerisclient.v1_21_11.render.GuiRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Valeris-styled replacement for the vanilla Options screen and its option-list
 * sub-screens. One screen hosts a stack of {@link OptionsPage}s, so "back"
 * unwinds to the parent page and only the outermost page closes the screen.
 */
public final class ValerisOptionsScreen extends Screen {

    private final Screen parent;
    private final OptionsMenuRenderer renderer = new OptionsMenuRenderer();
    private final GuiRenderContext renderContext = new GuiRenderContext();
    private final Deque<OptionsPage> stack = new ArrayDeque<>();

    public ValerisOptionsScreen(Screen parent) {
        super(Component.literal("Valeris Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (stack.isEmpty()) {
            stack.push(ValerisOptionPages.hub(
                    ValerisOptionPages.options(),
                    this::push,
                    () -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options,
                            minecraft.getLanguageManager())),
                    () -> minecraft.setScreen(new PackSelectionScreen(
                            minecraft.getResourcePackRepository(),
                            repo -> minecraft.setScreen(this),
                            minecraft.getResourcePackDirectory(),
                            Component.literal("Resource Packs"))),
                    () -> minecraft.setScreen(
                            new net.minecraft.client.gui.screens.CreditsAndAttributionScreen(this)),
                    () -> minecraft.setScreen(new KeyBindsScreen(this, minecraft.options))));
        }
    }

    private void push(OptionsPage page) {
        stack.push(page);
        renderer.resetScroll();
    }

    private OptionsPage page() {
        return stack.peek();
    }

    private OptionsMenuLayout layout() {
        return OptionsMenuLayout.compute(width, height);
    }

    /** Unwinds one page, or leaves the screen when already at the hub. */
    private void back() {
        if (stack.size() > 1) {
            stack.pop();
            renderer.resetScroll();
            return;
        }
        onClose();
    }

    @Override
    public void tick() {
        renderer.tick(1f / 20f);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BlurBackdrop.setActive(false);
        renderContext.prepare(graphics);
        renderer.render(renderContext, ValerisClient.get().themes().active(), layout(), page(),
                stack.size() > 1 ? "BACK" : "DONE", mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The renderer paints its own scrim; skip vanilla's blur/panorama pass.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (renderer.mousePressed(layout(), page(), event.x(), event.y())) {
            back();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        renderer.mouseDragged(layout(), page(), event.x(), event.y());
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        renderer.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (renderer.mouseScrolled(layout(), page(), verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 256) { // Escape unwinds one level rather than closing outright
            back();
            return true;
        }
        if (renderer.keyPressed(layout(), page(), event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.options.save();
        minecraft.setScreen(parent);
    }
}
