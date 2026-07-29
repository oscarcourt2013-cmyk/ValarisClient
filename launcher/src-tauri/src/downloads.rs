use crate::error::AppError;
use crate::paths;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::fs;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DownloadTask {
    pub id: String,
    pub name: String,
    pub progress: f64,
    pub speed: String,
    pub size: String,
    pub eta: String,
    pub status: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct DownloadDb {
    version: u32,
    tasks: Vec<DownloadTask>,
}

fn load() -> Result<DownloadDb, AppError> {
    let path = paths::user_data_dir().join("downloads.json");
    if !path.exists() {
        return Ok(DownloadDb {
            version: 1,
            tasks: vec![],
        });
    }
    Ok(serde_json::from_str(&fs::read_to_string(path)?).unwrap_or_default())
}

fn save(db: &DownloadDb) -> Result<(), AppError> {
    let path = paths::user_data_dir().join("downloads.json");
    fs::create_dir_all(paths::user_data_dir())?;
    fs::write(path, serde_json::to_string_pretty(db)?)?;
    Ok(())
}

pub fn list() -> Result<Vec<DownloadTask>, AppError> {
    let mut db = load()?;
    db.tasks.sort_by(|a, b| {
        let sa = if a.status == "downloading" { 0 } else { 1 };
        let sb = if b.status == "downloading" { 0 } else { 1 };
        sa.cmp(&sb)
    });
    Ok(db.tasks)
}

pub fn clear_completed() -> Result<Value, AppError> {
    let mut db = load()?;
    db.tasks.retain(|t| t.status != "completed");
    save(&db)?;
    Ok(json!({ "ok": true }))
}

pub fn remove(id: String) -> Result<Value, AppError> {
    let mut db = load()?;
    db.tasks.retain(|t| t.id != id);
    save(&db)?;
    Ok(json!({ "ok": true }))
}

pub fn push_completed(name: &str, size: &str, speed: &str) -> Result<(), AppError> {
    let mut db = load()?;
    db.tasks.insert(
        0,
        DownloadTask {
            id: Uuid::new_v4().to_string(),
            name: name.into(),
            progress: 100.0,
            speed: speed.into(),
            size: size.into(),
            eta: "—".into(),
            status: "completed".into(),
        },
    );
    if db.tasks.len() > 100 {
        db.tasks.truncate(100);
    }
    save(&db)
}
