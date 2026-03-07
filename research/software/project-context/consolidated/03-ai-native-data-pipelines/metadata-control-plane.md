# Metadata Control Plane: DuckDB-Backed Dynamic Source Management

## Executive Summary

This document details the architecture for migrating from static YAML configuration to a DuckDB-backed metadata control plane. The approach enables dynamic asset generation, polymorphic tool configuration, and metadata-driven pipeline orchestration.

---

## 1. The Case Against Static Configuration

### 1.1 YAML Limitations

| Limitation | Impact |
|------------|--------|
| **No Referential Integrity** | Source renamed in one place breaks downstream references |
| **No Query Capability** | Can't answer "Which Spanish-English sources update daily?" |
| **Concurrency Issues** | Merge conflicts in collaborative editing |
| **Static Orchestration** | Code deployment required for new sources |

### 1.2 The DuckDB Advantage

DuckDB is uniquely suited as an application metadata store:

| Feature | Benefit |
|---------|---------|
| **In-Process** | No server to provision; single file on disk |
| **OLAP Optimized** | High-performance metadata introspection |
| **Native JSON** | Store complex configs without rigid migrations |
| **Python Integration** | Zero-copy with dicts, Pydantic, DataFrames |

---

## 2. Comprehensive Schema Design

### 2.1 Core Entity: sources

Tool-agnostic master registry:

```sql
CREATE TABLE sources (
    source_id UUID PRIMARY KEY,
    name VARCHAR NOT NULL,
    source_type VARCHAR NOT NULL,  -- 'REST_API', 'GITHUB_REPO', 'WEB_CRAWL', 'PDF_ARCHIVE'
    owner_team VARCHAR,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now(),
    last_updated TIMESTAMP DEFAULT now()
);
```

### 2.2 Polymorphic Configuration: ingestion_configs

JSON columns accommodate different tool "shapes":

```sql
CREATE TABLE ingestion_configs (
    config_id UUID PRIMARY KEY,
    source_id UUID REFERENCES sources(source_id),
    tool_driver VARCHAR NOT NULL,  -- 'dlt', 'crawl4ai', 'cocoindex', 'custom_python'
    connection_spec JSON NOT NULL,
    extraction_strategy JSON NOT NULL,
    secrets_ref VARCHAR  -- 'env:GITHUB_TOKEN' - never store raw secrets
);
```

**JSON Structure by Tool:**

**dlt (REST API):**
```json
{
  "connection_spec": {
    "base_url": "https://api.example.com",
    "pagination": "header_link"
  },
  "extraction_strategy": {
    "endpoints": ["users", "posts"],
    "write_disposition": "merge",
    "primary_key": "id"
  }
}
```

**Crawl4ai (Web Scraping):**
```json
{
  "connection_spec": {
    "headless": true,
    "user_agent": "Mozilla/5.0...",
    "proxy_config": {}
  },
  "extraction_strategy": {
    "css_selector": "article.content",
    "word_count_threshold": 10,
    "excluded_tags": ["nav", "footer"]
  }
}
```

**CocoIndex (Semantic Indexing):**
```json
{
  "connection_spec": {
    "source_path": "s3://bucket/pdfs/"
  },
  "extraction_strategy": {
    "chunk_size": 2000,
    "chunk_overlap": 500,
    "embedding_model": "sentence-transformers/all-MiniLM-L6-v2"
  }
}
```

### 2.3 Bilingual Metadata

TMX and DataCite aligned for interoperability:

```sql
CREATE TABLE bilingual_metadata (
    meta_id UUID PRIMARY KEY,
    source_id UUID REFERENCES sources(source_id),
    source_lang VARCHAR NOT NULL,  -- ISO 639-1/3 code
    target_lang VARCHAR NOT NULL,
    domain VARCHAR,  -- 'Legal', 'Medical', 'Technical'
    alignment_method VARCHAR,  -- 'sentence_aligned', 'document_aligned'
    license_type VARCHAR,  -- 'CC-BY-4.0', 'MIT'
    citation_ref VARCHAR  -- DOI or URL
);
```

### 2.4 Schedule Definitions

Interface between metadata and Dagster:

```sql
CREATE TABLE schedule_definitions (
    schedule_id UUID PRIMARY KEY,
    source_id UUID REFERENCES sources(source_id),
    cron_schedule VARCHAR,  -- '0 2 * * *' for daily at 2 AM
    partition_def JSON,  -- {"type": "daily", "format": "%Y-%m-%d"}
    dagster_group VARCHAR  -- Asset graph grouping
);
```

---

## 3. Tool Hydration Patterns

### 3.1 dlt Dynamic Source Factory

```python
import dlt
from typing import Iterator

def build_dlt_source(config: dict):
    """Factory to create dlt source from database config."""

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

### 3.2 Crawl4ai Configuration Hydration

```python
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig

async def execute_crawl(config: dict):
    """Hydrate Crawl4ai configs from database JSON."""

    # Deserialize JSON to config objects
    browser_config = BrowserConfig(**config['connection_spec'])
    run_config = CrawlerRunConfig(**config['extraction_strategy'])

    async with AsyncWebCrawler(config=browser_config) as crawler:
        result = await crawler.arun(
            url=config['url'],
            config=run_config
        )

    return result.markdown
