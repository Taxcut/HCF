package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimMovementListener implements Listener {
    private final ClaimService claims;
    private final TeamService teams;
    private final Map<UUID, MovementState> state = new ConcurrentHashMap<>();

    public ClaimMovementListener(ClaimService claims, TeamService teams, HCFSettings settings) {
        this.claims = claims;
        this.teams = teams;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || sameBlock(from, to)) {
            return;
        }
        Player player = event.getPlayer();
        Claim current = claims.at(to).orElse(null);
        MovementState previous = state.put(player.getUniqueId(), new MovementState(to.getBlockX(), to.getBlockZ(), current == null ? null : current.id()));
        UUID previousClaim = previous == null ? null : previous.claimId();
        UUID currentClaim = current == null ? null : current.id();
        if (!java.util.Objects.equals(previousClaim, currentClaim)) {
            player.sendMessage(Text.color(entryMessage(player, current)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        state.remove(event.getPlayer().getUniqueId());
    }

    private String entryMessage(Player player, Claim current) {
        if (current == null) {
            return "&8[&aWilderness&8] &aNow entering Wilderness";
        }
        boolean own = teams.byPlayer(player.getUniqueId()).map(team -> team.id().equals(current.owner())).orElse(false);
        if (own) {
            return "&8[&aClaim&8] &aNow entering your faction claim &f" + current.name();
        }
        return "&8[&cClaim&8] &cNow entering enemy claim &f" + current.name();
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ();
    }

    private record MovementState(int blockX, int blockZ, UUID claimId) {}
}
