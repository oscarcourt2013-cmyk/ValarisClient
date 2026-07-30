package dev.valerisclient.core.modules.performance;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** Applies bundled performance presets in one click. */
public final class PerformanceProfilesModule extends Module {

    public enum Profile {
        LOW,
        MEDIUM,
        HIGH
    }

    private final EnumSetting<Profile> profile =
            addSetting(new EnumSetting<>("profile", "Profile", "Performance preset", Profile.MEDIUM));

    private final MinecraftAdapter adapter;

    private Profile appliedProfile;
    private int savedRenderDistance = -1;
    private int savedSimulationDistance = -1;
    private int savedParticleSetting = -1;
    private boolean savedCloudsEnabled;
    private int savedEntityDistance = -1;
    private boolean savedFancyGraphics;

    public PerformanceProfilesModule(MinecraftAdapter adapter) {
        super("performance-profiles", "Performance Profiles", "Apply LOW, MEDIUM, or HIGH presets", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
        listen(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onEnable() {
        captureOriginals();
        applyProfile(profile.get());
    }

    @Override
    protected void onDisable() {
        restoreOriginals();
        appliedProfile = null;
    }

    private void onTick(ClientTickEvent event) {
        Profile current = profile.get();
        if (current != appliedProfile) {
            applyProfile(current);
        }
    }

    private void captureOriginals() {
        if (savedRenderDistance < 0) {
            savedRenderDistance = adapter.renderDistance();
            savedSimulationDistance = adapter.simulationDistance();
            savedParticleSetting = adapter.particleSetting();
            savedCloudsEnabled = adapter.cloudsEnabled();
            savedEntityDistance = adapter.entityDistance();
            savedFancyGraphics = adapter.fancyGraphics();
        }
    }

    private void applyProfile(Profile target) {
        captureOriginals();
        switch (target) {
            case LOW -> {
                adapter.setRenderDistance(6);
                adapter.setSimulationDistance(5);
                adapter.setParticleSetting(2);
                adapter.setCloudsEnabled(false);
                adapter.setEntityDistance(50);
                adapter.setFancyGraphics(false);
            }
            case MEDIUM -> {
                adapter.setRenderDistance(10);
                adapter.setSimulationDistance(8);
                adapter.setParticleSetting(1);
                adapter.setCloudsEnabled(true);
                adapter.setEntityDistance(75);
                adapter.setFancyGraphics(false);
            }
            case HIGH -> {
                adapter.setRenderDistance(Math.max(savedRenderDistance, 16));
                adapter.setSimulationDistance(Math.max(savedSimulationDistance, 12));
                adapter.setParticleSetting(0);
                adapter.setCloudsEnabled(true);
                adapter.setEntityDistance(100);
                adapter.setFancyGraphics(true);
            }
        }
        appliedProfile = target;
    }

    private void restoreOriginals() {
        if (savedRenderDistance >= 0) {
            adapter.setRenderDistance(savedRenderDistance);
            adapter.setSimulationDistance(savedSimulationDistance);
            adapter.setParticleSetting(savedParticleSetting);
            adapter.setCloudsEnabled(savedCloudsEnabled);
            adapter.setEntityDistance(savedEntityDistance);
            adapter.setFancyGraphics(savedFancyGraphics);
            savedRenderDistance = -1;
        }
    }
}
