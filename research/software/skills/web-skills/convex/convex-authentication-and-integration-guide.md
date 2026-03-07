# Convex Authentication, Actions, and Integration Capabilities Research

## Table of Contents

1. [Authentication Patterns](#authentication-patterns)
2. [Actions vs Queries vs Mutations](#actions-vs-queries-vs-mutations)
3. [External API Integration Patterns](#external-api-integration-patterns)
4. [Scheduled Functions and Cron Jobs](#scheduled-functions-and-cron-jobs)
5. [HTTP Endpoints and Webhooks](#http-endpoints-and-webhooks)
6. [Vector Search and AI Integrations](#vector-search-and-ai-integrations)
7. [Component Architecture](#component-architecture)
8. [Environment Variables and Configuration](#environment-variables-and-configuration)
9. [Testing Patterns](#testing-patterns)
10. [Framework Integration (Next.js/React)](#framework-integration-nextjs-react)
11. [File Storage](#file-storage)
12. [Real-time Subscriptions](#real-time-subscriptions)

---

## 1. Authentication Patterns

### Convex Auth Overview

Convex Auth is a library for implementing authentication directly within your Convex backend, allowing you to authenticate users without needing an external authentication service or hosting server. It is currently in **beta**.

**Official Documentation**: https://docs.convex.dev/auth/convex-auth

### Technical Foundation

- Built on top of **Auth.js** (previously NextAuth.js)
- Supports multiple authentication providers simultaneously
- Leverages the Auth.js ecosystem for 80+ OAuth integrations out of the box

### Configuration Pattern

#### Backend Setup (`convex/auth.ts`)

The main configuration file configures available authentication methods:

```typescript
// convex/auth.ts
import { convexAuth } from "@convex-dev/auth/server";
import GitHub from "@auth/core/providers/github";
import Google from "@auth/core/providers/google";
import Resend from "@convex-dev/auth/providers/resend";
import { Password } from "@convex-dev/auth/providers/password";

export const { auth, signIn, signOut, store } = convexAuth({
  providers: [GitHub, Google, Resend, Password],
});
```

#### Schema Setup (`convex/schema.ts`)

Your schema must include tables used by the library:

```typescript
import { authTables } from "@convex-dev/auth/server";

export default defineSchema({
  ...authTables,
  // your other tables
});
```

The schema includes a `users` table with efficient indexes for lookups.

### Frontend Pattern

#### Provider Setup

Instead of using `ConvexProvider`, wrap your app in `ConvexAuthProvider`:

```typescript
import { ConvexAuthProvider } from "@convex-dev/auth/react";
import { ConvexReactClient } from "convex/react";

const convex = new ConvexReactClient(process.env.NEXT_PUBLIC_CONVEX_URL);

export default function App({ children }) {
  return (
    <ConvexAuthProvider client={convex}>
      {children}
    </ConvexAuthProvider>
  );
}
```

#### Conditional Rendering

Use `Authenticated` and `Unauthenticated` components:

```typescript
import { Authenticated, Unauthenticated } from "convex/react";

function MyApp() {
  return (
    <>
      <Authenticated>
        <Dashboard />
      </Authenticated>
      <Unauthenticated>
        <LoginPage />
      </Unauthenticated>
    </>
  );
}
```

### Authorization Pattern

The most common pattern is to check authentication and authorization at the beginning of each public function:

```typescript
import { query } from "./_generated/server";

export const getPrivateData = query({
  args: {},
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Not authenticated");
    }
    // Check authorization
    const user = await ctx.db
      .query("users")
      .withIndex("by_token", (q) =>
        q.eq("tokenIdentifier", identity.tokenIdentifier)
      )
      .unique();

    if (!user?.isAdmin) {
      throw new Error("Not authorized");
    }

    // Proceed with logic
    return await ctx.db.query("privateData").collect();
  },
});
```

### Supported Authentication Methods

1. **OAuth Providers**: 80+ integrations including GitHub, Google, Facebook, Twitter, etc.
2. **Magic Links**: Passwordless email authentication
3. **OTP (One-Time Password)**: Email-based codes
4. **Email and Password**: Traditional credential-based authentication
5. **Custom Auth**: Integration with custom OIDC providers

### Alternative: Better Auth Integration

Convex also integrates with **Better Auth** as an alternative authentication solution. Better Auth provides:
- Convex adapter for session storage
- OIDC provider capabilities
- Fine-grained control over authentication flows

**Documentation**: https://www.better-auth.com/docs/integrations/convex

### Use Cases

1. **Social Login Application**: Use Convex Auth with GitHub and Google providers for quick social authentication
2. **Enterprise SSO**: Configure custom OIDC provider to integrate with enterprise identity providers
3. **Multi-tenant SaaS**: Combine Convex Auth with custom authorization logic for role-based access control
4. **Passwordless App**: Use magic links or OTP for secure, passwordless authentication

---

## 2. Actions vs Queries vs Mutations

Understanding when to use each function type is fundamental to building scalable Convex backends.

**Official Documentation**: https://docs.convex.dev/functions/actions

### Queries

**Purpose**: Fetching data from the database

**Characteristics**:
- Read-only operations
- Automatically cached
- Reactive - clients are notified when results change
- Transactional - consistent view of data
- **Cannot** call external APIs
- **Cannot** perform non-deterministic operations

**When to Use**:
- Fetching user data
- Loading lists of items
- Reading any database state
- Implementing search across database tables

**Example**:

```typescript
import { query } from "./_generated/server";
import { v } from "convex/values";

export const getMessages = query({
  args: { channelId: v.id("channels") },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("messages")
      .withIndex("by_channel", (q) => q.eq("channelId", args.channelId))
      .order("desc")
      .take(100);
  },
});
```

### Mutations

**Purpose**: Writing data to the database

**Characteristics**:
- Can read and write to the database
- Run transactionally
- All reads get a consistent view
- All writes commit together or rollback together
- **Cannot** call external APIs directly
- **Cannot** perform non-deterministic operations (like fetching random data)

**When to Use**:
- Creating, updating, or deleting database records
- Atomic multi-step database operations
- Scheduling actions (mutations can schedule actions)
- Any operation that modifies application state

**Example**:

```typescript
import { mutation } from "./_generated/server";
import { v } from "convex/values";

export const sendMessage = mutation({
  args: {
    channelId: v.id("channels"),
    text: v.string(),
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    return await ctx.db.insert("messages", {
      channelId: args.channelId,
      text: args.text,
      userId: identity.subject,
      timestamp: Date.now(),
    });
  },
});
```

### Actions

**Purpose**: Calling third-party services and performing non-deterministic operations

**Characteristics**:
- Can call external APIs (fetch, LLM calls, payment processing)
- Can send emails
- Can interact with the database **indirectly** by calling queries and mutations
- Not transactional
- Can perform non-deterministic operations
- Can be run on a schedule

**When to Use**:
- Calling external APIs (Stripe, OpenAI, SendGrid)
- Processing payments
- Sending emails or SMS
- Generating AI content
- Webhook responses requiring external calls
- File processing with external services

**Example**:

```typescript
import { action } from "./_generated/server";
import { v } from "convex/values";
import { api } from "./_generated/api";

export const processPayment = action({
  args: {
    amount: v.number(),
    orderId: v.id("orders"),
  },
  handler: async (ctx, args) => {
    // Call external payment API
    const paymentResult = await fetch("https://api.stripe.com/v1/charges", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${process.env.STRIPE_SECRET_KEY}`,
      },
      body: JSON.stringify({
        amount: args.amount,
        currency: "usd",
      }),
    });

    const payment = await paymentResult.json();

    // Update database via mutation
    await ctx.runMutation(api.orders.markAsPaid, {
      orderId: args.orderId,
      paymentId: payment.id,
    });

    return payment;
  },
});
```

### Best Practices

#### 1. Minimize Calls from Actions to Queries/Mutations

Try to use as few calls from actions to queries and mutations as possible. Since queries and mutations are transactions, splitting logic into multiple calls introduces the risk of race conditions.

**Bad Example**:
```typescript
// Action calls mutation multiple times (race conditions possible)
export const badAction = action({
  handler: async (ctx, args) => {
    const data1 = await fetch("...");
    await ctx.runMutation(api.mutations.update1, { data: data1 });

    const data2 = await fetch("...");
    await ctx.runMutation(api.mutations.update2, { data: data2 });
  },
});
```

**Good Example**:
```typescript
// Action batches all updates in one mutation
export const goodAction = action({
  handler: async (ctx, args) => {
    const data1 = await fetch("...");
    const data2 = await fetch("...");

    await ctx.runMutation(api.mutations.updateAll, {
      data1,
      data2,
    });
  },
});
```

#### 2. Preferred Workflow: Mutation-First

The right sequence is mutation-first: persist information about the job you intend to do and then schedule it to be done in the background in one atomic step.

**Example**:
```typescript
export const createOrder = mutation({
  handler: async (ctx, args) => {
    // 1. Persist the order
    const orderId = await ctx.db.insert("orders", {
      ...args,
      status: "pending",
    });

    // 2. Schedule background processing
    await ctx.scheduler.runAfter(0, api.actions.processPayment, {
      orderId,
    });

    return orderId;
  },
});
```

This pattern ensures the job can be retried if it fails without needing the browser to remain online.

#### 3. Keep Most Logic in Queries/Mutations

Keep actions small and keep most work in queries and mutations. This is fundamental to building scalable Convex backends.

Actions should primarily:
- Fetch external data
- Call mutations with that data
- Return results

Heavy business logic should be in mutations for consistency and performance.

### Decision Matrix

| Need to... | Use |
|-----------|-----|
| Read database data | **Query** |
| Write to database | **Mutation** |
| Call external API | **Action** |
| Send email/SMS | **Action** |
| Process payment | **Action** |
| Call LLM | **Action** |
| Generate random data | **Action** |
| Schedule background work | **Mutation** (can schedule Actions) |
| Atomic multi-table update | **Mutation** |

---

## 3. External API Integration Patterns

Convex provides several patterns for integrating with external APIs and services.

**Official Documentation**: https://docs.convex.dev/tutorial/actions

### Pattern 1: Action Functions for Calling External APIs

Actions are the primary mechanism for calling external services from your Convex backend.

#### Basic Pattern

```typescript
import { action } from "./_generated/server";
import { v } from "convex/values";

export const fetchWeather = action({
  args: { city: v.string() },
  handler: async (ctx, args) => {
    const response = await fetch(
      `https://api.weather.com/v1/current?city=${args.city}`,
      {
        headers: {
          "Authorization": `Bearer ${process.env.WEATHER_API_KEY}`,
        },
      }
    );

    const weatherData = await response.json();
    return weatherData;
  },
});
```

#### Best Practices

1. **Minimize action work**: Keep actions as small as possible for highest throughput
2. **Error handling**: Implement robust error handling for external API failures
3. **Timeouts**: Set appropriate timeouts for external calls
4. **Retry logic**: Consider implementing retry logic for transient failures

#### Example with Error Handling

```typescript
export const fetchWithRetry = action({
  args: { url: v.string() },
  handler: async (ctx, args) => {
    let lastError;

    for (let i = 0; i < 3; i++) {
      try {
        const response = await fetch(args.url, {
          signal: AbortSignal.timeout(5000), // 5 second timeout
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
      } catch (error) {
        lastError = error;
        // Wait before retrying (exponential backoff)
        await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, i)));
      }
    }

    throw lastError;
  },
});
```

### Pattern 2: HTTP Actions for Receiving External Requests

HTTP actions allow external services to call into your Convex backend via HTTP endpoints.

**Official Documentation**: https://docs.convex.dev/functions/http-actions

#### Basic HTTP Action

```typescript
import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";

const http = httpRouter();

http.route({
  path: "/webhook/stripe",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const body = await request.json();

    // Verify webhook signature
    const signature = request.headers.get("stripe-signature");
    // ... verification logic

    // Process webhook
    await ctx.runMutation(api.payments.handleWebhook, {
      event: body,
    });

    return new Response(JSON.stringify({ received: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }),
});

export default http;
```

#### Endpoint URL

HTTP actions are exposed at: `https://<your-deployment-name>.convex.site`

For example: `https://happy-animal-123.convex.site/webhook/stripe`

#### Features

- Follow the Fetch API standard (Request/Response)
- Can manipulate request and response directly
- Interact with Convex data by running queries, mutations, and actions
- Support for routing with path parameters

#### Advanced Routing Example

```typescript
http.route({
  path: "/api/users/:userId",
  method: "GET",
  handler: httpAction(async (ctx, request) => {
    const userId = request.pathParams.userId;

    const user = await ctx.runQuery(api.users.get, {
      id: userId as Id<"users">,
    });

    return new Response(JSON.stringify(user), {
      headers: { "Content-Type": "application/json" },
    });
  }),
});
```

### Pattern 3: HTTP API for External Services

The Convex HTTP API allows external services to call your Convex functions programmatically.

**Official Documentation**: https://docs.convex.dev/http-api/

#### Calling Functions via HTTP

```bash
# POST to call any function
curl -X POST https://happy-animal-123.convex.cloud/api/function/myFunction \
  -H "Content-Type: application/json" \
  -d '{"arg1": "value1", "arg2": 123}'
```

#### Authentication

Optionally authenticate as a user via bearer token:

```bash
curl -X POST https://happy-animal-123.convex.cloud/api/function/myFunction \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Pattern 4: Advanced HTTP Routing with Hono

For complex HTTP APIs, Convex supports integration with **Hono**, a lightweight web framework.

**Blog Post**: https://stack.convex.dev/hono-with-convex

#### Example

```typescript
import { Hono } from "hono";
import { HonoWithConvex, HttpRouterWithHono } from "convex-helpers/server/hono";

const app: HonoWithConvex = new Hono();

app.get("/hello/:name", async (c) => {
  const name = c.req.param("name");
  return c.json({ message: `Hello ${name}!` });
});

app.post("/api/data", async (c) => {
  const body = await c.req.json();
  const result = await c.env.runMutation(api.data.create, body);
  return c.json(result);
});

export default new HttpRouterWithHono(app);
```

### Pattern 5: OpenAPI Client Generation

Generate OpenAPI specifications from your Convex deployment to create type-safe clients for unsupported languages.

**Official Documentation**: https://docs.convex.dev/client/open-api

### Pattern 6: Pre-built Component Integrations

Convex offers Components for seamless third-party service integrations:

- **Twilio**: SMS and voice
- **Resend**: Email sending
- **LaunchDarkly**: Feature flags
- **Cloudflare R2**: Object storage
- **And more**: https://www.convex.dev/components

#### Example: Resend Component

```typescript
import { Resend } from "@convex-dev/resend";

const resend = new Resend(components.resend);

export const sendWelcomeEmail = mutation({
  args: { email: v.string(), name: v.string() },
  handler: async (ctx, args) => {
    await resend.emails.send(ctx, {
      from: "welcome@myapp.com",
      to: args.email,
      subject: `Welcome ${args.name}!`,
      html: "<p>Thanks for signing up!</p>",
    });
  },
});
```

### Use Cases

1. **Payment Processing**: Integrate Stripe for payment processing
2. **Email Notifications**: Send emails via Resend or SendGrid
3. **SMS Alerts**: Send SMS via Twilio
4. **AI Processing**: Call OpenAI, Anthropic, or other AI services
5. **Webhook Receivers**: Receive webhooks from GitHub, Stripe, Clerk, etc.
6. **Third-party Data**: Fetch data from weather APIs, stock APIs, etc.

---

## 4. Scheduled Functions and Cron Jobs

Convex provides two main approaches for scheduling functions: one-time scheduled functions and recurring cron jobs.

**Official Documentation**: https://docs.convex.dev/scheduling/cron-jobs

### Scheduled Functions

Schedule functions to run at a later point in time (minutes to months in the future).

#### From a Mutation

```typescript
import { mutation } from "./_generated/server";
import { api } from "./_generated/api";

export const scheduleReminder = mutation({
  args: { message: v.string(), delayMs: v.number() },
  handler: async (ctx, args) => {
    await ctx.scheduler.runAfter(args.delayMs, api.actions.sendReminder, {
      message: args.message,
    });
  },
});
```

#### From an Action

```typescript
export const processData = action({
  handler: async (ctx, args) => {
    // Do some work
    const result = await fetch("...");

    // Schedule follow-up in 1 hour
    await ctx.scheduler.runAfter(
      3600000, // 1 hour in ms
      api.actions.followUp,
      { data: result }
    );
  },
});
```

### Cron Jobs

Schedule functions to run on a recurring basis.

#### Configuration File (`convex/crons.ts`)

```typescript
import { cronJobs } from "convex/server";
import { api } from "./_generated/api";

const crons = cronJobs();

// Every minute
crons.interval(
  "clear-presence-data",
  { seconds: 60 },
  api.presence.clear
);

// Every hour at :15
crons.hourly(
  "aggregate-stats",
  { minuteUTC: 15 },
  api.stats.aggregate
);

// Every day at 9 AM UTC
crons.daily(
  "send-daily-digest",
  { hourUTC: 9, minuteUTC: 0 },
  api.emails.sendDailyDigest
);

// Weekly on Mondays at 8 AM
crons.weekly(
  "weekly-cleanup",
  { hourUTC: 8, minuteUTC: 0, dayOfWeek: "monday" },
  api.cleanup.weekly
);

// Monthly on the 1st at midnight
crons.monthly(
  "monthly-report",
  { hourUTC: 0, minuteUTC: 0, day: 1 },
  api.reports.monthly
);

// Traditional cron syntax (every 5 minutes)
crons.cron(
  "sync-external-data",
  "*/5 * * * *",
  api.sync.external
);

export default crons;
```

#### Cron Syntax Methods

**1. interval()** - Runs every X seconds/minutes/hours
```typescript
crons.interval("job-name", { seconds: 30 }, api.function);
crons.interval("job-name", { minutes: 5 }, api.function);
crons.interval("job-name", { hours: 2 }, api.function);
```

**2. cron()** - Traditional 5-field cron syntax (UTC timezone)
```typescript
// Format: minute hour day month dayOfWeek
crons.cron("job-name", "*/15 * * * *", api.function); // Every 15 mins
crons.cron("job-name", "0 0 * * 0", api.function);    // Sundays at midnight
```

**3. Named methods** - Explicit configuration
```typescript
crons.hourly("job-name", { minuteUTC: 30 }, api.function);
crons.daily("job-name", { hourUTC: 14, minuteUTC: 0 }, api.function);
crons.weekly("job-name", {
  dayOfWeek: "friday",
  hourUTC: 17,
  minuteUTC: 0
}, api.function);
crons.monthly("job-name", {
  day: 15,
  hourUTC: 0,
  minuteUTC: 0
}, api.function);
```

### Runtime Cron Jobs (Dynamic Registration)

Built-in crons must be defined statically in `crons.ts`. For dynamic cron registration at runtime, use the **Crons Component**.

**Component**: https://github.com/get-convex/crons

#### Installation

```bash
npm install @convex-dev/crons
```

#### Setup

```typescript
// convex.config.ts
import { defineApp } from "convex/server";
import crons from "@convex-dev/crons/convex.config";

const app = defineApp();
app.use(crons);
export default app;
```

#### Usage

```typescript
import { components } from "./_generated/api";
import { Crons } from "@convex-dev/crons";

const crons = new Crons(components.crons);

export const registerDynamicCron = mutation({
  handler: async (ctx, args) => {
    await crons.register(ctx, {
      identifier: `user-${args.userId}-reminder`,
      schedule: { type: "interval", ms: 86400000 }, // daily
      functionReference: api.reminders.send,
      args: { userId: args.userId },
    });
  },
});
```

### Use Cases

1. **Data Cleanup**: Delete old records daily
2. **Email Digests**: Send weekly summary emails
3. **Data Synchronization**: Sync with external APIs hourly
4. **Backup Operations**: Create backups nightly
5. **Reminder Systems**: Send scheduled reminders
6. **Analytics Aggregation**: Compute metrics periodically
7. **Subscription Renewals**: Check and process renewals daily
8. **Content Publishing**: Publish scheduled content

### Best Practices

1. **Idempotency**: Make cron functions idempotent (safe to run multiple times)
2. **Error Handling**: Implement robust error handling
3. **Monitoring**: Track cron job execution in your database
4. **Batching**: Process large datasets in batches to avoid timeouts
5. **Timezone Awareness**: Remember crons run in UTC

---

## 5. HTTP Endpoints and Webhooks

Convex HTTP actions are ideal for building webhooks and custom HTTP APIs.

**Official Documentation**: https://docs.convex.dev/functions/http-actions

### Basic HTTP Action

```typescript
import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";

const http = httpRouter();

http.route({
  path: "/hello",
  method: "GET",
  handler: httpAction(async (ctx, request) => {
    return new Response("Hello, World!", {
      status: 200,
      headers: { "Content-Type": "text/plain" },
    });
  }),
});

export default http;
```

### Webhook Examples

#### 1. Clerk Authentication Webhook

Sync user data when users sign up, update, or delete accounts.

**Blog Post**: https://clerk.com/blog/webhooks-data-sync-convex

```typescript
import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";
import { Webhook } from "svix";

const http = httpRouter();

http.route({
  path: "/clerk",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const svixId = request.headers.get("svix-id");
    const svixTimestamp = request.headers.get("svix-timestamp");
    const svixSignature = request.headers.get("svix-signature");

    if (!svixId || !svixTimestamp || !svixSignature) {
      return new Response("Error: Missing svix headers", { status: 400 });
    }

    const body = await request.text();
    const webhook = new Webhook(process.env.CLERK_WEBHOOK_SECRET!);

    let event;
    try {
      event = webhook.verify(body, {
        "svix-id": svixId,
        "svix-timestamp": svixTimestamp,
        "svix-signature": svixSignature,
      });
    } catch (err) {
      return new Response("Error: Invalid signature", { status: 400 });
    }

    // Handle different event types
    switch (event.type) {
      case "user.created":
        await ctx.runMutation(api.users.create, {
          clerkId: event.data.id,
          email: event.data.email_addresses[0].email_address,
          name: `${event.data.first_name} ${event.data.last_name}`,
        });
        break;
      case "user.updated":
        await ctx.runMutation(api.users.update, {
          clerkId: event.data.id,
          email: event.data.email_addresses[0].email_address,
        });
        break;
      case "user.deleted":
        await ctx.runMutation(api.users.delete, {
          clerkId: event.data.id,
        });
        break;
    }

    return new Response(JSON.stringify({ success: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }),
});

export default http;
```

#### 2. Stripe Webhook

Handle Stripe payment events.

```typescript
http.route({
  path: "/stripe",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const signature = request.headers.get("stripe-signature");
    const body = await request.text();

    let event;
    try {
      // Verify webhook signature
      const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);
      event = stripe.webhooks.constructEvent(
        body,
        signature,
        process.env.STRIPE_WEBHOOK_SECRET
      );
    } catch (err) {
      return new Response(`Webhook Error: ${err.message}`, { status: 400 });
    }

    // Handle the event
    switch (event.type) {
      case "payment_intent.succeeded":
        await ctx.runMutation(api.payments.handleSuccess, {
          paymentIntentId: event.data.object.id,
        });
        break;
      case "payment_intent.payment_failed":
        await ctx.runMutation(api.payments.handleFailure, {
          paymentIntentId: event.data.object.id,
        });
        break;
    }

    return new Response(JSON.stringify({ received: true }), { status: 200 });
  }),
});
```

#### 3. Twilio SMS Webhook

Handle incoming SMS messages.

**Blog Post**: https://stack.convex.dev/webhooks-with-convex

```typescript
http.route({
  path: "/twilio/sms",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const body = await request.text();
    const params = new URLSearchParams(body);

    const from = params.get("From");
    const messageBody = params.get("Body");

    // Store the message
    await ctx.runMutation(api.messages.storeIncoming, {
      from,
      body: messageBody,
      timestamp: Date.now(),
    });

    // Respond with TwiML
    return new Response(
      `<?xml version="1.0" encoding="UTF-8"?>
       <Response>
         <Message>Thanks for your message!</Message>
       </Response>`,
      {
        status: 200,
        headers: { "Content-Type": "text/xml" },
      }
    );
  }),
});
```

#### 4. Discord Bot Webhook

**Blog Post**: https://stack.convex.dev/webhooks-with-convex

```typescript
http.route({
  path: "/discord",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const body = await request.json();

    // Discord sends a PING on first setup
    if (body.type === 1) {
      return new Response(JSON.stringify({ type: 1 }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    // Handle slash command
    if (body.type === 2) {
      const command = body.data.name;
      const userId = body.member.user.id;

      const response = await ctx.runAction(api.discord.handleCommand, {
        command,
        userId,
      });

      return new Response(JSON.stringify({
        type: 4,
        data: { content: response },
      }), {
        headers: { "Content-Type": "application/json" },
      });
    }

    return new Response("OK", { status: 200 });
  }),
});
```

### Advanced Routing

#### Path Parameters

```typescript
http.route({
  path: "/api/users/:userId/posts/:postId",
  method: "GET",
  handler: httpAction(async (ctx, request) => {
    const { userId, postId } = request.pathParams;

    const post = await ctx.runQuery(api.posts.get, {
      userId: userId as Id<"users">,
      postId: postId as Id<"posts">,
    });

    return new Response(JSON.stringify(post), {
      headers: { "Content-Type": "application/json" },
    });
  }),
});
```

#### Query Parameters

```typescript
http.route({
  path: "/api/search",
  method: "GET",
  handler: httpAction(async (ctx, request) => {
    const url = new URL(request.url);
    const query = url.searchParams.get("q");
    const limit = parseInt(url.searchParams.get("limit") || "10");

    const results = await ctx.runQuery(api.search.run, {
      query,
      limit,
    });

    return new Response(JSON.stringify(results), {
      headers: { "Content-Type": "application/json" },
    });
  }),
});
```

#### CORS Headers

```typescript
http.route({
  path: "/api/public",
  method: "GET",
  handler: httpAction(async (ctx, request) => {
    const data = await ctx.runQuery(api.data.getPublic, {});

    return new Response(JSON.stringify(data), {
      status: 200,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type",
      },
    });
  }),
});

// Handle preflight requests
http.route({
  path: "/api/public",
  method: "OPTIONS",
  handler: httpAction(async (ctx, request) => {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type",
      },
    });
  }),
});
```

### Use Cases

1. **Third-party Webhooks**: Receive events from Stripe, Clerk, GitHub, etc.
2. **SMS/Voice**: Handle Twilio webhooks for SMS and voice interactions
3. **Public APIs**: Build RESTful APIs for mobile apps or third-party integrations
4. **Bot Integrations**: Discord, Slack, Telegram bot webhooks
5. **Payment Processing**: Handle payment gateway callbacks
6. **Form Submissions**: Process form submissions from static sites
7. **IoT Devices**: Receive data from IoT devices

---

## 6. Vector Search and AI Integrations

Convex provides built-in vector search capabilities for AI applications.

**Official Documentation**: https://docs.convex.dev/search/vector-search

### Vector Search Overview

Vector search enables searching for documents based on semantic meaning using vector embeddings to calculate similarity.

**Key Constraint**: Vector searches can only be performed in **Convex actions**.

### Schema Setup

Define a vector index in your schema:

```typescript
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  documents: defineTable({
    text: v.string(),
    embedding: v.array(v.float64()),
    metadata: v.object({
      source: v.string(),
      author: v.string(),
    }),
  }).vectorIndex("by_embedding", {
    vectorField: "embedding",
    dimensions: 1536, // OpenAI ada-002 embedding size
    filterFields: ["metadata.source"], // Optional filters
  }),
});
```

### Storing Embeddings

#### Generate and Store

```typescript
import { action } from "./_generated/server";
import { api } from "./_generated/api";
import { v } from "convex/values";

