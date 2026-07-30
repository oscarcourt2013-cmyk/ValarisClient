# Handoff

State of the project as of 2026-07-30. Read this first if you are picking the
work up cold.

## What this repo is

A Fabric mod for **Minecraft 1.21.11** — the ValarisClient custom client — plus
two things that serve it.

```
core/           version-independent: modules, HUD, ClickGUI, config, themes.
                Never references a Minecraft class; everything goes through
                dev.valarisclient.core.adapter.*
mc-1.21.11/     the Fabric layer: mixins, screens, the adapter implementation
valaris-plugin/ Bukkit/Paper plugin, the server side of the valarisclient:main
                plugin channel
launcherfiles/  an Electron Minecraft launcher, written by a friend. Not wired
                to the client yet — see "Next" below
tools/ scripts/ generate the mod's cosmetic textures and its lang files
```

## Build and run

```bash
./gradlew :mc-1.21.11:build      # -> mc-1.21.11/build/libs/valaris-client-1.21.11-1.0.0.jar
./gradlew :mc-1.21.11:runClient  # launches a dev client
```

Needs **JDK 21**. Install the jar with Fabric Loader + Fabric API into any
launcher's `mods/` folder.

```bash
cd launcherfiles && npm install && npm start
```

## Decisions worth knowing

- **The brand is "Valaris" with an 'a'.** It was Prime, then Stellar, then
  Valeris, now Valaris. The repo was recreated under the matching name, so
  everything agrees. No reference to Prime, Feather, Lunar or Badlion may
  reappear anywhere.
- **1.21.11 is the only version, deliberately.** `mc-26.2` and a `mc-1.21.1`
  scaffold were deleted. 26.2 needed a JDK 25 that was never installed, so it
  could not produce a jar. Do not propose porting to more versions unasked.
- **The launcher that used to live here was deleted**, along with the website,
  brand art and docs. `launcherfiles/` replaces it.
- **The strict monochrome palette rule applies to launcher UI, not to the
  in-game client.** The client keeps its red accent and coloured HUD swatches.
- **Discord Rich Presence** publishes as application `1532388471795093656`. The
  previous id was inherited from the forked codebase and belonged to someone
  else, so presence was switched off until this one existed. For the icon to
  render, an art asset keyed `valaris_logo` must exist on that application.

## Traps

- `PrimedTnt`, `nearestPrimedTntFuseSeconds`, `nearbyPrimedTntCount` are vanilla
  Minecraft and plain English. A brand-rename script must require a non-letter
  or uppercase character after "prime", or it will break the build.
- `CosmeticTextures` maps ids to texture paths by hand. Renaming an id without
  renaming its file gives a missing texture at runtime and no compile error.
  This has happened once already.
- `ValarisLogo.SRC_WIDTH/SRC_HEIGHT` must equal the real pixel size of
  `textures/gui/logo.png`, or the logo renders stretched.
- The HUD editor's layout is derived from canvas size, and the effective GUI
  resolution can be 320x240. Anything with a `Math.max(<floor>, available/n)`
  will overflow the panel at small sizes.
- `mc-1.21.11/src/main/resources/valarisclient.mixins.json`, the Fabric mod id,
  and `assets/valarisclient/` must stay lowercase and agree with each other. A
  mismatch crashes the client at launch with an `IdentifierException`.

## Next

The launcher and the client do not talk to each other yet.

The client already has the hooks: `core/.../account/LauncherAccountStore.java`
reads and writes `%APPDATA%\valaris-client-launcher\accounts.json` so the
in-game title menu can switch accounts without relaunching. It is inert while
no launcher writes that file — an empty list, not a crash.

To connect them, look at `launcherfiles/src/auth.js` and
`launcherfiles/src/store.js` for how that launcher stores accounts, then either
point `accountsPath()` at its data directory or map its format onto the record
in `LauncherAccountStore`. Note `primeAccount` is bidirectional: the client
writes `tier` back into it.

Also outstanding:

- No GitHub release exists, so the in-game download button 404s. Tagging `v1.0.0`
  runs `.github/workflows/release.yml`, which builds and attaches the jar.
- `launcherfiles` needs its own Microsoft client id before its sign-in works —
  its README explains the Entra app registration.
