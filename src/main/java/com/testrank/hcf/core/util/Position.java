package com.testrank.hcf.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Optional;

public record Position(String world, double x, double y, double z, float yaw, float pitch) {
    public static Position from(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location must include a world.");
        }
        return new Position(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Optional<Location> toLocation() {
        var bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? Optional.empty() : Optional.of(new Location(bukkitWorld, x, y, z, yaw, pitch));
    }
}
