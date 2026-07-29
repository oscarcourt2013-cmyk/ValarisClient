package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Sends "gg" when a game-end message is detected. */
public final class AutoGgModule extends Module {

    private final MinecraftAdapter adapter;

    public AutoGgModule(MinecraftAdapter adapter) {
        super("auto-gg", "Auto GG", "Says gg when a game ends", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ChatMessageEvent.class, this::onChat);
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing()) {
            return;
        }
        String lower = event.text().toLowerCase();
        if (lower.contains("won") || lower.contains("victory") || lower.contains("game over")) {
            adapter.sendChat("gg");
        }
    }
}
