# Building Production Multi-Agent Systems: Complete Implementation Guide

**The modern AI agent stack now has standardized layers.** Agno provides high-performance agent orchestration (529× faster than LangGraph), Cognee with Memgraph enables hybrid knowledge graphs with vector+graph search, Model Context Protocol (MCP) standardizes tool integration across providers, and LiteLLM routes requests to 100+ LLM providers with caching and rate limiting—all orchestrated through Docker Compose. As of November 2025, these components represent production-ready patterns adopted by Block, Apollo, OpenAI, and Google DeepMind, enabling teams to build context-aware agents that scale.

This matters because **fragmented integrations have historically blocked AI adoption**. Before these standards emerged, teams built N×M custom connectors between agents, knowledge bases, tools, and LLMs. MCP alone has eliminated this overhead by providing USB-C-like standardization. Meanwhile, Agno's pure Python workflows and minimal memory footprint (6.6 KiB per agent) make it practical to deploy hundreds of agents concurrently, while Cognee's ECL pipeline automatically transforms unstructured text into queryable knowledge graphs. These aren't experimental tools—they're battle-tested frameworks with active communities and enterprise deployments.

The technical landscape shifted in late 2024 when Anthropic open-sourced MCP, followed by OpenAI's adoption in March 2025 and Google DeepMind's commitment in April 2025. This convergence created an ecosystem where agents, knowledge systems, and tools interoperate seamlessly. For teams building multi-agent systems today, understanding how Agno agents query Cognee knowledge graphs via MCP tools while LiteLLM optimizes model routing has become essential architecture knowledge.

## Agno and AgentOS: orchestration with minimal overhead

Agno rejects DSLs in favor of pure Python, achieving **instantiation 529× faster than LangGraph** with 50× lower memory footprint. The framework supports three team coordination modes (coordinate, collaborate, route), Pydantic-based structured outputs, and persistent memory across SQLite, PostgreSQL, MongoDB, or Redis backends. AgentOS provides production runtime as a FastAPI application.

### Team coordination patterns that scale

**Coordinate mode** enables sequential or parallel task delegation. A team leader orchestrates specialists, synthesizes outputs, and maintains context across interactions. This pattern suits complex workflows requiring hierarchical decision-making:

```python
from agno.agent import Agent
from agno.team import Team
from agno.models.openai import OpenAIChat
from agno.tools.duckduckgo import DuckDuckGoTools

web_agent = Agent(
    name="Web Agent",
    role="Search the web for information",
    model=OpenAIChat(id="gpt-4o"),
    tools=[DuckDuckGoTools()],
    instructions="Always include sources"
)

finance_agent = Agent(
    name="Finance Agent",
    role="Get financial data",
    model=OpenAIChat(id="gpt-4o"),
    tools=[YFinanceTools(stock_price=True)],
    instructions="Use tables to display data"
)

team = Team(
    name="Research Team",
    mode="coordinate",  # Leader delegates tasks
    model=OpenAIChat(id="gpt-4o"),
    members=[web_agent, finance_agent],
    enable_team_history=True,
    enable_agentic_context=True,
    share_member_interactions=True
)
```

**Collaborate mode** runs all team members concurrently on identical tasks, then synthesizes responses. This maximizes perspective diversity for analysis:

```python
team = Team(
    mode="collaborate",  # All members process simultaneously
    members=[analyst1, analyst2, analyst3],
    success_criteria="Query answered with consensus"
)
```

**Route mode** delegates to the most appropriate specialist without leader interpretation, minimizing latency for straightforward queries.

### Memory architecture enabling continuity

Agno implements three memory types: **session memory** (chat history), **user memory** (personalization), and **team memory** (shared context). Critical distinction: Kuzu graph storage uses file-based locking and fails under concurrent access—use Neo4j, Memgraph, or FalkorDB for multi-agent deployments.

```python
from agno.memory.v2.db.postgres import PostgresMemoryDb
from agno.memory.v2.memory import Memory

memory_db = PostgresMemoryDb(
    table_name="agent_memory",
    db_url="postgresql://user:pass@localhost:5432/agno"
)
memory = Memory(db=memory_db)

agent = Agent(
    model=OpenAIChat(id="gpt-4o"),
    memory=memory,
    enable_agentic_memory=True,  # Auto-extracts memories
    add_history_to_messages=True,
    num_history_responses=10
)

# Agent remembers user preferences across sessions
agent.print_response(
    "What are my hobbies?",
    user_id="user_123"
)
```

### Structured outputs with Pydantic (BAML optional)

**Clarification**: Agno uses Pydantic `response_model` for structured outputs. BAML is a separate DSL from BoundaryML that **generates** Pydantic models. The workflow is:

1. **Without BAML**: Define Pydantic models directly in Python
2. **With BAML**: Define schemas in `.baml` files → BAML compiler generates Pydantic models → Agno uses generated models

For simple cases, define Pydantic models directly:

```python
from pydantic import BaseModel, Field

class ResearchReport(BaseModel):
    summary: str = Field(..., description="Executive summary")
    findings: list[str] = Field(..., description="Key findings")
    confidence: float = Field(..., ge=0, le=1)

agent = Agent(
    model=OpenAIChat(id="gpt-4o"),
    response_model=ResearchReport  # Structured outputs enabled
)

response = agent.run("Analyze market trends")
print(f"Confidence: {response.content.confidence}")
```

### Workflow patterns replacing DSLs

Agno workflows use standard Python control flow—no YAML, no custom syntax. Workflows are **deterministic programs** with full Python expressiveness:

```python
from agno.workflow import Workflow
from agno.agent import Agent

class ResearchWorkflow(Workflow):
    researcher = Agent(name="Researcher", model=OpenAIChat(id="gpt-4o"))
    writer = Agent(name="Writer", model=OpenAIChat(id="gpt-4o"))
    
    def run(self, topic: str):
        # Check cache
        if self.session_state.get(f"article_{topic}"):
            yield RunResponse(content=self.session_state[f"article_{topic}"])
            return
        
        # Research step
        yield from self.researcher.run(f"Research {topic}")
        research = self.researcher.run_response.content
        
        # Writing step
        yield from self.writer.run(f"Write article: {research}")
        article = self.writer.run_response.content
        
        # Cache result
        self.session_state[f"article_{topic}"] = article
```

