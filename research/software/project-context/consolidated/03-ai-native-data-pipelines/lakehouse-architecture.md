# Real-Time Open Data Lakehouse Architecture

## Executive Summary

This document details the integration of OLake, Lakekeeper, and RisingWave to construct a second-generation open data lakehouse. The architecture eliminates JVM overhead, provides secure credential vending, and delivers sub-second data freshness from transactional sources to analytical endpoints.

---

## 1. The Modern Data Stack Crisis

### 1.1 Legacy Bottlenecks

| Component | Problem | Impact |
|-----------|---------|--------|
| **Debezium** | JVM GC pauses, Kafka dependency | Latency spikes, operational overhead |
| **Hive Metastore** | Centralized bottleneck | Slow query planning, no multi-table transactions |
| **Spark** | Batch-only, heavy resource footprint | High latency, expensive compute |

Specific limitations:
- Debezium MongoDB connector: 16MB document size cap
- HMS lacks native Iceberg atomic transaction support
- GC pauses introduce unpredictable latency

### 1.2 Second-Generation Stack

| Layer | Technology | Advantage |
|-------|------------|-----------|
| **Ingestion** | OLake (Go) | 300K+ rows/sec, no JVM, direct Iceberg writes |
| **Governance** | Lakekeeper (Rust) | Deterministic latency, credential vending |
| **Compute** | RisingWave | Streaming SQL, materialized views on Iceberg |

---

## 2. OLake: High-Velocity Ingestion

### 2.1 Architecture

OLake is a Go-based ELT framework with modular **Protocol Layer**:
- **Drivers:** Database-specific extraction logic
- **Writers:** Destination-specific loading (Iceberg, Parquet)

### 2.2 Parallelized Snapshotting

Single-threaded snapshots on large tables take days. OLake splits tables into chunks processed concurrently:

| Database | Chunking Strategy | Method |
|----------|-------------------|--------|
| **PostgreSQL** | Physical Block | CTID (tuple identifier) ranges |
| **MySQL** | Key Range | Primary key range queries |
| **MongoDB** | Vector Splitting | Split-Vector/Bucket-Auto commands |

**Result:** >300,000 rows/second throughput

### 2.3 Log-Based CDC Mechanics

**PostgreSQL (pgoutput):**
```sql
-- Create publication for CDC
CREATE PUBLICATION olake_pub FOR ALL TABLES;

-- Create replication slot
SELECT pg_create_logical_replication_slot('olake_slot', 'pgoutput');
```

OLake consumes WAL stream via logical replication.

**MySQL (Binlog):**
```sql
-- Required settings
SET GLOBAL binlog_format = 'ROW';
SET GLOBAL binlog_row_image = 'FULL';  -- Both before/after images
```

OLake acts as replica, consuming binary log stream.

**MongoDB (Oplog):**
- Tails operations log (capped collection)
- Maintains native BSON structure (handles >16MB documents)

### 2.4 Configuration

**source.json (PostgreSQL):**
```json
{
  "host": "postgres.example.com",
  "port": 5432,
  "database": "production",
  "update_method": {
    "replication_slot": "olake_slot",
    "publication": "olake_pub",
    "initial_wait_time": 5
  },
  "max_threads": 8,
  "ssl": {
    "mode": "verify-full"
  }
}
```

**destination.json (Lakekeeper REST Catalog):**
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "rest",
    "uri": "http://lakekeeper:8181/catalog/",
    "iceberg_s3_path": "s3://warehouse/",
    "io_impl": "org.apache.iceberg.aws.s3.S3FileIO",
    "s3_path_style": true
  }
}
```

**Key Parameters:**
- `s3_path_style: true` - Required for MinIO (prevents DNS bucket resolution)
- `io_impl` - Use native Iceberg S3FileIO (not Hadoop S3A)

### 2.5 State Management

OLake maintains `state.json` with exact transaction log position:
- PostgreSQL: LSN (Log Sequence Number)
- MySQL: Binlog filename + offset
- MongoDB: Oplog timestamp

**Exactly-Once Semantics:**
1. Crash occurs mid-write
2. OLake restarts, reads `state.json`
3. Resumes from last checkpoint
4. Iceberg commits are atomic - no partial data

---

## 3. Lakekeeper: Governance Control Plane

### 3.1 Rust Architecture Benefits

| JVM Catalog | Rust (Lakekeeper) |
|-------------|-------------------|
| GC pauses cause latency spikes | Deterministic, predictable latency |
| Large memory footprint | Minimal binary, sidecar-deployable |
| Complex threading model | Memory-safe concurrency |

### 3.2 Entity Hierarchy

```
Server (Root)
└── Project (Tenant Isolation)
    └── Warehouse (Storage Backend)
        └── Namespace (Hierarchical Grouping)
            └── Table/View (Iceberg Tables)
