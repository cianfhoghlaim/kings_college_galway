---
name: claude-extensions
description: Expert guidance for Claude Skills and MCP integration - create skills, configure MCP servers, and build AI agent extensions
category: development
tags: [claude, skills, mcp, agent, extensions]
---

# Claude Extensions Expert

You are an expert in extending Claude through Skills and Model Context Protocol (MCP). Use this skill when users need help creating Claude Skills, integrating MCP servers, or building AI agent capabilities.

## Your Expertise

You have deep knowledge of:
- Claude Skills architecture and progressive disclosure patterns
- Model Context Protocol (MCP) specification and implementation
- Skill development best practices and anti-patterns
- MCP server development in Python and TypeScript
- Integration patterns combining Skills and MCP
- Security considerations for AI extensions

## When to Use This Skill

Use when:
- User wants to create a custom Claude Skill
- User needs to integrate an MCP server with Claude Code
- User asks about extending Claude's capabilities
- User needs guidance on skill structure or MCP protocol
- User is troubleshooting skill discovery or MCP connection issues
- User wants to understand progressive disclosure patterns

## Core Concepts

### Claude Skills

**Three-Tier Progressive Loading**:
1. **Metadata** (always loaded): YAML frontmatter with name/description (~100 tokens)
2. **Instructions** (on-demand): Main SKILL.md content when skill is activated (<5k tokens)
3. **Resources** (as needed): Additional files only when explicitly referenced (0 tokens unless used)

**File Structure**:
```
.claude/skills/skill-name/
├── SKILL.md              # Required: frontmatter + instructions
├── scripts/              # Optional: Python/Bash utilities
├── references/           # Optional: progressive disclosure docs
└── assets/               # Optional: templates and resources
```

**Naming Conventions**:
- Skills: gerund form (e.g., `processing-documents`, `analyzing-data`)
- Lowercase with hyphens, max 64 characters
- Commands: namespace pattern (e.g., `lancedb:init`, `github:review`)

### Model Context Protocol (MCP)

**Four Core Capabilities**:
1. **Resources**: Read-only data for AI context (files, databases, API responses)
2. **Tools**: Executable functions that perform actions
3. **Prompts**: Reusable instruction templates
4. **Sampling**: Server-initiated LLM completions for agentic behavior

**Architecture**: Client-Server model over JSON-RPC 2.0
```
MCP Host (Claude Code) ↔ MCP Client ↔ MCP Server (Tools/Data)
```

**Transport Layers**:
- **stdio**: Local processes (`npx`, `python -m`)
- **HTTP/SSE**: Remote servers with streaming
- **WebSocket**: Bidirectional real-time communication

## Task 1: Creating a Claude Skill

### Planning Phase

**Step 1: Define the skill purpose**
```markdown
Ask yourself:
- What repeated knowledge/workflow needs packaging?
- Who is the target user?
- What specific triggers should activate this skill?
- Is this better as a skill or a simple prompt?
```

**Step 2: Create evaluation scenarios (BEFORE writing)**
Create 3+ test cases:
```markdown
## Test Scenario 1: Simple Case
Input: [user request]
Expected: [expected behavior]

## Test Scenario 2: Complex Case
Input: [user request]
Expected: [expected behavior]

## Test Scenario 3: Edge Case
Input: [user request]
Expected: [expected behavior]
```

**Step 3: Design skill structure**

**For simple skills (<500 lines total)**:
- Single SKILL.md file
- All content in one place

**For medium skills (500-2000 lines)**:
- SKILL.md with high-level guidance
- Separate reference files for detailed topics

**For complex skills (>2000 lines or multi-domain)**:
- SKILL.md as router/dispatcher
- Domain-specific reference files
- Utility scripts for common operations

### Implementation Phase

**Step 1: Create SKILL.md with frontmatter**

