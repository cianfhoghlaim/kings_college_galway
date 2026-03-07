# Docker Compose Architecture Overview

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     HACKATHON DOCKER COMPOSE ECOSYSTEM                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│ FRONTEND & EXTERNAL ACCESS LAYER                                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Web Browsers / API Clients / CLI Tools                                       │
│         │                │                │                                  │
│         ├────────────────┼────────────────┤                                  │
│         │                │                │                                  │
│    3000,3001          8000,4000        11235,6333                           │
│         │                │                │                                  │
└─────────┼────────────────┼────────────────┼───────────────────────────────┘
          │                │                │
          v                v                v
┌────────────────┬──────────────────┬──────────────────┐
│                │                  │                  │
│   WEB UIs      │     API GATEWAYS │   DATA ACCESS    │
│   (Port 3000)  │     (Port 8000)  │   (Port 11235)   │
│                │                  │                  │
├────────────────┼──────────────────┼──────────────────┤
│ ○ Forgejo      │ ○ Kong/Supabase  │ ○ Crawl4AI       │
│ ○ Agno         │ ○ PostgREST      │   (Web Scraper)  │
│ ○ Memgraph Lab │ ○ GoTrue (Auth)  │                  │
│ ○ Dagster      │ ○ Realtime       │                  │
│ ○ MLFlow       │                  │                  │
│ ○ Termix       │                  │                  │
└────────────────┴──────────────────┴──────────────────┘


┌──────────────────────────────────────────────────────────────────────────────┐
│ APPLICATION LAYER                                                            │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │  Orchestration   │    │  LLM Services   │    │  Data Processing          │
│  │                  │    │                  │    │                          │
│  ├──────────────┤    ├──────────────┤    ├──────────────┤                  │
│  │ ○ Dagster   │    │ ○ LiteLLM    │    │ ○ Cognee     │                  │
│  │   (ETL)     │    │   (Proxy)    │    │   (Graph)    │                  │
│  │ ○ Pangolin  │    │ ○ Langfuse   │    │ ○ Crawl4AI   │                  │
│  │   (Network) │    │   (Tracing)  │    │   (Scraper)  │                  │
│  │ ○ Komodo    │    │ ○ MLFlow     │    │ ○ Agno       │                  │
│  │   (Infra)   │    │   (Tracking) │    │   (Agents)   │                  │
│  │ ○ Termix    │    │              │    │              │                  │
│  │   (Shell)   │    │              │    │              │                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
│         │                   │                    │                          │
│         └───────────────────┼────────────────────┘                          │
│                             │                                               │
└─────────────────────────────┼───────────────────────────────────────────────┘
                              │
                              v
┌──────────────────────────────────────────────────────────────────────────────┐
│ DATA LAYER                                                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐    │
│  │  PRIMARY DATA   │  │ SPECIALIZED DBS │  │    CACHE & SEARCH       │    │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐    │
│  │ ○ Supabase      │  │ ○ Memgraph      │  │ ○ Qdrant                │    │
│  │   PostgreSQL    │  │   (Graph DB)    │  │   (Vector Search)       │    │
│  │   + Auth        │  │ ○ LanceDB       │  │ ○ Dragonfly             │    │
│  │   + APIs        │  │   (Vector)      │  │   (Redis Cache)         │    │
│  │                 │  │                 │  │                         │    │
│  │ ○ Multiple      │  │ ○ Kuzu          │  │ ○ MinIO                 │    │
│  │   PostgreSQL    │  │   (Graph)       │  │   (S3-compatible)       │    │
│  │   instances     │  │                 │  │                         │    │
│  │   (Forgejo,     │  │ ○ Neo4j         │  │ ○ Garage                │    │
│  │    Dagster,     │  │   (Graph)       │  │   (S3-compatible)       │    │
│  │    LiteLLM,     │  │                 │  │                         │    │
│  │    Langfuse,    │  │                 │  │                         │    │
│  │    MLFlow)      │  │                 │  │                         │    │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘


