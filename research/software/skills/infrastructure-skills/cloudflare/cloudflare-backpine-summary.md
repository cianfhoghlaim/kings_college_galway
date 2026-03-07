# Cloudflare Full-Stack Repository Summary

## Overview

The **web/backpine** repository is a production-grade monorepo demonstrating advanced Cloudflare Workers platform features for building a smart link routing and monitoring service. It showcases geo-based routing, AI-powered link evaluation, real-time analytics, and subscription management.

---

## Repository Structure

### Monorepo Architecture

```
web/backpine/
├── apps/
│   ├── user-application/          # Frontend + API (React + TanStack Router + TRPC)
│   │   ├── worker/                # Cloudflare Worker entrypoint
│   │   │   ├── index.ts          # Main worker entry
│   │   │   ├── hono/app.ts       # Hono server + routing
│   │   │   └── trpc/             # Type-safe API layer
│   │   └── src/
│   │       ├── components/       # React components
│   │       └── routes/           # TanStack Router routes
│   │
│   └── data-service/             # Backend service (Hono + Cloudflare features)
│       └── src/
│           ├── index.ts          # WorkerEntrypoint class
│           ├── hono/app.ts       # Smart routing API
│           ├── workflows/        # Cloudflare Workflows
│           ├── durable-objects/  # Durable Objects
│           ├── queue-handlers/   # Queue consumers
│           └── helpers/          # AI, routing, browser logic
│
└── packages/
    └── data-ops/                 # Shared database, auth, queries
        └── src/
            ├── db/database.ts    # Drizzle ORM initialization
            ├── auth.ts           # Better Auth configuration
            ├── queries/          # Database queries
            └── zod/              # Runtime validation schemas
```

---

## Core Code Locations

### User Application (`apps/user-application/`)

| Component | File Path | Description |
|-----------|-----------|-------------|
| **Worker Entry** | `worker/index.ts:4` | Cloudflare Worker entrypoint, initializes database |
| **API Server** | `worker/hono/app.ts:8` | Hono server with Better Auth + TRPC |
| **Auth Middleware** | `worker/hono/app.ts:41` | Session validation middleware |
| **TRPC Router** | `worker/trpc/router.ts:1` | Main API router combining sub-routers |
| **Link Operations** | `worker/trpc/routers/links.ts:1` | CRUD operations for links |
| **Evaluations API** | `worker/trpc/routers/evaluations.ts:1` | Fetch evaluation results |
| **Protected Routes** | `src/routes/app/_authed.tsx:8` | Auth-protected TanStack routes |
| **Dashboard** | `src/components/dashboard/index.tsx:1` | Main dashboard component |
| **Link Management** | `src/components/link/` | Link editing components |
| **Payments** | `src/components/payments/upgrade-page.tsx:1` | Stripe subscription UI |
| **Auth Client** | `src/components/auth/client.ts:1` | Better Auth React client |
| **Vite Config** | `vite.config.ts:1` | Cloudflare + TanStack plugins |

### Data Service (`apps/data-service/`)

| Component | File Path | Description |
|-----------|-----------|-------------|
| **Worker Entry** | `src/index.ts:10` | WorkerEntrypoint with queue handler |
| **Smart Routing** | `src/hono/app.ts:28` | Geo-based link redirection endpoint |
| **Queue Handler** | `src/index.ts:20` | Processes link click events |
| **Evaluation Workflow** | `src/workflows/destination-evalutation-workflow.ts:8` | Multi-step AI evaluation |
| **Evaluation Scheduler** | `src/durable-objects/evaluation-scheduler.ts:13` | Alarm-based scheduling |
| **Click Tracker** | `src/durable-objects/link-click-tracker.ts:5` | SQL + WebSocket real-time tracking |
| **Browser Rendering** | `src/helpers/browser-render.ts:3` | Puppeteer screenshot/content extraction |
| **AI Checker** | `src/helpers/ai-destination-checker.ts:5` | Workers AI product detection |
| **Routing Helpers** | `src/helpers/route-ops.ts:1` | KV caching, geo-routing logic |

### Shared Package (`packages/data-ops/`)

| Component | File Path | Description |
|-----------|-----------|-------------|
| **Database Init** | `src/db/database.ts:5` | Drizzle ORM D1 initialization |
| **Auth Config** | `src/auth.ts:22` | Better Auth + Stripe plugin setup |
| **Link Queries** | `src/queries/links.ts:1` | Database operations for links |
| **Evaluation Queries** | `src/queries/evaluations.ts:1` | Database operations for evaluations |

---

## Cloudflare Features Integration

### 1. D1 Database (SQLite)

**Location**: `packages/data-ops/src/db/database.ts:1`

