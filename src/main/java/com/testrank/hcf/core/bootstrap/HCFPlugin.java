package com.testrank.hcf.core.bootstrap;

import com.testrank.hcf.core.abilities.AbilityService;
import com.testrank.hcf.core.anticheat.AntiCheatBridge;
import com.testrank.hcf.core.api.PermissionService;
import com.testrank.hcf.core.api.PlaceholderService;
import com.testrank.hcf.core.api.ServiceRegistry;
import com.testrank.hcf.core.chat.ChatService;
import com.testrank.hcf.core.claim.ClaimRepository;
import com.testrank.hcf.core.claim.ClaimSelectionService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.classes.PvpClassService;
import com.testrank.hcf.core.combat.AntiCleanService;
import com.testrank.hcf.core.combat.CombatService;
import com.testrank.hcf.core.commands.CoreCommand;
import com.testrank.hcf.core.commands.FreezeCommand;
import com.testrank.hcf.core.commands.HCFCommand;
import com.testrank.hcf.core.commands.ReportCommand;
import com.testrank.hcf.core.commands.StaffCommand;
import com.testrank.hcf.core.commands.TeamCommand;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.citadel.CitadelService;
import com.testrank.hcf.core.conquest.ConquestService;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.eotw.EotwService;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.freeze.FreezeService;
import com.testrank.hcf.core.glowstone.GlowstoneService;
import com.testrank.hcf.core.koth.KothService;
import com.testrank.hcf.core.leaderboard.LeaderboardService;
import com.testrank.hcf.core.listeners.ClaimMovementListener;
import com.testrank.hcf.core.listeners.ClaimWandListener;
import com.testrank.hcf.core.listeners.ChatListener;
import com.testrank.hcf.core.listeners.CommandBlockListener;
import com.testrank.hcf.core.listeners.CombatListener;
import com.testrank.hcf.core.listeners.LogoutListener;
import com.testrank.hcf.core.listeners.MenuListener;
import com.testrank.hcf.core.listeners.PartnerItemListener;
import com.testrank.hcf.core.listeners.PvpProtectionListener;
import com.testrank.hcf.core.listeners.ProfileListener;
import com.testrank.hcf.core.listeners.ReadOnlyInventoryListener;
import com.testrank.hcf.core.listeners.SotwListener;
import com.testrank.hcf.core.listeners.StaffListener;
import com.testrank.hcf.core.listeners.SettingsGameplayListener;
import com.testrank.hcf.core.listeners.StatsTrackingListener;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import com.testrank.hcf.core.menu.MenuService;
import com.testrank.hcf.core.mongo.MongoManager;
import com.testrank.hcf.core.nametag.NametagService;
import com.testrank.hcf.core.packets.PacketHookService;
import com.testrank.hcf.core.particle.ParticleIntelService;
import com.testrank.hcf.core.partneritems.PartnerItemService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.profile.ProfileRepository;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.pvp.PvpProtectionService;
import com.testrank.hcf.core.redis.RedisManager;
import com.testrank.hcf.core.scoreboard.ScoreboardService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.shop.ShopService;
import com.testrank.hcf.core.sotw.SotwService;
import com.testrank.hcf.core.staff.LastInventoryService;
import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.staff.ReportService;
import com.testrank.hcf.core.tab.TabService;
import com.testrank.hcf.core.team.TeamRepository;
import com.testrank.hcf.core.team.DtrService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.threading.TpsService;
import com.testrank.hcf.core.timer.CooldownService;
import com.testrank.hcf.core.timer.GlobalTimerService;
import com.testrank.hcf.core.vanish.VanishService;
import com.testrank.hcf.core.waypoint.WaypointService;
import org.bukkit.command.PluginCommand;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;

public final class HCFPlugin extends JavaPlugin {
    private ServiceRegistry services;

