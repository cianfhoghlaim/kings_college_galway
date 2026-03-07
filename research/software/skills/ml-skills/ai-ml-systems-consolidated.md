# Comprehensive AI/ML Systems Architecture & Integration Guide

**Document Version:** 1.0
**Last Updated:** 2025-11-12
**Purpose:** Consolidated reference for AI compute strategy, multi-agent architecture, structured outputs, knowledge management, and document processing pipelines.

---

## Table of Contents

1. [Overview](#overview)
2. [AI Compute Strategy](#ai-compute-strategy)
3. [Multi-Agent Architecture with Agno AgentOS](#multi-agent-architecture)
4. [Structured Outputs with BAML, Pydantic, and Zod](#structured-outputs)
5. [Knowledge Management: Cognee vs CocoIndex](#knowledge-management)
6. [Document Processing Pipeline](#document-processing-pipeline)
7. [LLM Infrastructure with LiteLLM Gateway](#llm-infrastructure)
8. [Vector & Graph Databases](#vector-graph-databases)
9. [End-to-End Workflows](#end-to-end-workflows)
10. [Integration Patterns](#integration-patterns)
11. [Decision Matrices](#decision-matrices)
12. [Best Practices](#best-practices)

---

## Overview

This document consolidates research and architectural patterns for building production AI/ML systems that combine:

- **Tiered AI compute allocation** (planner vs worker models)
- **Multi-agent orchestration** with Agno AgentOS
- **Structured, schema-enforced outputs** via BAML → Pydantic (Python) and Zod (TypeScript)
- **Hybrid knowledge management** using Cognee and CocoIndex
- **Advanced document processing** with Docling, Unstract, Qwen-VL, and Crawl4AI
- **Unified LLM gateway** with LiteLLM for cost optimization and reliability
- **Vector and graph database integration** for semantic search and knowledge graphs

The architecture emphasizes **cost efficiency**, **type safety**, **incremental processing**, and **production reliability** across polyglot environments (Python and TypeScript).

---

## AI Compute Strategy

### The Tiered Allocation Framework

A sophisticated "Plan and Act" model separates high-reasoning "Planner" models from high-speed "Worker" models to optimize for both reasoning capability and cost/latency.

#### Planner Tier (High Reasoning)

Reserved for the orchestrator—the primary brain of agentic systems. Models excel at:
- Understanding ambiguous, complex, multi-step commands
- Decomposing tasks into logical sequences
- Synthesizing results from multiple worker agents

**Primary Models:**
- **GPT-5 Pro** – Premier choice for orchestration, unified reasoning across domains
- **Claude Code Max** – Excellent alternative with massive context window, SOTA code reasoning

**Use Cases:**
- Agentic orchestration and planning
- Complex code generation and refactoring
- Multi-step synthesis and decision-making

#### Worker Tier (High Volume, Low Cost)

Optimized for speed, low cost, and high throughput. Designed to execute simple, well-defined sub-tasks.

**Primary Models:**
- **Gemini 2.5 Flash** – Ideal workhorse for high-speed, low-cost execution
- **Z.ai GLM Coding Max** – Specialized coding model for routine code tasks

**Use Cases:**
- High-volume text extraction and structured data extraction
- Routine code generation and modification
- Batch processing of documents

#### Specialist Tier (Domain-Specific)

Models with unique, non-textual capabilities for specific domains.

**Visual Analysis (VLM):**
- **Gemini 2.5 Pro API** – SOTA native PDF/image understanding
- **Fine-tuned Qwen3-VL** – Domain-specific crypto charts/dashboards

**Safety & Filtering:**
- **gpt-oss-safeguard-20b** – Guardrail model for content classification

### AI Compute Allocation Matrix

| Task Category | Primary Compute | Secondary / Fallback | Not Recommended | Rationale |
|--------------|-----------------|---------------------|-----------------|-----------|
| **Agentic Orchestration** | GPT-5 Pro (API) | Claude Code Max (API) | Gemini Flash, Local M4 Max | Highest reasoning required |
| **High-Volume Text Extraction** | Gemini 2.5 Flash (API) | Z.ai GLM Max (API) | GPT-5 Pro, Claude Code Max | Cost-effective, high-speed |
| **Complex Code Generation** | Claude Code Max (API) | GPT-5 Pro (API) | Gemini Flash | SOTA code reasoning |
| **Routine Code Generation** | Z.ai GLM Coding Max (API) | GitHub Copilot | GPT-5 Pro | Specialized, lower-cost |
| **VLM/OCR (Documents)** | Gemini 2.5 Pro (API) | Fine-Tuned Qwen-VL | GPT-5 Pro | Native PDF/image understanding |
| **Local Dev & Testing** | M4 Max (llama.cpp) | Gemini 2.5 Flash (API) | GPT-5 Pro | Zero-cost iteration |
| **Production Fine-Tuned** | Hugging Face Pro Endpoint | Google Cloud Endpoint | M4 Max | Scalable, managed hosting |

### Local Development with M4 Max

The 48GB M4 Max laptop serves as a **zero-cost prototyping hub**:

- Run 33B parameter models at 4-bit quantization locally
- Eliminates API costs during prompt engineering and debugging
- Fast iteration cycle with llama.cpp OpenAI-compatible API
- Seamless transition to cloud APIs for production validation

**Setup Workflow:**
```bash
# Build llama.cpp with Metal support
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
make -j cc=mps

# Download quantized model
huggingface-cli download Qwen/Qwen2-7B-Instruct-GGUF qwen2-7b-instruct-q4_K_M.gguf --local-dir ./models

# Run local OpenAI-compatible server
build/bin/Release/llama-server -m models/qwen2-7b-instruct-q4_K_M.gguf -c 4096 --jinja
```

---

## Multi-Agent Architecture

### Agno AgentOS Framework

Agno provides a multi-agent framework with shared memory and external tool access via the **Model Context Protocol (MCP)**. Key features:

- **MCP Integration:** Standardized tool/data source connections
- **Knowledge Subsystem:** Connectors to 20+ vector stores (LanceDB, PgVector, etc.)
- **Shared Memory:** Redis-based persistence for session storage
- **Agent Collaboration:** Teams of agents working together on complex tasks

### Multi-Agent System Design

The architecture organizes agents into **domain-focused teams**, orchestrated by Dagster with DLT for data ingestion.

#### Team 1: Code & Documentation Analysis

**Responsibilities:**
- Analyze software repositories
- Summarize code structure and architecture
- Evaluate documentation completeness
- Extract key findings

**Pipeline Configuration:**

```python
from agno.agent import Agent
from agno.tools.hackernews import HackerNewsTools

# Repository Analysis Agent
repo_analyzer = Agent(
    name="RepoAnalyzer",
    model=llm_model,  # Gemini 2.5 Pro via LiteLLM
    instructions=[
        "1. Summarize the repository's purpose and architecture",
        "2. List key technologies and main topics",
        "3. Analyze code quality and patterns",
        "4. Evaluate documentation completeness",
        "5. Provide recommendations"
    ],
    output_schema=RepoSummary,  # BAML-defined schema
    db=db,  # Shared Redis
    knowledge=knowledge  # LanceDB index
)
```

**Data Flow:**
1. DLT ingests repository files → DuckLake storage
2. CocoIndex processes files → generates embeddings → LanceDB
3. Agno agents analyze via vector search → structured JSON output
4. Results stored in Cognee knowledge graph

#### Team 2: Sentiment Analysis & Social Monitoring

**Responsibilities:**
- Gather unstructured data from news/forums/social media
- Gauge public sentiment and trends
- Produce structured insights and alerts

**Pipeline Configuration:**

```python
# Sentiment Analysis Team
sentiment_team = Team(
    name="Sentiment Research Team",
    model=llm_model,
    members=[
        topic_extractor_agent,
        sentiment_summarizer_agent,
        web_searcher_agent
    ],
    instructions=[
        "1. Extract major topics from social posts",
        "2. Analyze sentiment for each topic",
        "3. Identify trending discussions",
        "4. Generate alerts for sentiment spikes"
    ],
    output_schema=SentimentReport,
    db=db,
    knowledge=knowledge
)
```

**Data Sources:**
- **Crawl4AI** for web scraping (news, HackerNews, forums)
- **API ingestion** for social media (Twitter, Discord, Reddit)
- **RisingWave** for real-time stream processing

**Output Structure:**
```json
{
  "overall_sentiment": "Slightly Negative",
  "positive_trends": ["Feature X excitement", "..."],
  "negative_trends": ["Service Y outage complaints", "..."],
  "notable_topics": [
    {
      "topic": "New Feature X Launch",
      "sentiment": "Positive",
      "summary": "Users celebrate the launch..."
    }
  ]
}
```

#### Team 3: Financial Analytics & Anomaly Detection

**Responsibilities:**
- Ingest market data, on-chain transactions, DeFi metrics
- Identify trends, correlations, and anomalies
- Real-time analytics with historical context

**Pipeline Configuration:**

**Batch Ingestion:**
- DLT for periodic API pulls (prices, volumes, protocol stats)
- DuckDB storage with incremental loading

**Stream Processing:**
- RisingWave for live blockchain events and trades
- Ibis for unified batch/stream transformations

**Agent Workflow:**
```python
# Trend Summarizer Agent
trend_agent = Agent(
    name="TrendSummarizer",
    instructions=[
        "Analyze latest crypto market trends",
        "Summarize key metrics (price, volume, TVL)",
        "Identify notable events in last 24h",
        "Provide forward-looking outlook"
    ],
    output_schema=MarketTrendReport
)

# Anomaly Detector Agent
anomaly_agent = Agent(
    name="AnomalyDetector",
    instructions=[
        "Detect outliers in market data",
        "Correlate with news/social sentiment",
        "Explain likely causes",
        "Assess impact"
    ],
    output_schema=AnomalyAlert
)
```

### Integration with MCP

Agno's MCP support enables:
- FastAPI endpoint (`/mcp`) for agent control
- External tool integration (Cognee, CocoIndex)
- Standardized interfaces across platforms

**Example MCP Tool Integration:**
```python
from agno.os import AgentOS

agent_os = AgentOS(teams=[research_team])
agent_os.enable_mcp_server = True
app = agent_os.get_app()  # FastAPI app with MCP endpoints
```

---

## Structured Outputs

### BAML (Boundary AI Markup Language)

BAML provides a DSL for defining:
- Agent behaviors and prompts
- Structured output schemas
- Tool interactions

**Key Benefits:**
- Schema-aligned parsing ensures LLM outputs match expected format
- Single source of truth for domain models
- Multi-language code generation (Python/TypeScript)

### End-to-End Schema Modeling

#### 1. Define Domain Classes in BAML

```baml
// Domain schema definitions in BAML
class DocumentChunk {
  id string                 @description("Unique chunk identifier")
  repo string               @description("Repository name or ID")
  file_path string?         @description("Source file path (if applicable)")
  content string            @description("Text content of the chunk")
  embedding float[]         @description("Embedding vector of the content")
}

class ToolCall {
  tool_name string          @description("Name of the tool to invoke")
  params map<string, any>   @description("Input parameters for the tool")
  result any?               @description("Result returned by the tool")
}

class RepoSummary {
  repo_name string          @description("Repository name")
  description string        @description("High-level description")
  main_topics string[]      @description("Key topics or technologies")
  file_count int            @description("Number of files analyzed")
}
```

#### 2. Generate Pydantic Models (Python)

Run `baml-cli generate` to create:

```python
# Auto-generated in baml_client/types.py
from pydantic import BaseModel, Field
from typing import List, Optional, Any, Dict

class DocumentChunk(BaseModel):
    id: str
    repo: str
    file_path: Optional[str] = None
    content: str
    embedding: List[float]

class ToolCall(BaseModel):
    tool_name: str
    params: Dict[str, Any]
    result: Optional[Any] = None

class RepoSummary(BaseModel):
    repo_name: str
    description: str
    main_topics: List[str]
    file_count: int
```

#### 3. Generate TypeScript Types and Zod Schemas

```typescript
// Auto-generated in baml_client/types.ts
export interface DocumentChunk {
  id: string;
  repo: string;
  file_path?: string;
  content: string;
  embedding: number[];
}

// Corresponding Zod schema
import { z } from 'zod';

export const DocumentChunkSchema = z.object({
  id: z.string(),
  repo: z.string(),
  file_path: z.string().optional(),
  content: z.string(),
  embedding: z.array(z.number())
});

export type DocumentChunk = z.infer<typeof DocumentChunkSchema>;
```

### Integration Across Stack

**DLT Pipeline:**
```python
import dlt
from baml_client.types import DocumentChunk

@dlt.resource(name="document_chunks")
def ingest_repo_chunks(repo_path: str):
    for chunk_text in split_file_to_chunks(repo_path):
        chunk = DocumentChunk(
            id=compute_chunk_id(repo_path, ...),
            repo=repo_path,
            file_path=current_file_path,
            content=chunk_text,
            embedding=embed(chunk_text)
        )
        yield chunk.dict()
```

**Hono API (TypeScript):**
```typescript
import { Hono } from 'hono';
import { UserQuerySchema, DocumentChunkSchema } from './schemas';

const app = new Hono();

app.post('/ask', async (c) => {
  const body = await c.req.json();
  const userQuery = UserQuerySchema.parse(body);  // Runtime validation

  const results = await searchMemory(userQuery);
  const safeResults = DocumentChunkSchema.array().parse(results);

  return c.json(safeResults);
});
```

**Drizzle ORM:**
```typescript
import { pgTable, text, integer } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";

export const documentChunks = pgTable("document_chunks", {
  id: text("id").primaryKey(),
  repo: text("repo").notNull(),
  file_path: text("file_path"),
  content: text("content").notNull(),
  embedding: text("embedding")  // store as JSON or vector type
});

export const DocumentChunkInsertSchema = createInsertSchema(documentChunks);
```

---

## Knowledge Management

### Cognee vs CocoIndex Decision Matrix

Both tools provide vector + graph capabilities but with different focuses:

| Feature | Cognee | CocoIndex |
|---------|--------|-----------|
| **Primary Focus** | Ready-to-use AI memory layer | Data engineering toolkit for indexing |
| **Query Interface** | Built-in (Python API + MCP server) | Manual (query LanceDB/Neo4j directly) |
| **Retrieval Strategies** | Multiple (vector, graph, hybrid GraphRAG) | Requires custom implementation |
| **Pipeline Control** | Opinionated (simple ingestion) | Fine-grained (custom flows, incremental) |
| **Online Learning** | Supported (feedback loops) | Not built-in |
| **Storage Backends** | Neo4j, Memgraph, Kuzu, LanceDB, Redis | Neo4j, Memgraph, Kuzu, LanceDB, Qdrant, Postgres |
| **Best For** | Runtime query answering, agent memory | Complex ETL, custom ontologies, large-scale |

### Recommended Integration Strategy

**Use CocoIndex for Indexing + Cognee for Querying:**

1. **CocoIndex** handles heavy lifting:
   - Custom extraction logic
   - Incremental updates with Postgres triggers
   - Outputs to LanceDB (vectors) + Memgraph (graph)

2. **Cognee** provides intelligent retrieval:
   - Smart query interface (hybrid retrieval)
   - MCP server for agent integration
   - Memory management and feedback loops

**Example Workflow:**

```python
# 1. CocoIndex: Build knowledge base
import cocoindex
from cocoindex.sources import LocalFile

@cocoindex.flow_def(name="DocIndex")
def doc_index_flow(flow_builder, data_scope):
    # Ingest documents
    data_scope["files"] = flow_builder.add_source(
        LocalFile(path="/docs", included_patterns=["*.md", "*.py"])
    )

    # Extract relationships with LLM
    with data_scope["files"].row() as file:
        file["relationships"] = file["content"].transform(
            cocoindex.functions.ExtractByLlm(
                instruction="Extract subject-predicate-object triples",
                output_schema=Relationship
            )
        )

    # Export to Memgraph and LanceDB
    data_scope.add_collector("doc_graph").collect(
        entity=file["relationships"]["subject"],
        relation=file["relationships"]["predicate"],
        target=file["relationships"]["object"]
    ).export("knowledge_graph", cocoindex.storages.Memgraph())

# 2. Cognee: Query the indexed data
import cognee

# Point Cognee to existing Memgraph + LanceDB
cognee.config.set_graph_db(memgraph_connection)
cognee.config.set_vector_db(lancedb_connection)

# Query with intelligent retrieval
results = cognee.search(
    "What are the key features of DocumentChunk?",
    search_type="GraphCompletion"  # Combines graph + vector
)
```

### Cognee Features

**Extract-Cognify-Load Pipeline:**
- Chunks documents automatically
- Generates embeddings for semantic search
- Identifies entities and relationships using LLMs
- Builds dynamic knowledge graph

**Hybrid Retrieval Modes:**
- **Pure Vector:** Semantic similarity search
- **Pure Graph:** Structured traversal (Cypher queries)
- **GraphRAG:** Combined approach for complex queries

**MCP Server Integration:**
```python
from cognee import MCP

# Run Cognee as MCP server
cognee_server = MCP()
cognee_server.start()

# Agno agents can now query via MCP
# No custom glue code needed
```

### CocoIndex Features

**Incremental Processing:**
- Postgres-based state tracking
- Only processes new/modified data
- Real-time updates with triggers

**LLM-Powered Transformations:**
```python
# Extract structured data with LLM
chunk["summary"] = chunk["content"].transform(
    cocoindex.functions.ExtractByLlm(
        instruction="Summarize this code chunk in 2 sentences",
        output_schema=CodeSummary,
        llm_spec=LlmSpec(api_type="openai", model="gpt-4o")
    )
)

# Generate embeddings
chunk["embedding"] = chunk["content"].transform(
    cocoindex.functions.EmbedText(
        llm_spec=LlmSpec(api_type="openai", model="text-embedding-3-small")
    )
)
```

**Multi-Backend Support:**
- OpenAI, Anthropic, Google Vertex, Azure
- Ollama for local models
- LiteLLM for unified interface

---

## Document Processing Pipeline

### Advanced PDF Parsing Stack

Integrate **Docling**, **Unstract**, **Qwen-VL**, and **Crawl4AI** for comprehensive document processing.

#### Tool Roles

| Tool | Primary Role | Integration Point | Output/Focus |
|------|-------------|-------------------|--------------|
| **Crawl4AI** | PDF retrieval & initial text extraction | Base pipeline | Clean Markdown, basic structure |
| **Docling** | Advanced parsing (layout, tables, OCR) | Replace/augment Crawl4AI parsing | Rich Markdown/HTML/JSON with structure |
| **Unstract** | LLM-powered data extraction (ETL) | Post-processing after text extraction | Structured data (JSON, key fields) |
| **Qwen-VL** | Vision-Language model for images/vision | Assist during parsing or post-process | Text from images, OCR, descriptions |

#### PDF Processing Workflow

**1. Fetch PDF with Crawl4AI:**
```python
from crawl4ai import AsyncWebCrawler

async with AsyncWebCrawler() as crawler:
    result = await crawler.arun(
        url="https://example.com/document.pdf",
        pdf=True  # Get raw PDF bytes
    )
    pdf_bytes = result.pdf
```

**2. Parse with Docling (with VLM support):**
```python
from docling import DocumentConverter
from docling.vlm import VlmPipeline

# Configure Docling with Qwen-VL for vision tasks
vlm_pipeline = VlmPipeline(
    model="qwen-vl",
    endpoint="http://localhost:8080/v1"  # Local Qwen-VL server
)

converter = DocumentConverter(vlm_pipeline=vlm_pipeline)
result = converter.convert(pdf_bytes)

# Get structured output
markdown = result.to_markdown()  # Preserves tables, formulas (LaTeX)
json_output = result.to_json()   # Full structure with metadata
```

**3. Extract Structured Data with Unstract:**
```python
import requests

# Send parsed markdown to Unstract API
unstruct_response = requests.post(
    "http://unstract-api:8000/extract",
    json={
        "text": markdown,
        "schema": {
            "invoice_number": "string",
            "date": "string",
            "total_amount": "float",
            "line_items": ["object"]
        }
    }
)

structured_data = unstruct_response.json()
```

**4. Handle Images with Qwen-VL:**
```python
from qwen_vl import QwenVL

model = QwenVL(model_path="Qwen/Qwen-VL-Chat")

# For scanned pages or complex diagrams
for page_image in extract_page_images(pdf_bytes):
    ocr_text = model.generate(
        image=page_image,
        prompt="Read the following document page and output the text in Markdown format."
    )
    # Append OCR text to markdown
```

### Integration Examples

**Example 1: Research Paper Processing**

Complex PDF with tables, formulas, multi-column layout:

```python
async def process_research_paper(pdf_url):
    # 1. Fetch with Crawl4AI
    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(url=pdf_url, pdf=True)

    # 2. Parse with Docling (preserves formulas as LaTeX)
    converter = DocumentConverter(
        vlm_pipeline=VlmPipeline(model="qwen-vl")
    )
    doc = converter.convert(result.pdf)

    # 3. Extract metadata with Unstract
    metadata = unstract_extract(
        text=doc.to_markdown(),
        schema={
            "title": "string",
            "authors": ["string"],
            "abstract": "string",
            "key_findings": ["string"]
        }
    )

    return {
        "markdown": doc.to_markdown(),
        "metadata": metadata,
        "images": doc.images
    }
```

**Example 2: Invoice Batch Processing**

High-volume structured data extraction:

```python
async def process_invoice_batch(pdf_urls):
    results = []

    for url in pdf_urls:
        # Crawl4AI for fast retrieval
        pdf_bytes = await fetch_pdf(url)

        # Docling for accurate table extraction
        doc = DocumentConverter().convert(pdf_bytes)

        # Unstract with Gemini 2.5 Flash (high-volume, low-cost)
        invoice_data = unstract_extract(
            text=doc.to_markdown(),
            schema=InvoiceSchema,
            model="gemini-2.5-flash"
        )

        results.append(invoice_data)

    return results
```

### Docling VLM Integration

Docling can use Qwen-VL to produce structured HTML from PDF pages:

```python
from docling.vlm import VlmPipeline

# Configure VLM pipeline
vlm = VlmPipeline(
    model="qwen-vl-max",
    prompt_template="""
    You are an AI that converts document images to structured HTML.
    Output well-formatted HTML with:
    - Proper heading hierarchy (h1, h2, h3)
    - Tables using <table> tags
    - Math formulas in LaTeX: $formula$
    - Preserve all text content
    """
)

# Docling will call Qwen-VL for each page
converter = DocumentConverter(vlm_pipeline=vlm)
result = converter.convert(pdf_path)

# Result includes:
# - Full HTML with preserved structure
# - LaTeX formulas
# - Table data in semantic markup
```

---

## LLM Infrastructure

### LiteLLM as Centralized Gateway

LiteLLM serves as a **self-hosted proxy server** (LLM Gateway) providing:

1. **Centralized Key Management**
2. **Intelligent Routing**
3. **Unified Observability**

#### Architecture Benefits

- **Single Endpoint:** All services call LiteLLM proxy (no direct provider access)
- **Cost Control:** Track usage, enforce budgets, route to cost-effective models
- **Provider Abstraction:** Switch models without changing application code
- **Fallback & Load Balancing:** Automatic failover, cost-based routing

#### LiteLLM Configuration

```yaml
# config.yaml - Define all physical compute assets

model_list:
  # Planner models
  - model_name: gpt-5-pro
    litellm_params:
      model: gpt-5-pro
      api_key: ${OPENAI_API_KEY}

  - model_name: claude-code-max
    litellm_params:
      model: claude-code-max
      api_key: ${ANTHROPIC_API_KEY}

  # Worker models
  - model_name: gemini-2.5-flash
    litellm_params:
      model: gemini/gemini-2.5-flash
      api_key: ${GEMINI_API_KEY}

  - model_name: zai-glm-max
    litellm_params:
      model: openrouter/zai/glm-coding-max
      api_key: ${OPENROUTER_API_KEY}

  # Specialist models
  - model_name: qwen-vl-tuned
    litellm_params:
      model: huggingface/qwen-vl-custom
      api_base: ${HF_ENDPOINT_URL}
      api_key: ${HF_API_KEY}

  # Local development
  - model_name: local-dev-model
    litellm_params:
      model: openai/local-model
      api_base: http://localhost:8080/v1
      api_key: unused

# Virtual model aliases (Plan and Act tiers)
router:
  - model_group: planner_agent
    models:
      - gpt-5-pro
      - claude-code-max
    routing_strategy: priority  # Try first, then fallback

  - model_group: worker_agent
    models:
      - gemini-2.5-flash
      - zai-glm-max
    routing_strategy: cost_based  # Choose cheapest available

  - model_group: vision_agent
    models:
      - gemini-2.5-pro
      - qwen-vl-tuned
    routing_strategy: least_busy

# Observability
logging:
  - type: langfuse
    langfuse_public_key: ${LANGFUSE_PUBLIC_KEY}
    langfuse_secret_key: ${LANGFUSE_SECRET_KEY}

  - type: postgres
    db_url: ${POSTGRES_URL}
    table_name: llm_logs
```

#### Agent Integration

Agno agents use virtual model aliases:

```python
from agno.agent import Agent
from agno.models.openai import OpenAIChat

# Configure to call LiteLLM proxy
LITELLM_PROXY_URL = "https://api.yourdomain.com/v1"
LITELLM_API_KEY = "sk-litellm-internal-key"

# Planner agent
planner = Agent(
    name="MasterPlanner",
    model=OpenAIChat(
        id="planner_agent",  # Virtual alias, not actual model
        api_base=LITELLM_PROXY_URL,
        api_key=LITELLM_API_KEY
    ),
    instructions=[...]
)

# Worker agent
worker = Agent(
    name="DataExtractor",
    model=OpenAIChat(
        id="worker_agent",  # Routes to Gemini Flash or GLM Max
        api_base=LITELLM_PROXY_URL,
        api_key=LITELLM_API_KEY
    ),
    instructions=[...]
)
```

### Hosting with Pangolin

**Public Access Model (Open Endpoint):**

Expose LiteLLM via HTTPS for external integrations:

```yaml
# Pangolin Resource Configuration
Domain: api.example.com
Resource Type: HTTP (Pangolin handles TLS on port 443)
Target: 127.0.0.1:4000  # LiteLLM local endpoint
Access Control: Allow (no auth required for API key-protected service)
```

**Private Access Model (VPN-Gated):**

Restrict LiteLLM to team members via Olm VPN:

```yaml
# Site Resource Configuration
Type: TCP
Port: 4000
Target: 127.0.0.1:4000
Access: Olm VPN clients only
```

Team members connect via Olm client:
```bash
olm --id <client-id> --secret <secret> --endpoint https://pangolin-server
```

Then access LiteLLM at `http://100.90.128.0:4000` (site VPN IP).

---

## Vector & Graph Databases

### LanceDB (Vector Store)

**Key Features:**
- Apache Arrow-based for high performance
- Embedded or client-server mode
- Native support in Agno, CocoIndex, Cognee

**Usage with CocoIndex:**
```python
from cocoindex.storages import LanceDB

# Export embeddings to LanceDB
data_scope.add_collector("code_index").collect(
    id=chunk["id"],
    text=chunk["content"],
    vector=chunk["embedding"]
).export(
    "code_vectors",
    LanceDB(uri="./lancedb", table_name="code_chunks")
)
```

**Usage with Cognee:**
```python
import cognee

# Configure Cognee to use existing LanceDB
cognee.config.set_vector_db({
    "type": "lancedb",
    "uri": "./lancedb"
})

# Cognee will use LanceDB for semantic search
results = cognee.search("query", search_type="VECTOR")
```

### Memgraph (Graph Database)

**Key Features:**
- Neo4j protocol-compatible (Cypher queries)
- In-memory for high-speed traversal
- Real-time analytics with streaming support

**Usage with CocoIndex:**
```python
from cocoindex.storages import Memgraph

# Export relationships to Memgraph
data_scope.add_collector("knowledge_graph").collect(
    subject=relationship["subject"],
    predicate=relationship["predicate"],
    object=relationship["object"]
).export(
    "doc_graph",
    Memgraph(
        connection_string="bolt://localhost:7687",
        node_labels={"Entity": ["subject", "object"]},
        edge_types={"RELATIONSHIP": "predicate"}
    )
)
```

**Usage with Cognee:**
```python
# Configure Cognee to use Memgraph
cognee.config.set_graph_db({
    "type": "memgraph",
    "uri": "bolt://localhost:7687",
    "username": "memgraph",
    "password": ""
})

# Cognee can perform graph traversals
results = cognee.search("query", search_type="GRAPH")
```

### DuckLake + RisingWave (Batch + Stream)

**DuckLake (DuckDB Lakehouse):**
- Parquet-backed storage
- PostgreSQL catalog for metadata
- ACID compliance for analytics

**RisingWave (Streaming):**
- Real-time materialized views
- Sub-second latency for incremental queries
- Kafka, CDC, WebSocket connectors

**Unified with Ibis:**
```python
import ibis

# Define transformation once
def compute_metrics(table):
    return (
        table
        .group_by("token")
        .aggregate(
            volume=table["amount"].sum(),
            avg_price=table["price"].mean(),
            count=table.count()
        )
    )

# Run on DuckDB (batch)
duckdb_conn = ibis.duckdb.connect("data.ddb")
batch_metrics = compute_metrics(duckdb_conn.table("transactions"))

# Run on RisingWave (stream)
risingwave_conn = ibis.postgres.connect(
    host="risingwave", port=4566, database="dev"
)
stream_metrics = compute_metrics(risingwave_conn.table("transactions"))
```

---

## End-to-End Workflows

### Workflow 1: Local Git Repository Analysis

**Objective:** Ingest, index, and analyze local code repositories with LLM agents.

**Tools:** DLT, CocoIndex, Repomix, Agno, BAML

**Steps:**

1. **Ingest with DLT:**
```python
import dlt
from dlt.sources.filesystem import filesystem_source

repo_path = "/path/to/repo"
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

2. **Index with CocoIndex:**
```python
@cocoindex.flow_def(name="CodeIndex")
def code_index_flow(flow_builder, data_scope):
    # Ingest files
    data_scope["files"] = flow_builder.add_source(
        LocalFile(
            path=repo_path,
            included_patterns=["*.py", "*.cpp", "*.md"]
        )
    )

    # Parse and chunk code
    with data_scope["files"].row() as file:
        file["chunks"] = file["content"].transform(
            cocoindex.functions.SplitRecursively(),
            language=file["ext"],
            chunk_size=1000
        )

        # Generate embeddings
        with file["chunks"].row() as chunk:
            chunk["embedding"] = chunk["text"].transform(
                cocoindex.functions.SentenceTransformerEmbed(
                    model="all-MiniLM-L6-v2"
                )
            )

    # Export to vector store
    data_scope.add_collector("code_index").collect(
        filename=file["filename"],
        text=chunk["text"],
        vector=chunk["embedding"]
    ).export("code_vectors", cocoindex.storages.LanceDB())
```

3. **Package with Repomix:**
```bash
repomix --compress --output-format xml
```

4. **Analyze with Agno:**
```python
from agno.agent import Agent

# Load Repomix output
with open("repomix-output.xml") as f:
    repo_context = f.read()

# Create analysis agent
repo_analyzer = Agent(
    name="RepoAnalyzer",
    model=OpenAIChat(id="planner_agent"),
    instructions=[
        "1. Summarize repository purpose and architecture",
        "2. List key technologies and patterns",
        "3. Evaluate code quality and documentation",
        "4. Provide recommendations"
    ],
    output_schema=RepoSummary
)

# Run analysis
analysis = repo_analyzer.run(repo_context)
print(analysis)
```

### Workflow 2: Real-Time Sentiment Monitoring

**Objective:** Monitor social media sentiment with streaming analytics.

**Tools:** Crawl4AI, DLT, RisingWave, CocoIndex, Agno

**Steps:**

1. **Ingest Social Data:**
```python
# Crawl4AI for public sources
async def crawl_news():
    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(
            url="https://news.ycombinator.com",
            extraction_strategy=LLMExtractionStrategy(
                provider="openai/gpt-4o-mini",
                instruction="Extract article titles and summaries"
            )
        )
    return result

# DLT for API sources (Reddit, Twitter)
@dlt.resource
def reddit_posts():
    reddit = praw.Reddit(...)
    for post in reddit.subreddit("cryptocurrency").hot(limit=100):
        yield {
            "id": post.id,
            "title": post.title,
            "content": post.selftext,
            "score": post.score,
            "created": post.created_utc
        }
```

2. **Stream Processing with RisingWave:**
```sql
-- Create materialized view for rolling sentiment
CREATE MATERIALIZED VIEW sentiment_trends AS
SELECT
    window_start,
    topic,
    AVG(sentiment_score) as avg_sentiment,
    COUNT(*) as post_count
FROM TUMBLE(posts, created, INTERVAL '5' MINUTE)
GROUP BY window_start, topic;

-- Alert on sentiment spikes
CREATE SINK sentiment_alerts AS
SELECT * FROM sentiment_trends
WHERE ABS(avg_sentiment - LAG(avg_sentiment) OVER (PARTITION BY topic ORDER BY window_start)) > 0.3
INTO kafka_sink;
```

3. **LLM Analysis with Agno:**
```python
# Triggered by RisingWave alert
def analyze_sentiment_spike(topic, posts):
    sentiment_agent = Agent(
        name="SentimentAnalyzer",
        model=OpenAIChat(id="worker_agent"),  # Gemini Flash
        instructions=[
            "Analyze sentiment of these posts",
            "Identify main positive and negative themes",
            "Explain the sentiment shift"
        ],
        output_schema=SentimentReport
    )

    return sentiment_agent.run(posts)
```

### Workflow 3: Multi-Modal Document Processing

**Objective:** Extract structured data from complex PDFs with images.

**Tools:** Crawl4AI, Docling, Qwen-VL, Unstract, CocoIndex

**Steps:**

1. **Fetch PDFs:**
```python
async def fetch_documents(urls):
    async with AsyncWebCrawler() as crawler:
        for url in urls:
            result = await crawler.arun(url=url, pdf=True)
            yield result.pdf
```

2. **Parse with Docling + Qwen-VL:**
```python
from docling import DocumentConverter
from docling.vlm import VlmPipeline

# Configure VLM for image-heavy documents
vlm = VlmPipeline(
    model="qwen-vl",
    endpoint="http://localhost:8080/v1"
)

converter = DocumentConverter(vlm_pipeline=vlm)

async for pdf_bytes in fetch_documents(urls):
    doc = converter.convert(pdf_bytes)

    # Get structured output
    yield {
        "markdown": doc.to_markdown(),
        "tables": doc.tables,
        "images": doc.images,
        "formulas": doc.formulas  # LaTeX format
    }
```

3. **Extract with Unstract:**
```python
for doc in parsed_documents:
    structured_data = unstract_extract(
        text=doc["markdown"],
        schema={
            "title": "string",
            "authors": ["string"],
            "abstract": "string",
            "key_findings": ["string"],
            "financial_data": {
                "revenue": "float",
                "expenses": "float",
                "profit": "float"
            }
        },
        model="gemini-2.5-flash"
    )

    # Store in database
    db.insert("documents", structured_data)
```

4. **Index for Search:**
```python
@cocoindex.flow_def(name="DocIndex")
def index_documents(flow_builder, data_scope):
    # Load from database
    data_scope["docs"] = flow_builder.add_source(
        PostgresSource(table="documents")
    )

    # Generate embeddings
    with data_scope["docs"].row() as doc:
        doc["embedding"] = doc["abstract"].transform(
            cocoindex.functions.EmbedText(
                llm_spec=LlmSpec(
                    api_type="openai",
                    model="text-embedding-3-small"
                )
            )
        )

    # Export to vector store
    data_scope.add_collector("doc_index").export(
        "document_vectors",
        cocoindex.storages.LanceDB()
    )
```

---

## Integration Patterns

### Pattern 1: Tiered LLM Orchestration

**Principle:** Use expensive models for planning, cheap models for execution.

**Implementation:**
```python
class TieredAgent:
    def __init__(self):
        self.planner = OpenAIChat(id="planner_agent")  # GPT-5 Pro
        self.worker = OpenAIChat(id="worker_agent")    # Gemini Flash

    async def execute_task(self, task: str):
        # Step 1: Plan with expensive model
        plan = await self.planner.generate(
            f"Break down this task into subtasks: {task}"
        )

        # Step 2: Execute subtasks with cheap model
        results = []
        for subtask in plan.subtasks:
            result = await self.worker.generate(
                f"Execute: {subtask.description}"
            )
            results.append(result)

        # Step 3: Synthesize with expensive model
        final = await self.planner.generate(
            f"Synthesize these results: {results}"
        )

        return final
```

### Pattern 2: Incremental Knowledge Graph Building

**Principle:** Only process new/changed data, maintain graph consistency.

**Implementation:**
```python
@cocoindex.flow_def(name="IncrementalKG")
def incremental_kg_flow(flow_builder, data_scope):
    # Track state with Postgres
    data_scope["files"] = flow_builder.add_source(
        LocalFile(path="/docs"),
        state_table="file_processing_state"
    )

    # CocoIndex automatically detects new/changed files
    with data_scope["files"].row() as file:
        # Only process if file changed
        if file.is_new_or_modified:
            # Extract entities and relationships
            file["entities"] = file["content"].transform(
                cocoindex.functions.ExtractByLlm(
                    instruction="Extract named entities",
                    output_schema=EntityList
                )
            )

            file["relationships"] = file["content"].transform(
                cocoindex.functions.ExtractByLlm(
                    instruction="Extract relationships as SPO triples",
                    output_schema=RelationshipList
                )
            )

    # Merge into graph (upsert nodes/edges)
    data_scope.add_collector("kg").export(
        "knowledge_graph",
        cocoindex.storages.Memgraph(
            merge_strategy="upsert"  # Update existing, insert new
        )
    )
```

### Pattern 3: Hybrid Retrieval (Vector + Graph)

**Principle:** Combine semantic search with structured queries.

**Implementation:**
```python
class HybridRetriever:
    def __init__(self):
        self.vector_db = LanceDB(uri="./lancedb")
        self.graph_db = Memgraph(uri="bolt://localhost:7687")

    async def search(self, query: str, mode: str = "hybrid"):
        if mode == "vector":
            # Pure semantic search
            return self.vector_db.search(
                query_vector=embed(query),
                limit=10
            )

        elif mode == "graph":
            # Pure graph traversal
            entities = extract_entities(query)
            cypher = f"""
                MATCH (e:Entity)-[r]->(t:Entity)
                WHERE e.name IN {entities}
                RETURN e, r, t
            """
            return self.graph_db.query(cypher)

        else:  # hybrid
            # 1. Vector search for relevant documents
            docs = self.vector_db.search(
                query_vector=embed(query),
                limit=20
            )

            # 2. Extract entities from top docs
            entities = set()
            for doc in docs[:5]:
                entities.update(extract_entities(doc.text))

            # 3. Graph search for related entities
            cypher = f"""
                MATCH path = (e:Entity)-[*1..2]-(related:Entity)
                WHERE e.name IN {list(entities)}
                RETURN path
                LIMIT 50
            """
            graph_results = self.graph_db.query(cypher)

            # 4. Combine and rank
            return self.merge_results(docs, graph_results)
```

### Pattern 4: Schema-Driven Development

**Principle:** Define schema once in BAML, generate code for all layers.

**Workflow:**
```bash
# 1. Define schema in BAML
cat > baml_src/domain.baml <<EOF
class DocumentChunk {
  id string
  content string
  embedding float[]
}
EOF

# 2. Generate Python (Pydantic)
baml-cli generate --target python

# 3. Generate TypeScript (Zod)
baml-cli generate --target typescript

# 4. Use in DLT pipeline
cat > pipelines/ingest.py <<EOF
from baml_client.types import DocumentChunk

@dlt.resource
def chunks():
    for chunk in process_files():
        yield DocumentChunk(**chunk).dict()
EOF

# 5. Use in Hono API
cat > api/routes.ts <<EOF
import { DocumentChunkSchema } from './schemas';

app.post('/chunks', async (c) => {
    const body = await c.req.json();
    const chunk = DocumentChunkSchema.parse(body);
    await db.insert(chunk);
    return c.json({ success: true });
});
EOF
```

---

## Decision Matrices

### When to Use Which Model

| Scenario | Recommended Model | Rationale |
|----------|------------------|-----------|
| Complex multi-step planning | GPT-5 Pro | Highest reasoning, SOTA orchestration |
| Large codebase refactoring | Claude Code Max | 200k context, excellent code understanding |
| High-volume data extraction | Gemini 2.5 Flash | 10x cheaper than GPT-4, optimized for speed |
| PDF/image understanding | Gemini 2.5 Pro | Native document parsing, OCR |
| Domain-specific vision | Fine-tuned Qwen-VL | Custom training for crypto charts, etc. |
| Local development/testing | Llama 3 8B (M4 Max) | Zero cost, private, fast iteration |
| Routine code generation | Z.ai GLM Coding Max | Specialized, lower cost than GPT-4 |

### Cognee vs CocoIndex

| Use Case | Recommended Tool | Reasoning |
|----------|-----------------|-----------|
| Quick prototype with existing data | Cognee alone | Simplest setup, built-in retrieval |
| Custom ontology with complex ETL | CocoIndex alone | Fine-grained control |
| Large-scale production system | Both (CocoIndex → Cognee) | Best of both worlds |
| Real-time updates from DB | CocoIndex | Native Postgres trigger support |
| Agent memory with feedback loops | Cognee | Built-in online learning |
| Multi-modal RAG (text + images) | Both | CocoIndex for indexing, Cognee for retrieval |

### Vector Store Selection

| Database | Best For | Avoid For |
|----------|----------|-----------|
| **LanceDB** | Embedded usage, prototypes, local-first | Distributed systems needing sharding |
| **Qdrant** | Production, high-scale, filtering | Simple use cases (overkill) |
| **Postgres + pgvector** | When you already use Postgres | Billion-scale vectors |
| **Redis** | Low-latency caching, real-time | Primary vector store (persistence) |

### Stream vs Batch Processing

| Data Characteristics | Recommended Approach | Implementation |
|---------------------|---------------------|----------------|
| Low volume (<1M records/day) | Batch only (DuckDB) | DLT → DuckDB → CocoIndex |
| High volume, hourly updates | Mini-batch (DuckDB + triggers) | DLT (hourly) → Postgres triggers → CocoIndex |
| Real-time events (>10k/sec) | Streaming (RisingWave) | Kafka → RisingWave → Materialized views |
| Mixed: batch history + real-time | Lambda architecture (both) | DuckDB (batch) + RisingWave (stream) + Ibis (unified) |

---

## Best Practices

### Cost Optimization

1. **Tiered Model Usage:**
   - Never use GPT-5 for tasks Gemini Flash can handle
   - Route 90% of requests to worker models
   - Reserve planners for orchestration only

2. **Caching Strategies:**
   ```python
   from functools import lru_cache

   @lru_cache(maxsize=1000)
   def get_embedding(text: str) -> List[float]:
       # Cache embeddings to avoid re-computing
       return embed(text)
   ```

3. **Batch Processing:**
   ```python
   # Bad: One API call per item
   for item in items:
       result = llm.generate(item)

   # Good: Batch items in single call
   results = llm.generate_batch(items)
   ```

4. **Incremental Updates:**
   - Use CocoIndex's state tracking
   - Only re-embed changed content
   - Leverage Postgres triggers for real-time indexing

5. **Monitor & Alert:**
   ```python
   # Track costs in LiteLLM
   # Set budgets per model group
   litellm.set_budget("worker_agent", max_cost_per_hour=10.0)
   ```

### Reliability & Observability

1. **Schema Validation:**
   - Always validate LLM outputs with Pydantic/Zod
   - Fail fast on schema mismatches
   - Log validation errors for prompt tuning

2. **Retry Logic:**
   ```python
   from tenacity import retry, stop_after_attempt, wait_exponential

   @retry(
       stop=stop_after_attempt(3),
       wait=wait_exponential(min=1, max=10)
   )
   async def call_llm_with_retry(prompt: str):
       return await llm.generate(prompt)
   ```

3. **Observability Stack:**
   - **LiteLLM** → Langfuse for LLM traces
   - **Dagster** for pipeline orchestration logs
   - **CocoIndex** → Postgres for data lineage
   - **Memgraph** for graph analytics

4. **Health Checks:**
   ```python
   async def health_check():
       # Check all critical services
       checks = {
           "litellm": await ping_litellm(),
           "vector_db": await ping_lancedb(),
           "graph_db": await ping_memgraph(),
           "redis": await ping_redis()
       }
       return all(checks.values())
   ```

### Security & Privacy

1. **API Key Management:**
   - Store keys only in LiteLLM proxy config
   - Use environment variables or secret managers
   - Rotate keys regularly

2. **Data Isolation:**
   - Use Pangolin Olm VPN for internal services
   - Public endpoints only for intended APIs
   - Implement rate limiting on public routes

3. **PII Protection:**
   ```python
   # Redact sensitive data before sending to LLM
   from presidio_analyzer import AnalyzerEngine

   analyzer = AnalyzerEngine()

   def redact_pii(text: str) -> str:
       results = analyzer.analyze(text, entities=["PERSON", "EMAIL", "PHONE"])
       return redact(text, results)
   ```

4. **Audit Logging:**
   - Log all LLM requests/responses
   - Track data lineage through pipeline
   - Maintain compliance audit trail

### Performance Optimization

1. **Parallel Processing:**
   ```python
   import asyncio

   # Process multiple documents concurrently
   async def process_batch(docs):
       tasks = [process_doc(doc) for doc in docs]
       return await asyncio.gather(*tasks)
   ```

2. **Smart Chunking:**
   ```python
   # Context-aware chunking for code
   from cocoindex.functions import SplitRecursively

   chunks = SplitRecursively(
       language="python",
       chunk_size=1000,
       chunk_overlap=200  # Preserve context across boundaries
   )
   ```

3. **Embedding Optimization:**
   - Use smaller models for non-critical search
   - Batch embed operations
   - Cache frequently accessed embeddings

4. **Database Tuning:**
   ```sql
   -- Index vector columns for fast search
   CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops);

   -- Partition large tables by date
   CREATE TABLE events PARTITION BY RANGE (created_at);
   ```

### Development Workflow

1. **Local-First Development:**
   - Use M4 Max with llama.cpp for testing
   - Point LiteLLM to local models (config.dev.yaml)
   - Only use cloud APIs for final validation

2. **Testing Strategy:**
   ```python
   # Unit tests for schema validation
   def test_document_chunk_schema():
       chunk = DocumentChunk(
           id="test-123",
           content="test content",
           embedding=[0.1] * 384
       )
       assert chunk.dict() == DocumentChunkSchema.parse(chunk.dict())

   # Integration tests with mock LLM
   @pytest.fixture
   def mock_llm():
       return MockLLM(responses={"summary": "test summary"})

   def test_agent_workflow(mock_llm):
       agent = Agent(model=mock_llm)
       result = agent.run("test query")
       assert result.summary == "test summary"
   ```

3. **CI/CD Pipeline:**
   ```yaml
   # .github/workflows/test.yml
   name: Test AI Pipeline

   on: [push, pull_request]

   jobs:
     test:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v2

         - name: Generate BAML schemas
           run: baml-cli generate

         - name: Run Python tests
           run: |
             pytest tests/python/

         - name: Run TypeScript tests
           run: |
             npm test

         - name: Validate schemas match
           run: |
             python scripts/validate_schemas.py
   ```

4. **Monitoring in Production:**
   - Set up Grafana dashboards for LiteLLM metrics
   - Alert on high error rates or costs
   - Track P95 latency for agent workflows

### Documentation Standards

1. **Schema Documentation:**
   ```baml
   // Always include descriptions
   class DocumentChunk {
     id string @description("Unique identifier (UUID format)")
     content string @description("Extracted text content, max 10000 chars")
     embedding float[] @description("384-dim vector from all-MiniLM-L6-v2")
   }
   ```

2. **Agent Instructions:**
   ```python
   # Clear, numbered instructions
   agent = Agent(
       name="Summarizer",
       instructions=[
           "1. Read the entire document",
           "2. Identify the main topic and subtopics",
           "3. Extract key findings (max 5)",
           "4. Write a 2-3 sentence summary",
           "5. Format output as JSON matching SummarySchema"
       ]
   )
   ```

3. **Pipeline Documentation:**
   - Diagram data flows with Mermaid
   - Document expected schemas at each stage
   - Include example inputs/outputs

---

## Conclusion

This consolidated guide provides a comprehensive blueprint for building production AI/ML systems with:

- **Cost-effective compute allocation** through tiered model usage
- **Robust multi-agent orchestration** with Agno AgentOS
- **Type-safe, schema-driven development** using BAML, Pydantic, and Zod
- **Flexible knowledge management** combining Cognee and CocoIndex
- **Advanced document processing** with Docling, Qwen-VL, and Unstract
- **Centralized LLM infrastructure** via LiteLLM gateway
- **Hybrid vector + graph storage** for semantic search and knowledge graphs

By following these patterns and best practices, teams can build reliable, scalable, and maintainable AI systems that optimize for both performance and cost.

---

## References

### Core Technologies

- **Agno AgentOS:** https://github.com/agno-agi/agno
- **BAML:** https://github.com/BoundaryML/baml
- **LiteLLM:** https://docs.litellm.ai/
- **DLT:** https://dlthub.com/docs/
- **CocoIndex:** https://cocoindex.io/
- **Cognee:** https://www.cognee.ai/
- **Docling:** https://www.docling.ai/
- **Crawl4AI:** https://github.com/unclecode/crawl4ai
- **Repomix:** https://github.com/yamadashy/repomix
- **Dagster:** https://dagster.io/
- **RisingWave:** https://risingwave.com/
- **LanceDB:** https://lancedb.com/
- **Memgraph:** https://memgraph.com/
- **Pangolin:** https://docs.pangolin.net/

### Key Concepts

- **Tiered AI Compute:** Plan-and-Act architecture for cost optimization
- **Multi-Agent Systems:** Coordinated teams for complex workflows
- **Structured Outputs:** Schema-enforced LLM responses
- **Incremental Processing:** State-tracked updates for efficiency
- **Hybrid Retrieval:** Vector + graph for comprehensive search
- **Unified Gateway:** Single LLM proxy for observability
- **End-to-End Type Safety:** Consistent schemas across stack

---

**Document Status:** Complete
**Next Steps:** Implement specific workflows from Section 9 based on project needs
