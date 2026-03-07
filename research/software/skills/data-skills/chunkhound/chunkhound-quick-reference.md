# ChunkHound Quick Reference

**Last Updated:** 2025-11-20
**Companion to:** chunkhound-data-models-ontologies.md

## Core Data Models - At a Glance

### Chunk Model
```python
Chunk(
    symbol: str,              # identifier
    start_line: int,          # 1-based
    end_line: int,            # inclusive
    code: str,                # content
    chunk_type: ChunkType,    # classification
    file_id: int,             # parent file
    language: Language,       # programming language
    # Optional: id, file_path, parent_header, byte offsets, timestamps, metadata
)
```

### File Model
```python
File(
    path: str,                # relative path
    mtime: float,             # unix timestamp
    language: Language,       # detected language
    size_bytes: int,          # file size
    # Optional: id, content_hash, timestamps
)
```

### Embedding Model
```python
Embedding(
    chunk_id: int,            # reference to chunk
    provider: str,            # "openai", "voyageai"
    model: str,               # model identifier
    dims: int,                # vector dimensions
    vector: list[float],      # embedding vector
)
```

---

## Type System Cheat Sheet

### Key Enums

**ChunkType:**
- Code: `FUNCTION`, `METHOD`, `CLASS`, `INTERFACE`, `ENUM`, `NAMESPACE`
- Docs: `COMMENT`, `DOCSTRING`, `HEADER_1-6`, `PARAGRAPH`
- Config: `TABLE`, `KEY_VALUE`, `ARRAY`
- Other: `BLOCK`, `UNKNOWN`

**Language (29 total):**
- Programming: `PYTHON`, `JAVASCRIPT`, `TYPESCRIPT`, `JAVA`, `GO`, `RUST`, `CPP`, etc.
- Config: `JSON`, `YAML`, `TOML`, `HCL`, `MARKDOWN`
- Text: `TEXT`, `PDF`

---

## cAST Algorithm - Quick Summary

### Parameters
```python
max_chunk_size = 1200          # non-whitespace chars
max_token_limit = 6000         # estimated tokens
max_line_gap = 5               # for merging
max_cross_concept_gap = 1      # cross-concept merge gap
```

### Three Phases
1. **Parse & Extract** - Tree-sitter AST → Semantic concepts
2. **Split** - Break oversized chunks at syntax boundaries
3. **Merge** - Combine adjacent chunks respecting size/concept rules

### Performance
- **Recall@5:** +4.3 points on RepoEval
- **Pass@1:** +2.67 points on SWE-bench

---

## Database Schemas

### DuckDB Tables

**files:**
```sql
id, path, name, extension, size, modified_time,
content_hash, language, created_at, updated_at
```

**chunks:**
```sql
id, file_id, chunk_type, symbol, code, start_line,
end_line, start_byte, end_byte, language, timestamps
```

**embeddings_{dims}:** (dimension-specific tables)
```sql
id, chunk_id, provider, model, embedding[dims],
dims, created_at
-- HNSW index on embedding (cosine similarity)
```

### LanceDB Schema (PyArrow)

**chunks:**
```python
id, file_id, content, start_line, end_line, chunk_type,
language, name, embedding (fixed-size list), provider, model
```

---

## Search API Quick Reference

### Semantic Search
```python
search_semantic(
    query: str,                    # natural language
    page_size: int = 10,
    offset: int = 0,
    threshold: float | None = None,
    path_filter: str | None = None,
    force_strategy: str | None = None  # "single_hop" | "multi_hop"
)
→ tuple[list[dict], dict]  # (results, metadata)
```

### Regex Search
```python
search_regex(
    pattern: str,                  # regex pattern
    page_size: int = 10,
    offset: int = 0,
    path_filter: str | None = None
)
→ tuple[list[dict], dict]
```

### Multi-Hop Search Convergence
```
Termination Conditions:
├── Time: 5 seconds max
├── Volume: 500 candidates max
├── Quality: Require 5+ high-scoring candidates
├── Score Drop: Stop if scores drop ≥ 0.15
└── Min Relevance: Top-5 minimum < 0.3
```

