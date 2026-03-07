# TanStack Start: Comprehensive Architecture Research Report

## Executive Summary

TanStack Start is a modern, full-stack React framework that combines:
- **Server-Side Rendering (SSR)** with streaming responses
- **File-based routing** via TanStack Router
- **Type-safe APIs** using oRPC or custom handlers
- **Isomorphic code execution** (same code runs on server and client)
- **Vite-powered build system** with SSR-aware plugins
- **Automatic route generation** and code generation
- **First-class TypeScript support** with strict type safety

At its core, TanStack Start is designed to eliminate the complexity of modern full-stack development by providing an opinionated, integrated framework that handles both rendering modes seamlessly.

---

## 1. Overall Architecture and Design Philosophy

### Core Mental Model: "Unified Framework"

TanStack Start treats the server and client as a single unified codebase with two execution contexts, rather than separate applications. Key principles:

1. **Isomorphic by Default**: Write code once, run everywhere (server and client)
2. **Progressive Enhancement**: Server handles rendering; client hydrates for interactivity
3. **Type Safety First**: End-to-end TypeScript ensures correctness across the boundary
4. **Developer Experience**: Minimal configuration, sensible defaults, clear conventions

### Architectural Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  Presentation Layer                                              │
│  ├─ React Components (Routes)                                   │
│  ├─ UI State Management (TanStack Store)                        │
│  └─ Styling (Tailwind CSS, CSS Modules)                         │
├─────────────────────────────────────────────────────────────────┤
│  Routing & Request Handling                                     │
│  ├─ File-based Routes (TanStack Router)                         │
│  ├─ API Routes (Server handlers)                                │
│  ├─ Route Loaders (Data loading)                                │
│  └─ Server Actions (Mutations)                                  │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                      │
│  ├─ Server State (TanStack Query + Suspense)                    │
│  ├─ RPC/API Clients (oRPC, Fetch)                               │
│  ├─ Database Queries (Server-only)                              │
│  └─ External Services (AI, Third-party APIs)                    │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure                                                  │
│  ├─ Build System (Vite + TanStack plugins)                      │
│  ├─ HTTP Server (Express-like, Cloudflare Workers, Netlify)     │
│  ├─ Streaming & Serialization                                   │
│  └─ Runtime Context (Server environment variables, KV, etc.)    │
└─────────────────────────────────────────────────────────────────┘
```

### Design Philosophy Principles

1. **Colocation**: Keep related code together (component + data loading + API handler)
2. **Streaming**: Stream HTML and data to enable faster initial renders
3. **Server-centric**: Leverage server advantages (database access, secrets, computation)
4. **Automatic Code Splitting**: Routes are code-split automatically
5. **Type-Driven**: TypeScript is the source of truth, not an afterthought

---

## 2. Key Concepts and Terminology (Ontology)

### Core Terms

| Term | Definition | Example |
|------|-----------|---------|
| **File Route** | A file in `src/routes/` that defines a URL route | `/src/routes/chat.tsx` → `GET /chat` |
| **Route Tree** | Hierarchical structure of all routes (auto-generated) | `routeTree.gen.ts` - indexes all routes |
| **Entry Point** | Execution starts here (server-side and client-side) | `entry-server.tsx`, `entry-client.tsx` |
| **Handler** | Server function handling HTTP method | `POST`, `GET`, `PUT`, `DELETE` in routes |
| **Loader** | Async function loading data before rendering | `beforeLoad`, `loaderData` |
| **Action** | Server function for mutations/side effects | Form submissions, state changes |
| **Hydration** | Client-side React attachment to server-rendered HTML | `hydrateRoot()` in entry-client.tsx |
| **Isomorphic Function** | Code that runs on both server and client | `createIsomorphicFn().server(...).client(...)` |
| **Streaming** | Sending HTML/data in chunks instead of waiting for complete render | SSR streaming response |
| **Plugin** | Vite plugin extending build behavior | `tanstackStart()`, `@netlify/vite-plugin-tanstack-start` |

### Data Flow Ontology

```
Request Flow (Server-to-Client):
HTTP Request
  ↓
Router matches file route
  ↓
Server-side handlers execute
  ↓
