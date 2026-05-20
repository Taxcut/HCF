package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.claim.ClaimSelectionService;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.claim.ClaimType;
import com.testrank.hcf.core.eotw.EotwService;
import com.testrank.hcf.core.koth.KothService;
import com.testrank.hcf.core.partneritems.PartnerItemService;
import com.testrank.hcf.core.sotw.SotwService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.timer.GlobalTimerService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Locale;

public final class HCFCommand implements CommandExecutor {
    private final ClaimService claims;
    private final ClaimSelectionService selections;
    private final TeamService teams;
    private final PartnerItemService partnerItems;
    private final KothService koths;
    private final EotwService eotw;
    private final SotwService sotw;
    private final GlobalTimerService globalTimers;
    private final Threading threading;

    public HCFCommand(ClaimService claims, ClaimSelectionService selections, TeamService teams, PartnerItemService partnerItems,
                      KothService koths, EotwService eotw, SotwService sotw, GlobalTimerService globalTimers, Threading threading) {
        this.claims = claims;
        this.selections = selections;
        this.teams = teams;
        this.partnerItems = partnerItems;
        this.koths = koths;
        this.eotw = eotw;
        this.sotw = sotw;
        this.globalTimers = globalTimers;
        this.threading = threading;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color("&cPlayers only."));
            return true;
        }
        if (command.getName().equalsIgnoreCase("sotw") && args.length == 1 && args[0].equalsIgnoreCase("enable")) {
            if (!sotw.active()) {
                player.sendMessage(Text.color("&8[&cSOTW&8] &cSOTW is not active."));
                return true;
            }
            boolean first = sotw.enablePvp(player.getUniqueId());
            player.sendMessage(Text.color(first ? "&8[&cSOTW&8] &fYou have &aenabled &fPvP." : "&8[&cSOTW&8] &fYour PvP is already &aenabled&f."));
            return true;
        }
        if (!player.hasPermission("hcf.admin")) {
            sender.sendMessage(Text.color("&cNo permission."));
            return true;
        }

        String name = command.getName().toLowerCase(Locale.ROOT);
        try {
            if (name.equals("hcf")) {
                if (args.length == 0) {
                    sendHelp(player);
                    return true;
                }
                return handleLegacySubcommand(player, args);
            }
            return switch (name) {
                case "claimwand" -> claimWand(player);
                case "claim" -> claim(player, args);
                case "claimhere" -> claimHere(player);
                case "partner" -> partner(player, args);
                case "koth" -> koth(player, args);
                case "sotw" -> sotw(player, args);
                case "eotw" -> eotw(player, args);
                case "timer" -> timer(player, args);
                default -> {
                    sendHelp(player);
                    yield true;
                }
            };
        } catch (RuntimeException exception) {
            player.sendMessage(Text.color("&8[&cHCF&8] &c" + exception.getMessage()));
            return true;
        }
    }

    private boolean handleLegacySubcommand(Player player, String[] args) {
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (sub) {
            case "wand", "claimwand" -> claimWand(player);
            case "claim" -> claim(player, rest);
            case "claimhere" -> claimHere(player);
            case "partner" -> partner(player, rest);
            case "koth" -> koth(player, rest);
            case "sotw" -> sotw(player, rest);
            case "eotw" -> eotw(player, rest);
            case "timer" -> timer(player, rest);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean claimHere(Player player) {
        var loc = player.getLocation();
        claims.create(null, "Admin Claim", loc.getWorld().getName(), loc.getBlockX() - 10, loc.getBlockZ() - 10,
                        loc.getBlockX() + 10, loc.getBlockZ() + 10, ClaimType.WARZONE)
                .thenAccept(claim -> threading.runSync(() ->
                        player.sendMessage(Text.color("&8[&cHCF&8] &fCreated claim &c" + claim.name() + "&f."))))
                .exceptionally(throwable -> {
                    threading.runSync(() -> player.sendMessage(Text.color("&8[&cHCF&8] &c" + rootMessage(throwable))));
                    return null;
                });
        return true;
    }

    private boolean claimWand(Player player) {
        player.getInventory().addItem(selections.wand());
        player.sendMessage(Text.color("&8[&cHCF&8] &fClaim wand added. &7Left/right click blocks to select."));
        return true;
    }

    private boolean claim(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/claim <name>"));
            return true;
        }
        var selection = selections.selection(player);
        if (selection.isEmpty()) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cSelect two corners with &f/claimwand &cfirst."));
            return true;
        }
        var first = selection.get().first();
        var second = selection.get().second();
        var owner = teams.byPlayer(player.getUniqueId()).map(team -> team.id()).orElse(null);
        claims.create(owner, args[0], first.getWorld().getName(), first.getBlockX(), first.getBlockZ(), second.getBlockX(), second.getBlockZ(),
                        owner == null ? ClaimType.WARZONE : ClaimType.PLAYER)
                .thenAccept(claim -> threading.runSync(() ->
                        player.sendMessage(Text.color("&8[&cHCF&8] &fCreated claim &c" + claim.name() + "&f."))))
                .exceptionally(throwable -> {
                    threading.runSync(() -> player.sendMessage(Text.color("&8[&cHCF&8] &c" + rootMessage(throwable))));
                    return null;
                });
        return true;
    }

    private boolean partner(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/partner <id> [amount]"));
            return true;
        }
        try {
            int amount = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
            player.getInventory().addItem(partnerItems.create(args[0], amount));
            player.sendMessage(Text.color("&8[&cHCF&8] &fAdded &c" + amount + "x &fpartner item &c" + args[0] + "&f."));
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Text.color("&8[&cHCF&8] &c" + exception.getMessage()));
        }
        return true;
    }

    private boolean koth(Player player, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("start")) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/koth start <claim> [minutes]"));
            return true;
        }
        long minutes = args.length >= 3 ? positiveLong(args[2]) : 30L;
        koths.start(args[1], minutes * 60_000L);
        player.sendMessage(Text.color("&8[&cHCF&8] &fStarted KOTH &c" + args[1] + " &ffor &c" + minutes + "m&f."));
        return true;
    }

    private boolean sotw(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/sotw <minutes>"));
            return true;
        }
        int index = args[0].equalsIgnoreCase("enable") ? 1 : 0;
        if (args.length <= index) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/sotw enable <minutes>"));
            return true;
        }
        long minutes = positiveLong(args[index]);
        sotw.enable(minutes * 60_000L);
        player.sendMessage(Text.color("&8[&cHCF&8] &fSOTW enabled for &c" + minutes + "m&f."));
        return true;
    }

    private boolean eotw(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/eotw <true|false>"));
            return true;
        }
        boolean active = Boolean.parseBoolean(args[0]);
        eotw.active(active);
        player.sendMessage(Text.color(active ? "&8[&4HCF&8] &cEOTW enabled." : "&8[&cHCF&8] &fEOTW disabled."));
        return true;
    }

    private boolean timer(Player player, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("stop")) {
            globalTimers.remove(args[1]);
            player.sendMessage(Text.color("&8[&cHCF&8] &fRemoved timer &c" + args[1].replace('_', ' ') + "&f."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Text.color("&8[&cHCF&8] &cUsage: &f/timer <name> <minutes> &7or &f/timer stop <name>"));
            return true;
        }
        long minutes = positiveLong(args[1]);
        globalTimers.create(args[0], minutes * 60_000L);
        player.sendMessage(Text.color("&8[&cHCF&8] &fCreated timer &c" + args[0].replace('_', ' ') + " &ffor &c" + minutes + "m&f."));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Text.color("&8&m--------------------&8[ &c&lHCF &fAdmin &8]&8&m--------------------"));
        player.sendMessage(Text.color("&c/claimwand &8- &fGet the claim selection wand"));
        player.sendMessage(Text.color("&4/claim <name> &8- &7Create a selected claim"));
        player.sendMessage(Text.color("&c/claimhere &8- &fCreate a 21x21 warzone claim"));
        player.sendMessage(Text.color("&4/partner <id> [amount] &8- &7Give partner items"));
        player.sendMessage(Text.color("&c/koth start <claim> [minutes] &8- &fStart a KOTH"));
        player.sendMessage(Text.color("&4/sotw <minutes> &8- &7Start SOTW timer"));
        player.sendMessage(Text.color("&c/eotw <true|false> &8- &fToggle EOTW"));
        player.sendMessage(Text.color("&4/timer <name> <minutes> &8- &7Create a scoreboard timer"));
        player.sendMessage(Text.color("&8&m--------------------------------------------------"));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private static long positiveLong(String text) {
        long value = Long.parseLong(text);
        if (value <= 0L) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        return value;
    }
}
