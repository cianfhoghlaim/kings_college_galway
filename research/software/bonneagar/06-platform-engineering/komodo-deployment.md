# Komodo Deployment and Orchestration

## Executive Summary

Komodo v2 is a distributed orchestration platform for Docker Compose-based deployments. Its Core-Periphery architecture enables centralized management of distributed server fleets with GitOps workflows, automated deployments, and identity-aware ingress via Pangolin integration.

---

## 1. Core-Periphery Architecture

### 1.1 Component Overview

| Component | Role | Deployment |
|-----------|------|------------|
| **Komodo Core** | State engine, UI, API | Single instance |
| **Komodo Periphery** | Execution agent | Every managed server |
| **MongoDB** | Configuration database | With Core |
| **Pangolin** | Identity-aware ingress | Edge/gateway |

### 1.2 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    EXTERNAL ACCESS                           │
│              (Developers, CI/CD, Webhooks)                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      PANGOLIN                               │
│        Identity-Aware Reverse Proxy + WireGuard             │
│                    (OIDC, MFA)                              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    KOMODO CORE                              │
│         Web UI + API + State Management                     │
│                    (Port 9120)                              │
└─────────────────────────────────────────────────────────────┘
        ↓                    ↓                    ↓
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  PERIPHERY   │   │  PERIPHERY   │   │  PERIPHERY   │
│  (Server A)  │   │  (Server B)  │   │  (Server C)  │
│  Port 8120   │   │  Port 8120   │   │  Port 8120   │
└──────────────┘   └──────────────┘   └──────────────┘
        ↓                    ↓                    ↓
   [Docker]            [Docker]            [Docker]
   [Stacks]            [Stacks]            [Stacks]
```

### 1.3 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Core initiates to Periphery | Periphery can be behind NAT |
| Passkey authentication | Simple shared-secret model |
| Stateless Periphery | Easy deployment, no local persistence |
| MongoDB backend | Document store for complex configs |

---

## 2. Core Deployment

### 2.1 Docker Compose Configuration

```yaml
version: "3.8"

services:
  # ===========================================================================
  # KOMODO CORE
  # ===========================================================================
  core:
    image: ghcr.io/moghtech/komodo-core:2-dev
    container_name: komodo-core
    restart: unless-stopped
    ports:
      - "9120:9120"
    environment:
      # General Configuration
      - KOMODO_HOST=https://komodo.example.com
      - KOMODO_TITLE=Infrastructure Orchestrator
      - TZ=Europe/Dublin

      # Database Connection (MongoDB)
      - KOMODO_DATABASE_ADDRESS=mongo:27017
      - KOMODO_DATABASE_USERNAME=komodo
      - KOMODO_DATABASE_PASSWORD=${MONGO_PASSWORD}
      - KOMODO_DATABASE_DB_NAME=komodo

      # Security
      - KOMODO_PASSKEY=${KOMODO_PASSKEY}

      # OIDC (Optional)
      - KOMODO_OIDC_ENABLED=false
      # - KOMODO_OIDC_CLIENT_ID=...
      # - KOMODO_OIDC_PROVIDER=...

    volumes:
      - ./config/core.config.toml:/config/config.toml:ro
      - ./ssh-keys:/home/nonroot/.ssh:ro
    depends_on:
      mongo:
        condition: service_started
    networks:
      - komodo-net

  # ===========================================================================
  # MONGODB
  # ===========================================================================
  mongo:
    image: mongo:6.0
    container_name: komodo-mongo
    restart: unless-stopped
    environment:
      - MONGO_INITDB_ROOT_USERNAME=root
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD}
      - MONGO_INITDB_DATABASE=komodo
    volumes:
      - mongo_data:/data/db
      - mongo_config:/data/configdb
      - ./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro
    networks:
      - komodo-net

  # ===========================================================================
  # LOCAL PERIPHERY (Self-management)
  # ===========================================================================
  periphery:
    image: ghcr.io/moghtech/komodo-periphery:2-dev
    container_name: komodo-periphery-local
    restart: unless-stopped
    environment:
      - KOMODO_HOST=http://core:9120
      - KOMODO_PASSKEY=${KOMODO_PASSKEY}
      - KOMODO_SERVER_NAME=Core-Server
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - /etc/komodo:/etc/komodo
    depends_on:
      - core
    networks:
      - komodo-net

networks:
  komodo-net:
    driver: bridge

volumes:
  mongo_data:
  mongo_config:
```

### 2.2 MongoDB Initialization Script

```javascript
// init-mongo.js
db = db.getSiblingDB('komodo');

