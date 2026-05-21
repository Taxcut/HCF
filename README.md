# HCF Core

Legacy Spigot 1.7/1.8-targeted HCF core scaffolded for a Java 21 runtime with async MongoDB, Redis sync,
PacketEvents hooks, Lunar Apollo integration facades, optimized claim movement handling,
scoreboard/tab/nametag services, staff tools, menus, combat timers, teams, and event foundations.

## Threading Model

- Bukkit entity/world mutations are marshalled through `Threading.runSync`.
- MongoDB uses the reactive streams driver and exposes `CompletableFuture` repository APIs.
- Redis uses Lettuce async commands and pub/sub fanout for cross-server invalidation.
- Hot-path listeners perform O(1) cache lookups and defer persistence/expensive rendering.
- Claim movement checks are chunk/coordinate cached and skip unless the player crosses block buckets.
- Modern Paper/Adventure/PersistentDataContainer APIs are avoided in this legacy build.

## Build

This legacy HCF build targets Spigot/custom Spigot 1.7/1.8 APIs but intentionally compiles for Java 21.
The server JVM must run Java 21 or newer, and the custom Spigot must accept Java 21 class files. Downgrading to
Java 8 would require removing records, switch expressions, pattern matching, `List.of`, `Map.of`, and other modern
language/API usage across the codebase.

```powershell
./gradlew clean shadowJar test
```

The deployable plugin jar is written to:

```text
build/libs/hcf-1.0.jar
```

## Implemented Systems

- Async MongoDB repositories for profiles, teams, claims, with startup loading and indexes.
- Production mode gate: `server.production-mode=true` requires MongoDB to be enabled and reachable before gameplay systems enable.
- Lettuce Redis async commands plus pub/sub dirty sync for profiles and teams.
- Team lifecycle, invites, join/leave, roles, HQ, focus, logs, DTR death penalties, faction DTR broadcasts, persistent DTR freeze and regen.
- Chunk-indexed claim engine with faction balance purchases, world/spawn/size/overlap validation, `/f unclaim`, `/f unclaimall`, and admin selection.
- Combat tags, last-hit tracking, pearl/gapple cooldowns, PvP timer protection, SOTW protection.
- Deathbans, lives, `/revive`, and `/deathban check/remove` for staff recovery workflows.
- KOTH capture loop tied to claim ownership, with event scoring and broadcast.
- Partner item registry with persistent metadata, cooldowns, item generation, and potion effects.
- Staff mode, vanish, freeze, random teleport, inventory inspect, and reports.
- Team/ally/staff chat routing, private messages/replies, ignore toggles, social spy, mute chat, and slow chat with async-to-main-thread safety.
- Scoreboard, tab, nametag, waypoint, Lunar/client compatibility facades, and PacketEvents rate-limit boundary.
- Inventory menu framework with buttons, item builders, state tracking, and pagination.

## Release Notes

See [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) before deploying to the live HCF server.
