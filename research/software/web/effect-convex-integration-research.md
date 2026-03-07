# Effect.ts and Convex Integration Research

## Executive Summary

Effect.ts can be successfully integrated with Convex through two community-built libraries: **Confect** and **@maple/convex-effect**. Both libraries provide functional wrappers that transform Convex's Promise-based APIs into Effect's functional programming paradigm, enabling developers to leverage Effect's powerful error handling, composition, and type safety features within Convex's backend-as-a-service platform.

**Key Findings:**
- Integration is **viable and production-ready** through mature wrapper libraries
- **Confect** provides deep integration with Effect Schema for database validation
- **@maple/convex-effect** offers a lightweight 1:1 mapping of Convex APIs to Effect
- Both work within Convex's deterministic constraints for queries and mutations
- Actions (non-deterministic functions) work seamlessly with Effect patterns
- TypeScript configuration requires `exactOptionalPropertyTypes: false` due to Convex limitations

**Recommended Use Cases:**
- Projects already using Effect.ts ecosystem
- Applications requiring sophisticated error handling and effect composition
- Teams comfortable with functional programming paradigms
- Scenarios benefiting from Effect Schema's validation capabilities

---

## 1. Compatibility Analysis

### 1.1 Can Effect.ts Be Used with Convex?

**Yes**, Effect.ts is fully compatible with Convex through integration libraries. While Convex does not natively support Effect.ts (it uses standard TypeScript with Promises), two robust community libraries provide seamless integration:

