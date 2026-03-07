# Langfuse LLM Observability Platform - Comprehensive Research

## Executive Summary

Langfuse is an open-source LLM engineering platform (YC W23) that provides comprehensive observability, tracing, and analytics for LLM-powered applications. It enables teams to debug, analyze, and iterate on LLM applications through detailed tracing, cost tracking, evaluation, and prompt management capabilities.

**Key Stats:**
- Open source on GitHub
- Available as Cloud (SaaS) or self-hosted
- Supports Python, JavaScript/TypeScript SDKs
- Integrates with OpenTelemetry, LangChain, LlamaIndex, LiteLLM, and 50+ other frameworks
- Pricing: Traces + Observations + Scores

---

## 1. CORE ARCHITECTURE AND CONCEPTS

### 1.1 What is Langfuse

Langfuse is a purpose-built LLM observability platform that captures:
- Complete execution traces of LLM interactions
- Input/output data for every step in an application
- Latency and cost metrics for each operation
- User interactions and sessions
- Evaluation scores and feedback

**Core Purpose:**
Enable teams to collaboratively monitor, debug, analyze, and iterate on LLM applications in production and development environments.

### 1.2 Core Entities: The Data Model

#### Traces
- **Definition**: A single execution of an LLM feature, from start to finish
- **Purpose**: Container for all operations occurring in a request
- **Characteristics:**
  - Usually corresponds to a single API call to an application
  - Contains overall input/output data
  - Holds metadata: session_id, user_id, tags
  - Shares same ID as OTel trace (OpenTelemetry compatibility)
  - Has timestamps and execution duration
- **Key Attributes:**
  - `trace_id`: Unique identifier
  - `name`: Human-readable name
  - `user_id`: Associated user
  - `session_id`: Grouping with other traces
  - `metadata`: Custom key-value pairs
  - `tags`: Array of categorization tags
  - `input`, `output`: Trace-level data
  - `duration`: Total execution time

#### Observations
- **Definition**: Individual execution steps within a trace
- **Purpose**: Record granular operations in a trace hierarchy
- **Types of Observations:**

  1. **Event** - Discrete tracking points (no duration)
  2. **Span** - Generic operation with duration/timing
  3. **Generation** - LLM call with model, prompts, completions, tokens, costs
  4. **Agent** - Flow decision-making component
  5. **Tool** - External API call (weather API, search, etc.)
  6. **Chain** - Links context between application steps
  7. **Retriever** - Data retrieval (vector store, database)
  8. **Embedding** - Vector generation with tokens/costs
  9. **Guardrail** - Content protection component
  10. **Evaluator** - Output assessment function

- **Key Attributes (shared):**
  - `observation_id`: Unique ID
  - `trace_id`: Parent trace
  - `parent_observation_id`: Nesting support
  - `start_time`, `end_time`: Duration tracking
  - `input`, `output`: Data for the operation
  - `metadata`: Custom attributes
  - `status`: success, error, etc.
  - `level`: log level classification

#### Generations (Specialized Span)
- **Definition**: Specialized observation for LLM calls
- **Unique Attributes:**
  - `model`: Model name (OpenAI, Claude, etc.)
  - `model_parameters`: Temperature, max_tokens, etc.
  - `usage_details`: Token counts (input, output, cached, reasoning, audio, etc.)
  - `cost_details`: Calculated costs by token type
  - `prompt`: The input prompt(s)
  - `completion`: The model response
  - `top_level_spans`: Can be root-level operations in traces
  - `finish_reason`: Model's completion reason
  - `temperature`, `max_tokens`, etc.: Model-specific parameters

### 1.3 Data Model Relationships

```
Trace (single request)
├── Observation (step)
│   ├── Event (discrete point)
│   ├── Span (operation)
│   │   └── Generation (LLM call)
│   │       ├── Prompt data
│   │       ├── Completion data
│   │       └── Token/cost metrics
│   ├── Tool
│   ├── Retriever
│   └── [other observation types]
├── Sessions (optional grouping across traces)
├── Scores (evaluations on trace/observation)
└── Metadata & Tags
```

**Key Features:**
- Observations nest hierarchically (parent-child relationships)
- Automatic nesting via OpenTelemetry context propagation
- Manual nesting by setting parent_observation_id
- Traces can be linked across distributed systems via trace IDs

### 1.4 Sessions and User Tracking

#### Sessions
- **Definition**: Logical grouping of traces and observations across multiple API calls
- **Use Cases:**
  - Multi-turn conversations (chatbot interactions)
  - Extended workflows spanning multiple traces
  - Session replay for user interactions
  - Batch processes with related operations
- **Implementation:**
  - `session_id`: US-ASCII string < 200 characters
  - Propagate via `propagate_attributes(session_id="...")`
  - All observations with same session_id grouped together
  - Support for session bookmarking, sharing, annotations

#### User Tracking
- **Definition**: Map traces and observations to individual users
- **Implementation:**
  - `user_id`: Username, email, or unique identifier
  - Propagate via `propagate_attributes(user_id="...")`
- **Features:**
  - User Explorer dashboard showing all users
  - Segment by token usage, trace count, feedback
  - Cost and usage attribution per user
  - User activity traces and history

### 1.5 Metadata and Tagging Systems

#### Metadata
- **Purpose**: Attach arbitrary key-value pairs to observations
- **Scope**: Can be attached to traces, spans, generations, events
- **Propagation**: Child observations automatically inherit parent metadata
- **Use Cases:**
  - Track request context (source, region, environment)
  - Store custom business logic attributes
  - Enable filtering and analysis in dashboards
- **Example:**
  ```python
  with propagate_attributes(metadata={
      "source": "api",
      "region": "us-east-1",
      "user_tier": "premium",
      "feature_flag": "new_rag_v2"
  }):
      # All nested observations inherit this metadata
      result = process_request()
  ```

#### Tags
- **Purpose**: Flexible categorization of traces
- **Scope**: Applied at trace level
- **Usage:**
  - Filter traces in UI and API
  - Group by feature/version/environment
  - Common patterns:
    - App versions: 'app-v1', 'app-v2'
    - Techniques: 'rag', 'cot', 'few-shot'
    - Environments: 'local', 'staging', 'prod'
    - Experiments: 'exp-a', 'exp-b'
- **Implementation:**
  ```python
  langfuse_context.update_current_trace(
      tags=["production", "rag-v2", "user-feedback"]
  )
  ```

#### Scores
- **Definition**: Evaluation metrics on traces, observations, sessions, or dataset runs
- **Types:**
  - Numeric (0-1, 1-10 scale)
  - Categorical (good/bad, happy/sad)
  - Boolean (pass/fail)
- **Features:**
  - Optional comments and reasoning
  - Schema validation via score configs
  - Support for LLM-as-a-Judge scores
  - Manual annotations
  - Custom evaluation pipelines

---

## 2. TRACING AND OBSERVABILITY

### 2.1 How Traces and Spans Work

#### Trace Lifecycle
1. **Initialization**: Trace created with unique ID and timestamp
2. **Observation Logging**: Operations recorded as spans/observations
3. **Nesting**: Child operations nested under parent spans
4. **Completion**: Trace completed with overall metrics
5. **Export**: Data sent to Langfuse backend

#### Context Propagation (OpenTelemetry)
- **Automatic**: When you create a nested span, it automatically becomes a child of current span
- **Manual**: Can explicitly set parent via parent_observation_id
- **Distributed**: Trace IDs propagate across service boundaries
- **Benefit**: No need to manually thread context through function calls

**Example Flow:**
```
Trace: user_request_123
├─ Span: retrieval
│  └─ Operation: vector_search (nested automatically)
├─ Span: llm_generation
│  ├─ Generation: llm_call (OpenAI)
│  └─ Span: post_processing
└─ Span: response_formatting
```

### 2.2 Observation Types and Characteristics

| Type | Purpose | Duration | Tokens/Cost | Key Attributes |
|------|---------|----------|-------------|-----------------|
| **Event** | Discrete occurrences | No | No | message, level |
| **Span** | Generic operations | Yes | No | name, status |
| **Generation** | LLM calls | Yes | Yes | model, prompt, completion |
| **Tool** | API calls | Yes | No | tool_name, result |
| **Retriever** | Data lookups | Yes | No | retriever_name, items |
| **Embedding** | Vector generation | Yes | Yes | model, tokens |
| **Agent** | Decision logic | Yes | No | action, reasoning |
| **Chain** | Step linking | Yes | No | chain_type |
| **Guardrail** | Safety checks | Yes | No | violation_type |
| **Evaluator** | Output assessment | Yes | No | score, reasoning |

### 2.3 Input/Output Tracking

#### What Gets Captured
- **Inputs**: Prompts, queries, function arguments
- **Outputs**: Model completions, API responses, function returns
- **Multi-modal Support**: Text, images, audio, JSON, tables
- **Large Payloads**: Stored in S3/blob storage with database references
- **Streaming**: Capture first token latency separately

#### Serialization
- **JSON Format**: Structured data serialized as JSON
- **Text Format**: Plain text for logs and error messages
- **Automatic Inference**: SDKs automatically capture function arguments/returns
- **Custom Handling**: Can manually specify input/output data

### 2.4 Latency and Cost Tracking

#### Latency Metrics
- **Span Duration**: `end_time - start_time`
- **Time-to-First-Token (TTFT)**: For streaming generations
- **Queue Time**: Wait time before operation starts
- **Components Measured:**
  - Total trace duration
  - Per-operation duration
  - Bottleneck identification
  - Parallelism visualization

#### Cost Tracking
- **Automatic Calculation**: For supported models (OpenAI, Anthropic, Google)
- **Two Mechanisms:**
  1. **Ingestion**: Cost data from LLM provider response
  2. **Inference**: Calculate from tokens if cost not provided
- **Priority**: Ingested cost > Inferred cost
- **Custom Models**: Define custom model prices per project

### 2.5 Token Usage Tracking

#### Usage Types Supported
- **Basic**: `input`, `output`
- **Advanced**: `cached_tokens`, `audio_tokens`, `image_tokens`, `reasoning_tokens`
- **Flexibility**: Any custom usage types supported

#### How It Works
1. **LLM Provider Response**: SDK extracts token counts
2. **Model Definition**: Maps token counts to costs
3. **Calculation**: Applies price per token type
4. **Storage**: Stored with generation for analysis

#### Model Definitions
- **Predefined Models**: 100+ built-in models (OpenAI, Claude, etc.)
- **Custom Models**: Add your own via API or UI
- **Tokenizer Support**: Uses official tokenizers (tiktoken, etc.)
- **Price Management**: Update prices without code changes

```python
# Example: Custom model definition
{
    "model_name": "my-custom-llm",
    "input_cost": 0.001,  # per 1K tokens
    "output_cost": 0.002,
    "cached_tokens_cost": 0.0001,
    "tokenizer": "tiktoken:cl100k_base"
}
```

### 2.6 Trace Timeline Visualization

#### Visualization Modes
1. **Timeline View**: Chronological visualization
   - Observations displayed as bars on timeline
   - Width proportional to duration
   - Color-coded by latency/cost percentiles
   - TTFT shown separately for streaming
   - Hover for detailed latency info

2. **Tree View**: Hierarchical structure
   - Parent-child relationships
   - Expandable/collapsible nodes
   - Shows nesting depth

3. **Graph View**: Network visualization
   - Component relationships
   - Data flow between operations

4. **Detail Panels**: Content inspection
   - Input/output data
   - Metadata and tags
   - Token counts and costs

#### Latency Analysis
- **Identify Bottlenecks**: See which operations are slowest
- **Parallelism**: Understand concurrent operations
- **Comparisons**: Compare latency across versions
- **Debugging**: Inspect individual operations for issues

---

## 3. SELF-HOSTING AND DEPLOYMENT

### 3.1 Deployment Architecture

#### Architecture Components

```
┌─────────────────────────────────────────────┐
│  Application SDKs (Python, JS/TS)           │
│  or OpenTelemetry Integration               │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  Langfuse Web Server                        │
│  - REST API endpoint                        │
│  - Authentication (API keys)                │
│  - Request validation                       │
│  - Minimal processing                       │
└──────────────────┬──────────────────────────┘
                   │
                   ▼ (queued)
┌─────────────────────────────────────────────┐
│  Redis (Queue & Cache)                      │
│  - Ingestion queue                          │
│  - Cache for prompts                        │
│  - API key cache                            │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  Langfuse Worker (Async Processing)         │
│  - Tokenization                             │
│  - Cost calculation                         │
│  - Data enrichment                          │
│  - Rate limiting                            │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
   ┌─────────────┐      ┌──────────────┐
   │ PostgreSQL  │      │ ClickHouse   │
   │ (OLTP)      │      │ (OLAP)       │
   └─────────────┘      └──────────────┘
   - Users, orgs       - Traces, obs.
   - Projects, keys    - Analytics
   - Sessions          - Fast queries
        │                     │
        └──────────┬──────────┘
                   ▼
           ┌──────────────────┐
           │ S3/Blob Storage  │
           │ - Large payloads │
           │ - Multi-modal    │
           └──────────────────┘
```

#### Database Roles

**PostgreSQL (Transactional - OLTP)**
- Users and authentication
- Organizations and projects
- API keys and credentials
- Sessions metadata
- Configuration and state
- Indexes for fast lookups
- Version: >= 12

**ClickHouse (Analytical - OLAP)**
- Traces and their hierarchies
- Observations (all types)
- Scores and evaluations
- Analytics queries
- Time-series data
- Columnar storage for performance
- Version: >= 24.3
- Minimum 16GB RAM for larger deployments
- 3 replicas recommended for production

**Redis (Cache & Queue)**
- Ingestion queue (message buffer)
- Prompt caching
- API key cache (in-memory)
- Session cache
- Can use Redis or Valkey

**S3/Blob Storage (Object Storage)**
- Raw payloads (when > size threshold)
- Multi-modal data (images, audio)
- Large exports
- Backups
- Supports: AWS S3, MinIO, Azure Blob Storage, GCS

### 3.2 Deployment Options

#### 1. Langfuse Cloud (Hosted)
**Best For**: Quick setup, no infrastructure management
- **Availability**: US (Oregon) and EU (Ireland) regions
- **Maintenance**: Handled by Langfuse team
- **Scale**: Enterprise-grade infrastructure
- **Pricing**: Per-unit (traces + observations + scores)
- **Setup Time**: Minutes

