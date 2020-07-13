package io.alvintures.co2sensorapi.dto;

public class SensorMetrics {
    private Long max;
    private Long average;

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
    }

    public Long getAverage() {
        return average;
    }

    public void setAverage(Long average) {
        this.average = average;
    }
}
