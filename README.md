# Better TNT

Reduces TNT lag. Forge 1.20.1 and 1.16.5, server-side (also works in singleplayer).

[日本語の詳しい説明](README.ja.md)

## What it does

- **Faster explosions, same result.** Vanilla explosion code is sped up without changing what it does: block lookups along the rays, raycasts for entity exposure, and collision checks of moving TNT. Every optimization was checked against vanilla more than 300 million times per version, with zero differences.
- **Spreads explosions over ticks, only under load.** If a tick has already taken 40 ms, further explosions are run in later ticks. While the server keeps up, nothing is delayed.
- **Optional, non-vanilla settings.** Lighter explosion packets (on by default; clients get the same blocks from normal block updates), a cap on explosion effects per player, and fewer explosion rays.

## Results (6,875 TNT on a dedicated server)

|                    | Vanilla | Better TNT |
|--------------------|---------|------------|
| Peak tick time     | 181 ms  | 41 ms      |
| Lowest TPS         | 5.5     | 20         |
| Time to finish     | ~25 s   | ~16 s      |

## Things to know

- When the server is overloaded, delayed explosions happen later than in vanilla. TNT cannons and other builds that rely on same-tick explosions may misfire then.
- Delayed explosions are run on a normal shutdown, but lost if the server crashes.

## Config

`<world>/serverconfig/bettertnt-server-config.toml`. Everything that changes vanilla behavior is labeled in the file.

## Commands (op level 2)

- `/bettertnt`: stats for the last tick
- `/bettertnt flush`: run all delayed explosions now
- `/bettertnt clear`: drop all delayed explosions

## Compatibility

If Canary or Radium is installed, Better TNT turns off its overlapping optimizations automatically. Spreading explosions over ticks still works.
