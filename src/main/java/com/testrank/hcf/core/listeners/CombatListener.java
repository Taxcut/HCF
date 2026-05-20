package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.combat.AntiCleanService;
import com.testrank.hcf.core.combat.CombatService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.staff.LastInventoryService;
import com.testrank.hcf.core.team.DtrService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.timer.CooldownService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class CombatListener implements Listener {
    private final Plugin plugin;
    private final CombatService combat;
    private final AntiCleanService antiClean;
    private final CooldownService cooldowns;
    private final ProfileService profiles;
    private final PlayerStateService states;
    private final HCFSettings settings;
    private final DtrService dtr;
    private final TeamService teams;
    private final LastInventoryService lastInventories;
    private final PlayerSettingsService playerSettings;

    public CombatListener(Plugin plugin, CombatService combat, AntiCleanService antiClean, CooldownService cooldowns, ProfileService profiles, PlayerStateService states,
                          HCFSettings settings, DtrService dtr, TeamService teams, LastInventoryService lastInventories, PlayerSettingsService playerSettings) {
        this.plugin = plugin;
        this.combat = combat;
        this.antiClean = antiClean;
        this.cooldowns = cooldowns;
        this.profiles = profiles;
        this.states = states;
        this.settings = settings;
        this.dtr = dtr;
        this.teams = teams;
        this.lastInventories = lastInventories;
        this.playerSettings = playerSettings;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            Player attacker = attacker(event);
            if (attacker == null || attacker.equals(victim)) {
                return;
            }
            var attackerTeam = teams.byPlayer(attacker.getUniqueId());
            var victimTeam = teams.byPlayer(victim.getUniqueId());
            if (attackerTeam.isPresent() && victimTeam.isPresent() && attackerTeam.get().id().equals(victimTeam.get().id()) && !attackerTeam.get().friendlyFire()) {
                event.setCancelled(true);
                attacker.sendMessage(Text.color("&8[&cTeam&8] &cFriendly fire is disabled."));
                return;
            }
            AntiCleanService.DenyResult deny = antiClean.deny(attacker, victim);
            if (deny.denied()) {
                event.setCancelled(true);
                attacker.sendMessage(Text.color("&8[&cAntiClean&8] &c" + deny.message()));
                return;
            }
            combat.tag(attacker, victim);
            focusByHit(attacker, victim);
            antiClean.record(attacker, victim);
            profiles.cached(attacker.getUniqueId()).ifPresent(profile -> profile.addStatistic("damage_dealt", Math.round(event.getFinalDamage() * 10.0D)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        Player player = event.getPlayer();
        Material type = event.getItem().getType();
        if (type == Material.ENDER_PEARL && rightClick(event) && cooldowns.has(player.getUniqueId(), "enderpearl")) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            cooldowns.denyMessage(player.getUniqueId(), "enderpearl", "Enderpearl").ifPresent(message -> player.sendMessage(Text.color("&c" + message)));
        } else if (type == Material.FISHING_ROD && rightClick(event) && cooldowns.has(player.getUniqueId(), "rod")) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            cooldowns.denyMessage(player.getUniqueId(), "rod", "Fishing rod").ifPresent(message -> player.sendMessage(Text.color("&c" + message)));
        } else if (type == Material.FISHING_ROD && rightClick(event)) {
            cooldowns.put(player.getUniqueId(), "rod", settings.rodSeconds() * 1000L);
        } else if (type == Material.GOLDEN_APPLE && cooldowns.has(player.getUniqueId(), "gapple")) {
            event.setCancelled(true);
            cooldowns.denyMessage(player.getUniqueId(), "gapple", "Golden apple").ifPresent(message -> player.sendMessage(Text.color("&c" + message)));
        } else if (type == Material.GOLDEN_APPLE) {
            cooldowns.put(player.getUniqueId(), "gapple", settings.goldenAppleSeconds() * 1000L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl) || !(pearl.getShooter() instanceof Player player)) {
            return;
        }
        if (cooldowns.has(player.getUniqueId(), "enderpearl")) {
            event.setCancelled(true);
            pearl.remove();
            Bukkit.getScheduler().runTaskLater(plugin, () -> refund(player, Material.ENDER_PEARL), 1L);
            cooldowns.denyMessage(player.getUniqueId(), "enderpearl", "Enderpearl").ifPresent(message -> player.sendMessage(Text.color("&c" + message)));
            return;
        }
        cooldowns.put(player.getUniqueId(), "enderpearl", settings.pearlSeconds() * 1000L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        lastInventories.capture(victim);
        profiles.cached(victim.getUniqueId()).ifPresent(profile -> {
            profile.addDeath(event.getDeathMessage() == null ? "Unknown" : event.getDeathMessage());
            profiles.save(profile);
        });
        java.util.Optional<com.testrank.hcf.core.profile.Profile> killerProfile = combat.lastHit(victim.getUniqueId()).flatMap(profiles::cached);
        if (killerProfile.isPresent()) {
            var killer = killerProfile.get();
            killer.addKill();
            if (antiClean.fight(killer.uuid()).isPresent()) {
                killer.addStatistic("teamfight_kills", 1);
            }
            profiles.save(killer);
            states.addKillstreak(killer.uuid(), 1);
            Player killerPlayer = Bukkit.getPlayer(killer.uuid());
            if (killerPlayer != null) {
                broadcastLeaderboardSurpass(killerPlayer, killer.kills());
                broadcastDeath(event, deathMessage(victim, killerPlayer, killer.kills()));
            } else {
                broadcastDeath(event, environmentDeathMessage(victim));
            }
        } else {
            broadcastDeath(event, environmentDeathMessage(victim));
        }
        states.addKillstreak(victim.getUniqueId(), -states.killstreak(victim.getUniqueId()));
        dtr.handleDeath(victim);
        handleDeathban(victim);
        combat.clear(victim.getUniqueId());
    }

    private void handleDeathban(Player victim) {
        if (!settings.deathbanEnabled() || victim.hasPermission("hcf.deathban.bypass")) {
            return;
        }
        if (states.consumeLife(victim.getUniqueId())) {
            victim.sendMessage(Text.color("&8[&cLives&8] &fA life was consumed. Lives remaining: &c" + states.lives(victim.getUniqueId()) + "&f."));
            return;
        }
        long duration = settings.deathbanDefaultMinutes() * 60_000L;
        states.deathban(victim.getUniqueId(), duration);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                victim.kickPlayer(Text.color("&cYou are deathbanned for &f" + settings.deathbanDefaultMinutes() + " minutes&c."));
            }
        }, 20L);
    }

    private void focusByHit(Player attacker, Player victim) {
        if (!playerSettings.enabled(attacker.getUniqueId(), "focus-by-hit")) {
            return;
        }
        teams.byPlayer(attacker.getUniqueId()).ifPresent(team -> {
            if (victim.getUniqueId().equals(team.focused())) {
                return;
            }
            team.focused(victim.getUniqueId());
            teams.save(team);
        });
    }

    private void broadcastDeath(PlayerDeathEvent event, String message) {
        event.setDeathMessage(null);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (playerSettings.enabled(viewer.getUniqueId(), "death-messages")) {
                viewer.sendMessage(message);
            }
        }
    }

    private void broadcastLeaderboardSurpass(Player killer, int kills) {
        int top = profiles.cachedProfiles().stream().mapToInt(profile -> profile.uuid().equals(killer.getUniqueId()) ? 0 : profile.kills()).max().orElse(0);
        if (kills > top && kills > 0) {
            Bukkit.broadcastMessage(Text.color("&8[&cLeaderboards&8] &c" + killer.getName() + " &fhas surpassed the server kill leaderboard with &c" + kills + " kills&f."));
        }
    }

    private String deathMessage(Player victim, Player killer, int killerKills) {
        String weapon = weaponName(killer.getItemInHand());
        int deaths = profiles.cached(victim.getUniqueId()).map(profile -> profile.deaths()).orElse(0);
        return Text.color("&c" + victim.getName() + "&7[&f" + deaths + "&7] &fwas slain by &c" + killer.getName() + "&7[&f" + killerKills + "&7] &fusing &c" + weapon + "&f.");
    }

    private String environmentDeathMessage(Player victim) {
        int deaths = profiles.cached(victim.getUniqueId()).map(profile -> profile.deaths()).orElse(0);
        String cause = victim.getLastDamageCause() == null ? "died" : switch (victim.getLastDamageCause().getCause()) {
            case FALL -> "fell to their death";
            case LAVA -> "tried to swim in lava";
            case FIRE, FIRE_TICK -> "burned to death";
            case DROWNING -> "drowned";
            case SUFFOCATION -> "suffocated";
            case VOID -> "fell into the void";
            case CONTACT -> "was pricked to death";
            case MAGIC, POISON, WITHER -> "was killed by magic";
            default -> "died";
        };
        return Text.color("&c" + victim.getName() + "&7[&f" + deaths + "&7] &f" + cause + "&f.");
    }

    private void refund(Player player, Material material) {
        ItemStack hand = player.getItemInHand();
        if (hand != null && hand.getType() == material && hand.getAmount() < hand.getMaxStackSize()) {
            hand.setAmount(hand.getAmount() + 1);
            player.setItemInHand(hand);
        } else {
            player.getInventory().addItem(new ItemStack(material));
        }
        player.updateInventory();
    }

    private String weaponName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "Fists";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static boolean rightClick(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    private static Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
