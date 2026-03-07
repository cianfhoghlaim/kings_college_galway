# Infrastructure Research - Consolidated Index

This directory contains consolidated research for the infrastructure layer of the hackathon platform.

## Directory Structure

```
infrastructure/consolidated/
├── 00-overview/
│   ├── ARCHITECTURE.md        # Core infrastructure architecture
│   ├── DECISION_MATRICES.md   # Technology selection criteria
│   └── IMPLEMENTATION_GUIDE.md
├── 01-selfhosting/
│   ├── bunchloch.md           # Selfhosting stack overview
│   ├── comparing-approaches-pangolin-registration-komodo-deployment.md
│   └── hosting-litellm-pangolin-public-vs-private-access-models.md
├── 02-cicd/                   # CI/CD pipeline patterns
└── 03-cloud-services/         # Cloud service integrations
```

## Related Skills

Tool-specific documentation has been moved to `.claude/skills/`:

| Tool | Skill Location | Purpose |
|------|----------------|---------|
| Cloudflare | `.claude/skills/cloudflare/` | Workers, R2, D1, Tunnels |
| Dagger | `.claude/skills/dagger/` | CI/CD pipeline orchestration |
| Docker Compose | `.claude/skills/docker-compose/` | Container orchestration |
| Komodo | `.claude/skills/komodo/` | Deployment management |
| Pangolin | `.claude/skills/pangolin/` | VPN & reverse proxy |
| Pulumi | `.claude/skills/pulumi/` | Infrastructure as Code |
| LiteLLM | `.claude/skills/litellm/` | LLM gateway proxy |

## Bunchloch Stack (Selfhosting)

The "bunchloch" stack is our selfhosted infrastructure layer:

- **Komodo**: Container deployment and management
- **Pangolin**: VPN tunneling and reverse proxy with Traefik
- **LiteLLM**: LLM API gateway with multi-provider support
- **1Password Connect**: Secrets management
- **Dagger**: CI/CD pipelines

## Quick Links

- **Architecture Overview**: `00-overview/ARCHITECTURE.md`
- **Decision Matrices**: `00-overview/DECISION_MATRICES.md`
- **Selfhosting Guide**: `01-selfhosting/bunchloch.md`

## Archive

Original skill-specific research files are archived at:
`/research/archive/infrastructure-skills/`
