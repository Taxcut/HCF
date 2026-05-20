package com.testrank.hcf.core.pvp;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.profile.ProfileService;

import java.util.UUID;

public final class PvpProtectionService implements HCFService {
    private final ProfileService profiles;

    public PvpProtectionService(ProfileService profiles) {
        this.profiles = profiles;
    }

    public boolean protectedPlayer(UUID uuid) {
        return profiles.cached(uuid).map(profile -> profile.timers().getOrDefault("pvp_timer", 0L) > System.currentTimeMillis()).orElse(false);
    }

    public void grant(UUID uuid, long durationMillis) {
        profiles.cached(uuid).ifPresent(profile -> {
            profile.timers().put("pvp_timer", System.currentTimeMillis() + durationMillis);
            profiles.save(profile);
        });
    }

    public void remove(UUID uuid) {
        profiles.cached(uuid).ifPresent(profile -> {
            profile.timers().remove("pvp_timer");
            profiles.save(profile);
        });
    }
}
