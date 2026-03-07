---
name: Pangolin Debugging Assistant
description: Debug Pangolin applications - network issues, auth problems, API errors, and more.
category: Debugging
tags: [pangolin, debug, troubleshooting, errors]
---

# Pangolin Debugging Assistant

I'll help you debug your Pangolin application. Let me gather information about the issue you're experiencing.

## Quick Diagnostics

First, let's run some basic health checks. Please provide:

1. **What's the issue?** (Choose one or describe)
   - [ ] Authentication not working
   - [ ] API endpoints returning errors
   - [ ] Pangolin VPN not connecting
   - [ ] Real-time updates not syncing
   - [ ] Database queries failing
   - [ ] Build/deployment errors
   - [ ] Performance issues
   - [ ] Other: _______

2. **Environment:**
   - [ ] Development (local)
   - [ ] Staging
   - [ ] Production

3. **Recent changes:**
   - Did this work before?
   - What changed recently?

## Common Issues & Solutions

### 🔐 Authentication Issues

#### Symptom: "Session not persisting" / "User logged out immediately"

**Diagnostic Steps:**
```bash
# Check environment variables
echo $BETTER_AUTH_SECRET  # Should be 32+ characters
echo $BETTER_AUTH_URL     # Should match deployment URL

# Check browser cookies (DevTools > Application > Cookies)
# Look for: better-auth.session_token

# Check database
# For D1:
npx wrangler d1 execute pangolin-db --command="SELECT * FROM session LIMIT 5;"

# For local SQLite:
sqlite3 .wrangler/state/v3/d1/miniflare-D1DatabaseObject/*.sqlite "SELECT * FROM session;"
```

