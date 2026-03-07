# Comprehensive ORPC (oRPC) Research Report

## Executive Summary

**oRPC** is a **contract-first, type-safe RPC framework** designed to enable end-to-end type safety across client and server applications. It emphasizes **developer experience**, **OpenAPI generation**, and **code reuse** in monorepo architectures.

### Key Value Propositions:
- **100% Type Safety**: Shared contracts between server and clients
- **Contract-First Design**: Define API shape once, generate clients automatically
- **Automatic OpenAPI**: Generate OpenAPI specs and documentation automatically
- **Framework Agnostic**: Works with any server framework (Hono, Express, Node.js HTTP)
- **Real-time Support**: Built-in support for streaming and event-based communication
- **Middleware System**: Powerful middleware composition for cross-cutting concerns
- **Error Handling**: Type-safe error definitions and handling

---

## 1. CORE CONCEPTS & ARCHITECTURE

### 1.1 What is oRPC?

oRPC is a **Remote Procedure Call framework** that bridges the gap between TypeScript server and client applications. Unlike traditional RPC systems, oRPC uses a **contract-first approach** where:

1. You define an **API contract** once (with input/output schemas)
2. The **server implements** the contract
3. **Clients consume** the contract with full type inference
4. Type safety is enforced at **compile time**

### 1.2 Problem It Solves

**Traditional Approach Problems:**
- Manual type synchronization between client and server
- Runtime type mismatches cause bugs
- API documentation goes out of sync with code
- Boilerplate for creating clients and serialization
- No IDE autocomplete for API calls

**oRPC Solution:**
```typescript
// Single source of truth for the entire API
export const contract = {
  greeting: oc.input(InputSchema).output(OutputSchema),
  createTodo: oc.input(TodoInputSchema).output(TodoSchema),
};

// Server implements the contract
export const greeting = procedure.greeting.handler(({ input }) => {
  return { text: `Hello, ${input.name}` };
});

// Client gets full type safety automatically
const response = await client.greeting({ name: 'John' }); // ✓ Fully typed!
```

### 1.3 Core Architectural Patterns

#### Contract-First Architecture
```
┌─────────────────────────────────────┐
│      Contract Layer                 │
│  (packages/contracts)               │
│  - Zod schemas                      │
│  - OpenAPI metadata                 │
│  - Shared types                     │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        │             │
┌───────▼──────┐  ┌──▼─────────────┐
│   Server     │  │   Clients      │
│ Implementation│  │  (Web, Native) │
│  - Handlers  │  │  - Consumers   │
│  - Middleware│  │  - Type Safety │
└──────────────┘  └────────────────┘
```

#### Request/Response Flow
```
Client                 Server
  │                      │
  ├─→ RPCLink ──→ RPCHandler
  │   (JSON)      (deserialize)
  │               │
  │               ├─→ Parse Contract
  │               ├─→ Execute Handler
  │               ├─→ Apply Middleware
  │               └─→ Return Response
  │                     │
  └←─ (JSON) ─────────┤
     (typed!)
```

### 1.4 How It Differs From Other RPC Frameworks

| Feature | oRPC | tRPC | gRPC |
|---------|------|------|------|
| **Language** | TypeScript-first | TypeScript-first | Protocol Buffers |
| **Contract Model** | Contract-first | Procedure-first | Schema-first |
| **Type Safety** | 100% (TS types) | 100% (TS types) | Proto types |
| **OpenAPI** | Built-in | Via plugins | Requires conversion |
| **Streaming** | Yes (EventIterator) | Limited | Yes (Protocol) |
| **Server Framework** | Any (Hono, Node.js) | Node.js only | Language-agnostic |
| **Real-time** | Pub/Sub support | Subscriptions | gRPC streaming |
| **Learning Curve** | Moderate | Easy | Steep |

---

## 2. KEY FEATURES

### 2.1 Type Safety Mechanisms

#### Contract Definition with Zod
```typescript
import { oc } from '@orpc/contract';
import { z } from 'zod';

// Define input/output schemas
const UserInputSchema = z.object({
  email: z.string().email(),
  name: z.string().min(1),
});

const UserOutputSchema = z.object({
  id: z.string().uuid(),
  email: z.string(),
  name: z.string(),
  createdAt: z.date(),
});

// Create procedure with type safety
export const signup = oc
  .input(UserInputSchema)
  .output(UserOutputSchema);
```

