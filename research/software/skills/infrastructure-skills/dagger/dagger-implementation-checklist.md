# Dagger Pipeline Implementation Checklist

## Quick Reference for Claude Code Implementation

### Phase 1: Core Setup (Week 1-2)

#### Project Structure
- [ ] Create `/dagger` directory with Python module structure
- [ ] Create `/dagger/__init__.py` (main UnifiedPipeline class)
- [ ] Create `/dagger/infrastructure/__init__.py` (InfrastructurePipeline)
- [ ] Create `/dagger/web/__init__.py` (WebPipeline)
- [ ] Create `/dagger/data/__init__.py` (DataPipeline)
- [ ] Create `/dagger/shared/secrets.py` (SecretsManager)
- [ ] Create `/dagger/shared/testing.py` (TestRunner)
- [ ] Create `/dagger/shared/compose.py` (Docker Compose utilities)

#### Infrastructure Config
- [ ] Create `/infrastructure/komodo/stacks/` directory
- [ ] Create web-stack.yml with Pangolin labels
- [ ] Create data-stack.yml with Pangolin labels
- [ ] Create infra-stack.yml (Newt, Periphery, etc.)
- [ ] Create `/infrastructure/pangolin/newt-config.yml`
- [ ] Create `/infrastructure/pulumi/__main__.py`
- [ ] Create `/infrastructure/ansible/deploy-periphery.yml`

#### Docker Compose Testing
- [ ] Create `/docker-compose/web-stack.compose.yml`
- [ ] Create `/docker-compose/data-stack.compose.yml`
- [ ] Create `/docker-compose/full-stack.compose.yml`
- [ ] Add Pangolin labels to all services

#### CI/CD
- [ ] Create `.github/workflows/ci.yml`
- [ ] Configure GitHub secrets (OP_CONNECT_TOKEN, DAGGER_CLOUD_TOKEN)
- [ ] Set up branch protection rules

---

### Phase 2: Infrastructure Pipeline (Week 3-4)

#### Komodo Integration
- [ ] Implement `_test_compose_files()` function
- [ ] Implement `_test_pangolin_labels()` function
- [ ] Implement `_test_komodo_stacks()` function
- [ ] Implement `_test_compose_integration()` function
- [ ] Implement `_deploy_komodo_stacks()` with TypeScript SDK
- [ ] Test Komodo API authentication
- [ ] Test stack deployment to existing Komodo Core

#### Pangolin Integration
- [ ] Implement `_deploy_newt()` function
- [ ] Implement `_configure_pangolin_resources()` function
- [ ] Implement `_verify_olm_connectivity()` function
- [ ] Generate Pangolin TypeScript client from OpenAPI spec
- [ ] Test Newt container deployment
- [ ] Test Olm client connectivity
- [ ] Test Pangolin API blueprint creation

#### Pulumi Integration
- [ ] Implement `_deploy_pulumi()` function
- [ ] Create Pulumi TypeScript program for DNS
- [ ] Create Pulumi program for Cloudflare R2
- [ ] Test infrastructure provisioning
- [ ] Test secret injection from Pulumi outputs

#### Periphery Deployment
- [ ] Implement `_deploy_periphery()` function
- [ ] Create Ansible playbook for Periphery
- [ ] Test Periphery agent registration with Komodo Core
- [ ] Verify Periphery can execute Docker commands

---

### Phase 3: Web Development Pipeline (Week 5-6)

#### TypeScript Build
- [ ] Implement `_test_typecheck()` function
- [ ] Implement `_test_unit()` function
- [ ] Implement `_test_integration()` function
- [ ] Implement `_test_build()` function
- [ ] Set up Bun build caching
- [ ] Test TypeScript compilation

#### Container Images
- [ ] Implement `build_images()` for web stack
- [ ] Create Dockerfile for TanStack Start app
- [ ] Create Dockerfile for Hono API
- [ ] Create Dockerfile for Convex backend
- [ ] Test image builds locally
- [ ] Test registry push

#### BetterAuth Setup
- [ ] Configure BetterAuth with PostgreSQL
- [ ] Set up OIDC provider plugin
- [ ] Configure trusted origins for CORS
- [ ] Test authentication flow
- [ ] Test JWT validation

#### Deployment
- [ ] Implement `_deploy_to_komodo()` function
- [ ] Implement `_run_smoke_tests()` function
- [ ] Test deployment to staging
- [ ] Verify health endpoints

---

### Phase 4: Data Engineering Pipeline (Week 7-8)

