package me.byteful.plugin.leveltools.config;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.byteful.plugin.leveltools.profile.stat.StatType;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class EnchantmentModifierRegistry {
    private final Map<String, Map<StatType, Modifier>> modifiers;

    private EnchantmentModifierRegistry(@NotNull Map<String, Map<StatType, Modifier>> modifiers) {
        this.modifiers = modifiers;
    }

    @NotNull
    public static EnchantmentModifierRegistry load(@NotNull FileConfiguration config) {
        final Map<String, Map<StatType, Modifier>> result = new HashMap<>();
        final ConfigurationSection root = config.getConfigurationSection("enchantments");
        if (root == null) {
            return new EnchantmentModifierRegistry(result);
        }

        for (String enchantmentId : root.getKeys(false)) {
            final ConfigurationSection enchantmentSection = root.getConfigurationSection(enchantmentId);
            if (enchantmentSection == null) {
                continue;
            }

            final ConfigurationSection stats = enchantmentSection.getConfigurationSection("stats");
            if (stats == null) {
                continue;
            }

            final Map<StatType, Modifier> perStat = new EnumMap<>(StatType.class);
            for (String statKey : stats.getKeys(false)) {
                final ConfigurationSection stat = stats.getConfigurationSection(statKey);
                if (stat == null) {
                    continue;
                }

                perStat.put(
                        StatType.fromConfigKey(statKey),
                        new Modifier(
                                stat.getDouble("per_level", 0.0),
                                stat.getDouble("cap", Double.POSITIVE_INFINITY)
                        )
                );
            }
            result.put(normalizeEnchantmentId(enchantmentId), perStat);
        }

        return new EnchantmentModifierRegistry(result);
    }

    public double calculate(@NotNull ItemStack item, @NotNull StatType type) {
        final Registry<Enchantment> registry =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        double total = 0.0;

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            final NamespacedKey key = registry.getKeyOrThrow(entry.getKey());
            final Map<StatType, Modifier> enchantmentModifiers = modifiers.get(key.toString());
            if (enchantmentModifiers == null) {
                continue;
            }

            final Modifier modifier = enchantmentModifiers.get(type);
            if (modifier != null) {
                total += modifier.valueFor(entry.getValue());
            }
        }

        return total;
    }

    @NotNull
    private static String normalizeEnchantmentId(@NotNull String id) {
        final String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private record Modifier(double perLevel, double cap) {
        private double valueFor(int level) {
            return Math.min(cap, perLevel * Math.max(0, level));
        }
    }
}
