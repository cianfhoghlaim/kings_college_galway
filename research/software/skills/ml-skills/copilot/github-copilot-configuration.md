# GitHub Copilot Custom Configuration Research

**Research Date:** 2025-11-20
**Status:** Comprehensive analysis of GitHub Copilot customization capabilities

---

## Executive Summary

GitHub Copilot offers extensive customization capabilities through multiple configuration mechanisms including custom instruction files, prompt files, organization-level settings, and extensions. As of 2025, Copilot has significantly expanded its support for cross-platform configuration formats, including AGENTS.md, CLAUDE.md, and GEMINI.md files, demonstrating commitment to interoperability with other AI coding assistants.

---

## 1. Project-Specific Configuration Methods

### 1.1 Custom Instructions Files

GitHub Copilot supports multiple instruction file formats with different scopes and priorities:

#### Repository-Wide Instructions
**Primary File:** `.github/copilot-instructions.md`

```markdown
# Location
/your-project/.github/copilot-instructions.md

# Purpose
Applies to all chat requests and coding agent operations within the repository

# Format
Plain Markdown with natural language instructions

# Example Content
# Project Overview
This is a TypeScript React application using Next.js 14 and Tailwind CSS.

## Coding Standards
- Use functional components with TypeScript
- Prefer named exports over default exports
- Use Tailwind utility classes instead of custom CSS
- Follow the Airbnb style guide for JavaScript/TypeScript

## Project Structure
- `/app` - Next.js app directory
- `/components` - Reusable UI components
- `/lib` - Utility functions and shared logic
```

#### Path-Specific Instructions
**Location:** `.github/instructions/*.instructions.md`

These files use YAML frontmatter to specify which files or directories they apply to:

```markdown
---
description: 'Python testing guidelines using pytest'
applyTo: '**/*.py'
excludeAgent: 'code-review'  # Available since Nov 2025
---

# Python Testing Standards
- Use pytest for all test files
- Name test files with test_*.py pattern
- Use fixtures for common test setup
- Aim for 80% code coverage minimum
```

**Supported Frontmatter Properties:**
- `description`: 1-500 characters explaining the instruction purpose
- `applyTo`: Glob pattern(s) for file matching (e.g., `**/*.ts`, `**/*.py`)
- `excludeAgent`: Control which agents use the instructions
  - `"code-review"` - Hide from Copilot code review
  - `"coding-agent"` - Hide from Copilot coding agent

#### Cross-Platform Instruction Files (Added 2025)

**AGENTS.md Support** (Added August 2025)
```markdown
# Location Options
/your-project/AGENTS.md              # Repository root
/your-project/src/AGENTS.md          # Component-specific
/your-project/backend/AGENTS.md      # Service-specific

# Behavior
The nearest AGENTS.md file in the directory tree takes precedence

# Purpose
Provides a standardized, cross-platform format for AI coding assistants
Described as "a README for agents"
```

**Alternative Model-Specific Files:**
- `CLAUDE.md` - Claude-specific instructions (repository root)
- `GEMINI.md` - Gemini-specific instructions (repository root)

### 1.2 Prompt Files (Reusable Task Templates)

**Location:** `.github/prompts/*.prompt.md`

Prompt files define reusable templates for common development tasks:

```markdown
# Example: .github/prompts/code-review.prompt.md
Review the current changes for:
1. Code quality and best practices
2. Potential bugs or edge cases
3. Performance considerations
4. Security vulnerabilities
5. Documentation completeness

Provide specific suggestions for improvements.
```

**Usage:**
- Type `#prompt:` in Copilot Chat to see available prompts
- Reference by name: `#prompt:code-review`
- Works in VS Code, Visual Studio 2022, and JetBrains IDEs

**Key Differences from Instructions:**
- Instructions apply automatically to all requests
- Prompts are triggered on-demand for specific tasks
- Prompts enable task libraries and workflow standardization

