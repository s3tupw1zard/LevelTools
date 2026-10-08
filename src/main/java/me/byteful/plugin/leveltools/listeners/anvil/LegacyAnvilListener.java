package me.byteful.plugin.leveltools.listeners.anvil;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.AnvilCombineMode;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.model.LevelAndXPModel;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static me.byteful.plugin.leveltools.listeners.anvil.AnvilHelper.getResultItem;
import static me.byteful.plugin.leveltools.listeners.anvil.AnvilHelper.shouldBlockEnchantedBookResult;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class LegacyAnvilListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof AnvilInventory)) return;

        final int rawSlot = e.getRawSlot();
        if (rawSlot != 0 && rawSlot != 1 && rawSlot != 2) return;

        final AnvilInventory inv = (AnvilInventory) e.getInventory();
        final ItemStack firstItem = inv.getItem(0);
        final ItemStack secondItem = inv.getItem(1);
        final ItemStack result = inv.getItem(2);

        if (shouldBlockEnchantedBookResult(firstItem, secondItem, result)) {
            if (rawSlot == 2) {
                e.setCancelled(true);
            }
            inv.setItem(2, null);
            return;
        }

        if (!isHandledAnvilAction(firstItem, secondItem, result)) {
            return;
        }

        inv.setItem(2, createUpdatedResult(firstItem, secondItem, result, e.getWhoClicked()));
    }

    private static ItemStack preserveBaseProgression(ItemStack baseItem, ItemStack vanillaResult) {
        final ItemStack protectedResult = vanillaResult.clone();

        for (Map.Entry<Enchantment, Integer> entry : baseItem.getEnchantments().entrySet()) {
            if (protectedResult.getEnchantmentLevel(entry.getKey()) < entry.getValue()) {
                protectedResult.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        final ItemMeta sourceMeta = baseItem.getItemMeta();
        final ItemMeta targetMeta = protectedResult.getItemMeta();
        if (sourceMeta == null || targetMeta == null) {
            return protectedResult;
        }

        final Multimap<Attribute, AttributeModifier> sourceModifiers = sourceMeta.getAttributeModifiers();
        if (sourceModifiers == null || sourceModifiers.isEmpty()) {
            return protectedResult;
        }

        for (Map.Entry<Attribute, AttributeModifier> entry : sourceModifiers.entries()) {
            final AttributeModifier sourceModifier = entry.getValue();
            if (!isLevelToolsAttributeModifier(sourceModifier)) {
                continue;
            }

            final Collection<AttributeModifier> targetModifiers =
                    targetMeta.getAttributeModifiers(entry.getKey());
            if (targetModifiers != null) {
                for (AttributeModifier targetModifier : targetModifiers) {
                    if (sourceModifier.getUniqueId().equals(targetModifier.getUniqueId())) {
                        targetMeta.removeAttributeModifier(entry.getKey(), targetModifier);
                    }
                }
            }

            targetMeta.addAttributeModifier(entry.getKey(), sourceModifier);
        }

        protectedResult.setItemMeta(targetMeta);
        return protectedResult;
    }

    private static boolean isLevelToolsAttributeModifier(AttributeModifier modifier) {
        final UUID expected = UUID.nameUUIDFromBytes(
                modifier.getName().getBytes(StandardCharsets.UTF_8));
        return expected.equals(modifier.getUniqueId());
    }

    static boolean isHandledAnvilAction(ItemStack firstItem, ItemStack secondItem, ItemStack result) {
        return result != null
                && LevelToolsUtil.isSupportedTool(result.getType())
                && firstItem != null
                && secondItem != null
                && LevelToolsUtil.isSupportedTool(firstItem.getType());
    }

    static ItemStack createUpdatedResult(
            ItemStack firstItem, ItemStack secondItem, ItemStack result, HumanEntity viewer) {
        final ItemStack protectedResult = preserveBaseProgression(firstItem, result);
        final LevelToolsItem finalItem = LevelToolsUtil.createLevelToolsItem(protectedResult);

        if (LevelToolsUtil.isSupportedTool(secondItem.getType())) {
            final AnvilCombineMode mode = LevelToolsPlugin.getInstance().getAnvilCombineMode();
            final LevelAndXPModel first =
                    LevelAndXPModel.fromItem(LevelToolsUtil.createLevelToolsItem(firstItem));
            final LevelAndXPModel second =
                    LevelAndXPModel.fromItem(LevelToolsUtil.createLevelToolsItem(secondItem));
            final LevelAndXPModel finished = mode.getHandler().apply(first, second);
            finalItem.setLevel(finished.getLevel());
            finalItem.setXp(finished.getXp());
        } else {
            final LevelToolsItem original = LevelToolsUtil.createLevelToolsItem(firstItem);
            finalItem.setLevel(original.getLevel());
            finalItem.setXp(original.getXp());
            finalItem.setLastHandledReward(original.getLastHandledReward());
        }

        return getResultItem(finalItem, viewer, result);
    }
}
