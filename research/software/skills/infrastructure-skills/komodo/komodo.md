# Komodo Infrastructure Management Skill

## Description
Expert assistant for Komodo (komo.do) - an open-source Docker infrastructure management platform. This skill provides comprehensive support for deploying, managing, and automating containerized applications using Komodo's Core/Periphery architecture.

## Skill Scope

### What This Skill Covers
- Komodo resource configuration (Stacks, Deployments, Builds, Procedures, Actions)
- Docker Compose stack deployment via Komodo
- Build automation and CI/CD workflows
- GitOps with Resource Sync
- Infrastructure monitoring and alerting
- Komodo API and TypeScript SDK usage
- CLI (`km`) command usage
- Automation with Procedures and Actions
- Multi-server orchestration
- Security and access control configuration

### When to Use This Skill
- Deploying containerized applications with Komodo
- Creating or modifying Komodo resource configurations
- Setting up GitOps workflows with Resource Sync
- Automating infrastructure tasks with Procedures and Actions
- Troubleshooting Komodo deployments
- Configuring monitoring and alerts
- Managing multi-server Docker infrastructure
- Converting existing Docker setups to Komodo

## Core Concepts

### Architecture
Komodo uses a two-component architecture:
- **Core**: Central server with REST/WebSocket API and web UI (port 9120)
- **Periphery**: Lightweight agents on managed servers (port 8120)
- Communication: Core → Periphery (unidirectional initiation)
- Authentication: Passkey-based (KOMODO_PASSKEY = PERIPHERY_PASSKEY)

### Resource Types
All resources in Komodo share common traits:
- Unique name and ID
- Tagging for organization
- Permission controls
- Audit logging

**Primary Resources:**
1. **Server**: Connection to Periphery agent with monitoring
2. **Deployment**: Single Docker container deployment
3. **Stack**: Docker Compose project (UI, Host, or Git-based)
4. **Build**: Automated Docker image builds from Git
5. **Builder**: Build capacity (Server or AWS instances)
6. **Repo**: Git repository for script execution
7. **Procedure**: Multi-stage orchestration (parallel + sequential)
8. **Action**: TypeScript automation with full API access
9. **Resource Sync**: GitOps declarative infrastructure
10. **Alerter**: Notification routing system
11. **Template**: Reusable resource configurations

## Common Tasks

### Task 1: Create Docker Compose Stack from Git

**Context:** Deploy a Docker Compose application stored in Git with auto-deployment on push.

**Steps:**
1. Identify Stack configuration requirements:
   - Git repository URL and branch
   - Environment variables needed
   - Target Server for deployment
   - Docker registry accounts (if using private images)

2. Create Resource Sync TOML configuration:
```toml
[[stack]]
name = "my-application"
description = "My application stack deployed from Git"

[stack.config]
server = "main-server"
repo = "username/my-app-repo"
git_account = "github-account"
branch = "main"
file_paths = ["docker-compose.yml"]

[stack.config.environment]
NODE_ENV = "production"
APP_VERSION = "${APP_VERSION}"
DB_PASSWORD = "${DB_PASSWORD}"
API_KEY = "${API_KEY}"

[[stack.config.labels]]
environment = "production"
app = "my-application"
```

3. Configure webhook for auto-deployment:
   - Get webhook URL from Komodo Stack resource
   - Add to Git provider (GitHub/GitLab/Gitea)
   - Set content type: application/json
   - Configure webhook secret

4. Verify deployment:
   - Check Stack status in Komodo UI
   - Monitor container logs
   - Verify health checks

**Best Practices:**
- Use Git-based Stacks for version control
- Store secrets in Komodo Core config, not in compose files
- Use variable interpolation: `${VARIABLE_NAME}`
- Enable "Poll for Updates" to review changes before auto-deploy
- Use monorepo structure for multiple related stacks

### Task 2: Create Multi-Stage Deployment Procedure

