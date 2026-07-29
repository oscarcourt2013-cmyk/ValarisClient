use crate::error::{AppError, OkResult};
use crate::paths;
use chrono::Utc;
use md5::{Digest, Md5};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::fs;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StoredMinecraftAccount {
    pub id: String,
    #[serde(rename = "type")]
    pub account_type: String,
    pub username: String,
    pub uuid: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skin_url: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cape_url: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ms_refresh_token: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ms_auth_provider: Option<String>,
    pub added_at: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_used_at: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PrimeAccount {
    pub id: String,
    pub username: String,
    pub tier: String,
    pub level: u32,
    pub created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LauncherProfile {
    pub id: String,
    pub name: String,
    pub minecraft_account_id: String,
    pub instance_id: String,
    pub play_time_minutes: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountDatabase {
    pub version: u32,
    pub active_account_id: Option<String>,
    pub active_profile_id: String,
    pub accounts: Vec<StoredMinecraftAccount>,
    pub prime_account: PrimeAccount,
    pub profiles: Vec<LauncherProfile>,
}

impl Default for AccountDatabase {
    fn default() -> Self {
        Self {
            version: 1,
            active_account_id: None,
            active_profile_id: "default".into(),
            accounts: vec![],
            prime_account: PrimeAccount {
                id: "prime-local".into(),
                username: "Guest".into(),
                tier: "free".into(),
                level: 1,
                created_at: Utc::now().to_rfc3339(),
            },
            profiles: vec![LauncherProfile {
                id: "default".into(),
                name: "Default".into(),
                minecraft_account_id: String::new(),
                instance_id: "prime-fabric".into(),
                play_time_minutes: 0,
            }],
        }
    }
}

pub fn load() -> Result<AccountDatabase, AppError> {
    let path = paths::accounts_path();
    if !path.exists() {
        let db = AccountDatabase::default();
        save(&db)?;
        return Ok(db);
    }
    let raw = fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&raw).unwrap_or_default())
}

pub fn save(db: &AccountDatabase) -> Result<(), AppError> {
    let path = paths::accounts_path();
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }
    fs::write(path, serde_json::to_string_pretty(db)?)?;
    Ok(())
}

fn offline_uuid(username: &str) -> String {
    // Minecraft offline UUID: MD5("OfflinePlayer:" + name) with version/variant bits
    let mut hasher = Md5::new();
    hasher.update(format!("OfflinePlayer:{username}").as_bytes());
    let mut bytes = hasher.finalize();
    bytes[6] = (bytes[6] & 0x0f) | 0x30;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    format!(
        "{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",
        bytes[0], bytes[1], bytes[2], bytes[3],
        bytes[4], bytes[5], bytes[6], bytes[7],
        bytes[8], bytes[9], bytes[10], bytes[11],
        bytes[12], bytes[13], bytes[14], bytes[15]
    )
}

fn skin_url(uuid: &str) -> String {
    format!("https://mc-heads.net/avatar/{}/64", uuid.replace('-', ""))
}

fn to_public(account: &StoredMinecraftAccount) -> serde_json::Value {
    json!({
        "id": account.id,
        "type": account.account_type,
        "username": account.username,
        "uuid": account.uuid,
        "skinUrl": account.skin_url.clone().unwrap_or_else(|| skin_url(&account.uuid)),
        "capeUrl": account.cape_url,
    })
}

pub fn get_prime() -> Result<PrimeAccount, AppError> {
    Ok(load()?.prime_account)
}

pub fn get_minecraft() -> Result<Vec<serde_json::Value>, AppError> {
    Ok(load()?.accounts.iter().map(to_public).collect())
}

pub fn get_active() -> Result<Option<serde_json::Value>, AppError> {
    let db = load()?;
    Ok(db
        .active_account_id
        .as_ref()
        .and_then(|id| db.accounts.iter().find(|a| &a.id == id))
        .map(to_public))
}

pub fn get_active_stored() -> Result<Option<StoredMinecraftAccount>, AppError> {
    let db = load()?;
    Ok(db
        .active_account_id
        .as_ref()
        .and_then(|id| db.accounts.iter().find(|a| &a.id == id))
        .cloned())
}

