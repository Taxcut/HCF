package com.testrank.hcf.core.menu;

import com.testrank.hcf.core.leaderboard.LeaderboardService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LeaderboardMenu extends Menu {
    private static final int[] CATEGORY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            30, 32
    };

    private final MenuService menus;
    private final LeaderboardService leaderboards;
    private final LeaderboardService.Category category;

    public LeaderboardMenu(MenuService menus, LeaderboardService leaderboards) {
        this(menus, leaderboards, null);
    }

    private LeaderboardMenu(MenuService menus, LeaderboardService leaderboards, LeaderboardService.Category category) {
        this.menus = menus;
        this.leaderboards = leaderboards;
        this.category = category;
    }

    @Override
    public String title(Player player) {
        return Text.color(category == null ? "&cLeaderboards" : "&c" + category.displayName());
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public Map<Integer, Button> buttons(Player player) {
        Map<Integer, Button> buttons = base(size());
        if (category != null) {
            renderCategory(buttons);
            buttons.put(40, navigation(null, Material.ARROW, "&cBack", "&7Return to leaderboards."));
            return buttons;
        }
        LeaderboardService.Category[] categories = LeaderboardService.Category.values();
        for (int index = 0; index < categories.length && index < CATEGORY_SLOTS.length; index++) {
            LeaderboardService.Category selected = categories[index];
            buttons.put(CATEGORY_SLOTS[index], navigation(selected, icon(index), "&c" + selected.displayName(),
                    "&7View the top players for",
                    "&7this map statistic.",
                    "",
                    "&aClick to open."));
        }
        buttons.put(4, staticButton(item(Material.NETHER_STAR, "&cMap Leaderboards",
                "&7Cached and refreshed without",
                "&7heavy database work on click.",
                "",
                "&8Shows loaded player data.")));
        return buttons;
    }

    private void renderCategory(Map<Integer, Button> buttons) {
        int slot = 10;
        int rank = 1;
        for (LeaderboardService.Entry entry : leaderboards.top(category)) {
            buttons.put(slot++, staticButton(item(Material.PAPER, rankColor(rank) + "#" + rank + " &c" + entry.name(),
                    "&7Value&7: &f" + category.valueText(entry.value()),
                    "",
                    "&8Updated from live profile stats.")));
            rank++;
            if (slot == 17) {
                slot = 19;
            }
            if (slot == 26) {
                slot = 28;
            }
        }
    }

    private Button navigation(LeaderboardService.Category target, Material material, String name, String... lore) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item(material, name, lore);
            }

            @Override
            public void click(Player player, ClickType clickType) {
                new LeaderboardMenu(menus, leaderboards, target).open(player, menus);
            }
        };
    }

    private static Material icon(int index) {
        Material[] materials = {
                Material.DIAMOND_SWORD, Material.SKULL_ITEM, Material.BOOK, Material.GOLD_SWORD,
                Material.IRON_SWORD, Material.NETHER_STAR, Material.BEACON, Material.EMERALD,
                Material.FIREWORK, Material.MAP, Material.PAPER, Material.WATCH,
                Material.GOLD_INGOT, Material.REDSTONE, Material.POTION, Material.DIAMOND_ORE
        };
        return materials[index % materials.length];
    }

    private static String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "&6";
            case 2 -> "&f";
            case 3 -> "&9";
            default -> "&7";
        };
    }

    private static Map<Integer, Button> base(int size) {
        Map<Integer, Button> buttons = new LinkedHashMap<>();
        Button filler = staticButton(item(Material.STAINED_GLASS_PANE, (short) 7, " "));
        for (int slot = 0; slot < size; slot++) {
            buttons.put(slot, filler);
        }
        return buttons;
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
}
