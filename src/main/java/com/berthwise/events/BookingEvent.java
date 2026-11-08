package com.berthwise.events;

import java.time.Instant;

/** Something that happened to a booking or request. {@code bookingId} is null for WAITLISTED. */
public record BookingEvent(Type type, String bookingId, String vesselImo, Instant at) {

    public enum Type {
        CREATED,
        CONFIRMED,
        CANCELLED,
        WAITLISTED,
        COMPLETED
    }
}
