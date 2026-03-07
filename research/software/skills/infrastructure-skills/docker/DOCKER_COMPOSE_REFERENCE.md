# Docker Compose Stacks Analysis - Hackathon Project

## Executive Summary

This document provides a comprehensive analysis of 18 Docker Compose stacks in `/infrastructure/compose/`. The stacks are organized by functional category: Infrastructure, Data Processing, LLM Operations, Package Management, and Observability.

---

## 1. INFRASTRUCTURE FOUNDATION

### 1.1 Supabase (PostgreSQL + Full Stack Backend)
**Type**: Core database infrastructure
**Location**: `/compose/supabase/`
**Compose File**: `docker-compose.yml` (~230+ services/components)

**Key Services**:
- PostgreSQL 15 database
- Kong API Gateway (REST/GraphQL proxy)
- GoTrue (Auth server)
- PostgREST (auto-generated REST API)
- Realtime (WebSocket)
- Studio (Web Dashboard)
- Logflare (Analytics)

**Environment Variables**:
| Variable | Purpose | Example |
|----------|---------|---------|
| POSTGRES_PASSWORD | DB password | op://komodo/supabase/... |
| JWT_SECRET | Authentication | ef6dae4ba7c... |
| ANON_KEY | Public JWT key | eyJhbGciOiJIUzI1NiIs... |
| SERVICE_ROLE_KEY | Service JWT key | eyJhbGciOiJIUzI1NiIs... |
| POSTGRES_HOST | DB host | db |
| POSTGRES_PORT | DB port | 5432 |
| KONG_HTTP_PORT | API port | 8000 |
| SITE_URL | Frontend URL | http://localhost:3000 |

**API Endpoints**:
- REST API: `http://localhost:8000/rest/v1`
- GraphQL: `http://localhost:8000/graphql/v1`
- Auth: `http://localhost:8000/auth/v1`
- Realtime: WebSocket at port 8000
- Dashboard: `http://localhost:3000`

**Dependencies**: None (standalone)
**Build Requirements**: None (pulls images)
**Secrets Management**: Uses Komodo vaults for sensitive data

---

### 1.2 Dragonfly (Redis Cache)
**Type**: In-memory data store
**Location**: `/compose/dragonfly/`
**Compose File**: `compose.yaml`

**Structure**:
```yaml
Service: dragonfly
Image: docker.dragonflydb.io/dragonflydb/dragonfly
Port: 6379
Volumes: dragonflydata:/data
```

**Environment Variables**: None required
**API Endpoints**: Redis-compatible protocol on port 6379
**Dependencies**: None
**Build Requirements**: None

---

### 1.3 Qdrant (Vector Database)
**Type**: Vector search database
**Location**: `/compose/qdrant/`
**Compose File**: `compose.yaml`

**Structure**:
```yaml
Service: qdrant
Image: qdrant/qdrant:latest
Ports: 6333 (REST), 6334 (gRPC)
Volumes: ./qdrant_data
Config: Inline config.yaml (log_level: INFO)
```

**Environment Variables**: None required
**API Endpoints**:
- REST API: `http://localhost:6333`
- gRPC: `localhost:6334`

**Dependencies**: None
**Build Requirements**: None

---

### 1.4 Memgraph (Graph Database)
**Type**: Neo4j alternative for graphs
**Location**: `/compose/memgraph/`
**Compose File**: `compose.yaml`

**Key Services**:
- memgraph-mage (Graph database with algorithms)
- lab (Web UI for visualization)

**Environment Variables**:
```
MEMGRAPH_USER=op://dev-baile/memgraph_credentials/user
MEMGRAPH_PASSWORD=op://dev-baile/memgraph_credentials/password
```

**API Endpoints**:
- Bolt protocol: `localhost:7687`
- HTTPS: `localhost:7444`
- Web UI: `http://localhost:3000`

**Dependencies**: None
**Build Requirements**: None

---

## 2. DATA PROCESSING & ETL

### 2.1 Cognee (Knowledge Graph + Multi-DB)
**Type**: Data processing engine
**Location**: `/compose/cognee/`
**Compose Files**: `compose.yaml`, `.env`

**Key Services** (orchestrates external services):
- PostgreSQL with pgvector
- Memgraph (graph)
- Dragonfly (cache/redis)
- LanceDB (vector - file-based or cloud)

**Environment Variables** (Critical):
```
# LLM Configuration
LLM_API_KEY="your_api_key"
LLM_MODEL="openai/gpt-4o-mini"
LLM_PROVIDER="openai"
EMBEDDING_PROVIDER="openai"
EMBEDDING_MODEL="openai/text-embedding-3-large"

# Databases
DB_PROVIDER=postgres
DB_HOST=postgres
DB_PORT=5432
DB_USERNAME=cognee
DB_PASSWORD=cognee
GRAPH_DATABASE_URL=bolt://memgraph:7687
VECTOR_DB_PROVIDER="lancedb"

# Cache
REDIS_HOST=dragonfly
REDIS_PORT=6379

# Security
ACCEPT_LOCAL_FILE_PATH=True
ALLOW_HTTP_REQUESTS=True
ALLOW_CYPHER_QUERY=True
```

**Dependencies**: 
- PostgreSQL, Memgraph, Dragonfly (external)
- Requires LLM API key