export const addDocument = action({
  args: {
    text: v.string(),
    source: v.string(),
    author: v.string(),
  },
  handler: async (ctx, args) => {
    // Generate embedding using OpenAI
    const response = await fetch("https://api.openai.com/v1/embeddings", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        input: args.text,
        model: "text-embedding-ada-002",
      }),
    });

    const result = await response.json();
    const embedding = result.data[0].embedding;

    // Store in database with embedding
    await ctx.runMutation(api.documents.insert, {
      text: args.text,
      embedding,
      metadata: {
        source: args.source,
        author: args.author,
      },
    });
  },
});
```

### Searching with Vectors

```typescript
import { action } from "./_generated/server";
import { v } from "convex/values";

export const semanticSearch = action({
  args: {
    query: v.string(),
    limit: v.optional(v.number()),
  },
  handler: async (ctx, args) => {
    // Generate embedding for query
    const response = await fetch("https://api.openai.com/v1/embeddings", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        input: args.query,
        model: "text-embedding-ada-002",
      }),
    });

    const result = await response.json();
    const queryEmbedding = result.data[0].embedding;

    // Perform vector search
    const results = await ctx.vectorSearch("documents", "by_embedding", {
      vector: queryEmbedding,
      limit: args.limit ?? 10,
      filter: (q) => q.eq("metadata.source", "documentation"), // Optional filter
    });

    // Fetch full documents
    const documents = await Promise.all(
      results.map(async (result) => {
        const doc = await ctx.runQuery(api.documents.get, {
          id: result._id,
        });
        return {
          ...doc,
          score: result._score,
        };
      })
    );

    return documents;
  },
});
```

### AI Chat with RAG (Retrieval Augmented Generation)

**Blog Post**: https://stack.convex.dev/ai-chat-with-convex-vector-search

```typescript
import { action } from "./_generated/server";
import { v } from "convex/values";