### 1.3 Global vs Workspace Configuration

**Workspace Instructions:**
- Stored in `.github/` directory of repository
- Apply only within that workspace
- Checked into version control
- Shared across team members

**Global Instructions (IDE-Specific):**

For JetBrains:
```
macOS: /Users/YOUR-USERNAME/.config/github-copilot/intellij/global-copilot-instructions.md
Windows: C:\Users\YOUR-USERNAME\AppData\Local\github-copilot\intellij\global-copilot-instructions.md
```

For VS Code:
- Configured through user settings
- Available across all workspaces
- Personal to each developer

---

## 2. Organization and Enterprise Configuration

### 2.1 Organization Custom Instructions

**Availability:** GitHub Copilot Enterprise (Added April 2025)

**Features:**
- Set default instructions for all users in the organization
- Ensure consistent coding standards across teams
- Support for coding agent operations (Added November 2025)

**Configuration Path:**
1. Click profile picture → Organizations
2. Select organization → Settings
3. Navigate to "Code, planning, and automation" → Copilot → Policies
4. Configure organization-wide custom instructions

### 2.2 Instruction Priority Hierarchy

When multiple instruction sources exist, Copilot combines them with the following priority:

1. **Personal/User Instructions** (Highest priority)
2. **Repository Instructions** (`.github/copilot-instructions.md`, AGENTS.md)
3. **Organization Instructions** (Lowest priority)

**Important:** All relevant instruction sets are combined and provided to Copilot, not replaced.

### 2.3 Enterprise Policy Management

**Enterprise-Level Controls:**
- Override organization settings when explicit policies are set
- Manage Copilot usage policies across the enterprise
- Control feature availability and access

---

## 3. Slash Commands and Custom Commands

### 3.1 Built-in Slash Commands

Slash commands provide targeted assistance for specific tasks:

**Common Slash Commands:**
- `/explain` - Explain selected code
- `/fix` - Suggest fixes for problems
- `/tests` - Generate unit tests
- `/help` - Show available commands
- `/new` - Scaffold new code
- `/doc` - Generate documentation

**Usage:**
Type `/` in Copilot Chat to see all available commands for your context.

### 3.2 Custom Slash Commands

**Current Limitation:**
GitHub Copilot does not natively support user-defined slash commands through simple configuration files.

**Available Approaches:**

#### Option 1: VS Code Extensions
Create a custom VS Code extension using the Copilot Extension API.

**Example:** The "Copilot Custom Commands" extension from the VS Code marketplace provides custom slash command capabilities.

#### Option 2: GitHub Copilot Extensions

**Requirements:**
- Build agent using Copilot Extensions API
- Deploy as GitHub App or web service
- Use official Copilot Extensions SDK

**Reference Implementation:**
`github/spec-kit` - Open source toolkit demonstrating custom commands for spec-driven development:
```bash
# Initialize with spec-kit
uvx --from git+https://github.com/github/spec-kit.git specify init <PROJECT_NAME>

# Specify AI assistant
specify init my-project --ai copilot
specify init my-project --ai claude
specify init my-project --ai cursor-agent

# Provides custom commands like:
/specify - Start spec-driven development
/plan - Create implementation plan
/tasks - Break down tasks
```

**Example Copilot Extensions:**
- `Blackbeard` - Simple starter agent example
- `GitHub Models` - Complex multi-LLM integration
- `Function Calling` - Demonstrates function calling patterns (Go)
- `RAG Extension` - Retrieval augmented generation example (Go)

**Extension Development Resources:**
- Preview SDK available for streamlined development
- Handles request verification, payload parsing, response formatting
- Documentation: https://docs.github.com/en/copilot/building-copilot-extensions

### 3.3 Context Variables

Use the `#` symbol to reference specific context:

**Available Context Variables:**
- `#file:<filename>` - Reference specific file
- `#folder:<foldername>` - Reference directory
- `#symbol:<symbolname>` - Reference code symbol
- `#prompt:<promptname>` - Reference prompt file
- `@workspace` - Current workspace context
- `@vscode` - VS Code specific features

