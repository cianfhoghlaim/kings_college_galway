# TanStack Start: Comprehensive Research Report
## Patterns, Conventions, and Best Practices

**Document Date:** November 18, 2025  
**Research Scope:** Very Thorough  
**Data Sources:** Default TanStack Start project, examples, and comprehensive guides

---

## Executive Summary

TanStack Start is a full-stack meta-framework built on React and TanStack Router, designed for next-generation web applications. It provides seamless server-client integration through server functions, built-in SSR capabilities, and type-safe API patterns. This report details 7 key areas of patterns and conventions found in the codebase.

---

## 1. FILE STRUCTURE AND ORGANIZATION PATTERNS

### Directory Hierarchy

```
src/
├── routes/                          # File-based routing (auto-generated route tree)
│   ├── __root.tsx                   # Root layout component
│   ├── index.tsx                    # Home page
│   ├── demo/
│   │   ├── start.server-funcs.tsx   # Server function examples
│   │   ├── start.api-request.tsx    # API request patterns
│   │   ├── start.ssr.*.tsx          # SSR variations
│   │   └── ...
│   ├── example.guitars/
│   │   ├── index.tsx                # List view
│   │   └── $guitarId.tsx            # Dynamic route
│   ├── api.$.ts                     # Catch-all API route
│   └── api.rpc.$.ts                 # RPC endpoint
├── components/                      # React components
│   ├── Header.tsx                   # Main navigation header
│   ├── example-AIAssistant.tsx      # AI integration examples
│   └── example-GuitarRecommendation.tsx
├── lib/                             # Utilities and helpers
│   ├── demo-store.ts                # TanStack Store setup
│   ├── demo-store-devtools.tsx      # DevTools integration
│   └── utils.ts                     # Utility functions
├── integrations/                    # Third-party integrations
│   ├── tanstack-query/
│   │   ├── root-provider.tsx        # React Query setup
│   │   └── devtools.tsx
│   └── convex/
│       └── provider.tsx
├── data/                            # Static data
│   └── example-guitars.ts
├── db/                              # Database layer
│   ├── schema.ts                    # Drizzle schema
│   └── index.ts
├── orpc/                            # oRPC router definitions
│   ├── router.ts
│   └── schema.ts
├── router.tsx                       # Router initialization
├── routeTree.gen.ts                 # AUTO-GENERATED route tree (DO NOT EDIT)
├── styles.css                       # Global styles
└── polyfill.ts                      # Node.js polyfills for server context
```

### File Naming Conventions

1. **Route Files**: Use lowercase with dots for nested paths
   - `index.tsx` = root path
   - `demo.tsx` = `/demo`
   - `demo.page.tsx` = `/demo/page`
   - `$param.tsx` = dynamic segment (NOT `[param].tsx`)
   - `$.tsx` = catch-all route

2. **Component Files**: Use PascalCase
   - `Header.tsx`, `GuitarCard.tsx`, `AIAssistant.tsx`

3. **Utility Files**: Use camelCase with descriptive names
   - `demo-store.ts`, `demo-store-devtools.tsx`

### Key Organization Principles

**Reference Files:**
- `/home/user/hackathon/web/base/default_tanstack_start/src/` (Main structure)
- `/home/user/hackathon/web/base/default_tanstack_start/vite.config.ts` (Build config)

```typescript
// vite.config.ts - Shows how plugins work together
import { defineConfig } from 'vite'
import { devtools } from '@tanstack/devtools-vite'
import { tanstackStart } from '@tanstack/react-start/plugin/vite'
import viteReact from '@vitejs/plugin-react'
import viteTsConfigPaths from 'vite-tsconfig-paths'
import tailwindcss from '@tailwindcss/vite'

const config = defineConfig({
  plugins: [
    devtools(),
    viteTsConfigPaths({ projects: ['./tsconfig.json'] }),
    tailwindcss(),
    tanstackStart(),
    viteReact({
      babel: { plugins: ['babel-plugin-react-compiler'] },
    }),
  ],
})
```

---

## 2. ROUTING PATTERNS AND CONVENTIONS

### File-Based Routing Fundamentals

TanStack Start uses **automatic file-based routing** with the @tanstack/router-plugin. Route files are automatically discovered and compiled into a route tree.

#### Basic Route Definition

**Pattern: Simple Page Route**

