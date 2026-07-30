package dev.valerisclient.core.state;

import dev.valerisclient.core.account.ValerisAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClientBadgeStateTest {

    @AfterEach
    void tearDown() {
        ClientBadgeState.reset();
    }

    @Test
    void tierChangesAccent() {
        ClientBadgeState.setTier(ValerisAccountService.Tier.FREE);
        int freeAccent = ClientBadgeState.accent();

        ClientBadgeState.setTier(ValerisAccountService.Tier.PREMIUM);
        int valerisAccent = ClientBadgeState.accent();

        ClientBadgeState.setTier(ValerisAccountService.Tier.PRIME_PLUS);
        int plusAccent = ClientBadgeState.accent();

        assertNotEquals(freeAccent, valerisAccent);
        assertNotEquals(valerisAccent, plusAccent);
        assertEquals(0xFFF59E0B, plusAccent);
    }
}
