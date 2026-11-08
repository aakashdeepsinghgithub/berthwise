package com.berthwise.service;

import com.berthwise.events.BookingEvent;
import com.berthwise.events.EventBus;
import com.berthwise.model.Berth;
import com.berthwise.model.Booking;
import com.berthwise.model.BookingRequest;
import com.berthwise.model.BookingStatus;
import com.berthwise.model.Vessel;
import com.berthwise.repo.BookingRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Places booking requests onto berths and drives the booking lifecycle. */
public class BerthAllocator {

    static final Duration MIN_STAY = Duration.ofHours(2);
    static final Duration MAX_STAY = Duration.ofDays(14);

    private final BookingRepository repository;
    private final ClearanceChecker clearance;
    private final Map<String, Berth> berths;
    private final Map<String, Vessel> fleet;
    private final WaitlistQueue waitlist;
    private final EventBus events;
    private final Clock clock;

    public BerthAllocator(BookingRepository repository, ClearanceChecker clearance,
                          Map<String, Berth> berths, Map<String, Vessel> fleet,
                          WaitlistQueue waitlist, EventBus events, Clock clock) {
        this.repository = repository;
        this.clearance = clearance;
        this.berths = Map.copyOf(berths);
        this.fleet = Map.copyOf(fleet);
        this.waitlist = waitlist;
        this.events = events;
        this.clock = clock;
    }

    public AllocationResult allocate(BookingRequest request) {
        Vessel vessel = fleet.get(request.vesselImo());
        if (vessel == null) {
            return AllocationResult.rejected("Unknown vessel " + request.vesselImo());
        }

        Duration stay = Duration.between(request.start(), request.end());
        if (stay.compareTo(MIN_STAY) < 0 || stay.compareTo(MAX_STAY) > 0) {
            return AllocationResult.rejected("Stay must be between 2 hours and 14 days");
        }
        if (request.start().isBefore(clock.instant())) {
            return AllocationResult.rejected("Start time is in the past");
        }

        List<Berth> candidates;
        if (request.berthId() != null) {
            candidates = List.of(berths.get(request.berthId()));
        } else {
            candidates = new ArrayList<>(berths.values());
            candidates.sort(Comparator.comparingDouble(Berth::getLengthMeters));
        }

        List<String> reasons = new ArrayList<>();
        boolean physicallyPossible = false;
        for (Berth berth : candidates) {
            List<String> violations = clearance.violations(vessel, berth, request.start());
            if (!violations.isEmpty()) {
                reasons.addAll(violations);
                continue;
            }
            physicallyPossible = true;
            Booking booking = new Booking(vessel, berth.getId(), request.start(), request.end(),
                    request.priority(), clock.instant());
            if (repository.tryReserve(booking)) {
                publish(BookingEvent.Type.CREATED, booking);
                return AllocationResult.accepted(booking);
            }
            reasons.add(berth.getId() + " is occupied in the requested window");
        }

        if (!physicallyPossible) {
            return AllocationResult.rejected(String.join("; ", reasons));
        }
        waitlist.add(request);
        events.publish(new BookingEvent(BookingEvent.Type.WAITLISTED, null, vessel.getImo(),
                clock.instant()));
        return AllocationResult.waitlisted(String.join("; ", reasons));
    }

    public Booking confirm(String bookingId) {
        Booking booking = get(bookingId);
        if (booking.getStatus() != BookingStatus.TENTATIVE) {
            throw new IllegalStateException("Only TENTATIVE bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        publish(BookingEvent.Type.CONFIRMED, booking);
        return booking;
    }

    public Booking cancel(String bookingId) {
        Booking booking = get(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        publish(BookingEvent.Type.CANCELLED, booking);
        promoteFromWaitlist();
        return booking;
    }

    public Booking recordDeparture(String bookingId, Instant departedAt) {
        Booking booking = get(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can depart");
        }
        if (departedAt.isBefore(booking.getStart())) {
            throw new IllegalArgumentException("Departure cannot be before arrival");
        }
        booking.setActualDeparture(departedAt);
        booking.setStatus(BookingStatus.COMPLETED);
        publish(BookingEvent.Type.COMPLETED, booking);
        return booking;
    }

    private void promoteFromWaitlist() {
        BookingRequest next = waitlist.poll();
        if (next != null) {
            allocate(next);
        }
    }

    private Booking get(String bookingId) {
        return repository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("No booking " + bookingId));
    }

    private void publish(BookingEvent.Type type, Booking booking) {
        events.publish(new BookingEvent(type, booking.getId(), booking.getVessel().getImo(),
                clock.instant()));
    }
}
