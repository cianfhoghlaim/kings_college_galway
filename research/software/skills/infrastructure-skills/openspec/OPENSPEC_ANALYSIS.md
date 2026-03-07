# OpenSpec Analysis Report: Comprehensive Structure and Guidelines

## Document Created
2025-11-28

## Executive Summary

OpenSpec is a **specification-driven development system** that separates:
- **Current specifications** (`specs/`) - what IS built
- **Change proposals** (`changes/`) - what SHOULD be built
- **Archived changes** (`changes/archive/`) - what WAS implemented

The system uses a three-stage workflow: Create → Implement → Archive, with strict validation and format requirements for specs and proposals.

---

# Part 1: Complete Project Specification

## Project.md: Platform Architecture & Conventions

### Purpose
Modern, AI-native data platform and SaaS application unifying:
- Real-time data pipelines for GitHub analytics and web intelligence
- AI-powered knowledge systems with semantic search and graph databases
- Full-stack web application for user authentication, billing, and real-time collaboration
- Zero-trust infrastructure with automated deployment and secrets management

### Tech Stack Overview

**Data & Analytics Layer:**
- Lakehouse: DuckLake, Apache Iceberg (Lakekeeper), LakeFS, OLake
- Streaming: RisingWave, Ibis
- Ingestion: DLT (Data Load Tool), Crawl4AI (self-hosted web scraping), CocoIndex
- Storage: Cloudflare R2, PostgreSQL, Memgraph, LanceDB
- Orchestration: Dagster
- Transformation: SQLMesh, CocoIndex
- Query Engines: DuckDB, Trino

**AI/ML Layer:**
- Vector Database: LanceDB
- Knowledge Graph: Memgraph, Cognee
- LLM Gateway: LiteLLM (Gemini 2.5 Pro, OpenAI, Claude)
- Agent Framework: Agno AgentOS with BAML
- Schema Modeling: BAML → Pydantic (Python) + Zod (TypeScript)

**Web Application Layer:**
- Frontend: TanStack Start (React SSR + file-based routing)
- API Framework: Hono (multi-runtime: Cloudflare Workers, Node.js, Netlify)
- Real-time Backend: Convex (WebSocket-based state sync)
- Authentication: BetterAuth (OIDC provider, self-hosted)
- Database: Supabase PostgreSQL
- Billing: Polar.sh (Merchant of Record)
- GraphQL: Neo4j GraphQL Library (Memgraph), Hasura (DuckDB)

**Infrastructure & DevOps:**
- Deployment: Komodo (GitOps orchestration)
- Networking: Pangolin (WireGuard VPN, zero-trust access)
- CI/CD: Dagger (containerized pipelines)
- IaC: Pulumi (TypeScript/Python)
- Secrets: 1Password (CLI + Connect + SDK), SOPS with Age encryption
- Configuration: Ansible
- Containers: Docker + Docker Compose
- Cache: DragonflyDB (Redis-compatible)

### Code Style Conventions
- **TypeScript**: Strict mode, ESLint + Prettier
- **Python**: Type hints required, Black formatter, Ruff linter
- **Naming**: kebab-case for files/directories, camelCase for TS/JS, snake_case for Python
- **Imports**: Absolute paths with `@/` prefix for internal modules

### Architecture Patterns

#### 1. Separation of Concerns
Data Ingestion → Storage → Transformation → Query Layer → Application

#### 2. Type Safety End-to-End
- BAML as single source of truth for schemas
- Code generation: BAML → Pydantic (Python) + Zod (TypeScript)
- OpenAPI spec → TypeScript client generation
- Compile-time validation + runtime validation (Zod middleware)

#### 3. Hybrid Real-Time Model
- **Ephemeral Streaming**: TanStack Start server functions (AI token streams)
- **Persistent Sync**: Convex WebSocket (chat history, metrics, presence)
- Decision: Continuous generation → Stream; Discrete events → Convex

#### 4. Incremental Processing
- Only process changed data (DLT incremental loading, CocoIndex LISTEN/NOTIFY)
- Postgres triggers for real-time index updates
- Git-aware reindexing (sparse checkout for targeted updates)

#### 5. Multi-Tenancy
- Organization-based data isolation (`orgId` filter on all queries)
- Row-level security in PostgreSQL
- GraphQL authorization directives
- Separate Polar subscriptions per organization

#### 6. Zero-Trust Networking
- All internal services behind Pangolin VPN (Newt/Olm agents)
- Public endpoints exposed via Pangolin domains with automatic HTTPS
- No direct port exposure, no IP allowlists

