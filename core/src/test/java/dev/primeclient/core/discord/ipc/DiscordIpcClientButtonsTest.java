package dev.stellarclient.core.discord.ipc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stellarclient.core.discord.DiscordPresenceSnapshot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordIpcClientButtonsTest {

    @Test
    void serializesButtonsAsLabelUrlObjects() throws Exception {
        DiscordIpcClient client = new DiscordIpcClient("1525574680994648174");
        Method build = DiscordIpcClient.class.getDeclaredMethod(
                "buildSetActivity", DiscordPresenceSnapshot.class, boolean.class);
        build.setAccessible(true);

        DiscordPresenceSnapshot snapshot = new DiscordPresenceSnapshot(
                "In game",
                "StellarClient",
                "prime_logo",
                "Prime",
                "prime_logo",
                "",
                null,
                List.of(
                        new DiscordPresenceSnapshot.Button("StellarClient", "https://prime.example"),
                        new DiscordPresenceSnapshot.Button("Discord", "https://discord.com/app")
                ));

        String json = (String) build.invoke(client, snapshot, true);
        JsonObject activity = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject("args")
                .getAsJsonObject("activity");
        JsonArray buttons = activity.getAsJsonArray("buttons");

        assertEquals(2, buttons.size());
        assertEquals("StellarClient", buttons.get(0).getAsJsonObject().get("label").getAsString());
        assertEquals("https://prime.example", buttons.get(0).getAsJsonObject().get("url").getAsString());
        assertTrue(activity.get("metadata") == null || activity.get("metadata").isJsonNull());
    }
}