#### Type Inference at Client
```typescript
import { ContractRouterClient, InferContractRouterInputs, InferContractRouterOutputs } from '@orpc/contract';

type Inputs = InferContractRouterInputs<typeof contract>;  // ✓ Inferred!
type Outputs = InferContractRouterOutputs<typeof contract>; // ✓ Inferred!

// Client call is fully typed
const response = await client.signup({
  email: 'user@example.com', // ✓ Type checked
  name: 'John',              // ✓ Type checked
});
// response: UserOutputSchema (fully typed)
```

### 2.2 Router and Procedure Definitions

#### Basic Procedure
```typescript
import { os } from '@orpc/server';

// Simple procedure
export const listTodos = os
  .input(z.object({}))
  .handler(() => {
    return todos;
  });

// Procedure with input
export const addTodo = os
  .input(z.object({ name: z.string() }))
  .handler(({ input }) => {
    const newTodo = { id: Date.now(), name: input.name };
    todos.push(newTodo);
    return newTodo;
  });
```

#### Router Definition
```typescript
// Compose procedures into routers
export const appRouter = publicProcedure.router({
  public: {
    greeting: publicGreeting,
    list: listTodos,
  },
  private: {
    greeting: privateGreeting,
  },
});

// Router is nested and hierarchical
export type AppRouter = typeof appRouter;
```

#### Multiservice Router Aggregation
```typescript
// File: apps/api/src/main.ts
const authServiceRPCHandler = new RPCHandler(authRouter, { plugins: [...] });
const planetServiceRPCHandler = new RPCHandler(planetRouter, { plugins: [...] });

// Route based on path prefix
const authResult = await authServiceRPCHandler.handle(req, res, {
  prefix: '/rpc/auth',
  context: { authToken },
});

const planetResult = await planetServiceRPCHandler.handle(req, res, {
  prefix: '/rpc/planet',
  context: { getAuth, db },
});
```

### 2.3 Client/Server Patterns

#### Server-Side Implementation
```typescript
import { implement } from '@orpc/server';

export const os = implement(appContract);
export const context = os.$context<Context>();
export const publicProcedure = context;

// Handler receives input, context
export const signup = publicProcedure.auth.signup
  .handler(async ({ input, context }) => {
    const user = await db.users.create(input);
    return user;
  });
```

#### Client-Side Consumption
```typescript
import { createORPCClient } from '@orpc/client';
import { RPCLink } from '@orpc/client/fetch';
import { createTanstackQueryUtils } from '@orpc/tanstack-query';

// Create client with link
const link = new RPCLink({ url: '/api/rpc' });
const client = createORPCClient(link);

// Integrate with TanStack Query
export const orpc = createTanstackQueryUtils(client);

// Use in components
const { data } = useQuery(
  orpc.public.greeting.queryOptions({ input: { name: 'John' } })
);
```

#### Isomorphic Patterns (SSR Support)
```typescript
import { createIsomorphicFn } from '@tanstack/react-start';

const getORPCClient = createIsomorphicFn()
  .server(() => 
    createRouterClient(router, {
      context: () => ({
        headers: getRequestHeaders(),
      }),
    })
  )
  .client(() => {
    const link = new RPCLink({ url: '/api/rpc' });
    return createORPCClient(link);
  });

// Same client works on server and client
export const client = getORPCClient();
```

### 2.4 Middleware System

#### Basic Middleware Pattern
```typescript
import { os } from '@orpc/server';

// Define context for middleware
const publicProcedure = os.$context<{ user?: User }>();

// Create middleware
const authMiddleware = publicProcedure.middleware(async ({ context, next }) => {
  if (!context.user) {
    throw new ORPCError('UNAUTHORIZED', { message: 'Auth required' });
  }
  return next({
    context: {
      user: context.user, // Type-narrowed for downstream
    },
  });
});

// Apply middleware
export const protectedProcedure = publicProcedure.use(authMiddleware);
```

#### Middleware Composition
```typescript
// Pipe multiple middleware
export const publicProcedure = os
  .use(loggingMiddleware)
  .use(corsMiddleware)
  .use(rateLimitMiddleware);

// Apply additional middleware per handler
export const listPlanets = publicProcedure
  .use(retry({ times: 3 }))
  .planet.list
  .handler(async ({ input, context }) => {
    return context.db.planets.list(input.limit);
  });
```

