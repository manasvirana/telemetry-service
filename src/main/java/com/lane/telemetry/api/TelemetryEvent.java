package com.lane.telemetry.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelemetryEvent(
        Long timestamp,
        Double lat,
        Double lon,
        @JsonProperty("speed_kmph") Double speedKmph,
        @JsonProperty("accel_x") Double accelX,
        @JsonProperty("accel_y") Double accelY,
        @JsonProperty("accel_z") Double accelZ,
        @JsonProperty("gyro_x") Double gyroX,
        @JsonProperty("gyro_y") Double gyroY,
        @JsonProperty("gyro_z") Double gyroZ
) {}