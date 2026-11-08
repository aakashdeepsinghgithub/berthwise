package com.berthwise;

import com.berthwise.tide.TideModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.berthwise.TestData.T0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TideModelTest {

    private final TideModel tide = new TideModel(1.6, 1.2, T0);

    @Test
    void highWaterAtReference() {
        assertEquals(2.8, tide.heightAt(T0), 1e-9);
    }

    @Test
    void lowWaterHalfAPeriodLater() {
        long halfPeriodSeconds = Math.round(TideModel.PERIOD_HOURS / 2 * 3600);
        assertEquals(0.4, tide.heightAt(T0.plusSeconds(halfPeriodSeconds)), 1e-3);
    }

    @Test
    void nextWindowFindsRisingTide() {
        long halfPeriodSeconds = Math.round(TideModel.PERIOD_HOURS / 2 * 3600);
        var window = tide.nextWindow(T0.plusSeconds(halfPeriodSeconds), 2.0, Duration.ofHours(12));
        assertTrue(window.isPresent());
    }
}
