# Google Gemini Code Assist: Custom Configuration Research Report

**Research Date:** November 20, 2025
**Focus:** Custom configuration, project-specific behavior, commands, and standardization

---

## Executive Summary

Google's Gemini Code Assist offers extensive customization capabilities through multiple configuration mechanisms. The platform supports both IDE extensions (VS Code, JetBrains, Android Studio) and a powerful CLI tool, each with distinct configuration approaches. Recent 2025 updates introduced personalization features including custom commands, rules, and enhanced context management powered by the Gemini 2.5 model.

Key findings:
- **Project-level configuration** through `.gemini/` folder with YAML and Markdown files
- **Custom slash commands** using TOML format for CLI
- **IDE-specific customization** through rules and custom commands
- **MCP (Model Context Protocol)** integration for extensibility
- **Enterprise features** for codebase-specific training and indexing
- **Limited cross-platform standardization** (platform-specific formats dominate)

---

## 1. Project Configuration Mechanisms

### 1.1 Repository-Level Configuration (.gemini/ folder)

Gemini Code Assist supports a `.gemini/` folder at the repository root for project-wide configuration.

#### config.yaml

**Purpose:** Controls features, file exclusions, and code review behavior

**Location:** `.gemini/config.yaml`

**Schema:**
```yaml
# Enable fun features (poems, creative responses)
have_fun: false

# Memory configuration
memory_config:
  disabled: false

# Code review settings
code_review:
  disable: false
  comment_severity_threshold: MEDIUM  # Options: LOW, MEDIUM, HIGH, CRITICAL
  max_review_comments: -1  # -1 = unlimited
  pull_request_opened:
    help: false
    summary: true
    code_review: true
    include_drafts: true

# File patterns to ignore (glob format)
ignore_patterns: []
```

**Key Features:**
- **Severity filtering:** Control which code review comments appear based on importance
- **PR automation:** Configure automatic summaries and reviews on PR creation
- **File exclusions:** Use glob patterns to exclude specific files/folders
- **Configuration precedence:** Repository config overrides group/organization settings

#### styleguide.md

**Purpose:** Natural language coding conventions and best practices

**Location:** `.gemini/styleguide.md`

**Format:** Free-form Markdown (no strict schema)

**Usage:**
- Describe project-specific coding standards
- Define preferred libraries and frameworks
- Specify documentation requirements
- Set naming conventions
- Outline architectural patterns

**Example content:**
```markdown
# Project Style Guide

## Code Standards
- Use TypeScript strict mode for all new files
- Follow functional programming patterns where possible
- Prefer async/await over promise chains

## Documentation
- All public functions must have JSDoc comments
- Include @example tags for complex functions

## Testing
- Minimum 80% code coverage required
- Use Jest for unit tests, Playwright for E2E
```

**Configuration Hierarchy:**
- Repository `styleguide.md` combines with organization-level style guides
- Both are considered during code review and generation
- Repository settings take precedence in case of conflicts

### 1.2 Context Exclusion (.aiexclude)

**Purpose:** Exclude files from local codebase indexing

**Location:** `.aiexclude` (workspace root by default)

**Format:** Similar to `.gitignore` syntax

**Features:**
- Automatically respects `.gitignore` patterns
- Custom path configurable in VS Code settings: `Context Exclusion File`
- Affects code completion, generation, transformation, and chat context

**Example:**
```
# Build artifacts
dist/
build/
*.min.js

# Sensitive files
.env
secrets.yaml
*.key

# Large data files
data/
*.csv
*.db
```

---

## 2. Custom Commands & Skills

### 2.1 Gemini CLI Slash Commands

Gemini CLI supports custom slash commands through TOML configuration files, providing reusable prompts for streamlined workflows.

#### Command Storage Locations

**User-scoped (global):**
- Location: `~/.gemini/commands/`
- Availability: All projects for the current user
- Use case: Personal productivity commands

**Project-scoped:**
- Location: `.gemini/commands/`
- Availability: Only within the specific project
- Use case: Team-shared, project-specific workflows
- Recommendation: Check into version control for team distribution

#### TOML File Structure

**Minimal format:**
```toml
prompt = "Your instruction to Gemini"
```

