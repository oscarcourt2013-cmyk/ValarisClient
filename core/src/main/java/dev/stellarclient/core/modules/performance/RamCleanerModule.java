package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Periodically suggests garbage collection to free heap memory. */
public final class RamCleanerModule extends Module {

    private static final int GC_INTERVAL_TICKS = 600;

    private final BooleanSetting auto =
            addSetting(new BooleanSetting("auto", "Auto clean", "Run GC automatically on an interval", true));

    private final MinecraftAdapter adapter;
    private int tickCounter;

    public RamCleanerModule(MinecraftAdapter adapter) {
        super("ram-cleaner", "RAM Cleaner", "Suggests garbage collection periodically", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
        listen(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
    }

    private void onTick(ClientTickEvent event) {
        if (!auto.get()) {
            return;
        }
        tickCounter++;
        if (tickCounter >= GC_INTERVAL_TICKS) {
            tickCounter = 0;
            adapter.suggestGarbageCollection();
        }
    }
}