**Context:** Orchestrate updates across multiple services with proper sequencing.

**Steps:**
1. Identify deployment stages and dependencies:
   - Which repos need to be pulled
   - Which builds need to run
   - Which stacks depend on which builds
   - What cleanup is needed

2. Create Procedure TOML configuration:
```toml
[[procedure]]
name = "deploy-full-stack"
description = "Deploy all services in correct order"

# Stage 1: Pull all repositories (parallel)
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "PullRepo"
params = { repo = "frontend-repo" }

[[procedure.config.stages.executions]]
operation = "PullRepo"
params = { repo = "backend-repo" }

[[procedure.config.stages.executions]]
operation = "PullRepo"
params = { repo = "api-repo" }

# Stage 2: Build all images (parallel, waits for Stage 1)
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "RunBuild"
params = { build = "frontend-build" }

[[procedure.config.stages.executions]]
operation = "RunBuild"
params = { build = "backend-build" }

[[procedure.config.stages.executions]]
operation = "RunBuild"
params = { build = "api-build" }

# Stage 3: Deploy stacks (parallel, waits for Stage 2)
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "frontend-stack" }

[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "backend-stack" }

[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "api-stack" }

# Stage 4: Cleanup (waits for Stage 3)
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "PruneSystem"
params = { server = "main-server" }
```

3. Configure triggering:
   - Manual: Button in Komodo UI
   - Webhook: For CI/CD integration
   - Scheduled: Cron format for regular updates
   - Run-on-startup: For initialization

4. Test execution:
   - Run procedure manually first
   - Monitor stage completion
   - Check logs for errors
   - Verify all stacks deployed correctly

**Best Practices:**
- Group independent operations in same stage (parallel execution)
- Order stages by dependencies (sequential execution)
- Add cleanup stage at the end
- Use BatchDeployStackIfChanged for conditional updates
- Monitor procedure execution logs

### Task 3: Create TypeScript Action for Complex Automation

**Context:** Implement custom automation logic beyond Procedure capabilities.

**Steps:**
1. Identify automation requirements:
   - What resources need to be queried
   - What operations need to be performed
   - What conditions determine actions
   - What parameters should be configurable

2. Create Action TypeScript script:
```typescript
// Action: sync-environment-branches
// Description: Sync all stacks to match a target branch

// ARGS.branch is passed via webhook or manual execution
const targetBranch = ARGS.branch || "main";
const dryRun = ARGS.dryRun || false;

// Pre-initialized 'komodo' client available
const stacks = await komodo.read("ListStacks", {});

const updates = [];

for (const stack of stacks) {
  const config = await komodo.read("GetStack", { stack: stack.name });

  // Only update Git-based stacks
  if (config.config.repo && config.config.branch !== targetBranch) {
    updates.push({
      name: stack.name,
      oldBranch: config.config.branch,
      newBranch: targetBranch
    });

    if (!dryRun) {
      await komodo.write("UpdateStack", {
        name: stack.name,
        config: {
          ...config.config,
          branch: targetBranch
        }
      });

      // Deploy with new branch
      await komodo.execute("DeployStack", { stack: stack.name });
    }
  }
}

// Return summary
return {
  message: dryRun ? "Dry run completed" : "Branch sync completed",
  updatedStacks: updates.length,
  updates: updates
};
```

3. Create Action resource configuration:
```toml
[[action]]
name = "sync-environment-branches"
description = "Sync all stack branches to target environment"

[action.config]
# TypeScript code goes in Komodo UI editor

# Default arguments
[action.config.args]
branch = "main"
dryRun = false
```

4. Configure triggering and testing:
   - Test with dryRun=true first
   - Configure webhook for Git events
   - Use RunAction via API or CLI
   - Enable run-on-startup if needed (v1.19.0+)

**Best Practices:**
- Use ARGS for configurable parameters
- Provide sensible defaults
- Implement dry-run mode for testing
- Return useful summary information
- Use type-safe operations (TypeScript inference)
- Handle errors gracefully
- Log important actions for audit trail

