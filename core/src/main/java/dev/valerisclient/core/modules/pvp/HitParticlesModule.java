package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.AttackEntityEvent;
import dev.valerisclient.core.module.DoubleSetting;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.HitParticlesState;

/** Custom hit-only particles — world particles are never modified. */
public final class HitParticlesModule extends Module {

    private final EnumSetting<HitParticlesState.Preset> preset =
            addSetting(new EnumSetting<>("preset", "Preset", "Hit particle style",
                    HitParticlesState.Preset.SPARK));
    private final DoubleSetting intensity =
            addSetting(new DoubleSetting("intensity", "Intensity", "Particle amount multiplier", 1.0, 0.25, 2.0));

    private final MinecraftAdapter adapter;

    public HitParticlesModule(MinecraftAdapter adapter) {
        super("hit-particles", "Hit Particles", "Custom particles on hit only", ModuleCategory.PVP);
        this.adapter = adapter;
        listen(AttackEntityEvent.class, this::onAttack);
    }

    @Override
    protected void onEnable() {
        applyState();
    }

    @Override
    protected void onDisable() {
        HitParticlesState.reset();
    }

    private void applyState() {
        HitParticlesState.configure(isEnabled(), preset.get(), (float) intensity.get());
    }

    private void onAttack(AttackEntityEvent event) {
        if (!isEnabled()) {
            return;
        }
        applyState();
        if (!adapter.hasPlayer()) {
            return;
        }
        double x = adapter.playerX();
        double y = adapter.playerY() + 1.0;
        double z = adapter.playerZ();
        adapter.spawnHitParticles(x, y, z,
                HitParticlesState.color(),
                HitParticlesState.size(),
                HitParticlesState.scaledCount());
    }
}