export const chatWithContext = action({
  args: {
    message: v.string(),
    conversationId: v.id("conversations"),
  },
  handler: async (ctx, args) => {
    // 1. Search for relevant context
    const relevantDocs = await ctx.runAction(api.search.semanticSearch, {
      query: args.message,
      limit: 5,
    });

    // 2. Build context from search results
    const context = relevantDocs
      .map(doc => doc.text)
      .join("\n\n");

    // 3. Call LLM with context
    const response = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "gpt-4",
        messages: [
          {
            role: "system",
            content: `You are a helpful assistant. Use the following context to answer questions:\n\n${context}`,
          },
          {
            role: "user",
            content: args.message,
          },
        ],
      }),
    });

    const result = await response.json();
    const reply = result.choices[0].message.content;

    // 4. Store conversation
    await ctx.runMutation(api.conversations.addMessage, {
      conversationId: args.conversationId,
      userMessage: args.message,
      assistantReply: reply,
      sources: relevantDocs.map(d => d._id),
    });

    return { reply, sources: relevantDocs };
  },
});
```

### LangChain Integration

Convex has official LangChain integration for vector storage.

**Documentation**: https://js.langchain.com/docs/integrations/vectorstores/convex/

```typescript
import { ConvexVectorStore } from "@langchain/community/vectorstores/convex";
import { OpenAIEmbeddings } from "@langchain/openai";