#### 2. Docker Compose (Development/Testing)
**Best For**: Local development, proof-of-concept, < 1M traces/month
- **Setup**: Single `docker-compose up`
- **Components**: PostgreSQL, ClickHouse, Redis, Langfuse containers
- **Time**: ~2-3 minutes to ready state
- **Limitations**: No HA, no persistence, security not production-ready
- **Not Recommended**: Production use

#### 3. Kubernetes + Helm (Production)
**Best For**: Production, high availability, scalability
- **Helm Chart**: Community-maintained langfuse/langfuse-k8s
- **Requirements**: Kubernetes 1.19+, Helm 3
- **Components**: Separate deployments for web/worker, managed DBs
- **HA**: Multiple replicas per component
- **Scaling**: Horizontal Pod Autoscaling (HPA), KEDA, VPA
- **Storage**: Persistent volumes for databases
- **Time**: 30+ minutes with proper configuration

#### 4. Cloud Templates (AWS, Azure, GCP)
**Best For**: Quick production setup on specific cloud
- **Infrastructure**: Terraform templates
- **Managed Services**: Managed RDS, managed ClickHouse
- **Networking**: VPCs, security groups configured
- **Time**: 15-30 minutes with defaults

### 3.3 Environment Variables and Configuration

#### Critical Configuration Variables

**Application Core:**
```bash
# Domain and authentication
NEXTAUTH_URL=https://langfuse.mycompany.com
NEXTAUTH_SECRET=<random-256-bit-key>

# Encryption
ENCRYPTION_KEY=<random-256-bit-key>
SALT=<random-salt>

# Database connections
DATABASE_URL=postgresql://user:password@host:5432/langfuse
CLICKHOUSE_URL=http://clickhouse:8123
CLICKHOUSE_PASSWORD=password

# Redis
REDIS_CONNECTION_STRING=redis://redis:6379

# API Configuration
LANGFUSE_BASE_URL=https://langfuse.mycompany.com
```

**Optional Initialization:**
```bash
LANGFUSE_INIT_ORG_ID=org-1
LANGFUSE_INIT_PROJECT_ID=proj-1
LANGFUSE_INIT_USER_EMAIL=admin@company.com
LANGFUSE_INIT_USER_PASSWORD=<secure-password>
```

**Storage & Performance:**
```bash
# S3/Blob storage
S3_ENDPOINT=https://s3.amazonaws.com
S3_BUCKET_NAME=langfuse-bucket
S3_ACCESS_KEY_ID=<key>
S3_SECRET_ACCESS_KEY=<secret>
S3_REGION=us-east-1

# ClickHouse specifics
CLICKHOUSE_CLUSTER_NAME=default
CLICKHOUSE_REPLICATION_FACTOR=3

# Worker configuration
LANGFUSE_INGESTION_QUEUE_PROCESSING_CONCURRENCY=20
LANGFUSE_TRACE_UPSERT_WORKER_CONCURRENCY=20
```

**Security & Compliance:**
```bash
# CORS
ALLOWED_ORIGINS=https://myapp.com,https://api.myapp.com

# Data retention
LANGFUSE_DATA_RETENTION_DAYS=90

# SSO (if using)
OAUTH_PROVIDER_ID=okta
OAUTH_CLIENT_ID=<id>
OAUTH_CLIENT_SECRET=<secret>
```

### 3.4 Docker Deployment

#### Basic Docker Compose Setup
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: langfuse
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: langfuse
    volumes:
      - postgres_data:/var/lib/postgresql/data

  clickhouse:
    image: clickhouse/clickhouse-server:latest
    environment:
      CLICKHOUSE_DB: langfuse
    volumes:
      - clickhouse_data:/var/lib/clickhouse
    
  redis:
    image: redis:7-alpine
    
  langfuse-web:
    image: langfuse/langfuse:latest
    depends_on:
      - postgres
      - clickhouse
      - redis
    environment:
      DATABASE_URL: postgresql://langfuse:${POSTGRES_PASSWORD}@postgres:5432/langfuse
      CLICKHOUSE_URL: http://clickhouse:8123
      REDIS_CONNECTION_STRING: redis://redis:6379
      NEXTAUTH_SECRET: ${NEXTAUTH_SECRET}
      ENCRYPTION_KEY: ${ENCRYPTION_KEY}
      SALT: ${SALT}
    ports:
      - "3000:3000"

  langfuse-worker:
    image: langfuse/langfuse:latest
    depends_on:
      - postgres
      - clickhouse
      - redis
    environment:
      # Same as web
      DATABASE_URL: postgresql://langfuse:${POSTGRES_PASSWORD}@postgres:5432/langfuse
      CLICKHOUSE_URL: http://clickhouse:8123
      REDIS_CONNECTION_STRING: redis://redis:6379
    command: "node dist/server.js worker"

volumes:
  postgres_data:
  clickhouse_data:
```

#### Kubernetes Deployment Example
```bash
# Add Helm repository
helm repo add langfuse https://langfuse.github.io/langfuse-k8s
helm repo update

# Install with values file
helm install langfuse langfuse/langfuse -f values.yaml

# Upgrade
helm upgrade langfuse langfuse/langfuse -f values.yaml
```

### 3.5 Database Requirements

#### PostgreSQL Requirements
- **Version**: >= 12 (16+ recommended)
- **RAM**: 2-4GB minimum
- **Storage**: 100GB+ for metadata
- **Connections**: Langfuse needs ~20 connections
- **Timezone**: Must be UTC (required!)
- **Backup**: Regular snapshots recommended
- **Managed Services**: AWS RDS, Azure Database, Google Cloud SQL

#### ClickHouse Requirements
- **Version**: >= 24.3
- **RAM**: 16GB minimum (32GB+ for larger deployments)
- **CPU**: 4+ cores recommended
- **Storage**: 500GB-5TB+ depending on retention
- **Replication**: 3 replicas minimum for HA
- **Timezone**: Must be UTC (required!)
- **Sharding**: Single shard supported (don't use multi-shard)
- **Network**: Low-latency connection to web/worker

#### Scaling Thresholds
| Traces/Month | Recommendation |
|--------------|-----------------|
| < 1M | Docker Compose on VM |
| 1M - 100M | Kubernetes, 2CPU/4GB per container |
| 100M - 1B | Kubernetes, 4CPU/8GB per container, ClickHouse large |
| > 1B | Kubernetes, enterprise ClickHouse, multi-region |

### 3.6 Configuration Best Practices

**Production Hardening:**
```bash
# Memory limits (Node.js)
NODE_OPTIONS=--max-old-space-size=20480

# Keep-alive timeout (prevent 502 errors)
KEEP_ALIVE_TIMEOUT=65000  # Load balancer timeout + 5s

# Connection pooling
DATABASE_CONNECTION_POOL_SIZE=20
DATABASE_CONNECTION_TIMEOUT_SECONDS=30

# Queue optimization
LANGFUSE_INGESTION_QUEUE_BATCH_SIZE=100
LANGFUSE_INGESTION_QUEUE_PROCESSING_TIMEOUT_SECONDS=60

# ClickHouse optimization
CLICKHOUSE_ASYNC_INSERT=true
CLICKHOUSE_ASYNC_INSERT_BUSY_TIMEOUT_MS=5000
```

**Observability & Monitoring:**
```bash
# Health endpoints
HEALTH_CHECK_ENABLED=true

# Metrics (StatsD)
TELEMETRY_ENABLED=true
STATSD_ENABLED=true
STATSD_HOST=localhost
STATSD_PORT=8125

# OpenTelemetry
OTEL_ENABLED=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
```

**Data Retention & Privacy:**
```bash
# Data retention policy
LANGFUSE_DATA_RETENTION_DAYS=90

# PII masking (enterprise)
DATA_MASKING_ENABLED=true
DATA_MASKING_PATTERNS=email,ssn,api_key

# GDPR compliance
GDPR_MODE=true
```

---

## 4. INTEGRATIONS AND FRAMEWORKS

### 4.1 Native SDK Support

#### Python SDK v3 (Latest - OpenTelemetry-based)
- **Latest Version**: June 2025 release
- **Installation**: `pip install langfuse`
- **Approach**: Decorator-based or context managers
- **Features**:
  - Automatic input/output capture
  - Async/sync support
  - Context propagation (OpenTelemetry)
  - Minimal code changes required

#### JavaScript/TypeScript SDK
- **Installation**: `npm install langfuse`
- **Supports**: Browser and Node.js
- **Features**: Same as Python SDK
- **Browser SDK**: Lightweight for frontend instrumentation

### 4.2 LangChain Integration

#### Setup
```python
from langfuse.langchain import CallbackHandler

# Initialize handler
langfuse_handler = CallbackHandler(
    public_key="pk-lf-...",
    secret_key="sk-lf-...",
    base_url="https://cloud.langfuse.com"
)

# Or use environment variables
# LANGFUSE_PUBLIC_KEY, LANGFUSE_SECRET_KEY, LANGFUSE_BASE_URL
```

#### Usage Patterns

**LCEL Chains (Recommended):**
```python
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from langchain.schema import StrOutputParser

prompt = ChatPromptTemplate.from_template("What is {topic}?")
model = ChatOpenAI()
chain = prompt | model | StrOutputParser()

# Invoke with callback
result = chain.invoke(
    {"topic": "Langfuse"},
    config={"callbacks": [langfuse_handler]}
)
```

**Constructor Callbacks:**
```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    model="gpt-4",
    callbacks=[langfuse_handler]  # Used for every call
)

response = llm.invoke("What is Langfuse?")
```

**Metadata & Attributes:**
```python
response = chain.invoke(
    {"person": "Obama"},
    config={
        "callbacks": [langfuse_handler],
        "metadata": {
            "langfuse_user_id": "user-123",
            "langfuse_session_id": "session-abc",
            "langfuse_tags": ["production", "rag-v2"]
        }
    }
)
```

#### Supported Methods
- `invoke()` - Synchronous
- `ainvoke()` - Asynchronous
- `batch()` - Batch processing
- `abatch()` - Async batch
- `stream()` - Token streaming
- `astream()` - Async streaming

### 4.3 OpenTelemetry Integration

#### Overview
- **Basis**: Langfuse Python SDK v3 is built on OpenTelemetry
- **Compatibility**: Works with any OTel-instrumented library
- **Automatic**: Any OTel span automatically becomes Langfuse observation
- **Ecosystem**: Integrates with 50+ OTel instrumentation libraries

#### Example: Automatic Anthropic Tracing
```python
from anthropic import Anthropic
from opentelemetry.instrumentation.anthropic import AnthropicInstrumentor
from langfuse import get_client

# Enable automatic instrumentation
AnthropicInstrumentor().instrument()

# Langfuse client auto-captures all Anthropic calls
langfuse = get_client()

client = Anthropic()
message = client.messages.create(
    model="claude-3-sonnet",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}]
)
# Automatically traced in Langfuse
```

#### Compatible Libraries
- **OpenLLMetry** - Generic LLM instrumentation
- **OpenLIT** - Open source observability
- **Anthropic SDK** - Automatic via instrumentation
- **Custom OTel**: Any library with OTel instrumentation

### 4.4 Other Framework Integrations
- **LlamaIndex**: Native integration
- **LiteLLM**: Proxy integration
- **DSPy**: Direct integration
- **LangGraph**: Via LangChain callback
- **Amazon Bedrock**: AWS integration
- **OpenAI SDK**: Direct or wrapper integration
- **Azure OpenAI**: Native integration
- **Custom Frameworks**: OpenTelemetry interface

---

## 5. ADVANCED FEATURES

### 5.1 Evaluations and Scoring

#### Evaluation Methods

**1. LLM-as-a-Judge**
- Uses another LLM to evaluate outputs
- Configurable rubrics and scoring prompts
- Chain-of-thought reasoning captured
- Cost-effective at scale
- More nuanced than metrics

**2. Custom Scores**
- Via Python/JavaScript SDKs
- Via REST API
- Backend evaluation pipeline
- User feedback collection
- Guardrail checks

**3. Human Annotations**
- UI-based scoring interface
- Batch annotation workflows
- Quality assurance
- Training data creation

#### Score Types
- **Numeric**: 0-1, 1-5, 1-10 scales
- **Categorical**: good/bad, happy/neutral/sad
- **Boolean**: pass/fail, approved/rejected

#### Score Analytics
Built-in metrics for evaluation validation:
- Pearson/Spearman correlation (compare evaluators)
- MAE, RMSE (error metrics)
- F1 Score (classification)
- Overall Agreement

### 5.2 Datasets and Experimentation

#### Dataset Structure
- **Dataset**: Collection of test items
- **Item**: Input, expected output, metadata
- **Run**: Execution of application against dataset
- **Comparison**: Side-by-side run comparison

#### Use Cases
- **Benchmarking**: Create standard test sets
- **Regression Detection**: Catch quality drops
- **A/B Testing**: Compare prompts/models
- **Edge Case Management**: Add new cases from production

#### Workflow
```
1. Create Dataset
   ├─ Add Items (inputs + expected outputs)
   ├─ Import from CSV
   └─ Label items

2. Run Experiment
   └─ Execute app against each item
   
3. Evaluate Results
   ├─ Apply LLM-as-Judge
   ├─ Compare across runs
   └─ View metrics
```

### 5.3 Prompt Management and Versioning

#### Core Features
- **Version Control**: Auto-versioned prompt changes
- **Labels**: Production, staging, experiment tags
- **Caching**: Client-side 60s TTL, server Redis cache
- **Protected Labels**: Admin-controlled production labels

#### Workflow
```python
# Fetch prompt (auto-cached)
prompt = langfuse.get_prompt(
    name="summarizer",
    label="production"  # or version=5
)

# Use the prompt template
formatted = prompt.compile(
    text=long_text,
    format_hint="markdown"
)

# Send to LLM
response = client.messages.create(
    model="claude-3-sonnet",
    messages=[{"role": "user", "content": formatted}]
)
```

#### Caching Strategy
- **Default TTL**: 60 seconds
- **Customizable**: Set per call
- **Fallback**: Stale cache returned if fetch fails
- **Async Refresh**: Background update doesn't block

---

## 6. SECURITY, COMPLIANCE, AND OPERATIONS

### 6.1 API Authentication

#### Basic Auth
- **Method**: HTTP Basic Authentication
- **Headers**: `Authorization: Basic base64(public_key:secret_key)`
- **Location**: Project Settings → API Keys

#### API Key Management
- **Public Key**: Identifies project
- **Secret Key**: Authentication credential
- **Scopes**: Project-level (can add org-level)
- **Rotation**: Create new keys, remove old ones
- **Security**: Treat secret key like password

```python
# SDK authentication
from langfuse import Langfuse