```

### 3.3 CocoIndex Flow Hydration

```python
import cocoindex

def build_cocoindex_flow(config: dict):
    """Create CocoIndex flow from database config."""

    @cocoindex.flow_def(name=config['name'])
    def dynamic_flow():
        source = cocoindex.sources.directory(
            path=config['connection_spec']['source_path']
        )

        chunks = source.transform(
            cocoindex.SplitRecursively,
            chunk_size=config['extraction_strategy']['chunk_size'],
            chunk_overlap=config['extraction_strategy']['chunk_overlap']
        )

        embeddings = chunks.transform(
            cocoindex.SentenceTransformerEmbed,
            model=config['extraction_strategy']['embedding_model']
        )

        return embeddings

    return dynamic_flow
```

---

## 4. Dagster Asset Factory Implementation

### 4.1 Loading Sources at Definition Time

```python
from dagster import Definitions, asset
import duckdb

def load_sources_from_duckdb() -> list[dict]:
    """Query active sources with their configurations."""
    conn = duckdb.connect("metadata.db")

    query = """
        SELECT
            s.source_id,
            s.name,
            s.source_type,
            ic.tool_driver,
            ic.connection_spec,
            ic.extraction_strategy,
            sd.cron_schedule,
            sd.dagster_group
        FROM sources s
        JOIN ingestion_configs ic USING (source_id)
        LEFT JOIN schedule_definitions sd USING (source_id)
        WHERE s.active = true
    """

    return conn.execute(query).fetchdf().to_dict('records')

def build_asset_for_source(config: dict):
    """Factory function to create asset from config."""

    @asset(
        name=f"source_{config['name']}",
        group_name=config.get('dagster_group', 'default')
    )
    def _asset(context):
        if config['tool_driver'] == 'dlt':
            source = build_dlt_source(config)
            return source()
        elif config['tool_driver'] == 'crawl4ai':
            return execute_crawl(config)
        elif config['tool_driver'] == 'cocoindex':
            flow = build_cocoindex_flow(config)
            return flow.update()

    return _asset

# Generate all assets at load time
sources = load_sources_from_duckdb()
generated_assets = [build_asset_for_source(s) for s in sources]

defs = Definitions(assets=generated_assets)
```

### 4.2 Scaling with Dynamic Partitions

For thousands of sources, use single partitioned asset:

```python
from dagster import DynamicPartitionsDefinition, asset, sensor, RunRequest

source_partitions = DynamicPartitionsDefinition(name="data_sources")

@asset(partitions_def=source_partitions)
def universal_ingestion_asset(context):
    """Single asset handles all sources via partition key."""
    source_id = context.partition_key

    # Fetch config for this specific source
    config = fetch_source_config(source_id)

    # Route to appropriate tool
    if config['tool_driver'] == 'dlt':
        return run_dlt_pipeline(config)
    elif config['tool_driver'] == 'crawl4ai':
        return run_crawl_pipeline(config)
    elif config['tool_driver'] == 'cocoindex':
        return run_cocoindex_pipeline(config)

@sensor(job=ingestion_job)
def source_registry_sensor(context):
    """Detect new sources in database."""
    import duckdb
    conn = duckdb.connect("metadata.db")

    active_sources = conn.execute(
        "SELECT source_id FROM sources WHERE active = true"
    ).fetchall()
    active_ids = [str(s[0]) for s in active_sources]

    existing = context.instance.get_dynamic_partitions("data_sources")

    new_sources = set(active_ids) - set(existing)
    if new_sources:
        context.instance.add_dynamic_partitions("data_sources", list(new_sources))
        for source_id in new_sources:
            yield RunRequest(partition_key=source_id)
```

### 4.3 Schedule-Driven Automation

```python
@sensor(job=scheduled_ingestion_job, minimum_interval_seconds=60)
def schedule_check_sensor(context):
    """Check DuckDB for sources due for update."""
    import duckdb
    from croniter import croniter
    from datetime import datetime

    conn = duckdb.connect("metadata.db")

    schedules = conn.execute("""
        SELECT source_id, cron_schedule, last_run
        FROM schedule_definitions sd
        JOIN sources s USING (source_id)
        WHERE s.active = true AND sd.cron_schedule IS NOT NULL
    """).fetchall()

    now = datetime.now()

    for source_id, cron, last_run in schedules:
        cron_iter = croniter(cron, last_run or datetime.min)
        next_run = cron_iter.get_next(datetime)

        if next_run <= now:
            yield RunRequest(
                run_key=f"{source_id}_{now.isoformat()}",
                partition_key=str(source_id)
            )
```

---

## 5. Administrative Interface

### 5.1 Streamlit Control Plane

```python
import streamlit as st
import duckdb
from pydantic import BaseModel, ValidationError

# Tool-specific config schemas
class DltConfig(BaseModel):
    base_url: str
    endpoints: list[str]
    write_disposition: str = "append"
    primary_key: str | None = None