1. **Confect** ([@rjdellecese/confect](https://github.com/rjdellecese/confect))
   - Deep integration framework
   - 154+ GitHub stars
   - Active development (v0.0.32 as of November 2025)
   - Comprehensive Effect Schema integration

2. **@maple/convex-effect** (JSR package)
   - Lightweight wrapper approach
   - 1:1 API mapping
   - Available on JavaScript Registry (JSR)

### 1.2 Known Issues and Limitations

#### TypeScript Configuration Constraint

**Critical Limitation**: When using Confect, you must set `exactOptionalPropertyTypes: false` in your `tsconfig.json`. This is a limitation of the underlying `convex-js` library, not Confect itself.

```json
{
  "compilerOptions": {
    "exactOptionalPropertyTypes": false  // Required for Confect
  }
}
```

**Impact**: This conflicts with Effect Schema's recommended configuration (`exactOptionalPropertyTypes: true`), potentially reducing type strictness for optional properties.

#### Schema Restrictions

**Not every Effect Schema is valid for use in Confect.** While the documentation mentions schema restrictions, specific unsupported patterns include:

- Schemas that don't align with Convex's supported data types
- Complex transformations that cannot be serialized to Convex's document model
- Schemas requiring runtime effects during validation

**Note**: The full list of schema restrictions is documented in Confect's official documentation at [rjdellecese.gitbook.io/confect](https://rjdellecese.gitbook.io/confect).

#### Runtime Environment Constraints

Convex functions run in two possible environments:

1. **Convex Runtime** (Default): V8-based JavaScript runtime
   - Supports most browser-compatible npm packages
   - Works with Effect.ts as it's browser-compatible

2. **Node.js Runtime** (Actions only with `"use node"`)
   - Available only for actions
   - Also compatible with Effect.ts

**Compatibility Status**: Effect.ts works in both runtime environments as it's designed for modern JavaScript/TypeScript runtimes.

### 1.3 Compatibility in Different Function Types

#### Queries (✅ Supported)

Effect works in Convex queries with the following considerations:

- **Determinism requirement maintained**: Effect programs in queries must be pure and deterministic
- **No side effects allowed**: Cannot use Effect operations that perform I/O (same as standard Convex queries)
- **Effect.gen() pattern works**: Generator-based effect composition is fully supported

#### Mutations (✅ Supported)

Effect works in mutations with these characteristics:

- **Transactional guarantees preserved**: Effect programs execute within Convex's ACID transactions
- **Determinism required**: Same constraints as queries apply
- **Database operations wrapped**: Effect-based database operations maintain consistency

#### Actions (✅ Fully Supported)

Actions are the **ideal use case** for Effect integration:

- **No determinism constraints**: Can use full Effect ecosystem including I/O operations
- **External API calls**: Effect's fetch capabilities work seamlessly
- **Complex effect composition**: Leverage Effect's full power for orchestrating external services
- **Error handling**: Effect's sophisticated error handling shines in actions

---

## 2. Integration Patterns

### 2.1 Confect Integration Patterns

#### Installation

```bash
npm install @rjdellecese/confect
# or
yarn add @rjdellecese/confect
# or
pnpm add @rjdellecese/confect
```

#### Basic Setup

**Step 1: Define Schema with Effect Schema**

```typescript
// convex/schema.ts
import { defineSchema, defineTable } from "@rjdellecese/confect";
import { Schema as S } from "@effect/schema/Schema";

export default defineSchema({
  notes: defineTable({
    text: S.String,
    createdAt: S.Date,
    tags: S.Array(S.String),
  }),

  users: defineTable({
    name: S.String,
    email: S.String.pipe(S.pattern(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)),
    age: S.optional(S.Number.pipe(S.int(), S.greaterThanOrEqualTo(0))),
  }),
});
```

**Step 2: Generate Function Constructors**

```typescript
// convex/functions.ts
import { makeFunctions } from "@rjdellecese/confect";
import schema from "./schema";

export const { query, mutation, action } = makeFunctions(schema);
```

**Step 3: Write Effect-based Functions**

```typescript
// convex/notes.ts
import { Effect } from "effect";
import { query, mutation, ConfectQueryCtx, ConfectMutationCtx } from "./functions";
import { Schema as S } from "@effect/schema/Schema";

// Define input/output schemas
const ListNotesArgs = S.Struct({});
const ListNotesResult = S.Array(S.Struct({
  _id: S.String,
  text: S.String,
  createdAt: S.Date,
  tags: S.Array(S.String),
}));

// Query using Effect.gen
export const listNotes = query({
  args: ListNotesArgs,
  returns: ListNotesResult,
  handler: () =>
    Effect.gen(function* () {
      const { db } = yield* ConfectQueryCtx;
      return yield* db.query("notes").order("desc").collect();
    }),
});

// Mutation using Effect.gen
const InsertNoteArgs = S.Struct({ text: S.String });
const InsertNoteResult = S.String; // Document ID

export const insertNote = mutation({
  args: InsertNoteArgs,
  returns: InsertNoteResult,
  handler: ({ text }) =>
    Effect.gen(function* () {
      const { db } = yield* ConfectMutationCtx;
      return yield* db.insert("notes", {
        text,
        createdAt: new Date(),
        tags: [],
      });
    }),
});
```

#### HTTP API with Effect HTTP

Confect also supports Effect's HTTP API for building HTTP endpoints:

```typescript
// convex/http/api.ts
import { HttpApi, HttpApiEndpoint } from "@effect/platform";
import { Schema as S } from "@effect/schema/Schema";

// Define HTTP endpoint with Effect
export const getNotesApi = HttpApiEndpoint.get("notes", "/api/notes")
  .addSuccess(S.Array(S.Struct({
    id: S.String,
    text: S.String,
  })))
  .addError(S.Struct({ error: S.String }));
```

### 2.2 @maple/convex-effect Integration Patterns

#### Installation

```bash
npx jsr add @maple/convex-effect
# or for Deno
deno add @maple/convex-effect
```

#### Basic Setup

```typescript
// convex/api.ts
import { createServerApi } from "@maple/convex-effect";

export const { query, mutation, action } = createServerApi();
```

#### Query Example

```typescript
import { Effect } from "effect";
import { query } from "./api";

export const getTodos = query({
  handler: (ctx) =>
    Effect.gen(function* () {
      const todos = yield* ctx.db.query("todos").collect();
      return todos;
    }),
});
```

#### Mutation with Error Handling

```typescript
import { Effect, Option } from "effect";
import { mutation } from "./api";

export const completeTodo = mutation({
  args: { id: v.id("todos") },
  handler: ({ id }, ctx) =>
    Effect.gen(function* () {
      const todo = yield* ctx.db.get(id);

      // In @maple/convex-effect, null returns raise errors
      // This creates a clean happy path

      yield* ctx.db.patch(id, { completed: true });
      return todo;
    }).pipe(
      Effect.catchAll((error) => {
        // Handle error cases
        if (error instanceof DocNotFound) {
          return Effect.fail({ error: "Todo not found" });
        }
        return Effect.dieMessage("Unexpected error");
      })
    ),
});
```

#### Action with External API Call

```typescript
import { Effect } from "effect";
import { action } from "./api";

export const fetchWeather = action({
  args: { city: v.string() },
  handler: ({ city }, ctx) =>
    Effect.gen(function* () {
      // Effect works perfectly in actions for external calls
      const response = yield* Effect.tryPromise({
        try: () => fetch(`https://api.weather.com/v1/${city}`),
        catch: (error) => new FetchError({ error }),
      });

      const data = yield* Effect.tryPromise({
        try: () => response.json(),
        catch: (error) => new ParseError({ error }),
      });

      // Store result in database via mutation
      yield* ctx.runMutation(api.weather.store, { city, data });

      return data;
    }),
});
```

### 2.3 Pattern Comparison

| Aspect | Confect | @maple/convex-effect |
|--------|---------|---------------------|
| **Integration Depth** | Deep - includes schema, validation, HTTP | Lightweight - API wrapper only |
| **Schema Support** | Effect Schema for database & validation | Standard Convex validators |
| **Type Safety** | End-to-end with Effect Schema | Standard TypeScript |
| **Learning Curve** | Steeper - requires Effect Schema knowledge | Gentler - familiar Convex APIs |
| **Bundle Size** | Larger - full framework | Smaller - thin wrapper |
| **Use Case** | New projects, schema-first design | Existing projects, gradual adoption |

---

## 3. Examples and Resources

### 3.1 Official Examples

#### Confect Example Project

The Confect repository includes a comprehensive example:

- **Repository**: [github.com/rjdellecese/confect/tree/main/example](https://github.com/rjdellecese/confect/tree/main/example)
- **Key Files**:
  - `example/convex/functions.ts` - Query, mutation, and action examples
  - `example/convex/http/api.ts` - HTTP API implementation
  - `example/convex/schema.ts` - Effect Schema database definitions

**Example Code Snippets from Repository**:

```typescript
// Query with Effect.gen
export const listNotes = query({
  args: ListNotesArgs,
  returns: ListNotesResult,
  handler: () =>
    Effect.gen(function* () {
      const { db } = yield* ConfectQueryCtx;
      return yield* db.query("notes").order("desc").collect();
    }),
});

