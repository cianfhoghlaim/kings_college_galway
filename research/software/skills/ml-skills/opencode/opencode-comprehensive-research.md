# OpenCode: Comprehensive Research Report

**Research Date:** November 2025
**Project:** https://github.com/sst/opencode
**Website:** https://opencode.ai/
**License:** MIT

## Executive Summary

OpenCode is an open-source AI coding agent built specifically for terminal environments. Developed by the SST (Serverless Stack) team, it has achieved significant adoption with 33,000+ GitHub stars, 300+ contributors, and approximately 300,000 monthly developers. The project emphasizes provider-agnostic AI integration, supporting 75+ LLM providers through Models.dev integration, while maintaining a privacy-first architecture that doesn't store code or context data.

**Key Statistics:**
- 33.4k GitHub stars, 2.7k forks
- 312 contributors
- 523 releases
- 4,267 commits on development branch
- MIT licensed
- Active Discord community
- 1.1M+ total downloads (GitHub releases + npm)

**Repository Composition:**
- TypeScript: 59.2%
- Python: 14.4%
- Go: 11.6%
- CSS: 8.0%
- MDX: 5.3%

---

## 1. Core Features & Capabilities

### 1.1 Model Flexibility and Provider Support

**Models.dev Integration:**
OpenCode leverages the AI SDK and Models.dev database to support 75+ LLM providers. Models.dev is an open-source database containing specifications, pricing, features, and context limits for AI models across multiple providers.

**How It Works:**
- Standard providers automatically pull model information from Models.dev
- Provider specifications include: model capabilities, pricing per million tokens, features (tool calling, file attachments), and context limits
- Popular providers are preloaded by default
- OpenCode dynamically installs provider SDKs via npm when needed

**Supported Providers Include:**
- Anthropic (Claude Pro/Max via Anthropic login)
- OpenAI
- Google (Gemini)
- Amazon Bedrock
- Azure OpenAI
- Local models (via Ollama and compatible endpoints)
- 70+ additional providers

**Provider Configuration:**
Credentials loaded from:
- Environment variables (e.g., `ANTHROPIC_API_KEY`)
- `~/.opencode/data/auth.json`
- Custom loaders for specialized providers (Bedrock, Vertex)

**Model Selection:**
Default format: `provider/model` (e.g., `anthropic/claude-sonnet-4-5`)

### 1.2 Agent Modes

OpenCode includes three distinct agent operational modes accessible via Tab key:

#### Build Agent (Primary Mode - Default)
- **Purpose:** General-purpose development agent
- **Permissions:** Full access to all tools and operations
- **Use Case:** Day-to-day coding tasks, feature implementation
- **Behavior:** Unrestricted file editing and command execution

#### Plan Agent (Primary Mode)
- **Purpose:** Read-only analysis and planning
- **Permissions:** Denies file edits by default, asks permission before running bash commands
- **Use Case:** Exploring unfamiliar codebases, code analysis
- **Behavior:** Non-destructive exploration with user confirmation for commands

#### General Agent (Subagent Mode)
- **Purpose:** Complex searches and multi-step tasks
- **Invocation:** Via `@general` in messages
- **Permissions:** Allow edit, ask bash, allow webfetch
- **Special:** Disables `todoread` and `todowrite` tools
- **Use Case:** Research, searching, and multi-step task delegation

**Agent Hierarchy:**
- **Primary Agents:** Directly selectable by users as main session agent
- **Subagents:** Only callable by other agents via the `task` tool for delegation
- **All Mode:** Available in both primary and subagent contexts

### 1.3 Terminal UI (TUI) Features

**Technology Stack:**
- Built with Go using Bubble Tea framework
- Communicates with TypeScript/Bun backend via HTTP and Server-Sent Events (SSE)
- Thin client architecture over core TypeScript engine

**Key Features:**
- **Native & Responsive:** Fast, native terminal experience
- **Themeable:** Custom themes via configuration
- **Real-time Updates:** Event-driven architecture with SSE streaming
- **Multi-session Support:** Start multiple parallel agents on the same project
- **Session Sharing:** Shareable links for debugging and collaboration
- **Interactive Dialogs:** Permission prompts, file selection, agent switching
- **Rich Context:** Drag-and-drop image support, `@` file references

**Navigation:**
- Tab key: Switch between agents
- Keyboard shortcuts for common operations
- Leader key support for custom keybindings

### 1.4 File Operations and Codebase Manipulation

**Built-in Tools:**

1. **read** - Display file contents
   - Binary file detection with safety checks
   - Context limit awareness
   - Line-based reading with offset/limit support

2. **write** - Create or overwrite entire files
   - Automatic directory creation
   - Binary file safety checks
   - UTF-8 encoding validation

3. **edit** - Modify file contents with line-based editing
   - Line-based replacement operations
   - Multiple edit operations per invocation
   - LSP integration for real-time diagnostics

4. **glob** - Pattern matching and file discovery
   - Glob pattern support (`**/*.js`, `src/**/*.ts`)
   - Fast file pattern matching
   - Works with any codebase size

5. **grep** - Content search and pattern matching
   - Full regex support
   - File type filtering
   - Context lines before/after matches

6. **bash** - Command execution
   - Working directory awareness
   - Output capture and streaming
   - Permission-controlled execution
   - Environment variable access

7. **todoread/todowrite** - Session-persistent task management
   - Track progress across agent interactions
   - Persistent storage within session
   - Task state management

8. **task** - Spawn sub-agents
   - Delegate specialized work to custom agents
   - Independent sessions with own models/tools/prompts
   - Results returned to parent agent

9. **webfetch** - Retrieve and parse web content
   - URL content fetching
   - HTML to markdown conversion
   - AI processing of fetched content

**LSP Integration for Enhanced File Operations:**

When agents edit code, Language Server Protocol clients provide:
- **Real-time Diagnostics:** Syntax and type error detection
- **Grounding Mechanism:** Prevents hallucinations by making agent aware of errors
- **Auto-formatting:** Code formatting during file operations
- **Symbol Information:** Function signatures, dependencies, usage patterns
- **Semantic Understanding:** Rich codebase structure analysis

**Supported Languages (14 built-in LSP servers):**
TypeScript, JavaScript, Python, Go, Rust, Ruby, C/C++, C#, Elixir, Zig, Java, Vue, Svelte

### 1.5 IDE Integration