```typescript
// Database initialization
import { drizzle } from "drizzle-orm/d1";

let db: ReturnType<typeof drizzle>;

export function initDatabase(bindingDb: D1Database) {
  db = drizzle(bindingDb);
}
```

**Usage**:
- User/auth tables via Better Auth adapter
- Links and destinations storage
- Evaluation results
- Type-safe queries with Drizzle ORM

**Example Query** (`packages/data-ops/src/queries/links.ts`):
```typescript
export async function getLink(id: string) {
  const db = getDb();
  return await db.query.links.findFirst({
    where: eq(links.id, id)
  });
}
```

---

### 2. KV (Key-Value Store)

**Location**: `apps/data-service/src/helpers/route-ops.ts:6`

**Pattern**: Read-through cache with 24-hour TTL

```typescript
async function getLinkInfoFromKv(env: Env, id: string) {
  const linkInfo = await env.CACHE.get(id); // KV lookup
  if (!linkInfo) return null;
  try {
    const parsedLinkInfo = JSON.parse(linkInfo);
    return linkSchema.parse(parsedLinkInfo);
  } catch (error) {
    return null;
  }
}

const TTL_TIME = 60 * 60 * 24; // 1 day

async function saveLinkInfoToKv(env: Env, id: string, linkInfo: LinkSchemaType) {
  await env.CACHE.put(id, JSON.stringify(linkInfo), {
    expirationTtl: TTL_TIME
  });
}
```

**Benefits**:
- Reduces D1 read load
- Low-latency link lookups
- Automatic expiration

---

### 3. R2 (Object Storage)

**Location**: `apps/data-service/src/workflows/destination-evalutation-workflow.ts:32`

**Storage Structure**:
```
evaluations/
└── {accountId}/
    ├── html/{evaluationId}                    # Full HTML snapshot
    ├── body-text/{evaluationId}               # Extracted text
    └── screenshots/{evaluationId}.png         # Page screenshot
```

**Implementation**:
```typescript
const r2PathHtml = `evaluations/${accountId}/html/${evaluationId}`;
const r2PathBodyText = `evaluations/${accountId}/body-text/${evaluationId}`;
const r2PathScreenshot = `evaluations/${accountId}/screenshots/${evaluationId}.png`;

// Convert base64 to buffer
const screenshotBase64 = data.screenshotDataUrl.replace(/^data:image\/png;base64,/, '');
const screenshotBuffer = Buffer.from(screenshotBase64, 'base64');

await this.env.BUCKET.put(r2PathHtml, data.html);
await this.env.BUCKET.put(r2PathBodyText, data.bodyText);
await this.env.BUCKET.put(r2PathScreenshot, screenshotBuffer);
```

---

### 4. Cloudflare Workers

#### User Application Worker
**Location**: `apps/user-application/worker/index.ts:4`

```typescript
import { initDatabase } from "@repo/data-ops/database";
import { App } from "./hono/app";

export default {
  fetch(request, env, ctx) {
    initDatabase(env.DB);
    return App.fetch(request, env, ctx)
  },
} satisfies ExportedHandler<ServiceBindings>;
```

**Features**:
- Serves React application with SSR
- Hono API routes (TRPC, Auth)
- Service-to-service bindings
- Integrated via Vite plugin: `vite.config.ts:4`

#### Data Service Worker
**Location**: `apps/data-service/src/index.ts:10`

```typescript
export default class DataService extends WorkerEntrypoint<Env> {
  constructor(ctx: ExecutionContext, env: Env) {
    super(ctx, env);
    initDatabase(env.DB);
  }

  fetch(request: Request) {
    return App.fetch(request, this.env, this.ctx);
  }

  async queue(batch: MessageBatch<unknown>) {
    for (const message of batch.messages) {
      const parsedEvent = QueueMessageSchema.safeParse(message.body);
      if (parsedEvent.success && event.type === "LINK_CLICK") {
        await handleLinkClick(this.env, event);
      }
    }
  }
}
```

---

### 5. Durable Objects

#### A. EvaluationScheduler

**Location**: `apps/data-service/src/durable-objects/evaluation-scheduler.ts:13`

**Pattern**: Alarm-based delayed workflow triggering

```typescript
export class EvaluationScheduler extends DurableObject<Env> {
  clickData: ClickData | undefined;

  // Collect click data and schedule alarm
  async collectLinkClick(accountId: string, linkId: string,
                         destinationUrl: string, destinationCountryCode: string) {
    this.clickData = { accountId, linkId, destinationUrl, destinationCountryCode };
    await this.ctx.storage.put('click_data', this.clickData);

    const alarm = await this.ctx.storage.getAlarm();
    if (!alarm) {
      const oneDay = moment().add(24, "hours").valueOf();
      await this.ctx.storage.setAlarm(oneDay);
    }
  }

  // Triggered after 24 hours
  async alarm() {
    const clickData = this.clickData;
    if (!clickData) throw new Error("Click data not set");

    // Trigger evaluation workflow
    await this.env.DESTINATION_EVALUATION_WORKFLOW.create({
      params: {
        linkId: clickData.linkId,
        accountId: clickData.accountId,
        destinationUrl: clickData.destinationUrl
      }
    });
  }
}
```