const vectorStore = new ConvexVectorStore(
  new OpenAIEmbeddings(),
  {
    convexUrl: process.env.CONVEX_URL,
    convexAPIKey: process.env.CONVEX_API_KEY,
  }
);

// Add documents
await vectorStore.addDocuments([
  { pageContent: "Text 1", metadata: { source: "doc1" } },
  { pageContent: "Text 2", metadata: { source: "doc2" } },
]);

// Search
const results = await vectorStore.similaritySearch("query", 5);
```

### AI Agents Framework

Convex has an official agents framework for building LLM-powered applications.

**Official Documentation**: https://docs.convex.dev/agents

#### Features

- **Agents**: Organize LLM prompting with models, prompts, and tools
- **Threads**: Persist messages across users and agents
- **Workflows**: Build multi-step operations durably
- **RAG Component**: Built-in retrieval augmented generation
- **Streaming**: Stream both text and objects

#### Example Agent

```typescript
import { Agent } from "@convex-dev/agent";

const myAgent = new Agent({
  model: {
    provider: "openai",
    model: "gpt-4",
  },
  systemPrompt: "You are a helpful assistant.",
  tools: {
    searchDocuments: {
      description: "Search the documentation",
      parameters: z.object({
        query: z.string(),
      }),
      handler: async ({ query }) => {
        const results = await ctx.runAction(api.search.semanticSearch, {
          query,
        });
        return results;
      },
    },
  },
});

export const chat = action({
  args: { threadId: v.id("threads"), message: v.string() },
  handler: async (ctx, args) => {
    const response = await myAgent.run(ctx, {
      threadId: args.threadId,
      message: args.message,
    });

    return response;
  },
});
```

### Use Cases

1. **Semantic Search**: Search documentation or knowledge bases by meaning
2. **RAG Chatbots**: Build AI assistants with access to your data
3. **Recommendation Systems**: Recommend similar products/content
4. **Document Classification**: Classify documents by semantic similarity
5. **Duplicate Detection**: Find duplicate or similar content
6. **Question Answering**: Answer questions from a knowledge base
7. **Content Discovery**: Help users discover relevant content

### Supported Embedding Models

- **OpenAI**: text-embedding-ada-002, text-embedding-3-small, text-embedding-3-large
- **Cohere**: embed-english-v3.0, embed-multilingual-v3.0
- **Hugging Face**: Various open-source models
- **Custom**: Any embedding model that produces float arrays

---

## 7. Component Architecture

Convex Components are independent, modular TypeScript building blocks for your backend.

**Official Documentation**: https://docs.convex.dev/components

### Overview

Components are similar to:
- **npm libraries**: Include functions, type safety, called from your code
- **Microservices**: Independent, self-contained backends
- **Frontend components**: Modular, reusable, composable

**Blog Post**: https://stack.convex.dev/backend-components

### Key Characteristics

#### 1. Isolation and Security

- Components can't read your app's tables unless you explicitly pass them
- Can't read file storage, environment variables, or call functions without permission
- Run in isolated environments
- Can't read/write global variables or patch system behavior

#### 2. Independent Data Storage

- Components can store data in their own database tables
- Have their own schema validation
- Independent file storage
- Isolated from main app's data

#### 3. Transactional Consistency

Data changes commit transactionally across calls to components, without distributed commit protocols or data inconsistencies.

### Using Components

#### Installation

```bash
npm install @convex-dev/aggregate
```

#### Configuration

```typescript
// convex.config.ts
import { defineApp } from "convex/server";
import aggregate from "@convex-dev/aggregate/convex.config";

const app = defineApp();
app.use(aggregate);

export default app;
```

#### Usage Example

```typescript
import { components } from "./_generated/api";
import { Aggregate } from "@convex-dev/aggregate";

const aggregate = new Aggregate(components.aggregate);

export const incrementCounter = mutation({
  handler: async (ctx, args) => {
    await aggregate.increment(ctx, {
      name: "page_views",
      value: 1,
    });
  },
});

export const getCount = query({
  handler: async (ctx) => {
    return await aggregate.get(ctx, { name: "page_views" });
  },
});
```

### Available Components

#### 1. Resend (Email)

```typescript
import { Resend } from "@convex-dev/resend";

const resend = new Resend(components.resend);

export const sendEmail = mutation({
  handler: async (ctx, args) => {
    await resend.emails.send(ctx, {
      from: "app@example.com",
      to: args.email,
      subject: "Welcome!",
      html: "<p>Thanks for signing up!</p>",
    });
  },
});
```

#### 2. Rate Limiting

```typescript
import { RateLimiter } from "@convex-dev/ratelimiter";

const rateLimiter = new RateLimiter(components.ratelimiter);

export const apiCall = mutation({
  handler: async (ctx, args) => {
    const { ok } = await rateLimiter.limit(ctx, {
      name: "api_calls",
      key: args.userId,
      limit: 100,
      period: 3600000, // 1 hour
    });

    if (!ok) {
      throw new Error("Rate limit exceeded");
    }

    // Proceed with API call
  },
});
```

#### 3. Cloudflare R2 (Object Storage)

```typescript
import { R2 } from "@convex-dev/r2";

const r2 = new R2(components.r2);

export const uploadFile = action({
  handler: async (ctx, args) => {
    await r2.put(ctx, {
      key: args.filename,
      value: args.data,
    });
  },
});
```

#### 4. Crons (Dynamic Scheduling)

```typescript
import { Crons } from "@convex-dev/crons";

const crons = new Crons(components.crons);

export const scheduleReminder = mutation({
  handler: async (ctx, args) => {
    await crons.register(ctx, {
      identifier: `reminder-${args.userId}`,
      schedule: { type: "interval", ms: 86400000 },
      functionReference: api.reminders.send,
      args: { userId: args.userId },
    });
  },
});
```

#### 5. Text Streaming (AI Chat)

**Blog Post**: https://stack.convex.dev/build-streaming-chat-app-with-persistent-text-streaming-component

```typescript
import { TextStreaming } from "@convex-dev/text-streaming";

const streaming = new TextStreaming(components.streaming);

export const streamResponse = action({
  handler: async (ctx, args) => {
    const streamId = await streaming.create(ctx);

    // Stream tokens from LLM
    const response = await fetch("...", { stream: true });
    for await (const chunk of response.body) {
      await streaming.append(ctx, { streamId, text: chunk });
    }

    await streaming.complete(ctx, { streamId });
    return streamId;
  },
});
```

### Building Custom Components

#### 1. Create Component Package

```typescript
// my-component/convex.config.ts
import { defineComponent } from "convex/server";

const component = defineComponent("myComponent");
export default component;
```

#### 2. Define Schema

```typescript
// my-component/schema.ts
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  items: defineTable({
    value: v.string(),
  }),
});
```

#### 3. Export Functions

```typescript
// my-component/index.ts
import { query, mutation } from "./_generated/server";

export const add = mutation({
  args: { value: v.string() },
  handler: async (ctx, args) => {
    await ctx.db.insert("items", { value: args.value });
  },
});

