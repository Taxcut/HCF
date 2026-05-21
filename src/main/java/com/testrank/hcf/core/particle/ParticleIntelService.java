package com.testrank.hcf.core.particle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.profile.Profile;
import com.testrank.hcf.core.redis.RedisManager;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamRole;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.util.Position;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public final class ParticleIntelService {
    public static final String CHANNEL = "particle:punishments:sync";

    private static final long DEFAULT_THROTTLE_MILLIS = 5_000L;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Plugin plugin;
    private final RedisManager redis;
    private final Threading threading;
    private final ConcurrentMap<String, Long> throttles = new ConcurrentHashMap<>();

    public ParticleIntelService(Plugin plugin, RedisManager redis, Threading threading) {
        this.plugin = plugin;
        this.redis = redis;
        this.threading = threading;
    }

    public void combatExploit(Player actor, Player target, String reason, Map<String, ?> context) {
        publishThrottled("combat:" + actor.getUniqueId() + ":" + reason, DEFAULT_THROTTLE_MILLIS, "combat_exploit", payload -> {
            payload.put("actor", player(actor, true));
            payload.put("target", player(target, true));
            payload.put("reason", reason);
            payload.put("context", copy(context));
        });
    }

    public void pearlGlitchAttempt(Player player, String reason, long remainingMillis) {
        publishThrottled("pearl:" + player.getUniqueId() + ":" + reason, 2_500L, "pearl_glitch_attempt", payload -> {
            payload.put("player", player(player, true));
            payload.put("reason", reason);
            payload.put("remainingMillis", remainingMillis);
        });
    }

    public void timerAbuse(Player player, String timerKey, String action, long remainingMillis) {
        publishThrottled("timer:" + player.getUniqueId() + ":" + timerKey + ":" + action, 2_500L, "timer_abuse", payload -> {
            payload.put("player", player(player, true));
            payload.put("timer", timerKey);
            payload.put("action", action);
            payload.put("remainingMillis", remainingMillis);
        });
    }

    public void packetAnomaly(UUID playerId, String playerName, String reason, String packet, int limit) {
        publishThrottled("packet:" + playerId + ":" + reason, DEFAULT_THROTTLE_MILLIS, "packet_combat_anomaly", payload -> {
            payload.put("player", player(playerId, playerName));
            payload.put("reason", reason);
            payload.put("packet", packet);
            payload.put("limitPerSecond", limit);
        });
    }

    public void reachFlag(Player attacker, Player target, double distance, double damage) {
        publishThrottled("reach:" + attacker.getUniqueId() + ":" + target.getUniqueId(), DEFAULT_THROTTLE_MILLIS, "reach_flag", payload -> {
            payload.put("attacker", player(attacker, true));
            payload.put("target", player(target, true));
            payload.put("distance", round(distance));
            payload.put("damage", round(damage));
        });
    }

    public void cpsSpike(UUID playerId, String playerName, int limit) {
        publishThrottled("cps:" + playerId, DEFAULT_THROTTLE_MILLIS, "cps_spike", payload -> {
            payload.put("player", player(playerId, playerName));
            payload.put("limitPerSecond", limit);
        });
    }

    public void factionPunishmentLink(Player actor, Team team, String action, Map<String, ?> context) {
        publish("faction_punishment_link", payload -> {
            payload.put("actor", player(actor, true));
            payload.put("team", team(team));
            payload.put("action", action);
            payload.put("context", copy(context));
        });
    }

    public void factionHistory(UUID playerId, String playerName, Team team, String action, UUID actorId) {
        publish("player_faction_history", payload -> {
            payload.put("player", player(playerId, playerName));
            payload.put("team", team(team));
            payload.put("action", action);
            payload.put("actorId", string(actorId));
        });
    }

    public void dtrImpact(Player player, Team team, double before, double after, String reason, long frozenMillis) {
        publish("dtr_impact_note", payload -> {
            payload.put("player", player(player, true));
            payload.put("team", team(team));
            payload.put("before", round(before));
            payload.put("after", round(after));
            payload.put("reason", reason);
            payload.put("frozenMillis", frozenMillis);
            payload.put("raidable", after <= 0.0D);
        });
    }

    public void raidAbuse(Player actor, Team team, String reason, Map<String, ?> context) {
        publishThrottled("raid:" + actor.getUniqueId() + ":" + reason, DEFAULT_THROTTLE_MILLIS, "raid_abuse_exploit", payload -> {
            payload.put("actor", player(actor, true));
            payload.put("team", team == null ? null : team(team));
            payload.put("reason", reason);
            payload.put("context", copy(context));
        });
    }

    public void staffNoteHook(Player staff, UUID targetId, String targetName, String noteType, String note) {
        publish("staff_note_hook", payload -> {
            payload.put("staff", player(staff, true));
            payload.put("target", player(targetId, targetName));
            payload.put("noteType", noteType);
            payload.put("note", note);
        });
    }

    public void profileExport(Profile profile, Team team, String reason) {
        publish("hcf_profile_export", payload -> {
            payload.put("reason", reason);
            payload.put("profile", profile(profile));
            payload.put("team", team == null ? null : team(team));
        });
    }

    public void claimIntel(Player actor, Team team, Claim claim, long price, int area) {
        publish("faction_punishment_link", payload -> {
            payload.put("actor", player(actor, true));
            payload.put("team", team(team));
            payload.put("action", "claim_created");
            payload.put("price", price);
            payload.put("area", area);
            payload.put("claim", claim(claim));
        });
    }

    public void accountLinkForward(Player player, String targetCommand, boolean particleAvailable) {
        publish("account_link_forward", payload -> {
            payload.put("player", player(player, true));
            payload.put("targetCommand", targetCommand);
            payload.put("particleCoreAvailable", particleAvailable);
        });
    }

    public void publish(String type, PayloadWriter writer) {
        if (!redis.enabled()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            writer.write(data);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not build Particle intel payload " + type, exception);
            return;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("schemaVersion", 1);
        envelope.put("source", "hcf");
        envelope.put("serverId", redis.serverId());
        envelope.put("type", type);
        envelope.put("timestamp", System.currentTimeMillis());
        envelope.put("data", data);
        threading.compute().execute(() -> {
            try {
                redis.publishRaw(CHANNEL, GSON.toJson(envelope));
            } catch (RuntimeException exception) {
                plugin.getLogger().fine("Particle intel publish failed: " + exception.getMessage());
            }
        });
    }

    public void publishThrottled(String key, long throttleMillis, String type, PayloadWriter writer) {
        long now = System.currentTimeMillis();
        Long previous = throttles.put(key, now);
        if (previous != null && now - previous < throttleMillis) {
            throttles.put(key, previous);
            return;
        }
        publish(type, writer);
    }

    private Map<String, Object> profile(Profile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", string(profile.uuid()));
        map.put("teamId", string(profile.teamId()));
        map.put("lastSeen", profile.lastSeen());
        map.put("kills", profile.kills());
        map.put("deaths", profile.deaths());
        map.put("pvpClass", profile.pvpClass());
        map.put("chatColor", profile.chatColor());
        map.put("energy", profile.energy());
        map.put("cooldowns", new LinkedHashMap<>(profile.cooldowns()));
        map.put("timers", new LinkedHashMap<>(profile.timers()));
        map.put("statistics", new LinkedHashMap<>(profile.statistics()));
        map.put("settings", new LinkedHashMap<>(profile.settings()));
        map.put("deathHistory", profile.deathHistory());
        return map;
    }

    private Map<String, Object> team(Team team) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", string(team.id()));
        map.put("name", team.name());
        map.put("dtr", round(team.dtr()));
        map.put("balance", round(team.balance()));
        map.put("points", team.points());
        map.put("kothCaps", team.kothCaps());
        map.put("frozen", team.frozen());
        map.put("claimLocked", team.claimLocked());
        map.put("regenPaused", team.regenPaused());
        map.put("friendlyFire", team.friendlyFire());
        map.put("members", members(team));
        map.put("allies", team.allies().stream().map(UUID::toString).toList());
        map.put("hq", position(team.hq()));
        map.put("rally", position(team.rally()));
        map.put("focused", string(team.focused()));
        return map;
    }

    private Map<String, String> members(Team team) {
        Map<String, String> members = new LinkedHashMap<>();
        for (Map.Entry<UUID, TeamRole> entry : team.members().entrySet()) {
            members.put(entry.getKey().toString(), entry.getValue().name());
        }
        return members;
    }

    private Map<String, Object> claim(Claim claim) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", string(claim.id()));
        map.put("owner", string(claim.owner()));
        map.put("name", claim.name());
        map.put("world", claim.world());
        map.put("minX", claim.minX());
        map.put("minZ", claim.minZ());
        map.put("maxX", claim.maxX());
        map.put("maxZ", claim.maxZ());
        map.put("type", claim.type().name());
        return map;
    }

    private Map<String, Object> player(Player player, boolean includeLocation) {
        Map<String, Object> map = player(player.getUniqueId(), player.getName());
        if (includeLocation && player.getWorld() != null) {
            map.put("location", location(player.getLocation()));
        }
        return map;
    }

    private Map<String, Object> player(UUID uuid, String name) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", string(uuid));
        map.put("name", name);
        return map;
    }

    private Map<String, Object> location(Location location) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", location.getWorld() == null ? "unknown" : location.getWorld().getName());
        map.put("x", round(location.getX()));
        map.put("y", round(location.getY()));
        map.put("z", round(location.getZ()));
        map.put("yaw", round(location.getYaw()));
        map.put("pitch", round(location.getPitch()));
        return map;
    }

    private Map<String, Object> position(Position position) {
        if (position == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", position.world());
        map.put("x", round(position.x()));
        map.put("y", round(position.y()));
        map.put("z", round(position.z()));
        map.put("yaw", round(position.yaw()));
        map.put("pitch", round(position.pitch()));
        return map;
    }

    private static Map<String, Object> copy(Map<String, ?> source) {
        return source == null ? Map.of() : new LinkedHashMap<>(source);
    }

    private static String string(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    @FunctionalInterface
    public interface PayloadWriter {
        void write(Map<String, Object> payload);
    }
}
