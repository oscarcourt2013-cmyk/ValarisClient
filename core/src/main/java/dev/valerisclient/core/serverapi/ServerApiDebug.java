package dev.valerisclient.core.serverapi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Debug snapshot for {@code /prime debug}. */
public final class ServerApiDebug {

    public enum HandshakeState {
        NONE,
        SENT,
        ACCEPTED,
        REJECTED
    }

    private volatile HandshakeState handshake = HandshakeState.NONE;
    private volatile boolean channelAvailable;
    private volatile String lastRejectReason = "";
    private final ArrayDeque<String> recentPackets = new ArrayDeque<>(8);

    public HandshakeState handshake() {
        return handshake;
    }

    public void setHandshake(HandshakeState state) {
        this.handshake = state == null ? HandshakeState.NONE : state;
    }

    public boolean channelAvailable() {
        return channelAvailable;
    }

    public void setChannelAvailable(boolean channelAvailable) {
        this.channelAvailable = channelAvailable;
    }

    public String lastRejectReason() {
        return lastRejectReason;
    }

    public void setLastRejectReason(String reason) {
        this.lastRejectReason = reason == null ? "" : reason;
    }

    public synchronized void recordPacket(String direction, String type, String raw) {
        String line = direction + " " + type + " " + truncate(raw, 120);
        while (recentPackets.size() >= 5) {
            recentPackets.pollFirst();
        }
        recentPackets.addLast(line);
    }

    public synchronized List<String> recentPackets() {
        return new ArrayList<>(recentPackets);
    }

    public synchronized void reset() {
        handshake = HandshakeState.NONE;
        channelAvailable = false;
        lastRejectReason = "";
        recentPackets.clear();
    }

    public String formatReport(int protocol, ValerisAccountManager account) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§lPrime Server API§r\n");
        sb.append("§7Channel: §f").append(ServerApiProtocol.CHANNEL).append('\n');
        sb.append("§7Protocol: §f").append(protocol).append('\n');
        sb.append("§7Compatible: §f").append(channelAvailable ? "yes" : "no / unknown").append('\n');
        sb.append("§7Handshake: §f").append(handshake.name());
        if (handshake == HandshakeState.REJECTED && !lastRejectReason.isBlank()) {
            sb.append(" §8(").append(lastRejectReason).append(')');
        }
        sb.append('\n');
        sb.append("§7Account: §fLv ").append(account.getLevel())
                .append(" · ").append(account.getXP()).append(" XP")
                .append(account.isLogged() ? " · logged" : "")
                .append('\n');
        sb.append("§7Recent packets:§r\n");
        List<String> packets = recentPackets();
        if (packets.isEmpty()) {
            sb.append("§8  (none)§r");
        } else {
            for (String p : packets) {
                sb.append("§8  ").append(p).append("§r\n");
            }
        }
        return sb.toString().trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String oneLine = s.replace('\n', ' ');
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }
}
