package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.pvp.PvpProtectionService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class PvpProtectionListener implements Listener {
    private final PvpProtectionService protection;

    public PvpProtectionListener(PvpProtectionService protection) {
        this.protection = protection;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        if (protection.protectedPlayer(victim.getUniqueId()) || protection.protectedPlayer(attacker.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(Text.color("&cPvP protection is active."));
        }
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
