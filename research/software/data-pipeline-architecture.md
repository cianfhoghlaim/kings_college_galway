# Data Pipeline Architecture

Comprehensive guide to AI-native data pipelines using BAML schema-first design, dlt for ETL, and Dagster for orchestration.

---

## Table of Contents

1. [Schema-First Design](#1-schema-first-design)
2. [BAML Integration](#2-baml-integration)
3. [ETL with dlt](#3-etl-with-dlt)
4. [Orchestration with Dagster](#4-orchestration-with-dagster)
5. [Lakehouse Architecture](#5-lakehouse-architecture)
6. [Metadata Control Plane](#6-metadata-control-plane)
7. [Multi-Database Ingestion](#7-multi-database-ingestion)
8. [Implementation Patterns](#8-implementation-patterns)

---

## 1. Schema-First Design

### 1.1 The Shift from Prompt Engineering to Schema Engineering

Modern AI pipelines require treating LLM outputs as structured data, not freeform text. BAML (Boundary AI Markup Language) serves as the **single source of truth** for data contracts.

**Architecture Flow:**
```
BAML Definition → Pydantic Models (Python) → dlt Pipelines
                → TypeScript Interfaces → Zod Schemas → TanStack/oRPC
```

### 1.2 Why Schema-First?

| Approach | Problem | Solution |
|----------|---------|----------|
| **Prompt Engineering** | Brittle, breaks on model updates | Formal schema contracts |
| **Runtime JSON Validation** | Token-heavy, slow | Compile-time validation |
| **Manual Type Sync** | Frontend/backend drift | Generated types |

---

## 2. BAML Integration

### 2.1 Dual-Target Code Generation

Configure BAML to output both Python and TypeScript clients:

```
// baml_src/generators.baml

// Generator 1: Python Data Layer
generator python_client {
  output_type "python/pydantic"
  output_dir "../backend/baml_client"
  version "0.76.2"
  default_client_mode "async"
}

// Generator 2: TypeScript Application Layer
generator typescript_client {
  output_type "typescript"
  output_dir "../frontend/src/baml_client"
  version "0.76.2"
  default_client_mode "async"
}
```

### 2.2 Defining Complex Entities

```
// baml_src/models.baml

enum EntityType {
  PERSON
  ORGANIZATION
  LOCATION
  CONCEPT
}

class IdentifiedEntity {
  name string @description("The canonical name of the entity")
  type EntityType
  confidence float
}

class ResearchInsight {
  id string @description("UUID")
  title string
  summary string
  entities IdentifiedEntity[]
  embedding_context string @description("Text used for vectorization")
  citations string[]
  published_date string
}

function ExtractInsight(text: string) -> ResearchInsight {
  client "openai/gpt-4o"
  prompt #"
    Analyze the following text and extract the research insight.
    Identify key entities and their types.

    {{ ctx.output_format }}

    Text:
    {{ text }}
  "#
}
```

### 2.3 Schema-Aligned Parsing (SAP)

BAML's SAP algorithm ensures LLM outputs are validated at extraction time, not runtime:

```python
from backend.baml_client import b
from backend.baml_client.types import ResearchInsight

# BAML call: Returns a validated Pydantic object
# If LLM produces malformed output, BAML raises exception
insight = b.ExtractInsight(text)  # Strictly typed
```

---

## 3. ETL with dlt

### 3.1 BAML-to-dlt Bridge

dlt natively introspects Pydantic models, making BAML-generated types first-class citizens:

```python
import dlt
from typing import Iterator
from backend.baml_client import b
from backend.baml_client.types import ResearchInsight

@dlt.source
def research_source(texts: list[str]):

    @dlt.resource(
        name="research_insights",
        write_disposition="merge",  # Upsert based on primary key
        primary_key="id",
        columns=ResearchInsight  # Pydantic model defines schema
    )
    def extract_insights() -> Iterator:
        for text in texts:
            insight = b.ExtractInsight(text)
            yield insight

    return extract_insights
```

### 3.2 Resource Configuration

```python
@dlt.resource(
    name="curriculum_standards",
    write_disposition="replace",
    primary_key=["subject_id", "level"]
)
def curriculum_resource(subjects: list[str]) -> Iterator[dict]:
    for subject in subjects:
        standards = fetch_curriculum(subject)
        yield from standards
```

### 3.3 Nested Data Handling

dlt automatically normalizes nested structures:

```python
# Input: {"id": "1", "entities": [{"name": "X", "type": "PERSON"}]}
# dlt creates:
#   - research_insights (parent table)
#   - research_insights__entities (child table with FK)
```

---

## 4. Orchestration with Dagster

### 4.1 Design Philosophy: Functional Core, Imperative Shell

```python
# Functional Core - Pure function (no I/O)
def parse_math_content(text: str) -> MathQuestion:
    """Pure function - fully testable."""
    entities = extract_entities(text)
    latex = normalize_latex(text)
    return MathQuestion(entities=entities, latex=latex)

# Imperative Shell - Dagster handles I/O
@asset
def processed_questions(context, raw_documents):
    """Dagster asset - manages I/O and state."""
    for doc in raw_documents:
        result = parse_math_content(doc.text)
        yield result
```

### 4.2 Asset-Based vs Task-Based

| Paradigm | Focus | State Tracking | Schema Drift |
|----------|-------|----------------|--------------|
| **Task-Based (Airflow)** | "Run the script" | Exit codes only | Manual |
| **Asset-Based (Dagster)** | "Ensure data exists" | Data lineage | Automatic |

### 4.3 Dynamic Partitioning

```python
from dagster import DynamicPartitionsDefinition, asset, sensor, RunRequest

exam_paper_partitions = DynamicPartitionsDefinition(name="exam_papers")

@asset(partitions_def=exam_paper_partitions)
def raw_pdf_content(context):
    """Asset representing binary content of specific exam paper."""
    partition_key = context.partition_key
    file_path = resolve_path(partition_key)
    with open(file_path, "rb") as f:
        return f.read()

@sensor(job=process_exam_job)
def new_exam_sensor(context):
    """Poll directory for new PDFs, register partitions."""
    current_files = list_files_in_directory()
    existing = context.instance.get_dynamic_partitions("exam_papers")

    new_files = [f for f in current_files if f not in existing]

    if new_files:
        context.instance.add_dynamic_partitions("exam_papers", new_files)
        for filename in new_files:
            yield RunRequest(run_key=filename, partition_key=filename)
```

### 4.4 Asset Graph Architecture

```
raw_pdf_file (Binary Input)
    ↓
extracted_markdown (Marker Processing)
    ↓
semantic_chunks (CocoIndex Splitting)
    ↓
vector_embeddings (Sentence Transformer)
    ↓
knowledge_graph_episodes (Graphiti Ingestion)
```

### 4.5 Asset Factory Pattern

For metadata-driven pipelines with many sources:

```python
from dagster import Definitions, asset
import duckdb

def load_sources_from_duckdb() -> list[dict]:
    """Query DuckDB for active source configurations."""
    conn = duckdb.connect("metadata.db")
    return conn.execute("""
        SELECT source_id, name, tool_driver, connection_spec, extraction_strategy
        FROM sources
        JOIN ingestion_configs USING (source_id)
        WHERE active = true
    """).fetchall()

def build_crawl_asset(config: dict):
    """Factory function to create crawler asset."""
    @asset(name=f"crawl_{config['name']}")
    def _crawl_asset(context):
        from crawl4ai import AsyncWebCrawler, CrawlerRunConfig

        run_config = CrawlerRunConfig(**config['extraction_strategy'])
        async with AsyncWebCrawler() as crawler:
            result = await crawler.arun(
                url=config['connection_spec']['url'],
                config=run_config
            )
        return result.markdown

    return _crawl_asset

# Generate assets at load time
sources = load_sources_from_duckdb()
generated_assets = [build_crawl_asset(s) for s in sources if s['tool_driver'] == 'crawl4ai']

defs = Definitions(assets=generated_assets)
```

---

## 5. Lakehouse Architecture

### 5.1 Second-Generation Stack

| Layer | Technology | Advantage |
|-------|------------|-----------|
| **Ingestion** | OLake (Go) | 300K+ rows/sec, no JVM, direct Iceberg |
| **Governance** | Lakekeeper (Rust) | Deterministic latency, credential vending |
| **Compute** | RisingWave | Streaming SQL, materialized views |

### 5.2 OLake High-Velocity Ingestion

**Parallelized Snapshotting:**

| Database | Chunking Strategy | Method |
|----------|-------------------|--------|
| **PostgreSQL** | Physical Block | CTID ranges |
| **MySQL** | Key Range | Primary key queries |
| **MongoDB** | Vector Splitting | Split-Vector commands |

**Configuration:**
```json
{
  "host": "postgres.example.com",
  "port": 5432,
  "database": "production",
  "update_method": {
    "replication_slot": "olake_slot",
    "publication": "olake_pub"
  },
  "max_threads": 8
}
```

### 5.3 Lakekeeper Governance

**Rust Architecture Benefits:**

| JVM Catalog | Rust (Lakekeeper) |
|-------------|-------------------|
| GC pauses cause latency spikes | Deterministic latency |
| Large memory footprint | Minimal binary |
| Complex threading model | Memory-safe concurrency |

**Credential Vending Flow:**
1. Client authenticates (OAuth2/OIDC)
2. Lakekeeper verifies permissions
3. Calls AWS STS to assume role
4. Returns short-lived, scoped credentials

### 5.4 RisingWave Streaming Compute

```sql
-- Create connection to Lakekeeper
CREATE CONNECTION lakekeeper_conn WITH (
  type = 'iceberg',
  catalog.type = 'rest',
  catalog.uri = 'http://lakekeeper:8181/catalog/',
  warehouse.path = 'main-warehouse',
  s3.endpoint = 'http://minio:9000'
);

-- Real-time aggregation
CREATE MATERIALIZED VIEW order_stats AS
SELECT
  date_trunc('hour', created_at) as hour,
  count(*) as order_count,
  sum(total_amount) as revenue
FROM main_warehouse.sales.orders
GROUP BY 1;
```

---

## 6. Metadata Control Plane

### 6.1 DuckDB as Metadata Store

**Why DuckDB over YAML:**

| Limitation | Impact |
|------------|--------|
| **No Referential Integrity** | Broken references |
| **No Query Capability** | Can't filter sources |
| **Concurrency Issues** | Merge conflicts |
| **Static Orchestration** | Deploy for new sources |

### 6.2 Schema Design

```sql
CREATE TABLE sources (
    source_id UUID PRIMARY KEY,
    name VARCHAR NOT NULL,
    source_type VARCHAR NOT NULL,  -- 'REST_API', 'WEB_CRAWL', 'PDF_ARCHIVE'
    owner_team VARCHAR,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE ingestion_configs (
    config_id UUID PRIMARY KEY,
    source_id UUID REFERENCES sources(source_id),
    tool_driver VARCHAR NOT NULL,  -- 'dlt', 'crawl4ai', 'cocoindex'
    connection_spec JSON NOT NULL,
    extraction_strategy JSON NOT NULL,
    secrets_ref VARCHAR  -- 'env:GITHUB_TOKEN'
);

CREATE TABLE bilingual_metadata (
    meta_id UUID PRIMARY KEY,
    source_id UUID REFERENCES sources(source_id),
    source_lang VARCHAR NOT NULL,  -- ISO 639 code
    target_lang VARCHAR NOT NULL,
    domain VARCHAR,  -- 'Legal', 'Medical', 'Technical'
    alignment_method VARCHAR,
    license_type VARCHAR
);

CREATE TABLE schedule_definitions (
    schedule_id UUID PRIMARY KEY,
    source_id UUID REFERENCES sources(source_id),
    cron_schedule VARCHAR,
    partition_def JSON,
    dagster_group VARCHAR
);
```

### 6.3 Tool Hydration Patterns

**dlt Dynamic Source Factory:**
```python
def build_dlt_source(config: dict):
    @dlt.source
    def dynamic_source():
        base_url = config['connection_spec']['base_url']

        for endpoint in config['extraction_strategy']['endpoints']:
            @dlt.resource(
                name=endpoint,
                write_disposition=config['extraction_strategy'].get('write_disposition', 'append'),
                primary_key=config['extraction_strategy'].get('primary_key')
            )
            def fetch_data(ep=endpoint):
                import requests
                response = requests.get(f"{base_url}/{ep}")
                yield from response.json()

            yield fetch_data

    return dynamic_source
```

---

## 7. Multi-Database Ingestion

### 7.1 Polyglot Persistence Strategy

| Destination | Integration | Schema Mapping |
|-------------|-------------|----------------|
| **PostgreSQL** | Native dlt destination | Auto table creation |
| **DuckDB** | Native dlt destination | Config change only |
| **LanceDB** | lancedb_adapter | Embedding field spec |
| **FalkorDB** | Custom destination | Cypher MERGE |
| **Graphiti** | Custom destination | add_episode API |

### 7.2 LanceDB Vector Integration

```python
from dlt.destinations.adapters import lancedb_adapter

def configure_lancedb_pipeline():
    source = research_source(["..."])

    # Specify fields to embed
    lancedb_adapter(
        source.extract_insights,
        embed=["embedding_context", "summary"]
    )

    pipeline = dlt.pipeline(
        pipeline_name="vector_ingestion",
        destination="lancedb",
        dataset_name="research_vectors"
    )
    return pipeline
```

### 7.3 FalkorDB Custom Destination

```python
import dlt
from falkordb import FalkorDB

@dlt.destination(batch_size=50)
def falkordb_destination(items, table_schema):
    """Custom dlt destination for graph database."""
    client = FalkorDB(host='localhost', port=6379)
    graph = client.select_graph('KnowledgeGraph')

    for item in items:
        # Create Insight Node
        query_insight = """
        MERGE (i:Insight {id: $id})
        SET i.title = $title, i.summary = $summary
        """
        graph.query(query_insight, {
            'id': item['id'],
            'title': item['title'],
            'summary': item['summary']
        })

        # Create Entity Nodes and Relationships
        if 'entities' in item:
            for entity in item['entities']:
                query_rel = """
                MATCH (i:Insight {id: $id})
                MERGE (e:Entity {name: $e_name})
                SET e.type = $e_type
                MERGE (i)-[:MENTIONS]->(e)
                """
                graph.query(query_rel, {
                    'id': item['id'],
                    'e_name': entity['name'],
                    'e_type': entity['type']
                })
```

### 7.4 Graphiti Custom Destination

```python
from graphiti_core import Graphiti, EpisodeType
import asyncio

@dlt.destination(batch_size=10)
def graphiti_destination(items, table_schema):
    """Loads data into Graphiti as episodes."""
    async def _ingest_batch():
        client = Graphiti("falkor://localhost:6379")

        for item in items:
            await client.add_episode(
                name=f"insight_{item['id']}",
                episode_body=item,
                source=EpisodeType.json,
                source_description="BAML Extracted Research",
                reference_time=datetime.now()
            )

        await client.close()

    asyncio.run(_ingest_batch())
```

---

## 8. Implementation Patterns

### 8.1 Complete Pipeline Example

```python
from dagster import asset, AssetIn
import marker
from cocoindex import SplitRecursively, SentenceTransformerEmbed

@asset(partitions_def=exam_paper_partitions)
def extracted_markdown(context, raw_pdf_content) -> str:
    """Convert PDF to LaTeX-preserving Markdown."""
    return marker.process_pdf(raw_pdf_content)

@asset(partitions_def=exam_paper_partitions)
def semantic_chunks(context, extracted_markdown) -> list[str]:
    """Split Markdown into syntax-aware chunks."""
    return SplitRecursively(
        extracted_markdown,
        language="markdown",
        chunk_size=2000,
        chunk_overlap=500
    )

@asset(partitions_def=exam_paper_partitions)
def vector_embeddings(context, semantic_chunks) -> list[dict]:
    """Generate embeddings for each chunk."""
    embedder = SentenceTransformerEmbed(
        model="sentence-transformers/all-MiniLM-L6-v2"
    )
    return [
        {"text": chunk, "embedding": embedder(chunk)}
        for chunk in semantic_chunks
    ]

@asset(partitions_def=exam_paper_partitions)
def knowledge_graph_episodes(context, extracted_markdown):
    """Ingest into Graphiti temporal graph."""
    from graphiti_core import Graphiti, EpisodeType

    client = Graphiti("falkor://localhost:6379")
    await client.add_episode(
        name=f"exam_{context.partition_key}",
        episode_body=extracted_markdown,
        source=EpisodeType.text,
        reference_time=parse_exam_date(context.partition_key)
    )
```

### 8.2 Docker Compose Deployment

```yaml
services:
  dagster-daemon:
    image: dagster/dagster:latest
    command: dagster-daemon run

  dagster-webserver:
    image: dagster/dagster:latest
    command: dagster-webserver -h 0.0.0.0 -p 3000

  postgres:
    image: postgres:15
    # Dagster metadata + CocoIndex vectors

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9090"

  lakekeeper:
    image: quay.io/lakekeeper/catalog:latest
    ports: ["8181:8181"]

  falkordb:
    image: falkordb/falkordb:latest
    # Graph storage for Graphiti

  risingwave:
    image: risingwavelabs/risingwave:latest
    ports: ["4566:4566", "5691:5691"]
```

### 8.3 Schema Evolution Workflow

```
1. Update BAML definition (add 'author' field)
2. Run: npm run codegen (baml-cli + ts-to-zod)
3. dlt detects new field in Pydantic model
4. Auto ALTER TABLE on Postgres
5. TypeScript compiler flags affected handlers
6. Zod schema includes .optional() for new field
```

### 8.4 Operational Patterns

**Asset Checks:**
```python
from dagster import asset_check, AssetCheckResult

@asset_check(asset=extracted_markdown)
def latex_density_check(context, extracted_markdown):
    """Verify PDF extraction produced LaTeX."""
    latex_count = extracted_markdown.count("$$")
    density = latex_count / len(extracted_markdown)

    return AssetCheckResult(
        passed=density > 0.01,
        metadata={"latex_density": density}
    )
```

**Retry Policies:**
```python
from dagster import RetryPolicy

@asset(
    partitions_def=exam_paper_partitions,
    retry_policy=RetryPolicy(max_retries=3, delay=30)
)
def extracted_markdown(raw_pdf_content):
    return marker.process_pdf(raw_pdf_content)
```

---

## References

- BAML Documentation: https://docs.boundaryml.com
- dlt Documentation: https://dlthub.com/docs
- Dagster Documentation: https://docs.dagster.io
- OLake: https://olake.io/docs
- Lakekeeper: https://docs.lakekeeper.io
- RisingWave: https://docs.risingwave.com
- CocoIndex: https://cocoindex.io/docs
- Graphiti: https://help.getzep.com/graphiti