**Example Usage:**
```
How does the #file:src/auth.ts authentication flow work?
Review all TypeScript files in #folder:components
Explain the #symbol:UserService class
```

---

## 4. Markdown-Based Skill Definitions

### 4.1 Native Support

GitHub Copilot **fully supports** markdown-based instruction and skill definitions:

**Supported Formats:**
- ✅ `.github/copilot-instructions.md` (Primary format)
- ✅ `.github/instructions/*.instructions.md` (Path-specific)
- ✅ `.github/prompts/*.prompt.md` (Reusable prompts)
- ✅ `AGENTS.md` (Cross-platform standard, added Aug 2025)
- ✅ `CLAUDE.md` (Alternative, root only)
- ✅ `GEMINI.md` (Alternative, root only)

### 4.2 Markdown Format Requirements

**Best Practices:**
- Use clear, concise natural language
- Structure with Markdown headers for organization
- Keep instructions focused (avoid overly long files)
- Use bullet points for guidelines and rules
- Include examples when helpful

**Recommended Content Sections:**
1. Project Overview - Purpose and context
2. Technology Stack - Frameworks, libraries, tools
3. Coding Standards - Style guides, conventions
4. Architecture Patterns - Design principles
5. Testing Requirements - Coverage, frameworks
6. Documentation Standards - Comment style, README updates

### 4.3 AGENTS.md Format

The AGENTS.md format is an open standard for cross-platform agent instructions:

```markdown
# Project Name

## Overview
Brief description of the project purpose and context

## Getting Started
How to set up and run the project

## Architecture
Key architectural decisions and patterns

## Development Guidelines
Coding standards, testing practices, review process

## Tools and Commands
Build commands, test runners, deployment procedures
```

**Advantages:**
- Platform-agnostic (works with Copilot, Claude Code, Cursor, etc.)
- Directory-scoped (can have multiple AGENTS.md files)
- Community-driven standard
- Simple markdown format

---

## 5. External Configuration Format Compatibility

### 5.1 Supported Formats

GitHub Copilot's instruction system is **Markdown-based only**:

**Native Support:**
- ✅ Markdown (`.md` files)
- ✅ YAML frontmatter in `.instructions.md` files

**Not Supported:**
- ❌ JSON configuration files
- ❌ YAML standalone files
- ❌ TOML files
- ❌ XML files

### 5.2 Cross-Platform Compatibility

**Compatibility with Other AI Coding Assistants:**

| Feature | GitHub Copilot | Claude Code | Cursor | Cline/Aider |
|---------|---------------|-------------|---------|-------------|
| `.github/copilot-instructions.md` | ✅ Native | ❌ No | ⚠️ Limited | ❌ No |
| `AGENTS.md` | ✅ Since Aug 2025 | ✅ Yes | ✅ Yes | ✅ Yes |
| `CLAUDE.md` | ✅ Since Nov 2025 | ✅ Native | ❌ No | ❌ No |
| Custom `.instructions.md` | ✅ Native | ❌ No | ❌ No | ❌ No |
| Custom slash commands | ⚠️ Extensions only | ✅ Native | ✅ Native | ✅ Native |
| Prompt files `.prompt.md` | ✅ Native | ❌ No | ⚠️ Different | ❌ No |

**Key Insight:** For maximum compatibility across AI coding tools, use `AGENTS.md` as the primary instruction format.

### 5.3 Migration Considerations

**From Claude Code to GitHub Copilot:**
- Rename `CLAUDE.md` to `.github/copilot-instructions.md` (or use both)
- Custom slash commands require extension development
- Skill definitions need conversion to prompt files

**From Cursor to GitHub Copilot:**
- Convert Cursor rules to `.github/copilot-instructions.md`
- Multi-file context handled differently
- Cursor has broader built-in model support

