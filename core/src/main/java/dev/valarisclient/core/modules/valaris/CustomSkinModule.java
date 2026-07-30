package dev.valarisclient.core.modules.valaris;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.skin.CustomSkinService;
import dev.valarisclient.core.state.CustomSkinState;

/** Custom body skin visible locally and to other ValarisClient users. */
public final class CustomSkinModule extends Module {

    private final CustomSkinService skins;
    private int pollTicks;

    public CustomSkinModule(CustomSkinService skins) {
        super("custom-skin", "Custom Skin",
                "Set a custom skin visible to Valaris peers (works offline/cracked)",
                ModuleCategory.VALARIS);
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
