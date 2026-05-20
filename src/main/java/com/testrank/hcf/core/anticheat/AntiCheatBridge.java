package com.testrank.hcf.core.anticheat;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiCheatBridge implements HCFService {
    private final Set<UUID> exempt = ConcurrentHashMap.newKeySet();

    public void exempt(Player player, boolean value) {
        if (value) {
            exempt.add(player.getUniqueId());
        } else {
            exempt.remove(player.getUniqueId());
        }
    }

    public boolean exempt(Player player) {
        return exempt.contains(player.getUniqueId());
    }
}
