# Agentic Scraping Architecture: Hunter-Gatherer-Operator Pattern

## Executive Summary

This document details the "Unified Scraping Swarm" architecture that orchestrates Skyvern, Crawl4AI, and Stagehand into a cohesive system. By leveraging the Model Context Protocol (MCP) as a control plane, organizations can dynamically compose workflows using the optimal tool for each task.

---

## 1. The Fragmentation Problem

Traditional scraping architectures deploy tools in silos:

| Silo Type | Tool | Problem |
|-----------|------|---------|
| **Visual Navigation** | Skyvern | Authenticated state trapped in isolated container |
| **High-Volume Extraction** | Crawl4AI | Cannot access Skyvern's authenticated session |
| **Tactical Interaction** | Stagehand | Requires manual session management |

The **Unified Swarm** resolves this by treating browser state as a shared resource while automation libraries act as transient clients.

---

## 2. Tool Philosophies and Architectural Roles

### 2.1 Skyvern: The Visual Reasoning Engine (Hunter)

Skyvern operates on a "Vision-First" paradigm, using Vision LLMs to interpret visual renderings rather than DOM parsing.

**Mechanism:**
1. Captures viewport screenshots
2. Overlays coordinate system
3. Feeds visual data to LLM with high-level goal
4. LLM returns coordinate-based actions

**Resilience Profile:**
- Immune to DOM thrashing (randomized class names)
- Resistant to layout changes
- Handles dynamic SPAs effectively

**Architectural Role:** The **Navigator** - traverses initial barriers, solves visual puzzles, reaches target state.

```python
# Skyvern Task Configuration
task_config = {
    "url": "https://examinations.ie",
    "goal": "Navigate to the exam archive and select Mathematics 2024",
    "browser_session_id": "pbs_shared_session_123",
    "max_steps": 10
}
```

### 2.2 Crawl4AI: The High-Velocity Extractor (Gatherer)

Crawl4AI focuses on efficient transformation of unstructured content into LLM-friendly formats.

**Mechanism:**
1. Loads pages via Playwright
2. Applies heuristic intelligence (BM25, content pruning)
3. Strips boilerplate (nav, footer, ads)
4. Converts to clean Markdown

**Performance Features:**
- Aggressive caching strategies
- Parallel execution via `arun_many()`
- Image loading disabled for bandwidth

**Architectural Role:** The **Extractor** - once navigation complete, pulls content efficiently.

```python
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig

browser_config = BrowserConfig(
    browser_type="chromium",
    cdp_url="http://browser-hub:9222",  # Shared browser
    verbose=True
)

run_config = CrawlerRunConfig(
    cache_mode="BYPASS",  # Real-time state after Skyvern navigation
    word_count_threshold=10,
    fit_markdown=True
)

async with AsyncWebCrawler(config=browser_config) as crawler:
    result = await crawler.arun(url="current_page", config=run_config)
```

### 2.3 Stagehand: The Hybrid Tactician (Operator)

Stagehand bridges rigid code and fluid AI intent with primitives: `act`, `extract`, `observe`.

**Mechanism:**
1. Interleaves deterministic Playwright code with AI instructions
2. Implements "Self-Healing" - caches successful selectors
3. Falls back to LLM only if cached selector fails

**Architectural Role:** The **Operator** - handles precise, multi-step interactions.

```typescript
import { Stagehand } from "@browserbasehq/stagehand";

const stagehand = new Stagehand({
  localBrowserLaunchOptions: {
    cdpUrl: "http://browser-hub:9222"
  }
});

await stagehand.init();

// First call: LLM finds selector
// Subsequent calls: Uses cached selector (no LLM cost)
await page.act("Select '2024' from the Year dropdown");
await page.act("Click the Search button");

// Extract with schema validation
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

---

## 3. Tool Selection Matrix

| Scenario | Tool | Rationale |
|----------|------|-----------|
| Unknown/Dynamic UI | **Skyvern** | Visual reasoning adapts to layout changes |
| Repetitive Forms | **Stagehand** | Cached selectors after first LLM call |
| Hierarchical Sites | **Crawl4AI** | Semantic filtering, high throughput |
| Legacy Deep Web | **Stagehand** | State-dependent interactions |
| CAPTCHA Challenge | **Solver Sidecar** | Token extraction microservice |

---

## 4. MCP Integration: The Nervous System

The Model Context Protocol provides a standardized interface for AI agents to discover and invoke tools.

### 4.1 Gateway Configuration

```json
{
  "mcpServers": {
    "skyvern": {
      "command": "docker",
      "args": ["exec", "-i", "skyvern_container", "skyvern", "mcp"]
    },
    "crawl4ai": {
      "command": "docker",
      "args": ["exec", "-i", "crawl4ai_container", "python", "-m", "crawl4ai_mcp"]
    },
    "stagehand": {
      "command": "docker",
      "args": ["exec", "-i", "stagehand_container", "npm", "start"]
    },
    "turnstile_solver": {
      "command": "docker",
      "args": ["exec", "-i", "solver_container", "python", "-m", "solver_mcp"]
    }
  }
}
```

### 4.2 Tool Schema Definitions

**Skyvern Tools:**
```yaml
- name: navigate_visual
  description: Use visual AI to navigate to a goal state
  parameters:
    url: string
    goal: string
    browser_session_id: string

