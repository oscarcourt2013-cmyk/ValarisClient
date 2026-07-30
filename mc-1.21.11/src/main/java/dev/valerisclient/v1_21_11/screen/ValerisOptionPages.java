package dev.valerisclient.v1_21_11.screen;

import dev.valerisclient.core.gui.options.OptionRow;
import dev.valerisclient.core.gui.options.OptionsPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

/**
 * Declarative page definitions for the Valeris options screens.
 *
 * <p>Rows close over Minecraft's {@link OptionInstance}s, so the styled renderer in
 * {@code core} drives real vanilla settings without knowing about Minecraft.</p>
 *
 * <p>Language, Resource Packs and Credits stay on their vanilla screens: those are
 * bespoke list / drag-and-drop / scrolling-credits UIs rather than option lists,
 * so they are reached through a styled row flagged {@code stillVanilla}.</p>
 */
final class ValerisOptionPages {

    private ValerisOptionPages() {
    }

    // --- row builders ---

    private static OptionRow toggle(String label, OptionInstance<Boolean> o) {
        return new OptionRow.Toggle(label, () -> Boolean.TRUE.equals(o.get()), o::set);
    }

    private static OptionRow intSlider(String label, OptionInstance<Integer> o, int min, int max,
                                       IntFunction<String> fmt) {
        return new OptionRow.Slider(label,
                () -> fmt.apply(o.get()),
                () -> (o.get() - min) / (double) Math.max(1, max - min),
                f -> o.set((int) Math.round(min + f * (max - min))));
    }

    private static OptionRow doubleSlider(String label, OptionInstance<Double> o, double min, double max,
                                          DoubleFunction<String> fmt) {
        return new OptionRow.Slider(label,
                () -> fmt.apply(o.get()),
                () -> (o.get() - min) / Math.max(1e-6, max - min),
                f -> o.set(min + f * (max - min)));
    }

    private static <E extends Enum<E>> OptionRow cycle(String label, OptionInstance<E> o, E[] values) {
        return new OptionRow.Cycle(label,
                () -> pretty(o.get().name()),
                () -> o.set(values[(o.get().ordinal() + 1) % values.length]));
    }

    private static OptionRow percent(String label, OptionInstance<Double> o) {
        return doubleSlider(label, o, 0d, 1d, v -> Math.round(v * 100) + "%");
    }

    private static OptionRow volume(String label, Options opts, SoundSource source) {
        OptionInstance<Double> o = opts.getSoundSourceOptionInstance(source);
        return doubleSlider(label, o, 0d, 1d,
                v -> v <= 0.001 ? "OFF" : Math.round(v * 100) + "%");
    }

