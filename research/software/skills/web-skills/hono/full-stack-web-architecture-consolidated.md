# Modern Full-Stack Web Application Architecture
## A Comprehensive Guide to Building Type-Safe, Real-Time Applications

---

## Table of Contents

1. [Executive Overview](#executive-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Frontend Architecture](#frontend-architecture)
4. [Backend & State Management](#backend--state-management)
5. [API Integration Patterns](#api-integration-patterns)
6. [Type Safety & Schema Validation](#type-safety--schema-validation)
7. [Real-Time Features](#real-time-features)
8. [Deployment](#deployment)
9. [Observability](#observability)
10. [Best Practices & Patterns](#best-practices--patterns)

---

## Executive Overview

### Full-Stack Architecture Philosophy

This document presents a comprehensive architecture for building modern, type-safe, real-time web applications. The approach centers on a strategic separation of concerns that prioritizes scalability, low latency, and maintainability while leveraging the best of contemporary web technologies.

#### Core Architectural Pillars

**1. TanStack Start** – Full-stack frontend framework providing:
- File-based, type-safe routing
- Server-Side Rendering (SSR)
- Server-to-client streaming
- Unified server and client function execution

**2. Convex** – Real-time state management and persistence:
- Single source of truth for application data
- WebSocket-based real-time synchronization across all clients
- Chat history, live metrics, and user presence management

**3. BetterAuth** – Self-hosted authentication provider:
- OpenID Connect (OIDC) support
- JWT-based authentication
- Multi-tenancy through organization-based access control

**4. Cloudflare/Netlify** – Edge and serverless deployment:
- Global CDN distribution
- Edge function execution
- Zero-config deployment workflows

**5. Type Safety** – End-to-end type consistency:
- Unified schema definitions (BAML → Pydantic + Zod)
- Compile-time validation throughout the stack
- Runtime validation at data boundaries

---

## Authentication & Authorization

### BetterAuth Setup and Configuration

BetterAuth serves as the central Identity Provider (IdP) using OpenID Connect. All components (frontend and backend) rely on BetterAuth for user authentication, with Supabase PostgreSQL as the primary database backing.

#### Architecture Overview

```
User → TanStack Frontend → BetterAuth (via Hono) → PostgreSQL
                         ↓
                    JWT/Session
                         ↓
           Convex Backend ← validates JWT
           Hono API ← validates JWT
```

#### Core Components

**BetterAuth (OIDC Provider)**
- Handles user authentication (email/password, social logins)
- Issues OIDC tokens (ID/Access tokens) via standard flows (Authorization Code with PKCE)
- Persists user profiles, credentials, sessions, and OIDC client data in PostgreSQL

**Supabase PostgreSQL**
- Stores BetterAuth's data tables (users, sessions, accounts)
- Can also store application data
- Single database instance for simplicity (can be separated in production)

**Hono (Backend API)**
- Lightweight TypeScript web server
- Integrates BetterAuth library as middleware/route handlers
- Hosts BetterAuth authentication endpoints (`/api/auth/*` routes)
- Provides protected application APIs

#### Docker Compose Configuration

```yaml
services:
  db:
    image: postgres:15-alpine
    container_name: supabase-db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: postgres
    volumes:
      - supabase-data:/var/lib/postgresql/data
    networks:
      - internal

  api:
    build: ./hono-api
    container_name: hono-api
    depends_on:
      - db
      - convex
    environment:
      DATABASE_URL: postgres://postgres:${DB_PASSWORD}@db:5432/postgres
      BETTER_AUTH_SECRET: ${AUTH_SECRET}
      BETTER_AUTH_URL: http://localhost:4000
    networks:
      - internal
    ports:
      - "4000:4000"

  convex:
    image: ghcr.io/get-convex/convex-backend:latest
    container_name: convex-backend
    depends_on:
      - db
    environment:
      DATABASE_URL: postgresql://postgres:${DB_PASSWORD}@db
      CONVEX_CLOUD_ORIGIN: http://convex:3210
      CONVEX_SITE_ORIGIN: http://convex:3211
    networks:
      - internal
    ports:
      - "3210:3210"
      - "3211:3211"

  frontend:
    build: ./tanstack-start
    container_name: tanstack-frontend
    depends_on:
      - api
    environment:
      PUBLIC_AUTH_URL: "http://localhost:4000/api/auth"
      PUBLIC_API_URL: "http://localhost:4000/api"
      PUBLIC_CONVEX_URL: "http://localhost:3210"
    networks:
      - internal
    ports:
      - "3000:3000"

networks:
  internal:
    driver: bridge

volumes:
  supabase-data:
```

#### Environment Variables & Secrets

**Critical Configuration:**

1. **Database Password** – Strong password for PostgreSQL
2. **BetterAuth Secret** – Long random string for encryption/hashing
   ```bash
   openssl rand -hex 32
   ```
3. **BetterAuth URL** – Base URL of the application (localhost in dev, production domain in prod)
4. **OIDC Client Config** – Client ID and redirect URIs
5. **Trusted Origins (CORS)** – Allowed origins for requests
   ```typescript
   trustedOrigins: ['http://localhost:3000']
   ```

### Convex OIDC Integration

Convex is configured to trust BetterAuth as an OIDC provider through `auth.config.ts`:

```typescript
// convex/auth.config.ts
export default {
  providers: [
    {
      domain: "http://localhost:4000", // BetterAuth issuer URL
      applicationID: "tanstack_frontend" // OIDC Client ID
    }
  ]
};
```

This configuration ensures:
- Convex accepts tokens where `iss` matches the BetterAuth URL
- Convex verifies tokens using BetterAuth's JWKS endpoint
- User identity is available in Convex functions via `auth.getUser()`

### JWT Validation Flow

**1. User Login (Frontend → BetterAuth)**
- User initiates login through TanStack Start app
- Frontend redirects to BetterAuth's OIDC authorization endpoint
- User provides credentials on BetterAuth's interface

**2. Authentication & Token Issuance**
- BetterAuth verifies credentials against PostgreSQL
- Creates user session persisted in database
- Issues ID token (JWT) and access token
- Tokens signed by BetterAuth with claims:
  - `sub`: User ID
  - `iss`: BetterAuth URL
  - `aud`: Application client ID
  - `exp`: Expiration timestamp

**3. Frontend Stores Token**
- Tokens stored in memory or HttpOnly cookie
- Access token included as `Authorization: Bearer <token>` header
- Convex client configured with ID token via `setAuth(token)`

**4. Backend Verification (Hono)**
```typescript
import { Hono } from 'hono';
import { auth } from './auth';

const app = new Hono();

app.use('/api/*', async (c, next) => {
  // BetterAuth middleware validates JWT
  const session = await auth.getSession(c);
  if (!session) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  c.set('user', session.user);
  await next();
});

app.get('/api/profile', async (c) => {
  const user = c.get('user');
  // User is authenticated and available
  return c.json({ user });
});
```

**5. Backend Verification (Convex)**
- Convex validates JWT signature using BetterAuth's JWKS
- Checks `iss`, `aud`, and `exp` claims
- Makes user identity available in server functions:

```typescript
// convex/users.ts
import { query } from "./_generated/server";

export const getCurrentUser = query({
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Not authenticated");
    }
    return identity;
  }
});
```

### Multi-Tenancy Patterns

**Organization-Based Access Control:**

1. **JWT Claims**
   - Include `orgId` in JWT issued by BetterAuth
   - Custom claims added during token creation

2. **Query Filtering**
   - All queries scoped to user's organization
   - Apply filters at database level

```typescript
// Hono API
app.get('/api/contracts', async (c) => {
  const user = c.get('user');
  const contracts = await db.query(
    'SELECT * FROM contracts WHERE org_id = $1',
    [user.orgId]
  );
  return c.json(contracts);
});

// Convex Query
export const listContracts = query({
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    return await ctx.db
      .query("contracts")
      .filter(q => q.eq(q.field("orgId"), identity.orgId))
      .collect();
  }
});
```

3. **GraphQL Auth Rules**
   - Neo4j GraphQL library supports `@authorization` directives
   - Inject tenant context at schema level

```graphql
type Contract @authorization(filter: [{ where: { orgId: "$jwt.orgId" } }]) {
  id: ID!
  title: String!
  orgId: String!
  parties: [Party!]!
}
```

---

## Frontend Architecture

### TanStack Start Routing

TanStack Start provides a full-stack foundation with file-based routing, SSR, and server-to-client streaming.

#### File-Based Routing Structure

```
src/routes/
├── dashboard/
│   ├── route.tsx          # Main layout with <Outlet />
│   ├── index.tsx          # Dashboard home (/dashboard)
│   ├── contracts/
│   │   ├── index.tsx      # Contract list (/dashboard/contracts)
│   │   └── $id.tsx        # Contract detail (/dashboard/contracts/:id)
│   └── chat-fullscreen.tsx # Full-page chat (/dashboard/chat-fullscreen)
└── index.tsx              # Root route (/)
```

#### Layout Components

**Main Dashboard Layout (`src/routes/dashboard/route.tsx`):**

```typescript
import { createFileRoute, Outlet } from '@tanstack/react-router';
import { DashboardHeader } from '~/components/DashboardHeader';
import { getInitialDashboardStats } from '~/server/stats';

export const Route = createFileRoute('/dashboard')({
  loader: async () => {
    // Pre-fetch data during SSR
    const stats = await getInitialDashboardStats();
    return { stats };
  },
  component: DashboardLayout
});

function DashboardLayout() {
  const { stats } = Route.useLoaderData();

  return (
    <div className="dashboard-container">
      <DashboardHeader />
      <div className="dashboard-content">
        <div className="chat-panel">
          {/* Chat interface */}
        </div>
        <div className="stats-panel">
          {/* Live metrics */}
        </div>
        <div className="main-content">
          <Outlet /> {/* Renders child routes */}
        </div>
      </div>
    </div>
  );
}
```

#### SSR and Route Loaders

Route loaders enable pre-fetching data before page render, eliminating loading spinners and content layout shifts.

**Server Function for Data Fetching:**

```typescript
// src/server/stats.ts
import { createServerFn } from '@tanstack/start';
import { useConvexAuth } from 'convex/react';

export const getInitialDashboardStats = createServerFn({ method: 'GET' })
  .handler(async ({ context }) => {
    // Secure server-side data fetching
    const convex = context.convex;
    const stats = await convex.query(api.stats.getDashboardData);
    return stats;
  });
```

**Route with Loader:**

```typescript
export const Route = createFileRoute('/dashboard')({
  loader: () => getInitialDashboardStats(),
  component: DashboardLayout
});
```

During initial page load:
1. TanStack's SSR engine executes the loader
2. Calls the server function and waits for data
3. Streams HTML to client with data already populated
4. Client-side React hydrates the application

### SSR and Streaming Capabilities

TanStack Start supports streaming responses for optimal performance:

**Streaming Server Function:**

```typescript
// src/server/ai.ts
export const askRepoAI = createServerFn({ method: 'POST' })
  .handler(async function* ({ data }) {
    // Async generator for streaming
    const { prompt } = data;

    // Call CodeRabbit API
    const response = await fetch('https://api.coderabbit.ai/chat', {
      method: 'POST',
      body: JSON.stringify({ prompt }),
      headers: { 'Content-Type': 'application/json' }
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      yield chunk; // Stream to client
    }
  });
```

**Client-Side Consumption:**

```typescript
import { useState } from 'react';
import { askRepoAI } from '~/server/ai';

function ChatComponent() {
  const [message, setMessage] = useState('');
  const [response, setResponse] = useState('');

  const handleSubmit = async () => {
    const stream = await askRepoAI({ data: { prompt: message } });
    const reader = stream.body.getReader();
    const decoder = new TextDecoder();

    let fullResponse = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      fullResponse += chunk;
      setResponse(fullResponse); // Update UI incrementally
    }

    // Persist to Convex after completion
    await convex.mutation(api.messages.save, {
      prompt: message,
      response: fullResponse
    });
  };

  return (
    <div>
      <input value={message} onChange={e => setMessage(e.target.value)} />
      <button onClick={handleSubmit}>Send</button>
      <div>{response}</div>
    </div>
  );
}
```

---

## Backend & State Management

### Convex as Unified Real-Time Backend

Convex provides a reactive backend with automatic WebSocket synchronization, making it the single source of truth for all persistent, shared state.

#### Core Concepts

**1. Queries** – Read data reactively:
```typescript
// convex/messages.ts
import { query } from "./_generated/server";

export const getAll = query({
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    return await ctx.db
      .query("messages")
      .filter(q => q.eq(q.field("userId"), identity.subject))
      .order("desc")
      .collect();
  }
});
```

**2. Mutations** – Write data:
```typescript
// convex/messages.ts
import { mutation } from "./_generated/server";
import { v } from "convex/values";

export const save = mutation({
  args: {
    prompt: v.string(),
    response: v.string()
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    await ctx.db.insert("messages", {
      userId: identity.subject,
      prompt: args.prompt,
      response: args.response,
      timestamp: Date.now()
    });
  }
});
```

**3. Actions** – External API calls:
```typescript
// convex/external.ts
import { action } from "./_generated/server";

export const callExternalAPI = action({
  handler: async (ctx, args) => {
    // Actions can make external HTTP requests
    const response = await fetch('https://api.example.com/data');
    const data = await response.json();

    // Can call mutations to persist results
    await ctx.runMutation(api.data.store, { data });
    return data;
  }
});
```

#### Real-Time Sync via WebSockets

**Client Setup:**

```typescript
// src/lib/convex.tsx
import { ConvexProvider, ConvexReactClient } from "convex/react";

const convex = new ConvexReactClient(import.meta.env.VITE_CONVEX_URL);

export function ConvexClientProvider({ children }) {
  return (
    <ConvexProvider client={convex}>
      {children}
    </ConvexProvider>
  );
}
```

**Using Queries in Components:**

```typescript
import { useQuery, useMutation } from "convex/react";
import { api } from "../convex/_generated/api";

function ChatHistory() {
  // Automatically subscribes to updates via WebSocket
  const messages = useQuery(api.messages.getAll);
  const saveMessage = useMutation(api.messages.save);

  // When ANY client calls saveMessage, this component re-renders
  // across ALL connected clients automatically

  return (
    <div>
      {messages?.map(msg => (
        <div key={msg._id}>
          <p><strong>Prompt:</strong> {msg.prompt}</p>
          <p><strong>Response:</strong> {msg.response}</p>
        </div>
      ))}
    </div>
  );
}
```

#### Hybrid Streaming Model

The architecture employs two complementary real-time mechanisms:

**1. Ephemeral Token Streaming (TanStack Start)**
- Used exclusively for AI agent's in-progress response
- Server function returns ReadableStream via async generator
- Provides lowest-latency "typing" effect
- Unidirectional, ephemeral, server-to-client push
- Only active user sees the stream

**2. Persistent State Sync (Convex)**
- Used for all committed application state
- Bi-directional WebSocket-based synchronization
- Accessed via `useQuery` hook
- All clients see updates instantly
- Provides durable, shared source of truth

**Implementation Pattern:**

```typescript
async function handleChatSubmit(prompt: string) {
  // 1. Optimistic UI update
  setMessages(prev => [...prev, { prompt, response: '', pending: true }]);

  // 2. Stream response from AI
  const stream = await askRepoAI({ data: { prompt } });
  let fullResponse = '';

  for await (const chunk of stream) {
    fullResponse += chunk;
    setCurrentResponse(fullResponse); // Update local state only
  }

  // 3. Commit to Convex (all clients will receive via WebSocket)
  await convex.mutation(api.messages.save, {
    prompt,
    response: fullResponse
  });

  // 4. Clear local state (Convex query will now provide the data)
  setCurrentResponse('');
}
```

This "stream-then-commit" model provides:
- Immediate feedback for the active user (streaming)
- Persistent, synchronized state across all clients (Convex)
- Best UX without sacrificing consistency

---

## API Integration Patterns

### OpenAPI/TypeScript Client Generation

For external APIs or microservices, generate type-safe TypeScript clients from OpenAPI specifications.

#### Choosing a Code Generator

**swagger-typescript-api** (Recommended for Node/Bun)
- No Java dependency (pure Node.js)
- Supports Fetch API and Axios
- Generates single file or modular structure
- Active maintenance and good TypeScript support

**Example Generation:**

```bash
# Install
npm install --save-dev swagger-typescript-api

# Generate client
npx swagger-typescript-api generate \
  --path ./openapi/api-spec.json \
  --output src/apiClient/ \
  --name ApiClient.ts \
  --api-class-name ApiClient \
  --axios
```

**Generated Usage:**

```typescript
// src/lib/apiClient.ts
import { ApiClient } from './apiClient/ApiClient';

const client = new ApiClient({
  baseURL: "https://api.example.com/v1",
  headers: {
    Authorization: `Bearer ${process.env.API_KEY}`
  }
});

// Fully typed methods
const contracts = await client.contracts.listContracts({
  orgId: "123"
});
```

#### Alternative: openapi-typescript-codegen

```bash
npx openapi-typescript-codegen \
  --input api-spec.json \
  --output src/apiClient \
  --client fetch
```

Supports multiple HTTP client options (fetch, node-fetch, Axios, XHR).

### GraphQL vs tRPC vs REST Decision Matrix

| Criterion | GraphQL | tRPC | REST |
|-----------|---------|------|------|
| **Type Safety** | Schema-based, needs codegen | End-to-end TypeScript inference | Manual or codegen required |
| **Learning Curve** | Moderate (new query language) | Low (TypeScript functions) | Low (standard HTTP) |
| **Over-fetching** | Solved (client specifies fields) | N/A (function calls) | Common issue |
| **Real-time** | Subscriptions available | Not built-in | SSE/WebSockets needed |
| **Tooling** | Excellent (Apollo, Relay) | Growing (tRPC client) | Standard HTTP tools |
| **Best For** | Complex data graphs, mobile apps | Full-stack TypeScript monorepos | Public APIs, simple services |

**Recommendation for Modern Stack:**

- **Use tRPC** for internal frontend ↔ backend communication in TypeScript monorepos
- **Use GraphQL** if exposing API to external clients or need complex nested queries
- **Use REST** for simple services or when interoperating with non-TypeScript systems

### Server Functions as API Gateway

TanStack Start server functions act as a secure gateway between frontend and backend services.

**Pattern: Server Function → Multiple Backends:**

```typescript
// src/server/data.ts
import { createServerFn } from '@tanstack/start';
import { ApiClient } from '~/lib/apiClient';
import { convex } from '~/lib/convex';

export const getDashboardData = createServerFn({ method: 'GET' })
  .handler(async ({ context }) => {
    const user = context.user; // From auth middleware

    // Parallel data fetching from multiple sources
    const [contracts, metrics, aiSummary] = await Promise.all([
      // External API via generated client
      ApiClient.contracts.list({ orgId: user.orgId }),

      // Convex query
      convex.query(api.metrics.getLatest, { userId: user.id }),

      // AI service
      fetch('https://ai-service.com/summarize', {
        method: 'POST',
        body: JSON.stringify({ orgId: user.orgId })
      }).then(r => r.json())
    ]);

    return { contracts, metrics, aiSummary };
  });
```

**Benefits:**
- Centralized authentication/authorization
- API key security (never exposed to client)
- Cross-origin request handling
- Response transformation and caching
- Error handling and retry logic

---

## Type Safety & Schema Validation

### BAML for Structured LLM Outputs

BAML (Boundary AI Markup Language) provides a single source of truth for domain models, generating corresponding Pydantic (Python) and TypeScript/Zod (JavaScript) types.

#### Defining Domain Classes in BAML

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

#### Generated Pydantic Models

```python
# Auto-generated by BAML (baml_client/types.py)
from pydantic import BaseModel, Field
from typing import List, Optional, Any, Dict
from datetime import datetime

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
    timestamp: datetime
```

#### Generated TypeScript Types

```typescript
// Auto-generated by BAML (baml_client/types.ts)
export interface DocumentChunk {
  id: string;
  repo: string;
  file_path?: string;
  content: string;
  embedding: number[];
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
  timestamp: string; // ISO 8601 string
}
```

### Zod for Runtime Validation

Create Zod schemas for runtime validation at data boundaries:

```typescript
// src/lib/schemas.ts
import { z } from 'zod';

export const DocumentChunkSchema = z.object({
  id: z.string(),
  repo: z.string(),
  file_path: z.string().optional(),
  content: z.string(),
  embedding: z.array(z.number())
});

export const ToolCallSchema = z.object({
  tool_name: z.string(),
  params: z.record(z.any()),
  result: z.any().optional()
});

export const RepoSummarySchema = z.object({
  repo_name: z.string(),
  description: z.string(),
  main_topics: z.array(z.string()),
  file_count: z.number().int()
});

export const UserQuerySchema = z.object({
  user_id: z.string(),
  query_text: z.string(),
  timestamp: z.string().datetime()
});

// Infer types from schemas
export type DocumentChunk = z.infer<typeof DocumentChunkSchema>;
export type ToolCall = z.infer<typeof ToolCallSchema>;
export type RepoSummary = z.infer<typeof RepoSummarySchema>;
export type UserQuery = z.infer<typeof UserQuerySchema>;
```

### End-to-End Type Safety

**Server-Side Validation (Hono):**

```typescript
import { Hono } from 'hono';
import { zValidator } from '@hono/zod-validator';
import { UserQuerySchema, DocumentChunkSchema } from '~/lib/schemas';

const app = new Hono();

app.post(
  '/ask',
  zValidator('json', UserQuerySchema),
  async (c) => {
    const query = c.req.valid('json'); // Typed as UserQuery

    // Process query...
    const results = await searchMemory(query);

    // Validate response
    const validated = DocumentChunkSchema.array().parse(results);
    return c.json(validated);
  }
);
```

**Client-Side Validation (TanStack Router):**

```typescript
import { createFileRoute } from '@tanstack/react-router';
import { DocumentChunkSchema } from '~/lib/schemas';

export const Route = createFileRoute('/search')({
  loader: async ({ search: { q } }) => {
    const response = await fetch('/api/search', {
      method: 'POST',
      body: JSON.stringify({ query: q })
    });

    const data = await response.json();

    // Validate response shape
    const chunks = DocumentChunkSchema.array().parse(data);
    return { chunks };
  },
  component: SearchPage
});

function SearchPage() {
  const { chunks } = Route.useLoaderData();
  // chunks is typed as DocumentChunk[]

  return (
    <div>
      {chunks.map(chunk => (
        <div key={chunk.id}>
          <h3>{chunk.repo}</h3>
          <p>{chunk.content}</p>
        </div>
      ))}
    </div>
  );
}
```

### Maintaining Schema Consistency

**1. Single Source of Truth**
- BAML class definitions are canonical
- All downstream models regenerated from BAML
- Include BAML file in version control
- Generation steps in build/CI pipeline

**2. Automated Generation Workflow**

```json
// package.json
{
  "scripts": {
    "generate": "npm run generate:baml && npm run generate:schemas",
    "generate:baml": "baml-cli generate",
    "generate:schemas": "node scripts/baml-to-zod.js",
    "prebuild": "npm run generate"
  }
}
```

**3. Testing Schema Alignment**

```typescript
// test/schema-alignment.test.ts
import { describe, it, expect } from 'vitest';
import { DocumentChunkSchema } from '~/lib/schemas';
import { DocumentChunk as PydanticDocumentChunk } from '../python/baml_client/types';

describe('Schema Alignment', () => {
  it('should validate Python→TypeScript serialization', () => {
    const pythonData = {
      id: "123",
      repo: "test-repo",
      content: "Test content",
      embedding: [0.1, 0.2, 0.3]
    };

    // Should parse without error
    const parsed = DocumentChunkSchema.parse(pythonData);
    expect(parsed).toEqual(pythonData);
  });
});
```

**4. Versioning Schemas**

Add schema version field for breaking changes:

```baml
class DocumentChunk {
  schema_version int @description("Schema version") @default(1)
  id string
  repo string
  // ... other fields
}
```

Handle migrations:

```typescript
function migrateDocumentChunk(data: any): DocumentChunk {
  if (data.schema_version === 1) {
    return DocumentChunkSchema.parse(data);
  }

  // Handle older versions
  if (!data.schema_version) {
    // Migrate v0 to v1
    return DocumentChunkSchema.parse({
      schema_version: 1,
      ...data,
      // Add new required fields with defaults
    });
  }

  throw new Error(`Unknown schema version: ${data.schema_version}`);
}
```

---

## Real-Time Features

### Chat Interface with Streaming

Implement a responsive chat interface with server-sent streaming and persistent storage.

**Component Implementation:**

```typescript
// src/components/Chat.tsx
import { useState } from 'react';
import { useQuery, useMutation } from 'convex/react';
import { api } from '~/convex/_generated/api';
import { askRepoAI } from '~/server/ai';

function Chat() {
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);

  // Subscribe to persistent chat history
  const messages = useQuery(api.messages.getAll);
  const saveMessage = useMutation(api.messages.save);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isStreaming) return;

    const prompt = input;
    setInput('');
    setIsStreaming(true);
    setStreaming('');

    try {
      // Stream response
      const stream = await askRepoAI({ data: { prompt } });
      const reader = stream.body.getReader();
      const decoder = new TextDecoder();

      let fullResponse = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        fullResponse += chunk;
        setStreaming(fullResponse); // Update UI with each token
      }

      // Commit to Convex (triggers sync to all clients)
      await saveMessage({
        prompt,
        response: fullResponse
      });

    } catch (error) {
      console.error('Chat error:', error);
    } finally {
      setIsStreaming(false);
      setStreaming('');
    }
  };

  return (
    <div className="chat-container">
      <div className="messages">
        {messages?.map(msg => (
          <div key={msg._id} className="message">
            <div className="prompt">
              <strong>You:</strong> {msg.prompt}
            </div>
            <div className="response">
              <strong>AI:</strong> {msg.response}
            </div>
          </div>
        ))}

        {/* Show streaming response */}
        {isStreaming && (
          <div className="message streaming">
            <div className="prompt">
              <strong>You:</strong> {input}
            </div>
            <div className="response">
              <strong>AI:</strong> {streaming}
              <span className="cursor">▊</span>
            </div>
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit}>
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Ask a question..."
          disabled={isStreaming}
        />
        <button type="submit" disabled={isStreaming}>
          {isStreaming ? 'Thinking...' : 'Send'}
        </button>
      </form>
    </div>
  );
}
```

### Live Dashboards with Selective Updates

Display real-time metrics that update only when data changes (no unnecessary polling).

**Data Pipeline:**

```
GitHub Event → DLT/RisingWave → Convex Mutation → WebSocket → UI Update
```

**Convex Schema:**

```typescript
// convex/schema.ts
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  github_stats: defineTable({
    orgId: v.string(),
    openIssues: v.number(),
    closedIssues: v.number(),
    openPRs: v.number(),
    lastRelease: v.optional(v.string()),
    updatedAt: v.number()
  }).index("by_org", ["orgId"]),

  sentiment_stats: defineTable({
    orgId: v.string(),
    positiveCount: v.number(),
    negativeCount: v.number(),
    neutralCount: v.number(),
    averageScore: v.number(),
    updatedAt: v.number()
  }).index("by_org", ["orgId"])
});
```

**Convex Mutations (called by pipeline):**

```typescript
// convex/metrics.ts
import { mutation } from "./_generated/server";
import { v } from "convex/values";

export const updateGithubStats = mutation({
  args: {
    orgId: v.string(),
    openIssues: v.number(),
    closedIssues: v.number(),
    openPRs: v.number(),
    lastRelease: v.optional(v.string())
  },
  handler: async (ctx, args) => {
    const existing = await ctx.db
      .query("github_stats")
      .withIndex("by_org", q => q.eq("orgId", args.orgId))
      .first();

    if (existing) {
      await ctx.db.patch(existing._id, {
        ...args,
        updatedAt: Date.now()
      });
    } else {
      await ctx.db.insert("github_stats", {
        ...args,
        updatedAt: Date.now()
      });
    }
  }
});

export const updateSentimentStats = mutation({
  args: {
    orgId: v.string(),
    positiveCount: v.number(),
    negativeCount: v.number(),
    neutralCount: v.number(),
    averageScore: v.number()
  },
  handler: async (ctx, args) => {
    const existing = await ctx.db
      .query("sentiment_stats")
      .withIndex("by_org", q => q.eq("orgId", args.orgId))
      .first();

    if (existing) {
      await ctx.db.patch(existing._id, {
        ...args,
        updatedAt: Date.now()
      });
    } else {
      await ctx.db.insert("sentiment_stats", {
        ...args,
        updatedAt: Date.now()
      });
    }
  }
});
```

**Convex Queries:**

```typescript
// convex/metrics.ts
import { query } from "./_generated/server";
import { v } from "convex/values";

export const getGithubStats = query({
  args: { orgId: v.string() },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("github_stats")
      .withIndex("by_org", q => q.eq("orgId", args.orgId))
      .first();
  }
});

export const getSentimentStats = query({
  args: { orgId: v.string() },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("sentiment_stats")
      .withIndex("by_org", q => q.eq("orgId", args.orgId))
      .first();
  }
});
```

**React Component:**

```typescript
// src/components/LiveDashboard.tsx
import { useQuery } from 'convex/react';
import { api } from '~/convex/_generated/api';

function LiveDashboard({ orgId }: { orgId: string }) {
  // Automatically updates when data changes
  const githubStats = useQuery(api.metrics.getGithubStats, { orgId });
  const sentimentStats = useQuery(api.metrics.getSentimentStats, { orgId });

  if (!githubStats || !sentimentStats) {
    return <div>Loading...</div>;
  }

  return (
    <div className="dashboard">
      <div className="card">
        <h2>GitHub Activity</h2>
        <div className="stats">
          <div>
            <span className="label">Open Issues</span>
            <span className="value">{githubStats.openIssues}</span>
          </div>
          <div>
            <span className="label">Open PRs</span>
            <span className="value">{githubStats.openPRs}</span>
          </div>
          <div>
            <span className="label">Latest Release</span>
            <span className="value">{githubStats.lastRelease || 'None'}</span>
          </div>
        </div>
      </div>

      <div className="card">
        <h2>Social Sentiment</h2>
        <div className="sentiment-score">
          <div className="score" style={{
            color: sentimentStats.averageScore > 0.5 ? 'green' : 'red'
          }}>
            {(sentimentStats.averageScore * 100).toFixed(1)}%
          </div>
          <div className="breakdown">
            <span>👍 {sentimentStats.positiveCount}</span>
            <span>😐 {sentimentStats.neutralCount}</span>
            <span>👎 {sentimentStats.negativeCount}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
```

**Key Benefits:**
- **No polling** – Updates pushed via WebSocket when data changes
- **Efficient** – Only changed data transmitted
- **Consistent** – All clients see same state
- **Simple** – No manual subscription management

### Collaboration and Presence

Implement real-time collaboration features using Convex's presence component.

**Installation:**

```bash
npm install @convex-dev/presence
```

**Setup:**

```typescript
// convex/presence.ts
import { Presence } from "@convex-dev/presence/Presence";

const presence = new Presence();
export default presence;
```

**Component Usage:**

```typescript
// src/components/CollaboratorPresence.tsx
import { usePresence } from '@convex-dev/react';
import presence from '~/convex/presence';

function CollaboratorPresence({ roomId }: { roomId: string }) {
  const { myPresence, othersPresence } = usePresence(presence, roomId);

  return (
    <div className="presence">
      <div className="presence-indicator">
        {othersPresence.length > 0 && (
          <div className="avatars">
            {othersPresence.map(user => (
              <img
                key={user.id}
                src={user.avatar}
                alt={user.name}
                title={user.name}
                className="avatar"
              />
            ))}
          </div>
        )}
        <span className="count">
          {othersPresence.length === 0
            ? 'Only you'
            : `${othersPresence.length + 1} viewing`}
        </span>
      </div>
    </div>
  );
}
```

---

## Deployment

### Cloudflare Workers/Pages Configuration

Deploy TanStack Start applications to Cloudflare for global edge distribution.

**Vite Configuration:**

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { tanstackStart } from '@tanstack/start/vite';

export default defineConfig({
  plugins: [
    react(),
    tanstackStart({
      adapter: 'cloudflare-pages'
    })
  ],
  build: {
    target: 'esnext'
  }
});
```

**wrangler.toml:**

```toml
name = "my-tanstack-app"
compatibility_date = "2024-01-01"
pages_build_output_dir = "dist"

[env.production]
vars = { NODE_ENV = "production" }

[[env.production.kv_namespaces]]
binding = "KV"
id = "your-kv-namespace-id"
```

**Deployment:**

```bash
# Build
npm run build

# Deploy to Cloudflare Pages
npx wrangler pages deploy dist
```

**Environment Variables (Cloudflare Dashboard):**

```
BETTER_AUTH_SECRET=xxx
CONVEX_URL=https://xxx.convex.cloud
DATABASE_URL=postgresql://...
```

### Netlify as Alternative

**netlify.toml:**

```toml
[build]
  command = "npm run build"
  publish = "dist"
  functions = "netlify/functions"

[[redirects]]
  from = "/api/*"
  to = "/.netlify/functions/:splat"
  status = 200

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

[functions]
  node_bundler = "esbuild"
  directory = "netlify/functions"
```

**Vite Configuration:**

```typescript
// vite.config.ts
export default defineConfig({
  plugins: [
    react(),
    tanstackStart({
      adapter: 'netlify'
    })
  ]
});
```

**Deployment:**

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
netlify deploy --prod
```

### Environment Variables and Secrets

**Best Practices:**

1. **Never commit secrets to Git**
   ```gitignore
   # .gitignore
   .env
   .env.local
   .env.*.local
   ```

2. **Use platform-specific secret management**
   - Cloudflare: Wrangler secrets or dashboard
   - Netlify: Environment variables dashboard
   - Docker: Environment files or Docker secrets

3. **Separate by environment**
   ```
   .env.development    # Local development
   .env.staging        # Staging environment
   .env.production     # Production (never committed)
   ```

4. **Type-safe environment variables**

```typescript
// src/lib/env.ts
import { z } from 'zod';

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'staging', 'production']),
  BETTER_AUTH_SECRET: z.string().min(32),
  DATABASE_URL: z.string().url(),
  CONVEX_URL: z.string().url(),
  SENTRY_DSN: z.string().optional(),
  OPENAI_API_KEY: z.string().optional()
});

export const env = envSchema.parse(process.env);
```

### CI/CD Patterns

**GitHub Actions Example:**

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Type check
        run: npm run typecheck

      - name: Lint
        run: npm run lint

      - name: Test
        run: npm test

  deploy-staging:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Build
        run: npm run build
        env:
          BETTER_AUTH_SECRET: ${{ secrets.BETTER_AUTH_SECRET_STAGING }}
          DATABASE_URL: ${{ secrets.DATABASE_URL_STAGING }}
          CONVEX_URL: ${{ secrets.CONVEX_URL_STAGING }}

      - name: Deploy to Netlify Staging
        run: npx netlify deploy --dir=dist
        env:
          NETLIFY_AUTH_TOKEN: ${{ secrets.NETLIFY_AUTH_TOKEN }}
          NETLIFY_SITE_ID: ${{ secrets.NETLIFY_SITE_ID_STAGING }}

  deploy-production:
    needs: test
    if: github.event_name == 'workflow_dispatch'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Build
        run: npm run build
        env:
          NODE_ENV: production
          BETTER_AUTH_SECRET: ${{ secrets.BETTER_AUTH_SECRET_PROD }}
          DATABASE_URL: ${{ secrets.DATABASE_URL_PROD }}
          CONVEX_URL: ${{ secrets.CONVEX_URL_PROD }}

      - name: Deploy to Cloudflare Pages
        run: npx wrangler pages deploy dist
        env:
          CLOUDFLARE_API_TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          CLOUDFLARE_ACCOUNT_ID: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
```

---

## Observability

### Sentry Integration

Full-stack error monitoring with TanStack Start.

**Installation:**

```bash
npm install @sentry/tanstackstart-react
```

**Client-Side Setup:**

```typescript
// src/entry-client.tsx
import * as Sentry from '@sentry/tanstackstart-react';
import { router } from './router';

Sentry.init({
  dsn: import.meta.env.VITE_SENTRY_DSN,
  environment: import.meta.env.MODE,
  integrations: [
    Sentry.tanstackRouterBrowserTracingIntegration(router),
    Sentry.replayIntegration()
  ],
  tracesSampleRate: 1.0,
  replaysSessionSampleRate: 0.1,
  replaysOnErrorSampleRate: 1.0
});
```

**Server-Side Setup:**

```typescript
// src/entry-server.tsx
import * as Sentry from '@sentry/node';

Sentry.init({
  dsn: process.env.SENTRY_DSN,
  environment: process.env.NODE_ENV,
  tracesSampleRate: 1.0
});
```

**Error Boundary:**

```typescript
// src/components/ErrorBoundary.tsx
import * as Sentry from '@sentry/tanstackstart-react';

export function ErrorBoundary({ children }: { children: React.ReactNode }) {
  return (
    <Sentry.ErrorBoundary
      fallback={({ error, resetError }) => (
        <div className="error-page">
          <h1>Something went wrong</h1>
          <p>{error.message}</p>
          <button onClick={resetError}>Try again</button>
        </div>
      )}
      showDialog
    >
      {children}
    </Sentry.ErrorBoundary>
  );
}
```

**Custom Error Tracking:**

```typescript
import * as Sentry from '@sentry/tanstackstart-react';

try {
  await riskyOperation();
} catch (error) {
  Sentry.captureException(error, {
    tags: { operation: 'riskyOperation' },
    extra: { userId: user.id }
  });
  throw error;
}
```

### Usage Tracking with Autumn

Track API usage and enforce limits/billing.

**Installation:**

```bash
npm install autumn-js
```

**Frontend Setup:**

```typescript
// src/lib/autumn.tsx
import { AutumnProvider } from 'autumn-js/react';

export function AutumnClientProvider({ children }: { children: React.ReactNode }) {
  return (
    <AutumnProvider apiKey={import.meta.env.VITE_AUTUMN_PUBLIC_KEY}>
      {children}
    </AutumnProvider>
  );
}
```

**Backend Tracking:**

```typescript
// src/server/ai.ts
import { Autumn } from 'autumn-js';

const autumn = new Autumn({
  secretKey: process.env.AUTUMN_SECRET_KEY
});

export const askRepoAI = createServerFn({ method: 'POST' })
  .handler(async function* ({ data, context }) {
    const { prompt } = data;
    const user = context.user;

    // Check if user has quota
    const canProceed = await autumn.check({
      customerId: user.id,
      featureId: 'ai_query'
    });

    if (!canProceed) {
      throw new Error('Usage limit reached');
    }

    // Stream response...
    for await (const chunk of aiStream) {
      yield chunk;
    }

    // Track usage after success
    await autumn.track({
      customerId: user.id,
      featureId: 'ai_query',
      value: 1
    });
  });
```

**Frontend Usage Display:**

```typescript
// src/components/UsageIndicator.tsx
import { useCustomer } from 'autumn-js/react';

function UsageIndicator() {
  const customer = useCustomer();

  if (!customer) return null;

  const usage = customer.usage['ai_query'];
  const limit = customer.plan.features['ai_query'].limit;

  return (
    <div className="usage-indicator">
      <span>AI Queries: {usage} / {limit}</span>
      <progress value={usage} max={limit} />
    </div>
  );
}
```

---

## Best Practices & Patterns

### When to Use Streaming vs Selective Updates

| Use Case | Approach | Reasoning |
|----------|----------|-----------|
| **AI Text Generation** | Server Streaming | Continuous output, immediate feedback needed |
| **Chat Messages** | Selective Updates (Convex) | Discrete events, need persistence |
| **Dashboard Metrics** | Selective Updates (Convex) | Infrequent changes, need consistency |
| **Live Activity Feed** | Selective Updates (Convex) | Event-driven, multiple consumers |
| **File Upload Progress** | Server Streaming | Continuous percentage updates |
| **Real-Time Collaboration** | Selective Updates (Convex) | Shared state across clients |

**Decision Matrix:**

```
Is content generated incrementally? → YES → Use Streaming
                                    ↓ NO
Is data shared across multiple users? → YES → Use Convex
                                         ↓ NO
Is data ephemeral (not persisted)? → YES → Use Streaming
                                     ↓ NO
                              Use Convex
```

### Server vs Client Data Fetching

**Fetch on Server When:**
- Initial page load (SSR)
- Data requires authentication
- Sensitive API keys involved
- SEO important
- Reduce client bundle size

```typescript
// ✅ Server-side
export const Route = createFileRoute('/dashboard')({
  loader: () => getInitialDashboardStats(), // Server function
  component: Dashboard
});
```

**Fetch on Client When:**
- User interaction triggers fetch
- Data updates frequently
- Optimistic updates needed
- Progressive enhancement

```typescript
// ✅ Client-side
function Dashboard() {
  const stats = useQuery(api.stats.get); // Convex query
  return <div>{stats?.value}</div>;
}
```

**Hybrid Approach (Best Practice):**

```typescript
// Server: Initial load
export const Route = createFileRoute('/dashboard')({
  loader: () => getInitialStats(),
  component: Dashboard
});

// Client: Real-time updates
function Dashboard() {
  const initialStats = Route.useLoaderData();
  const liveStats = useQuery(api.stats.get); // Overrides initial

  const stats = liveStats ?? initialStats; // Fallback pattern
  return <div>{stats.value}</div>;
}
```

### Security Considerations

**1. Authentication at Every Layer**

```typescript
// Server Function
export const secureOperation = createServerFn()
  .middleware(async ({ context, next }) => {
    const user = await getAuthenticatedUser(context);
    if (!user) throw new Error('Unauthorized');
    return next({ context: { ...context, user } });
  })
  .handler(async ({ context }) => {
    // context.user is guaranteed to exist
    return performOperation(context.user);
  });
```

**2. Input Validation**

```typescript
// Always validate untrusted input
import { z } from 'zod';

const inputSchema = z.object({
  name: z.string().min(1).max(100),
  email: z.string().email(),
  age: z.number().int().min(0).max(150)
});

export const createUser = createServerFn({ method: 'POST' })
  .handler(async ({ data }) => {
    const validated = inputSchema.parse(data); // Throws on invalid
    return db.users.create(validated);
  });
```

**3. CORS Configuration**

```typescript
// Hono
import { cors } from 'hono/cors';

app.use('/api/*', cors({
  origin: ['http://localhost:3000', 'https://app.example.com'],
  credentials: true
}));
```

**4. Rate Limiting**

```typescript
// Using rate limiter
import { RateLimiter } from 'limiter';

const limiter = new RateLimiter({
  tokensPerInterval: 100,
  interval: 'minute'
});

export const apiEndpoint = createServerFn()
  .middleware(async ({ context, next }) => {
    const remaining = await limiter.removeTokens(1);
    if (remaining < 0) {
      throw new Error('Rate limit exceeded');
    }
    return next();
  })
  .handler(async () => {
    // Protected by rate limiter
  });
```

**5. SQL Injection Prevention**

```typescript
// ✅ Parameterized queries
const users = await db.query(
  'SELECT * FROM users WHERE email = $1',
  [email]
);

// ❌ String concatenation (vulnerable)
const users = await db.query(
  `SELECT * FROM users WHERE email = '${email}'`
);
```

**6. XSS Prevention**

```typescript
// React automatically escapes content
<div>{userInput}</div> // Safe

// Dangerous: dangerouslySetInnerHTML
<div dangerouslySetInnerHTML={{ __html: userInput }} /> // ❌

// Use DOMPurify for HTML content
import DOMPurify from 'dompurify';
<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userInput) }} /> // ✅
```

### Performance Optimization

**1. Code Splitting**

```typescript
// Route-level splitting (automatic with TanStack Router)
const DashboardRoute = createFileRoute('/dashboard')({
  component: () => import('./Dashboard').then(m => m.Dashboard)
});

// Component-level splitting
const HeavyComponent = lazy(() => import('./HeavyComponent'));
```

**2. Memoization**

```typescript
import { useMemo } from 'react';

function ExpensiveComponent({ data }: { data: Item[] }) {
  const processed = useMemo(() => {
    return data.map(item => expensiveTransform(item));
  }, [data]); // Only recompute when data changes

  return <div>{processed}</div>;
}
```

**3. Virtualization**

```typescript
import { useVirtualizer } from '@tanstack/react-virtual';

function VirtualList({ items }: { items: Item[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 50
  });

  return (
    <div ref={parentRef} style={{ height: '600px', overflow: 'auto' }}>
      <div style={{ height: `${virtualizer.getTotalSize()}px` }}>
        {virtualizer.getVirtualItems().map(virtualRow => (
          <div
            key={virtualRow.index}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              transform: `translateY(${virtualRow.start}px)`
            }}
          >
            {items[virtualRow.index].name}
          </div>
        ))}
      </div>
    </div>
  );
}
```

**4. Database Indexing**

```typescript
// Convex schema with indexes
export default defineSchema({
  users: defineTable({
    email: v.string(),
    orgId: v.string(),
    createdAt: v.number()
  })
    .index("by_email", ["email"])
    .index("by_org", ["orgId"])
    .index("by_org_created", ["orgId", "createdAt"])
});
```

### Error Handling Patterns

**1. Global Error Boundary**

```typescript
// src/App.tsx
import { ErrorBoundary } from './components/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary>
      <Router />
    </ErrorBoundary>
  );
}
```

**2. Try-Catch with User Feedback**

```typescript
async function handleSubmit() {
  setLoading(true);
  setError(null);

  try {
    await submitForm(data);
    toast.success('Submitted successfully');
  } catch (error) {
    if (error instanceof ValidationError) {
      setError(error.message);
    } else {
      toast.error('An unexpected error occurred');
      Sentry.captureException(error);
    }
  } finally {
    setLoading(false);
  }
}
```

**3. Retry Logic**

```typescript
async function fetchWithRetry(url: string, retries = 3): Promise<Response> {
  for (let i = 0; i < retries; i++) {
    try {
      const response = await fetch(url);
      if (response.ok) return response;

      if (response.status >= 500) {
        // Server error, retry
        await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, i)));
        continue;
      }

      // Client error, don't retry
      throw new Error(`HTTP ${response.status}`);
    } catch (error) {
      if (i === retries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, i)));
    }
  }

  throw new Error('Max retries exceeded');
}
```

---

## Conclusion

This architecture provides a comprehensive foundation for building modern, type-safe, real-time web applications. Key takeaways:

**✅ Unified Type System** – BAML → Pydantic + Zod ensures consistency across the stack

**✅ Real-Time by Default** – Convex provides automatic synchronization without polling

**✅ Secure Authentication** – BetterAuth with OIDC supports multi-tenancy and self-hosting

**✅ Flexible Deployment** – Deploy to Cloudflare, Netlify, or any edge platform

**✅ Developer Experience** – TypeScript end-to-end with excellent tooling

**✅ Production Ready** – Built-in observability, error tracking, and usage monitoring

This stack balances cutting-edge technology with proven patterns, enabling teams to ship features quickly while maintaining high code quality and system reliability.

---

## Additional Resources

### Documentation
- [TanStack Start](https://tanstack.com/start)
- [Convex](https://docs.convex.dev/)
- [BetterAuth](https://www.better-auth.com/docs)
- [BAML](https://docs.boundaryml.com/)
- [Zod](https://zod.dev/)

### Templates
- [Better-T-Stack](https://github.com/AmanVarshney01/Better-T-Stack)
- [TanStack Start Examples](https://github.com/tanstack/start/tree/main/examples)
- [Convex Templates](https://www.convex.dev/templates)

### Community
- [TanStack Discord](https://discord.gg/tanstack)
- [Convex Discord](https://discord.gg/convex)
- [BetterAuth Discord](https://discord.gg/better-auth)