**Complete format:**
```toml
description = "Brief one-line description shown in /help menu"
prompt = """
Multi-line prompt with detailed instructions.
Supports {{args}} for dynamic user input.
Can execute shell commands with !{command}.
"""
```

#### Command Naming & Namespacing

**File path determines command name:**
- `commands/test.toml` → `/test`
- `commands/review.toml` → `/review`
- `commands/git/commit.toml` → `/git:commit` (namespaced)
- `commands/db/migrate.toml` → `/db:migrate`

**Rules:**
- Command names are **case-sensitive**
- Subdirectories create namespaces using colon (`:`) separator
- No spaces or special characters in filenames

#### Dynamic Arguments with {{args}}

The `{{args}}` placeholder is replaced with user-provided text:

```toml
description = "Generate unit tests for a function"
prompt = """
Create comprehensive unit tests for the following code:

{{args}}

Include edge cases and error scenarios.
"""
```

**Usage:** `/test function calculateTotal(items) { ... }`

#### Shell Command Injection with !{...}

Execute shell commands and inject output directly into prompts:

```toml
description = "Generate a Git commit message from staged changes"
prompt = """
Analyze the following staged changes and generate a concise commit message:

!{git diff --staged}

Follow conventional commit format (feat/fix/docs/etc).
"""
```

**Features:**
- Automatic shell escaping when `{{args}}` used inside `!{...}`
- Commands execute in the current working directory
- Output is captured and inserted into the prompt

#### Complete Example: Code Review Command

**File:** `.gemini/commands/review.toml`

```toml
description = "Review a pull request based on GitHub issue number"
prompt = """
Review the pull request for GitHub issue: {{args}}

Steps:
1. Fetch PR details: !{gh pr view {{args}} --json title,body,files}
2. Read modified files using available tools
3. Check against project conventions in styleguide.md
4. Analyze for:
   - Code quality and maintainability
   - Security vulnerabilities
   - Performance issues
   - Test coverage
   - Documentation completeness

Provide structured feedback with:
- Summary of changes
- Critical issues (must fix)
- Suggestions (nice to have)
- Positive highlights
"""
```

**Usage:** `/review 123`

### 2.2 IDE Extension Custom Commands

Custom commands are also available in IDE extensions (VS Code, JetBrains) with a different configuration approach.

#### VS Code Configuration

**Access:**
1. Open Settings (Ctrl+Shift+P / Cmd+Shift+P)
2. Search for "Gemini Code Assist"
3. Navigate to "Custom Commands" section
4. Add commands through UI

**Usage:**
- Open Quick Pick menu (Ctrl+I / Cmd+I)
- Select "Custom Commands"
- Choose or create a command

**Built-in commands:**
- `/fix` - Fix code issues
- `/generate` - Generate new code
- `/doc` - Add documentation
- `/simplify` - Simplify code

#### JetBrains IDE Configuration

**Access:**
1. Settings > Tools > Gemini > Prompt Library
2. Add custom commands
3. Configure scope (IDE-level or Project-level)

**Scopes:**
- **IDE-level:** Private to user, available across projects
- **Project-level:** Shared with team, project-specific

### 2.3 MCP Prompts as Slash Commands

Gemini CLI supports Model Context Protocol (MCP) prompts as slash commands, providing seamless integration with MCP servers.

**Features:**
- MCP prompt name becomes the command name
- MCP prompt description shows in help menu
- Arguments supported: `--<arg_name>="value"` or positional
- Listed in `/mcp` command output

**Example usage:**
```bash
# If an MCP server provides a "summarize" prompt
/summarize --file="README.md" --length="short"
```

---

## 3. IDE Rules System

The Rules system allows natural language instructions that guide Gemini's behavior across all interactions.

### 3.1 VS Code Rules

**Access:**
1. Command Palette (Ctrl+Shift+P / Cmd+Shift+P)
2. "Preferences: Open Settings (UI)"
3. Search: "Geminicodeassist: Rules"

**Configuration:**
- Multi-line text field
- One rule per line
- Applied to all prompts and requests

**Example rules:**
```
Always generate unit tests when creating a new function
Use TypeScript strict mode for type definitions
Prefer functional components over class components in React
Follow the repository's ESLint configuration
Include error handling in all API calls
Add JSDoc comments for public functions
```

