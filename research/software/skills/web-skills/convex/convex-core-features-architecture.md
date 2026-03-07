# Convex: Core Features and Architecture

## Executive Summary

Convex is an open-source, reactive backend platform that combines a document-relational database, serverless functions, and real-time synchronization into a unified TypeScript-first development environment. It provides ACID-compliant transactions, end-to-end type safety, and automatic real-time updates, eliminating the need for separate databases, API servers, and WebSocket infrastructure.

**Key Value Propositions:**
- Write database queries as TypeScript code directly in the database
- Automatic real-time reactivity - UI updates automatically when data changes
- End-to-end type safety from database schema to frontend
- Integrated backend platform (database, serverless functions, file storage, scheduling)
- No SQL, ORMs, or manual WebSocket management required

---

## 1. What is Convex?

### Overview

Convex is a hosted backend platform that provides:
- **Database**: A document-relational database with ACID compliance
- **Serverless Functions**: TypeScript/JavaScript functions running in the cloud
- **Real-time Sync**: Automatic WebSocket-based synchronization to clients
- **Client Libraries**: React hooks and JavaScript clients for seamless integration

### Core Philosophy

Just as React components react to state changes, Convex queries react to database changes. The platform tracks all dependencies for every query function, and whenever any dependency changes, Convex automatically reruns the query and updates all active subscriptions.

### Database Model: Document-Relational

Convex uses a hybrid "document-relational" model:
- **Document**: Store JSON-like nested objects in your database (similar to MongoDB)
- **Relational**: Use document IDs to create relationships between tables (like PostgreSQL)

This provides the flexibility of document databases with the structure of relational databases.

---

## 2. Core Concepts: Queries, Mutations, and Actions

### Queries

**Purpose**: Read data from the database

**Characteristics**:
- Pure functions that can only read from the database
- Deterministic - same inputs always produce same outputs
- Cannot call third-party APIs
- Automatically cached and subscribed to by clients
- Transactional reads with serializable isolation

**Example**:
```typescript
import { query } from "./_generated/server";
import { v } from "convex/values";

export const listMessages = query({
  args: { channel: v.string() },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("messages")
      .withIndex("by_channel", (q) => q.eq("channel", args.channel))
      .order("desc")
      .take(100);
  },
});
```

**Client Usage**:
```typescript
const messages = useQuery(api.messages.listMessages, { channel: "general" });
// Automatically updates when messages change!
```

### Mutations

**Purpose**: Write data to the database

**Characteristics**:
- Can read from and write to the database
- All changes happen in a single ACID transaction
- Either all changes commit or none do (atomic)
- Deterministic - cannot call third-party APIs
- Use optimistic concurrency control (OCC)

**Example**:
```typescript
import { mutation } from "./_generated/server";
import { v } from "convex/values";

export const sendMessage = mutation({
  args: {
    channel: v.string(),
    body: v.string(),
    author: v.string()
  },
  handler: async (ctx, args) => {
    const messageId = await ctx.db.insert("messages", {
      channel: args.channel,
      body: args.body,
      author: args.author,
      timestamp: Date.now(),
    });
    return messageId;
  },
});
```

### Actions

**Purpose**: Call third-party services and perform side effects

**Characteristics**:
- Can call external APIs (Stripe, OpenAI, etc.)
- Run in standard Node.js environment (or Convex runtime)
- Can interact with database indirectly by calling queries and mutations
- Not automatically retried on failure (due to side effects)
- Similar to AWS Lambda or Google Cloud Run functions

**Example**:
```typescript
import { action } from "./_generated/server";
import { api } from "./_generated/api";
import { v } from "convex/values";

export const processPayment = action({
  args: {
    userId: v.id("users"),
    amount: v.number()
  },
  handler: async (ctx, args) => {
    // Call external payment API
    const paymentResult = await stripe.charges.create({
      amount: args.amount,
      currency: "usd",
    });

    // Update database via mutation
    if (paymentResult.status === "succeeded") {
      await ctx.runMutation(api.payments.recordPayment, {
        userId: args.userId,
        amount: args.amount,
        stripeChargeId: paymentResult.id,
      });
    }

    return paymentResult;
  },
});
```