langfuse = Langfuse(
    public_key="pk-lf-...",
    secret_key="sk-lf-...",
    host="https://cloud.langfuse.com"
)
```

### 6.2 Data Security and Privacy

#### Encryption
- **In Transit**: TLS 1.2+ for all connections
- **At Rest**: AES-256 encryption in database
- **Keys**: Customer-managed in self-hosted

#### Data Retention
- **Default**: Indefinite (until account closed)
- **Configurable**: Per-project retention policies
- **Minimum**: 3 days
- **Purge**: Automatic nightly purge of expired data
- **Deletion**: On-demand via UI or API

#### GDPR Compliance
- **Right to Access**: Export user data
- **Right to Erasure**: Delete traces, projects, accounts
- **Right to Portability**: Export in standard formats
- **Data Minimization**: Collect only necessary data
- **DPA**: Data Processing Agreement available
- **Contact**: privacy@langfuse.com

#### Privacy Controls
- **Data Masking**: Enterprise feature to mask PII
- **IP Anonymization**: Optional
- **No Model Training**: User data never used to train models
- **Compliance Certifications**:
  - SOC2 Type 2
  - ISO 27001
  - Annual penetration tests

### 6.3 Self-Hosted Compliance

#### Database Encryption
- PostgreSQL: Enable at-rest encryption (AWS, Azure, GCP support)
- ClickHouse: Encryption at-rest supported
- Both: Network encryption required (TLS)

#### Network Security
- **VPC Isolation**: Run in private subnets
- **Security Groups**: Restrict access to databases
- **Secrets Management**: Use vault for credentials
- **IP Whitelisting**: Restrict API access

#### Backup & Recovery
- **PostgreSQL**: Regular snapshots (1x daily minimum)
- **ClickHouse**: Point-in-time recovery configured
- **Storage**: S3/bucket replication
- **RPO**: Depends on backup frequency
- **RTO**: Practice recovery procedures

### 6.4 Monitoring and Observability

#### Health Endpoints
```
GET /health - Basic health check
GET /ready - Readiness probe (for K8s)
```

#### Metrics (StatsD)
- `langfuse.queue.ingestion.length` - Queue depth
- `langfuse.trace.processing.duration` - Processing latency
- `langfuse.db.connection.count` - Active connections
- `langfuse.cache.hit_rate` - Cache efficiency

#### Logging
- Application logs: JSON structured format
- Database logs: Query performance and errors
- Queue logs: Processing and retries

### 6.5 Scaling and Performance Optimization

#### Worker Scaling
- **Metric**: Monitor CPU usage (target <50%)
- **Approach**: Scale workers by CPU load
- **Async**: All processing asynchronous
- **Queue**: Redis-backed with configurable concurrency

#### Database Optimization
- **ClickHouse**: Scale vertically (add memory)
- **PostgreSQL**: Connection pooling, indexes
- **Redis**: Monitor key evictions
- **S3**: Enable versioning and lifecycle policies

#### Common Bottlenecks and Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| 502/504 errors | Keep-alive timeout | Increase `KEEP_ALIVE_TIMEOUT` |
| High memory usage | Node.js heap | Increase `NODE_OPTIONS --max-old-space-size` |
| Slow queries | Large traces | Add data retention policy |
| Queue backlog | Worker undersized | Add worker replicas/increase concurrency |
| ClickHouse slow | Undersized instance | Scale vertically (add RAM) |

#### Queue Sharding (Advanced)
- **When**: If Redis CPU > 50%
- **How**: Configure `LANGFUSE_INGESTION_QUEUE_SHARDS`
- **Warning**: Don't reduce shards after setting
- **Impact**: Must scale `CONCURRENCY` settings proportionally

---

## 7. DATA FLOW EXAMPLES

### 7.1 Basic LLM Application Trace

```python
from langfuse import observe

@observe()
def process_user_request(user_query: str):
    # Create a trace automatically
    # Capture input/output
    
    retrieval_results = retrieve_context(user_query)
    
    response = generate_response(
        query=user_query,
        context=retrieval_results
    )
    
    return response

@observe()
def retrieve_context(query: str):
    # Nested span created automatically
    results = vector_store.search(query, top_k=5)
    return results

@observe()
def generate_response(query: str, context: list):
    # Another nested span
    prompt = f"Question: {query}\nContext: {context}"
    response = client.messages.create(
        model="claude-3-sonnet",
        messages=[{"role": "user", "content": prompt}]
    )
    return response.content[0].text

# Usage
result = process_user_request("What is Langfuse?")
# Trace captured with all spans!
```

**Resulting Trace Structure:**
```
Trace: process_user_request
├── Input: "What is Langfuse?"
├── Span: retrieve_context
│   ├── Input: "What is Langfuse?"
│   ├── Tool call: vector_store.search
│   └── Output: [results...]
├── Span: generate_response
│   ├── Input: query + context
│   ├── Generation: ChatOpenAI call
│   │   ├── Model: claude-3-sonnet
│   │   ├── Prompt: [formatted prompt]
│   │   ├── Completion: [response text]
│   │   ├── Tokens: {input: 150, output: 75}
│   │   └── Cost: $0.00075
│   └── Output: [final response]
└── Output: [final response]
```

### 7.2 LangChain RAG Application Trace

```python
from langfuse.langchain import CallbackHandler
from langchain_openai import ChatOpenAI
from langchain.retrievers import VectorStoreRetriever
from langchain.chains import RetrievalQA

# Setup Langfuse
handler = CallbackHandler()

# Build chain
llm = ChatOpenAI(model="gpt-4")
retriever = VectorStoreRetriever(vectorstore=my_vectorstore)
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    chain_type="stuff",
    retriever=retriever
)

# Execute with callback
result = qa_chain.invoke(
    {"query": "Tell me about Langfuse"},
    config={
        "callbacks": [handler],
        "metadata": {
            "langfuse_user_id": "user-123",
            "langfuse_session_id": "session-456",
            "langfuse_tags": ["production"]
        }
    }
)

# Automatically captures:
# - Retriever calls and documents
# - LLM calls with token counts
# - Chain execution flow
# - All nested operations
```

### 7.3 Multi-Turn Conversation Trace

```python
from langfuse import observe, propagate_attributes

SESSION_ID = "conv-user-123"

@observe()
def chat_turn(user_message: str, turn_num: int):
    with propagate_attributes(
        session_id=SESSION_ID,
        user_id="user-123",
        metadata={"turn": turn_num}
    ):
        # All operations inherit session/user context
        response = generate_response(user_message)
        return response

# Multiple calls create one session
chat_turn("Hello", turn_num=1)  # Trace 1, Session A
chat_turn("Tell me more", turn_num=2)  # Trace 2, Session A
chat_turn("Thanks", turn_num=3)  # Trace 3, Session A

# Session view shows all 3 traces grouped together
# "Session replay" shows conversation flow
```

---

## 8. PRICING AND COST MODEL

### 8.1 Cloud Pricing

**Billing Unit**: Traces + Observations + Scores

- **Example**: 1,000 traces with 5,000 observations + 500 scores = 6,500 units
- **Free Tier**: Limited monthly usage
- **Paid Tiers**: Stacked pricing (cheaper at scale)
- **Estimate**: 1M units/month ~ $100-200

### 8.2 Self-Hosted Costs

**Infrastructure Costs:**
- PostgreSQL: Managed $20-50/month or self-hosted
- ClickHouse: Managed $100-500/month or self-hosted
- Redis: Managed $10-30/month or self-hosted
- S3/Blob: Pay-per-GB (typically $50-200/month)
- Compute: Kubernetes or VM costs
- Total: $200-1000+/month depending on scale

**Considerations:**
- Software: Open source (free license)
- Support: Enterprise support available
- Dev time: Operational overhead

---

## 9. ARCHITECTURE PATTERNS AND BEST PRACTICES

### 9.1 SDK Integration Patterns

#### Pattern 1: Decorator-based (Simplest)
```python
from langfuse import observe

@observe()
def my_function(arg1, arg2):
    return process(arg1, arg2)
```
**Pros**: Minimal code, automatic capture
**Cons**: Limited customization

#### Pattern 2: Context Manager
```python
from langfuse import get_client

langfuse = get_client()

with langfuse.start_as_current_span(name="operation") as span:
    result = do_work()
    span.update(output=result)
```
**Pros**: Fine-grained control
**Cons**: More boilerplate

#### Pattern 3: Manual SDK Calls (Low-level)
```python
langfuse.trace(
    name="custom-trace",
    input={"query": "..."},
    output={"result": "..."},
    user_id="user-123"
)
```
**Pros**: Maximum control
**Cons**: Most boilerplate

### 9.2 Data Organization Best Practices

1. **Use consistent tags**: Standardize on versions, environments, techniques
2. **Attach metadata early**: Propagate once, inherit down
3. **Set user_id and session_id**: Enable cross-trace analysis
4. **Name observations clearly**: Use action verbs ("retrieve", "generate", "score")
5. **Capture business context**: Include feature flags, user segments, A/B test variants

### 9.3 Cost Optimization

1. **Filter at source**: Don't trace non-critical operations
2. **Sample in production**: Trace 1% of requests, 100% in dev
3. **Use retention policies**: Purge old traces automatically
4. **Compress payloads**: Store minimal necessary data
5. **Batch writes**: Use batch APIs for bulk ingestion

---

## 10. TROUBLESHOOTING AND FAQ

### 10.1 Common Issues

**Issue: Data not appearing in Langfuse**
- Check API keys are correct
- Verify environment variables set
- Check network connectivity to Langfuse endpoint
- Look at SDK logs for errors
- Ensure trace has at least ended (not incomplete)

**Issue: High latency in trace ingestion**
- Check worker CPU usage
- Add more worker replicas
- Verify database performance
- Reduce trace verbosity
- Use sampling in production

**Issue: 502/504 errors**
- Increase `KEEP_ALIVE_TIMEOUT`
- Check Load Balancer idle timeout
- Add more web replicas
- Monitor database connections

**Issue: Out of memory (Node.js)**
- Increase `NODE_OPTIONS --max-old-space-size`
- Reduce batch size
- Add more replicas with smaller batches

### 10.2 Version Compatibility

**Current Versions (2025):**
- SDK v3: OpenTelemetry-native (recommended)
- SDK v2: Legacy (still supported)
- Langfuse >= 3.63.0: Required for SDK v3

**Deprecation:**
- SDK v2: Sunset in 2025
- Migrate via compatibility guides

### 10.3 Getting Help

- **Documentation**: https://langfuse.com/docs
- **Community**: GitHub Discussions
- **Issues**: GitHub Issues for bugs
- **Email**: support@langfuse.com
- **Enterprise**: sales@langfuse.com
- **Privacy**: privacy@langfuse.com

---

## APPENDIX: KEY RESOURCES

### Official Links
- **Homepage**: https://langfuse.com
- **Docs**: https://langfuse.com/docs
- **GitHub**: https://github.com/langfuse/langfuse
- **Cloud**: https://cloud.langfuse.com

### SDKs and Clients
- **Python SDK**: https://github.com/langfuse/langfuse-python
- **JS/TS SDK**: https://github.com/langfuse/langfuse-js
- **Docker**: https://hub.docker.com/r/langfuse/langfuse
- **Helm Charts**: https://github.com/langfuse/langfuse-k8s

### Integration Guides
- **LangChain**: https://langfuse.com/docs/integrations/langchain
- **LlamaIndex**: https://langfuse.com/docs/integrations/llamaindex
- **LiteLLM**: https://langfuse.com/docs/integrations/litellm
- **OpenTelemetry**: https://langfuse.com/docs/integrations/otel

### Deployment Guides
- **Self-hosting**: https://langfuse.com/self-hosting
- **Docker Compose**: https://langfuse.com/self-hosting/deployment/docker-compose
- **Kubernetes**: https://langfuse.com/self-hosting/deployment/kubernetes-helm
- **AWS**: https://langfuse.com/self-hosting/deployment/aws

### Learning Resources
- **Blog**: https://langfuse.com/blog
- **Cookbook**: https://langfuse.com/guides/cookbook
- **YouTube**: Langfuse channel (tutorials and demos)
- **Community**: GitHub Discussions for questions

---

## SUMMARY TABLE: Feature Matrix

| Feature | Cloud | Self-Hosted | Notes |
|---------|-------|-------------|-------|
| Tracing | ✓ | ✓ | Core feature |
| Evaluations | ✓ | ✓ (v3.63+) | LLM-as-Judge, custom scores |
| Prompt Management | ✓ | ✓ | Versioning, caching |
| Datasets | ✓ | ✓ | Experimentation |
| Multi-region | US, EU | On-prem only | Cloud has US/EU |
| SSO/SAML | ✓ | Enterprise | Self-hosted limited |
| Data Retention | Configurable | Configurable | Automatic purge |
| GDPR Compliance | ✓ | Yes | Privacy controls |
| HA/Failover | Built-in | Kubernetes | Cloud managed |
| Cost | Per-unit | Infrastructure | Depends on volume |

---

**Document Version**: 1.0
**Last Updated**: November 2025
**Coverage**: Langfuse v3.63+


---

# Evaluation & Prompt Management

# Comprehensive Research: Langfuse Evaluation and Prompt Management Features

## Table of Contents
1. [Evaluation and Scoring](#evaluation-and-scoring)
2. [Prompt Management](#prompt-management)
3. [Analytics and Dashboards](#analytics-and-dashboards)
4. [Code Examples](#code-examples)
5. [Best Practices](#best-practices)

---

## EVALUATION AND SCORING

### Overview
Langfuse provides three primary evaluation approaches:
- **LLM-as-a-Judge**: Automatic scoring using language models
- **Human Annotations**: Manual evaluation by team members
- **Custom Scoring**: Flexible API/SDK-based scoring for specialized metrics

### 1. Score Types and Data Model

Langfuse supports three flexible score data types:

#### Numeric Scores
- Float values for continuous measurements
- Can have min/max constraints defined in ScoreConfig
- Examples: accuracy ratings (0-1), quality scores (1-10)

#### Categorical Scores
- String values for classification
- Must match predefined categories in ScoreConfig
- Examples: "correct", "partially_correct", "incorrect"

#### Boolean Scores
- Binary assessment (0 or 1)
- Examples: pass/fail, valid/invalid

### 2. Score Configuration & Standardization

Score Configs enforce consistent evaluation schemas across your team:

```python
# Creating a score config via UI ensures standardization
# Navigate to: Project Settings > Scores / Evaluation

