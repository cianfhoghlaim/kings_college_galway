# Pangolin Project: Architecture Patterns & Best Practices

## Architecture Overview

The Pangolin project exemplifies a **modern full-stack TypeScript architecture** with emphasis on:
- Type safety (TypeScript strict mode)
- Composability (functional components)
- Real-time capabilities (Convex)
- Streaming responses (AI SDK)
- Developer experience (Biomejs, TanStack devtools)

## Architectural Layers

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  (React Components, Tailwind CSS, Lucide Icons)            │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                    Routing Layer                            │
│  (TanStack Router - File-based routing with API routes)    │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                   Data Layer                               │
│  ├─ State Management: TanStack Query + TanStack Store      │
│  ├─ Data Fetching: oRPC + Convex                           │
│  └─ Validation: Zod schemas                               │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴───────────────┐
        │                              │
┌───────┴──────────────┐    ┌─────────┴──────────────┐
│    API Layer (oRPC)  │    │  Real-time (Convex)   │
│  - Type-safe RPC     │    │  - Real-time DB       │
│  - Zod validation    │    │  - Auth integration   │
│  - SSR support       │    │  - Search indexes     │
└───────┬──────────────┘    └──────────┬─────────────┘
        │                             │
        │      ┌──────────────────────┤
        │      │                      │
┌───────┴──────┴──┐    ┌─────────────┴────────┐    ┌──────────────┐
│ Authentication  │    │ Operational DB       │    │ AI/External  │
│ (Better Auth +  │    │ (Convex Real-time)   │    │ APIs         │
│  D1 SQLite)     │    │                      │    │ (Anthropic)  │
└─────────────────┘    └──────────────────────┘    └──────────────┘
```

## Key Architecture Decisions

### 1. Dual Database Strategy

| Database | Purpose | Use Case | ORM |
|----------|---------|----------|-----|
| **Convex** | Real-time operations | Chat sessions, messages, documents | Native API |
| **D1 + Drizzle** | Authentication | Users, sessions, accounts, verification | Drizzle |

**Rationale:**
- Convex: Best for real-time, subscriptions, search indexes
- D1: Simple, reliable for auth state (aligned with Cloudflare ecosystem)
- Separation of concerns: Auth isolated from application data

### 2. Type-Safe API Layer (oRPC)

**Advantages:**
- Single source of truth for API contracts (no separate OpenAPI specs)
- Type-safe on both client and server
- Works with SSR (no HTTP on server-side)
- Zod-based runtime validation
- Auto-generates TypeScript types from implementation

**Alternative patterns NOT used:**
- tRPC: Similar but less flexible routing
- GraphQL: More overhead for CRUD operations
- REST + OpenAPI: Verbose, type mapping complexity

### 3. React State Management Strategy

**State Categories:**
1. **Server State** → TanStack Query (cached, with refetch strategies)
2. **UI State** → TanStack Store (simple, synchronized)
3. **Form State** → React useState (controlled components)

**NOT used:**
- Redux: Overkill for this architecture
- Context + useReducer: Less performant than Query + Store combo
- Zustand: Less integrated with TanStack

### 4. Validation Strategy

**Zod Schemas:**
- Define once at API boundary
- Validates both input and output
- Auto-generates TypeScript types
- Runtime validation (type safety at runtime)

```typescript
// Single source of truth
const CreateChatInput = z.object({
  title: z.string(),
})

// Inferred type
type CreateChatInput = z.infer<typeof CreateChatInput>

// Used in oRPC handler
export const createChat = os.input(CreateChatInput).handler(...)

// Used in Convex mutation
export const createSession = mutation({
  args: { title: v.string() },
  handler: ...
})
```

### 5. Component Composition Pattern

**Principle:** Composition over Inheritance

**Pattern:**
```typescript
// Presentational Component (pure, no state)
export function ChatMessage({ role, content, sources }: ChatMessageProps) {
  return <div>...</div>
}

// Container Component (manages state, data)
export function DocumentChat({ documentId }: DocumentChatProps) {
  const { messages, input, handleSubmit } = useChat(...)
  return <div>
    <ChatMessage ... />
  </div>
}
```

**Benefits:**
- Easy to test (presentational components are pure functions)
- Reusable (ChatMessage used in multiple contexts)
- Single Responsibility Principle

## Design Patterns Identified

### 1. Provider Pattern (Context)

```typescript
// Root layout provides context
<TanstackQueryProvider>
  <ConvexClientProvider>
    <Router>
      <PageComponents />
    </Router>
  </ConvexClientProvider>
