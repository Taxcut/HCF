package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.util.Text;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color("&cOnly players can use this command."));
            return true;
        }
        if (!player.hasPermission("hcf.staff")) {
            player.sendMessage(Text.color("&cYou do not have permission to use this command."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Text.color("&cUsage: &e/gamemode <creative|survival|adventure|spectator>"));
            return true;
        }
        String mode = args[0].toLowerCase();
        GameMode gm;
        switch (mode) {
            case "creative", "c", "1" -> gm = GameMode.CREATIVE;
            case "survival", "s", "0" -> gm = GameMode.SURVIVAL;
            case "adventure", "a", "2" -> gm = GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> gm = GameMode.SPECTATOR;
            default -> {
                player.sendMessage(Text.color("&cUnknown gamemode: &e" + mode));
                return true;
            }
        }
        player.setGameMode(gm);
        player.sendMessage(Text.color("&aYour gamemode has been set to &e" + gm.name() + "&a! Enjoy your adventure!"));
        return true;
    }
}
