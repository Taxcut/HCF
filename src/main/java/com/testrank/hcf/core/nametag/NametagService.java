package com.testrank.hcf.core.nametag;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.api.PermissionService;
import com.testrank.hcf.core.combat.CombatService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NametagService implements HCFService {
    private static final double MAX_DISTANCE_SQUARED = 80.0D * 80.0D;
    private final Plugin plugin;
    private final TeamService teams;
    private final CombatService combat;
    private final ClientIntegrationService clients;
    private final StaffService staff;
    private final PermissionService permissions;
    private final PlayerSettingsService settings;
    private int taskId = -1;

    public NametagService(Plugin plugin, TeamService teams, CombatService combat, ClientIntegrationService clients,
                          StaffService staff, PermissionService permissions, PlayerSettingsService settings) {
        this.plugin = plugin;
        this.teams = teams;
        this.combat = combat;
        this.clients = clients;
        this.staff = staff;
        this.permissions = permissions;
        this.settings = settings;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 40L).getTaskId();
    }

    private void tick() {
        Player[] players = Bukkit.getOnlinePlayers().toArray(Player[]::new);
        for (Player viewer : players) {
            for (Player target : players) {
                if (viewer.getWorld() != target.getWorld()) {
                    continue;
                }
                if (viewer.getLocation().distanceSquared(target.getLocation()) <= MAX_DISTANCE_SQUARED) {
                    refresh(viewer, target);
                }
            }
        }
    }

    public void refresh(Player viewer, Player target) {
        Scoreboard scoreboard = viewer.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(scoreboard);
        }
        String id = "nt_" + target.getUniqueId().toString().substring(0, 12);
        Team team = scoreboard.getTeam(id);
        if (team == null) {
            team = scoreboard.registerNewTeam(id);
        }
        String prefix = prefix(viewer, target);
        if (!prefix.equals(team.getPrefix())) {
            team.setPrefix(prefix);
        }
        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
        if (settings.enabled(viewer.getUniqueId(), "lunar-nametags")) {
            clients.sendNametag(viewer, target, lunarLines(viewer, target, prefix));
        }
    }

    private String prefix(Player viewer, Player target) {
        if (staff.staffMode(target)) {
            return Text.color(permissions.rankColor(target));
        }
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return ChatColor.GREEN.toString();
        }
        boolean sameTeam = teams.byPlayer(viewer.getUniqueId()).flatMap(viewerTeam ->
                teams.byPlayer(target.getUniqueId()).map(targetTeam -> viewerTeam.id().equals(targetTeam.id()))).orElse(false);
        if (sameTeam) {
            return ChatColor.GREEN.toString();
        }
        if (combat.tag(target.getUniqueId()).isPresent()) {
            return ChatColor.RED.toString();
        }
        return ChatColor.RED.toString();
    }

    private java.util.List<String> lunarLines(Player viewer, Player target, String nameColor) {
        if (staff.staffMode(target)) {
            java.util.List<String> lines = new java.util.ArrayList<>(2);
            String state = staff.vanished(target) ? "" : ChatColor.STRIKETHROUGH.toString();
            lines.add(ChatColor.GRAY.toString() + ChatColor.ITALIC + state + "[StaffMode]");
            lines.add(Text.color(permissions.rankColor(target)) + target.getName());
            return lines;
        }
        java.util.Optional<com.testrank.hcf.core.team.Team> targetTeam = teams.byPlayer(target.getUniqueId());
        java.util.List<String> lines = new java.util.ArrayList<>(2);
        targetTeam.ifPresent(team -> lines.add(topMarker(team) + bracketColor(team) + "[" + team.name() + " " + String.format(java.util.Locale.US, "%.2f", team.dtr()) + "]"));
        lines.add(nameColor + target.getName());
        return lines;
    }

    private String topMarker(com.testrank.hcf.core.team.Team team) {
        return switch (rank(team)) {
            case 1 -> ChatColor.GOLD + "➊";
            case 2 -> ChatColor.WHITE + "➋";
            case 3 -> ChatColor.BLUE + "➌";
            default -> "";
        };
    }

    private String bracketColor(com.testrank.hcf.core.team.Team team) {
        return switch (rank(team)) {
            case 1 -> ChatColor.GOLD.toString();
            case 2 -> ChatColor.WHITE.toString();
            case 3 -> ChatColor.BLUE.toString();
            default -> ChatColor.DARK_GREEN.toString();
        };
    }

    private int rank(com.testrank.hcf.core.team.Team team) {
        java.util.List<com.testrank.hcf.core.team.Team> sorted = teams.teams().stream()
                .sorted(java.util.Comparator.<com.testrank.hcf.core.team.Team>comparingInt(com.testrank.hcf.core.team.Team::points)
                        .thenComparingDouble(com.testrank.hcf.core.team.Team::balance).reversed())
                .toList();
        for (int i = 0; i < sorted.size() && i < 3; i++) {
            if (sorted.get(i).id().equals(team.id())) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
