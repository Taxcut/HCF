package com.testrank.hcf.core.menu;

import com.testrank.hcf.core.team.DtrService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamRole;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TeamManageMenu extends Menu {
    private final MenuService menus;
    private final TeamService teams;
    private final DtrService dtr;
    private final Team team;
    private final Page page;

    public TeamManageMenu(MenuService menus, TeamService teams, DtrService dtr, Team team) {
        this(menus, teams, dtr, team, Page.HOME);
    }

    private TeamManageMenu(MenuService menus, TeamService teams, DtrService dtr, Team team, Page page) {
        this.menus = menus;
        this.teams = teams;
        this.dtr = dtr;
        this.team = team;
        this.page = page;
    }

    @Override
    public String title(Player player) {
        return Text.color(page == Page.HOME ? "&cTeam Manage" : "&c" + page.display);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public Map<Integer, Button> buttons(Player player) {
        Map<Integer, Button> buttons = base(size());
        if (page != Page.HOME) {
            detail(player, buttons);
            buttons.put(40, navigation(Page.HOME, Material.ARROW, "&cBack", "&7Return to team management."));
            return buttons;
        }

        buttons.put(10, navigation(Page.MEMBERS, Material.SKULL_ITEM, "&cMembers", "&7Manage and view your roster."));
        buttons.put(11, navigation(Page.STATISTICS, Material.PAPER, "&cStatistics", "&7View DTR, KOTH caps and activity."));
        buttons.put(12, navigation(Page.MISSIONS, Material.MAP, "&cMissions", "&7Track faction progression."));
        buttons.put(13, navigation(Page.CONTRACTS, Material.BOOK, "&cContracts", "&7View active faction contracts."));
        buttons.put(14, navigation(Page.POINTS, Material.NETHER_STAR, "&cPoints", "&7View faction points and ranking."));
        buttons.put(15, navigation(Page.GEM_SHOP, Material.EMERALD, "&cGem Shop", "&7Spend faction gems."));
        buttons.put(16, navigation(Page.SETTINGS, Material.REDSTONE_COMPARATOR, "&cTeam Settings", "&7Toggle team permissions."));
        buttons.put(22, navigation(Page.INFORMATION, Material.SIGN, "&cTeam Information", "&7View public faction information."));
        buttons.put(31, staticButton(item(Material.DIAMOND_BLOCK, "&c" + team.name(),
                "&7Leader&7: &f" + leaderName(team),
                "&7Members&7: &f" + online(team) + "/" + team.members().size(),
                "&7DTR&7: &a" + String.format(Locale.US, "%.2f", team.dtr()),
                "&7Balance&7: &a$" + (long) team.balance(),
                "",
                "&8Premium HCF faction control.")));
        return buttons;
    }

    private void detail(Player player, Map<Integer, Button> buttons) {
        switch (page) {
            case MEMBERS -> {
                int slot = 10;
                for (Map.Entry<UUID, TeamRole> entry : team.members().entrySet().stream()
                        .sorted(Comparator.comparing((Map.Entry<UUID, TeamRole> entry) -> entry.getValue().ordinal()).reversed())
                        .toList()) {
                    buttons.put(slot++, staticButton(item(Material.SKULL_ITEM, "&c" + memberName(entry.getKey()),
                            "&7Role&7: &f" + pretty(entry.getValue().name()),
                            "&7Status&7: " + (Bukkit.getPlayer(entry.getKey()) == null ? "&7Offline" : "&aOnline"),
                            "",
                            "&8Promotion and kick actions stay on /f commands.")));
                    if (slot == 17) {
                        slot = 19;
                    }
                }
            }
            case STATISTICS -> {
                buttons.put(11, staticButton(item(Material.PAPER, "&cDTR", "&7Current&7: &a" + String.format(Locale.US, "%.2f", team.dtr()), "&7Raidable&7: " + (dtr.raidability(team) ? "&cYes" : "&aNo"))));
                buttons.put(13, staticButton(item(Material.GOLD_INGOT, "&cEconomy", "&7Balance&7: &a$" + (long) team.balance(), "&7Points&7: &f" + team.points())));
                buttons.put(15, staticButton(item(Material.NETHER_STAR, "&cEvents", "&7KOTH Caps&7: &f" + team.kothCaps(), "&7Allies&7: &f" + team.allies().size())));
            }
            case SETTINGS -> {
                buttons.put(12, toggle(Material.CHEST, "&cClaim Lock", "&7Current&7: " + (team.claimLocked() ? "&aEnabled" : "&7Disabled"), () -> {
                    requireManage(player, TeamRole.CAPTAIN);
                    team.claimLocked(!team.claimLocked());
                    teams.save(team);
                    menus.refresh(player);
                }));
                buttons.put(14, toggle(Material.IRON_SWORD, "&cFriendly Fire", "&7Current&7: " + (team.friendlyFire() ? "&cEnabled" : "&aDisabled"), () -> {
                    requireManage(player, TeamRole.CAPTAIN);
                    team.friendlyFire(!team.friendlyFire());
                    teams.save(team);
                    menus.refresh(player);
                }));
            }
            case INFORMATION -> {
                buttons.put(11, staticButton(item(Material.SIGN, "&cHome", "&7" + location(team.hq()))));
                buttons.put(13, staticButton(item(Material.BEACON, "&cRally", "&7" + location(team.rally()))));
                buttons.put(15, staticButton(item(Material.BOOK, "&cLogs", "&7Recent logs&7: &f" + team.logs().size())));
            }
            case MISSIONS -> {
                buttons.put(11, staticButton(item(Material.MAP, "&cClaim Mission", "&7Claim Locked&7: " + (team.claimLocked() ? "&aYes" : "&7No"), "&7HQ Set&7: " + (team.hq() == null ? "&cNo" : "&aYes"))));
                buttons.put(13, staticButton(item(Material.DIAMOND_SWORD, "&cPvP Mission", "&7KOTH Captures&7: &f" + team.kothCaps(), "&7Faction Points&7: &f" + team.points())));
                buttons.put(15, staticButton(item(Material.CHEST, "&cEconomy Mission", "&7Balance&7: &a$" + (long) team.balance(), "&7Members&7: &f" + team.members().size())));
            }
            case CONTRACTS -> {
                buttons.put(12, staticButton(item(Material.PAPER, "&cActive Contract", "&7Win KOTHs, protect DTR,", "&7and grow your faction balance.", "", "&7Progress updates from live faction data.")));
                buttons.put(14, staticButton(item(Material.BOOK_AND_QUILL, "&cContract Progress", "&7Points&7: &f" + team.points(), "&7KOTH Caps&7: &f" + team.kothCaps(), "&7DTR&7: &a" + String.format(Locale.US, "%.2f", team.dtr()))));
            }
            case POINTS -> {
                buttons.put(11, staticButton(item(Material.NETHER_STAR, "&cFaction Points", "&7Current&7: &f" + team.points())));
                buttons.put(13, staticButton(item(Material.GOLD_INGOT, "&cEconomy Weight", "&7Balance&7: &a$" + (long) team.balance())));
                buttons.put(15, staticButton(item(Material.BEACON, "&cEvent Weight", "&7KOTH Captures&7: &f" + team.kothCaps())));
            }
            case GEM_SHOP -> {
                buttons.put(12, staticButton(item(Material.EMERALD, "&cFaction Gems", "&7Spend faction rewards from", "&7events and contracts.", "", "&7Current Points&7: &f" + team.points())));
                buttons.put(14, staticButton(item(Material.DIAMOND, "&cReward Status", "&7Reward purchases are controlled", "&7through faction points and admin", "&7commands for this build.")));
            }
        }
    }

    private Button navigation(Page target, Material material, String name, String... lore) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item(material, name, lore);
            }

            @Override
            public void click(Player player, ClickType clickType) {
                new TeamManageMenu(menus, teams, dtr, team, target).open(player, menus);
            }
        };
    }

    private Button toggle(Material material, String name, String status, Runnable action) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item(material, name, status, "", "&aClick to toggle.");
            }

            @Override
            public void click(Player player, ClickType clickType) {
                try {
                    action.run();
                    player.sendMessage(Text.color("&8[&cTeam&8] &fTeam setting updated."));
                } catch (RuntimeException exception) {
                    player.sendMessage(Text.color("&8[&cTeam&8] &c" + exception.getMessage()));
                }
            }
        };
    }

    private void requireManage(Player player, TeamRole role) {
        if (!teams.canManage(player.getUniqueId(), team, role)) {
            throw new IllegalArgumentException("You must be " + pretty(role.name()) + " or above.");
        }
    }

    private Map<Integer, Button> base(int size) {
        Map<Integer, Button> buttons = new LinkedHashMap<>();
        Button filler = staticButton(item(Material.STAINED_GLASS_PANE, (short) 7, " "));
        for (int slot = 0; slot < size; slot++) {
            buttons.put(slot, filler);
        }
        return buttons;
    }

    private int online(Team team) {
        int online = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.isMember(player.getUniqueId())) {
                online++;
            }
        }
        return online;
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

    private static String location(com.testrank.hcf.core.util.Position position) {
        return position == null ? "None" : (int) position.x() + ", " + (int) position.y() + ", " + (int) position.z();
    }

    private static String pretty(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static Button staticButton(ItemStack item) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item;
            }
        };
    }

    private static ItemStack item(Material material, String name, String... lore) {
        return item(material, (short) 0, name, lore);
    }

    private static ItemStack item(Material material, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(name));
        meta.setLore(List.of(lore).stream().map(Text::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private enum Page {
        HOME("Home"),
        MEMBERS("Members"),
        STATISTICS("Statistics"),
        MISSIONS("Missions"),
        CONTRACTS("Contracts"),
        POINTS("Points"),
        GEM_SHOP("Gem Shop"),
        SETTINGS("Team Settings"),
        INFORMATION("Team Information");

        private final String display;

        Page(String display) {
            this.display = display;
        }
    }
}
