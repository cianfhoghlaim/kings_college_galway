# CocoIndex OpenAPI Specification Research Report

**Date:** 2025-11-22
**Status:** No Official OpenAPI Specification Found
**Researcher:** Claude Code Agent

## Executive Summary

After comprehensive research across official documentation, GitHub repositories, and web sources, **CocoIndex does not have an official OpenAPI/Swagger specification**. CocoIndex is primarily a Python/Rust framework library for data transformation rather than a traditional REST API service. It uses Python decorators and SDK patterns rather than HTTP REST endpoints for most operations.

## Research Methodology

The following sources were searched:
- Official CocoIndex documentation at cocoindex.io
- GitHub repository (cocoindex-io/cocoindex)
- PyPI package repository
- Integration documentation (Qdrant)
- Web search for OpenAPI/Swagger files
- HTTP server documentation

## Key Findings

### 1. Official OpenAPI Specification: NO

**Result:** No official OpenAPI/Swagger specification exists for CocoIndex.

**Evidence:**
- No `openapi.json`, `openapi.yaml`, or `swagger.json` files found in the GitHub repository
- No OpenAPI documentation in the official docs at https://cocoindex.io/docs/
- No API specification files in the PyPI package
- GitHub searches for spec files returned no results

### 2. Architecture Type

CocoIndex is a **framework library**, not a REST API service:

- **Core Technology:** Python SDK with Rust-based performance-critical components
- **Primary Interface:** Python decorators and programmatic API
- **Installation:** `pip install cocoindex`
- **Usage Pattern:** Import as a library, not HTTP client-server model

### 3. HTTP Server Component (Limited)

CocoIndex does include a minimal HTTP server component, but it's not the primary interface:

**Base URL:** Configurable (e.g., `http://localhost:PORT`)

**Available Endpoints:**

#### Health Check Endpoint
```
GET /healthz
```
**Response:**
```json
{
  "status": "ok",
  "version": "0.3.5"
}
```

#### Internal API
```
/cocoindex/api/*
```
**Note:** The documentation explicitly states: "The internal API is mainly designed for CocoInsight to use today, is subject to change and not considered as stable."

**Server Command:**
```bash
python main.py cocoindex server -c https://cocoindex.io
```

### 4. Primary API: Python SDK

CocoIndex's main interface is through its Python SDK:

#### Core Decorators

**Transform Flow:**
```python
@cocoindex.transform_flow()
def text_to_embedding(text: cocoindex.DataSlice[str]) -> cocoindex.DataSlice[NDArray]:
    # Implementation
    pass
```

**Query Handler:**
```python
@flow.query_handler(name="semantic_search")
def handle_query(query_input):
    return cocoindex.QueryOutput(results=[...], query_info=...)
```

**Flow Definition:**
```python
@cocoindex.flow_def
def build_flow(flow_builder: FlowBuilder, data_scope: DataScope):
    # Add sources
    flow_builder.add_source(cocoindex.sources.LocalFile(path="data"))

    # Add transformations
    # Add storage targets
```

#### Key SDK Components

1. **Sources:**
   - `cocoindex.sources.LocalFile(path="...")`
   - Integration with various data sources

2. **Functions:**
   - `cocoindex.functions.SplitRecursively()` - text chunking
   - Custom transformation functions

3. **Storages/Targets:**
   - `cocoindex.storages.Qdrant()` - Vector database integration
   - Custom export targets
   - Multiple storage backend support

4. **Query Support:**
   - Integrated within indexing flows
   - Python-based query handlers
   - Not exposed as separate HTTP endpoints

### 5. API Capabilities

Based on the documentation, CocoIndex provides:

**Data Processing:**
- Real-time incremental data processing
- Data lineage tracking
- Dataflow programming model

**Transformations:**
- Text chunking and splitting
- Embedding generation
- Custom transformations via Python functions

**Integrations:**
- Vector databases (Qdrant, others)
- Knowledge graph systems (Kuzu)
- PostgreSQL for incremental processing
- Custom target systems

**Features:**
- Incremental updates
- Real-time data synchronization
- Observable transformations
- Type-safe data slices

### 6. Use Cases

CocoIndex is designed for:
- Building RAG (Retrieval-Augmented Generation) pipelines
- Creating knowledge graphs
- Semantic search indexing
- Codebase indexing for AI
- Custom data transformations for AI applications

## Feasibility of Generating OpenAPI Specification

