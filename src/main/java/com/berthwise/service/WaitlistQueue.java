package com.berthwise.service;

import com.berthwise.model.BookingRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Requests that could not be placed yet. Highest priority first, FIFO within a priority. */
public class WaitlistQueue {

    private static final Comparator<BookingRequest> SERVICE_ORDER =
            Comparator.comparingInt(BookingRequest::priority)
                    .thenComparing((a, b) ->
                            (int) (a.requestedAt().toEpochMilli() - b.requestedAt().toEpochMilli()));

    private final PriorityQueue<BookingRequest> queue = new PriorityQueue<>(SERVICE_ORDER);

    public synchronized void add(BookingRequest request) {
        queue.add(request);
    }

    /** Removes and returns the next request to serve, or null if empty. */
    public synchronized BookingRequest poll() {
        return queue.poll();
    }

    public synchronized int size() {
        return queue.size();
    }

    /** All waiting requests in the order they will be served. */
    public synchronized List<BookingRequest> snapshot() {
        return new ArrayList<>(queue);
    }
}
