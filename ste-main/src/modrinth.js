const fs = require("fs");
const fsp = fs.promises;
const path = require("path");
const crypto = require("crypto");
const AdmZip = require("adm-zip");
const minecraft = require("./minecraft");

const API = "https://api.modrinth.com/v2";
const USER_AGENT = "ValarisLauncher/1.0.0";
const ALLOWED_DOWNLOAD_HOSTS = new Set([
  "cdn.modrinth.com",
  "github.com",
  "raw.githubusercontent.com",
  "gitlab.com"
]);
const searchCache = new Map();
const versionsCache = new Map();

async function fetchJson(url) {
  const response = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!response.ok) throw new Error(`Modrinth request failed (${response.status}).`);
  return response.json();
}

function safeName(value) {
  return String(value || "modpack").toLowerCase().replace(/[^a-z0-9._-]+/g, "-").replace(/^-+|-+$/g, "").slice(0, 64) || "modpack";
}

function safeJoin(base, relative) {
  const clean = String(relative || "").replace(/\\/g, "/").replace(/^\/+/, "");
  const target = path.normalize(path.join(base, clean));
  const root = path.normalize(base + path.sep);
  if (!target.startsWith(root)) throw new Error("The modpack contains an unsafe file path.");
  return target;
}

function fileHash(buffer, algorithm) {
  return crypto.createHash(algorithm).update(buffer).digest("hex");
}

async function downloadBuffer(url) {
  const parsed = new URL(url);
  if (parsed.protocol !== "https:" || !ALLOWED_DOWNLOAD_HOSTS.has(parsed.hostname)) {
    throw new Error(`Blocked an unsafe modpack download host: ${parsed.hostname}`);
  }
  const response = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!response.ok) throw new Error(`Could not download a modpack file (${response.status}).`);
  return Buffer.from(await response.arrayBuffer());
}

async function downloadPackFile(file, baseDir) {
  const destination = safeJoin(baseDir, file.path);
  if (fs.existsSync(destination) && file.fileSize) {
    const stat = await fsp.stat(destination);
    if (stat.size === file.fileSize) return false;
  }
  const urls = Array.isArray(file.downloads) ? file.downloads : [];
  let lastError;
  for (const url of urls) {
    try {
      const data = await downloadBuffer(url);
      if (file.hashes?.sha512 && fileHash(data, "sha512") !== file.hashes.sha512) {
        throw new Error("SHA-512 check failed.");
      }
      if (!file.hashes?.sha512 && file.hashes?.sha1 && fileHash(data, "sha1") !== file.hashes.sha1) {
        throw new Error("SHA-1 check failed.");
      }
      await fsp.mkdir(path.dirname(destination), { recursive: true });
      await fsp.writeFile(destination, data);
      return true;
    } catch (error) {
      lastError = error;
    }
  }
  throw new Error(`Could not download ${file.path}: ${lastError?.message || "no valid download URL"}`);
}

function extractOverrides(zip, folderName, instanceDir) {
  const prefix = `${folderName}/`;
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory || !entry.entryName.startsWith(prefix)) continue;
    const relative = entry.entryName.slice(prefix.length);
    if (!relative) continue;
    const destination = safeJoin(instanceDir, relative);
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.writeFileSync(destination, entry.getData());
  }
}

async function searchModpacks(query = "") {
  const cacheKey = query.trim().toLowerCase();
  const cached = searchCache.get(cacheKey);
  if (cached && Date.now() - cached.time < 5 * 60 * 1000) return cached.value;
  const params = new URLSearchParams({
    facets: JSON.stringify([["project_type:modpack"], ["categories:fabric"]]),
    index: "downloads",
    limit: "12"
  });
  if (query.trim()) params.set("query", query.trim());
  const data = await fetchJson(`${API}/search?${params.toString()}`);
  const value = {
    totalHits: data.total_hits || 0,
    hits: (data.hits || []).map((hit) => ({
      projectId: hit.project_id,
      slug: hit.slug,
      title: hit.title,
      author: hit.author,
      description: hit.description,
      iconUrl: hit.icon_url,
      downloads: hit.downloads || 0,
      follows: hit.follows || 0,
      categories: hit.categories || [],
      versions: hit.versions || [],
      latestVersion: hit.latest_version
    }))
  };
  searchCache.set(cacheKey, { time: Date.now(), value });
  return value;
}

function pickMrpackVersion(versions) {
  for (const version of versions) {
    const file = (version.files || []).find((item) => item.primary && item.filename?.endsWith(".mrpack"))
      || (version.files || []).find((item) => item.filename?.endsWith(".mrpack"));
    if (file) return { version, file };
  }
  throw new Error("This Modrinth pack does not have an .mrpack download.");
}

function mrpackFromVersion(version) {
  return (version.files || []).find((item) => item.primary && item.filename?.endsWith(".mrpack"))
    || (version.files || []).find((item) => item.filename?.endsWith(".mrpack"));
}

