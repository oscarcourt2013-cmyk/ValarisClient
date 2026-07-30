//! Installs Fabric API + ValerisClient jar before launch (same behaviour as Electron ModPackService).
use crate::error::AppError;
use crate::instances;
use crate::minecraft_targets::{
    is_any_prime_jar, is_prime_jar_for_prefix, resolve_target, MinecraftTarget,
};
use crate::paths;
use serde_json::Value;
use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};

const FABRIC_API_PROJECT: &str = "P7dR8mSH";
const OWNER: &str = "oscarcourt2013-cmyk";
const REPO: &str = "ValerisClient";

fn client() -> Result<reqwest::Client, AppError> {
    reqwest::Client::builder()
        .user_agent("valeris-client-launcher/0.9.11")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))
}

async fn download(url: &str, dest: &Path) -> Result<(), AppError> {
    let bytes = client()?
        .get(url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .error_for_status()
        .map_err(|e| AppError::Message(e.to_string()))?
        .bytes()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if let Some(parent) = dest.parent() {
        fs::create_dir_all(parent)?;
    }
    let mut f = fs::File::create(dest)?;
    f.write_all(&bytes)?;
    Ok(())
}

async fn ensure_fabric_api(instance_id: &str, mc_version: &str, preferred: Option<&str>) -> Result<(), AppError> {
    let mods = paths::instance_mods_dir(instance_id);
    fs::create_dir_all(&mods)?;
    let preferred_name = preferred.map(|v| format!("fabric-api-{v}.jar"));
    if let Some(ref name) = preferred_name {
        let dest = mods.join(name);
        if dest.exists() {
            if let Ok(meta) = fs::metadata(&dest) {
                if meta.len() > 0 {
                    return Ok(());
                }
            }
        }
    }

    let url = format!(
        "https://api.modrinth.com/v2/project/{FABRIC_API_PROJECT}/version?game_versions=[\"{mc_version}\"]&loaders=[\"fabric\"]"
    );
    let versions: Vec<Value> = client()?
        .get(url)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;

    let chosen = preferred
        .and_then(|p| {
            versions
                .iter()
                .find(|v| v.get("version_number").and_then(|x| x.as_str()) == Some(p))
        })
        .or_else(|| versions.first())
        .ok_or_else(|| AppError::Message(format!("No Fabric API for Minecraft {mc_version}")))?;

    let file = chosen
        .get("files")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter()
                .find(|f| f.get("primary").and_then(|p| p.as_bool()).unwrap_or(false))
                .or_else(|| arr.first())
        })
        .ok_or_else(|| AppError::Message("Fabric API has no file".into()))?;
    let dl = file
        .get("url")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Fabric API download url missing".into()))?;
    let filename = file
        .get("filename")
        .and_then(|v| v.as_str())
        .unwrap_or("fabric-api.jar");
    let dest = mods.join(filename);
    if !dest.exists() || fs::metadata(&dest).map(|m| m.len() == 0).unwrap_or(true) {
        download(dl, &dest).await?;
    }
    Ok(())
}

fn find_local_build(target: &MinecraftTarget) -> Option<PathBuf> {
    let libs = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join(target.local_build_dir)
        .join("build")
        .join("libs");
    let Ok(rd) = fs::read_dir(libs) else {
        return None;
    };
    let mut best: Option<PathBuf> = None;
    for e in rd.flatten() {
        let name = e.file_name().to_string_lossy().to_string();
        if is_prime_jar_for_prefix(&name, target.jar_prefix) {
            best = Some(e.path());
        }
    }
    best
}

fn find_installed(mods: &Path, prefix: &str) -> Option<PathBuf> {
    let Ok(rd) = fs::read_dir(mods) else {
        return None;
    };
    let mut best: Option<PathBuf> = None;
    for e in rd.flatten() {
        let name = e.file_name().to_string_lossy().to_string();
        if is_prime_jar_for_prefix(&name, prefix) {
            best = Some(e.path());
        }
    }
    best
}

fn find_cached(prefix: &str) -> Option<PathBuf> {
    let cache = paths::cache_dir().join("prime-mod");
    let Ok(rd) = fs::read_dir(cache) else {
        return None;
    };
    let mut best: Option<PathBuf> = None;
    for e in rd.flatten() {
        let name = e.file_name().to_string_lossy().to_string();
        if is_prime_jar_for_prefix(&name, prefix) {
            best = Some(e.path());
        }
    }
    best
}

async fn download_from_github(prefix: &str) -> Result<Option<PathBuf>, AppError> {
    let url = format!("https://api.github.com/repos/{OWNER}/{REPO}/releases/latest");
    let res = client()?
        .get(url)
        .header("Accept", "application/vnd.github+json")
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if !res.status().is_success() {
        return Ok(None);
    }
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let assets = body.get("assets").and_then(|v| v.as_array()).cloned().unwrap_or_default();
    let asset = assets.iter().find(|a| {
        a.get("name")
            .and_then(|n| n.as_str())
            .map(|n| is_prime_jar_for_prefix(n, prefix))
            .unwrap_or(false)
    });
    let Some(asset) = asset else {
        return Ok(None);
    };
    let name = asset
        .get("name")
        .and_then(|n| n.as_str())
        .ok_or_else(|| AppError::Message("Prime asset name missing".into()))?;
    let dl = asset
        .get("browser_download_url")
        .and_then(|u| u.as_str())
        .ok_or_else(|| AppError::Message("Prime download url missing".into()))?;
    let cache = paths::cache_dir().join("prime-mod");
    fs::create_dir_all(&cache)?;
    let dest = cache.join(name);
    download(dl, &dest).await?;
    Ok(Some(dest))
}

fn remove_stale_prime(mods: &Path, keep: &str) {
    if let Ok(rd) = fs::read_dir(mods) {
        for e in rd.flatten() {
            let name = e.file_name().to_string_lossy().to_string();
            if is_any_prime_jar(&name) && name != keep {
                let _ = fs::remove_file(e.path());
            }
        }
    }
}

pub async fn ensure_instance_mods(instance_id: &str) -> Result<(), AppError> {
    let stored = instances::get_stored(instance_id)?
        .ok_or_else(|| AppError::Message("Instance not found".into()))?;
    if !stored.include_prime_mod || stored.loader != "fabric" {
        return Ok(());
    }

    let target = resolve_target(&stored.minecraft_version);
    let fabric_api = stored
        .fabric_api_version
        .as_deref()
        .unwrap_or(target.fabric_api);

    ensure_fabric_api(instance_id, &stored.minecraft_version, Some(fabric_api)).await?;

    let mods = paths::instance_mods_dir(instance_id);
    fs::create_dir_all(&mods)?;

    let source = if let Ok(env) = std::env::var("PRIME_CLIENT_JAR") {
        let p = PathBuf::from(env);
        if p.exists() {
            Some(p)
        } else {
            None
        }
    } else {
        None
    }
    .or_else(|| find_local_build(&target))
    .or_else(|| find_installed(&mods, target.jar_prefix))
    .or_else(|| find_cached(target.jar_prefix));

    let source = match source {
        Some(p) => p,
        None => match download_from_github(target.jar_prefix).await? {
            Some(p) => p,
            None => return Ok(()), // Fabric API alone is still useful
        },
    };

    let file_name = source
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("ValerisClient.jar")
        .to_string();
    remove_stale_prime(&mods, &file_name);
    let dest = mods.join(&file_name);
    if source != dest {
        fs::copy(&source, &dest)?;
    }
    Ok(())
}
