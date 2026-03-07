# Crawl4AI Analysis: Quick Reference Guide

## Key Findings at a Glance

### Core Design Patterns (6 Major Patterns)

| Pattern | Primary Use | Key Classes |
|---------|-------------|-------------|
| **Strategy** | Extraction methods | `JsonCssExtractionStrategy`, `LLMExtractionStrategy`, `*DeepCrawlStrategy` |
| **Builder** | Configuration | `BrowserConfig`, `CrawlerRunConfig` |
| **Context Manager** | Resource lifecycle | `AsyncWebCrawler` |
| **Hook/Callback** | Extension points | `BrowserConfig.hooks` dictionary |
| **Factory** | Content-type routing | Auto-selection of strategies (implicit) |
| **Composite** | Multiple strategies | Combining CSS + LLM extraction |

---

## Primary Data Model

### CrawlResult (The Core Output)
```
CrawlResult
├── url: str                                    [Where we crawled]
├── status: CrawlStatus                         [Success/failure]
├── markdown: MarkdownGenerationResult          [LLM-ready content]
│   ├── raw_markdown: str                       [Original extraction]
│   ├── fit_markdown: str                       [Cleaned version]
│   └── fit_html: str                           [HTML version]
├── extracted_data: Dict[str, Any]              [Structured extraction]
├── media: Dict[str, List]                      [Images, videos, etc.]
├── metadata: Dict[str, Any]                    [Page metadata]
├── raw_html: str                               [Original HTML]
├── pdf: bytes                                  [PDF if requested]
├── screenshot: bytes                           [Page screenshot]
└── session_id: str                             [Browser session ID]
```

---

## Extension Points (5 Main Categories)

### 1. Extraction Strategies
- **CSS-based:** Fast, deterministic, no LLM cost
- **LLM-based:** Semantic understanding, Pydantic schema validation
- **PDF-specific:** Specialized PDF parsing strategies

### 2. Hooks/Callbacks
- `on_page_context_created` - Perfect for login automation
- `on_before_fetch`, `on_after_fetch`, `on_content_ready`
- Receive Playwright page object for full control

### 3. JavaScript Execution
- Custom JS injection via `js_code` parameter
- Trigger dynamic content loading
- Wait for specific conditions with `wait_for` parameter

### 4. Deep Crawling
- `BFSDeepCrawlStrategy` - Breadth-first discovery
- `DFSDeepCrawlStrategy` - Depth-first discovery
- URL filtering: include/exclude patterns, domain limits

### 5. Content Filtering
- `css_filters` - Remove noise (nav, ads, footer, etc.)
- Multiple markdown versions: raw vs. cleaned
- Post-processing capabilities

---

## Configuration Hierarchy

### Minimum Config
```python
BrowserConfig()
CrawlerRunConfig(url="https://example.com")
```

### Full Config
```python
# Browser setup (persistent)
BrowserConfig(
    headless=True,                              # Headless mode
    use_managed_browser=True,                   # Persistent profile
    user_data_dir="./profiles/auth",            # Profile location
    browser_type="chromium",                    # Playwright browser
    viewport_size=(1920, 1080),                 # Page size
    proxy_type="http",                          # Proxy support
    hooks={"on_page_context_created": fn}      # Lifecycle hooks
)

# Run-specific config (per crawl)
CrawlerRunConfig(
    url="https://example.com",                  # Target URL
    extraction_strategy=JsonCssExtractionStrategy(...),  # How to extract
    js_code="// Custom JS",                     # Dynamic content
    wait_for="selector:.loaded",                # Wait condition
    delay=2.0,                                  # Rate limiting
    css_filters=["nav", ".ad"],                 # Remove elements
    deep_crawl_strategy=BFSDeepCrawlStrategy(max_depth=2)  # Recursive
)
```

---

## Design Principles

### 1. LLM-Ready by Default
- Converts HTML → Clean Markdown
- Removes noise automatically
- Multiple output formats (markdown, HTML, JSON)

### 2. Async-First Architecture
- Non-blocking operations via `async`/`await`
- Browser pool for concurrency
- Efficient resource management with context managers

### 3. Composable Strategies
- Mix & match extraction approaches
- CSS for structured data, LLM for semantic understanding
- Switch strategies at runtime

### 4. Type-Safe Extraction
- Pydantic schema validation
- JSON Schema generation
- Automatic type coercion

### 5. Extensible Pipeline
- Multiple hooks for custom logic
- JavaScript injection for dynamic content
- CSS filtering for content pruning

---

## Common Patterns & Use Cases

### Quick Reference (Pattern → Implementation)