// Action with random number (non-deterministic)
export const getRandom = action({
  args: GetRandomArgs,
  returns: GetRandomResult,
  handler: () => Effect.succeed(Math.random()),
});
```

### 3.2 Documentation

#### Confect Documentation

- **Getting Started Guide**: [rjdellecese.gitbook.io/confect/getting-started](https://rjdellecese.gitbook.io/confect/getting-started)
- **GitHub**: [github.com/rjdellecese/confect](https://github.com/rjdellecese/confect)
- **Package**: `@rjdellecese/confect` on npm

#### @maple/convex-effect Documentation

- **JSR Package**: [jsr.io/@maple/convex-effect](https://jsr.io/@maple/convex-effect)
- **API Reference**: [jsr.io/@maple/convex-effect/doc](https://jsr.io/@maple/convex-effect/doc)

### 3.3 Community Resources

#### Discord Communities

- **Effect Discord**: [discord.gg/effect-ts](https://discord.gg/effect-ts)
  - Community discussions about Effect.ts usage patterns
  - Help with Effect integration questions

- **Convex Discord**: [convex.dev/community](https://www.convex.dev/community)
  - Convex-specific questions and feature requests
  - Integration discussions with external libraries

#### Related Content

- **JS Party Podcast #297**: "Use Effect (not useEffect)" with Johannes Schickling
  - Discusses Effect.ts philosophy and use cases
  - [changelog.com/jsparty/297](https://changelog.com/jsparty/297)

- **Official Announcement**: Convex team acknowledged Confect on Twitter/X
  - [x.com/convex_dev/status/1800558928251744377](https://x.com/convex_dev/status/1800558928251744377)

### 3.4 GitHub Projects

#### Awesome Effect

The [m9tdev/awesome-effect](https://github.com/m9tdev/awesome-effect) repository lists Confect as a notable Effect integration:

> "A framework that deeply integrates Effect with Convex"

#### Effect Discord Bot

The Effect community maintains a Discord bot built with Effect, demonstrating production Effect usage:
- [github.com/Effect-TS/discord-bot](https://github.com/Effect-TS/discord-bot)

---

## 4. Best Practices and Recommendations

### 4.1 When to Use Effect with Convex

#### Recommended Scenarios

✅ **Use Effect + Convex when:**

1. **Already using Effect.ts**: Your frontend or other services use Effect
2. **Complex error handling needs**: Require sophisticated error types and recovery
3. **Schema-first design**: Want Effect Schema for validation across stack
4. **Functional programming team**: Team experienced with FP patterns
5. **Complex effect composition**: Actions that orchestrate multiple external services
6. **Type safety priority**: Need maximum compile-time safety guarantees

#### Not Recommended Scenarios

❌ **Avoid Effect + Convex when:**

1. **Team unfamiliar with FP**: Steep learning curve can slow development
2. **Simple CRUD application**: Standard Convex patterns are sufficient
3. **Performance-critical queries**: Additional abstraction layer adds minimal overhead
4. **Rapid prototyping**: Simpler patterns allow faster iteration
5. **Existing Convex codebase**: Migration effort may not justify benefits

### 4.2 Architecture Patterns

#### Pattern 1: Actions for Effect, Standard for Queries/Mutations

**Recommended for gradual adoption:**

```typescript
// Standard Convex query (simpler, faster)
export const listTodos = query({
  handler: async (ctx) => {
    return await ctx.db.query("todos").collect();
  },
});