```typescript
// src/routes/index.tsx
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: App,
})

function App() {
  return <div>Home Page</div>
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/index.tsx`

#### Dynamic Routes (Parameters)

**Pattern: Route with Dynamic Segments**

```typescript
// src/routes/example.guitars/$guitarId.tsx
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/example/guitars/$guitarId')({
  component: RouteComponent,
  loader: async ({ params }) => {
    const guitar = guitars.find((g) => g.id === +params.guitarId)
    if (!guitar) throw new Error('Guitar not found')
    return guitar
  },
})

function RouteComponent() {
  const guitar = Route.useLoaderData()
  const { guitarId } = Route.useParams()
  return <div>{guitar.name}</div>
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/example.guitars/$guitarId.tsx`

**Key Points:**
- File path: `$guitarId.tsx` automatically creates a dynamic route
- Parameters accessed via `Route.useParams()` hook
- TypeScript automatically infers types from route definition

#### Catch-All Routes

**Pattern: Dynamic Catch-All API Route**

```typescript
// src/routes/api.$.ts
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/api/$')({
  server: {
    handlers: {
      GET: async ({ request }) => {
        // Handle all /api/* requests
        return new Response('Not Found', { status: 404 })
      },
    },
  },
})
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/api.$.ts`

#### Root Layout

**Pattern: Root Route with Context**

```typescript
// src/routes/__root.tsx
import { createRootRouteWithContext } from '@tanstack/react-router'
import { HeadContent, Scripts } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'

interface MyRouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<MyRouterContext>()({
  head: () => ({
    meta: [
      { charSet: 'utf-8' },
      { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      { title: 'TanStack Start Starter' },
    ],
    links: [{ rel: 'stylesheet', href: appCss }],
  }),
  shellComponent: RootDocument,
})

function RootDocument({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head><HeadContent /></head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/__root.tsx`

### Link Navigation

**Pattern: Type-Safe Links with Parameters**

```typescript
import { Link } from '@tanstack/react-router'

// Simple link
<Link to="/">Home</Link>

// Link with parameters
<Link 
  to="/example/guitars/$guitarId" 
  params={{ guitarId: '1' }}
>
  View Guitar
</Link>

// Link with search parameters
<Link 
  to="/posts" 
  search={{ offset: 0, limit: 10 }}
>
  Posts
</Link>

// Active state styling
<Link
  to="/home"
  activeProps={{ className: 'font-bold text-blue-600' }}
  inactiveProps={{ className: 'text-gray-600' }}
>
  Home
</Link>
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/components/Header.tsx` (lines 67-95)

### Router Context and Navigation

**Pattern: Router with Context and Navigation**

```typescript
// src/router.tsx - Router initialization with QueryClient
import { createRouter } from '@tanstack/react-router'
import { setupRouterSsrQueryIntegration } from '@tanstack/react-router-ssr-query'
import { routeTree } from './routeTree.gen'

export const getRouter = () => {
  const rqContext = TanstackQuery.getContext()

  const router = createRouter({
    routeTree,
    context: { ...rqContext },
    defaultPreload: 'intent',  // Preload routes on link hover
    Wrap: (props) => (
      <TanstackQuery.Provider {...rqContext}>
        {props.children}
      </TanstackQuery.Provider>
    ),
  })

  setupRouterSsrQueryIntegration({ router, queryClient: rqContext.queryClient })
  return router
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/router.tsx`

**Pattern: Programmatic Navigation**

```typescript
import { useRouter } from '@tanstack/react-router'

function Component() {
  const router = useRouter()

  const onClick = () =>
    router.navigate({
      to: '/posts/$postId',
      params: { postId: 'my-post' },
    })

  const handleMutation = async () => {
    await createTodo({ data: todo })
    router.invalidate()  // Refresh loader data
  }
}
```

---

## 3. DATA LOADING PATTERNS (LOADERS AND ACTIONS)

### Server Functions Pattern

Server functions are RPC-style functions that execute on the server but can be called from client components with full type safety.

**Pattern: Basic Server Function**

