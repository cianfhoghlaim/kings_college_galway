# DLT → BAML → oRPC/OpenAPI: Type-Safe Pipeline Analysis

## Executive Summary

This research analyzes how dlthub's auto-generated schemas can be leveraged with BAML to create a fully type-safe pipeline spanning data ingestion to API/MCP endpoints. The key insight is that **BAML serves as the schema bridge** that generates both Pydantic (Python) and Zod (TypeScript) schemas from a single source of truth, enabling seamless integration across the entire stack.

---

## 1. DLT Schema Generation & Pydantic Integration

### 1.1 How DLT Auto-Generates Schemas

DLT automatically infers schemas during the normalization process:

```python
import dlt

@dlt.resource
def users():
    yield {"id": 1, "name": "Alice", "email": "alice@example.com"}
    yield {"id": 2, "name": "Bob", "email": "bob@example.com"}

# DLT automatically creates schema:
# - id: bigint
# - name: text
# - email: text
```

**Key capabilities:**
- Automatic type inference from Python data structures
- Schema evolution (adding columns, changing types)
- Export to YAML format for inspection/modification
- Incremental loading with state tracking

### 1.2 Current Pydantic Integration (Input Direction)

DLT supports using Pydantic models as schema definitions:

```python
from pydantic import BaseModel
import dlt

class User(BaseModel):
    id: int
    name: str
    email: str
    is_active: bool = True

@dlt.resource(name="users", columns=User)
def load_users():
    yield {"id": 1, "name": "Alice", "email": "alice@example.com"}
```

**Available functions in `dlt.common.libs.pydantic`:**
- `pydantic_to_table_schema_columns()` - Convert Pydantic model to table schema
- `apply_schema_contract_to_model()` - Configure model for schema evolution modes
- `create_list_model()` - Generate batch validation models
- `validate_and_filter_items()` - Validate data against models

### 1.3 The Export Gap (Schema → Pydantic)

**Current limitation**: DLT does not yet export inferred schemas to Pydantic models.

**2025 Roadmap**: DLT is "experimenting with different ways to represent schemas, ie. instead of yaml we want to offer the option to store dlt schemas as Pydantic models or data classes."

**Workaround strategies:**
1. **BAML as schema source** (recommended) - Define schemas in BAML, generate Pydantic
2. **Manual schema definition** - Define Pydantic models and use with DLT
3. **YAML → Code generation** - Parse exported YAML and generate Pydantic

---

## 2. BAML: The Schema Bridge

### 2.1 What BAML Does

