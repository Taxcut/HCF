package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.particle.ParticleIntelService;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.pvp.PvpProtectionService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.util.Title;
import com.testrank.hcf.core.waypoint.WaypointService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProfileListener implements Listener {
    private final ProfileService profiles;
    private final PlayerStateService states;
    private final PvpProtectionService pvpProtection;
    private final WaypointService waypoints;
    private final Threading threading;
    private final PlayerSettingsService settings;
    private final TeamService teams;
    private final ParticleIntelService intel;

    public ProfileListener(ProfileService profiles, PlayerStateService states, PvpProtectionService pvpProtection, WaypointService waypoints, Threading threading,
                           PlayerSettingsService settings, TeamService teams, ParticleIntelService intel) {
        this.profiles = profiles;
        this.states = states;
        this.pvpProtection = pvpProtection;
        this.waypoints = waypoints;
        this.threading = threading;
        this.settings = settings;
        this.teams = teams;
        this.intel = intel;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profiles.load(event.getPlayer().getUniqueId()).thenAccept(profile -> {
            states.join(event.getPlayer());
            intel.profileExport(profile, teams.byPlayer(profile.uuid()).orElse(null), "join");
            if (!profile.timers().containsKey("pvp_timer")) {
                pvpProtection.grant(event.getPlayer().getUniqueId(), 30L * 60_000L);
            }
            threading.runSync(() -> {
                long deathban = states.deathbanRemaining(event.getPlayer().getUniqueId());
                if (deathban > 0L) {
                    event.getPlayer().kickPlayer(com.testrank.hcf.core.util.Text.color("&cYou are deathbanned for &f" + formatDuration(deathban) + "&c."));
                    return;
                }
                waypoints.refresh(event.getPlayer());
                if (settings.enabled(event.getPlayer().getUniqueId(), "on-screen-warnings")) {
                    Title.send(event.getPlayer(), "&c&lHCF", "&fWelcome to the map. Use &c/help &fto begin.");
                }
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.cached(event.getPlayer().getUniqueId())
                .ifPresent(profile -> intel.profileExport(profile, teams.byPlayer(profile.uuid()).orElse(null), "quit"));
        states.quit(event.getPlayer());
        profiles.unload(event.getPlayer().getUniqueId());
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return minutes + "m " + remainder + "s";
    }
}