| Use Case | Pattern | Key Components |
|----------|---------|-----------------|
| Extract product prices | CSS Extraction | `JsonCssExtractionStrategy` + CSS selectors |
| Login to protected site | Hook + Session | `on_page_context_created` hook + `use_managed_browser` |
| Handle dynamic JS | JS Injection | `js_code` parameter + `wait_for` selector |
| Crawl entire site | Deep Crawling | `BFSDeepCrawlStrategy` or `DFSDeepCrawlStrategy` |
| Extract structured data | LLM Extraction | `LLMExtractionStrategy` + Pydantic schema |
| Clean for LLM input | Content Filtering | `css_filters` + `fit_markdown` |
| Parse PDF documents | PDF Strategy | Auto-detection + `PDFCrawlerStrategy` |
| Integrate with pipeline | dlt resource | Wrap in `@dlt.resource` decorator |
| Multi-page comparison | Multi-URL crawl | Loop with same extraction strategy |
| Get clean markdown | Content Pruning | Enable filters + use `fit_markdown` |

---

## Integration Ecosystem

### Upstream Sources
- Static HTML pages
- Dynamic JavaScript-heavy pages
- PDF documents
- Protected sites (with auth)
- Multi-page websites (deep crawling)

### Downstream Destinations
- **Data Pipelines:** dlt → DuckDB, PostgreSQL, Parquet
- **Vector DBs:** LanceDB, pgvector (for semantic search)
- **AI Models:** Direct input for RAG, fine-tuning
- **Document Processors:** Docling (complex PDFs), Unstract (structured extraction)
- **AI Agents:** Agno, LangChain (agentic workflows)

---

## Best Practices Checklist

### Performance
- [ ] Use CSS extraction first (cheaper/faster)
- [ ] Enable caching for repeated URLs
- [ ] Set appropriate `delay` for rate limiting
- [ ] Batch multiple URLs
- [ ] Use `css_filters` to reduce markdown size

### Reliability
- [ ] Check `result.status.is_success()` before processing
- [ ] Implement retry logic with exponential backoff
- [ ] Store original HTML alongside markdown
- [ ] Use session management for auth
- [ ] Log full results for debugging

### Data Quality
- [ ] Validate with Pydantic schemas
- [ ] Combine CSS (fast) + LLM (accurate)
- [ ] Store both `raw_markdown` and `fit_markdown`
- [ ] Use media extraction for images
- [ ] Test with real pages first

### Cost Management (LLM)
- [ ] CSS extraction → fallback to LLM
- [ ] Batch LLM requests
- [ ] Use cheaper models (GPT-3.5 Turbo, Claude Sonnet)
- [ ] Cache results by URL
- [ ] Monitor API costs

### Ethics & Legal
- [ ] Respect robots.txt
- [ ] Use appropriate `delay` values
- [ ] Set User-Agent headers
- [ ] Only crawl authorized content
- [ ] Get user consent for auth flows

---

## Architecture Strengths

1. **Flexibility:** Multiple strategies, swappable at runtime
2. **LLM-Native:** Built for AI/ML consumption from ground up
3. **Extensible:** Hooks, custom strategies, integration points
4. **Modern:** Async/await, Pydantic, type hints
5. **Practical:** Real-world features (auth, JS, PDF, proxies)

---

## Key Code Examples

### Basic Crawl
```python
from crawl4ai import AsyncWebCrawler, CrawlerRunConfig

async with AsyncWebCrawler() as crawler:
    result = await crawler.arun(CrawlerRunConfig(url="https://example.com"))
    print(result.markdown.fit_markdown)
```

### Structured Extraction
```python
from crawl4ai.extraction import JsonCssExtractionStrategy

strategy = JsonCssExtractionStrategy(
    extractions=[
        {"name": "title", "css": "h1", "type": "text"},
        {"name": "price", "css": ".price", "type": "text"},
    ]
)
config = CrawlerRunConfig(url="https://shop.com", extraction_strategy=strategy)
result = await crawler.arun(config)
print(result.extracted_data)  # {'title': '...', 'price': '...'}
```

### Login Automation
```python
async def login(page, context, **kwargs):
    await page.fill('input[name="email"]', "user@example.com")
    await page.fill('input[name="password"]', "password")
    await page.click("button[type='submit']")
    await page.wait_for_url("**/dashboard")

config = BrowserConfig(hooks={"on_page_context_created": login})
```

### dlt Integration
```python
@dlt.resource
async def web_crawler():
    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(CrawlerRunConfig(url="https://api.example.com"))
        yield result.extracted_data

pipeline = dlt.pipeline("web_crawler", destination="duckdb")
load_info = pipeline.run(web_crawler())
```

---

## Files in Repository

- `/home/user/hackathon/CRAWL4AI_ANALYSIS.md` - Comprehensive 758-line analysis
- `/home/user/hackathon/research/pdf/ingestion/crawl4ai_dlt.md` - Integration with dlt tutorial
- `/home/user/hackathon/infrastructure/compose/agno/cookbook/tools/crawl4ai_tools.py` - Agno integration example

---

## Next Steps for Usage

1. **Start Simple:** Basic crawl without extraction
2. **Add Extraction:** CSS selectors for known structure
3. **Handle Auth:** BrowserProfiler for login
4. **Scale Up:** Deep crawl strategies for entire sites
5. **Integrate:** Connect with dlt, vector DBs, AI models
6. **Optimize:** Monitor costs, cache, batch requests
