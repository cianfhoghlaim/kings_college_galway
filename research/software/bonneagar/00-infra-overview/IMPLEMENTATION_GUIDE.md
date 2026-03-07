# Infrastructure Implementation Guide

> Step-by-step CI/CD and deployment integration guide

**Last Updated**: December 2025
**Status**: Generated from existing documentation

---

## Quick Start

### Prerequisites

Before implementing the infrastructure stack, ensure you have:

1. **Docker** (24.0+) installed on all target servers
2. **1Password** account with CLI access configured
3. **Dagger CLI** (v0.14+) installed locally
4. **Git repository** (Forgejo or GitHub) with Actions enabled

### Initial Setup Checklist

- [ ] Docker daemon running on target servers
- [ ] 1Password Service Account token created
- [ ] Dagger Cloud account (optional, for caching)
- [ ] DNS records configured for Pangolin domains
- [ ] SSH access to production servers

---

## 1. Secrets Management Setup (1Password)

### Option A: CLI for Local Development

```bash
# Install 1Password CLI
brew install --cask 1password/tap/1password-cli

# Sign in (interactive)
op signin

# Read a secret
op read "op://vault-name/item-name/field-name"

# Run command with injected secrets
op run -- my-command
```

### Option B: Service Account for CI/CD

```bash
# Create service account in 1Password console
# Export the token
export OP_SERVICE_ACCOUNT_TOKEN="ops_..."

# Use in CI/CD (no interactive sign-in needed)
op read "op://vault/database/url"
```

### Option C: Connect Server for Always-On Services

```yaml
# docker-compose.yml for 1Password Connect
services:
  op-connect-api:
    image: 1password/connect-api:latest
    ports:
      - "8080:8080"
    volumes:
      - ./1password-credentials.json:/home/opuser/.op/1password-credentials.json:ro
    environment:
      OP_SESSION: ${OP_CONNECT_TOKEN}
```

**Reference**: See `/infrastructure/1password/` for detailed configuration.

---

## 2. Dagger Pipeline Setup

### Module Structure

```
.dagger/
├── src/
│   ├── index.ts              # Main entry point
│   ├── build.ts              # Build functions
│   ├── test.ts               # Test functions
│   └── deploy.ts             # Deploy functions
├── dagger.json               # Module configuration
├── package.json
└── tsconfig.json
```

### Basic Pipeline

```typescript
// .dagger/src/index.ts
import { dag, Container, Directory, object, func } from "@dagger.io/dagger"

@object()
export class Pipeline {
  @func()
  async build(src: Directory): Promise<Container> {
    return dag
      .container()
      .from("node:20-alpine")
      .withDirectory("/app", src)
      .withWorkdir("/app")
      .withExec(["npm", "install"])
      .withExec(["npm", "run", "build"])
  }

  @func()
  async test(src: Directory): Promise<string> {
    const container = await this.build(src)
    return container
      .withExec(["npm", "test"])
      .stdout()
  }

  @func()
  async publish(src: Directory, registry: string): Promise<string> {
    const container = await this.build(src)
    const tag = `${registry}/my-app:${Date.now()}`
    await container.publish(tag)
    return tag
  }
}
```

### Running Locally

```bash
# Install Dagger CLI
curl -fsSL https://dl.dagger.io/dagger/install.sh | sh

# Initialize module
dagger init --sdk=typescript

# Run build
dagger call build --src=.

# Run tests
dagger call test --src=.
```

**Reference**: See `/infrastructure/dagger/dagger-unified-pipeline-architecture.md` for complete patterns.

---

## 3. Komodo Deployment Setup

### Install Periphery Agent

On each target server:

```bash
# Using Docker
docker run -d \
  --name komodo-periphery \
  --restart always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e KOMODO_HOST="https://komodo.example.com" \
  -e PERIPHERY_PASSKEY="your-passkey" \
  ghcr.io/mbecker20/periphery:latest
```

### Stack Configuration

```yaml
# docker-compose.yml
version: "3.8"

services:
  app:
    image: ${REGISTRY}/my-app:${TAG:-latest}
    environment:
      - DATABASE_URL=op://vault/database/url
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=app.example.com"
    restart: unless-stopped

  database:
    image: postgres:16
    volumes:
      - db_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_PASSWORD=op://vault/database/password
    restart: unless-stopped

volumes:
  db_data:
```