┌──────────────────────────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE LAYER                                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ○ Docker Networks       ○ Docker Volumes    ○ External APIs               │
│    - forgejo_network       - postgres data      - OpenAI                    │
│    - dagster_network       - cache data         - Anthropic                 │
│    - agent-os              - search indices     - GitHub                    │
│    - pangolin              - artifacts          - Other LLM providers       │
│    - mlflow-network                                                         │
│                                                                               │
│  ○ Registry (Forgejo)   ○ Secrets (Vault)                                   │
│    - PyPI packages        - Komodo vault                                    │
│    - Docker images        - API keys                                        │
│    - Data-unified pkg     - DB passwords                                    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA PROCESSING PIPELINE                             │
└─────────────────────────────────────────────────────────────────────────────┘

INGESTION → PROCESSING → STORAGE → QUERYING → ANALYTICS
   │          │            │         │          │
   v          v            v         v          v

Crawl4AI    Cognee      Supabase   Qdrant    Langfuse
   │          │            │         │          │
   └──────────┴────────────┴─────────┴──────────┘
              │
              v
          Dagster
         (Orchestrator)
              │
       ┌──────┼──────┐
       │      │      │
       v      v      v
     GitHub  APIs  Reports
     Data  Output  Output
```

---

## Service Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SERVICE DEPENDENCIES CHART                               │
└─────────────────────────────────────────────────────────────────────────────┘

TIER 0 - STANDALONE (No Dependencies)
├── Dragonfly (Redis-compatible cache)
├── Qdrant (Vector database)
├── Termix (Web terminal)
├── Pangolin (Network configuration)
└── Komodo (Infrastructure agent)

TIER 1 - DATABASE-DEPENDENT
├── Forgejo → PostgreSQL
├── Supabase → PostgreSQL + external services
├── Memgraph (optional PostgreSQL extension)
├── LiteLLM → PostgreSQL
├── Langfuse → PostgreSQL
└── MLFlow → PostgreSQL + MinIO

TIER 2 - MULTI-SERVICE DEPENDENT
├── Cognee → PostgreSQL + Memgraph + Dragonfly + LanceDB + LLM API
├── Dagster → PostgreSQL + Forgejo + GitHub + Docker socket
├── Crawl4AI → LLM API keys
└── Agno → PostgreSQL (pgvector)

TIER 3 - DEPENDENT ON TIER 2
└── (None - everything converges at Supabase/Dagster)
```

---

## Network Topology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DOCKER NETWORK TOPOLOGY                               │
└─────────────────────────────────────────────────────────────────────────────┘

ISOLATED NETWORKS:
  
  forgejo_network
  ├── forgejo
  └── forgejo_db

  dagster_network
  ├── dagster_postgresql
  ├── dagster_user_code
  ├── dagster_webserver
  └── dagster_daemon
  
  agent-os
  ├── pgvector (Agno)
  └── api (Agno FastAPI)
  
  pangolin
  ├── pangolin
  ├── gerbil
  └── traefik
  
  mlflow-network
  ├── postgres (MLFlow)
  ├── minio
  └── mlflow

BRIDGE NETWORKS (Services on default Docker bridge):
  ├── Supabase (multiple services)
  ├── LiteLLM + PostgreSQL
  ├── Langfuse + PostgreSQL
  ├── Memgraph + Lab
  ├── Dragonfly
  ├── Qdrant
  ├── Crawl4AI
  ├── Garage
  ├── Infisical
  └── Termix

SHARED CONNECTIONS:
  - Forgejo → Dagster (via external forgejo_network)
  - All services → External APIs (OpenAI, Anthropic, GitHub, etc.)
```

---

## Port Map by Service

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PORT ALLOCATION MAP                                  │
└─────────────────────────────────────────────────────────────────────────────┘

WEB INTERFACES (3000-3999)
├── 3000: Forgejo (Git)
├── 3000: Supabase Studio
├── 3000: Memgraph Lab  
├── 3000: Agno API
├── 3000: Langfuse Dashboard
├── 3000: Termix
├── 3001: Dagster
├── 3001: Pangolin API
└── 3903: Garage Web UI

API ENDPOINTS (4000-4999)
├── 4000: LiteLLM Proxy
└── (Various internal APIs)

DATABASES & STORES (5000-5999)
├── 5000: MLFlow
├── 5432: PostgreSQL (multiple instances)
└── (Database internal ports)

SEARCH & VECTOR DBS (6300-6399)
├── 6333: Qdrant REST
└── 6334: Qdrant gRPC

CACHE & KV (6370-6379)
├── 6379: Dragonfly / Redis

DATABASE POOLING (6500-6599)
└── 6543: Supabase Supavisor

GRAPH DATABASES (7600-7699)
├── 7687: Memgraph Bolt
└── 7444: Memgraph HTTPS

API GATEWAYS & SERVICES (8000-8999)
├── 8000: Supabase Kong (API Gateway)
├── 8443: Supabase Kong HTTPS
├── 8080: Infisical
└── 8080: Termix

OBJECT STORAGE (9000-9999)
├── 9000: MinIO / Garage S3 API
└── 9001: MinIO Console

SPECIAL PORTS (11000+)
└── 11235: Crawl4AI

SSH (2222)
└── 2222: Forgejo SSH
```

