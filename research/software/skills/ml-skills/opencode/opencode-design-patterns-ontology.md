# OpenCode: Design Patterns, Programming Patterns & Conceptual Ontology

## Executive Summary

OpenCode is an open-source AI coding agent built for the terminal, designed to provide autonomous code generation, analysis, and modification capabilities through a command-line interface. Originally developed by SST (sst/opencode), it represents a terminal-first approach to AI-assisted development, emphasizing model-agnostic design, extensibility, and developer control.

**Key Characteristics:**
- Terminal-native TUI (Terminal User Interface) built with Go and Bubble Tea
- Client/server architecture enabling remote operation and multiple clients
- Event-driven design with strongly-typed event bus
- Model-agnostic provider system (Anthropic, OpenAI, Google, local models)
- Multi-agent system with primary agents and specialized subagents
- LSP (Language Server Protocol) integration for semantic code understanding
- Permission-based safety system balancing autonomy with user control
- SQLite-based persistence for sessions, messages, and file history
- Git-based snapshot system for rollback and undo capabilities

**Architecture Stack:**
- **Backend**: TypeScript/Bun with Hono HTTP framework
- **Frontend TUI**: Go with Bubble Tea framework (Elm Architecture)
- **Storage**: SQLite for persistence
- **Communication**: HTTP + Server-Sent Events (SSE)
- **AI Integration**: AI SDK with multiple provider support
- **Code Intelligence**: LSP servers (14 built-in languages)

---

## 1. CORE ARCHITECTURE AND DESIGN PATTERNS

### 1.1 Client/Server Architecture

OpenCode employs a **separation of concerns** through client/server design:

```
┌─────────────────────────────────────────────────────────┐
│                    OpenCode System                       │
├─────────────────────────────────────────────────────────┤
│  Clients (Multiple Concurrent)                          │
│  ├── TUI (Go/Bubble Tea)                                │
│  ├── VS Code Extension                                  │
│  ├── Mobile App (potential)                             │
│  └── SDK-based Custom Clients                           │
│                          ↕                               │
│              HTTP + SSE Communication                    │
│                          ↕                               │
│  Server (TypeScript/Bun)                                │
│  ├── Hono HTTP Framework                                │
│  ├── Session Management                                 │
│  ├── Tool Orchestration                                 │
│  ├── LLM Provider Integration                           │
│  ├── Event Bus (Pub/Sub)                                │
│  └── SQLite Storage                                     │
│                          ↕                               │
│  External Integrations                                  │
│  ├── LSP Servers                                        │
│  ├── MCP Servers                                        │
│  ├── Git (Snapshots)                                    │
│  └── LLM Providers                                      │
└─────────────────────────────────────────────────────────┘
```

**Design Benefits:**
- **Remote Operation**: Server runs on one machine, clients connect from anywhere
- **Multiple Clients**: TUI is just one interface; SDK enables custom clients
- **Scalability**: Separation allows independent scaling of UI and logic
- **Testability**: Server logic isolated from presentation concerns

### 1.2 Event-Driven Architecture

**Core Pattern**: Strongly-typed event bus as the orchestration backbone

```typescript
// Conceptual event flow
EventBus {
  publish(event: TypedEvent)
  subscribe(eventType: string, handler: (event) => void)
}

// Event types
- ToolCallEvent        // Tool execution started
- ToolResultEvent      // Tool execution completed
- TextUpdateEvent      // LLM streaming response
- PermissionRequest    // User approval needed
- SessionUpdate        // Session state changed
- MessageAdded         // New message in conversation
```

**Benefits:**
- **Loose Coupling**: Components communicate through events, not direct references
- **Real-Time Updates**: SSE streams events to clients for live UI updates
- **Extensibility**: New components subscribe to events without modifying core
- **Observability**: All actions flow through event bus for logging/debugging

**Implementation Pattern:**

```typescript
// Server publishes events
eventBus.publish({
  type: 'tool_call',
  toolName: 'edit',
  parameters: { file: 'src/app.ts', ... }
})

// TUI subscribes via SSE
const eventSource = new EventSource('/api/events')
eventSource.addEventListener('tool_call', (event) => {
  updateUI(JSON.parse(event.data))
})
```

### 1.3 Multi-Agent System Architecture

**Two-Tier Agent Hierarchy:**

