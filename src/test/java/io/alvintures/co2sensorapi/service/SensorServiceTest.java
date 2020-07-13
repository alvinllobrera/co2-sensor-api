package io.alvintures.co2sensorapi.service;

import io.alvintures.co2sensorapi.dao.SensorDao;
import io.alvintures.co2sensorapi.dto.Measurement;
import io.alvintures.co2sensorapi.dto.Sensor;
import io.alvintures.co2sensorapi.dto.SensorMetrics;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import io.alvintures.co2sensorapi.exception.SensorApiException;
import io.alvintures.co2sensorapi.util.TimestampUtil;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class SensorServiceTest {

    @MockBean
    private SensorDao sensorDaoMock;

    @Autowired
    private SensorService serviceToTest;

    @Test
    void storeMeasurement_createSensorIfNotYetExisting_initialStatusIsOk() {
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.empty());
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt())).thenReturn(new ArrayList<>());

        serviceToTest.storeMeasurement(UUID.randomUUID().toString(), 2000, ZonedDateTime.now().toString());

        final ArgumentCaptor<Sensor> sensorArgCaptor = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorDaoMock).createSensor(sensorArgCaptor.capture());
        assertThat(sensorArgCaptor.getValue().getStatus())
                .as("if c02 level <= 2000, initial status of sensor should be OK")
                .isEqualTo(SensorStatus.OK);
        verify(sensorDaoMock).createMeasurement(any()); // Check if store measurement is called as well
    }

    @Test
    void storeMeasurement_createSensorIfNotYetExisting_initialStatusIsWarn() {
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.empty());
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt())).thenReturn(Lists.newArrayList());

        serviceToTest.storeMeasurement(UUID.randomUUID().toString(), 3000, ZonedDateTime.now().toString());

        final ArgumentCaptor<Sensor> sensorArgCaptor = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorDaoMock).createSensor(sensorArgCaptor.capture());
        assertThat(sensorArgCaptor.getValue().getStatus())
                .as("if c02 level > 2000, initial status of sensor should be WARN")
                .isEqualTo(SensorStatus.WARN);
        verify(sensorDaoMock).createMeasurement(any()); // Check if store measurement is called as well
    }

    @Test
    void storeMeasurement_hasExistingMeasurement_shouldNotAcceptNewMeasurementOnTheSameMinute() {
        final ZonedDateTime now = ZonedDateTime.now();
        final String sensorUuid = UUID.randomUUID().toString();
        final Measurement existingMeasurement = buildMeasurement(now, sensorUuid, 3000L);
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt())).thenReturn(Lists.newArrayList(existingMeasurement));

        assertThatThrownBy(() -> serviceToTest.storeMeasurement(sensorUuid, 1000, now.toString()))
                .isInstanceOf(SensorApiException.class)
                .hasMessage("New measurement should not be recorded on the same minute.");
    }

    @Test
    void storeMeasurement_shouldNotAcceptNonIso8601Date() {
        final String sensorUuid = UUID.randomUUID().toString();
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt())).thenReturn(Lists.newArrayList());

        assertThatThrownBy(() -> serviceToTest.storeMeasurement(sensorUuid, 1000, "2020-12-12"))
                .isInstanceOf(SensorApiException.class)
                .hasMessage("time 2020-12-12 is in invalid format.");
    }

    @Test
    void storeMeasurement_hasExistingMeasurement_testWarnTrigger() {
        final ZonedDateTime now = ZonedDateTime.now();
        final String sensorUuid = UUID.randomUUID().toString();
        final Measurement existingMeasurement1 = buildMeasurement(now, sensorUuid, 1000L);
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt()))
                .thenReturn(Lists.newArrayList(existingMeasurement1, existingMeasurement1));
        final Sensor sensor = buildSensor(sensorUuid, SensorStatus.OK);
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.of(sensor));

        serviceToTest.storeMeasurement(sensorUuid, 2100L, now.plusMinutes(1).toString());

        verify(sensorDaoMock).updateSensorStatus(eq(sensorUuid), eq(SensorStatus.WARN));
        verify(sensorDaoMock).createMeasurement(any()); // Check if store measurement is called as well
    }

    @Test
    void storeMeasurement_hasExistingMeasurement_testAlertTrigger() {
        final ZonedDateTime now = ZonedDateTime.now();
        final String sensorUuid = UUID.randomUUID().toString();
        final Measurement existingMeasurement1 = buildMeasurement(now, sensorUuid, 3000L);
        final Measurement existingMeasurement2 = buildMeasurement(now.plusMinutes(1), sensorUuid, 2500L);
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt()))
                .thenReturn(Lists.newArrayList(existingMeasurement1, existingMeasurement2));
        final Sensor sensor = buildSensor(sensorUuid, SensorStatus.WARN);
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.of(sensor));

        serviceToTest.storeMeasurement(sensorUuid, 2100L, now.plusMinutes(2).toString());

        verify(sensorDaoMock).updateSensorStatus(eq(sensorUuid), eq(SensorStatus.ALERT));
        verify(sensorDaoMock).createMeasurement(any()); // Check if store measurement is called as well
    }

    @Test
    void storeMeasurement_hasExistingMeasurement_testSwitchStatusToOk() {
        final ZonedDateTime now = ZonedDateTime.now();
        final String sensorUuid = UUID.randomUUID().toString();
        final Measurement existingMeasurement1 = buildMeasurement(now, sensorUuid, 1000L);
        final Measurement existingMeasurement2 = buildMeasurement(now.plusMinutes(1), sensorUuid, 1900L);
        when(sensorDaoMock.getRecentMeasurements(anyString(), anyInt()))
                .thenReturn(Lists.newArrayList(existingMeasurement1, existingMeasurement2));
        final Sensor sensor = buildSensor(sensorUuid, SensorStatus.ALERT);
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.of(sensor));

        serviceToTest.storeMeasurement(sensorUuid, 1700L, now.plusMinutes(2).toString());

        verify(sensorDaoMock).updateSensorStatus(eq(sensorUuid), eq(SensorStatus.OK));
        verify(sensorDaoMock).createMeasurement(any()); // Check if store measurement is called as well
    }

    @Test
    void getStatus_whenSensorExists() {
        final String sensorUuid = UUID.randomUUID().toString();
        final Sensor sensor = buildSensor(sensorUuid, SensorStatus.WARN);
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.of(sensor));

        final Optional<SensorStatus> sensorStatus = serviceToTest.getStatus(sensorUuid);

        assertThat(sensorStatus.isPresent()).isTrue();
        assertThat(sensorStatus.get()).isEqualTo(sensor.getStatus());
    }

    @Test
    void getStatus_whenSensorDoesNotExists() {
        when(sensorDaoMock.getSensor(anyString())).thenReturn(Optional.empty());

        final Optional<SensorStatus> sensorStatus = serviceToTest.getStatus(UUID.randomUUID().toString());

        assertThat(sensorStatus.isPresent()).isFalse();
    }

    @Test
    void getMetrics_for1Day() {
        final SensorMetrics sensorMetrics = new SensorMetrics();
        sensorMetrics.setAverage(100L);
        sensorMetrics.setMax(200L);
        when(sensorDaoMock.getMetrics(anyString(), anyLong(), anyLong())).thenReturn(sensorMetrics);
        final String sensorUuid = UUID.randomUUID().toString();
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        final Map<String, Long> metrics = serviceToTest.getMetrics(sensorUuid, 1);

        assertThat(metrics)
                .as("should properly construct/map metrics")
                .containsEntry("maxLast1Day", 200L)
                .containsEntry("avgLast1Day", 100L);
        // Check if passed start and end date were correct
        verify(sensorDaoMock).getMetrics(
                eq(sensorUuid),
                eq(TimestampUtil.toUtcTimestamp(now.minusDays(1).toLocalDate().atStartOfDay(now.getZone()))),
                anyLong()
                // longThat(arg -> arg > TimestampUtil.toUtcTimestamp(now))
        );
    }

    @Test
    void getMetrics_moreThan1Day() {
        final SensorMetrics sensorMetrics = new SensorMetrics();
        sensorMetrics.setAverage(100L);
        sensorMetrics.setMax(200L);
        when(sensorDaoMock.getMetrics(anyString(), anyLong(), anyLong())).thenReturn(sensorMetrics);
        final String sensorUuid = UUID.randomUUID().toString();
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        final Map<String, Long> metrics = serviceToTest.getMetrics(sensorUuid, 5);

        assertThat(metrics)
                .as("should properly construct/map metrics")
                .containsEntry("maxLast5Days", 200L)
                .containsEntry("avgLast5Days", 100L);
        // Check if passed start and end date were correct
        verify(sensorDaoMock).getMetrics(
                eq(sensorUuid),
                eq(TimestampUtil.toUtcTimestamp(now.minusDays(5).toLocalDate().atStartOfDay(now.getZone()))),
                anyLong()
                // longThat(arg -> arg > TimestampUtil.toUtcTimestamp(now))
        );
    }

    @Test
    void getAlerts_hasAlerts() {
        final ZonedDateTime now = ZonedDateTime.now();
        final String sensorUuid = UUID.randomUUID().toString();
        final Measurement alert1 = buildMeasurement(now, sensorUuid, 2500L);
        alert1.setAlert(true);
        final Measurement alert2 = buildMeasurement(now.plusMinutes(1), sensorUuid, 2300L);
        alert2.setAlert(true);
        when(sensorDaoMock.getAlerts(anyString(), anyInt())).thenReturn(Lists.newArrayList(alert1, alert2));

        final Map<String, Object> alertList = serviceToTest.getAlerts(sensorUuid, 3);

        assertThat(alertList)
                .as("should contain the starting time of all alerts")
                .containsEntry("startTime", TimestampUtil.toIsoDateTime(alert1.getTime()))
                .as("should contain the ending time of all alerts")
                .containsEntry("endTime", TimestampUtil.toIsoDateTime(alert2.getTime()))
                .as("should contain all the CO2 levels that triggers the alert")
                .containsEntry("measurement1", alert1.getCo2Level())
                .containsEntry("measurement2", alert2.getCo2Level());
    }

    @Test
    void getAlerts_emptyAlerts() {
        when(sensorDaoMock.getAlerts(anyString(), anyInt())).thenReturn(Collections.emptyList());

        final Map<String, Object> alertList = serviceToTest.getAlerts(UUID.randomUUID().toString(), 3);

        assertThat(alertList).isEmpty();
    }

    private Sensor buildSensor(String sensorUuid, SensorStatus status) {
        final Sensor sensor = new Sensor();
        sensor.setUuid(sensorUuid);
        sensor.setStatus(status);
        return sensor;
    }

    private Measurement buildMeasurement(ZonedDateTime now, String sensorUuid, long co2Level) {
        final Measurement existingMeasurement1 = new Measurement();
        existingMeasurement1.setCo2Level(co2Level);
        existingMeasurement1.setSensorUUID(sensorUuid);
        existingMeasurement1.setTime(TimestampUtil.toUtcTimestamp(now));
        return existingMeasurement1;
    }
}