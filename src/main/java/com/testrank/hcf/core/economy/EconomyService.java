package com.testrank.hcf.core.economy;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.redis.RedisManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EconomyService implements HCFService {
    private final ProfileService profiles;
    private final RedisManager redis;
    private final ConcurrentMap<UUID, Long> balances = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Object> locks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<Void>> writeQueues = new ConcurrentHashMap<>();

    public EconomyService(ProfileService profiles, RedisManager redis) {
        this.profiles = profiles;
        this.redis = redis;
    }

    public long balance(UUID uuid) {
        Long cached = balances.get(uuid);
        if (cached != null) {
            return cached;
        }
        long loaded = profiles.cached(uuid).map(profile -> profile.statistic("balance")).orElse(0L);
        if (loaded > 0L) {
            balances.putIfAbsent(uuid, loaded);
        }
        return loaded;
    }

    public java.util.Map<UUID, Long> balances() {
        return java.util.Map.copyOf(balances);
    }

    public CompletableFuture<Long> add(UUID uuid, long amount) {
        if (amount <= 0L) {
            return CompletableFuture.completedFuture(balance(uuid));
        }
        long next;
        synchronized (lock(uuid)) {
            next = balances.merge(uuid, amount, Long::sum);
        }
        return enqueuePersist(uuid, next).thenApply(ignored -> next);
    }

    public CompletableFuture<Boolean> withdraw(UUID uuid, long amount) {
        if (amount <= 0L) {
            return CompletableFuture.completedFuture(false);
        }
        long next;
        synchronized (lock(uuid)) {
            long current = balance(uuid);
            if (current < amount) {
                return CompletableFuture.completedFuture(false);
            }
            next = current - amount;
            balances.put(uuid, next);
        }
        return enqueuePersist(uuid, next).thenApply(ignored -> true);
    }

    public CompletableFuture<Long> set(UUID uuid, long amount) {
        long next = Math.max(0L, amount);
        synchronized (lock(uuid)) {
            balances.put(uuid, next);
        }
        return enqueuePersist(uuid, next).thenApply(ignored -> next);
    }

    private CompletableFuture<Void> enqueuePersist(UUID uuid, long balance) {
        CompletableFuture<Void> queued = writeQueues.compute(uuid, (ignored, previous) -> {
            CompletableFuture<Void> base = previous == null ? CompletableFuture.completedFuture(null) : previous.exceptionally(throwable -> null);
            return base.thenCompose(done -> persistNow(uuid, balance));
        });
        return queued.whenComplete((ignored, throwable) -> writeQueues.remove(uuid, queued));
    }

    private CompletableFuture<Void> persistNow(UUID uuid, long balance) {
        profiles.cached(uuid).ifPresent(profile -> profile.statistics().put("balance", balance));
        CompletableFuture<Void> profileSave = profiles.cached(uuid)
                .map(profiles::save)
                .orElseGet(() -> CompletableFuture.completedFuture(null));
        return profileSave.thenCompose(ignored -> redis.hset("hcf:economy", uuid.toString(), Long.toString(balance)));
    }

    private Object lock(UUID uuid) {
        return locks.computeIfAbsent(uuid, ignored -> new Object());
    }
}
