package com.testrank.hcf.core.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.testrank.hcf.core.particle.ParticleIntelService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

final class PacketEventsBridge implements AutoCloseable {
    private static final int MAX_MOVEMENT_PACKETS_PER_SECOND = 120;
    private static final int MAX_ARM_SWINGS_PER_SECOND = 18;

    private final Plugin plugin;
    private final PacketRateLimiter movementLimiter;
    private final PacketRateLimiter animationLimiter = new PacketRateLimiter();
    private final ParticleIntelService intel;
    private final PacketEventsAPI<?> api;
    private final PacketListenerCommon listener;

    PacketEventsBridge(Plugin plugin, PacketRateLimiter movementLimiter, ParticleIntelService intel) {
        this.plugin = plugin;
        this.movementLimiter = movementLimiter;
        this.intel = intel;
        this.api = PacketEvents.getAPI();
        if (api == null) {
            throw new IllegalStateException("PacketEvents API is not initialized");
        }
        this.listener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                PacketTypeCommon type = event.getPacketType();
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                UUID uuid = player.getUniqueId();
                if (isMovement(type) && !PacketEventsBridge.this.movementLimiter.allow(uuid, MAX_MOVEMENT_PACKETS_PER_SECOND)) {
                    event.setCancelled(true);
                    intel.packetAnomaly(uuid, player.getName(), "movement_rate_limit", String.valueOf(type), MAX_MOVEMENT_PACKETS_PER_SECOND);
                    return;
                }
                if (isAnimation(type) && !PacketEventsBridge.this.animationLimiter.allow(uuid, MAX_ARM_SWINGS_PER_SECOND)) {
                    intel.cpsSpike(uuid, player.getName(), MAX_ARM_SWINGS_PER_SECOND);
                }
            }

            @Override
            public void onUserDisconnect(UserDisconnectEvent event) {
                UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
                if (uuid != null) {
                    PacketEventsBridge.this.movementLimiter.remove(uuid);
                    PacketEventsBridge.this.animationLimiter.remove(uuid);
                }
            }
        };
        api.getEventManager().registerListener(listener);
    }

    private static boolean isMovement(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.VEHICLE_MOVE;
    }

    private static boolean isAnimation(PacketTypeCommon type) {
        return type == PacketType.Play.Client.ANIMATION;
    }

    @Override
    public void close() {
        try {
            api.getEventManager().unregisterListener(listener);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("PacketEvents listener unregister failed: " + exception.getMessage());
        }
    }
}
