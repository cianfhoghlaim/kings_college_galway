# TanStack Start: Visual Architecture Patterns

Extracted from the `base-merged` implementation.

## Complete Request/Response Cycle

```
User Browser                          Vite Dev Server              Backend Services
    │                                       │                               │
    ├─ GET /chat ─────────────────────────>│                               │
    │                                       │                               │
    │                                       ├─ Route Match ─────────────────┤
    │                                       │  (src/routes/chat.tsx)        │
    │                                       │                               │
    │                                       ├─ Execute Loader ──────────────┤
    │                                       │  (beforeLoad)                  │
    │                                       │                               │
    │                                       ├─ Render Component ────────────┤
    │                                       │  (React.renderToString)       │
    │                                       │                               │
    │ HTML + CSS + Metadata <─────────────┤                               │
    │ Stream begins                         │                               │
    │                                       │                               │
    ├─ Parse HTML                          │                               │
    ├─ Download JS bundles                 │                               │
    │     ↓                                 │                               │
    ├─ Execute entry-client.tsx            │                               │
    │     ├─ createRouter()                │                               │
    │     ├─ hydrateRoot()                 │                               │
    │     ├─ Attach event listeners        │                               │
    │                                       │                               │
    ├─ Interactive UI Ready ✓              │                               │
    │                                       │                               │
    ├─ User types in chat ─────────────────>│ POST /api/chat                │
    │                                       ├──────────────────────────────>│
    │                                       │                               ├─ Call anthropic()
    │                                       │                               ├─ Stream response
    │<─────────── SSE Stream ──────────────┤<──────────────────────────────┤
    │                                       │                               │
    ├─ useChat hook updates               │                               │
    ├─ Component re-renders               │                               │
    ├─ New message appears                │                               │
    │                                       │                               │

```

## File Organization & Module Boundaries

```
src/
├── routes/                          ┌─────────────────────────────────┐
│   ├── __root.tsx      ─────┬──────>│  ROOT PROVIDER BOUNDARY         │
│   ├── index.tsx       │    │       │  ├─ TanstackQueryProvider      │
│   ├── chat.tsx        │    │       │  └─ ConvexClientProvider      │
│   │                   │    │       └─────────────────────────────────┘
│   └── api/            │    │                    │
│       ├── chat.ts     │    │                    ├─ Outlet (child routes)
│       └── rpc.$.ts    │    │                    │
│                       │    │       ┌─────────────┴──────────────────────┐
│                       │    └──────>│  ROUTE COMPONENTS                 │
│                       │            │  - Client only (hydrated)          │
│                       │            │  - Can use hooks (useState, etc.)  │
│                       │            └───────────────────────────────────┘
│                       │
│                       └──────────>  SERVER HANDLERS (POST /api/chat, etc.)
│                                     │
│                                     ├─ Async functions on server
│                                     ├─ Direct DB/secrets access
│                                     └─ Streaming responses
│
├── entry-server.tsx   ────────────>  SERVER ENTRY POINT
│                                     │
│                                     ├─ Matches incoming HTTP request
│                                     ├─ Executes handlers/loaders
│                                     ├─ Renders to HTML stream
│                                     └─ Returns Response
│
├── entry-client.tsx   ────────────>  CLIENT ENTRY POINT
│                                     │
│                                     ├─ hydrateRoot() call
│                                     ├─ Event listener attachment
│                                     └─ Client-side routing
│
├── router.tsx         ────────────>  ROUTER CONFIG
│                                     │
│                                     ├─ Route tree from routes/
│                                     ├─ Type registration
│                                     └─ Preload strategy
│
├── routeTree.gen.ts   ────────────>  AUTO-GENERATED
│                                     │
│                                     ├─ All routes indexed
│                                     ├─ Type definitions
│                                     ├─ Route hierarchy
│                                     └─ File-based import map
│
├── orpc/
│   ├── router/        ────────────>  API IMPLEMENTATIONS
│   │   └── chat.ts                   │
│   │                                 ├─ oRPC handlers
│   ├── client.ts      ────────────>  ISOMORPHIC CLIENT
│   │                                 │
│   │                                 ├─ .server() = Direct call
│   │                                 ├─ .client() = HTTP fetch
│   │                                 └─ Auto-typed from handlers
│   │
│   └── schema.ts      ────────────>  ZOD VALIDATION
│                                     │
│                                     ├─ Input schemas
│                                     ├─ Output schemas
│                                     └─ Runtime + TypeScript validation
│
├── integrations/      ────────────>  PROVIDER SETUP
│   ├── tanstack-query/              │
│   │   └── provider.tsx             ├─ QueryClient initialization
│   │                                 ├─ Devtools setup
│   └── convex/                      └─ Context defaults
│       └── provider.tsx
│
├── components/        ────────────>  REACT COMPONENTS
│   ├── DocumentChat.tsx             │
│   ├── ChatMessage.tsx              ├─ Client-side only
│   └── ...                          ├─ Use hooks (useState, etc.)
│                                     └─ Wrapped in providers
├── lib/
│   ├── auth.ts        ────────────>  SERVER + CLIENT CONFIG
│   ├── auth-client.ts              │
│   └── utils.ts                    ├─ Utilities
│                                     └─ Shared helpers
└── db/
    └── schema.ts      ────────────>  DATABASE SCHEMA
                                      │
                                      └─ D1/Drizzle ORM
```

