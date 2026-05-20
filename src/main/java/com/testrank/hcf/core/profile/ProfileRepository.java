package com.testrank.hcf.core.profile;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.testrank.hcf.core.mongo.MongoManager;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProfileRepository {
    private final MongoManager mongo;
    private final ProfileCodec codec = new ProfileCodec();
    private final ConcurrentMap<UUID, Profile> memory = new ConcurrentHashMap<>();

    public ProfileRepository(MongoManager mongo) {
        this.mongo = mongo;
    }

    public CompletableFuture<Profile> load(UUID uuid) {
        if (!mongo.enabled()) {
            return CompletableFuture.completedFuture(memory.computeIfAbsent(uuid, Profile::new));
        }
        return MongoManager.toFuture(mongo.collection("profiles").find(Filters.eq("uuid", uuid.toString())).first())
                .thenApply(document -> document == null ? new Profile(uuid) : codec.decode(document));
    }

    public CompletableFuture<Void> save(Profile profile) {
        if (!mongo.enabled()) {
            memory.put(profile.uuid(), profile);
            return CompletableFuture.completedFuture(null);
        }
        return MongoManager.toFuture(mongo.collection("profiles").replaceOne(
                Filters.eq("uuid", profile.uuid().toString()),
                codec.encode(profile),
                new ReplaceOptions().upsert(true)
        )).thenApply(ignored -> null);
    }

    public CompletableFuture<List<Profile>> loadAll() {
        if (!mongo.enabled()) {
            return CompletableFuture.completedFuture(List.copyOf(memory.values()));
        }
        return MongoManager.toListFuture(mongo.collection("profiles").find())
                .thenApply(documents -> documents.stream().map(codec::decode).toList());
    }
}
