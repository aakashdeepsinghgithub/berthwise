package com.berthwise;

import com.berthwise.model.Booking;
import com.berthwise.model.BookingStatus;
import com.berthwise.service.Invoice;
import com.berthwise.service.TariffCalculator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.berthwise.TestData.T0;
import static com.berthwise.TestData.booking;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TariffCalculatorTest {

    private final TariffCalculator calc = new TariffCalculator();

    @Test
    void portDuesForOneDayStay() {
        Booking b = booking(TestData.smallBulk(), "B3", T0, T0.plus(Duration.ofHours(24)));
        assertEquals(1_000L * 2_500, calc.invoice(b).portDuesPaise());
    }

    @Test
    void berthHireForTenAndAHalfHours() {
        Booking b = booking(TestData.container(), "B1", T0, T0.plus(Duration.ofMinutes(630)));
        assertEquals(10L * 180_000, calc.invoice(b).berthHirePaise());
    }

    @Test
    void demurrageChargedForOverstay() {
        Booking b = booking(TestData.container(), "B1", T0, T0.plus(Duration.ofHours(10)));
        b.setStatus(BookingStatus.COMPLETED);
        b.setActualDeparture(T0.plus(Duration.ofHours(12)));
        Invoice inv = calc.invoice(b);
        assertEquals(2L * 270_000, inv.demurragePaise());
    }

    @Test
    void totalIsSubtotalPlusGst() {
        Booking b = booking(TestData.smallBulk(), "B3", T0, T0.plus(Duration.ofHours(24)));
        Invoice inv = calc.invoice(b);
        assertEquals(inv.subtotalPaise() + inv.gstPaise(), inv.totalPaise());
    }
}
