# Pangolin Project: Patterns and Ontologies Deep Dive

## Overview
The Pangolin project is a comprehensive hackathon monorepo featuring:
- **base-merged**: Production-ready full-stack application
- **web/base**: Reusable base templates (TanStack Start, Convex, Chef)
- **web/examples**: 13+ example applications demonstrating various patterns
- **data-unified**: Data pipeline infrastructure with Python models
- **infrastructure**: Deployment infrastructure (Pulumi, Dagger, Docker)

---

## 1. CODE PATTERNS AND CONVENTIONS

### 1.1 File Organization Structure

#### Frontend (TypeScript/React)
```
src/
├── routes/                 # File-based routing (TanStack Router)
│   ├── __root.tsx        # Root layout component
│   ├── index.tsx         # Home page
│   ├── chat.tsx          # Chat feature page
│   └── api/              # API routes
│       ├── chat.ts       # Chat API endpoint
│       └── rpc.$.ts      # oRPC catch-all handler
├── components/           # React components
│   ├── ChatMessage.tsx   # Presentational components
│   └── DocumentChat.tsx  # Feature components
├── orpc/                 # Type-safe RPC layer
│   ├── router/          # Backend handlers
│   │   ├── chat.ts
│   │   ├── documents.ts
│   │   └── index.ts     # Router aggregation
│   ├── schema.ts        # Zod validation schemas
│   └── client.ts        # Isomorphic client
├── lib/                 # Utilities
│   ├── auth.ts         # Auth server setup
│   ├── auth-client.ts  # Auth client hooks
│   └── utils.ts        # General utilities
├── db/                 # Database layer
│   ├── schema.ts       # Drizzle ORM schema
│   └── index.ts        # DB instance
└── integrations/       # Provider integrations
    ├── convex/provider.tsx
    └── tanstack-query/provider.tsx
```

#### Backend (Convex)
```
convex/
├── schema.ts           # Table definitions with Convex types
├── chat.ts            # Convex mutations and queries
├── auth.config.ts     # Authentication configuration
├── convex.config.ts   # Convex configuration
└── _generated/        # Auto-generated types
    ├── api.d.ts
    ├── server.d.ts
    └── dataModel.d.ts
```

### 1.2 Naming Conventions

#### Files
- **Components**: PascalCase (e.g., `DocumentChat.tsx`, `ChatMessage.tsx`)
- **Pages**: kebab-case in routes (e.g., `index.tsx`, `chat.tsx`)
- **API routes**: kebab-case (e.g., `rpc.$.ts`, `chat.ts`)
- **Utilities**: camelCase (e.g., `auth-client.ts`, `utils.ts`)
- **Schemas**: Suffixed with "Schema" (e.g., `ChatSchema`, `MessageSchema`)

#### Variables & Functions
- **React Components**: PascalCase (e.g., `ChatMessage`, `DocumentChat`)
- **Custom Hooks**: `use` prefix (e.g., `useAuth()`, `useChat()`)
- **Database Tables**: Plural nouns (e.g., `chatSessions`, `messages`, `documents`)
- **Exports**: Named exports for utilities, default exports for components
- **Types**: PascalCase interfaces/types (e.g., `ChatMessageProps`, `DocumentChatProps`)

#### Constants
- **Validation Schemas**: Exported Zod objects (e.g., `MessageSchema`, `ChatSchema`)
- **Database Getters**: Prefixed with `get` (e.g., `getDB()`)
- **State Stores**: `new Store()` from @tanstack/react-store

### 1.3 Code Style Configuration

**Formatter: Biomejs**
```json
- indentStyle: space
- quoteStyle: single
- semicolons: asNeeded
- organizeImports: enabled
- formatOnSave: enforced
```

**TypeScript Configuration**
```json
- strict: true
- target: ES2022
- jsx: react-jsx
- moduleResolution: bundler
- baseUrl with path aliases (~/* → ./src/*)
```

---

## 2. ONTOLOGIES AND DATA MODELS

### 2.1 Domain Model: Chat & Document System

#### Core Entities

**User (Authentication)**
```typescript
// Drizzle ORM SQLite Schema
{
  id: text (primaryKey)
  name: text (notNull)
  email: text (notNull, unique)
  emailVerified: boolean (default: false)
  image: text (optional)
  createdAt: timestamp
  updatedAt: timestamp
}
```

