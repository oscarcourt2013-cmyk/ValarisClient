use crate::error::AppError;
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::path::Path;
use std::sync::Arc;
use std::time::Duration;
use tauri::{AppHandle, Emitter};
use tokio::sync::mpsc;
use tokio_tungstenite::{connect_async, tungstenite::Message};

const DEFAULT_API: &str = "http://194.9.172.102:26005";

#[derive(Debug, Clone)]
pub struct SocialSession {
    pub token: String,
    pub uuid: String,
    pub api_base: String,
}

#[derive(Debug, Deserialize)]
struct AuthResponse {
    token: String,
}

pub async fn ensure_session(uuid: &str, username: &str, offline: bool) -> Result<SocialSession, AppError> {
    let api_base = std::env::var("PRIME_API_BASE").unwrap_or_else(|_| DEFAULT_API.into());
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{api_base}/v1/auth/session"))
        .json(&json!({
            "uuid": uuid,
            "username": username,
            "offline": offline,
            "client": "launcher"
        }))
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if !res.status().is_success() {
        return Err(AppError::Message(format!("Auth failed ({})", res.status())));
    }
    let body: AuthResponse = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    Ok(SocialSession {
        token: body.token,
        uuid: uuid.to_string(),
        api_base,
    })
}

pub async fn get_json(session: &SocialSession, path: &str) -> Result<Value, AppError> {
    let client = reqwest::Client::new();
    let res = client
        .get(format!("{}{path}", session.api_base))
        .header("Authorization", format!("Bearer {}", session.token))
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let status = res.status();
    let body = res.text().await.map_err(|e| AppError::Message(e.to_string()))?;
    if !status.is_success() {
        return Err(AppError::Message(body));
    }
    Ok(serde_json::from_str(&body).unwrap_or(Value::Null))
}

pub async fn post_json(session: &SocialSession, path: &str, body: Value) -> Result<Value, AppError> {
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}{path}", session.api_base))
        .header("Authorization", format!("Bearer {}", session.token))
        .json(&body)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let status = res.status();
    let text = res.text().await.map_err(|e| AppError::Message(e.to_string()))?;
    if !status.is_success() {
        let err = serde_json::from_str::<Value>(&text)
            .ok()
            .and_then(|v| v.get("error").and_then(|e| e.as_str()).map(str::to_string))
            .unwrap_or(text);
        return Err(AppError::Message(err));
    }
    Ok(serde_json::from_str(&text).unwrap_or(Value::Null))
}

pub async fn put_json(session: &SocialSession, path: &str, body: Value) -> Result<Value, AppError> {
    let client = reqwest::Client::new();
    let res = client
        .put(format!("{}{path}", session.api_base))
        .header("Authorization", format!("Bearer {}", session.token))
        .json(&body)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let status = res.status();
    let text = res.text().await.map_err(|e| AppError::Message(e.to_string()))?;
    if !status.is_success() {
        let err = serde_json::from_str::<Value>(&text)
            .ok()
            .and_then(|v| v.get("error").and_then(|e| e.as_str()).map(str::to_string))
            .unwrap_or(text);
        return Err(AppError::Message(err));
    }
    Ok(serde_json::from_str(&text).unwrap_or(Value::Null))
}

