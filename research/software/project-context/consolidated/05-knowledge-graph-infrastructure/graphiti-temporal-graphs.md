# Graphiti: Temporal Knowledge Graphs for AI Agents

## Executive Summary

Graphiti, developed by Zep AI, provides a bi-temporal knowledge graph architecture designed for agentic AI systems. Unlike static knowledge bases, Graphiti tracks both when facts became true in the real world (valid time) and when the system learned about them (transaction time), enabling "time travel" queries essential for accurate reasoning.

---

## 1. The Episodic Memory Paradigm

### 1.1 Beyond Static RAG

Traditional RAG systems treat knowledge as a flat collection of vectorized chunks. Graphiti introduces a fundamentally different model:

| Paradigm | Structure | Query Capability |
|----------|-----------|-----------------|
| **Vector RAG** | Flat chunks | "Find similar text" |
| **GraphRAG** | Entity-relationship graph | "How are A and B connected?" |
| **Graphiti** | Temporal entity-relationship graph | "How were A and B connected in 2020?" |

### 1.2 The Episode as Atomic Unit

Every piece of information in Graphiti enters as an **Episode**—a discrete event with temporal context:

```python
from graphiti_core import Graphiti, EpisodeType
from datetime import datetime

client = Graphiti("neo4j://localhost:7687", "neo4j", "password")

# Add a text episode
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

# Add a JSON episode
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

---

## 2. Bi-Temporal Data Model

### 2.1 The Two Time Dimensions

| Dimension | Field | Definition | Query Example |
|-----------|-------|------------|---------------|
| **Valid Time** | `valid_at` | When fact was true in real world | "What was the curriculum in 2015?" |
| **Transaction Time** | `created_at` | When fact was recorded in system | "What did we know on Tuesday?" |
| **Expiration** | `invalid_at` | When fact ceased to be true | "When did the bonus points end?" |

### 2.2 Edge Lifecycle

```python
# Edge structure with temporal properties
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

### 2.3 Edge Invalidation Example

**Scenario:** The Irish Leaving Certificate Maths Bonus Points policy changed over time.

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

## 3. Custom Entity Types

### 3.1 Pydantic-Based Entity Schemas

Graphiti supports custom entity types defined via Pydantic:

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

class LearningOutcome(BaseModel):
    """A specific learning outcome from curriculum."""
    outcome_id: str = Field(description="Curriculum code")
    description: str = Field(description="Full text of outcome")
    strand: str = Field(description="Curriculum strand")
```

### 3.2 Episode Ingestion with Custom Types

```python
from graphiti_core import Graphiti, EpisodeType

client = Graphiti(uri="neo4j://localhost:7687")

await client.add_episode(
    name="exam_2024_math_p2",
    episode_body=markdown_text,
    source=EpisodeType.text,
    source_description="Leaving Certificate Mathematics Paper 2, 2024",
    reference_time=datetime(2024, 6, 10),
    entity_types=[MathTheorem, ExamQuestion, CurriculumStandard]
)
```

---

## 4. Hybrid Search Architecture

### 4.1 Search Modes

Graphiti combines multiple retrieval strategies:

| Mode | Mechanism | Use Case |
|------|-----------|----------|
| **Semantic** | Vector similarity (embeddings) | "Questions about geometry" |
| **Keyword** | BM25 full-text search | "Exact term lookup" |
| **Graph Traversal** | Multi-hop relationship following | "What connects X to Y?" |
| **Temporal** | Time-bounded queries | "Facts valid in 2020" |

### 4.2 Search API

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

### 4.3 Search Result Structure

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

---

## 5. Integration Patterns

### 5.1 With Dagster for Orchestration

```python
from dagster import asset, AssetExecutionContext
from graphiti_core import Graphiti, EpisodeType

@asset
def ingest_curriculum_episodes(context: AssetExecutionContext):
    """Ingest curriculum documents as Graphiti episodes."""
    client = Graphiti(
        uri=os.environ["NEO4J_URI"],
        user=os.environ["NEO4J_USER"],
        password=os.environ["NEO4J_PASSWORD"]
    )

    # Process each curriculum document
    for doc in get_curriculum_documents():
        await client.add_episode(
            name=f"curriculum_{doc.id}",
            episode_body=doc.content,
            source=EpisodeType.text,
            source_description=doc.title,
            reference_time=doc.effective_date,
            entity_types=[CurriculumStandard, LearningOutcome]
        )

    context.log.info(f"Ingested {len(documents)} curriculum episodes")
