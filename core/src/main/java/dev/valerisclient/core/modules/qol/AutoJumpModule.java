package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** Forces vanilla auto-jump while enabled (same as the Accessibility option). */
public final class AutoJumpModule extends Module {

    private final MinecraftAdapter adapter;
    private boolean savedAutoJump;
    private boolean hasSaved;

    public AutoJumpModule(MinecraftAdapter adapter) {
        super("auto-jump", "Auto Jump", "Automatically jump onto blocks like the vanilla option",
                ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
        if (!hasSaved) {
            savedAutoJump = adapter.autoJump();
            hasSaved = true;
        }
        adapter.setAutoJump(true);
    }

    @Override
    protected void onDisable() {
        if (hasSaved) {
            adapter.setAutoJump(savedAutoJump);
            hasSaved = false;
        }
    }

    private void onTick() {
        if (!adapter.autoJump()) {
            adapter.setAutoJump(true);
        }
    }
}
