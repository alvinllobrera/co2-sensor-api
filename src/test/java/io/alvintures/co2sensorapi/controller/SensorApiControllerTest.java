package io.alvintures.co2sensorapi.controller;

import com.github.javafaker.Faker;
import com.google.common.collect.Maps;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import io.alvintures.co2sensorapi.dto.StoreMeasurementParams;
import io.alvintures.co2sensorapi.exception.SensorApiException;
import io.alvintures.co2sensorapi.service.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class SensorApiControllerTest {

    private static final Faker FAKER = new Faker();

    @MockBean
    private SensorService sensorServiceMock;

    @Autowired
    private SensorApiController sensorApiController;

    @Test
    void collectMeasurement() {
        final String uuid = UUID.randomUUID().toString();
        final StoreMeasurementParams requestBody = new StoreMeasurementParams();
        requestBody.setCo2(FAKER.number().randomNumber());
        requestBody.setTime(ZonedDateTime.now().toString());

        sensorApiController.collectMeasurement(uuid, requestBody);

        verify(sensorServiceMock).storeMeasurement(eq(uuid), eq(requestBody.getCo2()), eq(requestBody.getTime()));
    }

    @Test
    void getSensorStatus_hasExistingSensor() {
        final String uuid = UUID.randomUUID().toString();
        when(sensorServiceMock.getStatus(uuid)).thenReturn(Optional.of(SensorStatus.OK));

        assertThat(sensorApiController.getSensorStatus(uuid))
                .as("should return the status returned by the service")
                .isEqualTo(SensorStatus.OK);
    }

    @Test
    void getSensorStatus_noSensorFound() {
        final String uuid = UUID.randomUUID().toString();
        when(sensorServiceMock.getStatus(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sensorApiController.getSensorStatus(uuid))
                .isInstanceOf(SensorApiException.class)
                .hasMessage("No records found for sensor with ID " + uuid);
    }

    @Test
    void getSensorMetrics() {
        final String uuid = UUID.randomUUID().toString();
        final Map<String, Long> sampleResponse = Maps.newHashMap();
        when(sensorServiceMock.getMetrics(eq(uuid), anyInt())).thenReturn(sampleResponse);

        assertThat(sensorApiController.getSensorMetrics(uuid))
                .as("should return the actual object returned by the service")
                .isEqualTo(sampleResponse);
    }

    @Test
    void getSensorAlerts() {
        final String uuid = UUID.randomUUID().toString();
        final Map<String, Object> sampleResponse = Maps.newHashMap();
        when(sensorServiceMock.getAlerts(eq(uuid), anyInt())).thenReturn(sampleResponse);

        assertThat(sensorApiController.getSensorAlerts(uuid))
                .isEqualTo(sampleResponse);
    }
}