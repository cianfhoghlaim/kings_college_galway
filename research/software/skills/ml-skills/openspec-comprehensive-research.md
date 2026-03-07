# OpenSpec: Comprehensive Research Report

## 1. Core Purpose & Philosophy

### Problem Statement
OpenSpec addresses a fundamental challenge in AI-assisted development: **AI coding assistants are powerful but unpredictable when requirements live solely in chat history**. Without formal documentation, AI tools operate on ephemeral context that is difficult to audit, version, or maintain across sessions.

### Value Proposition
OpenSpec introduces a **specification-driven development workflow** that:
- **Aligns humans and AI before implementation begins** through formal, auditable specifications
- **Locks intent via structured specs** rather than relying on conversational context
- **Creates a shared source of truth** between human stakeholders and AI coding assistants
- **Enables predictable AI behavior** by providing explicit, documented requirements

### Core Philosophy

The framework operates on three foundational principles:

1. **Specs are Truth**: The `specs/` directory represents what IS currently built and deployed
2. **Changes are Proposals**: The `changes/` directory represents what SHOULD change
3. **Agreement Before Implementation**: No code is written until specifications are reviewed and approved

This philosophy emphasizes **brownfield-first development** - excelling at modifying existing features rather than just greenfield projects. The system is designed to keep diffs explicit and manageable across feature development cycles.

---

## 2. Key Features & Capabilities

### Two-Folder Architecture

**`openspec/specs/`** - Current authoritative specifications
- Contains capabilities that are built and deployed
- Each capability has its own directory with `spec.md` and optional `design.md`
- Represents the source of truth for what exists

**`openspec/changes/`** - Proposed updates organized by feature
- Contains change proposals with spec deltas
- Organized into active changes and archived completed changes
- Keeps diffs explicit and prevents accidental overwrites

### Structured Change Management

