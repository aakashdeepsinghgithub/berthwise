package com.berthwise;

import com.berthwise.model.BookingRequest;
import com.berthwise.service.WaitlistQueue;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.berthwise.TestData.T0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WaitlistQueueTest {

    private static BookingRequest req(String imo, int priority, Duration requestedAfterT0) {
        return new BookingRequest(imo, null, T0.plus(Duration.ofDays(1)), T0.plus(Duration.ofDays(2)),
                priority, T0.plus(requestedAfterT0));
    }

    @Test
    void emptyQueueReturnsNull() {
        assertNull(new WaitlistQueue().poll());
    }

    @Test
    void fifoWithinSamePriority() {
        WaitlistQueue q = new WaitlistQueue();
        q.add(req("IMO0000002", 3, Duration.ofMinutes(5)));
        q.add(req("IMO0000001", 3, Duration.ofMinutes(1)));
        assertEquals("IMO0000001", q.poll().vesselImo());
        assertEquals("IMO0000002", q.poll().vesselImo());
    }

    @Test
    void higherPriorityServedFirst() {
        WaitlistQueue q = new WaitlistQueue();
        q.add(req("IMO0000001", 2, Duration.ZERO));
        q.add(req("IMO0000005", 5, Duration.ofMinutes(10)));
        assertEquals("IMO0000005", q.poll().vesselImo());
    }
}
