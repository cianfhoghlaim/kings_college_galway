# Cognee: Entity Resolution and Knowledge Structuring

## Executive Summary

Cognee acts as semantic middleware that transforms unstructured data into structured knowledge graphs. Its primary function is "cognification"—mapping isolated data points into a connected graph structure with entity resolution, ensuring consistent knowledge representation.

---

## 1. The Cognification Pipeline

### 1.1 Pipeline Stages

```
Unstructured Data
    ↓
┌─────────────────────────────────────┐
│          INGESTION (.add)           │
│   Documents, code files, text       │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│       COGNIFICATION (.cognify)      │
│   - Chunking                        │
│   - Embedding generation            │
│   - LLM entity extraction           │
│   - Relationship mapping            │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│        ENTITY RESOLUTION            │
│   - Merge "Maths" ↔ "Mathematics"   │
│   - Link cross-document entities    │
│   - Canonical node creation         │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│         GRAPH STORAGE               │
│   NetworkX / FalkorDB / Neo4j       │
└─────────────────────────────────────┘
```

### 1.2 Basic Usage

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

## 2. Entity Resolution Mechanisms

### 2.1 The Identity Problem

Unstructured data contains inconsistent nomenclature:

| Source A | Source B | Source C |
|----------|----------|----------|
| "Maths" | "Mathematics" | "Project Maths" |
| "LC" | "Leaving Cert" | "Leaving Certificate" |
| "H1" | "Honours 1" | "Grade H1" |

Without resolution, the graph becomes fragmented with duplicate nodes.

### 2.2 Resolution Strategy

Cognee uses LLM-based semantic reasoning to determine entity identity:

```python
# Internal resolution logic (conceptual)
async def resolve_entities(extracted_entities: list[Entity]) -> list[Entity]:
    """
    Merge semantically identical entities.
    """
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

### 2.3 Resolution Examples

| Raw Input | Canonical Form | Reasoning |
|-----------|----------------|-----------|
| "Maths", "Mathematics", "Math" | `Subject:Mathematics` | Same subject |
| "H1", "Honours 1", "Grade H1" | `Grade:H1` | Same grading symbol |
| "Trinity", "TCD", "Trinity College Dublin" | `University:TCD` | Same institution |
| "Physics", "Applied Physics" | Separate entities | Different subjects |

---

## 3. Ontology-Based Structuring

### 3.1 Defining Custom Ontologies

Cognee supports Pydantic-based ontology definitions:

```python
from cognee import DataPoint
from pydantic import Field

class Subject(DataPoint):
    """An academic subject in the curriculum."""
    name: str = Field(description="Canonical subject name")
    level: str = Field(description="Primary, Junior, Senior")
    code: str = Field(description="Official subject code")

class GradeRequirement(DataPoint):
    """A grade requirement for course entry."""
    subject: str = Field(description="Subject name")
    min_grade: str = Field(description="Minimum grade required")
    level: str = Field(description="Higher, Ordinary, Foundation")

class Course(DataPoint):
    """A university course/programme."""
    name: str = Field(description="Course name")
    institution: str = Field(description="University name")
    cao_code: str = Field(description="CAO application code")
    points: int = Field(description="Required CAO points")
```

### 3.2 Ontology-Guided Extraction

```python
import cognee
from cognee.infrastructure.llm.prompts import read_query_prompt

# Configure extraction with ontology
await cognee.config.set_ontology([Subject, GradeRequirement, Course])

# Add document
await cognee.add(matriculation_requirements_pdf)

# Cognify with ontology-guided extraction
await cognee.cognify()

# The LLM now extracts entities matching the defined ontology
```

---

## 4. Graph Storage Backends

### 4.1 NetworkX (Local Development)

- **Type:** In-memory Python graph
- **Best For:** Prototyping, small datasets
- **Export:** Easy JSON serialization

```python
from cognee.infrastructure.databases.graph import get_graph_client
import networkx as nx
from networkx.readwrite import json_graph

client = get_graph_client()
G = client.graph  # NetworkX DiGraph

# Export to JSON for frontend
data = json_graph.node_link_data(G)
```

### 4.2 FalkorDB (Production)

- **Type:** Redis-based graph database
- **Best For:** Low-latency production workloads
- **Query Language:** Cypher

```python
# Configure Cognee for FalkorDB
import cognee

await cognee.config.set_graph_database(
    driver="falkordb",
    uri="redis://localhost:6379"
)
```

### 4.3 Neo4j (Enterprise)

- **Type:** JVM-based graph database
- **Best For:** Enterprise features, tooling
- **Query Language:** Cypher

```python
await cognee.config.set_graph_database(
    driver="neo4j",
    uri="neo4j://localhost:7687",
    user="neo4j",
    password="password"
)
```

---

## 5. Hybrid Storage Integration

### 5.1 Vector + Graph Architecture

Cognee natively supports dual storage:

```
Document Chunk
    ↓