**Use Case**: Delay evaluations to detect broken/changed links over time

---

#### B. LinkClickTracker

**Location**: `apps/data-service/src/durable-objects/link-click-tracker.ts:5`

**Pattern**: SQL-backed storage + WebSocket broadcasting

```typescript
export class LinkClickTracker extends DurableObject {
  sql: SqlStorage;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    this.sql = ctx.storage.sql;

    // Create SQL table
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS geo_link_clicks (
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        country TEXT NOT NULL,
        time INTEGER NOT NULL
      )
    `);
  }

  // Store click in SQL
  async addClick(latitude: number, longitude: number, country: string, time: number) {
    this.sql.exec(
      `INSERT INTO geo_link_clicks (latitude, longitude, country, time)
       VALUES (?, ?, ?, ?)`,
      latitude, longitude, country, time
    );

    // Schedule alarm for batch processing
    const alarm = await this.ctx.storage.getAlarm();
    if (!alarm) await this.ctx.storage.setAlarm(moment().add(2, 'seconds').valueOf());
  }

  // Alarm triggers batch WebSocket broadcast
  async alarm() {
    const clickData = getRecentClicks(this.sql, this.mostRecentOffsetTime);

    const sockets = this.ctx.getWebSockets();
    for (const socket of sockets) {
      socket.send(JSON.stringify(clickData.clicks));
    }

    await this.flushOffsetTimes(clickData.mostRecentTime, clickData.oldestTime);
    deleteClicksBefore(this.sql, clickData.oldestTime);
  }

  // WebSocket handler
  async fetch(_: Request) {
    const webSocketPair = new WebSocketPair();
    const [client, server] = Object.values(webSocketPair);
    this.ctx.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: client });
  }
}
```

**Features**:
- **SQL Storage**: Persistent click data within Durable Object
- **Real-time Broadcasting**: Alarm-based batch updates every 2 seconds
- **WebSocket Support**: Direct connection from clients
- **Automatic Cleanup**: Deletes old clicks after broadcasting

---

### 6. Workflows

**Location**: `apps/data-service/src/workflows/destination-evalutation-workflow.ts:8`

**Multi-Step Evaluation Pipeline**:

```typescript
export class DestinationEvaluationWorkflow extends WorkflowEntrypoint<Env, Params> {
  async run(event: Readonly<WorkflowEvent<Params>>, step: WorkflowStep) {
    initDatabase(this.env.DB);

    // Step 1: Browser render with retry
    const evaluationInfo = await step.do(
      'Collect rendered destination page data',
      { retries: { limit: 1, delay: 1000 } },
      async () => {
        const evaluationId = uuidv4();
        const data = await collectDestinationInfo(this.env, event.payload.destinationUrl);

        // Save to R2
        const screenshotBase64 = data.screenshotDataUrl.replace(/^data:image\/png;base64,/, '');
        const screenshotBuffer = Buffer.from(screenshotBase64, 'base64');

        await this.env.BUCKET.put(`evaluations/${accountId}/html/${evaluationId}`, data.html);
        await this.env.BUCKET.put(`evaluations/${accountId}/body-text/${evaluationId}`, data.bodyText);
        await this.env.BUCKET.put(`evaluations/${accountId}/screenshots/${evaluationId}.png`, screenshotBuffer);

        return { bodyText: data.bodyText, evaluationId };
      }
    );

    // Step 2: AI analysis
    const aiStatus = await step.do(
      'Use AI to check status of page',
      { retries: { limit: 0, delay: 0 } },
      async () => {
        return await aiDestinationChecker(this.env, evaluationInfo.bodyText);
      }
    );

    // Step 3: Save to database
    await step.do('Save evaluation in database', async () => {
      return await addEvaluation({
        evaluationId: evaluationInfo.evaluationId,
        linkId: event.payload.linkId,
        status: aiStatus.status,
        reason: aiStatus.statusReason,
        accountId: event.payload.accountId,
        destinationUrl: event.payload.destinationUrl,
      });
    });
  }
}
```

**Benefits**:
- **Automatic Retries**: Configurable per step
- **State Persistence**: Survives worker restarts
- **Observability**: Built-in step tracking

---

### 7. Queues

#### Producer
**Location**: `apps/data-service/src/helpers/route-ops.ts:68`

```typescript
export async function captureLinkClickInBackground(env: Env, event: LinkClickMessageType) {
  await env.QUEUE.send(event);

  // Also update Durable Object
  const doId = env.LINK_CLICK_TRACKER_OBJECT.idFromName(event.data.accountId);
  const stub = env.LINK_CLICK_TRACKER_OBJECT.get(doId);
  await stub.addClick(
    event.data.latitude,
    event.data.longitude,
    event.data.country,
    moment().valueOf()
  );
}
```

#### Consumer
**Location**: `apps/data-service/src/index.ts:20`

```typescript
async queue(batch: MessageBatch<unknown>) {
  for (const message of batch.messages) {
    const parsedEvent = QueueMessageSchema.safeParse(message.body);
    if (parsedEvent.success) {
      const event = parsedEvent.data;
      if (event.type === "LINK_CLICK") {
        await handleLinkClick(this.env, event);
      }
    } else {
      console.error(parsedEvent.error);
    }
  }
}
```

**Pattern**: Type-safe message validation with Zod schemas

---

### 8. Workers AI

**Location**: `apps/data-service/src/helpers/ai-destination-checker.ts:5`

**Structured Output Generation**:

```typescript
import { generateObject } from 'ai';
import { createWorkersAI } from 'workers-ai-provider';
import { z } from 'zod';

