# Celtic Data Acquisition

This directory consolidates research on data acquisition strategies for Celtic language resources, including API access, web scraping methodologies, and pan-Celtic archive workflows.

## Overview

Data acquisition for Celtic languages requires a multi-pronged approach due to the fragmented nature of digital heritage resources across Ireland, Scotland, Wales, and the Isle of Man.

### Acquisition Methods

| Method | Priority | Use Case | Tools |
|--------|----------|----------|-------|
| **API Access** | Highest | Structured data with clear endpoints | Requests, aiohttp |
| **Direct Download** | High | TMX files, word lists, CSV exports | wget, Python |
| **GitHub Cloning** | Medium | Open-source repositories | git |
| **Web Scraping** | Lower | Gap-filling, editorial content | crawl4ai, Skyvern |

## Documents in this Category

| Document | Focus | Key Resources |
|----------|-------|---------------|
| `gaois-api-reference.md` | Irish data APIs | Logainm, Duchas, Tearma |
| `pan-celtic-scraping.md` | Cross-nation scraping | Education, heritage archives |
| `acquisition-pipeline.md` | Technical implementation | dlt, asyncio patterns |

## Primary Data Sources

### Ireland (Highest Priority)

| Source | Content | Method | Est. Size |
|--------|---------|--------|-----------|
| **Gaois APIs** | 200M+ words | API | Logainm, Duchas |
| **Parallel Corpus** | 130.5M words | TMX Download | Legislation |
| **Corpas.ie** | 240M+ words | Download | Word lists |
| **Ainm.ie** | 1.3M words | Scrape | Biographies |

### Scotland

| Source | Content | Method |
|--------|---------|--------|
| **Tobar an Dualchais** | 50k+ recordings | Faceted search |
| **SQA Past Papers** | Examination materials | Form scraping |
| **Education Scotland** | CfE documents | Hierarchical |

### Wales

| Source | Content | Method |
|--------|---------|--------|
| **Hwb** | Curriculum resources | React scraping |
| **WJEC** | Past papers | Search-based |
| **People's Collection** | Heritage archive | Map/list traversal |

### Isle of Man

| Source | Content | Method |
|--------|---------|--------|
| **Culture Vannin** | Audio, publications | Hierarchical |
| **LearnManx** | Dictionary, phrases | App extraction |

## Acquisition Priority Matrix

### Phase 1: API & Direct Downloads

1. Register for Gaois API key (gaois@dcu.ie)
2. Download Parallel Corpus TMX files
3. Download Corpas.ie word lists
4. Implement Logainm API collector
5. Implement Duchas API collector

### Phase 2: GitHub Resources

1. Clone gaoisalign (text alignment)
2. Clone sloinnte (surnames)
3. Clone Terminologue (terminology software)

### Phase 3: Strategic Scraping

1. Ainm.ie biographies (1,785 items)
2. Tearma.ie terminology
3. SQA/WJEC past papers
4. canuint.ie audio metadata

## Technical Stack

```yaml
Languages: Python 3.9+

Scraping:
  - crawl4ai: LLM-ready extraction
  - Skyvern: Agentic browser automation
  - Playwright: Browser control

APIs:
  - aiohttp: Async HTTP
  - requests: Sync HTTP
  - httpx: Modern HTTP client

Processing:
  - dlt: Pipeline orchestration
  - translate-toolkit: TMX parsing
  - beautifulsoup4: HTML parsing

Storage:
  - DuckDB: Local analytics
  - Parquet: Columnar storage
  - JSONL: Streaming format
```

## Interaction Types

Classification system for scraping strategies:

| Type | Pattern | Example Sites |
|------|---------|---------------|
| **Type A** | Hierarchical drill-down | ncca.ie, culturevannin.im |
| **Type B** | Complex form logic | examinations.ie, sqa.org.uk |
| **Type C** | Spatial/map traversal | canuint.ie, peoplescollection.wales |
| **Type D** | Sequential/paginated | duchas.ie, tobarandualchais.co.uk |

## Ethical Scraping Guidelines

1. **Respect robots.txt** - Check before scraping
2. **Rate limiting** - Max 1 request/second
3. **User-Agent** - Identify scraper purpose
4. **API First** - Always prefer official APIs
5. **Caching** - Avoid redundant requests
6. **Attribution** - Credit data sources

## Cross-References

- **Category 01 (Celtic Language AI)** - Model training with acquired data
- **Category 03 (Bilingual Dataset Creation)** - TMX processing pipeline
- **Main Research Category 04** - Stealth browser stack (Patchright, crawl4ai)

## Source Files Consolidated

- `irish_bilingual_dataset_research.md` - Gaois API documentation
- `Celtic Data Scraping and Integration Plan.md` - Skyvern integration
- `Open-Source Crawl4ai Anti-Bot Stack.md` - Browser automation
- `Crawl4ai Scraping and Site Analysis.md` - Site-specific strategies
