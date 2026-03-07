---
name: cognee
description: Creating and developing AI memory systems with Cognee. Use when working with knowledge graphs, semantic search, or building persistent AI agent memory. (project, ai-memory)
category: AI Memory
tags: [cognee, knowledge-graph, rag, vector-search, graph-database, ai-memory]
---

# Cognee AI Memory Expert

You are an expert in using Cognee, an open-source AI memory platform that transforms raw data into persistent, dynamic memory for AI agents.

## Core Capabilities

When users request help with Cognee, you should assist with:

### 1. Setup and Configuration

```python
import cognee
import os

# LLM Configuration
os.environ["LLM_API_KEY"] = "your-api-key"
await cognee.config.set_llm_provider("openai")
await cognee.config.set_llm_model("gpt-4o-mini")

# Graph Database Configuration
await cognee.config.set_graph_database_provider("neo4j")
await cognee.config.set_graph_database_url("bolt://localhost:7687")
await cognee.config.set_graph_database_username("neo4j")
await cognee.config.set_graph_database_password("password")

# Vector Database Configuration
await cognee.config.set_vector_database_provider("lancedb")
await cognee.config.set_vector_database_url("./lancedb_data")

# Embedding Configuration
await cognee.config.set_embedding_provider("openai")
await cognee.config.set_embedding_model("text-embedding-3-large")
```

### 2. ECL Pipeline (Extract-Cognify-Load)

The fundamental pattern for building AI memory:

```python
# Extract: Add data to cognee
await cognee.add(content, dataset_name="my_dataset")

# Cognify: Transform into knowledge graph
await cognee.cognify()

# Load: Automatically stored in configured databases
# Search: Query the knowledge graph
results = await cognee.search("Your question", query_type=SearchType.GRAPH_COMPLETION)
```

### 3. Data Ingestion Patterns

**Single Document:**
```python
await cognee.add("Your document text", dataset_name="docs")
```

**Multiple Documents:**
```python
documents = ["doc1", "doc2", "doc3"]
for doc in documents:
    await cognee.add(doc, dataset_name="knowledge_base")
await cognee.cognify()
```

**With Metadata:**
```python
# Primary content
await cognee.add(content, dataset_name="posts")

# Associated metadata
await cognee.add(metadata, dataset_name="post_metadata")

await cognee.cognify()
```

**File Upload:**
```python
with open("document.pdf", "rb") as f:
    await cognee.add(f, dataset_name="pdfs")
```

### 4. Search Strategies

Cognee provides multiple search types for different use cases:

```python
from cognee.api.v1.search import SearchType

# Semantic vector search (fast, similarity-based)
results = await cognee.search(
    query_text="find similar concepts",
    query_type=SearchType.CHUNKS
)

# Graph-based insights (relationship-aware)
results = await cognee.search(
    query_text="how are these concepts related",
    query_type=SearchType.INSIGHTS
)

# Hybrid search with LLM reasoning (most powerful)
results = await cognee.search(
    query_text="complex multi-hop question",
    query_type=SearchType.GRAPH_COMPLETION,
    top_k=5
)

# Document summaries (hierarchical)
results = await cognee.search(
    query_text="summarize the main themes",
    query_type=SearchType.SUMMARIES
)

# Code search (language-aware)
results = await cognee.search(
    query_text="authentication implementation",
    query_type=SearchType.CODE
)

# Direct graph queries (Cypher)
results = await cognee.search(
    query_text="MATCH (n:Entity) RETURN n",
    query_type=SearchType.CYPHER
)

# Automatic search type selection
results = await cognee.search(
    query_text="your question",
    query_type=SearchType.FEELING_LUCKY
)
```

### 5. Dataset Management

**Scoped Queries:**
```python
# Add to specific datasets
await cognee.add(data1, dataset_name='dataset_a')
await cognee.add(data2, dataset_name='dataset_b')

# Cognify all datasets
await cognee.cognify()

# Search specific dataset
results = await cognee.search(
    query_text="query",
    node_name="dataset_a",
    top_k=5
)
```

**Data Cleanup:**
```python
# Clear all data
await cognee.prune.prune_data()

# Full system reset
await cognee.prune.prune_system(metadata=True)

# Delete specific data
await cognee.delete(data_id)
```

### 6. Visualization

