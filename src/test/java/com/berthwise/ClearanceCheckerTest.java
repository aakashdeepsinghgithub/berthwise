package com.berthwise;

import com.berthwise.model.Berth;
import com.berthwise.model.Vessel;
import com.berthwise.model.VesselType;
import com.berthwise.service.ClearanceChecker;
import com.berthwise.tide.TideModel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.berthwise.TestData.T0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearanceCheckerTest {

    private final ClearanceChecker checker = new ClearanceChecker(new TideModel(1.6, 1.2, T0));

    @Test
    void requiredDepthIncludesTenPercentUnderKeelClearance() {
        Vessel v = new Vessel("IMO1234567", "Test", VesselType.BULK, 150, 25, 10.0, 20_000);
        assertEquals(11.0, checker.requiredDepth(v), 1e-9);
    }

    @Test
    void berthMustAcceptVesselType() {
        Berth tankerJetty = new Berth("B2", "Jetty", 300, 14, Set.of(VesselType.TANKER));
        assertFalse(checker.fits(TestData.container(), tankerJetty, T0));
    }

    @Test
    void containerFitsContainerQuayAtHighWater() {
        assertTrue(checker.fits(TestData.container(), TestData.containerQuay(), T0));
    }
}
