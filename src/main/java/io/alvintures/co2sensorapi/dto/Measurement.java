package io.alvintures.co2sensorapi.dto;

public class Measurement {
    private String sensorUUID;
    private Long time;
    private Long co2Level;
    private boolean isAlert;

    public String getSensorUUID() {
        return sensorUUID;
    }

    public void setSensorUUID(String sensorUUID) {
        this.sensorUUID = sensorUUID;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getCo2Level() {
        return co2Level;
    }

    public void setCo2Level(Long co2Level) {
        this.co2Level = co2Level;
    }

    public boolean isAlert() {
        return isAlert;
    }

    public void setAlert(boolean alert) {
        isAlert = alert;
    }
}