**Universal Approach:**
- Use `AGENTS.md` for cross-platform compatibility
- Keep tool-specific optimizations in respective config files
- Document differences in repository README

---

## 6. File Paths and Naming Conventions

### 6.1 Complete File Path Reference

```
your-project/
├── .github/
│   ├── copilot-instructions.md          # Repository-wide instructions (primary)
│   ├── instructions/
│   │   ├── python.instructions.md       # Python-specific guidelines
│   │   ├── frontend.instructions.md     # Frontend component rules
│   │   ├── api.instructions.md          # Backend API standards
│   │   └── testing.instructions.md      # Test file conventions
│   └── prompts/
│       ├── code-review.prompt.md        # Code review template
│       ├── refactor.prompt.md           # Refactoring workflow
│       ├── bug-fix.prompt.md            # Bug investigation template
│       └── feature.prompt.md            # New feature scaffolding
├── AGENTS.md                             # Cross-platform root instructions
├── CLAUDE.md                             # Claude-specific (alternative)
├── GEMINI.md                             # Gemini-specific (alternative)
├── src/
│   └── AGENTS.md                         # Source-specific instructions
└── backend/
    └── AGENTS.md                         # Backend-specific instructions
```

### 6.2 Naming Conventions

**Instruction Files:**
- Pattern: `<topic>.instructions.md`
- Location: `.github/instructions/`
- Examples: `typescript.instructions.md`, `database.instructions.md`

**Prompt Files:**
- Pattern: `<action>.prompt.md`
- Location: `.github/prompts/`
- Examples: `deploy.prompt.md`, `security-audit.prompt.md`

**Cross-Platform Files:**
- Must be exactly: `AGENTS.md`, `CLAUDE.md`, or `GEMINI.md`
- Can exist at repository root or in subdirectories (AGENTS.md only)
- Case-sensitive on Linux/macOS

### 6.3 File Detection and Priority

**Copilot's File Discovery Process:**

1. **User/Personal Instructions** (Highest priority)
   - IDE-specific global configuration

2. **Repository Instructions** (Combined)
   - `.github/copilot-instructions.md`
   - `.github/instructions/*.instructions.md` (matching current file)
   - `AGENTS.md` (nearest in directory tree)
   - `CLAUDE.md` or `GEMINI.md` (root only)

3. **Organization Instructions** (Lowest priority)
   - Set by organization administrators

**AGENTS.md Directory Precedence:**
When multiple `AGENTS.md` files exist, Copilot uses the nearest ancestor:
```
/your-project/AGENTS.md              # General project rules
/your-project/src/AGENTS.md          # Overrides for /src/**
/your-project/src/api/AGENTS.md      # Overrides for /src/api/**
```

### 6.4 File Size and Content Recommendations

**Best Practices:**
- Keep individual instruction files under 1000 lines
- Use focused, specific instructions rather than generic advice
- Break large instruction sets into multiple `.instructions.md` files
- Reference external documentation with links rather than duplicating
- Update instructions as project evolves

---

## 7. Recent Feature Updates (2025)

### Timeline of Major Enhancements

**January 2024:**
- Slash commands and context variables introduced

**April 2025:**
- Organization custom instructions launched
- Enterprise policy management expanded

**July 2025:**
- `.instructions.md` with YAML frontmatter support added
- Path-specific instruction targeting introduced

**August 2025:**
- `AGENTS.md` support added to coding agent
- Cross-platform compatibility enhanced

**November 2025:**
- `excludeAgent` property for agent-specific instructions
- Coding agent supports organization custom instructions
- `CLAUDE.md` and `GEMINI.md` file support added

### Upcoming Trends

Based on the research, GitHub Copilot is moving toward:
1. **Greater cross-platform compatibility** with other AI assistants
2. **More granular control** over when instructions apply
3. **Enhanced enterprise management** capabilities
4. **Standardization** around open formats like AGENTS.md

