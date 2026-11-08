package com.berthwise.tide;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Harmonic approximation of a semidiurnal tide (single M2 constituent).
 */
public class TideModel {

    public static final double PERIOD_HOURS = 12.42;
    private static final Duration SEARCH_STEP = Duration.ofMinutes(10);

    private final double meanLevelMeters;
    private final double amplitudeMeters;
    private final Instant highWaterReference;

    public TideModel(double meanLevelMeters, double amplitudeMeters, Instant highWaterReference) {
        if (amplitudeMeters < 0) {
            throw new IllegalArgumentException("amplitude must be non-negative");
        }
        this.meanLevelMeters = meanLevelMeters;
        this.amplitudeMeters = amplitudeMeters;
        this.highWaterReference = highWaterReference;
    }

    /** Tide height above chart datum at the given instant, in metres. */
    public double heightAt(Instant t) {
        long minutes = Duration.between(highWaterReference, t).toMinutes();
        double hours = minutes / 60;
        return meanLevelMeters + amplitudeMeters * Math.cos(2 * Math.PI * hours / PERIOD_HOURS);
    }

    /**
     * First instant at or after {@code from} (searched in 10-minute steps) when the tide is at
     * least {@code requiredHeight}, looking no further than {@code horizon}.
     */
    public Optional<Instant> nextWindow(Instant from, double requiredHeight, Duration horizon) {
        Instant limit = from.plus(horizon);
        for (Instant t = from; !t.isAfter(limit); t = t.plus(SEARCH_STEP)) {
            if (heightAt(t) >= requiredHeight) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public double getMeanLevelMeters() { return meanLevelMeters; }
    public double getAmplitudeMeters() { return amplitudeMeters; }
}
