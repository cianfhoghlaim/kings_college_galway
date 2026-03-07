# Infrastructure Decision Matrices

> Technology comparison and selection guidance for the infrastructure stack

**Last Updated**: December 2025
**Status**: Generated from existing documentation

---

## Overview

This document provides decision matrices for selecting appropriate technologies across the infrastructure stack. Each matrix includes comparison criteria, trade-offs, and recommendations.

---

## 1. Deployment Approach Comparison

### When to Use What

| Approach | Complexity | Scalability | Best For |
|----------|------------|-------------|----------|
| **Systemd** | Low | Single server | Simple services, edge deployments |
| **Docker Compose** | Medium | Single server | Development, small production |
| **Komodo** | Medium-High | Multi-server | Production deployments, GitOps |
| **Kubernetes** | High | Cluster | Large-scale, complex orchestration |

### Detailed Comparison

| Factor | Systemd | Docker Compose | Komodo | Kubernetes |
|--------|---------|----------------|--------|------------|
| Setup Time | Minutes | Minutes | Hours | Days |
| Learning Curve | Low | Low | Medium | High |
| Resource Overhead | Minimal | Low | Low | High |
| Failure Recovery | Manual | Manual | Automatic | Automatic |
| Rolling Updates | No | Limited | Yes | Yes |
| Multi-Server | No | No | Yes | Yes |
| Secrets Integration | Manual | Manual | 1Password native | Secrets CRD |
| Monitoring | External | External | Built-in | External |

### Recommendation

**This project uses: Komodo + Docker Compose**

Rationale:
- Multi-server support for distributed services
- Native 1Password integration for secrets
- Simpler than Kubernetes for our scale
- GitOps workflow via Periphery agents

---

## 2. Secrets Management Options

### Comparison Matrix

| Factor | 1Password CLI | 1Password Connect | 1Password SA | Infisical | Vault |
|--------|---------------|-------------------|--------------|-----------|-------|
| **Cost** | Included | Included | Included | Free tier | Free/Paid |
| **Self-Hosted** | No | Yes | No | Yes | Yes |
| **CI/CD Friendly** | No (interactive) | Yes | Yes | Yes | Yes |
| **Local Dev** | Yes | Overkill | Limited | Yes | Complex |
| **Rotation** | Manual | Manual | Manual | Auto | Auto |
| **Audit Logs** | Yes | Yes | Yes | Yes | Yes |
| **Complexity** | Low | Medium | Low | Low | High |

### Selection Flowchart

```
Start
  │
  ├─ Single developer?
  │   └─ Yes → 1Password CLI
  │
  ├─ CI/CD automation needed?
  │   ├─ Yes + existing 1Password → Service Account
  │   └─ Yes + open source preferred → Infisical
  │
  ├─ Always-on services need secrets?
  │   └─ Yes → 1Password Connect Server
  │
  └─ Enterprise compliance required?
      └─ Yes → HashiCorp Vault
```

### Recommendation

**This project uses: 1Password (CLI + Service Account)**

Rationale:
- Already using 1Password for team credentials
- Service Account for CI/CD automation
- CLI for local development
- No additional infrastructure required

---

## 3. Networking & Access Control

### Access Pattern Comparison

| Pattern | Security | User Experience | Complexity | Use Case |
|---------|----------|-----------------|------------|----------|
| **Public** (reverse proxy) | Medium | Best | Low | Public APIs, web apps |
| **Private** (VPN-only) | High | Requires client | Medium | Admin panels, internal tools |
| **Hybrid** | High | Mixed | High | Public frontend + private backend |
| **Direct IP** | Low | Direct | None | Development only |

### Pangolin Component Selection

| Component | Purpose | Install Where |
|-----------|---------|---------------|
| **Gerbil** | WireGuard tunnel server | Central/cloud |
| **Newt** | Site connector | Each server |
| **Olm** | VPN client | Developer machines |

### When to Use Each Access Pattern

| Service Type | Access Pattern | Example |
|--------------|----------------|---------|
| Public API | Public | api.example.com |
| Marketing site | Public | www.example.com |
| Admin dashboard | Private | admin.example.com |
| Database | Private | Never expose |
| Monitoring | Private | metrics.example.com |
| Internal API | Hybrid | Public endpoint, private health |

### Recommendation

**This project uses: Pangolin with Hybrid access**

Rationale:
- Zero-trust by default
- Public APIs via reverse proxy
- Private services via VPN
- Auto-registration via Docker labels

---

## 4. CI/CD Tool Comparison

### Feature Matrix

| Factor | Dagger | GitHub Actions | GitLab CI | Jenkins |
|--------|--------|----------------|-----------|---------|
| **Language** | TS/Python/Go | YAML | YAML | Groovy |
| **Local Testing** | Native | Limited (act) | Limited | Yes |
| **CI/Local Parity** | Exact | Different | Different | Different |
| **Caching** | Automatic | Manual config | Manual config | Plugins |
| **Debugging** | Interactive | Log-based | Log-based | Log-based |
| **Self-Hosted** | Any | Any | Any | Required |
| **Learning Curve** | Medium | Low | Low | High |
| **Reusability** | Modules | Composite actions | Includes | Shared libs |

### When to Choose Dagger

