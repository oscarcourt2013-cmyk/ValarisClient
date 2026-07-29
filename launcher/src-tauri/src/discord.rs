//! Discord Rich Presence over named pipe (Windows) / unix socket.
use crate::error::AppError;
use serde_json::json;
use std::io::{Read, Write};
use std::sync::atomic::{AtomicBool, Ordering};
use std::thread;
use std::time::Duration;

const APP_ID: &str = "1525574680994648174";
static ENABLED: AtomicBool = AtomicBool::new(true);

pub fn set_enabled(on: bool) {
    ENABLED.store(on, Ordering::SeqCst);
}

pub fn start_background() {
    thread::spawn(|| loop {
        if ENABLED.load(Ordering::SeqCst) {
            let _ = set_activity_idle();
        }
        thread::sleep(Duration::from_secs(15));
    });
}

fn encode_frame(opcode: u32, payload: &str) -> Vec<u8> {
    let bytes = payload.as_bytes();
    let mut out = Vec::with_capacity(8 + bytes.len());
    out.extend_from_slice(&opcode.to_le_bytes());
    out.extend_from_slice(&(bytes.len() as u32).to_le_bytes());
    out.extend_from_slice(bytes);
    out
}

#[cfg(windows)]
fn connect_pipe() -> Result<std::fs::File, AppError> {
    for i in 0..10 {
        let path = format!(r"\\.\pipe\discord-ipc-{i}");
        if let Ok(f) = std::fs::OpenOptions::new().read(true).write(true).open(&path) {
            return Ok(f);
        }
    }
    Err(AppError::Message("Discord IPC not found".into()))
}

#[cfg(not(windows))]
fn connect_pipe() -> Result<std::os::unix::net::UnixStream, AppError> {
    use std::os::unix::net::UnixStream;
    let base = std::env::var("XDG_RUNTIME_DIR").unwrap_or_else(|_| "/tmp".into());
    for i in 0..10 {
        let path = format!("{base}/discord-ipc-{i}");
        if let Ok(s) = UnixStream::connect(path) {
            return Ok(s);
        }
    }
    Err(AppError::Message("Discord IPC not found".into()))
}

fn set_activity_idle() -> Result<(), AppError> {
    #[cfg(windows)]
    {
        let mut pipe = connect_pipe()?;
        let handshake = json!({ "v": 1, "client_id": APP_ID }).to_string();
        pipe.write_all(&encode_frame(0, &handshake))?;
        let mut hdr = [0u8; 8];
        let _ = pipe.read_exact(&mut hdr);
        let len = u32::from_le_bytes([hdr[4], hdr[5], hdr[6], hdr[7]]) as usize;
        let mut body = vec![0u8; len.min(65536)];
        let _ = pipe.read_exact(&mut body);

        let pid = std::process::id();
        let activity = json!({
            "cmd": "SET_ACTIVITY",
            "args": {
                "pid": pid,
                "activity": {
                    "details": "StellarClient Launcher",
                    "state": "In the menu",
                    "assets": {
                        "large_image": "prime_logo",
                        "large_text": "StellarClient"
                    },
                    "buttons": [
                        { "label": "StellarClient", "url": format!("https://discord.com/applications/{APP_ID}") },
                        { "label": "Discord", "url": "https://discord.com/app" }
                    ]
                }
            },
            "nonce": uuid::Uuid::new_v4().to_string()
        })
        .to_string();
        pipe.write_all(&encode_frame(1, &activity))?;
    }
    #[cfg(not(windows))]
    {
        let mut pipe = connect_pipe()?;
        let handshake = json!({ "v": 1, "client_id": APP_ID }).to_string();
        pipe.write_all(&encode_frame(0, &handshake))?;
        let activity = json!({
            "cmd": "SET_ACTIVITY",
            "args": {
                "pid": std::process::id(),
                "activity": {
                    "details": "StellarClient Launcher",
                    "state": "In the menu",
                    "assets": { "large_image": "prime_logo", "large_text": "StellarClient" }
                }
            },
            "nonce": uuid::Uuid::new_v4().to_string()
        })
        .to_string();
        pipe.write_all(&encode_frame(1, &activity))?;
    }
    Ok(())
}
