# Cloudflare Workers: Comprehensive Research Report

## Table of Contents
1. [Overview](#overview)
2. [Core Features and Capabilities](#core-features-and-capabilities)
3. [Runtime API and Patterns](#runtime-api-and-patterns)
4. [Execution Model and Ontology](#execution-model-and-ontology)
5. [Integration Patterns](#integration-patterns)
6. [Platform Limits](#platform-limits)
7. [Best Practices](#best-practices)
8. [Common Use Cases and Examples](#common-use-cases-and-examples)
9. [Key Concepts and Terminology](#key-concepts-and-terminology)
10. [Development and Testing](#development-and-testing)
11. [Observability and Monitoring](#observability-and-monitoring)
12. [Security](#security)
13. [Pricing](#pricing)
14. [Migration and Compatibility](#migration-and-compatibility)

---

## Overview

Cloudflare Workers is a serverless compute platform that runs JavaScript (and WebAssembly) at the edge of Cloudflare's global network. It provides developers with the power to deploy serverless code instantly across hundreds of data centers worldwide, enabling low-latency, high-performance applications.

**Key Differentiators:**
- Runs on Cloudflare's global network of thousands of machines distributed across hundreds of locations
- Uses V8 isolates instead of containers or VMs for faster cold starts and lower memory usage
- Zero cold starts in most cases (99.99% warm request rate)
- Executes code closer to users for reduced latency
- No egress fees for data transfer

---

## Core Features and Capabilities

### 1. **Serverless Execution**
Workers execute code in response to HTTP requests, scheduled tasks (cron triggers), or queue messages without managing servers or infrastructure.

### 2. **Edge Computing**
Code runs on Cloudflare's edge network, closer to end users, reducing latency and improving performance.

### 3. **Language Support**
- **JavaScript/TypeScript**: Primary language with full ES modules support
- **WebAssembly**: Support for compiled languages (Rust, C++, Go, etc.)
- **Python**: Limited support (beta)

### 4. **Module Formats**
Two syntax styles are supported:
- **ES Modules** (recommended): Modern `export default` syntax
- **Service Workers**: Legacy `addEventListener` syntax (still supported)

### 5. **HTTP Request Handling**
Workers can intercept, modify, and respond to HTTP requests with full control over:
- Request/response headers
- Body streaming
- Status codes
- Redirects and rewrites

### 6. **WebSocket Support**
Real-time bidirectional communication with WebSocket support, often combined with Durable Objects for stateful coordination.

### 7. **Scheduled Tasks (Cron Triggers)**
Workers can be triggered on a schedule using cron expressions, ideal for:
- Periodic maintenance tasks
- Calling third-party APIs
- Data synchronization
- Background processing

### 8. **Static Assets**
Workers can serve static files (HTML, CSS, images) with:
- Automatic caching across Cloudflare's network
- Configurable routing (serve assets first or execute Worker code first)
- Assets binding for programmatic file access

### 9. **Streaming**
Workers support streaming responses using:
- ReadableStream
- WritableStream
- TransformStream

This enables processing large files without buffering entire content in memory.

---

## Runtime API and Patterns

### Fetch Handler

The primary entry point for HTTP requests. Receives three parameters:

```typescript
export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    // request: Incoming HTTP request
    // env: Bindings (KV, D1, R2, Durable Objects, secrets, etc.)
    // ctx: Context with waitUntil and passThroughOnException
    return new Response("Hello World!");
  }
}
```

**Key points:**
- `request`: Standard Request object with URL, method, headers, body
- `env`: Object containing all bindings and environment variables
- `ctx.waitUntil()`: Extends execution to wait for promises (useful for logging, analytics)
- `ctx.passThroughOnException()`: Allows request to pass through on error

### Service Bindings

Enable Worker-to-Worker communication without going through public URLs.

**Two communication methods:**

1. **RPC (Recommended)**: Call methods directly
```typescript
// Worker B exports a class
export class MyService extends WorkerEntrypoint {
  async myMethod(arg1: string) {
    return `Processed: ${arg1}`;
  }
}

// Worker A calls it
const result = await env.BINDING_NAME.myMethod("data");
```

2. **HTTP**: Forward Request objects
```typescript
const response = await env.BINDING_NAME.fetch(request);
```

**Important:** Service bindings are asynchronous - always `await` method calls.

### Scheduled Handler

For cron-triggered workers:

```typescript
export default {
  async scheduled(event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
    // event.scheduledTime: Timestamp of when this was scheduled
    // event.cron: Cron pattern that triggered this
    ctx.waitUntil(doBackgroundWork());
  }
}
```

### Tail Handler

For consuming logs from other Workers in real-time:

```typescript
export default {
  async tail(events: TraceItem[], env: Env, ctx: ExecutionContext) {
    // Process logs and send to external service
    for (const event of events) {
      // event.logs: console.log outputs
      // event.exceptions: Uncaught exceptions
      // event.scriptName: Source worker name
    }
  }
}
```

### Headers API

Workers impose fewer restrictions on headers compared to browsers:

```typescript
const headers = new Headers({
  "Content-Type": "application/json",
  "Custom-Header": "value"
});

// Iterate over headers
for (const [key, value] of headers) {
  console.log(`${key}: ${value}`);
}
```

### Fetch API

Make HTTP requests to external services:

```typescript
const response = await fetch("https://api.example.com/data", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ key: "value" })
});

const data = await response.json();
```

### Context (ctx) Methods

- **`ctx.waitUntil(promise)`**: Extends Worker execution to wait for a promise, useful for background tasks like logging
- **`ctx.passThroughOnException()`**: If an unhandled exception occurs, forward request to origin server

### Environment Variables and Secrets

**Environment Variables** (`env.VARIABLE_NAME`):
- Plain text strings or JSON values
- Defined in `wrangler.toml` under `[vars]`
- Visible in dashboard and configuration

**Secrets** (`env.SECRET_NAME`):
- Encrypted values not visible after definition
- Set via `wrangler secret put SECRET_NAME`
- Use for API keys, tokens, credentials
- Local development: store in `.dev.vars` file

### Bindings

Bindings connect Workers to Cloudflare resources, accessed via the `env` object:

```typescript
// KV namespace
await env.MY_KV.get("key");

// D1 database
await env.DB.prepare("SELECT * FROM users").all();

// R2 bucket
await env.MY_BUCKET.get("file.txt");

// Durable Object
const id = env.MY_DO.idFromName("object-name");
const stub = env.MY_DO.get(id);
await stub.fetch(request);

// Service binding
await env.MY_SERVICE.myMethod(arg);
```

---

## Execution Model and Ontology

### V8 Isolates

**What are isolates?**
- Lightweight execution contexts within a single process
- Use Google's V8 JavaScript engine (same as Chrome and Node.js)
- Multiple isolates can run in one process, switching thousands of times per second
- Memory is completely isolated between isolates

**Advantages over containers/VMs:**
- **Startup time**: ~100x faster than Node.js in a container
- **Memory**: Order of magnitude less memory on startup
- **Density**: Single runtime can host hundreds/thousands of isolates
- **Security**: Memory isolation prevents access to other code

### Edge Network Architecture

**Global Distribution:**
- Workers run on every Cloudflare data center (hundreds of locations)
- Each location can execute any Worker
- Automatic routing to nearest location

**Request Flow:**
1. User makes request
2. Cloudflare edge receives request at nearest data center
3. Worker executes in that location
4. Response returned to user

### Cold Starts

**Official Claims:**
- Isolates start in single-digit milliseconds
- 99.99% warm request rate (achieved via "Shard and Conquer" technique)
- Zero spin-up time in practice

**Real-World Experiences:**
- Users report first request: 60-500ms
- Subsequent requests: <10ms
- WebAssembly workloads may see higher initial latency (200-800ms)
- Discrepancy likely due to total request latency vs. pure worker startup time

**Cold Start Mitigation:**
- Smart Placement: Runs Workers closer to backend infrastructure when beneficial
- Consistent hashing distributes traffic to keep Workers warm
- No hard limits on Worker duration (unlike Lambda's 15-minute limit)

### Smart Placement

Workers can be automatically placed in optimal locations to minimize latency:
- If running backend logic, may execute closer to backend infrastructure
- Reduces round-trip time between Worker and origin servers
- Configurable via dashboard or API

---

## Integration Patterns

### Workers KV (Key-Value Storage)

**Best for:**
- Session data
- Configuration data
- API keys and credentials
- High-read, low-write workloads
- Data that doesn't need immediate consistency

**Characteristics:**
- Eventually consistent (global replication)
- Optimized for high read volumes with low latency
- Global distribution across Cloudflare network
- Simple key-value interface

**API Example:**
```typescript
// Write
await env.MY_KV.put("key", "value", {
  expirationTtl: 3600, // expire in 1 hour
  metadata: { userId: "123" }
});

// Read
const value = await env.MY_KV.get("key");
const valueAsJson = await env.MY_KV.get("key", "json");

// Delete
await env.MY_KV.delete("key");

// List keys
const list = await env.MY_KV.list({ prefix: "user:" });
```

### D1 (SQL Database)

**Best for:**
- Relational data
- Structured queries
- Transactional workloads
- Per-user or per-customer state

**Characteristics:**
- SQLite-based serverless database
- SQL interface with prepared statements
- Built on Durable Objects
- Global distribution

**API Example:**
```typescript
// Single query
const result = await env.DB.prepare(
  "SELECT * FROM users WHERE email = ?"
).bind(email).first();

// Batch queries
const results = await env.DB.batch([
  env.DB.prepare("INSERT INTO users (name) VALUES (?)").bind("Alice"),
  env.DB.prepare("INSERT INTO users (name) VALUES (?)").bind("Bob")
]);

// All results
const { results } = await env.DB.prepare(
  "SELECT * FROM products"
).all();
```

### R2 (Object Storage)

**Best for:**
- Large files
- Media assets (images, videos)
- User-uploaded content
- Unstructured data

**Characteristics:**
- S3-compatible API
- No egress fees
- Global distribution
- Supports multipart uploads

**API Example:**
```typescript
// Upload
await env.MY_BUCKET.put("file.txt", "content", {
  httpMetadata: {
    contentType: "text/plain"
  },
  customMetadata: {
    uploadedBy: "user123"
  }
});

// Download
const object = await env.MY_BUCKET.get("file.txt");
if (object) {
  const text = await object.text();
  const headers = object.httpMetadata;
}

// Delete
await env.MY_BUCKET.delete("file.txt");

// List objects
const list = await env.MY_BUCKET.list({ prefix: "images/" });
```

### Durable Objects

**Best for:**
- Stateful serverless workloads
- Real-time collaboration (chat, multiplayer games)
- Distributed coordination
- WebSocket connections
- Per-user/per-customer state

**Characteristics:**
- Combines compute + storage in a single object
- Globally unique instances
- Single-threaded execution (no concurrency issues)
- Strongly consistent storage
- Can hold WebSocket connections

**Key Coordination Patterns:**

1. **Multi-Client Coordination**: Use WebSockets to coordinate between clients
2. **Actor Model**: Each object is an actor processing messages
3. **Hierarchical Coordination**: Parent objects managing child objects
4. **Pure Coordination**: In-memory state without persistent storage (e.g., rate limiters)

**API Example:**
```typescript
// Define Durable Object class
export class Counter extends DurableObject {
  async fetch(request: Request) {
    let count = (await this.ctx.storage.get("count")) || 0;
    count++;
    await this.ctx.storage.put("count", count);
    return new Response(count.toString());
  }
}

// Access from Worker
const id = env.COUNTER.idFromName("global-counter");
const stub = env.COUNTER.get(id);
const response = await stub.fetch(request);
```

**WebSocket Hibernation:**
Workers can hibernate WebSocket connections in Durable Objects to save costs while maintaining connection state.

**blockConcurrencyWhile:**
```typescript
this.ctx.blockConcurrencyWhile(async () => {
  // Initialization code - no other requests processed during this
});
```

### Queues

**Best for:**
- Guaranteed delivery
- Offloading work from requests
- Worker-to-Worker messaging
- Buffering/batching data

**Characteristics:**
- At-least-once delivery
- Pull or push-based consumers
- Batch processing support
- No egress fees

**Patterns:**

1. **Producer-Consumer**: Single producer → queue → single consumer
2. **Fan-out**: Route messages to different queues based on attributes
3. **Pull-based**: HTTP consumer pulls messages on-demand

**API Example:**
```typescript
// Producer
await env.MY_QUEUE.send({
  type: "email",
  to: "user@example.com",
  subject: "Welcome"
});

// Consumer
export default {
  async queue(batch: MessageBatch, env: Env) {
    for (const message of batch.messages) {
      // Process message
      await processMessage(message.body);
      message.ack(); // Acknowledge successful processing
    }
  }
}
```

### Hyperdrive (Database Connection Pooling)

**Best for:**
- Connecting to external databases (Postgres, MySQL)
- Reducing connection overhead
- Global database access

**Characteristics:**
- Connection pooling globally distributed
- Query caching
- Supports Postgres, MySQL, and compatible databases
- Reduces latency by handling connection setup at the edge

**How it works:**
- Connection pool maintained between Worker and database
- Reuses connections instead of creating new ones
- Connection setup happens at edge (low latency)
- Actual database queries routed through optimal pools

**API Example:**
```typescript
const client = env.HYPERDRIVE.connect();
const result = await client.query("SELECT * FROM users WHERE id = $1", [userId]);
```

### Vectorize (Vector Database)

**Best for:**
- Semantic search
- Recommendation systems
- RAG (Retrieval Augmented Generation) for AI
- Document classification
- Anomaly detection

**Characteristics:**
- Globally distributed vector database
- Supports up to 5 million vectors per index
- Fast similarity search
- Free tier available

**Integration with Workers AI:**
Generate embeddings and store/query them in the same Worker.

**API Example:**
```typescript
// Insert vectors
await env.VECTORIZE_INDEX.insert([
  {
    id: "doc1",
    values: [0.1, 0.2, 0.3, ...], // embedding vector
    metadata: { title: "Document 1" }
  }
]);

// Query
const results = await env.VECTORIZE_INDEX.query(
  [0.15, 0.22, 0.31, ...], // query vector
  { topK: 5 }
);
```

### Workers AI

**Best for:**
- AI inference at the edge
- Text generation
- Embeddings
- Image classification
- Translation

**Characteristics:**
- Serverless GPU-powered inference
- Pay per inference (no hourly billing)
- 2-4x faster inference (2025 updates)
- Batch API for large workloads
- LoRA fine-tuning support

**Recent Updates (2025):**
- Llama 3.3 70B: 2-4x speed boost
- BGE embedding models: 2x faster
- Batch inference API
- Enhanced LoRA support (8 new models, ranks up to 32)

**API Example:**
```typescript
const response = await env.AI.run("@cf/meta/llama-3-8b-instruct", {
  prompt: "What is Cloudflare Workers?"
});

const embedding = await env.AI.run("@cf/baai/bge-base-en-v1.5", {
  text: "Document to embed"
});
```

### Analytics Engine

**Best for:**
- Custom metrics and telemetry
- Time-series analytics
- High-cardinality data
- Usage-based billing

**Characteristics:**
- Unlimited cardinality
- SQL-based querying
- 90-day free retention
- Fast writes from Workers
- Integration with Grafana

**API Example:**
```typescript
env.ANALYTICS.writeDataPoint({
  blobs: ["userId123", "api-endpoint"], // dimensions
  doubles: [1.5, 250], // metrics
  indexes: ["userId123"] // for faster queries
});
```

### Multi-Product Integration Example

```typescript
export default {
  async fetch(request: Request, env: Env) {
    // Check KV cache
    const cached = await env.KV.get("data");
    if (cached) return new Response(cached);

    // Query D1
    const dbResult = await env.DB.prepare(
      "SELECT * FROM items LIMIT 10"
    ).all();

    // Store large files in R2
    await env.R2.put("backup.json", JSON.stringify(dbResult));

    // Log to Analytics Engine
    env.ANALYTICS.writeDataPoint({
      blobs: ["fetch-items"],
      doubles: [dbResult.results.length]
    });

    // Send to Queue for async processing
    await env.QUEUE.send({ action: "backup" });

    const response = JSON.stringify(dbResult);
    await env.KV.put("data", response, { expirationTtl: 300 });

    return new Response(response);
  }
}
```

---

## Platform Limits

### Memory
- **128 MB per isolate** (includes JavaScript heap + WebAssembly memory)

### CPU Time
- **Default: 30 seconds** (protects against buggy code)
- **Maximum: 5 minutes (300,000 ms)** with opt-in via `cpu_ms` config
- CPU time = actual processing time (excludes I/O wait time)
- Paid plan: billed per CPU millisecond

### Request Limits
- **URL length**: 16 KB
- **Request headers**: 32 KB total, 16 KB per header
- **Request body**: Varies by Cloudflare plan (separate from Workers plan)
  - Free/Pro/Business: 100 MB
  - Enterprise: 500 MB
- **Subrequests**:
  - Bundled: 50 per request
  - Unbound: 1,000 per request

### Response Limits
- **No hard limit** on response size
- **Cache limits**: 512 MB (Free/Pro/Business), 5 GB (Enterprise)

### Worker Size
- **Free plan**: 3 MB after compression, 64 MB before
- **Paid plan**: 10 MB after compression, 64 MB before

### Worker Count
- **Free plan**: 100 Workers
- **Paid plan**: 500 Workers

### Duration
- **No hard limit** on wall-clock time (unlike AWS Lambda)
- Limited by CPU time instead

### Compatibility Dates
- Workers maintain backward compatibility forever
- New projects should use current date
- Old dates continue working indefinitely

---

## Best Practices

### Performance Optimization

1. **Use Smart Placement**
   - For backend-heavy Workers, enable Smart Placement to run closer to origin
   - Reduces latency between Worker and backend services

2. **Leverage Cache API**
   - Cache frequently accessed data
   - Can reduce response times to ~10ms
   - Use appropriate cache keys and TTLs

3. **Stream Large Responses**
   - Use ReadableStream/TransformStream for large files
   - Avoids buffering entire response in memory
   - Return response before writing chunks

4. **Optimize Subrequests**
   - Minimize external API calls
   - Use parallel fetches when possible
   - Consider caching results in KV

5. **Minimize Bundle Size**
   - Remove unused dependencies
   - Use tree-shaking
   - Consider code splitting for large applications

6. **Use ctx.waitUntil() for Background Work**
   - Don't block response on non-critical operations
   - Log analytics, write to queues in background

### Development Practices

1. **Use ES Modules Syntax**
   - Modern, recommended approach
   - Better TypeScript support
   - Cleaner binding access via `env`

2. **Set Compatibility Date**
   - Always use current date for new projects
   - Opt into latest runtime improvements
   - Review compatibility flags for specific features

3. **Type Safety**
   - Use TypeScript for better DX
   - Install `@cloudflare/workers-types`
   - Generate types with `wrangler types`

4. **Environment Variables vs Secrets**
   - Never put sensitive data in `[vars]`
   - Use secrets for API keys, tokens, credentials
   - Store in `.dev.vars` for local development

5. **Error Handling**
   - Implement proper try-catch blocks
   - Use `ctx.passThroughOnException()` when appropriate
   - Log errors to external service via Tail Workers

6. **Testing Strategy**
   - Use `wrangler dev` for local testing
   - Test with Miniflare for unit tests
   - Use remote bindings sparingly (prefer local simulation)

### Storage Selection

**Use KV when:**
- High read volume, low write volume
- Eventual consistency acceptable
- Simple key-value access patterns
- Global distribution important

**Use D1 when:**
- Need relational queries
- Strong consistency required
- Complex data relationships
- Per-user/per-customer isolated state

**Use R2 when:**
- Storing large files (>1 MB)
- Media assets
- User uploads
- Need S3 compatibility

**Use Durable Objects when:**
- Need strong consistency
- Real-time coordination
- WebSocket connections
- Per-entity state (chat rooms, game sessions)

**Use Hyperdrive when:**
- Connecting to external databases
- Need connection pooling
- Query caching beneficial

### Security Best Practices

1. **CORS Configuration**
   - Don't use wildcard `*` for production
   - Specify allowed origins explicitly
   - Validate preflight requests properly

2. **Authentication**
   - Implement at edge to reject unauthorized requests early
   - Use JWT validation
   - Consider Mutual TLS for service-to-service
   - Always authenticate on origin servers too (defense in depth)

3. **Rate Limiting**
   - Use native Rate Limiting API
   - Consider Durable Objects for complex rate limiting
   - Implement per-user and per-resource limits
   - Use Cloudflare Firewall to block IPs at limits

4. **Input Validation**
   - Validate all user input
   - Sanitize data before storage
   - Use prepared statements for database queries

5. **Secrets Management**
   - Rotate secrets regularly
   - Use `wrangler secret put` (never commit to git)
   - Access via `env.SECRET_NAME`
   - Consider Secrets Store for advanced use cases

---

## Common Use Cases and Examples

### 1. API Gateway / Proxy

```typescript
export default {
  async fetch(request: Request, env: Env) {
    const url = new URL(request.url);

    // Route to different backends
    if (url.pathname.startsWith("/api/v1")) {
      return fetch(`https://api-v1.example.com${url.pathname}`);
    } else if (url.pathname.startsWith("/api/v2")) {
      return fetch(`https://api-v2.example.com${url.pathname}`);
    }

    return new Response("Not found", { status: 404 });
  }
}
```

### 2. A/B Testing

```typescript
export default {
  async fetch(request: Request) {
    const variant = Math.random() < 0.5 ? "A" : "B";

    const response = await fetch(request);
    const modifiedResponse = new Response(response.body, response);
    modifiedResponse.headers.set("X-Variant", variant);

    return modifiedResponse;
  }
}
```

### 3. Authentication / JWT Validation

```typescript
import { verify } from "@cfworker/jwt";

export default {
  async fetch(request: Request, env: Env) {
    const token = request.headers.get("Authorization")?.replace("Bearer ", "");

    if (!token) {
      return new Response("Unauthorized", { status: 401 });
    }

    try {
      const isValid = await verify(token, env.JWT_SECRET);
      if (!isValid) throw new Error("Invalid token");

      // Continue to origin
      return fetch(request);
    } catch (e) {
      return new Response("Unauthorized", { status: 401 });
    }
  }
}
```

### 4. Image Optimization / CDN

```typescript
export default {
  async fetch(request: Request, env: Env) {
    const url = new URL(request.url);

    // Fetch original image from R2
    const object = await env.IMAGES.get(url.pathname.slice(1));
    if (!object) {
      return new Response("Not found", { status: 404 });
    }

    // Apply transformations
    const transformed = await fetch(request, {
      cf: {
        image: {
          width: 800,
          quality: 85,
          format: "webp"
        }
      }
    });

    return transformed;
  }
}
```

### 5. URL Redirects / Rewrites

```typescript
const redirects = {
  "/old-page": "/new-page",
  "/legacy/*": "/modern/*"
};

export default {
  async fetch(request: Request) {
    const url = new URL(request.url);
    const redirect = redirects[url.pathname];

    if (redirect) {
      return Response.redirect(new URL(redirect, request.url), 301);
    }

    return fetch(request);
  }
}
```

### 6. Rate Limiting

```typescript
export default {
  async fetch(request: Request, env: Env) {
    const ip = request.headers.get("CF-Connecting-IP");
    const key = `rate:${ip}`;

    const { success } = await env.RATE_LIMITER.limit({ key });

    if (!success) {
      return new Response("Too many requests", { status: 429 });
    }

    return fetch(request);
  }
}
```

### 7. Serverless API

```typescript
import { Hono } from "hono";

const app = new Hono();

app.get("/users/:id", async (c) => {
  const id = c.req.param("id");
  const user = await c.env.DB.prepare(
    "SELECT * FROM users WHERE id = ?"
  ).bind(id).first();

  return c.json(user);
});

app.post("/users", async (c) => {
  const body = await c.req.json();
  await c.env.DB.prepare(
    "INSERT INTO users (name, email) VALUES (?, ?)"
  ).bind(body.name, body.email).run();

  return c.json({ success: true });
});

export default app;
```

### 8. CORS Proxy

```typescript
export default {
  async fetch(request: Request) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE",
          "Access-Control-Allow-Headers": "Content-Type"
        }
      });
    }

    const response = await fetch(request);
    const newResponse = new Response(response.body, response);
    newResponse.headers.set("Access-Control-Allow-Origin", "*");

    return newResponse;
  }
}
```

### 9. Cron-triggered Background Task

```typescript
export default {
  async scheduled(event: ScheduledEvent, env: Env, ctx: ExecutionContext) {
    // Fetch data from external API
    const response = await fetch("https://api.example.com/data");
    const data = await response.json();

    // Store in D1
    const stmt = env.DB.prepare(
      "INSERT INTO daily_data (date, value) VALUES (?, ?)"
    );

    ctx.waitUntil(
      stmt.bind(new Date().toISOString(), data.value).run()
    );
  }
}
```

### 10. Real-time Chat with Durable Objects

```typescript
export class ChatRoom extends DurableObject {
  sessions: Set<WebSocket>;

  constructor(state: DurableObjectState, env: Env) {
    super(state, env);
    this.sessions = new Set();
  }

  async fetch(request: Request) {
    const upgrade = request.headers.get("Upgrade");
    if (upgrade === "websocket") {
      const [client, server] = Object.values(new WebSocketPair());

      this.sessions.add(server);
      server.accept();

      server.addEventListener("message", (event) => {
        // Broadcast to all clients
        for (const session of this.sessions) {
          session.send(event.data);
        }
      });

      return new Response(null, { status: 101, webSocket: client });
    }

    return new Response("Expected WebSocket", { status: 400 });
  }
}
```

---

## Key Concepts and Terminology

### Isolate
A lightweight execution context using V8 engine. Provides variable scope and safe execution environment. Multiple isolates run in a single process.

### Edge
Cloudflare's global network of data centers. Workers execute at the "edge" (closest to the user).

### Runtime
The Workers runtime (workerd) is an open-source execution environment based on V8, running at every Cloudflare data center.

### Binding
A connection from a Worker to a Cloudflare resource (KV, D1, R2, Durable Object, Service, etc.). Accessed via the `env` parameter.

### Service Worker
Legacy syntax using `addEventListener`. Still supported but ES Modules recommended.

### ES Modules
Modern syntax using `export default`. Recommended for new Workers.

### Compatibility Date
Date specified in `wrangler.toml` that determines which runtime features are enabled. Use current date for new projects.

### Compatibility Flags
Individual feature flags to enable/disable specific runtime behaviors.

### Wrangler
Official CLI tool for developing, testing, and deploying Workers.

### Miniflare
Local simulator for Workers runtime, used by `wrangler dev`. Runs workerd locally.

### Smart Placement
Automatic optimization that places Workers closer to backend infrastructure when beneficial.

### Subrequest
HTTP request made from within a Worker (using `fetch()`).

### waitUntil
Context method to extend Worker execution for promises (useful for background tasks).

### passThroughOnException
Context method to forward request to origin if Worker throws unhandled exception.

### Deployment
A version of a Worker actively serving traffic. Can consist of one or two versions (for gradual rollouts).

### Version
Snapshot of Worker code, configuration, bindings, and compatibility settings.

### Worker
A JavaScript/WebAssembly program that runs on Cloudflare's edge network.

### Durable Object
A class instance that combines compute and storage, with globally unique instances and strong consistency.

### Stub
Reference to a Durable Object instance, used to send requests to it.

### Namespace
A container for KV key-value pairs, bound to a Worker.

### Bucket
An R2 storage container for objects (files).

### Queue
A message queue for Worker-to-Worker communication with guaranteed delivery.

### Cron Trigger
A schedule that invokes a Worker's `scheduled()` handler.

### Tail Worker
A Worker that consumes logs from other Workers in real-time.

### WebSocket Pair
A pair of WebSockets (client and server) for bidirectional communication.

### Hibernate
Feature allowing Durable Objects to pause WebSocket connections to save costs.

### Analytics Engine
Service for writing and querying custom metrics with unlimited cardinality.

### Vectorize
Vector database for semantic search and AI applications.

### Hyperdrive
Connection pooling and query caching for external databases.

### Workers AI
Serverless GPU-powered AI inference service.

---

## Development and Testing

### Wrangler CLI

**Installation:**
```bash
npm install -g wrangler
```

**Key Commands:**

1. **Initialize Project**
```bash
wrangler init my-worker
```

2. **Local Development**
```bash
wrangler dev
# Starts local server at http://localhost:8787
# Press 'd' for DevTools, 'x' to exit
```

3. **Deploy**
```bash
wrangler deploy
```

4. **View Logs**
```bash
wrangler tail
```

5. **Manage Secrets**
```bash
wrangler secret put API_KEY
wrangler secret list
wrangler secret delete API_KEY
```

6. **Generate Types**
```bash
wrangler types
# Generates TypeScript types from bindings
```

7. **Test Cron Triggers**
```bash
wrangler dev --test-scheduled
# Exposes /__scheduled endpoint for testing
```

### Local Development with Miniflare

Wrangler dev uses Miniflare + workerd for local execution:

- **Local mode** (default): Runs entirely on your machine
- **Remote mode**: Connects to Cloudflare preview environment
- **Hybrid mode**: Local execution with remote bindings

**Remote Bindings Example:**
```toml
# wrangler.toml
[[kv_namespaces]]
binding = "MY_KV"
id = "abc123"
preview_id = "def456"  # Uses production data in dev
```

### Debugging

**Chrome DevTools:**
1. Run `wrangler dev`
2. Press `d` to open DevTools
3. Set breakpoints, inspect variables, view network requests

**Console Logging:**
```typescript
console.log("Debug:", variable);
console.error("Error:", error);
console.warn("Warning:", warning);
```

**Breakpoint Debugging:**
```typescript
debugger; // Pauses execution when DevTools open
```

**Performance Monitoring:**
```typescript
const start = performance.now();
await doWork();
const duration = performance.now() - start;
console.log(`Took ${duration}ms`);
```

### Testing

**Vitest Integration:**
```typescript
import { env, createExecutionContext } from "cloudflare:test";
import { describe, it, expect } from "vitest";
import worker from "../src/index";

describe("Worker", () => {
  it("responds with Hello World", async () => {
    const request = new Request("http://example.com");
    const ctx = createExecutionContext();
    const response = await worker.fetch(request, env, ctx);

    expect(await response.text()).toBe("Hello World");
  });
});
```

### Environment Setup

**wrangler.toml Example:**
```toml
name = "my-worker"
main = "src/index.ts"
compatibility_date = "2025-01-17"

[vars]
ENVIRONMENT = "production"

[[kv_namespaces]]
binding = "MY_KV"
id = "abc123"

[[d1_databases]]
binding = "DB"
database_name = "my-database"
database_id = "def456"

[[r2_buckets]]
binding = "MY_BUCKET"
bucket_name = "my-bucket"

[triggers]
crons = ["0 0 * * *"]  # Daily at midnight UTC
```

**.dev.vars Example:**
```
API_KEY=secret-key-for-local-dev
DATABASE_URL=postgres://localhost/testdb
```

### TypeScript Setup

**tsconfig.json:**
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "lib": ["ES2022"],
    "types": ["@cloudflare/workers-types"]
  }
}
```

**Install types:**
```bash
npm install -D @cloudflare/workers-types
```

**Generate binding types:**
```bash
wrangler types
# Creates worker-configuration.d.ts
```

---

## Observability and Monitoring

### Real-time Logs

**wrangler tail:**
```bash
wrangler tail
# Shows console.log, errors, and request info in real-time
```

**Dashboard Logs:**
- View in Cloudflare dashboard under Workers → [Worker Name] → Logs
- Limited to 10 concurrent viewers

### Workers Logs

Persistent logs stored and queryable:
- Available in Cloudflare dashboard
- Query by time range, status code, etc.
- Retention based on plan

### Tail Workers

Process logs programmatically and forward to external services:

```typescript
export default {
  async tail(events: TraceItem[], env: Env, ctx: ExecutionContext) {
    const logs = events.map(event => ({
      scriptName: event.scriptName,
      logs: event.logs,
      exceptions: event.exceptions,
      outcome: event.outcome
    }));

    // Send to external service (Datadog, Sentry, etc.)
    ctx.waitUntil(
      fetch("https://logs.example.com", {
        method: "POST",
        body: JSON.stringify(logs)
      })
    );
  }
}
```

### Analytics Engine

Write custom metrics:

```typescript
env.ANALYTICS.writeDataPoint({
  blobs: ["endpoint", "userId"],  // Dimensions
  doubles: [responseTime, 1],      // Metrics
  indexes: ["userId"]              // For faster queries
});
```

Query with SQL:
```sql
SELECT
  blob1 as endpoint,
  AVG(double1) as avg_response_time,
  SUM(double2) as request_count
FROM MY_DATASET
WHERE timestamp > NOW() - INTERVAL '24' HOUR
GROUP BY endpoint
```

### Workers Metrics

Built-in metrics available in dashboard:
- Request count
- Error rate
- Success rate
- CPU time (P50, P75, P99)
- Duration
- Status code distribution

### Tracing

Automatic distributed tracing (open beta):
- Trace requests across Workers, Durable Objects, and services
- View in Cloudflare dashboard
- Identify performance bottlenecks

### Integration with External Services

**Sentry:**
```typescript
import * as Sentry from "@sentry/cloudflare";

Sentry.init({
  dsn: env.SENTRY_DSN,
  tracesSampleRate: 1.0
});
```

**Datadog:**
Use Tail Workers to forward logs to Datadog.

**Grafana:**
Query Analytics Engine data via SQL API.

---

## Security

### CORS

**Basic CORS Headers:**
```typescript
const corsHeaders = {
  "Access-Control-Allow-Origin": "https://example.com",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE",
  "Access-Control-Allow-Headers": "Content-Type, Authorization"
};

export default {
  async fetch(request: Request) {
    // Handle preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const response = await fetch(request);
    const newResponse = new Response(response.body, response);

    Object.entries(corsHeaders).forEach(([key, value]) => {
      newResponse.headers.set(key, value);
    });

    return newResponse;
  }
}
```

**With Hono:**
```typescript
import { cors } from "hono/cors";

app.use("/*", cors({
  origin: ["https://example.com"],
  allowHeaders: ["Content-Type", "Authorization"],
  allowMethods: ["GET", "POST", "PUT", "DELETE"]
}));
```

### Authentication

**Basic Auth:**
```typescript
function parseBasicAuth(request: Request): [string, string] | null {
  const auth = request.headers.get("Authorization");
  if (!auth || !auth.startsWith("Basic ")) return null;

  const decoded = atob(auth.slice(6));
  const [username, password] = decoded.split(":");
  return [username, password];
}

export default {
  async fetch(request: Request, env: Env) {
    const creds = parseBasicAuth(request);

    if (!creds || creds[0] !== env.USERNAME || creds[1] !== env.PASSWORD) {
      return new Response("Unauthorized", {
        status: 401,
        headers: { "WWW-Authenticate": "Basic" }
      });
    }

    return fetch(request);
  }
}
```

**JWT Validation:**
```typescript
import { verify } from "@cfworker/jwt";

export default {
  async fetch(request: Request, env: Env) {
    const token = request.headers.get("Authorization")?.replace("Bearer ", "");

    try {
      const payload = await verify(token, env.JWT_SECRET);
      // Add user context to request
      return fetch(request);
    } catch (e) {
      return new Response("Unauthorized", { status: 401 });
    }
  }
}
```

**OAuth 2.0:**
Workers can implement full OAuth 2.0 servers or clients.

**Mutual TLS:**
Validate client certificates for service-to-service authentication.

### Rate Limiting

**Native API:**
```typescript
export default {
  async fetch(request: Request, env: Env) {
    const { success } = await env.RATE_LIMITER.limit({
      key: `${request.headers.get("CF-Connecting-IP")}:${new URL(request.url).pathname}`
    });

    if (!success) {
      return new Response("Rate limit exceeded", {
        status: 429,
        headers: { "Retry-After": "60" }
      });
    }

    return fetch(request);
  }
}
```

**Configuration:**
```toml
[[unsafe.bindings]]
name = "RATE_LIMITER"
type = "ratelimit"
namespace_id = "1001"
simple = { limit = 100, period = 60 }
```

**Durable Objects Rate Limiter:**
```typescript
export class RateLimiter extends DurableObject {
  async fetch(request: Request) {
    const key = new URL(request.url).searchParams.get("key");
    const limit = 100;
    const window = 60000; // 1 minute

    const now = Date.now();
    const requests = (await this.ctx.storage.get(key)) || [];

    // Remove old requests
    const recent = requests.filter((time: number) => now - time < window);

    if (recent.length >= limit) {
      return new Response(JSON.stringify({ allowed: false }));
    }

    recent.push(now);
    await this.ctx.storage.put(key, recent);

    return new Response(JSON.stringify({ allowed: true }));
  }
}
```

### DDoS Protection

- **Unmetered DDoS protection** on all plans (including Free)
- **Rate limiting** is free and unmetered
- Must use custom domain (not workers.dev) for full protection
- Combine with Cloudflare Firewall for IP blocking

### Input Validation

```typescript
function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export default {
  async fetch(request: Request, env: Env) {
    const body = await request.json();

    if (!validateEmail(body.email)) {
      return new Response("Invalid email", { status: 400 });
    }

    // Continue processing
  }
}
```

### SQL Injection Prevention

Always use prepared statements with D1:

```typescript
// ❌ BAD - vulnerable to SQL injection
await env.DB.prepare(`SELECT * FROM users WHERE email = '${email}'`).all();

// ✅ GOOD - safe with parameter binding
await env.DB.prepare("SELECT * FROM users WHERE email = ?").bind(email).all();
```

### Content Security Policy

```typescript
const response = new Response(html, {
  headers: {
    "Content-Type": "text/html",
    "Content-Security-Policy": "default-src 'self'; script-src 'self' 'unsafe-inline'"
  }
});
```

---

## Pricing

### Free Plan

**Included:**
- 100,000 requests/day
- 100 Workers
- 10 ms CPU time per request
- KV: 100,000 reads/day, 1,000 writes/day, 1 GB storage
- Durable Objects: 1,000 requests/day, 1 GB storage (SQLite only)
- Analytics: Basic metrics

### Paid Plan

**Cost:**
- $0.30 per million requests
- $0.02 per million CPU milliseconds
- No data transfer fees

**Limits:**
- 500 Workers
- 30 seconds CPU time (configurable up to 5 minutes)
- Unlimited requests

**KV Pricing:**
- Reads: $0.50 per 10 million
- Writes: $5.00 per million
- Storage: $0.50 per GB/month

**Durable Objects Pricing:**
- Requests: $0.15 per million
- Duration: $12.50 per million GB-seconds
- Storage: $0.20 per GB/month (SQLite), $1.00 per GB/month (key-value)

**R2 Pricing:**
- Storage: $0.015 per GB/month
- Class A operations (writes): $4.50 per million
- Class B operations (reads): $0.36 per million
- No egress fees

**D1 Pricing:**
- Rows read: $0.001 per million
- Rows written: $1.00 per million
- Storage: $0.75 per GB/month

**Workers AI Pricing:**
- Varies by model
- Charged per inference (no hourly costs)
- Example: Llama 3 ~$0.01 per 1,000 tokens

**Analytics Engine:**
- Free 90-day retention
- $0.25 per million data points written

**Queues Pricing:**
- Operations: $0.40 per million
- Maximum retries included

---

## Migration and Compatibility

### From AWS Lambda

**Key Differences:**
- Workers use V8 isolates, not containers
- No 15-minute timeout (CPU time limits instead)
- Different runtime environment (not Node.js by default)
- No file system access
- Environment variables work differently

**Migration Steps:**
1. Refactor Node.js-specific code (fs, path, process)
2. Use `nodejs_compat` flag for Node.js APIs
3. Replace AWS SDK calls with Cloudflare equivalents
4. Update handler signature to Workers format
5. Configure bindings instead of IAM roles

**Cost Savings:**
Typical reduction: 80-95% vs AWS Lambda + data transfer fees

### From Vercel

**Official Migration Guide:**
https://developers.cloudflare.com/workers/static-assets/migration-guides/vercel-to-workers/

**Steps:**
1. Identify build command and output directory
2. Create `wrangler.toml` configuration
3. Configure custom domains (must be Cloudflare zones)
4. Deploy with `wrangler deploy`

**Considerations:**
- Vercel-specific features not supported
- Edge Functions are already Cloudflare Workers
- Review compatibility matrix

### From Netlify

**Official Migration Guide:**
https://developers.cloudflare.com/workers/static-assets/migration-guides/netlify-to-workers/

**Steps:**
1. Find build settings
2. Set up Wrangler configuration
3. Migrate Netlify Functions to Workers
4. Deploy to Cloudflare

**Note:**
Netlify's Edge Functions are built on Cloudflare Workers, so migration is straightforward.

### Compatibility Dates and Flags

**Set Compatibility Date:**
```toml
# wrangler.toml
compatibility_date = "2025-01-17"  # Use current date
```

**Enable Node.js APIs:**
```toml
compatibility_flags = ["nodejs_compat"]
```

**Important Flags:**
- `nodejs_compat`: Enable Node.js runtime APIs and polyfills
- `nodejs_als`: Enable AsyncLocalStorage only
- `streams_enable_constructors`: Enable stream constructors

**Backward Compatibility:**
- Workers runtime supports old compatibility dates forever
- No forced upgrades or deprecations
- Safe to use old dates for existing Workers

### Module Format Migration

**From Service Worker to ES Modules:**

**Before (Service Worker):**
```typescript
addEventListener("fetch", (event) => {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  return new Response("Hello World");
}
```

**After (ES Modules):**
```typescript
export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext) {
    return new Response("Hello World");
  }
}
```

**Key Changes:**
- `addEventListener` → `export default`
- Global bindings → `env` parameter
- `event.waitUntil()` → `ctx.waitUntil()`

---

## Additional Resources

### Official Documentation
- **Workers Docs**: https://developers.cloudflare.com/workers/
- **API Reference**: https://developers.cloudflare.com/workers/runtime-apis/
- **Examples**: https://developers.cloudflare.com/workers/examples/
- **Discord Community**: https://discord.gg/cloudflaredev

### Tools and Libraries

**Routing:**
- Hono: https://hono.dev/
- itty-router: https://github.com/kwhitley/itty-router
- Worker Router: https://workers.tools/router/

**ORM/Database:**
- Drizzle ORM: Works with D1
- Prisma: Limited support

**Full Stack Frameworks:**
- Remix: Full support for Workers
- Next.js: Via @cloudflare/next-on-pages
- SvelteKit: Adapter available
- Astro: SSR adapter available

**Testing:**
- Vitest: Official integration
- Miniflare: Local testing

**CLI:**
- Wrangler: https://github.com/cloudflare/workers-sdk

### Community Resources
- **Workers Blog**: https://blog.cloudflare.com/tag/workers/
- **GitHub Discussions**: https://github.com/cloudflare/workers-sdk/discussions
- **Discord**: Active community support
- **Stack Overflow**: Tag `cloudflare-workers`

### Learning Paths
- **Cloudflare Learning Paths**: https://developers.cloudflare.com/learning-paths/workers/
- **Workers Playground**: https://workers.cloudflare.com/playground
- **Tutorials**: https://developers.cloudflare.com/workers/tutorials/

---

## Summary

Cloudflare Workers provides a powerful, globally distributed serverless platform with unique advantages:

**Strengths:**
- Near-zero cold starts with V8 isolates
- Global edge network (hundreds of locations)
- No egress fees
- Rich ecosystem of storage and compute products
- Excellent developer experience with Wrangler
- Competitive pricing

**Best For:**
- API gateways and proxies
- Edge computing and CDN logic
- Real-time applications (WebSockets + Durable Objects)
- Serverless APIs and backends
- Image optimization and transformation
- Authentication and authorization
- A/B testing and feature flags
- Rate limiting and DDoS protection

**Considerations:**
- 128 MB memory limit
- CPU time limits (30s default, 5min max)
- Different runtime than Node.js (use `nodejs_compat` for Node APIs)
- Eventual consistency for KV

**Ecosystem Integration:**
Workers integrates seamlessly with D1 (SQL), KV (key-value), R2 (object storage), Durable Objects (stateful compute), Queues (messaging), Hyperdrive (database pooling), Vectorize (vector database), and Workers AI (inference), providing a complete platform for building full-stack applications entirely on Cloudflare.

With continuous improvements in performance (2-4x faster inference in 2025), expanded capabilities (batch APIs, enhanced LoRA support), and a growing ecosystem of tools and frameworks, Cloudflare Workers is positioned as a leading edge computing platform for modern web applications.