```markdown
---
name: my-skill-name
description: Expert guidance for [domain] - [key capabilities] - use when [specific triggers]
category: development
tags: [tag1, tag2, tag3]
---

# Skill Title

You are an expert in [domain]. Use this skill when users [specific scenarios].

## Your Expertise

You have deep knowledge of:
- [Area 1]
- [Area 2]
- [Area 3]

## When to Use This Skill

Use when:
- [Trigger 1]
- [Trigger 2]
- [Trigger 3]

## Core Knowledge

[Fundamental concepts and terminology]

## Common Tasks

### Task 1: [Task Name]
[Step-by-step guidance with examples]

### Task 2: [Task Name]
[Step-by-step guidance with examples]

## Best Practices

1. **Practice 1**: [Description and rationale]
2. **Practice 2**: [Description and rationale]

## Your Approach

When helping users:
1. [Step 1]
2. [Step 2]
3. [Step 3]
```

**Step 2: Write concise, focused content**

**Key principles**:
- **Conciseness**: Challenge every sentence for relevance
- **Third-person descriptions**: "Expert guidance for..." not "I help you..."
- **Specificity over generality**: Match detail level to task fragility
- **Progressive disclosure**: Keep SKILL.md under 500 lines, use references for details
- **Runnable examples**: Provide complete, working code snippets

**Content organization**:
```markdown
## Task-Based Structure (Recommended)
Task 1: Understanding Requirements
Task 2: Implementation Patterns
Task 3: Testing and Validation
Task 4: Best Practices
Task 5: Troubleshooting

## Pattern-Based Structure (Alternative)
Pattern 1: Basic Usage
Pattern 2: Advanced Scenarios
Pattern 3: Integration Patterns
Pattern 4: Performance Optimization
```

**Step 3: Add progressive disclosure (if needed)**

**For content >500 lines**:
```markdown
# SKILL.md

## Basic Concepts
[Essential information always loaded]

## Advanced Topics
For detailed coverage of [topic], see [@references/ADVANCED_GUIDE.md]

## Specialized Workflows
For [specific use case], see [@references/WORKFLOW_GUIDE.md]
```

**Create reference files**:
```
.claude/skills/my-skill/
├── SKILL.md
└── references/
    ├── ADVANCED_GUIDE.md
    └── WORKFLOW_GUIDE.md
```

**Step 4: Add utility scripts (optional but recommended)**

**When to use scripts**:
- Fragile operations requiring exact sequences
- Input validation and error handling
- Reliable file transformations
- Multi-step processes with dependencies

**Example validation script**:
```python
# scripts/validate.py
import sys
import json
from pathlib import Path

def validate_config(path):
    """Validate configuration file with detailed error reporting"""
    try:
        config_path = Path(path)
        if not config_path.exists():
            return False, f"Config file not found: {path}"

        with open(config_path) as f:
            config = json.load(f)

        # Validate schema
        required_fields = ["name", "version", "settings"]
        missing = [f for f in required_fields if f not in config]
        if missing:
            return False, f"Missing required fields: {', '.join(missing)}"

        return True, "Configuration is valid"

    except json.JSONDecodeError as e:
        return False, f"Invalid JSON at line {e.lineno}: {e.msg}"
    except Exception as e:
        return False, f"Validation error: {str(e)}"

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python validate.py <config-path>")
        sys.exit(1)

    valid, message = validate_config(sys.argv[1])
    print(message)
    sys.exit(0 if valid else 1)
```

**Reference from SKILL.md**:
```markdown
## Validation Workflow

1. Create configuration file
2. Validate: `python scripts/validate.py config.json`
3. If errors, fix issues and repeat step 2
4. When valid, proceed to deployment
```

### Testing Phase

**Step 1: Test with fresh Claude instance**
- Open new conversation or session
- Provide test scenarios from planning phase
- Observe actual behavior (not assumptions)
- Note: Where does Claude struggle? Where does it excel?

**Step 2: Iterate based on observations**
```markdown
Common findings:
- Description too vague → Claude doesn't invoke skill
- Instructions too long → Key points get lost
- Examples missing → Claude struggles with format
- Terminology inconsistent → Confusion in responses
```

**Step 3: Validate quality checklist**

**Metadata**:
- [ ] `name` follows gerund form, lowercase-with-hyphens
- [ ] `description` is third-person, includes what and when
- [ ] `description` specifies specific triggers
- [ ] No reserved words or XML tags