Workflows support parallel execution via ThreadPoolExecutor, error handling with retry logic, and arbitrary Python control flow. Use workflows when order matters; use teams when scope exceeds single agents.

### Production deployment with AgentOS

```python
from agno.os import AgentOS
from agno.storage.agent.postgres import PostgresAgentStorage

storage = PostgresAgentStorage(
    table_name="prod_agents",
    db_url=os.getenv("DATABASE_URL")
)

agent = Agent(
    name="Production Agent",
    model=OpenAIChat(id="gpt-4o"),
    storage=storage,
    show_tool_calls=True,
    debug_mode=False
)

agent_os = AgentOS(
    description="Production System",
    agents=[agent],
    teams=[analysis_team]
)

app = agent_os.get_app()

# Production server
agent_os.serve(
    app="system:app",
    host="0.0.0.0",
    port=8000,
    reload=False
)
```

**Production checklist**: Use PostgreSQL storage (not SQLite), implement monitoring via AgentOps, set `num_history_responses` limits, use `--num_workers $(nproc)`, avoid in-memory state, deploy behind load balancer.

## Cognee with Memgraph: knowledge graphs at scale

Cognee transforms unstructured text into semantically searchable knowledge graphs through an **ECL (Extract, Cognify, Load)** pipeline. Integration with Memgraph's in-memory graph database enables hybrid vector+graph search with sub-millisecond queries and multi-hop traversals across millions of nodes.

### Setup architecture for production

**Core dependencies**:
```bash
pip install cognee neo4j  # Neo4j driver works with Memgraph
```

**Environment configuration**:
```env
# LLM for entity extraction
LLM_API_KEY=sk-xxx
LLM_MODEL=openai/gpt-4o-mini
LLM_PROVIDER=openai

# Embedding model
EMBEDDING_PROVIDER=openai
EMBEDDING_MODEL=openai/text-embedding-3-large

# Memgraph connection
GRAPH_DATABASE_PROVIDER=memgraph
GRAPH_DATABASE_URL=bolt://localhost:7687
GRAPH_DATABASE_USERNAME=""
GRAPH_DATABASE_PASSWORD=""
```

**Memgraph with vector search enabled**:
```bash
docker run -p 7687:7687 -p 7444:7444 \
  memgraph/memgraph-mage:latest \
  --schema-info-enabled=True
```

### The cognify operation pipeline

`.cognify()` executes six ordered tasks transforming text into queryable graphs:

1. **Classify documents**: Wrap files as Document objects with metadata
2. **Check permissions**: Enforce dataset access rights
3. **Extract chunks**: Split documents into manageable pieces
4. **Extract graph**: LLMs identify entities and relationships
5. **Summarize text**: Generate summaries for semantic search
6. **Add data points**: Embed nodes, write to vector store and graph DB

```python
import cognee
import asyncio

async def build_knowledge_graph():
    text = """Natural language processing (NLP) is a subfield of 
    computer science focused on interaction between computers and human language."""
    
    # Add data
    await cognee.add(text, dataset_name="nlp_knowledge")
    
    # Execute cognify pipeline
    await cognee.cognify()
    
    # Search with hybrid retrieval
    results = await cognee.search(
        query_text="What is NLP?",
        search_type="GRAPH_COMPLETION"  # Vector + graph traversal
    )
    
    for result in results:
        print(result)

asyncio.run(build_knowledge_graph())
```

### Custom DataPoints for domain modeling

Define Pydantic-based DataPoints for structured knowledge graphs:

```python
from cognee.low_level import DataPoint
from typing import List

class Person(DataPoint):
    name: str
    email: str
    metadata: dict = {"index_fields": ["name", "email"]}

class Department(DataPoint):
    name: str
    employees: List[Person]
    metadata: dict = {"index_fields": ["name"]}

class Company(DataPoint):
    name: str
    departments: List[Department]
    is_type: str = "Company"
    metadata: dict = {"index_fields": ["name"]}
```

These models automatically generate graph schemas in Memgraph with proper indexing and relationship types.

### Hybrid search: vector + graph traversal

**Search types in Cognee**:
- **INSIGHTS**: Pure vector-based semantic search
- **GRAPH_COMPLETION**: Hybrid vector + graph (recommended)
- **SUMMARIES**: Retrieves summarized chunks

The hybrid approach combines **vector similarity** (finding relevant starting nodes) with **graph traversal** (expanding context through relationships):

```cypher
-- Step 1: Vector search finds pivot nodes
CALL vector_search.search('entity_index', 5, $query_embedding)
YIELD node AS pivot

-- Step 2: Graph traversal expands context
MATCH (pivot)-[r*1..2]-(related)
RETURN pivot, r, related
```

**Vector index creation in Memgraph**:
```cypher
CREATE VECTOR INDEX ON :Document(embedding);
```

**Performance configuration**:
```bash
docker run memgraph/memgraph:latest \
  --experimental-enabled=vector-search \
  --experimental-config='{
    "vector-search": {
      "documents_index": {
        "label": "Document",
        "property": "embedding",
        "dimension": 1536,
        "capacity": 10000,
        "metric": "cos"
      }
    }
  }'
```

### Graph schema design best practices

**Indexing strategies** (prioritize high-cardinality properties):
```cypher
-- Label index for all Person nodes
CREATE INDEX ON :Person;

-- Property index for specific lookups
CREATE INDEX ON :Person(email);

-- Vector index for semantic search
CREATE VECTOR INDEX ON :Document(embedding);
```

