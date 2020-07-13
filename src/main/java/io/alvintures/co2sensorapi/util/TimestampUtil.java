package io.alvintures.co2sensorapi.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * This is a helper class to transform or manipulate timestamp.
 */
public class TimestampUtil {
    /**
     * Formats date time to a human readable timestamp which can be converted to Long data type for ease of comparison.
     */
    private static final DateTimeFormatter UTC_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * This is similar to {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} but instead of appending 'Z' on UTC it will
     * use the "+00:00" offset.
     */
    private static final DateTimeFormatter ISO_DATE_TIME_OFFSET_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendPattern("xxx")
            .toFormatter();

    /**
     * Examples:
     * 1) 2020-07-13T12:55:19+00:00 will return 20200712125519
     * 2) 2020-07-13T12:55:19+01:00 will return 20200712115519
     */
    public static Long toUtcTimestamp(final ZonedDateTime zonedDateTime) {
        return Long.parseLong(UTC_TIMESTAMP_FORMATTER.format(zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)));
    }

    /**
     * Examples:
     * 1) 20200713125519 will return 2020-07-13T12:55:19+00:00
     * 2) 20200713115519 will return 2020-07-13T11:55:19+00:00
     */
    public static String toIsoDateTime(final Long utcDateTime) {
        return toZonedDateTime(utcDateTime).format(ISO_DATE_TIME_OFFSET_FORMATTER);
    }

    /**
     * Same with {@link this#toIsoDateTime} but this will return an instance of {@link ZonedDateTime}.
     */
    public static ZonedDateTime toZonedDateTime(final Long utcDateTime) {
        return LocalDateTime.parse(String.valueOf(utcDateTime), UTC_TIMESTAMP_FORMATTER).atZone(ZoneOffset.UTC);
    }
}
