package com.testrank.hcf.core.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface Button {
    ItemStack icon(Player player);

    default void click(Player player, ClickType clickType) {}
}