**Build Requirements**: None (runs in Docker)

---

### 2.2 Crawl4AI (Web Scraping + LLM)
**Type**: Web crawling/scraping framework
**Location**: `/compose/crawl4ai/`
**Compose File**: `compose.yaml`

**Structure**:
```yaml
Service: crawl4ai
Image: unclecode/crawl4ai:${TAG:-latest}
Port: 11235
Memory: 4GB limit, 1GB reserved
GPU Support: Optional (ENABLE_GPU=false)
```

**Environment Variables** (from `.llm.env`):
```
OPENAI_API_KEY=
DEEPSEEK_API_KEY=
ANTHROPIC_API_KEY=
GROQ_API_KEY=
TOGETHER_API_KEY=
MISTRAL_API_KEY=
GEMINI_API_TOKEN=
LLM_PROVIDER= (optional override)
INSTALL_TYPE=default
ENABLE_GPU=false
```

**API Endpoints**: Health check at `http://localhost:11235/health`

**Dependencies**: None (standalone)
**Build Requirements**: Optional local build with Dockerfile

---

## 3. PACKAGE & ARTIFACT MANAGEMENT

### 3.1 Forgejo (Git + PyPI/Container Registry)
**Type**: Self-hosted Git with package registry
**Location**: `/compose/forgejo/`
**Compose File**: `compose.yaml`

**Key Services**:
- PostgreSQL 16 (database)
- Forgejo (Git server + registries)

**Environment Variables**:
```
# Database
FORGEJO_DB_USER=forgejo
FORGEJO_DB_PASSWORD=forgejo_password
FORGEJO_DB_NAME=forgejo

# Server
FORGEJO_DOMAIN=localhost
FORGEJO_ROOT_URL=http://localhost:3000
FORGEJO_HTTP_PORT=3000
FORGEJO_SSH_PORT=2222

# Security
FORGEJO_SECRET_KEY= (openssl rand -hex 32)

# Features
FORGEJO_DISABLE_REGISTRATION=false
FORGEJO_ACTIONS_ENABLED=false
FORGEJO__packages__ENABLED=true
FORGEJO__packages__LIMIT_TOTAL_OWNER_COUNT=-1
```

**API Endpoints**:
- Web UI: `http://localhost:3000`
- PyPI Registry: `http://localhost:3000/api/packages/{owner}/pypi`
- Container Registry: `http://localhost:3000/api/v2`
- SSH: `localhost:2222`

**SDK/Access**:
```bash
# Python package installation
pip install --index-url http://localhost:3000/api/packages/{owner}/pypi/simple data-unified

# Publication (.pypirc)
[forgejo]
repository = http://localhost:3000/api/packages/{owner}/pypi
username = {username}
password = {token}
```

**Dependencies**: None
**Build Requirements**: None

---

### 3.2 Garage (S3-Compatible Object Storage)
**Type**: Distributed S3-compatible storage
**Location**: `/compose/garage/`
**Compose File**: `docker-compose.yaml`

**Structure**:
```yaml
Service: garage
Image: dxflrs/garage:v1.0.1
Ports: 
  - 3900: S3 API
  - 3901: RPC
  - 3902: K2V API
  - 3903: Web UI
Config: ./garage.toml
Volumes: garage-meta, garage-data
```

**Environment Variables**:
```
GARAGE_RPC_PORT=3901
GARAGE_S3_API_PORT=3900
GARAGE_K2V_API_PORT=3902
GARAGE_WEB_PORT=3903
GARAGE_ADMIN_PORT=3904
RUST_LOG=garage=info
RPC_SECRET= (openssl rand -hex 32)
ADMIN_API_TOKEN= (openssl rand -base64 32)
S3_REGION=garage
REPLICATION_MODE=1 (1, 2, or 3)
```

**API Endpoints**:
- S3 API: `http://localhost:3900`
- Admin API: `http://localhost:3903`
- K2V (key-value): `http://localhost:3902`

**Dependencies**: None
**Build Requirements**: None (pulls image)
**Configuration**: Requires `./garage.toml`

---

## 4. LLM OPERATIONS & ORCHESTRATION

### 4.1 LiteLLM (LLM Proxy & Management)
**Type**: LLM provider abstraction
**Location**: `/compose/litellm/`
**Compose File**: `compose.yaml`

**Key Services**:
- LiteLLM proxy server
- PostgreSQL 15 (for model/key management)

**Environment Variables**:
```
# Security
LITELLM_MASTER_KEY=op://komodo/litellm/.../LITELLM_MASTER_KEY
LITELLM_SALT_KEY=op://komodo/litellm/.../LITELLM_SALT_KEY

# UI
UI_USERNAME=op://komodo/litellm/.../UI_USERNAME
UI_PASSWORD=op://komodo/litellm/.../UI_PASSWORD

# Database
DATABASE_URL=postgresql://litellm:litellm_password@postgres:5432/litellm
POSTGRES_USER=litellm
POSTGRES_PASSWORD=litellm_password
POSTGRES_DB=litellm

# LLM API Keys (all optional, as needed)
OPENAI_API_KEY=
ANTHROPIC_API_KEY=op://komodo/litellm/.../ANTHROPIC_API_KEY
HF_TOKEN=op://komodo/litellm/.../HF_TOKEN
OPENROUTER_API_KEY=op://komodo/litellm/.../OPENROUTER_API_KEY
AZURE_API_KEY=
GEMINI_API_KEY=op://komodo/litellm/.../GEMINI_API_KEY
GROQ_API_KEY=
TOGETHER_AI_API_KEY=
MISTRAL_API_KEY=
DEEPSEEK_API_KEY=op://komodo/litellm/.../DEEPSEEK_API_KEY

# Observability Integration
LANGFUSE_PUBLIC_KEY=op://komodo/litellm/.../LANGFUSE_PUBLIC_KEY
LANGFUSE_SECRET_KEY=op://komodo/litellm/.../LANGFUSE_SECRET_KEY
```

