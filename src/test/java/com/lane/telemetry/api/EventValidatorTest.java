package com.lane.telemetry.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventValidatorTest {

    private TelemetryEvent ok() {
        return new TelemetryEvent(
                1730894521123L,
                12.9716, 77.5946, 42.5,
                0.12, -0.03, 9.81,
                0.002, -0.001, 0.0
        );
    }

    @Test
    void validEventPasses() {
        assertNull(EventValidator.validate(ok()));
    }

    @Test
    void speedOutOfRangeIsRejected() {
        TelemetryEvent event = new TelemetryEvent(
                1730894521123L,
                12.9716, 77.5946, 400.0,
                0.12, -0.03, 9.81,
                0.002, -0.001, 0.0
        );
        assertEquals("speed_kmph out of range", EventValidator.validate(event));
    }

    @Test
    void missingFieldIsRejected() {
        TelemetryEvent event = new TelemetryEvent(
                1730894521123L,
                12.9716, 77.5946, null,
                0.12, -0.03, 9.81,
                0.002, -0.001, 0.0
        );
        assertEquals("required field missing", EventValidator.validate(event));
    }
}