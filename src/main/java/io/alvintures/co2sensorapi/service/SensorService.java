package io.alvintures.co2sensorapi.service;

import io.alvintures.co2sensorapi.dao.SensorDao;
import io.alvintures.co2sensorapi.dto.Measurement;
import io.alvintures.co2sensorapi.dto.Sensor;
import io.alvintures.co2sensorapi.dto.SensorMetrics;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import io.alvintures.co2sensorapi.exception.SensorApiException;
import io.alvintures.co2sensorapi.util.TimestampUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class SensorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sensor.class);

    @Value("${app.sensor.threshold:2000}")
    private long co2LevelThreshold;

    @Value("${app.sensor.alert.trigger.count:3}")
    private int co2AlertTriggerCount;

    @Autowired
    private SensorDao sensorDao;

    /**
     * This stores the new measurement if it is valid.
     * This method will also upsert the sensor based on the initial measurement for new sensor
     * or on the recent measurements if previous reading has already been collected for the sensor.
     */
    @Transactional
    public void storeMeasurement(final String sensorUuid, final long co2Level, final String timestamp) {
        final Optional<SensorStatus> sensorComputedStatus = getSensorStatusFromNewMeasurement(sensorUuid, co2Level, timestamp);

        // create or update sensor
        Optional<Sensor> sensorOpt = sensorDao.getSensor(sensorUuid);
        if(!sensorOpt.isPresent()) {
            final Sensor sensorToCreate = new Sensor();
            sensorToCreate.setUuid(sensorUuid);
            sensorToCreate.setStatus(co2Level > co2LevelThreshold ? SensorStatus.WARN : SensorStatus.OK);
            sensorDao.createSensor(sensorToCreate);
        } else {
            final Sensor existingSensor = sensorOpt.get();
            if (sensorComputedStatus.isPresent()) {
                if (sensorComputedStatus.get() != existingSensor.getStatus()) {
                    sensorDao.updateSensorStatus(sensorUuid, sensorComputedStatus.get());
                }
            } else {
                if (existingSensor.getStatus() == SensorStatus.OK) {
                    sensorDao.updateSensorStatus(sensorUuid, SensorStatus.WARN);
                }

            }
        }
    }

    public Optional<SensorStatus> getStatus(String sensorUuid) {
        return sensorDao.getSensor(sensorUuid).map(Sensor::getStatus);
    }

    /**
     * Returns sensor metrics which include the max and average CO2 level in the specified period of days.
     */
    public Map<String, Long> getMetrics(String sensorUuid, int periodInDays) {
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Long startDate = TimestampUtil.toUtcTimestamp(
                now.minusDays(periodInDays).toLocalDate().atStartOfDay(now.getZone())
        );
        final Long endDate = TimestampUtil.toUtcTimestamp(now);
        LOGGER.info("Retrieving metrics from {} to {}", startDate, endDate);
        final SensorMetrics sensorMetrics = sensorDao.getMetrics(sensorUuid, startDate, endDate);

        final String daysLabel = periodInDays > 1 ? "Days" : "Day";
        final Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("maxLast" + periodInDays + daysLabel, sensorMetrics.getMax());
        metrics.put("avgLast" + periodInDays + daysLabel, sensorMetrics.getAverage());
        return metrics;
    }

    /**
     * Returns the recent ALERT triggered by the sensor as well as the start and end date.
     */
    public Map<String, Object> getAlerts(String sensorUuid, int limit) {
        final List<Measurement> alerts = sensorDao.getAlerts(sensorUuid, limit);

        if (alerts.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, Object> metrics = new LinkedHashMap<>();

        final LongSummaryStatistics alertStats = alerts.stream().mapToLong(Measurement::getTime).summaryStatistics();
        metrics.put("startTime", TimestampUtil.toIsoDateTime(alertStats.getMin()));
        metrics.put("endTime", TimestampUtil.toIsoDateTime(alertStats.getMax()));

        final Map<String, Long> co2Measurements = IntStream.range(0, alerts.size())
                .boxed()
                .collect(Collectors.toMap(
                        counter -> "measurement" + (counter + 1),
                        counter -> alerts.get(counter).getCo2Level()
                ));
        metrics.putAll(co2Measurements);

        return metrics;
    }

    /**
     * This stores the new measurement and returns a sensor status if recent measurements falls on the same
     * reading based on the {@link this#co2LevelThreshold}.
     */
    private Optional<SensorStatus> getSensorStatusFromNewMeasurement(String sensorUuid, long co2Level, String timestamp) {
        final Long newMeasurementTime = convertIsoTimeToSystemTimestamp(timestamp);
        final List<Measurement> sensorLatestMeasurements = sensorDao.getRecentMeasurements(sensorUuid, co2AlertTriggerCount - 1);

        // Ensure that new measurement does not belong to the same minute of the day.
        if (!sensorLatestMeasurements.isEmpty()) {
            ensureNewReadingBelongsToNewMinute(sensorLatestMeasurements.get(0).getTime(), newMeasurementTime);
        }

        final Measurement newMeasurement = toMeasurement(sensorUuid, co2Level, newMeasurementTime);
        sensorLatestMeasurements.add(newMeasurement);

        final Optional<SensorStatus> sensorComputedStatus = analyzeSensorStatus(sensorLatestMeasurements);

        // Check if new measurement will trigger ALERT
        final boolean isAlert = sensorComputedStatus
                .map(computedStatus -> computedStatus == SensorStatus.ALERT
                        && sensorLatestMeasurements.size() == co2AlertTriggerCount)
                .orElse(false);
        LOGGER.info("New measurement isAlert: {}", isAlert);
        newMeasurement.setAlert(isAlert);

        sensorDao.createMeasurement(newMeasurement);

        return sensorComputedStatus;
    }

    private Measurement toMeasurement(final String sensorUuid, final long co2Level, final Long timestamp) {
        final Measurement measurement = new Measurement();
        measurement.setSensorUUID(sensorUuid);
        measurement.setCo2Level(co2Level);
        measurement.setTime(timestamp);
        return measurement;
    }

    private Long convertIsoTimeToSystemTimestamp(String timestamp) {
        try {
            return TimestampUtil.toUtcTimestamp(ZonedDateTime.parse(timestamp));
        } catch (Exception ex) {
            throw new SensorApiException(HttpStatus.BAD_REQUEST, "time " + timestamp + " is in invalid format.");
        }
    }

    /**
     * Throws {@link SensorApiException} if lastMeasurementTime and newMeasurementTime belongs on the same minute of the day.
     */
    private void ensureNewReadingBelongsToNewMinute(final Long lastMeasurementTime, final Long newMeasurementTime) {
        final ZonedDateTime lastReading = TimestampUtil.toZonedDateTime(lastMeasurementTime);
        final ZonedDateTime newReading = TimestampUtil.toZonedDateTime(newMeasurementTime);

        final boolean belongsOnSameMinute = lastReading.toLocalDate().isEqual(newReading.toLocalDate())
                && lastReading.toLocalTime().withSecond(0).withNano(0).equals(
                        newReading.toLocalTime().withSecond(0).withNano(0)
                );

        if (belongsOnSameMinute) {
            throw new SensorApiException(HttpStatus.CONFLICT, "New measurement should not be recorded on the same minute.");
        }
    }

    /**
     * Returns ALERT if all the recent measurements has CO2 level exceeding {@link this#co2LevelThreshold}.
     * If all recent measurements are equal or below threshold, it will return OK.
     * If none of the above criteria were satisfied, it will return empty.
     */
    private Optional<SensorStatus> analyzeSensorStatus(List<Measurement> measurements) {
        final Map<SensorStatus, Long> countByStatus = measurements.stream()
                .collect(Collectors.groupingBy(
                        measurement -> measurement.getCo2Level() > co2LevelThreshold ? SensorStatus.ALERT : SensorStatus.OK,
                        Collectors.counting()
                ));
        LOGGER.info("sensor measurement status count {}", countByStatus);

        if (countByStatus.size() == 1) {
            return Optional.of(countByStatus.keySet().iterator().next());
        } else {
            return Optional.empty();
        }
    }
}
