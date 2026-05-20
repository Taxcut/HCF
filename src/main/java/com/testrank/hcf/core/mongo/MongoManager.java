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

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
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
        toFuture(collection(collection).createIndex(Indexes.ascending(key))).exceptionally(throwable -> null);
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
