package com.testrank.hcf.core.team;

import org.bukkit.Location;
import com.testrank.hcf.core.util.Position;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Team {
    private final UUID id;
    private String name;
    private String nameLower;
    private final Map<UUID, TeamRole> members = new ConcurrentHashMap<>();
    private final Set<UUID> allies = ConcurrentHashMap.newKeySet();
    private final Deque<String> logs = new ArrayDeque<>(64);
    private volatile double dtr = 1.1D;
    private volatile double balance;
    private volatile Position hq;
    private volatile Position rally;
    private volatile UUID focused;
    private volatile boolean frozen;
    private volatile long frozenUntil;
    private volatile boolean claimLocked;
    private volatile int points;
    private volatile int kothCaps;
    private volatile boolean regenPaused;
    private volatile boolean friendlyFire;

    public Team(UUID id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.nameLower = name.toLowerCase(java.util.Locale.ROOT);
        this.members.put(leader, TeamRole.LEADER);
        log("Team created by " + leader);
    }

    Team(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.nameLower = name.toLowerCase(java.util.Locale.ROOT);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String nameLower() { return nameLower; }
    public Map<UUID, TeamRole> members() { return Collections.unmodifiableMap(members); }
    public Set<UUID> allies() { return Collections.unmodifiableSet(allies); }
    public double dtr() { return dtr; }
    public void dtr(double dtr) { this.dtr = Math.max(-0.99D, dtr); }
    public double balance() { return balance; }
    public void balance(double balance) { this.balance = Math.max(0D, balance); }
    public int points() { return points; }
    public void points(int points) { this.points = Math.max(0, points); }
    public int kothCaps() { return kothCaps; }
    public void kothCaps(int kothCaps) { this.kothCaps = Math.max(0, kothCaps); }
    public Position hq() { return hq; }
    public void hq(Position hq) { this.hq = hq; }
    public void hq(Location hq) { this.hq = hq == null ? null : Position.from(hq); }
    public Position rally() { return rally; }
    public void rally(Location rally) { this.rally = rally == null ? null : Position.from(rally); }
    public void rally(Position rally) { this.rally = rally; }
    public UUID focused() { return focused; }
    public void focused(UUID focused) { this.focused = focused; }
    public boolean frozen() { return frozen; }
    public void frozen(boolean frozen) { this.frozen = frozen; }
    public long frozenUntil() { return frozenUntil; }
    public void frozenUntil(long frozenUntil) { this.frozenUntil = Math.max(0L, frozenUntil); }
    public boolean claimLocked() { return claimLocked; }
    public void claimLocked(boolean claimLocked) { this.claimLocked = claimLocked; }
    public boolean regenPaused() { return regenPaused; }
    public void regenPaused(boolean regenPaused) { this.regenPaused = regenPaused; }
    public boolean friendlyFire() { return friendlyFire; }
    public void friendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }

    public void rename(String name) {
        this.name = name;
        this.nameLower = name.toLowerCase(java.util.Locale.ROOT);
        log("Team renamed to " + name);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public void member(UUID uuid, TeamRole role) {
        members.put(uuid, role);
        log("Member " + uuid + " set to " + role);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        log("Member " + uuid + " removed");
    }

    public void ally(UUID teamId) {
        allies.add(teamId);
    }

    public void unally(UUID teamId) {
        allies.remove(teamId);
    }

    public void log(String line) {
        synchronized (logs) {
            if (logs.size() == 64) {
                logs.removeFirst();
            }
            logs.addLast(System.currentTimeMillis() + "|" + line);
        }
    }

    public java.util.List<String> logs() {
        synchronized (logs) {
            return java.util.List.copyOf(logs);
        }
    }

    void loadMember(UUID uuid, TeamRole role) {
        members.put(uuid, role);
    }

    void loadAlly(UUID teamId) {
        allies.add(teamId);
    }

    void loadLog(String line) {
        synchronized (logs) {
            logs.addLast(line);
        }
    }
}
