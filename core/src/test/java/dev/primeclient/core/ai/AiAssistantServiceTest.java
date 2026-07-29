package dev.stellarclient.core.ai;

import dev.stellarclient.core.event.EventBus;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.module.ModuleManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAssistantServiceTest {

    @Test
    void interceptsAiCommandsAndIgnoresOtherChat() {
        RecordingAdapter adapter = new RecordingAdapter();
        AiAssistantService ai = new AiAssistantService(adapter, emptyModules(), adapter.configDirectory());

        assertFalse(ai.handleClientCommand("hello"));
        assertFalse(ai.handleClientCommand("/aide"));
        assertTrue(ai.handleClientCommand("/ai"));
        assertTrue(ai.handleClientCommand("/ai help"));
        assertTrue(ai.handleClientCommand("/ai status"));
        assertTrue(adapter.messages.stream().anyMatch(m -> m.contains("Assistant Prime")));
    }

    @Test
    void setkeyStoresLocally() {
        RecordingAdapter adapter = new RecordingAdapter();
        AiAssistantService ai = new AiAssistantService(adapter, emptyModules(), adapter.configDirectory());
        String key = "gsk_abcdefghijklmnopqrstuvwxyz0123456789ABCD";
        assertTrue(ai.handleClientCommand("/ai setkey " + key));
        assertTrue(ai.secrets().hasKey());
        assertTrue(key.equals(ai.secrets().groqApiKey()));
    }

    private static ModuleManager emptyModules() {
        return new ModuleManager(new EventBus(), new KeybindManager());
    }

    private static final class RecordingAdapter implements dev.stellarclient.core.adapter.MinecraftAdapter {
        private final PathHolder paths = new PathHolder();
        final List<String> messages = new ArrayList<>();
        final AtomicReference<Runnable> lastTask = new AtomicReference<>();

        @Override
        public String minecraftVersion() {
            return "test";
        }

        @Override
        public java.nio.file.Path gameDirectory() {
            return paths.root;
        }

        @Override
        public java.nio.file.Path configDirectory() {
            return paths.root;
        }

        @Override
        public boolean isInGame() {
            return false;
        }

        @Override
        public boolean isScreenOpen() {
            return false;
        }

        @Override
        public boolean isKeyDown(int glfwKey) {
            return false;
        }

        @Override
        public boolean isMouseButtonDown(int glfwButton) {
            return false;
        }

        @Override
        public int fps() {
            return 0;
        }

        @Override
        public boolean hasPlayer() {
            return false;
        }

        @Override
        public double playerX() {
            return 0;
        }

        @Override
        public double playerY() {
            return 0;
        }

        @Override
        public double playerZ() {
            return 0;
        }

        @Override
        public void runOnClientThread(Runnable task) {
            lastTask.set(task);
        }

        @Override
        public void openHudEditor() {
        }

        @Override
        public void openClickGui() {
        }

        @Override
        public void displayClientMessage(String message) {
            messages.add(message);
        }
    }

    private static final class PathHolder {
        final java.nio.file.Path root;

        PathHolder() {
            try {
                root = java.nio.file.Files.createTempDirectory("prime-ai-test");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
