# Dagster OpenAPI Specification Research

**Date:** 2025-11-22
**Status:** Complete
**Research Focus:** Official Dagster OpenAPI/Swagger specifications

---

## Executive Summary

**Does an Official OpenAPI Spec Exist?** **NO**

Dagster does not provide an official OpenAPI/Swagger specification for its APIs. Instead, Dagster primarily exposes a **GraphQL API** as its main programmatic interface, along with a limited **REST API** specifically for external asset management.

---

## API Architecture Overview

### Primary API: GraphQL

Dagster's main programmatic interface is a GraphQL API that provides comprehensive access to Dagster's functionality:

- **Endpoint:** `/graphql` (e.g., `http://localhost:3000/graphql` for local development)
- **Interactive Playground:** Available at the `/graphql` endpoint with built-in documentation
- **Schema Location:** `./js_modules/dagster-ui/packages/ui-core/src/graphql/schema.graphql` in the repository
- **Status:** Evolving and subject to breaking changes (documented in release notes)

**Key Capabilities:**
- Query information about Dagster runs (historical and currently executing)
- Launch or terminate job executions
- Access metadata about repositories, jobs, and ops
- Retrieve dependency structures and configuration schemas
- Trigger custom events

**Documentation:**
- GraphQL API Docs: https://docs.dagster.io/api/graphql
- GraphQL Python Client: https://docs.dagster.io/concepts/webserver/graphql-client
- dagster-graphql Library: https://docs.dagster.io/api/libraries/dagster-graphql

### Secondary API: External Assets REST API

Dagster provides a limited REST API specifically for external asset management:

**Base URL Format:**
- **Local:** `http://localhost:3000/`
- **Dagster Cloud:** `https://{ORGANIZATION}.dagster.cloud/{DEPLOYMENT_NAME}/`

**Authentication:**
- Header: `Dagster-Cloud-Api-Token` (for Dagster Cloud/Plus)
- Token Type: User Token (not Agent Token)

