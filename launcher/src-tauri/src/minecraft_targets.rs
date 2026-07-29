//! Mirror of launcher/src/shared/minecraft-targets.ts — keep in sync.

#[derive(Debug, Clone, Copy)]
pub struct MinecraftTarget {
    pub mc_version: &'static str,
    pub jar_prefix: &'static str,
    pub local_build_dir: &'static str,
    pub fabric_api: &'static str,
    pub fabric_loader: &'static str,
    pub java_major: u32,
}

pub const TARGET_26_2: MinecraftTarget = MinecraftTarget {
    mc_version: "26.2",
    jar_prefix: "stellar-client-26.2",
    local_build_dir: "mc-26.2",
    fabric_api: "0.154.2+26.2",
    fabric_loader: "0.19.3",
    java_major: 25,
};

pub const TARGET_1_21_11: MinecraftTarget = MinecraftTarget {
    mc_version: "1.21.11",
    jar_prefix: "stellar-client-1.21.11",
    local_build_dir: "mc-1.21.11",
    fabric_api: "0.141.4+1.21.11",
    fabric_loader: "0.19.3",
    java_major: 21,
};

pub const DEFAULT_TARGET: MinecraftTarget = TARGET_26_2;

pub const ALL_TARGETS: &[MinecraftTarget] = &[TARGET_26_2, TARGET_1_21_11];

pub fn resolve_target(minecraft_version: &str) -> MinecraftTarget {
    let raw = minecraft_version.trim();
    if raw.is_empty() {
        return DEFAULT_TARGET;
    }
    for t in ALL_TARGETS {
        if t.mc_version == raw || t.local_build_dir == raw {
            return *t;
        }
    }
    for t in ALL_TARGETS {
        if raw.starts_with(&format!("{}.", t.mc_version)) || raw.starts_with(&format!("{}-", t.mc_version))
        {
            return *t;
        }
    }
    DEFAULT_TARGET
}

pub fn is_prime_jar_for_prefix(name: &str, prefix: &str) -> bool {
    name.starts_with(prefix)
        && name.ends_with(".jar")
        && !name.contains("-sources")
        && !name.contains("-dev")
}

pub fn is_any_prime_jar(name: &str) -> bool {
    ALL_TARGETS
        .iter()
        .any(|t| is_prime_jar_for_prefix(name, t.jar_prefix))
}