#### Retry Middleware Example
```typescript
export function retry(options: { times: number }) {
  return os
    .$context<{ canRetry?: boolean }>()
    .middleware(({ context, next }) => {
      const canRetry = context.canRetry ?? true;
      
      if (!canRetry) return next();
      
      let times = 0;
      while (true) {
        try {
          return next({
            context: { canRetry: false },
          });
        } catch (e) {
          if (times >= options.times) throw e;
          times++;
        }
      }
    });
}
```

#### Context Middleware Pattern
```typescript
// Provide database to all handlers
const dbProviderMiddleware = os
  .$context<{ db?: DB }>()
  .middleware(async ({ context, next }) => {
    const db = context.db ?? createFakeDB();
    return next({
      context: { db },
    });
  });

export const publicProcedure = os.use(dbProviderMiddleware);
```

### 2.5 Error Handling

#### Type-Safe Errors
```typescript
// Define errors in contract
export const updatePlanet = oc
  .route({
    method: 'PUT',
    path: '/planets/{id}',
  })
  .errors({
    NOT_FOUND: {
      message: 'Planet not found',
      data: z.object({ id: z.number() }),
    },
    UNAUTHORIZED: {
      message: 'User not authorized to update',
    },
  })
  .input(UpdatePlanetSchema)
  .output(PlanetSchema);

// Throw with type safety
export const updatePlanet = authed.planet.update
  .handler(async ({ input, context, errors }) => {
    const planet = await context.db.planets.find(input.id);
    
    if (!planet) {
      throw errors.NOT_FOUND({ data: { id: input.id } });
    }
    
    return context.db.planets.update(input);
  });
```

#### ORPCError Standard Pattern
```typescript
import { ORPCError } from '@orpc/server';

// Throw structured errors
const authMiddleware = publicProcedure.middleware(async ({ context, next }) => {
  if (!context.session?.user) {
    throw new ORPCError('UNAUTHORIZED', {
      message: 'Authentication required',
    });
  }
  
  return next({
    context: {
      user: context.session.user,
    },
  });
});
```

#### Error Codes
Common error codes used in oRPC:
- `UNAUTHORIZED` - Authentication/authorization failed
- `NOT_FOUND` - Resource not found
- `BAD_REQUEST` - Invalid input
- `INTERNAL_SERVER_ERROR` - Server error

### 2.6 Request/Response Flow

#### Complete Request Lifecycle
```
1. Client Request
   └─→ createORPCClient + RPCLink serializes call
   └─→ HTTP POST to /api/rpc with JSON body

2. Server Receives
   └─→ RPCHandler.handle() parses path
   └─→ Extracts procedure path: 'public.greeting'

3. Contract Matching
   └─→ Looks up contract[public.greeting]
   └─→ Validates input schema with Zod

4. Middleware Pipeline
   └─→ auth → logging → rateLimit (left to right)
   └─→ Each middleware can modify context
   └─→ One middleware throws → pipeline stops

5. Handler Execution
   └─→ Call handler({ input, context })
   └─→ Handler logic executes

6. Response
   └─→ Validate output with Zod schema
   └─→ Serialize to JSON
   └─→ Return to client

7. Client Receives
   └─→ Deserialize JSON
   └─→ Type inference works (data is typed!)
   └─→ TanStack Query caches result
```

---

## 3. ONTOLOGIES & DATA STRUCTURES

### 3.1 Schema Definitions

#### Using Zod for Schemas
```typescript
import { z } from 'zod';

export const TodoSchema = z.object({
  id: z.number().int().min(1),
  name: z.string().min(1).max(255),
  completed: z.boolean().default(false),
  createdAt: z.date(),
});

export const TodoInputSchema = TodoSchema.omit({ id: true, createdAt: true });

export const PlanetSchema = z.object({
  id: z.number().int(),
  name: z.string().min(1).max(100),
  description: z.string().optional(),
  imageUrl: z.string().url().optional(),
  creatorId: z.string().uuid(),
});

export const NewPlanetSchema = PlanetSchema.omit({ id: true, creatorId: true });

export const UpdatePlanetSchema = NewPlanetSchema.partial().extend({
  id: z.number().int().min(1),
  image: z.instanceof(File).optional(),
});
```