### Deploy via TypeScript SDK

```typescript
import { KomodoClient } from "@komodo/client"

const client = new KomodoClient({
  url: "https://komodo.example.com",
  apiKey: process.env.KOMODO_API_KEY
})

// Deploy stack
await client.stacks.deploy({
  name: "my-app",
  server: "production-01",
  composePath: "./docker-compose.yml",
  env: {
    TAG: imageTag,
    REGISTRY: "registry.example.com"
  }
})
```

**Reference**: See `/infrastructure/komodo/` for API documentation.

---

## 4. Pangolin Zero-Trust Networking

### Install Newt (Site Connector)

On each server that hosts services:

```bash
# Docker installation
docker run -d \
  --name pangolin-newt \
  --restart always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e PANGOLIN_ENDPOINT="wss://gerbil.example.com" \
  -e PANGOLIN_ID="server-01" \
  -e PANGOLIN_SECRET="your-secret" \
  ghcr.io/pangolin/newt:latest
```

### Service Registration via Labels

```yaml
services:
  my-service:
    image: my-app:latest
    labels:
      - "pangolin.enable=true"
      - "pangolin.domain=myservice.example.com"
      - "pangolin.access=public"  # or "private" for VPN-only
```

### Configure Access Policies

```yaml
# pangolin-config.yaml
resources:
  - name: web-app
    type: http
    target: http://app:3000
    access: public
    domain: app.example.com

  - name: admin-panel
    type: http
    target: http://admin:8080
    access: private  # VPN-only
```

**Reference**: See `/infrastructure/pangolin/` for detailed configuration.

---

## 5. CI/CD Integration (GitHub Actions / Forgejo)

### Workflow File

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Dagger
        uses: dagger/dagger-for-github@v6

      - name: Build, Test, Deploy
        run: |
          dagger call build --src=.
          dagger call test --src=.
          dagger call publish --src=. --registry=${{ vars.REGISTRY }}
          dagger call deploy --tag=$TAG
        env:
          OP_SERVICE_ACCOUNT_TOKEN: ${{ secrets.OP_TOKEN }}
          KOMODO_API_KEY: ${{ secrets.KOMODO_KEY }}
```

---

## 6. Deployment Workflows

### Development (Local)

```bash
# Use Docker Compose directly
docker compose up -d

# Secrets via 1Password CLI
op run -- docker compose up -d
```

### Staging

```bash
# Deploy to staging server via Komodo
dagger call deploy --env=staging --tag=latest
```

### Production

```bash
# Deploy to production with specific tag
dagger call deploy --env=production --tag=v1.2.3
```

### Rollback

```bash
# Rollback to previous version
dagger call deploy --env=production --tag=v1.2.2
```

---

## 7. Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Periphery not connecting | Wrong passkey | Verify PERIPHERY_PASSKEY env var |
| Secrets not loading | Missing token | Check OP_SERVICE_ACCOUNT_TOKEN |
| Pangolin registration fails | DNS not configured | Add DNS records first |
| Build cache misses | Different source | Use --cache-to for persistent caching |

### Debug Commands

```bash
# Check Dagger version
dagger version

# Check Komodo Periphery logs
docker logs komodo-periphery

# Check Pangolin Newt logs
docker logs pangolin-newt

# Verify 1Password connection
op whoami
```

---

## 8. Reference Checklist

### Pre-Deployment

- [ ] All secrets stored in 1Password vault
- [ ] DNS records point to Pangolin domains
- [ ] Periphery agent healthy on target servers
- [ ] Docker images built and pushed to registry
- [ ] Database migrations ready

### Post-Deployment

- [ ] Health checks passing
- [ ] Logs accessible in Dozzle
- [ ] Metrics visible in Beszel
- [ ] SSL certificates valid
- [ ] Rollback tested

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Infrastructure architecture overview
- [DECISION_MATRICES.md](./DECISION_MATRICES.md) - Technology decision guidance
- [/infrastructure/dagger/](./dagger/) - Dagger pipeline details
- [/infrastructure/komodo/](./komodo/) - Komodo deployment details
- [/infrastructure/pangolin/](./pangolin/) - Pangolin networking details
- [/infrastructure/1password/](./1password/) - Secrets management details
