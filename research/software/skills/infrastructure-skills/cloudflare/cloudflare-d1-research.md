# Cloudflare D1 - Comprehensive Research Report

**Research Date:** November 17, 2025
**Status:** Generally Available (GA since April 2024)

## Executive Summary

Cloudflare D1 is a serverless SQL database built on SQLite that runs on Cloudflare's global network. It provides native integration with Cloudflare Workers and Pages, offers built-in disaster recovery through Time Travel, and is designed for horizontal scaling with thousands of smaller databases rather than vertical scaling of large monolithic databases.

---

## 1. Core Features and Capabilities

### 1.1 Primary Characteristics

- **SQLite-based**: Built on SQLite's query engine with full SQL compatibility
- **Serverless**: No provisioning required, scale-to-zero billing model
- **Global Distribution**: Runs on Cloudflare's edge network with automatic replication
- **Database Size**: Up to 10 GB per database (increased from 2GB)
- **Scale Model**: Designed for horizontal scale-out with multiple smaller databases (per-user, per-tenant, or per-entity)
- **Database Limits**: Up to 50,000 databases per account on Workers Paid plan (can be increased to millions on request)

### 1.2 Key Features

#### Time Travel (Point-in-Time Recovery)
- Restore database to any minute within the last 30 days
- Built-in disaster recovery and backup solution
- No additional cost or configuration required

#### Global Read Replication (Beta - April 2025)
- Automatic read replicas provisioned in every region
- Reduces query latency by routing to nearest replica
- Sequential consistency guaranteed across all replicas
- No additional cost for replicas
- Requires Sessions API to enable

#### Supported SQLite Extensions
- **FTS5**: Full-text search module (case-sensitive, use lowercase `fts5`)
- **JSON Extension**: Complete JSON querying and manipulation functions
- **Math Functions**: Standard mathematical operations

#### Batch Operations
- Execute multiple SQL statements in a single API call
- Massive performance improvement by reducing network round trips
- **Transaction Behavior**: Conflicting documentation exists:
  - Newer docs indicate batches ARE SQL transactions with rollback support
  - Older docs indicate batches are NOT true transactions (auto-commit mode)
  - Recommendation: Test for your specific use case

### 1.3 General Availability Status

- **GA Date**: April 1, 2024 (Developer Week)
- **Journey**: Alpha (late 2022) → Open Beta (September 2023) → GA (April 2024)
- **Production Ready**: Fully supported for production workloads

---

## 2. API Patterns and Common Usage

### 2.1 Access Methods

D1 provides three primary ways to interact with databases:

#### Workers Binding API (Primary Method)
```javascript
// Binding defined in wrangler.toml
export default {
  async fetch(request, env) {
    const { DB } = env; // D1 binding

    // Single query
    const result = await DB.prepare("SELECT * FROM users WHERE id = ?")
      .bind(userId)
      .first();

    // Batch queries
    const results = await DB.batch([
      DB.prepare("INSERT INTO users (name) VALUES (?)").bind("Alice"),
      DB.prepare("INSERT INTO users (name) VALUES (?)").bind("Bob"),
      DB.prepare("SELECT * FROM users")
    ]);

    return new Response(JSON.stringify(result));
  }
};
```

**Key Methods:**
- `DB.prepare(sql)` - Create prepared statement
- `.bind(...params)` - Bind parameters (prevents SQL injection)
- `.first()` - Return first result row as object
- `.all()` - Return all results with array of rows
- `.run()` - Execute without returning results
- `.raw()` - Return results as arrays instead of objects (performance optimized)
- `DB.batch([statements])` - Execute multiple statements

#### Sessions API (Required for Read Replication)
```javascript
export default {
  async fetch(request, env) {
    const { DB } = env;

    // Create a session for sequential consistency
    const session = DB.withSession();

    // All queries within session maintain consistency
    const user = await session.prepare("SELECT * FROM users WHERE id = ?")
      .bind(userId)
      .first();

    await session.prepare("UPDATE users SET last_login = ? WHERE id = ?")
      .bind(Date.now(), userId)
      .run();

    return new Response(JSON.stringify(user));
  }
};
```

**Session Characteristics:**
- Encapsulates all queries from one logical application session
- Ensures sequential consistency with bookmarks (Lamport timestamps)
- Guarantees: monotonic reads, read-your-writes, writes-follow-reads
- Required for read replication feature

#### REST API (Administrative Use)
```bash
# Query endpoint
POST https://api.cloudflare.com/client/v4/accounts/{account_id}/d1/database/{database_id}/query

# Raw query endpoint (performance optimized)
POST https://api.cloudflare.com/client/v4/accounts/{account_id}/d1/database/{database_id}/raw

# Export database
POST https://api.cloudflare.com/client/v4/accounts/{account_id}/d1/database/{database_id}/export

# Import database
POST https://api.cloudflare.com/client/v4/accounts/{account_id}/d1/database/{database_id}/import
```

**Note**: REST API subject to global Cloudflare API rate limits, best suited for administrative operations

### 2.2 Prepared Statements

D1 follows SQLite conventions for parameter binding:

- **Anonymous parameters**: `?` (recommended)
- **Ordered parameters**: `?1`, `?2`, `?3`, etc.
- **Named parameters**: Not currently supported (`:name`, `$name`, `@name`)

**Security Best Practice**: Always use prepared statements with `.bind()` to prevent SQL injection attacks.

### 2.3 Connection Limits

- Maximum 6 simultaneous connections per Worker invocation
- Each connection represents a separate query context

---

## 3. Data Model and Ontology

### 3.1 SQLite Foundation

D1 inherits SQLite's data model and type system:

#### Data Types
- **NULL**: Null value
- **INTEGER**: Signed integer (1, 2, 3, 4, 6, or 8 bytes)
- **REAL**: Floating point value (8-byte IEEE floating point)
- **TEXT**: Text string (UTF-8, UTF-16BE, or UTF-16LE)
- **BLOB**: Binary data stored exactly as input

#### Type Affinity
SQLite uses type affinity rather than strict typing. Columns have suggested types but can store other types.

**Recommendation**: Use `STRICT` tables to avoid type mismatches:
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  age INTEGER
) STRICT;
```

### 3.2 Schema Management

#### Foreign Keys
```sql
-- Enable foreign key constraints
PRAGMA foreign_keys = ON;

-- Define relationships
CREATE TABLE orders (
  id INTEGER PRIMARY KEY,
  customer_id INTEGER NOT NULL,
  FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

#### Virtual Tables (FTS5)
```sql
-- Full-text search index (lowercase fts5)
CREATE VIRTUAL TABLE documents_fts USING fts5(
  title,
  content,
  author
);

-- Insert data
INSERT INTO documents_fts (title, content, author)
VALUES ('My Title', 'Document content...', 'John Doe');

-- Search
SELECT * FROM documents_fts
WHERE documents_fts MATCH 'search query';
```

#### JSON Columns
```sql
-- Store JSON data
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  metadata TEXT -- Store JSON as TEXT
);

-- Query JSON data
SELECT
  id,
  json_extract(metadata, '$.name') as name,
  json_extract(metadata, '$.email') as email
FROM users
WHERE json_extract(metadata, '$.active') = true;
```

### 3.3 D1 Metadata Tables

D1 automatically creates metadata tables:
- **d1_migrations**: Tracks applied migrations (location configurable in wrangler.toml)

---

## 4. Integration Patterns with Cloudflare Services

### 4.1 Workers Integration (Primary)

D1 is designed to work seamlessly with Cloudflare Workers through bindings:

```toml
# wrangler.toml
name = "my-worker"
main = "src/index.js"

[[d1_databases]]
binding = "DB"
database_name = "my-database"
database_id = "uuid-here"
```

### 4.2 Pages Integration

Pages Functions can bind to D1 databases:

```toml
# wrangler.toml for Pages
[[d1_databases]]
binding = "DB"
database_name = "my-database"
database_id = "uuid-here"
```

Access in Pages Functions:
```javascript
export async function onRequest(context) {
  const { DB } = context.env;
  const results = await DB.prepare("SELECT * FROM posts").all();
  return new Response(JSON.stringify(results));
}
```

### 4.3 Multi-Service Architecture Patterns

#### Combining D1, KV, and R2
```javascript
export default {
  async fetch(request, env) {
    const { DB, MY_KV, MY_BUCKET } = env;

    // Use D1 for structured relational data
    const user = await DB.prepare("SELECT * FROM users WHERE id = ?")
      .bind(userId)
      .first();

    // Use KV for session/cache data (fast reads, 1 write/sec limit)
    const session = await MY_KV.get(`session:${sessionId}`);

    // Use R2 for large files/blobs
    const avatar = await MY_BUCKET.get(`avatars/${userId}.jpg`);

    return new Response(/* combined data */);
  }
};
```

**Service Selection Guidelines:**

| Service | Best For | Characteristics |
|---------|----------|-----------------|
| **D1** | Relational data, complex queries, ACID compliance | 10GB limit, SQL queries, transactions |
| **KV** | Session data, config, high-read cache | High read throughput, 1 write/sec per key, eventual consistency |
| **R2** | Large files, media, user uploads | Object storage, no egress fees, S3-compatible |
| **Durable Objects** | Strong consistency, coordination, real-time | Single-threaded, stateful, global uniqueness |

### 4.4 Framework Integration

D1 works with popular web frameworks:

- **Hono**: Lightweight web framework with D1 support
- **Remix**: Full-stack framework on Cloudflare Pages
- **SvelteKit**: Adapter available for Cloudflare
- **Next.js**: Can run on Cloudflare Pages (with limitations)

---

## 5. Best Practices and Limitations

### 5.1 Best Practices

#### Performance Optimization

**1. Use Indexes**
```sql
-- Create index with standard naming convention
CREATE INDEX IF NOT EXISTS idx_orders_customer_id
ON orders(customer_id);

-- Multi-column index (order matters)
CREATE INDEX IF NOT EXISTS idx_orders_customer_date
ON orders(customer_id, order_date DESC);

-- Partial index (smaller, faster)
CREATE INDEX IF NOT EXISTS idx_active_users
ON users(email)
WHERE active = true;

-- After creating indexes, analyze
PRAGMA optimize;
```

**2. Validate Index Usage**
```sql
-- Check if query uses index
EXPLAIN QUERY PLAN
SELECT * FROM orders WHERE customer_id = 123;
```

**3. Monitor Query Efficiency**
- Metric: `queryEfficiency = rows_returned / rows_read`
- Goal: Get as close to 1.0 as possible
- Higher efficiency = better performance + lower costs

**4. Use Batch Operations**
```javascript
// BAD: Multiple round trips
for (const user of users) {
  await DB.prepare("INSERT INTO users (name) VALUES (?)").bind(user.name).run();
}

// GOOD: Single batch operation
const statements = users.map(user =>
  DB.prepare("INSERT INTO users (name) VALUES (?)").bind(user.name)
);
await DB.batch(statements);
```

**5. Pagination Best Practices**
```sql
-- BAD: Offset-based (scans all rows)
SELECT * FROM posts ORDER BY created_at DESC LIMIT 20 OFFSET 1000;

-- GOOD: Cursor-based
SELECT * FROM posts
WHERE created_at < ?
ORDER BY created_at DESC
LIMIT 20;

-- Index for pagination
CREATE INDEX idx_posts_pagination
ON posts(post_type, created_at DESC);
```

**Avoid COUNT Queries**:
```sql
-- BAD: Full table scan even with indexes
SELECT COUNT(*) FROM users;

-- GOOD: Avoid total counts in paginated results
-- Use "has more" indicator instead
```

#### Security Best Practices

**1. Always Use Prepared Statements**
```javascript
// BAD: SQL injection vulnerability
const result = await DB.prepare(
  `SELECT * FROM users WHERE email = '${userInput}'`
).all();

// GOOD: Parameterized query
const result = await DB.prepare(
  "SELECT * FROM users WHERE email = ?"
).bind(userInput).all();
```

**2. Implement Access Control**
- D1 has no built-in authentication/authorization
- Implement access control in Worker layer
- Use API keys or bearer tokens for external access
- Create proxy Workers with custom authorization logic

**3. Data Security**
- TLS/SSL encryption in transit (Workers ↔ D1)
- REST API over HTTPS
- Store sensitive data (API keys) as Worker secrets

#### Development Best Practices

**1. Local Development**
```bash
# Persist local database
wrangler dev --persist-to=./local-db

# Reset database (useful for testing)
# Always use DROP TABLE before CREATE TABLE in dev
```

**2. Use STRICT Tables**
```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  email TEXT NOT NULL,
  age INTEGER
) STRICT;
```

**3. Migrations**
```bash
# Create migration
wrangler d1 migrations create my-database add_users_table

# Apply locally
wrangler d1 migrations apply my-database --local

# Apply to production
wrangler d1 migrations apply my-database --remote
```

### 5.2 Limitations

#### Storage and Scale Limits

| Limit | Free Tier | Paid Tier |
|-------|-----------|-----------|
| **Database Size** | 500 MB per DB | 10 GB per DB (hard limit) |
| **Total Databases** | 10 per account | 50,000 per account |
| **Total Storage** | N/A | 1 TB per account |
| **Daily Reads** | 5 million rows | Unlimited (pay per use) |
| **Daily Writes** | 100,000 rows | Unlimited (pay per use) |

**Important Notes:**
- 10 GB per-database limit cannot be increased
- For larger datasets, use horizontal scaling (multiple databases)
- Empty database consumes ~12 KB

#### Technical Limitations

**1. SQLite Compatibility**
- Some SQLite features may not be available
- ALTER TABLE support is limited (SQLite constraint)
- Cannot import raw .sqlite3 files (must export to SQL first)

**2. Query Limits**
- Maximum 100 bound parameters per query
- Impacts bulk inserts: with 10 columns, max 10 rows per INSERT

**3. Virtual Tables**
- FTS5 support available
- Export not supported for databases with virtual tables (workaround: drop, export, recreate)

**4. Concurrent Operations**
- Maximum 6 connections per Worker invocation
- Write throughput depends on primary database location

**5. Foreign Keys**
- Must explicitly enable: `PRAGMA foreign_keys = ON;`
- Use `PRAGMA defer_foreign_keys = true` when importing data

**6. Import/Export Limits**
- File size limit: 5 GiB (matches R2 upload limit)
- Large imports may require splitting into multiple files

#### Cost Considerations

**Pricing Model** (as of 2024):
- **Rows Read**: Charged per row scanned (not returned)
- **Rows Written**: INSERT, UPDATE, DELETE operations
- **Storage**: Per GB per month (tables + indexes)
- **No egress charges**: Free data transfer

**Cost Optimization Tips:**
- Create indexes to reduce rows scanned
- Use `.raw()` for better performance
- Implement cursor-based pagination
- Avoid COUNT(*) queries
- Use read replication for read-heavy workloads

---

## 6. Common Use Cases and Examples

### 6.1 Ideal Use Cases

#### Multi-Tenant Applications
```javascript
// Per-tenant database isolation
export default {
  async fetch(request, env) {
    const tenantId = getTenantFromRequest(request);
    const DB = env[`TENANT_${tenantId}_DB`]; // Dynamic binding

    const data = await DB.prepare("SELECT * FROM tenant_data").all();
    return new Response(JSON.stringify(data));
  }
};
```

**Benefits:**
- Complete data isolation between tenants
- Independent scaling per tenant
- Simplified GDPR compliance (tenant deletion)
- Up to 50,000 tenants on paid plan

#### Per-User Databases
- User-specific data storage
- Personal notes, preferences, settings
- Gaming: player state, inventory, achievements

#### E-Commerce Applications
- Product catalogs
- Order management
- Customer data
- Shopping cart persistence

#### Content Management
- Blog posts and comments
- Static site dynamic features
- User-generated content

#### Job Boards and Listings
- Job postings
- Applications tracking
- Conference schedules

### 6.2 Code Examples

#### Adding Comments to Static Blog
```javascript
// POST /api/comments
export default {
  async fetch(request, env) {
    const { DB } = env;

    if (request.method === 'POST') {
      const { postId, author, content } = await request.json();

      await DB.prepare(`
        INSERT INTO comments (post_id, author, content, created_at)
        VALUES (?, ?, ?, ?)
      `).bind(postId, author, content, Date.now()).run();

      return new Response('Comment added', { status: 201 });
    }

    // GET comments
    const url = new URL(request.url);
    const postId = url.searchParams.get('postId');

    const comments = await DB.prepare(`
      SELECT * FROM comments
      WHERE post_id = ?
      ORDER BY created_at DESC
    `).bind(postId).all();

    return new Response(JSON.stringify(comments.results));
  }
};
```

#### Authentication Implementation
```javascript
import bcrypt from 'bcryptjs';

export default {
  async fetch(request, env) {
    const { DB } = env;
    const { email, password } = await request.json();

    // Registration
    if (request.url.endsWith('/register')) {
      const hashedPassword = await bcrypt.hash(password, 10);

      await DB.prepare(`
        INSERT INTO users (email, password_hash, created_at)
        VALUES (?, ?, ?)
      `).bind(email, hashedPassword, Date.now()).run();

      return new Response('User created', { status: 201 });
    }

    // Login
    if (request.url.endsWith('/login')) {
      const user = await DB.prepare(
        "SELECT * FROM users WHERE email = ?"
      ).bind(email).first();

      if (!user || !await bcrypt.compare(password, user.password_hash)) {
        return new Response('Invalid credentials', { status: 401 });
      }

      // Generate session token
      const token = crypto.randomUUID();
      await DB.prepare(`
        INSERT INTO sessions (user_id, token, expires_at)
        VALUES (?, ?, ?)
      `).bind(user.id, token, Date.now() + 86400000).run();

      return new Response(JSON.stringify({ token }));
    }
  }
};
```

#### Full-Text Search
```javascript
export default {
  async fetch(request, env) {
    const { DB } = env;
    const url = new URL(request.url);
    const query = url.searchParams.get('q');

    // Search using FTS5
    const results = await DB.prepare(`
      SELECT
        documents.*,
        highlight(documents_fts, 1, '<mark>', '</mark>') as highlighted
      FROM documents_fts
      JOIN documents ON documents.id = documents_fts.rowid
      WHERE documents_fts MATCH ?
      ORDER BY rank
      LIMIT 20
    `).bind(query).all();

    return new Response(JSON.stringify(results.results));
  }
};
```

#### JSON Querying
```javascript
export default {
  async fetch(request, env) {
    const { DB } = env;

    // Query users with specific metadata
    const activeUsers = await DB.prepare(`
      SELECT
        id,
        json_extract(metadata, '$.name') as name,
        json_extract(metadata, '$.email') as email,
        json_extract(metadata, '$.preferences.theme') as theme
      FROM users
      WHERE json_extract(metadata, '$.active') = true
        AND json_extract(metadata, '$.preferences.notifications') = true
    `).all();

    return new Response(JSON.stringify(activeUsers.results));
  }
};
```

### 6.3 Real-World Examples

**Official Examples:**
- E-commerce store with read replication
- Job listing website (conference jobs)
- Purchase order administration system
- Hono framework integration
- Remix application integration
- SvelteKit application integration

---

## 7. Key Concepts and Terminology

### Core Concepts

**Binding**: Connection configuration in `wrangler.toml` that makes D1 databases available to Workers/Pages as `env.BINDING_NAME`

**Time Travel**: Built-in point-in-time recovery allowing restoration to any minute within last 30 days

**Read Replica**: Automatically created read-only database copies distributed globally for lower latency

**Session**: Logical grouping of queries with sequential consistency guarantees (required for read replication)

**Sequential Consistency**: Guarantee that all operations follow a global order with monotonic reads and read-your-writes properties

**Bookmark**: Lamport timestamp attached to queries ensuring replica is sufficiently up-to-date

**Query Efficiency**: Metric calculated as `rows_returned / rows_read`, target ~1.0 for optimal performance

**Prepared Statement**: Pre-compiled SQL query with parameter placeholders, prevents SQL injection

**Location Hint**: Optional geographic hint for primary database placement (weur, eeur, apac, oc, wnam, enam)

### SQLite Concepts

**Type Affinity**: SQLite's flexible typing system where columns have preferred types but accept others

**STRICT Mode**: Table option enforcing rigid type checking

**Virtual Table**: Special table type for extensions (e.g., FTS5 for full-text search)

**FTS5**: Full-Text Search module (version 5) for text search capabilities

**PRAGMA**: SQLite command for database configuration and analysis

**WAL Mode**: Write-Ahead Logging, SQLite's journaling mode (used by D1)

### Cloudflare Platform Concepts

**Workers**: Serverless execution environment on Cloudflare's edge network

**Pages**: Static site hosting with serverless functions

**Wrangler**: Official CLI tool for Cloudflare Workers and D1

**Edge**: Cloudflare's globally distributed network of data centers

**Binding**: Resource connection mechanism in Workers/Pages

---

## 8. ORM and Tooling Support

### 8.1 Drizzle ORM (Recommended)

**Status**: Full native support for D1

**Features:**
- TypeScript-first ORM
- Automatic schema generation from types
- SQL-like syntax
- Lightweight and performant
- Drizzle Kit for migrations
- Drizzle Studio for database UI

**Setup:**
```typescript
// drizzle.config.ts
import { defineConfig } from 'drizzle-kit';

export default defineConfig({
  schema: './src/schema.ts',
  out: './migrations',
  dialect: 'sqlite',
  driver: 'd1-http',
  dbCredentials: {
    accountId: process.env.CLOUDFLARE_ACCOUNT_ID,
    databaseId: process.env.CLOUDFLARE_DATABASE_ID,
    token: process.env.CLOUDFLARE_API_TOKEN,
  },
});
```

**Usage:**
```typescript
import { drizzle } from 'drizzle-orm/d1';
import { users } from './schema';

export default {
  async fetch(request, env) {
    const db = drizzle(env.DB);

    const allUsers = await db.select().from(users);
    const user = await db.select().from(users).where(eq(users.id, 1));

    return new Response(JSON.stringify(allUsers));
  }
};
```

### 8.2 Prisma ORM

**Status**: Supported as of version 5.12.0 (2024)

**Features:**
- Type-safe database client
- Declarative schema
- Migration system
- Prisma Studio for database UI

**Setup:**
```prisma
// schema.prisma
datasource db {
  provider = "sqlite"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
  previewFeatures = ["driverAdapters"]
}

model User {
  id    Int    @id @default(autoincrement())
  email String @unique
  name  String?
}
```

**Usage:**
```typescript
import { PrismaClient } from '@prisma/client';
import { PrismaD1 } from '@prisma/adapter-d1';

export default {
  async fetch(request, env) {
    const adapter = new PrismaD1(env.DB);
    const prisma = new PrismaClient({ adapter });

    const users = await prisma.user.findMany();

    return new Response(JSON.stringify(users));
  }
};
```

### 8.3 Other Tools

**Query Builders:**
- workers-qb: SQL query builder for Cloudflare Workers
- sqlite-cloudflare-d1: SQLite query builder

**Database Management:**
- DBCode: VS Code extension for D1 database management
- Drizzle Studio: Visual database browser
- Prisma Studio: Database GUI

**Authentication Libraries:**
- Auth.js: Authentication adapter for D1
- Better Auth: Modern auth library with D1 support

---

## 9. Wrangler CLI Commands

### Database Management

```bash
# Create database
wrangler d1 create <DATABASE_NAME> [--location=weur]

# List databases
wrangler d1 list

# Delete database
wrangler d1 delete <DATABASE_NAME>

# Database info
wrangler d1 info <DATABASE_NAME>
```

### Migrations

```bash
# Create new migration
wrangler d1 migrations create <DATABASE_NAME> <MIGRATION_NAME>

# List migrations
wrangler d1 migrations list <DATABASE_NAME> [--local|--remote]

# Apply migrations
wrangler d1 migrations apply <DATABASE_NAME> --local
wrangler d1 migrations apply <DATABASE_NAME> --remote
wrangler d1 migrations apply <DATABASE_NAME> --preview
```

### Execute Queries

```bash
# Execute SQL command
wrangler d1 execute <DATABASE_NAME> \
  --command="SELECT * FROM users" \
  --local

# Execute SQL file
wrangler d1 execute <DATABASE_NAME> \
  --file=./schema.sql \
  --remote

# JSON output
wrangler d1 execute <DATABASE_NAME> \
  --command="SELECT * FROM users" \
  --json \
  --remote
```

### Import/Export

```bash
# Export database
wrangler d1 export <DATABASE_NAME> \
  --remote \
  --output=./backup.sql

# Export schema only (no data)
wrangler d1 export <DATABASE_NAME> \
  --remote \
  --no-data \
  --output=./schema.sql

# Export specific table
wrangler d1 export <DATABASE_NAME> \
  --remote \
  --table=users \
  --output=./users.sql

# Import database
wrangler d1 execute <DATABASE_NAME> \
  --file=./backup.sql \
  --remote
```

### Local Development

```bash
# Start dev server with local D1
wrangler dev --local --persist

# Persist to specific location
wrangler dev --persist-to=./local-db

# Remote bindings (connect to remote D1 during dev)
wrangler dev --remote
```

---

## 10. Advanced Features

### 10.1 Global Read Replication

**Status**: Public Beta (as of April 2025)

**Overview:**
Automatically provisions read replicas globally, routing queries to nearest replica while maintaining sequential consistency.

**Benefits:**
- Reduced latency for read queries
- Increased read throughput
- No additional cost
- Automatic management by Cloudflare

**Requirements:**
- Must use Sessions API
- Database must have replication enabled

**Implementation:**
```javascript
export default {
  async fetch(request, env) {
    const { DB } = env;

    // Create session for consistency
    const session = DB.withSession();

    // Read queries automatically routed to nearest replica
    const posts = await session.prepare(
      "SELECT * FROM posts ORDER BY created_at DESC LIMIT 10"
    ).all();

    // Write queries always go to primary
    await session.prepare(
      "UPDATE posts SET views = views + 1 WHERE id = ?"
    ).bind(postId).run();

    return new Response(JSON.stringify(posts.results));
  }
};
```

**Consistency Guarantees:**
- **Monotonic Reads**: Later reads never see older data
- **Read Your Writes**: Reads always see your previous writes
- **Writes Follow Reads**: Writes based on read values maintain causality

### 10.2 Time Travel

**Capabilities:**
- Restore database to any minute in last 30 days
- Query historical data
- Built-in disaster recovery

**Use Cases:**
- Accidental data deletion recovery
- Compliance and audit trails
- Testing and debugging
- Historical data analysis

**Note**: Time Travel is automatic; specific API details not fully documented in public sources.

### 10.3 Metrics and Analytics

D1 provides observability through Cloudflare dashboard:

**Available Metrics:**
- Query count
- Rows read
- Rows written
- Query duration
- Query efficiency (rows returned / rows read)
- Error rates

**Accessing Metrics:**
- Cloudflare Dashboard: Analytics section
- GraphQL Analytics API
- Workers Analytics Engine integration

---

## 11. Migration Strategies

### 11.1 From SQLite

```bash
# Export existing SQLite database to SQL
sqlite3 existing.db .dump > export.sql

# Import to D1
wrangler d1 execute my-database --file=export.sql --remote
```

**Considerations:**
- Remove incompatible SQLite features
- Test queries for compatibility
- Adjust for 10GB size limit

### 11.2 From Other Databases

**General Process:**
1. Export schema and data to SQL format
2. Convert SQL to SQLite dialect
3. Split large datasets if needed
4. Import using D1 execute

**Tools:**
- Database-specific export utilities
- SQL conversion tools
- Custom migration scripts

### 11.3 Alpha to Production Backend

D1's production backend (GA) is faster than original alpha:

```bash
# Export from alpha database
wrangler d1 export old-database --remote --output=backup.sql

# Create new production database
wrangler d1 create new-database

# Import data
wrangler d1 execute new-database --file=backup.sql --remote

# Update wrangler.toml with new database_id
```

### 11.4 Foreign Key Handling

```sql
-- Temporarily disable FK checks during import
PRAGMA defer_foreign_keys = true;

-- Import data
-- ... INSERT statements ...

-- Re-enable checks
PRAGMA defer_foreign_keys = false;
```

---

## 12. Pricing Details (2024-2025)

### Free Tier (Workers Free Plan)

**Included:**
- 500 MB per database
- 10 databases per account
- 5 million rows read per day
- 100,000 rows written per day
- 5 GB total storage

**Limits:**
- Daily limits reset at 00:00 UTC
- API returns errors when limits exceeded
- Sufficient for prototyping and small projects

### Paid Tier (Workers Paid Plan - $5/month)

**Base Subscription: $5/month**

**Included:**
- 10 GB per database (20x increase)
- 50,000 databases per account (5000x increase)
- 1 TB total storage per account
- No daily limits

**Pay-per-use Pricing:**
- Rows read: Charged per million rows scanned
- Rows written: Charged per million rows modified
- Storage: Charged per GB per month

**No Charges For:**
- Data transfer (egress)
- Bandwidth
- Compute (when not querying)
- Read replicas

### Cost Optimization

**Strategies:**
1. **Index effectively**: Reduce rows scanned
2. **Use raw() method**: Better performance
3. **Batch operations**: Reduce API calls
4. **Cursor pagination**: Avoid COUNT queries
5. **Read replication**: Distribute read load

---

## 13. Security Considerations

### 13.1 Data Security

**In Transit:**
- TLS/SSL encryption (Workers ↔ D1)
- HTTPS for REST API
- Secure by default

**At Rest:**
- Managed by Cloudflare infrastructure
- Automatic backups via Time Travel
- No direct database file access

### 13.2 Access Control

**No Built-in Authentication:**
D1 itself has no authentication layer. Access control must be implemented in application layer.

**Recommended Patterns:**

**1. Worker as API Gateway:**
```javascript
async function requireAuth(request, env) {
  const token = request.headers.get('Authorization')?.replace('Bearer ', '');

  if (!token) {
    return new Response('Unauthorized', { status: 401 });
  }

  // Validate token against database or KV
  const session = await env.DB.prepare(
    "SELECT * FROM sessions WHERE token = ? AND expires_at > ?"
  ).bind(token, Date.now()).first();

  if (!session) {
    return new Response('Invalid token', { status: 401 });
  }

  return session.user_id;
}

export default {
  async fetch(request, env) {
    const userId = await requireAuth(request, env);
    if (userId instanceof Response) return userId;

    // Proceed with authorized request
    const data = await env.DB.prepare(
      "SELECT * FROM user_data WHERE user_id = ?"
    ).bind(userId).all();

    return new Response(JSON.stringify(data.results));
  }
};
```

**2. API Key Management:**
```javascript
// Store API keys as Worker secrets
export default {
  async fetch(request, env) {
    const apiKey = request.headers.get('X-API-Key');

    if (apiKey !== env.API_SECRET) {
      return new Response('Forbidden', { status: 403 });
    }

    // Process request
  }
};
```

### 13.3 SQL Injection Prevention

**Always use prepared statements:**
```javascript
// NEVER do this
const unsafe = await DB.prepare(
  `SELECT * FROM users WHERE email = '${userInput}'`
).all();

// ALWAYS do this
const safe = await DB.prepare(
  "SELECT * FROM users WHERE email = ?"
).bind(userInput).all();
```

---

## 14. Performance Characteristics

### 14.1 Latency

**Factors:**
- Worker location vs D1 primary database location
- Query complexity
- Index usage
- Network conditions

**Typical Ranges:**
- Simple indexed query: 10-50ms
- Complex query without index: 100-500ms+
- With read replication: 5-20ms (queries routed to nearest replica)

### 14.2 Throughput

**Read Throughput:**
- Scales with read replicas (beta feature)
- Limited by query complexity and database size

**Write Throughput:**
- All writes go to primary database
- Limited by SQLite's single-writer model
- Batch operations significantly improve performance

### 14.3 Performance Optimization Checklist

1. ✓ Create indexes on frequently queried columns
2. ✓ Run `PRAGMA optimize` after creating indexes
3. ✓ Use `EXPLAIN QUERY PLAN` to verify index usage
4. ✓ Monitor `queryEfficiency` metric (target ~1.0)
5. ✓ Use batch operations for multiple queries
6. ✓ Implement cursor-based pagination
7. ✓ Avoid COUNT(*) queries
8. ✓ Use `.raw()` for performance-critical queries
9. ✓ Enable read replication for read-heavy workloads
10. ✓ Choose appropriate database location hint

---

## 15. Comparison with Alternatives

### D1 vs Traditional Databases

| Feature | D1 | PostgreSQL/MySQL |
|---------|----|--------------------|
| **Hosting** | Serverless, managed | Self-hosted or managed |
| **Scaling** | Automatic, horizontal | Manual, typically vertical |
| **Pricing** | Usage-based | Instance-based |
| **Setup** | Instant | Requires provisioning |
| **Maintenance** | Zero | Regular updates needed |
| **Size Limit** | 10 GB per database | TBs+ |
| **Global Distribution** | Built-in | Requires setup |

### D1 vs Other Serverless Databases

| Feature | D1 | PlanetScale | Supabase | Turso |
|---------|----|-----------|-----------| ------|
| **SQL Dialect** | SQLite | MySQL | PostgreSQL | SQLite |
| **Free Tier** | 5M reads/day | 1B row reads/mo | 500 MB | 9 GB total |
| **Edge Deployment** | Yes | No | Partial | Yes |
| **Workers Integration** | Native | Via HTTP | Via HTTP | Via libSQL |
| **Max DB Size** | 10 GB | Varies | Varies | Unlimited |

### When to Choose D1

**Best For:**
- Cloudflare Workers/Pages applications
- Multi-tenant architectures
- Edge-first applications
- Applications needing global distribution
- Budget-conscious projects (generous free tier)
- SQLite compatibility requirements

**Not Ideal For:**
- Databases > 10 GB
- High write throughput requirements
- Complex PostgreSQL-specific features
- Applications not on Cloudflare platform

---

## 16. Future Roadmap and Updates

### Recent Additions (2024-2025)

- ✅ General Availability (April 2024)
- ✅ 10 GB per-database limit (up from 2 GB)
- ✅ 50,000 databases per account
- ✅ Global read replication (Beta - April 2025)
- ✅ Sessions API for consistency
- ✅ Prisma ORM support (v5.12.0)
- ✅ Enhanced export/import capabilities

### Current Beta Features

- **Global Read Replication**: Sequential consistency with automatic replica routing

### Community Requests

Based on community forums and GitHub discussions:
- Named parameters support
- Increased per-database size limits
- Additional SQLite extensions
- More granular observability
- Improved local development experience

---

## 17. Resources and Documentation

### Official Documentation

- **Primary Docs**: https://developers.cloudflare.com/d1/
- **Getting Started**: https://developers.cloudflare.com/d1/get-started/
- **Workers Binding API**: https://developers.cloudflare.com/d1/worker-api/
- **REST API Reference**: https://developers.cloudflare.com/api/resources/d1/
- **Best Practices**: https://developers.cloudflare.com/d1/best-practices/
- **Pricing**: https://developers.cloudflare.com/d1/platform/pricing/
- **Limits**: https://developers.cloudflare.com/d1/platform/limits/
- **Release Notes**: https://developers.cloudflare.com/d1/platform/release-notes/

### Blog Posts

- **Announcing D1**: https://blog.cloudflare.com/introducing-d1/
- **D1 GA Announcement**: https://blog.cloudflare.com/making-full-stack-easier-d1-ga-hyperdrive-queues/
- **Building D1**: https://blog.cloudflare.com/building-d1-a-global-database/
- **Read Replication**: https://blog.cloudflare.com/d1-read-replication-beta/
- **D1 to 11**: https://blog.cloudflare.com/d1-turning-it-up-to-11/

### Tutorials

- **Build an API**: https://developers.cloudflare.com/d1/tutorials/build-an-api-to-access-d1/
- **Comments API**: https://developers.cloudflare.com/d1/tutorials/build-a-comments-api/
- **Bulk Import**: https://developers.cloudflare.com/d1/tutorials/import-to-d1-with-rest-api/

### Community

- **Cloudflare Community Forums**: https://community.cloudflare.com/c/developers/d1/
- **Discord**: Cloudflare Developers server
- **GitHub Issues**: workers-sdk repository

### ORM Documentation

- **Drizzle**: https://orm.drizzle.team/docs/connect-cloudflare-d1
- **Prisma**: https://www.prisma.io/docs/guides/cloudflare-d1

---

## 18. Summary and Key Takeaways

### What D1 Is

Cloudflare D1 is a serverless SQL database built on SQLite that runs on Cloudflare's global network. It's designed for applications that benefit from:
- **Edge deployment**: Run queries close to users globally
- **Horizontal scaling**: Many small databases instead of one large database
- **Zero maintenance**: Fully managed with automatic backups
- **Native Workers integration**: Seamless binding system
- **Cost-effective**: Generous free tier and usage-based pricing

### What Makes D1 Unique

1. **Global by default**: Automatic replication and distribution
2. **SQLite compatibility**: Familiar SQL with proven stability
3. **Time Travel**: 30-day point-in-time recovery built-in
4. **Read replication**: Sequential consistency across global replicas
5. **Multi-database architecture**: Thousands of databases at same cost
6. **Scale-to-zero**: Pay only for actual queries, not idle time

### Critical Design Considerations

**D1 is optimized for:**
- Applications on Cloudflare Workers/Pages
- Multi-tenant SaaS architectures
- Per-user data isolation
- Read-heavy workloads (with replication)
- Edge-first applications
- Databases under 10 GB

**D1 is NOT optimized for:**
- Large monolithic databases (>10 GB)
- Extremely high write throughput
- Complex PostgreSQL/MySQL-specific features
- Applications requiring analytical queries
- Non-Cloudflare deployments

### Getting Started Checklist

1. ✓ Install Wrangler CLI: `npm install -g wrangler`
2. ✓ Authenticate: `wrangler login`
3. ✓ Create database: `wrangler d1 create my-database`
4. ✓ Add binding to wrangler.toml
5. ✓ Create schema migration
6. ✓ Apply migration locally: `wrangler d1 migrations apply my-database --local`
7. ✓ Test with `wrangler dev`
8. ✓ Deploy: `wrangler deploy`
9. ✓ Apply migration to production: `wrangler d1 migrations apply my-database --remote`

### Best Practices Recap

1. **Always use prepared statements** to prevent SQL injection
2. **Create indexes** on frequently queried columns
3. **Use PRAGMA optimize** after index creation
4. **Monitor queryEfficiency** metric (target ~1.0)
5. **Batch operations** for better performance
6. **Cursor-based pagination** instead of offset
7. **Avoid COUNT(*)** queries for better performance
8. **Use STRICT tables** for type safety
9. **Implement access control** in Worker layer
10. **Enable read replication** for read-heavy workloads

---

## Conclusion

Cloudflare D1 represents a significant advancement in serverless database technology, bringing SQL databases to the edge with zero configuration and global distribution. Its SQLite foundation provides familiar, battle-tested SQL semantics, while Cloudflare's infrastructure adds automatic replication, Time Travel backups, and seamless Workers integration.

The service is production-ready (GA since April 2024) and suitable for a wide range of applications, particularly those benefiting from multi-tenant architectures, edge deployment, and cost-effective scaling. With ongoing development including global read replication and expanding ORM support, D1 continues to mature as a compelling option for serverless SQL databases on the edge.

For developers building on Cloudflare's platform, D1 offers a natural, low-friction path to adding persistent SQL storage with minimal operational overhead and excellent performance characteristics.

---

**Report Compiled:** November 17, 2025
**Sources:** Official Cloudflare documentation, blog posts, community resources, and public announcements
**Latest Features:** Global Read Replication (Beta as of April 2025)