**API Endpoints**:
- Proxy server: `http://localhost:4000`
- Admin UI: `http://localhost:4000/ui/`

**Configuration**: Requires `./litellm_config.yaml`
**Dependencies**: PostgreSQL
**Build Requirements**: None

---

### 4.2 MLFlow (Model Tracking & Registry)
**Type**: ML experiment tracking
**Location**: `/compose/mlflow/`
**Compose File**: `compose.yaml`

**Key Services**:
- PostgreSQL 15 (backend store)
- MinIO (artifact storage)
- MLFlow server

**Environment Variables**:
```
# Database
POSTGRES_USER= (required)
POSTGRES_PASSWORD= (required)
POSTGRES_DB= (required)

# MinIO S3
MINIO_ROOT_USER= (required)
MINIO_ROOT_PASSWORD= (required)
MINIO_HOST=minio
MINIO_PORT=9000
MINIO_BUCKET=mlflow

# MLFlow
MLFLOW_BACKEND_STORE_URI=postgresql://user:pass@postgres:5432/db
MLFLOW_DEFAULT_ARTIFACT_ROOT=s3://mlflow
MLFLOW_S3_ENDPOINT_URL=http://minio:9000
MLFLOW_HOST=0.0.0.0
MLFLOW_PORT=5000
AWS_DEFAULT_REGION=us-east-1
```

**API Endpoints**:
- UI: `http://localhost:5000`
- Tracking API: `http://localhost:5000`

**Dependencies**: PostgreSQL, MinIO
**Build Requirements**: None

---

### 4.3 Langfuse (LLM Observability)
**Type**: LLM trace logging & analytics
**Location**: `/compose/langfuse/`
**Compose File**: `compose.yaml`

**Key Services**:
- Langfuse server
- PostgreSQL 15

**Environment Variables**:
```
# Application
LANGFUSE_VERSION=2
LANGFUSE_PORT=3000

# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=postgres
POSTGRES_PORT=5432

# Security (REQUIRED - generate with: openssl rand -base64 32)
NEXTAUTH_SECRET=
SALT=
ENCRYPTION_KEY=

# Optional
LANGFUSE_ENABLE_EXPERIMENTAL_FEATURES=false
TELEMETRY_ENABLED=true
LANGFUSE_DEFAULT_PROJECT_ID=
LANGFUSE_DEFAULT_PROJECT_ROLE=
```

**API Endpoints**:
- Dashboard: `http://localhost:3000`
- API: `http://localhost:3000/api`
- SDKs: Python, JS/TS available

**Dependencies**: PostgreSQL
**Build Requirements**: None

---

## 5. WORKFLOW & ORCHESTRATION

### 5.1 Dagster (Data Orchestration)
**Type**: Data pipeline orchestration
**Location**: `/compose/dagster/`
**Compose File**: `docker-compose.yaml`

**Key Services**:
- PostgreSQL 15 (state storage)
- Dagster User Code (gRPC server)
- Dagster Webserver (UI + GraphQL API)
- Dagster Daemon (scheduler/launcher)

**Environment Variables** (from `.env.example`):
```
# PostgreSQL
DAGSTER_POSTGRES_USER=dagster
DAGSTER_POSTGRES_PASSWORD=dagster_password
DAGSTER_POSTGRES_DB=dagster

# Forgejo PyPI Registry (for data-unified package)
FORGEJO_URL=http://forgejo:3000
FORGEJO_OWNER=data
FORGEJO_USER=
FORGEJO_TOKEN=
DATA_UNIFIED_VERSION=0.1.0

# GitHub API
GITHUB_TOKEN=ghp_your_token_here
GITHUB_INCREMENTAL_DAYS=30

# Ports
DAGSTER_WEBSERVER_PORT=3001
```

**API Endpoints**:
- Dashboard: `http://localhost:3001`
- GraphQL API: `http://localhost:3001/graphql`
- Docs: `http://localhost:3001/docs`

**Build Requirements**: 
- `Dockerfile` - Dagster image
- `Dockerfile.user_code` - User code gRPC server
- `Dockerfile.dagster` - Webserver/Daemon

**Dependencies**: 
- PostgreSQL
- Forgejo (for data-unified package)
- Docker socket (for DockerRunLauncher)

**Network Integration**: Connects to `forgejo_network` for package access

---

### 5.2 Pangolin (Network Config Management)
**Type**: Infrastructure as Code
**Location**: `/compose/pangolin/core/`
**Compose File**: `compose.yaml`

**Key Services**:
- Pangolin (main service)
- Gerbil (Wireguard VPN)
- Traefik (reverse proxy)