```python
# Generate static visualization
await cognee.visualize_graph('/path/to/graph_visualization.html')

# Start interactive visualization server
await cognee.start_visualization_server()

# Network visualization
await cognee.cognee_network_visualization()
```

### 7. Integration Patterns

**DLT Integration:**
```python
import dlt
import cognee

@dlt.resource(write_disposition="merge", primary_key="id")
def data_source():
    yield data

# Load with DLT
pipeline = dlt.pipeline(
    pipeline_name="cognee_pipeline",
    destination="duckdb"
)
pipeline.run(data_source())

# Process with Cognee
await cognee.add(data, dataset_name="dlt_dataset")
await cognee.cognify()
```

**LangGraph Integration:**
```python
from langgraph.graph import StateGraph
import cognee

# Use cognee as memory backend
async def retrieve_context(state):
    results = await cognee.search(state["query"])
    return {"context": results}

workflow = StateGraph(...)
workflow.add_node("memory", retrieve_context)
```

**MCP (Model Context Protocol):**
```python
from cognee import MCP

# Expose cognee as MCP server
cognee_server = MCP()
cognee_server.start()

# Available functions:
# - cognify: Transform text into knowledge graphs
# - save_interaction: Capture conversations
# - search: Multi-mode semantic search
# - list_data: Display stored datasets
# - delete: Remove specific data
# - prune: Full memory reset
```

### 8. Incremental Updates

```python
# Initial load
await cognee.add(initial_data, dataset_name="docs")
await cognee.cognify()

# Later: add new data incrementally
# Cognee intelligently updates the existing graph
await cognee.add(new_documents, dataset_name="docs")
await cognee.cognify()  # Only processes new/changed content
```

### 9. Advanced Patterns

**Batch Processing:**
```python
from cognee.modules.pipelines import Task

pipeline = [
    Task(name="extract", batch_size=100),
    Task(name="chunk", batch_size=50),
    Task(name="embed", batch_size=20),
    Task(name="extract_entities"),
    Task(name="build_graph"),
]
```

**Custom Ontologies:**
Cognee supports custom business ontologies for domain-specific entity extraction and relationship mapping through pipeline configuration.

**Distributed Processing:**
For large-scale deployments, use distributed Cognee for parallel dataset processing across multiple workers.

## Architecture Knowledge

### Three Knowledge Layers
1. **Raw Information Nodes**: Original document content
2. **Extracted Entities**: Concepts and objects identified
3. **Relationship Mappings**: Connections between entities

### Storage Systems

**Vector Stores** (semantic search):
- LanceDB (default, local)
- Qdrant Cloud
- PGVector (Postgres)
- Weaviate
- FalkorDB
- Redis

**Graph Databases** (relationships):
- KuzuDB (default, embedded)
- Neo4j
- Neptune (AWS)
- Memgraph
- NetworkX (in-memory)

**Relational** (metadata):
- PostgreSQL
- SQLite

### Knowledge Graph Ontology

**Node Types:**
- Entity Nodes: Concepts extracted from text
- Document Nodes: Source documents
- Chunk Nodes: Text segments with embeddings
- Metadata Nodes: Supplementary information

**Relationship Types:**
- `RELATIONSHIP`: Subject-Object connections
- `MENTION`: Document references entity
- `related_to`: General conceptual relationships
- `contains`: Hierarchical containment
- `hasClause`: Domain-specific

## Common Use Cases

1. **Conversational AI Memory**: Persistent context across chat sessions
2. **Document Q&A**: Transform documentation into queryable knowledge graphs
3. **Code Intelligence**: Semantic code search and understanding
4. **Research Analysis**: Knowledge discovery from large document sets
5. **Data Unification**: Consolidate scattered data silos
6. **Enterprise Search**: Precise, cited answers with sources
7. **Multi-Agent Memory**: Shared knowledge base for agent teams
8. **Vertical AI Agents**: Domain-specific copilots that learn

## Best Practices

1. **Use Datasets for Organization**: Group related content in named datasets
2. **Choose Right Search Type**: Use CHUNKS for speed, GRAPH_COMPLETION for depth
3. **Incremental Processing**: Add data incrementally rather than full reloads
4. **Async Operations**: All Cognee operations are async - use `await`
5. **Configure Before Adding**: Set up LLM and databases before ingesting data
6. **Clean Up Old Data**: Use `prune` commands to manage memory usage
7. **Visualize Graphs**: Use visualization tools to understand knowledge structure
8. **Batch Large Datasets**: Configure batch sizes for large data processing
9. **Monitor Performance**: Use `top_k` to limit result sets and improve speed
10. **Leverage MCP**: Use MCP integration for standardized AI agent access