// Effect-based action (complex orchestration)
export const syncWithThirdParty = action({
  handler: (ctx) =>
    Effect.gen(function* () {
      // Complex effect composition with error handling
      const externalData = yield* fetchExternalAPI();
      const processed = yield* processData(externalData);
      yield* ctx.runMutation(api.todos.updateBatch, { data: processed });
      return processed;
    }),
});
```

**Benefits**:
- Queries/mutations remain simple and performant
- Actions leverage Effect's power for complex operations
- Easier for team members unfamiliar with Effect

#### Pattern 2: Full Confect Integration

**Recommended for new projects:**

```typescript
// Define schema with Effect Schema
export default defineSchema({
  todos: defineTable({
    text: S.String,
    completed: S.Boolean,
    priority: S.Number.pipe(S.int(), S.between(1, 5)),
    tags: S.Array(S.String),
    dueDate: S.optional(S.Date),
  }),
});

// All functions use Effect.gen
export const createTodo = mutation({
  args: CreateTodoArgs,
  returns: CreateTodoResult,
  handler: (args) =>
    Effect.gen(function* () {
      const { db } = yield* ConfectMutationCtx;

      // Automatic schema validation from Effect Schema
      const todoId = yield* db.insert("todos", {
        ...args,
        completed: false,
      });

      return todoId;
    }),
});
```

**Benefits**:
- Consistent Effect patterns across entire backend
- Single source of truth for schemas (Effect Schema)
- Maximum type safety and validation

### 4.3 Error Handling Best Practices

#### Leverage Effect's Tagged Errors

```typescript
import { Data } from "effect";

