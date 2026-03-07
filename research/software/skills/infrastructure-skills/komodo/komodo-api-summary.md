# Komodo (komo.do) API Summary

**Quick Reference for Komodo Infrastructure Management Platform**

---

## OpenAPI Specification Status

🔴 **NO OFFICIAL OPENAPI SPEC EXISTS**

Komodo uses **Typeshare** (Rust → TypeScript type generation) instead of maintaining an OpenAPI specification.

---

## Essential Links

| Resource | URL |
|----------|-----|
| **Website** | https://komo.do |
| **Documentation** | https://komo.do/docs/intro |
| **API Docs** | https://komo.do/docs/ecosystem/api |
| **GitHub** | https://github.com/moghtech/komodo |
| **Demo** | https://demo.komo.do |
| **Discord** | https://discord.gg/DRqE8Fvg5Nc |
| **Rust Docs** | https://docs.rs/komodo_client/latest/komodo_client/ |
| **Crates.io** | https://crates.io/crates/komodo_client |
| **NPM** | https://www.npmjs.com/package/komodo_client |

---

## API Overview

### Base Endpoints

- `/auth` - Authentication & tokens
- `/user` - User self-management
- `/read` - Data retrieval (read-only)
- `/write` - Create/update/delete resources
- `/execute` - Execute actions on resources
- `/terminal` - Terminal operations

### Authentication

**Option 1: JWT Token**
```
Authorization: Bearer <token>
```

**Option 2: API Key/Secret**
```
X-Api-Key: <key>
X-Api-Secret: <secret>
```

### Request Format

```json
POST /read
Content-Type: application/json

{
  "type": "ListStacks",
  "params": {}
}
```

---

## Client Libraries

### TypeScript (Recommended)

```bash
npm install komodo_client
```

```typescript
import { KomodoClient, Types } from "komodo_client";

const komodo = KomodoClient("https://demo.komo.do", {
  type: "api-key",
  params: { key: "...", secret: "..." }
});

const stacks = await komodo.read("ListStacks", {});
const update = await komodo.execute("DeployStack", {
  stack: stacks[0].name
});
```

### Rust

```bash
cargo add komodo_client
```

```rust
use komodo_client::KomodoClient;

let client = KomodoClient::new("https://demo.komo.do")
    .with_api_key("key", "secret");
```

---

## Core Capabilities

### Infrastructure Management
- ✅ Server monitoring & shell access
- ✅ Docker container management
- ✅ Docker Compose stacks
- ✅ Docker image builds
- ✅ Environment variable management
- ✅ Git repository automation
- ✅ Audit logging

### Resource Types
- Servers, Deployments, Stacks
- Builds, Repos, Procedures
- Users, User Groups, API Keys
- Variables, Tags, Alerts
- Actions, Schedules, Updates

---

## Tech Stack

**Backend:** Rust + Axum + Tokio + Serde
**Frontend:** TypeScript + React
**Type Generation:** Typeshare (Rust → TypeScript)
**License:** GPL-3.0-or-later

---

## Why No OpenAPI?

**Design Choice:** Rust code is the source of truth
- TypeScript types auto-generated from Rust
- No specification drift
- Faster development iteration
- Type safety guaranteed

**Trade-off:** Less accessible for external developers

---

## Generating OpenAPI (If Needed)

### Option 1: Manual Creation
❌ High effort, manual maintenance

### Option 2: Add utoipa Integration
✅ Best long-term solution
- Add `utoipa` + `utoipa-axum` crates
- Annotate handlers with `#[utoipa::path]`
- Generate spec automatically
- Optional Swagger UI

### Option 3: Use Existing Clients
✅ **Recommended**
- Official TypeScript/Rust clients
- Full type safety
- Well-documented

---

## Key Endpoints (Examples)

### Read Operations
- `ListStacks` - Get all stacks
- `ListServers` - Get all servers
- `ListDeployments` - Get all deployments
- `GetStack` - Get stack details
- `GetServer` - Get server details

### Write Operations
- `CreateStack` - Create new stack
- `UpdateStack` - Update stack config
- `DeleteStack` - Remove stack
- `CreateDeployment` - Create deployment
- `UpdateDeployment` - Update deployment

### Execute Operations
- `DeployStack` - Deploy a stack
- `RunBuild` - Execute a build
- `RestartDeployment` - Restart container
- `RunProcedure` - Execute procedure
- `ExecuteAction` - Run an action

---

## Quick Start

1. **Install client:**
   ```bash
   npm install komodo_client
   ```

2. **Initialize:**
   ```typescript
   import { KomodoClient } from "komodo_client";
   const komodo = KomodoClient(API_URL, { /* auth */ });
   ```

3. **Use API:**
   ```typescript
   const data = await komodo.read("ListStacks", {});
   ```

---

## Getting Help

- 📖 [Full Documentation](https://komo.do/docs/intro)
- 💬 [Discord Community](https://discord.gg/DRqE8Fvg5Nc)
- 🐛 [GitHub Issues](https://github.com/moghtech/komodo/issues)
- 📚 [Rust API Reference](https://docs.rs/komodo_client/latest/komodo_client/)

---

**For complete research details, see:** [komodo-openapi-research.md](./komodo-openapi-research.md)

**Last Updated:** 2025-11-22 | **Version:** 1.19.5
