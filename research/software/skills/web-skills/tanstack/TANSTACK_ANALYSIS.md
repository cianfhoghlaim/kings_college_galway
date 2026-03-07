# TanStack Examples Analysis

## Summary

Analyzed 6 TanStack examples across `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/`:

1. **tanstack-better-auth** - Auth + tRPC + TanStack Start
2. **learn-platform** - Monorepo: Hono server + Vite client + Prisma + better-auth
3. **orcish-saas** - Dashboard + Charts + Tables
4. **tanstack-without-cloudflare** - Server functions + Prisma + Form validation
5. **orcish-tanstack-dashboard** - Dashboard with DnD + Charts + Tables
6. **tanstack-betterauth** - Auth + Drizzle + TanStack Start

---

## Individual Example Analysis

### 1. tanstack-better-auth
**Path:** `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/tanstack-better-auth/`

**Purpose:** Demonstrates authentication integration with better-auth, tRPC, and TanStack Start.

**Key Technologies:**
- Framework: TanStack Start (Vinxi)
- Auth: better-auth (GitHub OAuth)
- API: tRPC with @trpc/tanstack-react-query
- Styling: Tailwind CSS 4 + Radix UI
- Build: Vinxi, Vite

**Package Dependencies (Key):**
```
@tanstack/react-start: ^1.114.3
@tanstack/react-query: ^5.66.5
@tanstack/react-router: ^1.114.3
better-auth: ^1.2.5
@trpc/client: ^11.0.0
@trpc/server: ^11.0.0
```

**Auth Patterns:**
- Server-side: `betterAuth()` with GitHub provider
- Client: `createAuthClient()` from better-auth/react
- API Route: `/api/auth/$` using `createAPIFileRoute()`
- Middleware: `authMiddleware` for route protection
- Session: `useSession()` hook + server-side `getSession()`

**Routing Patterns:**
- File-based routing with `createFileRoute()` and `createAPIFileRoute()`
- Root layout: `__root.tsx` with `createRootRouteWithContext<MyRouterContext>()`
- Dashboard protection via `beforeLoad` + `redirect`
- Routes: index, dashboard, demo pages

**Server Function Patterns:**
- tRPC endpoints: `people.currentUserName.queryOptions()`
- Server functions via tRPC integration
- Route loaders with auth middleware

**Key Files:**
```
src/
├── lib/
│   ├── auth.ts               # betterAuth() config
│   ├── auth-client.ts        # createAuthClient() setup
│   ├── auth-middleware.ts    # Route protection middleware
│   └── auth-server-func.ts   # Server functions for auth data
├── routes/
│   ├── __root.tsx            # Root layout with context
│   ├── api.auth.$.ts         # Auth API handler
│   ├── api.trpc.$.tsx        # tRPC endpoint
│   ├── index.tsx             # Home with auth UI
│   └── dashboard.tsx         # Protected route example
└── integrations/
    ├── trpc/                 # tRPC setup
    └── tanstack-query/       # TanStack Query integration
```

---

### 2. learn-platform
**Path:** `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/learn-platform/`

**Purpose:** Full-stack learning platform with separate Hono server and Vite client.

**Architecture:** Monorepo with yarn workspaces
- `apps/server/` - Hono (Bun runtime)
- `apps/client/` - Vite + TanStack Router

**Key Technologies:**
- Server: Hono + Prisma + better-auth
- Client: Vite + TanStack Router + TanStack Query
- Database: PostgreSQL (Docker compose)
- Auth: better-auth (GitHub, email/password)
- Styling: Tailwind CSS
- Rich Text: Tiptap

**Server Package (Key):**
```
hono: ^4.10.2
@hono/zod-openapi: ^0.18.4
@prisma/client: 6.6.0
better-auth: ^1.3.34
pino: ^9.14.0  # Logging
```