// Define domain-specific errors
class TodoNotFound extends Data.TaggedError("TodoNotFound")<{
  todoId: string;
}> {}

class InvalidPriority extends Data.TaggedError("InvalidPriority")<{
  value: number;
}> {}

export const updateTodo = mutation({
  args: UpdateTodoArgs,
  handler: ({ id, priority }, ctx) =>
    Effect.gen(function* () {
      // Validate priority
      if (priority < 1 || priority > 5) {
        yield* new InvalidPriority({ value: priority });
      }

      const todo = yield* ctx.db.get(id).pipe(
        Effect.mapError(() => new TodoNotFound({ todoId: id }))
      );

      yield* ctx.db.patch(id, { priority });
      return todo;
    }).pipe(
      // Handle expected errors
      Effect.catchTag("InvalidPriority", (error) =>
        Effect.fail({ error: `Priority must be 1-5, got ${error.value}` })
      ),
      Effect.catchTag("TodoNotFound", (error) =>
        Effect.fail({ error: `Todo ${error.todoId} not found` })
      ),
    ),
});
```

#### Null Handling Pattern

In `@maple/convex-effect`, functions that return `null` in standard Convex raise errors instead:

```typescript
// Standard Convex - returns null
const todo = await ctx.db.get(id);
if (todo === null) {
  throw new Error("Not found");
}

// @maple/convex-effect - raises error automatically
const todo = yield* ctx.db.get(id);
// If not found, error is raised - clean happy path!
```

### 4.4 Performance Considerations

#### Minimal Overhead

Both integration libraries are **lightweight wrappers** that don't significantly impact performance:

- **Confect**: Adds schema validation overhead (similar to standard validators)
- **@maple/convex-effect**: Minimal wrapper around Convex APIs

#### Query Optimization

Effect's composition doesn't prevent Convex's query optimization:

```typescript
// This still benefits from Convex's query indexing
export const getMessagesByChannel = query({
  args: { channel: v.string() },
  handler: ({ channel }) =>
    Effect.gen(function* () {
      const { db } = yield* ConfectQueryCtx;

      // Index is used efficiently
      return yield* db
        .query("messages")
        .withIndex("by_channel", (q) => q.eq("channel", channel))
        .order("desc")
        .take(100);
    }),
});
```

#### Caching Behavior

Convex's automatic query caching and reactivity work **transparently** with Effect-based functions:

- Query results are cached by Convex infrastructure
- Subscriptions trigger re-execution on dependency changes
- Effect composition doesn't interfere with Convex's reactivity

### 4.5 Working Within Convex's Execution Model

#### Respecting Determinism

**Queries and Mutations Must Be Deterministic:**

```typescript
// ❌ WRONG - Non-deterministic in query
export const badQuery = query({
  handler: () =>
    Effect.gen(function* () {
      // Math.random() is deterministic in Convex (seeded)
      // but external fetch is NOT allowed
      const data = yield* Effect.tryPromise(() =>
        fetch("https://api.example.com")
      );
      return data;
    }),
});

// ✅ CORRECT - Use action for external calls
export const goodAction = action({
  handler: () =>
    Effect.gen(function* () {
      const data = yield* Effect.tryPromise(() =>
        fetch("https://api.example.com")
      );
      return data;
    }),
});
```

#### Convex Handles Determinism Automatically

Convex automatically makes certain operations deterministic:

- `Math.random()` uses seeded random generation
- `Date.now()` returns frozen timestamp during execution

**Effect code benefits from this automatically** - no special handling needed.

#### Transaction Boundaries

Multiple `ctx.runQuery` / `ctx.runMutation` calls in actions execute in **separate transactions**:

```typescript
// ❌ WRONG - No consistency between calls
export const inconsistentAction = action({
  handler: (ctx) =>
    Effect.gen(function* () {
      const count1 = yield* Effect.promise(() =>
        ctx.runQuery(api.todos.count)
      );
      const count2 = yield* Effect.promise(() =>
        ctx.runQuery(api.todos.count)
      );
      // count1 and count2 might differ!
      return { count1, count2 };
    }),
});

