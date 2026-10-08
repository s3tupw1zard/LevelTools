package me.byteful.plugin.leveltools.profile.stat;

public record CalculatedStats(
        double damageBonus,
        double criticalChance,
        double criticalDamage,
        double defense,
        double criticalDefense,
        double durabilityBonus
) {
    public static CalculatedStats empty() {
        return new CalculatedStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
}
