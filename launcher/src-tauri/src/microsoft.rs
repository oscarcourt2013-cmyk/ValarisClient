//! Microsoft â†’ Xbox Live â†’ Minecraft Services OAuth (Prism-compatible client).
use crate::accounts::{save, load, StoredMinecraftAccount};
use crate::error::{AppError, OkResult};
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use chrono::Utc;
use md5::{Digest, Md5};
use serde_json::{json, Value};
use sha2::Sha256;
use std::io::{Read, Write};
use std::net::TcpListener;
use std::sync::mpsc;
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use uuid::Uuid;

/// Public client used by many open-source Minecraft launchers (Prism / MultiMC family).
const CLIENT_ID: &str = "1ce91f64-568a-42b5-b1c3-4e6871f5b8c5";
const SCOPE: &str = "XboxLive.signin offline_access";

fn skin_url(uuid: &str) -> String {
    format!("https://mc-heads.net/avatar/{}/64", uuid.replace('-', ""))
}

fn pkce() -> (String, String) {
    let mut verifier_bytes = [0u8; 32];
    getrandom_fill(&mut verifier_bytes);
    let verifier = URL_SAFE_NO_PAD.encode(verifier_bytes);
    let mut hasher = Sha256::new();
    hasher.update(verifier.as_bytes());
    let challenge = URL_SAFE_NO_PAD.encode(hasher.finalize());
    (verifier, challenge)
}

fn getrandom_fill(buf: &mut [u8]) {
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};
    let mut h = DefaultHasher::new();
    SystemTime::now().hash(&mut h);
    Uuid::new_v4().hash(&mut h);
    let mut state = h.finish();
    for b in buf.iter_mut() {
        state = state.wrapping_mul(6364136223846793005).wrapping_add(1);
        *b = (state >> 33) as u8;
    }
}

fn http_client() -> Result<reqwest::Client, AppError> {
    reqwest::Client::builder()
        .timeout(Duration::from_secs(60))
        .user_agent("stellar-client-launcher/0.9.11")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))
}

/// Blocking OAuth with local redirect (opens system browser).
pub fn login_interactive() -> Result<OkResult, AppError> {
    let listener = TcpListener::bind("127.0.0.1:0").map_err(|e| AppError::Message(e.to_string()))?;
    let port = listener.local_addr()?.port();
    let redirect = format!("http://127.0.0.1:{port}/");
    let (verifier, challenge) = pkce();
    let state = Uuid::new_v4().to_string();
    let auth_url = format!(
        "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id={}&response_type=code&redirect_uri={}&scope={}&state={}&code_challenge={}&code_challenge_method=S256&prompt=select_account",
        CLIENT_ID,
        urlencoding::encode(&redirect),
        urlencoding::encode(SCOPE),
        urlencoding::encode(&state),
        urlencoding::encode(&challenge),
    );
    open::that(&auth_url).map_err(|e| AppError::Message(e.to_string()))?;

    let (tx, rx) = mpsc::channel();
    thread::spawn(move || {
        let _ = listener.set_nonblocking(false);
        if let Ok((mut stream, _)) = listener.accept() {
            let mut buf = [0u8; 4096];
            let n = stream.read(&mut buf).unwrap_or(0);
            let req = String::from_utf8_lossy(&buf[..n]);
            let first = req.lines().next().unwrap_or("");
            let code = first
                .split(' ')
                .nth(1)
                .and_then(|path| path.split('?').nth(1))
                .unwrap_or("")
                .split('&')
                .find_map(|p| p.strip_prefix("code=").map(|c| c.to_string()));
            let body = if code.is_some() {
                "<html><body style='font-family:sans-serif;background:#060608;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh'><div>Prime â€” you can close this tab.</div></body></html>"
            } else {
                "<html><body>Login failed.</body></html>"
            };
            let resp = format!(
                "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
                body.len(),
                body
            );
            let _ = stream.write_all(resp.as_bytes());
            let _ = tx.send(code);
        }
    });

    let code = rx
        .recv_timeout(Duration::from_secs(300))
        .map_err(|_| AppError::Message("Microsoft login timed out.".into()))?
        .ok_or_else(|| AppError::Message("Microsoft login cancelled or failed.".into()))?;

    let rt = tokio::runtime::Handle::try_current();
    let tokens = if let Ok(handle) = rt {
        handle.block_on(exchange_code(&code, &redirect, &verifier))?
    } else {
        tokio::runtime::Runtime::new()
            .map_err(|e| AppError::Message(e.to_string()))?
            .block_on(exchange_code(&code, &redirect, &verifier))?
    };

    let account = tokens_to_account(tokens)?;
    let mut db = load()?;
    // Replace existing MS account with same uuid
    db.accounts.retain(|a| a.uuid != account.uuid);
    let id = account.id.clone();
    db.active_account_id = Some(id.clone());
    db.prime_account.username = account.username.clone();
    db.prime_account.tier = "prime".into();
    if let Some(p) = db.profiles.iter_mut().find(|p| p.id == db.active_profile_id) {
        p.minecraft_account_id = id.clone();
    }
    db.accounts.push(account);
    save(&db)?;
    Ok(OkResult {
        ok: true,
        error: None,
        message: None,
        account_id: Some(id),
    })
}

