package dev.stellarclient.core.presence;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimePresenceServiceTest {

  @Test
  void tracksMarkedPrimeUsersWithLoadout() {
      PrimePresenceService service = new PrimePresenceService(new StubAdapter());
      UUID uuid = UUID.randomUUID();
      assertFalse(service.isPrime(uuid));
      service.markPrime(uuid, "cape-prime", "wings-aurora");
      assertTrue(service.isPrime(uuid));
  }

  @Test
  void tracksMarkedPrimeUsers() {
      PrimePresenceService service = new PrimePresenceService(new StubAdapter());
      UUID uuid = UUID.randomUUID();
      assertFalse(service.isPrime(uuid));
      service.markPrime(uuid);
      assertTrue(service.isPrime(uuid));
  }

    private static final class StubAdapter implements MinecraftAdapter {
        @Override public String minecraftVersion() { return "test"; }
        @Override public Path gameDirectory() { return Path.of("."); }
        @Override public Path configDirectory() { return Path.of("."); }
        @Override public boolean isInGame() { return false; }
        @Override public boolean isScreenOpen() { return false; }
        @Override public boolean isKeyDown(int glfwKey) { return false; }
        @Override public boolean isMouseButtonDown(int glfwButton) { return false; }
        @Override public int fps() { return 60; }
        @Override public boolean hasPlayer() { return false; }
        @Override public double playerX() { return 0; }
        @Override public double playerY() { return 0; }
        @Override public double playerZ() { return 0; }
        @Override public void runOnClientThread(Runnable task) { task.run(); }
        @Override public void openHudEditor() {}
        @Override public void openClickGui() {}
    }
}
