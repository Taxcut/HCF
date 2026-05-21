package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.claim.ClaimSelectionService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimProtectionListener implements Listener {
    private static final Set<String> PROTECTED_INTERACTS = Set.of(
            "CHEST", "TRAPPED_CHEST", "ENDER_CHEST", "FURNACE", "BURNING_FURNACE", "BREWING_STAND", "HOPPER",
            "DISPENSER", "DROPPER", "WORKBENCH", "ANVIL", "ENCHANTMENT_TABLE", "BEACON",
            "WOODEN_DOOR", "IRON_DOOR_BLOCK", "ACACIA_DOOR", "BIRCH_DOOR", "DARK_OAK_DOOR", "JUNGLE_DOOR", "SPRUCE_DOOR",
            "TRAP_DOOR", "FENCE_GATE", "ACACIA_FENCE_GATE", "BIRCH_FENCE_GATE", "DARK_OAK_FENCE_GATE", "JUNGLE_FENCE_GATE", "SPRUCE_FENCE_GATE",
            "LEVER", "STONE_BUTTON", "WOOD_BUTTON", "DIODE_BLOCK_ON", "DIODE_BLOCK_OFF", "REDSTONE_COMPARATOR_ON", "REDSTONE_COMPARATOR_OFF"
    );

    private final ClaimService claims;
    private final TeamService teams;
    private final PlayerStateService states;
    private final ConcurrentHashMap<UUID, Long> lastMessage = new ConcurrentHashMap<>();

    public ClaimProtectionListener(ClaimService claims, TeamService teams, PlayerStateService states) {
        this.claims = claims;
        this.teams = teams;
        this.states = states;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Claim claim = claims.at(event.getBlock().getLocation()).orElse(null);
        if (!canBuild(event.getPlayer(), claim)) {
            event.setCancelled(true);
            deny(event.getPlayer(), claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Claim claim = claims.at(event.getBlockPlaced().getLocation()).orElse(null);
        if (!canBuild(event.getPlayer(), claim)) {
            event.setCancelled(true);
            deny(event.getPlayer(), claim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getItem() != null && event.getItem().getType() == ClaimSelectionService.WAND_MATERIAL) {
            return;
        }
        Block block = event.getClickedBlock();
        Material type = block.getType();
        if (!PROTECTED_INTERACTS.contains(type.name())) {
            return;
        }
        Claim claim = claims.at(block.getLocation()).orElse(null);
        if (!canBuild(event.getPlayer(), claim)) {
            event.setCancelled(true);
            deny(event.getPlayer(), claim);
        }
    }

    private boolean canBuild(Player player, Claim claim) {
        if (claim == null) {
            return true;
        }
        if (player.hasPermission("hcf.admin") && states.staffBuild(player.getUniqueId())) {
            return true;
        }
        return teams.byPlayer(player.getUniqueId())
                .map(team -> team.id().equals(claim.owner()))
                .orElse(false);
    }

    private void deny(Player player, Claim claim) {
        long now = System.currentTimeMillis();
        long previous = lastMessage.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < 1_500L) {
            return;
        }
        lastMessage.put(player.getUniqueId(), now);
        String name = claim == null ? "this claim" : claim.name();
        player.sendMessage(Text.color("&8[&cClaim&8] &cYou cannot build or interact in &f" + name + "&c."));
    }
}
