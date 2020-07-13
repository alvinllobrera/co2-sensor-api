package io.alvintures.co2sensorapi.dao.mapper;

import io.alvintures.co2sensorapi.dto.Measurement;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MeasurementRowMapper implements RowMapper<Measurement> {

    @Override
    public Measurement mapRow(ResultSet rs, int rowNum) throws SQLException {
        final Measurement measurement = new Measurement();
        measurement.setSensorUUID(rs.getString("sensor_uuid"));
        measurement.setCo2Level(rs.getLong("co2_level"));
        measurement.setTime(rs.getLong("timestamp_utc"));
        measurement.setAlert(rs.getBoolean("is_alert"));
        return measurement;
    }
}
