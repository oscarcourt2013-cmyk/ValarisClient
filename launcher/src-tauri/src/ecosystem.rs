use crate::error::AppError;
use crate::paths;
use crate::settings;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::fs;
use std::time::Instant;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct EcosystemDb {
    pub version: u32,
    pub prime_coins: i64,
    pub owned_store_items: Vec<String>,
    pub equipped_cosmetics: Vec<String>,
    pub friends: Vec<Value>,
    pub favorite_servers: Vec<FavoriteServer>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FavoriteServer {
    pub id: String,
    pub name: String,
    pub address: String,
    pub players: u32,
    pub max_players: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ping: Option<u32>,
}

fn default_db() -> EcosystemDb {
    EcosystemDb {
        version: 1,
        prime_coins: 500,
        owned_store_items: vec!["cape-prime".into()],
        equipped_cosmetics: vec!["cape-prime".into(), "badge-veteran".into()],
        friends: vec![],
        favorite_servers: vec![],
    }
}

pub fn load() -> Result<EcosystemDb, AppError> {
    let path = paths::ecosystem_path();
    if !path.exists() {
        let db = default_db();
        save(&db)?;
        return Ok(db);
    }
    let raw = fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&raw).unwrap_or_else(|_| default_db()))
}

pub fn save(db: &EcosystemDb) -> Result<(), AppError> {
    let path = paths::ecosystem_path();
    if let Some(p) = path.parent() {
        fs::create_dir_all(p)?;
    }
    fs::write(path, serde_json::to_string_pretty(db)?)?;
    Ok(())
}

const STORE: &[(&str, &str, &str, i64, &str)] = &[
    ("cape-prime", "Prime Cape", "Official StellarClient cape.", 0, "cosmetic"),
    ("theme-crimson", "Crimson Theme", "Signature red Prime theme.", 0, "theme"),
    ("bg-nebula", "Nebula Background", "Animated space background.", 150, "background"),
    ("badge-founder", "Founder Badge", "Limited edition profile badge.", 500, "badge"),
    ("wings-ember", "Ember Wings", "Fiery cosmetic wings.", 400, "cosmetic"),
    ("pet-fox", "Arctic Fox", "Companion pet cosmetic.", 300, "cosmetic"),
    ("emote-wave", "Prime Wave", "Signature emote.", 100, "cosmetic"),
];

const STORE_TO_COSMETIC: &[(&str, &str)] = &[
    ("cape-prime", "cape-prime"),
    ("wings-ember", "wings-ember"),
    ("pet-fox", "pet-fox"),
    ("emote-wave", "emote-wave"),
    ("badge-founder", "badge-founder"),
];

const COSMETICS: &[(&str, &str, &str, &str)] = &[
    ("cape-prime", "Prime Cape", "cape", "legendary"),
    ("wings-ember", "Ember Wings", "wings", "epic"),
    ("pet-fox", "Arctic Fox", "pet", "rare"),
    ("emote-wave", "Prime Wave", "emote", "common"),
    ("badge-founder", "Founder", "badge", "legendary"),
    ("badge-veteran", "Veteran", "badge", "rare"),
];

pub fn store_catalog() -> Result<Vec<Value>, AppError> {
    let db = load()?;
    Ok(STORE
        .iter()
        .map(|(id, name, desc, price, cat)| {
            json!({
                "id": id,
                "name": name,
                "description": desc,
                "price": price,
                "category": cat,
                "owned": db.owned_store_items.iter().any(|x| x == id)
            })
        })
        .collect())
}

pub fn balance() -> Result<i64, AppError> {
    Ok(load()?.prime_coins)
}

pub fn purchase(item_id: String) -> Result<Value, AppError> {
    let Some(&(id, _, _, price, _)) = STORE.iter().find(|(i, ..)| *i == item_id) else {
        return Ok(json!({ "ok": false, "error": "Unknown item." }));
    };
    let mut db = load()?;
    if db.owned_store_items.iter().any(|x| x == id) {
        return Ok(json!({ "ok": false, "error": "Already owned." }));
    }
    if db.prime_coins < price {
        return Ok(json!({ "ok": false, "error": "Not enough Prime Coins." }));
    }
    db.prime_coins -= price;
    db.owned_store_items.push(id.to_string());
    if let Some((_, cos)) = STORE_TO_COSMETIC.iter().find(|(s, _)| *s == id) {
        if !db.equipped_cosmetics.iter().any(|x| x == *cos) {
            // don't auto-equip all; just ensure owned via store
        }
    }
    save(&db)?;
    if id == "bg-nebula" {
        let _ = settings::update_merge(json!({ "backgroundNebula": true }));
    }
    if id == "theme-crimson" {
        let _ = settings::update_merge(json!({ "theme": "prime-crimson" }));
    }
    crate::bridge::sync_all_prime_instances()?;
    Ok(json!({ "ok": true, "balance": db.prime_coins }))
}

pub fn cosmetic_list() -> Result<Vec<Value>, AppError> {
    let db = load()?;
    let mut owned: Vec<String> = vec!["badge-veteran".into()];
    for (store, cos) in STORE_TO_COSMETIC {
        if db.owned_store_items.iter().any(|x| x == store) {
            owned.push((*cos).into());
        }
    }
    if db.owned_store_items.iter().any(|x| x == "cape-prime") {
        owned.push("cape-prime".into());
    }
    owned.sort();
    owned.dedup();
    Ok(COSMETICS
        .iter()
        .filter(|(id, ..)| owned.iter().any(|o| o == id))
        .map(|(id, name, ty, rarity)| {
            json!({
                "id": id,
                "name": name,
                "type": ty,
                "rarity": rarity,
                "equipped": db.equipped_cosmetics.iter().any(|e| e == id)
            })
        })
        .collect())
}

