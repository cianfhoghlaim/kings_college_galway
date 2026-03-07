# Implementation Guide & Best Practices

## Quick Navigation

This document provides step-by-step implementation patterns, tutorials, and best practices for the data-unified platform.

**Related Documents:**
- [ARCHITECTURE.md](./ARCHITECTURE.md) - Core architecture reference
- [SCHEMAS_AND_TYPES.md](./SCHEMAS_AND_TYPES.md) - Type safety patterns
- [AI_MEMORY.md](./AI_MEMORY.md) - Agent and knowledge systems

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [DuckDB + R2 Integration](#duckdb--r2-integration)
3. [Lakehouse Setup](#lakehouse-setup)
4. [Crawl4AI + DLT Pipeline](#crawl4ai--dlt-pipeline)
5. [Modular Component Design](#modular-component-design)
6. [Framework Adapter Patterns](#framework-adapter-patterns)
7. [Dagster Integration](#dagster-integration)
8. [CLI Development with Typer](#cli-development-with-typer)
9. [Incremental Processing](#incremental-processing)
10. [Production Deployment](#production-deployment)

---

## Getting Started

### Prerequisites

```bash
# Python 3.11+
python --version

# Install uv (fast package manager)
curl -LsSf https://astral.sh/uv/install.sh | sh

# Create virtual environment
uv venv
source .venv/bin/activate

# Install core dependencies
uv pip install dlt duckdb dagster cocoindex pydantic typer rich
```

### Project Structure

```
data-unified/
├── cli/                    # Typer CLI commands
│   ├── __init__.py
│   ├── main.py
│   └── commands/
├── core/                   # Pure Python business logic
│   ├── __init__.py
│   ├── models.py           # Pydantic schemas
│   └── functions.py        # Core functions
├── adapters/               # Framework-specific adapters
│   ├── dagster.py
│   ├── dlt.py
│   └── marimo.py
├── pipelines/              # DLT pipelines
│   ├── github_to_r2/
│   ├── docs_to_knowledge/
│   └── shared/
├── dagster_project/        # Dagster assets & jobs
│   ├── __init__.py
│   └── definitions.py
├── cocoindex/              # CocoIndex flows
│   ├── flows/
│   └── config.py
├── pyproject.toml
└── .env
```

---

## DuckDB + R2 Integration

### Configure R2 Access

```python
import duckdb

con = duckdb.connect()
con.execute("INSTALL httpfs; LOAD httpfs;")

# Create R2 secret (store credentials securely!)
con.execute("""
    CREATE SECRET cloudflare_r2 (
        TYPE S3,
        KEY_ID 'YOUR_R2_ACCESS_KEY_ID',
        SECRET 'YOUR_R2_SECRET_ACCESS_KEY',
        ENDPOINT 'https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com',
        REGION 'auto'
    );
""")
```

### Read Parquet from R2

```python
# Query Parquet files directly from R2
df = con.execute("""
    SELECT *
    FROM read_parquet('s3://my-bucket/data/*.parquet')
    WHERE created_at > '2024-01-01'
""").fetchdf()
```

### Write Parquet to R2

```python
# Write query results to R2
con.execute("""
    COPY (
        SELECT * FROM my_table
    ) TO 's3://my-bucket/output/data.parquet'
    (FORMAT PARQUET, COMPRESSION 'ZSTD');
""")
```

### Using DLT with R2

```python
# .dlt/secrets.toml
# [destination.filesystem.credentials]
# aws_access_key_id = "R2_ACCESS_KEY"
# aws_secret_access_key = "R2_SECRET_KEY"
# endpoint_url = "https://ACCOUNT_ID.r2.cloudflarestorage.com"

import dlt

pipeline = dlt.pipeline(
    pipeline_name="data_lake",
    destination="filesystem",
    dataset_name="raw"
)

# Data automatically written as Parquet to R2
pipeline.run(
    data_source,
    loader_file_format="parquet"
)
```

---

## Lakehouse Setup

### DuckLake with PostgreSQL Catalog

```python
import duckdb

con = duckdb.connect()

# Attach DuckLake catalog
con.execute("""
    ATTACH 'ducklake:analytics.ducklake' AS analytics
    (TYPE POSTGRES,
     HOST 'postgres.example.com',
     DATABASE 'catalog',
     USER 'ducklake',
     PASSWORD 'secret',
     STORAGE_PATH 's3://my-bucket/warehouse');
""")

# Use DuckLake catalog
con.execute("USE analytics;")

# Create versioned table (data in R2, metadata in Postgres)
con.execute("""
    CREATE TABLE events AS
    SELECT * FROM read_parquet('s3://bucket/raw/events/*.parquet');
""")

# Time travel query
con.execute("""
    SELECT * FROM events
    AT TIMESTAMP '2024-01-01 00:00:00';
""")
```

### Apache Iceberg with Lakekeeper

```yaml
# docker-compose.yml for Lakekeeper
services:
  lakekeeper:
    image: lakekeeper/catalog:latest
    ports:
      - "8181:8181"
    environment:
      LAKEKEEPER__LISTEN_PORT: 8181
      LAKEKEEPER__CATALOG_TYPE: postgres
      LAKEKEEPER__PG_DATABASE_URL: postgres://user:pass@postgres:5432/iceberg
      LAKEKEEPER__WAREHOUSE__DEFAULT__STORAGE_PROFILE__TYPE: s3
      LAKEKEEPER__WAREHOUSE__DEFAULT__STORAGE_PROFILE__BUCKET: my-iceberg-bucket
      LAKEKEEPER__WAREHOUSE__DEFAULT__STORAGE_PROFILE__ENDPOINT: https://account.r2.cloudflarestorage.com

  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
      POSTGRES_DB: iceberg
```

```python
# Query Iceberg via DuckDB
con.execute("INSTALL iceberg; LOAD iceberg;")

con.execute("""
    SELECT * FROM iceberg_scan(
        's3://my-iceberg-bucket/warehouse/events',
        catalog_type='rest',
        catalog_uri='http://lakekeeper:8181/catalog'
    );
""")
```

---

## Crawl4AI + DLT Pipeline

### Basic Web Scraping

```python
import asyncio
from crawl4ai import AsyncWebCrawler

async def scrape_docs():
    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(
            url="https://docs.example.com",
            word_count_threshold=10,
            excluded_tags=["nav", "footer"]
        )

        if result.status.is_success():
            return result.markdown  # Clean markdown output

asyncio.run(scrape_docs())
```

### DLT Integration

```python
import dlt
from crawl4ai import AsyncWebCrawler

@dlt.resource(name="scraped_docs")
async def scrape_documentation(urls: list[str]):
    """Scrape documentation and yield for DLT processing"""
    async with AsyncWebCrawler() as crawler:
        for url in urls:
            result = await crawler.arun(url=url)
            if result.status.is_success():
                yield {
                    "url": url,
                    "title": result.metadata.get("title", ""),
                    "content": result.markdown,
                    "scraped_at": datetime.now().isoformat()
                }

# Run pipeline
pipeline = dlt.pipeline(
    pipeline_name="docs_scraping",
    destination="duckdb",
    dataset_name="documentation"
)

urls = [
    "https://docs.dlt.io",
    "https://cocoindex.io/docs",
    "https://dagster.io/docs"
]

asyncio.run(pipeline.run(scrape_documentation(urls)))
```

### LLM Extraction with Crawl4AI

```python
from crawl4ai import AsyncWebCrawler
from crawl4ai.extraction_strategy import LLMExtractionStrategy

# Define extraction schema
schema = {
    "name": "APIEndpoint",
    "fields": [
        {"name": "method", "type": "string", "description": "HTTP method"},
        {"name": "path", "type": "string", "description": "API path"},
        {"name": "description", "type": "string", "description": "What it does"},
        {"name": "parameters", "type": "array", "description": "Query params"}
    ]
}

async def extract_api_docs(url: str):
    extraction = LLMExtractionStrategy(
        provider="openai/gpt-4",
        api_token="YOUR_API_KEY",
        schema=schema,
        instruction="Extract all API endpoints from this documentation page"
    )

    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(
            url=url,
            extraction_strategy=extraction
        )

        return result.extracted_content  # Structured JSON
```

---

## Modular Component Design

### Three-Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│          INTERFACE LAYER (Framework-Specific)               │
│  Dagster assets, DLT resources, Marimo cells, Typer CLI    │
├─────────────────────────────────────────────────────────────┤
│          ADAPTER LAYER (Thin Wrappers, 5-20 lines)          │
│  @dagster_asset, @dlt_resource, @marimo_cell, @typer_cmd   │
├─────────────────────────────────────────────────────────────┤
│          CORE LAYER (Pure Python + Pydantic)                │
│  Business logic, models, validation - NO framework imports  │
└─────────────────────────────────────────────────────────────┘
```

### Core Layer (Pure Python)

```python
# core/models.py
from pydantic import BaseModel
from datetime import datetime

class ProcessedData(BaseModel):
    id: str
    content: str
    processed_at: datetime
    metadata: dict = {}

class ProcessingConfig(BaseModel):
    chunk_size: int = 1000
    overlap: int = 200
    model: str = "all-MiniLM-L6-v2"
```

```python
# core/functions.py
from .models import ProcessedData, ProcessingConfig

def process_data(raw_data: dict, config: ProcessingConfig) -> ProcessedData:
    """Pure Python function - no framework dependencies"""
    # Business logic here
    return ProcessedData(
        id=raw_data["id"],
        content=raw_data["content"],
        processed_at=datetime.now(),
        metadata={"chunk_size": config.chunk_size}
    )

def validate_input(data: dict) -> bool:
    """Validation logic - reusable across frameworks"""
    return "id" in data and "content" in data
```

### Adapter Layer

```python
# adapters/dagster.py
from dagster import asset, AssetExecutionContext
from core.functions import process_data, validate_input
from core.models import ProcessingConfig

@asset(description="Process data using core logic")
def processed_data_asset(context: AssetExecutionContext, raw_data: dict):
    """Thin wrapper around core function"""
    config = ProcessingConfig()
    if validate_input(raw_data):
        result = process_data(raw_data, config)
        context.log.info(f"Processed: {result.id}")
        return result.dict()
    raise ValueError("Invalid input data")
```

```python
# adapters/dlt.py
import dlt
from core.functions import process_data, validate_input
from core.models import ProcessingConfig

@dlt.resource(name="processed_data")
def processed_data_resource(raw_items: list[dict]):
    """Thin wrapper for DLT pipeline"""
    config = ProcessingConfig()
    for item in raw_items:
        if validate_input(item):
            result = process_data(item, config)
            yield result.dict()
```

```python
# adapters/typer.py
import typer
from rich import print
from core.functions import process_data, validate_input
from core.models import ProcessingConfig

app = typer.Typer()

@app.command()
def process(input_file: str, chunk_size: int = 1000):
    """CLI wrapper around core logic"""
    config = ProcessingConfig(chunk_size=chunk_size)
    # Load and process data
    print(f"[green]Processing with chunk_size={chunk_size}[/green]")
```

### Benefits

| Metric | Traditional | Modular |
|--------|-------------|---------|
| Code reuse | ~30% | ~100% |
| Adapter duplication | 80%+ | <5% |
| Test complexity | High | Low (test core only) |
| Framework migration | Days | Hours |

---

## Framework Adapter Patterns

### Dagster Adapter

```python
from dagster import asset, op, job, Definitions
from core.functions import fetch_data, transform_data, load_data

@asset(description="Fetch raw data from source")
def raw_data():
    return fetch_data()

@asset(deps=[raw_data])
def transformed_data(raw_data):
    return transform_data(raw_data)

@asset(deps=[transformed_data])
def loaded_data(transformed_data):
    return load_data(transformed_data)

defs = Definitions(assets=[raw_data, transformed_data, loaded_data])
```

### DLT Adapter

```python
import dlt
from core.functions import fetch_data, transform_data

@dlt.source
def my_source():
    @dlt.resource(name="raw_data", write_disposition="replace")
    def raw_data_resource():
        yield from fetch_data()

    @dlt.transformer(data_from=raw_data_resource)
    def transformed_data(items):
        for item in items:
            yield transform_data(item)

    return raw_data_resource, transformed_data

pipeline = dlt.pipeline(
    pipeline_name="my_pipeline",
    destination="duckdb"
)

pipeline.run(my_source())
```

### Marimo Adapter

```python
import marimo as mo
from core.functions import fetch_data, create_visualization

@mo.cell
def data_cell():
    """Fetch and display data"""
    data = fetch_data()
    return mo.ui.table(data)

@mo.cell
def viz_cell(data_cell):
    """Create visualization from data"""
    fig = create_visualization(data_cell.value)
    return mo.ui.plotly(fig)
```

### Typer CLI Adapter

```python
import typer
from rich.console import Console
from rich.table import Table
from core.functions import fetch_data, search_data

app = typer.Typer(help="Data CLI")
console = Console()

@app.command()
def list_data(limit: int = 10):
    """List data from source"""
    data = fetch_data()[:limit]
    table = Table(title="Data")
    for col in data[0].keys():
        table.add_column(col)
    for row in data:
        table.add_row(*[str(v) for v in row.values()])
    console.print(table)

@app.command()
def search(query: str, top_k: int = 5):
    """Search data"""
    results = search_data(query, top_k)
    for r in results:
        console.print(f"[green]{r['title']}[/green]: {r['score']:.3f}")
```

---

## Dagster Integration

### Asset-Based Pipeline

```python
from dagster import asset, Definitions, ScheduleDefinition, define_asset_job

@asset(description="Clone GitHub repository")
def cloned_repo(context):
    import subprocess
    repo_url = context.op_config.get("repo_url", "https://github.com/org/repo")
    subprocess.run(["git", "clone", "--depth", "1", repo_url, "/tmp/repo"])
    return {"path": "/tmp/repo"}

@asset(deps=[cloned_repo], description="Index repository with CocoIndex")
def indexed_repo(context, cloned_repo):
    import cocoindex
    # Run CocoIndex flow
    flow = cocoindex.get_flow("code_index")
    flow.run(path=cloned_repo["path"])
    return {"indexed": True}

@asset(deps=[indexed_repo], description="Upload to R2")
def uploaded_data(context, indexed_repo):
    # Upload indexed data to R2
    return {"bucket": "my-bucket", "key": "indexed/repo"}

# Job and schedule
index_job = define_asset_job("index_github_repo")
daily_schedule = ScheduleDefinition(
    job=index_job,
    cron_schedule="0 2 * * *"  # 2 AM daily
)

defs = Definitions(
    assets=[cloned_repo, indexed_repo, uploaded_data],
    schedules=[daily_schedule]
)
```

### DLT + Dagster Integration

```python
from dagster import asset
import dlt

@asset
def github_data():
    """Load GitHub data with DLT"""
    pipeline = dlt.pipeline(
        pipeline_name="github",
        destination="duckdb",
        dataset_name="github_data"
    )

    @dlt.resource(write_disposition="merge", primary_key="id")
    def issues():
        import requests
        resp = requests.get("https://api.github.com/repos/org/repo/issues")
        yield resp.json()

    load_info = pipeline.run(issues())
    return {"rows_loaded": load_info.loads[0].row_counts}
```

### Sensor for Change Detection

```python
from dagster import sensor, RunRequest, SkipReason
import requests

@sensor(job=index_job)
def github_commit_sensor(context):
    """Trigger reindex when new commits appear"""
    resp = requests.get("https://api.github.com/repos/org/repo/branches/main")
    latest_sha = resp.json()["commit"]["sha"]

    if latest_sha != context.cursor:
        context.update_cursor(latest_sha)
        return RunRequest(
            run_key=f"commit_{latest_sha}",
            run_config={"ops": {"cloned_repo": {"config": {"sha": latest_sha}}}}
        )

    return SkipReason("No new commits")
```

---

## CLI Development with Typer

### Basic CLI Structure

```python
# cli/main.py
import typer
from rich.console import Console

app = typer.Typer(help="Data Unified CLI")
console = Console()

# Import subcommands
from cli.commands import github, docs, query

app.add_typer(github.app, name="github", help="GitHub operations")
app.add_typer(docs.app, name="docs", help="Documentation operations")
app.add_typer(query.app, name="query", help="Query operations")

@app.command()
def status():
    """Show system status"""
    console.print("[green]System operational[/green]")

if __name__ == "__main__":
    app()
```

### Subcommand Module

```python
# cli/commands/github.py
import typer
from rich.console import Console
from rich.table import Table
from core.functions import clone_repo, index_repo, search_code

app = typer.Typer()
console = Console()

@app.command()
def clone(
    repo_url: str = typer.Argument(..., help="GitHub repository URL"),
    branch: str = typer.Option("main", help="Branch to clone")
):
    """Clone a GitHub repository"""
    with console.status(f"Cloning {repo_url}..."):
        result = clone_repo(repo_url, branch)
    console.print(f"[green]Cloned to {result['path']}[/green]")

@app.command()
def index(
    path: str = typer.Argument(..., help="Path to repository"),
    force: bool = typer.Option(False, "--force", help="Force reindex")
):
    """Index repository for search"""
    with console.status("Indexing..."):
        result = index_repo(path, force)
    console.print(f"[green]Indexed {result['files']} files[/green]")

@app.command()
def search(
    query: str = typer.Argument(..., help="Search query"),
    top_k: int = typer.Option(5, help="Number of results")
):
    """Search indexed code"""
    results = search_code(query, top_k)

    table = Table(title="Search Results")
    table.add_column("File")
    table.add_column("Score")
    table.add_column("Snippet")

    for r in results:
        table.add_row(r["file"], f"{r['score']:.3f}", r["snippet"][:50])

    console.print(table)
```

### Rich Output Formatting

```python
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.progress import track

console = Console()

def display_results(results: list):
    """Display results in a formatted table"""
    table = Table(title="Results", show_header=True)
    table.add_column("ID", style="cyan")
    table.add_column("Name", style="green")
    table.add_column("Score", justify="right")

    for r in results:
        table.add_row(r["id"], r["name"], f"{r['score']:.4f}")

    console.print(table)

def display_summary(summary: dict):
    """Display summary in a panel"""
    content = "\n".join([f"[bold]{k}:[/bold] {v}" for k, v in summary.items()])
    console.print(Panel(content, title="Summary", border_style="blue"))

def process_with_progress(items: list):
    """Process items with progress bar"""
    results = []
    for item in track(items, description="Processing..."):
        results.append(process_item(item))
    return results
```

---

## Incremental Processing

### DLT Incremental Loading

```python
import dlt
from datetime import datetime

@dlt.resource(write_disposition="append")
def incremental_data(
    updated_at=dlt.sources.incremental(
        "updated_at",
        initial_value=datetime(2024, 1, 1)
    )
):
    """Only load records updated since last run"""
    # Query uses last_value to filter
    query = f"""
        SELECT * FROM source_table
        WHERE updated_at > '{updated_at.last_value}'
        ORDER BY updated_at
    """
    for batch in fetch_batches(query):
        yield batch

pipeline = dlt.pipeline(
    pipeline_name="incremental",
    destination="duckdb"
)

# First run: loads all since 2024-01-01
# Subsequent runs: only new records
pipeline.run(incremental_data())
```

### CocoIndex Incremental Indexing

```python
import cocoindex

@cocoindex.flow_def(name="incremental_code")
def incremental_flow(flow_builder, data_scope):
    """CocoIndex automatically tracks state in Postgres"""

    data_scope["files"] = flow_builder.add_source(
        cocoindex.sources.LocalFile(
            path="/repo",
            included_patterns=["*.py"]
        )
    )

    # Only changed files are reprocessed on subsequent runs
    with data_scope["files"].row() as file:
        file["chunks"] = file["content"].transform(
            cocoindex.functions.SplitRecursively()
        )

        with file["chunks"].row() as chunk:
            chunk["embedding"] = chunk["text"].transform(
                cocoindex.functions.SentenceTransformerEmbed()
            )

            data_scope.add_collector("index").collect(
                filename=file["filename"],
                text=chunk["text"],
                embedding=chunk["embedding"]
            )

    # Export with primary key for upsert behavior
    data_scope.get_collector("index").export(
        "code_index",
        cocoindex.storages.Postgres(),
        primary_key_fields=["filename", "text"]
    )

# Run flow
flow = incremental_flow()
flow.run()  # Full index
flow.run()  # Only changes
```

### Dagster Sensor-Based Updates

```python
from dagster import sensor, RunRequest
import os
from hashlib import md5

@sensor(job=reindex_job, minimum_interval_seconds=60)
def file_change_sensor(context):
    """Detect file changes and trigger reindex"""
    watch_dir = "/data/source"
    current_hash = get_directory_hash(watch_dir)

    if current_hash != context.cursor:
        context.update_cursor(current_hash)
        return RunRequest(run_key=f"change_{current_hash[:8]}")

    return SkipReason("No changes detected")

def get_directory_hash(path: str) -> str:
    """Hash directory contents for change detection"""
    hasher = md5()
    for root, dirs, files in os.walk(path):
        for f in sorted(files):
            filepath = os.path.join(root, f)
            hasher.update(f.encode())
            hasher.update(str(os.path.getmtime(filepath)).encode())
    return hasher.hexdigest()
```

---

## Production Deployment

### Docker Compose Stack

```yaml
# docker-compose.yml
version: "3.8"

services:
  dagster-webserver:
    image: dagster/dagster:latest
    ports:
      - "3000:3000"
    environment:
      DAGSTER_HOME: /opt/dagster
    volumes:
      - ./dagster_project:/opt/dagster/app
      - dagster-storage:/opt/dagster/storage

  dagster-daemon:
    image: dagster/dagster:latest
    command: dagster-daemon run
    environment:
      DAGSTER_HOME: /opt/dagster
    volumes:
      - ./dagster_project:/opt/dagster/app
      - dagster-storage:/opt/dagster/storage

  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: dagster
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: dagster
    volumes:
      - postgres-data:/var/lib/postgresql/data

  lancedb:
    image: lancedb/lancedb:latest
    volumes:
      - lancedb-data:/data

  memgraph:
    image: memgraph/memgraph:latest
    ports:
      - "7687:7687"
    volumes:
      - memgraph-data:/var/lib/memgraph

volumes:
  dagster-storage:
  postgres-data:
  lancedb-data:
  memgraph-data:
```

### Environment Configuration

```bash
# .env
# R2 Storage
R2_ACCESS_KEY_ID=your_access_key
R2_SECRET_ACCESS_KEY=your_secret_key
R2_ACCOUNT_ID=your_account_id
R2_BUCKET_NAME=data-lake

# Database
POSTGRES_HOST=postgres
POSTGRES_USER=dagster
POSTGRES_PASSWORD=secret
POSTGRES_DB=dagster

# LLM
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...

# Embedding
VOYAGE_API_KEY=pa-...
EMBEDDING_MODEL=voyage-code-3
```

### Production Checklist

- [ ] Use secrets manager (1Password, Vault) for credentials
- [ ] Enable Dagster schedules and sensors
- [ ] Set up monitoring (Grafana, Prometheus)
- [ ] Configure log aggregation
- [ ] Enable HTTPS for web interfaces
- [ ] Set up backup for Postgres and vector DBs
- [ ] Configure resource limits in Docker
- [ ] Set up CI/CD for deployments

---

## References

**Sources:**
- data-stack-tutorials-and-examples.md
- unified-modular-component-design-dagster-dlt-marimo-typer.md
- stage-1-incremental-update-pipeline.md
- stage-2-technical-implementation-outline.md

**External:**
- [DuckDB Documentation](https://duckdb.org/docs)
- [DLT Documentation](https://dlthub.com/docs)
- [Dagster Documentation](https://docs.dagster.io)
- [CocoIndex Documentation](https://cocoindex.io/docs)
- [Crawl4AI Documentation](https://crawl4ai.com)

**Last Updated:** November 29, 2024
