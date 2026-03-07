# Firecrawl OpenAPI Specification Research Report

**Date:** 2025-11-22
**Research Focus:** Official Firecrawl OpenAPI/Swagger Specifications

---

## Executive Summary

**Official OpenAPI Spec Exists:** ✅ YES

An official OpenAPI 3.x specification for Firecrawl is maintained and actively updated by Mendable AI (the company behind Firecrawl). The specification is comprehensive, well-documented, and used for automated SDK generation across multiple programming languages.

---

## OpenAPI Specification Location

### Primary Official Source
- **URL:** https://raw.githubusercontent.com/mendableai/firecrawl/main/apps/api/v1-openapi.json
- **Repository:** https://github.com/mendableai/firecrawl
- **File Path:** `/apps/api/v1-openapi.json`
- **Version:** v1
- **Format:** OpenAPI 3.x (JSON)

### Alternative Sources
- **Firecrawl-Simple Fork:** https://github.com/devflowinc/firecrawl-simple/blob/main/apps/api/v1-openapi.json
  - Stripped-down, self-hostable version optimized for self-hosting
  - Billing logic and some AI features removed
  - OpenAPI 3.0.0 specification

---

## Specification Details

### Core Metadata
- **Title:** Firecrawl API
- **Version:** v1
- **Base Server:** https://api.firecrawl.dev/v1
- **Authentication:** Bearer Token (HTTP Authorization header)
- **Contact:**
  - Organization: Mendable AI (Firecrawl)
  - Website: https://firecrawl.dev
  - Email: humans@trieve.ai (for firecrawl-simple fork)

---

## API Endpoint Coverage

The OpenAPI specification covers ALL official Firecrawl API endpoints:

### 1. Scraping Operations
- `POST /scrape` - Extract content from single URLs with optional LLM processing
  - Supports multiple output formats: markdown, HTML, rawHtml, links, screenshot, extract
  - Configurable timeouts, wait delays, custom headers
  - LLM extraction with schema/prompt support

- `POST /batch/scrape` - Process multiple URLs asynchronously
- `GET /batch/scrape/{id}` - Check batch job status
- `DELETE /batch/scrape/{id}` - Cancel batch operations
- `GET /batch/scrape/{id}/errors` - Retrieve error details

### 2. Crawling Operations
- `POST /crawl` - Traverse websites with configurable depth/path rules
  - Sitemap support
  - Maximum depth configuration
  - Path inclusion/exclusion patterns

- `GET /crawl/{id}` - Monitor crawl progress and retrieve paginated results
- `DELETE /crawl/{id}` - Halt ongoing crawls
- `GET /crawl/{id}/errors` - Access crawl failure information
- `GET /crawl/active` - List team's active crawl jobs

### 3. Mapping Operations
- `POST /map` - Index website structure and discover URLs
  - Search query support
  - Subdomain inclusion options
  - Configurable result limits (max 5000)

### 4. Extraction Operations
- `POST /extract` - Extract structured data from pages using LLMs
  - Natural language-based extraction
  - Schema-based structured output

### 5. Search Operations
- `POST /search` - Query and optionally scrape search results
  - Full page content extraction
  - Multiple output format support

### 6. Advanced Features
- `POST /deep-research` - Multi-iteration research operations
- `POST /llmstxt` - Generate AI-friendly site documentation (LLMs.txt format)

### 7. Billing & Monitoring
- `GET /team/credit-usage` - Monitor credit consumption
- `GET /team/token-usage` - Track token usage

---

## Request/Response Schema Coverage

### Key Request Options (ScrapeOptions Schema)
The specification includes detailed schemas for:
- **Output Formats:** markdown, HTML, screenshots, links, JSON
- **Content Filtering:** exclude/include specific HTML tags
- **Browser Emulation:** mobile mode, JavaScript execution control
- **Proxy Configuration:** basic, stealth, auto-retry modes
- **Advanced Actions:** clicking, scrolling, form input, waiting
- **Geographic Targeting:** locale and location settings
- **Cache Control:** skip-cache options, TLS verification settings
- **LLM Extraction:** custom prompts and structured schemas

### Response Schemas
- **ScrapeResponse:** Returns markdown, HTML variants, screenshots, links, metadata, and optional LLM extraction results
- **CrawlStatusResponseObj:** Provides crawl progress (status, total/completed counts), expiration timestamps, paginated data access
- **CrawlResponse:** Returns job ID and URL for tracking
- **MapResponse:** Returns array of discovered links with metadata
- **Error Responses:** Standardized error format with HTTP status codes

### Security Schemas
- **bearerAuth:** HTTP Bearer token authentication required for all endpoints

---

## SDK Generation & Adoption

The official OpenAPI specification is actively used for automated SDK generation:

### Generated SDKs
1. **C# SDK (tryAGI/Firecrawl)**
   - Repository: https://github.com/tryAGI/Firecrawl
   - Generated using: AutoSDK
   - Documentation: https://tryagi.github.io/Firecrawl/

2. **C# SDK (Ai4c-AI/Firecrawl-CSharp)**
   - Repository: https://github.com/Ai4c-AI/Firecrawl-CSharp
   - Also based on official OpenAPI spec

3. **Official SDKs**
   - Python: https://github.com/mendableai/firecrawl-py
   - Go: https://github.com/mendableai/firecrawl-go
   - JavaScript/TypeScript: Available in main repository

---

## Documentation Resources

