package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.settings.PlayerSettingsService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public final class SettingsGameplayListener implements Listener {
    private final PlayerSettingsService settings;

    public SettingsGameplayListener(PlayerSettingsService settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.COBBLESTONE
                && !settings.enabled(event.getPlayer().getUniqueId(), "cobblestone-pickup")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !settings.enabled(killer.getUniqueId(), "mob-drops")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
