package dev.valarisclient.core.modules.streamers;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.stream.StreamerPrivacyState;

/** Masks other players' nametags with stable session aliases. */
public final class StreamNameMaskModule extends Module {

    private final BooleanSetting maskSelf =
            addSetting(new BooleanSetting(
                    "mask-self", "Mask self", "Also mask your own nametag", false));

    public StreamNameMaskModule() {
        super("stream-name-mask", "Stream Name Mask",
                "Masks player nametags to prevent stream-sniping", ModuleCategory.STREAMERS);
        listen(ClientTickEvent.class, event -> syncMaskSelf());
    }

    @Override
    protected void onEnable() {
        StreamerPrivacyState.setNameMask(true);
        syncMaskSelf();
    }

    @Override
    protected void onDisable() {
        StreamerPrivacyState.setNameMask(false);
        StreamerPrivacyState.setMaskSelf(false);
    }

    private void syncMaskSelf() {
        if (isEnabled()) {
            StreamerPrivacyState.setMaskSelf(maskSelf.get());
        }
    }
}
