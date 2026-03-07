# Gradio OpenAPI Specification Research

**Research Date:** 2025-11-22
**Subject:** OpenAPI/Swagger Specification Support in Gradio
**Status:** Complete

---

## Executive Summary

**Does Gradio have an official OpenAPI specification?**
**YES** - Gradio automatically generates an OpenAPI v3 specification for every Gradio application, accessible at the endpoint `<your-gradio-app-url>/gradio_api/openapi.json`.

**Key Finding:** Gradio both **produces** OpenAPI specifications (for its own apps) and **consumes** OpenAPI specifications (via `gr.load_openapi` to generate UIs from external APIs).

---

## 1. Official OpenAPI Specification Access

### Endpoint Location
Every Gradio application automatically exposes its OpenAPI specification at:
```
https://<your-gradio-app-url>/gradio_api/openapi.json
```

### Specification Details
- **Format:** OpenAPI v3 (JSON only)
- **Generation:** Automatically generated from Gradio app function signatures
- **Coverage:** Includes all API endpoints, parameters, types, and example inputs
- **Technology Stack:** Built on FastAPI + Pydantic + Swagger

### Implementation Timeline
- **Issue #672** (Expose inputs/outputs in openapi.json): Filed requesting machine-readable API format
- **PR #11103**: Merged - Implemented OpenAPI specification exposure
- **Status:** Feature is live and fully implemented

---

## 2. API Documentation Features

### Built-in API Documentation Page
Every Gradio app includes an interactive API documentation page accessible via:
- **Location:** "Use via API" link in the app footer
- **Access:** `<your-gradio-app-url>?view=api`

### Documentation Capabilities
The API page provides:
1. **Endpoint Discovery:** Automatically generated API endpoint names based on function names
2. **Code Snippets:** Complete examples for both Python and JavaScript clients
3. **Parameter Details:** Types, example inputs, and usage instructions
4. **API Recorder:** Tool for generating client code by recording UI interactions
5. **MCP Server Instructions:** Integration guidelines for Model Control Protocol

### Customization Options
- **Custom Endpoint Names:** Use `api_name` parameter in event listeners
- **Hide from Docs:** Use `show_api=False` to hide endpoints while keeping them functional
- **Disable Endpoints:** Set `api_name=False` to completely disable programmatic access

---

## 3. Additional API Endpoints

### `/info` Endpoint
**Purpose:** Returns metadata about available API endpoints

**Location:**
- `<your-gradio-app-url>/info`
- `<your-gradio-app-url>/info/`

**Query Parameters:**
- `all_endpoints` (optional): When set to `True`, returns information about all endpoints including unnamed ones

**Response Includes:**
- Named and unnamed endpoints
- Parameters each endpoint accepts
- Return types for each endpoint
- Configuration metadata for the Gradio application

**Usage:** Primarily used internally by Gradio clients (Python and JavaScript) for endpoint discovery

---

## 4. Consuming External OpenAPI Specifications

### `gr.load_openapi()` Function
Gradio provides a function to automatically generate Gradio UIs from external OpenAPI v3 specifications.

### Syntax
```python
import gradio as gr

demo = gr.load_openapi(
    openapi_spec="<URL, file path, or Python dict>",
    base_url="<API base URL>",
    paths=["<optional regex patterns>"],  # e.g., ["/pet.*"]
    methods=["get", "post"]  # optional HTTP methods filter
)

demo.launch()
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `openapi_spec` | str/dict | Yes | URL, file path, or Python dictionary containing OpenAPI v3 spec (JSON only) |
| `base_url` | str | Yes | Base URL for API endpoints (e.g., `https://api.example.com/v1`) |
| `paths` | list[str] | No | Endpoint path patterns (supports regex). If omitted, all paths included |
| `methods` | list[str] | No | HTTP methods to include (e.g., `["get", "post"]`). If omitted, all methods included |

### Example: PetStore API
```python
import gradio as gr

demo = gr.load_openapi(
    openapi_spec="https://petstore3.swagger.io/api/v3/openapi.json",
    base_url="https://petstore3.swagger.io/api/v3",
    paths=["/pet.*"],
    methods=["get", "post"],
)

demo.launch()
```

### Generated Features
- **Sidebar Navigation:** Displays available endpoints
- **Interactive Forms:** Creates UI components for each operation parameter
- **Real-time Testing:** Enables API calls and response viewing directly from browser
- **MCP Integration:** Can be deployed as MCP server for programmatic orchestration

