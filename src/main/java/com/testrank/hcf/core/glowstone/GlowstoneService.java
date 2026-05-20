package com.testrank.hcf.core.glowstone;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEvent;
import com.testrank.hcf.core.events.HCFEventType;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowstoneService implements HCFService, Listener {
    private final Plugin plugin;
    private final EventService events;
    private final HCFSettings settings;
    private final Set<Location> broken = ConcurrentHashMap.newKeySet();
    private volatile int nextAnnounce = 15;
    private int taskId = -1;

    public GlowstoneService(Plugin plugin, EventService events, HCFSettings settings) {
        this.plugin = plugin;
        this.events = events;
        this.settings = settings;
    }

    @Override
    public void start() {
        long ticks = Math.max(15, settings.glowstoneResetMinutes()) * 60L * 20L;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::reset, ticks, ticks).getTaskId();
    }

    public HCFEvent start(long durationMillis) {
        HCFEvent event = events.create(HCFEventType.GLOWSTONE, "Glowstone Mountain");
        event.start(durationMillis);
        return event;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.GLOWSTONE) {
            return;
        }
        broken.add(event.getBlock().getLocation());
        int percent = Math.min(100, broken.size() * 100 / 300);
        if (percent >= nextAnnounce) {
            Bukkit.broadcastMessage(Text.color("&8[&6Glowstone Mountain&8] &e" + nextAnnounce + "% has been mined!"));
            nextAnnounce += 15;
        }
    }

    public void reset() {
        for (Location location : broken) {
            if (location.getWorld() != null) {
                location.getBlock().setType(Material.GLOWSTONE);
            }
        }
        broken.clear();
        nextAnnounce = 15;
        Bukkit.broadcastMessage(Text.color("&8[&6Glowstone Mountain&8] &fAll glowstone has been reset!"));
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
