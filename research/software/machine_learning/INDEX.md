# Machine Learning Research - Consolidated Index

This directory contains consolidated research for the AI/ML layer of the hackathon platform.

## Directory Structure

```
machine_learning/consolidated/
├── 00-overview/
│   ├── ai-ml-systems-consolidated.md   # Comprehensive AI/ML architecture
│   ├── ai-compute-allocation-strategy.md
│   └── openspec-comprehensive-research.md
├── 01-agent-frameworks/
│   ├── mcp-research-report.md          # Model Context Protocol
│   └── stage-3-production-multi-agent-systems.md
├── 02-model-serving/                   # Model deployment patterns
└── 03-mlops/                           # ML operations & monitoring
```

## Related Skills

Tool-specific documentation has been moved to `.claude/skills/`:

| Tool | Skill Location | Purpose |
|------|----------------|---------|
| Agno | `.claude/skills/agno/` | Multi-agent orchestration |
| BAML | `.claude/skills/baml/` | Structured LLM outputs |
| Cognee | `.claude/skills/cognee/` | AI memory & knowledge graphs |
| HuggingFace | `.claude/skills/huggingface/` | Model hub & transformers |
| LiteLLM | `.claude/skills/litellm/` | Multi-provider LLM gateway |
| MLflow | `.claude/skills/mlflow/` | ML lifecycle management |

## AI/ML Architecture Overview

The platform uses a layered AI architecture:

1. **Agent Layer** (Agno)
   - Multi-agent orchestration
   - Tool calling & function execution
   - Memory persistence

2. **LLM Gateway** (LiteLLM)
   - Multi-provider support
   - Rate limiting & fallbacks
   - Cost tracking

3. **Knowledge Layer** (Cognee + LanceDB)
   - GraphRAG for contextual retrieval
   - Vector embeddings
   - Semantic search

4. **Type Safety** (BAML + Pydantic)
   - Structured LLM outputs
   - Schema validation
   - Error recovery

## Quick Links

- **Architecture Overview**: `00-overview/ai-ml-systems-consolidated.md`
- **Compute Strategy**: `00-overview/ai-compute-allocation-strategy.md`
- **MCP Protocol**: `01-agent-frameworks/mcp-research-report.md`
- **Multi-Agent Systems**: `01-agent-frameworks/stage-3-production-multi-agent-systems.md`

## Archive

Original skill-specific research files are archived at:
`/research/archive/ml-skills/`