### 3.2 Type Inference Patterns

#### Input/Output Inference
```typescript
import { InferContractRouterInputs, InferContractRouterOutputs } from '@orpc/contract';

export type AppInputs = InferContractRouterInputs<typeof appContract>;
type Inputs = AppInputs['public']['greeting']; // { name?: string }

export type AppOutputs = InferContractRouterOutputs<typeof appContract>;
type Output = AppOutputs['public']['greeting']; // { text: string }
```

#### Type Narrowing with Context
```typescript
// Public procedure has optional user
const publicProcedure = os.$context<{ user?: User }>();

// After auth middleware, user is guaranteed
const authed = publicProcedure.use(async ({ context, next }) => {
  if (!context.user) throw new Error('Unauthorized');
  return next({
    context: {
      user: context.user, // Type narrowed from User | undefined to User
    },
  });
});

// Handler receives narrowed context
export const getProfile = authed.getProfile.handler(async ({ context }) => {
  // context.user is User (not optional!)
  return { userId: context.user.id };
});
```

### 3.3 Input/Output Validation

#### Zod Validation (Automatic)
```typescript
// oRPC automatically validates using Zod schemas
export const createUser = publicProcedure.createUser
  .handler(async ({ input }) => {
    // input is guaranteed to match UserInputSchema
    // Invalid input is rejected before handler runs
    return { id: '1', ...input };
  });

// Client-side, if input doesn't match:
await client.createUser({
  email: 'invalid-email', // ✗ TypeScript error
  name: 'John',
});
```

#### Custom Validation in Handler
```typescript
export const updatePassword = authed.updatePassword
  .handler(async ({ input, context }) => {
    // Additional runtime validation
    if (input.newPassword.length < 8) {
      throw new ORPCError('BAD_REQUEST', {
        message: 'Password must be at least 8 characters',
      });
    }
    
    // Update logic
    return { success: true };
  });
```

### 3.4 Context Handling

#### Context Definition
```typescript
interface Context {
  // Automatically available
  session?: { user: User };
  headers: Headers;
  
  // Database
  db?: DB;
  
  // Auth function
  getAuth?: () => Promise<Auth | null>;
  
  // Real-time
  roomPublisher?: Publisher;
}

export const publicProcedure = os.$context<Context>();
```

#### Context Propagation
```typescript
// Server: Create context from request
const server = new RPCHandler(router);

await server.handle(request, response, {
  prefix: '/api/rpc',
  context: {
    headers: request.headers,
    getAuth: async (token) => {
      const auth = await validateToken(token);
      return auth;
    },
  },
});
```

---

## 4. INTEGRATION PATTERNS

### 4.1 Framework Integration: Hono

```typescript
import { Hono } from 'hono';
import { RPCHandler } from '@orpc/server/node';
import { router } from './routers';

const app = new Hono();
const rpcHandler = new RPCHandler(router);

// Mount RPC handler
app.all('/api/rpc/:path*', async (c) => {
  const { response } = await rpcHandler.handle(c.req.raw, {
    prefix: '/api/rpc',
    context: {
      headers: c.req.headers,
      user: c.get('user'), // From auth middleware
    },
  });
  
  return response || c.text('Not Found', 404);
});

export default app;
```

### 4.2 Framework Integration: Next.js / TanStack Start

```typescript
// File: src/routes/api.rpc.$.ts
import { RPCHandler } from '@orpc/server/fetch';
import { createFileRoute } from '@tanstack/react-router';
import router from '@/orpc/router';

const handler = new RPCHandler(router);

async function handle({ request }: { request: Request }) {
  const { response } = await handler.handle(request, {
    prefix: '/api/rpc',
    context: {},
  });
  return response ?? new Response('Not Found', { status: 404 });
}

export const Route = createFileRoute('/api/rpc/$')({
  server: {
    handlers: {
      GET: handle,
      POST: handle,
      PUT: handle,
      DELETE: handle,
    },
  },
});
```

### 4.3 React/Next.js Integration