### Important Constraints

**Performance:**
- Incremental Updates: Process only changed data (no full re-indexing)
- Caching: Aggressive use of DuckDB cache, Redis/Dragonfly for hot data
- Indexing: Pre-build vector indexes (IVF/HNSW), Iceberg partition pruning

**Security:**
- No Hardcoded Secrets: All credentials in 1Password
- Encryption at Rest: SOPS for Git, encrypted Postgres backups
- Encryption in Transit: TLS everywhere (Pangolin automatic HTTPS)
- Input Validation: Zod schemas on all external inputs
- SQL Safety: Parameterized queries only
- OWASP Top 10: Prevent XSS, SQL injection, CSRF, authentication bypass

**Data Sovereignty:**
- Self-Hosted Services: BetterAuth, Convex, LanceDB, Memgraph, RisingWave
- Cloud Minimization: Only use Cloudflare (R2, D1) and LiteLLM gateway
- Export Capability: All data exportable as Parquet/CSV

---

# Part 2: Existing Specifications

## Current Capabilities (9 specs)

### 1. Data Ingestion (`specs/data-ingestion/spec.md`)
**Focus**: Declarative, incremental data loading from diverse sources

**Key Requirements:**
- Batch Data Loading (DLT with automatic schema inference)
- Web Content Extraction (Firecrawl with JS rendering)
- Incremental AI Indexing (CocoIndex with Postgres LISTEN/NOTIFY)
- Database Change Data Capture (OLake with low-latency CDC)
- Pipeline State Management (resumable, idempotent workflows)
- Data Quality Validation (Pydantic models from BAML)

**Integration Points:**
- Dagster Orchestration
- Storage Sinks (PostgreSQL, Iceberg, LanceDB, Memgraph)
- Schema Registry (BAML, Pydantic)

---

### 2. Orchestration (`specs/orchestration/spec.md`)
**Focus**: Software-defined data pipeline management using Dagster

**Key Requirements:**
- Asset-Based Pipeline Definition (declarative, automatic dependency resolution)
- Event-Driven Execution (sensors for files, webhooks, schedules)
- Incremental Pipeline Execution (stale detection, upstream propagation)
- Resource Management (database pooling, secret injection)
- Observability and Monitoring (structured logging, asset checks)

---

### 3. Lakehouse Storage
**Focus**: OLAP-optimized analytical storage with Iceberg, DuckLake, LakeFS

---

### 4. Streaming Analytics
**Focus**: Real-time stream processing with RisingWave, Ibis

---

### 5. Realtime Data
**Focus**: WebSocket-based state sync with Convex

---

### 6. User Authentication
**Focus**: BetterAuth OIDC provider, JWT validation, multi-tenancy

---

### 7. Secrets Management
**Focus**: 1Password integration, SOPS encryption, secret injection

---

### 8. CI/CD Pipeline
**Focus**: Dagger-based containerized pipeline automation

---

### 9. Deployment Orchestration
**Focus**: Komodo GitOps, Pangolin zero-trust networking

---

# Part 3: Existing Proposed Changes

## Active Changes (7 total)

### 1. enhance-github-pipeline-ingestion
**Status**: Proposed (not yet implemented)

**What it Adds:**
- GitHub Repository Discovery (organization + user-level access)
- Commit History Ingestion (with incremental loading)
- Issue and Pull Request Tracking (with comments, reviews)
- GitHub Rate Limit Management (exponential backoff)
- GitHub Webhook Integration (real-time event ingestion)
- Type-Safe GitHub Schemas (BAML-generated Pydantic)

**Affected Specs**: `data-ingestion`

**Structure:**
```
changes/enhance-github-pipeline-ingestion/
├── proposal.md      # Problem statement, impact analysis
├── tasks.md         # 6 implementation sections with subtasks
└── specs/
    └── data-ingestion/
        └── spec.md  # ADDED: 6 new requirements + MODIFIED: 1 existing
```

---

### 2. add-type-safe-rpc-api
**Status**: Proposed

**What it Adds:**
- New `web-api` capability specification
- oRPC contract definitions with Zod schemas
- Automatic OpenAPI spec generation
- Middleware system (auth, validation, logging, rate limiting)
- Streaming API support for long-running operations
- BAML → Zod schema synchronization

**Affected Specs**: New capability `web-api`

