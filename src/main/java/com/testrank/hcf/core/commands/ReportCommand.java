package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.staff.ReportService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class ReportCommand implements CommandExecutor {
    private final ReportService reports;

    public ReportCommand(ReportService reports) {
        this.reports = reports;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Text.color("&c/report <player> <reason>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Text.color("&cPlayer not found."));
            return true;
        }
        reports.report(player, target, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        player.sendMessage(Text.color("&aReport sent to online staff."));
        return true;
    }
}
