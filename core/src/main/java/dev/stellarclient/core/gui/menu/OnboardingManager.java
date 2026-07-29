package dev.stellarclient.core.gui.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.config.ConfigBinding;
import dev.stellarclient.core.theme.ThemeManager;

/** First-run onboarding wizard state. */
public final class OnboardingManager implements ConfigBinding {

    private boolean completed;
    private int step;
    private String chosenTheme = "prime-crimson";
    private String chosenProfile = "default";

    public boolean completed() {
        return completed;
    }

    public int step() {
        return step;
    }

    public String chosenTheme() {
        return chosenTheme;
    }

    public String chosenProfile() {
        return chosenProfile;
    }

    public void nextStep() {
        step++;
        if (step >= 4) {
            completed = true;
        }
    }

    public void setChosenTheme(String themeId) {
        this.chosenTheme = ThemeManager.normalizeId(themeId);
    }

    public void setChosenProfile(String profile) {
        this.chosenProfile = profile;
    }

    public void skip() {
        completed = true;
    }

    @Override
    public String configKey() {
        return "onboarding";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("completed", completed);
        json.addProperty("step", step);
        json.addProperty("theme", chosenTheme);
        json.addProperty("profile", chosenProfile);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("completed")) completed = json.get("completed").getAsBoolean();
        if (json.has("step")) step = json.get("step").getAsInt();
        if (json.has("theme")) {
            chosenTheme = ThemeManager.normalizeId(json.get("theme").getAsString());
        }
        if (json.has("profile")) chosenProfile = json.get("profile").getAsString();
    }
}
