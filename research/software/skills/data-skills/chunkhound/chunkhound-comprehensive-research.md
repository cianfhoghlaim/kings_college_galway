# ChunkHound: Comprehensive Research and Integration Guide

## Executive Summary

ChunkHound is a deep research tool for codebases that transforms code into searchable knowledge bases for AI assistants. Built on Carnegie Mellon's cAST (Chunking via Abstract Syntax Trees) algorithm, it provides semantic search, multi-hop exploration, and regex pattern matching for 29+ file types. The tool integrates with AI assistants via the Model Context Protocol (MCP) and works with Claude Desktop, VS Code, Cursor, Windsurf, and other MCP-compatible tools.

**Key Achievements:**
- 4.3 point gain in Recall@5 on RepoEval retrieval benchmarks
- 2.67 point gain in Pass@1 on SWE-bench generation tasks
- 10-100x faster indexing via native git bindings
- Handles millions of lines across multi-language monorepos
- Local-first architecture with zero cloud dependencies

**Architecture:** Two-layer system combining enhanced RAG (base layer) with orchestrated code research (exploration layer).

---

## Table of Contents

1. [Core Architecture](#core-architecture)
2. [Usage Patterns for Semantic Search](#usage-patterns-for-semantic-search)
3. [Multi-Hop Search Strategies](#multi-hop-search-strategies)
4. [Integration with Development Workflows](#integration-with-development-workflows)
5. [MCP Server Patterns](#mcp-server-patterns)
6. [Performance Optimization](#performance-optimization)
7. [Indexing Strategies](#indexing-strategies)
8. [Configuration Best Practices](#configuration-best-practices)
9. [Common Pitfalls and Troubleshooting](#common-pitfalls-and-troubleshooting)
10. [Real-World Use Cases](#real-world-use-cases)

---

## Core Architecture

### Two-Layer System

#### Base Layer: Enhanced RAG

**cAST Chunking:**
- Structure-aware code segmentation using Abstract Syntax Trees
- Respects syntactic boundaries while enforcing size limits (1200 characters)
- Intelligently merges small adjacent pieces to prevent fragmentation
- Hierarchical traversal: classes → functions → statements
- Prevents breaking code mid-statement unlike fixed-size chunking

**Semantic Search:**
- Natural language queries via HNSW vector indexing
- Pluggable embedding backends: OpenAI, VoyageAI, Ollama
- Local vector search using DuckDB
- No per-token charges for large codebases

**Regex Search:**
- Exact pattern matching for comprehensive symbol coverage
- Zero API costs (local database operation)
- Complements semantic search for precise symbol discovery

#### Orchestration Layer: Code Research

**Multi-Hop Exploration:**
- BFS traversal discovering architectural relationships
- Query expansion for broader semantic discovery
- Iterative questioning based on discovered patterns
- Continuous reranking to prevent semantic drift

**Adaptive Scaling:**
- Dynamic token budgets: 30k-150k based on repository size
- Map-reduce synthesis for massive codebases
- Convergence detection to prevent infinite loops
- Scales from 10KB projects to 10M+ LOC monorepos

### Technical Foundation

**Database:** DuckDB (primary OLAP with HNSW indexing), LanceDB (experimental vector storage with Apache Arrow)

**Parsing:** Tree-sitter for universal AST parsing across 29 languages

**Integration:** Model Context Protocol (MCP) for AI assistant connectivity

**Language Support Categories:**
1. Programming: Python, JavaScript, TypeScript, Java, Go, Rust, C, C++, C#, Swift, Objective-C, Zig, Haskell, PHP, Ruby, Scala
2. Configuration: JSON, YAML, TOML, HCL, Markdown
3. Specialized: Vue, SQL, Shell scripts, Dockerfile, k8s manifests

---

## Usage Patterns for Semantic Search

### Pattern 1: Natural Language Code Discovery

**Use Case:** Finding functionality without knowing exact implementation details

**Example Query:** "How does user authentication work?"

**What ChunkHound Returns:**
- Login function implementations
- Password hashing utilities
- Token validation logic
- Session management code
- Security logging functions

**Why It Works:** Semantic embeddings capture intent and meaning, not just keywords. The AI assistant receives contextually complete code chunks showing the full authentication flow.

**Best Practice:**
```bash
# Index with semantic embeddings enabled (default)
chunkhound index

# Query naturally through AI assistant
# "Find all authentication-related code"
# "Show me how API errors are handled"
# "Where is the database connection logic?"
```

### Pattern 2: Architectural Understanding

**Use Case:** Understanding how systems connect and interact

**Example Query:** "How does the frontend communicate with the backend?"

**Multi-Hop Process:**
1. Finds frontend API client code
2. Discovers corresponding backend endpoints
3. Locates middleware and authentication layers
4. Shows error handling and validation
5. Identifies data transformation logic

**Configuration:**
```json
{
  "embedding": {
    "provider": "voyageai",
    "api_key": "your-key",
    "model": "voyage-code-2"
  }
}
```

### Pattern 3: Cross-Language Pattern Discovery

**Use Case:** Finding similar patterns across polyglot codebases

**Example Query:** "Show all database query implementations"

**Returns:**
- SQL queries in Python (SQLAlchemy, psycopg2)
- ORM models in TypeScript (Prisma, TypeORM)
- Raw SQL in Go
- Query builders in Java

**Best Practice:** ChunkHound's language-agnostic semantic concepts identify similar patterns regardless of implementation language.

### Pattern 4: Documentation-Code Correlation

**Use Case:** Linking documentation to implementation

**Workflow:**
```bash
# Index both code and markdown documentation
chunkhound index

# Real-time markdown indexing for live documentation
# MCP server auto-updates as docs change
```

**Query:** "Find the implementation described in the API design doc"

**Returns:** Relevant code chunks that match documented specifications, helping ensure code-documentation alignment.

---

## Multi-Hop Search Strategies

### How Multi-Hop Search Works

#### Phase 1: Expanded Initial Retrieval

```
Standard Search: Fetch 10 results
Multi-Hop Search: Fetch 30 results (3x multiplier, up to 100 max)
```

Provides reranking algorithm with more high-quality candidates to evaluate.

#### Phase 2: Seed Selection and Neighbor Discovery

1. Take highest-scoring chunks as seeds
2. Discover semantic neighbors (code sharing similar patterns/concepts)
3. Create "hops" from query → initial matches → related code

#### Phase 3: Continuous Reranking

- Rerank ALL discovered code against original query
- Prevents semantic drift
- Maintains relevance throughout exploration

#### Phase 4: Convergence Detection

**Termination Conditions:**
- Score improvements degrade >0.15 between iterations
- 5-second timeout reached
- Maximum hop depth achieved

### Multi-Hop Use Cases

#### Use Case 1: Complete Feature Discovery

**Query:** "HNSW optimization"

**Hop Progression:**
```
Hop 1: Embedding repository code
Hop 2: DuckDB provider optimizations
Hop 3: Search service coordination
Hop 4: Indexing orchestration
```

**Result:** End-to-end picture of vector indexing across architectural layers

#### Use Case 2: Security Audit Trail

**Query:** "Authentication security"

**Discovered Relationships:**
```
Login Handler
  ↓
Password Hashing (bcrypt/argon2)
  ↓
Token Generation (JWT)
  ↓
Token Validation Middleware
  ↓
Session Management
  ↓
Security Logging & Audit
```

**Value:** Comprehensive security understanding spanning multiple modules

#### Use Case 3: Data Flow Tracing

**Query:** "User data processing"

**Multi-Hop Discovery:**
1. Input validation endpoints
2. Data transformation utilities
3. Database models and schemas
4. Caching layers
5. API serialization logic
6. Frontend rendering code

**Configuration for Multi-Hop:**
```json
{
  "embedding": {
    "provider": "voyageai",
    "api_key": "your-key",
    "model": "voyage-code-2"
  }
}
```

**Note:** Multi-hop reranking requires providers supporting reranking (VoyageAI, custom TEI servers).

### Hybrid Search Strategy

**Combining Semantic + Regex:**

```bash
# AI assistant workflow (automatic via Code Research tool)
1. Semantic query: "authentication implementation"
   → Finds conceptually related code

2. Regex follow-up: "def (login|authenticate|verify_credentials)"
   → Discovers all auth function names

3. Semantic expansion on discovered symbols
   → Explores implementation details
```

**Why Hybrid Works:**
- Semantic search casts wide nets for concepts
- Regex ensures comprehensive symbol coverage
- Together they provide both breadth and precision

---

## Integration with Development Workflows

### Workflow 1: Real-Time Code Exploration (MCP Integration)

**Setup:**
```bash
# Install ChunkHound
curl -LsSf https://astral.sh/uv/install.sh | sh
uv tool install chunkhound

# Configure project
cd /path/to/project
cat > .chunkhound.json << 'EOF'
{
  "embedding": {
    "provider": "openai",
    "api_key": "sk-your-key"
  }
}
EOF

# Initial index
chunkhound index
```

**AI Assistant Integration:**
- MCP server automatically monitors file changes
- Index updates in real-time as you edit
- No manual re-indexing required
- AI assistant always has current codebase context

**Developer Experience:**
1. Edit code in VS Code/Cursor/Windsurf
2. ChunkHound detects changes via MCP
3. Updates only changed chunks (smart diffs)
4. AI assistant queries updated index instantly

### Workflow 2: Branch-Aware Development

**Scenario:** Feature branch development with frequent main rebases

**ChunkHound Behavior:**
```bash
# Developer switches branches
git checkout feature/new-auth

# ChunkHound automatically:
# 1. Detects branch switch
# 2. Identifies changed files
# 3. Re-indexes only modified chunks
# 4. Preserves unchanged chunk embeddings
```

**Performance Benefit:**
- Only changed files re-processed
- Unchanged embeddings retained
- Fast branch switching (seconds, not minutes)

### Workflow 3: Monorepo Development

**Configuration for Monorepos:**
```json
{
  "embedding": {
    "provider": "openai",
    "api_key": "sk-your-key"
  },
  "indexing": {
    "include": [
      "services/auth/**/*.ts",
      "services/api/**/*.py",
      "shared/lib/**/*.js"
    ],
    "exclude": [
      "**/node_modules/**",
      "**/dist/**",
      "**/*.test.*"
    ]
  }
}
```

**Adaptive Scaling:**
- 10KB project: Quick semantic search, 30k token budget
- 10M LOC monorepo: Deep exploration, map-reduce synthesis, 150k token budget

**Map-Reduce for Massive Codebases:**
1. Query breaks into parallel subtasks
2. Each subtask explores code segments
3. Results synthesized into coherent answer
4. Prevents context collapse on multi-million LOC codebases

### Workflow 4: Offline Development

**Use Case:** Air-gapped environments, travel, privacy-sensitive projects

**Configuration:**
```json
{
  "embedding": {
    "provider": "openai-compatible",
    "base_url": "http://localhost:11434",
    "model": "nomic-embed-text",
    "api_key": "not-needed"
  }
}
```

**Setup with Ollama:**
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull embedding model
ollama pull nomic-embed-text

# Index with local embeddings
chunkhound index
```

**Benefits:**
- Zero external API calls
- Complete privacy (code never leaves machine)
- No per-token costs
- Works without internet connection

### Workflow 5: CI/CD Integration

**Use Case:** Documentation validation, architectural compliance checks

**Example: Verify Documentation Matches Code**
```yaml
# .github/workflows/docs-check.yml
name: Documentation Sync Check
on: [pull_request]
jobs:
  check-docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install ChunkHound
        run: |
          curl -LsSf https://astral.sh/uv/install.sh | sh
          uv tool install chunkhound
      - name: Index codebase
        run: chunkhound index --no-embeddings
      - name: Verify patterns
        run: |
          # Use regex search to verify documented patterns exist
          # Custom validation script using ChunkHound CLI
```

**Note:** For CI/CD, consider `--no-embeddings` to skip embedding generation and use regex-only search for fast validation.

---

## MCP Server Patterns

### Pattern 1: Claude Desktop Integration

**Configuration File:** `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS)

```json
{
  "mcpServers": {
    "chunkhound": {
      "command": "uv",
      "args": ["run", "chunkhound", "mcp"],
      "env": {
        "OPENAI_API_KEY": "sk-your-key-here",
        "CHUNKHOUND_DB_PATH": "/custom/path/to/.chunkhound",
        "CHUNKHOUND_EMBEDDING_MODEL": "text-embedding-3-small"
      }
    }
  }
}
```

**Multiple Project Setup:**
```json
{
  "mcpServers": {
    "chunkhound-project-a": {
      "command": "uv",
      "args": ["run", "chunkhound", "mcp", "/path/to/project-a"],
      "env": {
        "OPENAI_API_KEY": "sk-your-key"
      }
    },
    "chunkhound-project-b": {
      "command": "uv",
      "args": ["run", "chunkhound", "mcp", "/path/to/project-b"],
      "env": {
        "OPENAI_API_KEY": "sk-your-key"
      }
    }
  }
}
```

**After Configuration:**
1. Restart Claude Desktop
2. ChunkHound tools appear in Claude's interface
3. AI can search codebase semantically and with regex
4. File watching enabled automatically

### Pattern 2: VS Code MCP Integration

**Configuration File:** `.vscode/mcp.json` (project root)

```json
{
  "servers": {
    "chunkhound": {
      "type": "stdio",
      "command": "chunkhound",
      "args": ["mcp", "/path/to/project"]
    }
  }
}
```

**VS Code Extension Setup:**
1. Install MCP extension for VS Code
2. Create `.vscode/mcp.json` with above configuration
3. Reload VS Code window
4. ChunkHound integrates with AI assistant features

### Pattern 3: Cursor IDE Integration

**Configuration File:** `.cursor/mcp.json`

```json
{
  "mcpServers": {
    "chunkhound": {
      "command": "chunkhound",
      "args": ["mcp", "/path/to/project"]
    }
  }
}
```

**Cursor Workflow:**
1. ChunkHound enhances Cursor's Composer feature
2. Better context understanding for agentic workflows
3. Tab completions informed by semantic search
4. Automatic codebase exploration during feature implementation

### Pattern 4: Windsurf Integration

**Configuration File:** `~/.codeium/windsurf/mcp_config.json`

```json
{
  "mcpServers": {
    "chunkhound": {
      "command": "uv",
      "args": ["run", "chunkhound", "mcp"],
      "env": {
        "OPENAI_API_KEY": "sk-your-key"
      }
    }
  }
}
```

**Windsurf Cascade Enhancement:**
- ChunkHound improves Cascade's context indexing
- Better agentic workflow with semantic code understanding
- Enhanced multi-file editing with architectural awareness

### Pattern 5: HTTP MCP Server (Remote Access)

**Use Case:** Shared development teams, remote access to indexed codebases

**Server Setup:**
```bash
# Start HTTP MCP server
chunkhound mcp --http --port 8080 /path/to/project
```

**Client Configuration:**
```json
{
  "mcpServers": {
    "chunkhound-remote": {
      "type": "http",
      "url": "http://codebase-server.local:8080"
    }
  }
}
```

**Known Issues (as of Nov 2025):**
- Issue #53: Concurrent HTTP instances may crash or return empty semantic search results
- Recommendation: Use stdio mode for production, HTTP for experimentation

### Pattern 6: Workspace with Multiple Repositories

**Use Case:** Monorepo workspace with multiple git repositories

**Known Limitation (Issue #87):**
- Workspace indexing discovers only one repository when multiple exist
- Current workaround: Index repositories individually

**Workaround Configuration:**
```json
{
  "mcpServers": {
    "chunkhound-repo1": {
      "command": "chunkhound",
      "args": ["mcp", "/workspace/repo1"]
    },
    "chunkhound-repo2": {
      "command": "chunkhound",
      "args": ["mcp", "/workspace/repo2"]
    }
  }
}
```

### MCP Best Practices

1. **Environment Variables Over Config Files:** Store API keys in environment variables, not `.chunkhound.json`
2. **Project-Specific Servers:** Configure separate MCP servers for each major project
3. **Stdio Over HTTP:** Use stdio mode for reliability; HTTP mode is experimental
4. **Restart Required:** Restart IDE/Claude Desktop after MCP config changes
5. **Verify Connection:** Check IDE logs to confirm MCP server connection

---

## Performance Optimization

### Optimization 1: Fast Indexing (10-100x Speedup)

**Techniques:**
1. Native git bindings for file discovery
2. Parallel directory traversal
3. ProcessPoolExecutor for CPU-bound parsing
4. Provider-aware embedding batching

**Embedding Batch Sizes:**
- OpenAI: 8 embeddings per request
- VoyageAI: 40 embeddings per request
- Auto-tuned via `chunkhound calibrate`

**Measurement:**
```bash
# Profile indexing performance
CHUNKHOUND_NO_RICH=1 chunkhound index --profile-startup 2>profile.json

# Analyze timing
cat profile.json | jq '.discovery_time_ms, .embedding_time_ms'
```

**Results to Expect:**
- Small projects (<1000 files): 1-5 seconds
- Medium projects (1000-10000 files): 10-30 seconds
- Large monorepos (10000+ files): 1-5 minutes

### Optimization 2: Differential Updates (Smart Content Diffs)

**How It Works:**
```python
# ChunkHound internal logic (conceptual)
for chunk in code_chunks:
    old_content = db.get_chunk_content(chunk.id)
    new_content = chunk.content

    if old_content == new_content:
        # Preserve existing embedding
        continue
    else:
        # Re-generate embedding only for changed chunk
        chunk.embedding = generate_embedding(new_content)
        db.update(chunk)
```

**Performance Impact:**
- Typical code change: 1-5% of chunks modified
- Update time: ~2-5 seconds (vs. full re-index: minutes)
- Embedding API calls: 95-99% reduction

**Real-World Example:**
```
Codebase: 5000 files, 50000 chunks
Developer edits: 3 files, 15 chunks changed

Traditional approach: Re-embed all 50000 chunks (5 minutes, $5 API cost)
ChunkHound approach: Re-embed 15 chunks (2 seconds, $0.01 API cost)
```

### Optimization 3: Branch-Switch Efficiency

**Git Integration:**
```bash
# ChunkHound detects branch switches
git checkout feature/new-auth

# Internal process:
# 1. Compare current HEAD with indexed commit
# 2. Identify changed files via git diff
# 3. Re-index only changed files
# 4. Update database incrementally
```

**Performance:**
- Branch switch detection: <100ms
- Typical branch diff: 10-50 files
- Re-index time: 2-10 seconds

### Optimization 4: YAML Parsing Acceleration

**Problem:** Large Kubernetes manifests slow to parse with tree-sitter

**Solution:** RapidYAML parser for YAML files

**Performance Comparison:**
```
k8s manifest (10MB):
- tree-sitter: 10-15 seconds
- RapidYAML: 100-150 milliseconds

Speedup: 100x faster
```

**Automatic:** ChunkHound automatically uses RapidYAML for YAML files

### Optimization 5: Embedding Provider Selection

**Performance Characteristics:**

| Provider | Speed | Cost | Quality | Offline |
|----------|-------|------|---------|---------|
| OpenAI text-embedding-3-small | Fast | Low | Good | No |
| OpenAI text-embedding-3-large | Medium | High | Excellent | No |
| VoyageAI voyage-code-2 | Fast | Medium | Excellent | No |
| Ollama nomic-embed-text | Medium | Free | Good | Yes |

**Recommendation:**
- **Development:** Ollama (free, private)
- **Production:** VoyageAI voyage-code-2 (best quality for code)
- **Budget-conscious:** OpenAI text-embedding-3-small

**Configuration Examples:**

```json
// Fast, cheap, cloud-based
{
  "embedding": {
    "provider": "openai",
    "model": "text-embedding-3-small",
    "api_key": "sk-your-key"
  }
}

// Best quality for code
{
  "embedding": {
    "provider": "voyageai",
    "model": "voyage-code-2",
    "api_key": "your-key"
  }
}

// Free, private, offline
{
  "embedding": {
    "provider": "openai-compatible",
    "base_url": "http://localhost:11434",
    "model": "nomic-embed-text"
  }
}
```

### Optimization 6: Selective Indexing

**Strategy:** Index only relevant code, exclude build artifacts and dependencies

```json
{
  "indexing": {
    "include": [
      "src/**/*.{ts,tsx,js,jsx}",
      "lib/**/*.py",
      "docs/**/*.md"
    ],
    "exclude": [
      "**/node_modules/**",
      "**/dist/**",
      "**/build/**",
      "**/.next/**",
      "**/__pycache__/**",
      "**/*.min.js",
      "**/*.map"
    ]
  }
}
```

**Impact:**
- 50-70% reduction in indexed files
- Faster indexing (fewer files to process)
- Lower embedding costs
- More relevant search results (no noise from dependencies)

### Optimization 7: Calibration Command

**Auto-Tune Batch Sizes:**
```bash
chunkhound calibrate
```

**What It Does:**
- Tests various batch sizes for your provider
- Measures throughput and latency
- Selects optimal batch size
- Saves configuration automatically

**When to Run:**
- After changing embedding providers
- When experiencing slow indexing
- After upgrading ChunkHound

---

## Indexing Strategies

### Strategy 1: Gitignore-Aware Indexing (Default)

**Behavior:**
- Respects `.gitignore` automatically
- Applies workspace `.gitignore` to non-repo paths (configurable)
- No manual exclusion rules needed for common patterns

**Configuration:**
```json
{
  "indexing": {
    "workspace_gitignore_nonrepo": true  // default
  }
}
```

**CLI Override:**
```bash
# Disable workspace gitignore for non-repo paths
chunkhound index --no-nonrepo-gitignore
```

**Use Case:** Standard development workflow where `.gitignore` defines untracked files

### Strategy 2: Config-Only Indexing

**Behavior:**
- Ignores `.gitignore` completely
- Only uses exclusion rules from `.chunkhound.json`
- Useful for indexing git-ignored documentation or data files

**Configuration:**
```json
{
  "indexing": {
    "exclude_mode": "config_only",
    "include": [
      "**/*.py",
      "**/*.md",
      "data/schemas/**/*.json"  // Index git-ignored data
    ],
    "exclude": [
      "**/__pycache__/**"
    ]
  }
}
```

**Use Case:** Indexing private documentation, data schemas, or configuration examples stored in git-ignored directories

### Strategy 3: Monorepo Service-Specific Indexing

**Scenario:** Large monorepo with multiple services, index only relevant services

**Configuration:**
```json
{
  "indexing": {
    "include": [
      "services/auth/**/*.{ts,js}",
      "services/api/**/*.{ts,js}",
      "shared/lib/**/*.{ts,js}",
      "packages/common/**/*.{ts,js}"
    ],
    "exclude": [
      "**/node_modules/**",
      "**/*.test.{ts,js}",
      "**/*.spec.{ts,js}"
    ]
  }
}
```

**Best Practice for Anchored Paths:**
```
Correct: "services/auth/**/*.ts"
Incorrect: "/services/auth/**/*.ts" (leading slash may cause issues)

Monorepo nested case:
If ChunkHound root is at /workspace/monorepo/
Use: "monorepo/services/auth/**/*.ts"
```

### Strategy 4: Incremental Indexing (Production)

**Workflow:**
```bash
# Initial full index
chunkhound index

# MCP server maintains index automatically
chunkhound mcp

# Manual incremental update (if needed)
chunkhound index  # Only changed files re-processed
```

**How Incremental Works:**
1. ChunkHound stores last indexed commit hash
2. Compares current working tree with indexed state
3. Identifies added/modified/deleted files
4. Updates only changed chunks
5. Preserves embeddings for unchanged code

**Verification:**
```bash
# Simulate to see what would be indexed
chunkhound index --simulate --show-sizes --sort size_desc
```

### Strategy 5: Test-Free Indexing

**Rationale:** Exclude test files to focus on production code

**Configuration:**
```json
{
  "indexing": {
    "exclude": [
      "**/*.test.*",
      "**/*.spec.*",
      "**/test/**",
      "**/tests/**",
      "**/__tests__/**",
      "**/spec/**"
    ]
  }
}
```

**Trade-offs:**
- Pros: Faster indexing, reduced noise, lower costs
- Cons: AI can't help with test-related questions

**Recommendation:** Include tests for projects where test coverage and test writing are primary concerns; exclude for production code exploration.

### Strategy 6: Multi-Language Projects

**Configuration:**
```json
{
  "indexing": {
    "include": [
      "backend/**/*.{py,sql}",
      "frontend/**/*.{ts,tsx,css}",
      "mobile/**/*.{swift,kt}",
      "docs/**/*.md"
    ]
  }
}
```

**Language-Specific Considerations:**
- **Python:** Exclude `**/__pycache__/**`, `**/*.pyc`, `.venv/**`
- **JavaScript/TypeScript:** Exclude `**/node_modules/**`, `**/dist/**`, `**/build/**`
- **Java:** Exclude `**/target/**`, `**/*.class`
- **Go:** Exclude `**/vendor/**` (if not using modules)
- **Rust:** Exclude `**/target/**`

### Strategy 7: Documentation-First Indexing

**Use Case:** Index documentation before code for better AI context

**Workflow:**
```bash
# Index markdown documentation first
chunkhound index --no-embeddings  # Fast, regex-only

# Add semantic embeddings for docs
chunkhound index

# Now index code
# (Continue as normal)
```

**Configuration:**
```json
{
  "indexing": {
    "include": [
      "docs/**/*.md",
      "README.md",
      "ARCHITECTURE.md",
      "src/**/*.{ts,js,py}"
    ]
  }
}
```

**Benefit:** AI understands documented architecture before exploring code implementation

### Strategy 8: Simulation and Verification

**Preview Files Before Indexing:**
```bash
# List all files that would be indexed
chunkhound index --simulate . --sort path

# Show file sizes
chunkhound index --simulate . --show-sizes --sort size_desc

# JSON output for programmatic processing
chunkhound index --simulate . --json > files.json

# Debug ignore rules
chunkhound index --simulate . --debug-ignores

# Compare with git
chunkhound index --check-ignores --vs git --json > ignore_diff.json
```

**Use Cases:**
- Verify `.gitignore` rules work as expected
- Check which files consume most embedding tokens
- Debug inclusion/exclusion patterns
- Validate configuration before expensive embedding generation

---

## Configuration Best Practices

### Practice 1: Environment Variable API Keys

**Recommended:**
```bash
# Set in shell environment
export OPENAI_API_KEY="sk-your-key"
export VOYAGEAI_API_KEY="your-key"

# Configure ChunkHound without API key in config
cat > .chunkhound.json << 'EOF'
{
  "embedding": {
    "provider": "openai"
  }
}
EOF
```

**Not Recommended:**
```json
// API key in config file (security risk)
{
  "embedding": {
    "provider": "openai",
    "api_key": "sk-your-key-here"  // DON'T DO THIS
  }
}
```

**Security:**
```bash
# Add to .gitignore
echo ".chunkhound.json" >> .gitignore
```

### Practice 2: Provider-Specific Configuration

#### OpenAI (Most Common)

```json
{
  "embedding": {
    "provider": "openai",
    "model": "text-embedding-3-small"
  }
}
```

Environment variable: `OPENAI_API_KEY`

#### VoyageAI (Best for Code)

```json
{
  "embedding": {
    "provider": "voyageai",
    "model": "voyage-code-2"
  }
}
```

Environment variable: `VOYAGEAI_API_KEY`

Features: Supports multi-hop reranking for better semantic search

#### Ollama (Local, Offline)

```json
{
  "embedding": {
    "provider": "openai-compatible",
    "base_url": "http://localhost:11434",
    "model": "nomic-embed-text"
  }
}
```

Setup:
```bash
ollama pull nomic-embed-text
ollama serve
```

#### Text Embeddings Inference (TEI)

```json
{
  "embedding": {
    "provider": "tei",
    "base_url": "http://localhost:8080"
  }
}
```

Features: Supports reranker format for two-stage retrieval

#### BGE-IN-ICL (In-Context Learning)

```json
{
  "embedding": {
    "provider": "bge-in-icl",
    "base_url": "http://localhost:8080",
    "language": "python",
    "enable_icl": true
  }
}
```

Features: Language-specific embeddings, in-context learning support

### Practice 3: Database Location Configuration

**Custom Database Path:**
```json
{
  "database": {
    "path": "/custom/path/to/.chunkhound"
  }
}
```

**Via Environment Variable:**
```bash
export CHUNKHOUND_DB_PATH="/custom/path/to/.chunkhound"
```

**Use Cases:**
- Shared network storage for team-wide index
- SSD vs HDD storage optimization
- Separating database from source code

### Practice 4: Project Templates

**Minimal Template (Regex Only):**
```json
{
  "indexing": {
    "exclude": [
      "**/node_modules/**",
      "**/dist/**"
    ]
  }
}
```

Run: `chunkhound index --no-embeddings`

**Standard Template (OpenAI):**
```json
{
  "embedding": {
    "provider": "openai",
    "model": "text-embedding-3-small"
  },
  "indexing": {
    "exclude": [
      "**/node_modules/**",
      "**/dist/**",
      "**/*.test.*"
    ]
  }
}
```

**Advanced Template (VoyageAI, Multi-Hop):**
```json
{
  "embedding": {
    "provider": "voyageai",
    "model": "voyage-code-2"
  },
  "indexing": {
    "include": [
      "src/**/*.{ts,tsx}",
      "lib/**/*.py"
    ],
    "exclude": [
      "**/node_modules/**",
      "**/dist/**",
      "**/__pycache__/**",
      "**/*.test.*",
      "**/*.spec.*"
    ],
    "exclude_mode": "layered"
  }
}
```

**Offline Template (Ollama):**
```json
{
  "embedding": {
    "provider": "openai-compatible",
    "base_url": "http://localhost:11434",
    "model": "nomic-embed-text"
  },
  "indexing": {
    "workspace_gitignore_nonrepo": true
  }
}
```

### Practice 5: Team Standardization

**Repo Root `.chunkhound.json.template`:**
```json
{
  "embedding": {
    "provider": "openai",
    "model": "text-embedding-3-small"
  },
  "indexing": {
    "include": [
      "src/**/*.ts",
      "lib/**/*.py"
    ],
    "exclude": [
      "**/*.test.*"
    ]
  }
}
```

**Team Setup:**
```bash
# Each developer copies template
cp .chunkhound.json.template .chunkhound.json

# Sets their own API key
export OPENAI_API_KEY="individual-developer-key"

# Indexes with consistent config
chunkhound index
```

**`.gitignore`:**
```gitignore
.chunkhound.json
.chunkhound/
```

### Practice 6: Configuration Validation

**Verify Configuration:**
```bash
# Simulate indexing to verify include/exclude rules
chunkhound index --simulate --debug-ignores

# Check which files will be indexed
chunkhound index --simulate --sort path --json | jq '.files[] | .path'

# Verify file sizes (estimate embedding costs)
chunkhound index --simulate --show-sizes --sort size_desc
```

**Cost Estimation:**
```bash
# Get total characters to embed
total_chars=$(chunkhound index --simulate --json | jq '[.files[].size] | add')

# Estimate tokens (rough: chars / 4)
tokens=$((total_chars / 4))

# Estimate cost (OpenAI text-embedding-3-small: $0.00002 per 1K tokens)
cost=$(echo "scale=4; $tokens * 0.00002 / 1000" | bc)

echo "Estimated embedding cost: \$$cost"
```

---

## Common Pitfalls and Troubleshooting

### Pitfall 1: High CPU Usage During Indexing

**Issue (GitHub #43):** ChunkHound uses 100% CPU and takes long to return results

**Root Causes:**
1. Large codebase with many files
2. CPU-bound parsing (Tree-sitter)
3. Embedding generation bottleneck

**Solutions:**

```bash
# Profile to identify bottleneck
CHUNKHOUND_NO_RICH=1 chunkhound index --profile-startup 2>profile.json

# Check timing breakdown
cat profile.json | jq '{
  discovery_ms: .discovery_time_ms,
  parsing_ms: .parsing_time_ms,
  embedding_ms: .embedding_time_ms
}'
```

**If parsing is slow:**
- Reduce file count via better exclusion rules
- Index in stages (docs first, then code)

**If embedding is slow:**
- Use faster provider (OpenAI > VoyageAI > Ollama)
- Calibrate batch sizes: `chunkhound calibrate`
- Consider `--no-embeddings` for initial index, add embeddings later

### Pitfall 2: Empty Semantic Search Results (HTTP MCP)

**Issue (GitHub #53):** Multiple concurrent MCP instances cause stdio crashes and HTTP semantic search returns empty results

**Workaround:**
```json
// Use stdio mode instead of HTTP
{
  "mcpServers": {
    "chunkhound": {
      "command": "uv",
      "args": ["run", "chunkhound", "mcp"],  // stdio mode
      "env": {
        "OPENAI_API_KEY": "sk-your-key"
      }
    }
  }
}
```

**Status:** HTTP mode is experimental; stdio is production-ready

### Pitfall 3: MCP Server Reconnection Issues

**Issue (GitHub #93):** CLI fails to reconnect to MCP server when starting new conversations in same session

**Symptoms:**
- First conversation works
- Subsequent conversations fail to connect
- Need to restart entire application

**Temporary Workaround:**
- Restart Claude Desktop / IDE between conversations
- Monitor GitHub issue for updates

**Prevention:**
- Use stdio mode (more stable than HTTP)
- Restart IDE after config changes

### Pitfall 4: Multiple Repositories in Workspace

**Issue (GitHub #87):** Workspace indexing only discovers one repository when multiple git repos exist

**Current Behavior:**
```
/workspace/
  ├── repo-a/  # ✓ Indexed
  ├── repo-b/  # ✗ Not indexed
  └── repo-c/  # ✗ Not indexed
```

**Workaround:**
```json
{
  "mcpServers": {
    "chunkhound-repo-a": {
      "command": "chunkhound",
      "args": ["mcp", "/workspace/repo-a"]
    },
    "chunkhound-repo-b": {
      "command": "chunkhound",
      "args": ["mcp", "/workspace/repo-b"]
    },
    "chunkhound-repo-c": {
      "command": "chunkhound",
      "args": ["mcp", "/workspace/repo-c"]
    }
  }
}
```

### Pitfall 5: Embedding Model Compatibility

**Issue (GitHub #41):** Qwen3-Embedding-8B model doesn't support embedding operations

**Problem:** Not all models advertised as "embedding models" support OpenAI-compatible embedding API

**Solution:**
```bash
# Test model compatibility
curl http://localhost:11434/v1/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "model": "your-model-name",
    "input": "test"
  }'
```

**Recommended Models:**
- **Ollama:** nomic-embed-text, mxbai-embed-large
- **OpenAI:** text-embedding-3-small, text-embedding-3-large
- **VoyageAI:** voyage-code-2, voyage-large-2

### Pitfall 6: Incorrect Anchored Paths in Monorepos

**Issue:** Include patterns like `"src/**/*.ts"` don't match files

**Problem:** ChunkHound resolves paths from its working directory, not repository root

**Debugging:**
```bash
# Check what ChunkHound sees
chunkhound index --simulate --debug-ignores

# If ChunkHound runs from /workspace/
# And repo is at /workspace/monorepo/
# Then pattern should be:
"monorepo/src/**/*.ts"  # Correct
# Not:
"src/**/*.ts"  # Won't match
```

**Solution:**
```json
{
  "indexing": {
    "include": [
      "monorepo/src/**/*.ts",
      "monorepo/lib/**/*.py"
    ]
  }
}
```

### Pitfall 7: Large File Skipping

**Symptom:** Some large files not indexed

**Possible Cause:** ChunkHound may skip extremely large files to prevent memory issues

**Check File Sizes:**
```bash
chunkhound index --simulate --show-sizes --sort size_desc | head -20
```

**Workaround:**
- Break large files into smaller modules
- Or use regex-only search (no embedding) for large files

### Pitfall 8: Gitignore Confusion

**Issue:** Files unexpectedly excluded or included

**Debug:**
```bash
# See ignore decisions
chunkhound index --debug-ignores

# Compare with git's view
chunkhound index --check-ignores --vs git --json > diff.json

# Analyze differences
cat diff.json | jq '.differences[]'
```

**Common Causes:**
1. Workspace `.gitignore` applied to non-repo paths (toggle: `workspace_gitignore_nonrepo`)
2. Nested `.gitignore` files
3. Global git ignore rules (`~/.gitignore_global`)

**Solution:**
```json
{
  "indexing": {
    "exclude_mode": "config_only",  // Ignore all .gitignore files
    "include": [
      "explicit/paths/**/*.ts"
    ],
    "exclude": [
      "explicit/exclude/**"
    ]
  }
}
```

### Pitfall 9: Slow Branch Switching

**Symptom:** Indexing takes long after `git checkout`

**Possible Causes:**
1. Many files changed between branches
2. Full re-index triggered instead of incremental
3. Embedding generation bottleneck

**Diagnosis:**
```bash
# Check how many files changed
git diff --name-only main..feature-branch | wc -l

# Profile re-indexing
CHUNKHOUND_NO_RICH=1 chunkhound index --profile-startup 2>profile.json
```

**Optimization:**
- Reduce changed files via better branching strategy
- Use faster embedding provider
- Consider regex-only index (`--no-embeddings`) for rapid prototyping

### Pitfall 10: MCP Server Not Found in IDE

**Symptom:** ChunkHound doesn't appear in AI assistant tools

**Checklist:**
1. ✓ ChunkHound installed: `which chunkhound`
2. ✓ MCP config file exists and valid JSON
3. ✓ IDE restarted after config change
4. ✓ API keys set (for semantic search)
5. ✓ ChunkHound indexed: `chunkhound index`

**Test MCP Server Manually:**
```bash
# Start MCP server
chunkhound mcp

# Should start without errors
# Press Ctrl+C to stop
```

**Check IDE Logs:**
- Claude Desktop: `~/Library/Logs/Claude/`
- VS Code: View → Output → MCP
- Cursor: Similar to VS Code

---

## Real-World Use Cases

### Use Case 1: Onboarding New Developers

**Scenario:** New developer joins team, needs to understand large codebase

**Workflow:**
```bash
# Developer clones repository
git clone https://github.com/company/product
cd product

# Sets up ChunkHound
cp .chunkhound.json.template .chunkhound.json
export OPENAI_API_KEY="their-key"
chunkhound index

# Configures Claude Desktop
# (MCP config provided by team)
```

**AI-Assisted Exploration:**
```
Developer: "How does user authentication work in this codebase?"

AI (using ChunkHound):
- Finds authentication middleware
- Discovers JWT token validation
- Shows session management code
- Identifies related security logging
- Provides architectural overview

Developer: "Where are API rate limits implemented?"

AI (using ChunkHound):
- Locates rate limiting middleware
- Shows Redis integration for tracking
- Finds configuration management
- Displays error handling for rate-limited requests
```

**Benefits:**
- Faster onboarding (days instead of weeks)
- Self-service exploration
- Contextually complete code examples
- Reduced senior developer interruptions

### Use Case 2: Security Audit

**Scenario:** Security team audits codebase for vulnerabilities

**Setup:**
```json
{
  "embedding": {
    "provider": "voyageai",
    "model": "voyage-code-2"
  },
  "indexing": {
    "include": [
      "src/**/*.{ts,js,py}",
      "lib/**/*.{ts,js,py}"
    ],
    "exclude": [
      "**/*.test.*"
    ]
  }
}
```

**Audit Queries:**
```
"Find all SQL query construction code"
→ Identifies potential SQL injection vectors

"Show me password handling and hashing"
→ Verifies secure password practices

"Find all external API calls with user input"
→ Checks for proper input sanitization

"Show authentication and authorization checks"
→ Ensures consistent security enforcement

"Find all file system operations"
→ Checks for path traversal vulnerabilities
```

**Multi-Hop Advantage:**
- Discovers complete attack surfaces (all code paths)
- Finds indirect vulnerabilities (multi-hop relationships)
- Provides architectural context for risk assessment

### Use Case 3: Refactoring Legacy Code

**Scenario:** Team needs to refactor deprecated API usage

**Goal:** Replace all uses of `old-http-client` with `new-fetch-api`

**ChunkHound Workflow:**

```
Step 1: Discovery
Query: "Find all HTTP client usage"
Result: Semantic search finds both old and new API usage

Step 2: Pattern Analysis
Regex: "import.*old-http-client"
Result: Exact list of files importing old API

Step 3: Implementation Patterns
Query: "Show me how the new fetch API is used"
Result: Examples of new API usage in codebase

Step 4: Related Dependencies
Multi-hop search discovers:
- Error handling tied to old API
- Retry logic specific to old client
- Mocking in tests
- Configuration management
```

**Outcome:**
- Comprehensive understanding of refactoring scope
- Examples of new patterns already in use
- Identification of related code requiring updates

### Use Case 4: API Documentation Generation

**Scenario:** Team needs to document internal APIs for developer portal

**Setup:**
```json
{
  "embedding": {
    "provider": "openai"
  },
  "indexing": {
    "include": [
      "src/api/**/*.ts",
      "src/controllers/**/*.py",
      "docs/api/**/*.md"
    ]
  }
}
```

**Documentation Workflow:**
```
Query: "Find all API endpoints for user management"

ChunkHound provides:
- Endpoint definitions
- Request/response schemas
- Validation logic
- Related documentation (if exists)

AI generates:
- API reference documentation
- Example requests/responses
- Error code descriptions
- Authentication requirements
```

**Multi-Hop Enhancement:**
- Links endpoints to business logic
- Discovers middleware and interceptors
- Shows complete request lifecycle

### Use Case 5: Bug Investigation

**Scenario:** Production bug in payment processing, need to understand full flow

**Investigation Query:**
```
"How does payment processing work from checkout to confirmation?"
```

**ChunkHound Multi-Hop Discovery:**
```
Frontend: Checkout Component
  ↓
API: POST /api/payments
  ↓
Controller: PaymentController.create()
  ↓
Service: PaymentService.processPayment()
  ↓
Payment Gateway: StripeClient.charge()
  ↓
Database: Transaction.create()
  ↓
Event: PaymentSuccessEvent
  ↓
Notification: EmailService.sendReceipt()
```

**Follow-Up Queries:**
```
"Show me error handling in payment flow"
→ Discovers try/catch blocks, error logging

"Find all places where payment status is checked"
→ Identifies all status-dependent logic

"Show me payment-related tests"
→ Finds test cases for verification
```

**Benefits:**
- Complete architectural understanding in minutes
- Identifies all code paths involved in bug
- Provides context for root cause analysis

### Use Case 6: Polyglot Microservices

**Scenario:** Microservices architecture with Python backend, TypeScript frontend, Go services

**Configuration:**
```json
{
  "embedding": {
    "provider": "voyageai",
    "model": "voyage-code-2"
  },
  "indexing": {
    "include": [
      "backend/**/*.py",
      "frontend/**/*.{ts,tsx}",
      "services/**/*.go",
      "proto/**/*.proto"
    ]
  }
}
```

**Cross-Service Query:**
```
"How does the frontend communicate with the user service?"
```

**ChunkHound Discovers:**
```
Frontend (TypeScript):
- API client making requests
- Type definitions for user data

Backend Gateway (Python):
- API endpoint receiving request
- Request validation

User Service (Go):
- gRPC service implementation
- Database queries

Proto Definitions:
- Shared service contracts
```

**Value:**
- Language-agnostic semantic search
- Complete understanding across service boundaries
- Identifies integration patterns

### Use Case 7: Compliance and Licensing

**Scenario:** Legal team needs to verify third-party license compliance

**Query:**
```
"Find all third-party library imports and dependencies"
```

**Regex Patterns:**
```
Python: "^import\s+(?!app|lib|src)"
JavaScript: "from ['\"](?!@/|@app|./|../)"
Go: "^import.*github.com"
```

**ChunkHound Workflow:**
1. Regex search finds all import statements
2. Semantic search identifies library usage patterns
3. AI extracts unique library names
4. Generates compliance report

**Outcome:**
- Complete dependency inventory
- Usage context for each library
- License compliance verification

### Use Case 8: Performance Optimization

**Scenario:** Application slow, need to find performance bottlenecks

**Investigation Queries:**
```
"Find all database queries"
→ Identifies N+1 query patterns

"Show me all loops iterating over large data"
→ Finds potential algorithmic inefficiencies

"Find caching implementations"
→ Verifies caching strategy

"Show me all API calls to external services"
→ Identifies network bottlenecks
```

**Multi-Hop Analysis:**
```
Query: "How is user data fetched and rendered?"

Discovery:
1. API endpoint fetching users
2. Database query (SELECT * - no pagination!)
3. Response serialization (full objects)
4. Frontend rendering (iterating all users)

Optimization opportunities identified:
- Add pagination to query
- Implement cursor-based pagination
- Lazy load user details
- Cache user list
```

### Use Case 9: Incident Response

**Scenario:** Production outage, need to quickly understand system behavior

**Real-Time Investigation:**
```
Query: "Show me all error logging in the authentication system"

ChunkHound finds:
- Error handlers
- Logging statements
- Exception definitions
- Monitoring integration

Query: "Find all retry and fallback logic"

Discovers:
- Retry decorators/wrappers
- Circuit breakers
- Fallback implementations
- Timeout configurations
```

**Value:**
- Rapid incident understanding
- Identifies monitoring gaps
- Provides context for hotfix development

### Use Case 10: Technical Debt Assessment

**Scenario:** Team needs to quantify and prioritize technical debt

**Queries:**
```
"Find all TODO and FIXME comments"
→ Identifies acknowledged debt

"Show me all deprecated API usage"
→ Finds upgrade requirements

"Find duplicate or similar code"
→ Identifies refactoring opportunities (via semantic similarity)

"Show me all complex conditional logic"
→ Finds simplification candidates
```

**Semantic Debt Discovery:**
```
Query: "Find similar authentication implementations"

ChunkHound discovers:
- 5 different login handlers
- 3 token validation approaches
- 2 session management patterns

Recommendation:
- Standardize on single approach
- Refactor into shared library
- Estimate: 3 weeks to unify
```

---

## Conclusion

ChunkHound represents a significant advancement in code intelligence tools for AI assistants. Its combination of cAST-aware chunking, multi-hop semantic search, and local-first architecture makes it uniquely suited for modern development workflows.

### Key Takeaways

1. **Semantic Search Revolutionizes Code Exploration:** Natural language queries provide contextually complete code understanding, drastically reducing time to comprehension.

2. **Multi-Hop Search Discovers Architecture:** Traditional search finds direct matches; multi-hop search reveals relationships and architectural patterns across the codebase.

3. **Local-First Enables Privacy and Performance:** No cloud dependencies means faster responses, complete privacy, and zero per-token costs.

4. **MCP Integration Makes AI Assistants Smarter:** Seamless integration with Claude, Cursor, Windsurf, and VS Code turns AI assistants into true codebase experts.

5. **Incremental Updates Enable Real-Time Workflows:** Smart differential indexing keeps the knowledge base current without manual re-indexing.

6. **Monorepo-Ready with Adaptive Scaling:** From 10KB projects to 10M+ LOC monorepos, ChunkHound adapts its search strategy and token budgets automatically.

### When to Use ChunkHound

**Ideal Scenarios:**
- Large codebases (>10,000 files)
- Polyglot projects (multiple languages)
- Onboarding new developers
- Security audits and compliance
- Refactoring legacy code
- Bug investigation across services
- Architectural understanding
- Technical debt assessment

**Less Suitable Scenarios:**
- Very small projects (<100 files) - overhead may not be justified
- Highly dynamic codebases with constant churn - frequent re-indexing needed
- Projects without AI assistant integration - ChunkHound designed for MCP workflow

### Future Considerations

**Tracking GitHub Issues:**
- #93: MCP reconnection issues
- #87: Multiple repository workspace support
- #53: HTTP MCP server stability

**Recommended Monitoring:**
- Watch ChunkHound GitHub releases for new language support
- Monitor MCP specification evolution
- Track embedding model improvements (better quality, lower costs)

### Getting Started Checklist

```bash
# 1. Install ChunkHound
curl -LsSf https://astral.sh/uv/install.sh | sh
uv tool install chunkhound

# 2. Configure embedding provider
export OPENAI_API_KEY="sk-your-key"

# 3. Create project configuration
cat > .chunkhound.json << 'EOF'
{
  "embedding": {
    "provider": "openai",
    "model": "text-embedding-3-small"
  },
  "indexing": {
    "exclude": [
      "**/node_modules/**",
      "**/dist/**",
      "**/*.test.*"
    ]
  }
}
EOF

# 4. Add to .gitignore
echo ".chunkhound.json" >> .gitignore
echo ".chunkhound/" >> .gitignore

# 5. Index codebase
chunkhound index

# 6. Configure MCP server (Claude Desktop example)
# Edit: ~/Library/Application Support/Claude/claude_desktop_config.json
{
  "mcpServers": {
    "chunkhound": {
      "command": "uv",
      "args": ["run", "chunkhound", "mcp"],
      "env": {
        "OPENAI_API_KEY": "sk-your-key"
      }
    }
  }
}

# 7. Restart Claude Desktop

# 8. Test with query
# "How does authentication work in this codebase?"
```

### Resources

- **Official Documentation:** https://chunkhound.github.io/
- **GitHub Repository:** https://github.com/chunkhound/chunkhound
- **PyPI Package:** https://pypi.org/project/chunkhound/
- **MCP Specification:** https://modelcontextprotocol.io/
- **cAST Paper:** Carnegie Mellon University research

### License

ChunkHound is released under the MIT License.

---

**Document Version:** 2.0
**Last Updated:** November 20, 2025
**Research Source:** Official documentation, GitHub repository, GitHub issues, web research
**Generated By:** Claude Code Agent
