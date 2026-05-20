package com.testrank.hcf.core.timer;

public record GlobalTimer(String id, String displayName, long expiresAt) {
    public boolean active() {
        return expiresAt > System.currentTimeMillis();
    }

    public long remaining() {
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }
}
