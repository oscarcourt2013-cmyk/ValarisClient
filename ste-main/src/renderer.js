const api = window.valaris;
let state;
let versions;
let currentPage = "home";
let settingsTab = "general";
let progress = { percent: 0, message: "" };
let consoleLines = [];
let versionSearch = "";
let versionFilter = "all";
let modpacks = null;
let modpackSearch = "";
let modpackLoading = false;
let installedProfiles = {};
let partneredServers = [];
let partnersLoading = false;
let partnersLoaded = false;
let runningGame = null;
let runtimeTicker = null;
const pageOrder = ["home", "profiles", "library", "modpacks", "accounts", "servers", "settings"];
let skinViewer = null;

const themes = [
  ["crimson", "Crimson", "#ef233c", "#25030b", "#ff7a8d"],
  ["midnight", "Midnight", "#38bdf8", "#07111f", "#9be7ff"],
  ["aurora", "Aurora", "#34d399", "#03180f", "#9ff5ce"],
  ["obsidian", "Obsidian", "#f5d77b", "#151104", "#fff3b0"],
  ["ember", "Ember", "#fb923c", "#1b0902", "#ffc08a"]
];

const iconPaths = {
  home: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/><path d="M9 20v-6h6v6"/>',
  folder: '<path d="M3 6h6l2 2h10v11H3z"/>',
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
  server: '<rect x="3" y="4" width="18" height="6" rx="2"/><rect x="3" y="14" width="18" height="6" rx="2"/><path d="M7 7h.01M7 17h.01"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V21h-4v-.09A1.7 1.7 0 0 0 9 19.37a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.63 15 1.7 1.7 0 0 0 3.07 14H3v-4h.09A1.7 1.7 0 0 0 4.63 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.63 1.7 1.7 0 0 0 10 3.07V3h4v.09A1.7 1.7 0 0 0 15 4.63a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.37 9 1.7 1.7 0 0 0 20.93 10H21v4h-.09A1.7 1.7 0 0 0 19.4 15z"/>',
  discord: '<path d="M8.7 7.3a10 10 0 0 1 6.6 0M7.3 17c3 2 6.4 2 9.4 0"/><path d="M6.5 5.5C3.8 9.5 3 13 3 16.5c2 1.5 4 2.5 6 3l1-1.8M17.5 5.5c2.7 4 3.5 7.5 3.5 11-2 1.5-4 2.5-6 3l-1-1.8"/><circle cx="8.5" cy="13" r="1"/><circle cx="15.5" cy="13" r="1"/>',
  chevron: '<path d="m9 18 6-6-6-6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  play: '<path d="m8 5 11 7-11 7z"/>',
  download: '<path d="M12 3v12m0 0 4-4m-4 4-4-4"/><path d="M5 21h14"/>',
  edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4z"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v6M14 11v6"/>',
  java: '<path d="M8 18c-4 1-4 3 0 3h8c4 0 4-2 0-3"/><path d="M8 14h8c0 3-1 4-4 4s-4-1-4-4zM10 3c4 2-3 3 2 6M14 2c4 3-3 4 1 7"/>',
  memory: '<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M9 9h6v6H9zM9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 14h3M1 9h3M1 14h3"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/>',
  harddrive: '<path d="M4 6h16l2 8H2z"/><path d="M2 14v5h20v-5M17 17h.01"/>',
  shield: '<path d="M12 3 4 6v6c0 5 3.5 8 8 10 4.5-2 8-5 8-10V6z"/><path d="m9 12 2 2 4-5"/>',
  copy: '<rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>',
  terminal: '<path d="m4 7 5 5-5 5M11 17h9"/>',
  refresh: '<path d="M20 7v5h-5M4 17v-5h5"/><path d="M6.1 9a7 7 0 0 1 11.4-2L20 12M4 12l2.5 5a7 7 0 0 0 11.4-2"/>'
  ,package: '<path d="m21 8-9-5-9 5 9 5 9-5z"/><path d="M3 8v8l9 5 9-5V8"/><path d="M12 13v8"/><path d="m7.5 5.5 9 5"/>'
};

function icon(name) {
  return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${iconPaths[name] || iconPaths.chevron}</svg>`;
}

function injectIcons(root = document) {
  root.querySelectorAll("[data-icon]").forEach((node) => { node.innerHTML = icon(node.dataset.icon); });
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" })[char]);
}

function toast(message, type = "") {
  const node = document.createElement("div");
  node.className = `toast ${type}`;
  node.textContent = message;
  document.querySelector("#toastRoot").appendChild(node);
  setTimeout(() => node.remove(), 4200);
}

function applyTheme() {
  document.documentElement.dataset.theme = state?.settings?.theme || "midnight";
}

function activeProfile() {
  return state.profiles.find((profile) => profile.id === state.activeProfileId);
}

function activeAccount() {
  return state.accounts.find((account) => account.id === state.activeAccountId);
}

function profileVersion(profile) {
  return profile?.baseVersion || profile?.version || "";
}

function profileLoader(profile) {
  if (!profile) return "Vanilla";
  if (profile.modpack) return "Modpack";
  if (profile.loader) return profile.loader.charAt(0).toUpperCase() + profile.loader.slice(1);
  return "Vanilla";
}

function avatar(account, small = false) {
  if (!account) return `<span class="avatar${small ? " small" : ""}">V</span>`;
  const src = `https://mc-heads.net/avatar/${encodeURIComponent(account.username)}/64`;
  return `<span class="avatar${small ? " small" : ""}"><img src="${src}" alt=""></span>`;
}

function skinBody(account) {
  const name = account?.username || "MHF_Steve";
  const skin = `https://mc-heads.net/skin/${encodeURIComponent(name)}`;
  return `<div class="skin-stage" data-skin-name="${escapeHtml(name)}" data-skin-url="${escapeHtml(skin)}"><canvas id="skinCanvas" width="198" height="350" aria-label="${escapeHtml(name)} 3D skin"></canvas></div>`;
}

function updateTopAccount() {
  const account = activeAccount();
  document.querySelector("#topUsername").textContent = account?.username || "No account";
  const avatarNode = document.querySelector("#topAvatar");
  if (account) avatarNode.innerHTML = `<img src="https://mc-heads.net/avatar/${encodeURIComponent(account.username)}/64" alt="">`;
  else avatarNode.textContent = "V";
}

function launchLabel(profile, account) {
  if (!profile && !account) return "SET UP";
  if (!profile) return "CREATE PROFILE";
  if (!account) return "ADD ACCOUNT";
  return "PLAY";
}

function homeAction(profile, account) {
  if (runningGame) return { action: "noop", label: `RUNNING ${formatRuntime()}`, iconName: "play", disabled: true };
  if (!profile) return { action: "new-profile", label: "CREATE PROFILE", iconName: "plus" };
  if (installedProfiles[profile.id]) return { action: "launch", label: account ? "PLAY" : "ADD ACCOUNT", iconName: account ? "play" : "users" };
  return { action: "install", label: "INSTALL", iconName: "download" };
}

