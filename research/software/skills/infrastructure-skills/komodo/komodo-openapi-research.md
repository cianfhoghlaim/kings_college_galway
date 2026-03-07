# Komodo (komo.do) OpenAPI Research Report

**Date:** 2025-11-22
**Researcher:** Claude
**Subject:** OpenAPI/Swagger Specification for Komodo (komo.do)

---

## Executive Summary

**Does an official OpenAPI spec exist?** **NO**

Komodo (komo.do) does **not** provide an official OpenAPI/Swagger specification. Instead, the project uses a **Rust-first, code-generation approach** with [Typeshare](https://github.com/1Password/typeshare) to generate TypeScript types directly from Rust source code. This eliminates the need for maintaining separate API specification files.

---

## Project Overview

### What is Komodo?

**Komodo** is an open-source infrastructure management platform that provides a centralized web interface for managing:
- Server health monitoring and shell connections
- Docker container deployments and logging
- Docker Compose stacks with automatic updates
- Docker image builds with version control
- Repository automation and scripting
- Configuration and environment variable management
- Audit logging

### Key Information

- **Official Website:** https://komo.do
- **Documentation:** https://komo.do/docs/intro
- **GitHub Repository:** https://github.com/moghtech/komodo
- **Demo Instance:** https://demo.komo.do
- **Build Server:** https://build.komo.do
- **Discord Community:** https://discord.gg/DRqE8Fvg5Nc
- **License:** GPL-3.0-or-later
- **Languages:** Rust (57.5%), TypeScript (39.6%), JavaScript (1.7%)

---

## API Architecture

### API Structure

Komodo Core exposes an **RPC-like HTTP API** with JSON serialization. The API is organized into functional categories:

1. **`/auth`** - Authentication and token management
2. **`/user`** - Self-management operations (API keys, etc.)
3. **`/read`** - Read-only data retrieval requests
4. **`/write`** - Create, update, delete operations
5. **`/execute`** - Action execution on resources (e.g., `RunBuild`, `DeployStack`)
6. **`/terminal`** - Terminal-related functionality

### Request Format

All API requests use HTTP POST with a standard JSON format:

```json
{
  "type": "EndpointName",
  "params": {
    /* endpoint-specific parameters */
  }
}
```

### Authentication

Two authentication methods are supported:

1. **JWT Token:** `Authorization` header
2. **API Key/Secret:** `X-Api-Key` and `X-Api-Secret` headers

### Response Format

Errors return JSON with:
- Error message
- Traceback array for debugging

---

## Available Client Libraries

### Rust Client

- **Package:** `komodo_client` on [crates.io](https://crates.io/crates/komodo_client)
- **Documentation:** https://docs.rs/komodo_client/latest/komodo_client/
- **Version:** 1.19.5
- **Coverage:** ~63.76% documented

**Example:**
```rust
use komodo_client::{KomodoClient, entities::*};

let client = KomodoClient::new("https://demo.komo.do")
    .with_api_key("your_key", "your_secret");

let stacks = client.read().list_stacks(ListStacksRequest {}).await?;
let update = client.execute().deploy_stack(DeployStackRequest {
    stack: stacks[0].name.clone()
}).await?;
```

### TypeScript Client

- **Package:** `komodo_client` on [NPM](https://www.npmjs.com/package/komodo_client)
- **Type-safe:** Full TypeScript type inference

**Example:**
```typescript
import { KomodoClient, Types } from "komodo_client";

const komodo = KomodoClient("https://demo.komo.do", {
  type: "api-key",
  params: {
    key: "your_key",
    secret: "your_secret",
  },
});

// Inferred as Types.StackListItem[]
const stacks = await komodo.read("ListStacks", {});

// Inferred as Types.Update
const update = await komodo.execute("DeployStack", {
  stack: stacks[0].name,
});
```

---

## Type Generation System

### Typeshare-Based Generation

Komodo uses **[Typeshare](https://github.com/1Password/typeshare)** to generate TypeScript types from Rust:

**Generation Command:**
```bash
RUST_BACKTRACE=1 typeshare . --lang=typescript --output-file=./client/core/ts/src/types.ts
```

**Generation Script:** `/client/core/ts/generate_types.mjs`

### Post-Processing Transformations

The generation script applies regex-based transformations:
- Converts Rust enum variants to TypeScript discriminated unions
- Maps Rust collections to TypeScript equivalents:
  - `IndexSet` → `Array`
  - `IndexMap` → `Record`
- Adds union types for permission configurations

### Design Philosophy

**Rust as the Source of Truth:**
- Backend Rust types are the canonical API definition
- TypeScript types are automatically generated
- Eliminates synchronization overhead between specification and implementation
- No separate OpenAPI specification to maintain

---

## API Entities

Based on the documentation at [docs.rs/komodo_client/entities](https://docs.rs/komodo_client/latest/komodo_client/entities/), the API includes:

### Core Resource Types
- **Actions** - Automated action definitions
- **Alerts** - Alert configurations
- **Builds** - Docker image build configurations
- **Deployments** - Container deployment definitions
- **Procedures** - Scripted automation procedures
- **Repos** - Git repository configurations
- **Servers** - Connected server definitions
- **Stacks** - Docker Compose stack configurations
- **Resources** - Generic resource abstractions
- **Schedules** - Scheduling configurations
- **Tags** - Resource tagging system
- **Updates** - Update/operation status tracking
- **Users** - User accounts
- **User Groups** - Permission groups
- **Variables** - Environment variable management

### Infrastructure Entities
- Docker networks, images, containers
- Git providers and Docker registries
- API keys and permissions
- Core and periphery configuration
- TOML-based resource definitions

### Supporting Types
- Maintenance windows and schedules
- Environment variables and file contents
- System commands and execution arguments
- Log validation utilities
- Timestamp generation (Unix milliseconds)
- Naming conventions (Docker-compatible, path-compatible)

---

## Technical Stack

### Backend (Rust)
- **Web Framework:** [Axum](https://github.com/tokio-rs/axum) (HTTP/WebSocket server)
- **Async Runtime:** [Tokio](https://tokio.rs/)
- **Serialization:** [Serde](https://serde.rs/) (JSON)
- **Type Sharing:** [Typeshare](https://github.com/1Password/typeshare)

### Frontend (TypeScript)
- **Generated Client:** From Rust types via Typeshare
- **Hooks:** Custom React hooks wrapping `komodo_client` API in `lib/hooks`

### Two-Component Architecture

1. **Core Service**
   - Web server hosting API and browser UI
   - Central management point
   - All user interactions flow through Core

2. **Periphery Service**
   - Lightweight HTTP API server on target machines
   - Executes Docker operations
   - Monitors system resources
   - Provides Git operations and terminal access
   - Security: IP filtering and passkey authentication

---

## Why No OpenAPI Specification?

### Rust Tooling Ecosystem

While Rust has OpenAPI generation tools available:
- [**utoipa**](https://github.com/juhaku/utoipa) - Code-first OpenAPI generation with macros
- [**aide**](https://github.com/tamasfe/aide) - OpenAPI code generation for Axum
- [**oasgen**](https://github.com/kurtbuilds/oasgen) - Generates OpenAPI 3.0 from Rust code

**Komodo chose not to use these tools.**

### Design Trade-offs

**Benefits of Typeshare Approach:**
- ✅ Guaranteed type safety between Rust and TypeScript
- ✅ Single source of truth (Rust code)
- ✅ No specification drift or synchronization issues
- ✅ Fast development iteration
- ✅ Automatic client generation on every build

**Drawbacks:**
- ❌ No OpenAPI specification for third-party tools
- ❌ No Swagger UI for interactive API exploration
- ❌ Limited discoverability for external developers
- ❌ No standard API documentation format
- ❌ Harder to generate clients in other languages

---

## Verification Tests

### Direct API Endpoint Check

```bash
curl -s -I "https://demo.komo.do/openapi.json"
# Result: HTTP/2 404
```

**Conclusion:** No OpenAPI spec served at standard endpoints (`/openapi.json`, `/swagger.json`, `/api-docs`)

### Repository Search

Searched the GitHub repository for:
- ✗ `openapi.json` or `openapi.yaml`
- ✗ `swagger.json` or `swagger.yaml`
- ✗ `utoipa` crate usage
- ✗ `aide` crate usage
- ✗ Swagger UI integration

**Conclusion:** No OpenAPI generation infrastructure present in codebase

---

## API Capabilities Summary

Based on the available documentation and client libraries, Komodo's API supports:

### Server Management
- List, create, update, delete servers
- Monitor server health
- Execute shell commands
- View system metrics

### Docker Operations
- Manage containers (start, stop, restart, remove)
- View container logs
- Manage images (build, pull, remove)
- Manage networks and volumes

### Deployment Management
- Deploy and manage Docker containers
- Configure environment variables
- Manage container configurations
- Track deployment status

### Stack Management
- Deploy Docker Compose stacks
- Manage stack configurations
- Automatic updates from Git repositories
- UI-defined or Git-backed stacks

### Build Management
- Build Docker images
- Version control for builds
- Webhook-triggered builds
- Build history and logs

### Repository Automation
- Execute scripts from Git repositories
- Manage repository configurations
- Automated procedures
- Git operations

### Configuration Management
- Centralized environment variables
- Secret interpolation
- TOML-based resource definitions
- Variable import/export

### User & Permission Management
- User accounts and groups
- API key management
- Granular permission system
- OAuth authentication support

### Monitoring & Logging
- Audit logs for all actions
- Container logs
- Build logs
- System metrics

---

## Generating an OpenAPI Spec

### Is It Feasible?

**YES**, but it would require significant effort:

### Option 1: Manual Creation
- Review Rust source code in `/client/core/rs/src/api/`
- Document each endpoint manually
- Create OpenAPI 3.0 YAML/JSON specification
- Maintain specification alongside code changes

**Effort:** High
**Maintenance:** Manual synchronization required

### Option 2: Add utoipa to Codebase
- Add `utoipa` and `utoipa-axum` crates
- Annotate API handlers with `#[utoipa::path]` macros
- Annotate entity structs with `#[utoipa::ToSchema]` derives
- Configure OpenAPI specification generation
- Optionally add Swagger UI integration

**Effort:** Medium-High
**Maintenance:** Automatic if properly integrated
**Tradeoff:** Additional code annotations required

### Option 3: Reverse Engineering
- Capture API requests/responses from clients
- Use network inspection tools
- Document observed endpoints
- Generate approximate specification

**Effort:** Medium
**Accuracy:** Limited to observed endpoints

### Recommended Approach

**For external developers:**
1. Use the official TypeScript or Rust clients
2. Refer to `docs.rs` documentation for entity types
3. Review example code in documentation

**For OpenAPI requirement:**
1. Contact maintainers via GitHub or Discord
2. Request official OpenAPI support
3. Or contribute an implementation using `utoipa`

---

## References & Sources

### Official Documentation
- [Komodo Home](https://komo.do/)
- [Getting Started](https://komo.do/docs/intro)
- [API and Clients](https://komo.do/docs/ecosystem/api)
- [Resources Documentation](https://komo.do/docs/resources)

### GitHub
- [moghtech/komodo Repository](https://github.com/moghtech/komodo)
- [Periphery Service Documentation](https://deepwiki.com/moghtech/komodo/2.2-periphery-service)
- [Dependencies and Build System](https://deepwiki.com/moghtech/komodo/10-dependencies-and-build-system)
- [Frontend Documentation](https://deepwiki.com/moghtech/komodo/9-deployment-options)

### Package Documentation
- [komodo_client (Rust)](https://docs.rs/komodo_client/latest/komodo_client/)
- [komodo_client API](https://docs.rs/komodo_client/latest/komodo_client/api/index.html)
- [komodo_client Entities](https://docs.rs/komodo_client/latest/komodo_client/entities/)
- [komodo_client on crates.io](https://crates.io/crates/komodo_client)

### Tools & Technologies
- [Typeshare (1Password)](https://github.com/1Password/typeshare)
- [Axum Web Framework](https://github.com/tokio-rs/axum)
- [utoipa - OpenAPI for Rust](https://github.com/juhaku/utoipa)
- [aide - Axum OpenAPI](https://docs.rs/aide/latest/aide/)

### Community
- [Discord Community](https://discord.gg/DRqE8Fvg5Nc)
- [GitHub Issues](https://github.com/moghtech/komodo/issues)
- [GitHub Discussions](https://github.com/moghtech/komodo/discussions)

---

## Conclusion

Komodo (komo.do) does **not** provide an official OpenAPI specification. The project deliberately chose a **Rust-first development approach** using Typeshare for type generation rather than maintaining a separate API specification.

### For Integration:
- **Best Option:** Use the official `komodo_client` library (Rust or TypeScript)
- **Alternative:** Make direct HTTP POST requests to documented endpoints
- **Documentation:** Refer to [docs.rs/komodo_client](https://docs.rs/komodo_client/latest/komodo_client/)

### For OpenAPI Spec:
- **Official Spec:** Does not exist
- **Feasibility:** Possible to generate manually or contribute `utoipa` integration
- **Recommendation:** Contact maintainers or contribute to the open-source project

---

**Research completed:** 2025-11-22
**Last verified:** Komodo v1.19.5
