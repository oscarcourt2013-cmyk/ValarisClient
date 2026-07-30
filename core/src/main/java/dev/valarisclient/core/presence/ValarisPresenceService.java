package dev.valarisclient.core.presence;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.state.ClientBadgeState;
import dev.valarisclient.core.state.CosmeticsState;
import dev.valarisclient.core.state.CustomSkinState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks ValarisClient users discovered via Fabric presence payloads (LAN / integrated server). */
public final class ValarisPresenceService {

    private static final int INITIAL_ANNOUNCE_TICKS = 20;
    private static final int REANNOUNCE_INTERVAL_TICKS = 100;

    private final MinecraftAdapter adapter;
    private final Set<UUID> valarisUsers = ConcurrentHashMap.newKeySet();
    private int announceTicks = -1;
    private int reannounceCooldown;
    private boolean announceSent;
    private Runnable networkAnnouncer = () -> {};

    public ValarisPresenceService(MinecraftAdapter adapter) {
        this.adapter = adapter;
    }

    public void setNetworkAnnouncer(Runnable announcer) {
        this.networkAnnouncer = announcer != null ? announcer : () -> {};
    }

    public void onWorldJoin() {
        valarisUsers.clear();
        CosmeticsState.clearPeers();
        CustomSkinState.clearPeers();
        announceTicks = 0;
        announceSent = false;
        reannounceCooldown = 0;
        if (ClientBadgeState.active()) {
            markLocalPlayer();
        }
    }

    public void onWorldLeave() {
        valarisUsers.clear();
        CosmeticsState.clearPeers();
        CustomSkinState.clearPeers();
        announceTicks = -1;
        announceSent = false;
        reannounceCooldown = 0;
    }

    public void tick() {
        if (!ClientBadgeState.active() || announceTicks < 0) {
            CosmeticsState.consumeAnnounceDirty();
            CustomSkinState.consumeAnnounceDirty();
            return;
        }
        // Keep self marked even if world-join raced ahead of module enable / UUID settle.
        markLocalPlayer();
        if (!announceSent) {
            announceTicks++;
            if (announceTicks >= INITIAL_ANNOUNCE_TICKS) {
                announcePresence();
            }
            return;
        }
        boolean dirty = CosmeticsState.consumeAnnounceDirty() | CustomSkinState.consumeAnnounceDirty();
        if (dirty) {
            announcePresence();
            reannounceCooldown = REANNOUNCE_INTERVAL_TICKS;
            return;
        }
        if (reannounceCooldown > 0) {
            reannounceCooldown--;
            return;
        }
        announcePresence();
        reannounceCooldown = REANNOUNCE_INTERVAL_TICKS;
    }

    public void onModuleEnabled() {
        markLocalPlayer();
        announceSent = false;
        announceTicks = 0;
        if (adapter.hasPlayer()) {
            announcePresence();
        }
    }

    public void onModuleDisabled() {
        UUID local = localUuid();
        if (local != null) {
            valarisUsers.remove(local);
        }
    }

    public boolean isPrime(UUID uuid) {
        return uuid != null && valarisUsers.contains(uuid);
    }

    public void markPrime(UUID uuid) {
        markPrime(uuid, "", "", "");
    }

    public void markPrime(UUID uuid, String capeId, String wingsId) {
        markPrime(uuid, capeId, wingsId, "");
    }

    public void markPrime(UUID uuid, String capeId, String wingsId, String skinHash) {
        if (uuid == null) {
            return;
        }
        valarisUsers.add(uuid);
        CosmeticsState.setPeerLoadout(uuid, capeId, wingsId);
        if (skinHash == null || skinHash.isBlank()) {
            CustomSkinState.removePeer(uuid);
            return;
        }
        // Texture bytes arrive via SkinTexturePayload; keep old bytes until matching packet.
    }

    /** Force a presence broadcast (e.g. after equipping cosmetics or changing skin). */
    public void requestAnnounce() {
        CosmeticsState.markAnnounceDirty();
        CustomSkinState.markAnnounceDirty();
        if (ClientBadgeState.active() && adapter.hasPlayer()) {
            announcePresence();
            reannounceCooldown = REANNOUNCE_INTERVAL_TICKS;
        }
    }

    private void announcePresence() {
        if (localUuid() == null) {
            return;
        }
        networkAnnouncer.run();
        announceSent = true;
    }

    private void markLocalPlayer() {
        UUID local = localUuid();
        if (local != null) {
            valarisUsers.add(local);
            CosmeticsState.setPeerLoadout(
                    local, CosmeticsState.localCapeId(), CosmeticsState.localWingsId());
            if (CustomSkinState.hasLocal()) {
                byte[] bytes = CustomSkinState.localBytes();
                if (bytes != null) {
                    CustomSkinState.setPeer(local, bytes);
                }
            }
        }
    }

    private UUID localUuid() {
        String raw = adapter.playerUuid();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