# Example configurations:
# - Numeric: min=0, max=1, name="accuracy"
# - Categorical: categories=["good", "fair", "poor"], name="quality"
# - Boolean: true/false for pass/fail scenarios

# When ingesting scores, reference the configId:
langfuse.create_score(
    name="accuracy",
    value=0.95,
    trace_id="trace_123",
    config_id="config_score_123",  # Validates against config schema
    data_type="NUMERIC"
)
```

### 3. LLM-as-a-Judge Evaluation

#### How It Works
An LLM evaluates outputs by:
1. Receiving a trace or dataset entry
2. Assessing quality based on a rubric
3. Scoring and providing chain-of-thought reasoning

#### Key Benefits
- **Scalability**: Score thousands of outputs quickly and cost-effectively
- **Nuance**: Captures complexity (helpfulness, safety, coherence) better than metrics
- **Consistency**: Fixed rubrics enable repeatable scoring

#### Built-in Evaluation Templates
Langfuse provides pre-built templates for:
- Hallucination detection
- Helpfulness assessment
- Relevance scoring
- Toxicity detection
- Correctness evaluation
- Context relevance
- Context correctness
- Conciseness measurement

#### Supported LLMs for Evaluation
Works with any LLM supporting tool/function calling:
- OpenAI
- Azure OpenAI
- Anthropic
- AWS Bedrock
- Any LLM via LiteLLM gateway

#### Implementation Example
```python
# Langfuse handles LLM-as-judge evaluation setup in UI
# Select your evaluator, configure variables, apply to traces/datasets
# Each evaluation creates a full trace for complete visibility

# Access results via API
evaluations = langfuse.api.scores.get_many()
for score in evaluations:
    print(f"Evaluation: {score.name}")
    print(f"Score: {score.value}")
    print(f"Comment: {score.comment}")  # Chain-of-thought reasoning
```

### 4. Human Annotation Workflows

#### Single Item Annotation
Annotate individual traces, sessions, or observations directly from detail views.

#### Annotation Queues for Scale
For large-scale projects:
1. Create named queues with specific score dimensions
2. Assign queue access to team members
3. Process items sequentially with immediate feedback
4. Track progress via summary metrics

#### Team Collaboration Features
```python
# Setup in UI:
# 1. Navigate to Human Annotation
# 2. Click "New queue"
# 3. Select Score Configs for standardized scoring
# 4. Assign team members to queue
# 5. Add traces/sessions to annotate

# Example workflow:
# - Quality Assurance Queue: score "relevance", "correctness"
# - Safety Review Queue: score "toxic_content", "safety_issues"
# - UX Feedback Queue: score "clarity", "helpfulness"
```

#### Benchmarking
Establish human baseline scores to:
- Compare and evaluate other metrics
- Provide clear performance reference
- Enhance objectivity of evaluations

### 5. Custom Scoring via SDK/API

#### Use Cases
1. **User Feedback Collection**: Capture in-app user feedback via Browser SDK
2. **External Evaluation Pipelines**: Continuously monitor quality by fetching traces and running evaluations
3. **Guardrails & Security**: Validate output format, keywords, or length
4. **Runtime Evaluations**: Track if SQL code executed successfully, if JSON is valid
5. **Custom Metrics**: Any specialized evaluation logic

#### Python SDK Implementation
```python
from langfuse import get_client

langfuse = get_client()

# Method 1: Score a specific observation
with langfuse.start_as_current_observation(as_type="generation", name="summary") as gen:
    gen.update(output="summary text...")
    
    # Score the generation
    gen.score(name="conciseness", value=0.8, data_type="NUMERIC")
    
    # Score the entire trace
    gen.score_trace(name="user_feedback", value="positive", data_type="CATEGORICAL")

# Method 2: Score context-aware
with langfuse.start_as_current_observation(as_type="span", name="complex_task") as span:
    # ... task execution ...
    langfuse.score_current_span(name="quality", value=True, data_type="BOOLEAN")
    if task_successful:
        langfuse.score_current_trace(name="success", value=1.0, data_type="NUMERIC")

# Method 3: Low-level create score (when IDs are known)
langfuse.create_score(
    name="fact_check",
    value=0.95,
    trace_id="trace_abc123",
    observation_id="obs_def456",  # Optional
    session_id="session_xyz",      # Optional
    data_type="NUMERIC",
    comment="95% of claims verified"
)

# Update existing scores (by providing score_id)
langfuse.create_score(
    name="fact_check",
    value=0.98,
    score_id="score_existing",  # Updates if exists
    trace_id="trace_abc123",
    data_type="NUMERIC"
)
```

#### JavaScript/TypeScript Implementation
```typescript
import { LangfuseClient } from "@langfuse/client";

const langfuse = new LangfuseClient();

// Fetch scores
const scores = await langfuse.api.scoreV2.get();

// Create score with validation
await langfuse.api.scoreV2.create({
    name: "accuracy",
    value: 0.92,
    traceId: "trace_123",
    dataType: "NUMERIC",
    configId: "config_accuracy",  // Validates against schema
    comment: "Output matches expected format"
});

// Update existing score
await langfuse.api.scoreV2.create({
    id: "score_existing",  // Updates existing score
    name: "accuracy",
    value: 0.95,
    traceId: "trace_123"
});
```

### 6. Datasets and Experiments

#### Dataset Management
Datasets are collections of inputs and expected outputs for systematic testing.

```python
from langfuse import get_client

langfuse = get_client()

# Create dataset
dataset = langfuse.create_dataset(name="customer_support_qa")

# Add items
dataset.items.add(
    input={"query": "How do I reset my password?"},
    expected_output="Password reset link sent to email"
)

dataset.items.add(
    input={"query": "What are your hours?"},
    expected_output="Open 9 AM - 5 PM EST, Monday-Friday"
)

# Fetch for use in experiments
my_dataset = langfuse.get_dataset("customer_support_qa")
```

#### JavaScript/TypeScript
```typescript
// Create dataset
const dataset = await langfuse.api.datasets.create({
    name: "customer_support_qa"
});

// Add items
await langfuse.api.datasetItems.create({
    datasetId: dataset.id,
    input: { query: "How do I reset my password?" },
    expectedOutput: "Password reset link sent to email"
});
```

#### Running Experiments with Evaluators

```python
from langfuse import Evaluation, get_client
from langfuse.openai import OpenAI

# Define evaluator function
def accuracy_evaluator(*, input, output, expected_output, **kwargs):
    """Evaluate if output matches expected output"""
    if expected_output and expected_output.lower() in output.lower():
        return Evaluation(
            name="accuracy",
            value=1.0,
            comment="Output contains expected content"
        )
    return Evaluation(
        name="accuracy",
        value=0.0,
        comment="Output missing expected content"
    )

def length_evaluator(*, input, output, **kwargs):
    """Evaluate response length"""
    return Evaluation(
        name="response_length",
        value=len(output),
        comment=f"{len(output)} characters"
    )

# Define task (what to run)
def my_task(*, item, **kwargs):
    """Your LLM application logic"""
    question = item["input"]["query"]
    response = OpenAI().chat.completions.create(
        model="gpt-4",
        messages=[{"role": "user", "content": question}]
    )
    return response.choices[0].message.content

# Run experiment
langfuse = get_client()
result = langfuse.run_experiment(
    name="Customer Support QA - GPT-4",
    data=[
        {"input": {"query": "How do I reset password?"}, "expected_output": "Reset link"},
        {"input": {"query": "What are hours?"}, "expected_output": "9 AM - 5 PM EST"}
    ],
    task=my_task,
    evaluators=[accuracy_evaluator, length_evaluator],
    max_concurrency=5
)

# Print results
print(result.format())
```

#### Run-Level Evaluators (Aggregate Metrics)
```python
def average_accuracy(*, item_results, **kwargs):
    """Calculate average accuracy across all items"""
    accuracies = [
        eval.value for result in item_results
        for eval in result.evaluations
        if eval.name == "accuracy"
    ]
    
    if not accuracies:
        return Evaluation(name="avg_accuracy", value=None)
    
    avg = sum(accuracies) / len(accuracies)
    return Evaluation(
        name="avg_accuracy",
        value=avg,
        comment=f"Average: {avg:.2%}"
    )

result = langfuse.run_experiment(
    name="Comprehensive Analysis",
    data=test_data,
    task=my_task,
    evaluators=[accuracy_evaluator],
    run_evaluators=[average_accuracy]  # Aggregate metric
)
```

### 7. External Evaluation Pipelines

For continuous quality monitoring:

```python
from langfuse import get_client
from deepeval import evaluate  # Example: using Deepeval

langfuse = get_client()

# Step 1: Fetch traces from Langfuse
traces = langfuse.api.traces.list()

# Step 2: Run evaluations
for trace in traces:
    # Get the LLM output
    output = trace.output
    
    # Run custom evaluation (e.g., with Deepeval)
    score = evaluate(output)
    
    # Step 3: Send results back to Langfuse
    langfuse.create_score(
        name="deepeval_score",
        value=score,
        trace_id=trace.id,
        data_type="NUMERIC"
    )
```

### 8. User Feedback Collection

#### Explicit Feedback
Direct prompts for user ratings or comments:

```python
# Browser SDK for frontend feedback collection
# Install: npm install @langfuse/web

import { Langfuse } from "@langfuse/web";

const langfuse = new Langfuse({
    publicKey: "pk-lf-...",
    baseUrl: "https://cloud.langfuse.com"
});

// Collect feedback from user
function rateResponse(traceId, rating) {
    langfuse.score({
        name: "user_rating",
        value: rating,  // 1-5 stars
        traceId: traceId,
        dataType: "NUMERIC"
    });
}

// Or categorical feedback
function markResponse(traceId, feedback) {
    langfuse.score({
        name: "user_feedback",
        value: feedback,  // "helpful", "not_helpful", "spam"
        traceId: traceId,
        dataType: "CATEGORICAL"
    });
}
```

#### Implicit Feedback
Inferred from user behavior (clicks, time spent, etc.):

```python
# Track user interactions as implicit feedback
langfuse.create_score(
    name="user_accepted",
    value=1.0 if user_accepted_output else 0.0,
    trace_id=trace_id,
    data_type="BOOLEAN",
    comment="User accepted the generated response"
)

# Track if user re-queried (indicates dissatisfaction)
if user_requeried:
    langfuse.create_score(
        name="user_requery",
        value=1.0,
        trace_id=trace_id,
        data_type="BOOLEAN",
        comment="User submitted a follow-up query"
    )
```

---

## PROMPT MANAGEMENT

### 1. Overview and Core Concepts

Langfuse Prompt Management enables:
- Version control and rollback
- Non-code prompt updates
- A/B testing in production
- Prompt deployment workflows
- Collaboration with non-technical team members

### 2. Prompt Types and Templates

#### Text Prompts
Single string with optional variables:
```python
from langfuse import get_client

langfuse = get_client()

# Create text prompt with variables
langfuse.create_prompt(
    name="summarizer",
    type="text",
    prompt="Summarize the following text in {{language}}:\n\n{{text}}",
    config={
        "model": "gpt-4o",
        "temperature": 0.3
    },
    labels=["production"]
)

# Fetch and compile
prompt = langfuse.get_prompt("summarizer")
compiled = prompt.compile(language="French", text="Long document here...")
```

#### Chat Prompts
Array of messages for conversational models:
```python
langfuse.create_prompt(
    name="customer_support",
    type="chat",
    prompt=[
        {
            "role": "system",
            "content": "You are a helpful customer support assistant. Always be polite and helpful."
        },
        {
            "role": "user",
            "content": "{{customer_question}}"
        }
    ],
    config={
        "model": "gpt-4-turbo",
        "temperature": 0.7,
        "max_tokens": 500
    },
    labels=["production"]
)
```

#### JavaScript/TypeScript
```typescript
// Create text prompt
await langfuse.prompt.create({
    name: "email_writer",
    type: "text",
    prompt: "Write a professional email about {{topic}} in {{tone}} tone",
    labels: ["production"],
    config: {
        model: "gpt-4o",
        temperature: 0.5
    }
});

// Create chat prompt
await langfuse.prompt.create({
    name: "chatbot",
    type: "chat",
    prompt: [
        { role: "system", content: "You are helpful chatbot" },
        { role: "user", content: "{{user_message}}" }
    ],
    labels: ["production"]
});
```

### 3. Prompt Versioning and Labeling

#### Automatic Versioning
Each prompt edit creates a new version with auto-incrementing version number.

#### Labels for Deployment
Special labels for environment and experiment management:

```python
# Fetch production version (default)
prompt = langfuse.get_prompt("customer_support")

# Fetch specific version
prompt_v1 = langfuse.get_prompt("customer_support", version=1)

# Fetch staging version
prompt_staging = langfuse.get_prompt("customer_support", label="staging")

# Fetch for A/B test
prompt_a = langfuse.get_prompt("customer_support", label="prod-a")
prompt_b = langfuse.get_prompt("customer_support", label="prod-b")
```

#### Built-in Labels
- **`production`**: Default version returned by SDKs
- **`latest`**: Most recently created version
- **Custom labels**: `staging`, `dev`, `tenant-1`, `prod-a`, `prod-b`, etc.

#### Label Management
```python
# In UI: Update > Versions tab
# Select version and assign/remove labels
# E.g., assign version 5 to "production"

# Protected Labels (v2.0+)
# Project admins can protect labels like "production"
# Prevents viewer/member roles from modifying
```

### 4. Prompt Deployment Workflow

```
Development → Testing → Staging → Production

1. Create new version in "dev" environment
2. Test with "staging" label
3. Validate performance on dataset
4. Deploy to "production" label
5. Monitor metrics
6. Rollback if needed (reassign production label)
```

### 5. Integration with LangChain

#### Python Example
```python
from langfuse import get_client
from langfuse.langchain import CallbackHandler
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

# Setup
langfuse = get_client()
langfuse_callback = CallbackHandler()

# Create prompt in Langfuse
langfuse.create_prompt(
    name="event-planner",
    prompt="Plan an event titled {{Event Name}}. {{Event Description}} "
           "will be held in {{Location}} on {{Date}}. "
           "Consider: audience, budget, venue, catering, entertainment. "
           "Provide detailed plan including vendors.",
    config={
        "model": "gpt-4o",
        "temperature": 0
    },
    labels=["production"]
)

