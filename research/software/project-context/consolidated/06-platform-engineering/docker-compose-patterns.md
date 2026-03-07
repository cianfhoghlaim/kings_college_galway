# Docker Compose Patterns for AI Infrastructure

## Executive Summary

This document details Docker Compose patterns for deploying AI-native infrastructure stacks, including service topology, networking, volume management, and health monitoring. The patterns support development, staging, and production environments with appropriate scaling strategies.

---

## 1. Service Topology

### 1.1 Reference Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT APPLICATIONS                       │
│              (Cursor, Open WebUI, Custom Apps)              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   LITELLM GATEWAY                           │
│              Unified OpenAI-Compatible Interface            │
│                      (Port 4000)                            │
└─────────────────────────────────────────────────────────────┘
        ↓                    ↓                    ↓
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   MLX/GGUF   │   │   CLOUD API  │   │    vLLM      │
│   (Local)    │   │   (OpenAI)   │   │   (GPU)      │
│  Port 8081   │   │              │   │  Port 8082   │
└──────────────┘   └──────────────┘   └──────────────┘
        ↓                    ↓                    ↓
┌─────────────────────────────────────────────────────────────┐
│                   STORAGE LAYER                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  MinIO   │  │ Postgres │  │ LanceDB  │  │ FalkorDB │   │
│  │   (S3)   │  │  (Meta)  │  │ (Vector) │  │ (Graph)  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Service Categories

| Category | Services | Purpose |
|----------|----------|---------|
| **Gateway** | LiteLLM, Traefik, Pangolin | API routing, auth |
| **Compute** | MLX Server, vLLM, Ollama | Model inference |
| **Storage** | MinIO, Postgres, DuckDB | Data persistence |
| **Vector** | LanceDB, Qdrant | Embedding storage |
| **Graph** | FalkorDB, Neo4j | Knowledge graphs |
| **Orchestration** | Dagster, Dagster Daemon | Pipeline management |
| **Observability** | Grafana, Prometheus | Monitoring |

---

## 2. Complete Docker Compose Stack

### 2.1 Core Infrastructure

```yaml
version: "3.8"

services:
  # ==========================================================================
  # OBJECT STORAGE
  # ==========================================================================
  minio:
    image: minio/minio:latest
    container_name: minio
    command: server /data --console-address ":9090"
    ports:
      - "9000:9000"   # S3 API
      - "9090:9090"   # Console
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD:-minioadmin}
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - ai-stack

  # ==========================================================================
  # METADATA DATABASE
  # ==========================================================================
  postgres:
    image: postgres:15-alpine
    container_name: postgres
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-password}
      POSTGRES_DB: ${POSTGRES_DB:-postgres}
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ai-stack

  # ==========================================================================
  # VECTOR DATABASE
  # ==========================================================================
  lancedb:
    image: lancedb/lancedb:latest
    container_name: lancedb
    ports:
      - "8082:8080"
    volumes:
      - lance_data:/data
    networks:
      - ai-stack

  # ==========================================================================
  # GRAPH DATABASE
  # ==========================================================================
  falkordb:
    image: falkordb/falkordb:latest
    container_name: falkordb
    ports:
      - "6379:6379"
    volumes:
      - falkor_data:/data
    command: --save 60 1 --appendonly yes
    networks:
      - ai-stack

  # ==========================================================================
  # API GATEWAY
  # ==========================================================================
  litellm:
    image: ghcr.io/berriai/litellm:main-latest
    container_name: litellm
    command: --config /app/config.yaml
    ports:
      - "4000:4000"
    volumes:
      - ./litellm-config.yaml:/app/config.yaml:ro
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - LITELLM_MASTER_KEY=${LITELLM_MASTER_KEY:-sk-master}
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - ai-stack

  # ==========================================================================
  # ORCHESTRATION
  # ==========================================================================
  dagster-webserver:
    build:
      context: .
      dockerfile: Dockerfile.dagster
    container_name: dagster-webserver
    command: dagster-webserver -h 0.0.0.0 -p 3000
    ports:
      - "3000:3000"
    environment:
      - DAGSTER_HOME=/opt/dagster/dagster_home
      - POSTGRES_HOST=postgres
    volumes:
      - ./dagster_home:/opt/dagster/dagster_home
      - ./pipelines:/opt/dagster/app
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - ai-stack

  dagster-daemon:
    build:
      context: .
      dockerfile: Dockerfile.dagster
    container_name: dagster-daemon
    command: dagster-daemon run
    environment:
      - DAGSTER_HOME=/opt/dagster/dagster_home
    volumes:
      - ./dagster_home:/opt/dagster/dagster_home
      - ./pipelines:/opt/dagster/app
    depends_on:
      - dagster-webserver
    networks:
      - ai-stack

# ==========================================================================
# NETWORKS
# ==========================================================================
networks:
  ai-stack:
    driver: bridge

# ==========================================================================
# VOLUMES
# ==========================================================================
volumes:
  minio_data:
  pg_data:
  lance_data:
  falkor_data:
```