```typescript
// src/routes/demo/start.server-funcs.tsx
import { createServerFn } from '@tanstack/react-start'
import fs from 'node:fs'

const TODOS_FILE = 'todos.json'

async function readTodos() {
  return JSON.parse(
    await fs.promises.readFile(TODOS_FILE, 'utf-8').catch(() =>
      JSON.stringify([
        { id: 1, name: 'Get groceries' },
        { id: 2, name: 'Buy a new phone' },
      ])
    ),
  )
}

// Getter function
const getTodos = createServerFn({
  method: 'GET',
}).handler(async () => await readTodos())

// Mutation function
const addTodo = createServerFn({ method: 'POST' })
  .inputValidator((d: string) => d)
  .handler(async ({ data }) => {
    const todos = await readTodos()
    todos.push({ id: todos.length + 1, name: data })
    await fs.promises.writeFile(TODOS_FILE, JSON.stringify(todos, null, 2))
    return todos
  })
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/start.server-funcs.tsx`

### Loaders for Server-Side Data

**Pattern: Loader with Path Parameters**

```typescript
export const Route = createFileRoute('/example/guitars/$guitarId')({
  component: RouteComponent,
  loader: async ({ params }) => {
    const guitar = guitars.find((g) => g.id === +params.guitarId)
    if (!guitar) throw new Error('Guitar not found')
    return guitar
  },
})

function RouteComponent() {
  const guitar = Route.useLoaderData()  // Fully typed!
  return <h1>{guitar.name}</h1>
}
```

**Pattern: Loader with Search Parameters**

```typescript
export const Route = createFileRoute('/posts')({
  validateSearch: (search) =>
    search as {
      offset: number
      limit: number
    },
  loaderDeps: ({ search: { offset, limit } }) => ({ offset, limit }),
  loader: ({ deps: { offset, limit } }) =>
    fetchPosts({ offset, limit }),
})
```

**Pattern: Loader with Abort Signal**

```typescript
export const Route = createFileRoute('/posts')({
  loader: ({ abortController }) =>
    fetchPosts({
      signal: abortController.signal,
    }),
})
```

### Loader with Database Integration

**Pattern: Drizzle ORM Loader**

```typescript
// src/routes/demo/drizzle.tsx
import { createServerFn } from '@tanstack/react-start'
import { db } from '@/db'
import { todos } from '@/db/schema'

const getTodos = createServerFn({
  method: 'GET',
}).handler(async () => {
  return await db.query.todos.findMany({
    orderBy: [desc(todos.createdAt)],
  })
})

export const Route = createFileRoute('/demo/drizzle')({
  component: DemoDrizzle,
  loader: async () => await getTodos(),
})
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/drizzle.tsx`

### Server Routes (API Endpoints)

**Pattern: Server Route with Multiple Methods**

```typescript
// src/routes/api/posts.ts
import { createFileRoute, json } from '@tanstack/react-start'

export const Route = createFileRoute('/api/posts')({
  server: {
    handlers: {
      GET: async ({ request }) => {
        const posts = await getAllPosts()
        return json(posts)
      },
      POST: async ({ request }) => {
        const body = await request.json()
        const post = await createPost(body)
        return json(post, { status: 201 })
      },
    },
  },
})
```

**Pattern: Server Route with Dynamic Segments**

```typescript
export const Route = createFileRoute('/users/$id/posts/$postId')({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const { id, postId } = params
        return json({
          userId: id,
          postId: postId,
        })
      },
    },
  },
})
```

### Loader with TanStack Query Integration

**Pattern: Server Function with TanStack Query**

```typescript
import { useServerFn } from '@tanstack/react-start'
import { useQuery } from '@tanstack/react-query'

export function Time() {
  const getTime = useServerFn(getServerTime)

  const timeQuery = useQuery({
    queryKey: 'time',
    queryFn: () => getTime(),
  })
}
```

### Deferred Data with Streaming

**Pattern: Streaming Response from Server**

```typescript
import { createServerFn } from '@tanstack/react-start'

export const streamEvents = createServerFn({
  method: 'GET',
  response: 'raw',
}).handler(async ({ signal }) => {
  const stream = new ReadableStream({
    async start(controller) {
      controller.enqueue(new TextEncoder().encode('Connection established\n'))

      let count = 0
      const interval = setInterval(() => {
        if (signal.aborted) {
          clearInterval(interval)
          controller.close()
          return
        }

        controller.enqueue(
          new TextEncoder().encode(`Event ${++count}: ${new Date().toISOString()}\n`)
        )

        if (count >= 10) {
          clearInterval(interval)
          controller.close()
        }
      }, 1000)

      signal.addEventListener('abort', () => {
        clearInterval(interval)
        controller.close()
      })
    },
  })

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
    },
  })
})
```

