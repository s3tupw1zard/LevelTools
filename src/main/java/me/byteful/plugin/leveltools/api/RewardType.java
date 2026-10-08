package me.byteful.plugin.leveltools.api;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum RewardType {
    COMMAND("command", false) {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    String.join(" ", Arrays.copyOfRange(split, 1, split.length))
                            .replace("{player}", player.getName())
                            .replace("%player%", player.getName()));
        }
    },
    PLAYER_COMMAND("player-command", false) {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            player.performCommand(
                    String.join(" ", Arrays.copyOfRange(split, 1, split.length))
                            .replace("{player}", player.getName())
                            .replace("%player%", player.getName()));
        }
    },
    PLAYER_OPCOMMAND("player-opcommand", false) {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            final boolean wasOp = player.isOp();

            try {
                if (!wasOp) {
                    player.setOp(true);
                }
                PLAYER_COMMAND.apply(tool, split, player);
            } finally {
                player.setOp(wasOp);
            }
        }
    },
    ENCHANT("enchant") {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            if (split.length < 3 || !isInteger(split[2])) {
                return;
            }

            resolveEnchantment(split[1]).ifPresent(
                    enchantment -> tool.enchant(enchantment, Integer.parseInt(split[2])));
        }
    },
    ENCHANT_2("enchant2") {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            if (split.length < 3 || !isInteger(split[2])) {
                return;
            }

            final int level = Integer.parseInt(split[2]);
            resolveEnchantment(split[1]).ifPresent(enchantment -> {
                if (tool.getItemStack().getEnchantmentLevel(enchantment) < level) {
                    tool.enchant(enchantment, level);
                }
            });
        }
    },
    ENCHANT_3("enchant3") {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            if (split.length < 3 || !isInteger(split[2])) {
                return;
            }

            final int level = Integer.parseInt(split[2]);
            resolveEnchantment(split[1]).ifPresent(enchantment -> {
                final int currentLevel = tool.getItemStack().getEnchantmentLevel(enchantment);
                tool.enchant(enchantment, currentLevel + level);
            });
        }
    },
    ATTRIBUTE("attribute") {
        @Override
        public void apply(
                @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player) {
            if (split.length < 3 || !isDouble(split[2])) {
                return;
            }

            String attribute = split[1];
            final double modifier = Double.parseDouble(split[2]);

            if (attribute.indexOf('_') != attribute.lastIndexOf('_')) {
                attribute = attribute.toLowerCase(Locale.ROOT).replaceFirst("_+", ".").trim();
            }

            tool.modifyAttribute(attribute, modifier);
        }
    };

    @NotNull
    private final String configKey;
    private final boolean shouldUpdate;

    RewardType(@NotNull String configKey) {
        this(configKey, true);
    }

    RewardType(@NotNull String configKey, boolean shouldUpdate) {
        this.configKey = configKey;
        this.shouldUpdate = shouldUpdate;
    }

    @NotNull
    public static Optional<RewardType> fromConfigKey(@NotNull String configKey) {
        for (RewardType value : values()) {
            if (value.configKey.equals(configKey)) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    @NotNull
    private static Optional<Enchantment> resolveEnchantment(@NotNull String configuredName) {
        final Registry<Enchantment> registry =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        final String normalized = configuredName.trim().toLowerCase(Locale.ROOT);
        final NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);

        return key == null ? Optional.empty() : Optional.ofNullable(registry.get(key));
    }

    private static boolean isInteger(@NotNull String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isDouble(@NotNull String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public abstract void apply(
            @NotNull LevelToolsItem tool, @NotNull String[] split, @NotNull Player player);

    @NotNull
    public String getConfigKey() {
        return configKey;
    }

    public boolean isShouldUpdate() {
        return shouldUpdate;
    }
}