```
┌────────────────────────────────────────────────────────┐
│              Primary Agents (User-Facing)              │
│  ┌──────────────┐         ┌──────────────┐            │
│  │    Build     │         │     Plan     │            │
│  │  (Full Tool  │  Tab    │  (Read-Only  │            │
│  │   Access)    │ ◄─────► │  Analysis)   │            │
│  └──────────────┘         └──────────────┘            │
│         │                        │                     │
│         └────────┬───────────────┘                     │
│                  │ Can invoke                          │
│                  ↓                                     │
│  ┌───────────────────────────────────────────┐        │
│  │         Subagents (Specialized)           │        │
│  │  ┌──────────┐  ┌──────────┐  ┌─────────┐ │        │
│  │  │ General  │  │  Custom  │  │  Task   │ │        │
│  │  │(Research)│  │ Subagent │  │ Manager │ │        │
│  │  └──────────┘  └──────────┘  └─────────┘ │        │
│  └───────────────────────────────────────────┘        │
└────────────────────────────────────────────────────────┘
```

**Agent Design Patterns:**

1. **Permission-Based Restrictions**
   - Tools configurable as "ask", "allow", or "deny"
   - Different agents have different tool access
   - Plan agent: read-only (bash, edit, write denied)
   - Build agent: full access with optional confirmations

2. **Model Specialization**
   - Agents can use different LLM models
   - Task-specific temperature settings (0.05-0.15 for precision, 0.2-0.3 for creativity)
   - Model selection based on task requirements

3. **Tool Isolation**
   - Agents specify allowed tools in configuration
   - Prevents unintended side effects
   - Enables safe exploratory agents

4. **Subagent Invocation**
   - Primary agents invoke subagents via `@mention` or tool calls
   - Subagents create child sessions with inherited context
   - Hierarchical session tree (parent → child relationships)

---

## 2. PROGRAMMING PATTERNS & WORKFLOW IDIOMS

### 2.1 Development Workflow Patterns

**Pattern 1: Planning-First Methodology**

```
Workflow:
1. Start with Plan agent (Tab to switch)
   └─ "Analyze the codebase and suggest how to add dark mode"

2. Plan agent explores (read-only)
   ├─ Reads relevant files
   ├─ Analyzes architecture
   └─ Suggests implementation steps

3. Switch to Build agent (Tab)
   └─ "Implement the dark mode plan"

4. Build agent executes
   ├─ Creates files
   ├─ Modifies components
   └─ Adds configuration
```

**Pattern 2: Multi-Agent Orchestration**

```
Sequential Agent Chain:

Task Manager Agent (planning)
  ↓ Outputs: task breakdown, affected files
Coder Agent (implementation)
  ↓ Outputs: code changes
Tester Agent (quality assurance)
  ↓ Outputs: test files
Documentation Agent (docs)
  ↓ Outputs: updated docs
Quality Agent (review)
  ↓ Outputs: feedback
Reviewer Agent (validation)
  └→ Final approval
```

### 2.2 Prompting Patterns

**Pattern 1: File References**

```
Syntax: @filename or @path/to/file

Examples:
  "Review @src/auth.ts for security issues"
  "Update @package.json to add new dependency"
  "Make @components/Button.tsx use theme colors"

With Line Numbers:
  "@File#L37-42" - specific line range

VS Code Integration:
  Cmd+Option+K / Alt+Ctrl+K inserts file reference
```

**Pattern 2: Subagent Invocation**

```
Explicit Mention:
  "@general research the latest React patterns"
  "@security-auditor review this code"

Automatic Invocation (via agent tool):
  Primary agent decides to invoke subagent
  Creates child session automatically
```

**Pattern 3: Custom Commands**

```
Create: ~/.config/opencode/commands/prime-context.md
---
name: prime-context
description: Add project context
---
Review the project structure and understand:
1. Main technologies used
2. Architecture patterns
3. Key entry points
Load context from @README.md and @package.json

Usage:
  $ opencode
  > /prime-context
```

---

## 3. CONCEPTUAL ONTOLOGY

### 3.1 Core Concepts

**Agent**
- **Definition**: A specialized AI assistant configured for specific tasks and workflows
- **Characteristics**:
  - Has model assignment (can differ between agents)
  - Has tool access permissions
  - Has temperature/creativity setting
  - Has custom system prompt
  - Can be primary (user-facing) or subagent (invoked)
- **Mental Model**: Think of agents as personas with different roles and capabilities
- **Example**: "build" agent for implementation, "plan" agent for analysis