**VS Code Extension:**

**Installation:**
Available from Visual Studio Marketplace: `sst-dev.opencode`

**Key Features:**
- **Keyboard Shortcuts:**
  - `Cmd+Esc` (Mac) / `Ctrl+Esc` (Windows/Linux): Open OpenCode in split terminal or focus existing session
  - `Cmd+Shift+Esc` (Mac) / `Ctrl+Shift+Esc` (Windows/Linux): Start new OpenCode session
  - `Cmd+Option+K` (Mac) / `Alt+Ctrl+K` (Linux/Windows): Insert file references (e.g., `@File#L37-42`)

- **Context Awareness:** Automatically share current selection or tab with OpenCode
- **UI Button:** Click OpenCode button in VS Code UI
- **Terminal-Based:** Lightweight integration via integrated terminal
- **Split View:** Opens in split terminal view for parallel work

**Architecture Philosophy:**
- Terminal-first design (not language server or custom views)
- Lightweight extension footprint
- Works with any IDE supporting terminal (VS Code, Cursor, etc.)
- Compatible with neovim and other terminal-based editors

### 1.6 Advanced Capabilities

**Session Management:**
- **Conversation History:** MessageV2 format storing user/assistant messages with structured parts (text, tool calls, reasoning, file attachments)
- **Parent-Child Relationships:** Conversation branching support
- **Session Sharing:** Via Cloudflare Workers with Durable Objects
- **Automatic Message Compaction:** Manages context when approaching limits
- **Per-Directory Singleton:** Project isolation pattern
- **XDG Base Directory Standard:** Filesystem-based persistence

**Session Operations:**
- `/sessions`: Display list of project sessions
- `/undo` and `/redo`: Revert and reapply modifications
- `/init`: Initialize project with OpenCode
- Shareable links for collaboration

**Git Integration:**
- Git snapshots track working states
- Rollback capability if tool execution fails
- Automatic state preservation

---

## 2. Technical Architecture

### 2.1 Implementation Language and Overall Architecture

**Client/Server Design:**

OpenCode employs a separation of concerns through client/server architecture:

```
┌─────────────────────┐
│   Go TUI Client     │ (Terminal User Interface)
│   (Bubble Tea)      │
└──────────┬──────────┘
           │ HTTP + SSE
           ▼
┌─────────────────────┐
│  TypeScript/Bun     │ (Backend Server)
│  Backend (Hono)     │
└──────────┬──────────┘
           │
     ┌─────┴─────┬─────────┬──────────┐
     ▼           ▼         ▼          ▼
  Provider    Tool      LSP/MCP    Session
  Layer      Registry   Clients    Manager
```

**Components:**

1. **Backend (TypeScript/Bun):**
   - Core application published as `opencode-ai` on npm
   - HTTP server built with Hono framework
   - Exposes REST API via OpenAPI specification
   - Handles AI provider abstraction
   - Manages tool registry and execution
   - SQLite for session persistence

2. **Frontend (Go TUI):**
   - Separate Go application using Bubble Tea framework
   - HTTP client communicating with backend
   - Server-Sent Events (SSE) for real-time updates
   - Thin client over TypeScript engine
   - Platform-specific binaries

3. **Communication Protocols:**
   - HTTP for synchronous requests
   - Server-Sent Events (SSE) for streaming responses
   - Event Bus for cross-component communication
   - JSON-RPC for LSP communication (via vscode-jsonrpc)

**Flexibility:**
Any HTTP client can drive the agent (mobile apps, web interfaces, automation scripts, custom CLIs)

### 2.2 LLM Interaction Layer

**"The Trillion Dollar Loop":**

OpenCode implements a continuous cycle:

```
1. User Prompt + Conversation History + Available Tools
   ↓
2. LLM Decides Which Tools to Invoke
   ↓
3. Tool Execution in Bun Runtime
   ↓
4. Results Fed Back to Model
   ↓
5. Loop Continues Until Model Stops or Limits Reached
```

**Provider Abstraction:**

The provider layer abstracts 10+ AI services through a unified interface:

- **Dynamic Provider Installation:** Providers installed via npm on-demand
- **Credential Management:** Environment variables, auth.json, or custom loaders
- **Model Discovery:** Via Models.dev API with user override support
- **Caching:** Models cached after first load
- **OpenAI-Compatible Endpoints:** Support for self-hosted models

**AI SDK Standardization:**
Interactions standardized across providers (Anthropic, OpenAI, Gemini), allowing provider swaps by changing API keys.

**Context Management:**
- Automatic summarization when approaching context limits
- System prompt: "Provide a detailed but concise summary..."
- Token waste reduction while preserving critical information
- Context limit tracking per model via Models.dev

### 2.3 Tool System Architecture

**Three-Source Integration:**

```
Tool Registry
├── Built-in Tools (9 core tools)
├── MCP Server Tools (dynamically registered)
└── Plugin Tools (custom user-defined)
```

**Tool Definition Framework:**

Tools defined via `@opencode-ai/plugin` package with Zod-based schemas:

```typescript
{
  name: string,
  description: string,
  schema: ZodSchema,  // Input validation
  execute: (args) => Promise<result>
}
```

**Execution Flow with Permissions:**

```
1. AI Model Generates Tool Call (name + arguments)
   ↓
2. Registry Lookup (retrieve tool definition)
   ↓
3. Permission Check (evaluate against agent rules)
   ↓
4. User Confirmation (if permission = "ask")
   ↓
5. Tool Execution (run implementation)
   ↓
6. Result Processing (MessageV2.Part with type "tool")
   ↓
7. Response Continuation (return to AI model)
```

**Permission System:**

Three evaluation modes:
- **allow:** Auto-execute without prompting
- **deny:** Reject with error
- **ask:** Display interactive permission dialog

Permissions configured per-agent using:
- Global permission strings
- Wildcard patterns for bash commands
- Specificity-based matching
- Granular tool-level control

**Tool Descriptions:**
Each tool includes detailed description guiding LLM on:
- Proper usage patterns
- Parameter formats and validation
- Limitations and constraints
- Expected behavior

### 2.4 Configuration System

**Hierarchical Precedence (highest to lowest):**

