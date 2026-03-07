# 07. Technical Implementation

Pipeline architecture, anti-bot strategies, and data source management.

## Overview

This category covers the technical implementation details for building Celtic language data pipelines, including:
- Managing diverse data sources
- Anti-bot and rate limiting strategies
- Pipeline orchestration patterns

## Documents

| File | Description |
|------|-------------|
| `Managing Diverse Data Sources for Pipelines.md` | Multi-source pipeline architecture |
| `Open-Source Crawl4ai Anti-Bot Stack.md` | Anti-detection strategies for scraping |

## Key Patterns

### Source Management

```python
# DLT source configuration pattern
import dlt

@dlt.source
def celtic_sources():
    @dlt.resource(write_disposition="merge")
    def gaois_api():
        # Gaois API with rate limiting
        yield from fetch_gaois_data()

    @dlt.resource(write_disposition="append")
    def scraped_sites():
        # Web scraping with anti-bot
        yield from scrape_with_crawl4ai()

    return gaois_api, scraped_sites
```

### Anti-Bot Stack

- **Browser Fingerprinting** - playwright-stealth
- **Proxy Rotation** - Residential proxies
- **Request Pacing** - Adaptive rate limiting
- **Session Management** - Cookie persistence

### Rate Limiting

| Source | Rate Limit | Strategy |
|--------|------------|----------|
| Gaois APIs | 100 req/min | Token bucket |
| Educational sites | 1 req/3s | Polite delay |
| Government portals | Varies | Adaptive |

## Technical Stack

```yaml
Scraping:
  - crawl4ai (primary)
  - playwright (fallback)
  - requests (API calls)

Pipeline:
  - dlt (orchestration)
  - DuckDB (storage)
  - Dagster (scheduling)

Monitoring:
  - Prometheus (metrics)
  - Grafana (dashboards)
```

## Best Practices

1. **Respect robots.txt** - Check site policies
2. **Identify yourself** - Use descriptive User-Agent
3. **Rate limit** - Don't overwhelm servers
4. **Cache aggressively** - Reduce repeat requests
5. **Handle errors gracefully** - Exponential backoff

## Related Categories

- **02-celtic-data-acquisition** - Source catalog
- **04-geospatial-linguistics** - DuckDB patterns
