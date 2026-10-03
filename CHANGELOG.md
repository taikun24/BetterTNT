# Changelog

The release workflow uses the first version section of this file as the CurseForge changelog.
Keep the heading format (`## <version>`), and keep it equal to `mod.version` in `gradle.properties`.

## 1.1.0

Complete rewrite. Now available for Forge 1.20.1 as well as 1.16.5.

### Changed

- **Explosions run faster with exactly the vanilla result.** Block lookups along explosion rays, entity exposure raycasts and collision checks of moving TNT were sped up. Every optimization was compared against vanilla more than 300 million times per version, with zero differences.
- **Explosions are only spread over ticks when the server is behind** (a tick has already taken 40 ms). While the server keeps up, nothing is delayed.
- New optional settings: lighter explosion packets (on by default), a cap on explosion effects per player, fewer explosion rays, and TNT that waits in the world instead of vanishing. Each setting that changes vanilla behavior is labeled in the config.
- New `/bettertnt` command: stats, `flush` and `clear`.

### Fixed (from 1.0.0)

- Delayed explosions did not break blocks.
- Delayed explosions played their sound and particles immediately.
- The config file had no effect (the limit was always 100).
- Dedicated servers crashed on startup.
- Explosions were counted twice, and the budget was reset twice per tick.
- Delayed explosions ran in reverse order.
- Delayed explosions were lost when the server stopped.