export async function aiDestinationChecker(env: Env, bodyText: string) {
  const workersAi = createWorkersAI({ binding: env.AI });

  const result = await generateObject({
    mode: 'json',
    model: workersAi('@cf/meta/llama-3.3-70b-instruct-fp8-fast' as any),
    prompt: `You will analyze the provided webpage content and determine if it reflects
             a product that is currently available, not available, or if the status is unclear.

             Webpage Content:
             ${bodyText}`,
    system: `You are an AI assistant for ecommerce analysis...`,
    schema: z.object({
      pageStatus: z.object({
        status: z.enum(['AVAILABLE_PRODUCT', 'NOT_AVAILABLE_PRODUCT', 'UNKNOWN_STATUS']),
        statusReason: z.string().describe('A concise explanation...')
      })
    })
  });

  return {
    status: result.object.pageStatus.status,
    statusReason: result.object.pageStatus.statusReason,
  };
}
```

**Features**:
- **Model**: Llama 3.3 70B (fast inference)
- **Structured Output**: Zod schema validation
- **Use Case**: Product availability detection from webpage text

---

### 9. Browser Rendering API

**Location**: `apps/data-service/src/helpers/browser-render.ts:3`

```typescript
import puppeteer from '@cloudflare/puppeteer';

export async function collectDestinationInfo(env: Env, destinationUrl: string) {
  const browser = await puppeteer.launch(env.VIRTUAL_BROWSER);
  const page = await browser.newPage();
  const response = await page.goto(destinationUrl);
  await page.waitForNetworkIdle();

  const bodyText = (await page.$eval('body', (el) => el.innerText)) as string;
  const html = await page.content();
  const status = response ? response.status() : 0;

  const screenshot = await page.screenshot({ encoding: 'base64' });
  const screenshotDataUrl = `data:image/png;base64,${screenshot}`;

  await browser.close();
  return { status, bodyText, html, screenshotDataUrl };
}
```

**Capabilities**:
- Full Chrome browser in Worker
- JavaScript rendering
- Screenshot capture
- Network inspection

---

## Better Auth + Stripe Integration

### Better Auth Configuration

**Location**: `packages/data-ops/src/auth.ts:22`

```typescript
import { betterAuth } from "better-auth";
import { drizzleAdapter } from "better-auth/adapters/drizzle";
import { stripe } from "@better-auth/stripe";
import Stripe from "stripe";

