package dev.stellarclient.core.bundle;

import java.util.List;

/** One-click module preset for ClickGUI and profile onboarding. */
public record ModuleBundle(String id, String name, String description, List<String> moduleIds) {
}
