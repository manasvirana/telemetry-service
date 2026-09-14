package com.lane.telemetry.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IngestResponse(
        @JsonProperty("device_id") String deviceId,
        int accepted,
        int duplicates,
        int rejected,
        List<IngestError> errors
) {}