export function getAuth(
  google: { clientId: string; clientSecret: string },
  stripe: StripeConfig,
  secret: string,
): ReturnType<typeof betterAuth> {
  if (auth) return auth;

  auth = createBetterAuth(
    drizzleAdapter(getDb(), {
      provider: "sqlite",
      schema: { user, session, account, verification, subscription }
    }),
    secret,
    stripe,
    google,
  );
  return auth;
}
```

**Features**:
- **Adapter**: Drizzle for D1 SQLite storage
- **Provider**: Google OAuth only (email/password disabled)
- **Stripe Plugin**: Automatic subscription management

---

### Stripe Integration

**Configuration** (`apps/user-application/worker/hono/app.ts:13`):

```typescript
const getAuthInstance = (env: Env) => {
  return getAuth(
    { clientId: env.GOOGLE_CLIENT_ID, clientSecret: env.GOOGLE_CLIENT_SECRET },
    {
      stripeWebhookSecret: env.STRIPE_WEBHOOK_KEY,
      stripeApiKey: env.STRIPE_SECRET_KEY,
      plans: [
        { name: "basic", priceId: env.STRIPE_PRODUCT_BASIC },
        { name: "pro", priceId: env.STRIPE_PRODUCT_PRO },
        { name: "enterprise", priceId: env.STRIPE_PRODUCT_ENTERPRISE },
      ],
    },
    env.APP_SECRET,
  );
};
```

**Plugin Features** (`packages/data-ops/src/auth.ts:41`):
- Auto customer creation on signup: `createCustomerOnSignUp: true`
- Subscription management with 3 plans
- Webhook handling for payment events

**UI Components**:
- **Upgrade Page**: `src/components/payments/upgrade-page.tsx:1` - Plan selection and checkout
- **Cancel Dialog**: `src/components/payments/cancel-subscription-dialog.tsx:1` - Cancellation flow
- **Status Sidebar**: `src/components/payments/subscription-status-sidebar.tsx:1` - Current plan display

---

### Auth Middleware

**Location**: `apps/user-application/worker/hono/app.ts:41`

```typescript
const authMiddleware = createMiddleware(async (c, next) => {
  const auth = getAuthInstance(c.env);
  const session = await auth.api.getSession({ headers: c.req.raw.headers });
  if (!session?.user) {
    return c.text("Unauthorized", 401);
  }
  const userId = session.user.id;
  c.set("userId", userId);
  await next();
});

// Protected routes
App.all("/trpc/*", authMiddleware, (c) => { ... });
App.get("/click-socket", authMiddleware, async (c) => { ... });
```

---

## Hono + TanStack Integration

### Hono API Layer

#### User Application
**Location**: `apps/user-application/worker/hono/app.ts:8`

```typescript
export const App = new Hono<{
  Bindings: ServiceBindings;
  Variables: { userId: string };
}>();

// TRPC integration
App.all("/trpc/*", authMiddleware, (c) => {
  const userId = c.get("userId");
  return fetchRequestHandler({
    endpoint: "/trpc",
    req: c.req.raw,
    router: appRouter,
    createContext: () => createContext({ req: c.req.raw, env: c.env, workerCtx: c.executionCtx, userId }),
  });
});

// WebSocket proxy (service-to-service binding)
App.get("/click-socket", authMiddleware, async (c) => {
  const userId = c.get("userId");
  const headers = new Headers(c.req.raw.headers);
  headers.set("account-id", userId);
  const proxiedRequest = new Request(c.req.raw, { headers });
  return c.env.BACKEND_SERVICE.fetch(proxiedRequest);
});

// Auth routes
App.on(["POST", "GET"], "/api/auth/*", (c) => {
  const auth = getAuthInstance(c.env);
  return auth.handler(c.req.raw);
});
```

---

#### Data Service
**Location**: `apps/data-service/src/hono/app.ts:8`

```typescript
export const App = new Hono<{ Bindings: Env }>();

App.use('*', cors());

// WebSocket endpoint for real-time clicks
App.get('/click-socket', async (c) => {
  const upgradeHeader = c.req.header('Upgrade');
  if (!upgradeHeader || upgradeHeader !== 'websocket') {
    return c.text('Expected Upgrade: websocket', 426);
  }

  const accountId = c.req.header('account-id');
  if (!accountId) return c.text('No Headers', 404);

  const doId = c.env.LINK_CLICK_TRACKER_OBJECT.idFromName(accountId);
  const stub = c.env.LINK_CLICK_TRACKER_OBJECT.get(doId);
  return await stub.fetch(c.req.raw);
});

// Smart geo-routing endpoint
App.get('/:id', async (c) => {
  const id = c.req.param('id');

  // Get link info (KV cache → D1 fallback)
  const linkInfo = await getRoutingDestinations(c.env, id);
  if (!linkInfo) return c.text('Destination not found', 404);

  // Parse Cloudflare headers
  const cfHeader = cloudflareInfoSchema.safeParse(c.req.raw.cf);
  if (!cfHeader.success) return c.text('Invalid Cloudflare headers', 400);

  // Select destination by country
  const headers = cfHeader.data;
  const destination = getDestinationForCountry(linkInfo, headers.country);

  // Queue click event (async)
  const queueMessage: LinkClickMessageType = {
    type: "LINK_CLICK",
    data: {
      id, country: headers.country, destination,
      accountId: linkInfo.accountId,
      latitude: headers.latitude,
      longitude: headers.longitude,
      timestamp: new Date().toISOString()
    }
  };
  c.executionCtx.waitUntil(captureLinkClickInBackground(c.env, queueMessage));

  return c.redirect(destination);
});
```

**Smart Routing Flow**:
1. Parse Cloudflare request headers (`cf.country`, `cf.latitude`, `cf.longitude`)
2. Check KV cache for link configuration
3. Fallback to D1 if cache miss
4. Select appropriate destination by country code
5. Send click event to Queue (async with `waitUntil`)
6. Redirect user to destination

---

### TanStack Router

**Plugin Configuration**: `vite.config.ts:12`

```typescript
import { tanstackRouter } from "@tanstack/router-plugin/vite";

