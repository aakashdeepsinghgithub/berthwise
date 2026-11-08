package com.berthwise.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A request for berth time. {@code berthId} may be null, meaning "any suitable berth".
 * Priority 1..5 where 5 is the most urgent.
 */
public record BookingRequest(String vesselImo,
                             String berthId,
                             Instant start,
                             Instant end,
                             int priority,
                             Instant requestedAt) {

    public BookingRequest {
        Objects.requireNonNull(vesselImo, "vesselImo");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(requestedAt, "requestedAt");
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("priority must be between 1 and 5");
        }
    }
}
