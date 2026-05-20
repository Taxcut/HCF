package com.testrank.hcf.core.claim;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimSelectionService implements HCFService {
    public static final Material WAND_MATERIAL = Material.GOLD_HOE;
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public ItemStack wand() {
        ItemStack item = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bClaim Wand");
        item.setItemMeta(meta);
        return item;
    }

    public void first(Player player, Location location) {
        selections.merge(player.getUniqueId(), new Selection(location, null), (old, ignored) -> new Selection(location, old.second()));
    }

    public void second(Player player, Location location) {
        selections.merge(player.getUniqueId(), new Selection(null, location), (old, ignored) -> new Selection(old.first(), location));
    }

    public Optional<Selection> selection(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection == null || !selection.complete() ? Optional.empty() : Optional.of(selection);
    }

    public void clear(Player player) {
        selections.remove(player.getUniqueId());
    }

    public record Selection(Location first, Location second) {
        public boolean complete() {
            return first != null && second != null && first.getWorld() != null && first.getWorld().equals(second.getWorld());
        }
    }
}