export const list = query({
  handler: async (ctx) => {
    return await ctx.db.query("items").collect();
  },
});
```

#### 4. Use in Main App

```typescript
// convex.config.ts
import { defineApp } from "convex/server";
import myComponent from "./my-component/convex.config";

const app = defineApp();
app.use(myComponent, { name: "myComponent" });
export default app;
```

```typescript
// Usage
import { components } from "./_generated/api";

export const useComponent = mutation({
  handler: async (ctx) => {
    await components.myComponent.add(ctx, { value: "test" });
    const items = await components.myComponent.list(ctx);
    return items;
  },
});
```

### Use Cases

1. **Email**: Send transactional emails with Resend
2. **SMS**: Send SMS with Twilio component
3. **File Storage**: Store files in Cloudflare R2
4. **Rate Limiting**: Protect APIs with rate limits
5. **Feature Flags**: Use LaunchDarkly component
6. **Payments**: Process payments with Stripe component
7. **Analytics**: Track events with analytics component
8. **Search**: Full-text search component
9. **AI Agents**: Reusable AI agent implementations

### Benefits

1. **Modularity**: Encapsulate functionality
2. **Reusability**: Share components across projects
3. **Security**: Isolated data and permissions
4. **Type Safety**: Full TypeScript support
5. **Consistency**: Transactional guarantees
6. **Simplicity**: No microservice complexity

---

## 8. Environment Variables and Configuration

Convex provides secure environment variable management for configuration and secrets.

**Official Documentation**: https://docs.convex.dev/production/environment-variables

### CLI Management

#### List Variables

```bash
npx convex env list
npx convex env list --prod
```

#### Set Variables

```bash
npx convex env set API_KEY secret-api-key
npx convex env set OPENAI_API_KEY sk-xxx --prod
```

#### Remove Variables

```bash
npx convex env remove API_KEY
```

### Dashboard Management

Environment variables can also be managed via the Convex dashboard:

1. Navigate to your deployment
2. Go to Settings → Environment Variables
3. Add, update, or remove variables

### Accessing Variables

#### In Functions

```typescript
import { action } from "./_generated/server";

export const callExternalAPI = action({
  handler: async (ctx, args) => {
    const apiKey = process.env.API_KEY;

    const response = await fetch("https://api.example.com/data", {
      headers: {
        "Authorization": `Bearer ${apiKey}`,
      },
    });

    return await response.json();
  },
});
```

### Per-Deployment Configuration

Environment variables are set **per-deployment**, allowing different values for dev and prod:

```bash
# Development
npx convex env set STRIPE_KEY sk_test_xxx

# Production
npx convex env set STRIPE_KEY sk_live_xxx --prod
```

### Built-in Variables

Convex provides built-in environment variables:

- **CONVEX_CLOUD_URL**: Your deployment URL
- **CONVEX_SITE_URL**: Your deployment site URL (for HTTP actions)

```typescript
export const getConfig = query({
  handler: async (ctx) => {
    return {
      deploymentUrl: process.env.CONVEX_CLOUD_URL,
      siteUrl: process.env.CONVEX_SITE_URL,
    };
  },
});
```

### Best Practices

#### 1. Never Commit Secrets

Never commit API keys or secrets to version control:

```bash
# .gitignore
.env
.env.local
convex.json
```

#### 2. Use Strong Secrets

Generate strong, random secrets:

```bash
# Generate a random secret
openssl rand -hex 32
```

#### 3. Separate Dev and Prod

Always use different values for development and production:

```bash
# Development
npx convex env set DATABASE_URL postgres://localhost:5432/dev

# Production
npx convex env set DATABASE_URL postgres://prod-server:5432/prod --prod
```

#### 4. Document Required Variables

Document required environment variables in your README:

```markdown
## Required Environment Variables

- `OPENAI_API_KEY`: OpenAI API key for embeddings
- `STRIPE_SECRET_KEY`: Stripe secret key for payments
- `RESEND_API_KEY`: Resend API key for emails
```

#### 5. Validate on Startup

Validate required variables are set:

```typescript
// convex/init.ts
import { internalMutation } from "./_generated/server";

export const validateEnv = internalMutation({
  handler: async (ctx) => {
    const required = [
      "OPENAI_API_KEY",
      "STRIPE_SECRET_KEY",
      "RESEND_API_KEY",
    ];

    for (const key of required) {
      if (!process.env[key]) {
        throw new Error(`Missing required environment variable: ${key}`);
      }
    }
  },
});
```

### Use Cases

1. **API Keys**: Store third-party API keys (OpenAI, Stripe, etc.)
2. **Database URLs**: Connection strings for external databases
3. **Webhook Secrets**: Secrets for verifying webhooks
4. **Feature Flags**: Toggle features via environment
5. **Configuration**: App-specific configuration values
6. **OAuth Credentials**: Client IDs and secrets for OAuth

### Security Notes

1. Environment variables are **encrypted at rest**
2. Only visible to your Convex functions (not client-side)
3. Can only be viewed/modified by team members with appropriate permissions
4. Never logged or exposed in error messages
5. Isolated per-deployment

---

## 9. Testing Patterns

Convex provides multiple approaches for testing your backend logic.

**Official Documentation**: https://docs.convex.dev/testing

**Blog Post**: https://stack.convex.dev/testing-patterns

### Testing Approaches

#### 1. convex-test (Recommended for Unit Tests)

The `convex-test` library provides a mock implementation of the Convex backend in JavaScript for fast automated testing.

**Official Documentation**: https://docs.convex.dev/testing/convex-test

##### Installation

```bash
npm install --save-dev convex-test vitest
```

##### Basic Example

```typescript
// convex/messages.test.ts
import { convexTest } from "convex-test";
import { describe, it, expect } from "vitest";
import schema from "./schema";
import { api } from "./_generated/api";

describe("messages", () => {
  it("creates a message", async () => {
    const t = convexTest(schema);

    const messageId = await t.mutation(api.messages.send, {
      text: "Hello, world!",
      channelId: await t.run(async (ctx) => {
        return await ctx.db.insert("channels", { name: "general" });
      }),
    });

    const message = await t.query(api.messages.get, { id: messageId });
    expect(message?.text).toBe("Hello, world!");
  });

  it("enforces authentication", async () => {
    const t = convexTest(schema);

    await expect(
      t.mutation(api.messages.send, {
        text: "Hello",
        channelId: "123",
      })
    ).rejects.toThrow("Not authenticated");
  });
});
```

##### With Authentication

```typescript
it("sends message as authenticated user", async () => {
  const t = convexTest(schema);

  const asUser = t.withIdentity({
    subject: "user123",
    email: "test@example.com",
  });

  const messageId = await asUser.mutation(api.messages.send, {
    text: "Authenticated message",
    channelId: "channel123",
  });

  expect(messageId).toBeDefined();
});
```

##### Testing Actions

```typescript
it("calls external API in action", async () => {
  const t = convexTest(schema);

  // Mock fetch
  global.fetch = vi.fn().mockResolvedValue({
    json: async () => ({ weather: "sunny" }),
  });

  const result = await t.action(api.weather.fetch, {
    city: "San Francisco",
  });

  expect(result.weather).toBe("sunny");
  expect(fetch).toHaveBeenCalledWith(
    expect.stringContaining("San Francisco"),
    expect.any(Object)
  );
});
```

##### Testing with Files

```typescript
it("handles file upload", async () => {
  const t = convexTest(schema);

  const blob = new Blob(["test content"], { type: "text/plain" });
  const storageId = await t.run(async (ctx) => {
    return await ctx.storage.store(blob);
  });

  const url = await t.query(api.files.getUrl, { storageId });
  expect(url).toBeDefined();
});
```

#### 2. Local Backend Testing

Test with a real Convex backend running locally using the open-source backend.

**Blog Post**: https://stack.convex.dev/testing-with-local-oss-backend

```bash
# Start local backend
npx convex dev --once --local-backend

# Run tests against local backend
npm test
```

**Note**: Generally recommend using `convex-test` over local backend for unit tests due to speed.

#### 3. Preview/Staging Environments

Deploy to a staging project for integration testing:

```bash
# Deploy to preview
npx convex deploy --preview