async function getModpackVersions(projectId) {
  if (!projectId) throw new Error("Choose a Modrinth pack first.");
  const cached = versionsCache.get(projectId);
  if (cached && Date.now() - cached.time < 10 * 60 * 1000) return cached.value;
  const versions = await fetchJson(`${API}/project/${encodeURIComponent(projectId)}/version`);
  const value = versions
    .map((version) => {
      const file = mrpackFromVersion(version);
      const gameVersions = version.game_versions || [];
      const loaders = version.loaders || [];
      if (!file || !gameVersions.length || !loaders.includes("fabric")) return null;
      return {
        id: version.id,
        name: version.name,
        versionNumber: version.version_number,
        versionType: version.version_type,
        datePublished: version.date_published,
        minecraftVersions: gameVersions,
        minecraftVersion: gameVersions[0],
        loader: "fabric",
        fileName: file.filename,
        downloads: version.downloads || 0
      };
    })
    .filter(Boolean);
  versionsCache.set(projectId, { time: Date.now(), value });
  return value;
}

async function installModpack(project, root, settings, emit = () => {}) {
  if (!project?.projectId) throw new Error("Choose a Modrinth pack first.");
  emit({ stage: "modpack", message: `Finding ${project.title || "modpack"} on Modrinth`, percent: 3 });
  const versions = await fetchJson(`${API}/project/${encodeURIComponent(project.projectId)}/version`);
  const chosenVersions = project.versionId ? versions.filter((item) => item.id === project.versionId) : versions;
  const { version, file } = pickMrpackVersion(chosenVersions);

  const cacheDir = path.join(root, "modpacks", "cache");
  await fsp.mkdir(cacheDir, { recursive: true });
  const archivePath = path.join(cacheDir, `${safeName(project.slug || project.projectId)}-${version.id}.mrpack`);
  await minecraft.download(file.url, archivePath, file.hashes?.sha1);

  const zip = new AdmZip(archivePath);
  const indexEntry = zip.getEntry("modrinth.index.json");
  if (!indexEntry) throw new Error("This .mrpack is missing modrinth.index.json.");
  const index = JSON.parse(indexEntry.getData().toString("utf8"));
  const dependencies = index.dependencies || {};
  const minecraftVersion = dependencies.minecraft;
  const fabricLoader = dependencies["fabric-loader"];
  if (!minecraftVersion) throw new Error("This modpack did not list a Minecraft version.");
  if (!fabricLoader) {
    throw new Error("This build currently installs Fabric Modrinth packs. Pick a pack with the Fabric loader.");
  }

  emit({ stage: "loader", message: `Installing Fabric ${fabricLoader}`, percent: 8 });
  const loaderVersionId = await minecraft.installFabricLoader(minecraftVersion, fabricLoader, root, emit);

  const instanceDir = path.join(root, "instances", `modrinth-${safeName(project.slug || project.projectId)}`);
  await fsp.mkdir(instanceDir, { recursive: true });
  const clientFiles = (index.files || []).filter((item) => item.env?.client !== "unsupported");
  let done = 0;
  const queue = [...clientFiles];
  const workers = Array.from({ length: Math.min(16, Math.max(1, queue.length)) }, async () => {
    while (queue.length) {
      const item = queue.shift();
      await downloadPackFile(item, instanceDir);
      done += 1;
      if (done % 5 === 0 || done === clientFiles.length) {
        emit({ stage: "modpack", message: `Installing modpack files (${done}/${clientFiles.length})`, percent: 12 + Math.round((done / Math.max(clientFiles.length, 1)) * 74) });
      }
    }
  });
  await Promise.all(workers);

  extractOverrides(zip, "overrides", instanceDir);
  extractOverrides(zip, "client-overrides", instanceDir);

  const metadata = {
    projectId: project.projectId,
    slug: project.slug,
    title: project.title || index.name,
    modpackVersionId: version.id,
    modpackVersion: version.version_number,
    minecraftVersion,
    loader: "fabric",
    loaderVersion: fabricLoader,
    installedAt: new Date().toISOString()
  };
  await fsp.writeFile(path.join(instanceDir, "valaris-modpack.json"), JSON.stringify(metadata, null, 2), "utf8");

  emit({ stage: "ready", message: `${metadata.title} is ready`, percent: 100 });
  return {
    id: crypto.randomUUID(),
    name: String(index.name || project.title || "Modrinth Pack").slice(0, 40),
    version: loaderVersionId,
    baseVersion: minecraftVersion,
    loader: "fabric",
    loaderVersion: fabricLoader,
    type: "modpack",
    memory: Math.max(1024, Number(settings.memory || 4096)),
    gameDirectory: instanceDir,
    createdAt: new Date().toISOString(),
    modpack: metadata
  };
}

module.exports = { searchModpacks, getModpackVersions, installModpack };
