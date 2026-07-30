package dev.valerisclient.core.modules.prime;

import dev.valerisclient.core.cosmetics.CosmeticManager;
import dev.valerisclient.core.cosmetics.CosmeticType;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.DoubleSetting;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.CapePhysicsState;
import dev.valerisclient.core.state.CosmeticsState;

/** Equip Prime cosmetics (capes and wings — world-rendered). */
public final class PrimeCosmeticsModule extends Module {

    private final EnumSetting<CosmeticType> slot =
            addSetting(new EnumSetting<>("slot", "Slot", "Cosmetic slot to edit", CosmeticType.CAPE));
    private final BooleanSetting clothPhysics = addSetting(new BooleanSetting(
            "cloth-physics", "Cloth Physics",
            "Lightweight cape swing from movement / jump (Valeris capes only)", true));
    private final DoubleSetting physicsIntensity = addSetting(new DoubleSetting(
            "physics-intensity", "Physics Intensity",
            "How strongly the cape reacts to motion", 1.0, 0.0, 1.5));

    private final CosmeticManager cosmetics;

    public PrimeCosmeticsModule(CosmeticManager cosmetics) {
        super("prime-cosmetics", "Valeris Cosmetics",
                "Capes and wings visible to you and Valeris peers", ModuleCategory.PRIME);
        this.cosmetics = cosmetics;
        listen(ClientTickEvent.class, event -> {
            if (isEnabled()) {
                pushState();
            }
        });
    }

    @Override
    protected void onEnable() {
        pushState();
    }

    @Override
    protected void onDisable() {
        CosmeticsState.reset();
    }

    /** Cycles equipped item in the selected slot. */
    public void cycleEquipped() {
        CosmeticType type = slot.get();
        if (type != CosmeticType.CAPE && type != CosmeticType.WINGS) {
            type = CosmeticType.CAPE;
        }
        final CosmeticType cycleType = type;
        var items = cosmetics.catalog().values().stream()
                .filter(i -> i.type() == cycleType).toList();
        if (items.isEmpty()) {
            return;
        }
        var current = cosmetics.equipped(type);
        int idx = 0;
        if (current != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id().equals(current.id())) {
                    idx = (i + 1) % items.size();
                    break;
                }
            }
        }
        cosmetics.equip(type, items.get(idx).id());
    }

    private void pushState() {
        var cape = cosmetics.equipped(CosmeticType.CAPE);
        var wings = cosmetics.equipped(CosmeticType.WINGS);
        CosmeticsState.setLocalLoadout(
                cape != null ? cape.id() : "",
                wings != null ? wings.id() : "");
        CapePhysicsState.setActive(clothPhysics.get());
        CapePhysicsState.setIntensity((float) physicsIntensity.get());
    }
}
