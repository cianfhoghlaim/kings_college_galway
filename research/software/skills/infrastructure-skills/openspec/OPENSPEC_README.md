# OpenSpec Documentation Index

This directory contains comprehensive analysis of the OpenSpec specification-driven development system used by the data-unified platform.

## Quick Start

1. **First time?** Read [EXPLORATION_SUMMARY.md](#explorationmd) (5 min read)
2. **Need templates?** Read [INTEGRATION_PROPOSAL_GUIDE.md](#integrationmd) (10 min read)
3. **Need deep reference?** Read [OPENSPEC_ANALYSIS.md](#openspecmd) (30 min read)

---

## Documents

### EXPLORATION_SUMMARY.md
**Purpose**: Executive summary and quick reference  
**Length**: 395 lines (12 KB)  
**Best for**: Getting oriented, understanding key patterns

**Contains:**
- Key findings about OpenSpec system
- 9 existing capabilities overview
- What's specified vs what needs proposals
- Integration path for example technologies
- Critical rules for spec writing
- Validation commands
- Next steps

**Start here if**: You need a quick overview of the system

---

### INTEGRATION_PROPOSAL_GUIDE.md
**Purpose**: How-to guide for creating integration proposals  
**Length**: 458 lines (16 KB)  
**Best for**: Creating proposals for new technologies (Gradio, Evidence.dev, MCP-UI)

**Contains:**
- Framework for mapping technologies to capabilities
- Proposal, tasks, and design file templates
- Complete Spec delta format with examples
- Validation checklist
- CLI commands reference
- Real Gradio integration walkthrough
- Key success criteria

**Start here if**: You're creating an integration proposal

---

### OPENSPEC_ANALYSIS.md
**Purpose**: Complete reference documentation  
**Length**: 731 lines (21 KB)  
**Best for**: Deep understanding, troubleshooting

**Contains (9 parts):**
1. Complete Project Specification (tech stack, conventions, constraints)
2. Existing Specifications (9 capabilities, each described)
3. Existing Proposed Changes (7 active proposals)
4. OpenSpec Format and Templates (directory structure, file formats)
5. Validation and Quality Assurance (CLI commands, error handling)
6. Implementation Workflow (create → implement → archive)
7. Summary: What's Specified vs What Needs Proposals
8. Key Takeaways for Integration Proposals
9. Real-World Proposal Example (enhance-github-pipeline-ingestion)

**Start here if**: You need comprehensive reference material

---

## The OpenSpec System

OpenSpec is a specification-driven development framework that separates:
- **Current reality** (`openspec/specs/`) - What is built
- **Future plans** (`openspec/changes/`) - What should be built
- **History** (`openspec/changes/archive/`) - What was built

### Three-Stage Workflow
1. **Create**: Write proposal + spec deltas + tasks
2. **Implement**: Execute tasks following the specification
3. **Archive**: Move completed change to archive, update specs

### Enforcement
- Strict format validation via CLI tool
- Each requirement must have testable scenarios
- Scenarios must follow exact format
- No vague requirements allowed

---

## Platform Architecture (Summary)

**9 Existing Capabilities:**
1. Data Ingestion (DLT, Crawl4AI, CocoIndex, OLake)
2. Orchestration (Dagster asset pipelines)
3. Lakehouse Storage (Iceberg, DuckLake, LakeFS)
4. Streaming Analytics (RisingWave, Ibis)
5. Realtime Data (Convex WebSocket)
6. User Authentication (BetterAuth OIDC)
7. Secrets Management (1Password, SOPS)
8. CI/CD Pipeline (Dagger)
9. Deployment Orchestration (Komodo, Pangolin)

**Architectural Priorities:**
- Type safety: BAML → Pydantic/Zod
- Multi-tenancy: orgId-based isolation everywhere
- Security: Zero-trust networking, encryption by default
- Incremental processing: No full re-indexing

---

## Creating an Integration Proposal

### Basic Structure
```
openspec/changes/add-[tech]-integration-[capability]/
├── proposal.md                    # Why + What + Impact
├── tasks.md                       # Implementation checklist
├── design.md (optional)           # Architectural decisions
└── specs/
    ├── web-application/spec.md   # If modifying existing capability
    └── [new-capability]/spec.md  # If creating new capability
```

### Critical Format Rules

**Scenario headers MUST use exactly 4 hashtags:**
```markdown
#### Scenario: Login success
- **WHEN** valid credentials
- **THEN** return JWT token
```

**NOT these:**
```markdown
- **Scenario: Login**        ❌ Bullet instead of ####
### Scenario: Login          ❌ Only 3 hashtags
**Scenario**: Login          ❌ Bold instead of ####
```

### Delta Operations
| Operation | Use When |
|-----------|----------|
| ADDED | New capability (standalone) |
| MODIFIED | Changing behavior (include full requirement) |
| REMOVED | Deprecating feature (reason + migration) |
| RENAMED | Name-only changes |

---

## Validation

Before submitting a proposal:

```bash
cd /Users/cliste/dev/bonneagar/hackathon

# Validate format and content
openspec validate add-[tech]-integration --strict

# Debug scenario parsing
openspec show add-[tech]-integration --json --deltas-only

# Should see no errors
```

---

## Reference Files

**Source Documentation:**
- `/Users/cliste/dev/bonneagar/hackathon/openspec/AGENTS.md` - AI assistant instructions (457 lines)
- `/Users/cliste/dev/bonneagar/hackathon/openspec/project.md` - Platform architecture (209 lines)
- `/Users/cliste/dev/bonneagar/hackathon/openspec/specs/*/spec.md` - 9 capability specs

**Active Proposals:**
- `enhance-github-pipeline-ingestion/` - GitHub API integration details
- `add-type-safe-rpc-api/` - Web API framework (oRPC)
- `add-code-indexing-capability/` - AI knowledge graph
- 4 others in progress

---

## Common Questions

**Q: How do I create a new proposal?**  
A: Follow the structure in INTEGRATION_PROPOSAL_GUIDE.md. Start with proposal.md explaining the "Why", then write spec deltas with ADDED/MODIFIED/REMOVED requirements, then tasks.md.

**Q: What if my technology affects multiple capabilities?**  
A: Create separate delta files for each capability under `changes/[id]/specs/[capability]/spec.md`

**Q: How specific do scenarios need to be?**  
A: Very specific. Include exact values, field names, error codes. "When user submits form" is too vague; "When user enters email john@example.com in login form" is good.

**Q: Can I skip the tasks.md file?**  
A: No. Tasks.md is part of the three-document requirement (proposal.md + tasks.md + spec deltas).

**Q: How long should a proposal be?**  
A: proposal.md should be 1-2 pages. Spec deltas can be longer (3-5+ pages depending on scope). tasks.md typically 1-2 pages.

**Q: Can I start implementation before proposal approval?**  
A: No. AGENTS.md explicitly states: "Do not start implementation until the proposal is reviewed and approved."

---

## Example Technologies

### Gradio (Python UI Framework)
- Maps to: `web-application` capability
- What it does: Rapid UI development for AI models
- Integration: Python backend → Hono API → Frontend
- Key requirements: Type safety, multi-tenancy, streaming

### Evidence.dev (SQL-Based Dashboards)
- Maps to: New `analytics-dashboards` capability
- What it does: SQL-based business intelligence dashboards
- Integration: Markdown dashboard definitions → DuckDB queries → UI
- Key requirements: Query performance, role-based access, incremental refresh

### MCP-UI (Model Context Protocol UI)
- Maps to: Depends on what MCP is
- Needs: Research and assessment before proposing

---

## Key Insights

1. **OpenSpec is the source of truth** - Platform decisions are recorded in specs, not code comments
2. **Type safety is mandatory** - Every API must use BAML → Pydantic/Zod
3. **Multi-tenancy is everywhere** - Every feature needs `orgId` filtering
4. **Scenarios = Requirements** - If it's not in a testable scenario, it doesn't exist
5. **Validation is automatic** - CLI catches format issues immediately

---

## Next Steps

1. **Read EXPLORATION_SUMMARY.md** (5 min)
2. **Research your technology** - What does it do, what problem does it solve?
3. **Map to capabilities** - Which existing capability does it extend, or is it new?
4. **Follow INTEGRATION_PROPOSAL_GUIDE.md** - Create proposal, tasks, spec delta
5. **Validate** - Run `openspec validate --strict`
6. **Submit for review** - Share with team

---

## Total Documentation

- **OPENSPEC_ANALYSIS.md**: 731 lines, complete reference
- **INTEGRATION_PROPOSAL_GUIDE.md**: 458 lines, how-to guide  
- **EXPLORATION_SUMMARY.md**: 395 lines, quick reference
- **Total**: 1,584 lines of guidance

Plus source documents at `/Users/cliste/dev/bonneagar/hackathon/openspec/`

---

**Last Updated**: November 28, 2025  
**Source Location**: `/Users/cliste/dev/bonneagar/hackathon/openspec/`  
**Generated by**: Claude Code exploration