---

## Configuration Quick Guide

### .chunkhound.json Structure
```json
{
  "embedding": {
    "provider": "openai|voyageai",
    "model": "text-embedding-3-small",
    "api_key": "sk-...",
    "rerank_model": "rerank-english-v3.0",
    "rerank_url": "https://api.cohere.ai/v1/rerank"
  },
  "indexing": {
    "exclude": ["node_modules", "*.min.js"],
    "exclude_mode": "combined|config_only|gitignore_only",
    "force_reindex": false,
    "batch_size": 50
  },
  "database": {
    "provider": "duckdb|lancedb",
    "path": ".chunkhound"
  },
  "llm": {
    "provider": "openai|ollama|claude-code-cli",
    "utility_model": "gpt-5-nano",
    "synthesis_model": "gpt-5"
  }
}
```

### Environment Variables
```bash
# Embedding
CHUNKHOUND_EMBEDDING__PROVIDER=openai
CHUNKHOUND_EMBEDDING__API_KEY=sk-...
CHUNKHOUND_EMBEDDING__MODEL=text-embedding-3-small

# Database
CHUNKHOUND_DATABASE__PROVIDER=duckdb
CHUNKHOUND_DATABASE__PATH=.chunkhound

# Indexing
CHUNKHOUND_INDEXING__EXCLUDE='["node_modules", "dist"]'
CHUNKHOUND_INDEXING__FORCE_REINDEX=false
```

---

## Parser Ontology Summary

### Python AST Mappings
| Construct | ChunkType | Metadata |
|-----------|-----------|----------|
| `function_definition` | DEFINITION | decorators, params, types, async, generator |
| `class_definition` | DEFINITION | decorators, superclasses |
| `import_statement` | IMPORT | statement text |
| `comment` | COMMENT | is_docstring, location |

### TypeScript AST Mappings
| Construct | ChunkType | Metadata |
|-----------|-----------|----------|
| `interface_declaration` | DEFINITION | type_parameters, extends |
| `type_alias_declaration` | DEFINITION | type definition |
| `enum_declaration` | DEFINITION | members |
| `decorator` | Metadata | name, arguments |

### Markdown Mappings
| Element | ChunkType |
|---------|-----------|
| `# Heading` | HEADER_1 |
| `## Heading` | HEADER_2 |
| ` ```code``` ` | CODE_BLOCK |
| Paragraph | PARAGRAPH |
| Table | TABLE |

---

## Embedding Service

### Batching Configuration
```python
Max Chunks per Batch: 300
Provider Token Limit: With 20% safety margin
Concurrency: Auto-detected (default 8)
Adaptive Retry: Up to 3 levels of splitting
```

### Data Flow
```
generate_embeddings_for_chunks()
  → filter_existing_embeddings()
  → create_token_aware_batches()
  → generate_embeddings_in_batches()  # parallel
  → db.insert_embeddings_batch()
```

### Provider Defaults
| Provider | Model | Dimensions |
|----------|-------|------------|
| OpenAI | text-embedding-3-small | 1536 |
| VoyageAI | voyage-code-2 | 1024 |

---

## MCP Integration

### Server Modes
```bash
# STDIO (default)
uv run chunkhound mcp stdio

# HTTP
uv run chunkhound mcp http --port 5173
```

### MCP Tools
1. **search_semantic** - Natural language search
2. **search_regex** - Pattern matching
3. **code_research** - Multi-hop semantic exploration

### Critical Constraints
- **NO_STDOUT_LOGS** in stdio mode (breaks JSON-RPC)
- **SINGLE_THREADED_ONLY** for database access
- **Global state** preserved across tool invocations

---

## Performance Optimizations

### Critical Performance Rules
```python
Embedding Batching: MANDATORY (100x difference)
├── Batch size: 100-300 embeddings
└── Concurrent batches: Provider-dependent

Vector Index: DROP_BEFORE_BULK_INSERT (20x speedup)
├── Drop HNSW before bulk embedding insert
└── Rebuild after insert completes

Parser Caching: Lazy instantiation + reuse
└── Cache language parsers in global registry
```