---

## Configuration Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CONFIGURATION DEPENDENCIES                              │
└─────────────────────────────────────────────────────────────────────────────┘

ENVIRONMENT VARIABLES (from .env files)
├── LLM API Keys (External)
│   ├── OPENAI_API_KEY
│   ├── ANTHROPIC_API_KEY
│   ├── GROQ_API_KEY
│   ├── DEEPSEEK_API_KEY
│   └── ... (15+ other providers)
│
├── GitHub Tokens (External)
│   ├── GITHUB_TOKEN (for Dagster)
│   └── GITHUB_API_KEY
│
├── Generated Secrets (Stored Securely)
│   ├── Forgejo: FORGEJO_SECRET_KEY
│   ├── Garage: RPC_SECRET, ADMIN_API_TOKEN
│   ├── Langfuse: NEXTAUTH_SECRET, SALT, ENCRYPTION_KEY
│   ├── LiteLLM: LITELLM_MASTER_KEY, LITELLM_SALT_KEY
│   ├── Infisical: ENCRYPTION_KEY, AUTH_SECRET
│   └── Supabase: JWT_SECRET, ANON_KEY, SERVICE_ROLE_KEY
│
├── Database Credentials (Internal)
│   ├── PostgreSQL: Multiple POSTGRES_USER/PASSWORD pairs
│   ├── Memgraph: MEMGRAPH_USER, MEMGRAPH_PASSWORD
│   └── (Connection strings built from these)
│
├── Service URLs (Internal)
│   ├── FORGEJO_URL → http://forgejo:3000
│   ├── POSTGRES_HOST → postgres/db
│   ├── REDIS_HOST → dragonfly
│   ├── GRAPH_DATABASE_URL → bolt://memgraph:7687
│   └── ... (many others)
│
└── Feature Flags & Configurations
    ├── FORGEJO_DISABLE_REGISTRATION
    ├── FORGEJO_ACTIONS_ENABLED
    ├── LANGFUSE_ENABLE_EXPERIMENTAL_FEATURES
    ├── ENABLE_GPU (Crawl4AI)
    └── REPLICATION_MODE (Garage)

EXTERNAL CONFIGURATION FILES
├── docker-compose.yaml
│   └── (Service definitions, ports, volumes, networks)
│
├── Dockerfile (custom builds)
│   ├── Dagster (3x Dockerfiles)
│   └── Agno
│
├── litellm_config.yaml
│   └── (Model routes, fallbacks, settings)
│
├── garage.toml
│   └── (Storage configuration)
│
├── workspace.yaml (Dagster)
│   └── (Pipeline definitions)
│
├── dagster.yaml (Dagster)
│   └── (Executor, storage, scheduler)
│
└── config files (Pangolin, Traefik, etc.)
    └── (Various infrastructure configurations)