</TanstackQueryProvider>
```

**Used for:**
- Global state providers
- API client initialization
- Configuration context

### 2. Custom Hook Pattern

```typescript
export function useAuth() {
  return authClient
}

export function useChat(options: UseChartOptions) {
  const { messages, input, handleSubmit } = useChat(options)
  return { messages, input, handleSubmit }
}
```

**Benefits:**
- Encapsulates complex logic
- Reusable across components
- Easier testing via mock hooks

### 3. Handler Pattern (Functions)

```typescript
// oRPC handlers
export const createChat = os
  .input(CreateChatInput)
  .handler(({ input }) => {
    // Centralized business logic
  })

// Convex handlers
export const createSession = mutation({
  args: { title: v.string() },
  handler: async (ctx, args) => {
    // Centralized business logic
  },
})
```

**Benefits:**
- Centralized business logic
- Type-safe inputs/outputs
- Easily testable

### 4. Invariant Pattern (Error Handling)

```typescript
import invariant from 'tiny-invariant'

const board = await fetchBoard(id)
invariant(board, `missing board ${id}`)
// TypeScript knows board is not null after this
```

**Benefits:**
- Type narrowing
- Clear error messages
- Fail-fast principle

### 5. Isomorphic Pattern

```typescript
export const getORPCClient = createIsomorphicFn()
  .server(() => createRouterClient(router))  // Direct calls
  .client(() => createORPCClient(link))      // HTTP calls