**Key Dependencies:**
- Integrates with `user-auth` (BetterAuth JWT validation)
- Uses BAML schemas from `ai-knowledge` and `data-ingestion`
- Connects to `realtime-data` (Convex)

---

### 3. add-code-indexing-capability
**Status**: Proposed

**What it Adds:**
- New `ai-knowledge` capability specification
- Tree-sitter code parsing across 20+ languages
- Vector embedding generation with LanceDB (IVF/HNSW indexes)
- Knowledge graph construction with Memgraph
- Hybrid search API (vector + keyword + graph)
- Incremental reindexing with Postgres triggers

**Affected Specs**: New capability `ai-knowledge`

**Key Dependencies:**
- Requires `data-ingestion` for Git repository loading
- Integrates with `orchestration` (Dagster)
- Uses `lakehouse-storage` for Parquet-based code file storage

---

### 4. refactor-shared-infrastructure
**Status**: Proposed
(Details not examined in detail)

---

### 5. enhance-gitops-cicd
**Status**: Proposed
(Details not examined in detail)

---

### 6. add-github-repo-pipeline-ui
**Status**: Proposed
(Details not examined in detail)

---

### 7. archive/ directory
**Purpose**: Completed changes that have been deployed and moved to archive

---

# Part 4: OpenSpec Format and Template

## Directory Structure

```
openspec/
├── AGENTS.md                         # AI assistant instructions
├── project.md                        # Project conventions and context
├── specs/                            # Current specifications
│   ├── [capability]/
│   │   ├── spec.md                  # Requirements and scenarios
│   │   └── design.md (optional)     # Technical patterns
│   ├── data-ingestion/
│   ├── orchestration/
│   ├── lakehouse-storage/
│   ├── user-auth/
│   └── [9 more capabilities]
│
└── changes/                          # Proposed changes
    ├── [change-id]/
    │   ├── proposal.md              # Why, what, impact
    │   ├── tasks.md                 # Implementation checklist
    │   ├── design.md (optional)     # Technical decisions
    │   └── specs/                   # Delta specifications
    │       ├── [capability]/
    │       │   └── spec.md          # ADDED/MODIFIED/REMOVED
    │       └── [another-capability]/
    │           └── spec.md
    │
    ├── enhance-github-pipeline-ingestion/
    ├── add-type-safe-rpc-api/
    ├── add-code-indexing-capability/
    ├── [3 more active changes]
    │
    └── archive/
        └── YYYY-MM-DD-[completed-changes]/
```

---

## Spec File Format

### Spec Header
```markdown
# Capability: [Name]

## Overview
[1-2 sentences on purpose]

## Requirements

### Requirement: [Name]
The system SHALL [requirement statement]

#### Scenario: [Scenario Name]
- **GIVEN** [precondition]
- **WHEN** [action]
- **THEN** [expected result]
- **AND** [additional assertion]
```

### Critical Format Rules

