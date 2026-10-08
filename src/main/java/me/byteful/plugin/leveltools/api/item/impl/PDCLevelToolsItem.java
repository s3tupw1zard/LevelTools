package me.byteful.plugin.leveltools.api.item.impl;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PDCLevelToolsItem implements LevelToolsItem {
    @NotNull
    public static final NamespacedKey
            LEVEL_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "levelToolsLevel"),
            XP_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "levelToolsXp"),
            LAST_REWARD_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "levelToolsReward"),
            ITEM_ID_KEY = new NamespacedKey(LevelToolsPlugin.getInstance(), "item_id");

    @NotNull
    private ItemStack stack;
    @NotNull
    private Map<Enchantment, Integer> enchantments;
    @NotNull
    private Map<String, Double> attributes;
    private int level;
    private double xp;
    private int lastHandledReward;
    @NotNull
    private String itemId;

    public PDCLevelToolsItem(@NotNull ItemStack stack) {
        this(stack, stack.getItemMeta());
    }

    public PDCLevelToolsItem(@NotNull ItemStack stack, @Nullable ItemMeta meta) {
        this.stack = stack;
        this.enchantments = new HashMap<>();
        this.attributes = new HashMap<>();
        readState(meta);
    }

    private void readState(@Nullable ItemMeta meta) {
        if (meta == null) {
            level = 1;
            xp = 0.0D;
            lastHandledReward = -1;
            itemId = UUID.randomUUID().toString();

            return;
        }

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final Integer storedLevel = pdc.get(LEVEL_KEY, PersistentDataType.INTEGER);
        final Double storedXp = pdc.get(XP_KEY, PersistentDataType.DOUBLE);
        final Integer storedLastReward = pdc.get(LAST_REWARD_KEY, PersistentDataType.INTEGER);
        final String storedItemId = pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
        level = storedLevel == null ? 1 : Math.max(storedLevel, 1);
        xp = storedXp == null ? 0.0D : Math.max(storedXp, 0.0D);
        lastHandledReward = storedLastReward == null ? -1 : storedLastReward;
        itemId = storedItemId == null ? UUID.randomUUID().toString() : storedItemId;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return getItemStack(getMaxXp());
    }

    @Override
    public @NotNull ItemStack getItemStack(double maxXp) {
        final ItemStack built = stack.clone();
        final ItemMeta meta = built.getItemMeta();
        assert meta != null : "ItemMeta is null! Should not happen.";

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        pdc.set(XP_KEY, PersistentDataType.DOUBLE, xp);
        pdc.set(LAST_REWARD_KEY, PersistentDataType.INTEGER, lastHandledReward);
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        LevelToolsUtil.applyCalculatedDurability(built, meta, level);
        LevelToolsUtil.applyDisplay(built, meta, enchantments, level, xp, maxXp);
        applyAttributes(meta);
        built.setItemMeta(meta);

        return built;
    }

    private void applyAttributes(@NotNull ItemMeta meta) {
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            final String name = entry.getKey();
            final Registry<Attribute> attributeRegistry =
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE);
            final Attribute attribute = resolveAttribute(attributeRegistry, name);
            final NamespacedKey attributeKey = attributeRegistry.getKeyOrThrow(attribute);
            final NamespacedKey modifierKey =
                    new NamespacedKey(LevelToolsPlugin.getInstance(), "reward/" + attributeKey.getKey());

            final Collection<AttributeModifier> existing = meta.getAttributeModifiers(attribute);
            if (existing != null) {
                for (AttributeModifier modifier : existing) {
                    if (modifierKey.equals(modifier.getKey())) {
                        meta.removeAttributeModifier(attribute, modifier);
                    }
                }
            }

            meta.addAttributeModifier(
                    attribute,
                    new AttributeModifier(
                            modifierKey,
                            entry.getValue(),
                            AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private static Attribute resolveAttribute(
            @NotNull Registry<Attribute> attributeRegistry, @NotNull String configuredName) {
        String normalized = configuredName.trim().toLowerCase(Locale.ROOT);
        final NamespacedKey key;

        if (normalized.contains(":")) {
            key = NamespacedKey.fromString(normalized);
        } else {
            final int legacyPrefixSeparator = normalized.indexOf('.');
            if (legacyPrefixSeparator >= 0) {
                normalized = normalized.substring(legacyPrefixSeparator + 1);
            }
            key = NamespacedKey.minecraft(normalized.replace('.', '_'));
        }

        if (key == null) {
            throw new IllegalArgumentException("Invalid attribute key: " + configuredName);
        }

        final Attribute attribute = attributeRegistry.get(key);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute: " + configuredName);
        }

        return attribute;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(level, 1);
    }

    @Override
    public double getXp() {
        return xp;
    }

    @Override
    public void setXp(double xp) {
        this.xp = Math.max(xp, 0.0D);
    }

    @Override
    public int getLastHandledReward() {
        return lastHandledReward;
    }

    @Override
    public void setLastHandledReward(int rewardKey) {
        this.lastHandledReward = rewardKey;
    }

    @Override
    public void enchant(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }

    @Override
    public void modifyAttribute(String attribute, double modifier) {
        attributes.put(attribute, modifier);
    }

    @NotNull
    public String getItemId() {
        return itemId;
    }

    public void setItemId(@NotNull String itemId) {
        this.itemId = itemId;
    }

    @NotNull
    public ItemStack getStack() {
        return stack;
    }

    public void setStack(@NotNull ItemStack stack) {
        this.stack = stack;
        readState(stack.getItemMeta());
    }

    @NotNull
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public @NotNull Map<String, Double> getAttributes() {
        return attributes;
    }

    public void setAttributes(@NotNull Map<String, Double> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PDCLevelToolsItem that = (PDCLevelToolsItem) o;
        return level == that.level
                && Double.compare(that.xp, xp) == 0
                && lastHandledReward == that.lastHandledReward
                && stack.equals(that.stack)
                && enchantments.equals(that.enchantments)
                && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, enchantments, attributes, level, xp, lastHandledReward);
    }

    @Override
    public String toString() {
        return "PDCLevelToolsItem{"
                + "stack="
                + stack
                + ", enchantments="
                + enchantments
                + ", attributes="
                + attributes
                + ", level="
                + level
                + ", xp="
                + xp
                + ", lastHandledReward="
                + lastHandledReward
                + '}';
    }
}
