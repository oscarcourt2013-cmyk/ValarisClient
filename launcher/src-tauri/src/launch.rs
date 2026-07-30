//! Spawns the Node launch-bridge only while starting the game (not a persistent Electron shell).
use crate::accounts;
use crate::bridge;
use crate::ecosystem;
use crate::error::AppError;
use crate::instances;
use crate::logs;
use crate::microsoft;
use crate::modpack;
use crate::paths;
use crate::settings;
use crate::social;
use crate::state::AppState;
use serde_json::{json, Value};
use std::path::PathBuf;
use std::process::Stdio;
use std::sync::atomic::{AtomicBool, Ordering};
use tauri::{AppHandle, Emitter, Manager};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::Command;

static LAUNCHING: AtomicBool = AtomicBool::new(false);

fn bridge_script() -> Result<PathBuf, AppError> {
    // Dev: launcher/resources/launch-bridge/launch.js
    let candidates = [
        PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../resources/launch-bridge/launch.js"),
        PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("resources/launch-bridge/launch.js"),
    ];
    candidates
        .into_iter()
        .find(|p| p.exists())
        .ok_or_else(|| AppError::Message("launch-bridge/launch.js missing â€” run npm install in resources/launch-bridge".into()))
}

fn node_bin() -> String {
    which::which("node")
        .ok()
        .map(|p| p.to_string_lossy().to_string())
        .unwrap_or_else(|| "node".into())
}

pub async fn launch_game(
    app: AppHandle,
    instance_id: String,
    server_address: Option<String>,
) -> Result<Value, AppError> {
    if LAUNCHING.swap(true, Ordering::SeqCst) {
        return Ok(json!({ "ok": false, "message": "Already launching", "error": "busy" }));
    }
    let result = launch_inner(app, instance_id, server_address).await;
    LAUNCHING.store(false, Ordering::SeqCst);
    result
}

