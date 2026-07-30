package dev.valerisclient.core.modules.valeris;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.profile.ProfileManager;

/** Switches between named configuration profiles. */
public final class ValerisProfilesModule extends Module {

    public enum Profile {
        DEFAULT("default"),
        PVP("pvp"),
        SURVIVAL("survival");

        private final String id;

        Profile(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }
    }

    private final EnumSetting<Profile> profile =
            addSetting(new EnumSetting<>("profile", "Profile", "Active configuration profile", Profile.DEFAULT));

    private final ProfileManager profiles;
    private String lastApplied = "";

    public ValerisProfilesModule(ProfileManager profiles) {
        super("prime-profiles", "Valeris Profiles", "Switch between default, PvP, and survival configs", ModuleCategory.PRIME);
        this.profiles = profiles;

        listen(ClientTickEvent.class, event -> applyProfile());
    }

    @Override
    protected void onEnable() {
        syncFromActive();
    }

    private void syncFromActive() {
        String active = profiles.activeProfile();
        for (Profile candidate : Profile.values()) {
            if (candidate.id().equals(active)) {
                profile.set(candidate);
                lastApplied = active;
                return;
            }
        }
        lastApplied = active;
    }

    private void applyProfile() {
        String target = profile.get().id();
        if (target.equals(lastApplied)) {
            return;
        }
        profiles.switchTo(target);
        lastApplied = target;
    }
}
