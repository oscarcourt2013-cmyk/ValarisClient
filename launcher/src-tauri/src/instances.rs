use crate::error::AppError;
use crate::minecraft_targets::{resolve_target, DEFAULT_TARGET};
use crate::paths;
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::fs;
use uuid::Uuid;
use walkdir::WalkDir;

const DEFAULT_FABRIC_LOADER: &str = DEFAULT_TARGET.fabric_loader;
const DEFAULT_FABRIC_API: &str = DEFAULT_TARGET.fabric_api;
const DEFAULT_MC_VERSION: &str = DEFAULT_TARGET.mc_version;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StoredInstance {
    pub id: String,
    pub name: String,
    pub minecraft_version: String,
    pub loader: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fabric_loader_version: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fabric_api_version: Option<String>,
    pub include_prime_mod: bool,
    pub ram_mb: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub java_path: Option<String>,
    pub jvm_args: Vec<String>,
    pub is_default: bool,
    pub created_at: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_played: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct InstanceDatabase {
    version: u32,
    instances: Vec<StoredInstance>,
}

impl Default for InstanceDatabase {
    fn default() -> Self {
        Self {
            version: 1,
            instances: vec![StoredInstance {
                id: "prime-fabric".into(),
                name: "StellarClient".into(),
                minecraft_version: DEFAULT_MC_VERSION.into(),
                loader: "fabric".into(),
                fabric_loader_version: Some(DEFAULT_FABRIC_LOADER.into()),
                fabric_api_version: Some(DEFAULT_FABRIC_API.into()),
                include_prime_mod: true,
                ram_mb: 4096,
                java_path: None,
                jvm_args: vec!["-XX:+UseG1GC".into()],
                is_default: true,
                created_at: Utc::now().to_rfc3339(),
                last_played: None,
            }],
        }
    }
}

fn load_db() -> Result<InstanceDatabase, AppError> {
    let path = paths::instances_path();
    if !path.exists() {
        let db = InstanceDatabase::default();
        save_db(&db)?;
        return Ok(db);
    }
    let raw = fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&raw).unwrap_or_default())
}

fn save_db(db: &InstanceDatabase) -> Result<(), AppError> {
    let path = paths::instances_path();
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }
    fs::write(path, serde_json::to_string_pretty(db)?)?;
    Ok(())
}

fn clamp_ram(ram: u32) -> u32 {
    ram.clamp(512, 16384)
}

fn count_mods(id: &str) -> u32 {
    let dir = paths::instance_mods_dir(id);
    if !dir.exists() {
        return 0;
    }
    WalkDir::new(dir)
        .max_depth(1)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().and_then(|x| x.to_str()) == Some("jar"))
        .count() as u32
}

fn to_public(stored: &StoredInstance) -> serde_json::Value {
    json!({
        "id": stored.id,
        "name": stored.name,
        "minecraftVersion": stored.minecraft_version,
        "loader": stored.loader,
        "ramMb": stored.ram_mb,
        "javaPath": stored.java_path,
        "jvmArgs": stored.jvm_args,
        "modCount": count_mods(&stored.id),
        "isDefault": stored.is_default,
        "includePrimeMod": stored.include_prime_mod,
        "fabricLoaderVersion": stored.fabric_loader_version,
        "fabricApiVersion": stored.fabric_api_version,
        "createdAt": stored.created_at,
        "lastPlayed": stored.last_played,
    })
}

pub fn list() -> Result<Vec<serde_json::Value>, AppError> {
    Ok(load_db()?.instances.iter().map(to_public).collect())
}

pub fn get(id: &str) -> Result<Option<serde_json::Value>, AppError> {
    Ok(load_db()?
        .instances
        .iter()
        .find(|i| i.id == id)
        .map(to_public))
}

pub fn get_stored(id: &str) -> Result<Option<StoredInstance>, AppError> {
    Ok(load_db()?.instances.into_iter().find(|i| i.id == id))
}

