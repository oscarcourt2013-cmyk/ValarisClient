package dev.stellarclient.core.state;

import dev.stellarclient.core.cosmetics.CosmeticLoadout;
import dev.stellarclient.core.cosmetics.CosmeticTextures;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Client-side cosmetic overrides, read by version-layer render hooks. */
public final class CosmeticsState {

    /** Prefer cape IDs via {@link #localLoadout()}. Kept for older callers. */
    @Deprecated
    public enum CapeStyle {
        NONE,
        PRIME,
        STAR
    }

    private static CosmeticLoadout localLoadout = CosmeticLoadout.EMPTY;
    private static final Map<UUID, CosmeticLoadout> peers = new ConcurrentHashMap<>();
    private static final AtomicBoolean announceDirty = new AtomicBoolean(false);

    private CosmeticsState() {
    }

    public static CosmeticLoadout localLoadout() {
        return localLoadout;
    }

    public static String localCapeId() {
        return localLoadout.capeId();
    }

    public static String localWingsId() {
        return localLoadout.wingsId();
    }

    public static void setLocalLoadout(String capeId, String wingsId) {
        CosmeticLoadout next = new CosmeticLoadout(
                CosmeticTextures.isKnownCape(capeId) ? capeId : "",
                CosmeticTextures.isKnownWings(wingsId) ? wingsId : "");
        if (!next.equals(localLoadout)) {
            localLoadout = next;
            announceDirty.set(true);
        }
    }

    /** Loadout for rendering: local player always uses local equipment. */
    public static CosmeticLoadout loadoutFor(UUID uuid, boolean localPlayer) {
        if (localPlayer) {
            return localLoadout;
        }
        if (uuid == null) {
            return CosmeticLoadout.EMPTY;
        }
        return peers.getOrDefault(uuid, CosmeticLoadout.EMPTY);
    }

    public static void setPeerLoadout(UUID uuid, String capeId, String wingsId) {
        if (uuid == null) {
            return;
        }
        peers.put(uuid, new CosmeticLoadout(
                CosmeticTextures.isKnownCape(capeId) ? capeId : "",
                CosmeticTextures.isKnownWings(wingsId) ? wingsId : ""));
    }

    public static void clearPeers() {
        peers.clear();
    }

    public static boolean consumeAnnounceDirty() {
        return announceDirty.getAndSet(false);
    }

    public static void markAnnounceDirty() {
        announceDirty.set(true);
    }

    public static CapeStyle capeStyle() {
        String id = localLoadout.capeId();
        if (id.isBlank()) {
            return CapeStyle.NONE;
        }
        if ("cape-star".equals(id)) {
            return CapeStyle.STAR;
        }
        return CapeStyle.PRIME;
    }

    public static int accentTint() {
        return 0;
    }

    public static void setCapeStyle(CapeStyle style) {
        if (style == null || style == CapeStyle.NONE) {
            setLocalLoadout("", localLoadout.wingsId());
        } else if (style == CapeStyle.STAR) {
            setLocalLoadout("cape-star", localLoadout.wingsId());
        } else {
            setLocalLoadout("cape-prime", localLoadout.wingsId());
        }
    }

    public static void setAccentTint(int tintArgb) {
        // Tint comes from catalog items; API kept for compatibility.
    }

    public static void reset() {
        localLoadout = CosmeticLoadout.EMPTY;
        peers.clear();
        announceDirty.set(false);
        CapePhysicsState.reset();
    }
}
