package dev.valarisclient.v1_21_11.multiplayer;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.servers.PartnerServerState;
import dev.valarisclient.core.servers.PartnerServers;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds partner servers to the vanilla multiplayer list, once each.
 *
 * <p>Deliberately not sticky: a partner is offered a single time and the entry
 * then belongs to the player, who may rename, reorder or delete it. An earlier
 * build re-pinned partners on every load and cancelled deletion, which made
 * them impossible to get rid of.</p>
 */
public final class PartnerServerList {

    private PartnerServerList() {
    }

    public static void sync(ServerList list) {
        if (list == null) {
            return;
        }
        boolean changed = removeLegacyPartners(list);
        changed |= addNewPartners(list);
        if (changed) {
            list.save();
        }
    }

    /** Strips entries an older build pinned here and refused to delete. */
    private static boolean removeLegacyPartners(ServerList list) {
        List<ServerData> stale = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ServerData data = list.get(i);
            if (data != null && PartnerServers.isLegacyInjectedAddress(data.ip)) {
                stale.add(data);
            }
        }
        for (ServerData data : stale) {
            list.remove(data);
        }
        return !stale.isEmpty();
    }

    private static boolean addNewPartners(ServerList list) {
        ValarisClient client = ValarisClient.get();
        PartnerServerState state = client == null ? null : client.partnerServers();
        if (state == null) {
            return false;
        }
        boolean changed = false;
        for (PartnerServers.Entry partner : PartnerServers.partners()) {
            // markOffered is true only the first time, so a deleted partner
            // is not resurrected on the next load.
            if (!state.markOffered(partner.address()) || contains(list, partner.address())) {
                continue;
            }
            list.add(new ServerData(
                    PartnerServers.displayName(partner),
                    partner.address(),
                    ServerData.Type.OTHER), false);
            changed = true;
        }
        return changed;
    }

    private static boolean contains(ServerList list, String address) {
        String want = hostKey(address);
        for (int i = 0; i < list.size(); i++) {
            ServerData data = list.get(i);
            if (data != null && hostKey(data.ip).equals(want)) {
                return true;
            }
        }
        return false;
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