function formatRuntime() {
  if (!runningGame?.startedAt) return "00:00";
  const total = Math.max(0, Math.floor((Date.now() - runningGame.startedAt) / 1000));
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  return hours ? `${hours}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}` : `${minutes}:${String(seconds).padStart(2, "0")}`;
}

function formatBytes(bytes) {
  if (!Number(bytes)) return "";
  const units = ["B", "KB", "MB", "GB"];
  let value = Number(bytes);
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${value.toFixed(value >= 10 || unit === 0 ? 0 : 1)} ${units[unit]}`;
}

function updateRuntimeUI() {
  if (!runningGame || currentPage !== "home") return;
  const button = document.querySelector(".launch-button");
  const sub = document.querySelector(".launch-sub");
  const runtime = formatRuntime();
  if (button) button.innerHTML = `${icon("play")} RUNNING ${runtime}`;
  if (sub) sub.textContent = `Minecraft has been running for ${runtime}`;
  injectIcons(button || document);
}

function startRuntimeTicker() {
  clearInterval(runtimeTicker);
  runtimeTicker = setInterval(updateRuntimeUI, 1000);
}

function stopRuntimeTicker() {
  clearInterval(runtimeTicker);
  runtimeTicker = null;
}

function setupHint(profile, account) {
  if (!profile && !account) return "Add a local or Microsoft account, then choose a Minecraft version.";
  if (!profile) return "Create a profile from the version library to begin.";
  if (!account) return "Add an account. Local accounts work for offline testing.";
  return `Minecraft ${profileVersion(profile)} - ${profile.name}`;
}

function updateNavLiquid() {
  const liquid = document.querySelector("#navLiquid");
  const active = document.querySelector(`.nav-item[data-page="${currentPage}"]`);
  const box = document.querySelector(".nav-links");
  if (!liquid || !active || !box) return;
  liquid.style.width = `${active.offsetWidth}px`;
  liquid.style.transform = `translateX(${active.offsetLeft}px)`;
}

function disposeSkinViewer() {
  if (skinViewer) {
    skinViewer.dispose();
    skinViewer = null;
  }
}

function setupSkinViewer() {
  const stage = document.querySelector(".skin-stage");
  const canvas = document.querySelector("#skinCanvas");
  if (!stage || !canvas || !window.skinview3d?.SkinViewer) {
    disposeSkinViewer();
    return;
  }
  disposeSkinViewer();
  const rect = stage.getBoundingClientRect();
  const width = Math.max(180, Math.round(rect.width));
  const height = Math.max(320, Math.round(rect.height));
  canvas.width = width;
  canvas.height = height;
  skinViewer = new window.skinview3d.SkinViewer({
    canvas,
    width,
    height,
    skin: stage.dataset.skinUrl,
    model: "auto-detect",
    enableControls: false,
    fov: 38,
    zoom: 0.82
  });
  skinViewer.renderer.setClearColor(0x000000, 0);
  skinViewer.playerObject.rotation.y = -0.42;
  skinViewer.playerObject.rotation.x = 0.06;
  skinViewer.playerObject.position.y = -2;
  if (window.skinview3d.IdleAnimation) {
    skinViewer.animation = new window.skinview3d.IdleAnimation();
    skinViewer.animation.speed = 0.35;
  }
}

function pageHead(title, subtitle, iconName, action = "") {
  return `<div class="page-head"><div class="head-title"><div class="head-icon">${icon(iconName)}</div><div><h1>${title}</h1><p>${subtitle}</p></div></div>${action}</div>`;
}

function navigate(page) {
  const oldIndex = pageOrder.indexOf(currentPage);
  const newIndex = pageOrder.indexOf(page);
  currentPage = page;
  document.querySelectorAll(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.page === page));
  render(newIndex >= oldIndex ? "forward" : "back");
}

function render(direction = "forward") {
  const root = document.querySelector("#page");
  disposeSkinViewer();
  if (currentPage === "home") root.innerHTML = renderHome();
  if (currentPage === "profiles") root.innerHTML = renderProfiles();
  if (currentPage === "library") root.innerHTML = renderLibrary();
  if (currentPage === "modpacks") root.innerHTML = renderModpacks();
  if (currentPage === "accounts") root.innerHTML = renderAccounts();
  if (currentPage === "servers") root.innerHTML = renderServers();
  if (currentPage === "settings") root.innerHTML = renderSettings();
  updateNavLiquid();
  injectIcons(root);
  bindPage();
  if (currentPage === "home") requestAnimationFrame(setupSkinViewer);
  if (currentPage === "home") refreshActiveInstallState().catch(() => {});
}

function renderHome() {
  const profile = activeProfile();
  const account = activeAccount();
  const profileName = profile?.name || "Create your first profile";
  const user = account?.username || "Welcome to Valaris";
  const ready = Boolean(profile && account);
  const versionLabel = profileVersion(profile);
  const main = homeAction(profile, account);
  const partnerSource = partneredServers.length ? partneredServers : [
    { id: "empty-1", name: partnersLoading ? "Loading partners" : "Partnered Servers", address: partnersLoading ? "Syncing from admin panel" : "Add servers from the website admin panel", badge: "INFO", disabled: true },
    { id: "empty-2", name: "Modrinth packs", address: "Browse installable packs", badge: "PACKS", page: "modpacks" }
  ];
  const serverRows = partnerSource.slice(0, 5).map((server, index) => `
    <button class="home-server-row ${server.featured ? "featured" : ""}" ${server.page ? `data-page="${server.page}"` : server.address && !server.disabled ? `data-action="join-server" data-address="${escapeHtml(server.address)}"` : ""}>
      <span class="server-rank">${escapeHtml(server.badge || `#${index + 1}`)}</span>
      <span class="server-cube">${server.icon ? `<img src="${escapeHtml(server.icon)}" alt="">` : icon(server.page === "modpacks" ? "package" : "server")}</span>
      <span class="server-copy"><strong>${escapeHtml(server.name)}${server.online === false ? ' <em>Offline</em>' : ""}</strong><small>${escapeHtml(server.motd || server.address || "Partnered with Valaris")}</small></span>
      <span class="server-meta">${server.players != null && server.maxPlayers ? `${Number(server.players).toLocaleString()}/${Number(server.maxPlayers).toLocaleString()}` : `<span class="server-dot ${server.online === false ? "offline" : ""}"></span>`}</span>
    </button>`).join("");
  return `
    <div class="home-grid">
      <section class="hero-card">
        <div class="hero-top">
          <div>
            <div class="player">${avatar(account)}<div><div class="player-line"><h1>${escapeHtml(user)}</h1>${ready ? '<span class="online">READY</span>' : '<span class="online muted-chip">SETUP</span>'}</div><p>${escapeHtml(profileName)}</p></div></div>
            <div class="tag-row">${profile ? `<span class="tag accent">${escapeHtml(versionLabel)}</span><span class="tag">${escapeHtml(profileLoader(profile))}</span><span class="tag">${profile.memory} MB RAM</span>` : '<span class="tag">No profile selected</span>'}</div>
          </div>
          <div class="hero-actions"><button class="secondary" data-action="console">${icon("terminal")} Console</button><button class="secondary" data-page="library">${icon("download")} Library</button><button class="secondary" data-page="modpacks">${icon("package")} Packs</button></div>
        </div>
        ${state.settings.hideCharacter ? "" : skinBody(account)}
        <div class="launch-zone">
          <div class="launch-buttons single">
            <button class="primary launch-button" data-action="${main.action}" ${main.disabled ? "disabled" : ""}>${icon(main.iconName)} ${main.label}</button>
          </div>
          <div class="progress-track ${progress.percent > 0 && progress.percent < 100 ? "visible" : ""}"><div class="progress-fill" style="width:${progress.percent}%"></div></div>
          <div class="launch-sub">${escapeHtml(runningGame ? `Minecraft has been running for ${formatRuntime()}` : progress.message || setupHint(profile, account))}</div>
        </div>
      </section>
      <aside class="home-side-panel">
        <div class="home-side-head">
          <span class="home-side-icon">${icon("server")}</span>
          <div><h2>Partnered Servers</h2><p>Added from the admin panel</p></div>
          <button class="text-button" data-page="servers">View</button>
        </div>
        <div class="home-server-list">${serverRows}</div>
        <div class="home-side-actions">
          <button class="pixel-shortcut" data-action="new-profile">${icon("plus")} Profile</button>
          <button class="pixel-shortcut" data-page="modpacks">${icon("package")} Modpacks</button>
        </div>
      </aside>
    </div>`;
}

function renderProfiles() {
  const action = `<div class="head-actions"><button class="secondary" data-page="library">${icon("download")} Version library</button><button class="secondary" data-page="modpacks">${icon("package")} Modpacks</button><button class="primary" data-action="new-profile">${icon("plus")} New profile</button></div>`;
  const cards = state.profiles.map((profile) => `
    <article class="profile-card ${profile.id === state.activeProfileId ? "active" : ""}">
      <div class="profile-top">${avatar(activeAccount(), true)}<div class="profile-name">${escapeHtml(profile.name)}</div>${profile.id === state.activeProfileId ? '<span class="active-label">ACTIVE</span>' : ""}</div>
      <div class="profile-body"><div class="tag-row"><span class="tag accent">${escapeHtml(profileVersion(profile))}</span><span class="tag">${escapeHtml(profileLoader(profile))}</span>${profile.modpack ? `<span class="tag">${escapeHtml(profile.modpack.modpackVersion || "Modrinth")}</span>` : ""}</div><div class="tag-row"><span class="tag">${icon("java")} Auto Java</span><span class="tag">${icon("memory")} ${profile.memory} MB</span></div></div>
      <div class="profile-actions"><button data-action="play-profile" data-id="${profile.id}">${icon("play")} Play</button><button data-action="install-profile" data-id="${profile.id}">${icon("download")} Install</button><button data-action="edit-profile" data-id="${profile.id}">${icon("edit")} Edit</button><button data-action="activate-profile" data-id="${profile.id}">${profile.id === state.activeProfileId ? "Selected" : "Select"}</button><button data-action="delete-profile" data-id="${profile.id}" title="Delete">${icon("trash")}</button></div>
    </article>`).join("");
  return `${pageHead("Profiles", "Manage game versions and configurations", "folder", action)}${cards ? `<div class="profile-grid">${cards}</div>` : `<div class="empty-state"><div><div class="head-icon">${icon("folder")}</div><h2>No profiles yet</h2><p>Create a profile and choose any Minecraft Java version.</p><button class="primary" data-action="new-profile">${icon("plus")} Create profile</button></div></div>`}`;
}

function renderModpacks() {
  const controls = `<div class="library-tools"><input class="field" id="modpackSearch" value="${escapeHtml(modpackSearch)}" placeholder="Search Fabric packs on Modrinth"><button class="primary" data-action="search-modpacks">${icon("refresh")} Search</button></div>`;
  if (modpackLoading) {
    return `${pageHead("Modpacks", "Install Fabric packs from Modrinth into clean Valaris folders", "package", controls)}<div class="empty-state"><div><div class="head-icon">${icon("package")}</div><h2>Loading Modrinth</h2><p>Finding packs that can be installed into Valaris.</p></div></div>`;
  }
  const cards = (modpacks?.hits || []).map((pack) => {
    const iconMarkup = pack.iconUrl ? `<img src="${escapeHtml(pack.iconUrl)}" alt="">` : icon("package");
    const categories = (pack.categories || []).slice(0, 4).map((item) => `<span class="tag">${escapeHtml(item)}</span>`).join("");
    return `<article class="modpack-card">
      <div class="modpack-top"><span class="modpack-icon">${iconMarkup}</span><div><h2>${escapeHtml(pack.title)}</h2><p>by ${escapeHtml(pack.author || "Modrinth creator")}</p></div></div>
      <p class="modpack-copy">${escapeHtml(pack.description || "No description provided.")}</p>
      <div class="tag-row">${categories}<span class="tag accent">${Number(pack.downloads || 0).toLocaleString()} downloads</span></div>
      <div class="modpack-actions"><button class="primary" data-action="install-modpack" data-id="${escapeHtml(pack.projectId)}">${icon("download")} Install</button></div>
    </article>`;
  }).join("");
  return `${pageHead("Modpacks", "Install Fabric packs from Modrinth into clean Valaris folders", "package", controls)}
    <section class="modpack-note"><strong>Modrinth packs install as profiles.</strong><span>Valaris reads the .mrpack metadata, creates an instance folder, installs Fabric, then downloads the pack files and overrides.</span></section>
    ${cards ? `<section class="modpack-grid">${cards}</section>` : `<div class="empty-state"><div><div class="head-icon">${icon("package")}</div><h2>No packs loaded</h2><p>Search for a Fabric modpack from Modrinth.</p><button class="primary" data-action="search-modpacks">${icon("refresh")} Load packs</button></div></div>`}`;
}

function libraryVersions() {
  const items = versions?.versions || [];
  const query = versionSearch.trim().toLowerCase();
  return items.filter((version) => {
    if (versionFilter !== "all" && version.type !== versionFilter) return false;
    if (version.type === "snapshot" && !state.settings.showSnapshots && versionFilter !== "snapshot") return false;
    if ((version.type === "old_alpha" || version.type === "old_beta") && !state.settings.showHistorical && versionFilter !== version.type) return false;
    if (query && !version.id.toLowerCase().includes(query)) return false;
    return true;
  }).slice(0, 80);
}

function renderLibrary() {
  const counts = {
    release: versions?.versions?.filter((item) => item.type === "release").length || 0,
    snapshot: versions?.versions?.filter((item) => item.type === "snapshot").length || 0,
    old_beta: versions?.versions?.filter((item) => item.type === "old_beta").length || 0,
    old_alpha: versions?.versions?.filter((item) => item.type === "old_alpha").length || 0
  };
  const controls = `<div class="library-tools"><input class="field" id="versionSearch" value="${escapeHtml(versionSearch)}" placeholder="Search versions"><div class="segmented">
    ${[["all", "All"], ["release", "Release"], ["snapshot", "Snapshot"], ["old_beta", "Beta"], ["old_alpha", "Alpha"]].map(([id, label]) => `<button class="${versionFilter === id ? "active" : ""}" data-version-filter="${id}">${label}</button>`).join("")}
  </div></div>`;
  if (!versions) {
    return `${pageHead("Library", "Loading the official Minecraft version catalog", "download", controls)}<div class="empty-state"><div><div class="head-icon">${icon("refresh")}</div><h2>Loading versions</h2><p>Valaris is reaching Mojang's catalog.</p></div></div>`;
  }
  const rows = libraryVersions().map((version) => {
    const profile = state.profiles.find((item) => item.version === version.id);
    return `<article class="version-row">
      <div class="version-main"><strong>${escapeHtml(version.id)}</strong><span>${escapeHtml(version.type.replace("_", " "))}</span></div>
      <div class="version-date">${new Date(version.releaseTime || version.time).toLocaleDateString()}</div>
      <div class="version-actions">
        ${profile ? `<button class="secondary" data-action="activate-profile" data-id="${profile.id}">Use profile</button>` : `<button class="secondary" data-action="profile-from-version" data-version="${escapeHtml(version.id)}">Profile</button>`}
        <button class="primary" data-action="install-version" data-version="${escapeHtml(version.id)}">${icon("download")} Install</button>
      </div>
    </article>`;
  }).join("");
  return `${pageHead("Library", "All official Minecraft Java versions", "download", controls)}
    <section class="library-summary">
      <div class="stat"><strong>${counts.release}</strong><span>Releases</span></div>
      <div class="stat"><strong>${counts.snapshot}</strong><span>Snapshots</span></div>
      <div class="stat"><strong>${counts.old_beta}</strong><span>Beta</span></div>
      <div class="stat"><strong>${counts.old_alpha}</strong><span>Alpha</span></div>
    </section>
    <section class="version-list">${rows || `<div class="empty-state"><div><h2>No versions found</h2><p>Try another search or enable historical versions in Settings.</p></div></div>`}</section>`;
}