1. Environment variable overrides (`OPENCODE_PERMISSION` flag)
2. Plugin configurations
3. Well-known provider configurations (`.well-known/opencode`)
4. Custom inline config (`OPENCODE_CONFIG_CONTENT`)
5. Custom config file (`OPENCODE_CONFIG`)
6. Project configs (upward `.opencode/` directory search)
7. Global configs (`~/.config/opencode/`)

**XDG-Compliant Directory Paths:**

- **Global config:** `~/.config/opencode/` (loads `config.json`, `opencode.json`, or `opencode.jsonc`)
- **Project config:** `.opencode/` directories (searched bottom-up)
- **State/cache:** `~/.local/state/opencode/` and `~/.cache/opencode/`
- **Application data:** `~/.local/share/opencode/`

**Core Configuration Options:**

```json
{
  "model": "provider/model",           // Default AI model
  "small_model": "provider/model",     // Title generation model
  "theme": "theme-name",               // Interface theme
  "username": "custom-name",           // Conversation username
  "share": "manual|auto|disabled",     // Sharing behavior
  "autoupdate": true,                  // Auto-update setting
  "permission": "ask|allow|deny",      // Global permissions

  "agent": {                           // Named agent definitions
    "agent-name": {
      "model": "override-model",
      "temperature": 0.7,
      "top_p": 0.9,
      "prompt": "custom system prompt",
      "tools": ["tool1", "tool2"],
      "permission": {
        "edit": "allow",
        "bash": "ask",
        "webfetch": "deny"
      },
      "mode": "primary|subagent|all",
      "description": "when to use this agent"
    }
  },

  "provider": {},                      // Provider credentials
  "command": {},                       // Command templates
  "mcp": {},                           // MCP server configs
  "lsp": {},                           // LSP server configs
  "tools": {},                         // Tool enable/disable
  "keybinds": {}                       // Custom keybindings
}
```

**File Format Support:**

- **JSONC:** JSON with comments and trailing commas (via `jsonc-parser`)
- **Markdown:** Agent/command definitions with YAML frontmatter
- **Environment Interpolation:** `{env:VAR_NAME}` syntax
- **File References:** `{file:path}` for external inclusion

**Markdown-Based Configuration:**

Agent files (`.opencode/agent/*.md`):
```markdown
---
model: anthropic/claude-sonnet-4
temperature: 0.7
permission:
  edit: allow
  bash: ask
---

You are a specialized agent for...
```

Nested directories create hierarchical names:
- `.opencode/agent/codegen/python.md` → agent name: `codegen/python`

**Validation:**

Allowed `.opencode/` subdirectories:
- `agent/`, `command/`, `mcp/`, `lsp/`, `formatter/`, `plugin/`, `instructions/`

Typos (e.g., `agents/` plural) rejected to prevent configuration errors.

**Error Handling:**

- `ConfigJsonError`: JSONC parsing failures with line/column information
- `ConfigInvalidError`: Schema validation failures via Zod

### 2.5 LSP Integration Architecture

**Three-Layer Organization:**

```
┌──────────────────────────────────────┐
│  LSP Namespace (Coordination Layer)  │
│  - State management                  │
│  - API functions                     │
└─────────────┬────────────────────────┘
              │
     ┌────────┴────────┐
     ▼                 ▼
┌──────────┐    ┌──────────────┐
│ LSPServer│    │  LSPClient   │
│(Definitions)   │(Connections) │
└──────────┘    └──────────────┘
```

**Capabilities:**

1. **Automatic Server Discovery:** Based on file extensions
2. **On-Demand Process Spawning:** Servers started when needed
3. **Tool Integration:** Diagnostics, symbol lookup, hover info exposed to AI agents
4. **Real-Time Analysis:** Live feedback during code editing

**Communication:**
- JSON-RPC protocol via `vscode-jsonrpc`
- Bidirectional communication with language servers
- Standard LSP methods support

**14 Built-in Servers:**
TypeScript, JavaScript, Python, Go, Rust, Ruby, C/C++, C#, Elixir, Zig, Java, Vue, Svelte

**Custom LSP Configuration:**
Users can add custom LSP servers specifying:
- File extensions handled
- Server command and arguments
- Initialization options

### 2.6 Model Context Protocol (MCP) Integration

**Overview:**

MCP extends OpenCode's built-in tool system by connecting to external tool providers.

**Architecture:**

```
OpenCode Tool Registry
├── Built-in Tools
├── Plugin Tools
└── MCP Tools ─┬─ Local MCP Servers (subprocess)
               └─ Remote MCP Servers (HTTP)
```

**How It Works:**

1. **Initialization:** MCP servers initialized during OpenCode bootstrap
2. **Tool Discovery:** Server advertises available tools
3. **Registration:** Tools fetched and added to tool registry
4. **Exposure:** MCP tools exposed via `/experimental/tool` endpoint
5. **Schema Conversion:** Tools converted to JSON Schema for AI model compatibility

**Configuration:**

In `opencode.json` under `mcp` field:

```json
{
  "mcp": {
    "server-name": {
      "type": "local",
      "command": "node",
      "args": ["server.js"],
      "env": {
        "API_KEY": "{env:MY_API_KEY}"
      }
    },
    "remote-server": {
      "type": "remote",
      "url": "https://api.example.com/mcp",
      "headers": {
        "Authorization": "Bearer {env:TOKEN}"
      }
    }
  }
}
```

