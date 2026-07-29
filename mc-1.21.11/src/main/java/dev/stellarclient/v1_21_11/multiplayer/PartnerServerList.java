package dev.stellarclient.v1_21_11.multiplayer;

import dev.stellarclient.core.servers.PartnerServers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

/** Injects partner servers into the vanilla multiplayer list and pins them to the top. */
public final class PartnerServerList {

    /** Guards against re-entry when ensurePartners itself calls add/swap (mixin RETURN hooks). */
    private static final ThreadLocal<Boolean> ENSURING = ThreadLocal.withInitial(() -> false);

    private PartnerServerList() {
    }

    public static void ensurePartners(ServerList list) {
        if (list == null || Boolean.TRUE.equals(ENSURING.get())) {
            return;
        }
        ENSURING.set(true);
        try {
            int pinIndex = 0;
            for (PartnerServers.Entry partner : PartnerServers.partners()) {
                int existing = indexOfAddress(list, partner.address());
                if (existing < 0) {
                    ServerData data = new ServerData(
                            PartnerServers.displayName(partner),
                            partner.address(),
                            ServerData.Type.OTHER);
                    // No save() during load â€” re-injected every load, avoids recursion.
                    list.add(data, false);
                    existing = list.size() - 1;
                } else {
                    ServerData data = list.get(existing);
                    if (data != null) {
                        data.name = PartnerServers.displayName(partner);
                    }
                }
                // Bubble-swap to pinned slot so partners stay at indices 0..n-1 in catalog order.
                while (existing > pinIndex) {
                    list.swap(existing, existing - 1);
                    existing--;
                }
                pinIndex++;
            }
        } finally {
            ENSURING.set(false);
        }
    }

    public static void ensureLoaded() {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        ensurePartners(list);
    }

    private static int indexOfAddress(ServerList list, String address) {
        String want = hostKey(address);
        for (int i = 0; i < list.size(); i++) {
            ServerData data = list.get(i);
            if (data != null && PartnerServers.isPartnerAddress(data.ip)
                    && hostKey(data.ip).equals(want)) {
                return i;
            }
        }
        return -1;
    }

    private static String hostKey(String address) {
        if (address == null) {
            return "";
        }
        String a = address.trim().toLowerCase();
        int colon = a.lastIndexOf(':');
        if (colon > 0 && !a.startsWith("[")) {
            a = a.substring(0, colon);
        }
        return a;
    }
}
