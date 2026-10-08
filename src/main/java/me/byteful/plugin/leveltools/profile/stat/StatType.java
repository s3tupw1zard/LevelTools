package me.byteful.plugin.leveltools.profile.stat;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum StatType {
    DAMAGE("damage"),
    CRITICAL_CHANCE("critical_chance"),
    CRITICAL_DAMAGE("critical_damage"),
    DEFENSE("defense"),
    CRITICAL_DEFENSE("critical_defense"),
    DURABILITY("durability");

    private final String configKey;

    StatType(@NotNull String configKey) {
        this.configKey = configKey;
    }

    @NotNull
    public String getConfigKey() {
        return configKey;
    }

    public static StatType fromConfigKey(@NotNull String key) {
        final String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (StatType type : values()) {
            if (type.configKey.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown stat: " + key);
    }
}
