package com.testrank.hcf.core.scoreboard;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.combat.AntiCleanService;
import com.testrank.hcf.core.combat.CombatService;
import com.testrank.hcf.core.events.EventService;
import com.testrank.hcf.core.events.HCFEvent;
import com.testrank.hcf.core.events.HCFEventType;
import com.testrank.hcf.core.koth.KothService;
import com.testrank.hcf.core.classes.PvpClassService;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.settings.PlayerSettingsService;
import com.testrank.hcf.core.sotw.SotwService;
import com.testrank.hcf.core.staff.StaffService;
import com.testrank.hcf.core.team.Team;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.timer.CooldownService;
import com.testrank.hcf.core.timer.GlobalTimerService;
import com.testrank.hcf.core.threading.TpsService;
import com.testrank.hcf.core.util.ActionBar;
import com.testrank.hcf.core.util.Position;
import com.testrank.hcf.core.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardService implements HCFService {
    private static final String TOP_BORDER = "&7&m--------------------";
    private static final String BOTTOM_BORDER = "&8&m--------------------";
    private static final long TITLE_CYCLE_MILLIS = 10_000L;

    private final Plugin plugin;
    private final ProfileService profiles;
    private final CombatService combat;
    private final AntiCleanService antiClean;
    private final CooldownService cooldowns;
    private final EventService events;
    private final KothService koths;
    private final GlobalTimerService globalTimers;
    private final SotwService sotw;
    private final StaffService staff;
    private final TpsService tps;
    private final TeamService teams;
    private final PvpClassService classes;
    private final PlayerSettingsService playerSettings;
    private final Map<UUID, List<String>> lastLines = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastTitles = new ConcurrentHashMap<>();
    private int taskId = -1;

    public ScoreboardService(Plugin plugin, ProfileService profiles, CombatService combat, AntiCleanService antiClean, CooldownService cooldowns,
                             EventService events, KothService koths, GlobalTimerService globalTimers, SotwService sotw, StaffService staff,
                             TpsService tps, TeamService teams, PvpClassService classes, PlayerSettingsService playerSettings) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.combat = combat;
        this.antiClean = antiClean;
        this.cooldowns = cooldowns;
        this.events = events;
        this.koths = koths;
        this.globalTimers = globalTimers;
        this.sotw = sotw;
        this.staff = staff;
        this.tps = tps;
        this.teams = teams;
        this.classes = classes;
        this.playerSettings = playerSettings;
    }

    @Override
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L).getTaskId();
    }

    private void tick() {
        java.util.Set<UUID> online = new java.util.HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
            render(player);
            actionBar(player);
        }
        lastLines.keySet().removeIf(uuid -> !online.contains(uuid));
        lastTitles.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    private void render(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        List<String> lines = lines(player);
        Objective objective = board.getObjective("hcf");
        if (lines.isEmpty()) {
            if (objective != null || lastLines.remove(player.getUniqueId()) != null) {
                clear(board);
            }
            lastTitles.remove(player.getUniqueId());
            if (objective != null) {
                objective.unregister();
            }
            return;
        }
        String title = Text.color(title());
        if (objective == null) {
            objective = board.registerNewObjective("hcf", "dummy");
            objective.setDisplayName(title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else if (!title.equals(lastTitles.get(player.getUniqueId()))) {
            objective.setDisplayName(title);
        }
        List<String> rendered = new ArrayList<>(Math.min(15, lines.size() + 2));
        rendered.add(TOP_BORDER);
        rendered.addAll(lines.size() > 13 ? lines.subList(0, 13) : lines);
        rendered.add(BOTTOM_BORDER);
        List<String> colored = new ArrayList<>(rendered.size());
        int unique = 0;
        for (String line : rendered) {
            colored.add(Text.color(uniqueLine(line, unique++)));
        }
        if (colored.equals(lastLines.get(player.getUniqueId())) && title.equals(lastTitles.get(player.getUniqueId()))) {
            return;
        }
        clear(board);
        int score = 15;
        for (String line : colored) {
            objective.getScore(line).setScore(score--);
        }
        lastLines.put(player.getUniqueId(), colored);
        lastTitles.put(player.getUniqueId(), title);
    }

    private String title() {
        long elapsed = System.currentTimeMillis() % TITLE_CYCLE_MILLIS;
        int redLetters = (int) (elapsed / (TITLE_CYCLE_MILLIS / 4L));
        if (redLetters > 3) {
            redLetters = 3;
        }
        char[] chars = {'H', 'C', 'F'};
        StringBuilder title = new StringBuilder();
        for (int index = 0; index < chars.length; index++) {
            title.append(index < redLetters ? "&c&l" : "&f&l").append(chars[index]);
        }
        return title.toString();
    }

    private List<String> lines(Player player) {
        List<String> lines = new ArrayList<>(15);

        globalTimers.activeTimers().forEach(timer -> {
            String lower = timer.displayName().toLowerCase(Locale.ROOT);
            if (lower.contains("outpost") && !playerSettings.enabled(player.getUniqueId(), "outpost-scoreboard")) {
                return;
            }
            if (lower.contains("sale") && !playerSettings.enabled(player.getUniqueId(), "sale-timer-scoreboard")) {
                return;
            }
            addTimer(lines, "&a" + timer.displayName(), timer.remaining());
        });

        if (sotw.active()) {
            addTimer(lines, sotw.pvpEnabled(player.getUniqueId()) ? "&c&mSOTW Timer" : "&aSOTW Timer", sotw.remaining());
        }

        for (KothService.KothDisplay koth : koths.activeDisplays()) {
            lines.add("&c&l" + koth.name());
            lines.add("&7Coords&7: &f" + ((koth.claim().minX() + koth.claim().maxX()) / 2) + ", " + ((koth.claim().minZ() + koth.claim().maxZ()) / 2));
            addTimer(lines, "&cCapture", koth.captureRemaining());
        }

        for (HCFEvent event : events.events()) {
            if (event.active() && event.type() != HCFEventType.KOTH) {
                addTimer(lines, "&c" + event.name(), event.endsAt() - System.currentTimeMillis());
            }
        }

        profiles.cached(player.getUniqueId()).ifPresent(profile -> {
            profile.cooldowns().forEach((key, value) -> {
                long remaining = value - System.currentTimeMillis();
                if (remaining > 0L && shownCooldown(player, key)) {
                    addTimer(lines, "&c" + cooldownName(key), remaining);
                }
            });
            for (Map.Entry<String, Long> entry : profile.timers().entrySet()) {
                long remaining = entry.getValue() - System.currentTimeMillis();
                if (remaining > 0L) {
                    addTimer(lines, "&a" + displayName(entry.getKey()), remaining);
                }
            }
        });

        combat.tag(player.getUniqueId()).ifPresent(tag ->
                addTimer(lines, "&cCombat Tag", tag.expiresAt() - System.currentTimeMillis()));

        if (playerSettings.enabled(player.getUniqueId(), "teamfight-scoreboard")) {
            antiClean.fight(player.getUniqueId()).ifPresent(fight ->
                    addTimer(lines, "&cAntiClean", fight.remaining()));
        }

        long warmup = classes.warmupRemaining(player.getUniqueId());
        if (warmup > 0L) {
            addTimer(lines, "&cClass Warmup", warmup);
        }

        focusedSection(player, lines);

        if (staff.staffMode(player)) {
            lines.add("&7&m--------------------");
            lines.add("&cStaff Mode&7: &fEnabled");
            lines.add("&cVanished&7: " + (staff.vanished(player) ? "&aEnabled" : "&cDisabled"));
            lines.add("&cStaff Online&7: &f" + staff.staffOnline());
            if (player.isOp()) {
                lines.add("&cTPS&7: &f" + String.format(Locale.US, "%.2f", tps.tps()));
            }
        }

        return lines;
    }

    private void focusedSection(Player player, List<String> lines) {
        Team team = teams.byPlayer(player.getUniqueId()).orElse(null);
        if (team == null || team.focused() == null) {
            return;
        }
        Team focused = teams.byPlayer(team.focused()).orElse(null);
        if (focused == null) {
            Player target = Bukkit.getPlayer(team.focused());
            if (target != null) {
                focused = teams.byPlayer(target.getUniqueId()).orElse(null);
            }
        }
        if (focused == null) {
            return;
        }
        lines.add("&7&m--------------------");
        lines.add("&cTeam&7: &f" + focused.name());
        lines.add("&cHome&7: &f" + location(focused.hq()));
        lines.add("&cDTR&7: &f" + dtrText(player, focused.dtr()));
        lines.add("&cOnline&7: &f" + online(focused) + "/" + focused.members().size());
    }

    private void actionBar(Player player) {
        if (staff.staffMode(player)) {
            ActionBar.send(player, "&cStaff Mode &8| &fVanished: " + (staff.vanished(player) ? "&aEnabled" : "&cDisabled"));
            return;
        }
        long pearl = cooldowns.remaining(player.getUniqueId(), "enderpearl");
        if (pearl > 0L) {
            ActionBar.send(player, "&cEnderpearl Cooldown&7: &f" + formatDuration(pearl));
        }
    }

    private static void addTimer(List<String> lines, String label, long remainingMillis) {
        lines.add(label + "&7: &f" + formatDuration(remainingMillis));
    }

    private static String displayName(String key) {
        if (key.equalsIgnoreCase("pvp_timer")) {
            return "PvP Timer";
        }
        String[] words = key.replace('_', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.length() == 0 ? "Timer" : builder.toString();
    }

    private boolean shownCooldown(Player player, String key) {
        if ((key.startsWith("ability:") || key.startsWith("partner:"))
                && !playerSettings.enabled(player.getUniqueId(), "ability-cooldown-scoreboard")) {
            return false;
        }
        return key.equalsIgnoreCase("enderpearl") || key.equalsIgnoreCase("rod") || key.startsWith("ability:") || key.startsWith("partner:");
    }

    private static String cooldownName(String key) {
        if (key.equalsIgnoreCase("enderpearl")) {
            return "Enderpearl";
        }
        if (key.equalsIgnoreCase("rod")) {
            return "Rod";
        }
        if (key.startsWith("ability:")) {
            return "Ability " + displayName(key.substring("ability:".length()));
        }
        if (key.startsWith("partner:")) {
            return "Ability " + displayName(key.substring("partner:".length()));
        }
        return displayName(key);
    }

    private static String location(Position position) {
        return position == null ? "None" : (int) position.x() + ", " + (int) position.y() + ", " + (int) position.z();
    }

    private String dtrText(Player player, double dtr) {
        if (!playerSettings.enabled(player.getUniqueId(), "dtr-hearts")) {
            return String.format(Locale.US, "%.1f", dtr);
        }
        return String.format(Locale.US, "%.2f", dtr) + " \u2764";
    }

    private static int online(Team team) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.isMember(player.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private static void clear(Scoreboard board) {
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
    }

    private static String uniqueLine(String line, int unique) {
        String[] suffixes = {"&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e"};
        return line + suffixes[unique % suffixes.length];
    }

    @Override
    public void close() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        lastLines.clear();
        lastTitles.clear();
    }
}
