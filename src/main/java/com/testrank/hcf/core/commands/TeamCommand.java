package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.chat.ChatChannel;
import com.testrank.hcf.core.chat.ChatService;
import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.claim.ClaimSelectionService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.claim.ClaimType;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.menu.MenuService;
import com.testrank.hcf.core.menu.TeamManageMenu;
import com.testrank.hcf.core.particle.ParticleIntelService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.team.DtrService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamRole;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.util.Position;
import com.testrank.hcf.core.util.Text;
import com.testrank.hcf.core.util.Title;
import com.testrank.hcf.core.waypoint.WaypointService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeamCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "create", "disband", "rename", "roster", "chat", "info", "who", "show", "manage", "invite", "uninvite", "join", "leave",
            "focus", "unfocus", "stuck", "top", "rally", "unrally", "ally", "unally", "leader", "promote", "demote",
            "lockclaim", "claim", "claimwand", "wand", "unclaim", "unclaimall", "sethq", "hq", "kick", "withdraw", "deposit", "list", "map", "base", "falltrap",
            "camp", "coords", "friendlyfire", "setdtr", "setregen", "setleader", "setbalance", "setpoints",
            "setkothcaps", "forceclaim", "bypass", "forcedisband", "forcejoin", "forcekick", "forcepromote", "forcedemote", "teleport"
    );
    private static final List<String> PLAYER_ARGUMENTS = List.of("invite", "uninvite", "focus", "leader", "promote", "demote", "kick",
            "setleader", "forcejoin", "forcekick", "forcepromote", "forcedemote");
    private static final List<String> TEAM_ARGUMENTS = List.of("info", "roster", "ally", "unally", "setdtr", "setregen", "setbalance",
            "setpoints", "setkothcaps", "forcedisband", "forcejoin", "teleport");

    private final TeamService teams;
    private final DtrService dtr;
    private final ClaimService claims;
    private final ClaimSelectionService selections;
    private final Threading threading;
    private final EconomyService economy;
    private final PlayerStateService states;
    private final ChatService chat;
    private final WaypointService waypoints;
    private final HCFSettings settings;
    private final Plugin plugin;
    private final MenuService menus;
    private final ParticleIntelService intel;
    private final Map<UUID, List<Location>> mapViews = new java.util.concurrent.ConcurrentHashMap<>();

    public TeamCommand(TeamService teams, DtrService dtr, ClaimService claims, ClaimSelectionService selections,
                       Threading threading, EconomyService economy, PlayerStateService states, ChatService chat, WaypointService waypoints,
                       HCFSettings settings, Plugin plugin, MenuService menus, ParticleIntelService intel) {
        this.teams = teams;
        this.dtr = dtr;
        this.claims = claims;
        this.selections = selections;
        this.threading = threading;
        this.economy = economy;
        this.states = states;
        this.chat = chat;
        this.waypoints = waypoints;
        this.settings = settings;
        this.plugin = plugin;
        this.menus = menus;
        this.intel = intel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            help(player, label);
            return true;
        }

        try {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "create" -> create(player, args);
                case "disband" -> disband(player);
                case "rename" -> rename(player, args);
                case "roster" -> roster(player, args);
                case "chat", "c" -> teamChat(player, args);
                case "info", "who", "show" -> info(player, args);
                case "manage" -> manage(player);
                case "invite" -> invite(player, args);
                case "uninvite" -> uninvite(player, args);
                case "join", "accept" -> join(player);
                case "leave" -> leave(player);
                case "focus" -> focus(player, args);
                case "unfocus" -> unfocus(player);
                case "stuck" -> stuck(player);
                case "top" -> top(player);
                case "rally" -> rally(player);
                case "unrally" -> unrally(player);
                case "ally" -> ally(player, args, true);
                case "unally" -> ally(player, args, false);
                case "leader" -> leader(player, args);
                case "promote" -> promote(player, args, true, false);
                case "demote" -> promote(player, args, false, false);
                case "lockclaim" -> lockClaim(player);
                case "claimwand", "wand" -> claimWand(player);
                case "claim" -> claim(player, args);
                case "unclaim" -> unclaim(player, args);
                case "unclaimall" -> unclaimAll(player);
                case "sethq" -> setHq(player);
                case "hq" -> hq(player);
                case "kick" -> kick(player, args);
                case "withdraw" -> withdraw(player, args);
                case "deposit" -> deposit(player, args);
                case "list", "sort" -> list(player, args);
                case "map" -> map(player);
                case "base" -> base(player);
                case "falltrap" -> falltrap(player);
                case "camp" -> camp(player);
                case "coords" -> coords(player);
                case "friendlyfire", "ff" -> friendlyFire(player);
                case "setdtr" -> setDtr(player, args);
                case "setregen" -> setRegen(player, args);
                case "setleader" -> setLeader(player, args);
                case "setbalance" -> setBalance(player, args);
                case "setpoints" -> setPoints(player, args);
                case "setkothcaps" -> setKothCaps(player, args);
                case "forceclaim" -> forceClaim(player, args);
                case "bypass" -> bypass(player);
                case "forcedisband" -> forceDisband(player, args);
                case "forcejoin" -> forceJoin(player, args);
                case "forcekick" -> forceKick(player, args);
                case "forcepromote" -> promote(player, args, true, true);
                case "forcedemote" -> promote(player, args, false, true);
                case "teleport" -> teleport(player, args);
                default -> help(player, label);
            }
        } catch (RuntimeException exception) {
            player.sendMessage(color("&8[&cTeam&8] &c" + exception.getMessage()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (PLAYER_ARGUMENTS.contains(sub)) {
                return filter(onlinePlayers(), args[1]);
            }
            if (TEAM_ARGUMENTS.contains(sub)) {
                return filter(teamNames(), args[1]);
            }
            if (sub.equals("chat")) {
                return filter(List.of("public", "team", "ally"), args[1]);
            }
            if (sub.equals("list") || sub.equals("sort")) {
                return filter(List.of("online", "balance", "points", "dtr", "members"), args[1]);
            }
            if (sub.equals("unclaim")) {
                return filter(ownedClaimNames(sender), args[1]);
            }
            if (sub.equals("forceclaim")) {
                return filter(java.util.Arrays.stream(ClaimType.values()).map(type -> type.name().toLowerCase(Locale.ROOT)).toList(), args[1]);
            }
        }
        if (args.length == 3 && (sub.equals("setleader") || sub.equals("forcejoin"))) {
            return filter(onlinePlayers(), args[2]);
        }
        if (args.length == 4 && sub.equals("forcejoin")) {
            return filter(List.of("member", "captain", "co_leader", "leader"), args[3]);
        }
        if (args.length == 3 && sub.equals("setregen")) {
            return filter(List.of("enabled", "disabled"), args[2]);
        }
        return java.util.Collections.emptyList();
    }

    private void create(Player player, String[] args) {
        String name = arg(args, 1, "/team create <name>");
        teams.create(name, player.getUniqueId()).thenAccept(team ->
                threading.runSync(() -> {
                    intel.factionPunishmentLink(player, team, "created", Map.of("leader", player.getUniqueId().toString()));
                    intel.factionHistory(player.getUniqueId(), player.getName(), team, "created", player.getUniqueId());
                    player.sendMessage(color("&8[&cTeam&8] &fCreated faction &c" + name + "&f."));
                    Bukkit.broadcastMessage(color("&eFaction &c" + name + " &ehas been &acreated by &f" + player.getName()));
                    Title.send(player, "&c&lFaction Created", "&fYou are now leading &c" + name + "&f.");
                })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void disband(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.LEADER);
        teams.disband(team).thenRun(() -> threading.runSync(() -> {
            intel.factionPunishmentLink(player, team, "disbanded", Map.of("actor", player.getUniqueId().toString()));
            team.members().keySet().forEach(member -> intel.factionHistory(member, memberName(member), team, "disbanded", player.getUniqueId()));
            player.sendMessage(color("&8[&cTeam&8] &fFaction &c" + team.name() + " &fwas disbanded."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void rename(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.LEADER);
        String name = arg(args, 1, "/team rename <name>");
        complete(player, teams.rename(team, name), "&fFaction renamed to &c" + name + "&f.");
    }

    private void roster(Player player, String[] args) {
        Team team = args.length >= 2 ? team(args[1]) : ownTeam(player);
        player.sendMessage(color("&8&m--------------------&8[ &c" + team.name() + " &fRoster &8]&8&m--------------------"));
        team.members().entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<UUID, TeamRole> entry) -> entry.getValue().ordinal()).reversed())
                .forEach(entry -> player.sendMessage(color("&c" + memberName(entry.getKey()) + " &8- &f" + pretty(entry.getValue().name()))));
        player.sendMessage(color("&8&m--------------------------------------------------"));
    }

    private void teamChat(Player player, String[] args) {
        if (args.length == 1) {
            chat.channel(player, ChatChannel.TEAM);
            player.sendMessage(color("&8[&cTeam&8] &fChat channel set to &cTeam&f."));
            return;
        }
        chat.teamChat(player, join(args, 1));
    }

    private void info(Player player, String[] args) {
        Team team = args.length >= 2 ? team(args[1]) : ownTeam(player);
        String manage = team.isMember(player.getUniqueId()) && teams.canManage(player.getUniqueId(), team, TeamRole.CAPTAIN) ? " &7- &e[Manage]" : "";
        player.sendMessage(color("&7&m--------------------------------------------------"));
        player.sendMessage(color("&c" + team.name() + " &7[&f" + onlineCount(team) + "&7/&f" + team.members().size() + "&7] &7- &cHome&7: &f" + compactLocation(team.hq()) + manage));
        player.sendMessage(color("&cLeader&7: &f" + leaderName(team) + "&7[&f0&7]"));
        List<String> captains = roleMembers(team, TeamRole.CAPTAIN, TeamRole.CO_LEADER);
        if (!captains.isEmpty()) {
            player.sendMessage(color("&cCaptains&7: &f" + String.join("&7, &f", captains)));
        }
        List<String> members = roleMembers(team, TeamRole.MEMBER);
        if (!members.isEmpty()) {
            player.sendMessage(color("&cMembers&7: &f" + String.join("&7, &f", members)));
        }
        player.sendMessage(color("&cBalance&7: &a$" + (long) team.balance()));
        player.sendMessage(color("&cDeaths Until Raidable&7: &a" + String.format(Locale.US, "%.2f", Math.max(0.0D, team.dtr())) + (dtr.raidability(team) ? " &4RAIDABLE" : "")));
        player.sendMessage(color("&7&m--------------------------------------------------"));
    }

    private void invite(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        Player target = target(arg(args, 1, "/team invite <player>"));
        teams.invite(team, target.getUniqueId(), player.getUniqueId());
        target.sendMessage(color("&8[&cTeam&8] &f" + player.getName() + " invited you to &c" + team.name() + "&f. Use &c/team join&f."));
        player.sendMessage(color("&8[&cTeam&8] &fInvite sent to &c" + target.getName() + "&f."));
    }

    private void manage(Player player) {
        Team team = ownTeam(player);
        new TeamManageMenu(menus, teams, dtr, settings, team).open(player, menus);
    }

    private void uninvite(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        Player target = target(arg(args, 1, "/team uninvite <player>"));
        boolean revoked = teams.revokeInvite(team, target.getUniqueId());
        player.sendMessage(color(revoked ? "&8[&cTeam&8] &fRevoked invite for &c" + target.getName() + "&f." : "&8[&cTeam&8] &cThat player does not have an invite."));
    }

    private void join(Player player) {
        teams.acceptInvite(player.getUniqueId()).thenAccept(team -> threading.runSync(() -> {
            intel.factionHistory(player.getUniqueId(), player.getName(), team, "joined", player.getUniqueId());
            intel.factionPunishmentLink(player, team, "joined", Map.of("member", player.getUniqueId().toString()));
            player.sendMessage(color("&8[&cTeam&8] &fJoined your new faction."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void leave(Player player) {
        Team team = ownTeam(player);
        teams.leave(player.getUniqueId()).thenRun(() -> threading.runSync(() -> {
            intel.factionHistory(player.getUniqueId(), player.getName(), team, "left", player.getUniqueId());
            player.sendMessage(color("&8[&cTeam&8] &fYou left your faction."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void focus(Player player, String[] args) {
        Team team = ownTeam(player);
        String name = arg(args, 1, "/team focus <player|team>");
        Player target = Bukkit.getPlayerExact(name);
        UUID focused = target == null ? teams.byName(name)
                .flatMap(faction -> faction.members().entrySet().stream().filter(entry -> entry.getValue() == TeamRole.LEADER).map(Map.Entry::getKey).findFirst())
                .orElseThrow(() -> new IllegalArgumentException("Player or faction not found.")) : target.getUniqueId();
        team.focused(focused);
        teams.save(team);
        announce(team, "&8[&cTeam&8] &f" + player.getName() + " focused &c" + name + "&f.");
    }

    private void unfocus(Player player) {
        Team team = ownTeam(player);
        team.focused(null);
        teams.save(team);
        announce(team, "&8[&cTeam&8] &fFaction focus cleared.");
    }

    private void stuck(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage(color("&8[&cTeam&8] &fYou were moved to this world's spawn."));
    }

    private void top(Player player) {
        player.sendMessage(color("&8&m--------------------&8[ &cFaction Top &8]&8&m--------------------"));
        teams.teams().stream()
                .sorted(Comparator.<Team>comparingInt(Team::points).thenComparingDouble(Team::balance).reversed())
                .limit(10)
                .forEach(team -> player.sendMessage(color("&c" + team.name() + " &8- &f" + team.points() + " pts &8| &a$" + (long) team.balance() + " &8| &f" + team.members().size() + " members")));
        player.sendMessage(color("&8&m--------------------------------------------------"));
    }

    private void rally(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        team.rally(player.getLocation());
        teams.save(team);
        refreshWaypoints(team);
        announce(team, "&8[&cTeam&8] &fRally set at &c" + shortLocation(player.getLocation()) + "&f.");
    }

    private void unrally(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        team.rally((Position) null);
        teams.save(team);
        refreshWaypoints(team);
        announce(team, "&8[&cTeam&8] &fRally cleared.");
    }

    private void ally(Player player, String[] args, boolean add) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.LEADER);
        Team other = team(arg(args, 1, add ? "/team ally <team>" : "/team unally <team>"));
        if (team.id().equals(other.id())) {
            throw new IllegalArgumentException("You cannot ally your own faction.");
        }
        if (add) {
            team.ally(other.id());
            other.ally(team.id());
        } else {
            team.unally(other.id());
            other.unally(team.id());
        }
        teams.save(team);
        teams.save(other);
        player.sendMessage(color("&8[&cTeam&8] &fAlly relation with &c" + other.name() + " &fis now " + (add ? "&aenabled" : "&cdisabled") + "&f."));
    }

    private void leader(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.LEADER);
        Player target = target(arg(args, 1, "/team leader <player>"));
        if (!team.isMember(target.getUniqueId())) {
            throw new IllegalArgumentException("That player is not in your faction.");
        }
        team.members().entrySet().stream()
                .filter(entry -> entry.getValue() == TeamRole.LEADER)
                .findFirst()
                .ifPresent(entry -> team.member(entry.getKey(), TeamRole.CO_LEADER));
        team.member(target.getUniqueId(), TeamRole.LEADER);
        complete(player, teams.save(team), "&fLeadership transferred to &c" + target.getName() + "&f.");
    }

    private void promote(Player player, String[] args, boolean promote, boolean forced) {
        if (forced) {
            requireAdmin(player);
        }
        Player target = target(arg(args, 1, forced ? "/team forcepromote <player>" : "/team promote <player>"));
        Team team = forced ? teams.byPlayer(target.getUniqueId()).orElseThrow(() -> new IllegalArgumentException("Target is not in a faction.")) : ownTeam(player);
        if (!forced) {
            requireRole(player, team, TeamRole.CO_LEADER);
            if (!team.isMember(target.getUniqueId())) {
                throw new IllegalArgumentException("That player is not in your faction.");
            }
        }
        TeamRole current = team.members().get(target.getUniqueId());
        if (current == TeamRole.LEADER && !forced) {
            throw new IllegalArgumentException("Use /team leader to transfer leadership.");
        }
        int nextOrdinal = current.ordinal() + (promote ? 1 : -1);
        int cap = forced ? TeamRole.LEADER.ordinal() : TeamRole.CO_LEADER.ordinal();
        TeamRole next = TeamRole.values()[Math.max(TeamRole.MEMBER.ordinal(), Math.min(cap, nextOrdinal))];
        teams.setRole(team, target.getUniqueId(), next).thenRun(() -> threading.runSync(() -> {
            intel.factionPunishmentLink(player, team, forced ? "force_role_changed" : "role_changed", Map.of(
                    "target", target.getUniqueId().toString(),
                    "role", next.name()
            ));
            player.sendMessage(color("&8[&cTeam&8] &fSet &c" + target.getName() + " &fto &c" + pretty(next.name()) + "&f."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void lockClaim(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        team.claimLocked(!team.claimLocked());
        teams.save(team);
        announce(team, "&8[&cTeam&8] &fClaim lock is now " + (team.claimLocked() ? "&aenabled" : "&cdisabled") + "&f.");
    }

    private void claimWand(Player player) {
        player.getInventory().addItem(selections.wand());
        player.sendMessage(color("&8[&cClaim&8] &fClaim wand added. &7Left/right click corners, then &csneak-left-click &7to buy."));
    }

    private void claim(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, team.claimLocked() ? TeamRole.LEADER : TeamRole.CAPTAIN);
        claimWand(player);
    }

    private void unclaim(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        Claim claim = args.length >= 2 ? claims.byName(args[1]).orElseThrow(() -> new IllegalArgumentException("Claim not found."))
                : claims.at(player.getLocation()).orElseThrow(() -> new IllegalArgumentException("Stand inside a faction claim or use /f unclaim <claim>."));
        if (!team.id().equals(claim.owner())) {
            throw new IllegalArgumentException("That claim is not owned by your faction.");
        }
        claims.delete(claim.id()).thenAccept(deleted -> threading.runSync(() ->
                player.sendMessage(color("&8[&cClaim&8] &fUnclaimed &c" + deleted.name() + "&f."))))
                .exceptionally(throwable -> {
                    threading.runSync(() -> player.sendMessage(color("&8[&cClaim&8] &c" + rootMessage(throwable))));
                    return null;
                });
    }

    private void unclaimAll(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.LEADER);
        claims.deleteAllOwned(team.id()).thenAccept(count -> threading.runSync(() ->
                player.sendMessage(color("&8[&cClaim&8] &fRemoved &c" + count + " &ffaction claim" + (count == 1 ? "" : "s") + "&f."))))
                .exceptionally(throwable -> {
                    threading.runSync(() -> player.sendMessage(color("&8[&cClaim&8] &c" + rootMessage(throwable))));
                    return null;
                });
    }

    private void setHq(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        complete(player, teams.setHq(team, player.getLocation()).thenRun(() -> refreshWaypoints(team)), "&fFaction HQ set at &c" + shortLocation(player.getLocation()) + "&f.");
    }

    private void hq(Player player) {
        Team team = ownTeam(player);
        Position hq = team.hq();
        if (hq == null) {
            throw new IllegalArgumentException("Your faction does not have an HQ.");
        }
        hq.toLocation().ifPresentOrElse(player::teleport, () -> player.sendMessage(color("&8[&cTeam&8] &cHQ world is not loaded.")));
    }

    private void kick(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        Player target = target(arg(args, 1, "/team kick <player>"));
        if (!team.isMember(target.getUniqueId())) {
            throw new IllegalArgumentException("That player is not in your faction.");
        }
        if (team.members().get(target.getUniqueId()).ordinal() >= team.members().get(player.getUniqueId()).ordinal()) {
            throw new IllegalArgumentException("You cannot kick that role.");
        }
        teams.kick(team, target.getUniqueId()).thenRun(() -> threading.runSync(() -> {
            intel.factionHistory(target.getUniqueId(), target.getName(), team, "kicked", player.getUniqueId());
            intel.factionPunishmentLink(player, team, "member_kicked", Map.of("target", target.getUniqueId().toString()));
            player.sendMessage(color("&8[&cTeam&8] &fKicked &c" + target.getName() + " &ffrom the faction."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void withdraw(Player player, String[] args) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        long amount = positiveLong(arg(args, 1, "/team withdraw <amount>"));
        if (team.balance() < amount) {
            throw new IllegalArgumentException("Your faction does not have that much money.");
        }
        team.balance(team.balance() - amount);
        economy.add(player.getUniqueId(), amount);
        complete(player, teams.save(team), "&fWithdrew &a$" + amount + " &ffrom faction balance.");
    }

    private void deposit(Player player, String[] args) {
        Team team = ownTeam(player);
        long amount = positiveLong(arg(args, 1, "/team deposit <amount>"));
        economy.withdraw(player.getUniqueId(), amount).thenAccept(success -> {
            if (!success) {
                threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &cYou do not have enough money.")));
                return;
            }
            team.balance(team.balance() + amount);
            teams.save(team).thenRun(() -> threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &fDeposited &a$" + amount + " &finto faction balance."))));
        });
    }

    private void list(Player player, String[] args) {
        String sort = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "online";
        Comparator<Team> comparator = switch (sort) {
            case "balance" -> Comparator.comparingDouble(Team::balance);
            case "points" -> Comparator.comparingInt(Team::points);
            case "dtr" -> Comparator.comparingDouble(Team::dtr);
            case "members" -> Comparator.comparingInt(team -> team.members().size());
            default -> Comparator.comparingInt(this::onlineCount);
        };
        player.sendMessage(color("&8&m--------------------&8[ &cFactions &8]&8&m--------------------"));
        teams.teams().stream().sorted(comparator.reversed()).limit(10)
                .forEach(team -> player.sendMessage(color("&c" + team.name() + " &8- &f" + onlineCount(team) + "/" + team.members().size() + " online &8| &fDTR " + format(team.dtr()))));
        player.sendMessage(color("&8&m--------------------------------------------------"));
    }

    private void map(Player player) {
        List<Location> active = mapViews.remove(player.getUniqueId());
        if (active != null) {
            clearCorners(player, active);
            player.sendMessage(color("&8[&cClaim Map&8] &fClaim map &cdisabled&f."));
            return;
        }
        player.sendMessage(color("&8&m--------------------&8[ &cClaim Map &8]&8&m--------------------"));
        java.util.List<Claim> visible = claims.claims().stream()
                .filter(claim -> claim.world().equals(player.getWorld().getName()))
                .sorted(Comparator.comparingInt(claim -> distanceSquared(player.getLocation(), claim)))
                .limit(8)
                .toList();
        List<Location> shown = new ArrayList<>();
        visible.forEach(claim -> {
            player.sendMessage(color("&c" + claim.name() + " &8- &f" + claim.minX() + "," + claim.minZ() + " &7to &f" + claim.maxX() + "," + claim.maxZ()));
            shown.addAll(showCorners(player, claim));
        });
        if (!shown.isEmpty()) {
            mapViews.put(player.getUniqueId(), shown);
            player.sendMessage(color("&8[&cClaim Map&8] &7Use &c/f map &7again to hide the corner towers."));
        }
        player.sendMessage(color("&8&m--------------------------------------------------"));
    }

    private void base(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        if (!states.consumeBaseToken(player.getUniqueId())) {
            throw new IllegalArgumentException("You do not have a base token.");
        }
        complete(player, teams.setHq(team, player.getLocation()).thenRun(() -> refreshWaypoints(team)), "&fBase token used. HQ moved to &c" + shortLocation(player.getLocation()) + "&f.");
    }

    private void falltrap(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        if (!states.consumeFalltrapToken(player.getUniqueId())) {
            throw new IllegalArgumentException("You do not have a falltrap token.");
        }
        team.rally(player.getLocation());
        complete(player, teams.save(team), "&fFalltrap token used. Rally moved to &c" + shortLocation(player.getLocation()) + "&f.");
    }

    private void camp(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        team.rally(player.getLocation());
        complete(player, teams.save(team).thenRun(() -> refreshWaypoints(team)), "&fFaction camp set at &c" + shortLocation(player.getLocation()) + "&f.");
    }

    private void coords(Player player) {
        Team team = ownTeam(player);
        player.sendMessage(color("&8&m--------------------&8[ &cFaction Coords &8]&8&m--------------------"));
        player.sendMessage(color("&cHQ&7: &f" + locationText(team.hq())));
        player.sendMessage(color("&cRally/Camp&7: &f" + locationText(team.rally())));
        claims.claims().stream().filter(claim -> claim.owner().equals(team.id())).findFirst()
                .ifPresentOrElse(claim -> player.sendMessage(color("&cClaim&7: &f" + claim.minX() + "," + claim.minZ() + " &7to &f" + claim.maxX() + "," + claim.maxZ())),
                        () -> player.sendMessage(color("&cClaim&7: &fNone")));
        player.sendMessage(color("&8&m--------------------------------------------------"));
    }

    private void friendlyFire(Player player) {
        Team team = ownTeam(player);
        requireRole(player, team, TeamRole.CAPTAIN);
        team.friendlyFire(!team.friendlyFire());
        complete(player, teams.save(team), "&fFriendly fire is now " + (team.friendlyFire() ? "&aenabled" : "&cdisabled") + "&f.");
    }

    private void setDtr(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team setdtr <team> <dtr>"));
        double before = team.dtr();
        team.dtr(Double.parseDouble(arg(args, 2, "/team setdtr <team> <dtr>")));
        intel.dtrImpact(player, team, before, team.dtr(), "admin_setdtr", 0L);
        complete(player, teams.save(team), "&fSet &c" + team.name() + " &fDTR to &c" + format(team.dtr()) + "&f.");
    }

    private void setRegen(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team setregen <team> <enabled|disabled>"));
        boolean enabled = parseToggle(arg(args, 2, "/team setregen <team> <enabled|disabled>"));
        team.regenPaused(!enabled);
        complete(player, teams.save(team), "&fDTR regen for &c" + team.name() + " &fis now " + (enabled ? "&aenabled" : "&cdisabled") + "&f.");
    }

    private void setLeader(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team setleader <team> <player>"));
        Player target = target(arg(args, 2, "/team setleader <team> <player>"));
        team.members().entrySet().stream()
                .filter(entry -> entry.getValue() == TeamRole.LEADER)
                .findFirst()
                .ifPresent(entry -> team.member(entry.getKey(), TeamRole.CO_LEADER));
        team.member(target.getUniqueId(), TeamRole.LEADER);
        complete(player, teams.forceJoin(team, target.getUniqueId(), TeamRole.LEADER), "&fSet &c" + target.getName() + " &fas leader of &c" + team.name() + "&f.");
    }

    private void setBalance(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team setbalance <team> <amount>"));
        team.balance(positiveLong(arg(args, 2, "/team setbalance <team> <amount>")));
        complete(player, teams.save(team), "&fSet &c" + team.name() + " &fbalance to &a$" + (long) team.balance() + "&f.");
    }

    private void setPoints(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team setpoints <team> <points>"));
        team.points((int) positiveLong(arg(args, 2, "/team setpoints <team> <points>")));
        complete(player, teams.save(team), "&fSet &c" + team.name() + " &fpoints to &c" + team.points() + "&f.");
    }

    private void setKothCaps(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team setkothcaps <team> <caps>"));
        team.kothCaps((int) positiveLong(arg(args, 2, "/team setkothcaps <team> <caps>")));
        complete(player, teams.save(team), "&fSet &c" + team.name() + " &fKOTH caps to &c" + team.kothCaps() + "&f.");
    }

    private void forceClaim(Player player, String[] args) {
        requireAdmin(player);
        ClaimType type = args.length >= 2 ? ClaimType.valueOf(args[1].toUpperCase(Locale.ROOT)) : ClaimType.WARZONE;
        String name = args.length >= 3 ? args[2] : uniqueClaimName(type.name().charAt(0) + type.name().substring(1).toLowerCase(Locale.ROOT));
        var selection = selections.selection(player).orElseThrow(() -> new IllegalArgumentException("Select two corners with /claimwand first."));
        claims.create(null, name, selection.first().getWorld().getName(),
                        selection.first().getBlockX(), selection.first().getBlockZ(),
                        selection.second().getBlockX(), selection.second().getBlockZ(), type)
                .thenAccept(claim -> threading.runSync(() -> {
                    selections.clear(player);
                    player.sendMessage(color("&8[&cClaim&8] &fForce-created &c" + claim.name() + " &fas &c" + claim.type().name() + "&f."));
                }))
                .exceptionally(throwable -> {
                    threading.runSync(() -> player.sendMessage(color("&8[&cClaim&8] &c" + rootMessage(throwable))));
                    return null;
                });
    }

    private void bypass(Player player) {
        requireAdmin(player);
        boolean enabled = states.toggleStaffBuild(player.getUniqueId());
        player.sendMessage(color("&8[&cTeam&8] &fClaim bypass/staff build: " + (enabled ? "&aEnabled" : "&cDisabled")));
    }

    private void forceDisband(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team forcedisband <team>"));
        teams.disband(team).thenRun(() -> threading.runSync(() -> {
            intel.factionPunishmentLink(player, team, "force_disbanded", Map.of("actor", player.getUniqueId().toString()));
            team.members().keySet().forEach(member -> intel.factionHistory(member, memberName(member), team, "force_disbanded", player.getUniqueId()));
            player.sendMessage(color("&8[&cTeam&8] &fForce disbanded &c" + team.name() + "&f."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void forceJoin(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team forcejoin <team> <player> [role]"));
        Player target = target(arg(args, 2, "/team forcejoin <team> <player> [role]"));
        TeamRole role = args.length >= 4 ? role(args[3]) : TeamRole.MEMBER;
        teams.forceJoin(team, target.getUniqueId(), role).thenRun(() -> threading.runSync(() -> {
            intel.factionHistory(target.getUniqueId(), target.getName(), team, "force_joined", player.getUniqueId());
            intel.factionPunishmentLink(player, team, "force_joined", Map.of(
                    "target", target.getUniqueId().toString(),
                    "role", role.name()
            ));
            player.sendMessage(color("&8[&cTeam&8] &fForce joined &c" + target.getName() + " &fto &c" + team.name() + "&f."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void forceKick(Player player, String[] args) {
        requireAdmin(player);
        Player target = target(arg(args, 1, "/team forcekick <player>"));
        Team team = teams.byPlayer(target.getUniqueId()).orElseThrow(() -> new IllegalArgumentException("Target is not in a faction."));
        teams.kick(team, target.getUniqueId()).thenRun(() -> threading.runSync(() -> {
            intel.factionHistory(target.getUniqueId(), target.getName(), team, "force_kicked", player.getUniqueId());
            intel.factionPunishmentLink(player, team, "force_kicked", Map.of("target", target.getUniqueId().toString()));
            player.sendMessage(color("&8[&cTeam&8] &fForce kicked &c" + target.getName() + " &ffrom &c" + team.name() + "&f."));
        })).exceptionally(throwable -> {
            threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
            return null;
        });
    }

    private void teleport(Player player, String[] args) {
        requireAdmin(player);
        Team team = team(arg(args, 1, "/team teleport <team>"));
        Position hq = team.hq();
        if (hq == null) {
            throw new IllegalArgumentException("That faction does not have an HQ.");
        }
        hq.toLocation().ifPresentOrElse(player::teleport, () -> player.sendMessage(color("&8[&cTeam&8] &cHQ world is not loaded.")));
    }

    private void help(Player player, String label) {
        player.sendMessage(color("&8&m--------------------&8[ &c&lFaction &fCommands &8]&8&m--------------------"));
        player.sendMessage(color("&c/" + label + " create <name> &8- &fCreate a faction"));
        player.sendMessage(color("&4/" + label + " invite <player> &8- &7Invite a member"));
        player.sendMessage(color("&c/" + label + " claim &8- &fClaim land after using the wand"));
        player.sendMessage(color("&4/" + label + " unclaim/unclaimall &8- &7Remove faction claims"));
        player.sendMessage(color("&4/" + label + " sethq &8- &7Set HQ, &c/" + label + " hq &7to teleport"));
        player.sendMessage(color("&c/" + label + " focus <player> &8- &fMark a target"));
        player.sendMessage(color("&4/" + label + " rally &8- &7Set rally, &c/" + label + " unrally &7to clear"));
        player.sendMessage(color("&c/" + label + " deposit/withdraw <amount> &8- &fManage balance"));
        player.sendMessage(color("&4/" + label + " info [team] &8- &7View faction details"));
        player.sendMessage(color("&c/" + label + " top/list/map &8- &fFaction rankings and claims"));
        player.sendMessage(color("&8&m--------------------------------------------------"));
    }

    private Team ownTeam(Player player) {
        return teams.byPlayer(player.getUniqueId()).orElseThrow(() -> new IllegalArgumentException("You are not in a faction."));
    }

    private Team team(String name) {
        return teams.byName(name).orElseThrow(() -> new IllegalArgumentException("Faction not found."));
    }

    private Player target(String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            throw new IllegalArgumentException("Player not found.");
        }
        return target;
    }

    private void requireRole(Player player, Team team, TeamRole minimum) {
        if (!teams.canManage(player.getUniqueId(), team, minimum)) {
            throw new IllegalArgumentException("You must be " + pretty(minimum.name()) + " or above.");
        }
    }

    private void requireAdmin(Player player) {
        if (!player.hasPermission("hcf.admin")) {
            throw new IllegalArgumentException("No permission.");
        }
    }

    private void complete(Player player, CompletableFuture<?> future, String success) {
        future.thenRun(() -> threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] " + success))))
                .exceptionally(throwable -> {
                    threading.runSync(() -> player.sendMessage(color("&8[&cTeam&8] &c" + rootMessage(throwable))));
                    return null;
                });
    }

    private void announce(Team team, String message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> team.isMember(player.getUniqueId()))
                .forEach(player -> player.sendMessage(color(message)));
    }

    private void refreshWaypoints(Team team) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> team.isMember(player.getUniqueId()))
                .forEach(waypoints::refresh);
    }

    private List<Location> showCorners(Player player, Claim claim) {
        int[][] corners = {
                {claim.minX(), claim.minZ()},
                {claim.minX(), claim.maxZ()},
                {claim.maxX(), claim.minZ()},
                {claim.maxX(), claim.maxZ()}
        };
        List<Location> sent = new ArrayList<>(256);
        for (int[] corner : corners) {
            for (int y = 1; y <= 255; y += 4) {
                Location location = new Location(player.getWorld(), corner[0], y, corner[1]);
                player.sendBlockChange(location, Material.STAINED_GLASS, (byte) 14);
                sent.add(location);
            }
        }
        return sent;
    }

    private void clearCorners(Player player, List<Location> locations) {
        for (Location location : locations) {
            if (!location.getWorld().equals(player.getWorld())) {
                continue;
            }
            player.sendBlockChange(location, location.getBlock().getType(), location.getBlock().getData());
        }
    }

    private int area(Location first, Location second) {
        return (Math.abs(first.getBlockX() - second.getBlockX()) + 1) * (Math.abs(first.getBlockZ() - second.getBlockZ()) + 1);
    }

    private long claimPrice(int area) {
        return Math.max(250L, Math.round(area * settings.claimPricePerBlock()));
    }

    private int onlineCount(Team team) {
        int online = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.isMember(player.getUniqueId())) {
                online++;
            }
        }
        return online;
    }

    private static int distanceSquared(Location location, Claim claim) {
        int centerX = (claim.minX() + claim.maxX()) / 2;
        int centerZ = (claim.minZ() + claim.maxZ()) / 2;
        int dx = location.getBlockX() - centerX;
        int dz = location.getBlockZ() - centerZ;
        return dx * dx + dz * dz;
    }

    private static TeamRole role(String text) {
        try {
            return TeamRole.valueOf(text.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unknown role.");
        }
    }

    private static boolean parseToggle(String text) {
        return text.equalsIgnoreCase("on") || text.equalsIgnoreCase("enable") || text.equalsIgnoreCase("enabled") || text.equalsIgnoreCase("true");
    }

    private static long positiveLong(String text) {
        long value = Long.parseLong(text);
        if (value <= 0L) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        return value;
    }

    private static String leaderName(Team team) {
        return team.members().entrySet().stream()
                .filter(entry -> entry.getValue() == TeamRole.LEADER)
                .map(entry -> memberName(entry.getKey()))
                .findFirst()
                .orElse("Unknown");
    }

    private static String memberName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
    }

    private static String locationText(Position position) {
        if (position == null) {
            return "None";
        }
        return position.world() + " " + (int) position.x() + ", " + (int) position.y() + ", " + (int) position.z();
    }

    private static String compactLocation(Position position) {
        if (position == null) {
            return "None";
        }
        return (int) position.x() + ", " + (int) position.y() + ", " + (int) position.z();
    }

    private static List<String> roleMembers(Team team, TeamRole... roles) {
        java.util.Set<TeamRole> accepted = java.util.EnumSet.noneOf(TeamRole.class);
        java.util.Collections.addAll(accepted, roles);
        return team.members().entrySet().stream()
                .filter(entry -> accepted.contains(entry.getValue()))
                .map(entry -> memberName(entry.getKey()) + "&7[&f0&7]")
                .toList();
    }

    private static String shortLocation(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private static String arg(String[] args, int index, String usage) {
        if (args.length <= index) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
        return args[index];
    }

    private static String join(String[] args, int start) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }

    private static String pretty(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private List<String> teamNames() {
        return teams.teams().stream().map(Team::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private String uniqueClaimName(String base) {
        if (claims.byName(base).isEmpty()) {
            return base;
        }
        for (int i = 2; i < 100; i++) {
            String candidate = base + "-" + i;
            if (claims.byName(candidate).isEmpty()) {
                return candidate;
            }
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    private List<String> ownedClaimNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        return teams.byPlayer(player.getUniqueId())
                .map(team -> claims.byOwner(team.id()).stream().map(Claim::name).sorted(String.CASE_INSENSITIVE_ORDER).toList())
                .orElseGet(List::of);
    }

    private static List<String> onlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static String color(String text) {
        return Text.color(text);
    }
}
