use crate::accounts;
use crate::bridge;
use crate::content;
use crate::discord;
use crate::downloads;
use crate::ecosystem;
use crate::error::{AppError, OkResult};
use crate::instances;
use crate::java;
use crate::launch;
use crate::logs;
use crate::paths;
use crate::performance;
use crate::settings;
use crate::social;
use crate::state::AppState;
use crate::updates;
use serde_json::{json, Value};
use std::fs;
use tauri::{AppHandle, State};

#[tauri::command]
pub fn window_minimize(app: AppHandle) -> Result<(), AppError> {
    if let Some(win) = app.get_webview_window("main") {
        win.minimize().map_err(|e| AppError::Message(e.to_string()))?;
    }
    Ok(())
}

#[tauri::command]
pub fn window_maximize(app: AppHandle) -> Result<(), AppError> {
    if let Some(win) = app.get_webview_window("main") {
        if win.is_maximized().unwrap_or(false) {
            win.unmaximize().map_err(|e| AppError::Message(e.to_string()))?;
        } else {
            win.maximize().map_err(|e| AppError::Message(e.to_string()))?;
        }
    }
    Ok(())
}

#[tauri::command]
pub fn window_close(app: AppHandle) -> Result<(), AppError> {
    if let Some(win) = app.get_webview_window("main") {
        win.close().map_err(|e| AppError::Message(e.to_string()))?;
    }
    Ok(())
}

#[tauri::command]
pub fn app_get_version() -> String {
    env!("CARGO_PKG_VERSION").to_string()
}

#[tauri::command]
pub fn app_get_platform() -> String {
    std::env::consts::OS.to_string()
}

#[tauri::command]
pub fn app_restart(app: AppHandle) {
    app.restart();
}

#[tauri::command]
pub fn boot_initialize(state: State<'_, AppState>) -> Result<(), AppError> {
    state.ensure_dirs()?;
    let _ = settings::load()?;
    let _ = accounts::load()?;
    let _ = instances::list()?;
    let s = settings::load()?;
    discord::set_enabled(s.discord_rpc);
    Ok(())
}

#[tauri::command]
pub fn settings_get() -> Result<settings::LauncherSettings, AppError> {
    settings::load()
}

#[tauri::command]
pub fn settings_update(patch: Value) -> Result<Value, AppError> {
    let settings = settings::update_merge(patch)?;
    discord::set_enabled(settings.discord_rpc);
    Ok(json!({ "settings": settings }))
}

#[tauri::command]
pub fn account_get_prime() -> Result<accounts::PrimeAccount, AppError> {
    accounts::get_prime()
}

#[tauri::command]
pub fn account_get_minecraft() -> Result<Vec<Value>, AppError> {
    accounts::get_minecraft()
}

#[tauri::command]
pub fn account_get_active() -> Result<Option<Value>, AppError> {
    accounts::get_active()
}

#[tauri::command]
pub fn account_set_active(account_id: String) -> Result<Option<Value>, AppError> {
    accounts::set_active(account_id)
}

#[tauri::command]
pub fn account_add_offline(username: String) -> Result<OkResult, AppError> {
    accounts::add_offline(username)
}

#[tauri::command]
pub fn account_remove(account_id: String) -> Result<OkResult, AppError> {
    accounts::remove(account_id)
}

#[tauri::command]
pub fn account_login_microsoft() -> Result<OkResult, AppError> {
    accounts::login_microsoft()
}

#[tauri::command]
pub fn account_refresh_microsoft(account_id: String) -> Result<OkResult, AppError> {
    accounts::refresh_microsoft(account_id)
}

#[tauri::command]
pub fn account_sync_prime() -> Result<Value, AppError> {
    accounts::sync_prime()
}

#[tauri::command]
pub fn instance_list() -> Result<Vec<Value>, AppError> {
    instances::list()
}

#[tauri::command]
pub fn instance_get(id: String) -> Result<Option<Value>, AppError> {
    instances::get(&id)
}

