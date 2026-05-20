package com.testrank.hcf.core.packets;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PacketRateLimiter {
    private final ConcurrentMap<UUID, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(UUID player, int maxPerSecond) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(player, ignored -> new Window(now, 0));
        synchronized (window) {
            if (now - window.startedAt >= 1000L) {
                window.startedAt = now;
                window.count = 0;
            }
            return ++window.count <= maxPerSecond;
        }
    }

    public void remove(UUID player) {
        windows.remove(player);
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