### Task 4: Set Up GitOps with Resource Sync

**Context:** Manage all infrastructure as code using Git repository.

**Steps:**
1. Create infrastructure repository structure:
```
/komodo-infra
  /stacks
    frontend.toml
    backend.toml
    database.toml
  /deployments
    monitoring.toml
  /procedures
    deploy-all.toml
    update-all.toml
  /actions
    custom-automation.toml
  sync.toml
```

2. Define Resource Sync configuration (sync.toml):
```toml
[[resource_sync]]
name = "komodo-infrastructure"
description = "All infrastructure as code"

[resource_sync.config]
repo = "username/komodo-infra"
git_account = "github-account"
branch = "main"
resource_path = [
  "stacks/*.toml",
  "deployments/*.toml",
  "procedures/*.toml",
  "actions/*.toml"
]
managed = true
files_on_stack = false
git_provider = "github"
```

3. Create individual resource definitions:

**Example: stacks/frontend.toml**
```toml
[[stack]]
name = "frontend"
description = "Frontend React application"

[stack.config]
server = "main-server"
repo = "username/frontend-app"
git_account = "github-account"
branch = "main"
file_paths = ["docker-compose.yml"]

[stack.config.environment]
NODE_ENV = "production"
API_URL = "${API_URL}"
APP_VERSION = "${APP_VERSION}"

[[stack.config.after]]
"database"
```

**Example: procedures/deploy-all.toml**
```toml
[[procedure]]
name = "deploy-all-production"
description = "Deploy all production services"

[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "BatchDeployStackIfChanged"
params = { pattern = "prod-*" }
```

4. Set up workflow:
   - Commit resource definitions to Git
   - Create Resource Sync in Komodo UI
   - Configure webhook for auto-sync
   - Test by making changes in UI
   - Verify changes committed back to Git (bidirectional sync)
   - Test by making changes in Git
   - Verify changes applied in Komodo

**Best Practices:**
- Use managed mode for Komodo to own resource state
- Organize resources by type in subdirectories
- Use `after` arrays for deployment ordering
- Version control all infrastructure changes
- Review changes via Git PR process before merge
- Enable webhook for automatic sync
- Use bidirectional sync for UI changes
- Document resource configurations

### Task 5: Configure Build and Auto-Deploy Workflow

**Context:** Automate building Docker images and deploying to environments on Git push.

**Steps:**
1. Create Build resource configuration:
```toml
[[build]]
name = "my-app-build"
description = "Build Docker image from Git"

[build.config]
repo = "username/my-app"
git_account = "github-account"
branch = "main"
builder = "main-server-builder"
docker_account = "dockerhub-account"

[build.config.build_args]
NODE_VERSION = "20"
BUILD_ENV = "production"

[build.config.image]
name = "username/my-app"
tag = "latest"
```

2. Create Deployment resource configuration:
```toml
[[deployment]]
name = "my-app-deployment"
description = "Deploy my-app container"

[deployment.config]
server = "main-server"
build = "my-app-build"
network = "host"

[deployment.config.environment]
PORT = "3000"
DB_HOST = "${DB_HOST}"
DB_PASSWORD = "${DB_PASSWORD}"

[[deployment.config.volumes]]
host = "/data/app"
container = "/app/data"

[deployment.config.restart]
policy = "unless-stopped"

[deployment.config.auto_update]
enabled = true
```

3. Configure webhooks:
   - Get Build webhook URL from Komodo
   - Add webhook to Git repository
   - Set trigger events: push to main branch
   - Configure webhook secret
   - Test with a push to repository

4. Verify workflow:
   - Push code to Git → Webhook triggers Build
   - Build completes → Image pushed to registry
   - Deployment auto-updates to new image
   - Container restarts with new version

