[![Java CI with Gradle](https://github.com/s3tupw1zard/LevelTools/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/s3tupw1zard/LevelTools/actions/workflows/gradle.yml)
[![](https://jitpack.io/v/s3tupw1zard/LevelTools.svg)](https://jitpack.io/#s3tupw1zard/LevelTools)

<h5 align="center">Maintained fork: https://github.com/s3tupw1zard/LevelTools</h5>
<h5 align="center">Issues / support: https://github.com/s3tupw1zard/LevelTools/issues</h5>
<h5 align="center">Original project by byteful: https://github.com/byteful/LevelTools</h5>
<h5 align="center">Upstream documentation: https://github.com/byteful/LevelTools/wiki</h5>

![Logo](https://github.com/byteful/LevelTools/blob/main/LevelTools%20Large%20Logo.png?raw=true)

<h3 align="center">LevelTools, originally created by byteful and extended by s3tupw1zard.</h3>

## Features

- Targets Paper/Purpur 26.2+ with Java 25.
- Supports Folia.
- No required dependencies; PlaceholderAPI is optional.
- Profile-based configuration system.
- Any item can level up with custom triggers.
- Configurable global, item-profile, and permission-based XP formulas.
- Commands & enchants on level up.
- Supports blacklisting/whitelisting for blocks, entities, and items.
- ActionBar notifications.
- Item lore modification.
- Optional enchanted-book blocking for LevelTools items.
- Farming trigger support for fully grown player-planted crops.

## What's New in 2026.1

The maintained fork uses SemVer-compatible CalVer: `YYYY.RELEASE.PATCH`.
The first stable target is `2026.1.0`; development builds use
`2026.1.0-SNAPSHOT`.

- Java 25 and Paper/Purpur 26.2 baseline.
- Current Gradle and Shadow toolchain.
- Fork-owned update checking and support links.
- Removal of legacy Bukkit farming and attribute APIs.
- Warning-free compilation enforced in CI.

### Previous upstream 2.2 changes

- `ARMOR_DURABILITY` trigger awards XP when armor takes damage.
- Configurable XP formulas under `xp_formulas`, selectable per player via `leveltools.formula.<id>`.
- Toggle to block enchanted books from being applied to LevelTools items.
- `block_data_storage` option (`SQLITE`) for persisting per-block placement data.
- Farming trigger gains `ignore_player_placed_blocks_for_fully_grown_crops` for fully-grown crop handling.
- Automatic config migration from v1.x and earlier v2.x layouts.

## Profile System

LevelTools uses a modular profile-based configuration system. Any item can be configured to level up.

### Profile Types

| Profile Type | Purpose | File |
|-------------|---------|------|
| **Trigger Profiles** | Define how XP is gained | `trigger_profiles.yml` |
| **Reward Profiles** | Define rewards per level | `reward_profiles.yml` |
| **Display Profiles** | Define name, lore, action bar | `display_profiles.yml` |
| **Item Profiles** | Tie everything together | `item_profiles.yml` |

### Trigger Types

- `BLOCK_BREAK` - XP when breaking blocks
- `ENTITY_KILL` - XP when killing entities
- `FISHING` - XP when catching items
- `RIGHT_CLICK` / `LEFT_CLICK` - XP on click
- `CONSUME` - XP when consuming items
- `FARMING` - XP when tilling soil and breaking fully-grown crops
- `ARMOR_DURABILITY` - XP when worn armor takes damage

### Default Supported Items

Out of the box, LevelTools ships profiles for:
- Pickaxes, Axes, Shovels (block mining)
- Swords (combat)
- Bows, Crossbows (ranged)
- Fishing Rods (fishing)
- Tridents (combat / ranged)
- Hoes (farming)

Add any item by creating custom profiles. See the [Wiki](https://github.com/byteful/LevelTools/wiki) for details.

### Migration from v1.x

Your old config will be automatically backed up to `old_config.yml` and migrated to the new profile system.

Earlier v2.x configs are also updated automatically: `level_xp_formula` is moved to `xp_formulas.global`.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/leveltools help [page]` | Paginated command help | None |
| `/leveltools reload` | Reloads configuration | `leveltools.admin` |
| `/leveltools reset <player>` | Reset hand item for player | `leveltools.admin` |
| `/leveltools reset <player> --all` | Reset all items for player | `leveltools.admin` |
| `/leveltools xp <amount>` | Set hand item XP | `leveltools.admin` |
| `/leveltools level <level>` | Set hand item level | `leveltools.admin` |
| `/leveltools levelup` | Increase hand item level by 1 | `leveltools.admin` |
| `/leveltools debug` | Show debug information | `leveltools.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `leveltools.admin` | Access to admin commands | op |
| `leveltools.enabled` | Allow leveling for this player | true |
| `leveltools.formula.<id>` | Selects an XP formula defined under `xp_formulas` | false |

## Developer API

**View detailed API usage [here](https://github.com/byteful/LevelTools/wiki/Developer-API).**

## PlaceholderAPI

| Placeholder | Description |
|-------------|-------------|
| `%leveltools_level%` | Current item level (main hand) |
| `%leveltools_xp%` | Current XP (main hand) |
| `%leveltools_max_xp%` | XP required for the next level |
| `%leveltools_progress%` | Progress percentage (0-100, 1 decimal) |
| `%leveltools_progress_bar%` | Rendered progress bar |
| `%leveltools_item_profile%` | Item profile id for the hand item |
| `%leveltools_max_level%` | Max level for the current item profile |

## Documentation

- [Configuration](https://github.com/byteful/LevelTools/wiki/Configuration)
- [Trigger Profiles](https://github.com/byteful/LevelTools/wiki/Trigger-Profiles)
- [Reward Profiles](https://github.com/byteful/LevelTools/wiki/Reward-Profiles)
- [Display Profiles](https://github.com/byteful/LevelTools/wiki/Display-Profiles)
- [Item Profiles](https://github.com/byteful/LevelTools/wiki/Item-Profiles)
