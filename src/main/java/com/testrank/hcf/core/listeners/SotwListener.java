package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.sotw.SotwService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class SotwListener implements Listener {
    private final SotwService sotw;

    public SotwListener(SotwService sotw) {
        this.sotw = sotw;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!sotw.active() || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        if (!sotw.protectedPlayer(victim.getUniqueId()) && !sotw.protectedPlayer(attacker.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        attacker.sendMessage(Text.color("&cSOTW protection is active. Use /sotw enable to enable PvP."));
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
