package io.alvintures.co2sensorapi.dao;

import com.github.javafaker.Faker;
import io.alvintures.co2sensorapi.dto.Measurement;
import io.alvintures.co2sensorapi.dto.Sensor;
import io.alvintures.co2sensorapi.dto.SensorMetrics;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import io.alvintures.co2sensorapi.util.TimestampUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SensorDaoTest {

    private static final Faker FAKER = new Faker();

    @Autowired
    private SensorDao daoToTest;

    @Test
    void testCreationAndRetrievalOfSensor() {
        final Sensor sensorToSave = randomSensor(SensorStatus.WARN);

        daoToTest.createSensor(sensorToSave);

        final Optional<Sensor> createdSensor = daoToTest.getSensor(sensorToSave.getUuid());
        assertThat(createdSensor).isNotEmpty();
        assertThat(createdSensor.get().getStatus())
                .as("The status should be the one set during creation.")
                .isEqualTo(sensorToSave.getStatus());
    }

    @Test
    void testUpdateAndRetrievalOfSensor() {
        final Sensor sensor = randomSensor(SensorStatus.ALERT);
        daoToTest.createSensor(sensor);
        final Optional<Sensor> createdSensor = daoToTest.getSensor(sensor.getUuid());
        assertThat(createdSensor.get().getStatus()).isEqualTo(sensor.getStatus());

        daoToTest.updateSensorStatus(sensor.getUuid(), SensorStatus.OK);

        final Optional<Sensor> updatedSensor = daoToTest.getSensor(sensor.getUuid());
        assertThat(updatedSensor).isNotEmpty();
        assertThat(updatedSensor.get().getStatus())
                .as("The status should be updated with OK")
                .isEqualTo(SensorStatus.OK);
    }

    @Test
    void getRecentMeasurements() {
        final String sensorUuid = UUID.randomUUID().toString();
        final ZonedDateTime now = ZonedDateTime.now();
        final Measurement firstMatchingMeasurement = randomMeasurement(sensorUuid, now);
        final Measurement lastMatchingMeasurement = randomMeasurement(sensorUuid, now.plusMinutes(1));
        final Measurement nonMatchingMeasurement = randomMeasurement(UUID.randomUUID().toString(), ZonedDateTime.now());
        daoToTest.createMeasurement(lastMatchingMeasurement);
        daoToTest.createMeasurement(firstMatchingMeasurement);
        daoToTest.createMeasurement(nonMatchingMeasurement);

        final List<Measurement> measurements = daoToTest.getRecentMeasurements(sensorUuid, 3);

        assertThat(measurements.size())
                .as("Only two measurement matches")
                .isEqualTo(2);
        assertThat(measurements.get(0))
                .as("matches correct value is set and in order")
                .matches(measurement -> lastMatchingMeasurement.getTime().equals(measurement.getTime()))
                .matches(measurement -> lastMatchingMeasurement.getCo2Level().equals(measurement.getCo2Level()));
    }

    @Test
    void getMetrics() {
        final String sensorUuid = UUID.randomUUID().toString();
        final ZonedDateTime now = ZonedDateTime.now();
        final Measurement measurement1 = randomMeasurement(sensorUuid, now, true);
        measurement1.setCo2Level(1000L);
        final Measurement measurement2 = randomMeasurement(sensorUuid, now.plusMinutes(1), true);
        measurement2.setCo2Level(2000L);
        final Measurement measurement3 = randomMeasurement(sensorUuid, now.plusMinutes(2));
        measurement3.setCo2Level(3000L);
        daoToTest.createMeasurement(measurement1);
        daoToTest.createMeasurement(measurement2);
        daoToTest.createMeasurement(measurement3);

        final SensorMetrics metrics = daoToTest.getMetrics(
                sensorUuid, TimestampUtil.toUtcTimestamp(now.minusDays(1)), TimestampUtil.toUtcTimestamp(now.plusMinutes(1))
        );

        assertThat(metrics.getMax())
                .as("measurement2 has the max co2 level of 2000 because measurement3 was not part of the date range.")
                .isEqualTo(measurement2.getCo2Level());
        assertThat(metrics.getAverage())
                .as("the average of measurement 1 and 2 is 1500")
                .isEqualTo(1500L);
    }

    @Test
    void getAlerts() {
        final String sensorUuid = UUID.randomUUID().toString();
        final ZonedDateTime now = ZonedDateTime.now();
        final Measurement firstAlert = randomMeasurement(sensorUuid, now, true);
        final Measurement lastAlert = randomMeasurement(sensorUuid, now.plusMinutes(1), true);
        final Measurement nonAlert = randomMeasurement(sensorUuid, now);
        daoToTest.createMeasurement(lastAlert);
        daoToTest.createMeasurement(firstAlert);
        daoToTest.createMeasurement(nonAlert);

        final List<Measurement> measurements = daoToTest.getAlerts(sensorUuid, 3);

        assertThat(measurements.size())
                .as("Only two measurement triggers an alert")
                .isEqualTo(2);
        assertThat(measurements.get(0))
                .as("matches correct value is set and in order")
                .matches(measurement -> lastAlert.getTime().equals(measurement.getTime()))
                .matches(measurement -> lastAlert.getCo2Level().equals(measurement.getCo2Level()));
    }

    private Sensor randomSensor(SensorStatus sensorStatus) {
        final Sensor sensorToSave = new Sensor();
        sensorToSave.setUuid(UUID.randomUUID().toString());
        sensorToSave.setStatus(sensorStatus);
        return sensorToSave;
    }

    private Measurement randomMeasurement(String sensorUuid, ZonedDateTime time) {
        return randomMeasurement(sensorUuid, time, false);
    }

    private Measurement randomMeasurement(String sensorUuid, ZonedDateTime time, boolean isAlert) {
        final Measurement measurement = new Measurement();
        measurement.setSensorUUID(sensorUuid);
        measurement.setTime(TimestampUtil.toUtcTimestamp(time));
        measurement.setCo2Level(FAKER.number().numberBetween(1000L, 5000L));
        measurement.setAlert(isAlert);
        return measurement;
    }
}