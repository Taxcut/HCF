package com.testrank.hcf.core.profile;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

public final class PlayerStateService implements HCFService {
    private final HCFSettings settings;
    private final ProfileService profiles;
    private final ConcurrentMap<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> playtime = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> lives = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> baseTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> falltrapTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> killstreaks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> deathbans = new ConcurrentHashMap<>();
    private final Set<UUID> reclaimed = ConcurrentHashMap.newKeySet();
    private final Set<UUID> redeemed = ConcurrentHashMap.newKeySet();
    private final Set<UUID> cobbleDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> soundsDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> privateMessagesDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> staffBuild = ConcurrentHashMap.newKeySet();
    private final Set<UUID> strengthNerf = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<UUID, Set<UUID>> ignored = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> logoutUntil = new ConcurrentHashMap<>();

    public PlayerStateService(HCFSettings settings, ProfileService profiles) {
        this.settings = settings;
        this.profiles = profiles;
    }

    public void join(Player player) {
        join(player.getUniqueId());
    }

    public void join(UUID uuid) {
        sessionStart.put(uuid, System.currentTimeMillis());
        profiles.cached(uuid).ifPresentOrElse(profile -> {
            lives.putIfAbsent(uuid, (int) profile.statistics().getOrDefault("lives", (long) settings.defaultLives()).longValue());
            playtime.put(uuid, profile.statistic("playtime"));
            killstreaks.put(uuid, (int) profile.statistic("killstreak"));
            long deathbanUntil = profile.statistic("deathban_until");
            if (deathbanUntil > System.currentTimeMillis()) {
                deathbans.put(uuid, deathbanUntil);
            }
        }, () -> lives.putIfAbsent(uuid, settings.defaultLives()));
    }

    public void quit(Player player) {
        long started = sessionStart.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
        long total = playtime.merge(player.getUniqueId(), System.currentTimeMillis() - started, Long::sum);
        sessionStart.remove(player.getUniqueId());
        logoutUntil.remove(player.getUniqueId());
        persist(player.getUniqueId(), "playtime", total);
    }

    public long playtime(UUID uuid) {
        long stored = playtime.getOrDefault(uuid, 0L);
        Long started = sessionStart.get(uuid);
        return started == null ? stored : stored + (System.currentTimeMillis() - started);
    }

    public int lives(UUID uuid) {
        Integer cached = lives.get(uuid);
        if (cached != null) {
            return cached;
        }
        return profiles.cached(uuid)
                .map(profile -> (int) profile.statistics().getOrDefault("lives", (long) settings.defaultLives()).longValue())
                .orElse(settings.defaultLives());
    }

    public void lives(UUID uuid, int amount) {
        int next = Math.max(0, amount);
        lives.put(uuid, next);
        persist(uuid, "lives", next);
    }

    public int addLives(UUID uuid, int amount) {
        int next = lives.merge(uuid, amount, Integer::sum);
        if (next < 0) {
            next = 0;
            lives.put(uuid, 0);
        }
        persist(uuid, "lives", next);
        return next;
    }

    public boolean consumeLife(UUID uuid) {
        boolean consumed = consume(lives, uuid);
        if (consumed) {
            persist(uuid, "lives", lives(uuid));
        }
        return consumed;
    }

    public int baseTokens(UUID uuid) {
        return baseTokens.getOrDefault(uuid, 0);
    }

    public int addBaseTokens(UUID uuid, int amount) {
        return baseTokens.merge(uuid, amount, Integer::sum);
    }

    public boolean consumeBaseToken(UUID uuid) {
        return consume(baseTokens, uuid);
    }

    public int falltrapTokens(UUID uuid) {
        return falltrapTokens.getOrDefault(uuid, 0);
    }

    public int addFalltrapTokens(UUID uuid, int amount) {
        return falltrapTokens.merge(uuid, amount, Integer::sum);
    }

    public boolean consumeFalltrapToken(UUID uuid) {
        return consume(falltrapTokens, uuid);
    }

    public boolean toggleCobble(UUID uuid) {
        return toggle(cobbleDisabled, uuid);
    }

    public boolean cobbleDisabled(UUID uuid) {
        return cobbleDisabled.contains(uuid);
    }

    public boolean toggleSounds(UUID uuid) {
        return toggle(soundsDisabled, uuid);
    }

    public boolean soundsDisabled(UUID uuid) {
        return soundsDisabled.contains(uuid);
    }

    public boolean togglePm(UUID uuid) {
        return toggle(privateMessagesDisabled, uuid);
    }