**Chat Session**
```typescript
// Convex Schema
{
  userId: string (indexed)
  title: string
  isActive: boolean (indexed)
}

// Drizzle Schema
{
  id: text (primaryKey)
  userId: text (references user, cascade)
  title: text
  createdAt: timestamp
  updatedAt: timestamp
}
```

**Message**
```typescript
// Convex Schema
{
  sessionId: id<'chatSessions'>
  userId: string (optional)
  content: string
  role: 'user' | 'assistant' | 'system'
  metadata?: { sources?: string[] }
}

// Drizzle Schema
{
  id: text (primaryKey)
  chatId: text (references chat, cascade)
  role: 'user' | 'assistant' | 'system'
  content: text
  createdAt: timestamp
}
```

**Document**
```typescript
// Convex Schema
{
  userId: string
  title: string
  content: string
  documentChunks?: array (indexed)
}

// Drizzle Schema
{
  id: text (primaryKey)
  userId: text (references user, cascade)
  title: text
  content: text
  createdAt: timestamp
  updatedAt: timestamp
}
```

**Authentication Records**
```typescript
Session {
  id: text (primaryKey)
  expiresAt: timestamp
  token: text (unique)
  userId: text (references user, cascade)
  ipAddress: text
  userAgent: text
}

Account {
  id: text (primaryKey)
  userId: text (references user, cascade)
  accountId: text
  providerId: text ('google' | 'github')
  accessToken: text
  refreshToken: text
  accessTokenExpiresAt: timestamp
}

Verification {
  id: text (primaryKey)
  identifier: text
  value: text
  expiresAt: timestamp
}
```

### 2.2 Validation Schemas (Zod)

#### Request/Response Schemas
```typescript
// Core entity schemas
const MessageSchema = z.object({
  id: z.string(),
  chatId: z.string(),
  role: z.enum(['user', 'assistant', 'system']),
  content: z.string(),
  createdAt: z.number(),
})

const ChatSchema = z.object({
  id: z.string(),
  userId: z.string(),
  title: z.string(),
  createdAt: z.number(),
  updatedAt: z.number(),
})

const DocumentSchema = z.object({
  id: z.string(),
  userId: z.string(),
  title: z.string(),
  content: z.string(),
  createdAt: z.number(),
  updatedAt: z.number(),
})

// Input schemas for mutations
const CreateChatInput = z.object({
  title: z.string(),
})

const CreateMessageInput = z.object({
  chatId: z.string(),
  content: z.string(),
})

const CreateDocumentInput = z.object({
  title: z.string(),
  content: z.string(),
})
```

#### Convex Value Schemas
```typescript
const updateBoardSchema = v.object({
  id: board.fields.id,
  name: v.optional(board.fields.name),
  color: v.optional(v.string()),
})

// Reusing table validators for type inference
export type Board = Infer<typeof board>
export type Column = Infer<typeof column>
export type Item = Infer<typeof item>
```

### 2.3 Type Definitions

#### Component Props
```typescript
interface ChatMessageProps {
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: string[]
}

interface DocumentChatProps {
  documentId?: string
  documentTitle?: string
}

interface Message {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: string[]
}
```

#### API Response Types
```typescript
type DB = ReturnType<typeof getDB>
type Auth = ReturnType<typeof createAuth>
type RouterClient<T> = ReturnType<typeof createORPCClient>
```

### 2.4 Relationship Patterns

**User → Chat Sessions** (One-to-Many)
- Constraint: `FOREIGN KEY (userId) REFERENCES user(id) ON DELETE CASCADE`
- Index: `by_user` on `userId`

**Chat Session → Messages** (One-to-Many)
- Constraint: `FOREIGN KEY (chatId) REFERENCES chat(id) ON DELETE CASCADE`
- Index: `by_session` on `sessionId`

**User → Documents** (One-to-Many)
- Constraint: `FOREIGN KEY (userId) REFERENCES user(id) ON DELETE CASCADE`
- Index: `by_user` on `userId`

**Document → Document Chunks** (One-to-Many)
- Constraint: `FOREIGN KEY (documentId) REFERENCES documents(_id) ON DELETE CASCADE`
- Index: `by_document` on `documentId`
- Search Index: Full-text search on content

---

## 3. DESIGN PATTERNS

### 3.1 API Layer Pattern (oRPC)