function renderAccounts() {
  const list = state.accounts.map((account) => `
    <article class="account-card ${account.id === state.activeAccountId ? "active" : ""}">
      ${avatar(account)}
      <div class="account-info"><h3>${escapeHtml(account.username)} <span class="account-type">${account.type === "microsoft" ? "MICROSOFT" : "LOCAL"}</span></h3><p>Added ${new Date(account.addedAt).toLocaleDateString()} · ${escapeHtml(account.uuid)}</p></div>
      ${account.id === state.activeAccountId ? '<span class="check">✓</span>' : `<button class="secondary" data-action="activate-account" data-id="${account.id}">Use account</button>`}
      <button class="text-button" data-action="delete-account" data-id="${account.id}" title="Remove">${icon("trash")}</button>
    </article>`).join("");
  return `${pageHead("Accounts", "Manage Minecraft identities", "users")}
    <button class="wide-card add-account-card" data-action="add-account"><span class="big-add">${icon("plus")}</span><div><h2>Add an account</h2><p>Microsoft account or local testing account</p></div>${icon("chevron")}</button>
    <h2 class="section-title">Your accounts</h2>
    ${list ? `<div class="account-list">${list}</div>` : `<div class="empty-state"><div><h2>No accounts added</h2><p>Add a Microsoft account to play online, or a local account for offline testing.</p><button class="primary" data-action="add-account">${icon("plus")} Add account</button></div></div>`}`;
}

