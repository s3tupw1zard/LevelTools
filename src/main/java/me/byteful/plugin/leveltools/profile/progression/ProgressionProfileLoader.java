package me.byteful.plugin.leveltools.profile.progression;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class ProgressionProfileLoader {
    private final Logger logger;

    public ProgressionProfileLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    @NotNull
    public Map<String, ProgressionProfile> load(@NotNull FileConfiguration config) {
        final Map<String, ProgressionProfile> profiles = new HashMap<>();
        final ConfigurationSection root = config.getConfigurationSection("profiles");
        if (root == null) {
            logger.warning("No progression profiles found in progression_profiles.yml");
            return profiles;
        }

        for (String id : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            try {
                final ConfigurationSection xp = section.getConfigurationSection("xp");
                if (xp == null) {
                    throw new IllegalArgumentException("Missing 'xp' section");
                }

                profiles.put(id, new ProgressionProfile(
                        id,
                        section.getInt("max_level", 100),
                        xp.getInt("reference_max_level", 100),
                        xp.getDouble("first_level_xp", 100.0),
                        xp.getDouble("final_level_xp", 10000.0),
                        xp.getDouble("curve_exponent", 1.85),
                        xp.getDouble("max_level_influence", 0.5)
                ));
            } catch (RuntimeException e) {
                logger.severe("Failed to load progression profile '" + id + "': " + e.getMessage());
            }
        }

        logger.info("Loaded " + profiles.size() + " progression profile(s).");
        return profiles;
    }
}