export default defineConfig({
  plugins: [
    tanstackRouter({ autoCodeSplitting: true }),
    viteReact(),
    tailwindcss(),
    cloudflare(),
  ],
});
```

**Protected Routes**: `src/routes/app/_authed.tsx:10`

```typescript
export const Route = createFileRoute("/app/_authed")({
  component: RouteComponent,
  beforeLoad: async () => {
    const session = await authClient.getSession();
    if (!session.data?.session) {
      throw redirect({to: "/"});
    }
  }
});
```

**Features**:
- File-based routing
- Auto code splitting per route
- Type-safe route definitions
- Session-based protection

---

### TRPC Type-Safe APIs

**Server Setup** (`apps/user-application/worker/trpc/`):

```typescript
// router.ts
import { linksRouter } from './routers/links';
import { evaluationsRouter } from './routers/evaluations';
import { createTRPCRouter } from './trpc-instance';

export const appRouter = createTRPCRouter({
  links: linksRouter,
  evaluations: evaluationsRouter,
});

export type AppRouter = typeof appRouter;
```

**Context** (`context.ts`):
```typescript
export const createContext = ({ req, env, workerCtx, userId }) => ({
  req,
  env,
  workerCtx,
  userId,
});
```

**Client Usage** (inferred from structure):
```typescript
import { trpc } from '@/utils/trpc';

// In React component
const { data: links } = trpc.links.list.useQuery();
const createLink = trpc.links.create.useMutation();
```

---

## Complete Flow Examples

### 1. Smart Geo-Routing Flow

```
User clicks link (e.g., example.com/abc123)
    ↓
[Data Service Worker] apps/data-service/src/hono/app.ts:28
    ↓
Parse Cloudflare headers (country, lat, long) → line 36
    ↓
Check KV cache → apps/data-service/src/helpers/route-ops.ts:6
    ↓ (if cache miss)
Query D1 database → line 35
    ↓
Save to KV cache (24h TTL) → line 19
    ↓
Select destination by country → line 42
    ↓
Send click event to Queue → line 56
    ↓ (async background)
Update Durable Object with geo data → route-ops.ts:70
    ↓
Redirect user to destination → hono/app.ts:59
```

**Key Files**:
- Smart routing: `apps/data-service/src/hono/app.ts:28`
- KV caching: `apps/data-service/src/helpers/route-ops.ts:6`
- Queue producer: `apps/data-service/src/helpers/route-ops.ts:68`

---

### 2. AI Evaluation Workflow

```
Link click captured in Queue
    ↓
[Queue Consumer] apps/data-service/src/index.ts:20
    ↓
Parse & validate message → src/queue-handlers/link-clicks.ts:1
    ↓
[Durable Object] Schedule evaluation → src/durable-objects/evaluation-scheduler.ts:23
    ↓
Store click data in DO storage → line 30
    ↓
Set 24-hour alarm → line 34
    ↓
⏰ [24 hours later]
    ↓
Alarm fires → line 40
    ↓
Trigger Workflow → line 47
    ↓
[Workflow Step 1] Browser render → src/workflows/destination-evalutation-workflow.ts:12
    ├─ Launch Puppeteer → src/helpers/browser-render.ts:4
    ├─ Capture HTML, text, screenshot → line 9
    └─ Save to R2 → workflow:32
        ├─ evaluations/{accountId}/html/{evaluationId}
        ├─ evaluations/{accountId}/body-text/{evaluationId}
        └─ evaluations/{accountId}/screenshots/{evaluationId}.png
    ↓
[Workflow Step 2] AI analysis → workflow:42
    ├─ Initialize Workers AI → src/helpers/ai-destination-checker.ts:6
    ├─ Generate structured output (Llama 3.3) → line 7
    └─ Return status + reason → line 49
    ↓
[Workflow Step 3] Save to D1 → workflow:55
    └─ Store evaluation results → packages/data-ops/src/queries/evaluations.ts:1
```

**Key Files**:
- Queue handler: `apps/data-service/src/index.ts:20`
- Scheduler DO: `apps/data-service/src/durable-objects/evaluation-scheduler.ts:13`
- Workflow: `apps/data-service/src/workflows/destination-evalutation-workflow.ts:8`
- Browser render: `apps/data-service/src/helpers/browser-render.ts:3`
- AI checker: `apps/data-service/src/helpers/ai-destination-checker.ts:5`

---

### 3. Real-Time Click Tracking

```
User app connects WebSocket
    ↓
[User App] src/routes/app/_authed/dashboard.tsx (inferred)
    ↓