**Best Practices:**
- Use semantic versioning for image tags
- Enable auto-update for development/staging
- Use "Poll for Updates" for production (manual approval)
- Configure build notifications via Alerter
- Use AWS Builder for burst capacity
- Set resource limits on deployments
- Use health checks in containers
- Monitor build success/failure rates

## API Usage Patterns

### Using TypeScript SDK
```typescript
import { KomodoClient } from 'komodo_client';

const client = KomodoClient({
  url: "https://komodo.example.com",
  type: "api-key",
  params: {
    key: "your-api-key",
    secret: "your-api-secret"
  }
});

// Read operations
const stacks = await client.read("ListStacks", {});
const deployment = await client.read("GetDeployment", {
  deployment: "my-app"
});

// Execute operations
await client.execute("DeployStack", { stack: "frontend" });
await client.execute("RunBuild", { build: "my-app-build" });

// Write operations
await client.write("UpdateStack", {
  name: "frontend",
  config: { /* updated configuration */ }
});
```

### Using CLI (`km`)
```bash
# List containers
km ps --down

# Deploy resources
km deploy-stack frontend
km run-build my-app-build
km deploy my-app-deployment

# Run automation
km run action sync-branches
km run procedure deploy-all-production

# Maintenance
km prune-system
km database backup

# Skip confirmations (CI/CD)
km deploy-stack frontend --yes
```

### In Actions (Pre-initialized Client)
```typescript
// 'komodo' client is already available
const stacks = await komodo.read("ListStacks", {});

// Access webhook data
const branch = WEBHOOK_BRANCH;
const payload = WEBHOOK_BODY;

// Access action arguments
const environment = ARGS.environment || "production";
const dryRun = ARGS.dryRun || false;

// Perform operations
for (const stack of stacks) {
  if (stack.name.includes(environment)) {
    await komodo.execute("DeployStack", { stack: stack.name });
  }
}
```

## Configuration Patterns

### Environment Variables
**Core (.env):**
```bash
# Essential
KOMODO_PASSKEY=random_secure_passkey
KOMODO_HOST=https://komodo.example.com
KOMODO_TITLE="Production Komodo"

# Initial setup
KOMODO_INIT_ADMIN_USERNAME=admin
KOMODO_INIT_ADMIN_PASSWORD=secure_password
KOMODO_FIRST_SERVER=http://192.168.1.10:8120

# Automation
KOMODO_RESOURCE_POLL_INTERVAL=3600
KOMODO_WEBHOOK_SECRET=webhook_secret

# OAuth (optional)
KOMODO_GITHUB_OAUTH_ENABLED=true
KOMODO_GITHUB_OAUTH_ID=github_client_id
KOMODO_GITHUB_OAUTH_SECRET=github_client_secret
```

**Periphery (periphery.toml):**
```toml
passkey = "matches_komodo_passkey"
port = 8120

[directories]
repo_dir = "/opt/komodo/repos"
stack_dir = "/opt/komodo/stacks"

[security]
allowed_ips = ["192.168.1.0/24"]
```

### Secrets and Variables
**Core Config (core.toml):**
```toml
[secrets]
DB_PASSWORD = "secret_database_password"
API_KEY = "secret_api_key"
JWT_SECRET = "jwt_signing_secret"

[variables]
APP_VERSION = "1.0.0"
ENVIRONMENT = "production"
API_URL = "https://api.example.com"
```

**Usage in Resources:**
```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DB_PASSWORD=${DB_PASSWORD}
      - API_KEY=${API_KEY}
      - APP_VERSION=${APP_VERSION}
```

## Troubleshooting Guide

### Issue: Stack fails to deploy
**Diagnosis:**
1. Check Stack logs in Komodo UI
2. Verify Server connection and health
3. Check docker-compose.yml syntax
4. Verify environment variables are set
5. Check image availability in registry

