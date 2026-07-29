use crate::downloads;
use crate::error::AppError;
use crate::instances;
use crate::options_txt;
use crate::paths;
use crate::settings;
use serde_json::{json, Value};
use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};
use tauri::AppHandle;
use tauri_plugin_dialog::{DialogExt, FilePath};

fn client() -> Result<reqwest::Client, AppError> {
    reqwest::Client::builder()
        .user_agent("stellar-client-launcher/0.9.11")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))
}

async fn resolve_instance(instance_id: Option<String>) -> Result<String, AppError> {
    if let Some(id) = instance_id {
        return Ok(id);
    }
    instances::get_default()?
        .and_then(|v| v.get("id").and_then(|x| x.as_str()).map(str::to_string))
        .ok_or_else(|| AppError::Message("No instance.".into()))
}

fn display_name(file_name: &str, title: Option<&str>) -> String {
    title
        .map(str::to_string)
        .unwrap_or_else(|| {
            file_name
                .trim_end_matches(".zip")
                .trim_end_matches(".jar")
                .replace(['-', '_', '+'], " ")
                .trim()
                .to_string()
        })
}

async fn download_bytes(url: &str) -> Result<Vec<u8>, AppError> {
    client()?
        .get(url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .error_for_status()
        .map_err(|e| AppError::Message(e.to_string()))?
        .bytes()
        .await
        .map(|b| b.to_vec())
        .map_err(|e| AppError::Message(e.to_string()))
}

fn write_file(dest: &Path, bytes: &[u8]) -> Result<(), AppError> {
    if let Some(parent) = dest.parent() {
        fs::create_dir_all(parent)?;
    }
    let mut f = fs::File::create(dest)?;
    f.write_all(bytes)?;
    Ok(())
}

pub async fn list_mods(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    let id = resolve_instance(instance_id).await?;
    let dir = paths::instance_mods_dir(&id);
    fs::create_dir_all(&dir)?;
    let mut out = vec![];
    if let Ok(rd) = fs::read_dir(dir) {
        for e in rd.flatten() {
            let name = e.file_name().to_string_lossy().to_string();
            if !name.ends_with(".jar") && !name.ends_with(".jar.disabled") {
                continue;
            }
            let enabled = name.ends_with(".jar") && !name.ends_with(".jar.disabled");
            let file_name = name.clone();
            let base = name.replace(".disabled", "");
            out.push(json!({
                "id": base,
                "fileName": file_name,
                "name": base.trim_end_matches(".jar"),
                "description": "Local mod file",
                "version": "",
                "author": "",
                "enabled": enabled,
                "source": "local"
            }));
        }
    }
    Ok(out)
}

pub async fn set_mod_enabled(
    file_name: String,
    enabled: bool,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let dir = paths::instance_mods_dir(&id);
    let base = file_name.replace(".disabled", "");
    let jar = dir.join(&base);
    let dis = dir.join(format!("{base}.disabled"));
    if enabled {
        if dis.exists() {
            fs::rename(&dis, &jar)?;
        }
    } else if jar.exists() {
        fs::rename(&jar, &dis)?;
    }
    Ok(json!({ "ok": true }))
}

pub async fn remove_mod(file_name: String, instance_id: Option<String>) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let dir = paths::instance_mods_dir(&id);
    let base = file_name.replace(".disabled", "");
    let _ = fs::remove_file(dir.join(&file_name));
    let _ = fs::remove_file(dir.join(format!("{base}.disabled")));
    let _ = fs::remove_file(dir.join(&base));
    Ok(json!({ "ok": true }))
}

fn pick_file(app: &AppHandle, filters: Vec<(&str, &[&str])>) -> Result<Option<PathBuf>, AppError> {
    let mut builder = app.dialog().file();
    for (name, exts) in filters {
        builder = builder.add_filter(name, exts);
    }
    let picked = builder.blocking_pick_file();
    Ok(picked.map(|p| match p {
        FilePath::Path(path) => path,
        FilePath::Url(url) => PathBuf::from(url.to_string()),
    }))
}

pub async fn import_mod(app: AppHandle, instance_id: Option<String>) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let Some(src) = pick_file(&app, vec![("Minecraft Mods", &["jar"])])? else {
        return Ok(json!({ "ok": false, "error": "cancelled" }));
    };
    let name = src
        .file_name()
        .and_then(|n| n.to_str())
        .ok_or_else(|| AppError::Message("Invalid file".into()))?
        .to_string();
    if !name.ends_with(".jar") {
        return Ok(json!({ "ok": false, "error": "Mods must be .jar files." }));
    }
    let dest = paths::instance_mods_dir(&id).join(&name);
    fs::create_dir_all(dest.parent().unwrap())?;
    fs::copy(&src, &dest)?;
    let _ = downloads::push_completed(&name, "local", "import");
    Ok(json!({ "ok": true, "fileName": name }))
}

