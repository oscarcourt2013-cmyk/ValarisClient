# StellarClient (Paper / Purpur plugin)

Server-side bridge for the **StellarClient** Fabric mod.

- Channel: `StellarClient:main` (protocol 1 â€” see [docs/SERVER_API.md](../docs/SERVER_API.md))
- Detects official clients via handshake (UUID + name + protocol checks)
- SQLite (default) or MySQL
- XP, rewards, achievements, missions, `/prime` GUI
- PlaceholderAPI: `%StellarClient_status%` `%StellarClient_version%` `%StellarClient_level%`

## Build

```bash
cd prime-plugin
../gradlew.bat build
```

Jar: `build/libs/StellarClient-1.0.0.jar`

## Install

1. Drop the jar into `plugins/`
2. Restart (Paper/Purpur **1.21.11**)
3. Edit `plugins/StellarClient/config.yml`
4. Optional: PlaceholderAPI, Vault (for `eco give` reward commands)

## Commands

| Command | Permission |
|---------|------------|
| `/prime` | `StellarClient.use` |
| `/prime reload` | `StellarClient.reload` |
| `/prime info <player>` | `StellarClient.info` |
| `/prime rewards` | `StellarClient.use` |
| `/prime achievements` | `StellarClient.use` |

## API (other plugins)

```java
StellarClientAPI api = StellarClientPlugin.api();
if (api.isStellarClient(player)) {
    api.addPrimeXP(player.getUniqueId(), 10, "SERVER_EVENT");
}
```