#### Setup with TanStack Query
```typescript
import { createORPCClient } from '@orpc/client';
import { RPCLink } from '@orpc/client/fetch';
import { createTanstackQueryUtils } from '@orpc/tanstack-query';
import { QueryClient } from '@tanstack/react-query';

// Create query client
export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      toast.error(`Error: ${error.message}`);
    },
  }),
});

// Create RPC link with credentials
export const link = new RPCLink({
  url: '/api/rpc',
  fetch(url, options) {
    return fetch(url, {
      ...options,
      credentials: 'include', // Include cookies
    });
  },
  headers: async () => {
    // Add auth headers
    const token = localStorage.getItem('authToken');
    return token ? { Authorization: `Bearer ${token}` } : {};
  },
});

// Create client and query utilities
const client = createORPCClient(link);
export const orpc = createTanstackQueryUtils(client);
```

#### Using in Components
```typescript
import { useQuery, useMutation } from '@tanstack/react-query';
import { orpc } from '@/utils/orpc';

export function TodoList() {
  // Query
  const { data: todos } = useQuery(
    orpc.listTodos.queryOptions({ input: {} })
  );
  
  // Mutation
  const { mutate: addTodo } = useMutation(
    orpc.addTodo.mutationOptions({
      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: orpc.listTodos.key(),
        });
      },
    })
  );
  
  return (
    <>
      {todos?.map((todo) => (
        <TodoItem key={todo.id} todo={todo} />
      ))}
      <button onClick={() => addTodo({ name: 'New todo' })}>
        Add Todo
      </button>
    </>
  );
}
```

#### Infinite Queries
```typescript
export function PlanetsList() {
  const { data, fetchNextPage, hasNextPage } = useSuspenseInfiniteQuery(
    orpc.planet.list.infiniteOptions({
      input: (cursor) => ({ cursor, limit: 10 }),
      getNextPageParam: (lastPage) =>
        lastPage.length === 10 ? lastPage.at(-1)?.id : null,
      initialPageParam: 0,
    })
  );
  
  return (
    <>
      {data.pages.flat().map((planet) => (
        <PlanetCard key={planet.id} planet={planet} />
      ))}
      <button onClick={() => fetchNextPage()} disabled={!hasNextPage}>
        Load More
      </button>
    </>
  );
}
```

### 4.4 React Native / Expo Integration

```typescript
import { createORPCClient } from '@orpc/client';
import { RPCLink } from '@orpc/client/fetch';
import { createTanstackQueryUtils } from '@orpc/tanstack-query';
import { authClient } from '@/lib/auth-client';

export const link = new RPCLink({
  url: `${process.env.EXPO_PUBLIC_SERVER_URL}/rpc`,
  headers() {
    const headers = new Map<string, string>();
    const cookies = authClient.getCookie();
    if (cookies) {
      headers.set('Cookie', cookies);
    }
    return Object.fromEntries(headers);
  },
});

const client = createORPCClient(link);
export const orpc = createTanstackQueryUtils(client);
```

### 4.5 Authentication & Authorization

#### Auth Middleware
```typescript
import { ORPCError } from '@orpc/server';
import { auth } from '@repo/auth-service';

const authMiddleware = context.middleware(async ({ context, next }) => {
  const session = await auth.api.getSession({
    headers: context.headers,
  });
  
  if (!session) {
    throw new ORPCError('UNAUTHORIZED', {
      message: 'Authentication required',
    });
  }
  
  return next({
    context: {
      session,
      user: session.user,
    },
  });
});

export const protectedProcedure = publicProcedure.use(authMiddleware);
```

#### Bearer Token Auth
```typescript
export const authed = pub.use(async ({ context, next }) => {
  const authToken = context.authToken;
  const auth = await getAuth(authToken);
  
  if (!auth) {
    throw new ORPCError('UNAUTHORIZED');
  }
  
  return next({
    context: {
      auth,
    },
  });
});
```

### 4.6 Streaming & Real-time Support

#### Event Iterator Pattern
```typescript
import { eventIterator, oc } from '@orpc/contract';

// Contract definition
export const subscribe = oc
  .route({
    method: 'GET',
    path: '/room/subscribe',
  })
  .input(z.object({ room: z.string() }))
  .output(eventIterator(z.object({ message: z.string() })));

// Server implementation
export const subscribe = authed.room.subscribe
  .handler(async ({ input, context, lastEventId, signal }) => {
    // Subscribe to room publisher
    return context.roomPublisher.subscribe(input.room, {
      lastEventId,
      signal, // For cancellation
    });
  });
```

### 4.7 OpenAPI Generation

