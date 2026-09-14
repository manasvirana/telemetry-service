package com.lane.telemetry.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SummaryResponse(
        @JsonProperty("device_id") String deviceId,
        Long from,
        Long to,
        @JsonProperty("avg_speed_kmph") Double avgSpeedKmph,
        @JsonProperty("max_accel_magnitude") Double maxAccelMagnitude,
        @JsonProperty("event_count") long eventCount
) {}