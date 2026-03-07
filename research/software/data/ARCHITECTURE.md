# Data Stack Architecture Reference

## Quick Navigation

This is the primary architecture reference for the data-unified platform. Consolidated from 19 source documents.

**Related Documents:**
- [SCHEMAS_AND_TYPES.md](./SCHEMAS_AND_TYPES.md) - BAML, Pydantic, Zod type system
- [AI_MEMORY.md](./AI_MEMORY.md) - Agno, Cognee, CocoIndex agent integrations
- [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Step-by-step implementation patterns

---

## Table of Contents

1. [Overview & Key Principles](#overview--key-principles)
2. [Lakehouse Architecture](#lakehouse-architecture)
   - [DuckLake: SQL-Based Table Format](#ducklake-sql-based-table-format)
   - [Apache Iceberg & Lakekeeper](#apache-iceberg--lakekeeper)
   - [LakeFS: Version Control for Data](#lakefs-version-control-for-data)
   - [OLake: CDC to Lakehouse](#olake-cdc-to-lakehouse)
   - [Integration Patterns](#lakehouse-integration-patterns)
3. [Streaming & Real-Time Processing](#streaming--real-time-processing)
   - [RisingWave Architecture](#risingwave-architecture)
   - [Materialized Views & Feature Engineering](#materialized-views--feature-engineering)
   - [Stream-Batch Unification with Ibis](#stream-batch-unification-with-ibis)
4. [Orchestration & Workflow](#orchestration--workflow)
   - [Dagster: Data Pipeline Orchestration](#dagster-data-pipeline-orchestration)
   - [DLT Integration Patterns](#dlt-integration-patterns)
   - [SQLMesh Integration](#sqlmesh-integration)
   - [Event-Driven Architectures](#event-driven-architectures)
5. [Data Ingestion](#data-ingestion)
   - [DLT: Data Load Tool](#dlt-data-load-tool)
   - [Crawl4AI: AI-Native Web Scraping](#crawl4ai-ai-native-web-scraping)
   - [Git Sparse-Checkout for Code Reindexing](#git-sparse-checkout-for-code-reindexing)
6. [Transformation & Semantic Layers](#transformation--semantic-layers)
   - [SQLMesh: SQL-Based Transformations](#sqlmesh-sql-based-transformations)
   - [Ibis: Portable Dataframe API](#ibis-portable-dataframe-api)
7. [Incremental Processing Patterns](#incremental-processing-patterns)
   - [CocoIndex: Incremental Indexing](#cocoindex-incremental-indexing)
   - [DLT State Management](#dlt-state-management)
8. [Modern Data Stack Integration](#modern-data-stack-integration)
   - [Vector Databases: LanceDB](#vector-databases-lancedb)
   - [Knowledge Graphs: Cognee & Memgraph](#knowledge-graphs-cognee--memgraph)
   - [DuckDB Analytics](#duckdb-analytics)
   - [Cloudflare R2 Storage](#cloudflare-r2-storage)
9. [Architecture Diagrams](#architecture-diagrams)
10. [Best Practices](#best-practices)

---

## Overview & Key Principles

Modern data pipelines require integrating multiple specialized systems to handle batch processing, real-time streams, analytics, and AI workloads. This architecture unifies:

- **Lakehouse formats** (DuckLake, Iceberg) for ACID transactions on object storage
- **Streaming systems** (RisingWave) for real-time materialized views
- **Orchestration** (Dagster) for workflow management
- **Ingestion tools** (DLT, Crawl4AI) for data acquisition
- **Transformation layers** (SQLMesh, Ibis) for portable analytics
- **Incremental processing** (CocoIndex) for efficient updates
- **AI/ML integration** (LanceDB, Cognee) for semantic search and knowledge graphs

### Key Design Principles

1. **Separation of Storage and Compute**: Use object storage (S3, R2) with query engines that read data in-situ
2. **Open Standards**: Parquet, Iceberg, Arrow for interoperability
3. **Incremental Processing**: Only process changed data to minimize waste
4. **Unified API**: Single interface (Ibis) for batch and streaming
5. **Schema Evolution**: Automatic handling of schema changes
6. **Multi-Tenancy**: Built-in isolation for organizations
7. **Type Safety**: BAML → Pydantic/Zod code generation for cross-language consistency

### 6-Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 1: Intelligent Ingestion                                         │
│  ├─ DLT (API data with incremental cursors)                            │
│  ├─ Git Sparse-Checkout (selective repository cloning)                 │
│  ├─ Repomix (AI-friendly codebase packaging)                           │
│  └─ Crawl4AI (documentation scraping with LLM extraction)              │
└─────────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 2: Storage & Cataloging                                         │
│  ├─ DuckLake (versioned Parquet + PostgreSQL catalog)                 │
│  └─ PostgreSQL (CocoIndex state, Feast catalog, MLflow tracking)      │
└─────────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 3: Transformation & Enrichment                                   │
│  ├─ SQLMesh (interval-based incremental models)                        │
│  ├─ Ibis (post-load transformations)                                   │
│  └─ CocoIndex (AI-powered transformations + embeddings)                │
└─────────────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────┴─────────┐
                    ↓                   ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│  Layer 4a: Semantic      │  │  Layer 4b: Feature Store │
│  ├─ CocoIndex (vectors)  │  │  ├─ Feast (offline:      │
│  ├─ LanceDB (ANN search) │  │  │    DuckDB)             │
│  └─ pgvector (Postgres)  │  │  └─ DragonflyDB (online) │
└──────────────────────────┘  └──────────────────────────┘
                    ↓                   ↓
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 5: ML & Analytics                                                │
│  ├─ MLflow (experiment tracking, model registry)                       │
│  ├─ Agno + BAML (LLM-powered analysis agents)                         │
│  ├─ RisingWave (real-time materialized views)                         │
│  └─ Custom Models (trained on Feast features)                         │
└─────────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 6: Orchestration & Observability                                │
│  ├─ Dagster (asset-based workflows, sensors, schedules)               │
│  ├─ OpenAI Integration (native LLM calls in pipelines)                │
│  └─ CocoInsight (data lineage visualization)                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Lakehouse Architecture

### DuckLake: SQL-Based Table Format

DuckLake is an open table format that uses a SQL database for metadata and Parquet files for data storage. Unlike Iceberg/Delta which use JSON manifest files, DuckLake leverages existing SQL databases (PostgreSQL, MySQL, or DuckDB itself) as the catalog.

**Architecture:**
```
┌─────────────────┐      ┌──────────────────┐
│  SQL Catalog    │◄─────│   DuckDB/App     │
│  (Postgres/     │      │   Client         │
│   DuckDB)       │      └──────────────────┘
└─────────────────┘               │
        │                         │
        │ Metadata                │ Read/Write
        │ (schemas, versions)     │ Parquet
        ▼                         ▼
┌─────────────────────────────────────┐
│   Object Storage (S3/R2)            │
│   └── Parquet Files                 │
└─────────────────────────────────────┘
```

**Key Features:**
- **ACID Transactions**: Snapshot isolation via SQL database
- **Time Travel**: Query historical versions of data
- **Schema Evolution**: Add/modify columns without rewriting data
- **Multi-User**: Concurrent reads/writes with optimistic locking
- **Lightweight**: No heavy services required (unlike Hive metastore)

**Configuration Example:**
```python
import duckdb

# Attach DuckLake catalog with Postgres metadata
con = duckdb.connect()
con.execute("""
    ATTACH 'ducklake:my_catalog.ducklake' AS catalog
    (TYPE POSTGRES,
     HOST 'localhost',
     DATABASE 'catalog_db',
     STORAGE_PATH 's3://my-bucket/data');
""")

# Use DuckLake catalog
con.execute("USE catalog;")

# Create table (data goes to S3, metadata to Postgres)
con.execute("""
    CREATE TABLE events AS
    SELECT * FROM read_parquet('source.parquet');
""")
```

### Apache Iceberg & Lakekeeper

Apache Iceberg is a table format designed for large analytic datasets with strong consistency guarantees. **Lakekeeper** is an open-source Iceberg catalog service (REST API) written in Rust.

**Architecture:**
```
┌────────────────────┐
│   Query Engines    │  Trino, Spark, DuckDB, etc.
│  (via Iceberg API) │
└──────────┬─────────┘
           │
           ▼
┌────────────────────┐
│   Lakekeeper       │  REST Catalog Service
│   (Rust-based)     │  - Metadata management
│                    │  - OIDC auth, OPA policies
└──────────┬─────────┘  - Change event streams
           │
           │ Metadata reads/writes
           ▼
┌────────────────────┐
│   PostgreSQL       │  Stores table metadata
│   Metadata Store   │  (snapshots, schemas, partitions)
└────────────────────┘
           │
           │ Points to data files
           ▼
┌────────────────────────────────┐
│   Object Storage (S3/R2)       │
│   └── Parquet/ORC Files        │
└────────────────────────────────┘
```

**Lakekeeper Features:**
- **REST Catalog API**: Standard Iceberg catalog interface
- **Governance**: Fine-grained access control, OIDC integration
- **Event Streams**: Emit change events on table modifications
- **Multi-Engine**: Works with Trino, Spark, Flink, DuckDB, ClickHouse

### LakeFS: Version Control for Data

LakeFS provides Git-like version control for data lakes, enabling branching, commits, and rollbacks at the storage layer.

**Architecture:**
```
┌─────────────────────────────────────┐
│   Applications / Query Engines      │
└──────────────┬──────────────────────┘
               │
               │ S3 API calls
               ▼
┌─────────────────────────────────────┐
│          LakeFS Server              │
│  ┌─────────────────────────────┐   │
│  │  Version Control Layer       │   │
│  │  - Branches (main, dev)      │   │
│  │  - Commits, Tags             │   │
│  │  - Zero-copy branching       │   │
│  └─────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │
               │ Reads/writes actual data
               ▼
┌─────────────────────────────────────┐
│   Object Storage (S3/R2/GCS)        │
│   └── Data Files (Parquet, etc.)    │
└─────────────────────────────────────┘
```

**Key Capabilities:**
- **Branching**: Create isolated copies of entire data lake
- **Commits**: Atomic snapshots of repository state
- **Merging**: Merge changes from branch to main
- **Rollback**: Revert to previous commit instantly
- **Format Agnostic**: Works with any file type (Parquet, Iceberg, Delta)

### OLake: CDC to Lakehouse

OLake is an open-source data replication tool for moving operational database data into lakehouse formats.

**Supported Sources:** PostgreSQL, MySQL, MongoDB, Oracle, Kafka (WIP)
**Destination Formats:** Apache Iceberg (with Lakekeeper catalog), Raw Parquet

**Setup Example (PostgreSQL to Iceberg):**

1. **Configure PostgreSQL for logical replication:**
```sql
ALTER SYSTEM SET wal_level = 'logical';
ALTER SYSTEM SET max_replication_slots = 4;
SELECT pg_reload_conf();

CREATE PUBLICATION olake_publication
FOR ALL TABLES
WITH (publish='insert,update,delete,truncate');
```

2. **OLake destination configuration:**
```json
{
  "type": "ICEBERG",
  "writer": {
    "catalog_type": "rest",
    "rest_catalog_url": "https://lakekeeper.example.com/catalog",
    "iceberg_s3_path": "s3://my-bucket/warehouse",
    "s3_endpoint": "https://account-id.r2.cloudflarestorage.com"
  }
}
```

### Lakehouse Integration Patterns

**DuckLake vs Iceberg Decision Matrix:**

| Criteria | DuckLake | Iceberg |
|----------|----------|---------|
| Setup complexity | Lower (SQL DB) | Higher (catalog service) |
| Multi-engine support | DuckDB primarily | Trino, Spark, Flink, DuckDB |
| Schema evolution | Via SQL DDL | Rich API |
| Time travel | Yes | Yes (more mature) |
| Partitioning | Basic | Advanced (hidden partitions) |
| Best for | Small-medium datasets | Large-scale enterprise |

**Recommended Pattern:**
- Use **DuckLake** for development/prototyping and small-medium workloads
- Use **Iceberg + Lakekeeper** for production with multi-engine requirements
- Use **LakeFS** for both when data versioning is critical

---

## Streaming & Real-Time Processing

### RisingWave Architecture

RisingWave is a distributed SQL streaming database that maintains incrementally-updated materialized views.

**Core Concepts:**
```
┌──────────────────────┐
│   Data Sources       │  Kafka, PostgreSQL CDC, Kinesis, etc.
└──────────┬───────────┘
           │
           │ Ingest streams
           ▼
┌──────────────────────┐
│   RisingWave         │
│  ┌────────────────┐  │
│  │  Sources       │  │  CREATE SOURCE kafka_events ...
│  ├────────────────┤  │
│  │  Tables        │  │  CREATE TABLE events ...
│  ├────────────────┤  │
│  │  Materialized  │  │  CREATE MATERIALIZED VIEW ...
│  │  Views         │  │  (Continuously updated)
│  └────────────────┘  │
└──────────┬───────────┘
           │
           │ Sink results
           ▼
┌──────────────────────┐
│   Destinations       │  Postgres, Redis, Iceberg, Kafka
└──────────────────────┘
```

**Key Features:**
- **PostgreSQL Compatible**: Use psql client and Postgres drivers
- **Incremental Computation**: Only processes changed data
- **Exactly-Once Semantics**: Consistent results even with failures
- **High Throughput**: 10M+ events/second

**Example: Real-Time User Features**

```sql
-- Create source from PostgreSQL CDC
CREATE SOURCE postgres_orders
WITH (
  connector = 'postgres-cdc',
  hostname = 'db.example.com',
  database.name = 'ecommerce',
  slot.name = 'rw_slot'
);

-- Create materialized view for real-time aggregation
CREATE MATERIALIZED VIEW user_spending_recent AS
SELECT
  user_id,
  DATE_TRUNC('month', created_at) AS month,
  SUM(total_amount) AS total_spending,
  COUNT(id) AS order_count
FROM postgres_orders
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '2 months'
GROUP BY user_id, DATE_TRUNC('month', created_at);

-- Sink to Iceberg for long-term storage
CREATE SINK user_spending_iceberg
FROM user_spending_recent
WITH (
  connector = 'iceberg',
  type = 'upsert',
  warehouse.path = 's3://my-bucket/warehouse',
  catalog.type = 'rest',
  catalog.uri = 'https://lakekeeper.example.com/catalog'
);
```

### Stream-Batch Unification with Ibis

Ibis provides a unified API for both batch and streaming data processing.

```python
import ibis

# Same code works on DuckDB (batch) or RisingWave (stream)
def compute_user_metrics(backend):
    """Compute user spending metrics (works on any backend)"""
    orders = backend.table('orders')

    return (
        orders
        .filter(orders.created_at >= ibis.now() - ibis.interval(months=2))
        .group_by([
            orders.user_id,
            orders.created_at.truncate('month').name('month')
        ])
        .aggregate(
            total_spending=orders.total_amount.sum(),
            order_count=orders.count()
        )
    )

# Test on DuckDB (batch)
duckdb_con = ibis.duckdb.connect('data.duckdb')
batch_result = compute_user_metrics(duckdb_con).execute()

# Deploy to RisingWave (stream)
rw_con = ibis.risingwave.connect(host='risingwave.example.com')
rw_con.create_materialized_view('user_spending_stream', compute_user_metrics(rw_con))
```

---

## Orchestration & Workflow

### Dagster: Data Pipeline Orchestration

Dagster orchestrates complex data pipelines with software-defined assets.

**Core Concepts:**
- **Assets**: Data products with lineage tracking
- **Jobs**: Workflows composed of operations
- **Sensors**: Event-driven triggers
- **Schedules**: Time-based execution
- **Resources**: Shared connections and configurations

**Example: PostgreSQL Backup to Iceberg**

```python
import dagster as dg
from dagster import asset, Definitions

@asset
def postgres_snapshot(context):
    """Trigger OLake to snapshot Postgres to Iceberg"""
    result = subprocess.run([
        'docker', 'run', '--rm',
        'olakego/source-postgres:latest',
        'sync', '--config', '/config/source.json'
    ], capture_output=True)

    if result.returncode != 0:
        raise Exception(f"OLake sync failed: {result.stderr}")

    context.log.info(f"Synced Postgres to Iceberg")
    return {"status": "success"}

@asset(deps=[postgres_snapshot])
def iceberg_table_stats(context):
    """Compute statistics on Iceberg tables"""
    import duckdb
    con = duckdb.connect()
    con.execute("INSTALL iceberg; LOAD iceberg;")

    stats = con.execute("""
        SELECT COUNT(*) as row_count
        FROM iceberg_scan('s3://bucket/warehouse/users');
    """).fetchone()

    return dict(row_count=stats[0])

defs = Definitions(
    assets=[postgres_snapshot, iceberg_table_stats],
    schedules=[
        dg.ScheduleDefinition(
            job=dg.define_asset_job("backup_job"),
            cron_schedule="0 2 * * *"  # 2 AM daily
        )
    ]
)
```

### DLT Integration Patterns

**Dagster + DLT:**
```python
from dagster import asset
import dlt

@asset
def github_data(context):
    """Load GitHub issues with DLT"""
    pipeline = dlt.pipeline(
        pipeline_name="github_data",
        destination="duckdb",
        dataset_name="github_analytics"
    )

    @dlt.resource(write_disposition="merge", primary_key="id")
    def issues():
        response = requests.get("https://api.github.com/repos/owner/repo/issues")
        yield response.json()

    load_info = pipeline.run(issues())
    context.log.info(f"Loaded {load_info.loads[0].row_counts} rows")
    return load_info
```

**DLT + SQLMesh:**
```bash
# Generate SQLMesh project from DLT pipeline
sqlmesh generate-from-dlt --pipeline my_pipeline --output-dir models/
```

### Event-Driven Architectures

**Sensor Pattern for Change Detection:**

```python
from dagster import sensor, RunRequest, SkipReason

@sensor(job=reindex_job)
def github_repo_updated_sensor(context):
    """Trigger reindex only when repo has new commits"""
    response = requests.get(f"https://api.github.com/repos/{repo}/branches/main")
    latest_sha = response.json()['commit']['sha']

    if latest_sha != context.cursor:
        context.update_cursor(latest_sha)
        return RunRequest(run_key=f"commit_{latest_sha}")

    return SkipReason("No new commits")
```

---

## Data Ingestion

### DLT: Data Load Tool

DLT is a Python library for declarative data ingestion with automatic schema management.

**Core Features:**
- Schema inference and evolution
- Incremental loading with state management
- Normalization of nested JSON
- Multiple destinations (DuckDB, Postgres, BigQuery, filesystem)

**Writing to Cloudflare R2:**
```python
# .dlt/secrets.toml
# [destination.filesystem.credentials]
# aws_access_key_id = "<R2_ACCESS_KEY>"
# endpoint_url = "https://<account-id>.r2.cloudflarestorage.com"

pipeline = dlt.pipeline(
    pipeline_name="data_lake",
    destination="filesystem",
    dataset_name="raw_data"
)

pipeline.run(data_source, loader_file_format="parquet")
```

### Crawl4AI: AI-Native Web Scraping

Crawl4AI extracts clean, LLM-ready content from websites.

**Features:**
- JavaScript rendering (Playwright-based)
- Session management for authenticated scraping
- Clean Markdown output
- Structured extraction with LLMs or CSS selectors

**Integration with DLT:**
```python
import dlt
from crawl4ai import AsyncWebCrawler

@dlt.resource(name="scraped_articles")
async def scrape_website():
    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(url="https://blog.example.com")
        if result.status.is_success():
            yield result.extracted_data

pipeline = dlt.pipeline("web_scraping", destination="duckdb")
asyncio.run(pipeline.run(scrape_website()))
```

### Git Sparse-Checkout for Code Reindexing

```python
@asset
def sparse_checkout_repo(context):
    """Clone repo with sparse-checkout for specific paths"""
    repo_path = "/tmp/repo"
    target_path = "src/api"

    if not os.path.exists(repo_path):
        subprocess.run([
            'git', 'clone', '--filter=blob:none', '--sparse',
            'https://github.com/org/repo.git', repo_path
        ])
        subprocess.run(
            ['git', 'sparse-checkout', 'set', target_path],
            cwd=repo_path
        )
    else:
        subprocess.run(['git', 'pull'], cwd=repo_path)

    return {"path": repo_path}
```

---

## Transformation & Semantic Layers

### SQLMesh: SQL-Based Transformations

SQLMesh provides version-controlled SQL models with automatic incremental detection.

```sql
-- models/user_metrics.sql
MODEL (
  name user_metrics,
  kind INCREMENTAL_BY_TIME_RANGE (time_column created_at),
  grain user_id
);

SELECT
  user_id,
  DATE_TRUNC('month', created_at) AS month,
  SUM(amount) AS total_spent
FROM raw.transactions
WHERE created_at BETWEEN @start_date AND @end_date
GROUP BY user_id, DATE_TRUNC('month', created_at);
```

### Ibis: Portable Dataframe API

**Supported Backends:** DuckDB, PostgreSQL, RisingWave, Spark, BigQuery, Snowflake, +20 more

```python
import ibis

con = ibis.duckdb.connect('analytics.duckdb')
orders = con.table('orders')
users = con.table('users')

user_stats = (
    orders
    .join(users, orders.user_id == users.id)
    .group_by(users.country)
    .aggregate(
        total_orders=orders.count(),
        total_revenue=orders.amount.sum()
    )
)

# Same code works on RisingWave for streaming
rw_con = ibis.risingwave.connect(host='risingwave.example.com')
rw_con.create_materialized_view('user_stats_live', user_stats)
```

---

## Incremental Processing Patterns

### CocoIndex: Incremental Indexing

CocoIndex is a Rust-based ETL framework for building AI indexes with incremental updates.

```python
import cocoindex
from cocoindex.sources import LocalFile
from cocoindex.functions import SplitRecursively, SentenceTransformerEmbed

@cocoindex.flow_def(name="code_index")
def build_code_index(flow_builder, data_scope):
    data_scope["files"] = flow_builder.add_source(
        LocalFile(
            path="/path/to/repo",
            included_patterns=["*.py", "*.js", "*.md"]
        )
    )

    with data_scope["files"].row() as file:
        file["chunks"] = file["content"].transform(
            SplitRecursively(),
            chunk_size=1000
        )

        with file["chunks"].row() as chunk:
            chunk["embedding"] = chunk["text"].transform(
                SentenceTransformerEmbed(model="all-MiniLM-L6-v2")
            )

            data_scope.add_collector("code_vectors").collect(
                filename=file["filename"],
                chunk_text=chunk["text"],
                embedding=chunk["embedding"]
            )

    # Export to vector database
    data_scope.get_collector("code_vectors").export(
        "code_search_index",
        cocoindex.storages.Postgres(),
        vector_indexes=[
            cocoindex.VectorIndex("embedding", cocoindex.VectorSimilarityMetric.COSINE_SIMILARITY)
        ]
    )

# Build index (only new/changed files processed on subsequent runs)
flow = build_code_index()
flow.run()
```

### DLT State Management

```python
import dlt

@dlt.resource(write_disposition="append")
def incremental_load(
    updated_at=dlt.sources.incremental('updated_at', initial_value='2024-01-01')
):
    """Only load records updated since last run"""
    query = f"SELECT * FROM table WHERE updated_at > '{updated_at.last_value}'"
    for batch in fetch_batches(query):
        yield batch

pipeline = dlt.pipeline("incremental", destination="duckdb")
pipeline.run(incremental_load())  # Subsequent runs only fetch new data
```

---

## Modern Data Stack Integration

### Vector Databases: LanceDB

```python
import lancedb

db = lancedb.connect("/path/to/lancedb")

data = [
    {"id": 1, "text": "Machine learning is AI", "vector": [...], "metadata": {"category": "AI"}},
    {"id": 2, "text": "Deep learning uses neural networks", "vector": [...], "metadata": {"category": "AI"}}
]

table = db.create_table("documents", data)
table.create_index(metric="cosine", num_partitions=256)

# Semantic search
results = table.search(query_vector).metric("cosine").limit(5).to_pandas()
```

### Knowledge Graphs: Cognee & Memgraph

```python
import cognee

cognee.config.set_vector_db({"provider": "lancedb", "path": "/data/vectors"})
cognee.config.set_graph_db({"provider": "memgraph", "host": "memgraph.example.com"})

cognee.add_data_points([
    "Contract ABC signed between Company A and Company B.",
    "Force majeure clause allows termination in case of pandemic."
])
cognee.cognify()

result = cognee.search("Which contracts have force majeure clauses?", search_type="knowledge_graph")
```

### Cloudflare R2 Storage

```python
import duckdb

con = duckdb.connect()
con.execute("INSTALL httpfs; LOAD httpfs;")

# Configure R2 access
con.execute("""
    CREATE SECRET cloudflare_r2 (
        TYPE S3,
        KEY_ID '<ACCESS_KEY>',
        SECRET '<SECRET_KEY>',
        ENDPOINT 'https://<account>.r2.cloudflarestorage.com',
        REGION 'auto'
    );
""")

# Query Parquet files from R2
result = con.execute("SELECT * FROM read_parquet('s3://bucket/data/*.parquet')").fetchdf()
```

---

## Architecture Diagrams

### End-to-End Data Pipeline

```
┌─────────────────────────────────────────────────────┐
│              Data Sources                           │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐           │
│  │ Web APIs│  │PostgreSQL│  │  Files  │           │
│  └────┬────┘  └─────┬────┘  └────┬────┘           │
└───────┼────────────┼─────────────┼─────────────────┘
        │            │             │
        │  Crawl4AI  │   OLake     │   DLT
        ├────────────┴─────────────┴────────┐
        │                                    │
        ▼                                    ▼
┌────────────────────────┐      ┌──────────────────────┐
│   Dagster Pipeline     │      │  RisingWave Stream   │
│   Orchestration        │◄────►│  Processing          │
└───────────┬────────────┘      └──────────┬───────────┘
            │                              │
    ┌───────┴───────┐              ┌──────┴────────┐
    ▼               ▼              ▼               ▼
┌─────────┐   ┌──────────┐   ┌────────┐   ┌──────────┐
│ DuckLake│   │ Iceberg  │   │LanceDB │   │Memgraph  │
│(DuckDB) │   │(Lakekeeper)  │(Vectors)│  │ (Graph)  │
└────┬────┘   └─────┬────┘   └────┬───┘   └─────┬────┘
     │              │             │              │
     └──────────────┴─────────────┴──────────────┘
                    │
                    ▼
            ┌────────────────┐
            │ Object Storage │
            │  (R2/S3)       │
            └────────────────┘
```

### Lakehouse Architecture

```
┌────────────────────────────────────────────┐
│         LakeFS (Version Control)           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │   main   │  │   dev    │  │   test   │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼─────────────┼─────────────┼────────┘
        │             │             │
        └─────────────┴─────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │   Catalog Layer             │
        │  ┌──────────────────────┐   │
        │  │ Lakekeeper (Iceberg) │   │
        │  │   or                 │   │
        │  │ DuckLake (SQL DB)    │   │
        │  └──────────────────────┘   │
        └─────────────┬───────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │   Object Storage (R2/S3)    │
        │  warehouse/                 │
        │  ├── table1/data/*.parquet  │
        │  └── table2/data/*.parquet  │
        └─────────────────────────────┘
```

---

## Best Practices

### 1. Schema Management
- Use DLT's schema evolution for automatic column additions
- Use Iceberg's schema evolution API for production tables
- Enforce schema contracts with primary keys and nullable constraints

### 2. Incremental Processing
- Default to `write_disposition="append"` with incremental cursors
- Use CocoIndex for automatic change detection on file systems
- Force full refresh only when schema changes require it

### 3. Data Quality
- Validate data in DLT resources before loading
- Use RisingWave constraints for streaming validation
- Implement data contracts at API boundaries

### 4. Performance
- Partition large tables by time (day/month)
- Create indexes on join and filter columns
- Use columnar formats (Parquet) for analytics

### 5. Multi-Tenancy
- Filter all queries by `org_id`
- Use row-level security in PostgreSQL
- Implement authorization in GraphQL layer

### 6. Secrets Management
- Use `.dlt/secrets.toml` for DLT credentials (excluded from git)
- Use environment variables for Dagster resources
- Never commit credentials to version control

---

## References

**Primary Sources:**
- modern-data-stack-consolidated.md (original comprehensive guide)
- architecture-reference.md (DLT, CocoIndex, LanceDB patterns)
- core-pipeline-architecture.md (Dagster, DLT, SQLMesh integrations)
- lakehouse-ducklake-architecture.md (DuckLake, LakeFS, Lakekeeper, OLake)
- streaming-risingwave-architecture.md (RisingWave, materialized views)
- stage-2-technical-implementation-outline.md (6-layer architecture)

**Last Updated:** November 29, 2024
