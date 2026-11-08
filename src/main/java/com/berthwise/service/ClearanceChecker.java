package com.berthwise.service;

import com.berthwise.model.Berth;
import com.berthwise.model.Vessel;
import com.berthwise.tide.TideModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Physical-fit rules: type, length with fender margin, depth with under-keel clearance. */
public class ClearanceChecker {

    static final int UKC_PERCENT = 10;
    static final double FENDER_MARGIN = 1.10;

    private final TideModel tide;

    public ClearanceChecker(TideModel tide) {
        this.tide = tide;
    }

    public boolean fitsLength(Vessel vessel, Berth berth) {
        return berth.getLengthMeters() * FENDER_MARGIN >= vessel.getLoaMeters();
    }

    /** Draft plus the under-keel clearance the harbour master requires. */
    public double requiredDepth(Vessel vessel) {
        return vessel.getDraftMeters() + vessel.getDraftMeters() * (UKC_PERCENT / 100);
    }

    public boolean hasDepth(Vessel vessel, Berth berth, Instant arrival) {
        double available = berth.getDepthAtDatumMeters() + tide.heightAt(arrival);
        return available >= requiredDepth(vessel);
    }

    /** Human-readable reasons the vessel cannot use the berth; empty if it fits. */
    public List<String> violations(Vessel vessel, Berth berth, Instant arrival) {
        List<String> reasons = new ArrayList<>();
        if (!berth.accepts(vessel.getType())) {
            reasons.add(berth.getId() + " does not accept " + vessel.getType());
        }
        if (!fitsLength(vessel, berth)) {
            reasons.add(berth.getId() + " too short for LOA " + vessel.getLoaMeters() + " m");
        }
        if (!hasDepth(vessel, berth, arrival)) {
            reasons.add(String.format("%s too shallow at arrival (needs %.2f m)",
                    berth.getId(), requiredDepth(vessel)));
        }
        return reasons;
    }

    public boolean fits(Vessel vessel, Berth berth, Instant arrival) {
        return violations(vessel, berth, arrival).isEmpty();
    }
}