# Run integration tests
npm run test:integration
```

### Testing Patterns

#### Test Core Business Logic

Start by testing the logic that is core to your value proposition:

```typescript
describe("subscription billing", () => {
  it("calculates correct billing amount", async () => {
    const t = convexTest(schema);

    const userId = await t.run(async (ctx) => {
      return await ctx.db.insert("users", {
        plan: "pro",
        seats: 5,
      });
    });

    const amount = await t.query(api.billing.calculateAmount, { userId });
    expect(amount).toBe(5 * 29.99); // $29.99 per seat
  });
});
```

#### Test Security and Authorization

Test authorization logic to prevent security bugs:

```typescript
describe("document access", () => {
  it("prevents unauthorized access", async () => {
    const t = convexTest(schema);

    const documentId = await t.run(async (ctx) => {
      return await ctx.db.insert("documents", {
        ownerId: "user1",
        content: "Secret",
      });
    });

    const asOtherUser = t.withIdentity({ subject: "user2" });

    await expect(
      asOtherUser.query(api.documents.get, { id: documentId })
    ).rejects.toThrow("Access denied");
  });
});
```

#### Test Data Consistency

Ensure mutations maintain data consistency:

```typescript
describe("inventory management", () => {
  it("prevents overselling", async () => {
    const t = convexTest(schema);

    const productId = await t.run(async (ctx) => {
      return await ctx.db.insert("products", {
        name: "Widget",
        stock: 1,
      });
    });

    // First order succeeds
    await t.mutation(api.orders.create, {
      productId,
      quantity: 1,
    });

    // Second order fails (out of stock)
    await expect(
      t.mutation(api.orders.create, {
        productId,
        quantity: 1,
      })
    ).rejects.toThrow("Out of stock");
  });
});
```

#### Test Edge Cases

Test boundary conditions and edge cases:

```typescript
describe("pagination", () => {
  it("handles empty results", async () => {
    const t = convexTest(schema);

    const results = await t.query(api.search.paginate, {
      cursor: null,
      limit: 10,
    });

    expect(results.page).toHaveLength(0);
    expect(results.isDone).toBe(true);
  });

  it("handles cursor pagination correctly", async () => {
    const t = convexTest(schema);

    // Insert 25 items
    await t.run(async (ctx) => {
      for (let i = 0; i < 25; i++) {
        await ctx.db.insert("items", { value: i });
      }
    });

    // First page
    const page1 = await t.query(api.items.paginate, {
      cursor: null,
      limit: 10,
    });
    expect(page1.page).toHaveLength(10);
    expect(page1.isDone).toBe(false);

    // Second page
    const page2 = await t.query(api.items.paginate, {
      cursor: page1.continueCursor,
      limit: 10,
    });
    expect(page2.page).toHaveLength(10);

    // Third page
    const page3 = await t.query(api.items.paginate, {
      cursor: page2.continueCursor,
      limit: 10,
    });
    expect(page3.page).toHaveLength(5);
    expect(page3.isDone).toBe(true);
  });
});
```

#### Smoke Tests

Basic tests to ensure no glaring issues:

```typescript
describe("smoke tests", () => {
  it("app loads without errors", async () => {
    const t = convexTest(schema);

    // Just ensure queries don't throw
    await expect(
      t.query(api.homepage.getData, {})
    ).resolves.toBeDefined();
  });
});
```

### Best Practices

#### 1. Test Business Invariants

Encode guarantees you want the code to make:

```typescript
it("maintains account balance consistency", async () => {
  const t = convexTest(schema);

  const accountId = await t.run(async (ctx) => {
    return await ctx.db.insert("accounts", { balance: 1000 });
  });

  // Transfer should fail if insufficient funds
  await expect(
    t.mutation(api.accounts.transfer, {
      fromAccountId: accountId,
      amount: 1500,
    })
  ).rejects.toThrow("Insufficient funds");

  // Balance should be unchanged
  const account = await t.run(async (ctx) => {
    return await ctx.db.get(accountId);
  });
  expect(account?.balance).toBe(1000);
});
```

#### 2. Test with Realistic Data

Use realistic test data:

```typescript
const testUser = {
  email: "test@example.com",
  name: "Test User",
  preferences: {
    theme: "dark",
    notifications: true,
  },
};
```

#### 3. Use Factories

Create helper functions for common test data:

```typescript
async function createTestUser(ctx: any, overrides = {}) {
  return await ctx.db.insert("users", {
    email: "test@example.com",
    name: "Test User",
    createdAt: Date.now(),
    ...overrides,
  });
}

it("creates user with custom email", async () => {
  const t = convexTest(schema);

  const userId = await t.run(async (ctx) => {
    return await createTestUser(ctx, {
      email: "custom@example.com",
    });
  });

  const user = await t.run(async (ctx) => {
    return await ctx.db.get(userId);
  });

  expect(user?.email).toBe("custom@example.com");
});
```

#### 4. Randomized Testing

Use randomization to catch edge cases:

**Blog Post**: https://news.convex.dev/randomized-testing/

```typescript
it("handles random data correctly", async () => {
  const t = convexTest(schema);

  for (let i = 0; i < 100; i++) {
    const randomValue = Math.random() * 1000;

    await t.mutation(api.data.insert, {
      value: randomValue,
    });
  }

  const results = await t.query(api.data.getAll, {});
  expect(results.length).toBe(100);
});
```

### Integration Testing

Test interactions between components:

```typescript
describe("order fulfillment flow", () => {
  it("processes order end-to-end", async () => {
    const t = convexTest(schema);

    // Mock external payment API
    global.fetch = vi.fn().mockResolvedValue({
      json: async () => ({ status: "succeeded", id: "pay_123" }),
    });

    // Create order
    const orderId = await t.mutation(api.orders.create, {
      items: [{ productId: "prod_1", quantity: 2 }],
      total: 59.98,
    });

    // Process payment (action)
    await t.action(api.payments.process, { orderId });

    // Verify order status updated
    const order = await t.run(async (ctx) => {
      return await ctx.db.get(orderId);
    });

    expect(order?.status).toBe("paid");
    expect(order?.paymentId).toBe("pay_123");
  });
});
```

### Use Cases

1. **Unit Tests**: Test individual functions with `convex-test`
2. **Integration Tests**: Test workflows across multiple functions
3. **Security Tests**: Verify authorization and authentication
4. **Data Integrity**: Ensure consistency constraints
5. **Edge Cases**: Test boundary conditions
6. **Regression Tests**: Prevent bugs from reoccurring

---

## 10. Framework Integration (Next.js/React)

Convex has first-class integration with React and Next.js.

**Official Documentation**: https://docs.convex.dev/client/react/nextjs/

### Next.js App Router Integration

#### 1. Installation

```bash
npm install convex
npx convex dev
```

#### 2. Create ConvexClientProvider

```typescript
// app/ConvexClientProvider.tsx
"use client";

import { ConvexProvider, ConvexReactClient } from "convex/react";
import { ReactNode } from "react";

const convex = new ConvexReactClient(process.env.NEXT_PUBLIC_CONVEX_URL!);

export function ConvexClientProvider({ children }: { children: ReactNode }) {
  return <ConvexProvider client={convex}>{children}</ConvexProvider>;
}
```

#### 3. Wrap App in Provider

```typescript
// app/layout.tsx
import { ConvexClientProvider } from "./ConvexClientProvider";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <ConvexClientProvider>{children}</ConvexClientProvider>
      </body>
    </html>
  );
}
```

#### 4. Use in Client Components

```typescript
// app/messages/page.tsx
"use client";

import { useQuery, useMutation } from "convex/react";
import { api } from "@/convex/_generated/api";

export default function MessagesPage() {
  const messages = useQuery(api.messages.list);
  const sendMessage = useMutation(api.messages.send);

  const handleSend = () => {
    sendMessage({ text: "Hello!", channelId: "general" });
  };

  if (!messages) return <div>Loading...</div>;

  return (
    <div>
      {messages.map((msg) => (
        <div key={msg._id}>{msg.text}</div>
      ))}
      <button onClick={handleSend}>Send</button>
    </div>
  );
}
```

### Server Rendering with Preloading (Next.js 15)

For optimal performance with server rendering while maintaining reactivity:

**Official Documentation**: https://docs.convex.dev/client/react/nextjs/server-rendering

#### Server Component with Preloading

```typescript
// app/dashboard/page.tsx
import { preloadQuery } from "convex/nextjs";
import { api } from "@/convex/_generated/api";
import { DashboardClient } from "./DashboardClient";

export default async function DashboardPage() {
  const preloadedData = await preloadQuery(api.dashboard.getData);

  return <DashboardClient preloadedData={preloadedData} />;
}
```

#### Client Component Using Preloaded Data

```typescript
// app/dashboard/DashboardClient.tsx
"use client";

import { Preloaded, usePreloadedQuery } from "convex/react";
import { api } from "@/convex/_generated/api";

export function DashboardClient({
  preloadedData,
}: {
  preloadedData: Preloaded<typeof api.dashboard.getData>;
}) {
  // Still reactive - updates automatically!
  const data = usePreloadedQuery(preloadedData);

  return <div>{/* Render data */}</div>;
}
```

### Static/Non-Reactive Server Data

For data that doesn't need reactivity:

```typescript
import { fetchQuery } from "convex/nextjs";
import { api } from "@/convex/_generated/api";

export default async function StaticPage() {
  const data = await fetchQuery(api.content.get, { slug: "about" });

  return <div>{data.content}</div>;
}
```

### Authentication with Clerk (Next.js 15)

**Official Documentation**: https://clerk.com/docs/integrations/databases/convex

#### Setup

```bash
npm install @clerk/nextjs
```

#### Wrap Providers

```typescript
// app/layout.tsx
import { ClerkProvider } from "@clerk/nextjs";
import { ConvexClientProvider } from "./ConvexClientProvider";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <ClerkProvider>
      <html lang="en">
        <body>
          <ConvexClientProvider>{children}</ConvexClientProvider>
        </body>
      </html>
    </ClerkProvider>
  );
}
```

**Important**: ClerkProvider must wrap ConvexClientProvider.

#### Update ConvexClientProvider

```typescript
// app/ConvexClientProvider.tsx
"use client";

import { ClerkProvider, useAuth } from "@clerk/nextjs";
import { ConvexProviderWithClerk } from "convex/react-clerk";
import { ConvexReactClient } from "convex/react";

const convex = new ConvexReactClient(process.env.NEXT_PUBLIC_CONVEX_URL!);

export function ConvexClientProvider({ children }: { children: ReactNode }) {
  return (
    <ConvexProviderWithClerk client={convex} useAuth={useAuth}>
      {children}
    </ConvexProviderWithClerk>
  );
}
```

#### Protected Routes

```typescript
"use client";

import { Authenticated, Unauthenticated } from "convex/react";
import { SignInButton, UserButton } from "@clerk/nextjs";

