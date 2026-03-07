# Platform Engineering & Infrastructure

This directory consolidates research on deployment infrastructure, MLOps practices, and platform engineering patterns for AI-native applications.

## Overview

The research covers the complete platform stack:
- **Container Orchestration**: Docker Compose, Kubernetes patterns
- **MLOps Infrastructure**: Model serving, experiment tracking
- **Gateway Services**: LiteLLM, API routing, quota management
- **Storage Systems**: Object storage, vector databases, caching
- **Observability**: Monitoring, logging, tracing

## Documents in this Category

| Document | Focus | Key Technologies |
|----------|-------|------------------|
| `docker-compose-patterns.md` | Multi-service orchestration | Docker Compose, networking |
| `mlops-infrastructure.md` | Model deployment and tracking | MLflow, Modal, Nebius |
| `api-gateway-patterns.md` | LLM routing and management | LiteLLM, llama-swap |
| `storage-architecture.md` | Multi-modal data persistence | MinIO, LanceDB, DuckDB |

## Key Architectural Decisions

### 1. Service Topology

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

### 2. LiteLLM Gateway Configuration

```yaml
# config.yaml
model_list:
  # Local MLX models
  - model_name: qwen-vl
    litellm_params:
      model: openai/qwen2.5-vl-32b-instruct
      api_base: "http://localhost:8082/v1"
      api_key: "sk-local-mlx"

  # Local llama.cpp models
  - model_name: olmocr
    litellm_params:
      model: openai/olmocr
      api_base: "http://localhost:8081/v1"
      api_key: "sk-local-llama"

  # Cloud fallback
  - model_name: gpt-4o
    litellm_params:
      model: openai/gpt-4o
      api_key: os.environ/OPENAI_API_KEY

# Routing rules
router_settings:
  routing_strategy: "latency-based-routing"
  fallbacks:
    - qwen-vl: [gpt-4o]

general_settings:
  master_key: "sk-master-secret"
```

### 3. Llama-Swap Model Router

```yaml
# llama-swap config for memory management
listen: :8080
models:
  - name: qwen-vl
    cmd: "llama-server -m /models/Qwen2.5-VL-7B-Q4_K_M.gguf --port 8081 --n-gpu-layers 99"
    ttl: 300  # Unload after 5 minutes idle

  - name: olmocr
    cmd: "llama-server -m /models/olmOCR-Q4_K_M.gguf --clip_model_path /models/mmproj.gguf --port 8081 --n-gpu-layers 99"
    ttl: 300

# Memory management: Only one model loaded at a time
# Automatic swap based on incoming requests
```

## Quick Reference

### Docker Compose Base Stack

```yaml
version: "3.8"

services:
  # Object Storage
  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9090"
    ports: ["9000:9000", "9090:9090"]
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data

  # Metadata Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
      POSTGRES_DB: postgres
    volumes:
      - pg_data:/var/lib/postgresql/data

  # Vector Database
  lancedb:
    image: lancedb/lancedb:latest
    volumes:
      - lance_data:/data

  # Graph Database
  falkordb:
    image: falkordb/falkordb:latest
    ports: ["6379:6379"]
    volumes:
      - falkor_data:/data

  # API Gateway
  litellm:
    image: ghcr.io/berriai/litellm:main-latest
    command: --config /app/config.yaml
    ports: ["4000:4000"]
    volumes:
      - ./litellm-config.yaml:/app/config.yaml
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}

volumes:
  minio_data:
  pg_data:
  lance_data:
  falkor_data:
```

### MLflow Tracking Setup

```python
import mlflow

# Configure tracking
mlflow.set_tracking_uri("http://localhost:5000")
mlflow.set_experiment("document-extraction")

with mlflow.start_run():
    # Log parameters
    mlflow.log_param("model", "qwen2.5-vl-7b")
    mlflow.log_param("quantization", "4-bit")

    # Log metrics
    mlflow.log_metric("accuracy", 0.95)
    mlflow.log_metric("latency_ms", 250)

    # Log artifacts
    mlflow.log_artifact("output.json")
```

### Dagster Resource Configuration

```python
from dagster import resource, Definitions

@resource
def minio_resource(context):
    import boto3
    return boto3.client(
        's3',
        endpoint_url='http://minio:9000',
        aws_access_key_id='minioadmin',
        aws_secret_access_key='minioadmin'
    )

@resource
def postgres_resource(context):
    import psycopg2
    return psycopg2.connect(
        host='postgres',
        database='postgres',
        user='postgres',
        password='password'
    )

defs = Definitions(
    assets=[...],
    resources={
        "minio": minio_resource,
        "postgres": postgres_resource
    }
)
```

## Source Files Consolidated

This category merges content from:
- Infrastructure sections from various documents
- `Integrating Olake, Lakekeeper, RisingWave.md` (Docker patterns)
- Apple Silicon deployment configurations
- LiteLLM and llama-swap configurations

## Environment Matrix

### Development (Local Mac)

| Component | Implementation | Memory |
|-----------|---------------|--------|
| LLM Serving | MLX / llama.cpp | 16-32GB |
| Vector DB | LanceDB (embedded) | 2GB |
| Graph DB | FalkorDB (Docker) | 1GB |
| Object Storage | Local filesystem | N/A |
| Orchestration | Dagster (local) | 2GB |

### Staging (Single Server)

| Component | Implementation | Specs |
|-----------|---------------|-------|
| LLM Serving | vLLM + GPU | 24GB VRAM |
| Vector DB | LanceDB (standalone) | 8GB |
| Graph DB | FalkorDB (cluster) | 8GB |
| Object Storage | MinIO | 100GB |
| Orchestration | Dagster (Docker) | 4GB |

### Production (Kubernetes)

| Component | Implementation | Replicas |
|-----------|---------------|----------|
| LLM Serving | vLLM + autoscale | 2-10 |
| Vector DB | LanceDB Cloud | Managed |
| Graph DB | Neo4j Aura | Managed |
| Object Storage | S3 / R2 | Managed |
| Orchestration | Dagster Cloud | Managed |

## Implementation Priorities

### Phase 1: Local Development
1. Docker Compose base stack
2. LiteLLM gateway configuration
3. Local model serving (MLX/llama.cpp)

### Phase 2: CI/CD Pipeline
1. GitHub Actions for testing
2. Docker image builds
3. Automated deployments

### Phase 3: Staging Environment
1. GPU server provisioning
2. vLLM deployment
3. Monitoring setup

### Phase 4: Production
1. Kubernetes manifests
2. Autoscaling configuration
3. Disaster recovery
