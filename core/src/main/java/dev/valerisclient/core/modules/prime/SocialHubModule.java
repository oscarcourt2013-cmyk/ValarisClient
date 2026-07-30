package dev.valerisclient.core.modules.prime;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.social.SocialService;
import dev.valerisclient.core.social.SocialSettings;

/** Friends, DM chat, party — synced with ValerisClient Launcher via the unified backend. */
public final class SocialHubModule extends Module {

    private final SocialService social;
    private final StringSetting apiBase =
            addSetting(new StringSetting("api-base", "API URL", "Valeris social + voice backend",
                    SocialSettings.DEFAULT_API));
    private final BooleanSetting autoConnect =
            addSetting(new BooleanSetting("auto-connect", "Auto connect",
                    "Connect to the social backend while in-game", true));

    public SocialHubModule(SocialService social) {
        super("social-hub", "Social Hub",
                "Friends, private chat and party — synced with the launcher",
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
        // Keep the live session — only the ClickGUI toggle goes off.
        // Disconnect happens on world leave.
        applySettings();
    }

    private void applySettings() {
        social.settings().setApiBase(apiBase.get());
        social.settings().setEnabled(autoConnect.get());
    }
}