db.createUser({
  user: 'komodo',
  pwd: process.env.MONGO_PASSWORD || 'komodo_password',
  roles: [
    { role: 'readWrite', db: 'komodo' }
  ]
});

// Create initial indexes
db.servers.createIndex({ name: 1 }, { unique: true });
db.stacks.createIndex({ name: 1 });
db.deployments.createIndex({ timestamp: -1 });
```

### 2.3 TOML Configuration

```toml
# core.config.toml
[general]
title = "Infrastructure Orchestrator"
host = "https://komodo.example.com"

[database]
address = "mongo:27017"
username = "komodo"
db_name = "komodo"

[security]
# Passkey is loaded from environment

[oidc]
enabled = false
# Uncomment for OIDC:
# enabled = true
# provider = "https://auth.example.com/.well-known/openid-configuration"
# client_id = "komodo"
# redirect_uri = "https://komodo.example.com/auth/oidc/callback"

[logging]
level = "info"
otlp_endpoint = ""  # Optional: OpenTelemetry endpoint
```

---

## 3. Periphery Deployment with Ansible

### 3.1 Ansible Role Installation

```bash
ansible-galaxy role install bpbradley.komodo
```

### 3.2 Inventory Configuration

```yaml
# inventory.yaml
all:
  children:
    komodo_nodes:
      hosts:
        server-a:
          ansible_host: 192.168.1.10
          komodo_server_name: "Production-A"
        server-b:
          ansible_host: 192.168.1.11
          komodo_server_name: "Production-B"
        server-c:
          ansible_host: 192.168.1.12
          komodo_server_name: "Staging"
```

### 3.3 Playbook

```yaml
# deploy_periphery.yaml
---
- name: Deploy Komodo Periphery Agents
  hosts: komodo_nodes
  become: true

  vars_files:
    - secrets/vault.yml

  vars:
    komodo_version: "2-dev"
    komodo_user: "komodo"
    komodo_group: "docker"

    # Auto-registration
    enable_server_management: true
    komodo_core_url: "https://komodo.example.com"

    # Vaulted secrets
    komodo_core_api_key: "{{ vault_komodo_api_key }}"
    komodo_core_api_secret: "{{ vault_komodo_api_secret }}"
    komodo_passkeys: ["{{ vault_komodo_shared_passkey }}"]

  roles:
    - role: bpbradley.komodo
      vars:
        komodo_action: "install"
        komodo_allowed_ips:
          - "127.0.0.1"
          - "{{ komodo_core_ip }}"

  tasks:
    - name: Verify Periphery Service
      systemd:
        name: komodo-periphery
        state: started
        enabled: yes
```

### 3.4 Vault Secrets

```yaml
# secrets/vault.yml (encrypted with ansible-vault)
vault_komodo_api_key: "km-api-key-xxxxx"
vault_komodo_api_secret: "km-api-secret-xxxxx"
vault_komodo_shared_passkey: "super-secure-passkey-xxxxx"
```

### 3.5 Role Variables Reference

| Variable | Default | Purpose |
|----------|---------|---------|
| `komodo_version` | "2-dev" | Binary version to download |
| `komodo_action` | "install" | install, update, uninstall |
| `enable_server_management` | true | Auto-register with Core |
| `komodo_core_url` | "" | Core API endpoint |
| `komodo_passkeys` | [] | List of valid passkeys |
| `komodo_user` | "komodo" | System user for service |
| `komodo_group` | "docker" | Group for Docker access |
| `komodo_logging_level` | "info" | Log verbosity |

---

## 4. Pangolin Integration

### 4.1 Pangolin Stack

```yaml
# docker-compose.pangolin.yaml
services:
  pangolin:
    image: ghcr.io/fosrl/pangolin:latest
    container_name: pangolin
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
      - "51820:51820/udp"  # WireGuard
    environment:
      - PANGOLIN_DOMAIN=network.example.com
      - PANGOLIN_EMAIL=admin@example.com
    volumes:
      - ./pangolin/config:/data/config
      - ./pangolin/letsencrypt:/data/letsencrypt
    networks:
      - komodo-net
```

### 4.2 Traefik Configuration for Komodo

```yaml
# pangolin/config/traefik/dynamic_config.yml
http:
  routers:
    komodo-ui:
      rule: "Host(`komodo.network.example.com`)"
      service: komodo-service
      tls:
        certResolver: letsencrypt
      middlewares:
        - pangolin-auth

  services:
    komodo-service:
      loadBalancer:
        servers:
          - url: "http://komodo-core:9120"

  middlewares:
    pangolin-auth:
      # Enforce OIDC authentication before reaching Komodo
      forwardAuth:
        address: "http://pangolin:8080/verify"
        trustForwardHeader: true