### 2.2 LLM Serving Addition (GPU)

```yaml
  # Add to services section for GPU environments
  vllm:
    image: vllm/vllm-openai:latest
    container_name: vllm
    ports:
      - "8080:8000"
    volumes:
      - ./models:/models
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    command: >
      --model /models/Qwen2.5-7B-Instruct
      --host 0.0.0.0
      --port 8000
      --max-model-len 8192
    networks:
      - ai-stack
```

---

## 3. LiteLLM Gateway Configuration

### 3.1 Multi-Model Configuration

```yaml
# litellm-config.yaml
model_list:
  # Local MLX models (Apple Silicon)
  - model_name: qwen-vl
    litellm_params:
      model: openai/qwen2.5-vl-32b-instruct
      api_base: "http://host.docker.internal:8081/v1"
      api_key: "sk-local-mlx"

  # Local llama.cpp models
  - model_name: olmocr
    litellm_params:
      model: openai/olmocr
      api_base: "http://llama-server:8081/v1"
      api_key: "sk-local-llama"

  # vLLM served model
  - model_name: qwen-instruct
    litellm_params:
      model: openai/Qwen2.5-7B-Instruct
      api_base: "http://vllm:8000/v1"
      api_key: "sk-vllm"

  # Cloud fallbacks
  - model_name: gpt-4o
    litellm_params:
      model: openai/gpt-4o
      api_key: os.environ/OPENAI_API_KEY

  - model_name: claude-sonnet
    litellm_params:
      model: anthropic/claude-sonnet-4-20250514
      api_key: os.environ/ANTHROPIC_API_KEY

# Routing configuration
router_settings:
  routing_strategy: "latency-based-routing"
  fallbacks:
    - qwen-vl: [gpt-4o]
    - olmocr: [gpt-4o]
    - qwen-instruct: [claude-sonnet, gpt-4o]

# General settings
general_settings:
  master_key: os.environ/LITELLM_MASTER_KEY
  database_url: "postgresql://postgres:password@postgres:5432/litellm"

# Logging
litellm_settings:
  success_callback: ["langfuse"]
  failure_callback: ["langfuse"]
```

### 3.2 Rate Limiting

```yaml
# Add to litellm-config.yaml
router_settings:
  # Per-model rate limits
  model_rpm_limits:
    gpt-4o: 60
    claude-sonnet: 50
    qwen-instruct: 1000  # Local, no external limits

  # User-based quotas
  user_rpm_limits:
    default: 100
    premium: 500
```

---

## 4. llama-swap Model Router

For memory-constrained environments, llama-swap provides automatic model swapping.

### 4.1 Configuration

```yaml
# llama-swap-config.yaml
listen: :8080

models:
  - name: qwen-vl
    cmd: >
      llama-server
      -m /models/Qwen2.5-VL-7B-Q4_K_M.gguf
      --port 8081
      --n-gpu-layers 99
      --ctx-size 8192
    ttl: 300  # Unload after 5 minutes idle

  - name: olmocr
    cmd: >
      llama-server
      -m /models/olmOCR-Q4_K_M.gguf
      --clip_model_path /models/mmproj.gguf
      --port 8081
      --n-gpu-layers 99
    ttl: 300

  - name: mistral-code
    cmd: >
      llama-server
      -m /models/Mistral-7B-Instruct-v0.3.Q4_K_M.gguf
      --port 8081
      --n-gpu-layers 99
    ttl: 600  # Keep longer for coding sessions

# Memory management: Only one model loaded at a time
# Automatic swap based on incoming requests
```

### 4.2 Docker Integration

```yaml
  llama-swap:
    image: ghcr.io/mostlygeek/llama-swap:latest
    container_name: llama-swap
    ports:
      - "8080:8080"
    volumes:
      - ./llama-swap-config.yaml:/app/config.yaml:ro
      - ./models:/models
    command: -config /app/config.yaml
    networks:
      - ai-stack
```

---

