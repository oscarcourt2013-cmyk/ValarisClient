package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.state.ChatOverlayState;

/** Compact chat layout toggle for overlay hooks. */
public final class BetterChatModule extends Module {

    private final BooleanSetting compact =
            addSetting(new BooleanSetting("compact", "Compact", "Use compact chat formatting", false));

    private boolean lastCompact;

    public BetterChatModule() {
        super("better-chat", "Better Chat", "Improves chat readability", ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> syncIfChanged());
    }

    @Override
    protected void onEnable() {
        apply();
    }

    @Override
    protected void onDisable() {
        ChatOverlayState.setCompact(false);
    }

    private void syncIfChanged() {
        boolean current = compact.get();
        if (current != lastCompact) {
            apply();
        }
    }

    private void apply() {
        lastCompact = compact.get();
        ChatOverlayState.setCompact(lastCompact);
    }
}
