# LiteLLM Proxy - Deployment & Operations Guide

## Table of Contents
1. [Local Development](#local-development)
2. [Docker Deployment](#docker-deployment)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Cloud Platform Deployments](#cloud-platform-deployments)
5. [Database Migration](#database-migration)
6. [Monitoring & Logging](#monitoring--logging)
7. [Troubleshooting](#troubleshooting)
8. [Performance Tuning](#performance-tuning)

---

## Local Development

### Quick Start

```bash
# Install dependencies
pip install 'litellm[proxy]'

# Create minimal config
cat > config.yaml << 'EOFCONFIG'
model_list:
  - model_name: gpt-3.5-turbo
    litellm_params:
      model: gpt-3.5-turbo
      api_key: ${OPENAI_API_KEY}

general_settings:
  master_key: sk-local-1234567890
EOFCONFIG

# Set environment variables
export OPENAI_API_KEY=sk-...
export LITELLM_LOG=DEBUG

# Start proxy
litellm --config config.yaml --detailed_debug

# In another terminal, test the proxy
curl http://localhost:4000/v1/models \
  -H "Authorization: Bearer sk-local-1234567890"
```

### Development with PostgreSQL

```bash
# Install PostgreSQL (macOS)
brew install postgresql
brew services start postgresql

# Create database
createdb litellm_dev

# Set connection string
export DATABASE_URL=postgresql://localhost/litellm_dev
export LITELLM_MASTER_KEY=sk-dev-1234567890
export OPENAI_API_KEY=sk-...

# Start proxy with database
litellm --config config.yaml --detailed_debug
```

### Development Docker Compose

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: litellm_dev
      POSTGRES_HOST_AUTH_METHOD: trust
    ports:
      - "5432:5432"

  litellm:
    image: ghcr.io/berriai/litellm-database:main-latest
    depends_on:
      - postgres
    environment:
      DATABASE_URL: postgresql://postgres@postgres:5432/litellm_dev
      LITELLM_MASTER_KEY: sk-dev-1234567890
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      LITELLM_LOG: DEBUG
    ports:
      - "4000:4000"
    volumes:
      - ./config.yaml:/app/config.yaml
    command: litellm --config /app/config.yaml --detailed_debug
```

---

## Docker Deployment

### Single Container

```bash
# Pull image
docker pull ghcr.io/berriai/litellm-database:main-stable

# Create environment file
cat > .env << 'EOFENV'
LITELLM_MASTER_KEY=sk-prod-1234567890
LITELLM_SALT_KEY=sk-salt-1234567890
DATABASE_URL=postgresql://litellm:password@postgres:5432/litellm
OPENAI_API_KEY=sk-...
AZURE_API_KEY=...
AZURE_API_BASE=...
LANGFUSE_PUBLIC_KEY=...
LANGFUSE_SECRET_KEY=...
LITELLM_LOG=INFO
EOFENV

# Run container
docker run -d \
  --env-file .env \
  -v $(pwd)/config.yaml:/app/config.yaml \
  -p 4000:4000 \
  --name litellm \
  --restart unless-stopped \
  ghcr.io/berriai/litellm-database:main-stable \
  --config /app/config.yaml \
  --num_workers 4

# Check logs
docker logs -f litellm

# Stop container
docker stop litellm
docker rm litellm
```

### Docker Compose with Complete Stack

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-litellm}
      POSTGRES_USER: ${DB_USER:-litellm}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-litellm_password}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-litellm}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD:-redis_password}
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-redis_password}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  litellm:
    image: ghcr.io/berriai/litellm-database:main-stable
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      LITELLM_MASTER_KEY: ${LITELLM_MASTER_KEY}
      LITELLM_SALT_KEY: ${LITELLM_SALT_KEY}
      DATABASE_URL: postgresql://${DB_USER:-litellm}:${DB_PASSWORD:-litellm_password}@postgres:5432/${DB_NAME:-litellm}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      AZURE_API_KEY: ${AZURE_API_KEY}
      AZURE_API_BASE: ${AZURE_API_BASE}
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_password}
      LANGFUSE_PUBLIC_KEY: ${LANGFUSE_PUBLIC_KEY}
      LANGFUSE_SECRET_KEY: ${LANGFUSE_SECRET_KEY}
      HELICONE_API_KEY: ${HELICONE_API_KEY}
      LITELLM_LOG: ${LITELLM_LOG:-INFO}
    ports:
      - "4000:4000"
    volumes:
      - ./config.yaml:/app/config.yaml
      - ./litellm_logs:/app/logs
    command: >
      litellm --config /app/config.yaml
      --host 0.0.0.0
      --port 4000
      --num_workers 4
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4000/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  postgres_data:
```

### Run Docker Compose

```bash
# Create .env file
cat > .env << 'EOFENV'
LITELLM_MASTER_KEY=sk-1234567890
LITELLM_SALT_KEY=sk-salt-1234567890
DB_NAME=litellm
DB_USER=litellm
DB_PASSWORD=litellm_password
REDIS_PASSWORD=redis_password
OPENAI_API_KEY=sk-...
AZURE_API_KEY=...
AZURE_API_BASE=...
LANGFUSE_PUBLIC_KEY=...
LANGFUSE_SECRET_KEY=...
LITELLM_LOG=INFO
EOFENV

# Start services
docker-compose up -d

# View status
docker-compose ps

# View logs
docker-compose logs -f litellm

# Access proxy
curl -H "Authorization: Bearer sk-1234567890" http://localhost:4000/v1/models

# Stop services
docker-compose down

# Remove volumes (careful!)
docker-compose down -v
```

---

## Kubernetes Deployment

### Helm Installation

```bash
# Add Helm repository
helm repo add berriai https://berriai.github.io/litellm-helm
helm repo update

# Create values file
cat > values.yaml << 'EOFHELM'
replicaCount: 3

image:
  repository: ghcr.io/berriai/litellm-database
  tag: main-stable

service:
  type: LoadBalancer
  port: 4000

resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"

env:
  LITELLM_MASTER_KEY:
    secretKeyRef:
      name: litellm-secrets
      key: master-key
  DATABASE_URL:
    secretKeyRef:
      name: litellm-secrets
      key: database-url
  REDIS_HOST: redis.default.svc.cluster.local
  REDIS_PASSWORD:
    secretKeyRef:
      name: litellm-secrets
      key: redis-password
  OPENAI_API_KEY:
    secretKeyRef:
      name: llm-keys
      key: openai

configMap:
  config.yaml: |
    model_list:
      - model_name: gpt-4o
        litellm_params:
          model: gpt-4o
          api_key: ${OPENAI_API_KEY}

healthCheck:
  enabled: true
  path: /health
EOFHELM

# Install release
helm install litellm berriai/litellm-proxy -f values.yaml

# Check deployment
kubectl get pods -l app=litellm-proxy
kubectl logs -f deployment/litellm-proxy

# Upgrade
helm upgrade litellm berriai/litellm-proxy -f values.yaml

# Uninstall
helm uninstall litellm
```

### Manual Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace litellm

# Create secrets
kubectl create secret generic litellm-secrets \
  --from-literal=master-key=sk-1234567890 \
  --from-literal=database-url=postgresql://... \
  --from-literal=redis-password=... \
  -n litellm

kubectl create secret generic llm-keys \
  --from-literal=openai=sk-... \
  --from-literal=anthropic=... \
  -n litellm

# Create ConfigMap for config.yaml
kubectl create configmap litellm-config \
  --from-file=config.yaml \
  -n litellm

# Deploy PostgreSQL
kubectl apply -f postgres-deployment.yaml -n litellm

# Deploy Redis
kubectl apply -f redis-deployment.yaml -n litellm

# Deploy LiteLLM
kubectl apply -f litellm-deployment.yaml -n litellm

# Check status
kubectl get all -n litellm

# View logs
kubectl logs -f deployment/litellm-proxy -n litellm

# Port forward for testing
kubectl port-forward -n litellm svc/litellm-proxy 4000:4000
```

---

## Cloud Platform Deployments

### AWS ECS (Fargate)

```json
{
  "family": "litellm-proxy",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "litellm",
      "image": "ghcr.io/berriai/litellm-database:main-stable",
      "portMappings": [
        {
          "containerPort": 4000,
          "hostPort": 4000,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "LITELLM_LOG",
          "value": "INFO"
        },
        {
          "name": "REDIS_HOST",
          "value": "redis.example.com"
        }
      ],
      "secrets": [
        {
          "name": "LITELLM_MASTER_KEY",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:litellm-master-key"
        },
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:litellm-database-url"
        }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:4000/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/litellm-proxy",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Google Cloud Run

```bash
# Create .env.yaml
cat > .env.yaml << 'EOFENV'
LITELLM_MASTER_KEY: sk-1234567890
LITELLM_SALT_KEY: sk-salt-1234567890
DATABASE_URL: postgresql://...
OPENAI_API_KEY: sk-...
LANGFUSE_PUBLIC_KEY: ...
LANGFUSE_SECRET_KEY: ...
EOFENV

# Deploy to Cloud Run
gcloud run deploy litellm-proxy \
  --image ghcr.io/berriai/litellm-database:main-stable \
  --platform managed \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --timeout 3600 \
  --max-instances 10 \
  --env-vars-file .env.yaml \
  --set-cloudsql-instances project:region:instance
```

### Heroku Deployment

```bash
# Create Procfile
cat > Procfile << 'EOFPROC'
web: litellm --config config.yaml --host 0.0.0.0 --port $PORT
EOFPROC

# Create app
heroku create litellm-proxy

# Set config variables
heroku config:set -a litellm-proxy \
  LITELLM_MASTER_KEY=sk-1234567890 \
  OPENAI_API_KEY=sk-... \
  DATABASE_URL=postgresql://...

# Deploy
git push heroku main

# View logs
heroku logs -f -a litellm-proxy
```

---

## Database Migration

### Initial Setup

```bash
# LiteLLM automatically creates tables on first run
# No manual migration needed!

# Just ensure DATABASE_URL is set:
export DATABASE_URL=postgresql://user:password@host:5432/litellm
litellm --config config.yaml
```

### Backup Database

```bash
# PostgreSQL backup
pg_dump -U litellm -h localhost litellm > backup.sql

# Restore from backup
psql -U litellm -h localhost litellm < backup.sql

# Docker backup
docker exec postgres pg_dump -U litellm litellm > backup.sql
```

### Database Upgrade

```bash
# Update LiteLLM
pip install --upgrade litellm

# Run proxy (migrations happen automatically)
litellm --config config.yaml

# Verify upgrade
psql -U litellm -h localhost litellm -c "\dt"
```

---

## Monitoring & Logging

### Prometheus Metrics

```bash
# Metrics endpoint
curl http://localhost:4000/metrics
```

### Langfuse Integration

```yaml
litellm_settings:
  success_callback: ["langfuse"]
  failure_callback: ["langfuse"]

environment_variables:
  LANGFUSE_PUBLIC_KEY: pk-...
  LANGFUSE_SECRET_KEY: sk-...
```

### Datadog Integration

```yaml
litellm_settings:
  success_callback: ["datadog"]
  failure_callback: ["datadog"]

environment_variables:
  DATADOG_API_KEY: ...
  DATADOG_SITE: datadoghq.com
```

### Log Levels

```bash
# Debug logging
export LITELLM_LOG=DEBUG
litellm --config config.yaml

# Info logging (default)
export LITELLM_LOG=INFO
litellm --config config.yaml

# Warning logging
export LITELLM_LOG=WARNING
litellm --config config.yaml
```

---

## Troubleshooting

### Common Issues

#### Database Connection Error

```
Error: psycopg2.OperationalError: could not connect to server
```

**Solution:**
```bash
# Check PostgreSQL is running
psql -U litellm -h localhost -d litellm -c "SELECT 1"

# Verify connection string
echo $DATABASE_URL

# Test connection with psql
psql $DATABASE_URL
```

#### API Key Not Working

```
Error: Authentication failed - invalid key
```

**Solution:**
```bash
# Check key exists in database
psql $DATABASE_URL -c "SELECT key_name, created_at, spend FROM api_keys LIMIT 5;"

# Generate new key
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"models": ["gpt-4o"]}'
```

#### Rate Limit Exceeded

```
Error: Rate limit exceeded for model
```

**Solution:**
```bash
# Check current usage
redis-cli -a $REDIS_PASSWORD INFO

# Increase rate limits in config
rpm: 300  # Increase from 200

# Or create key with higher limits
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"rpm_limit": 500, "tpm_limit": 100000}'
```

#### Budget Exceeded

```
Error: Budget exceeded for this key
```

**Solution:**
```bash
# Check spend
curl http://localhost:4000/key/info \
  -H "Authorization: Bearer sk-1234567890" \
  -d '{"key": "sk-..."}'

# Update budget
curl -X POST http://localhost:4000/key/update \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{"key": "sk-...", "max_budget": 1000.0}'
```

#### Health Check Failing

```
Error: Health check failed for model
```

**Solution:**
```bash
# Check health endpoint
curl http://localhost:4000/health

# View health details
curl http://localhost:4000/health/readiness

# Check model configuration
curl http://localhost:4000/v1/models
```

### Debug Commands

```bash
# Check proxy status
curl http://localhost:4000/health

# List all models
curl http://localhost:4000/v1/models \
  -H "Authorization: Bearer sk-1234567890"

# Test a model
curl -X POST http://localhost:4000/v1/chat/completions \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "test"}]
  }'

# View logs
docker logs litellm

# Check database
psql $DATABASE_URL -c "SELECT * FROM spend_logs ORDER BY created_at DESC LIMIT 5;"
```

---

## Performance Tuning

### Worker Configuration

```bash
# Match workers to CPU cores
# Single core = 1 worker
# 4 cores = 4 workers
# 8 cores = 8 workers

docker run ... --num_workers 4
```

### Connection Pool Tuning

```yaml
general_settings:
  database_connection_pool_limit: 10  # Increase for high concurrency
  database_connection_timeout: 60     # Timeout in seconds
```

### Redis Optimization

```yaml
router_settings:
  redis_host: ${REDIS_HOST}
  redis_password: ${REDIS_PASSWORD}
  redis_port: 6379
  redis_ttl: 300  # Cache TTL in seconds
```

### Request Optimization

```yaml
litellm_settings:
  cache: true                    # Enable caching
  cache_type: redis
  cache_host: ${REDIS_HOST}
  cache_port: 6379
  
  # Batch writes for better throughput
  proxy_batch_write_at: 60      # Batch every 60 seconds
```

### Load Testing

```bash
# Install Apache Bench
apt-get install apache2-utils

# Load test the proxy
ab -n 1000 -c 10 \
  -H "Authorization: Bearer sk-1234567890" \
  -H "Content-Type: application/json" \
  -p request.json \
  http://localhost:4000/v1/chat/completions

# Using wrk
wrk -t4 -c100 -d30s \
  -H "Authorization: Bearer sk-1234567890" \
  http://localhost:4000/health
```

---

