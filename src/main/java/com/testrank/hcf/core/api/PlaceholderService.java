package com.testrank.hcf.core.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderService implements HCFService {
    private volatile boolean placeholderApiAvailable;

    @Override
    public void start() {
        placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String apply(Player player, String input) {
        if (!placeholderApiAvailable) {
            return input;
        }
        try {
            Class<?> placeholders = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) placeholders.getMethod("setPlaceholders", Player.class, String.class).invoke(null, player, input);
        } catch (ReflectiveOperationException exception) {
            return input;
        }
    }
}
