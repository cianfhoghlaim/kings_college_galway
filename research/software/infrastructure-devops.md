# Infrastructure & DevOps

Comprehensive guide to container-first architecture using Dagger CI/CD, Komodo deployment orchestration, Pangolin zero-trust networking, and 1Password secrets management.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Core Stack Components](#2-core-stack-components)
3. [CI/CD with Dagger](#3-cicd-with-dagger)
4. [Deployment with Komodo](#4-deployment-with-komodo)
5. [Zero-Trust Networking with Pangolin](#5-zero-trust-networking-with-pangolin)
6. [Secrets Management](#6-secrets-management)
7. [Infrastructure as Code](#7-infrastructure-as-code)
8. [LLM Gateway](#8-llm-gateway)
9. [Decision Matrices](#9-decision-matrices)
10. [Implementation Guide](#10-implementation-guide)

---

## 1. Architecture Overview

### 1.1 Key Principles

1. **Container-First**: All services run in containers for consistency and portability
2. **Secrets Never in Git**: All sensitive data managed through 1Password vaults
3. **Zero-Trust Networking**: Services accessible only through authenticated tunnels
4. **Infrastructure as Code**: All infrastructure defined in code and version controlled
5. **Modular Pipelines**: Build, test, and deploy steps organized as reusable Dagger modules

### 1.2 Technology Stack

| Category | Tool | Purpose |
|----------|------|---------|
| **Git Hosting** | Forgejo | Self-hosted Git with Actions |
| **CI/CD** | Dagger | Programmable pipelines in code |
| **Deployment** | Komodo | Docker Compose orchestration |
| **Networking** | Pangolin | Zero-trust tunnel access |
| **Secrets** | 1Password | Vault-based secrets management |
| **IaC** | Pulumi | Cloud resource provisioning |
| **Config** | Ansible | Server configuration |
| **Serverless** | Cloudflare | Workers, D1, R2 |

### 1.3 End-to-End Flow

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

---

## 2. Core Stack Components

### 2.1 Component Diagram

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

## 3. CI/CD with Dagger

### 3.1 Why Dagger Over YAML

| Limitation of YAML | Dagger Solution |
|--------------------|-----------------|
| Different locally vs CI | Same code everywhere |
| No real programming | TypeScript/Python/Go |
| Hard to test | Unit tests on pipelines |
| Copy-paste reuse | Module imports |

### 3.2 Module Structure

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

### 3.3 Pipeline Implementation

```typescript
// .dagger/src/index.ts
import { dag, Container, Directory, Secret } from "@dagger.io/dagger"

export async function build(src: Directory): Promise<Container> {
  return dag
    .container()
    .from("node:20-alpine")
    .withDirectory("/app", src)
    .withWorkdir("/app")
    .withExec(["npm", "install"])
    .withExec(["npm", "run", "build"])
}

export async function test(src: Directory): Promise<string> {
  const container = await build(src)
  return container
    .withExec(["npm", "test"])
    .stdout()
}

export async function publish(container: Container, registry: string): Promise<string> {
  return container.publish(`${registry}/my-app:latest`)
}

export async function deploy(imageRef: string): Promise<void> {
  // Call Komodo SDK
  const komodo = dag.komodo()
  await komodo.deployStack({
    name: "my-app",
    image: imageRef,
    server: "production-01"
  })
}
```

### 3.4 Secrets Integration

```typescript
// .dagger/src/secrets.ts
import { dag, Secret } from "@dagger.io/dagger"
import { execSync } from "child_process"

export function getOnePasswordSecret(ref: string): Secret {
  const value = execSync(`op read ${ref}`).toString().trim()
  return dag.setSecret(ref, value)
}

export async function withSecrets(container: Container): Promise<Container> {
  const dbUrl = getOnePasswordSecret("op://vault/database/url")
  const apiKey = getOnePasswordSecret("op://vault/api/key")

  return container
    .withSecretVariable("DATABASE_URL", dbUrl)
    .withSecretVariable("API_KEY", apiKey)
}
```

### 3.5 Forgejo Actions Workflow

```yaml
# .forgejo/workflows/ci.yml
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

## 4. Deployment with Komodo

### 4.1 Architecture

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
          ▼                      ▼
┌─────────────────────┐  ┌─────────────────────┐
│ Periphery Agent     │  │ Periphery Agent     │
│ (Server 1)          │  │ (Server 2)          │
│ - Docker daemon     │  │ - Docker daemon     │
│ - Compose runtime   │  │ - Compose runtime   │
└─────────────────────┘  └─────────────────────┘
```

### 4.2 TypeScript SDK Usage

```typescript
import { KomodoClient } from "@komodo/client"

const client = new KomodoClient({
  url: "https://komodo.example.com",
  apiKey: process.env.KOMODO_API_KEY
})

// Deploy a stack
await client.stacks.deploy({
  name: "my-app",
  server: "production-01",
  composePath: "./docker-compose.yml",
  environment: {
    TAG: "v1.2.3"
  }
})

// Check deployment status
const status = await client.stacks.status("my-app")
console.log(status.health)
```

### 4.3 Stack Configuration

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
    restart: always

  database:
    image: postgres:16
    volumes:
      - db_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_PASSWORD=op://vault/database/password
    labels:
      - "pangolin.enable=false"  # Private only

volumes:
  db_data:
```

---

## 5. Zero-Trust Networking with Pangolin

### 5.1 Components

| Component | Location | Purpose |
|-----------|----------|---------|
| **Gerbil** | Cloud/Central | WireGuard tunnel server |
| **Newt** | Each server | Site connector |
| **Olm** | Client devices | VPN client |

### 5.2 Access Patterns

**Public Access (Internet-facing):**
```yaml
# pangolin-config.yaml
resources:
  - name: web-app
    type: http
    target: http://app:3000
    access: public
    domain: app.example.com
```

**Private Access (VPN-only):**
```yaml
resources:
  - name: admin-panel
    type: http
    target: http://admin:8080
    access: private
    # Only accessible via Olm VPN
```

**Hybrid Access:**
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

### 5.3 Docker Label Auto-Registration

Newt detects services based on Docker labels:

```yaml
services:
  my-service:
    image: my-app:latest
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=myservice.example.com"
      - "pangolin.access=public"
```

### 5.4 Newt Configuration

```yaml
# /etc/pangolin/newt.yaml
endpoint: wss://gerbil.example.com
site_id: production-01
docker:
  enabled: true
  label_prefix: pangolin
wireguard:
  private_key_file: /etc/pangolin/wg-private.key
```

---

## 6. Secrets Management

### 6.1 1Password Integration Options

| Method | Use Case | Pros | Cons |
|--------|----------|------|------|
| **CLI (op)** | CI/CD, local dev | Simple, no server | Requires login |
| **Connect** | Always-on services | No interactive auth | Requires server |
| **Service Account** | CI/CD automation | Token-based | Limited to 1 vault |

### 6.2 Dagger + 1Password

```typescript
import { dag, Secret } from "@dagger.io/dagger"
import { execSync } from "child_process"

export async function withSecrets(container: Container): Promise<Container> {
  // Get secret from 1Password
  const dbUrl = dag.setSecret("db-url",
    execSync("op read op://vault/database/url").toString().trim()
  )

  return container.withSecretVariable("DATABASE_URL", dbUrl)
}
```

### 6.3 Environment Variable Pattern

```bash
# In CI/CD
export OP_SERVICE_ACCOUNT_TOKEN="${{ secrets.OP_TOKEN }}"

# In scripts
op read "op://Infrastructure/database/password"
```

### 6.4 SOPS + 1Password Hybrid

For encrypted files in git:

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

## 7. Infrastructure as Code

### 7.1 Pulumi (TypeScript)

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

### 7.2 Ansible for Server Configuration

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

## 8. LLM Gateway

### 8.1 LiteLLM Configuration

```yaml
# litellm_config.yaml
model_list:
  # Primary models
  - model_name: claude-sonnet
    litellm_params:
      model: claude-3-5-sonnet-latest
      api_key: os.environ/ANTHROPIC_API_KEY

  - model_name: claude-opus
    litellm_params:
      model: claude-3-opus-20240229
      api_key: os.environ/ANTHROPIC_API_KEY

  - model_name: gpt-4o
    litellm_params:
      model: gpt-4o
      api_key: os.environ/OPENAI_API_KEY

  # Fallback chain
  - model_name: general-purpose
    litellm_params:
      model: claude-3-5-sonnet-latest
      api_key: os.environ/ANTHROPIC_API_KEY
    fallbacks:
      - model: gpt-4o
        api_key: os.environ/OPENAI_API_KEY

  # Cost-optimized routing
  - model_name: fast
    litellm_params:
      model: claude-3-5-haiku-latest
      api_key: os.environ/ANTHROPIC_API_KEY

general_settings:
  master_key: os.environ/LITELLM_MASTER_KEY
  database_url: os.environ/DATABASE_URL

litellm_settings:
  drop_params: true
  set_verbose: false
  max_budget: 100  # USD per month
  budget_duration: monthly
```

### 8.2 Docker Compose for LiteLLM

```yaml
services:
  litellm:
    image: ghcr.io/berriai/litellm:main-latest
    command: --config /app/config.yaml
    ports:
      - "4000:4000"
    volumes:
      - ./litellm_config.yaml:/app/config.yaml
    environment:
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - LITELLM_MASTER_KEY=${LITELLM_MASTER_KEY}
      - DATABASE_URL=postgresql://litellm:password@postgres:5432/litellm
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=llm.example.com"
      - "pangolin.access=private"

  postgres:
    image: postgres:16
    environment:
      - POSTGRES_USER=litellm
      - POSTGRES_PASSWORD=password
      - POSTGRES_DB=litellm
    volumes:
      - litellm_db:/var/lib/postgresql/data

volumes:
  litellm_db:
```

---

## 9. Decision Matrices

### 9.1 Deployment Approach

| Approach | Complexity | Scalability | Best For |
|----------|------------|-------------|----------|
| **Systemd** | Low | Single server | Edge deployments |
| **Docker Compose** | Medium | Single server | Development |
| **Komodo** | Medium-High | Multi-server | Production |
| **Kubernetes** | High | Cluster | Large-scale |

### 9.2 Secrets Management

| Factor | 1Password CLI | 1Password Connect | 1Password SA | Infisical |
|--------|---------------|-------------------|--------------|-----------|
| **CI/CD Friendly** | No | Yes | Yes | Yes |
| **Local Dev** | Yes | Overkill | Limited | Yes |
| **Complexity** | Low | Medium | Low | Low |

### 9.3 CI/CD Tools

| Factor | Dagger | GitHub Actions | GitLab CI |
|--------|--------|----------------|-----------|
| **Language** | TS/Python/Go | YAML | YAML |
| **Local Testing** | Native | Limited | Limited |
| **CI/Local Parity** | Exact | Different | Different |
| **Debugging** | Interactive | Log-based | Log-based |

### 9.4 Cloud Platforms

| Factor | Cloudflare | AWS Lambda | Vercel |
|--------|------------|------------|--------|
| **Cold Start** | None (V8) | 100ms+ | Variable |
| **Edge Locations** | 200+ | Limited | 10+ |
| **Vendor Lock-in** | Low | High | Medium |

---

## 10. Implementation Guide

### 10.1 Phase 1: Environment Setup

```bash
# Install core tools
brew install dagger
brew install 1password-cli

# Rust for SpacetimeDB (if using)
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
cargo install spacetime-cli

# Node.js for Dagger TypeScript
fnm install 20
fnm use 20
```

### 10.2 Phase 2: Dagger Module Setup

```bash
# Initialize Dagger module
mkdir -p .dagger/src
cd .dagger

# Create dagger.json
cat > dagger.json << 'EOF'
{
  "name": "my-project",
  "sdk": "typescript"
}
EOF

# Create package.json
cat > package.json << 'EOF'
{
  "name": "@my-project/dagger",
  "type": "module",
  "dependencies": {
    "@dagger.io/dagger": "^0.9.0"
  }
}
EOF

npm install
```

### 10.3 Phase 3: Server Provisioning

```bash
# Run Ansible playbook
ansible-playbook -i inventory.ini playbooks/setup-server.yml

# Verify Periphery connection
curl https://komodo.example.com/api/servers
```

### 10.4 Phase 4: Service Deployment

```bash
# Local test
dagger call build --src=.
dagger call test --src=.

# Deploy
dagger call deploy --env=production
```

### 10.5 Complete Docker Compose Stack

```yaml
version: "3.8"

services:
  # Deployment orchestration
  komodo:
    image: ghcr.io/mbecker20/komodo:latest
    ports:
      - "9120:9120"
    environment:
      - KOMODO_PASSKEYS=${KOMODO_PASSKEYS}
    volumes:
      - komodo_data:/data

  # Zero-trust networking
  gerbil:
    image: ghcr.io/fossorial/gerbil:latest
    ports:
      - "443:443"
      - "51820:51820/udp"
    cap_add:
      - NET_ADMIN

  # LLM Gateway
  litellm:
    image: ghcr.io/berriai/litellm:main-latest
    command: --config /app/config.yaml
    volumes:
      - ./litellm_config.yaml:/app/config.yaml
    environment:
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}

  # Monitoring
  dozzle:
    image: amir20/dozzle:latest
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=logs.example.com"
      - "pangolin.access=private"

volumes:
  komodo_data:
```

---

## References

- Dagger Documentation: https://docs.dagger.io
- Komodo Documentation: https://komo.do/docs
- Pangolin Documentation: https://pangolin.dev/docs
- 1Password CLI: https://developer.1password.com/docs/cli
- Pulumi: https://www.pulumi.com/docs
- LiteLLM: https://docs.litellm.ai
- Cloudflare Workers: https://developers.cloudflare.com/workers
