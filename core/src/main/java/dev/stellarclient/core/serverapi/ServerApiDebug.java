package dev.stellarclient.core.serverapi;

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

    public String formatReport(int protocol, PrimeAccountManager account) {
        StringBuilder sb = new StringBuilder();
        sb.append("Â§6Â§lPrime Server APIÂ§r\n");
        sb.append("Â§7Channel: Â§f").append(ServerApiProtocol.CHANNEL).append('\n');
        sb.append("Â§7Protocol: Â§f").append(protocol).append('\n');
        sb.append("Â§7Compatible: Â§f").append(channelAvailable ? "yes" : "no / unknown").append('\n');
        sb.append("Â§7Handshake: Â§f").append(handshake.name());
        if (handshake == HandshakeState.REJECTED && !lastRejectReason.isBlank()) {
            sb.append(" Â§8(").append(lastRejectReason).append(')');
        }
        sb.append('\n');
        sb.append("Â§7Account: Â§fLv ").append(account.getLevel())
                .append(" Â· ").append(account.getXP()).append(" XP")
                .append(account.isLogged() ? " Â· logged" : "")
                .append('\n');
        sb.append("Â§7Recent packets:Â§r\n");
        List<String> packets = recentPackets();
        if (packets.isEmpty()) {
            sb.append("Â§8  (none)Â§r");
        } else {
            for (String p : packets) {
                sb.append("Â§8  ").append(p).append("Â§r\n");
            }
        }
        return sb.toString().trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String oneLine = s.replace('\n', ' ');
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "â€¦";
    }
}