    @Override
    public void onEnable() {
        if (!isSupportedServer()) {
            getLogger().severe("HCF legacy build requires Spigot/Bukkit 1.7.x or 1.8.x. Detected Bukkit version: " + getServer().getBukkitVersion());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        saveBundledResource("abilities.yml");
        saveBundledResource("conquest.yml");
        saveBundledResource("events.yml");
        saveBundledResource("factions.yml");
        saveBundledResource("kits.yml");
        saveBundledResource("koths.yml");
        saveBundledResource("shop.yml");
        saveBundledResource("stats.yml");
        HCFSettings settings = HCFSettings.from(getConfig());
        validateConfig();
        services = new ServiceRegistry();

        Threading threading = services.register(Threading.class, new Threading(this));
        TpsService tps = services.register(TpsService.class, new TpsService(this));
        MongoManager mongo = services.register(MongoManager.class, new MongoManager(settings));
        RedisManager redis = services.register(RedisManager.class, new RedisManager(settings));
        ParticleIntelService particleIntel = services.register(ParticleIntelService.class, new ParticleIntelService(this, redis, threading));
        ProfileService profiles = services.register(ProfileService.class, new ProfileService(new ProfileRepository(mongo), redis));
        PlayerStateService states = services.register(PlayerStateService.class, new PlayerStateService(settings, profiles));
        TeamService teams = services.register(TeamService.class, new TeamService(new TeamRepository(mongo), profiles, redis, settings));
        DtrService dtr = services.register(DtrService.class, new DtrService(this, teams, settings, particleIntel));
        ClaimService claims = services.register(ClaimService.class, new ClaimService(new ClaimRepository(mongo)));
        ClaimSelectionService claimSelections = services.register(ClaimSelectionService.class, new ClaimSelectionService());
        CombatService combat = services.register(CombatService.class, new CombatService(settings));
        CooldownService cooldowns = services.register(CooldownService.class, new CooldownService(profiles));
        ClientIntegrationService clients = services.register(ClientIntegrationService.class, new ClientIntegrationService(this));
        StaffService staff = services.register(StaffService.class, new StaffService(this, clients));
        ReportService reports = services.register(ReportService.class, new ReportService());
        LastInventoryService lastInventories = services.register(LastInventoryService.class, new LastInventoryService());
        EventService events = services.register(EventService.class, new EventService());
        GlobalTimerService globalTimers = services.register(GlobalTimerService.class, new GlobalTimerService());
        EotwService eotw = services.register(EotwService.class, new EotwService());
        SotwService sotw = services.register(SotwService.class, new SotwService());
        KothService koths = services.register(KothService.class, new KothService(this, events, claims, teams, profiles, settings));
        PlayerSettingsService playerSettings = services.register(PlayerSettingsService.class, new PlayerSettingsService(profiles));
        AntiCleanService antiClean = services.register(AntiCleanService.class, new AntiCleanService(this, teams, koths, settings, playerSettings));

        PermissionService permissions = services.register(PermissionService.class, new PermissionService());
        WaypointService waypoints = services.register(WaypointService.class, new WaypointService(teams, clients, playerSettings));
        services.register(PlaceholderService.class, new PlaceholderService());
        MenuService menus = services.register(MenuService.class, new MenuService());
        services.register(NametagService.class, new NametagService(this, teams, combat, clients, staff, permissions, playerSettings));
        PvpClassService pvpClasses = services.register(PvpClassService.class, new PvpClassService(this, profiles, settings));
        services.register(ScoreboardService.class, new ScoreboardService(this, profiles, combat, antiClean, cooldowns, events, koths, globalTimers, sotw, staff, tps, teams, pvpClasses, playerSettings));
        services.register(PacketHookService.class, new PacketHookService(this, clients, particleIntel));
        ChatService chat = services.register(ChatService.class, new ChatService(teams, permissions, playerSettings));
        EconomyService economy = services.register(EconomyService.class, new EconomyService(profiles, redis));
        ShopService shop = services.register(ShopService.class, new ShopService(this, economy, mongo, threading));
        LeaderboardService leaderboards = services.register(LeaderboardService.class, new LeaderboardService(profiles, states, economy));
        services.register(AbilityService.class, new AbilityService(cooldowns, clients));
        PartnerItemService partnerItems = services.register(PartnerItemService.class, new PartnerItemService(cooldowns, clients));
        PvpProtectionService pvpProtection = services.register(PvpProtectionService.class, new PvpProtectionService(profiles));
        services.register(AntiCheatBridge.class, new AntiCheatBridge());
        services.register(VanishService.class, new VanishService(staff));
        services.register(FreezeService.class, new FreezeService(staff));
        services.register(TabService.class, new TabService(this, teams, tps, profiles, claims, events));
        services.register(ConquestService.class, new ConquestService(events));
        services.register(CitadelService.class, new CitadelService(events));
        GlowstoneService glowstone = services.register(GlowstoneService.class, new GlowstoneService(this, events, settings));

        services.startAll();
        registerListeners(
                new CommandBlockListener(),
                new ProfileListener(profiles, states, pvpProtection, waypoints, threading, playerSettings, teams, particleIntel),
                new CombatListener(this, combat, antiClean, cooldowns, profiles, states, settings, dtr, teams, lastInventories, playerSettings, particleIntel),
                new LogoutListener(states),
                new ClaimMovementListener(claims, teams, playerSettings),
                new ClaimWandListener(claimSelections, claims, teams, economy, settings, threading, particleIntel),
                new ChatListener(chat, teams, threading),
                new MenuListener(services.require(MenuService.class)),
                new ReadOnlyInventoryListener(),
                new PartnerItemListener(partnerItems),
                new PvpProtectionListener(pvpProtection),
                new SettingsGameplayListener(playerSettings),
                new StatsTrackingListener(profiles),
                new SotwListener(sotw),
                new StaffListener(staff, states),
                glowstone
        );
        command("team").setExecutor(new TeamCommand(teams, dtr, claims, claimSelections, threading, economy, states, chat, waypoints, settings, this, menus, particleIntel));
        command("staff").setExecutor(new StaffCommand(staff));
        command("freeze").setExecutor(new FreezeCommand(staff));
        HCFCommand adminCommand = new HCFCommand(claims, claimSelections, teams, partnerItems, koths, eotw, sotw, globalTimers, threading);
        CoreCommand coreCommand = new CoreCommand(this, profiles, states, economy, staff, reports, lastInventories, teams, claims, dtr, combat, pvpProtection, chat, events, koths, sotw, eotw, globalTimers, threading, menus, playerSettings, shop, leaderboards, particleIntel);
        command("hcf").setExecutor(adminCommand);
        command("claimwand").setExecutor(adminCommand);
        command("claim").setExecutor(adminCommand);
        command("claimhere").setExecutor(adminCommand);
        command("partner").setExecutor(adminCommand);
        command("koth").setExecutor(adminCommand);
        command("sotw").setExecutor(adminCommand);
        command("eotw").setExecutor(adminCommand);
        command("timer").setExecutor(adminCommand);
        command("report").setExecutor(new ReportCommand(reports));
        registerCoreCommands(coreCommand);
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.close();
        }
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private PluginCommand command(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command missing from plugin.yml: " + name);
        }
        return command;
    }

