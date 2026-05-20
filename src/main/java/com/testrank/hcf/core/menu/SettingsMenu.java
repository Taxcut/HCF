package com.testrank.hcf.core.menu;

import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettingsMenu extends Menu {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            31, 32
    };

    private final MenuService menus;
    private final PlayerSettingsService settings;

    public SettingsMenu(MenuService menus, PlayerSettingsService settings) {
        this.menus = menus;
        this.settings = settings;
    }

    @Override
    public String title(Player player) {
        return Text.color("&cSettings");
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
        for (int index = 0; index < PlayerSettingsService.DEFINITIONS.size() && index < SLOTS.length; index++) {
            PlayerSettingsService.SettingDefinition definition = PlayerSettingsService.DEFINITIONS.get(index);
            buttons.put(SLOTS[index], toggle(definition, icon(index)));
        }
        buttons.put(40, staticButton(item(Material.BOOK, "&cSettings Help",
                "&7These toggles control client-side",
                "&7and HCF scoreboard preferences.",
                "",
                "&8Changes save automatically.")));
        return buttons;
    }

    private Button toggle(PlayerSettingsService.SettingDefinition definition, Material material) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                boolean enabled = settings.enabled(player.getUniqueId(), definition.key());
                return item(material, enabled ? "&a" + definition.displayName() : "&7" + definition.displayName(),
                        "&7Current Status: " + (enabled ? "&aEnabled" : "&7Disabled"),
                        "",
                        enabled ? "&cClick to disable." : "&aClick to enable.");
            }

            @Override
            public void click(Player player, ClickType clickType) {
                settings.toggle(player.getUniqueId(), definition.key());
                player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 1.25F);
                menus.refresh(player);
            }
        };
    }

    private static Material icon(int index) {
        Material[] materials = {
                Material.EYE_OF_ENDER, Material.NAME_TAG, Material.REDSTONE, Material.DIAMOND_SWORD,
                Material.MAP, Material.WATCH, Material.BEACON, Material.GOLD_INGOT,
                Material.BOW, Material.BONE, Material.COBBLESTONE, Material.GLASS,
                Material.SKULL_ITEM, Material.REDSTONE_TORCH_ON, Material.PAPER, Material.GOLD_BLOCK
        };
        return materials[index % materials.length];
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

    private static ItemStack item(Material material, short durability, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, durability);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(name));
        meta.setLore(List.of(lore).stream().map(Text::color).toList());
        item.setItemMeta(meta);
        return item;
    }
}
