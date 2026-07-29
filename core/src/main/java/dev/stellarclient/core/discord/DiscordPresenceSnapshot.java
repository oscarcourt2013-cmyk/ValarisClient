package dev.stellarclient.core.discord;

import java.util.List;
import java.util.Objects;

/** Immutable Discord Rich Presence payload. */
public final class DiscordPresenceSnapshot {

    public record Button(String label, String url) {
        public Button {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(url, "url");
        }
    }

    private final String details;
    private final String state;
    private final String largeImageKey;
    private final String largeImageText;
    private final String smallImageKey;
    private final String smallImageText;
    private final Long startEpochSeconds;
    private final List<Button> buttons;

    public DiscordPresenceSnapshot(String details, String state,
                                   String largeImageKey, String largeImageText,
                                   String smallImageKey, String smallImageText,
                                   Long startEpochSeconds, List<Button> buttons) {
        this.details = details != null ? details : "";
        this.state = state != null ? state : "";
        this.largeImageKey = largeImageKey != null && !largeImageKey.isBlank() ? largeImageKey : "prime_logo";
        this.largeImageText = largeImageText != null ? largeImageText : "StellarClient";
        this.smallImageKey = smallImageKey != null ? smallImageKey : "";
        this.smallImageText = smallImageText != null ? smallImageText : "";
        this.startEpochSeconds = startEpochSeconds;
        this.buttons = buttons != null ? List.copyOf(buttons) : List.of();
    }

    public String details() {
        return details;
    }

    public String state() {
        return state;
    }

    public String largeImageKey() {
        return largeImageKey;
    }

    public String largeImageText() {
        return largeImageText;
    }

    public String smallImageKey() {
        return smallImageKey;
    }

    public String smallImageText() {
        return smallImageText;
    }

    public Long startEpochSeconds() {
        return startEpochSeconds;
    }

    public List<Button> buttons() {
        return buttons;
    }

    /** Fingerprint used to skip redundant IPC updates. */
    public String fingerprint() {
        return details + '\0' + state + '\0' + largeImageKey + '\0' + smallImageKey
                + '\0' + startEpochSeconds + '\0' + buttons;
    }
}
