package com.testrank.hcf.core.staff;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffService implements HCFService {
    private static final Set<String> STAFF_ITEM_NAMES = Set.of(
            "random teleport",
            "inventory inspector",
            "freeze player",
            "vanish enabled",
            "vanish disabled",
            "online staff",
            "staff settings"
    );

    private final Plugin plugin;
    private final ClientIntegrationService clients;
    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, InventorySnapshot> storedInventories = new ConcurrentHashMap<>();

    public StaffService(Plugin plugin, ClientIntegrationService clients) {
        this.plugin = plugin;
        this.clients = clients;
    }

    public boolean toggleStaff(Player player) {
        boolean enabled = staffMode.add(player.getUniqueId());
        if (!enabled) {
            staffMode.remove(player.getUniqueId());
            vanished.remove(player.getUniqueId());
            restoreInventory(player);
        } else {
            vanished.add(player.getUniqueId());
            applyStaffInventory(player);
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        applyVanish(player);
        clients.staffModules(player, enabled);
        return enabled;
    }

    public boolean staffMode(Player player) {
        return staffMode.contains(player.getUniqueId());
    }

    public boolean toggleVanish(Player player) {
        boolean enabled = vanished.add(player.getUniqueId());
        if (!enabled) {
            vanished.remove(player.getUniqueId());
        }
        applyVanish(player);
        if (staffMode(player)) {
            updateVanishItem(player);
        }
        return enabled;
    }

    public long staffOnline() {
        return Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("hcf.staff")).count();
    }

    public void freeze(Player player, boolean value) {
        if (value) {
            frozen.add(player.getUniqueId());
        } else {
            frozen.remove(player.getUniqueId());
        }
    }

    public boolean frozen(Player player) {
        return frozen.contains(player.getUniqueId());
    }

    public boolean vanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void applyVanish(Player subject) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.hasPermission("hcf.staff")) {
                viewer.showPlayer(subject);
            } else if (vanished(subject)) {
                viewer.hidePlayer(subject);
            } else {
                viewer.showPlayer(subject);
            }
        }
    }

    public void handleJoin(Player player) {
        for (Player subject : Bukkit.getOnlinePlayers()) {
            applyVanish(subject);
        }
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasStaff = staffMode.remove(uuid);
        vanished.remove(uuid);
        frozen.remove(uuid);
        if (wasStaff) {
            restoreInventory(player);
            clients.staffModules(player, false);
        } else {
            storedInventories.remove(uuid);
        }
    }

    public boolean staffItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        String stripped = ChatColor.stripColor(meta.getDisplayName());
        return stripped != null && STAFF_ITEM_NAMES.contains(stripped.toLowerCase(Locale.ROOT));
    }

    private void applyStaffInventory(Player player) {
        storedInventories.putIfAbsent(player.getUniqueId(), new InventorySnapshot(
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone(),
                player.getAllowFlight(),
                player.isFlying()));
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(0, item(Material.COMPASS, "§cRandom Teleport"));
        player.getInventory().setItem(1, item(Material.BOOK, "§cInventory Inspector"));
        player.getInventory().setItem(3, item(Material.PACKED_ICE, "§bFreeze Player"));
        updateVanishItem(player);
        player.getInventory().setItem(7, item(Material.WATCH, "§cOnline Staff"));
        player.getInventory().setItem(8, item(Material.QUARTZ, "§fStaff Settings"));
        player.updateInventory();
    }

    private void restoreInventory(Player player) {
        InventorySnapshot snapshot = storedInventories.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        player.getInventory().setContents(snapshot.contents());
        player.getInventory().setArmorContents(snapshot.armor());
        if (!snapshot.allowFlight() || !snapshot.flying()) {
            player.setFlying(false);
        }
        player.setAllowFlight(snapshot.allowFlight());
        if (snapshot.allowFlight()) {
            player.setFlying(snapshot.flying());
        }
        player.updateInventory();
    }

    private void updateVanishItem(Player player) {
        player.getInventory().setItem(4, item(Material.INK_SACK, vanished(player) ? "§aVanish Enabled" : "§cVanish Disabled"));
    }

    private ItemStack item(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void close() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (staffMode(player)) {
                restoreInventory(player);
                clients.staffModules(player, false);
            }
        }
        staffMode.clear();
        vanished.clear();
        frozen.clear();
        storedInventories.clear();
    }

    private record InventorySnapshot(ItemStack[] contents, ItemStack[] armor, boolean allowFlight, boolean flying) {}
}
