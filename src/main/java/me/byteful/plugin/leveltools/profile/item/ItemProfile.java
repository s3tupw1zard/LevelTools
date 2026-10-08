package me.byteful.plugin.leveltools.profile.item;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ItemProfile {
    private final String id;
    private final Set<Material> materials;
    private final List<String> triggerProfileIds;
    private final String rewardProfileId;
    private final String displayProfileId;
    private final String progressionProfileId;
    private final String statProfileId;
    private final int legacyMaxLevel;
    private final String legacyLevelXpFormula;
    private final String extendsProfileId;

    public ItemProfile(
            @NotNull String id,
            @NotNull Set<Material> materials,
            @NotNull List<String> triggerProfileIds,
            @NotNull String rewardProfileId,
            @NotNull String displayProfileId,
            @NotNull String progressionProfileId,
            @NotNull String statProfileId,
            int legacyMaxLevel,
            @Nullable String legacyLevelXpFormula,
            @Nullable String extendsProfileId
    ) {
        this.id = id;
        this.materials = Collections.unmodifiableSet(materials);
        this.triggerProfileIds =
                Collections.unmodifiableList(sanitizeTriggerProfileIds(triggerProfileIds));
        this.rewardProfileId = rewardProfileId;
        this.displayProfileId = displayProfileId;
        this.progressionProfileId = progressionProfileId;
        this.statProfileId = statProfileId;
        this.legacyMaxLevel = legacyMaxLevel;
        this.legacyLevelXpFormula = legacyLevelXpFormula;
        this.extendsProfileId = extendsProfileId;
    }

    public static Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public Set<Material> getMaterials() {
        return materials;
    }

    @NotNull
    public List<String> getTriggerProfileIds() {
        return triggerProfileIds;
    }

    @Deprecated
    @NotNull
    public String getTriggerProfileId() {
        return triggerProfileIds.getFirst();
    }

    @NotNull
    public String getRewardProfileId() {
        return rewardProfileId;
    }

    @NotNull
    public String getDisplayProfileId() {
        return displayProfileId;
    }

    @NotNull
    public String getProgressionProfileId() {
        return progressionProfileId;
    }

    @NotNull
    public String getStatProfileId() {
        return statProfileId;
    }

    /**
     * Legacy fallback retained for external integrations during the 2026.1 transition.
     * Core progression uses {@link #getProgressionProfileId()}.
     */
    public int getMaxLevel() {
        return legacyMaxLevel;
    }

    @Nullable
    public String getLevelXpFormula() {
        return legacyLevelXpFormula;
    }

    @Nullable
    public String getExtendsProfileId() {
        return extendsProfileId;
    }

    public boolean hasCustomXpFormula() {
        return legacyLevelXpFormula != null && !legacyLevelXpFormula.isEmpty();
    }

    public boolean extendsProfile() {
        return extendsProfileId != null && !extendsProfileId.isEmpty();
    }

    public boolean matchesMaterial(@NotNull Material material) {
        return materials.contains(material);
    }

    public static final class Builder {
        private final String id;
        private Set<Material> materials = Collections.emptySet();
        private List<String> triggerProfileIds = Collections.emptyList();
        private String rewardProfileId = "none";
        private String displayProfileId = "default";
        private String progressionProfileId = "default";
        private String statProfileId = "none";
        private int legacyMaxLevel = 100;
        private String legacyLevelXpFormula;
        private String extendsProfileId;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public Builder materials(@NotNull Set<Material> materials) {
            this.materials = materials;
            return this;
        }

        public Builder triggerProfiles(@NotNull List<String> triggerProfileIds) {
            this.triggerProfileIds = triggerProfileIds;
            return this;
        }

        @Deprecated
        public Builder triggerProfile(@NotNull String triggerProfileId) {
            this.triggerProfileIds = Collections.singletonList(triggerProfileId);
            return this;
        }

        public Builder rewardProfile(@NotNull String rewardProfileId) {
            this.rewardProfileId = rewardProfileId;
            return this;
        }

        public Builder displayProfile(@NotNull String displayProfileId) {
            this.displayProfileId = displayProfileId;
            return this;
        }

        public Builder progressionProfile(@NotNull String progressionProfileId) {
            this.progressionProfileId = progressionProfileId;
            return this;
        }

        public Builder statProfile(@NotNull String statProfileId) {
            this.statProfileId = statProfileId;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.legacyMaxLevel = maxLevel;
            return this;
        }

        public Builder levelXpFormula(@Nullable String levelXpFormula) {
            this.legacyLevelXpFormula = levelXpFormula;
            return this;
        }

        public Builder extendsProfile(@Nullable String extendsProfileId) {
            this.extendsProfileId = extendsProfileId;
            return this;
        }

        public ItemProfile build() {
            return new ItemProfile(
                    id,
                    materials,
                    triggerProfileIds,
                    rewardProfileId,
                    displayProfileId,
                    progressionProfileId,
                    statProfileId,
                    legacyMaxLevel,
                    legacyLevelXpFormula,
                    extendsProfileId
            );
        }
    }

    @NotNull
    private static List<String> sanitizeTriggerProfileIds(@NotNull List<String> triggerProfileIds) {
        final LinkedHashSet<String> uniqueIds = new LinkedHashSet<>();
        for (String triggerProfileId : triggerProfileIds) {
            if (triggerProfileId == null) {
                continue;
            }

            final String trimmed = triggerProfileId.trim();
            if (!trimmed.isEmpty()) {
                uniqueIds.add(trimmed);
            }
        }

        return new ArrayList<>(uniqueIds);
    }
}
