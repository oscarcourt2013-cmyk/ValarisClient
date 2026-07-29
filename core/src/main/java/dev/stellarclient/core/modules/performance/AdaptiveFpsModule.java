package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.AttackEntityEvent;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Reduces particles and render distance during combat. */
public final class AdaptiveFpsModule extends Module {

    private static final long COMBAT_MS = 4000L;

    private final MinecraftAdapter adapter;

    private int savedRender = -1;
    private int savedParticles = -1;
    private long lastCombatMillis;
    private boolean adjusted;

    public AdaptiveFpsModule(MinecraftAdapter adapter) {
        super("adaptive-fps", "Adaptive FPS", "Lower settings during combat", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
        listen(AttackEntityEvent.class, event -> lastCombatMillis = System.currentTimeMillis());
        listen(ClientTickEvent.class, event -> tick());
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private void tick() {
        boolean combat = System.currentTimeMillis() - lastCombatMillis < COMBAT_MS;
        if (combat && !adjusted) {
            savedRender = adapter.renderDistance();
            savedParticles = adapter.particleSetting();
            adapter.setRenderDistance(Math.max(6, savedRender - 4));
            adapter.setParticleSetting(Math.min(2, savedParticles + 1));
            adjusted = true;
        } else if (!combat && adjusted) {
            restore();
        }
    }

    private void restore() {
        if (adjusted) {
            if (savedRender >= 0) {
                adapter.setRenderDistance(savedRender);
            }
            if (savedParticles >= 0) {
                adapter.setParticleSetting(savedParticles);
            }
            savedRender = -1;
            savedParticles = -1;
            adjusted = false;
        }
    }
}
