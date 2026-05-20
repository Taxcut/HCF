package com.testrank.hcf.core.classes;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.profile.ProfileService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PvpClassService implements HCFService {
    private final Plugin plugin;
    private final ProfileService profiles;
    private final HCFSettings settings;
    private final Map<UUID, String> active = new ConcurrentHashMap<>();
    private final Map<UUID, Warmup> warmups = new ConcurrentHashMap<>();
    private int taskId = -1;

    public PvpClassService(Plugin plugin, ProfileService profiles, HCFSettings settings) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.settings = settings;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L).getTaskId();
    }

    public void setClass(Player player, String classId) {
        profiles.cached(player.getUniqueId()).ifPresent(profile -> profile.pvpClass(classId));
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String detected = detect(player);
            String current = active.getOrDefault(player.getUniqueId(), "NONE");
            if (detected.equals("NONE")) {
                warmups.remove(player.getUniqueId());
                if (!current.equals("NONE")) {
                    clearEffects(player, current);
                    active.remove(player.getUniqueId());
                    setClass(player, "NONE");
                }
                continue;
            }
            if (detected.equals(current)) {
                applyEffects(player, detected);
                continue;
            }
            Warmup warmup = warmups.get(player.getUniqueId());
            long now = System.currentTimeMillis();
            if (warmup == null || !warmup.classId().equals(detected)) {
                warmups.put(player.getUniqueId(), new Warmup(detected, now + settings.classWarmupSeconds() * 1000L));
                continue;
            }
            if (warmup.endsAt() > now) {
                continue;
            }
            clearEffects(player, current);
            warmups.remove(player.getUniqueId());
            active.put(player.getUniqueId(), detected);
            setClass(player, detected);
            applyEffects(player, detected);
        }
    }

    public long warmupRemaining(UUID uuid) {
        Warmup warmup = warmups.get(uuid);
        if (warmup == null) {
            return 0L;
        }
        long remaining = warmup.endsAt() - System.currentTimeMillis();
        if (remaining <= 0L) {
            return 0L;
        }
        return remaining;
    }

    private String detect(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (matches(armor, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET) && settings.archerEnabled()) {
            return "ARCHER";
        }
        if (matches(armor, Material.GOLD_BOOTS, Material.GOLD_LEGGINGS, Material.GOLD_CHESTPLATE, Material.GOLD_HELMET) && settings.bardEnabled()) {
            return "BARD";
        }
        if (matches(armor, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET) && settings.rogueEnabled()) {
            return "ROGUE";
        }
        if (matches(armor, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET) && settings.ghostEnabled()) {
            return "GHOST";
        }
        return "NONE";
    }

    private void applyEffects(Player player, String classId) {
        switch (classId) {
            case "ARCHER" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 2, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 140, 1, true), true);
            }
            case "BARD" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 1, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 0, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 1, true), true);
            }
            case "ROGUE" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 2, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 140, 1, true), true);
            }
            case "GHOST" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 1, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 140, 0, true), true);
            }
            default -> {
            }
        }
    }

    private void clearEffects(Player player, String classId) {
        switch (classId) {
            case "ARCHER", "ROGUE" -> {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.JUMP);
            }
            case "BARD" -> {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }
            case "GHOST" -> {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            default -> {
            }
        }
    }

    private boolean matches(ItemStack[] armor, Material boots, Material leggings, Material chestplate, Material helmet) {
        return armor.length >= 4
                && armor[0] != null && armor[0].getType() == boots
                && armor[1] != null && armor[1].getType() == leggings
                && armor[2] != null && armor[2].getType() == chestplate
                && armor[3] != null && armor[3].getType() == helmet;
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private record Warmup(String classId, long endsAt) {}
}