**Resolution:**
```bash
# On Periphery server
docker compose -f /path/to/stack/docker-compose.yml config
docker compose -f /path/to/stack/docker-compose.yml up -d
```

### Issue: Build fails
**Diagnosis:**
1. Check Build logs in Komodo
2. Verify Dockerfile exists in repository
3. Check Builder connectivity
4. Verify build arguments
5. Check Docker registry credentials

**Resolution:**
- Test build locally: `docker build -t test .`
- Verify Builder has Docker access
- Check registry authentication
- Review build args and secrets

### Issue: Webhook not triggering
**Diagnosis:**
1. Verify webhook URL is correct
2. Check webhook secret matches
3. Review Git provider webhook delivery logs
4. Check Komodo webhook logs
5. Verify KOMODO_WEBHOOK_SECRET is set

**Resolution:**
- Test webhook manually in Git provider
- Check Komodo logs for webhook receipt
- Verify content type is application/json
- Ensure webhook secret matches
- Check firewall rules

### Issue: Periphery unreachable
**Diagnosis:**
1. Check Periphery service status: `systemctl status komodo-periphery`
2. Verify port 8120 is open
3. Check passkey configuration
4. Verify IP whitelisting
5. Check network connectivity from Core to Periphery

**Resolution:**
```bash
# Restart Periphery
systemctl restart komodo-periphery

# Check logs
journalctl -u komodo-periphery -f

# Test connectivity
curl http://periphery-host:8120/health

# Verify firewall
ufw status
```

### Issue: Resource Sync not applying changes
**Diagnosis:**
1. Check Resource Sync status in UI
2. Verify TOML syntax is valid
3. Check Git repository accessibility
4. Review Resource Sync logs
5. Verify webhook is configured

**Resolution:**
- Validate TOML: Use TOML validator
- Check Git credentials
- Manually trigger sync in UI
- Review error messages in logs
- Verify `managed = true` in config

## Security Best Practices

### Network Security
- Always use reverse proxy for HTTPS (Caddy, Nginx, Traefik)
- Firewall Periphery port 8120: Only allow Core IP
- Use IP whitelisting in Periphery config
- Disable direct internet access to Core/Periphery ports
- Use VPN for remote access (Tailscale, WireGuard, Pangolin)

### Authentication & Authorization
- Rotate API keys regularly
- Use OAuth/OIDC for team access
- Implement User Groups for permission management
- Follow principle of least privilege
- Enable user approval workflow
- Review permissions regularly

### Secrets Management
- Store secrets in Komodo Core config, not in compose files
- Use variable interpolation: `${SECRET_NAME}`
- Consider external secret management (1Password, Vault)
- Use file-based secrets for Docker Compose: `${SECRET}_FILE`
- Never commit secrets to Git
- Rotate secrets periodically

### Infrastructure Security
- Keep Komodo Core and Periphery updated
- Regular security audits of resources
- Monitor audit logs for suspicious activity
- Use read-only permissions where possible
- Enable alerting for security events
- Regular backups of Komodo database

## Performance Optimization

### Deployment Performance
- Use host networking when possible (lower overhead)
- Configure resource limits (CPU, memory) appropriately
- Use monorepo structure for multiple stacks (single clone)
- Local builders for frequent builds
- AWS builders for burst capacity
- Prune unused Docker resources regularly

### Monitoring Performance
- Set appropriate polling intervals (avoid too frequent)
- Use alerter unresolved timeout to prevent spam
- Configure critical thresholds realistically
- Use batch operations for multiple resources
- Leverage procedure stages for parallelization

### Build Performance
- Use Docker layer caching effectively
- Multi-stage builds for smaller images
- Use .dockerignore to exclude unnecessary files
- Consider build argument reuse
- Use local registry for private dependencies

## Integration Examples

