package com.testrank.hcf.core.claim;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClaimTest {
    @Test
    void normalizesCornersAndDetectsOverlap() {
        Claim claim = new Claim(UUID.randomUUID(), UUID.randomUUID(), "Test", "world", 20, 30, 10, 5, ClaimType.PLAYER);

        assertTrue(claim.overlaps("world", 9, 4, 11, 6));
        assertTrue(claim.overlaps("world", 15, 10, 40, 40));
        assertFalse(claim.overlaps("world", 21, 31, 30, 40));
        assertFalse(claim.overlaps("world_nether", 15, 10, 40, 40));
    }
}