Data loader executes (beforeLoad)
  ↓
React component renders to string
  ↓
HTML streamed to client
  ↓
Metadata/data serialized and embedded
  ↓
Client hydrates in browser
  ↓
Event listeners attached
  ↓
Interactive UI ready

State Management Ontology:
Server State (Source of Truth)
  ├─ Database (Convex, D1, PostgreSQL)
  ├─ External APIs (AI, Third-party)
  └─ Runtime Context (User session, headers)
  
Client State
  ├─ Cached Server State (TanStack Query)
  ├─ UI State (TanStack Store, React state)
  └─ Local Form State (TanStack Form, React state)
```

### Routing Ontology

1. **File-based routes**: File name = URL path
2. **Dynamic segments**: `[param]` in filename
3. **Catch-all routes**: `$` (splat) for remaining path
4. **API routes**: Files in `routes/api/` become API endpoints
5. **Layout routes**: `__root.tsx` and `_layout.tsx` for wrapper components
6. **Route tree**: Auto-generated from file structure

---

## 3. Build System and Bundling Approach

### Vite + TanStack Plugin System

**Build Configuration (`vite.config.ts`)**:

```typescript
import { tanstackStart } from '@tanstack/react-start/plugin/vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import netlifyPlugin from '@netlify/vite-plugin-tanstack-start'

export default defineConfig({
  plugins: [
    react({ babel: { plugins: [['babel-plugin-react-compiler', {}]] } }),
    ...tanstackStart(),           // TanStack Start plugin (spreads multiple plugins)
    tailwindcss(),                // CSS preprocessing
    tsconfigPaths(),              // Path alias resolution
    ...(enableNetlifyPlugin ? netlifyPlugin() : []),  // Platform-specific plugin
  ],
  // ... configuration
})
```

### What the TanStack Start Plugin Does

The `tanstackStart()` plugin performs multiple transformations:

1. **Route Tree Generation**: Scans `src/routes/` and generates `routeTree.gen.ts`
2. **Server/Client Split**: Identifies code that runs only on server vs. client
3. **API Route Recognition**: Converts file routes into HTTP endpoints
4. **Code Splitting**: Automatically chunks route components
5. **Type Generation**: Creates TypeScript types for routes
6. **Metadata Extraction**: Extracts loader data, handlers, etc.

### Build Output Structure

```
.output/
├── public/                    # Client-side static assets
│   └── _start/               # TanStack Start generated assets
│       ├── [hash].js         # Client code chunks
│       ├── manifest.json     # Asset manifest
│       └── styles.css        # CSS bundles
├── server/                   # Server code
│   ├── index.mjs             # Server entry point
│   ├── [handler].mjs         # API route handlers
│   └── chunks/               # Code chunks
└── [platform-specific]/      # Netlify functions, CF Workers, etc.
```

### Bundling Strategy

1. **Client Bundle**: Tree-shaken React components, routes are code-split
2. **Server Bundle**: Full Node.js/Worker runtime, database drivers, secrets
3. **Streaming**: HTML streamed before data loads (progressive rendering)
4. **Asset Optimization**: Images, fonts hashed and cached
5. **Tree Shaking**: Unused code removed at build time
6. **Minification**: Production builds minified and optimized

### Platform Integration

```
Vite Config → TanStack Plugin → Platform Adapter → Deployment Format

Examples:
- Cloudflare Workers: .wrangler/
- Netlify Functions: .netlify/functions/
- Node.js: .output/server/
- Vercel: .vercel/
- Docker: .output/ (standalone)
```

---

## 4. Server/Client Split and Hydration

### Execution Model

**Two Entry Points**:
- `src/entry-server.tsx`: Runs only on the server during SSR
- `src/entry-client.tsx`: Runs only on the browser to hydrate

### Server Entry Point

```typescript
// src/entry-server.tsx
import { StartServer } from '@tanstack/react-start/server'
import { createRouter } from './router'
import type { RequestHandler } from '@tanstack/react-start'

