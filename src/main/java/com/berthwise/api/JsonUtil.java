package com.berthwise.api;

import com.berthwise.model.Booking;
import com.berthwise.model.BookingRequest;
import com.berthwise.service.Invoice;
import com.berthwise.util.TimeUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Minimal JSON helpers so the service stays dependency-free. Handles flat objects only. */
public final class JsonUtil {

    private static final Pattern FIELD = Pattern.compile(
            "\"(\\w+)\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|-?\\d+(?:\\.\\d+)?|null|true|false)");

    private JsonUtil() {
    }

    /** Parses a flat JSON object into string values. Null values are omitted. */
    public static Map<String, String> parseFlat(String json) {
        if (json == null || !json.trim().startsWith("{")) {
            throw new IllegalArgumentException("Request body must be a JSON object");
        }
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = FIELD.matcher(json);
        while (m.find()) {
            String key = m.group(1);
            String value = m.group(3) != null ? unescape(m.group(3)) : m.group(2);
            if (!"null".equals(value)) {
                out.put(key, value);
            }
        }
        return out;
    }

    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    public static String booking(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("vesselImo", b.getVessel().getImo());
        m.put("vesselName", b.getVessel().getName());
        m.put("berthId", b.getBerthId());
        m.put("start", TimeUtil.formatLocal(b.getStart()));
        m.put("end", TimeUtil.formatLocal(b.getEnd()));
        m.put("status", b.getStatus().name());
        m.put("priority", b.getPriority());
        m.put("actualDeparture",
                b.getActualDeparture() == null ? null : TimeUtil.formatLocal(b.getActualDeparture()));
        return object(m);
    }

    public static String bookings(Collection<Booking> list) {
        return list.stream().map(JsonUtil::booking).collect(Collectors.joining(",", "[", "]"));
    }

    public static String request(BookingRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vesselImo", r.vesselImo());
        m.put("berthId", r.berthId());
        m.put("start", TimeUtil.formatLocal(r.start()));
        m.put("end", TimeUtil.formatLocal(r.end()));
        m.put("priority", r.priority());
        m.put("requestedAt", TimeUtil.formatLocal(r.requestedAt()));
        return object(m);
    }

    public static String requests(Collection<BookingRequest> list) {
        return list.stream().map(JsonUtil::request).collect(Collectors.joining(",", "[", "]"));
    }

    public static String invoice(Invoice i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookingId", i.bookingId());
        m.put("portDuesPaise", i.portDuesPaise());
        m.put("berthHirePaise", i.berthHirePaise());
        m.put("demurragePaise", i.demurragePaise());
        m.put("gstPaise", i.gstPaise());
        m.put("totalPaise", i.totalPaise());
        return object(m);
    }

    public static String object(Map<String, ?> map) {
        return map.entrySet().stream()
                .map(e -> quote(e.getKey()) + ":" + value(e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return quote(v.toString());
    }

    public static String error(String message) {
        return "{\"error\":" + quote(message) + "}";
    }
}
