package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.claim.ClaimSelectionService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.claim.ClaimType;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.particle.ParticleIntelService;
import com.testrank.hcf.core.team.TeamRole;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.util.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimWandListener implements Listener {
    private final ClaimSelectionService selections;
    private final ClaimService claims;
    private final TeamService teams;
    private final EconomyService economy;
    private final HCFSettings settings;
    private final Threading threading;
    private final ParticleIntelService intel;
    private final Set<UUID> purchasing = ConcurrentHashMap.newKeySet();

    public ClaimWandListener(ClaimSelectionService selections, ClaimService claims, TeamService teams, EconomyService economy, HCFSettings settings, Threading threading,
                             ParticleIntelService intel) {
        this.selections = selections;
        this.claims = claims;
        this.teams = teams;
        this.economy = economy;
        this.settings = settings;
        this.threading = threading;
        this.intel = intel;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getClickedBlock() == null || event.getItem().getType() != ClaimSelectionService.WAND_MATERIAL) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking()) {
            claim(event);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selections.first(event.getPlayer(), event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(Text.color("&8[&cClaim&8] &fFirst position set at &c" + loc(event.getClickedBlock().getLocation()) + "&f."));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selections.second(event.getPlayer(), event.getClickedBlock().getLocation());
            selections.selection(event.getPlayer()).ifPresentOrElse(selection -> {
                int area = area(selection.first(), selection.second());
                long price = price(area);
                event.getPlayer().sendMessage(Text.color("&8[&cClaim&8] &fSecond position set. &7Size: &c" + area + " blocks &7Price: &a$" + price + "&f."));
                event.getPlayer().sendMessage(Text.color("&8[&cClaim&8] &7Sneak-left-click with the wand to purchase this claim."));
            }, () -> event.getPlayer().sendMessage(Text.color("&8[&cClaim&8] &fSecond position set at &c" + loc(event.getClickedBlock().getLocation()) + "&f.")));
        }
    }

    private void claim(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!purchasing.add(player.getUniqueId())) {
            player.sendMessage(Text.color("&8[&cClaim&8] &cYour previous claim purchase is still processing."));
            return;
        }
        var team = teams.byPlayer(player.getUniqueId()).orElse(null);
        if (team == null) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cYou must be in a faction to claim."));
            return;
        }
        if (!teams.canManage(player.getUniqueId(), team, TeamRole.CAPTAIN)) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cYou must be captain or above to claim."));
            return;
        }
        long existingClaims = claims.claims().stream().filter(claim -> team.id().equals(claim.owner())).count();
        if (existingClaims >= settings.factionMaxClaims()) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cYour faction already has the maximum amount of claims."));
            return;
        }
        var selection = selections.selection(player);
        if (selection.isEmpty()) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cSelect two corners first."));
            return;
        }
        String world = selection.get().first().getWorld().getName();
        if (!claimingAllowedInWorld(world)) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cYou cannot claim in &f" + world + "&c."));
            return;
        }
        int firstX = selection.get().first().getBlockX();
        int firstZ = selection.get().first().getBlockZ();
        int secondX = selection.get().second().getBlockX();
        int secondZ = selection.get().second().getBlockZ();
        if (tooCloseToSpawn(selection.get().first().getWorld(), firstX, firstZ, secondX, secondZ)) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cThis claim is too close to spawn or warzone."));
            return;
        }
        int width = Math.abs(selection.get().first().getBlockX() - selection.get().second().getBlockX()) + 1;
        int length = Math.abs(selection.get().first().getBlockZ() - selection.get().second().getBlockZ()) + 1;
        if (width < settings.claimMinimumSize() || length < settings.claimMinimumSize()) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cClaims must be at least &f" + settings.claimMinimumSize() + "x" + settings.claimMinimumSize() + "&c."));
            return;
        }
        if (width > settings.claimMaximumSize() || length > settings.claimMaximumSize()) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cClaims cannot be larger than &f" + settings.claimMaximumSize() + "x" + settings.claimMaximumSize() + "&c."));
            return;
        }
        int area = area(selection.get().first(), selection.get().second());
        long price = price(area);
        if (team.balance() < price) {
            purchasing.remove(player.getUniqueId());
            player.sendMessage(Text.color("&8[&cClaim&8] &cYour faction needs &a$" + price + " &cto purchase this claim. Deposit money with &f/f deposit&c."));
            return;
        }
        String claimName = nextClaimName(team.name(), (int) existingClaims + 1);
        double previousBalance = team.balance();
        team.balance(previousBalance - price);
        teams.save(team).thenCompose(ignored -> claims.create(team.id(), claimName, world, firstX, firstZ, secondX, secondZ, ClaimType.PLAYER))
                .thenAccept(claim -> threading.runSync(() -> {
                    purchasing.remove(player.getUniqueId());
                    removeWand(player);
                    selections.clear(player);
                    intel.claimIntel(player, team, claim, price, area);
                    player.sendMessage(Text.color("&8[&cClaim&8] &fPurchased &c" + area + " blocks &ffor &a$" + price + " &ffrom faction balance."));
                }))
                .exceptionally(throwable -> {
                    team.balance(previousBalance);
                    teams.save(team);
                    purchasing.remove(player.getUniqueId());
                    threading.runSync(() -> {
                        String reason = rootMessage(throwable);
                        intel.raidAbuse(player, team, "claim_create_failed", java.util.Map.of(
                                "reason", reason,
                                "area", area,
                                "price", price
                        ));
                        player.sendMessage(Text.color("&8[&cClaim&8] &cCould not create claim: " + reason));
                    });
                    return null;
                });
    }

    private void removeWand(org.bukkit.entity.Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand != null && hand.getType() == ClaimSelectionService.WAND_MATERIAL) {
            if (hand.getAmount() <= 1) {
                player.setItemInHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
                player.setItemInHand(hand);
            }
            player.updateInventory();
            return;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() == ClaimSelectionService.WAND_MATERIAL) {
                if (item.getAmount() <= 1) {
                    player.getInventory().setItem(slot, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItem(slot, item);
                }
                player.updateInventory();
                return;
            }
        }
    }

    private int area(org.bukkit.Location first, org.bukkit.Location second) {
        return (Math.abs(first.getBlockX() - second.getBlockX()) + 1) * (Math.abs(first.getBlockZ() - second.getBlockZ()) + 1);
    }

    private long price(int area) {
        return Math.max(settings.claimMinimumPrice(), Math.round(area * settings.claimPricePerBlock()));
    }

    private boolean claimingAllowedInWorld(String world) {
        if (settings.claimAllowedWorlds().isEmpty()) {
            return true;
        }
        for (String allowed : settings.claimAllowedWorlds()) {
            if (allowed.equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    private boolean tooCloseToSpawn(org.bukkit.World world, int firstX, int firstZ, int secondX, int secondZ) {
        int minimum = settings.claimMinimumSpawnDistance();
        if (minimum <= 0 || world == null) {
            return false;
        }
        org.bukkit.Location spawn = world.getSpawnLocation();
        int minX = Math.min(firstX, secondX);
        int maxX = Math.max(firstX, secondX);
        int minZ = Math.min(firstZ, secondZ);
        int maxZ = Math.max(firstZ, secondZ);
        int closestX = Math.max(minX, Math.min(spawn.getBlockX(), maxX));
        int closestZ = Math.max(minZ, Math.min(spawn.getBlockZ(), maxZ));
        int dx = closestX - spawn.getBlockX();
        int dz = closestZ - spawn.getBlockZ();
        return (dx * dx + dz * dz) < minimum * minimum;
    }

    private String nextClaimName(String teamName, int nextNumber) {
        String base = teamName;
        if (claims.byName(base).isEmpty()) {
            return base;
        }
        for (int i = Math.max(2, nextNumber); i < nextNumber + 100; i++) {
            String candidate = teamName + "-" + i;
            if (claims.byName(candidate).isEmpty()) {
                return candidate;
            }
        }
        return teamName + "-" + UUID.randomUUID().toString().substring(0, 4).toLowerCase(Locale.ROOT);
    }

    private String loc(org.bukkit.Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }
}