### Official Documentation
- **API Reference:** https://docs.firecrawl.dev/api-reference/introduction
- **Quickstart Guide:** https://docs.firecrawl.dev/introduction
- **Documentation Repository:** https://github.com/mendableai/firecrawl-docs
- **API Playground:** https://www.firecrawl.dev/playground

### Additional Resources
- **Main GitHub Repository:** https://github.com/mendableai/firecrawl
- **MCP Server:** https://github.com/mendableai/firecrawl-mcp-server
- **Blog/Tutorials:** https://www.firecrawl.dev/blog/

---

## Specification Quality & Maintenance

### Current Status
- **Actively Maintained:** ✅ Yes
- **Version Control:** Tracked in main repository
- **SDK Support:** Multiple auto-generated SDKs
- **Breaking Changes:** Versioned API (currently v1)

### Known Considerations
1. **Handwritten vs Generated:** The OpenAPI spec is currently handwritten and manually updated
   - Issue #12 in firecrawl-simple discusses moving to JSDOC-based generation to reduce discrepancies
   - Manual maintenance can lead to spec-server inconsistencies

2. **Self-Hosting Support:** There are discussions about including Swagger UI and Redoc for self-hosters
   - Would allow viewing spec without external documentation vendors

3. **Documentation Updates:** PRs like #1003 show active maintenance of the OpenAPI spec for new features

---

## Generation Feasibility Assessment

### For the Official Firecrawl API
**Feasibility:** ✅ NOT NEEDED - Official spec already exists and is comprehensive

The existing OpenAPI specification is:
- Complete and covers all documented endpoints
- Actively maintained and updated
- Production-ready for SDK generation
- Used successfully for multiple language SDKs

### For Custom/Fork Implementations
**Feasibility:** ✅ HIGHLY FEASIBLE

If you need to generate a modified specification:
1. Use the official spec as a base template
2. Modify endpoint definitions as needed
3. Update schemas for custom features
4. Validate using OpenAPI validators
5. Generate SDKs using tools like AutoSDK, OpenAPI Generator, or Swagger Codegen

---

## API Capabilities Summary

### Core Features
1. **Web Scraping**
   - Single page extraction
   - Batch processing support
   - Multiple output formats (Markdown, HTML, JSON, Screenshots)
   - Custom header and cookie support

2. **Web Crawling**
   - Recursive site traversal
   - Depth control
   - Path filtering (include/exclude patterns)
   - Sitemap integration
   - Progress monitoring

3. **URL Mapping**
   - Complete site URL discovery
   - Fast and reliable indexing
   - Search query filtering
   - Subdomain support

4. **Search Integration**
   - Web search capabilities
   - Full content extraction from results
   - Configurable result limits

5. **LLM-Powered Extraction**
   - Natural language queries
   - Schema-based structured data extraction
   - Custom prompt support
   - JSON output generation

6. **Advanced Features**
   - Deep research (multi-iteration)
   - LLMs.txt generation
   - Browser automation actions
   - Proxy support (basic, stealth, auto-retry)
   - Geographic targeting

7. **Monitoring & Billing**
   - Credit usage tracking
   - Token consumption monitoring
   - Error logging and retrieval
   - Active job management

---

## Recommendations

### For Using the OpenAPI Spec

1. **Direct Integration**
   - Use the official spec from: https://raw.githubusercontent.com/mendableai/firecrawl/main/apps/api/v1-openapi.json
   - Stay updated with the main repository for changes
   - Monitor PRs and releases for API updates

2. **SDK Generation**
   - Use AutoSDK, OpenAPI Generator, or Swagger Codegen
   - Reference existing SDKs for best practices
   - Implement Bearer token authentication

3. **Self-Hosting**
   - Consider firecrawl-simple for lighter deployments
   - Use the same OpenAPI spec structure
   - Add Swagger UI/Redoc for documentation

4. **Testing & Validation**
   - Use the API playground for endpoint testing
   - Validate requests against the OpenAPI schemas
   - Monitor rate limits (429 responses)

### For Custom Development

1. Fork the official specification as a starting point
2. Maintain version compatibility for existing integrations
3. Document any extensions or modifications
4. Consider contributing improvements back to the main repository

---

## Conclusion

Firecrawl provides a comprehensive, well-maintained OpenAPI 3.x specification that covers all official API endpoints and features. The specification is production-ready, actively used for SDK generation across multiple languages, and suitable for direct integration into any OpenAPI-compatible tooling.

**Key Takeaways:**
- Official OpenAPI spec exists and is publicly accessible
- Comprehensive coverage of all API endpoints
- Actively maintained with version control
- Successfully used for automated SDK generation
- No need to generate from scratch - use the official spec directly

---

## Sources

- [Firecrawl Official Documentation](https://docs.firecrawl.dev/api-reference/introduction)
- [Firecrawl GitHub Repository](https://github.com/mendableai/firecrawl)
- [OpenAPI Spec Location](https://raw.githubusercontent.com/mendableai/firecrawl/main/apps/api/v1-openapi.json)
- [tryAGI/Firecrawl SDK](https://github.com/tryAGI/Firecrawl)
- [Firecrawl-Simple Fork](https://github.com/devflowinc/firecrawl-simple/blob/main/apps/api/v1-openapi.json)
- [Firecrawl API Playground](https://www.firecrawl.dev/playground)
- [PR #1003 - Credit Usage Docs](https://github.com/mendableai/firecrawl/pull/1003)
- [Issue #430 - API Reference](https://github.com/mendableai/firecrawl/issues/430)
