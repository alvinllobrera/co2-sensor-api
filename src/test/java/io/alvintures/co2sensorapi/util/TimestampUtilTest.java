package io.alvintures.co2sensorapi.util;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TimestampUtilTest {

    @Test
    void toUtcTimestamp_NonUtcZonedDateTime() {
        final ZonedDateTime germanUnityDay = ZonedDateTime.parse("1990-10-03T12:34:56+02:00");

        final Long germanUnityDayUtcTimestamp = TimestampUtil.toUtcTimestamp(germanUnityDay);

        assertThat(germanUnityDayUtcTimestamp)
                .as("should be converted in a human readable long date (yyyyMMddHHmmss) adjusted in UTC")
                .isEqualTo(19901003103456L);
    }

    @Test
    void toUtcTimestamp_utcZonedDateTime() {
        final ZonedDateTime germanUnityDay = ZonedDateTime.parse("1990-10-03T12:34:56Z");

        final Long germanUnityDayUtcTimestamp = TimestampUtil.toUtcTimestamp(germanUnityDay);

        assertThat(germanUnityDayUtcTimestamp)
                .as("should be converted in a human readable long (yyyyMMddHHmmss) with the same value for the provided UTC date.")
                .isEqualTo(19901003123456L);
    }

    @Test
    void toIsoDateTime() {
        final Long germanUnityDay = 19901003123456L; // yyyyMMddHHmmss

        final String germanUnityDayIsoDateTime = TimestampUtil.toIsoDateTime(germanUnityDay);

        assertThat(germanUnityDayIsoDateTime)
                .as("should be converted to ISO-8601 format")
                .isEqualTo("1990-10-03T12:34:56+00:00");
    }

    @Test
    void toZonedDateTime() {
        final Long germanUnityDay = 19901003123456L; // yyyyMMddHHmmss

        final ZonedDateTime germanUnityDayZonedDateTime = TimestampUtil.toZonedDateTime(germanUnityDay);

        assertThat(germanUnityDayZonedDateTime)
                .as("should be converted to correct instance of ZonedDateTime")
                .isEqualTo("1990-10-03T12:34:56+00:00");
    }
}