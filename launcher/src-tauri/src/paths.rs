use std::path::PathBuf;

/// Same folder as Electron: `%APPDATA%/stellar-client-launcher` on Windows.
pub fn user_data_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("stellar-client-launcher")
}

pub fn settings_path() -> PathBuf {
    user_data_dir().join("settings.json")
}

pub fn accounts_path() -> PathBuf {
    user_data_dir().join("accounts.json")
}

pub fn instances_path() -> PathBuf {
    user_data_dir().join("instances.json")
}

pub fn ecosystem_path() -> PathBuf {
    user_data_dir().join("ecosystem.json")
}

pub fn runtime_root() -> PathBuf {
    user_data_dir().join("runtime").join("minecraft")
}

pub fn java_runtime_dir(version: u32) -> PathBuf {
    user_data_dir().join("runtime").join(format!("jre-{version}"))
}

pub fn instance_root(id: &str) -> PathBuf {
    user_data_dir().join("instances").join(id)
}

pub fn instance_game_dir(id: &str) -> PathBuf {
    instance_root(id).join("game")
}

pub fn instance_mods_dir(id: &str) -> PathBuf {
    instance_game_dir(id).join("mods")
}

pub fn logs_dir() -> PathBuf {
    user_data_dir().join("logs")
}

pub fn cache_dir() -> PathBuf {
    user_data_dir().join("cache")
}