---

## 8. Comparison with Other AI Coding Assistants

### 8.1 Configuration Approach Comparison

**GitHub Copilot:**
- **Strength:** Native IDE integration, enterprise management, broad platform support
- **Weakness:** Limited custom slash command support without extensions
- **Architecture:** Plugin-based, preserves existing IDE workflows
- **Best For:** Teams using VS Code, Visual Studio, or JetBrains with enterprise needs

**Cursor:**
- **Strength:** Standalone IDE with built-in AI, full codebase context, custom rules
- **Weakness:** Migration friction from existing IDEs, proprietary format
- **Architecture:** AI-first complete environment
- **Best For:** Individual developers or teams willing to switch IDEs

**Claude Code:**
- **Strength:** Advanced conversational abilities, skill system, custom commands
- **Weakness:** Limited IDE integration, primarily web-based
- **Architecture:** Chat-centric with CLI support
- **Best For:** Complex reasoning tasks, research, exploration

**Cline/Aider:**
- **Strength:** Terminal-based, git-aware, minimal setup
- **Weakness:** No GUI, limited enterprise features
- **Architecture:** Command-line first
- **Best For:** Terminal-native developers, automation scripts

### 8.2 Feature Matrix

| Capability | Copilot | Cursor | Claude Code | Cline |
|------------|---------|--------|-------------|-------|
| Inline completions | ✅ Excellent | ✅ Excellent | ❌ No | ⚠️ Limited |
| Chat interface | ✅ Good | ✅ Excellent | ✅ Excellent | ⚠️ CLI only |
| Multi-file context | ⚠️ Limited | ✅ Excellent | ✅ Good | ✅ Good |
| Custom instructions | ✅ Excellent | ✅ Good | ✅ Excellent | ⚠️ Basic |
| Slash commands | ✅ Built-in | ✅ Custom | ✅ Custom | ✅ Custom |
| Enterprise support | ✅ Excellent | ⚠️ Growing | ❌ Limited | ❌ No |
| Model selection | ⚠️ Limited | ✅ Extensive | ✅ Claude only | ⚠️ Limited |
| Pricing (per month) | $10-39 | $20-40 | Pay-per-use | Free/Open |

### 8.3 When to Choose GitHub Copilot

**Ideal Scenarios:**
- Enterprise environment with compliance requirements
- Team already using VS Code, Visual Studio, or JetBrains
- Need for consistent coding standards across organization
- Desire for official Microsoft/GitHub support
- Budget for per-seat licensing model

**Consider Alternatives When:**
- Need extensive custom slash commands (→ Claude Code, Cursor)
- Want full codebase context awareness (→ Cursor)
- Prefer terminal-based workflow (→ Cline, Aider)
- Need maximum model flexibility (→ Cursor)
- Working on small personal projects (→ Free alternatives)

---

## 9. Implementation Recommendations

### 9.1 Getting Started Checklist

**Phase 1: Basic Configuration**
- [ ] Create `.github/copilot-instructions.md` with project overview
- [ ] Document technology stack and coding standards
- [ ] Add project structure explanation
- [ ] Include common commands and workflows

**Phase 2: Advanced Customization**
- [ ] Create path-specific `.instructions.md` files for different file types
- [ ] Add reusable `.prompt.md` files for common tasks
- [ ] Configure YAML frontmatter for targeted application
- [ ] Test instructions with actual coding scenarios

**Phase 3: Cross-Platform Support**
- [ ] Add `AGENTS.md` for compatibility with other AI tools
- [ ] Document differences between tool-specific configurations
- [ ] Update README with AI assistant guidelines
- [ ] Share best practices with team

**Phase 4: Enterprise Rollout** (If applicable)
- [ ] Configure organization-level custom instructions
- [ ] Set up enterprise policies
- [ ] Train team on custom prompts and slash commands
- [ ] Monitor usage and iterate on instructions

### 9.2 Sample Configuration Templates

