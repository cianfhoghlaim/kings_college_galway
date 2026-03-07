# Dual-Engine Graph + Complementary LLM Serving Integration

## Overview

This document describes two key architectural patterns derived from project-context research:

1. **Dual-Engine Graph Strategy**: FalkorDB (Graphiti) for dynamic memory + Memgraph for static knowledge
2. **Complementary LLM Serving**: llama-swap for VRAM management + LiteLLM for unified routing

## Part 1: Dual-Engine Graph Architecture

### Problem Statement

AI memory systems require both:
- **Static knowledge**: Validated facts, domain ontology, reference data (requires ACID guarantees)
- **Dynamic memory**: Session context, user interactions, episodic memories (requires high write velocity)

A single graph database struggles to optimize for both workloads simultaneously.

### Solution: Dual-Engine Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                   AI Memory Layer                            │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────┐   ┌───────────────────────────┐  │
│  │ Static Knowledge      │   │ Dynamic Memory            │  │
│  │ (Memgraph)            │   │ (FalkorDB via Graphiti)   │  │
│  │                       │   │                           │  │
│  │ - Domain ontology     │   │ - Session context         │  │
│  │ - Validated facts     │   │ - User interactions       │  │
│  │ - Reference data      │   │ - Episodic memories       │  │
│  │ - ACID guaranteed     │   │ - High write velocity     │  │
│  │ - CocoIndex managed   │   │ - Graphiti managed        │  │
│  └───────────────────────┘   └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Engine Selection Matrix

| Use Case | Memgraph (Static) | FalkorDB (Dynamic) |
|----------|-------------------|-------------------|
| Domain knowledge | ✅ | |
| Conversation history | | ✅ |
| Validated facts | ✅ | |
| Learning from interactions | | ✅ |
| Multi-hop reasoning | ✅ | |
| Session-specific context | | ✅ |
| ACID critical operations | ✅ | |
| High-velocity writes | | ✅ |

### Implementation with Cognee

```python
import cognee
from graphiti_core import Graphiti
from graphiti_core.graph import FalkorDBDriver

# Configure static knowledge (Memgraph via Cognee)
await cognee.config.set_graph_database_provider("memgraph")
await cognee.config.set_graph_database_url("bolt://memgraph:7687")

# Configure dynamic memory (FalkorDB via Graphiti)
dynamic_memory = Graphiti(
    driver=FalkorDBDriver(host="falkordb", port=6379)
)

# Query routing function
async def intelligent_search(query: str, session_id: str = None):
    # Always query static knowledge
    static_results = await cognee.search(
        query_text=query,
        query_type=SearchType.GRAPH_COMPLETION
    )

    # Add session context if available
    if session_id:
        dynamic_results = await dynamic_memory.search(
            query=query,
            center_node_uuid=session_id
        )
        return merge_results(static_results, dynamic_results)

    return static_results
```

### Data Promotion Pattern

Validated insights should be promoted from dynamic to static storage:

```python
async def promote_to_static(insight_id: str):
    """Move validated insight from FalkorDB to Memgraph."""
    insight = await dynamic_memory.get_node(insight_id)

    if insight.confidence > 0.95 and insight.verification_count > 3:
        # Add to static knowledge
        await cognee.add(insight.content, dataset_name="validated_insights")
        await cognee.cognify()

        # Archive from dynamic
        await dynamic_memory.archive_node(insight_id)
```

---

## Part 2: Complementary LLM Serving Architecture

### Problem Statement

Local inference on memory-constrained hardware (even 48GB M4 Max) faces challenges:
- Multiple models cannot fit in VRAM simultaneously
- Model switching causes OOM errors
- No unified interface for local vs cloud routing

### Solution: llama-swap + LiteLLM

```
┌─────────────────────────────────────────────────────────────┐
│  Application Layer (Agno, Dagger, etc.)                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  LiteLLM Proxy (Unified Routing & Observability)            │
│  - Single API endpoint for all models                       │
│  - Cost tracking across local + cloud                       │
│  - Fallback chains and load balancing                       │
│  - Centralized logging to Langfuse                          │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
   Cloud APIs       llama-swap        Ollama
   (OpenAI,         (VRAM Mgmt)       (Simple)
    Claude,              │
    Gemini)              │
                         ▼
                   llama.cpp server
                   (OpenAI-compatible)
```

### Layer Responsibilities

| Layer | Responsibility | Key Features |
|-------|---------------|--------------|
| **llama-swap** | VRAM management | Model hot-swapping, OOM prevention, lifecycle management |
| **LiteLLM** | API routing | Unified interface, cost tracking, fallbacks, observability |
| **llama.cpp** | Inference engine | OpenAI-compatible API, Metal/CUDA acceleration |

