use crate::error::AppError;
use crate::paths;
use parking_lot::Mutex;
use serde_json::{json, Value};
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::sync::OnceLock;
use uuid::Uuid;

#[derive(Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LogEntry {
    pub id: String,
    pub timestamp: String,
    pub level: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase: Option<String>,
    pub message: String,
}

struct LogState {
    entries: Vec<LogEntry>,
}

fn state() -> &'static Mutex<LogState> {
    static STATE: OnceLock<Mutex<LogState>> = OnceLock::new();
    STATE.get_or_init(|| Mutex::new(LogState { entries: vec![] }))
}

fn append_disk(level: &str, message: &str) {
    let dir = paths::logs_dir();
    let _ = fs::create_dir_all(&dir);
    let path = dir.join("launch.log");
    if let Ok(mut f) = OpenOptions::new().create(true).append(true).open(path) {
        let ts = chrono::Local::now().format("%Y-%m-%d %H:%M:%S");
        let _ = writeln!(f, "[{ts}] [{level}] {message}");
    }
}

pub fn append(level: &str, message: &str, phase: Option<&str>) -> LogEntry {
    let entry = LogEntry {
        id: Uuid::new_v4().to_string(),
        timestamp: chrono::Utc::now().to_rfc3339(),
        level: level.into(),
        phase: phase.map(str::to_string),
        message: message.into(),
    };
    append_disk(level, message);
    let mut g = state().lock();
    g.entries.push(entry.clone());
    if g.entries.len() > 2000 {
        let drain = g.entries.len() - 2000;
        g.entries.drain(0..drain);
    }
    entry
}

pub fn list() -> Vec<LogEntry> {
    state().lock().entries.clone()
}

pub fn clear() -> Result<Value, AppError> {
    state().lock().entries.clear();
    let path = paths::logs_dir().join("launch.log");
    let _ = fs::write(path, "");
    Ok(json!({ "ok": true }))
}

pub fn open_folder() -> Result<(), AppError> {
    let dir = paths::logs_dir();
    fs::create_dir_all(&dir)?;
    open::that(dir).map_err(|e| AppError::Message(e.to_string()))
}

pub fn open_crash(path: String) -> Result<(), AppError> {
    open::that(path).map_err(|e| AppError::Message(e.to_string()))
}
