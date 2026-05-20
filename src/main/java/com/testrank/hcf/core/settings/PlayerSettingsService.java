package com.testrank.hcf.core.settings;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.profile.Profile;
import com.testrank.hcf.core.profile.ProfileService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerSettingsService implements HCFService {
    public static final List<SettingDefinition> DEFINITIONS = List.of(
            new SettingDefinition("lunar-team-view", "Lunar Team View", true),
            new SettingDefinition("lunar-nametags", "Lunar Nametags", true),
            new SettingDefinition("dtr-hearts", "DTR Hearts", true),
            new SettingDefinition("teamfight-statistics", "Teamfight Statistics", true),
            new SettingDefinition("teamfight-scoreboard", "Teamfight Scoreboard", true),
            new SettingDefinition("ability-cooldown-scoreboard", "Ability Cooldown Scoreboard", true),
            new SettingDefinition("outpost-scoreboard", "Outpost Scoreboard", true),
            new SettingDefinition("sale-timer-scoreboard", "Sale Timer Scoreboard", true),
            new SettingDefinition("focus-by-hit", "Focus by Hit", true),
            new SettingDefinition("mob-drops", "Mob Drops Toggle", true),
            new SettingDefinition("cobblestone-pickup", "Cobblestone Pickup", true),
            new SettingDefinition("hologram-visibility", "Hologram Visibility", true),
            new SettingDefinition("death-messages", "Death Messages", true),
            new SettingDefinition("annoying-messages", "Annoying Messages", true),
            new SettingDefinition("on-screen-warnings", "On-Screen Warnings", true),
            new SettingDefinition("lambo-mode", "Lambo Mode", false)
    );

    private final ProfileService profiles;

    public PlayerSettingsService(ProfileService profiles) {
        this.profiles = profiles;
    }

    public boolean enabled(UUID uuid, String key) {
        SettingDefinition definition = definition(key).orElse(new SettingDefinition(key, key, true));
        return profiles.cached(uuid).map(profile -> profile.setting(key, definition.defaultEnabled())).orElse(definition.defaultEnabled());
    }

    public CompletableFuture<Boolean> toggle(UUID uuid, String key) {
        Profile profile = profiles.cached(uuid).orElseThrow(() -> new IllegalStateException("Profile is not loaded."));
        SettingDefinition definition = definition(key).orElse(new SettingDefinition(key, key, true));
        boolean next = profile.toggleSetting(key, definition.defaultEnabled());
        return profiles.save(profile).thenApply(ignored -> next);
    }

    public String chatColor(UUID uuid) {
        return profiles.cached(uuid).map(Profile::chatColor).orElse("&f");
    }

    public CompletableFuture<Void> chatColor(UUID uuid, String color) {
        Profile profile = profiles.cached(uuid).orElseThrow(() -> new IllegalStateException("Profile is not loaded."));
        profile.chatColor(color);
        return profiles.save(profile);
    }

    public Optional<SettingDefinition> definition(String key) {
        return DEFINITIONS.stream().filter(definition -> definition.key().equalsIgnoreCase(key)).findFirst();
    }

    public record SettingDefinition(String key, String displayName, boolean defaultEnabled) {}

    public enum ChatColorOption {
        GOLD("Gold", "&6", "hcf.chatcolor.gold"),
        GRAY("Gray", "&7", "hcf.chatcolor.gray"),
        BLUE("Blue", "&9", "hcf.chatcolor.blue"),
        GREEN("Green", "&a", "hcf.chatcolor.green"),
        RED("Red", "&c", "hcf.chatcolor.red"),
        PURPLE("Purple", "&d", "hcf.chatcolor.purple"),
        YELLOW("Yellow", "&e", "hcf.chatcolor.yellow"),
        WHITE("White", "&f", "hcf.chatcolor.white"),
        AQUA("Aqua", "&b", "hcf.chatcolor.aqua"),
        DARK_RED("Dark Red", "&4", "hcf.chatcolor.darkred"),
        DARK_GREEN("Dark Green", "&2", "hcf.chatcolor.darkgreen"),
        DARK_AQUA("Dark Aqua", "&3", "hcf.chatcolor.darkaqua"),
        DARK_BLUE("Dark Blue", "&1", "hcf.chatcolor.darkblue"),
        DARK_PURPLE("Dark Purple", "&5", "hcf.chatcolor.darkpurple"),
        BLACK("Black", "&0", "hcf.chatcolor.black");

        private final String displayName;
        private final String color;
        private final String permission;

        ChatColorOption(String displayName, String color, String permission) {
            this.displayName = displayName;
            this.color = color;
            this.permission = permission;
        }

        public String displayName() {
            return displayName;
        }

        public String color() {
            return color;
        }

        public String permission() {
            return permission;
        }

        public static Optional<ChatColorOption> byColor(String color) {
            return Arrays.stream(values()).filter(option -> option.color.equalsIgnoreCase(color)).findFirst();
        }
    }
}
