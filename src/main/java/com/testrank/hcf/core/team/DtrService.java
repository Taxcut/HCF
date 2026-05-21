package com.testrank.hcf.core.team;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.particle.ParticleIntelService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DtrService implements HCFService {
    private final Plugin plugin;
    private final TeamService teams;
    private final HCFSettings settings;
    private final ParticleIntelService intel;
    private final Map<UUID, Long> frozenUntil = new ConcurrentHashMap<>();
    private int taskId = -1;

    public DtrService(Plugin plugin, TeamService teams, HCFSettings settings, ParticleIntelService intel) {
        this.plugin = plugin;
        this.teams = teams;
        this.settings = settings;
        this.intel = intel;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60L, 20L * 60L).getTaskId();
    }

    public void handleDeath(Player player) {
        teams.byPlayer(player.getUniqueId()).ifPresent(team -> {
            double before = team.dtr();
            double after = before - 1.0D;
            team.dtr(after);
            team.frozen(true);
            long frozenMillis = settings.factionFreezeDurationSeconds() * 1000L;
            frozenUntil.put(team.id(), System.currentTimeMillis() + frozenMillis);
            team.log("DTR penalty from death: " + player.getUniqueId());
            teams.save(team);
            intel.dtrImpact(player, team, before, team.dtr(), "death", frozenMillis);
        });
    }

    public boolean raidability(Team team) {
        return team.dtr() <= 0.0D;
    }

    public long frozenRemaining(Team team) {
        return Math.max(0L, frozenUntil.getOrDefault(team.id(), 0L) - System.currentTimeMillis());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Team team : teams.teams()) {
            long remaining = frozenUntil.getOrDefault(team.id(), 0L) - now;
            if (remaining > 0L) {
                continue;
            }
            if (team.frozen()) {
                team.frozen(false);
            }
            if (team.regenPaused()) {
                continue;
            }
            double cap = Math.min(settings.factionMaxDtr(), Math.max(settings.factionDtrPerMember(), team.members().size() * settings.factionDtrPerMember()));
            if (team.dtr() < cap) {
                team.dtr(Math.min(cap, team.dtr() + settings.factionRegenPerMinute()));
                teams.save(team);
            }
        }
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
