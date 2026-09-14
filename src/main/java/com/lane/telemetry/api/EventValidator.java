package com.lane.telemetry.api;

public class EventValidator {

    public static String validate(TelemetryEvent event) {
        if (event == null) {
            return "event is missing";
        }
        if (event.timestamp() == null
                || event.lat() == null
                || event.lon() == null
                || event.speedKmph() == null
                || event.accelX() == null
                || event.accelY() == null
                || event.accelZ() == null
                || event.gyroX() == null
                || event.gyroY() == null
                || event.gyroZ() == null) {
            return "required field missing";
        }
        if (event.timestamp() <= 0) {
            return "timestamp must be a positive integer";
        }
        long maxFuture = System.currentTimeMillis() + 24L * 60 * 60 * 1000;
        if (event.timestamp() > maxFuture) {
            return "timestamp more than 24 hours in the future";
        }
        if (event.lat() < -90 || event.lat() > 90) {
            return "lat out of range";
        }
        if (event.lon() < -180 || event.lon() > 180) {
            return "lon out of range";
        }
        if (event.speedKmph() < 0 || event.speedKmph() > 300) {
            return "speed_kmph out of range";
        }
        return null;
    }
}