package com.testrank.hcf.core.menu;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;

    private ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public ItemBuilder name(String name) {
        var meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        var meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return item.clone();
    }
}
