package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.ColorSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.HitColorState;

/** Custom entity hit-overlay color. */
public final class HitColorModule extends Module {

    private final ColorSetting hitColor =
            addSetting(new ColorSetting("hit-color", "Hit Color", "Color applied when entities are hit", 0x80FF0000));

    public HitColorModule() {
        super("hit-color", "Hit Color", "Changes the color shown when you hit an entity", ModuleCategory.PVP);
        listen(ClientTickEvent.class, event -> HitColorState.setArgb(hitColor.get()));
    }

    @Override
    protected void onEnable() {
        HitColorState.setActive(true);
        HitColorState.setArgb(hitColor.get());
    }

    @Override
    protected void onDisable() {
        HitColorState.reset();
    }
}
