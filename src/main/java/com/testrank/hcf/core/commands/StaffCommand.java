package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffCommand implements CommandExecutor {
    private final StaffService staff;

    public StaffCommand(StaffService staff) {
        this.staff = staff;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("hcf.staff")) {
            sender.sendMessage(Text.color("&cNo permission."));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("inspect")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(Text.color("&cPlayer not found."));
                return true;
            }
            player.openInventory(target.getInventory());
            player.sendMessage(Text.color("&eInspecting &f" + target.getName() + "&e."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("rtp")) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(target -> target != player && !target.hasPermission("hcf.staff"))
                    .findAny()
                    .ifPresentOrElse(target -> {
                        player.teleport(target.getLocation());
                        player.sendMessage(Text.color("&eTeleported to &f" + target.getName() + "&e."));
                    }, () -> player.sendMessage(Text.color("&cNo target available.")));
            return true;
        }
        boolean enabled = staff.toggleStaff(player);
        player.sendMessage(Text.color(enabled ? "&aStaff mode enabled." : "&cStaff mode disabled."));
        return true;
    }
}