Each change proposal includes:
- **proposal.md**: Why, what changes, and impact assessment
- **tasks.md**: Implementation checklist with numbered subtasks
- **design.md** (optional): Technical decisions, alternatives, and migration plans
- **specs/** subdirectory: Delta files showing ADDED/MODIFIED/REMOVED/RENAMED requirements

### Multi-Tool Support

Native integration with 20+ AI coding assistants:
- **Native slash commands**: Claude Code, CodeBuddy, Cursor, Cline, Windsurf, Factory Droid, and others
- **AGENTS.md compatibility**: Universal fallback for any AI tool
- **Automatic configuration**: `openspec init` sets up tool-specific files with managed marker blocks
- **Zero API keys required**: Works entirely through local file system

### Rich CLI Commands

```bash
# Discovery & Exploration
openspec list                    # List active changes
openspec list --specs            # List capabilities
openspec show [item]             # Display change or spec details
openspec spec list --long        # Detailed spec enumeration

# Workflow Operations
openspec init [path]             # Initialize OpenSpec structure
openspec update [path]           # Refresh instruction files
openspec scaffold <change-id>   # Generate change template
openspec validate [item]         # Check correctness
openspec archive <change-id>    # Complete and merge changes

# Interactive Mode
openspec view                    # Dashboard with progress metrics
openspec show                    # Prompts for selection
```

### Validation & Quality Assurance

- **Format validation**: Ensures spec deltas follow required structure
- **Scenario validation**: Requires at least one scenario per requirement
- **Strict mode**: Comprehensive checks via `--strict` flag
- **JSON output**: Machine-readable output for automation
- **Pre-implementation checks**: Validates before any code is written

---

## 3. Patterns & Architecture

### Software Architecture

**Technology Stack**:
- **Language**: TypeScript with strict mode
- **Runtime**: Node.js ≥20.19.0 (ESM modules)
- **CLI Framework**: Commander.js for command handling
- **Package Manager**: pnpm
- **Distribution**: npm package registry

**Code Organization** (Three-layer architecture):

1. **CLI Layer** (`src/cli/`): Command-line interface and user interaction
2. **Command Layer** (`src/commands/`): Individual command implementations
3. **Core Layer** (`src/core/`): Business logic modules
   - `parsers/`: Markdown and spec parsing
   - `validation/`: Requirement and scenario validation
   - `templates/`: File generation templates
   - `configurators/`: Tool-specific configuration
   - `converters/`: Data transformation
   - `schemas/`: Type definitions

**Design Principles**:
- **Async/await** for all asynchronous operations
- **Minimal dependencies** to reduce attack surface
- **Descriptive naming** optimized for AI comprehension
- **Error propagation** to CLI layer for consistent feedback
- **Semantic exit codes**: 0 (success), 1 (error), 2 (misuse)

### Workflow Patterns

**Three-Stage Development Lifecycle**:

#### Stage 1: Creating Changes
1. Review current state: `openspec list`, `openspec list --specs`
2. Choose unique kebab-case `change-id` (verb-led: `add-`, `update-`, `remove-`)
3. Scaffold proposal, tasks, design (optional), and spec deltas
4. Draft spec deltas using operation headers
5. Validate with `openspec validate <id> --strict`
6. Request approval before implementation

#### Stage 2: Implementing Changes
1. Read `proposal.md` to understand goals
2. Read `design.md` (if exists) for technical decisions
3. Read `tasks.md` for implementation checklist
4. Implement tasks sequentially
5. Confirm completion of every task
6. Update checklist: mark all items `- [x]`
7. Approval gate: Don't start without approval

#### Stage 3: Archiving Changes
1. After deployment, create separate PR
2. Move `changes/[name]/` → `changes/archive/YYYY-MM-DD-[name]/`
3. Update `specs/` to reflect new reality
4. Run `openspec archive <change-id>` with flags as needed
5. Validate with `openspec validate --strict`

### Delta-Based Change Model

The system uses **operation headers** to express specification changes:

- **`## ADDED Requirements`**: New capabilities that can stand alone
- **`## MODIFIED Requirements`**: Changed behavior of existing requirements (must include full updated content)
- **`## REMOVED Requirements`**: Deprecated features with reason and migration path
- **`## RENAMED Requirements`**: Name changes (FROM/TO syntax)

**Critical Decision Logic**:
- Use ADDED when introducing orthogonal new capability
- Use MODIFIED when changing behavior/scope (copy entire requirement, update, paste)
- Avoid partial MODIFIED deltas - they cause information loss during archiving

---

## 4. Ontology & Concepts

### Core Entities

**Capability**: A single, focused area of functionality
- Stored in `openspec/specs/[capability-name]/`
- Contains `spec.md` (requirements) and optional `design.md` (technical patterns)
- Naming: verb-noun format (`user-auth`, `payment-capture`)
- Single purpose per capability (10-minute understandability rule)

**Change**: A proposed modification to one or more capabilities
- Stored in `openspec/changes/[change-id]/`
- Contains proposal, tasks, optional design, and spec deltas
- Unique kebab-case identifier with verb-led prefix
- Represents what SHOULD change (proposal state)

**Requirement**: A normative statement of system behavior
- Uses SHALL/MUST for mandatory requirements
- Must have at least one scenario
- Format: `### Requirement: [Name]`
- Describes what the system must do

**Scenario**: Concrete example of requirement behavior
- Given-When-Then format with structured bullets
- Format: `#### Scenario: [Name]` (exactly 4 hashtags)
- Required elements: **WHEN** (trigger), **THEN** (outcome)
- Optional: **GIVEN** (preconditions), **AND** (additional assertions)

### Mental Model

OpenSpec conceptualizes software development as a **specification lifecycle**:

```
Current State (specs/)
       ↓
  Proposal (changes/[id]/)
       ↓
   Review & Approval
       ↓
  Implementation (code)
       ↓
   Archive (changes/archive/)
       ↓
  Update Specs (specs/)
```

**Key Insight**: Specifications are **living documents** that evolve with the codebase. They're not upfront design documents written once and forgotten, but rather maintained sources of truth that reflect current reality.

### Terminology

**Spec Delta**: A file showing proposed changes to a capability's requirements using operation headers (ADDED/MODIFIED/REMOVED/RENAMED)

**Managed Block**: Comment markers (`<!-- OPENSPEC:START -->` / `<!-- OPENSPEC:END -->`) that allow `openspec update` to refresh instruction snippets without overwriting custom content

**Idempotent Scaffolding**: The ability to run `openspec scaffold` multiple times safely - existing content is preserved, only missing files are created

**Brownfield-First**: Design philosophy prioritizing modification of existing features over greenfield development

**Capability Naming**: Verb-noun format emphasizing single-purpose capabilities that can be understood in 10 minutes

**Change ID Naming**: Kebab-case, verb-led identifiers that are short, descriptive, and unique

---

## 5. Integration & Usage

### For Developers

**Initial Setup**:
```bash
# Install globally
npm install -g openspec

# Initialize in project
cd my-project
openspec init

# Select AI tools from multi-select menu
# Creates openspec/ structure and tool-specific files
```

**Daily Workflow**:
```bash
# Starting new feature
openspec list --specs           # Check existing capabilities
openspec list                   # Check active changes
openspec scaffold add-2fa       # Generate change structure

# Edit generated files:
# - openspec/changes/add-2fa/proposal.md
# - openspec/changes/add-2fa/tasks.md
# - openspec/changes/add-2fa/specs/auth/spec.md

# Validate before sharing
openspec validate add-2fa --strict

# After implementation and deployment
openspec archive add-2fa
```

### For AI Agents

**Context Loading Pattern**:
1. Read `/openspec/AGENTS.md` for workflow instructions
2. Check `openspec/project.md` for project-specific conventions
3. Run `openspec list` to see active changes
4. Run `openspec list --specs` to see existing capabilities
5. Use `openspec show [item]` for detailed inspection

**Decision Tree for AI Agents**:
```
New request received?
├─ Bug fix restoring spec behavior? → Fix directly
├─ Typo/format/comment? → Fix directly
├─ Dependency update (non-breaking)? → Fix directly
├─ New feature/capability? → Create proposal
├─ Breaking change? → Create proposal
├─ Architecture change? → Create proposal
└─ Unclear? → Create proposal (safer)
```

**Proposal Creation by AI**:
```bash
# 1. Explore context
openspec spec list --long
openspec list
rg -n "Requirement:|Scenario:" openspec/specs  # Full-text search

# 2. Scaffold
mkdir -p openspec/changes/add-2fa/specs/auth
cat > openspec/changes/add-2fa/proposal.md << 'EOF'
## Why
Users need enhanced security beyond passwords.

## What Changes
- Add two-factor authentication to login flow
- Support TOTP and SMS methods

## Impact
- Affected specs: auth
- Affected code: auth/login.ts, auth/verify.ts
EOF

# 3. Write delta
cat > openspec/changes/add-2fa/specs/auth/spec.md << 'EOF'
## ADDED Requirements
### Requirement: Two-Factor Authentication
Users MUST provide a second factor during login.

#### Scenario: OTP required
- **WHEN** valid credentials are provided
- **THEN** an OTP challenge is required
EOF

# 4. Validate
openspec validate add-2fa --strict
```

### Tool-Specific Integration

**Claude Code**:
- Reads `CLAUDE.md` with managed OpenSpec instructions block
- Uses slash commands: `/openspec:proposal`, `/openspec:apply`, `/openspec:archive`

**Cursor**:
- Reads `.cursorrules` with managed OpenSpec instructions block
- Custom rules automatically point to `@/openspec/AGENTS.md`

**Cline**:
- Reads `.clinerules` with similar managed block structure
- Integrates with MCP for enhanced capabilities

**Universal Fallback**:
- Any tool can read root `AGENTS.md` file
- Points to `@/openspec/AGENTS.md` for detailed workflow
- No tool-specific configuration required

---

## 6. Unique Differentiators

### 1. Brownfield-First Design
Unlike most specification frameworks that focus on greenfield projects, OpenSpec **excels at modifying existing features**. The delta-based change model makes it natural to express "here's what we're adding/changing/removing" rather than rewriting entire specs.

### 2. Grouped Change Folders
Most tools scatter specification changes across files. OpenSpec's **single folder per change** (`changes/[id]/`) groups related specifications, designs, and tasks together, making it easy to understand scope and impact.

### 3. AI Agent as First-Class User
OpenSpec is explicitly designed for **human-AI collaboration**, not just human-to-human communication. The AGENTS.md file provides detailed workflow instructions, decision trees, and error recovery guidance specifically for AI coding assistants.

### 4. Zero API Dependencies
Unlike cloud-based specification tools, OpenSpec **requires no API keys, accounts, or network calls**. It's entirely file-system based, making it fast, private, and secure.

### 5. Multi-Tool Orchestration
Rather than forcing teams onto a single AI assistant, OpenSpec provides **unified management across 20+ tools**. One `openspec update` command refreshes all tool configurations simultaneously.

### 6. Managed Block Technology
The use of **comment markers for managed blocks** allows OpenSpec to update instruction files without overwriting custom content. This enables automatic updates while preserving user customizations.

### 7. Validation-First Workflow
OpenSpec validates specifications **before implementation**, catching formatting errors, missing scenarios, and structural issues early. Most frameworks validate after code is written.

### 8. Explicit State Transitions
The three-folder structure (`specs/`, `changes/`, `changes/archive/`) makes the **state of every feature obvious**: is it built, proposed, or completed? This eliminates confusion about what's real versus planned.

### 9. Scenario-Driven Requirements
The **mandatory scenario requirement** ensures every specification includes concrete examples. You can't write "The system SHALL authenticate users" without showing what that actually looks like.

### 10. Simplicity-First Culture
The framework actively **pushes against premature complexity**, recommending <100 lines of code, single-file implementations, and boring patterns unless data proves otherwise.

---

## 7. Best Practices

### Simplicity First

**Default Constraints**:
- **<100 lines** of new code per change
- **Single-file** implementations until proven insufficient
- **Avoid frameworks** without clear justification
- **Choose boring, proven patterns** over novel approaches

**Complexity Triggers** (only add complexity with):
- Performance data showing current solution too slow
- Concrete scale requirements (>1000 users, >100MB data)
- Multiple proven use cases requiring abstraction

### Requirement Writing

**Normative Language**:
- Use **SHALL/MUST** for mandatory requirements
- Avoid should/may unless intentionally non-normative
- Be specific and testable

**Scenario Quality**:
- Include **at least one scenario** per requirement
- Use **Given-When-Then** structure consistently
- Show **both success and failure cases**
- Make scenarios **concrete and specific** (not abstract)

**Example of Good Requirement**:
```markdown
### Requirement: Session Expiry
User sessions SHALL expire after 7 days of inactivity.

#### Scenario: Session expires
- **GIVEN** a user logged in 8 days ago
- **WHEN** making an authenticated request
- **THEN** the request SHALL be rejected with 401 Unauthorized
- **AND** the session SHALL be deleted from the database

#### Scenario: Session within expiry window
- **GIVEN** a user logged in 5 days ago
- **WHEN** making an authenticated request
- **THEN** the request SHALL succeed
- **AND** the session expiry SHALL NOT be extended
```

### Change Management

**Capability Naming**:
- Use **verb-noun format**: `user-auth`, `payment-capture`, `data-ingestion`
- **Single purpose** per capability
- Apply **10-minute understandability rule**: can someone new understand it in 10 minutes?
- **Split if description needs "AND"**: if you say "X AND Y", it's probably two capabilities

**Change ID Naming**:
- Use **kebab-case**: `add-two-factor-auth`, not `AddTwoFactorAuth`
- **Verb-led prefixes**: `add-`, `update-`, `remove-`, `refactor-`
- Keep it **short and descriptive**: `add-2fa` better than `add-two-factor-authentication-with-totp-and-sms`
- Ensure **uniqueness**: if taken, append `-2`, `-3`, etc.

**When to Create design.md**:
Create `design.md` **only if any of these apply**:
- Cross-cutting change (multiple services/modules)
- New architectural pattern
- New external dependency
- Significant data model changes
- Security, performance, or migration complexity
- Ambiguity that benefits from technical decisions before coding

Otherwise, **omit it**. Most simple features don't need a design doc.

### Clear References

**Code Locations**:
- Use `file.ts:42` format for precise line references
- Link to specific functions: `auth.ts:validateToken()`

**Spec References**:
- Use full path: `specs/auth/spec.md`
- Reference requirements: `specs/auth/spec.md - Requirement: Session Management`

**Related Work**:
- Link to related changes: `changes/add-oauth/`
- Link to PRs: `See PR #123 for implementation`

### Validation & Error Recovery

**Always Validate**:
```bash
# Use strict mode for comprehensive checks
openspec validate [change] --strict

# Check before sharing proposal
openspec validate add-2fa --strict

# Bulk validation
openspec validate --strict
```

**Debug Failures**:
```bash
# Debug delta parsing
openspec show [change] --json | jq '.deltas'

# Check specific requirement
openspec show [spec] --json -r 1

# View all validation errors
openspec validate [change] --strict --json
```

**Common Errors**:

1. **"Change must have at least one delta"**
   - Check that `changes/[name]/specs/` exists with .md files
   - Verify files have operation headers (`## ADDED Requirements`)

2. **"Requirement must have at least one scenario"**
   - Use exactly `#### Scenario:` (4 hashtags)
   - Don't use bullets or bold for scenario headers

3. **Silent scenario parsing failures**
   - Debug with `openspec show [change] --json --deltas-only`
   - Verify exact format: `#### Scenario: Name`

### Tool Selection for AI Agents

| Task | Tool | Why |
|------|------|-----|
| Find files by pattern | `Glob` | Fast pattern matching |
| Search code content | `Grep` | Optimized regex search |
| Read specific files | `Read` | Direct file access |
| List specs | `openspec list --specs` | Structured output |
| List changes | `openspec list` | Current state |
| Full-text search | `rg -n "pattern" openspec/` | When structure unknown |

### Conflict Resolution

**Change Conflicts**:
1. Run `openspec list` to see active changes
2. Check for overlapping specs
3. Coordinate with change owners
4. Consider combining proposals

**Validation Failures**:
1. Run with `--strict` flag first
2. Check JSON output for details
3. Verify spec file format exactly
4. Ensure scenarios use `####` headers

**Missing Context**:
1. Read `project.md` first
2. Check related specs with `openspec show`
3. Review recent archives
4. Ask for clarification if ambiguous

---

## Conclusion

OpenSpec represents a **paradigm shift in how humans and AI collaborate on software development**. By formalizing specifications in a structured, auditable format, it transforms AI coding assistants from unpredictable chat partners into reliable implementation agents that work from shared truth.

The framework's unique combination of:
- **Delta-based change management** for brownfield development
- **AI-first design** with explicit workflow instructions
- **Multi-tool orchestration** without vendor lock-in
- **Validation-first workflow** catching errors early
- **Simplicity-first culture** preventing premature complexity

...makes it particularly well-suited for teams that want to maintain control and predictability while leveraging AI assistance at scale.

The most profound insight from OpenSpec is that **specifications are not documentation debt—they're assets that make AI reliable**. When AI has explicit, structured requirements to work from, it becomes a force multiplier. When it only has chat history, it's a dice roll.

OpenSpec turns AI coding assistants from **unpredictable to predictable**, from **context-dependent to context-aware**, and from **tools you hope work to tools you trust**.

---

## References

- **Repository**: https://github.com/Fission-AI/OpenSpec
- **Documentation**: `/openspec/AGENTS.md` (local), repository AGENTS.md
- **License**: MIT
- **Stars**: 9.8k (as of research date)
- **Local Implementation**: /home/user/hackathon/openspec/
