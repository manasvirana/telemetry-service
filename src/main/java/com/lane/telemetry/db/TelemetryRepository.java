package com.lane.telemetry.db;

import com.lane.telemetry.api.TelemetryEvent;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import com.lane.telemetry.api.SummaryResponse;

@Repository
public class TelemetryRepository {

    private final DatabaseClient db;

    public TelemetryRepository(DatabaseClient db) {
        this.db = db;
    }

    public Mono<Long> insertIgnoreDuplicate(String deviceId, TelemetryEvent event) {
        String sql = """
                INSERT INTO telemetry_events (
                    device_id, event_time, lat, lon, speed_kmph,
                    accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z
                ) VALUES (
                    :deviceId, :eventTime, :lat, :lon, :speed,
                    :ax, :ay, :az, :gx, :gy, :gz
                )
                ON CONFLICT (device_id, event_time) DO NOTHING
                """;

        return db.sql(sql)
                .bind("deviceId", deviceId)
                .bind("eventTime", event.timestamp())
                .bind("lat", event.lat())
                .bind("lon", event.lon())
                .bind("speed", event.speedKmph())
                .bind("ax", event.accelX())
                .bind("ay", event.accelY())
                .bind("az", event.accelZ())
                .bind("gx", event.gyroX())
                .bind("gy", event.gyroY())
                .bind("gz", event.gyroZ())
                .fetch()
                .rowsUpdated();
    }

    public Mono<SummaryResponse> summary(String deviceId, long from, long to) {
        String sql = """
                SELECT
                    AVG(speed_kmph) AS avg_speed,
                    MAX(SQRT(
                        accel_x * accel_x + accel_y * accel_y + accel_z * accel_z
                    )) AS max_accel,
                    COUNT(*) AS event_count
                FROM telemetry_events
                WHERE device_id = :deviceId
                  AND event_time >= :fromTime
                  AND event_time <= :toTime
                """;
    
        return db.sql(sql)
                .bind("deviceId", deviceId)
                .bind("fromTime", from)
                .bind("toTime", to)
                .map((row, metadata) -> new SummaryResponse(
                        deviceId,
                        from,
                        to,
                        row.get("avg_speed", Double.class),
                        row.get("max_accel", Double.class),
                        row.get("event_count", Long.class)
                ))
                .one();
    }
}