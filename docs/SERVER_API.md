# StellarClient Server API

Custom payload channel for partner Minecraft servers to detect and integrate with **StellarClient**.

| | |
|--|--|
| Channel | `StellarClient:main` |
| Protocol | `1` |
| Transport | Fabric `CustomPacketPayload` / plugin messaging |
| Direction | C2S handshake + profile Â· S2C sync / XP / rewards / notify |

> Handshake proves the player runs StellarClient. **Do not trust the client for grants, economy, or permissions** â€” always validate rewards server-side.

## Handshake

On join, when the server advertises the channel, the client sends:

```json
{
  "t": "HANDSHAKE",
  "client": "StellarClient",
  "version": "2.0.0",
  "protocol": 1,
  "account": "<player-uuid>"
}
```

Server replies:

```json
{ "t": "SERVER_ACCEPTED", "message": "Welcome to Elysia SMP" }
```

or

```json
{ "t": "SERVER_REJECTED", "message": "Outdated protocol" }
```

## Packet types

| `t` | Direction | Purpose |
|-----|-----------|---------|
| `HANDSHAKE` | C2S | Client identity |
| `SERVER_ACCEPTED` / `SERVER_REJECTED` | S2C | Handshake result |
| `ACCOUNT_SYNC` | S2C | `{ level, xp, logged, rewards[], friends[] }` |
| `XP` | S2C | `{ amount, kind: SERVER_PLAYTIME\|SERVER_EVENT\|SERVER_ACHIEVEMENT }` |
| `REWARD` | S2C | `{ title, message, action: message\|link\|open_ui\|notification, link? }` |
| `NOTIFY` | S2C | `{ kind: friend\|reward\|event\|announce, title, message, chat? }` |
| `PROFILE_REQUEST` | S2C | Ask client for profile |
| `PROFILE` | C2S | `{ client, version, protocol, level, xp, logged }` |

Unknown `t` values are ignored (forward compatible).

## Client managers

- `PrimeAccountManager` â€” `getLevel()`, `getXP()`, `getFriends()`, `isLogged()`
- HUD toasts via `NotificationManager` (e.g. `âš¡ +50 XP Prime`)
- `/prime debug` â€” channel, handshake, protocol, last packets (client-only)

## Partner servers

StellarClient pins partner entries in the multiplayer list (non-removable):

- **Elysia SMP** â€” `elysiasmp.fr`

## Paper / Spigot example (plugin messaging)

Production plugin (recommended): [`prime-plugin/`](../prime-plugin/) â€” Paper/Purpur 1.21.11, SQLite/MySQL, XP, rewards, PAPI.

Minimal listener sketch:
```java
public final class PrimeBridge implements PluginMessageListener {
    public static final String CHANNEL = "StellarClient:main";

    public void register(JavaPlugin plugin) {
        var messenger = Bukkit.getMessenger();
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;
        String json = new String(message, StandardCharsets.UTF_8);
        // Parse "t":"HANDSHAKE" â€¦ then:
        player.sendPluginMessage(
            /* plugin */, CHANNEL,
            "{\"t\":\"SERVER_ACCEPTED\",\"message\":\"Welcome\"}".getBytes(StandardCharsets.UTF_8));
    }
}
```

> On modern Paper + Fabric clients, prefer the Fabric custom payload codec when both sides are Fabric. Plugin messaging remains the common bridge for Bukkit-based partners.

## Fabric server example (sketch)

```java
// Register the same PayloadType + StreamCodec as the client (UTF-8 JSON string).
ServerPlayNetworking.registerGlobalReceiver(MainPayload.TYPE, (payload, context) -> {
    // parse payload.json(), reply with ServerPlayNetworking.send(...)
});
```

## Security

1. Treat handshake as **detection only**.
2. Never award XP / items from client claims without server logic.
3. Rate-limit inbound plugin messages.
4. Reject mismatched protocol versions (`protocol != 1`).

## Compatibility

- Minecraft **1.21.x** and **26.2** StellarClient jars
- Protocol `1` â€” bump only on breaking changes; keep old servers working by ignoring unknown fields

## Debug

In-game chat:

```
/prime debug
```

Shows channel availability, handshake state, protocol, and the last 5 packets.