export default (async (req, ctx) => {
  const router = createRouter()
  
  // Render to stream (enables progressive rendering)
  const stream = await router.server.renderToStream(
    <StartServer router={router} />,
    {
      req,
      ctx,
    },
  )
  
  return new Response(stream, {
    headers: {
      'Content-Type': 'text/html',
      'Transfer-Encoding': 'chunked',
    },
  })
}) satisfies RequestHandler
```

**What happens**:
1. HTTP request arrives
2. Router matches against file routes
3. Server-side handlers/loaders execute
4. React component tree renders to HTML string
5. HTML streamed in chunks to client
6. Critical data serialized and embedded in HTML

### Client Entry Point

```typescript
// src/entry-client.tsx
import { hydrateRoot } from 'react-dom/client'
import { StartClient } from '@tanstack/react-start'
import { createRouter } from './router'

const router = createRouter()

// Hydrate: attach React event listeners to server-rendered HTML
hydrateRoot(document.getElementById('root')!, <StartClient router={router} />)
```

**What happens**:
1. Browser receives HTML from server
2. HTML is parsed and rendered by browser
3. React hydrates (attaches event listeners, reuses DOM)
4. Client-side state is initialized
5. Subsequent navigation is client-side (no full page reload)

### The Hydration Contract

**Critical**: Server render and client render must produce identical output to enable hydration.

Mechanisms to ensure this:
- No randomness in server render
- No date/time differences (shared state)
- No client-only state in server render
- Metadata embedded in HTML to restore state

**Suppressing Hydration Mismatches**:
- Use `suppressHydrationWarning` on problematic elements
- Defer client-only rendering with `useEffect`
- Use Suspense boundaries for async content

### Isomorphic Function Pattern

The framework provides `createIsomorphicFn()` to write code that behaves differently on server vs. client:

```typescript
// src/orpc/client.ts
const getORPCClient = createIsomorphicFn()
  .server(() =>
    createRouterClient(router, {
      context: () => ({
        headers: getRequestHeaders(),  // Only on server
      }),
    }),
  )
  .client((): RouterClient<typeof router> => {
    const link = new RPCLink({
      url: `${window.location.origin}/api/rpc`,  // Only on client
    })
    return createORPCClient(link)
  })

export const client: RouterClient<typeof router> = getORPCClient()
```

**This pattern allows**:
- Server: Direct database/RPC calls (no network overhead)
- Client: Fetch-based RPC calls over HTTP

---

## 5. Middleware and Plugin Architecture

### Route Handlers: Server-Side Middleware

File routes can define HTTP handlers that execute on the server:

```typescript
// src/routes/api/rpc.$.ts
import { createFileRoute } from '@tanstack/react-router'
import { RPCHandler } from '@orpc/server/fetch'
import router from '~/orpc/router'

const handler = new RPCHandler(router, {
  context: () => ({}),
})

const handle = async ({ request }: { request: Request }) => {
  const result = await handler.handle(request)
  if (!result.matched) {
    return new Response('Not Found', { status: 404 })
  }
  return result.response
}

export const Route = createFileRoute('/api/rpc/$')({
  server: {
    handlers: {
      GET: handle,
      POST: handle,
      PUT: handle,
      PATCH: handle,
      DELETE: handle,
    },
  },
})
```

### Loader Pattern: Data Loading Middleware

Loaders execute before rendering and can access server context:

```typescript
export const Route = createFileRoute('/dashboard/$userId')({
  beforeLoad: async ({ params, context }) => {
    // Run before rendering, has access to server context
    const user = await db.user.findUnique({ id: params.userId })
    if (!user) throw new NotFoundError()
    return { user }
  },
  component: DashboardComponent,
})
```

### Provider Pattern: Context Middleware

Wrap the app with providers to add global functionality:

```typescript
// src/routes/__root.tsx
export const Route = createRootRoute({
  component: RootComponent,
})