    private void saveBundledResource(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    private void validateConfig() {
        String spawnWorld = getConfig().getString("spawn.world", "world");
        if (getServer().getWorld(spawnWorld) == null) {
            getLogger().warning("Configured spawn.world '" + spawnWorld + "' is not loaded; falling back to the default world.");
        }
        if (getConfig().getDouble("claims.price-per-block", 2.0D) < 0.0D) {
            getLogger().warning("claims.price-per-block cannot be negative; runtime value will be clamped to 0.");
        }
        if (getConfig().getInt("claims.minimum-size", 5) <= 0) {
            getLogger().warning("claims.minimum-size must be positive; runtime value will use the default.");
        }
        if (getConfig().getInt("claims.maximum-size", 150) < getConfig().getInt("claims.minimum-size", 5)) {
            getLogger().warning("claims.maximum-size is smaller than claims.minimum-size; runtime value will be clamped.");
        }
        if (getConfig().getInt("faction.max-members", 25) <= 0) {
            getLogger().warning("faction.max-members must be positive; runtime value will use the default.");
        }
        if (getConfig().getInt("faction.max-claims", 6) <= 0) {
            getLogger().warning("faction.max-claims must be positive; runtime value will use the default.");
        }
        if (getConfig().getInt("timers.rod", 3) <= 0) {
            getLogger().warning("timers.rod must be positive; runtime value will use the default.");
        }
        if (getConfig().getInt("combat.enderpearl-seconds", 16) <= 0) {
            getLogger().warning("combat.enderpearl-seconds must be positive; runtime value will use the default.");
        }
        if (getConfig().getInt("koth.cap-time-seconds", 900) <= 0) {
            getLogger().warning("koth.cap-time-seconds must be positive; runtime value will use the default.");
        }
        validateWorld("deathban-arena.world");
        validateWorld("holograms.location.world");
        validateDuration("timers.logout", 30);
        validateDuration("timers.home", 10);
        validateDuration("deathban.default-duration-minutes", 30);
        validateDuration("sotw.default-minutes", 60);
        validatePotionEffects("potion-limiter.allowed");
        validateSounds("sounds");
        validateShopConfig();
    }

    private void validateWorld(String path) {
        String world = getConfig().getString(path);
        if (world != null && getServer().getWorld(world) == null) {
            getLogger().warning("Configured world at " + path + " is not loaded: " + world);
        }
    }

    private void validateDuration(String path, int fallback) {
        if (getConfig().contains(path) && getConfig().getInt(path, fallback) <= 0) {
            getLogger().warning(path + " must be positive; runtime value will use the default.");
        }
    }

    private void validatePotionEffects(String path) {
        for (String name : getConfig().getStringList(path)) {
            if (PotionEffectType.getByName(name) == null) {
                getLogger().warning("Invalid potion effect at " + path + ": " + name);
            }
        }
    }

    private void validateSounds(String path) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(true)) {
            if (!section.isString(key)) {
                continue;
            }
            String value = section.getString(key);
            try {
                Sound.valueOf(value);
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Invalid sound at " + path + "." + key + ": " + value);
            }
        }
    }

    private void validateShopConfig() {
        File file = new File(getDataFolder(), "shop.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration shop = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection categories = shop.getConfigurationSection("categories");
        if (categories == null) {
            getLogger().warning("shop.yml is missing a categories section.");
            return;
        }
        for (String category : categories.getKeys(false)) {
            String base = "categories." + category;
            validateShopMaterial(shop, base + ".icon");
            validateShopSlot(shop, base + ".slot", 0, 35);
            ConfigurationSection items = shop.getConfigurationSection(base + ".items");
            if (items == null) {
                continue;
            }
            for (String item : items.getKeys(false)) {
                String itemPath = base + ".items." + item;
                validateShopMaterial(shop, itemPath + ".material");
                validateShopSlot(shop, itemPath + ".slot", 0, 53);
                validateShopPrice(shop, itemPath + ".buy");
                validateShopPrice(shop, itemPath + ".sell");
            }
        }
    }

    private void validateShopMaterial(YamlConfiguration config, String path) {
        String value = config.getString(path);
        if (value != null && Material.matchMaterial(value) == null) {
            getLogger().warning("Invalid material in shop.yml at " + path + ": " + value);
        }
    }

    private void validateShopSlot(YamlConfiguration config, String path, int min, int max) {
        if (config.contains(path)) {
            int slot = config.getInt(path);
            if (slot < min || slot > max) {
                getLogger().warning("Invalid GUI slot in shop.yml at " + path + ": " + slot + " (allowed " + min + "-" + max + ")");
            }
        }
    }

    private void validateShopPrice(YamlConfiguration config, String path) {
        if (config.contains(path) && config.getLong(path) < -1L) {
            getLogger().warning("Invalid price in shop.yml at " + path + ": " + config.getLong(path) + " (use -1 to disable)");
        }
    }

    private void registerCoreCommands(CoreCommand coreCommand) {
        String[] names = {
                "help", "request", "gamemode", "broadcast", "clearchat", "heal", "feed", "kill", "invsee", "message",
                "ping", "tp", "tphere", "tplocation", "tpall", "more", "world", "top", "ignore", "rename", "repair",
                "clear", "balance", "cobble", "basetoken", "falltraptoken", "crowbar", "ecomanage", "enchant",
                "editmenu", "endplayers", "netherplayers", "focus", "unfocus", "near", "lives", "lff", "livesmanage",
                "logout", "leaderboards", "leaderboard", "managebasetoken", "managefalltraptoken", "playtime", "redeem", "pvp",
                "resetredeem", "reclaim", "resetreclaim", "lastinv", "sendbasetoken", "sendfalltraptoken", "setend",
                "settings", "spawn", "vanish", "staffchat", "telllocation", "stats", "strengthnerf", "togglepm",
                "togglecobble", "togglesounds", "deathban", "pay", "killtag", "schedule", "customtimer", "keyall",
                "reportsmenu", "requestsmenu", "staffbuild", "spawner", "killstreak", "kit", "conquest", "ktk",
                "purge", "citadel", "mountain", "systemteam", "setbal", "changelog", "panic",
                "discord", "teamspeak", "twitter", "store", "social", "website", "media", "giveaway", "shop", "chatcolor", "link"
        };
        for (String name : names) {
            command(name).setExecutor(coreCommand);
        }
    }

    private boolean isSupportedServer() {
        String version = getServer().getBukkitVersion();
        return version.startsWith("1.7") || version.startsWith("1.8");
    }
}
