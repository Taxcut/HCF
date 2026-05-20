package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FreezeCommand implements CommandExecutor {
    private final StaffService staff;

    public FreezeCommand(StaffService staff) {
        this.staff = staff;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hcf.staff")) {
            sender.sendMessage(Text.color("&cNo permission."));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Text.color("&c/freeze <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Text.color("&cPlayer not found."));
            return true;
        }
        boolean next = !staff.frozen(target);
        staff.freeze(target, next);
        target.sendMessage(Text.color(next ? "&cYou have been frozen." : "&aYou have been unfrozen."));
        sender.sendMessage(Text.color("&e" + target.getName() + (next ? " frozen." : " unfrozen.")));
        return true;
    }
}
