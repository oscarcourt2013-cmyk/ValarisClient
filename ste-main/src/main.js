const { app, BrowserWindow, ipcMain, dialog, shell } = require("electron");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const { execFile } = require("child_process");
const { Readable } = require("stream");
const { pipeline } = require("stream/promises");
const { Store } = require("./store");
const minecraft = require("./minecraft");
const modrinth = require("./modrinth");
const { DiscordIpcClient } = require("./discordIpcClient");
const auth = require("./auth");

const DEFAULT_DISCORD_CLIENT_ID = "1532351441006759966";

let window;
let store;
let rpc;
let microsoftAttempt;

function minecraftRoot() {
  return store.data.settings.gameDirectory || path.join(app.getPath("userData"), "minecraft");
}

function publicState() {
  return {
    ...store.get(),
    accounts: store.data.accounts.map(({ accessToken, refreshToken, ...account }) => account)
  };
}

function compareVersions(a, b) {
  const left = String(a || "0").split(/[.-]/).map((part) => Number.parseInt(part, 10) || 0);
  const right = String(b || "0").split(/[.-]/).map((part) => Number.parseInt(part, 10) || 0);
  const length = Math.max(left.length, right.length);
  for (let i = 0; i < length; i++) {
    if ((left[i] || 0) > (right[i] || 0)) return 1;
    if ((left[i] || 0) < (right[i] || 0)) return -1;
  }
  return 0;
}

