# Web Research - Consolidated Index

This directory contains consolidated research for the web application layer of the hackathon platform.

## Directory Structure

```
web/consolidated/
├── 00-overview/
│   ├── architecture-patterns.md       # Web architecture patterns
│   ├── web-tech-tutorials-and-examples.md
│   └── INDEX.md
├── 01-frameworks/                     # Framework-specific patterns
└── 02-integrations/
    ├── dlt-baml-orpc-mcp-typesafe-pipeline-analysis.md
    ├── effect-convex-integration-research.md
    ├── effect-ts-tanstack-start-integration.md
    └── mcp-ui-gradio-evidence-integration-analysis.md
```

## Related Skills

Tool-specific documentation has been moved to `.claude/skills/`:

| Tool | Skill Location | Purpose |
|------|----------------|---------|
| TanStack Start | `.claude/skills/tanstack-start/` | Full-stack React framework |
| Convex | `.claude/skills/convex/` | Real-time backend |
| Effect-TS | `.claude/skills/effect-ts/` | Type-safe functional programming |
| Hono | `.claude/skills/hono/` | Lightweight web framework |
| oRPC | `.claude/skills/orpc/` | Type-safe RPC |
| Evidence | `.claude/skills/evidence/` | BI dashboards |

## Web Stack Architecture

The platform uses a modern TypeScript-first web stack:

1. **Frontend Framework** (TanStack Start)
   - File-based routing
   - Server functions
   - SSR & streaming

2. **Real-time Backend** (Convex)
   - Reactive data sync
   - Serverless functions
   - Built-in authentication

3. **Type Safety** (Effect-TS + oRPC)
   - Functional error handling
   - End-to-end type safety
   - Schema validation

4. **API Gateway** (Hono)
   - Lightweight HTTP server
   - Multi-runtime support
   - Middleware composition

## Quick Links

- **Architecture Patterns**: `00-overview/architecture-patterns.md`
- **Effect + TanStack Integration**: `02-integrations/effect-ts-tanstack-start-integration.md`
- **Effect + Convex Integration**: `02-integrations/effect-convex-integration-research.md`

## Archive

Original skill-specific research files are archived at:
`/research/archive/web-skills/`
