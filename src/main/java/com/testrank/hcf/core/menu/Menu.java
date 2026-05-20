package com.testrank.hcf.core.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public abstract class Menu {
    public abstract String title(Player player);

    public int size() {
        return 54;
    }

    public abstract Map<Integer, Button> buttons(Player player);

    public void open(Player player, MenuService menus) {
        Inventory inventory = Bukkit.createInventory(null, size(), title(player));
        Map<Integer, Button> buttons = buttons(player);
        buttons.forEach((slot, button) -> inventory.setItem(slot, button.icon(player)));
        menus.track(player, inventory, buttons, this);
        player.openInventory(inventory);
    }
}