pub fn get_default() -> Result<Option<serde_json::Value>, AppError> {
    let db = load_db()?;
    Ok(db
        .instances
        .iter()
        .find(|i| i.is_default)
        .or_else(|| db.instances.first())
        .map(to_public))
}

pub fn create(input: serde_json::Value) -> Result<serde_json::Value, AppError> {
    let name = input
        .get("name")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|s| !s.is_empty() && s.len() <= 32)
        .ok_or_else(|| AppError::Message("Name must be 1â€“32 characters.".into()))?;
    let loader = input
        .get("loader")
        .and_then(|v| v.as_str())
        .unwrap_or("fabric");
    if loader != "vanilla" && loader != "fabric" {
        return Ok(json!({ "ok": false, "error": "Only Vanilla and Fabric are supported." }));
    }
    let mut db = load_db()?;
    let include_prime = input
        .get("includePrimeMod")
        .and_then(|v| v.as_bool())
        .unwrap_or(loader == "fabric");
    let mc_version = input
        .get("minecraftVersion")
        .and_then(|v| v.as_str())
        .unwrap_or(DEFAULT_MC_VERSION)
        .to_string();
    let target = resolve_target(&mc_version);
    let stored = StoredInstance {
        id: Uuid::new_v4().to_string(),
        name: name.to_string(),
        minecraft_version: mc_version,
        loader: loader.to_string(),
        fabric_loader_version: if loader == "fabric" {
            Some(
                input
                    .get("fabricLoaderVersion")
                    .and_then(|v| v.as_str())
                    .unwrap_or(target.fabric_loader)
                    .to_string(),
            )
        } else {
            None
        },
        fabric_api_version: if loader == "fabric" {
            if let Some(v) = input.get("fabricApiVersion").and_then(|v| v.as_str()) {
                Some(v.to_string())
            } else if include_prime {
                Some(target.fabric_api.to_string())
            } else {
                None
            }
        } else {
            None
        },
        include_prime_mod: include_prime && loader == "fabric",
        ram_mb: clamp_ram(input.get("ramMb").and_then(|v| v.as_u64()).unwrap_or(4096) as u32),
        java_path: None,
        jvm_args: input
            .get("jvmArgs")
            .and_then(|v| v.as_array())
            .map(|a| {
                a.iter()
                    .filter_map(|x| x.as_str().map(str::to_string))
                    .collect()
            })
            .unwrap_or_else(|| {
                if loader == "fabric" {
                    vec!["-XX:+UseG1GC".into()]
                } else {
                    vec![]
                }
            }),
        is_default: db.instances.is_empty(),
        created_at: Utc::now().to_rfc3339(),
        last_played: None,
    };
    let id = stored.id.clone();
    fs::create_dir_all(paths::instance_game_dir(&id))?;
    fs::create_dir_all(paths::instance_mods_dir(&id))?;
    db.instances.push(stored);
    save_db(&db)?;
    Ok(json!({ "ok": true, "id": id }))
}

