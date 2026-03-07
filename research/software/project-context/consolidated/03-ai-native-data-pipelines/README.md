# AI-Native Data Pipelines

This directory consolidates research on modern data engineering infrastructure for AI-ready data production, including ETL/ELT frameworks, orchestration patterns, and real-time lakehouse architectures.

## Overview

The research covers the complete data pipeline stack for building semantic knowledge systems:
- **BAML Integration**: Schema-first AI output validation with dlt and Pydantic
- **Orchestration**: Dagster asset-based workflows with dynamic partitioning
- **Semantic Indexing**: CocoIndex for incremental vector indexing
- **Real-Time Lakehouse**: OLake, Lakekeeper, RisingWave stack
- **Metadata Management**: DuckDB-backed control planes

## Documents in this Category

| Document | Focus | Key Technologies |
|----------|-------|------------------|
| `baml-dlt-integration.md` | Schema-first ETL with AI validation | BAML, dlt, Pydantic, Zod, TanStack AI |
| `dagster-orchestration.md` | Asset-based pipeline orchestration | Dagster, CocoIndex, Graphiti, Marker |
| `metadata-control-plane.md` | Dynamic source management | DuckDB, Crawl4ai, TMX standards |
| `lakehouse-architecture.md` | Real-time open data lakehouse | OLake, Lakekeeper, RisingWave, Iceberg |

## Key Architectural Decisions

### 1. Schema-First AI Development

```
BAML Definition (Single Source of Truth)
├── Python Layer
│   ├── Pydantic Models (validated)
│   ├── dlt Resources (schema hints)
│   └── Custom Destinations (FalkorDB, Graphiti)
└── TypeScript Layer
    ├── TypeScript Interfaces
    ├── Zod Schemas (ts-to-zod)
    └── TanStack AI Tools
```

### 2. Multi-Database Ingestion Strategy

| Database | Integration Method | Use Case |
|----------|-------------------|----------|
| PostgreSQL | dlt native | Relational storage |
| LanceDB | dlt adapter | Vector similarity |
| FalkorDB | Custom destination | Graph relationships |
| Graphiti | Custom destination | Temporal knowledge |
| DuckDB | Native Python | Metadata control plane |

### 3. Real-Time Lakehouse Stack

```
Transaction (Source DB)
    ↓
OLake (Go-based CDC)
    ↓ Parquet to S3
Lakekeeper (Rust REST Catalog)
    ↓ Atomic commits
RisingWave (Streaming SQL)
    ↓ Materialized views
Analytics/ML Applications
```

## Source Files Consolidated

This category merges content from:
- `BAML, DLT, and AI Workflow Integration.md`
- `Dagster Orchestration for Cocoindex, Graphiti.md`
- `Managing Diverse Data Sources for Pipelines.md`
- `Integrating Olake, Lakekeeper, RisingWave.md`
- `BAML, Graphiti, Tanstack AI Pipeline.md`
- `BAML for Syllabus-Driven Data Extraction.md`

## Quick Reference

### Tool Selection Matrix

| Task | Tool | Rationale |
|------|------|-----------|
| PDF Extraction | Marker | LaTeX preservation, 4-10x faster than Nougat |
| Schema Validation | BAML | Compile-time verification, SAP algorithm |
| ETL Orchestration | dlt | Pydantic schema inference, auto-normalization |
| Workflow Orchestration | Dagster | Asset-based, dynamic partitions, sensors |
| Semantic Chunking | CocoIndex | Tree-sitter syntax-aware, incremental |
| Temporal Knowledge | Graphiti | Bi-temporal graphs, entity resolution |
| Metadata Store | DuckDB | In-process OLAP, JSON support |
| Real-Time Ingestion | OLake | Go-based, 300k+ rows/sec, direct Iceberg |
| Catalog Management | Lakekeeper | Rust, credential vending, OpenFGA |
| Streaming Compute | RisingWave | PostgreSQL-compatible, Iceberg native |

### Performance Benchmarks

| Operation | Tool | Throughput |
|-----------|------|------------|
| CDC Replication | OLake | >300,000 rows/sec |
| PDF Extraction | Marker | ~10 pages/sec |
| Vector Embedding | CocoIndex | Incremental (only changed) |
| Metadata Queries | DuckDB | Sub-millisecond |
| Commit Latency | Lakekeeper | Deterministic (no GC) |

### Configuration Patterns

**dlt Resource with BAML Schema:**
```python
@dlt.resource(
    name="research_insights",
    write_disposition="merge",
    primary_key="id",
    columns=ResearchInsight  # BAML-generated Pydantic
)
def extract_insights():
    for text in texts:
        insight = baml.ExtractInsight(text)
        yield insight
```

**Dagster Dynamic Partition:**
```python
exam_paper_partitions = DynamicPartitionsDefinition(name="exam_papers")

@asset(partitions_def=exam_paper_partitions)
def extracted_markdown(context):
    partition_key = context.partition_key
    return marker.process_pdf(partition_key)
```

## Implementation Priorities

### Phase 1: Schema Foundation
1. Define BAML schemas for data entities
2. Configure dual-target code generation (Python/TypeScript)
3. Set up dlt pipelines with Pydantic schema hints

### Phase 2: Orchestration Layer
1. Implement Dagster asset factories
2. Configure dynamic partitions for file ingestion
3. Add sensors for event-driven automation

### Phase 3: Semantic Intelligence
1. Integrate CocoIndex for vector indexing
2. Connect Graphiti for temporal knowledge graphs
3. Implement hybrid retrieval strategies

### Phase 4: Real-Time Lakehouse
1. Deploy OLake for CDC ingestion
2. Configure Lakekeeper for catalog management
3. Set up RisingWave for streaming analytics