function RootComponent() {
  return (
    <TanstackQueryProvider>
      <ConvexClientProvider>
        <TanStackRouterDevtools position="bottom-right" />
        <Outlet />
      </ConvexClientProvider>
    </TanstackQueryProvider>
  )
}
```

### Integration Providers: Pluggable Systems

Providers initialize third-party services:

```typescript
// src/integrations/tanstack-query/provider.tsx
export function TanstackQueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            refetchOnWindowFocus: false,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}
```

### Vite Plugin System: Build-Time Middleware

Plugins transform code during build:

1. **React Plugin** (`@vitejs/plugin-react`): JSX compilation, Fast Refresh
2. **TanStack Start Plugin** (`tanstackStart()`): Route tree generation, code splitting
3. **Tailwind Plugin** (`@tailwindcss/vite`): CSS generation from utilities
4. **Platform Plugins** (`@netlify/vite-plugin-tanstack-start`): Platform-specific optimizations

Each plugin hooks into Vite's build pipeline to transform modules.

---

## 6. Integration Points with Vite, Vinxi, and Other Tools

### Vite Ecosystem Integration

| Tool | Purpose | Integration |
|------|---------|-------------|
| **Vite** | Build tool & dev server | Primary bundler, fast HMR, module federation |
| **esbuild** | Fast JS/TS transpiler | Vite uses for production builds |
| **Rollup** | Module bundler | Vite uses for code splitting, tree-shaking |
| **Babel** | JS transpiler | React Compiler integration |

### TanStack Ecosystem

```
TanStack Start
├─ TanStack Router          # File-based routing
├─ TanStack Query           # Server state management
├─ TanStack Store           # Simple state container
├─ TanStack Form            # Form validation & state
├─ TanStack Router Devtools # Debugging
└─ TanStack Devtools        # Performance monitoring
```

### Vinxi Notes

Vinxi was the predecessor/underlying technology for TanStack Start:
- TanStack Start abstracts away Vinxi complexity
- Uses Vinxi's manifest system for SSR
- Provides cleaner API on top of Vinxi

### Third-Party Integrations

**API Solutions**:
- **oRPC**: Type-safe RPC with OpenAPI generation
- **TRPC**: Type-safe API layer (alternative)
- **Hono**: Lightweight HTTP framework
- **Express**: Traditional Node.js server

**State Management**:
- **TanStack Query**: Server state (default)
- **Convex**: Real-time database (in base-merged)
- **Redux**: Complex app state (alternative)
- **Zustand**: Lightweight state (alternative)

**Database Access**:
- **Drizzle ORM**: Type-safe query builder
- **Prisma**: ORM with migrations
- **SQLc**: SQL-first approach
- **Raw SQL**: For fine-grained control

**Data Streaming**:
- **React Server Components** (RSC): Streaming component serialization
- **Suspense**: Progressive rendering boundaries
- **Error Boundaries**: Error handling during streaming

**Authentication**:
- **Better Auth**: OAuth + session management (in base-merged)
- **Clerk**: Authentication service
- **Auth0**: Enterprise auth
- **NextAuth**: Self-hosted auth

### Deployment Platform Integrations

Each platform requires specific plugin:

```typescript
// Netlify
import netlifyPlugin from '@netlify/vite-plugin-tanstack-start'

// Cloudflare Workers
import { cloudflarePlugin } from '@tanstack/react-start/adapters/cloudflare'

// Vercel (via Node.js adapter)
// No special plugin needed

// AWS Lambda (via Node.js adapter)
// Custom serverless.yml configuration
```

---

## 7. Type System and Type Safety Approach

### TypeScript Configuration

```json
{
  "compilerOptions": {
    "jsx": "react-jsx",
    "jsxImportSource": "react",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "moduleDetection": "force",
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": true,
    "noEmit": true,
    "strict": true,
    "skipLibCheck": true,
    "noUncheckedIndexedAccess": true,
    "noPropertyAccessFromIndexSignature": false,
    "baseUrl": ".",
    "paths": {
      "~/*": ["./src/*"]
    }
  }
}
```

**Key Options**:
- `strict: true`: Enables all strict type checks
- `verbatimModuleSyntax`: Ensures correct module syntax
- `noEmit: true`: Vite handles compilation, TS is for type checking
- `moduleResolution: "Bundler"`: Supports modern module resolution
- `paths`: Enables path aliases (e.g., `~/` for `src/`)

### Type Safety Across Boundaries

**1. Route Type Registry**:
```typescript
// Auto-generated, maintains type safety for all routes
declare module '@tanstack/react-router' {
  interface FileRoutesByPath {
    '/': {
      id: '/'
      path: '/'
      fullPath: '/'
      preLoaderRoute: typeof IndexRouteImport
      parentRoute: typeof rootRouteImport
    }
    // ... all routes
  }
}
```

**2. Router Type Registration**:
```typescript
// src/router.tsx
export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
```

This ensures:
- `useRouter()` knows the exact router type
- `navigate()` only accepts valid routes
- Route parameters are typed correctly

**3. API Type Safety with oRPC**:
```typescript
// src/orpc/router/index.ts
export default {
  listChats,
  createChat,
  getChatMessages,
  createMessage,
  listDocuments,
  createDocument,
  getDocument,
}

