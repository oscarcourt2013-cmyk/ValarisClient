package dev.valerisclient.core.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valerisclient.core.config.ConfigBinding;
import dev.valerisclient.core.state.CosmeticsState;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Equipment manager for client-side cosmetics (capes + wings only). */
public final class CosmeticManager implements ConfigBinding {

    private final Map<String, CosmeticItem> catalog = new LinkedHashMap<>();
    private final EnumMap<CosmeticType, String> equipped = new EnumMap<>(CosmeticType.class);

    public CosmeticManager() {
        register(new CosmeticItem("cape-prime", "Valeris Cape", CosmeticType.CAPE, CosmeticItem.Rarity.LEGENDARY, 0xFF3B82F6));
        register(new CosmeticItem("cape-star", "Star Cape", CosmeticType.CAPE, CosmeticItem.Rarity.EPIC, 0xFFFFD700));
        register(new CosmeticItem("cape-crimson", "Crimson Cape", CosmeticType.CAPE, CosmeticItem.Rarity.EPIC, 0xFFE11D48));
        register(new CosmeticItem("cape-midnight", "Midnight Cape", CosmeticType.CAPE, CosmeticItem.Rarity.RARE, 0xFF6366F1));
        register(new CosmeticItem("wings-ember", "Ember Wings", CosmeticType.WINGS, CosmeticItem.Rarity.LEGENDARY, 0xFFFF6B35));
        register(new CosmeticItem("wings-aurora", "Aurora Wings", CosmeticType.WINGS, CosmeticItem.Rarity.EPIC, 0xFF22D3EE));
        equip(CosmeticType.CAPE, "cape-prime");
        equip(CosmeticType.WINGS, "wings-aurora");
    }

    public void register(CosmeticItem item) {
        catalog.put(item.id(), item);
    }

    public Map<String, CosmeticItem> catalog() {
        return catalog;
    }

    public void equip(CosmeticType type, String itemId) {
        if (type != CosmeticType.CAPE && type != CosmeticType.WINGS) {
            return;
        }
        if (itemId == null || itemId.isBlank()) {
            equipped.remove(type);
        } else if ("wings-light".equals(itemId)) {
            // Legacy bridge / profile alias
            equipped.put(type, "wings-aurora");
        } else if (catalog.containsKey(itemId)) {
            equipped.put(type, itemId);
        }
        syncState();
    }

    public CosmeticItem equipped(CosmeticType type) {
        String id = equipped.get(type);
        return id == null ? null : catalog.get(id);
    }

    public void unequip(CosmeticType type) {
        equipped.remove(type);
        syncState();
    }

    private void syncState() {
        CosmeticItem cape = equipped(CosmeticType.CAPE);
        CosmeticItem wings = equipped(CosmeticType.WINGS);
        CosmeticsState.setLocalLoadout(
                cape != null ? cape.id() : "",
                wings != null ? wings.id() : "");
        if (cape != null) {
            CosmeticsState.setAccentTint(cape.tintArgb());
        } else {
            CosmeticsState.setAccentTint(0);
        }
    }

    @Override
    public String configKey() {
        return "cosmetics";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Map.Entry<CosmeticType, String> entry : equipped.entrySet()) {
            if (entry.getKey() == CosmeticType.CAPE || entry.getKey() == CosmeticType.WINGS) {
                json.addProperty(entry.getKey().name(), entry.getValue());
            }
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        equipped.clear();
        for (CosmeticType type : new CosmeticType[]{CosmeticType.CAPE, CosmeticType.WINGS}) {
            if (json.has(type.name())) {
                equip(type, json.get(type.name()).getAsString());
            }
        }
        syncState();
    }
}