### 3.2 JetBrains Rules

**Access:** Settings > Tools > Gemini > Prompt Library > Rules

**Scopes:**
- **IDE scope:** Personal rules, all projects
- **Project scope:** Team-shared, version-controlled

**Features:**
- Scope selector for organizing rules
- Project-level rules can be committed to repository
- Same multi-line format as VS Code

### 3.3 Rules vs. styleguide.md

| Feature | Rules (IDE) | styleguide.md (Repository) |
|---------|------------|----------------------------|
| **Scope** | IDE-wide or project-wide | Repository-wide |
| **Format** | Plain text lines | Markdown document |
| **Storage** | IDE settings / `.gemini/` | `.gemini/styleguide.md` |
| **Purpose** | Immediate behavior guidance | Comprehensive style guide |
| **Applies to** | All prompts and generations | Code reviews and context |
| **Version control** | Optional (project scope) | Required (in repository) |

**Recommendation:** Use Rules for immediate constraints, styleguide.md for comprehensive documentation.

---

## 4. Enterprise Features: Code Customization

Gemini Code Assist Enterprise offers advanced customization based on your organization's private codebase.

### 4.1 Repository Indexing

**Purpose:** Train Gemini on organization-specific code patterns

**Process:**
1. Configure remote repositories for indexing
2. Gemini analyzes and parses repository structure
3. Index used for contextually aware suggestions

**Benefits:**
- Code suggestions aligned with organizational style
- Faster lookups within your codebase
- Better understanding of internal libraries and frameworks

### 4.2 Repository Context Selection

**Usage:** Type `@` in chat prompt to select repositories

**Features:**
- Select one or more indexed repositories as context
- Focus suggestions on specific microservices or modules
- Get relevant code based on your current task

**Example:**
```
@backend-api @shared-utils How do I authenticate API requests?
```

### 4.3 Code Customization Benefits

- **Aligned suggestions:** Code matches your team's patterns
- **Faster development:** Less manual correction needed
- **Knowledge retention:** Captures organizational best practices
- **Consistency:** Standardized approach across team members

**Security & Privacy:**
- Source code stored in isolated Google Cloud managed project
- No training of foundation Gemini model with private data
- Full control over indexed repositories
- Data can be purged at any time

---

## 5. MCP (Model Context Protocol) Integration

### 5.1 Overview

Gemini Code Assist supports MCP for extensibility, allowing integration with external tools and data sources.

**MCP Capabilities:**
- Connect to local or remote MCP servers
- Access external APIs and databases
- Integrate with development tools (GitHub, Slack, etc.)
- Extend agent capabilities with custom tools

### 5.2 Configuration Format (settings.json)

**Location:**
- Global: `~/.gemini/settings.json`
- Project: `.gemini/settings.json`

**Structure:**
```json
{
  "mcpServers": {
    "server-name": {
      "command": "executable-path",
      "args": ["arg1", "arg2"],
      "env": {
        "ENV_VAR": "value"
      },
      "timeout": 30000,
      "includeTools": ["tool1", "tool2"],
      "excludeTools": ["tool3"]
    },
    "http-server": {
      "httpUrl": "https://api.example.com/mcp",
      "headers": {
        "Authorization": "Bearer token"
      },
      "timeout": 30000
    },
    "sse-server": {
      "url": "https://events.example.com/stream"
    }
  }
}
```

### 5.3 Transport Types

**1. Stdio (Local Process)**
```json
{
  "git": {
    "command": "uvx",
    "args": ["mcp-server-git", "--repository", "."]
  }
}
```

**2. HTTP**
```json
{
  "github": {
    "httpUrl": "https://api.github.com/mcp",
    "headers": {
      "Authorization": "Bearer ghp_xxxxx"
    }
  }
}
```

**3. SSE (Server-Sent Events)**
```json
{
  "realtime": {
    "url": "https://events.example.com/mcp-stream"
  }
}
```

### 5.4 Built-in MCP Commands

**In Gemini CLI:**
- `/tools` - Display available tools from MCP servers
- `/mcp` - List configured servers and connection status

