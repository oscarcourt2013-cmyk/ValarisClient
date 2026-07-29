use crate::error::AppError;
use crate::ecosystem;
use crate::instances;
use crate::paths;
use crate::settings;
use serde_json::{json, Map, Value};
use std::fs;

/// Writes `{game}/config/StellarClient/profiles/default.json` for the mod.
pub fn sync_instance(instance_id: &str) -> Result<(), AppError> {
    let game = paths::instance_game_dir(instance_id);
    let profile = game
        .join("config")
        .join("StellarClient")
        .join("profiles")
        .join("default.json");
    if let Some(parent) = profile.parent() {
        fs::create_dir_all(parent)?;
    }

    let equipped = ecosystem::equipped_for_bridge()?;
    let mut cape = "cape-prime";
    let mut wings = "wings-light";
    let mut badge = "badge-founder";
    for id in &equipped {
        match id.as_str() {
            "cape-prime" => cape = "cape-prime",
            "wings-ember" => wings = "wings-light",
            "badge-founder" | "badge-veteran" => badge = "badge-founder",
            _ => {}
        }
    }
    let settings = settings::load()?;
    let discord = settings.discord_rpc;
    let theme = normalize_theme(&settings.theme);
    let preset = settings.performance_preset.as_str();

    let mut existing = if profile.exists() {
        serde_json::from_str(&fs::read_to_string(&profile)?).unwrap_or(json!({}))
    } else {
        json!({})
    };
    if let Some(obj) = existing.as_object_mut() {
        obj.insert(
            "cosmetics".into(),
            json!({ "CAPE": cape, "WINGS": wings, "BADGE": badge }),
        );
        obj.insert("theme".into(), json!({ "active": theme }));

        let mut modules = obj
            .get("modules")
            .and_then(|v| v.as_object())
            .cloned()
            .unwrap_or_else(Map::new);
        merge_module_enabled(&mut modules, "discord-rpc", discord);
        apply_perf_preset(&mut modules, preset);
        obj.insert("modules".into(), Value::Object(modules));
    }
    fs::write(profile, serde_json::to_string_pretty(&existing)?)?;
    Ok(())
}

fn merge_module_enabled(modules: &mut Map<String, Value>, id: &str, enabled: bool) {
    let mut section = modules
        .get(id)
        .and_then(|v| v.as_object())
        .cloned()
        .unwrap_or_else(Map::new);
    section.insert("enabled".into(), Value::Bool(enabled));
    modules.insert(id.into(), Value::Object(section));
}

fn apply_perf_preset(modules: &mut Map<String, Value>, preset: &str) {
    let (enable, disable): (&[&str], &[&str]) = match preset {
        "low" => (
            &[
                "fps-booster",
                "entity-culling",
                "particle-optimizer",
                "dynamic-fps",
                "animation-optimizer",
            ],
            &[
                "keystrokes",
                "crosshair-editor",
                "coordinates",
                "target-hud",
                "armor-hud",
                "potion-hud",
            ],
        ),
        "performance" => (
            &[
                "fps-booster",
                "entity-culling",
                "particle-optimizer",
                "animation-optimizer",
                "dynamic-fps",
            ],
            &["keystrokes", "crosshair-editor", "target-hud"],
        ),
        "ultra" => (
            &[],
            &[
                "fps-booster",
                "entity-culling",
                "particle-optimizer",
                "animation-optimizer",
                "dynamic-fps",
            ],
        ),
        _ => (
            &["dynamic-fps"],
            &[
                "fps-booster",
                "entity-culling",
                "particle-optimizer",
                "animation-optimizer",
            ],
        ),
    };
    for id in enable {
        merge_module_enabled(modules, id, true);
    }
    for id in disable {
        merge_module_enabled(modules, id, false);
    }
}

fn normalize_theme(id: &str) -> String {
    settings::normalize_theme_id(id)
}

pub fn sync_all_prime_instances() -> Result<(), AppError> {
    let list = instances::list()?;
    for inst in list {
        let include = inst
            .get("includePrimeMod")
            .and_then(|v| v.as_bool())
            .unwrap_or(false);
        if !include {
            continue;
        }
        if let Some(id) = inst.get("id").and_then(|v| v.as_str()) {
            sync_instance(id)?;
        }
    }
    Ok(())
}
