package com.testrank.hcf.core.timer;

import com.testrank.hcf.core.api.HCFService;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GlobalTimerService implements HCFService {
    private final Map<String, GlobalTimer> timers = new ConcurrentHashMap<>();

    public GlobalTimer create(String name, long durationMillis) {
        String id = normalize(name);
        GlobalTimer timer = new GlobalTimer(id, prettify(name), System.currentTimeMillis() + durationMillis);
        timers.put(id, timer);
        return timer;
    }

    public void remove(String name) {
        timers.remove(normalize(name));
    }

    public Collection<GlobalTimer> activeTimers() {
        timers.values().removeIf(timer -> !timer.active());
        return timers.values();
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String prettify(String input) {
        String normalized = input.replace('_', ' ').trim();
        if (normalized.isEmpty()) {
            return "Timer";
        }
        String[] words = normalized.split("\\s+");
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
        return builder.toString();
    }
}
