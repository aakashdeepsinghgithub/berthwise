package com.berthwise.service;

import com.berthwise.model.Booking;

/** Outcome of an allocation attempt. {@code booking} is set only when ACCEPTED. */
public record AllocationResult(Outcome outcome, Booking booking, String reason) {

    public enum Outcome { ACCEPTED, WAITLISTED, REJECTED }

    public static AllocationResult accepted(Booking booking) {
        return new AllocationResult(Outcome.ACCEPTED, booking, null);
    }

    public static AllocationResult waitlisted(String reason) {
        return new AllocationResult(Outcome.WAITLISTED, null, reason);
    }

    public static AllocationResult rejected(String reason) {
        return new AllocationResult(Outcome.REJECTED, null, reason);
    }
}