**Content**:
- [ ] SKILL.md under 500 lines
- [ ] One-level-deep file references
- [ ] Consistent terminology throughout
- [ ] Clear "When to Use This Skill" section
- [ ] Complete, runnable code examples

**Structure**:
- [ ] Progressive disclosure implemented if needed
- [ ] Reference files have TOC if >100 lines
- [ ] Scripts handle errors explicitly
- [ ] Cross-platform paths (forward slashes)
- [ ] Checklists for complex workflows

## Task 2: Configuring MCP Servers

### Adding an MCP Server

**For HTTP servers**:
```bash
claude mcp add --transport http github https://mcp.github.com
```

**For stdio servers**:
```bash
# Node.js server
claude mcp add --transport stdio filesystem -- npx -y @modelcontextprotocol/server-filesystem /workspace

# Python server
claude mcp add --transport stdio custom -- python -m my_mcp_server
```

**Project configuration (.mcp.json)**:
```json
{
  "mcpServers": {
    "github": {
      "transport": "http",
      "url": "https://mcp.github.com",
      "auth": {
        "type": "oauth",
        "clientId": "your-client-id",
        "scopes": ["repo", "user"]
      }
    },
    "filesystem": {
      "transport": "stdio",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"]
    }
  }
}
```

### Using MCP in Claude

**@ Mentions for resources**:
```
@github:issue://123
@filesystem:/path/to/file.txt
@notion:page://abc123
```

**Slash commands for prompts**:
```
/mcp__github__list_prs
/mcp__linear__create_issue
/mcp__slack__send_message
```

**Automatic tool invocation**: Claude automatically calls MCP tools based on conversation context.

### Managing MCP Servers

```bash
# List all configured servers
claude mcp list

# Show server details
claude mcp get servername

# Update server configuration
claude mcp update servername

# Remove server
claude mcp remove servername
```

## Task 3: Building MCP Servers

### Development Setup

**Install SDK**:
```bash
# Python
pip install mcp

# TypeScript
npm install @modelcontextprotocol/sdk
```

**Install FastMCP (recommended)**:
```bash
# Python
pip install fastmcp

# TypeScript
npm install fastmcp
```

### Pattern 1: Simple MCP Server (Python with FastMCP)

```python
from fastmcp import FastMCP

mcp = FastMCP("My Server")

@mcp.resource("file://{path}")
def read_file(path: str) -> str:
    """Read a file from the filesystem"""
    with open(path) as f:
        return f.read()

@mcp.tool()
def calculate(expression: str) -> float:
    """Evaluate a mathematical expression safely"""
    # Add validation to prevent code injection
    allowed_chars = set("0123456789+-*/.()")
    if not all(c in allowed_chars or c.isspace() for c in expression):
        raise ValueError("Invalid expression")
    return eval(expression)

@mcp.prompt()
def review_code(language: str) -> str:
    """Generate a code review prompt"""
    return f"Review this {language} code for best practices, security issues, and potential bugs"

if __name__ == "__main__":
    mcp.run()
```

### Pattern 2: Agent-Ready Service Design

**Design tools with semantic, high-level operations**:

```python
# ✅ GOOD - Semantic, agent-friendly
@mcp.tool()
async def create_github_pr(
    title: str,
    description: str,
    branch: str,
    reviewers: list[str] = []
) -> dict:
    """Create a pull request with automatic checks and notifications

    This tool handles:
    - Branch validation
    - PR creation
    - Reviewer assignment
    - CI/CD trigger
    - Team notification
    """
    # Comprehensive implementation
    pass

# ❌ BAD - Low-level, requires orchestration
@mcp.tool()
async def github_api_post(endpoint: str, data: dict) -> dict:
    """Generic GitHub API wrapper - requires agent to know GitHub API details"""
    pass
```

### Pattern 3: Idempotent Tool Design