function renderServers() {
  const cards = partneredServers.map((server) => `
    <article class="server-card partner-card">
      <div class="server-mark">${server.icon ? `<img src="${escapeHtml(server.icon)}" alt="">` : icon("server")}</div>
      <div class="partner-card-main">
        <div class="partner-card-top">
          <h2>${escapeHtml(server.name)}</h2>
          <span class="server-state ${server.online === false ? "offline" : "online"}">${server.online === false ? "Offline" : "Online"}</span>
        </div>
        <p class="partner-motd">${escapeHtml(server.motd || "No MOTD returned yet.")}</p>
        <button class="partner-address" data-action="copy-server" data-address="${escapeHtml(server.address || "")}">${icon("copy")} ${escapeHtml(server.address || "Partnered with Valaris")}</button>
        <div class="tag-row">
          <span class="tag accent">${escapeHtml(server.badge || "PARTNER")}</span>
          ${server.featured ? '<span class="tag">Featured</span>' : ""}
          ${server.players != null && server.maxPlayers != null ? `<span class="tag">${Number(server.players).toLocaleString()}/${Number(server.maxPlayers).toLocaleString()} players</span>` : '<span class="tag">Players loading</span>'}
          ${server.version ? `<span class="tag">${escapeHtml(server.version)}</span>` : ""}
        </div>
      </div>
      <div class="server-actions">
        <button class="primary" data-action="join-server" data-address="${escapeHtml(server.address)}">${icon("play")} Join</button>
      </div>
    </article>`).join("");
  return `${pageHead("Partnered Servers", "Official partners managed from the admin panel", "server", `<button class="secondary" data-action="refresh-partners">${icon("refresh")} Refresh</button>`)}
    ${cards ? `<div class="server-grid">${cards}</div>` : `<div class="empty-state"><div><div class="head-icon">${icon("server")}</div><h2>${partnersLoading ? "Loading partners" : "No partnered servers yet"}</h2><p>Add partnered Minecraft servers from the website admin panel. They are read-only inside the launcher.</p></div></div>`}`;
}

function settingsNav() {
  const tabs = [
    ["general", "settings", "General", "Launcher and presence"],
    ["appearance", "globe", "Appearance", "Themes and background"],
    ["game", "memory", "Game & performance", "Java and memory"],
    ["storage", "harddrive", "Storage", "Game files and folders"],
    ["support", "shield", "Support & recovery", "Diagnostics and console"]
  ];
  return `<aside class="settings-nav">${tabs.map(([id, ico, title, sub]) => `<button class="settings-tab ${settingsTab === id ? "active" : ""}" data-settings-tab="${id}">${icon(ico)}<span><strong>${title}</strong><small>${sub}</small></span></button>`).join("")}<div class="support-status">Changes save automatically</div></aside>`;
}

function settingRow(iconName, title, copy, control) {
  return `<div class="setting-row"><span class="setting-icon">${icon(iconName)}</span><div class="setting-copy"><strong>${title}</strong><p>${copy}</p></div>${control}</div>`;
}