**1. Scenario Headers (MUST use #### exactly)**
```markdown
#### Scenario: User login success
- **WHEN** valid credentials provided
- **THEN** return JWT token
```

**WRONG formats:**
```markdown
- **Scenario: User login**     ❌
**Scenario**: User login        ❌
### Scenario: User login        ❌
```

**2. Scenario Content Format (MUST use bullets with WHEN/THEN)**
- Use `- **WHEN**` format (not bullet text)
- Use `- **THEN**` format (not bullet text)
- Use `- **AND**` for additional assertions
- Each requirement MUST have at least one scenario

**3. Requirement Wording**
- Use SHALL/MUST for normative requirements
- Use SHOULD/MAY only when intentionally non-normative

---

## Proposal File Format

```markdown
# Change: [Brief description]

## Why
[1-2 sentences on problem/opportunity]

## What Changes
- [Bullet list of changes]
- [Mark breaking changes with **BREAKING**]

## Impact
- Affected specs: [list capabilities]
- Affected code: [key files/systems]
- Breaking changes: [Yes/No and details]
- Dependencies: [what this depends on]
```

---

## Tasks File Format

```markdown
# Implementation Tasks

## 1. Phase Name
- [ ] 1.1 First task
- [ ] 1.2 Second task
- [ ] 1.3 Third task

## 2. Another Phase
- [ ] 2.1 Task
- [ ] 2.2 Task
```

---

## Design File (Optional)

Create only when:
- Cross-cutting change (multiple services/modules)
- New external dependency or significant data model changes
- Security, performance, or migration complexity
- Ambiguity benefits from technical decisions before coding

Template:
```markdown
## Context
[Background, constraints, stakeholders]

## Goals / Non-Goals
- Goals: [...]
- Non-Goals: [...]

## Decisions
- Decision: [What and why]
- Alternatives considered: [Options + rationale]

## Risks / Trade-offs
- [Risk] → Mitigation

## Migration Plan
[Steps, rollback]

## Open Questions
- [...]
```

---

## Delta Specification Format

Delta files go in `changes/[id]/specs/[capability]/spec.md`

### Delta Operations

```markdown
## ADDED Requirements
### Requirement: New Feature
The system SHALL provide...

#### Scenario: Success case
- **WHEN** user performs action
- **THEN** expected result

## MODIFIED Requirements
### Requirement: Existing Feature
[Complete modified requirement with all scenarios]

## REMOVED Requirements
### Requirement: Old Feature
**Reason**: [Why removing]
**Migration**: [How to handle]

## RENAMED Requirements
- FROM: `### Requirement: Login`
- TO: `### Requirement: User Authentication`
```

### Delta Operation Rules

**ADDED:**
- Introduces a new capability or sub-capability
- Can stand alone as a requirement
- Use when change is orthogonal

**MODIFIED:**
- Changes behavior, scope, or acceptance criteria of existing requirement
- MUST include full updated requirement (header + all scenarios)
- Archiver will replace entire requirement (partial deltas lose previous details)
- Common pitfall: Using MODIFIED to add new concern without including previous text

**REMOVED:**
- Deprecation notice
- Include reason and migration path

**RENAMED:**
- Used when only name changes
- If also changing behavior, use RENAMED + MODIFIED

---

# Part 5: Validation and Quality Assurance

## CLI Commands

```bash
# Essential
openspec list                              # List active changes
openspec list --specs                      # List specifications
openspec show [item]                       # Display change or spec
openspec validate [item]                   # Validate changes or specs
openspec archive <change-id> [--yes|-y]   # Archive after deployment

# Initialization
openspec init [path]                       # Initialize OpenSpec
openspec update [path]                     # Update instruction files

# Debugging
openspec show [change] --json --deltas-only
openspec validate [change] --strict
```

## Validation Checklist

Before sharing a proposal, run:
```bash
openspec validate <change-id> --strict
```

Common validation errors:
1. **"Change must have at least one delta"**
   - Check `changes/[name]/specs/` exists with .md files
   - Verify files have operation prefixes (## ADDED Requirements)

2. **"Requirement must have at least one scenario"**
   - Check scenarios use `#### Scenario:` format (exactly 4 hashtags)
   - Don't use bullet points or bold for scenario headers

3. **"Silent scenario parsing failures"**
   - Exact format required: `#### Scenario: Name`
   - Debug with: `openspec show [change] --json --deltas-only`

---

# Part 6: Implementation Workflow

## Stage 1: Creating Changes

**When to create:**
- Add features or functionality
- Make breaking changes (API, schema)
- Change architecture or patterns
- Optimize performance (changes behavior)
- Update security patterns

**Skip proposal for:**
- Bug fixes (restore intended behavior)
- Typos, formatting, comments
- Dependency updates (non-breaking)
- Configuration changes
- Tests for existing behavior

**Workflow:**
1. Review `openspec/project.md`, `openspec list`, and `openspec list --specs`
2. Choose unique verb-led `change-id` (kebab-case)
3. Scaffold: `proposal.md`, `tasks.md`, optional `design.md`, spec deltas
4. Write deltas using ADDED/MODIFIED/REMOVED with scenarios
5. Run `openspec validate <id> --strict`
6. Share proposal for review

## Stage 2: Implementing Changes

1. **Read proposal.md** - Understand what's being built
2. **Read design.md** (if exists) - Review technical decisions
3. **Read tasks.md** - Get implementation checklist
4. **Implement tasks sequentially** - Complete in order
5. **Confirm completion** - Ensure every item is finished
6. **Update checklist** - Set every task to `- [x]`
7. **Approval gate** - Don't start until proposal approved

## Stage 3: Archiving Changes

After deployment, create PR to:
```bash
# Move to archive with specs updates
openspec archive <change-id> [--skip-specs] [--yes]

# For tooling-only changes (no spec updates)
openspec archive <change-id> --skip-specs --yes
```

Then:
- Move `changes/[name]/` → `changes/archive/YYYY-MM-DD-[name]/`
- Update `specs/` if capabilities changed
- Run `openspec validate --strict` to confirm

---

# Part 7: Summary: What's Specified vs What Needs Proposals

## Fully Specified (Current Capabilities)

The following have complete specifications in `specs/`:

1. **Data Ingestion** ✓
   - Batch loading, web scraping, AI indexing, CDC, state management, data quality

2. **Orchestration** ✓
   - Asset definition, event-driven execution, incremental execution, resources, observability

3. **Lakehouse Storage** ✓
   - Iceberg, DuckLake, LakeFS integration

4. **Streaming Analytics** ✓
   - RisingWave, Ibis real-time processing

5. **Realtime Data** ✓
   - Convex WebSocket state sync

6. **User Authentication** ✓
   - BetterAuth OIDC, JWT validation, multi-tenancy

7. **Secrets Management** ✓
   - 1Password integration, SOPS encryption

8. **CI/CD Pipeline** ✓
   - Dagger containerized pipelines

9. **Deployment Orchestration** ✓
   - Komodo GitOps, Pangolin zero-trust

## Proposed But Not Yet Implemented (Needs Specification)

1. **Web API** (Proposed: add-type-safe-rpc-api)
   - oRPC contracts, Zod schemas, OpenAPI generation, middleware, streaming

2. **AI Knowledge Graph** (Proposed: add-code-indexing-capability)
   - Tree-sitter parsing, vector embeddings, Memgraph graph, hybrid search

3. **GitHub Ingestion Specifics** (Proposed: enhance-github-pipeline-ingestion)
   - GitHub API specialization, webhook handling, rate limiting

4. **Enhanced GitOps CI/CD** (Proposed: enhance-gitops-cicd)
   - CI/CD pipeline enhancements (details TBD)

5. **Infrastructure Refactoring** (Proposed: refactor-shared-infrastructure)
   - Shared infrastructure improvements (details TBD)

6. **GitHub UI** (Proposed: add-github-repo-pipeline-ui)
   - User interface for repo management (details TBD)

---

# Part 8: Key Takeaways for Integration Proposals

## Change ID Naming Convention
Use kebab-case, verb-led prefixes:
- `add-[capability]` - new features
- `update-[capability]` - enhancements
- `remove-[capability]` - deprecations
- `refactor-[capability]` - code reorganization

Example from codebase:
- `enhance-github-pipeline-ingestion`
- `add-type-safe-rpc-api`
- `add-code-indexing-capability`

## Multi-Capability Changes
When a change spans multiple capabilities, create separate spec delta files:

```
changes/add-2fa-notify/
├── proposal.md
├── tasks.md
└── specs/
    ├── auth/spec.md          # ADDED: Two-Factor Authentication
    └── notifications/spec.md # ADDED: OTP Email Notification
```

## Scenario Specification Standard

Every requirement must include at least one `#### Scenario:` with:
- **GIVEN**: Preconditions
- **WHEN**: The action/event
- **THEN**: Expected outcome
- **AND**: Additional assertions (optional, repeatable)

Example from data-ingestion spec:
```markdown
#### Scenario: API ingestion with incremental loading
- **GIVEN** a GitHub API source with authentication
- **WHEN** DLT pipeline executes with incremental cursor on `updated_at`
- **THEN** only new/modified records since last run SHALL be fetched
- **AND** pipeline state SHALL be persisted for next run
```

## Type Safety is Architectural Priority

The project uses:
- BAML as source of truth for schemas
- Automatic code generation (Pydantic + Zod)
- Zod runtime validation on API boundaries
- No `any` types in TypeScript

Proposals must account for this in spec requirements.

---

# Part 9: Real-World Proposal Example

### Example: enhance-github-pipeline-ingestion

**Files created:**
```
openspec/changes/enhance-github-pipeline-ingestion/
├── proposal.md (42 lines) - Problem + impact
├── tasks.md (41 lines) - 6 sections, ~20 subtasks
└── specs/
    └── data-ingestion/
        └── spec.md (161 lines) - Delta spec
```

**Delta spec contains:**
- 6 ADDED Requirements (GitHub Repository Discovery, Commit History, Issues/PRs, Rate Limits, Webhooks, Type-Safe Schemas)
- 1 MODIFIED Requirement (Batch Data Loading with GitHub-specific error handling)
- Total: 7 requirements with 20+ scenarios

**Key characteristics:**
- Additive only (no breaking changes)
- Affects single capability (data-ingestion)
- References implementation paths (examples/, pipelines/)
- Clear dependencies (DLT, Dagster, BAML)

---

