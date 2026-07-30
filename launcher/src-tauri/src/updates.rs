use crate::error::AppError;
use crate::minecraft_targets::{is_prime_jar_for_prefix, DEFAULT_TARGET};
use crate::paths;
use serde_json::{json, Value};
use std::fs;
use std::io::Write;
use std::path::PathBuf;
use tauri::Emitter;

const OWNER: &str = "oscarcourt2013-cmyk";
const REPO: &str = "ValerisClient";

pub async fn check(current_launcher: &str) -> Result<Value, AppError> {
    let client = reqwest::Client::builder()
        .user_agent("valeris-client-launcher")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))?;
    let url = format!("https://api.github.com/repos/{OWNER}/{REPO}/releases/latest");
    let res = client
        .get(&url)
        .header("Accept", "application/vnd.github+json")
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if !res.status().is_success() {
        return Ok(json!({
            "checkedAt": chrono::Utc::now().to_rfc3339(),
            "notes": "",
            "anyUpdateAvailable": false,
            "launcher": { "current": current_launcher, "latest": current_launcher, "updateAvailable": false },
            "mod": { "current": "1.2.56", "latest": "1.2.56", "updateAvailable": false },
            "current": current_launcher,
            "latest": current_launcher,
            "updateAvailable": false
        }));
    }
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let tag = body
        .get("tag_name")
        .and_then(|v| v.as_str())
        .unwrap_or(current_launcher)
        .trim_start_matches('v')
        .to_string();
    let notes = body
        .get("body")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let assets = body.get("assets").and_then(|v| v.as_array()).cloned().unwrap_or_default();
    let launcher_asset = assets.iter().find(|a| {
        a.get("name")
            .and_then(|n| n.as_str())
            .map(|n| n.starts_with("valeris-client-launcher-Setup-") && n.ends_with(".exe"))
            .unwrap_or(false)
    });
    // Prefer recommended target jar; fall back to any known Prime jar.
    let preferred_prefix = DEFAULT_TARGET.jar_prefix;
    let mod_asset = assets
        .iter()
        .find(|a| {
            a.get("name")
                .and_then(|n| n.as_str())
                .map(|n| is_prime_jar_for_prefix(n, preferred_prefix))
                .unwrap_or(false)
        })
        .or_else(|| {
            assets.iter().find(|a| {
                a.get("name")
                    .and_then(|n| n.as_str())
                    .map(|n| n.starts_with("ValerisClient-") && n.ends_with(".jar"))
                    .unwrap_or(false)
            })
        });
    let launcher_url = launcher_asset
        .and_then(|a| a.get("browser_download_url").and_then(|u| u.as_str()))
        .map(str::to_string);
    let mod_url = mod_asset
        .and_then(|a| a.get("browser_download_url").and_then(|u| u.as_str()))
        .map(str::to_string);
    let mod_name = mod_asset
        .and_then(|a| a.get("name").and_then(|n| n.as_str()))
        .map(str::to_string);
    let launcher_update = tag != current_launcher && launcher_url.is_some();
    Ok(json!({
        "checkedAt": chrono::Utc::now().to_rfc3339(),
        "notes": notes,
        "releaseUrl": format!("https://github.com/{OWNER}/{REPO}/releases"),
        "anyUpdateAvailable": launcher_update || mod_url.is_some(),
        "launcher": {
            "current": current_launcher,
            "latest": tag,
            "updateAvailable": launcher_update,
            "downloadUrl": launcher_url,
            "fileName": launcher_asset.and_then(|a| a.get("name").and_then(|n| n.as_str()))
        },
        "mod": {
            "current": "local",
            "latest": tag,
            "updateAvailable": mod_url.is_some(),
            "downloadUrl": mod_url,
            "fileName": mod_name
        },
        "current": current_launcher,
        "latest": tag,
        "updateAvailable": launcher_update
    }))
}

pub async fn install_launcher(app: tauri::AppHandle, current: &str) -> Result<Value, AppError> {
    #[cfg(not(windows))]
    {
        let _ = (app, current);
        return Ok(json!({ "ok": false, "errorKey": "unsupported_platform" }));
    }
    #[cfg(windows)]
    {
        let status = check(current).await?;
        let launcher = status.get("launcher").cloned().unwrap_or(Value::Null);
        let available = launcher
            .get("updateAvailable")
            .and_then(|v| v.as_bool())
            .unwrap_or(false);
        let download_url = launcher
            .get("downloadUrl")
            .and_then(|v| v.as_str())
            .map(str::to_string);
        let file_name = launcher
            .get("fileName")
            .and_then(|v| v.as_str())
            .map(str::to_string)
            .unwrap_or_else(|| format!("valeris-client-launcher-Setup-{}.exe", launcher.get("latest").and_then(|v| v.as_str()).unwrap_or("latest")));
        let latest = launcher
            .get("latest")
            .and_then(|v| v.as_str())
            .unwrap_or(current)
            .to_string();

        if !available || download_url.is_none() {
            return Ok(json!({ "ok": false, "errorKey": "no_update" }));
        }
        let download_url = download_url.unwrap();

        let _ = app.emit(
            "update:progress",
            json!({ "target": "launcher", "phase": "downloading", "percent": 0, "detail": &file_name }),
        );

        let bytes = reqwest::Client::builder()
            .user_agent("valeris-client-launcher")
            .build()
            .map_err(|e| AppError::Message(e.to_string()))?
            .get(&download_url)
            .send()
            .await
            .map_err(|e| AppError::Message(e.to_string()))?
            .bytes()
            .await
            .map_err(|e| AppError::Message(e.to_string()))?;

        let dest = std::env::temp_dir().join(&file_name);
        fs::File::create(&dest)?.write_all(&bytes)?;

        let _ = app.emit(
            "update:progress",
            json!({ "target": "launcher", "phase": "installing", "percent": 100 }),
        );

        let exe = std::env::current_exe().map_err(|e| AppError::Message(e.to_string()))?;
        let install_dir = exe
            .parent()
            .map(|p| p.to_path_buf())
            .unwrap_or_else(|| PathBuf::from("."));
        let args = vec![
            "/S".to_string(),
            format!("/D={}", install_dir.display()),
        ];
        std::process::Command::new(&dest)
            .args(&args)
            .spawn()
            .map_err(|e| AppError::Message(e.to_string()))?;

        let app2 = app.clone();
        std::thread::spawn(move || {
            std::thread::sleep(std::time::Duration::from_millis(500));
            app2.exit(0);
        });

        Ok(json!({ "ok": true, "version": latest }))
    }
}

pub async fn install_mod(download_url: String, file_name: String, instance_id: String) -> Result<Value, AppError> {
    let client = reqwest::Client::builder()
        .user_agent("valeris-client-launcher")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))?;
    let bytes = client
        .get(&download_url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .bytes()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let cache = paths::cache_dir().join("prime-mod");
    fs::create_dir_all(&cache)?;
    let cached = cache.join(&file_name);
    fs::File::create(&cached)?.write_all(&bytes)?;
    let mods = paths::instance_mods_dir(&instance_id);
    fs::create_dir_all(&mods)?;
    // remove old prime jars
    if let Ok(rd) = fs::read_dir(&mods) {
        for e in rd.flatten() {
            let n = e.file_name().to_string_lossy().to_string();
            if n.starts_with("ValerisClient-") && n.ends_with(".jar") {
                let _ = fs::remove_file(e.path());
            }
        }
    }
    fs::copy(&cached, mods.join(&file_name))?;
    Ok(json!({ "ok": true, "version": file_name }))
}