---

## 4. STATE MANAGEMENT PATTERNS

### TanStack Store Integration

**Pattern: Global State with TanStack Store**

```typescript
// src/lib/demo-store.ts
import { Derived, Store } from '@tanstack/store'

export const store = new Store({
  firstName: 'Jane',
  lastName: 'Smith',
})

export const fullName = new Derived({
  fn: () => `${store.state.firstName} ${store.state.lastName}`,
  deps: [store],
})

fullName.mount()
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/lib/demo-store.ts`

**Pattern: Using Store in Components**

```typescript
// src/routes/demo/store.tsx
import { useStore } from '@tanstack/react-store'
import { fullName, store } from '@/lib/demo-store'

function FirstName() {
  const firstName = useStore(store, (state) => state.firstName)
  return (
    <input
      type="text"
      value={firstName}
      onChange={(e) =>
        store.setState((state) => ({ ...state, firstName: e.target.value }))
      }
    />
  )
}

function FullName() {
  const fName = useStore(fullName)
  return <div>{fName}</div>
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/store.tsx`

### TanStack Query Integration

**Pattern: React Query with Router Integration**

```typescript
// src/integrations/tanstack-query/root-provider.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

export function getContext() {
  const queryClient = new QueryClient()
  return { queryClient }
}

export function Provider({
  children,
  queryClient,
}: {
  children: React.ReactNode
  queryClient: QueryClient
}) {
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/integrations/tanstack-query/root-provider.tsx`

**Pattern: Query with TanStack Query**

```typescript
// src/routes/demo/tanstack-query.tsx
import { useQuery, useMutation } from '@tanstack/react-query'

function TanStackQueryDemo() {
  const { data, refetch } = useQuery<Todo[]>({
    queryKey: ['todos'],
    queryFn: () => fetch('/demo/api/tq-todos').then((res) => res.json()),
    initialData: [],
  })

  const { mutate: addTodo } = useMutation({
    mutationFn: (todo: string) =>
      fetch('/demo/api/tq-todos', {
        method: 'POST',
        body: JSON.stringify(todo),
      }).then((res) => res.json()),
    onSuccess: () => refetch(),
  })

  const handleSubmit = async () => {
    await addTodo(todo)
    setTodo('')
  }

  return (
    <div>
      <ul>{data?.map((t) => <li key={t.id}>{t.name}</li>)}</ul>
    </div>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/tanstack-query.tsx`

### Convex Backend Integration

**Pattern: Convex Queries and Mutations**

```typescript
// src/routes/demo/convex.tsx
import { useCallback, useState } from 'react'
import { useQuery, useMutation } from 'convex/react'
import { api } from '../../../convex/_generated/api'

function ConvexTodos() {
  const todos = useQuery(api.todos.list)
  const addTodo = useMutation(api.todos.add)
  const toggleTodo = useMutation(api.todos.toggle)
  const removeTodo = useMutation(api.todos.remove)

  const [newTodo, setNewTodo] = useState('')

  const handleAddTodo = useCallback(async () => {
    if (newTodo.trim()) {
      await addTodo({ text: newTodo.trim() })
      setNewTodo('')
    }
  }, [addTodo, newTodo])

  return (
    <div>
      <ul>
        {todos?.map((todo) => (
          <li key={todo._id}>
            {todo.text}
            <button onClick={() => toggleTodo({ id: todo._id })}>
              Toggle
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/convex.tsx`

### Router Invalidation for Mutations

**Pattern: Invalidating Loader Data After Mutation**

```typescript
import { useRouter } from '@tanstack/react-router'

function Home() {
  const router = useRouter()

  const submitTodo = useCallback(async () => {
    const todos = await addTodo({ data: todo })
    setTodo('')
    router.invalidate()  // Refresh all loader data
  }, [addTodo, todo])

  return (
    <button onClick={submitTodo}>Add todo</button>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/start.server-funcs.tsx` (lines 59-63)

---

## 5. ERROR HANDLING PATTERNS

### Route Error Boundaries

**Pattern: Error Component with Reset**

```typescript
export const Route = createFileRoute('/posts')({
  loader: () => fetchPosts(),
  errorComponent: ({ error, reset }) => {
    return (
      <div>
        <p>Error: {error.message}</p>
        <button onClick={() => reset()}>Retry</button>
      </div>
    )
  },
  component: PageComponent,
})
```

