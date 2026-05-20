package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BroadcastCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Text.color("&cUsage: &e/broadcast <message>"));
            return true;
        }
        String message = String.join(" ", args);
        Bukkit.broadcastMessage(Text.color("&6&l[Broadcast] &f" + message));
        return true;
    }
}
