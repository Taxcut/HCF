package com.testrank.hcf.core.sotw;

import com.testrank.hcf.core.api.HCFService;

public final class SotwService implements HCFService {
    private volatile long enabledUntil;
    private final java.util.Set<java.util.UUID> pvpEnabled = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void enable(long durationMillis) {
        enabledUntil = System.currentTimeMillis() + durationMillis;
        pvpEnabled.clear();
    }

    public boolean active() {
        return enabledUntil > System.currentTimeMillis();
    }

    public long remaining() {
        return Math.max(0L, enabledUntil - System.currentTimeMillis());
    }

    public boolean enablePvp(java.util.UUID uuid) {
        return pvpEnabled.add(uuid);
    }

    public boolean pvpEnabled(java.util.UUID uuid) {
        return pvpEnabled.contains(uuid);
    }

    public boolean protectedPlayer(java.util.UUID uuid) {
        return active() && !pvpEnabled(uuid);
    }
}
