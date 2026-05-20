package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color("&cOnly players can use this command."));
            return true;
        }
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.sendMessage(Text.color("&aYou feel a warm light as your wounds vanish! &eYou are fully healed."));
        return true;
    }
}
