package me.byteful.plugin.leveltools.util;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import me.byteful.plugin.leveltools.LevelToolsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import me.byteful.plugin.leveltools.api.item.LevelToolsItem;
import me.byteful.plugin.leveltools.api.item.impl.PDCLevelToolsItem;
import me.byteful.plugin.leveltools.api.scheduler.Scheduler;
import me.byteful.plugin.leveltools.api.scheduler.impl.bukkit.BukkitScheduler;
import me.byteful.plugin.leveltools.api.scheduler.impl.folia.FoliaScheduler;
import me.byteful.plugin.leveltools.api.trigger.TriggerSlot;
import me.byteful.plugin.leveltools.profile.ProfileManager;
import me.byteful.plugin.leveltools.profile.display.DisplayProfile;
import me.byteful.plugin.leveltools.profile.display.ProgressBarConfig;
import me.byteful.plugin.leveltools.profile.item.ItemProfile;
import me.byteful.plugin.leveltools.profile.reward.RewardEntry;
import me.byteful.plugin.leveltools.profile.reward.RewardProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.byteful.plugin.leveltools.util.Text.*;

public final class LevelToolsUtil {
    public static final int MID_VERSION;
    private static final Pattern MINECRAFT_VERSION_PATTERN =
            Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+].*|\\.(?!\\d).*)?$");
    private static final Pattern SERVER_MINECRAFT_VERSION_PATTERN =
            Pattern.compile("\\(MC:\\s*([^\\s)]+)\\)");
    private static final String LORE_PREFIX = "§§";
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();
    private static final String LEGACY_LEVEL_KEY = "levelToolsLevel";
    private static final String LEGACY_XP_KEY = "levelToolsXp";
    private static final String LEGACY_REWARD_KEY = "levelToolsReward";
    public static final boolean IS_PAPER = hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");
    private static final MinecraftVersion MINECRAFT_VERSION;
    private static final boolean REQUIRES_LEGACY_ANVIL_LISTENER;
    private static final boolean SUPPORTS_DUAL_WIELDING;
    private static final boolean SUPPORTS_TRANSLATABLE_ITEM_DISPLAY_NAMES;
    private static final boolean SUPPORTS_BLOCK_DATA;
    private static final boolean SUPPORTS_PERSISTENT_DATA_CONTAINER;
    private static final boolean SUPPORTS_SPIGOT_ACTION_BAR;

    static {
        MINECRAFT_VERSION = resolveMinecraftVersion();
        MID_VERSION = MINECRAFT_VERSION.getCompatibilityMajor();
        REQUIRES_LEGACY_ANVIL_LISTENER = isMinecraftVersionBefore(1, 9);
        SUPPORTS_DUAL_WIELDING = isMinecraftVersionAtLeast(1, 9);
        SUPPORTS_TRANSLATABLE_ITEM_DISPLAY_NAMES = isMinecraftVersionAtLeast(1, 13);
        SUPPORTS_BLOCK_DATA = isMinecraftVersionAtLeast(1, 13);
        SUPPORTS_PERSISTENT_DATA_CONTAINER = isMinecraftVersionAtLeast(1, 14);
        SUPPORTS_SPIGOT_ACTION_BAR = isMinecraftVersionAtLeast(1, 13);
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isSupportedTool(Material material) {
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return false;
        }
        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return false;
        }
        return profileManager.hasMaterialProfile(material);
    }

    @Nullable
    public static ItemProfile getItemProfile(Material material) {
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return null;
        }
        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return null;
        }
        return profileManager.getProfileForMaterial(material);
    }

    public static ItemStack getHand(Player player) {
        return player.getInventory().getItemInMainHand().clone();
    }

    public static void setHand(Player player, ItemStack stack) {
        player.getInventory().setItemInMainHand(stack);
    }

    public static void setItemInSlot(@NotNull Player player, @Nullable TriggerSlot slot, @NotNull ItemStack stack) {
        if (slot == null || slot == TriggerSlot.HAND) {
            setHand(player, stack);
            return;
        }

        if (slot == TriggerSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(stack);
            return;
        }

        switch (slot) {
            case HELMET:
                player.getInventory().setHelmet(stack);
                return;
            case CHESTPLATE:
                player.getInventory().setChestplate(stack);
                return;
            case LEGGINGS:
                player.getInventory().setLeggings(stack);
                return;
            case BOOTS:
                player.getInventory().setBoots(stack);
                return;
            default:
                setHand(player, stack);
        }
    }

    @Nullable
    public static ItemStack getItemInSlot(@NotNull Player player, @Nullable TriggerSlot slot) {
        if (slot == null || slot == TriggerSlot.HAND) {
            return getHand(player);
        }

        if (slot == TriggerSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }

        switch (slot) {
            case HELMET:
                return player.getInventory().getHelmet();
            case CHESTPLATE:
                return player.getInventory().getChestplate();
            case LEGGINGS:
                return player.getInventory().getLeggings();
            case BOOTS:
                return player.getInventory().getBoots();
            default:
                return getHand(player);
        }
    }

    public static String createProgressBar(double xp, double maxXp, @Nullable DisplayProfile displayProfile) {
        if (displayProfile != null) {
            return displayProfile.getProgressBar().buildProgressBar(xp, maxXp);
        }
        return ProgressBarConfig.defaultConfig().buildProgressBar(xp, maxXp);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);

        return bd.doubleValue();
    }

    public static int roundDown(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(1, RoundingMode.DOWN);

        return bd.intValue();
    }

    public static double getMaxXp(@Nullable Player player, @Nullable ItemProfile itemProfile, @NotNull LevelToolsItem tool) {
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null || instance.getXpFormulaRegistry() == null) {
            return tool.getMaxXp();
        }

        return instance.getXpFormulaRegistry().evaluateMaxXp(player, itemProfile, tool.getLevel());
    }

    @NotNull
    public static ItemStack getItemStack(
            @NotNull LevelToolsItem tool,
            @Nullable Player player,
            @Nullable ItemProfile itemProfile
    ) {
        return tool.getItemStack(getMaxXp(player, itemProfile, tool));
    }

    @Nullable
    public static ItemStack getBackingItemStack(@NotNull LevelToolsItem tool) {
        return tool instanceof PDCLevelToolsItem
                ? ((PDCLevelToolsItem) tool).getStack()
                : null;
    }

    public static boolean rebindLevelToolsItem(@NotNull LevelToolsItem tool, @NotNull ItemStack stack) {
        if (!(tool instanceof PDCLevelToolsItem pdcItem)) {
            return false;
        }

        final int level = tool.getLevel();
        final double xp = tool.getXp();
        final int lastHandledReward = tool.getLastHandledReward();

        pdcItem.setStack(stack);
        tool.setLevel(level);
        tool.setXp(xp);
        tool.setLastHandledReward(lastHandledReward);
        return true;
    }

    public static LevelToolsItem createLevelToolsItem(@NotNull ItemStack stack) {
        return createPdcItem(stack);
    }

    private static LevelToolsItem createPdcItem(@NotNull ItemStack stack) {
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return new PDCLevelToolsItem(stack, null);
        }

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(PDCLevelToolsItem.LEVEL_KEY, PersistentDataType.INTEGER)
                || pdc.has(PDCLevelToolsItem.XP_KEY, PersistentDataType.DOUBLE)
                || pdc.has(PDCLevelToolsItem.LAST_REWARD_KEY, PersistentDataType.INTEGER)) {
            return new PDCLevelToolsItem(stack, meta);
        }

        final LegacyLevelData legacy = NBT.get(stack, (ReadableItemNBT nbt) -> new LegacyLevelData(
                nbt.hasTag(LEGACY_LEVEL_KEY) ? nbt.getInteger(LEGACY_LEVEL_KEY) : null,
                nbt.hasTag(LEGACY_XP_KEY) ? nbt.getDouble(LEGACY_XP_KEY) : null,
                nbt.hasTag(LEGACY_REWARD_KEY) ? nbt.getInteger(LEGACY_REWARD_KEY) : null));

        if (!legacy.hasAnyValue()) {
            return new PDCLevelToolsItem(stack, meta);
        }

        final ItemStack migrated = stack.clone();
        NBT.modify(migrated, nbt -> {
            nbt.removeKey(LEGACY_LEVEL_KEY);
            nbt.removeKey(LEGACY_XP_KEY);
            nbt.removeKey(LEGACY_REWARD_KEY);
        });

        final PDCLevelToolsItem item = new PDCLevelToolsItem(migrated);
        if (legacy.level() != null) {
            item.setLevel(legacy.level());
        }
        if (legacy.xp() != null) {
            item.setXp(legacy.xp());
        }
        if (legacy.lastReward() != null) {
            item.setLastHandledReward(legacy.lastReward());
        }
        return item;
    }

    public static String getServerVersion() {
        String version = Bukkit.getVersion();
        String[] split = version.split(" ");
        return split[split.length - 1].trim().replace(")", "");
    }

    public static boolean requiresLegacyAnvilListener() {
        return REQUIRES_LEGACY_ANVIL_LISTENER;
    }

    public static boolean supportsDualWielding() {
        return SUPPORTS_DUAL_WIELDING;
    }

    public static boolean supportsTranslatableItemDisplayNames() {
        return SUPPORTS_TRANSLATABLE_ITEM_DISPLAY_NAMES;
    }

    public static boolean supportsBlockData() {
        return SUPPORTS_BLOCK_DATA;
    }

    public static boolean supportsPersistentDataContainer() {
        return SUPPORTS_PERSISTENT_DATA_CONTAINER;
    }

    public static boolean supportsSpigotActionBar() {
        return SUPPORTS_SPIGOT_ACTION_BAR;
    }

    private static boolean isMinecraftVersionAtLeast(int major, int minor) {
        return MINECRAFT_VERSION.compareTo(new MinecraftVersion(major, minor, 0)) >= 0;
    }

    private static boolean isMinecraftVersionBefore(int major, int minor) {
        return MINECRAFT_VERSION.compareTo(new MinecraftVersion(major, minor, 0)) < 0;
    }

    private static MinecraftVersion resolveMinecraftVersion() {
        final String minecraftVersion = getMinecraftVersion();
        if (minecraftVersion != null) {
            return parseMinecraftVersion(minecraftVersion);
        }

        final String serverVersion = getMinecraftVersionFromServerVersion(Bukkit.getVersion());
        if (serverVersion != null) {
            return parseMinecraftVersion(serverVersion);
        }

        return parseMinecraftVersion(Bukkit.getBukkitVersion());
    }

    @Nullable
    private static String getMinecraftVersion() {
        try {
            final Method method = Bukkit.class.getMethod("getMinecraftVersion");
            final Object version = method.invoke(null);
            if (version instanceof String) {
                final String value = ((String) version).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (ReflectiveOperationException | SecurityException ignored) {
        }

        return null;
    }

    @Nullable
    private static String getMinecraftVersionFromServerVersion(@NotNull String version) {
        final Matcher matcher = SERVER_MINECRAFT_VERSION_PATTERN.matcher(version);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    static MinecraftVersion parseMinecraftVersion(@NotNull String version) {
        final Matcher matcher = MINECRAFT_VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalStateException("Unable to parse Bukkit Minecraft version: " + version);
        }

        return new MinecraftVersion(
                Integer.parseInt(matcher.group(1)),
                parseVersionPart(matcher.group(2)),
                parseVersionPart(matcher.group(3))
        );
    }

    private static int parseVersionPart(@Nullable String value) {
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public static ItemStack buildItemStack(
            ItemStack stack, Map<Enchantment, Integer> enchantments, int level, double xp, double maxXp) {
        final ItemMeta meta = stack.getItemMeta();
        assert meta != null : "ItemMeta is null! Should not happen.";
        applyDisplay(stack, meta, enchantments, level, xp, maxXp);
        stack.setItemMeta(meta);

        return stack;
    }

    public static void applyDisplay(
            ItemStack stack, ItemMeta meta, Map<Enchantment, Integer> enchantments, int level, double xp,
            double maxXp) {
        final DisplayProfile displayProfile = getDisplayProfileForMaterial(stack.getType());
        final String progressBar = createProgressBar(xp, maxXp, displayProfile);

        if (displayProfile != null) {
            DisplayProfile.NameDisplay nameDisplay = displayProfile.getNameDisplay();
            if (nameDisplay.isEnabled()) {
                final String text = colorize(nameDisplay.getText()
                        .replace("{level}", String.valueOf(level))
                        .replace("{xp}", String.valueOf(xp))
                        .replace("{max_xp}", String.valueOf(maxXp))
                        .replace("{max_xp_formatted}", formatMoney(maxXp))
                        .replace("{xp_formatted}", formatMoney(xp))
                        .replace("{progress_bar}", progressBar));

                if (nameDisplay.getText().contains("{item}")
                        && supportsTranslatableItemDisplayNames()
                        && IS_PAPER) {
                    AdventureHelper.setDisplayNameWithTranslatable(meta, text, stack);
                } else {
                    meta.displayName(LEGACY_SERIALIZER.deserialize(text));
                }
            }

            DisplayProfile.LoreDisplay loreDisplay = displayProfile.getLoreDisplay();
            if (loreDisplay.isEnabled()) {
                List<String> lines = loreDisplay.getLines().stream()
                        .map(str -> LORE_PREFIX + str)
                        .map(str -> colorize(
                                str.replace("{level}", String.valueOf(level))
                                        .replace("{xp}", String.valueOf(xp))
                                        .replace("{max_xp}", String.valueOf(maxXp))
                                        .replace("{progress_bar}", progressBar))
                                .replace("{max_xp_formatted}", formatMoney(maxXp))
                                .replace("{xp_formatted}", formatMoney(xp)))
                        .collect(Collectors.toList());
                smartSetLore(meta, lines);
            }
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        if (LevelToolsPlugin.getInstance().getConfigManager().getSettings().isHideAttributes()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
    }

    @Nullable
    private static DisplayProfile getDisplayProfileForMaterial(Material material) {
        ItemProfile itemProfile = getItemProfile(material);
        if (itemProfile == null) {
            return null;
        }
        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return null;
        }
        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return null;
        }
        return profileManager.getDisplayProfileFor(itemProfile);
    }

    private static void smartSetLore(@NotNull ItemMeta meta, @NotNull List<String> toAdd) {
        final List<Component> additions = toAdd.stream()
                .map(LEGACY_SERIALIZER::deserialize)
                .collect(Collectors.toCollection(ArrayList::new));
        final List<Component> existing = meta.lore();

        if (existing == null || existing.isEmpty()) {
            meta.lore(additions);
            return;
        }

        final List<Component> lore = new ArrayList<>(existing);
        final int[] bounds = findPrefixBounds(lore);
        final int start = bounds[0];
        final int end = bounds[1];

        if (start == -1) {
            lore.addAll(additions);
            meta.lore(lore);
            return;
        }

        lore.subList(start, end + 1).clear();
        lore.addAll(start, additions);
        meta.lore(lore);
    }

    private static int[] findPrefixBounds(@NotNull List<Component> lore) {
        final int[] bounds = new int[]{-1, -1};
        for (int i = 0; i < lore.size(); i++) {
            if (LEGACY_SERIALIZER.serialize(lore.get(i)).startsWith(LORE_PREFIX)) {
                if (bounds[0] == -1) {
                    bounds[0] = i;
                }
                bounds[1] = i;
            }
        }
        return bounds;
    }

    public static void handleReward(LevelToolsItem tool, Player player) {
        Material material = tool.getItemStack().getType();
        ItemProfile itemProfile = getItemProfile(material);
        if (itemProfile == null) {
            return;
        }

        LevelToolsPlugin instance = LevelToolsPlugin.getInstance();
        if (instance == null) {
            return;
        }

        ProfileManager profileManager = instance.getProfileManager();
        if (profileManager == null) {
            return;
        }

        RewardProfile rewardProfile = profileManager.getRewardProfileFor(itemProfile);
        if (rewardProfile == null) {
            return;
        }

        if (applyRewards(tool, player, rewardProfile)) {
            setHand(player, getItemStack(tool, player, itemProfile));
        }
    }

    public static boolean applyRewards(
            @NotNull LevelToolsItem tool, @NotNull Player player, @NotNull RewardProfile rewardProfile) {
        int level = tool.getLevel();
        if (!rewardProfile.hasRewardsForLevel(level) || tool.getLastHandledReward() == level) {
            return false;
        }

        tool.setLastHandledReward(level);
        for (RewardEntry entry : rewardProfile.getRewardsForLevel(level)) {
            entry.apply(tool, player);
        }

        return true;
    }

    public static void sendActionBar(Player player, String msg) {
        player.sendActionBar(LEGACY_SERIALIZER.deserialize(msg));
    }

    public static Scheduler createScheduler(LevelToolsPlugin plugin) {
        if (isFolia()) {
            return new FoliaScheduler(plugin);
        }

        return new BukkitScheduler(plugin);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private record LegacyLevelData(Integer level, Double xp, Integer lastReward) {
        private boolean hasAnyValue() {
            return level != null || xp != null || lastReward != null;
        }
    }

    static final class MinecraftVersion implements Comparable<MinecraftVersion> {
        private final int major;
        private final int minor;
        private final int patch;

        private MinecraftVersion(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        private int getCompatibilityMajor() {
            if (major == 1) {
                return minor;
            }
            return major;
        }

        @Override
        public int compareTo(@NotNull MinecraftVersion other) {
            if (major != other.major) {
                return Integer.compare(major, other.major);
            }
            if (minor != other.minor) {
                return Integer.compare(minor, other.minor);
            }
            return Integer.compare(patch, other.patch);
        }
    }
}
