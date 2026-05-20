package com.testrank.hcf.core.staff;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LastInventoryService implements HCFService {
    private final Map<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();

    public void capture(Player player) {
        snapshots.put(player.getUniqueId(), new Snapshot(copy(player.getInventory().getContents()), copy(player.getInventory().getArmorContents())));
    }

    public Inventory inventory(UUID uuid, String name) {
        Snapshot snapshot = snapshots.get(uuid);
        if (snapshot == null) {
            return null;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, "Last Inventory: " + name);
        ItemStack[] contents = snapshot.contents();
        for (int slot = 0; slot < contents.length && slot < 36; slot++) {
            inventory.setItem(slot, clone(contents[slot]));
        }
        ItemStack[] armor = snapshot.armor();
        for (int index = 0; index < armor.length && 45 + index < 54; index++) {
            inventory.setItem(45 + index, clone(armor[index]));
        }
        return inventory;
    }

    private static ItemStack[] copy(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) {
            copy[index] = clone(source[index]);
        }
        return copy;
    }

    private static ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private record Snapshot(ItemStack[] contents, ItemStack[] armor) {}
}
