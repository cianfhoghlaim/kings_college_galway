# Pangolin OpenAPI Specification Research Report

## Executive Summary

**Does an official OpenAPI spec exist?** ✅ **YES**

Pangolin (fosrl/pangolin) provides an official OpenAPI 3.x specification for its Integration API, accessible via Swagger UI. The specification is dynamically generated at build time and serves comprehensive REST API documentation for all Pangolin operations.

---

## OpenAPI Specification Details

### Location & Access

**Official Public API (Cloud Service):**
- **Swagger UI**: https://api.pangolin.net/v1/docs
- **OpenAPI JSON**: https://api.pangolin.net/v1/openapi.json (likely endpoint)
- **Base URL**: https://api.pangolin.net/v1

**Self-Hosted Deployment:**
- **Swagger UI**: https://api.{your-domain}/v1/docs
- **Configuration Required**: Set `enable_integration_api: true` in `config.yml`
- **Default Port**: 3003 (customizable)
- **Traefik Routing**: Required to expose API externally

### Version Information

**Current Version**: v1 (path: `/v1`)
**Integration API Introduced**: v1.3.0 (October 2024)
**Generally Available**: v1.4.0 (November 2024)
**Latest Pangolin Version**: v1.4.0+

**Key Releases:**
- **v1.3.0**: Added external integration REST API with Swagger UI docs
- **v1.4.0**: Made Integration API available to all users (previously limited), added auto-provisioning IdP users and scoped API key permissions

### Specification Type

- **Standard**: OpenAPI 3.x (Swagger)
- **Format**: JSON (YAML may also be available)
- **Generation**: Dynamically generated at build time
- **Source Control**: NOT committed to repository (in `.gitignore` as `config/openapi.yaml`)
- **Repository**: https://github.com/fosrl/pangolin
- **Documentation Repo**: https://github.com/fosrl/docs-v2

---

## API Coverage

### Capabilities Documented

The Integration API provides programmatic access to all Pangolin functionality:

#### 1. Configuration Management
- Traefik dynamic configuration retrieval (`GET /api/v1/traefik-config`)
- Gerbil VPN configuration (`GET /api/v1/gerbil/get-config`)
- System health checks (`GET /api/v1/`)

#### 2. Network & VPN Operations
- WireGuard tunnel management
- Agent bandwidth reporting (`POST /api/v1/gerbil/receive-bandwidth`)
- Reverse proxy routing configuration
- Access control rules (public/private endpoints)

#### 3. Identity & Access Control
- User and organization management
- Role-based access control (`GET /org/{orgId}/roles`)
- API key management with scoped permissions
- OAuth/OIDC integration endpoints

#### 4. Monitoring & Observability
- Service health status
- Bandwidth usage metrics
- Agent connectivity status
- Container discovery (Docker socket integration)

### Authentication

**Method**: Bearer Token (API Keys)
**Header**: `Authorization: Bearer <API_KEY>`
**Security Scheme**: Defined in OpenAPI spec
**Scoped Permissions**: API keys can be created with specific operation scopes

### API Design Patterns

- **RESTful**: Standard HTTP methods (GET, POST, PUT, DELETE)
- **Versioned**: Path-based versioning (`/v1`)
- **JSON**: Request/response format
- **Error Handling**: Standard HTTP status codes
- **Documentation**: Interactive Swagger UI with try-it-out functionality

---

## Generating TypeScript Clients

### Recommended Tools

Based on existing research in `/home/user/hackathon/research/web/generating-typescript-client-pangolin-api-openapi-spec.md`:

#### 1. swagger-typescript-api (Recommended)
```bash
# Install
npm install --save-dev swagger-typescript-api

# Generate client
npx swagger-typescript-api generate \
  --path openapi/pangolin-api.json \
  --output src/pangolinClient/ \
  --name PangolinApi.ts \
  --api-class-name PangolinApi \
  --axios
```

**Benefits:**
- Pure Node.js tool (no Java required)
- Supports Fetch API and Axios
- Modular output option
- Works well with Bun and Node.js 18+

#### 2. openapi-typescript-codegen
```bash
npx openapi-typescript-codegen \
  --input pangolin-api.json \
  --output src/pangolinClient \
  --client fetch
```

**Benefits:**
- Lightweight and framework-agnostic
- Multiple HTTP client options (fetch, axios, node-fetch)
- Separate files for models, services, and core utilities

#### 3. OpenAPI Generator (Java-based)
```bash
openapi-generator-cli generate \
  -i pangolin-api.json \
  -g typescript-axios \
  -o ./src/pangolinClient
```

**Benefits:**
- Very robust, supports OpenAPI 3.x
- Multiple generator options (typescript-fetch, typescript-axios)
- Extensive configuration options
- Requires Java runtime

### Obtaining the Spec

**From Swagger UI:**
1. Navigate to https://api.pangolin.net/v1/docs
2. Use browser dev tools to find network request to `.json` endpoint
3. Download or save the OpenAPI JSON specification

**From Self-Hosted Instance:**
1. Enable Integration API in `config.yml`
2. Access Swagger UI at `https://api.{your-domain}/v1/docs`
3. Retrieve spec from your instance's endpoint

**Not Available:**
- No static OpenAPI file in GitHub repository
- Spec must be obtained from running instance

---

## Self-Hosting Configuration

### Enable Integration API

**Edit `config.yml`:**
```yaml
flags:
  enable_integration_api: true

server:
  integration_api_port: 3003  # Optional: customize port
```

### Expose via Traefik

