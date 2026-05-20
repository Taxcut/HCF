package com.testrank.hcf.core.shop;

import com.testrank.hcf.core.menu.Button;
import com.testrank.hcf.core.menu.Menu;
import com.testrank.hcf.core.menu.MenuService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopMenu extends Menu {
    private final MenuService menus;
    private final ShopService shop;

    public ShopMenu(MenuService menus, ShopService shop) {
        this.menus = menus;
        this.shop = shop;
    }

    @Override
    public String title(Player player) {
        return Text.color("&cShop");
    }

    @Override
    public int size() {
        return 36;
    }

    @Override
    public Map<Integer, Button> buttons(Player player) {
        Map<Integer, Button> buttons = base(size());
        for (ShopCategory category : shop.categories()) {
            buttons.put(category.slot(), new Button() {
                @Override
                public ItemStack icon(Player player) {
                    ItemStack item = new ItemStack(category.icon(), 1, category.iconData());
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(Text.color("&c" + category.displayName()));
                    meta.setLore(List.of(
                            Text.color("&7Browse this category's buy"),
                            Text.color("&7and sell offers."),
                            "",
                            Text.color("&aClick to open.")
                    ));
                    item.setItemMeta(meta);
                    return item;
                }

                @Override
                public void click(Player player, ClickType clickType) {
                    new ShopCategoryMenu(menus, shop, category).open(player, menus);
                }
            });
        }
        buttons.put(31, new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item(Material.EMERALD, "&aSell All",
                        "&7Sell every sellable item in",
                        "&7your inventory at once.",
                        "",
                        "&aClick to sell all.");
            }

            @Override
            public void click(Player player, ClickType clickType) {
                shop.sellAll(player);
                menus.refresh(player);
            }
        });
        return buttons;
    }

    static Map<Integer, Button> base(int size) {
        Map<Integer, Button> buttons = new LinkedHashMap<>();
        Button filler = staticButton(item(Material.STAINED_GLASS_PANE, (short) 7, " "));
        for (int slot = 0; slot < size; slot++) {
            buttons.put(slot, filler);
        }
        return buttons;
    }

    static Button staticButton(ItemStack item) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return item;
            }
        };
    }

    static ItemStack item(Material material, String name, String... lore) {
        return item(material, (short) 0, name, lore);
    }

    static ItemStack item(Material material, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(name));
        meta.setLore(List.of(lore).stream().map(Text::color).toList());
        item.setItemMeta(meta);
        return item;
    }
}
