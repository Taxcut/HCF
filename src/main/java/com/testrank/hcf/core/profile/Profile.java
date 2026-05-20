package com.testrank.hcf.core.profile;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Profile {
    private final UUID uuid;
    private volatile UUID teamId;
    private volatile long lastSeen;
    private volatile int kills;
    private volatile int deaths;
    private volatile String pvpClass = "NONE";
    private volatile String chatColor = "&f";
    private volatile int energy;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> timers = new ConcurrentHashMap<>();
    private final Map<String, Long> statistics = new ConcurrentHashMap<>();
    private final Map<String, Boolean> settings = new ConcurrentHashMap<>();
    private final ArrayList<String> deathHistory = new ArrayList<>(16);

    public Profile(UUID uuid) {
        this.uuid = uuid;
        this.lastSeen = Instant.now().toEpochMilli();
    }

    public UUID uuid() {
        return uuid;
    }

    public Optional<Player> player() {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public UUID teamId() {
        return teamId;
    }

    public void teamId(UUID teamId) {
        this.teamId = teamId;
    }

    public int kills() {
        return kills;
    }

    void kills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        kills++;
    }

    public int deaths() {
        return deaths;
    }

    void deaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void addDeath(String entry) {
        deaths++;
        synchronized (deathHistory) {
            if (deathHistory.size() == 16) {
                deathHistory.remove(0);
            }
            deathHistory.add(entry);
        }
    }

    public String pvpClass() {
        return pvpClass;
    }

    public void pvpClass(String pvpClass) {
        this.pvpClass = pvpClass;
    }

    public String chatColor() {
        return chatColor;
    }

    public void chatColor(String chatColor) {
        this.chatColor = chatColor == null || chatColor.isBlank() ? "&f" : chatColor;
    }

    public int energy() {
        return energy;
    }

    public void energy(int energy) {
        this.energy = Math.max(0, Math.min(120, energy));
    }

    public Map<String, Long> cooldowns() {
        return cooldowns;
    }

    public Map<String, Long> timers() {
        return timers;
    }

    public Map<String, Long> statistics() {
        return statistics;
    }

    public long statistic(String key) {
        return statistics.getOrDefault(key, 0L);
    }

    public void addStatistic(String key, long amount) {
        statistics.merge(key, amount, Long::sum);
    }

    public Map<String, Boolean> settings() {
        return settings;
    }

    public boolean setting(String key, boolean defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public boolean toggleSetting(String key, boolean defaultValue) {
        boolean next = !setting(key, defaultValue);
        settings.put(key, next);
        return next;
    }

    public boolean hasCooldown(String key) {
        return cooldowns.getOrDefault(key, 0L) > System.currentTimeMillis();
    }

    public long cooldownRemaining(String key) {
        return Math.max(0L, cooldowns.getOrDefault(key, 0L) - System.currentTimeMillis());
    }

    public void cooldown(String key, long millis) {
        cooldowns.put(key, System.currentTimeMillis() + millis);
    }

    public void seenNow() {
        lastSeen = Instant.now().toEpochMilli();
    }

    public long lastSeen() {
        return lastSeen;
    }

    void lastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public java.util.List<String> deathHistory() {
        synchronized (deathHistory) {
            return Collections.unmodifiableList(new ArrayList<>(deathHistory));
        }
    }

    void loadDeath(String entry) {
        synchronized (deathHistory) {
            if (deathHistory.size() < 16) {
                deathHistory.add(entry);
            }
        }
    }
}
