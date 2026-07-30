package dev.valarisclient.core.modules.valaris;

import dev.valarisclient.core.account.ValarisAccountService;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.presence.ValarisPresenceService;
import dev.valarisclient.core.state.ClientBadgeState;

/** Shows a Valaris badge beside usernames in the tab list for other ValarisClient users. */
public final class ClientBadgeModule extends Module {

    private final ValarisPresenceService presence;
    private final ValarisAccountService account;

    public ClientBadgeModule(ValarisPresenceService presence, ValarisAccountService account) {
        super("client-badge", "Client Badge",
                "Shows a Valaris marker next to players using ValarisClient in the tab list",
                ModuleCategory.VALARIS);
        this.presence = presence;
        this.account = account;
        listen(ClientTickEvent.class, event -> {
            presence.tick();
            ClientBadgeState.setTier(account.tier());
        });
    }

    @Override
    protected void onEnable() {
        ClientBadgeState.setActive(true);
        ClientBadgeState.setTier(account.tier());
        presence.onModuleEnabled();
    }

    @Override
    protected void onDisable() {
        ClientBadgeState.setActive(false);
        presence.onModuleDisabled();
    }
}