# Fetch and convert to LangChain format
langfuse_prompt = langfuse.get_prompt("event-planner")

# Note: Langfuse uses {{variable}} but LangChain uses {variable}
langchain_prompt = ChatPromptTemplate.from_template(
    langfuse_prompt.get_langchain_prompt(),  # Converts {{ }} to { }
    metadata={"langfuse_prompt": langfuse_prompt}
)

# Build chain
model = langfuse_prompt.config["model"]
temp = langfuse_prompt.config["temperature"]
llm = ChatOpenAI(model=model, temperature=temp)
chain = langchain_prompt | llm

# Execute with tracing
response = chain.invoke(
    {
        "Event Name": "Wedding",
        "Event Description": "Celebrating union of Julia and Alex",
        "Location": "Central Park, NYC",
        "Date": "June 5, 2024"
    },
    config={"callbacks": [langfuse_callback]}
)
```

#### JavaScript/TypeScript Example
```typescript
import { LangfuseClient } from "@langfuse/client";
import { PromptTemplate } from "@langchain/core/prompts";

const langfuse = new LangfuseClient();

// Create prompt
await langfuse.prompt.create({
    name: "jokes",
    type: "text",
    prompt: "Tell me a joke about {{topic}}",
    labels: ["production"],
    config: {
        model: "gpt-4o",
        temperature: 0.7
    }
});

// Fetch and use
const prompt = await langfuse.prompt.get({
    name: "jokes",
    label: "production"
});

// Convert to LangChain format
const promptTemplate = PromptTemplate.fromTemplate(
    prompt.getLangchainPrompt()
).withConfig({
    metadata: { langfusePrompt: prompt }
});
```

### 6. A/B Testing Prompts in Production

```python
import random
from langfuse import get_client

langfuse = get_client()

def get_response(user_query):
    # Randomly select prompt version
    variant = random.choice(["prod-a", "prod-b"])
    
    # Fetch selected variant
    prompt = langfuse.get_prompt("customer_support", label=variant)
    
    # Use with LLM
    response = openai.chat.completions.create(
        model=prompt.config["model"],
        temperature=prompt.config["temperature"],
        messages=[
            {"role": "user", "content": prompt.compile(query=user_query)}
        ]
    )
    
    # Langfuse automatically tracks which prompt version was used
    # via the metadata linking in your tracing setup
    return response.choices[0].message.content
```

Performance metrics (latency, cost, quality) are automatically tracked per label.

### 7. Configuration Storage

Store model parameters alongside prompts:

```python
langfuse.create_prompt(
    name="analyzer",
    prompt="Analyze {{text}}",
    config={
        "model": "gpt-4o",
        "temperature": 0.2,
        "max_tokens": 1000,
        "top_p": 0.95,
        "tools": [
            {
                "type": "function",
                "function": {
                    "name": "extract_entities",
                    "description": "Extract entities from text"
                }
            }
        ]
    }
)

# Fetch and apply config
prompt = langfuse.get_prompt("analyzer")
llm_config = prompt.config
```

---

## ANALYTICS AND DASHBOARDS

### 1. Custom Dashboards

#### Creating Dashboards
```
UI: Dashboards > New Dashboard > Configure Query
- Select view (traces, observations, scores)
- Choose dimensions (model, user, feature, prompt_version)
- Select metrics (count, latency, cost, etc.)
- Apply filters and time granularity
```

#### Query Engine Features
- Multi-level aggregations across traces, observations, sessions, scores
- Complex filtering by metadata, timestamps, user properties
- Time granularity: hour, day, week, month

#### Visualization Types
- **Line charts**: Latency trends, cost over time
- **Bar charts**: Model comparison, user metrics
- **Time series**: Real-time monitoring
- **Pie charts**: Cost/usage distribution

#### Example Queries
```
Query 1: Average Latency by Model
View: observations
Dimensions: model
Metrics: avg(latency)
Filter: environment = "production"

Query 2: Cost Breakdown by User
View: observations
Dimensions: userId
Metrics: sum(cost)
Time: daily
Filter: tags.feature = "chat"

Query 3: Quality Score Trends
View: scores-numeric
Dimensions: prompt_version
Metrics: avg(value)
Filter: name = "user_rating"
```

### 2. Cost Tracking and Token Usage

#### Cost Tracking Methods

**Method 1: Automatic Calculation**
For supported models (OpenAI, Anthropic, Google):
```python
# When using integrations, cost is calculated automatically
response = openai.chat.completions.create(model="gpt-4", ...)
# Langfuse extracts token usage and calculates cost

# Token usage available via API
traces = langfuse.api.traces.list()
for trace in traces:
    for obs in trace.observations:
        if obs.model:
            print(f"Model: {obs.model}")
            print(f"Input tokens: {obs.usage.input_tokens}")
            print(f"Output tokens: {obs.usage.output_tokens}")
            print(f"Cost: ${obs.cost_usd}")
```

**Method 2: Custom Model Definitions**
```
UI: Project Settings > Models > + New Model

Configure:
- Model name (e.g., "my-custom-model")
- Tokenizer (regex pattern matching)
- Pricing per token type:
  - input_tokens: $0.001 per token
  - output_tokens: $0.002 per token
  - cached_tokens: $0.0001 per token
```

**Method 3: Manual Cost Ingestion**
```python
langfuse.create_observation(
    type="generation",
    name="llm_call",
    model="custom-model",
    usage={
        "input_tokens": 100,
        "output_tokens": 50,
        "cached_tokens": 10
    },
    cost_usd=0.00123  # Or provide usage and let cost calculate
)
```

#### Usage Types
Standard types:
- `input`, `output` (all models)
- `cached_tokens` (when using cache)
- `audio_tokens`, `image_tokens` (multimodal models)

Custom types:
```python
# Define custom usage types
usage = {
    "reasoning_tokens": 1000,  # For reasoning models
    "multimodal_tokens": 500,
    "api_calls": 5             # Custom metrics
}
```

#### Important: Reasoning Models (o1, etc.)
Cost inference cannot work without explicit token usage:
```python
# Must provide token usage for o1 models
langfuse.create_observation(
    type="generation",
    model="o1",
    usage={
        "input_tokens": 500,
        "output_tokens": 1500,
        "reasoning_tokens": 5000  # Explicitly required
    }
)

# With integrations (LangChain, LiteLLM), tokens collected automatically
```

### 3. Metrics API

#### Endpoint
`GET /api/public/metrics`

#### Basic Query Structure
```python
import requests

# Example: Daily cost by model
query = {
    "view": "observations",
    "dimensions": ["model"],
    "metrics": ["totalCost", "count"],
    "filters": [
        {
            "name": "timestamp",
            "operator": "gte",
            "value": "2024-01-01"
        }
    ],
    "timeGranularity": "day"
}

response = requests.get(
    "https://cloud.langfuse.com/api/public/metrics",
    json=query,
    headers={
        "Authorization": f"Bearer {API_KEY}"
    }
)

results = response.json()
# Returns aggregated metrics grouped by dimensions
```

#### Supported Views
- `traces`: Count, latency by trace name, user
- `observations`: Latency, token usage, cost by model, user
- `scores-numeric`: Average/percentile scores
- `scores-categorical`: Score counts by category

#### Aggregation Functions
- `count`, `sum`, `avg`, `min`, `max`
- Percentiles: `p50`, `p75`, `p90`, `p95`, `p99`

#### Example Queries

**Query 1: Token usage by model this month**
```json
{
  "view": "observations",
  "dimensions": ["model"],
  "metrics": ["totalTokens", "totalCost"],
  "filters": [{
    "name": "timestamp",
    "operator": "gte",
    "value": "2024-11-01"
  }]
}
```

**Query 2: Cost by user (last 7 days)**
```json
{
  "view": "observations",
  "dimensions": ["userId"],
  "metrics": ["totalCost", "count"],
  "filters": [{
    "name": "timestamp",
    "operator": "gte",
    "value": "2024-11-12"
  }],
  "timeGranularity": "day"
}
```

**Query 3: Latency percentiles by feature**
```json
{
  "view": "observations",
  "dimensions": ["tags.feature"],
  "metrics": ["p50(latency)", "p95(latency)", "p99(latency)"]
}
```

### 4. Daily Metrics API

For retrieving aggregated daily usage and cost metrics:

```python
# Request daily metrics
response = requests.get(
    "https://cloud.langfuse.com/api/public/daily-metrics",
    params={
        "from": "2024-11-01",
        "to": "2024-11-30",
        "groupBy": "userId"
    },
    headers={"Authorization": f"Bearer {API_KEY}"}
)

# Returns:
# {
#   "data": [
#     {
#       "date": "2024-11-01",
#       "userId": "user_123",
#       "usage": {
#         "input_tokens": 10000,
#         "output_tokens": 5000,
#         "requests": 25
#       },
#       "cost": 0.087
#     }
#   ]
# }
```

### 5. Data Relationships and Scoring

#### Trace-Observation Relationships
```
Trace (overall interaction)
├── Observation 1 (e.g., LLM generation)
│   └── Child observations
├── Observation 2 (e.g., database query)
└── Observation 3 (e.g., API call)

Scores can attach to any level:
- Trace-level: Overall session quality
- Observation-level: Specific step evaluation
```

#### Scoring Architecture
```python
# Trace-level score (evaluates entire interaction)
langfuse.create_score(
    name="user_satisfaction",
    value=4.5,
    trace_id="trace_123",
    data_type="NUMERIC"
)

# Observation-level score (evaluates specific step)
langfuse.create_score(
    name="output_quality",
    value=0.95,
    observation_id="obs_456",  # Specific LLM call
    trace_id="trace_123",
    data_type="NUMERIC"
)

# Session-level score (evaluates multiple traces)
langfuse.create_score(
    name="conversation_quality",
    value="excellent",
    session_id="session_789",
    data_type="CATEGORICAL"
)
```

#### Querying Related Data
```python
# Fetch trace with all observations and scores
trace = langfuse.api.traces.get("trace_123")

# Access nested data
for obs in trace.observations:
    print(f"Observation: {obs.name}")
    
    # Scores are linked via reference
    scores = langfuse.api.scores.list(
        observation_id=obs.id
    )
    for score in scores:
        print(f"  Score: {score.name} = {score.value}")
```

---

## CODE EXAMPLES

### Complete Evaluation Workflow Example

```python
from langfuse import Evaluation, get_client
from langfuse.openai import OpenAI
from langfuse.langchain import CallbackHandler

# Setup
langfuse = get_client()
callback_handler = CallbackHandler()

# 1. Create dataset
dataset = langfuse.create_dataset("qa-benchmark")
dataset.items.add(
    input={"question": "What is the capital of France?"},
    expected_output="Paris"
)
dataset.items.add(
    input={"question": "Who wrote Romeo and Juliet?"},
    expected_output="William Shakespeare"
)

# 2. Define evaluators
def accuracy_eval(*, input, output, expected_output, **kwargs):
    match = expected_output.lower() in output.lower()
    return Evaluation(
        name="accuracy",
        value=1.0 if match else 0.0,
        comment="Correct" if match else "Incorrect"
    )

def helpfulness_eval(*, output, **kwargs):
    # Could use LLM for nuanced evaluation
    score = 1.0 if len(output) > 50 else 0.5
    return Evaluation(
        name="helpfulness",
        value=score,
        comment=f"Response: {len(output)} chars"
    )

# 3. Create prompt
langfuse.create_prompt(
    name="qa-answerer",
    prompt="Answer this question: {{question}}",
    config={"model": "gpt-4o", "temperature": 0},
    labels=["production"]
)

# 4. Define task
def qa_task(*, item, **kwargs):
    prompt = langfuse.get_prompt("qa-answerer")
    response = OpenAI().chat.completions.create(
        model=prompt.config["model"],
        messages=[{
            "role": "user",
            "content": prompt.compile(question=item["input"]["question"])
        }]
    )
    return response.choices[0].message.content

# 5. Run experiment
result = langfuse.run_experiment(
    name="QA Model - GPT-4o",
    data=langfuse.get_dataset("qa-benchmark").items,
    task=qa_task,
    evaluators=[accuracy_eval, helpfulness_eval]
)

# 6. View results
print(result.format())

# 7. Query results programmatically
traces = langfuse.api.traces.list(name="QA Model - GPT-4o")
for trace in traces:
    scores = langfuse.api.scores.list(trace_id=trace.id)
    for score in scores:
        print(f"{trace.id}: {score.name} = {score.value}")
```

### Frontend User Feedback Collection

```typescript
// Browser SDK for collecting user feedback

import { Langfuse } from "@langfuse/web";

const langfuse = new Langfuse({
    publicKey: "pk-lf-...",
    baseUrl: "https://cloud.langfuse.com"
});

// Capture current trace ID from your application
const currentTraceId = localStorage.getItem("langfuse_trace_id");

// Rating widget
function createRatingWidget(traceId) {
    const container = document.getElementById("feedback");
    
    for (let i = 1; i <= 5; i++) {
        const button = document.createElement("button");
        button.textContent = "⭐".repeat(i);
        button.onclick = () => {
            // Send feedback to Langfuse
            langfuse.score({
                name: "user_rating",
                value: i,
                traceId: traceId,
                dataType: "NUMERIC"
            });
            
            // Notify user
            container.innerHTML = "Thanks for your feedback!";
        };
        container.appendChild(button);
    }
}

// Thumbs up/down widget
function createThumbsWidget(traceId) {
    const thumbsUp = document.getElementById("thumbs-up");
    const thumbsDown = document.getElementById("thumbs-down");
    
    thumbsUp.onclick = () => {
        langfuse.score({
            name: "user_thumbs",
            value: "thumbs_up",
            traceId: traceId,
            dataType: "CATEGORICAL"
        });
    };
    
    thumbsDown.onclick = () => {
        langfuse.score({
            name: "user_thumbs",
            value: "thumbs_down",
            traceId: traceId,
            dataType: "CATEGORICAL"
        });
    };
}

// Initialize on page load
createRatingWidget(currentTraceId);
createThumbsWidget(currentTraceId);
```

---

## BEST PRACTICES

### 1. Evaluation Strategy

#### Multi-Method Approach
Don't rely on a single evaluation technique:
- Use human annotation for baseline
- Deploy LLM-as-judge for scalability
- Collect user feedback for real-world signal
- Implement custom metrics for domain-specific needs

#### Offline vs Online Evaluation
```
Development:
- Build test dataset (20-50 representative samples)
- Run experiments with multiple evaluators
- Validate changes before production