**Environment Variables**: None required
**API Endpoints**: `http://localhost:3001/api/v1/`
**Dependencies**: None
**Build Requirements**: None
**Network**: Custom network `pangolin` with bridge driver

---

### 5.3 Komodo (Infrastructure Orchestration)
**Type**: Server/container orchestration
**Location**: `/compose/komodo/periphery/`
**Compose File**: `compose.yaml`

**Key Services**:
- Komodo Periphery (agent)

**Environment Variables**:
```
PERIPHERY_ROOT_DIRECTORY=/etc/komodo
PERIPHERY_PASSKEYS=abc123
PERIPHERY_SSL_ENABLED=true
PERIPHERY_DISABLE_TERMINALS=false
PERIPHERY_INCLUDE_DISK_MOUNTS=/etc/hostname
```

**Volumes**:
- Docker socket access
- /proc for process monitoring
- /etc/komodo (agent root)

**Dependencies**: None (standalone agent)
**Build Requirements**: None

---

## 6. DEVELOPMENT & AGENTIC FRAMEWORKS

### 6.1 Agno (Agentic Framework)
**Type**: Agent development framework
**Location**: `/compose/agno/`
**Compose File**: `compose.yaml`

**Key Services**:
- pgvector (Postgres + vector search)
- API server (FastAPI)

**Environment Variables**:
```
# Database
DB_USER=ai
DB_PASSWORD=ai
DB_NAME=ai

# LLM
OPENAI_API_KEY= (required)

# Internal
DB_HOST=pgvector
DB_PORT=5432
WAIT_FOR_DB=True
PRINT_ENV_ON_LOAD=True
```

**API Endpoints**: `http://localhost:8000`

**Build Requirements**: 
- Local Dockerfile in root
- Image: `${IMAGE_NAME:-agent-os}:${IMAGE_TAG:-latest}`
- Volume mounts current directory for development

**Dependencies**: PostgreSQL
**Network**: Custom `agent-os` network

---

### 6.2 Termix (Terminal Multiplexer)
**Type**: Web-based terminal
**Location**: `/compose/termix/`
**Compose File**: `compose.yaml`

**Structure**:
```yaml
Service: termix
Image: ghcr.io/lukegus/termix:latest
Port: 8080
Volumes: termix-data:/app/data
```

**Environment Variables**: None required
**API Endpoints**: `http://localhost:8080`
**Dependencies**: None

---

## 7. SECRETS & CONFIGURATION MANAGEMENT

### 7.1 Infisical (Secrets Management)
**Type**: Secrets management platform
**Location**: `/compose/infisical/`
**Compose File**: Not found (uses `.env.template`)

**Environment Variables** (from template):
```
# Encryption
ENCRYPTION_KEY=6c1fe4e407b8911c104518103505b218
AUTH_SECRET=5lrMXKKWCVocS/uerPsl7V+TX/aaUaI7iDkgl3tSmLE=

# Database
POSTGRES_USER=infisical
POSTGRES_PASSWORD=infisical
POSTGRES_DB=infisical
DB_CONNECTION_URI=postgres://infisical:infisical@db:5432/infisical

# Redis
REDIS_URL=redis://redis:6379

# Server
SITE_URL=http://localhost:8080

# Optional integrations (GitHub, Gitlab, etc)
CLIENT_ID_GITHUB=
CLIENT_SECRET_GITHUB=
...
```

**Status**: No compose file (incomplete)

---

## SERVICE DEPENDENCY MATRIX

```
┌─────────────────────────────────────────────────────────────────┐
│ SERVICE DEPENDENCIES & DATA FLOW                                │
└─────────────────────────────────────────────────────────────────┘

CORE INFRASTRUCTURE:
├── Supabase (PostgreSQL + APIs)
│   └── depends on: PostgreSQL, Redis (optional)
├── Forgejo (Git + PyPI Registry)
│   └── depends on: PostgreSQL
├── Garage (S3 Storage)
│   └── depends on: none
├── Dragonfly (Cache)
│   └── depends on: none
├── Qdrant (Vector DB)
│   └── depends on: none
├── Memgraph (Graph DB)
│   └── depends on: none

DATA PROCESSING:
├── Cognee (Knowledge Graph)
│   └── depends on: PostgreSQL, Memgraph, Dragonfly, LanceDB, LLM API
├── Crawl4AI (Web Scraping)
│   └── depends on: LLM API keys

ORCHESTRATION:
├── Dagster (Pipeline Orchestration)
│   ├── depends on: PostgreSQL, Forgejo, GitHub API, Docker
│   └── provides data to: Supabase, S3, BigQuery, Snowflake
├── Pangolin (Network Management)
│   └── depends on: none
└── Komodo Periphery (Infrastructure Agent)
    └── depends on: Docker, /proc, /etc/komodo

LLM OPERATIONS:
├── LiteLLM (LLM Proxy)
│   ├── depends on: PostgreSQL, LLM API keys
│   └── integrates with: Langfuse
├── MLFlow (Model Tracking)
│   ├── depends on: PostgreSQL, MinIO
│   └── provides: Model registry, Experiment tracking
└── Langfuse (LLM Observability)
    └── depends on: PostgreSQL

DEVELOPMENT:
├── Agno (Agent Framework)
│   ├── depends on: PostgreSQL (pgvector)
│   └── needs: OPENAI_API_KEY
└── Termix (Terminal)
    └── depends on: none
```

