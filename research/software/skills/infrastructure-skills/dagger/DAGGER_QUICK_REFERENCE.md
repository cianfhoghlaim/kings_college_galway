# Dagger Patterns Quick Reference

## One-Page Cheat Sheet for Monorepo CI/CD

### Python (UV) Monorepo Pattern

**dagger.json**
```json
{
  "name": "my-dagger",
  "engineVersion": "v0.18.12",
  "sdk": {"source": "python"},
  "source": ".dagger"
}
```

**pyproject.toml (root)**
```toml
[tool.uv.workspace]
members = ["packages/core", "packages/api"]

[tool.uv.sources]
my-dagger = { path = ".dagger" }

[project.entry-points."dagger.mod"]
main_object = "my_dagger:MyDagger"
```

**Dockerfile (multi-stage UV)**
```dockerfile
FROM python:3.12-slim AS base
COPY --from=ghcr.io/astral-sh/uv:0.5.27 /uv /bin/uv
ENV UV_FROZEN=1 UV_COMPILE_BYTECODE=1

FROM base AS deps-prod
COPY pyproject.toml uv.lock ./
ARG PACKAGE
RUN uv sync --no-install-workspace --no-dev --package $PACKAGE

FROM base AS deps-dev
COPY pyproject.toml uv.lock ./
ARG PACKAGE
RUN uv sync --no-install-workspace --only-group dev --inexact

FROM deps-${INCLUDE:-prod} AS final
COPY . .
ARG PACKAGE
RUN uv sync --package $PACKAGE
```

**Dagger Module (.dagger/src/my_dagger/__init__.py)**
```python
from dagger import dag, function, object_type, Container

@object_type
class MyDagger:
    @function
    async def test(self, package: str, root_dir) -> str:
        # Parse uv.lock for dependencies
        import tomli
        uv_lock = tomli.loads(await root_dir.file("uv.lock").contents())
        
        # Get project dependencies from uv.lock
        # Build container, install deps, run tests
        container = (
            dag.container()
            .from("python:3.12-slim")
            .with_directory("/src", root_dir)
            .with_exec(["curl", "-LsSf", "https://astral.sh/uv/install.sh", "|", "sh"])
            .with_workdir("/src")
            .with_exec(["uv", "sync", "--package", package])
            .with_exec(["pytest"])
        )
        return await container.stdout()
```

---

### TypeScript (Bun/Turborepo) Pattern

**dagger.json**
```json
{
  "name": "my-dagger",
  "engineVersion": "v0.18.12",
  "sdk": {"source": "typescript"}
}
```

**src/index.ts**
```typescript
import { dag, object, func, Directory, Container } from "@dagger.io/dagger";

@object()
export class MyDagger {
  @func()
  async test(pkg: string): Promise<string> {
    return await dag
      .container()
      .from("oven/bun:latest")
      .withMountedCache("/root/.bun", dag.cacheVolume("bun-cache"))
      .withDirectory("/src", dag.host().directory("."))
      .withWorkdir("/src")
      .withExec(["bun", "install"])
      .withExec(["bunx", "turbo", "test", `--filter=${pkg}`])
      .stdout();
  }

  @func()
  async build(pkg: string): Promise<Directory> {
    return dag
      .container()
      .from("oven/bun:latest")
      .withMountedCache("/root/.bun", dag.cacheVolume("bun-cache"))
      .withDirectory("/src", dag.host().directory("."))
      .withWorkdir("/src")
      .withExec(["bun", "install"])
      .withExec(["bunx", "turbo", "build", `--filter=${pkg}`])
      .directory("/src/packages/{pkg}/dist");
  }
}
```

---

### Hybrid Monorepo Pattern

**Root Dagger (Python) orchestrates both**
```python
@object_type
class HybridCI:
    @function
    async def test_all(self, root_dir: RootDir) -> str:
        py_tests = await self.test_python_package(root_dir, "core")
        ts_tests = await self.test_typescript_package(root_dir, "web")
        return f"Python: {py_tests}\nTypeScript: {ts_tests}"

    @function
    async def test_python_package(self, root_dir: RootDir, pkg: str) -> str:
        # ... UV-based testing ...
        pass

    @function
    async def test_typescript_package(self, root_dir: RootDir, pkg: str) -> str:
        # ... Bun/Turbo-based testing ...
        pass
```

---

### Service Networking & Testing

**Port Binding**
```typescript
// Frontend tests with live backend
dag
  .container()
  .from("cypress/included:14.0.3")
  .withServiceBinding("localhost", backendService)  // Connect to port 8080
  .withServiceBinding("frontend", frontendService)  // Connect to port 80
  .withExec(["npm", "run", "test:e2e"])
```

**Service Creation**
```go
func (b *Backend) Serve() *dagger.Service {
    return b.Container().
        WithExposedPort(8080).
        AsService(dagger.ContainerAsServiceOpts{
            UseEntrypoint: true,
        })
}
```

---

### Caching Patterns

**Named Cache Volumes (persistent across runs)**
```typescript
// NPM cache
.withMountedCache("/root/.npm", dag.cacheVolume("npm-cache"))

// Bun cache
.withMountedCache("/root/.bun", dag.cacheVolume("bun-cache"))

// UV cache
.withMountedCache("/root/.cache/uv", dag.cacheVolume("uv-cache"))

// Build tools (Go, etc)
.withMountedCache("/root/.cache/go", dag.cacheVolume("go-cache"))
```

---

### Secret Management

**Setting Secrets**
```go
// From environment
secret := dag.SetSecret("api_token", os.Getenv("API_TOKEN"))

// As environment variable
container.WithSecretVariable("API_KEY", secret)

// As file
container.WithSecretFile("/run/secrets/key", secret)
```