#[tauri::command]
pub fn instance_get_default() -> Result<Option<Value>, AppError> {
    instances::get_default()
}

#[tauri::command]
pub fn instance_create(input: Value) -> Result<Value, AppError> {
    instances::create(input)
}

#[tauri::command]
pub fn instance_update(input: Value) -> Result<Value, AppError> {
    instances::update(input)
}

#[tauri::command]
pub fn instance_remove(id: String, delete_files: Option<bool>) -> Result<Value, AppError> {
    instances::remove(&id, delete_files.unwrap_or(false))
}

#[tauri::command]
pub fn instance_duplicate(id: String) -> Result<Value, AppError> {
    instances::duplicate(&id)
}

#[tauri::command]
pub fn instance_set_default(id: String) -> Result<Value, AppError> {
    instances::set_default(&id)
}

#[tauri::command]
pub fn instance_open_folder(id: String) -> Result<(), AppError> {
    instances::open_folder(&id)
}

#[tauri::command]
pub fn profile_get_active() -> Result<Option<accounts::LauncherProfile>, AppError> {
    accounts::get_active_profile()
}

#[tauri::command]
pub fn profile_set_instance(instance_id: String) -> Result<(), AppError> {
    accounts::set_instance(instance_id)
}

#[tauri::command]
pub async fn launch_game(
    app: AppHandle,
    instance_id: String,
    server_address: Option<String>,
) -> Result<Value, AppError> {
    launch::launch_game(app, instance_id, server_address).await
}

#[tauri::command]
pub fn launch_is_running() -> bool {
    launch::is_running()
}

#[tauri::command]
pub async fn news_list() -> Result<Vec<Value>, AppError> {
    let api = std::env::var("PRIME_API_BASE").unwrap_or_else(|_| "http://194.9.172.102:26005".into());
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(8))
        .user_agent("stellar-client-launcher")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))?;
    let mut items = vec![];
    if let Ok(res) = client.get(format!("{api}/v1/news")).send().await {
        if res.status().is_success() {
            if let Ok(body) = res.json::<Value>().await {
                if let Some(arr) = body.get("items").and_then(|v| v.as_array()) {
                    items.extend(arr.iter().cloned());
                }
            }
        }
    }
    if items.is_empty() {
        items.push(json!({
            "id": "prime-platform",
            "title": "Prime Platform",
            "summary": "Launcher + game + friends â€” one ecosystem.",
            "date": chrono::Utc::now().date_naive().to_string(),
            "tag": "launcher"
        }));
    }
    Ok(items)
}

#[tauri::command]
pub async fn content_mods_list(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    content::list_mods(instance_id).await
}

#[tauri::command]
pub async fn content_mods_set_enabled(
    file_name: String,
    enabled: bool,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::set_mod_enabled(file_name, enabled, instance_id).await
}

#[tauri::command]
pub async fn content_mods_remove(
    file_name: String,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::remove_mod(file_name, instance_id).await
}

#[tauri::command]
pub async fn content_search_modrinth(
    query: String,
    project_type: Option<String>,
    instance_id: Option<String>,
) -> Result<Vec<Value>, AppError> {
    content::search_modrinth(query, project_type.unwrap_or_else(|| "mod".into()), instance_id).await
}

#[tauri::command]
pub async fn content_install_modrinth(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    content::install_modrinth_mod(project_id, title, instance_id, version_id).await
}

#[tauri::command]
pub async fn content_resource_packs_list(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    content::list_resource_packs(instance_id).await
}

#[tauri::command]
pub async fn content_shaders_list(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    content::list_shaders(instance_id).await
}

#[tauri::command]
pub async fn update_check() -> Result<Value, AppError> {
    updates::check(env!("CARGO_PKG_VERSION")).await
}

#[tauri::command]
pub async fn update_install_mod(
    download_url: String,
    file_name: String,
    instance_id: String,
) -> Result<Value, AppError> {
    updates::install_mod(download_url, file_name, instance_id).await
}

