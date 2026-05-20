package com.testrank.hcf.core.packets;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PacketRateLimiterTest {
    @Test
    void deniesPacketsPastWindowLimit() {
        PacketRateLimiter limiter = new PacketRateLimiter();
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player, 2));
        assertTrue(limiter.allow(player, 2));
        assertFalse(limiter.allow(player, 2));
    }
}