**Common Causes:**
1. **BETTER_AUTH_SECRET not set** → Set in `.env` or deployment platform
2. **BETTER_AUTH_URL mismatch** → Must exactly match deployment URL (including https://)
3. **Cookies blocked** → Check browser settings, CORS configuration
4. **SameSite cookie issues** → Check cookie attributes in Network tab
5. **Database not accessible** → Verify D1 binding or connection string

**Solutions:**
```typescript
// Verify Better Auth config (app/lib/auth.ts)
export const auth = betterAuth({
  secret: process.env.BETTER_AUTH_SECRET, // Must be set!
  baseURL: process.env.BETTER_AUTH_URL,   // Must match deployment!
  database: {
    // Check connection is correct
  }
})

// Debug session creation
auth.api.getSession({ headers: request.headers })
  .then(session => console.log('Session:', session))
  .catch(err => console.error('Session error:', err))
```

#### Symptom: "OAuth redirect not working"

**Diagnostic Steps:**
```bash
# Check OAuth credentials
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET
echo $GITHUB_CLIENT_ID
echo $GITHUB_CLIENT_SECRET

# Verify redirect URIs in provider console match exactly:
# Google: https://console.cloud.google.com
# GitHub: https://github.com/settings/developers

# Check callback URL format
# Should be: https://your-app.com/api/auth/callback/google
```

**Common Causes:**
1. **Redirect URI mismatch** → Add exact URL to OAuth app settings
2. **Client credentials wrong** → Verify copied correctly
3. **OAuth app not enabled** → Check app is active in provider console
4. **Scopes insufficient** → Ensure email scope requested

**Solutions:**
```typescript
// Verify OAuth config (app/lib/auth.ts)
socialProviders: {
  google: {
    clientId: process.env.GOOGLE_CLIENT_ID!,
    clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    // redirectURI is auto-generated as: {baseURL}/api/auth/callback/google
  }
}
```

---

### 🌐 API & oRPC Issues

#### Symptom: "404 Not Found on API endpoint"

**Diagnostic Steps:**
```bash
# Check file structure
ls -R app/routes/api/

# Verify oRPC route exists
# Should be: app/routes/api/rpc.ts (or similar)

# Check TanStack Router routes
cat app/routes.gen.ts | grep api

# Test endpoint directly
curl -X POST http://localhost:3000/api/rpc/chat.list \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Common Causes:**
1. **File naming wrong** → Must follow TanStack Router conventions
2. **Route not exported** → Check export default or export const
3. **Build cache stale** → Clear `.vite` cache
4. **Dev server not restarted** → Restart after adding routes

**Solutions:**
```typescript
// Correct oRPC route structure (app/routes/api/rpc.ts)
import { createFileRoute } from '@tanstack/react-router'
import { orpcHandler } from '~/lib/orpc'
import { chatRouter } from '~/api/chat'

export const Route = createFileRoute('/api/rpc')({
  loader: async ({ request }) => {
    return await orpcHandler({
      request,
      router: chatRouter
    })
  }
})
```

#### Symptom: "Type errors in oRPC client"

**Diagnostic Steps:**
```bash
# Regenerate oRPC client types
npm run orpc:generate

# Check TypeScript version
npm list typescript

# Verify Zod schemas match
cat app/api/schemas.ts
```

**Common Causes:**
1. **Schema out of sync** → Regenerate client types
2. **Zod version mismatch** → Update to latest
3. **TypeScript strict mode** → Check tsconfig.json

**Solutions:**
```typescript
// Ensure schema is properly exported
export const createChatSchema = z.object({
  title: z.string(),
  userId: z.string()
})

export type CreateChatInput = z.infer<typeof createChatSchema>

// Use in router
router.create(async (input: CreateChatInput) => {
  const validated = createChatSchema.parse(input)
  // ...
})
```

---

### 🔄 Real-time & Convex Issues

#### Symptom: "Convex not syncing" / "useQuery returns undefined"

**Diagnostic Steps:**
```bash
# Check Convex deployment
npx convex dev  # Should show connected

# Verify environment variable
echo $CONVEX_DEPLOYMENT

# Check browser console
# Should see: "Convex client connected"

# Test query directly in Convex dashboard
# https://dashboard.convex.dev
```

**Common Causes:**
1. **Convex not deployed** → Run `npx convex dev` or `npx convex deploy`
2. **CONVEX_DEPLOYMENT wrong** → Copy from Convex dashboard
3. **Network blocked** → Check firewall/CORS
4. **Auth not configured** → Set up Convex auth

**Solutions:**
```typescript
// Verify Convex client setup (app/lib/convex.ts)
import { ConvexReactClient } from 'convex/react'

export const convex = new ConvexReactClient(
  import.meta.env.VITE_CONVEX_DEPLOYMENT! // Must be set!
)

// Check provider wraps app (app/routes/__root.tsx)
<ConvexProvider client={convex}>
  <Outlet />
</ConvexProvider>

// Debug query
const messages = useQuery(api.messages.list, { sessionId: '123' })
console.log('Messages:', messages) // undefined = loading, null = no data
```

#### Symptom: "Convex mutation fails with auth error"

**Diagnostic Steps:**
```bash
# Check Convex auth config
cat convex/auth.config.ts

# Verify auth middleware
cat convex/_generated/auth.ts

# Test auth in Convex dashboard
```

**Common Causes:**
1. **Auth not set up** → Configure Convex auth provider
2. **Token not sent** → Check useConvexAuth hook
3. **User identity not found** → Verify getUserIdentity() works

**Solutions:**
```typescript
// Convex mutation with auth (convex/mutations/chat.ts)
import { mutation } from '../_generated/server'
import { v } from 'convex/values'

export const create = mutation({
  args: { title: v.string() },
  handler: async (ctx, args) => {
    // Get authenticated user
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) {
      throw new Error('Unauthenticated')
    }

    // Create with ownership
    return await ctx.db.insert('chats', {
      title: args.title,
      userId: identity.subject,
      createdAt: Date.now()
    })
  }
})
```

---

### 🌐 Pangolin VPN & Network Issues

#### Symptom: "VPN not connecting" / "Gerbil agent offline"

**Diagnostic Steps:**
```bash
# Check Pangolin services
docker ps | grep -E "(pangolin|gerbil|traefik)"

# Check logs
docker logs pangolin-pangolin-1
docker logs pangolin-gerbil-1
docker logs pangolin-traefik-1

# Verify ports
netstat -tuln | grep -E "(51820|3001|80|443)"

# Test UDP port (WireGuard)
nc -vzu localhost 51820

# Check Pangolin API
curl http://localhost:3001/api/v1/
```

**Common Causes:**
1. **Services not running** → Start with `docker compose up -d`
2. **Port 51820 blocked** → Open UDP port in firewall
3. **WireGuard config wrong** → Regenerate from Pangolin dashboard
4. **Network interface conflict** → Check `ip addr` for conflicts

**Solutions:**
```bash
# Restart Pangolin stack
cd infrastructure/compose/pangolin
docker compose down
docker compose up -d

# Check Gerbil config
docker exec pangolin-gerbil-1 cat /etc/wireguard/wg0.conf

# Test Traefik routing
curl -H "Host: your-service.local" http://localhost:80

# View Traefik dashboard
open http://localhost:8080
```

#### Symptom: "HTTPS certificate not working"

**Diagnostic Steps:**
```bash
# Check Traefik logs for ACME (Let's Encrypt)
docker logs pangolin-traefik-1 | grep acme

# Verify DNS points to server
dig your-domain.com

# Check certificate
openssl s_client -connect your-domain.com:443 -servername your-domain.com

