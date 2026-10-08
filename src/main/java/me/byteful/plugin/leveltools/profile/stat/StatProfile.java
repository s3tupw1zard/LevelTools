package me.byteful.plugin.leveltools.profile.stat;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class StatProfile {
    private final String id;
    private final Map<StatType, StatCurve> curves;

    public StatProfile(@NotNull String id, @NotNull Map<StatType, StatCurve> curves) {
        this.id = id;
        this.curves = Collections.unmodifiableMap(new EnumMap<>(curves));
    }

    @NotNull
    public String getId() {
        return id;
    }

    public double evaluate(@NotNull StatType type, double progress) {
        final StatCurve curve = curves.get(type);
        return curve == null ? 0.0 : curve.evaluate(progress);
    }
}
