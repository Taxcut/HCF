package com.testrank.hcf.core.claim;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClaimIndex {
    private final ConcurrentMap<String, List<Claim>> chunkClaims = new ConcurrentHashMap<>();

    public void rebuild(Iterable<Claim> claims) {
        ConcurrentMap<String, List<Claim>> next = new ConcurrentHashMap<>();
        for (Claim claim : claims) {
            int minChunkX = claim.minX() >> 4;
            int maxChunkX = claim.maxX() >> 4;
            int minChunkZ = claim.minZ() >> 4;
            int maxChunkZ = claim.maxZ() >> 4;
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    next.computeIfAbsent(key(claim.world(), cx, cz), ignored -> new ArrayList<>(2)).add(claim);
                }
            }
        }
        chunkClaims.clear();
        chunkClaims.putAll(next);
    }

    public Optional<Claim> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        List<Claim> candidates = chunkClaims.get(key(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        for (Claim claim : candidates) {
            if (claim.contains(location)) {
                return Optional.of(claim);
            }
        }
        return Optional.empty();
    }

    private static String key(String world, int chunkX, int chunkZ) {
        return world + ':' + chunkX + ':' + chunkZ;
    }
}
