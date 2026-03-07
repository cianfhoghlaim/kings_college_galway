# RisingWave SQL Patterns and Streaming Concepts Reference

A comprehensive guide to RisingWave SQL syntax, streaming patterns, and best practices for real-time data processing.

## Table of Contents

1. [SQL Syntax and Extensions](#sql-syntax-and-extensions)
2. [Streaming Patterns](#streaming-patterns)
3. [Data Types and Functions](#data-types-and-functions)
4. [Common Use Cases](#common-use-cases)

---

## SQL Syntax and Extensions

### CREATE SOURCE

Sources establish connections to external data without storing data in RisingWave. They act as data entry points.

#### Basic Syntax

```sql
CREATE SOURCE [IF NOT EXISTS] source_name (
    column_name data_type [AS source_column_name] [NOT NULL],
    ...
    [, PRIMARY KEY (column_name, ...)]
)
WITH (
    connector = 'connector_name',
    connector_property = 'value',
    ...
)
FORMAT format_type ENCODE encode_type;
```

**Note:** PRIMARY KEY in sources is optional and indicates semantic meaning only, not an enforced constraint.

#### Kafka Source Examples

**Basic Kafka Source with JSON:**

```sql
CREATE SOURCE website_visits_stream (
    timestamp TIMESTAMP,
    user_id VARCHAR,
    page_id VARCHAR,
    action VARCHAR
)
WITH (
    connector = 'kafka',
    topic = 'user_activity',
    properties.bootstrap.server = 'broker1:9092,broker2:9092',
    scan.startup.mode = 'earliest'
)
FORMAT PLAIN ENCODE JSON;
```

**Kafka Source with Avro and Schema Registry:**

```sql
CREATE SOURCE avro_source (
    *,
    gen_i32_field INT AS int32_field + 2
)
INCLUDE KEY AS some_key
WITH (
    connector = 'kafka',
    topic = 'avro-topic',
    properties.bootstrap.server = 'message_queue:29092'
)
FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);
```

**Kafka Source with Watermark for Event Time Processing:**

```sql
CREATE SOURCE events (
    event_id VARCHAR,
    user_id VARCHAR,
    event_type VARCHAR,
    event_time TIMESTAMP,
    payload JSONB,
    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
)
WITH (
    connector = 'kafka',
    topic = 'events',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT PLAIN ENCODE JSON;
```

#### PostgreSQL CDC Source

CDC sources must use CREATE TABLE (not CREATE SOURCE) and require a PRIMARY KEY.

```sql
CREATE TABLE orders (
    order_id INTEGER,
    customer_id INTEGER,
    product_id VARCHAR,
    quantity INTEGER,
    total_price DECIMAL,
    order_time TIMESTAMP,
    status VARCHAR,
    PRIMARY KEY (order_id)
)
WITH (
    connector = 'postgres-cdc',
    hostname = '127.0.0.1',
    port = '5432',
    username = 'postgres',
    password = 'postgres',
    database.name = 'ecommerce',
    schema.name = 'public',
    table.name = 'orders'
);
```

#### MySQL CDC Source

```sql
CREATE TABLE users (
    user_id INTEGER,
    username VARCHAR,
    email VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (user_id)
)
WITH (
    connector = 'mysql-cdc',
    hostname = '127.0.0.1',
    port = '3306',
    username = 'root',
    password = 'password',
    database.name = 'app_db',
    table.name = 'users'
);
```

### CREATE TABLE vs CREATE SOURCE

| Aspect | Source | Table |
|--------|--------|-------|
| Data Storage | No storage, entry point only | Stores data internally |
| Primary Key | Optional, semantic only | Enforced constraint |
| CDC Support | Must use Table | Required for CDC |
| Consistency | Jobs may see inconsistent results | Guaranteed consistent view |
| Fault Tolerance | Cannot resume from checkpoint | Resumes from last checkpoint |

### CREATE MATERIALIZED VIEW

Materialized views store query results and update automatically as source data changes.

#### Basic Syntax

```sql
CREATE MATERIALIZED VIEW view_name AS
SELECT ...
FROM source_or_table
[WHERE ...]
[GROUP BY ...]
[EMIT ON WINDOW CLOSE];  -- Optional: for window-based processing
```

#### Aggregation Examples

**Simple Aggregation:**

```sql
CREATE MATERIALIZED VIEW customer_sales AS
SELECT
    customer_id,
    COUNT(*) AS order_count,
    SUM(total_price) AS total_sales,
    AVG(total_price) AS avg_order_value,
    MAX(order_time) AS last_order
FROM orders
GROUP BY customer_id;
```

**Multi-level Aggregation:**

```sql
CREATE MATERIALIZED VIEW daily_product_stats AS
SELECT
    DATE_TRUNC('day', order_time) AS order_date,
    product_id,
    COUNT(*) AS units_sold,
    SUM(quantity) AS total_quantity,
    SUM(total_price) AS revenue
FROM orders
GROUP BY DATE_TRUNC('day', order_time), product_id;
```

### CREATE SINK

Sinks output processed data to external systems.

#### Basic Syntax

```sql
CREATE SINK [IF NOT EXISTS] sink_name
[FROM table_or_mv | AS select_query]
WITH (
    connector = 'connector_name',
    connector_parameter = 'value',
    ...
)
FORMAT format_type ENCODE encode_type [(encode_options)];
```

#### Kafka Sink

```sql
CREATE SINK orders_sink
FROM processed_orders
WITH (
    connector = 'kafka',
    topic = 'processed-orders',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT UPSERT ENCODE JSON;
```

**With Avro Encoding:**

```sql
CREATE SINK avro_sink
FROM analytics_results
WITH (
    connector = 'kafka',
    topic = 'analytics-output',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);
```

#### BigQuery Sink

```sql
CREATE SINK bigquery_sink
FROM daily_metrics
WITH (
    connector = 'bigquery',
    bigquery.local.path = '/path/to/credentials.json',
    bigquery.project = 'my-project',
    bigquery.dataset = 'analytics',
    bigquery.table = 'daily_metrics'
);
```

#### Snowflake Sink

```sql
CREATE SINK snowflake_sink
FROM processed_data
WITH (
    connector = 'snowflake',
    s3.bucket_name = 'staging-bucket',
    s3.credentials.access = 'ACCESS_KEY',
    s3.credentials.secret = 'SECRET_KEY',
    s3.region_name = 'us-east-1',
    s3.path = 'risingwave/staging'
);
```

### CREATE INDEX

Indexes speed up batch queries on non-primary columns.

```sql
-- Basic index
CREATE INDEX idx_orders_customer
ON orders (customer_id);

-- Index with included columns
CREATE INDEX idx_orders_time_customer
ON orders (order_time)
INCLUDE (customer_id, total_price);

-- Index on expression
CREATE INDEX idx_orders_date
ON orders (DATE_TRUNC('day', order_time));
```

---

## Streaming Patterns

### Time Window Functions

#### Tumbling Windows (Non-overlapping)

Fixed-size, contiguous, non-overlapping time intervals.

```sql
-- Count events per 5-minute window
CREATE MATERIALIZED VIEW events_per_window AS
SELECT
    window_start,
    window_end,
    COUNT(*) AS event_count,
    COUNT(DISTINCT user_id) AS unique_users
FROM TUMBLE(events, event_time, INTERVAL '5' MINUTE)
GROUP BY window_start, window_end;
```

**With Multiple Aggregations:**

```sql
CREATE MATERIALIZED VIEW traffic_metrics AS
SELECT
    window_start,
    window_end,
    page_id,
    COUNT(*) AS page_views,
    COUNT(DISTINCT user_id) AS unique_visitors,
    AVG(load_time_ms) AS avg_load_time
FROM TUMBLE(page_events, event_time, INTERVAL '10' MINUTE)
GROUP BY window_start, window_end, page_id;
```

#### Hopping (Sliding) Windows

Fixed-size windows that can overlap.

```sql
-- 2-minute window, sliding every 30 seconds
CREATE MATERIALIZED VIEW rolling_metrics AS
SELECT
    window_start,
    window_end,
    COUNT(*) AS event_count,
    SUM(amount) AS total_amount
FROM HOP(
    transactions,
    transaction_time,
    INTERVAL '30' SECOND,  -- Slide interval
    INTERVAL '2' MINUTE    -- Window size
)
GROUP BY window_start, window_end;
```

**Rolling Average Example:**

```sql
CREATE MATERIALIZED VIEW rolling_avg_price AS
SELECT
    window_start,
    window_end,
    symbol,
    AVG(price) AS avg_price,
    MIN(price) AS min_price,
    MAX(price) AS max_price
FROM HOP(
    stock_ticks,
    tick_time,
    INTERVAL '1' MINUTE,   -- Update every minute
    INTERVAL '5' MINUTE    -- 5-minute rolling window
)
GROUP BY window_start, window_end, symbol;
```

#### Session Windows

Session windows are supported in batch mode and emit-on-window-close streaming mode.

```sql
-- Session windows with 30-minute gap
CREATE MATERIALIZED VIEW user_sessions AS
SELECT
    user_id,
    window_start AS session_start,
    window_end AS session_end,
    COUNT(*) AS events_in_session
FROM SESSION(
    user_events,
    event_time,
    INTERVAL '30' MINUTE
)
GROUP BY user_id, window_start, window_end
EMIT ON WINDOW CLOSE;
```

### Emit on Window Close

Generates final results only when windows close, improving performance for append-only sinks.

```sql
-- Define source with watermark
CREATE SOURCE sensor_readings (
    sensor_id VARCHAR,
    temperature DOUBLE,
    reading_time TIMESTAMP,
    WATERMARK FOR reading_time AS reading_time - INTERVAL '10' SECOND
)
WITH (
    connector = 'kafka',
    topic = 'sensors',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT PLAIN ENCODE JSON;

-- Materialized view with emit on window close
CREATE MATERIALIZED VIEW avg_temperature AS
SELECT
    window_start,
    window_end,
    sensor_id,
    AVG(temperature) AS avg_temp,
    MAX(temperature) AS max_temp,
    MIN(temperature) AS min_temp
FROM TUMBLE(sensor_readings, reading_time, INTERVAL '1' MINUTE)
GROUP BY window_start, window_end, sensor_id
EMIT ON WINDOW CLOSE;
```

### Temporal Joins

Join streaming data with dimension tables using point-in-time lookups.

#### Append-only Temporal Join

```sql
-- Fact table (streaming)
CREATE SOURCE order_events (
    order_id VARCHAR,
    product_id VARCHAR,
    quantity INTEGER,
    order_time TIMESTAMP,
    WATERMARK FOR order_time AS order_time - INTERVAL '5' SECOND
)
WITH (
    connector = 'kafka',
    topic = 'orders',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT PLAIN ENCODE JSON;

-- Dimension table
CREATE TABLE products (
    product_id VARCHAR PRIMARY KEY,
    product_name VARCHAR,
    category VARCHAR,
    price DECIMAL
);

-- Temporal join
CREATE MATERIALIZED VIEW enriched_orders AS
SELECT
    o.order_id,
    o.product_id,
    p.product_name,
    p.category,
    o.quantity,
    p.price * o.quantity AS total_amount,
    o.order_time
FROM order_events o
LEFT JOIN products p
    FOR SYSTEM_TIME AS OF PROCTIME()
    ON o.product_id = p.product_id;
```

#### Temporal Join with Delay Filter

Handle late-arriving dimension data:

```sql
CREATE MATERIALIZED VIEW enriched_orders_with_delay AS
SELECT
    o.order_id,
    o.product_id,
    p.product_name,
    o.quantity,
    o.order_time
FROM (
    SELECT * FROM order_events
    WHERE order_time + INTERVAL '5' SECOND < NOW()  -- Delay filter
) o
LEFT JOIN products p
    FOR SYSTEM_TIME AS OF PROCTIME()
    ON o.product_id = p.product_id;
```

### Interval Joins

Time-bounded joins between two streams.

```sql
-- Join clicks with impressions within a time window
CREATE MATERIALIZED VIEW click_through AS
SELECT
    i.impression_id,
    i.ad_id,
    i.user_id,
    c.click_time,
    i.impression_time
FROM impressions i
JOIN clicks c
    ON i.ad_id = c.ad_id
    AND i.user_id = c.user_id
    AND c.click_time BETWEEN i.impression_time
        AND i.impression_time + INTERVAL '1' HOUR;
```

### Deduplication Patterns

#### DISTINCT ON

```sql
-- Get latest order per customer
CREATE MATERIALIZED VIEW latest_orders AS
SELECT DISTINCT ON (customer_id)
    order_id,
    customer_id,
    order_time,
    total_price
FROM orders
ORDER BY customer_id, order_time DESC;
```

#### Deduplication with Window Functions

```sql
CREATE MATERIALIZED VIEW deduplicated_events AS
SELECT * FROM (
    SELECT
        *,
        ROW_NUMBER() OVER (
            PARTITION BY event_id
            ORDER BY event_time DESC
        ) AS rn
    FROM events
)
WHERE rn = 1;
```

### CDC Patterns

#### PostgreSQL CDC with Transformations

```sql
-- Source CDC table
CREATE TABLE source_orders (
    id INTEGER PRIMARY KEY,
    customer_id INTEGER,
    items JSONB,
    status VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
WITH (
    connector = 'postgres-cdc',
    hostname = 'postgres',
    port = '5432',
    username = 'cdc_user',
    password = 'password',
    database.name = 'app',
    schema.name = 'public',
    table.name = 'orders'
);

-- Transform CDC data
CREATE MATERIALIZED VIEW order_summary AS
SELECT
    id,
    customer_id,
    jsonb_array_length(items) AS item_count,
    status,
    CASE
        WHEN status = 'completed' THEN updated_at - created_at
        ELSE NULL
    END AS completion_time
FROM source_orders;
```

#### Multi-table CDC Join

```sql
-- CDC tables
CREATE TABLE cdc_customers (
    customer_id INTEGER PRIMARY KEY,
    name VARCHAR,
    email VARCHAR
)
WITH (connector = 'postgres-cdc', ...);

CREATE TABLE cdc_orders (
    order_id INTEGER PRIMARY KEY,
    customer_id INTEGER,
    total DECIMAL
)
WITH (connector = 'postgres-cdc', ...);

-- Join CDC tables
CREATE MATERIALIZED VIEW customer_orders AS
SELECT
    c.customer_id,
    c.name,
    c.email,
    COUNT(o.order_id) AS order_count,
    SUM(o.total) AS lifetime_value
FROM cdc_customers c
LEFT JOIN cdc_orders o ON c.customer_id = o.customer_id
GROUP BY c.customer_id, c.name, c.email;
```

---

## Data Types and Functions

### Supported Data Types

| Type | Description | Example |
|------|-------------|---------|
| `BOOLEAN` | True/false | `TRUE`, `FALSE` |
| `SMALLINT` | 2-byte integer | `32767` |
| `INTEGER` | 4-byte integer | `2147483647` |
| `BIGINT` | 8-byte integer | `9223372036854775807` |
| `REAL` | 4-byte float | `3.14` |
| `DOUBLE PRECISION` | 8-byte float | `3.141592653589793` |
| `NUMERIC` | Arbitrary precision | `123.456789` |
| `VARCHAR` | Variable-length string | `'hello'` |
| `BYTEA` | Binary data | `'\x48656c6c6f'` |
| `DATE` | Date without time | `'2024-01-15'` |
| `TIME` | Time without date | `'14:30:00'` |
| `TIMESTAMP` | Date and time | `'2024-01-15 14:30:00'` |
| `TIMESTAMPTZ` | Timestamp with timezone | `'2024-01-15 14:30:00+00'` |
| `INTERVAL` | Time span | `INTERVAL '1' DAY` |
| `JSONB` | Binary JSON | `'{"key": "value"}'` |
| `ARRAY` | Array type | `ARRAY[1, 2, 3]` |
| `STRUCT` | Composite type | `ROW(1, 'test')` |
| `MAP` | Key-value pairs | `MAP{'a': 1, 'b': 2}` |

### JSON/JSONB Functions

#### Extraction and Navigation

```sql
-- Extract values
SELECT
    data->'user'->>'name' AS user_name,           -- Text extraction
    data->'user'->'age' AS user_age,              -- JSON extraction
    data#>>'{user,address,city}' AS city,         -- Path extraction
    jsonb_extract_path_text(data, 'user', 'email') AS email
FROM events;
```

#### JSON Construction

```sql
-- Build JSON objects
SELECT
    jsonb_build_object(
        'user_id', user_id,
        'metrics', jsonb_build_object(
            'count', event_count,
            'total', total_amount
        )
    ) AS result
FROM user_metrics;

-- Aggregate to JSON array
SELECT
    user_id,
    jsonb_agg(
        jsonb_build_object(
            'event_type', event_type,
            'timestamp', event_time
        ) ORDER BY event_time
    ) AS events
FROM user_events
GROUP BY user_id;
```

#### JSON Manipulation

```sql
-- Object aggregation
SELECT
    jsonb_object_agg(key, value) AS config
FROM settings;

-- Strip nulls
SELECT jsonb_strip_nulls('{"a": 1, "b": null}'::jsonb);

-- Pretty print
SELECT jsonb_pretty(data) FROM events;

-- Convert to array
SELECT jsonb_to_array(items) FROM orders;
```

### Date/Time Functions

#### Current Time

```sql
SELECT
    NOW(),                              -- Current timestamp
    CURRENT_DATE,                       -- Current date
    CURRENT_TIME,                       -- Current time
    CURRENT_TIMESTAMP;                  -- Current timestamp with timezone
```

#### Extraction

```sql
SELECT
    EXTRACT(YEAR FROM order_time) AS year,
    EXTRACT(MONTH FROM order_time) AS month,
    EXTRACT(DAY FROM order_time) AS day,
    EXTRACT(HOUR FROM order_time) AS hour,
    EXTRACT(DOW FROM order_time) AS day_of_week,
    EXTRACT(EPOCH FROM order_time) AS unix_timestamp,
    DATE_PART('minute', order_time) AS minute
FROM orders;
```

#### Manipulation

```sql
SELECT
    DATE_TRUNC('hour', event_time) AS hour_start,
    DATE_TRUNC('day', event_time) AS day_start,
    event_time + INTERVAL '1' HOUR AS plus_one_hour,
    event_time - INTERVAL '30' MINUTE AS minus_30_min,
    AGE(NOW(), created_at) AS age
FROM events;
```

### Aggregate Functions

#### Basic Aggregates

```sql
SELECT
    COUNT(*) AS total_count,
    COUNT(DISTINCT user_id) AS unique_users,
    SUM(amount) AS total_amount,
    AVG(amount) AS average_amount,
    MIN(amount) AS min_amount,
    MAX(amount) AS max_amount,
    BOOL_AND(is_valid) AS all_valid,
    BOOL_OR(is_fraud) AS any_fraud
FROM transactions;
```

#### Statistical Functions

```sql
SELECT
    STDDEV_POP(amount) AS std_dev_pop,
    STDDEV_SAMP(amount) AS std_dev_sample,
    VAR_POP(amount) AS variance_pop,
    VAR_SAMP(amount) AS variance_sample
FROM transactions
GROUP BY category;
```

#### Ordered-Set Aggregates

```sql
SELECT
    category,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY amount) AS median,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY amount) AS p95,
    MODE() WITHIN GROUP (ORDER BY status) AS most_common_status
FROM orders
GROUP BY category;
```

#### Array and String Aggregation

```sql
SELECT
    user_id,
    ARRAY_AGG(product_id ORDER BY order_time) AS products_ordered,
    STRING_AGG(product_name, ', ' ORDER BY order_time) AS product_list
FROM orders
GROUP BY user_id;
```

### Window Functions

```sql
SELECT
    order_id,
    customer_id,
    total_price,
    -- Ranking
    ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY order_time) AS order_num,
    RANK() OVER (PARTITION BY customer_id ORDER BY total_price DESC) AS price_rank,
    DENSE_RANK() OVER (ORDER BY total_price DESC) AS dense_rank,

    -- Navigation
    LAG(total_price, 1) OVER (PARTITION BY customer_id ORDER BY order_time) AS prev_order,
    LEAD(total_price, 1) OVER (PARTITION BY customer_id ORDER BY order_time) AS next_order,
    FIRST_VALUE(total_price) OVER (PARTITION BY customer_id ORDER BY order_time) AS first_order,
    LAST_VALUE(total_price) OVER (
        PARTITION BY customer_id
        ORDER BY order_time
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS last_order,

    -- Running aggregates
    SUM(total_price) OVER (PARTITION BY customer_id ORDER BY order_time) AS running_total,
    AVG(total_price) OVER (
        PARTITION BY customer_id
        ORDER BY order_time
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS moving_avg
FROM orders;
```

### String Functions

```sql
SELECT
    -- Concatenation
    CONCAT(first_name, ' ', last_name) AS full_name,
    CONCAT_WS(', ', city, state, country) AS location,

    -- Case conversion
    UPPER(name) AS upper_name,
    LOWER(email) AS lower_email,
    INITCAP(title) AS title_case,

    -- Trimming
    TRIM(description) AS trimmed,
    LTRIM(code, '0') AS left_trimmed,
    RTRIM(path, '/') AS right_trimmed,

    -- Substring
    SUBSTRING(phone FROM 1 FOR 3) AS area_code,
    LEFT(zip_code, 5) AS zip5,
    RIGHT(account_number, 4) AS last_four,

    -- Search and replace
    POSITION('@' IN email) AS at_position,
    REPLACE(url, 'http://', 'https://') AS secure_url,

    -- Split
    SPLIT_PART(email, '@', 2) AS domain,
    REGEXP_SPLIT_TO_ARRAY(tags, ',') AS tag_array
FROM users;
```

#### Regular Expressions

```sql
SELECT
    -- Pattern matching
    REGEXP_MATCH(text, '\d{3}-\d{4}') AS phone_match,
    REGEXP_MATCHES(log_line, '(\w+)=(\w+)', 'g') AS key_values,

    -- Replacement
    REGEXP_REPLACE(phone, '[^0-9]', '', 'g') AS digits_only,

    -- Count matches
    REGEXP_COUNT(description, '\b\w+\b') AS word_count
FROM data;
```

---

## Common Use Cases

### Real-Time Analytics Dashboard

```sql
-- Source with watermark
CREATE SOURCE page_views (
    session_id VARCHAR,
    user_id VARCHAR,
    page_url VARCHAR,
    referrer VARCHAR,
    device_type VARCHAR,
    view_time TIMESTAMP,
    load_time_ms INTEGER,
    WATERMARK FOR view_time AS view_time - INTERVAL '10' SECOND
)
WITH (
    connector = 'kafka',
    topic = 'page-views',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT PLAIN ENCODE JSON;

-- Real-time metrics per minute
CREATE MATERIALIZED VIEW realtime_metrics AS
SELECT
    window_start,
    window_end,
    COUNT(*) AS page_views,
    COUNT(DISTINCT user_id) AS unique_users,
    COUNT(DISTINCT session_id) AS sessions,
    AVG(load_time_ms) AS avg_load_time,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY load_time_ms) AS p95_load_time
FROM TUMBLE(page_views, view_time, INTERVAL '1' MINUTE)
GROUP BY window_start, window_end
EMIT ON WINDOW CLOSE;

-- Traffic by device type
CREATE MATERIALIZED VIEW traffic_by_device AS
SELECT
    device_type,
    COUNT(*) AS views,
    COUNT(DISTINCT user_id) AS users
FROM page_views
GROUP BY device_type;

-- Top pages (rolling 5 minutes)
CREATE MATERIALIZED VIEW top_pages AS
SELECT
    window_start,
    page_url,
    COUNT(*) AS view_count
FROM HOP(page_views, view_time, INTERVAL '1' MINUTE, INTERVAL '5' MINUTE)
GROUP BY window_start, page_url
ORDER BY view_count DESC;
```

### Fraud Detection System

```sql
-- Transaction source
CREATE SOURCE transactions (
    transaction_id VARCHAR,
    card_id VARCHAR,
    merchant_id VARCHAR,
    amount DECIMAL,
    location VARCHAR,
    transaction_time TIMESTAMP,
    WATERMARK FOR transaction_time AS transaction_time - INTERVAL '5' SECOND
)
WITH (
    connector = 'kafka',
    topic = 'transactions',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT PLAIN ENCODE JSON;

-- Velocity check: cards used more than 5 times in 5 minutes
CREATE MATERIALIZED VIEW high_velocity_cards AS
SELECT
    window_start,
    window_end,
    card_id,
    COUNT(*) AS transaction_count,
    SUM(amount) AS total_amount
FROM TUMBLE(transactions, transaction_time, INTERVAL '5' MINUTE)
GROUP BY window_start, window_end, card_id
HAVING COUNT(*) > 5;

-- Geographic anomaly: same card in different locations within 30 minutes
CREATE MATERIALIZED VIEW geo_anomalies AS
SELECT
    t1.card_id,
    t1.location AS location1,
    t2.location AS location2,
    t1.transaction_time AS time1,
    t2.transaction_time AS time2
FROM transactions t1
JOIN transactions t2
    ON t1.card_id = t2.card_id
    AND t1.location != t2.location
    AND t2.transaction_time BETWEEN t1.transaction_time
        AND t1.transaction_time + INTERVAL '30' MINUTE
    AND t1.transaction_time < t2.transaction_time;

-- High-value transactions
CREATE MATERIALIZED VIEW high_value_alerts AS
SELECT
    transaction_id,
    card_id,
    amount,
    merchant_id,
    transaction_time
FROM transactions
WHERE amount > 10000;
```

### Event-Driven Order Processing

```sql
-- Order events
CREATE TABLE order_events (
    order_id VARCHAR PRIMARY KEY,
    customer_id VARCHAR,
    status VARCHAR,
    items JSONB,
    total_amount DECIMAL,
    event_time TIMESTAMP
)
WITH (
    connector = 'kafka',
    topic = 'order-events',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT UPSERT ENCODE JSON;

-- Customer dimension
CREATE TABLE customers (
    customer_id VARCHAR PRIMARY KEY,
    tier VARCHAR,
    region VARCHAR
)
WITH (connector = 'postgres-cdc', ...);

-- Order fulfillment SLA tracking
CREATE MATERIALIZED VIEW order_sla_tracking AS
SELECT
    o.order_id,
    o.customer_id,
    c.tier,
    o.status,
    o.event_time,
    CASE
        WHEN c.tier = 'premium' THEN INTERVAL '4' HOUR
        WHEN c.tier = 'standard' THEN INTERVAL '24' HOUR
        ELSE INTERVAL '48' HOUR
    END AS sla_deadline,
    o.event_time + CASE
        WHEN c.tier = 'premium' THEN INTERVAL '4' HOUR
        WHEN c.tier = 'standard' THEN INTERVAL '24' HOUR
        ELSE INTERVAL '48' HOUR
    END AS due_by
FROM order_events o
JOIN customers c ON o.customer_id = c.customer_id
WHERE o.status = 'pending';

-- Revenue by region (real-time)
CREATE MATERIALIZED VIEW revenue_by_region AS
SELECT
    c.region,
    DATE_TRUNC('hour', o.event_time) AS hour,
    COUNT(*) AS order_count,
    SUM(o.total_amount) AS revenue
FROM order_events o
JOIN customers c ON o.customer_id = c.customer_id
WHERE o.status = 'completed'
GROUP BY c.region, DATE_TRUNC('hour', o.event_time);
```

### IoT Sensor Monitoring

```sql
-- Sensor data source
CREATE SOURCE sensor_data (
    sensor_id VARCHAR,
    metric_name VARCHAR,
    value DOUBLE,
    unit VARCHAR,
    reading_time TIMESTAMP,
    WATERMARK FOR reading_time AS reading_time - INTERVAL '30' SECOND
)
WITH (
    connector = 'kafka',
    topic = 'sensor-readings',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT PLAIN ENCODE JSON;

-- Sensor thresholds dimension
CREATE TABLE sensor_thresholds (
    sensor_id VARCHAR PRIMARY KEY,
    metric_name VARCHAR,
    min_value DOUBLE,
    max_value DOUBLE,
    alert_enabled BOOLEAN
);

-- Anomaly detection
CREATE MATERIALIZED VIEW sensor_anomalies AS
SELECT
    s.sensor_id,
    s.metric_name,
    s.value,
    t.min_value,
    t.max_value,
    s.reading_time,
    CASE
        WHEN s.value < t.min_value THEN 'below_threshold'
        WHEN s.value > t.max_value THEN 'above_threshold'
        ELSE 'normal'
    END AS status
FROM sensor_data s
JOIN sensor_thresholds t
    FOR SYSTEM_TIME AS OF PROCTIME()
    ON s.sensor_id = t.sensor_id
    AND s.metric_name = t.metric_name
WHERE t.alert_enabled = TRUE
    AND (s.value < t.min_value OR s.value > t.max_value);

-- Hourly aggregations
CREATE MATERIALIZED VIEW hourly_sensor_stats AS
SELECT
    window_start,
    window_end,
    sensor_id,
    metric_name,
    AVG(value) AS avg_value,
    MIN(value) AS min_value,
    MAX(value) AS max_value,
    STDDEV_SAMP(value) AS std_dev,
    COUNT(*) AS reading_count
FROM TUMBLE(sensor_data, reading_time, INTERVAL '1' HOUR)
GROUP BY window_start, window_end, sensor_id, metric_name
EMIT ON WINDOW CLOSE;
```

### CDC Data Replication Pipeline

```sql
-- Source: PostgreSQL CDC
CREATE TABLE source_products (
    product_id INTEGER PRIMARY KEY,
    name VARCHAR,
    description TEXT,
    price DECIMAL,
    inventory INTEGER,
    category_id INTEGER,
    updated_at TIMESTAMP
)
WITH (
    connector = 'postgres-cdc',
    hostname = 'source-db',
    port = '5432',
    username = 'cdc_user',
    password = 'password',
    database.name = 'ecommerce',
    schema.name = 'public',
    table.name = 'products'
);

-- Transform and enrich
CREATE MATERIALIZED VIEW enriched_products AS
SELECT
    p.product_id,
    p.name,
    p.description,
    p.price,
    p.inventory,
    CASE
        WHEN p.inventory = 0 THEN 'out_of_stock'
        WHEN p.inventory < 10 THEN 'low_stock'
        ELSE 'in_stock'
    END AS stock_status,
    p.updated_at
FROM source_products p;

-- Sink to destination
CREATE SINK products_sink
FROM enriched_products
WITH (
    connector = 'kafka',
    topic = 'enriched-products',
    properties.bootstrap.server = 'localhost:9092'
)
FORMAT UPSERT ENCODE JSON;

-- Sink to data warehouse
CREATE SINK products_warehouse_sink
FROM enriched_products
WITH (
    connector = 'bigquery',
    bigquery.project = 'analytics-project',
    bigquery.dataset = 'ecommerce',
    bigquery.table = 'products'
);
```

---

## Performance Best Practices

### Index Optimization

```sql
-- Create indexes for frequently filtered columns
CREATE INDEX idx_orders_time ON orders (order_time);
CREATE INDEX idx_orders_customer_status ON orders (customer_id, status);

-- Index with included columns for covering queries
CREATE INDEX idx_orders_covering ON orders (customer_id)
INCLUDE (order_time, total_amount, status);
```

### Watermark Configuration

- Set watermark delay based on expected data latency
- Use `EMIT ON WINDOW CLOSE` for append-only sinks
- Consider late data handling requirements

```sql
-- Aggressive watermark for low-latency requirements
WATERMARK FOR event_time AS event_time - INTERVAL '1' SECOND

-- Conservative watermark for out-of-order data
WATERMARK FOR event_time AS event_time - INTERVAL '5' MINUTE
```

### Join Optimization

- Use temporal joins for fact-dimension patterns
- Use interval joins for time-bounded stream-stream joins
- Create indexes on dimension tables for lookup performance

```sql
-- Create index on dimension table for temporal join
CREATE INDEX idx_products_id ON products (product_id);

-- Use temporal join instead of regular join
SELECT * FROM orders o
LEFT JOIN products p
    FOR SYSTEM_TIME AS OF PROCTIME()
    ON o.product_id = p.product_id;
```

---

## Documentation References

- **CREATE SOURCE**: https://docs.risingwave.com/sql/commands/sql-create-source
- **CREATE MATERIALIZED VIEW**: https://docs.risingwave.com/sql/commands/sql-create-materialized-view
- **CREATE SINK**: https://docs.risingwave.com/sql/commands/sql-create-sink
- **Time Windows**: https://docs.risingwave.com/processing/sql/time-windows
- **Joins**: https://docs.risingwave.com/processing/sql/joins
- **Aggregate Functions**: https://docs.risingwave.com/sql/functions/aggregate
- **Window Functions**: https://docs.risingwave.com/sql/functions/window-functions
- **JSON Functions**: https://docs.risingwave.com/sql/functions/json
- **Date/Time Functions**: https://docs.risingwave.com/docs/current/sql-function-datetime/
- **Data Types**: https://docs.risingwave.com/sql/data-types/overview
- **Emit on Window Close**: https://docs.risingwave.com/processing/emit-on-window-close
- **PostgreSQL CDC**: https://docs.risingwave.com/ingestion/sources/postgresql/pg-cdc
- **MySQL CDC**: https://docs.risingwave.com/integrations/sources/mysql-cdc
