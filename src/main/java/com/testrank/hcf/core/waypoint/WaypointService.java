package com.testrank.hcf.core.waypoint;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamService;
import org.bukkit.entity.Player;

public final class WaypointService implements HCFService {
    private final TeamService teams;
    private final ClientIntegrationService clients;
    private final PlayerSettingsService settings;

    public WaypointService(TeamService teams, ClientIntegrationService clients, PlayerSettingsService settings) {
        this.teams = teams;
        this.clients = clients;
        this.settings = settings;
    }

    public void refresh(Player player) {
        if (!settings.enabled(player.getUniqueId(), "lunar-team-view")) {
            return;
        }
        teams.byPlayer(player.getUniqueId()).ifPresent(team -> {
            if (team.hq() != null) {
                team.hq().toLocation().ifPresent(hq -> clients.sendWaypoint(player, "team_hq", "Team HQ", hq, 0xFF00FF66));
            }
            if (team.rally() != null) {
                team.rally().toLocation().ifPresent(rally -> clients.sendWaypoint(player, "team_rally", "Team Rally", rally, 0xFFFF4444));
            }
        });
    }
}