Production:
- Monitor key quality metrics
- Collect user feedback
- Track performance trends
- Alert on regressions
```

### 2. Cost Optimization

#### Monitor Spending
```python
# Regular cost queries
query = {
    "view": "observations",
    "dimensions": ["model"],
    "metrics": ["totalCost", "count"],
    "timeGranularity": "day"
}

# Set up alerts if costs exceed budget
total_cost = sum(result["totalCost"] for result in daily_results)
if total_cost > daily_budget:
    send_alert(f"Daily cost ${total_cost} exceeds budget")
```

#### Optimize Model Selection
- Use cheaper models for simple tasks
- Reserve expensive models for complex reasoning
- Cache common prompts and responses
- Batch requests when possible

### 3. Prompt Management Best Practices

#### Version Control Discipline
1. Never edit production prompt directly
2. Create new version first
3. Test on "staging" label
4. Validate with dataset experiments
5. Deploy to "production" only after validation

#### Documentation
```python
# Include description in prompt config
langfuse.create_prompt(
    name="classifier",
    prompt="Classify: {{text}}",
    config={
        "model": "gpt-4o",
        "description": "Classify support tickets into categories",
        "changelog": "v2: Added instruction for multi-class output",
        "created": "2024-11-15"
    }
)
```

### 4. Team Collaboration

#### Annotation Queue Setup
```
1. Define score configs (standardize evaluation)
   - Quality: good/fair/poor
   - Relevance: 0-10 scale
   - Correctness: binary

2. Create annotation queues
   - Quality Review Queue
   - Safety Review Queue
   - User Experience Queue

3. Assign team members
4. Track progress and consensus
```

#### Role-Based Access
- **Admin/Owner**: Create configs, protect labels
- **Member**: Annotate, create datasets
- **Viewer**: Read dashboards, view scores

### 5. Monitoring and Alerting

#### Key Metrics to Monitor
- Quality scores (trends, drops)
- Response latency (p50, p95, p99)
- Error rates and failure modes
- Cost per transaction
- Token usage patterns

#### Dashboard Setup
```
Quality Dashboard:
- Average accuracy by model
- User satisfaction trends
- Error rate by feature

Performance Dashboard:
- Latency trends
- Token usage vs cost
- Model comparison

Cost Dashboard:
- Daily spend by model
- Cost per user/feature
- Budget tracking
```

### 6. Handling Edge Cases

#### Reasoning Models
Always provide explicit token counts for o1 models:
```python
langfuse.create_observation(
    model="o1",
    usage={
        "input_tokens": 500,
        "output_tokens": 1500,
        "reasoning_tokens": 10000  # Must explicit
    }
)
```

#### Multi-turn Conversations
```python
# Use sessions to group related traces
trace = langfuse.create_trace(
    name="customer_conversation",
    session_id="conversation_123",
    user_id="user_456"
)

# Score overall conversation, not individual turns
langfuse.create_score(
    name="conversation_quality",
    value=4.5,
    session_id="conversation_123"
)
```

---

## Summary and Key Takeaways

### Evaluation Framework
- **Langfuse Scores**: Flexible objects for any evaluation metric
- **Three Methods**: LLM-as-judge (scalable), Human annotation (accurate), Custom scoring (flexible)
- **Score Configs**: Standardize evaluation across teams
- **Datasets**: Curate test data for systematic evaluation

### Prompt Management
- **Version Control**: Track all changes with automatic versioning
- **Labels**: Deploy to different environments/experiments
- **Non-code Updates**: Modify prompts without redeploying
- **A/B Testing**: Compare prompt versions in production

### Analytics
- **Custom Dashboards**: Build dashboards with flexible queries
- **Cost Tracking**: Monitor spend by model, user, feature
- **Metrics API**: Export data for custom analytics
- **Performance Monitoring**: Track quality, latency, errors

### Recommended Workflow
1. Create datasets with representative inputs
2. Implement multi-method evaluation
3. Version prompts and manage via labels
4. Monitor quality metrics continuously
5. Iterate based on evaluation results
6. Scale successful approaches production-wide


---

# SDK Integrations

# Langfuse: Comprehensive SDK, Integrations, and API Patterns Research

## Table of Contents

1. [Python SDK](#python-sdk)
2. [JavaScript/TypeScript SDK](#javascripttypescript-sdk)
3. [Framework Integrations](#framework-integrations)
4. [API and OpenTelemetry](#api-and-opentelemetry)
5. [No-Code Platform Integrations](#no-code-platform-integrations)
6. [Best Practices](#best-practices)

---

## Python SDK

### Overview

The Langfuse Python SDK v3 is built on OpenTelemetry for robust observability and context propagation. The latest version (v3.10.1+) provides three instrumentation approaches:

1. **Decorator pattern** (`@observe`) - Simplest, automatic nesting
2. **Context managers** - Recommended for chunks of work, explicit control
3. **Manual observations** - Maximum control with explicit span lifecycle management

### Installation

```bash
pip install langfuse
```

### Basic Setup

```python
from langfuse import get_client, observe, propagate_attributes

# Initialize client (uses environment variables by default)
langfuse = get_client()

# Environment variables:
# LANGFUSE_SECRET_KEY="sk-lf-..."
# LANGFUSE_PUBLIC_KEY="pk-lf-..."
# LANGFUSE_BASE_URL="https://cloud.langfuse.com"  # EU region
# or LANGFUSE_BASE_URL="https://us.cloud.langfuse.com"  # US region

# Or explicit initialization:
langfuse = Langfuse(
    public_key="pk-lf-...",
    secret_key="sk-lf-...",
    base_url="https://cloud.langfuse.com"
)
```

### 1. Decorator Pattern (@observe)

The simplest approach for automatic tracing with minimal code changes.

#### Basic Function Tracing

```python
from langfuse import observe, get_client

@observe()
def process_user_query(query: str) -> str:
    """Automatically traced function"""
    result = some_processing(query)
    return result

@observe()
async def async_process(query: str) -> str:
    """Supports async functions"""
    result = await async_operation(query)
    return result

# Functions are automatically traced
result = process_user_query("What is AI?")
```

#### With Custom Names and Attributes

```python
from langfuse import observe

@observe(
    name="custom-span-name",
    as_type="chain"  # Can be: span, generation, retrieval, tool, agent
)
def complex_pipeline(input_data: dict) -> dict:
    # ... implementation ...
    return output

@observe(
    name="llm-generation",
    as_type="generation"
)
def call_llm(prompt: str, model: str = "gpt-4o") -> str:
    # ... LLM call ...
    return response
```

#### Propagating Attributes with Decorator

```python
from langfuse import observe, propagate_attributes

@observe()
def user_workflow(user_id: str, session_id: str, query: str):
    """Propagate user context to all child spans"""
    with propagate_attributes(
        user_id=user_id,
        session_id=session_id,
        tags=["production", "user-request"],
        metadata={"email": "user@example.com", "tier": "premium"},
        version="1.0.0"
    ):
        # All child observations inherit these attributes
        result = process_query(query)
        store_result(result)
    return result

# Usage
user_workflow(
    user_id="user_123",
    session_id="session_abc",
    query="What is machine learning?"
)
```

### 2. Context Managers

Recommended approach for explicit control and automatic nesting of observations.

#### Basic Context Manager Usage

```python
from langfuse import get_client

langfuse = get_client()

def example_with_context_managers():
    # Create a root span
    with langfuse.start_as_current_span(
        name="user-request-pipeline",
        input={"user_query": "Tell me about AI"},
    ) as root_span:
        # This span is now the active observation in the context
        
        # Child observations created within this block are automatically nested
        with langfuse.start_as_current_span(
            name="retrieve-context",
        ) as retrieval_span:
            documents = retrieve_documents("AI")
            retrieval_span.update(output={"doc_count": len(documents)})
        
        # Update root span with final output
        root_span.update(output={"status": "completed"})
```

#### Generation (LLM Call) Observations

```python
from langfuse import get_client
import openai

langfuse = get_client()

def llm_call_with_generation():
    with langfuse.start_as_current_generation(
        name="llm-response",
        model="gpt-4o",
        input={
            "messages": [
                {"role": "user", "content": "What is AI?"}
            ]
        }
    ) as generation:
        # Make LLM call
        response = openai.ChatCompletion.create(
            model="gpt-4o",
            messages=[{"role": "user", "content": "What is AI?"}]
        )
        
        # Update generation with LLM output
        generation.update(
            output=response.choices[0].message.content,
            usage={
                "input_tokens": response.usage.prompt_tokens,
                "output_tokens": response.usage.completion_tokens
            }
        )
```

#### Nested Context Managers

```python
from langfuse import get_client

langfuse = get_client()

def complex_retrieval_pipeline():
    with langfuse.start_as_current_span(
        name="rag-pipeline",
        input={"query": "Who won the World Cup in 2022?"}
    ) as pipeline:
        
        # Step 1: Retrieve documents
        with langfuse.start_as_current_span(
            name="vector-search",
            as_type="retrieval"
        ) as retrieval:
            docs = vector_db.search("World Cup 2022")
            retrieval.update(
                output={"document_count": len(docs), "documents": docs}
            )
        
        # Step 2: Call LLM with retrieved context
        with langfuse.start_as_current_generation(
            name="answer-generation",
            model="gpt-4o",
            input={"context": docs, "query": "Who won?"}
        ) as generation:
            answer = llm_generate(docs, "Who won?")
            generation.update(output=answer)
        
        # Final output
        pipeline.update(output={"answer": answer})
```

#### Observation Types

```python
from langfuse import get_client

langfuse = get_client()

# Generic span
with langfuse.start_as_current_observation(
    as_type="span",
    name="processing"
) as span:
    pass

# LLM generation
with langfuse.start_as_current_observation(
    as_type="generation",
    name="llm-call",
    model="gpt-4o"
) as gen:
    pass

# Retrieval operation
with langfuse.start_as_current_observation(
    as_type="retrieval",
    name="vector-search"
) as ret:
    pass

# Tool/Function call
with langfuse.start_as_current_observation(
    as_type="tool",
    name="calculator"
) as tool:
    pass

# Agent workflow
with langfuse.start_as_current_observation(
    as_type="agent",
    name="react-agent"
) as agent:
    pass
```

### 3. Manual Observations

For fine-grained control over observation lifecycle.

```python
from langfuse import get_client

langfuse = get_client()

# Manual span management
span = langfuse.start_span(
    name="manual-processing",
    input={"data": "input_value"}
)

try:
    # Do some work
    result = process_data()
    span.end(output=result)
except Exception as e:
    span.end(error=str(e))

# Manual generation
generation = langfuse.start_generation(
    name="llm-call",
    model="gpt-4o",
    input={"prompt": "Hello"}
)

try:
    response = openai.ChatCompletion.create(...)
    generation.end(output=response.content)
except Exception as e:
    generation.end(error=str(e))
```

### 4. Advanced Features

#### Batch Processing and Flushing

```python
from langfuse import get_client

langfuse = get_client()

# Configuration for batching
langfuse = Langfuse(
    public_key="pk-lf-...",
    secret_key="sk-lf-...",
    flush_at=100,  # Flush after 100 events
    flush_interval=10,  # Flush every 10 seconds
)

# Manual flush
langfuse.flush()

# For short-lived environments (serverless), use shutdown
langfuse.shutdown()  # Blocks until all events are flushed
```

#### Attribute Propagation

```python
from langfuse import observe, propagate_attributes, get_client

# Propagate attributes to all child observations
def workflow_with_propagation(user_id: str, session_id: str):
    with propagate_attributes(
        user_id=user_id,
        session_id=session_id,
        tags=["production", "batch-processing"],
        metadata={
            "region": "us-east-1",
            "tier": "premium",
            "experiment": "variant_a"
        },
        version="2.0.0"
    ):
        # All observations created here inherit these attributes
        langfuse = get_client()
        with langfuse.start_as_current_span("main-task") as span:
            subtask()
```

#### Error Handling and Logging

```python
from langfuse import observe, get_client

@observe(as_type="span")
def error_handling_example(data):
    langfuse = get_client()
    
    with langfuse.start_as_current_span("risky-operation") as span:
        try:
            result = risky_operation(data)
            span.update(output=result)
        except ValueError as e:
            # Span will record the error
            span.update(level="ERROR")
            raise
        except Exception as e:
            span.update(
                output={"error": str(e)},
                level="ERROR"
            )
            return None
```

#### Async Support

```python
from langfuse import observe, get_client

@observe()
async def async_pipeline(query: str):
    """Supports async/await"""
    langfuse = get_client()
    
    with langfuse.start_as_current_span("async-chain") as span:
        # Async operations
        results = await asyncio.gather(
            fetch_documents(query),
            fetch_embeddings(query)
        )
        span.update(output={"status": "gathered"})
        
        # Async LLM call
        with langfuse.start_as_current_generation(
            name="async-llm",
            model="gpt-4o"
        ) as gen:
            response = await openai.ChatCompletion.acreate(...)
            gen.update(output=response.content)
```

---

## JavaScript/TypeScript SDK

### Overview

The Langfuse TypeScript SDK v4 (GA in August 2025) is built on OpenTelemetry v2, providing robust observability for JavaScript/Node.js applications. Three main instrumentation approaches:

1. **`startActiveObservation`** - Callback-based with automatic lifecycle management
2. **`observe`** - Decorator/wrapper pattern for existing functions
3. **`startObservation`** - Manual control with `endObservation`

### Installation

```bash
npm install @langfuse/tracing
# or
pnpm add @langfuse/tracing
```

### Basic Setup

```typescript
import { LangfuseSpanProcessor } from "@langfuse/otel";
import { NodeTracerProvider } from "@opentelemetry/sdk-node";
import { getNodeAutoInstrumentations } from "@opentelemetry/auto-instrumentations-node";

// Initialize tracer provider
const provider = new NodeTracerProvider({
  resource: new Resource({
    "service.name": "my-app",
  }),
});

// Add Langfuse exporter
provider.addSpanProcessor(
  new LangfuseSpanProcessor({
    publicKey: process.env.LANGFUSE_PUBLIC_KEY,
    secretKey: process.env.LANGFUSE_SECRET_KEY,
    baseUrl: process.env.LANGFUSE_BASE_URL,
  })
);

