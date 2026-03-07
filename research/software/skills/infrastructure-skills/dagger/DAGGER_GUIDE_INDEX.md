# Dagger CI/CD - Complete Guide Index

> Consolidated navigation guide for Dagger CI/CD patterns in hybrid Python/TypeScript monorepos.

## Quick Navigation

| I want to... | Go to |
|--------------|-------|
| **Build Python (UV) monorepo** | [DAGGER_PATTERNS_ANALYSIS.md](./DAGGER_PATTERNS_ANALYSIS.md) Section 1 |
| **Build TypeScript (Bun) workspace** | [DAGGER_QUICK_REFERENCE.md](./DAGGER_QUICK_REFERENCE.md) "TypeScript Pattern" |
| **Add agentic CI/CD** | [DAGGER_PATTERNS_ANALYSIS.md](./DAGGER_PATTERNS_ANALYSIS.md) Section 2 |
| **Build hybrid Python + TypeScript** | [DAGGER_PATTERNS_ANALYSIS.md](./DAGGER_PATTERNS_ANALYSIS.md) Section 6 |
| **Quick lookup while coding** | [DAGGER_QUICK_REFERENCE.md](./DAGGER_QUICK_REFERENCE.md) |
| **Infrastructure orchestration** | [infrastructure/dagger/](./infrastructure/dagger/) |
| **Troubleshoot issues** | [DAGGER_QUICK_REFERENCE.md](./DAGGER_QUICK_REFERENCE.md) "Common Mistakes" |

---

## Documentation Structure

### Core Documents (Root Level)

| Document | Lines | Purpose |
|----------|-------|---------|
| **DAGGER_GUIDE_INDEX.md** | ~200 | This file - navigation and overview |
| **DAGGER_PATTERNS_ANALYSIS.md** | 1,776 | Comprehensive technical reference |
| **DAGGER_QUICK_REFERENCE.md** | 459 | One-page cheat sheet with code snippets |

### Infrastructure-Specific (Subdirectory)

| Document | Lines | Purpose |
|----------|-------|---------|
| **dagger-unified-pipeline-architecture.md** | 2,232 | Complete implementation reference |
| **dagger-implementation-checklist.md** | 430 | Phase-by-phase task tracking |
| **dagger-orchestration-guide.md** | ~1,500 | Komodo/Pangolin/Pulumi integration |

---

## Example Patterns Analyzed

### 1. uv-dagger-dream (Python)
- Multi-stage Dockerfile strategy for UV
- Dependency graph resolution from uv.lock
- Editable install pattern for workspaces
- Type-safe path handling with annotations

### 2. greetings-api (Go + TypeScript)
- Submodule composition (root → backend → frontend)
- Three agentic patterns (develop, review, debug)
- Workspace I/O for LLM agents
- GitHub integration (issues, PRs, code suggestions)

### 3. technical-content-summarizer (TypeScript)
- Main agent + workspace module pattern
- Tool-based validation
- Agent iteration loops
- Error-based feedback

---

## Dagger Concepts Quick Reference

### Container Operations
```python
.from_(image)              # Select base image
.with_directory(path, dir) # Copy directory into container
.with_file(path, file)     # Copy file into container
.with_exec(cmd)            # Execute command
.with_workdir(path)        # Set working directory
.with_exposed_port(port)   # Expose port for services
.as_service()              # Convert to service
```

### Caching
```python
.with_mounted_cache(path, volume)  # Mount persistent cache
dag.cache_volume(name)              # Create named cache volume
# Named volumes: npm-cache, uv-cache, bun-cache
```

### Services & Networking
```python
.with_service_binding(name, service)  # Connect to another service
# Access from container: http://name:port
.as_service(use_entrypoint=True)      # Service creation
```

### Secrets
```python
dag.set_secret(name, value)           # Create secret
.with_secret_variable(ENV_VAR, secret) # Inject as env var
.with_secret_file(path, secret)        # Write to file
```

### LLM Agents
```python
dag.LLM({"model": "claude-sonnet-4-0"})  # Create LLM
.with_env(env)                            # Pass inputs/outputs
.with_prompt_file(file)                   # Load prompt from file
.loop()                                   # Run until success
.last_reply()                             # Get final response
```

---

## Implementation Phases

### Phase 1: Foundation
1. Create `.dagger/` directory in monorepo root
2. Choose primary language (Python recommended)
3. Create `dagger.json` and `pyproject.toml`
4. Write root class in `.dagger/src/monorepo.py`

### Phase 2: Python Support
1. Add multi-stage Dockerfile for UV
2. Implement `build_python_package()` function
3. Implement `test_python_package()` function
4. Add caching for UV: `/root/.cache/uv`

### Phase 3: TypeScript Support
1. Implement `build_typescript_package()` function
2. Implement `test_typescript_package()` function
3. Add caching for Bun: `/root/.bun`

### Phase 4: Integration
1. Create orchestration functions: `check()`, `build()`, `release()`
2. Implement service networking for E2E tests
3. Add GitHub integration if needed

### Phase 5: Agentic CI (Optional)
1. Add LLM agent support for feature development
2. Implement review and debug agents
3. Create prompt files for each agent
4. Set up GitHub comment handlers

---

## Performance Checklist

- [ ] Use specific image tags with sha256 hashes (not `latest`)
- [ ] Mount cache volumes at the right path for your tool
- [ ] Order Dockerfile layers: unchanged → occasionally → frequently changing
- [ ] Use `.with_mounted_cache()` before `.with_exec()` for build tools
- [ ] Exclude large files with `Ignore` annotations
- [ ] Use smaller base images (chainguard, alpine, slim variants)
- [ ] Parallel tasks: Use `asyncio.gather()` in Python

---

## Useful Commands

```bash
# List available Dagger functions
dagger functions

# Call a function
dagger call test-python-package --package core

# Verbose output
dagger call check --verbose

# Help
dagger call <function> --help

# With environment variables
MY_TOKEN=abc dagger call deploy --token $MY_TOKEN
```

---

## Summary Matrix

| Aspect | uv-dagger-dream | greetings-api | tech-summarizer | Your Use Case |
|--------|-----------------|---------------|-----------------|---------------|
| Language | Python | Go/TypeScript | TypeScript | Python/TypeScript |
| Package Manager | UV | Go modules/NPM | NPM | UV/Bun |
| Primary Pattern | Monorepo workspace | Submodule composition | AI agents | Orchestration |
| Build Strategy | Multi-stage Docker | Language-specific | TypeScript build | Hybrid |
| Testing | pytest | gotest/Cypress | Custom validation | pytest/Vitest |
| Agents | No | Yes (3 types) | Yes (1 type) | Potential |
| GitHub Integration | No | Yes | No | Optional |
| Caching Strategy | UV + pip | Built-in | NPM | UV/Bun |

---

## External Resources

- [Dagger Documentation](https://docs.dagger.io/)
- [Dagger Python SDK](https://docs.dagger.io/sdk/python/)
- [Dagger Go SDK](https://docs.dagger.io/sdk/go/)
- [Dagger TypeScript SDK](https://docs.dagger.io/sdk/typescript/)
- [UV Documentation](https://docs.astral.sh/uv/)
- [Bun Documentation](https://bun.sh/docs)
- [Turborepo](https://turbo.build/)

---

*Consolidated from README_DAGGER_ANALYSIS.md and DAGGER_ANALYSIS_INDEX.md*
*Last updated: November 2025*