#### Server-Side Handler Pattern
```typescript
// Using oRPC server
import { os } from '@orpc/server'
import * as z from 'zod'

export const createChat = os
  .input(CreateChatInput)
  .handler(({ input }) => {
    // Type-safe input from Zod schema
    return {
      id: crypto.randomUUID(),
      userId: 'temp-user',
      title: input.title,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    }
  })

// Router aggregation
export default {
  listChats,
  createChat,
  getChatMessages,
  createMessage,
  listDocuments,
  createDocument,
  getDocument,
}
```

**Pattern Properties:**
- Input validation via Zod schema chaining
- Handler receives typed input
- Auto-generates TypeScript types
- Works with Fetch API, HTTP, and SSR

#### Client-Side Integration Pattern
```typescript
// Isomorphic client setup
const getORPCClient = createIsomorphicFn()
  .server(() =>
    createRouterClient(router, {
      context: () => ({
        headers: getRequestHeaders(),
      }),
    }),
  )
  .client((): RouterClient<typeof router> => {
    const link = new RPCLink({
      url: `${window.location.origin}/api/rpc`,
    })
    return createORPCClient(link)
  })

// TanStack Query utilities
export const orpc = createTanstackQueryUtils(client)
```

### 3.2 Backend Query/Mutation Pattern (Convex)

#### Mutation Pattern
```typescript
export const createSession = mutation({
  args: {
    title: v.string(),
  },
  handler: async (ctx, args) => {
    // Authentication check
    const userId = await ctx.auth.getUserIdentity()
    if (!userId) {
      throw new Error('Not authenticated')
    }

    // Database insert
    return await ctx.db.insert('chatSessions', {
      userId: userId.subject,
      title: args.title,
      isActive: true,
    })
  },
})
```

#### Query Pattern with Validation
```typescript
export const getMessages = query({
  args: { sessionId: v.id('chatSessions') },
  handler: async (ctx, args) => {
    // Authentication
    const userId = await ctx.auth.getUserIdentity()
    if (!userId) {
      throw new Error('Not authenticated')
    }

    // Authorization - ownership check
    const session = await ctx.db.get(args.sessionId)
    if (!session || session.userId !== userId.subject) {
      throw new Error('Unauthorized')
    }

    // Query with index
    return await ctx.db
      .query('messages')
      .withIndex('by_session', (q) => q.eq('sessionId', args.sessionId))
      .collect()
  },
})
```

**Pattern Properties:**
- Type-safe args validation with `v` schema
- Automatic authentication via `ctx.auth`
- Authorization via ownership verification
- Query optimization with indexes
- Cascading deletions via schema constraints

#### Helper Validation Pattern
```typescript
// Utility for ensuring entity exists
async function ensureBoardExists(
  ctx: QueryCtx,
  boardId: string,
): Promise<Doc<'boards'>> {
  const board = await ctx.db
    .query('boards')
    .withIndex('id', (q) => q.eq('id', boardId))
    .unique()

  invariant(board, `missing board ${boardId}`)
  return board
}
```

### 3.3 React Component Pattern

#### Controlled Form Component
```typescript
interface DocumentChatProps {
  documentId?: string
  documentTitle?: string
}

export function DocumentChat({
  documentId,
  documentTitle = 'Document',
}: DocumentChatProps) {
  const [localMessages, setLocalMessages] = useState<Message[]>([])
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const { messages, input, handleInputChange, handleSubmit, isLoading } =
    useChat({
      api: '/api/chat',
      body: { documentId },
      onFinish: (message) => {
        setLocalMessages((prev) => [...prev, {
          id: message.id,
          role: message.role as 'assistant',
          content: message.content,
        }])
      },
    })

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      {/* Messages List */}
      {/* Input Form */}
    </div>
  )
}
```

**Pattern Properties:**
- Props interface at component top
- useState for local state
- useRef for DOM manipulation
- useEffect for side effects
- useChat hook for streaming responses
- Composition over inheritance
- Functional component (FC pattern not used)

