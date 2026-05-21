package com.testrank.hcf.core.team;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.testrank.hcf.core.mongo.MongoManager;
import com.testrank.hcf.core.util.Position;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TeamRepository {
    private final MongoManager mongo;
    private final ConcurrentMap<UUID, Team> memory = new ConcurrentHashMap<>();

    public TeamRepository(MongoManager mongo) {
        this.mongo = mongo;
    }

    public CompletableFuture<Void> save(Team team) {
        if (!mongo.enabled()) {
            memory.put(team.id(), team);
            return CompletableFuture.completedFuture(null);
        }
        Document members = new Document();
        team.members().forEach((uuid, role) -> members.append(uuid.toString(), role.name()));
        Document document = new Document("id", team.id().toString())
                .append("name", team.name())
                .append("nameLower", team.nameLower())
                .append("members", members)
                .append("allies", team.allies().stream().map(UUID::toString).toList())
                .append("dtr", team.dtr())
                .append("balance", team.balance())
                .append("points", team.points())
                .append("kothCaps", team.kothCaps())
                .append("hq", encodeLocation(team.hq()))
                .append("rally", encodeLocation(team.rally()))
                .append("focused", team.focused() == null ? null : team.focused().toString())
                .append("frozen", team.frozen())
                .append("frozenUntil", team.frozenUntil())
                .append("claimLocked", team.claimLocked())
                .append("regenPaused", team.regenPaused())
                .append("friendlyFire", team.friendlyFire())
                .append("logs", team.logs());
        return MongoManager.toFuture(mongo.collection("teams").replaceOne(
                Filters.eq("id", team.id().toString()),
                document,
                new ReplaceOptions().upsert(true)
        )).thenApply(ignored -> null);
    }

    public CompletableFuture<Void> delete(UUID id) {
        if (!mongo.enabled()) {
            memory.remove(id);
            return CompletableFuture.completedFuture(null);
        }
        return MongoManager.toFuture(mongo.collection("teams").deleteOne(Filters.eq("id", id.toString()))).thenApply(ignored -> null);
    }

    public CompletableFuture<List<Team>> loadAll() {
        if (!mongo.enabled()) {
            return CompletableFuture.completedFuture(List.copyOf(memory.values()));
        }
        return MongoManager.toListFuture(mongo.collection("teams").find()).thenApply(documents -> documents.stream().map(this::decode).toList());
    }

    public CompletableFuture<Team> load(UUID id) {
        if (!mongo.enabled()) {
            return CompletableFuture.completedFuture(memory.get(id));
        }
        return MongoManager.toFuture(mongo.collection("teams").find(Filters.eq("id", id.toString())).first())
                .thenApply(document -> document == null ? null : decode(document));
    }

    private Team decode(Document document) {
        Team team = new Team(UUID.fromString(document.getString("id")), document.getString("name"));
        Object members = document.get("members");
        if (members instanceof Document memberDocument) {
            for (Map.Entry<String, Object> entry : memberDocument.entrySet()) {
                team.loadMember(UUID.fromString(entry.getKey()), TeamRole.valueOf(String.valueOf(entry.getValue())));
            }
        }
        for (String ally : document.getList("allies", String.class, List.of())) {
            team.loadAlly(UUID.fromString(ally));
        }
        team.dtr(document.getDouble("dtr") == null ? 1.1D : document.getDouble("dtr"));
        Number balance = document.get("balance", Number.class);
        team.balance(balance == null ? 0D : balance.doubleValue());
        team.points(document.getInteger("points", 0));
        team.kothCaps(document.getInteger("kothCaps", 0));
        team.hq(decodeLocation(document.get("hq", Document.class)));
        team.rally(decodeLocation(document.get("rally", Document.class)));
        String focused = document.getString("focused");
        if (focused != null) {
            team.focused(UUID.fromString(focused));
        }
        team.frozen(document.getBoolean("frozen", false));
        Number frozenUntil = document.get("frozenUntil", Number.class);
        team.frozenUntil(frozenUntil == null ? 0L : frozenUntil.longValue());
        team.claimLocked(document.getBoolean("claimLocked", false));
        team.regenPaused(document.getBoolean("regenPaused", false));
        team.friendlyFire(document.getBoolean("friendlyFire", false));
        for (String log : document.getList("logs", String.class, List.of())) {
            team.loadLog(log);
        }
        return team;
    }

    private static Document encodeLocation(Position location) {
        if (location == null) {
            return null;
        }
        return new Document("world", location.world())
                .append("x", location.x())
                .append("y", location.y())
                .append("z", location.z())
                .append("yaw", location.yaw())
                .append("pitch", location.pitch());
    }

    private static Position decodeLocation(Document document) {
        if (document == null) {
            return null;
        }
        return new Position(
                document.getString("world"),
                document.get("x", Number.class).doubleValue(),
                document.get("y", Number.class).doubleValue(),
                document.get("z", Number.class).doubleValue(),
                document.get("yaw", Number.class).floatValue(),
                document.get("pitch", Number.class).floatValue()
        );
    }
}
