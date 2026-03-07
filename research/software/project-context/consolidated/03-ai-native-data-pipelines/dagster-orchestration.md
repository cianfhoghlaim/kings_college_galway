# Dagster Orchestration for Semantic Knowledge Systems

## Executive Summary

This document details the implementation of Dagster as the orchestration layer for AI-native data pipelines, covering asset-based workflows, dynamic partitioning, sensor-driven automation, and integration with CocoIndex and Graphiti for semantic intelligence.

---

## 1. Design Philosophy: Functional Core, Imperative Shell

### 1.1 Separation of Concerns

The architecture rigorously separates business logic from I/O operations:

**Functional Core (Pure Logic):**
- Text cleaning and LaTeX normalization
- Entity extraction using linguistic patterns
- Data structuring into Pydantic objects
- Never connects to databases, APIs, or filesystems

**Imperative Shell (Dagster):**
- Manages sensors detecting new files
- Handles database connections
- Orchestrates API calls
- Controls state and execution schedules

```python
# Functional Core - Pure function
def parse_math_content(text: str) -> MathQuestion:
    """Pure function - no I/O, fully testable."""
    entities = extract_entities(text)
    latex = normalize_latex(text)
    return MathQuestion(entities=entities, latex=latex)

# Imperative Shell - Dagster handles I/O
@asset
def processed_questions(context, raw_documents):
    """Dagster asset - manages I/O and state."""
    for doc in raw_documents:
        result = parse_math_content(doc.text)  # Call pure function
        yield result
```

### 1.2 Asset-Based vs Task-Based Orchestration

| Paradigm | Focus | State Tracking | Schema Drift |
|----------|-------|----------------|--------------|
| **Task-Based (Airflow)** | "Run the script" | Exit codes only | Manual |
| **Asset-Based (Dagster)** | "Ensure data exists" | Data lineage | Automatic |

Dagster tracks **data assets**, not tasks:
- `raw_exam_pdf` → `extracted_markdown` → `semantic_chunks` → `vector_embeddings`
- Implicit dependency graph inferred from code
- Freshness policies replace cron schedules

---

## 2. Dynamic Partitioning for File Ingestion

### 2.1 The Dynamic Partitions Pattern

Static partitioning fails for educational data where files arrive irregularly. Dynamic partitioning allows runtime partition creation:

```python
from dagster import DynamicPartitionsDefinition, asset

# Define dynamic partition set (initially empty)
exam_paper_partitions = DynamicPartitionsDefinition(name="exam_papers")

@asset(partitions_def=exam_paper_partitions)
def raw_pdf_content(context):
    """Asset representing binary content of specific exam paper."""
    partition_key = context.partition_key
    file_path = resolve_path(partition_key)
    with open(file_path, "rb") as f:
        return f.read()

@asset(partitions_def=exam_paper_partitions)
def extracted_markdown(context, raw_pdf_content):
    """Marker extraction - depends on raw PDF."""
    return marker.process_pdf(raw_pdf_content)
```

**Benefits:**
- Each exam paper has discrete asset lineage
- Failure in "Math_Paper_2023" doesn't block "Math_Paper_2024"
- Granular debugging and backfilling

### 2.2 Sensor-Driven Automation

Sensors detect new files and register partitions automatically:

```python
from dagster import sensor, RunRequest

@sensor(job=process_exam_job)
def new_exam_sensor(context):
    """Poll directory for new PDFs, register partitions."""
    current_files = list_files_in_directory()
    existing_partitions = context.instance.get_dynamic_partitions("exam_papers")

    new_files = [f for f in current_files if f not in existing_partitions]

    if new_files:
        # Register new partitions in Dagster's state
        context.instance.add_dynamic_partitions("exam_papers", new_files)

        # Request run for each new file
        for filename in new_files:
            yield RunRequest(
                run_key=filename,
                partition_key=filename
            )
```

**Workflow:**
1. Sensor polls source directory
2. Diffs against existing partitions
3. Registers new partition keys
4. Yields `RunRequest` for each new file