struct MsTokens {
    access_token: String,
    refresh_token: String,
}

async fn exchange_code(code: &str, redirect: &str, verifier: &str) -> Result<MsTokens, AppError> {
    let client = http_client()?;
    let res = client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
        .form(&[
            ("client_id", CLIENT_ID),
            ("code", code),
            ("redirect_uri", redirect),
            ("grant_type", "authorization_code"),
            ("code_verifier", verifier),
            ("scope", SCOPE),
        ])
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let access = body
        .get("access_token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message(format!("Token error: {body}")))?
        .to_string();
    let refresh = body
        .get("refresh_token")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    Ok(MsTokens {
        access_token: access,
        refresh_token: refresh,
    })
}

async fn refresh_ms(refresh_token: &str) -> Result<MsTokens, AppError> {
    let client = http_client()?;
    let res = client
        .post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
        .form(&[
            ("client_id", CLIENT_ID),
            ("refresh_token", refresh_token),
            ("grant_type", "refresh_token"),
            ("scope", SCOPE),
        ])
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let access = body
        .get("access_token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Refresh failed â€” sign in again.".into()))?
        .to_string();
    let refresh = body
        .get("refresh_token")
        .and_then(|v| v.as_str())
        .unwrap_or(refresh_token)
        .to_string();
    Ok(MsTokens {
        access_token: access,
        refresh_token: refresh,
    })
}

async fn xbox_minecraft(ms_access: &str) -> Result<(String, String, String, Option<String>, Option<String>), AppError> {
    let client = http_client()?;
    // Xbox Live
    let xbox_body = json!({
        "Properties": {
            "AuthMethod": "RPS",
            "SiteName": "user.auth.xboxlive.com",
            "RpsTicket": format!("d={ms_access}")
        },
        "RelyingParty": "http://auth.xboxlive.com",
        "TokenType": "JWT"
    });
    let xbox: Value = client
        .post("https://user.auth.xboxlive.com/user/authenticate")
        .json(&xbox_body)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let xbox_token = xbox
        .get("Token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Xbox auth failed.".into()))?;
    let uhs = xbox
        .pointer("/DisplayClaims/xui/0/uhs")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Xbox UHS missing.".into()))?;

    // XSTS
    let xsts_body = json!({
        "Properties": {
            "SandboxId": "RETAIL",
            "UserTokens": [xbox_token]
        },
        "RelyingParty": "rp://api.minecraftservices.com/",
        "TokenType": "JWT"
    });
    let xsts: Value = client
        .post("https://xsts.auth.xboxlive.com/xsts/authorize")
        .json(&xsts_body)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let xsts_token = xsts
        .get("Token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("XSTS auth failed (no Minecraft?).".into()))?;

    // Minecraft
    let mc_login = json!({
        "identityToken": format!("XBL3.0 x={uhs};{xsts_token}")
    });
    let mc: Value = client
        .post("https://api.minecraftservices.com/authentication/login_with_xbox")
        .json(&mc_login)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let mc_token = mc
        .get("access_token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Minecraft login failed.".into()))?
        .to_string();

    let profile: Value = client
        .get("https://api.minecraftservices.com/minecraft/profile")
        .bearer_auth(&mc_token)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let uuid = profile
        .get("id")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("No Minecraft profile â€” buy the game on this account.".into()))?
        .to_string();
    let name = profile
        .get("name")
        .and_then(|v| v.as_str())
        .unwrap_or("Player")
        .to_string();
    let dashed = if uuid.contains('-') {
        uuid
    } else {
        format!(
            "{}-{}-{}-{}-{}",
            &uuid[0..8],
            &uuid[8..12],
            &uuid[12..16],
            &uuid[16..20],
            &uuid[20..32]
        )
    };
    let skin = profile
        .get("skins")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter().find(|s| s.get("state").and_then(|x| x.as_str()) == Some("ACTIVE"))
        })
        .and_then(|s| s.get("url").and_then(|u| u.as_str()).map(str::to_string));
    let cape = profile
        .get("capes")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter().find(|s| s.get("state").and_then(|x| x.as_str()) == Some("ACTIVE"))
        })
        .and_then(|s| s.get("url").and_then(|u| u.as_str()).map(str::to_string));
    Ok((mc_token, dashed, name, skin, cape))
}