function renderSettingsPanel() {
  const s = state.settings;
  if (settingsTab === "general") return `
    <section class="settings-panel"><div class="panel-heading"><h2>General</h2><p style="color:var(--muted);margin-top:5px">Launcher behavior and connected services</p></div>
    <div class="setting-group"><h3>Discord integration</h3>
      ${settingRow("discord", "Discord Rich Presence", "Detect Discord and show Valaris Client when connected", `<button class="switch ${s.discordEnabled ? "on" : ""}" data-setting-toggle="discordEnabled"></button>`)}
      <label class="label">Discord application ID</label><input class="field" id="discordClientId" value="${escapeHtml(s.discordClientId)}" placeholder="Create an app at discord.com/developers">
      <p class="modal-note" style="margin-top:8px">Valaris detects the running Discord app automatically. Discord still requires an application ID for Rich Presence.</p>
    </div>
    <div class="setting-group"><h3>Partnered servers</h3><label class="label">Feed URL</label><input class="field" id="partnerServersUrl" value="${escapeHtml(s.partnerServersUrl)}" placeholder="https://your-site.com/api/partnered-servers-public"><p class="modal-note" style="margin-top:8px">Servers from this admin feed appear in the launcher and are read-only for users.</p></div>
    <div class="setting-group"><h3>Launcher updates</h3><label class="label">Update feed URL</label><input class="field" id="updateFeedUrl" value="${escapeHtml(s.updateFeedUrl)}" placeholder="https://your-site.com/api/update"><p class="modal-note" style="margin-top:8px">Valaris checks this feed on startup and asks users to update when you publish a newer version.</p></div>
    <div class="setting-group"><h3>Microsoft sign-in</h3><label class="label">Azure application (client) ID</label><input class="field" id="microsoftClientId" value="${escapeHtml(s.microsoftClientId)}" placeholder="Required for Microsoft device-code login"><p class="modal-note" style="margin-top:8px">The Azure app must allow public client flows and use the consumers tenant.</p></div></section>`;
  if (settingsTab === "appearance") return `
    <section class="settings-panel"><div class="panel-heading"><h2>Appearance</h2><p style="color:var(--muted);margin-top:5px">Theme, glass blur and the Valaris background</p></div>
      <div class="setting-group">
        <h3>Theme</h3>
        <div class="theme-grid">
          ${themes.map(([id, label, color, deep, glow]) => `<button class="theme-chip ${s.theme === id ? "active" : ""}" data-theme-choice="${id}" style="--chip:${color};--chip-deep:${deep};--chip-glow:${glow}"><span class="theme-dot"></span><span class="theme-name">${label}</span><span class="theme-sample"><i></i><i></i><i></i></span></button>`).join("")}
        </div>
      </div>
      <div class="setting-group"><h3>Client background</h3>
        ${settingRow("globe", "Space wallpaper", "Uses the Earth orbit image behind the launcher with glass blur on panels", `<span class="theme-saved">ACTIVE</span>`)}
        ${settingRow("users", "Hide character preview", "Show only the background art on the home card", `<button class="switch ${s.hideCharacter ? "on" : ""}" data-setting-toggle="hideCharacter"></button>`)}
      </div>
    </section>`;
  if (settingsTab === "game") return `
    <section class="settings-panel"><div class="panel-heading"><h2>Game & performance</h2><p style="color:var(--muted);margin-top:5px">Java runtime, memory and downloads</p></div>
    <div class="setting-group"><h3>Memory allocation</h3>${settingRow("memory", "Maximum memory", "Recommended: 4096 MB for modern vanilla Minecraft", `<strong id="memoryValue">${s.memory} MB</strong>`)}<div class="range-wrap"><span>1 GB</span><input type="range" min="1024" max="16384" step="512" value="${s.memory}" id="memoryRange"><span>16 GB</span></div></div>
    <div class="setting-group"><h3>Java runtime</h3><div class="form-row"><input class="field" id="javaPath" value="${escapeHtml(s.javaPath)}" placeholder="Automatic (javaw.exe on PATH)"><button class="secondary" data-action="choose-java">Browse</button></div></div>
    <div class="setting-group">${settingRow("play", "Keep launcher open", "Leave Valaris visible after the game starts", `<button class="switch ${s.keepLauncherOpen ? "on" : ""}" data-setting-toggle="keepLauncherOpen"></button>`)}</div></section>`;
  if (settingsTab === "storage") return `
    <section class="settings-panel"><div class="panel-heading"><h2>Storage</h2><p style="color:var(--muted);margin-top:5px">Game files, versions, libraries and assets</p></div>
    <div class="setting-group"><h3>Minecraft data folder</h3><div class="form-row"><input class="field" id="gameDirectory" value="${escapeHtml(s.gameDirectory)}" placeholder="Valaris default data directory"><button class="secondary" data-action="choose-folder">Browse</button></div>
      <div style="display:flex;gap:10px;margin-top:14px"><button class="secondary" data-action="open-game-folder">${icon("folder")} Open folder</button><button class="danger-button" data-action="clear-cache">${icon("trash")} Clear asset cache</button></div></div>
    <div class="setting-group"><h3>Version catalog</h3>${settingRow("globe", "Show snapshots", "Include development snapshots in version lists", `<button class="switch ${s.showSnapshots ? "on" : ""}" data-setting-toggle="showSnapshots"></button>`)}${settingRow("refresh", "Show historical versions", "Include old alpha and beta builds", `<button class="switch ${s.showHistorical ? "on" : ""}" data-setting-toggle="showHistorical"></button>`)}</div></section>`;
  return `
    <section class="settings-panel"><div class="panel-heading"><h2>Support & recovery</h2><p style="color:var(--muted);margin-top:5px">Diagnostics and launcher output</p></div>
    <div class="setting-group"><h3>Live console</h3><div class="console">${escapeHtml(consoleLines.slice(-250).join("")) || "Launcher output will appear here when Minecraft installs or starts."}</div><div style="display:flex;gap:10px;margin-top:13px"><button class="secondary" data-action="refresh-versions">${icon("refresh")} Refresh version catalog</button><button class="secondary" data-action="clear-console">${icon("trash")} Clear console</button></div></div>
    <div class="setting-group"><h3>About Valaris</h3><p class="modal-note">Valaris 1.0.0 downloads official Minecraft Java Edition files directly from Mojang services. Valaris is an independent launcher and is not affiliated with Mojang Studios or Microsoft.</p></div></section>`;
}

function renderSettings() {
  return `${pageHead("Settings", "Configure launcher preferences and defaults", "settings")}<div class="settings-layout">${settingsNav()}${renderSettingsPanel()}</div>`;
}

function modal(content) {
  document.querySelector("#modalRoot").innerHTML = `<div class="modal-backdrop"><div class="modal">${content}</div></div>`;
  injectIcons(document.querySelector("#modalRoot"));
}

function closeModal() {
  document.querySelector("#modalRoot").innerHTML = "";
}

async function ensureVersions(force = false) {
  if (versions && !force) return versions;
  try {
    versions = await api.getVersions();
    if (currentPage === "home" || currentPage === "library") render();
    return versions;
  } catch (error) {
    toast(`Version catalog: ${error.message}`, "error");
    throw error;
  }
}

async function ensureModpacks(force = false) {
  if (modpacks && !force) return modpacks;
  try {
    modpackLoading = true;
    if (currentPage === "modpacks") render();
    modpacks = await api.searchModpacks(modpackSearch);
    return modpacks;
  } catch (error) {
    toast(`Modrinth: ${error.message}`, "error");
    throw error;
  } finally {
    modpackLoading = false;
    if (currentPage === "modpacks") render();
  }
}

