package com.testrank.hcf.core.profile;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProfileCooldownTest {
    @Test
    void cooldownExpiresByTimestamp() throws Exception {
        Profile profile = new Profile(UUID.randomUUID());

        profile.cooldown("pearl", 30L);

        assertTrue(profile.hasCooldown("pearl"));
        Thread.sleep(45L);
        assertFalse(profile.hasCooldown("pearl"));
    }
}