### Error Handling in Sentry Integration

**Pattern: Sentry Integration with Error Boundary**

```typescript
// src/routes/demo/sentry.testing.tsx
import * as Sentry from '@sentry/tanstackstart-react'

export const Route = createFileRoute('/demo/sentry/testing')({
  component: RouteComponent,
  errorComponent: ({ error }) => {
    useEffect(() => {
      Sentry.captureException(error)
    }, [error])
    return <div>Error: {error.message}</div>
  },
})

// Server function with error tracking
const badServerFunc = createServerFn({
  method: 'GET',
}).handler(async () => {
  return await Sentry.startSpan(
    {
      name: 'Reading non-existent file',
      op: 'file.read',
    },
    async () => {
      try {
        await fs.readFile('./doesnt-exist', 'utf-8')
        return true
      } catch (error) {
        Sentry.captureException(error)
        throw error
      }
    },
  )
})
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/sentry.testing.tsx`

### Error Handling in Server Functions

**Pattern: Try-Catch in Server Functions**

```typescript
const createTodo = createServerFn({
  method: 'POST',
})
  .inputValidator((data: { title: string }) => data)
  .handler(async ({ data }) => {
    try {
      await db.insert(todos).values({ title: data.title })
      return { success: true }
    } catch (error) {
      console.error('Failed to create todo:', error)
      throw error
    }
  })

function DemoDrizzle() {
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.target as HTMLFormElement)
    const title = formData.get('title') as string

    try {
      await createTodo({ data: { title } })
      router.invalidate()
      ;(e.target as HTMLFormElement).reset()
    } catch (error) {
      console.error('Failed to create todo:', error)
    }
  }
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/drizzle.tsx`

### Loader Error Handling with Not Found

**Pattern: Throwing Error in Loader**

```typescript
export const Route = createFileRoute('/example/guitars/$guitarId')({
  component: RouteComponent,
  loader: async ({ params }) => {
    const guitar = guitars.find((guitar) => guitar.id === +params.guitarId)
    if (!guitar) {
      throw new Error('Guitar not found')
    }
    return guitar
  },
})
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/example.guitars/$guitarId.tsx`

---

## 6. COMMON CODE PATTERNS AND IDIOMS

### Lazy Route Loading

**Pattern: Lazy Component Loading**

```typescript
import { createFileRoute, lazyRouteComponent } from '@tanstack/react-router'

export const Route = createFileRoute('/posts')({
  component: lazyRouteComponent(() => import('./PostList')),
})
```

### Protected/Authenticated Routes

**Pattern: Route with Authentication Guard**

```typescript
export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async ({ location }) => {
    if (!isAuthenticated()) {
      throw redirect({
        to: '/login',
        search: {
          redirect: location.href,
        },
      })
    }
  },
  component: PageComponent,
})

function PageComponent() {
  return <div>Protected Content</div>
}
```

### SSR Modes

**Pattern: Data-Only SSR**

```typescript
// Only server-side data, client-side rendering
export const Route = createFileRoute('/demo/start/ssr/data-only')({
  ssr: 'data-only',
  component: RouteComponent,
  loader: async () => await getPunkSongs(),
})
```

**Pattern: Full SSR**

```typescript
// Full document and component rendered on server
export const Route = createFileRoute('/demo/start/ssr/full-ssr')({
  // ssr: true is default
  component: RouteComponent,
  loader: async () => await getPunkSongs(),
})
```

**Pattern: SPA Mode (No SSR)**

```typescript
export const Route = createFileRoute('/demo/start/ssr/spa-mode')({
  ssr: false,
  beforeLoad: () => {
    console.log('Executes on client during hydration')
  },
  component: () => <div>Client-only rendered</div>,
})
```

### Nested Route Groups

**Pattern: Route Grouping with Dots**

```
Routes are organized as:
src/routes/
├── demo.tsx (path: /demo)
├── demo.page1.tsx (path: /demo/page1)
├── demo.page2.tsx (path: /demo/page2)
└── example.guitars/
    ├── index.tsx (path: /example/guitars/)
    └── $guitarId.tsx (path: /example/guitars/$guitarId)
```

### Form Handling with Server Functions

**Pattern: Form Submission with Validation**

