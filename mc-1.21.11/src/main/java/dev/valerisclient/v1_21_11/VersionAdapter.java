package dev.valerisclient.v1_21_11;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.gui.menu.TitleScreenGate;
import dev.valerisclient.core.util.DirectionUtil;
import dev.valerisclient.v1_21_11.mixin.MinecraftUserAccessor;
import dev.valerisclient.core.account.LauncherAccountStore;
import dev.valerisclient.v1_21_11.screen.AccountSwitcherScreen;
import dev.valerisclient.v1_21_11.screen.SocialHubScreen;
import dev.valerisclient.v1_21_11.screen.PrimeTitleScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.particles.DustParticleOptions;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** {@link MinecraftAdapter} for Minecraft 1.21.11. */
public final class VersionAdapter implements MinecraftAdapter {

    private long sessionStartMillis;

    @Override
    public String minecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isInGame() {
        return Minecraft.getInstance().level != null;
    }

    @Override
    public boolean isScreenOpen() {
        return Minecraft.getInstance().screen != null;
    }

    @Override
    public boolean isKeyDown(int glfwKey) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean isMouseButtonDown(int glfwButton) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetMouseButton(window, glfwButton) == GLFW.GLFW_PRESS;
    }

    @Override
    public int fps() {
        return Minecraft.getInstance().getFps();
    }

    @Override
    public boolean hasPlayer() {
        return Minecraft.getInstance().player != null;
    }

    @Override
    public double playerX() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getX() : 0;
    }

    @Override
    public double playerY() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getY() : 0;
    }

    @Override
    public double playerZ() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getZ() : 0;
    }

    @Override
    public void runOnClientThread(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    @Override
    public void openHudEditor() {
        Minecraft.getInstance().setScreen(new dev.valerisclient.v1_21_11.screen.HudEditorScreen());
    }

    @Override
    public void openClickGui() {
        Minecraft.getInstance().setScreen(new dev.valerisclient.v1_21_11.screen.ClickGuiScreen());
    }

    @Override
    public void openPrimeSettings() {
        ValerisClient.get().clickGui().showSettings();
        Minecraft.getInstance().setScreen(new dev.valerisclient.v1_21_11.screen.ClickGuiScreen());
    }

    @Override
    public void openVanillaTitleScreen() {
        TitleScreenGate.requestVanilla();
        Minecraft.getInstance().setScreen(new TitleScreen());
    }

    @Override
    public void openExternalLink(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void displayClientMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.gui != null) {
                mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(message));
            }
        });
    }

    @Override
    public void openSingleplayer() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen != null ? mc.screen : null;
        mc.setScreen(new SelectWorldScreen(parent));
    }

    @Override
    public void openMultiplayer() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen != null ? mc.screen : null;
        mc.setScreen(new JoinMultiplayerScreen(parent));
    }

    @Override
    public void openOptions() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen != null ? mc.screen : null;
        mc.setScreen(new OptionsScreen(parent, mc.options));
    }

    @Override
    public void openAdvancements() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        mc.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(
                mc.player.connection.getAdvancements()));
    }

    @Override
    public void openStatistics() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Screen parent = mc.screen;
        mc.setScreen(new net.minecraft.client.gui.screens.achievement.StatsScreen(parent, mc.player.getStats()));
    }

    @Override
    public void openFeedbackLink() {
        openExternalLink("https://aka.ms/javafeedback?ref=game");
    }

    @Override
    public void openBugReportLink() {
        openExternalLink("https://aka.ms/snapshotbugs?ref=game");
    }

    @Override
    public void openShareToLan() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer()) {
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server == null || server.isPublished()) {
            return;
        }
        Screen parent = mc.screen;
        mc.setScreen(new net.minecraft.client.gui.screens.ShareToLanScreen(parent));
    }

    @Override
    public void disconnectToTitle() {
        Minecraft mc = Minecraft.getInstance();
        boolean local = mc.isLocalServer();
        if (local) {
            // Shows saving progress then TitleScreen — TitleScreenMixin redirects to Prime.
            mc.disconnectWithSavingScreen();
        } else {
            mc.disconnect(new JoinMultiplayerScreen(new TitleScreen()), false);
        }
    }

    @Override
    public void openAccountSwitcher() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            ValerisClient.get().notifications().warning(
                    "Account",
                    "Return to the main menu before switching accounts.");
            return;
        }
        Screen parent = mc.screen instanceof PrimeTitleScreen ? mc.screen : new PrimeTitleScreen();
        mc.setScreen(new AccountSwitcherScreen(parent));
    }

    @Override
    public void openSocialHub() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        mc.setScreen(new SocialHubScreen(parent));
    }

    @Override
    public void joinMultiplayerServer(String address) {
        if (address == null || address.isBlank()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ServerData data = new ServerData("Party", address.trim(), ServerData.Type.OTHER);
        Screen parent = mc.screen != null ? mc.screen : new JoinMultiplayerScreen(null);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                parent, mc, ServerAddress.parseString(address.trim()), data, false, null);
    }

    @Override
    public String sessionAccountType() {
        Minecraft mc = Minecraft.getInstance();
        User user = mc.getUser();
        if (user == null) {
            return "";
        }
        String token = user.getAccessToken();
        if (token == null || token.isBlank() || "0".equals(token)) {
            return "offline";
        }
        String uuid = user.getProfileId().toString();
        for (LauncherAccountStore.AccountEntry account : LauncherAccountStore.list()) {
            if (uuid.equalsIgnoreCase(account.uuid())) {
                return account.microsoft() ? "microsoft" : "offline";
            }
        }
        return "microsoft";
    }

    @Override
    public void closeAccountSwitcher() {
        Minecraft.getInstance().setScreen(new PrimeTitleScreen());
    }

    @Override
    public boolean applyMinecraftSession(String username, String uuid, String accessToken, boolean microsoft) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null || username == null || username.isBlank() || uuid == null || uuid.isBlank()) {
            return false;
        }
        try {
            UUID id = parseUuid(uuid);
            String token = accessToken == null || accessToken.isBlank() ? "0" : accessToken;
            User user = new User(username, id, token, Optional.empty(), Optional.empty());
            ((MinecraftUserAccessor) (Object) mc).ValerisClient$setUser(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static UUID parseUuid(String raw) {
        String uuid = raw.trim();
        if (!uuid.contains("-") && uuid.length() == 32) {
            uuid = uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-"
                    + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-"
                    + uuid.substring(20, 32);
        }
        return UUID.fromString(uuid);
    }

    @Override
    public void quitGame() {
        Minecraft.getInstance().stop();
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public int ping() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) {
            return 0;
        }
        var info = player.connection.getPlayerInfo(player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    @Override
    public String serverAddress() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server != null ? server.ip : "Singleplayer";
    }

    @Override
    public boolean isMultiplayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && !mc.isLocalServer();
    }

    @Override
    public String playerUuid() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getUUID().toString();
        }
        User user = Minecraft.getInstance().getUser();
        return user != null ? user.getProfileId().toString() : "";
    }

    @Override
    public int onlinePlayerCount() {
        return tabPlayers().size();
    }

    @Override
    public String onlinePlayerUuid(int index) {
        List<PlayerInfo> players = tabPlayers();
        if (index < 0 || index >= players.size()) {
            return "";
        }
        return players.get(index).getProfile().id().toString();
    }

    @Override
    public String onlinePlayerName(int index) {
        List<PlayerInfo> players = tabPlayers();
        if (index < 0 || index >= players.size()) {
            return "";
        }
        return players.get(index).getProfile().name();
    }

    @Override
    public double playerXForUuid(String playerUuid) {
        return playerCoord(playerUuid, Coord.X);
    }

    @Override
    public double playerYForUuid(String playerUuid) {
        return playerCoord(playerUuid, Coord.Y);
    }

    @Override
    public double playerZForUuid(String playerUuid) {
        return playerCoord(playerUuid, Coord.Z);
    }

    private enum Coord { X, Y, Z }

    private double playerCoord(String playerUuid, Coord axis) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || playerUuid == null || playerUuid.isBlank()) {
            return Double.NaN;
        }
        try {
            var entity = mc.level.getPlayerByUUID(UUID.fromString(playerUuid));
            if (entity == null) {
                return Double.NaN;
            }
            return switch (axis) {
                case X -> entity.getX();
                case Y -> entity.getY();
                case Z -> entity.getZ();
            };
        } catch (IllegalArgumentException e) {
            return Double.NaN;
        }
    }

    private List<PlayerInfo> tabPlayers() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) {
            return List.of();
        }
        return new ArrayList<>(player.connection.getOnlinePlayers());
    }

    @Override
    public float playerYaw() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getYRot() : 0;
    }

    @Override
    public float playerPitch() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getXRot() : 0;
    }

    @Override
    public String playerName() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return player.getName().getString();
        }
        var user = Minecraft.getInstance().getUser();
        return user != null ? user.getName() : "";
    }

    @Override
    public float playerHealth() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getHealth() : 0;
    }

    @Override
    public float playerMaxHealth() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getMaxHealth() : 20;
    }

    @Override
    public boolean isSprinting() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isSprinting();
    }

    @Override
    public void setSprinting(boolean sprint) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setSprinting(sprint);
        }
    }

    @Override
    public boolean isSneaking() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isShiftKeyDown();
    }

    @Override
    public void setSneaking(boolean sneak) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setShiftKeyDown(sneak);
        }
    }

    @Override
    public boolean isMovingForward() {
        return Minecraft.getInstance().options.keyUp.isDown();
    }

    @Override
    public boolean isDead() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (player.isDeadOrDying() || Minecraft.getInstance().screen instanceof DeathScreen);
    }

    @Override
    public void respawn() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.respawn();
        }
    }

    @Override
    public boolean hasTarget() {
        return Minecraft.getInstance().crosshairPickEntity != null;
    }

    @Override
    public String targetName() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        return entity != null ? entity.getName().getString() : "";
    }

    @Override
    public float targetHealth() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            return living.getHealth();
        }
        return 0;
    }

    @Override
    public float targetMaxHealth() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            return living.getMaxHealth();
        }
        return 20;
    }

    @Override
    public float attackCooldown() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getAttackStrengthScale(0) : 1.0f;
    }

    @Override
    public float targetDistance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.crosshairPickEntity == null) {
            return 0;
        }
        return mc.player.distanceTo(mc.crosshairPickEntity);
    }

    @Override
    public float playerFallDistance() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? (float) player.fallDistance : 0f;
    }

    @Override
    public boolean playerOnGround() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.onGround();
    }

    @Override
    public boolean playerFallFlying() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isFallFlying();
    }

    @Override
    public boolean playerBlocking() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem().is(Items.SHIELD);
    }

    @Override
    public boolean targetBlocking() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            return player.isUsingItem() && player.getUseItem().is(Items.SHIELD);
        }
        return false;
    }

    @Override
    public int targetShieldDurabilityPercent() {
        var entity = Minecraft.getInstance().crosshairPickEntity;
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return -1;
        }
        ItemStack stack = player.getOffhandItem();
        if (stack.isEmpty() || !stack.is(Items.SHIELD)) {
            stack = player.getMainHandItem();
        }
        if (stack.isEmpty() || !stack.is(Items.SHIELD)) {
            return -1;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        return (max - stack.getDamageValue()) * 100 / max;
    }

    @Override
    public String offhandItemName() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return "";
        }
        ItemStack stack = player.getOffhandItem();
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    @Override
    public int offhandShieldDurabilityPercent() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return -1;
        }
        ItemStack stack = player.getOffhandItem();
        if (stack.isEmpty() || !stack.is(Items.SHIELD)) {
            return -1;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        return (max - stack.getDamageValue()) * 100 / max;
    }

    @Override
    public int playerFoodLevel() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getFoodData().getFoodLevel() : 20;
    }

    @Override
    public float playerSaturation() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getFoodData().getSaturationLevel() : 0;
    }

    @Override
    public long worldDayTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getLevelData().getDayTime() % 24000L : 0L;
    }

    @Override
    public long worldDayNumber() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0L;
        }
        return mc.level.getLevelData().getDayTime() / 24000L;
    }

    @Override
    public double playerHorizontalSpeed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        var velocity = player.getDeltaMovement();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
    }

    @Override
    public boolean autoJump() {
        return Boolean.TRUE.equals(Minecraft.getInstance().options.autoJump().get());
    }

    @Override
    public void setAutoJump(boolean enabled) {
        Minecraft.getInstance().options.autoJump().set(enabled);
    }

    @Override
    public float nearestPrimedTntFuseSeconds() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return -1f;
        }
        float best = -1f;
        double maxDistSq = 32.0 * 32.0;
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt)) {
                continue;
            }
            double distSq = tnt.distanceToSqr(mc.player);
            if (distSq > maxDistSq) {
                continue;
            }
            float fuse = tnt.getFuse() / 20f;
            if (best < 0f || fuse < best) {
                best = fuse;
            }
        }
        return best;
    }

    @Override
    public int nearbyPrimedTntCount() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return 0;
        }
        int count = 0;
        double maxDistSq = 32.0 * 32.0;
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt)) {
                continue;
            }
            if (tnt.distanceToSqr(mc.player) <= maxDistSq) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean worldRaining() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getLevelData().isRaining();
    }

    @Override
    public boolean worldThundering() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getLevelData().isThundering();
    }

    @Override
    public int blockLightLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return 0;
        }
        return mc.level.getMaxLocalRawBrightness(mc.player.blockPosition());
    }

    @Override
    public int playerEffectAmplifier(String effectId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || effectId == null || effectId.isBlank()) {
            return -1;
        }
        for (MobEffectInstance effect : player.getActiveEffects()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .getKey(effect.getEffect().value())
                    .getPath();
            if (effectId.equalsIgnoreCase(id)) {
                return effect.getAmplifier();
            }
        }
        return -1;
    }

    @Override
    public String blockUnderPlayerName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return "";
        }
        var state = mc.level.getBlockState(mc.player.blockPosition().below());
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    @Override
    public int cropGrowthStage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return -1;
        }
        var state = mc.level.getBlockState(mc.player.blockPosition().below());
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3);
        }
        return -1;
    }

    @Override
    public boolean mobSpawnSafeAtFeet() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return true;
        }
        var pos = mc.player.blockPosition();
        if (mc.level.getMaxLocalRawBrightness(pos) <= 7) {
            var below = mc.level.getBlockState(pos.below());
            if (below.isSolidRender()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double[] raycastLookBlock(double maxDistance) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }
        var hit = player.pick(maxDistance, 0f, false);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return null;
        }
        var blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
        var center = blockHit.getBlockPos().getCenter();
        double dist = hit.getLocation().distanceTo(player.getEyePosition());
        return new double[]{center.x, center.y, center.z, dist};
    }

    @Override
    public String heldToolCategory() {
        String name = heldItemName().toLowerCase(Locale.ROOT);
        if (name.contains("sword")) {
            return "sword";
        }
        if (name.contains("axe") && !name.contains("pickaxe")) {
            return "axe";
        }
        if (name.contains("pickaxe")) {
            return "pickaxe";
        }
        if (name.contains("shovel")) {
            return "shovel";
        }
        return "";
    }

    @Override
    public int fireworkRocketCount() {
        return countItemsMatching("firework");
    }

    @Override
    public int heldItemDurabilityPercent() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return -1;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return -1;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        return (max - stack.getDamageValue()) * 100 / max;
    }

    @Override
    public double spawnDistance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        double dx = worldSpawnX() - mc.player.getX();
        double dz = worldSpawnZ() - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public float itemCooldownReady(String key) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 1f;
        }
        Item item = switch (key) {
            case "pearl" -> Items.ENDER_PEARL;
            case "gapple" -> Items.ENCHANTED_GOLDEN_APPLE;
            case "chorus" -> Items.CHORUS_FRUIT;
            case "wind" -> Items.WIND_CHARGE;
            default -> null;
        };
        if (item == null) {
            return 1f;
        }
        return 1f - player.getCooldowns().getCooldownPercent(new ItemStack(item), 0f);
    }

    @Override
    public int countItemsMatching(String filter) {
        if (filter == null || filter.isBlank()) {
            return 0;
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        int total = 0;
        for (int i = 0; i < inventorySlotCount(); i++) {
            String name = inventorySlotItemName(i).toLowerCase(Locale.ROOT);
            if (!name.isEmpty() && name.contains(needle)) {
                total += inventorySlotItemCount(i);
            }
        }
        return total;
    }

    @Override
    public int countHotbarItemsMatching(String filter) {
        if (filter == null || filter.isBlank()) {
            return 0;
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        int total = 0;
        int limit = Math.min(9, inventorySlotCount());
        for (int i = 0; i < limit; i++) {
            String name = inventorySlotItemName(i).toLowerCase(Locale.ROOT);
            if (!name.isEmpty() && name.contains(needle)) {
                total += inventorySlotItemCount(i);
            }
        }
        return total;
    }

    @Override
    public boolean hasArmor(int slot) {
        return !armorStack(slot).isEmpty();
    }

    @Override
    public int armorDurability(int slot) {
        ItemStack stack = armorStack(slot);
        return stack.isEmpty() ? 0 : stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public int armorMaxDurability(int slot) {
        ItemStack stack = armorStack(slot);
        return stack.isEmpty() ? 0 : stack.getMaxDamage();
    }

    @Override
    public int potionEffectCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : player.getActiveEffects().size();
    }

    @Override
    public String potionName(int index) {
        MobEffectInstance effect = effectAt(index);
        return effect != null ? effect.getEffect().value().getDisplayName().getString() : "";
    }

    @Override
    public int potionDurationSeconds(int index) {
        MobEffectInstance effect = effectAt(index);
        return effect != null ? effect.getDuration() / 20 : 0;
    }

    @Override
    public int potionAmplifier(int index) {
        MobEffectInstance effect = effectAt(index);
        return effect != null ? effect.getAmplifier() : 0;
    }

    @Override
    public String heldItemName() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return "";
        }
        ItemStack stack = player.getMainHandItem();
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    @Override
    public int heldItemCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getMainHandItem().getCount() : 0;
    }

    @Override
    public String hoveredItemName() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return "";
        }
        ItemStack stack = player.containerMenu.getCarried();
        if (!stack.isEmpty()) {
            return stack.getHoverName().getString();
        }
        return heldItemName();
    }

    @Override
    public boolean hoveredItemIsShulkerBox() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.getMainHandItem().getItem().toString().contains("shulker_box");
    }

    @Override
    public int shulkerSlotCount() {
        ItemContainerContents contents = shulkerContents();
        if (contents == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack slot : contents.nonEmptyItems()) {
            if (!slot.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String shulkerSlotItem(int index) {
        ItemContainerContents contents = shulkerContents();
        if (contents == null) {
            return "";
        }
        int i = 0;
        for (ItemStack slot : contents.nonEmptyItems()) {
            if (!slot.isEmpty()) {
                if (i == index) {
                    return slot.getHoverName().getString();
                }
                i++;
            }
        }
        return "";
    }

    @Override
    public int shulkerSlotCount(int index) {
        ItemContainerContents contents = shulkerContents();
        if (contents == null) {
            return 0;
        }
        int i = 0;
        for (ItemStack slot : contents.nonEmptyItems()) {
            if (!slot.isEmpty()) {
                if (i == index) {
                    return slot.getCount();
                }
                i++;
            }
        }
        return 0;
    }

    @Override
    public long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    @Override
    public long maxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    @Override
    public void suggestGarbageCollection() {
        System.gc();
    }

    @Override
    public void sendChat(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.connection != null) {
            if (message.startsWith("/")) {
                player.connection.sendCommand(message.substring(1));
            } else {
                player.connection.sendChat(message);
            }
        }
    }

    @Override
    public long sessionMillis() {
        return sessionStartMillis == 0 ? 0 : System.currentTimeMillis() - sessionStartMillis;
    }

    /** Called by the version entrypoint when the player joins a world. */
    public void markSessionStart() {
        sessionStartMillis = System.currentTimeMillis();
    }

    /** Called by the version entrypoint when the player leaves a world. */
    public void markSessionEnd() {
        sessionStartMillis = 0;
    }

    @Override
    public String facingDirection() {
        return DirectionUtil.fromYaw(playerYaw());
    }

    @Override
    public String biomeName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return "";
        }
        return mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("");
    }

    @Override
    public String dimensionId() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.dimension().identifier().getPath() : "overworld";
    }

    @Override
    public double worldSpawnX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getRespawnData().pos().getX() : 0;
    }

    @Override
    public double worldSpawnZ() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getRespawnData().pos().getZ() : 0;
    }

    @Override
    public int playerXpLevel() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.experienceLevel : 0;
    }

    @Override
    public float playerXpProgress() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.experienceProgress : 0;
    }

    @Override
    public String scoreboardTitle() {
        return ScoreboardAccess.title();
    }

    @Override
    public int scoreboardLineCount() {
        return ScoreboardAccess.lineCount();
    }

    @Override
    public String scoreboardLine(int index) {
        return ScoreboardAccess.line(index);
    }

    @Override
    public boolean isContainerScreenOpen() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.containerMenu != player.inventoryMenu;
    }

    @Override
    public int openContainerStorageSlotCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.containerMenu == null) {
            return 0;
        }
        return Math.max(0, player.containerMenu.slots.size() - 36);
    }

    @Override
    public String openContainerSlotItemName(int index) {
        return containerSlot(index, false);
    }

    @Override
    public int openContainerSlotItemCount(int index) {
        ItemStack stack = containerStack(index, false);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    @Override
    public int inventorySlotCount() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getInventory().getContainerSize() : 0;
    }

    @Override
    public String inventorySlotItemName(int index) {
        ItemStack stack = inventoryStack(index);
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    @Override
    public int inventorySlotItemCount(int index) {
        ItemStack stack = inventoryStack(index);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    @Override
    public boolean hasRecentInput() {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                || mc.options.keyJump.isDown() || mc.options.keyShift.isDown()
                || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()
                || isKeyDown(87) || isKeyDown(65) || isKeyDown(83) || isKeyDown(68)
                || isKeyDown(32) || isKeyDown(340) || isKeyDown(341)
                || isMouseButtonDown(0) || isMouseButtonDown(1) || isMouseButtonDown(2);
    }

    @Override
    public int renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    @Override
    public void setRenderDistance(int chunks) {
        Minecraft.getInstance().options.renderDistance().set(chunks);
    }

    @Override
    public int simulationDistance() {
        return Minecraft.getInstance().options.simulationDistance().get();
    }

    @Override
    public void setSimulationDistance(int chunks) {
        Minecraft.getInstance().options.simulationDistance().set(chunks);
    }

    @Override
    public int maxFps() {
        return Minecraft.getInstance().options.framerateLimit().get();
    }

    @Override
    public void setMaxFps(int fps) {
        Minecraft.getInstance().options.framerateLimit().set(fps);
    }

    @Override
    public int particleSetting() {
        return Minecraft.getInstance().options.particles().get().ordinal();
    }

    @Override
    public void setParticleSetting(int setting) {
        ParticleStatus[] values = ParticleStatus.values();
        if (setting >= 0 && setting < values.length) {
            Minecraft.getInstance().options.particles().set(values[setting]);
        }
    }

    @Override
    public boolean cloudsEnabled() {
        return Minecraft.getInstance().options.cloudStatus().get() != net.minecraft.client.CloudStatus.OFF;
    }

    @Override
    public void setCloudsEnabled(boolean enabled) {
        Minecraft.getInstance().options.cloudStatus().set(
                enabled ? net.minecraft.client.CloudStatus.FANCY : net.minecraft.client.CloudStatus.OFF);
    }

    @Override
    public int entityDistance() {
        return (int) (Minecraft.getInstance().options.entityDistanceScaling().get() * 100);
    }

    @Override
    public void setEntityDistance(int percent) {
        Minecraft.getInstance().options.entityDistanceScaling().set(percent / 100.0);
    }

    @Override
    public boolean fancyGraphics() {
        GraphicsPreset preset = Minecraft.getInstance().options.graphicsPreset().get();
        return preset == GraphicsPreset.FANCY || preset == GraphicsPreset.FABULOUS;
    }

    @Override
    public void setFancyGraphics(boolean enabled) {
        Minecraft.getInstance().options.graphicsPreset().set(
                enabled ? GraphicsPreset.FANCY : GraphicsPreset.FAST);
    }

    @Override
    public boolean isWindowFocused() {
        return Minecraft.getInstance().isWindowActive();
    }

    @Override
    public float baseFov() {
        return Minecraft.getInstance().options.fov().get().floatValue();
    }

    @Override
    public int serverListCount() {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        return list.size();
    }

    @Override
    public String serverListName(int index) {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        return index >= 0 && index < list.size() ? list.get(index).name : "";
    }

    @Override
    public String serverListAddress(int index) {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        return index >= 0 && index < list.size() ? list.get(index).ip : "";
    }

    @Override
    public void connectToServer(int index) {
        Minecraft mc = Minecraft.getInstance();
        ServerList list = new ServerList(mc);
        list.load();
        if (index < 0 || index >= list.size()) {
            return;
        }
        ServerData data = list.get(index);
        Screen parent = mc.screen != null ? mc.screen : new JoinMultiplayerScreen(null);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                parent, mc, ServerAddress.parseString(data.ip), data, false, null);
    }

    @Override
    public void takeScreenshot() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.Screenshot.grab(
                mc.gameDirectory,
                mc.getMainRenderTarget(),
                message -> mc.execute(() -> mc.gui.getChat().addMessage(message)));
    }

    @Override
    public boolean captureFrame(Path outputPath) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Files.createDirectories(outputPath.getParent());
            final boolean[] ok = {false};
            net.minecraft.client.Screenshot.takeScreenshot(
                    mc.getMainRenderTarget(),
                    image -> {
                        try {
                            image.writeToFile(outputPath);
                            ok[0] = true;
                        } catch (IOException ignored) {
                        } finally {
                            image.close();
                        }
                    });
            return ok[0] && Files.isRegularFile(outputPath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setHudHidden(boolean hidden) {
        Minecraft.getInstance().options.hideGui = hidden;
    }

    @Override
    public boolean isHudHidden() {
        return Minecraft.getInstance().options.hideGui;
    }

    @Override
    public float gamma() {
        return Minecraft.getInstance().options.gamma().get().floatValue();
    }

    @Override
    public void setGamma(float gamma) {
        Minecraft.getInstance().options.gamma().set((double) Math.clamp(gamma, 0.0F, 1.0F));
    }

    private static ItemStack armorStack(int slot) {
        EquipmentSlot[] slots = {
                EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
        };
        if (slot < 0 || slot >= slots.length) {
            return ItemStack.EMPTY;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(slots[slot]);
    }

    private static MobEffectInstance effectAt(int index) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        var effects = player.getActiveEffects();
        if (index < 0 || index >= effects.size()) {
            return null;
        }
        int i = 0;
        for (MobEffectInstance effect : effects) {
            if (i++ == index) {
                return effect;
            }
        }
        return null;
    }

    private static ItemContainerContents shulkerContents() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        return stack.get(DataComponents.CONTAINER);
    }

    private static String containerSlot(int index, boolean includePlayerInv) {
        ItemStack stack = containerStack(index, includePlayerInv);
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private static ItemStack containerStack(int index, boolean includePlayerInv) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.containerMenu == null || index < 0) {
            return ItemStack.EMPTY;
        }
        int storageSlots = Math.max(0, player.containerMenu.slots.size() - 36);
        int limit = includePlayerInv ? player.containerMenu.slots.size() : storageSlots;
        if (index >= limit) {
            return ItemStack.EMPTY;
        }
        return player.containerMenu.getSlot(index).getItem();
    }

    private static ItemStack inventoryStack(int index) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || index < 0 || index >= player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(index);
    }

    @Override
    public void spawnHitParticles(double x, double y, double z, int color, float size, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int rgb = color & 0xFFFFFF;
        DustParticleOptions options = new DustParticleOptions(rgb, Math.max(0.5f, size));
        var random = mc.level.getRandom();
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.5;
            double oy = (random.nextDouble() - 0.5) * 0.5;
            double oz = (random.nextDouble() - 0.5) * 0.5;
            mc.particleEngine.createParticle(options, x + ox, y + oy, z + oz, 0, 0.03, 0);
        }
    }

    @Override
    public String translate(String key, String fallback, Object... args) {
        try {
            Component component = args.length == 0
                    ? Component.translatable(key)
                    : Component.translatable(key, args);
            String resolved = component.getString();
            if (resolved.equals(key)) {
                return formatFallback(fallback, args);
            }
            return resolved;
        } catch (Exception ignored) {
            return formatFallback(fallback, args);
        }
    }

    private static String formatFallback(String fallback, Object... args) {
        if (args == null || args.length == 0) {
            return fallback;
        }
        try {
            return String.format(Locale.ROOT, fallback, args);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
