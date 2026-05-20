package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) player.sendMessage("");
            player.sendMessage(Text.color("&b&lChat has been cleared by &e" + sender.getName() + "&b!"));
        }
        return true;
    }
}
