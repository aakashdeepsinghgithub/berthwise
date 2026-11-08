package com.berthwise.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Asynchronous fan-out of booking events to listeners (notifications, audit, pilots' desk). */
public class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    private final List<Consumer<BookingEvent>> listeners = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public void subscribe(Consumer<BookingEvent> listener) {
        listeners.add(listener);
    }

    public void publish(BookingEvent event) {
        for (Consumer<BookingEvent> listener : listeners) {
            executor.submit(() -> listener.accept(event));
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warning("Event listeners did not finish within 5s");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
