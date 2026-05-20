package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class LogoutListener implements Listener {
    private final PlayerStateService states;

    public LogoutListener(PlayerStateService states) {
        this.states = states;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        cancel(event.getPlayer(), "movement");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            cancel(victim, "combat");
        }
        Player attacker = attacker(event);
        if (attacker != null) {
            cancel(attacker, "combat");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.cancelLogout(event.getPlayer().getUniqueId());
    }

    private void cancel(Player player, String reason) {
        if (!states.loggingOut(player.getUniqueId())) {
            return;
        }
        states.cancelLogout(player.getUniqueId());
        player.sendMessage(Text.color("&8[&cLogout&8] &cSafe logout cancelled due to " + reason + "."));
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