**Feasibility: LOW**

**Reasons:**
1. **Not REST-based:** CocoIndex is primarily a library/framework, not a REST API service
2. **Limited HTTP endpoints:** Only a health check and unstable internal API exist
3. **Python SDK is primary interface:** The SDK uses decorators and programmatic patterns that don't map to REST
4. **Architecture mismatch:** OpenAPI is designed for HTTP APIs, but CocoIndex's architecture is function-based

**Potential Approach:**
If an HTTP wrapper were needed, one could:
- Wrap the Python SDK in a FastAPI or Flask application
- Expose flow execution and query endpoints
- Document those endpoints with OpenAPI annotations
- This would be a custom implementation, not official

## Official Resources

### Documentation
- **Main Documentation:** https://cocoindex.io/docs/
- **Overview:** https://cocoindex.io/docs/
- **Quickstart Guide:** https://cocoindex.io/docs/getting_started/quickstart
- **HTTP Server Docs:** https://cocoindex.io/docs/http_server
- **Query Documentation:** https://cocoindex.io/docs/query
- **Installation:** https://cocoindex.io/docs/getting_started/installation

### Code Repositories
- **Main Repository:** https://github.com/cocoindex-io/cocoindex
- **Organization:** https://github.com/cocoindex-io
- **Quickstart Repo:** https://github.com/cocoindex-io/cocoindex-quickstart
- **Document AI Example:** https://github.com/cocoindex-io/cocoindex-etl-with-document-ai

### Package Managers
- **PyPI:** https://pypi.org/project/cocoindex/
- **Installation Command:** `pip install -U cocoindex`

### Integration Documentation
- **Qdrant Integration:** https://qdrant.tech/documentation/data-management/cocoindex/

### Blog & Tutorials
- **Official Blog:** https://cocoindex.io/blogs/
- **Medium:** https://medium.com/@cocoindex.io
- **Substack:** https://cocoindexio.substack.com/

### Community
- **Twitter/X:** https://x.com/cocoindex_io
- **LinkedIn:** https://www.linkedin.com/company/cocoindex
- **Hacker News Discussion:** https://news.ycombinator.com/item?id=43772582

## Recommendations

1. **For Integration:**
   - Use the Python SDK (`pip install cocoindex`)
   - Follow the quickstart guide at https://cocoindex.io/docs/getting_started/quickstart
   - Reference examples in the GitHub repository

2. **For REST API Needs:**
   - The HTTP server component is minimal and unstable
   - Consider building a custom wrapper using FastAPI if REST interface is required
   - Document any custom endpoints with OpenAPI annotations

3. **For Documentation:**
   - Refer to the official Python SDK documentation
   - The codebase is open source (Apache 2.0) for detailed implementation reference
   - Check integration examples for specific use cases

## Conclusion

CocoIndex does not provide an official OpenAPI specification because it is fundamentally a Python/Rust framework library rather than a REST API service. The project's architecture centers on programmatic SDK usage through Python decorators and function composition. While a minimal HTTP server exists for the CocoInsight UI, it's not the primary interface and lacks comprehensive REST API documentation.

For projects requiring integration with CocoIndex, the recommended approach is to use the Python SDK directly rather than expecting REST API endpoints. If REST API access is essential, a custom wrapper service would need to be developed.

## Research Sources

1. [CocoIndex - Qdrant Documentation](https://qdrant.tech/documentation/data-management/cocoindex/)
2. [GitHub - cocoindex-io/cocoindex](https://github.com/cocoindex-io/cocoindex)
3. [CocoIndex Official Documentation](https://cocoindex.io/docs/)
4. [CocoIndex Homepage](https://cocoindex.io/)
5. [CocoIndex - PyPI](https://pypi.org/project/cocoindex/)
6. [CocoIndex HTTP Server Documentation](https://cocoindex.io/docs/http_server)
7. [CocoIndex Query Documentation](https://cocoindex.io/docs/query)
8. [CocoIndex Quickstart Guide](https://cocoindex.io/docs/getting_started/quickstart)
9. [Swagger OpenAPI Specification](https://swagger.io/specification/)
10. [CocoIndex Medium Blog](https://medium.com/@cocoindex.io/cocoindex-a-data-indexing-platform-for-ai-application-4d6a1fb3cbb2)

---

**Last Updated:** 2025-11-22
**Confidence Level:** High
**Verification Status:** Comprehensive research completed across multiple official sources
