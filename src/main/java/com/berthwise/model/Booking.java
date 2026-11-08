package com.berthwise.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A vessel occupying a berth over the half-open interval [start, end). */
public class Booking {

    private final String id;
    private final Vessel vessel;
    private final String berthId;
    private final Instant start;
    private final Instant end;
    private final int priority;
    private final Instant createdAt;

    private volatile BookingStatus status;
    private volatile Instant actualDeparture;

    public Booking(Vessel vessel, String berthId, Instant start, Instant end,
                   int priority, Instant createdAt) {
        this(UUID.randomUUID().toString(), vessel, berthId, start, end, priority, createdAt,
                BookingStatus.TENTATIVE);
    }

    public Booking(String id, Vessel vessel, String berthId, Instant start, Instant end,
                   int priority, Instant createdAt, BookingStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.vessel = Objects.requireNonNull(vessel, "vessel");
        this.berthId = Objects.requireNonNull(berthId, "berthId");
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Booking end must be after start");
        }
        this.priority = priority;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * True if both bookings are on the same berth and their time ranges intersect.
     */
    public boolean overlaps(Booking other) {
        if (!berthId.equals(other.berthId)) {
            return false;
        }
        return !start.isAfter(other.end) && !other.start.isAfter(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public String getId() { return id; }
    public Vessel getVessel() { return vessel; }
    public String getBerthId() { return berthId; }
    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }
    public int getPriority() { return priority; }
    public Instant getCreatedAt() { return createdAt; }
    public BookingStatus getStatus() { return status; }
    public Instant getActualDeparture() { return actualDeparture; }

    public void setStatus(BookingStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public void setActualDeparture(Instant actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    @Override
    public String toString() {
        return "Booking[" + id + " " + vessel.getImo() + " @" + berthId + " " + start + ".." + end
                + " " + status + "]";
    }
}
