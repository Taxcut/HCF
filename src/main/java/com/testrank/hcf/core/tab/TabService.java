package com.testrank.hcf.core.tab;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.claim.Claim;
import com.testrank.hcf.core.claim.ClaimService;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEvent;
import com.testrank.hcf.core.events.HCFEventType;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.TpsService;
import com.testrank.hcf.core.util.Position;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TabService implements HCFService {
    private static final int ROWS = 20;
    private static final int COLUMNS = 4;
    private static final int SLOT_COUNT = ROWS * COLUMNS;

    private final Plugin plugin;
    private final TeamService teams;
    private final TpsService tps;
    private final ProfileService profiles;
    private final ClaimService claims;
    private final EventService events;
    private final Map<UUID, TabSession> sessions = new ConcurrentHashMap<>();
    private TabPacketAdapter packets;
    private int taskId = -1;

    public TabService(Plugin plugin, TeamService teams, TpsService tps, ProfileService profiles, ClaimService claims, EventService events) {
        this.plugin = plugin;
        this.teams = teams;
        this.tps = tps;
        this.profiles = profiles;
        this.claims = claims;
        this.events = events;
    }

    @Override
    public void start() {
        this.packets = TabPacketAdapter.create();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
    }

    private void tick() {
        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            online.add(viewer.getUniqueId());
            if (packets == null || !packets.supported()) {
                fallback(viewer);
                continue;
            }
            TabSession session = sessions.computeIfAbsent(viewer.getUniqueId(), uuid -> new TabSession(viewer.getUniqueId()));
            List<TabLine> layout = layout(viewer);
            packets.removeRealPlayers(viewer);
            if (!session.initialized) {
                packets.add(viewer, session.entries, layout);
                session.initialized = true;
            } else {
                packets.update(viewer, session.entries, layout);
            }
            packets.headerFooter(viewer, "", "");
        }
        sessions.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    private List<TabLine> layout(Player player) {
        List<TabLine> lines = new ArrayList<>(SLOT_COUNT);
        for (int index = 0; index < SLOT_COUNT; index++) {
            lines.add(new TabLine(" ", 0));
        }

        Team ownTeam = teams.byPlayer(player.getUniqueId()).orElse(null);
        Location location = player.getLocation();
        int kills = profiles.cached(player.getUniqueId()).map(profile -> profile.kills()).orElse(0);
        int deaths = profiles.cached(player.getUniqueId()).map(profile -> profile.deaths()).orElse(0);

        set(lines, 0, 0, "&cHome&7:");
        set(lines, 0, 1, ownTeam == null ? "&fNone" : "&f" + compact(ownTeam.hq()));
        set(lines, 0, 3, "&cTeam Info&7:");
        set(lines, 0, 4, "&fDTR&7: &a" + (ownTeam == null ? "0.00" : String.format(Locale.US, "%.2f", Math.max(0.0D, ownTeam.dtr()))));
        set(lines, 0, 5, "&fOnline&7: &a" + (ownTeam == null ? "0/0" : online(ownTeam) + "/" + ownTeam.members().size()));
        set(lines, 0, 6, "&fBalance&7: &a$" + (ownTeam == null ? 0L : (long) ownTeam.balance()));
        set(lines, 0, 8, "&cStatistics&7:");
        set(lines, 0, 9, "&fKills&7: &f" + kills);
        set(lines, 0, 10, "&fDeaths&7: &f" + deaths);
        set(lines, 0, 12, "&cYour Location&7:");
        set(lines, 0, 13, locationName(player, ownTeam));
        set(lines, 0, 14, "&f" + location.getBlockX() + ", " + location.getBlockZ() + " &7[" + direction(location.getYaw()) + "]");

        set(lines, 1, 0, "&c&lHCF");
        set(lines, 1, 2, ownTeam == null ? "&7No Faction" : topMarker(ownTeam) + "&c" + trim(ownTeam.name(), 16));
        set(lines, 1, 3, "&a" + trim(player.getName(), 14) + " &a▂▃▅", ping(player));

        set(lines, 2, 0, "&cEnd Portals&7:");
        set(lines, 2, 1, "&f1500, 1500");
        set(lines, 2, 2, "&fin each quadrant");
        set(lines, 2, 4, "&cMap Kit&7:");
        set(lines, 2, 5, "&fProt 2, Sharp 2");
        set(lines, 2, 7, "&cBorder&7:");
        set(lines, 2, 8, "&f3000");
        set(lines, 2, 10, "&cPlayers&7:");
        set(lines, 2, 11, "&f" + Bukkit.getOnlinePlayers().size());
        set(lines, 2, 13, "&cNext KOTH&7:");
        set(lines, 2, 14, nextKoth());

        set(lines, 3, 0, "&cTeam List&7:");
        List<Team> topTeams = teams.teams().stream()
                .filter(team -> online(team) > 0 || team.points() > 0 || team.balance() > 0.0D)
                .sorted(Comparator.<Team>comparingInt(this::online).thenComparingInt(Team::points).thenComparingDouble(Team::balance).reversed())
                .limit(19)
                .toList();
        if (topTeams.isEmpty()) {
            set(lines, 3, 1, "&7No teams online");
        } else {
            for (int index = 0; index < topTeams.size(); index++) {
                Team team = topTeams.get(index);
                String color = ownTeam != null && ownTeam.id().equals(team.id()) ? "&a" : "&7";
                set(lines, 3, index + 1, topMarker(team) + color + trim(team.name(), 13) + " &7(" + online(team) + ")");
            }
        }

        return lines;
    }

    private void set(List<TabLine> lines, int column, int row, String text) {
        set(lines, column, row, text, 0);
    }

    private void set(List<TabLine> lines, int column, int row, String text, int ping) {
        lines.set(column * ROWS + row, new TabLine(Text.color(text), ping));
    }

    private String locationName(Player player, Team ownTeam) {
        Optional<Claim> claim = claims.at(player.getLocation());
        if (claim.isEmpty()) {
            return "&aWilderness";
        }
        Team owner = teams.teams().stream().filter(team -> team.id().equals(claim.get().owner())).findFirst().orElse(null);
        if (owner != null && ownTeam != null && owner.id().equals(ownTeam.id())) {
            return "&a" + trim(claim.get().name(), 16);
        }
        if (owner != null) {
            return "&c" + trim(claim.get().name(), 16);
        }
        return "&a" + trim(claim.get().name(), 16);
    }

    private String nextKoth() {
        Optional<HCFEvent> active = events.active(HCFEventType.KOTH);
        if (active.isPresent()) {
            return "&f" + trim(active.get().name(), 16);
        }
        return "&f/koth schedule";
    }

    private String topMarker(Team team) {
        int rank = teams.teams().stream()
                .sorted(Comparator.<Team>comparingInt(Team::points).thenComparingDouble(Team::balance).reversed())
                .toList()
                .indexOf(team) + 1;
        return switch (rank) {
            case 1 -> "&6➊ ";
            case 2 -> "&f➋ ";
            case 3 -> "&9➌ ";
            default -> "";
        };
    }

    private int online(Team team) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.isMember(player.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

    private String compact(Position position) {
        if (position == null) {
            return "None";
        }
        return (int) position.x() + ", " + (int) position.y() + ", " + (int) position.z();
    }

    private String direction(float yaw) {
        float wrapped = (yaw % 360.0F + 360.0F) % 360.0F;
        if (wrapped >= 45.0F && wrapped < 135.0F) {
            return "W";
        }
        if (wrapped >= 135.0F && wrapped < 225.0F) {
            return "N";
        }
        if (wrapped >= 225.0F && wrapped < 315.0F) {
            return "E";
        }
        return "S";
    }

    private int ping(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private void fallback(Player player) {
        Team team = teams.byPlayer(player.getUniqueId()).orElse(null);
        String faction = team == null ? "" : "&8[&c" + trim(team.name(), 8) + "&8] ";
        player.setPlayerListName(Text.color(faction + "&a" + trim(player.getName(), 16)));
    }

    private static String trim(String input, int limit) {
        return input.length() <= limit ? input : input.substring(0, limit);
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        if (packets != null && packets.supported()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                TabSession session = sessions.remove(player.getUniqueId());
                if (session != null && session.initialized) {
                    packets.remove(player, session.entries);
                }
            }
        }
    }

    private record TabLine(String text, int ping) {}

    private static final class TabSession {
        private final List<TabEntry> entries = new ArrayList<>(SLOT_COUNT);
        private boolean initialized;

        private TabSession(UUID viewerId) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                String name = String.format(Locale.US, "HCF%02d", slot);
                UUID uuid = UUID.nameUUIDFromBytes(("hcf-tab:" + viewerId + ":" + slot).getBytes(StandardCharsets.UTF_8));
                entries.add(new TabEntry(uuid, name));
            }
        }
    }

    private record TabEntry(UUID uuid, String profileName) {}

    private static final class TabPacketAdapter {
        private final Class<?> packetInfoClass;
        private final Class<?> actionClass;
        private final Class<?> dataClass;
        private final Class<?> gameProfileClass;
        private final Class<?> gameModeClass;
        private final Class<?> componentClass;
        private final Class<?> packetClass;
        private final Class<?> headerFooterClass;
        private final Field actionField;
        private final Field dataListField;
        private final Field footerField;
        private final Constructor<?> dataConstructor;
        private final Constructor<?> gameProfileConstructor;
        private final Method serializer;
        private final Method sendPacket;
        private final Object survivalMode;
        private final boolean innerDataConstructor;
        private final boolean supported;

        private TabPacketAdapter(Class<?> packetInfoClass, Class<?> actionClass, Class<?> dataClass, Class<?> gameProfileClass,
                                 Class<?> gameModeClass, Class<?> componentClass, Class<?> packetClass, Class<?> headerFooterClass,
                                 Field actionField, Field dataListField, Field footerField, Constructor<?> dataConstructor,
                                 Constructor<?> gameProfileConstructor, Method serializer, Method sendPacket, Object survivalMode,
                                 boolean innerDataConstructor, boolean supported) {
            this.packetInfoClass = packetInfoClass;
            this.actionClass = actionClass;
            this.dataClass = dataClass;
            this.gameProfileClass = gameProfileClass;
            this.gameModeClass = gameModeClass;
            this.componentClass = componentClass;
            this.packetClass = packetClass;
            this.headerFooterClass = headerFooterClass;
            this.actionField = actionField;
            this.dataListField = dataListField;
            this.footerField = footerField;
            this.dataConstructor = dataConstructor;
            this.gameProfileConstructor = gameProfileConstructor;
            this.serializer = serializer;
            this.sendPacket = sendPacket;
            this.survivalMode = survivalMode;
            this.innerDataConstructor = innerDataConstructor;
            this.supported = supported;
        }

        private static TabPacketAdapter create() {
            try {
                String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                Class<?> packetInfoClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo");
                Class<?> actionClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
                Class<?> dataClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo$PlayerInfoData");
                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                Class<?> gameModeClass = Class.forName("net.minecraft.server." + version + ".WorldSettings$EnumGamemode");
                Class<?> componentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
                Class<?> serializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
                Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".Packet");
                Class<?> headerFooterClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerListHeaderFooter");

                Field actionField = firstField(packetInfoClass, actionClass);
                Field dataListField = firstField(packetInfoClass, List.class);
                Field footerField = headerFooterClass.getDeclaredField("b");
                actionField.setAccessible(true);
                dataListField.setAccessible(true);
                footerField.setAccessible(true);

                Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
                Constructor<?> dataConstructor = null;
                boolean innerDataConstructor = false;
                for (Constructor<?> constructor : dataClass.getDeclaredConstructors()) {
                    Class<?>[] parameters = constructor.getParameterTypes();
                    if (parameters.length == 5 && parameters[0] == packetInfoClass) {
                        dataConstructor = constructor;
                        innerDataConstructor = true;
                        break;
                    }
                    if (parameters.length == 4 && parameters[0] == gameProfileClass) {
                        dataConstructor = constructor;
                    }
                }
                if (dataConstructor == null) {
                    return unsupported();
                }
                dataConstructor.setAccessible(true);

                Method serializer = serializerClass.getMethod("a", String.class);
                Method sendPacket = null;
                Object connection = null;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Object handle = player.getClass().getMethod("getHandle").invoke(player);
                    connection = handle.getClass().getField("playerConnection").get(handle);
                    break;
                }
                if (connection != null) {
                    sendPacket = connection.getClass().getMethod("sendPacket", packetClass);
                }
                Object survivalMode = enumValue(gameModeClass, "SURVIVAL");
                return new TabPacketAdapter(packetInfoClass, actionClass, dataClass, gameProfileClass, gameModeClass, componentClass,
                        packetClass, headerFooterClass, actionField, dataListField, footerField, dataConstructor, gameProfileConstructor,
                        serializer, sendPacket, survivalMode, innerDataConstructor, true);
            } catch (ReflectiveOperationException exception) {
                return unsupported();
            }
        }

        private static TabPacketAdapter unsupported() {
            return new TabPacketAdapter(null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, false, false);
        }

        private boolean supported() {
            return supported;
        }

        private void add(Player viewer, List<TabEntry> entries, List<TabLine> lines) {
            send(viewer, "ADD_PLAYER", entries, lines);
        }

        private void update(Player viewer, List<TabEntry> entries, List<TabLine> lines) {
            send(viewer, "UPDATE_DISPLAY_NAME", entries, lines);
            send(viewer, "UPDATE_LATENCY", entries, lines);
        }

        private void remove(Player viewer, List<TabEntry> entries) {
            send(viewer, "REMOVE_PLAYER", entries, blankLines(entries.size()));
        }

        private void removeRealPlayers(Player viewer) {
            try {
                Object packet = packetInfoClass.getDeclaredConstructor().newInstance();
                actionField.set(packet, enumValue(actionClass, "REMOVE_PLAYER"));
                List<Object> list = dataList(packet);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    list.add(data(packet, profile(player), 0, null));
                }
                sendPacket(viewer, packet);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        private void headerFooter(Player viewer, String header, String footer) {
            try {
                Object headerComponent = component(header);
                Object footerComponent = component(footer);
                Object packet = headerFooterClass.getConstructor(componentClass).newInstance(headerComponent);
                footerField.set(packet, footerComponent);
                sendPacket(viewer, packet);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        private void send(Player viewer, String action, List<TabEntry> entries, List<TabLine> lines) {
            try {
                Object packet = packetInfoClass.getDeclaredConstructor().newInstance();
                actionField.set(packet, enumValue(actionClass, action));
                List<Object> list = dataList(packet);
                for (int index = 0; index < entries.size(); index++) {
                    TabEntry entry = entries.get(index);
                    TabLine line = lines.get(index);
                    Object profile = gameProfileConstructor.newInstance(entry.uuid(), entry.profileName());
                    Object display = action.equals("REMOVE_PLAYER") ? null : component(line.text());
                    list.add(data(packet, profile, line.ping(), display));
                }
                sendPacket(viewer, packet);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        @SuppressWarnings("unchecked")
        private List<Object> dataList(Object packet) throws IllegalAccessException {
            return (List<Object>) dataListField.get(packet);
        }

        private Object data(Object packet, Object profile, int ping, Object display) throws ReflectiveOperationException {
            if (innerDataConstructor) {
                return dataConstructor.newInstance(packet, profile, ping, survivalMode, display);
            }
            return dataConstructor.newInstance(profile, ping, survivalMode, display);
        }

        private Object profile(Player player) throws ReflectiveOperationException {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getMethod("getProfile").invoke(handle);
        }

        private Object component(String text) throws ReflectiveOperationException {
            return serializer.invoke(null, "{\"text\":\"" + json(text) + "\"}");
        }

        private void sendPacket(Player viewer, Object packet) throws ReflectiveOperationException {
            Object handle = viewer.getClass().getMethod("getHandle").invoke(viewer);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method method = sendPacket == null ? connection.getClass().getMethod("sendPacket", packetClass) : sendPacket;
            method.invoke(connection, packet);
        }

        private static Field firstField(Class<?> owner, Class<?> type) {
            for (Field field : owner.getDeclaredFields()) {
                if (type.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
            throw new IllegalStateException("Missing field " + type.getName());
        }

        private static Object enumValue(Class<?> enumClass, String name) {
            for (Object constant : enumClass.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(name)) {
                    return constant;
                }
            }
            return enumClass.getEnumConstants()[0];
        }

        private static List<TabLine> blankLines(int size) {
            List<TabLine> lines = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                lines.add(new TabLine(" ", 0));
            }
            return lines;
        }

        private static String json(String input) {
            return input.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