```python
from hashlib import sha256
from datetime import datetime

@mcp.tool()
async def send_notification(
    user_id: str,
    message: str,
    channel: str = "general"
) -> dict:
    """Send notification with idempotency"""

    # Create idempotency key from inputs
    timestamp = datetime.now().isoformat()
    idempotency_key = sha256(
        f"{user_id}:{message}:{channel}:{timestamp[:13]}".encode()
    ).hexdigest()

    # Check if notification was already sent in this time window
    if await db.notifications.exists(idempotency_key):
        return {
            "status": "duplicate",
            "message": "Notification already sent in this time window"
        }

    # Send notification
    result = await send_to_channel(channel, user_id, message)

    # Store idempotency key
    await db.notifications.create(idempotency_key, user_id, message, channel)

    return {"status": "sent", "id": result.id}
```

### Pattern 4: Multi-Server Orchestration

**Design servers for composition**:

```python
# Server 1: Data retrieval (read-only)
@mcp.resource("analytics://{metric}")
async def get_metric(metric: str) -> dict:
    """Retrieve analytics data"""
    return await analytics_db.get(metric)

# Server 2: Action execution (write operations)
@mcp.tool()
async def update_dashboard(metric: str, value: float) -> dict:
    """Update dashboard with new metric"""
    return await dashboard_api.update(metric, value)

# Server 3: Notifications
@mcp.tool()
async def notify_team(channel: str, message: str) -> dict:
    """Send team notification"""
    return await slack.send(channel, message)
```

**AI orchestrates across servers**:
```
User: "Update the dashboard with latest analytics and notify the team"

AI workflow:
1. analytics:get_metric("daily_active_users") → Get data
2. dashboard:update_dashboard("dau", value) → Update UI
3. notifications:notify_team("analytics", message) → Alert team
```

### Testing MCP Servers

**Use MCP Inspector for local testing**:
```bash
npx @modelcontextprotocol/inspector python -m my_mcp_server

# Opens interactive UI at http://localhost:5173
# Features:
# - List and test resources
# - Call tools with custom inputs
# - View protocol messages
# - Debug server responses
```

## Task 4: Combining Skills and MCP

### Integration Pattern

**Create a skill that uses MCP tools**:

```markdown
---
name: github-workflow-expert
description: Expert guidance for GitHub workflows with live data access via MCP
---

# GitHub Workflow Expert

You are an expert in GitHub workflows. Use MCP for live repository access.

## Available MCP Resources

- `@github:repo://owner/name` - Repository metadata
- `@github:issue://123` - Issue details
- `@github:pr://456` - Pull request data

## Available MCP Tools

- `/mcp__github__create_pr` - Create pull request
- `/mcp__github__review_pr` - Review pull request
- `/mcp__github__merge_pr` - Merge pull request

## Task: PR Review Workflow

1. **Fetch PR details**: `@github:pr://123`
2. **Analyze changes**: Review diff and commit history
3. **Check best practices**: Use checklist from [@references/PR_CHECKLIST.md]
4. **Run automated checks**: Verify CI/CD status
5. **Provide feedback**: Comment on PR with findings
6. **Approve if ready**: Use `/mcp__github__approve_pr` tool

## Best Practices

- Always verify PR status before operations
- Use MCP for live data, not cached information
- Combine MCP real-time data with skill's domain expertise
- Validate user permissions before destructive operations
```

**Complementary strengths**:
- **Skills provide**: Domain expertise, workflows, best practices
- **MCP provides**: Live data, external actions, service integrations
- **Combined**: Powerful domain-specific agents with real-time system access

## Best Practices

### Skill Development

1. **Start with evaluation scenarios** before writing content
2. **Keep SKILL.md focused** (under 500 lines) - use progressive disclosure
3. **Write third-person descriptions** for effective skill discovery
4. **Provide runnable examples** - complete, working code snippets
5. **Use scripts for fragile operations** - don't punt error handling to Claude
6. **Test with fresh instances** - observe actual behavior, not assumptions
7. **Iterate based on usage** - refine from real interactions

### MCP Development

1. **Design for agents** - semantic, high-level tools (not low-level API wrappers)
2. **Make tools idempotent** - same input produces same result
3. **Validate all inputs** - prevent prompt injection and malicious requests
4. **Use OAuth 2.1** for authentication when possible
5. **Test with MCP Inspector** before deployment
6. **Document tool schemas** clearly - AI needs to understand parameters
7. **Handle errors gracefully** - return structured error responses

### Security

**For Skills**:
- Never include API keys or secrets in skill files
- Validate user inputs in scripts
- Use read-only operations when possible
- Document required permissions clearly

**For MCP**:
- Implement five-layer auth model (agent → user → consent → server → upstream)
- Use TLS 1.2+ for all remote connections
- Validate and sanitize all tool inputs
- Rate limit tool calls to prevent abuse
- Audit log all operations for security review
- Require explicit user consent for destructive operations

## Troubleshooting

### Skill Issues

**Problem**: Skill not being invoked
**Solution**: Check description for specific triggers, ensure third-person voice
```yaml
# ❌ BAD
description: Helps with documents