# Test HTTP challenge
curl http://your-domain.com/.well-known/acme-challenge/
```

**Common Causes:**
1. **DNS not propagated** → Wait 5-15 minutes
2. **Port 80/443 not open** → Check firewall rules
3. **Email not set** → Set LETSENCRYPT_EMAIL in .env
4. **Rate limit hit** → Let's Encrypt has limits (5 certs/week)

**Solutions:**
```yaml
# Check Traefik config (docker-compose.yml)
traefik:
  command:
    - --certificatesresolvers.letsencrypt.acme.email=${LETSENCRYPT_EMAIL}
    - --certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web

# Force certificate renewal
docker exec pangolin-traefik-1 rm /acme.json
docker restart pangolin-traefik-1
```

---

### 💾 Database Issues

#### Symptom: "Database query returns empty / null"

**Diagnostic Steps:**
```bash
# Check database connection
# For D1:
npx wrangler d1 execute pangolin-db --command="SELECT count(*) FROM sqlite_master WHERE type='table';"

# For Convex:
# Open dashboard: https://dashboard.convex.dev

# Verify data exists
npx wrangler d1 execute pangolin-db --command="SELECT * FROM chats LIMIT 5;"

# Check indexes
npx wrangler d1 execute pangolin-db --command="SELECT * FROM sqlite_master WHERE type='index';"
```

**Common Causes:**
1. **Migrations not applied** → Run `npm run db:migrate`
2. **Wrong database** → Check environment (dev vs prod)
3. **Ownership filter too strict** → Verify userId matches
4. **Index missing** → Add index for query

**Solutions:**
```typescript
// Verify Convex query (convex/queries/chats.ts)
export const list = query({
  args: {},
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity()
    if (!identity) return [] // Return empty instead of throwing

    // Add logging
    console.log('Querying for user:', identity.subject)

    const chats = await ctx.db
      .query('chats')
      .withIndex('by_user', (q) => q.eq('userId', identity.subject))
      .collect()

    console.log('Found chats:', chats.length)
    return chats
  }
})
```

---

### 🚀 Build & Performance Issues

#### Symptom: "Build fails with type errors"

**Diagnostic Steps:**
```bash
# Run type check
npm run typecheck

# Check specific file
npx tsc --noEmit app/routes/problematic.tsx

# Update dependencies
npm update

# Clear cache
rm -rf node_modules/.vite
npm install
```

**Common Causes:**
1. **TypeScript version mismatch** → Update to latest
2. **Missing type definitions** → Install @types/* packages
3. **Path alias not configured** → Check tsconfig.json
4. **Strict mode errors** → Fix type assertions

#### Symptom: "App is slow / high latency"

**Diagnostic Steps:**
```bash
# Run Lighthouse audit
npm run build
npm run preview
# Open Chrome DevTools > Lighthouse

# Check bundle size
npm run build
ls -lh dist/

# Profile with TanStack Query Devtools
# Add: import { TanStackQueryDevtools } from '@tanstack/react-query-devtools'
```

**Common Causes:**
1. **Too many re-renders** → Check React DevTools Profiler
2. **Slow queries** → Add indexes, optimize Convex queries
3. **Large bundle** → Code split, lazy load routes
4. **No caching** → Configure TanStack Query staleTime

**Solutions:**
```typescript
// Optimize query with caching
const chats = useConvexQuery(
  api.chats.list,
  {},
  {
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000    // 10 minutes
  }
)

// Lazy load routes
const FeaturePage = lazy(() => import('./routes/feature'))

// Memoize expensive computations
const processedData = useMemo(() => {
  return heavyComputation(data)
}, [data])
```

---

## Debug Tools

### Browser DevTools
```javascript
// In browser console:

// Check auth state
localStorage.getItem('better-auth.session_token')

// Check Convex state
window.convex?.sync?.queryResults

// Check TanStack Query cache
window.__TANSTACK_QUERY_DEVTOOLS__

// Network inspection
// DevTools > Network > Filter by "api" or "convex"
```

### Server-Side Logging
```typescript
// Add debug logging (remove in production!)
console.log('Debug point 1:', { variable1, variable2 })

// Convex functions
console.log('Convex handler called:', { args, identity })

// API routes
console.log('API request:', {
  method: request.method,
  url: request.url,
  headers: Object.fromEntries(request.headers)
})
```

### Docker Debugging
```bash
# View all container logs
docker compose logs -f

# Enter container shell
docker exec -it pangolin-app-1 sh

# Check container network
docker network inspect pangolin_default

# View container resources
docker stats
```

## Next Steps

To help debug effectively, please provide:

1. **Exact error message** (copy-paste from console/logs)
2. **Steps to reproduce** (what actions trigger the issue?)
3. **Expected vs actual behavior**
4. **Relevant code snippets** (component, query, mutation)
5. **Environment** (local dev, staging, production)

I'll provide targeted debugging steps and solutions!
