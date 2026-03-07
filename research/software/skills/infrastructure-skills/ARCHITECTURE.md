# Infrastructure Architecture Reference

## Quick Navigation

This is the primary infrastructure architecture reference. Consolidated from root-level infrastructure research documents.

**Tool-Specific Documentation:**
- [dagger/](./dagger/) - Dagger CI/CD pipelines
- [komodo/](./komodo/) - Komodo deployment orchestration
- [pangolin/](./pangolin/) - Pangolin zero-trust networking
- [1password/](./1password/) - 1Password secrets management
- [cloudflare/](./cloudflare/) - Cloudflare services (Workers, D1, R2, Tunnel)
- [pulumi/](./pulumi/) - Infrastructure as Code

**External Research (Reference Only):**
- [external-tools/github-spec-kit/](./external-tools/github-spec-kit/) - GitHub Spec Kit research (external tool, not used in project - we use OpenSpec)

---

## Table of Contents

1. [Overview & Key Principles](#overview--key-principles)
2. [Core Stack Components](#core-stack-components)
3. [System Architecture](#system-architecture)
4. [CI/CD Pipeline Architecture](#cicd-pipeline-architecture)
5. [Deployment Orchestration](#deployment-orchestration)
6. [Zero-Trust Networking](#zero-trust-networking)
7. [Secrets Management](#secrets-management)
8. [Infrastructure as Code](#infrastructure-as-code)
9. [Integration Patterns](#integration-patterns)

---

## Overview & Key Principles

This infrastructure stack enables declarative, cross-language workflows that work consistently from local development through production deployment.

### Key Architectural Principles

1. **Container-First**: All services run in containers for consistency and portability
2. **Secrets Never in Git**: All sensitive data managed through 1Password vaults
3. **Zero-Trust Networking**: Services accessible only through authenticated Pangolin tunnels
4. **Infrastructure as Code**: All infrastructure defined in code and version controlled
5. **Modular Pipelines**: Build, test, and deploy steps organized as reusable Dagger modules

### Technology Stack

| Category | Tool | Purpose |
|----------|------|---------|
| **Git Hosting** | Forgejo | Self-hosted Git with Actions |
| **CI/CD** | Dagger | Programmable pipelines in code |
| **Deployment** | Komodo | Docker Compose orchestration |
| **Networking** | Pangolin | Zero-trust tunnel access |
| **Secrets** | 1Password | Vault-based secrets management |
| **IaC** | Pulumi | Cloud resource provisioning |
| **Config** | Ansible | Server configuration |

---

## Core Stack Components

### Dagger - CI/CD in Code

Dagger replaces YAML-based CI with real programming languages (TypeScript, Python, Go).

**Key Advantages:**
- **Unified Pipelines**: Same code runs locally and in CI
- **Monorepo Support**: Context filtering for multi-project repos
- **Cross-Language**: Modules can call each other across languages
- **Caching**: Automatic layer caching for fast builds

```typescript
// Example Dagger pipeline
import { dag, Container, Directory } from "@dagger.io/dagger"

export async function build(src: Directory): Promise<Container> {
  return dag
    .container()
    .from("node:20-alpine")
    .withDirectory("/app", src)
    .withWorkdir("/app")
    .withExec(["npm", "install"])
    .withExec(["npm", "run", "build"])
}
```

### Komodo - Deployment Orchestration

Komodo manages Docker Compose stacks across servers via agents (Periphery).

**Capabilities:**
- Deploy Docker Compose stacks to remote servers
- Manage multiple deployment targets
- TypeScript SDK for programmatic control
- Integrates with 1Password for secrets

```typescript
// Komodo TypeScript SDK
import { KomodoClient } from "@komodo/client"

const client = new KomodoClient({
  url: "https://komodo.example.com",
  apiKey: process.env.KOMODO_API_KEY
})

await client.stacks.deploy({
  name: "my-app",
  server: "production-01",
  composePath: "./docker-compose.yml"
})
```

### Pangolin - Zero-Trust Access

Pangolin provides secure access to services without exposing ports.

**Components:**
- **Newt**: Site connector (runs on server)
- **Olm**: Client agent (runs on user machines)
- **Gerbil**: WireGuard-based tunnel server

**Access Models:**
- **Public**: Internet-accessible via reverse proxy
- **Private**: VPN-only access through Olm
- **Hybrid**: Public frontend, private backend

### 1Password - Secrets Management

1Password provides vault-based secrets with CLI and Connect options.

**Integration Points:**
- Dagger: Inject secrets during builds
- Komodo: Reference secrets in compose files
- Ansible: Provision secrets to servers
- Pulumi: Cloud resource credentials

---

## System Architecture

### End-to-End Flow

```
Developer Push → Forgejo Actions → Dagger Pipeline → Build Images
                                  ↓
                         Inject 1Password Secrets
                                  ↓
                         Test with Docker Compose
                                  ↓
                    Publish to Forgejo Registry
                                  ↓
              Deploy via Komodo TypeScript SDK
                                  ↓
        Pangolin Newt Auto-registers Services
                                  ↓
              Production Deployment
```

### Component Interactions

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Developer Workstation                            │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Local Development                                               │   │
│  │  - dagger call build                                            │   │
│  │  - docker compose up                                            │   │
│  │  - op run -- command (1Password CLI)                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ git push
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Forgejo + Actions                             │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────────────┐ │
│  │  Git Server  │→→│ Actions      │→→│ Dagger Pipeline               │ │
│  │              │  │ Runner       │  │ - Build images                │ │
│  │  Registry    │←←│              │←←│ - Run tests                   │ │
│  └──────────────┘  └──────────────┘  │ - Push to registry            │ │
│                                       └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Komodo SDK deploy
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Production Server                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Komodo Periphery Agent                                          │  │
│  │  - Receives deployment commands                                  │  │
│  │  - Manages Docker Compose stacks                                 │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Pangolin Newt                                                   │  │
│  │  - WireGuard tunnel to Gerbil                                    │  │
│  │  - Auto-registers services                                       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Application Containers                                          │  │
│  │  - Web apps, APIs, databases                                     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## CI/CD Pipeline Architecture

### Dagger Module Structure

```
.dagger/
├── src/
│   ├── index.ts              # Main entry point
│   ├── build.ts              # Build functions
│   ├── test.ts               # Test functions
│   ├── deploy.ts             # Deploy functions
│   └── secrets.ts            # 1Password integration
├── dagger.json               # Module configuration
└── package.json
```

### Pipeline Stages

**1. Build Stage**
```typescript
export async function build(src: Directory): Promise<Container> {
  // Install dependencies
  // Compile TypeScript
  // Bundle application
  return container
}
```

**2. Test Stage**
```typescript
export async function test(src: Directory): Promise<string> {
  // Run unit tests
  // Run integration tests
  // Run linting
  return testResults
}
```

**3. Publish Stage**
```typescript
export async function publish(container: Container): Promise<string> {
  // Tag image
  // Push to registry
  return imageRef
}
```

**4. Deploy Stage**
```typescript
export async function deploy(imageRef: string): Promise<void> {
  // Call Komodo API
  // Update stack
  // Verify health
}
```

### Forgejo Actions Workflow

```yaml
name: CI/CD Pipeline
on:
  push:
    branches: [main]

jobs:
  build-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Install Dagger
        run: |
          curl -fsSL https://dl.dagger.io/dagger/install.sh | sh
          sudo mv bin/dagger /usr/local/bin/

      - name: Run Pipeline
        run: |
          dagger call build --src=.
          dagger call test --src=.
          dagger call publish
          dagger call deploy
        env:
          OP_SERVICE_ACCOUNT_TOKEN: ${{ secrets.OP_TOKEN }}
          KOMODO_API_KEY: ${{ secrets.KOMODO_KEY }}
```

---

## Deployment Orchestration

### Komodo Architecture

```
┌─────────────────────────────────────────────────┐
│              Komodo Core                        │
│  ┌──────────────────────────────────────────┐   │
│  │  API Server                              │   │
│  │  - REST API for deployments              │   │
│  │  - WebSocket for real-time updates       │   │
│  │  - Stack configuration storage           │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
          │                      │
          │                      │
          ▼                      ▼
┌─────────────────────┐  ┌─────────────────────┐
│ Periphery Agent     │  │ Periphery Agent     │
│ (Server 1)          │  │ (Server 2)          │
│ - Docker daemon     │  │ - Docker daemon     │
│ - Compose runtime   │  │ - Compose runtime   │
└─────────────────────┘  └─────────────────────┘
```

### Stack Configuration

```yaml
# docker-compose.yml for Komodo deployment
version: "3.8"

services:
  app:
    image: registry.example.com/my-app:${TAG:-latest}
    environment:
      - DATABASE_URL=op://vault/database/url
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=app.example.com"

  database:
    image: postgres:16
    volumes:
      - db_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_PASSWORD=op://vault/database/password

volumes:
  db_data:
```

---

## Zero-Trust Networking

### Pangolin Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **Gerbil** | Cloud/Central | WireGuard tunnel server |
| **Newt** | Each server | Site connector |
| **Olm** | Client devices | VPN client |

### Access Patterns

**Public Access (Internet-facing)**
```yaml
# pangolin-config.yaml
resources:
  - name: web-app
    type: http
    target: http://app:3000
    access: public
    domain: app.example.com
```

**Private Access (VPN-only)**
```yaml
resources:
  - name: admin-panel
    type: http
    target: http://admin:8080
    access: private
    # Only accessible via Olm VPN
```

**Hybrid Access**
```yaml
resources:
  - name: api
    type: http
    target: http://api:4000
    access: public
    domain: api.example.com

  - name: api-internal
    type: http
    target: http://api:4000/internal
    access: private
    # Internal endpoints VPN-only
```

### Newt Auto-Registration

Newt can automatically register services based on Docker labels:

```yaml
services:
  my-service:
    image: my-app:latest
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=myservice.example.com"
      - "pangolin.access=public"
```

---

## Secrets Management

### 1Password Integration Options

| Method | Use Case | Pros | Cons |
|--------|----------|------|------|
| **CLI (op)** | CI/CD, local dev | Simple, no server | Requires login |
| **Connect** | Always-on services | No interactive auth | Requires server |
| **Service Account** | CI/CD automation | Token-based | Limited to 1 vault |

### Dagger + 1Password

```typescript
import { dag, Secret } from "@dagger.io/dagger"

export async function withSecrets(container: Container): Promise<Container> {
  // Get secret from 1Password
  const dbUrl = dag.setSecret("db-url",
    await execCmd("op read op://vault/database/url")
  )

  return container.withSecretVariable("DATABASE_URL", dbUrl)
}
```

### SOPS + 1Password Hybrid

For files that need to be in git (encrypted):

```yaml
# .sops.yaml
creation_rules:
  - path_regex: \.enc\.yaml$
    age: age1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# secrets.enc.yaml (encrypted)
database:
  host: ENC[AES256_GCM,data:...,type:str]
  password: ENC[AES256_GCM,data:...,type:str]
```

---

## Infrastructure as Code

### Pulumi for Cloud Resources

```typescript
// pulumi/index.ts
import * as pulumi from "@pulumi/pulumi"
import * as cloudflare from "@pulumi/cloudflare"

// Create R2 bucket
const bucket = new cloudflare.R2Bucket("data-bucket", {
  accountId: process.env.CLOUDFLARE_ACCOUNT_ID,
  name: "my-data-bucket",
})

// Create D1 database
const database = new cloudflare.D1Database("app-db", {
  accountId: process.env.CLOUDFLARE_ACCOUNT_ID,
  name: "app-database",
})

// Export outputs
export const bucketName = bucket.name
export const databaseId = database.id
```

### Ansible for Server Config

```yaml
# playbooks/setup-server.yml
- name: Setup production server
  hosts: production
  become: yes
  tasks:
    - name: Install Docker
      ansible.builtin.apt:
        name: docker.io
        state: present

    - name: Install Komodo Periphery
      community.docker.docker_container:
        name: komodo-periphery
        image: ghcr.io/mbecker20/periphery:latest
        restart_policy: always
        env:
          KOMODO_HOST: "https://komodo.example.com"
          PERIPHERY_PASSKEY: "{{ lookup('community.general.onepassword', 'periphery-key') }}"

    - name: Install Pangolin Newt
      community.docker.docker_container:
        name: pangolin-newt
        image: ghcr.io/pangolin/newt:latest
        restart_policy: always
        volumes:
          - /var/run/docker.sock:/var/run/docker.sock
        env:
          PANGOLIN_ENDPOINT: "wss://gerbil.example.com"
          PANGOLIN_ID: "{{ inventory_hostname }}"
```

---

## Integration Patterns

### Complete Deployment Flow

```
1. Developer pushes to main branch
   └─→ Forgejo triggers Actions workflow

2. Dagger pipeline executes
   ├─→ Build: Compile code, create container
   ├─→ Test: Run unit/integration tests
   ├─→ Secrets: Inject 1Password credentials
   └─→ Publish: Push to Forgejo registry

3. Komodo deployment
   ├─→ SDK calls Komodo API
   ├─→ Periphery pulls new image
   └─→ Stack redeployed with zero downtime

4. Pangolin registration
   ├─→ Newt detects new service
   ├─→ Registers with Gerbil
   └─→ DNS/TLS configured automatically

5. Traffic flows
   └─→ Users access via Pangolin domain
```

### Multi-Environment Strategy

```
Development:
├─→ Local Docker Compose
├─→ 1Password CLI for secrets
└─→ No Pangolin (localhost)

Staging:
├─→ Komodo deployment to staging server
├─→ 1Password Connect for secrets
└─→ Pangolin private access (VPN-only)

Production:
├─→ Komodo deployment to production cluster
├─→ 1Password Connect for secrets
└─→ Pangolin public + private access
```

---

## References

**Source Documents:**
- ci-cd-stack-integration-guide.md
- infrastructure-consolidation-guide.md
- ci-cd-platform-architecture.md
- deciding-between-systemd-agents-docker-compose-komodo-pangolin.md

**Tool Documentation:**
- [Dagger Documentation](https://docs.dagger.io)
- [Komodo Documentation](https://komo.do/docs)
- [Pangolin Documentation](https://pangolin.dev/docs)
- [1Password CLI](https://developer.1password.com/docs/cli)

**Last Updated:** November 29, 2024
