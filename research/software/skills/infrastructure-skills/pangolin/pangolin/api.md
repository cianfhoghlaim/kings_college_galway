---
name: Pangolin API Development
description: Create type-safe APIs with oRPC, Convex, and Better Auth in Pangolin apps.
category: Development
tags: [pangolin, api, orpc, convex, development]
---

# Pangolin API Development Assistant

I'll help you create type-safe, production-ready APIs for your Pangolin application using oRPC and Convex.

## API Development Workflow

1. **Define Schema** → Zod validation schemas
2. **Create Backend** → Convex functions or oRPC handlers
3. **Generate Types** → Automatic type inference
4. **Use in Frontend** → Type-safe client calls
5. **Test** → Unit and integration tests

## Quick Start: Create a New API Endpoint

### Step 1: Define Your Data Schema

```typescript
// app/api/schemas/feature.ts
import { z } from 'zod'

export const featureSchema = z.object({
  id: z.string(),
  title: z.string().min(1).max(100),
  description: z.string().optional(),
  userId: z.string(),
  createdAt: z.number(),
  updatedAt: z.number()
})

export const createFeatureSchema = featureSchema.omit({
  id: true,
  createdAt: true,
  updatedAt: true
})

export const updateFeatureSchema = featureSchema.partial().required({ id: true })

export type Feature = z.infer<typeof featureSchema>
export type CreateFeatureInput = z.infer<typeof createFeatureSchema>
export type UpdateFeatureInput = z.infer<typeof updateFeatureSchema>
```

### Step 2A: Create Convex Backend (Recommended for Real-time)

```typescript
// convex/schema.ts
import { defineSchema, defineTable } from 'convex/server'
import { v } from 'convex/values'

export default defineSchema({
  features: defineTable({
    title: v.string(),
    description: v.optional(v.string()),
    userId: v.string(),
    createdAt: v.number(),
    updatedAt: v.number()
  })
    .index('by_user', ['userId'])
    .index('by_created', ['createdAt'])
})
```

```typescript
// convex/queries/features.ts
import { query } from '../_generated/server'
import { v } from 'convex/values'

// List user's features
export const list = query({
  args: {},
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    return await ctx.db
      .query('features')
      .withIndex('by_user', (q) => q.eq('userId', identity.subject))
      .order('desc')
      .collect()
  }
})

// Get single feature
export const get = query({
  args: { id: v.id('features') },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    const feature = await ctx.db.get(args.id)
    if (!feature) throw new Error('Feature not found')
    if (feature.userId !== identity.subject) throw new Error('Unauthorized')

    return feature
  }
})
```

```typescript
// convex/mutations/features.ts
import { mutation } from '../_generated/server'
import { v } from 'convex/values'

// Create feature
export const create = mutation({
  args: {
    title: v.string(),
    description: v.optional(v.string())
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    const now = Date.now()
    const featureId = await ctx.db.insert('features', {
      title: args.title,
      description: args.description,
      userId: identity.subject,
      createdAt: now,
      updatedAt: now
    })

    return await ctx.db.get(featureId)
  }
})

// Update feature
export const update = mutation({
  args: {
    id: v.id('features'),
    title: v.optional(v.string()),
    description: v.optional(v.string())
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    const feature = await ctx.db.get(args.id)
    if (!feature) throw new Error('Feature not found')
    if (feature.userId !== identity.subject) throw new Error('Unauthorized')

    await ctx.db.patch(args.id, {
      ...args,
      id: undefined, // Remove id from patch
      updatedAt: Date.now()
    })

    return await ctx.db.get(args.id)
  }
})

// Delete feature
export const remove = mutation({
  args: { id: v.id('features') },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    const feature = await ctx.db.get(args.id)
    if (!feature) throw new Error('Feature not found')
    if (feature.userId !== identity.subject) throw new Error('Unauthorized')

    await ctx.db.delete(args.id)
    return { success: true }
  }
})
```

### Step 2B: Create oRPC API (Alternative for REST-like APIs)

