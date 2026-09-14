# telemetry-service

Spring WebFlux + R2DBC + Postgres. Ingest phone telemetry in batches, query a time-range summary.

No auth. Single node. Scale write-up: `SYSTEM_DESIGN.md`.

## Setup

Java 20, Docker.

```powershell
docker run --name telemetry-pg -e POSTGRES_USER=telemetry -e POSTGRES_PASSWORD=telemetry -e POSTGRES_DB=telemetry -p 5433:5432 -d postgres:16

$env:JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Kolkata"
.\gradlew.bat bootRun
```

App: `http://localhost:8080`. Flyway creates the table on start.

```powershell
$env:JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Kolkata"
.\gradlew.bat test
```

Port 5433 because 5432 was already in use. `JAVA_TOOL_OPTIONS` is needed on Windows. JDBC otherwise sends timezone `Asia/Calcutta`, which Postgres 16 rejects.

## Endpoints

`POST /api/v1/telemetry/ingest`

`GET /api/v1/telemetry/summary?device_id=&from=&to=`

`from` / `to` are epoch millis, inclusive.

## Screenshots

### Ingest : valid batch

<img width="1020" height="583" alt="image" src="https://github.com/user-attachments/assets/6b45f0ac-7979-4c2c-828d-59c94f0b10d7" />


### Ingest : retry / duplicates

<img width="1012" height="597" alt="image" src="https://github.com/user-attachments/assets/ece4176b-49e6-4cac-86bb-928cd9ffd791" />


### Ingest : mixed valid / invalid

<img width="1026" height="787" alt="image" src="https://github.com/user-attachments/assets/876c4a97-c3a4-47fb-8198-ea7b4a7613a6" />


### Ingest : 400 missing device_id

<img width="1012" height="616" alt="image" src="https://github.com/user-attachments/assets/45c184ba-0ff7-4538-a203-890b0a351236" />


### Ingest : 400 empty events

<img width="1008" height="420" alt="image" src="https://github.com/user-attachments/assets/d5f1607f-c832-4ebe-a6dd-9422965a178e" />


### Ingest : 429 rate limit

Code default is 50 ingest requests / second. For this screenshot I set it to 5 so I could hit 429 without a load generator, then set it back to 50.
<img width="648" height="119" alt="image" src="https://github.com/user-attachments/assets/c934f4f0-d206-4c2e-8f09-5b8919682757" />



### Summary : with data

<img width="1014" height="609" alt="image" src="https://github.com/user-attachments/assets/1b316c40-59fb-480f-9d0b-a1094f933970" />


### Summary : empty range

<img width="1010" height="581" alt="image" src="https://github.com/user-attachments/assets/d05257aa-f494-4e2d-84ce-82f1157b27bb" />


### Summary : 400 from > to

<img width="1014" height="488" alt="image" src="https://github.com/user-attachments/assets/1b167018-64f8-4749-9b55-49af685cbef5" />


## Design Notes

**Schema.** One table, `telemetry_events`. Columns match the payload: `device_id`, `event_time` (epoch millis, didn't call it `timestamp` since that's a Postgres type), lat, lon, speed, accel x/y/z, gyro x/y/z. Primary key is `(device_id, event_time)`. That's the only index. It enforces one row per device per timestamp, and it's the same index used by `WHERE device_id = ? AND event_time BETWEEN ? AND ?`. Adding a second index on the same columns would just slow down writes for no real benefit.

**Duplicates.** Dedup happens at the PK level, not in application code. The insert is `ON CONFLICT (device_id, event_time) DO NOTHING`. Whether it's the same batch, a client retry after a dropped response, or two concurrent requests for the same device, Postgres takes the row lock, one insert lands, and the rest return 0 rows affected. That's how `accepted` vs `duplicates` gets counted. Invalid events never reach the table at all, so those show up as `rejected`, not `duplicates`. Inserts run through `concatMap` (one at a time, in order) so the counts stay accurate instead of racing each other.

**Production.** This repo writes straight from HTTP to Postgres, on purpose, since it's a small demo. At 5k batches/sec I wouldn't do that. I'd put API Gateway in front, have the ingest layer only validate and publish to Kafka (MSK, keyed by `device_id`), and let consumers write raw data to S3 and queryable data to Timescale. The summary endpoint would read from Timescale only, never from Kafka. Full reasoning is in `SYSTEM_DESIGN.md`.

## Docker

```bash
docker build -t telemetry-service .
```

Needs Postgres. I used `bootRun` locally.

