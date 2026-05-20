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

import java.util.List;
import java.util.Map;

public final class ShopCategoryMenu extends Menu {
    private final MenuService menus;
    private final ShopService shop;
    private final ShopCategory category;

    public ShopCategoryMenu(MenuService menus, ShopService shop, ShopCategory category) {
        this.menus = menus;
        this.shop = shop;
        this.category = category;
    }

    @Override
    public String title(Player player) {
        return Text.color("&cShop - " + category.displayName());
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public Map<Integer, Button> buttons(Player player) {
        Map<Integer, Button> buttons = ShopMenu.base(size());
        for (ShopItem item : category.items()) {
            buttons.put(item.slot(), button(item));
        }
        buttons.put(45, new Button() {
            @Override
            public ItemStack icon(Player player) {
                return ShopMenu.item(Material.ARROW, "&cBack", "&7Return to shop categories.", "", "&aClick to go back.");
            }

            @Override
            public void click(Player player, ClickType clickType) {
                new ShopMenu(menus, shop).open(player, menus);
            }
        });
        buttons.put(49, new Button() {
            @Override
            public ItemStack icon(Player player) {
                return ShopMenu.item(Material.EMERALD, "&aSell All",
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

    private Button button(ShopItem shopItem) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                ItemStack item = shop.stack(shopItem, shopItem.amount());
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(Text.color("&c" + shopItem.displayName()));
                meta.setLore(List.of(
                        Text.color("&7Amount&7: &f" + shopItem.amount()),
                        Text.color("&aBuy&7: " + price(shopItem.buyPrice())),
                        Text.color("&cSell&7: " + price(shopItem.sellPrice())),
                        "",
                        Text.color("&aLeft Click &7to buy."),
                        Text.color("&cRight Click &7to sell."),
                        Text.color("&eShift Click &7for 64x bulk."),
                        Text.color("&bMiddle Click &7to sell all of this item.")
                ));
                item.setItemMeta(meta);
                return item;
            }

            @Override
            public void click(Player player, ClickType clickType) {
                if (clickType == ClickType.MIDDLE) {
                    shop.sellAll(player, shopItem);
                } else if (clickType.isRightClick()) {
                    shop.sell(player, shopItem, clickType.isShiftClick() ? 64 : 1);
                } else if (clickType.isLeftClick()) {
                    shop.buy(player, shopItem, clickType.isShiftClick() ? 64 : 1);
                }
                menus.refresh(player);
            }
        };
    }

    private static String price(long price) {
        return price < 0L ? Text.color("&7Not available") : Text.color("&f$" + price);
    }
}
