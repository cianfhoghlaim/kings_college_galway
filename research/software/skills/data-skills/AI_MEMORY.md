# AI Memory, Agents & Knowledge Management

## Quick Navigation

This document covers agent frameworks, knowledge graphs, and AI memory systems for the data-unified platform.

**Related Documents:**
- [ARCHITECTURE.md](./ARCHITECTURE.md) - Core architecture reference
- [SCHEMAS_AND_TYPES.md](./SCHEMAS_AND_TYPES.md) - BAML, Pydantic, Zod type system
- [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Step-by-step patterns

---

## Table of Contents

1. [Overview: AI Memory Architecture](#overview-ai-memory-architecture)
2. [Agno AgentOS](#agno-agentos)
3. [BAML: Structured LLM Outputs](#baml-structured-llm-outputs)
4. [Cognee: AI Memory Engine](#cognee-ai-memory-engine)
5. [CocoIndex: AI-Native Indexing](#cocoindex-ai-native-indexing)
6. [CocoIndex vs Cognee: Overlap Analysis](#cocoindex-vs-cognee-overlap-analysis)
7. [Integration Patterns](#integration-patterns)
8. [Code Analysis Workflow Example](#code-analysis-workflow-example)

---

## Overview: AI Memory Architecture

The AI memory stack combines agent frameworks with knowledge stores to provide LLM-based systems with durable, structured memory.

### Component Stack

```
┌─────────────────────────────────────────────────────────────┐
│                   Agent Orchestration                       │
│                   (Agno AgentOS + BAML)                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ MCP / Tool Calls
                          │
┌─────────────────────────┴───────────────────────────────────┐
│                   Knowledge Layer                           │
│  ┌─────────────────────┐   ┌─────────────────────────────┐  │
│  │     Cognee          │   │      CocoIndex             │  │
│  │  (Memory Engine)    │   │   (Indexing Pipeline)      │  │
│  │  - Query Interface  │   │  - Data Transformation     │  │
│  │  - GraphRAG         │   │  - Incremental Updates     │  │
│  │  - MCP Server       │   │  - Custom Extraction       │  │
│  └──────────┬──────────┘   └──────────────┬──────────────┘  │
└─────────────┼────────────────────────────┼──────────────────┘
              │                            │
              └───────────┬────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────┐
│                   Storage Layer                             │
│  ┌─────────────────────┐   ┌─────────────────────────────┐  │
│  │   Vector Store      │   │    Graph Store              │  │
│  │   (LanceDB)         │   │   (Memgraph/Neo4j)          │  │
│  │   - Embeddings      │   │   - Entity relationships    │  │
│  │   - Semantic search │   │   - Knowledge graph         │  │
│  └─────────────────────┘   └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Key Capabilities

| Component | Role | Strengths |
|-----------|------|-----------|
| **Agno AgentOS** | Agent runtime & orchestration | MCP support, 20+ vector DB connectors |
| **BAML** | Structured LLM outputs | Type-safe prompts, schema enforcement |
| **Cognee** | Memory retrieval engine | Hybrid search, GraphRAG, MCP server |
| **CocoIndex** | Indexing pipeline | Incremental updates, custom extraction |

---

## Agno AgentOS

Agno's AgentOS is a framework for building multi-agent systems with shared memory and external tool access.

### Key Features

- **MCP Support**: Model Context Protocol for standardized tool/data access
- **Knowledge Subsystem**: Connectors to 20+ vector stores
- **Multi-Agent**: Teams of agents with shared memory
- **Tool Integration**: External APIs via MCP endpoints

### MCP Integration

```python
from agno.agent import Agent
from agno.models.openai import OpenAIChat

# Enable MCP server for agent control
agent = Agent(
    name="CodeAnalyzer",
    model=OpenAIChat(id="gpt-4"),
    enable_mcp_server=True,  # Spins up FastAPI endpoint at /mcp
    description="AI agent that analyzes code repositories"
)

# Agent can now be controlled via MCP protocol
# and can call external tool APIs
```

### Knowledge Base Integration

```python
from agno.knowledge import KnowledgeBase
from agno.vectordb import LanceDBClient

# Connect to LanceDB for semantic search
knowledge = KnowledgeBase(
    vectordb=LanceDBClient(path="/data/vectors"),
    embedding_model="sentence-transformers/all-MiniLM-L6-v2"
)

# Agent can query knowledge base
agent = Agent(
    name="RAGAgent",
    knowledge=knowledge,
    instructions=["Use knowledge base to answer questions about the codebase"]
)
```

---

## BAML: Structured LLM Outputs

BAML (Boundary AI Markup Language) defines agent behaviors, prompts, and expected output formats.

### Core Concepts

- **Structured Prompts**: Define expected output format
- **Schema Enforcement**: Validate LLM outputs against schema
- **Tool Definitions**: Specify tool interfaces for agents
- **Traceable Reasoning**: Make agent logic debuggable

### BAML Tool Definition

```baml
// Define tool for vector search
function SearchCodebase {
  input QueryInput {
    query string @description("Natural language query")
    top_k int @description("Number of results to return")
  }

  output SearchResult[] {
    filename string
    content string
    score float
  }
}

// Define agent with structured output
function AnalyzeRepository {
  input RepoContext {
    files string @description("Repository content from Repomix")
  }

  output RepoAnalysis {
    summary string @description("High-level summary")
    key_findings string[] @description("Notable observations")
    conclusion string @description("Final recommendations")
  }
}
```

### Agent with BAML Schema

```python
from agno.agent import Agent
from agno.models.openai import OpenAIChat

repo_analyzer = Agent(
    name="RepoAnalyzer",
    model=OpenAIChat(id="gpt-4"),
    description="AI agent that summarizes software repositories",
    instructions=[
        "Provide a high-level summary of the repository",
        "Analyze code for notable patterns or issues",
        "Review documentation for completeness",
        "Conclude with recommendations"
    ],
    output_schema={
        "summary": str,
        "key_findings": list,
        "conclusion": str,
        "docker_compose": str  # Optional: for Arduino projects
    }
)
```

---

## Cognee: AI Memory Engine

Cognee is an open-source AI memory framework providing durable, structured memory for LLM-based agents.

### Architecture

```
┌─────────────────┐
│  Input Sources  │  PDFs, Markdown, Code, APIs
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│                  Cognee Pipeline                │
│        (Extract → Cognify → Load)               │
│  ┌───────────────────────────────────────────┐  │
│  │  1. Chunk documents                       │  │
│  │  2. Generate embeddings                   │  │
│  │  3. Extract entities with LLM             │  │
│  │  4. Infer relationships between entities  │  │
│  └───────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
┌─────────────────┐     ┌─────────────────────┐
│  Vector Store   │     │   Graph Store       │
│  (LanceDB)      │     │  (Memgraph/Neo4j)   │
│  - Embeddings   │     │  - Entity nodes     │
│  - Semantic     │     │  - Relationship     │
│    search       │     │    edges            │
└─────────────────┘     └─────────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │     Query API         │
         │  - cognee.search()    │
         │  - MCP Server         │
         │  - GraphRAG           │
         └───────────────────────┘
```

### Supported Storage Backends

**Vector Stores:**
- LanceDB
- Redis
- Qdrant
- PgVector

**Graph Stores:**
- Neo4j
- Memgraph
- Kuzu
- NetworkX (in-memory)

### Basic Usage

```python
import cognee

# Configure backends
cognee.config.set_llm_config({
    "provider": "openai",
    "model": "gpt-4",
    "api_key": "<OPENAI_KEY>"
})

cognee.config.set_vector_db({
    "provider": "lancedb",
    "path": "/data/vectors"
})

cognee.config.set_graph_db({
    "provider": "memgraph",
    "host": "memgraph.example.com",
    "port": 7687
})

# Ingest documents
cognee.add_data_points([
    "CocoIndex supports Incremental Processing.",
    "Cognee provides hybrid retrieval: vector + graph."
])

# Build knowledge graph
cognee.cognify()

# Query with different strategies
result = cognee.search(
    "What indexing features does CocoIndex have?",
    search_type="hybrid"  # or "vector", "graph", "graph_completion"
)
```

### Search Types

| Type | Description | Best For |
|------|-------------|----------|
| `vector` | Pure semantic similarity | Simple questions, fact lookup |
| `graph` | Graph traversal only | Relationship queries |
| `hybrid` | Combines vector + graph | Complex questions |
| `graph_completion` | LLM-enhanced graph reasoning | Multi-hop reasoning |

### MCP Server

```bash
# Start Cognee MCP server
cognee serve --port 8080

# Any MCP-compliant agent can now query Cognee
# Claude, Cursor, or Agno AgentOS agents
```

---

## CocoIndex: AI-Native Indexing

CocoIndex is an ETL framework for building custom AI indexing pipelines with incremental updates.

### Key Features

- **Incremental Processing**: Tracks state in Postgres, only reprocesses changes
- **Code Parsing**: Tree-sitter for language-aware chunking
- **Custom Extraction**: Define arbitrary transformation flows
- **Multi-Target Export**: Vector DBs + Graph DBs simultaneously

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                   CocoIndex Flow                             │
│                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────────────┐│
│  │  Sources    │──▶│ Transforms  │──▶│  Collectors         ││
│  │  LocalFile  │   │ SplitRecur  │   │  (intermediate)     ││
│  │  Postgres   │   │ Embed       │   │                     ││
│  │  API        │   │ Extract     │   └──────────┬──────────┘│
│  └─────────────┘   └─────────────┘              │           │
│                                                  │           │
│                                          ┌──────┴──────┐    │
│                                          │   Targets   │    │
│                                          │  - LanceDB  │    │
│                                          │  - Neo4j    │    │
│                                          │  - Postgres │    │
│                                          │  - Qdrant   │    │
│                                          └─────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

### Code Indexing Flow

```python
import cocoindex
from cocoindex.sources import LocalFile
from cocoindex.functions import SplitRecursively, SentenceTransformerEmbed

@cocoindex.flow_def(name="CodeIndex")
def code_index_flow(flow_builder, data_scope):
    # Source: Local repository
    data_scope["files"] = flow_builder.add_source(
        LocalFile(
            path="/path/to/repo",
            included_patterns=["*.py", "*.js", "*.md"],
            excluded_patterns=[".*", "node_modules"]
        )
    )

    # Transform: Parse and chunk
    with data_scope["files"].row() as file:
        file["ext"] = file["filename"].transform(lambda fn: fn.split(".")[-1])

        file["chunks"] = file["content"].transform(
            SplitRecursively(),
            language=file["ext"],
            chunk_size=1000,
            chunk_overlap=200
        )

        # Transform: Embed chunks
        with file["chunks"].row() as chunk:
            chunk["embedding"] = chunk["text"].transform(
                SentenceTransformerEmbed(
                    model="sentence-transformers/all-MiniLM-L6-v2"
                )
            )

            # Collect for export
            data_scope.add_collector("code_index").collect(
                filename=file["filename"],
                text=chunk["text"],
                embedding=chunk["embedding"]
            )

    # Export to vector database
    code_index = data_scope.get_collector("code_index")
    code_index.export(
        "code_embedding_index",
        cocoindex.storages.Postgres(),
        primary_key_fields=["filename", "text"],
        vector_indexes=[
            cocoindex.VectorIndex(
                "embedding",
                cocoindex.VectorSimilarityMetric.COSINE_SIMILARITY
            )
        ]
    )

# Run flow (incremental on subsequent runs)
flow = code_index_flow()
flow.run()
```

### Knowledge Graph Flow

```python
@cocoindex.flow_def(name="DocsKnowledgeGraph")
def docs_kg_flow(flow_builder, data_scope):
    data_scope["docs"] = flow_builder.add_source(
        LocalFile(path="/path/to/docs", included_patterns=["*.md"])
    )

    with data_scope["docs"].row() as doc:
        # Extract entities and relationships with LLM
        doc["entities"] = doc["content"].transform(
            cocoindex.functions.ExtractEntities()
        )

        doc["relationships"] = doc["content"].transform(
            cocoindex.functions.ExtractRelationships()
        )

        # Collect for graph export
        with doc["entities"].row() as entity:
            data_scope.add_collector("entities").collect(
                name=entity["name"],
                type=entity["type"],
                source_doc=doc["filename"]
            )

        with doc["relationships"].row() as rel:
            data_scope.add_collector("relationships").collect(
                subject=rel["subject"],
                predicate=rel["predicate"],
                object=rel["object"]
            )

    # Export to Neo4j/Memgraph
    entities = data_scope.get_collector("entities")
    entities.export(
        "Entity",
        cocoindex.storages.Neo4j(),
        node_label="Entity"
    )

    rels = data_scope.get_collector("relationships")
    rels.export(
        "Relationships",
        cocoindex.storages.Neo4j(),
        edge_type="RELATIONSHIP"
    )
```

---

## CocoIndex vs Cognee: Overlap Analysis

Both tools handle chunking, embedding, entity extraction, and knowledge graph creation.

### Feature Comparison

| Capability | Cognee | CocoIndex |
|------------|--------|-----------|
| **Document chunking** | ✅ Built-in | ✅ Built-in |
| **Embedding generation** | ✅ Built-in | ✅ Built-in |
| **Entity extraction** | ✅ LLM-based | ✅ LLM-based |
| **Relationship inference** | ✅ Automatic | ✅ Configurable |
| **LanceDB support** | ✅ Native | ✅ Native |
| **Neo4j/Memgraph support** | ✅ Native | ✅ Native |
| **Query interface** | ✅ Built-in (search API) | ❌ External (you implement) |
| **MCP server** | ✅ Built-in | ❌ Not included |
| **Incremental updates** | ✅ Via API | ✅ Postgres state tracking |
| **Custom extraction logic** | ⚠️ Limited | ✅ Flexible flows |
| **Feedback loops** | ✅ Online learning | ❌ Not included |

### When to Use Each

**Use Cognee alone when:**
- You want a plug-and-play memory solution
- Your data is standard (documents, PDFs, code)
- You need GraphRAG out of the box
- You want MCP integration for agents

**Use CocoIndex alone when:**
- You need custom extraction pipelines
- You have complex/diverse data sources
- You want fine-grained control over indexing
- You'll implement your own query layer

**Use both together when:**
- CocoIndex builds the knowledge base (custom ETL)
- Cognee provides the query interface (GraphRAG, MCP)
- You need the best of both: custom indexing + intelligent retrieval

### Hybrid Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Indexing Stage                          │
│                     (CocoIndex)                             │
│  - Custom extraction flows                                  │
│  - Incremental processing                                   │
│  - Export to LanceDB + Memgraph                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     Storage Layer                           │
│  ┌─────────────────┐       ┌─────────────────────────────┐  │
│  │   LanceDB       │       │      Memgraph               │  │
│  │   (vectors)     │       │   (knowledge graph)         │  │
│  └────────┬────────┘       └──────────────┬──────────────┘  │
└───────────┼────────────────────────────────┼────────────────┘
            │                                │
            └───────────────┬────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     Query Stage                             │
│                     (Cognee)                                │
│  - Configure to use existing LanceDB + Memgraph             │
│  - GraphRAG search strategies                               │
│  - MCP server for agent access                              │
│  - Online learning from feedback                            │
└─────────────────────────────────────────────────────────────┘
```

### Dual-Engine Graph Strategy

For production deployments, consider a **dual-engine architecture** for graph storage:

```
┌─────────────────────────────────────────────────────────────┐
│                   Graph Storage Layer                        │
│  ┌───────────────────────┐   ┌───────────────────────────┐  │
│  │ Static Knowledge      │   │ Dynamic Memory            │  │
│  │ (Memgraph)            │   │ (FalkorDB via Graphiti)   │  │
│  │                       │   │                           │  │
│  │ - Domain ontology     │   │ - Session context         │  │
│  │ - Validated facts     │   │ - User interactions       │  │
│  │ - Reference data      │   │ - Episodic memories       │  │
│  │ - ACID guaranteed     │   │ - High write velocity     │  │
│  └───────────────────────┘   └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**When to use dual-engine:**
- **Memgraph (static)**: Long-term validated knowledge, domain schemas, reference data
- **FalkorDB (dynamic)**: Session-specific context, agent learning, conversation history

**Data flow:**
1. Ephemeral interactions → FalkorDB
2. Validated insights → Promoted to Memgraph
3. Query routing: Generic → Memgraph, User-specific → FalkorDB

---

## Integration Patterns

### Pattern 1: AgentOS + Cognee via MCP

```python
from agno.agent import Agent
from agno.tools import MCPTool

# Connect to Cognee's MCP server
cognee_tool = MCPTool(
    name="cognee_memory",
    endpoint="http://cognee-server:8080/mcp"
)

agent = Agent(
    name="RAGAgent",
    tools=[cognee_tool],
    instructions=[
        "Use cognee_memory tool to search for relevant information",
        "Combine vector and graph results for comprehensive answers"
    ]
)

# Agent automatically calls Cognee for knowledge retrieval
response = agent.run("What features does CocoIndex support?")
```

### Pattern 2: AgentOS + CocoIndex + Custom Graph Tool

```python
from agno.agent import Agent
from agno.knowledge import KnowledgeBase
from agno.vectordb import LanceDBClient

# Connect to CocoIndex-populated LanceDB
knowledge = KnowledgeBase(
    vectordb=LanceDBClient(path="/data/cocoindex/vectors")
)

# Custom tool for graph queries
def graph_search(query: str) -> list:
    """Query Memgraph for related entities"""
    from neo4j import GraphDatabase
    driver = GraphDatabase.driver("bolt://memgraph:7687")
    with driver.session() as session:
        result = session.run(
            "MATCH (e:Entity)-[r]-(related) "
            "WHERE e.name CONTAINS $query "
            "RETURN e, r, related LIMIT 10",
            query=query
        )
        return [dict(record) for record in result]

agent = Agent(
    name="HybridRAGAgent",
    knowledge=knowledge,
    tools=[graph_search],
    instructions=[
        "First search knowledge base for relevant chunks",
        "Then use graph_search for related entities",
        "Synthesize both sources for complete answers"
    ]
)
```

### Pattern 3: BAML + Cognee for Agentic RAG

```baml
// Define tools in BAML
function VectorSearch {
  input { query string, top_k int }
  output DocumentChunk[]
}

function GraphSearch {
  input { entity string, depth int }
  output Entity[]
}

// Agent decides which tool to use
function AgenticRAG {
  input UserQuestion { question string }

  output Answer {
    reasoning string @description("Explain which tools were used")
    sources DocumentChunk[]
    answer string
  }
}
```

```python
# Python implementation using BAML-defined tools
from baml_client import b

async def answer_question(question: str):
    # BAML ensures structured output
    result = await b.AgenticRAG(question=question)

    # result.reasoning explains tool usage
    # result.sources lists retrieved documents
    # result.answer is the final response
    return result
```

---

## Code Analysis Workflow Example

End-to-end workflow for analyzing local Git repositories.

### Pipeline Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  1. DLT         │───▶│  2. CocoIndex   │───▶│  3. Repomix     │
│  (Ingest files) │    │  (Index & chunk)│    │  (Package repo) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                      │
                                                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      4. Agno + BAML                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │ Summarize Code  │───▶│ Review Docs     │───▶│ Conclusions │ │
│  └─────────────────┘    └─────────────────┘    └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                                      │
                                                      ▼
                            ┌─────────────────────────────────────┐
                            │  5. Output                          │
                            │  - Structured report                │
                            │  - Docker Compose (if Arduino)      │
                            └─────────────────────────────────────┘
```

### Step 1: Ingest with DLT

```python
import dlt
from dlt.sources.filesystem import filesystem_source

repo_path = "/path/to/local/repo"

files = filesystem_source(
    bucket_url=f"file://{repo_path}",
    file_glob="**/*.*"
)

pipeline = dlt.pipeline(
    pipeline_name="repo_pipeline",
    destination="duckdb",
    dataset_name="repo_data"
)

pipeline.run(files)
```

### Step 2: Index with CocoIndex

```python
import cocoindex
from cocoindex.sources import LocalFile

@cocoindex.flow_def(name="CodeIndex")
def code_index_flow(flow_builder, data_scope):
    data_scope["files"] = flow_builder.add_source(
        LocalFile(
            path="/path/to/local/repo",
            included_patterns=["*.py", "*.cpp", "*.ino", "*.md"],
            excluded_patterns=[".*", "node_modules"]
        )
    )

    with data_scope["files"].row() as file:
        file["ext"] = file["filename"].transform(lambda fn: fn.split(".")[-1])
        file["chunks"] = file["content"].transform(
            cocoindex.functions.SplitRecursively(),
            language=file["ext"],
            chunk_size=1000
        )

        with file["chunks"].row() as chunk:
            chunk["embedding"] = chunk["text"].transform(
                cocoindex.functions.SentenceTransformerEmbed(
                    model="all-MiniLM-L6-v2"
                )
            )

            data_scope.add_collector("code_index").collect(
                filename=file["filename"],
                text=chunk["text"],
                embedding=chunk["embedding"]
            )

    data_scope.get_collector("code_index").export(
        "code_embedding_index",
        cocoindex.storages.Postgres()
    )

code_index_flow().run()
```

### Step 3: Package with Repomix

```bash
# Install Repomix
npm install -g repomix

# Package repository for LLM consumption
repomix --compress

# Output: repomix-output.xml (single AI-friendly file)
```

### Step 4: Analyze with Agno + BAML

```python
from agno.agent import Agent
from agno.models.openai import OpenAIChat

# Load Repomix output
with open("repomix-output.xml") as f:
    repo_context = f.read()

repo_analyzer = Agent(
    name="RepoAnalyzer",
    model=OpenAIChat(id="gpt-4"),
    instructions=[
        "Provide a high-level summary of the repository",
        "Analyze code for notable patterns or issues",
        "Review documentation for completeness",
        "If .ino files exist, suggest Docker Compose for Arduino builds"
    ],
    output_schema={
        "summary": str,
        "key_findings": list,
        "conclusion": str,
        "docker_compose": str  # Optional
    }
)

result = repo_analyzer.run(repo_context)
```

### Step 5: Example Output

```json
{
  "summary": "This project is an IoT sensor logging system written in C++ and Python. It consists of Arduino firmware (.ino) and a Python Flask web app.",

  "key_findings": [
    "The Arduino code handles sensor data gathering and uses external XYZ library",
    "The Python backend has comprehensive unit tests, but logging is rudimentary",
    "The repository lacks CI/CD configuration"
  ],

  "conclusion": "Overall functional and reasonably documented. Recommend adding error handling in Arduino code and expanding deployment documentation.",

  "docker_compose": "version: '3.8'\nservices:\n  arduino_builder:\n    image: arduino/arduino-cli:latest\n    volumes:\n      - ./:/workspace\n    command: arduino-cli compile --fqbn arduino:avr:uno /workspace/project.ino"
}
```

---

## Best Practices

### 1. Choose the Right Architecture

- **Simple RAG**: Use Cognee alone
- **Custom ETL**: Use CocoIndex alone
- **Production hybrid**: CocoIndex for indexing, Cognee for querying

### 2. Optimize for Incremental Updates

- CocoIndex tracks state in Postgres automatically
- Cognee can ingest new data in batches
- Both avoid reprocessing unchanged content

### 3. Use MCP for Agent Integration

- Cognee's MCP server provides standard interface
- Any MCP-compliant agent can query the knowledge base
- Reduces custom integration code

### 4. Define Clear Output Schemas

- BAML ensures structured LLM outputs
- Pydantic/Zod provide runtime validation
- Consistent schemas across the stack

### 5. Monitor Memory Quality

- Track retrieval accuracy over time
- Use feedback loops (Cognee supports online learning)
- Periodically rebuild indexes if data drifts

---

## References

**Sources:**
- agents-knowledge-management.md (AgentOS, BAML, Cognee, CocoIndex integration)

**External:**
- [Agno Documentation](https://docs.agno.com)
- [BAML Documentation](https://docs.boundaryml.com)
- [Cognee Documentation](https://www.cognee.ai)
- [CocoIndex Documentation](https://cocoindex.io/docs)

**Last Updated:** November 29, 2024
