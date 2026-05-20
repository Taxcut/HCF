package com.testrank.hcf.core.packets;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.lunar.ClientIntegrationService;
import org.bukkit.plugin.Plugin;

public final class PacketHookService implements HCFService {
    private final Plugin plugin;
    private final ClientIntegrationService clients;
    private final PacketRateLimiter movementLimiter = new PacketRateLimiter();
    private AutoCloseable bridge;

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
        try {
            bridge = new PacketEventsBridge(plugin, movementLimiter);
            plugin.getLogger().info("PacketEvents listener registered; movement packet limiting is active.");
        } catch (LinkageError | RuntimeException exception) {
            bridge = null;
            plugin.getLogger().warning("PacketEvents detected but could not be initialized: " + exception.getMessage());
        }
    }

    public PacketRateLimiter movementLimiter() {
        return movementLimiter;
    }

    @Override
    public void close() {
        if (bridge != null) {
            try {
                bridge.close();
            } catch (Exception exception) {
                plugin.getLogger().warning("PacketEvents bridge shutdown failed: " + exception.getMessage());
            }
        }
    }
}
