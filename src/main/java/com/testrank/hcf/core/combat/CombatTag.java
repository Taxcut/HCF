package com.testrank.hcf.core.combat;

import java.util.UUID;

public record CombatTag(UUID opponent, long expiresAt, CombatTagType type) {
    public boolean active() {
        return expiresAt > System.currentTimeMillis();
    }
}
