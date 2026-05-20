package com.testrank.hcf.core.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ReadOnlyInventoryListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!readOnly(event.getInventory().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!readOnly(event.getInventory().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            player.updateInventory();
        }
    }

    private boolean readOnly(String title) {
        return title != null && title.startsWith("Last Inventory:");
    }
}