// ✅ CORRECT - Single transaction for consistency
export const consistentQuery = query({
  handler: () =>
    Effect.gen(function* () {
      const { db } = yield* ConfectQueryCtx;
      const todos = yield* db.query("todos").collect();
      return { count: todos.length };
    }),
});
```

### 4.6 Migration Strategy

#### Gradual Adoption Approach

For existing Convex projects, adopt Effect gradually:

**Phase 1: Actions Only**
```typescript
// Keep existing queries/mutations
export const listTodos = query({
  handler: async (ctx) => {
    return await ctx.db.query("todos").collect();
  },
});

// Add Effect for new actions
export const syncTodos = action({
  handler: (ctx) => Effect.gen(function* () {
    // New Effect-based code
  }),
});
```

**Phase 2: New Functions**
```typescript
// New functions use Effect
export const createTodoV2 = mutation({
  handler: (args) => Effect.gen(function* () {
    // Effect-based implementation
  }),
});
```

**Phase 3: Migrate Critical Paths**
```typescript
// Rewrite existing functions as needed
export const listTodosV2 = query({
  handler: () => Effect.gen(function* () {
    // Migrated to Effect
  }),
});
```

#### Library Selection Decision Tree

```
Are you starting a new project?
├─ Yes → Consider Confect for full integration
│  └─ Does your team know Effect Schema?
│     ├─ Yes → Use Confect
│     └─ No → Use @maple/convex-effect (gentler learning curve)
│
└─ No (existing project) → Start with @maple/convex-effect
   └─ Add Effect to actions first
      └─ Gradually expand to queries/mutations if beneficial
```

### 4.7 TypeScript Configuration

#### Required Settings

```json
{
  "compilerOptions": {
    // Required for Confect due to convex-js limitation
    "exactOptionalPropertyTypes": false,

    // Recommended Effect settings
    "strict": true,
    "strictNullChecks": true,
    "noUncheckedIndexedAccess": true,

    // Convex requirements
    "module": "ESNext",
    "target": "ES2021",
    "lib": ["ES2021"],
    "moduleResolution": "bundler"
  }
}
```

---

## 5. Integration Decision Matrix

### 5.1 Quick Decision Guide

| Factor | Use Standard Convex | Use @maple/convex-effect | Use Confect |
|--------|-------------------|------------------------|-------------|
| **Team FP Experience** | Low | Medium | High |
| **Project Complexity** | Simple CRUD | Moderate | Complex |
| **Schema Requirements** | Basic | Basic | Advanced validation |
| **Error Handling Needs** | Simple | Moderate | Sophisticated |
| **Learning Time** | Days | 1-2 weeks | 2-4 weeks |
| **Type Safety Level** | Good | Great | Excellent |
| **Bundle Size Impact** | Minimal | Small | Moderate |
| **Existing Codebase** | Any | Convex | New project preferred |

### 5.2 Use Case Recommendations

#### E-commerce Application

**Recommendation**: Confect

**Reasoning**:
- Complex validation needs (product schemas, pricing rules)
- Sophisticated error handling (payment processing, inventory)
- Effect Schema provides end-to-end type safety for product catalog
- Actions for external payment/shipping API integration

#### Real-time Chat Application

**Recommendation**: Standard Convex or @maple/convex-effect

**Reasoning**:
- Simple message schema (text, user, timestamp)
- Performance-critical query paths (message loading)
- Standard Convex patterns are sufficient
- If using Effect elsewhere, @maple/convex-effect for consistency

#### Multi-tenant SaaS Platform

**Recommendation**: Confect

**Reasoning**:
- Complex tenant isolation requirements
- Sophisticated permission/authorization logic
- Effect's error handling for multi-stage business processes
- Schema validation for tenant-specific configurations

#### Analytics Dashboard

**Recommendation**: Standard Convex

**Reasoning**:
- Simple aggregation queries
- Performance-critical data loading
- Minimal business logic complexity
- Effect overhead not justified

---

## 6. Gotchas and Considerations

### 6.1 Common Pitfalls

#### Pitfall 1: Forgetting Determinism Constraints

```typescript
// ❌ WRONG - External fetch in mutation
export const badMutation = mutation({
  handler: () =>
    Effect.gen(function* () {
      const data = yield* Effect.tryPromise(() =>
        fetch("https://api.example.com")
      );
      // This will fail! Mutations can't make external calls
    }),
});
```

**Solution**: Use actions for non-deterministic operations.

#### Pitfall 2: Mixing Promise and Effect Patterns

```typescript
// ❌ WRONG - Inconsistent patterns
export const mixedPatterns = action({
  handler: async (ctx) => {
    const result1 = await ctx.runQuery(api.todos.list);

    const result2 = yield* Effect.succeed(42);
    // Can't mix async/await with Effect.gen!
  },
});
```

**Solution**: Choose Effect.gen OR async/await, not both in same function.

#### Pitfall 3: Not Handling Effect Failures

```typescript
// ❌ WRONG - Unhandled failures
export const unhandledFailure = mutation({
  handler: () =>
    Effect.gen(function* () {
      const result = yield* Effect.fail(new Error("Oops"));
      // Error propagates - might crash unexpectedly
    }),
});