### Configuration

#### llama-swap Configuration

```yaml
# llama-swap config.yaml
models:
  qwen-7b:
    path: /models/qwen2.5-7b-instruct-q4_k_m.gguf
    n_gpu_layers: 35
    ctx_size: 8192

  llama-70b:
    path: /models/llama-3.3-70b-q4_k_m.gguf
    n_gpu_layers: 60
    ctx_size: 4096

  deepseek-coder:
    path: /models/deepseek-coder-33b-q4_k_m.gguf
    n_gpu_layers: 50
    ctx_size: 4096

# Only one model loaded at a time
max_loaded_models: 1
swap_timeout: 30  # seconds to wait for model swap
```

#### LiteLLM Proxy Configuration

```yaml
# LiteLLM config.yaml
model_list:
  # Cloud models (high capability)
  - model_name: planner_agent
    litellm_params:
      model: gpt-4o
      api_key: os.environ/OPENAI_API_KEY

  - model_name: planner_agent
    litellm_params:
      model: claude-sonnet-4-20250514
      api_key: os.environ/ANTHROPIC_API_KEY

  # Local models via llama-swap (zero cost)
  - model_name: local-7b
    litellm_params:
      model: openai/qwen-7b
      api_base: http://localhost:8080/v1
      api_key: "not-needed"

  - model_name: local-70b
    litellm_params:
      model: openai/llama-70b
      api_base: http://localhost:8080/v1
      api_key: "not-needed"

  - model_name: local-coder
    litellm_params:
      model: openai/deepseek-coder
      api_base: http://localhost:8080/v1
      api_key: "not-needed"

router_settings:
  routing_strategy: least-busy
  fallbacks:
    - local-7b: ["planner_agent"]  # Fallback to cloud if local fails

litellm_settings:
  drop_params: true
  num_retries: 3
```

### Usage Pattern

```python
from openai import OpenAI

# Single client for all models (local + cloud)
client = OpenAI(
    base_url="http://localhost:4000/v1",  # LiteLLM proxy
    api_key="sk-your-master-key"
)

# Request routes through: LiteLLM → llama-swap → llama.cpp
# Model hot-swapping handled automatically by llama-swap
response = client.chat.completions.create(
    model="local-7b",  # Virtual model name
    messages=[{"role": "user", "content": "Hello"}]
)

# Same client for cloud fallback
response = client.chat.completions.create(
    model="planner_agent",  # Routes to GPT-4 or Claude
    messages=[{"role": "user", "content": "Complex planning task"}]
)
```

---

## Integration: Combined Architecture

For full production deployment, combine both patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                   Agno Agent Orchestration                  │
│  - Planner agents (cloud via LiteLLM)                       │
│  - Worker agents (local via llama-swap + LiteLLM)           │
└────────────────────────┬────────────────────────────────────┘
                         │
           ┌─────────────┴─────────────┐
           │                           │
           ▼                           ▼
┌─────────────────────┐     ┌─────────────────────────────────┐
│  LiteLLM Proxy      │     │  AI Memory Layer                │
│  (LLM Routing)      │     │  ┌───────────┐ ┌─────────────┐  │
│                     │     │  │ Cognee    │ │ Graphiti    │  │
│  ┌───────────────┐  │     │  │ (Static)  │ │ (Dynamic)   │  │
│  │ Cloud APIs    │  │     │  │           │ │             │  │
│  │ llama-swap    │  │     │  │ Memgraph  │ │ FalkorDB    │  │
│  │ Ollama        │  │     │  └───────────┘ └─────────────┘  │
│  └───────────────┘  │     └─────────────────────────────────┘
└─────────────────────┘
```

### Key Benefits

1. **Cost Optimization**: Local-first inference with cloud fallback
2. **Memory Efficiency**: llama-swap prevents OOM on constrained hardware
3. **Data Separation**: Static knowledge (ACID) vs dynamic memory (velocity)
4. **Unified Interface**: Single LiteLLM endpoint for all LLM calls
5. **Observability**: Centralized cost tracking and logging

### Related Research

- `/research/project-context/Graph Tech Integration and Recommendation.md`
- `/research/project-context/llama.cpp-implementation.md`
- `/research/machine_learning/ai-compute-allocation-strategy.md`
- `/research/data/AI_MEMORY.md`

### OpenSpec Changes

These patterns are reflected in:
- `add-dual-graph-engine-architecture` (Tier 1)
- `add-local-llm-inference` (Tier 4)