---

## 5. Historical Context and Evolution

### Swagger UI Removal (Issue #4054)
**What Changed:**
- Gradio removed the `/docs` endpoint that provided traditional Swagger UI documentation
- Replaced with sidebar documentation at `?view=api` focused on `gradio_client` usage

**User Concerns:**
1. **Language Lock-in:** New docs only show Python examples via `gradio_client`
2. **Cross-platform Limitation:** Users building browser/JavaScript applications cannot use Python client
3. **Custom Routes Undocumented:** Developers using `add_api_route()` lose documentation for custom endpoints
4. **HTTP REST Documentation:** Old Swagger docs were language-agnostic and worked with any HTTP client (curl, JavaScript, etc.)

**User Requests:**
- Reintroduce Swagger docs at `/docs` endpoint
- Make it optional via flag in `launch()` method
- Enable integration with any HTTP client, not just `gradio_client`

**Gradio's Position:**
- Prefers steering users toward `gradio_client` for reliability
- OpenAPI spec still available at `/gradio_api/openapi.json` for those who need it

---

## 6. API Architecture and Technology

### Core Technologies
- **FastAPI:** Web framework powering Gradio's backend
- **Pydantic:** Data validation and schema generation
- **Swagger/OpenAPI:** API specification standard

### Dynamic Schema Generation
Gradio defines dynamic Pydantic models when creating applications, enabling:
- Automatic input validation and type checking
- Schema inference from Python function signatures
- OpenAPI documentation generation
- Acceptable values definition for UI components (dropdowns, sliders)

### Client Libraries
**Python Client:**
- `gradio_client` package
- `Client.view_api()` method for endpoint discovery

**JavaScript Client:**
- `@gradio/client` npm package
- Built-in API discovery capabilities

---

## 7. Current Limitations and Considerations

### OpenAPI Specification Completeness
- **Status:** Feature complete as of PR #11103
- **Format:** JSON only (no YAML support explicitly mentioned)
- **Version:** OpenAPI v3 specification

### Documentation Gaps
1. **No Traditional Swagger UI:** Gradio removed `/docs` endpoint in favor of custom API page
2. **Client Library Focus:** Documentation emphasizes official clients over direct HTTP access
3. **Custom Routes:** Limited documentation for endpoints added via `add_api_route()`

### Recommended Approach
For programmatic integration, Gradio recommends:
1. **Preferred:** Use official Python or JavaScript clients
2. **Alternative:** Access `/gradio_api/openapi.json` for direct HTTP integration
3. **Discovery:** Use `/info` endpoint for runtime endpoint discovery

---

## 8. Integration Examples

### Accessing OpenAPI Spec
```bash
# Fetch OpenAPI specification
curl https://your-gradio-app.com/gradio_api/openapi.json

# Get endpoint information
curl https://your-gradio-app.com/info?all_endpoints=true
```

### Using with API Testing Tools
The OpenAPI specification at `/gradio_api/openapi.json` can be imported into:
- **Postman:** Import > Link > Paste OpenAPI JSON URL
- **Insomnia:** Import > URL > Enter spec endpoint
- **SwaggerHub:** Create API > Import > URL
- **OpenAPI Generator:** Generate client SDKs in multiple languages

### Generating Client Code
```bash
# Example: Generate Python client from Gradio OpenAPI spec
openapi-generator-cli generate \
  -i https://your-gradio-app.com/gradio_api/openapi.json \
  -g python \
  -o ./gradio-python-client
```

---

## 9. Use Cases and Applications

### When to Use OpenAPI Spec
1. **Cross-language Integration:** Integrate Gradio apps with non-Python services
2. **API Documentation:** Generate comprehensive API docs for stakeholders
3. **Client Generation:** Auto-generate client libraries in multiple languages
4. **Testing:** Import into API testing tools for automated testing
5. **Validation:** Verify API coherency and contract compliance

### When to Use `gr.load_openapi()`
1. **API Exploration:** Create quick UI for testing external APIs
2. **Documentation:** Provide interactive documentation for your API
3. **Rapid Prototyping:** Build API clients without writing frontend code
4. **MCP Integration:** Deploy as Model Control Protocol server

---

## 10. Key Takeaways