#### Python Testing
- [ ] Implement `_test_lint()` function
- [ ] Implement `_test_typecheck()` function
- [ ] Implement `_test_unit()` function
- [ ] Implement `_test_dlt_pipelines()` function
- [ ] Implement `_test_sqlmesh()` function
- [ ] Implement `_test_feast()` function

#### DLT Pipelines
- [ ] Create DLT GitHub pipeline
- [ ] Configure incremental loading
- [ ] Set up DuckDB destination
- [ ] Test pipeline execution
- [ ] Verify state management

#### CocoIndex Integration
- [ ] Create CocoIndex semantic indexing flow
- [ ] Configure OpenAI/Anthropic API
- [ ] Set up LanceDB vector store
- [ ] Test embedding generation
- [ ] Test semantic search

#### SQLMesh Transformations
- [ ] Create SQLMesh models
- [ ] Configure interval-based incremental
- [ ] Test model dependencies
- [ ] Verify transformation logic

#### Feast Feature Store
- [ ] Define Feast entities
- [ ] Create feature views
- [ ] Configure DuckDB offline store
- [ ] Configure DragonflyDB online store
- [ ] Test feature materialization
- [ ] Test online serving (<1ms latency)

#### MLflow Tracking
- [ ] Set up MLflow tracking server
- [ ] Configure PostgreSQL backend
- [ ] Test experiment logging
- [ ] Test model registry

#### Container Images
- [ ] Implement `build_images()` for data stack
- [ ] Create Dockerfile for DLT pipelines
- [ ] Create Dockerfile for CocoIndex flows
- [ ] Create Dockerfile for Dagster
- [ ] Test image builds

---

### Phase 5: Shared Infrastructure (Week 9)

#### 1Password Integration
- [ ] Implement `get_secret()` function
- [ ] Implement `get_env_secret()` function
- [ ] Implement `inject_secrets()` function
- [ ] Implement `store_secret()` function
- [ ] Test secret retrieval
- [ ] Test secret injection into containers
- [ ] Verify no plaintext exposure

#### Testing Framework
- [ ] Implement `test_docker_compose()` function
- [ ] Implement `test_pangolin_connectivity()` function
- [ ] Implement `run_e2e_tests()` function
- [ ] Set up Playwright for E2E tests
- [ ] Test Docker-in-Docker service
- [ ] Test full stack integration

---

### Phase 6: Deployment Workflows (Week 10)

#### Development Workflow
- [ ] Create `dagger/workflows/dev.py`
- [ ] Implement fast iteration loop
- [ ] Test local Docker Compose deployment
- [ ] Verify hot reload works

#### Staging Workflow
- [ ] Create `dagger/workflows/staging.py`
- [ ] Implement comprehensive test suite
- [ ] Test deployment to staging Komodo
- [ ] Verify smoke tests pass

#### Production Workflow
- [ ] Create `dagger/workflows/production.py`
- [ ] Implement manual approval gate
- [ ] Implement blue-green deployment
- [ ] Implement automatic rollback
- [ ] Test rollback functionality

---

### Phase 7: CI/CD Integration (Week 11)

#### GitHub Actions
- [ ] Create CI workflow for tests
- [ ] Create staging deployment workflow
- [ ] Create production deployment workflow
- [ ] Configure environment protection rules
- [ ] Test CI pipeline end-to-end

#### Monitoring
- [ ] Set up deployment notifications
- [ ] Configure error alerting
- [ ] Create deployment dashboard
- [ ] Set up log aggregation

---

### Phase 8: Documentation & Training (Week 12)

#### Documentation
- [ ] Write README for each pipeline
- [ ] Document common workflows
- [ ] Create troubleshooting guide
- [ ] Document rollback procedures
- [ ] Create architecture diagrams

#### Training
- [ ] Create developer onboarding guide
- [ ] Document local development setup
- [ ] Create video walkthrough
- [ ] Conduct team training session

---

## Critical Path

The following must be completed in order:

1. **Core Setup** → All other phases depend on this
2. **Infrastructure Pipeline** → Web and Data depend on this
3. **Web Pipeline** ← Can be done in parallel with Data
4. **Data Pipeline** ← Can be done in parallel with Web
5. **Shared Infrastructure** → All pipelines use this
6. **Deployment Workflows** → Requires all pipelines
7. **CI/CD Integration** → Final integration
8. **Documentation** → Ongoing throughout

---

## Quick Commands Reference

