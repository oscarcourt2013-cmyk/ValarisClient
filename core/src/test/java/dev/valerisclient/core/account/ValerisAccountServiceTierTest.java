package dev.valerisclient.core.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValerisAccountServiceTierTest {

    @Test
    void parseLauncherTierIds() {
        assertEquals(ValerisAccountService.Tier.FREE, ValerisAccountService.parseTier("free"));
        assertEquals(ValerisAccountService.Tier.PREMIUM, ValerisAccountService.parseTier("prime"));
        assertEquals(ValerisAccountService.Tier.PREMIUM, ValerisAccountService.parseTier("PREMIUM"));
        assertEquals(ValerisAccountService.Tier.PRIME_PLUS, ValerisAccountService.parseTier("prime_plus"));
    }
}