**Client Package (Key):**
```
@tanstack/react-router: ^1.133.22
@tanstack/react-query: ^5.90.5
better-auth: ^1.3.29
hono: ^4.10.2
react-hook-form: ^7.65.0
```

**Auth Patterns:**
- Hono server with better-auth handler
- Route: `router.on(["POST", "GET"], "/auth/**", (c) => auth.handler(c.req.raw))`
- Client: better-auth createAuthClient
- Protected routes: Layout pattern with `_authenticated.tsx`

**Routing Patterns (Client):**
- File-based routing
- Auth layout: `_authenticated.tsx` wraps protected routes
- Role-based paths: `/creator/`, `/student/`
- Nested routes: `/creator/$courseId/`, `/courses/$courseId/lessons/$lessonId`

**Database Patterns:**
- Prisma schema with models: User, Course, Lesson, Payment, etc.
- Migrations: `prisma migrate dev`
- Seed scripts available

**API Patterns (Hono):**
```typescript
routes/
├── auth.ts          # Auth endpoints
├── courses.ts       # Course CRUD
├── creator.ts       # Creator-specific operations
├── student.ts       # Student-specific operations
├── payments.ts      # Payment handling
└── user-settings.ts # User profile
```

---

### 3. orcish-saas
**Path:** `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/orcish-saas/`

**Purpose:** SaaS template with dashboard, tables, and charts.

**Key Technologies:**
- Framework: TanStack Start (Vite, SSR-capable)
- UI: Radix UI + shadcn + Tailwind CSS 4
- Tables: TanStack React Table
- Charts: Recharts 2.15.4
- Backend: Nitro

**Package Dependencies:**
```
@tanstack/react-start: ^1.132.0
@tanstack/react-table: ^8.21.3
@tanstack/react-router: ^1.132.0
recharts: 2.15.4
nitro: latest
```

**Key Features:**
- Data tables with sorting/filtering
- Chart visualizations
- Navigation menu system
- Progress indicators
- Tooltips and dropdowns

**Routing Patterns:**
- File-based routing
- Root: `__root.tsx` with shellComponent
- No auth implemented (SaaS template skeleton)

**UI Patterns:**
- Extensive Radix UI component usage
- shadcn-style wrapper components
- Tailwind animations (tw-animate-css)
- Progress bars, tabs, toggles

---

### 4. tanstack-without-cloudflare
**Path:** `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/tanstack-without-cloudflare/`

**Purpose:** Demonstrates TanStack Start without Cloudflare, using Prisma for data persistence.

**Key Technologies:**
- Framework: TanStack Start
- Database: Prisma + SQLite (locally)
- Forms: React Hook Form + Zod validation
- Styling: Tailwind CSS 4 + shadcn
- Backend: Nitro

**Package Dependencies:**
```
@tanstack/react-start: ^1.132.0
@tanstack/react-router: ^1.132.0
@prisma/client: ^6.19.0
react-hook-form: ^7.66.0
zod: 4.0.0
```

**Server Function Patterns:**
```typescript
// API route with handlers
export const Route = createFileRoute('/api/demo')({
  server: {
    handlers: {
      GET: async () => {
        return json({ message: 'Hello from GET' })
      },
      POST: async ({ request }) => {
        const body = await request.json()
        return json({ message: `Hello, ${body.name}` })
      }
    }
  }
})
```

**Key Routes:**
- `/posts` - Server-rendered posts list (Prisma + server functions)
- `/posts/create` - React Hook Form + Zod validation
- `/layers` - Nested layout demo
- `/middleware-demo` - Middleware example
- `/server-routes` - API endpoint demo
- `/typesafe-params` - Zod-validated search params

**Database Integration:**
- Prisma schema for posts
- Server functions accessing DB
- Type generation from schema

---

### 5. orcish-tanstack-dashboard
**Path:** `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/orcish-tanstack-dashboard/`

**Purpose:** Advanced dashboard template with drag-and-drop, complex tables, and charts.