```

---

## Deployment Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DEPLOYMENT SEQUENCE                                     │
└─────────────────────────────────────────────────────────────────────────────┘

START
  │
  ├─ [PHASE 1] Infrastructure Foundation
  │  └─ docker-compose up
  │     ├─ dragonfly (6379) ──────────────────────┐
  │     ├─ qdrant (6333) ──────────────────────────┤
  │     ├─ memgraph (7687) ──────────────────────┤
  │     └─ (No dependencies, can start in parallel)
  │
  ├─ [PHASE 2] Core Services (wait for PG health)
  │  └─ docker-compose up
  │     ├─ forgejo (PostgreSQL + service)
  │     │  ├─ forgejo_db health check
  │     │  └─ forgejo ready
  │     ├─ supabase (many services)
  │     │  └─ postgresql health check
  │     ├─ litellm (PostgreSQL + proxy)
  │     │  └─ postgres health check
  │     ├─ langfuse (PostgreSQL + app)
  │     │  └─ postgres health check
  │     └─ mlflow (PostgreSQL + MinIO)
  │        ├─ postgres health check
  │        └─ minio health check
  │
  ├─ [PHASE 3] Orchestration & Observability
  │  └─ docker-compose up
  │     ├─ dagster (needs Forgejo ready first!)
  │     │  ├─ Forgejo must be running (package registry)
  │     │  ├─ PostgreSQL health check
  │     │  └─ All 3 containers (user_code, webserver, daemon)
  │     ├─ cognee (needs all DBs, LLM keys)
  │     ├─ crawl4ai (stateless, just needs LLM keys)
  │     └─ agno (needs PostgreSQL)
  │
  ├─ [PHASE 4] Additional Services (optional)
  │  └─ docker-compose up
  │     ├─ garage (S3 storage)
  │     ├─ pangolin (network config)
  │     └─ komodo (infra agent)
  │
  └─ [VERIFICATION]
     ├─ curl http://localhost:3001 → Dagster
     ├─ curl http://localhost:8000/rest/v1 → Supabase
     ├─ curl http://localhost:4000 → LiteLLM
     ├─ curl http://localhost:3000 → Forgejo/Langfuse
     └─ All health checks passed
        │
        └─ READY FOR USE

COMMON STARTUP ISSUES:
├── Port conflicts → Change in .env
├── Missing secrets → Generate with openssl
├── Network issues → Check docker network
├── PG not ready → Wait for health check
└── API key errors → Verify .env files
```

---

## Storage Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        STORAGE ARCHITECTURE                                   │
└─────────────────────────────────────────────────────────────────────────────┘

RELATIONAL (SQL)
├── Supabase PostgreSQL
│   ├── Auth tables (users, sessions)
│   ├── Storage (file metadata)
│   └── Custom user tables
│
├── Forgejo PostgreSQL
│   ├── Repositories
│   ├── Users & Orgs
│   ├── Issues & PRs
│   └── Packages
│
├── Dagster PostgreSQL
│   ├── Runs
│   ├── Events
│   ├── Assets
│   └── Schedules
│
├── Langfuse PostgreSQL
│   ├── Projects
│   ├── Traces
│   ├── Spans
│   └── Observations
│
├── LiteLLM PostgreSQL
│   ├── API keys
│   ├── Models
│   └── Usage logs
│
└── MLFlow PostgreSQL
    ├── Experiments
    ├── Runs
    └── Metrics/Params

GRAPH (NoSQL)
├── Memgraph
│   ├── Knowledge graphs
│   ├── Entity relationships
│   └── Computed properties
│
└── Cognee (Kuzu embedded)
    └── Entity relationships

VECTOR (Embeddings)
├── Qdrant
│   ├── Text embeddings
│   ├── Document chunks
│   └── Semantic search indexes
│
└── LanceDB (Cognee)
    └── Vector embeddings

KEY-VALUE (Cache)
├── Dragonfly (Redis-compatible)
│   ├── Session cache
│   ├── Embedding cache
│   └── Temporary data
│
└── Memgraph cache layer
    └── Query result cache

OBJECT STORAGE (Blob)
├── MinIO (MLFlow artifacts)
│   ├── Model files
│   ├── Artifacts
│   └── Datasets
│
├── Garage (S3-compatible)
│   ├── General object storage
│   ├── Backups
│   └── Archives
│
└── Supabase Storage
    ├── User uploads
    ├── Media files
    └── Documents

FILE SYSTEM (Local)
├── Docker Volumes
│   ├── pgdata (PostgreSQL volumes)
│   ├── mg_lib (Memgraph)
│   ├── garage-meta/data (Garage)
│   ├── dragonfly-data (Cache)
│   ├── qdrant_data (Vector DB)
│   ├── termix-data (Terminal)
│   └── Various other volumes
│
└── Mounted Config Directories
    ├── ./config (various services)
    ├── ./workspace.yaml (Dagster)
    ├── ./dagster.yaml (Dagster)
    ├── ./litellm_config.yaml (LiteLLM)
    └── ./garage.toml (Garage)
