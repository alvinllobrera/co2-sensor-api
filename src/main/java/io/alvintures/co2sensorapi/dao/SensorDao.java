package io.alvintures.co2sensorapi.dao;

import io.alvintures.co2sensorapi.dao.mapper.MeasurementRowMapper;
import io.alvintures.co2sensorapi.dao.mapper.SensorMetricsRowMapper;
import io.alvintures.co2sensorapi.dao.mapper.SensorRowMapper;
import io.alvintures.co2sensorapi.dto.Measurement;
import io.alvintures.co2sensorapi.dto.Sensor;
import io.alvintures.co2sensorapi.dto.SensorMetrics;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SensorDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createSensor(final Sensor sensor) {
        jdbcTemplate.update(
                "INSERT INTO sensor (uuid, status) VALUES (?, ?)",
                sensor.getUuid(), sensor.getStatus().name()
        );
    }

    public void updateSensorStatus(final String sensorUuid, final SensorStatus sensorStatus) {
        jdbcTemplate.update(
                "UPDATE sensor SET status = ? WHERE uuid = ?",
                sensorStatus.name(), sensorUuid
        );
    }

    public Optional<Sensor> getSensor(String uuid) {
        final List<Sensor> sensors = jdbcTemplate.query(
                "SELECT * FROM sensor WHERE uuid = ?", new Object[]{uuid}, new SensorRowMapper()
        );

        if (sensors.size() == 1) {
            return Optional.of(sensors.get(0));
        } else if (sensors.isEmpty()) {
            return Optional.empty();
        } else {
            throw new IllegalStateException("Multiple sensor found for id " + uuid);
        }

    }
    public void createMeasurement(final Measurement measurement) {
        jdbcTemplate.update(
                "INSERT INTO measurement (sensor_uuid, timestamp_utc, co2_level, is_alert) VALUES (?, ?, ?, ?)",
                measurement.getSensorUUID(), measurement.getTime(), measurement.getCo2Level(), measurement.isAlert()
        );
    }

    public List<Measurement> getRecentMeasurements(final String sensorUuid, final int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM measurement WHERE sensor_uuid = ? ORDER BY timestamp_utc DESC LIMIT ?",
                new Object[]{sensorUuid, limit},
                new MeasurementRowMapper()
        );
    }

    public List<Measurement> getAlerts(final String sensorUuid, final int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM measurement WHERE sensor_uuid = ? AND is_alert = 'TRUE' " +
                        "ORDER BY timestamp_utc DESC LIMIT ?",
                new Object[]{sensorUuid, limit},
                new MeasurementRowMapper()
        );
    }

    public SensorMetrics getMetrics(String sensorUuid, Long startDate, Long endDate) {
        return jdbcTemplate.queryForObject(
                "SELECT AVG(co2_level) as ave_level, MAX(co2_level) as max_level FROM measurement "
                        + "WHERE sensor_uuid = ? AND timestamp_utc >= ? AND timestamp_utc <= ?",
                new Object[]{sensorUuid, startDate, endDate},
                new SensorMetricsRowMapper()
        );
    }
}
