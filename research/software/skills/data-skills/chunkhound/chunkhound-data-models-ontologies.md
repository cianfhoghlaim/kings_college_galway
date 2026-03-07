# ChunkHound Data Models, Ontologies, and Semantic Structure

**Research Date:** 2025-11-20
**Source:** https://chunkhound.github.io/ and https://github.com/chunkhound/chunkhound
**Version:** Based on main branch analysis

## Executive Summary

ChunkHound is a local-first semantic code search tool that transforms codebases into searchable knowledge bases using the **cAST (Chunking via Abstract Syntax Trees)** algorithm from Carnegie Mellon University. This document provides comprehensive technical documentation of ChunkHound's data models, ontologies, type systems, and semantic structures.

### Key Performance Metrics
- **4.3 point gain** in Recall@5 on RepoEval retrieval
- **2.67 point gain** in Pass@1 on SWE-bench generation
- **29+ language support** via Tree-sitter parsers
- **~5ms query performance** with HNSW vector indexing

---

## Table of Contents

1. [Core Data Models](#core-data-models)
2. [Type System](#type-system)
3. [The cAST Algorithm](#the-cast-algorithm)
4. [Database Schemas](#database-schemas)
5. [Embedding Structure](#embedding-structure)
6. [Search Models](#search-models)
7. [Configuration Schemas](#configuration-schemas)
8. [Parser Ontologies](#parser-ontologies)
9. [Research Service Models](#research-service-models)
10. [Registry Architecture](#registry-architecture)

---

## Core Data Models

### 1. Chunk Model

**Location:** `chunkhound/core/models/chunk.py`

The `Chunk` is the fundamental atomic unit representing a semantic code segment. It's implemented as a frozen (immutable) dataclass.

#### Core Fields

```python
@dataclass(frozen=True)
class Chunk:
    # Required Fields
    symbol: str                    # Function, class, or element identifier
    start_line: LineNumber         # Beginning position (1-based)
    end_line: LineNumber           # Ending position (1-based, inclusive)
    code: str                      # Source content
    chunk_type: ChunkType          # Classification of semantic unit
    file_id: FileId                # Parent file reference
    language: Language             # Programming language designation

    # Optional Fields
    id: ChunkId | None = None                # Unique identifier for persisted chunks
    file_path: FilePath | None = None        # Source file location
    parent_header: str | None = None         # Hierarchy marker for nested content
    start_byte: ByteOffset | None = None     # Byte position start
    end_byte: ByteOffset | None = None       # Byte position end
    created_at: datetime | None = None       # Initial indexing timestamp
    updated_at: datetime | None = None       # Last modification timestamp
    metadata: dict[str, Any] | None = None   # Language-specific attributes
```

#### Validation Rules

- Positive line numbers required
- Start line cannot exceed end line
- Code content cannot be empty
- Non-negative byte offsets (if provided)
- Start byte cannot exceed end byte

#### Key Methods & Properties

**Conversion Methods:**
- `from_dict()` - Dictionary deserialization with backward compatibility
- `to_dict()` - Dictionary serialization

**Measurements:**
- `line_count` - Number of lines in chunk
- `char_count` - Character count
- `byte_count` - Byte size

**Classification:**
- `is_code_chunk()` - Returns true for code content
- `is_documentation_chunk()` - Returns true for documentation

**Range Operations:**
- `contains_line(line_number)` - Check if line is within chunk
- `overlaps_with(other_chunk)` - Check for overlap with another chunk

**Builders (Immutability Pattern):**
- `with_id(id)` - Create new instance with ID
- `with_file_path(path)` - Create new instance with file path

---

### 2. File Model

**Location:** `chunkhound/core/models/file.py`

Represents an indexed source code file with metadata and change tracking.

#### Structure

```python
@dataclass(frozen=True)
class File:
    id: FileId | None                 # Unique identifier (optional for new files)
    path: FilePath                    # Relative path with forward slashes
    mtime: Timestamp                  # Unix timestamp of last modification
    language: Language                # Programming language classification
    size_bytes: int                   # File size in bytes
    content_hash: str | None = None   # Checksum for change detection
    created_at: datetime | None = None
    updated_at: datetime | None = None
```

#### Factory Methods

- `from_path(path)` - Creates File instance from filesystem, extracting metadata and detecting language
- `from_dict(data)` - Deserializes File from dictionary with type coercion
- `to_dict()` - Serializes to dictionary

#### Path Properties

Read-only convenience properties:
- `name` - File basename
- `extension` - File extension
- `stem` - Filename without extension
- `parent_dir` - Parent directory
- `relative_path` - Path relative to workspace

#### Builder Methods

- `with_id(id)` - Returns new instance with ID
- `with_updated_mtime(mtime)` - Returns new instance with updated modification time

---

### 3. Embedding Model

**Location:** `chunkhound/core/models/embedding.py`

Represents a vector embedding for code chunks with similarity computation capabilities.

#### Primary Model

```python
@dataclass(frozen=True)
class Embedding:
    chunk_id: ChunkId              # Reference to associated code chunk
    provider: ProviderName         # Embedding service (e.g., "openai")
    model: ModelName               # Generation model identifier
    dims: Dimensions               # Vector dimensionality (positive integer)
    vector: EmbeddingVector        # Numeric array of embedding values
    created_at: datetime | None = None
```

#### Key Capabilities

**Similarity Computations:**
- `dot_product(other)` - Dot product similarity
- `cosine_similarity(other)` - Cosine similarity measure
- `euclidean_distance(other)` - Euclidean distance

**Vector Operations:**
- `normalize()` - Returns L2-normalized embedding
- `is_compatible_with(other)` - Validates dimensions match

**Serialization:**
- `from_dict(data)` - Dictionary deserialization
- `to_dict()` - Dictionary serialization

#### EmbeddingResult Model

Represents batch embedding generation output:

```python
@dataclass
class EmbeddingResult:
    embeddings: list[EmbeddingVector]  # List of vector arrays
    model: ModelName                   # Generation model name
    provider: ProviderName             # Service provider
    dims: Dimensions                   # Shared dimensionality
    total_tokens: int | None = None    # Optional token usage count
```

**Methods:**
- `to_embeddings(chunk_ids)` - Converts batch results to individual Embedding objects
- `count` - Property returning number of embeddings

---

## Type System

**Location:** `chunkhound/core/types/common.py`

ChunkHound employs a sophisticated type system using Python's `NewType` for semantic type safety and enums for classifications.

### Type Aliases

#### String-Based Types
```python
ProviderName = NewType('ProviderName', str)  # AI provider identifier
ModelName = NewType('ModelName', str)        # Model identifier
FilePath = NewType('FilePath', str)          # File path as string
```

#### Numeric Types
```python
ChunkId = NewType('ChunkId', int)            # Database identifier for chunks
FileId = NewType('FileId', int)              # Database identifier for files
LineNumber = NewType('LineNumber', int)      # 1-based line numbering
ByteOffset = NewType('ByteOffset', int)      # Byte positions in files
Timestamp = NewType('Timestamp', float)      # Unix timestamps
Distance = NewType('Distance', float)        # Vector similarity scores
Dimensions = NewType('Dimensions', int)      # Embedding dimensions
```

#### Complex Types
```python
EmbeddingVector = list[float]                # Vector embedding representation
```

---

### ChunkType Enum

Categorizes semantic content types with hierarchical classification.

```python
class ChunkType(Enum):
    # Code Structures
    FUNCTION = "function"
    METHOD = "method"
    CLASS = "class"
    INTERFACE = "interface"
    STRUCT = "struct"
    ENUM = "enum"
    NAMESPACE = "namespace"
    MODULE = "module"
    CONSTANT = "constant"
    VARIABLE = "variable"

    # Documentation
    COMMENT = "comment"
    DOCSTRING = "docstring"
    HEADER_1 = "header_1"
    HEADER_2 = "header_2"
    HEADER_3 = "header_3"
    HEADER_4 = "header_4"
    HEADER_5 = "header_5"
    HEADER_6 = "header_6"
    PARAGRAPH = "paragraph"
    CODE_BLOCK = "code_block"

    # Configuration
    TABLE = "table"
    KEY_VALUE = "key_value"
    ARRAY = "array"

    # Generic
    BLOCK = "block"
    UNKNOWN = "unknown"
```

#### Properties

- `is_code` - Returns True for code structure types
- `is_documentation` - Returns True for documentation types

#### Conversion Methods

- `from_string(value)` - Creates enum from string with fallback to UNKNOWN
- `from_file_extension(ext)` - Creates enum based on file extension

---

### Language Enum

Represents supported programming languages and file types.

```python
class Language(Enum):
    # Programming Languages (Tree-sitter)
    PYTHON = "python"
    JAVASCRIPT = "javascript"
    TYPESCRIPT = "typescript"
    JSX = "jsx"
    TSX = "tsx"
    JAVA = "java"
    KOTLIN = "kotlin"
    GROOVY = "groovy"
    C = "c"
    CPP = "cpp"
    CSHARP = "csharp"
    GO = "go"
    RUST = "rust"
    HASKELL = "haskell"
    SWIFT = "swift"
    BASH = "bash"
    MATLAB = "matlab"
    MAKEFILE = "makefile"
    OBJECTIVE_C = "objective_c"
    PHP = "php"
    VUE = "vue"
    ZIG = "zig"

    # Configuration Languages (Tree-sitter)
    JSON = "json"
    YAML = "yaml"
    TOML = "toml"
    HCL = "hcl"
    MARKDOWN = "markdown"

    # Text-based (Custom parsers)
    TEXT = "text"
    PDF = "pdf"

    UNKNOWN = "unknown"
```

#### Properties

- `is_programming_language` - True for programming languages
- `supports_classes` - True for OOP languages
- `supports_interfaces` - True for languages with interface support
- `is_structured_config_language` - True for JSON, YAML, TOML, HCL

#### Methods

- `from_string(value)` - String to enum conversion
- `from_file_extension(ext)` - File extension to language detection
- `get_file_patterns()` - Returns glob patterns for the language

---

## The cAST Algorithm

### Overview

**cAST (Chunking via Abstract Syntax Trees)** is a structure-aware code segmentation algorithm that preserves semantic meaning while enforcing size constraints.

**Academic Reference:** Zhang et al. (2025) - "cAST: Enhancing Code Retrieval-Augmented Generation with Structural Chunking via Abstract Syntax Tree"
**arXiv:** https://arxiv.org/abs/2506.15655

### Algorithm Parameters

```python
max_chunk_size: int = 1200          # Non-whitespace character limit
max_token_limit: int = 6000         # Estimated token limit (chars/3.5)
max_line_gap: int = 5               # Maximum gap for merging chunks
max_cross_concept_gap: int = 1      # Gap limit for cross-concept merging
min_chunk_size: int = 20            # Minimum viable chunk size
```

### Three-Phase Process

#### Phase 1: Parse & Extract

**Tree-sitter Parsing:**
- Universal AST parsing across 29+ languages
- Language-agnostic semantic preservation
- Zero-copy parsing with byte-range references

**Concept Extraction:**
Traverses AST to identify semantic units:

```python
class ConceptType(Enum):
    DEFINITION = "definition"    # Functions, classes, methods
    BLOCK = "block"              # Control flow, code segments
    COMMENT = "comment"          # Comments and documentation
    IMPORT = "import"            # Import/include statements
    STRUCTURE = "structure"      # High-level containers
```

#### Phase 2: Split Large Nodes

**Content-Aware Splitting:**

1. **Minified Code Detection** - Identifies long lines without breaks
2. **Line-Based Division** - Splits at natural line boundaries
3. **Emergency Character Split** - Handles pathological cases

**Splitting Priorities:**
- Preserve statement boundaries (semicolons, braces)
- Maintain line integrity
- Avoid mid-token splits
- Respect indentation levels

#### Phase 3: Merge Small Adjacent Chunks

**Greedy Merging Strategy:**

Combines adjacent chunks respecting:

```python
Merge Constraints:
├── Size: Keep under 1200 non-whitespace chars
├── Token Limit: Stay under 6000 estimated tokens
├── Line Gap: Max 5-line gap (1 for cross-concept)
├── Concept Compatibility:
│   ├── DEFINITION ↔ COMMENT ✓
│   ├── DEFINITION ↔ DEFINITION ✓
│   ├── BLOCK ↔ COMMENT ✓
│   ├── BLOCK ↔ BLOCK ✓
│   └── Others: More restrictive
└── Nesting: Don't merge nested chunks
```

**Concept-Specific Merging Rules:**

- **DEFINITION chunks**: Remain intact unless oversized; aggressive sibling merging
- **BLOCK chunks**: Enable liberal merging within proximity
- **COMMENT chunks**: Merge only consecutive comments (gap ≤ 1 line)
- **IMPORT chunks**: Merge related imports together

#### Deduplication

Final phase removes identical content chunks, preserving highest-specificity:

**Priority Order:**
1. DEFINITION (highest)
2. IMPORT
3. COMMENT
4. BLOCK
5. STRUCTURE (lowest)

### Size Metrics

**ChunkMetrics Class:**

```python
class ChunkMetrics:
    @staticmethod
    def non_whitespace_chars(text: str) -> int:
        """Primary size metric - counts non-whitespace chars"""

    @staticmethod
    def estimated_tokens(text: str) -> int:
        """Estimates tokens using 3.5 chars/token ratio"""

    @staticmethod
    def is_within_limits(text: str) -> bool:
        """Checks both char and token limits"""
```

### Implementation Location

**Primary File:** `chunkhound/parsers/universal_parser.py`

**Key Classes:**
- `TreeSitterEngine` - AST parsing engine
- `ConceptExtractor` - Semantic unit extraction
- `ChunkMerger` - Merge logic implementation
- `ChunkSplitter` - Split logic for oversized chunks

---

## Database Schemas

ChunkHound supports two database backends with different schemas optimized for their respective strengths.

### DuckDB Schema

**Location:** `chunkhound/providers/database/duckdb_provider.py`

DuckDB provides OLAP columnar storage with HNSW vector indexing for fast semantic search.

#### Files Table

```sql
CREATE TABLE files (
    id INTEGER PRIMARY KEY DEFAULT nextval('files_id_seq'),
    path TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    extension TEXT,
    size INTEGER,
    modified_time TIMESTAMP,
    content_hash TEXT,
    language TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_files_path ON files(path);
CREATE INDEX idx_files_language ON files(language);
```

#### Chunks Table

```sql
CREATE TABLE chunks (
    id INTEGER PRIMARY KEY DEFAULT nextval('chunks_id_seq'),
    file_id INTEGER REFERENCES files(id),
    chunk_type TEXT NOT NULL,
    symbol TEXT,
    code TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    start_byte INTEGER,
    end_byte INTEGER,
    language TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_chunks_file_id ON chunks(file_id);
CREATE INDEX idx_chunks_type ON chunks(chunk_type);
CREATE INDEX idx_chunks_symbol ON chunks(symbol);
```

#### Embeddings Tables (Dimension-Specific)

DuckDB creates separate tables per embedding dimension for optimal vector operations:

```sql
CREATE TABLE embeddings_{dims} (
    id INTEGER PRIMARY KEY DEFAULT nextval('embeddings_id_seq'),
    chunk_id INTEGER NOT NULL,
    provider TEXT NOT NULL,
    model TEXT NOT NULL,
    embedding FLOAT[{dims}],
    dims INTEGER NOT NULL DEFAULT {dims},
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Vector Index (HNSW)
CREATE INDEX idx_hnsw_{dims} ON embeddings_{dims}
USING HNSW (embedding)
WITH (metric = 'cosine');

-- Standard Indexes
CREATE INDEX idx_{dims}_chunk_id ON embeddings_{dims}(chunk_id);
CREATE INDEX idx_{dims}_provider_model ON embeddings_{dims}(provider, model);
```

**Example Tables:**
- `embeddings_1536` - OpenAI text-embedding-3-small
- `embeddings_1024` - VoyageAI voyage-code-2
- `embeddings_768` - Sentence transformers models

**HNSW Performance:**
- ~5ms query time for nearest neighbor search
- Cosine similarity metric by default
- Automatic index building on first query

---

### LanceDB Schema

**Location:** `chunkhound/providers/database/lancedb_provider.py`

LanceDB uses Apache Arrow format for columnar vector storage with experimental support.

#### Files Table Schema (PyArrow)

```python
files_schema = pa.schema([
    pa.field("id", pa.int64()),
    pa.field("path", pa.string()),
    pa.field("size", pa.int64()),
    pa.field("modified_time", pa.float64()),
    pa.field("content_hash", pa.string()),
    pa.field("indexed_time", pa.float64()),
    pa.field("language", pa.string()),
    pa.field("encoding", pa.string()),
    pa.field("line_count", pa.int64())
])
```

#### Chunks Table Schema (PyArrow)

Dynamic schema based on embedding dimensions:

```python
chunks_schema = pa.schema([
    pa.field("id", pa.int64()),
    pa.field("file_id", pa.int64()),
    pa.field("content", pa.string()),
    pa.field("start_line", pa.int64()),
    pa.field("end_line", pa.int64()),
    pa.field("chunk_type", pa.string()),
    pa.field("language", pa.string()),
    pa.field("name", pa.string()),
    pa.field("embedding", pa.list_(pa.float32(), list_size=dims)),  # Fixed-size
    pa.field("provider", pa.string()),
    pa.field("model", pa.string()),
    pa.field("created_time", pa.float64())
])
```

#### Vector Index Specifications

```python
Index Configuration:
├── Type: IVF_HNSW_SQ (when configured)
├── Distance Metric: Cosine (default)
├── Training Requirements:
│   ├── IVF PQ: Minimum 1,000 embeddings
│   └── General: Minimum 256 rows
└── Schema Evolution: Auto-migration to fixed-size lists
```

**Index Creation:**
- Deferred until sufficient data available
- Auto-configured based on dimensionality
- Supports custom index parameters

---

## Embedding Structure

### Embedding Service Architecture

**Location:** `chunkhound/services/embedding_service.py`

#### Provider Interface

```python
class EmbeddingProvider(Protocol):
    """Interface for embedding providers"""

    async def embed(self, texts: list[str]) -> EmbeddingResult:
        """Generate embeddings for text batch"""

    def get_recommended_concurrency(self) -> int:
        """Optimal parallel batch count"""

    def get_max_tokens_per_batch(self) -> int:
        """Token limit per batch"""

    def get_max_documents_per_batch(self) -> int:
        """Document limit per batch"""

    @property
    def name(self) -> ProviderName:
        """Provider identifier"""

    @property
    def model(self) -> ModelName:
        """Model identifier"""
```

### Batching Strategy

**Token-Aware Batching:**

```python
Batch Parameters:
├── Max Chunks per Batch: 300
├── Provider Token Limit: With 20% safety margin
├── Concurrency: Auto-detected from provider (default 8)
└── Adaptive Retry: Up to 3 levels of batch splitting
```

**Batching Algorithm:**

1. **Calculate Token Budget** - Provider limit × 0.8 safety factor
2. **Estimate Chunk Tokens** - Characters ÷ 3.5
3. **Group Chunks** - Fill batches up to token budget
4. **Limit Batch Size** - Cap at 300 chunks/batch

**Retry Strategy:**

```python
On Token Limit Error:
├── Split batch in half
├── Retry each half independently
├── Recurse up to 3 levels deep
└── Fail if still over limit
```

### Concurrency Management

```python
class EmbeddingService:
    async def _generate_embeddings_in_batches(
        self,
        batches: list[EmbeddingBatch]
    ) -> list[Embedding]:
        """Process batches with controlled concurrency"""

        semaphore = asyncio.Semaphore(self.concurrency)

        async with semaphore:
            results = await asyncio.gather(
                *[self._process_batch(batch) for batch in batches],
                return_exceptions=True
            )

        return results
```

### Embedding Metadata Storage

```python
Embedding Record:
├── chunk_id: ChunkId
├── provider: ProviderName
├── model: ModelName
├── dims: Dimensions
├── vector: EmbeddingVector
└── created_at: datetime

Storage Strategy:
├── Dimension-specific tables (DuckDB)
├── Deduplication by (chunk_id, provider, model)
├── Batch insert optimization
└── Existing embedding filtration
```

### Data Flow Pipeline

```
1. generate_embeddings_for_chunks(chunk_ids, texts)
   ↓
2. _filter_existing_embeddings()
   ↓ (Skip already embedded)
3. _create_token_aware_batches()
   ↓ (Group by token limits)
4. _generate_embeddings_in_batches()
   ↓ (Parallel processing)
5. _db.insert_embeddings_batch()
   ↓ (Persist to database)
6. Return list[Embedding]
```

---

## Search Models

### Search Service API

**Location:** `chunkhound/services/search_service.py`

#### Query Structures

**Semantic Search Parameters:**

```python
class SemanticSearchParams:
    query: str                        # Natural language query
    page_size: int = 10               # Results per page
    offset: int = 0                   # Pagination offset
    threshold: float | None = None    # Similarity threshold
    provider: str | None = None       # Override embedding provider
    model: str | None = None          # Override model
    path_filter: str | None = None    # Path scope (e.g., 'src/')
    force_strategy: str | None = None # 'single_hop' or 'multi_hop'
```

**Regex Search Parameters:**

```python
class RegexSearchParams:
    pattern: str                      # Regular expression
    page_size: int = 10               # Results per page
    offset: int = 0                   # Pagination offset
    path_filter: str | None = None    # Path scope restriction
```

**Hybrid Search Parameters:**

```python
class HybridSearchParams:
    query: str                        # Primary semantic query
    regex_pattern: str | None = None  # Supplementary pattern
    page_size: int = 10               # Results per page
    offset: int = 0                   # Pagination offset
    semantic_weight: float = 0.7      # Semantic result weighting
    threshold: float | None = None    # Similarity threshold
```

### Result Models

**Return Format:**

```python
SearchResults = tuple[
    list[dict[str, Any]],  # Enhanced result dictionaries
    dict[str, Any]         # Pagination metadata
]
```

**Result Dictionary Structure:**

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
    "similarity": float,      # Semantic search only
    "rerank_score": float,    # Multi-hop search only
    "parent_header": str | None,
    "metadata": dict[str, Any]
}
```

**Pagination Metadata:**

```python
{
    "offset": int,
    "page_size": int,
    "total": int | None,      # Total count when available
    "has_more": bool,
    "next_offset": int | None
}
```

### Multi-Hop Search Algorithm

**Location:** `chunkhound/services/search/multi_hop_strategy.py`

#### Algorithm Flow

```
Phase 1: Initial Search & Reranking
├── Execute single-hop search (up to 100 candidates)
├── Apply reranking model
└── Establish baseline relevance scores

Phase 2: Dynamic Expansion Loop
├── Select top candidates (score-based)
├── Find 20 similar chunks per candidate
├── Rerank consolidated result set
├── Check convergence conditions
└── Repeat or terminate

Phase 3: Final Filtering & Pagination
├── Apply similarity threshold
├── Sort by rerank score
├── Apply pagination
└── Return with metadata
```

#### Convergence Detection

**Five Termination Conditions:**

```python
Convergence Rules:
├── 1. Time Constraint: Stop after 5 seconds
├── 2. Volume Limit: Stop at 500 total candidates
├── 3. Candidate Quality: Require 5+ high-scoring candidates
├── 4. Score Degradation: Stop if scores drop ≥ 0.15
└── 5. Minimum Relevance: Halt if top-5 minimum < 0.3
```

#### Reranking Logic

```python
Reranking Flow:
├── Query: Original user query (fixed)
├── Documents: Consolidated candidate set (growing)
├── Reranker: External reranking provider
│   ├── Cohere Rerank API
│   ├── Text Embeddings Inference (TEI)
│   └── Auto-detection format
├── Output: Relevance scores per document
└── Update: Replace similarity with rerank scores
```

### Search Strategy Selection

**Automatic Strategy Selection:**

```python
Strategy Logic:
├── If multi-hop disabled: Use single-hop
├── If initial results < threshold: Try multi-hop
├── If convergence fails: Fall back to single-hop
└── Override with force_strategy parameter
```

### API Methods Summary

| Method | Async | Purpose |
|--------|-------|---------|
| `search_semantic()` | Yes | Vector similarity with auto strategy |
| `search_regex()` | No | Pattern matching on code |
| `search_regex_async()` | Yes | Non-blocking pattern matching |
| `search_hybrid()` | Yes | Combined semantic + regex |
| `get_chunk_context()` | No | Retrieve surrounding lines |
| `get_file_chunks()` | No | List all chunks in file |

---

## Configuration Schemas

### Main Configuration Model

**Location:** `chunkhound/core/config/config.py`

#### Config Class Hierarchy

```python
@dataclass
class Config:
    database: DatabaseConfig              # Database settings
    embedding: EmbeddingConfig | None     # Embedding provider config
    llm: LLMConfig | None                 # Language model settings
    mcp: MCPConfig                        # MCP server configuration
    indexing: IndexingConfig              # Indexing behavior
    debug: bool = False                   # Debug mode flag

    # Private fields (excluded from serialization)
    target_dir: Path | None = field(default=None, repr=False)
    embeddings_disabled: bool = field(default=False, repr=False)
```

#### Configuration Precedence

```
Configuration Sources (highest to lowest priority):
1. CLI arguments
2. Local .chunkhound.json in target directory
3. Config file via --config path
4. Environment variables
5. Default values
```

---

### Database Configuration

**Location:** `chunkhound/core/config/database_config.py`

```python
class DatabaseConfig(BaseModel):
    path: Path | None = None              # Database directory
    provider: Literal["duckdb", "lancedb"] = "duckdb"

    @validator("path")
    def convert_path(cls, v):
        """Convert string to Path object"""
        return Path(v) if v else None

    def is_configured(self) -> bool:
        """Check if path is set"""
        return self.path is not None
```

**Environment Variables:**
- `CHUNKHOUND_DATABASE__PATH`
- `CHUNKHOUND_DATABASE__PROVIDER`
- `CHUNKHOUND_DB_PATH` (legacy)

**CLI Arguments:**
- `--db` / `--database-path`
- `--database-provider`

**Default Paths:**
- DuckDB: `{path}/chunks.db`
- LanceDB: `{path}/lancedb/`

---

### Embedding Configuration

**Location:** `chunkhound/core/config/embedding_config.py`

```python
class EmbeddingConfig(BaseModel):
    # Provider Settings
    provider: Literal["openai", "voyageai"] = "openai"
    model: str | None = None              # Auto-defaults if None
    api_key: SecretStr | None = None      # Authentication
    base_url: str | None = None           # Custom API endpoint

    # Reranking Configuration
    rerank_model: str | None = None       # Rerank model name
    rerank_url: str | None = None         # Rerank endpoint
    rerank_format: Literal["cohere", "tei", "auto"] | None = None
    rerank_batch_size: int = 100          # Rerank batch size

    # Performance Settings (internal)
    batch_size: int = 100                 # Embedding batch size
    timeout: int = 30                     # Request timeout (seconds)
    max_retries: int = 3                  # Retry attempts
    max_concurrent_batches: int = 5       # Concurrent batch limit
```

**Provider Defaults:**

| Provider | Default Model | Dimensions |
|----------|---------------|------------|
| OpenAI | text-embedding-3-small | 1536 |
| VoyageAI | voyage-code-2 | 1024 |

**Validation Rules:**

- Relative rerank URLs require base_url
- Cohere format demands rerank_model
- API key required for most providers

**Environment Variables:**
- `CHUNKHOUND_EMBEDDING__PROVIDER`
- `CHUNKHOUND_EMBEDDING__MODEL`
- `CHUNKHOUND_EMBEDDING__API_KEY`
- `CHUNKHOUND_EMBEDDING__RERANK_MODEL`
- etc.

---

### Indexing Configuration

**Location:** `chunkhound/core/config/indexing_config.py`

```python
class IndexingConfig(BaseModel):
    # Behavior Settings
    force_reindex: bool = False           # Re-index all files
    batch_size: int = 50                  # Parser batch size
    db_batch_size: int = 1000             # Database batch size
    max_concurrent: int = 4               # Parser worker processes

    # File Discovery
    include: list[str] = Field(default_factory=_default_includes)
    exclude: list[str] = Field(default_factory=_default_excludes)
    exclude_mode: Literal["combined", "config_only", "gitignore_only"] = "combined"
    workspace_gitignore_nonrepo: bool = True

    # Performance Tuning
    per_file_timeout_seconds: float = 3.0
    config_file_size_threshold_kb: int = 20
```

#### Default Exclusions

**Categories:**

```python
Virtual Environments & Dependencies:
├── node_modules/
├── venv/, .venv/, env/
├── .git/, .svn/
├── vendor/
└── __pycache__/

Build Artifacts:
├── dist/, build/
├── target/
├── out/, output/
├── *.egg-info/
└── .next/, .nuxt/

IDE Files:
├── .vscode/
├── .idea/
├── *.swp, *.swo
└── .DS_Store

Generated/Minified:
├── *.min.js, *.min.css
├── bundle.*.js
├── *-lock.json, *.lock
└── coverage/

Large Config Files:
└── Files > 20KB in structured formats (JSON, YAML, TOML)
```

#### File Discovery Backends

```python
Discovery Strategy:
├── Git Repository (preferred):
│   ├── Uses 'git ls-files'
│   ├── Returns tracked + untracked non-ignored files
│   └── Applies pathspec filters for performance
└── Python Fallback:
    ├── Filesystem traversal
    ├── Applies .gitignore patterns
    └── Respects exclude configuration
```

**Environment Variables:**
- `CHUNKHOUND_INDEXING__EXCLUDE`
- `CHUNKHOUND_INDEXING__INCLUDE`
- `CHUNKHOUND_INDEXING__FORCE_REINDEX`
- etc.

---

### LLM Configuration

**Location:** `chunkhound/core/config/llm_config.py`

```python
class LLMConfig(BaseModel):
    # Provider Selection
    provider: Literal["openai", "ollama", "claude-code-cli", "codex-cli"]

    # Model Configuration
    utility_model: str | None = None      # Query expansion, classification
    synthesis_model: str | None = None    # Large context analysis

    # Authentication
    api_key: SecretStr | None = None
    base_url: str | None = None

    # Performance Settings
    timeout: int = 60
    max_retries: int = 3

    # Provider-Specific
    codex_reasoning_effort: Literal["low", "medium", "high"] | None = None
```

**Default Models by Provider:**

| Provider | Utility Model | Synthesis Model |
|----------|---------------|-----------------|
| OpenAI | gpt-5-nano | gpt-5 |
| Ollama | llama3.2 | llama3.2 |
| Claude Code CLI | claude-haiku-4-5 | claude-haiku-4-5 |
| Codex CLI | codex | codex |

**Environment Variables:**
- `CHUNKHOUND_LLM__PROVIDER`
- `CHUNKHOUND_LLM__API_KEY`
- `CHUNKHOUND_LLM__UTILITY_MODEL`
- `CHUNKHOUND_LLM__SYNTHESIS_MODEL`
- `CHUNKHOUND_LLM__CODEX_REASONING_EFFORT`

---

### MCP Configuration

**Location:** `chunkhound/core/config/mcp_config.py`

```python
class MCPConfig(BaseModel):
    # Server Settings
    server_mode: Literal["stdio", "http"] = "stdio"
    port: int = 5173                      # HTTP mode only
    host: str = "localhost"               # HTTP mode only

    # Capabilities
    enable_semantic_search: bool = True
    enable_regex_search: bool = True
    enable_code_research: bool = True

    # Performance
    max_search_results: int = 100
    default_page_size: int = 10
```

**Environment Variables:**
- `CHUNKHOUND_MCP__SERVER_MODE`
- `CHUNKHOUND_MCP__PORT`
- `CHUNKHOUND_MCP__ENABLE_CODE_RESEARCH`

---

## Parser Ontologies

### Universal Parser Architecture

**Location:** `chunkhound/parsers/universal_parser.py`

ChunkHound uses a three-tier parser architecture:

```
Universal Parser
├── TreeSitterEngine (AST generation)
├── ConceptExtractor (semantic units)
└── cAST Algorithm (chunking logic)
```

### Language Mapping System

Each supported language has a mapping class that defines how AST nodes translate to semantic concepts.

**Location:** `chunkhound/parsers/mappings/{language}.py`

#### Python Mapping

**File:** `chunkhound/parsers/mappings/python.py`

**AST Node → ChunkType Mappings:**

| Python Construct | AST Node Type | Concept | Metadata Captured |
|-----------------|---------------|---------|-------------------|
| Function | `function_definition` | DEFINITION | decorators, parameters, type_hints, is_async, is_generator |
| Async Function | `async_function_definition` | DEFINITION | kind: "async_function", async markers |
| Class | `class_definition` | DEFINITION | decorators, superclasses, is_abstract |
| Method | `function_definition` (in class) | DEFINITION | Same as function + class context |
| Docstring | `expression_statement(string)` | COMMENT | is_docstring: true, location |
| Comment | `comment` | COMMENT | comment_type: "line_comment" |
| Import | `import_statement` | IMPORT | type: "import" |
| From Import | `import_from_statement` | IMPORT | type: "from_import" |
| If/While/For | `if_statement`, etc. | BLOCK | control flow type |
| Top-level Dict/List | `assignment` (module level) | DEFINITION | chunk_type_hint: "object"/"array" |

**Metadata Extraction:**

```python
Function Metadata:
├── decorators: list[str]                 # @property, @staticmethod, etc.
├── parameters: list[dict]                # Name, type, default, kind (*args, **kwargs)
├── type_hints: dict[str, str]            # Parameter types + return type
├── is_async: bool                        # Async function flag
├── is_generator: bool                    # Contains yield statements
└── is_lambda: bool                       # Lambda function

Class Metadata:
├── decorators: list[str]                 # Class decorators
├── superclasses: list[str]               # Base classes
├── is_abstract: bool                     # Contains ABC or abstractmethod
└── methods: list[str]                    # Method names (for reference)

Docstring Metadata:
├── is_docstring: bool = True
├── docstring_type: Literal["module", "class", "function"]
├── raw_content: str                      # Unprocessed docstring
└── location: str                         # Parent context
```

**Filtering Logic:**

```python
def should_include_node(node, code) -> bool:
    # Exclude trivial implementations
    if len(code) < 20:
        return False

    # Exclude placeholder functions
    if is_function and code in ["pass", "...", "return None"]:
        return False

    return True
```

---

#### TypeScript Mapping

**File:** `chunkhound/parsers/mappings/typescript.py`

**TypeScript-Specific Constructs:**

| TypeScript Construct | AST Node Type | Concept | Metadata |
|---------------------|---------------|---------|----------|
| Interface | `interface_declaration` | DEFINITION | type_parameters, extends, members |
| Type Alias | `type_alias_declaration` | DEFINITION | type_parameters, type_definition |
| Enum | `enum_declaration` | DEFINITION | members, values |
| Namespace | `namespace_declaration` | DEFINITION | nested declarations |
| Decorator | `decorator` | Metadata | decorator_name, arguments |
| Type Annotation | `type_annotation` | Metadata | type_string |
| Generic Params | `type_parameters` | Metadata | constraints, defaults |

**Enhanced Metadata:**

```python
Function/Method Metadata:
├── parameters: list[dict]
│   ├── name: str
│   ├── type: str | None                  # Type annotation
│   ├── optional: bool                    # Has ? marker
│   ├── default: str | None               # Default value
│   └── rest: bool                        # ...args
├── return_type: str | None               # Return type annotation
├── type_parameters: list[str]            # Generic type params
├── access_modifier: Literal["public", "private", "protected"] | None
├── is_static: bool
├── is_readonly: bool
├── is_async: bool
└── decorators: list[dict]                # Name + arguments

Class Metadata:
├── implements: list[str]                 # Implemented interfaces
├── extends: str | None                   # Base class
├── type_parameters: list[str]            # Generic params
├── decorators: list[dict]
├── is_abstract: bool
└── members: dict                         # Properties, methods, accessors

Interface Metadata:
├── extends: list[str]                    # Extended interfaces
├── type_parameters: list[str]
├── members: list[dict]                   # Property signatures
└── call_signatures: list[str]            # Function signatures
```

**React Component Detection:**

```python
def is_react_component(name: str) -> bool:
    """Detect React components by capitalization"""
    return name[0].isupper() if name else False
```

---

#### Java Mapping

**File:** `chunkhound/parsers/mappings/java.py`

**Java-Specific Constructs:**

| Java Construct | AST Node Type | Concept | Notes |
|---------------|---------------|---------|-------|
| Class | `class_declaration` | DEFINITION | Supports inner classes |
| Interface | `interface_declaration` | DEFINITION | Method signatures |
| Enum | `enum_declaration` | DEFINITION | Enum constants |
| Annotation | `annotation_type_declaration` | DEFINITION | Custom annotations |
| Method | `method_declaration` | DEFINITION | Signature + body |
| Constructor | `constructor_declaration` | DEFINITION | Special method |
| Field | `field_declaration` | DEFINITION | Class variables |
| Package | `package_declaration` | IMPORT | Package statement |

**Metadata Extraction:**

```python
Method Metadata:
├── modifiers: list[str]                  # public, static, final, etc.
├── return_type: str
├── parameters: list[dict]
├── throws: list[str]                     # Exception types
├── type_parameters: list[str]            # <T extends Foo>
└── annotations: list[str]                # @Override, etc.

Class Metadata:
├── modifiers: list[str]                  # public, abstract, final
├── implements: list[str]
├── extends: str | None
├── type_parameters: list[str]
├── annotations: list[str]
└── is_inner_class: bool
```

---

### Markdown Mapping

**File:** `chunkhound/parsers/mappings/markdown.py`

**Markdown Structure Mapping:**

| Markdown Element | AST Node Type | ChunkType | Notes |
|-----------------|---------------|-----------|-------|
| # Heading | `atx_heading(level=1)` | HEADER_1 | Top-level sections |
| ## Heading | `atx_heading(level=2)` | HEADER_2 | Subsections |
| ### Heading | `atx_heading(level=3)` | HEADER_3 | Sub-subsections |
| #### to ###### | `atx_heading(level=4-6)` | HEADER_4-6 | Lower-level headings |
| Paragraph | `paragraph` | PARAGRAPH | Text blocks |
| Code Block | `fenced_code_block` | CODE_BLOCK | ```language |
| Table | `table` | TABLE | Structured data |
| List | `list` | BLOCK | Ordered/unordered |

**Metadata:**

```python
Code Block Metadata:
├── language: str | None                  # Syntax highlighting hint
├── raw_content: str                      # Code text
└── fence_char: Literal["```", "~~~"]

Header Metadata:
├── level: int                            # 1-6
├── text: str                             # Header text
└── anchor: str                           # Generated anchor ID

Table Metadata:
├── headers: list[str]
├── rows: list[list[str]]
└── alignment: list[Literal["left", "right", "center"]]
```

---

### JSON/YAML/TOML Mapping

**Location:** `chunkhound/parsers/mappings/{format}.py`

Configuration file parsers extract semantic structure:

**Mapping Rules:**

| Element | ChunkType | Conditions |
|---------|-----------|------------|
| Top-level object | TABLE | Root dictionary |
| Nested object | KEY_VALUE | Named dictionary |
| Array | ARRAY | List structure |
| Key-value pair | KEY_VALUE | Scalar value |

**Metadata:**

```python
Key-Value Metadata:
├── key: str                              # Property name
├── value_type: str                       # Type of value
├── depth: int                            # Nesting level
└── path: str                             # JSON path (e.g., "config.database.host")

Array Metadata:
├── key: str                              # Array name
├── item_count: int                       # Number of items
├── item_type: str                        # Homogeneous type (if applicable)
└── depth: int
```

---

### Parser Registry

**Location:** `chunkhound/registry/__init__.py`

#### Lazy Parser Loading

```python
class LazyLanguageParsers:
    """Thread-safe lazy parser instantiation"""

    def __init__(self):
        self._factories: dict[Language, Callable[[], Parser]] = {}
        self._instances: dict[Language, Parser] = {}
        self._lock = threading.Lock()

    def register(self, language: Language, factory: Callable[[], Parser]):
        """Register parser factory for language"""
        self._factories[language] = factory

    def get(self, language: Language) -> Parser:
        """Get parser instance, creating on first access"""
        if language not in self._instances:
            with self._lock:
                if language not in self._instances:
                    factory = self._factories.get(language)
                    if factory:
                        self._instances[language] = factory()

        return self._instances.get(language)
```

#### Registration Flow

```python
def _setup_language_parsers(config: Config) -> LazyLanguageParsers:
    """Register all language parsers"""
    parsers = LazyLanguageParsers()

    for language in Language:
        if language == Language.UNKNOWN:
            continue

        factory = get_parser_factory(language)
        if factory:
            parsers.register(language, factory)

    return parsers
```

---

## Research Service Models

**Location:** `chunkhound/services/research/models.py`

The research service implements breadth-first semantic exploration for comprehensive code understanding.

### BFSNode Model

Represents individual nodes in the semantic exploration graph:

```python
@dataclass
class BFSNode:
    # Query Information
    query: str                            # Search query for this node
    parent: BFSNode | None                # Parent node (None for root)
    children: list[BFSNode]               # Child nodes
    depth: int                            # Depth in exploration tree

    # Retrieved Content
    chunks: list[Chunk]                   # Relevant code chunks
    full_files: dict[str, str]            # Complete file contents

    # Analysis Results
    answer: str | None                    # Generated answer
    unanswered_aspects: list[str]         # Remaining questions

    # Resource Management
    token_budget: int                     # Allocated tokens for this node
    is_terminal: bool                     # No further expansion
```

### ResearchContext Model

Maintains traversal state during exploration:

```python
@dataclass
class ResearchContext:
    initial_query: str                    # Original user query
    ancestor_chain: list[BFSNode]         # Path from root to current
    exploration_path: list[str]           # Query text trail

    def get_depth(self) -> int:
        """Current depth in tree"""
        return len(self.ancestor_chain)

    def get_path_string(self) -> str:
        """Formatted path for display"""
        return " → ".join(self.exploration_path)
```

### Token Budget Configuration

Adaptive depth-dependent token allocation:

```python
Token Budgets by Depth:
├── Leaf Nodes (deepest analysis):
│   ├── File Content: 50,000 tokens
│   └── Answer Output: 21,000 tokens (18k + 3k bonus)
├── Mid-Level Nodes:
│   ├── File Content: 30,000 tokens
│   └── Answer Output: 15,000 tokens
└── Root Node (synthesis):
    ├── File Content: 10,000 tokens
    └── Answer Output: 11,000-19,000 tokens

Query Expansion: 10,000 tokens per iteration
Follow-up Generation: 8,000-15,000 tokens (reasoning model overhead)
```

### Single-Pass Architecture

Consolidated synthesis with larger context window:

```python
Single-Pass Budget:
├── Total Budget: 150,000 tokens
├── Output Allocation: 30,000 tokens
├── Context Window: 120,000 tokens
└── Use Case: Long-context models (GPT-4, Claude)
```

### Relevance Thresholds

```python
Relevance Configuration:
├── Initial Retrieval: 0.5 threshold (broad recall)
├── Reranking: Applied after initial retrieval
├── Node Similarity: LLM evaluation (0.2 reserved for future)
└── Final Filtering: User-configurable threshold
```

### Research Result Format

```python
Research Output Structure:
├── Summary: Executive overview
├── Architectural Overview: High-level design
├── Key Components:
│   ├── Component Name
│   ├── Location: file.ts:45-120
│   ├── Purpose: Description
│   └── Code Sample: Snippet
├── Relationships: Component interactions
├── Usage Examples: Real codebase examples
├── Implementation Recommendations: Best practices
└── Citations: [1] file.ts:line references
```

---

## Registry Architecture

**Location:** `chunkhound/registry/__init__.py`

The registry implements a dependency injection container for managing ChunkHound's components.

### ProviderRegistry Class

Central registry managing providers, parsers, and configuration:

```python
class ProviderRegistry:
    def __init__(self, config: Config):
        self._config = config
        self._providers: dict[str, Any] = {}
        self._language_parsers: LazyLanguageParsers = None

        self._setup_database_provider()
        self._setup_embedding_provider()
        self._setup_language_parsers()
```

### Provider Categories

#### 1. Database Providers

```python
def _setup_database_provider(self):
    """Initialize database backend"""
    if self._config.database.provider == "duckdb":
        provider = DuckDBProvider(self._config.database.path)
    elif self._config.database.provider == "lancedb":
        provider = LanceDBProvider(self._config.database.path)

    self._providers["database"] = provider
```

**Registered Providers:**
- `DuckDBProvider` - Default OLAP database with HNSW
- `LanceDBProvider` - Experimental Arrow-based storage

#### 2. Embedding Providers

```python
def _setup_embedding_provider(self):
    """Initialize embedding service"""
    if self._config.embeddings_disabled:
        self._providers["embedding"] = None
        return

    factory = EmbeddingProviderFactory(self._config.embedding)
    provider = factory.create()

    self._providers["embedding"] = provider
```

**Supported Providers:**
- `OpenAIEmbeddingProvider` - OpenAI text-embedding models
- `VoyageAIEmbeddingProvider` - Voyage AI code embeddings
- `OllamaEmbeddingProvider` - Local Ollama models

#### 3. Language Parsers

```python
def _setup_language_parsers(self):
    """Register lazy-loaded parsers"""
    self._language_parsers = LazyLanguageParsers()

    for language in Language:
        factory = get_parser_factory(language)
        if factory:
            self._language_parsers.register(language, factory)
```

**Parser Types:**
- **Tree-sitter parsers** - For programming languages (Python, TypeScript, etc.)
- **Structured parsers** - For JSON, YAML, TOML, HCL
- **Text parsers** - For Markdown, plain text
- **PDF parser** - For PDF documents (via pymupdf)

### Service Factory Methods

The registry creates higher-level services with injected dependencies:

```python
class ProviderRegistry:
    def create_indexing_coordinator(self) -> IndexingCoordinator:
        """Create indexing service"""
        return IndexingCoordinator(
            database=self.get_provider("database"),
            embedding_service=self.create_embedding_service(),
            parsers=self._language_parsers,
            config=self._config
        )

    def create_search_service(self) -> SearchService:
        """Create search service"""
        return SearchService(
            database=self.get_provider("database"),
            embedding_service=self.create_embedding_service(),
            config=self._config
        )

    def create_embedding_service(self) -> EmbeddingService | None:
        """Create embedding service"""
        if self._config.embeddings_disabled:
            return None

        return EmbeddingService(
            database=self.get_provider("database"),
            provider=self.get_provider("embedding"),
            config=self._config.embedding
        )
```

### Global Registry Access

```python
# Singleton pattern
_registry: ProviderRegistry | None = None

def get_registry() -> ProviderRegistry:
    """Get global registry instance"""
    global _registry
    if _registry is None:
        raise RuntimeError("Registry not initialized")
    return _registry

def get_provider(name: str) -> Any:
    """Get provider by name"""
    return get_registry().get_provider(name)
```

### Graceful Degradation

The registry handles missing dependencies:

```python
Degradation Strategy:
├── embeddings_disabled = True:
│   ├── Skip embedding generation
│   ├── Disable semantic search
│   └── Enable regex search only
├── LLM config missing:
│   ├── Disable code research
│   └── Enable basic search
└── Database unavailable:
    └── Raise fatal error (required dependency)
```

---

## MCP Integration Architecture

### Server Modes

ChunkHound implements the Model Context Protocol in two modes:

#### 1. STDIO Mode (Default)

```python
Command: uv run chunkhound mcp stdio

Characteristics:
├── JSON-RPC over stdin/stdout
├── Stateful connection (global state preserved)
├── NO_STDOUT_LOGS constraint (breaks protocol)
└── Single-threaded database access
```

#### 2. HTTP Mode

```python
Command: uv run chunkhound mcp http --port 5173

Characteristics:
├── HTTP JSON-RPC endpoint
├── RESTful API compatibility
├── Multi-client support
└── Standard logging allowed
```

### MCP Tools Exposed

**Location:** `chunkhound/mcp_server/stdio.py`

```python
MCP Tools:
├── search_semantic:
│   ├── Input: {query: str, limit: int, threshold: float, path: str}
│   └── Output: list[ChunkResult]
├── search_regex:
│   ├── Input: {pattern: str, limit: int, path: str}
│   └── Output: list[ChunkResult]
└── code_research:
    ├── Input: {query: str, max_depth: int}
    └── Output: ResearchReport (markdown)
```

### Tool Response Format

```python
Tool Response Structure:
├── Type: list[types.TextContent]
├── Content:
│   ├── type: "text"
│   ├── text: JSON-formatted or markdown string
│   └── annotations: Optional metadata
└── Error Handling: Exception → error response
```

### Database Concurrency Constraints

```python
Critical Constraint: SINGLE_THREADED_ONLY
├── DuckDB: Not thread-safe, concurrent access → corruption
├── LanceDB: Not thread-safe, concurrent access → segfault
├── Solution: SerialDatabaseProvider wrapper
└── Pattern: Single-threaded queue for all DB operations
```

### SerialDatabaseProvider

```python
class SerialDatabaseProvider:
    """Wrapper enforcing single-threaded DB access"""

    def __init__(self, provider: DatabaseProvider):
        self._provider = provider
        self._lock = threading.Lock()

    def execute(self, operation: Callable[[], T]) -> T:
        """Execute operation with exclusive lock"""
        with self._lock:
            return operation()
```

### Global State Management

```python
MCP Server State:
├── Database Connection: Persistent across tool calls
├── Embedding Service: Reused for all semantic searches
├── Parser Registry: Cached language parsers
└── Configuration: Immutable after initialization

Rationale: STDIO_CONSTRAINT
├── stdio mode maintains connection throughout session
├── Stateless design would require reconnection overhead
└── Global state optimizes repeated operations
```

### Performance Optimizations

```python
Optimization Strategies:
├── Embedding Batching: MANDATORY (100x performance difference)
│   └── Batch size: 100-300 embeddings per request
├── Vector Index: DROP_BEFORE_BULK_INSERT (20x speedup)
│   ├── Drop HNSW index before bulk embedding insert
│   └── Rebuild index after insert completes
└── Parser Caching: Lazy instantiation + reuse
    └── Parsers cached per language in global registry
```

---

## Appendix: Key File Locations

### Core Data Models
- `chunkhound/core/models/chunk.py` - Chunk data model
- `chunkhound/core/models/embedding.py` - Embedding model
- `chunkhound/core/models/file.py` - File model
- `chunkhound/core/types/common.py` - Type system

### Algorithms
- `chunkhound/parsers/universal_parser.py` - cAST algorithm
- `chunkhound/services/search/multi_hop_strategy.py` - Multi-hop search
- `chunkhound/services/embedding_service.py` - Embedding batching

### Database
- `chunkhound/providers/database/duckdb_provider.py` - DuckDB schema
- `chunkhound/providers/database/lancedb_provider.py` - LanceDB schema
- `chunkhound/providers/database/duckdb/chunk_repository.py` - Chunk storage
- `chunkhound/providers/database/duckdb/embedding_repository.py` - Embedding storage

### Configuration
- `chunkhound/core/config/config.py` - Main configuration
- `chunkhound/core/config/embedding_config.py` - Embedding config
- `chunkhound/core/config/indexing_config.py` - Indexing config
- `chunkhound/core/config/llm_config.py` - LLM config

### Parsers
- `chunkhound/parsers/mappings/python.py` - Python ontology
- `chunkhound/parsers/mappings/typescript.py` - TypeScript ontology
- `chunkhound/parsers/mappings/java.py` - Java ontology
- `chunkhound/parsers/mappings/markdown.py` - Markdown ontology

### Services
- `chunkhound/services/search_service.py` - Search API
- `chunkhound/services/indexing_coordinator.py` - Indexing orchestration
- `chunkhound/services/research/models.py` - Research models

### Registry & MCP
- `chunkhound/registry/__init__.py` - Component registry
- `chunkhound/mcp_server/stdio.py` - MCP server implementation

---

## References

1. **Academic Paper:** Zhang et al. (2025). "cAST: Enhancing Code Retrieval-Augmented Generation with Structural Chunking via Abstract Syntax Tree". arXiv:2506.15655. https://arxiv.org/abs/2506.15655

2. **Reference Implementation:** astchunk - https://github.com/yilinjz/astchunk

3. **Official Documentation:** https://chunkhound.github.io/

4. **GitHub Repository:** https://github.com/chunkhound/chunkhound

5. **Dependencies:**
   - Tree-sitter: https://tree-sitter.github.io/
   - DuckDB: https://duckdb.org/
   - LanceDB: https://lancedb.com/
   - Pydantic: https://docs.pydantic.dev/

---

**Document Version:** 1.0
**Last Updated:** 2025-11-20
**Maintained By:** Research Team