**Query optimization patterns**:
```cypher
-- GOOD: Inline filtering
MATCH (p:Person)-[:WORKS_AT]->(c:Company {country: 'USA'})
RETURN p, c;

-- BAD: Post-filtering (slower)
MATCH (p:Person)-[:WORKS_AT]->(c:Company)
WHERE c.country = 'USA'
RETURN p, c;
```

**Performance analysis**:
```cypher
PROFILE MATCH (n:Person {email: 'john@example.com'}) RETURN n;
```

Returns operator pipeline, hits per operator, and execution time breakdowns.

### Multi-agent integration patterns

**Critical**: Default Kuzu graph store is NOT suitable for concurrent access. Use Neo4j, Memgraph, or FalkorDB for multi-agent deployments:

```env
# For single-agent development
GRAPH_DATABASE=networkx
VECTOR_DATABASE=lancedb

# For multi-agent production
GRAPH_DATABASE=memgraph
VECTOR_DATABASE=qdrant
RELATIONAL_DATABASE=postgresql://rds-endpoint
```

**Session isolation**:
```python
# Per-user memory isolation
await cognee.add(docs, user_id="user_123")
results = await cognee.search(query, user_id="user_123")

# Dataset-based isolation
await cognee.add(docs, dataset_name="project_alpha")
```

## Model Context Protocol: standardizing tool integration

MCP is an **open protocol standardizing AI-to-tool integration**, eliminating N×M custom connectors. Announced by Anthropic in November 2024, adopted by OpenAI (March 2025) and Google DeepMind (April 2025), MCP defines how applications provide context to LLMs through Resources (data), Tools (functions), and Prompts (templates).

Think of MCP as **USB-C for AI applications**—a universal connector replacing fragmented integrations. Built on JSON-RPC 2.0, MCP supports stdio (local) and HTTP/SSE (remote) transports.

### Architecture and components

**MCP architecture** follows client-host-server pattern:
- **MCP Host**: Environment where LLM runs (Claude Desktop, IDEs, custom apps)
- **MCP Client**: Application sending requests to MCP servers
- **MCP Server**: Exposes tools, resources, and prompts via standardized interface

**Communication flow**:
1. Client connects to MCP server
2. Server responds with capabilities (tools list)
3. Client sends query to LLM with tool information
4. LLM requests tool execution
5. Client forwards tool calls to server
6. Server executes tools, returns results
7. Client provides results back to LLM

### Building MCP servers with FastMCP

The **official Python SDK** uses FastMCP for rapid server development:

```python
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("Weather Server")

@mcp.tool()
async def get_weather(city: str) -> dict:
    """Get current weather for a city"""
    # FastMCP uses docstrings for tool descriptions
    return {
        "city": city,
        "temperature": 72,
        "conditions": "sunny"
    }

@mcp.resource("file://data/{filename}")
async def read_data(filename: str) -> str:
    """Expose data files as resources"""
    with open(f"data/{filename}") as f:
        return f.read()

if __name__ == "__main__":
    mcp.run(transport="stdio")
```

**Key features**:
- **Type hints and docstrings** automatically generate tool definitions
- **@mcp.tool()** decorator exposes functions as tools
- **@mcp.resource()** exposes data as readable resources
- **Context object** provides progress tracking and logging

### Integration with agent frameworks

**Agno/AgentOS integration**:
```python
from agno.agent import Agent
from agno.tools.mcp import MCPTools

agent = Agent(
    name="MCP-enabled Agent",
    model=OpenAIChat(id="gpt-4o"),
    tools=[MCPTools(
        transport="streamable-http",
        url="http://mcp-gateway:9000/mcp"
    )]
)
```

**OpenAI Agents SDK integration**:
```python
from agents import Agent, Runner
from agents.mcp import MCPServerStreamableHttp

async with MCPServerStreamableHttp(
    name="Production MCP Server",
    params={
        "url": "http://localhost:8000/mcp",
        "headers": {"Authorization": f"Bearer {token}"},
        "timeout": 10
    },
    cache_tools_list=True,
    max_retry_attempts=3
) as server:
    agent = Agent(
        name="Assistant",
        instructions="Use MCP tools for data access",
        mcp_servers=[server]
    )
    result = await Runner.run(agent, "Fetch latest metrics")
```

**LangChain/LangGraph integration**:
```python
from langchain_cognee import get_cognee_tools

session_id = "user_123"
cognee_tools = get_cognee_tools(session_id=session_id)

agent = create_react_agent(
    llm,
    tools=cognee_tools,
    prompt="You have persistent memory via Cognee MCP tools"
)
```

### Transport layers and deployment

**Stdio (local development)**:
```python
from mcp.client.stdio import stdio_client
from mcp import ClientSession, StdioServerParameters

server_params = StdioServerParameters(
    command="uv",
    args=["run", "server.py"],
    env={"API_KEY": os.getenv("API_KEY")}
)

async with stdio_client(server_params) as (stdio, write):
    async with ClientSession(stdio, write) as session:
        await session.initialize()
        tools = await session.list_tools()
```

**Streamable HTTP (production)**:
```python
# Server side
from mcp.server.fastmcp import FastMCP
app = FastMCP("Production Server")

# Deploy as HTTP endpoint
uvicorn.run(app, host="0.0.0.0", port=8000)

# Client side
session = MCPServerStreamableHttp(
    params={"url": "https://mcp-server.example.com/mcp"}
)
```

### Security considerations

**Access control patterns**:
```python
@mcp.tool()
async def sensitive_operation(ctx: Context, action: str):
    # Check authorization
    if not ctx.user.has_permission("admin"):
        raise PermissionError("Unauthorized")
    
    # Audit logging
    ctx.info(f"User {ctx.user.id} executed {action}")
    
    return execute_action(action)
```

**Authentication risks** (April 2025 security analysis identified):
- Prompt injection vulnerabilities
- Over-broad permission scopes
- Tool combination attacks enabling file exfiltration
- Lookalike tools replacing trusted ones