---

## ENVIRONMENT VARIABLE MANAGEMENT MATRIX

| Service | Type | Required | Optional | Template File | Uses Vault |
|---------|------|----------|----------|---------------|-----------|
| **Supabase** | Secrets | POSTGRES_PASSWORD | OPENAI_API_KEY | N/A | Yes (Komodo) |
| **Forgejo** | Config | FORGEJO_DB_PASSWORD, SECRET_KEY | DOMAIN, HTTP_PORT | `.env.example` | No |
| **Garage** | Config | RPC_SECRET, ADMIN_API_TOKEN | BOOTSTRAP_PEERS | `.env` | No |
| **Dragonfly** | Config | None | RUST_LOG | N/A | No |
| **Qdrant** | Config | None | None | N/A | No |
| **Memgraph** | Secrets | PASSWORD | None | `.env.template` | Yes (OnePassword) |
| **Cognee** | Config | LLM_API_KEY | 15+ database/LLM configs | `.env` | No |
| **Crawl4AI** | Secrets | LLM_API_KEY (at least 1) | Multiple LLM providers | `.llm.env` | No |
| **Dagster** | Config | GITHUB_TOKEN, FORGEJO_TOKEN | DAGSTER_WEBSERVER_PORT | `.env.example` | No |
| **Pangolin** | Config | None | None | N/A | No |
| **Komodo** | Config | None | PERIPHERY_PASSKEYS | N/A | No |
| **Agno** | Config | OPENAI_API_KEY | DB_*, custom ports | N/A | No |
| **Termix** | Config | None | PORT | N/A | No |
| **LiteLLM** | Secrets | LITELLM_MASTER_KEY | Multiple LLM keys | `.env.template` | Yes (Komodo vault) |
| **MLFlow** | Config | POSTGRES_*, MINIO_* | AWS_DEFAULT_REGION | N/A | No |
| **Langfuse** | Secrets | NEXTAUTH_SECRET, SALT, ENCRYPTION_KEY | TELEMETRY_ENABLED | N/A | No |
| **Infisical** | Secrets | ENCRYPTION_KEY, AUTH_SECRET | OAuth tokens | `.env.template` | No |

---

## NETWORK ARCHITECTURE

### Custom Networks
```
forgejo_network       - Shared by: Forgejo, Dagster
dagster_network       - Dagster internal
agent-os             - Agno framework
pangolin             - Pangolin services
mlflow-network       - MLFlow ecosystem
default (Docker)     - Most other services
```

### Port Allocation Reference
```
Port Range 3000-3999  (Web UIs):
  3000: Forgejo, Supabase Studio, Memgraph Lab, Agno, Termix
  3001: Dagster Webserver, Pangolin API
  3903: Garage Web UI

Port Range 4000-4999 (APIs):
  4000: LiteLLM
  4001: Langfuse

Port Range 5000-5999 (Databases/Data):
  5432: PostgreSQL instances (Supabase, Forgejo, Langfuse, MLFlow, etc.)
  5000: MLFlow UI

Port Range 6000-6999 (Search/Graph):
  6333: Qdrant REST
  6334: Qdrant gRPC
  6379: Dragonfly/Redis
  6543: Supabase Supavisor (pooling)

Port Range 7000-7999 (Graph/Additional):
  7687: Memgraph Bolt
  7444: Memgraph HTTPS

Port Range 8000-8999 (APIs/Secondary):
  8000: Supabase Kong (API Gateway)
  8000: Crawl4AI health
  8080: Infisical, Termix
  8443: Supabase Kong HTTPS

Port Range 9000-9999 (Object Storage):
  9000: MinIO S3 API
  9001: MinIO Console

Port Range 11000+:
  11235: Crawl4AI
```

---

## BUILD & DEPLOYMENT REQUIREMENTS

### Services Requiring Build
1. **Dagster** (3 Dockerfiles):
   - `Dockerfile` - Main webserver/daemon
   - `Dockerfile.user_code` - gRPC server for pipeline code
   - `Dockerfile.dagster` - Reusable Dagster image

2. **Agno**:
   - `Dockerfile` - FastAPI server with pgvector

3. **Crawl4AI** (Optional):
   - `Dockerfile` - Local build with INSTALL_TYPE/GPU options

### Services Using Pre-built Images
All others pull from registries:
- `docker.io`: Most official images
- `ghcr.io`: GitHub Container Registry (LiteLLM, MLFlow, Komodo, Termix)
- `codeberg.org`: Forgejo
- `docker.dragonflydb.io`: Dragonfly

---

## OBSERVABILITY & INTEGRATION CHAIN

### Data Collection Pipeline
```
Crawl4AI → Cognee → PostgreSQL
   ↓
Dagster (orchestrates) → GitHub data → Cognee/PostgreSQL
   ↓
MLFlow (tracks models) → PostgreSQL + MinIO
   ↓
Langfuse (traces LLM calls) → PostgreSQL
   ↓
LiteLLM (proxies requests) → Integrated with Langfuse
   ↓
Supabase (stores results) → PostgreSQL + APIs
   ↓
Qdrant (vector search) ← Cognee embeddings
```

