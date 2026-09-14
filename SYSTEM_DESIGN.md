# Part 3 — System design

In the interview I talked about API Gateway, Kafka, consumers, and storing data on AWS (including blob). This write-up is that same idea, filled in with the numbers from the brief.

The take-home is one box and Postgres. Below is what I would actually put in AWS if this was real traffic.

## Numbers

10,000 devices, each sending a batch about every 2 seconds:

`10,000 / 2 = 5,000 requests/sec`

The phones sample at 10–50 Hz. I picked 30 as a boring middle:

`10,000 × 30 = 300,000 events/sec`

So a batch is ~60 events. That sits inside the 1–500 limit.

I assumed ~120 bytes per stored row (id, millis, eight doubles, some Postgres overhead). Not the JSON size.

`300,000 × 120 × 86,400 ≈ 3.1 TB/day`

That is before compression. That is also why I would not keep every row hot forever.

## Flow

Phone hits API Gateway. Gateway forwards to an ingest service on ECS. That service checks the batch the same way this repo does, then puts valid events on Kafka. It does not wait for a DB write.

Kafka topic is `telemetry-events`. Message key is `device_id` so one phone stays on one partition.

Consumers do the slow work. One writer puts the raw batch on S3. Another writer puts rows into a query DB so summary is a normal SQL range query.

Summary is a different ECS service. It only reads the query DB. It does not consume Kafka on the request path.

I would not query S3 for avg speed. You would end up opening a bunch of files every time.

## Choices

**Front door:** API Gateway HTTP API → ECS. I want one URL, TLS, and throttling in one place. I would not put SQS here, Kafka is already the buffer. Gateway should stay the front door, not the place that talks to the DB.

**Buffer:** Amazon MSK. I have used Kafka, I know consumer groups and replay. Key = `device_id`. Kinesis can do this job but I would rather not relearn the APIs. SQS is a queue. Ordering is weak and you cannot rewind, which is a bad mix at 300k events/s.

**Compute:** ECS Fargate for ingest, consumers, and query. This load is always on. Lambda at 5k req/s is a lot of invocations for no real gain.

**Query DB:** TimescaleDB on RDS (Aurora is fine too). Same unique `(device_id, event_time)` idea as the take-home. I already have `AVG` and `MAX(sqrt(...))` in SQL, and `ON CONFLICT DO NOTHING` still works. Timestream is more "AWS native" but upserts are annoying and I would have to rewrite the queries. Dynamo is good at point reads, not this aggregation.

**Archive:** S3, prefix by date and device. If I need to re-run a day I can use Athena. At ~3 TB/day, keeping everything in RDS fills the disk and vacuum never keeps up.

**No partition:** Kafka with one partition means one consumer and lag immediately. One giant Timescale/Postgres index means range scans get worse every day. S3 with no date/device prefix means you list the whole bucket.

## Failures

**Late event (45 min), summary already returned.** It still goes through Kafka and gets stored with the event time, not "now". Next summary for that window will include it. I do not go back and change the old response. I would not cache summaries at the start.

**Batch retried 3 times.** Ingest publishes 3 times. Kafka will have copies. The DB unique key drops the extras. S3 might keep 3 objects, which is fine — they are cheap. The phone can get a 200 on a retry.

**10x traffic in a minute.** ~50k req/s. Gateway can throttle until we raise the limit. Kafka holds the spike. Consumers fall behind, we add more. ECS scales. RDS is what struggles if we try to write all of it hot. Then we write S3 first or we sample. Ingest stays up because it only validates and produces.

## Conclusion

This homework: one process, Postgres, unique key. Real version: Gateway, Kafka, consumers, S3 for raw files, Timescale for queries, partition on `device_id`. Ingest only validates and publishes. Summary stays a SQL query.