class CrawlConfig(BaseModel):
    url: str
    css_selector: str
    headless: bool = True
    word_count_threshold: int = 10

st.title("Data Source Registry")

# Source type selection
source_type = st.selectbox("Source Type", ["REST_API", "WEB_CRAWL", "PDF_ARCHIVE"])
tool_driver = st.selectbox("Tool", ["dlt", "crawl4ai", "cocoindex"])

# Dynamic form based on tool
if tool_driver == "dlt":
    base_url = st.text_input("Base URL")
    endpoints = st.text_area("Endpoints (one per line)").split("\n")

    config = DltConfig(base_url=base_url, endpoints=endpoints)

elif tool_driver == "crawl4ai":
    url = st.text_input("URL")
    css_selector = st.text_input("CSS Selector", "article.content")

    config = CrawlConfig(url=url, css_selector=css_selector)

# Validation and save
if st.button("Save Source"):
    try:
        # Pydantic validates before saving
        validated = config.model_dump()

        conn = duckdb.connect("metadata.db")
        # Insert into database...
        st.success("Source saved!")
    except ValidationError as e:
        st.error(f"Validation error: {e}")
```

### 5.2 Bilingual Metadata Form

```python
st.subheader("Bilingual Configuration")

source_lang = st.selectbox("Source Language", ["en", "ga", "es", "fr", "de"])
target_lang = st.selectbox("Target Language", ["en", "ga", "es", "fr", "de"])
domain = st.selectbox("Domain (TMX)", ["Legal", "Medical", "Technical", "General"])
alignment = st.selectbox("Alignment", ["sentence_aligned", "document_aligned"])
license_type = st.selectbox("License", ["CC-BY-4.0", "MIT", "Proprietary"])

# URL template for bilingual crawling
url_template = st.text_input(
    "URL Template",
    "https://site.com/{lang}/page",
    help="Use {lang} placeholder for language code"
)
```

---

## 6. Migration Roadmap

### Phase 1: Schema & Migration (Week 1)
1. Provision DuckDB persistent file
2. Define SQL DDL with JSON columns
3. Write migration script from `sources.yaml`

### Phase 2: Asset Factories (Week 2)
1. Refactor Dagster to remove hardcoded assets
2. Implement `load_sources_from_duckdb()`
3. Build tool-specific factory functions
4. Wire into Definitions

### Phase 3: Dynamic Scaling (Week 3)
1. Transition to partitioned asset pattern
2. Implement registry sensor
3. Configure schedule-driven automation

### Phase 4: Admin UI (Week 4)
1. Deploy Streamlit application
2. Implement Pydantic validation
3. Add TMX/ISO language code validation

---

## 7. Platform Comparison

### 7.1 Meltano

| Aspect | Assessment |
|--------|------------|
| **Pros** | CLI-driven, manages virtual environments, can orchestrate dlt |
| **Cons** | Creates YAML silo, doesn't solve scalability issue |
| **Verdict** | Replaces one static config with another |

### 7.2 Airbyte

| Aspect | Assessment |
|--------|------------|
| **Pros** | User-friendly UI for standard APIs |
| **Cons** | Custom code requires Docker containers conforming to Airbyte protocol |
| **Verdict** | Too heavy for lightweight dlt/Crawl4ai/CocoIndex stack |

### 7.3 Recommendation

**Custom DuckDB + Streamlit + Dagster**
- DuckDB: Queryable, typed configuration store
- Streamlit: Validation-rich admin interface
- Dagster: Dynamic asset generation and scheduling

This provides optimal flexibility without connector-centric constraints.

---

## 8. TMX and Metadata Standards

### 8.1 TMX Header Mapping

```sql
-- Map database fields to TMX attributes
SELECT
    source_lang AS srclang,
    'en' AS adminlang,
    'DagsterPipeline' AS creationtool,
    CASE source_type
        WHEN 'WEB_CRAWL' THEN 'HTML'
        WHEN 'PDF_ARCHIVE' THEN 'PlainText'
        ELSE 'unknown'
    END AS datatype,
    domain
FROM bilingual_metadata bm
JOIN sources s USING (source_id);
```

### 8.2 DataCite Integration

```sql
-- Add citation fields for academic datasets
ALTER TABLE bilingual_metadata ADD COLUMN doi VARCHAR;
ALTER TABLE bilingual_metadata ADD COLUMN contributor_type VARCHAR DEFAULT 'DataCollector';
ALTER TABLE bilingual_metadata ADD COLUMN is_translation_of VARCHAR;  -- Reference to original
```

---

## References

- DuckDB JSON: https://duckdb.org/docs/sql/data_types/json
- DuckDB Concurrency: https://duckdb.org/docs/connect/concurrency
- Dagster Dynamic Partitions: https://dagster.io/blog/dynamic-partitioning
- TMX Standard: https://standards.clarin.eu/sis/views/view-format.xq?id=fTMX
- DataCite Schema: https://schema.datacite.org/
- Streamlit: https://streamlit.io/
