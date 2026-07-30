package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.ColorSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.state.HitColorState;

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
