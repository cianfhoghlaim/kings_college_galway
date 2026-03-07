# BAML-dlt Integration: Schema-First AI Workflow Architecture

## Executive Summary

This document details the integration of BAML (Boundary AI Markup Language) with dlt (Data Load Tool) to create a unified schema architecture that bridges the gap between probabilistic LLM outputs and deterministic data systems. The approach treats BAML as the single source of truth, with generated Pydantic models driving dlt pipeline schema inference.

---

## 1. The Schema Engineering Paradigm

### 1.1 From Prompt Engineering to Schema Engineering

Traditional prompt engineering is brittle - a model update or slight input variation can break downstream parsers. BAML represents the maturation to "schema engineering":

| Approach | Method | Failure Mode |
|----------|--------|--------------|
| **Prompt Engineering** | Craft English instructions for JSON | Model variations break parsers |
| **JSON Schema Validation** | Runtime schema checking | Token-heavy, slow |
| **BAML Schema Engineering** | Compile-time type definition + SAP parsing | Fail-fast, deterministic |

BAML's **Schema-Aligned Parsing (SAP)** algorithm allows robust parsing of imperfect LLM outputs in milliseconds, eliminating costly retry loops.

### 1.2 Architecture Overview

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

---

## 2. Dual-Target Code Generation

### 2.1 generators.baml Configuration

```baml
// baml_src/generators.baml

// Generator 1: Python Data Layer
generator python_client {
  output_type "python/pydantic"
  output_dir "../backend/baml_client"
  version "0.76.2"
  default_client_mode "async"  // High-throughput dlt ingestion
}

// Generator 2: TypeScript Application Layer
generator typescript_client {
  output_type "typescript"
  output_dir "../frontend/src/baml_client"
  version "0.76.2"
  default_client_mode "async"
}
```

Every `baml-cli generate` execution creates two semantically identical but language-specific libraries.

### 2.2 Complex Entity Definitions

```baml
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
  entities IdentifiedEntity[]  // Nested objects for Graph extraction
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

The `@description` annotations become Pydantic field descriptions (usable by dlt) and JSDoc comments in TypeScript.

---

## 3. dlt Integration: BAML-to-Pipeline Bridge

### 3.1 Resource Definition with Pydantic Schema

dlt's native Pydantic introspection turns BAML-generated classes into "Schema Hints":

```python
import dlt
from typing import Iterator
from backend.baml_client import b
from backend.baml_client.types import ResearchInsight

@dlt.source
def research_source(texts: list[str]):

    @dlt.resource(
        name="research_insights",
        write_disposition="merge",
        primary_key="id",
        columns=ResearchInsight  # Pydantic model defines schema
    )
    def extract_insights() -> Iterator:
        for text in texts:
            # BAML call returns validated Pydantic object
            insight = b.ExtractInsight(text)
            yield insight

    return extract_insights
```

BAML's SAP ensures objects are valid before dlt sees them - "fail-fast" prevents schema pollution.

### 3.2 Multi-Database Ingestion Strategy

| Database | Integration Method | Use Case |
|----------|-------------------|----------|
| **PostgreSQL** | dlt native destination | Relational storage |
| **DuckDB** | dlt native destination | Analytical queries |
| **LanceDB** | dlt adapter | Vector similarity search |
| **FalkorDB** | Custom destination | Graph relationships |
| **Graphiti** | Custom destination | Temporal knowledge |

#### LanceDB Vector Integration

```python
from dlt.destinations.adapters import lancedb_adapter

def configure_lancedb_pipeline():
    source = research_source(["..."])

    # Specify which fields to embed
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

#### FalkorDB Custom Destination

```python
import dlt
from falkordb import FalkorDB

@dlt.destination(batch_size=50)
def falkordb_destination(items, table_schema):
    """Load BAML objects into FalkorDB graph."""
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
        for entity in item.get('entities', []):
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

#### Graphiti Custom Destination

```python
from graphiti_core import Graphiti, EpisodeType
import asyncio

@dlt.destination(batch_size=10)
def graphiti_destination(items, table_schema):
    """Load data into Graphiti as temporal episodes."""
    async def _ingest_batch():
        client = Graphiti("falkor://localhost:6379")

        for item in items:
            await client.add_episode(
                name=f"insight_{item['id']}",
                episode_body=item,  # Pass entire Pydantic dict
                source=EpisodeType.json,
                source_description="BAML Extracted Research",
                reference_time=datetime.now()
            )

        await client.close()

    asyncio.run(_ingest_batch())