**Mitigation strategies**:
- Implement fine-grained permissions per tool
- Use OAuth 2.1 resource server patterns
- Validate all inputs against schemas
- Audit tool execution logs
- Require approval workflows for sensitive operations

## LiteLLM: unified model routing and optimization

LiteLLM provides a **production-ready gateway** to 100+ LLM providers through OpenAI-compatible API. Features include load balancing, automatic fallbacks, multi-level rate limiting, semantic caching, cost tracking, and comprehensive monitoring. As of November 2025, supports Gemini 2.5 Pro with reasoning tokens, 6.5× faster completion via fastuuid, and improved multi-instance rate limiting.

### Gemini 2.5 Pro configuration

**Google AI Studio route** (simpler for development):
```python
from litellm import completion
import os

os.environ['GEMINI_API_KEY'] = "your-api-key"

response = completion(
    model="gemini/gemini-2.5-pro",
    messages=[{"role": "user", "content": "Explain quantum computing"}]
)
```

**Vertex AI route** (preferred for production):
```python
response = completion(
    model="vertex_ai/gemini-2.5-pro",
    messages=[{"role": "user", "content": "Analyze this data"}],
    vertex_credentials=vertex_credentials_json,
    vertex_project="my-gcp-project",
    vertex_location="us-central1"
)
```

**Multi-provider configuration**:
```yaml
model_list:
  - model_name: gemini-2.5-pro
    litellm_params:
      model: gemini/gemini-2.5-pro
      api_key: os.environ/GEMINI_API_KEY
      rpm: 600  # Rate limit per deployment
      
  - model_name: gemini-2.5-pro
    litellm_params:
      model: vertex_ai/gemini-2.5-pro
      vertex_project: project-id
      vertex_location: us-central1
      rpm: 1000
      
  - model_name: gpt-4o
    litellm_params:
      model: openai/gpt-4o
      api_key: os.environ/OPENAI_API_KEY
      rpm: 3500
```

**Gemini-specific features**:
- JSON Schema validation with `response_format`
- Context caching with custom TTL (7200s)
- Google Search, URL Context, Code Execution tools
- Thinking/reasoning mode with budget tokens
- TTS capabilities with pcm16 format

### Proxy server deployment

**Quick start**:
```bash
pip install 'litellm[proxy]'
litellm --config /path/to/config.yaml --port 4000
```

**Production configuration**:
```yaml
model_list:
  - model_name: gpt-3.5-turbo
    litellm_params:
      model: azure/gpt-3.5-turbo
      api_base: os.environ/AZURE_API_BASE
      api_key: os.environ/AZURE_API_KEY
      rpm: 600
      
router_settings:
  routing_strategy: usage-based-routing-v2  # Recommended
  redis_host: os.environ/REDIS_HOST
  redis_port: os.environ/REDIS_PORT
  redis_password: os.environ/REDIS_PASSWORD
  num_retries: 2
  timeout: 30
  fallbacks:
    - {"gemini/gemini-2.5-pro": ["openai/gpt-4o"]}
    
litellm_settings:
  cache: True
  cache_params:
    type: redis
    host: os.environ/REDIS_HOST
    port: os.environ/REDIS_PORT
    password: os.environ/REDIS_PASSWORD
    ttl: 600
  success_callback: ["prometheus", "langfuse"]
    
general_settings:
  master_key: sk-1234
  database_url: "postgresql://user:pass@host:5432/litellm"
```

**Docker deployment**:
```bash
docker run \
  -v $(pwd)/litellm-config.yaml:/app/config.yaml \
  -e LITELLM_MASTER_KEY=sk-1234 \
  -e DATABASE_URL=postgresql://... \
  -p 4000:4000 \
  ghcr.io/berriai/litellm:main-stable \
  --config /app/config.yaml --num_workers 4
```

### Rate limiting strategies

**Multi-level rate limiting** (deployment, key, user, team):

```yaml
# Deployment-level
model_list:
  - model_name: gpt-4
    litellm_params:
      model: openai/gpt-4
      rpm: 3500  # Requests per minute
      tpm: 500000  # Tokens per minute
```

```bash
# Key-level
curl -X POST 'http://localhost:4000/key/generate' \
  -H 'Authorization: Bearer sk-1234' \
  -d '{
    "max_parallel_requests": 10,
    "tpm_limit": 20000,
    "rpm_limit": 100,
    "duration": "30d"
  }'

# User-level
curl -X POST 'http://localhost:4000/user/new' \
  -H 'Authorization: Bearer sk-1234' \
  -d '{
    "user_id": "engineer@company.com",
    "tpm_limit": 50000,
    "rpm_limit": 200
  }'
```

**Multi-instance rate limiting** (experimental, 2× faster):
```bash
export EXPERIMENTAL_MULTI_INSTANCE_RATE_LIMITING="True"
```

Syncs in-memory cache with Redis every 0.01s with max 10 request drift at 100 RPS across 3 instances.

### Caching strategies for cost optimization

**Redis caching** (production standard):
```yaml
litellm_settings:
  cache: True
  cache_params:
    type: redis
    host: os.environ/REDIS_HOST
    port: os.environ/REDIS_PORT
    password: os.environ/REDIS_PASSWORD
    ttl: 600
    namespace: "litellm_caching"
```

**Semantic caching** (finds similar queries):
```python
from litellm.caching.caching import Cache

litellm.cache = Cache(
    type="redis-semantic",
    host=os.environ["REDIS_HOST"],
    port=os.environ["REDIS_PORT"],
    password=os.environ["REDIS_PASSWORD"],
    similarity_threshold=0.8,  # 0-1 scale
    ttl=120,
    redis_semantic_cache_embedding_model="text-embedding-ada-002"
)
```

**Context caching** (for long prompts):
```python
response = completion(
    model="gemini/gemini-1.5-pro",
    messages=[{
        "role": "system",
        "content": [{
            "type": "text",
            "text": "Long document..." * 4000,
            "cache_control": {
                "type": "ephemeral",
                "ttl": "7200s"  # 2 hours
            }
        }]
    }]
)
```

