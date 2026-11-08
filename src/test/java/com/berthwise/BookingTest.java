package com.berthwise;

import com.berthwise.model.Booking;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.berthwise.TestData.T0;
import static com.berthwise.TestData.booking;
import static com.berthwise.TestData.container;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTest {

    @Test
    void overlappingBookingsOnSameBerthConflict() {
        Booking a = booking(container(), "B1", T0, T0.plus(Duration.ofHours(6)));
        Booking b = booking(container(), "B1", T0.plus(Duration.ofHours(5)), T0.plus(Duration.ofHours(9)));
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void differentBerthsNeverConflict() {
        Booking a = booking(container(), "B1", T0, T0.plus(Duration.ofHours(6)));
        Booking b = booking(container(), "B3", T0, T0.plus(Duration.ofHours(6)));
        assertFalse(a.overlaps(b));
    }

    @Test
    void backToBackBookingsDoNotOverlap() {
        Booking a = booking(container(), "B1", T0, T0.plus(Duration.ofHours(6)));
        Booking b = booking(container(), "B1", T0.plus(Duration.ofHours(6)), T0.plus(Duration.ofHours(10)));
        assertFalse(a.overlaps(b), "[start, end) intervals that touch must not conflict");
    }

    @Test
    void endMustBeAfterStart() {
        assertThrows(IllegalArgumentException.class, () -> booking(container(), "B1", T0, T0));
    }
}
