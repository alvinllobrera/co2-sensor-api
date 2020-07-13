package io.alvintures.co2sensorapi.dao.mapper;

import io.alvintures.co2sensorapi.dto.SensorMetrics;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SensorMetricsRowMapper implements RowMapper<SensorMetrics> {

    @Override
    public SensorMetrics mapRow(ResultSet rs, int rowNum) throws SQLException {
        final SensorMetrics sensorMetrics = new SensorMetrics();
        sensorMetrics.setAverage(rs.getLong("ave_level"));
        sensorMetrics.setMax(rs.getLong("max_level"));
        return sensorMetrics;
    }
}
