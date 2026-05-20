package com.testrank.hcf.core.shop;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.economy.EconomyService;
import com.testrank.hcf.core.mongo.MongoManager;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.util.Text;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ShopService implements HCFService {
    private final Plugin plugin;
    private final EconomyService economy;
    private final MongoManager mongo;
    private final Threading threading;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    public ShopService(Plugin plugin, EconomyService economy, MongoManager mongo, Threading threading) {
        this.plugin = plugin;
        this.economy = economy;
        this.mongo = mongo;
        this.threading = threading;
    }

    @Override
    public void start() {
        reload();
    }

    public void reload() {
        categories.clear();
        File file = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            return;
        }
        for (String categoryId : section.getKeys(false)) {
            ConfigurationSection categorySection = section.getConfigurationSection(categoryId);
            if (categorySection == null) {
                continue;
            }
            List<ShopItem> items = new ArrayList<>();
            Set<Integer> usedSlots = new HashSet<>();
            ConfigurationSection itemSection = categorySection.getConfigurationSection("items");
            if (itemSection != null) {
                for (String itemId : itemSection.getKeys(false)) {
                    ConfigurationSection raw = itemSection.getConfigurationSection(itemId);
                    if (raw == null) {
                        continue;
                    }
                    Material material = material(raw.getString("material", "STONE"), "shop.yml categories." + categoryId + ".items." + itemId + ".material", Material.STONE);
                    int slot = boundedSlot(raw.getInt("slot", items.size()), 0, 53, "shop.yml categories." + categoryId + ".items." + itemId + ".slot");
                    while (usedSlots.contains(slot) && slot < 53) {
                        slot++;
                    }
                    usedSlots.add(slot);
                    items.add(new ShopItem(
                            itemId,
                            material,
                            (short) raw.getInt("data", 0),
                            Math.max(1, raw.getInt("amount", 1)),
                            Math.max(-1L, raw.getLong("buy", -1L)),
                            Math.max(-1L, raw.getLong("sell", -1L)),
                            slot,
                            raw.getString("name", pretty(material.name()))
                    ));
                }
            }
            items.sort(Comparator.comparingInt(ShopItem::slot));
            categories.put(categoryId.toLowerCase(Locale.ROOT), new ShopCategory(
                    categoryId.toLowerCase(Locale.ROOT),
                    categorySection.getString("name", pretty(categoryId)),
                    material(categorySection.getString("icon", "CHEST"), "shop.yml categories." + categoryId + ".icon", Material.CHEST),
                    (short) categorySection.getInt("icon-data", 0),
                    boundedSlot(categorySection.getInt("slot", categories.size()), 0, 35, "shop.yml categories." + categoryId + ".slot"),
                    List.copyOf(items)
            ));
        }
        plugin.getLogger().info("Loaded " + categories.size() + " shop categories.");
    }

    public Collection<ShopCategory> categories() {
        return categories.values();
    }

    public Optional<ShopCategory> category(String id) {
        return Optional.ofNullable(categories.get(id.toLowerCase(Locale.ROOT)));
    }

    public void buy(Player player, ShopItem item, int multiplier) {
        if (item.buyPrice() < 0L) {
            player.sendMessage(Text.color("&8[&cShop&8] &cThis item cannot be purchased."));
            return;
        }
        int units = safeUnits(item.amount(), multiplier);
        int amount = item.amount() * units;
        long price = item.buyPrice() * units;
        if (!hasSpace(player, item, amount)) {
            player.sendMessage(Text.color("&8[&cShop&8] &cYou do not have enough inventory space."));
            return;
        }
        economy.withdraw(player.getUniqueId(), price).thenAccept(success ->
                threading.runSync(() -> {
                    if (!success) {
                        player.sendMessage(Text.color("&8[&cShop&8] &cYou need $" + price + " to buy this."));
                        player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.8F, 0.6F);
                        return;
                    }
                    if (!player.isOnline() || !hasSpace(player, item, amount)) {
                        economy.add(player.getUniqueId(), price);
                        if (player.isOnline()) {
                            player.sendMessage(Text.color("&8[&cShop&8] &cYour inventory filled up; purchase refunded."));
                            player.playSound(player.getLocation(), Sound.NOTE_BASS, 0.8F, 0.6F);
                        }
                        return;
                    }
                    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack(item, amount));
                    if (!leftovers.isEmpty()) {
                        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                        player.sendMessage(Text.color("&8[&cShop&8] &cInventory changed mid-purchase; dropped leftovers at your feet."));
                    }
                    player.updateInventory();
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.4F);
                    player.sendMessage(Text.color("&8[&cShop&8] &fBought &cx" + amount + " " + item.displayName() + " &ffor &a$" + price + "&f."));
                    log(player, "BUY", item, amount, price);
                }));
    }

    public void sell(Player player, ShopItem item, int multiplier) {
        if (item.sellPrice() < 0L) {
            player.sendMessage(Text.color("&8[&cShop&8] &cThis item cannot be sold."));
            return;
        }
        int units = safeUnits(item.amount(), multiplier);
        int amount = item.amount() * units;
        if (count(player, item) < amount) {
            player.sendMessage(Text.color("&8[&cShop&8] &cYou do not have enough of that item."));
            return;
        }
        remove(player, item, amount);
        long value = item.sellPrice() * units;
        economy.add(player.getUniqueId(), value).thenRun(() ->
                threading.runSync(() -> {
                    player.playSound(player.getLocation(), Sound.CLICK, 0.8F, 1.6F);
                    player.sendMessage(Text.color("&8[&cShop&8] &fSold &cx" + amount + " " + item.displayName() + " &ffor &a$" + value + "&f."));
                    log(player, "SELL", item, amount, value);
                }));
    }

    public void sellAll(Player player, ShopItem item) {
        if (item.sellPrice() < 0L) {
            player.sendMessage(Text.color("&8[&cShop&8] &cThis item cannot be sold."));
            return;
        }
        int amount = count(player, item);
        int units = amount / Math.max(1, item.amount());
        if (units <= 0) {
            player.sendMessage(Text.color("&8[&cShop&8] &cYou do not have any of that item."));
            return;
        }
        int removed = units * item.amount();
        remove(player, item, removed);
        long value = Math.max(0L, units * item.sellPrice());
        economy.add(player.getUniqueId(), value).thenRun(() ->
                threading.runSync(() -> {
                    player.playSound(player.getLocation(), Sound.CLICK, 0.8F, 1.8F);
                    player.sendMessage(Text.color("&8[&cShop&8] &fSold all &c" + item.displayName() + " &ffor &a$" + value + "&f."));
                    log(player, "SELL_ALL", item, removed, value);
                }));
    }

    public void sellAll(Player player) {
        long total = 0L;
        List<SoldItem> sold = new ArrayList<>();
        for (ShopCategory category : categories.values()) {
            for (ShopItem item : category.items()) {
                if (item.sellPrice() <= 0L) {
                    continue;
                }
                int amount = count(player, item);
                int units = amount / Math.max(1, item.amount());
                if (units <= 0) {
                    continue;
                }
                int removed = units * item.amount();
                remove(player, item, removed);
                long value = units * item.sellPrice();
                total += value;
                sold.add(new SoldItem(item, removed, value));
            }
        }
        if (total <= 0L) {
            player.sendMessage(Text.color("&8[&cShop&8] &cYou do not have anything sellable."));
            return;
        }
        long finalTotal = total;
        economy.add(player.getUniqueId(), finalTotal).thenRun(() ->
                threading.runSync(() -> {
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.35F);
                    player.sendMessage(Text.color("&8[&cShop&8] &fSold &ceverything sellable &ffor &a$" + finalTotal + "&f."));
                    for (SoldItem soldItem : sold) {
                        log(player, "SELL_ALL", soldItem.item(), soldItem.amount(), soldItem.value());
                    }
                }));
    }

    public ItemStack stack(ShopItem item, int amount) {
        return new ItemStack(item.material(), amount, item.data());
    }

    private boolean hasSpace(Player player, ShopItem item, int amount) {
        int remaining = amount;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content == null || content.getType() == Material.AIR) {
                remaining -= item.material().getMaxStackSize();
            } else if (same(content, item) && content.getAmount() < content.getMaxStackSize()) {
                remaining -= content.getMaxStackSize() - content.getAmount();
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private int count(Player player, ShopItem item) {
        int total = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (same(content, item)) {
                total += content.getAmount();
            }
        }
        return total;
    }

    private void remove(Player player, ShopItem item, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack content = contents[slot];
            if (!same(content, item)) {
                continue;
            }
            int taken = Math.min(content.getAmount(), remaining);
            remaining -= taken;
            content.setAmount(content.getAmount() - taken);
            contents[slot] = content.getAmount() <= 0 ? null : content;
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    private boolean same(ItemStack stack, ShopItem item) {
        return stack != null
                && stack.getType() == item.material()
                && stack.getDurability() == item.data();
    }

    private int safeUnits(int base, int multiplier) {
        int safeBase = Math.max(1, base);
        int maxUnits = Math.max(1, 2304 / safeBase);
        return Math.max(1, Math.min(Math.max(1, multiplier), maxUnits));
    }

    private void log(Player player, String action, ShopItem item, int amount, long value) {
        plugin.getLogger().info("[Shop] " + player.getName() + " " + action + " " + amount + "x " + item.id() + " for $" + value);
        if (!mongo.enabled()) {
            return;
        }
        Document document = new Document("type", "shop")
                .append("createdAt", System.currentTimeMillis())
                .append("player", player.getUniqueId().toString())
                .append("name", player.getName())
                .append("action", action)
                .append("item", item.id())
                .append("amount", amount)
                .append("value", value);
        MongoManager.toFuture(mongo.collection("logs").insertOne(document)).exceptionally(throwable -> null);
    }

    private Material material(String name, String path, Material fallback) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        if (material == null) {
            plugin.getLogger().warning("Invalid material at " + path + ": " + name + " (using " + fallback.name() + ")");
            return fallback;
        }
        return material;
    }

    private int boundedSlot(int slot, int min, int max, String path) {
        if (slot < min || slot > max) {
            int bounded = Math.max(min, Math.min(max, slot));
            plugin.getLogger().warning("Invalid slot at " + path + ": " + slot + " (using " + bounded + ")");
            return bounded;
        }
        return slot;
    }

    private static String pretty(String input) {
        String[] parts = input.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.length() == 0 ? "Item" : builder.toString();
    }

    private record SoldItem(ShopItem item, int amount, long value) {}
}
