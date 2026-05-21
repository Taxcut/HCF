# HCF 1.0 Release Checklist

## Runtime
- Java: Java 21 runtime is required for this build.
- Server: Spigot/custom Spigot 1.7/1.8 with Java 21 class loading support.
- Build: `./gradlew clean shadowJar test`
- Jar: `build/libs/hcf-1.0.jar`

## Required Production Settings
- Set `server.production-mode: true` for the live release.
- Set `mongo.enabled: true` and configure `mongo.uri` / `mongo.database`.
- Keep Redis optional unless the network needs cross-server sync and Particle intel publishing.
- With production mode enabled, HCF refuses to boot if MongoDB cannot be verified.

## Optional Dependencies
- Particle Core / Rank Core for `%particle_rank%`, staff notes, punishment intel, and `/link discord`.
- PlaceholderAPI and LuckPerms are soft hooks.
- PacketEvents and Lunar Apollo degrade gracefully if missing.

## Pre-Release In-Game Tests
- Join with a new profile, verify PvP timer and scoreboard.
- Create a faction, invite/join, deposit balance, claim land, set HQ, run `/f show`, `/f map`, `/f unclaim`.
- Kill a faction member and verify DTR loss, faction broadcast, freeze timer, deathban/lives behavior, and persistence after restart.
- Test `/revive <player>` and `/deathban check/remove <player>` on an offline deathbanned profile.
- Test `/msg`, `/reply`, `/ignore`, `/unignore`, `/togglepm`, `/request`, `/report`, `/socialspy`.
- Test `/staff`, `/vanish`, `/freeze`, `/staffbuild`, `/invsee`, `/ec`, `/heal`, `/feed`, `/mutechat`, `/slowchat`.
- Start a KOTH from an existing claim and verify cap/scoreboard/reward behavior.
- Buy/sell in `/shop`, claim `/kit`, `/reclaim`, `/redeem`, then restart and verify persistence.

## Permissions To Assign
- `hcf.staff` for moderation commands.
- `hcf.admin` for administrative commands.
- `hcf.command.revive` or `hcf.staff.revive` for `/revive`.
- `hcf.deathban.bypass` for deathban bypass.
- `hcf.kit.bypass` for kit cooldown bypass.

## Config Hot Reload
- `/hcf reload` reloads Bukkit config and dynamic ability definitions.
- Restart after changing constructor-level systems such as Mongo, Redis, scoreboard intervals, DTR caps, and claim sizing.