#### Presentational Component
```typescript
interface ChatMessageProps {
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: string[]
}

export function ChatMessage({ role, content, sources }: ChatMessageProps) {
  const isUser = role === 'user'

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} py-3`}>
      <div className="flex items-start gap-3 max-w-3xl">
        {!isUser && <AvatarBadge />}
        <div className={`px-4 py-3 rounded-lg ${getMessageClasses(isUser)}`}>
          {isUser ? (
            <PlainText content={content} />
          ) : (
            <MarkdownContent content={content} />
          )}
          {sources && <SourceLinks sources={sources} />}
        </div>
        {isUser && <UserAvatarBadge />}
      </div>
    </div>
  )
}
```

**Pattern Properties:**
- Pure presentational (no data fetching)
- Props-driven rendering
- Conditional rendering for different roles
- Markdown rendering for assistant messages
- Optional sources display

### 3.4 State Management Pattern

#### TanStack Store Pattern
```typescript
// Global state using TanStack Store
export const showAIAssistant = new Store(false)

// Usage in component
const isOpen = useStore(showAIAssistant)

// Toggle
onClick={() => showAIAssistant.setState((state) => !state)}
```

#### TanStack Query Pattern (with Convex)
```typescript
// Using convexQuery utilities
const { data: board } = useSuspenseQuery(
  convexQuery(api.board.getBoard, { id: boardId }),
)

// Mutation
const updateBoardMutation = useUpdateBoardMutation()

// In component
updateBoardMutation.mutate({
  id: board.id,
  name: value,
})

// Optimistic updates
value={
  updateBoardMutation.isPending && updateBoardMutation.variables.name
    ? updateBoardMutation.variables.name
    : board.name
}
```

### 3.5 Root Layout Pattern (TanStack Router)

```typescript
export const Route = createRootRoute({
  component: RootComponent,
})

function RootComponent() {
  return (
    <TanstackQueryProvider>
      <ConvexClientProvider>
        <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
          <Outlet />
        </div>
        <TanStackRouterDevtools position="bottom-right" />
      </ConvexClientProvider>
    </TanstackQueryProvider>
  )
}
```

**Pattern Properties:**
- Provider composition (Query → Convex → Router)
- Outlet for child routes
- Devtools integration
- Wrapper div for styling

### 3.6 Authentication Pattern

#### Server-Side Auth Setup
```typescript
import { betterAuth } from 'better-auth'
import { drizzleAdapter } from 'better-auth/adapters/drizzle'

export function createAuth(d1: D1Database) {
  const db = getDB(d1)

  return betterAuth({
    database: drizzleAdapter(db, {
      provider: 'sqlite',
      schema: {
        user: schema.user,
        session: schema.session,
        account: schema.account,
        verification: schema.verification,
      },
    }),
    emailAndPassword: {
      enabled: true,
      requireEmailVerification: false,
    },
    socialProviders: {
      google: {
        clientId: process.env.GOOGLE_CLIENT_ID || '',
        clientSecret: process.env.GOOGLE_CLIENT_SECRET || '',
        enabled: !!process.env.GOOGLE_CLIENT_ID,
      },
      github: {
        clientId: process.env.GITHUB_CLIENT_ID || '',
        clientSecret: process.env.GITHUB_CLIENT_SECRET || '',
        enabled: !!process.env.GITHUB_CLIENT_ID,
      },
    },
    secret: process.env.BETTER_AUTH_SECRET || 'your-secret-key-change-this',
    baseURL: process.env.BETTER_AUTH_URL || 'http://localhost:3000',
  })
}
```

#### Client-Side Auth Hook
```typescript
import { createAuthClient } from 'better-auth/client'

export const authClient = createAuthClient({
  baseURL: import.meta.env.VITE_BETTER_AUTH_URL || 'http://localhost:3000',
})

export function useAuth() {
  return authClient
}
```

**Pattern Properties:**
- Better Auth framework integration
- Drizzle ORM adapter for SQLite
- Email/password + OAuth2 support
- Environment-based configuration
- Isomorphic client/server

---

## 4. INTEGRATION PATTERNS

### 4.1 Backend Integration: Convex + Better Auth + Drizzle

**Three Database Systems:**
1. **Convex** (Real-time, operational data)
   - Chat sessions and messages
   - Document chunks
   - Real-time subscriptions

2. **D1/SQLite + Drizzle** (Authentication)
   - User credentials
   - Sessions and accounts
   - Verification tokens

3. **External APIs**
   - Claude via @ai-sdk/anthropic
   - OAuth providers (Google, GitHub)

**Authorization Pattern:**
```typescript
// Convex context provides authenticated user
const userId = await ctx.auth.getUserIdentity()

