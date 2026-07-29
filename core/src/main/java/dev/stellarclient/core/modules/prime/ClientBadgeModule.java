package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.account.PrimeAccountService;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.presence.PrimePresenceService;
import dev.stellarclient.core.state.ClientBadgeState;

/** Shows a Prime badge beside usernames in the tab list for other StellarClient users. */
public final class ClientBadgeModule extends Module {

    private final PrimePresenceService presence;
    private final PrimeAccountService account;

    public ClientBadgeModule(PrimePresenceService presence, PrimeAccountService account) {
        super("client-badge", "Client Badge",
                "Shows a Prime marker next to players using StellarClient in the tab list",
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
