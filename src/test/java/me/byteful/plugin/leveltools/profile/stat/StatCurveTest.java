package me.byteful.plugin.leveltools.profile.stat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatCurveTest {
    @Test
    void curveUsesExactConfiguredBounds() {
        final StatCurve curve = new StatCurve(0.005, 0.15, 1.30);

        assertEquals(0.005, curve.evaluate(0.0), 1.0E-9);
        assertEquals(0.15, curve.evaluate(1.0), 1.0E-9);
    }

    @Test
    void curveClampsProgressOutsideLevelRange() {
        final StatCurve curve = new StatCurve(0.0, 3.0, 1.15);

        assertEquals(0.0, curve.evaluate(-1.0), 1.0E-9);
        assertEquals(3.0, curve.evaluate(2.0), 1.0E-9);
    }

    @Test
    void positiveProgressionCurveImprovesEveryLevelStep() {
        final StatCurve curve = new StatCurve(0.0, 3.0, 1.15);
        double previous = curve.evaluate(0.0);

        for (int level = 2; level <= 100; level++) {
            final double progress = (level - 1.0) / 99.0;
            final double current = curve.evaluate(progress);
            assertTrue(current > previous);
            previous = current;
        }
    }
}
