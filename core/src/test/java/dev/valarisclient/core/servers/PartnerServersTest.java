package dev.valarisclient.core.servers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerServersTest {

    @Test
    void injectsNoPartners() {
        assertTrue(PartnerServers.partners().isEmpty());
        assertFalse(PartnerServers.isPartnerAddress("elysiasmp.fr"));
        assertFalse(PartnerServers.isPartnerAddress("hypixel.net"));
    }

    @Test
    void recognizesLegacyInjectedAddressesForCleanup() {
        assertTrue(PartnerServers.isLegacyInjectedAddress("elysiasmp.fr"));
        assertTrue(PartnerServers.isLegacyInjectedAddress("ElysiaSMP.fr:25565"));
        assertFalse(PartnerServers.isLegacyInjectedAddress("hypixel.net"));
    }
}
