# RisingWave Best Practices, Performance Optimization, and Operational Guidance

A comprehensive guide for developers working with RisingWave, the cloud-native streaming database.

---

## Table of Contents

1. [Schema Design](#1-schema-design)
2. [Performance Optimization](#2-performance-optimization)
3. [Operational Best Practices](#3-operational-best-practices)
4. [Anti-patterns to Avoid](#4-anti-patterns-to-avoid)
5. [Comparison with Other Systems](#5-comparison-with-other-systems)

---

## 1. Schema Design

### Primary Key Selection

**Upsert Behavior**: For tables with primary key constraints, inserting a record with an existing key will **overwrite** the existing record. Design your keys accordingly.

```sql
-- Primary key enables upsert semantics
CREATE TABLE orders (
  order_id INT PRIMARY KEY,
  customer_id INT,
  amount DECIMAL,
  status VARCHAR
);
```

**Implicit Row ID**: For append-only streams without explicit primary keys, RisingWave derives a primary key by adding a `row_id` column, converting the stream to upsert semantics internally.

**Best Practices**:
- Choose primary keys that match your upsert/deduplication requirements
- For CDC sources, always define primary keys matching the source table
- Consider composite primary keys for tables that need uniqueness across multiple columns

### Index Strategies

RisingWave indexes are **implemented as specialized materialized views**, making them cost-effective to create and maintain.

```sql
-- Basic index creation
CREATE INDEX idx_orders_customer ON orders(customer_id);

-- Index with included columns (for covering queries)
CREATE INDEX idx_orders_customer ON orders(customer_id)
  INCLUDE (amount, status);

-- Specify distribution for prefix queries
CREATE INDEX idx_customers_name ON customers(c_name, c_nationkey)
  DISTRIBUTED BY (c_name);
```

**Key Guidelines**:

1. **Check SELECT columns**: All columns in your SELECT should appear in the INCLUDE clause
2. **Check WHERE conditions**: Columns used in filtering should be in index_column
3. **Timestamp ranges**: If filtering by timestamp (`BETWEEN t1 AND t2`), include that column in index_column
4. **Distribution matters**: RisingWave distributes data using the first index column by default. Specify DISTRIBUTED BY if queries only provide a prefix of index columns

**Index vs PostgreSQL Difference**: By default, RisingWave includes **all columns** of a table in an index if you omit the INCLUDE clause. This eliminates primary table lookups, which are slower in cloud environments.

### Partitioning Approaches

RisingWave uses **consistent hashing** for automatic data partitioning across compute nodes for parallel execution.

**Explicit Partitioning**:
- Use `DISTRIBUTED BY` in index creation to control data distribution
- Plan partitioning keys carefully - they affect query patterns and cross-partition operations

### Data Modeling for Streaming

#### Tables vs Sources

| Feature | Table | Source |
|---------|-------|--------|
| Data persistence | Yes | No |
| Storage space | Higher | Lower |
| Supports updates/deletes | Yes | No |
| Optimization potential | Lower | Higher (append-only) |

```sql
-- Source: no persistence, cannot modify data
CREATE SOURCE pageviews (
  user_id INT,
  page VARCHAR,
  timestamp TIMESTAMP
) WITH (
  connector = 'kafka',
  ...
);

-- Table: persists data, supports updates
CREATE TABLE user_sessions (
  session_id INT PRIMARY KEY,
  user_id INT,
  start_time TIMESTAMP
) WITH (
  connector = 'kafka',
  ...
);
```

#### Append-Only Tables with Watermarks

For streaming scenarios with time-based processing:

```sql
CREATE TABLE user_actions (
  user_name VARCHAR,
  data VARCHAR,
  user_action_time TIMESTAMP,
  -- Watermark allows 5 seconds late data
  WATERMARK FOR user_action_time AS user_action_time - INTERVAL '5' SECOND
) APPEND ONLY
WITH (connector = 'kafka', ...);
```

**Watermark Benefits**:
- Controls state size by filtering late data
- Enables temporal joins and windowing
- Only available for append-only tables

#### Temporal Filters for Data Cleanup

Clean up old data in materialized views:

```sql
CREATE MATERIALIZED VIEW recent_sales AS
SELECT * FROM sales
WHERE sale_date > NOW() - INTERVAL '1 week';
```

---

## 2. Performance Optimization

### Query Optimization Techniques

#### Streaming Query Optimization

1. **Bushy Join Trees**: RisingWave's optimizer creates bushy join trees when possible, enabling parallel data flow and reducing latency

2. **Cascading Materialized Views (MV-on-MV)**:
   ```sql
   -- Base aggregation
   CREATE MATERIALIZED VIEW hourly_sales AS
   SELECT product_id, date_trunc('hour', sale_time) as hour, SUM(amount) as total
   FROM sales
   GROUP BY product_id, date_trunc('hour', sale_time);

   -- Build on top without middleware
   CREATE MATERIALIZED VIEW daily_sales AS
   SELECT product_id, date_trunc('day', hour) as day, SUM(total) as daily_total
   FROM hourly_sales
   GROUP BY product_id, date_trunc('day', hour);
   ```

3. **Minimize Join Rows**: Reduce join cardinality to prevent bottlenecks
4. **Simplify Complex Queries**: Remove unnecessary joins, subqueries, and functions

#### Batch Query Optimization

- Use indexes for frequently accessed columns
- Increase `block_cache` and `meta_cache` for batch-serving nodes
- Avoid full table scans - ensure WHERE clauses use indexed columns

### Memory Management

#### Cache Types and Configuration

| Cache Type | Purpose | Best For |
|------------|---------|----------|
| Operator Cache | Intermediate state for joins/aggregations | Streaming queries |
| Block Cache | Cached data blocks from storage | Batch queries |
| Meta Cache | SST metadata | Both |

**Default Streaming Configuration**: More memory allocated to operator cache

**Batch-Serving Configuration**: Increase block_cache and meta_cache, reduce operator_cache

#### Memory Architecture

RisingWave reserves **30%** of total memory as buffer for traffic spikes. The remaining 70% is usable memory with tiered eviction:

- **Stable threshold**: Normal operation
- **Graceful threshold**: Begin eviction
- **Aggressive threshold**: Intensify eviction
- **>90%**: Maximum eviction intensity

Configure eviction with `memory_controller_eviction_factor_XXX` variables.

#### Memory-Only Mode (v2.6+)

For workloads where operator states fit in memory:
- Eliminates cache misses
- Provides consistent low latency
- Requires sufficient memory for all intermediate states

### Parallelism Configuration

```sql
-- Set parallelism for streaming queries in current session
SET streaming_parallelism = 16;

-- Set parallelism for batch queries
SET batch_parallelism = 8;
```

**Calculation**: With 3 compute nodes, each with 8 CPUs, maximum parallelism = 24

**Best Practices**:
- Start with defaults, increase for bottleneck fragments
- Use Grafana to identify parallelism needs
- Consider scaling out vs scaling up

### Checkpoint Tuning

#### Key Parameters

```toml
[system]
barrier_interval_ms = 1000    # Default: 1 second
checkpoint_frequency = 1      # Checkpoints per barrier
```

**Default Interval**: 1 second (vs Flink's default of 30 minutes)

#### Checkpoint Mechanism

1. Meta node injects barriers into input streams
2. Barriers flow downstream with data (never overtaking)
3. Compute nodes buffer dirty states in shared buffer
4. Async flush to SST files in object storage (S3)
5. Checkpoint completes when all states registered with meta service

**Tuning Considerations**:
- Lower RPO (less data loss) = more frequent checkpoints
- More frequent checkpoints = higher storage/compute overhead
- Shared buffer capacity: 4GB max by default

#### Troubleshooting Checkpoints

Monitor **Barrier Latency** in Grafana dashboard > Streaming. High latency indicates pipeline slowdown.

```sql
-- Trigger ad-hoc recovery (superuser only)
RECOVER;

-- Alter streaming rate limit during recovery
ALTER SYSTEM SET streaming_rate_limit = ...;
```

---

## 3. Operational Best Practices

### Monitoring and Observability

#### Critical Metrics to Monitor

| Metric | Location | Issue Indicator |
|--------|----------|-----------------|
| Barrier Latency | Streaming panel | Consistently high = pipeline stuck |
| Actor Output Blocking Time | Streaming Actors | High = backpressure |
| Executor Cache Miss Ratio | Streaming Actors | High = insufficient memory |
| CPU Usage (avg per core) | Cluster Node | >80% sustained = bottleneck |
| Uploading Memory | Hummock (Write) | High = shared buffer issues |

#### Grafana Dashboard Navigation

- **Streaming performance**: Grafana dashboard (dev) > Streaming
- **Actor-level metrics**: Grafana dashboard (dev) > Streaming Actors
- **Storage performance**: Grafana dashboard (dev) > Hummock (Read/Write)
- **Node resources**: Grafana dashboard (dev) > Cluster Node

#### Log Analysis

Search for specific patterns:
- `"blocked at requiring memory"` - State table writes waiting for shared buffer
- Error messages indicating failures

### Backup and Recovery

#### Checkpoint-Based Recovery

**RPO (Recovery Point Objective)**:
- Directly tied to checkpoint frequency
- Shorter RPO = more frequent checkpoints
- Configure upstream sources (Kafka) for replayability

**RTO (Recovery Time Objective)**:
- Depends on state size and network performance
- RisingWave's architecture enables efficient recovery
- Can scale infrastructure quickly (Kubernetes pods, VMs)

#### Recovery Process

1. Detect failure via health checks
2. Load last checkpoint from Hummock (object storage)
3. Resume processing from correct upstream offsets
4. Replay data from last checkpoint

**Upstream Configuration**: Ensure Kafka topics are configured for durability and replayability to achieve near-zero RPO.

### Scaling Strategies

#### Decoupled Compute and Storage

RisingWave's architecture allows independent scaling:
- **Compute**: Scale up (more CPU/memory) or out (more nodes)
- **Storage**: Object storage (S3) scales automatically

#### Dynamic Scaling During Backfill

Add nodes dynamically during backfill operations for high-parallelism processing. Backfill occurs during:
- Initial stream computation
- Upstream format changes
- Logic modifications
- Failure recovery

#### Resource Isolation

Configure separate node types for different workloads:
- **Compute nodes**: Stream processing
- **Serving nodes**: Batch queries

### Troubleshooting Common Issues

#### High Latency

**Diagnosis Steps**:
1. Check Barrier Latency panel for stuck barriers
2. Check backpressure panel for blocked actors
3. Check resource utilization (CPU >80%, memory, cache miss)

**Solutions**:
- Increase parallelism for bottleneck fragments
- Scale up compute resources
- Optimize queries (remove unnecessary joins)

#### Backpressure

**Finding Root Cause**:
1. Open Grafana "Streaming - Backpressure" panel
2. Find channels with high backpressure
3. Identify frontmost fragment (backpressure propagates upstream)

**Solutions**:
- Increase parallelism for slow operators
- Optimize query logic
- Scale resources

#### Out of Memory (OOM)

**Most common production issue!**

**Causes**:
- Caching overflow
- Large computation states
- Network transmission buffers
- Execution planning

**Solutions**:
- Scale up memory
- Scale out to distribute load
- Tune cache eviction policies
- Optimize queries to reduce state size

#### Cache Miss Issues

**Symptoms**:
- Executor memory usage smaller than expected
- High executor cache miss ratio

**Solutions**:
- Increase compute node memory
- Scale out to more nodes

---

## 4. Anti-patterns to Avoid

### Query Anti-patterns

1. **Unnecessary Joins**: Each join maintains state; remove joins that aren't essential
2. **Full Table Scans**: Always use indexed columns in WHERE clauses
3. **Inefficient WHERE Clauses**: Avoid complex functions that prevent index usage
4. **Heavy Ad-hoc OLAP Queries**: RisingWave isn't optimized for full-scan analytics

   ```sql
   -- Anti-pattern: Ad-hoc OLAP on RisingWave
   SELECT product_category, SUM(amount)
   FROM all_sales_history  -- billions of rows
   GROUP BY product_category;

   -- Better: Sink to dedicated OLAP system (ClickHouse, Pinot)
   ```

5. **Low-Cardinality Join Columns**: Easily triggers massive row amplification

### State Management Anti-patterns

1. **Unbounded State Growth**: Use temporal filters and TTL to manage state size

   ```sql
   -- Anti-pattern: Unbounded state
   CREATE MATERIALIZED VIEW all_events AS SELECT * FROM events;

   -- Better: Temporal filter
   CREATE MATERIALIZED VIEW recent_events AS
   SELECT * FROM events WHERE event_time > NOW() - INTERVAL '7 days';
   ```

2. **Expensive Aggregations**: Functions like `array_agg` are costly
3. **Ignoring Watermarks**: Without watermarks, state can grow indefinitely

### Design Anti-patterns

1. **Using RisingWave as Primary OLAP**: Sink output to dedicated OLAP systems for complex analytics
2. **Not Defining Primary Keys for CDC**: CDC sources require primary keys for correctness
3. **Ignoring NULL Handling**:
   - Batch inserts throw errors for NULL in NOT NULL columns
   - Streaming ignores rows with NULL in NOT NULL columns

### Performance Anti-patterns

1. **Not Creating Indexes**: Indexes are cheap in RisingWave - create them for repeated query patterns
2. **Wrong Distribution Key**: Causes hotspots and uneven load
3. **Not Monitoring Cache Performance**: High cache miss = degraded performance
4. **Ignoring Backpressure**: It's a symptom of deeper issues

### Configuration Anti-patterns

1. **Default Configuration for All Workloads**:
   - Streaming-optimized nodes need larger operator cache
   - Batch-serving nodes need larger block/meta cache

2. **Not Tuning Checkpoint Frequency**: Balance RPO requirements vs overhead

---

## 5. Comparison with Other Systems

### RisingWave vs Apache Flink

| Aspect | RisingWave | Apache Flink |
|--------|------------|--------------|
| **Interface** | SQL (PostgreSQL syntax) | Java/Scala/Python APIs, Flink SQL |
| **Learning Curve** | Lower (familiar SQL) | Higher (new APIs) |
| **State Storage** | Cloud object storage (S3) | Local (RocksDB) |
| **Checkpoint Default** | 1 second | 30 minutes |
| **Infrastructure** | Managed, cloud-native | Own cluster required |
| **Cascading MVs** | Yes (MV-on-MV) | No native equivalent |
| **CEP/ML APIs** | No | Yes |

**Choose RisingWave when**:
- Team is familiar with PostgreSQL
- You want simpler operations
- Cloud-native deployment is important
- You need cascading materialized views

**Choose Flink when**:
- You need multiple language support
- Complex event processing (CEP) is required
- ML pipeline integration is needed
- Unified batch and stream processing is critical

### RisingWave vs ksqlDB

| Aspect | RisingWave | ksqlDB |
|--------|------------|--------|
| **Architecture** | Standalone database | Kafka-dependent |
| **State Storage** | Cloud object storage | Kafka topics + RocksDB |
| **Resource Efficiency** | Higher | Lower (several times more for same state) |
| **Data Consistency** | Stronger guarantees | Potential inconsistency issues |
| **Use Case Scope** | Broader | Kafka-centric |

**Choose RisingWave when**:
- You need a standalone streaming database
- Resource efficiency is important
- You have complex use cases beyond simple transformations

**Choose ksqlDB when**:
- You're deeply invested in the Kafka ecosystem
- You have simpler stream processing needs
- You want tight Kafka integration

### RisingWave vs Materialize

| Aspect | RisingWave | Materialize |
|--------|------------|-------------|
| **State Storage** | Cloud object storage | In-memory |
| **Scalability** | Horizontal (distributed) | Limited (effectively single-node) |
| **Checkpointing** | Yes | No (replay recovery) |
| **Engine** | Custom | Timely/Differential Dataflow |
| **PostgreSQL Compatibility** | Yes | Yes |

**Choose RisingWave when**:
- You need to handle large state sizes
- Horizontal scalability is required
- Checkpoint-based recovery is preferred

**Choose Materialize when**:
- State fits in memory
- You need complex SQL joins with very fresh results
- You can tolerate replay-based recovery

### When to Choose RisingWave

**Ideal Use Cases**:
- Real-time dashboards and monitoring
- Alerting systems
- Event-driven applications
- Streaming ETL
- Real-time feature engineering

**RisingWave Excels When**:
- You want PostgreSQL-like simplicity
- You need cloud-native operations with separate compute/storage scaling
- You require cascading materialized views
- You prefer SQL-only interface
- You need frequent checkpoints for low RPO
- You want indexes that are cheap to create and maintain

**Consider Alternatives When**:
- You need complex OLAP analytics (use ClickHouse, Pinot, Druid)
- You need CEP or ML APIs (use Flink)
- You're committed to Kafka ecosystem (consider ksqlDB)
- All state fits in memory and you want Differential Dataflow (consider Materialize)

---

## Quick Reference

### Essential SQL Commands

```sql
-- Check system parameters
SHOW PARAMETERS;

-- Alter system parameters (superuser)
ALTER SYSTEM SET barrier_interval_ms = 500;

-- Set session parallelism
SET streaming_parallelism = 16;
SET batch_parallelism = 8;

-- Trigger recovery (superuser)
RECOVER;

-- Create index with best practices
CREATE INDEX idx_name ON table(filter_columns)
  INCLUDE (select_columns)
  DISTRIBUTED BY (prefix_columns);

-- Create temporal filter MV
CREATE MATERIALIZED VIEW mv_name AS
SELECT * FROM source
WHERE timestamp_col > NOW() - INTERVAL '7 days';

-- Create watermarked append-only table
CREATE TABLE t (
  ...,
  ts TIMESTAMP,
  WATERMARK FOR ts AS ts - INTERVAL '5' SECOND
) APPEND ONLY;
```

### Key Configuration Parameters

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `barrier_interval_ms` | 1000 | Checkpoint interval |
| `checkpoint_frequency` | 1 | Checkpoints per barrier |
| `streaming_parallelism` | Auto | Parallel workers for streaming |
| `batch_parallelism` | Auto | Parallel workers for batch |

### Monitoring Checklist

- [ ] Barrier latency stable
- [ ] No sustained backpressure
- [ ] CPU usage < 80%
- [ ] Cache miss ratio acceptable
- [ ] No OOM errors in logs
- [ ] Checkpoint completion normal

---

## Additional Resources

- **Official Documentation**: https://docs.risingwave.com
- **Tutorials**: https://tutorials.risingwave.com
- **GitHub**: https://github.com/risingwavelabs/risingwave
- **Blog**: https://risingwave.com/blog

---

*Research compiled: 2025-11-18*
