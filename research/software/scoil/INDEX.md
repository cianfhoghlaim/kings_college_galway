# Data Research - Consolidated Index

This directory contains consolidated research for the data layer of the hackathon platform.

## Directory Structure

```
data/consolidated/
├── 00-overview/           # Architecture & integration guides
│   ├── ARCHITECTURE.md    # Core data stack architecture
│   ├── AI_MEMORY.md       # Agent & knowledge graph patterns
│   ├── IMPLEMENTATION_GUIDE.md
│   ├── SCHEMAS_AND_TYPES.md
│   └── SOURCES.md
├── 01-ingestion-pipelines/  # DLT, Crawl4AI, OLake patterns
├── 02-storage-engines/      # DuckDB, LanceDB, Iceberg
├── 03-transformation/       # Ibis, SQLMesh, feature engineering
└── 04-analytics/            # Visualization, BI, dashboards
```

## Related Skills

Tool-specific documentation has been moved to `.claude/skills/`:

| Tool | Skill Location | Purpose |
|------|----------------|---------|
| DLT | `.claude/skills/dlt/` | Data ingestion pipelines |
| Crawl4AI | `.claude/skills/crawl4ai/` | Web scraping |
| Dagster | `.claude/skills/dagster/` | Pipeline orchestration |
| DuckDB | `.claude/skills/duckdb/` | Analytics engine |
| LanceDB | `.claude/skills/lancedb/` | Vector database |
| Cognee | `.claude/skills/cognee/` | Knowledge graphs |
| Ibis | `.claude/skills/ibis/` | Portable dataframes |
| Feast | `.claude/skills/feast/` | Feature store |
| Evidence | `.claude/skills/evidence/` | BI dashboards |
| Marimo | `.claude/skills/marimo/` | Reactive notebooks |
| OLake | `.claude/skills/olake/` | CDC replication |
| RisingWave | `.claude/skills/risingwave/` | Streaming SQL |
| Memgraph | `.claude/skills/memgraph/` | Graph database |
| Pydantic | `.claude/skills/pydantic/` | Data validation |
| CocoIndex | `.claude/skills/cocoindex/` | Incremental indexing |
| DuckLake | `.claude/skills/ducklake/` | SQL table format |
| ChunkHound | `.claude/skills/chunkhound/` | Document chunking |
| Firecrawl | `.claude/skills/firecrawl/` | Web crawling |

## Quick Links

- **Architecture Overview**: `00-overview/ARCHITECTURE.md`
- **AI Memory Patterns**: `00-overview/AI_MEMORY.md`
- **Implementation Guide**: `00-overview/IMPLEMENTATION_GUIDE.md`
- **Type System**: `00-overview/SCHEMAS_AND_TYPES.md`

## Archive

Original skill-specific research files are archived at:
`/research/archive/data-skills/`
