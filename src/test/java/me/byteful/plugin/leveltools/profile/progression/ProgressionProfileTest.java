package me.byteful.plugin.leveltools.profile.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionProfileTest {
    @Test
    void levelOneAndMaxLevelNormalizeToCurveBounds() {
        final ProgressionProfile profile = profile(100, 0.5);

        assertEquals(0.0, profile.normalizedProgress(1), 1.0E-9);
        assertEquals(1.0, profile.normalizedProgress(100), 1.0E-9);
        assertEquals(0.0, profile.normalizedProgress(-10), 1.0E-9);
        assertEquals(1.0, profile.normalizedProgress(500), 1.0E-9);
    }

    @Test
    void firstAndLastTransitionsUseConfiguredXpBounds() {
        final ProgressionProfile profile = profile(100, 0.5);

        assertEquals(100.0, profile.xpRequiredForLevel(1), 0.1);
        assertEquals(10000.0, profile.xpRequiredForLevel(99), 0.1);
        assertEquals(10000.0, profile.xpRequiredForLevel(100), 0.1);
    }

    @Test
    void levelCountInfluenceControlsTotalXpScaling() {
        final double total100AtZero = totalXp(profile(100, 0.0));
        final double total1000AtZero = totalXp(profile(1000, 0.0));
        final double ratioZero = total1000AtZero / total100AtZero;

        final double total100AtHalf = totalXp(profile(100, 0.5));
        final double total1000AtHalf = totalXp(profile(1000, 0.5));
        final double ratioHalf = total1000AtHalf / total100AtHalf;

        final double total100AtOne = totalXp(profile(100, 1.0));
        final double total1000AtOne = totalXp(profile(1000, 1.0));
        final double ratioOne = total1000AtOne / total100AtOne;

        assertTrue(ratioZero > 0.95 && ratioZero < 1.10, "0.0 influence should keep total XP similar");
        assertTrue(ratioHalf > 2.9 && ratioHalf < 3.5, "0.5 influence should scale sublinearly");
        assertTrue(ratioOne > 9.5 && ratioOne < 10.6, "1.0 influence should scale near-linearly");
    }

    @Test
    void laterLevelsAlwaysRequireMoreXpWithDefaultCurve() {
        final ProgressionProfile profile = profile(100, 0.5);
        double previous = profile.xpRequiredForLevel(1);

        for (int level = 2; level < profile.getMaxLevel(); level++) {
            final double current = profile.xpRequiredForLevel(level);
            assertTrue(current >= previous);
            previous = current;
        }
    }

    private static ProgressionProfile profile(int maxLevel, double influence) {
        return new ProgressionProfile(
                "test",
                maxLevel,
                100,
                100.0,
                10000.0,
                1.85,
                influence
        );
    }

    private static double totalXp(ProgressionProfile profile) {
        double total = 0.0;
        for (int level = 1; level < profile.getMaxLevel(); level++) {
            total += profile.xpRequiredForLevel(level);
        }
        return total;
    }
}
