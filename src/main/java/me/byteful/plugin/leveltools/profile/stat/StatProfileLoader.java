package me.byteful.plugin.leveltools.profile.stat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class StatProfileLoader {
    private final Logger logger;

    public StatProfileLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    @NotNull
    public Map<String, StatProfile> load(@NotNull FileConfiguration config) {
        final Map<String, StatProfile> profiles = new HashMap<>();
        final ConfigurationSection root = config.getConfigurationSection("profiles");
        if (root == null) {
            logger.warning("No stat profiles found in stat_profiles.yml");
            return profiles;
        }

        for (String id : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            try {
                final Map<StatType, StatCurve> curves = new EnumMap<>(StatType.class);
                for (String statKey : section.getKeys(false)) {
                    final ConfigurationSection stat = section.getConfigurationSection(statKey);
                    if (stat == null) {
                        continue;
                    }

                    curves.put(
                            StatType.fromConfigKey(statKey),
                            new StatCurve(
                                    stat.getDouble("start", 0.0),
                                    stat.getDouble("max", 0.0),
                                    stat.getDouble("exponent", 1.0)
                            )
                    );
                }
                profiles.put(id, new StatProfile(id, curves));
            } catch (RuntimeException e) {
                logger.severe("Failed to load stat profile '" + id + "': " + e.getMessage());
            }
        }

        logger.info("Loaded " + profiles.size() + " stat profile(s).");
        return profiles;
    }
}
