package me.byteful.plugin.leveltools.profile.item;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public final class ItemProfileLoader {
    private final Logger logger;

    public ItemProfileLoader(@NotNull Logger logger) {
        this.logger = logger;
    }

    @NotNull
    public Map<String, ItemProfile> load(@NotNull FileConfiguration config) {
        Map<String, ItemProfile> profiles = new HashMap<>();
        Map<String, ConfigurationSection> rawProfiles = new HashMap<>();
        ConfigurationSection profilesSection = config.getConfigurationSection("profiles");

        if (profilesSection == null) {
            logger.warning("No item profiles found in item_profiles.yml");
            return profiles;
        }

        for (String profileId : profilesSection.getKeys(false)) {
            ConfigurationSection profileSection = profilesSection.getConfigurationSection(profileId);
            if (profileSection != null) {
                rawProfiles.put(profileId, profileSection);
            }
        }

        for (String profileId : rawProfiles.keySet()) {
            if (!profiles.containsKey(profileId)) {
                try {
                    ItemProfile profile = resolveProfile(profileId, rawProfiles, profiles, new HashSet<>());
                    if (profile != null) {
                        profiles.put(profileId, profile);
                    }
                } catch (Exception e) {
                    logger.severe("Failed to load item profile '" + profileId + "': " + e.getMessage());
                }
            }
        }

        logger.info("Loaded " + profiles.size() + " item profile(s).");
        return profiles;
    }

    @Nullable
    private ItemProfile resolveProfile(
            @NotNull String profileId,
            @NotNull Map<String, ConfigurationSection> rawProfiles,
            @NotNull Map<String, ItemProfile> resolvedProfiles,
            @NotNull Set<String> resolvingChain
    ) {
        if (resolvedProfiles.containsKey(profileId)) {
            return resolvedProfiles.get(profileId);
        }

        if (resolvingChain.contains(profileId)) {
            throw new IllegalStateException("Circular inheritance detected: " + String.join(" -> ", resolvingChain) + " -> " + profileId);
        }

        ConfigurationSection section = rawProfiles.get(profileId);
        if (section == null) {
            logger.warning("Item profile '" + profileId + "' not found");
            return null;
        }

        resolvingChain.add(profileId);

        String extendsId = section.getString("extends");
        ItemProfile parentProfile = null;

        if (extendsId != null && !extendsId.isEmpty()) {
            parentProfile = resolveProfile(extendsId, rawProfiles, resolvedProfiles, resolvingChain);
            if (parentProfile == null) {
                throw new IllegalStateException("Parent profile '" + extendsId + "' not found for profile '" + profileId + "'");
            }
        }

        ItemProfile profile = parseProfile(profileId, section, parentProfile);
        resolvedProfiles.put(profileId, profile);
        resolvingChain.remove(profileId);

        return profile;
    }

    @NotNull
    private ItemProfile parseProfile(
            @NotNull String id,
            @NotNull ConfigurationSection section,
            @Nullable ItemProfile parent
    ) {
        Set<Material> materials = parseMaterials(section.getStringList("materials"));
        if (materials.isEmpty() && parent != null) {
            materials = parent.getMaterials();
        }

        List<String> triggerProfileIds = parseTriggerProfileIds(section, parent);
        if (triggerProfileIds.isEmpty()) {
            throw new IllegalArgumentException("Missing 'trigger_profiles' field");
        }

        String rewardProfileId = section.getString("reward_profile");
        if (rewardProfileId == null && parent != null) {
            rewardProfileId = parent.getRewardProfileId();
        }
        if (rewardProfileId == null) {
            throw new IllegalArgumentException("Missing 'reward_profile' field");
        }

        String displayProfileId = section.getString("display_profile");
        if (displayProfileId == null && parent != null) {
            displayProfileId = parent.getDisplayProfileId();
        }
        if (displayProfileId == null) {
            displayProfileId = "default";
        }

        final String progressionProfileId = section.getString(
                "progression_profile",
                parent != null ? parent.getProgressionProfileId() : "default"
        );
        final String statProfileId = section.getString(
                "stat_profile",
                parent != null ? parent.getStatProfileId() : "none"
        );

        int maxLevel = section.getInt("max_level", parent != null ? parent.getMaxLevel() : 100);

        String levelXpFormula = section.getString("level_xp_formula");
        if (levelXpFormula == null && parent != null) {
            levelXpFormula = parent.getLevelXpFormula();
        }

        String extendsProfileId = section.getString("extends");

        return ItemProfile.builder(id)
                .materials(materials)
                .triggerProfiles(triggerProfileIds)
                .rewardProfile(rewardProfileId)
                .displayProfile(displayProfileId)
                .progressionProfile(progressionProfileId)
                .statProfile(statProfileId)
                .maxLevel(maxLevel)
                .levelXpFormula(levelXpFormula)
                .extendsProfile(extendsProfileId)
                .build();
    }

    @NotNull
    private List<String> parseTriggerProfileIds(
            @NotNull ConfigurationSection section,
            @Nullable ItemProfile parent
    ) {
        if (section.contains("trigger_profiles")) {
            return section.getStringList("trigger_profiles");
        }

        String singleTriggerProfile = section.getString("trigger_profile");
        if (singleTriggerProfile != null && !singleTriggerProfile.trim().isEmpty()) {
            return Collections.singletonList(singleTriggerProfile);
        }

        if (parent != null) {
            return parent.getTriggerProfileIds();
        }

        return Collections.emptyList();
    }

    @NotNull
    private Set<Material> parseMaterials(@NotNull List<String> materialNames) {
        Set<Material> materials = new HashSet<>();

        for (String name : materialNames) {
            final String normalized = name.trim().toUpperCase(Locale.ROOT);
            Material material = Material.matchMaterial(normalized);

            if (material == null) {
                try {
                    material = XMaterial.matchXMaterial(normalized)
                            .map(XMaterial::get)
                            .orElse(null);
                } catch (IllegalArgumentException ignored) {
                    material = null;
                }
            }

            if (material == null) {
                logger.warning("Unknown material: " + name);
                continue;
            }

            materials.add(material);
        }

        return materials;
    }
}
