const fs = require("fs");
const fsp = fs.promises;
const path = require("path");
const os = require("os");
const crypto = require("crypto");
const { spawn } = require("child_process");
const AdmZip = require("adm-zip");

const MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
const JAVA_RUNTIME_URL = "https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
const OS_NAME = process.platform === "win32" ? "windows" : process.platform === "darwin" ? "osx" : "linux";
let versionManifestCache = null;

async function fetchJson(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Download failed (${response.status}): ${url}`);
  return response.json();
}

async function getVersions(cacheRoot) {
  if (versionManifestCache) return versionManifestCache;
  const cachePath = cacheRoot ? path.join(cacheRoot, "version_manifest_v2.json") : null;
  if (cachePath && fs.existsSync(cachePath)) {
    try {
      const stat = await fsp.stat(cachePath);
      const cached = JSON.parse(await fsp.readFile(cachePath, "utf8"));
      versionManifestCache = { latest: cached.latest, versions: cached.versions };
      if (Date.now() - stat.mtimeMs < 6 * 60 * 60 * 1000) return versionManifestCache;
    } catch {}
  }
  try {
    const manifest = await fetchJson(MANIFEST_URL);
    versionManifestCache = { latest: manifest.latest, versions: manifest.versions };
    if (cachePath) {
      await fsp.mkdir(path.dirname(cachePath), { recursive: true });
      await fsp.writeFile(cachePath, JSON.stringify(manifest), "utf8");
    }
    return versionManifestCache;
  } catch (error) {
    if (versionManifestCache) return versionManifestCache;
    throw error;
  }
}

function rulesAllow(rules = []) {
  if (!rules.length) return true;
  let allowed = false;
  for (const rule of rules) {
    let matches = true;
    if (rule.os?.name && rule.os.name !== OS_NAME) matches = false;
    if (rule.os?.arch) {
      const arch = process.arch === "x64" ? "x86_64" : process.arch;
      if (rule.os.arch !== arch) matches = false;
    }
    if (rule.features) matches = false;
    if (matches) allowed = rule.action === "allow";
  }
  return allowed;
}

function mavenPath(name) {
  const [group, artifact, version, classifier] = name.split(":");
  const file = `${artifact}-${version}${classifier ? `-${classifier}` : ""}.jar`;
  return `${group.replace(/\./g, "/")}/${artifact}/${version}/${file}`;
}

async function download(url, destination, expectedSha1, progress, expectedSize) {
  if (fs.existsSync(destination)) {
    if (expectedSize) {
      const stat = await fsp.stat(destination);
      if (stat.size === expectedSize) return;
    }
    if (!expectedSha1) return;
    const existing = crypto.createHash("sha1").update(await fsp.readFile(destination)).digest("hex");
    if (existing === expectedSha1) return;
  }
  await fsp.mkdir(path.dirname(destination), { recursive: true });
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Could not download ${url} (${response.status})`);
  const data = Buffer.from(await response.arrayBuffer());
  if (expectedSha1) {
    const actual = crypto.createHash("sha1").update(data).digest("hex");
    if (actual !== expectedSha1) throw new Error(`File verification failed for ${path.basename(destination)}`);
  }
  await fsp.writeFile(destination, data);
  progress?.(data.length);
}

function javaPlatformKey() {
  if (process.platform === "win32") {
    if (process.arch === "arm64") return "windows-arm64";
    if (process.arch === "ia32") return "windows-x86";
    return "windows-x64";
  }
  if (process.platform === "darwin") return process.arch === "arm64" ? "mac-os-arm64" : "mac-os";
  if (process.arch === "ia32") return "linux-i386";
  return "linux";
}

function defaultJavaComponent(details) {
  if (details.javaVersion?.component) return details.javaVersion.component;
  return "jre-legacy";
}

function javaBinary(runtimeDir) {
  if (process.platform === "win32") {
    const javaExe = path.join(runtimeDir, "bin", "java.exe");
    const javawExe = path.join(runtimeDir, "bin", "javaw.exe");
    if (fs.existsSync(javaExe)) return javaExe;
    return javawExe;
  }
  if (process.platform === "darwin") {
    const bundleJava = path.join(runtimeDir, "jre.bundle", "Contents", "Home", "bin", "java");
    if (fs.existsSync(bundleJava)) return bundleJava;
  }
  return path.join(runtimeDir, "bin", "java");
}

