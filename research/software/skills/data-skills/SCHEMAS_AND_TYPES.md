# Schemas, Data Contracts & Type Safety

## Quick Navigation

This document covers the type safety architecture for the data-unified platform using BAML as source of truth.

**Related Documents:**
- [ARCHITECTURE.md](./ARCHITECTURE.md) - Core architecture reference
- [AI_MEMORY.md](./AI_MEMORY.md) - Agno, Cognee, CocoIndex integrations
- [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Step-by-step patterns

---

## Table of Contents

1. [Overview: Unified Domain Schema](#overview-unified-domain-schema)
2. [BAML: Single Source of Truth](#baml-single-source-of-truth)
3. [Pydantic Models (Python)](#pydantic-models-python)
4. [TypeScript Types & Zod Schemas](#typescript-types--zod-schemas)
5. [Database Schema Integration](#database-schema-integration)
6. [Cross-Layer Integration](#cross-layer-integration)
7. [Schema Versioning & Evolution](#schema-versioning--evolution)
8. [Data Flow Patterns](#data-flow-patterns)

---

## Overview: Unified Domain Schema

To maintain **type consistency across the entire stack**, we define core domain classes once using **BAML (Boundary AI Markup Language)** and generate corresponding models in Python (Pydantic) and TypeScript (Zod).

### Target Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   BAML Definitions                          │
│                  (Single Source of Truth)                   │
│                    baml_src/domain.baml                     │
└────────────────┬──────────────────────────┬─────────────────┘
                 │                          │
         ┌───────┴───────┐          ┌───────┴───────┐
         ▼               ▼          ▼               ▼
┌─────────────────┐ ┌───────────────────┐ ┌─────────────────┐
│ Pydantic Models │ │ TypeScript Types  │ │ Zod Schemas     │
│ (Python)        │ │ (interfaces)      │ │ (runtime)       │
└────────┬────────┘ └────────┬──────────┘ └────────┬────────┘
         │                   │                     │
         ▼                   ▼                     ▼
   ┌───────────┐      ┌───────────┐         ┌───────────┐
   │ DLT       │      │ Hono API  │         │ TanStack  │
   │ CocoIndex │      │ Drizzle   │         │ Convex    │
   │ Cognee    │      │ Convex    │         │ Frontend  │
   └───────────┘      └───────────┘         └───────────┘
```

### Components Using Unified Schema

| Component | Language | Role | Schema Integration |
|-----------|----------|------|-------------------|
| DLT | Python | ETL ingestion | Pydantic for validation |
| CocoIndex | Python | Code/doc chunking & embedding | Pydantic for chunk models |
| Cognee | Python | Memory layer (graph + vector) | Pydantic for graph nodes |
| Drizzle ORM | TypeScript | Database queries | TS types + Zod |
| Hono API | TypeScript | Backend service | Zod validation |
| TanStack Start | TypeScript | Frontend | TS types + Zod |
| Convex | TypeScript | Real-time backend | Convex validators + Zod |

---

## BAML: Single Source of Truth

### Defining Domain Classes

```baml
// baml_src/domain.baml

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

class UserQuery {
  user_id string            @description("ID of the user making the query")
  query_text string         @description("The user's question or prompt")
  timestamp datetime        @description("When the query was made")
}
```

### BAML Type Reference

| BAML Type | Python | TypeScript | Description |
|-----------|--------|------------|-------------|
| `string` | `str` | `string` | Text value |
| `int` | `int` | `number` | Integer |
| `float` | `float` | `number` | Decimal |
| `bool` | `bool` | `boolean` | Boolean |
| `datetime` | `datetime` | `string` (ISO) | Timestamp |
| `string[]` | `List[str]` | `string[]` | Array |
| `string?` | `Optional[str]` | `string \| undefined` | Optional |
| `map<K,V>` | `Dict[K,V]` | `Record<K,V>` | Key-value |
| `any` | `Any` | `any` | Flexible type |

### Generating Code

```bash
# Generate Python and TypeScript clients
baml-cli generate

# Output:
# - baml_client/types.py (Pydantic models)
# - baml_client/types.ts (TypeScript interfaces)
```

---

## Pydantic Models (Python)

### Auto-Generated Models

```python
# baml_client/types.py (auto-generated)
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

class UserQuery(BaseModel):
    user_id: str
    query_text: str
    timestamp: str  # ISO 8601
```

### DLT Integration

```python
import dlt
from baml_client.types import DocumentChunk

@dlt.resource(name="document_chunks")
def ingest_repo_chunks(repo_path: str):
    """DLT resource using Pydantic model for schema enforcement"""
    for chunk_text in split_file_to_chunks(repo_path):
        chunk = DocumentChunk(
            id=compute_chunk_id(repo_path),
            repo=repo_path,
            file_path=current_file_path,
            content=chunk_text,
            embedding=embed(chunk_text)
        )
        yield chunk.dict()  # DLT consumes dict

# DLT automatically creates table schema from Pydantic fields
pipeline = dlt.pipeline(
    pipeline_name="code_indexing",
    destination="duckdb",
    dataset_name="codebase"
)

pipeline.run(ingest_repo_chunks("/path/to/repo"))
```

### CocoIndex Integration

```python
import cocoindex
from baml_client.types import DocumentChunk

@cocoindex.flow_def(name="code_index")
def build_code_index(flow_builder, data_scope):
    # ... chunking and embedding logic ...

    with file["chunks"].row() as chunk:
        # Create validated DocumentChunk
        doc_chunk = DocumentChunk(
            id=generate_id(),
            repo=repo_name,
            file_path=file["filename"],
            content=chunk["text"],
            embedding=chunk["embedding"]
        )

        data_scope.add_collector("chunks").collect(**doc_chunk.dict())
```

### Cognee Integration

```python
import cognee
from baml_client.types import DocumentChunk, RepoSummary

# Add document chunks to knowledge graph
for chunk in document_chunks:
    validated_chunk = DocumentChunk.parse_obj(chunk)
    cognee.add_data_points([validated_chunk.json()])

# Cognee stores nodes with properties matching Pydantic fields
cognee.cognify()

# Search returns data that can be parsed back to model
results = cognee.search("query")
typed_results = [DocumentChunk.parse_obj(r) for r in results]
```

---

## TypeScript Types & Zod Schemas

### Auto-Generated Types

```typescript
// baml_client/types.ts (auto-generated)
export interface DocumentChunk {
  id: string;
  repo: string;
  file_path?: string;
  content: string;
  embedding: number[];  // float[] → number[]
}

export interface ToolCall {
  tool_name: string;
  params: Record<string, any>;
  result?: any;
}

export interface RepoSummary {
  repo_name: string;
  description: string;
  main_topics: string[];
  file_count: number;
}

export interface UserQuery {
  user_id: string;
  query_text: string;
  timestamp: string;  // ISO 8601
}
```

### Zod Schemas for Runtime Validation

```typescript
// schemas.ts
import { z } from 'zod';

export const DocumentChunkSchema = z.object({
  id: z.string(),
  repo: z.string(),
  file_path: z.string().optional(),
  content: z.string(),
  embedding: z.array(z.number())
});
export type DocumentChunk = z.infer<typeof DocumentChunkSchema>;

export const ToolCallSchema = z.object({
  tool_name: z.string(),
  params: z.record(z.any()),
  result: z.any().optional()
});
export type ToolCall = z.infer<typeof ToolCallSchema>;

export const RepoSummarySchema = z.object({
  repo_name: z.string(),
  description: z.string(),
  main_topics: z.array(z.string()),
  file_count: z.number().int()
});
export type RepoSummary = z.infer<typeof RepoSummarySchema>;

export const UserQuerySchema = z.object({
  user_id: z.string(),
  query_text: z.string(),
  timestamp: z.string().datetime()
});
export type UserQuery = z.infer<typeof UserQuerySchema>;
```

### Automated Generation: Pydantic → Zod

```bash
# Install pydantic2zod
pip install pydantic2zod

# Generate Zod from Pydantic
pydantic2zod baml_client/types.py > src/schemas.ts
```

Or use JSON Schema as intermediate:

```python
from baml_client.types import DocumentChunk

# Export JSON Schema
json_schema = DocumentChunk.schema_json()

# Use zod-to-json-schema or json-schema-to-zod tools
```

---

## Database Schema Integration

### Drizzle ORM (TypeScript)

```typescript
// db/schema.ts
import { pgTable, text, integer, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema, createSelectSchema } from "drizzle-zod";

export const documentChunks = pgTable("document_chunks", {
  id: text("id").primaryKey(),
  repo: text("repo").notNull(),
  file_path: text("file_path"),
  content: text("content").notNull(),
  embedding: jsonb("embedding").$type<number[]>()
});

// Auto-generate Zod schemas from Drizzle
export const DocumentChunkInsertSchema = createInsertSchema(documentChunks);
export const DocumentChunkSelectSchema = createSelectSchema(documentChunks);

// Type inference
export type DocumentChunkDB = typeof documentChunks.$inferSelect;
export type DocumentChunkInsert = typeof documentChunks.$inferInsert;
```

### Convex Schema

```typescript
// convex/schema.ts
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  documentChunks: defineTable({
    userId: v.string(),
    repo: v.string(),
    filePath: v.optional(v.string()),
    content: v.string(),
    embedding: v.array(v.number())
  })
    .index("by_user", ["userId"])
    .index("by_repo", ["repo"])
    .searchIndex("search_content", { searchField: "content" }),

  chatSessions: defineTable({
    userId: v.string(),
    title: v.string(),
    isActive: v.boolean()
  })
    .index("by_user", ["userId"])
    .index("by_active", ["isActive"]),

  messages: defineTable({
    sessionId: v.id("chatSessions"),
    userId: v.optional(v.string()),
    content: v.string(),
    role: v.union(v.literal("user"), v.literal("assistant"), v.literal("system")),
    metadata: v.optional(v.object({ sources: v.optional(v.array(v.string())) }))
  })
    .index("by_session", ["sessionId"])
});
```

### Dual Database Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Application                            │
├──────────────────────────────┬──────────────────────────────┤
│                              │                              │
│  Authentication Database     │   Operational Database       │
│  (Cloudflare D1 + SQLite)    │   (Convex Real-time)        │
│                              │                              │
│  ├─ user                     │   ├─ chatSessions           │
│  ├─ session                  │   ├─ messages               │
│  ├─ account                  │   ├─ documentChunks         │
│  └─ verification             │   └─ documents              │
│                              │                              │
│  (Drizzle ORM)               │   (Real-time subscriptions) │
└──────────────────────────────┴──────────────────────────────┘
```

---

## Cross-Layer Integration

### Hono API (TypeScript Backend)

```typescript
import { Hono } from 'hono';
import { UserQuerySchema, DocumentChunkSchema } from './schemas';
import { searchMemory } from './services/search';

const app = new Hono();

app.post('/ask', async (c) => {
  // Validate request body with Zod
  const body = await c.req.json();
  const userQuery = UserQuerySchema.parse(body);

  // userQuery is now typed as UserQuery
  const results = await searchMemory(userQuery);

  // Validate response shape
  const safeResults = DocumentChunkSchema.array().parse(results);

  return c.json(safeResults);
});
```

### TanStack Start (Frontend)

```typescript
import { DocumentChunkSchema } from '../schemas';

const searchRoute = router.createRoute({
  path: '/search',
  loader: async ({ search: { q } }) => {
    const res = await fetch('/ask', {
      method: 'POST',
      body: JSON.stringify({
        user_id: currentUserId,
        query_text: q,
        timestamp: new Date().toISOString()
      })
    });

    const data = await res.json();

    // Client-side validation
    const chunks = DocumentChunkSchema.array().parse(data);

    return { chunks };
  },
  element: <SearchPage />
});
```

### Convex Mutations with Zod

```typescript
// convex/logToolCall.ts
import { mutation } from "./_generated/server";
import { v } from "convex/values";
import { ToolCallSchema } from "../schemas";

export default mutation({
  args: { toolCall: v.any() },
  handler: async (ctx, args) => {
    // Validate with Zod schema
    const toolCall = ToolCallSchema.parse(args.toolCall);

    // Insert validated data
    await ctx.db.insert("tool_calls", toolCall);
  }
});
```

---

## Schema Versioning & Evolution

### Versioning Strategy

```typescript
// Add schema_version to critical types
export const DocumentChunkSchema = z.object({
  schema_version: z.literal(1).default(1),
  id: z.string(),
  repo: z.string(),
  // ... other fields
});
```

### Evolution Workflow

1. **Update BAML**: Modify `domain.baml` with new field
2. **Regenerate**: Run `baml-cli generate`
3. **Test Alignment**: Compare Pydantic ↔ Zod schemas
4. **Migrate DB**: Run Drizzle/Convex migrations
5. **Bump Version**: Increment `schema_version`

### Backward Compatibility

```typescript
// Handle multiple schema versions
const parseChunk = (data: unknown) => {
  // Try current version
  const v2Result = DocumentChunkSchemaV2.safeParse(data);
  if (v2Result.success) return v2Result.data;

  // Fallback to older version with migration
  const v1Result = DocumentChunkSchemaV1.safeParse(data);
  if (v1Result.success) {
    return migrateV1ToV2(v1Result.data);
  }

  throw new Error('Unknown schema version');
};
```

### DLT Schema Evolution

```python
# DLT automatically handles schema evolution
pipeline = dlt.pipeline(
    pipeline_name="evolving_data",
    destination="duckdb"
)

# New field appears? DLT adds column
# Type changes? DLT adjusts if compatible
pipeline.run(data_with_new_fields)
```

---

## Data Flow Patterns

### Chat Creation Flow

```
User Input (Frontend Form)
    │
    ▼
┌────────────────────────────┐
│ Zod Schema Validation      │
│ UserQuerySchema.parse()    │
└────────────────────────────┘
    │
    ├─ Invalid? → Return validation error
    │
    ▼
┌────────────────────────────┐
│ Type-safe API handler      │
│ (Hono with validated data) │
└────────────────────────────┘
    │
    ▼
┌────────────────────────────┐
│ Search Memory (Cognee)     │
│ Returns DocumentChunk[]    │
└────────────────────────────┘
    │
    ▼
┌────────────────────────────┐
│ Response Validation        │
│ DocumentChunkSchema.parse()│
└────────────────────────────┘
    │
    ▼
┌────────────────────────────┐
│ Frontend receives typed    │
│ data with autocompletion   │
└────────────────────────────┘
```

### Cascade Delete Pattern

```
user.id = "user-123" DELETED
    │
    ├─ session (userId: "user-123") ──► CASCADE DELETE
    │
    ├─ account (userId: "user-123") ──► CASCADE DELETE
    │
    ├─ chat (userId: "user-123") ──────► CASCADE DELETE
    │       │
    │       └─ message (chatId) ────────► CASCADE DELETE
    │
    └─ document (userId: "user-123") ──► CASCADE DELETE
            │
            └─ documentChunk (documentId) ► CASCADE DELETE
```

### Index Strategy

**Drizzle (Authentication):**
- `user.email` - UNIQUE
- `session.userId` - INDEX
- `session.token` - UNIQUE
- `chat.userId` - INDEX
- `message.chatId` - INDEX

**Convex (Operational):**
- `chatSessions.by_user` - INDEX on userId
- `messages.by_session` - INDEX on sessionId
- `documents.search_content` - SEARCH INDEX
- `documentChunks.by_document` - INDEX on documentId

---

## Best Practices

### 1. Never Edit Generated Files

```
baml_src/domain.baml  ← Edit this
baml_client/types.py  ← Don't edit (regenerated)
baml_client/types.ts  ← Don't edit (regenerated)
```

### 2. Validate at Boundaries

- **API entry**: Validate incoming requests
- **API exit**: Validate outgoing responses
- **DB read**: Validate after database queries
- **External services**: Validate third-party data

### 3. Use Type Inference

```typescript
// Infer from Zod instead of duplicating
export type DocumentChunk = z.infer<typeof DocumentChunkSchema>;

// Infer from Drizzle
export type DocumentChunkDB = typeof documentChunks.$inferSelect;
```

### 4. Test Schema Alignment

```typescript
// Ensure Pydantic and Zod match
test('schemas are aligned', () => {
  const pythonJson = await getPydanticJsonSchema('DocumentChunk');
  const zodJson = zodToJsonSchema(DocumentChunkSchema);
  expect(pythonJson).toEqual(zodJson);
});
```

### 5. Log Validation Failures

```typescript
const result = DocumentChunkSchema.safeParse(data);
if (!result.success) {
  logger.error('Schema validation failed', {
    errors: result.error.issues,
    data: JSON.stringify(data).slice(0, 1000)
  });
  throw new ValidationError(result.error);
}
```

---

## References

**Sources:**
- schema-data-contracts.md (BAML, Pydantic, Zod integration)
- data-model-relationships.md (Database schemas, ER diagrams)

**External:**
- [BAML Documentation](https://docs.boundaryml.com)
- [Pydantic Documentation](https://docs.pydantic.dev)
- [Zod Documentation](https://zod.dev)
- [Drizzle ORM](https://orm.drizzle.team)
- [Convex Documentation](https://docs.convex.dev)

**Last Updated:** November 29, 2024