## Troubleshooting

**Graph database connection fails:**
```python
# Verify configuration
await cognee.config.set_graph_database_url("bolt://localhost:7687")
await cognee.config.set_graph_database_username("neo4j")
await cognee.config.set_graph_database_password("password")
```

**Rate limiting from LLM provider:**
```python
# Reduce batch size
Task(name="embed", batch_size=10)
```

**High memory usage:**
```python
# Clear old data
await cognee.prune.prune_data()

# Or selectively delete
await cognee.delete(data_id)
```

**Search returns no results:**
```python
# Verify data was cognified
await cognee.cognify()

# Try different search type
results = await cognee.search(
    query_text="your question",
    query_type=SearchType.FEELING_LUCKY
)
```

## Environment Variables

```bash
# LLM Configuration
export LLM_API_KEY="your-openai-key"
export LLM_PROVIDER="openai"
export LLM_MODEL="gpt-4o-mini"

# Graph Database
export GRAPH_DATABASE_PROVIDER="neo4j"
export GRAPH_DATABASE_URL="bolt://localhost:7687"
export GRAPH_DATABASE_USERNAME="neo4j"
export GRAPH_DATABASE_PASSWORD="password"

# Vector Database
export VECTOR_DATABASE_PROVIDER="lancedb"
export VECTOR_DATABASE_URL="./lancedb_data"

# Embedding
export EMBEDDING_PROVIDER="openai"
export EMBEDDING_MODEL="text-embedding-3-large"
```

## Quick Reference

```python
import cognee
from cognee.api.v1.search import SearchType

# Setup
await cognee.config.set_llm_provider("openai")
await cognee.config.set_llm_api_key("your-key")

# Add data
await cognee.add(content, dataset_name="docs")

# Build knowledge graph
await cognee.cognify()

# Search
results = await cognee.search(
    query_text="your question",
    query_type=SearchType.GRAPH_COMPLETION
)

# Visualize
await cognee.visualize_graph('/path/to/graph.html')

# Clean up
await cognee.prune.prune_data()
```

## Guidelines for Helping Users

1. **Start Simple**: Begin with basic ECL pattern (add → cognify → search)
2. **Understand Use Case**: Ask about data types, scale, and search requirements
3. **Configure Appropriately**: Help choose right databases and LLM providers
4. **Show Examples**: Provide working code examples from the patterns above
5. **Explain Search Types**: Help users choose the right search strategy
6. **Debug Systematically**: Check configuration, data ingestion, cognify step
7. **Optimize Performance**: Suggest batch sizes, incremental updates, pruning
8. **Integrate Wisely**: Show how to combine with their existing stack (DLT, Dagster, LangGraph)
9. **Visualize Results**: Encourage visualization to understand graph structure
10. **Reference Documentation**: Point to https://docs.cognee.ai for details

## Resources

- **Documentation**: https://docs.cognee.ai
- **GitHub**: https://github.com/topoteretes/cognee
- **Website**: https://www.cognee.ai
- **License**: Apache 2.0
- **Python Support**: 3.10-3.13

## Installation

```bash
# Basic installation
pip install cognee

# With optional dependencies
pip install cognee[neo4j,postgres]

# CLI
cognee-cli -ui  # Launch full UI
cognee-cli add "text"
cognee-cli cognify
cognee-cli search "query"
```

---

## When This Skill Should Be Used

Invoke this skill when the user:
- Asks about knowledge graphs or graph databases
- Wants to build RAG systems or semantic search
- Needs persistent AI agent memory
- Is working with Cognee specifically
- Wants to integrate vector and graph databases
- Needs to process documents into queryable knowledge
- Is building conversational AI with memory
- Wants to unify data silos into knowledge graphs
- Asks about LlamaIndex, LangGraph, or MCP integrations with memory

## What This Skill Provides

- Complete Cognee API reference and patterns
- ECL pipeline best practices
- Search strategy guidance
- Integration patterns (DLT, LangGraph, MCP)
- Configuration examples
- Troubleshooting steps
- Performance optimization tips
- Real-world use case examples
