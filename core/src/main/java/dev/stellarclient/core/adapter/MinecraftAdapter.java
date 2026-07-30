package dev.stellarclient.core.adapter;

import java.nio.file.Path;

/**
 * Version-independent gateway to Minecraft.
 *
 * <p>This is the only door between the common core and the game. Each
 * supported Minecraft version ships its own implementation in its version
 * layer. Core code must never reference Minecraft classes directly.</p>
 *
 * <p>New methods use {@code default} implementations so test doubles stay
 * minimal. Version layers override what they support.</p>
 */
public interface MinecraftAdapter {

    String minecraftVersion();

    Path gameDirectory();

    Path configDirectory();

    boolean isInGame();

    boolean isScreenOpen();

    boolean isKeyDown(int glfwKey);

    boolean isMouseButtonDown(int glfwButton);

    int fps();

    boolean hasPlayer();

    double playerX();

    double playerY();

    double playerZ();

    void runOnClientThread(Runnable task);

    void openHudEditor();

    void openClickGui();

    /** Opens ClickGUI directly on the settings view. */
    default void openPrimeSettings() {
        openClickGui();
    }

    /** Opens the vanilla Minecraft title screen (without Prime redirect). */
    default void openVanillaTitleScreen() {
    }

    /** Opens a URL in the system browser. */
    default void openExternalLink(String url) {
    }

    /** Shows a client-only chat line (not sent to the server). */
    default void displayClientMessage(String message) {
    }

    /** Opens the vanilla world selection screen from the title menu. */
    default void openSingleplayer() {
    }

    /** Opens the vanilla multiplayer browser from the title menu. */
    default void openMultiplayer() {
    }

    /** Opens vanilla game options from the title menu. */
    default void openOptions() {
    }

    /** Opens the advancements screen from the pause menu. */
    default void openAdvancements() {
    }

    /** Opens the statistics screen from the pause menu. */
    default void openStatistics() {
    }

    /** Opens Mojang / Minecraft feedback site. */
    default void openFeedbackLink() {
    }

    /** Opens the Minecraft bug tracker. */
    default void openBugReportLink() {
    }

    /** Opens the Share to LAN screen when available. */
    default void openShareToLan() {
    }

    /** Disconnects and returns to the title screen (save & quit / disconnect). */
    default void disconnectToTitle() {
    }

    /** Opens the in-game Minecraft account switcher (title menu). */
    default void openAccountSwitcher() {
    }

    /** Opens the in-game social hub overlay (pause menu). */
    default void openSocialHub() {
    }

    /** Connects to a multiplayer server by address (e.g. party join). */
    default void joinMultiplayerServer(String address) {
    }

    /** Active session type: {@code microsoft}, {@code offline}, or empty when unknown. */
    default String sessionAccountType() {
        return "";
    }

    /** Closes the account switcher and returns to the Prime title menu. */
    default void closeAccountSwitcher() {
    }

    /**
     * Replaces the active Minecraft {@code User} session without restarting the client.
     * Only safe from the title screen (no world loaded).
     */
    default boolean applyMinecraftSession(String username, String uuid, String accessToken, boolean microsoft) {
        return false;
    }

    /** Closes the game from the title menu. */
    default void quitGame() {
    }

    /** Closes the active screen and returns to the game. */
    default void closeCurrentScreen() {
    }

    /** Multiplayer latency in ms, or {@code 0} when unavailable. */
    default int ping() {
        return 0;
    }

    /** Current server address, or {@code "Singleplayer"}. */
    default String serverAddress() {
        return "";
    }

    /** {@code true} when connected to a remote multiplayer server. */
    default boolean isMultiplayer() {
        return false;
    }

    /** Local player UUID string, or empty when unavailable. */
    default String playerUuid() {
        return "";
    }

    /** Players visible in the tab list (multiplayer). */
    default int onlinePlayerCount() {
        return 0;
    }

    default String onlinePlayerUuid(int index) {
        return "";
    }

    default String onlinePlayerName(int index) {
        return "";
    }

