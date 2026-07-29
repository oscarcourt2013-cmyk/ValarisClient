package dev.stellarclient.core.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimeAccountServiceTierTest {

    @Test
    void parseLauncherTierIds() {
        assertEquals(PrimeAccountService.Tier.FREE, PrimeAccountService.parseTier("free"));
        assertEquals(PrimeAccountService.Tier.PREMIUM, PrimeAccountService.parseTier("prime"));
        assertEquals(PrimeAccountService.Tier.PREMIUM, PrimeAccountService.parseTier("PREMIUM"));
        assertEquals(PrimeAccountService.Tier.PRIME_PLUS, PrimeAccountService.parseTier("prime_plus"));
    }
}
