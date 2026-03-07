# Bunchloch Infrastructure Stack

Bunchloch is the integrated self-hosted infrastructure platform for the hackathon project. It combines container orchestration, zero-trust networking, Git/package hosting, and secrets management into a cohesive stack.

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         BUNCHLOCH STACK                                     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 1PASSWORD CONNECT (Secrets Foundation)                              │   │
│  │  - op-connect-api (8080)                                            │   │
│  │  - op-connect-sync (8081)                                           │   │
│  │  Provides: Centralized secret storage and retrieval                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              │ Secrets via API                             │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ LOCKET (Secrets Sidecar)                                            │   │
│  │  - Provider: op-connect                                             │   │
│  │  - Mode: watch (continuous sync)                                    │   │
│  │  - Output: /run/secrets/locket/* (tmpfs)                           │   │
│  │  Provides: Secure secrets injection into containers                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│              ┌───────────────┼───────────────┐                             │
│              ▼               ▼               ▼                              │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────┐              │
│  │ PANGOLIN      │  │ KOMODO        │  │ FORGEJO           │              │
│  │ (Networking)  │  │ (Containers)  │  │ (Git/Packages)    │              │
│  └───────────────┘  └───────────────┘  └───────────────────┘              │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

## Component Directory Structure

```
infrastructure/bunchloch/
├── README.md                    # This file
├── authentication/              # Standalone auth testing
│   └── docker-compose.yml       # Pocket ID + TinyAuth + Traefik
├── automation/                  # Ansible orchestration
│   ├── compose.yaml             # Ansible Execution Environment
│   ├── SETUP.md
│   └── ansible/
│       ├── inventory/
│       │   └── komodo.yml       # Server definitions
│       └── playbooks/
│           ├── komodo.yml       # Periphery deployment
│           └── periphery.yml    # With Locket integration
├── forgejo/                     # Git + Package Registry
│   ├── compose.yaml
│   └── README.md
├── komodo/                      # Container Orchestration
│   ├── komodo-core/
│   │   ├── mongo.compose.yaml   # Core + MongoDB + Periphery
│   │   └── compose.env
│   └── periphery/
│       └── compose.yaml         # Remote periphery agents
└── pangolin/                    # Zero-Trust Networking
    ├── pangolin-core/
    │   ├── compose.yaml
    │   └── config/
    │       ├── config.yml
    │       ├── traefik/
    │       └── middleware-manager/
    ├── newt/                    # Tunnel agents
    │   └── compose.yaml
    └── olm/                     # Lightweight tunnel client
        └── compose.yaml
```

## Service Relationships

### 1. 1Password Connect → Locket → All Services

**Flow:** 1Password vaults → Connect API → Locket sidecar → Service secrets

1Password Connect provides the centralized secret store. Locket runs as a sidecar container in each stack that needs secrets, watching template files and writing decrypted values to tmpfs volumes.

**Key secrets managed:**
- Database credentials (PostgreSQL, MongoDB)
- Server secrets (JWT, passkeys)
- API tokens (Newt ID, Newt secret)
- OAuth credentials

### 2. Komodo (Container Orchestration)

**Components:**
- **Komodo Core** (port 9120): Web UI and API for managing containers
- **MongoDB**: State database for Komodo
- **Periphery**: Agent deployed on each server to manage Docker

**Architecture:**
- Core runs centrally with outbound-only mode (secure by default)
- Periphery agents connect TO Core (no inbound ports needed)
- Communication secured via passkeys

**Key Features:**
- Stack deployment (Docker Compose stacks)
- Container lifecycle management
- Multi-server orchestration
- GitOps integration (webhooks)

### 3. Pangolin (Zero-Trust Reverse Proxy)

**Core Components:**
- **Pangolin** (port 3001): Identity-aware proxy engine
- **Gerbil** (port 51820/UDP): WireGuard tunnel controller
- **Traefik** (ports 80, 443): Dynamic load balancer
- **Middleware Manager** (port 3456): Traefik rules UI

**Authentication:**
- **Pocket ID** (port 1411): Passkey-based OIDC provider
- **TinyAuth** (port 3000): Forward authentication middleware

**Tunnel Agents:**
- **Newt**: Connects local services to Pangolin
- **OLM**: Lightweight tunnel client for remote sites

**Network Flow:**
```
External Request → Traefik → TinyAuth (auth) → Gerbil (tunnel) → Service
```

### 4. Forgejo (Git & Package Registry)

**Services:**
- PostgreSQL: Git repository database
- Forgejo (ports 3000 HTTP, 2222 SSH): Git server

**Features:**
- Git repositories (GitHub alternative)
- PyPI package registry
- Container registry (OCI)
- CI/CD with Forgejo Actions

**Package URLs:**
- PyPI: `http://forgejo:3000/api/packages/{owner}/pypi`
- Install: `pip install --index-url http://forgejo:3000/api/packages/{owner}/pypi/simple {pkg}`

### 5. Ansible Automation

**Purpose:** Automate Periphery deployment across servers

**Execution Environment:**
- Image: `ghcr.io/bpbradley/ansible/komodo-ee:latest`
- Integrated with 1Password Connect for vault secrets
- SSH access to target servers

**Playbooks:**
- `komodo.yml`: Deploy/update Komodo periphery
- `periphery.yml`: Periphery with Locket integration

## Startup Order

For full stack deployment, services should start in this order:

1. **1Password Connect** - Secrets foundation (must be running first)
2. **Locket sidecars** - Wait for Connect to be healthy
3. **PostgreSQL** (Pangolin & Forgejo) - Database initialization
4. **MongoDB** (Komodo) - State storage
5. **Pangolin services** - Depends on Locket & PostgreSQL
6. **Komodo Core** - Depends on MongoDB
7. **Forgejo** - Depends on PostgreSQL
8. **Newt/OLM tunnels** - Depends on Pangolin network
9. **Komodo Periphery** - Depends on Core

## Quick Start

### 1. Start Secrets (Required First)
```bash
cd infrastructure/stacks/secrets/onepassword
docker compose up -d
```

### 2. Start Pangolin Core
```bash
cd infrastructure/bunchloch/pangolin/pangolin-core
docker compose up -d
```

### 3. Start Komodo Core
```bash
cd infrastructure/bunchloch/komodo/komodo-core
docker compose up -d
```

### 4. Start Forgejo
```bash
cd infrastructure/bunchloch/forgejo
docker compose up -d
```

### 5. Deploy Remote Periphery (via Ansible)
```bash
cd infrastructure/bunchloch/automation
docker compose run --rm ansible ansible-playbook playbooks/komodo.yml
```

## Networks

| Network | Type | Services |
|---------|------|----------|
| `pangolin` | bridge | Pangolin, Gerbil, Traefik, auth services |
| `forgejo_network` | bridge | Forgejo, PostgreSQL |
| `default` | bridge | Komodo Core, MongoDB, Periphery |

## Ports Reference

| Service | Port | Protocol | Purpose |
|---------|------|----------|---------|
| 1Password API | 8080 | HTTP | Secret retrieval |
| 1Password Sync | 8081 | HTTP | Credential sync |
| Komodo Core | 9120 | TCP/WSS | Cluster control |
| Pangolin | 3001 | HTTP | Identity proxy |
| Gerbil | 51820 | UDP | WireGuard tunnel |
| Traefik | 80, 443 | TCP | HTTP/S routing |
| Pocket ID | 1411 | HTTP | OIDC provider |
| TinyAuth | 3000 | HTTP | Forward auth |
| Forgejo HTTP | 3000 | HTTP | Git web UI |
| Forgejo SSH | 2222 | SSH | Git clone |

## Related Documentation

- **Komodo**: `/research/infrastructure/komodo/`
- **Pangolin**: `/research/infrastructure/pangolin/`
- **Dagger Deployment**: `/dagger/README.md`
- **OpenSpec Infrastructure Specs**: `/openspec/specs/komodo-infrastructure/`
