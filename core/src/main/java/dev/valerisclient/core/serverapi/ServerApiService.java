package dev.valerisclient.core.serverapi;

import com.google.gson.JsonObject;
import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.design.ValerisVersion;
import dev.valerisclient.core.notification.NotificationManager;
import dev.valerisclient.core.servers.PartnerServers;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Client-side bus for the {@code valerisclient:main} custom payload channel.
 */
public final class ServerApiService {

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private final PrimeAccountManager account;
    private final PrimeXpHandler xpHandler;
    private final PrimeRewardHandler rewardHandler;
    private final PrimeNotifyHandler notifyHandler;
    private final ServerApiDebug debug = new ServerApiDebug();

    private volatile Consumer<String> outboundSender;
    private volatile BooleanSupplier channelProbe;
    private boolean friendPresenceAnnounced;

    public ServerApiService(
            MinecraftAdapter adapter,
            NotificationManager notifications) {
        this.adapter = adapter;
        this.notifications = notifications;
        this.account = new PrimeAccountManager();
        this.xpHandler = new PrimeXpHandler(account, notifications);
        this.rewardHandler = new PrimeRewardHandler(notifications, adapter);
        this.notifyHandler = new PrimeNotifyHandler(notifications, adapter);
    }

    public PrimeAccountManager account() {
        return account;
    }

    public ServerApiDebug debug() {
        return debug;
    }

    /** Wired by version-layer networking. */
    public void setOutboundSender(Consumer<String> sender) {
        this.outboundSender = sender;
    }

    /** Wired by version-layer networking — {@code ClientPlayNetworking.canSend}. */
    public void setChannelProbe(BooleanSupplier probe) {
        this.channelProbe = probe;
    }

    public void onWorldJoin() {
        friendPresenceAnnounced = false;
        debug.setHandshake(ServerApiDebug.HandshakeState.NONE);
        debug.setLastRejectReason("");
        BooleanSupplier probe = channelProbe;
        boolean can = probe != null && probe.getAsBoolean();
        debug.setChannelAvailable(can);
        if (can) {
            sendHandshake();
        }
        maybeAnnounceFriendsOnPartner();
    }

    public void onWorldLeave() {
        account.resetSession();
        debug.reset();
        friendPresenceAnnounced = false;
    }

    public void markChannelAvailable(boolean available) {
        debug.setChannelAvailable(available);
    }

