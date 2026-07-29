use parking_lot::Mutex;
use std::fs;
use std::sync::Arc;
use tokio::sync::mpsc;

use crate::error::AppError;
use crate::paths;
use crate::social::SocialSession;

#[derive(Default)]
pub struct AppStateInner {
    pub social: Option<SocialSession>,
    pub social_ws_started: bool,
}

pub struct AppState {
    pub inner: Arc<Mutex<AppStateInner>>,
    pub social_ws_tx: Arc<Mutex<Option<mpsc::UnboundedSender<String>>>>,
}

impl AppState {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(Mutex::new(AppStateInner::default())),
            social_ws_tx: Arc::new(Mutex::new(None)),
        }
    }

    pub fn ensure_dirs(&self) -> Result<(), AppError> {
        fs::create_dir_all(paths::user_data_dir())?;
        Ok(())
    }
}