async function installJavaRuntime(component, root, emit = () => {}) {
  const platform = javaPlatformKey();
  const runtimeDir = path.join(root, "runtime", platform, component);
  const binary = javaBinary(runtimeDir);
  if (fs.existsSync(binary)) return binary;

  emit({ stage: "java", message: `Installing ${component}`, percent: 4 });
  const manifest = await fetchJson(JAVA_RUNTIME_URL);
  const runtime = manifest[platform]?.[component]?.[0];
  if (!runtime?.manifest?.url) {
    throw new Error(`Valaris could not find a Mojang Java runtime for ${platform}/${component}. Choose Java manually in Settings.`);
  }

  const manifestPath = path.join(root, "runtime", platform, `${component}.json`);
  await download(runtime.manifest.url, manifestPath, runtime.manifest.sha1);
  const runtimeManifest = JSON.parse(await fsp.readFile(manifestPath, "utf8"));
  const files = Object.entries(runtimeManifest.files || {});
  let done = 0;

  for (const [relativeName, file] of files) {
    const target = path.normalize(path.join(runtimeDir, relativeName));
    if (!target.startsWith(path.normalize(runtimeDir))) throw new Error("Runtime manifest contained an unsafe path.");
    if (file.type === "directory") {
      await fsp.mkdir(target, { recursive: true });
    } else if (file.type === "file" && file.downloads?.raw?.url) {
      await download(file.downloads.raw.url, target, file.downloads.raw.sha1);
      if (file.executable && process.platform !== "win32") await fsp.chmod(target, 0o755).catch(() => {});
    }
    done += 1;
    if (done % 20 === 0 || done === files.length) {
      emit({ stage: "java", message: `Installing Java runtime (${done}/${files.length})`, percent: 4 + Math.round((done / Math.max(files.length, 1)) * 8) });
    }
  }

  if (!fs.existsSync(javaBinary(runtimeDir))) {
    throw new Error("Java runtime installed, but Valaris could not find the Java executable.");
  }
  return javaBinary(runtimeDir);
}

async function resolveDetails(versionId, root) {
  const localJsonPath = path.join(root, "versions", versionId, `${versionId}.json`);
  if (fs.existsSync(localJsonPath)) {
    const local = JSON.parse(await fsp.readFile(localJsonPath, "utf8"));
    if (!local.inheritsFrom) return local;
    const parent = await resolveDetails(local.inheritsFrom, root);
    return {
      ...parent,
      ...local,
      libraries: [...(parent.libraries || []), ...(local.libraries || [])],
      arguments: {
        game: [...(parent.arguments?.game || []), ...(local.arguments?.game || [])],
        jvm: [...(parent.arguments?.jvm || []), ...(local.arguments?.jvm || [])]
      }
    };
  }
  const manifest = await getVersions();
  const entry = manifest.versions.find((version) => version.id === versionId);
  if (!entry) throw new Error(`Minecraft ${versionId} was not found.`);
  const jsonPath = path.join(root, "versions", versionId, `${versionId}.json`);
  await download(entry.url, jsonPath, entry.sha1);
  const details = JSON.parse(await fsp.readFile(jsonPath, "utf8"));
  if (!details.inheritsFrom) return details;
  const parent = await resolveDetails(details.inheritsFrom, root);
  return {
    ...parent,
    ...details,
    libraries: [...(parent.libraries || []), ...(details.libraries || [])],
    arguments: {
      game: [...(parent.arguments?.game || []), ...(details.arguments?.game || [])],
      jvm: [...(parent.arguments?.jvm || []), ...(details.arguments?.jvm || [])]
    }
  };
}

async function installFabricLoader(minecraftVersion, loaderVersion, root, emit = () => {}) {
  emit({ stage: "loader", message: `Preparing Fabric ${loaderVersion}`, percent: 4 });
  const url = `https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(minecraftVersion)}/${encodeURIComponent(loaderVersion)}/profile/json`;
  const details = await fetchJson(url);
  const id = details.id || `fabric-loader-${loaderVersion}-${minecraftVersion}`;
  details.id = id;
  const versionDir = path.join(root, "versions", id);
  await fsp.mkdir(versionDir, { recursive: true });
  await fsp.writeFile(path.join(versionDir, `${id}.json`), JSON.stringify(details, null, 2), "utf8");
  return id;
}

function nativeClassifier(library) {
  const value = library.natives?.[OS_NAME];
  if (!value) return null;
  return value.replace("${arch}", process.arch === "x64" ? "64" : "32");
}

