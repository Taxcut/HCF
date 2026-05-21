package com.testrank.hcf.core.partneritems;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import com.testrank.hcf.core.timer.CooldownService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PartnerItemService implements HCFService {
    private final Plugin plugin;
    private final CooldownService cooldowns;
    private final ClientIntegrationService clients;
    private final Map<String, PartnerItemDefinition> definitions = new ConcurrentHashMap<>();
    private static final String META_PREFIX = "§0partner:";

    public PartnerItemService(Plugin plugin, CooldownService cooldowns, ClientIntegrationService clients) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.clients = clients;
    }

    @Override
    public void start() {
        registerDefaults();
        loadConfiguredItems();
    }

    private void registerDefaults() {
        register(new PartnerItemDefinition(
                "rage_brick",
                "§cRage Brick",
                Material.BRICK,
                List.of("§7Right click to activate.", "§cCooldown§7: §f60s"),
                60_000L,
                List.of(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 8, 1)),
                List.of()
        ));
        register(new PartnerItemDefinition(
                "guardian_angel",
                "§bGuardian Angel",
                Material.FEATHER,
                List.of("§7Right click to activate.", "§cCooldown§7: §f90s"),
                90_000L,
                List.of(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2), new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 1)),
                List.of()
        ));
    }

    private void loadConfiguredItems() {
        File file = new File(plugin.getDataFolder(), "abilities.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.getBoolean("enabled", true)) {
            definitions.clear();
            return;
        }
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        int defaultCooldown = config.getInt("defaults.global-cooldown-seconds", 8);
        for (String id : items.getKeys(false)) {
            String path = "items." + id;
            if (!config.getBoolean(path + ".enabled", true)) {
                definitions.remove(id);
                continue;
            }
            Material material = Material.matchMaterial(config.getString(path + ".material", "NETHER_STAR"));
            if (material == null) {
                plugin.getLogger().warning("Skipping ability item " + id + " because material is invalid.");
                continue;
            }
            List<String> lore = config.getStringList(path + ".lore").stream().map(Text::color).toList();
            register(new PartnerItemDefinition(
                    id,
                    Text.color(config.getString(path + ".name", "&c" + id.replace('_', ' '))),
                    material,
                    lore,
                    Math.max(1, config.getInt(path + ".cooldown-seconds", defaultCooldown)) * 1000L,
                    effects(config.getStringList(path + ".self-effects")),
                    effects(config.getStringList(path + ".target-effects"))
            ));
        }
    }

    public void register(PartnerItemDefinition definition) {
        definitions.put(definition.id(), definition);
    }

    public List<String> ids() {
        return definitions.keySet().stream().sorted().toList();
    }

    public ItemStack create(String id, int amount) {
        PartnerItemDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown partner item: " + id);
        }
        ItemStack item = new ItemStack(definition.material(), amount);
        var meta = item.getItemMeta();
        meta.setDisplayName(definition.displayName());
        List<String> lore = new ArrayList<>();
        if (definition.lore().isEmpty()) {
            lore.add("§7Right click to activate.");
        } else {
            lore.addAll(definition.lore());
        }
        lore.add(META_PREFIX + id);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return Optional.empty();
        }
        return lore.stream().filter(line -> line.startsWith(META_PREFIX)).map(line -> line.substring(META_PREFIX.length())).findFirst();
    }

    public boolean tryUse(Player player, ItemStack item) {
        Optional<String> optionalId = id(item);
        if (optionalId.isEmpty()) {
            return false;
        }
        String id = optionalId.get();
        PartnerItemDefinition definition = definitions.get(id);
        if (definition == null || cooldowns.has(player.getUniqueId(), "partner:" + id)) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), "partner:" + id, definition.cooldownMillis());
        definition.selfEffects().forEach(player::addPotionEffect);
        item.setAmount(Math.max(0, item.getAmount() - 1));
        clients.sendNotification(player, "Partner Item", id + " used");
        return true;
    }

    private static List<PotionEffect> effects(List<String> lines) {
        List<PotionEffect> effects = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase(java.util.Locale.ROOT));
            if (type == null) {
                continue;
            }
            int seconds = parts.length >= 2 ? parseInt(parts[1], 5) : 5;
            int amplifier = parts.length >= 3 ? parseInt(parts[2], 1) - 1 : 0;
            effects.add(new PotionEffect(type, Math.max(1, seconds) * 20, Math.max(0, amplifier)));
        }
        return effects;
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
