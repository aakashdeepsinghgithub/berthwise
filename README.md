# BerthWise: Port Berth Allocation & Tidal-Window Scheduling Service

BerthWise is a backend service for a mid-sized Indian seaport. It decides **which berth a
vessel can tie up at, and when**. It checks the vessel against the berth's length, its water
depth, the tide at arrival, and the existing schedule. The service also keeps a priority
waitlist, issues invoices, and reports berth utilization.

It is plain Java 17 with no runtime dependencies. The HTTP layer uses the JDK's built-in
`com.sun.net.httpserver`. Tests use JUnit 5.

```
mvn compile
mvn test
mvn exec:java            # starts the API on http://localhost:8080 using ./data/*.csv
```

---

## 1. Domain

| Concept  | Description |
|----------|-------------|
| **Vessel** | Identified **only** by its IMO number (`IMO` + 7 digits). Name, type, LOA (length overall, m), beam (m), draft (m), gross tonnage (GT). Two vessel objects with the same IMO are the same vessel, even if the name differs (ships get renamed). |
| **Berth** | Id, name, quay length (m), depth at chart datum (m), set of allowed vessel types. |
| **Booking** | A vessel occupying a berth over the half-open interval **[start, end)**. Status: `TENTATIVE → CONFIRMED → COMPLETED`, or `CANCELLED`. |
| **Waitlist** | Requests that could not be placed yet. |

Vessel types and berth-hire rates:

| Type      | Hourly berth hire |
|-----------|------------------:|
| CONTAINER | ₹1,800 |
| BULK      | ₹1,200 |
| TANKER    | ₹2,100 |
| RORO      | ₹1,500 |
| CRUISE    | ₹2,500 |

All money is held as **`long` paise** (₹1 = 100 paise). Floating-point money is not allowed.

## 2. Business rules

### 2.1 Physical fit
A vessel fits a berth only if **all** of these are true:

1. **Type**: the berth allows the vessel's type.
2. **Length with fender margin**: the quay must be at least 10% longer than the ship:
   `berth.length >= vessel.loa × 1.10`
3. **Depth with under-keel clearance (UKC)**: at the **arrival** instant,
   `berth.depthAtDatum + tideHeight(arrival) >= vessel.draft + UKC`,
   where `UKC = 10% of draft`.

### 2.2 Tide model
The port has a semidiurnal tide, modelled as

```
height(t) = mean + amplitude × cos( 2π × hoursSince(highWaterRef, t) / 12.42 )
```

`hoursSince` is **fractional** hours. For example, 90 minutes is 1.5 h. The port uses
mean = 1.6 m, amplitude = 1.2 m, and a high-water reference of 2026-01-01 03:10 IST.

### 2.3 Scheduling
* Bookings are half-open intervals: **[start, end)**. A booking ending at 10:00 and another
  starting at 10:00 on the same berth **do not conflict** (back-to-back is allowed and common).
* Stay must be at least **2 hours** and at most **14 days**. Start cannot be in the past.
* If the request names a berth, only that berth is tried. An unknown berth id is a
  validation error (HTTP 422), not a server error.
* If no berth is named, berths are tried in **best-fit order**: shortest quay first.
* `CANCELLED` bookings never block a berth. `TENTATIVE`, `CONFIRMED` and `COMPLETED` do.
* Two concurrent requests for the same berth and time **must never both succeed**.
* A new booking is `TENTATIVE`, which is a hold. Holds not confirmed within **30 minutes**
  are cancelled automatically by a background sweeper and removed from the berth schedule.
* Status transitions:
  * confirm: `TENTATIVE` only
  * depart: `CONFIRMED` only, which sets `actualDeparture` and makes it `COMPLETED`
  * cancel: `TENTATIVE` or `CONFIRMED` only. Cancelling a `COMPLETED` or `CANCELLED`
    booking is a conflict (HTTP 409).
* After a cancellation, the head of the waitlist is retried once.
* Callers must not be able to change the repository's internal state through returned
  collections.

### 2.4 Waitlist
* Priority runs from 1 to 5, where **5 is the most urgent** (for example, hospital supplies
  or LNG). Higher priority is served first.
* Ties are broken FIFO by `requestedAt`.
* `GET /waitlist` lists requests **in the order they will be served**.

### 2.5 Tariff (invoice)
For a booking, with `departure = actualDeparture` if present, otherwise `end`:

| Line | Rule |
|------|------|
| Port dues | GT × ₹25.00 **per started 24-hour period** of the booked stay [start, end) |
| Berth hire | hourly rate × **started hours** of the booked stay (2 h 1 min is charged as 3 h) |
| Demurrage | when departure is after `end`: 1.5 × hourly rate × started overstay hours |
| GST | 18% of (port dues + berth hire + demurrage), **rounded half-up** to the paisa |
| Total | subtotal + GST |

Invoices must be correct for the largest ships we handle (cruise ships of 228,000 GT staying
the full 14 days).

### 2.6 Utilization report
For a window [from, to) and each berth:

```
utilization = (hours of CONFIRMED or COMPLETED bookings overlapping the window,
               clipped to the window) / window hours
```

TENTATIVE holds are not counted. Every berth appears in the output, even at 0.0.

### 2.7 Events
The allocator publishes events: `CREATED`, `CONFIRMED`, `CANCELLED`, `WAITLISTED`,
`COMPLETED`. Listeners run asynchronously. Listeners **must receive events in publication
order**. A failing listener must be **logged** and must not be silently ignored.

### 2.8 Time
All times the API accepts or returns are **port local time (Asia/Kolkata)** in the format
`yyyy-MM-dd HH:mm`. The result must not depend on the server's time zone. Production runs
in UTC containers.

## 3. HTTP API

| Method & path | Body / query | Success | Errors |
|---|---|---|---|
| `POST /bookings` | `{"vesselImo","berthId"?,"start","end","priority"?}` | 201 booking, or 202 waitlisted | 400 malformed, 422 rule violation |
| `GET /bookings` | `?berth=B1` (optional) | 200 list | |
| `GET /bookings/{id}` | | 200 | 404 |
| `POST /bookings/{id}/confirm` | | 200 | 404, 409 |
| `POST /bookings/{id}/cancel` | | 200 | 404, 409 |
| `POST /bookings/{id}/depart` | `?at=yyyy-MM-dd HH:mm` | 200 | 404, 409 |
| `GET /bookings/{id}/invoice` | | 200 | 404 |
| `GET /tide` | `?at=...` (URL-encoded) | 200 `{height}` | 400 |
| `GET /report` | `?from=...&to=...` | 200 `{berthId: utilization}` | 400 |
| `GET /waitlist` | | 200 list in service order | |

Query parameters are URL-encoded, for example `?at=2026-11-09%2015%3A30`.

## 4. Data files
`data/vessels.csv` and `data/berths.csv`. Both have a header row followed by **all** data
rows. In `allowed_types`, `|` separates multiple values.

## 5. Layout
```
model/    Vessel, VesselType, Berth, Booking, BookingStatus, BookingRequest
tide/     TideModel
service/  ClearanceChecker, BerthAllocator, AllocationResult, TariffCalculator, Invoice,
          WaitlistQueue, UtilizationReport
repo/     BookingRepository
events/   EventBus, BookingEvent
io/       FleetCsvLoader
api/      HttpApi, JsonUtil
util/     TimeUtil
App.java  wiring + hold sweeper
```