export default function ProtectedPage() {
  return (
    <>
      <Authenticated>
        <UserButton />
        <Dashboard />
      </Authenticated>
      <Unauthenticated>
        <SignInButton mode="modal" />
      </Unauthenticated>
    </>
  );
}
```

### React Hooks

#### useQuery

Fetch data reactively:

```typescript
import { useQuery } from "convex/react";
import { api } from "@/convex/_generated/api";

function MyComponent() {
  const data = useQuery(api.myQuery, { arg: "value" });

  if (data === undefined) return <div>Loading...</div>;

  return <div>{JSON.stringify(data)}</div>;
}
```

#### useMutation

Call mutations:

```typescript
import { useMutation } from "convex/react";
import { api } from "@/convex/_generated/api";

function MyComponent() {
  const updateData = useMutation(api.myMutation);

  const handleClick = () => {
    updateData({ key: "value" });
  };

  return <button onClick={handleClick}>Update</button>;
}
```

#### useAction

Call actions:

```typescript
import { useAction } from "convex/react";
import { api } from "@/convex/_generated/api";

function MyComponent() {
  const processPayment = useAction(api.payments.process);

  const handlePayment = async () => {
    try {
      const result = await processPayment({ amount: 1000 });
      console.log("Payment successful:", result);
    } catch (error) {
      console.error("Payment failed:", error);
    }
  };

  return <button onClick={handlePayment}>Pay $10</button>;
}
```

### Optimistic Updates

Update UI immediately before mutation completes:

```typescript
import { useMutation } from "convex/react";
import { api } from "@/convex/_generated/api";
import { useOptimisticMutation } from "./useOptimisticMutation";

function TodoList() {
  const todos = useQuery(api.todos.list);
  const addTodo = useMutation(api.todos.add);

  const optimisticAddTodo = useOptimisticMutation(addTodo, {
    optimisticUpdate: (localStore, args) => {
      const currentTodos = localStore.getQuery(api.todos.list) || [];
      localStore.setQuery(api.todos.list, {}, [
        ...currentTodos,
        {
          _id: "temp-id",
          _creationTime: Date.now(),
          ...args,
          completed: false,
        },
      ]);
    },
  });

  return (
    <div>
      {todos?.map((todo) => (
        <div key={todo._id}>{todo.text}</div>
      ))}
      <button onClick={() => optimisticAddTodo({ text: "New todo" })}>
        Add Todo
      </button>
    </div>
  );
}
```

### Pagination

```typescript
import { usePaginatedQuery } from "convex/react";
import { api } from "@/convex/_generated/api";

function PaginatedList() {
  const { results, status, loadMore } = usePaginatedQuery(
    api.items.paginate,
    {},
    { initialNumItems: 20 }
  );

  return (
    <div>
      {results.map((item) => (
        <div key={item._id}>{item.name}</div>
      ))}
      {status === "CanLoadMore" && (
        <button onClick={() => loadMore(20)}>Load More</button>
      )}
    </div>
  );
}
```

### Use Cases

1. **Real-time Dashboards**: Live updating data with useQuery
2. **Chat Applications**: Real-time messaging with reactive queries
3. **Collaborative Tools**: Live collaboration with Convex sync
4. **E-commerce**: Product catalogs with server rendering
5. **Social Media**: Real-time feeds and notifications
6. **Admin Panels**: CRUD interfaces with mutations

---

## 11. File Storage

Convex provides built-in file storage for uploading and serving files.

**Official Documentation**: https://docs.convex.dev/file-storage/upload-files

### Upload Process

Convex uses a three-step upload process:

1. **Get upload URL**: Call a mutation to generate a short-lived upload URL
2. **POST file**: Upload the file to that URL
3. **Save storage ID**: Store the returned storage ID in your database

### Method 1: Upload URL Pattern

#### Step 1: Generate Upload URL

```typescript
// convex/files.ts
import { mutation } from "./_generated/server";

export const generateUploadUrl = mutation({
  handler: async (ctx) => {
    // Check authentication
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Not authenticated");
    }

    // Generate upload URL (expires in 1 hour)
    return await ctx.storage.generateUploadUrl();
  },
});
```

#### Step 2: Upload from Client

```typescript
// Client-side upload
import { useMutation } from "convex/react";
import { api } from "@/convex/_generated/api";

function FileUpload() {
  const generateUploadUrl = useMutation(api.files.generateUploadUrl);
  const saveFile = useMutation(api.files.save);

  const handleUpload = async (file: File) => {
    // 1. Get upload URL
    const uploadUrl = await generateUploadUrl();

    // 2. POST file to URL
    const response = await fetch(uploadUrl, {
      method: "POST",
      headers: { "Content-Type": file.type },
      body: file,
    });

    const { storageId } = await response.json();

    // 3. Save storage ID
    await saveFile({
      storageId,
      filename: file.name,
      type: file.type,
    });
  };

  return (
    <input
      type="file"
      onChange={(e) => {
        const file = e.target.files?.[0];
        if (file) handleUpload(file);
      }}
    />
  );
}
```

#### Step 3: Save File Metadata

```typescript
import { mutation } from "./_generated/server";
import { v } from "convex/values";

export const save = mutation({
  args: {
    storageId: v.string(),
    filename: v.string(),
    type: v.string(),
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    await ctx.db.insert("files", {
      storageId: args.storageId,
      filename: args.filename,
      type: args.type,
      userId: identity.subject,
      uploadedAt: Date.now(),
    });
  },
});
```

### Method 2: HTTP Action Pattern

For more control, use HTTP actions to handle the entire upload flow:

```typescript
import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";

const http = httpRouter();

http.route({
  path: "/upload",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    // Get file from form data
    const blob = await request.blob();

    // Store file
    const storageId = await ctx.storage.store(blob);

    // Save metadata
    await ctx.runMutation(api.files.save, {
      storageId,
      filename: request.headers.get("x-filename") || "unknown",
      type: blob.type,
    });

    return new Response(JSON.stringify({ storageId }), {
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
      },
    });
  }),
});

export default http;
```

### Retrieving Files

#### Get File URL

```typescript
import { query } from "./_generated/server";
import { v } from "convex/values";

export const getUrl = query({
  args: { storageId: v.string() },
  handler: async (ctx, args) => {
    return await ctx.storage.getUrl(args.storageId);
  },
});
```

#### Client-side Usage

```typescript
import { useQuery } from "convex/react";
import { api } from "@/convex/_generated/api";

function FileDisplay({ storageId }: { storageId: string }) {
  const url = useQuery(api.files.getUrl, { storageId });

  if (!url) return <div>Loading...</div>;

  return <img src={url} alt="Uploaded file" />;
}
```

### Image Upload with Preview

```typescript
"use client";

import { useState } from "react";
import { useMutation } from "convex/react";
import { api } from "@/convex/_generated/api";

export function ImageUpload() {
  const [preview, setPreview] = useState<string>();
  const generateUploadUrl = useMutation(api.files.generateUploadUrl);
  const saveImage = useMutation(api.files.saveImage);

  const handleImageUpload = async (file: File) => {
    // Show preview
    const reader = new FileReader();
    reader.onloadend = () => setPreview(reader.result as string);
    reader.readAsDataURL(file);

    // Upload to Convex
    const uploadUrl = await generateUploadUrl();

    const response = await fetch(uploadUrl, {
      method: "POST",
      headers: { "Content-Type": file.type },
      body: file,
    });

    const { storageId } = await response.json();

    await saveImage({
      storageId,
      filename: file.name,
    });
  };

  return (
    <div>
      <input
        type="file"
        accept="image/*"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleImageUpload(file);
        }}
      />
      {preview && <img src={preview} alt="Preview" width={200} />}
    </div>
  );
}
```

### File Metadata Schema

```typescript
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  files: defineTable({
    storageId: v.string(),
    filename: v.string(),
    type: v.string(),
    size: v.optional(v.number()),
    userId: v.string(),
    uploadedAt: v.number(),
  })
    .index("by_user", ["userId"])
    .index("by_storage_id", ["storageId"]),
});
```

### Deleting Files

```typescript
import { mutation } from "./_generated/server";
import { v } from "convex/values";

export const deleteFile = mutation({
  args: { fileId: v.id("files") },
  handler: async (ctx, args) => {
    const file = await ctx.db.get(args.fileId);
    if (!file) throw new Error("File not found");

    // Check authorization
    const identity = await ctx.auth.getUserIdentity();
    if (file.userId !== identity?.subject) {
      throw new Error("Unauthorized");
    }

    // Delete from storage
    await ctx.storage.delete(file.storageId);

    // Delete metadata
    await ctx.db.delete(args.fileId);
  },
});
```

### Use Cases

1. **Profile Pictures**: User avatar uploads
2. **Document Management**: PDF, Word, Excel file storage
3. **Media Libraries**: Image and video galleries
4. **Attachments**: Email or message attachments
5. **Generated Files**: Store generated PDFs, images, etc.
6. **Backups**: Store exported data

### Constraints

- Upload URL expires in **1 hour**
- Upload POST request has **2 minute timeout**
- No file size limit (but timeout applies)
- All file types supported

### Using with Components

For production file storage with CDN, use Cloudflare R2 component:

```typescript
import { R2 } from "@convex-dev/r2";

const r2 = new R2(components.r2);