```

### 5.2 With BAML for Extraction

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

### 5.3 MCP Tool Wrapper

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

---

## 6. Visualization Considerations

### 6.1 React State Model for Temporal Graphs

```typescript
// React state for temporal visualization
const [fullGraph, setFullGraph] = useState({ nodes: [], links: [] });
const [displayedGraph, setDisplayedGraph] = useState({ nodes: [], links: [] });
const [currentTime, setCurrentTime] = useState(Date.now());

// Filter effect based on temporal cursor
useEffect(() => {
  if (!fullGraph.links.length) return;

  // Filter links based on valid_at/invalid_at
  const activeLinks = fullGraph.links.filter(link => {
    const validFrom = new Date(link.valid_at).getTime();
    const validUntil = link.invalid_at
      ? new Date(link.invalid_at).getTime()
      : Infinity;
    return currentTime >= validFrom && currentTime < validUntil;
  });

  // Filter nodes: Keep nodes with at least one active link
  const activeNodeIds = new Set();
  activeLinks.forEach(l => {
    activeNodeIds.add(l.source);
    activeNodeIds.add(l.target);
  });
  const activeNodes = fullGraph.nodes.filter(n => activeNodeIds.has(n.id));

  setDisplayedGraph({ nodes: activeNodes, links: activeLinks });
}, [fullGraph, currentTime]);
```

### 6.2 Time Slider Component

```typescript
// Time slider with histogram
const TimeSlider = ({ graphData, onChange }) => {
  // Calculate event distribution for histogram
  const histogram = useMemo(() => {
    const buckets = {};
    graphData.links.forEach(link => {
      const month = new Date(link.valid_at).toISOString().slice(0, 7);
      buckets[month] = (buckets[month] || 0) + 1;
    });
    return buckets;
  }, [graphData]);

  return (
    <div className="time-slider">
      <div className="histogram">
        {Object.entries(histogram).map(([month, count]) => (
          <div key={month} style={{ height: `${count * 10}px` }} />
        ))}
      </div>
      <input
        type="range"
        min={minTime}
        max={maxTime}
        value={currentTime}
        onChange={e => onChange(parseInt(e.target.value))}
      />
    </div>
  );
};
```

---

## 7. Database Backend Options

### 7.1 FalkorDB (Recommended)

| Feature | Value |
|---------|-------|
| **Architecture** | Redis-based graph database |
| **Query Language** | Cypher |
| **Latency** | Sub-millisecond |
| **Graphiti Support** | Native |

```python
# FalkorDB connection
from graphiti_core import Graphiti

client = Graphiti(
    uri="redis://localhost:6379",
    driver="falkordb"
)
```

### 7.2 Neo4j

| Feature | Value |
|---------|-------|
| **Architecture** | JVM-based graph database |
| **Query Language** | Cypher |
| **Tooling** | Extensive (Neo4j Browser, Bloom) |
| **Graphiti Support** | Native |

```python
# Neo4j connection
client = Graphiti(
    uri="neo4j://localhost:7687",
    user="neo4j",
    password="password"
)
```

---

## 8. Implementation Priorities

### Phase 1: Graph Foundation
1. Deploy FalkorDB or Neo4j
2. Configure Graphiti client
3. Define domain entity types (Pydantic)

### Phase 2: Ingestion Pipeline
1. Integrate with Dagster assets
2. Implement episode batching
3. Configure BAML extraction pipeline

### Phase 3: Query Interface
1. Implement hybrid search
2. Add time-travel queries
3. Build MCP tool wrapper

### Phase 4: Visualization
1. Export to frontend JSON format
2. Implement time slider
3. Build react-force-graph integration

---

## References

- Graphiti GitHub: https://github.com/getzep/graphiti
- Zep Documentation: https://help.getzep.com/graphiti/
- FalkorDB: https://docs.falkordb.com/
- Neo4j Temporal Modeling: https://neo4j.com/blog/temporal-data-management/
