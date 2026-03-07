# Cognee OpenAPI Specification Research

**Research Date:** 2025-11-22
**Researcher:** Claude Code
**Status:** Complete

## Executive Summary

**Official OpenAPI Spec Exists:** ✅ YES

Cognee provides an official OpenAPI 3.1.0 specification accessible through their FastAPI-based backend. The specification is comprehensive and covers the complete knowledge graph lifecycle, including data ingestion, cognitive processing, search, visualization, and access control.

## OpenAPI Specification Details

### Location & Access

- **Primary URL:** https://api.cognee.ai/openapi.json
- **Interactive Swagger UI:** https://api.cognee.ai/docs
- **Alternative (Cloud):** https://cognee--cognee-saas-backend-serve.modal.run/docs
- **Local Development:** http://localhost:8000/openapi.json (when running locally via Docker)
- **OpenAPI Version:** 3.1.0
- **API Version:** 1.0.0

### Authentication

- **Method:** API Key authentication
- **Header:** `X-Api-Key`
- **Type:** JWT-based bearer tokens
- **Features:** User registration, login, password reset, email verification

### Coverage

The OpenAPI specification is comprehensive and production-ready, covering:

#### 1. **Core Data Management Endpoints**
- `POST /api/add` - Ingest text, documents, or structured data into datasets
- `POST /api/cognify` - Execute cognitive processing to build knowledge graphs
- `POST /api/search` - Perform semantic searches across the knowledge graph
- `DELETE /api/delete` - Remove data items from datasets
- `POST /api/visualize` - Generate interactive HTML graph visualizations

#### 2. **Dataset Operations**
- `POST /api/datasets/` - Create new datasets
- `GET /api/datasets/` - List all datasets
- `GET /api/datasets/{id}/data` - Access dataset contents and metadata
- `GET /api/datasets/status` - Monitor processing pipeline status
- Dataset management with UUID or name-based access

#### 3. **Access Control & Security**
- Role management endpoints
- Tenant management for multi-tenancy
- Permission assignment for dataset access
- Principal-based access control system

#### 4. **Additional Features**
- Notebook creation and management (`/api/notebooks/`)
- Search history tracking
- Interactive visualizations

#### 5. **Processing Pipeline Capabilities**
- Document classification
- Text chunking
- Entity extraction
- Relationship detection
- Vector embedding generation
- Content summarization

#### 6. **Search Types (15 Modes)**
The API supports 15 different search modes:
- `GRAPH_COMPLETION` - LLM-powered responses with graph context
- `CHUNKS` - Raw text segments matching queries
- `SUMMARIES` - Pre-generated hierarchical summaries
- `INSIGHTS` - Structured entity relationships
- `CODE` - Code-specific search with syntax understanding
- Semantic search
- RAG completion
- Temporal queries
- And more...

### Data Models

The specification includes comprehensive DTOs (Data Transfer Objects) for:
- Request payloads
- Dataset metadata
- Search results
- Validation error handling
- Entity and relationship schemas

## Documentation Resources

### Official Documentation
- **Main Docs:** https://docs.cognee.ai/
- **API Reference:** https://docs.cognee.ai/api-reference/introduction
- **REST API Server Guide:** https://docs.cognee.ai/how-to-guides/cognee-sdk/rest-api-server
- **User Authentication:** https://docs.cognee.ai/reference/user-authentication

### GitHub Repository
- **Main Repository:** https://github.com/topoteretes/cognee
- **Description:** "Memory for AI Agents in 6 lines of code"
- **Language:** Python (FastAPI-based)
- **License:** Open Source
- **Community Repos:**
  - Starter Kit: https://github.com/topoteretes/cognee-starter
  - Community Plugins: https://github.com/topoteretes/cognee-community
  - n8n Integration: https://github.com/topoteretes/cognee-n8n

### Additional Resources
- **NPM Package:** @lineai/cognee-api
- **FalkorDB Integration:** https://docs.falkordb.com/agentic-memory/cognee.html
- **Redis Integration:** https://redis.io/blog/build-faster-ai-memory-with-cognee-and-redis/

## Deployment Options

### 1. Managed Cloud Platform
- **URL:** https://api.cognee.ai
- **Base URL (Alternative):** https://cognee--cognee-saas-backend-serve.modal.run
- **Features:**
  - Production-ready
  - Fully managed service
  - Automatic scaling
  - 99.9% uptime SLA
  - Enterprise features

### 2. Self-Hosted Development
- **Setup:** Docker Compose
- **Command:** `docker compose --profile postgres up -d`
- **Local URL:** http://localhost:8000
- **Swagger UI:** http://localhost:8000/docs
- **OpenAPI Spec:** http://localhost:8000/openapi.json

## Technical Stack