```typescript
const createTodo = createServerFn({ method: 'POST' })
  .inputValidator((data: { title: string }) => data)
  .handler(async ({ data }) => {
    await db.insert(todos).values({ title: data.title })
    return { success: true }
  })

function DemoDrizzle() {
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.target as HTMLFormElement)
    const title = formData.get('title') as string

    if (!title) return

    try {
      await createTodo({ data: { title } })
      router.invalidate()
      ;(e.target as HTMLFormElement).reset()
    } catch (error) {
      console.error('Failed:', error)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input type="text" name="title" required />
      <button type="submit">Add</button>
    </form>
  )
}
```

### API Request Pattern

**Pattern: Direct Fetch API Calls**

```typescript
// src/routes/demo/start.api-request.tsx
import { useQuery } from '@tanstack/react-query'

function getNames() {
  return fetch('/demo/api/names').then((res) => res.json() as Promise<string[]>)
}

export const Route = createFileRoute('/demo/start/api-request')({
  component: Home,
})

function Home() {
  const { data: names = [] } = useQuery({
    queryKey: ['names'],
    queryFn: getNames,
  })

  return (
    <div>
      <ul>
        {names.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
    </div>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/start.api-request.tsx`

### Deferred Loading with Await Component

**Pattern: Streaming Data with Await**

```typescript
import { createFileRoute, Await } from '@tanstack/react-router'
import { defer } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  loader: () => {
    const deferredPromise = defer(fetch('/api/data'))
    return { deferredPromise }
  },
  component: PageComponent,
})

function PageComponent() {
  const { deferredPromise } = Route.useLoaderData()

  return (
    <Await promise={deferredPromise}>
      {(data) => <div>{JSON.stringify(data)}</div>}
    </Await>
  )
}
```

---

## 7. TYPESCRIPT USAGE PATTERNS

### TypeScript Configuration

**Pattern: Strict TypeScript Setup**

```typescript
// tsconfig.json
{
  "compilerOptions": {
    "target": "ES2022",
    "jsx": "react-jsx",
    "module": "ESNext",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "types": ["vite/client"],
    
    /* Type Checking */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": false,
    "noEmit": true,
    
    /* Strict Mode */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true,
    
    /* Path Aliases */
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/tsconfig.json`

### Type-Safe Route Parameters

**Pattern: Inferred Parameter Types**

```typescript
export const Route = createFileRoute('/posts/$postId')({
  loader: ({ params }) => {
    // params.postId is automatically typed as string
    return fetchPost(params.postId)
  },
  component: PostComponent,
})

function PostComponent() {
  // Type is inferred from loader return type
  const post = Route.useLoaderData()
  
  // params are automatically typed
  const { postId } = Route.useParams()
  
  return <div>{post.title}</div>
}
```

### Type-Safe Loaders

**Pattern: Loader Return Type Inference**

```typescript
const getTodos = createServerFn({
  method: 'GET',
}).handler(async () => {
  const todos: Todo[] = await readTodos()
  return todos  // Return type is inferred as Todo[]
})

export const Route = createFileRoute('/todos')({
  loader: async () => await getTodos(),
  component: TodoList,
})

function TodoList() {
  // todos is automatically typed as Todo[]
  const todos = Route.useLoaderData()
  return (
    <ul>
      {todos.map((todo) => (
        <li key={todo.id}>{todo.name}</li>
      ))}
    </ul>
  )
}
```

### Type-Safe Server Functions

**Pattern: Input Validation with Types**

```typescript
import { z } from 'zod'

const PersonSchema = z.object({
  name: z.string(),
  age: z.number(),
})

export const greetPerson = createServerFn({ method: 'POST' })
  .inputValidator((data: unknown) => PersonSchema.parse(data))
  .handler(async ({ data }) => {
    // data is typed as { name: string; age: number }
    return `Hello, ${data.name}!`
  })

// Usage is type-safe
const message = await greetPerson({
  data: { name: 'John', age: 30 },
})
```

### Router Context Types

**Pattern: Typed Router Context**

```typescript
import type { QueryClient } from '@tanstack/react-query'

interface MyRouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<MyRouterContext>()({
  // ...
})

// In other routes, context is typed
export const getRouter = () => {
  const rqContext = TanstackQuery.getContext()

  const router = createRouter({
    routeTree,
    context: { ...rqContext },  // Context is typed
    Wrap: (props) => (
      <TanstackQuery.Provider {...rqContext}>
        {props.children}
      </TanstackQuery.Provider>
    ),
  })
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/router.tsx`

