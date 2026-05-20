package com.testrank.hcf.core.packets;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import org.bukkit.plugin.Plugin;

public final class PacketHookService implements HCFService {
    private final Plugin plugin;
    private final ClientIntegrationService clients;
    private final PacketRateLimiter movementLimiter = new PacketRateLimiter();

    public PacketHookService(Plugin plugin, ClientIntegrationService clients) {
        this.plugin = plugin;
        this.clients = clients;
    }

    @Override
    public void start() {
        if (!clients.packetEventsAvailable()) {
            plugin.getLogger().info("PacketEvents was not detected; packet-level optimizations are disabled.");
            return;
        }
        plugin.getLogger().info("PacketEvents detected; HCF packet limiter is ready.");
    }

    public PacketRateLimiter movementLimiter() {
        return movementLimiter;
    }
}