pub fn cosmetic_toggle(cosmetic_id: String) -> Result<Value, AppError> {
    let mut db = load()?;
    let owned = cosmetic_list()?;
    if !owned.iter().any(|c| c.get("id").and_then(|v| v.as_str()) == Some(&cosmetic_id)) {
        return Ok(json!({ "ok": false, "error": "Not owned." }));
    }
    let ty = COSMETICS
        .iter()
        .find(|(id, ..)| *id == cosmetic_id)
        .map(|(_, _, t, _)| *t)
        .unwrap_or("badge");
    if db.equipped_cosmetics.iter().any(|e| e == &cosmetic_id) {
        db.equipped_cosmetics.retain(|e| e != &cosmetic_id);
    } else {
        if ty != "badge" {
            let same_type: Vec<String> = COSMETICS
                .iter()
                .filter(|(_, _, t, _)| *t == ty)
                .map(|(id, ..)| (*id).to_string())
                .collect();
            db.equipped_cosmetics.retain(|e| !same_type.contains(e));
        }
        db.equipped_cosmetics.push(cosmetic_id);
    }
    save(&db)?;
    crate::bridge::sync_all_prime_instances()?;
    Ok(json!({ "ok": true }))
}

pub fn servers_list() -> Result<Vec<FavoriteServer>, AppError> {
    Ok(load()?.favorite_servers)
}

fn parse_address(address: &str) -> Result<(String, u16), AppError> {
    let address = address.trim();
    if address.is_empty() || address.len() > 255 {
        return Err(AppError::Message("Invalid address.".into()));
    }
    if let Some((host, port)) = address.rsplit_once(':') {
        if let Ok(p) = port.parse::<u16>() {
            return Ok((host.to_string(), p));
        }
    }
    Ok((address.to_string(), 25565))
}

pub async fn servers_add(name: String, address: String) -> Result<Value, AppError> {
    let name = name.trim().to_string();
    if name.is_empty() || name.len() > 48 {
        return Ok(json!({ "ok": false, "error": "Name must be 1â€“48 characters." }));
    }
    let (host, port) = match parse_address(&address) {
        Ok(v) => v,
        Err(e) => return Ok(json!({ "ok": false, "error": e.to_string() })),
    };
    let mut db = load()?;
    let mut server = FavoriteServer {
        id: Uuid::new_v4().to_string(),
        name,
        address: format!("{host}:{port}"),
        players: 0,
        max_players: 0,
        ping: None,
    };
    refresh_one(&mut server).await;
    db.favorite_servers.push(server);
    save(&db)?;
    Ok(json!({ "ok": true }))
}

pub fn servers_remove(server_id: String) -> Result<Value, AppError> {
    let mut db = load()?;
    db.favorite_servers.retain(|s| s.id != server_id);
    save(&db)?;
    Ok(json!({ "ok": true }))
}

async fn refresh_one(server: &mut FavoriteServer) {
    let Ok((host, port)) = parse_address(&server.address) else {
        return;
    };
    let url = format!("https://api.mcstatus.io/v2/status/java/{host}:{port}");
    let start = Instant::now();
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .user_agent("stellar-client-launcher")
        .build();
    let Ok(client) = client else { return };
    match client.get(&url).send().await {
        Ok(res) if res.status().is_success() => {
            if let Ok(body) = res.json::<Value>().await {
                let online = body.get("online").and_then(|v| v.as_bool()).unwrap_or(false);
                if online {
                    server.players = body
                        .pointer("/players/online")
                        .and_then(|v| v.as_u64())
                        .unwrap_or(0) as u32;
                    server.max_players = body
                        .pointer("/players/max")
                        .and_then(|v| v.as_u64())
                        .unwrap_or(0) as u32;
                    server.ping = Some(start.elapsed().as_millis() as u32);
                } else {
                    server.players = 0;
                    server.max_players = 0;
                    server.ping = None;
                }
            }
        }
        _ => {
            server.players = 0;
            server.max_players = 0;
            server.ping = None;
        }
    }
}

pub async fn servers_refresh(server_id: String) -> Result<Option<FavoriteServer>, AppError> {
    let mut db = load()?;
    if let Some(s) = db.favorite_servers.iter_mut().find(|s| s.id == server_id) {
        refresh_one(s).await;
        let out = s.clone();
        save(&db)?;
        return Ok(Some(out));
    }
    Ok(None)
}

pub async fn servers_refresh_all() -> Result<Vec<FavoriteServer>, AppError> {
    let mut db = load()?;
    for s in &mut db.favorite_servers {
        refresh_one(s).await;
    }
    save(&db)?;
    Ok(db.favorite_servers)
}

pub fn reward_launch_coins() -> Result<(), AppError> {
    let mut db = load()?;
    db.prime_coins += 10;
    save(&db)
}

pub fn equipped_for_bridge() -> Result<Vec<String>, AppError> {
    Ok(load()?.equipped_cosmetics)
}