function filenameFromDisposition(value) {
  const match = String(value || "").match(/filename\*?=(?:UTF-8''|")?([^";]+)/i);
  if (!match) return "";
  return decodeURIComponent(match[1].replace(/"/g, "").trim());
}

function safeUpdateFilename(value, fallback = "Valaris-Update.exe") {
  const clean = path.basename(String(value || fallback)).replace(/[<>:"/\\|?*\x00-\x1F]/g, "_");
  return clean || fallback;
}

function updateKey(manifest) {
  return [
    manifest?.latestVersion || "",
    manifest?.publishedAt || "",
    manifest?.file?.filename || "",
    manifest?.file?.updatedAt || ""
  ].join("|");
}

async function downloadUpdateFile(url, preferredFilename = "") {
  const parsed = new URL(String(url || ""));
  if (!["http:", "https:"].includes(parsed.protocol)) throw new Error("Update download URL must be http or https.");
  const response = await fetch(parsed, { cache: "no-store" });
  if (!response.ok) throw new Error(`Update download failed (${response.status}).`);
  const filename = safeUpdateFilename(
    preferredFilename || filenameFromDisposition(response.headers.get("content-disposition")) || path.basename(parsed.pathname),
    "Valaris-Update.exe"
  );
  const folder = path.join(app.getPath("userData"), "updates");
  fs.mkdirSync(folder, { recursive: true });
  const target = path.join(folder, filename);
  if (response.body) {
    await pipeline(Readable.fromWeb(response.body), fs.createWriteStream(target));
  } else {
    fs.writeFileSync(target, Buffer.from(await response.arrayBuffer()));
  }
  return target;
}

function send(channel, value) {
  if (window && !window.isDestroyed()) window.webContents.send(channel, value);
}

function isDiscordRunning() {
  return new Promise((resolve) => {
    if (process.platform === "win32") {
      execFile("tasklist", ["/NH"], { windowsHide: true }, (error, stdout) => {
        if (error) return resolve(false);
        resolve(/\b(discord|discordptb|discordcanary)\.exe\b/i.test(stdout));
      });
      return;
    }
    execFile("pgrep", ["-if", "Discord|discord"], (error, stdout) => {
      resolve(!error && Boolean(stdout.trim()));
    });
  });
}

function stellarActivity(profile, launched = false) {
  const version = profile?.baseVersion || profile?.version;
  return {
    details: "Valaris Client",
    state: launched && profile ? `Playing ${version} - ${profile.name}` : profile ? `Ready: ${profile.name}` : "In the launcher",
    startTimestamp: new Date(),
    largeImageKey: "valaris_logo",
    largeImageText: "Valaris Client",
    buttons: [
      { label: "Valaris Client", url: `https://discord.com/applications/${store.data.settings.discordClientId || DEFAULT_DISCORD_CLIENT_ID}` },
      { label: "Discord", url: "https://discord.com/app" }
    ]
  };
}

function saveProfile(profile) {
  const clean = {
    id: profile.id || crypto.randomUUID(),
    name: String(profile.name || profile.version).trim().slice(0, 40),
    version: String(profile.version),
    baseVersion: profile.baseVersion || "",
    loader: profile.loader || "",
    loaderVersion: profile.loaderVersion || "",
    type: profile.type || "release",
    memory: Math.max(1024, Math.min(32768, Number(profile.memory || store.data.settings.memory))),
    gameDirectory: profile.gameDirectory || "",
    modpack: profile.modpack || null,
    createdAt: profile.createdAt || new Date().toISOString()
  };
  const index = store.data.profiles.findIndex((item) => item.id === clean.id);
  if (index >= 0) store.data.profiles[index] = clean;
  else store.data.profiles.push(clean);
  if (!store.data.activeProfileId) store.data.activeProfileId = clean.id;
  store.save();
  refreshDiscord();
  return clean;
}

async function refreshDiscord() {
  try {
    if (rpc) {
      await rpc.destroy().catch(() => {});
      rpc = null;
    }
    const settings = store.data.settings;
    if (!settings.discordEnabled) return;
    const discordClientId = settings.discordClientId || DEFAULT_DISCORD_CLIENT_ID;
    if (!await isDiscordRunning()) {
      send("discord:status", { connected: false, message: "Discord is not running." });
      return;
    }
    rpc = new DiscordIpcClient(discordClientId);
    const profile = store.data.profiles.find((item) => item.id === store.data.activeProfileId);
    const ok = await rpc.setActivity(stellarActivity(profile));
    if (!ok) throw new Error(rpc.lastError || "Discord rejected the activity.");
    send("discord:status", { connected: true, message: "Discord Rich Presence connected through IPC." });
  } catch (error) {
    rpc = null;
    send("discord:status", { connected: false, message: error.message });
  }
}

function createWindow() {
  window = new BrowserWindow({
    width: 1500,
    height: 920,
    minWidth: 1100,
    minHeight: 720,
    frame: false,
    icon: path.join(__dirname, "..", "assets", "logo.ico"),
    show: false,
    backgroundColor: "#080812",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  window.loadFile(path.join(__dirname, "index.html"));
  window.once("ready-to-show", () => window.show());
  if (process.env.VALARIS_SCREENSHOT) {
    window.webContents.once("did-finish-load", () => {
      setTimeout(async () => {
        const image = await window.capturePage();
        fs.writeFileSync(process.env.VALARIS_SCREENSHOT, image.toPNG());
        app.quit();
      }, 3600);
    });
  }
}

app.whenReady().then(() => {
  store = new Store(path.join(app.getPath("userData"), "valaris.json"));
  createWindow();
  refreshDiscord();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

ipcMain.handle("window:minimize", () => window.minimize());
ipcMain.handle("window:maximize", () => window.isMaximized() ? window.unmaximize() : window.maximize());
ipcMain.handle("window:close", () => window.close());
ipcMain.handle("state:get", () => publicState());
ipcMain.handle("versions:get", () => minecraft.getVersions(path.join(app.getPath("userData"), "cache")));

ipcMain.handle("settings:update", async (_, settings) => {
  store.update({ settings: { ...store.data.settings, ...settings } });
  if (Object.prototype.hasOwnProperty.call(settings, "discordEnabled") || Object.prototype.hasOwnProperty.call(settings, "discordClientId")) {
    await refreshDiscord();
  }
  return publicState();
});

ipcMain.handle("dialog:java", async () => {
  const result = await dialog.showOpenDialog(window, {
    title: "Choose Java executable",
    properties: ["openFile"],
    filters: [{ name: "Java", extensions: ["exe"] }]
  });
  return result.canceled ? null : result.filePaths[0];
});

ipcMain.handle("dialog:folder", async () => {
  const result = await dialog.showOpenDialog(window, {
    title: "Choose Minecraft data folder",
    properties: ["openDirectory", "createDirectory"]
  });
  return result.canceled ? null : result.filePaths[0];
});

ipcMain.handle("folder:open", async (_, target) => {
  const folder = target || minecraftRoot();
  fs.mkdirSync(folder, { recursive: true });
  return shell.openPath(folder);
});

ipcMain.handle("profiles:save", (_, profile) => {
  saveProfile(profile);
  return publicState();
});

ipcMain.handle("profiles:delete", (_, id) => {
  store.data.profiles = store.data.profiles.filter((item) => item.id !== id);
  if (store.data.activeProfileId === id) store.data.activeProfileId = store.data.profiles[0]?.id || null;
  store.save();
  return publicState();
});

ipcMain.handle("profiles:activate", (_, id) => {
  store.update({ activeProfileId: id });
  refreshDiscord();
  return publicState();
});

ipcMain.handle("servers:save", (_, server) => {
  const clean = {
    id: server.id || crypto.randomUUID(),
    name: String(server.name || "Minecraft Server").trim().slice(0, 40),
    address: String(server.address || "").trim().slice(0, 120)
  };
  if (!clean.address) throw new Error("Enter a server address.");
  const index = store.data.servers.findIndex((item) => item.id === clean.id);
  if (index >= 0) store.data.servers[index] = clean;
  else store.data.servers.push(clean);
  store.save();
  return publicState();
});

ipcMain.handle("servers:delete", (_, id) => {
  store.data.servers = store.data.servers.filter((item) => item.id !== id);
  store.save();
  return publicState();
});

ipcMain.handle("accounts:add-offline", (_, username) => {
  store.addOfflineAccount(username);
  return publicState();
});

ipcMain.handle("accounts:delete", (_, id) => {
  store.data.accounts = store.data.accounts.filter((item) => item.id !== id);
  if (store.data.activeAccountId === id) store.data.activeAccountId = store.data.accounts[0]?.id || null;
  store.save();
  return publicState();
});

ipcMain.handle("accounts:activate", (_, id) => {
  store.update({ activeAccountId: id });
  return publicState();
});

ipcMain.handle("auth:microsoft-begin", async () => {
  const clientId = store.data.settings.microsoftClientId;
  const device = await auth.beginMicrosoftLogin(clientId);
  microsoftAttempt = { clientId, device };
  auth.pollMicrosoftLogin(clientId, device, (message) => send("auth:status", { message }))
    .then((account) => {
      account.id = crypto.randomUUID();
      store.data.accounts.push(account);
      store.data.activeAccountId = account.id;
      store.save();
      send("auth:complete", { state: publicState() });
    })
    .catch((error) => send("auth:complete", { error: error.message }));
  return {
    userCode: device.user_code,
    verificationUri: device.verification_uri,
    message: device.message
  };
});

ipcMain.handle("external:open", (_, url) => shell.openExternal(url));

ipcMain.handle("modpacks:search", (_, query) => modrinth.searchModpacks(query));
ipcMain.handle("modpacks:versions", (_, projectId) => modrinth.getModpackVersions(projectId));

ipcMain.handle("modpacks:install", async (_, project) => {
  const profile = await modrinth.installModpack(project, minecraftRoot(), store.data.settings, (progress) => send("minecraft:progress", progress));
  saveProfile(profile);
  store.data.activeProfileId = profile.id;
  store.save();
  await refreshDiscord();
  return publicState();
});

ipcMain.handle("partners:get", async () => {
  try {
    const url = store.data.settings.partnerServersUrl;
    if (!url) return [];
    const response = await fetch(url);
    if (!response.ok) return [];
    const data = await response.json();
    return Array.isArray(data.servers) ? data.servers : [];
  } catch {
    return [];
  }
});

ipcMain.handle("updates:check", async () => {
  try {
    const url = store.data.settings.updateFeedUrl;
    if (!url) return { available: false, currentVersion: app.getVersion() };
    const response = await fetch(url, { cache: "no-store" });
    if (!response.ok) return { available: false, currentVersion: app.getVersion() };
    const manifest = await response.json();
    const currentVersion = app.getVersion();
    const latestVersion = String(manifest.latestVersion || "").trim();
    const key = updateKey(manifest);
    const newer = manifest.enabled && latestVersion && compareVersions(latestVersion, currentVersion) > 0;
    const sameVersionNewBuild = manifest.enabled && latestVersion && compareVersions(latestVersion, currentVersion) >= 0 && Boolean(manifest.publishedAt || manifest.file?.updatedAt);
    const skipped = !manifest.required && (store.data.settings.skippedUpdateKey ? store.data.settings.skippedUpdateKey === key : store.data.settings.skippedUpdateVersion === latestVersion);
    const applied = store.data.settings.appliedUpdateKey === key;
    return {
      available: Boolean((newer || sameVersionNewBuild) && !skipped && !applied && manifest.downloadUrl),
      currentVersion,
      latestVersion,
      updateKey: key,
      title: manifest.title || "Valaris Client update",
      notes: manifest.notes || "",
      required: Boolean(manifest.required),
      downloadUrl: manifest.downloadUrl || "",
      file: manifest.file || null,
      publishedAt: manifest.publishedAt || null
    };
  } catch {
    return { available: false, currentVersion: app.getVersion() };
  }
});

ipcMain.handle("updates:skip", (_, update) => {
  store.update({
    settings: {
      ...store.data.settings,
      skippedUpdateVersion: String(update?.version || ""),
      skippedUpdateKey: String(update?.updateKey || "")
    }
  });
  return true;
});

ipcMain.handle("updates:download-run", async (_, update) => {
  const target = await downloadUpdateFile(update?.downloadUrl, update?.filename);
  const result = await shell.openPath(target);
  if (result) throw new Error(result);
  if (update?.updateKey) {
    store.update({ settings: { ...store.data.settings, appliedUpdateKey: String(update.updateKey), skippedUpdateKey: "" } });
  }
  setTimeout(() => app.quit(), 1200);
  return { path: target };
});

ipcMain.handle("minecraft:install", async (_, profileId) => {
  const profile = store.data.profiles.find((item) => item.id === profileId);
  if (!profile) throw new Error("Choose a profile first.");
  return minecraft.installProfile(profile, store.data.settings, minecraftRoot(), (progress) => send("minecraft:progress", progress));
});

ipcMain.handle("minecraft:is-installed", (_, profileId) => {
  const profile = store.data.profiles.find((item) => item.id === profileId);
  if (!profile) return false;
  return minecraft.isProfileInstalled(profile, minecraftRoot());
});

ipcMain.handle("minecraft:launch", async (_, profileId, serverAddress) => {
  const profile = store.data.profiles.find((item) => item.id === profileId);
  const account = store.data.accounts.find((item) => item.id === store.data.activeAccountId);
  if (!profile) throw new Error("Choose a profile first.");
  if (!account) throw new Error("Add and select an account first.");
  const result = await minecraft.launchGame({
    profile: serverAddress ? { ...profile, serverAddress } : profile,
    account,
    settings: store.data.settings,
    root: minecraftRoot(),
    emit: (progress) => send("minecraft:progress", progress)
  });
  if (rpc) {
    rpc.setActivity(stellarActivity(profile, true)).catch(() => {});
  }
  if (!store.data.settings.keepLauncherOpen) window.hide();
  return result;
});

ipcMain.handle("cache:clear", async () => {
  const cache = path.join(minecraftRoot(), "assets");
  if (fs.existsSync(cache)) await fs.promises.rm(cache, { recursive: true, force: true });
  return true;
});