```

**Multi-Tenancy:** Credentials for one warehouse cannot access another.

### 3.3 Credential Vending

Traditional lakes require "god mode" S3 access for all compute engines. Lakekeeper provides table-level security:

**Flow:**
1. Client authenticates with Lakekeeper (OAuth2/OIDC)
2. Lakekeeper verifies permissions
3. Lakekeeper calls AWS STS to assume role
4. Returns short-lived, scoped credentials (specific table prefix only)

**Result:** Compromised compute worker can't access entire lake.

### 3.4 Fine-Grained Authorization (OpenFGA)

Relationship-Based Access Control (ReBAC):
```
# Policy: User can read table if owner of parent project OR in auditor group
user:alice can read table:sales.q1 if
  owner of project:sales OR
  member of group:auditors
```

### 3.5 Bootstrapping

```bash
# Initialize Lakekeeper
curl -X POST http://localhost:8181/management/v1/bootstrap \
  -H 'Content-Type: application/json' \
  -d '{"accept-terms-of-use": true}'

# Create warehouse with MinIO storage
curl -X POST http://localhost:8181/management/v1/warehouse \
  -H 'Content-Type: application/json' \
  -d '{
    "warehouse-name": "main-warehouse",
    "storage-profile": {
      "type": "s3",
      "bucket": "iceberg-data",
      "endpoint": "http://minio:9000",
      "region": "us-east-1",
      "path-style-access": true,
      "flavor": "minio"
    },
    "storage-credential": {
      "type": "s3",
      "aws-access-key-id": "minioadmin",
      "aws-secret-access-key": "minioadmin"
    }
  }'
```

---

## 4. RisingWave: Streaming Compute

### 4.1 Architecture

- **Hummock:** LSM-tree storage engine optimized for S3
- **PostgreSQL Compatible:** Standard SQL, psql connectivity
- **Iceberg Native:** First-class source and sink support

### 4.2 Iceberg REST Catalog Connection

```sql
-- Create connection to Lakekeeper
CREATE CONNECTION lakekeeper_conn WITH (
  type = 'iceberg',
  catalog.type = 'rest',
  catalog.uri = 'http://lakekeeper:8181/catalog/',
  warehouse.path = 'main-warehouse',
  s3.endpoint = 'http://minio:9000',
  s3.access.key = 'minioadmin',
  s3.secret.key = 'minioadmin',
  s3.region = 'us-east-1',
  s3.path.style.access = 'true'
);

-- Set as default connection
SET iceberg_engine_connection = 'lakekeeper_conn';

-- Query OLake-created tables
SELECT * FROM main_warehouse.public.users LIMIT 10;
```

### 4.3 Materialized Views

```sql
-- Real-time aggregation on streaming data
CREATE MATERIALIZED VIEW order_stats AS
SELECT
  date_trunc('hour', created_at) as hour,
  count(*) as order_count,
  sum(total_amount) as revenue
FROM main_warehouse.sales.orders
GROUP BY 1;
```

RisingWave detects Iceberg snapshot changes and automatically refreshes.

---

## 5. Integration Architecture

### 5.1 Data Flow

```
PostgreSQL (Source)
    │
    ▼
OLake (CDC via pgoutput)
    │
    ├── Write Parquet files to S3
    │
    └── Commit Transaction to Lakekeeper
            │
            ▼
    Lakekeeper (Atomic metadata update)
            │
            ▼
    RisingWave (Detect new snapshot)
            │
            ▼
    Materialized Views / BI Tools
```

### 5.2 Latency Analysis

| Stage | Latency |
|-------|---------|
| Database replication | ~100ms |
| OLake buffering | Configurable (batch size/time) |
| Iceberg commit | ~50ms |
| RisingWave refresh | Configurable |
| **End-to-End** | Sub-minute (tuned) |

### 5.3 Consistency Guarantees

- **Read Committed / Snapshot Isolation**
- RisingWave never sees partial writes
- Snapshot switch in Lakekeeper is atomic
- No dirty reads of uncommitted files

---

## 6. Docker Compose Deployment

```yaml
version: "3.8"

