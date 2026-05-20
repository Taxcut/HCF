package com.testrank.hcf.core.timer;

import com.testrank.hcf.core.profile.ProfileService;

import java.util.Optional;
import java.util.UUID;

public final class CooldownService {
    private final ProfileService profiles;

    public CooldownService(ProfileService profiles) {
        this.profiles = profiles;
    }

    public boolean has(UUID uuid, String key) {
        return profiles.cached(uuid).map(profile -> profile.hasCooldown(key)).orElse(false);
    }

    public long remaining(UUID uuid, String key) {
        return profiles.cached(uuid).map(profile -> profile.cooldownRemaining(key)).orElse(0L);
    }

    public void put(UUID uuid, String key, long millis) {
        profiles.cached(uuid).ifPresent(profile -> profile.cooldown(key, millis));
    }

    public Optional<String> denyMessage(UUID uuid, String key, String label) {
        long remaining = remaining(uuid, key);
        return remaining <= 0 ? Optional.empty() : Optional.of(label + " is on cooldown for " + ((remaining + 999) / 1000) + "s.");
    }
}
