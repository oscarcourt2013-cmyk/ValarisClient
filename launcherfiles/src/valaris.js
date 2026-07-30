const fs = require("fs");
const fsp = fs.promises;
const path = require("path");
const crypto = require("crypto");
const minecraft = require("./minecraft");

const USER_AGENT = "ValarisLauncher/1.0.0";
const MODRINTH_API = "https://api.modrinth.com/v2";
const FABRIC_META = "https://meta.fabricmc.net/v2";

// The client mod targets one Minecraft version at a time on purpose; see HANDOFF.md.
// Adding a row here is all that is needed once another mc-* module ships a jar.
const SUPPORTED = [{ minecraftVersion: "1.21.11", clientVersion: "1.0.0" }];

async function fetchJson(url) {
  const response = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!response.ok) throw new Error(`Request failed (${response.status}).`);
  return response.json();
}

function jarName(entry) {
  return `valaris-client-${entry.minecraftVersion}-${entry.clientVersion}.jar`;
}

// Packaged builds carry the jar in resources/; a dev checkout falls back to the
// Gradle output. TODO: prefer the GitHub release asset once v1.0.0 is tagged.
function candidateJarPaths(entry) {
  const name = jarName(entry);
  const paths = [];
  if (process.resourcesPath) paths.push(path.join(process.resourcesPath, "valaris", name));
  paths.push(path.join(__dirname, "..", "resources", name));
  paths.push(path.join(__dirname, "..", "..", `mc-${entry.minecraftVersion}`, "build", "libs", name));
  return paths;
}

function locateClientJar(entry) {
  return candidateJarPaths(entry).find((item) => fs.existsSync(item)) || null;
}

function supportedVersions() {
  return SUPPORTED.map((entry) => ({
    minecraftVersion: entry.minecraftVersion,
    clientVersion: entry.clientVersion,
    available: Boolean(locateClientJar(entry))
  }));
}

async function latestLoaderVersion(minecraftVersion) {
  const list = await fetchJson(`${FABRIC_META}/versions/loader/${encodeURIComponent(minecraftVersion)}`);
  const chosen = list.find((item) => item.loader?.stable) || list[0];
  if (!chosen?.loader?.version) throw new Error(`Fabric does not support Minecraft ${minecraftVersion}.`);
  return chosen.loader.version;
}

// The client needs Fabric API alongside it, so pull the matching build.
async function installFabricApi(minecraftVersion, modsDir, emit) {
  const query = new URLSearchParams({
    game_versions: JSON.stringify([minecraftVersion]),
    loaders: JSON.stringify(["fabric"])
  });
  const versions = await fetchJson(`${MODRINTH_API}/project/fabric-api/version?${query.toString()}`);
  const version = versions[0];
  const file = version?.files?.find((item) => item.primary) || version?.files?.[0];
  if (!file) throw new Error(`No Fabric API build exists for Minecraft ${minecraftVersion}.`);
  emit({ stage: "modpack", message: `Downloading Fabric API ${version.version_number}`, percent: 62 });
  await minecraft.download(file.url, path.join(modsDir, file.filename), file.hashes?.sha1);
  return file.filename;
}

async function removeMatching(dir, pattern) {
  const entries = await fsp.readdir(dir).catch(() => []);
  for (const entry of entries) {
    if (pattern.test(entry)) await fsp.rm(path.join(dir, entry), { force: true });
  }
}

async function installProfile(options, root, settings, emit = () => {}) {
  const minecraftVersion = String(options?.minecraftVersion || SUPPORTED[0].minecraftVersion);
  const entry = SUPPORTED.find((item) => item.minecraftVersion === minecraftVersion);
  if (!entry) throw new Error(`The Valaris client does not support Minecraft ${minecraftVersion}.`);
  const jar = locateClientJar(entry);
  if (!jar) {
    throw new Error(`Could not find ${jarName(entry)}. Build it with "gradlew :mc-${entry.minecraftVersion}:build".`);
  }

  emit({ stage: "loader", message: "Resolving the Fabric loader", percent: 5 });
  const loaderVersion = await latestLoaderVersion(minecraftVersion);
  const versionId = await minecraft.installFabricLoader(minecraftVersion, loaderVersion, root, emit);

  const instanceDir = path.join(root, "instances", `valaris-${minecraftVersion}`);
  const modsDir = path.join(instanceDir, "mods");
  await fsp.mkdir(modsDir, { recursive: true });

  // Drop older builds first so two client jars never load at once.
  await removeMatching(modsDir, /^valaris-client-.*\.jar$/i);
  await removeMatching(modsDir, /^fabric-api-.*\.jar$/i);

  emit({ stage: "modpack", message: `Installing the Valaris client ${entry.clientVersion}`, percent: 40 });
  await fsp.copyFile(jar, path.join(modsDir, jarName(entry)));

  await installFabricApi(minecraftVersion, modsDir, emit);

  const metadata = {
    clientVersion: entry.clientVersion,
    minecraftVersion,
    loaderVersion,
    installedAt: new Date().toISOString()
  };
  await fsp.writeFile(path.join(instanceDir, "valaris-client.json"), JSON.stringify(metadata, null, 2), "utf8");

  emit({ stage: "ready", message: `Valaris client ${entry.clientVersion} is ready`, percent: 100 });
  return {
    id: crypto.randomUUID(),
    name: `Valaris ${minecraftVersion}`,
    version: versionId,
    baseVersion: minecraftVersion,
    loader: "fabric",
    loaderVersion,
    type: "valaris",
    memory: Math.max(1024, Number(settings.memory || 4096)),
    gameDirectory: instanceDir,
    createdAt: new Date().toISOString(),
    valaris: metadata
  };
}

module.exports = { supportedVersions, installProfile };
