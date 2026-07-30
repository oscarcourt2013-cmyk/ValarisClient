package dev.valarisclient.core.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValarisAccountServiceTierTest {

    @Test
    void parseLauncherTierIds() {
        assertEquals(ValarisAccountService.Tier.FREE, ValarisAccountService.parseTier("free"));
        assertEquals(ValarisAccountService.Tier.PREMIUM, ValarisAccountService.parseTier("valaris"));
        assertEquals(ValarisAccountService.Tier.PREMIUM, ValarisAccountService.parseTier("PREMIUM"));
        assertEquals(ValarisAccountService.Tier.VALARIS_PLUS, ValarisAccountService.parseTier("valaris_plus"));
    }
}