**Caching best practices**: Use Redis for multi-instance deployments, set appropriate TTLs based on data freshness needs, combine semantic + context caching, monitor cache hit rates via logging.

### Load balancing and high availability

**Routing strategies**:
- **usage-based-routing-v2**: Recommended for production (fastest, most efficient)
- **least-busy**: Routes to endpoint with fewest active requests
- **latency-based-routing**: Routes to fastest endpoint
- **simple-shuffle**: Random distribution

**High availability configuration**:
```yaml
model_list:
  # Primary: Anthropic direct
  - model_name: sonnet-4
    litellm_params:
      model: anthropic/claude-sonnet-4-20250514
      api_key: os.environ/ANTHROPIC_API_KEY
      
  # Secondary: Vertex AI
  - model_name: sonnet-4
    litellm_params:
      model: vertex_ai/claude-sonnet-4-20250514
      vertex_project: my-project
      
  # Tertiary: Bedrock
  - model_name: sonnet-4
    litellm_params:
      model: bedrock/anthropic.claude-3-5-sonnet-20241022-v2:0

router_settings:
  routing_strategy: latency-based-routing
  num_retries: 2
  fallbacks:
    - {"sonnet-4": ["gpt-4o", "gemini-2.5-pro"]}
```

### Cost tracking and monitoring

**Automatic cost tracking**:
```python
response = completion(
    model="gpt-4",
    messages=[{"role": "user", "content": "Hello"}],
    metadata={
        "team": "engineering",
        "project": "chatbot",
        "environment": "production"
    }
)

print(f"Cost: ${response.cost:.4f}")
print(f"Tokens: {response.usage.total_tokens}")
```

**Prometheus integration**:
```yaml
litellm_settings:
  success_callback: ["prometheus"]
```

Access metrics at `http://localhost:4000/metrics`:
- `litellm_requests_total`
- `litellm_request_duration_seconds`
- `litellm_tokens_total`
- `litellm_spend_total`

**Database logging** (PostgreSQL required):
```yaml
general_settings:
  database_url: "postgresql://user:pass@host:5432/litellm"
```

Enables spend tracking endpoints, user activity dashboards, and historical cost analysis.

### Agent framework integration

**LangChain**:
```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="gpt-4",
    openai_api_key="sk-litellm-key",
    openai_api_base="http://localhost:4000"
)
```

**LlamaIndex**:
```python
from llama_index.llms import OpenAI

llm = OpenAI(
    model="gpt-4",
    api_key="sk-litellm-key",
    api_base="http://localhost:4000"
)
```

**Agno**:
```python
from agno.models.openai import OpenAIChat

model = OpenAIChat(
    id="gpt-4",
    api_key="sk-litellm-key",
    base_url="http://localhost:4000"
)
```

## Docker Compose: orchestrating the complete stack

Docker Compose orchestrates LanceDB (embedded), PostgreSQL (Cognee), Memgraph (graph DB), Redis (caching/memory), and LiteLLM proxy into a production-ready multi-agent platform. Critical insights: LanceDB runs embedded (no container needed), Memgraph requires schema-info enabled, Redis needs memory policies configured, and all services require health checks.

### Complete stack configuration

```yaml
version: "3.9"

services:
  # Vector storage (S3-compatible backend)
  localstack:
    image: localstack/localstack:4.0
    ports: ["4566:4566"]
    environment: [SERVICES=s3]
    healthcheck:
      test: ["CMD", "curl", "-s", "http://localhost:4566/_localstack/health"]
      interval: 5s
      retries: 3
    networks: [agent-network]

  # Graph database
  memgraph:
    image: memgraph/memgraph-mage:latest
    ports: ["7687:7687", "7444:7444"]
    command: ["--schema-info-enabled=True", "--log-level=INFO"]
    volumes: [memgraph_data:/var/lib/memgraph]
    healthcheck:
      test: ["CMD-SHELL", "echo 'RETURN 0;' | mgconsole || exit 1"]
      interval: 10s
      retries: 3
    networks: [agent-network]

  # Memgraph visualization
  memgraph-lab:
    image: memgraph/lab:latest
    ports: ["3000:3000"]
    depends_on:
      memgraph: {condition: service_healthy}
    environment: [QUICK_CONNECT_MG_HOST=memgraph]
    networks: [agent-network]

  # Relational database (Cognee + LiteLLM)
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: cognee
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: cognee_db
    volumes: [postgres_data:/var/lib/postgresql/data]
    command:
      - -c shared_buffers=256MB
      - -c effective_cache_size=1GB
      - -c max_connections=200
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "cognee"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks: [agent-network]

  # Cache and agent memory
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: >
      redis-server
      --save 20 1
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 2gb
      --maxmemory-policy allkeys-lru
      --appendonly yes
      --tcp-backlog 511
      --tcp-keepalive 300
    volumes: [redis_data:/data]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
    deploy:
      resources:
        limits: {memory: 2G}
    networks: [agent-network]

  # LLM gateway
  litellm:
    image: ghcr.io/berriai/litellm-database:main-stable
    ports: ["4000:4000"]
    volumes: [./litellm-config.yaml:/app/config.yaml:ro]
    command: ["--port", "4000", "--config", "/app/config.yaml", "--num_workers", "4"]
    environment:
      - LITELLM_MASTER_KEY=${LITELLM_MASTER_KEY}
      - DATABASE_URL=postgresql://cognee:${POSTGRES_PASSWORD}@postgres:5432/litellm
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    depends_on:
      postgres: {condition: service_healthy}
      redis: {condition: service_healthy}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4000/health/liveliness"]
      interval: 15s
      start_period: 120s
    deploy:
      replicas: 2
      resources:
        limits: {memory: 2g}
    networks: [agent-network]

  # Knowledge graph service
  cognee:
    build: ./cognee
    environment:
      - DB_PROVIDER=postgres
      - DB_HOST=postgres
      - DB_NAME=cognee_db
      - DB_USERNAME=cognee
      - DB_PASSWORD=${POSTGRES_PASSWORD}
      - GRAPH_DATABASE_PROVIDER=memgraph
      - GRAPH_DATABASE_URL=bolt://memgraph:7687
      - REDIS_URL=redis://:${REDIS_PASSWORD}@redis:6379
      - LLM_API_KEY=${OPENAI_API_KEY}
    depends_on:
      postgres: {condition: service_healthy}
      memgraph: {condition: service_healthy}
    networks: [agent-network]

  # Agent orchestration
  agno-agent:
    build: ./agno
    environment:
      - LITELLM_API_BASE=http://litellm:4000
      - LITELLM_API_KEY=${LITELLM_MASTER_KEY}
      - MEMGRAPH_HOST=memgraph
      - COGNEE_API_BASE=http://cognee:8000
      - POSTGRES_URL=postgresql://cognee:${POSTGRES_PASSWORD}@postgres:5432/agno
      - REDIS_HOST=redis
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    depends_on:
      litellm: {condition: service_healthy}
    networks: [agent-network]

volumes:
  memgraph_data:
  postgres_data:
  redis_data:

networks:
  agent-network:
    driver: bridge
```

