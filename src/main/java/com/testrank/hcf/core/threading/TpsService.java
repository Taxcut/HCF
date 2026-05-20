package com.testrank.hcf.core.threading;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class TpsService implements HCFService {
    private final Plugin plugin;
    private volatile long lastTick = System.nanoTime();
    private volatile double tps = 20.0D;
    private int taskId = -1;

    public TpsService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L).getTaskId();
    }

    public double tps() {
        return Math.min(20.0D, tps);
    }

    private void tick() {
        long now = System.nanoTime();
        long diff = now - lastTick;
        lastTick = now;
        if (diff <= 0L) {
            return;
        }
        double instant = 1_000_000_000.0D / diff;
        tps = (tps * 0.95D) + (instant * 0.05D);
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
