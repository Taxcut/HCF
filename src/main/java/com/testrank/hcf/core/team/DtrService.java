package com.testrank.hcf.core.team;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.claim.ClaimType;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.particle.ParticleIntelService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DtrService implements HCFService {
    private final Plugin plugin;
    private final TeamService teams;
    private final ClaimService claims;
    private final HCFSettings settings;
    private final ParticleIntelService intel;
    private final Map<UUID, Long> recentDeathPenalties = new ConcurrentHashMap<>();
    private int taskId = -1;

    public DtrService(Plugin plugin, TeamService teams, ClaimService claims, HCFSettings settings, ParticleIntelService intel) {
        this.plugin = plugin;
        this.teams = teams;
        this.claims = claims;
        this.settings = settings;
        this.intel = intel;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60L, 20L * 60L).getTaskId();
    }

    public Optional<DtrPenalty> handleDeath(Player player) {
        if (isDtrLossDisabled(player)) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        Long previousPenalty = recentDeathPenalties.put(player.getUniqueId(), now);
        if (previousPenalty != null && now - previousPenalty < 2_000L) {
            return Optional.empty();
        }
        Optional<Team> optional = teams.byPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Team team = optional.get();
        double before = team.dtr();
        double loss = settings.factionDtrLossPerDeath();
        double after = before - loss;
        team.dtr(after);
        team.frozen(true);
        long frozenMillis = settings.factionFreezeDurationSeconds() * 1000L;
        long frozenUntil = now + frozenMillis;
        team.frozenUntil(frozenUntil);
        team.log("DTR penalty from death: " + player.getUniqueId() + " (" + format(before) + " -> " + format(team.dtr()) + ")");
        teams.save(team);
        intel.dtrImpact(player, team, before, team.dtr(), "death", frozenMillis);
        DtrPenalty penalty = new DtrPenalty(team, before, team.dtr(), loss, frozenMillis);
        broadcastDeathPenalty(player, penalty);
        return Optional.of(penalty);
    }

    public boolean raidability(Team team) {
        return team.dtr() <= 0.0D;
    }

    public long frozenRemaining(Team team) {
        return Math.max(0L, team.frozenUntil() - System.currentTimeMillis());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        recentDeathPenalties.entrySet().removeIf(entry -> now - entry.getValue() > 10_000L);
        for (Team team : teams.teams()) {
            long remaining = team.frozenUntil() - now;
            if (remaining > 0L) {
                continue;
            }
            if (team.frozen()) {
                team.frozen(false);
                team.frozenUntil(0L);
                teams.save(team);
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

    private boolean isDtrLossDisabled(Player player) {
        String world = player.getWorld() == null ? "" : player.getWorld().getName();
        for (String disabled : settings.factionDtrLossDisabledWorlds()) {
            if (disabled.equalsIgnoreCase(world)) {
                return true;
            }
        }
        return claims.at(player.getLocation()).map(claim -> claim.type() == ClaimType.SAFEZONE).orElse(false);
    }

    private void broadcastDeathPenalty(Player dead, DtrPenalty penalty) {
        if (!settings.factionDtrDeathBroadcast()) {
            return;
        }
        String message = settings.factionDtrDeathMessage()
                .replace("%player%", dead.getName())
                .replace("%loss%", format(penalty.loss()))
                .replace("%old_dtr%", format(penalty.oldDtr()))
                .replace("%new_dtr%", format(penalty.newDtr()))
                .replace("%freeze%", formatDuration(penalty.frozenMillis()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (penalty.team().isMember(player.getUniqueId())) {
                player.sendMessage(Text.color(message));
            }
        }
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return minutes > 0 ? minutes + "m" + (remainder == 0 ? "" : " " + remainder + "s") : remainder + "s";
    }

    public record DtrPenalty(Team team, double oldDtr, double newDtr, double loss, long frozenMillis) {}

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