**Using in LLM Agents**
```go
env := dag.Env().
    WithSecretInput("github_token", ghSecret, "GitHub access").
    WithStringInput("repo", "owner/repo", "Repository")

agent := dag.LLM(opts).WithEnv(env).Loop()
```

---

### LLM Agents (Agentic CI)

**Basic Agent**
```go
summary := dag.LLM(dagger.LLMOpts{Model: "claude-sonnet-4-0"}).
    WithPrompt("Your prompt here").
    LastReply(ctx)
```

**Agent with Workspace (can read/write/test code)**
```go
ws := dag.Workspace(sourceDir, backend.AsWorkspaceCheckable())

env := dag.Env().
    WithWorkspaceInput("workspace", ws, "workspace to modify").
    WithStringInput("task", "description", "what to do").
    WithWorkspaceOutput("result", "modified workspace")

agent := dag.LLM(opts).
    WithEnv(env).
    WithPromptFile(dag.CurrentModule().Source().File("prompts/task.md")).
    Loop()  // Keeps running until success

result := agent.Env().Output("result").AsWorkspace().Work()
```

**Prompt Files (external for easy iteration)**
```markdown
# prompts/task.md

You are a developer assistant.

## Task
$task

## Tools Available
You have access to a workspace with:
- read(path): read file
- write(path, content): write file  
- run(cmd): execute command
- check(): validate code

## Constraints
- Always test changes with check() before finishing
- Never stop until check() passes
```

**Token Tracking**
```go
totalTokens, _ := agent.TokenUsage().TotalTokens(ctx)
fmt.Printf("Tokens used: %d\n", totalTokens)
```

---

### GitHub Integration

**Reading Issues & PRs**
```go
gh := dag.GithubIssue(dagger.GithubIssueOpts{Token: ghToken})

issue := gh.Read(repo, issueId)
body, _ := issue.Body(ctx)
author, _ := issue.Author(ctx)
headRef, _ := issue.HeadRef(ctx)
```

**Creating PRs Programmatically**
```go
pr := gh.CreatePullRequest(
    repo,
    "Feature Title",
    "PR description",
    sourceDirectory,  // Your code changes
)

url, _ := pr.URL(ctx)
issueNum, _ := pr.IssueNumber(ctx)
```

**Posting Code Suggestions**
```go
gh.WritePullRequestCodeComment(
    ctx, repo, prNumber, commitSha,
    "```suggestion\nfixed code here\n```",
    "path/to/file.go",
    "RIGHT",  // RIGHT or LEFT side of diff
    lineNumber,
)
```

---

### Container Image Selection

**Security-first (Chainguard)**
```go
dag.Container().From("cgr.dev/chainguard/wolfi-base:latest@sha256:...")
```

**Tool-specific**
```
node           - Generic Node.js
oven/bun       - Bun runtime
python:3.12    - Python
golang:1.22    - Go
cypress/included:14.0.3  - Cypress for E2E
```

---

### Common dagger.json Configurations

**Python Module**
```json
{
  "name": "my-module",
  "engineVersion": "v0.18.12",
  "sdk": {"source": "python"},
  "source": ".dagger"
}
```

**Go Module with Dependencies**
```json
{
  "name": "greetings",
  "engineVersion": "v0.18.12",
  "sdk": {"source": "go"},
  "dependencies": [
    {"name": "backend", "source": ".dagger/backend"},
    {"name": "github-issue", "source": "github.com/org/dag-modules/github-issue"}
  ],
  "source": ".dagger"
}
```

**TypeScript Module with Workspaces**
```json
{
  "name": "summarizer-agent",
  "engineVersion": "v0.18.7",
  "sdk": {"source": "typescript"},
  "dependencies": [
    {"name": "reader-workspace", "source": "reader-workspace"}
  ]
}
```

---

### Testing Strategies

**Multi-Version Testing**
```python
versions = ["3.10", "3.11", "3.12"]
for version in versions:
    container = dag.container().from(f"python:{version}-slim")
    # Test on each version
```

**E2E Testing with Live Services**
```typescript
const backend = backendModule.serve();
const frontend = frontendModule.serve();

return dag
  .container()
  .from("cypress/included:14.0.3")
  .withServiceBinding("api", backend)
  .withServiceBinding("web", frontend)
  .withExec(["npm", "run", "test:e2e"])
  .stdout();
```

**Parallel Execution**
```python
import asyncio

results = await asyncio.gather(
    self.test_python(root_dir),
    self.test_typescript(root_dir),
    self.lint(root_dir),
)
```

---

### Debug & Development

**Debug Sleep for Inspection**
```python
container.with_exec(["sleep", "3600"])  # 1 hour for debugging
```

**Interactive Container**
```bash
dagger call test --debug-sleep 3600  # Keep running
docker exec -it <container> bash
```

**Verbose Output**
```go
dag.LLM(opts).WithVerbosity(3)  // Max verbosity
```

**Working with Diffs**
```go
diff, _ := agent.Env().
    Output("modified").
    AsWorkspace().
    Diff(ctx)  // Returns unified diff
```

---

### Performance Tips

1. **Order matters**: Put changing code layers last
2. **Use specific image tags with sha256**: Avoids re-pulling
3. **Mount caches early**: Name them consistently for reuse
4. **Lazy evaluation**: Dagger runs only what's needed
5. **Parallel tasks**: Use async/await or goroutines
6. **Small base images**: Chainguard/alpine/slim variants
7. **Cache warming**: Pre-fetch large models/dependencies

---

### Common Mistakes

- Forgetting to mount cache volumes
- Using latest image tags (not reproducible)
- Hardcoding paths instead of using DefaultPath
- Not returning Directory/File from operations
- Mixing stateful/stateless operations
- Forgetting WithWorkdir for relative paths
- Not awaiting async functions
- Secrets in environment output

