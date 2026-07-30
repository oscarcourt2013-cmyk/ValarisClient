package dev.valarisclient.core.state;

import dev.valarisclient.core.account.ValarisAccountService;
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
        ClientBadgeState.setTier(ValarisAccountService.Tier.FREE);
        int freeAccent = ClientBadgeState.accent();

        ClientBadgeState.setTier(ValarisAccountService.Tier.PREMIUM);
        int valarisAccent = ClientBadgeState.accent();

        ClientBadgeState.setTier(ValarisAccountService.Tier.VALARIS_PLUS);
        int plusAccent = ClientBadgeState.accent();

        assertNotEquals(freeAccent, valarisAccent);
        assertNotEquals(valarisAccent, plusAccent);
        assertEquals(0xFFF59E0B, plusAccent);
    }
}