## 5. Health Monitoring Patterns

### 5.1 Service Health Checks

```yaml
# Comprehensive health check configuration
services:
  critical-service:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

### 5.2 Dependency Chain

```yaml
services:
  app:
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      minio:
        condition: service_started
```

### 5.3 Docker Compose Profiles

```yaml
# Use profiles for environment-specific services
services:
  # Always started
  postgres:
    profiles: ["dev", "staging", "prod"]

  # GPU services only in staging/prod
  vllm:
    profiles: ["staging", "prod"]

  # Debug tools only in dev
  pgadmin:
    profiles: ["dev"]

# Start specific profile:
# docker compose --profile dev up
```

---

## 6. Volume and Data Management

### 6.1 Volume Naming Conventions

```yaml
volumes:
  # Database volumes
  pg_data:
    name: ${PROJECT_NAME:-ai}_postgres_data

  # Object storage
  minio_data:
    name: ${PROJECT_NAME:-ai}_minio_data

  # Model cache
  model_cache:
    name: ${PROJECT_NAME:-ai}_model_cache

  # Shared temp space
  shared_tmp:
    driver: local
    driver_opts:
      type: tmpfs
      device: tmpfs
      o: size=2g
```

### 6.2 Backup Strategy

```bash
#!/bin/bash
# backup.sh - Volume backup script

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/${DATE}"

mkdir -p ${BACKUP_DIR}

# Postgres backup
docker compose exec -T postgres pg_dump -U postgres postgres > ${BACKUP_DIR}/postgres.sql

# MinIO backup (using mc client)
docker run --rm --network ai-stack \
  -v ${BACKUP_DIR}:/backup \
  minio/mc mirror ai-minio/data /backup/minio

# Compress
tar -czf ${BACKUP_DIR}.tar.gz ${BACKUP_DIR}
rm -rf ${BACKUP_DIR}

echo "Backup complete: ${BACKUP_DIR}.tar.gz"
```

---

## 7. Environment-Specific Overrides

### 7.1 Development Override

```yaml
# docker-compose.dev.yaml
services:
  postgres:
    ports:
      - "5432:5432"  # Expose for local tools

  minio:
    environment:
      MINIO_ROOT_USER: dev
      MINIO_ROOT_PASSWORD: devpassword

  litellm:
    environment:
      - LITELLM_LOG_LEVEL=DEBUG

  # Add development tools
  pgadmin:
    image: dpage/pgadmin4
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@local.dev
      PGADMIN_DEFAULT_PASSWORD: admin
```

### 7.2 Production Override

```yaml
# docker-compose.prod.yaml
services:
  postgres:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G

  minio:
    deploy:
      replicas: 1
      resources:
        limits:
          memory: 2G

  litellm:
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1'
          memory: 1G
```

### 7.3 Usage

```bash
# Development
docker compose -f docker-compose.yaml -f docker-compose.dev.yaml up

# Production
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml up -d
```

---

## 8. Networking Patterns

### 8.1 Multi-Network Isolation

```yaml
networks:
  # Public-facing services
  frontend:
    driver: bridge

  # Internal services only
  backend:
    driver: bridge
    internal: true

  # Database network (most restricted)
  database:
    driver: bridge
    internal: true

services:
  traefik:
    networks:
      - frontend

  litellm:
    networks:
      - frontend
      - backend

  dagster:
    networks:
      - backend
      - database

  postgres:
    networks:
      - database
```

### 8.2 Host Network Access (Mac)

```yaml
  # For services needing host network on Mac
  local-llm-client:
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      - LLM_API_BASE=http://host.docker.internal:8081/v1
```

---

## 9. Implementation Priorities

### Phase 1: Core Services
1. Deploy MinIO, Postgres, FalkorDB
2. Configure volume persistence
3. Set up health checks

### Phase 2: Gateway Layer
1. Deploy LiteLLM with basic config
2. Configure local model routing
3. Add cloud fallbacks

### Phase 3: Orchestration
1. Deploy Dagster webserver and daemon
2. Configure database connection
3. Set up pipeline volumes

### Phase 4: Production Hardening
1. Add resource limits
2. Configure backup strategy
3. Set up monitoring integration

---

## References

- Docker Compose Specification: https://docs.docker.com/compose/compose-file/
- LiteLLM Configuration: https://docs.litellm.ai/docs/
- MinIO Docker: https://min.io/docs/minio/container/index.html
- Dagster Docker: https://docs.dagster.io/deployment/guides/docker
