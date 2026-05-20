package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class MenuListener implements Listener {
    private final MenuService menus;

    public MenuListener(MenuService menus) {
        this.menus = menus;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        MenuService.MenuState state = menus.state(player);
        if (state == null || !isTrackedTop(event.getView().getTopInventory(), state.inventory())) {
            return;
        }
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= state.inventory().getSize()) {
            player.updateInventory();
            return;
        }
        var button = state.buttons().get(rawSlot);
        if (button != null) {
            button.click(player, event.getClick());
        }
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        MenuService.MenuState state = menus.state(player);
        if (state == null || !isTrackedTop(event.getView().getTopInventory(), state.inventory())) {
            return;
        }
        event.setCancelled(true);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player
                && menus.state(player) != null
                && isTrackedTop(event.getInventory(), menus.state(player).inventory())) {
            menus.clear(player);
        }
    }

    private boolean isTrackedTop(Inventory current, Inventory tracked) {
        return current != null && tracked != null && current.equals(tracked);
    }
}
