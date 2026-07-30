package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.event.ChatMessageEvent;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.StringSetting;
import dev.valarisclient.core.notification.NotificationManager;

import java.util.Arrays;
import java.util.Locale;

/** Parses chat for friend deaths and pings you. */
public final class FriendDeathPingModule extends Module {

    private final StringSetting friends =
            addSetting(new StringSetting("friends", "Friends", "Comma-separated friend names", ""));

    private final NotificationManager notifications;

    public FriendDeathPingModule(NotificationManager notifications) {
        super("friend-death-ping", "Friend Death Ping", "Notify when a friend dies in chat", ModuleCategory.SURVIVAL);
        this.notifications = notifications;
        listen(ChatMessageEvent.class, this::onChat);
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing()) {
            return;
        }
        String raw = friends.get().trim();
        if (raw.isEmpty()) {
            return;
        }
        String lower = event.text().toLowerCase(Locale.ROOT);
        if (!lower.contains("died") && !lower.contains("slain") && !lower.contains("killed")) {
            return;
        }
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(name -> lower.contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .ifPresent(name -> notifications.error("Friend Down", name + " died!"));
    }
}
