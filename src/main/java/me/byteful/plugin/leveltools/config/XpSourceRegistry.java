package me.byteful.plugin.leveltools.config;

import me.byteful.plugin.leveltools.api.trigger.TriggerContext;
import me.byteful.plugin.leveltools.api.trigger.TriggerIds;
import me.byteful.plugin.leveltools.profile.trigger.XpModifierConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class XpSourceRegistry {
    private final Map<String, XpModifierConfig> sources;

    private XpSourceRegistry(@NotNull Map<String, XpModifierConfig> sources) {
        this.sources = Collections.unmodifiableMap(sources);
    }

    @NotNull
    public static XpSourceRegistry load(@NotNull FileConfiguration config) {
        final Map<String, XpModifierConfig> sources = new HashMap<>();
        final ConfigurationSection root = config.getConfigurationSection("sources");
        if (root == null) {
            return new XpSourceRegistry(sources);
        }

        for (String profileId : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(profileId);
            if (section == null) {
                continue;
            }

            final ConfigurationSection defaultSection = section.getConfigurationSection("default");
            final double defaultMin =
                    defaultSection == null ? 1.0 : defaultSection.getDouble("min", 1.0);
            final double defaultMax =
                    defaultSection == null ? defaultMin : defaultSection.getDouble("max", defaultMin);

            final Map<String, XpModifierConfig.XpModifierRange> custom = new HashMap<>();
            final ConfigurationSection customSection = section.getConfigurationSection("custom");
            if (customSection != null) {
                for (String source : customSection.getKeys(false)) {
                    final ConfigurationSection range = customSection.getConfigurationSection(source);
                    if (range == null) {
                        continue;
                    }

                    custom.put(
                            source.trim().toUpperCase(Locale.ROOT),
                            new XpModifierConfig.XpModifierRange(
                                    range.getDouble("min", defaultMin),
                                    range.getDouble("max", defaultMax)
                            )
                    );
                }
            }

            sources.put(
                    profileId.trim().toLowerCase(Locale.ROOT),
                    new XpModifierConfig(defaultMin, defaultMax, custom)
            );
        }

        return new XpSourceRegistry(sources);
    }

    public boolean hasSource(@NotNull String profileId) {
        return sources.containsKey(profileId.trim().toLowerCase(Locale.ROOT));
    }

    public double calculate(@NotNull String profileId, @NotNull TriggerContext context) {
        final XpModifierConfig config =
                sources.get(profileId.trim().toLowerCase(Locale.ROOT));
        if (config == null) {
            return 0.0;
        }

        return config.calculateModifier(resolveSourceKey(context));
    }

    @NotNull
    private static String resolveSourceKey(@NotNull TriggerContext context) {
        if (TriggerIds.FARMING.equals(context.getTriggerId())
                && context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            return "TILL";
        }

        final Block block = context.getSourceAs(Block.class);
        if (block != null) {
            return block.getType().name();
        }

        final Material material = context.getSourceAs(Material.class);
        if (material != null) {
            return material.name();
        }

        final ItemStack itemStack = context.getSourceAs(ItemStack.class);
        if (itemStack != null) {
            return itemStack.getType().name();
        }

        final Entity entity = context.getSourceAs(Entity.class);
        if (entity instanceof Item item) {
            return item.getItemStack().getType().name();
        }
        if (entity != null) {
            return entity.getType().name();
        }

        return "DEFAULT";
    }
}
