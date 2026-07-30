package dev.valerisclient.v26_2.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.state.CustomSkinState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers DynamicTextures for Prime custom skins and builds overridden PlayerSkin values.
 *
 * <p>Important: {@link ClientAsset.ResourceTexture}'s single-arg ctor remaps
 * {@code ns:path} → {@code ns:textures/path.png}. DynamicTexture registrations must use the
 * two-arg ctor with identical {@code id} and {@code texturePath}, matching TextureManager.</p>
 */
public final class CustomSkinTextures {

    private record Entry(Identifier id, String hash, DynamicTexture texture) {
    }

    private static final Map<UUID, Entry> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<String, Entry> BY_HASH = new ConcurrentHashMap<>();
    private static final Set<String> FAILED_HASHES = ConcurrentHashMap.newKeySet();

    private CustomSkinTextures() {
    }

    public static PlayerSkin maybeOverride(UUID uuid, boolean localPlayer, PlayerSkin original) {
        if (uuid == null || original == null) {
            return original;
        }
        byte[] png = CustomSkinState.bytesFor(uuid, localPlayer);
        if (png == null || png.length == 0) {
            return original;
        }
        String hash = localPlayer ? CustomSkinState.localHash() : CustomSkinState.peerHash(uuid);
        if (hash == null || hash.isBlank()) {
            hash = CustomSkinState.hashOf(png);
        }
        if (FAILED_HASHES.contains(hash)) {
            return original;
        }
        Identifier bodyId = ensureRegistered(uuid, hash, png);
        if (bodyId == null) {
            return original;
        }
        // id == texturePath so TextureManager lookup hits the DynamicTexture we registered.
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(bodyId, bodyId);
        return PlayerSkin.insecure(body, original.cape(), original.elytra(), original.model());
    }

    private static Identifier ensureRegistered(UUID uuid, String hash, byte[] png) {
        Entry byHash = BY_HASH.get(hash);
        if (byHash != null) {
            BY_PLAYER.put(uuid, byHash);
            return byHash.id();
        }
        Entry existing = BY_PLAYER.get(uuid);
        if (existing != null && hash.equals(existing.hash())) {
            return existing.id();
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        NativeImage image = null;
        try {
            image = NativeImage.read(new ByteArrayInputStream(png));
            if (!isSupportedSkinSize(image)) {
                ValerisClient.LOGGER.warn(
                        "Custom skin for {} has unsupported size {}x{} (need 64x64 or 64x32)",
                        uuid, image.getWidth(), image.getHeight());
                FAILED_HASHES.add(hash);
                image.close();
                return null;
            }
            Identifier id = Identifier.fromNamespaceAndPath(
                    ValerisClient.MOD_ID, "skins/dynamic/" + uuid.toString().replace("-", ""));
            DynamicTexture texture = new DynamicTexture(
                    () -> "prime-skin-" + hash.substring(0, Math.min(8, hash.length())), image);
            image = null; // ownership transferred to DynamicTexture
            client.getTextureManager().register(id, texture);
            Entry entry = new Entry(id, hash, texture);
            BY_PLAYER.put(uuid, entry);
            BY_HASH.put(hash, entry);
            return id;
        } catch (IOException | RuntimeException e) {
            FAILED_HASHES.add(hash);
            ValerisClient.LOGGER.warn("Failed to upload custom skin for {}: {}", uuid, e.toString());
            if (image != null) {
                image.close();
            }
            return null;
        }
    }

    private static boolean isSupportedSkinSize(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        return w == 64 && (h == 64 || h == 32);
    }
}
