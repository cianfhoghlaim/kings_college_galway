# Knowledge Graph Infrastructure

This directory consolidates research on temporal knowledge graphs, graph databases, and semantic memory systems for AI agents.

## Overview

The research covers the complete stack for knowledge graph construction and querying:
- **Graph Databases**: FalkorDB, Neo4j, Memgraph comparisons
- **Temporal Knowledge**: Graphiti bi-temporal modeling
- **Entity Resolution**: LLM-based deduplication strategies
- **Hybrid Search**: Combining semantic, keyword, and graph traversal
- **Visualization**: Tools for exploring knowledge structures

## Documents in this Category

| Document | Focus | Key Technologies |
|----------|-------|------------------|
| `graphiti-temporal-graphs.md` | Bi-temporal knowledge modeling | Graphiti, FalkorDB, Pydantic |
| `graph-database-comparison.md` | FalkorDB vs Neo4j vs Memgraph | Query performance, Python clients |
| `entity-resolution.md` | Deduplication and linking strategies | LLM extraction, custom types |
| `visualization-tools.md` | Graph exploration interfaces | Cognee, NetworkX, D3.js |

## Key Architectural Decisions

### 1. Graphiti Episode Architecture

```
User Conversation / Document
    ↓
┌─────────────────────────────────────┐
│         EPISODE INGESTION           │
│   add_episode(body, reference_time) │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│         LLM EXTRACTION              │
│   - Named Entity Recognition        │
│   - Relationship Extraction         │
│   - Fact Timestamping              │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│       ENTITY RESOLUTION             │
│   - Merge "Maths" ↔ "Mathematics"   │
│   - Link cross-episode entities     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│      TEMPORAL GRAPH STORAGE         │
│   - Valid Time (when fact true)     │
│   - Transaction Time (when stored)  │
└─────────────────────────────────────┘
```

### 2. Bi-Temporal Modeling

| Time Dimension | Definition | Query Example |
|----------------|------------|---------------|
| **Valid Time** | When fact was true in real world | "What was the curriculum in 2015?" |
| **Transaction Time** | When fact was recorded | "What did we know on Tuesday?" |

```python
await graphiti.add_episode(
    name="curriculum_2024",
    episode_body=text,
    source=EpisodeType.text,
    reference_time=datetime(2024, 9, 1),  # Valid time
    entity_types=[CurriculumStandard, LearningOutcome]
)
```

### 3. Custom Entity Types

```python
from pydantic import BaseModel, Field

class MathTheorem(BaseModel):
    name: str = Field(description="Theorem name, e.g., Pythagoras")
    latex_def: str = Field(description="LaTeX definition")

class ExamTopic(BaseModel):
    name: str = Field(description="Curriculum topic")
    code: str = Field(description="Curriculum code, e.g., C1.2")

class ExamQuestion(BaseModel):
    question_id: str
    topic: ExamTopic
    difficulty: str = Field(description="H1-H8 scale")
    year: int
```

## Quick Reference

### Graphiti Episode Ingestion

```python
from graphiti_core import Graphiti, EpisodeType

client = Graphiti("falkor://localhost:6379")

# Text episode
await client.add_episode(
    name="exam_2024_math",
    episode_body=markdown_text,
    source=EpisodeType.text,
    source_description="Leaving Certificate Math Paper 2024",
    reference_time=exam_date,
    entity_types=[MathTheorem, ExamTopic, ExamQuestion]
)

# JSON episode
await client.add_episode(
    name="student_profile",
    episode_body={"name": "Alice", "subjects": [...]},
    source=EpisodeType.json,
    reference_time=datetime.now()
)
```

### Hybrid Search

```python
# Combine semantic + keyword + graph traversal
results = await client.search(
    query="geometry questions involving circles",
    search_type="hybrid",
    filters={
        "entity_type": "ExamQuestion",
        "valid_time_after": datetime(2020, 1, 1)
    },
    limit=10
)

# Time travel query
historical = await client.search(
    query="Matrix multiplication definition",
    reference_time=datetime(2015, 6, 1)  # As known in 2015
)
```

### FalkorDB Direct Queries

```cypher
// Find all questions linked to a theorem
MATCH (t:Theorem {name: "Pythagoras"})
      <-[:TESTS]-(q:Question)
WHERE q.year >= 2020
RETURN q.question_id, q.difficulty, q.year
ORDER BY q.year DESC

// Entity resolution - merge synonyms
MATCH (e1:Topic {name: "Maths"}), (e2:Topic {name: "Mathematics"})
CALL apoc.refactor.mergeNodes([e1, e2])
YIELD node
RETURN node
```

## Graph Database Comparison

| Feature | FalkorDB | Neo4j | Memgraph |
|---------|----------|-------|----------|
| **Architecture** | Redis-based | JVM | C++ |
| **Memory Model** | In-memory | Hybrid | In-memory |
| **Query Language** | Cypher | Cypher | Cypher |
| **Python Client** | `falkordb-py` | `neo4j` | `gqlalchemy` |
| **Graphiti Support** | Native | Native | Community |
| **License** | Source-available | Commercial/Community | BSL |
| **Best For** | Low-latency, embedded | Enterprise, tooling | Real-time analytics |

## Source Files Consolidated

This category merges content from:
- `Visualizing Cognee and Graphiti Graphs.md`
- Graphiti sections from `Dagster Orchestration for Cocoindex, Graphiti.md`
- Graphiti sections from `BAML, Graphiti, Tanstack AI Pipeline.md`

## Visualization Approaches

### NetworkX + Matplotlib

```python
import networkx as nx
import matplotlib.pyplot as plt

# Export from Graphiti to NetworkX
G = nx.DiGraph()
for entity in entities:
    G.add_node(entity.id, label=entity.name, type=entity.type)
for edge in edges:
    G.add_edge(edge.source, edge.target, relation=edge.type)

# Draw
pos = nx.spring_layout(G)
nx.draw(G, pos, with_labels=True, node_color='lightblue')
plt.show()
```

### Cognee Integration

```python
from cognee import cognify, search

# Build knowledge graph from documents
await cognify([document1, document2])

# Search with reasoning
results = await search("What topics connect calculus to physics?")
```

## Implementation Priorities

### Phase 1: Graph Foundation
1. Deploy FalkorDB (or Neo4j)
2. Configure Graphiti client
3. Define domain entity types (Pydantic)

### Phase 2: Ingestion Pipeline
1. Integrate with Dagster assets
2. Implement episode batching
3. Configure entity resolution

### Phase 3: Query Interface
1. Implement hybrid search
2. Add time-travel queries
3. Build MCP tool wrapper

### Phase 4: Visualization
1. Export to NetworkX
2. Build interactive dashboard
3. Integrate with Streamlit UI