services:
  # Storage Layer
  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9090"
    ports: ["9000:9000", "9090:9090"]
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin

  # Metadata Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
      POSTGRES_DB: postgres

  # Governance Layer
  lakekeeper:
    image: quay.io/lakekeeper/catalog:latest
    ports: ["8181:8181"]
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      LAKEKEEPER__PG_DATABASE_URL_READ: postgresql://postgres:password@postgres:5432/postgres
      LAKEKEEPER__PG_DATABASE_URL_WRITE: postgresql://postgres:password@postgres:5432/postgres
      LAKEKEEPER__PG_ENCRYPTION_KEY: "change-me-in-production"
      RUST_LOG: info
    command: ["serve"]

  # Ingestion Layer
  olake-ui:
    image: registry-1.docker.io/olakego/ui:latest
    ports: ["8000:8000"]
    environment:
      POSTGRES_DB: "postgres://postgres:password@postgres:5432/olake_db"
      PERSISTENT_DIR: /mnt/olake-data
    volumes:
      - ./olake-data:/mnt/olake-data

  # Compute Layer
  risingwave:
    image: risingwavelabs/risingwave:latest
    ports: ["4566:4566", "5691:5691"]
    command: risingwave playground
```

---

## 7. Operational Considerations

### 7.1 Schema Evolution

```
Source DDL Change (ALTER TABLE ADD COLUMN)
    │
    ▼
OLake CDC detects DDL event
    │
    ▼
OLake pauses data write
    │
    ▼
Sends UpdateSchema to Lakekeeper
    │
    ▼
Lakekeeper validates (safe evolution?)
    │
    ▼
Metadata updated, OLake resumes
```

**RisingWave:** May need `REFRESH` command or `auto.schema.change` configuration.

### 7.2 Small File Problem

High-frequency CDC creates many small Parquet files. Mitigation:

```sql
-- Periodic compaction job (Flink/Spark)
CALL system.rewrite_data_files(
  table => 'db.table',
  options => map('target-file-size-bytes', '134217728')  -- 128MB
);
```

Future Lakekeeper versions will support automated table maintenance.

### 7.3 Monitoring

| Component | Metrics |
|-----------|---------|
| **OLake** | `stats.json`: rows_synced, speed_rps, memory_usage |
| **Lakekeeper** | HTTP 5xx rate on `/catalog/` endpoints |
| **RisingWave** | Dashboard (port 5691), Prometheus metrics |

---

## 8. Performance Benchmarks

### 8.1 OLake Throughput

| Source | Rows/Second | Notes |
|--------|-------------|-------|
| PostgreSQL | 300,000+ | CTID chunking |
| MySQL | 250,000+ | PK range splitting |
| MongoDB | 200,000+ | Vector splitting |

### 8.2 Latency Comparison

| Stack | End-to-End Latency |
|-------|-------------------|
| Debezium + Kafka + Spark | 5-30 minutes |
| OLake + Lakekeeper + RisingWave | <1 minute |

### 8.3 Resource Footprint

| Component | Memory | Notes |
|-----------|--------|-------|
| OLake | ~500MB | Go, no JVM |
| Lakekeeper | ~100MB | Rust binary |
| Debezium + Kafka | 4-8GB+ | JVM heap tuning |

---

## 9. Implementation Priorities

### Phase 1: Core Infrastructure
1. Deploy MinIO + PostgreSQL + Lakekeeper
2. Bootstrap Lakekeeper with warehouse configuration
3. Verify REST catalog connectivity

### Phase 2: Ingestion Pipeline
1. Configure OLake source (PostgreSQL/MySQL)
2. Configure OLake destination (REST catalog)
3. Run initial snapshot + CDC

### Phase 3: Compute Layer
1. Deploy RisingWave
2. Create Iceberg connection
3. Build materialized views

### Phase 4: Production Hardening
1. Configure credential vending
2. Set up OpenFGA policies
3. Implement compaction jobs
4. Configure monitoring/alerting

---

## References

- OLake Documentation: https://olake.io/docs/
- Lakekeeper: https://docs.lakekeeper.io/
- RisingWave Iceberg: https://docs.risingwave.com/iceberg/
- Apache Iceberg REST Catalog Spec: https://iceberg.apache.org/docs/latest/rest-catalog/
