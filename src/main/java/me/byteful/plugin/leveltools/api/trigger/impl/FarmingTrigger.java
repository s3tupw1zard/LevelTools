package me.byteful.plugin.leveltools.api.trigger.impl;

import com.cryptomorin.xseries.XMaterial;
import me.byteful.plugin.leveltools.api.trigger.Trigger;
import me.byteful.plugin.leveltools.api.trigger.TriggerContext;
import me.byteful.plugin.leveltools.api.trigger.TriggerIds;
import me.byteful.plugin.leveltools.profile.trigger.TriggerProfile;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public final class FarmingTrigger implements Trigger {
    private static final Set<XMaterial> TILLABLE_BLOCKS = EnumSet.of(
            XMaterial.DIRT,
            XMaterial.GRASS_BLOCK,
            XMaterial.DIRT_PATH,
            XMaterial.COARSE_DIRT,
            XMaterial.ROOTED_DIRT
    );

    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.FARMING;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        final Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return false;
        }

        final TriggerProfile profile = context.getTriggerProfile();
        final Material blockType = block.getType();

        if (context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            if (!TILLABLE_BLOCKS.contains(XMaterial.matchXMaterial(blockType))) {
                return false;
            }
            return profile.isSourceAllowed("TILL");
        }

        if (context.getOriginalEventAs(BlockBreakEvent.class) != null) {
            if (!isMatureCropSource(block)) {
                return false;
            }

            return profile.isSourceAllowed(blockType.name());
        }

        return false;
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        final Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return 0;
        }

        final TriggerProfile profile = context.getTriggerProfile();

        if (context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            return profile.calculateXpModifier("TILL");
        }

        return profile.calculateXpModifier(block.getType().name());
    }

    public static boolean isMatureCropSource(@NotNull Block block) {
        final BlockData data = block.getBlockData();
        return data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }
}
