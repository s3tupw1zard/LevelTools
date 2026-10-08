package me.byteful.plugin.leveltools.profile.display;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class DisplayProfileLoader {
    private final Logger logger;

    public DisplayProfileLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    @NotNull
    public Map<String, DisplayProfile> load(@NotNull FileConfiguration config) {
        final Map<String, DisplayProfile> profiles = new HashMap<>();
        final Map<String, ConfigurationSection> raw = new HashMap<>();
        final ConfigurationSection root = config.getConfigurationSection("profiles");

        if (root == null) {
            logger.warning("No display profiles found in display_profiles.yml");
            return profiles;
        }

        for (String id : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            if (section != null) {
                raw.put(id, section);
            }
        }

        for (String id : raw.keySet()) {
            try {
                resolve(id, raw, profiles, new HashSet<>());
            } catch (RuntimeException e) {
                logger.severe("Failed to load display profile '" + id + "': " + e.getMessage());
            }
        }

        logger.info("Loaded " + profiles.size() + " display profile(s).");
        return profiles;
    }

    @NotNull
    private DisplayProfile resolve(
            @NotNull String id,
            @NotNull Map<String, ConfigurationSection> raw,
            @NotNull Map<String, DisplayProfile> resolved,
            @NotNull Set<String> chain
    ) {
        final DisplayProfile existing = resolved.get(id);
        if (existing != null) {
            return existing;
        }
        if (!chain.add(id)) {
            throw new IllegalStateException("Circular display profile inheritance: " + chain);
        }

        final ConfigurationSection section = raw.get(id);
        if (section == null) {
            throw new IllegalArgumentException("Unknown display profile: " + id);
        }

        DisplayProfile parent = null;
        final String parentId = section.getString("extends");
        if (parentId != null && !parentId.isBlank()) {
            parent = resolve(parentId, raw, resolved, chain);
        }

        final DisplayProfile profile = parseProfile(id, section, parent);
        resolved.put(id, profile);
        chain.remove(id);
        return profile;
    }

    @NotNull
    private DisplayProfile parseProfile(
            @NotNull String id,
            @NotNull ConfigurationSection section,
            @Nullable DisplayProfile parent
    ) {
        final DisplayProfile.NameDisplay nameDisplay =
                section.isConfigurationSection("name")
                        ? parseNameDisplay(section.getConfigurationSection("name"))
                        : parent == null ? DisplayProfile.NameDisplay.disabled() : parent.getNameDisplay();
        final DisplayProfile.ActionBarDisplay actionBarDisplay =
                section.isConfigurationSection("action_bar")
                        ? parseActionBarDisplay(section.getConfigurationSection("action_bar"))
                        : parent == null
                                ? DisplayProfile.ActionBarDisplay.disabled()
                                : parent.getActionBarDisplay();
        final DisplayProfile.LoreDisplay loreDisplay =
                section.isConfigurationSection("lore")
                        ? parseLoreDisplay(section.getConfigurationSection("lore"))
                        : parent == null ? DisplayProfile.LoreDisplay.disabled() : parent.getLoreDisplay();
        final ProgressBarConfig progressBar =
                section.isConfigurationSection("progress_bar")
                        ? parseProgressBar(section.getConfigurationSection("progress_bar"))
                        : parent == null ? ProgressBarConfig.defaultConfig() : parent.getProgressBar();

        return DisplayProfile.builder(id)
                .nameDisplay(nameDisplay)
                .actionBarDisplay(actionBarDisplay)
                .loreDisplay(loreDisplay)
                .progressBar(progressBar)
                .build();
    }

    @NotNull
    private DisplayProfile.NameDisplay parseNameDisplay(@Nullable ConfigurationSection section) {
        if (section == null) {
            return DisplayProfile.NameDisplay.disabled();
        }
        return new DisplayProfile.NameDisplay(
                section.getBoolean("enabled", false),
                section.getString("text", "{item} &7- &b{level}")
        );
    }

    @NotNull
    private DisplayProfile.ActionBarDisplay parseActionBarDisplay(
            @Nullable ConfigurationSection section
    ) {
        if (section == null) {
            return DisplayProfile.ActionBarDisplay.disabled();
        }
        return new DisplayProfile.ActionBarDisplay(
                section.getBoolean("enabled", true),
                section.getString(
                        "text",
                        "{progress_bar} &e{xp_formatted}&6/&e{max_xp_formatted}"
                )
        );
    }

    @NotNull
    private DisplayProfile.LoreDisplay parseLoreDisplay(@Nullable ConfigurationSection section) {
        if (section == null) {
            return DisplayProfile.LoreDisplay.disabled();
        }
        final List<String> lines = section.getStringList("lines");
        return new DisplayProfile.LoreDisplay(section.getBoolean("enabled", true), lines);
    }

    @NotNull
    private ProgressBarConfig parseProgressBar(@Nullable ConfigurationSection section) {
        if (section == null) {
            return ProgressBarConfig.defaultConfig();
        }

        return ProgressBarConfig.builder()
                .totalBars(section.getInt("total_bars", 50))
                .barSymbol(getChar(section, "bar_symbol", '|'))
                .prefixSymbol(getChar(section, "prefix_symbol", '['))
                .suffixSymbol(getChar(section, "suffix_symbol", ']'))
                .prefixColor(getChar(section, "prefix_color", '8'))
                .suffixColor(getChar(section, "suffix_color", '8'))
                .completedColor(getChar(section, "completed_color", 'e'))
                .placeholderColor(getChar(section, "placeholder_color", '7'))
                .build();
    }

    private char getChar(
            @NotNull ConfigurationSection section,
            @NotNull String key,
            char defaultValue
    ) {
        final String value = section.getString(key);
        return value == null || value.isEmpty() ? defaultValue : value.charAt(0);
    }
}
