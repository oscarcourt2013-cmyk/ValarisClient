mod accounts;
mod bridge;
mod commands;
mod content;
mod discord;
mod downloads;
mod ecosystem;
mod error;
mod instances;
mod java;
mod launch;
mod logs;
mod microsoft;
mod minecraft_targets;
mod modpack;
mod options_txt;
mod paths;
mod performance;
mod settings;
mod social;
mod state;
mod updates;

use state::AppState;
use tauri::Manager;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .manage(AppState::new())
        .invoke_handler(tauri::generate_handler![
            commands::window_minimize,
            commands::window_maximize,
            commands::window_close,
            commands::app_get_version,
            commands::app_get_platform,
            commands::app_restart,
            commands::boot_initialize,
            commands::settings_get,
            commands::settings_update,
            commands::account_get_prime,
            commands::account_get_minecraft,
            commands::account_get_active,
            commands::account_set_active,
            commands::account_add_offline,
            commands::account_remove,
            commands::account_login_microsoft,
            commands::account_refresh_microsoft,
            commands::account_sync_prime,
            commands::instance_list,
            commands::instance_get,
            commands::instance_get_default,
            commands::instance_create,
            commands::instance_update,
            commands::instance_remove,
            commands::instance_duplicate,
            commands::instance_set_default,
            commands::instance_open_folder,
            commands::profile_get_active,
            commands::profile_get_all,
            commands::profile_set_instance,
            commands::bridge_sync,
            commands::launch_game,
            commands::launch_is_running,
            commands::launch_logs_list,
            commands::launch_logs_clear,
            commands::launch_logs_open_folder,
            commands::launch_crash_open,
            commands::news_list,
            commands::content_mods_list,
            commands::content_mods_set_enabled,
            commands::content_mods_remove,
            commands::content_import_mod,
            commands::content_search_modrinth,
            commands::content_search_curseforge,
            commands::content_install_modrinth,
            commands::content_install_curseforge,
            commands::content_install_resource_modrinth,
            commands::content_install_resource_curseforge,
            commands::content_install_shader_modrinth,
            commands::content_install_shader_curseforge,
            commands::content_list_versions,
            commands::content_resource_packs_list,
            commands::content_resource_set_active,
            commands::content_resource_remove,
            commands::content_resource_import,
            commands::content_shaders_list,
            commands::content_shader_set_active,
            commands::content_shader_remove,
            commands::content_shader_import,
            commands::update_check,
            commands::update_install_mod,
            commands::update_install_launcher,
            commands::update_dismiss,
            commands::friends_list,
            commands::friends_add,
            commands::friends_remove,
            commands::friends_accept,
            commands::friends_update_note,
            commands::friends_refresh_all,
            commands::social_connect,
            commands::chat_conversations,
            commands::chat_open_dm,
            commands::chat_messages,
            commands::chat_send,
            commands::chat_upload,
            commands::party_get,
            commands::party_create,
            commands::party_invite,
            commands::party_leave,
            commands::party_accept,
            commands::party_decline,
            commands::party_set_server,
            commands::social_typing,
            commands::media_list,
            commands::media_open_folder,
            commands::media_open_file,
            commands::store_catalog,
            commands::store_balance,
            commands::store_purchase,
            commands::cosmetic_list,
            commands::cosmetic_toggle,
            commands::servers_list,
            commands::servers_add,
            commands::servers_remove,
            commands::servers_refresh,
            commands::servers_refresh_all,
            commands::performance_hardware,
            commands::performance_presets,
            commands::performance_selected,
            commands::performance_apply,
            commands::downloads_list,
            commands::downloads_clear,
            commands::downloads_remove,
            commands::settings_java_list,
            commands::settings_java_browse,
            commands::settings_java_add,
            commands::dialog_open_file,
        ])
        .setup(|app| {
            let state = app.state::<AppState>();
            state.ensure_dirs()?;
            discord::start_background();
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running ValerisClient Launcher");
}
