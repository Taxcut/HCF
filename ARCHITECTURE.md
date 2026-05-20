# HCF Core Architecture

## Bootstrap Sequence

1. `HCFPlugin` loads `config.yml` into immutable `HCFSettings`.
2. The `ServiceRegistry` constructs infrastructure first: `Threading`, `MongoManager`, `RedisManager`.
3. Repositories and cache-backed services are registered: profiles, teams, claims, combat, cooldowns.
4. Client-facing services start after domain services: Lunar/client hooks, waypoints, nametags, scoreboard, tab.
5. Event, staff, PvP class, economy, ability, partner-item, and packet hook modules are registered.
6. `startAll()` initializes indexes, Redis, packet/client availability, and low-frequency render tasks.
7. Listeners and commands are registered last so no event can hit a partially built service graph.

## Module Map

- `api`: service lifecycle and dependency registry.
- `bootstrap`: legacy Bukkit/Spigot plugin entrypoint.
- `profile`: UUID profile cache, async repository, stats, cooldowns, timers, death history.
- `team`: faction identity, DTR, HQ, roles, allies, logs, focus, freeze state.
- `claim`: rectangular claims, chunk-indexed lookup, movement-friendly detection.
- `combat`: combat/archer tags, last-hit metadata, logout-safe tracking primitives.
- `timer`: reusable cooldown facade.
- `classes`: PvP class state bridge.
- `pvp`: PvP timer/protection API.
- `abilities` and `partneritems`: cooldown-backed use gates with client notifications.
- `events`, `koth`, `conquest`, `citadel`, `glowstone`, `eotw`, `sotw`: event state and scoring foundations.
- `staff`, `freeze`, `vanish`, `anticheat`: staff operations and compatibility hooks.
- `chat`: team chat and slow-chat control.
- `packets`: PacketEvents adapter boundary.
- `lunar`, `waypoint`, `nametag`, `scoreboard`, `tab`: client presentation and packet-light UI.
- `mongo`, `redis`: async persistence and cross-server messaging.
- `menu`: inventory framework with tracked button callbacks.
- `listeners`, `commands`: thin Bukkit adapters only.

## Persistence

Mongo collections are prepared for `profiles`, `teams`, `claims`, `cooldowns`, `timers`, `events`, `logs`, and `partner_items`.
Repositories return `CompletableFuture` and never block the server thread. Redis uses Lettuce async commands for cache sync,
economy updates, and future pub/sub invalidation.

Current startup loaders hydrate teams and claims into in-memory indexes. Profile records load on session join, then save on quit
and shutdown. Team/profile saves publish Redis dirty messages so other servers invalidate or reload their local cache copy.

## Hot Path Rules

- Movement checks bucket block coordinates before touching claim indexes.
- Claim lookup uses chunk buckets instead of scanning every claim.
- Combat listeners only update in-memory maps and profile counters.
- Scoreboard/tab updates run at conservative intervals and reuse Bukkit scoreboards.
- Client integrations degrade gracefully for vanilla users.
- Bukkit mutations from async callbacks are marshalled through `Threading.runSync`.
- Nametag refresh is distance-aware and scheduled rather than performed inside movement listeners.

## Extension Points

PacketEvents and Lunar Apollo are isolated behind `PacketHookService` and `ClientIntegrationService`. This keeps the core stable
across minor API shifts while allowing production adapter modules to register exact packet listeners, rich Apollo notifications,
bossbar syncing, waypoint rendering, and nametag overlays.
