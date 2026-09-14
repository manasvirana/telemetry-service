package com.lane.telemetry.api;

public record IngestError(
        int index,
        String reason
) {}