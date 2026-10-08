[![Java CI with Gradle](https://github.com/s3tupw1zard/LevelTools/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/s3tupw1zard/LevelTools/actions/workflows/gradle.yml)

<h5 align="center">Maintained fork: https://github.com/s3tupw1zard/LevelTools</h5>
<h5 align="center">Issues / support: https://github.com/s3tupw1zard/LevelTools/issues</h5>
<h5 align="center">Original project by byteful: https://github.com/byteful/LevelTools</h5>

![Logo](https://github.com/byteful/LevelTools/blob/main/LevelTools%20Large%20Logo.png?raw=true)

<h3 align="center">LevelTools, originally created by byteful and extended by s3tupw1zard.</h3>

## 2026.1 Direction

The maintained fork targets Paper/Purpur 26.2+ and Java 25. Versioning uses
SemVer-compatible CalVer (`YYYY.RELEASE.PATCH`), with `2026.1.0` as the first
stable target and `2026.1.0-SNAPSHOT` for development builds.

The 2026.1 progression model deliberately separates item levels from enchantments:

- Level **1** is the vanilla/baseline item.
- Every level improves formula-derived stats; there are no hardcoded per-level stat tables.
- Default max level is 100 but the curve supports other caps without making large caps exponentially impractical.
- Leveling does **not** add enchantments automatically.
- Player-applied enchantments keep their vanilla behavior and can add small configurable LevelTools stat bonuses.
- Item state is stored in PDC; legacy LevelTools NBT data is migrated one-way.
- Java compilation is enforced with `-Xlint:all -Werror`.

## Formula-Based Stats

Stats use normalized level progress, so the same profile remains meaningful at max level 50, 100, 500 or 1000.

```text
progress = (level - 1) / (maxLevel - 1)

value = start + (max - start) * progress^exponent
```

Default stat types:

- Damage
- Critical hit chance
- Critical hit damage
- Defense
- Critical defense
- Max durability

Melee weapons currently default to a maximum **+300% LevelTools damage bonus**
(4x the baseline before vanilla enchantment effects), 15% LevelTools critical
chance and +75% LevelTools critical damage at max level.

Armor defense is combined multiplicatively across pieces, avoiding accidental
100%+ damage reduction.

## XP Progression

Required XP is configured in `progression_profiles.yml`.

The default profile starts at 100 XP for Level 1 -> 2 and ramps non-linearly to
10,000 XP for the final Level 99 -> 100 transition.

`max_level_influence` controls how total grind changes when the level cap changes:

- `0.0`: roughly the same total XP even with more/fewer level steps.
- `0.5`: sublinear total-XP growth (default).
- `1.0`: roughly linear total-XP growth.

At max level the display is capped at full progress instead of showing a fake next level.

## Combat XP and Shared Kills

Combat XP no longer uses a flat 1-2 XP reward. A mob produces an XP pool from its
max health and an optional entity multiplier. Difficult mobs ship with stronger
multipliers; for example Warden, Wither and Ender Dragon are explicitly weighted.

When multiple players/items contribute damage:

- XP is damage-weighted.
- Overkill is ignored by default.
- Contributions below 2% are ignored by default.
- The XP pool is not duplicated for every player.
- Contributions are tracked per player **and per LevelTools item**, so switching
  weapons does not move all XP to the item held at death.
- Optional group scaling exists but is disabled by default.

Configured boss mobs can also grant a level-based bonus. This bonus can require
one or more LevelTools critical hits and is protected by a persistent per-player,
per-mob claim interval.

## Configuration Layout

| File | Purpose |
| --- | --- |
| `config.yml` | Runtime/general plugin settings |
| `item_profiles.yml` | Materials and references to the profiles below |
| `progression_profiles.yml` | Level cap and non-linear required-XP curves |
| `stat_profiles.yml` | Damage, crit, defense and durability curves |
| `xp_sources.yml` | Mining, farming, fishing, armor and combat XP values |
| `enchantment_modifiers.yml` | Optional enchantment -> LevelTools stat contributions |
| `trigger_profiles.yml` | When/where a trigger may fire; filters and slots |
| `display_profiles.yml` | Compact item lore, action bar and progress display |
| `reward_profiles.yml` | Optional milestone commands/cosmetics; no default auto-enchants |

Existing 2026.1-pre-progression profile files are backed up and migrated. Custom
legacy `max_level` values are converted to dedicated progression profiles.

## Default Supported Items

- Pickaxes, axes, shovels and hoes
- Swords
- Bows and crossbows
- Tridents
- Maces
- Wooden through Netherite spears, including Copper
- Wearable armor, including Copper and Turtle Helmet
- Fishing rods

## Compact Item Display

Different display profiles keep tooltips readable instead of showing every
possible stat on every item.

A weapon can show:

```text
Level 73/100
[progress] 6.84K/7.52K

Attack: 24.61 (+207.6%)
Critical: 9.6% • +48.2%
Durability: +70.1%
```

Armor instead shows Defense, Critical Defense and Durability. A detailed stats
GUI is intentionally left for a later feature.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/leveltools help [page]` | Paginated command help | None |
| `/leveltools reload` | Reload configuration | `leveltools.admin` |
| `/leveltools reset <player>` | Reset hand item for player | `leveltools.admin` |
| `/leveltools reset <player> --all` | Reset all LevelTools items for player | `leveltools.admin` |
| `/leveltools xp <amount>` | Set hand item XP | `leveltools.admin` |
| `/leveltools level <level>` | Set hand item level within its progression cap | `leveltools.admin` |
| `/leveltools levelup` | Increase hand item level by one | `leveltools.admin` |
| `/leveltools debug` | Show debug information | `leveltools.admin` |

## PlaceholderAPI

| Placeholder | Description |
| --- | --- |
| `%leveltools_level%` | Current item level |
| `%leveltools_xp%` | Current item XP |
| `%leveltools_max_xp%` | XP threshold for the current level |
| `%leveltools_progress%` | Progress percentage |
| `%leveltools_progress_bar%` | Rendered progress bar |
| `%leveltools_item_profile%` | Current item profile |
| `%leveltools_max_level%` | Max level from the progression profile |

## Attribution

This repository is a maintained fork of byteful's LevelTools and remains licensed
under the GNU Affero General Public License v3.0. The original LevelTools codebase
was created by byteful; the 2026.1 modernization and extended progression work is
maintained by s3tupw1zard.