// ✅ CORRECT - Handle failures
export const handledFailure = mutation({
  handler: () =>
    Effect.gen(function* () {
      const result = yield* Effect.fail(new Error("Oops")).pipe(
        Effect.catchAll((error) =>
          Effect.succeed({ error: error.message })
        )
      );
      return result;
    }),
});
```

#### Pitfall 4: Schema Validation Overhead in Hot Paths

```typescript
// 🐌 SLOW - Complex validation on every query
const ComplexSchema = S.Struct({
  // 50+ fields with complex validation rules
});

export const hotQuery = query({
  returns: ComplexSchema,
  handler: () => Effect.gen(function* () {
    // Returns validate against ComplexSchema on every call
  }),
});
```

**Solution**: Use simpler return schemas for hot paths, validate only inputs.

### 6.2 Convex-Specific Considerations

#### Runtime Limits Apply to Effect Code

- **Query/Mutation timeout**: Still applies to Effect composition
- **Memory limits**: Effect programs must fit within Convex limits (64MB default, 512MB Node.js)
- **Concurrent operations**: Maximum 1,000 concurrent operations

#### Convex's Automatic Retries

Queries and mutations are automatically retried by Convex on conflict. **Effect code will be re-executed** during retries:

```typescript
// This Effect will run multiple times if there's a conflict
export const incrementCounter = mutation({
  handler: () =>
    Effect.gen(function* () {
      const { db } = yield* ConfectMutationCtx;
      const counter = yield* db.query("counters").first();

      // If another mutation modifies counter concurrently,
      // this entire Effect will re-execute
      yield* db.patch(counter._id, {
        count: counter.count + 1,
      });
    }),
});
```

**Implication**: Ensure Effect programs in queries/mutations are idempotent.

#### Client-Side Convex Hooks Don't Change

React hooks work the same with Effect-based backend:

```typescript
// Frontend code unchanged
const todos = useQuery(api.todos.list);
const createTodo = useMutation(api.todos.create);

