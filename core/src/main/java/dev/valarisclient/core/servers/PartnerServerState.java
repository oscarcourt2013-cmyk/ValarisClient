package dev.valarisclient.core.servers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.LinkedHashSet;
import java.util.Set;

import dev.valarisclient.core.config.ConfigBinding;

/**
 * Remembers which partner servers have already been offered to the player.
 *
 * <p>A partner is added to the vanilla server list once. After that the player
 * owns the entry: deleting or reordering it sticks, because it is never
 * re-injected. Without this record every load would put a deleted partner
 * straight back.</p>
 */
public final class PartnerServerState implements ConfigBinding {

    private final Set<String> offered = new LinkedHashSet<>();

    @Override
    public String configKey() {
        return "partnerServers";
    }

    /** True the first time an address is seen; records it so later calls are false. */
    public boolean markOffered(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        return offered.add(address.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean wasOffered(String address) {
        return address != null && offered.contains(address.trim().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public JsonElement saveConfig() {
        JsonArray array = new JsonArray();
        for (String address : offered) {
            array.add(address);
        }
        return array;
    }

    @Override
    public void loadConfig(JsonElement element) {
        offered.clear();
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry != null && entry.isJsonPrimitive()) {
                offered.add(entry.getAsString().trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }
}