pub async fn search_modrinth(
    query: String,
    project_type: String,
    instance_id: Option<String>,
) -> Result<Vec<Value>, AppError> {
    let id = resolve_instance(instance_id).await?;
    let inst = instances::get_stored(&id)?.ok_or_else(|| AppError::Message("No instance".into()))?;
    let pt = if project_type.is_empty() {
        "mod"
    } else {
        project_type.as_str()
    };
    let mut facets = vec![
        vec![format!("project_type:{pt}")],
        vec![format!("versions:{}", inst.minecraft_version)],
    ];
    if pt == "mod" {
        facets.push(vec![format!("categories:{}", inst.loader)]);
    }
    let url = format!(
        "https://api.modrinth.com/v2/search?query={}&limit=20&index=relevance&facets={}",
        urlencoding::encode(&query),
        urlencoding::encode(&serde_json::to_string(&facets).unwrap_or_else(|_| "[]".into()))
    );
    let res: Value = client()?
        .get(url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    Ok(res
        .get("hits")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default())
}

async fn curse_key() -> Result<String, AppError> {
    let key = settings::load()?
        .curse_forge_api_key
        .unwrap_or_default()
        .trim()
        .to_string();
    if key.is_empty() {
        return Err(AppError::Message(
            "CurseForge API key is not configured in launcher settings.".into(),
        ));
    }
    Ok(key)
}

async fn curse_get(path: &str, params: &[(&str, String)]) -> Result<Value, AppError> {
    let key = curse_key().await?;
    let mut url = reqwest::Url::parse(&format!("https://api.curseforge.com/v1{path}"))
        .map_err(|e| AppError::Message(e.to_string()))?;
    {
        let mut q = url.query_pairs_mut();
        for (k, v) in params {
            if !v.is_empty() {
                q.append_pair(k, v);
            }
        }
    }
    let res = client()?
        .get(url)
        .header("Accept", "application/json")
        .header("x-api-key", key)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if !res.status().is_success() {
        let status = res.status();
        let body = res.text().await.unwrap_or_default();
        return Err(AppError::Message(format!(
            "CurseForge API error ({status}): {}",
            body.chars().take(120).collect::<String>()
        )));
    }
    res.json().await.map_err(|e| AppError::Message(e.to_string()))
}

pub async fn search_curseforge(
    query: String,
    project_type: String,
    instance_id: Option<String>,
) -> Result<Vec<Value>, AppError> {
    let id = resolve_instance(instance_id).await?;
    let inst = instances::get_stored(&id)?.ok_or_else(|| AppError::Message("No instance".into()))?;
    let class_id = match project_type.as_str() {
        "resourcepack" => 12,
        "shader" => 6552,
        _ => 6,
    };
    let mut params = vec![
        ("gameId", "432".into()),
        ("classId", class_id.to_string()),
        ("searchFilter", query),
        ("gameVersion", inst.minecraft_version.clone()),
        ("sortField", "2".into()),
        ("sortOrder", "desc".into()),
        ("pageSize", "20".into()),
    ];
    if project_type == "mod" || project_type.is_empty() {
        let loader = match inst.loader.as_str() {
            "forge" => 1,
            "quilt" => 5,
            _ => 4,
        };
        params.push(("modLoaderType", loader.to_string()));
    }
    let body = curse_get("/mods/search", &params).await?;
    let data = body.get("data").and_then(|v| v.as_array()).cloned().unwrap_or_default();
    Ok(data
        .into_iter()
        .map(|mod_| {
            json!({
                "project_id": mod_.get("id").and_then(|v| v.as_u64()).unwrap_or(0).to_string(),
                "slug": mod_.get("slug"),
                "title": mod_.get("name"),
                "description": mod_.get("summary").unwrap_or(&json!("")),
                "downloads": mod_.get("downloadCount").unwrap_or(&json!(0)),
                "icon_url": mod_.pointer("/logo/thumbnailUrl").or_else(|| mod_.pointer("/logo/url")),
                "project_type": project_type
            })
        })
        .collect())
}

async fn pick_modrinth_version(
    project_id: &str,
    mc: &str,
    loader: Option<&str>,
    version_id: Option<&str>,
) -> Result<Value, AppError> {
    if let Some(vid) = version_id {
        return client()?
            .get(format!("https://api.modrinth.com/v2/version/{vid}"))
            .send()
            .await
            .map_err(|e| AppError::Message(e.to_string()))?
            .json()
            .await
            .map_err(|e| AppError::Message(e.to_string()));
    }
    let mut url = format!(
        "https://api.modrinth.com/v2/project/{project_id}/version?game_versions=[\"{mc}\"]"
    );
    if let Some(l) = loader {
        url.push_str(&format!("&loaders=[\"{l}\"]"));
    }
    let versions: Vec<Value> = client()?
        .get(url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    versions
        .into_iter()
        .next()
        .ok_or_else(|| AppError::Message("No compatible Modrinth version".into()))
}

fn primary_file(version: &Value) -> Result<&Value, AppError> {
    version
        .get("files")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter()
                .find(|f| f.get("primary").and_then(|p| p.as_bool()).unwrap_or(false))
                .or_else(|| arr.first())
        })
        .ok_or_else(|| AppError::Message("No file".into()))
}

