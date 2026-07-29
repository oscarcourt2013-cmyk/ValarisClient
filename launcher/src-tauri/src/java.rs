use crate::error::AppError;
use crate::settings;
use serde_json::{json, Value};
use std::path::{Path, PathBuf};
use std::process::Command;

#[derive(Clone)]
pub struct JavaInstall {
    pub path: String,
    pub major: u32,
    pub label: String,
}

fn parse_major(version_line: &str) -> Option<u32> {
    // version "21.0.1" or version "1.8.0_402"
    let start = version_line.find('"')? + 1;
    let end = version_line[start..].find('"')? + start;
    let ver = &version_line[start..end];
    if let Some(rest) = ver.strip_prefix("1.") {
        return rest.split(|c: char| !c.is_ascii_digit()).next()?.parse().ok();
    }
    ver.split(|c: char| !c.is_ascii_digit()).next()?.parse().ok()
}

fn probe(java: &Path) -> Option<JavaInstall> {
    let out = Command::new(java).arg("-version").output().ok()?;
    let text = String::from_utf8_lossy(&out.stderr);
    let line = text.lines().next()?;
    let major = parse_major(line)?;
    if major < 21 {
        return None;
    }
    Some(JavaInstall {
        path: java.to_string_lossy().to_string(),
        major,
        label: format!("Java {major} — {}", java.display()),
    })
}

fn push_unique(out: &mut Vec<JavaInstall>, inst: JavaInstall) {
    if out.iter().any(|j| j.path == inst.path) {
        return;
    }
    out.push(inst);
}

fn scan_dir(root: &Path, out: &mut Vec<JavaInstall>) {
    let bin = if cfg!(windows) {
        root.join("bin").join("java.exe")
    } else {
        root.join("bin").join("java")
    };
    if let Some(j) = probe(&bin) {
        push_unique(out, j);
    }
    if let Ok(rd) = std::fs::read_dir(root) {
        for e in rd.flatten() {
            let p = e.path();
            if p.is_dir() {
                let nested = if cfg!(windows) {
                    p.join("bin").join("java.exe")
                } else {
                    p.join("bin").join("java")
                };
                if let Some(j) = probe(&nested) {
                    push_unique(out, j);
                }
            }
        }
    }
}

pub fn list_installations() -> Result<Vec<Value>, AppError> {
    let mut found = Vec::new();
    if let Ok(java) = which::which("java") {
        if let Some(j) = probe(&java) {
            push_unique(&mut found, j);
        }
    }
    if let Ok(home) = std::env::var("JAVA_HOME") {
        scan_dir(Path::new(&home), &mut found);
    }
    #[cfg(windows)]
    {
        let roots = [
            r"C:\Program Files\Java",
            r"C:\Program Files\Eclipse Adoptium",
            r"C:\Program Files\Microsoft",
            r"C:\Program Files\Zulu",
            r"C:\Program Files\Amazon Corretto",
            r"C:\Program Files\BellSoft",
        ];
        for r in roots {
            scan_dir(Path::new(r), &mut found);
        }
    }
    let settings = settings::load()?;
    for custom in &settings.custom_java_paths {
        if let Some(j) = probe(Path::new(custom)) {
            push_unique(&mut found, j);
        }
    }
    found.sort_by(|a, b| {
        let sa = if a.major == 21 { 0 } else { 1 };
        let sb = if b.major == 21 { 0 } else { 1 };
        sa.cmp(&sb).then(b.major.cmp(&a.major))
    });
    Ok(found
        .into_iter()
        .map(|j| json!({ "path": j.path, "major": j.major, "label": j.label }))
        .collect())
}

pub fn validate(path: String) -> Result<Value, AppError> {
    match probe(Path::new(&path)) {
        Some(j) => Ok(json!({ "ok": true, "install": { "path": j.path, "major": j.major, "label": j.label } })),
        None => Ok(json!({ "ok": false, "error": "Not a valid Java 21+ executable." })),
    }
}

pub fn add_custom(path: String) -> Result<Value, AppError> {
    let v = validate(path.clone())?;
    if !v.get("ok").and_then(|x| x.as_bool()).unwrap_or(false) {
        return Ok(v);
    }
    let mut s = settings::load()?;
    if !s.custom_java_paths.iter().any(|p| p == &path) {
        s.custom_java_paths.push(path.clone());
    }
    s.default_java_path = Some(path);
    settings::save(&s)?;
    Ok(v)
}

pub fn browse(app: &tauri::AppHandle) -> Result<Value, AppError> {
    use tauri_plugin_dialog::{DialogExt, FilePath};
    let mut builder = app.dialog().file();
    #[cfg(windows)]
    {
        builder = builder.add_filter("Java", &["exe"]);
    }
    let picked = builder.blocking_pick_file();
    let Some(picked) = picked else {
        return Ok(json!({ "ok": false }));
    };
    let path = match picked {
        FilePath::Path(p) => p,
        FilePath::Url(u) => PathBuf::from(u.to_string()),
    };
    let path_str = path.to_string_lossy().to_string();
    match probe(&path) {
        Some(j) => Ok(json!({
            "ok": true,
            "install": { "path": j.path, "major": j.major, "label": j.label }
        })),
        None => {
            // User may have picked java.exe — probe that path; or a folder
            let nested = if path.is_dir() {
                if cfg!(windows) {
                    path.join("bin").join("java.exe")
                } else {
                    path.join("bin").join("java")
                }
            } else {
                path.clone()
            };
            match probe(&nested) {
                Some(j) => Ok(json!({
                    "ok": true,
                    "install": { "path": j.path, "major": j.major, "label": j.label }
                })),
                None => Ok(json!({ "ok": false, "error": format!("Not a valid Java 21+ executable: {path_str}") })),
            }
        }
    }
}