// Auto-generates:
// - OpenAPI schema
// - Client types
// - JSON schema validation
```

**4. Zod Schema Integration**:
```typescript
// src/orpc/schema.ts
export const CreateChatInput = z.object({
  title: z.string(),
})

// Type inferred from schema
type CreateChatInput = z.infer<typeof CreateChatInput>
```

Benefits:
- Runtime validation matches TypeScript types
- API schema generated from types
- Type errors caught at build time

**5. Component Props Type Safety**:
```typescript
interface DocumentChatProps {
  documentId?: string
  documentTitle?: string
}

export function DocumentChat({ documentId, documentTitle }: DocumentChatProps) {
  // Props are fully typed
}
```

### Type Inference Patterns

```typescript
// 1. Automatic return type inference
export const listChats = os.input(z.object({}))
  .handler(() => {
    return []  // Type automatically inferred from handler
  })

// 2. Input type inference
const getORPCClient = createIsomorphicFn()
  .server(() => createRouterClient(router))  // Type inferred from router
  .client(() => createORPCClient(link))

// 3. Loader data typing
const loaderData = await loader()  // Fully typed
```

### End-to-End Type Flow

```
┌─────────────────────────────────────────────────────┐
│ 1. Define Zod Schema                                │
│    export const CreateChatInput = z.object({...})   │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│ 2. oRPC Handler (Server)                            │
│    export const createChat = os.input(Schema)       │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│ 3. Router Registration                              │
│    export default { createChat }                    │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│ 4. Type Registration in Module Declaration          │
│    export type RouterClient<typeof router>          │
└──────────────┬──────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────┐
│ 5. Client Usage (Fully Typed!)                      │
│    const result = client.createChat({...})          │
│    // Type errors if wrong shape                    │
└─────────────────────────────────────────────────────┘
```

### Type Checking at Build Time

The build process includes:
1. TypeScript type checking (no emit, just validation)
2. Biome linting and formatting verification
3. Route tree type generation
4. Exported types validation

---

## 8. Advanced Patterns and Best Practices

### Streaming HTML with Data

TanStack Start supports streaming both HTML and data:

```typescript
// Server render streams HTML incrementally
const stream = await router.server.renderToStream(
  <StartServer router={router} />,
  { req, ctx },
)

// Data can be serialized into HTML
// Client hydrates with same data
```

This enables:
- Faster First Byte (critical HTML arrives faster)
- Progressive rendering (user sees content sooner)
- Fallback UI during async operations (Suspense)

### Code Splitting Strategy

Routes are automatically code-split:
- Each route is a separate chunk
- Preloading on hover/intent (configurable)
- Parallel downloads for better performance

### Default Preload Configuration

```typescript
export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',  // Preload on mouse enter
})
```

Options:
- `'intent'`: Preload when user shows intent (hover)
- `'render'`: Preload when route renders
- `false`: No preloading

### Error Handling

Components can define error boundaries:

```typescript
export const Route = createFileRoute('/post/$postId')({
  errorComponent: ErrorComponent,
  notFoundComponent: NotFoundComponent,
})

function ErrorComponent() {
  return <div>An error occurred</div>
}
```

---

## 9. Complete Data Flow Example

### Request Lifecycle (base-merged project)

```
1. User visits http://localhost:3000/chat

2. SERVER:
   - HTTP GET /chat arrives at entry-server.tsx
   - Router matches to src/routes/chat.tsx
   - Component: ChatComponent = () => <DocumentChat />

3. RENDERING:
   - React renders: <StartServer router={router} />
   - Components render including ChatComponent
   - HTML and CSS serialized
   