pub async fn install_modrinth(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
    dest_kind: &str,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let inst = instances::get_stored(&id)?.ok_or_else(|| AppError::Message("No instance".into()))?;
    let loader = if dest_kind == "mod" {
        Some(inst.loader.as_str())
    } else {
        None
    };
    let version = pick_modrinth_version(
        &project_id,
        &inst.minecraft_version,
        loader,
        version_id.as_deref(),
    )
    .await?;
    let file = primary_file(&version)?;
    let url = file
        .get("url")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("No download url".into()))?;
    let filename = file
        .get("filename")
        .and_then(|v| v.as_str())
        .unwrap_or("content.bin")
        .to_string();
    let bytes = download_bytes(url).await?;
    let dest_dir = match dest_kind {
        "resourcepack" => paths::instance_game_dir(&id).join("resourcepacks"),
        "shader" => paths::instance_game_dir(&id).join("shaderpacks"),
        _ => paths::instance_mods_dir(&id),
    };
    let dest = dest_dir.join(&filename);
    write_file(&dest, &bytes)?;
    let _ = downloads::push_completed(&title, &format!("{} KB", bytes.len() / 1024), "Modrinth");
    Ok(json!({ "ok": true, "fileName": filename }))
}

pub async fn install_modrinth_mod(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    version_id: Option<String>,
) -> Result<Value, AppError> {
    install_modrinth(project_id, title, instance_id, version_id, "mod").await
}

async fn curse_download_file(
    mod_id: &str,
    file_id: u64,
    dest: &Path,
) -> Result<(), AppError> {
    let body = curse_get(
        &format!("/mods/{mod_id}/files/{file_id}/download-url"),
        &[],
    )
    .await?;
    let url = body
        .get("data")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("CurseForge did not return a download URL.".into()))?;
    let bytes = download_bytes(url).await?;
    write_file(dest, &bytes)
}

async fn curse_pick_file(
    mod_id: &str,
    mc: &str,
    loader: Option<&str>,
    file_id: Option<&str>,
) -> Result<(u64, String), AppError> {
    if let Some(fid) = file_id {
        let id_num: u64 = fid
            .parse()
            .map_err(|_| AppError::Message("Invalid CurseForge file id".into()))?;
        let body = curse_get(&format!("/mods/{mod_id}/files/{id_num}"), &[]).await?;
        let data = body
            .get("data")
            .ok_or_else(|| AppError::Message("CurseForge file missing".into()))?;
        let name = data
            .get("fileName")
            .and_then(|v| v.as_str())
            .unwrap_or("file.bin")
            .to_string();
        return Ok((id_num, name));
    }
    let mut params = vec![
        ("gameVersion", mc.to_string()),
        ("pageSize", "50".into()),
        ("sortField", "2".into()),
        ("sortOrder", "desc".into()),
    ];
    if let Some(l) = loader {
        let loader_type = match l {
            "forge" => 1,
            "quilt" => 5,
            _ => 4,
        };
        params.push(("modLoaderType", loader_type.to_string()));
    }
    let body = curse_get(&format!("/mods/{mod_id}/files"), &params).await?;
    let first = body
        .get("data")
        .and_then(|v| v.as_array())
        .and_then(|a| a.first())
        .ok_or_else(|| AppError::Message(format!("No compatible CurseForge file for Minecraft {mc}.")))?;
    let id_num = first
        .get("id")
        .and_then(|v| v.as_u64())
        .ok_or_else(|| AppError::Message("CurseForge file id missing".into()))?;
    let name = first
        .get("fileName")
        .and_then(|v| v.as_str())
        .unwrap_or("file.bin")
        .to_string();
    Ok((id_num, name))
}

