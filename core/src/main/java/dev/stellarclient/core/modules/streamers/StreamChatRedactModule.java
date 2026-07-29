package dev.stellarclient.core.modules.streamers;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.stream.StreamerPrivacyState;

/** Redacts coordinates, IPs, and whispers in displayed chat. */
public final class StreamChatRedactModule extends Module {

    private final BooleanSetting redactCoords =
            addSetting(new BooleanSetting(
                    "redact-coords", "Redact coords", "Hide coordinates in chat", true));
    private final BooleanSetting redactIps =
            addSetting(new BooleanSetting(
                    "redact-ips", "Redact IPs", "Hide IP addresses and ports in chat", true));
    private final BooleanSetting redactWhispers =
            addSetting(new BooleanSetting(
                    "redact-whispers", "Redact whispers", "Hide private message content", true));

    public StreamChatRedactModule() {
        super("stream-chat-redact", "Stream Chat Redact",
                "Redacts sensitive data in chat for streams", ModuleCategory.STREAMERS);
        listen(ClientTickEvent.class, event -> syncOptions());
    }

    @Override
    protected void onEnable() {
        StreamerPrivacyState.setChatRedact(true);
        syncOptions();
    }

    @Override
    protected void onDisable() {
        StreamerPrivacyState.setChatRedact(false);
    }

    private void syncOptions() {
        if (!isEnabled()) {
            return;
        }
        StreamerPrivacyState.setRedactCoords(redactCoords.get());
        StreamerPrivacyState.setRedactIps(redactIps.get());
        StreamerPrivacyState.setRedactWhispers(redactWhispers.get());
    }
}
