package com.testrank.hcf.core.partneritems;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import com.testrank.hcf.core.timer.CooldownService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PartnerItemService implements HCFService {
    private final CooldownService cooldowns;
    private final ClientIntegrationService clients;
    private final Map<String, PartnerItemDefinition> definitions = new ConcurrentHashMap<>();
    private static final String META_PREFIX = "§0partner:";

    public PartnerItemService(CooldownService cooldowns, ClientIntegrationService clients) {
        this.cooldowns = cooldowns;
        this.clients = clients;
    }

    @Override
    public void start() {
        register(new PartnerItemDefinition(
                "rage_brick",
                "§cRage Brick",
                Material.BRICK,
                60_000L,
                List.of(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 8, 1)),
                List.of()
        ));
        register(new PartnerItemDefinition(
                "guardian_angel",
                "§bGuardian Angel",
                Material.FEATHER,
                90_000L,
                List.of(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2), new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 1)),
                List.of()
        ));
    }

    public void register(PartnerItemDefinition definition) {
        definitions.put(definition.id(), definition);
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
        lore.add("§7Right click to activate.");
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
}
