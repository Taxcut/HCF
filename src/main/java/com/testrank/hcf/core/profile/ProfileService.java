package com.testrank.hcf.core.profile;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.redis.RedisManager;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProfileService implements HCFService {
    private final ProfileRepository repository;
    private final RedisManager redis;
    private final ConcurrentMap<UUID, Profile> cache = new ConcurrentHashMap<>();

    public ProfileService(ProfileRepository repository, RedisManager redis) {
        this.repository = repository;
        this.redis = redis;
    }

    public CompletableFuture<Profile> load(UUID uuid) {
        Profile cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.load(uuid).thenApply(profile -> {
            Profile existing = cache.putIfAbsent(uuid, profile);
            return existing == null ? profile : existing;
        });
    }

    @Override
    public void start() {
        redis.subscribe("profiles", envelope -> {
            try {
                UUID uuid = UUID.fromString(envelope.payload());
                cached(uuid).filter(profile -> profile.player().isEmpty()).ifPresent(profile -> cache.remove(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    public Optional<Profile> cached(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public Collection<Profile> cachedProfiles() {
        return cache.values();
    }

    public CompletableFuture<Void> save(Profile profile) {
        profile.seenNow();
        return repository.save(profile).thenCompose(ignored -> redis.publish("profiles", profile.uuid().toString()));
    }

    public CompletableFuture<Void> unload(UUID uuid) {
        Profile profile = cache.remove(uuid);
        return profile == null ? CompletableFuture.completedFuture(null) : save(profile);
    }

    @Override
    public void close() {
        CompletableFuture.allOf(cache.values().stream().map(this::save).toArray(CompletableFuture[]::new)).join();
    }
}
