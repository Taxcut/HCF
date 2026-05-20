package com.testrank.hcf.core.commands;

import com.testrank.hcf.core.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color("&cOnly players can use this command."));
            return true;
        }
        player.sendMessage(Text.color("&8&m----------------&8[ &c&lHCF Help &8]&8&m----------------"));
        player.sendMessage(Text.color("&bNeed help? &fHere are some useful commands:"));
        player.sendMessage(Text.color("&e/report &7- &fReport a player or issue."));
        player.sendMessage(Text.color("&e/request &7- &fRequest staff assistance."));
        player.sendMessage(Text.color("&e/gamemode &7- &fChange your game mode (staff only)."));
        player.sendMessage(Text.color("&e/balance &7- &fCheck your balance."));
        player.sendMessage(Text.color("&e/team &7- &fTeam management commands."));
        player.sendMessage(Text.color("&e/spawn &7- &fTeleport to spawn."));
        player.sendMessage(Text.color("&e/staff &7- &fStaff utilities."));
        player.sendMessage(Text.color("&e/kit &7- &fClaim your kits."));
        player.sendMessage(Text.color("&e/stats &7- &fView your stats."));
        player.sendMessage(Text.color("&7Type &e/help <command> &7for more info!"));
        return true;
    }
}