┌─────────────────┐    ┌─────────────────┐
│   VECTOR STORE  │    │   GRAPH STORE   │
│   (Embeddings)  │    │   (Entities)    │
│                 │    │                 │
│   "Find similar │    │   "How is A     │
│    content"     │    │    related to   │
│                 │    │    B?"          │
└─────────────────┘    └─────────────────┘
         ↓                     ↓
    ┌─────────────────────────────────┐
    │        HYBRID SEARCH            │
    │   Vector similarity + Graph     │
    │   traversal combined            │
    └─────────────────────────────────┘
```

### 5.2 Configuration

```python
import cognee

# Configure vector store
await cognee.config.set_vector_database(
    driver="qdrant",
    uri="http://localhost:6333"
)

# Configure graph store
await cognee.config.set_graph_database(
    driver="falkordb",
    uri="redis://localhost:6379"
)
```

---

## 6. API Integration Patterns

### 6.1 FastAPI Backend

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

### 6.2 With Cypher for Complex Queries

```python
@app.get("/api/subject-requirements/{subject}")
async def get_subject_requirements(subject: str):
    """Get all course requirements for a subject."""
    from cognee.infrastructure.databases.graph import get_graph_client

    client = get_graph_client()

    # Cypher query for Neo4j/FalkorDB
    query = """
    MATCH (s:Subject {name: $subject})
          <-[:REQUIRES]-(c:Course)
    RETURN c.name as course, c.institution as university,
           c.cao_code as code, c.points as points
    ORDER BY c.points DESC
    """

    results = client.execute(query, {"subject": subject})
    return {"requirements": results}
```

---

## 7. Visualization Patterns

### 7.1 Built-in PyVis Export

```python
# Generate interactive HTML visualization
await cognee.visualize_graph("output/knowledge_graph.html")
```

### 7.2 Custom React Integration

```python
# Backend endpoint for react-force-graph
@app.get("/api/graph-data")
async def get_visualization_data():
    from cognee.infrastructure.databases.graph import get_graph_client
    import networkx as nx
    from networkx.readwrite import json_graph

    client = get_graph_client()
    G = client.graph

    # Convert to format expected by react-force-graph
    data = json_graph.node_link_data(G)

    # Enrich with visualization metadata
    for node in data["nodes"]:
        node["group"] = node.get("type", "default")
        node["label"] = node.get("name", node["id"])

    for link in data["links"]:
        link["label"] = link.get("relation_type", "RELATES_TO")

    return data
```

### 7.3 Frontend Component

```typescript
// React component with Cognee data
import ForceGraph2D from 'react-force-graph-2d';

const KnowledgeGraph = () => {
  const [graphData, setGraphData] = useState({ nodes: [], links: [] });

  useEffect(() => {
    fetch('/api/graph-data')
      .then(res => res.json())
      .then(data => setGraphData(data));
  }, []);

  return (
    <ForceGraph2D
      graphData={graphData}
      nodeLabel="label"
      nodeAutoColorBy="group"
      linkLabel="label"
      linkDirectionalArrowLength={3}
    />
  );
};
```

---

## 8. Cognee vs Graphiti

### 8.1 Comparison Matrix

| Aspect | Cognee | Graphiti |
|--------|--------|----------|
| **Focus** | Semantic structuring | Temporal persistence |
| **Time Handling** | Snapshot-based | Bi-temporal edges |
| **Entity Resolution** | LLM-based merging | Episode-based |
| **Best For** | Static enterprise data | Dynamic agent memory |
| **Query Strength** | Ontology-aligned | Time-travel queries |

### 8.2 Complementary Usage

In the unified pipeline:
1. **Cognee** handles entity resolution and semantic structuring
2. **Graphiti** persists the resolved entities with temporal metadata

```python
# Combined workflow
async def unified_pipeline(document: str, reference_time: datetime):
    # Step 1: Cognee for entity resolution
    await cognee.add(document)
    await cognee.cognify()

    # Step 2: Export resolved entities
    client = get_graph_client()
    entities = extract_resolved_entities(client.graph)

    # Step 3: Graphiti for temporal persistence
    await graphiti.add_episode(
        name=f"document_{hash(document)[:8]}",
        episode_body=entities,
        source=EpisodeType.json,
        reference_time=reference_time
    )
```

---

## 9. Implementation Priorities

### Phase 1: Core Setup
1. Deploy Cognee with NetworkX backend
2. Define domain ontology (Pydantic classes)
3. Test basic ingestion and cognification

### Phase 2: Production Backend
1. Migrate to FalkorDB or Neo4j
2. Configure hybrid vector + graph storage
3. Implement entity resolution tuning

### Phase 3: API Layer
1. Build FastAPI endpoints
2. Implement Cypher query interface
3. Add visualization data export

### Phase 4: Integration
1. Connect with Dagster for orchestration
2. Integrate with Graphiti for temporal persistence
3. Build unified pipeline

---

## References

- Cognee Documentation: https://docs.cognee.ai/
- Cognee GitHub: https://github.com/topoteretes/cognee
- FalkorDB Cognee Integration: https://docs.falkordb.com/agentic-memory/cognee.html
- NetworkX JSON Export: https://networkx.org/documentation/stable/reference/readwrite/json_graph.html