// Verify ownership before operations
if (session.userId !== userId.subject) {
  throw new Error('Unauthorized')
}
```

### 4.2 Frontend Integration: Router + Query + Convex + Auth

**Provider Stack (Root Layout):**
```
TanstackQueryProvider
  └─ ConvexClientProvider
     └─ Router with Outlet
        └─ Pages/Routes
```

**Data Flow:**
1. Route requests data via TanStack Query
2. Query uses Convex utilities
3. Convex client authenticates automatically
4. Results update local state via React

### 4.3 API Integration: oRPC + Fetch + SSR

**Isomorphic Execution:**
```typescript
// Server-side: Direct function calls (no HTTP)
const router = createRouter()
const result = await router.createChat({ title: 'Test' })

// Client-side: HTTP via fetch
const link = new RPCLink({ url: '/api/rpc' })
const result = await client.createChat({ title: 'Test' })
```

**Route Handler Pattern:**
```typescript
export const Route = createFileRoute('/api/rpc/$')({
  server: {
    handlers: {
      POST: async ({ request }) => {
        const handler = new RPCHandler(router)
        const result = await handler.handle(request)
        return result.matched ? result.response : new Response('Not Found', { status: 404 })
      },
    },
  },
})
```

### 4.4 AI/Chat Integration Pattern

**Stream Response Pattern:**
```typescript
export const Route = createFileRoute('/api/chat')({
  server: {
    handlers: {
      POST: async ({ request }) => {
        const { messages, documentId } = await request.json()

        const result = streamText({
          model: anthropic('claude-3-5-sonnet-20241022'),
          system: `You are a helpful AI assistant...`,
          messages,
        })

        return result.toDataStreamResponse()
      },
    },
  },
})
```

**Client Hook Pattern:**
```typescript
const { messages, input, handleInputChange, handleSubmit, isLoading } =
  useChat({
    api: '/api/chat',
    body: { documentId },
    onFinish: (message) => {
      // Handle completed message
    },
  })
```

### 4.5 MCP (Model Context Protocol) Pattern

**Available in Examples:**
- @ai-sdk/mcp integration for tool use
- Utility functions for demo tools
- Tool discovery and invocation

### 4.6 Web Socket / Real-time Pattern

**Convex Real-time Capabilities:**
```typescript
// Search index for full-text search
.searchIndex('search_content', {
  searchField: 'content',
  filterFields: ['userId'],
})

// Indexed queries for fast retrieval
.index('by_user', ['userId'])
.index('by_active', ['isActive'])
```

**Presence Tracking Available:**
- @convex-dev/presence package
- Real-time user status
- Cursor positions, typing indicators

---

## 5. ERROR HANDLING PATTERNS

### 5.1 Convex Error Handling

```typescript
// Errors thrown in mutations/queries propagate to client
if (!userId) {
  throw new Error('Not authenticated')
}

if (!session) {
  throw new Error('Session not found')
}

if (session.userId !== userId.subject) {
  throw new Error('Unauthorized: You do not own this session')
}
```

### 5.2 Invariant-based Validation

```typescript
import invariant from 'tiny-invariant'

async function ensureBoardExists(
  ctx: QueryCtx,
  boardId: string,
): Promise<Doc<'boards'>> {
  const board = await ctx.db
    .query('boards')
    .withIndex('id', (q) => q.eq('id', boardId))
    .unique()

  invariant(board, `missing board ${boardId}`)
  return board
}
```

### 5.3 React Error Boundaries

**Pattern Available but Not Extensively Used:**
- DefaultCatchBoundary component in examples
- Error fallback UI components
- Can be attached to route definitions

---

## 6. CODE STYLE & CONVENTIONS

### 6.1 Import Organization (via Biomejs)

```typescript
// Order:
// 1. External dependencies
import { useCallback, useMemo, useRef } from 'react'
import invariant from 'tiny-invariant'
import { useSuspenseQuery } from '@tanstack/react-query'

// 2. Internal imports (absolute paths)
import { convexQuery } from '@convex-dev/react-query'
import { api } from '../../convex/_generated/api.js'
import { useUpdateBoardMutation } from '../queries.js'
import { NewColumn } from './NewColumn.js'

// 3. Type imports
import type { Column } from 'convex/schema.js'
```

### 6.2 Component Structure Convention

```typescript
// 1. Imports
import { ... } from '...'

// 2. Type definitions (Props interfaces)
interface ComponentProps {
  prop1: string
  prop2?: number
}

