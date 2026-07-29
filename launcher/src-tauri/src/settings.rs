use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::fs;
use std::io;

use crate::error::AppError;
use crate::paths;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LauncherSettings {
    pub version: u32,
    pub language: String,
    pub close_on_launch: bool,
    pub auto_update: bool,
    pub theme: String,
    pub background_nebula: bool,
    pub hardware_accel: bool,
    pub default_ram_mb: u32,
    pub performance_preset: String,
    pub analytics: bool,
    pub discord_rpc: bool,
    pub concurrent_downloads: u32,
    pub developer_mode: bool,
    pub jvm_args: Vec<String>,
    pub default_java_path: Option<String>,
    pub custom_java_paths: Vec<String>,
    pub game_width: u32,
    pub game_height: u32,
    pub game_display_mode: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_update_check: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_prime_sync: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub curse_forge_api_key: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_server_address: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dismissed_update_banner: Option<String>,
}

impl Default for LauncherSettings {
    fn default() -> Self {
        Self {
            version: 1,
            language: "en".into(),
            close_on_launch: false,
            auto_update: true,
            theme: "prime-crimson".into(),
            background_nebula: true,
            hardware_accel: true,
            default_ram_mb: 4096,
            performance_preset: "balanced".into(),
            analytics: false,
            discord_rpc: true,
            concurrent_downloads: 4,
            developer_mode: false,
            jvm_args: vec![],
            default_java_path: None,
            custom_java_paths: vec![],
            game_width: 854,
            game_height: 480,
            game_display_mode: "windowed".into(),
            last_update_check: None,
            last_prime_sync: None,
            curse_forge_api_key: None,
            last_server_address: None,
            dismissed_update_banner: None,
        }
    }
}

pub fn load() -> Result<LauncherSettings, AppError> {
    let path = paths::settings_path();
    if !path.exists() {
        let defaults = LauncherSettings::default();
        save(&defaults)?;
        return Ok(defaults);
    }
    let raw = fs::read_to_string(&path)?;
    // Accept Electron camelCase JSON via serde rename
    let mut settings: LauncherSettings = serde_json::from_str(&raw).unwrap_or_default();
    settings.theme = normalize_theme_id(&settings.theme);
    Ok(settings)
}

pub fn normalize_theme_id(id: &str) -> String {
    match id {
        "prime-midnight" | "prime-aurora" | "prime-crimson" => id.into(),
        "prime-light" => "prime-midnight".into(),
        _ => "prime-crimson".into(),
    }
}

pub fn save(settings: &LauncherSettings) -> Result<(), AppError> {
    let path = paths::settings_path();
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }
    let json = serde_json::to_string_pretty(settings)?;
    fs::write(path, json)?;
    Ok(())
}

pub fn update_merge(patch: Value) -> Result<LauncherSettings, AppError> {
    let mut current = serde_json::to_value(load()?)?;
    if let (Value::Object(base), Value::Object(p)) = (&mut current, patch) {
        for (k, v) in p {
            base.insert(k, v);
        }
    }
    let mut settings: LauncherSettings = serde_json::from_value(current)?;
    settings.theme = normalize_theme_id(&settings.theme);
    save(&settings)?;
    Ok(settings)
}

impl From<io::Error> for AppError {
    fn from(value: io::Error) -> Self {
        AppError::Io(value.to_string())
    }
}

impl From<serde_json::Error> for AppError {
    fn from(value: serde_json::Error) -> Self {
        AppError::Json(value.to_string())
    }
}