    /** {@code HIGH_CONTRAST} -> {@code High Contrast}. */
    private static String pretty(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p, 1, p.length());
        }
        return sb.toString();
    }

    private static OptionRow modelPart(String label, Options opts, PlayerModelPart part) {
        return new OptionRow.Toggle(label,
                () -> opts.isModelPartEnabled(part),
                v -> opts.setModelPart(part, v));
    }

    // --- pages ---

    static OptionsPage hub(Options opts, Consumer<OptionsPage> open, Runnable openVanillaLanguage,
                           Runnable openVanillaPacks, Runnable openVanillaCredits, Runnable openKeyBinds) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(intSlider("FOV", opts.fov(), 30, 110,
                v -> v == 70 ? "Normal" : (v == 110 ? "Quake Pro" : String.valueOf(v))));
        rows.add(percent("Brightness", opts.gamma()));
        rows.add(new OptionRow.Link("Video Settings", () -> open.accept(video(opts))));
        rows.add(new OptionRow.Link("Controls", () -> open.accept(controls(opts, openKeyBinds))));
        rows.add(new OptionRow.Link("Music & Sounds", () -> open.accept(sounds(opts))));
        rows.add(new OptionRow.Link("Chat Settings", () -> open.accept(chat(opts))));
        rows.add(new OptionRow.Link("Accessibility", () -> open.accept(accessibility(opts))));
        rows.add(new OptionRow.Link("Skin Customization", () -> open.accept(skin(opts))));
        rows.add(new OptionRow.Link("Online", () -> open.accept(online(opts))));
        rows.add(new OptionRow.Link("Telemetry Data", () -> open.accept(telemetry(opts))));
        rows.add(new OptionRow.Link("Language...", openVanillaLanguage, true));
        rows.add(new OptionRow.Link("Resource Packs...", openVanillaPacks, true));
        rows.add(new OptionRow.Link("Credits & Attribution...", openVanillaCredits, true));
        return new OptionsPage("OPTIONS", rows);
    }

    static OptionsPage video(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(cycle("Graphics", opts.graphicsPreset(), net.minecraft.client.GraphicsPreset.values()));
        rows.add(intSlider("Render Distance", opts.renderDistance(), 2, 32, v -> v + " chunks"));
        rows.add(intSlider("Simulation Distance", opts.simulationDistance(), 5, 32, v -> v + " chunks"));
        rows.add(intSlider("Max Framerate", opts.framerateLimit(), 10, 260,
                v -> v >= 260 ? "Unlimited" : v + " fps"));
        rows.add(toggle("VSync", opts.enableVsync()));
        rows.add(intSlider("GUI Scale", opts.guiScale(), 0, 4, v -> v == 0 ? "Auto" : String.valueOf(v)));
        rows.add(cycle("Clouds", opts.cloudStatus(), net.minecraft.client.CloudStatus.values()));
        rows.add(intSlider("Cloud Range", opts.cloudRange(), 8, 192, v -> v + " chunks"));
        rows.add(cycle("Particles", opts.particles(), net.minecraft.server.level.ParticleStatus.values()));
        rows.add(toggle("Entity Shadows", opts.entityShadows()));
        rows.add(doubleSlider("Entity Distance", opts.entityDistanceScaling(), 0.5d, 5d,
                v -> Math.round(v * 100) + "%"));
        rows.add(toggle("Ambient Occlusion", opts.ambientOcclusion()));
        rows.add(toggle("Vignette", opts.vignette()));
        rows.add(toggle("Cutout Leaves", opts.cutoutLeaves()));
        rows.add(toggle("Improved Transparency", opts.improvedTransparency()));
        rows.add(intSlider("Mipmap Levels", opts.mipmapLevels(), 0, 4, String::valueOf));
        rows.add(cycle("Texture Filtering", opts.textureFiltering(),
                net.minecraft.client.TextureFilteringMethod.values()));
        rows.add(intSlider("Biome Blend", opts.biomeBlendRadius(), 0, 7,
                v -> v == 0 ? "Off" : v + " chunks"));
        rows.add(cycle("Chunk Updates", opts.prioritizeChunkUpdates(),
                net.minecraft.client.PrioritizeChunkUpdates.values()));
        rows.add(cycle("Inactivity FPS", opts.inactivityFpsLimit(),
                net.minecraft.client.InactivityFpsLimit.values()));
        rows.add(intSlider("Weather Radius", opts.weatherRadius(), 3, 32, v -> v + " chunks"));
        rows.add(intSlider("Menu Blur", opts.menuBackgroundBlurriness(), 0, 10,
                v -> v == 0 ? "Off" : String.valueOf(v)));
        rows.add(toggle("View Bobbing", opts.bobView()));
        return new OptionsPage("VIDEO SETTINGS", rows);
    }

    static OptionsPage controls(Options opts, Runnable openKeyBinds) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(new OptionRow.Link("Key Binds...", openKeyBinds, true));
        rows.add(percent("Sensitivity", opts.sensitivity()));
        rows.add(toggle("Invert Mouse X", opts.invertMouseX()));
        rows.add(toggle("Invert Mouse Y", opts.invertMouseY()));
        rows.add(percent("Scroll Sensitivity", opts.mouseWheelSensitivity()));
        rows.add(toggle("Discrete Scroll", opts.discreteMouseScroll()));
        rows.add(toggle("Raw Input", opts.rawMouseInput()));
        rows.add(toggle("Auto-Jump", opts.autoJump()));
        rows.add(toggle("Toggle Sneak", opts.toggleCrouch()));
        rows.add(toggle("Toggle Sprint", opts.toggleSprint()));
        rows.add(toggle("Toggle Attack", opts.toggleAttack()));
        rows.add(toggle("Toggle Use", opts.toggleUse()));
        rows.add(intSlider("Sprint Window", opts.sprintWindow(), 0, 20, String::valueOf));
        rows.add(cycle("Main Hand", opts.mainHand(),
                net.minecraft.world.entity.HumanoidArm.values()));
        rows.add(cycle("Attack Indicator", opts.attackIndicator(),
                net.minecraft.client.AttackIndicatorStatus.values()));
        rows.add(toggle("Operator Items Tab", opts.operatorItemsTab()));
        rows.add(toggle("Rotate With Minecart", opts.rotateWithMinecart()));
        rows.add(toggle("Allow Cursor Changes", opts.allowCursorChanges()));
        return new OptionsPage("CONTROLS", rows);
    }

    static OptionsPage sounds(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(volume("Master Volume", opts, SoundSource.MASTER));
        rows.add(volume("Music", opts, SoundSource.MUSIC));
        rows.add(volume("Jukebox / Note Blocks", opts, SoundSource.RECORDS));
        rows.add(volume("Weather", opts, SoundSource.WEATHER));
        rows.add(volume("Blocks", opts, SoundSource.BLOCKS));
        rows.add(volume("Hostile Creatures", opts, SoundSource.HOSTILE));
        rows.add(volume("Friendly Creatures", opts, SoundSource.NEUTRAL));
        rows.add(volume("Players", opts, SoundSource.PLAYERS));
        rows.add(volume("Ambient / Environment", opts, SoundSource.AMBIENT));
        rows.add(volume("Voice / Speech", opts, SoundSource.VOICE));
        rows.add(volume("UI", opts, SoundSource.UI));
        rows.add(toggle("Show Subtitles", opts.showSubtitles()));
        rows.add(cycle("Music Frequency", opts.musicFrequency(),
                net.minecraft.client.sounds.MusicManager.MusicFrequency.values()));
        rows.add(cycle("Music Toast", opts.musicToast(),
                net.minecraft.client.MusicToastDisplayState.values()));
        return new OptionsPage("MUSIC & SOUNDS", rows);
    }

    static OptionsPage chat(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(cycle("Chat Visibility", opts.chatVisibility(),
                net.minecraft.world.entity.player.ChatVisiblity.values()));
        rows.add(toggle("Colors", opts.chatColors()));
        rows.add(toggle("Web Links", opts.chatLinks()));
        rows.add(toggle("Prompt On Links", opts.chatLinksPrompt()));
        rows.add(percent("Opacity", opts.chatOpacity()));
        rows.add(percent("Text Background", opts.textBackgroundOpacity()));
        rows.add(doubleSlider("Scale", opts.chatScale(), 0d, 1d,
                v -> v <= 0.001 ? "Off" : Math.round(v * 100) + "%"));
        rows.add(percent("Line Spacing", opts.chatLineSpacing()));
        rows.add(percent("Width", opts.chatWidth()));
        rows.add(percent("Focused Height", opts.chatHeightFocused()));
        rows.add(percent("Unfocused Height", opts.chatHeightUnfocused()));
        rows.add(doubleSlider("Delay", opts.chatDelay(), 0d, 6d,
                v -> v <= 0.001 ? "None" : String.format(Locale.ROOT, "%.1fs", v)));
        rows.add(toggle("Command Suggestions", opts.autoSuggestions()));
        rows.add(toggle("Hide Matched Names", opts.hideMatchedNames()));
        rows.add(toggle("Only Secure Chat", opts.onlyShowSecureChat()));
        rows.add(toggle("Save Chat Drafts", opts.saveChatDrafts()));
        rows.add(toggle("Reduced Debug Info", opts.reducedDebugInfo()));
        return new OptionsPage("CHAT SETTINGS", rows);
    }

    static OptionsPage accessibility(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(cycle("Narrator", opts.narrator(), net.minecraft.client.NarratorStatus.values()));
        rows.add(toggle("Narrator Hotkey", opts.narratorHotkey()));
        rows.add(toggle("High Contrast", opts.highContrast()));
        rows.add(toggle("High Contrast Outline", opts.highContrastBlockOutline()));
        rows.add(percent("Damage Tilt", opts.damageTiltStrength()));
        rows.add(percent("Distortion Effects", opts.screenEffectScale()));
        rows.add(percent("FOV Effects", opts.fovEffectScale()));
        rows.add(percent("Darkness Pulsing", opts.darknessEffectScale()));
        rows.add(percent("Glint Speed", opts.glintSpeed()));
        rows.add(percent("Glint Strength", opts.glintStrength()));
        rows.add(toggle("Hide Lightning Flashes", opts.hideLightningFlash()));
        rows.add(toggle("Hide Splash Texts", opts.hideSplashTexts()));
        rows.add(percent("Panorama Speed", opts.panoramaSpeed()));
        rows.add(doubleSlider("Notification Time", opts.notificationDisplayTime(), 0.5d, 10d,
                v -> String.format(Locale.ROOT, "%.1fx", v)));
        rows.add(toggle("Autosave Indicator", opts.showAutosaveIndicator()));
        rows.add(toggle("Dark Loading Screen", opts.darkMojangStudiosBackground()));
        rows.add(toggle("Force Unicode Font", opts.forceUnicodeFont()));
        rows.add(toggle("Japanese Glyph Variants", opts.japaneseGlyphVariants()));
        return new OptionsPage("ACCESSIBILITY", rows);
    }

    static OptionsPage skin(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        for (PlayerModelPart part : PlayerModelPart.values()) {
            rows.add(modelPart(pretty(part.name()), opts, part));
        }
        rows.add(cycle("Main Hand", opts.mainHand(),
                net.minecraft.world.entity.HumanoidArm.values()));
        return new OptionsPage("SKIN CUSTOMIZATION", rows);
    }

    static OptionsPage online(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(toggle("Allow Server Listing", opts.allowServerListing()));
        rows.add(toggle("Realms Notifications", opts.realmsNotifications()));
        rows.add(toggle("Only Secure Chat", opts.onlyShowSecureChat()));
        return new OptionsPage("ONLINE", rows);
    }

    static OptionsPage telemetry(Options opts) {
        List<OptionRow> rows = new ArrayList<>();
        rows.add(toggle("Send Optional Data", opts.telemetryOptInExtra()));
        rows.add(new OptionRow.Info("Required Data", () -> "Always sent by Minecraft"));
        rows.add(new OptionRow.Info("Collected By", () -> "Mojang / Microsoft"));
        return new OptionsPage("TELEMETRY DATA", rows);
    }

    static Options options() {
        return Minecraft.getInstance().options;
    }
}
