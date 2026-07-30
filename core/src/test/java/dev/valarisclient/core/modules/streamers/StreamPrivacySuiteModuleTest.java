package dev.valarisclient.core.modules.streamers;

import dev.valarisclient.core.event.EventBus;
import dev.valarisclient.core.keybind.KeybindManager;
import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.stream.StreamerPrivacyState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamPrivacySuiteModuleTest {

    private ModuleManager manager;
    private StreamPrivacySuiteModule suite;
    private StreamDebugShieldModule debugShield;
    private StreamChatRedactModule chatRedact;

    @BeforeEach
    void setUp() {
        manager = new ModuleManager(new EventBus(), new KeybindManager());
        suite = manager.register(new StreamPrivacySuiteModule(manager));
        debugShield = manager.register(new StreamDebugShieldModule());
        chatRedact = manager.register(new StreamChatRedactModule());
        manager.register(new StreamNameMaskModule());
        manager.register(new StreamHudShieldModule(new dev.valarisclient.core.hud.HudManager()));
        manager.register(new StreamBrandingModule(new dev.valarisclient.core.hud.HudManager()));
    }

    @AfterEach
    void reset() {
        StreamerPrivacyState.reset();
        suite.setEnabled(false);
        debugShield.setEnabled(false);
        chatRedact.setEnabled(false);
    }

    @Test
    void enablingSuiteEnablesSiblings() {
        suite.setEnabled(true);
        assertTrue(debugShield.isEnabled());
        assertTrue(chatRedact.isEnabled());
        assertTrue(StreamerPrivacyState.debugShield());
        assertTrue(StreamerPrivacyState.chatRedact());
    }

    @Test
    void disablingSuiteDisablesSiblings() {
        suite.setEnabled(true);
        suite.setEnabled(false);
        assertFalse(debugShield.isEnabled());
        assertFalse(chatRedact.isEnabled());
        assertFalse(StreamerPrivacyState.debugShield());
        assertFalse(StreamerPrivacyState.chatRedact());
    }
}
