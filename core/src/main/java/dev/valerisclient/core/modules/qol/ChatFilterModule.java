package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.state.ChatFilterState;

/** Filters incoming chat messages containing configured words. */
public final class ChatFilterModule extends Module {

    private final StringSetting words =
            addSetting(new StringSetting("words", "Filter words", "Comma-separated words to hide", ""));

    private String lastSynced = "";

    public ChatFilterModule() {
        super("chat-filter", "Chat Filter", "Hides chat messages with blocked words", ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> syncIfChanged());
    }

    @Override
    protected void onEnable() {
        sync();
    }

    @Override
    protected void onDisable() {
        ChatFilterState.setEnabled(false);
        lastSynced = "";
    }

    private void syncIfChanged() {
        String current = words.get();
        if (!current.equals(lastSynced)) {
            sync();
        }
    }

    private void sync() {
        lastSynced = words.get();
        ChatFilterState.setWords(lastSynced);
        ChatFilterState.setEnabled(true);
    }
}