#### Automatic OpenAPI Documentation
```typescript
import { OpenAPIHandler } from '@orpc/openapi/fetch';
import { OpenAPIReferencePlugin } from '@orpc/openapi/plugins';

const handler = new OpenAPIHandler(router, {
  plugins: [
    new OpenAPIReferencePlugin({
      specGenerateOptions: {
        info: {
          title: 'My API',
          version: '1.0.0',
        },
        security: [{ bearerAuth: [] }],
        components: {
          securitySchemes: {
            bearerAuth: {
              type: 'http',
              scheme: 'bearer',
            },
          },
        },
      },
      docsConfig: {
        authentication: {
          securitySchemes: {
            bearerAuth: {
              token: 'default-token',
            },
          },
        },
      },
    }),
  ],
});

// OpenAPI docs available at /api
```

---

## 5. BEST PRACTICES

### 5.1 Code Organization

#### Monorepo Structure
```
project/
├── packages/
│   ├── contracts/           # API contracts
│   │   ├── src/
│   │   │   ├── index.ts
│   │   │   ├── auth.ts
│   │   │   └── schemas/
│   │   │       └── user.ts
│   │   └── tsup.config.ts
│   ├── auth-service/        # Auth implementation
│   │   ├── src/
│   │   │   ├── orpc.ts
│   │   │   ├── routers/
│   │   │   │   └── auth.ts
│   │   │   └── index.ts
│   └── shared/              # Utils, types
├── apps/
│   ├── server/              # Hono server
│   │   ├── src/
│   │   │   ├── main.ts
│   │   │   ├── index.html
│   │   │   └── routes/
│   │   └── package.json
│   ├── web/                 # Next.js/TanStack
│   │   ├── src/
│   │   │   ├── utils/
│   │   │   │   └── orpc.ts
│   │   │   ├── hooks/
│   │   │   └── components/
│   │   └── package.json
│   └── mobile/              # React Native
│       └── utils/orpc.ts
└── package.json
```

#### Dependency Management for TS2742 Prevention
```json
{
  "dependencies": {
    "@orpc/contract": "^1.8.5",
    "@orpc/server": "^1.8.5",
    "@orpc/client": "^1.8.5"
  }
}
```

```typescript
// tsup.config.ts for contracts package
export default defineConfig({
  external: [
    '@orpc/contract',
    '@orpc/server',
    'zod', // Important for preventing duplicate module resolution
  ],
});
```

### 5.2 Contract-First Development Workflow

```typescript
// 1. Define contract
// packages/contracts/auth.ts
export const signup = oc
  .input(NewUserSchema)
  .output(UserSchema);

// 2. Implement on server
// packages/auth-service/routers/auth.ts
export const signup = publicProcedure.auth.signup
  .handler(async ({ input }) => {
    return db.users.create(input);
  });

// 3. Consume on client
// apps/web/components/SignUpForm.tsx
const { mutate } = useMutation(
  orpc.auth.signup.mutationOptions()
);

await mutate({ email: 'user@example.com', name: 'John' });
// ✓ Fully typed, no runtime surprises!
```

### 5.3 Testing Patterns

#### Router Testing
```typescript
import { describe, it, expect } from 'vitest';
import { appRouter } from './index';

describe('auth router', () => {
  it('should sign up a user', async () => {
    const result = await appRouter.auth.signup.call({
      email: 'test@example.com',
      name: 'Test User',
    });
    
    expect(result).toHaveProperty('id');
    expect(result.email).toBe('test@example.com');
  });
});
```

### 5.4 Middleware Best Practices

#### Dedupe Middleware Pattern
```typescript
// Use ?? to avoid unnecessary middleware execution
export const dbProviderMiddleware = os
  .$context<{ db?: DB }>()
  .middleware(async ({ context, next }) => {
    // Only call createFakeDB() if db is not provided
    const db = context.db ?? createFakeDB();
    
    return next({
      context: { db },
    });
  });
```

#### Middleware Ordering
```typescript
// Good: Order matters
export const publicProcedure = os
  .use(loggingMiddleware)        // Log all requests
  .use(authMiddleware)           // Check auth first
  .use(rateLimitMiddleware)      // Rate limit authenticated users
  .use(dbProviderMiddleware);    // Setup DB context

// Bad: Wrong order
export const publicProcedure = os
  .use(rateLimitMiddleware)      // ✗ Rate limit before auth
  .use(authMiddleware)
  .use(dbProviderMiddleware);
```

