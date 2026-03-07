# RisingWave Connectors, Integrations, and Ecosystem Research

This comprehensive research document covers RisingWave's connector ecosystem, including source connectors, sink connectors, integrations, and configuration best practices.

## Table of Contents

1. [Source Connectors](#source-connectors)
   - [Kafka](#kafka-source)
   - [PostgreSQL CDC](#postgresql-cdc)
   - [MySQL CDC](#mysql-cdc)
   - [S3/File Sources](#s3-file-sources)
   - [Kinesis](#kinesis-source)
   - [Pulsar](#pulsar-source)
   - [Google Pub/Sub](#google-pubsub-source)
   - [NATS JetStream](#nats-jetstream-source)
2. [Sink Connectors](#sink-connectors)
   - [Kafka](#kafka-sink)
   - [JDBC (PostgreSQL/MySQL)](#jdbc-sink)
   - [Redis](#redis-sink)
   - [Elasticsearch](#elasticsearch-sink)
   - [ClickHouse](#clickhouse-sink)
   - [S3/Iceberg](#s3-iceberg-sink)
   - [Delta Lake](#delta-lake-sink)
   - [BigQuery](#bigquery-sink)
   - [Snowflake](#snowflake-sink)
3. [Data Formats](#data-formats)
4. [Integrations](#integrations)
   - [dbt](#dbt-integration)
   - [Grafana & Prometheus](#grafana-prometheus)
   - [Client Libraries](#client-libraries)
5. [Security & Authentication](#security-authentication)
6. [Performance Tuning](#performance-tuning)
7. [Best Practices](#best-practices)

---

## Source Connectors

### Kafka Source

RisingWave provides robust Kafka source connectivity with support for multiple authentication methods and data formats.

#### Basic Configuration

```sql
CREATE SOURCE my_kafka_source (
    user_id INT,
    product_id VARCHAR,
    timestamp TIMESTAMP
)
WITH (
    connector='kafka',
    topic='user_activity',
    properties.bootstrap.server='broker1:9092,broker2:9092'
)
FORMAT PLAIN ENCODE JSON;
```

#### With Metadata Extraction

```sql
CREATE SOURCE kafka_with_metadata (
    user_id INT,
    product_id VARCHAR,
    timestamp TIMESTAMP
)
INCLUDE key AS kafka_key
INCLUDE partition AS kafka_partition
INCLUDE offset AS kafka_offset
INCLUDE timestamp AS kafka_timestamp
WITH (
    connector='kafka',
    topic='user_activity',
    properties.bootstrap.server='localhost:9092'
)
FORMAT PLAIN ENCODE JSON;
```

#### Reusable Connections (v2.2+)

```sql
-- Create a reusable connection
CREATE CONNECTION kafka_conn1 WITH (
    type = 'kafka',
    properties.bootstrap.server = 'localhost:9092'
);

-- Use the connection for sources
CREATE SOURCE kafka_source (
    id int,
    name varchar,
    email varchar,
    age int
)
WITH (
    connector = 'kafka',
    connection = 'kafka_conn1',
    topic = 'topic1',
    scan.startup.mode='latest'
)
FORMAT PLAIN ENCODE JSON;
```

#### With Avro and Schema Registry

```sql
CREATE SOURCE avro_source
WITH (
    connector='kafka',
    topic='demo_topic',
    properties.bootstrap.server='172.10.1.1:9090,172.10.1.2:9090',
    scan.startup.mode='latest',
    scan.startup.timestamp.millis='140000000'
)
FORMAT PLAIN ENCODE AVRO (
    message = 'message_name',
    schema.registry = 'http://127.0.0.1:8081',
    schema.registry.username='your_username',
    schema.registry.password='your_password'
);
```

#### Key Parameters

| Parameter | Description |
|-----------|-------------|
| `connector` | Set to `'kafka'` |
| `topic` | Kafka topic name |
| `properties.bootstrap.server` | Comma-separated list of brokers |
| `scan.startup.mode` | `'earliest'`, `'latest'`, or `'timestamp'` |
| `scan.startup.timestamp.millis` | Timestamp for startup (when mode is timestamp) |
| `properties.group.id` | Consumer group ID |

---

### PostgreSQL CDC

RisingWave supports PostgreSQL CDC using native connectors compatible with PostgreSQL versions 10-17.

#### Prerequisites

1. Set `wal_level` to `logical` in PostgreSQL:
```sql
ALTER SYSTEM SET wal_level = logical;
-- Requires restart
```

2. Grant required privileges:
```sql
CREATE USER risingwave_user REPLICATION LOGIN CREATEDB PASSWORD 'password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO risingwave_user;
```

#### Basic Configuration

```sql
CREATE TABLE pg_orders (
    o_orderkey BIGINT,
    o_custkey INTEGER,
    o_totalprice NUMERIC,
    o_orderdate TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (o_orderkey)
)
WITH (
    connector = 'postgres-cdc',
    hostname = '127.0.0.1',
    port = '5432',
    username = 'postgresuser',
    password = 'postgrespw',
    database.name = 'mydb',
    schema.name = 'public',
    table.name = 'orders'
);
```

#### Using CDC Source with Multiple Tables

```sql
-- Create the source first
CREATE SOURCE postgres_source WITH (
    connector = 'postgres-cdc',
    hostname = '127.0.0.1',
    port = '5432',
    username = 'postgresuser',
    password = 'postgrespw',
    database.name = 'mydb'
);

-- Create tables from the source
CREATE TABLE sales (
    sale_id INTEGER PRIMARY KEY,
    user_id INTEGER,
    product_id INTEGER,
    sale_date DATE,
    quantity INTEGER,
    total_price NUMERIC
) FROM postgres_source TABLE 'public.sales';

CREATE TABLE products (
    product_id INTEGER PRIMARY KEY,
    name VARCHAR,
    price NUMERIC
) FROM postgres_source TABLE 'public.products';
```

#### AWS RDS Configuration

For AWS RDS:
1. Create a parameter group (e.g., `pg-cdc`)
2. Set `rds.logical_replication = 1`
3. Apply the parameter group to your instance
4. Restart the instance

#### Key Parameters

| Parameter | Description |
|-----------|-------------|
| `connector` | Set to `'postgres-cdc'` |
| `hostname` | PostgreSQL host |
| `port` | PostgreSQL port (default: 5432) |
| `username` / `password` | Credentials |
| `database.name` | Database name |
| `schema.name` | Schema name |
| `table.name` | Table name |
| `slot.name` | Replication slot name (optional) |

---

### MySQL CDC

RisingWave supports MySQL CDC for versions 5.7, 8.0, 8.4, and compatible databases (MariaDB, TiDB).

#### Prerequisites

1. Enable binary logging in `my.cnf`:
```ini
server-id = 223344
log_bin = mysql-bin
binlog_format = ROW
binlog_row_image = FULL
expire_logs_days = 10
```

2. Create user with privileges:
```sql
CREATE USER 'risingwave'@'%' IDENTIFIED BY 'password';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'risingwave'@'%';
FLUSH PRIVILEGES;
```

#### Basic Configuration

```sql
-- Create the source
CREATE SOURCE mysql_mydb WITH (
    connector = 'mysql-cdc',
    hostname = '127.0.0.1',
    port = '3306',
    username = 'root',
    password = '123456',
    database.name = 'mydb',
    server.id = 5888
);

-- Create table from the source
CREATE TABLE t1_rw (
    v1 int,
    v2 int,
    PRIMARY KEY(v1)
) FROM mysql_mydb TABLE 'mydb.t1';
```

#### With Generated Columns

```sql
CREATE TABLE orders (
    order_id int,
    amount decimal,
    -- Generated column
    next_id int AS order_id + 1,
    PRIMARY KEY(order_id)
) FROM mysql_source TABLE 'mydb.orders';
```

#### Key Parameters

| Parameter | Description |
|-----------|-------------|
| `connector` | Set to `'mysql-cdc'` |
| `hostname` | MySQL host |
| `port` | MySQL port (default: 3306) |
| `username` / `password` | Credentials |
| `database.name` | Database name |
| `server.id` | Unique server ID for replication |

---

### S3 File Sources

RisingWave supports ingesting CSV, ndjson, and Parquet files from S3.

#### CSV Configuration

```sql
CREATE TABLE s3_csv_source (
    id int,
    name varchar,
    age int
)
WITH (
    connector = 's3',
    s3.region_name = 'ap-southeast-2',
    s3.bucket_name = 'example-s3-source',
    s3.credentials.access = 'your_access_key',
    s3.credentials.secret = 'your_secret_key'
)
FORMAT PLAIN ENCODE CSV (
    without_header = 'true',
    delimiter = ','
);
```

#### JSON (ndjson) Configuration

```sql
CREATE SOURCE s3_json_source (
    id int,
    name varchar,
    data jsonb
)
WITH (
    connector = 's3',
    s3.region_name = 'us-east-1',
    s3.bucket_name = 'my-bucket',
    s3.credentials.access = 'access_key',
    s3.credentials.secret = 'secret_key',
    match_pattern = '*.json'
)
FORMAT PLAIN ENCODE JSON;
```

#### Parquet with file_scan()

```sql
-- Read a single Parquet file
SELECT * FROM file_scan(
    's3://bucket/path/file.parquet',
    parquet,
    's3.region' = 'us-east-1',
    's3.access.key' = 'key',
    's3.secret.key' = 'secret'
);

-- Read directory of Parquet files
SELECT * FROM file_scan(
    's3://bucket/path/',
    parquet,
    's3.region' = 'us-east-1',
    's3.access.key' = 'key',
    's3.secret.key' = 'secret'
);
```

#### Important Notes

- **Avro is NOT supported** for S3 sources (only for message queues)
- RisingWave does not guarantee file read order
- New files are automatically ingested
- Deleted files are not detected
- Empty cells in CSV are parsed as NULL

---

### Kinesis Source

```sql
CREATE SOURCE kinesis_source (
    user_id INT,
    action VARCHAR,
    timestamp TIMESTAMP
)
WITH (
    connector='kinesis',
    stream='your_stream_name',
    aws.region='us-east-1',
    aws.credentials.access_key_id = 'your_access_key',
    aws.credentials.secret_access_key = 'your_secret_key'
)
FORMAT PLAIN ENCODE JSON;
```

#### With IAM Role

```sql
CREATE SOURCE kinesis_with_role
WITH (
    connector='kinesis',
    stream='my-stream',
    aws.region='us-east-1',
    aws.credentials.role.arn = 'arn:aws:iam::602389639824:role/demo_role',
    aws.credentials.role.external_id = 'external_id'
)
FORMAT PLAIN ENCODE JSON;
```

#### Key Parameters

| Parameter | Description |
|-----------|-------------|
| `connector` | Set to `'kinesis'` |
| `stream` | Kinesis stream name |
| `aws.region` | AWS region |
| `aws.credentials.access_key_id` | Access key |
| `aws.credentials.secret_access_key` | Secret key |
| `aws.credentials.session_token` | Session token (optional) |
| `aws.credentials.role.arn` | IAM role ARN |
| `endpoint` | Custom endpoint (optional) |

---

### Pulsar Source

```sql
CREATE SOURCE pulsar_source
WITH (
    connector='pulsar',
    topic='demo_topic',
    service.url='pulsar://localhost:6650/',
    scan.startup.mode='latest'
)
FORMAT PLAIN ENCODE JSON;
```

#### With OAuth Authentication

```sql
CREATE SOURCE pulsar_oauth_source
WITH (
    connector='pulsar',
    topic='demo_topic',
    service.url='pulsar://localhost:6650/',
    oauth.issuer.url='https://auth.streamnative.cloud/',
    oauth.credentials.url='s3://bucket_name/your_key_file.file',
    oauth.audience='urn:sn:pulsar:o-d6fgh:instance-0',
    aws.credentials.access_key_id='access_key',
    aws.credentials.secret_access_key='secret_key',
    scan.startup.mode='latest'
)
FORMAT PLAIN ENCODE AVRO (
    message = 'message',
    schema.location = 'https://bucket.s3-us-west-2.amazonaws.com/schema.avsc'
);
```

---

### Google Pub/Sub Source

```sql
CREATE SOURCE pubsub_source (
    message_id VARCHAR,
    data JSONB,
    attributes JSONB
)
WITH (
    connector = 'google_pubsub',
    pubsub.subscription = 'projects/my-project/subscriptions/my-subscription',
    pubsub.credentials = '{
        "type": "service_account",
        "project_id": "my-project",
        ...
    }'
)
FORMAT PLAIN ENCODE JSON;
```

**Note**: Google Pub/Sub provides at-least-once semantics (not exactly-once) due to SDK limitations.

---

### NATS JetStream Source

```sql
CREATE TABLE nats_source
WITH (
    connector = 'nats',
    server_url = 'nats-server:4222',
    subject = 'live_stream_metrics',
    stream = 'risingwave',
    connect_mode = 'plain'
)
FORMAT PLAIN ENCODE PROTOBUF (
    message = 'livestream.schema.LiveStreamMetrics',
    schema.location = 'http://file_server:8080/schema'
);
```

#### With Authentication

```sql
CREATE TABLE nats_auth_source
WITH (
    connector = 'nats',
    server_url = 'nats-server:4222',
    subject = 'events',
    stream = 'mystream',
    connect_mode = 'user_and_password',
    username = 'user',
    password = 'password',
    consumer.durable_name = 'risingwave_consumer',
    consumer.ack_policy = 'explicit'
)
FORMAT PLAIN ENCODE JSON;
```

---

## Sink Connectors

### Kafka Sink

#### Basic Configuration

```sql
CREATE SINK kafka_sink FROM my_materialized_view
WITH (
    connector='kafka',
    properties.bootstrap.server='localhost:9092',
    topic='output_topic'
)
FORMAT PLAIN ENCODE JSON;
```

#### UPSERT with Primary Key

```sql
CREATE SINK upsert_sink FROM my_table
WITH (
    properties.bootstrap.server = 'localhost:9092',
    topic = 'upsert_topic',
    connector = 'kafka',
    primary_key = 'user_id'
)
FORMAT UPSERT ENCODE JSON;
```

#### With SSL Encryption

```sql
CREATE SINK ssl_sink FROM mv1
WITH (
    connector='kafka',
    topic='secure-events',
    properties.bootstrap.server='localhost:9093',
    properties.security.protocol='SSL',
    properties.ssl.ca.location='/path/to/ca-cert',
    properties.ssl.certificate.location='/path/to/client.pem',
    properties.ssl.key.location='/path/to/client.key',
    properties.ssl.key.password='keypassword'
)
FORMAT PLAIN ENCODE JSON;
```

#### With SASL/PLAIN and SSL

```sql
CREATE SINK sasl_ssl_sink FROM mv1
WITH (
    connector='kafka',
    topic='secure-events',
    properties.bootstrap.server='localhost:9093',
    properties.sasl.mechanism='PLAIN',
    properties.security.protocol='SASL_SSL',
    properties.sasl.username='admin',
    properties.sasl.password='admin-secret',
    properties.ssl.ca.location='/path/to/ca-cert',
    properties.ssl.certificate.location='/path/to/client.pem',
    properties.ssl.key.location='/path/to/client.key',
    properties.ssl.key.password='keypassword'
)
FORMAT PLAIN ENCODE JSON;
```

#### With PrivateLink

```sql
CREATE SINK privatelink_sink FROM mv
WITH (
    connector='kafka',
    properties.bootstrap.server='b-1.xxx.amazonaws.com:9092,b-2.xxx.amazonaws.com:9092',
    topic='msk_topic',
    privatelink.endpoint='10.148.0.4',
    privatelink.targets = '[{"port": 8001}, {"port": 8002}]'
)
FORMAT PLAIN ENCODE JSON (
    force_append_only='true'
);
```

---

### JDBC Sink

#### PostgreSQL Sink

```sql
CREATE SINK postgres_sink FROM my_table
WITH (
    connector = 'jdbc',
    jdbc.url = 'jdbc:postgresql://postgres:5432/mydb',
    user = 'myuser',
    password = '123456',
    table.name = 'target_table',
    type = 'upsert',
    primary_key = 'id'
);
```

#### MySQL Sink

```sql
CREATE SINK mysql_sink FROM my_table
WITH (
    connector = 'jdbc',
    jdbc.url = 'jdbc:mysql://mysql:3306/mydb?ssl-mode=REQUIRED',
    user = 'myuser',
    password = 'mypassword',
    table.name = 'target_table',
    type = 'upsert',
    primary_key = 'id'
);
```

#### Native PostgreSQL Connector (v2.2+)

Set in configuration:
```yaml
[streaming.developer]
stream_switch_jdbc_pg_to_native = true
```

Then create the sink as usual. This uses a Rust-based native connector instead of JDBC.

#### Key Parameters

| Parameter | Description |
|-----------|-------------|
| `connector` | Set to `'jdbc'` |
| `jdbc.url` | JDBC connection URL |
| `user` / `password` | Database credentials |
| `table.name` | Target table name |
| `type` | `'upsert'` or `'append-only'` |
| `primary_key` | Required for upsert mode |

**Note**: Requires JDK 11+ for JDBC connector.

---

### Redis Sink

#### Key-Value Cache Pattern

```sql
CREATE SINK redis_cache_sink FROM user_profiles_mv
WITH (
    connector = 'redis',
    redis.url = 'redis://127.0.0.1:6379/',
    primary_key = 'user_id'
)
FORMAT PLAIN ENCODE JSON (
    force_append_only = 'true'
);
```

#### Geospatial Pattern

```sql
CREATE SINK geo_sink FROM driver_locations
WITH (
    connector = 'redis',
    redis.url = 'redis://127.0.0.1:6379/',
    primary_key = 'driver_id,city'
)
FORMAT UPSERT ENCODE TEMPLATE (
    redis_value_type = 'geospatial',
    key_format = 'drivers:{city}',
    member = 'driver_id',
    longitude = 'longitude',
    latitude = 'latitude'
);
```

---

### Elasticsearch Sink

```sql
CREATE SINK es_sink FROM my_view
WITH (
    connector = 'elasticsearch',
    primary_key = 'doc_id',
    index = 'my_index',
    url = 'http://elasticsearch:9200',
    username = 'elastic',
    password = 'password',
    delimiter = '_'
);
```

#### With Dynamic Index

```sql
CREATE SINK es_dynamic_sink FROM my_view
WITH (
    connector = 'elasticsearch',
    primary_key = 'doc_id',
    index_column = 'index_name_column',
    url = 'http://elasticsearch:9200',
    username = 'elastic',
    password = 'password'
);
```

**Notes**:
- Supports Elasticsearch 7.x and 8.x
- Defaults to upsert mode (append-only not supported)
- Provides at-least-once delivery semantics
- Requires JDK 11+
- **Premium feature** in self-hosted deployments

---

### ClickHouse Sink

```sql
CREATE SINK clickhouse_sink FROM my_table
WITH (
    connector = 'clickhouse',
    type = 'upsert',
    clickhouse.url = 'http://clickhouse:8123',
    clickhouse.user = 'default',
    clickhouse.password = 'password',
    clickhouse.database = 'default',
    clickhouse.table = 'target_table',
    primary_key = 'id'
);
```

**Best Practice**: Use deduplication engines like `ReplacingMergeTree` in ClickHouse to handle potential duplicate writes during RisingWave recovery.

---

### S3/Iceberg Sink

#### Iceberg with Storage Catalog

```sql
CREATE SINK iceberg_sink FROM my_table
WITH (
    connector = 'iceberg',
    type = 'upsert',
    primary_key = 'id',
    database.name = 'demo_db',
    table.name = 'target_table',
    catalog.name = 'demo',
    catalog.type = 'storage',
    warehouse.path = 's3a://my-bucket/iceberg',
    s3.endpoint = 'https://s3.amazonaws.com',
    s3.region = 'us-east-1',
    s3.access.key = 'access_key',
    s3.secret.key = 'secret_key',
    create_table_if_not_exists = 'true'
);
```

#### Iceberg with REST Catalog

```sql
CREATE SINK iceberg_rest_sink FROM my_table
WITH (
    connector = 'iceberg',
    type = 'append-only',
    force_append_only = 'true',
    database.name = 'demo_db',
    table.name = 'target_table',
    catalog.name = 'demo',
    catalog.type = 'rest',
    catalog.uri = 'http://iceberg-rest:8181'
);
```

#### Direct S3 Sink with Parquet

```sql
CREATE SINK s3_parquet_sink AS SELECT * FROM my_table
WITH (
    connector = 's3',
    s3.path = 'output/',
    s3.region_name = 'us-east-1',
    s3.bucket_name = 'my-bucket',
    s3.credentials.access = 'access_key',
    s3.credentials.secret = 'secret_key',
    type = 'append-only'
)
FORMAT PLAIN ENCODE PARQUET (
    force_append_only = 'true'
);
```

#### Amazon S3 Tables Integration

```sql
CREATE SINK s3_tables_sink FROM my_table
WITH (
    connector = 'iceberg',
    type = 'upsert',
    primary_key = 'id',
    database.name = 'my_namespace',
    table.name = 'my_table',
    catalog.name = 's3tables',
    catalog.type = 'rest',
    catalog.uri = 'https://s3tables.us-east-1.amazonaws.com/iceberg',
    -- SigV4 authentication
    s3.region = 'us-east-1',
    s3.access.key = 'access_key',
    s3.secret.key = 'secret_key'
);
```

---

### Delta Lake Sink

```sql
CREATE SINK delta_sink FROM my_table
WITH (
    connector = 'deltalake',
    type = 'append-only',
    force_append_only = 'true',
    location = 's3a://my-bucket/delta-table',
    s3.endpoint = 'https://s3.amazonaws.com',
    s3.access.key = 'access_key',
    s3.secret.key = 'secret_key'
);
```

---

### BigQuery Sink

```sql
CREATE SINK bigquery_sink FROM my_table
WITH (
    connector = 'bigquery',
    type = 'append-only',
    bigquery.local.path = '/path/to/service-account.json',
    bigquery.project = 'my-project',
    bigquery.dataset = 'my_dataset',
    bigquery.table = 'my_table',
    force_append_only = 'true'
);
```

---

### Snowflake Sink

```sql
CREATE SINK snowflake_sink FROM my_table
WITH (
    connector = 'snowflake',
    s3.bucket_name = 'staging-bucket',
    s3.credentials.access = 'access_key',
    s3.credentials.secret = 'secret_key',
    s3.region_name = 'us-east-1',
    s3.path = 'staging/'
);
```

**Note**: Uses Snowpipe for data loading. Data is staged in S3 in JSON format before loading.

**Premium feature** in self-hosted deployments.

---

## Data Formats

### FORMAT and ENCODE Options

| ENCODE | Compatible FORMATS | Description |
|--------|-------------------|-------------|
| `JSON` | PLAIN, UPSERT, DEBEZIUM | JSON serialization |
| `AVRO` | PLAIN, UPSERT, DEBEZIUM | Avro with schema registry |
| `PROTOBUF` | PLAIN | Protocol Buffers |
| `CSV` | PLAIN | Comma-separated values |
| `BYTES` | PLAIN | Raw bytes (single BYTEA field) |

### JSON Examples

```sql
-- Basic JSON
FORMAT PLAIN ENCODE JSON

-- With schema registry
FORMAT PLAIN ENCODE JSON (
    schema.registry = 'http://registry:8081'
)
```

### Avro Examples

```sql
-- With schema registry
FORMAT PLAIN ENCODE AVRO (
    schema.registry = 'http://registry:8081',
    schema.registry.username = 'username',
    schema.registry.password = 'password'
)

-- With S3 schema location
FORMAT PLAIN ENCODE AVRO (
    message = 'MyMessage',
    schema.location = 's3://bucket/schema.avsc'
)
```

### Protobuf Examples

```sql
FORMAT PLAIN ENCODE PROTOBUF (
    message = 'package.MessageName',
    schema.location = 'http://server/schema.proto'
)

-- With schema registry
FORMAT PLAIN ENCODE PROTOBUF (
    message = 'package.MessageName',
    schema.registry = 'http://registry:8081'
)
```

### CSV Examples

```sql
FORMAT PLAIN ENCODE CSV (
    without_header = 'true',
    delimiter = ','
)

-- Tab-delimited
FORMAT PLAIN ENCODE CSV (
    without_header = 'false',
    delimiter = '\t'
)
```

### Timestamp Handling

```sql
FORMAT PLAIN ENCODE JSON (
    timestamptz.handling.mode = 'micro'  -- or 'milli'
)
```

---

## Integrations

### dbt Integration

#### Installation

```bash
pip install dbt-core dbt-risingwave
```

#### Configuration (profiles.yml)

```yaml
risingwave_project:
  target: dev
  outputs:
    dev:
      type: risingwave
      host: localhost
      port: 4566
      user: root
      password: ""
      database: dev
      schema: public
      threads: 4
      streaming_parallelism: 4
      streaming_max_parallelism: 8
```

#### Materializations

**Materialized View:**
```sql
{{ config(materialized='materialized_view') }}
SELECT * FROM source_table
```

**Table:**
```sql
{{ config(materialized='table') }}
SELECT * FROM source_table
```

**Ephemeral (CTE):**
```sql
{{ config(materialized='ephemeral') }}
SELECT * FROM source_table
```

**Zero Downtime Rebuilds (v2.2+):**
```sql
{{ config(
    materialized='materialized_view',
    zero_downtime={'enabled': true}
) }}
SELECT * FROM source_table
```

Run with: `dbt run --vars 'zero_downtime: true'`

#### Commands

```bash
# Create new models
dbt run

# Drop and recreate models
dbt run --full-refresh

# Select specific models
dbt run --select "my_model+"  # Model and children
dbt run --select "+my_model"  # Model and parents
```

---

### Grafana & Prometheus

#### Kubernetes Setup

```bash
# Port-forward Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:http-web

# Access from external hosts
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:http-web --address 0.0.0.0
```

Default credentials:
- Username: `admin`
- Password: `prom-operator`

#### Demo Cluster Setup

```bash
# Clone repository
git clone https://github.com/risingwavelabs/risingwave.git

# Start demo with Prometheus and Grafana
cd risingwave/integration_tests/prometheus
docker compose up -d
```

#### Metric Relabeling

If namespace filters don't work, add the `risingwave_name` label:

```yaml
# In Prometheus Operator endpoint spec
metricRelabelings:
  - sourceLabels: [__name__]
    targetLabel: risingwave_name
    replacement: 'my-cluster'
```

#### Using RisingWave with Grafana

1. Create a data source connection to RisingWave (PostgreSQL compatible)
2. Build materialized views for metrics
3. Visualize in Grafana dashboards

---

### Client Libraries

#### Python (psycopg2)

```python
import psycopg2

conn = psycopg2.connect(
    host="localhost",
    port=4566,
    user="root",
    dbname="dev"
)
conn.autocommit = True

cursor = conn.cursor()
cursor.execute("SELECT * FROM my_table")
results = cursor.fetchall()
```

#### Python (risingwave-py SDK)

```bash
pip install risingwave-py psycopg2-binary
```

```python
from risingwave import RisingWave, OutputFormat

rw = RisingWave(
    host="localhost",
    port=4566,
    user="root",
    database="dev"
)

# Subscribe to changes
@rw.on_change("my_materialized_view")
def handler(event):
    print(f"Change: {event}")
```

#### Python (SQLAlchemy)

```bash
pip install SQLAlchemy sqlalchemy-risingwave psycopg2-binary
```

```python
from sqlalchemy import create_engine

engine = create_engine('risingwave+psycopg2://root@localhost:4566/dev')

with engine.connect() as conn:
    result = conn.execute("SELECT * FROM my_table")
    for row in result:
        print(row)
```

#### Java (JDBC)

```java
import java.sql.*;

String url = "jdbc:postgresql://localhost:4566/dev";
String user = "root";
String password = "";

Connection conn = DriverManager.getConnection(url, user, password);
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM my_table");

while (rs.next()) {
    System.out.println(rs.getString(1));
}
```

#### Other Languages

RisingWave is PostgreSQL wire-compatible, so any PostgreSQL client library works:
- **Go**: `pgx`, `database/sql` with `lib/pq`
- **Node.js**: `pg`, `postgres`
- **Ruby**: `pg` gem

---

## Security & Authentication

### Kafka SSL (Without SASL)

```sql
CREATE SOURCE ssl_source (
    column1 varchar,
    column2 integer
)
WITH (
    connector='kafka',
    topic='secure-topic',
    properties.bootstrap.server='localhost:9093',
    scan.startup.mode='earliest',
    properties.security.protocol='SSL',
    properties.ssl.ca.location='/path/to/ca-cert',
    properties.ssl.certificate.location='/path/to/client.pem',
    properties.ssl.key.location='/path/to/client.key',
    properties.ssl.key.password='keypassword'
)
FORMAT PLAIN ENCODE JSON;
```

### Kafka SASL/PLAIN (Without SSL)

```sql
CREATE SOURCE sasl_source (
    column1 varchar,
    column2 integer
)
WITH (
    connector='kafka',
    topic='secure-topic',
    properties.bootstrap.server='localhost:9093',
    scan.startup.mode='earliest',
    properties.sasl.mechanism='PLAIN',
    properties.security.protocol='SASL_PLAINTEXT',
    properties.sasl.username='admin',
    properties.sasl.password='admin-secret'
)
FORMAT PLAIN ENCODE JSON;
```

### Kafka SASL/PLAIN with SSL

```sql
CREATE SOURCE sasl_ssl_source (
    column1 varchar,
    column2 integer
)
WITH (
    connector='kafka',
    topic='secure-topic',
    properties.bootstrap.server='localhost:9093',
    properties.sasl.mechanism='PLAIN',
    properties.security.protocol='SASL_SSL',
    properties.sasl.username='admin',
    properties.sasl.password='admin-secret',
    properties.ssl.ca.location='/path/to/ca-cert',
    properties.ssl.certificate.location='/path/to/client.pem',
    properties.ssl.key.location='/path/to/client.key',
    properties.ssl.key.password='keypassword'
)
FORMAT PLAIN ENCODE JSON;
```

### SSL Troubleshooting

Bypass CA verification (for testing):
```sql
properties.ssl.endpoint.identification.algorithm='none'
```

### AWS Authentication

#### Access Keys
```sql
aws.credentials.access_key_id = 'AKIAIOSFODNN7EXAMPLE',
aws.credentials.secret_access_key = 'wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY'
```

#### Session Token (Temporary Credentials)
```sql
aws.credentials.session_token = 'FwoGZXIvYXdzE...'
```

#### IAM Role
```sql
aws.credentials.role.arn = 'arn:aws:iam::123456789012:role/MyRole',
aws.credentials.role.external_id = 'external-id-123'
```

---

## Performance Tuning

### Parallelism Configuration

#### Default Behavior

By default, RisingWave utilizes maximum available CPUs for streaming jobs.

#### For 300+ Streaming Jobs

Update configuration in `risingwave.yaml`:

```yaml
[meta]
disable_automatic_parallelism_control = true
default_parallelism = 8
```

#### Session Variables

```sql
-- Set streaming parallelism
SET streaming_parallelism = 4;

-- Set batch parallelism
SET batch_parallelism = 8;
```

#### Check Parallelism

```sql
SELECT * FROM rw_fragment_parallelism;
```

#### Alter Parallelism

```sql
ALTER MATERIALIZED VIEW my_mv SET PARALLELISM = 4;
```

### dbt with Parallelism

```sql
{{ config(
    materialized='materialized_view',
    streaming_parallelism=2,
    streaming_max_parallelism=8
) }}
```

### Sink Decoupling

Enable buffering between RisingWave and downstream systems:

```sql
SET sink_decouple = true;
```

Benefits:
- Protects RisingWave from downstream performance issues
- Maintains stability during downstream unavailability

### Dedicated Batch-Serving Cluster

For sub-second batch query latency:
1. Deploy separate compute nodes for batch queries
2. Isolates batch workloads from streaming
3. Improves availability of batch processing

### Common Performance Considerations

1. **Partitioning Skew**: Ensure even data distribution
2. **State Management**: Monitor state size for stateful operations
3. **Communication Overhead**: Minimize cross-node shuffling
4. **Memory Management**: Monitor for OOM errors
5. **Checkpoint Intervals**: Balance between latency and overhead

---

## Best Practices

### Source Configuration

1. **Use Tables for Primary Key Constraints**: RisingWave only enforces PK constraints on tables, not sources
   ```sql
   CREATE TABLE (not SOURCE) for PK enforcement
   ```

2. **Reusable Connections (v2.2+)**: Define connection once, use across multiple sources/sinks
   ```sql
   CREATE CONNECTION kafka_conn WITH (...);
   ```

3. **Schema Registry**: Always use for Avro/Protobuf to ensure schema consistency

4. **Startup Mode**: Use `scan.startup.mode='latest'` for new deployments to avoid reprocessing

### Sink Configuration

1. **Sink Decoupling**: Enable for production to protect against downstream issues

2. **Primary Keys**: Always specify for upsert sinks
   ```sql
   primary_key = 'id'
   ```

3. **Deduplication in Downstream**: Use deduplication engines (e.g., ClickHouse ReplacingMergeTree)

4. **Batch Size Tuning**: Configure appropriate batch sizes for throughput vs. latency tradeoff

### CDC Best Practices

1. **Replication Slots**: Monitor and manage PostgreSQL replication slots
2. **WAL Retention**: Configure appropriate retention for recovery
3. **Privilege Management**: Use minimal required privileges
4. **Network Latency**: Deploy RisingWave close to source databases

### Data Format Best Practices

1. **Avro/Protobuf**: Preferred for schema evolution support
2. **JSON**: Use for flexibility but monitor for schema drift
3. **CSV**: Only for simple, flat data structures

### Monitoring Best Practices

1. **Set up Grafana dashboards**: Use provided templates
2. **Monitor key metrics**:
   - Streaming lag
   - Memory usage
   - Checkpoint latency
   - Sink throughput
3. **Alert on anomalies**: Configure Prometheus alerts

### Security Best Practices

1. **SSL/TLS**: Always enable for production Kafka connections
2. **Credential Management**: Use secrets management, not hardcoded values
3. **Network Isolation**: Use PrivateLink for cloud deployments
4. **Minimal Privileges**: Grant only required permissions

---

## Resources

### Official Documentation

- **Main Docs**: https://docs.risingwave.com
- **Kafka Source**: https://docs.risingwave.com/ingestion/sources/kafka
- **PostgreSQL CDC**: https://docs.risingwave.com/ingestion/sources/postgresql/pg-cdc
- **MySQL CDC**: https://docs.risingwave.com/ingestion/sources/mysql/mysql-cdc
- **S3 Source**: https://docs.risingwave.com/integrations/sources/s3
- **Kinesis Source**: https://docs.risingwave.com/integrations/sources/kinesis
- **Pulsar Source**: https://docs.risingwave.com/integrations/sources/pulsar
- **Data Delivery Overview**: https://docs.risingwave.com/docs/current/data-delivery/
- **dbt Setup**: https://docs.getdbt.com/docs/core/connect-data-platform/risingwave-setup
- **Python SDK**: https://docs.risingwave.com/python-sdk/intro

### GitHub Repositories

- **RisingWave**: https://github.com/risingwavelabs/risingwave
- **dbt-risingwave**: https://github.com/risingwavelabs/dbt-risingwave
- **sqlalchemy-risingwave**: https://github.com/risingwavelabs/sqlalchemy-risingwave
- **risingwave-py**: https://github.com/risingwavelabs/risingwave-py

### Tools

- **SQL Generator**: https://sql.risingwave.com - Interactive tool for generating connector SQL

### Version History

- **v2.2**: Reusable connections, zero-downtime dbt rebuilds, native PostgreSQL sink
- **v2.1**: Enhanced Iceberg connector
- **v2.0**: Python SDK improvements
- **v1.9**: Additional sink connectors
- **v1.7**: Adaptive parallelism

---

## Conclusion

RisingWave provides a comprehensive connector ecosystem that enables seamless integration with modern data infrastructure. Key strengths include:

1. **PostgreSQL Wire Compatibility**: Use existing tools and drivers
2. **Native CDC Support**: Direct database change capture without Debezium
3. **Multiple Data Formats**: JSON, Avro, Protobuf, CSV, Parquet
4. **Cloud-Native**: AWS, GCP, and Azure integrations
5. **Open Table Formats**: Iceberg and Delta Lake support
6. **dbt Integration**: Transform streaming data with familiar SQL

For production deployments, focus on:
- Proper security configuration (SSL/SASL)
- Performance tuning (parallelism, sink decoupling)
- Monitoring (Grafana/Prometheus)
- High availability configuration

The connector ecosystem continues to expand with each release, so check the official documentation for the latest supported integrations and features.
