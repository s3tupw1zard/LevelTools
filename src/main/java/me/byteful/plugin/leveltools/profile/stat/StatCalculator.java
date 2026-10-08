package me.byteful.plugin.leveltools.profile.stat;

import me.byteful.plugin.leveltools.config.EnchantmentModifierRegistry;
import me.byteful.plugin.leveltools.profile.ProfileManager;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.profile.progression.ProgressionProfile;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StatCalculator {
    private final ProfileManager profileManager;
    private final EnchantmentModifierRegistry enchantmentModifiers;

    public StatCalculator(
            @NotNull ProfileManager profileManager,
            @NotNull EnchantmentModifierRegistry enchantmentModifiers
    ) {
        this.profileManager = profileManager;
        this.enchantmentModifiers = enchantmentModifiers;
    }

    @NotNull
    public CalculatedStats calculate(
            @NotNull ItemStack item,
            @Nullable ItemProfile itemProfile,
            int level
    ) {
        if (itemProfile == null) {
            return CalculatedStats.empty();
        }

        final ProgressionProfile progression = profileManager.getProgressionProfileFor(itemProfile);
        final StatProfile statProfile = profileManager.getStatProfileFor(itemProfile);
        if (progression == null || statProfile == null) {
            return CalculatedStats.empty();
        }

        final double progress = progression.normalizedProgress(level);
        final double damage = value(item, statProfile, StatType.DAMAGE, progress);
        final double critChance =
                clamp(value(item, statProfile, StatType.CRITICAL_CHANCE, progress), 0.0, 1.0);
        final double critDamage =
                Math.max(0.0, value(item, statProfile, StatType.CRITICAL_DAMAGE, progress));
        final double defense =
                clamp(value(item, statProfile, StatType.DEFENSE, progress), 0.0, 0.95);
        final double critDefense =
                clamp(value(item, statProfile, StatType.CRITICAL_DEFENSE, progress), 0.0, 0.95);
        final double durability =
                Math.max(0.0, value(item, statProfile, StatType.DURABILITY, progress));

        return new CalculatedStats(
                damage,
                critChance,
                critDamage,
                defense,
                critDefense,
                durability
        );
    }

    private double value(
            @NotNull ItemStack item,
            @NotNull StatProfile profile,
            @NotNull StatType type,
            double progress
    ) {
        return profile.evaluate(type, progress) + enchantmentModifiers.calculate(item, type);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