**Minimal .github/copilot-instructions.md:**
```markdown
# Project: [Your Project Name]

## Technology Stack
- Language: [e.g., TypeScript]
- Framework: [e.g., React 18 + Next.js 14]
- Styling: [e.g., Tailwind CSS]
- Testing: [e.g., Jest + React Testing Library]

## Coding Standards
- Use functional components with hooks
- Prefer composition over inheritance
- Write unit tests for business logic
- Document complex algorithms

## Project Structure
- `/app` - Next.js app router pages
- `/components` - Reusable UI components
- `/lib` - Shared utilities and helpers
- `/tests` - Test files
```

**Sample .github/instructions/testing.instructions.md:**
```markdown
---
description: 'Testing standards for all test files'
applyTo: '**/*.test.ts'
---

# Testing Guidelines

## Test Structure
- Use describe/it blocks for organization
- One assertion per test when possible
- Use meaningful test descriptions

## Best Practices
- Mock external dependencies
- Test edge cases and error conditions
- Aim for 80% code coverage
- Keep tests fast and isolated
```

**Sample .github/prompts/feature.prompt.md:**
```markdown
Create a new feature following these steps:

1. Create component file in /components
2. Write TypeScript interfaces for props
3. Implement component with proper typing
4. Add unit tests in /tests
5. Update Storybook story if UI component
6. Document usage in component header

Ensure all code follows project standards and includes proper error handling.
```

### 9.3 Common Pitfalls to Avoid

**❌ Don't:**
- Write overly generic instructions that could apply to any project
- Duplicate information already in documentation
- Make instructions too long or detailed (over 1000 lines)
- Include outdated or conflicting guidelines
- Forget to update instructions when project evolves

**✅ Do:**
- Keep instructions specific to your project's unique needs
- Link to external documentation rather than duplicating it
- Review and update instructions quarterly
- Test instructions with real Copilot usage
- Gather team feedback on instruction effectiveness

---

## 10. Security and Privacy Considerations

### 10.1 Data Handling

**What Copilot Receives:**
- Code in open editor tabs
- Content of instruction files
- Conversation history in Copilot Chat
- Repository structure and file names

**What's NOT Sent:**
- Uncommitted changes (unless explicitly included in context)
- Files outside workspace (unless referenced)
- Environment variables
- Secrets in .env files (if properly configured)

### 10.2 Enterprise Security Features

**GitHub Copilot Enterprise:**
- EU data residency (available since October 2024)
- Code and repository data can stay within European Union
- Audit logs for compliance
- IP indemnification
- No training on your code (business/enterprise tiers)

### 10.3 Best Practices for Secure Configuration

**Instruction File Security:**
- Never include secrets, API keys, or credentials in instruction files
- Don't document security vulnerabilities in instruction files
- Avoid exposing proprietary algorithms in detail
- Keep sensitive architectural decisions in private documentation

**Prompt File Security:**
- Review prompts for unintended information disclosure
- Be cautious with prompts that generate configuration files
- Validate generated code before committing
- Use code review for Copilot-generated sensitive code

---

## 11. Resources and References

### 11.1 Official Documentation

