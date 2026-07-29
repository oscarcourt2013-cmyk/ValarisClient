package dev.stellarclient.core.modules.streamers;

import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.stream.StreamerPrivacyState;

/** Replaces the F3 debug overlay with a stream-safe minimal view. */
public final class StreamDebugShieldModule extends Module {

    public StreamDebugShieldModule() {
        super("stream-debug-shield", "Stream Debug Shield",
                "Sanitizes the F3 debug overlay for streams", ModuleCategory.STREAMERS);
    }

    @Override
    protected void onEnable() {
        StreamerPrivacyState.setDebugShield(true);
    }

    @Override
    protected void onDisable() {
        StreamerPrivacyState.setDebugShield(false);
    }
}