provider.register();

// Environment variables:
// LANGFUSE_PUBLIC_KEY="pk-lf-..."
// LANGFUSE_SECRET_KEY="sk-lf-..."
// LANGFUSE_BASE_URL="https://cloud.langfuse.com"
```

### 1. startActiveObservation (Recommended)

The recommended approach with automatic lifecycle management and context handling.

#### Basic Usage

```typescript
import { startActiveObservation } from "@langfuse/tracing";

await startActiveObservation(
  "my-first-trace",
  async (span) => {
    span.update({
      input: "Hello, Langfuse!",
      output: "This is my first trace!"
    });
  }
);
```

#### With Nested Observations

```typescript
import { startActiveObservation } from "@langfuse/tracing";

async function retrieverQAPipeline(query: string) {
  await startActiveObservation(
    "rag-pipeline",
    async (pipelineSpan) => {
      pipelineSpan.update({
        input: { query },
        metadata: { type: "rag" }
      });

      // Nested retrieval
      await startActiveObservation(
        "vector-search",
        async (retrievalSpan) => {
          const documents = await vectorStore.search(query);
          retrievalSpan.update({
            output: {
              documentCount: documents.length,
              documents
            },
            metadata: { vectorDb: "pinecone" }
          });
        },
        { asType: "retrieval" }
      );

      // Nested generation
      await startActiveObservation(
        "llm-generation",
        async (generationSpan) => {
          const response = await openai.chat.completions.create({
            model: "gpt-4o",
            messages: [
              {
                role: "system",
                content: `You are a helpful assistant. Use the following context: ${documents}`
              },
              { role: "user", content: query }
            ]
          });

          generationSpan.update({
            output: response.choices[0].message.content,
            usage: {
              inputTokens: response.usage?.prompt_tokens,
              outputTokens: response.usage?.completion_tokens
            }
          });
        },
        { asType: "generation", model: "gpt-4o" }
      );

      pipelineSpan.update({ output: { status: "completed" } });
    },
    { asType: "agent" }
  );
}
```

#### Observation Types

```typescript
import { startActiveObservation } from "@langfuse/tracing";

// Generic span
await startActiveObservation(
  "processing",
  async (span) => {
    // implementation
  },
  { asType: "span" }
);

// LLM generation
await startActiveObservation(
  "llm-call",
  async (gen) => {
    // implementation
  },
  { asType: "generation", model: "gpt-4o" }
);

// Retrieval
await startActiveObservation(
  "vector-search",
  async (ret) => {
    // implementation
  },
  { asType: "retrieval" }
);

// Tool/Function
await startActiveObservation(
  "calculator",
  async (tool) => {
    // implementation
  },
  { asType: "tool" }
);

// Agent
await startActiveObservation(
  "agent-workflow",
  async (agent) => {
    // implementation
  },
  { asType: "agent" }
);
```

### 2. observe (Decorator Pattern)

Wrap existing functions without modifying their logic.

#### Basic Function Wrapping

```typescript
import { observe } from "@langfuse/tracing";

const tracedFunction = observe(async (source: string) => {
  return { data: `some data from ${source}` };
});

const result = await tracedFunction("API");
```

#### With Custom Options

```typescript
import { observe, updateActiveObservation } from "@langfuse/tracing";

const llmCall = observe(
  async (prompt: string) => {
    updateActiveObservation({
      metadata: { model: "gpt-4o", temperature: 0.7 }
    });

    const response = await openai.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: prompt }]
    });

    return response.choices[0].message.content;
  },
  {
    name: "llm-generation",
    asType: "generation"
  }
);

const answer = await llmCall("What is AI?");
```

#### Wrapping Methods

```typescript
import { observe } from "@langfuse/tracing";

class DataProcessor {
  private vectorStore: VectorStore;

  retrieveDocuments = observe(
    async (query: string) => {
      return this.vectorStore.search(query);
    },
    { name: "vector-search", asType: "retrieval" }
  );

  generateResponse = observe(
    async (context: string, query: string) => {
      const response = await openai.chat.completions.create({
        model: "gpt-4o",
        messages: [
          { role: "system", content: `Context: ${context}` },
          { role: "user", content: query }
        ]
      });
      return response.choices[0].message.content;
    },
    { name: "response-generation", asType: "generation" }
  );
}
```

### 3. Manual Control (startObservation)

For fine-grained control over observation lifecycle.

```typescript
import { startObservation, endObservation } from "@langfuse/tracing";

// Manual span management
const spanId = startObservation({
  name: "manual-processing",
  input: { data: "input_value" }
});

try {
  const result = await processData();
  endObservation(spanId, {
    output: result
  });
} catch (error) {
  endObservation(spanId, {
    output: { error: String(error) }
  });
}
```

### 4. Attribute Propagation

```typescript
import {
  startActiveObservation,
  propagateAttributes
} from "@langfuse/tracing";

await startActiveObservation(
  "user-workflow",
  async (span) => {
    await propagateAttributes(
      {
        userId: "user_123",
        sessionId: "session_abc",
        tags: ["production", "premium-tier"],
        metadata: {
          email: "user@example.com",
          region: "us-east-1",
          experiment: "variant_a"
        },
        version: "1.0.0"
      },
      async () => {
        // All observations created here inherit these attributes
        // including nested startActiveObservation calls
        await processUserRequest();
      }
    );
  }
);
```

### 5. Serverless / Short-Lived Environments

Critical for Vercel, AWS Lambda, and other serverless platforms.

#### Vercel/Next.js with waitUntil

```typescript
import { startActiveObservation } from "@langfuse/tracing";
import { waitUntil } from "@vercel/functions";

export async function POST(request: Request) {
  const promise = startActiveObservation(
    "api-request",
    async (span) => {
      span.update({
        input: await request.json()
      });

      const result = await processRequest();
      span.update({ output: result });

      return result;
    }
  );

  waitUntil(promise);
  return new Response(JSON.stringify(await promise));
}
```

#### AWS Lambda with Flushing

```typescript
import { startActiveObservation } from "@langfuse/tracing";
import { langfuseSpanProcessor } from "@langfuse/otel";

export async function handler(event: any, context: any) {
  try {
    await startActiveObservation(
      "lambda-execution",
      async (span) => {
        span.update({
          input: event,
          metadata: { functionName: context.functionName }
        });

        const result = await processEvent(event);
        span.update({ output: result });

        return result;
      }
    );
  } finally {
    // Critical: Force flush before Lambda returns
    await langfuseSpanProcessor.forceFlush();
  }
}
```

### 6. Error Handling

```typescript
import { startActiveObservation } from "@langfuse/tracing";

async function processWithErrorHandling() {
  try {
    await startActiveObservation(
      "risky-operation",
      async (span) => {
        try {
          const result = await riskyOperation();
          span.update({ output: result });
        } catch (error) {
          span.update({
            output: { error: String(error) },
            level: "ERROR"
          });
          throw error;
        }
      }
    );
  } catch (error) {
    console.error("Failed:", error);
  }
}
```

---

## Framework Integrations

### LangChain Integration

#### Python LangChain

##### Setup

```bash
pip install langfuse langchain langchain_openai langchain_community
```

##### Basic Usage

```python
from langfuse.langchain import CallbackHandler
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# Initialize callback handler
langfuse_handler = CallbackHandler()

# Create chain
prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant."),
    ("user", "{input}")
])

model = ChatOpenAI(model="gpt-4o")
chain = prompt | model | StrOutputParser()

# Single invocation
result = chain.invoke(
    {"input": "What is AI?"},
    config={"callbacks": [langfuse_handler]}
)

# Batch processing
results = chain.batch(
    [
        {"input": "What is AI?"},
        {"input": "What is ML?"},
        {"input": "What is DL?"}
    ],
    config={"callbacks": [langfuse_handler]}
)

# Async support
async_result = await chain.ainvoke(
    {"input": "What is AI?"},
    config={"callbacks": [langfuse_handler]}
)

# Streaming
for chunk in chain.stream(
    {"input": "What is AI?"},
    config={"callbacks": [langfuse_handler]}
):
    print(chunk, end="", flush=True)
```

##### With Distributed Tracing

```python
from langfuse.langchain import CallbackHandler
from langfuse import get_client

langfuse_handler = CallbackHandler(
    trace_name="my-custom-trace",
    session_id="session_123",
    user_id="user_456",
    tags=["production"],
    metadata={"version": "1.0"}
)

result = chain.invoke(
    {"input": "Query"},
    config={"callbacks": [langfuse_handler]}
)
```

##### RetrievalQA Example

```python
from langchain_community.document_loaders import WebBaseLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import FAISS
from langchain.chains import RetrievalQA
from langchain_openai import ChatOpenAI
from langfuse.langchain import CallbackHandler

# Setup
loader = WebBaseLoader("https://example.com")
docs = loader.load()

splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
split_docs = splitter.split_documents(docs)

embeddings = OpenAIEmbeddings()
vectorstore = FAISS.from_documents(split_docs, embeddings)

# Chain
llm = ChatOpenAI(model="gpt-4o")
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    chain_type="stuff",
    retriever=vectorstore.as_retriever()
)

# With tracing
langfuse_handler = CallbackHandler()
result = qa_chain.run(
    "What is the main topic?",
    callbacks=[langfuse_handler]
)
```

#### JavaScript/TypeScript LangChain

```typescript
import { ChatOpenAI } from "@langchain/openai";
import { ChatPromptTemplate } from "@langchain/core/prompts";
import { StringOutputParser } from "@langchain/core/output_parsers";
import { startActiveObservation } from "@langfuse/tracing";

const llm = new ChatOpenAI({ modelName: "gpt-4o" });

const prompt = ChatPromptTemplate.fromMessages([
  ["system", "You are a helpful assistant."],
  ["user", "{input}"],
]);

const chain = prompt.pipe(llm).pipe(new StringOutputParser());

// Traced invocation
await startActiveObservation(
  "langchain-query",
  async (span) => {
    const result = await chain.invoke({
      input: "What is AI?"
    });

    span.update({
      output: result,
      metadata: { chainType: "langchain" }
    });
  }
);
```

### LlamaIndex Integration

#### Python LlamaIndex

##### Via Callback Handler

```python
from llama_index.core import Settings
from llama_index.core.callbacks import CallbackManager
from langfuse.llama_index import LlamaIndexCallbackHandler
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader

# Setup
langfuse_callback = LlamaIndexCallbackHandler()
Settings.callback_manager = CallbackManager([langfuse_callback])

# Load and index
documents = SimpleDirectoryReader("./data").load_data()
index = VectorStoreIndex.from_documents(documents)

# Query
query_engine = index.as_query_engine()
response = query_engine.query("What is the main topic?")
```

##### Via Instrumentation Module (Beta)

```python
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor
from openinference.instrumentation.llama_index import LlamaIndexInstrumentor
import base64
import os

# Setup OTEL
LANGFUSE_AUTH = base64.b64encode(
    f"{os.getenv('LANGFUSE_PUBLIC_KEY')}:{os.getenv('LANGFUSE_SECRET_KEY')}".encode()
).decode()

os.environ["OTEL_EXPORTER_OTLP_ENDPOINT"] = "https://cloud.langfuse.com/api/public/otel"
os.environ["OTEL_EXPORTER_OTLP_HEADERS"] = f"Authorization=Basic {LANGFUSE_AUTH}"

tracer_provider = TracerProvider()
tracer_provider.add_span_processor(SimpleSpanProcessor(OTLPSpanExporter()))

# Instrument LlamaIndex
LlamaIndexInstrumentor().instrument(tracer_provider=tracer_provider)

# All LlamaIndex operations are now traced
```

### OpenAI SDK Integration

#### Python OpenAI

```python
from langfuse.openai import OpenAI as LangfuseOpenAI

# Direct drop-in replacement
client = LangfuseOpenAI(
    api_key="sk-...",
    public_key="pk-lf-...",
    secret_key="sk-lf-..."
)

# All OpenAI calls are automatically traced
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello"}]
)

# With session context
client = LangfuseOpenAI(
    api_key="sk-...",
    public_key="pk-lf-...",
    secret_key="sk-lf-...",
    langfuse_session_id="session_123",
    langfuse_user_id="user_456",
    langfuse_tags=["production"],
    langfuse_metadata={"version": "1.0"}
)
```

#### TypeScript OpenAI

```typescript
import { observeOpenAI } from "@langfuse/openai";
import OpenAI from "openai";
import { startActiveObservation } from "@langfuse/tracing";

const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

await startActiveObservation(
  "user-request",
  async (span) => {
    // Wrap OpenAI client with Langfuse observation
    const tracedClient = observeOpenAI(client, {
      parent: span,
      generationName: "chat-completion"
    });

    const response = await tracedClient.chat.completions.create({
      model: "gpt-4o",
      messages: [{ role: "user", content: "What is AI?" }]
    });

    span.update({
      output: response.choices[0].message.content
    });
  }
);
```

### Anthropic Integration

#### Python Anthropic (via OpenTelemetry)

```python
from anthropic import Anthropic
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor
from opentelemetry.instrumentation.anthropic import AnthropicInstrumentor
import base64
import os

# Setup OpenTelemetry
LANGFUSE_AUTH = base64.b64encode(
    f"{os.getenv('LANGFUSE_PUBLIC_KEY')}:{os.getenv('LANGFUSE_SECRET_KEY')}".encode()
).decode()

os.environ["OTEL_EXPORTER_OTLP_ENDPOINT"] = "https://cloud.langfuse.com/api/public/otel"
os.environ["OTEL_EXPORTER_OTLP_HEADERS"] = f"Authorization=Basic {LANGFUSE_AUTH}"

tracer_provider = TracerProvider()
tracer_provider.add_span_processor(SimpleSpanProcessor(OTLPSpanExporter()))

# Instrument Anthropic
AnthropicInstrumentor().instrument(tracer_provider=tracer_provider)

# All Anthropic calls are now traced
client = Anthropic(api_key="sk-ant-...")
response = client.messages.create(
    model="claude-3-5-sonnet-20241022",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello"}]
)
```

### Instructor Integration (Structured Outputs)

```python
from langfuse.openai import OpenAI as LangfuseOpenAI
import instructor
from pydantic import BaseModel, Field

class User(BaseModel):
    name: str = Field(description="The user's name")
    age: int = Field(description="The user's age")
    email: str = Field(description="The user's email")

