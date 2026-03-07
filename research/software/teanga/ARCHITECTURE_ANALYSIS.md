# DuckLake Unified Platform - Architecture Analysis

## Table of Contents

1. [Directory Overview](#directory-overview)
2. [Technology Usage by Subdirectory](#technology-usage-by-subdirectory)
3. [Integration Priorities & Implementation Status](#integration-priorities--implementation-status)
4. [Remaining Integration Work](#remaining-integration-work)

---

## Directory Overview

```
/Users/cliste/dev/cianfhoghlaim/ducklake/
├── iceberg.py                      # Lance Namespace + Iceberg REST Catalog integration (591 lines)
├── lakekeeper.compose.yaml         # Lakekeeper Docker Compose (NEW)
├── lakekeeper.env.example          # Lakekeeper environment config (NEW)
├── cloudflare_ducklake/            # DuckLake + SQLMesh + Cloudflare R2
├── ducklake-examples/              # Integration notebooks (Ibis, PySpark, etc.)
├── ducklake-workshop/              # TPCH workshop with DuckLake features
├── lance/                          # LanceDB examples + Lance Namespace
├── mlflow_kafka_ducklake/          # ML pipelines with Kafka + MLflow + Graph
├── pixi_sqlmesh_ducklake/          # SQLMesh + Dagster orchestration platform
├── sqlmesh-ibis/                   # Ibis semantic layer + SQLMesh
└── scripts/                        # Test scripts (NEW)
```

---

## Technology Usage by Subdirectory

### 1. `/mlflow_kafka_ducklake/` - ML Pipeline Platform

#### Directory Structure
```
mlflow_kafka_ducklake/
├── .ci/                    # CI/CD provisioning (Kafka, Ollama, PostgreSQL)
├── .github/workflows/      # GitHub Actions release pipeline
├── dlctl/                  # CLI tool for Data Lab control
├── export/                 # Data export functionality
├── graph/                  # Graph database operations (Kuzu-based)
├── infra/                  # Infrastructure as Code (Terraform, Docker)
├── ingest/                 # Data ingestion (Kaggle, HuggingFace, DataCite)
├── ml/                     # ML pipelines, training, serving
├── shared/                 # Core utilities and modules
├── tests/                  # Test suite
└── transform/              # dbt transformation models
```

#### DuckDB/DuckLake Usage
**File:** `shared/lakehouse.py`

| Feature | Implementation |
|---------|----------------|
| Connection | `duckdb.connect()` with in-memory and persistent modes |
| Catalog Attachments | S3-backed catalogs (stage, secure_stage, marts) |
| Exports | `COPY {table} TO '{s3_path}' (FORMAT parquet)` |
| ML Datasets | K-fold cross-validation support (3, 5, 10 folds) |
| Snapshots | Version tracking via `snapshots()` table function |
| Inference Storage | JSON data + predictions + feedback arrays |

**Inference Results Schema:**
```sql
CREATE TABLE secure_stage."{schema}".inference_results (
    inference_uuid VARCHAR,
    model_name VARCHAR NOT NULL,
    model_version VARCHAR NOT NULL,
    data JSON,
    prediction DOUBLE NOT NULL,
    feedback DOUBLE[],
    created_at TIMESTAMP
)
```

#### Kafka Usage
**File:** `ml/events.py`

| Component | Configuration |
|-----------|---------------|
| Topics | `ml_inference_results`, `ml_inference_feedback` |
| Consumer Groups | `lakehouse-inference-result-consumer`, `lakehouse-inference-feedback-consumer` |
| Producer | `aiokafka.AIOKafkaProducer` with JSON serialization |
| Batch Size | 1000 messages |
| Flush Interval | 15 minutes |

**Message Flow:**
```
Inference Request → FastAPI Server → Kafka Producer
                                          ↓
                              ml_inference_results topic
                                          ↓
                              Async Consumer Loop
                                          ↓
                              DuckDB Lakehouse Insert
```

#### MLflow Usage
**File:** `ml/mlflow.py`

| Feature | Implementation |
|---------|----------------|
| Tracking URI | `http://docker-shared:5000` |
| Experiment Creation | `mlflow.set_experiment(experiment_name)` |
| Model Logging | `mlflow.sklearn.log_model()` with signature inference |
| Model Registry | Registered model names: `{schema}_{method}_{features}` |
| Artifacts | S3 bucket: `mlflow/artifacts` |

**Supported Training Methods:**
- XGBoost with GridSearchCV
- Logistic Regression with GridSearchCV
- Feature types: TF-IDF vectorization, Sentence Transformer embeddings

#### Lance/Vector Storage
**Note:** Uses Kuzu's native vector indexing instead of LanceDB.

**File:** `graph/ops.py`

```sql
-- Vector index creation
CALL create_vector_index("{node_table}", "{index_name}", "embedding");

-- K-NN query
CALL query_vector_index('{node_table}', '{index_name}', $embedding, k)
WHERE distance <= $max_distance
RETURN node.node_id, distance
```

#### Iceberg/Lakekeeper Integration
**File:** `shared/lakekeeper.py` (NEW)

| Class | Purpose |
|-------|---------|
| `LakekeeperConfig` | Configuration from environment variables |
| `LakekeeperClient` | REST API client for management endpoints |
| `get_lance_namespace()` | Factory for IcebergNamespace instances |
| `setup_lakekeeper_warehouse()` | Bootstrap helper for dev environments |

---

### 2. `/pixi_sqlmesh_ducklake/` - Orchestration Platform

#### Directory Structure
```
pixi_sqlmesh_ducklake/
├── projects/
│   ├── 01_duckdb_basics/           # DuckDB introduction
│   ├── 02_pg_duckdb/               # PostgreSQL + pg_duckdb
│   ├── 03_ducklake_basics/         # DuckLake fundamentals
│   ├── 04_sql_automation_basics/   # SQLMesh + dbt
│   ├── 05_ingestion/               # dlt data ingestion
│   ├── 06_orchestration_basics/    # Dagster tutorials
│   ├── 07_external/                # Ray + external compute
│   └── 100_combined/               # Full integrated demo
├── services/
│   ├── proxy/                      # Traefik configuration
│   ├── database/                   # PostgreSQL + pg_duckdb
│   └── objectstore/                # MinIO UI
├── docker-compose.yaml             # 1060+ lines, full stack
└── pyproject.toml                  # Pixi workspace config
```

#### SQLMesh Configuration
**File:** `projects/04_sql_automation_basics/sqlmesh/config.yaml`

**Gateways:**

| Gateway | Connection | Catalog Type | State Storage |
|---------|------------|--------------|---------------|
| `devlocal` | DuckDB | DuckLake (file) | Local DuckDB |
| `dev` | DuckDB | PostgreSQL | PostgreSQL |

**DuckLake Catalog Configuration:**
```yaml
catalogs:
  ducklake_catalog:
    type: postgres
    path: "postgres:dbname=ducklake_catalog user=${CATALOG_DB_USER}..."
    data_path: "s3://my-duck-lake/dev/minidemo"
    read_only: true
```

**Extensions:** `ducklake`, `httpfs`, `postgres`, `sqlite`

#### DuckLake Resource Implementation
**File:** `projects/100_combined/tutorial-shared/tutorial_shared/resources/ducklake.py`

```python
class DuckLakeResource(DuckDBConnectionProvider):
    metadata_backend: Union[PostgresConfig, SqliteConfig, DuckDBConfig]
    storage_backend: Union[S3Config, DuckLakeLocalDirectory]
    alias: str = "ducklake"
    plugins: List[str] = ["ducklake"]
```

**Metadata Backends:**
- PostgreSQL (with secrets)
- SQLite (file-based)
- DuckDB (embedded)

**Storage Backends:**
- S3/S3-Compatible (MinIO, R2)
- Local filesystem

#### Dagster Integration Patterns
**File:** `projects/100_combined/combined_basics/combined/definitions.py`

**Asset Definition Pattern:**
```python
@dg.multi_asset(specs=poke_asset_specs, can_subset=True)
def load_pokemon_to_ducklake(context, ducklake: DuckLakeResource):
    pipeline = dlt.pipeline(
        pipeline_name="rest_api_pokemon_to_ducklake",
        destination=dlt.destinations.sqlalchemy(ducklake.get_engine()),
    )
    load_info = pipeline.run(source_to_run)
```

**Resource Configuration:**
```python
RESOURCES_LOCAL = {
    "ducklake": DuckLakeResource(
        metadata_backend=PostgresConfig(...),
        storage_backend=S3Config(...),
    ),
    "dlt": DagsterDltResource(),
    "ray_cluster": LocalRay(...),
}
```

#### Docker Compose Services
| Service | Image | Purpose |
|---------|-------|---------|
| `proxy` | Traefik v3.5 | Reverse proxy, HTTPS |
| `minio` | MinIO | S3-compatible storage |
| `database` | PostgreSQL 18 + pg_duckdb | Hybrid OLTP/OLAP |
| `openmetadata-server` | OpenMetadata | Data catalog |
| `kafka-broker` | Confluent KRaft | Event streaming |
| `datahub-*` | DataHub | Alternative catalog |

---

### 3. `/lance/` - Vector Database & Examples

#### Directory Structure
```
lance/
├── __init__.py                 # Lance Namespace exports
├── lancedb.compose.yaml        # Docker Compose for LanceDB
└── examples/                   # 17+ example directories
    ├── Chatbot_with_Parler_TTS/
    ├── Multilingual_RAG/
    ├── multi-document-agentic-rag/
    ├── multimodal-recipe-agent/
    ├── time-travel-rag/
    ├── lance-ray/
    ├── cognee-RAG/
    └── ...
```

#### LanceDB Usage Patterns

**Basic Vector Search:**
```python
db = lancedb.connect(config.DB_URI)
tbl = db.create_table(config.TABLE_NAME, data=initial_data)
results = tbl.search(query_vector).limit(k).to_pandas()
```

**Hybrid Search with Reranking:**
```python
table.create_fts_index("text", replace=True)
reranker = ColbertReranker()
results = (
    table.search(query, query_type="hybrid")
    .limit(5)
    .rerank(reranker=reranker)
    .to_pandas()
)
```

**Multimodal Search (Text + Image):**
```python
# Text search
results = table.search(text_embedding, vector_column_name="text_embedding")

# Image search
results = table.search(image_embedding, vector_column_name="image_embedding")
```

**Time Travel:**
```python
tbl.checkout(version_num)
results = tbl.search(query_vector).limit(1).to_pandas()
```

#### Docker Compose (S3/R2 Integration)
**File:** `lancedb.compose.yaml`

```yaml
services:
  lance_s3_mounter:
    image: rclone/rclone:latest
    command: mount r2:${R2_BUCKET} /data/s3 --vfs-cache-mode full
    environment:
      RCLONE_CONFIG_R2_PROVIDER: Cloudflare
      RCLONE_CONFIG_R2_ENDPOINT: ${AWS_ENDPOINT_URL}

  lancedb_viewer:
    image: lancedb/lancedb-viewer:latest
    environment:
      LANCE_DATASET_PATH: /data/lance
```

---

### 4. `/cloudflare_ducklake/` - R2 Integration

#### SQLMesh + DuckLake + R2 Configuration
**File:** `config.yaml`

```yaml
gateways:
  ducklake:
    connection:
      type: duckdb
      catalogs:
        ducklake:
          type: ducklake
          path: postgres://neon-host/database    # Metadata
          data_path: r2://ducklake               # Storage
          encrypted: true
      secrets:
        - type: r2
          account_id: ${R2__ACCOUNT_ID}
          key_id: ${R2__ACCESS_KEY_ID}
          secret: ${R2__SECRET_ACCESS_KEY}
```

#### Model Types
| Model | Type | Schedule |
|-------|------|----------|
| `seed_model.sql` | SEED | - |
| `full_model.sql` | FULL | @daily |
| `incremental_model.sql` | INCREMENTAL_BY_TIME_RANGE | - |

---

### 5. `/ducklake-workshop/` - TPCH Tutorial

#### Key Scripts & Features

| Script | DuckLake Feature |
|--------|------------------|
| `01_bootstrap_catalog.sql` | `ducklake_add_data_files()` - zero-copy registration |
| `02_repartition_orders.sql` | Hive-style partitioning (`year=YYYY/month=MM/day=DD`) |
| `04_make_manifest.sql` | `ducklake_snapshot` metadata queries |
| `06_compaction.sql` | `ducklake_merge_adjacent_files()` |
| `07_time_travel.sql` | `SELECT * FROM table AT (VERSION => N)` |

#### DuckLake-Specific SQL Functions
```sql
-- Zero-copy file registration
SELECT ducklake_add_data_files('schema', 'table', ['file1.parquet', 'file2.parquet']);

-- File compaction
SELECT ducklake_merge_adjacent_files('schema', 'table');

-- Cleanup old snapshots
SELECT ducklake_cleanup_old_files('schema', 'table', retention_hours);

-- Query at version
SELECT * FROM schema.table AT (VERSION => 5);
SELECT * FROM schema.table AT (TIMESTAMP => '2024-01-01 00:00:00');
```

---

### 6. `/sqlmesh-ibis/` - Semantic Layer

#### Boring Semantic Layer (BSL) Architecture
```
sqlmesh-ibis/
├── ibis/                       # SQLMesh project
│   ├── config.yaml
│   └── models/
└── boring-semantic-layer/      # Production semantic layer
    ├── agents/                 # LLM-based query generation
    │   ├── backends/           # LangGraph, MCP integration
    │   └── chats/              # CLI, Slack interfaces
    ├── chart/                  # Visualization (Altair, Plotly)
    └── examples/               # 22+ example notebooks
```

#### Ibis Integration
**Note:** Ibis is used as the unified DataFrame API in the Boring Semantic Layer, not directly in the DuckLake projects.

**Key Patterns:**
- Lazy evaluation across backends
- Backend-agnostic transformations
- Query optimization (projection pushdown)
- Session analysis patterns

---

### 7. `/iceberg.py` - Lance Namespace Implementation

#### IcebergNamespace Class (591 lines)
**Purpose:** Bridge Lance format with Iceberg REST Catalog

```python
class IcebergNamespace(LanceNamespace):
    TABLE_TYPE_LANCE = "lance"
    TABLE_TYPE_KEY = "table_type"

    def __init__(self, **properties):
        self.config = IcebergNamespaceConfig(properties)
        self.rest_client = RestClient(...)
```

#### Namespace Operations
| Method | Iceberg REST Endpoint |
|--------|----------------------|
| `list_namespaces()` | `GET /namespaces` |
| `create_namespace()` | `POST /namespaces` |
| `describe_namespace()` | `GET /namespaces/{ns}` |
| `drop_namespace()` | `DELETE /namespaces/{ns}` |

#### Table Operations
| Method | Iceberg REST Endpoint |
|--------|----------------------|
| `list_tables()` | `GET /namespaces/{ns}/tables` |
| `create_empty_table()` | `POST /namespaces/{ns}/tables` |
| `describe_table()` | `GET /namespaces/{ns}/tables/{tbl}` |
| `drop_table()` | `DELETE /namespaces/{ns}/tables/{tbl}` |

#### Key Implementation Details
- Tables marked with `table_type=lance` property
- Namespace encoding: `\x1F` separator for URL safety
- Zero-copy registration: properties point to Lance data location
- Dummy Iceberg schema created for metadata compatibility

---

## Integration Priorities & Implementation Status

### Priority 1: Lakekeeper (Iceberg REST Catalog) - IMPLEMENTED

| Component | Status | File |
|-----------|--------|------|
| Docker Compose | Done | `lakekeeper.compose.yaml` |
| Environment Config | Done | `lakekeeper.env.example` |
| Python Client | Done | `mlflow_kafka_ducklake/shared/lakekeeper.py` |
| Test Script | Done | `scripts/test_lakekeeper.py` |

**What was created:**
```yaml
# lakekeeper.compose.yaml
services:
  lakekeeper-postgres:    # Metadata storage
  lakekeeper-migrate:     # Database migrations
  lakekeeper:             # REST Catalog server (port 8181)
```

### Priority 2: Lance Namespace + Lakekeeper - IMPLEMENTED

| Component | Status | File |
|-----------|--------|------|
| IcebergNamespace | Existing | `iceberg.py` |
| Lakekeeper Factory | Done | `shared/lakekeeper.py` |
| Configuration | Done | Environment-based |

**Integration Pattern:**
```python
from shared.lakekeeper import get_lance_namespace

ns = get_lance_namespace(
    iceberg_endpoint="http://localhost:8181",
    iceberg_warehouse="dev-warehouse",
)
```

### Priority 3: DuckLake Catalog Infrastructure - EXISTING

| Component | Status | Location |
|-----------|--------|----------|
| PostgreSQL Backend | Existing | `pixi_sqlmesh_ducklake/docker-compose.yaml` |
| S3/MinIO Storage | Existing | `pixi_sqlmesh_ducklake/docker-compose.yaml` |
| DuckLake Resource | Existing | `tutorial-shared/resources/ducklake.py` |

### Priority 4: Kafka Streaming - EXISTING

| Component | Status | Location |
|-----------|--------|----------|
| Kafka Broker | Existing | `pixi_sqlmesh_ducklake/docker-compose.yaml` |
| ML Events | Existing | `mlflow_kafka_ducklake/ml/events.py` |
| Consumer Loops | Existing | Async Kafka consumers |

### Priority 5: MLflow Tracking - EXISTING

| Component | Status | Location |
|-----------|--------|----------|
| MLflow Server | Profile | `pixi_sqlmesh_ducklake/docker-compose.yaml` |
| Training Integration | Existing | `mlflow_kafka_ducklake/ml/mlflow.py` |
| Model Registry | Existing | sklearn model logging |

---

## Remaining Integration Work

### 1. SQLMesh + Lakekeeper Gateway

**Status:** NOT IMPLEMENTED

**What's needed:**
- SQLMesh doesn't natively support Iceberg REST catalogs
- Current workaround: Use DuckDB's iceberg extension
- Future: Custom SQLMesh gateway for Lakekeeper

**Proposed Configuration:**
```yaml
gateways:
  lakekeeper:
    connection:
      type: duckdb
      extensions:
        - iceberg
        - httpfs
      # Load Iceberg tables via REST catalog
    state_connection:
      type: postgres
```

### 2. Unified Docker Compose

**Status:** PARTIAL

**Current State:**
- `pixi_sqlmesh_ducklake/docker-compose.yaml` - Main infrastructure
- `lakekeeper.compose.yaml` - Lakekeeper overlay
- `lance/lancedb.compose.yaml` - LanceDB with R2

**What's needed:**
- Compose overlay integration guide
- Network bridging between stacks
- Shared secret management

**Usage:**
```bash
# Combined stack
docker compose \
  -f pixi_sqlmesh_ducklake/docker-compose.yaml \
  -f lakekeeper.compose.yaml \
  up -d
```

### 3. Dagster + Lakekeeper Assets

**Status:** NOT IMPLEMENTED

**What's needed:**
- `LakekeeperResource` for Dagster
- Assets for warehouse/namespace management
- Sensors for catalog change detection

**Proposed Implementation:**
```python
class LakekeeperResource(ConfigurableResource):
    endpoint: str
    warehouse: str

    def get_namespace(self) -> IcebergNamespace:
        return get_lance_namespace(
            iceberg_endpoint=self.endpoint,
            iceberg_warehouse=self.warehouse,
        )

@asset
def lance_embeddings_table(lakekeeper: LakekeeperResource):
    ns = lakekeeper.get_namespace()
    # Create/update Lance table in Iceberg catalog
```

### 4. Knowledge Graph Integration

**Status:** NOT IMPLEMENTED

**Components to integrate:**
- Kuzu (existing in mlflow_kafka_ducklake)
- LanceDB (existing in lance/)
- Cognee (referenced in examples)
- Memgraph (referenced in original plan)

**What's needed:**
- Unified graph resource for Dagster
- Vector index sync between LanceDB and Kuzu
- GraphRAG pipeline assets

### 5. Secret Management (Locket)

**Status:** NOT IMPLEMENTED

**What's needed:**
- Locket configuration for 1Password Connect
- `secrets.env` template with references
- Integration with existing Docker Compose

**Proposed:**
```env
# secrets.env
LAKEKEEPER_DB_PASSWORD=op://vault/lakekeeper/password
AWS_SECRET_ACCESS_KEY=op://vault/minio/secret
MLFLOW_TRACKING_TOKEN=op://vault/mlflow/token
```

### 6. End-to-End Integration Test

**Status:** NOT IMPLEMENTED

**Test Pipeline:**
1. Ingest data via dlt → DuckLake
2. Transform with SQLMesh
3. Train model → MLflow
4. Generate embeddings → LanceDB
5. Build knowledge graph → Kuzu
6. Query via Lance Namespace → Lakekeeper
7. Serve predictions → Kafka

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ORCHESTRATION                                   │
│                     Dagster (Assets, Resources, Sensors)                     │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│   INGESTION   │         │  TRANSFORM    │         │   ML/AI       │
│     dlt       │         │   SQLMesh     │         │   MLflow      │
│   (REST API,  │         │   dbt         │         │   Training    │
│    Kaggle)    │         │               │         │   Serving     │
└───────┬───────┘         └───────┬───────┘         └───────┬───────┘
        │                         │                         │
        └─────────────────────────┼─────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           METADATA CATALOG                                   │
│  ┌─────────────────────┐              ┌─────────────────────┐               │
│  │     Lakekeeper      │◄────────────►│   Lance Namespace   │               │
│  │  (Iceberg REST)     │              │  (IcebergNamespace) │               │
│  │    Port: 8181       │              │                     │               │
│  └─────────────────────┘              └─────────────────────┘               │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│   DuckLake    │         │    Lance      │         │    Kuzu       │
│   (Parquet)   │         │  (Multimodal) │         │   (Graph)     │
│   Analytics   │         │   Vectors     │         │   Knowledge   │
└───────┬───────┘         └───────┬───────┘         └───────┬───────┘
        │                         │                         │
        └─────────────────────────┼─────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OBJECT STORAGE                                     │
│                 MinIO (Dev) / Cloudflare R2 (Prod) / S3                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           EVENT STREAMING                                    │
│                      Kafka (ml_inference_results)                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start Commands

```bash
# 1. Start Lakekeeper
cd /Users/cliste/dev/cianfhoghlaim/ducklake
docker compose -f lakekeeper.compose.yaml up -d

# 2. Test Lakekeeper health
curl http://localhost:8181/health

# 3. Run integration test
python scripts/test_lakekeeper.py

# 4. Start full stack (with existing infrastructure)
docker compose \
  -f pixi_sqlmesh_ducklake/docker-compose.yaml \
  -f lakekeeper.compose.yaml \
  --profile kafka \
  --profile mlflow \
  up -d
```

---

## File Reference

| File | Purpose | Lines |
|------|---------|-------|
| `iceberg.py` | Lance Namespace + Iceberg REST | 591 |
| `lakekeeper.compose.yaml` | Lakekeeper Docker stack | 94 |
| `mlflow_kafka_ducklake/shared/lakekeeper.py` | Lakekeeper Python client | 180 |
| `mlflow_kafka_ducklake/shared/lakehouse.py` | DuckDB connection management | ~500 |
| `mlflow_kafka_ducklake/ml/events.py` | Kafka producers/consumers | ~200 |
| `mlflow_kafka_ducklake/graph/ops.py` | Kuzu graph operations | ~600 |
| `pixi_sqlmesh_ducklake/docker-compose.yaml` | Main infrastructure | 1060 |
| `tutorial-shared/resources/ducklake.py` | DuckLake Dagster resource | ~300 |

---

*Generated: 2024-12-26*
*Project: DuckLake Unified Platform*
