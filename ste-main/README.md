# Valaris Launcher

Valaris is an independent Windows launcher for Minecraft: Java Edition with a custom violet/cyan interface.

## Included

- Complete official Mojang version catalog, including releases, snapshots, old beta, and old alpha builds
- Verified client, library, asset, native, and legacy-resource downloads
- Isolated profiles with per-profile memory and game folders
- Microsoft device-code sign-in and local/offline testing accounts
- Saved multiplayer servers with direct launch
- Java, memory, storage, appearance, cache, and catalog settings
- Discord Rich Presence
- Live download progress and game console
- Windows installer and portable unpacked build

## Run from source

```powershell
npm.cmd install
npm.cmd start
```

Build the Windows installer:

```powershell
npm.cmd run dist
```

## Microsoft account setup

Microsoft does not provide a generic client ID that third-party launchers can safely share. Create an app registration in Microsoft Entra:

1. Select personal Microsoft accounts as a supported account type.
2. Enable public client flows.
3. Copy the application (client) ID.
4. Paste it into **Settings → General → Microsoft sign-in** in Valaris.

Players still need to own Minecraft: Java Edition on the connected Microsoft account.

## Discord Rich Presence setup

1. Create an application in the Discord Developer Portal.
2. Copy its application ID into **Settings → General → Discord integration**.
3. Optionally upload a Rich Presence art asset named `valaris`.
4. Keep the Discord desktop app running.

## Java

Valaris uses the Java executable configured under **Settings → Game & performance**. Different Minecraft generations require different Java versions; Java 8 is commonly needed for historical versions, Java 17 for many modern versions, and Java 21 for current releases.

Valaris is not affiliated with Mojang Studios or Microsoft.