### GitHub Actions Integration
```yaml
# .github/workflows/deploy.yml
name: Deploy to Komodo

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Install Komodo CLI
        run: |
          cargo install komodo_cli

      - name: Configure Komodo
        run: |
          mkdir -p ~/.config/komodo
          cat > ~/.config/komodo/creds.toml <<EOF
          url = "${{ secrets.KOMODO_URL }}"
          key = "${{ secrets.KOMODO_KEY }}"
          secret = "${{ secrets.KOMODO_SECRET }}"
          EOF

      - name: Deploy
        run: |
          km run-build my-app-build --yes
          km deploy-stack production-stack --yes
```

### Renovate Integration
```json
{
  "extends": ["config:base"],
  "docker": {
    "enabled": true
  },
  "packageRules": [
    {
      "matchDatasources": ["docker"],
      "automerge": true,
      "automergeType": "branch"
    }
  ]
}
```

### Ansible Integration
```yaml
# playbook.yml
- name: Deploy Komodo Periphery
  hosts: docker_hosts
  tasks:
    - name: Install Periphery
      shell: |
        curl -sSL https://raw.githubusercontent.com/moghtech/komodo/main/scripts/setup-periphery.py | python3
      environment:
        PERIPHERY_PASSKEY: "{{ komodo_passkey }}"

    - name: Configure Periphery
      template:
        src: periphery.toml.j2
        dest: /etc/komodo/periphery.toml
```

## Reference Files

### Complete Stack Example
```toml
[[stack]]
name = "production-app"
description = "Production application stack"

[stack.config]
server = "prod-server"
repo = "company/production-app"
git_account = "github-prod"
branch = "main"
file_paths = ["docker-compose.yml", "configs/app.json"]

[stack.config.environment]
NODE_ENV = "production"
APP_VERSION = "${APP_VERSION}"
DB_HOST = "${DB_HOST}"
DB_PASSWORD = "${DB_PASSWORD}"
API_KEY = "${API_KEY}"
REDIS_URL = "redis://redis:6379"

[[stack.config.labels]]
environment = "production"
team = "backend"
criticality = "high"

[[stack.config.after]]
"database-stack"
"redis-stack"

[stack.config.auto_update]
enabled = false  # Manual approval for production
```

### Complete Procedure Example
```toml
[[procedure]]
name = "production-deployment"
description = "Full production deployment workflow"

# Stage 1: Preparation
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "PullRepo"
params = { repo = "frontend-repo" }

[[procedure.config.stages.executions]]
operation = "PullRepo"
params = { repo = "backend-repo" }

# Stage 2: Build images
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "RunBuild"
params = { build = "frontend-build" }

[[procedure.config.stages.executions]]
operation = "RunBuild"
params = { build = "backend-build" }

# Stage 3: Deploy infrastructure
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "database-stack" }

[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "redis-stack" }

# Stage 4: Deploy applications
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "backend-stack" }

# Stage 5: Deploy frontend (after backend is up)
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "DeployStack"
params = { stack = "frontend-stack" }

# Stage 6: Cleanup
[[procedure.config.stages]]
[[procedure.config.stages.executions]]
operation = "PruneImages"
params = { server = "prod-server" }
```

## Resources

### Official Documentation
- Website: https://komo.do
- Docs: https://komo.do/docs
- GitHub: https://github.com/mbecker20/komodo
- Discord: discord.gg/DRqE8Fvg5c

### Community Tools
- komodo-import: Import existing Docker Compose stacks
- komodo-utilities: Custom alerters and extensions

### Related Projects
- Pangolin: VPN/networking integration
- Dagger: CI/CD orchestration
- Ansible: Infrastructure provisioning

## Skill Activation

This skill should be automatically activated when the user's query involves:
- Komodo infrastructure management
- Docker Compose deployments via Komodo
- GitOps workflows
- Container orchestration
- Build automation
- Infrastructure as code with Komodo
- Komodo resource configuration
- Multi-server Docker management

---

*Last Updated: 2025-11-17*
*Komodo Version: v1.19.5*
*License: GPL-3.0*
