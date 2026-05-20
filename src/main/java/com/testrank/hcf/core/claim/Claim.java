package com.testrank.hcf.core.claim;

import org.bukkit.Location;

import java.util.UUID;

public record Claim(
        UUID id,
        UUID owner,
        String name,
        String world,
        int minX,
        int minZ,
        int maxX,
        int maxZ,
        ClaimType type
) {
    public Claim {
        int realMinX = Math.min(minX, maxX);
        int realMaxX = Math.max(minX, maxX);
        int realMinZ = Math.min(minZ, maxZ);
        int realMaxZ = Math.max(minZ, maxZ);
        minX = realMinX;
        maxX = realMaxX;
        minZ = realMinZ;
        maxZ = realMaxZ;
    }

    public boolean contains(Location location) {
        return location.getWorld() != null
                && world.equals(location.getWorld().getName())
                && location.getBlockX() >= minX
                && location.getBlockX() <= maxX
                && location.getBlockZ() >= minZ
                && location.getBlockZ() <= maxZ;
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        return maxX >= chunkMinX && minX <= chunkMaxX && maxZ >= chunkMinZ && minZ <= chunkMaxZ;
    }

    public boolean overlaps(String otherWorld, int otherMinX, int otherMinZ, int otherMaxX, int otherMaxZ) {
        int realMinX = Math.min(otherMinX, otherMaxX);
        int realMaxX = Math.max(otherMinX, otherMaxX);
        int realMinZ = Math.min(otherMinZ, otherMaxZ);
        int realMaxZ = Math.max(otherMinZ, otherMaxZ);
        return world.equals(otherWorld)
                && maxX >= realMinX
                && minX <= realMaxX
                && maxZ >= realMinZ
                && minZ <= realMaxZ;
    }
}
