# ValerisClient (Paper / Purpur plugin)

Server-side bridge for the **ValerisClient** Fabric mod.

- Channel: `ValerisClient:main` (protocol 1 — see [docs/SERVER_API.md](../docs/SERVER_API.md))
- Detects official clients via handshake (UUID + name + protocol checks)
- SQLite (default) or MySQL
- XP, rewards, achievements, missions, `/prime` GUI
- PlaceholderAPI: `%ValerisClient_status%` `%ValerisClient_version%` `%ValerisClient_level%`

## Build

```bash
cd prime-plugin
../gradlew.bat build
```

Jar: `build/libs/ValerisClient-1.0.0.jar`

## Install

1. Drop the jar into `plugins/`
2. Restart (Paper/Purpur **1.21.11**)
3. Edit `plugins/ValerisClient/config.yml`
4. Optional: PlaceholderAPI, Vault (for `eco give` reward commands)

## Commands

| Command | Permission |
|---------|------------|
| `/prime` | `ValerisClient.use` |
| `/prime reload` | `ValerisClient.reload` |
| `/prime info <player>` | `ValerisClient.info` |
| `/prime rewards` | `ValerisClient.use` |
| `/prime achievements` | `ValerisClient.use` |

## API (other plugins)

```java
ValerisClientAPI api = ValerisClientPlugin.api();
if (api.isValerisClient(player)) {
    api.addPrimeXP(player.getUniqueId(), 10, "SERVER_EVENT");
}
```
