package com.testrank.hcf.core.menu;

import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.profile.PlayerStateService;
import com.testrank.hcf.core.profile.Profile;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.team.DtrService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.timer.GlobalTimerService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HelpMenu extends Menu {
    private static final int[] FEATURE_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 40};
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a z", Locale.US);

    private final ProfileService profiles;
    private final PlayerStateService states;
    private final TeamService teams;
    private final DtrService dtr;
    private final EventService events;
    private final GlobalTimerService timers;

    public HelpMenu(ProfileService profiles, PlayerStateService states, TeamService teams, DtrService dtr,
                    EventService events, GlobalTimerService timers) {
        this.profiles = profiles;
        this.states = states;
        this.teams = teams;
        this.dtr = dtr;
        this.events = events;
        this.timers = timers;
    }

    @Override
    public String title(Player player) {
        return "Help";
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public Map<Integer, Button> buttons(Player player) {
        Map<Integer, Button> buttons = new LinkedHashMap<>();
        Button filler = staticButton(item(Material.STAINED_GLASS_PANE, (short) 7, " "));
        for (int slot = 0; slot < size(); slot++) {
            buttons.put(slot, filler);
        }

        buttons.put(4, action(
                item(Material.BOOK, "&cWelcome to HCF",
                        "&7HCF is a competitive faction gamemode",
                        "&7focused on team-based PvP, claims,",
                        "&7events and survival progression.",
                        "",
                        "&cServer Links:",
                        "&4▌ &fStore: &chttps://store.hcf.local",
                        "&4▌ &fWebsite: &chttps://hcf.local",
                        "&4▌ &fDiscord: &chttps://discord.gg/hcf",
                        "",
                        "&aClick to view all links in chat."),
                clicked -> {
                    clicked.closeInventory();
                    clicked.sendMessage(Text.color("&8[&cHCF&8] &fStore: &chttps://store.hcf.local"));
                    clicked.sendMessage(Text.color("&8[&cHCF&8] &fWebsite: &chttps://hcf.local"));
                    clicked.sendMessage(Text.color("&8[&cHCF&8] &fDiscord: &chttps://discord.gg/hcf"));
                }));

        int index = 0;
        for (HelpEntry entry : entries(player)) {
            buttons.put(FEATURE_SLOTS[index++], action(entry.icon(), entry.action()));
        }
        return buttons;
    }

    private List<HelpEntry> entries(Player player) {
        List<HelpEntry> entries = new ArrayList<>(FEATURE_SLOTS.length);
        entries.add(new HelpEntry(item(Material.NETHER_STAR, "&cEvents",
                "&7Play, win and compete in events for",
                "&7OP items, points, currency, and more!",
                "",
                "&cInformation:",
                "&4▌ &fKOTH",
                "&4▌ &fCitadel",
                "&4▌ &fConquest",
                "&4▌ &fGlowstone Mountain",
                "&4▌ &fSOTW / EOTW",
                "",
                "&aClick for more information about all events."),
                clicked -> clicked.performCommand("schedule")));

        entries.add(new HelpEntry(factions(player), clicked -> clicked.performCommand("team info")));

        entries.add(new HelpEntry(item(Material.DIAMOND_CHESTPLATE, "&cKits",
                "&7Access kits for items, armor and potions.",
                "&7Several are free and the rest unlock",
                "&7through ranks or events.",
                "",
                "&cInformation:",
                "&4▌ &fStarter Kit: &aUnlocked",
                "&4▌ &fPvP Kit: &aUnlocked",
                "&4▌ &fKits Unlocked: &c2&7/&c6",
                "",
                "&aClick to view all kits."),
                clicked -> clicked.performCommand("kit")));

        entries.add(new HelpEntry(item(Material.CHEST, "&cCustom Items",
                "&7Items that are custom to the server.",
                "",
                "&cItems:",
                "&4▌ &fAbility Items",
                "&4▌ &fPartner Items",
                "&4▌ &fExotic Items",
                "&4▌ &fCrowbars",
                "&4▌ &fCustom Enchants",
                "",
                "&aClick to view all custom items."),
                clicked -> clicked.performCommand("partner")));

        entries.add(new HelpEntry(item(Material.BLAZE_ROD, "&cSettings",
                "&7Modify your experience in the settings.",
                "",
                "&aClick to modify settings."),
                clicked -> clicked.performCommand("settings")));

        entries.add(new HelpEntry(item(Material.EMERALD_BLOCK, "&cFree Items",
                "&7Need free items? Click to find out",
                "&7everyday ways to get stacked and setup.",
                "",
                "&cInformation:",
                "&4▌ &fFree Keys",
                "&4▌ &fFree Ranks",
                "&4▌ &fFree Kits",
                "&4▌ &fFree Ability Items",
                "&4▌ &fReclaim Rewards",
                "",
                "&aClick to view ways to get free items."),
                clicked -> clicked.performCommand("reclaim")));

        entries.add(new HelpEntry(item(Material.GOLD_INGOT, "&cBattle Pass",
                "&7Complete challenges and unlock rewards",
                "&7to advance further in your playing experience!",
                "",
                "&cInformation:",
                "&4▌ &fTier: &c1",
                "&4▌ &fExperience: &c0 XP",
                "",
                "&aClick to view progression rewards."),
                clicked -> clicked.performCommand("leaderboards missions_completed")));

        entries.add(new HelpEntry(leaderboards(player), clicked -> clicked.performCommand("leaderboards")));

        entries.add(new HelpEntry(schedule(), clicked -> clicked.performCommand("schedule")));

        entries.add(new HelpEntry(item(Material.ANVIL, "&cCosmetics",
                "&7Enhance your game with cosmetics.",
                "",
                "&cInformation:",
                "&4▌ &fTags",
                "&4▌ &fKill Tags",
                "&4▌ &fChat Colors",
                "&4▌ &fKill Effects",
                "",
                "&aClick to view all cosmetics."),
                clicked -> clicked.performCommand("chatcolor")));

        entries.add(new HelpEntry(item(Material.PAPER, "&cChangelogs",
                "&7View recent server updates and fixes.",
                "",
                "&aClick to view changelogs."),
                clicked -> clicked.performCommand("changelog")));

        entries.add(new HelpEntry(item(Material.REDSTONE, "&cPanic",
                "&7Quickly alert staff if you need help.",
                "",
                "&aClick to panic."),
                clicked -> clicked.performCommand("panic")));

        entries.add(new HelpEntry(item(Material.SIGN, "&cSocial",
                "&7View server social links.",
                "",
                "&aClick to view socials."),
                clicked -> clicked.performCommand("discord")));

        entries.add(new HelpEntry(item(Material.MAP, "&cMedia",
                "&7Creator and media information.",
                "",
                "&aClick to view media commands."),
                clicked -> clicked.performCommand("media")));

        entries.add(new HelpEntry(item(Material.CHEST, "&cGiveaway",
                "&7Join server giveaways and rewards.",
                "",
                "&aClick to view giveaway info."),
                clicked -> clicked.performCommand("giveaway")));
        return entries;
    }

    private ItemStack factions(Player player) {
        Team team = teams.byPlayer(player.getUniqueId()).orElse(null);
        String hq = team == null || team.hq() == null ? "No" : "Yes";
        int points = team == null ? 0 : team.points();
        int members = team == null ? 1 : team.members().size();
        String dtrText = team == null ? "1.01/1.01" : String.format(Locale.US, "%.2f/%.2f", Math.max(0.0D, team.dtr()), Math.max(1.01D, team.members().size() + 0.01D));
        String raid = team != null && dtr.raidability(team) ? " &4RAIDABLE" : "";
        return item(Material.DIAMOND_BLOCK, "&cFactions",
                "&7Team up with other players,",
                "&7claim land, complete tasks and more!",
                "",
                "&cInformation:",
                "&4▌ &fHQ: &c" + hq,
                "&4▌ &fGems: &c0",
                "&4▌ &fPoints: &c" + points,
                "&4▌ &fMembers: &c" + members + "&7/&c" + members,
                "&4▌ &fDeaths Until Raidable: &a" + dtrText + raid,
                "",
                "&aClick to manage your faction.");
    }

    private ItemStack leaderboards(Player player) {
        Profile profile = profiles.cached(player.getUniqueId()).orElse(null);
        int kills = profile == null ? 0 : profile.kills();
        int deaths = profile == null ? 0 : profile.deaths();
        double kdr = deaths == 0 ? kills : (double) kills / deaths;
        return item(Material.FIREWORK, "&cLeaderboards",
                "&7View leaderboards for this map.",
                "",
                "&cYour Statistics:",
                "&4▌ &fKills: &c" + kills,
                "&4▌ &fDeaths: &c" + deaths,
                "&4▌ &fAssists: &c0",
                "&4▌ &fKDR: &c" + String.format(Locale.US, "%.1f", kdr),
                "&4▌ &fPlaytime: &c" + formatPlaytime(states.playtime(player.getUniqueId())),
                "",
                "&aClick to view the map's leaderboards.");
    }

    private ItemStack schedule() {
        long activeEvents = events.events().stream().filter(event -> event.active()).count();
        long activeTimers = timers.activeTimers().size();
        String time = LocalDateTime.now(ZoneId.systemDefault()).atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
        return item(Material.WATCH, "&cSchedule",
                "&7View everything that's supposed",
                "&7to happen in the map and when.",
                "",
                "&cInformation:",
                "&4▌ &fCurrent Time: &c" + time,
                "&4▌ &fActive Events: &c" + activeEvents,
                "&4▌ &fActive Timers: &c" + activeTimers,
                "",
                "&aClick to view the map's schedule.");
    }

    private static Button staticButton(ItemStack item) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item;
            }
        };
    }

    private static Button action(ItemStack item, java.util.function.Consumer<Player> action) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item;
            }

            @Override
            public void click(Player player, ClickType clickType) {
                action.accept(player);
            }
        };
    }

    private static ItemStack item(Material material, String name, String... lore) {
        return item(material, (short) 0, name, lore);
    }

    private static ItemStack item(Material material, short durability, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(name));
        List<String> colored = new ArrayList<>(lore.length);
        for (String line : lore) {
            colored.add(Text.color(line));
        }
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatPlaytime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remaining = seconds % 60L;
        if (hours > 0) {
            return hours + " hours " + minutes + " minutes";
        }
        return minutes + " minutes " + remaining + " seconds";
    }

    private record HelpEntry(ItemStack icon, java.util.function.Consumer<Player> action) {}
}
