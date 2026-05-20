package com.testrank.hcf.core.chat;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.api.PermissionService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatService implements HCFService {
    private final TeamService teams;
    private final PermissionService permissions;
    private final PlayerSettingsService settings;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, ChatChannel> channels = new ConcurrentHashMap<>();
    private volatile long globalSlowMillis;

    public ChatService(TeamService teams, PermissionService permissions, PlayerSettingsService settings) {
        this.teams = teams;
        this.permissions = permissions;
        this.settings = settings;
    }

    public boolean canChat(Player player) {
        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (next > now) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), now + globalSlowMillis);
        return true;
    }

    public void teamChat(Player sender, String message) {
        teams.byPlayer(sender.getUniqueId()).ifPresent(team ->
                Bukkit.getOnlinePlayers().stream()
                        .filter(player -> team.isMember(player.getUniqueId()))
                        .forEach(player -> player.sendMessage(Text.color(chatIdentity(sender) + "&7: " + messageColor(sender) + message))));
    }

    public ChatChannel channel(Player player) {
        return channels.getOrDefault(player.getUniqueId(), ChatChannel.GLOBAL);
    }

    public void channel(Player player, ChatChannel channel) {
        channels.put(player.getUniqueId(), channel);
    }

    public void allyChat(Player sender, String message) {
        teams.byPlayer(sender.getUniqueId()).ifPresent(team ->
                Bukkit.getOnlinePlayers().stream()
                        .filter(player -> teams.byPlayer(player.getUniqueId())
                                .map(other -> other.id().equals(team.id()) || team.allies().contains(other.id()))
                                .orElse(false))
                        .forEach(player -> player.sendMessage("§d[Ally] §f" + sender.getName() + ": §d" + message)));
    }

    public void staffChat(Player sender, String message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("hcf.staff"))
                .forEach(player -> player.sendMessage("§9[Staff] §f" + sender.getName() + ": §b" + message));
    }

    public String fTopMarker(Team team) {
        int rank = rank(team);
        return switch (rank) {
            case 1 -> "&6➊";
            case 2 -> "&f➋";
            case 3 -> "&9➌";
            default -> "";
        };
    }

    public String factionPrefix(Team team) {
        String marker = fTopMarker(team);
        return marker.isBlank() ? "&6[&c" + team.name() + "&6]" : "&6[&c" + team.name() + "&6] " + marker;
    }

    public String chatIdentity(Player player) {
        StringBuilder builder = new StringBuilder();
        teams.byPlayer(player.getUniqueId()).ifPresent(team -> append(builder, factionPrefix(team)));
        append(builder, rank(player));
        append(builder, permissions.rankColor(player) + player.getName());
        return builder.toString();
    }

    private int rank(Team team) {
        java.util.List<Team> sorted = teams.teams().stream()
                .sorted(java.util.Comparator.<Team>comparingInt(Team::points).thenComparingDouble(Team::balance).reversed())
                .toList();
        for (int i = 0; i < sorted.size() && i < 3; i++) {
            if (sorted.get(i).id().equals(team.id())) {
                return i + 1;
            }
        }
        return -1;
    }

    private String rank(Player player) {
        String rank = permissions.particleRank(player);
        return rank.isBlank() ? permissions.chatPrefix(player) : rank;
    }

    public void globalSlow(long millis) {
        globalSlowMillis = Math.max(0L, millis);
    }

    public String messageColor(Player player) {
        return settings.chatColor(player.getUniqueId());
    }

    private static void append(StringBuilder builder, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(part.trim());
    }
}
