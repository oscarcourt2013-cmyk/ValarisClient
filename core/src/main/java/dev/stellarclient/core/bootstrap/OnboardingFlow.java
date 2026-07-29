package dev.stellarclient.core.bootstrap;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.gui.menu.OnboardingManager;

/** Applies onboarding choices to themes, profiles and modules. */
public final class OnboardingFlow {

    private OnboardingFlow() {
    }

    public static void applyChoices(StellarClient client) {
        OnboardingManager onboarding = client.onboarding();
        client.themes().trySetActive(onboarding.chosenTheme());
        String profile = onboarding.chosenProfile();
        if (!profile.equals(client.profiles().activeProfile())) {
            client.profiles().switchTo(profile);
        }
        FirstRunConfigurator.applyPreset(client.modules(), client.favorites(), profile);
        client.profiles().saveActive();
        client.notifications().success("Configuration appliquÃ©e",
                "Profil " + profileLabel(profile) + " â€¢ thÃ¨me " + onboarding.chosenTheme());
    }

    private static String profileLabel(String id) {
        return switch (id) {
            case "pvp" -> "PvP";
            case "survival" -> "Survie";
            default -> "Ã‰quilibrÃ©";
        };
    }
}
