package me.byteful.plugin.leveltools.profile.progression;

import org.jetbrains.annotations.NotNull;

public final class ProgressionProfile {
    private final String id;
    private final int maxLevel;
    private final int referenceMaxLevel;
    private final double firstLevelXp;
    private final double finalLevelXp;
    private final double curveExponent;
    private final double maxLevelInfluence;

    public ProgressionProfile(
            @NotNull String id,
            int maxLevel,
            int referenceMaxLevel,
            double firstLevelXp,
            double finalLevelXp,
            double curveExponent,
            double maxLevelInfluence
    ) {
        if (maxLevel < 2) {
            throw new IllegalArgumentException("max_level must be at least 2");
        }
        if (referenceMaxLevel < 2) {
            throw new IllegalArgumentException("reference_max_level must be at least 2");
        }
        if (firstLevelXp <= 0.0 || finalLevelXp <= 0.0) {
            throw new IllegalArgumentException("XP requirements must be positive");
        }
        if (curveExponent <= 0.0) {
            throw new IllegalArgumentException("curve_exponent must be positive");
        }
        if (maxLevelInfluence < 0.0 || maxLevelInfluence > 1.0) {
            throw new IllegalArgumentException("max_level_influence must be between 0 and 1");
        }

        this.id = id;
        this.maxLevel = maxLevel;
        this.referenceMaxLevel = referenceMaxLevel;
        this.firstLevelXp = firstLevelXp;
        this.finalLevelXp = finalLevelXp;
        this.curveExponent = curveExponent;
        this.maxLevelInfluence = maxLevelInfluence;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int clampLevel(int level) {
        return Math.max(1, Math.min(level, maxLevel));
    }

    public double normalizedProgress(int level) {
        return clamp01((clampLevel(level) - 1.0) / (maxLevel - 1.0));
    }

    public double xpRequiredForLevel(int currentLevel) {
        if (currentLevel >= maxLevel) {
            return xpRequiredForLevel(maxLevel - 1);
        }

        final int level = Math.max(1, currentLevel);
        final double transitionProgress = maxLevel <= 2
                ? 1.0
                : clamp01((level - 1.0) / (maxLevel - 2.0));
        final double shapedProgress = Math.pow(transitionProgress, curveExponent);
        final double referenceXp =
                firstLevelXp + (finalLevelXp - firstLevelXp) * shapedProgress;

        // Keep total grind controllable when max_level changes. A value of:
        // 0.0 ~= same total XP, 0.5 = sublinear, 1.0 ~= linear total scaling.
        final double transitionRatio =
                (maxLevel - 1.0) / Math.max(1.0, referenceMaxLevel - 1.0);
        final double levelCountScale =
                Math.pow(transitionRatio, maxLevelInfluence - 1.0);

        return roundOneDecimal(Math.max(1.0, referenceXp * levelCountScale));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