### Development
```bash
# Test everything locally
dagger call test-all --source=.

# Test Docker Compose configs
dagger call infrastructure test-compose-files --source=.

# Start local development environment
dagger call deploy-all --source=. --environment=dev
```

### Testing
```bash
# Test infrastructure
dagger call infrastructure test-all --source=.

# Test web
dagger call web test-all --source=.

# Test data
dagger call data test-all --source=.

# Test Pangolin connectivity
dagger call infrastructure verify-olm-connectivity --environment=staging
```

### Deployment
```bash
# Deploy to staging
dagger call deploy-all --source=. --environment=staging

# Deploy to production (requires approval in CI)
dagger call deploy-all --source=. --environment=production

# Rollback
dagger call infrastructure rollback --environment=production --previous-version=abc123
```

### Building
```bash
# Build web images
dagger call web build-images \
  --source=. \
  --registry=ghcr.io/yourorg \
  --tag=$(git rev-parse --short HEAD) \
  --registry-username=env:GITHUB_USER \
  --registry-password=env:GITHUB_TOKEN

# Build data images
dagger call data build-images \
  --source=. \
  --registry=ghcr.io/yourorg \
  --tag=$(git rev-parse --short HEAD) \
  --registry-username=env:GITHUB_USER \
  --registry-password=env:GITHUB_TOKEN
```

---

## Priority Focus Areas

### For Immediate Implementation (Week 1-4)

1. **Docker Compose Testing** ← Most critical for validation
   - Implement compose file validation
   - Implement Pangolin label validation
   - Implement integration testing
   
2. **Komodo Integration** ← Required for deployment
   - Implement stack deployment via TypeScript SDK
   - Test with existing Komodo Core
   - Verify periphery communication
   
3. **Pangolin Integration** ← Required for networking
   - Deploy Newt agents
   - Configure resources via API
   - Test Olm connectivity
   
4. **1Password Secrets** ← Required for all pipelines
   - Implement secret fetching
   - Test secret injection
   - Verify no plaintext exposure

---

## Success Criteria

### Infrastructure Pipeline
- ✅ All Docker Compose files validate successfully
- ✅ All Pangolin labels are valid
- ✅ Compose stacks start and pass health checks
- ✅ Newt agents register with Pangolin
- ✅ Olm clients can connect and reach services
- ✅ Komodo deployments succeed
- ✅ Pulumi provisions infrastructure without errors

### Web Pipeline
- ✅ TypeScript compiles without errors
- ✅ All unit tests pass
- ✅ Integration tests pass with real services
- ✅ Production build completes successfully
- ✅ Docker images build and push
- ✅ BetterAuth authentication works
- ✅ Convex real-time updates work
- ✅ Health endpoints respond

### Data Pipeline
- ✅ Python code passes linting and type checking
- ✅ All unit tests pass
- ✅ DLT pipelines execute incrementally
- ✅ CocoIndex generates embeddings
- ✅ SQLMesh models transform data correctly
- ✅ Feast serves features with <1ms latency
- ✅ MLflow tracks experiments
- ✅ Dagster orchestrates workflows

### End-to-End
- ✅ Full stack deploys to dev/staging/production
- ✅ All services communicate correctly
- ✅ Pangolin networking works
- ✅ Authentication flows work
- ✅ Data pipelines execute on schedule
- ✅ Rollbacks work correctly
- ✅ CI/CD pipeline passes

---

## Common Troubleshooting

### Docker Compose Issues
```bash
# Validate syntax
docker-compose -f file.yml config --quiet

# Check service health
docker-compose ps --format json | jq '.[] | select(.Health == "healthy")'

# View logs
docker-compose logs --tail=100
```

### Pangolin Issues
```bash
# Check Newt registration
curl http://localhost:8080/api/sites

# Test Olm connection
olm --id $OLM_ID --secret $OLM_SECRET --endpoint https://pangolin.example.com

# Verify tunnel
ip link show | grep wg
```

### Komodo Issues
```bash
# Check periphery status
curl -H "Authorization: Bearer $TOKEN" http://komodo-core/api/servers

# View deployment logs
curl -H "Authorization: Bearer $TOKEN" http://komodo-core/api/deployments/{id}/logs
```

### 1Password Issues
```bash
# Test op CLI
op signin
op read "op://DevOps/test-secret/credential"

# Verify Dagger integration
dagger call secrets-manager get-secret --key=test-secret
```

---

This checklist provides a structured approach for Claude Code to implement the comprehensive Dagger pipeline orchestration system. Start with Phase 1 and work sequentially through each phase, checking off items as they're completed.
