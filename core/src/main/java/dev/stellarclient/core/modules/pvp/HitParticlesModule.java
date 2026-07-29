package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.AttackEntityEvent;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.EnumSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.HitParticlesState;

/** Custom hit-only particles â€” world particles are never modified. */
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
