# ValarisClient (Paper / Purpur plugin)

Server-side bridge for the **ValarisClient** Fabric mod.

- Channel: `ValarisClient:main` (protocol 1 — see [docs/SERVER_API.md](../docs/SERVER_API.md))
- Detects official clients via handshake (UUID + name + protocol checks)
- SQLite (default) or MySQL
- XP, rewards, achievements, missions, `/valaris` GUI
- PlaceholderAPI: `%ValarisClient_status%` `%ValarisClient_version%` `%ValarisClient_level%`

## Build

```bash
cd valaris-plugin
../gradlew.bat build
```

Jar: `build/libs/ValarisClient-1.0.0.jar`

## Install

1. Drop the jar into `plugins/`
2. Restart (Paper/Purpur **1.21.11**)
3. Edit `plugins/ValarisClient/config.yml`
4. Optional: PlaceholderAPI, Vault (for `eco give` reward commands)

## Commands

| Command | Permission |
|---------|------------|
| `/valaris` | `ValarisClient.use` |
| `/valaris reload` | `ValarisClient.reload` |
| `/valaris info <player>` | `ValarisClient.info` |
| `/valaris rewards` | `ValarisClient.use` |
| `/valaris achievements` | `ValarisClient.use` |

## API (other plugins)

```java
ValarisClientAPI api = ValarisClientPlugin.api();
if (api.isValarisClient(player)) {
    api.addPrimeXP(player.getUniqueId(), 10, "SERVER_EVENT");
}
```
