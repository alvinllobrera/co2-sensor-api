package io.alvintures.co2sensorapi.dao.mapper;

import io.alvintures.co2sensorapi.dto.Sensor;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SensorRowMapper implements RowMapper<Sensor> {

    @Override
    public Sensor mapRow(ResultSet rs, int rowNum) throws SQLException {
        final Sensor sensor = new Sensor();
        sensor.setUuid(rs.getString("uuid"));
        sensor.setStatus(SensorStatus.valueOf(rs.getString("status")));
        return sensor;
    }
}