**Key Technologies:**
- Framework: TanStack Start
- DnD: @dnd-kit (core, sortable, modifiers)
- Tables: TanStack React Table
- Charts: Recharts
- UI: Radix UI + shadcn + Tailwind CSS 4
- Icons: Lucide React + Tabler Icons
- Notifications: Sonner
- Theme: next-themes

**Package Dependencies:**
```
@dnd-kit/core: ^6.3.1
@dnd-kit/sortable: ^10.0.0
@tanstack/react-table: ^8.21.3
recharts: ^2.15.4
sonner: ^2.0.7
next-themes: ^0.4.6
```

**Key Features:**
- Drag-and-drop interface
- Complex data tables with filtering/sorting
- Chart visualizations
- Dark/light theme support
- Toast notifications
- Advanced UI components (tabs, dialogs, popovers)

**UI Component Library:**
- Extensive Radix UI primitives
- shadcn component patterns
- Tailwind CSS grid layouts
- Theme provider integration

---

### 6. tanstack-betterauth
**Path:** `/Users/cliste/dev/bonneagar/hackathon/web/examples-working/tanstack/tanstack-betterauth/`

**Purpose:** TanStack Start with better-auth and Drizzle ORM (PostgreSQL).

**Key Technologies:**
- Framework: TanStack Start
- Auth: better-auth ^1.3.4
- Database: Drizzle ORM + PostgreSQL
- Styling: Tailwind CSS 4
- Base UI: @base-ui-components/react (beta)

**Package Dependencies:**
```
@tanstack/react-start: ^1.130.3
@tanstack/react-router: ^1.130.2
better-auth: ^1.3.4
drizzle-orm: ^0.44.4
pg: ^8.16.3
drizzle-kit: ^0.31.4  # devDep
```

**Database Schema (Drizzle):**
```typescript
export const user = pgTable("user", {
  id, name, email, emailVerified, image,
  createdAt, updatedAt
})
export const session = pgTable("session", { ... })
export const account = pgTable("account", { ... })
export const verification = pgTable("verification", { ... })
```

**Auth Patterns:**
```typescript
// Server: better-auth with Drizzle adapter
export const auth = betterAuth({
  database: drizzleAdapter(db, {
    provider: "pg",
    schema: { user, session, account, verification }
  }),
  socialProviders: { github: { ... } }
})

// Client: standard better-auth client
export const { signIn, signUp, useSession, signOut } =
  createAuthClient({ baseURL: "http://localhost:3000" })

// Middleware: auth.api.getSession() pattern
export const authMiddleware = createMiddleware({ type: "function" })
  .server(async ({ next }) => {
    const session = await auth.api.getSession({
      headers: getHeaders() as unknown as Headers
    })
    return next({ context: { user: { ... } } })
  })
```

**Routes:**
```
src/
├── lib/
│   ├── auth.ts           # betterAuth + Drizzle config
│   ├── auth-client.ts    # Client auth setup
│   ├── auth-middleware.ts # Route protection
│   └── auth-server-fn.ts # Server functions
├── db/
│   └── schema.ts         # Drizzle schema
└── routes/
    ├── __root.tsx        # Root layout
    ├── index.tsx         # Home/login
    ├── dashboard.tsx     # Protected
    └── api/              # API routes
```

---

## Overlapping Patterns

### Authentication Pattern
**Used in:** tanstack-better-auth, learn-platform, tanstack-betterauth

**Common Elements:**
1. **Server Setup:**
   - `betterAuth()` factory with config
   - GitHub (and other) social providers
   - Database adapter (none/Drizzle)

2. **Client Setup:**
   - `createAuthClient()` with baseURL
   - `useSession()` hook
   - `signIn()`, `signOut()`, `signUp()` helpers

3. **Route Protection:**
   - Middleware approach: `authMiddleware`
   - Route loaders checking session
   - Redirect on auth failure