Prefer Dagger when:
- You want local testing to match CI exactly
- Pipeline logic is complex (conditionals, loops)
- Multi-language support needed
- Container-based workflows
- Debugging pipelines is important

Prefer YAML-based CI (Actions/GitLab) when:
- Simple linear workflows
- Team prefers declarative config
- Existing YAML expertise
- Tight integration with specific platform

### Recommendation

**This project uses: Dagger + Forgejo Actions**

Rationale:
- TypeScript for complex pipeline logic
- Local testing matches CI exactly
- Module-based code reuse
- Forgejo Actions for triggers

---

## 5. Infrastructure as Code

### IaC Tool Comparison

| Factor | Pulumi | Terraform | Ansible | CloudFormation |
|--------|--------|-----------|---------|----------------|
| **Language** | TS/Python/Go/YAML | HCL | YAML | JSON/YAML |
| **State Management** | Backend required | Backend required | Stateless | AWS native |
| **Learning Curve** | Medium | Medium | Low | Medium |
| **Testing** | Native | Limited | Limited | Limited |
| **Provider Coverage** | Good | Excellent | N/A (config) | AWS only |
| **Type Safety** | Strong (TS) | Limited | None | None |
| **Secret Handling** | Good | External | External | SSM |

### Selection Guide

| Use Case | Recommended Tool |
|----------|------------------|
| Cloud resource provisioning | Pulumi (TypeScript) |
| Server configuration | Ansible |
| AWS-only infrastructure | CloudFormation |
| Multi-cloud, team prefers HCL | Terraform |
| Kubernetes resources | Pulumi or Helm |

### Recommendation

**This project uses: Pulumi (TypeScript) + Ansible**

Rationale:
- TypeScript for type-safe infrastructure
- Consistency with application code
- Ansible for server configuration
- 1Password integration for secrets

---

## 6. Installation Methods

### Tool Installation Comparison

| Factor | Host OS (systemd) | Docker Container | Komodo Periphery |
|--------|-------------------|------------------|------------------|
| **Dependencies** | Must manage | Isolated | Isolated |
| **Updates** | Package manager | Image pull | Image pull |
| **Privileges** | Root required | Container only | Container only |
| **Resource Usage** | Lower | Higher | Higher |
| **Debugging** | Direct access | Container access | Container access |
| **Portability** | OS-specific | Any Docker host | Any Docker host |

### Recommendation by Tool

| Tool | Recommended Installation |
|------|-------------------------|
| 1Password CLI | Host OS (for interactive use) |
| Komodo Periphery | Docker container |
| Pangolin Newt | Docker container |
| Database | Docker container |
| Application | Docker container |

---

## 7. Cloud Platform Comparison

### Serverless Platform Matrix

| Factor | Cloudflare | AWS Lambda | Azure Functions | Vercel |
|--------|------------|------------|-----------------|--------|
| **Cold Start** | None (V8) | 100ms+ | 100ms+ | Variable |
| **Pricing** | Generous free | Pay per invoke | Pay per invoke | Generous free |
| **Database** | D1 (SQLite) | RDS/DynamoDB | CosmosDB | Postgres |
| **Storage** | R2 (S3 compat) | S3 | Blob Storage | Blob |
| **Edge Locations** | 200+ | Limited | Limited | 10+ |
| **Vendor Lock-in** | Low | High | High | Medium |

### When to Choose Each

| Requirement | Platform |
|-------------|----------|
| Global low latency | Cloudflare Workers |
| Complex AWS integration | AWS Lambda |
| Microsoft ecosystem | Azure Functions |
| Next.js deployment | Vercel |
| Self-hosted required | None (use containers) |

### Recommendation

**This project uses: Cloudflare (Workers, D1, R2)**

Rationale:
- Zero cold starts (V8 isolates)
- Global edge network (200+ locations)
- S3-compatible storage (R2)
- SQLite-compatible database (D1)
- Cost-effective for our scale

---

## 8. Decision Summary

### Current Stack

| Category | Choice | Alternatives Considered |
|----------|--------|------------------------|
| **Deployment** | Komodo | Docker Compose, Kubernetes |
| **Secrets** | 1Password | Infisical, Vault |
| **Networking** | Pangolin | Cloudflare Tunnel, Tailscale |
| **CI/CD** | Dagger | GitHub Actions, GitLab CI |
| **IaC** | Pulumi | Terraform, CloudFormation |
| **Serverless** | Cloudflare | AWS Lambda, Vercel |
| **Config Mgmt** | Ansible | Chef, Puppet |

### Decision Criteria Used

1. **Team expertise**: Prefer familiar technologies
2. **Complexity budget**: Avoid over-engineering
3. **Cost**: Open source when possible
4. **Integration**: 1Password across all tools
5. **Vendor lock-in**: Minimize where possible
6. **Type safety**: TypeScript/Pydantic preferred

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Infrastructure architecture overview
- [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Step-by-step setup guide
- [/infrastructure/dagger/](./dagger/) - Dagger pipeline documentation
- [/infrastructure/komodo/](./komodo/) - Komodo deployment documentation
- [/infrastructure/pangolin/](./pangolin/) - Pangolin networking documentation
- [/infrastructure/pulumi/](./pulumi/) - Pulumi IaC documentation