```typescript
// app/api/features.ts
import { z } from 'zod'
import { router } from '~/lib/orpc'
import { db } from '~/lib/db'
import { features } from '~/db/schema'
import { eq, and } from 'drizzle-orm'
import { requireAuth } from '~/lib/auth-middleware'

export const featuresRouter = router({
  // List features
  list: async (_, context) => {
    const user = await requireAuth(context)
    const userFeatures = await db
      .select()
      .from(features)
      .where(eq(features.userId, user.id))
      .orderBy(features.createdAt, 'desc')
    return userFeatures
  },

  // Get single feature
  get: async (input: { id: string }, context) => {
    const user = await requireAuth(context)
    const feature = await db.query.features.findFirst({
      where: and(
        eq(features.id, input.id),
        eq(features.userId, user.id)
      )
    })
    if (!feature) throw new Error('Feature not found')
    return feature
  },

  // Create feature
  create: async (input: CreateFeatureInput, context) => {
    const validated = createFeatureSchema.parse(input)
    const user = await requireAuth(context)

    const [feature] = await db.insert(features).values({
      ...validated,
      userId: user.id,
      createdAt: Date.now(),
      updatedAt: Date.now()
    }).returning()

    return feature
  },

  // Update feature
  update: async (input: UpdateFeatureInput, context) => {
    const validated = updateFeatureSchema.parse(input)
    const user = await requireAuth(context)

    const existing = await db.query.features.findFirst({
      where: and(
        eq(features.id, validated.id),
        eq(features.userId, user.id)
      )
    })
    if (!existing) throw new Error('Feature not found')

    const [updated] = await db
      .update(features)
      .set({ ...validated, updatedAt: Date.now() })
      .where(eq(features.id, validated.id))
      .returning()

    return updated
  },

  // Delete feature
  delete: async (input: { id: string }, context) => {
    const user = await requireAuth(context)

    const existing = await db.query.features.findFirst({
      where: and(
        eq(features.id, input.id),
        eq(features.userId, user.id)
      )
    })
    if (!existing) throw new Error('Feature not found')

    await db.delete(features).where(eq(features.id, input.id))
    return { success: true }
  }
})
```

### Step 3: Use in Frontend

#### With Convex:
```typescript
// app/components/FeatureList.tsx
import { useQuery, useMutation } from 'convex/react'
import { api } from '~/convex/_generated/api'

export function FeatureList() {
  // Real-time query
  const features = useQuery(api.queries.features.list)
  const createFeature = useMutation(api.mutations.features.create)
  const deleteFeature = useMutation(api.mutations.features.remove)

  const handleCreate = async () => {
    await createFeature({
      title: 'New Feature',
      description: 'Description here'
    })
  }

  const handleDelete = async (id: string) => {
    await deleteFeature({ id })
  }

  if (features === undefined) return <div>Loading...</div>

  return (
    <div>
      <button onClick={handleCreate}>Create Feature</button>
      {features.map((feature) => (
        <div key={feature._id}>
          <h3>{feature.title}</h3>
          <p>{feature.description}</p>
          <button onClick={() => handleDelete(feature._id)}>Delete</button>
        </div>
      ))}
    </div>
  )
}
```

#### With oRPC:
```typescript
// app/components/FeatureList.tsx
import { useQuery, useMutation } from '@tanstack/react-query'
import { orpcClient } from '~/lib/orpc-client'

export function FeatureList() {
  // Query with caching
  const { data: features, isLoading } = useQuery({
    queryKey: ['features'],
    queryFn: () => orpcClient.features.list()
  })

  const createMutation = useMutation({
    mutationFn: (input: CreateFeatureInput) => orpcClient.features.create(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['features'] })
    }
  })

  if (isLoading) return <div>Loading...</div>

  return (
    <div>
      <button onClick={() => createMutation.mutate({ title: 'New', description: 'Test' })}>
        Create Feature
      </button>
      {features?.map((feature) => (
        <div key={feature.id}>
          <h3>{feature.title}</h3>
          <p>{feature.description}</p>
        </div>
      ))}
    </div>
  )
}
```

## Advanced Patterns

### Authentication Middleware

```typescript
// app/lib/auth-middleware.ts
import { auth } from '~/lib/auth'
import invariant from 'tiny-invariant'

export async function requireAuth(context: { request: Request }) {
  const session = await auth.api.getSession({
    headers: context.request.headers
  })

  invariant(session?.user, 'Unauthenticated')
  return session.user
}

// Optional auth (returns undefined if not logged in)
export async function optionalAuth(context: { request: Request }) {
  const session = await auth.api.getSession({
    headers: context.request.headers
  })
  return session?.user
}
```

### Pagination

```typescript
// Convex pagination
export const listPaginated = query({
  args: {
    cursor: v.optional(v.string()),
    limit: v.optional(v.number())
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    const limit = args.limit ?? 20
    const results = await ctx.db
      .query('features')
      .withIndex('by_user', (q) => q.eq('userId', identity.subject))
      .paginate({
        cursor: args.cursor ?? null,
        numItems: limit
      })

    return {
      items: results.page,
      nextCursor: results.continueCursor,
      hasMore: results.isDone === false
    }
  }
})

// Frontend usage
const { data, fetchNextPage, hasNextPage } = useInfiniteQuery({
  queryKey: ['features'],
  queryFn: ({ pageParam }) => orpcClient.features.listPaginated({ cursor: pageParam }),
  getNextPageParam: (lastPage) => lastPage.nextCursor
})
```

### Search & Filtering