async function ensurePartneredServers(force = false) {
  if (partnersLoaded && !force) return partneredServers;
  try {
    partnersLoading = true;
    if (currentPage === "home" || currentPage === "servers") render();
    partneredServers = await api.getPartnerServers();
    partnersLoaded = true;
    return partneredServers;
  } catch (error) {
    partnersLoaded = true;
    consoleLines.push(`Partnered servers: ${error.message}\n`);
    return partneredServers;
  } finally {
    partnersLoading = false;
    if (currentPage === "home" || currentPage === "servers") render();
  }
}

async function checkForUpdates(showNoUpdate = false) {
  try {
    const update = await api.checkUpdates();
    if (!update?.available) {
      if (showNoUpdate) toast("Valaris is up to date.", "success");
      return;
    }
    const size = update.file?.size ? ` · ${formatBytes(update.file.size)}` : "";
    modal(`<div class="modal-head"><h2>${escapeHtml(update.title || "Valaris update available")}</h2>${update.required ? "" : '<button class="modal-close" data-action="update-later" data-version="' + escapeHtml(update.latestVersion) + '" data-update-key="' + escapeHtml(update.updateKey || "") + '">×</button>'}</div>
      <div class="update-card">
        <span class="head-icon">${icon("download")}</span>
        <div>
          <h3>Version ${escapeHtml(update.latestVersion)}</h3>
          <p>Current version: ${escapeHtml(update.currentVersion)}${size}</p>
        </div>
      </div>
      <p class="modal-note update-notes">${escapeHtml(update.notes || "A new Valaris Client build is ready to download.")}</p>
      <div class="modal-actions">
        ${update.required ? "" : `<button class="secondary" data-action="update-later" data-version="${escapeHtml(update.latestVersion)}" data-update-key="${escapeHtml(update.updateKey || "")}">Later</button>`}
        <button class="primary" data-action="update-now" data-url="${escapeHtml(update.downloadUrl)}" data-filename="${escapeHtml(update.file?.filename || "")}" data-update-key="${escapeHtml(update.updateKey || "")}">${icon("download")} Update now</button>
      </div>`);
  } catch (error) {
    if (showNoUpdate) toast(error.message || "Update check failed.", "error");
  }
}

async function refreshActiveInstallState() {
  const profile = activeProfile();
  if (!profile?.id) return;
  const installed = await api.isInstalled(profile.id);
  if (installedProfiles[profile.id] !== installed) {
    installedProfiles = { ...installedProfiles, [profile.id]: installed };
    if (currentPage === "home") render();
  }
}

async function showProfileModal(existing) {
  modal(`<div class="modal-head"><h2>${existing ? "Edit profile" : "New profile"}</h2><button class="modal-close" data-action="close-modal">×</button></div><p class="modal-note">Loading the official Minecraft version catalog…</p>`);
  try {
    const catalog = await ensureVersions();
    const filtered = catalog.versions.filter((version) => {
      if (version.type === "snapshot" && !state.settings.showSnapshots) return false;
      if ((version.type === "old_alpha" || version.type === "old_beta") && !state.settings.showHistorical) return false;
      return true;
    });
    modal(`<div class="modal-head"><h2>${existing ? "Edit profile" : "New profile"}</h2><button class="modal-close" data-action="close-modal">×</button></div>
      <form id="profileForm">
        <label class="label">Profile name</label><input class="field" name="name" value="${escapeHtml(existing?.name || `Minecraft ${catalog.latest.release}`)}" maxlength="40" required>
        <label class="label">Minecraft version</label><select class="field" name="version">${filtered.map((version) => `<option value="${escapeHtml(version.id)}" ${version.id === (existing?.version || catalog.latest.release) ? "selected" : ""}>${escapeHtml(version.id)} · ${version.type.replace("_", " ")}</option>`).join("")}</select>
        <label class="label">Memory (MB)</label><input class="field" name="memory" type="number" min="1024" max="32768" step="512" value="${existing?.memory || state.settings.memory}">
        <label class="label">Instance folder (optional)</label><input class="field" name="gameDirectory" value="${escapeHtml(existing?.gameDirectory || "")}" placeholder="Valaris creates an isolated folder automatically">
        <div class="modal-actions"><button type="button" class="secondary" data-action="close-modal">Cancel</button><button class="primary" type="submit">Save profile</button></div>
      </form>`);
    document.querySelector("#profileForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = Object.fromEntries(new FormData(event.currentTarget));
      try {
        state = await api.saveProfile({ ...existing, ...data, memory: Number(data.memory) });
        closeModal(); updateTopAccount(); render(); toast("Profile saved.", "success");
      } catch (error) { toast(error.message, "error"); }
    });
  } catch {}
}

function showAccountModal() {
  modal(`<div class="modal-head"><h2>Add an account</h2><button class="modal-close" data-action="close-modal">×</button></div>
    <div class="choice-grid">
      <button class="choice" data-action="microsoft-account">${icon("shield")}<h3>Microsoft</h3><p>Owns Minecraft Java Edition. Supports online play and authenticated servers.</p></button>
      <button class="choice" data-action="offline-account">${icon("users")}<h3>Local account</h3><p>For offline testing and local worlds. Online-mode servers will reject it.</p></button>
    </div>`);
}

function showOfflineModal() {
  modal(`<div class="modal-head"><h2>Add local account</h2><button class="modal-close" data-action="close-modal">×</button></div>
    <form id="offlineForm"><p class="modal-note">Local accounts are intended for offline testing. They do not authenticate ownership.</p><label class="label">Username</label><input class="field" name="username" minlength="3" maxlength="16" pattern="[A-Za-z0-9_]+" required autofocus><div class="modal-actions"><button type="button" class="secondary" data-action="close-modal">Cancel</button><button class="primary" type="submit">Add account</button></div></form>`);
  document.querySelector("#offlineForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      state = await api.addOfflineAccount(new FormData(event.currentTarget).get("username"));
      closeModal(); updateTopAccount(); render(); toast("Local account added.", "success");
    } catch (error) { toast(error.message, "error"); }
  });
}

async function beginMicrosoft() {
  try {
    const data = await api.beginMicrosoftLogin();
    modal(`<div class="modal-head"><h2>Sign in with Microsoft</h2><button class="modal-close" data-action="close-modal">×</button></div><p class="modal-note">Open Microsoft’s verification page and enter this one-time code:</p><div class="device-code">${escapeHtml(data.userCode)}</div><button class="primary" style="width:100%" data-action="open-microsoft" data-url="${escapeHtml(data.verificationUri)}">${icon("globe")} Open Microsoft sign-in</button><p class="modal-note" id="authStatus" style="text-align:center;margin-top:16px">Waiting for approval…</p>`);
  } catch (error) {
    toast(error.message, "error");
    closeModal();
    if (error.message.includes("application ID")) { settingsTab = "general"; navigate("settings"); }
  }
}

function showServerModal() {
  modal(`<div class="modal-head"><h2>Add server</h2><button class="modal-close" data-action="close-modal">×</button></div><form id="serverForm"><label class="label">Server name</label><input class="field" name="name" required maxlength="40"><label class="label">Address</label><input class="field" name="address" required placeholder="play.example.net or host:port"><div class="modal-actions"><button type="button" class="secondary" data-action="close-modal">Cancel</button><button class="primary" type="submit">Save server</button></div></form>`);
  document.querySelector("#serverForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      state = await api.saveServer(Object.fromEntries(new FormData(event.currentTarget)));
      closeModal(); render(); toast("Server saved.", "success");
    } catch (error) { toast(error.message, "error"); }
  });
}

