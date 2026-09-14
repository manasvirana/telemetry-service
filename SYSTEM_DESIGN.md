# Part 3 : System design

In the interview I said: requests hit API Gateway, Kafka holds the work, consumers process it, we store data on AWS (including blob storage). This is that same plan, with the numbers from the brief.

The code I submitted is one app and Postgres. That is fine for the homework. It would not be enough for 10,000 phones.

## Numbers

10,000 devices, each sending a batch about every 2 seconds:

`10,000 / 2 = 5,000 requests per second`

Each phone samples 10–50 times a second. I used 30 as a middle value:

`10,000 × 30 = 300,000 events per second`

That is about 60 events in each HTTP batch (inside the 1–500 limit).

I treated one stored event as ~120 bytes (ids, time, GPS, accel, gyro — not the JSON text):

`300,000 × 120 × 86,400 ≈ 3.1 TB per day`

That is a lot. You cannot keep all of it in one Postgres forever.

## How a request would flow

1. Phone POSTs a batch to **API Gateway** (the public URL / HTTPS front door).
2. Gateway sends it to my ingest app running on **ECS** (AWS running my container).
3. Ingest does the same checks as this repo. Valid events go to **Kafka** (on AWS this is **MSK** — managed Kafka). I do not wait for the database on that HTTP call.
4. Kafka splits data by `device_id`, so one phone’s events stay in order on one partition.
5. **Consumers** (more ECS apps) read from Kafka.
   - One writes the raw batch to **S3** (blob / files, cheap to keep).
   - One writes rows into a query database so summary is a normal SQL query.
6. The summary API is a separate small app. It only reads the query database. It does not read Kafka on each GET.

I would not run `/summary` off S3. That would mean opening a bunch of files every time someone asks for avg speed.

## What I would pick, and what I would not

**API Gateway** — one URL, HTTPS, can throttle. I would not put the database behind Gateway. Gateway is just the door.

**Kafka (MSK)** — buffer. If ingest is slow or a consumer dies, messages wait here and we can replay. I would not use **SQS** (a simple queue): ordering per phone is weak, and you cannot rewind easily. **Kinesis** can also stream, but I already know Kafka.

**ECS** — run ingest, consumers, and the query API as containers. **Lambda** (run a function per request) is a poor fit for a steady 5,000 requests/sec.

**Query store: TimescaleDB on RDS** (Postgres with time-series support). Same unique `(device_id, event_time)` as the homework. I can keep `AVG`, `MAX(sqrt(...))`, and `ON CONFLICT DO NOTHING`. **Timestream** is AWS’s time-series DB but the queries would change and upserts are awkward. **DynamoDB** is good at “get one item by key”, not “average this device over a time range”.

**S3** — dump of raw batches, folder-style prefix by date and device. If I need to re-read a day I can use **Athena** (SQL over S3 files).

**If I do not split the data:** Kafka with one partition = one consumer, it falls behind. One giant Postgres table/index = range queries get slower every day. S3 with no date/device folders = you search the whole bucket.

## What happens in the three cases they asked

**An event arrives 45 minutes late, after we already returned a summary.**  
It still goes Gateway → Kafka → consumer. We save it with the phone’s timestamp, not “now”. The next summary for that window includes it. We do not change the old HTTP response. I would not cache summaries at the start.

**The same batch is sent 3 times.**  
Ingest publishes 3 times. Kafka will have copies. The database unique key ignores extras. S3 might keep 3 files, which is fine — they are cheap. The phone can still get HTTP 200 on a retry.

**Traffic jumps 10x in one minute.**  
About 50k requests/sec. Gateway can throttle until we raise the limit. Kafka holds the extra. Consumers lag, we start more of them. ECS adds containers. Postgres/Timescale is what struggles if we try to write everything hot. Then we write S3 first or we sample. Ingest stays up because it only checks the batch and writes to Kafka.

## Short version

Homework: one process, Postgres, unique key.  
Production: Gateway in front, Kafka in the middle, consumers in the back, S3 for raw files, Timescale for queries, split by `device_id`. Ingest only validates and publishes. Summary is still a SQL query.