**Session**
- **Definition**: A conversation thread between user and agent
- **Characteristics**:
  - Contains message history
  - Has associated agent and model
  - Can have parent session (if child/subagent session)
  - Persists across OpenCode restarts
  - Can be shared via URL
- **Mental Model**: Like a Git branch for conversations
- **Lifecycle**: Create → Messages exchange → Share/Archive

**Message**
- **Definition**: A single communication in a session
- **Roles**: user, assistant, system
- **Characteristics**:
  - Composed of parts (text, tool calls, tool results)
  - Timestamped
  - Immutable once created
- **Mental Model**: Like a commit in Git, but for conversation

**Tool**
- **Definition**: A capability the agent can invoke to interact with the environment
- **Characteristics**:
  - Has name, description, parameters
  - Has execute function
  - Subject to permissions
  - Can be built-in or MCP-provided
- **Mental Model**: APIs that the LLM calls to perform actions
- **Example**: read tool, edit tool, bash tool

**Provider**
- **Definition**: An LLM service that powers the agent
- **Supported**: Anthropic, OpenAI, Google, local (Ollama), OpenAI-compatible
- **Characteristics**:
  - Has API credentials
  - Has model catalog
  - Has pricing structure
- **Mental Model**: The "brain" behind the agent
- **Selection**: Via model string "anthropic/claude-sonnet-4-5"

### 3.2 Mental Model: How OpenCode Structures Development

**Traditional Development:**
```
Developer ←→ Code Editor ←→ Terminal ←→ Filesystem
     ↓                         ↓
  Thinking              Execute Commands
```

**OpenCode Model:**
```
Developer ←→ OpenCode TUI
                ↓
         (Natural Language)
                ↓
         LLM Agent (Brain)
         ↓     ↓     ↓
      Tools (Arms & Feet)
      ↓      ↓       ↓
   Files  Shell   Web

Snapshots: Git-based time travel
Permissions: Safety guardrails
LSP: Semantic understanding
```

**Cognitive Framework:**

1. **Agent as Collaborator**: The LLM is a pair programmer, not just a tool
2. **Conversation as Workflow**: Development happens through dialogue
3. **Tools as Capabilities**: Agent's abilities defined by available tools
4. **Sessions as Context**: Conversation history carries project understanding
5. **Snapshots as Safety**: Every action can be undone
6. **Permissions as Control**: User retains final authority

---

## 4. CONFIGURATION & EXTENSIBILITY

### 4.1 Configuration System

**Configuration File Locations:**

```
Priority (later overrides earlier):

1. Built-in Defaults
2. Global: ~/.config/opencode/opencode.json
3. Project: ./opencode.json
4. Environment: $OPENCODE_CONFIG
5. Directory: $OPENCODE_CONFIG_DIR
```

**Configuration Schema:**

```json
{
  "model": "anthropic/claude-sonnet-4-5",
  "small_model": "anthropic/claude-haiku-4",
  "providers": {
    "anthropic": {
      "apiKey": "{env:ANTHROPIC_API_KEY}"
    }
  },
  "agent": {
    "custom-agent": {
      "file": ".opencode/agent/custom.md",
      "temperature": 0.1,
      "tools": {
        "bash": "ask",
        "write": "allow"
      }
    }
  },
  "tools": {
    "bash": "ask",
    "write": "allow"
  },
  "mcp": {
    "postgres": {
      "type": "stdio",
      "command": "mcp-server-postgres",
      "args": ["--connection", "{env:DATABASE_URL}"]
    }
  }
}
```

---

## 5. BEST PRACTICES & PATTERNS SUMMARY

### 5.1 Effective Agent Usage

1. **Use Plan Agent First**: Explore and understand before implementing
2. **Create Specialized Agents**: Task-specific agents for workflows
3. **Leverage Subagents**: Decompose complex tasks via @mentions
4. **Optimize Context**: Use specific file references, avoid broad searches
5. **Session Management**: Start fresh when context is stale

### 5.2 Configuration Best Practices

1. **Global vs Project**: Personal preferences global, team conventions in project
2. **Rule Organization**: Modular rule files, share via symlinks/submodules
3. **Permission Strategy**: Default "ask" for bash, "allow" for read-only tools
4. **Model Selection**: Use small_model for simple tasks to reduce costs
5. **Formatter Integration**: Auto-format to maintain code consistency

---

**Document Version**: 1.0
**Last Updated**: 2025-11-20
**Research Sources**: OpenCode documentation, GitHub repository, community articles, technical deep dives
