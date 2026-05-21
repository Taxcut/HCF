package com.testrank.hcf.core.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Indexes;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class MongoManager implements HCFService {
    private final MongoClient client;
    private final MongoDatabase database;
    private final boolean enabled;

    public MongoManager(HCFSettings settings) {
        this.enabled = settings.mongoEnabled();
        if (!enabled) {
            this.client = null;
            this.database = null;
            return;
        }
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(settings.mongoUri()))
                .retryWrites(true)
                .build();
        this.client = MongoClients.create(clientSettings);
        this.database = client.getDatabase(settings.mongoDatabase());
    }

    public MongoCollection<Document> collection(String name) {
        if (!enabled) {
            throw new IllegalStateException("MongoDB is disabled.");
        }
        return database.getCollection(name);
    }

    public boolean verifyConnection(long timeout, TimeUnit unit) {
        if (!enabled) {
            return false;
        }
        try {
            toFuture(database.runCommand(new Document("ping", 1))).orTimeout(timeout, unit).join();
            return true;
        } catch (RuntimeException exception) {
            Logger.getLogger("HCF").warning("MongoDB startup verification failed: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        toFuture(database.runCommand(new Document("ping", 1)))
                .orTimeout(3, TimeUnit.SECONDS)
                .thenAccept(ignored -> Logger.getLogger("HCF").info("MongoDB connection verified."))
                .exceptionally(throwable -> {
                    Logger.getLogger("HCF").warning("MongoDB is enabled but the connection could not be verified: " + throwable.getMessage());
                    return null;
                });
        index("profiles", "uuid");
        index("teams", "id");
        index("teams", "nameLower");
        index("claims", "world");
        index("cooldowns", "owner");
        index("timers", "owner");
        index("events", "type");
        index("logs", "createdAt");
        index("partner_items", "id");
    }

    public static <T> CompletableFuture<T> toFuture(org.reactivestreams.Publisher<T> publisher) {
        SingleResultSubscriber<T> subscriber = new SingleResultSubscriber<>();
        publisher.subscribe(subscriber);
        return subscriber.future();
    }

    public static <T> CompletableFuture<java.util.List<T>> toListFuture(org.reactivestreams.Publisher<T> publisher) {
        ListResultSubscriber<T> subscriber = new ListResultSubscriber<>();
        publisher.subscribe(subscriber);
        return subscriber.future();
    }

    private void index(String collection, String key) {
        toFuture(collection(collection).createIndex(Indexes.ascending(key))).exceptionally(throwable -> {
            Logger.getLogger("HCF").warning("MongoDB index creation failed for " + collection + "." + key + ": " + throwable.getMessage());
            return null;
        });
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    public boolean enabled() {
        return enabled;
    }
}