**Features:**
- Tool filtering with `includeTools`/`excludeTools`
- Authentication via headers or environment variables
- Timeout configuration for reliability
- Automatic reconnection on failure

---

## 6. Local Codebase Awareness

### 6.1 Automatic Indexing

**Features:**
- Enabled by default
- Indexes workspace files for context
- Improves relevance of suggestions and responses

**Applies to:**
- Code completion
- Code generation
- Code transformation
- Chat responses

### 6.2 Manual Context Selection

**Usage:** Type `@` in chat to select specific files

**Benefits:**
- Include only relevant files in context
- Reduce token usage
- More focused responses

**Example:**
```
@src/auth.ts @src/middleware.ts How can I add rate limiting?
```

### 6.3 Context Management

**Best practices:**
- Use `.aiexclude` to exclude large or irrelevant files
- Select specific files with `@` for focused queries
- Leverage repository indexing (Enterprise) for large codebases
- Combine local and remote repository context

---

## 7. Agent Mode Features

Gemini Code Assist offers an "Agent Mode" that expands capabilities beyond simple chat interactions.

### 7.1 Capabilities

**Multi-file edits:**
- Modify multiple files in a single operation
- Maintain consistency across changes

**Full project context:**
- Understands entire codebase structure
- Makes informed architectural decisions

**Built-in tools:**
- File read/write
- Grep search
- Code analysis
- Test execution

**MCP integration:**
- Access external tools and services
- Execute custom workflows
- Integrate with CI/CD pipelines

**Human-in-the-loop:**
- Review suggested changes before applying
- Approve or reject individual modifications
- Iterative refinement

### 7.2 Usage

**Activation:**
- Available in IDE extensions and CLI
- Triggered by complex, multi-step tasks
- Automatic context gathering

**Example workflow:**
```
User: "Add authentication to the API using JWT tokens"

Agent:
1. Analyzes current API structure
2. Identifies files requiring modification
3. Proposes changes across:
   - Auth middleware
   - User routes
   - Database models
   - Configuration files
4. Presents diff view for review
5. Applies changes on approval
6. Suggests tests and documentation updates
```

---

## 8. Cross-Platform Configuration Standards

### 8.1 Gemini-Specific Formats

Gemini Code Assist uses platform-specific configuration formats:

| Format | Purpose | Portability |
|--------|---------|-------------|
| `.gemini/config.yaml` | Repository settings | Gemini-specific |
| `.gemini/styleguide.md` | Coding conventions | Readable by any tool |
| `.gemini/commands/*.toml` | CLI slash commands | Gemini CLI only |
| `~/.gemini/settings.json` | User preferences, MCP | Gemini-specific |
| `.aiexclude` | Context exclusion | Potentially shareable |

### 8.2 Comparison with Other AI Assistants

**GitHub Copilot:**
- Configuration: `.github/copilot-instructions.md`
- Format: Markdown (natural language)
- Scope: Repository-wide
- Features: Custom instructions only (no commands)

**Cursor:**
- Configuration: `.cursor/rules` (current) or `.cursorrules` (legacy)
- Format: Individual rule files or single file
- Scope: Project-wide
- Features: Project rules, workspace configuration

**Claude Code (this platform):**
- Configuration: `.claude/`, `CLAUDE.md`
- Format: Markdown for instructions, TOML for commands
- Scope: Project and user-level
- Features: Slash commands, skills, instructions

### 8.3 Emerging Standards

**Limited standardization exists:**
- No unified format across platforms
- Each tool has proprietary configuration
- Markdown instructions most portable

**Emerging initiatives:**
- **AGENTS.md**: Proposed universal format for AI coding assistants
- **ContextHub**: Tool for managing unified configurations with symlinks
- **knowhub**: System for sharing AI assistant rules across repositories

**Current reality:**
- Teams using multiple AI tools must maintain separate configs
- Markdown-based instructions (styleguide.md, copilot-instructions.md) offer some portability
- Custom commands and rules are platform-specific

### 8.4 Interoperability Recommendations

**For maximum compatibility:**

1. **Use Markdown for instructions:**
   - `.gemini/styleguide.md` readable by humans and other tools
   - `.github/copilot-instructions.md` for GitHub Copilot
   - `CLAUDE.md` for Claude Code