pub async fn install_curseforge(
    project_id: String,
    title: String,
    instance_id: Option<String>,
    file_id: Option<String>,
    dest_kind: &str,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let inst = instances::get_stored(&id)?.ok_or_else(|| AppError::Message("No instance".into()))?;
    let loader = if dest_kind == "mod" {
        Some(inst.loader.as_str())
    } else {
        None
    };
    let (fid, filename) = curse_pick_file(
        &project_id,
        &inst.minecraft_version,
        loader,
        file_id.as_deref(),
    )
    .await?;
    let dest_dir = match dest_kind {
        "resourcepack" => paths::instance_game_dir(&id).join("resourcepacks"),
        "shader" => paths::instance_game_dir(&id).join("shaderpacks"),
        _ => paths::instance_mods_dir(&id),
    };
    fs::create_dir_all(&dest_dir)?;
    let dest = dest_dir.join(&filename);
    curse_download_file(&project_id, fid, &dest).await?;
    let _ = downloads::push_completed(&title, "CurseForge", "CurseForge");
    Ok(json!({ "ok": true, "fileName": filename }))
}

pub async fn list_versions(
    project_id: String,
    project_type: String,
    source: String,
    instance_id: Option<String>,
) -> Result<Vec<Value>, AppError> {
    let id = resolve_instance(instance_id).await?;
    let inst = instances::get_stored(&id)?.ok_or_else(|| AppError::Message("No instance".into()))?;
    let loader = if project_type == "mod" {
        Some(inst.loader.as_str())
    } else {
        None
    };

    if source == "curseforge" {
        let mut params = vec![
            ("gameVersion", inst.minecraft_version.clone()),
            ("pageSize", "50".into()),
            ("sortField", "2".into()),
            ("sortOrder", "desc".into()),
        ];
        if let Some(l) = loader {
            let loader_type = match l {
                "forge" => 1,
                "quilt" => 5,
                _ => 4,
            };
            params.push(("modLoaderType", loader_type.to_string()));
        }
        let body = curse_get(&format!("/mods/{project_id}/files"), &params).await?;
        let files = body.get("data").and_then(|v| v.as_array()).cloned().unwrap_or_default();
        return Ok(files
            .into_iter()
            .enumerate()
            .map(|(index, file)| {
                let file_name = file.get("fileName").and_then(|v| v.as_str()).unwrap_or("");
                json!({
                    "id": file.get("id").and_then(|v| v.as_u64()).unwrap_or(0).to_string(),
                    "versionNumber": file_name.trim_end_matches(".jar").trim_end_matches(".zip"),
                    "gameVersions": file.get("gameVersions").unwrap_or(&json!([])),
                    "loaders": file.get("modLoaders")
                        .and_then(|v| v.as_array())
                        .map(|arr| arr.iter().filter_map(|e| e.get("name").and_then(|n| n.as_str()).map(|s| s.to_lowercase())).collect::<Vec<_>>())
                        .unwrap_or_default(),
                    "fileName": file_name,
                    "recommended": index == 0
                })
            })
            .collect());
    }

    let mut url = format!(
        "https://api.modrinth.com/v2/project/{project_id}/version?game_versions=[\"{}\"]",
        inst.minecraft_version
    );
    if let Some(l) = loader {
        url.push_str(&format!("&loaders=[\"{l}\"]"));
    }
    let versions: Vec<Value> = client()?
        .get(url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    Ok(versions
        .into_iter()
        .enumerate()
        .map(|(index, version)| {
            let file = version
                .get("files")
                .and_then(|v| v.as_array())
                .and_then(|arr| {
                    arr.iter()
                        .find(|f| f.get("primary").and_then(|p| p.as_bool()).unwrap_or(false))
                        .or_else(|| arr.first())
                });
            json!({
                "id": version.get("id"),
                "versionNumber": version.get("version_number"),
                "gameVersions": version.get("game_versions").unwrap_or(&json!([])),
                "loaders": version.get("loaders").unwrap_or(&json!([])),
                "fileName": file.and_then(|f| f.get("filename")),
                "recommended": index == 0
            })
        })
        .collect())
}

pub async fn list_resource_packs(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    let id = resolve_instance(instance_id).await?;
    let dir = paths::instance_game_dir(&id).join("resourcepacks");
    fs::create_dir_all(&dir)?;
    let active = options_txt::get_active_resource_pack(&id)?;
    let mut out = vec![];
    if let Ok(rd) = fs::read_dir(dir) {
        for e in rd.flatten() {
            let name = e.file_name().to_string_lossy().to_string();
            if !name.ends_with(".zip") {
                continue;
            }
            out.push(json!({
                "id": urlencoding::encode(&name),
                "fileName": name,
                "name": display_name(&name, None),
                "description": "Local resource pack",
                "resolution": "Mixed",
                "active": active.as_deref() == Some(name.as_str())
            }));
        }
    }
    Ok(out)
}

pub async fn set_resource_pack_active(
    file_name: Option<String>,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    options_txt::set_active_resource_pack(&id, file_name.as_deref())?;
    Ok(json!({ "ok": true }))
}

pub async fn remove_resource_pack(
    file_name: String,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    if options_txt::get_active_resource_pack(&id)?.as_deref() == Some(file_name.as_str()) {
        options_txt::set_active_resource_pack(&id, None)?;
    }
    let path = paths::instance_game_dir(&id)
        .join("resourcepacks")
        .join(&file_name);
    let _ = fs::remove_file(path);
    Ok(json!({ "ok": true }))
}

pub async fn import_resource_pack(
    app: AppHandle,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let Some(src) = pick_file(&app, vec![("Resource Packs", &["zip"])])? else {
        return Ok(json!({ "ok": false, "error": "cancelled" }));
    };
    let name = src
        .file_name()
        .and_then(|n| n.to_str())
        .ok_or_else(|| AppError::Message("Invalid file".into()))?
        .to_string();
    if !name.ends_with(".zip") {
        return Ok(json!({ "ok": false, "error": "Resource packs must be .zip files." }));
    }
    let dest = paths::instance_game_dir(&id).join("resourcepacks").join(&name);
    fs::create_dir_all(dest.parent().unwrap())?;
    fs::copy(&src, &dest)?;
    Ok(json!({ "ok": true, "fileName": name }))
}

pub async fn list_shaders(instance_id: Option<String>) -> Result<Vec<Value>, AppError> {
    let id = resolve_instance(instance_id).await?;
    let dir = paths::instance_game_dir(&id).join("shaderpacks");
    fs::create_dir_all(&dir)?;
    let active = options_txt::get_active_shader(&id)?;
    let mut out = vec![];
    if let Ok(rd) = fs::read_dir(dir) {
        for e in rd.flatten() {
            let name = e.file_name().to_string_lossy().to_string();
            if !name.ends_with(".zip") {
                continue;
            }
            out.push(json!({
                "id": urlencoding::encode(&name),
                "fileName": name,
                "name": display_name(&name, None),
                "description": "Local shader pack",
                "backend": "iris",
                "active": active.as_deref() == Some(name.as_str())
            }));
        }
    }
    Ok(out)
}

pub async fn set_shader_active(
    file_name: Option<String>,
    instance_id: Option<String>,
) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    options_txt::set_active_shader(&id, file_name.as_deref())?;
    Ok(json!({ "ok": true }))
}