async function ensureProfileForVersion(versionId) {
  let profile = state.profiles.find((item) => item.version === versionId);
  if (profile) {
    state = await api.activateProfile(profile.id);
    return state.profiles.find((item) => item.id === profile.id);
  }
  state = await api.saveProfile({
    name: `Minecraft ${versionId}`,
    version: versionId,
    memory: state.settings.memory
  });
  return activeProfile();
}

async function installProfile(profile) {
  if (!profile) return navigate("profiles");
  progress = { percent: 1, message: `Preparing ${profileVersion(profile)}` };
  if (currentPage !== "home") navigate("home"); else render();
  await api.install(profile.id);
  installedProfiles = { ...installedProfiles, [profile.id]: true };
  progress = { percent: 100, message: `${profileVersion(profile)} is installed and ready` };
  render();
  toast(`${profile.name} is ready.`, "success");
}

async function showModpackVersionModal(projectId) {
  const project = (modpacks?.hits || []).find((item) => item.projectId === projectId);
  if (!project) return toast("Pick a modpack first.", "error");
  modal(`<div class="modal-head"><h2>${escapeHtml(project.title)}</h2><button class="modal-close" data-action="close-modal">×</button></div><p class="modal-note">Loading supported Fabric versions from Modrinth…</p>`);
  try {
    const supported = await api.getModpackVersions(project.projectId);
    const rows = supported.map((version) => `<button class="version-choice" data-action="install-modpack-version" data-id="${escapeHtml(project.projectId)}" data-version-id="${escapeHtml(version.id)}">
      <span><strong>${escapeHtml(version.versionNumber)}</strong><small>${escapeHtml(version.name || "Modrinth version")}</small></span>
      <span><strong>${escapeHtml((version.minecraftVersions || [version.minecraftVersion]).join(", "))}</strong><small>Fabric pack</small></span>
      <span class="tag ${version.versionType === "release" ? "accent" : ""}">${escapeHtml(version.versionType)}</span>
    </button>`).join("");
    modal(`<div class="modal-head"><h2>${escapeHtml(project.title)}</h2><button class="modal-close" data-action="close-modal">×</button></div>
      <p class="modal-note">Choose a version that this modpack actually supports. Valaris only shows Fabric .mrpack releases here.</p>
      <div class="version-choice-list">${rows || `<div class="empty-mini">No supported Fabric .mrpack versions were found for this pack.</div>`}</div>`);
  } catch (error) {
    closeModal();
    toast(error.message, "error");
  }
}

async function installModpack(projectId, versionId) {
  const project = (modpacks?.hits || []).find((item) => item.projectId === projectId);
  if (!project) return toast("Pick a modpack first.", "error");
  closeModal();
  progress = { percent: 1, message: `Preparing ${project.title}` };
  if (currentPage !== "home") navigate("home"); else render();
  state = await api.installModpack({ ...project, versionId });
  progress = { percent: 100, message: `${project.title} is installed and ready` };
  if (state.activeProfileId) installedProfiles = { ...installedProfiles, [state.activeProfileId]: true };
  updateTopAccount();
  render();
  toast(`${project.title} installed as a profile.`, "success");
}

async function launch(serverAddress, profileOverride) {
  const profile = profileOverride || activeProfile();
  if (!profile || !activeAccount()) {
    navigate(!activeAccount() ? "accounts" : "profiles");
    return;
  }
  if (profile.id !== state.activeProfileId) state = await api.activateProfile(profile.id);
  progress = { percent: 1, message: "Preparing Minecraft" };
  if (currentPage !== "home") navigate("home"); else render();
  try {
    const result = await api.launch(profile.id, serverAddress);
    runningGame = { pid: result.pid, startedAt: Date.now() };
    startRuntimeTicker();
    progress = { percent: 100, message: `Minecraft started - PID ${result.pid}` };
    render();
    toast("Minecraft is running.", "success");
  } catch (error) {
    progress = { percent: 0, message: error.message };
    render();
    toast(error.message.includes("ENOENT") ? "Java was not found. Choose a Java executable in Settings." : error.message, "error");
  }
}

function showConsole() {
  modal(`<div class="modal-head"><h2>Launcher console</h2><button class="modal-close" data-action="close-modal">×</button></div><div class="console" id="modalConsole">${escapeHtml(consoleLines.slice(-300).join("")) || "No output yet."}</div>`);
}

function bindPage() {
  document.querySelectorAll("[data-settings-tab]").forEach((button) => button.addEventListener("click", () => { settingsTab = button.dataset.settingsTab; render(); }));
  document.querySelectorAll("[data-theme-choice]").forEach((button) => button.addEventListener("click", async () => {
    const previous = state.settings.theme;
    state = { ...state, settings: { ...state.settings, theme: button.dataset.themeChoice } };
    applyTheme();
    render();
    try {
      state = await api.saveSettings({ theme: button.dataset.themeChoice });
    } catch (error) {
      state = { ...state, settings: { ...state.settings, theme: previous } };
      applyTheme();
      render();
      toast(error.message || "Theme could not be saved.", "error");
    }
  }));
  document.querySelectorAll("[data-version-filter]").forEach((button) => button.addEventListener("click", () => { versionFilter = button.dataset.versionFilter; render(); }));
  const search = document.querySelector("#versionSearch");
  search?.addEventListener("input", () => { versionSearch = search.value; render(); document.querySelector("#versionSearch")?.focus(); });
  const modSearch = document.querySelector("#modpackSearch");
  modSearch?.addEventListener("change", () => { modpackSearch = modSearch.value; ensureModpacks(true).catch(() => {}); });
  document.querySelectorAll("[data-setting-toggle]").forEach((button) => button.addEventListener("click", async () => {
    const key = button.dataset.settingToggle;
    state = await api.saveSettings({ [key]: !state.settings[key] });
    applyTheme();
    render();
  }));
  const discord = document.querySelector("#discordClientId");
  discord?.addEventListener("change", async () => { state = await api.saveSettings({ discordClientId: discord.value.trim() }); toast("Discord setting saved.", "success"); });
  const microsoft = document.querySelector("#microsoftClientId");
  microsoft?.addEventListener("change", async () => { state = await api.saveSettings({ microsoftClientId: microsoft.value.trim() }); toast("Microsoft sign-in setting saved.", "success"); });
  const partnersUrl = document.querySelector("#partnerServersUrl");
  partnersUrl?.addEventListener("change", async () => {
    state = await api.saveSettings({ partnerServersUrl: partnersUrl.value.trim() });
    partneredServers = [];
    partnersLoaded = false;
    await ensurePartneredServers(true);
    toast("Partnered server feed saved.", "success");
  });
  const updateFeed = document.querySelector("#updateFeedUrl");
  updateFeed?.addEventListener("change", async () => {
    state = await api.saveSettings({ updateFeedUrl: updateFeed.value.trim(), skippedUpdateVersion: "" });
    toast("Update feed saved.", "success");
    checkForUpdates(true);
  });
  const range = document.querySelector("#memoryRange");
  range?.addEventListener("input", () => { document.querySelector("#memoryValue").textContent = `${range.value} MB`; });
  range?.addEventListener("change", async () => { state = await api.saveSettings({ memory: Number(range.value) }); });
  const javaPath = document.querySelector("#javaPath");
  javaPath?.addEventListener("change", async () => { state = await api.saveSettings({ javaPath: javaPath.value.trim() }); });
  const gameDir = document.querySelector("#gameDirectory");
  gameDir?.addEventListener("change", async () => { state = await api.saveSettings({ gameDirectory: gameDir.value.trim() }); });
}