async fn social_session(state: &AppState) -> Result<social::SocialSession, AppError> {
    {
        let guard = state.inner.lock();
        if let Some(session) = guard.social.clone() {
            return Ok(session);
        }
    }
    let active = accounts::get_active()?.ok_or_else(|| AppError::Message("No Minecraft account. Log in first.".into()))?;
    let uuid = active.get("uuid").and_then(|v| v.as_str()).unwrap_or("");
    let username = active.get("username").and_then(|v| v.as_str()).unwrap_or("Player");
    let offline = active.get("type").and_then(|v| v.as_str()) == Some("offline");
    let session = social::ensure_session(uuid, username, offline).await?;
    state.inner.lock().social = Some(session.clone());
    Ok(session)
}

#[tauri::command]
pub async fn social_connect(app: AppHandle, state: State<'_, AppState>) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    let start_ws = {
        let mut guard = state.inner.lock();
        if !guard.social_ws_started {
            guard.social_ws_started = true;
            true
        } else {
            false
        }
    };
    if start_ws {
        social::spawn_ws(app, session.clone(), state.social_ws_tx.clone());
    }
    social::set_presence(
        state.social_ws_tx.lock().as_ref(),
        "online",
        "In launcher",
        None,
    );
    Ok(json!({ "token": session.token, "uuid": session.uuid }))
}

#[tauri::command]
pub async fn friends_list(state: State<'_, AppState>) -> Result<Vec<social::FriendEntry>, AppError> {
    let session = social_session(&state).await?;
    let payload = social::get_json(&session, "/v1/friends").await?;
    Ok(social::map_friends(&payload))
}

#[tauri::command]
pub async fn friends_add(state: State<'_, AppState>, username: String, _note: Option<String>) -> Result<OkResult, AppError> {
    let session = social_session(&state).await?;
    match social::post_json(&session, "/v1/friends/request", json!({ "username": username })).await {
        Ok(_) => Ok(OkResult::ok()),
        Err(e) => Ok(OkResult::err(e.to_string())),
    }
}

#[tauri::command]
pub async fn friends_accept(state: State<'_, AppState>, friend_id: String) -> Result<OkResult, AppError> {
    let session = social_session(&state).await?;
    match social::post_json(&session, "/v1/friends/accept", json!({ "uuid": friend_id })).await {
        Ok(_) => Ok(OkResult::ok()),
        Err(e) => Ok(OkResult::err(e.to_string())),
    }
}

#[tauri::command]
pub async fn friends_remove(state: State<'_, AppState>, friend_id: String) -> Result<OkResult, AppError> {
    let session = social_session(&state).await?;
    match social::delete(&session, &format!("/v1/friends/{friend_id}")).await {
        Ok(_) => Ok(OkResult::ok()),
        Err(e) => Ok(OkResult::err(e.to_string())),
    }
}

#[tauri::command]
pub async fn friends_update_note(
    state: State<'_, AppState>,
    friend_id: String,
    note: String,
) -> Result<OkResult, AppError> {
    let session = social_session(&state).await?;
    match social::put_json(
        &session,
        &format!("/v1/friends/{friend_id}/note"),
        json!({ "note": note }),
    )
    .await
    {
        Ok(_) => Ok(OkResult::ok()),
        Err(e) => Ok(OkResult::err(e.to_string())),
    }
}

#[tauri::command]
pub async fn friends_refresh_all(state: State<'_, AppState>) -> Result<Vec<social::FriendEntry>, AppError> {
    friends_list(state).await
}

#[tauri::command]
pub async fn chat_conversations(state: State<'_, AppState>) -> Result<Vec<Value>, AppError> {
    let session = social_session(&state).await?;
    let payload = social::get_json(&session, "/v1/conversations").await?;
    Ok(payload
        .get("conversations")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default())
}

#[tauri::command]
pub async fn chat_open_dm(state: State<'_, AppState>, uuid: String) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    let payload = social::post_json(&session, "/v1/conversations/dm", json!({ "uuid": uuid })).await?;
    Ok(payload.get("conversation").cloned().unwrap_or(payload))
}

