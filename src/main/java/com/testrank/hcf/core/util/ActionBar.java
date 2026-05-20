package com.testrank.hcf.core.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ActionBar {
    private ActionBar() {}

    public static void send(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            String version = player.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> componentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Object component = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + json(Text.color(message)) + "\"}");
            Constructor<?> constructor = packetClass.getConstructor(componentClass, byte.class);
            Object packet = constructor.newInstance(component, (byte) 2);
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacket.invoke(connection, packet);
        } catch (ReflectiveOperationException ignored) {
            player.sendMessage(Text.color(message));
        }
    }

    private static String json(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