**Key API Endpoints:**

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/app/mcp/status` | GET | Connection state and tool count per server |
| `/app/mcp/add` | POST | Dynamically register new MCP servers |
| `/experimental/tool` | GET | List all tools including MCP-registered |

**Error Handling:**

- Timeout default: 5000ms
- Failed servers marked but don't crash session
- Graceful degradation with remaining tools
- Individual tool execution failures return error states

**Comparison: Built-in vs MCP Tools:**

| Aspect | Built-in Tools | MCP Tools |
|--------|---------------|-----------|
| Definition | In codebase | External servers |
| Registration | Compile time | Runtime (dynamic) |
| Integration | Native | Via MCP protocol |
| Permissions | Same system | Same system |

### 2.7 SDK and Distribution

**SDK Packages:**

1. **JavaScript/TypeScript SDK** (`@opencode-ai/sdk`)
   - Three export paths for different integration scenarios
   - Generated from OpenAPI specification via `@hey-api/openapi-ts`
   - Type-safe client code
   - Auto-synced with server API
   - Client and server exports

2. **Go SDK** (`github.com/sst/opencode-sdk-go`)
   - Convenient REST API access
   - Requires Go 1.22+
   - Protocol translation for TUI
   - Type-safe bindings

3. **Plugin SDK** (`@opencode-ai/plugin`)
   - Zod-based schema definitions
   - Tool definition framework
   - Utilities for custom tool creation

**Distribution Channels:**

```
Platform Packages:
├── NPM: opencode-ai (main package)
├── NPM: opencode-{os}-{arch} (platform-specific binaries)
├── Homebrew (macOS/Linux)
├── Scoop (Windows)
├── Chocolatey (Windows)
├── AUR/Paru (Arch Linux)
├── Nix flake
├── mise (universal binary installer)
└── VS Code Marketplace
```

**Monorepo Organization:**

- Bun workspace management
- Centralized dependency catalog via `bun.lock`
- Key directories:
  - `packages/` - Core packages
  - `infra/` - Infrastructure code
  - `sdks/vscode/` - VS Code extension
  - `specs/` - OpenAPI specifications

**Installation Directory Priority:**

1. `$OPENCODE_INSTALL_DIR` (custom)
2. `$XDG_BIN_DIR` (XDG specification)
3. `$HOME/bin` (if available)
4. `$HOME/.opencode/bin` (default fallback)

### 2.8 Event System and Real-Time Updates

**Event Bus Architecture:**

Cross-component communication via publication/subscription pattern.

**Key Events:**

- Session updates
- Message part changes
- Permission requests
- File modifications
- Server status changes
- Tool execution progress

**Client Integration:**

- SSE (Server-Sent Events) streaming
- Real-time UI updates
- Event subscription management
- Graceful reconnection handling

**Benefits:**

- Responsive developer experience
- Live feedback during long operations
- Parallel client support
- Decoupled component architecture

---

## 3. Key Differentiators

### 3.1 Comparison with Claude Code

**Fundamental Differences:**

| Aspect | OpenCode | Claude Code |
|--------|----------|-------------|
| **License** | Open source (MIT), always free | Proprietary, subscription required |
| **Model Support** | 75+ providers via Models.dev | Primarily Anthropic models |
| **Local Models** | Full support via Ollama, etc. | Limited local model support |
| **Architecture** | Client/server, Go TUI + TypeScript backend | Integrated architecture |
| **Extensibility** | MCP, plugins, custom agents | Limited extensibility |
| **Provider Lock-in** | None - switch freely | Anthropic-focused |
| **Claude Pro Integration** | Yes, via Anthropic login | Native integration |

**Performance:**

- **Claude Code:** 72.7% accuracy on SWE-bench Verified
- **OpenCode with Sonnet-4:** Nearly identical output to Claude Code in testing

**Behavioral Differences (from user testing):**

*Claude Code:*
- More conservative with code changes
- Minor issues: nullable field handling, unconventional defaults
- Best overall experience in testing
- Rarely reformats existing code

*OpenCode:*
- More agentic, takes initiative
- Performs numerous actions without explicit permission
- All tested models attempted unsolicited code reformatting
- OpenCode with Sonnet-4: Removed existing tests (6 removed, 2 added)
- Requires iteration to correct aggressive changes

**Best Use Cases:**

*Choose OpenCode if you prioritize:*
- Cost-effectiveness
- Open-source flexibility
- Wide choice of AI models
- Provider independence
- Extensibility and customization
- Self-hosted/local model support

*Choose Claude Code if you prioritize:*
- Proven high performance on complex tasks
- Polished, integrated experience
- Large-scale, multi-file project comprehension
- Conservative code changes
- Strong codebase context maintenance

### 3.2 Open-Source Nature and MIT License

**Implications:**

1. **No Vendor Lock-in:**
   - Fork and modify as needed
   - Self-host entirely
   - Community-driven development

2. **Transparency:**
   - Audit code for security
   - Understand internal behavior
   - Trust through verification

3. **Extensibility:**
   - Create custom tools and agents
   - Integrate with proprietary systems
   - Build commercial products on top

4. **Community Contributions:**
   - 312 contributors
   - Active development
   - Community-driven features

5. **Cost:**
   - No licensing fees
   - Pay only for AI provider usage
   - Leverage existing subscriptions (Copilot, Claude Pro)

### 3.3 Multi-Provider Support (75+ Providers)

**Strategic Advantage:**

- **Resilience:** No single point of failure
- **Cost Optimization:** Choose cheapest provider per task
- **Performance Tuning:** Best model for each use case
- **Geographic Compliance:** Regional provider requirements
- **Experimentation:** Easy A/B testing of models

**Provider Categories:**

1. **Major Cloud Providers:**
   - Anthropic (Claude)
   - OpenAI (GPT)
   - Google (Gemini)
   - Amazon Bedrock
   - Azure OpenAI

2. **Specialized Providers:**
   - Cohere
   - AI21 Labs
   - Together AI
   - Replicate

3. **Local/Self-Hosted:**
   - Ollama
   - LM Studio
   - vLLM
   - Text Generation WebUI
   - Any OpenAI-compatible endpoint

**Dynamic Provider Management:**

- On-demand SDK installation
- Automatic credential discovery
- Provider-specific optimizations
- Fallback mechanisms

### 3.4 Terminal-First Design Philosophy

**Philosophy:**

Built by neovim enthusiasts for developers who live in the terminal.

**Benefits:**

1. **Lightweight:** Minimal resource footprint
2. **Fast:** Native performance, no browser overhead
3. **Scriptable:** Integrate into shell workflows
4. **Universal:** Works over SSH, in containers, anywhere
5. **Keyboard-Driven:** Efficient for power users
6. **Distraction-Free:** Focus on code, not UI chrome

**Architecture Advantages:**

- **Any IDE Compatible:** Works with VS Code, Cursor, neovim, Emacs, etc.
- **Remote Work:** SSH-friendly, works on remote servers
- **CI/CD Integration:** Non-interactive mode for automation
- **Session Persistence:** Resume work across terminal sessions
- **Multiplexer Friendly:** Works with tmux, screen, zellij

**Non-Interactive Mode:**

```bash
# Single-shot execution
opencode -p "Explain the use of context in Go"

# JSON output for scripting
opencode -p "Find all TODO comments" -f json

# Quiet mode (no spinner)
opencode -p "Run tests" -q
```

**Integration Patterns:**

```bash
# Shell alias
alias ai='opencode -p'

