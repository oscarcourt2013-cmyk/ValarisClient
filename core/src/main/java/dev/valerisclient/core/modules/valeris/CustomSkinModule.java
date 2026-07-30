package dev.valerisclient.core.modules.valeris;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.skin.CustomSkinService;
import dev.valerisclient.core.state.CustomSkinState;

/** Custom body skin visible locally and to other ValerisClient users. */
public final class CustomSkinModule extends Module {

    private final CustomSkinService skins;
    private int pollTicks;

    public CustomSkinModule(CustomSkinService skins) {
        super("custom-skin", "Custom Skin",
                "Set a custom skin visible to Valeris peers (works offline/cracked)",
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