2. **Document platform-specific features:**
   - Maintain README explaining custom commands
   - Provide examples for each platform

3. **Standardize where possible:**
   - `.gitignore` patterns (respected by many tools)
   - `.aiexclude` conventions
   - EditorConfig for formatting

4. **Consider abstraction layers:**
   - Tools like ContextHub to maintain single source of truth
   - Scripts to generate platform-specific configs from master file

---

## 9. Configuration Best Practices

### 9.1 Repository Setup

**Recommended structure:**
```
project-root/
├── .gemini/
│   ├── config.yaml          # Repository settings
│   ├── styleguide.md        # Coding conventions
│   └── commands/            # Team-shared commands
│       ├── review.toml
│       ├── test.toml
│       └── deploy/
│           ├── staging.toml
│           └── production.toml
├── .aiexclude              # Context exclusions
└── .gitignore              # Standard Git exclusions
```

**Version control:**
- ✅ Commit `.gemini/config.yaml`
- ✅ Commit `.gemini/styleguide.md`
- ✅ Commit `.gemini/commands/` (project-scoped)
- ✅ Commit `.aiexclude`
- ❌ Don't commit `~/.gemini/settings.json` (contains user credentials)

### 9.2 Team Collaboration

**Establish conventions:**
1. Document custom commands in project README
2. Define code review standards in styleguide.md
3. Use project-scoped JetBrains rules for team consistency
4. Share MCP server configurations (without credentials)

**Communication:**
- Add comments in config files explaining choices
- Update team when adding new commands
- Document expected behavior in styleguide.md
- Create onboarding guide for Gemini setup

### 9.3 Security Considerations

**Protect sensitive data:**
- Use `.aiexclude` for secrets, keys, credentials
- Add environment variables to exclusion list
- Don't commit API keys in settings.json
- Use MCP environment variables for credentials

**Enterprise data:**
- Review indexed repositories regularly
- Understand data retention policies
- Use organization-level controls for compliance
- Audit code customization usage

### 9.4 Performance Optimization

**Context management:**
- Exclude large data files via `.aiexclude`
- Use selective context with `@` mentions
- Index only relevant repositories (Enterprise)
- Limit max_review_comments to prevent overload

**Command efficiency:**
- Keep prompts focused and specific
- Use shell commands to preprocess data
- Cache expensive operations in scripts
- Leverage MCP servers for heavy computations

---

## 10. Migration Guide

### 10.1 From No Configuration

**Step 1: Add basic configuration**
```yaml
# .gemini/config.yaml
have_fun: false
code_review:
  comment_severity_threshold: MEDIUM
ignore_patterns:
  - "*.min.js"
  - "dist/"
  - "build/"
```

**Step 2: Create style guide**
```markdown
# .gemini/styleguide.md

## Code Standards
- [Your team's conventions]

## Testing Requirements
- [Coverage expectations]
```

**Step 3: Add useful commands**
```toml
# .gemini/commands/test.toml
description = "Run tests for changed files"
prompt = """
Run tests for files changed in the current branch:
!{git diff main --name-only | grep '.test.'}
"""
```

### 10.2 From GitHub Copilot

**Convert instructions:**
1. Copy `.github/copilot-instructions.md` content
2. Create `.gemini/styleguide.md`
3. Paste and adapt formatting if needed

**Copilot doesn't have equivalents for:**
- config.yaml settings
- Custom slash commands
- MCP integration

### 10.3 From Cursor

**Convert rules:**
1. Copy `.cursorrules` or `.cursor/rules` content
2. Add as IDE Rules (Settings > Gemini > Rules)
3. Or include in `.gemini/styleguide.md`

**Note:** Cursor rules are closer to Gemini Rules than config.yaml

---

## 11. Future Roadmap & Trends

### 11.1 Recent Additions (2025)

**May/June 2025 updates:**
- Gemini 2.5 model integration
- Custom commands in IDE extensions
- Rules/personalization features
- Enhanced context management
- MCP support in CLI

### 11.2 Expected Evolution

**Based on industry trends:**
- Increased standardization across AI assistants
- More sophisticated context management
- Deeper integration with development tools
- Enhanced team collaboration features
- Better privacy and security controls