---

## 3. Asset Graph Architecture

### 3.1 The Document Processing Pipeline

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

### 3.2 Implementation

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
        reference_time=extract_exam_date(context.partition_key),
        entity_types=[MathTheorem, ExamTopic]
    )
```

### 3.3 Memoization Benefits

When changing only the embedding model:
- `raw_pdf_content` - NOT recomputed
- `extracted_markdown` - NOT recomputed
- `semantic_chunks` - NOT recomputed
- `vector_embeddings` - RECOMPUTED (logic changed)
- `knowledge_graph_episodes` - NOT recomputed (independent)

---

## 4. Asset Factory Pattern for Metadata-Driven Pipelines

### 4.1 Dynamic Asset Generation

For pipelines with many sources (100+ scraping targets), generate assets programmatically:

```python
from dagster import Definitions, asset

def load_sources_from_duckdb() -> list[dict]:
    """Query DuckDB for active source configurations."""
    import duckdb
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
generated_assets = [
    build_crawl_asset(s) for s in sources
    if s['tool_driver'] == 'crawl4ai'
]

defs = Definitions(assets=generated_assets)
```

### 4.2 Scaling with Generic Partitioned Assets

For thousands of sources, use a single partitioned asset instead:

```python
from dagster import DynamicPartitionsDefinition, asset, sensor

source_partitions = DynamicPartitionsDefinition(name="data_sources")

@asset(partitions_def=source_partitions)
def generic_crawler_job(context):
    """Single asset handles all crawling via partition key."""
    source_id = context.partition_key

    # Fetch config for this specific source
    config = fetch_config_from_duckdb(source_id)

    # Execute crawl with config
    result = execute_crawl(config)
    return result

@sensor(job=crawl_job)
def source_registry_sensor(context):
    """Monitor DuckDB for new sources."""
    active_sources = get_active_source_ids()
    existing = context.instance.get_dynamic_partitions("data_sources")

    new_sources = set(active_sources) - set(existing)
    if new_sources:
        context.instance.add_dynamic_partitions("data_sources", list(new_sources))
        for source_id in new_sources:
            yield RunRequest(partition_key=source_id)
```

---

## 5. CocoIndex Integration

### 5.1 Library vs Service Pattern

CocoIndex can run as a service with internal orchestration, but running "orchestrator within orchestrator" creates complexity. **Use CocoIndex as a library within Dagster assets.**

### 5.2 Semantic Chunking

CocoIndex's `SplitRecursively` uses Tree-sitter for syntax-aware splitting:

```python
@asset
def semantic_chunks(extracted_markdown: str) -> list[str]:
    """Syntax-aware chunking preserves LaTeX equations."""
    import cocoindex

    return cocoindex.SplitRecursively(
        text=extracted_markdown,
        language="markdown",  # Tree-sitter parser
        chunk_size=2000,
        chunk_overlap=500
    )
```

**Why Tree-sitter matters:**
- Recognizes code blocks (LaTeX `$$...$$`) as atomic units
- Respects header boundaries (`# Question 1`)
- Produces semantically coherent chunks

### 5.3 Hybrid Embedding Strategy

```python
@cocoindex.transform_flow()
def text_to_embedding(text: cocoindex.DataSlice[str]):
    """Reusable transform for indexing AND querying."""
    return text.transform(
        cocoindex.functions.SentenceTransformerEmbed(
            model="sentence-transformers/all-MiniLM-L6-v2"
        )
    )

@asset
def vector_index(semantic_chunks: list[str]):
    """Build vector index with hybrid strategy."""
    import cocoindex

    # Textual embedding
    embeddings = [text_to_embedding(chunk) for chunk in chunks]

    # Preserve raw LaTeX as metadata
    records = [
        {"text": chunk, "embedding": emb, "latex": extract_latex(chunk)}
        for chunk, emb in zip(chunks, embeddings)
    ]

    cocoindex.collect(records, "research_vectors")
```

---

## 6. Graphiti Integration

### 6.1 Temporal Knowledge Graph

