package me.byteful.plugin.leveltools.profile.stat;

public final class StatCurve {
    private final double start;
    private final double max;
    private final double exponent;

    public StatCurve(double start, double max, double exponent) {
        if (exponent <= 0.0) {
            throw new IllegalArgumentException("Stat curve exponent must be positive");
        }
        this.start = start;
        this.max = max;
        this.exponent = exponent;
    }

    public double evaluate(double normalizedProgress) {
        final double progress = Math.max(0.0, Math.min(1.0, normalizedProgress));
        return start + (max - start) * Math.pow(progress, exponent);
    }
}