4. **API Endpoints:**
   - `/api/auth/$` catch-all route
   - `auth.handler(request)` pattern

### Routing Pattern
**Used in:** All examples

**Common Elements:**
1. **File-Based Routing:**
   - `src/routes/` directory structure
   - `createFileRoute()` API
   - Auto-generated `routeTree.gen.ts`

2. **Root Layout:**
   - `__root.tsx` or `src/app`
   - `HeadContent`, `Scripts` for SSR
   - Global context/providers
   - Devtools integration

3. **API Routes:**
   - `/api/*` pattern
   - `createFileRoute('/api/...')` with `server.handlers`
   - GET/POST handlers

4. **Nested Layouts:**
   - `_layout.tsx` pattern (in learn-platform)
   - Protected routes with `_authenticated.tsx`

### Server Function Pattern
**Used in:** tanstack-without-cloudflare, tanstack-better-auth, tanstack-betterauth

**Common Elements:**
1. **Inline Server Logic:**
   ```typescript
   export const Route = createFileRoute('/api/demo')({
     server: {
       handlers: {
         GET: async () => { ... }
       }
     }
   })
   ```

2. **Server Functions:**
   - Direct database access
   - Called from route loaders
   - Type-safe via server context

3. **Database Integration:**
   - Prisma (tanstack-without-cloudflare)
   - Drizzle (tanstack-betterauth)
   - Hono + Prisma (learn-platform)

### Form & Validation Pattern
**Used in:** tanstack-without-cloudflare, learn-platform

**Common Elements:**
1. **React Hook Form:**
   - `useForm()` hook
   - Field registration
   - `resolver()` with Zod

2. **Zod Schemas:**
   - Shared server/client schemas
   - Runtime validation
   - Type inference

3. **Server Mutations:**
   - Form submission to server functions
   - Validation on both sides
   - Error handling/display

### UI/Styling Pattern
**Used in:** All examples

**Common Elements:**
1. **Tailwind CSS:**
   - v4 with @tailwindcss/vite
   - Responsive utilities
   - Custom animations (tw-animate-css)

2. **Radix UI Primitives:**
   - Low-level accessible components
   - Dialog, Menu, Select, etc.

3. **shadcn Component Pattern:**
   - Copy-paste components
   - Button, Card, Input, etc.
   - Built on Radix UI

4. **Theme Management:**
   - next-themes (dashboard examples)
   - CSS custom properties
   - Dark/light mode support

---

## Essential TanStack Start Foundation Patterns

### 1. **Core Setup** (Required)
- File-based routing with `createFileRoute()`
- Root layout (`__root.tsx`) with `createRootRoute()`
- App config (`app.config.ts`) with Vite plugins
- HTML shell: `<HeadContent />`, `<Scripts />`

### 2. **Authentication** (Recommended)
- better-auth server setup
- createAuthClient on client
- Middleware for protected routes
- `/api/auth/$` endpoint

### 3. **Data Fetching** (Recommended)
- Server functions via route handlers
- TanStack Query for client state (optional but common)
- Route loaders for pre-fetching
- Server context for user data

### 4. **Styling** (Standard)
- Tailwind CSS 4 with @tailwindcss/vite
- Radix UI + shadcn components
- App CSS entry point

### 5. **Database** (For Data Persistence)
- Prisma or Drizzle ORM
- Server functions accessing DB
- Type generation from schema

---

## Recommendations for Unified Template

### Merge Strategy

Create **ONE comprehensive TanStack Start template** combining the best patterns:

```
unified-tanstack-template/
├── app.config.ts                    # from tanstack-better-auth
├── package.json                     # Consolidated deps
├── prisma/
│   ├── schema.prisma               # from tanstack-without-cloudflare
│   └── migrations/
├── src/
│   ├── lib/
│   │   ├── auth.ts                 # better-auth + Drizzle
│   │   ├── auth-client.ts
│   │   ├── auth-middleware.ts
│   │   ├── db.ts                   # Database connection
│   │   └── utils.ts
│   ├── components/
│   │   ├── ui/                     # shadcn components
│   │   ├── Header.tsx
│   │   ├── Navigation.tsx
│   │   └── ThemeProvider.tsx
│   ├── routes/
│   │   ├── __root.tsx              # Root with theme, auth middleware
│   │   ├── index.tsx               # Public home
│   │   ├── login.tsx               # Auth pages
│   │   ├── register.tsx
│   │   ├── dashboard.tsx           # Protected route
│   │   ├── api/
│   │   │   ├── auth.$.ts           # Auth handler
│   │   │   └── demo.ts             # API example
│   │   └── _authenticated.tsx      # Protected layout
│   ├── schemas/                    # Zod schemas for forms
│   │   └── user.ts
│   ├── styles.css                  # Global styles
│   ├── router.tsx                  # Router setup
│   └── server.ts                   # Server handler
├── tsconfig.json
└── vite.config.ts
```

### Key Decisions:

1. **Auth Foundation:**
   - Use better-auth (widely adopted)
   - Support Drizzle ORM with PostgreSQL (modern, type-safe)
   - Include GitHub + email/password providers

2. **Database:**
   - Use Drizzle ORM (better than Prisma for this stack)
   - PostgreSQL as primary
   - Include schema for User, Session, Account, Verification
   - Add migrations setup

3. **Forms:**
   - Include React Hook Form + Zod examples
   - Create reusable form components
   - Validation schema examples

4. **UI Library:**
   - shadcn components (button, card, form, input, etc.)
   - Tailwind CSS 4
   - Dark/light theme with next-themes
   - Radix UI as foundation

5. **Data Fetching:**
   - TanStack Query for client state
   - Server functions for mutations
   - Route loaders for page data
   - Type-safe API calls

6. **Project Structure:**
   - Clear separation: lib, components, routes, schemas
   - Example: public, authenticated, admin routes
   - API route examples (GET/POST)
   - Protected route middleware

7. **Examples to Include:**
   - Login/logout flow
   - Protected dashboard
   - Form submission with validation
   - Data table with sorting/filtering
   - API fetch example
   - Error handling patterns

### Implementation Order:

1. Start with tanstack-without-cloudflare (server functions base)
2. Add better-auth from tanstack-betterauth (auth + Drizzle)
3. Integrate form patterns from learn-platform
4. Add UI components from orcish-tanstack-dashboard
5. Create comprehensive README with:
   - Setup instructions
   - Architecture overview
   - Common patterns explained
   - Extension examples

### Technology Stack (Final):

```json
{
  "core": [
    "@tanstack/react-start: ^1.132.0",
    "@tanstack/react-router: ^1.132.0",
    "react: ^19.2.0"
  ],
  "auth": [
    "better-auth: ^1.3.4"
  ],
  "database": [
    "drizzle-orm: ^0.44.4",
    "drizzle-kit: ^0.31.4"
  ],
  "forms": [
    "react-hook-form: ^7.66.0",
    "zod: ^4.1.12"
  ],
  "ui": [
    "@radix-ui/*",
    "tailwindcss: ^4.1.0",
    "lucide-react: ^0.544.0"
  ],
  "devTools": [
    "typescript: ^5.8.0",
    "vite: ^7.0.0"
  ]
}
```

---

## Key Takeaways

1. **File-based routing is universal** - All examples use it
2. **Authentication is critical** - 3 examples implement it with better-auth
3. **Server functions are powerful** - Inline DB access without extra layers
4. **Database abstraction matters** - Drizzle > Prisma for modern stacks
5. **UI consistency** - Radix UI + shadcn + Tailwind is the standard
6. **Developer experience** - Type safety (Zod, TypeScript) is everywhere
7. **Separation of concerns** - Clear lib/, components/, routes/, schemas/ structure