### Service networking patterns

**DNS resolution**: Services communicate via service names (e.g., `postgres:5432`, `memgraph:7687`). Docker automatically provides DNS resolution within networks.

**Network isolation**: Use internal networks for databases:
```yaml
networks:
  frontend: {driver: bridge}
  backend:
    driver: bridge
    internal: true  # No external access
```

**Cross-project communication**:
```yaml
# Project 1: Create shared network
networks:
  shared-net:
    name: multi-agent-network
    external: false

# Project 2: Reuse shared network
networks:
  shared-net:
    name: multi-agent-network
    external: true
```

### Volume management for persistence

**Named volumes** (production):
```yaml
volumes:
  postgres_data: {driver: local}
  redis_data: {driver: local}
  memgraph_data: {driver: local}
```

**Bind mounts** (development):
```yaml
services:
  app:
    volumes:
      - ./config:/app/config:ro  # Read-only
      - ./data:/app/data:rw      # Read-write
```

**Backup strategy**:
```yaml
services:
  backup:
    image: prodrigestivill/postgres-backup-local:latest
    environment:
      - POSTGRES_HOST=postgres
      - SCHEDULE=@daily
      - BACKUP_KEEP_DAYS=7
    volumes: [./backups:/backups]
```

### Environment configuration

```env
# LLM providers
OPENAI_API_KEY=sk-xxx
ANTHROPIC_API_KEY=sk-ant-xxx
GEMINI_API_KEY=xxx

# Database
POSTGRES_PASSWORD=secure_pass
POSTGRES_USER=cognee
POSTGRES_DB=cognee_db

# Redis
REDIS_PASSWORD=redis_pass

# LiteLLM
LITELLM_MASTER_KEY=sk-1234
LITELLM_SALT_KEY=salt-abc123

# Graph
GRAPH_DATABASE_URL=bolt://memgraph:7687
```

### Health checks for reliability

**PostgreSQL**:
```yaml
healthcheck:
  test: ["CMD", "pg_isready", "-U", "cognee"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Redis**:
```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 30s
```

**Memgraph**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "echo 'RETURN 0;' | mgconsole || exit 1"]
  interval: 10s
  retries: 3
```

**HTTP services**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### Resource limits and scaling

```yaml
services:
  api:
    deploy:
      replicas: 3
      resources:
        limits: {cpus: '2.0', memory: 4G}
        reservations: {cpus: '1.0', memory: 2G}
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
      update_config:
        parallelism: 2
        delay: 10s
```

### Monitoring stack

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    volumes: [./prometheus.yml:/etc/prometheus/prometheus.yml]
    ports: ["9090:9090"]
    networks: [agent-network]

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
    depends_on: [prometheus]
    networks: [agent-network]
```

## Integration architecture: data flow patterns

Understanding how components interact is critical for debugging and optimization. The data flows through three primary pathways: **knowledge ingestion**, **query execution**, and **tool invocation**.

### Knowledge ingestion flow

**Path**: Raw data → Cognee → PostgreSQL + Memgraph + Vector Store

1. Application sends documents to Cognee via REST API (`POST /add`)
2. Cognee executes ECL pipeline:
   - Chunks documents into semantic units
   - LLM extracts entities and relationships
   - Generates embeddings via OpenAI/Gemini API
   - Stores metadata in PostgreSQL
   - Commits graph structure to Memgraph via Bolt protocol (`:7687`)
   - Writes vectors to LanceDB/Qdrant
3. Indexes automatically created in Memgraph for entity properties

**Code example**:
```python
import cognee
import asyncio

async def ingest_knowledge():
    # Add documents to Cognee
    await cognee.add([
        "docs/architecture.md",
        "docs/api-spec.yaml"
    ], dataset_name="engineering_docs")
    
    # Execute pipeline (stores to Postgres + Memgraph)
    await cognee.cognify()
    
    print("Knowledge graph built successfully")

asyncio.run(ingest_knowledge())
```

### Query execution flow

**Path**: Agno Agent → LiteLLM → Model Provider → Cognee → Memgraph

1. User query sent to Agno agent
2. Agent formats request, sends to LiteLLM proxy (`http://litellm:4000/chat/completions`)
3. LiteLLM:
   - Checks Redis cache for similar queries
   - Applies rate limiting (checks Redis counters)
   - Routes to optimal provider (Gemini/OpenAI/Claude)
   - Logs request to PostgreSQL for cost tracking
4. Model generates response, potentially requesting Cognee tool
5. Agent invokes Cognee search via MCP:
   - Vector search finds semantically similar nodes
   - Graph traversal expands context via relationships
   - Results merged and returned