pub async fn delete(session: &SocialSession, path: &str) -> Result<Value, AppError> {
    let client = reqwest::Client::new();
    let res = client
        .delete(format!("{}{path}", session.api_base))
        .header("Authorization", format!("Bearer {}", session.token))
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let status = res.status();
    let text = res.text().await.map_err(|e| AppError::Message(e.to_string()))?;
    if !status.is_success() {
        return Err(AppError::Message(text));
    }
    Ok(serde_json::from_str(&text).unwrap_or(json!({ "ok": true })))
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FriendEntry {
    pub id: String,
    pub username: String,
    pub status: String,
    pub activity: Option<String>,
    pub server_address: Option<String>,
    pub note: Option<String>,
    pub pending: Option<bool>,
    pub incoming: Option<bool>,
}

pub async fn upload_image(session: &SocialSession, file_path: String) -> Result<String, AppError> {
    let path = Path::new(&file_path);
    let bytes = tokio::fs::read(path)
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let filename = path
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("image.jpg")
        .to_string();
    let lower = filename.to_lowercase();
    let content_type = if lower.ends_with(".png") {
        "image/png"
    } else if lower.ends_with(".webp") {
        "image/webp"
    } else if lower.ends_with(".gif") {
        "image/gif"
    } else {
        "image/jpeg"
    };

    let part = reqwest::multipart::Part::bytes(bytes)
        .file_name(filename)
        .mime_str(content_type)
        .map_err(|e| AppError::Message(e.to_string()))?;
    let form = reqwest::multipart::Form::new().part("file", part);
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/v1/upload", session.api_base))
        .header("Authorization", format!("Bearer {}", session.token))
        .multipart(form)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if !res.status().is_success() {
        let data: Value = res.json().await.unwrap_or(Value::Null);
        let err = data
            .get("error")
            .and_then(|v| v.as_str())
            .unwrap_or("Upload failed");
        return Err(AppError::Message(err.into()));
    }
    let data: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let url = data
        .get("url")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Upload missing url".into()))?;
    if url.starts_with("http") {
        Ok(url.to_string())
    } else {
        Ok(format!("{}{url}", session.api_base.trim_end_matches('/')))
    }
}

pub fn map_friends(payload: &Value) -> Vec<FriendEntry> {
    let mut out = vec![];
    let Some(arr) = payload.get("friends").and_then(|v| v.as_array()) else {
        return out;
    };
    for f in arr {
        let status = f.get("status").and_then(|v| v.as_str()).unwrap_or("pending");
        let incoming = f.get("incoming").and_then(|v| v.as_bool()).unwrap_or(false);
        if status != "accepted" && status != "pending" && !incoming {
            continue;
        }
        let presence = f.get("presence");
        let pstatus = presence
            .and_then(|p| p.get("status"))
            .and_then(|v| v.as_str())
            .unwrap_or("offline");
        let server_address = presence
            .and_then(|p| p.get("serverAddress"))
            .and_then(|v| v.as_str())
            .or_else(|| f.get("serverAddress").and_then(|v| v.as_str()))
            .map(str::to_string);
        let note = f
            .get("note")
            .and_then(|v| v.as_str())
            .filter(|s| !s.is_empty())
            .map(str::to_string);
        let activity = if let Some(n) = note.clone() {
            Some(n)
        } else if status == "pending" {
            Some("Pending friend request".into())
        } else {
            presence
                .and_then(|p| p.get("activity"))
                .and_then(|v| v.as_str())
                .map(str::to_string)
                .or_else(|| {
                    server_address
                        .as_ref()
                        .map(|addr| format!("Playing {addr}"))
                })
        };
        out.push(FriendEntry {
            id: f.get("uuid").and_then(|v| v.as_str()).unwrap_or("").into(),
            username: f.get("username").and_then(|v| v.as_str()).unwrap_or("Player").into(),
            status: pstatus.into(),
            activity,
            server_address,
            note,
            pending: Some(status == "pending"),
            incoming: Some(incoming),
        });
    }
    out
}

fn ws_url(session: &SocialSession) -> String {
    let base = session
        .api_base
        .replace("https://", "wss://")
        .replace("http://", "ws://");
    format!(
        "{}/social?token={}",
        base.trim_end_matches('/'),
        urlencoding::encode(&session.token)
    )
}

fn presence_message(status: &str, activity: &str, server_address: Option<&str>) -> String {
    json!({
        "t": "presence",
        "status": status,
        "activity": activity,
        "serverAddress": server_address
    })
    .to_string()
}

/// Best-effort presence update over the cached social WebSocket sender.
pub fn set_presence(
    tx: Option<&mpsc::UnboundedSender<String>>,
    status: &str,
    activity: &str,
    server_address: Option<&str>,
) {
    if let Some(sender) = tx {
        let _ = sender.send(presence_message(status, activity, server_address));
    }
}

/// Opens a persistent social WebSocket, forwards events to the UI, and accepts outbound presence messages.
pub fn spawn_ws(
    app: AppHandle,
    session: SocialSession,
    ws_tx_slot: Arc<parking_lot::Mutex<Option<mpsc::UnboundedSender<String>>>>,
) {
    tokio::spawn(async move {
        let url = ws_url(&session);
        loop {
            match connect_async(&url).await {
                Ok((stream, _)) => {
                    let (mut write, mut read) = stream.split();
                    let (out_tx, mut out_rx) = mpsc::unbounded_channel::<String>();
                    *ws_tx_slot.lock() = Some(out_tx.clone());
                    let _ = write
                        .send(Message::Text(
                            presence_message("online", "In launcher", None).into(),
                        ))
                        .await;

                    loop {
                        tokio::select! {
                            _ = tokio::time::sleep(Duration::from_secs(25)) => {
                                let ping = json!({ "t": "ping", "ts": std::time::SystemTime::now()
                                    .duration_since(std::time::UNIX_EPOCH)
                                    .map(|d| d.as_millis() as u64)
                                    .unwrap_or(0) }).to_string();
                                if write.send(Message::Text(ping.into())).await.is_err() {
                                    break;
                                }
                            }
                            incoming = read.next() => {
                                match incoming {
                                    Some(Ok(Message::Text(text))) => {
                                        if let Ok(value) = serde_json::from_str::<Value>(&text) {
                                            if value.get("t").and_then(|v| v.as_str()) == Some("pong") {
                                                continue;
                                            }
                                            let _ = app.emit("social:event", value);
                                        }
                                    }
                                    Some(Ok(Message::Close(_))) | None => break,
                                    Some(Err(_)) => break,
                                    _ => {}
                                }
                            }
                            outgoing = out_rx.recv() => {
                                match outgoing {
                                    Some(text) => {
                                        if write.send(Message::Text(text.into())).await.is_err() {
                                            break;
                                        }
                                    }
                                    None => break,
                                }
                            }
                        }
                    }
                }
                Err(_) => {}
            }
            *ws_tx_slot.lock() = None;
            tokio::time::sleep(Duration::from_secs(3)).await;
        }
    });
}
