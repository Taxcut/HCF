package com.testrank.hcf.core.combat;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.koth.KothService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AntiCleanService implements HCFService {
    private final Plugin plugin;
    private final TeamService teams;
    private final KothService koths;
    private final HCFSettings settings;
    private final PlayerSettingsService playerSettings;
    private final ConcurrentMap<FightKey, PendingFight> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<FightKey, ActiveFight> active = new ConcurrentHashMap<>();
    private int taskId = -1;

    public AntiCleanService(Plugin plugin, TeamService teams, KothService koths, HCFSettings settings, PlayerSettingsService playerSettings) {
        this.plugin = plugin;
        this.teams = teams;
        this.koths = koths;
        this.settings = settings;
        this.playerSettings = playerSettings;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
    }

    public DenyResult deny(Player attacker, Player victim) {
        if (disabledNearKoth(attacker.getLocation()) || disabledNearKoth(victim.getLocation())) {
            return DenyResult.allowed();
        }
        Optional<Team> attackerTeam = teams.byPlayer(attacker.getUniqueId());
        Optional<Team> victimTeam = teams.byPlayer(victim.getUniqueId());
        if (attackerTeam.isPresent() && victimTeam.isPresent() && attackerTeam.get().id().equals(victimTeam.get().id())) {
            return DenyResult.allowed();
        }
        for (Map.Entry<FightKey, ActiveFight> entry : active.entrySet()) {
            ActiveFight fight = entry.getValue();
            boolean attackerParticipant = fight.participates(attackerTeam.map(Team::id).orElse(attacker.getUniqueId()));
            boolean victimParticipant = fight.participates(victimTeam.map(Team::id).orElse(victim.getUniqueId()));
            if (attackerParticipant == victimParticipant) {
                continue;
            }
            fight.blockedHits++;
            return new DenyResult(true, "AntiClean is protecting this fight for " + seconds(fight.expiresAt) + "s.");
        }
        return DenyResult.allowed();
    }

    public void record(Player attacker, Player victim) {
        if (disabledNearKoth(attacker.getLocation()) || disabledNearKoth(victim.getLocation())) {
            return;
        }
        UUID attackerSide = teams.byPlayer(attacker.getUniqueId()).map(Team::id).orElse(attacker.getUniqueId());
        UUID victimSide = teams.byPlayer(victim.getUniqueId()).map(Team::id).orElse(victim.getUniqueId());
        if (attackerSide.equals(victimSide)) {
            return;
        }
        if (sideSize(attackerSide) > settings.antiCleanTeamLimit() || sideSize(victimSide) > settings.antiCleanTeamLimit()) {
            return;
        }
        FightKey key = FightKey.of(attackerSide, victimSide);
        ActiveFight activeFight = active.get(key);
        if (activeFight != null) {
            activeFight.hits++;
            activeFight.expiresAt = System.currentTimeMillis() + settings.antiCleanSeconds() * 1000L;
            return;
        }
        PendingFight next = pending.compute(key, (ignored, old) -> {
            long now = System.currentTimeMillis();
            if (old == null || old.expiresAt < now) {
                return new PendingFight(1, now + 12_000L);
            }
            old.hits++;
            old.expiresAt = now + 12_000L;
            return old;
        });
        if (next != null && next.hits >= settings.antiCleanHits()) {
            pending.remove(key);
            ActiveFight fight = new ActiveFight(key.first, key.second, System.currentTimeMillis(), System.currentTimeMillis() + settings.antiCleanSeconds() * 1000L);
            fight.hits = next.hits;
            active.put(key, fight);
            notifyFight(fight, "&8[&cAntiClean&8] &fFight protection activated for &c" + settings.antiCleanSeconds() + "s&f.");
        }
    }

    public Optional<ActiveFight> fight(UUID player) {
        UUID side = teams.byPlayer(player).map(Team::id).orElse(player);
        return active.values().stream().filter(fight -> fight.participates(side)).findFirst();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        active.entrySet().removeIf(entry -> {
            ActiveFight fight = entry.getValue();
            if (fight.expiresAt > now) {
                return false;
            }
            notifyFight(fight, "&8[&cAntiClean&8] &fFight protection ended. &7Hits: &c" + fight.hits + " &7Blocked cleans: &c" + fight.blockedHits + "&f.");
            return true;
        });
    }

    private boolean disabledNearKoth(Location location) {
        int radius = Math.max(0, settings.antiCleanKothDisableRadius());
        if (radius == 0) {
            return false;
        }
        int radiusSquared = radius * radius;
        for (Claim claim : koths.activeCapzones()) {
            if (!Objects.equals(location.getWorld() == null ? null : location.getWorld().getName(), claim.world())) {
                continue;
            }
            if (claim.contains(location)) {
                return true;
            }
            int centerX = (claim.minX() + claim.maxX()) / 2;
            int centerZ = (claim.minZ() + claim.maxZ()) / 2;
            int dx = location.getBlockX() - centerX;
            int dz = location.getBlockZ() - centerZ;
            if (dx * dx + dz * dz <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private int sideSize(UUID side) {
        return teams.byId(side).map(team -> team.members().size()).orElse(1);
    }

    private void notifyFight(ActiveFight fight, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID side = teams.byPlayer(player.getUniqueId()).map(Team::id).orElse(player.getUniqueId());
            if (fight.participates(side) && playerSettings.enabled(player.getUniqueId(), "teamfight-statistics")) {
                player.sendMessage(Text.color(message));
            }
        }
    }

    private static long seconds(long expiresAt) {
        return Math.max(0L, (expiresAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private static final class PendingFight {
        private int hits;
        private long expiresAt;

        private PendingFight(int hits, long expiresAt) {
            this.hits = hits;
            this.expiresAt = expiresAt;
        }
    }

    public static final class ActiveFight {
        private final UUID first;
        private final UUID second;
        private final long startedAt;
        private volatile long expiresAt;
        private volatile int hits;
        private volatile int blockedHits;

        private ActiveFight(UUID first, UUID second, long startedAt, long expiresAt) {
            this.first = first;
            this.second = second;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
        }

        public boolean participates(UUID side) {
            return first.equals(side) || second.equals(side);
        }

        public long remaining() {
            return Math.max(0L, expiresAt - System.currentTimeMillis());
        }

        public int hits() {
            return hits;
        }

        public int blockedHits() {
            return blockedHits;
        }

        public long startedAt() {
            return startedAt;
        }
    }

    private record FightKey(UUID first, UUID second) {
        private static FightKey of(UUID a, UUID b) {
            return a.compareTo(b) <= 0 ? new FightKey(a, b) : new FightKey(b, a);
        }
    }

    public record DenyResult(boolean denied, String message) {
        private static DenyResult allowed() {
            return new DenyResult(false, "");
        }
    }
}
