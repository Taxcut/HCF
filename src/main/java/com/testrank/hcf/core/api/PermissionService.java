package com.testrank.hcf.core.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Objects;

public final class PermissionService implements HCFService {
    private volatile boolean luckPermsAvailable;
    private volatile boolean vaultAvailable;
    private volatile boolean placeholderApiAvailable;

    @Override
    public void start() {
        luckPermsAvailable = Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
        vaultAvailable = Bukkit.getPluginManager().isPluginEnabled("Vault");
        placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public String primaryGroup(Player player) {
        if (!luckPermsAvailable) {
            return player.isOp() ? "operator" : "default";
        }
        try {
            Object provider = Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
            Object userManager = provider.getClass().getMethod("getUserManager").invoke(provider);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());
            return user == null ? "default" : (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
        } catch (ReflectiveOperationException exception) {
            return "default";
        }
    }

    public String chatPrefix(Player player) {
        String prefix = placeholder(player, "%particle_rank%");
        if (!prefix.isBlank()) {
            return prefix;
        }
        prefix = luckPermsPrefix(player);
        if (!prefix.isBlank()) {
            return prefix;
        }
        prefix = vaultPrefix(player);
        if (!prefix.isBlank()) {
            return prefix;
        }
        prefix = displayNamePrefix(player);
        if (!prefix.isBlank()) {
            return prefix;
        }
        String group = primaryGroup(player);
        if (group.equalsIgnoreCase("default")) {
            return "";
        }
        return "&8[&c" + pretty(group) + "&8]";
    }

    public String rankColor(Player player) {
        String color = placeholder(player, "%particle_rank_color%");
        return color.isBlank() ? "&f" : color;
    }

    public String particleRank(Player player) {
        return placeholder(player, "%particle_rank%");
    }

    public String placeholder(Player player, String placeholder) {
        if (!placeholderApiAvailable) {
            return "";
        }
        try {
            Object value = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                    .getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, placeholder);
            String text = value == null ? "" : String.valueOf(value).trim();
            return text.equals(placeholder) ? "" : text;
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }

    private String luckPermsPrefix(Player player) {
        if (!luckPermsAvailable) {
            return "";
        }
        try {
            Object provider = Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
            Object userManager = provider.getClass().getMethod("getUserManager").invoke(provider);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return "";
            }
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            Object prefix = metaData.getClass().getMethod("getPrefix").invoke(metaData);
            return prefix == null ? "" : String.valueOf(prefix).trim();
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }

    private String vaultPrefix(Player player) {
        if (!vaultAvailable) {
            return "";
        }
        try {
            Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            Object registration = Bukkit.getServicesManager().getRegistration(chatClass);
            if (registration == null) {
                return "";
            }
            Object provider = registration.getClass().getMethod("getProvider").invoke(registration);
            for (Method method : provider.getClass().getMethods()) {
                if (!method.getName().equals("getPlayerPrefix")) {
                    continue;
                }
                Object value = invokeVaultPrefix(method, provider, player);
                if (value != null && !String.valueOf(value).isBlank()) {
                    return String.valueOf(value).trim();
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "";
    }

    private Object invokeVaultPrefix(Method method, Object provider, Player player) throws ReflectiveOperationException {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length == 1 && parameters[0].isAssignableFrom(Player.class)) {
            return method.invoke(provider, player);
        }
        if (parameters.length == 2 && parameters[0] == String.class && parameters[1].isAssignableFrom(Player.class)) {
            return method.invoke(provider, player.getWorld().getName(), player);
        }
        if (parameters.length == 2 && parameters[0] == String.class && parameters[1] == String.class) {
            return method.invoke(provider, player.getWorld().getName(), player.getName());
        }
        return null;
    }

    private String displayNamePrefix(Player player) {
        String displayName = player.getDisplayName();
        if (displayName == null || displayName.equals(player.getName()) || !displayName.contains(player.getName())) {
            return "";
        }
        String prefix = displayName.substring(0, displayName.indexOf(player.getName())).trim();
        return Objects.equals(prefix, "") ? "" : prefix;
    }

    private static String pretty(String group) {
        String cleaned = group.replace('_', ' ').replace('-', ' ').trim();
        if (cleaned.isEmpty()) {
            return group;
        }
        StringBuilder builder = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
            }
        }
        return builder.toString();
    }
}
