package com.berthwise;

import com.berthwise.api.HttpApi;
import com.berthwise.events.EventBus;
import com.berthwise.io.FleetCsvLoader;
import com.berthwise.model.Berth;
import com.berthwise.model.Vessel;
import com.berthwise.repo.BookingRepository;
import com.berthwise.service.BerthAllocator;
import com.berthwise.service.ClearanceChecker;
import com.berthwise.service.TariffCalculator;
import com.berthwise.service.UtilizationReport;
import com.berthwise.service.WaitlistQueue;
import com.berthwise.tide.TideModel;
import com.berthwise.util.TimeUtil;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Wires the service together and starts the API plus the hold sweeper. */
public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());
    private static final Duration HOLD_TTL = Duration.ofMinutes(30);

    private App() {
    }

    public static void main(String[] args) throws Exception {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        FleetCsvLoader loader = new FleetCsvLoader();
        List<Vessel> vessels = loader.loadVessels(dataDir.resolve("vessels.csv"));
        List<Berth> berths = loader.loadBerths(dataDir.resolve("berths.csv"));
        LOG.info("Loaded " + vessels.size() + " vessels and " + berths.size() + " berths");

        Map<String, Vessel> fleet = vessels.stream()
                .collect(Collectors.toMap(Vessel::getImo, Function.identity()));
        Map<String, Berth> berthMap = berths.stream()
                .collect(Collectors.toMap(Berth::getId, Function.identity()));

        Clock clock = Clock.systemUTC();
        TideModel tide = new TideModel(1.6, 1.2,
                LocalDateTime.of(2026, 1, 1, 3, 10).atZone(TimeUtil.PORT_ZONE).toInstant());

        BookingRepository repository = new BookingRepository();
        WaitlistQueue waitlist = new WaitlistQueue();
        EventBus events = new EventBus();
        events.subscribe(e -> LOG.info("event " + e.type() + " booking=" + e.bookingId()
                + " vessel=" + e.vesselImo()));

        BerthAllocator allocator = new BerthAllocator(repository, new ClearanceChecker(tide),
                berthMap, fleet, waitlist, events, clock);

        HttpApi api = new HttpApi(allocator, repository, new TariffCalculator(),
                new UtilizationReport(), tide, waitlist, berths, clock);
        api.start(port);

        ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor();
        sweeper.scheduleAtFixedRate(() -> {
            int n = repository.cancelExpiredHolds(clock.instant(), HOLD_TTL);
            if (n > 0) {
                LOG.info("Released " + n + " expired holds");
            }
        }, 1, 1, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sweeper.shutdownNow();
            api.stop();
            events.shutdown();
        }));
    }
}
