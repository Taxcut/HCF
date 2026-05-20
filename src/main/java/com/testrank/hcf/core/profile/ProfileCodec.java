package com.testrank.hcf.core.profile;

import org.bson.Document;

import java.util.Map;
import java.util.UUID;

public final class ProfileCodec {
    public Document encode(Profile profile) {
        Document document = new Document("uuid", profile.uuid().toString())
                .append("teamId", profile.teamId() == null ? null : profile.teamId().toString())
                .append("lastSeen", profile.lastSeen())
                .append("kills", profile.kills())
                .append("deaths", profile.deaths())
                .append("pvpClass", profile.pvpClass())
                .append("chatColor", profile.chatColor())
                .append("energy", profile.energy())
                .append("cooldowns", new Document(profile.cooldowns()))
                .append("timers", new Document(profile.timers()))
                .append("statistics", new Document(profile.statistics()))
                .append("settings", new Document(profile.settings()))
                .append("deathHistory", profile.deathHistory());
        return document;
    }

    public Profile decode(Document document) {
        Profile profile = new Profile(UUID.fromString(document.getString("uuid")));
        profile.kills(document.getInteger("kills", 0));
        profile.deaths(document.getInteger("deaths", 0));
        profile.lastSeen(document.getLong("lastSeen") == null ? System.currentTimeMillis() : document.getLong("lastSeen"));
        String team = document.getString("teamId");
        if (team != null) {
            profile.teamId(UUID.fromString(team));
        }
        profile.pvpClass(document.getString("pvpClass") == null ? "NONE" : document.getString("pvpClass"));
        profile.chatColor(document.getString("chatColor"));
        profile.energy(document.getInteger("energy", 0));
        Object cooldowns = document.get("cooldowns");
        if (cooldowns instanceof Document cooldownDocument) {
            for (Map.Entry<String, Object> entry : cooldownDocument.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    profile.cooldowns().put(entry.getKey(), number.longValue());
                }
            }
        }
        Object timers = document.get("timers");
        if (timers instanceof Document timerDocument) {
            for (Map.Entry<String, Object> entry : timerDocument.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    profile.timers().put(entry.getKey(), number.longValue());
                }
            }
        }
        Object statistics = document.get("statistics");
        if (statistics instanceof Document statisticDocument) {
            for (Map.Entry<String, Object> entry : statisticDocument.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    profile.statistics().put(entry.getKey(), number.longValue());
                }
            }
        }
        Object settings = document.get("settings");
        if (settings instanceof Document settingsDocument) {
            for (Map.Entry<String, Object> entry : settingsDocument.entrySet()) {
                profile.settings().put(entry.getKey(), Boolean.parseBoolean(String.valueOf(entry.getValue())));
            }
        }
        for (String death : document.getList("deathHistory", String.class, java.util.List.of())) {
            profile.loadDeath(death);
        }
        return profile;
    }
}
