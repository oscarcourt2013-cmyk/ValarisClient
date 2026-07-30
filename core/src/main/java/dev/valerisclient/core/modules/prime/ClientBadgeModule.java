package dev.valerisclient.core.modules.prime;

import dev.valerisclient.core.account.PrimeAccountService;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.presence.PrimePresenceService;
import dev.valerisclient.core.state.ClientBadgeState;

/** Shows a Prime badge beside usernames in the tab list for other ValerisClient users. */
public final class ClientBadgeModule extends Module {

    private final PrimePresenceService presence;
    private final PrimeAccountService account;

    public ClientBadgeModule(PrimePresenceService presence, PrimeAccountService account) {
        super("client-badge", "Client Badge",
                "Shows a Valeris marker next to players using ValerisClient in the tab list",
                ModuleCategory.PRIME);
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
