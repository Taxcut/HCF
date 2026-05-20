package com.testrank.hcf.core.combat;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CombatService implements HCFService {
    private final HCFSettings settings;
    private final ConcurrentMap<UUID, CombatTag> tags = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> lastHit = new ConcurrentHashMap<>();

    public CombatService(HCFSettings settings) {
        this.settings = settings;
    }

    public void tag(Player attacker, Player victim) {
        long expires = System.currentTimeMillis() + settings.combatTagSeconds() * 1000L;
        tags.put(attacker.getUniqueId(), new CombatTag(victim.getUniqueId(), expires, CombatTagType.PVP));
        tags.put(victim.getUniqueId(), new CombatTag(attacker.getUniqueId(), expires, CombatTagType.PVP));
        lastHit.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    public void archerTag(Player attacker, Player victim) {
        tags.put(victim.getUniqueId(), new CombatTag(attacker.getUniqueId(), System.currentTimeMillis() + settings.archerTagSeconds() * 1000L, CombatTagType.ARCHER));
    }

    public Optional<CombatTag> tag(UUID uuid) {
        CombatTag tag = tags.get(uuid);
        if (tag == null) {
            return Optional.empty();
        }
        if (!tag.active()) {
            tags.remove(uuid, tag);
            return Optional.empty();
        }
        return Optional.of(tag);
    }

    public Optional<UUID> lastHit(UUID victim) {
        return Optional.ofNullable(lastHit.get(victim));
    }

    public void clear(UUID uuid) {
        tags.remove(uuid);
        lastHit.remove(uuid);
    }
}
