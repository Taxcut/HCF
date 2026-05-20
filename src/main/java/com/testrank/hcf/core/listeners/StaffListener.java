package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffListener implements Listener {
    private final StaffService staff;
    private final PlayerStateService states;
    private final Map<UUID, Long> buildWarnings = new ConcurrentHashMap<>();

    public StaffListener(StaffService staff, PlayerStateService states) {
        this.staff = staff;
        this.states = states;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!staff.frozen(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
            event.getPlayer().sendMessage(Text.color("&cYou are frozen."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staff.staffMode(player) || event.getItem() == null || !rightClick(event)) {
            return;
        }
        ItemStack item = event.getItem();
        if (!staff.staffItem(item)) {
            return;
        }
        event.setCancelled(true);
        if (item.getType() == Material.COMPASS) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(target -> target != player && !target.hasPermission("hcf.staff"))
                    .findAny()
                    .ifPresentOrElse(target -> {
                        player.teleport(target);
                        player.sendMessage(Text.color("&8[&cStaff&8] &fTeleported to &c" + target.getName() + "&f."));
                    }, () -> player.sendMessage(Text.color("&8[&cStaff&8] &cNo players available.")));
        } else if (item.getType() == Material.INK_SACK) {
            boolean enabled = staff.toggleVanish(player);
            player.sendMessage(Text.color("&8[&cStaff&8] &fVanish: " + (enabled ? "&aEnabled" : "&cDisabled")));
        } else if (item.getType() == Material.WATCH) {
            player.sendMessage(Text.color("&8[&cStaff&8] &fStaff online: &c" + staff.staffOnline()));
        } else if (item.getType() == Material.QUARTZ) {
            player.performCommand("settings");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!staff.staffMode(player) || !(event.getRightClicked() instanceof Player target)) {
            return;
        }
        ItemStack item = player.getItemInHand();
        if (!staff.staffItem(item)) {
            return;
        }
        event.setCancelled(true);
        if (item.getType() == Material.BOOK) {
            player.openInventory(target.getInventory());
            player.sendMessage(Text.color("&8[&cStaff&8] &fInspecting &c" + target.getName() + "&f."));
        } else if (item.getType() == Material.PACKED_ICE) {
            player.performCommand("freeze " + target.getName());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !staff.staffMode(player)) {
            return;
        }
        boolean hotbarStaffItem = event.getHotbarButton() >= 0
                && staff.staffItem(player.getInventory().getItem(event.getHotbarButton()));
        if (staff.staffItem(event.getCurrentItem()) || staff.staffItem(event.getCursor()) || hotbarStaffItem) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (staff.staffMode(event.getPlayer()) && staff.staffItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (staff.staffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (protectBuild(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (protectBuild(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        staff.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        buildWarnings.remove(event.getPlayer().getUniqueId());
        staff.handleQuit(event.getPlayer());
    }

    private boolean rightClick(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean protectBuild(Player player) {
        if (!staff.staffMode(player) || states.staffBuild(player.getUniqueId())) {
            return false;
        }
        long now = System.currentTimeMillis();
        long next = buildWarnings.getOrDefault(player.getUniqueId(), 0L);
        if (next <= now) {
            buildWarnings.put(player.getUniqueId(), now + 1500L);
            player.sendMessage(Text.color("&8[&cStaff&8] &cStaff build is disabled. Use &f/staffbuild &cto edit blocks."));
        }
        return true;
    }
}
