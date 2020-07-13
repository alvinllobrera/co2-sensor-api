CREATE TABLE IF NOT EXISTS sensor
(
    uuid VARCHAR(36) NOT NULL,
    status VARCHAR(10),
    PRIMARY KEY(uuid)
);

CREATE TABLE IF NOT EXISTS measurement
(
    sensor_uuid VARCHAR(36) NOT NULL,
    timestamp_utc BIGINT NOT NULL,
    co2_level INTEGER NOT NULL,
    is_alert VARCHAR(5)
);