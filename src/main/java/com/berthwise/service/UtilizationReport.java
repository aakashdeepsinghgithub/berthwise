package com.berthwise.service;

import com.berthwise.model.Berth;
import com.berthwise.model.Booking;
import com.berthwise.model.BookingStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fraction of a time window each berth was occupied. */
public class UtilizationReport {

    public Map<String, Double> utilization(Collection<Booking> bookings, Collection<Berth> berths,
                                           Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("Report window must end after it starts");
        }
        double windowHours = Duration.between(from, to).toMinutes() / 60.0;

        Map<String, Double> result = new LinkedHashMap<>();
        berths.stream()
                .sorted(Comparator.comparing(Berth::getId))
                .forEach(b -> result.put(b.getId(), 0.0));

        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                continue;
            }
            if (booking.getEnd().isBefore(from) || booking.getStart().isAfter(to)) {
                continue;
            }
            double hours = Duration.between(booking.getStart(), booking.getEnd()).toMinutes() / 60.0;
            result.merge(booking.getBerthId(), hours / windowHours, Double::sum);
        }
        return result;
    }
}
