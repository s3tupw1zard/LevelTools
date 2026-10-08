package me.byteful.plugin.leveltools.listeners;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.profile.ProfileManager;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.profile.stat.CalculatedStats;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class DefenseListener implements Listener {
    private final ProfileManager profileManager;

    public DefenseListener(@NotNull ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (LevelToolsPlugin.getInstance()
                .getConfigManager()
                .getSettings()
                .getDisabledWorlds()
                .contains(player.getWorld().getName())) {
            return;
        }

        double remainingDamage = 1.0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType() == Material.AIR) {
                continue;
            }

            final ItemProfile profile = profileManager.getProfileForMaterial(armor.getType());
            if (profile == null) {
                continue;
            }

            final LevelToolsItem item = LevelToolsUtil.createLevelToolsItem(armor);
            final CalculatedStats stats =
                    LevelToolsPlugin.getInstance()
                            .getStatCalculator()
                            .calculate(armor, profile, item.getLevel());
            remainingDamage *= 1.0 - stats.defense();
        }

        event.setDamage(Math.max(0.0, event.getDamage() * remainingDamage));
    }
}
