# Docker Compose Analysis - Complete Documentation Index

## Overview

This directory contains comprehensive documentation for all 17 Docker Compose stacks in the `/infrastructure/compose/` directory, covering 18 containerized services organized across multiple functional domains.

**Total Documentation**: 1,985 lines across 3 documents

---

## Document Guide

### 1. DOCKER_COMPOSE_ANALYSIS.md (Detailed Reference)
**Size**: 891 lines | **Focus**: Complete service breakdown

The most comprehensive document covering:
- Each service with detailed specifications
- Environment variables required/optional
- API endpoints and SDK availability
- Build dependencies and custom images
- Inter-service dependencies

**Use this when you need:**
- Full details about a specific service
- Complete environment variable reference
- API endpoint documentation
- Build instructions
- Network integration details

**Key Sections:**
- Infrastructure Foundation (Supabase, Forgejo, Garage, Dragonfly, Qdrant, Memgraph)
- Data Processing & ETL (Cognee, Crawl4AI)
- Package & Artifact Management (Forgejo, Garage)
- LLM Operations & Orchestration (LiteLLM, MLFlow, Langfuse)
- Workflow & Orchestration (Dagster, Pangolin, Komodo)
- Development & Agentic Frameworks (Agno, Termix)
- Secrets & Configuration Management (Infisical)
- Service Dependency Matrix
- Environment Variable Management Matrix
- Network Architecture
- Build & Deployment Requirements
- Observability & Integration Chain
- Service Management Matrix
- Deployment Checklist
- Critical Integration Points
- Known Limitations & Notes

---

### 2. DOCKER_COMPOSE_MATRIX.md (Quick Reference & Deployment)
**Size**: 409 lines | **Focus**: Quick lookup and deployment guide

Fast-reference guide with:
- Service matrix (17 services in table format)
- Environment setup by category (MVP vs. Advanced)
- Secrets & keys required
- Port assignments by range
- Environment file checklist
- Deployment order (5 phases)
- Health check commands
- Common issues & solutions
- Integration summary
- Scaling considerations

**Use this when you need:**
- Quick lookup of service requirements
- Port numbers for testing
- Deployment order
- Health check commands
- Step-by-step setup instructions

**Key Sections:**
- Service Matrix (Env vars, Build, Secrets, Dependencies, Status)
- Environment Setup by Category
- Critical Path vs. Optional Services
- Secrets & Keys Required
- Port Assignments Reference
- Environment File Checklist (4-step setup)
- Deployment Order (5 phases)
- Health Check Commands
- Common Issues & Solutions
- Environment Variable Templates
- Integration Summary
- Scaling Considerations

---

### 3. DOCKER_COMPOSE_ARCHITECTURE.md (Visual Diagrams & Design)
**Size**: 685 lines | **Focus**: Architecture visualization

Comprehensive architecture documentation with ASCII diagrams:
- System architecture overview
- Data flow pipeline
- Service dependency graph
- Network topology
- Port map by service
- Configuration dependencies
- Deployment flow
- Storage architecture
- Authentication & authorization
- Health & monitoring strategy

**Use this when you need:**
- Visual understanding of system architecture
- Data flow between services
- Network topology overview
- Deployment sequence
- Storage architecture details
- Authentication flow
- Monitoring strategy

**Key Sections:**
- System Architecture Diagram (4-layer)
- Data Flow Architecture
- Service Dependency Graph (Tier 0-3)
- Network Topology
- Port Map by Service
- Configuration Dependencies
- Deployment Flow (5-phase)
- Storage Architecture
- Authentication & Authorization Flow
- Health & Monitoring Strategy

---

## Service Quick Reference