### Authentication & Secrets Chain
```
Komodo vault (.env.template files)
    ↓
LiteLLM, Memgraph, Supabase secrets
    ↓
Docker Compose env substitution
    ↓
Running services
```

---

## SERVICE MANAGEMENT MATRIX

| Service | Env Vars | Image Build | Secrets | Config Files | Health Check |
|---------|----------|------------|---------|--------------|--------------|
| Supabase | 20+ | No | Vault refs | docker-compose.yml | Script-based |
| Forgejo | 15+ | No | PASSWORD, SECRET_KEY | compose.yaml | pg_isready |
| Garage | 8+ | No | RPC_SECRET | garage.toml + .env | None |
| Dragonfly | 2 | No | None | compose.yaml | None |
| Qdrant | 0 | No | None | Inline config | None |
| Memgraph | 2 | No | Vault refs | compose.yaml | None |
| Cognee | 40+ | No | LLM_API_KEY | .env | None |
| Crawl4AI | 10+ | Optional | LLM keys | .llm.env | HTTP (11235) |
| Dagster | 12+ | 3x Yes | GITHUB_TOKEN | workspace.yaml | pg_isready, HTTP |
| Pangolin | 0 | No | None | config/*.yml | HTTP (3001) |
| Komodo | 4+ | No | PASSKEYS | periphery.config.toml | None |
| Agno | 8+ | Yes | OPENAI_API_KEY | None | HTTP (8000) |
| Termix | 1 | No | None | compose.yaml | None |
| LiteLLM | 20+ | No | Vault refs | litellm_config.yaml | None |
| MLFlow | 12+ | No | None | compose.yaml | HTTP (5000) |
| Langfuse | 8+ | No | Random keys | compose.yaml | HTTP (3000) |
| Infisical | 30+ | No | Vault refs | .env.template | None |

---

## DEPLOYMENT CHECKLIST

### Pre-deployment
- [ ] Generate security keys (openssl rand -hex 32)
- [ ] Copy all .env.example/.env.template files to .env
- [ ] Fill in LLM API keys (OpenAI, Anthropic, etc.)
- [ ] Fill in GitHub token for Dagster
- [ ] Setup Komodo vault references for sensitive data
- [ ] Configure Forgejo registry credentials if using data-unified
- [ ] Verify port availability (3000-11235 range)

### Post-deployment
- [ ] Test Supabase: `curl http://localhost:8000/rest/v1`
- [ ] Test Forgejo: Visit `http://localhost:3000`
- [ ] Test Dagster: Visit `http://localhost:3001`
- [ ] Test LiteLLM: Visit `http://localhost:4000/ui/`
- [ ] Test Langfuse: Visit `http://localhost:3000`
- [ ] Verify PostgreSQL databases: `psql postgresql://user:pass@localhost:5432/db`
- [ ] Test Crawl4AI: `curl http://localhost:11235/health`

---

## CRITICAL INTEGRATION POINTS

1. **Forgejo ↔ Dagster**: data-unified package installation via PyPI
2. **Dagster ↔ Supabase**: Data warehouse for pipeline results
3. **Cognee ↔ Memgraph**: Graph data storage
4. **Cognee ↔ Dragonfly**: Cache for embeddings
5. **LiteLLM ↔ Langfuse**: LLM call tracing
6. **Crawl4AI ↔ Cognee**: Web content processing
7. **All LLM services**: Require external API keys (OpenAI, Anthropic, etc.)
8. **Dagster ↔ GitHub**: Repository data ingestion

---

## KNOWN LIMITATIONS & NOTES

1. **Infisical**: Only .env.template provided, no compose.yaml
2. **Pangolin/Komodo**: Minimal/example configurations
3. **Garage**: Single-node setup (REPLICATION_MODE=1)
4. **Port Conflicts**: Ensure ports 3000-11235 available
5. **Docker Socket**: Dagster & Komodo need `/var/run/docker.sock` access
6. **Secrets Management**: Heavy reliance on Komodo vault for production
7. **Network Isolation**: Most services on isolated networks but interconnected
8. **Database Sizing**: Default PostgreSQL with minimal resources

---

# Service Management Matrix

## SERVICE MATRIX

| # | Service | Type | ENV | BUILD | Secrets | Config | Dependencies | Status | Priority |
|---|---------|------|-----|-------|---------|--------|--------------|--------|----------|
| 1 | **Supabase** | Core DB | 20+ | No | Vault refs | compose.yml | None | Ready | Critical |
| 2 | **Forgejo** | Registry | 15+ | No | 2 | compose.yaml | None | Ready | Critical |
| 3 | **Dagster** | Orchestration | 12+ | 3x Yes | 2 | 3 files | PG, Forgejo, GH | Ready | Critical |
| 4 | **LiteLLM** | LLM Proxy | 20+ | No | Vault refs | litellm_config.yaml | PostgreSQL | Ready | High |
| 5 | **Cognee** | Data Engine | 40+ | No | LLM key | .env | PG, Memgraph, Cache | Ready | High |
| 6 | **Langfuse** | Observability | 8+ | No | 3 keys | compose.yaml | PostgreSQL | Ready | High |
| 7 | **Crawl4AI** | Web Scraper | 10+ | Opt | LLM keys | .llm.env | None | Ready | Medium |
| 8 | **MLFlow** | Model Tracking | 12+ | No | None | compose.yaml | PG, MinIO | Ready | Medium |
| 9 | **Agno** | Agent Framework | 8+ | Yes | 1 key | None | PostgreSQL | Ready | Medium |
| 10 | **Garage** | S3 Storage | 8+ | No | 2 | garage.toml | None | Ready | Medium |
| 11 | **Memgraph** | Graph DB | 2 | No | Vault refs | compose.yaml | None | Ready | Medium |
| 12 | **Qdrant** | Vector DB | 0 | No | None | Inline | None | Ready | Low |
| 13 | **Dragonfly** | Cache | 2 | No | None | compose.yaml | None | Ready | Low |
| 14 | **Pangolin** | Network Mgmt | 0 | No | None | config/*.yml | None | Example | Low |
| 15 | **Komodo** | Infra Agent | 4+ | No | 1 | config.toml | Docker socket | Example | Low |
| 16 | **Termix** | Terminal | 1 | No | None | compose.yaml | None | Ready | Utility |
| 17 | **Infisical** | Secrets Mgmt | 30+ | No | Vault refs | .env.template | None | Incomplete | Low |

---

## ENVIRONMENT SETUP BY CATEGORY

### Critical Path (Required for MVP)
```
1. Supabase          → Primary database & auth
2. Forgejo           → Package registry for data-unified
3. Dagster           → Pipeline orchestration
4. LiteLLM           → LLM abstraction layer
5. Langfuse          → Observability
```

### Data Processing (Recommended)
```
6. Cognee            → Knowledge graph processing
7. Crawl4AI          → Web content extraction
8. Memgraph          → Graph database
9. Dragonfly         → Cache layer
10. Qdrant           → Vector search
```

### Advanced (Optional)
```
11. MLFlow           → Model experiment tracking
12. Agno             → Agent development
13. Garage           → S3-compatible storage
14. Pangolin         → Infrastructure networking
15. Komodo           → Server orchestration
```

---

## SECRETS & KEYS REQUIRED

### API Keys (External Services)
- **OpenAI** OPENAI_API_KEY (for LLMs, Supabase SQL editor)
- **Anthropic** ANTHROPIC_API_KEY (optional, for multi-LLM)
- **Other LLMs**: Groq, DeepSeek, Mistral, etc. (optional)
- **GitHub** GITHUB_TOKEN (Dagster, for repo data)

### Generated Keys (one-time, store securely)
- **Forgejo** FORGEJO_SECRET_KEY (openssl rand -hex 32)
- **Garage** RPC_SECRET, ADMIN_API_TOKEN
- **Langfuse** NEXTAUTH_SECRET, SALT, ENCRYPTION_KEY
- **Infisical** ENCRYPTION_KEY, AUTH_SECRET
- **LiteLLM** LITELLM_MASTER_KEY, LITELLM_SALT_KEY
- **Supabase** JWT_SECRET, ANON_KEY, SERVICE_ROLE_KEY

### Vault-Managed (Komodo)
- Supabase credentials
- LiteLLM keys
- Memgraph credentials
- Various API keys

---

## PORT ASSIGNMENTS

### Web UIs (3000-3999)
```
3000: Forgejo, Supabase Studio, Memgraph Lab, Agno
3001: Dagster, Pangolin API
3903: Garage Web UI
```

### APIs (4000-4999)
```
4000: LiteLLM
```

### Databases (5432+)
```
5432: Multiple PostgreSQL instances
5000: MLFlow UI
```

### Search & Cache (6333-6379)
```
6333: Qdrant REST
6334: Qdrant gRPC
6379: Dragonfly/Redis
```

### Graph (7687+)
```
7687: Memgraph Bolt
```

### Storage & APIs (8000-8999)
```
8000: Supabase Kong API Gateway
8080: Infisical, Termix
```

### Object Storage (9000-9999)
```
9000: MinIO S3 API
9001: MinIO Console
```

### Special (11235+)
```
11235: Crawl4AI
```

---

## ENVIRONMENT FILE CHECKLIST

### Step 1: Copy Templates
```bash
cd /infrastructure/compose

# Copy all templates
cp cognee/.env.template cognee/.env
cp litellm/.env.template litellm/.env
cp memgraph/.env.template memgraph/.env
cp infisical/.env.template infisical/.env
cp forgejo/.env.example forgejo/.env
cp dagster/.env.example dagster/.env

# Crawl4AI special case
cp crawl4ai/.llm.env.example crawl4ai/.llm.env
```

### Step 2: Generate Secrets
```bash
# Generate Forgejo secret
openssl rand -hex 32  # → FORGEJO_SECRET_KEY

# Generate Garage secrets
openssl rand -hex 32  # → RPC_SECRET
openssl rand -base64 32  # → ADMIN_API_TOKEN

# Generate Langfuse secrets
openssl rand -base64 32  # → NEXTAUTH_SECRET
openssl rand -base64 32  # → SALT
openssl rand -base64 32  # → ENCRYPTION_KEY

# Generate LiteLLM secrets
openssl rand -hex 32  # → LITELLM_MASTER_KEY
openssl rand -hex 32  # → LITELLM_SALT_KEY
```

### Step 3: Fill in API Keys
```bash
# Get these from services:
# - OpenAI: https://platform.openai.com/account/api-keys
# - Anthropic: https://console.anthropic.com/
# - GitHub: https://github.com/settings/tokens
# - Groq, DeepSeek, Mistral, etc.

# Edit each .env file and add your keys
nano cognee/.env
nano litellm/.env
nano crawl4ai/.llm.env
nano dagster/.env
# ... etc
```

### Step 4: Verify Configurations
```bash
# Check all .env files exist
ls -la */\.env