```

---

## 4. TypeScript Layer: BAML to Zod to TanStack

### 4.1 Automated Zod Generation

Since BAML generates TypeScript interfaces (not Zod schemas), bridge with `ts-to-zod`:

```json
{
  "scripts": {
    "generate:baml": "baml-cli generate",
    "generate:zod": "ts-to-zod --input ./src/baml_client/types.ts --output ./src/gen/zod.ts --skipValidation",
    "codegen": "npm run generate:baml && npm run generate:zod"
  }
}
```

### 4.2 TanStack AI Tool Integration

```typescript
import { toolDefinition } from '@tanstack/ai';
import { researchInsightSchema } from '../gen/zod';

export const saveInsightTool = toolDefinition({
  name: 'save_insight',
  description: 'Persists a validated research insight to the database.',
  inputSchema: researchInsightSchema,
  execute: async (insight) => {
    // 'insight' is fully typed as ResearchInsight
    console.log(`Saving insight: ${insight.title}`);
    return { success: true, id: insight.id };
  },
});
```

### 4.3 oRPC Integration

```typescript
import { os } from '@orpc/server';
import { researchInsightSchema } from '../gen/zod';
import { db } from '../db/drizzle';
import { insightsTable } from '../db/schema';

export const appRouter = os.router({
  submitInsight: os.procedure
    .input(researchInsightSchema)
    .handler(async ({ input }) => {
      await db.insert(insightsTable).values({
        id: input.id,
        title: input.title,
        summary: input.summary,
        publishedDate: input.published_date,
        entities: input.entities  // Store as JSONB
      });
      return { status: 'stored' };
    }),
});
```

---

## 5. Schema Evolution Workflow

### 5.1 Adding a New Field

**Step 1: Update BAML**
```baml
class ResearchInsight {
  // ...existing fields
  author string? @description("Primary author name")  // NEW
}
```

**Step 2: Run Codegen**
```bash
npm run codegen  # baml-cli generate && ts-to-zod
```

**Step 3: dlt Auto-Evolution**
On next pipeline run, dlt detects the new `author` field in Pydantic model and automatically performs `ALTER TABLE` on PostgreSQL.

**Step 4: Frontend Updates**
TypeScript compiler flags any handlers that need updating. Zod schema includes `.optional()` for backward compatibility.

---

## 6. Feature Matrix

| Component | Role | BAML Integration | Validation Timing |
|-----------|------|------------------|-------------------|
| **dlt (Core)** | Pipeline Orchestrator | Pydantic Model (Direct) | Runtime (Schema Contract) |
| **PostgreSQL** | Relational Store | dlt Native | Write-Time (DB Constraints) |
| **LanceDB** | Vector Store | dlt Adapter | Write-Time (Schema Check) |
| **FalkorDB** | Graph Store | Custom Destination | Write-Time (Graph Logic) |
| **Graphiti** | Agent Memory | Custom Destination | Ingestion-Time |
| **TanStack AI** | Tool Definitions | Zod (via ts-to-zod) | Generation-Time (LLM output) |
| **oRPC** | API RPC | Zod (via ts-to-zod) | Request-Time (API Boundary) |

---

## 7. Performance Considerations

### 7.1 Token Efficiency

BAML reduces prompt size by up to 40% compared to verbose JSON Schema, improving latency and cost.

### 7.2 Async Pipeline Architecture

Writing to multiple databases introduces latency. Recommended pattern:

```python
async def process_document(text):
    # Phase 1: BAML extraction (background worker)
    insight = await b.ExtractInsight(text)

    # Phase 2: Parallel database writes
    await asyncio.gather(
        postgres_pipeline.load(insight),
        lancedb_pipeline.load(insight),
        graphiti_client.add_episode(insight)
    )

    return insight
```

### 7.3 Latency vs Throughput

- BAML extraction should occur in background workers (Celery/Temporal)
- Frontend should use optimistic UI patterns
- Use dlt's asyncio features for extraction parallelism
- Serialize/batch loading phase to avoid rate limits

---

## 8. Implementation Priorities

### Phase 1: Schema Foundation
1. Define BAML schemas for core data entities
2. Configure dual-target code generation (Python/TypeScript)
3. Set up dlt pipelines with Pydantic schema hints

### Phase 2: Multi-Database Integration
1. Configure native destinations (PostgreSQL, LanceDB)
2. Implement custom destinations (FalkorDB, Graphiti)
3. Set up ts-to-zod automation

### Phase 3: Frontend Integration
1. Integrate Zod schemas with TanStack AI tools
2. Configure oRPC with BAML-derived schemas
3. Implement schema evolution workflow

---

## References

- BAML Documentation: https://docs.boundaryml.com
- dlt Resources: https://dlthub.com/docs/general-usage/resource
- LanceDB Adapter: https://dlthub.com/docs/dlt-ecosystem/destinations/lancedb
- ts-to-zod: https://github.com/fabien0102/ts-to-zod
- TanStack AI: https://github.com/TanStack/ai