### 5.5 Error Handling Best Practices

#### Define Errors in Contract
```typescript
export const updatePlanet = oc
  .errors({
    NOT_FOUND: {
      message: 'Planet not found',
      data: z.object({ id: z.number() }),
    },
    UNAUTHORIZED: {
      message: 'You cannot update this planet',
    },
    INVALID_INPUT: {
      message: 'Invalid planet data',
      data: z.object({ field: z.string() }),
    },
  })
  .input(UpdatePlanetSchema)
  .output(PlanetSchema);
```

#### Error Handler Pattern
```typescript
// Handler receives errors helper
export const updatePlanet = authed.planet.update
  .handler(async ({ input, context, errors }) => {
    const planet = await context.db.planets.find(input.id);
    
    if (!planet) {
      throw errors.NOT_FOUND({ data: { id: input.id } });
    }
    
    if (planet.creatorId !== context.auth.userId) {
      throw errors.UNAUTHORIZED();
    }
    
    return context.db.planets.update(input);
  });
```

### 5.6 Common Pitfalls & Solutions

| Pitfall | Problem | Solution |
|---------|---------|----------|
| **Duplicate Dependencies** | TS2742 error, multiple module instances | Centralize @orpc/* in root package.json, externalize in tsup.config.ts |
| **Wrong Middleware Order** | Auth checks happen after rate limiting | Order middleware logically (logging → auth → business logic) |
| **Context Type Mismatch** | Types don't match between middleware | Use explicit types and middleware composition |
| **No Error Definitions** | Client doesn't know about error types | Define errors in contract with proper schemas |
| **Missing Context** | Handler can't access required data | Use middleware to provide context |
| **Synchronous Validation** | Invalid data reaches handler | Use Zod schemas in contract definition |

---

## 6. ADVANCED PATTERNS

### 6.1 Multiservice Architecture

#### Service Aggregation Pattern
```typescript
// apps/api/src/main.ts
const server = createServer(async (req, res) => {
  // Try each service
  const authResult = await authServiceRPCHandler.handle(req, res, {
    prefix: '/rpc/auth',
    context: { authToken },
  });
  if (authResult.matched) return;
  
  const planetResult = await planetServiceRPCHandler.handle(req, res, {
    prefix: '/rpc/planet',
    context: { getAuth, db },
  });
  if (planetResult.matched) return;
  
  const chatResult = await chatServiceRPCHandler.handle(req, res, {
    prefix: '/rpc/chat',
    context: { roomPublisher, getAuth },
  });
  if (chatResult.matched) return;
  
  res.writeHead(404);
  res.end('Not Found');
});
```

#### Service Contracts
```typescript
// packages/planet-contract/src/index.ts
export const contract = {
  planet: {
    list: listPlanets,
    create: createPlanet,
    find: findPlanet,
    update: updatePlanet,
  },
};

// packages/auth-contract/src/index.ts
export const contract = {
  auth: {
    signup,
    signin,
    me,
  },
};
```

### 6.2 Complex Request/Response Types

#### File Uploads
```typescript
import { z } from 'zod';

export const createPlanet = oc
  .input(
    z.object({
      name: z.string(),
      description: z.string().optional(),
      image: z.instanceof(File).optional(),
    })
  )
  .output(PlanetSchema);

// Client usage
const { mutate } = useMutation(
  orpc.planet.create.mutationOptions()
);

const formData = new FormData();
formData.append('name', 'Mars');
formData.append('image', imageFile);

mutate(Object.fromEntries(formData));
```

#### Form Data Handling
```typescript
// Component
const handleSubmit = async (e: FormEvent) => {
  const form = new FormData(e.currentTarget);
  
  await createMutation.mutateAsync({
    name: form.get('name') as string,
    description: (form.get('description') as string) || undefined,
    image: form.get('image') as File | undefined,
  });
};
```

### 6.3 OpenAPI Specifications

#### Route Metadata
```typescript
export const listPlanets = oc
  .route({
    method: 'GET',
    path: '/planets',
    summary: 'List all planets',
    description: 'Retrieve a paginated list of planets',
    tags: ['Planets'],
  })
  .input(
    z.object({
      limit: z.number().int().min(1).max(100).default(10),
      cursor: z.number().int().min(0).default(0),
    })
  )
  .output(z.array(PlanetSchema));

export const createPlanet = oc
  .route({
    method: 'POST',
    path: '/planets',
    summary: 'Create a planet',
    tags: ['Planets'],
  })
  .input(NewPlanetSchema)
  .output(PlanetSchema);
```

---

## 7. KEY FILE LOCATIONS IN CODEBASE

### oRPC Query Example (Monorepo with Contracts)
- `/home/user/hackathon/web/examples/orpc_query/packages/contracts/src/index.ts` - Contract definitions
- `/home/user/hackathon/web/examples/orpc_query/apps/server/src/lib/orpc.ts` - Server setup with middleware
- `/home/user/hackathon/web/examples/orpc_query/apps/server/src/routers/public.ts` - Public procedures
- `/home/user/hackathon/web/examples/orpc_query/apps/server/src/routers/private.ts` - Protected procedures
- `/home/user/hackathon/web/examples/orpc_query/apps/web/src/utils/orpc.ts` - Web client setup
- `/home/user/hackathon/web/examples/orpc_query/apps/native/utils/orpc.ts` - Native client setup

### Multiservice Example (Service Architecture)
- `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/packages/auth-contract/src/contract/auth.ts` - Auth contract
- `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/packages/planet-contract/src/contract/planet.ts` - Planet contract
- `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/packages/chat-contract/src/contract/room.ts` - Chat contract (streaming)
- `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/packages/planet-service/src/orpc.ts` - Service ORPC setup
- `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/packages/planet-service/src/routers/planet.ts` - Procedure implementations
- `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/packages/planet-service/src/middlewares/` - Middleware implementations

### TanStack Start Example
- `/home/user/hackathon/web/base/default_tanstack_start/src/orpc/client.ts` - Isomorphic client setup
- `/home/user/hackathon/web/base/default_tanstack_start/src/orpc/router/todos.ts` - Simple todo procedures
- `/home/user/hackathon/web/base/default_tanstack_start/src/routes/api.rpc.$.ts` - RPC endpoint handler
- `/home/user/hackathon/web/base/default_tanstack_start/src/routes/api.$.ts` - OpenAPI endpoint handler
- `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/orpc-todo.tsx` - Component usage example

### Base Merged Example
- `/home/user/hackathon/base-merged/src/orpc/router/chat.ts` - Chat procedures
- `/home/user/hackathon/base-merged/src/orpc/router/documents.ts` - Document procedures

---

## 8. INTEGRATION DOCUMENTATION

### Media References
- **README**: `/home/user/hackathon/web/examples/orpc_query/README.md` - Basic setup guide
- **Medium Article**: `/home/user/hackathon/web/examples/orpc_query/MEDIUM.md` - Comprehensive tutorial with TS2742 error resolution
- **Multiservice README**: `/home/user/hackathon/web/examples/orpc-multiservice-monorepo-playground/README.md` - Architecture patterns

### Dependencies
Key @orpc packages used:
- `@orpc/contract` - Contract definitions and types
- `@orpc/server` - Server implementation
- `@orpc/client` - Client creation
- `@orpc/tanstack-query` - TanStack Query integration
- `@orpc/openapi` - OpenAPI generation
- `@orpc/zod` - Zod schema support
- `@orpc/server/fetch` - Fetch API handler
- `@orpc/server/node` - Node.js handler

---

## 9. SUMMARY

### oRPC Strengths
1. **True Type Safety**: Contracts propagate types through entire stack
2. **Developer Experience**: IDE autocomplete, instant feedback
3. **Less Boilerplate**: No need for separate client generation
4. **OpenAPI Auto-generation**: Documentation stays in sync with code
5. **Flexible**: Works with any framework (Hono, Next.js, etc.)
6. **Real-time Support**: Event iterators for streaming
7. **Error Type Safety**: Defined errors in contract

### When to Use oRPC
- TypeScript monorepos with shared code
- API-first applications where contract is critical
- Teams prioritizing type safety and DX
- Projects needing OpenAPI documentation
- Microservices with typed contracts

### Alternative Considerations
- **tRPC**: Similar DX, Node.js focused, different philosophy
- **REST + OpenAPI**: More standardized, less tight coupling
- **GraphQL**: More flexible queries, higher complexity

---

