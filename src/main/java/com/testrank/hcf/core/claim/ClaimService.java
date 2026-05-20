package com.testrank.hcf.core.claim;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClaimService implements HCFService {
    private final ClaimRepository repository;
    private final ClaimIndex index = new ClaimIndex();
    private final ConcurrentMap<UUID, Claim> claims = new ConcurrentHashMap<>();

    public ClaimService(ClaimRepository repository) {
        this.repository = repository;
    }

    public Optional<Claim> at(Location location) {
        return index.at(location);
    }

    public Optional<Claim> byName(String name) {
        return claims.values().stream().filter(claim -> claim.name().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public void start() {
        repository.loadAll().thenAccept(loaded -> {
            for (Claim claim : loaded) {
                claims.put(claim.id(), claim);
            }
            index.rebuild(claims.values());
        });
    }

    public CompletableFuture<Claim> create(UUID owner, String name, String world, int minX, int minZ, int maxX, int maxZ, ClaimType type) {
        Claim claim;
        synchronized (this) {
            if (byName(name).isPresent()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("A claim with that name already exists."));
            }
            for (Claim existing : claims.values()) {
                if (existing.overlaps(world, minX, minZ, maxX, maxZ)) {
                    return CompletableFuture.failedFuture(new IllegalArgumentException("Claim overlaps " + existing.name() + "."));
                }
            }
            claim = new Claim(UUID.randomUUID(), owner, name, world, minX, minZ, maxX, maxZ, type);
            claims.put(claim.id(), claim);
            index.rebuild(claims.values());
        }
        return repository.save(claim).thenApply(ignored -> claim).exceptionally(throwable -> {
            synchronized (this) {
                claims.remove(claim.id());
                index.rebuild(claims.values());
            }
            throw new java.util.concurrent.CompletionException(throwable);
        });
    }

    public Collection<Claim> claims() {
        return claims.values();
    }
}
