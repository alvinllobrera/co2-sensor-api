package io.alvintures.co2sensorapi.exception;

import org.springframework.http.HttpStatus;

public class SensorApiException extends RuntimeException {

    private final HttpStatus status;

    public SensorApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