    /** Distance in blocks to another player, or {@link Double#POSITIVE_INFINITY}. */
    default double distanceToPlayer(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = playerXForUuid(playerUuid) - playerX();
        double dy = playerYForUuid(playerUuid) - playerY();
        double dz = playerZForUuid(playerUuid) - playerZ();
        if (!Double.isFinite(dx)) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** World X for a player UUID, or {@link Double#NaN} when unknown. */
    default double playerXForUuid(String playerUuid) {
        return Double.NaN;
    }

    default double playerYForUuid(String playerUuid) {
        return Double.NaN;
    }

    default double playerZForUuid(String playerUuid) {
        return Double.NaN;
    }

    // --- player ---

    default float playerYaw() {
        return 0;
    }

    default float playerPitch() {
        return 0;
    }

    default String playerName() {
        return "";
    }

    default float playerHealth() {
        return 0;
    }

    default float playerMaxHealth() {
        return 20;
    }

    default boolean isSprinting() {
        return false;
    }

    default void setSprinting(boolean sprint) {
    }

    default boolean isSneaking() {
        return false;
    }

    default void setSneaking(boolean sneak) {
    }

    /** Whether the forward movement key is held. */
    default boolean isMovingForward() {
        return false;
    }

    default boolean isDead() {
        return false;
    }

    default void respawn() {
    }

    // --- combat / target ---

    default boolean hasTarget() {
        return false;
    }

    default String targetName() {
        return "";
    }

    default float targetHealth() {
        return 0;
    }

    default float targetMaxHealth() {
        return 20;
    }

    default float attackCooldown() {
        return 1.0f;
    }

    /** Distance in blocks to crosshair target, or {@code 0} when none. */
    default float targetDistance() {
        return 0;
    }

    /** Blocks fallen since last landing — used for mace smash and crits. */
    default float playerFallDistance() {
        return 0;
    }

    default boolean playerOnGround() {
        return true;
    }

    default boolean playerFallFlying() {
        return false;
    }

    /** {@code true} while actively blocking with a shield. */
    default boolean playerBlocking() {
        return false;
    }

    /** {@code true} when crosshair target is blocking with a shield. */
    default boolean targetBlocking() {
        return false;
    }

    /** Shield durability percent of crosshair target, or {@code -1}. */
    default int targetShieldDurabilityPercent() {
        return -1;
    }

    default String offhandItemName() {
        return "";
    }

    /** Shield durability percent in offhand, or {@code -1} if none. */
    default int offhandShieldDurabilityPercent() {
        return -1;
    }

    /** Food level 0–20. */
    default int playerFoodLevel() {
        return 20;
    }

    /**
     * Item cooldown readiness 0–1 ({@code 1} = ready).
     * Keys: {@code pearl}, {@code gapple}, {@code chorus}, {@code wind}.
     */
    default float itemCooldownReady(String key) {
        return 1f;
    }

    /** Count inventory stacks whose name contains {@code filter} (case-insensitive). */
    default int countItemsMatching(String filter) {
        return 0;
    }

    /** Count items in hotbar slots 0–8 matching {@code filter}. */
    default int countHotbarItemsMatching(String filter) {
        return 0;
    }

    // --- armor (0=boots, 1=leggings, 2=chest, 3=helmet) ---

    default int armorSlotCount() {
        return 4;
    }

    default boolean hasArmor(int slot) {
        return false;
    }

    default int armorDurability(int slot) {
        return 0;
    }

    default int armorMaxDurability(int slot) {
        return 0;
    }

    // --- potion effects ---

    default int potionEffectCount() {
        return 0;
    }

    default String potionName(int index) {
        return "";
    }

    default int potionDurationSeconds(int index) {
        return 0;
    }

    default int potionAmplifier(int index) {
        return 0;
    }

    // --- items ---

    default String heldItemName() {
        return "";
    }

    default int heldItemCount() {
        return 0;
    }

    default String hoveredItemName() {
        return "";
    }

    default boolean hoveredItemIsShulkerBox() {
        return false;
    }

    default int shulkerSlotCount() {
        return 0;
    }

    default String shulkerSlotItem(int index) {
        return "";
    }

    default int shulkerSlotCount(int index) {
        return 0;
    }

    default float playerSaturation() {
        return 0;
    }

    /** Day time 0–23999. */
    default long worldDayTime() {
        return 0;
    }

    /** Absolute world day index (day 0 = first day). */
    default long worldDayNumber() {
        return 0;
    }

    /**
     * Horizontal speed in metres per second (blocks/s), derived from velocity.
     * Vertical motion is ignored.
     */
    default double playerHorizontalSpeed() {
        return 0;
    }

    /** Vanilla auto-jump option. */
    default boolean autoJump() {
        return false;
    }

    default void setAutoJump(boolean enabled) {
    }

    /**
     * Fuse remaining on the nearest primed TNT within range, in seconds.
     * Returns {@code -1} when none are nearby or entity access is unavailable.
     */
    default float nearestPrimedTntFuseSeconds() {
        return -1f;
    }

    /** Count of primed TNT entities near the player, or {@code -1} when unsupported. */
    default int nearbyPrimedTntCount() {
        return -1;
    }

    default boolean worldRaining() {
        return false;
    }

    default boolean worldThundering() {
        return false;
    }

    /** Block light 0–15 at the player's feet. */
    default int blockLightLevel() {
        return 0;
    }

    /** Effect amplifier for {@code effectId} (e.g. {@code bad_omen}), or {@code -1}. */
    default int playerEffectAmplifier(String effectId) {
        return -1;
    }

    /** Registry path of block at the player's feet, or empty. */
    default String blockUnderPlayerName() {
        return "";
    }

    /** Crop age 0–7, or {@code -1} when the block below is not a crop. */
    default int cropGrowthStage() {
        return -1;
    }

    /** {@code true} when mob spawning is unlikely at the player's feet. */
    default boolean mobSpawnSafeAtFeet() {
        return blockLightLevel() > 7;
    }

    /** Look-direction block raycast: {@code [x, y, z, distance]} or {@code null}. */
    default double[] raycastLookBlock(double maxDistance) {
        return null;
    }

    /** Held tool category: {@code sword}, {@code axe}, {@code pickaxe}, {@code shovel}, or empty. */
    default String heldToolCategory() {
        return "";
    }

    /** Firework rocket count in inventory. */
    default int fireworkRocketCount() {
        return countItemsMatching("firework");
    }

    /** Durability percent for held item, or {@code -1}. */
    default int heldItemDurabilityPercent() {
        return -1;
    }

    /** Horizontal distance to world spawn. */
    default double spawnDistance() {
        return 0;
    }

    // --- memory ---

    default long usedMemoryMb() {
        return 0;
    }

    default long maxMemoryMb() {
        return 0;
    }

    default void suggestGarbageCollection() {
    }

    // --- chat ---

    default void sendChat(String message) {
    }

    // --- session ---

    /** Milliseconds since the player joined the current world. */
    default long sessionMillis() {
        return 0;
    }

    // --- world ---

    default String facingDirection() {
        return "N";
    }

    default String biomeName() {
        return "";
    }

    /** Current dimension id path, e.g. {@code overworld} or {@code the_nether}. */
    default String dimensionId() {
        return "overworld";
    }

    default double worldSpawnX() {
        return 0;
    }

    default double worldSpawnZ() {
        return 0;
    }

    default int playerXpLevel() {
        return 0;
    }

    default float playerXpProgress() {
        return 0;
    }

    /** Sidebar scoreboard title, or empty when unavailable. */
    default String scoreboardTitle() {
        return "";
    }

    default int scoreboardLineCount() {
        return 0;
    }

    /** Formatted sidebar line {@code index} (0 = top), or empty. */
    default String scoreboardLine(int index) {
        return "";
    }

    /** Whether a container screen (chest, hopper, etc.) is open. */
    default boolean isContainerScreenOpen() {
        return false;
    }

    /** Storage slots in the open container (excludes player inventory). */
    default int openContainerStorageSlotCount() {
        return 0;
    }

    default String openContainerSlotItemName(int index) {
        return "";
    }

    default int openContainerSlotItemCount(int index) {
        return 0;
    }

    default int inventorySlotCount() {
        return 0;
    }

    default String inventorySlotItemName(int index) {
        return "";
    }

    default int inventorySlotItemCount(int index) {
        return 0;
    }

    /** Whether any movement, mouse, or keyboard input occurred recently. */
    default boolean hasRecentInput() {
        return isKeyDown(87) || isKeyDown(65) || isKeyDown(83) || isKeyDown(68)
                || isKeyDown(32) || isKeyDown(340) || isKeyDown(341)
                || isMouseButtonDown(0) || isMouseButtonDown(1) || isMouseButtonDown(2);
    }

    // --- options / performance ---

    default int renderDistance() {
        return 12;
    }

    default void setRenderDistance(int chunks) {
    }

    default int simulationDistance() {
        return 12;
    }

    default void setSimulationDistance(int chunks) {
    }

    default int maxFps() {
        return 120;
    }

    default void setMaxFps(int fps) {
    }

    /** 0 = all, 1 = decreased, 2 = minimal. */
    default int particleSetting() {
        return 0;
    }

    default void setParticleSetting(int setting) {
    }

    default boolean cloudsEnabled() {
        return true;
    }

    default void setCloudsEnabled(boolean enabled) {
    }

    default int entityDistance() {
        return 100;
    }

    default void setEntityDistance(int percent) {
    }

    default boolean fancyGraphics() {
        return true;
    }

    default void setFancyGraphics(boolean enabled) {
    }

    default boolean isWindowFocused() {
        return true;
    }

    // --- camera / FOV ---

    default float baseFov() {
        return 70;
    }

    // --- servers ---

    default int serverListCount() {
        return 0;
    }

    default String serverListName(int index) {
        return "";
    }

    default String serverListAddress(int index) {
        return "";
    }

    default void connectToServer(int index) {
    }

    // --- screenshot / HUD ---

    default void takeScreenshot() {
    }

    /** Writes the current framebuffer to {@code outputPath} (PNG). Used by clip recorder. */
    default boolean captureFrame(java.nio.file.Path outputPath) {
        return false;
    }

    default void setHudHidden(boolean hidden) {
    }

    default boolean isHudHidden() {
        return false;
    }

    /** Vanilla brightness slider value (0.0–1.0). */
    default float gamma() {
        return 0.5F;
    }

    /** Sets the vanilla brightness slider (0.0–1.0). */
    default void setGamma(float gamma) {
    }

    /** Spawns client-side hit particles at world coordinates. */
    default void spawnHitParticles(double x, double y, double z, int color, float size, int count) {
    }

    /** Resolves a StellarClient translation key using the active Minecraft language. */
    default String translate(String key, String fallback, Object... args) {
        if (args == null || args.length == 0) {
            return fallback;
        }
        try {
            return String.format(java.util.Locale.ROOT, fallback, args);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
