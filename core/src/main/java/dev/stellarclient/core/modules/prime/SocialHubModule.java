package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.social.SocialService;
import dev.stellarclient.core.social.SocialSettings;

/** Friends, DM chat, party â€” synced with StellarClient Launcher via the unified backend. */
public final class SocialHubModule extends Module {

    private final SocialService social;
    private final StringSetting apiBase =
            addSetting(new StringSetting("api-base", "API URL", "Prime social + voice backend",
                    SocialSettings.DEFAULT_API));
    private final BooleanSetting autoConnect =
            addSetting(new BooleanSetting("auto-connect", "Auto connect",
                    "Connect to the social backend while in-game", true));

    public SocialHubModule(SocialService social) {
        super("social-hub", "Social Hub",
                "Friends, private chat and party â€” synced with the launcher",
                ModuleCategory.PRIME);
        this.social = social;
        listen(ClientTickEvent.class, event -> applySettings());
    }

    @Override
    protected void onEnable() {
        applySettings();
        if (autoConnect.get()) {
            social.onWorldJoin();
        }
    }

    @Override
    protected void onDisable() {
        // Keep the live session â€” only the ClickGUI toggle goes off.
        // Disconnect happens on world leave.
        applySettings();
    }

    private void applySettings() {
        social.settings().setApiBase(apiBase.get());
        social.settings().setEnabled(autoConnect.get());
    }
}