**Edit `dynamic_config.yml`:**
```yaml
http:
  routers:
    pangolin-api:
      rule: "Host(`api.example.com`)"
      service: pangolin-integration-api
      entryPoints:
        - websecure
      tls:
        certResolver: letsencrypt

  services:
    pangolin-integration-api:
      loadBalancer:
        servers:
          - url: "http://pangolin:3003"
```

### Docker Compose Example

Located at `/home/user/hackathon/infrastructure/compose/pangolin/`:

```yaml
services:
  pangolin:
    image: fosrl/pangolin:v1.4.0
    ports:
      - "3001:3001"  # Management API
      - "3003:3003"  # Integration API
    volumes:
      - ./config:/config
    environment:
      - ENABLE_INTEGRATION_API=true
```

---

## API Client Usage Example

### TypeScript Client Initialization

```typescript
import { PangolinApi } from './pangolinClient/PangolinApi'

// Initialize client with authentication
const pangolinApi = new PangolinApi({
  baseURL: "https://api.pangolin.net/v1",
  headers: {
    Authorization: `Bearer ${process.env.PANGOLIN_API_KEY}`
  }
})

// Example: List organization roles
const orgId = "your-org-id"
const response = await pangolinApi.getOrgRoles({ orgId })
console.log("Roles:", response.data)
```

### Environment Variables

```bash
# .env file
PANGOLIN_API_KEY=your-api-key-here
PANGOLIN_BASE_URL=https://api.pangolin.net/v1
```

---

## Integration with Hackathon Project

### Current Usage in Project

The hackathon project uses Pangolin for:

1. **Network Infrastructure** (`/home/user/hackathon/infrastructure/compose/pangolin/`)
   - WireGuard VPN tunneling
   - Reverse proxy with Traefik
   - Identity-aware access control

2. **Docker Compose Stack** (`/home/user/hackathon/poc/oracle-komodo-pangolin-dagster/`)
   - Integration with Komodo infrastructure manager
   - Dagster orchestration
   - Newt client for VPN connectivity

3. **Research Documentation** (`/home/user/hackathon/research/`)
   - Architecture patterns
   - Deployment guides
   - Integration strategies

### Potential Uses for OpenAPI Spec

1. **Automated Infrastructure Management**
   - Generate TypeScript SDK for Pangolin API
   - Integrate with Komodo deployment scripts
   - Programmatic VPN configuration

2. **CI/CD Integration**
   - Automated service registration
   - Dynamic routing configuration
   - Health check monitoring

3. **Dashboard Development**
   - Build custom UI for Pangolin management
   - Real-time service monitoring
   - User access control interface

---

## Documented Limitations & Gotchas

### Known Issues

1. **Spec Accuracy**: GitHub issue #1339 mentions a minor documentation typo in Swagger docs
2. **Generation**: OpenAPI spec is generated at build time, not version-controlled
3. **Access**: No static spec file available; must retrieve from running instance

### Recommendations

1. **Version Control Spec**: Download and commit OpenAPI JSON to your repo for stability
2. **Automate Regeneration**: Set up periodic spec updates via CI/CD
3. **Use Node Tools**: Prefer `swagger-typescript-api` over Java-based generators for Node/Bun projects
4. **Environment-Based Config**: Never hardcode API keys; use environment variables

---

## Feasibility Assessment

### Can We Generate a TypeScript Client?

✅ **YES - Highly Feasible**

**Reasons:**
- Official OpenAPI 3.x specification exists
- Well-documented Swagger UI available
- Multiple proven TypeScript code generation tools
- Existing documentation in project demonstrates feasibility
- Active community and support

### Recommended Approach

1. **Obtain Spec**: Download from `https://api.pangolin.net/v1/docs`
2. **Choose Tool**: Use `swagger-typescript-api` for Node.js compatibility
3. **Generate Client**: Create TypeScript SDK with type-safe API methods
4. **Version Control**: Commit both spec and generated client
5. **Automate Updates**: Set up workflow to regenerate on spec changes

### Integration Timeline

- **Immediate**: Download OpenAPI spec (5 minutes)
- **Short-term**: Generate TypeScript client (30 minutes)
- **Medium-term**: Integrate with deployment scripts (2-4 hours)
- **Long-term**: Build custom management dashboard (1-2 days)

---

## Additional Resources

### Official Documentation

- **Main Docs**: https://docs.pangolin.net
- **Integration API Guide**: https://docs.pangolin.net/self-host/advanced/integration-api
- **GitHub Repository**: https://github.com/fosrl/pangolin
- **Releases**: https://github.com/fosrl/pangolin/releases

### Community Resources

- **Discussions**: https://github.com/orgs/fosrl/discussions
- **Discord/Slack**: Active community channels
- **Cloud Service**: https://app.pangolin.net (free tier available)

### Related Project Documentation

- `/home/user/hackathon/research/web/generating-typescript-client-pangolin-api-openapi-spec.md`
- `/home/user/hackathon/research/web/pangolin-patterns.md`
- `/home/user/hackathon/research/llms-txt/web/pangolin-llms.txt`
- `/home/user/hackathon/.claude/commands/pangolin.md`

---

## Conclusion

Pangolin provides a comprehensive, well-documented OpenAPI specification for its Integration API. The specification is production-ready, actively maintained, and suitable for generating type-safe TypeScript clients. The existing research in this project demonstrates successful client generation patterns, making this a low-risk, high-value integration opportunity.

**Next Steps:**
1. Download OpenAPI spec from Swagger UI
2. Generate TypeScript client using `swagger-typescript-api`
3. Integrate with existing infrastructure automation
4. Consider building custom management tools

---

**Report Generated**: 2025-11-22
**Pangolin Version Researched**: v1.4.0+
**OpenAPI Version**: 3.x
**Research Status**: ✅ Complete