# Setup Langfuse OpenAI client
client = LangfuseOpenAI(
    api_key="sk-...",
    public_key="pk-lf-...",
    secret_key="sk-lf-..."
)

# Patch with instructor
patched_client = instructor.from_openai(client)

# Structured output is automatically traced
user = patched_client.chat.completions.create(
    model="gpt-4o",
    response_model=User,
    messages=[
        {"role": "user", "content": "Extract user info from: John is 30 years old and his email is john@example.com"}
    ]
)

print(user.name)  # "John"
print(user.age)   # 30
```

### Vercel AI SDK Integration

#### Next.js Integration

```typescript
import { generateText } from "ai";
import { openai } from "@ai-sdk/openai";
import { startActiveObservation } from "@langfuse/tracing";

export async function POST(request: Request) {
  try {
    const { messages } = await request.json();

    const result = await startActiveObservation(
      "ai-generation",
      async (span) => {
        const { text, usage } = await generateText({
          model: openai("gpt-4o"),
          system: "You are a helpful assistant.",
          messages,
          experimental_telemetry: {
            isEnabled: true
          }
        });

        span.update({
          output: text,
          usage: {
            inputTokens: usage.promptTokens,
            outputTokens: usage.completionTokens
          }
        });

        return text;
      }
    );

    return new Response(JSON.stringify({ text: result }));
  } catch (error) {
    console.error("Error:", error);
    return new Response("Error", { status: 500 });
  }
}
```

### LiteLLM Integration

#### Via LiteLLM Proxy

```yaml
# litellm_config.yaml
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: openai/gpt-4o
      api_key: sk-...

litellm_settings:
  callbacks: ["langfuse_otel"]
  database_url: "postgresql://user:pass@localhost/litellm"
  drop_params: true
  allow_duplicate_keys: false
```

Environment setup:
```bash
export LANGFUSE_PUBLIC_KEY="pk-lf-..."
export LANGFUSE_SECRET_KEY="sk-lf-..."
export OTEL_EXPORTER_OTLP_ENDPOINT="https://cloud.langfuse.com/api/public/otel"
```

Launch proxy:
```bash
litellm --config /path/to/litellm_config.yaml
```

#### Python LiteLLM SDK

```python
from litellm import completion
import os

os.environ["LANGFUSE_PUBLIC_KEY"] = "pk-lf-..."
os.environ["LANGFUSE_SECRET_KEY"] = "sk-lf-..."

# All calls automatically traced via LiteLLM's callback system
response = completion(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello"}],
    langfuse_metadata={
        "user_id": "user_123",
        "session_id": "session_abc",
        "tags": ["production"]
    }
)
```

### AWS Bedrock Integration

#### Python with @observe Decorator

```python
from langfuse import observe
import boto3
import json

bedrock = boto3.client('bedrock-runtime', region_name='us-east-1')

@observe(as_type="generation")
def call_bedrock_claude(prompt: str) -> str:
    """Call AWS Bedrock Claude model with tracing"""
    response = bedrock.invoke_model(
        modelId="anthropic.claude-3-5-sonnet-20241022-v2:0",
        body=json.dumps({
            "anthropic_version": "bedrock-2023-06-01",
            "max_tokens": 1024,
            "messages": [
                {
                    "role": "user",
                    "content": prompt
                }
            ]
        })
    )

    result = json.loads(response['body'].read())
    return result['content'][0]['text']

# Usage
answer = call_bedrock_claude("What is AWS Bedrock?")
```

#### With Context Manager

```python
from langfuse import get_client
import boto3
import json

langfuse = get_client()
bedrock = boto3.client('bedrock-runtime', region_name='us-east-1')

def bedrock_rag_pipeline(query: str):
    with langfuse.start_as_current_span(
        name="bedrock-rag",
        input={"query": query}
    ) as pipeline:
        
        # Retrieve documents
        with langfuse.start_as_current_span(
            name="retrieval",
            as_type="retrieval"
        ) as retrieval:
            documents = vector_store.search(query)
            retrieval.update(output={"doc_count": len(documents)})
        
        # Call Bedrock
        with langfuse.start_as_current_generation(
            name="bedrock-claude",
            model="claude-3-5-sonnet"
        ) as generation:
            context = "\n".join([doc.page_content for doc in documents])
            
            response = bedrock.invoke_model(
                modelId="anthropic.claude-3-5-sonnet-20241022-v2:0",
                body=json.dumps({
                    "anthropic_version": "bedrock-2023-06-01",
                    "max_tokens": 1024,
                    "messages": [
                        {
                            "role": "user",
                            "content": f"Context:\n{context}\n\nQuestion: {query}"
                        }
                    ]
                })
            )
            
            result = json.loads(response['body'].read())
            answer = result['content'][0]['text']
            generation.update(output=answer)
        
        pipeline.update(output={"answer": answer})
```

---

## API and OpenTelemetry

### REST API Overview

Langfuse provides a public REST API for direct integration without SDKs.

#### Authentication

Uses HTTP Basic Auth with Langfuse API keys:

```bash
# Format
Authorization: Basic <base64(public_key:secret_key)>

# Example with curl
curl -u pk-lf-...:sk-lf-... https://cloud.langfuse.com/api/public/projects

# Python
import requests
from requests.auth import HTTPBasicAuth

auth = HTTPBasicAuth("pk-lf-...", "sk-lf-...")
response = requests.get(
    "https://cloud.langfuse.com/api/public/projects",
    auth=auth
)
```

#### Base URLs

- **EU Region**: `https://cloud.langfuse.com`
- **US Region**: `https://us.cloud.langfuse.com`
- **HIPAA Region**: `https://hipaa.cloud.langfuse.com`
- **Local**: `http://localhost:3000`

#### Key Endpoints

```
GET    /api/public/projects                 - List projects
POST   /api/public/ingestion                - Ingest observations (batch)
POST   /api/public/ingestion/event          - Single event ingestion
GET    /api/public/traces/{traceId}         - Get trace details
GET    /api/public/observations/{obsId}     - Get observation details
GET    /api/public/sessions/{sessionId}     - Get session details
```

#### Batch Ingestion

```python
import requests
from requests.auth import HTTPBasicAuth
import json

auth = HTTPBasicAuth("pk-lf-...", "sk-lf-...")

# Batch multiple events
events = [
    {
        "id": "trace-1",
        "type": "trace-create",
        "timestamp": "2025-01-01T12:00:00Z",
        "body": {
            "id": "trace-1",
            "userId": "user_123",
            "sessionId": "session_abc",
            "metadata": {"version": "1.0"}
        }
    },
    {
        "id": "span-1",
        "type": "observation-create",
        "timestamp": "2025-01-01T12:00:00Z",
        "body": {
            "id": "span-1",
            "traceId": "trace-1",
            "type": "span",
            "name": "processing",
            "input": {"data": "test"},
            "output": {"result": "done"},
            "startTime": "2025-01-01T12:00:00Z",
            "endTime": "2025-01-01T12:00:01Z"
        }
    }
]

response = requests.post(
    "https://cloud.langfuse.com/api/public/ingestion",
    auth=auth,
    json={"batch": events}
)

print(response.status_code)  # 207 (partial success possible)
print(response.json())
```

### OpenTelemetry Integration

#### OTEL Endpoint

Langfuse accepts OpenTelemetry Protocol (OTLP) traces at the `/api/public/otel` endpoint.

#### Python OTEL Setup

```python
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
import base64
import os

# Setup auth
LANGFUSE_AUTH = base64.b64encode(
    f"{os.getenv('LANGFUSE_PUBLIC_KEY')}:{os.getenv('LANGFUSE_SECRET_KEY')}".encode()
).decode()

# Configure exporter
exporter = OTLPSpanExporter(
    endpoint="https://cloud.langfuse.com/api/public/otel",
    headers=(("Authorization", f"Basic {LANGFUSE_AUTH}"),)
)

# Setup tracer
tracer_provider = TracerProvider()
tracer_provider.add_span_processor(SimpleSpanProcessor(exporter))

# Use tracer
tracer = tracer_provider.get_tracer(__name__)
with tracer.start_as_current_span("my-span") as span:
    span.set_attribute("custom.attribute", "value")
```

#### Langfuse-Specific OTEL Attributes

```python
from opentelemetry import trace

tracer = trace.get_tracer(__name__)

with tracer.start_as_current_span("llm-call") as span:
    # Langfuse-specific attributes
    span.set_attribute("langfuse.observation.type", "generation")
    span.set_attribute("langfuse.observation.model", "gpt-4o")
    span.set_attribute("langfuse.observation.input", '{"prompt": "Hello"}')
    span.set_attribute("langfuse.observation.output", '{"response": "Hi"}')
    span.set_attribute("langfuse.trace.user_id", "user_123")
    span.set_attribute("langfuse.trace.session_id", "session_abc")
    span.set_attribute("langfuse.trace.tags", ["production"])
    span.set_attribute("langfuse.trace.metadata", '{"version": "1.0"}')
```

#### OpenLIT Integration

```python
import openlit
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor
from opentelemetry import trace
import base64
import os

# Setup OTEL for Langfuse
LANGFUSE_AUTH = base64.b64encode(
    f"{os.getenv('LANGFUSE_PUBLIC_KEY')}:{os.getenv('LANGFUSE_SECRET_KEY')}".encode()
).decode()

os.environ["OTEL_EXPORTER_OTLP_ENDPOINT"] = "https://cloud.langfuse.com/api/public/otel"
os.environ["OTEL_EXPORTER_OTLP_HEADERS"] = f"Authorization=Basic {LANGFUSE_AUTH}"

tracer_provider = TracerProvider()
tracer_provider.add_span_processor(SimpleSpanProcessor(OTLPSpanExporter()))
trace.set_tracer_provider(tracer_provider)

# Initialize OpenLIT instrumentation
openlit.init(disable_batch=True)

# All instrumented libraries now send traces to Langfuse
```

---

## No-Code Platform Integrations

### Dify Integration

```yaml
# In Dify app settings:
# Navigate to Monitoring > Third-party LLMOps
# Provider: Langfuse
# Endpoint: https://cloud.langfuse.com
# Public Key: pk-lf-...
# Secret Key: sk-lf-...
```

Benefits:
- Automatic tracing of all Dify workflows
- Session/chat history tracking
- User interaction analytics
- Cost and token monitoring
- A/B testing and evaluation

### Flowise Integration

```json
// In Flowise Credentials:
{
  "name": "Langfuse",
  "type": "langfuse",
  "publicKey": "pk-lf-...",
  "secretKey": "sk-lf-...",
  "baseURL": "https://cloud.langfuse.com"
}
```

### Langflow Integration

```bash
# Start Langflow with Langfuse environment variables
export LANGFUSE_PUBLIC_KEY="pk-lf-..."
export LANGFUSE_SECRET_KEY="sk-lf-..."
export LANGFUSE_BASE_URL="https://cloud.langfuse.com"

python -m langflow run
```

---

## Best Practices

### 1. Batching and Flushing

```python
# Production setup with optimized batching
langfuse = Langfuse(
    public_key="pk-lf-...",
    secret_key="sk-lf-...",
    flush_at=100,       # Flush after 100 events
    flush_interval=30,  # Or every 30 seconds
)

# In FastAPI/Flask shutdown
@app.on_event("shutdown")
def shutdown():
    langfuse.shutdown()  # Blocks until all flushed
```

### 2. Error Handling

```python
from langfuse import observe, get_client

@observe()
def robust_function(data):
    langfuse = get_client()
    
    try:
        result = process(data)
        return result
    except ValueError as e:
        langfuse.update_current_trace(tags=["error"])
        raise
    finally:
        langfuse.flush()  # In short-lived apps
```

### 3. Session Management

```python
from langfuse import propagate_attributes

def user_conversation(user_id: str, session_id: str):
    """Track multi-turn conversations"""
    with propagate_attributes(
        user_id=user_id,
        session_id=session_id,  # Same for all turns
        metadata={"conversation_type": "support"}
    ):
        # Multiple interactions within same session
        response1 = chat_turn("Hello")
        response2 = chat_turn("Tell me more")
```

### 4. Cost Tracking (Token Usage)

```python
from langfuse import get_client

@observe(as_type="generation")
def call_llm(prompt: str):
    response = openai.ChatCompletion.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}]
    )
    
    langfuse = get_client()
    langfuse.update_current_trace(
        metadata={
            "cost": calculate_cost(
                response.usage.prompt_tokens,
                response.usage.completion_tokens,
                "gpt-4o"
            )
        }
    )
    
    return response.choices[0].message.content
```

### 5. Metadata Organization

```python
# Use structured metadata for filtering/analytics
with propagate_attributes(
    metadata={
        "request_id": uuid.uuid4().hex,
        "user_tier": "premium",
        "region": "us-east-1",
        "feature": "recommendation_engine",
        "experiment": "variant_b",
        "deployment": "production"
    }
):
    process_request()
```

### 6. Serverless Best Practices

```typescript
// TypeScript in Next.js API route
import { startActiveObservation } from "@langfuse/tracing";
import { langfuseSpanProcessor } from "@langfuse/otel";

export async function POST(request: Request) {
  try {
    const result = await startActiveObservation(
      "api-call",
      async (span) => {
        const data = await request.json();
        span.update({ input: data });

        const output = await process(data);
        span.update({ output });

        return output;
      }
    );

    return Response.json(result);
  } finally {
    // CRITICAL: Flush before function exits
    await langfuseSpanProcessor.forceFlush();
  }
}
```

---

## Summary Table

| Feature | Python SDK | TypeScript SDK | LangChain | OpenAI | LiteLLM | Bedrock |
|---------|-----------|----------------|-----------|--------|---------|---------|
| Decorators | ✓ @observe | ✓ observe() | ✓ Callback | - | ✓ Auto | ✓ @observe |
| Context Managers | ✓ start_as_current | ✓ startActiveObservation | - | - | - | - |
| Async Support | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Batch Processing | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| Session Tracking | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Error Tracking | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Nested Spans | ✓ OTel | ✓ OTel | ✓ | ✓ | ✓ | ✓ |
| Token Usage | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

---

## Resources

- **Official Docs**: https://langfuse.com/docs
- **Python SDK**: https://github.com/langfuse/langfuse-python
- **TypeScript SDK**: https://github.com/langfuse/langfuse-js
- **API Reference**: https://api.reference.langfuse.com/
- **Cookbook Examples**: https://langfuse.com/guides

