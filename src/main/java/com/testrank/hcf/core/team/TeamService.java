package com.testrank.hcf.core.team;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.config.HCFSettings;
import com.testrank.hcf.core.profile.ProfileService;
import com.testrank.hcf.core.redis.RedisManager;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TeamService implements HCFService {
    private final TeamRepository repository;
    private final ProfileService profiles;
    private final RedisManager redis;
    private final HCFSettings settings;
    private final ConcurrentMap<UUID, Team> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> byName = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TeamInvite> invites = new ConcurrentHashMap<>();

    public TeamService(TeamRepository repository, ProfileService profiles, RedisManager redis, HCFSettings settings) {
        this.repository = repository;
        this.profiles = profiles;
        this.redis = redis;
        this.settings = settings;
    }

    public CompletableFuture<Team> create(String name, UUID leader) {
        validateName(name);
        if (byPlayer(leader).isPresent()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You are already in a faction."));
        }
        String key = name.toLowerCase(java.util.Locale.ROOT);
        synchronized (this) {
            if (byName.containsKey(key)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("A team with that name already exists."));
            }
            Team team = new Team(UUID.randomUUID(), name, leader);
            byId.put(team.id(), team);
            byName.put(key, team.id());
            setProfileTeam(leader, team.id());
            return repository.save(team).thenApply(ignored -> team).exceptionally(throwable -> {
                byId.remove(team.id());
                byName.remove(key);
                setProfileTeam(leader, null);
                throw new java.util.concurrent.CompletionException(throwable);
            });
        }
    }

    @Override
    public void start() {
        repository.loadAll().thenAccept(loaded -> {
            for (Team team : loaded) {
                cache(team);
            }
        });
        redis.subscribe("teams", envelope -> {
            try {
                if (envelope.payload().startsWith("delete:")) {
                    UUID deleted = UUID.fromString(envelope.payload().substring("delete:".length()));
                    Team removed = byId.remove(deleted);
                    if (removed != null) {
                        byName.remove(removed.nameLower());
                    }
                    return;
                }
                UUID id = UUID.fromString(envelope.payload());
                repository.load(id).thenAccept(team -> {
                    if (team != null) {
                        cache(team);
                    }
                });
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    public Optional<Team> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Team> byName(String name) {
        UUID id = byName.get(name.toLowerCase(java.util.Locale.ROOT));
        return id == null ? Optional.empty() : byId(id);
    }

    public Optional<Team> byPlayer(UUID player) {
        return profiles.cached(player).map(profile -> profile.teamId()).flatMap(this::byId);
    }

    public boolean invite(Team team, UUID invited, UUID inviter) {
        if (team.members().size() >= settings.factionMaxMembers()) {
            throw new IllegalStateException("That faction is full.");
        }
        if (byPlayer(invited).isPresent()) {
            throw new IllegalStateException("That player is already in a faction.");
        }
        invites.put(invited, new TeamInvite(team.id(), inviter, System.currentTimeMillis() + settings.factionInviteExpireSeconds() * 1000L));
        team.log("Invite sent to " + invited + " by " + inviter);
        return true;
    }

    public boolean revokeInvite(Team team, UUID invited) {
        TeamInvite invite = invites.get(invited);
        if (invite == null || !invite.teamId().equals(team.id())) {
            return false;
        }
        invites.remove(invited);
        team.log("Invite revoked for " + invited);
        return true;
    }

    public CompletableFuture<Team> acceptInvite(UUID player) {
        TeamInvite invite = invites.remove(player);
        if (invite == null || invite.expiresAt() <= System.currentTimeMillis()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You do not have an active invite."));
        }
        Team team = byId.get(invite.teamId());
        if (team == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("That team no longer exists."));
        }
        if (byPlayer(player).isPresent()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You are already in a faction."));
        }
        if (team.members().size() >= settings.factionMaxMembers()) {
            return CompletableFuture.failedFuture(new IllegalStateException("That faction is full."));
        }
        team.member(player, TeamRole.MEMBER);
        setProfileTeam(player, team.id());
        return save(team).thenApply(ignored -> team);
    }

    public CompletableFuture<Void> leave(UUID player) {
        Optional<Team> optional = byPlayer(player);
        if (optional.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("You are not in a team."));
        }
        Team team = optional.get();
        if (team.members().get(player) == TeamRole.LEADER && team.members().size() > 1) {
            return CompletableFuture.failedFuture(new IllegalStateException("Transfer leader before leaving."));
        }
        team.removeMember(player);
        setProfileTeam(player, null);
        if (team.members().isEmpty()) {
            byId.remove(team.id());
            byName.remove(team.nameLower());
            return repository.delete(team.id()).thenCompose(ignored -> redis.publish("teams", "delete:" + team.id()));
        }
        return save(team);
    }

    public CompletableFuture<Void> disband(Team team) {
        byId.remove(team.id());
        byName.remove(team.nameLower());
        team.members().keySet().forEach(member -> setProfileTeam(member, null));
        return repository.delete(team.id()).thenCompose(ignored -> redis.publish("teams", "delete:" + team.id()));
    }

    public CompletableFuture<Void> rename(Team team, String name) {
        String nextKey = name.toLowerCase(java.util.Locale.ROOT);
        UUID existing = byName.get(nextKey);
        if (existing != null && !existing.equals(team.id())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("A team with that name already exists."));
        }
        validateName(name);
        byName.remove(team.nameLower());
        team.rename(name);
        byName.put(team.nameLower(), team.id());
        return save(team);
    }

    public CompletableFuture<Void> kick(Team team, UUID target) {
        team.removeMember(target);
        setProfileTeam(target, null);
        return save(team);
    }

    public CompletableFuture<Void> forceJoin(Team team, UUID target, TeamRole role) {
        CompletableFuture<Void> oldSave = CompletableFuture.completedFuture(null);
        Optional<Team> old = byPlayer(target);
        if (old.isPresent() && !old.get().id().equals(team.id())) {
            old.get().removeMember(target);
            oldSave = save(old.get());
        }
        if (!team.isMember(target) && team.members().size() >= settings.factionMaxMembers()) {
            return CompletableFuture.failedFuture(new IllegalStateException("That faction is full."));
        }
        team.member(target, role);
        setProfileTeam(target, team.id());
        return oldSave.thenCompose(ignored -> save(team));
    }

    public CompletableFuture<Void> setRole(Team team, UUID target, TeamRole role) {
        if (!team.isMember(target)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Target is not in that team."));
        }
        team.member(target, role);
        return save(team);
    }

    public CompletableFuture<Void> setHq(Team team, org.bukkit.Location location) {
        team.hq(location);
        team.log("HQ updated");
        return save(team);
    }

    public boolean canManage(UUID actor, Team team, TeamRole minimum) {
        TeamRole role = team.members().get(actor);
        return role != null && role.ordinal() >= minimum.ordinal();
    }

    public Collection<Team> teams() {
        return byId.values();
    }

    public CompletableFuture<Void> save(Team team) {
        return repository.save(team).thenCompose(ignored -> redis.publish("teams", team.id().toString()));
    }

    private record TeamInvite(UUID teamId, UUID inviter, long expiresAt) {}

    private void cache(Team team) {
        byId.put(team.id(), team);
        byName.put(team.nameLower(), team.id());
        team.members().keySet().forEach(member -> profiles.cached(member).ifPresent(profile -> profile.teamId(team.id())));
    }

    private void setProfileTeam(UUID player, UUID teamId) {
        profiles.cached(player).ifPresent(profile -> {
            profile.teamId(teamId);
            profiles.save(profile);
        });
    }

    private static void validateName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Faction names must be 3-16 letters, numbers, or underscores.");
        }
    }

    @Override
    public void close() {
        CompletableFuture.allOf(byId.values().stream().map(repository::save).toArray(CompletableFuture[]::new)).join();
    }
}