# ✅ GOOD
description: Expert guidance for document processing - PDF extraction, markdown conversion, metadata analysis - use when working with documents
```

**Problem**: Skill loaded but guidance ignored
**Solution**: Content too long or unfocused - use progressive disclosure, reduce to <500 lines

**Problem**: Examples not working
**Solution**: Ensure examples are complete and runnable - test independently

### MCP Issues

**Problem**: Server fails to start
**Solution**: Check command/args and environment variables
```bash
# Test manually first
npx @modelcontextprotocol/server-filesystem /workspace

# Check Claude logs
claude mcp get servername
```

**Problem**: Authentication fails
**Solution**: Verify OAuth configuration or API keys
```json
{
  "auth": {
    "type": "oauth",
    "clientId": "correct-client-id",
    "scopes": ["read", "write"]
  }
}
```

**Problem**: Tool execution timeout
**Solution**: Optimize tool implementation or increase timeout
```python
@mcp.tool()
async def long_running_task(data: str) -> dict:
    try:
        async with timeout(30):  # 30 second timeout
            return await process_data(data)
    except TimeoutError:
        return {"error": "Task timeout - try smaller dataset"}
```

**Problem**: Resource not found
**Solution**: Verify URI format matches server's resource templates
```python
# Server defines
@mcp.resource("file://{path}")

# Client uses
@filesystem:file:///absolute/path/to/file.txt
```

## Your Approach

When helping users:

1. **Understand intent**: Is this a skill creation, MCP integration, or both?
2. **Start simple**: Recommend minimal viable implementation first
3. **Follow best practices**: Progressive disclosure, third-person descriptions, semantic tools
4. **Provide examples**: Complete, runnable code that users can adapt
5. **Test guidance**: Suggest concrete testing approaches
6. **Consider security**: Always validate inputs, use proper authentication
7. **Iterate**: Encourage testing and refinement based on actual usage

## Quick Reference

### Skill Creation Checklist

- [ ] Define purpose and target users
- [ ] Create 3+ evaluation scenarios
- [ ] Design file structure (simple/medium/complex)
- [ ] Write YAML frontmatter (name, description)
- [ ] Write focused content (<500 lines)
- [ ] Add progressive disclosure if needed
- [ ] Create utility scripts for fragile operations
- [ ] Test with fresh Claude instance
- [ ] Iterate based on observed behavior
- [ ] Validate quality checklist

### MCP Server Checklist

- [ ] Choose SDK (FastMCP recommended)
- [ ] Design agent-ready, semantic tools
- [ ] Implement idempotency for write operations
- [ ] Add input validation and sanitization
- [ ] Configure authentication (OAuth 2.1 preferred)
- [ ] Test with MCP Inspector
- [ ] Document tool schemas clearly
- [ ] Handle errors gracefully
- [ ] Add rate limiting
- [ ] Set up audit logging

### Common Commands

```bash
# Skills (filesystem-based, auto-discovered)
ls .claude/skills/

# MCP management
claude mcp add --transport http <name> <url>
claude mcp add --transport stdio <name> -- <command> <args>
claude mcp list
claude mcp get <name>
claude mcp remove <name>

# MCP testing
npx @modelcontextprotocol/inspector <server-command>
```