## Type Safety: End-to-End Flow

```
Code Layer                          Type Checking              Runtime Validation
─────────────────────────────────────────────────────────────────────────────

1. Zod Schema
   ┌──────────────────────────┐
   │ export const             │  ───┬──> TypeScript type  ──┬──> Zod.parse()
   │  CreateChatInput =       │     │    inference          │     validates at
   │  z.object({             │     │                        │     runtime
   │    title: z.string(),   │     │                        │
   │  })                      │     │                        │
   └──────────────────────────┘     │                        │
                                     │                        │
2. oRPC Handler                      │                        │
   ┌──────────────────────────┐     │                        │
   │ export const createChat  │  ───┤─> Handler knows       │
   │   = os                   │     │    input type          │
   │   .input(Schema)         │     │                        │
   │   .handler(({input}) =>  │     │                        │
   │     // input is typed!   │     │                        │
   │   )                      │     │                        │
   └──────────────────────────┘     │                        │
                                     │                        │
3. Router Registration               │                        │
   ┌──────────────────────────┐     │                        │
   │ export default {         │  ───┤─> Router has all       │
   │   createChat,            │     │    handler types       │
   │   // ...                 │     │                        │
   │ }                        │     │                        │
   └──────────────────────────┘     │                        │
                                     │                        │
4. Type Module Declaration           │                        │
   ┌──────────────────────────┐     │                        │
   │ declare module           │  ───┤─> Global router type   │
   │   '@tanstack/react-      │     │    available           │
   │    router' {             │     │                        │
   │   interface Register {   │     │                        │
   │     router: typeof       │     │                        │
   │       router             │     │                        │
   │   }                      │     │                        │
   │ }                        │     │                        │
   └──────────────────────────┘     │                        │
                                     │                        │
5. Client Usage (FULLY TYPED!)       │                        │
   ┌──────────────────────────┐     │                        │
   │ const result =           │  ───┬──> TypeScript knows  ──┴──> HTTP call
   │   client.createChat({    │         input type must      matches type
   │   title: 'My Chat'       │         have 'title' field   from schema
   │ })                       │                              (validated on
   │                          │         TypeScript errors    server side)
   │ // Fully typed!          │         if wrong shape
   └──────────────────────────┘

Data Flow Summary:
- Schema defines contract
- Handler implements contract
- Router exposes all handlers with types
- Module declaration makes types global
- Client gets autocomplete and type checking
- Runtime validation matches TypeScript types
```

## Streaming: Progressive HTML Rendering

