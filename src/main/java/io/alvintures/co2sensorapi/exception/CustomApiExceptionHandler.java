package io.alvintures.co2sensorapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(SensorApiException.class)
    protected ResponseEntity<Object> handleSensorApiException(SensorApiException exception) {
        return new ResponseEntity<>(
                new ApiExceptionResponse(exception.getStatus(), exception.getMessage()),
                exception.getStatus()
        );
    }

    /**
     * The response to the consumer if tthere is unhandled {@link SensorApiException}.
     */
    public static class ApiExceptionResponse {
        private String status;
        private String message;

        public ApiExceptionResponse(HttpStatus status, String message) {
            this.status = status.getReasonPhrase();
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