### Type-Safe API Handlers

**Pattern: Typed Server Routes**

```typescript
export const Route = createFileRoute('/api/users')({
  server: {
    handlers: {
      GET: async ({ request }): Promise<Response> => {
        const users: User[] = await getUsers()
        return json(users)
      },
      POST: async ({ request }): Promise<Response> => {
        const body = (await request.json()) as CreateUserInput
        const user = await createUser(body)
        return json(user, { status: 201 })
      },
    },
  },
})
```

### Generic Type Patterns

**Pattern: Generic Component Types**

```typescript
interface RouteComponentProps<T> {
  data: T
  isLoading: boolean
  error?: Error
}

function DataDisplay<T extends Record<string, any>>({
  data,
  isLoading,
}: RouteComponentProps<T>) {
  if (isLoading) return <div>Loading...</div>
  return <pre>{JSON.stringify(data, null, 2)}</pre>
}
```

### Type Guard Patterns

**Pattern: Route Type Guards**

```typescript
function isAuthenticatedRoute(
  route: Route,
): route is Route & { params: { userId: string } } {
  return 'userId' in route.params
}

// Usage
if (isAuthenticatedRoute(currentRoute)) {
  console.log(currentRoute.params.userId)  // Now typed!
}
```

---

## 8. CONFIGURATION AND BUILD PATTERNS

### Vite Configuration

**Pattern: Complete Vite Setup with TanStack Start**

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import { devtools } from '@tanstack/devtools-vite'
import { tanstackStart } from '@tanstack/react-start/plugin/vite'
import viteReact from '@vitejs/plugin-react'
import viteTsConfigPaths from 'vite-tsconfig-paths'
import tailwindcss from '@tailwindcss/vite'
import netlify from '@netlify/vite-plugin-tanstack-start'

const config = defineConfig({
  plugins: [
    devtools(),
    netlify(),
    viteTsConfigPaths({ projects: ['./tsconfig.json'] }),
    tailwindcss(),
    tanstackStart(),
    viteReact({
      babel: { plugins: ['babel-plugin-react-compiler'] },
    }),
  ],
})