# CI/CD pipeline
opencode -p "Run linter and fix issues" -q

# Git hook
opencode -p "Review changes in last commit"
```

### 3.5 Additional Differentiators

**Privacy-First Architecture:**
- No code storage by OpenCode itself
- Suitable for privacy-sensitive environments
- Data only sent to chosen AI provider
- Local model support for complete privacy

**Session Sharing:**
- Shareable links for collaboration
- Reference sessions for debugging
- Team knowledge sharing
- Cloudflare Workers + Durable Objects infrastructure

**Zen (Premium Offering):**
- Curated model selection
- Tested and benchmarked for coding agents
- Validated performance
- No provider inconsistency concerns
- Handpicked models

**Client/Server Architecture Benefits:**
- Remote operation capability
- Multiple client interfaces
- Mobile app potential
- Web interface option
- Automation script integration

---

## 4. Usage Patterns

### 4.1 Installation and Setup

**Quick Install (Recommended):**

```bash
curl -fsSL https://opencode.ai/install | bash
```

**Package Manager Installation:**

```bash
# npm
npm i -g opencode-ai@latest

# Bun
bun add -g opencode-ai@latest

# pnpm
pnpm add -g opencode-ai@latest

# Yarn
yarn global add opencode-ai@latest

# Homebrew (macOS/Linux)
brew install opencode

# Windows (Scoop)
scoop bucket add extras
scoop install extras/opencode

# Windows (Chocolatey)
choco install opencode

# Arch Linux (Paru)
paru -S opencode-bin

# Universal (mise)
mise use --pin -g ubi:sst/opencode

# NixOS
nix run nixpkgs#opencode
```

**Initial Setup:**

1. **Start OpenCode:**
   ```bash
   opencode
   ```

2. **Provider Authentication:**
   - Interactive TUI prompts for provider selection
   - Guided authentication workflow
   - Supports multiple providers simultaneously

3. **Project Initialization:**
   ```bash
   /init
   ```

4. **Configure via Environment Variables:**
   ```bash
   export ANTHROPIC_API_KEY="your-key"
   export OPENAI_API_KEY="your-key"
   ```

5. **Or via auth.json:**
   ```json
   // ~/.opencode/data/auth.json
   {
     "anthropic": {
       "apiKey": "your-key"
     },
     "openai": {
       "apiKey": "your-key"
     }
   }
   ```

### 4.2 Common Workflows

**1. Exploring Unfamiliar Codebases:**

```
1. Start OpenCode in project directory
2. Switch to Plan agent (Tab key)
3. Ask: "Explain the architecture of this codebase"
4. Follow up: "Show me how authentication works"
5. Reference specific files: "@src/auth.ts explain this"
```

**2. Feature Implementation:**

```
1. Use Plan agent to create implementation plan
2. Ask: "Plan how to add user profile feature"
3. Review and refine plan
4. Switch to Build agent (Tab key)
5. Ask: "Implement the plan we just created"
6. Monitor tool executions and file changes
7. Test and iterate
```

**3. Bug Fixing:**

```
1. Build agent (default)
2. Describe bug: "Users can't log in with OAuth"
3. Agent investigates: reads logs, checks config, examines code
4. Review proposed fix
5. Approve file changes
6. Run tests: "Run the auth test suite"
```

**4. Code Review and Refactoring:**

```
1. Plan agent for analysis
2. Ask: "@src/services/ review this code for improvements"
3. Agent identifies issues, suggests refactoring
4. Switch to Build agent
5. Ask: "Apply the refactoring suggestions"
6. Review changes with git diff
```

**5. Multi-Step Complex Tasks:**

```
1. Build agent
2. Use @general subagent: "@general research best practices for GraphQL caching"
3. General agent performs research
4. Build agent uses research results
5. Implement solution based on findings
```

**6. Documentation Generation:**

```
1. Plan agent
2. Ask: "Generate API documentation for @src/api/"
3. Review generated docs
4. Build agent: "Create README.md with the documentation"
```

**7. Test Writing:**

```
1. Build agent
2. Ask: "Write tests for @src/utils/validation.ts"
3. Agent creates test file
4. Run tests: "Run the new tests"
5. Iterate on failures
```

### 4.3 Best Practices

**Agent Selection:**

- **Start with Plan:** For exploration and understanding
- **Switch to Build:** When ready to make changes
- **Use @general:** For research and multi-step analysis

**File References:**

- Use `@` to reference files: `@src/app.ts`
- Reference specific lines: `@File#L37-42`
- Reference directories: `@src/components/`
- Drag images into terminal for visual context

**Permission Management:**

```json
// .opencode/opencode.json
{
  "permission": "ask",  // Global default
  "agent": {
    "trusted": {
      "permission": {
        "edit": "allow",
        "bash": "ask",
        "webfetch": "allow"
      }
    },
    "safe": {
      "permission": {
        "edit": "deny",
        "bash": "deny",
        "webfetch": "allow"
      }
    }
  }
}
```

**Session Management:**

- Use `/sessions` to view and resume sessions
- Share sessions for collaboration: shareable links
- Use `/undo` to revert changes
- Use `/redo` to reapply changes

**Configuration Organization:**

```
.opencode/
├── opencode.json          # Main configuration
├── agent/
│   ├── backend.md         # Backend specialist agent
│   ├── frontend.md        # Frontend specialist agent
│   └── devops/
│       └── docker.md      # Docker specialist
├── command/
│   ├── review.md          # Custom /review command
│   └── test.md            # Custom /test command
└── instructions/
    └── coding-style.md    # Project guidelines
```

**Custom Agents:**

```markdown
<!-- .opencode/agent/backend.md -->
---
model: anthropic/claude-sonnet-4
temperature: 0.3
permission:
  edit: allow
  bash: ask
mode: primary
description: Backend development with Python and PostgreSQL
---

You are a backend development specialist. Focus on:
- Python best practices and type hints
- PostgreSQL query optimization
- API design and RESTful principles
- Error handling and logging
- Security considerations
```

**Rules and Guidelines:**

```markdown
<!-- .opencode/instructions/typescript-guidelines.md -->

## TypeScript Guidelines

- Always use strict mode
- Prefer interfaces over types for object shapes
- Use const assertions for literal types
- Avoid `any` - use `unknown` instead
- Document complex types with JSDoc
```

Reference in prompts: `@instructions/typescript-guidelines.md`

