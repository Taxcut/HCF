package com.testrank.hcf.core.leaderboard;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.profile.Profile;
import com.testrank.hcf.core.profile.ProfileService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToLongFunction;

public final class LeaderboardService implements HCFService {
    private final ProfileService profiles;
    private final PlayerStateService states;
    private final EconomyService economy;
    private final Map<Category, List<Entry>> cache = new ConcurrentHashMap<>();
    private volatile long expiresAt;

    public LeaderboardService(ProfileService profiles, PlayerStateService states, EconomyService economy) {
        this.profiles = profiles;
        this.states = states;
        this.economy = economy;
    }

    public List<Entry> top(Category category) {
        if (System.currentTimeMillis() > expiresAt) {
            rebuild();
        }
        return cache.getOrDefault(category, List.of());
    }

    public void rebuild() {
        for (Category category : Category.values()) {
            if (category == Category.PLAYTIME) {
                cache.put(category, profiles.cachedProfiles().stream()
                        .map(profile -> new Entry(profile.uuid(), name(profile.uuid()), states.playtime(profile.uuid())))
                        .sorted(Comparator.comparingLong(Entry::value).reversed())
                        .limit(10)
                        .toList());
                continue;
            }
            cache.put(category, profiles.cachedProfiles().stream()
                    .map(profile -> new Entry(profile.uuid(), name(profile.uuid()), category.value(profile)))
                    .filter(entry -> entry.value() > 0L || category == Category.DEATHS)
                    .sorted(Comparator.comparingLong(Entry::value).reversed())
                    .limit(10)
                    .toList());
        }
        cache.put(Category.KILLSTREAK, states.killstreaks().entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), name(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingLong(Entry::value).reversed())
                .limit(10)
                .toList());
        cache.put(Category.WEALTH, economy.balances().entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), name(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingLong(Entry::value).reversed())
                .limit(10)
                .toList());
        expiresAt = System.currentTimeMillis() + 60_000L;
    }

    private static String name(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
    }

    public record Entry(UUID uuid, String name, long value) {}

    public enum Category {
        KILLS("Kills", Profile::kills),
        DEATHS("Deaths", Profile::deaths),
        KDR("KDR", profile -> profile.deaths() == 0 ? profile.kills() * 100L : Math.round(((double) profile.kills() / profile.deaths()) * 100.0D)),
        KILLSTREAK("Killstreak", profile -> 0L),
        TEAMFIGHT_KILLS("Teamfight Kills", profile -> profile.statistic("teamfight_kills")),
        KOTH_CAPTURES("KOTH Captures", profile -> profile.statistic("koth_captures")),
        CITADEL_CAPTURES("Citadel Captures", profile -> profile.statistic("citadel_captures")),
        OUTPOST_CAPTURES("Outpost Captures", profile -> profile.statistic("outpost_captures")),
        CAVE_EVENT_WINS("Cave Event Wins", profile -> profile.statistic("cave_event_wins")),
        MISSIONS_COMPLETED("Missions Completed", profile -> profile.statistic("missions_completed")),
        CONTRACTS_COMPLETED("Contracts Completed", profile -> profile.statistic("contracts_completed")),
        PLAYTIME("Playtime", profile -> 0L),
        WEALTH("Wealth", profile -> 0L),
        DAMAGE_DEALT("Damage Dealt", profile -> profile.statistic("damage_dealt")),
        POTIONS_SPLASHED("Potions Splashed", profile -> profile.statistic("potions_splashed")),
        ORES_MINED("Ores Mined", profile -> profile.statistic("ores_mined"));

        private final String displayName;
        private final ToLongFunction<Profile> metric;

        Category(String displayName, ToLongFunction<Profile> metric) {
            this.displayName = displayName;
            this.metric = metric;
        }

        public String displayName() {
            return displayName;
        }

        private long value(Profile profile) {
            return metric.applyAsLong(profile);
        }

        public String valueText(long value) {
            if (this == KDR) {
                return String.format(Locale.US, "%.2f", value / 100.0D);
            }
            if (this == DAMAGE_DEALT) {
                return String.format(Locale.US, "%.1f", value / 10.0D);
            }
            return Long.toString(value);
        }
    }
}