```

**Benefits:**
- Code reuse (same API for client and server)
- Zero-latency on server-side
- Automatic HTTP on client-side

## Best Practices Observed

### 1. Error Handling

**Do's:**
- Throw errors in Convex handlers (propagate to client)
- Use invariant() for non-recoverable states
- Provide clear error messages

**Don'ts:**
- Don't silently fail
- Don't use console.error as error handling
- Don't mix error types

### 2. Database Access

**Do's:**
- Always check authentication first: `ctx.auth.getUserIdentity()`
- Verify ownership: `if (session.userId !== userId.subject)`
- Use indexes for queries: `.withIndex('by_user', (q) => ...)`
- Use cascading deletes: `.references(() => user.id, { onDelete: 'cascade' })`

**Don'ts:**
- Don't expose all data without auth checks
- Don't do N+1 queries (use Promise.all)
- Don't create indexes after knowing access patterns

### 3. Component Development

**Do's:**
- Define props interfaces at component top
- Use React hooks for state management
- Extract logic to custom hooks
- Compose components (avoid prop drilling)

**Don'ts:**
- Don't use default exports for hooks/utils
- Don't put business logic in components
- Don't use index-based keys in lists
- Don't drill props more than 2 levels deep

### 4. Type Safety

**Do's:**
- Use TypeScript strict mode
- Let types be inferred: `const user = await fetchUser()` (inferred type)
- Use Zod for runtime validation
- Use type inference: `type Board = Infer<typeof board>`

**Don'ts:**
- Don't use `any` type
- Don't use `as` for type casting (use narrowing instead)
- Don't leave variables untyped

### 5. Performance

**Do's:**
- Use `useMemo` for expensive computations
- Use `useCallback` for function refs
- Use `useSuspenseQuery` for data dependencies
- Optimize images with srcset
- Debounce input handlers

**Don'ts:**
- Don't skip dependency arrays
- Don't create functions in render
- Don't pass inline objects as props

## Integration Patterns & Concerns

### 1. AI Integration

**Pattern:** Streaming responses with Server-Sent Events

```typescript
const result = streamText({
  model: anthropic('claude-3-5-sonnet'),
  messages,
})
return result.toDataStreamResponse()
```

**Concerns:**
- Token limits (currently no streaming limitations)
- Cost per request (tracked via Anthropic API)
- Latency (depends on model size)

### 2. Real-time Updates

**Pattern:** Convex subscriptions with TanStack Query

```typescript
// Subscribe to changes
const { data: messages } = useSuspenseQuery(
  convexQuery(api.chat.getMessages, { sessionId })
)
```

**Concerns:**
- Convex connection limits
- Bandwidth for large message streams
- Offline support (currently not implemented)

### 3. File Uploads

**Not implemented in base-merged**, but infrastructure supports:
- Cloudflare R2 for storage
- File metadata in Drizzle
- Progress tracking via multipart upload

### 4. Search & Indexing

**Pattern:** Full-text search with Convex search indexes

```typescript
.searchIndex('search_content', {
  searchField: 'content',
  filterFields: ['userId'],
})
```

**Concerns:**
- Index size limits
- Search relevance tuning
- Performance with large datasets

## Testing Patterns

### Unit Testing (not extensive in base-merged)

```typescript
// Example pattern
describe('ChatMessage', () => {
  it('renders user message correctly', () => {
    render(<ChatMessage role="user" content="Hello" />)
    expect(screen.getByText('Hello')).toBeInTheDocument()
  })
})
```

### Integration Testing

Patterns available but not heavily used:
- Vitest for unit/integration
- Testing Library for component testing
- Convex testing utilities for backend

## Deployment Patterns

### Frontend Deployment

- **Target:** Netlify (via TanStack Start plugin)
- **Build:** Vite SSR build
- **Environment:** Server functions + Edge functions

### Backend Deployment

- **Convex:** Automatically deployed (serverless)
- **D1:** Cloudflare database (no deployment needed)
- **API Routes:** Deployed with frontend (via TanStack Start)

### Environment Variables

**Client-side:**
- Prefixed with `VITE_` (e.g., `VITE_CONVEX_URL`)
- Available in `import.meta.env`

**Server-side:**
- Access via `process.env`
- Cloudflare Workers: Available in handler context

## Monitoring & Observability

### Available but Minimal Usage

- **Sentry Integration:** `@sentry/tanstackstart-react`
- **Error Tracking:** Not configured in base-merged
- **Performance:** TanStack Devtools for debugging

### Recommended Additions

1. **Error Tracking:** Sentry for production errors
2. **Analytics:** Event tracking for user interactions
3. **Logging:** Structured logging for API calls
4. **Monitoring:** Uptime monitoring for critical paths

## Security Considerations

### Implemented

- **Authentication:** Better Auth with OAuth2
- **Authorization:** User ownership checks in Convex
- **Input Validation:** Zod schema validation
- **Session Management:** Token-based sessions
- **HTTPS:** Enforced by Cloudflare

### Recommended Enhancements

1. **Rate Limiting:** Add rate limits to API endpoints
2. **CORS:** Configure CORS properly for API
3. **CSP:** Content Security Policy headers
4. **XSS Protection:** Sanitize markdown rendering (rehype-sanitize included)
5. **SQL Injection:** Protected by ORM usage (Drizzle, Convex)

## Future Extension Points

### 1. Real-time Collaboration

Add via Convex presence + WebSockets:
```typescript
// Track typing indicators
// Cursor positions
// User presence
```

### 2. AI Features

- Image processing (new model)
- Voice input/output (speech API)
- Vector embeddings for semantic search

### 3. Offline Support

- Service Worker caching
- Local-first sync via Replicache or similar
- Conflict resolution strategies

### 4. Mobile Support

- React Native version (share components)
- Native app bridges
- Push notifications

### 5. Analytics & Insights

- User behavior tracking
- Usage metrics
- Cost optimization insights

## Summary: Pangolin Architecture Philosophy

Pangolin embraces:

1. **Type-First Development:** TypeScript strict mode throughout
2. **Minimal Abstractions:** Prefer composition over frameworks
3. **Developer Experience:** Excellent tooling (Biomejs, Vite, TanStack devtools)
4. **Scalability:** Clear separation of concerns (Auth DB vs. Operational DB)
5. **Real-time by Default:** Convex as primary database
6. **Type Safety at Boundaries:** Zod for API contracts
7. **Functional Programming:** Pure functions, composition, immutability
8. **Standards-Based:** Uses established standards (REST, OAuth2, JSON, HTTP/2)

This architecture is particularly well-suited for:
- Real-time collaborative applications
- AI/ML-powered features
- Rapid iteration and prototyping
- Teams comfortable with TypeScript
- Projects requiring strict type safety
