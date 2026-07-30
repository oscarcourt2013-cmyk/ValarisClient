package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.ChatOverlayState;

/** Highlights chat lines containing your player name. */
public final class MentionHighlightModule extends Module {

    private final MinecraftAdapter adapter;

    public MentionHighlightModule(MinecraftAdapter adapter) {
        super("mention-highlight", "Mention Highlight", "Highlights chat when your name appears", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> sync());
    }

    @Override
    protected void onEnable() {
        sync();
    }

    @Override
    protected void onDisable() {
        ChatOverlayState.setHighlightMentions(false, "");
    }

    private void sync() {
        ChatOverlayState.setHighlightMentions(true, adapter.playerName());
    }
}