6. Model incorporates knowledge graph context into final response
7. Agent returns to user with citations

**Code example**:
```python
from agno.agent import Agent
from agno.models.openai import OpenAIChat
import cognee

# Agent with LiteLLM and Cognee access
agent = Agent(
    name="Knowledge Agent",
    model=OpenAIChat(
        id="gemini-2.5-pro",  # Routed through LiteLLM
        api_key="sk-litellm-master-key",
        base_url="http://litellm:4000"
    ),
    instructions="Search knowledge graph for context"
)

async def query_with_context(query: str):
    # Search Cognee knowledge graph
    kg_results = await cognee.search(
        query_text=query,
        search_type="GRAPH_COMPLETION"
    )
    
    # Agent processes with context
    response = agent.run(
        f"Answer using this context: {kg_results}. Query: {query}"
    )
    return response
```

### Tool invocation flow

**Path**: Agent → MCP Client → MCP Server → External Tool → Response

1. Agent determines tool needed (via LLM reasoning)
2. Agent's MCP client sends tool request to MCP server
3. MCP server validates permissions, executes tool
4. Tool interacts with external system (GitHub API, database, etc.)
5. Results returned through MCP protocol
6. Agent processes tool output, continues reasoning
7. May trigger additional tool calls or return final answer

**Code example**:
```python
from mcp.server.fastmcp import FastMCP
import httpx

mcp = FastMCP("GitHub MCP Server")

@mcp.tool()
async def get_pr_details(repo: str, pr_number: int) -> dict:
    """Fetch pull request details from GitHub"""
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"https://api.github.com/repos/{repo}/pulls/{pr_number}",
            headers={"Authorization": f"token {os.getenv('GITHUB_TOKEN')}"}
        )
        return response.json()

# Agent uses MCP tool
from agno.agent import Agent
from agno.tools.mcp import MCPTools

agent = Agent(
    name="DevOps Agent",
    model=OpenAIChat(id="gpt-4o"),
    tools=[MCPTools(transport="stdio", command="python", args=["mcp_server.py"])]
)

agent.print_response("Summarize PR #123 in acme/api-gateway")
```

### Multi-agent coordination flow

**Path**: Team Leader → Member Agents → Parallel Execution → Synthesis

1. User query sent to Agno Team
2. Team leader (coordinator) analyzes query
3. Leader delegates subtasks to specialist agents in parallel
4. Each specialist:
   - Queries LiteLLM for model access
   - Searches Cognee for relevant knowledge
   - Invokes MCP tools as needed
   - Returns findings to leader
5. Leader synthesizes member responses
6. May trigger follow-up queries to members
7. Returns comprehensive answer to user

**Code example**:
```python
from agno.team import Team
from agno.agent import Agent
from agno.storage.agent.postgres import PostgresAgentStorage

storage = PostgresAgentStorage(
    db_url="postgresql://cognee:pass@postgres:5432/agno"
)

# Specialized agents
research_agent = Agent(name="Researcher", storage=storage, ...)
code_agent = Agent(name="Code Analyst", storage=storage, ...)
security_agent = Agent(name="Security Auditor", storage=storage, ...)

# Coordinated team
team = Team(
    name="DevSecOps Team",
    mode="coordinate",  # Leader orchestrates
    members=[research_agent, code_agent, security_agent],
    storage=storage,
    enable_team_history=True,
    share_member_interactions=True
)

# Parallel execution with synthesis
team.print_response(
    "Review this pull request for security, code quality, and best practices",
    session_id="pr_review_456"
)
```

## Production best practices and optimization

### Common pitfalls and solutions

**Pitfall 1**: Using Kuzu graph database with multiple agents
- **Problem**: File-based locking causes deadlocks under concurrent access
- **Solution**: Use Memgraph, Neo4j, or FalkorDB for multi-agent deployments

**Pitfall 2**: No cache warming before production
- **Problem**: First requests to LiteLLM take 30+ seconds
- **Solution**: Execute warmup requests during deployment, enable persistent Redis cache

**Pitfall 3**: Unbounded agent history
- **Problem**: Agent memory grows unbounded, causing OOM errors
- **Solution**: Set `num_history_responses=10` and implement periodic pruning

**Pitfall 4**: Hardcoded API credentials
- **Problem**: Security vulnerabilities, rotation nightmares
- **Solution**: Use environment variables, Docker secrets, or vault solutions

**Pitfall 5**: No rate limiting on MCP tools
- **Problem**: Agents overwhelm external APIs
- **Solution**: Implement rate limiting in MCP server, use approval workflows

**Pitfall 6**: Single LiteLLM worker
- **Problem**: Sequential request processing bottlenecks throughput
- **Solution**: Use `--num_workers $(nproc)` to match CPU cores

**Pitfall 7**: Missing health checks
- **Problem**: Docker Compose starts services before dependencies ready
- **Solution**: Implement health checks on all services with proper conditions

### Performance optimization strategies

**Database query optimization**:
```cypher
-- Enable query plan analysis
PROFILE MATCH (n:Person {email: 'user@example.com'}) RETURN n;

-- Optimize with indexes
CREATE INDEX ON :Person(email);

-- Inline filtering (faster)
MATCH (p:Person)-[:WORKS_AT]->(c:Company {country: 'USA'}) RETURN p;

-- Set query memory limits
SET GLOBAL QUERY EXECUTION MEMORY 8GB;
```

**Redis optimization**:
```yaml
redis:
  command: >
    redis-server
    --maxmemory 2gb
    --maxmemory-policy allkeys-lru
    --tcp-backlog 511
    --tcp-keepalive 300
    --save 900 1  # Snapshot every 15min if 1 key changed
```

**PostgreSQL tuning**:
```yaml
postgres:
  command:
    - -c shared_buffers=256MB
    - -c effective_cache_size=1GB
    - -c max_connections=200
    - -c work_mem=4MB
```