# Verify required vars are set (non-empty)
grep -h "^[A-Z_]*=$" */\.env  # Shows empty vars
```

---

## DEPLOYMENT ORDER

### Phase 1: Infrastructure (No dependencies)
1. Dragonfly (cache)
2. Qdrant (vector DB)
3. Memgraph (graph DB)

### Phase 2: Core Services (Dependencies on Phase 1)
4. Forgejo (needs working before Dagster)
5. Supabase (main DB + APIs)
6. PostgreSQL instances (for other services)

### Phase 3: Orchestration & LLM
7. Dagster (needs Forgejo ready)
8. LiteLLM (LLM proxy)
9. Langfuse (observability)

### Phase 4: Data Processing
10. Cognee (needs Memgraph, Dragonfly, LLMs)
11. Crawl4AI (web scraping)
12. MLFlow (optional, for experiment tracking)

### Phase 5: Development & Utilities
13. Agno (agent framework)
14. Garage (S3 storage)
15. Pangolin (network management)
16. Komodo (infrastructure)
17. Termix (utilities)

---

## HEALTH CHECK COMMANDS

```bash
# Supabase API Gateway
curl http://localhost:8000/rest/v1

# Forgejo
curl http://localhost:3000

# Dagster
curl http://localhost:3001

# LiteLLM
curl http://localhost:4000/health

