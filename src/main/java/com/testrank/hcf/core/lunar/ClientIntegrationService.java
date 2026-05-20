package com.testrank.hcf.core.lunar;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientIntegrationService implements HCFService {
    private final Plugin plugin;
    private final Map<UUID, ClientBrand> brands = new ConcurrentHashMap<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private volatile boolean apolloAvailable;
    private volatile boolean packetEventsAvailable;

    public ClientIntegrationService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        apolloAvailable = classPresent("com.lunarclient.apollo.Apollo");
        packetEventsAvailable = classPresent("com.github.retrooper.packetevents.PacketEvents");
        if (apolloAvailable) {
            plugin.getLogger().info("Apollo detected; Lunar nametags, waypoints and staff modules are enabled.");
        } else {
            plugin.getLogger().info("Apollo was not detected; Lunar-only client hooks are disabled.");
        }
    }

    public void brand(Player player, ClientBrand brand) {
        brands.put(player.getUniqueId(), brand);
    }

    public ClientBrand brand(Player player) {
        return brands.getOrDefault(player.getUniqueId(), ClientBrand.VANILLA);
    }

    public boolean apolloAvailable() {
        return apolloAvailable;
    }

    public boolean packetEventsAvailable() {
        return packetEventsAvailable;
    }

    public void sendWaypoint(Player player, String id, String name, Location location, int color) {
        if (!apolloAvailable || location.getWorld() == null) {
            return;
        }
        try {
            Object apolloPlayer = apolloPlayer(player).orElse(null);
            if (apolloPlayer == null) {
                return;
            }
            Class<?> waypointModuleClass = Class.forName("com.lunarclient.apollo.module.waypoint.WaypointModule");
            Object module = module(waypointModuleClass);
            if (module == null) {
                return;
            }
            Class<?> blockLocationClass = Class.forName("com.lunarclient.apollo.common.location.ApolloBlockLocation");
            Object blockLocation = blockLocationClass.getMethod("builder").invoke(null);
            blockLocation.getClass().getMethod("world", String.class).invoke(blockLocation, location.getWorld().getName());
            blockLocation.getClass().getMethod("x", int.class).invoke(blockLocation, location.getBlockX());
            blockLocation.getClass().getMethod("y", int.class).invoke(blockLocation, location.getBlockY());
            blockLocation.getClass().getMethod("z", int.class).invoke(blockLocation, location.getBlockZ());
            Object builtLocation = blockLocation.getClass().getMethod("build").invoke(blockLocation);

            Class<?> waypointClass = Class.forName("com.lunarclient.apollo.module.waypoint.Waypoint");
            Object waypoint = waypointClass.getMethod("builder").invoke(null);
            waypoint.getClass().getMethod("name", String.class).invoke(waypoint, name);
            waypoint.getClass().getMethod("location", blockLocationClass).invoke(waypoint, builtLocation);
            waypoint.getClass().getMethod("color", Color.class).invoke(waypoint, new Color(color, true));
            waypoint.getClass().getMethod("preventRemoval", boolean.class).invoke(waypoint, false);
            waypoint.getClass().getMethod("hidden", boolean.class).invoke(waypoint, false);
            Object builtWaypoint = waypoint.getClass().getMethod("build").invoke(waypoint);
            module.getClass().getMethod("displayWaypoint", Class.forName("com.lunarclient.apollo.recipients.Recipients"), waypointClass)
                    .invoke(module, apolloPlayer, builtWaypoint);
        } catch (ReflectiveOperationException | LinkageError exception) {
            warnOnce("apollo-waypoint", "Could not send an Apollo waypoint. Lunar waypoint support may be disabled or incompatible.", exception);
        }
    }

    public void sendNametag(Player viewer, Player target, List<String> lines) {
        if (!apolloAvailable) {
            return;
        }
        try {
            Object apolloViewer = apolloPlayer(viewer).orElse(null);
            if (apolloViewer == null) {
                return;
            }
            Class<?> nametagModuleClass = Class.forName("com.lunarclient.apollo.module.nametag.NametagModule");
            Object module = module(nametagModuleClass);
            if (module == null) {
                return;
            }
            Class<?> nametagClass = Class.forName("com.lunarclient.apollo.module.nametag.Nametag");
            List<Object> components = components(nametagClass.getClassLoader(), lines);
            Object builder = nametagClass.getMethod("builder").invoke(null);
            builder.getClass().getMethod("lines", List.class).invoke(builder, components);
            Object nametag = builder.getClass().getMethod("build").invoke(builder);
            module.getClass().getMethod("overrideNametag", Class.forName("com.lunarclient.apollo.recipients.Recipients"), UUID.class, nametagClass)
                    .invoke(module, apolloViewer, target.getUniqueId(), nametag);
        } catch (ReflectiveOperationException | LinkageError exception) {
            warnOnce("apollo-nametag", "Could not send an Apollo nametag. Check that Apollo-Bukkit matches this server and includes Adventure components.", exception);
        }
    }

    public void staffModules(Player player, boolean enabled) {
        if (!apolloAvailable) {
            return;
        }
        try {
            Object apolloPlayer = apolloPlayer(player).orElse(null);
            if (apolloPlayer == null) {
                return;
            }
            Class<?> staffModuleClass = Class.forName("com.lunarclient.apollo.module.staffmod.StaffModModule");
            Object module = module(staffModuleClass);
            if (module == null) {
                return;
            }
            String method = enabled ? "enableAllStaffMods" : "disableAllStaffMods";
            module.getClass().getMethod(method, Class.forName("com.lunarclient.apollo.recipients.Recipients")).invoke(module, apolloPlayer);
        } catch (ReflectiveOperationException | LinkageError exception) {
            warnOnce("apollo-staff", "Could not toggle Apollo staff modules. Staff module support may be disabled or incompatible.", exception);
        }
    }

    public void sendNotification(Player player, String title, String message) {
        if (player.isOnline()) {
            player.sendMessage(title + " - " + message);
        }
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private Optional<?> apolloPlayer(Player player) throws ReflectiveOperationException {
        Class<?> apollo = Class.forName("com.lunarclient.apollo.Apollo");
        Object manager = apollo.getMethod("getPlayerManager").invoke(null);
        Object optional = manager.getClass().getMethod("getPlayer", UUID.class).invoke(manager, player.getUniqueId());
        return optional instanceof Optional<?> found ? found : Optional.empty();
    }

    private Object module(Class<?> moduleClass) throws ReflectiveOperationException {
        Class<?> apollo = Class.forName("com.lunarclient.apollo.Apollo");
        Object manager = apollo.getMethod("getModuleManager").invoke(null);
        Object module = manager.getClass().getMethod("getModule", Class.class).invoke(manager, moduleClass);
        if (module != null) {
            Object enabled = module.getClass().getMethod("isEnabled").invoke(module);
            if (!Boolean.TRUE.equals(enabled)) {
                module.getClass().getMethod("enable").invoke(module);
            }
        }
        return module;
    }

    private List<Object> components(ClassLoader classLoader, List<String> lines) throws ReflectiveOperationException {
        try {
            Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer", true, classLoader);
            Object serializer = serializerClass.getMethod("legacySection").invoke(null);
            Method deserialize = serializerClass.getMethod("deserialize", String.class);
            List<Object> components = new ArrayList<>(lines.size());
            for (String line : lines) {
                components.add(deserialize.invoke(serializer, line));
            }
            return components;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component", true, classLoader);
            Method text = componentClass.getMethod("text", String.class);
            List<Object> components = new ArrayList<>(lines.size());
            for (String line : lines) {
                components.add(text.invoke(null, ChatColor.stripColor(line)));
            }
            return components;
        }
    }

    private void warnOnce(String key, String message, Throwable throwable) {
        if (warned.add(key)) {
            plugin.getLogger().warning(message + " Cause: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
