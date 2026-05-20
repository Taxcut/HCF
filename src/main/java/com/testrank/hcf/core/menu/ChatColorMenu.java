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

public final class ChatColorMenu extends Menu {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            31
    };

    private final MenuService menus;
    private final PlayerSettingsService settings;

    public ChatColorMenu(MenuService menus, PlayerSettingsService settings) {
        this.menus = menus;
        this.settings = settings;
    }

    @Override
    public String title(Player player) {
        return Text.color("&cChat Color");
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
        PlayerSettingsService.ChatColorOption[] options = PlayerSettingsService.ChatColorOption.values();
        for (int index = 0; index < options.length && index < SLOTS.length; index++) {
            buttons.put(SLOTS[index], option(options[index]));
        }
        buttons.put(4, staticButton(item(Material.SIGN, "&cPreview",
                "&7[Rank] " + player.getName() + "&7: " + settings.chatColor(player.getUniqueId()) + "Hello world!",
                "",
                "&8Permissions unlock premium colors.")));
        return buttons;
    }

    private Button option(PlayerSettingsService.ChatColorOption option) {
        return new Button() {
            @Override
            public ItemStack icon(Player player) {
                boolean unlocked = player.hasPermission(option.permission()) || player.hasPermission("hcf.chatcolor.*") || player.isOp();
                boolean selected = settings.chatColor(player.getUniqueId()).equalsIgnoreCase(option.color());
                Material material = selected ? Material.EMERALD_BLOCK : unlocked ? Material.INK_SACK : Material.STAINED_CLAY;
                short data = selected ? 0 : dyeData(option);
                return item(material, data, option.color() + option.displayName(),
                        "&7Preview&7: " + option.color() + "Hello world!",
                        "&7Status&7: " + (selected ? "&aSelected" : unlocked ? "&aUnlocked" : "&cLocked"),
                        "",
                        unlocked ? "&aClick to select." : "&cUnlock this from rank/store rewards.");
            }

            @Override
            public void click(Player player, ClickType clickType) {
                boolean unlocked = player.hasPermission(option.permission()) || player.hasPermission("hcf.chatcolor.*") || player.isOp();
                if (!unlocked) {
                    player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.7F, 0.6F);
                    player.sendMessage(Text.color("&8[&cChatColor&8] &cYou have not unlocked " + option.displayName() + "."));
                    return;
                }
                settings.chatColor(player.getUniqueId(), option.color());
                player.playSound(player.getLocation(), Sound.CLICK, 0.7F, 1.6F);
                menus.refresh(player);
            }
        };
    }

    private static short dyeData(PlayerSettingsService.ChatColorOption option) {
        return switch (option) {
            case GOLD, YELLOW -> 11;
            case GRAY -> 8;
            case BLUE, DARK_BLUE -> 4;
            case GREEN, DARK_GREEN -> 10;
            case RED, DARK_RED -> 1;
            case PURPLE, DARK_PURPLE -> 5;
            case AQUA, DARK_AQUA -> 6;
            case BLACK -> 0;
            case WHITE -> 15;
        };
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