**LiteLLM caching**:
```python
# Combine semantic + context caching
litellm.cache = Cache(
    type="redis-semantic",
    similarity_threshold=0.85,  # Higher = fewer cache hits but more accurate
    ttl=600
)

# Context caching for long prompts
response = completion(
    model="gemini/gemini-1.5-pro",
    messages=[{
        "role": "system",
        "content": [{
            "type": "text",
            "text": long_system_prompt,
            "cache_control": {"type": "ephemeral", "ttl": "3600s"}
        }]
    }]
)
```

**Agno agent optimization**:
```python
agent = Agent(
    model=OpenAIChat(id="gpt-4o-mini"),  # Use smaller models when appropriate
    tools=[essential_tools_only],  # Limit toolset
    max_loops=5,  # Prevent infinite reasoning loops
    num_history_responses=10,  # Limit memory
    show_tool_calls=False  # Disable verbose logging in production
)
```

### Testing and debugging approaches

**Unit testing MCP servers**:
```python
import pytest
from mcp.server.fastmcp import FastMCP

@pytest.mark.asyncio
async def test_weather_tool():
    mcp = FastMCP("Test Server")
    
    @mcp.tool()
    async def get_weather(city: str) -> dict:
        return {"city": city, "temp": 72}
    
    result = await mcp.call_tool("get_weather", {"city": "Boston"})
    assert result["temp"] == 72
```

**Integration testing with MCP Inspector**:
```bash
# Start MCP inspector for interactive testing
mcp dev server.py --with pandas --with numpy

# Test tool execution, resource access, prompt rendering
```

**Load testing LiteLLM**:
```python
import asyncio
import httpx

async def load_test():
    async with httpx.AsyncClient() as client:
        tasks = [
            client.post(
                "http://litellm:4000/chat/completions",
                json={"model": "gpt-4o", "messages": [{"role": "user", "content": "test"}]},
                headers={"Authorization": "Bearer sk-1234"}
            )
            for _ in range(100)
        ]
        responses = await asyncio.gather(*tasks)
        print(f"Success rate: {sum(r.status_code == 200 for r in responses)/len(responses)}")

asyncio.run(load_test())
```

**Monitoring agent performance**:
```python
import agentops

agentops.init(api_key=os.getenv("AGENTOPS_API_KEY"))

agent = Agent(
    name="Monitored Agent",
    model=OpenAIChat(id="gpt-4o"),
    show_tool_calls=True  # Logs to AgentOps dashboard
)

# View metrics: latency, token usage, tool calls, error rates
```

**Debugging knowledge graph queries**:
```python
# Enable verbose logging
import logging
logging.basicConfig(level=logging.DEBUG)

# Test Memgraph connectivity
from neo4j import GraphDatabase

driver = GraphDatabase.driver("bolt://localhost:7687")
with driver.session() as session:
    result = session.run("MATCH (n) RETURN count(n)")
    print(f"Total nodes: {result.single()[0]}")
```

### Security hardening

**Docker secrets management**:
```yaml
services:
  api:
    secrets: [db_password, api_key]

secrets:
  db_password:
    file: ./secrets/db_password.txt
  api_key:
    external: true
```

**Network isolation**:
```yaml
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true  # Databases unreachable from internet
```

**MCP authorization**:
```python
from mcp.server.auth import require_permission

@mcp.tool()
@require_permission("admin")
async def delete_resource(resource_id: str):
    # Only admin role can execute
    return delete(resource_id)
```

**LiteLLM API key rotation**:
```bash
# Generate new key
curl -X POST 'http://localhost:4000/key/generate' \
  -H 'Authorization: Bearer sk-master' \
  -d '{"duration": "7d"}'

# Revoke compromised key
curl -X POST 'http://localhost:4000/key/delete' \
  -d '{"key": "sk-compromised"}'
```

## Conclusion and ecosystem trajectory

The convergence of Agno's performance optimizations, Cognee's knowledge graph automation, MCP's standardized tool integration, and LiteLLM's unified model routing represents a **maturation of the multi-agent ecosystem**. Teams can now assemble production systems from interoperable components rather than building custom infrastructure from scratch.

**Key architectural decisions** for new projects:
1. **Agent orchestration**: Choose Agno for pure Python workflows and minimal overhead. Avoid LangGraph if performance matters.
2. **Knowledge management**: Use Cognee with Memgraph for hybrid search. PostgreSQL for metadata, Memgraph for relationships, vector store for embeddings.
3. **Tool integration**: Adopt MCP servers for all external integrations. Build once, use across Claude, OpenAI Agents, LangChain, and custom frameworks.
4. **Model routing**: Deploy LiteLLM proxy for cost optimization, rate limiting, and provider flexibility. Never call model APIs directly from agents.
5. **Infrastructure**: Orchestrate with Docker Compose for development, migrate to Kubernetes for production scale.

**Emerging patterns** as of November 2025:
- **Hybrid reasoning**: Gemini 2.5 Pro's thinking tokens combined with knowledge graph context enables deeper analysis
- **Multi-hop graph queries**: Agents traversing 3-4 relationship hops in Memgraph for comprehensive context
- **Semantic caching**: 80%+ cache hit rates reducing costs by 70% in production deployments
- **MCP tool composition**: Agents chaining multiple MCP tools (GitHub → Jira → Slack) for autonomous workflows

**Avoiding premature optimization**: Start with SQLite and local LLMs for prototyping. Graduate to PostgreSQL + Redis + LiteLLM when concurrency demands arise. Add Memgraph when knowledge graphs provide value. Don't deploy MCP until tool reuse justifies protocol overhead.

The ecosystem's rapid maturation—MCP adopted by three major providers within six months, Agno achieving 50× efficiency gains, Cognee automating knowledge graph construction—signals that **multi-agent systems have transitioned from research to engineering**. Teams building AI agents today leverage standardized components, not experimental frameworks. This architecture guide provides the production patterns to deploy scalable, maintainable agent systems using the ecosystem's most mature tools.