async function installVersion(versionId, root, emit = () => {}) {
  emit({ stage: "metadata", message: `Preparing Minecraft ${versionId}`, percent: 2 });
  const details = await resolveDetails(versionId, root);
  const client = details.downloads?.client;
  if (!client) throw new Error("This version does not contain a client download.");
  const versionDir = path.join(root, "versions", versionId);
  await download(client.url, path.join(versionDir, `${versionId}.jar`), client.sha1);
  emit({ stage: "libraries", message: "Downloading libraries", percent: 12 });

  const libraries = [];
  const natives = [];
  const eligible = (details.libraries || []).filter((library) => rulesAllow(library.rules));
  let completed = 0;
  for (const library of eligible) {
    const artifact = library.downloads?.artifact;
    if (artifact) {
      const target = path.join(root, "libraries", artifact.path);
      await download(artifact.url, target, artifact.sha1);
      libraries.push(target);
    } else if (library.name && library.url) {
      const rel = mavenPath(library.name);
      const target = path.join(root, "libraries", rel);
      await download(`${library.url.replace(/\/$/, "")}/${rel}`, target);
      libraries.push(target);
    }
    const classifier = nativeClassifier(library);
    const native = classifier && library.downloads?.classifiers?.[classifier];
    if (native) {
      const target = path.join(root, "libraries", native.path);
      await download(native.url, target, native.sha1);
      natives.push({ file: target, excludes: library.extract?.exclude || [] });
    }
    completed += 1;
    emit({ stage: "libraries", message: "Downloading libraries", percent: 12 + Math.round((completed / Math.max(eligible.length, 1)) * 35) });
  }

  if (details.assetIndex?.url) {
    emit({ stage: "assets", message: "Downloading game assets", percent: 50 });
    const index = details.assetIndex;
    const indexPath = path.join(root, "assets", "indexes", `${index.id}.json`);
    await download(index.url, indexPath, index.sha1);
    const assetIndex = JSON.parse(await fsp.readFile(indexPath, "utf8"));
    const assets = Object.values(assetIndex.objects || {});
    let assetDone = 0;
    const queue = [...assets];
    const workers = Array.from({ length: 48 }, async () => {
      while (queue.length) {
        const asset = queue.shift();
        const prefix = asset.hash.slice(0, 2);
        await download(
          `https://resources.download.minecraft.net/${prefix}/${asset.hash}`,
          path.join(root, "assets", "objects", prefix, asset.hash),
          asset.hash,
          undefined,
          asset.size
        );
        assetDone += 1;
        if (assetDone % 100 === 0 || assetDone === assets.length) {
          emit({ stage: "assets", message: `Downloading assets (${assetDone}/${assets.length})`, percent: 50 + Math.round((assetDone / Math.max(assets.length, 1)) * 45) });
        }
      }
    });
    await Promise.all(workers);
    if (assetIndex.virtual || assetIndex.map_to_resources) {
      emit({ stage: "assets", message: "Preparing legacy assets", percent: 96 });
      for (const [name, asset] of Object.entries(assetIndex.objects || {})) {
        const source = path.join(root, "assets", "objects", asset.hash.slice(0, 2), asset.hash);
        if (assetIndex.virtual) {
          const destination = path.join(root, "assets", "virtual", index.id, name);
          await fsp.mkdir(path.dirname(destination), { recursive: true });
          await fsp.copyFile(source, destination);
        }
        if (assetIndex.map_to_resources) {
          const destination = path.join(root, "resources", name);
          await fsp.mkdir(path.dirname(destination), { recursive: true });
          await fsp.copyFile(source, destination);
        }
      }
    }
  } else {
    emit({ stage: "assets", message: "This old version has no asset index", percent: 95 });
  }

  const nativesDir = path.join(versionDir, "natives");
  await fsp.mkdir(nativesDir, { recursive: true });
  for (const native of natives) {
    const zip = new AdmZip(native.file);
    for (const entry of zip.getEntries()) {
      if (entry.isDirectory || entry.entryName.startsWith("META-INF/")) continue;
      if (native.excludes.some((item) => entry.entryName.startsWith(item))) continue;
      const destination = path.join(nativesDir, path.basename(entry.entryName));
      await fsp.writeFile(destination, entry.getData());
    }
  }
  emit({ stage: "ready", message: "Ready to play", percent: 100 });
  return { details, libraries, nativesDir };
}

async function installProfile(profile, settings, root, emit = () => {}) {
  if (profile.loader === "fabric") {
    const versionId = await installFabricLoader(profile.baseVersion || profile.minecraftVersion || profile.version, profile.loaderVersion, root, emit);
    profile = { ...profile, version: versionId };
  }
  const installed = await installVersion(profile.version, root, emit);
  if (!settings.javaPath) {
    await installJavaRuntime(defaultJavaComponent(installed.details), root, emit);
  }
  emit({ stage: "ready", message: "Ready to play", percent: 100 });
  return installed;
}

