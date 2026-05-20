package com.testrank.hcf.core.koth;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEvent;
import com.testrank.hcf.core.events.HCFEventType;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.team.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KothService implements HCFService {
    private final Plugin plugin;
    private final EventService events;
    private final ClaimService claims;
    private final TeamService teams;
    private final ProfileService profiles;
    private final long captureMillis;
    private final Map<UUID, CaptureState> captures = new ConcurrentHashMap<>();
    private int taskId = -1;

    public KothService(Plugin plugin, EventService events, ClaimService claims, TeamService teams, ProfileService profiles, HCFSettings settings) {
        this.plugin = plugin;
        this.events = events;
        this.claims = claims;
        this.teams = teams;
        this.profiles = profiles;
        this.captureMillis = settings.kothCapSeconds() * 1000L;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
    }

    public HCFEvent start(String name, long durationMillis) {
        Claim claim = claims.byName(name).orElseThrow(() -> new IllegalArgumentException("KOTH claim not found: " + name));
        HCFEvent event = events.create(HCFEventType.KOTH, name);
        event.start(durationMillis);
        captures.put(event.id(), new CaptureState(claim, null, 0L));
        return event;
    }

    public List<KothDisplay> activeDisplays() {
        return events.events().stream()
                .filter(event -> event.active() && event.type() == HCFEventType.KOTH)
                .map(event -> {
                    CaptureState state = captures.get(event.id());
                    if (state == null) {
                        return null;
                    }
                    long elapsed = state.startedAt() <= 0L ? 0L : System.currentTimeMillis() - state.startedAt();
                    long captureRemaining = Math.max(0L, captureMillis - elapsed);
                    return new KothDisplay(event.name(), state.claim(), captureRemaining);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<Claim> activeCapzones() {
        return activeDisplays().stream().map(KothDisplay::claim).toList();
    }

    private void tick() {
        for (HCFEvent event : events.events()) {
            if (!event.active() || event.type() != HCFEventType.KOTH) {
                continue;
            }
            CaptureState state = captures.get(event.id());
            if (state == null) {
                continue;
            }
            List<Player> inside = Bukkit.getOnlinePlayers().stream()
                    .map(Player.class::cast)
                    .filter(player -> state.claim().contains(player.getLocation()))
                    .toList();
            Player capturer = inside.stream().findFirst().orElse(null);
            if (capturer == null) {
                captures.put(event.id(), new CaptureState(state.claim(), null, 0L));
                continue;
            }
            long distinctSides = inside.stream()
                    .map(player -> teams.byPlayer(player.getUniqueId()).map(team -> team.id()).orElse(player.getUniqueId()))
                    .distinct()
                    .count();
            if (distinctSides > 1L) {
                captures.put(event.id(), new CaptureState(state.claim(), null, 0L));
                continue;
            }
            UUID teamId = teams.byPlayer(capturer.getUniqueId()).map(team -> team.id()).orElse(capturer.getUniqueId());
            long startedAt = teamId.equals(state.capturingTeam()) ? state.startedAt() : System.currentTimeMillis();
            if (System.currentTimeMillis() - startedAt >= captureMillis) {
                event.addScore(teamId, 1);
                event.stop();
                profiles.cached(capturer.getUniqueId()).ifPresent(profile -> {
                    profile.addStatistic("koth_captures", 1);
                    profiles.save(profile);
                });
                teams.byPlayer(capturer.getUniqueId()).ifPresent(team -> {
                    team.kothCaps(team.kothCaps() + 1);
                    team.points(team.points() + 5);
                    teams.save(team);
                });
                Bukkit.broadcastMessage("§8[§cKOTH§8] §f" + event.name() + " captured by §c" + capturer.getName() + "§f.");
                captures.remove(event.id());
            } else {
                captures.put(event.id(), new CaptureState(state.claim(), teamId, startedAt));
            }
        }
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private record CaptureState(Claim claim, UUID capturingTeam, long startedAt) {}

    public record KothDisplay(String name, Claim claim, long captureRemaining) {}
}