```typescript
// Convex search
export const search = query({
  args: {
    query: v.string(),
    limit: v.optional(v.number())
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) throw new Error('Unauthenticated')

    // Simple search (case-insensitive contains)
    const all = await ctx.db
      .query('features')
      .withIndex('by_user', (q) => q.eq('userId', identity.subject))
      .collect()

    const filtered = all.filter((feature) =>
      feature.title.toLowerCase().includes(args.query.toLowerCase())
    )

    return filtered.slice(0, args.limit ?? 50)
  }
})

// For better search, use Convex vector search or external service
```

### File Upload (with R2)

```typescript
// API endpoint for upload URL
export const uploadRouter = router({
  getUploadUrl: async (input: { filename: string }, context) => {
    const user = await requireAuth(context)

    // Generate presigned URL for R2
    const key = `users/${user.id}/${Date.now()}-${input.filename}`
    const uploadUrl = await generatePresignedUrl(key, 'put')

    return {
      uploadUrl,
      key,
      downloadUrl: `https://your-r2-domain.com/${key}`
    }
  }
})

// Frontend usage
const uploadFile = async (file: File) => {
  // Get upload URL
  const { uploadUrl, downloadUrl } = await orpcClient.upload.getUploadUrl({
    filename: file.name
  })

  // Upload to R2
  await fetch(uploadUrl, {
    method: 'PUT',
    body: file,
    headers: { 'Content-Type': file.type }
  })

  return downloadUrl
}
```

### Streaming Responses (AI Chat)

```typescript
// API route with streaming
export const Route = createAPIRoute({
  POST: async ({ request }) => {
    const { messages, documentId } = await request.json()

    // Stream from Claude
    const stream = await anthropic.messages.stream({
      model: 'claude-3-5-sonnet-20241022',
      messages,
      max_tokens: 4096,
      stream: true
    })

    // Return SSE stream
    return new Response(stream.toReadableStream(), {
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        Connection: 'keep-alive'
      }
    })
  }
})

// Frontend with useChat
import { useChat } from '@ai-sdk/react'

const { messages, input, handleSubmit, isLoading } = useChat({
  api: '/api/chat',
  body: { documentId }
})
```

## Testing Your APIs

### Unit Tests (Vitest)

```typescript
// app/api/features.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { featuresRouter } from './features'

describe('Features API', () => {
  beforeEach(async () => {
    // Setup test database
    await db.delete(features)
  })

  it('should create a feature', async () => {
    const result = await featuresRouter.create(
      { title: 'Test', description: 'Test description' },
      { request: mockAuthRequest() }
    )

    expect(result.title).toBe('Test')
    expect(result.userId).toBe(mockUser.id)
  })

  it('should require authentication', async () => {
    await expect(
      featuresRouter.list({}, { request: new Request('http://localhost') })
    ).rejects.toThrow('Unauthenticated')
  })

  it('should enforce ownership', async () => {
    const feature = await createFeature({ userId: 'other-user' })

    await expect(
      featuresRouter.get({ id: feature.id }, { request: mockAuthRequest() })
    ).rejects.toThrow('Feature not found')
  })
})
```

### Integration Tests (Playwright)

```typescript
// tests/features.spec.ts
import { test, expect } from '@playwright/test'

test.describe('Features', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('[name="email"]', 'test@example.com')
    await page.fill('[name="password"]', 'password')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')
  })

  test('should create a feature', async ({ page }) => {
    await page.goto('/features')
    await page.click('text=Create Feature')
    await page.fill('[name="title"]', 'New Feature')
    await page.fill('[name="description"]', 'Test description')
    await page.click('button[type="submit"]')

    await expect(page.locator('text=New Feature')).toBeVisible()
  })
})
```

## API Best Practices

1. **Always validate input** with Zod schemas
2. **Check authentication** at the start of every handler
3. **Verify ownership** before read/write operations
4. **Use indexes** for frequently queried fields
5. **Return meaningful errors** with context
6. **Log important events** (errors, auth failures)
7. **Rate limit** expensive operations
8. **Cache responses** when appropriate
9. **Use transactions** for multi-step operations
10. **Version your APIs** for breaking changes

## Common Patterns Summary

| Pattern | Convex | oRPC + D1 |
|---------|--------|-----------|
| **Read data** | `query()` with `ctx.db.query()` | Router method with `db.select()` |
| **Write data** | `mutation()` with `ctx.db.insert()` | Router method with `db.insert()` |
| **Auth check** | `ctx.auth.getUserIdentity()` | `requireAuth(context)` middleware |
| **Real-time** | ✅ Automatic subscription | ❌ Need websockets/polling |
| **Type safety** | ✅ Generated types | ✅ Zod + TypeScript |
| **Caching** | ✅ Built-in | Manual with TanStack Query |
| **Transactions** | ✅ Built-in | Manual with Drizzle |

## Next Steps

Tell me what you want to build:
- [ ] CRUD API for a resource
- [ ] Real-time chat/collaboration
- [ ] File upload/download
- [ ] Search functionality
- [ ] AI integration
- [ ] Third-party API integration
- [ ] Other: ______________

I'll provide complete, production-ready code following Pangolin patterns!
