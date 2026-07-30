package dev.valerisclient.core.state;

import dev.valerisclient.core.account.PrimeAccountService;
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
        ClientBadgeState.setTier(PrimeAccountService.Tier.FREE);
        int freeAccent = ClientBadgeState.accent();

        ClientBadgeState.setTier(PrimeAccountService.Tier.PREMIUM);
        int primeAccent = ClientBadgeState.accent();

        ClientBadgeState.setTier(PrimeAccountService.Tier.PRIME_PLUS);
        int plusAccent = ClientBadgeState.accent();

        assertNotEquals(freeAccent, primeAccent);
        assertNotEquals(primeAccent, plusAccent);
        assertEquals(0xFFF59E0B, plusAccent);
    }
}
