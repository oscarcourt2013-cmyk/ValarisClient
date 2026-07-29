package dev.stellarclient.core.modules.streamers;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.stream.StreamerPrivacyState;

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
