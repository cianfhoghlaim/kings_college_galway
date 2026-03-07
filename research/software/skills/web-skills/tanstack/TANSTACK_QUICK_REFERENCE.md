# TanStack Examples - Quick Reference

## File Locations
```
/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/
├── tanstack-better-auth/          # Auth + tRPC pattern
├── learn-platform/                # Monorepo with Hono server
├── orcish-saas/                   # Dashboard template
├── tanstack-without-cloudflare/   # Server functions demo
├── orcish-tanstack-dashboard/     # Advanced dashboard UI
└── tanstack-betterauth/           # Auth + Drizzle ORM
```

## What Each Example Teaches

### tanstack-better-auth (Auth + tRPC)
**Learn:** Authentication with tRPC API
```typescript
// Key files
src/lib/auth.ts                    // betterAuth() setup
src/lib/auth-client.ts             // createAuthClient()
src/lib/auth-middleware.ts         // Route protection
src/routes/api.auth.$.ts           // Auth endpoint
src/routes/dashboard.tsx           // Protected route
src/integrations/trpc/             // tRPC setup
```

### learn-platform (Full-stack)
**Learn:** Monorepo structure with separate server/client
```
Key patterns
- apps/server/                     // Hono server with better-auth
  └── routes/auth.ts               // Auth endpoints
- apps/client/                     // Vite + TanStack Router
  └── routes/_authenticated.tsx    // Protected layout
```

### orcish-saas (Dashboard UI)
**Learn:** Radix UI + shadcn components, navigation, tables
```typescript
// Key files
src/routes/__root.tsx              // Navigation layout
src/components/                    // UI component collection
recharts for data visualization
```

### tanstack-without-cloudflare (Server Functions)
**Learn:** API routes with database access
```typescript
// Key pattern
src/routes/api/demo.ts
export const Route = createFileRoute('/api/demo')({
  server: {
    handlers: {
      GET: async () => { ... },
      POST: async ({ request }) => { ... }
    }
  }
})
```

### orcish-tanstack-dashboard (Advanced UI)
**Learn:** Drag-and-drop, complex tables, theming
```typescript
// Key features
@dnd-kit for drag and drop
@tanstack/react-table for complex tables
next-themes for dark/light mode
recharts for charts
```

### tanstack-betterauth (Auth + Drizzle)
**Learn:** Modern auth with type-safe ORM
```typescript
// Key files
src/lib/auth.ts                    // better-auth + drizzleAdapter()
src/db/schema.ts                   // Drizzle schema with pgTable()
src/routes/__root.tsx              // Root layout
```

## Common File Patterns

### Root Layout Pattern
```typescript
// All examples use this pattern
export const Route = createRootRoute({
  head: () => ({ meta, links }),
  shellComponent: RootDocument    // or component: ...
})

function RootDocument({ children }) {
  return (
    <html>
      <head><HeadContent /></head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  )
}
```

### API Route Pattern
```typescript
// tanstack-without-cloudflare pattern
export const Route = createFileRoute('/api/demo')({
  server: {
    handlers: {
      GET: async () => json({ data }),
      POST: async ({ request }) => {
        const body = await request.json()
        return json({ result })
      }
    }
  }
})
```

### Auth Pattern
```typescript
// Server setup
export const auth = betterAuth({ /* config */ })

// API handler
export const APIRoute = createAPIFileRoute("/api/auth/$")({
  GET: ({ request }) => auth.handler(request),
  POST: ({ request }) => auth.handler(request)
})

// Middleware protection
export const authMiddleware = createMiddleware().server(async ({ next }) => {
  const session = await auth.api.getSession({ headers })
  return next({ context: { user: session?.user } })
})

// Client usage
const { data: session } = useSession()
const handleSignIn = () => signIn.social({ provider: "github" })
```

## Technology Versions by Example

### Core Dependencies
| Package | tanstack-better-auth | tanstack-without-cloudflare | learn-platform | tanstack-betterauth |
|---------|---------------------|---------------------------|-----------------|-------------------|
| @tanstack/react-start | 1.114.3 | 1.132.0 | N/A | 1.130.3 |
| @tanstack/react-router | 1.114.3 | 1.132.0 | 1.133.22 | 1.130.2 |
| react | 19.0.0 | 19.2.0 | 19.2.0 | 19.1.1 |

### Auth & Database
| Package | Example | Version |
|---------|---------|---------|
| better-auth | All auth examples | 1.2.5 - 1.3.4 |
| @prisma/client | tanstack-without-cloudflare | 6.19.0 |
| @prisma/client | learn-platform | 6.6.0 |
| drizzle-orm | tanstack-betterauth | 0.44.4 |
| pg | tanstack-betterauth | 8.16.3 |

### UI & Forms
| Package | Used By | Version |
|---------|---------|---------|
| tailwindcss | All | 4.0.6 - 4.1.0 |
| zod | tanstack-without-cloudflare, learn-platform | 4.0.0 - 4.1.12 |
| react-hook-form | tanstack-without-cloudflare, learn-platform | 7.65.0 - 7.66.0 |
| recharts | orcish-saas, orcish-tanstack-dashboard | 2.15.4 |
| @dnd-kit/core | orcish-tanstack-dashboard | 6.3.1 |

## Key Decision Points

### Auth Framework
- **better-auth** (3 examples) - Modern, lightweight, OAuth support
- Alternative: Auth.js (not used in examples)

### Database
- **Prisma** (2 examples) - Used in tanstack-without-cloudflare, learn-platform
- **Drizzle** (1 example) - Used in tanstack-betterauth (more type-safe)
- **Hono with Prisma** (1 example) - Full-stack pattern

### Forms
- **React Hook Form + Zod** (2 examples)
- Alternative: Form component libraries

### Backend
- **Inline TanStack Start functions** (server functions)
- **Hono** (separate server in learn-platform)
- **No external backend** (orcish examples)

## Unified Template Decision Matrix

| Aspect | Recommendation | Reason |
|--------|---|---------|
| Auth | better-auth | 3 examples use it, simple, OAuth-ready |
| Database | Drizzle ORM | More type-safe than Prisma |
| Backend | TanStack Start server functions | Simpler than Hono, no monorepo |
| Forms | React Hook Form + Zod | Standardized in examples |
| UI Framework | Radix UI + shadcn | Universal across examples |
| Styling | Tailwind CSS 4 | Universal with @tailwindcss/vite |
| Icons | lucide-react | Standard choice |
| Theme | next-themes | Used in dashboard examples |

## Analysis Documents
- **TANSTACK_ANALYSIS.md** (650 lines) - Detailed breakdown
- **TANSTACK_SUMMARY.md** (235 lines) - Executive summary
- **TANSTACK_QUICK_REFERENCE.md** (this file) - Quick lookup

---

**For comprehensive analysis, see:** `/Users/cliste/dev/bonneagar/hackathon/TANSTACK_ANALYSIS.md`
