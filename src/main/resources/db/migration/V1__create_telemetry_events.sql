CREATE TABLE telemetry_events (
    device_id    VARCHAR(64)     NOT NULL,
    event_time   BIGINT          NOT NULL,
    lat          DOUBLE PRECISION NOT NULL,
    lon          DOUBLE PRECISION NOT NULL,
    speed_kmph   DOUBLE PRECISION NOT NULL,
    accel_x      DOUBLE PRECISION NOT NULL,
    accel_y      DOUBLE PRECISION NOT NULL,
    accel_z      DOUBLE PRECISION NOT NULL,
    gyro_x       DOUBLE PRECISION NOT NULL,
    gyro_y       DOUBLE PRECISION NOT NULL,
    gyro_z       DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (device_id, event_time)
);