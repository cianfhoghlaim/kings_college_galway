# Web Automation & Archival Systems

This directory consolidates research on autonomous web scraping architectures, anti-bot evasion strategies, and AI-driven content extraction for building comprehensive data archives.

## Overview

The research covers the complete stack for intelligent web data acquisition:
- **Stealth Browser Infrastructure**: Patchright, Browserless, CDP architecture
- **Anti-Bot Evasion**: Cloudflare Turnstile bypass, TLS fingerprinting countermeasures
- **Agentic Scraping**: Skyvern visual agents, Stagehand operators, Crawl4AI gatherers
- **MCP Integration**: Model Context Protocol for tool interoperability
- **Irish Educational Archives**: examinations.ie, ncca.ie, curriculumonline.ie workflows

## Documents in this Category

| Document | Focus | Key Technologies |
|----------|-------|------------------|
| `stealth-browser-stack.md` | Self-hosted anti-detection infrastructure | Patchright, CDP, Docker Compose |
| `agentic-scraping-architecture.md` | Hunter-Gatherer-Operator patterns | Skyvern, Stagehand, Crawl4AI |
| `adaptive-crawling.md` | Semantic vector traversal strategies | Crawl4AI, BestFirstStrategy, BM25 |
| `irish-archives-workflow.md` | Educational data acquisition pipelines | examinations.ie, ncca.ie workflows |

## Key Architectural Decisions

### 1. The Hybrid "Swarm" Architecture

```
User Request
    ↓
┌─────────────────────────────────────────┐
│           MCP Orchestrator              │
│    (Tool Discovery & Dispatch)          │
└─────────────────────────────────────────┘
    ↓                    ↓                ↓
┌──────────┐      ┌──────────┐      ┌──────────┐
│ SKYVERN  │      │STAGEHAND │      │ CRAWL4AI │
│ (Hunter) │      │(Operator)│      │(Gatherer)│
│          │      │          │      │          │
│ Visual   │      │ Cached   │      │ Semantic │
│ Mapping  │      │ Actions  │      │ Crawling │
└──────────┘      └──────────┘      └──────────┘
    ↓                    ↓                ↓
         ┌───────────────────────────┐
         │    PATCHRIGHT GRID       │
         │  (Stealth Browser Pool)  │
         └───────────────────────────┘
```

### 2. Tool Selection Matrix

| Scenario | Tool | Rationale |
|----------|------|-----------|
| Unknown/Dynamic UI | Skyvern | Visual reasoning adapts to layout changes |
| Repetitive Forms | Stagehand | Cached selectors after first LLM call |
| Hierarchical Sites | Crawl4AI | Semantic filtering, high throughput |
| Legacy Deep Web | Stagehand | State-dependent interactions |
| CAPTCHA Challenge | Theyka Solver | Token extraction microservice |

### 3. Cloudflare Turnstile Detection Layers

| Layer | Detection Method | Countermeasure |
|-------|-----------------|----------------|
| Network (TLS) | JA3/JA4 fingerprinting | Patchright binary patching |
| Runtime | `navigator.webdriver` | Patchright C++ patches |
| Behavioral | Mouse/keystroke entropy | Human-like delays |
| Canvas/WebGL | GPU fingerprinting | Xvfb headful mode |

## Quick Reference

### Docker Compose Stack

```yaml
services:
  browser-grid:
    build: ./browser-grid  # Patchright stealth
    shm_size: '2gb'
    cap_add: [SYS_ADMIN]

  solver-service:
    image: theyka/turnstile-solver:latest
    environment:
      - BROWSER_TYPE=chromium

  mcp-server:
    build: ./mcp-server
    environment:
      - CDP_URL=ws://browser-grid:9222
      - SOLVER_API_URL=http://solver-service:5000/turnstile
```

### Crawl4AI Adaptive Configuration

```python
from crawl4ai import AsyncWebCrawler, AdaptiveConfig
from crawl4ai.deep_crawling import BestFirstCrawlingStrategy

strategy = BestFirstCrawlingStrategy(
    max_depth=5,
    max_pages=5000,
    scorer_config={
        "keywords": ["curriculum", "specification", "syllabus"],
        "weight": 0.85
    }
)

config = CrawlerRunConfig(
    deep_crawl_strategy=strategy,
    fit_markdown=True,
    adaptive_config=AdaptiveConfig(
        confidence_threshold=0.8,
        min_gain_threshold=0.05
    )
)
```

### Stagehand Form Automation

```typescript
import { Stagehand } from "@browserbasehq/stagehand";

const stagehand = new Stagehand({ llmClient: myClient });
await stagehand.init();

// First call: LLM finds selector
// Subsequent calls: Uses cached selector (no LLM cost)
await page.act("Select '2024' from the Year dropdown");
await page.act("Click the Search button");

// Extract with schema
const data = await page.extract({
    instruction: "Extract all exam paper download links",
    schema: z.object({
        papers: z.array(z.object({
            subject: z.string(),
            level: z.string(),
            downloadUrl: z.string()
        }))
    })
});
```

## Source Files Consolidated

This category merges content from:
- `Open-Source Crawl4ai Anti-Bot Stack.md`
- `Integrating Skyvern with Crawl4AI_Stagehand.md`
- `Open-Source Web Scraping Architecture Analysis.md`
- `Celtic Data Scraping and Integration Plan.md`
- `Unified Scraping Swarm Stack Optimization.md`

## Performance Benchmarks

| Tool | Pages/Minute | Cost/1000 Pages | Best For |
|------|-------------|-----------------|----------|
| Crawl4AI | 100-500 | ~$0 (local) | Discovery |
| Stagehand (cached) | 50-100 | ~$0.50 | Forms |
| Stagehand (cold) | 5-10 | ~$5.00 | New layouts |
| Skyvern | 3-5 | ~$50.00 | Complex UI |

## Licensing Considerations

| Tool | License | Commercial Use |
|------|---------|----------------|
| Crawl4AI | Apache 2.0 | Permissive |
| Stagehand | MIT | Permissive |
| Skyvern | AGPL-3.0 | Restrictive (copyleft) |
| Patchright | MIT | Permissive |
| Browserless | Mix (OSS/Commercial) | Check edition |

## Implementation Priorities

### Phase 1: Stealth Infrastructure
1. Build Patchright Docker image with Xvfb
2. Deploy browser grid with CDP exposure
3. Configure Theyka solver service

### Phase 2: Crawler Integration
1. Implement Crawl4AI adaptive strategy
2. Configure semantic keyword scoring
3. Set up fit_markdown processing

### Phase 3: Agent Orchestration
1. Deploy Stagehand for form automation
2. Implement selector caching persistence
3. Add self-healing error recovery

### Phase 4: MCP Unification
1. Wrap scrapers as MCP tools
2. Configure tool discovery
3. Integrate with AI orchestrator