### For Developers
✅ **Official Spec Exists:** Every Gradio app has an OpenAPI v3 specification at `/gradio_api/openapi.json`
✅ **Automatic Generation:** No configuration needed - spec is auto-generated from function signatures
✅ **Full Coverage:** Includes all endpoints, parameters, types, and examples
✅ **Bidirectional:** Gradio both produces and consumes OpenAPI specifications
✅ **Standards Compliant:** Uses OpenAPI v3, compatible with standard tooling

### For Integration
✅ **HTTP REST API:** Available for any language/framework via OpenAPI spec
✅ **Client Libraries:** Official Python and JavaScript clients recommended
✅ **Tooling Support:** Works with Postman, Insomnia, OpenAPI Generator, etc.
✅ **Discovery Endpoint:** `/info` endpoint for runtime endpoint discovery
⚠️ **No Swagger UI:** Traditional `/docs` endpoint removed in favor of custom API page

### For Architecture
✅ **FastAPI-based:** Built on modern Python async framework
✅ **Pydantic Validation:** Type-safe with automatic schema generation
✅ **Dynamic Models:** Schemas generated from Python function signatures
✅ **MCP Compatible:** Can be deployed as Model Control Protocol servers

---

## 11. Official Resources

### Documentation
- **From OpenAPI Spec Guide:** https://www.gradio.app/guides/from-openapi-spec
- **View API Page Guide:** https://www.gradio.app/guides/view-api-page
- **Getting Started (Python):** https://www.gradio.app/guides/getting-started-with-the-python-client
- **Getting Started (JS):** https://www.gradio.app/guides/getting-started-with-the-js-client
- **Querying with cURL:** https://www.gradio.app/guides/querying-gradio-apps-with-curl
- **Main Documentation:** https://www.gradio.app/docs

### GitHub
- **Main Repository:** https://github.com/gradio-app/gradio
- **Issue #672 (OpenAPI exposure):** https://github.com/gradio-app/gradio/issues/672
- **Issue #4054 (Swagger UI removal):** https://github.com/gradio-app/gradio/issues/4054
- **Issue #7287 (OpenAPI docs):** https://github.com/gradio-app/gradio/issues/7287

### Client Libraries
- **Python Client:** `pip install gradio_client`
- **JavaScript Client:** `npm install @gradio/client`

---

## 12. Conclusion

**Gradio provides comprehensive OpenAPI v3 support** through automatic specification generation for all Gradio applications. The specification is accessible at `/gradio_api/openapi.json` and includes complete endpoint definitions, parameters, types, and examples.

**Key strengths:**
- Zero-configuration automatic generation
- Standards-compliant OpenAPI v3
- Compatible with standard tooling ecosystem
- Bidirectional support (produce and consume specs)
- Built on modern FastAPI + Pydantic stack

**Considerations:**
- Traditional Swagger UI removed (but spec still available)
- Documentation emphasizes official clients over direct HTTP
- Custom routes may need additional documentation

**Bottom line:** Generating custom OpenAPI specifications for Gradio is **not necessary** - the framework already provides complete, automatic OpenAPI v3 specifications for all applications.

---

## Sources

1. [From OpenAPI Spec - Gradio Guide](https://www.gradio.app/guides/from-openapi-spec)
2. [View API Page - Gradio Guide](https://www.gradio.app/guides/view-api-page)
3. [Gradio Documentation](https://www.gradio.app/docs)
4. [Getting Started With The Python Client](https://www.gradio.app/guides/getting-started-with-the-python-client)
5. [Getting Started With The JS Client](https://www.gradio.app/guides/getting-started-with-the-js-client)
6. [Querying Gradio Apps With Curl](https://www.gradio.app/guides/querying-gradio-apps-with-curl)
7. [Issue #672: Expose inputs/outputs in openapi.json](https://github.com/gradio-app/gradio/issues/672)
8. [Issue #4054: Reintroduce Swagger API docs](https://github.com/gradio-app/gradio/issues/4054)
9. [Issue #7287: OpenAPI docs not working](https://github.com/gradio-app/gradio/issues/7287)
10. [Gradio GitHub Repository](https://github.com/gradio-app/gradio)
11. [Gradio API - API Tracker](https://apitracker.io/a/gradio-app)
12. [Building MCP Server With Gradio](https://www.gradio.app/guides/building-mcp-server-with-gradio)
13. [Gradio Python Client Docs](https://www.gradio.app/docs/python-client/client)
14. [Gradio JavaScript Client Docs](https://www.gradio.app/docs/js-client)