Request to /click-socket → apps/user-application/worker/hono/app.ts:68
    ↓
Auth middleware validates session → line 41
    ↓
Proxy to data-service (service binding) → line 73
    ↓
[Data Service] apps/data-service/src/hono/app.ts:13
    ↓
Validate WebSocket upgrade → line 15
    ↓
Get Durable Object stub → line 22
    ↓
[Durable Object] apps/data-service/src/durable-objects/link-click-tracker.ts:69
    ↓
Create WebSocket pair → line 70
    ↓
Accept WebSocket connection → line 72
    ↓
Return 101 response → line 75
    ↓
─────────────────────────────
[Meanwhile, clicks are stored]
    ↓
Click event captured → route-ops.ts:68
    ↓
[Durable Object] Add to SQL table → link-click-tracker.ts:34
    ↓
Schedule 2-second alarm → line 46
    ↓
⏰ [Alarm fires]
    ↓
Query recent clicks from SQL → line 51
    ↓
Broadcast to all connected WebSocket clients → line 54
    ↓
Delete old clicks → line 59
```

**Key Files**:
- WebSocket proxy: `apps/user-application/worker/hono/app.ts:68`
- WS endpoint: `apps/data-service/src/hono/app.ts:13`
- Click tracker DO: `apps/data-service/src/durable-objects/link-click-tracker.ts:5`

---

## Comparison: Routing.md vs Backpine

The **Routing & Layout.md** document describes a **TanStack Start + Convex** application for GitHub repo analytics. The **backpine repository** demonstrates a **parallel architecture** using Cloudflare primitives.

### Architectural Mapping

| Feature | Routing.md (Convex) | Backpine (Cloudflare) | Notes |
|---------|---------------------|------------------------|-------|
| **Routing** | TanStack Start (SSR) | TanStack Router (CSR) + Vite | Backpine uses client-side routing |
| **Real-Time DB** | Convex | D1 + Durable Objects | Convex = managed; Cloudflare = DIY |
| **Mutations** | Convex mutations | TRPC mutations + Queues | Similar patterns, different frameworks |
| **Queries** | `useQuery` (Convex) | `trpc.*.useQuery()` | Both type-safe |
| **WebSocket** | Convex (automatic) | Manual in Durable Objects | Convex abstracts sockets |
| **AI Integration** | CodeRabbit + external LLM | Workers AI (Llama 3.3) | Cloudflare = native, no external API |
| **Web Scraping** | Firecrawl | Browser Rendering API | Both capture web content |
| **Usage Tracking** | Autumn | Could add Analytics Engine | Cloudflare has native analytics |
| **Error Monitoring** | Sentry | Could add Tail Workers | Cloudflare has log streaming |
| **Deployment** | Netlify/Cloudflare | Cloudflare Workers (native) | Backpine is edge-first |

---

### Real-Time Strategy Comparison

#### Routing.md (Convex)
```typescript
// Automatic real-time sync
const messages = useQuery(api.messages.getMessages);
// WebSocket handled by Convex backend
// No manual socket management needed
```

**Pros**: Simple, managed, automatic sync
**Cons**: Vendor lock-in, less control

---

#### Backpine (Cloudflare)
```typescript
// Manual WebSocket in Durable Object
export class LinkClickTracker extends DurableObject {
  async addClick(...) {
    this.sql.exec(`INSERT INTO geo_link_clicks ...`);
    await this.ctx.storage.setAlarm(moment().add(2, 'seconds').valueOf());
  }

  async alarm() {
    const clickData = getRecentClicks(this.sql, this.mostRecentOffsetTime);
    const sockets = this.ctx.getWebSockets();
    for (const socket of sockets) {
      socket.send(JSON.stringify(clickData.clicks));
    }
  }