4. STREAMING:
   - Response starts streaming HTML
   - Browser renders partial page (no interactivity yet)
   - Client bundle JS starts downloading
   
5. CLIENT:
   - HTML fully received
   - Browser parses and renders HTML
   - entry-client.tsx executes
   - hydrateRoot() attaches React to #root
   - Event listeners attached to buttons, inputs
   
6. INTERACTION:
   - User types in chat input
   - handleInputChange fires
   - useChat hook from @ai-sdk/react manages state
   - handleSubmit sends POST to /api/chat
   
7. API CALL:
   - POST /api/chat
   - Routes to src/routes/api/chat.ts handler
   - streamText() from ai SDK calls anthropic()
   - Response streams back as Server-Sent Events
   
8. UI UPDATE:
   - Messages array updates in state
   - Component re-renders
   - New messages appear in DOM
   - Scroll to bottom effect triggers

9. SUBSEQUENT NAVIGATION:
   - User clicks /chat link
   - TanStack Router matches route (client-side)
   - No full page reload
   - Only necessary components re-render
```

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│                     HTTP Request                        │
│                   GET /chat?id=123                      │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│          Router Matching (File Route)                   │
│         src/routes/chat.tsx                             │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│       Loader Execution (beforeLoad)                     │
│  • Check authentication                                 │
│  • Load user preferences                                │
│  • Query initial data                                   │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│          Component Rendering                            │
│    React.renderToString(<StartServer />)                │
│    Generates HTML string                                │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│       Serialization & Streaming                         │
│  • HTML chunks sent to browser                          │
│  • Metadata embedded in <script>                        │
│  • Client JS bundle referenced                          │
└──────────────┬──────────────────────────────────────────┘
               │
       (Network)
               │
┌──────────────▼──────────────────────────────────────────┐
│         Browser HTML Parsing                            │
│    DOM tree constructed                                 │
│    CSS applied                                          │
│    Initial render visible                              │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│        JavaScript Execution                             │
│  1. entry-client.tsx loads                              │
│  2. Router initialized                                  │
│  3. hydrateRoot() called                                │
│  4. Event listeners attached                            │
│  5. Interactive UI ready                                │
└─────────────────────────────────────────────────────────┘
```

---

## 10. Key Takeaways for Developers

### Mental Model to Understand TanStack Start

Think of it as:
- **Not quite a frontend framework** (has server responsibilities)
- **Not quite a backend framework** (focused on React)
- **A full-stack React framework** with the server/client boundary managed automatically

### Key Concepts to Master

1. **File-based routing** = Convention-based organization
2. **Entry points** = Two different execution environments
3. **Isomorphic functions** = Same code, different implementations
4. **Type-driven development** = Types flow end-to-end
5. **Streaming** = Progressive rendering for performance
6. **Handlers and loaders** = Server-side logic co-located with routes

### Architecture Decision Checklist

When building features in TanStack Start:

```
□ Is this route/page?          → Use file route in src/routes/
□ Is this an API endpoint?     → Use src/routes/api/ + handler
□ Does it need data loading?   → Use beforeLoad (loader)
□ Is it a mutation?            → Use server handler + form submission
□ Is it client state?          → Use React state or TanStack Store
□ Is it server state?          → Use TanStack Query or Convex
□ Is it sensitive (auth)?      → Keep on server, use isomorphic function
□ Does it need streaming?      → Use async iterator, Response stream
□ Is it an external service?   → Add to integrations/ provider
□ Does it need type safety?    → Use Zod schemas + oRPC
```

---

## Conclusion

TanStack Start represents a paradigm shift in full-stack React development by:

1. **Eliminating boilerplate**: One framework handles routing, rendering, data, and APIs
2. **Improving type safety**: End-to-end TypeScript without gaps
3. **Leveraging modern standards**: Uses standards-based APIs (fetch, streams, etc.)
4. **Enabling progressive enhancement**: Server-rendered base with client enhancements
5. **Providing excellent DX**: Clear conventions, sensible defaults, automatic code generation

The architecture is designed to make the server/client boundary feel seamless while keeping developers aware of the implications (database access, secrets, performance, etc.).

