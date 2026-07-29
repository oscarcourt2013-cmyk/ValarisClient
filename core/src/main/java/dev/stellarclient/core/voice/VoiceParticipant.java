package dev.stellarclient.core.voice;

/** A StellarClient user in the current voice room. */
public final class VoiceParticipant {

    private final String id;
    private final String name;
    private volatile boolean speaking;
    private volatile double x;
    private volatile double y;
    private volatile double z;
    private volatile String groupId = "";

    public VoiceParticipant(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String groupId() {
        return groupId == null ? "" : groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId == null ? "" : groupId;
    }

    public boolean inGroup(String group) {
        return group != null && !group.isBlank() && group.equals(groupId());
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean speaking() {
        return speaking;
    }

    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