```
Timeline: How HTML Reaches Browser

t=0ms: Server starts rendering
       │
       ├─ Critical HTML headers sent
       │  <html><head>...</head><body><div id="root">
       │
       ├─ Suspense boundaries pause for async
       │  <Suspense fallback={...}><Loading/></Suspense>
       │
t=100ms: Critical CSS
        │  <style>/* Critical path styles */</style>
        │
t=150ms: Initial page structure visible
        │  [Browser begins rendering while waiting for JS]
        │  User sees: Skeleton UI, Loading states
        │
t=200ms: Remaining HTML
        │  More route-specific HTML
        │  More CSS
        │
t=250ms: JavaScript chunks start arriving
        │  <script src="/_start/chunk-1.js"></script>
        │  <script src="/_start/chunk-2.js"></script>
        │
t=300ms: Script metadata (router state, loader data)
        │  <script>
        │    window.__ROUTER_STATE__ = {...}
        │    window.__LOADER_DATA__ = {...}
        │  </script>
        │
t=350ms: entry-client.tsx executes
        │  ├─ createRouter()
        │  ├─ Initialize providers
        │  └─ hydrateRoot()
        │
t=400ms: Hydration complete
        │  Event listeners attached
        │  UI becomes interactive ✓

Traditional SSR:
t=0 ───────────────────────────────────────────── t=500ms
     [Waiting for everything]
     [Nothing visible on screen]
                                    Visible ────────>

TanStack Start Streaming:
t=0 ──────> [Visible] ──────────────> [Interactive]
     100ms          250ms               400ms
     
User sees content faster!
```

## Hydration: Server → Client Transition

```
SERVER (entry-server.tsx)              CLIENT (entry-client.tsx)
─────────────────────────────────      ──────────────────────────

1. HTTP request arrives                1. Browser receives HTML
   GET /chat                              ├─ Parses HTML
   req, ctx                               ├─ Renders to page
                                         ├─ Loads CSS
2. Router matches                       ├─ Downloads JS bundles
   /chat → src/routes/chat.tsx

3. Create providers                   2. JS executes
   ├─ QueryClient                       ├─ import statements
   ├─ ConvexClient                      ├─ module initialization

4. Render React tree                 3. entry-client.tsx runs
   React.renderToString()               const router = createRouter()
   ├─ <RootComponent>                   hydrateRoot(
   ├─ <ChatComponent>                     document.getElementById('root')!,
   ├─ <DocumentChat/>                     <StartClient router={router} />
                                       )

5. Generate HTML string             4. React "hydrates"
   ├─ Serialize component tree        ├─ Attaches to existing DOM
   ├─ Serialize styles                ├─ Restores router state
   ├─ Embed metadata                  ├─ Restores component state
   │  └─ __ROUTER_STATE__             ├─ CRITICAL: Must match server!
   │  └─ __LOADER_DATA__

6. Stream to client                 5. Event listeners attached
   Response.stream()                  ├─ onClick handlers
   ├─ HTML chunks                     ├─ Form submissions
   ├─ Timing: chunked                 ├─ Input changes
                                      └─ All interactive features

7. Complete                        6. Navigation is now client-side
   response.end()                     ├─ No more full page reloads
                                      ├─ Only route components update
                                      └─ Smooth UX

HYDRATION CONTRACT:
- Server HTML + React tree state MUST MATCH Client render
- If mismatch: Warning in console, React falls back to client render
- Avoid: randomness, Date.now(), client-only state in server render
```

