package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.util.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;

public final class CommandBlockListener implements Listener {
    private static final Set<String> BLOCKED = Set.of("me", "pl", "plugins");

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() <= 1) {
            return;
        }
        String root = message.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int namespace = root.indexOf(':');
        if (namespace >= 0) {
            root = root.substring(namespace + 1);
        }
        if (!BLOCKED.contains(root)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Text.color("&8[&cHCF&8] &cThat command is hidden on this server."));
    }
}