async fn launch_inner(
    app: AppHandle,
    instance_id: String,
    server_address: Option<String>,
) -> Result<Value, AppError> {
    let _ = app.emit("launch:log-reset", json!({}));
    let entry = logs::append("info", "Launch started", Some("start"));
    let _ = app.emit("launch:log-append", &entry);
    let _ = app.emit(
        "launch:progress",
        json!({ "phase": "start", "detail": "Preparingâ€¦", "percent": 2 }),
    );

    let stored = instances::get_stored(&instance_id)?
        .ok_or_else(|| AppError::Message("Instance not found.".into()))?;
    let account = accounts::get_active_stored()?
        .ok_or_else(|| AppError::Message("No Minecraft account selected.".into()))?;

    let _ = bridge::sync_instance(&instance_id);
    let _ = app.emit(
        "launch:progress",
        json!({ "phase": "mods", "detail": "Installing ValerisClient & Fabric APIâ€¦", "percent": 12 }),
    );
    logs::append("info", "Ensuring modsâ€¦", Some("mods"));
    if let Err(e) = modpack::ensure_instance_mods(&instance_id).await {
        let msg = e.to_string();
        logs::append("error", &msg, Some("mods"));
        let _ = app.emit(
            "launch:progress",
            json!({ "phase": "error", "detail": &msg }),
        );
        return Ok(json!({ "ok": false, "message": msg, "error": msg }));
    }

    let authenticator = microsoft::launch_authenticator(&account).await?;
    let s = settings::load()?;
    let presence_server = server_address.clone();

    let mut game_args = vec![];
    if let Some(addr) = server_address {
        let (host, port) = if let Some((h, p)) = addr.rsplit_once(':') {
            (h.to_string(), p.parse::<u16>().unwrap_or(25565))
        } else {
            (addr, 25565)
        };
        game_args.push("--server".into());
        game_args.push(host);
        game_args.push("--port".into());
        game_args.push(port.to_string());
    }
    if s.game_display_mode == "fullscreen" {
        game_args.push("--fullscreen".into());
    }

    let mut jvm = s.jvm_args.clone();
    jvm.extend(stored.jvm_args.clone());
    jvm.retain(|a| !a.contains("UseCompactObjectHeaders"));

    let cfg = json!({
        "authenticator": authenticator,
        "gameDir": paths::instance_game_dir(&instance_id),
        "runtimePath": paths::runtime_root(),
        "options": {
            "minecraftVersion": stored.minecraft_version,
            "loader": stored.loader,
            "fabricLoaderVersion": stored.fabric_loader_version.unwrap_or_else(|| "0.19.3".into()),
            "includePrimeMod": stored.include_prime_mod,
            "ramMb": stored.ram_mb,
            "jvmArgs": jvm,
            "javaPath": stored.java_path.or(s.default_java_path),
            "javaVersion": 21,
            "width": s.game_width,
            "height": s.game_height,
            "fullscreen": s.game_display_mode == "fullscreen",
            "gameArgs": game_args
        }
    });

    let script = bridge_script()?;
    let bridge_dir = script.parent().unwrap().to_path_buf();
    // Ensure deps
    if !bridge_dir.join("node_modules").exists() {
        let _ = app.emit(
            "launch:progress",
            json!({ "phase": "start", "detail": "Installing launch bridgeâ€¦", "percent": 3 }),
        );
        let status = Command::new(node_bin().replace("node", "npm").replace("node.exe", "npm.cmd"))
            .arg("install")
            .current_dir(&bridge_dir)
            .status()
            .await;
        // fallback npm
        if status.is_err() || !status.map(|s| s.success()).unwrap_or(false) {
            let _ = Command::new("npm")
                .arg("install")
                .current_dir(&bridge_dir)
                .status()
                .await;
        }
    }

    let mut child = Command::new(node_bin())
        .arg(&script)
        .current_dir(&bridge_dir)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(false)
        .spawn()
        .map_err(|e| AppError::Message(format!("Failed to start launch bridge: {e}")))?;

    if let Some(mut stdin) = child.stdin.take() {
        use tokio::io::AsyncWriteExt;
        stdin
            .write_all(cfg.to_string().as_bytes())
            .await
            .map_err(|e| AppError::Message(e.to_string()))?;
        drop(stdin);
    }

    let stdout = child.stdout.take().ok_or_else(|| AppError::Message("No stdout".into()))?;
    let mut lines = BufReader::new(stdout).lines();
    let mut ok = false;
    let mut message = "Launch finished".to_string();

    while let Ok(Some(line)) = lines.next_line().await {
        if let Ok(msg) = serde_json::from_str::<Value>(&line) {
            match msg.get("t").and_then(|v| v.as_str()) {
                Some("progress") => {
                    if let Some(detail) = msg.get("detail").and_then(|v| v.as_str()) {
                        let phase = msg.get("phase").and_then(|v| v.as_str());
                        let entry = logs::append("info", detail, phase);
                        let _ = app.emit("launch:log-append", &entry);
                    }
                    let phase = msg.get("phase").and_then(|v| v.as_str()).unwrap_or("");
                    if phase == "stopped" || phase == "crashed" {
                        if let Some(state) = app.try_state::<AppState>() {
                            social::set_presence(
                                state.social_ws_tx.lock().as_ref(),
                                "online",
                                "In launcher",
                                None,
                            );
                        }
                    }
                    let _ = app.emit("launch:progress", msg);
                }
                Some("done") => {
                    ok = true;
                    message = msg
                        .get("message")
                        .and_then(|v| v.as_str())
                        .unwrap_or("ok")
                        .to_string();
                    let entry = logs::append("info", &message, Some("running"));
                    let _ = app.emit("launch:log-append", &entry);
                    let _ = app.emit(
                        "launch:progress",
                        json!({ "phase": "running", "detail": &message, "percent": 100 }),
                    );
                    if let Some(state) = app.try_state::<AppState>() {
                        social::set_presence(
                            state.social_ws_tx.lock().as_ref(),
                            "in-game",
                            "Playing",
                            presence_server.as_deref(),
                        );
                    }
                    // Keep reading until the bridge exits so we catch phase=stopped.
                    let app_bg = app.clone();
                    tokio::spawn(async move {
                        while let Ok(Some(line)) = lines.next_line().await {
                            if let Ok(msg) = serde_json::from_str::<Value>(&line) {
                                if msg.get("t").and_then(|v| v.as_str()) == Some("progress") {
                                    let phase =
                                        msg.get("phase").and_then(|v| v.as_str()).unwrap_or("");
                                    if let Some(detail) = msg.get("detail").and_then(|v| v.as_str())
                                    {
                                        let entry = logs::append("info", detail, Some(phase));
                                        let _ = app_bg.emit("launch:log-append", &entry);
                                    }
                                    if phase == "stopped" || phase == "crashed" {
                                        if let Some(state) = app_bg.try_state::<AppState>() {
                                            social::set_presence(
                                                state.social_ws_tx.lock().as_ref(),
                                                "online",
                                                "In launcher",
                                                None,
                                            );
                                        }
                                    }
                                    let _ = app_bg.emit("launch:progress", msg);
                                }
                            }
                        }
                        let _ = child.wait().await;
                        if let Some(state) = app_bg.try_state::<AppState>() {
                            social::set_presence(
                                state.social_ws_tx.lock().as_ref(),
                                "online",
                                "In launcher",
                                None,
                            );
                        }
                        let _ = app_bg.emit(
                            "launch:progress",
                            json!({ "phase": "stopped", "detail": "Game exited" }),
                        );
                    });
                    break;
                }
                Some("error") => {
                    message = msg
                        .get("message")
                        .and_then(|v| v.as_str())
                        .unwrap_or("Launch failed")
                        .to_string();
                    let entry = logs::append("error", &message, Some("error"));
                    let _ = app.emit("launch:log-append", &entry);
                    let _ = app.emit(
                        "launch:progress",
                        json!({ "phase": "error", "detail": &message }),
                    );
                    break;
                }
                _ => {}
            }
        }
    }

    if ok {
        let _ = instances::mark_played(&instance_id);
        let _ = ecosystem::reward_launch_coins();
        Ok(json!({ "ok": true, "message": message }))
    } else {
        Ok(json!({ "ok": false, "message": message.clone(), "error": message }))
    }
}

pub fn is_running() -> bool {
    LAUNCHING.load(Ordering::SeqCst)
}