### Query Performance
- HNSW nearest neighbor: ~5ms
- Database batch write: 50-100ms
- Multi-hop convergence: Up to 5 seconds

---

## CLI Commands Quick Reference

```bash
# Index repository
chunkhound index [path]

# Search
chunkhound search "query text"
chunkhound search --regex "pattern"

# Code research
chunkhound research "architecture question"

# MCP server
chunkhound mcp stdio
chunkhound mcp http --port 5173

# Diagnostics
chunkhound index --simulate          # Dry run
chunkhound index --check-ignores     # Verify patterns
chunkhound index --profile-startup   # Performance metrics
```

---

## Common File Locations

```
chunkhound/
├── core/
│   ├── models/
│   │   ├── chunk.py              # Chunk data model
│   │   ├── embedding.py          # Embedding model
│   │   └── file.py               # File model
│   ├── types/
│   │   └── common.py             # Type system
│   └── config/
│       ├── config.py             # Main config
│       ├── embedding_config.py   # Embedding settings
│       └── indexing_config.py    # Indexing settings
├── parsers/
│   ├── universal_parser.py       # cAST algorithm
│   └── mappings/
│       ├── python.py             # Python ontology
│       ├── typescript.py         # TypeScript ontology
│       └── [language].py         # Other languages
├── providers/
│   └── database/
│       ├── duckdb_provider.py    # DuckDB schema
│       └── lancedb_provider.py   # LanceDB schema
├── services/
│   ├── embedding_service.py      # Embedding batching
│   ├── search_service.py         # Search API
│   └── search/
│       └── multi_hop_strategy.py # Multi-hop algorithm
├── mcp_server/
│   └── stdio.py                  # MCP implementation
└── registry/
    └── __init__.py               # Component registry
```

---

## Result Format Reference

### Search Result Dictionary
```python
{
    "chunk_id": int,
    "file_path": str,
    "file_id": int,
    "symbol": str,
    "chunk_type": str,
    "code": str,
    "start_line": int,
    "end_line": int,
    "language": str,
    "similarity": float,          # semantic only
    "rerank_score": float,        # multi-hop only
    "parent_header": str | None,
    "metadata": dict
}
```

### Pagination Metadata
```python
{
    "offset": int,
    "page_size": int,
    "total": int | None,
    "has_more": bool,
    "next_offset": int | None
}
```

---

## Research Service Configuration

### Token Budgets (Depth-Dependent)
```python
Leaf Nodes:
├── File Content: 50,000 tokens
└── Answer Output: 21,000 tokens

Mid-Level:
├── File Content: 30,000 tokens
└── Answer Output: 15,000 tokens

Root (Synthesis):
├── File Content: 10,000 tokens
└── Answer Output: 11,000-19,000 tokens

Single-Pass (Long Context):
├── Total: 150,000 tokens
└── Output: 30,000 tokens
```

---

## Troubleshooting Quick Tips

### Common Issues

**Embeddings not generated:**
- Check API key: `CHUNKHOUND_EMBEDDING__API_KEY`
- Verify provider: `openai` or `voyageai`
- Check connectivity to embedding endpoint

**Files not indexed:**
- Verify not in `.gitignore`
- Check `exclude` patterns in config
- Use `--simulate` to debug discovery

**Search returns no results:**
- Verify embeddings exist in database
- Check `threshold` parameter (try lowering)
- Try regex search to verify chunks exist

**MCP connection issues:**
- Ensure no print() in stdio mode
- Verify JSON-RPC protocol compliance
- Check database path is accessible

**Performance issues:**
- Enable embedding batching
- Drop HNSW before bulk inserts
- Increase `max_concurrent` for indexing
- Check `per_file_timeout_seconds` for slow parsers

---

## References

- **Main Documentation:** https://chunkhound.github.io/
- **GitHub:** https://github.com/chunkhound/chunkhound
- **cAST Paper:** https://arxiv.org/abs/2506.15655
- **Full Analysis:** chunkhound-data-models-ontologies.md

---

**Version:** 1.0
**For:** ChunkHound Research
**Maintained By:** Research Team