### 4.4 Command-Line Interface

**Basic Usage:**

```bash
# Start TUI (default)
opencode

# Start with specific agent
opencode build
opencode plan

# Non-interactive mode
opencode -p "prompt here"

# JSON output
opencode -p "prompt" -f json

# Quiet mode (no spinner)
opencode -p "prompt" -q
```

**Key Commands (within TUI):**

- `/init` - Initialize project with OpenCode
- `/sessions` - List and manage sessions
- `/undo` - Revert last changes
- `/redo` - Reapply reverted changes
- Custom commands from `.opencode/command/*.md`

**Agent Commands:**

```bash
# Create new agent
opencode agent

# Interactive agent creation wizard
# - Custom system prompts
# - Tool configuration
# - Permission settings
```

**Authentication:**

```bash
# Login with provider
opencode auth login

# Configure API keys
# Interactive credential management
```

**Custom Commands:**

Create `.opencode/command/review.md`:

```markdown
---
description: Review code for best practices
---

Review the following code for:
- Security issues
- Performance problems
- Code style violations
- Missing tests
```

Use: `/review @src/app.ts`

### 4.5 Workflow Orchestration

**Multi-Agent Workflows:**

OpenCode supports complex workflows with specialized agents:

```
Task Manager Agent
  ↓ plans and breaks down
Coder Agent
  ↓ implements changes
Tester Agent
  ↓ creates and runs tests
Documentation Agent
  ↓ updates docs
Quality Agent
  ↓ reviews code quality
Reviewer Agent
  ↓ final validation
```

**Custom Workflow Example:**

```bash
# Custom workflow chain
opencode workflow --chain "plan,coder-agent,tester"
```

**Agent Project Structure:**

```
.opencode/
├── agents/
│   └── workflow-orchestrator.md
└── subagents/
    ├── mermaid-extractor.prompt
    ├── image-generator.prompt
    ├── markdown-rebuilder.prompt
    ├── pandoc-converter.prompt
    ├── pipeline-validator.prompt
    └── utf-sanitizer.prompt
```

**Self-Healing Documentation Example:**

From community usage patterns:

1. **Pipeline Validator** - Checks documentation integrity
2. **UTF Sanitizer** - Fixes encoding issues
3. **Mermaid Extractor** - Extracts diagrams
4. **Image Generator** - Creates visualizations
5. **Markdown Rebuilder** - Reconstructs documentation
6. **Workflow Orchestrator** - Coordinates all agents

### 4.6 Integration Patterns

**CI/CD Integration:**

```yaml
# .github/workflows/opencode-review.yml
name: OpenCode Review
on: [pull_request]
jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Install OpenCode
        run: curl -fsSL https://opencode.ai/install | bash
      - name: Review PR
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
        run: |
          opencode -p "Review the changes in this PR for security and performance issues" -q
```

**Shell Aliases:**

```bash
# ~/.bashrc or ~/.zshrc
alias ai='opencode -p'
alias aifix='opencode -p "Fix linting errors in current directory"'
alias aitest='opencode -p "Write tests for the current file"'
alias aiexplain='opencode -p'
```

Usage:
```bash
ai "Explain what this code does"
aifix
aitest
```

**Git Hooks:**

```bash
# .git/hooks/pre-commit
#!/bin/bash
opencode -p "Review staged changes and ensure no security issues" -q
```

**VS Code Workflow:**

1. Write code in VS Code
2. Hit `Cmd+Esc` to open OpenCode
3. Ask questions or request changes
4. OpenCode makes edits
5. Changes immediately visible in VS Code
6. Review and continue

### 4.7 Advanced Usage

**Context Awareness:**

```bash
# Attach images for context
# Drag image files into terminal
# OpenCode processes visual information

# Reference specific line ranges
@src/app.ts#L10-25

# Reference entire directories
@src/components/
```

**Model Selection Per Task:**

```json
{
  "agent": {
    "fast": {
      "model": "anthropic/claude-haiku-4",
      "description": "Quick questions and simple tasks"
    },
    "deep": {
      "model": "anthropic/claude-sonnet-4",
      "description": "Complex reasoning and refactoring"
    },
    "code": {
      "model": "openai/gpt-4",
      "description": "Code generation with GPT-4"
    }
  }
}
```

**MCP Server Integration:**

```json
{
  "mcp": {
    "web-search": {
      "type": "remote",
      "url": "https://api.brightdata.com/mcp",
      "headers": {
        "Authorization": "Bearer {env:BRIGHTDATA_TOKEN}"
      }
    },
    "database": {
      "type": "local",
      "command": "node",
      "args": ["./mcp-servers/database.js"],
      "env": {
        "DB_URL": "{env:DATABASE_URL}"
      }
    }
  }
}
```

**Custom LSP Servers:**

```json
{
  "lsp": {
    "customlang": {
      "extensions": [".customlang"],
      "command": "customlang-lsp",
      "args": ["--stdio"]
    }
  }
}
```

---

## 5. GitHub Repository Structure

### 5.1 Repository Organization

**Main Repository:** https://github.com/sst/opencode

**Key Directories:**

```
sst/opencode/
├── packages/              # Core packages
│   ├── opencode/         # Main backend (TypeScript/Bun)
│   │   ├── src/
│   │   │   ├── tool/     # Tool implementations
│   │   │   ├── provider/ # AI provider integrations
│   │   │   ├── agent/    # Agent system
│   │   │   ├── session/  # Session management
│   │   │   ├── lsp/      # LSP integration
│   │   │   └── mcp/      # MCP integration
│   │   └── package.json
│   ├── sdk/              # JavaScript/TypeScript SDK
│   └── plugin/           # Plugin SDK
├── sdks/
│   └── vscode/           # VS Code extension
├── specs/                # OpenAPI specifications
├── infra/                # Infrastructure code (Cloudflare Workers)
├── cmd/
│   └── opencode/         # Go TUI implementation
├── go.mod                # Go dependencies
├── package.json          # Root package.json
├── bun.lock             # Bun lockfile
└── README.md
```

### 5.2 Documentation

**Official Documentation:** https://opencode.ai/docs/

**Structure:**

