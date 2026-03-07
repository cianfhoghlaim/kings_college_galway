# Gemini Code Assist Quick Reference

## Configuration File Locations

```
project-root/
├── .gemini/
│   ├── config.yaml              # Repository settings
│   ├── styleguide.md            # Natural language coding conventions
│   └── commands/                # Project-scoped slash commands
│       └── *.toml
├── .aiexclude                   # Files to exclude from context
└── .gitignore                   # Also respected by Gemini

~/.gemini/
├── settings.json                # User settings + MCP servers
└── commands/                    # User-scoped slash commands
    └── *.toml
```

## config.yaml Example

```yaml
have_fun: false
memory_config:
  disabled: false
code_review:
  disable: false
  comment_severity_threshold: MEDIUM  # LOW, MEDIUM, HIGH, CRITICAL
  max_review_comments: -1  # -1 = unlimited
  pull_request_opened:
    summary: true
    code_review: true
    include_drafts: true
ignore_patterns:
  - "*.min.js"
  - "dist/"
  - "node_modules/"
```

## styleguide.md Example

```markdown
# Project Code Style Guide

## Language Standards
- TypeScript strict mode required
- ESLint must pass before commits
- Follow functional programming patterns

## Testing
- Minimum 80% code coverage
- Jest for unit tests
- Playwright for E2E

## Documentation
- JSDoc for all public functions
- README for each major module
```

## Custom Slash Command (TOML)

**Basic format:**
```toml
description = "Brief description for /help menu"
prompt = """
Your instructions here.
Use {{args}} for user input.
Execute commands with !{shell command}.
"""
```

**Example - Code Review:**
```toml
# .gemini/commands/review.toml
description = "Review a GitHub pull request"
prompt = """
Review GitHub PR #{{args}}

1. Fetch details: !{gh pr view {{args}} --json files}
2. Check code quality, security, tests
3. Reference project styleguide.md
4. Provide structured feedback
"""
```

**Usage:** `/review 123`

**Namespacing:**
- `commands/test.toml` → `/test`
- `commands/git/commit.toml` → `/git:commit`

## IDE Rules

**VS Code:**
1. Ctrl+Shift+P (Cmd+Shift+P on Mac)
2. "Preferences: Open Settings (UI)"
3. Search "Geminicodeassist: Rules"
4. Add rules (one per line)

**JetBrains:**
1. Settings > Tools > Gemini > Prompt Library > Rules
2. Choose scope: IDE (personal) or Project (shared)
3. Add rules

**Example rules:**
```
Always generate unit tests for new functions
Use TypeScript strict mode
Prefer async/await over promise chains
Include error handling in API calls
Add JSDoc for public functions
```

## MCP Server Configuration

**Location:** `~/.gemini/settings.json`

**Stdio server:**
```json
{
  "mcpServers": {
    "git": {
      "command": "uvx",
      "args": ["mcp-server-git", "--repository", "."],
      "timeout": 30000
    }
  }
}
```

**HTTP server:**
```json
{
  "mcpServers": {
    "github": {
      "httpUrl": "https://api.github.com/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_TOKEN"
      }
    }
  }
}
```

## Context Selection

**In chat:**
- `@filename` - Include specific local file
- `@repository` - Include remote indexed repo (Enterprise)

**Exclusions:**
Create `.aiexclude` with patterns:
```
.env
secrets/
*.key
data/*.csv
dist/
```

## Built-in Commands

**CLI:**
- `/help` - List all commands
- `/tools` - Show available tools
- `/mcp` - List MCP servers and status

**VS Code Quick Pick (Ctrl+I / Cmd+I):**
- `/fix` - Fix code issues
- `/generate` - Generate new code
- `/doc` - Add documentation
- `/simplify` - Simplify code

## Command Naming Conventions

✅ **Good:**
- `test.toml` → `/test`
- `review-pr.toml` → `/review-pr`
- `git/commit.toml` → `/git:commit`

❌ **Bad:**
- `Test.toml` → `/Test` (case-sensitive!)
- `my command.toml` → Invalid (spaces)

## Scope Comparison

| Feature | User Scope | Project Scope | Repository Scope |
|---------|-----------|---------------|------------------|
| **Commands** | `~/.gemini/commands/` | `.gemini/commands/` | N/A |
| **Settings** | `~/.gemini/settings.json` | `.gemini/settings.json` | N/A |
| **Config** | N/A | N/A | `.gemini/config.yaml` |
| **Style Guide** | N/A | N/A | `.gemini/styleguide.md` |
| **Rules (VS Code)** | Personal | Can be project | N/A |
| **Rules (JetBrains)** | IDE-level | Project-level | N/A |

## Quick Setup Checklist

### For a New Project

- [ ] Create `.gemini/` folder
- [ ] Add `config.yaml` with team settings
- [ ] Write `styleguide.md` with coding conventions
- [ ] Create useful team commands in `.gemini/commands/`
- [ ] Add `.aiexclude` for sensitive/large files
- [ ] Commit `.gemini/` folder to Git
- [ ] Document setup in project README

### For Individual Use

- [ ] Install Gemini CLI or IDE extension
- [ ] Configure Rules in IDE settings
- [ ] Create personal commands in `~/.gemini/commands/`
- [ ] Set up MCP servers in `~/.gemini/settings.json`
- [ ] Add `.aiexclude` to projects

## Comparison with Other Tools

| Feature | Gemini | Copilot | Cursor | Claude Code |
|---------|--------|---------|--------|-------------|
| **Config File** | `.gemini/config.yaml` | `.github/copilot-instructions.md` | `.cursor/rules` | `CLAUDE.md` |
| **Commands** | TOML files | ❌ | ❌ | Markdown |
| **MCP** | ✅ | ❌ | ❌ | ✅ |
| **Exclusions** | `.aiexclude` | `.gitignore` | `.cursorignore` | `.gitignore` |

## Common Patterns

### Team Code Review Command
```toml
# .gemini/commands/review.toml
description = "Comprehensive PR review"
prompt = """
Review the current changes:
!{git diff main}

Check:
1. Style compliance (see styleguide.md)
2. Security issues
3. Test coverage
4. Documentation

Provide actionable feedback.
"""
```

### Testing Command
```toml
# .gemini/commands/test.toml
description = "Run tests for changed files"
prompt = """
Files changed: !{git diff --name-only main}

Generate and run tests for these files.
Ensure all pass before proceeding.
"""
```

### Documentation Generator
```toml
# .gemini/commands/doc.toml
description = "Generate missing documentation"
prompt = """
Analyze: {{args}}

Generate:
1. JSDoc/docstrings for functions
2. README if missing
3. Usage examples
4. Type definitions
"""
```

## Tips & Best Practices

**For better responses:**
1. Use specific file context with `@`
2. Reference styleguide.md in prompts
3. Break complex tasks into multiple commands
4. Use shell commands to provide current state

**For team collaboration:**
1. Document all custom commands
2. Use consistent naming conventions
3. Keep config.yaml simple and well-commented
4. Update styleguide.md as standards evolve

**For security:**
1. Always exclude secrets in `.aiexclude`
2. Don't commit `settings.json` with tokens
3. Use environment variables for MCP credentials
4. Review indexed repositories regularly (Enterprise)

## Getting Help

- Official docs: https://developers.google.com/gemini-code-assist/docs/
- CLI GitHub: https://github.com/google-gemini/gemini-cli
- Use `/help` in CLI to list commands
- Check `/mcp` for MCP server status