- name: solve_auth
  description: Complete authentication workflow
  parameters:
    credentials_id: string
```

**Crawl4AI Tools:**
```yaml
- name: extract_markdown
  description: Extract page content as clean Markdown
  parameters:
    url: string | "current_page"

- name: crawl_site
  description: Recursive crawl from starting point
  parameters:
    url: string
    max_depth: integer
    keywords: array[string]
```

**Stagehand Tools:**
```yaml
- name: act
  description: Execute specific UI action
  parameters:
    instruction: string

- name: extract_data
  description: Extract data with schema validation
  parameters:
    instruction: string
    schema: object
```

### 4.3 Dynamic Routing Example

**User Request:** "Download the invoice for the last order from Amazon."

**Orchestrator Plan:**
1. Call `skyvern.navigate_visual("amazon.com", "Go to recent orders")`
2. *Wait for completion*
3. Call `stagehand.act("Click the 'Invoice' link for the top order")`
4. *Wait for navigation*
5. Call `crawl4ai.extract_markdown("current_page")`

---

## 5. Performance Benchmarks

| Tool | Pages/Minute | Cost/1000 Pages | Best For |
|------|-------------|-----------------|----------|
| **Crawl4AI** | 100-500 | ~$0 (local) | Discovery, bulk extraction |
| **Stagehand (cached)** | 50-100 | ~$0.50 | Repetitive forms |
| **Stagehand (cold)** | 5-10 | ~$5.00 | New layouts |
| **Skyvern** | 3-5 | ~$50.00 | Complex visual UI |

---

## 6. Interoperability and State Management

### 6.1 Session Handoff Protocol

```python
# Unified session management
class SwarmSession:
    def __init__(self, cdp_url: str, session_id: str):
        self.cdp_url = cdp_url
        self.session_id = session_id
        self._lock = asyncio.Lock()

    async def with_skyvern(self):
        async with self._lock:
            return SkyvernClient(
                browser_session_id=self.session_id,
                cdp_url=self.cdp_url
            )

    async def with_crawl4ai(self):
        async with self._lock:
            browser_config = BrowserConfig(cdp_url=self.cdp_url)
            return AsyncWebCrawler(config=browser_config)

    async def with_stagehand(self):
        async with self._lock:
            return Stagehand(localBrowserLaunchOptions={
                "cdpUrl": self.cdp_url
            })
```

### 6.2 Cookie and State Persistence

```python
# Export session state for backup/restore
async def export_session_state(session: SwarmSession) -> dict:
    browser, context, _ = await attach_to_shared_browser(session.cdp_url)

    return {
        "cookies": await context.cookies(),
        "local_storage": await context.storage_state(),
        "session_id": session.session_id,
        "timestamp": datetime.utcnow().isoformat()
    }

async def restore_session_state(session: SwarmSession, state: dict):
    browser, context, _ = await attach_to_shared_browser(session.cdp_url)

    await context.add_cookies(state["cookies"])
    await context.add_init_script(f"""
        Object.keys({state['local_storage']}).forEach(key => {{
            localStorage.setItem(key, {state['local_storage']}[key]);
        }});
    """)
```

---

## 7. Licensing Considerations

| Tool | License | Commercial Use |
|------|---------|----------------|
| **Crawl4AI** | Apache 2.0 | Permissive |
| **Stagehand** | MIT | Permissive |
| **Skyvern** | AGPL-3.0 | Restrictive (copyleft) |
| **Patchright** | MIT | Permissive |
| **Browserless** | Mix (OSS/Commercial) | Check edition |

---

## 8. Implementation Priorities

### Phase 1: Foundation
1. Deploy shared Patchright browser grid
2. Configure CDP exposure and session persistence
3. Test basic tool connectivity

### Phase 2: Tool Integration
1. Deploy Skyvern with remote browser config
2. Configure Crawl4AI with CDP connection
3. Set up Stagehand with LOCAL_CDP_URL

### Phase 3: MCP Orchestration
1. Build MCP Gateway with tool registry
2. Implement browser locking mechanism
3. Create unified session management

### Phase 4: Production Workflows
1. Design domain-specific workflow templates
2. Implement error recovery and retry logic
3. Add monitoring and alerting

---

## References

- Skyvern Documentation: https://skyvern.com/docs
- Crawl4AI Documentation: https://docs.crawl4ai.com/
- Stagehand: https://github.com/browserbase/stagehand
- MCP Protocol: https://modelcontextprotocol.io/