// 3. Component function
export function ComponentName(props: ComponentProps) {
  // Hooks at top
  const [state, setState] = useState()
  const ref = useRef()

  // Effects
  useEffect(() => {
    // side effects
  }, [])

  // Handlers
  const handleClick = () => {}

  // Computed values
  const derived = useMemo(() => {}, [])

  // Render
  return (
    <div>
      {/* JSX */}
    </div>
  )
}
```

### 6.3 Tailwind CSS Convention

- Utility-first approach
- Gradient utilities for branding: `from-orange-500 to-red-600`
- Dark mode: `dark:` prefix
- Responsive: `lg:`, `md:`, `sm:` breakpoints
- Animations: Custom tw-animate-css package

---

## 7. TECHNOLOGY STACK PATTERNS

### Frontend Stack
- **Framework**: React 19.2.0 + TanStack Start 1.132.36
- **Routing**: TanStack Router 1.132.33
- **Data**: TanStack Query 5.90.2 + Convex
- **Backend**: oRPC 1.7.5 + Convex
- **Auth**: Better Auth 1.3.27 + OAuth2
- **Styling**: Tailwind CSS 4.1.8
- **UI Components**: Radix UI primitives + lucide-react icons
- **AI Integration**: @ai-sdk/anthropic 2.0.1
- **Streaming**: ai (SDK) 5.0.8 for ServerSentEvents

### Backend Stack
- **Runtime**: Cloudflare Workers (via Wrangler)
- **Backend**: Convex 1.27.3 (real-time database)
- **Database (Auth)**: D1 SQLite + Drizzle ORM
- **ORM**: Drizzle 0.39.0
- **Validation**: Zod 4.1.11
- **Type Safety**: TypeScript 5.7.2 strict mode

### Infrastructure
- **Deployment**: Netlify + Cloudflare
- **Build**: Vite 7.1.9
- **Testing**: Vitest 3.0.5
- **Linting**: Biomejs 2.2.4
- **Package Manager**: npm/pnpm

### Data Pipeline
- **Framework**: Dagster
- **Schema**: Pydantic models
- **Storage**: R2 (Cloudflare)
- **Vector DB**: LanceDB
- **Graph DB**: Memgraph
- **Extraction**: dlt (data load tool)

---

## 8. FILE PATTERNS & GENERATION

### Auto-Generated Files
```
src/routeTree.gen.ts          # TanStack Router route tree
convex/_generated/            # Convex type definitions
  ├── api.d.ts               # API functions
  ├── server.d.ts            # Server context types
  └── dataModel.d.ts         # Database schema types
```

### Configuration Files
- `vite.config.ts`: Build configuration with TanStack Start plugin
- `tsconfig.json`: TypeScript configuration
- `biome.json`: Code formatting and linting
- `drizzle.config.ts`: Database schema configuration
- `convex.config.ts`: Convex app configuration

---

## 9. PACKAGE DEPENDENCIES PHILOSOPHY

### Frontend Libraries
- **Minimal Abstractions**: Prefer composition (React + Radix UI)
- **Type Safety First**: Zod for validation, TypeScript strict
- **Server Components**: Not used in this project (TanStack Start doesn't support)
- **State**: Minimal (TanStack Query for server, TanStack Store for client UI)

### Development Tools
- **Fast Formatting**: Biomejs over Prettier
- **Monorepo**: Workspace structure over Nx/Turborepo
- **Package Manager**: npm (via Wrangler)

---

## Summary Table

| Aspect | Pattern | Example |
|--------|---------|---------|
| **API** | oRPC + Zod | `createChat.input(CreateChatInput).handler(...)` |
| **Database** | Convex + Drizzle | `mutation({args, handler})` |
| **Auth** | Better Auth + OAuth | Email/password + Google/GitHub |
| **React** | Hooks + Functional | `useState`, `useRef`, `useEffect` |
| **State** | TanStack Query + Store | `useSuspenseQuery`, `useStore` |
| **Styling** | Tailwind CSS | Utility classes with dark mode |
| **Validation** | Zod | Schema-based runtime validation |
| **Type Inference** | Convex `Infer` | `type Board = Infer<typeof board>` |
| **Error Handling** | Throw + Invariant | Errors propagate to client |
| **CI/CD** | GitHub Actions | Biome checks, type checking |
