# Comprehensive Analysis of Dagger Examples

This document provides detailed patterns extracted from three primary Dagger examples in the infrastructure directory, tailored for a monorepo managing both Python (uv) and TypeScript (bun/turborepo) workspaces.

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [1. uv-dagger-dream: Python Monorepo Pattern](#1-uv-dagger-dream-python-monorepo-pattern)
3. [2. greetings-api: Full CI/CD with Agentic Features](#2-greetings-api-full-cicd-with-agentic-features)
4. [3. technical-content-summarizer: TypeScript & AI Agents](#3-technical-content-summarizer-typescript--ai-agents)
5. [Cross-Cutting Patterns](#cross-cutting-patterns)
6. [Adaptation for Hybrid Monorepo](#adaptation-for-hybrid-monorepo)

---

## Architecture Overview

### Dagger Fundamentals
- **Language Support**: Python SDK, Go SDK, TypeScript SDK
- **Module Composition**: Dependency-based module composition
- **Container-First**: All operations wrapped in containers
- **Caching**: Multi-layer caching (pip, npm, build artifacts)
- **Secrets**: First-class secret management for tokens/credentials
- **Declarative API**: Functions decorated with `@function` (Python) or `@func()` (TypeScript)

---

## 1. uv-dagger-dream: Python Monorepo Pattern

### Project Structure
```
uv-dagger-dream/
├── .dagger/
│   ├── src/monorepo_dagger/
│   │   └── main.py          # Core Dagger module
│   ├── pyproject.toml        # Dagger module metadata
│   └── uv.lock
├── projects/
│   ├── lib-one/
│   ├── lib-two/
│   └── ...
├── weird-location/nested/lib-three/
├── pyproject.toml           # Root workspace config
├── uv.lock
└── Dockerfile               # Multi-stage dependency build
```

### Key Patterns

#### 1a. Root Workspace Configuration
**File**: `pyproject.toml`
```toml
[project]
name = "uv-dagger-dream"
version = "0.1.0"
requires-python = ">=3.12"

[dependency-groups]
dev = ["pyright>=1.1.394", "ruff>=0.9.7"]
dagger = ["monorepo-dagger"]

[tool.uv.workspace]
members = [
    "projects/lib-one",
    "projects/lib-two",
    "weird-location/nested/lib-three"  # Flexible member location
]

[tool.uv.sources]
monorepo-dagger = { path = ".dagger" }

[project.entry-points."dagger.mod"]
main_object = "monorepo_dagger:MonorepoDagger"
```

**Key Insights**:
- UV workspace supports members in arbitrary locations
- Dagger modules registered via entry points
- Dependency groups allow dev-only tools
- Local path sources enable in-workspace Dagger modules

#### 1b. Dagger Module Metadata
**File**: `.dagger/pyproject.toml`
```toml
[project]
name = "monorepo-dagger"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "dagger-io",
    "tomli>=2.2.1",  # For parsing uv.lock
]

[build-system]
requires = ["hatchling==1.25.0"]
build-backend = "hatchling.build"
```

#### 1c. Multi-Stage Dockerfile for UV
**File**: `Dockerfile`
```dockerfile
# options: prod,dev
ARG INCLUDE_DEPENDENCIES=dev
ARG PYTHON_VERSION=3.12.8

FROM python:${PYTHON_VERSION}-slim AS base

ENV DEBIAN_FRONTEND=noninteractive
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    apt-get update && apt-get install -y git curl gcc libpq-dev

# Install uv
COPY --from=ghcr.io/astral-sh/uv:0.5.27 /uv /bin/uv

ENV UV_PROJECT_ENVIRONMENT=/usr/local/ \
    UV_PYTHON=/usr/local/bin/python \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_FROZEN=1

# Production dependencies only (no workspace)
FROM base AS deps-prod
WORKDIR /src
COPY pyproject.toml uv.lock ./
ARG PACKAGE
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --no-install-workspace --all-extras --no-dev --package $PACKAGE

# Development dependencies (includes dev tools)
FROM deps-prod AS deps-dev
ARG PACKAGE
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --no-install-workspace --only-group dev --inexact && \
    uv sync --no-install-workspace --all-extras --inexact --package $PACKAGE

FROM deps-${INCLUDE_DEPENDENCIES} AS final
ARG PACKAGE
COPY . .
RUN uv sync --all-extras --inexact --package $PACKAGE
```

**Multi-stage Strategy**:
1. `deps-prod`: Minimal runtime dependencies
2. `deps-dev`: Includes testing & linting tools
3. `final`: Complete project with source code
4. Selective build target based on use case (e.g., for testing use `deps-dev`)

#### 1d. Core Dagger Module Implementation
**File**: `.dagger/src/monorepo_dagger/main.py`

```python
from typing import Annotated, TypeAlias
import dagger
import tomli
from dagger import (
    BuildArg, Container, DefaultPath, File, Ignore, dag, function, object_type,
)

# Define ignored files for context optimization
IGNORE = Ignore([
    ".env", ".git", "**/.venv", "**__pycache__**",
    ".dagger/sdk", "**/.pytest_cache", "**/.ruff_cache",
])

# Type aliases with path and ignore annotations
RootDir: TypeAlias = Annotated[
    dagger.Directory,
    DefaultPath("."),
    IGNORE,
]

SourceDir: TypeAlias = Annotated[
    dagger.Directory,
    IGNORE,
]

@object_type
class MonorepoDagger:
    @function
    async def build_project(
        self,
        root_dir: RootDir,
        project: str,
        debug_sleep: float = 0.0,
    ) -> Container:
        """Build container with project and dependencies."""
        # Step 1: Create container with third-party deps only
        container = self.container_with_third_party_dependencies(
            pyproject_toml=root_dir.file("pyproject.toml"),
            uv_lock=root_dir.file("uv.lock"),
            dockerfile=root_dir.file("Dockerfile"),
            project=project,
        )

        # Step 2: Map project sources (resolves dependency tree)
        project_sources_map = await self.get_project_sources_map(
            root_dir.file("uv.lock"), project
        )

        # Step 3: Copy source code for project and its dependencies
        container = self.copy_source_code(container, root_dir, project_sources_map)
        container = container.with_exec(["sleep", str(debug_sleep)])

        # Step 4: Install local dependencies in editable mode
        container = self.install_local_dependencies(container, project)

        # Step 5: Set working directory to project
        container = container.with_workdir(f"/src/{project_sources_map[project]}")

        return container

    def container_with_third_party_dependencies(
        self,
        pyproject_toml: File,
        uv_lock: File,
        dockerfile: File,
        project: str,
    ) -> Container:
        """Build deps-dev layer without source code."""
        build_context = (
            dag.directory()
            .with_file("pyproject.toml", pyproject_toml)
            .with_file("uv.lock", uv_lock)
            .with_file("/Dockerfile", dockerfile)
            .with_new_file("README.md", "Dummy README.md")
        )

        return build_context.docker_build(
            target="deps-dev",
            dockerfile="/Dockerfile",
            build_args=[BuildArg(name="PACKAGE", value=project)],
        )

    async def get_project_sources_map(
        self,
        uv_lock: File,
        project: str,
    ) -> dict[str, str]:
        """Recursively find all local dependencies for a project."""
        uv_lock_dict = tomli.loads(await uv_lock.contents())
        members = set(uv_lock_dict["manifest"]["members"])

        local_projects = {project}

        def find_deps_for_package(package_name: str):
            for package in uv_lock_dict["package"]:
                if package["name"] == package_name:
                    dependencies = package.get("dependencies", [])
                    for dep in dependencies:
                        if isinstance(dep, dict) and dep.get("name") in members:
                            local_projects.add(dep["name"])
                            find_deps_for_package(dep["name"])

        find_deps_for_package(project)

        # Map package names to their source directories
        project_sources_map = {}
        for package in uv_lock_dict["package"]:
            if package["name"] in local_projects:
                project_sources_map[package["name"]] = package["source"]["editable"]

        return project_sources_map

    def copy_source_code(
        self,
        container: Container,
        root_dir: RootDir,
        project_sources_map: dict[str, str],
    ) -> Container:
        """Copy source code for project and dependencies."""
        for project, project_source_path in project_sources_map.items():
            container = container.with_directory(
                f"/src/{project_source_path}",
                root_dir.directory(project_source_path),
            )
        return container

    def install_local_dependencies(
        self, container: Container, project: str
    ) -> Container:
        """Install project and dependencies in editable mode."""
        return container.with_exec([
            "uv", "sync",
            "--inexact",
            "--package", project,
        ])

    @function
    async def pytest(self, root_dir: RootDir, project: str) -> str:
        """Run pytest for a project."""
        container = await self.build_project(root_dir, project)
        return await container.with_exec(["pytest"]).stdout()

    @function
    async def pyright(self, root_dir: RootDir, project: str) -> str:
        """Run type checking."""
        container = await self.build_project(root_dir, project)
        return await container.with_exec(["pyright"]).stdout()
```

**Key Patterns**:
1. **Dependency Graph Resolution**: Parses `uv.lock` to find all local dependencies
2. **Incremental Building**: Separates deps layer from source layer for better caching
3. **Editable Installs**: Uses `uv sync --package` for single-package editable installs
4. **Type Annotations**: Uses `TypeAlias` with `DefaultPath` and `Ignore` for smart path handling
5. **Recursive Traversal**: Walks dependency tree to find all local projects

---

## 2. greetings-api: Full CI/CD with Agentic Features

### Project Structure
```
greetings-api/
├── main.go                      # Backend API
├── main_test.go
├── greetings.json              # Data
├── website/                     # Frontend
│   ├── index.html
│   ├── package.json
│   └── cypress/               # E2E tests
├── .dagger/
│   ├── main.go                # Root module
│   ├── backend/main.go        # Backend submodule
│   ├── frontend/src/index.ts  # Frontend submodule
│   ├── develop.go             # Agent: develop features
│   ├── review.go              # Agent: code review
│   ├── debugger.go            # Agent: fix tests
│   ├── prompts/               # LLM prompts
│   │   ├── assignment.md
│   │   ├── review.md
│   │   └── fix_tests.md
│   └── workspace/             # Workspace for agent I/O
├── dagger.json
└── CONTRIBUTING.md
```

### Key Patterns

#### 2a. Root Module with Submodules
**File**: `.dagger/main.go`

```go
package main

import (
    "context"
    "fmt"
    "github.com/kpenfound/greetings-api/.dagger/internal/dagger"
)

type Greetings struct {
    // +private
    Source   *dagger.Directory
    Repo     string  // GitHub repo
    Image    string  // Container image
    App      string  // Netlify app name
    Backend  *dagger.Backend   // Submodule
    Frontend *dagger.Frontend  // Submodule
}

func New(
    // +optional
    // +defaultPath="/"
    // +ignore=[".git", "**/node_modules"]
    source *dagger.Directory,
    // +optional
    // +default="github.com/kpenfound/greetings-api"
    repo string,
    // +optional
    // +default="kylepenfound/greetings-api:latest"
    image string,
    // +optional
    // +default="dagger-demo"
    app string,
) *Greetings {
    g := &Greetings{
        Source:   source,
        Repo:     repo,
        Image:    image,
        App:      app,
        Backend:  dag.Backend(source.WithoutDirectory("website")),
    }
    g.Frontend = dag.Frontend(source.Directory("website"), g.Backend.Serve())
    return g
}

// Orchestrate CI checks
func (g *Greetings) Check(
    ctx context.Context,
    githubToken *dagger.Secret,      // For commenting
    commit string,                    // Commit hash
    model string,                     // LLM model
) (string, error) {
    // Run linting
    lintOut, err := g.Lint(ctx)
    if err != nil {
        // Debug failures if token provided
        if githubToken != nil {
            _ = g.DebugBrokenTestsPr(ctx, githubToken, commit, model)
        }
        return "", err
    }

    // Run tests
    testOut, err := g.Test(ctx)
    if err != nil {
        if githubToken != nil {
            _ = g.DebugBrokenTestsPr(ctx, githubToken, commit, model)
        }
        return "", err
    }

    // Build
    _, err = g.Build().Sync(ctx)
    if err != nil {
        return "", err
    }

    return lintOut + "\n\n" + testOut, nil
}

// Delegate to submodules
func (g *Greetings) Test(ctx context.Context) (string, error) {
    backendResult, err := g.Backend.UnitTest(ctx)
    if err != nil {
        return "", err
    }

    frontendResult, err := g.Frontend.UnitTest(ctx)
    if err != nil {
        return "", err
    }

    return backendResult + "\n" + frontendResult, nil
}

func (g *Greetings) Lint(ctx context.Context) (string, error) {
    backendResult, err := g.Backend.Lint(ctx)
    if err != nil {
        return "", err
    }

    frontendResult, err := g.Frontend.Lint(ctx)
    if err != nil {
        return "", err
    }
    return backendResult + "\n" + frontendResult, nil
}

func (g *Greetings) Build() *dagger.Directory {
    return dag.Directory().
        WithFile("/build/greetings-api", g.Backend.Binary()).
        WithDirectory("build/website/", g.Frontend.Build())
}

func (g *Greetings) Serve() *dagger.Service {
    backendService := g.Backend.Serve()
    frontendService := g.Frontend.Serve()

    return dag.Proxy().
        WithService(backendService, "backend", 8080, 8080).
        WithService(frontendService, "frontend", 8081, 80).
        Service()
}

func (g *Greetings) Release(
    ctx context.Context,
    tag string,
    ghToken *dagger.Secret,
) (string, error) {
    build := g.Build()
    // Compress assets
    assets := dag.Container().From("alpine:3.18").
        WithDirectory("/assets", build).
        WithWorkdir("/assets/build").
        WithExec([]string{"tar", "czf", "website.tar.gz", "website/"}).
        WithExec([]string{"rm", "-r", "website"}).
        Directory("/assets/build")
    _, _ = assets.Sync(ctx)

    title := fmt.Sprintf("Release %s", tag)
    return dag.GithubRelease().Create(ctx, g.Repo, tag, title, ghToken, ...)
}
```

**Composition Pattern**:
- Root module delegates to `Backend` and `Frontend` submodules
- Submodules handle language-specific operations
- Root orchestrates workflow (lint → test → build)

#### 2b. Backend Submodule (Go)
**File**: `.dagger/backend/main.go`

```go
package main

import (
    "context"
    "runtime"
    "backend/internal/dagger"
)

type Backend struct {
    Source *dagger.Directory
}

func New(source *dagger.Directory) *Backend {
    return &Backend{Source: source}
}

// Unit tests
func (b *Backend) UnitTest(ctx context.Context) (string, error) {
    return dag.
        Golang().
        WithSource(b.Source).
        Test(ctx)
}

// Linting with golangci-lint
func (b *Backend) Lint(ctx context.Context) (string, error) {
    return dag.
        Golang().
        WithSource(b.Source).
        GolangciLint(ctx)
}

// Format and fix
func (b *Backend) Format() *dagger.Directory {
    return dag.
        Golang().
        WithSource(b.Source).
        Fmt().
        GolangciLintFix()
}

// Build binary
func (b *Backend) Build(arch string) *dagger.Directory {
    if arch == "" {
        arch = runtime.GOARCH
    }
    return dag.
        Golang().
        WithSource(b.Source).
        Build([]string{}, dagger.GolangBuildOpts{Arch: arch})
}

// Get compiled binary
func (b *Backend) Binary(arch string) *dagger.File {
    d := b.Build(arch)
    return d.File("greetings-api")
}

// Container for runtime
func (b *Backend) Container(arch string) *dagger.Container {
    if arch == "" {
        arch = runtime.GOARCH
    }
    bin := b.Binary(arch)
    return dag.
        Container(dagger.ContainerOpts{Platform: dagger.Platform(arch)}).
        From("cgr.dev/chainguard/wolfi-base:latest@sha256:...").
        WithFile("/bin/greetings-api", bin).
        WithEntrypoint([]string{"/bin/greetings-api"}).
        WithExposedPort(8080)
}

// Service for networking
func (b *Backend) Serve() *dagger.Service {
    return b.Container(runtime.GOARCH).AsService(...)
}

// Stateless operations for agents
func (b *Backend) CheckDirectory(
    ctx context.Context,
    source *dagger.Directory,
) (string, error) {
    b.Source = source
    return b.Check(ctx)
}

func (b *Backend) FormatDirectory(
    source *dagger.Directory,
) *dagger.Directory {
    b.Source = source
    return b.Format()
}

func (b *Backend) AsWorkspaceCheckable() *dagger.BackendCheckable {
    // Returns implementation of Checkable interface for agents
    return &backendCheckable{backend: b}
}
```

**Key Patterns**:
- Uses DAG module dependencies (Golang, Container)
- Supports multi-architecture builds
- Provides stateless operations that accept directories (for agent re-entrancy)
- Returns Service for port binding and inter-service communication

#### 2c. Frontend Submodule (TypeScript)
**File**: `.dagger/frontend/src/index.ts`

```typescript
import { dag, Directory, object, func, Service } from "@dagger.io/dagger";

@object()
export class Frontend {
  @func()
  source: Directory;
  backend: Service;

  constructor(source: Directory, backend: Service) {
    this.source = source;
    this.backend = backend;
  }

  @func()
  async lint(): Promise<string> {
    return dag
      .container()
      .from("node")
      .withMountedCache("/root/.npm", dag.cacheVolume("npm-cache"))
      .withWorkdir("/app")
      .withDirectory("/app", this.source)
      .withExec(["npm", "ci"])
      .withExec(["npm", "run", "lint"])
      .stdout();
  }

  @func()
  format(): Directory {
    return dag
      .container()
      .from("node")
      .withMountedCache("/root/.npm", dag.cacheVolume("npm-cache"))
      .withWorkdir("/app")
      .withDirectory("/app", this.source)
      .withExec(["npm", "ci"])
      .withExec(["npm", "run", "lint"])
      .directory("/app");
  }

  @func()
  async unitTest(): Promise<string> {
    return await dag
      .container()
      .from("cypress/included:14.0.3")
      .withMountedCache("/root/.npm", dag.cacheVolume("npm-cache"))
      .withServiceBinding("localhost", this.backend)    // Backend available as localhost
      .withServiceBinding("frontend", this.serve())      // Frontend self-reference
      .withWorkdir("/app")
      .withDirectory("/app", this.source)
      .withExec(["npm", "ci"])
      .withExec(["npm", "run", "test:e2e"])
      .stdout();
  }

  @func()
  build(): Directory {
    return this.source;  // Already built static files
  }

  @func()
  serve(): Service {
    return dag
      .container()
      .from("nginx")
      .withDirectory("/usr/share/nginx/html", this.source)
      .asService({ useEntrypoint: true });
  }
}
```

**Key Patterns**:
- Cache volumes for npm dependencies
- Service bindings for inter-service communication
- Cypress for E2E tests with live services
- Immutable service definitions

#### 2d. Agentic CI: Develop Agent
**File**: `.dagger/develop.go`

```go
// Complete an assignment for the greetings project
func (g *Greetings) Develop(
    ctx context.Context,
    assignment string,
    model string,  // "claude-sonnet-4-0"
) *dagger.Directory {
    prompt := dag.CurrentModule().Source().File("prompts/assignment.md")

    // Create workspace with checkable backend
    ws := dag.Workspace(
        g.Source,
        g.Backend.AsWorkspaceCheckable(),
    )

    // Setup LLM environment
    env := dag.Env().
        WithWorkspaceInput("workspace", ws, "workspace to read, write, and test code").
        WithStringInput("assignment", assignment, "the assignment to complete").
        WithWorkspaceOutput("completed", "workspace with developed solution")

    // Run agent loop until success
    agent := dag.LLM(dagger.LLMOpts{Model: model}).
        WithEnv(env).
        WithPromptFile(prompt).
        Loop()  // Keeps running until successful

    // Track token usage
    totalTokens, _ := agent.TokenUsage().TotalTokens(ctx)
    fmt.Printf("Total token usage: %d\n", totalTokens)

    // Return completed work
    return agent.Env().
        Output("completed").
        AsWorkspace().
        Work()
}

// Create PR from issue
func (g *Greetings) DevelopPullRequest(
    ctx context.Context,
    githubToken *dagger.Secret,
    issueId int,
    model string,
) (string, error) {
    gh := dag.GithubIssue(dagger.GithubIssueOpts{Token: githubToken})
    
    // Get issue body as assignment
    issue := gh.Read(g.Repo, issueId)
    assignment, _ := issue.Body(ctx)

    // Develop solution
    work := g.Develop(ctx, assignment, model)

    // Generate PR title with LLM
    title, _ := dag.LLM(dagger.LLMOpts{Model: model}).
        WithPrompt("Write PR title (under 150 chars, nothing else):\n" + assignment).
        LastReply(ctx)

    // Create PR
    body := fmt.Sprintf("%s\n\nCompleted by Agent\nFixes https://%s/issues/%d\n", 
        assignment, g.Repo, issueId)
    pr := gh.CreatePullRequest(g.Repo, strings.TrimSpace(title), body, work)

    // Auto-trigger review
    id, _ := pr.IssueNumber(ctx)
    if id > 0 {
        _ = g.PullRequestReview(ctx, githubToken, id, model)
    }

    return pr.URL(ctx)
}

// Agent feedback loop
func (g *Greetings) DevelopFeedback(
    ctx context.Context,
    source *dagger.Directory,
    assignment string,
    diff string,
    feedback string,
    model string,
) (*dagger.Directory, error) {
    prompt := dag.CurrentModule().Source().File("prompts/feedback.md")

    ws := dag.Workspace(source, g.Backend.AsWorkspaceCheckable())

    env := dag.Env().
        WithWorkspaceInput("workspace", ws, "workspace to read, write, and test code").
        WithStringInput("description", assignment, "the description").
        WithStringInput("feedback", feedback, "the feedback").
        WithStringInput("diff", diff, "the git diff").
        WithWorkspaceOutput("completed", "workspace with feedback implemented")

    agent := dag.LLM(dagger.LLMOpts{Model: model}).
        WithEnv(env).
        WithPromptFile(prompt).
        Loop()

    return agent.Env().
        Output("completed").
        AsWorkspace().
        Work(), nil
}

// Receive feedback via GitHub comment
func (g *Greetings) PullRequestFeedback(
    ctx context.Context,
    githubToken *dagger.Secret,
    issueId int,
    feedback string,
    model string,
) error {
    feedback = strings.ReplaceAll(feedback, "/agent ", "")

    gh := dag.GithubIssue(dagger.GithubIssueOpts{Token: githubToken})
    issue := gh.Read(g.Repo, issueId)
    description, _ := issue.Body(ctx)
    headRef, _ := issue.HeadRef(ctx)
    diffURL, _ := issue.DiffURL(ctx)
    diff, _ := dag.HTTP(diffURL).Contents(ctx)

    head := dag.Git(g.Repo).Ref(headRef).Tree()
    completed, _ := g.DevelopFeedback(ctx, head, description, diff, feedback, model)

    return gh.CreatePullRequestCommit(ctx, g.Repo, completed, headRef)
}
```

**Agentic Patterns**:
1. **Workspace I/O**: LLM gets workspace with tools to read/write/test code
2. **Checkable Interface**: Backend provides validation interface for agent
3. **Prompt Files**: External prompt files for agent behavior
4. **Loop Pattern**: Agent keeps running until success
5. **Token Tracking**: Monitor LLM usage
6. **GitHub Integration**: Read issues, create PRs, handle feedback loops

#### 2e. Review Agent
**File**: `.dagger/review.go`

```go
func (g *Greetings) DevelopReview(
    ctx context.Context,
    source *dagger.Directory,
    assignment string,
    diff string,
    model string,
) (string, error) {
    prompt := dag.CurrentModule().Source().File("prompts/review.md")

    ws := dag.Workspace(source, g.Backend.AsWorkspaceCheckable())

    env := dag.Env().
        WithWorkspaceInput("workspace", ws, "workspace to read and test").
        WithStringInput("description", assignment, "PR description").
        WithStringInput("diff", diff, "git diff").
        WithStringOutput("review", "code review")

    agent := dag.LLM(dagger.LLMOpts{Model: model}).
        WithEnv(env).
        WithPromptFile(prompt).
        Loop()

    return agent.Env().Output("review").AsString(ctx)
}

func (g *Greetings) PullRequestReview(
    ctx context.Context,
    githubToken *dagger.Secret,
    issueId int,
    model string,
) error {
    gh := dag.GithubIssue(dagger.GithubIssueOpts{Token: githubToken})
    issue := gh.Read(g.Repo, issueId)
    description, _ := issue.Body(ctx)
    headRef, _ := issue.HeadRef(ctx)
    diffURL, _ := issue.DiffURL(ctx)
    diff, _ := dag.HTTP(diffURL).Contents(ctx)

    head := dag.Git(g.Repo).Ref(headRef).Tree()
    review, _ := g.DevelopReview(ctx, head, description, diff, model)

    // Post review as comment
    commentErr := gh.WriteComment(ctx, g.Repo, issueId, review)

    // If PR author is bot, trigger feedback loop
    author, _ := issue.Author(ctx)
    if author == "agent-kal[bot]" {
        feedback := fmt.Sprintf(`Received feedback:\n<feedback>%s</feedback>
If requesting changes, make them. If feedback on solution, update .dagger/prompts/assignment.md.
If feedback for all contributors, update CONTRIBUTING.md.`, review)
        _ = g.PullRequestFeedback(ctx, githubToken, issueId, feedback, model)
    }

    return commentErr
}
```

**Review Pattern**:
- Automated code review on PRs
- Self-improving feedback loop (updates prompts based on review)
- Markdown output for GitHub comments

#### 2f. Debugger Agent
**File**: `.dagger/debugger.go`

```go
func (g *Greetings) DebugTests(
    ctx context.Context,
    model string,
) (string, error) {
    prompt := dag.CurrentModule().Source().File("prompts/fix_tests.md")

    // Check backend tests
    if _, berr := g.Backend.CheckDirectory(ctx, g.Backend.Source()); berr != nil {
        ws := dag.Workspace(g.Backend.Source(), g.Backend.AsWorkspaceCheckable())
        env := dag.Env().
            WithWorkspaceInput("workspace", ws, "workspace to read and write").
            WithWorkspaceOutput("fixed", "workspace with fixed tests")

        return dag.LLM(dagger.LLMOpts{Model: model}).
            WithEnv(env).
            WithPromptFile(prompt).
            Env().
            Output("fixed").
            AsWorkspace().
            Diff(ctx)  // Return unified diff
    }

    // Check frontend tests
    if _, ferr := g.Frontend.CheckDirectory(ctx, g.Frontend.Source()); ferr != nil {
        ws := dag.Workspace(g.Frontend.Source(), g.Frontend.AsWorkspaceCheckable())
        env := dag.Env().
            WithWorkspaceInput("workspace", ws, "workspace to read and write").
            WithWorkspaceOutput("fixed", "workspace with fixed tests")

        return dag.LLM(dagger.LLMOpts{Model: model}).
            WithEnv(env).
            WithPromptFile(prompt).
            Env().
            Output("fixed").
            AsWorkspace().
            Diff(ctx)
    }

    return "", fmt.Errorf("no broken tests found")
}

func (g *Greetings) DebugBrokenTestsPr(
    ctx context.Context,
    githubToken *dagger.Secret,
    commit string,
    model string,
) error {
    gh := dag.GithubIssue(dagger.GithubIssueOpts{Token: githubToken})
    
    // Find PR from commit
    gitRef := dag.Git(g.Repo).Commit(commit)
    gitSource := gitRef.Tree()
    pr, _ := gh.GetPrForCommit(ctx, g.Repo, commit)

    // Reset to PR state
    g = New(gitSource, g.Repo, g.Image, g.App)

    // Get fix suggestions
    suggestionDiff, _ := g.DebugTests(ctx, model)

    // Parse diff and convert to code suggestions
    codeSuggestions := parseDiff(suggestionDiff)

    // Post as PR code comments
    for _, suggestion := range codeSuggestions {
        markupSuggestion := "```suggestion\n" + strings.Join(suggestion.Suggestion, "\n") + "\n```"
        _ = gh.WritePullRequestCodeComment(
            ctx, g.Repo, pr, commit, markupSuggestion,
            suggestion.File, "RIGHT", suggestion.Line)
    }

    return nil
}
```

**Debugger Pattern**:
- Automatically detects broken tests
- Generates fix suggestions
- Posts as PR code suggestions (GitHub's suggestion feature)
- Diff-based review format

#### 2g. LLM Prompts
**File**: `.dagger/prompts/assignment.md`
```markdown
You are a programmer working on the Greetings API project

## Problem solving process

1. Consider the assignments intent
2. Evaluate the architecture at `## Project Architecture` in CONTRIBUTING.md
3. Understand how assignment should be implemented
4. Implement the assignment in workspace provided
5. Run the checks and incorporate changes needed

## Assignment

Here is your assignment: $assignment

## Constraints
- You have access to a workspace with code and tests
- The workspace has tools to read, write, and run tests
- Be sure to always write changes to workspace
- Always run check after writing changes
- You are not done until check tool is successful and assignment is complete
```

**Prompt Engineering**:
- External prompt files for easy iteration
- Template variables like `$assignment`
- Clear constraints and success criteria

---

## 3. technical-content-summarizer: TypeScript & AI Agents

### Project Structure
```
technical-content-summarizer/
├── src/
│   └── index.ts              # Main agent
├── reader-workspace/
│   ├── src/
│   │   └── index.ts          # Workspace implementation
│   ├── dagger.json
│   └── package.json
├── dagger.json
└── package.json
```

### Key Patterns

#### 3a. Main Agent Implementation
**File**: `src/index.ts`

```typescript
import { dag, object, func } from "@dagger.io/dagger";

@object()
export class TechSummarizerAgent {
  /**
   * Summarize content of a URL using AI agent with validation tools
   */
  @func()
  async summarize(
    url: string,
    minLength: number = 100,
    maxLength: number = 200,
    forbiddenWords: string[] = [],
  ): Promise<string> {
    // Create workspace with validation tools
    const ws = dag.readerWorkspace(minLength, maxLength, forbiddenWords);

    // Setup LLM environment with inputs
    const env = dag
      .env()
      .withReaderWorkspaceInput("workspace", ws, "The workspace to use")
      .withStringInput("url", url, "The URL for summarization");

    // Run LLM with agent loop
    const summary = dag
      .llm()
      .withEnv(env)
      .withPrompt(
        `You are an experienced technical writer responsive to feedback.
You have been given access to a workspace.

$url

You must use the provided workspace check-content tool to verify your summary and respond to corrections it issues. DO NOT STOP UNTIL THE SUMMARY PASSES THE CHECK-CONTENT TOOL.`,
      )
      .lastReply();

    return summary;
  }
}
```

**Agent Loop Pattern**:
- Workspace provides tools for validation
- LLM gets feedback loop
- Agent keeps iterating until validation passes
- Variable substitution in prompts

#### 3b. Reader Workspace Implementation
**File**: `reader-workspace/src/index.ts`

```typescript
import * as cheerio from "cheerio";
import { object, func } from "@dagger.io/dagger";

@object()
export class ReaderWorkspace {
  private minLength: number;
  private maxLength: number;
  private forbiddenWords: string[];
  private plainTextContent: string;

  constructor(minLength: number, maxLength: number, forbiddenWords: string[]) {
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.forbiddenWords = forbiddenWords;
  }

  /**
   * Get content from URL and return as plain text
   */
  @func()
  async getContent(url: string): Promise<string> {
    if (this.plainTextContent) {
      return this.plainTextContent;
    }

    let html = (await fetch(url)).text();
    let $ = cheerio.load(await html);

    // Remove script, style, and navigation elements
    $("script, source, style, head, img, svg, a, form, link, iframe").remove();
    $("*").removeClass();

    // Remove data attributes
    $("*").each((_, el) => {
      if (el.type === "tag" || el.type === "script" || el.type === "style") {
        for (const attr of Object.keys(el.attribs || {})) {
          if (attr.startsWith("data-")) {
            $(el).removeAttr(attr);
          }
        }
      }
    });

    const content = $("body").text().replace(/\s+/g, " ");
    this.plainTextContent = content;

    return content;
  }

  /**
   * Validate content against constraints
   * Throws error if invalid, returns true if valid
   */
  @func()
  checkContent(content: string): boolean {
    if (!content || content.length === 0) {
      throw new Error("You produced no content! Write something!");
    }

    if (content.length > this.maxLength) {
      throw new Error(
        "You wrote too much! The maximum length is " +
          this.maxLength +
          " characters.",
      );
    }

    if (content.length < this.minLength) {
      throw new Error(
        "You wrote too little! The minimum length is " +
          this.minLength +
          " characters.",
      );
    }

    if (
      this.forbiddenWords.some((word) =>
        content.toLowerCase().includes(" " + word.toLowerCase() + " "),
      )
    ) {
      const foundWord = this.forbiddenWords.find((word) =>
        content.toLowerCase().includes(word.toLowerCase()),
      );
      throw new Error("You used a forbidden word! Never use: " + foundWord);
    }

    return true;
  }
}
```

**Workspace Pattern**:
- Stateful workspace with mutable properties
- Tool functions that agents can call
- Validation errors provide feedback to agent
- Caching (plainTextContent for multiple calls)

#### 3c. dagger.json Configuration
**File**: `dagger.json`
```json
{
  "name": "tech-summarizer-agent",
  "engineVersion": "v0.18.7",
  "sdk": {
    "source": "typescript"
  },
  "dependencies": [
    {
      "name": "reader-workspace",
      "source": "reader-workspace"
    }
  ]
}
```

**Dependency Resolution**:
- Workspace module declared as dependency
- Local path resolution for monorepo-style workspaces

---

## Cross-Cutting Patterns

### 1. Caching Strategies

#### NPM Cache
```typescript
.withMountedCache("/root/.npm", dag.cacheVolume("npm-cache"))
.withExec(["npm", "ci"])
```

#### Python/UV Cache
```dockerfile
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --no-install-workspace --all-extras --no-dev
```

#### Build Output Cache
```go
// Golang modules are cached automatically
dag.Golang().WithSource(source).Build()
```

**Cache Volume Keys**:
- Named cache volumes persist across runs
- Mounted at specific paths based on tool
- Shared between steps/agents when named identically

### 2. Secret Management

#### Environment Variables
```go
secret := client.SetSecret("name", value)
container.WithSecretVariable("ENV_VAR_NAME", secret)
```

#### File Secrets
```go
container.WithSecretFile("/run/secrets/token", ghToken)
```

#### Usage in Development Agent
```go
env := dag.Env().
    WithWorkspaceInput("workspace", ws, "desc").
    WithStringInput("key", value, "desc")

agent := dag.LLM(opts).WithEnv(env).Loop()
```

### 3. Service Networking

#### Service Binding (Port Mapping)
```typescript
.withServiceBinding("localhost", this.backend)  // Port 8080
.withServiceBinding("frontend", this.serve())   // Port 80
```

#### Usage in Tests
```typescript
dag.container()
  .from("cypress/included:14.0.3")
  .withServiceBinding("localhost", backendService)
  .withExec(["npm", "run", "test:e2e"])
```

#### Service Creation
```go
return container.AsService(dagger.ContainerAsServiceOpts{
    UseEntrypoint: true,
})
```

### 4. Module Composition Patterns

#### Submodule Structure
```go
dependencies = [
  {name: "backend", source: ".dagger/backend"},
  {name: "frontend", source: ".dagger/frontend"},
  {name: "external-module", source: "github.com/org/repo"},
]
```

#### Calling Submodules
```go
type Root struct {
    Backend *dagger.Backend
    Frontend *dagger.Frontend
}

backend := dag.Backend(source)
frontend := dag.Frontend(source)
```

#### Cross-Module Services
```go
// Frontend gets backend service
g.Frontend = dag.Frontend(source.Directory("website"), g.Backend.Serve())
```

### 5. Directory and File Handling

#### Ignore Patterns
```python
IGNORE = Ignore([
    ".env", ".git", "**/.venv", "**__pycache__**",
    ".dagger/sdk", "**/.pytest_cache", "**/.ruff_cache",
])

RootDir: TypeAlias = Annotated[
    dagger.Directory,
    DefaultPath("."),
    IGNORE,
]
```

#### Directory Operations
```python
# Exclude directories
container = container.with_directory(
    "/src/projects/lib-one",
    root_dir.directory("projects/lib-one"),
)

# Extract files
binary = build_dir.file("bin/app")

# Get directory from container
output_dir = container.directory("/build")
```

#### File Embedding
```go
//go:embed greetings.json
var greetingsJson []byte
```

### 6. Container Image Selection

#### Security-First Approach
```go
dag.Container().
    From("cgr.dev/chainguard/wolfi-base:latest@sha256:a8c9c2888304e62c133af76f520c9c9e6b3ce6f1a45e3eaa57f6639eb8053c90")
```

#### Tool-Specific Images
```typescript
// Cypress included image for E2E
.from("cypress/included:14.0.3")

// Generic Node image
.from("node")

// Nginx for serving
.from("nginx")
```

#### Multi-Platform Builds
```go
dag.Container(dagger.ContainerOpts{
    Platform: dagger.Platform(arch),  // "linux/amd64" or "linux/arm64"
})
```

### 7. LLM Integration Patterns

#### Basic LLM Call
```go
dag.LLM(dagger.LLMOpts{Model: "claude-sonnet-4-0"}).
    WithPrompt("Your prompt here").
    LastReply(ctx)
```

#### With Environment
```go
env := dag.Env().
    WithWorkspaceInput("workspace", ws, "description").
    WithStringInput("key", value, "description").
    WithStringOutput("result", "description")

agent := dag.LLM(opts).
    WithEnv(env).
    WithPromptFile(promptFile).
    Loop()  // Keeps running until success
```

#### Token Tracking
```go
totalTokens, _ := agent.TokenUsage().TotalTokens(ctx)
```

#### Prompt Files
```go
prompt := dag.CurrentModule().Source().File("prompts/assignment.md")
```

### 8. GitHub Integration

#### Reading Issues
```go
gh := dag.GithubIssue(dagger.GithubIssueOpts{Token: ghToken})
issue := gh.Read(repo, issueId)
body, _ := issue.Body(ctx)
author, _ := issue.Author(ctx)
```

#### Creating PRs
```go
pr := gh.CreatePullRequest(repo, title, body, sourceDir)
url, _ := pr.URL(ctx)
issueNum, _ := pr.IssueNumber(ctx)
```

#### Code Comments
```go
gh.WritePullRequestCodeComment(
    ctx, repo, pr, commit,
    "```suggestion\nfixed code\n```",
    filePath, "RIGHT", lineNumber)
```

#### Commits
```go
gh.CreatePullRequestCommit(ctx, repo, sourceDir, branch)
```

### 9. Workspace Pattern (for LLM Agents)

#### Creating Workspace
```go
ws := dag.Workspace(
    sourceDir,
    backend.AsWorkspaceCheckable(),  // Provides validation
)
```

#### Adding to LLM Environment
```go
env := dag.Env().
    WithWorkspaceInput("workspace", ws, "workspace to modify").
    WithWorkspaceOutput("result", "modified workspace")
```

#### Extracting Results
```go
resultDir := agent.Env().
    Output("result").
    AsWorkspace().
    Work()
```

#### Getting Diff
```go
diff, _ := agent.Env().
    Output("modified").
    AsWorkspace().
    Diff(ctx)
```

---

## Adaptation for Hybrid Monorepo

### Proposed Structure for Python/TypeScript Monorepo

```
monorepo/
├── .dagger/                          # Dagger modules
│   ├── src/
│   │   ├── python_module.py          # Python orchestration
│   │   └── typescript_module.py       # Delegate to TS
│   ├── pyproject.toml
│   └── uv.lock
│
├── python/                           # Python workspace
│   ├── packages/
│   │   ├── core/
│   │   │   └── pyproject.toml
│   │   └── services/
│   │       └── pyproject.toml
│   ├── pyproject.toml                # Root workspace
│   └── uv.lock
│
├── typescript/                       # TypeScript workspace
│   ├── packages/
│   │   ├── web/
│   │   │   └── package.json
│   │   └── api/
│   │       └── package.json
│   ├── package.json
│   ├── turbo.json                    # Turborepo config
│   └── pnpm-lock.yaml (or bun.lockb)
│
├── pyproject.toml                    # Root Python config
└── dagger.json
```

### Implementation Strategy

#### 1. Root Dagger Module (Python-based)

```python
# .dagger/src/monorepo_dagger.py

from typing import Annotated
import dagger
from dagger import dag, function, object_type, DefaultPath, Ignore, Container

IGNORE = Ignore([".git", "**/.venv", "**/node_modules", "**/.dagger/sdk"])

RootDir: TypeAlias = Annotated[dagger.Directory, DefaultPath("."), IGNORE]

@object_type
class MonorepoCI:
    @function
    async def test_python_package(
        self,
        root_dir: RootDir,
        package: str,
    ) -> str:
        """Test a Python package using UV."""
        container = (
            dag.container()
            .from("python:3.12-slim")
            .with_directory("/src", root_dir)
            .with_workdir("/src/python")
        )
        
        # Use uv-dagger-dream pattern
        container = container.with_exec([
            "curl", "-LsSf", "https://astral.sh/uv/install.sh", "|", "sh"
        ])
        
        return await container.with_exec([
            "uv", "sync", "--package", package
        ]).with_exec([
            "pytest", f"packages/{package}"
        ]).stdout()

    @function
    async def test_typescript_package(
        self,
        root_dir: RootDir,
        package: str,
    ) -> str:
        """Test a TypeScript package using Bun."""
        return await (
            dag.container()
            .from("oven/bun:latest")
            .with_directory("/src", root_dir)
            .with_workdir("/src/typescript")
            .with_exec(["bun", "install"])
            .with_exec(["bun", "test", f"packages/{package}"])
            .stdout()
        )

    @function
    async def build_python(
        self,
        root_dir: RootDir,
        package: str,
    ) -> Container:
        """Build Python package for PyPI."""
        container = (
            dag.container()
            .from("python:3.12-slim")
            .with_directory("/src", root_dir)
            .with_workdir("/src/python")
            .with_exec(["pip", "install", "build", "twine"])
            .with_exec(["python", "-m", "build", f"packages/{package}"])
        )
        return container

    @function
    async def build_typescript(
        self,
        root_dir: RootDir,
    ) -> Container:
        """Build all TypeScript packages."""
        return (
            dag.container()
            .from("oven/bun:latest")
            .with_directory("/src", root_dir)
            .with_workdir("/src/typescript")
            .with_exec(["bun", "install"])
            .with_exec(["bunx", "turbo", "build"])
        )

    @function
    async def lint_all(self, root_dir: RootDir) -> str:
        """Run linting for all packages."""
        py_lint = await self.lint_python(root_dir)
        ts_lint = await self.lint_typescript(root_dir)
        return f"Python:\n{py_lint}\n\nTypeScript:\n{ts_lint}"

    @function
    async def lint_python(self, root_dir: RootDir) -> str:
        """Lint Python code."""
        return await (
            dag.container()
            .from("python:3.12-slim")
            .with_directory("/src", root_dir)
            .with_workdir("/src/python")
            .with_exec(["pip", "install", "ruff", "pyright"])
            .with_exec(["ruff", "check", "."])
            .with_exec(["pyright"])
            .stdout()
        )

    @function
    async def lint_typescript(self, root_dir: RootDir) -> str:
        """Lint TypeScript code."""
        return await (
            dag.container()
            .from("oven/bun:latest")
            .with_directory("/src", root_dir)
            .with_workdir("/src/typescript")
            .with_exec(["bun", "install"])
            .with_exec(["bunx", "turbo", "lint"])
            .stdout()
        )

    @function
    async def check(self, root_dir: RootDir) -> str:
        """Run all checks (lint + test)."""
        lint_result = await self.lint_all(root_dir)
        
        py_test = await self.test_python_package(root_dir, "core")
        ts_test = await self.test_typescript_package(root_dir, "web")
        
        return f"Lint:\n{lint_result}\n\nTests:\nPython: {py_test}\nTypeScript: {ts_test}"
```

#### 2. Usage Examples

```bash
# Test individual packages
dagger call test-python-package --package core
dagger call test-typescript-package --package web

# Build for release
dagger call build-python --package core
dagger call build-typescript

# Full CI/CD
dagger call check

# With environment variables
dagger call build-python \
  --package core \
  --root-dir . \
  --secret-token $(cat ~/.pypi_token)
```

#### 3. Integration with Agents

```python
# Extend for agentic development

@function
async def develop_feature(
    self,
    root_dir: RootDir,
    assignment: str,
    language: str,  # "python" or "typescript"
    model: str = "claude-sonnet-4-0",
) -> Container:
    """Develop a feature using an agent."""
    
    if language == "python":
        workspace_container = await self.build_python_dev_env(root_dir)
    else:
        workspace_container = await self.build_typescript_dev_env(root_dir)
    
    # Create workspace for agent
    ws = dag.Workspace(
        root_dir,
        workspace_container.as_workspace_checkable(),
    )
    
    env = dag.Env().with_workspace_input(
        "workspace", ws, "workspace to develop in"
    ).with_string_input(
        "assignment", assignment, "feature to develop"
    )
    
    agent = dag.LLM({"model": model}).with_env(env).with_prompt_file(
        root_dir.file(".dagger/prompts/feature_assignment.md")
    ).loop()
    
    return agent.Env().Output("completed").AsWorkspace().Work()
```

#### 4. PyPI Publishing

```python
@function
async def publish_python(
    self,
    root_dir: RootDir,
    package: str,
    pypi_token: dagger.Secret,
    version: str,
) -> str:
    """Publish Python package to PyPI."""
    
    build_container = await self.build_python(root_dir, package)
    
    return await (
        build_container
        .with_secret_variable("TWINE_PASSWORD", pypi_token)
        .with_env_variable("TWINE_USERNAME", "__token__")
        .with_exec([
            "python", "-m", "twine", "upload",
            f"packages/{package}/dist/*"
        ])
        .stdout()
    )
```

#### 5. NPM Registry Publishing

```python
@function
async def publish_typescript(
    self,
    root_dir: RootDir,
    npm_token: dagger.Secret,
) -> str:
    """Publish TypeScript packages to npm."""
    
    return await (
        dag.container()
        .from("oven/bun:latest")
        .with_directory("/src", root_dir)
        .with_workdir("/src/typescript")
        .with_file(".npmrc", (
            dag.directory()
            .with_new_file(".npmrc", "//registry.npmjs.org/:_authToken=${NPM_TOKEN}")
            .file(".npmrc")
        ))
        .with_secret_variable("NPM_TOKEN", npm_token)
        .with_exec(["bun", "install"])
        .with_exec(["bunx", "turbo", "build"])
        .with_exec(["npm", "publish", "--workspaces"])
        .stdout()
    )
```

---

## Summary Table of Patterns

| Pattern | Language | Use Case | Example |
|---------|----------|----------|---------|
| Workspace Monorepo | Python (UV) | Managing multiple packages | uv-dagger-dream |
| Submodule Composition | Go | Separating concerns (backend/frontend) | greetings-api |
| AI Agent Development | Go/TypeScript | Automating feature development & review | greetings-api develop.go |
| Service Networking | TypeScript | E2E testing with live services | greetings-api frontend |
| Multi-Version Testing | Python | Testing across Python versions | hf-model-ops |
| Custom Workspaces | TypeScript | AI agent tools & validation | technical-content-summarizer |
| Secret Management | Any | API tokens, credentials | All examples with credentials |
| Caching Strategy | Any | Performance optimization | All examples |
| GitHub Integration | Go | CI/CD automation | greetings-api with GH modules |

---

## Key Takeaways

1. **Modular Composition**: Build complex pipelines from simple, reusable modules
2. **Language Flexibility**: Mix Python, Go, TypeScript in same project
3. **Container-First**: Everything runs in containers for reproducibility
4. **Caching Is Critical**: Leverage named cache volumes for performance
5. **Agentic Patterns**: Workspaces + LLM loops enable autonomous development
6. **Testing First**: Integrate testing throughout (unit, E2E, integration)
7. **Secrets Handling**: Use first-class secret management, never embed tokens
8. **GitHub Integration**: Automate the full SDLC (develop, review, debug)
9. **Flexible File Structure**: Workspaces support arbitrary project layouts
10. **Prompt Engineering**: External prompts for easy agent behavior iteration

