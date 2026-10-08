package me.byteful.plugin.leveltools.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigUpdater {
    private static final Map<String, String> DEFAULT_STAT_PROFILES = Map.ofEntries(
            Map.entry("pickaxes", "tool"),
            Map.entry("axes", "tool"),
            Map.entry("shovels", "tool"),
            Map.entry("hoes", "tool"),
            Map.entry("swords", "melee_weapon"),
            Map.entry("tridents", "melee_weapon"),
            Map.entry("maces", "melee_weapon"),
            Map.entry("spears", "melee_weapon"),
            Map.entry("bows", "ranged_weapon"),
            Map.entry("crossbows", "ranged_weapon"),
            Map.entry("fishing_rods", "utility"),
            Map.entry("armor", "armor")
    );
    private static final Map<String, String> DEFAULT_DISPLAY_PROFILES = Map.ofEntries(
            Map.entry("pickaxes", "tool"),
            Map.entry("axes", "tool"),
            Map.entry("shovels", "tool"),
            Map.entry("hoes", "tool"),
            Map.entry("swords", "weapon"),
            Map.entry("tridents", "weapon"),
            Map.entry("maces", "weapon"),
            Map.entry("spears", "weapon"),
            Map.entry("bows", "ranged_weapon"),
            Map.entry("crossbows", "ranged_weapon"),
            Map.entry("fishing_rods", "utility"),
            Map.entry("armor", "armor")
    );
    private static final Set<String> LEGACY_DEFAULT_REWARD_PROFILES = Set.of(
            "tools",
            "swords",
            "bows",
            "crossbows",
            "fishing_rods",
            "tridents",
            "hoes"
    );

    private final Path dataFolder;
    private final Logger logger;

    public ConfigUpdater(@NotNull Path dataFolder, @NotNull Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void updateMainConfig() {
        Path configPath = dataFolder.resolve("config.yml");
        if (!Files.exists(configPath)) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configPath.toFile());
        boolean changed = false;

        if (config.contains("xp_formulas")) {
            config.set("xp_formulas", null);
            changed = true;
        }

        if (config.contains("level_xp_formula")) {
            config.set("level_xp_formula", null);
            changed = true;
        }

        if (!config.contains("prevent_enchanted_books_on_leveltools_items")) {
            config.set("prevent_enchanted_books_on_leveltools_items", false);
            changed = true;
        }

        if (!config.contains("farming.ignore_player_placed_blocks_for_fully_grown_crops")) {
            config.set("farming.ignore_player_placed_blocks_for_fully_grown_crops", true);
            changed = true;
        }

        changed |= ensureString(
                config,
                "messages.successfully_set_level",
                "&aSet {item} to level {level}."
        );
        changed |= ensureString(
                config,
                "messages.successfully_set_xp",
                "&aSet {item} XP to {xp}."
        );
        changed |= ensureString(
                config,
                "messages.successfully_level_up",
                "&aIncreased {item} to level {level}."
        );
        changed |= ensureString(
                config,
                "messages.already_max_level",
                "&e{item} is already at the maximum level ({level})."
        );
        changed |= ensureString(
                config,
                "messages.successfully_reset_tools",
                "&aReset {count} LevelTools item(s) for {player} to level 1 with 0 XP."
        );
        changed |= ensureString(
                config,
                "messages.successfully_reset_hand_tool",
                "&aReset {item} for {player} to level 1 with 0 XP."
        );

        if (config.contains("force_nbt")) {
            config.set("force_nbt", null);
            changed = true;
        }

        if (!changed) {
            return;
        }

        backupConfig(configPath);
        try {
            config.save(configPath.toFile());
            logger.info("Updated config.yml for the 2026.1 configuration baseline.");
        } catch (IOException e) {
            logger.severe("Failed to update config.yml: " + e.getMessage());
        }
    }

    public void updateProgressionConfigs() {
        final Path itemPath = dataFolder.resolve("item_profiles.yml");
        final Path progressionPath = dataFolder.resolve("progression_profiles.yml");
        final Path displayPath = dataFolder.resolve("display_profiles.yml");
        final Path xpSourcesPath = dataFolder.resolve("xp_sources.yml");
        if (!Files.exists(itemPath)
                || !Files.exists(progressionPath)
                || !Files.exists(displayPath)
                || !Files.exists(xpSourcesPath)) {
            return;
        }

        final YamlConfiguration itemConfig =
                YamlConfiguration.loadConfiguration(itemPath.toFile());
        final YamlConfiguration progressionConfig =
                YamlConfiguration.loadConfiguration(progressionPath.toFile());
        final YamlConfiguration displayConfig =
                YamlConfiguration.loadConfiguration(displayPath.toFile());
        final YamlConfiguration xpSourcesConfig =
                YamlConfiguration.loadConfiguration(xpSourcesPath.toFile());
        final var profiles = itemConfig.getConfigurationSection("profiles");
        if (profiles == null) {
            return;
        }

        boolean itemChanged = false;
        boolean progressionChanged = false;
        boolean displayChanged = ensureFormulaDisplayProfiles(displayConfig);
        boolean xpSourcesChanged = false;
        if (!xpSourcesConfig.contains("combat.sharing.dual_wield_xp_penalty")) {
            xpSourcesConfig.set("combat.sharing.dual_wield_xp_penalty", 0.30);
            xpSourcesChanged = true;
        }

        for (String profileId : profiles.getKeys(false)) {
            final var profile = profiles.getConfigurationSection(profileId);
            if (profile == null) {
                continue;
            }

            if (!profile.contains("progression_profile")) {
                final int oldMaxLevel = profile.getInt("max_level", 100);
                if (oldMaxLevel != 100) {
                    final String migratedId = "migrated_" + normalizeProfileId(profileId);
                    final String base = "profiles." + migratedId;
                    if (!progressionConfig.contains(base)) {
                        progressionConfig.set(base + ".max_level", Math.max(2, oldMaxLevel));
                        progressionConfig.set(base + ".xp.reference_max_level", 100);
                        progressionConfig.set(base + ".xp.first_level_xp", 100.0);
                        progressionConfig.set(base + ".xp.final_level_xp", 10000.0);
                        progressionConfig.set(base + ".xp.curve_exponent", 1.85);
                        progressionConfig.set(base + ".xp.max_level_influence", 0.50);
                        progressionChanged = true;
                    }
                    profile.set("progression_profile", migratedId);
                } else {
                    profile.set("progression_profile", "default");
                }
                itemChanged = true;
            }

            if (!profile.contains("stat_profile")) {
                profile.set(
                        "stat_profile",
                        DEFAULT_STAT_PROFILES.getOrDefault(profileId, "none")
                );
                itemChanged = true;
            }

            final String displayProfile = profile.getString("display_profile", "default");
            final String formulaDisplay = DEFAULT_DISPLAY_PROFILES.get(profileId);
            if ("default".equals(displayProfile) && formulaDisplay != null) {
                profile.set("display_profile", formulaDisplay);
                itemChanged = true;
            }

            final String rewardProfile = profile.getString("reward_profile");
            if (rewardProfile != null
                    && LEGACY_DEFAULT_REWARD_PROFILES.contains(rewardProfile)
                    && DEFAULT_STAT_PROFILES.containsKey(profileId)) {
                profile.set("reward_profile", "none");
                itemChanged = true;
            }

            if (profile.contains("max_level")) {
                profile.set("max_level", null);
                itemChanged = true;
            }

            if (profile.contains("level_xp_formula")) {
                logger.warning(
                        "Item profile '" + profileId
                                + "' used legacy level_xp_formula. The formula-based 2026.1 "
                                + "progression model now uses progression_profiles.yml; the old "
                                + "formula was left in the backup only.");
                profile.set("level_xp_formula", null);
                itemChanged = true;
            }
        }

        if (itemChanged) {
            backupFile(itemPath, "item_profiles-2026.1-backup-");
            save(itemConfig, itemPath, "item_profiles.yml");
        }

        if (progressionChanged) {
            backupFile(progressionPath, "progression_profiles-2026.1-backup-");
            save(progressionConfig, progressionPath, "progression_profiles.yml");
        }

        if (displayChanged) {
            backupFile(displayPath, "display_profiles-2026.1-backup-");
            save(displayConfig, displayPath, "display_profiles.yml");
        }

        if (xpSourcesChanged) {
            backupFile(xpSourcesPath, "xp_sources-2026.1-backup-");
            save(xpSourcesConfig, xpSourcesPath, "xp_sources.yml");
        }
    }

    private static boolean ensureFormulaDisplayProfiles(@NotNull YamlConfiguration config) {
        boolean changed = false;
        changed |= ensureDisplayProfile(
                config,
                "weapon",
                java.util.List.of(
                        "",
                        "&eLevel: &6{level}&7/&6{max_level}",
                        "{progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}",
                        "",
                        "&cAttack: &f{attack_damage} &7({damage_bonus})",
                        "&6Critical: &f{critical_chance} &7• &f+{critical_damage}",
                        "&aDurability: &f+{durability_bonus}"
                )
        );
        changed |= ensureDisplayProfile(
                config,
                "ranged_weapon",
                java.util.List.of(
                        "",
                        "&eLevel: &6{level}&7/&6{max_level}",
                        "{progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}",
                        "",
                        "&cDamage: &f+{damage_bonus}",
                        "&6Critical: &f{critical_chance} &7• &f+{critical_damage}",
                        "&aDurability: &f+{durability_bonus}"
                )
        );
        changed |= ensureDisplayProfile(
                config,
                "armor",
                java.util.List.of(
                        "",
                        "&eLevel: &6{level}&7/&6{max_level}",
                        "{progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}",
                        "",
                        "&9Defense: &f{defense}",
                        "&bCrit Defense: &f{critical_defense}",
                        "&aDurability: &f+{durability_bonus}"
                )
        );
        changed |= ensureDisplayProfile(
                config,
                "tool",
                java.util.List.of(
                        "",
                        "&eLevel: &6{level}&7/&6{max_level}",
                        "{progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}",
                        "",
                        "&aDurability: &f+{durability_bonus}"
                )
        );
        changed |= ensureDisplayProfile(
                config,
                "utility",
                java.util.List.of(
                        "",
                        "&eLevel: &6{level}&7/&6{max_level}",
                        "{progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}",
                        "",
                        "&aDurability: &f+{durability_bonus}"
                )
        );

        if (!config.contains("profiles.armor.action_bar")) {
            config.set("profiles.armor.action_bar.enabled", true);
            config.set(
                    "profiles.armor.action_bar.text",
                    "&bArmor {slot}: {progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}"
            );
            changed = true;
        }

        return changed;
    }

    private static boolean ensureDisplayProfile(
            @NotNull YamlConfiguration config,
            @NotNull String id,
            @NotNull java.util.List<String> lore
    ) {
        final String base = "profiles." + id;
        if (config.contains(base)) {
            return false;
        }

        config.set(base + ".extends", "default");
        config.set(base + ".lore.enabled", true);
        config.set(base + ".lore.lines", lore);
        return true;
    }

    @NotNull
    private static String normalizeProfileId(@NotNull String profileId) {
        return profileId.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_");
    }

    private static boolean ensureString(
            @NotNull YamlConfiguration config,
            @NotNull String path,
            @NotNull String value
    ) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    private void save(
            @NotNull YamlConfiguration config,
            @NotNull Path path,
            @NotNull String displayName
    ) {
        try {
            config.save(path.toFile());
            logger.info("Updated " + displayName + " for formula-based progression.");
        } catch (IOException e) {
            logger.severe("Failed to update " + displayName + ": " + e.getMessage());
        }
    }

    private void backupFile(@NotNull Path source, @NotNull String prefix) {
        final Path backup =
                dataFolder.resolve(prefix + System.currentTimeMillis() + ".yml");
        try {
            Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
            logger.info("Backed up " + source.getFileName() + " to " + backup.getFileName());
        } catch (IOException e) {
            logger.warning(
                    "Failed to backup " + source.getFileName() + " before updating: "
                            + e.getMessage());
        }
    }

    private void backupConfig(@NotNull Path configPath) {
        Path backupPath = dataFolder.resolve("config-2026.1-backup-" + System.currentTimeMillis() + ".yml");
        try {
            Files.copy(configPath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
            logger.info("Backed up config.yml to " + backupPath.getFileName());
        } catch (IOException e) {
            logger.warning("Failed to backup config.yml before updating: " + e.getMessage());
        }
    }
}