**Available Endpoints:**

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/report_asset_materialization/{ASSET_KEY}` | POST | Records an AssetMaterialization event for an external asset |
| `/report_asset_check/{ASSET_KEY}` | POST | Records an asset check evaluation result |
| `/report_asset_observation/{ASSET_KEY}` | POST | Records an AssetObservation event |

**Request/Response Format:**
- **Content-Type:** `application/json`
- **Success Response (200):** `{}`
- **Error Response (400):** `{"error": "..."}`

**Common Parameters:**
- `asset_key` (required) - Asset identifier
- `metadata` (optional) - Key-value pairs about the asset
- `data_version` (optional) - Version tracking
- `description` (optional) - Human-readable explanation
- `partition` (optional) - Partition identifier
- `passed` (for checks) - Boolean check result
- `check_name` (for checks) - Name of the check

**Documentation:** https://docs.dagster.io/api/rest-apis/external-assets-rest-api

### CLI-based API: `dg api`

Dagster Plus offers REST-like API operations through the `dg api` CLI command:

**Capabilities:**
- Managing agents
- Listing deployments
- Managing runs
- Managing schedules
- Managing secrets (encrypted environment variables)

**Documentation:** https://docs.dagster.io/api/clis/dg-cli/dg-api

---

## Repository Analysis

### GitHub Repository: dagster-io/dagster

**URL:** https://github.com/dagster-io/dagster

**Search Results:**
- **No OpenAPI/Swagger files found** (no `openapi.yaml`, `swagger.json`, or `openapi.json`)
- **API Specification Files:**
  - `RUN_API_SPECIFICATION.md` - Documents the Dagster Plus Run Events API (GraphQL-based, not OpenAPI)
  - `RUN_API_IMPLEMENTATION_PLAN.md` - Implementation planning document
  - `.graphqlrc.yml` - GraphQL configuration file

**Note:** The REST API is documented through narrative documentation pages rather than machine-readable specification files.

---

## Generating an OpenAPI Specification

### Current State: Not Feasible Out-of-the-Box

Since Dagster does not provide an OpenAPI specification, you would need to:

1. **For the External Assets REST API:** Manually create an OpenAPI spec based on the documentation
2. **For the GraphQL API:** Convert the GraphQL schema to OpenAPI using conversion tools

### GraphQL Schema Extraction

The GraphQL schema can be obtained through introspection:

**Methods:**
1. **Using get-graphql-schema CLI:**
   ```bash
   npx get-graphql-schema http://localhost:3000/graphql > schema.graphql
   ```

2. **Using gql-sdl:**
   ```bash
   npx gql-sdl http://localhost:3000/graphql
   ```

3. **Using Apollo CLI:**
   ```bash
   npx apollo client:download-schema --endpoint=localhost:3000/graphql schema.json
   ```

4. **Direct Introspection Query:** Execute GraphQL introspection queries against the `/graphql` endpoint

**Schema Location in Repository:**
`./js_modules/dagster-ui/packages/ui-core/src/graphql/schema.graphql`

### GraphQL to OpenAPI Conversion Tools

If you need an OpenAPI specification, you can convert the GraphQL schema using these tools:

1. **graphql-to-openapi** (by schwer)
   - GitHub: https://github.com/schwer/graphql-to-openapi
   - NPM: https://www.npmjs.com/package/graphql-to-openapi
   - Usage:
     ```bash
     npx graphql-to-openapi --yaml --schema schema.graphql --query query.graphql
     ```

2. **graph-to-openapi** (by ThoughtSpot)
   - GitHub: https://github.com/thoughtspot/graph-to-openapi
   - Usage:
     ```javascript
     import { getOpenAPISpec } from '@thoughtspot/gql-to-openapi';
     const { spec } = getOpenAPISpec({
       schema,
       info: {},
       basePath: '/api/v1',
     });
     ```

**Limitations:**
- GraphQL and REST APIs have different paradigms; conversion may not capture all GraphQL features
- GraphQL's type system and query flexibility don't map perfectly to REST/OpenAPI
- Generated specs may require manual adjustments

---

## API Capabilities Summary

### What Dagster's APIs Can Do

**Via GraphQL API:**
- Launch and terminate job executions
- Query run status and history
- Access job, op, and repository metadata
- Retrieve asset information
- Execute custom queries and mutations
- Access dependency graphs
- Retrieve configuration schemas

**Via External Assets REST API:**
- Report asset materializations from external systems
- Record asset check results
- Log asset observations with metadata
- Integrate external data pipelines with Dagster

**Via Python SDK:**
- Comprehensive programmatic access to all Dagster functionality
- Direct Python API without HTTP overhead
- Type-safe interactions with Dagster constructs

### What Dagster's APIs Are NOT Designed For

- Traditional REST-based CRUD operations
- OpenAPI-driven API client generation
- Swagger UI exploration
- REST API documentation standards

---

## Documentation Resources

### Official Dagster Documentation

- **Main API Reference:** https://docs.dagster.io/api
- **GraphQL API:** https://docs.dagster.io/api/graphql
- **GraphQL Python Client:** https://docs.dagster.io/concepts/webserver/graphql-client
- **External Assets REST API:** https://docs.dagster.io/api/rest-apis/external-assets-rest-api
- **External Assets Instance API:** https://docs.dagster.io/api/dagster/external-assets-instance-api
- **dg CLI API Reference:** https://docs.dagster.io/api/clis/dg-cli/dg-api
- **Dagster SDK:** https://docs.dagster.io/api/dagster
- **dagster-graphql Library:** https://docs.dagster.io/api/libraries/dagster-graphql
- **Connecting to APIs Guide:** https://docs.dagster.io/guides/build/external-resources/connecting-to-apis
- **Dagster Webserver:** https://docs.dagster.io/guides/operate/webserver

### Community Resources

- **API Tracker:** https://apitracker.io/a/dagster-io (mentions OpenAPI/Swagger specs but doesn't link to actual files)
- **GitHub Discussions:**
  - Execute pipeline by API endpoint: https://github.com/dagster-io/dagster/discussions/4301
  - Using DagsterGraphQLClient with Dagster Cloud: https://github.com/dagster-io/dagster/discussions/7772
  - Custom GraphQL queries: https://github.com/dagster-io/dagster/discussions/24061
  - Asynchronous REST API consumption: https://github.com/dagster-io/dagster/discussions/18401

### Third-Party Integrations

- **Cube.dev Integration:** https://cube.dev/docs/product/apis-integrations/orchestration-api/dagster
- **dlt + Dagster:** https://dlthub.com/blog/multi-asset-rest-api-pipelines

---

## Recommendations

### For Programmatic Access

1. **Use the GraphQL API** for comprehensive access to Dagster functionality
2. **Use the Python Client** (`dagster-graphql`) for type-safe Python integrations
3. **Use the External Assets REST API** for reporting events from external systems
4. **Use the Python SDK** directly for the most comprehensive and type-safe access

### For API Documentation

1. Explore the **GraphQL Playground** at `/graphql` for interactive API documentation
2. Use **GraphQL introspection** to discover available queries and mutations
3. Refer to the **official Dagster documentation** for detailed API guides
4. Review the **GraphQL schema file** in the repository for schema details

### If OpenAPI Specification is Required

1. **For External Assets REST API:**
   - Manually create an OpenAPI spec based on the documentation
   - Limited scope (3 endpoints) makes manual creation feasible

2. **For GraphQL API:**
   - Extract the GraphQL schema using introspection
   - Convert to OpenAPI using tools like `graphql-to-openapi` or `graph-to-openapi`
   - Be aware of conversion limitations and expect manual adjustments
   - Consider whether OpenAPI is the right tool for a GraphQL API

### Alternative Approaches

Instead of converting GraphQL to OpenAPI, consider:
- Using GraphQL-native tools and clients
- Leveraging GraphQL's built-in introspection and type system
- Using the Dagster Python SDK for programmatic access
- Embracing GraphQL's flexibility rather than forcing REST paradigms

---

## Conclusion

Dagster does not provide an official OpenAPI/Swagger specification. The platform is built around a **GraphQL-first API design** with a limited REST API for external asset integration. This architectural choice reflects Dagster's focus on:

- **Flexible querying** through GraphQL's query language
- **Type-safe interactions** via GraphQL's type system
- **Python-native development** through decorators and the Python SDK
- **Interactive exploration** via GraphQL Playground

While it's technically possible to extract the GraphQL schema and convert it to OpenAPI, this approach goes against Dagster's design philosophy and may result in a suboptimal developer experience. For best results, use Dagster's APIs as designed:

- **GraphQL API** for comprehensive programmatic access
- **Python SDK** for Python-native development
- **External Assets REST API** for lightweight external integrations

---

## Sources

- [Dagster API Reference](https://docs.dagster.io/api)
- [Dagster GraphQL API Documentation](https://docs.dagster.io/api/graphql)
- [External Assets REST API](https://docs.dagster.io/api/rest-apis/external-assets-rest-api)
- [Dagster GitHub Repository](https://github.com/dagster-io/dagster)
- [Dagster GraphQL Python Client](https://docs.dagster.io/concepts/webserver/graphql-client)
- [dagster-graphql Library](https://docs.dagster.io/api/libraries/dagster-graphql)
- [Dagster Webserver Documentation](https://docs.dagster.io/guides/operate/webserver)
- [Dagster API Tracker](https://apitracker.io/a/dagster-io)
- [GraphQL Introspection](https://graphql.org/learn/introspection/)
- [graphql-to-openapi Tool](https://github.com/schwer/graphql-to-openapi)
- [graph-to-openapi Tool](https://github.com/thoughtspot/graph-to-openapi)

---

## Additional Context

### API Tracker Reference

While API Tracker (https://apitracker.io/a/dagster-io) lists "OpenAPI/Swagger specs" as a feature for Dagster, the actual search through Dagster's documentation and GitHub repository did not reveal any published OpenAPI specification files. This listing may be inaccurate or may refer to the theoretical possibility of generating such specs rather than their actual availability.

### GraphQL Evolution Notice

The Dagster documentation explicitly states: "The GraphQL API is still evolving and is subject to breaking changes." Some portions of the API exist primarily for internal webserver use. Users should:
- Check release notes for breaking changes
- Pin to specific Dagster versions in production
- Test GraphQL integrations thoroughly during upgrades
- Use the Python SDK for more stable integrations

### Python-First Design Philosophy

Dagster's design philosophy emphasizes Python-native development patterns over API-first approaches. The platform encourages:
- Declaring data assets as Python functions using decorators (`@dg.asset`)
- Using Python types for configuration and validation
- Leveraging Python's ecosystem for data processing
- Direct SDK usage over HTTP API calls

This philosophy explains why OpenAPI specifications are not a priority for the Dagster project.