  async fetch(_: Request) {
    const webSocketPair = new WebSocketPair();
    const [client, server] = Object.values(webSocketPair);
    this.ctx.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: client });
  }
}
```

**Pros**: Full control, SQL storage, edge performance
**Cons**: More code, manual alarm management

---

### Key Differences

| Aspect | Routing.md | Backpine |
|--------|------------|----------|
| **Data Layer** | Convex (managed real-time DB) | D1 + Durable Objects + KV (build-your-own) |
| **Framework** | TanStack Start (SSR) | TanStack Router (CSR) + Hono |
| **AI** | External LLM (Claude/GPT-4) via CodeRabbit | Native Workers AI (Llama 3.3) |
| **Real-Time** | Convex selective live updates | DO alarms + WebSocket broadcasting |
| **Complexity** | Low (managed services) | Medium (manual implementations) |
| **Control** | Limited (abstracted) | High (low-level primitives) |
| **Cost Model** | Convex pricing | Cloudflare Workers pricing |

---

## Technology Stack Summary

### Frontend
- **Framework**: React 19
- **Routing**: TanStack Router with file-based routes
- **State**: Zustand + TRPC React Query
- **Styling**: Tailwind CSS 4 + shadcn/ui components
- **Icons**: Lucide React, Tabler Icons
- **Maps**: react-simple-maps for geo visualization

### Backend
- **API Layer**: Hono (lightweight web framework)
- **Type Safety**: TRPC for end-to-end types
- **Database ORM**: Drizzle for D1 SQLite
- **Auth**: Better Auth with Google OAuth
- **Payments**: Stripe via Better Auth plugin
- **Validation**: Zod schemas throughout

### Cloudflare Platform
- **Runtime**: Cloudflare Workers (V8 isolates)
- **Database**: D1 (SQLite)
- **Cache**: KV (key-value store)
- **Storage**: R2 (object storage)
- **Real-Time**: Durable Objects (SQL + WebSockets)
- **Orchestration**: Workflows (multi-step processes)
- **Async Processing**: Queues
- **AI**: Workers AI (Llama 3.3)
- **Browser**: Browser Rendering API (Puppeteer)

### Development
- **Build Tool**: Vite 7
- **Bundler**: Cloudflare Vite Plugin
- **Language**: TypeScript 5
- **Package Manager**: pnpm (workspace)
- **Testing**: Vitest

---

## Key Patterns & Best Practices

### 1. Service-to-Service Bindings
**User app proxies WebSocket to data service** (`apps/user-application/worker/hono/app.ts:68`):
```typescript
App.get("/click-socket", authMiddleware, async (c) => {
  const userId = c.get("userId");
  const headers = new Headers(c.req.raw.headers);
  headers.set("account-id", userId);
  const proxiedRequest = new Request(c.req.raw, { headers });
  return c.env.BACKEND_SERVICE.fetch(proxiedRequest); // Service binding
});
```

### 2. KV Read-Through Cache
**Always check cache before database** (`apps/data-service/src/helpers/route-ops.ts:32`):
```typescript
export async function getRoutingDestinations(env: Env, id: string) {
  const linkInfo = await getLinkInfoFromKv(env, id); // Try KV
  if (linkInfo) return linkInfo;
  const linkInfoFromDb = await getLink(id); // Fallback to D1
  if (!linkInfoFromDb) return null;
  await saveLinkInfoToKv(env, id, linkInfoFromDb); // Populate cache
  return linkInfoFromDb;
}
```

### 3. Type-Safe Queues with Zod
**Validate all queue messages** (`apps/data-service/src/index.ts:22`):
```typescript
async queue(batch: MessageBatch<unknown>) {
  for (const message of batch.messages) {
    const parsedEvent = QueueMessageSchema.safeParse(message.body);
    if (parsedEvent.success) {
      const event = parsedEvent.data; // Typed!
      if (event.type === "LINK_CLICK") {
        await handleLinkClick(this.env, event);
      }
    }
  }
}
```

### 4. Workflow with Retries
**Declarative error handling** (`apps/data-service/src/workflows/destination-evalutation-workflow.ts:12`):
```typescript
const evaluationInfo = await step.do(
  'Collect rendered destination page data',
  { retries: { limit: 1, delay: 1000 } }, // Retry config
  async () => { ... }
);
```

### 5. SQL in Durable Objects
**Persistent, queryable state** (`apps/data-service/src/durable-objects/link-click-tracker.ts:23`):
```typescript
this.sql.exec(`
  CREATE TABLE IF NOT EXISTS geo_link_clicks (
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    country TEXT NOT NULL,
    time INTEGER NOT NULL
  )
`);
```

---

## Summary

The **web/backpine** repository is a masterclass in Cloudflare Workers development, showcasing:

✅ **All major Cloudflare services**: D1, KV, R2, Durable Objects, Workflows, Queues, Workers AI, Browser Rendering
✅ **Production patterns**: Hono + TRPC + TanStack Router + Drizzle ORM
✅ **Auth + Payments**: Better Auth with Stripe subscription plugin
✅ **Real-time architecture**: Durable Objects with SQL storage + WebSocket broadcasting
✅ **AI workflows**: Multi-step evaluation with retries and artifact storage
✅ **Smart routing**: Geo-based redirection with KV caching
✅ **Type safety**: End-to-end TypeScript with Zod validation

While the **Routing.md** document outlines a Convex-based real-time dashboard, **backpine** demonstrates how to achieve similar capabilities using Cloudflare's primitives, offering more control at the cost of added complexity.

**Choose Convex** for: Speed, simplicity, automatic real-time sync
**Choose Cloudflare** for: Fine-grained control, edge performance, cost optimization, avoiding vendor lock-in

Both approaches are valid production architectures for modern real-time applications.