BAML (Boundary AI Markup Language) provides:
- Single source of truth for type definitions
- Code generation for multiple languages (Python, TypeScript, Ruby, Go, Java, C#, Rust)
- Schema-Aligned Parsing (SAP) for robust LLM output parsing
- 60% token efficiency compared to JSON schemas

### 2.2 Schema Definition in BAML

```baml
// baml_src/domain.baml

class DocumentChunk {
  id string                 @description("Unique chunk identifier")
  repo string               @description("Repository name or ID")
  file_path string?         @description("Source file path (if applicable)")
  content string            @description("Text content of the chunk")
  embedding float[]         @description("Embedding vector of the content")
}

class ApiResponse {
  success bool
  data DocumentChunk[]
  error string?
  timestamp datetime
}

enum ProcessingStatus {
  PENDING
  PROCESSING
  COMPLETED
  FAILED
}
```

### 2.3 Generated Pydantic Models

Running `baml-cli generate` produces:

```python
# baml_client/types.py (auto-generated)

from pydantic import BaseModel, Field
from typing import List, Optional, Any
from datetime import datetime
from enum import Enum

class ProcessingStatus(str, Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"

class DocumentChunk(BaseModel):
    id: str
    repo: str
    file_path: Optional[str] = None
    content: str
    embedding: List[float]

class ApiResponse(BaseModel):
    success: bool
    data: List[DocumentChunk]
    error: Optional[str] = None
    timestamp: datetime
```

### 2.4 Generated TypeScript Types

```typescript
// baml_client/types.ts (auto-generated)

export type ProcessingStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface DocumentChunk {
  id: string;
  repo: string;
  file_path?: string;
  content: string;
  embedding: number[];
}

export interface ApiResponse {
  success: boolean;
  data: DocumentChunk[];
  error?: string;
  timestamp: string;
}
```

### 2.5 Generated Zod Schemas

For runtime validation in TypeScript, create Zod schemas from the types:

```typescript
// schemas.ts

import { z } from 'zod';

export const ProcessingStatusSchema = z.enum([
  "PENDING", "PROCESSING", "COMPLETED", "FAILED"
]);

export const DocumentChunkSchema = z.object({
  id: z.string(),
  repo: z.string(),
  file_path: z.string().optional(),
  content: z.string(),
  embedding: z.array(z.number())
});

export const ApiResponseSchema = z.object({
  success: z.boolean(),
  data: z.array(DocumentChunkSchema),
  error: z.string().optional(),
  timestamp: z.string().datetime()
});

// Type inference
export type DocumentChunk = z.infer<typeof DocumentChunkSchema>;
export type ApiResponse = z.infer<typeof ApiResponseSchema>;
```

**Automation options:**
- `pydantic2zod` - Convert Pydantic models to Zod schemas
- Custom BAML generator plugin for Zod
- JSON Schema as intermediate format

---

## 3. oRPC: Type-Safe API Contracts

### 3.1 What oRPC Provides

oRPC is a TypeScript RPC framework with:
- **Contract-first development** - Define API once, generate clients
- **100% type safety** - Shared contracts between server and clients
- **Automatic OpenAPI generation** - First-class OpenAPI 3.1.1 support
- **Schema validation** - Works with Zod, Valibot, ArkType

### 3.2 Contract Definition with Zod

```typescript
// contracts/api.ts

import { oc } from '@orpc/contract';
import { DocumentChunkSchema, ApiResponseSchema } from './schemas';
import { z } from 'zod';

// Query contract
export const searchChunks = oc
  .input(z.object({
    query: z.string(),
    repo: z.string().optional(),
    limit: z.number().default(10)
  }))
  .output(ApiResponseSchema);

// Mutation contract
export const indexRepository = oc
  .input(z.object({
    repo_url: z.string().url(),
    branch: z.string().default('main')
  }))
  .output(z.object({
    job_id: z.string(),
    status: ProcessingStatusSchema
  }));

// Contract bundle
export const contract = oc.router({
  search: searchChunks,
  index: indexRepository
});
```

### 3.3 Server Implementation

```typescript
// server/router.ts

import { os } from '@orpc/server';
import { contract } from '../contracts/api';
import { searchMemory, startIndexJob } from './services';

export const router = os.contract(contract).router({
  search: os.search.handler(async ({ input }) => {
    const chunks = await searchMemory(input.query, input.repo, input.limit);
    return {
      success: true,
      data: chunks,
      timestamp: new Date().toISOString()
    };
  }),

  index: os.index.handler(async ({ input }) => {
    const job = await startIndexJob(input.repo_url, input.branch);
    return {
      job_id: job.id,
      status: job.status
    };
  })
});
```

### 3.4 OpenAPI Generation

```typescript
// server/openapi.ts

import { OpenAPIGenerator } from '@orpc/openapi';
import { ZodToJsonSchemaConverter } from '@orpc/zod';
import { router } from './router';

const generator = new OpenAPIGenerator({
  schemaConverters: [new ZodToJsonSchemaConverter()]
});

export const openApiSpec = await generator.generate(router, {
  info: {
    title: 'Document Chunk API',
    version: '1.0.0',
    description: 'Type-safe API for document indexing and search'
  },
  servers: [
    { url: 'https://api.example.com', description: 'Production' }
  ]
});
```

### 3.5 Type-Safe Client

```typescript
// client/api.ts

import { createORPCClient } from '@orpc/client';
import { createORPCReactQueryUtils } from '@orpc/tanstack-query';
import { contract } from '../contracts/api';

// Create typed client
const client = createORPCClient<typeof contract>({
  baseURL: 'https://api.example.com'
});

// TanStack Query integration
export const orpc = createORPCReactQueryUtils(client);

// Usage in React component
function SearchComponent() {
  const { data, isLoading } = orpc.search.useQuery({
    input: { query: 'authentication', limit: 5 }
  });

  // data is fully typed as ApiResponse
  return (
    <div>
      {data?.data.map(chunk => (
        <div key={chunk.id}>{chunk.content}</div>
      ))}
    </div>
  );
}
```

---

## 4. MCP: Type-Safe Tool Endpoints

### 4.1 MCP Protocol Overview

Model Context Protocol (MCP) provides:
- Standardized way for AI agents to access external tools
- Three primitives: Resources (GET), Tools (POST), Prompts (templates)
- JSON-RPC 2.0 protocol
- Multiple transports: stdio, HTTP, SSE, WebSocket

### 4.2 Pydantic AI + MCP Integration

```python
# mcp_server/tools.py

from mcp.server import Server
from mcp.types import Tool, TextContent
from pydantic import BaseModel
from baml_client.types import DocumentChunk, ApiResponse

# Type-safe tool input/output with Pydantic
class SearchInput(BaseModel):
    query: str
    repo: str | None = None
    limit: int = 10

server = Server("document-search")

@server.tool()
async def search_chunks(input: SearchInput) -> list[DocumentChunk]:
    """Search for document chunks matching the query."""
    # Pydantic validates input automatically
    results = await perform_search(input.query, input.repo, input.limit)
    # Return type is validated against DocumentChunk schema
    return results

@server.tool()
async def get_chunk_by_id(chunk_id: str) -> DocumentChunk | None:
    """Retrieve a specific document chunk by ID."""
    return await fetch_chunk(chunk_id)
```

### 4.3 Exposing oRPC Endpoints as MCP Tools

```python
# mcp_server/orpc_bridge.py

from mcp.server import Server
from pydantic import BaseModel
import httpx

# Mirror oRPC contracts as MCP tools
class SearchRequest(BaseModel):
    query: str
    repo: str | None = None
    limit: int = 10

class IndexRequest(BaseModel):
    repo_url: str
    branch: str = "main"

server = Server("orpc-bridge")

@server.tool()
async def api_search(request: SearchRequest):
    """Search documents via oRPC API (type-safe)."""
    async with httpx.AsyncClient() as client:
        response = await client.post(
            "https://api.example.com/search",
            json=request.model_dump()
        )
        # Response validated against ApiResponse schema
        return ApiResponse.model_validate(response.json())

@server.tool()
async def api_index(request: IndexRequest):
    """Index a repository via oRPC API (type-safe)."""
    async with httpx.AsyncClient() as client:
        response = await client.post(
            "https://api.example.com/index",
            json=request.model_dump()
        )
        return response.json()
```

### 4.4 TypeScript MCP Server with Zod

```typescript
// mcp-server/tools.ts

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { DocumentChunkSchema } from './schemas';
import { z } from 'zod';

const server = new McpServer({
  name: 'document-search',
  version: '1.0.0'
});

// Define tool with Zod schema validation
server.tool(
  'search_chunks',
  z.object({
    query: z.string(),
    repo: z.string().optional(),
    limit: z.number().default(10)
  }),
  async (input) => {
    const results = await performSearch(input.query, input.repo, input.limit);
    // Validate output
    const validated = z.array(DocumentChunkSchema).parse(results);
    return { content: [{ type: 'text', text: JSON.stringify(validated) }] };
  }
);
```

---

## 5. Complete Type-Safe Pipeline Architecture

### 5.1 Schema Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    BAML Schema Definitions                   │
│                   (Single Source of Truth)                   │
│                                                              │
│   class DocumentChunk { id: string, content: string, ... }   │
└────────────────────────┬────────────────────────────────────┘
                         │
              ┌──────────┼──────────┐
              │          │          │
              ▼          │          ▼
     ┌────────────┐      │      ┌────────────┐
     │  Pydantic  │      │      │ TypeScript │
     │   Models   │      │      │   Types    │
     └─────┬──────┘      │      └─────┬──────┘
           │             │            │
     ┌─────▼──────┐      │      ┌─────▼──────┐
     │    DLT     │      │      │    Zod     │
     │  Pipeline  │      │      │  Schemas   │
     └─────┬──────┘      │      └─────┬──────┘
           │             │            │
           │             │      ┌─────▼──────┐
           │             │      │   oRPC     │
           │             │      │ Contracts  │
           │             │      └─────┬──────┘
           │             │            │
     ┌─────▼─────────────┼────────────▼──────┐
     │           Shared Data Layer            │
     │   (Database, Vector Store, Graph)      │
     └─────┬─────────────┬────────────┬──────┘
           │             │            │
           ▼             ▼            ▼
     ┌──────────┐  ┌──────────┐  ┌──────────┐
     │   MCP    │  │ OpenAPI  │  │  Client  │
     │  Tools   │  │   Spec   │  │   SDK    │
     └──────────┘  └──────────┘  └──────────┘
```

### 5.2 Data Flow Example

**Step 1: Define Schema in BAML**
```baml
class CodeSnippet {
  id string
  repo string
  file_path string
  language string
  content string
  embedding float[]
  created_at datetime
}
```

**Step 2: Use Pydantic in DLT Pipeline**
```python
from baml_client.types import CodeSnippet
import dlt

@dlt.resource(columns=CodeSnippet)
def extract_code_snippets(repo_path: str):
    for file in scan_repository(repo_path):
        snippet = CodeSnippet(
            id=generate_id(file),
            repo=repo_path,
            file_path=file.path,
            language=detect_language(file),
            content=file.content,
            embedding=embed(file.content),
            created_at=datetime.now()
        )
        yield snippet.model_dump()
```

**Step 3: Define oRPC Contract with Zod**
```typescript
import { CodeSnippetSchema } from './schemas';

export const getSnippets = oc
  .input(z.object({ repo: z.string(), language: z.string().optional() }))
  .output(z.array(CodeSnippetSchema));
```

**Step 4: Expose as MCP Tool**
```python
@server.tool()
async def search_code(query: str, language: str = None) -> list[CodeSnippet]:
    """Search code snippets with semantic similarity."""
    return await vector_search(query, language)
```

### 5.3 Type Safety Benefits

| Layer | Validation Type | Technology |
|-------|----------------|------------|
| ETL Input | Runtime | DLT + Pydantic |
| LLM Output | Runtime + Parse | BAML SAP |
| API Request | Runtime | Zod + oRPC |
| API Response | Runtime | Zod + oRPC |
| Client Call | Compile-time | TypeScript + oRPC |
| MCP Tool | Runtime | Pydantic/Zod |

---

## 6. Implementation Strategy

### 6.1 Recommended Approach

1. **Start with BAML** - Define all domain types in `.baml` files
2. **Generate code** - Run `baml-cli generate` for both Python and TypeScript
3. **Create Zod schemas** - Use `pydantic2zod` or manual definition
4. **Define oRPC contracts** - Use Zod schemas for input/output
5. **Implement servers** - Both API (oRPC) and MCP tools
6. **Generate OpenAPI** - For external integrations and documentation

### 6.2 Build Pipeline

```yaml
# .github/workflows/schema-sync.yml

name: Schema Synchronization

on:
  push:
    paths:
      - 'baml_src/**/*.baml'

jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Generate Python/TS from BAML
        run: baml-cli generate

      - name: Generate Zod from Pydantic
        run: python scripts/pydantic_to_zod.py

      - name: Generate OpenAPI from oRPC
        run: npm run generate:openapi

      - name: Type check
        run: |
          mypy baml_client/
          npx tsc --noEmit

      - name: Commit generated code
        run: |
          git add baml_client/ src/schemas/
          git commit -m "chore: regenerate schemas from BAML"
```

### 6.3 Version Control Strategy

```baml
// Include version in schemas for evolution tracking
class CodeSnippet {
  _schema_version int = 2  // Increment on breaking changes
  id string
  // ... fields
}
```

---

## 7. Current Limitations & Workarounds

### 7.1 DLT Schema Export Gap

**Problem**: DLT doesn't export inferred schemas to Pydantic yet.

**Workarounds**:
1. Define schemas in BAML first, then use with DLT
2. Export DLT schema to YAML, convert to Pydantic manually
3. Use `datamodel-code-generator` to convert JSON Schema to Pydantic

### 7.2 BAML → Zod Generation

**Problem**: BAML doesn't directly generate Zod schemas.

**Solutions**:
1. Use `pydantic2zod` on generated Pydantic models
2. Use JSON Schema as intermediate format
3. Write custom BAML generator plugin

### 7.3 oRPC SSL Issues

**Problem**: Some oRPC documentation endpoints have SSL issues.

**Solution**: Use npm packages directly and community examples.

---

## 8. Production Considerations

### 8.1 Error Handling

```typescript
// Graceful validation errors
const result = ApiResponseSchema.safeParse(data);
if (!result.success) {
  logger.error('Schema validation failed', {
    errors: result.error.issues,
    data
  });
  throw new ValidationError(result.error);
}
```

### 8.2 Performance

- **Pydantic v2** - Rust-based validation, 5-50x faster than v1
- **Zod** - Efficient parsing with early termination
- **oRPC** - Lightweight, works on edge runtimes

### 8.3 Monitoring

```python
# Track schema validation failures
@metrics.counter("schema_validation_errors")
def validate_chunk(data: dict) -> DocumentChunk:
    try:
        return DocumentChunk.model_validate(data)
    except ValidationError as e:
        metrics.increment("schema_validation_errors", tags={"type": "DocumentChunk"})
        raise
```

---

## 9. Conclusion

The combination of **DLT + BAML + oRPC + MCP** creates a powerful type-safe pipeline:

1. **BAML** serves as the single source of truth for schemas
2. **Pydantic** provides runtime validation in Python (DLT, MCP tools)
3. **Zod** provides runtime validation in TypeScript (oRPC, MCP servers)
4. **oRPC** generates OpenAPI specs automatically from contracts
5. **MCP** exposes tools with type-safe inputs/outputs

This architecture ensures that data flowing from ingestion (DLT) through processing to APIs and AI tools maintains type consistency at every boundary, catching errors early and improving developer experience through IDE autocompletion and compile-time checks.

---

## References

- [DLT Documentation - Schema](https://dlthub.com/docs/general-usage/schema)
- [DLT Pydantic Integration](https://dlthub.com/docs/api_reference/dlt/common/libs/pydantic)
- [BAML Documentation](https://docs.boundaryml.com)
- [oRPC Documentation](https://orpc.unnoq.com/docs)
- [MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk)
- [Pydantic AI MCP Guide](https://ai.pydantic.dev/mcp/overview/)
- [pydantic2zod](https://github.com/argyle-engineering/pydantic2zod)