#[tauri::command]
pub async fn chat_messages(state: State<'_, AppState>, conversation_id: String) -> Result<Vec<Value>, AppError> {
    let session = social_session(&state).await?;
    let payload = social::get_json(
        &session,
        &format!("/v1/conversations/{conversation_id}/messages"),
    )
    .await?;
    Ok(payload
        .get("messages")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default())
}

#[tauri::command]
pub async fn chat_send(
    state: State<'_, AppState>,
    conversation_id: String,
    text: String,
    image_url: Option<String>,
) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    let payload = social::post_json(
        &session,
        &format!("/v1/conversations/{conversation_id}/messages"),
        json!({ "text": text, "imageUrl": image_url }),
    )
    .await?;
    Ok(payload.get("message").cloned().unwrap_or(payload))
}

#[tauri::command]
pub async fn party_get(state: State<'_, AppState>) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::get_json(&session, "/v1/party").await
}

#[tauri::command]
pub async fn party_create(state: State<'_, AppState>) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::post_json(&session, "/v1/party", json!({})).await
}

#[tauri::command]
pub async fn party_invite(state: State<'_, AppState>, uuid: String) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::post_json(&session, "/v1/party/invite", json!({ "uuid": uuid })).await
}

#[tauri::command]
pub async fn party_leave(state: State<'_, AppState>) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::post_json(&session, "/v1/party/leave", json!({})).await
}

#[tauri::command]
pub async fn party_accept(state: State<'_, AppState>, invite_id: String) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::post_json(
        &session,
        "/v1/party/accept",
        json!({ "inviteId": invite_id }),
    )
    .await
}

#[tauri::command]
pub async fn party_decline(state: State<'_, AppState>, invite_id: String) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::post_json(
        &session,
        "/v1/party/decline",
        json!({ "inviteId": invite_id }),
    )
    .await
}

#[tauri::command]
pub async fn party_set_server(
    state: State<'_, AppState>,
    server_address: String,
) -> Result<Value, AppError> {
    let session = social_session(&state).await?;
    social::post_json(
        &session,
        "/v1/party/server",
        json!({ "serverAddress": server_address }),
    )
    .await
}

#[tauri::command]
pub async fn social_typing(
    state: State<'_, AppState>,
    conversation_id: String,
) -> Result<(), AppError> {
    let tx = state.social_ws_tx.lock().clone();
    if let Some(sender) = tx {
        let _ = sender.send(
            json!({ "t": "typing", "conversationId": conversation_id }).to_string(),
        );
    }
    Ok(())
}

#[tauri::command]
pub async fn chat_upload(state: State<'_, AppState>, file_path: String) -> Result<String, AppError> {
    let session = social_session(&state).await?;
    social::upload_image(&session, file_path).await
}

#[tauri::command]
pub fn media_list(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    let id = instance_id
        .or_else(|| {
            instances::get_default()
                .ok()
                .flatten()
                .and_then(|v| v.get("id").and_then(|x| x.as_str()).map(str::to_string))
        })
        .unwrap_or_else(|| "prime-fabric".into());
    let screenshots = paths::instance_game_dir(&id).join("screenshots");
    let mut out = vec![];
    if let Ok(rd) = fs::read_dir(screenshots) {
        for e in rd.flatten() {
            let p = e.path();
            if p.extension().and_then(|x| x.to_str()) == Some("png") {
                out.push(json!({
                    "id": e.file_name().to_string_lossy(),
                    "type": "screenshot",
                    "title": e.file_name().to_string_lossy(),
                    "date": "",
                    "size": "",
                    "filePath": p.to_string_lossy(),
                }));
            }
        }
    }
    Ok(out)
}

