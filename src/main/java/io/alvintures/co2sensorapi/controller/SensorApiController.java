package io.alvintures.co2sensorapi.controller;

import com.google.common.collect.ImmutableMap;
import io.alvintures.co2sensorapi.dto.SensorMetrics;
import io.alvintures.co2sensorapi.dto.SensorStatus;
import io.alvintures.co2sensorapi.dto.StoreMeasurementParams;
import io.alvintures.co2sensorapi.exception.SensorApiException;
import io.alvintures.co2sensorapi.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("/api/v1/sensors")
public class SensorApiController {

    @Autowired
    private SensorService sensorService;

    @Operation(description = "Store the CO2 measurement collected by the sensor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The measurement has been captured and registered."),
            @ApiResponse(responseCode = "400", description = "One or more of the parameters sent is invalid."),
            @ApiResponse(responseCode = "409", description = "The measurement is rejected due to conflicts (e.g. multiple records for a single minute"),
            @ApiResponse(responseCode = "500", description = "Something went wrong (e.g. server temporary issue).")
    })
    @PostMapping("/{uuid}/measurements")
    public void collectMeasurement(@Parameter(description = "The ID of the sensor") @PathVariable String uuid,
                                   @RequestBody StoreMeasurementParams requestBody) {
        sensorService.storeMeasurement(uuid, requestBody.getCo2(), requestBody.getTime());
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status of the sensor is found",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "404", description = "Sensor is not found."),
            @ApiResponse(responseCode = "500", description = "Something went wrong (e.g. server temporary issue).")
    })
    @Operation(description = "Retrieves the current status of the sensor.")
    @GetMapping("/{uuid}")
    public Map<String, SensorStatus> getSensorStatus(@Parameter(description = "The ID of the sensor") @PathVariable String uuid) {
        return sensorService.getStatus(uuid)
                .map(status -> ImmutableMap.of("status", status))
                .orElseThrow(() -> new SensorApiException(HttpStatus.NOT_FOUND, "No records found for sensor with ID " + uuid));
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics has been generated.",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "500", description = "Something went wrong (e.g. server temporary issue).")
    })
    @Operation(description = "Gives the maximum and average CO2 level recorded from the past 30 days.")
    @GetMapping("/{uuid}/metrics")
    public Map<String, Long> getSensorMetrics(@Parameter(description = "The ID of the sensor") @PathVariable String uuid) {
        return sensorService.getMetrics(uuid, 30);
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts has been provided.",
                    content = {@Content(mediaType = "application/json")}),
            @ApiResponse(responseCode = "500", description = "Something went wrong (e.g. server temporary issue).")
    })
    @Operation(description = "Gives the list of alerts recorded on the sensor.")
    @GetMapping("/{uuid}/alerts")
    public Map<String, Object> getSensorAlerts(@Parameter(description = "The ID of the sensor") @PathVariable String uuid) {
        return sensorService.getAlerts(uuid, 3);
    }


}
