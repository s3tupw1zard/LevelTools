package me.byteful.plugin.leveltools.config;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CombatXpConfig {
    private final double baseXp;
    private final double healthScale;
    private final double healthExponent;
    private final double minimumXp;
    private final double maximumXp;
    private final Map<String, Double> entityMultipliers;
    private final Sharing sharing;
    private final LevelBonus levelBonus;

    private CombatXpConfig(
            double baseXp,
            double healthScale,
            double healthExponent,
            double minimumXp,
            double maximumXp,
            @NotNull Map<String, Double> entityMultipliers,
            @NotNull Sharing sharing,
            @NotNull LevelBonus levelBonus
    ) {
        this.baseXp = baseXp;
        this.healthScale = healthScale;
        this.healthExponent = healthExponent;
        this.minimumXp = minimumXp;
        this.maximumXp = maximumXp;
        this.entityMultipliers = Collections.unmodifiableMap(entityMultipliers);
        this.sharing = sharing;
        this.levelBonus = levelBonus;
    }

    @NotNull
    public static CombatXpConfig load(@NotNull FileConfiguration config) {
        final ConfigurationSection combat = config.getConfigurationSection("combat");
        if (combat == null) {
            return defaults();
        }

        final ConfigurationSection formula = combat.getConfigurationSection("formula");
        final Map<String, Double> multipliers = new HashMap<>();
        final ConfigurationSection multiplierSection =
                combat.getConfigurationSection("entity_multipliers");
        if (multiplierSection != null) {
            for (String key : multiplierSection.getKeys(false)) {
                multipliers.put(
                        key.trim().toUpperCase(Locale.ROOT),
                        Math.max(0.0, multiplierSection.getDouble(key, 1.0))
                );
            }
        }

        return new CombatXpConfig(
                formula == null ? 2.0 : formula.getDouble("base_xp", 2.0),
                formula == null ? 0.35 : formula.getDouble("health_scale", 0.35),
                formula == null ? 0.80 : formula.getDouble("health_exponent", 0.80),
                formula == null ? 1.0 : formula.getDouble("minimum_xp", 1.0),
                formula == null ? 750.0 : formula.getDouble("maximum_xp", 750.0),
                multipliers,
                Sharing.from(combat.getConfigurationSection("sharing")),
                LevelBonus.from(combat.getConfigurationSection("level_bonus"))
        );
    }

    public double calculatePool(@NotNull LivingEntity entity) {
        final AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        final double maxHealth =
                maxHealthAttribute == null
                        ? Math.max(1.0, entity.getHealth())
                        : maxHealthAttribute.getValue();
        final double raw =
                baseXp + healthScale * Math.pow(Math.max(1.0, maxHealth), healthExponent);
        final double multiplier = entityMultipliers.getOrDefault(entity.getType().name(), 1.0);
        return clamp(raw * multiplier, minimumXp, maximumXp);
    }

    @NotNull
    public Sharing getSharing() {
        return sharing;
    }

    @NotNull
    public LevelBonus getLevelBonus() {
        return levelBonus;
    }

    private static CombatXpConfig defaults() {
        return new CombatXpConfig(
                2.0,
                0.35,
                0.80,
                1.0,
                750.0,
                Collections.emptyMap(),
                Sharing.defaults(),
                LevelBonus.disabled()
        );
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Sharing(
            boolean enabled,
            long contributionTimeoutMillis,
            double minimumContribution,
            boolean countOverkill,
            boolean groupScalingEnabled,
            double perExtraPlayer,
            double maxGroupMultiplier
    ) {
        @NotNull
        private static Sharing from(@Nullable ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }

            final ConfigurationSection group = section.getConfigurationSection("group_scaling");
            return new Sharing(
                    section.getBoolean("enabled", true),
                    Math.max(1L, section.getLong("contribution_timeout_seconds", 45L)) * 1000L,
                    clamp(section.getDouble("minimum_contribution", 0.02), 0.0, 1.0),
                    section.getBoolean("count_overkill", false),
                    group != null && group.getBoolean("enabled", false),
                    group == null ? 0.05 : Math.max(0.0, group.getDouble("per_extra_player", 0.05)),
                    group == null ? 1.25 : Math.max(1.0, group.getDouble("max_multiplier", 1.25))
            );
        }

        @NotNull
        private static Sharing defaults() {
            return new Sharing(true, 45000L, 0.02, false, false, 0.05, 1.25);
        }

        public double groupMultiplier(int players) {
            if (!groupScalingEnabled || players <= 1) {
                return 1.0;
            }
            return Math.min(maxGroupMultiplier, 1.0 + (players - 1) * perExtraPlayer);
        }
    }

    public static final class LevelBonus {
        private final boolean enabled;
        private final double curveExponent;
        private final CriticalDamageBonus criticalDamageBonus;
        private final Map<String, Rule> rules;

        private LevelBonus(
                boolean enabled,
                double curveExponent,
                @NotNull CriticalDamageBonus criticalDamageBonus,
                @NotNull Map<String, Rule> rules
        ) {
            this.enabled = enabled;
            this.curveExponent = curveExponent;
            this.criticalDamageBonus = criticalDamageBonus;
            this.rules = Collections.unmodifiableMap(rules);
        }

        @NotNull
        private static LevelBonus from(@Nullable ConfigurationSection section) {
            if (section == null || !section.getBoolean("enabled", false)) {
                return disabled();
            }

            final Map<String, Rule> rules = new HashMap<>();
            final ConfigurationSection mobs = section.getConfigurationSection("mobs");
            if (mobs != null) {
                for (String entityType : mobs.getKeys(false)) {
                    final ConfigurationSection rule = mobs.getConfigurationSection(entityType);
                    if (rule == null) {
                        continue;
                    }

                    rules.put(
                            entityType.trim().toUpperCase(Locale.ROOT),
                            new Rule(
                                    Math.max(1.0, rule.getDouble("max_multiplier", 1.0)),
                                    Math.max(0L, rule.getLong("claim_interval_seconds", 0L)) * 1000L,
                                    Math.max(1, rule.getInt("minimum_level", 1)),
                                    Math.max(0, rule.getInt("required_leveltools_critical_hits", 0))
                            )
                    );
                }
            }

            return new LevelBonus(
                    true,
                    Math.max(0.01, section.getDouble("curve_exponent", 1.25)),
                    CriticalDamageBonus.from(section.getConfigurationSection("critical_damage_bonus")),
                    rules
            );
        }

        @NotNull
        private static LevelBonus disabled() {
            return new LevelBonus(
                    false,
                    1.25,
                    CriticalDamageBonus.disabled(),
                    Collections.emptyMap()
            );
        }

        @Nullable
        public Rule getRule(@NotNull String entityType) {
            return enabled ? rules.get(entityType.toUpperCase(Locale.ROOT)) : null;
        }

        public double multiplier(
                @NotNull Rule rule,
                double normalizedProgress,
                double criticalDamageShare
        ) {
            final double progress = clamp(normalizedProgress, 0.0, 1.0);
            final double levelMultiplier =
                    1.0 + (rule.maxMultiplier() - 1.0) * Math.pow(progress, curveExponent);
            return levelMultiplier * criticalDamageBonus.multiplier(criticalDamageShare);
        }
    }

    public record CriticalDamageBonus(
            boolean enabled,
            double maxMultiplier,
            double curveExponent
    ) {
        @NotNull
        private static CriticalDamageBonus from(@Nullable ConfigurationSection section) {
            if (section == null || !section.getBoolean("enabled", false)) {
                return disabled();
            }

            return new CriticalDamageBonus(
                    true,
                    Math.max(1.0, section.getDouble("max_multiplier", 1.10)),
                    Math.max(0.01, section.getDouble("curve_exponent", 1.0))
            );
        }

        @NotNull
        private static CriticalDamageBonus disabled() {
            return new CriticalDamageBonus(false, 1.0, 1.0);
        }

        public double multiplier(double criticalDamageShare) {
            if (!enabled) {
                return 1.0;
            }

            final double share = clamp(criticalDamageShare, 0.0, 1.0);
            return 1.0
                    + (maxMultiplier - 1.0) * Math.pow(share, curveExponent);
        }
    }

    public record Rule(
            double maxMultiplier,
            long claimIntervalMillis,
            int minimumLevel,
            int requiredCriticalHits
    ) {
    }
}