export default config
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/vite.config.ts`

### Package.json Scripts

**Pattern: Development and Build Scripts**

```json
{
  "scripts": {
    "dev": "NODE_OPTIONS='--import ./instrument.server.mjs' vite dev --port 3000",
    "build": "vite build && cp instrument.server.mjs .output/server",
    "serve": "vite preview",
    "test": "vitest run",
    "format": "biome format",
    "lint": "biome lint",
    "check": "biome check",
    "start": "node --import ./.output/server/instrument.server.mjs .output/server/index.mjs",
    "db:generate": "drizzle-kit generate",
    "db:migrate": "drizzle-kit migrate",
    "db:push": "drizzle-kit push"
  }
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/package.json`

### Environment Variables

**Pattern: Environment Variable Usage**

```typescript
// Client-side (must use VITE_ prefix)
const apiUrl = import.meta.env.VITE_API_URL

// Server-side (can access process.env)
const dbUrl = process.env.DATABASE_URL

// In server functions
const getSecretData = createServerFn({
  method: 'GET',
}).handler(async () => {
  const apiKey = process.env.SECRET_API_KEY
  return await fetchData(apiKey)
})
```

---

## 9. ADVANCED PATTERNS AND BEST PRACTICES

### oRPC Integration

**Pattern: Type-Safe RPC with oRPC**

```typescript
// src/routes/api.$.ts
import { OpenAPIHandler } from '@orpc/openapi/fetch'
import { createFileRoute } from '@tanstack/react-router'
import router from '@/orpc/router'

const handler = new OpenAPIHandler(router, {
  plugins: [
    // OpenAPI documentation generation
    new OpenAPIReferencePlugin({
      specGenerateOptions: {
        info: { title: 'TanStack API', version: '1.0.0' },
      },
    }),
  ],
})

export const Route = createFileRoute('/api/$')({
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

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/routes/api.$.ts`

### Component Patterns with TanStack Devtools

**Pattern: Header with Dynamic Navigation**

```typescript
// src/components/Header.tsx
import { Link } from '@tanstack/react-router'
import { useState } from 'react'

export default function Header() {
  const [isOpen, setIsOpen] = useState(false)
  const [groupedExpanded, setGroupedExpanded] = useState<
    Record<string, boolean>
  >({})

  return (
    <>
      <header className="p-4 flex items-center bg-gray-800 text-white shadow-lg">
        <button onClick={() => setIsOpen(true)} aria-label="Open menu">
          <Menu size={24} />
        </button>
        <h1 className="ml-4 text-xl font-semibold">
          <Link to="/">
            <img src="/tanstack-logo.svg" alt="Logo" className="h-10" />
          </Link>
        </h1>
      </header>

      <aside className={`fixed top-0 left-0 h-full w-80 z-50 ${isOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        <nav className="flex-1 p-4">
          <Link
            to="/"
            onClick={() => setIsOpen(false)}
            activeProps={{ className: 'bg-cyan-600' }}
          >
            Home
          </Link>
          {/* More navigation items */}
        </nav>
      </aside>
    </>
  )
}
```

**Reference:** `/home/user/hackathon/web/base/default_tanstack_start/src/components/Header.tsx`

### AI Integration Patterns

**Pattern: Server-Side AI Integration**

```typescript
import { createServerFn } from '@tanstack/react-start'
import { ai } from '@ai-sdk/anthropic'

export const generateResponse = createServerFn({ method: 'POST' })
  .inputValidator((data: { prompt: string }) => data)
  .handler(async ({ data, signal }) => {
    const response = await streamText({
      model: ai('claude-3-5-sonnet-20241022'),
      prompt: data.prompt,
      signal,
    })

    return response.toTextStream()
  })
```

---

## 10. KEY FRAMEWORK DEPENDENCIES

```json
{
  "@tanstack/react-router": "^1.132.0",
  "@tanstack/react-start": "^1.132.0",
  "@tanstack/react-query": "^5.66.5",
  "@tanstack/react-store": "^0.7.0",
  "react": "^19.2.0",
  "react-dom": "^19.2.0",
  "vite": "^7.1.7",
  "tailwindcss": "^4.0.6"
}
```

---

## 11. SUMMARY TABLE: PATTERNS AT A GLANCE

| Pattern Category | Key Concepts | File Examples |
|---|---|---|
| **File Structure** | Flat + hierarchical organization, Component/lib/integrations separation | `src/routes/`, `src/components/`, `src/lib/` |
| **Routing** | File-based, `$param` syntax, root layout, catch-all routes | `__root.tsx`, `$guitarId.tsx`, `api.$.ts` |
| **Loaders** | `createServerFn`, path params, search params, abort signals | `start.server-funcs.tsx`, `drizzle.tsx` |
| **Server Functions** | RPC-style, input validators, streaming, abort handling | `demo.start-server-funcs.tsx` |
| **State Management** | TanStack Store, TanStack Query, Convex, Router invalidation | `demo-store.ts`, `tanstack-query.tsx` |
| **Error Handling** | Error boundaries, Sentry integration, error throwing | `sentry.testing.tsx` |
| **SSR Modes** | Full SSR, data-only SSR, SPA mode | `start.ssr.*.tsx` files |
| **TypeScript** | Strict mode, context types, inference, type guards | `router.tsx`, `tsconfig.json` |

---

## 12. RESOURCES AND REFERENCES

**Documentation:**
- `/home/user/hackathon/web/examples/pages-2-tanstack-converter/TanStack-Start-React-Cookbook.md`
- `/home/user/hackathon/web/examples/pages-2-tanstack-converter/NextJS-Pages-Router-To-TanStack-Start.md`

**Example Projects:**
- `/home/user/hackathon/web/base/default_tanstack_start/`
- `/home/user/hackathon/web/examples/tanstack-better-auth/`
- `/home/user/hackathon/web/examples/react-compiler-on-tanstack/`

**Key Files:**
- Router config: `/home/user/hackathon/web/base/default_tanstack_start/src/router.tsx`
- Root route: `/home/user/hackathon/web/base/default_tanstack_start/src/routes/__root.tsx`
- Demo routes: `/home/user/hackathon/web/base/default_tanstack_start/src/routes/demo/`

---

**Report Compiled By:** Claude Code Research Assistant  
**Date:** November 18, 2025  
**Thoroughness Level:** Very Thorough (covering all major patterns with examples)
