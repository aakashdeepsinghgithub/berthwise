package com.berthwise.service;

import com.berthwise.model.Booking;
import com.berthwise.model.Vessel;

import java.time.Duration;
import java.time.Instant;

/** Computes port dues, berth hire, demurrage and GST for a booking. All amounts in paise. */
public class TariffCalculator {

    /** ₹25.00 per GT per started 24h period. */
    public static final int PORT_DUES_PAISE_PER_GT = 2_500;
    public static final double GST_RATE = 0.18;
    public static final double DEMURRAGE_MULTIPLIER = 1.5;

    public Invoice invoice(Booking booking) {
        Vessel vessel = booking.getVessel();
        int hourlyRate = vessel.getType().getHourlyRatePaise();
        Duration booked = Duration.between(booking.getStart(), booking.getEnd());

        int days = (int) ceilPeriods(booked, Duration.ofDays(1));
        long portDues = vessel.getGrossTonnage() * PORT_DUES_PAISE_PER_GT * days;

        long bookedHours = booked.toHours();
        long berthHire = bookedHours * hourlyRate;

        long demurrage = 0;
        Instant departure = booking.getActualDeparture() != null
                ? booking.getActualDeparture()
                : booking.getEnd();
        if (departure.isAfter(booking.getEnd())) {
            long overstayHours = ceilPeriods(Duration.between(booking.getEnd(), departure),
                    Duration.ofHours(1));
            demurrage = Math.round(overstayHours * hourlyRate * DEMURRAGE_MULTIPLIER);
        }

        long subtotal = portDues + berthHire + demurrage;
        long gst = (long) (subtotal * GST_RATE);
        return new Invoice(booking.getId(), portDues, berthHire, demurrage, gst, subtotal + gst);
    }

    /** Number of started periods: 0 for zero duration, otherwise rounds up. */
    static long ceilPeriods(Duration duration, Duration period) {
        long nanos = duration.toNanos();
        long periodNanos = period.toNanos();
        return (nanos + periodNanos - 1) / periodNanos;
    }
}