function updateProgressUI() {
  if (currentPage !== "home") return;
  const fill = document.querySelector(".progress-fill");
  const track = document.querySelector(".progress-track");
  const sub = document.querySelector(".launch-sub");
  const profile = activeProfile();
  const account = activeAccount();
  if (fill) fill.style.width = `${Math.max(0, Math.min(100, progress.percent || 0))}%`;
  if (track) track.classList.toggle("visible", progress.percent > 0 && progress.percent < 100);
  if (sub) sub.textContent = progress.message || setupHint(profile, account);
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action],[data-window],[data-page]");
  if (!button) return;
  if (button.dataset.window) return api[button.dataset.window]();
  if (button.dataset.page) {
    navigate(button.dataset.page);
    if (currentPage === "modpacks") ensureModpacks().catch(() => {});
    if (currentPage === "servers") ensurePartneredServers().catch(() => {});
    return;
  }
  const action = button.dataset.action;
  if (!action) return;
  try {
    if (action === "noop") return;
    if (action === "new-profile") showProfileModal();
    if (action === "edit-profile") showProfileModal(state.profiles.find((item) => item.id === button.dataset.id));
    if (action === "delete-profile" && confirm("Delete this profile configuration? Downloaded shared game files will be kept.")) { state = await api.deleteProfile(button.dataset.id); render(); }
    if (action === "activate-profile") { state = await api.activateProfile(button.dataset.id); render(); toast("Active profile changed.", "success"); }
    if (action === "add-account") showAccountModal();
    if (action === "offline-account") showOfflineModal();
    if (action === "microsoft-account") beginMicrosoft();
    if (action === "activate-account") { state = await api.activateAccount(button.dataset.id); updateTopAccount(); render(); }
    if (action === "delete-account" && confirm("Remove this account from Valaris?")) { state = await api.deleteAccount(button.dataset.id); updateTopAccount(); render(); }
    if (action === "close-modal") closeModal();
    if (action === "open-microsoft") api.openExternal(button.dataset.url);
    if (action === "launch") launch();
    if (action === "play-profile") launch(undefined, state.profiles.find((item) => item.id === button.dataset.id));
    if (action === "install") await installProfile(activeProfile());
    if (action === "install-profile") await installProfile(state.profiles.find((item) => item.id === button.dataset.id));
    if (action === "profile-from-version") { await ensureProfileForVersion(button.dataset.version); render(); toast("Profile created.", "success"); }
    if (action === "install-version") await installProfile(await ensureProfileForVersion(button.dataset.version));
    if (action === "search-modpacks") { const input = document.querySelector("#modpackSearch"); modpackSearch = input?.value || modpackSearch; await ensureModpacks(true); }
    if (action === "install-modpack") await showModpackVersionModal(button.dataset.id);
    if (action === "install-modpack-version") await installModpack(button.dataset.id, button.dataset.versionId);
    if (action === "console") showConsole();
    if (action === "new-server") showServerModal();
    if (action === "delete-server" && confirm("Remove this saved server?")) { state = await api.deleteServer(button.dataset.id); render(); }
    if (action === "join-server") launch(button.dataset.address);
    if (action === "copy-server") { api.copyText(button.dataset.address); toast("Server address copied.", "success"); }
    if (action === "update-now") {
      modal(`<div class="modal-head"><h2>Downloading update</h2></div><p class="modal-note">Valaris is downloading the new installer. It will open automatically when ready.</p>`);
      await api.downloadAndRunUpdate({ downloadUrl: button.dataset.url, filename: button.dataset.filename, updateKey: button.dataset.updateKey });
    }
    if (action === "update-later") { await api.skipUpdate({ version: button.dataset.version, updateKey: button.dataset.updateKey }); closeModal(); toast("Update skipped for now.", "success"); }
    if (action === "choose-java") { const selected = await api.chooseJava(); if (selected) { state = await api.saveSettings({ javaPath: selected }); render(); } }
    if (action === "choose-folder") { const selected = await api.chooseFolder(); if (selected) { state = await api.saveSettings({ gameDirectory: selected }); render(); } }
    if (action === "open-game-folder") await api.openFolder(state.settings.gameDirectory || "");
    if (action === "clear-cache" && confirm("Delete the downloaded Minecraft asset cache? It will be downloaded again when needed.")) { await api.clearCache(); toast("Asset cache cleared.", "success"); }
    if (action === "refresh-partners") { await ensurePartneredServers(true); toast("Partnered servers refreshed.", "success"); }
    if (action === "refresh-versions") { await ensureVersions(true); toast("Version catalog refreshed.", "success"); render(); }
    if (action === "clear-console") { consoleLines = []; render(); }
  } catch (error) {
    toast(error.message || String(error), "error");
  }
});

api.onProgress((value) => {
  if (value.stage === "console") consoleLines.push(value.message);
  else if (value.stage === "closed") {
    runningGame = null;
    stopRuntimeTicker();
    progress = { percent: 0, message: "Minecraft closed. Ready to play again." };
    if (currentPage === "home") render();
  }
  else if (value.stage === "error") { consoleLines.push(`ERROR: ${value.message}\n`); toast(value.message, "error"); }
  else progress = { percent: value.percent ?? progress.percent, message: value.message || progress.message };
  if (currentPage === "home" && value.stage !== "console") updateProgressUI();
  const modalConsole = document.querySelector("#modalConsole");
  if (modalConsole) { modalConsole.textContent = consoleLines.slice(-300).join(""); modalConsole.scrollTop = modalConsole.scrollHeight; }
});

api.onAuthStatus((value) => {
  const node = document.querySelector("#authStatus");
  if (node) node.textContent = value.message;
});

api.onAuthComplete((value) => {
  if (value.error) return toast(value.error, "error");
  state = value.state;
  closeModal();
  updateTopAccount();
  render();
  toast("Microsoft account connected.", "success");
});

api.onDiscordStatus((value) => {
  if (!value.connected && state?.settings.discordEnabled && state?.settings.discordClientId) toast(`Discord: ${value.message}`, "error");
});

injectIcons();
window.addEventListener("resize", () => {
  updateNavLiquid();
  if (currentPage === "home") requestAnimationFrame(setupSkinViewer);
});

(async function init() {
  state = await api.getState();
  applyTheme();
  updateTopAccount();
  render();
  ensureVersions().catch(() => {});
  ensurePartneredServers().catch(() => {});
  setTimeout(() => checkForUpdates(), 900);
})();