pub fn update(input: serde_json::Value) -> Result<serde_json::Value, AppError> {
    let id = input
        .get("id")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("id required".into()))?;
    let mut db = load_db()?;
    let Some(inst) = db.instances.iter_mut().find(|i| i.id == id) else {
        return Ok(json!({ "ok": false, "error": "Instance not found." }));
    };
    if let Some(name) = input.get("name").and_then(|v| v.as_str()) {
        let trimmed = name.trim();
        if trimmed.is_empty() || trimmed.len() > 32 {
            return Ok(json!({ "ok": false, "error": "Name must be 1â€“32 characters." }));
        }
        inst.name = trimmed.to_string();
    }
    if let Some(v) = input.get("minecraftVersion").and_then(|v| v.as_str()) {
        inst.minecraft_version = v.to_string();
        if inst.include_prime_mod && input.get("fabricApiVersion").is_none() {
            let target = resolve_target(v);
            inst.fabric_api_version = Some(target.fabric_api.into());
            if inst.fabric_loader_version.is_none() {
                inst.fabric_loader_version = Some(target.fabric_loader.into());
            }
        }
    }
    if let Some(v) = input.get("loader").and_then(|v| v.as_str()) {
        inst.loader = v.to_string();
    }
    if let Some(v) = input.get("fabricLoaderVersion").and_then(|v| v.as_str()) {
        inst.fabric_loader_version = Some(v.to_string());
    }
    if let Some(v) = input.get("fabricApiVersion").and_then(|v| v.as_str()) {
        inst.fabric_api_version = Some(v.to_string());
    }
    if let Some(v) = input.get("includePrimeMod").and_then(|v| v.as_bool()) {
        inst.include_prime_mod = v;
    }
    if let Some(v) = input.get("ramMb").and_then(|v| v.as_u64()) {
        inst.ram_mb = clamp_ram(v as u32);
    }
    if let Some(v) = input.get("javaPath") {
        inst.java_path = v.as_str().map(str::to_string);
    }
    if let Some(arr) = input.get("jvmArgs").and_then(|v| v.as_array()) {
        inst.jvm_args = arr
            .iter()
            .filter_map(|x| x.as_str().map(str::to_string))
            .collect();
    }
    save_db(&db)?;
    Ok(json!({ "ok": true }))
}

pub fn remove(id: &str, delete_files: bool) -> Result<serde_json::Value, AppError> {
    let mut db = load_db()?;
    if db.instances.len() <= 1 {
        return Ok(json!({ "ok": false, "error": "Cannot delete the last instance." }));
    }
    let was_default = db
        .instances
        .iter()
        .find(|i| i.id == id)
        .map(|i| i.is_default)
        .unwrap_or(false);
    let before = db.instances.len();
    db.instances.retain(|i| i.id != id);
    if db.instances.len() == before {
        return Ok(json!({ "ok": false, "error": "Instance not found." }));
    }
    if was_default {
        if let Some(first) = db.instances.first_mut() {
            first.is_default = true;
        }
    }
    save_db(&db)?;
    if delete_files {
        let dir = paths::instance_root(id);
        let _ = fs::remove_dir_all(dir);
    }
    Ok(json!({ "ok": true }))
}

pub fn duplicate(id: &str) -> Result<serde_json::Value, AppError> {
    let mut db = load_db()?;
    let Some(src) = db.instances.iter().find(|i| i.id == id).cloned() else {
        return Ok(json!({ "ok": false, "error": "Instance not found." }));
    };
    let mut copy = src;
    copy.id = Uuid::new_v4().to_string();
    copy.name = format!("{} Copy", copy.name);
    if copy.name.len() > 32 {
        copy.name = copy.name.chars().take(32).collect();
    }
    copy.is_default = false;
    copy.created_at = Utc::now().to_rfc3339();
    copy.last_played = None;
    let new_id = copy.id.clone();
    fs::create_dir_all(paths::instance_game_dir(&new_id))?;
    fs::create_dir_all(paths::instance_mods_dir(&new_id))?;
    db.instances.push(copy);
    save_db(&db)?;
    Ok(json!({ "ok": true, "id": new_id }))
}

pub fn set_default(id: &str) -> Result<serde_json::Value, AppError> {
    let mut db = load_db()?;
    if !db.instances.iter().any(|i| i.id == id) {
        return Ok(json!({ "ok": false, "error": "Instance not found." }));
    }
    for i in &mut db.instances {
        i.is_default = i.id == id;
    }
    save_db(&db)?;
    Ok(json!({ "ok": true }))
}

pub fn mark_played(id: &str) -> Result<(), AppError> {
    let mut db = load_db()?;
    if let Some(i) = db.instances.iter_mut().find(|i| i.id == id) {
        i.last_played = Some(Utc::now().to_rfc3339());
        save_db(&db)?;
    }
    Ok(())
}

pub fn open_folder(id: &str) -> Result<(), AppError> {
    let dir = paths::instance_game_dir(id);
    fs::create_dir_all(&dir)?;
    open::that(&dir).map_err(|e| AppError::Message(e.to_string()))
}
