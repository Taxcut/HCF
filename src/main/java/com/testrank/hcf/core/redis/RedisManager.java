package com.testrank.hcf.core.redis;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class RedisManager implements HCFService {
    private final String serverId;
    private final boolean enabled;
    private final RedisClient client;
    private final ExecutorService connector;
    private final Map<String, List<Consumer<RedisEnvelope>>> handlers = new ConcurrentHashMap<>();
    private volatile boolean available;
    private volatile boolean closing;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    public RedisManager(HCFSettings settings) {
        this.serverId = settings.serverId();
        this.enabled = settings.redisEnabled();
        this.client = enabled ? RedisClient.create(settings.redisUri()) : null;
        this.connector = enabled ? Executors.newSingleThreadExecutor(daemon("hcf-redis-connect")) : null;
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        CompletableFuture.runAsync(this::connect, connector)
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    available = false;
                    Logger.getLogger("HCF").warning("Redis connection did not complete within 5 seconds. Redis sync is disabled unless it connects later.");
                    return null;
                });
    }

    private void connect() {
        try {
            this.connection = client.connect();
            this.pubSubConnection = client.connectPubSub();
            this.pubSubConnection.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    List<Consumer<RedisEnvelope>> consumers = handlers.get(channel);
                    if (consumers == null || consumers.isEmpty()) {
                        return;
                    }
                    RedisEnvelope envelope = RedisEnvelope.parse(message);
                    if (serverId.equals(envelope.sourceServer())) {
                        return;
                    }
                    for (Consumer<RedisEnvelope> consumer : consumers) {
                        consumer.accept(envelope);
                    }
                }
            });
            if (closing) {
                close();
                return;
            }
            available = true;
            for (String channel : handlers.keySet()) {
                pubSubConnection.async().subscribe(channel).exceptionally(throwable -> {
                    Logger.getLogger("HCF").warning("Redis deferred subscribe failed for " + channel + ": " + throwable.getMessage());
                    return null;
                });
            }
            Logger.getLogger("HCF").info("Redis connection established.");
        } catch (RuntimeException exception) {
            available = false;
            Logger.getLogger("HCF").warning("Redis is enabled but the connection failed. Redis sync is disabled for this session: " + exception.getMessage());
        }
    }

    public String serverId() {
        return serverId;
    }

    public CompletableFuture<Void> publish(String channel, String payload) {
        return publishTo("hcf:" + channel, serverId + "|" + payload, channel);
    }

    public CompletableFuture<Void> publishRaw(String channel, String payload) {
        return publishTo(channel, payload, channel);
    }

    private CompletableFuture<Void> publishTo(String redisChannel, String payload, String logChannel) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        RedisAsyncCommands<String, String> commands = connection.async();
        return commands.publish(redisChannel, payload).thenAccept(ignored -> {}).toCompletableFuture()
                .exceptionally(throwable -> {
                    Logger.getLogger("HCF").warning("Redis publish failed on channel " + logChannel + ": " + throwable.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> hset(String key, String field, String value) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return connection.async().hset(key, field, value).thenAccept(ignored -> {}).toCompletableFuture()
                .exceptionally(throwable -> {
                    Logger.getLogger("HCF").warning("Redis hset failed for " + key + ": " + throwable.getMessage());
                    return null;
                });
    }

    public CompletableFuture<String> hget(String key, String field) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return connection.async().hget(key, field).toCompletableFuture()
                .exceptionally(throwable -> {
                    Logger.getLogger("HCF").warning("Redis hget failed for " + key + ": " + throwable.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> subscribe(String channel, Consumer<RedisEnvelope> consumer) {
        String namespaced = "hcf:" + channel;
        handlers.computeIfAbsent(namespaced, ignored -> new CopyOnWriteArrayList<>()).add(consumer);
        if (!available || pubSubConnection == null) {
            return CompletableFuture.completedFuture(null);
        }
        return pubSubConnection.async().subscribe(namespaced).thenAccept(ignored -> {}).toCompletableFuture()
                .exceptionally(throwable -> {
                    Logger.getLogger("HCF").warning("Redis subscribe failed for " + channel + ": " + throwable.getMessage());
                    return null;
                });
    }

    @Override
    public void close() {
        closing = true;
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
        if (connector != null) {
            connector.shutdownNow();
        }
    }

    public boolean enabled() {
        return enabled && available;
    }

    private static ThreadFactory daemon(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
