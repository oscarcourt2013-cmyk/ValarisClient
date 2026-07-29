package dev.stellarclient.core.servers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerServersTest {

    @Test
    void recognizesElysiaPartner() {
        assertTrue(PartnerServers.isPartnerAddress("elysiasmp.fr"));
        assertTrue(PartnerServers.isPartnerAddress("ElysiaSMP.fr:25565"));
        assertFalse(PartnerServers.isPartnerAddress("hypixel.net"));
    }
}