#[tauri::command]
pub fn media_open_folder(instance_id: Option<String>) -> Result<(), AppError> {
    let id = instance_id.unwrap_or_else(|| "prime-fabric".into());
    let dir = paths::instance_game_dir(&id).join("screenshots");
    fs::create_dir_all(&dir)?;
    open::that(dir).map_err(|e| AppError::Message(e.to_string()))
}

#[tauri::command]
pub fn media_open_file(file_path: String) -> Result<(), AppError> {
    open::that(file_path).map_err(|e| AppError::Message(e.to_string()))
}

#[tauri::command]
pub fn profile_get_all() -> Result<Vec<accounts::LauncherProfile>, AppError> {
    accounts::get_all_profiles()
}

#[tauri::command]
pub fn bridge_sync(instance_id: String) -> Result<OkResult, AppError> {
    bridge::sync_instance(&instance_id)?;
    Ok(OkResult::ok())
}

#[tauri::command]
pub fn launch_logs_list() -> Vec<logs::LogEntry> {
    logs::list()
}

#[tauri::command]
pub fn launch_logs_clear() -> Result<Value, AppError> {
    logs::clear()
}

#[tauri::command]
pub fn launch_logs_open_folder() -> Result<(), AppError> {
    logs::open_folder()
}

#[tauri::command]
pub fn launch_crash_open(file_path: String) -> Result<(), AppError> {
    logs::open_crash(file_path)
}

#[tauri::command]
pub async fn content_import_mod(
    app: AppHandle,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::import_mod(app, instance_id).await
}

#[tauri::command]
pub async fn content_search_curseforge(
    query: String,
    project_type: Option<String>,
    instance_id: Option<String>,
) -> Result<Vec<Value>, AppError> {
    content::search_curseforge(
        query,
        project_type.unwrap_or_else(|| "mod".into()),
        instance_id,
    )
    .await
}

#[tauri::command]
pub async fn content_install_curseforge(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    content::install_curseforge(project_id, title, instance_id, version_id, "mod").await
}

#[tauri::command]
pub async fn content_install_resource_modrinth(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    content::install_modrinth(project_id, title, instance_id, version_id, "resourcepack").await
}

#[tauri::command]
pub async fn content_install_resource_curseforge(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    content::install_curseforge(project_id, title, instance_id, version_id, "resourcepack").await
}

#[tauri::command]
pub async fn content_install_shader_modrinth(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    content::install_modrinth(project_id, title, instance_id, version_id, "shader").await
}

#[tauri::command]
pub async fn content_install_shader_curseforge(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    content::install_curseforge(project_id, title, instance_id, version_id, "shader").await
}

#[tauri::command]
pub async fn content_list_versions(
    project_id: String,
    project_type: String,
    source: String,
    instance_id: Option<String>,
) -> Result<Vec<Value>, AppError> {
    content::list_versions(project_id, project_type, source, instance_id).await
}

#[tauri::command]
pub async fn content_resource_set_active(
    file_name: Option<String>,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::set_resource_pack_active(file_name, instance_id).await
}

#[tauri::command]
pub async fn content_resource_remove(
    file_name: String,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::remove_resource_pack(file_name, instance_id).await
}

#[tauri::command]
pub async fn content_resource_import(
    app: AppHandle,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::import_resource_pack(app, instance_id).await
}

#[tauri::command]
pub async fn content_shader_set_active(
    file_name: Option<String>,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::set_shader_active(file_name, instance_id).await
}

#[tauri::command]
pub async fn content_shader_remove(
    file_name: String,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::remove_shader(file_name, instance_id).await
}

#[tauri::command]
pub async fn content_shader_import(
    app: AppHandle,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    content::import_shader(app, instance_id).await
}

#[tauri::command]
pub fn store_catalog() -> Result<Vec<Value>, AppError> {
    ecosystem::store_catalog()
}

#[tauri::command]
pub fn store_balance() -> Result<i64, AppError> {
    ecosystem::balance()
}

#[tauri::command]
pub fn store_purchase(item_id: String) -> Result<Value, AppError> {
    ecosystem::purchase(item_id)
}

