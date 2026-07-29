package dev.stellarclient.core.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Client-side custom skin bytes for local player and Prime peers. */
public final class CustomSkinState {

    private static final AtomicReference<byte[]> localBytes = new AtomicReference<>();
    private static final AtomicReference<String> localHash = new AtomicReference<>("");
    private static final Map<UUID, byte[]> peers = new ConcurrentHashMap<>();
    private static final Map<UUID, String> peerHashes = new ConcurrentHashMap<>();
    private static final AtomicBoolean announceDirty = new AtomicBoolean(false);

    private CustomSkinState() {
    }

    public static void setLocal(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            clearLocal();
            return;
        }
        byte[] copy = bytes.clone();
        localBytes.set(copy);
        localHash.set(hashOf(copy));
        announceDirty.set(true);
    }

    public static void clearLocal() {
        localBytes.set(null);
        localHash.set("");
        announceDirty.set(true);
    }

    public static byte[] localBytes() {
        byte[] bytes = localBytes.get();
        return bytes == null ? null : bytes.clone();
    }

    public static String localHash() {
        String hash = localHash.get();
        return hash == null ? "" : hash;
    }

    public static boolean hasLocal() {
        byte[] bytes = localBytes.get();
        return bytes != null && bytes.length > 0;
    }

    public static void setPeer(UUID uuid, byte[] bytes) {
        if (uuid == null || bytes == null || bytes.length == 0) {
            return;
        }
        byte[] copy = bytes.clone();
        peers.put(uuid, copy);
        peerHashes.put(uuid, hashOf(copy));
    }

    public static void removePeer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        peers.remove(uuid);
        peerHashes.remove(uuid);
    }

    public static byte[] peerBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        byte[] bytes = peers.get(uuid);
        return bytes == null ? null : bytes.clone();
    }

    public static String peerHash(UUID uuid) {
        if (uuid == null) {
            return "";
        }
        return peerHashes.getOrDefault(uuid, "");
    }

    public static boolean needsPeerTexture(UUID uuid, String expectedHash) {
        if (uuid == null || expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        return !expectedHash.equals(peerHashes.get(uuid));
    }

    /** PNG for rendering: local player uses local bytes, peers use peer map. */
    public static byte[] bytesFor(UUID uuid, boolean localPlayer) {
        if (localPlayer) {
            return localBytes();
        }
        return peerBytes(uuid);
    }

    public static void clearPeers() {
        peers.clear();
        peerHashes.clear();
    }

    public static boolean consumeAnnounceDirty() {
        return announceDirty.getAndSet(false);
    }

    public static void markAnnounceDirty() {
        announceDirty.set(true);
    }

    public static void reset() {
        localBytes.set(null);
        localHash.set("");
        peers.clear();
        peerHashes.clear();
        announceDirty.set(false);
    }

    public static String hashOf(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