| Service | Type | Critical | Status | Env Vars | Build | Port |
|---------|------|----------|--------|----------|-------|------|
| Supabase | Core DB | Yes | Ready | 20+ | No | 8000 |
| Forgejo | Registry | Yes | Ready | 15+ | No | 3000 |
| Dagster | Orchestration | Yes | Ready | 12+ | 3x | 3001 |
| LiteLLM | LLM Proxy | High | Ready | 20+ | No | 4000 |
| Cognee | Data Engine | High | Ready | 40+ | No | - |
| Langfuse | Observability | High | Ready | 8+ | No | 3000 |
| Crawl4AI | Web Scraper | Medium | Ready | 10+ | Opt | 11235 |
| MLFlow | Model Tracking | Medium | Ready | 12+ | No | 5000 |
| Agno | Agent Framework | Medium | Ready | 8+ | Yes | 8000 |
| Garage | S3 Storage | Medium | Ready | 8+ | No | 3900 |
| Memgraph | Graph DB | Medium | Ready | 2 | No | 7687 |
| Qdrant | Vector DB | Low | Ready | 0 | No | 6333 |
| Dragonfly | Cache | Low | Ready | 2 | No | 6379 |
| Pangolin | Network Mgmt | Low | Example | 0 | No | 3001 |
| Komodo | Infra Agent | Low | Example | 4+ | No | - |
| Termix | Terminal | Utility | Ready | 1 | No | 8080 |
| Infisical | Secrets Mgmt | Low | Incomplete | 30+ | No | 8080 |

---

## Critical Path (MVP Deployment)

To get a working system, deploy in this order:

1. **Supabase** - Core database, auth, APIs (8000)
2. **Forgejo** - Git + PyPI registry (3000)
3. **Dagster** - Pipeline orchestration (3001)
4. **LiteLLM** - LLM proxy (4000)
5. **Langfuse** - Observability (3000)

Supporting services (deploy simultaneously):
- Dragonfly (6379) - Cache
- Qdrant (6333) - Vector search
- Memgraph (7687) - Graph DB

---

## Getting Started Checklist

### Preparation
- [ ] Review DOCKER_COMPOSE_MATRIX.md for overview
- [ ] Read DOCKER_COMPOSE_ANALYSIS.md for service details
- [ ] Understand DOCKER_COMPOSE_ARCHITECTURE.md topology

### Setup
- [ ] Copy all `.env.example` and `.env.template` files
- [ ] Generate security keys (openssl rand)
- [ ] Obtain LLM API keys (OpenAI, Anthropic, etc.)
- [ ] Get GitHub token for Dagster
- [ ] Configure Komodo vault references

### Deployment
- [ ] Phase 1: Infrastructure (Dragonfly, Qdrant, Memgraph)
- [ ] Phase 2: Core Services (Forgejo, Supabase, databases)
- [ ] Phase 3: Orchestration (Dagster, LiteLLM, Langfuse)
- [ ] Phase 4: Data Processing (Cognee, Crawl4AI)
- [ ] Phase 5: Advanced (Garage, Pangolin, Komodo)

### Verification
- [ ] Run health check commands (see MATRIX.md)
- [ ] Test key integrations
- [ ] Verify API endpoints accessible
- [ ] Monitor logs for errors

---

## Port Ranges

| Range | Purpose | Examples |
|-------|---------|----------|
| 3000-3001 | Web UIs | Forgejo, Dagster, Supabase, Langfuse |
| 4000-4999 | APIs | LiteLLM |
| 5000-5999 | Databases | MLFlow, PostgreSQL |
| 6300-6379 | Search/Cache | Qdrant, Dragonfly |
| 7687-7444 | Graphs | Memgraph |
| 8000-8999 | Gateways | Supabase Kong, Infisical, Termix |
| 9000-9001 | Object Storage | MinIO, Garage |
| 11235+ | Special | Crawl4AI |

---

## Key Integrations

### Data Processing Pipeline
```
Crawl4AI (scrape) → Cognee (process) → Supabase (store)
                         ↓
                    Qdrant (search)
                         ↓
                    Langfuse (observe)
```

### Orchestration Flow
```
Dagster (scheduler) → Forgejo (packages) → GitHub (data)
         ↓
      Supabase (warehouse)
```

