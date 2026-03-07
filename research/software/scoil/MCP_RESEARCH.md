# Dagger MCP Integration Research

> **Date:** 2025-12-15
> **Source:** Research for cianfhoghlaim project

## Overview

Dagger has **native MCP (Model Context Protocol) support**, allowing any Dagger module to be exposed as an MCP server that Claude Code, Cursor, or other MCP clients can call directly.

## Key Documentation

- **Primary Source**: https://docs.dagger.io/features/llm
- **Daggerverse**: https://daggerverse.dev (community modules)

## How It Works

### Expose Dagger Module as MCP Server

```bash
# Start any Dagger module as an MCP server
dagger -m <module> mcp

# Examples:
dagger -m github.com/kpenfound/dag/puzzmo mcp
dagger -m ./my-local-module mcp
```

This command:
1. Starts the module as an MCP server using stdio transport
2. Exposes all module functions as MCP tools
3. Can be consumed by any MCP client (Claude Desktop, Cursor, Goose, etc.)

### MCP Configuration for Claude Desktop

```json
{
  "mcpServers": {
    "my-dagger-module": {
      "command": "dagger",
      "args": ["-m", "github.com/owner/module", "mcp"]
    }
  }
}
```

## Two MCP Patterns

1. **Expose Dagger Modules as MCP Servers** (Available Now)
   - Run `dagger -m <module> mcp` to expose module functions as tools
   - Any Dagger function becomes callable by LLMs via MCP
   - Works with local modules, Daggerverse modules, or GitHub URLs

2. **Connect to External MCP Servers from Dagger** (Coming Soon)
   - Dagger pipelines will be able to call external MCP servers
   - Enables pipelines to use MCP tools like Firecrawl, Browserbase, etc.

## Dagger LLM Integration

Dagger has built-in LLM support via the `LLM` type:

```go
// Within a Dagger module
func (m *MyModule) Analyze(ctx context.Context) (string, error) {
    llm := dag.LLM()

    // Add tools for the LLM to use
    llm = llm.WithTool(dag.Grep())
    llm = llm.WithTool(dag.MyCustomTool())

    // Run agent loop
    result, err := llm.Ask(ctx, "Analyze this codebase")
    return result.Text(), err
}
```

## Example: Wrap Pipeline in Dagger Module

Create a Dagger module that calls external pipelines:

```python
# dagger/src/main.py
import dagger
from dagger import function, object_type

@object_type
class MyPipeline:
    @function
    async def run_phase1(self) -> str:
        """Run Phase 1 mapping."""
        return await dagger.container() \
            .from_("python:3.11") \
            .with_workdir("/app") \
            .with_exec(["python", "run_mapping.py"]) \
            .stdout()

    @function
    async def run_phase2(self, target: str) -> str:
        """Run Phase 2 for a specific target."""
        return await dagger.container() \
            .from_("python:3.11") \
            .with_workdir("/app") \
            .with_exec(["python", "run_scraping.py", target]) \
            .stdout()
```

Then expose via MCP:
```bash
dagger -m ./dagger mcp
```

## Implementation Tasks

1. Create `dagger/` directory in project
2. Initialize Dagger module: `dagger init --sdk=python`
3. Implement wrapper functions
4. Test MCP exposure: `dagger -m ./dagger mcp`
5. Add to Claude Code MCP config

## References

- Dagger LLM docs: https://docs.dagger.io/features/llm
- Daggerverse: https://daggerverse.dev
- Dagger SDK docs: https://docs.dagger.io/sdk/python