Graphiti enables bi-temporal queries:
- **Valid Time:** When the fact was true (exam date)
- **Transaction Time:** When fact was recorded

```python
@asset(partitions_def=exam_paper_partitions)
def temporal_knowledge_graph(context, extracted_markdown):
    """Ingest exam data with temporal context."""
    from graphiti_core import Graphiti, EpisodeType
    from pydantic import BaseModel, Field

    # Define ontology
    class MathTheorem(BaseModel):
        name: str = Field(description="Theorem name, e.g., Pythagoras")
        latex_def: str = Field(description="LaTeX definition")

    class ExamTopic(BaseModel):
        name: str = Field(description="Curriculum topic")
        code: str = Field(description="Curriculum code, e.g., C1.2")

    client = Graphiti("falkor://localhost:6379")

    await client.add_episode(
        name=f"exam_{context.partition_key}",
        episode_body=extracted_markdown,
        source=EpisodeType.text,
        reference_time=parse_exam_date(context.partition_key),
        entity_types=[MathTheorem, ExamTopic]  # Constrain extraction
    )
```

### 6.2 Entity Resolution

Graphiti performs LLM-based entity resolution:
- "Question 1" in Exam Paper links to "Question 1" in Marking Scheme
- "Maths" and "Mathematics" merge into single entity

### 6.3 Hybrid Search

```python
def search_knowledge_graph(query: str):
    """Combine semantic, keyword, and graph traversal."""
    client = Graphiti("falkor://localhost:6379")

    results = await client.search(
        query=query,
        search_type="hybrid",  # Semantic + BM25 + Graph
        limit=10
    )

    return results
```

---

## 7. Operational Patterns

### 7.1 Asset Checks

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

### 7.2 Retry Policies

```python
from dagster import RetryPolicy

@asset(
    partitions_def=exam_paper_partitions,
    retry_policy=RetryPolicy(max_retries=3, delay=30)
)
def extracted_markdown(raw_pdf_content):
    """Retry on transient failures."""
    return marker.process_pdf(raw_pdf_content)
```

### 7.3 Resource Configuration

```python
from dagster import resource, Definitions

@resource
def graphiti_resource(context):
    """Configurable Graphiti connection."""
    return Graphiti(context.resource_config["uri"])

defs = Definitions(
    assets=[...],
    resources={
        "graphiti": graphiti_resource.configured({
            "uri": "falkor://localhost:6379"
        })
    }
)
```

---

## 8. Deployment Architecture

### 8.1 Dockerized Stack

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
    # Shared storage for Dagster metadata + CocoIndex vectors

  falkordb:
    image: falkordb/falkordb:latest
    # Graph storage for Graphiti

  gpu-worker:
    image: custom/extraction-worker
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
```

### 8.2 Database Independence

- **Graphiti:** Swap Neo4j/FalkorDB via connection URI
- **CocoIndex:** Uses PostgreSQL + pgvector (vendor-neutral)
- **Marker:** Local GPU inference (no API dependency)

---

## 9. Implementation Priorities

### Phase 1: Core Pipeline
1. Implement Marker extraction asset
2. Add dynamic partitioning for file ingestion
3. Configure sensor for new file detection

### Phase 2: Semantic Intelligence
1. Integrate CocoIndex for syntax-aware chunking
2. Build vector embedding pipeline
3. Add hybrid embedding strategy

### Phase 3: Knowledge Graph
1. Deploy Graphiti with FalkorDB
2. Define domain ontology (Pydantic models)
3. Implement temporal episode ingestion

### Phase 4: Operational Maturity
1. Add asset checks for data quality
2. Configure retry policies
3. Implement monitoring/alerting

---

## References

- Dagster Partitioning: https://docs.dagster.io/guides/build/partitions-and-backfills
- Dagster Sensors: https://docs.dagster.io/guides/automate/sensors
- CocoIndex: https://cocoindex.io/docs/
- Graphiti: https://help.getzep.com/graphiti/
- Marker PDF: https://github.com/VikParuchuri/marker