    public boolean pmDisabled(UUID uuid) {
        return privateMessagesDisabled.contains(uuid);
    }

    public boolean toggleStaffBuild(UUID uuid) {
        return toggle(staffBuild, uuid);
    }

    public boolean staffBuild(UUID uuid) {
        return staffBuild.contains(uuid);
    }

    public boolean toggleStrengthNerf(UUID uuid) {
        return toggle(strengthNerf, uuid);
    }

    public boolean strengthNerf(UUID uuid) {
        return strengthNerf.contains(uuid);
    }

    public boolean toggleIgnore(UUID actor, UUID target) {
        Set<UUID> set = ignored.computeIfAbsent(actor, ignoredUuid -> ConcurrentHashMap.newKeySet());
        return toggle(set, target);
    }

    public boolean ignoring(UUID actor, UUID target) {
        return ignored.getOrDefault(actor, Set.of()).contains(target);
    }

    public boolean markRedeemed(UUID uuid) {
        return redeemed.add(uuid);
    }

    public void resetRedeem(UUID uuid) {
        redeemed.remove(uuid);
    }

    public boolean markReclaimed(UUID uuid) {
        return reclaimed.add(uuid);
    }

    public void resetReclaim(UUID uuid) {
        reclaimed.remove(uuid);
    }

    public int addKillstreak(UUID uuid, int amount) {
        int next = killstreaks.compute(uuid, (ignored, current) -> Math.max(0, (current == null ? 0 : current) + amount));
        persist(uuid, "killstreak", next);
        return next;
    }

    public int killstreak(UUID uuid) {
        return killstreaks.getOrDefault(uuid, 0);
    }

    public void deathban(UUID uuid, long millis) {
        long until = System.currentTimeMillis() + millis;
        deathbans.put(uuid, until);
        persist(uuid, "deathban_until", until);
    }

    public CompletableFuture<Boolean> revive(UUID uuid) {
        long now = System.currentTimeMillis();
        Long cachedUntil = deathbans.remove(uuid);
        return profiles.load(uuid).thenCompose(profile -> {
            long storedUntil = profile.statistic("deathban_until");
            boolean wasDeathbanned = (cachedUntil != null && cachedUntil > now) || storedUntil > now;
            profile.statistics().put("deathban_until", 0L);
            return profiles.save(profile).thenApply(ignored -> wasDeathbanned);
        });
    }

    public CompletableFuture<Long> deathbanRemainingAsync(UUID uuid) {
        long cached = deathbanRemaining(uuid);
        if (cached > 0L || profiles.cached(uuid).isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        return profiles.load(uuid).thenApply(profile -> Math.max(0L, profile.statistic("deathban_until") - System.currentTimeMillis()));
    }

    public long deathbanRemaining(UUID uuid) {
        long until = deathbans.getOrDefault(uuid, profiles.cached(uuid).map(profile -> profile.statistic("deathban_until")).orElse(0L));
        return Math.max(0L, until - System.currentTimeMillis());
    }

    public Map<UUID, Integer> killstreaks() {
        return Map.copyOf(killstreaks);
    }

    public void startLogout(UUID uuid, long millis) {
        logoutUntil.put(uuid, System.currentTimeMillis() + Math.max(1_000L, millis));
    }

    public void cancelLogout(UUID uuid) {
        logoutUntil.remove(uuid);
    }

    public boolean loggingOut(UUID uuid) {
        return logoutRemaining(uuid) > 0L;
    }

    public long logoutRemaining(UUID uuid) {
        long until = logoutUntil.getOrDefault(uuid, 0L);
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            logoutUntil.remove(uuid);
            return 0L;
        }
        return remaining;
    }

    private static boolean toggle(Set<UUID> set, UUID uuid) {
        if (set.remove(uuid)) {
            return false;
        }
        set.add(uuid);
        return true;
    }

    private static boolean consume(ConcurrentMap<UUID, Integer> map, UUID uuid) {
        while (true) {
            int current = map.getOrDefault(uuid, 0);
            if (current <= 0) {
                return false;
            }
            if (map.replace(uuid, current, current - 1)) {
                return true;
            }
            if (!map.containsKey(uuid) && map.putIfAbsent(uuid, current - 1) == null) {
                return true;
            }
        }
    }

    private void persist(UUID uuid, String key, long value) {
        profiles.cached(uuid).ifPresent(profile -> {
            profile.statistics().put(key, value);
            profiles.save(profile);
        });
    }
}
