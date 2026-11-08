package com.berthwise.api;

import com.berthwise.model.Berth;
import com.berthwise.model.Booking;
import com.berthwise.model.BookingRequest;
import com.berthwise.repo.BookingRepository;
import com.berthwise.service.AllocationResult;
import com.berthwise.service.BerthAllocator;
import com.berthwise.service.TariffCalculator;
import com.berthwise.service.UtilizationReport;
import com.berthwise.service.WaitlistQueue;
import com.berthwise.tide.TideModel;
import com.berthwise.util.TimeUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** REST-ish API over the JDK HTTP server. See README section 3. */
public class HttpApi {

    private static final Logger LOG = Logger.getLogger(HttpApi.class.getName());

    private final BerthAllocator allocator;
    private final BookingRepository repository;
    private final TariffCalculator tariff;
    private final UtilizationReport report;
    private final TideModel tide;
    private final WaitlistQueue waitlist;
    private final Collection<Berth> berths;
    private final Clock clock;

    private HttpServer server;
    private ExecutorService workers;

    public HttpApi(BerthAllocator allocator, BookingRepository repository, TariffCalculator tariff,
                   UtilizationReport report, TideModel tide, WaitlistQueue waitlist,
                   Collection<Berth> berths, Clock clock) {
        this.allocator = allocator;
        this.repository = repository;
        this.tariff = tariff;
        this.report = report;
        this.tide = tide;
        this.waitlist = waitlist;
        this.berths = berths;
        this.clock = clock;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/bookings", ex -> guarded(ex, this::handleBookings));
        server.createContext("/tide", ex -> guarded(ex, this::handleTide));
        server.createContext("/report", ex -> guarded(ex, this::handleReport));
        server.createContext("/waitlist", ex -> guarded(ex, this::handleWaitlist));
        workers = Executors.newFixedThreadPool(8);
        server.setExecutor(workers);
        server.start();
        LOG.info("BerthWise API listening on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            workers.shutdown();
        }
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange ex) throws Exception;
    }

    private void guarded(HttpExchange ex, Handler handler) throws IOException {
        try {
            handler.handle(ex);
        } catch (NoSuchElementException e) {
            respond(ex, 404, JsonUtil.error(e.getMessage()));
        } catch (IllegalStateException e) {
            respond(ex, 409, JsonUtil.error(e.getMessage()));
        } catch (IllegalArgumentException | DateTimeException e) {
            respond(ex, 400, JsonUtil.error(e.getMessage()));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error for " + ex.getRequestURI(), e);
            respond(ex, 500, JsonUtil.error("Internal server error"));
        } finally {
            ex.close();
        }
    }

    private void handleBookings(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().split("/");
        Map<String, String> query = query(ex);

        if (parts.length == 2) {
            if ("POST".equals(method)) {
                create(ex);
                return;
            }
            if ("GET".equals(method)) {
                String berth = query.get("berth");
                Collection<Booking> list = berth == null ? repository.all() : repository.forBerth(berth);
                respond(ex, 200, JsonUtil.bookings(list));
                return;
            }
        } else if (parts.length == 3 && "GET".equals(method)) {
            Booking booking = repository.findById(parts[2])
                    .orElseThrow(() -> new NoSuchElementException("No booking " + parts[2]));
            respond(ex, 200, JsonUtil.booking(booking));
            return;
        } else if (parts.length == 4) {
            String id = parts[2];
            String action = parts[3];
            if ("POST".equals(method)) {
                switch (action) {
                    case "confirm" -> {
                        respond(ex, 200, JsonUtil.booking(allocator.confirm(id)));
                        return;
                    }
                    case "cancel" -> {
                        respond(ex, 200, JsonUtil.booking(allocator.cancel(id)));
                        return;
                    }
                    case "depart" -> {
                        String at = query.get("at");
                        Instant departed = at == null ? clock.instant() : TimeUtil.parseLocal(at);
                        respond(ex, 200, JsonUtil.booking(allocator.recordDeparture(id, departed)));
                        return;
                    }
                    default -> { }
                }
            } else if ("GET".equals(method) && "invoice".equals(action)) {
                Booking booking = repository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("No booking " + id));
                respond(ex, 200, JsonUtil.invoice(tariff.invoice(booking)));
                return;
            }
        }
        respond(ex, 405, JsonUtil.error("Unsupported operation"));
    }

    private void create(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = JsonUtil.parseFlat(body);

        BookingRequest request = new BookingRequest(
                require(fields, "vesselImo"),
                fields.get("berthId"),
                TimeUtil.parseLocal(require(fields, "start")),
                TimeUtil.parseLocal(require(fields, "end")),
                Integer.parseInt(fields.getOrDefault("priority", "3")),
                clock.instant());

        AllocationResult result = allocator.allocate(request);
        switch (result.outcome()) {
            case ACCEPTED -> respond(ex, 201, JsonUtil.booking(result.booking()));
            case WAITLISTED -> respond(ex, 202,
                    "{\"status\":\"WAITLISTED\",\"reason\":" + JsonUtil.quote(result.reason()) + "}");
            case REJECTED -> respond(ex, 200, JsonUtil.error(result.reason()));
        }
    }

    private void handleTide(HttpExchange ex) throws IOException {
        String at = query(ex).get("at");
        Instant t = at == null ? clock.instant() : TimeUtil.parseLocal(at);
        respond(ex, 200, String.format("{\"at\":%s,\"heightMeters\":%.3f}",
                JsonUtil.quote(TimeUtil.formatLocal(t)), tide.heightAt(t)));
    }

    private void handleReport(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex);
        Instant from = TimeUtil.parseLocal(require(q, "from"));
        Instant to = TimeUtil.parseLocal(require(q, "to"));
        respond(ex, 200, JsonUtil.object(report.utilization(repository.all(), berths, from, to)));
    }

    private void handleWaitlist(HttpExchange ex) throws IOException {
        respond(ex, 200, JsonUtil.requests(waitlist.snapshot()));
    }

    private static String require(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return value;
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> params = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return params;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return params;
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