**Primary Resources:**
- [GitHub Copilot Documentation](https://docs.github.com/copilot)
- [VS Code Copilot Docs](https://code.visualstudio.com/docs/copilot)
- [Custom Instructions Guide](https://docs.github.com/copilot/customizing-copilot/adding-custom-instructions-for-github-copilot)
- [Building Copilot Extensions](https://docs.github.com/en/copilot/building-copilot-extensions)

**Changelog and Updates:**
- [GitHub Changelog - Copilot](https://github.blog/changelog/)
- [VS Code Release Notes](https://code.visualstudio.com/updates)

### 11.2 Community Resources

**Example Repositories:**
- [github/awesome-copilot](https://github.com/github/awesome-copilot) - Community instructions and prompts
- [github/spec-kit](https://github.com/github/spec-kit) - Spec-driven development toolkit
- [copilot-extensions](https://github.com/copilot-extensions) - Example extensions

**AGENTS.md Standard:**
- [agents.md](https://agents.md/) - Official AGENTS.md specification
- Cross-platform instruction format documentation

### 11.3 Learning Resources

**Tutorials and Guides:**
- [5 Tips for Writing Better Custom Instructions](https://github.blog/ai-and-ml/github-copilot/5-tips-for-writing-better-custom-instructions-for-copilot/)
- [How to Write a Great AGENTS.md](https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/)
- [Boost Your Copilot Collaboration with Reusable Prompt Files](https://devblogs.microsoft.com/visualstudio/boost-your-copilot-collaboration-with-reusable-prompt-files/)

---

## 12. Conclusion and Recommendations

### Key Findings

1. **Robust Configuration System:** GitHub Copilot offers comprehensive customization through markdown-based instruction files, supporting both repository-wide and path-specific configurations.

2. **Cross-Platform Evolution:** The addition of AGENTS.md, CLAUDE.md, and GEMINI.md support in 2025 demonstrates GitHub's commitment to interoperability with other AI coding assistants.

3. **Enterprise Ready:** Organization-level instructions, policy management, and compliance features make Copilot suitable for large-scale enterprise deployments.

4. **Extension Limitations:** While built-in slash commands are powerful, creating custom slash commands requires extension development, unlike some competitors (Claude Code, Cursor) that support this natively.

5. **Markdown-Only:** Configuration is limited to Markdown format with YAML frontmatter - no JSON, TOML, or pure YAML configuration files are supported.

### Recommended Configuration Strategy

**For Small Projects/Individual Developers:**
```
project/
├── AGENTS.md                      # Cross-platform compatibility
└── .github/
    └── copilot-instructions.md    # Copilot-specific optimizations
```

**For Medium Projects/Small Teams:**
```
project/
├── AGENTS.md
├── .github/
│   ├── copilot-instructions.md
│   ├── instructions/
│   │   ├── frontend.instructions.md
│   │   └── backend.instructions.md
│   └── prompts/
│       ├── code-review.prompt.md
│       └── feature.prompt.md
```

**For Enterprise/Large Teams:**
```
project/
├── AGENTS.md
├── .github/
│   ├── copilot-instructions.md
│   ├── instructions/
│   │   ├── typescript.instructions.md
│   │   ├── python.instructions.md
│   │   ├── testing.instructions.md
│   │   └── security.instructions.md
│   └── prompts/
│       ├── code-review.prompt.md
│       ├── security-audit.prompt.md
│       ├── refactor.prompt.md
│       └── deployment.prompt.md
└── [subdirectories with scoped AGENTS.md files]
```

Plus organization-level custom instructions configured in GitHub Enterprise settings.

### Final Recommendations

1. **Start Simple:** Begin with basic `.github/copilot-instructions.md`, iterate based on team feedback

2. **Embrace AGENTS.md:** Use it as primary format for maximum tool compatibility

3. **Use Path-Specific Instructions:** Leverage `.instructions.md` with `applyTo` frontmatter for different file types

4. **Create Prompt Library:** Build reusable `.prompt.md` files for common workflows

5. **Monitor and Update:** Treat instruction files as living documentation, update quarterly

6. **Consider Extensions:** If custom slash commands are critical, evaluate building a Copilot Extension or using spec-kit

7. **Enterprise Planning:** For large organizations, invest in organization-level configuration and policy management

### Future Watch

Keep an eye on:
- Expanded `excludeAgent` capabilities for finer control
- Potential JSON/YAML configuration support
- Enhanced custom slash command APIs
- Deeper integration with other AI coding assistants
- Community-driven instruction template sharing

---

**Document Version:** 1.0
**Last Updated:** 2025-11-20
**Maintained By:** Research Team
**Next Review:** 2025-12-20