- **Framework:** FastAPI (Python)
- **API Style:** RESTful
- **Documentation:** Auto-generated OpenAPI 3.1.0 spec
- **UI Tools:**
  - Swagger UI (at `/docs`)
  - ReDoc (likely available at `/redoc`)
- **Database:** PostgreSQL (in Docker setup)
- **Vector Search:** Supports multiple backends
- **Graph Database:** Compatible with FalkorDB and others

## API Request/Response Patterns

### Request Parameters
- Dataset identification (name or UUID)
- Custom prompts for search/processing
- Filtering by node types
- Result limits (default: 10)
- Search mode selection

### Response Formats
- Structured JSON responses
- Interactive HTML visualizations
- Search results with metadata
- Error handling with validation details

## Use Cases Covered by API

Based on the OpenAPI specification and documentation:

1. **Knowledge Graph Construction** - Transform unstructured data into structured graphs
2. **Semantic Search** - Natural language queries across knowledge bases
3. **AI Agent Memory** - Persistent memory for AI applications
4. **Code Analysis** - Code graph pipeline for software understanding
5. **Multi-tenant Systems** - Role-based access control for datasets
6. **Interactive Exploration** - Notebook-style interfaces for data exploration
7. **Relationship Discovery** - Entity and relationship extraction

## OpenAPI Specification Quality Assessment

### Strengths ✅
- **Comprehensive:** Covers all core functionality
- **Well-Structured:** Clear endpoint organization
- **Versioned:** Proper API versioning (v1)
- **Interactive:** Swagger UI for testing
- **Auto-Generated:** FastAPI ensures spec stays in sync with code
- **Production-Ready:** Used in live cloud deployment
- **Multi-Environment:** Works for both cloud and self-hosted

### Completeness ✅
- All CRUD operations documented
- Authentication schemes defined
- Error responses specified
- Request/response schemas included
- Query parameters documented
- Path parameters specified

## Feasibility of Generating OpenAPI Spec

**Not Necessary** - An official, comprehensive OpenAPI 3.1.0 specification already exists and is actively maintained.

The specification is:
- ✅ Auto-generated by FastAPI framework
- ✅ Always in sync with the actual API implementation
- ✅ Publicly accessible
- ✅ Production-ready
- ✅ Interactive (Swagger UI)
- ✅ Comprehensive (covers all endpoints)

## Recommendations

### For API Consumers
1. **Use the Official Spec:** Download from https://api.cognee.ai/openapi.json
2. **Interactive Testing:** Use Swagger UI at https://api.cognee.ai/docs
3. **Code Generation:** Use the OpenAPI spec with tools like:
   - `openapi-generator` for client SDKs
   - `swagger-codegen` for multiple languages
   - Language-specific tools (e.g., `openapi-python-client`)

### For Integration
1. **Python:** Use the official `cognee` SDK or generate from OpenAPI spec
2. **JavaScript/TypeScript:** Use `@lineai/cognee-api` npm package or generate client
3. **Other Languages:** Generate clients from the OpenAPI specification
4. **n8n:** Use the official cognee-n8n integration

### For Development
1. **Local Testing:** Run via Docker and access http://localhost:8000/docs
2. **API Exploration:** Use Swagger UI for interactive testing
3. **Documentation:** Reference https://docs.cognee.ai for guides and examples

## Conclusion

Cognee provides a **comprehensive, production-ready OpenAPI 3.1.0 specification** that fully documents their knowledge graph API. The specification is:

- ✅ Officially maintained
- ✅ Auto-generated and always up-to-date
- ✅ Publicly accessible
- ✅ Interactive via Swagger UI
- ✅ Comprehensive in coverage
- ✅ Available for both cloud and self-hosted deployments

**No custom OpenAPI generation is needed** - the official specification should be used directly for all integration, testing, and client generation purposes.

## Sources

- [Cognee API Documentation](https://docs.cognee.ai/api-reference/introduction)
- [Cognee Official Website](https://www.cognee.ai/)
- [Cognee GitHub Repository](https://github.com/topoteretes/cognee)
- [REST API Server Documentation](https://docs.cognee.ai/how-to-guides/cognee-sdk/rest-api-server)
- [OpenAPI Specification](https://api.cognee.ai/openapi.json)
- [Swagger UI Interface](https://api.cognee.ai/docs)
- [Cognee Starter Kit](https://github.com/topoteretes/cognee-starter)
- [FalkorDB Cognee Integration](https://docs.falkordb.com/agentic-memory/cognee.html)
- [Redis Cognee Integration](https://redis.io/blog/build-faster-ai-memory-with-cognee-and-redis/)

---

**Research Completed:** 2025-11-22
**Last Verified:** API accessible and OpenAPI spec retrieved successfully
**Next Steps:** Use the official OpenAPI specification for integration or client generation as needed