pub async fn remove_shader(file_name: String, instance_id: Option<String>) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    if options_txt::get_active_shader(&id)?.as_deref() == Some(file_name.as_str()) {
        options_txt::set_active_shader(&id, None)?;
    }
    let path = paths::instance_game_dir(&id)
        .join("shaderpacks")
        .join(&file_name);
    let _ = fs::remove_file(path);
    Ok(json!({ "ok": true }))
}

pub async fn import_shader(app: AppHandle, instance_id: Option<String>) -> Result<Value, AppError> {
    let id = resolve_instance(instance_id).await?;
    let Some(src) = pick_file(&app, vec![("Shader Packs", &["zip"])])? else {
        return Ok(json!({ "ok": false, "error": "cancelled" }));
    };
    let name = src
        .file_name()
        .and_then(|n| n.to_str())
        .ok_or_else(|| AppError::Message("Invalid file".into()))?
        .to_string();
    if !name.ends_with(".zip") {
        return Ok(json!({ "ok": false, "error": "Shader packs must be .zip files." }));
    }
    let dest = paths::instance_game_dir(&id).join("shaderpacks").join(&name);
    fs::create_dir_all(dest.parent().unwrap())?;
    fs::copy(&src, &dest)?;
    Ok(json!({ "ok": true, "fileName": name }))
}