export const uploadToR2 = action({
  handler: async (ctx, args) => {
    await r2.put(ctx, {
      key: args.filename,
      value: args.data,
    });
  },
});
```

---

## 12. Real-time Subscriptions

Convex's reactive database provides automatic real-time updates to connected clients.

**Official Documentation**: https://docs.convex.dev/realtime

### How Reactivity Works

The Convex database is **reactive**: whenever data on which a query depends changes, the query is rerun and client subscriptions are updated automatically.

**Key Features**:
- Tracks all dependencies for every query
- Automatically reruns queries when dependencies change
- Pushes updates to clients via WebSocket
- No manual cache invalidation needed
- Guarantees consistent views across all queries

### Basic Subscription

#### Query Function

```typescript
// convex/messages.ts
import { query } from "./_generated/server";
import { v } from "convex/values";

export const list = query({
  args: { channelId: v.id("channels") },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("messages")
      .withIndex("by_channel", (q) => q.eq("channelId", args.channelId))
      .order("desc")
      .collect();
  },
});
```

#### Client Subscription

```typescript
"use client";

import { useQuery } from "convex/react";
import { api } from "@/convex/_generated/api";

export function MessageList({ channelId }: { channelId: Id<"channels"> }) {
  // Automatically subscribes and updates on changes
  const messages = useQuery(api.messages.list, { channelId });

  if (messages === undefined) return <div>Loading...</div>;

  return (
    <div>
      {messages.map((message) => (
        <div key={message._id}>{message.text}</div>
      ))}
    </div>
  );
}
```

### How It Works

1. **First Call**: `useQuery` creates a subscription to your Convex backend
2. **WebSocket**: `ConvexReactClient` maintains a WebSocket connection
3. **Dependency Tracking**: Convex tracks which database rows the query read
4. **Change Detection**: When any tracked row changes, Convex detects it
5. **Rerun**: Query is automatically rerun with new data
6. **Push**: New results pushed to client via WebSocket
7. **Rerender**: React component rerenders with updated data

### Automatic Caching

Convex automatically caches query results:
- Future calls read from cache
- Cache updates when underlying data changes
- No manual cache invalidation needed

### Consistent Views

Convex ensures your application always renders a consistent view:
- All `useQuery` calls reflect the same database state
- Never renders inconsistent state where only some queries reflect new data
- Guarantees based on single logical database state

### Real-time Chat Example

#### Schema

```typescript
export default defineSchema({
  channels: defineTable({
    name: v.string(),
    createdAt: v.number(),
  }),

  messages: defineTable({
    channelId: v.id("channels"),
    text: v.string(),
    userId: v.string(),
    timestamp: v.number(),
  }).index("by_channel", ["channelId"]),

  presence: defineTable({
    channelId: v.id("channels"),
    userId: v.string(),
    lastSeen: v.number(),
  })
    .index("by_channel", ["channelId"])
    .index("by_user", ["userId"]),
});
```

#### Queries

```typescript
// Get messages (real-time)
export const getMessages = query({
  args: { channelId: v.id("channels") },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("messages")
      .withIndex("by_channel", (q) => q.eq("channelId", args.channelId))
      .order("desc")
      .take(100);
  },
});

// Get online users (real-time)
export const getOnlineUsers = query({
  args: { channelId: v.id("channels") },
  handler: async (ctx, args) => {
    const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;

    return await ctx.db
      .query("presence")
      .withIndex("by_channel", (q) => q.eq("channelId", args.channelId))
      .filter((q) => q.gt(q.field("lastSeen"), fiveMinutesAgo))
      .collect();
  },
});
```

#### Mutations

```typescript
// Send message
export const send = mutation({
  args: {
    channelId: v.id("channels"),
    text: v.string(),
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    await ctx.db.insert("messages", {
      channelId: args.channelId,
      text: args.text,
      userId: identity.subject,
      timestamp: Date.now(),
    });
  },
});

// Update presence
export const updatePresence = mutation({
  args: { channelId: v.id("channels") },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    const existing = await ctx.db
      .query("presence")
      .withIndex("by_user", (q) => q.eq("userId", identity.subject))
      .first();

    if (existing) {
      await ctx.db.patch(existing._id, { lastSeen: Date.now() });
    } else {
      await ctx.db.insert("presence", {
        channelId: args.channelId,
        userId: identity.subject,
        lastSeen: Date.now(),
      });
    }
  },
});
```

#### React Component

```typescript
"use client";

import { useQuery, useMutation } from "convex/react";
import { api } from "@/convex/_generated/api";
import { useEffect } from "react";

export function ChatRoom({ channelId }: { channelId: Id<"channels"> }) {
  const messages = useQuery(api.messages.getMessages, { channelId });
  const onlineUsers = useQuery(api.presence.getOnlineUsers, { channelId });
  const sendMessage = useMutation(api.messages.send);
  const updatePresence = useMutation(api.presence.updatePresence);

  // Update presence every 30 seconds
  useEffect(() => {
    updatePresence({ channelId });
    const interval = setInterval(() => {
      updatePresence({ channelId });
    }, 30000);

    return () => clearInterval(interval);
  }, [channelId, updatePresence]);

  return (
    <div className="flex">
      <div className="flex-1">
        <div className="messages">
          {messages?.map((msg) => (
            <div key={msg._id}>{msg.text}</div>
          ))}
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const text = new FormData(e.currentTarget).get("text") as string;
            sendMessage({ channelId, text });
            e.currentTarget.reset();
          }}
        >
          <input name="text" placeholder="Type a message..." />
        </form>
      </div>

      <div className="w-48">
        <h3>Online ({onlineUsers?.length})</h3>
        {onlineUsers?.map((user) => (
          <div key={user._id}>{user.userId}</div>
        ))}
      </div>
    </div>
  );
}
```

### Real-time Dashboard Example

```typescript
// Real-time analytics query
export const getAnalytics = query({
  handler: async (ctx) => {
    const now = Date.now();
    const oneDayAgo = now - 24 * 60 * 60 * 1000;

    const [totalUsers, activeUsers, totalRevenue] = await Promise.all([
      ctx.db.query("users").collect().then((users) => users.length),
      ctx.db
        .query("users")
        .filter((q) => q.gt(q.field("lastActive"), oneDayAgo))
        .collect()
        .then((users) => users.length),
      ctx.db
        .query("orders")
        .filter((q) => q.gte(q.field("createdAt"), oneDayAgo))
        .collect()
        .then((orders) =>
          orders.reduce((sum, order) => sum + order.amount, 0)
        ),
    ]);

    return {
      totalUsers,
      activeUsers,
      totalRevenue,
      updatedAt: now,
    };
  },
});
```

```typescript
"use client";

export function Dashboard() {
  const analytics = useQuery(api.analytics.getAnalytics);

  return (
    <div>
      <h1>Real-time Dashboard</h1>
      <div className="grid grid-cols-3 gap-4">
        <div className="card">
          <h2>Total Users</h2>
          <p className="text-4xl">{analytics?.totalUsers}</p>
        </div>
        <div className="card">
          <h2>Active Users (24h)</h2>
          <p className="text-4xl">{analytics?.activeUsers}</p>
        </div>
        <div className="card">
          <h2>Revenue (24h)</h2>
          <p className="text-4xl">${analytics?.totalRevenue?.toFixed(2)}</p>
        </div>
      </div>
      <p className="text-sm text-gray-500">
        Last updated: {new Date(analytics?.updatedAt || 0).toLocaleTimeString()}
      </p>
    </div>
  );
}
```

### TanStack Query Integration

For applications already using TanStack Query (React Query), Convex provides an adapter:

**Documentation**: https://docs.convex.dev/client/tanstack/tanstack-query/

```typescript
import { useConvexQuery } from "@convex-dev/react-query";
import { api } from "@/convex/_generated/api";

function MyComponent() {
  const { data, isLoading } = useConvexQuery(
    api.messages.list,
    { channelId: "channel123" }
  );

  return <div>{/* Use data */}</div>;
}
```

### Use Cases

1. **Chat Applications**: Real-time messaging
2. **Collaborative Editing**: Live document collaboration
3. **Live Dashboards**: Real-time analytics and monitoring
4. **Multiplayer Games**: Real-time game state
5. **Social Feeds**: Live-updating social media feeds
6. **Presence Indicators**: Show who's online
7. **Notification Systems**: Real-time notifications
8. **Live Auctions**: Real-time bidding
9. **Stock Tickers**: Real-time price updates
10. **IoT Dashboards**: Live sensor data

### Performance

- **Efficient**: Only sends updates for changed data
- **Scalable**: Handles thousands of concurrent subscriptions
- **Smart**: Batches updates when multiple changes occur
- **Automatic**: No manual subscription management needed

---

## Summary

Convex provides a comprehensive backend platform with:

1. **Built-in Authentication**: Convex Auth with 80+ providers
2. **Clear Function Model**: Queries (read), Mutations (write), Actions (external)
3. **External Integration**: HTTP actions, webhooks, API integrations
4. **Scheduling**: One-time scheduled functions and recurring cron jobs
5. **HTTP Endpoints**: Custom REST APIs and webhook receivers
6. **Vector Search**: Built-in semantic search for AI applications
7. **Components**: Modular, reusable backend functionality
8. **Secure Configuration**: Environment variables for secrets
9. **Comprehensive Testing**: Unit tests with convex-test
10. **Framework Integration**: First-class Next.js and React support
11. **File Storage**: Built-in file upload and serving
12. **Real-time Sync**: Automatic reactive subscriptions

This makes Convex ideal for building modern web applications that require real-time updates, AI capabilities, and seamless integration with external services.

---

## Additional Resources

- **Official Documentation**: https://docs.convex.dev
- **Stack Blog**: https://stack.convex.dev
- **GitHub**: https://github.com/get-convex
- **Discord Community**: https://convex.dev/community
- **Templates**: https://www.convex.dev/templates
- **Components**: https://www.convex.dev/components
