package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.profile.ProfileService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.EnumSet;
import java.util.Set;

public final class StatsTrackingListener implements Listener {
    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE,
            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.REDSTONE_ORE,
            Material.GLOWING_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.QUARTZ_ORE
    );

    private final ProfileService profiles;

    public StatsTrackingListener(ProfileService profiles) {
        this.profiles = profiles;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        if (!ORES.contains(event.getBlock().getType())) {
            return;
        }
        profiles.cached(event.getPlayer().getUniqueId()).ifPresent(profile -> {
            profile.addStatistic("ores_mined", 1);
            profiles.save(profile);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        Projectile potion = event.getPotion();
        if (!(potion.getShooter() instanceof Player player)) {
            return;
        }
        profiles.cached(player.getUniqueId()).ifPresent(profile -> {
            profile.addStatistic("potions_splashed", 1);
            profiles.save(profile);
        });
    }
}
