package com.testrank.hcf.core.staff;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class ReportService implements HCFService {
    private final Deque<Report> reports = new ArrayDeque<>(128);

    public void report(Player reporter, Player target, String reason) {
        Report report = new Report(reporter.getUniqueId(), target.getUniqueId(), reason, System.currentTimeMillis());
        synchronized (reports) {
            if (reports.size() == 128) {
                reports.removeFirst();
            }
            reports.addLast(report);
        }
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("hcf.staff"))
                .forEach(player -> player.sendMessage("§c[Report] §f" + reporter.getName() + " §7reported §f" + target.getName() + "§7: §c" + reason));
    }

    public List<Report> recent() {
        synchronized (reports) {
            return List.copyOf(reports);
        }
    }

    public record Report(UUID reporter, UUID target, String reason, long createdAt) {}
}