# Langfuse
curl http://localhost:3000/api/health

# Crawl4AI
curl http://localhost:11235/health

# Qdrant
curl http://localhost:6333/health

# Dragonfly
redis-cli -p 6379 ping

# MLFlow
curl http://localhost:5000

# Memgraph
curl http://localhost:7444

# Garage S3
curl http://localhost:3900

# PostgreSQL instances
psql postgresql://user:pass@localhost:5432/dbname -c "SELECT 1"
```

---

## COMMON ISSUES & SOLUTIONS

### Port Already in Use
```bash
# Find process using port
lsof -i :3000

# Kill process
kill -9 <PID>

# Or change port in .env
FORGEJO_HTTP_PORT=3001
```

### PostgreSQL Connection Failed
```bash
# Check if PostgreSQL is running
docker-compose logs postgres

# Verify credentials in .env
grep POSTGRES_ */\.env

# Check volume mounts
docker volume ls | grep postgres
```

### LLM API Key Invalid
```bash
# Test key directly
curl -H "Authorization: Bearer $OPENAI_API_KEY" \
  https://api.openai.com/v1/models

# Check .env file
cat cognee/.env | grep LLM_API_KEY
```

### Vault References Not Resolving
```bash
# Ensure Komodo vault is running
docker ps | grep komodo

# Check vault CLI
op vault list

# Manually resolve and update .env
op read op://vault/service/key
```

---

## ENVIRONMENT VARIABLE TEMPLATES

### Minimal Setup (.env)
```bash
# Supabase
POSTGRES_PASSWORD=your_secure_password

# Forgejo
FORGEJO_DB_PASSWORD=your_secure_password
FORGEJO_SECRET_KEY=your_hex_key

# Dagster
GITHUB_TOKEN=ghp_your_token
DAGSTER_POSTGRES_PASSWORD=your_secure_password

# LLMs
OPENAI_API_KEY=sk-your-key
ANTHROPIC_API_KEY=sk-ant-your-key

# Langfuse
NEXTAUTH_SECRET=your_random_base64
SALT=your_random_base64
ENCRYPTION_KEY=your_random_base64
```

### Full Setup (.env.example pattern)
See individual `.env.example` and `.env.template` files for complete listings.

---

## INTEGRATION SUMMARY

### Data Flow
```
Web (Crawl4AI)
    ↓
Cognee (Processing)
    ↓
PostgreSQL (Supabase)
    ↓
Dagster (Orchestration) → GitHub Data
    ↓
Qdrant (Vector Search)
MLFlow (Model Tracking)
LiteLLM (LLM Proxy) → Langfuse (Tracing)
    ↓
APIs (Kong/PostgREST)
    ↓
Clients
```

### Storage Architecture
```
Forgejo:    Source code + Python packages
Supabase:   Primary database + auth
Garage:     S3-compatible object storage
Dragonfly:  Cache layer
Memgraph:   Knowledge graphs
Qdrant:     Vector embeddings
MLFlow:     Model artifacts (MinIO)
```

---

## SCALING CONSIDERATIONS

### Single-Node Limitations
- Garage set to REPLICATION_MODE=1 (no redundancy)
- Single PostgreSQL instance (no replication)
- Dragonfly single instance (no clustering)
- Memgraph single instance

### For Production
- Enable Garage replication (mode 2-3)
- Setup PostgreSQL streaming replication
- Use Dragonfly cluster mode
- Add Memgraph replication
- Load balance with Traefik/Kong
- Use managed databases if available
