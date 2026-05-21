package com.testrank.hcf.core.claim;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.testrank.hcf.core.mongo.MongoManager;
import org.bson.Document;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClaimRepository {
    private final MongoManager mongo;
    private final ConcurrentMap<UUID, Claim> memory = new ConcurrentHashMap<>();

    public ClaimRepository(MongoManager mongo) {
        this.mongo = mongo;
    }

    public CompletableFuture<Void> save(Claim claim) {
        if (!mongo.enabled()) {
            memory.put(claim.id(), claim);
            return CompletableFuture.completedFuture(null);
        }
        Document document = new Document("id", claim.id().toString())
                .append("owner", claim.owner() == null ? null : claim.owner().toString())
                .append("name", claim.name())
                .append("world", claim.world())
                .append("minX", claim.minX())
                .append("minZ", claim.minZ())
                .append("maxX", claim.maxX())
                .append("maxZ", claim.maxZ())
                .append("type", claim.type().name());
        return MongoManager.toFuture(mongo.collection("claims").replaceOne(
                Filters.eq("id", claim.id().toString()),
                document,
                new ReplaceOptions().upsert(true)
        )).thenApply(ignored -> null);
    }

    public CompletableFuture<List<Claim>> loadAll() {
        if (!mongo.enabled()) {
            return CompletableFuture.completedFuture(List.copyOf(memory.values()));
        }
        return MongoManager.toListFuture(mongo.collection("claims").find()).thenApply(documents -> documents.stream().map(this::decode).toList());
    }

    public CompletableFuture<Void> delete(UUID id) {
        if (!mongo.enabled()) {
            memory.remove(id);
            return CompletableFuture.completedFuture(null);
        }
        return MongoManager.toFuture(mongo.collection("claims").deleteOne(Filters.eq("id", id.toString()))).thenApply(ignored -> null);
    }

    private Claim decode(Document document) {
        String owner = document.getString("owner");
        return new Claim(
                UUID.fromString(document.getString("id")),
                owner == null ? null : UUID.fromString(owner),
                document.getString("name"),
                document.getString("world"),
                document.getInteger("minX"),
                document.getInteger("minZ"),
                document.getInteger("maxX"),
                document.getInteger("maxZ"),
                ClaimType.valueOf(document.getString("type"))
        );
    }
}
