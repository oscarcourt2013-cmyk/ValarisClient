package dev.stellarclient.core.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSecretsStoreTest {

    @TempDir
    Path temp;

    @Test
    void persistsAndReloadsKey() {
        AiSecretsStore store = new AiSecretsStore(temp);
        assertFalse(store.hasKey());
        store.setGroqApiKey("gsk_testkeyabcdefghijklmnopqrstuvwxyz0123456789");
        assertTrue(store.hasKey());

        AiSecretsStore reloaded = new AiSecretsStore(temp);
        assertEquals("gsk_testkeyabcdefghijklmnopqrstuvwxyz0123456789", reloaded.groqApiKey());
    }
}