### Key Differences Summary

| Feature | Queries | Mutations | Actions |
|---------|---------|-----------|---------|
| Read Database | ✓ | ✓ | Via queries only |
| Write Database | ✗ | ✓ | Via mutations only |
| Third-party APIs | ✗ | ✗ | ✓ |
| Deterministic | ✓ | ✓ | ✗ |
| Automatic Retry | ✓ | ✓ | ✗ |
| Real-time Sync | ✓ | Triggers updates | ✗ |
| Transactional | ✓ | ✓ | ✗ |

---

## 3. The Convex Database

### Data Model

**Tables and Documents**:
- Tables contain documents (similar to JavaScript objects)
- Documents can be arbitrary Convex objects
- Supports JSON plus 64-bit integers and binary data
- Fields can contain nested arrays and objects

**Example Document**:
```typescript
{
  _id: "kjh23kj4h23k4j",  // Auto-generated document ID
  _creationTime: 1699234567890,  // Auto-generated timestamp
  title: "My Task",
  completed: false,
  tags: ["urgent", "work"],
  assignee: {
    userId: "user123",
    name: "Alice"
  }
}
```

### Relational Data

Reference documents using IDs:
```typescript
// Users table
{
  _id: "user123",
  name: "Alice",
  email: "alice@example.com"
}

// Tasks table
{
  _id: "task456",
  title: "Complete project",
  assignedTo: "user123",  // References user by ID
  projectId: "proj789"     // References project by ID
}
```

### Database Operations

**Basic CRUD**:
```typescript
// Create
const id = await ctx.db.insert("tasks", { title: "New task" });

// Read
const task = await ctx.db.get(id);

// Update
await ctx.db.patch(id, { completed: true });

// Delete
await ctx.db.delete(id);
```

**Querying**:
```typescript
// Get all documents
const allTasks = await ctx.db.query("tasks").collect();

// Filter with index
const userTasks = await ctx.db
  .query("tasks")
  .withIndex("by_user", (q) => q.eq("assignedTo", userId))
  .collect();

// Order and limit
const recentTasks = await ctx.db
  .query("tasks")
  .order("desc")
  .take(10);
```

### Indexes

Indexes improve query performance on large tables:

**Defining Indexes** (in `convex/schema.ts`):
```typescript
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  tasks: defineTable({
    title: v.string(),
    completed: v.boolean(),
    assignedTo: v.id("users"),
    dueDate: v.number(),
  })
    .index("by_user", ["assignedTo"])
    .index("by_user_and_status", ["assignedTo", "completed"])
    .index("by_due_date", ["dueDate"]),
});
```

**Using Indexes**:
```typescript
// Efficient indexed query
const completed = await ctx.db
  .query("tasks")
  .withIndex("by_user_and_status", (q) =>
    q.eq("assignedTo", userId).eq("completed", true)
  )
  .collect();
```