function isProfileInstalled(profile, root) {
  if (!profile) return false;
  const versionId = profile.version;
  if (!versionId) return false;
  const versionDir = path.join(root, "versions", versionId);
  const versionJson = path.join(versionDir, `${versionId}.json`);
  const versionJar = path.join(versionDir, `${versionId}.jar`);
  if (!fs.existsSync(versionJson) || !fs.existsSync(versionJar)) return false;
  if (profile.modpack?.projectId || profile.type === "modpack") {
    const gameDir = profile.gameDirectory || path.join(root, "instances", profile.id);
    return fs.existsSync(path.join(gameDir, "valaris-modpack.json"));
  }
  return true;
}

function flattenArguments(items = []) {
  const result = [];
  for (const item of items) {
    if (typeof item === "string") result.push(item);
    else if (item && rulesAllow(item.rules)) result.push(...(Array.isArray(item.value) ? item.value : [item.value]));
  }
  return result;
}

function expand(value, vars) {
  return String(value).replace(/\$\{([^}]+)\}/g, (_, key) => vars[key] ?? "");
}

async function launchGame({ profile, account, settings, root, emit = () => {} }) {
  const installed = await installProfile(profile, settings, root, emit);
  const { details, libraries, nativesDir } = installed;
  const versionJar = path.join(root, "versions", profile.version, `${profile.version}.jar`);
  const classpath = [...libraries, versionJar].join(path.delimiter);
  const gameDir = profile.gameDirectory || path.join(root, "instances", profile.id);
  await fsp.mkdir(gameDir, { recursive: true });
  const vars = {
    natives_directory: nativesDir,
    launcher_name: "Valaris",
    launcher_version: "1.0.0",
    classpath,
    classpath_separator: path.delimiter,
    library_directory: path.join(root, "libraries"),
    auth_player_name: account.username,
    version_name: profile.version,
    game_directory: gameDir,
    assets_root: path.join(root, "assets"),
    game_assets: details.assetIndex?.virtual
      ? path.join(root, "assets", "virtual", details.assetIndex.id)
      : details.assetIndex?.map_to_resources ? path.join(root, "resources") : path.join(root, "assets"),
    assets_index_name: details.assetIndex?.id || details.assets || "legacy",
    auth_uuid: account.uuid.replace(/-/g, ""),
    auth_access_token: account.accessToken || "0",
    user_type: account.type === "microsoft" ? "msa" : "legacy",
    version_type: details.type || "release",
    user_properties: "{}",
    clientid: "",
    auth_xuid: ""
  };

  let gameArgs;
  if (details.arguments?.game) gameArgs = flattenArguments(details.arguments.game);
  else gameArgs = String(details.minecraftArguments || "").match(/(?:[^\s"]+|"[^"]*")+/g) || [];
  if (profile.serverAddress) {
    const [host, port] = String(profile.serverAddress).split(":");
    gameArgs.push("--server", host);
    if (port) gameArgs.push("--port", port);
  }
  let jvmArgs = details.arguments?.jvm ? flattenArguments(details.arguments.jvm) : [
    "-Djava.library.path=${natives_directory}",
    "-cp",
    "${classpath}"
  ];
  jvmArgs = jvmArgs.filter((arg) => !String(arg).startsWith("-Xmx") && !String(arg).startsWith("-Xms"));
  if (details.logging?.client?.file) {
    const logFile = path.join(root, "assets", "log_configs", details.logging.client.file.id);
    await download(details.logging.client.file.url, logFile, details.logging.client.file.sha1);
    jvmArgs.push(expand(details.logging.client.argument, { path: logFile }));
  }
  jvmArgs.unshift(`-Xmx${Math.max(1024, Number(profile.memory || settings.memory || 4096))}M`, "-Xms512M");
  const args = [...jvmArgs, details.mainClass, ...gameArgs].map((arg) => expand(arg, vars));
  const java = settings.javaPath || await installJavaRuntime(defaultJavaComponent(details), root, emit);
  emit({ stage: "launching", message: `Starting Minecraft ${profile.version}`, percent: 100 });
  const child = spawn(java, args, { cwd: gameDir, detached: false, windowsHide: true });
  child.stdout?.on("data", (data) => emit({ stage: "console", message: data.toString() }));
  child.stderr?.on("data", (data) => emit({ stage: "console", message: data.toString() }));
  child.on("error", (error) => emit({ stage: "error", message: error.message }));
  child.on("close", (code) => {
    emit({ stage: "console", message: `Minecraft exited with code ${code ?? "unknown"}\n` });
    emit({ stage: "closed", message: "Minecraft closed", code: code ?? 0, percent: 100 });
    if (code && code !== 0) emit({ stage: "error", message: `Minecraft closed with exit code ${code}. Check the console for details.` });
  });
  return new Promise((resolve) => {
    child.on("spawn", () => resolve({ pid: child.pid }));
  });
}

module.exports = { getVersions, installVersion, installProfile, launchGame, installFabricLoader, download, isProfileInstalled };
