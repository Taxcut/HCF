package com.testrank.hcf.core.events;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HCFEvent {
    private final UUID id = UUID.randomUUID();
    private final HCFEventType type;
    private final String name;
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();
    private volatile long endsAt;
    private volatile boolean active;

    public HCFEvent(HCFEventType type, String name) {
        this.type = type;
        this.name = name;
    }

    public UUID id() { return id; }
    public HCFEventType type() { return type; }
    public String name() { return name; }
    public long endsAt() { return endsAt; }
    public boolean active() { return active && (endsAt <= 0L || endsAt > System.currentTimeMillis()); }
    public Map<UUID, Integer> scores() { return scores; }

    public void start(long durationMillis) {
        active = true;
        endsAt = System.currentTimeMillis() + durationMillis;
    }

    public void stop() {
        active = false;
    }

    public void addScore(UUID team, int amount) {
        scores.merge(team, amount, Integer::sum);
    }
}
