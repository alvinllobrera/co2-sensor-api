package io.alvintures.co2sensorapi.dto;

public class StoreMeasurementParams {
    private Long co2;
    private String time;

    public Long getCo2() {
        return co2;
    }

    public void setCo2(Long co2) {
        this.co2 = co2;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
