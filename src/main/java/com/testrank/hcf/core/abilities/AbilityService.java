package com.testrank.hcf.core.abilities;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import com.testrank.hcf.core.timer.CooldownService;
import org.bukkit.entity.Player;

public final class AbilityService implements HCFService {
    private final CooldownService cooldowns;
    private final ClientIntegrationService clients;

    public AbilityService(CooldownService cooldowns, ClientIntegrationService clients) {
        this.cooldowns = cooldowns;
        this.clients = clients;
    }

    public boolean trigger(Player player, String ability, long cooldownMillis) {
        if (cooldowns.has(player.getUniqueId(), "ability:" + ability)) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), "ability:" + ability, cooldownMillis);
        clients.sendNotification(player, "Ability", ability + " activated");
        return true;
    }
}