```
Documentation
├── Getting Started
│   └── Intro (installation, setup, basics)
├── Usage
│   ├── TUI (Terminal UI features)
│   ├── CLI (Command-line interface)
│   ├── IDE (VS Code, Cursor integration)
│   ├── Zen (Curated models)
│   ├── Share (Session sharing)
│   ├── GitHub Integration
│   └── GitLab Integration
├── Configuration
│   ├── Tools
│   ├── Rules
│   ├── Agents
│   ├── Models
│   ├── Themes
│   ├── Keybindings
│   ├── Commands
│   ├── Formatters
│   ├── Permissions
│   ├── LSP Servers
│   ├── MCP Servers
│   ├── ACP Support
│   └── Custom Tools
└── Development
    ├── SDK
    ├── Server
    └── Plugin
```

**Additional Resources:**

- **DeepWiki Documentation:** https://deepwiki.com/sst/opencode
  - Comprehensive community-maintained documentation
  - Technical deep dives
  - Architecture explanations
  - Configuration examples

- **Discord Community:** Active community for support and discussion

### 5.3 Release Cadence

**Statistics:**
- 523 releases
- Active development since June 2025
- Consistent daily downloads: 10,000+
- Total downloads: 1.1M+ (GitHub + npm)

**Update Mechanism:**
- Auto-update support (configurable)
- Manual update via package manager
- Development branch access via Nix flake

---

## 6. Community and Ecosystem

### 6.1 Community Metrics

**GitHub Presence:**
- 33,400+ stars
- 2,700+ forks
- 312 contributors
- 4,267 commits on development branch
- Active issues and pull requests

**User Base:**
- ~300,000 monthly developers
- Growing daily adoption
- 10,000+ daily downloads

**Repository Status:**
Note: A separate repository (`opencode-ai/opencode`) was archived on July 29, 2025. The main active repository is `sst/opencode`.

### 6.2 Ecosystem Extensions

**VS Code Extension:**
- Marketplace: `sst-dev.opencode`
- Community extensions (unofficial): `bascodes.opencode-inofficial`

**MCP Server Ecosystem:**
- Bright Data Web MCP
- Community-developed MCP servers
- Growing third-party integration

**Plugin Ecosystem:**
- Custom tool development via `@opencode-ai/plugin`
- Community-shared agents and configurations
- GitHub Gists with configurations

### 6.3 Learning Resources

**Official:**
- Main documentation: https://opencode.ai/docs/
- GitHub README: Comprehensive setup guide
- DeepWiki: https://deepwiki.com/sst/opencode

**Community Content:**
- Medium articles on workflows
- Tutorial blogs (freeCodeCamp, DEV.to)
- Video tutorials
- Configuration examples on GitHub Gists

**Example Resources:**
- "How to Integrate AI into Your Terminal Using OpenCode" (freeCodeCamp)
- "OpenCode: Open Source Claude Code Alternative" (multiple sources)
- "How Coding Agents Actually Work: Inside OpenCode" (technical deep dive)
- GitHub discussions and issue threads

---

## 7. Technical Deep Dive: How It Works

### 7.1 The LLM Loop in Detail

**Step-by-Step Flow:**

1. **Input Assembly:**
   ```
   User Prompt
   + Conversation History
   + System Prompt (agent-specific)
   + Available Tools (with descriptions)
   + File Context (LSP diagnostics, git state)
   → Assembled into AI SDK format
   ```

2. **LLM Processing:**
   ```
   Provider-agnostic AI SDK call
   → Model analyzes context
   → Decides on tool invocations or text response
   → Returns tool calls or message
   ```

3. **Tool Execution:**
   ```
   For each tool call:
     → Registry lookup
     → Permission check
     → User confirmation (if "ask")
     → Execute tool implementation
     → Capture result
   ```

4. **Context Update:**
   ```
   Tool results
   + Previous context
   → Added to conversation history
   → MessageV2 format with structured parts
   ```

5. **Loop Continuation:**
   ```
   If model stopped: End
   If context limit reached: Summarize and continue
   Else: Go to step 2
   ```

### 7.2 Grounding Mechanisms

**LSP Diagnostics:**
- Real-time error detection prevents hallucinations
- Agent aware of syntax/type errors immediately
- Feedback loop: Edit → Diagnose → Fix → Verify

**Git Snapshots:**
- State preservation before risky operations
- Rollback capability on failure
- Change tracking for transparency

**Binary File Detection:**
- Prevents corruption of non-text files
- Safety checks before read/write operations

**Context Limits:**
- Models.dev provides accurate limits per model
- Automatic summarization before hitting limits
- Preserves critical information while reducing tokens

### 7.3 Session Persistence

**Storage:**
- SQLite database for session data
- XDG-compliant directory structure
- MessageV2 format for conversation history

**Features:**
- Per-directory singleton pattern (one session per project)
- Parent-child relationships for branching
- Session sharing via Cloudflare Workers + Durable Objects
- Automatic compaction when approaching limits

**Resumption:**
- Sessions persist across terminal sessions
- `/sessions` command lists available sessions
- Automatic loading of previous session in same directory

---

## 8. Practical Considerations

### 8.1 Cost Management

**Provider Flexibility:**
- Use cheaper models for simple tasks (Haiku)
- Reserve expensive models for complex reasoning (Opus)
- Leverage existing subscriptions (Claude Pro via Anthropic login)
- Local models for zero API cost

**Token Optimization:**
- Automatic summarization reduces context bloat
- Efficient tool descriptions
- Strategic file referencing instead of full codebase context

### 8.2 Security and Privacy

**Privacy-First Design:**
- No code storage by OpenCode itself
- Data only sent to chosen AI provider
- Local model support for complete privacy
- Open-source auditability

**Permission System:**
- Granular control over tool execution
- Confirmation prompts for risky operations
- Agent-specific permission policies
- Per-tool permission configuration

**Credential Management:**
- Environment variables
- Encrypted auth.json
- Custom credential loaders
- No credential leakage to logs

### 8.3 Performance Characteristics

**TUI Performance:**
- Native Go binary: fast startup
- Minimal resource footprint
- Efficient SSE streaming
- Responsive UI during long operations

**Backend Performance:**
- Bun runtime: faster than Node.js
- HTTP/SSE architecture: scalable
- SQLite: fast session persistence
- Tool execution: parallelizable

**Model Performance:**
- Provider-dependent
- OpenCode overhead: minimal
- Network latency: main bottleneck
- Streaming responses: better perceived performance

### 8.4 Limitations and Known Issues

