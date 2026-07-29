package dev.stellarclient.core.cosmetics;

import dev.stellarclient.core.state.CosmeticsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticManagerTest {

    @AfterEach
    void tearDown() {
        CosmeticsState.reset();
    }

    @Test
    void catalogIsCapeAndWingsOnly() {
        CosmeticManager manager = new CosmeticManager();
        assertTrue(manager.catalog().values().stream().allMatch(
                i -> i.type() == CosmeticType.CAPE || i.type() == CosmeticType.WINGS));
        assertTrue(manager.catalog().containsKey("cape-prime"));
        assertTrue(manager.catalog().containsKey("wings-ember"));
        assertFalse(manager.catalog().containsKey("hat-crown"));
    }

    @Test
    void equipUpdatesLocalState() {
        CosmeticManager manager = new CosmeticManager();
        manager.equip(CosmeticType.CAPE, "cape-crimson");
        manager.equip(CosmeticType.WINGS, "wings-ember");
        assertEquals("cape-crimson", CosmeticsState.localCapeId());
        assertEquals("wings-ember", CosmeticsState.localWingsId());
    }

    @Test
    void legacyWingsLightAliasesAurora() {
        CosmeticManager manager = new CosmeticManager();
        manager.equip(CosmeticType.WINGS, "wings-light");
        assertEquals("wings-aurora", manager.equipped(CosmeticType.WINGS).id());
    }

    @Test
    void peerLoadoutIsSeparateFromLocal() {
        UUID peer = UUID.randomUUID();
        CosmeticsState.setLocalLoadout("cape-prime", "wings-aurora");
        CosmeticsState.setPeerLoadout(peer, "cape-star", "wings-ember");
        assertEquals("cape-prime", CosmeticsState.loadoutFor(peer, true).capeId());
        assertEquals("cape-star", CosmeticsState.loadoutFor(peer, false).capeId());
        assertEquals("wings-ember", CosmeticsState.loadoutFor(peer, false).wingsId());
    }
}