**Best Practices**:
- Use `.withIndex()` conditions instead of `.filter()` when possible
- Avoid redundant indexes (e.g., if you have `by_foo_and_bar`, you don't need `by_foo`)
- Indexes save on query performance but add storage overhead

### Transactions and Consistency

**ACID Compliance**:
- **Atomicity**: All changes in a mutation succeed or all fail
- **Consistency**: Database always in valid state
- **Isolation**: Serializable isolation - strictest guarantee
- **Durability**: Committed transactions are permanent

**Optimistic Concurrency Control (OCC)**:
- Mutations execute assuming no conflicts
- If conflict detected, transaction automatically retries
- Provides high performance with strong consistency

---

## 4. Type Safety and Schema Definitions

### End-to-End Type Safety

Convex provides automatic type safety from database to frontend:

**1. Define Schema** (`convex/schema.ts`):
```typescript
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  users: defineTable({
    name: v.string(),
    email: v.string(),
    age: v.optional(v.number()),
    roles: v.array(v.string()),
  }).index("by_email", ["email"]),

  tasks: defineTable({
    title: v.string(),
    completed: v.boolean(),
    assignedTo: v.id("users"),
    dueDate: v.optional(v.number()),
  })
    .index("by_user", ["assignedTo"])
    .index("by_status", ["completed"]),
});
```

**2. Generated Types**:
Convex automatically generates TypeScript types in `convex/_generated/dataModel.ts`:
```typescript
// Auto-generated types
type User = {
  _id: Id<"users">;
  _creationTime: number;
  name: string;
  email: string;
  age?: number;
  roles: string[];
};

type Task = {
  _id: Id<"tasks">;
  _creationTime: number;
  title: string;
  completed: boolean;
  assignedTo: Id<"users">;
  dueDate?: number;
};

// Use in functions
export type Doc<TableName> = ...
```

**3. Type-Safe Functions**:
```typescript
import { query } from "./_generated/server";
import { Doc } from "./_generated/dataModel";

export const getUser = query({
  args: { userId: v.id("users") },
  handler: async (ctx, args): Promise<Doc<"users"> | null> => {
    return await ctx.db.get(args.userId);
  },
});
```

**4. Type-Safe Client**:
```typescript
import { useQuery } from "convex/react";
import { api } from "../convex/_generated/api";

function UserProfile({ userId }: { userId: Id<"users"> }) {
  // TypeScript knows the exact shape of the returned data!
  const user = useQuery(api.users.getUser, { userId });

  if (!user) return <div>Loading...</div>;

  return (
    <div>
      <h1>{user.name}</h1>  {/* TypeScript knows user.name exists */}
      <p>{user.email}</p>
    </div>
  );
}
```

### Runtime Validation

**Schema Validation**:
- After schema is deployed, all inserts/updates are validated
- Invalid data is rejected at runtime
- Prevents data corruption

**Function Argument Validation**:
```typescript
import { v } from "convex/values";

export const createTask = mutation({
  args: {
    title: v.string(),
    dueDate: v.optional(v.number()),
    tags: v.array(v.string()),
    priority: v.union(
      v.literal("low"),
      v.literal("medium"),
      v.literal("high")
    ),
  },
  handler: async (ctx, args) => {
    // args are validated before handler runs
    // TypeScript knows exact types
  },
});
```

### Validators Reference

Common validator types:
```typescript
import { v } from "convex/values";

// Primitives
v.string()
v.number()
v.boolean()
v.null()
v.id("tableName")  // Document ID

// Containers
v.array(v.string())
v.object({
  name: v.string(),
  age: v.number()
})

// Optionals and Unions
v.optional(v.string())
v.union(v.string(), v.number())
v.literal("specific_value")

// Special
v.any()  // Avoid when possible
v.bytes()  // Binary data
```

---

## 5. Real-Time Reactivity and Subscriptions

### How Reactivity Works

**Automatic Dependency Tracking**:
1. Client calls `useQuery()` to subscribe to a query
2. Query function reads data from database
3. Convex tracks which documents/rows were read
4. When tracked data changes, query automatically reruns
5. New results pushed to all subscribed clients via WebSocket

**Example Flow**:
```typescript
// Client subscribes
const messages = useQuery(api.messages.list, { channel: "general" });

// User posts a message (mutation)
await runMutation(api.messages.send, {
  channel: "general",
  text: "Hello!"
});

// Convex automatically:
// 1. Detects messages table changed
// 2. Reruns list query
// 3. Pushes update to all subscribed clients
// 4. React re-renders with new data
```

### Client Implementation

**React Hooks**:
```typescript
import { useQuery, useMutation } from "convex/react";
import { api } from "../convex/_generated/api";

function ChatRoom() {
  // Subscribe to messages - auto-updates on changes
  const messages = useQuery(api.messages.list, {
    channel: "general"
  });

  // Get mutation function
  const sendMessage = useMutation(api.messages.send);

  const handleSend = () => {
    sendMessage({
      channel: "general",
      text: "Hello!"
    });
    // UI will update automatically when mutation completes
  };

  return (
    <div>
      {messages?.map(msg => (
        <div key={msg._id}>{msg.text}</div>
      ))}
      <button onClick={handleSend}>Send</button>
    </div>
  );
}
```

**JavaScript Client** (for non-React apps):
```typescript
import { ConvexClient } from "convex/browser";

const client = new ConvexClient(CONVEX_URL);

// Subscribe to query
const unsubscribe = client.subscribe(
  api.messages.list,
  { channel: "general" },
  (messages) => {
    console.log("Messages updated:", messages);
    // Update UI with new messages
  }
);

// Call mutation
await client.mutation(api.messages.send, {
  channel: "general",
  text: "Hello!",
});

// Cleanup
unsubscribe();
```

### WebSocket Connection

**Automatic Management**:
- Client libraries connect via WebSocket automatically
- Connection maintained in background
- Automatic reconnection on network issues
- Efficient binary protocol for updates

**Consistent Snapshots**:
- All subscriptions update to same database snapshot simultaneously
- Ensures UI always shows consistent data
- No race conditions or partial updates

### Performance Characteristics

**Efficient Updates**:
- Only changed data is transmitted
- Smart diffing of query results
- Batched updates for multiple changes
- Scales to thousands of concurrent subscriptions per deployment

**Query Caching**:
- Queries automatically cached server-side
- Multiple clients share cached results
- Cache invalidated when dependencies change

---

## 6. The Convex Runtime Environment

### Function Execution

**Two Execution Environments**:

1. **Convex Runtime** (default):
   - Isolated JavaScript environment
   - Runs queries, mutations, and lightweight actions
   - Fast startup, efficient execution
   - Limited to deterministic operations

2. **Node.js Runtime**:
   - Full Node.js 18 environment
   - Runs in AWS Lambda
   - For actions requiring Node.js libraries
   - Specified with `"use node"` directive

### Serverless Architecture

**How Functions Execute**:
- Functions exported from `convex/` directory
- Deployed as serverless functions
- Execute on-demand, then hibernate
- Auto-scaling based on load

**Example Structure**:
```
convex/
├── _generated/          # Auto-generated types
├── schema.ts           # Database schema
├── messages.ts         # Message functions
├── users.ts            # User functions
├── http.ts             # HTTP endpoints
└── crons.ts            # Scheduled functions
```

### Development vs Production

**Development Environment**:
- Each developer has personal dev deployment
- Changes pushed instantly with `npx convex dev`
- Types regenerated automatically
- Full isolation from production

**Production Environment**:
- Shared production deployment
- Deploy with `npx convex deploy`
- Can integrate with CI/CD pipelines
- Zero-downtime deployments

### Cloud Infrastructure

**Hosted Platform**:
- Runs on Amazon RDS (MySQL for persistence)
- Serverless functions on AWS Lambda (for Node.js actions)
- Global CDN for low-latency access
- Automatic backups and redundancy

**Open Source Option**:
- Self-hosted backend available
- Uses SQLite, PostgreSQL, or MySQL
- Same API as cloud version

---

## 7. Development and Deployment Workflow

### Initial Setup

**1. Install Convex**:
```bash
npm install convex
npx convex dev
```

**2. First Run Options**:
- **Cloud Development**: Creates account, project, and dev deployment
- **Local Development**: Runs open-source backend locally

**3. Environment Variables**:
Convex automatically creates `.env.local`:
```bash
CONVEX_DEPLOYMENT=dev:your-deployment-123456
CONVEX_URL=https://your-deployment.convex.cloud
```

### Development Workflow

**`npx convex dev`** (keep running during development):
- Watches `convex/` folder for changes
- Pushes functions, schema, and indexes on save
- Regenerates TypeScript types in `convex/_generated/`
- Hot-reloads changes instantly

**Typical Development Flow**:
```bash
# Terminal 1: Run Convex dev server
npx convex dev

# Terminal 2: Run your app
npm run dev
```

**Development Features**:
- Real-time function updates (no restart needed)
- Type checking with TypeScript
- Automatic schema migrations
- Personal dev deployment per developer

### Deployment to Production

**Manual Deployment**:
```bash
npx convex deploy
```
- Prompts for confirmation
- Bundles functions and dependencies
- Pushes to production deployment
- Zero downtime - old functions serve requests during deployment
- New functions available immediately after deployment

**CI/CD Integration**:
```yaml
# GitHub Actions example
- name: Deploy to Convex
  run: |
    npx convex deploy --cmd-url-env-var-name CONVEX_URL_PROD
  env:
    CONVEX_DEPLOY_KEY: ${{ secrets.CONVEX_DEPLOY_KEY }}
```

**Deployment Structure**:
- One production deployment per project
- One dev deployment per team member
- Preview deployments for branches (optional)
- Environment-specific configuration

### Project Configuration

**`convex.json`**:
```json
{
  "project": "your-project-123",
  "functions": "convex/",
  "generateTS": "convex/_generated"
}
```

**Schema Migrations**:
- Schema changes applied automatically on deploy
- Indexes built in background (for large tables)
- No manual migration scripts needed
- Rollback support for schema issues

### Database Dashboard

**Convex Dashboard** (https://dashboard.convex.dev):
- View and edit data in tables
- Monitor function execution
- View logs and errors
- Manage deployments and team members
- Configure scheduled functions
- View performance metrics

---

## 8. Advanced Features

### HTTP Actions and Webhooks

**Purpose**: Receive webhooks from external services or provide HTTP APIs

**Definition** (`convex/http.ts`):
```typescript
import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";

const http = httpRouter();

// Webhook endpoint
http.route({
  path: "/webhook/stripe",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const payload = await request.json();

    // Verify webhook signature
    // ...

    // Process webhook by calling mutation
    await ctx.runMutation(api.payments.processWebhook, payload);

    return new Response(null, { status: 200 });
  }),
});

export default http;
```

**Endpoint URL**: `https://your-deployment.convex.site/webhook/stripe`

### File Storage

**Built-in File Storage**:
- Store images, documents, videos
- Integrated with database (no separate service)
- Automatic URL generation
- Size limits and access control

**Uploading Files**:
```typescript
import { mutation } from "./_generated/server";

export const generateUploadUrl = mutation(async (ctx) => {
  return await ctx.storage.generateUploadUrl();
});

export const saveFile = mutation({
  args: { storageId: v.id("_storage"), fileName: v.string() },
  handler: async (ctx, args) => {
    await ctx.db.insert("files", {
      storageId: args.storageId,
      fileName: args.fileName,
    });
  },
});
```

**Client Upload**:
```typescript
const generateUploadUrl = useMutation(api.files.generateUploadUrl);
const saveFile = useMutation(api.files.saveFile);

const handleUpload = async (file: File) => {
  // Get upload URL
  const uploadUrl = await generateUploadUrl();

  // Upload file
  const result = await fetch(uploadUrl, {
    method: "POST",
    headers: { "Content-Type": file.type },
    body: file,
  });

  const { storageId } = await result.json();

  // Save metadata
  await saveFile({ storageId, fileName: file.name });
};
```

**Retrieving Files**:
```typescript
const url = await ctx.storage.getUrl(storageId);
// Returns: "https://your-deployment.convex.cloud/api/storage/abc123"
```

### Scheduled Functions and Cron Jobs

**Scheduling Options**:

**1. Cron Jobs** (`convex/crons.ts`):
```typescript
import { cronJobs } from "convex/server";
import { internal } from "./_generated/api";

const crons = cronJobs();

// Every day at 9 AM UTC
crons.daily(
  "daily cleanup",
  { hourUTC: 9, minuteUTC: 0 },
  internal.tasks.cleanupOldTasks
);

// Every hour
crons.hourly(
  "send reminders",
  { minuteUTC: 0 },
  internal.notifications.sendReminders
);

// Custom cron expression
crons.cron(
  "weekly report",
  "0 9 * * MON",  // Every Monday at 9 AM
  internal.reports.generateWeekly
);

export default crons;
```

**2. One-Time Scheduling**:
```typescript
export const scheduleTask = mutation({
  handler: async (ctx, args) => {
    // Schedule function to run in 1 hour
    await ctx.scheduler.runAfter(
      3600 * 1000,  // 1 hour in ms
      api.tasks.processTask,
      { taskId: "123" }
    );

    // Schedule for specific time
    await ctx.scheduler.runAt(
      new Date("2025-12-31T23:59:59Z"),
      api.notifications.sendNewYearGreeting
    );
  },
});
```

### Vector Search

**Purpose**: Semantic search using AI embeddings (for RAG, recommendations, etc.)

**Define Vector Index** (in schema):
```typescript
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  documents: defineTable({
    text: v.string(),
    embedding: v.array(v.float64()),
    metadata: v.object({
      title: v.string(),
      author: v.string(),
    }),
  }).vectorIndex("by_embedding", {
    vectorField: "embedding",
    dimensions: 1536,  // OpenAI embedding size
    filterFields: ["metadata.author"],  // Optional filters
  }),
});
```

**Search with Vectors**:
```typescript
import { action } from "./_generated/server";
import OpenAI from "openai";

export const searchDocuments = action({
  args: { query: v.string() },
  handler: async (ctx, args) => {
    // Generate embedding for query
    const openai = new OpenAI();
    const embedding = await openai.embeddings.create({
      model: "text-embedding-3-small",
      input: args.query,
    });

    // Vector search
    const results = await ctx.runQuery(
      api.documents.vectorSearch,
      {
        embedding: embedding.data[0].embedding,
        limit: 10,
      }
    );

    return results;
  },
});

export const vectorSearch = query({
  args: {
    embedding: v.array(v.float64()),
    limit: v.number(),
  },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("documents")
      .withIndex("by_embedding", (q) =>
        q.similar(args.embedding, args.limit)
      )
      .collect();
  },
});
```

**Results Include Similarity Scores**:
- Range: -1 (least similar) to 1 (most similar)
- Based on cosine similarity
- Supports millions of vectors

### Authentication

**Convex Auth** (built-in, beta):
```typescript
import { Auth } from "@convex-dev/auth/server";

const auth = new Auth({
  providers: [
    Google,
    GitHub,
    EmailPassword,
  ],
});

export default auth;
```

**Third-Party Integrations**:
- **Clerk**: Full-featured auth with webhooks
- **Auth0**: Enterprise authentication
- **Custom JWT**: Bring your own auth provider

**Accessing Auth in Functions**:
```typescript
export const getCurrentUser = query({
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) return null;

    return await ctx.db
      .query("users")
      .withIndex("by_token", (q) =>
        q.eq("tokenIdentifier", identity.tokenIdentifier)
      )
      .unique();
  },
});
```

---

## 9. Best Practices

### Performance Optimization

**1. Use Indexed Conditions**:
```typescript
// ❌ Inefficient - filters all documents
const tasks = await ctx.db
  .query("tasks")
  .filter((q) => q.eq(q.field("userId"), userId))
  .collect();

// ✅ Efficient - uses index
const tasks = await ctx.db
  .query("tasks")
  .withIndex("by_user", (q) => q.eq("userId", userId))
  .collect();
```

**2. Avoid Redundant Indexes**:
```typescript
// ❌ Redundant - only need the compound index
.index("by_user", ["userId"])
.index("by_user_and_status", ["userId", "status"])

// ✅ Efficient - compound index covers both use cases
.index("by_user_and_status", ["userId", "status"])
```

**3. Limit Query Results**:
```typescript
// Use take() to limit results
const recentTasks = await ctx.db
  .query("tasks")
  .order("desc")
  .take(50);  // Only fetch what you need
```

### TypeScript Best Practices

**1. Always Await Promises**:
```typescript
// ❌ Missing await - operation won't complete!
ctx.scheduler.runAfter(1000, api.tasks.process);

// ✅ Properly awaited
await ctx.scheduler.runAfter(1000, api.tasks.process);
```

**2. Use Type Inference**:
```typescript
// ✅ Types are inferred from schema
const task = await ctx.db.get(taskId);
// task is Doc<"tasks"> | null
```

**3. Enable ESLint Rules**:
```json
{
  "rules": {
    "@typescript-eslint/no-floating-promises": "error"
  }
}
```

### Code Organization

**1. Separate Concerns**:
```
convex/
├── users.ts          # User management
├── tasks.ts          # Task operations
├── auth.ts           # Authentication
├── payments.ts       # Payment processing
└── lib/              # Shared utilities
    ├── validators.ts
    └── helpers.ts
```

**2. Use Internal Functions**:
```typescript
import { internalMutation } from "./_generated/server";

// Only callable from other Convex functions
export const dangerousOperation = internalMutation({
  handler: async (ctx) => {
    // Can only be called from actions/mutations
    // Not exposed to client
  },
});
```

**3. Component Reusability**:
- Use Convex Components for shared functionality
- Pre-built components: rate limiting, presence, counters

### Security

**1. Validate All Inputs**:
```typescript
// Use validators to prevent invalid data
export const createTask = mutation({
  args: {
    title: v.string(),
    priority: v.union(
      v.literal("low"),
      v.literal("medium"),
      v.literal("high")
    ),
  },
  handler: async (ctx, args) => {
    // args are validated before this runs
  },
});
```

**2. Check Authorization**:
```typescript
export const deleteTask = mutation({
  args: { taskId: v.id("tasks") },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    const task = await ctx.db.get(args.taskId);
    if (task.ownerId !== identity.subject) {
      throw new Error("Not authorized");
    }

    await ctx.db.delete(args.taskId);
  },
});
```

**3. Use Internal Functions for Sensitive Operations**:
```typescript
// Internal - can't be called from client
export const adminDeleteUser = internalMutation({
  args: { userId: v.id("users") },
  handler: async (ctx, args) => {
    // Only callable from other Convex functions
    await ctx.db.delete(args.userId);
  },
});
```

### Testing

**Focus on Core Logic**:
- Test business logic and security-critical code
- Start with high-value tests
- Use Vitest or Jest for unit tests

**Example Test**:
```typescript
import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import schema from "./schema";
import { createTask } from "./tasks";

describe("tasks", () => {
  it("creates a task", async () => {
    const t = convexTest(schema);
    const taskId = await t.mutation(createTask, {
      title: "Test task",
      priority: "high",
    });

    const task = await t.run((ctx) => ctx.db.get(taskId));
    expect(task?.title).toBe("Test task");
  });
});
```

---

## 10. Integration with Frontend Frameworks

### React

**Setup**:
```typescript
import { ConvexProvider, ConvexReactClient } from "convex/react";

const convex = new ConvexReactClient(import.meta.env.VITE_CONVEX_URL);

function App() {
  return (
    <ConvexProvider client={convex}>
      <YourApp />
    </ConvexProvider>
  );
}
```

**Hooks**:
```typescript
import { useQuery, useMutation, useAction } from "convex/react";

function TaskList() {
  const tasks = useQuery(api.tasks.list);
  const createTask = useMutation(api.tasks.create);
  const processTask = useAction(api.tasks.process);

  // tasks updates automatically!
  return <div>{tasks?.map(...)}</div>;
}
```

### Next.js

**App Router** (recommended):
```typescript
// app/ConvexClientProvider.tsx
"use client";
import { ConvexProvider, ConvexReactClient } from "convex/react";

const convex = new ConvexReactClient(process.env.NEXT_PUBLIC_CONVEX_URL!);

export function ConvexClientProvider({ children }) {
  return <ConvexProvider client={convex}>{children}</ConvexProvider>;
}

// app/layout.tsx
import { ConvexClientProvider } from "./ConvexClientProvider";

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        <ConvexClientProvider>{children}</ConvexClientProvider>
      </body>
    </html>
  );
}
```

### Vue, Svelte, etc.

**Vanilla JavaScript Client**:
```typescript
import { ConvexClient } from "convex/browser";

const client = new ConvexClient(CONVEX_URL);

const unsubscribe = client.subscribe(
  api.tasks.list,
  {},
  (tasks) => {
    // Update UI with tasks
  }
);
```

---

## 11. Comparison with Traditional Stack

### Traditional Stack

**Components Needed**:
- Database (PostgreSQL, MongoDB)
- API Server (Express, FastAPI)
- ORM (Prisma, TypeORM)
- WebSocket Server (Socket.io)
- Hosting (AWS, Heroku)
- Type generation tools
- Migration management

**Manual Setup**:
- Configure database connections
- Write API endpoints
- Handle WebSocket connections
- Manage type synchronization
- Write migration scripts
- Deploy and scale separately

### Convex Stack

**All-in-One Platform**:
- ✅ Database (document-relational)
- ✅ Serverless functions
- ✅ Real-time sync (WebSocket)
- ✅ Type generation (automatic)
- ✅ Hosting (built-in)
- ✅ File storage
- ✅ Scheduled functions
- ✅ Vector search

**Automatic**:
- Connection management
- Type synchronization
- Schema migrations
- Real-time updates
- Scaling and deployment

---

## 12. Common Use Cases

### Real-Time Applications
- Chat applications
- Collaborative tools (docs, whiteboards)
- Live dashboards and monitoring
- Multiplayer games
- Social feeds

### AI-Powered Applications
- RAG (Retrieval Augmented Generation) with vector search
- AI chat interfaces with streaming
- Semantic search
- Document analysis
- Recommendation systems

### SaaS Applications
- Project management tools
- CRM systems
- Task management
- Team collaboration
- Customer portals

### E-Commerce
- Product catalogs
- Shopping carts
- Order management
- Real-time inventory
- Customer notifications

---

## 13. Learning Resources

### Official Documentation
- **Main Docs**: https://docs.convex.dev
- **Tutorial**: https://docs.convex.dev/tutorial
- **API Reference**: https://docs.convex.dev/api

### Community
- **Stack**: https://stack.convex.dev (blog with patterns and guides)
- **Discord**: Active community support
- **GitHub**: https://github.com/get-convex

### Example Projects
- Chat application
- Task manager
- E-commerce store
- AI chat with RAG
- Collaborative whiteboard

---

## 14. Key Takeaways for LLMs

When working with Convex projects, remember:

1. **Queries** are for reading data and automatically sync to clients
2. **Mutations** are for writing data in ACID transactions
3. **Actions** are for calling external APIs and services
4. **Schema** provides end-to-end type safety (define in `schema.ts`)
5. **Indexes** dramatically improve query performance (use `.withIndex()`)
6. Real-time updates are **automatic** (no manual WebSocket code)
7. All functions are in the `convex/` directory
8. Types are **auto-generated** in `convex/_generated/`
9. Use `npx convex dev` during development
10. Deploy with `npx convex deploy`

### Common File Locations
- Schema: `convex/schema.ts`
- Functions: `convex/*.ts`
- Generated types: `convex/_generated/`
- HTTP routes: `convex/http.ts`
- Cron jobs: `convex/crons.ts`
- Config: `convex.json`

### Type Safety Patterns
```typescript
// Import generated types
import { Doc, Id } from "./_generated/dataModel";
import { api } from "./_generated/api";

// Use Doc type for documents
const user: Doc<"users"> = await ctx.db.get(userId);

// Use Id type for references
const taskId: Id<"tasks"> = "...";

// Import API for calling functions
await ctx.runMutation(api.tasks.create, { ... });
```

---

## Conclusion

Convex represents a paradigm shift in backend development, combining database, serverless functions, and real-time sync into a unified TypeScript-first platform. By eliminating the complexity of managing separate services and manual synchronization, Convex enables developers to build sophisticated real-time applications with dramatically less code and infrastructure complexity.

The platform's automatic reactivity, end-to-end type safety, and integrated features make it particularly well-suited for modern applications requiring real-time updates, from collaborative tools to AI-powered apps with RAG capabilities.

For LLMs assisting with Convex projects, understanding the distinction between queries, mutations, and actions, along with the automatic type generation and real-time synchronization model, is essential for providing accurate and idiomatic guidance.