**Gemini-specific:**
- Expanded MCP ecosystem
- More built-in tools in agent mode
- Improved code customization algorithms
- Better multi-repository understanding
- Enhanced project structure awareness

---

## 12. Comparison Matrix

| Feature | Gemini Code Assist | GitHub Copilot | Cursor | Claude Code |
|---------|-------------------|----------------|--------|-------------|
| **Custom Instructions** | ✅ Rules + styleguide.md | ✅ copilot-instructions.md | ✅ .cursor/rules | ✅ CLAUDE.md |
| **Slash Commands** | ✅ TOML files | ❌ | ❌ | ✅ Markdown files |
| **File Exclusions** | ✅ .aiexclude | ✅ .gitignore | ✅ .cursorignore | ✅ .gitignore |
| **Repository Config** | ✅ .gemini/ folder | ✅ .github/ folder | ✅ .cursor/ folder | ✅ .claude/ folder |
| **MCP Integration** | ✅ Native support | ❌ | ❌ | ✅ Native support |
| **Enterprise Indexing** | ✅ Private repo indexing | ❌ | ❌ | ❌ |
| **Agent Mode** | ✅ Multi-file edits | ❌ | ✅ Agent features | ✅ Agent features |
| **IDE Support** | VS Code, JetBrains, Android Studio | VS Code, Visual Studio, JetBrains | Cursor (VS Code fork) | CLI, Web |
| **CLI Tool** | ✅ gemini-cli | ❌ | ❌ | ✅ Native |
| **Code Review** | ✅ Automated PR reviews | ✅ Comments | ✅ Code review | ✅ PR tools |

---

## 13. Conclusion

### Key Findings

**Strengths:**
1. **Comprehensive configuration system** with multiple layers (user, project, repository)
2. **Powerful CLI** with custom commands via TOML
3. **MCP integration** for extensibility
4. **Enterprise features** for organization-specific customization
5. **Multi-IDE support** with consistent features

**Limitations:**
1. **Platform-specific formats** - limited portability to other tools
2. **No cross-platform standard** - must maintain separate configs for multi-tool teams
3. **Learning curve** - multiple configuration mechanisms to understand
4. **Enterprise dependency** - best features require paid tier

### Recommendations

**For individual developers:**
- Start with IDE Rules for immediate customization
- Create user-scoped commands (~/.gemini/commands/) for personal workflows
- Use .aiexclude to exclude irrelevant files

**For teams:**
- Establish .gemini/ folder structure in repositories
- Document coding standards in styleguide.md
- Create project-scoped commands for common workflows
- Use config.yaml for code review automation

**For enterprises:**
- Enable Code Customization for organization-specific training
- Set up repository indexing for internal libraries
- Deploy organization-level style guides
- Integrate MCP servers for internal tools

### Future Outlook

Gemini Code Assist is actively evolving with regular feature additions. The 2025 updates demonstrate Google's commitment to customization and extensibility. However, the AI coding assistant space still lacks standardization, requiring teams using multiple tools to maintain separate configurations.

The MCP protocol offers promise for tool integration standardization, but configuration formats remain tool-specific. Teams should expect to maintain platform-specific configs for the foreseeable future while advocating for industry-wide standards.

---

## 14. Additional Resources

### Official Documentation
- Gemini Code Assist Docs: https://developers.google.com/gemini-code-assist/docs/
- Gemini CLI GitHub: https://github.com/google-gemini/gemini-cli
- Code Customization Guide: https://cloud.google.com/gemini/docs/codeassist/code-customization
- Custom Commands Blog: https://cloud.google.com/blog/topics/developers-practitioners/gemini-cli-custom-slash-commands

### Community Resources
- Gemini CLI Codelabs: https://codelabs.developers.google.com/gemini-cli-hands-on
- MCP Specification: https://modelcontextprotocol.io/
- Community command repositories on GitHub

### Related Standards
- AGENTS.md proposal for universal AI assistant configuration
- Model Context Protocol (MCP) for tool integration
- EditorConfig for cross-platform editor settings

---

**Report prepared for:** Hackathon Project Research
**Next steps:** Evaluate applicability to current project architecture and team workflow
