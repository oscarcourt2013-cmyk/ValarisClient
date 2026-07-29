use crate::error::AppError;
use crate::instances;
use crate::paths;
use crate::settings;
use serde_json::{json, Value};
use std::fs;

const PRESETS: &[(&str, &str, u32, u32, &str)] = &[
    ("low", "Low PC", 2048, 8, "Minimum settings for weak hardware."),
    ("balanced", "Balanced", 4096, 12, "Recommended for most players."),
    ("performance", "Performance", 6144, 16, "High FPS competitive setup."),
    ("ultra", "Ultra", 8192, 24, "Maximum quality for powerful PCs."),
];

pub fn hardware() -> Value {
    let ram_gb = (sys_info_ram_mb() / 1024.0).round() as u32;
    json!({
        "cpu": sys_info_cpu(),
        "gpu": sys_info_gpu(),
        "ramGb": ram_gb
    })
}

fn sys_info_ram_mb() -> f64 {
    // Prefer reading from /proc or Windows API; simple: 8GB default if unknown
    let total = std::fs::read_to_string("/proc/meminfo")
        .ok()
        .and_then(|s| {
            s.lines()
                .find(|l| l.starts_with("MemTotal:"))
                .and_then(|l| l.split_whitespace().nth(1))
                .and_then(|n| n.parse::<f64>().ok())
                .map(|kb| kb / 1024.0)
        });
    if let Some(mb) = total {
        return mb;
    }
    // Windows: wmic OS get TotalVisibleMemorySize
    #[cfg(windows)]
    {
        if let Ok(out) = std::process::Command::new("wmic")
            .args(["OS", "get", "TotalVisibleMemorySize", "/value"])
            .output()
        {
            let text = String::from_utf8_lossy(&out.stdout);
            if let Some(line) = text.lines().find(|l| l.starts_with("TotalVisibleMemorySize=")) {
                if let Ok(kb) = line.trim_start_matches("TotalVisibleMemorySize=").trim().parse::<f64>() {
                    return kb / 1024.0;
                }
            }
        }
    }
    8192.0
}

fn sys_info_cpu() -> String {
    #[cfg(windows)]
    {
        if let Ok(out) = std::process::Command::new("wmic")
            .args(["cpu", "get", "name", "/value"])
            .output()
        {
            let text = String::from_utf8_lossy(&out.stdout);
            if let Some(line) = text.lines().find(|l| l.starts_with("Name=")) {
                return line.trim_start_matches("Name=").trim().to_string();
            }
        }
    }
    std::env::var("PROCESSOR_IDENTIFIER").unwrap_or_else(|_| "CPU".into())
}

fn sys_info_gpu() -> String {
    #[cfg(windows)]
    {
        if let Ok(out) = std::process::Command::new("wmic")
            .args(["path", "win32_VideoController", "get", "name"])
            .output()
        {
            let text = String::from_utf8_lossy(&out.stdout);
            for line in text.lines().skip(1) {
                let t = line.trim();
                if !t.is_empty() {
                    return t.to_string();
                }
            }
        }
    }
    std::env::var("GPU_DEVICE").unwrap_or_else(|_| "Unknown GPU".into())
}

pub fn presets() -> Vec<Value> {
    PRESETS
        .iter()
        .map(|(id, label, ram, rd, desc)| {
            json!({
                "id": id,
                "label": label,
                "ramMb": ram,
                "renderDistance": rd,
                "description": desc
            })
        })
        .collect()
}

pub fn selected() -> Result<String, AppError> {
    Ok(settings::load()?.performance_preset)
}

pub fn apply(preset_id: String, instance_id: Option<String>) -> Result<Value, AppError> {
    let Some(&(_, _, ram_mb, render_distance, _)) = PRESETS.iter().find(|(id, ..)| *id == preset_id) else {
        return Ok(json!({ "ok": false, "error": "Unknown preset." }));
    };
    let hw_ram = (sys_info_ram_mb() as u32).max(1);
    let capped = ram_mb.min((hw_ram as f64 * 1024.0 * 0.75) as u32).max(512);

    let id = if let Some(i) = instance_id {
        i
    } else {
        instances::get_default()?
            .and_then(|v| v.get("id").and_then(|x| x.as_str()).map(str::to_string))
            .ok_or_else(|| AppError::Message("No instance.".into()))?
    };

    // Update instance RAM + JVM
    let _ = instances::update(json!({
        "id": id,
        "ramMb": capped,
        "jvmArgs": ["-XX:+UseG1GC"]
    }))?;

    let options = paths::instance_game_dir(&id).join("options.txt");
    if let Some(parent) = options.parent() {
        fs::create_dir_all(parent)?;
    }
    let mut map = std::collections::BTreeMap::<String, String>::new();
    if options.exists() {
        for line in fs::read_to_string(&options)?.lines() {
            if let Some((k, v)) = line.split_once(':') {
                map.insert(k.to_string(), v.to_string());
            }
        }
    }
    map.insert("renderDistance".into(), render_distance.to_string());
    map.insert(
        "simulationDistance".into(),
        render_distance.min(12).to_string(),
    );
    let max_fps = match preset_id.as_str() {
        "ultra" => 260,
        "performance" => 240,
        _ => 120,
    };
    map.insert("maxFps".into(), max_fps.to_string());
    let graphics = match preset_id.as_str() {
        "low" => 0,
        "ultra" => 2,
        _ => 1,
    };
    map.insert("graphicsMode".into(), graphics.to_string());
    let body: String = map.iter().map(|(k, v)| format!("{k}:{v}\n")).collect();
    fs::write(options, body)?;

    let mut s = settings::load()?;
    s.performance_preset = preset_id;
    s.default_ram_mb = capped;
    settings::save(&s)?;
    Ok(json!({ "ok": true }))
}
