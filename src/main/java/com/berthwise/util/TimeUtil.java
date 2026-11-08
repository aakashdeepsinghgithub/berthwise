package com.berthwise.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Conversions between API text (port local time) and Instants. */
public final class TimeUtil {

    public static final ZoneId PORT_ZONE = ZoneId.of("Asia/Kolkata");
    public static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeUtil() {
    }

    /** Parses "yyyy-MM-dd HH:mm" in port local time. */
    public static Instant parseLocal(String text) {
        return LocalDateTime.parse(text.trim(), LOCAL_FORMAT)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    /** Formats an instant as "yyyy-MM-dd HH:mm" in port local time. */
    public static String formatLocal(Instant instant) {
        return LOCAL_FORMAT.format(instant.atZone(PORT_ZONE));
    }
}