fn tokens_to_account(tokens: MsTokens) -> Result<StoredMinecraftAccount, AppError> {
    let rt = tokio::runtime::Runtime::new().map_err(|e| AppError::Message(e.to_string()))?;
    let (mc_token, uuid, name, skin, cape) =
        rt.block_on(xbox_minecraft(&tokens.access_token))?;
    let _ = mc_token; // access for launch obtained via refresh later
    Ok(StoredMinecraftAccount {
        id: Uuid::new_v4().to_string(),
        account_type: "microsoft".into(),
        username: name,
        uuid: uuid.clone(),
        skin_url: Some(skin.unwrap_or_else(|| skin_url(&uuid))),
        cape_url: cape,
        ms_refresh_token: Some(tokens.refresh_token),
        ms_auth_provider: Some("azure".into()),
        added_at: Utc::now().to_rfc3339(),
        last_used_at: Some(Utc::now().to_rfc3339()),
    })
}

pub fn refresh_account(account_id: &str) -> Result<OkResult, AppError> {
    let mut db = load()?;
    let Some(account) = db.accounts.iter_mut().find(|a| a.id == account_id) else {
        return Ok(OkResult::err("Account not found."));
    };
    let Some(refresh) = account.ms_refresh_token.clone() else {
        return Ok(OkResult::err("No Microsoft refresh token â€” sign in again."));
    };
    let rt = tokio::runtime::Runtime::new().map_err(|e| AppError::Message(e.to_string()))?;
    let tokens = rt.block_on(refresh_ms(&refresh))?;
    let (mc_token, uuid, name, skin, cape) =
        rt.block_on(xbox_minecraft(&tokens.access_token))?;
    let _ = mc_token;
    account.username = name;
    account.uuid = uuid.clone();
    account.skin_url = Some(skin.unwrap_or_else(|| skin_url(&uuid)));
    account.cape_url = cape;
    account.ms_refresh_token = Some(tokens.refresh_token);
    account.ms_auth_provider = Some("azure".into());
    account.last_used_at = Some(Utc::now().to_rfc3339());
    save(&db)?;
    Ok(OkResult::ok())
}

/// Build minecraft-java-core style authenticator JSON for the launch bridge.
pub async fn launch_authenticator(account: &StoredMinecraftAccount) -> Result<Value, AppError> {
    if account.account_type == "offline" {
        return Ok(json!({
            "name": account.username,
            "uuid": account.uuid.replace('-', ""),
            "access_token": "prime_offline_access_token",
            "client_token": account.id,
            "user_properties": "{}",
            "meta": { "type": "offline", "online": false }
        }));
    }
    let refresh = account
        .ms_refresh_token
        .as_deref()
        .ok_or_else(|| AppError::Message("Sign in with Microsoft again.".into()))?;
    let tokens = refresh_ms(refresh).await?;
    // Persist rotated refresh
    {
        let mut db = load()?;
        if let Some(a) = db.accounts.iter_mut().find(|a| a.id == account.id) {
            a.ms_refresh_token = Some(tokens.refresh_token.clone());
            save(&db)?;
        }
    }
    let (mc_token, uuid, name, _, _) = xbox_minecraft(&tokens.access_token).await?;
    Ok(json!({
        "name": name,
        "uuid": uuid.replace('-', ""),
        "access_token": mc_token,
        "client_token": account.id,
        "user_properties": "{}",
        "meta": { "type": "Xbox", "demo": false }
    }))
}

// silence unused md5 import if any
#[allow(dead_code)]
fn _md5_unused(s: &str) -> String {
    let mut h = Md5::new();
    h.update(s.as_bytes());
    hex::encode(h.finalize())
}
