package com.testrank.hcf.core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record HCFSettings(
        boolean mongoEnabled,
        String mongoUri,
        String mongoDatabase,
        boolean redisEnabled,
        String redisUri,
        String serverId,
        String networkName,
        boolean deathbanEnabled,
        int deathbanDefaultMinutes,
        int defaultLives,
        int combatTagSeconds,
        int archerTagSeconds,
        int pearlSeconds,
        int rodSeconds,
        int goldenAppleSeconds,
        int antiCleanHits,
        int antiCleanSeconds,
        int antiCleanTeamLimit,
        int antiCleanKothDisableRadius,
        int factionMaxMembers,
        int factionMaxClaims,
        double factionDtrPerMember,
        double factionMaxDtr,
        double factionRegenPerMinute,
        int factionFreezeDurationSeconds,
        int factionInviteExpireSeconds,
        int claimMovementBlockShift,
        double claimPricePerBlock,
        long claimMinimumPrice,
        int claimMinimumSize,
        int claimMaximumSize,
        int glowstoneResetMinutes,
        int classWarmupSeconds,
        int kothCapSeconds,
        boolean archerEnabled,
        boolean bardEnabled,
        boolean rogueEnabled,
        boolean ghostEnabled,
        List<String> scoreboardTitleFrames
) {
    public HCFSettings {
        combatTagSeconds = positive(combatTagSeconds, 30);
        archerTagSeconds = positive(archerTagSeconds, 10);
        pearlSeconds = positive(pearlSeconds, 16);
        rodSeconds = positive(rodSeconds, 3);
        goldenAppleSeconds = positive(goldenAppleSeconds, 300);
        antiCleanHits = positive(antiCleanHits, 3);
        antiCleanSeconds = positive(antiCleanSeconds, 45);
        antiCleanTeamLimit = positive(antiCleanTeamLimit, 8);
        antiCleanKothDisableRadius = Math.max(0, antiCleanKothDisableRadius);
        factionMaxMembers = positive(factionMaxMembers, 25);
        factionMaxClaims = positive(factionMaxClaims, 6);
        factionDtrPerMember = factionDtrPerMember <= 0.0D ? 1.01D : factionDtrPerMember;
        factionMaxDtr = factionMaxDtr <= 0.0D ? 6.01D : factionMaxDtr;
        factionRegenPerMinute = factionRegenPerMinute <= 0.0D ? 0.1D : factionRegenPerMinute;
        factionFreezeDurationSeconds = positive(factionFreezeDurationSeconds, 30);
        factionInviteExpireSeconds = positive(factionInviteExpireSeconds, 60);
        claimMovementBlockShift = Math.max(1, Math.min(8, claimMovementBlockShift));
        claimPricePerBlock = Math.max(0.0D, claimPricePerBlock);
        claimMinimumPrice = Math.max(0L, claimMinimumPrice);
        claimMinimumSize = positive(claimMinimumSize, 5);
        claimMaximumSize = Math.max(claimMinimumSize, claimMaximumSize <= 0 ? 150 : claimMaximumSize);
        glowstoneResetMinutes = positive(glowstoneResetMinutes, 20);
        classWarmupSeconds = Math.max(0, classWarmupSeconds);
        kothCapSeconds = positive(kothCapSeconds, 900);
        scoreboardTitleFrames = scoreboardTitleFrames == null ? List.of() : List.copyOf(scoreboardTitleFrames);
        deathbanDefaultMinutes = positive(deathbanDefaultMinutes, 30);
        defaultLives = Math.max(0, defaultLives);
    }

    public static HCFSettings from(FileConfiguration config) {
        return new HCFSettings(
                config.getBoolean("mongo.enabled", false),
                config.getString("mongo.uri", "mongodb://127.0.0.1:27017"),
                config.getString("mongo.database", "hcf"),
                config.getBoolean("redis.enabled", false),
                config.getString("redis.uri", "redis://127.0.0.1:6379"),
                config.getString("server.id", "hcf-01"),
                config.getString("server.network-name", "HCF"),
                config.getBoolean("deathban.enabled", true),
                config.getInt("deathban.default-duration-minutes", 30),
                config.getInt("deathban.lives", 3),
                config.getInt("combat.tag-seconds", 30),
                config.getInt("combat.archer-tag-seconds", 10),
                config.getInt("combat.enderpearl-seconds", config.getInt("timers.ender-pearl", 16)),
                config.getInt("timers.rod", config.getInt("rod-cooldown.seconds", 3)),
                config.getInt("combat.golden-apple-seconds", config.getInt("timers.gapple", 300)),
                config.getInt("anticlean.hits-to-activate", 3),
                config.getInt("anticlean.duration-seconds", 45),
                config.getInt("anticlean.team-limit", 8),
                config.getInt("anticlean.koth-disable-radius", 35),
                config.getInt("faction.max-members", 25),
                config.getInt("faction.max-claims", 6),
                config.getDouble("faction.dtr-per-member", 1.01D),
                config.getDouble("faction.max-dtr", 6.01D),
                config.getDouble("faction.regen-per-minute", 0.1D),
                config.getInt("faction.freeze-duration-seconds", 30),
                config.getInt("faction.invite-expire-seconds", 60),
                config.getInt("claims.movement-check-block-shift", 4),
                config.getDouble("claims.price-per-block", 2.0D),
                config.getLong("claims.minimum-price", 250L),
                config.getInt("claims.minimum-size", 5),
                config.getInt("claims.maximum-size", 150),
                config.getInt("glowstone.reset-minutes", 20),
                config.getInt("pvp-classes.warmup-seconds", 3),
                config.getInt("koth.cap-time-seconds", 900),
                config.getBoolean("classes.archer", true),
                config.getBoolean("classes.bard", true),
                config.getBoolean("classes.rogue", true),
                config.getBoolean("classes.ghost", true),
                config.getStringList("scoreboard.title-frames")
        );
    }

    private static int positive(int value, int fallback) {
        return value <= 0 ? fallback : value;
    }
}
