package com.testrank.hcf.core.util;

import org.bukkit.entity.Player;

public final class Title {
    private Title() {}

    public static void send(Player player, String title, String subtitle) {
        try {
            player.getClass().getMethod("sendTitle", String.class, String.class).invoke(player, Text.color(title), Text.color(subtitle));
        } catch (ReflectiveOperationException ignored) {
            player.sendMessage(Text.color(title + " &7- " + subtitle));
        }
    }
}
