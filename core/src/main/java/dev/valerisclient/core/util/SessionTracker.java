package dev.valerisclient.core.util;

/** Tracks how long the current in-game session has been running. */
public final class SessionTracker {

    private long sessionStartMillis;

    public void onJoin() {
        sessionStartMillis = System.currentTimeMillis();
    }

    public void onLeave() {
        sessionStartMillis = 0;
    }

    public long sessionMillis() {
        return sessionStartMillis == 0 ? 0 : System.currentTimeMillis() - sessionStartMillis;
    }
}