**From User Testing:**
- Tendency to reformat existing code without permission
- Occasional aggressive changes (removing tests, etc.)
- Requires iteration to correct overreach
- Model quality varies significantly by provider

**Technical Constraints:**
- Context window limits (model-dependent)
- Network dependency for cloud models
- Permission dialogs can interrupt flow
- LSP server availability varies by language

**Best Practices to Mitigate:**
- Start with Plan agent for exploration
- Use "ask" permission mode for critical operations
- Review changes before accepting
- Test changes incrementally
- Use version control (git)

---

## 9. Comparison Summary

### OpenCode Strengths

✅ **Open source and free** - No licensing costs, MIT license
✅ **Provider agnostic** - 75+ providers, no vendor lock-in
✅ **Local model support** - Complete privacy option
✅ **Extensible** - MCP, plugins, custom agents
✅ **Terminal-first** - Fast, lightweight, SSH-friendly
✅ **Client/server architecture** - Flexible integration
✅ **Active development** - 300+ contributors, regular releases
✅ **Privacy-focused** - No code storage
✅ **Session sharing** - Collaboration-friendly
✅ **Multi-session support** - Parallel workflows

### OpenCode Weaknesses

⚠️ **More aggressive changes** - Tendency to reformat without asking
⚠️ **Model quality variance** - Depends on provider choice
⚠️ **Less polished** - Compared to commercial alternatives
⚠️ **Newer project** - Less battle-tested than Claude Code
⚠️ **Configuration complexity** - More options = steeper learning curve

### Claude Code Strengths

✅ **Proven performance** - 72.7% SWE-bench Verified accuracy
✅ **Conservative changes** - Rarely reformats without reason
✅ **Polished experience** - Integrated and refined
✅ **Strong context handling** - Excellent multi-file comprehension
✅ **Battle-tested** - Mature and stable

### Claude Code Weaknesses

⚠️ **Proprietary and paid** - Subscription required
⚠️ **Vendor lock-in** - Primarily Anthropic models
⚠️ **Limited extensibility** - Closed ecosystem
⚠️ **Less flexible** - One provider, one approach

---

## 10. Use Case Recommendations

### When to Choose OpenCode

**Best For:**

1. **Cost-Conscious Development:**
   - Startups and individual developers
   - Leveraging existing AI subscriptions
   - Using local models for free operation

2. **Multi-Provider Workflows:**
   - A/B testing different models
   - Geographic compliance requirements
   - Provider resilience strategies

3. **Extensibility Needs:**
   - Custom tool development
   - Domain-specific agents
   - Integration with proprietary systems

4. **Privacy-Sensitive Projects:**
   - Enterprise with data restrictions
   - Government/regulated industries
   - Intellectual property concerns

5. **Terminal-First Developers:**
   - Neovim/Emacs users
   - Remote development via SSH
   - DevOps and infrastructure work

6. **Open-Source Advocates:**
   - Need to audit code
   - Want to contribute
   - Prefer transparency

### When to Choose Claude Code

**Best For:**

1. **Enterprise Development:**
   - Budget for subscriptions
   - Need proven track record
   - Large-scale, complex codebases

2. **Mission-Critical Projects:**
   - High accuracy requirements
   - Conservative change approach preferred
   - Mature tooling needs

3. **Claude-Focused Workflows:**
   - Already invested in Claude ecosystem
   - Prefer Anthropic's models
   - Don't need multi-provider flexibility

---

## 11. Future Outlook

### Current Trajectory

**Active Development:**
- Nearly 4,000 commits
- 523 releases
- Growing contributor base
- Consistent feature additions

**Adoption Growth:**
- 10,000+ daily downloads
- 300,000 monthly developers
- Increasing GitHub stars
- Expanding ecosystem

### Potential Evolution

**Likely Developments:**

1. **Improved UX:**
   - Less aggressive default behavior
   - Better change confirmation workflows
   - Enhanced permission system

2. **Ecosystem Growth:**
   - More MCP servers
   - Community plugins
   - Pre-configured agents for common tasks

3. **Performance Enhancements:**
   - Faster tool execution
   - Better context management
   - Optimized provider integrations

4. **Platform Expansion:**
   - Web interface
   - Mobile clients
   - IDE plugins beyond VS Code

5. **Enterprise Features:**
   - Team collaboration tools
   - Usage analytics
   - Compliance features

---

## 12. Conclusion

OpenCode represents a significant open-source alternative in the AI coding agent space. Its provider-agnostic architecture, terminal-first design, and extensibility make it a compelling choice for developers who value flexibility, privacy, and cost control.

While it may be more aggressive in making changes compared to Claude Code and has a steeper learning curve due to its extensive configuration options, these characteristics also reflect its strengths: it's highly customizable, adaptable, and designed for power users who want control over every aspect of their AI coding assistant.

For teams and individuals who:
- Want to avoid vendor lock-in
- Need multi-provider support
- Prefer open-source solutions
- Work primarily in terminal environments
- Require extensibility and customization
- Have privacy or compliance requirements

**OpenCode is an excellent choice.**

For those who prioritize polish, proven performance on complex tasks, and are comfortable with a single-provider solution, Claude Code may remain the better option—but OpenCode is rapidly closing the gap.

---

## 13. Additional Resources

### Documentation
- **Official Docs:** https://opencode.ai/docs/
- **DeepWiki:** https://deepwiki.com/sst/opencode
- **GitHub:** https://github.com/sst/opencode

### Community
- **Discord:** Active community for support
- **GitHub Discussions:** Feature requests and Q&A
- **GitHub Issues:** Bug reports and tracking

### SDKs and Development
- **JavaScript SDK:** `@opencode-ai/sdk`
- **Go SDK:** `github.com/sst/opencode-sdk-go`
- **Plugin SDK:** `@opencode-ai/plugin`
- **OpenAPI Spec:** Available in repository

### Learning
- freeCodeCamp: "How to Integrate AI into Your Terminal Using OpenCode"
- Medium: Multiple in-depth articles and workflows
- DEV.to: Community tutorials and comparisons
- Technical deep dives: Architecture and implementation details

### Configuration Examples
- GitHub Gists: Community-shared configurations
- Repository examples: Sample agent and command definitions
- DeepWiki: Configuration patterns and best practices

---

**Report Compiled:** November 2025
**Based on:** Website analysis, GitHub repository research, community resources, technical documentation, and user testimonials.

---