// Effect is purely backend concern
```

### 6.3 Effect-Specific Considerations

#### Layer System Integration

Convex context doesn't integrate with Effect's Layer system out of the box. Workaround:

```typescript
// Define service that wraps Convex context
class ConvexDBService extends Context.Tag("ConvexDBService")<
  ConvexDBService,
  { db: DatabaseReader }
>() {}

export const listTodos = query({
  handler: (ctx) =>
    Effect.gen(function* () {
      const { db } = yield* ConvexDBService;
      return yield* db.query("todos").collect();
    }).pipe(
      Effect.provideService(ConvexDBService, { db: ctx.db })
    ),
});
```

#### Effect.Service Pattern

For complex applications, wrap Convex operations in Effect services:

```typescript
// Define service interface
class TodoService extends Context.Tag("TodoService")<
  TodoService,
  {
    readonly list: Effect.Effect<Todo[], never>;
    readonly create: (text: string) => Effect.Effect<string, CreateError>;
  }
>() {}

// Implement service using Convex
const makeTodoService = (db: DatabaseReader): TodoService => ({
  list: Effect.tryPromise({
    try: () => db.query("todos").collect(),
    catch: () => new QueryError(),
  }),
  create: (text) => Effect.tryPromise({
    try: () => db.insert("todos", { text, completed: false }),
    catch: () => new CreateError(),
  }),
});

// Use in functions
export const listTodos = query({
  handler: (ctx) =>
    Effect.gen(function* () {
      const todoService = yield* TodoService;
      return yield* todoService.list;
    }).pipe(
      Effect.provideService(TodoService, makeTodoService(ctx.db))
    ),
});
```

---

## 7. Conclusion

### 7.1 Summary

Effect.ts integration with Convex is **viable, production-ready, and recommended** for teams that:

1. Already use Effect.ts in their stack
2. Require sophisticated error handling and effect composition
3. Value functional programming paradigms and type safety
4. Have team members experienced with FP concepts

The integration works seamlessly through **Confect** (deep integration with Effect Schema) or **@maple/convex-effect** (lightweight wrapper), both of which respect Convex's deterministic constraints while enabling Effect's powerful abstractions.

### 7.2 Key Takeaways

✅ **Pros of Effect + Convex:**
- Unified functional programming approach across stack
- Superior error handling with tagged errors
- Effect Schema provides end-to-end validation
- Perfect for complex effect composition in actions
- Convex's reactivity and caching work transparently

⚠️ **Cons and Limitations:**
- Steeper learning curve for teams unfamiliar with FP
- `exactOptionalPropertyTypes: false` TypeScript requirement
- Schema restrictions (not all Effect Schemas supported)
- Additional abstraction layer (minimal performance impact)
- Not ideal for simple CRUD applications

### 7.3 Final Recommendations

**Start with @maple/convex-effect if:**
- Existing Convex project wanting to adopt Effect gradually
- Team learning Effect for the first time
- Need quick wins with minimal disruption

**Choose Confect if:**
- Starting a new project
- Want Effect Schema for database and API validation
- Team comfortable with Effect ecosystem
- Building complex application with sophisticated schemas

**Stick with standard Convex if:**
- Simple application with basic CRUD operations
- Team unfamiliar with functional programming
- Performance-critical queries with minimal business logic
- Rapid prototyping phase

### 7.4 Additional Resources

- **Confect Documentation**: [rjdellecese.gitbook.io/confect](https://rjdellecese.gitbook.io/confect)
- **Effect Documentation**: [effect.website/docs](https://effect.website/docs)
- **Convex Documentation**: [docs.convex.dev](https://docs.convex.dev)
- **Effect Discord**: [discord.gg/effect-ts](https://discord.gg/effect-ts)
- **Convex Discord**: [convex.dev/community](https://www.convex.dev/community)

---

**Research Date**: 2025-11-20
**Libraries Reviewed**:
- Confect v0.0.32 (@rjdellecese/confect)
- @maple/convex-effect (JSR package)
**Convex Version**: Latest (2025)
**Effect Version**: Latest (2025)
