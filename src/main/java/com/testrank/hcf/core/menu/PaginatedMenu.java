package com.testrank.hcf.core.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class PaginatedMenu extends Menu {
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final int page;
    private final MenuService menus;

    protected PaginatedMenu(MenuService menus, int page) {
        this.menus = menus;
        this.page = Math.max(0, page);
    }

    protected abstract List<Button> content(Player player);

    protected abstract PaginatedMenu page(int page);

    @Override
    public Map<Integer, Button> buttons(Player player) {
        Map<Integer, Button> buttons = new LinkedHashMap<>();
        List<Button> content = content(player);
        int start = page * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && start + i < content.size(); i++) {
            buttons.put(CONTENT_SLOTS[i], content.get(start + i));
        }
        if (page > 0) {
            buttons.put(45, navigation("§ePrevious", page - 1));
        }
        if (start + CONTENT_SLOTS.length < content.size()) {
            buttons.put(53, navigation("§eNext", page + 1));
        }
        return buttons;
    }

    private Button navigation(String name, int targetPage) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                return ItemBuilder.of(Material.ARROW).name(name).build();
            }

            @Override
            public void click(Player player, org.bukkit.event.inventory.ClickType clickType) {
                page(targetPage).open(player, menus);
            }
        };
    }
}
