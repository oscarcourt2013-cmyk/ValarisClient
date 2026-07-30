const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const OLD_DISCORD_CLIENT_ID = "1525574680994648174";
const DEFAULT_DISCORD_CLIENT_ID = "1532351441006759966";

const DEFAULTS = {
  profiles: [],
  accounts: [],
  servers: [],
  activeProfileId: null,
  activeAccountId: null,
  settings: {
    javaPath: "",
    memory: 4096,
    gameDirectory: "",
    keepLauncherOpen: true,
    showSnapshots: false,
    showHistorical: true,
    discordEnabled: true,
    discordClientId: DEFAULT_DISCORD_CLIENT_ID,
    microsoftClientId: "",
    hideCharacter: false,
    theme: "midnight",
    partnerServersUrl: "http://localhost:3000/api/partnered-servers-public",
    updateFeedUrl: "http://localhost:3000/api/update",
    skippedUpdateVersion: "",
    skippedUpdateKey: "",
    appliedUpdateKey: ""
  }
};

class Store {
  constructor(file) {
    this.file = file;
    this.data = this.read();
  }

  read() {
    try {
      const value = JSON.parse(fs.readFileSync(this.file, "utf8"));
      const merged = {
        ...structuredClone(DEFAULTS),
        ...value,
        settings: { ...DEFAULTS.settings, ...(value.settings || {}) }
      };
      if (merged.settings.discordClientId === OLD_DISCORD_CLIENT_ID) {
        merged.settings.discordClientId = DEFAULT_DISCORD_CLIENT_ID;
      }
      return merged;
    } catch {
      return structuredClone(DEFAULTS);
    }
  }

  save() {
    fs.mkdirSync(path.dirname(this.file), { recursive: true });
    const temp = `${this.file}.tmp`;
    fs.writeFileSync(temp, JSON.stringify(this.data, null, 2), "utf8");
    fs.renameSync(temp, this.file);
  }

  get() {
    return this.data;
  }

  update(patch) {
    this.data = { ...this.data, ...patch };
    if (patch.settings) {
      this.data.settings = { ...DEFAULTS.settings, ...this.data.settings, ...patch.settings };
    }
    this.save();
    return this.data;
  }

  addOfflineAccount(username) {
    const clean = String(username || "").trim().replace(/[^A-Za-z0-9_]/g, "").slice(0, 16);
    if (clean.length < 3) throw new Error("Username must be 3–16 letters, numbers, or underscores.");
    const digest = crypto.createHash("md5").update(`OfflinePlayer:${clean}`).digest("hex");
    const uuid = `${digest.slice(0, 8)}-${digest.slice(8, 12)}-3${digest.slice(13, 16)}-a${digest.slice(17, 20)}-${digest.slice(20)}`;
    const account = {
      id: crypto.randomUUID(),
      type: "offline",
      username: clean,
      uuid,
      accessToken: "0",
      addedAt: new Date().toISOString()
    };
    this.data.accounts.push(account);
    this.data.activeAccountId = account.id;
    this.save();
    return account;
  }
}

module.exports = { Store, DEFAULTS };
