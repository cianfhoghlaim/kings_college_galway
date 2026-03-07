# OLake Architecture Patterns & Integration Guide

## Table of Contents
1. [Architectural Patterns](#architectural-patterns)
2. [Integration Patterns](#integration-patterns)
3. [Deployment Patterns](#deployment-patterns)
4. [Best Practices](#best-practices)

---

## Architectural Patterns

### Overview

OLake is an open-source, high-performance data replication tool that transforms databases into Apache Iceberg-based data lakehouses. Its architecture is built on four core components: CLI, Framework (CDK), Connectors/Drivers, and Writers.

### 1. Direct-Write vs Queue-Based Architecture

#### Direct-Write Architecture (OLake Approach)

OLake implements a **direct-write pattern** that eliminates intermediary storage queues:

```
Source Database
    ↓
Driver (Connector)
    ↓
Writer (Iceberg/Parquet)
    ↓
Object Storage (S3, GCS, MinIO)
```

**Key Benefits:**
- **Reduced Latency**: Records are pushed immediately upon extraction, not queued
- **Lower Resource Usage**: No intermediate message broker or staging database required
- **Fewer Data Copies**: Direct path from source to destination
- **Simpler Architecture**: Fewer moving parts to manage and troubleshoot

**Performance Example:**
- MongoDB replication: 230 million rows in 46 minutes
- Parallel processing with concurrent chunks
- Near real-time CDC following initial snapshot

#### Alternative Queue-Based Approach

Traditional architectures use intermediate storage:
```
Source → Kafka/RabbitMQ → Processing → Storage
```

**Trade-offs:**
- Better for handling traffic spikes
- More complex infrastructure
- Higher latency but better decoupling

**Decision:**
Use OLake's direct-write when:
- Real-time latency is critical (<5 min target)
- Infrastructure simplicity is valued
- CDC streams are consistent and manageable

### 2. Go-Java Hybrid Design Rationale

#### Go Implementation (Core Framework)

OLake's main binary is written in Go:

```go
// Core components in Go:
- CLI interface
- Connector framework
- State management
- Schema discovery
- Parallel chunking logic
```

**Advantages:**
- Single self-contained binary (cross-platform)
- Excellent concurrency model (goroutines)
- Fast startup and deployment
- Low memory footprint
- Easy Docker containerization

#### Java Integration (Iceberg Writer)

The Iceberg writer integrates with Java ecosystem:

```
OLake (Go) ↔ gRPC ↔ Java Iceberg Service
```

**Java Components:**
- Apache Iceberg client libraries
- Catalog implementations (JDBC, Glue, REST)
- Schema evolution handling
- Transaction management

**Configuration Example:**
```json
{
  "destination": {
    "type": "ICEBERG",
    "writer": {
      "catalog_type": "rest",  // Can be: rest, glue, jdbc, hive
      "rest_catalog_url": "http://lakekeeper:8181",
      "warehouse": "s3://my-bucket/warehouse",
      "s3_endpoint": "https://s3.amazonaws.com",
      "aws_region": "us-east-1"
    }
  }
}
```

#### Design Rationale

1. **Separation of Concerns**: Go handles extraction/orchestration, Java handles Iceberg complexity
2. **Leveraging Ecosystems**: Use best tool for each job
3. **Stability**: Iceberg libraries are Java-native
4. **Scalability**: Go for high-concurrency I/O, Java for complex transactions

### 3. Plugin Architecture for Sources & Destinations

#### Driver/Connector Architecture

OLake uses a pluggable driver pattern:

```
┌─────────────────┐
│  OLake Core     │
│  Framework      │
└────────┬────────┘
         │
    ┌────┴────┬──────────┬─────────┐
    ↓         ↓          ↓         ↓
 MongoDB   PostgreSQL  MySQL     Oracle
 Driver    Driver      Driver    Driver
```

#### Core Driver Interface

Every driver implements four standard commands:

```bash
# 1. Specification (configuration schema)
olake discover --config source.json

# 2. Connection validation
olake check --config source.json

# 3. Schema discovery
olake discover --config source.json > streams.json

# 4. Data replication
olake sync --config source.json \
           --catalog streams.json \
           --destination dest.json
```

#### Driver Responsibilities

```
┌─────────────────────────────────────┐
│     Source Connector (Driver)        │
├─────────────────────────────────────┤
│ • Full Load (parallel chunking)      │
│ • CDC setup (replication slots, etc) │
│ • Schema detection                   │
│ • Schema evolution handling          │
│ • Connection pooling                 │
│ • Error recovery & checkpointing     │
└─────────────────────────────────────┘
```

#### Writer Architecture

Writers handle destination-specific logic:

```json
{
  "writers": [
    {
      "type": "ICEBERG",
      "catalog_type": "rest",
      "features": [
        "atomic_writes",
        "schema_evolution",
        "partitioning",
        "compaction"
      ]
    },
    {
      "type": "PARQUET",
      "storage": "s3://bucket/parquet",
      "partitioning": "year/month/day"
    }
  ]
}
```

#### Creating Custom Drivers

Plugin interface (simplified pseudocode):

```go
type Driver interface {
    // Configuration schema
    Spec() json.Schema
    
    // Validate connection
    Check(config Config) error
    
    // Discover tables and columns
    Discover(config Config) (Catalog, error)
    
    // Execute replication
    Sync(config Config, state State) (State, error)
}

// MongoDB driver implementation
type MongoDBDriver struct {
    client *mongo.Client
}

func (d *MongoDBDriver) Sync(config Config, state State) (State, error) {
    // Parallel chunking logic
    chunks := d.partitionCollections(config.Collections)
    
    // Process chunks concurrently
    results := d.processChunksParallel(chunks, state)
    
    // Write to destination via writer
    return d.writer.Write(results)
}
```

### 4. State Management & Checkpointing

#### State Structure

OLake maintains detailed state for resumability:

```json
{
  "version": 1,
  "scope": "global",  // or "stream"
  "streams": [
    {
      "stream_id": "postgres.public.users",
      "sync_type": "incremental",  // full-load, cdc, or incremental
      "stream_state": {
        "cursor_field": "updated_at",
        "cursor_value": "2025-01-15T10:30:00Z",
        "chunks": [
          {
            "chunk_id": 1,
            "min_cursor": "2025-01-15T00:00:00Z",
            "max_cursor": "2025-01-15T10:00:00Z",
            "status": "succeeded"
          }
        ]
      }
    }
  ]
}
```

#### Checkpoint Mechanism

**Full Load Checkpointing:**

```
Phase 1: Initial snapshot
├── Partition collection into chunks
├── Process each chunk in parallel
├── Track completion status per chunk
└── Mark "full_load" complete when all chunks done

Resumability: If sync fails at chunk 50/100
  → Restart skips processed chunks 1-49
  → Resumes from chunk 50
```

**CDC Checkpointing:**

```go
type CDCCheckpoint struct {
    // PostgreSQL WAL position
    WALPosition     string  // e.g., "0/12345678"
    
    // MongoDB change stream resume token
    ResumeToken     string  // Base64 encoded token
    
    // Cursor for general CDC
    LastCursorID    interface{}
    
    // Timestamp of last processed change
    LastProcessedAt time.Time
}
```

**Resumable Replication:**

```
Initial Run:
1. Full load from table → Parquet files
2. Create cursor checkpoint (e.g., max(updated_at))
3. Start CDC from checkpoint
4. Stream changes → Iceberg incremental snapshots

Resume after failure:
1. Load cursor from checkpoint
2. Skip already-loaded chunks
3. Continue CDC from last position
4. No data duplication, no gaps
```

#### State Persistence

```bash
# State stored in Iceberg metadata or external DB
# Example with PostgreSQL state store:

CREATE TABLE olake_state (
    stream_id VARCHAR(255),
    sync_type VARCHAR(50),
    cursor_field VARCHAR(255),
    cursor_value TEXT,
    checkpoint_time TIMESTAMP,
    status VARCHAR(50),
    PRIMARY KEY (stream_id)
);

# Configuration:
olake sync --config source.json \
           --state-backend postgresql \
           --state-url "postgresql://user:pass@localhost/olake"
```

### 5. Error Handling Patterns

#### Retry Strategy

```json
{
  "retry_policy": {
    "max_retries": 3,
    "initial_backoff_ms": 1000,
    "backoff_multiplier": 2.0,
    "max_backoff_ms": 60000,
    "retry_on": [
      "network_timeout",
      "temporary_database_error",
      "s3_rate_limit"
    ]
  }
}
```

#### Implemented Error Handling

```
Failure Types:
├── Transient (retry)
│   ├── Network timeout
│   ├── Database connection lost
│   └── S3 rate limiting
├── Permanent (skip/alert)
│   ├── Schema mismatch
│   ├── Permission denied
│   └── Corrupted data
└── Manual intervention
    ├── Table schema changed
    ├── Source data corruption
    └── Destination out of space
```

#### Dead Letter Queue Pattern

```go
// For unrecoverable records:
type DeadLetterQueue struct {
    Records     []Record      // Failed records
    Error       string        // Error reason
    Timestamp   time.Time
    RetryCount  int
}

// Store in S3 for analysis:
// s3://bucket/dead-letter-queue/{stream}/{date}/{error_type}/
```

#### Monitoring & Alerting

```yaml
# Prometheus metrics
olake_sync_duration_seconds     # Histogram of sync times
olake_records_processed_total    # Counter of processed records
olake_errors_total               # Counter of errors by type
olake_checkpoint_lag_seconds     # Gauge: how far behind source
olake_file_count_total           # Small file problem indicator
```

---

## Integration Patterns

### 1. Lakekeeper (REST Catalog) Integration

#### Architecture

```
OLake ──(REST API)──> Lakekeeper ──(Metadata)──> PostgreSQL
         ↓                           ↓
    Write Parquet              Table Catalog
    to S3/MinIO               ACID Guarantees
```

#### Configuration

```json
{
  "destination": {
    "type": "ICEBERG",
    "writer": {
      "catalog_type": "rest",
      "rest_catalog_url": "http://lakekeeper.example.com:8181",
      "warehouse": "s3://my-bucket/warehouse",
      "namespace": "analytics",
      "token": "${LAKEKEEPER_API_TOKEN}"
    }
  }
}
```

#### Integration Workflow

```bash
# 1. Setup Lakekeeper with PostgreSQL backend
docker run -d \
  -e DATABASE_URL="postgresql://user:pass@postgres/lakekeeper" \
  -p 8181:8181 \
  apache/iceberg:latest-rest

# 2. Configure OLake with REST catalog
# (shown above in configuration)

# 3. Run discovery and sync
olake discover --config source.json > streams.json
olake sync --config source.json \
           --catalog streams.json \
           --destination destination.json

# 4. Query from Trino/Presto
SELECT * FROM iceberg.analytics.postgres_users LIMIT 10;

# 5. Time travel to previous snapshot
SELECT * FROM iceberg.analytics.postgres_users 
FOR SYSTEM_TIME AS OF TIMESTAMP '2025-01-15 10:00:00';
```

#### Key Features

| Feature | Benefit |
|---------|---------|
| REST API | Language-agnostic catalog access |
| PostgreSQL Metadata | Persistent, queryable table catalog |
| Change Events | Audit trail of all modifications |
| OIDC Integration | Centralized authentication |
| Fine-grained ACLs | Row/column-level access control |

### 2. LakeFS Version Control Integration

#### Architecture

```
LakeFS (Version Control Layer)
├── main branch (production)
├── dev branch (development)
└── staging branch (validation)
    ↓
S3 / MinIO / GCS (Object Storage)
    ↓
Iceberg Tables (Lakekeeper)
```

#### Integration Setup

```bash
# 1. Configure OLake to write to LakeFS S3 endpoint
cat destination.json
{
  "destination": {
    "type": "ICEBERG",
    "writer": {
      "catalog_type": "rest",
      "rest_catalog_url": "http://lakekeeper:8181",
      "warehouse": "s3://lakefs-endpoint/analytics/main/warehouse",
      "s3_endpoint": "http://lakefs:8000",  # LakeFS endpoint
      "aws_access_key": "${LAKEFS_ACCESS_KEY}",
      "aws_secret_key": "${LAKEFS_SECRET_KEY}"
    }
  }
}

# 2. Run sync to staging branch
olake sync --config source.json \
           --catalog streams.json \
           --destination destination.json \
           --lakefs-branch staging

# 3. Validate data on staging
docker run trino --server http://trino:8080
SELECT COUNT(*) FROM iceberg.staging_analytics.postgres_users;

# 4. Merge to production
lakefs api commits create \
  --repo analytics \
  --branch staging \
  --message "Validated: postgres sync" | \
lakefs api commits merge \
  --repo analytics \
  --sourceRef staging \
  --destinationRef main
```

#### Zero-Copy Branching

```bash
# Create isolated branch for experiments
lakefs refs branch create \
  --repo analytics \
  --branch-id experiment-2025-01 \
  --source main

# Data is not copied; files are versioned
# Same physical files, different logical views

# Merge back (or discard) without extra data movement
lakefs refs branch delete --repo analytics --branch-id experiment-2025-01
```

### 3. Dagster Orchestration Patterns

#### Job Structure

```python
from dagster import job, op, Field, String, DependencyDefinition
from dagster_shell import execute_shell_command

@op(config_schema={"database": Field(String)})
def discover_tables(context):
    """Discover schema from source database"""
    db = context.op_config["database"]
    execute_shell_command(
        f"olake discover --config /etc/olake/{db}_source.json > /tmp/{db}_streams.json"
    )
    return f"/tmp/{db}_streams.json"

@op(config_schema={"database": Field(String), "streams": Field(String)})
def sync_data(context, streams):
    """Replicate data to Iceberg"""
    db = context.op_config["database"]
    execute_shell_command(
        f"olake sync --config /etc/olake/{db}_source.json " +
        f"--catalog {streams} " +
        f"--destination /etc/olake/{db}_destination.json"
    )
    return {"status": "completed", "database": db}

@op
def validate_sync(context, sync_result):
    """Validate replicated data"""
    # Query Iceberg to verify row counts, checksums
    pass

@job
def postgres_to_iceberg():
    streams = discover_tables()
    result = sync_data(streams=streams)
    validate_sync(sync_result=result)
```

#### Scheduling

```python
from dagster_cron import build_schedule_from_cron_expression

postgres_sync_schedule = build_schedule_from_cron_expression(
    "0 */6 * * *",  # Every 6 hours
    job_name="postgres_to_iceberg"
)

mongodb_cdc_schedule = build_schedule_from_cron_expression(
    "*/5 * * * *",  # Every 5 minutes (CDC)
    job_name="mongodb_to_iceberg"
)
```

#### Multi-Database Orchestration

```python
DATABASES = ["postgres", "mongodb", "mysql"]

for db in DATABASES:
    @job(name=f"{db}_sync")
    def sync_job():
        streams = discover_tables(database=db)
        sync_data(database=db, streams=streams)
        validate_sync()
```

### 4. RisingWave Streaming Consumption

#### Architecture

```
OLake (Batch)          RisingWave (Streaming)
└─> Iceberg Tables ──────> Materialized Views
    (Snapshots)             (Real-time)
                                  ↓
                           Analytics Queries
```

#### Setup

```sql
-- 1. Create RisingWave source from Iceberg
CREATE SOURCE iceberg_users WITH (
    connector = 'iceberg',
    catalog_type = 'rest',
    rest_catalog_url = 'http://lakekeeper:8181',
    warehouse = 's3://bucket/warehouse',
    database = 'analytics',
    table = 'postgres_users'
);

-- 2. Create materialized view for aggregations
CREATE MATERIALIZED VIEW user_stats AS
SELECT 
    DATE_TRUNC('hour', updated_at) as hour,
    COUNT(*) as user_count,
    COUNT(DISTINCT country) as countries
FROM iceberg_users
WHERE updated_at > NOW() - INTERVAL '24 hours'
GROUP BY DATE_TRUNC('hour', updated_at);

-- 3. Sink results back to Iceberg
CREATE SINK user_stats_sink INTO iceberg_sink
FROM user_stats
WITH (
    connector = 'iceberg',
    catalog_type = 'rest',
    rest_catalog_url = 'http://lakekeeper:8181',
    warehouse = 's3://bucket/warehouse',
    database = 'analytics',
    table = 'user_stats_hourly'
);
```

#### Real-time Analytics Pattern

```
MongoDB (source)
    ↓
OLake → Iceberg (batch snapshots, daily)
    ↓
RisingWave (ingest + aggregate)
    ↓
├─> Dashboards (sub-second latency)
├─> Alerts (anomaly detection)
└─> API (real-time metrics)
```

### 5. Multi-Catalog Strategies

#### Catalog Selection

```
┌─────────────────────────────────────┐
│  OLake Iceberg Writer Supports:     │
├─────────────────────────────────────┤
│ 1. REST (Lakekeeper)   – OpenSource │
│ 2. AWS Glue            – Managed    │
│ 3. JDBC                – Custom DB  │
│ 4. Hive MetaStore      – Legacy     │
└─────────────────────────────────────┘
```

#### Configuration Examples

**REST Catalog (Lakekeeper)**
```json
{
  "catalog_type": "rest",
  "rest_catalog_url": "http://lakekeeper:8181",
  "warehouse": "s3://bucket/warehouse"
}
```

**AWS Glue Catalog**
```json
{
  "catalog_type": "glue",
  "aws_region": "us-east-1",
  "warehouse": "s3://bucket/warehouse",
  "aws_access_key": "${AWS_ACCESS_KEY}",
  "aws_secret_key": "${AWS_SECRET_KEY}"
}
```

**JDBC Catalog (Custom DB)**
```json
{
  "catalog_type": "jdbc",
  "jdbc_url": "jdbc:postgresql://postgres:5432/iceberg_catalog",
  "jdbc_user": "iceberg_user",
  "jdbc_password": "${JDBC_PASSWORD}",
  "warehouse": "s3://bucket/warehouse"
}
```

**Hive MetaStore**
```json
{
  "catalog_type": "hive",
  "hive_metastore_uri": "thrift://hive:9083",
  "warehouse": "s3://bucket/warehouse"
}
```

#### Multi-Catalog Failover

```python
# Dynamically select catalog based on availability
CATALOGS = [
    {
        "name": "lakekeeper",
        "config": {"catalog_type": "rest", ...},
        "priority": 1
    },
    {
        "name": "glue",
        "config": {"catalog_type": "glue", ...},
        "priority": 2
    },
    {
        "name": "hive",
        "config": {"catalog_type": "hive", ...},
        "priority": 3
    }
]

def get_catalog():
    for catalog in sorted(CATALOGS, key=lambda x: x['priority']):
        if catalog_healthy(catalog['name']):
            return catalog['config']
    raise Exception("All catalogs unavailable")
```

---

## Deployment Patterns

### 1. Docker Compose for Development

#### Single-Node Setup

```yaml
version: '3.8'

services:
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"

  postgres-catalog:
    image: postgres:15
    environment:
      POSTGRES_DB: lakekeeper
      POSTGRES_USER: lakekeeper
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

  lakekeeper:
    image: apache/iceberg:latest-rest
    environment:
      DATABASE_URL: postgresql://lakekeeper:${POSTGRES_PASSWORD}@postgres-catalog/lakekeeper
    ports:
      - "8181:8181"
    depends_on:
      - postgres-catalog

  olake:
    image: ghcr.io/datazip-inc/olake:latest
    volumes:
      - ./config:/etc/olake
      - ./state:/var/lib/olake
    environment:
      LAKEKEEPER_URL: http://lakekeeper:8181
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: minioadmin
      S3_SECRET_KEY: minioadmin
    command: sync --config /etc/olake/config.json
    depends_on:
      - lakekeeper
      - minio

volumes:
  postgres_data:
```

#### Configuration Files

**source.json** (PostgreSQL source):
```json
{
  "type": "postgres",
  "host": "source-postgres",
  "port": 5432,
  "database": "source_db",
  "username": "postgres",
  "password": "${PG_PASSWORD}",
  "update_method": {
    "replication_slot": "olake_slot",
    "publication": "olake_publication",
    "initial_wait_time": 120
  },
  "max_threads": 4
}
```

**destination.json** (Iceberg on MinIO):
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "rest",
    "rest_catalog_url": "http://lakekeeper:8181",
    "warehouse": "s3://lakehouse/warehouse",
    "s3_endpoint": "http://minio:9000",
    "aws_region": "us-east-1",
    "aws_access_key": "minioadmin",
    "aws_secret_key": "minioadmin",
    "iceberg_db": "source_analytics"
  }
}
```

#### Startup Commands

```bash
# Start services
docker-compose up -d

# Wait for services to be healthy
docker-compose exec postgres-catalog pg_isready -U lakekeeper
docker-compose exec lakekeeper curl -s http://localhost:8181/v1/config

# Initialize source database for CDC
docker-compose exec source-postgres psql -U postgres -d source_db << 'SQL'
  ALTER SYSTEM SET wal_level = logical;
  ALTER SYSTEM SET max_replication_slots = 4;
  SELECT pg_reload_conf();
  CREATE PUBLICATION olake_publication FOR ALL TABLES;
  SELECT pg_create_logical_replication_slot('olake_slot', 'pgoutput');
SQL

# Discover and sync
docker-compose exec olake olake discover --config /etc/olake/source.json
docker-compose exec olake olake sync --config /etc/olake/source.json \
                                      --destination /etc/olake/destination.json
```

### 2. Kubernetes Helm Deployment

#### Helm Chart Structure

```
olake-helm/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-prod.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    └── statefulset.yaml
```

#### values.yaml

```yaml
replicaCount: 3

image:
  repository: ghcr.io/datazip-inc/olake
  tag: v1.0.0
  pullPolicy: IfNotPresent

resources:
  requests:
    cpu: 1000m
    memory: 2Gi
  limits:
    cpu: 2000m
    memory: 4Gi

persistence:
  enabled: true
  size: 10Gi
  storageClass: fast-ssd

lakekeeper:
  url: http://lakekeeper:8181
  username: admin
  passwordSecret: lakekeeper-token

s3:
  endpoint: https://s3.amazonaws.com
  region: us-east-1
  bucket: data-lake

postgres:
  host: postgres.default.svc.cluster.local
  port: 5432
  database: source_db

monitoring:
  enabled: true
  prometheus: true
  interval: 30s
```

#### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: olake
spec:
  serviceName: olake
  replicas: 3
  selector:
    matchLabels:
      app: olake
  template:
    metadata:
      labels:
        app: olake
    spec:
      containers:
      - name: olake
        image: ghcr.io/datazip-inc/olake:v1.0.0
        resources:
          requests:
            cpu: "1"
            memory: "2Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
        volumeMounts:
        - name: config
          mountPath: /etc/olake
        - name: state
          mountPath: /var/lib/olake
        env:
        - name: LAKEKEEPER_URL
          valueFrom:
            configMapKeyRef:
              name: olake-config
              key: lakekeeper_url
        - name: S3_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: s3-credentials
              key: access_key
      volumes:
      - name: config
        configMap:
          name: olake-config
      volumeClaimTemplates:
      - metadata:
          name: state
        spec:
          accessModes: [ "ReadWriteOnce" ]
          storageClassName: fast-ssd
          resources:
            requests:
              storage: 10Gi
```

#### Deployment Commands

```bash
# Install Helm chart
helm repo add olake https://datazip-inc.github.io/olake-helm
helm install olake olake/olake \
  -f values-prod.yaml \
  --namespace data-platform \
  --create-namespace

# Upgrade with new configuration
helm upgrade olake olake/olake \
  -f values-prod.yaml \
  --namespace data-platform

# Monitor rollout
kubectl rollout status statefulset/olake -n data-platform

# View logs
kubectl logs -f deployment/olake-0 -n data-platform
```

### 3. CI/CD Pipeline Integration

#### GitHub Actions Workflow

```yaml
name: OLake Data Pipeline

on:
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours
  workflow_dispatch:

jobs:
  sync-postgres:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Pull OLake image
        run: docker pull ghcr.io/datazip-inc/olake:latest

      - name: Run discovery
        run: |
          docker run --rm \
            -e PG_HOST=${{ secrets.PG_HOST }} \
            -e PG_PASSWORD=${{ secrets.PG_PASSWORD }} \
            -v ${{ github.workspace }}/config:/config \
            ghcr.io/datazip-inc/olake:latest \
            discover --config /config/source.json > streams.json

      - name: Run sync
        run: |
          docker run --rm \
            -e AWS_ACCESS_KEY_ID=${{ secrets.AWS_ACCESS_KEY_ID }} \
            -e AWS_SECRET_ACCESS_KEY=${{ secrets.AWS_SECRET_ACCESS_KEY }} \
            -e LAKEKEEPER_TOKEN=${{ secrets.LAKEKEEPER_TOKEN }} \
            -v ${{ github.workspace }}/config:/config \
            ghcr.io/datazip-inc/olake:latest \
            sync --config /config/source.json \
                 --catalog streams.json \
                 --destination /config/destination.json

      - name: Validate sync
        run: |
          # Query Iceberg to verify data
          docker run --rm \
            duckdb \
            "SELECT COUNT(*) FROM read_iceberg('s3://bucket/warehouse/analytics/users')"

      - name: Notify on failure
        if: failure()
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: ${{ github.event.number }},
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: 'OLake sync failed! Check logs.'
            })
```

### 4. High Availability Setup

#### Multi-Region Deployment

```yaml
# Primary Region (us-east-1)
Primary:
  - OLake Workers (3 replicas)
  - Lakekeeper (3 replicas)
  - PostgreSQL (primary)
  - MinIO/S3 (replication enabled)

# Secondary Region (us-west-2) 
Standby:
  - OLake Workers (1 replica, ready to scale)
  - Lakekeeper (read-only)
  - PostgreSQL (replica)
  - S3 Cross-region replication

Failover Logic:
  - Monitor primary lakekeeper health
  - On failure: Switch Iceberg queries to secondary
  - OLake automatically retries to secondary S3
```

#### Health Checks

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  exec:
    command:
    - /bin/sh
    - -c
    - "curl -f http://localhost:8080/ready || exit 1"
  initialDelaySeconds: 5
  periodSeconds: 5

startupProbe:
  exec:
    command:
    - /bin/sh
    - -c
    - "olake check --config /etc/olake/config.json"
  failureThreshold: 30
  periodSeconds: 2
```

### 5. Monitoring with Prometheus/Grafana

#### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 30s

scrape_configs:
  - job_name: 'olake'
    static_configs:
      - targets: ['olake:9090']
    metrics_path: '/metrics'

  - job_name: 'lakekeeper'
    static_configs:
      - targets: ['lakekeeper:8181']

  - job_name: 'iceberg'
    static_configs:
      - targets: ['iceberg-exporter:9091']
```

#### Key Metrics

```
# OLake specific
olake_sync_duration_seconds{database="postgres", status="success"}
olake_records_processed_total{source="mongodb", sink="iceberg"}
olake_errors_total{type="network_timeout", database="mysql"}
olake_checkpoint_lag_seconds{stream="postgres.public.users"}
olake_file_count_total{table="users", status="small"}

# Iceberg specific
iceberg_snapshots_total{table="analytics.users"}
iceberg_manifest_files{table="analytics.users"}
iceberg_table_size_bytes{table="analytics.users"}
iceberg_query_latency_ms{catalog="lakekeeper"}

# S3 specific
s3_put_object_duration_seconds{bucket="data-lake"}
s3_list_objects_duration_seconds{bucket="data-lake"}
s3_get_object_errors_total{bucket="data-lake"}
```

#### Grafana Dashboard JSON

```json
{
  "dashboard": {
    "title": "OLake Replication",
    "panels": [
      {
        "title": "Records/sec",
        "targets": [
          {
            "expr": "rate(olake_records_processed_total[5m])"
          }
        ]
      },
      {
        "title": "Checkpoint Lag",
        "targets": [
          {
            "expr": "olake_checkpoint_lag_seconds"
          }
        ]
      },
      {
        "title": "Small Files Count",
        "targets": [
          {
            "expr": "olake_file_count_total{status='small'}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(olake_errors_total[5m])"
          }
        ]
      }
    ]
  }
}
```

---

## Best Practices

### 1. Partitioning Strategies

#### Table Design

```json
{
  "streams": [
    {
      "name": "users",
      "partitioning": {
        "columns": ["country", "created_date"],
        "strategy": "date_range"
      }
    }
  ]
}
```

**Partitioning Strategy Selection:**

| Strategy | Best For | Example |
|----------|----------|---------|
| Time-based | Historical data, time-series | `PARTITION BY year, month` |
| Categorical | Fixed categories | `PARTITION BY region, product_type` |
| Range | Numeric ranges | `PARTITION BY (id % 100)` |
| Hybrid | Large tables with time + category | `PARTITION BY year, country` |

#### Configuration Example

```json
{
  "writer": {
    "partitioning": {
      "type": "identity",
      "columns": [
        {
          "name": "country",
          "type": "identity"
        },
        {
          "name": "created_date",
          "type": "day"  // Bucketed by day
        }
      ]
    }
  }
}
```

### 2. Compaction Schedules

#### The Small File Problem

```
Issue: CDC writes small files
Result: Slow queries, high metadata overhead
Solution: Compact files regularly

OLake writes:
├── Batch 1: 1.2 MB
├── Batch 2: 0.8 MB  
├── Batch 3: 1.5 MB  
└── Total: 3 files < 5 MB (inefficient!)

After compaction:
└── Compacted: 3.5 MB (1 file, query 3x faster)
```

#### Compaction Strategy

```python
# Dagster job for periodic compaction
@op
def compact_iceberg_tables():
    """Compact small files in Iceberg tables"""
    from pyspark.sql import SparkSession
    
    spark = SparkSession.builder.appName("iceberg-compaction").getOrCreate()
    
    # Compact tables with many small files
    tables = [
        "analytics.postgres_users",
        "analytics.mongodb_events",
        "analytics.mysql_orders"
    ]
    
    for table in tables:
        df = spark.read.format("iceberg").load(table)
        
        # Get file statistics
        files_df = spark.sql(f"SELECT * FROM {table}.files")
        small_files = files_df.filter("file_size_in_bytes < 134217728").count()
        
        if small_files > 5:
            # Trigger compaction
            spark.sql(f"""
                CALL system.rewrite_data_files('{table}')
            """)
            
            context.log.info(f"Compacted {table}: {small_files} files")

@schedule(
    job_name="compact_iceberg_tables",
    cron_schedule="0 2 * * *"  # Daily at 2 AM
)
def daily_compaction():
    pass
```

#### Monitoring Small Files

```sql
-- Find tables with compaction needed
SELECT 
    table_name,
    COUNT(*) as file_count,
    SUM(file_size_in_bytes) as total_size,
    AVG(file_size_in_bytes) as avg_size,
    COUNT(CASE WHEN file_size_in_bytes < 134217728 THEN 1 END) as small_files
FROM iceberg.analytics.files_v1
WHERE table_name IN (
    'postgres_users', 
    'mongodb_events',
    'mysql_orders'
)
GROUP BY table_name
HAVING small_files > 5
ORDER BY small_files DESC;
```

### 3. Security Patterns

#### Secrets Management

```bash
# Use environment variables (not in config files!)
export PG_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id postgres/password \
  --query SecretString --output text)

export S3_ACCESS_KEY=$(aws secretsmanager get-secret-value \
  --secret-id s3/access-key \
  --query SecretString --output text)

export LAKEKEEPER_TOKEN=$(aws secretsmanager get-secret-value \
  --secret-id lakekeeper/token \
  --query SecretString --output text)

# Configure OLake with secrets from environment
olake sync --config /etc/olake/config.json
```

#### Encryption in Transit

```json
{
  "writer": {
    "catalog_type": "rest",
    "rest_catalog_url": "https://lakekeeper.example.com",  // HTTPS
    "s3_endpoint": "https://s3.amazonaws.com",             // HTTPS
    "ssl_verify": true
  }
}
```

#### Data Encryption at Rest

```bash
# S3 Server-Side Encryption
aws s3api put-bucket-encryption \
  --bucket data-lake \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "aws:kms",
        "KMSMasterKeyID": "arn:aws:kms:us-east-1:123456789:key/..."
      }
    }]
  }'

# PostgreSQL source encryption
psql -h postgres.example.com -U postgres -d source_db << 'SQL'
-- Enable SSL for all connections
ALTER SYSTEM SET ssl = on;
ALTER SYSTEM SET ssl_cert_file = '/etc/ssl/certs/server.crt';
ALTER SYSTEM SET ssl_key_file = '/etc/ssl/private/server.key';
SELECT pg_reload_conf();
SQL
```

#### Access Control

```yaml
# Kubernetes RBAC for OLake service account
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: olake-sync
rules:
  - apiGroups: [""]
    resources: ["configmaps"]
    verbs: ["get", "list"]
  - apiGroups: [""]
    resources: ["secrets"]
    verbs: ["get"]  # Not "list" for security

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: olake-sync
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: olake-sync
subjects:
  - kind: ServiceAccount
    name: olake
```

### 4. Performance Tuning

#### Parallel Processing Configuration

```json
{
  "max_threads": 8,
  "chunk_size": 100000,
  "batch_size": 5000,
  "writer": {
    "parallel_uploads": 4,
    "buffer_size_mb": 256
  }
}
```

**Parameter Tuning Guide:**

```
max_threads:
  ├─ Too low (<2)  → Underutilizes resources
  ├─ Optimal (4-8) → Good balance for most workloads
  └─ Too high (>16)→ Excessive context switching, DB stress

chunk_size:
  ├─ Small (10K)   → More chunks, better resume capability
  ├─ Medium (100K) → Balanced
  └─ Large (1M)    → Fewer chunks, higher latency

batch_size:
  ├─ Small (1K)    → Frequent writes, high metadata overhead
  ├─ Medium (5K)   → Balanced
  └─ Large (50K)   → Fewer writes, risk of OOM
```

#### Memory Optimization

```yaml
# Kubernetes resource optimization
resources:
  requests:
    cpu: 1000m        # 1 core
    memory: 2Gi
  limits:
    cpu: 2000m        # Max 2 cores
    memory: 4Gi

# Configure for large tables
env:
  - name: GOMAXPROCS
    value: "2"        # Limit Go runtime threads
  - name: GOMEMLIMIT
    value: "3500M"    # Heap limit (slightly below container limit)
```

#### Throughput Monitoring

```sql
-- Monitor replication throughput
SELECT
  stream_id,
  COUNT(*) as records_processed,
  EXTRACT(EPOCH FROM MAX(checkpoint_time) - MIN(checkpoint_time)) as duration_sec,
  ROUND(COUNT(*) / NULLIF(
    EXTRACT(EPOCH FROM MAX(checkpoint_time) - MIN(checkpoint_time)), 
    0), 2) as records_per_sec
FROM olake_sync_history
WHERE checkpoint_time > NOW() - INTERVAL '1 hour'
GROUP BY stream_id
ORDER BY records_per_sec DESC;
```

### 5. Troubleshooting Common Issues

#### Issue: Checkpoint Lag Growing

```bash
# Check current lag
kubectl exec -it olake-0 -n data-platform -- \
  curl localhost:9090/metrics | grep checkpoint_lag

# Diagnosis
kubectl logs olake-0 -n data-platform | grep -i "lag\|slow\|error"

# Solution: Increase parallelism
kubectl patch statefulset olake -n data-platform --type='json' -p='[
  {"op": "replace", "path": "/spec/template/spec/containers/0/env", "value": [
    {"name": "MAX_THREADS", "value": "16"}
  ]}
]'
```

#### Issue: Too Many Small Files

```bash
# Identify tables with small file problem
docker run --rm duckdb << 'SQL'
SELECT 
  table_name,
  COUNT(*) as file_count,
  ROUND(AVG(file_size_in_bytes) / 1048576, 2) as avg_size_mb
FROM read_iceberg('s3://bucket/warehouse/*/*/files_v1')
GROUP BY table_name
HAVING COUNT(*) > 10 AND AVG(file_size_in_bytes) < 134217728
ORDER BY file_count DESC;
SQL

# Trigger compaction immediately
docker run --rm \
  -e SPARK_SUBMIT_ARGS="--driver-memory 4G" \
  spark:latest \
  spark-submit --class org.apache.iceberg.spark.procedures.RemoveOrphanFilesAction \
    /opt/spark/jars/iceberg-spark-runtime.jar \
    s3://bucket/warehouse/analytics/users
```

#### Issue: Memory Exhaustion

```bash
# Check memory usage
kubectl top pod olake-0 -n data-platform

# Get heap dump
kubectl exec olake-0 -n data-platform -- \
  curl -X POST localhost:6060/debug/pprof/heap > heap.dump

# Analyze (locally)
go tool pprof heap.dump
(pprof) top10
```

#### Issue: Connection Pool Exhaustion

```json
{
  "source": {
    "max_connections": 10,
    "connection_timeout_ms": 30000,
    "idle_timeout_ms": 600000
  }
}
```

```sql
-- Monitor source DB connections
-- PostgreSQL
SELECT count(*) as connection_count 
FROM pg_stat_activity 
WHERE application_name = 'olake';

-- MySQL
SHOW PROCESSLIST;
```

---

## Reference Documentation

- **OLake Docs**: https://olake.io/docs
- **Apache Iceberg**: https://iceberg.apache.org/
- **Lakekeeper**: https://docs.lakekeeper.io/
- **LakeFS**: https://docs.lakefs.io/
- **Kubernetes Helm**: https://helm.sh/docs/

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**OLake Version**: v1.0+
