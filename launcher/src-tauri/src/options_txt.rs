use crate::error::AppError;
use crate::paths;
use std::fs;

fn options_path(instance_id: &str) -> std::path::PathBuf {
    paths::instance_game_dir(instance_id).join("options.txt")
}

pub fn read_lines(instance_id: &str) -> Result<Vec<String>, AppError> {
    let path = options_path(instance_id);
    if !path.exists() {
        return Ok(vec![]);
    }
    Ok(fs::read_to_string(path)?.lines().map(str::to_string).collect())
}

pub fn write_lines(instance_id: &str, lines: &[String]) -> Result<(), AppError> {
    let path = options_path(instance_id);
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }
    fs::write(path, lines.join("\n"))?;
    Ok(())
}

pub fn get_value(lines: &[String], key: &str) -> Option<String> {
    let prefix = format!("{key}:");
    lines
        .iter()
        .find(|l| l.starts_with(&prefix))
        .map(|l| l[prefix.len()..].to_string())
}

pub fn set_value(lines: &mut Vec<String>, key: &str, value: &str) {
    let prefix = format!("{key}:");
    for line in lines.iter_mut() {
        if line.starts_with(&prefix) {
            *line = format!("{prefix}{value}");
            return;
        }
    }
    lines.push(format!("{prefix}{value}"));
}

fn parse_resource_packs(raw: Option<&str>) -> Vec<String> {
    let Some(raw) = raw else {
        return vec!["vanilla".into()];
    };
    serde_json::from_str::<Vec<String>>(raw).unwrap_or_else(|_| vec!["vanilla".into()])
}

pub fn get_active_resource_pack(instance_id: &str) -> Result<Option<String>, AppError> {
    let lines = read_lines(instance_id)?;
    let packs = parse_resource_packs(get_value(&lines, "resourcePacks").as_deref());
    let last = packs.last().cloned();
    Ok(match last.as_deref() {
        None | Some("vanilla") => None,
        Some(s) if s.starts_with("file/") => Some(s["file/".len()..].to_string()),
        Some(s) => Some(s.to_string()),
    })
}

pub fn set_active_resource_pack(instance_id: &str, file_name: Option<&str>) -> Result<(), AppError> {
    let mut lines = read_lines(instance_id)?;
    let current = parse_resource_packs(get_value(&lines, "resourcePacks").as_deref());
    let mut next: Vec<String> = current
        .into_iter()
        .filter(|p| p == "vanilla" || !p.starts_with("file/"))
        .collect();
    if let Some(name) = file_name {
        next.push(format!("file/{name}"));
    }
    set_value(
        &mut lines,
        "resourcePacks",
        &serde_json::to_string(&next).unwrap_or_else(|_| "[\"vanilla\"]".into()),
    );
    write_lines(instance_id, &lines)
}

pub fn get_active_shader(instance_id: &str) -> Result<Option<String>, AppError> {
    let lines = read_lines(instance_id)?;
    let value = get_value(&lines, "shaderPack");
    Ok(match value.as_deref() {
        None | Some("OFF") | Some("\"\"") => None,
        Some(v) => Some(v.trim_matches('"').to_string()),
    })
}

pub fn set_active_shader(instance_id: &str, file_name: Option<&str>) -> Result<(), AppError> {
    let mut lines = read_lines(instance_id)?;
    let value = file_name
        .map(|n| format!("\"{n}\""))
        .unwrap_or_else(|| "OFF".into());
    set_value(&mut lines, "shaderPack", &value);
    write_lines(instance_id, &lines)
}
