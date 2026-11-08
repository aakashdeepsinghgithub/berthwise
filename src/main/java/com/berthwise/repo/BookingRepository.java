package com.berthwise.repo;

import com.berthwise.model.Booking;
import com.berthwise.model.BookingStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store of bookings, indexed by id and by berth. Thread-safe. */
public class BookingRepository {

    private final Map<String, Booking> byId = new ConcurrentHashMap<>();
    private final Map<String, List<Booking>> byBerth = new ConcurrentHashMap<>();

    /** Any non-cancelled booking on the same berth that overlaps the candidate. */
    public Optional<Booking> findConflict(Booking candidate) {
        for (Booking existing : byBerth.getOrDefault(candidate.getBerthId(), List.of())) {
            if (existing.getStatus() != BookingStatus.CANCELLED && existing.overlaps(candidate)) {
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    /**
     * Stores the booking if it does not conflict with an existing one.
     *
     * @return true if stored, false if the slot is taken
     */
    public boolean tryReserve(Booking booking) {
        if (findConflict(booking).isPresent()) {
            return false;
        }
        byBerth.computeIfAbsent(booking.getBerthId(), k -> new ArrayList<>()).add(booking);
        byId.put(booking.getId(), booking);
        return true;
    }

    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Booking> forBerth(String berthId) {
        return byBerth.getOrDefault(berthId, new ArrayList<>());
    }

    public Collection<Booking> all() {
        return List.copyOf(byId.values());
    }

    /**
     * Cancels TENTATIVE holds older than {@code holdTtl} and removes them from the schedule.
     *
     * @return number of holds cancelled
     */
    public int cancelExpiredHolds(Instant now, Duration holdTtl) {
        int cancelled = 0;
        for (List<Booking> schedule : byBerth.values()) {
            for (Booking b : schedule) {
                if (b.getStatus() == BookingStatus.TENTATIVE
                        && b.getCreatedAt().plus(holdTtl).isBefore(now)) {
                    b.setStatus(BookingStatus.CANCELLED);
                    schedule.remove(b);
                    byId.remove(b.getId());
                    cancelled++;
                }
            }
        }
        return cancelled;
    }
}
