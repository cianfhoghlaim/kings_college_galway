# Dagster API Quick Reference

**Last Updated:** 2025-11-22

---

## Quick Answer: Does Dagster Have an OpenAPI Spec?

**NO** - Dagster does not provide an official OpenAPI/Swagger specification.

**Instead, use:**
- **GraphQL API** (primary) - Comprehensive programmatic access
- **External Assets REST API** (limited) - 3 endpoints for external asset management
- **Python SDK** (recommended) - Type-safe native Python access

---

## GraphQL API (Primary Interface)

### Endpoint
```
http://localhost:3000/graphql          # Local development
https://org.dagster.cloud/prod/graphql  # Dagster Cloud
```

### Interactive Playground
Navigate to `/graphql` in your browser for:
- Interactive query builder
- Built-in schema documentation
- Query testing and debugging

### Schema Introspection
```bash
# Using get-graphql-schema
npx get-graphql-schema http://localhost:3000/graphql > schema.graphql

# Using gql-sdl
npx gql-sdl http://localhost:3000/graphql

# Using Apollo CLI
npx apollo client:download-schema --endpoint=localhost:3000/graphql schema.json
```

### Python Client
```python
from dagster_graphql import DagsterGraphQLClient

client = DagsterGraphQLClient("localhost", port_number=3000)
```

**Docs:** https://docs.dagster.io/api/graphql

---

## External Assets REST API (Limited Scope)

### Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/report_asset_materialization/{ASSET_KEY}` | POST | Report asset materialization |
| `/report_asset_check/{ASSET_KEY}` | POST | Report asset check result |
| `/report_asset_observation/{ASSET_KEY}` | POST | Report asset observation |

### Authentication (Dagster Cloud)
```bash
curl -X POST \
  -H "Dagster-Cloud-Api-Token: YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  https://org.dagster.cloud/deployment/report_asset_materialization/my_asset
```

### Local Example
```bash
curl -X POST localhost:3000/report_asset_materialization/my_asset \
  -H "Content-Type: application/json" \
  -d '{
    "metadata": {"key": "value"},
    "data_version": "v1.0",
    "description": "Asset updated"
  }'
```

**Docs:** https://docs.dagster.io/api/rest-apis/external-assets-rest-api

---

## CLI-based API (`dg api`)

```bash
# List deployments
dg api deployments list

# Manage runs
dg api runs list
dg api runs events <run-id>

# Manage schedules
dg api schedules list

# Manage secrets
dg api secrets list
```

**Docs:** https://docs.dagster.io/api/clis/dg-cli/dg-api

---

## Converting GraphQL to OpenAPI (If Required)

### Option 1: graphql-to-openapi
```bash
npx graphql-to-openapi --yaml --schema schema.graphql --query query.graphql
```
**GitHub:** https://github.com/schwer/graphql-to-openapi

### Option 2: graph-to-openapi
```javascript
import { getOpenAPISpec } from '@thoughtspot/gql-to-openapi';

const { spec } = getOpenAPISpec({
  schema,
  info: {},
  basePath: '/api/v1',
});
```
**GitHub:** https://github.com/thoughtspot/graph-to-openapi

**Note:** Conversion has limitations; GraphQL features don't map perfectly to REST/OpenAPI.

---

## Common Use Cases

### 1. Trigger a Job Run
**Use:** GraphQL API or Python SDK
```python
from dagster_graphql import DagsterGraphQLClient

client = DagsterGraphQLClient("localhost", port_number=3000)
# Use GraphQL mutations to launch runs
```

### 2. Query Run Status
**Use:** GraphQL API
```graphql
query {
  runOrError(runId: "your-run-id") {
    ... on Run {
      status
      stats {
        stepsFailed
        stepsSucceeded
      }
    }
  }
}
```

### 3. Report External Asset Update
**Use:** External Assets REST API
```bash
curl -X POST localhost:3000/report_asset_materialization/my_external_asset \
  -H "Content-Type: application/json" \
  -d '{"data_version": "2025-11-22T10:00:00Z"}'
```

### 4. Get Job Metadata
**Use:** GraphQL API or Python SDK
```graphql
query {
  repositoryOrError(repositorySelector: {...}) {
    ... on Repository {
      pipelines {
        name
        description
      }
    }
  }
}
```

---

## Key Limitations

1. **No OpenAPI Spec** - Must use GraphQL or manually document REST endpoints
2. **GraphQL API is Evolving** - Subject to breaking changes (check release notes)
3. **Limited REST API** - Only 3 endpoints for external assets
4. **Internal API Portions** - Some GraphQL fields exist for internal webserver use

---

## Best Practices

### ✅ DO
- Use GraphQL API for comprehensive programmatic access
- Use Python SDK for Python-native integrations
- Use External Assets REST API for lightweight external integrations
- Explore GraphQL Playground for interactive documentation
- Use introspection to discover available queries/mutations
- Pin Dagster versions in production
- Check release notes for breaking changes

### ❌ DON'T
- Expect traditional REST API with OpenAPI spec
- Rely on undocumented internal GraphQL fields
- Convert GraphQL to OpenAPI without understanding limitations
- Use External Assets REST API for job execution (use GraphQL instead)

---

## When to Use Each API

| Use Case | Recommended API |
|----------|----------------|
| Run job executions | GraphQL API |
| Query run status/history | GraphQL API |
| Access job/op metadata | GraphQL API |
| Report external asset updates | External Assets REST API |
| Python-native integrations | Python SDK |
| CLI automation | `dg api` commands |
| Interactive exploration | GraphQL Playground |

---

## Documentation Links

- **Main API Reference:** https://docs.dagster.io/api
- **GraphQL API:** https://docs.dagster.io/api/graphql
- **GraphQL Python Client:** https://docs.dagster.io/concepts/webserver/graphql-client
- **External Assets REST API:** https://docs.dagster.io/api/rest-apis/external-assets-rest-api
- **dagster-graphql Library:** https://docs.dagster.io/api/libraries/dagster-graphql
- **dg CLI Reference:** https://docs.dagster.io/api/clis/dg-cli/dg-api
- **GitHub Repository:** https://github.com/dagster-io/dagster

---

## TL;DR

- ❌ **No OpenAPI spec exists**
- ✅ **GraphQL API is primary interface** - Use for comprehensive access
- ✅ **Limited REST API exists** - 3 endpoints for external asset management
- ✅ **Python SDK recommended** - For Python-native development
- ⚠️ **GraphQL API is evolving** - Check release notes for breaking changes
- 🔧 **Can convert GraphQL→OpenAPI** - Using tools, but with limitations

**For most use cases, use the GraphQL API or Python SDK directly rather than trying to generate an OpenAPI specification.**
