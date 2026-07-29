package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.skin.CustomSkinService;
import dev.stellarclient.core.state.CustomSkinState;

/** Custom body skin visible locally and to other StellarClient users. */
public final class CustomSkinModule extends Module {

    private final CustomSkinService skins;
    private int pollTicks;

    public CustomSkinModule(CustomSkinService skins) {
        super("custom-skin", "Custom Skin",
                "Set a custom skin visible to Prime peers (works offline/cracked)",
                ModuleCategory.PRIME);
        this.skins = skins;
        listen(ClientTickEvent.class, event -> {
            if (!isEnabled()) {
                return;
            }
            pollTicks++;
            if (pollTicks >= 40) {
                pollTicks = 0;
                skins.pollBridgeFile();
            }
        });
    }

    @Override
    protected void onEnable() {
        skins.setEnabled(true);
        skins.loadFromDisk();
        CustomSkinState.markAnnounceDirty();
    }

    @Override
    protected void onDisable() {
        skins.setEnabled(false);
    }
}