#[tauri::command]
pub fn cosmetic_list() -> Result<Vec<Value>, AppError> {
    ecosystem::cosmetic_list()
}

#[tauri::command]
pub fn cosmetic_toggle(cosmetic_id: String) -> Result<Value, AppError> {
    ecosystem::cosmetic_toggle(cosmetic_id)
}

#[tauri::command]
pub fn servers_list() -> Result<Vec<ecosystem::FavoriteServer>, AppError> {
    ecosystem::servers_list()
}

#[tauri::command]
pub async fn servers_add(name: String, address: String) -> Result<Value, AppError> {
    ecosystem::servers_add(name, address).await
}

#[tauri::command]
pub fn servers_remove(server_id: String) -> Result<Value, AppError> {
    ecosystem::servers_remove(server_id)
}

#[tauri::command]
pub async fn servers_refresh(server_id: String) -> Result<Option<ecosystem::FavoriteServer>, AppError> {
    ecosystem::servers_refresh(server_id).await
}

#[tauri::command]
pub async fn servers_refresh_all() -> Result<Vec<ecosystem::FavoriteServer>, AppError> {
    ecosystem::servers_refresh_all().await
}

#[tauri::command]
pub fn performance_hardware() -> Value {
    performance::hardware()
}

#[tauri::command]
pub fn performance_presets() -> Vec<Value> {
    performance::presets()
}

#[tauri::command]
pub fn performance_selected() -> Result<String, AppError> {
    performance::selected()
}

#[tauri::command]
pub fn performance_apply(
    preset_id: String,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    performance::apply(preset_id, instance_id)
}

#[tauri::command]
pub fn downloads_list() -> Result<Vec<downloads::DownloadTask>, AppError> {
    downloads::list()
}

#[tauri::command]
pub fn downloads_clear() -> Result<Value, AppError> {
    downloads::clear_completed()
}

#[tauri::command]
pub fn downloads_remove(task_id: String) -> Result<Value, AppError> {
    downloads::remove(task_id)
}

#[tauri::command]
pub fn settings_java_list() -> Result<Vec<Value>, AppError> {
    java::list_installations()
}

#[tauri::command]
pub fn settings_java_browse(app: AppHandle) -> Result<Value, AppError> {
    java::browse(&app)
}

#[tauri::command]
pub fn settings_java_add(java_path: String) -> Result<Value, AppError> {
    java::add_custom(java_path)
}

#[tauri::command]
pub async fn update_install_launcher(app: AppHandle) -> Result<Value, AppError> {
    updates::install_launcher(app, env!("CARGO_PKG_VERSION")).await
}

#[tauri::command]
pub fn update_dismiss() -> Result<OkResult, AppError> {
    let mut s = settings::load()?;
    s.last_update_check = Some(chrono::Utc::now().to_rfc3339());
    settings::save(&s)?;
    Ok(OkResult::ok())
}

#[tauri::command]
pub fn dialog_open_file(
    app: AppHandle,
    filters: Option<Vec<Value>>,
) -> Result<Option<String>, AppError> {
    use tauri_plugin_dialog::{DialogExt, FilePath};
    let mut builder = app.dialog().file();
    if let Some(filters) = filters {
        for f in filters {
            let name = f
                .get("name")
                .and_then(|v| v.as_str())
                .unwrap_or("Files")
                .to_string();
            let exts: Vec<String> = f
                .get("extensions")
                .and_then(|v| v.as_array())
                .map(|arr| {
                    arr.iter()
                        .filter_map(|e| e.as_str().map(str::to_string))
                        .collect()
                })
                .unwrap_or_default();
            let refs: Vec<&str> = exts.iter().map(|s| s.as_str()).collect();
            builder = builder.add_filter(&name, &refs);
        }
    }
    Ok(builder.blocking_pick_file().map(|p| match p {
        FilePath::Path(path) => path.to_string_lossy().to_string(),
        FilePath::Url(url) => url.to_string(),
    }))
}

use tauri::Manager;