pub fn set_active(account_id: String) -> Result<Option<serde_json::Value>, AppError> {
    let mut db = load()?;
    let Some(account) = db.accounts.iter_mut().find(|a| a.id == account_id) else {
        return Err(AppError::Message("Account not found.".into()));
    };
    account.last_used_at = Some(Utc::now().to_rfc3339());
    db.active_account_id = Some(account_id.clone());
    db.prime_account.username = account.username.clone();
    db.prime_account.tier = if account.account_type == "microsoft" {
        "prime".into()
    } else {
        "free".into()
    };
    if let Some(profile) = db.profiles.iter_mut().find(|p| p.id == db.active_profile_id) {
        profile.minecraft_account_id = account_id;
    }
    let public = to_public(account);
    save(&db)?;
    Ok(Some(public))
}

pub fn add_offline(username: String) -> Result<OkResult, AppError> {
    let trimmed = username.trim().to_string();
    if trimmed.len() < 3 || trimmed.len() > 16 || !trimmed.chars().all(|c| c.is_ascii_alphanumeric() || c == '_') {
        return Ok(OkResult::err("Invalid offline username."));
    }
    let mut db = load()?;
    if db.accounts.iter().any(|a| a.username.eq_ignore_ascii_case(&trimmed)) {
        return Ok(OkResult::err("Account already exists."));
    }
    let id = Uuid::new_v4().to_string();
    let uuid = offline_uuid(&trimmed);
    let account = StoredMinecraftAccount {
        id: id.clone(),
        account_type: "offline".into(),
        username: trimmed.clone(),
        uuid: uuid.clone(),
        skin_url: Some(skin_url(&uuid)),
        cape_url: None,
        ms_refresh_token: None,
        ms_auth_provider: None,
        added_at: Utc::now().to_rfc3339(),
        last_used_at: Some(Utc::now().to_rfc3339()),
    };
    db.active_account_id = Some(id.clone());
    db.prime_account.username = trimmed;
    db.prime_account.tier = "free".into();
    if let Some(profile) = db.profiles.iter_mut().find(|p| p.id == db.active_profile_id) {
        profile.minecraft_account_id = id.clone();
    }
    db.accounts.push(account);
    save(&db)?;
    Ok(OkResult {
        ok: true,
        error: None,
        message: None,
        account_id: Some(id),
    })
}

pub fn remove(account_id: String) -> Result<OkResult, AppError> {
    let mut db = load()?;
    let before = db.accounts.len();
    db.accounts.retain(|a| a.id != account_id);
    if db.accounts.len() == before {
        return Ok(OkResult::err("Account not found."));
    }
    if db.active_account_id.as_deref() == Some(&account_id) {
        db.active_account_id = db.accounts.first().map(|a| a.id.clone());
    }
    save(&db)?;
    Ok(OkResult::ok())
}

pub fn login_microsoft() -> Result<OkResult, AppError> {
    crate::microsoft::login_interactive()
}

pub fn refresh_microsoft(account_id: String) -> Result<OkResult, AppError> {
    crate::microsoft::refresh_account(&account_id)
}

pub fn sync_prime() -> Result<serde_json::Value, AppError> {
    let _db = load()?;
    Ok(json!({
        "ok": true,
        "lastSync": Utc::now().to_rfc3339(),
        "message": "Local sync only — no cloud server"
    }))
}

pub fn get_active_profile() -> Result<Option<LauncherProfile>, AppError> {
    let db = load()?;
    Ok(db.profiles.into_iter().find(|p| p.id == db.active_profile_id))
}

pub fn get_all_profiles() -> Result<Vec<LauncherProfile>, AppError> {
    Ok(load()?.profiles)
}

pub fn set_instance(instance_id: String) -> Result<(), AppError> {
    let mut db = load()?;
    if let Some(profile) = db.profiles.iter_mut().find(|p| p.id == db.active_profile_id) {
        profile.instance_id = instance_id;
    }
    save(&db)?;
    Ok(())
}
