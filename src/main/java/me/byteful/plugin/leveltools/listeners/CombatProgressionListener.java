package me.byteful.plugin.leveltools.listeners;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.api.trigger.TriggerIds;
import me.byteful.plugin.leveltools.config.CombatXpConfig;
import me.byteful.plugin.leveltools.profile.ProfileManager;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.profile.progression.ProgressionProfile;
import me.byteful.plugin.leveltools.profile.stat.CalculatedStats;
import me.byteful.plugin.leveltools.util.LevelToolsUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class CombatProgressionListener implements Listener {
    private final ProfileManager profileManager;
    private final XPHandler xpHandler;
    private final Map<EntityDamageByEntityEvent, HitContext> pendingHits =
            new ConcurrentHashMap<>();
    private final Map<UUID, MobContribution> contributions = new ConcurrentHashMap<>();

    public CombatProgressionListener(@NotNull ProfileManager profileManager) {
        this.profileManager = profileManager;
        this.xpHandler = new XPHandler(profileManager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatStats(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        final AttackSource source = resolveAttackSource(event);
        if (source == null || !source.player().hasPermission("leveltools.enabled")) {
            return;
        }
        if (LevelToolsPlugin.getInstance()
                .getConfigManager()
                .getSettings()
                .getDisabledWorlds()
                .contains(source.player().getWorld().getName())) {
            return;
        }

        final PreparedWeapon prepared = prepareWeapon(source);
        if (prepared == null) {
            return;
        }

        final ItemProfile itemProfile = profileManager.getProfileForMaterial(prepared.item().getType());
        if (itemProfile == null || !isCombatTargetAllowed(itemProfile, target)) {
            return;
        }

        final LevelToolsItem tool = LevelToolsUtil.createLevelToolsItem(prepared.item());
        final CalculatedStats attackStats =
                LevelToolsPlugin.getInstance()
                        .getStatCalculator()
                        .calculate(prepared.item(), itemProfile, tool.getLevel());
        final DefenseStats defense = calculateDefense(target);

        final double normalDamage = event.getDamage() * (1.0 + attackStats.damageBonus());
        final boolean levelToolsCritical =
                !event.isCritical()
                        && attackStats.criticalChance() > 0.0
                        && ThreadLocalRandom.current().nextDouble() < attackStats.criticalChance();
        final double criticalBonus = levelToolsCritical
                ? normalDamage
                        * attackStats.criticalDamage()
                        * (1.0 - defense.criticalDefense())
                : 0.0;
        event.setDamage(Math.max(0.0, normalDamage + criticalBonus));

        final ProgressionProfile progression = profileManager.getProgressionProfileFor(itemProfile);
        pendingHits.put(
                event,
                new HitContext(
                        source.player().getUniqueId(),
                        prepared.itemId(),
                        prepared.item().getType(),
                        prepared.projectileId(),
                        tool.getLevel(),
                        progression == null ? 0.0 : progression.normalizedProgress(tool.getLevel()),
                        levelToolsCritical
                )
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onContribution(@NotNull EntityDamageByEntityEvent event) {
        final HitContext hit = pendingHits.remove(event);
        if (hit == null || event.isCancelled()
                || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        final CombatXpConfig config = LevelToolsPlugin.getInstance().getCombatXpConfig();
        final double finalDamage = Math.max(0.0, event.getFinalDamage());
        final double effectiveDamage = config.getSharing().countOverkill()
                ? finalDamage
                : Math.min(finalDamage, Math.max(0.0, target.getHealth()));
        if (effectiveDamage <= 0.0) {
            return;
        }

        contributions
                .computeIfAbsent(target.getUniqueId(), ignored -> new MobContribution())
                .add(hit, effectiveDamage, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        final MobContribution mob = contributions.remove(event.getEntity().getUniqueId());
        if (mob != null && !mob.entries.isEmpty()) {
            distribute(event.getEntity(), mob);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(@NotNull EntityRemoveEvent event) {
        if (event.getCause() == EntityRemoveEvent.Cause.DEATH) {
            return;
        }
        contributions.remove(event.getEntity().getUniqueId());
    }

    private void distribute(@NotNull LivingEntity entity, @NotNull MobContribution mob) {
        final CombatXpConfig config = LevelToolsPlugin.getInstance().getCombatXpConfig();
        final CombatXpConfig.Sharing sharing = config.getSharing();
        final long now = System.currentTimeMillis();

        final List<Contribution> recent = mob.entries.values().stream()
                .filter(entry -> now - entry.lastHitMillis <= sharing.contributionTimeoutMillis())
                .toList();
        if (recent.isEmpty()) {
            return;
        }

        final double allDamage = recent.stream().mapToDouble(entry -> entry.damage).sum();
        if (allDamage <= 0.0) {
            return;
        }

        final List<Contribution> qualified;
        if (sharing.enabled()) {
            qualified = recent.stream()
                    .filter(entry -> entry.damage / allDamage >= sharing.minimumContribution())
                    .toList();
        } else {
            final Contribution lastHit = recent.stream()
                    .max((left, right) -> Long.compare(left.lastHitMillis, right.lastHitMillis))
                    .orElse(null);
            qualified = lastHit == null ? List.of() : List.of(lastHit);
        }

        if (qualified.isEmpty()) {
            return;
        }

        final double qualifiedDamage = qualified.stream().mapToDouble(entry -> entry.damage).sum();
        final int playerCount =
                (int) qualified.stream().map(entry -> entry.playerId).distinct().count();
        final double pool = config.calculatePool(entity) * sharing.groupMultiplier(playerCount);

        final Map<ContributionKey, Double> bonuses =
                calculateLevelBonuses(entity, qualified, config, now);

        for (Contribution entry : qualified) {
            final Player player = Bukkit.getPlayer(entry.playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            final double share = entry.damage / qualifiedDamage;
            final ContributionKey key = new ContributionKey(entry.playerId, entry.itemId);
            award(entry, player, pool * share * bonuses.getOrDefault(key, 1.0));
        }
    }

    @NotNull
    private Map<ContributionKey, Double> calculateLevelBonuses(
            @NotNull LivingEntity entity,
            @NotNull List<Contribution> contributionsForFight,
            @NotNull CombatXpConfig config,
            long now
    ) {
        final Map<ContributionKey, Double> result = new HashMap<>();
        final CombatXpConfig.Rule rule =
                config.getLevelBonus().getRule(entity.getType().name());
        if (rule == null) {
            return result;
        }

        final Map<UUID, List<Contribution>> byPlayer = contributionsForFight.stream()
                .collect(java.util.stream.Collectors.groupingBy(entry -> entry.playerId));

        for (Map.Entry<UUID, List<Contribution>> playerEntry : byPlayer.entrySet()) {
            final Player player = Bukkit.getPlayer(playerEntry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }

            final NamespacedKey cooldownKey = new NamespacedKey(
                    LevelToolsPlugin.getInstance(),
                    "combat_bonus_" + entity.getType().name().toLowerCase(java.util.Locale.ROOT)
            );
            final PersistentDataContainer pdc = player.getPersistentDataContainer();
            final Long lastClaim = pdc.get(cooldownKey, PersistentDataType.LONG);
            if (lastClaim != null && now - lastClaim < rule.claimIntervalMillis()) {
                continue;
            }

            boolean claimed = false;
            for (Contribution contribution : playerEntry.getValue()) {
                if (contribution.level < rule.minimumLevel()
                        || contribution.criticalHits < rule.requiredCriticalHits()) {
                    continue;
                }

                final ContributionKey key =
                        new ContributionKey(contribution.playerId, contribution.itemId);
                final double criticalDamageShare = contribution.damage <= 0.0
                        ? 0.0
                        : contribution.criticalDamage / contribution.damage;
                result.put(
                        key,
                        config.getLevelBonus().multiplier(
                                rule,
                                contribution.progress,
                                criticalDamageShare
                        )
                );
                claimed = true;
            }

            if (claimed) {
                pdc.set(cooldownKey, PersistentDataType.LONG, now);
            }
        }

        return result;
    }

    private void award(@NotNull Contribution contribution, @NotNull Player player, double xp) {
        if (xp <= 0.0) {
            return;
        }

        final ResolvedWeapon resolved = resolveWeapon(player, contribution);
        if (resolved == null) {
            return;
        }

        final ItemProfile itemProfile = profileManager.getProfileForMaterial(resolved.item().getType());
        if (itemProfile == null) {
            return;
        }

        xpHandler.handleDirectCommit(
                player,
                itemProfile,
                LevelToolsUtil.createLevelToolsItem(resolved.item()),
                xp,
                resolved.committer()
        );
    }

    @Nullable
    private ResolvedWeapon resolveWeapon(
            @NotNull Player player,
            @NotNull Contribution contribution
    ) {
        if (contribution.projectileId != null) {
            final Entity projectile = Bukkit.getEntity(contribution.projectileId);
            if (projectile instanceof Trident trident) {
                final ItemStack item = trident.getItemStack();
                if (contribution.itemId.equals(LevelToolsUtil.getStoredItemId(item))) {
                    return new ResolvedWeapon(item, trident::setItemStack);
                }
            }
        }

        final ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (!contribution.itemId.equals(LevelToolsUtil.getStoredItemId(item))) {
                continue;
            }

            final int targetSlot = slot;
            return new ResolvedWeapon(
                    item,
                    updated -> player.getInventory().setItem(targetSlot, updated)
            );
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == contribution.material) {
            return new ResolvedWeapon(
                    hand,
                    updated -> player.getInventory().setItemInMainHand(updated)
            );
        }

        return null;
    }

    @Nullable
    private PreparedWeapon prepareWeapon(@NotNull AttackSource source) {
        final ItemStack weapon = source.item();
        if (weapon.getType() == Material.AIR
                || profileManager.getProfileForMaterial(weapon.getType()) == null) {
            return null;
        }

        String itemId = LevelToolsUtil.getStoredItemId(weapon);
        ItemStack prepared = weapon;

        if (itemId == null) {
            final ItemProfile itemProfile = profileManager.getProfileForMaterial(weapon.getType());
            final LevelToolsItem tool = LevelToolsUtil.createLevelToolsItem(weapon);
            prepared = LevelToolsUtil.getItemStack(tool, source.player(), itemProfile);
            itemId = LevelToolsUtil.getStoredItemId(prepared);
            if (itemId == null) {
                return null;
            }

            if (source.directEntity() instanceof Trident trident) {
                trident.setItemStack(prepared);
            } else if (source.directEntity() instanceof AbstractArrow arrow) {
                arrow.setWeapon(prepared);
                updateMatchingInventoryWeapon(source.player(), weapon, prepared);
            } else {
                source.player().getInventory().setItemInMainHand(prepared);
            }
        }

        return new PreparedWeapon(
                prepared,
                itemId,
                source.directEntity() instanceof Trident trident
                        ? trident.getUniqueId()
                        : null
        );
    }

    private void updateMatchingInventoryWeapon(
            @NotNull Player player,
            @NotNull ItemStack original,
            @NotNull ItemStack updated
    ) {
        if (player.getInventory().getItemInMainHand().getType() == original.getType()) {
            player.getInventory().setItemInMainHand(updated);
            return;
        }

        final ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            final ItemStack candidate = contents[slot];
            if (candidate != null && candidate.getType() == original.getType()) {
                player.getInventory().setItem(slot, updated);
                return;
            }
        }
    }

    private boolean isCombatTargetAllowed(
            @NotNull ItemProfile itemProfile,
            @NotNull LivingEntity target
    ) {
        return profileManager.getTriggerProfilesFor(itemProfile).stream()
                .filter(profile -> TriggerIds.ENTITY_KILL.equals(profile.getTriggerId()))
                .anyMatch(profile -> profile.isSourceAllowed(target.getType().name()));
    }

    @NotNull
    private DefenseStats calculateDefense(@NotNull LivingEntity target) {
        if (!(target instanceof Player player)) {
            return DefenseStats.NONE;
        }

        double remainingCriticalDefense = 1.0;
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
            remainingCriticalDefense *= 1.0 - stats.criticalDefense();
        }

        return new DefenseStats(1.0 - remainingCriticalDefense);
    }

    @Nullable
    private AttackSource resolveAttackSource(@NotNull EntityDamageByEntityEvent event) {
        Player player = null;
        final Entity causing = event.getDamageSource().getCausingEntity();
        if (causing instanceof Player causingPlayer) {
            player = causingPlayer;
        } else if (event.getDamager() instanceof Player directPlayer) {
            player = directPlayer;
        } else if (event.getDamager() instanceof AbstractArrow arrow
                && arrow.getShooter() instanceof Player shooter) {
            player = shooter;
        }

        if (player == null) {
            return null;
        }

        final Entity direct = event.getDamageSource().getDirectEntity();
        final ItemStack item;
        if (direct instanceof Trident trident) {
            item = trident.getItemStack();
        } else if (direct instanceof AbstractArrow arrow && arrow.getWeapon() != null) {
            item = arrow.getWeapon();
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        return new AttackSource(player, item, direct);
    }

    private record AttackSource(Player player, ItemStack item, @Nullable Entity directEntity) {
    }

    private record PreparedWeapon(ItemStack item, String itemId, @Nullable UUID projectileId) {
    }

    private record HitContext(
            UUID playerId,
            String itemId,
            Material material,
            @Nullable UUID projectileId,
            int level,
            double progress,
            boolean critical
    ) {
    }

    private record ContributionKey(UUID playerId, String itemId) {
    }

    private static final class Contribution {
        private final UUID playerId;
        private final String itemId;
        private Material material;
        private UUID projectileId;
        private int level;
        private double progress;
        private double damage;
        private double criticalDamage;
        private int criticalHits;
        private long lastHitMillis;

        private Contribution(@NotNull HitContext hit) {
            this.playerId = hit.playerId();
            this.itemId = hit.itemId();
            this.material = hit.material();
            this.projectileId = hit.projectileId();
            this.level = hit.level();
            this.progress = hit.progress();
        }

        private void add(@NotNull HitContext hit, double amount, long now) {
            damage += amount;
            lastHitMillis = now;
            material = hit.material();
            projectileId = hit.projectileId();
            level = Math.max(level, hit.level());
            progress = Math.max(progress, hit.progress());
            if (hit.critical()) {
                criticalHits++;
                criticalDamage += amount;
            }
        }
    }

    private static final class MobContribution {
        private final Map<ContributionKey, Contribution> entries = new ConcurrentHashMap<>();

        private void add(@NotNull HitContext hit, double damage, long now) {
            final ContributionKey key = new ContributionKey(hit.playerId(), hit.itemId());
            entries.computeIfAbsent(key, ignored -> new Contribution(hit))
                    .add(hit, damage, now);
        }
    }

    private record DefenseStats(double criticalDefense) {
        private static final DefenseStats NONE = new DefenseStats(0.0);
    }

    private record ResolvedWeapon(ItemStack item, Consumer<ItemStack> committer) {
    }
}