```

### 4.3 Tunneled Periphery Access

For Periphery agents behind NAT/firewalls:

```yaml
# On remote server with Periphery
services:
  newt:
    image: ghcr.io/fosrl/newt:latest
    container_name: newt
    environment:
      - PANGOLIN_SERVER=wg.network.example.com:51820
      - NEWT_HOSTNAME=remote-server
      - NEWT_PORT=8120  # Expose Periphery port through tunnel
    volumes:
      - ./newt/config:/config
    network_mode: host
```

**Result:**
- Periphery accessible at `http://remote-server.pangolin.internal:8120`
- No public port exposure required
- Traffic encrypted via WireGuard

---

## 5. GitOps Workflow

### 5.1 Webhook Configuration

```yaml
# GitHub webhook payload handling
# Komodo Core receives webhooks at /api/v1/webhook/git

# GitHub Repository Settings:
# - Payload URL: https://komodo.example.com/api/v1/webhook/git
# - Content type: application/json
# - Secret: ${WEBHOOK_SECRET}
# - Events: Push, Pull Request
```

### 5.2 Deployment Pipeline

```
Developer Push
      ↓
GitHub Webhook
      ↓
Komodo Core (validates signature)
      ↓
Identifies affected Stack
      ↓
Sends commands to Periphery:
  1. git pull
  2. docker compose build (if needed)
  3. docker compose pull (if using images)
  4. docker compose up -d
      ↓
Streams logs back to UI
```

### 5.3 Stack Definition

```yaml
# In Komodo UI or API
stack:
  name: "my-application"
  server: "Production-A"
  repo:
    url: "git@github.com:org/my-app.git"
    branch: "main"
    path: "/"  # Path to docker-compose.yaml
  webhook_enabled: true
  auto_deploy: true
  environment:
    - DATABASE_URL=postgresql://...
    - REDIS_URL=redis://...
```

---

## 6. Database Migration (v1 to v2)

### 6.1 Schema Changes

Komodo v2 introduced breaking database schema changes:

| v1 | v2 |
|----|-----|
| SQLite/FerretDB v1 | MongoDB or FerretDB v2 |
| Flat configuration | Nested document model |
| Simple passkey | Bi-directional auth |

### 6.2 Migration Process

```bash
# 1. Backup v1 database
docker compose exec core km database export > backup.json

# 2. Start v2 with migration utility
docker run --rm \
  -v ./backup.json:/backup.json \
  -v ./v2-data:/data \
  ghcr.io/moghtech/komodo-util:2-dev \
  migrate --from /backup.json --to /data

# 3. Start v2 Core with migrated data
docker compose -f docker-compose.v2.yaml up -d
```

### 6.3 Rollback Capability

```bash
# If v2 migration fails, rollback schema
docker compose exec core km database v1-downgrade -y
```

---

## 7. Observability

### 7.1 OpenTelemetry Integration

```yaml
# Periphery telemetry configuration
periphery:
  environment:
    - KOMODO_LOGGING_LEVEL=info
    - KOMODO_LOGGING_OTLP_ENDPOINT=http://otel-collector:4317
```

### 7.2 Prometheus Metrics

```yaml
# Prometheus scrape config
scrape_configs:
  - job_name: 'komodo-core'
    static_configs:
      - targets: ['komodo-core:9120']
    metrics_path: /metrics

  - job_name: 'komodo-periphery'
    static_configs:
      - targets:
        - 'server-a:8120'
        - 'server-b:8120'
        - 'server-c:8120'
    metrics_path: /metrics
```

### 7.3 Grafana Dashboard

Key metrics to monitor:

| Metric | Purpose |
|--------|---------|
| `komodo_deployments_total` | Deployment count |
| `komodo_deployment_duration_seconds` | Deployment latency |
| `komodo_periphery_connected` | Agent connectivity |
| `komodo_stack_health` | Stack status |

---

## 8. Implementation Priorities

### Phase 1: Core Deployment
1. Deploy MongoDB and Core
2. Configure passkey authentication
3. Test local Periphery

### Phase 2: Distributed Agents
1. Set up Ansible inventory
2. Deploy Periphery to servers
3. Verify auto-registration

### Phase 3: GitOps Integration
1. Configure GitHub webhooks
2. Create Stack definitions
3. Test automated deployments

### Phase 4: Production Hardening
1. Deploy Pangolin for secure ingress
2. Configure OIDC authentication
3. Set up monitoring and alerting

---

## References

- Komodo Documentation: https://komo.do/docs/
- Komodo GitHub: https://github.com/moghtech/komodo
- Ansible Role: https://github.com/bpbradley/ansible-role-komodo
- Pangolin: https://docs.pangolin.net/
