package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.chat.ChatService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.combat.CombatService;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.eotw.EotwService;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEventType;
import com.testrank.hcf.core.koth.KothService;
import com.testrank.hcf.core.leaderboard.LeaderboardService;
import com.testrank.hcf.core.menu.ChatColorMenu;
import com.testrank.hcf.core.menu.HelpMenu;
import com.testrank.hcf.core.menu.LeaderboardMenu;
import com.testrank.hcf.core.menu.MenuService;
import com.testrank.hcf.core.menu.SettingsMenu;
import com.testrank.hcf.core.particle.ParticleIntelService;
import com.testrank.hcf.core.shop.ShopMenu;
import com.testrank.hcf.core.shop.ShopService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.pvp.PvpProtectionService;
import com.testrank.hcf.core.sotw.SotwService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.staff.LastInventoryService;
import com.testrank.hcf.core.staff.ReportService;
import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.team.DtrService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.timer.GlobalTimerService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CoreCommand implements CommandExecutor {
    private final Plugin plugin;
    private final ProfileService profiles;
    private final PlayerStateService states;
    private final EconomyService economy;
    private final StaffService staff;
    private final ReportService reports;
    private final LastInventoryService lastInventories;
    private final TeamService teams;
    private final DtrService dtr;
    private final ClaimService claims;
    private final CombatService combat;
    private final PvpProtectionService pvp;
    private final ChatService chat;
    private final EventService events;
    private final KothService koths;
    private final SotwService sotw;
    private final EotwService eotw;
    private final GlobalTimerService timers;
    private final Threading threading;
    private final MenuService menus;
    private final PlayerSettingsService playerSettings;
    private final ShopService shop;
    private final LeaderboardService leaderboardService;
    private final ParticleIntelService intel;
    private final Set<UUID> giveawayEntries = ConcurrentHashMap.newKeySet();
    private Location spawn;
    private Location endExit;

    public CoreCommand(Plugin plugin, ProfileService profiles, PlayerStateService states, EconomyService economy, StaffService staff,
                       ReportService reports, LastInventoryService lastInventories, TeamService teams, ClaimService claims,
                       DtrService dtr, CombatService combat, PvpProtectionService pvp, ChatService chat, EventService events, KothService koths,
                       SotwService sotw, EotwService eotw, GlobalTimerService timers, Threading threading, MenuService menus,
                       PlayerSettingsService playerSettings, ShopService shop, LeaderboardService leaderboardService, ParticleIntelService intel) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.states = states;
        this.economy = economy;
        this.staff = staff;
        this.reports = reports;
        this.lastInventories = lastInventories;
        this.teams = teams;
        this.dtr = dtr;
        this.claims = claims;
        this.combat = combat;
        this.pvp = pvp;
        this.chat = chat;
        this.events = events;
        this.koths = koths;
        this.sotw = sotw;
        this.eotw = eotw;
        this.timers = timers;
        this.threading = threading;
        this.menus = menus;
        this.playerSettings = playerSettings;
        this.shop = shop;
        this.leaderboardService = leaderboardService;
        this.intel = intel;
        World world = Bukkit.getWorlds().get(0);
        this.spawn = configuredLocation(plugin, "spawn", world.getSpawnLocation());
        this.endExit = world.getSpawnLocation();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        try {
            switch (name) {
                case "help" -> help(sender);
                case "request" -> request(sender, args);
                case "gamemode" -> gamemode(sender, args);
                case "broadcast" -> broadcast(sender, args);
                case "clearchat" -> clearchat(sender);
                case "heal" -> heal(sender, args);
                case "feed" -> feed(sender, args);
                case "kill" -> kill(sender, args);
                case "invsee" -> invsee(sender, args);
                case "message", "msg", "tell", "w" -> message(sender, args);
                case "ping" -> ping(sender, args);
                case "tp" -> tp(sender, args);
                case "tphere" -> tphere(sender, args);
                case "tplocation" -> tplocation(sender, args);
                case "tpall" -> tpall(sender);
                case "more" -> more(sender);
                case "world" -> world(sender, args);
                case "top" -> top(sender);
                case "ignore" -> ignore(sender, args);
                case "rename" -> rename(sender, args);
                case "repair" -> repair(sender);
                case "clear" -> clear(sender, args);
                case "balance" -> balance(sender, args);
                case "setbal" -> setbal(sender, args);
                case "pay" -> pay(sender, args);
                case "cobble", "togglecobble" -> toggleCobble(sender);
                case "togglesounds" -> toggleSounds(sender);
                case "togglepm" -> togglePm(sender);
                case "basetoken", "falltraptoken", "managebasetoken", "managefalltraptoken", "sendbasetoken", "sendfalltraptoken" -> tokens(sender, name, args);
                case "crowbar" -> giveNamed(sender, Material.TRIPWIRE_HOOK, "&cCrowbar", 1);
                case "ecomanage" -> ecomanage(sender, args);
                case "enchant" -> enchant(sender, args);
                case "settings" -> settings(sender);
                case "editmenu", "reportsmenu", "requestsmenu" -> menu(sender, name);
                case "endplayers", "netherplayers" -> worldPlayers(sender, name.startsWith("end") ? "world_the_end" : "world_nether");
                case "focus", "unfocus" -> focus(sender, name, args);
                case "near" -> near(sender);
                case "lives", "livesmanage" -> lives(sender, name, args);
                case "lff" -> lff(sender);
                case "logout" -> logout(sender);
                case "leaderboards", "leaderboard" -> leaderboards(sender);
                case "playtime" -> playtime(sender, args);
                case "redeem", "resetredeem", "reclaim", "resetreclaim" -> redeem(sender, name, args);
                case "pvp" -> pvp(sender, args);
                case "lastinv" -> lastinv(sender, args);
                case "setend" -> setend(sender);
                case "spawn" -> spawn(sender);
                case "vanish" -> vanish(sender);
                case "staffchat" -> staffchat(sender, args);
                case "telllocation" -> tellLocation(sender, args);
                case "stats" -> stats(sender, args);
                case "strengthnerf" -> strengthNerf(sender);
                case "deathban" -> deathban(sender, args);
                case "killtag" -> killtag(sender, args);
                case "schedule" -> schedule(sender);
                case "customtimer" -> customTimer(sender, args);
                case "keyall" -> keyall(sender, args);
                case "staffbuild" -> staffBuild(sender);
                case "spawner" -> giveNamed(sender, Material.MOB_SPAWNER, "&cSpawner", 1);
                case "killstreak" -> killstreak(sender, args);
                case "kit" -> kit(sender, args);
                case "conquest", "ktk", "purge", "citadel", "mountain", "systemteam" -> eventCommand(sender, name, args);
                case "changelog", "changelogs" -> changelog(sender);
                case "panic" -> panic(sender, args);
                case "discord", "teamspeak", "twitter", "store", "social", "website" -> social(sender, name);
                case "media" -> media(sender);
                case "giveaway" -> giveaway(sender, args);
                case "shop" -> shop(sender);
                case "chatcolor", "chatcolors" -> chatColor(sender);
                case "link" -> link(sender, args);
                default -> help(sender);
            }
        } catch (RuntimeException exception) {
            sender.sendMessage(color("&8[&cHCF&8] &c" + exception.getMessage()));
        }
        return true;
    }

    private void help(CommandSender sender) {
        if (sender instanceof Player player) {
            new HelpMenu(profiles, states, teams, dtr, events, timers).open(player, menus);
            return;
        }
        sender.sendMessage(color("&8[&cHCF&8] &fPlayer help opens an inventory menu. Core commands: &c/team&f, &c/staff&f, &c/timer&f, &c/koth&f."));
    }

    private void request(CommandSender sender, String[] args) {
        Player player = player(sender);
        String reason = args.length == 0 ? "No reason provided" : join(args, 0);
        Bukkit.getOnlinePlayers().stream().filter(staffer -> staffer.hasPermission("hcf.staff"))
                .forEach(staffer -> staffer.sendMessage(color("&8[&cRequest&8] &f" + player.getName() + "&7: &c" + reason)));
        sender.sendMessage(color("&8[&cHCF&8] &fYour request was sent to online staff."));
    }

    private void gamemode(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        Player target = args.length >= 2 ? target(args[1]) : player(sender);
        String mode = args.length == 0 ? "1" : args[0].toLowerCase(Locale.ROOT);
        GameMode gameMode = switch (mode) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> GameMode.CREATIVE;
        };
        target.setGameMode(gameMode);
        sender.sendMessage(color("&8[&cHCF&8] &fSet &c" + target.getName() + "&f to &c" + gameMode.name() + "&f."));
    }

    private void broadcast(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        Bukkit.broadcastMessage(color("&8[&4Alert&8] &f" + joinRequired(args, 0)));
    }

    private void clearchat(CommandSender sender) {
        require(sender, "hcf.staff");
        for (int i = 0; i < 120; i++) Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(color("&8[&cHCF&8] &fChat was cleared by &c" + sender.getName() + "&f."));
    }

    private void heal(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        target.setHealth(target.getMaxHealth());
        target.setFireTicks(0);
        target.sendMessage(color("&8[&cHCF&8] &fYou were healed."));
    }

    private void feed(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        target.setFoodLevel(20);
        target.setSaturation(20F);
        target.sendMessage(color("&8[&cHCF&8] &fYou were fed."));
    }

    private void kill(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        target(arg(args, 0, "/kill <player>")).setHealth(0D);
        sender.sendMessage(color("&8[&cHCF&8] &fPlayer killed."));
    }

    private void invsee(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        player(sender).openInventory(target(arg(args, 0, "/invsee <player>")).getInventory());
    }

    private void message(CommandSender sender, String[] args) {
        Player player = player(sender);
        Player target = target(arg(args, 0, "/message <player> <message>"));
        if (states.pmDisabled(target.getUniqueId()) || states.ignoring(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(color("&8[&cHCF&8] &cThat player is not accepting messages."));
            return;
        }
        String message = joinRequired(args, 1);
        player.sendMessage(color("&7(To &c" + target.getName() + "&7) &f" + message));
        target.sendMessage(color("&7(From &c" + player.getName() + "&7) &f" + message));
    }

    private void ping(CommandSender sender, String[] args) {
        Player target = args.length == 0 ? player(sender) : target(args[0]);
        sender.sendMessage(color("&8[&cHCF&8] &f" + target.getName() + "'s ping: &c" + pingValue(target) + "ms"));
    }

    private void tp(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player player = player(sender);
        Player target = target(arg(args, 0, "/tp <player>"));
        player.teleport(target);
        player.sendMessage(color("&8[&cHCF&8] &fTeleported to &c" + target.getName() + "&f."));
    }

    private void tphere(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        target(arg(args, 0, "/tphere <player>")).teleport(player(sender));
    }

    private void tplocation(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player player = player(sender);
        World world = args.length >= 4 ? world(args[3]) : player.getWorld();
        player.teleport(new Location(world,
                Double.parseDouble(arg(args, 0, "/tplocation <x> <y> <z> [world]")),
                Double.parseDouble(arg(args, 1, "/tplocation <x> <y> <z> [world]")),
                Double.parseDouble(arg(args, 2, "/tplocation <x> <y> <z> [world]"))));
    }

    private void tpall(CommandSender sender) {
        require(sender, "hcf.admin");
        Player player = player(sender);
        Bukkit.getOnlinePlayers().forEach(target -> target.teleport(player));
    }

    private void more(CommandSender sender) {
        require(sender, "hcf.admin");
        ItemStack item = player(sender).getItemInHand();
        if (item == null || item.getType() == Material.AIR) throw new IllegalArgumentException("Hold an item first.");
        item.setAmount(item.getMaxStackSize());
    }

    private void world(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        player(sender).teleport(world(arg(args, 0, "/world <world>")).getSpawnLocation());
    }

    private void top(CommandSender sender) {
        require(sender, "hcf.staff");
        Player player = player(sender);
        player.teleport(new Location(player.getWorld(), player.getLocation().getBlockX() + 0.5D, player.getWorld().getHighestBlockYAt(player.getLocation()) + 1D, player.getLocation().getBlockZ() + 0.5D));
    }

    private void ignore(CommandSender sender, String[] args) {
        Player player = player(sender);
        boolean enabled = states.toggleIgnore(player.getUniqueId(), target(arg(args, 0, "/ignore <player>")).getUniqueId());
        player.sendMessage(color("&8[&cHCF&8] &fIgnore is now " + (enabled ? "&aenabled" : "&cdisabled") + "&f."));
    }

    private void rename(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        ItemStack item = player(sender).getItemInHand();
        if (item == null || item.getType() == Material.AIR) throw new IllegalArgumentException("Hold an item first.");
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(joinRequired(args, 0)));
        item.setItemMeta(meta);
    }

    private void repair(CommandSender sender) {
        require(sender, "hcf.admin");
        ItemStack item = player(sender).getItemInHand();
        if (item != null) item.setDurability((short) 0);
        sender.sendMessage(color("&8[&cHCF&8] &fItem repaired."));
    }

    private void clear(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        target.getInventory().clear();
        target.getInventory().setArmorContents(null);
    }

    private void balance(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        sender.sendMessage(color("&8[&cEco&8] &f" + target.getName() + "'s balance: &a$" + economy.balance(target.getUniqueId())));
    }

    private void pay(CommandSender sender, String[] args) {
        Player player = player(sender);
        Player target = target(arg(args, 0, "/pay <player> <amount>"));
        long amount = positiveLong(arg(args, 1, "/pay <player> <amount>"));
        if (player.equals(target)) {
            throw new IllegalArgumentException("You cannot pay yourself.");
        }
        economy.withdraw(player.getUniqueId(), amount).thenAccept(success -> {
            if (!success) {
                threading.runSync(() -> player.sendMessage(color("&8[&cEco&8] &cYou do not have enough money.")));
                return;
            }
            economy.add(target.getUniqueId(), amount).thenRun(() -> threading.runSync(() -> {
                player.sendMessage(color("&8[&cEco&8] &fSent &a$" + amount + " &fto &c" + target.getName() + "&f."));
                target.sendMessage(color("&8[&cEco&8] &fReceived &a$" + amount + " &ffrom &c" + player.getName() + "&f."));
            }));
        });
    }

    private void setbal(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        Player target;
        long amount;
        if (args.length == 1) {
            target = player(sender);
            amount = Long.parseLong(args[0]);
        } else {
            target = target(arg(args, 0, "/setbal [player] <amount>"));
            amount = Long.parseLong(arg(args, 1, "/setbal [player] <amount>"));
        }
        economy.set(target.getUniqueId(), amount);
        sender.sendMessage(color("&8[&cEco&8] &fSet &c" + target.getName() + "&f's balance to &a$" + amount + "&f."));
    }

    private void toggleCobble(CommandSender sender) {
        boolean disabled = states.toggleCobble(player(sender).getUniqueId());
        sender.sendMessage(color("&8[&cHCF&8] &fCobble pickup: " + (disabled ? "&cDisabled" : "&aEnabled")));
    }

    private void toggleSounds(CommandSender sender) {
        boolean disabled = states.toggleSounds(player(sender).getUniqueId());
        sender.sendMessage(color("&8[&cHCF&8] &fSounds: " + (disabled ? "&cDisabled" : "&aEnabled")));
    }

    private void togglePm(CommandSender sender) {
        boolean disabled = states.togglePm(player(sender).getUniqueId());
        sender.sendMessage(color("&8[&cHCF&8] &fPrivate messages: " + (disabled ? "&cDisabled" : "&aEnabled")));
    }

    private void tokens(CommandSender sender, String name, String[] args) {
        require(sender, "hcf.admin");
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        int amount = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
        boolean base = name.contains("base");
        int value = base ? states.addBaseTokens(target.getUniqueId(), amount) : states.addFalltrapTokens(target.getUniqueId(), amount);
        sender.sendMessage(color("&8[&cTokens&8] &f" + target.getName() + " now has &c" + value + (base ? " base" : " falltrap") + " tokens&f."));
    }

    private void ecomanage(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        String action = arg(args, 0, "/ecomanage <set|add> <player> <amount>");
        Player target = target(arg(args, 1, "/ecomanage <set|add> <player> <amount>"));
        long amount = Long.parseLong(arg(args, 2, "/ecomanage <set|add> <player> <amount>"));
        if (action.equalsIgnoreCase("set")) economy.set(target.getUniqueId(), amount);
        else economy.add(target.getUniqueId(), amount);
        sender.sendMessage(color("&8[&cEco&8] &fUpdated &c" + target.getName() + "&f."));
    }

    private void enchant(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        Enchantment enchantment = Enchantment.getByName(arg(args, 0, "/enchant <enchant> <level>").toUpperCase(Locale.ROOT));
        if (enchantment == null) throw new IllegalArgumentException("Unknown enchantment.");
        player(sender).getItemInHand().addUnsafeEnchantment(enchantment, Integer.parseInt(arg(args, 1, "/enchant <enchant> <level>")));
    }

    private void menu(CommandSender sender, String name) {
        require(sender, "hcf.staff");
        if (name.equals("reportsmenu")) {
            sender.sendMessage(color("&8&m----------------&8[ &cReports &8]&8&m----------------"));
            java.util.List<ReportService.Report> recent = reports.recent();
            recent.stream().skip(Math.max(0, recent.size() - 10)).forEach(report ->
                    sender.sendMessage(color("&c" + shortId(report.reporter()) + " &7reported &c" + shortId(report.target()) + "&7: &f" + report.reason())));
            sender.sendMessage(color("&8&m--------------------------------------------------"));
            return;
        }
        if (name.equals("requestsmenu")) {
            sender.sendMessage(color("&8[&cRequests&8] &fUse &c/request <message> &ffor live staff requests."));
            return;
        }
        settings(sender);
    }

    private void settings(CommandSender sender) {
        new SettingsMenu(menus, playerSettings).open(player(sender), menus);
    }

    private void worldPlayers(CommandSender sender, String worldName) {
        World world = Bukkit.getWorld(worldName);
        int count = world == null ? 0 : world.getPlayers().size();
        sender.sendMessage(color("&8[&cHCF&8] &fPlayers in &c" + worldName + "&f: &c" + count));
    }

    private void focus(CommandSender sender, String name, String[] args) {
        Player player = player(sender);
        var team = teams.byPlayer(player.getUniqueId()).orElseThrow(() -> new IllegalArgumentException("You are not in a faction."));
        team.focused(name.equals("unfocus") ? null : target(arg(args, 0, "/focus <player>")).getUniqueId());
        teams.save(team);
        player.sendMessage(color("&8[&cTeam&8] &fFocus updated."));
    }

    private void near(CommandSender sender) {
        Player player = player(sender);
        sender.sendMessage(color("&8&m----------------&8[ &cNearby &8]&8&m----------------"));
        Bukkit.getOnlinePlayers().stream()
                .filter(other -> other != player && other.getWorld().equals(player.getWorld()) && other.getLocation().distanceSquared(player.getLocation()) <= 200 * 200)
                .forEach(other -> sender.sendMessage(color("&c" + other.getName() + " &8- &f" + (int) other.getLocation().distance(player.getLocation()) + " blocks")));
    }

    private void lives(CommandSender sender, String name, String[] args) {
        if (name.equals("livesmanage")) {
            require(sender, "hcf.admin");
            states.addLives(target(arg(args, 0, "/livesmanage <player> <amount>")).getUniqueId(), Integer.parseInt(arg(args, 1, "/livesmanage <player> <amount>")));
        }
        Player target = args.length >= 1 && !name.equals("livesmanage") ? target(args[0]) : player(sender);
        sender.sendMessage(color("&8[&cLives&8] &f" + target.getName() + ": &c" + states.lives(target.getUniqueId())));
    }

    private void lff(CommandSender sender) {
        Bukkit.broadcastMessage(color("&8[&cLFF&8] &f" + sender.getName() + " &7is looking for a faction."));
    }

    private void logout(CommandSender sender) {
        Player player = player(sender);
        int seconds = Math.max(1, plugin.getConfig().getInt("timers.logout", 30));
        states.startLogout(player.getUniqueId(), seconds * 1000L);
        player.sendMessage(color("&8[&cLogout&8] &fDo not move or enter combat for &c" + seconds + " seconds &fto safely logout."));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || states.logoutRemaining(player.getUniqueId()) > 0L) {
                return;
            }
            states.cancelLogout(player.getUniqueId());
            player.kickPlayer(color("&aYou logged out safely."));
        }, seconds * 20L);
    }

    private void leaderboards(CommandSender sender) {
        new LeaderboardMenu(menus, leaderboardService).open(player(sender), menus);
    }

    private void shop(CommandSender sender) {
        new ShopMenu(menus, shop).open(player(sender), menus);
    }

    private void chatColor(CommandSender sender) {
        new ChatColorMenu(menus, playerSettings).open(player(sender), menus);
    }

    private void playtime(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        sender.sendMessage(color("&8[&cHCF&8] &fPlaytime: &c" + formatDuration(states.playtime(target.getUniqueId()))));
    }

    private void redeem(CommandSender sender, String name, String[] args) {
        Player player = args.length >= 1 && sender.hasPermission("hcf.admin") ? target(args[0]) : player(sender);
        if (name.equals("resetredeem")) {
            require(sender, "hcf.admin");
            states.resetRedeem(player.getUniqueId());
            sender.sendMessage(color("&8[&cHCF&8] &fReset redeem for &c" + player.getName() + "&f."));
        } else if (name.equals("resetreclaim")) {
            require(sender, "hcf.admin");
            states.resetReclaim(player.getUniqueId());
            sender.sendMessage(color("&8[&cHCF&8] &fReset reclaim for &c" + player.getName() + "&f."));
        } else if (name.equals("redeem")) {
            if (!states.markRedeemed(player.getUniqueId())) {
                sender.sendMessage(color("&8[&cHCF&8] &cYou already redeemed this map."));
                return;
            }
            economy.add(player.getUniqueId(), 500L);
            sender.sendMessage(color("&8[&cHCF&8] &fRedeemed &a$500&f."));
        } else if (name.equals("reclaim")) {
            if (!states.markReclaimed(player.getUniqueId())) {
                sender.sendMessage(color("&8[&cHCF&8] &cYou already reclaimed this map."));
                return;
            }
            player.getInventory().addItem(named(Material.TRIPWIRE_HOOK, "&cReclaim Key"));
            sender.sendMessage(color("&8[&cHCF&8] &fReclaim key added."));
        }
    }

    private void pvp(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (args.length >= 1 && args[0].equalsIgnoreCase("enable")) pvp.remove(player.getUniqueId());
        sender.sendMessage(color("&8[&cPvP&8] &fProtection: " + (pvp.protectedPlayer(player.getUniqueId()) ? "&aEnabled" : "&cDisabled")));
    }

    private void lastinv(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player viewer = player(sender);
        Player target = target(arg(args, 0, "/lastinv <player>"));
        Inventory inventory = lastInventories.inventory(target.getUniqueId(), target.getName());
        if (inventory == null) throw new IllegalArgumentException("No last inventory stored.");
        viewer.openInventory(inventory);
    }

    private void setend(CommandSender sender) {
        require(sender, "hcf.admin");
        endExit = player(sender).getLocation();
        sender.sendMessage(color("&8[&cHCF&8] &fEnd exit updated."));
    }

    private void spawn(CommandSender sender) {
        player(sender).teleport(spawn);
    }

    private void vanish(CommandSender sender) {
        require(sender, "hcf.staff");
        boolean enabled = staff.toggleVanish(player(sender));
        sender.sendMessage(color("&8[&cStaff&8] &fVanish: " + (enabled ? "&aEnabled" : "&cDisabled")));
    }

    private void staffchat(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        chat.staffChat(player(sender), joinRequired(args, 0));
    }

    private void tellLocation(CommandSender sender, String[] args) {
        Player player = player(sender);
        Player target = target(arg(args, 0, "/telllocation <player>"));
        Location loc = player.getLocation();
        target.sendMessage(color("&8[&cLocation&8] &f" + player.getName() + "&7: &c" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
    }

    private void stats(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        profiles.cached(target.getUniqueId()).ifPresent(profile -> {
            intel.profileExport(profile, teams.byPlayer(target.getUniqueId()).orElse(null), "stats_command");
            int kills = profile.kills();
            int deaths = profile.deaths();
            String kdr = deaths == 0 ? "Infinity" : String.format(Locale.US, "%.2f", (double) kills / deaths);
            String faction = teams.byPlayer(target.getUniqueId()).map(team -> team.name()).orElse("None");
            sender.sendMessage(color("&7&m--------------------------------------------------"));
            sender.sendMessage(color("&e" + target.getName() + "'s Statistics"));
            sender.sendMessage("");
            sender.sendMessage(color("&cKills&7: &f" + kills));
            sender.sendMessage(color("&cDeaths&7: &f" + deaths));
            sender.sendMessage(color("&cAssists&7: &f0"));
            sender.sendMessage(color("&cKill Streak&7: &f" + states.killstreak(target.getUniqueId())));
            sender.sendMessage(color("&cKDR&7: &f" + kdr));
            sender.sendMessage(color("&cFaction&7: &f" + faction));
            sender.sendMessage(color("&cPast Factions&7: &fNone"));
            sender.sendMessage(color("&cPlaytime&7: &f" + formatDuration(states.playtime(target.getUniqueId()))));
            sender.sendMessage(color("&7&m--------------------------------------------------"));
        });
    }

    private void strengthNerf(CommandSender sender) {
        boolean enabled = states.toggleStrengthNerf(player(sender).getUniqueId());
        sender.sendMessage(color("&8[&cHCF&8] &fStrength nerf: " + (enabled ? "&aEnabled" : "&cDisabled")));
    }

    private void deathban(CommandSender sender, String[] args) {
        require(sender, "hcf.staff");
        Player target = target(arg(args, 0, "/deathban <player> <minutes>"));
        long minutes = positiveLong(arg(args, 1, "/deathban <player> <minutes>"));
        states.deathban(target.getUniqueId(), minutes * 60_000L);
        if (sender instanceof Player staffer) {
            intel.staffNoteHook(staffer, target.getUniqueId(), target.getName(), "deathban", "Deathbanned for " + minutes + " minutes");
        }
        sender.sendMessage(color("&8[&cStaff&8] &fDeathbanned &c" + target.getName() + "&f."));
    }

    private void killtag(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        sender.sendMessage(color("&8[&cCombat&8] &f" + target.getName() + ": " + (combat.tag(target.getUniqueId()).isPresent() ? "&cTagged" : "&aSafe")));
    }

    private void schedule(CommandSender sender) {
        sender.sendMessage(color("&8[&cSchedule&8] &fActive events: &c" + events.events().stream().filter(event -> event.active()).count()));
    }

    private void customTimer(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        timers.create(arg(args, 0, "/customtimer <name> <minutes>"), positiveLong(arg(args, 1, "/customtimer <name> <minutes>")) * 60_000L);
        sender.sendMessage(color("&8[&cTimer&8] &fCustom timer created."));
    }

    private void keyall(CommandSender sender, String[] args) {
        require(sender, "hcf.admin");
        Bukkit.getOnlinePlayers().forEach(player -> player.getInventory().addItem(named(Material.TRIPWIRE_HOOK, args.length == 0 ? "&cKey" : join(args, 0))));
        Bukkit.broadcastMessage(color("&8[&cKeyAll&8] &fEveryone received a key."));
    }

    private void staffBuild(CommandSender sender) {
        require(sender, "hcf.staff");
        boolean enabled = states.toggleStaffBuild(player(sender).getUniqueId());
        sender.sendMessage(color("&8[&cStaff&8] &fStaff build: " + (enabled ? "&aEnabled" : "&cDisabled")));
    }

    private void killstreak(CommandSender sender, String[] args) {
        Player target = args.length >= 1 ? target(args[0]) : player(sender);
        sender.sendMessage(color("&8[&cKillstreak&8] &f" + target.getName() + ": &c" + states.killstreak(target.getUniqueId())));
    }

    private void kit(CommandSender sender, String[] args) {
        Player player = player(sender);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD), new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 32));
        player.sendMessage(color("&8[&cKit&8] &fStarter kit applied."));
    }

    private void eventCommand(CommandSender sender, String name, String[] args) {
        require(sender, "hcf.admin");
        if (name.equals("systemteam")) {
            sender.sendMessage(color("&8[&cSystem&8] &fSystem teams: &cSafezone, Warzone, Roads, Events"));
            return;
        }
        HCFEventType type = switch (name) {
            case "conquest" -> HCFEventType.CONQUEST;
            case "citadel" -> HCFEventType.CITADEL;
            case "mountain" -> HCFEventType.GLOWSTONE;
            case "ktk" -> HCFEventType.KILL_THE_KING;
            default -> HCFEventType.EOTW;
        };
        var event = events.create(type, Character.toUpperCase(name.charAt(0)) + name.substring(1));
        event.start((args.length >= 1 ? positiveLong(args[0]) : 30L) * 60_000L);
        Bukkit.broadcastMessage(color("&8[&cEvent&8] &f" + event.name() + " has started."));
    }

    private void changelog(CommandSender sender) {
        sender.sendMessage(color("&8&m----------------&8[ &cChangelog &8]&8&m----------------"));
        sender.sendMessage(color("&c• &fAdded HCF tab, help/settings GUIs, anti-clean, claim polish."));
        sender.sendMessage(color("&c• &fImproved scoreboard timers, staff mode, pearls and faction chat."));
        sender.sendMessage(color("&c• &fAdded glowstone mountain reset hooks and class toggles."));
    }

    private void panic(CommandSender sender, String[] args) {
        Player player = player(sender);
        String reason = args.length == 0 ? "No reason provided" : join(args, 0);
        Bukkit.getOnlinePlayers().stream().filter(staffer -> staffer.hasPermission("hcf.staff"))
                .forEach(staffer -> staffer.sendMessage(color("&8[&4Panic&8] &c" + player.getName() + " &fneeds help: &c" + reason)));
        intel.staffNoteHook(player, player.getUniqueId(), player.getName(), "panic", reason);
        player.sendMessage(color("&8[&cPanic&8] &fOnline staff have been alerted."));
    }

    private void link(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (args.length == 0 || !args[0].equalsIgnoreCase("discord")) {
            player.sendMessage(color("&8[&cLink&8] &fUsage: &c/link discord"));
            return;
        }
        boolean particleAvailable = Bukkit.getPluginManager().getPlugin("ParticleCore") != null
                || Bukkit.getPluginManager().getPlugin("Particle-Core") != null
                || Bukkit.getPluginManager().getPlugin("Particle") != null;
        boolean forwarded = executeParticleLink(player, args);
        intel.accountLinkForward(player, "/link discord", particleAvailable);
        if (!forwarded) {
            player.sendMessage(color(particleAvailable
                    ? "&8[&cLink&8] &cParticle Core is loaded, but its link command was not reachable."
                    : "&8[&cLink&8] &cParticle Core is not available on this server."));
        }
    }

    private void social(CommandSender sender, String name) {
        sender.sendMessage(color("&8&m----------------&8[ &cSocials &8]&8&m----------------"));
        sender.sendMessage(color("&cStore&7: &fhttps://store.hcf.local"));
        sender.sendMessage(color("&cDiscord&7: &fhttps://discord.gg/hcf"));
        sender.sendMessage(color("&cWebsite&7: &fhttps://hcf.local"));
        sender.sendMessage(color("&cTeamspeak&7: &fts.hcf.local"));
    }

    private void media(CommandSender sender) {
        sender.sendMessage(color("&8[&cMedia&8] &fApply for media rank in our Discord: &chttps://discord.gg/hcf&f."));
    }

    private void giveaway(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("draw")) {
            require(sender, "hcf.admin");
            giveawayEntries.stream().findAny().ifPresentOrElse(winner -> {
                Player player = Bukkit.getPlayer(winner);
                String name = player == null ? winner.toString().substring(0, 8) : player.getName();
                Bukkit.broadcastMessage(color("&8[&cGiveaway&8] &fWinner selected: &c" + name + "&f."));
                giveawayEntries.clear();
            }, () -> sender.sendMessage(color("&8[&cGiveaway&8] &cNo entries yet.")));
            return;
        }
        Player player = player(sender);
        boolean added = giveawayEntries.add(player.getUniqueId());
        player.sendMessage(color(added ? "&8[&cGiveaway&8] &fYou entered the current giveaway." : "&8[&cGiveaway&8] &cYou are already entered."));
    }

    private void giveNamed(CommandSender sender, Material material, String name, int amount) {
        require(sender, "hcf.admin");
        player(sender).getInventory().addItem(named(material, name, amount));
    }

    private ItemStack named(Material material, String name) {
        return named(material, name, 1);
    }

    private ItemStack named(Material material, String name, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    private boolean executeParticleLink(Player player, String[] args) {
        Command command = particleNamespacedCommand();
        if (command != null) {
            return command.execute(player, command.getName(), args);
        }
        org.bukkit.command.PluginCommand link = Bukkit.getPluginCommand("link");
        if (link != null && link.getPlugin() != plugin) {
            return link.execute(player, "link", args);
        }
        return false;
    }

    private Command particleNamespacedCommand() {
        String[] candidates = {"particlecore:link", "particle-core:link", "particle:link"};
        try {
            Object commandMap = Bukkit.getServer().getClass().getMethod("getCommandMap").invoke(Bukkit.getServer());
            java.lang.reflect.Method getCommand = commandMap.getClass().getMethod("getCommand", String.class);
            for (String candidate : candidates) {
                Object command = getCommand.invoke(commandMap, candidate);
                if (command instanceof Command bukkitCommand) {
                    return bukkitCommand;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private Player player(CommandSender sender) {
        if (!(sender instanceof Player player)) throw new IllegalArgumentException("Players only.");
        return player;
    }

    private Player target(String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) throw new IllegalArgumentException("Player not found.");
        return target;
    }

    private World world(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) throw new IllegalArgumentException("World not found.");
        return world;
    }

    private static Location configuredLocation(Plugin plugin, String path, Location fallback) {
        String worldName = plugin.getConfig().getString(path + ".world", fallback.getWorld().getName());
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = fallback.getWorld();
        }
        return new Location(world,
                plugin.getConfig().getDouble(path + ".x", fallback.getX()),
                plugin.getConfig().getDouble(path + ".y", fallback.getY()),
                plugin.getConfig().getDouble(path + ".z", fallback.getZ()),
                (float) plugin.getConfig().getDouble(path + ".yaw", fallback.getYaw()),
                (float) plugin.getConfig().getDouble(path + ".pitch", fallback.getPitch()));
    }

    private static long positiveLong(String text) {
        long value = Long.parseLong(text);
        if (value <= 0L) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        return value;
    }

    private void require(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) throw new IllegalArgumentException("No permission.");
    }

    private static String joinRequired(String[] args, int start) {
        if (args.length <= start) throw new IllegalArgumentException("Missing arguments.");
        return join(args, start);
    }

    private static String arg(String[] args, int index, String usage) {
        if (args.length <= index) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
        return args[index];
    }

    private static String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private static String shortId(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? uuid.toString().substring(0, 8) : player.getName();
    }

    private static String color(String text) {
        return Text.color(text);
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return hours > 0 ? String.format(Locale.US, "%dh %02dm", hours, minutes) : String.format(Locale.US, "%dm %02ds", minutes, secs);
    }

    private static int pingValue(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (ReflectiveOperationException exception) {
            return -1;
        }
    }
}
