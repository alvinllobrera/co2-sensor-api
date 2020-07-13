package io.alvintures.co2sensorapi.dto;

public class Sensor {
    private String uuid;
    private SensorStatus status;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public SensorStatus getStatus() {
        return status;
    }

    public void setStatus(SensorStatus status) {
        this.status = status;
    }
}