### LLM Services Chain
```
LiteLLM (proxy) → Multiple providers (OpenAI, Anthropic, etc.)
     ↓
  Langfuse (tracing)
     ↓
  MLFlow (tracking)
```

---

## Environment Variables Summary

### Required (must have)
- OPENAI_API_KEY or other LLM API key
- GITHUB_TOKEN (for Dagster)
- Database passwords (generate with openssl)

### Generated (security-critical)
- FORGEJO_SECRET_KEY
- NEXTAUTH_SECRET, SALT, ENCRYPTION_KEY (Langfuse)
- LITELLM_MASTER_KEY, LITELLM_SALT_KEY
- RPC_SECRET, ADMIN_API_TOKEN (Garage)

### Vault-Managed (Komodo)
- Supabase credentials
- Memgraph credentials
- Multiple LLM API keys

---

## Service Dependencies Summary

**Tier 0 (No dependencies)**
- Dragonfly, Qdrant, Termix, Pangolin, Komodo

**Tier 1 (Database only)**
- Forgejo, Supabase, Memgraph, LiteLLM, Langfuse, MLFlow

**Tier 2 (Multiple services)**
- Cognee (PG + Memgraph + Dragonfly + LanceDB + LLM)
- Dagster (PG + Forgejo + GitHub + Docker)
- Crawl4AI (LLM keys)
- Agno (PostgreSQL)

---

## File Structure Reference

```
/infrastructure/compose/
├── agno/              - Agent framework
│   └── compose.yaml
├── cognee/            - Knowledge graph
│   ├── compose.yaml
│   ├── .env
│   └── .env.template
├── crawl4ai/          - Web scraper
│   ├── compose.yaml
│   └── .llm.env
├── dagster/           - Orchestration
│   ├── docker-compose.yaml
│   ├── Dockerfile*
│   ├── workspace.yaml
│   └── .env.example
├── dragonfly/         - Cache
│   └── compose.yaml
├── forgejo/           - Git + PyPI
│   ├── compose.yaml
│   └── .env.example
├── garage/            - S3 Storage
│   ├── docker-compose.yaml
│   ├── garage.toml
│   └── .env
├── infisical/         - Secrets
│   ├── .env.template (no compose.yaml)
├── komodo/            - Infra agent
│   └── periphery/compose.yaml
├── langfuse/          - Observability
│   ├── compose.yaml
│   └── .env
├── litellm/           - LLM Proxy
│   ├── compose.yaml
│   ├── litellm_config.yaml
│   └── .env.template
├── memgraph/          - Graph DB
│   ├── compose.yaml
│   └── .env.template
├── mlflow/            - Model tracking
│   └── compose.yaml
├── pangolin/          - Network mgmt
│   └── core/compose.yaml
├── qdrant/            - Vector search
│   └── compose.yaml
├── supabase/          - Core DB
│   ├── docker-compose.yml
│   └── .env
└── termix/            - Terminal
    └── compose.yaml
```

---

## Useful Commands

### List all services
```bash
cd /infrastructure/compose
ls -d */ | sort
```

### Check service status
```bash
docker-compose ps
docker-compose logs <service>
```

### Health checks
```bash
# See DOCKER_COMPOSE_MATRIX.md for full list
curl http://localhost:8000/rest/v1  # Supabase
curl http://localhost:3001          # Dagster
curl http://localhost:4000/health   # LiteLLM
```

### Generate secrets
```bash
openssl rand -hex 32    # For hex keys
openssl rand -base64 32 # For base64 keys
```

---

## For More Information

- **Detailed Service Specs**: See DOCKER_COMPOSE_ANALYSIS.md
- **Deployment Instructions**: See DOCKER_COMPOSE_MATRIX.md
- **Architecture Details**: See DOCKER_COMPOSE_ARCHITECTURE.md
- **Project Documentation**: See /CLAUDE.md (project instructions)
- **OpenSpec Guide**: See /openspec/AGENTS.md (for change proposals)

---

## Document Version

Created: November 28, 2025
Analyzed Services: 17
Total Compose Files: 15
Total Environment: 1,985 lines of documentation