    public String buildHandshakeJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("t", ServerApiProtocol.T_HANDSHAKE);
        obj.addProperty("client", ServerApiProtocol.CLIENT_NAME);
        obj.addProperty("version", ValerisVersion.VERSION);
        obj.addProperty("protocol", ServerApiProtocol.PROTOCOL);
        String uuid = adapter.playerUuid();
        obj.addProperty("account", uuid != null ? uuid : "");
        return obj.toString();
    }

    public void sendHandshake() {
        Consumer<String> sender = outboundSender;
        if (sender == null) {
            return;
        }
        String json = buildHandshakeJson();
        debug.recordPacket("C2S", ServerApiProtocol.T_HANDSHAKE, json);
        debug.setHandshake(ServerApiDebug.HandshakeState.SENT);
        try {
            sender.accept(json);
        } catch (Exception ignored) {
            debug.setChannelAvailable(false);
        }
    }

    public void onPayload(String json) {
        JsonObject obj = ServerApiProtocol.parse(json);
        String type = ServerApiProtocol.typeOf(obj);
        debug.recordPacket("S2C", type.isBlank() ? "?" : type, json);
        debug.setChannelAvailable(true);

        switch (type) {
            case ServerApiProtocol.T_SERVER_ACCEPTED -> {
                debug.setHandshake(ServerApiDebug.HandshakeState.ACCEPTED);
                String msg = ServerApiProtocol.str(obj, "message", "Server accepted ValerisClient");
                notifications.success("Valeris", msg);
                maybeAnnounceFriendsOnPartner();
            }
            case ServerApiProtocol.T_SERVER_REJECTED -> {
                debug.setHandshake(ServerApiDebug.HandshakeState.REJECTED);
                String reason = ServerApiProtocol.str(obj, "message",
                        ServerApiProtocol.str(obj, "reason", "rejected"));
                debug.setLastRejectReason(reason);
                notifications.warning("Valeris", "Server rejected handshake: " + reason);
            }
            case ServerApiProtocol.T_ACCOUNT_SYNC -> {
                account.applyAccountSync(obj);
                notifications.info("Valeris", "Account synced · Lv " + account.getLevel());
            }
            case ServerApiProtocol.T_XP, "SERVER_PLAYTIME", "SERVER_EVENT", "SERVER_ACHIEVEMENT" -> {
                if (!ServerApiProtocol.T_XP.equals(type) && !obj.has("kind")) {
                    obj.addProperty("kind", type);
                }
                xpHandler.handle(obj);
            }
            case ServerApiProtocol.T_REWARD, ServerApiProtocol.REWARD_AVAILABLE -> rewardHandler.handle(obj);
            case ServerApiProtocol.T_NOTIFY -> notifyHandler.handle(obj);
            case ServerApiProtocol.T_PROFILE_REQUEST -> sendProfile();
            default -> {
                // Unknown types ignored for forward compatibility.
            }
        }
    }

    private void sendProfile() {
        Consumer<String> sender = outboundSender;
        if (sender == null) {
            return;
        }
        JsonObject profile = new JsonObject();
        profile.addProperty("t", ServerApiProtocol.T_PROFILE);
        profile.addProperty("client", ServerApiProtocol.CLIENT_NAME);
        profile.addProperty("version", ValerisVersion.VERSION);
        profile.addProperty("protocol", ServerApiProtocol.PROTOCOL);
        profile.addProperty("level", account.getLevel());
        profile.addProperty("xp", account.getXP());
        profile.addProperty("logged", account.isLogged());
        String json = profile.toString();
        debug.recordPacket("C2S", ServerApiProtocol.T_PROFILE, json);
        sender.accept(json);
    }

    /**
     * Handles {@code /prime} client commands. Returns {@code true} if the chat
     * message must not be sent to the server.
     */
    public boolean handleClientCommand(String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (!text.regionMatches(true, 0, "/prime", 0, 6)) {
            return false;
        }
        String rest = text.length() > 6 ? text.substring(6).trim() : "";
        if (rest.isEmpty() || "help".equalsIgnoreCase(rest)) {
            adapter.displayClientMessage("§6[Valeris] §f/prime debug §7— Server API status");
            return true;
        }
        if ("debug".equalsIgnoreCase(rest)) {
            adapter.displayClientMessage(debug.formatReport(ServerApiProtocol.PROTOCOL, account));
            return true;
        }
        adapter.displayClientMessage("§6[Valeris] §cUnknown subcommand. Try §f/prime debug");
        return true;
    }

    private void maybeAnnounceFriendsOnPartner() {
        if (friendPresenceAnnounced) {
            return;
        }
        String server = adapter.serverAddress();
        if (!PartnerServers.isPartnerAddress(server)) {
            return;
        }
        List<PrimeAccountManager.FriendView> friends = account.getFriends();
        for (PrimeAccountManager.FriendView f : friends) {
            if (f.serverAddress() == null || f.serverAddress().isBlank()) {
                continue;
            }
            if (!PartnerServers.isPartnerAddress(f.serverAddress())
                    && !normalizeHost(f.serverAddress()).equals(normalizeHost(server))) {
                continue;
            }
            String status = f.status() == null ? "" : f.status().toLowerCase(Locale.ROOT);
            if (!"in-game".equals(status) && !"online".equals(status)) {
                continue;
            }
            notifications.info("Friends",
                    "⚡ " + f.username() + " joue actuellement sur Elysia SMP");
            friendPresenceAnnounced = true;
            return;
        }
    }

    private static String normalizeHost(String address) {
        if (address == null) {
            return "";
        }
        String a = address.trim().toLowerCase(Locale.ROOT);
        int colon = a.lastIndexOf(':');
        if (colon > 0 && a.indexOf(']') < 0) {
            a = a.substring(0, colon);
        }
        return a;
    }

    /** Convenience for hooks before {@link ValerisClient} is fully wired in tests. */
    public static boolean tryHandleClientCommand(String message) {
        try {
            return ValerisClient.get().serverApi().handleClientCommand(message);
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
