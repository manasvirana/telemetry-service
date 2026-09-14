package com.lane.telemetry.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IngestRequest(
        @JsonProperty("device_id") String deviceId,
        List<TelemetryEvent> events
) {}