## State Management Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        APPLICATION STATE                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                    │
│  SERVER STATE (Source of Truth)                                  │
│  ├─ Database (Convex, D1)                    ┌─────────────────┐ │
│  ├─ Third-party APIs                         │  SERVER CONTEXT │ │
│  └─ Secrets/Config                           │  ├─ Headers     │ │
│                                               │  ├─ User ID     │ │
│     [Accessed on server via handlers/loaders] │  ├─ Cookies     │ │
│                                               │  └─ Env vars    │ │
│                                               └─────────────────┘ │
│         │                                              │            │
│         │ Serialize & Embed in HTML                   │            │
│         └──────────────────┬──────────────────────────┘            │
│                            │                                        │
│         ┌──────────────────▼────────────────────────┐              │
│         │ CLIENT STATE (Cached & Synced)            │              │
│         └──────────────────┬────────────────────────┘              │
│                            │                                        │
│    ┌───────────────────────┼───────────────────────┐               │
│    │                       │                       │               │
│    ▼                       ▼                       ▼               │
│  TanStack Query         UI State              Form State         │
│  ├─ Cache from        TanStack Store         React State        │
│    server state       ├─ Simple objects      ├─ useChat()       │
│  ├─ Refetch strategy  ├─ Events             ├─ useState()      │
│  ├─ Background sync   └─ Subscriptions      └─ useForm()       │
│  └─ Optimistic updates                                          │
│                                                                   │
│  Handles: GET requests, list items, cached data                │
│  Handles: Theme, UI visibility, animations                     │
│  Handles: Form input, validation, focus                        │
│                                                                   │
│  Server data flows DOWN (server→client)                         │
│  Mutations flow UP (client→server)                              │
│  TanStack Query invalidates & refetches                         │
└──────────────────────────────────────────────────────────────────┘
```

## Build Process: From Routes to HTTP

```
                        TanStack Start Plugin
                        (Vite integration)
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
    
    File Scanning        Route Analysis        Type Generation
    └─ src/routes/      └─ Identify:           └─ routeTree.gen.ts
      ├─ index.tsx          ├─ Components      ├─ Route types
      ├─ chat.tsx           ├─ Handlers        ├─ Type registry
      ├─ api/               ├─ Loaders         └─ Exports
      │  ├─ chat.ts         └─ Params
      │  └─ rpc.$.ts
      └─ __root.tsx

         │
         └──> Analyzed Routes
              ├─ /         (index.tsx)
              ├─ /chat     (chat.tsx)
              ├─ /api/chat (api/chat.ts - handler)
              └─ /api/rpc/* (api/rpc.$.ts - RPC)

                      │
         ┌────────────┼────────────┐
         │            │            │
         ▼            ▼            ▼
    
    Bundle      Code Splitting    Metadata
    ├─ Client    ├─ Entry client   ├─ Handlers
    │  ├─ JS     ├─ Each route     ├─ Loaders
    │  ├─ CSS    │  is separate     ├─ Types
    │  └─ Assets │  chunk          └─ Preload hints
    │            └─ Lazy loaded
    └─ Server
       ├─ JS
       └─ Entry point

         │
         └──> HTTP Server Ready
              ├─ GET / → route handler
              ├─ GET /chat → route handler
              ├─ POST /api/chat → handler
              └─ /api/rpc/* → oRPC handler

Routes are HTTP endpoints!
```

## Error Handling Flow

```
Component Renders                    Error Occurs
   │                                    │
   ├─ Route loads                      ├─ Throws error
   ├─ Loader executes                  │
   │                                    ▼
   ├─ Component renders           Error Boundary?
   │                                    │
   │                          ┌─────────┼─────────┐
   │                          │         │         │
   │                         YES       NO       YES
   │                          │         │         │
   │                          ▼         ▼         ▼
   │                      Caught    Unhandled  Caught at
   │                      at        Error:     component
   │                      loader    500 Page   level
   │                      level     Error      (Error
   │                      (throw              component)
   │                      redirect)
   │
   ├─ Component renders
   ├─ Returns HTML
   │
   └─> Client
       ├─ Hydrates
       ├─ Navigation errors
       │  caught by router
       └─ Component errors
          caught by boundary

Error Precedence:
1. beforeLoad errors → Redirect or error component
2. Handler errors → 500 response
3. Component render errors → Error boundary
4. Event handler errors → Logged to console
```

## Integration Provider Composition

```
Application Root
    │
    └─> __root.tsx (Root Layout)
        │
        └─> TanstackQueryProvider (QueryClient setup)
            │
            └─> ConvexClientProvider (Convex init)
                │
                └─> TanStackRouterDevtools (Dev only)
                    │
                    └─> <Outlet /> (Child routes render here)
                        │
                        ├─> /chat
                        │   └─> DocumentChat component
                        │
                        └─> /
                            └─> Home component

Each Provider:
1. Creates/initializes service (QueryClient, ConvexClient)
2. Sets default options
3. Adds devtools
4. Wraps children with Context
5. Children can useQuery(), useConvex(), etc.

Data flows:
- Down: Props, Context values
- Up: Events, Mutations
- Side: Network requests (Query), subscriptions (Convex)
```

## Isomorphic Code Pattern

```
createIsomorphicFn()
    │
    ├─ .server(() => {
    │     // This code is bundled ONLY for server
    │     // Has access to:
    │     ├─ Database connections
    │     ├─ Environment variables
    │     ├─ File system
    │     ├─ Server context (headers, cookies)
    │     └─ Secret APIs
    │     
    │     return data from getRequestHeaders()
    │   })
    │
    ├─ .client(() => {
    │     // This code is bundled ONLY for client
    │     // Has access to:
    │     ├─ window, document
    │     ├─ Fetch API
    │     ├─ Local storage
    │     ├─ DOM APIs
    │     └─ HTTP endpoints
    │     
    │     return fetch('/api/data').then(r => r.json())
    │   })
    │
    └─> getORPCClient()
        │
        ├─ On server: Direct function call (no HTTP)
        │  - Fast, has all context
        │  - Can't hydrate in browser (different impl)
        │
        └─ On client: HTTP request
           - Slower, but available in browser
           - Matches server-side API contract

Usage:
const client = getORPCClient()
const data = await client.getChatMessages({ chatId: '123' })

// Same line of code
// Different implementation based on context!
```

## Performance Characteristics

```
Metric                  Impact              Strategy
──────────────────────────────────────────────────────────

First Byte (TTFB)      ⬆️ Critical          Streaming HTML early
                                           Execute expensive loaders
                                           in parallel

First Contentful       ⬆️ High              Send minimal HTML first
Paint (FCP)                                 Use Suspense fallbacks
                                           Skeleton screens

Largest Contentful    ⬆️ High              Code splitting per route
Paint (LCP)                                Preload on intent
                                           TanStack Query caching

Cumulative Layout     ⬆️ Medium            Prevent hydration mismatch
Shift (CLS)                                Fixed size containers
                                           suppressHydrationWarning

Time to Interactive   ⬆️ Critical          Minimal JavaScript
(TTI)                                      hydrateRoot() fast
                                           React compiler

Core Web Vitals Score ⬆️ SEO              All above combined


Route Preloading:
  defaultPreload: 'intent'
  ├─ Preload on mouseover/focus (smart!)
  ├─ Download JS in background
  └─ Click is instant

Code Splitting:
  Each route → separate chunk
  ├─ Parallel downloads
  ├─ Lazy loaded
  └─ Only necessary code

Caching:
  TanStack Query
  ├─ Client-side cache
  ├─ Configurable staleTime
  └─ Background refetch
```

## Key Mental Models

### Mental Model 1: "One Framework, Two Contexts"

```
Think of TanStack Start as a single application with TWO execution contexts:

┌─ SERVER CONTEXT ──────────┐    ┌─ CLIENT CONTEXT ────────────┐
│ Node.js / Worker Runtime  │    │ Browser / JavaScript Engine │
│                           │    │                             │
│ ✓ Database access         │    │ ✗ Database access          │
│ ✓ File system             │    │ ✗ File system              │
│ ✓ Secrets                 │    │ ✗ Secrets                  │
│ ✓ CPU-intensive compute   │    │ ✗ CPU-intensive            │
│ ✗ DOM                     │    │ ✓ DOM                      │
│ ✗ Browser APIs            │    │ ✓ Browser APIs             │
│ ✓ HTTP (incoming)         │    │ ✓ HTTP (outgoing)          │
│                           │    │                             │
│ Entry: entry-server.tsx   │    │ Entry: entry-client.tsx    │
│ Code: handlers, loaders   │    │ Code: components, hooks    │
└─────────────────────────────    ───────────────────────────┘
      │                                      │
      └──────────── SAME CODEBASE ──────────┘
           File-based routes
           Shared types
           Shared schemas
           Isomorphic functions
```

### Mental Model 2: "Convention Over Configuration"

```
File name = URL path = Type-safe route

src/routes/chat.tsx
    ↓
GET /chat
    ↓
Typed route in useRouter(), navigate()

No config files needed!
```

### Mental Model 3: "Type Safety Flows End-to-End"

```
Schema → Handler → Router → Client = Fully typed

Zod.object({ title: z.string() })
         ↓
  os.input(Schema).handler(({ input }) => {...})
         ↓
  export default { createChat }
         ↓
  declare module '@tanstack/react-router' { interface Register { router } }
         ↓
  const result = client.createChat({ title: 'test' })
  // ✓ TypeScript knows this is valid
  // ✓ Runtime validates input
  // ✓ Response type known
  // ✓ No bugs!
```

### Mental Model 4: "Colocation Reduces Cognitive Load"

```
Same file, same directory hierarchy, related code together

src/routes/api/rpc.$.ts     → API endpoint definition
src/orpc/router/index.ts    → All handlers listed
src/orpc/router/chat.ts     → Implementation of chat operations
src/orpc/schema.ts          → Types used by chat operations
src/routes/chat.tsx         → Component that uses chat operations
src/components/DocumentChat → UI component for chat

Easy to find related code!
```