```

---

## Authentication & Authorization Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION ARCHITECTURE                               │
└─────────────────────────────────────────────────────────────────────────────┘

EXTERNAL AUTH SOURCES
├── GitHub (OAuth for Infisical)
├── Google (OAuth for Infisical)
├── GitLab (OAuth for Infisical)
└── Other cloud providers

INTERNAL AUTH SERVICES
│
├── Supabase GoTrue (Primary Auth Server)
│   ├── Email/Password auth
│   ├── OAuth providers
│   ├── MFA support
│   └── JWT token generation
│       ├── ANON_KEY (public)
│       └── SERVICE_ROLE_KEY (server)
│
├── Forgejo Built-in Auth
│   ├── Local users
│   ├── OAuth integration
│   └── API tokens for CI/CD
│
├── Langfuse NextAuth
│   ├── Session management
│   ├── JWT tokens
│   └── Project-level access
│
├── Infisical Custom Auth
│   ├── Local users
│   ├── SSO providers
│   └── API key auth
│
├── LiteLLM API Key Auth
│   ├── Master key verification
│   ├── Per-provider keys
│   └── Salt-based hashing
│
└── Dagster Built-in Auth
    ├── Basic HTTP auth
    ├── GraphQL API access
    └── Run-level permissions

DATA ACCESS PATTERNS
│
├── Public Access
│   ├── Supabase REST (anon key)
│   └── Public GraphQL endpoints
│
├── Authenticated Access
│   ├── User auth required
│   ├── JWT token in headers
│   └── Row-level security (RLS)
│
├── Service-to-Service
│   ├── Internal docker network
│   ├── No external auth
│   └── Database connection strings
│
└── API Token Access
    ├── Fixed API keys
    ├── Per-service tokens
    └── Vault-stored credentials
```

---

## Health & Monitoring Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    MONITORING & HEALTH CHECKS                                │
└─────────────────────────────────────────────────────────────────────────────┘

SERVICE HEALTH CHECKS
├── Database Services
│   ├── PostgreSQL instances
│   │   └── pg_isready command
│   ├── Memgraph
│   │   └── Bolt protocol ping
│   ├── Qdrant
│   │   └── REST /health
│   ├── Dragonfly
│   │   └── Redis PING
│   └── MinIO
│       └── HTTP health endpoint
│
├── Application Services
│   ├── Supabase Kong
│   │   └── /status endpoint
│   ├── Dagster
│   │   ├── /graphql health
│   │   └── WebSocket availability
│   ├── LiteLLM
│   │   └── /health endpoint
│   ├── Langfuse
│   │   └── /api/health
│   ├── Crawl4AI
│   │   └── /health endpoint
│   └── Memgraph Lab
│       └── HTTP port availability
│
└── Infrastructure
    ├── Docker socket availability
    ├── Network connectivity
    ├── Volume mount accessibility
    └── DNS resolution

LOGGING AGGREGATION
├── Docker logs
│   ├── docker-compose logs
│   └── docker logs <container>
│
├── Service-specific logs
│   ├── PostgreSQL log files
│   ├── Dagster event store
│   ├── Langfuse trace database
│   └── Application stdout/stderr
│
└── Monitoring Tools (Optional)
    ├── Prometheus (metrics)
    ├── Grafana (visualization)
    ├── ELK Stack (logs)
    └── Sentry (error tracking)

OBSERVABILITY LAYER
├── Langfuse (LLM-specific)
│   ├── Traces all LLM calls
│   ├── Embeddings metadata
│   └── Cost tracking
│
├── LiteLLM logging
│   ├── Request/response logs
│   ├── Model usage
│   └── Error tracking
│
├── Dagster event logging
│   ├── Run execution events
│   ├── Asset materializations
│   └── Sensor triggers
│
└── Application metrics
    ├── Request latency
    ├── Error rates
    ├── Database connection pools
    └── Cache hit rates
```

---

## Document Map

**For Complete Reference, see:**
1. `/DOCKER_COMPOSE_ANALYSIS.md` - Detailed service breakdown
2. `/DOCKER_COMPOSE_MATRIX.md` - Quick reference matrix & deployment guide
3. `/DOCKER_COMPOSE_ARCHITECTURE.md` - This file (visual diagrams)

