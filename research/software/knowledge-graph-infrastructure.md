# Knowledge Graph Infrastructure: Unified Architecture Guide

A comprehensive reference for building temporal knowledge graphs, entity resolution pipelines, and hybrid search systems for AI agents.

---

## Table of Contents

1. [Dual-Engine Architecture](#1-dual-engine-architecture)
2. [Temporal Knowledge Graphs](#2-temporal-knowledge-graphs)
3. [Entity Resolution](#3-entity-resolution)
4. [Database Comparison](#4-database-comparison)
5. [Hybrid Search](#5-hybrid-search)
6. [Implementation Reference](#6-implementation-reference)

---

## 1. Dual-Engine Architecture

### 1.1 The Architectural Imperative

Traditional RAG systems suffer from "contextual blindness" - treating information as static snapshots without temporal validity or structural relationships. The dual-engine architecture addresses this by segregating workloads across specialized graph databases:

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                   COGNEE ORCHESTRATION LAYER                 │
                    │            (API Interface / Memory Management)               │
                    └─────────────────────────────────────────────────────────────┘
                                              │
                         ┌────────────────────┴────────────────────┐
                         │                                         │
                         ▼                                         ▼
          ┌──────────────────────────────┐        ┌──────────────────────────────┐
          │     DYNAMIC MEMORY PLANE     │        │     STATIC ASSET PLANE       │
          │                              │        │                              │
          │  ┌────────────────────────┐  │        │  ┌────────────────────────┐  │
          │  │       GRAPHITI         │  │        │  │       COCOINDEX        │  │
          │  │  (Temporal Episodes)   │  │        │  │   (Asset Indexing)     │  │
          │  └────────────────────────┘  │        │  └────────────────────────┘  │
          │            │                 │        │            │                 │
          │            ▼                 │        │            ▼                 │
          │  ┌────────────────────────┐  │        │  ┌────────────────────────┐  │
          │  │       FALKORDB         │  │        │  │       MEMGRAPH         │  │
          │  │  (Sparse Matrix/Redis) │  │        │  │   (C++ In-Memory)      │  │
          │  │  Port: 6379            │  │        │  │   Port: 7687 (Bolt)    │  │
          │  └────────────────────────┘  │        │  └────────────────────────┘  │
          └──────────────────────────────┘        └──────────────────────────────┘
                         │                                         │
                         │         ┌─────────────────────┐         │
                         └────────>│      LANCEDB        │<────────┘
                                   │  (Vector Embeddings)│
                                   └─────────────────────┘
```

### 1.2 Engine Responsibilities

| Plane | Engine | Database | Data Type | Characteristics |
|-------|--------|----------|-----------|-----------------|
| **Dynamic Memory** | Graphiti | FalkorDB | Conversations, user preferences, temporal state | High write velocity, bi-temporal edges |
| **Static Assets** | Cocoindex | Memgraph | Codebases, documentation, PDFs | Slowly changing, deep algorithmic analysis |

### 1.3 Component Definitions

**Cocoindex (The Librarian)**
- Declarative ETL framework for "Asset Intelligence"
- Monitors repositories, detects changes, incrementally updates knowledge base
- Runs as background worker ensuring graph reflects current file state

**Cognee (The Orchestrator)**
- Memory management framework defining knowledge topology
- Orchestrates data flow from ingestion to retrieval
- Acts as "Operating System" for agent memory

**Graphiti (The Hippocampus)**
- Specialized graph engine for episodic and temporal memory
- Enforces ontology of Episodes, Entities, and Communities
- Simulates human-like memory consolidation

### 1.4 Why Two Databases?

The compatibility gaps between FalkorDB and Memgraph prevent a single-database solution:

| Issue | FalkorDB | Memgraph |
|-------|----------|----------|
| **Graphiti Support** | Native (optimized driver) | Failed PR #900 - test failures |
| **Cocoindex Support** | Experimental Bolt only | Native Bolt protocol |
| **Vector Index Syntax** | GraphBLAS-native | Different from Neo4j templates |
| **APOC Procedures** | Not supported | Partial support |

---

## 2. Temporal Knowledge Graphs

### 2.1 The Bi-Temporal Data Model

Graphiti implements rigorous bi-temporal modeling, tracking two distinct timelines for every fact:

| Dimension | Field | Definition | Query Example |
|-----------|-------|------------|---------------|
| **Valid Time** | `valid_at` | When fact was true in real world | "What was the curriculum in 2015?" |
| **Transaction Time** | `created_at` | When fact was recorded in system | "What did we know on Tuesday?" |
| **Expiration** | `invalid_at` | When fact ceased to be true | "When did the bonus points end?" |

This enables **retroactive corrections**: if an agent learns on Friday that a Tuesday meeting was cancelled, Graphiti updates the graph to reflect the cancelled status for Tuesday while retaining provenance that the system believed it was scheduled until Friday.

### 2.2 The Episode as Atomic Unit

Every piece of information enters as an Episode - a discrete event with temporal context:

```python
from graphiti_core import Graphiti, EpisodeType
from datetime import datetime

client = Graphiti("redis://localhost:6379", driver="falkordb")

# Text episode with reference time
await client.add_episode(
    name="curriculum_update_2024",
    episode_body="""
    The Leaving Certificate Mathematics syllabus now includes
    Financial Mathematics as a mandatory topic starting 2024.
    """,
    source=EpisodeType.text,
    source_description="NCCA Curriculum Update",
    reference_time=datetime(2024, 9, 1)  # Valid time
)

# JSON episode for structured data
await client.add_episode(
    name="student_profile",
    episode_body={
        "name": "Alice Murphy",
        "subjects": ["Mathematics", "Physics", "Chemistry"],
        "target_points": 550
    },
    source=EpisodeType.json,
    reference_time=datetime.now()
)
```

### 2.3 Edge Lifecycle with Temporal Properties

```python
class TemporalEdge:
    source: str           # Source entity UUID
    target: str           # Target entity UUID
    relation_type: str    # e.g., "TEACHES", "REQUIRES"
    fact: str             # Human-readable description
    valid_at: datetime    # When fact became true
    invalid_at: datetime  # When fact ceased being true (or None)
    created_at: datetime  # System ingestion time
    expired_at: datetime  # System expiration time (or None)
```

### 2.4 Hierarchical Memory Organization

```
┌─────────────────────────────────────────────────────────────────┐
│                    COMMUNITY SUBGRAPH                            │
│   High-level summaries: "DevOps Infrastructure"                 │
│   Reduces latency and token costs for concept queries           │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ Leiden/Louvain Clustering
                              │
┌─────────────────────────────────────────────────────────────────┐
│                  SEMANTIC ENTITY SUBGRAPH                        │
│   Entities + Edges with embeddings (1024 dimensions)            │
│   Enables hybrid search: vector + graph + keyword               │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │ LLM Extraction
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    EPISODIC SUBGRAPH                             │
│   Raw stream: chat logs, transactions, events                   │
│   Immutable ground truth for re-processing                      │
└─────────────────────────────────────────────────────────────────┘
```

### 2.5 Edge Invalidation Example

**Scenario:** Irish Leaving Certificate Maths Bonus Points policy changed over time.

```python
# 2011: No bonus points
await client.add_episode(
    name="maths_policy_2011",
    episode_body="Higher Level Maths grade H6 awards standard points only.",
    reference_time=datetime(2011, 9, 1)
)

# 2012: Bonus points introduced
await client.add_episode(
    name="maths_bonus_2012",
    episode_body="Higher Level Maths grade H6 or above now awards 25 bonus points.",
    reference_time=datetime(2012, 9, 1)
)
```

**Result in Graph:**
- Edge `Maths --[HAS_BONUS_POINTS]--> 0` with `invalid_at=2012-09-01`
- Edge `Maths --[HAS_BONUS_POINTS]--> 25` with `valid_at=2012-09-01`

---

## 3. Entity Resolution

### 3.1 The Identity Problem

Unstructured data contains inconsistent nomenclature:

| Source A | Source B | Source C |
|----------|----------|----------|
| "Maths" | "Mathematics" | "Project Maths" |
| "LC" | "Leaving Cert" | "Leaving Certificate" |
| "H1" | "Honours 1" | "Grade H1" |

Without resolution, the graph fragments with duplicate nodes.

### 3.2 The Cognification Pipeline

```
Unstructured Data
    │
    ▼
┌─────────────────────────────────────┐
│          INGESTION (.add)           │
│   Documents, code files, text       │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│       COGNIFICATION (.cognify)      │
│   - Chunking                        │
│   - Embedding generation            │
│   - LLM entity extraction           │
│   - Relationship mapping            │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│        ENTITY RESOLUTION            │
│   - Merge "Maths" <-> "Mathematics" │
│   - Link cross-document entities    │
│   - Canonical node creation         │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│         GRAPH STORAGE               │
│   NetworkX / FalkorDB / Neo4j       │
└─────────────────────────────────────┘
```

### 3.3 LLM-Based Resolution Strategy

```python
async def resolve_entities(extracted_entities: list[Entity]) -> list[Entity]:
    """Merge semantically identical entities."""
    resolved = []
    seen_canonical = {}

    for entity in extracted_entities:
        # Check if semantically similar to existing entity
        for canonical, variants in seen_canonical.items():
            if llm_judge_same_entity(entity.name, canonical):
                # Merge into existing canonical entity
                variants.append(entity)
                break
        else:
            # New canonical entity
            canonical_name = llm_canonicalize_name(entity.name)
            seen_canonical[canonical_name] = [entity]
            resolved.append(Entity(name=canonical_name, type=entity.type))

    return resolved
```

### 3.4 Resolution Examples

| Raw Input | Canonical Form | Reasoning |
|-----------|----------------|-----------|
| "Maths", "Mathematics", "Math" | `Subject:Mathematics` | Same subject |
| "H1", "Honours 1", "Grade H1" | `Grade:H1` | Same grading symbol |
| "Trinity", "TCD", "Trinity College Dublin" | `University:TCD` | Same institution |
| "Physics", "Applied Physics" | Separate entities | Different subjects |

### 3.5 Custom Ontology Definitions

```python
from pydantic import BaseModel, Field
from typing import Optional

class CurriculumStandard(BaseModel):
    """An educational curriculum standard or specification."""
    name: str = Field(description="Name of the curriculum standard")
    code: str = Field(description="Official code, e.g., MA-H-1.2")
    level: str = Field(description="Primary, Junior Cycle, Senior Cycle")
    subject: str = Field(description="Subject area")

class ExamQuestion(BaseModel):
    """A specific examination question."""
    question_id: str = Field(description="Unique identifier")
    year: int = Field(description="Examination year")
    paper: int = Field(description="Paper number (1 or 2)")
    level: str = Field(description="Higher, Ordinary, Foundation")
    marks: int = Field(description="Total marks available")

class MathTheorem(BaseModel):
    """A mathematical theorem or concept."""
    name: str = Field(description="Theorem name, e.g., Pythagoras")
    latex_def: str = Field(description="LaTeX definition")
    prerequisites: list[str] = Field(default=[], description="Required concepts")
```

### 3.6 Cognee Basic Usage

```python
import cognee

# Step 1: Add data
await cognee.add("Leaving Certificate Mathematics covers algebra, geometry, and calculus.")
await cognee.add("LC Maths also includes financial mathematics since 2024.")

# Step 2: Cognify (process and structure)
await cognee.cognify()

# Step 3: Search the structured knowledge
results = await cognee.search("What topics are in LC Mathematics?")

# Step 4: Visualize (debugging)
await cognee.visualize_graph("knowledge_graph.html")
```

---

## 4. Database Comparison

### 4.1 Feature Matrix

| Feature | FalkorDB | Neo4j | Memgraph |
|---------|----------|-------|----------|
| **Architecture** | Redis-based (GraphBLAS) | JVM-based | C++ In-Memory |
| **Memory Model** | Sparse matrices | Hybrid | Pointer chasing |
| **Query Language** | Cypher | Cypher | Cypher |
| **Python Client** | `falkordb-py` | `neo4j` | `gqlalchemy` |
| **Graphiti Support** | Native | Native | Community (failing) |
| **Vector Indexing** | Native HNSW | Plugin | Native |
| **License** | Source-available | Commercial/Community | BSL |
| **Best For** | Low-latency hybrid search | Enterprise tooling | Real-time analytics |

### 4.2 FalkorDB: The Sparse Matrix Engine

FalkorDB represents a radical departure from traditional graph databases by using **Linear Algebra** instead of pointer-based adjacency:

**GraphBLAS Architecture:**
- Graph stored as sparse adjacency matrices
- Nodes are matrix indices, edges are non-zero values
- Graph traversal = Matrix multiplication
- Leverages CPU SIMD instructions, avoids cache misses

**Vector-Native Integration:**
- HNSW indexing directly in matrix structure
- "Pre-filtering" where vector similarity acts as matrix mask
- Extremely efficient hybrid queries

```python
# FalkorDB connection
from graphiti_core import Graphiti

client = Graphiti(
    uri="redis://localhost:6379",
    driver="falkordb"
)
```

### 4.3 Memgraph: The C++ Powerhouse

**In-Memory Pointer Architecture:**
- Traditional object-oriented approach
- Node objects contain pointers to Relationship objects
- Exceptional for deep traversal and write-heavy workloads

**MAGE Library:**
- Built-in PageRank, Louvain community detection, Node2Vec
- Algorithms run in database kernel (not client-side)

```python
# Memgraph connection via gqlalchemy
from gqlalchemy import Memgraph

memgraph = Memgraph(
    host="localhost",
    port=7687,
    username="memgraph",
    password="memgraph"
)
```

### 4.4 Neo4j

**Enterprise Standard:**
- Most mature Cypher implementation
- Extensive tooling (Browser, Bloom, Aura)
- Graphiti native support

```python
# Neo4j connection
client = Graphiti(
    uri="neo4j://localhost:7687",
    user="neo4j",
    password="password"
)
```

### 4.5 Decision Matrix

| Criterion | Single DB (Memgraph) | Single DB (FalkorDB) | Dual Engine |
|-----------|---------------------|---------------------|-------------|
| **Graphiti Compatibility** | High Risk | Native | Native |
| **Cocoindex Compatibility** | Native | Medium Risk | Native |
| **System Complexity** | Low | Low | Medium |
| **Performance** | Risk of index failure | Risk of ETL failure | Optimal |

**Recommendation:** Adopt the Dual-Engine Architecture. Each workload hits its ideal engine.

---

## 5. Hybrid Search

### 5.1 Search Modes

Graphiti combines multiple retrieval strategies:

| Mode | Mechanism | Use Case |
|------|-----------|----------|
| **Semantic** | Vector similarity (embeddings) | "Questions about geometry" |
| **Keyword** | BM25 full-text search | "Exact term lookup" |
| **Graph Traversal** | Multi-hop relationship following | "What connects X to Y?" |
| **Temporal** | Time-bounded queries | "Facts valid in 2020" |

### 5.2 Vector + Graph Architecture

```
Document Chunk
    │
    ▼
┌─────────────────┐    ┌─────────────────┐
│   VECTOR STORE  │    │   GRAPH STORE   │
│   (Embeddings)  │    │   (Entities)    │
│                 │    │                 │
│   "Find similar │    │   "How is A     │
│    content"     │    │    related to   │
│                 │    │    B?"          │
└─────────────────┘    └─────────────────┘
         │                     │
         ▼                     ▼
    ┌─────────────────────────────────┐
    │        HYBRID SEARCH            │
    │   Vector similarity + Graph     │
    │   traversal combined            │
    └─────────────────────────────────┘
```

### 5.3 Search API

```python
# Hybrid search with temporal filtering
results = await client.search(
    query="geometry questions involving circles",
    search_type="hybrid",
    filters={
        "entity_type": "ExamQuestion",
        "valid_time_after": datetime(2020, 1, 1),
        "valid_time_before": datetime(2024, 12, 31)
    },
    limit=10
)

# Time travel query - knowledge as of a specific date
historical = await client.search(
    query="Matrix multiplication definition",
    reference_time=datetime(2015, 6, 1)  # As known in 2015
)
```

### 5.4 Search Result Structure

```python
from graphiti_core import SearchResult

# SearchResult contains:
# - nodes: List of matching entities
# - edges: Relationships connecting them
# - scores: Relevance scores (semantic + keyword + graph centrality)
# - context: Subgraph for visualization

for result in results.nodes:
    print(f"Entity: {result.name}")
    print(f"Type: {result.entity_type}")
    print(f"Valid: {result.valid_at} - {result.invalid_at or 'present'}")
    print(f"Score: {result.score}")
```

### 5.5 Cypher Queries for Complex Retrieval

```cypher
-- Find all questions linked to a theorem
MATCH (t:Theorem {name: "Pythagoras"})
      <-[:TESTS]-(q:Question)
WHERE q.year >= 2020
RETURN q.question_id, q.difficulty, q.year
ORDER BY q.year DESC

-- Entity resolution - merge synonyms
MATCH (e1:Topic {name: "Maths"}), (e2:Topic {name: "Mathematics"})
CALL apoc.refactor.mergeNodes([e1, e2])
YIELD node
RETURN node
```

---

## 6. Implementation Reference

### 6.1 Docker Compose Configuration

```yaml
version: "3.8"

services:
  # GRAPH DATABASE LAYER (Dual Engine)

  # FalkorDB: Dedicated for Graphiti (Agent Memory)
  falkordb:
    image: falkordb/falkordb:latest
    container_name: falkordb
    ports:
      - "6379:6379"    # Redis Protocol (Primary for Graphiti)
      - "3000:3000"    # FalkorDB Browser UI
    volumes:
      - ./data/falkordb:/data
    environment:
      - FALKORDB_ARGS="BOLT_PORT 7687"
    networks:
      - ai_network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Memgraph: Dedicated for Cocoindex (Static Knowledge)
  memgraph:
    image: memgraph/memgraph-platform:latest
    container_name: memgraph
    ports:
      - "7687:7687"    # Bolt Protocol
      - "7444:7444"    # HTTP Logs / WebSocket
      - "3001:3000"    # Memgraph Lab (remapped)
    environment:
      - MEMGRAPH_USER=memgraph
      - MEMGRAPH_PASSWORD=memgraph
    volumes:
      - ./data/memgraph:/var/lib/memgraph
    networks:
      - ai_network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:7444"]
      interval: 10s
      timeout: 5s
      retries: 5

  # STORAGE LAYER

  postgres:
    image: postgres:15
    container_name: postgres
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
      POSTGRES_DB: cognee_meta
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    networks:
      - ai_network

  # APPLICATION LAYER

  # Cocoindex: The ETL Worker
  cocoindex_worker:
    build:
      context: .
      dockerfile: Dockerfile.cocoindex
    container_name: cocoindex
    environment:
      - COCOINDEX_DATABASE_URL=postgresql://user:password@postgres:5432/cognee_meta
      - GRAPH_HOST=memgraph
      - GRAPH_PORT=7687
      - GRAPH_USER=memgraph
      - GRAPH_PASSWORD=memgraph
      - LANCEDB_URI=/app/data/lancedb
    volumes:
      - ./codebase:/app/codebase
      - ./data/lancedb:/app/data/lancedb
    depends_on:
      memgraph:
        condition: service_healthy
      postgres:
        condition: service_started
    networks:
      - ai_network

  # Cognee / Graphiti: The Agent API
  cognee_app:
    build:
      context: .
      dockerfile: Dockerfile.cognee
    container_name: cognee
    environment:
      - LLM_API_KEY=${LLM_API_KEY}
      - GRAPHITI_URI=falkor://falkordb:6379
      - GRAPH_DATABASE_PROVIDER=falkordb
    volumes:
      - ./data/lancedb:/app/data/lancedb
    depends_on:
      falkordb:
        condition: service_healthy
      postgres:
        condition: service_started
    networks:
      - ai_network

networks:
  ai_network:
    driver: bridge
```

### 6.2 Graphiti with FalkorDB Configuration

```python
# file: app/config/graph_store.py
import os
from graphiti_core import Graphiti
from graphiti_core.driver.falkordb_driver import FalkorDriver
from cognee.infrastructure.databases.graph.graph_store import GraphStore

class CustomGraphitiAdapter(GraphStore):
    def __init__(self):
        self.driver = FalkorDriver(
            host=os.getenv("FALKORDB_HOST", "falkordb"),
            port=int(os.getenv("FALKORDB_PORT", 6379))
        )
        self.graphiti = Graphiti(
            graph_driver=self.driver,
            llm_client=...  # Configured LLM Client
        )

    async def initialize(self):
        # CRITICAL: Creates vector indices in FalkorDB
        await self.graphiti.build_indices_and_constraints()
```

### 6.3 Cocoindex with Memgraph Configuration

```python
# file: app/cocoindex/pipelines.py
from cocoindex import flow_def, sources, targets
from cocoindex.targets import Neo4j, Neo4jConnectionSpec

@flow_def(name="CodebaseIngestion")
def ingestion_flow(flow, scope):
    # 1. Source: Watch the codebase directory
    scope["files"] = flow.add_source(sources.LocalFile(path="/app/codebase"))

    # ... (Transformations: chunking, embedding) ...

    # 2. Target: Export structure to Memgraph (using Neo4j target)
    scope["nodes"].export(
        "knowledge_graph",
        targets.Neo4j(
            connection=Neo4jConnectionSpec(
                url="bolt://memgraph:7687",
                user="memgraph",
                password="memgraph"
            ),
            mapping=targets.Mapping(...)
        )
    )
```

### 6.4 FastAPI Backend Integration

```python
from fastapi import FastAPI
import cognee
from networkx.readwrite import json_graph

app = FastAPI()

@app.post("/api/ingest")
async def ingest_document(content: str):
    """Ingest document into knowledge graph."""
    await cognee.add(content)
    await cognee.cognify()
    return {"status": "success"}

@app.get("/api/knowledge-graph")
async def get_graph_data():
    """Return full graph for visualization."""
    from cognee.infrastructure.databases.graph import get_graph_client

    client = get_graph_client()
    G = client.graph

    return json_graph.node_link_data(G)

@app.get("/api/search")
async def search_knowledge(query: str):
    """Search the knowledge graph."""
    results = await cognee.search(query)
    return {"results": results}
```

### 6.5 MCP Tool Wrapper

```python
from mcp.server import Server

app = Server("graphiti-memory")

@app.call_tool("add_memory")
async def add_memory(body: str, timestamp: str, source: str):
    """Add new memory to temporal knowledge graph."""
    await graphiti.add_episode(
        name=f"memory_{datetime.now().isoformat()}",
        episode_body=body,
        source=EpisodeType.text,
        source_description=source,
        reference_time=datetime.fromisoformat(timestamp)
    )
    return {"status": "success"}

@app.call_tool("search_memory")
async def search_memory(query: str, time_context: str = None):
    """Search temporal knowledge graph."""
    ref_time = datetime.fromisoformat(time_context) if time_context else None

    results = await graphiti.search(
        query=query,
        reference_time=ref_time,
        limit=10
    )

    return {
        "nodes": [n.dict() for n in results.nodes],
        "edges": [e.dict() for e in results.edges]
    }
```

### 6.6 BAML Integration for Extraction

```python
from baml_client import b
from graphiti_core import Graphiti

async def extract_and_ingest(text: str, reference_time: datetime):
    """Use BAML for extraction, Graphiti for persistence."""

    # BAML extracts structured data with guaranteed schema
    extracted = b.ExtractEducationEntities(text)

    # Convert BAML output to episode body
    episode_body = {
        "theorems": [t.dict() for t in extracted.theorems],
        "questions": [q.dict() for q in extracted.questions],
        "standards": [s.dict() for s in extracted.standards]
    }

    # Persist to temporal graph
    await graphiti.add_episode(
        name=f"extraction_{hash(text)[:8]}",
        episode_body=episode_body,
        source=EpisodeType.json,
        reference_time=reference_time
    )
```

### 6.7 Visualization: React Temporal Graph

```typescript
import ForceGraph2D from 'react-force-graph-2d';
import { useState, useEffect, useMemo } from 'react';

interface TemporalGraphProps {
  fullGraph: GraphData;
  timeRange: { min: Date; max: Date };
}

const TemporalGraph = ({ fullGraph, timeRange }: TemporalGraphProps) => {
  const [currentTime, setCurrentTime] = useState(timeRange.max.getTime());

  // Filter graph based on current time
  const displayedGraph = useMemo(() => {
    const activeLinks = fullGraph.links.filter(link => {
      const validFrom = new Date(link.valid_at).getTime();
      const validUntil = link.invalid_at
        ? new Date(link.invalid_at).getTime()
        : Infinity;
      return currentTime >= validFrom && currentTime < validUntil;
    });

    const activeNodeIds = new Set<string>();
    activeLinks.forEach(link => {
      activeNodeIds.add(link.source);
      activeNodeIds.add(link.target);
    });

    const activeNodes = fullGraph.nodes.filter(node =>
      activeNodeIds.has(node.id)
    );

    return { nodes: activeNodes, links: activeLinks };
  }, [fullGraph, currentTime]);

  return (
    <div>
      <TimeSlider
        min={timeRange.min.getTime()}
        max={timeRange.max.getTime()}
        value={currentTime}
        onChange={setCurrentTime}
      />
      <ForceGraph2D
        graphData={displayedGraph}
        nodeLabel="label"
        nodeAutoColorBy="group"
        linkLabel="relation"
        linkDirectionalArrowLength={3}
      />
    </div>
  );
};
```

### 6.8 Backend Export for Visualization

```python
async def export_search_result(results: SearchResult) -> dict:
    """Export Graphiti search results for visualization."""
    nodes = []
    links = []
    node_ids = set()

    for entity in results.nodes:
        nodes.append({
            "id": entity.uuid,
            "label": entity.name,
            "group": entity.entity_type,
            "valid_at": entity.valid_at.isoformat() if entity.valid_at else None,
            "invalid_at": entity.invalid_at.isoformat() if entity.invalid_at else None,
            "score": entity.score
        })
        node_ids.add(entity.uuid)

    for edge in results.edges:
        if edge.source in node_ids and edge.target in node_ids:
            links.append({
                "source": edge.source,
                "target": edge.target,
                "label": edge.relation_type,
                "fact": edge.fact,
                "valid_at": edge.valid_at.isoformat() if edge.valid_at else None,
                "invalid_at": edge.invalid_at.isoformat() if edge.invalid_at else None
            })

    return {"nodes": nodes, "links": links}
```

---

## Implementation Priorities

### Phase 1: Graph Foundation
1. Deploy FalkorDB and Memgraph via Docker Compose
2. Configure Graphiti client with FalkorDB driver
3. Define domain entity types (Pydantic schemas)
4. Run `build_indices_and_constraints()`

### Phase 2: Ingestion Pipeline
1. Integrate Cocoindex with Memgraph for static assets
2. Configure Cognee with Graphiti adapter for dynamic memory
3. Implement BAML extraction pipeline
4. Set up episode batching for high-volume ingestion

### Phase 3: Query Interface
1. Implement hybrid search (vector + keyword + graph)
2. Add time-travel queries with `reference_time`
3. Build MCP tool wrappers for agent integration
4. Create FastAPI endpoints for external access

### Phase 4: Visualization
1. Set up react-force-graph frontend
2. Implement time slider for temporal navigation
3. Add progressive loading for large graphs
4. Build NetworkX export for debugging

---

## References

### Graphiti / Temporal Graphs
- Graphiti GitHub: https://github.com/getzep/graphiti
- Zep Documentation: https://help.getzep.com/graphiti/
- Zep Temporal Architecture Paper: https://arxiv.org/html/2501.13956v1

### Graph Databases
- FalkorDB: https://docs.falkordb.com/
- FalkorDB Indexing: https://docs.falkordb.com/cypher/indexing/
- Memgraph: https://memgraph.com/docs
- Neo4j: https://neo4j.com/docs/

### Entity Resolution / Cognee
- Cognee Documentation: https://docs.cognee.ai/
- Cognee GitHub: https://github.com/topoteretes/cognee
- Cognee-Graphiti Integration: https://www.cognee.ai/blog/deep-dives/cognee-graphiti-integrating-temporal-aware-graphs

### Cocoindex
- Cocoindex Documentation: https://cocoindex.io/docs/
- Real-time Codebase Indexing: https://cocoindex.io/docs/examples/code_index

### Visualization
- react-force-graph: https://github.com/vasturiano/react-force-graph
- Cytoscape.js: https://js.cytoscape.org/
- NetworkX JSON Export: https://networkx.org/documentation/stable/reference/readwrite/json_graph.html
