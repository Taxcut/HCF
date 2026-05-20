package com.testrank.hcf.core.leaderboard;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.profile.Profile;
import com.testrank.hcf.core.profile.ProfileService;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToLongFunction;

public final class LeaderboardService implements HCFService {
    private final ProfileService profiles;
    private final PlayerStateService states;
    private final EconomyService economy;
    private final Map<Category, List<Entry>> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean rebuilding = new AtomicBoolean();
    private volatile long expiresAt;

    public LeaderboardService(ProfileService profiles, PlayerStateService states, EconomyService economy) {
        this.profiles = profiles;
        this.states = states;
        this.economy = economy;
    }

    @Override
    public void start() {
        rebuild();
    }

    public List<Entry> top(Category category) {
        if (System.currentTimeMillis() > expiresAt) {
            rebuild();
        }
        return cache.getOrDefault(category, List.of());
    }

    public void rebuild() {
        if (!rebuilding.compareAndSet(false, true)) {
            return;
        }
        profiles.loadAll().whenComplete((loaded, throwable) -> {
            try {
                if (throwable != null || loaded == null) {
                    return;
                }
                Map<Category, List<Entry>> rebuilt = new EnumMap<>(Category.class);
                for (Category category : Category.values()) {
                    rebuilt.put(category, loaded.stream()
                            .map(profile -> new Entry(profile.uuid(), name(profile.uuid()), value(category, profile)))
                            .filter(entry -> entry.value() > 0L || category == Category.DEATHS)
                            .sorted(Comparator.comparingLong(Entry::value).reversed())
                            .limit(10)
                            .toList());
                }
                cache.clear();
                cache.putAll(rebuilt);
                expiresAt = System.currentTimeMillis() + 60_000L;
            } finally {
                rebuilding.set(false);
            }
        });
    }

    private long value(Category category, Profile profile) {
        return switch (category) {
            case PLAYTIME -> Math.max(profile.statistic("playtime"), states.playtime(profile.uuid()));
            case KILLSTREAK -> Math.max(profile.statistic("killstreak"), states.killstreak(profile.uuid()));
            case WEALTH -> Math.max(profile.statistic("balance"), economy.balance(profile.uuid()));
            default -> category.value(profile);
        };
    }

    private static String name(UUID uuid) {
        return uuid.toString().substring(0, 8);
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
