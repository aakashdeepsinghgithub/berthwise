package com.berthwise;

import com.berthwise.model.Berth;
import com.berthwise.model.Booking;
import com.berthwise.model.Vessel;
import com.berthwise.model.VesselType;

import java.time.Instant;
import java.util.Set;

/** Shared fixtures. */
public final class TestData {

    public static final Instant T0 = Instant.parse("2026-12-01T00:00:00Z");

    private TestData() {
    }

    public static Vessel container() {
        return new Vessel("IMO9619907", "Sundarban Bay", VesselType.CONTAINER, 260.0, 32.3, 12.5, 40_000);
    }

    public static Vessel smallBulk() {
        return new Vessel("IMO9300776", "Malabar Trader", VesselType.BULK, 189.9, 32.3, 10.0, 1_000);
    }

    public static Berth containerQuay() {
        return new Berth("B1", "North Container Quay", 400, 15.0, Set.of(VesselType.CONTAINER));
    }

    public static Booking booking(Vessel v, String berth, Instant start, Instant end) {
        return new Booking(v, berth, start, end, 3, T0);
    }
}
