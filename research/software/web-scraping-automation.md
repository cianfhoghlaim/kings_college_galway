# Web Scraping & Automation

Comprehensive guide to agentic web scraping infrastructure for Celtic language data acquisition.

---

## 1. Stealth Browser Infrastructure

Modern anti-bot systems require hardened browser infrastructure.

### 1.1 Detection Vectors

| Vector | Method | Standard Tool Behavior |
|--------|--------|------------------------|
| **navigator.webdriver** | JS property check | Returns `true` in automated browsers |
| **Runtime.enable Leak** | CDP monitoring | Explicit call triggers detection |
| **Stack Traces** | Error analysis | Reveals automation library |
| **CDP Flags** | Command-line inspection | `--enable-automation` flag |
| **Canvas/WebGL** | GPU fingerprinting | Inconsistent with user agent |

### 1.2 Cloudflare Turnstile Layers

| Layer | Method | Difficulty |
|-------|--------|------------|
| Network (TLS) | JA3/JA4 fingerprinting | High |
| Runtime | `navigator.webdriver` | Medium |
| Behavioral | Mouse/keystroke entropy | High |
| Canvas/WebGL | GPU fingerprinting | Medium |

### 1.3 Patchright Configuration

Patchright patches detection leaks at binary/protocol level.

**Key Patches:**
1. Runtime.enable re-architecture
2. Flag sanitization
3. Console API disabled
4. CDP isolation

**Docker Build:**
```dockerfile
FROM mcr.microsoft.com/playwright/python:v1.49.0-jammy

RUN apt-get update && apt-get install -y \
    xvfb \
    socat \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

RUN pip install patchright && patchright install chromium

RUN useradd -m automation
USER automation
WORKDIR /home/automation

EXPOSE 9222

COPY start_browser.sh /start_browser.sh
ENTRYPOINT ["/bin/bash", "/start_browser.sh"]
```

**Launch Script:**
```bash
#!/bin/bash
Xvfb :99 -screen 0 1920x1080x24 &
export DISPLAY=:99

BROWSER_BIN=$(python3 -c "import patchright; print(patchright.executable_path('chromium'))")

"$BROWSER_BIN" \
  --remote-debugging-port=9222 \
  --remote-debugging-address=0.0.0.0 \
  --user-data-dir=/home/automation/chrome_data \
  --no-first-run \
  --disable-blink-features=AutomationControlled \
  --disable-infobars \
  --start-maximized
```

---

## 2. Agentic Scraping Architecture

### 2.1 Hunter-Gatherer-Operator Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                    MCP ORCHESTRATOR                          │
│              (Tool Discovery & Dispatch)                     │
└─────────────────────────────────────────────────────────────┘
    ↓                    ↓                    ↓
┌──────────┐      ┌──────────┐      ┌──────────┐
│ SKYVERN  │      │STAGEHAND │      │ CRAWL4AI │
│ (Hunter) │      │(Operator)│      │(Gatherer)│
└──────────┘      └──────────┘      └──────────┘
    ↓                    ↓                    ↓
         ┌───────────────────────────┐
         │    PATCHRIGHT GRID       │
         │  (Stealth Browser Pool)  │
         │      CDP Port 9222       │
         └───────────────────────────┘
```

### 2.2 Tool Roles

| Tool | Role | Mechanism | Best For |
|------|------|-----------|----------|
| **Skyvern** | Hunter | Vision LLM + coordinates | Unknown/dynamic UI |
| **Crawl4AI** | Gatherer | BM25 + content pruning | Bulk extraction |
| **Stagehand** | Operator | Cached selectors | Repetitive forms |

### 2.3 Performance Benchmarks

| Tool | Pages/Min | Cost/1K Pages | Use Case |
|------|-----------|---------------|----------|
| Crawl4AI | 100-500 | ~$0 (local) | Discovery, bulk |
| Stagehand (cached) | 50-100 | ~$0.50 | Repetitive |
| Stagehand (cold) | 5-10 | ~$5.00 | New layouts |
| Skyvern | 3-5 | ~$50.00 | Complex visual |

### 2.4 Tool Selection Matrix

| Scenario | Tool | Rationale |
|----------|------|-----------|
| Unknown/Dynamic UI | **Skyvern** | Visual reasoning adapts |
| Repetitive Forms | **Stagehand** | Cached selectors |
| Hierarchical Sites | **Crawl4AI** | Semantic filtering |
| Legacy Deep Web | **Stagehand** | State-dependent |
| CAPTCHA Challenge | **Solver Sidecar** | Token extraction |

---

## 3. Docker Compose Stack

```yaml
version: "3.8"

services:
  browser-grid:
    build: ./browser-grid
    container_name: browser-hub
    shm_size: '2gb'
    cap_add:
      - SYS_ADMIN
    networks:
      - scraping-mesh
    volumes:
      - browser_data:/home/automation/chrome_data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9222/json"]
      interval: 30s
      timeout: 10s
      retries: 3

  solver-service:
    image: theyka/turnstile-solver:latest
    container_name: turnstile-solver
    environment:
      - BROWSER_TYPE=chromium
      - CDP_URL=ws://browser-hub:9222
    networks:
      - scraping-mesh
    depends_on:
      - browser-grid

  mcp-gateway:
    build: ./mcp-server
    container_name: mcp-gateway
    ports:
      - "3000:3000"
    environment:
      - CDP_URL=ws://browser-hub:9222
      - SOLVER_API_URL=http://solver-service:5000/turnstile
    networks:
      - scraping-mesh

networks:
  scraping-mesh:
    driver: bridge

volumes:
  browser_data:
```

---

## 4. Celtic Data Sources

### 4.1 Ireland - Primary Targets

#### ncca.ie (Curriculum)
| Property | Value |
|----------|-------|
| **URL** | https://ncca.ie/en |
| **Type** | Type A - Hierarchical |
| **Language Toggle** | /en/ ↔ /ga/ |

**Structure:** Early Childhood (Aistear) → Primary → Junior Cycle → Senior Cycle

#### examinations.ie (Past Papers)
| Property | Value |
|----------|-------|
| **URL** | https://www.examinations.ie |
| **Type** | Type B - Complex Form |
| **Challenge** | ASP.NET __VIEWSTATE |

**Skyvern Prompt:**
```
GOAL: Download Mathematics Higher Level papers for 2023-2024

INSTRUCTIONS:
1. Select '2023' from Year dropdown
2. Wait for Subject list to update
3. Select 'Mathematics' from Subject
4. Select 'Higher Level' from Level
5. Click 'Search' button
6. Download all PDF links

GUARDRAILS:
- Wait for AJAX completion
- Handle copyright modals
- Only click .pdf links
```

#### canuint.ie (Dialect Archive)
| Property | Value |
|----------|-------|
| **URL** | https://www.canuint.ie/ga/ |
| **Type** | Type C - Spatial |
| **Content** | Audio dialect recordings |

**Geographic Coverage:** Ulster (6), Connacht (16), Leinster (1), Munster (19)

### 4.2 Scotland - Primary Targets

#### SQA Past Papers
| Property | Value |
|----------|-------|
| **URL** | https://www.sqa.org.uk/pastpapers/findpastpaper.htm |
| **Subjects** | Gaidhlig, Gaelic (Learners), Eachdraidh, Matamataig |

#### Tobar an Dualchais
| Property | Value |
|----------|-------|
| **URL** | https://www.tobarandualchais.co.uk |
| **Content** | 50,000+ oral recordings |
| **Filters** | Gaelic, Scots, English |

### 4.3 Wales - Primary Targets

#### Hwb
| Property | Value |
|----------|-------|
| **URL** | https://hwb.gov.wales/curriculum-for-wales/ |
| **Challenge** | Heavy React/JavaScript |

**Structure:** 6 Areas of Learning and Experience (AoLEs), Progression Steps 1-5

#### WJEC Past Papers
| Property | Value |
|----------|-------|
| **URL** | https://www.wjec.co.uk/home/past-papers |
| **Search Terms** | "Welsh Language", "Cymraeg" |

### 4.4 Isle of Man

#### Culture Vannin
| Property | Value |
|----------|-------|
| **URL** | https://www.culturevannin.im |
| **Content** | Manx language materials |
| **Sections** | Watch & Listen, Publications, Learn Manx |

---

## 5. Gaois API Reference

### 5.1 Authentication

```python
@dataclass
class GaoisConfig:
    api_key: str = os.getenv("GAOIS_API_KEY", "")
    base_urls: dict = None

    def __post_init__(self):
        self.base_urls = {
            "logainm": "https://www.logainm.ie/api/v1.0",
            "duchas": "https://www.duchas.ie/api/v0.6"
        }

    @property
    def headers(self) -> dict:
        return {"X-Api-Key": self.api_key}
```

**Methods:**
- HTTP Header: `X-Api-Key: <API_KEY>`
- Query Parameter: `?apiKey=<API_KEY>`
- HTTP Basic Auth: `https://API_KEY@www.logainm.ie/...`

### 5.2 Logainm API v1.0 (Placenames)

| Property | Value |
|----------|-------|
| **Endpoint** | https://www.logainm.ie/api/v1.0 |
| **Content** | 100,000+ placenames |

**Endpoints:**
```
GET /api/v1.0/placenames
GET /api/v1.0/placenames/{id}
GET /api/v1.0/search?q={query}
```

**Response:**
```json
{
  "id": 37704,
  "nameGA": "Baile Hein",
  "nameEN": "Hayestown",
  "category": "townland",
  "coordinates": {"latitude": 53.5285, "longitude": -6.8542},
  "county": "Meath"
}
```

### 5.3 Duchas API v0.6 (Folklore)

| Property | Value |
|----------|-------|
| **Endpoint** | https://www.duchas.ie/api/v0.6 |
| **Status** | Beta |

**Collections:**
- **CBE (Main Collection):** 2,400 bound volumes
- **CBES (Schools):** 740,000 pages (1937-1939)
- **CBEG (Photos):** 80,000 photographs

**Language Filter:** ~66% Irish, ~33% English

**Endpoints:**
```
GET /api/v0.6/collections
GET /api/v0.6/stories?language=ga&county=Cork
```

### 5.4 Direct Downloads

| Source | Format | Size |
|--------|--------|------|
| Parallel Corpus | TMX | 130.5M words |
| Corpas.ie Word Lists | TAB (ZIP) | 240M+ words |

**TMX Parsing:**
```python
from translate.tools import tmxfile

def parse_tmx(filepath: str):
    with open(filepath, 'rb') as f:
        tmx = tmxfile.tmxfile(f)
        for unit in tmx.units:
            yield {
                "source": unit.source,
                "target": unit.target,
                "id": unit.getid()
            }
```

### 5.5 Dataset Size Summary

| Source | Irish Words | English Words | Items |
|--------|-------------|---------------|-------|
| Parallel Corpus | 68M | 62.5M | 130M segments |
| Corpas.ie | 240M | - | - |
| Duchas API | ~50M | ~30M | 80,000+ |
| Logainm API | - | - | 100,000+ |
| Ainm.ie | 1.3M | - | 1,785 |
| Tearma.ie | 100K+ | 100K+ | 10,000+ |
| **Total** | **359M+** | **93M+** | **200K+** |

---

## 6. Site Classification & Workflows

### 6.1 Interaction Types

| Type | Pattern | Block Strategy | Examples |
|------|---------|----------------|----------|
| **A** | Hierarchical drill-down | Navigation Block | ncca.ie, culturevannin.im |
| **B** | Complex form logic | Navigation V2 Block | examinations.ie, sqa.org.uk |
| **C** | Spatial/map traversal | Navigation V2 Block | canuint.ie, peoplescollection.wales |
| **D** | Sequential/paginated | Navigation V2 Block | duchas.ie, tobarandualchais.co.uk |

### 6.2 Skyvern sources.yaml

```yaml
groups:
  - id: irish_educational_framework
    targets:
      - url: "https://www.curriculumonline.ie/Primary/Curriculum-Areas/"
        name: "Irish Primary Curriculum"
        type: "Type_A_Hierarchical"
        priority: high

      - url: "https://ncca.ie/en/junior-cycle/subjects/"
        name: "NCCA Junior Cycle"
        type: "Type_A_Hierarchical"
        priority: high

  - id: scottish_qualifications
    targets:
      - url: "https://www.sqa.org.uk/pastpapers/findpastpaper.htm"
        name: "SQA Past Papers"
        type: "Type_B_Form"
        inputs:
          subjects: ["Gaidhlig", "Gaelic (Learners)"]
          levels: ["National 5", "Higher", "Advanced Higher"]

  - id: welsh_digital_learning
    targets:
      - url: "https://hwb.gov.wales/curriculum-for-wales/"
        name: "Hwb Curriculum"
        type: "Type_D_Sequential"
        notes: "Heavy React. Use wait_for_network_idle"

  - id: celtic_audio_archives
    targets:
      - url: "https://www.canuint.ie/ga/"
        name: "Taisce Chanuinti"
        type: "Type_C_Spatial"
        instruction: "Use text list, not map canvas"

      - url: "https://www.tobarandualchais.co.uk/"
        name: "Tobar an Dualchais"
        type: "Type_D_Sequential"
        filters:
          language: "Gaelic"
```

---

## 7. Implementation Patterns

### 7.1 Shared CDP Connection

```python
from playwright.async_api import async_playwright

async def attach_to_shared_browser(cdp_url: str):
    playwright = await async_playwright().start()
    browser = await playwright.chromium.connect_over_cdp(cdp_url)

    if not browser.contexts:
        context = await browser.new_context()
    else:
        context = browser.contexts[0]

    if not context.pages:
        page = await context.new_page()
    else:
        page = context.pages[0]

    return browser, context, page
```

### 7.2 Concurrency Control

```python
import asyncio
from contextlib import asynccontextmanager

class BrowserLock:
    def __init__(self):
        self._lock = asyncio.Lock()
        self._owner = None

    @asynccontextmanager
    async def acquire(self, tool_name: str):
        await self._lock.acquire()
        self._owner = tool_name
        try:
            yield
        finally:
            self._owner = None
            self._lock.release()
```

### 7.3 Session State Export

```python
async def export_session_state(session):
    browser, context, _ = await attach_to_shared_browser(session.cdp_url)

    return {
        "cookies": await context.cookies(),
        "local_storage": await context.storage_state(),
        "session_id": session.session_id,
        "timestamp": datetime.utcnow().isoformat()
    }
```

### 7.4 Bilingual Page Scraping

```python
async def scrape_bilingual(base_url: str, path: str):
    async with AsyncWebCrawler() as crawler:
        en_result = await crawler.arun(
            url=f"{base_url}/en/{path}",
            extract_structured=True
        )

        ga_result = await crawler.arun(
            url=f"{base_url}/ga/{path}",
            extract_structured=True
        )

        return {
            "path": path,
            "english": en_result.markdown,
            "irish": ga_result.markdown
        }
```

### 7.5 Rate-Limited Collection

```python
async def scrape_with_rate_limit(urls: list, delay: float = 1.0):
    results = []

    async with AsyncWebCrawler() as crawler:
        for url in urls:
            result = await crawler.arun(url=url)
            results.append(result)
            await asyncio.sleep(delay)

    return results
```

---

## 8. MCP Gateway Configuration

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

---

## 9. Gaois API Collector

```python
class GaoisCollector:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.base_urls = {
            "logainm": "https://www.logainm.ie/api/v1.0",
            "duchas": "https://www.duchas.ie/api/v0.6"
        }
        self.headers = {"X-Api-Key": api_key}

    async def _fetch_paginated(self, session, url, params=None):
        page = 1
        params = params or {}

        while True:
            params["page"] = page
            params["per_page"] = 100

            async with session.get(url, headers=self.headers, params=params) as response:
                if response.status != 200:
                    break

                data = await response.json()
                results = data.get("results", [])

                if not results:
                    break

                for item in results:
                    yield item

                page += 1
                await asyncio.sleep(0.5)

    async def collect_placenames(self):
        async with aiohttp.ClientSession() as session:
            url = f"{self.base_urls['logainm']}/placenames"
            return [p async for p in self._fetch_paginated(session, url)]

    async def collect_folklore(self, language=None):
        async with aiohttp.ClientSession() as session:
            url = f"{self.base_urls['duchas']}/stories"
            params = {"language": language} if language else {}
            return [s async for s in self._fetch_paginated(session, url, params)]
```

---

## 10. Ethical Guidelines

### 10.1 robots.txt Compliance

```bash
curl https://www.tearma.ie/robots.txt
curl https://www.logainm.ie/robots.txt
curl https://www.sqa.org.uk/robots.txt
curl https://hwb.gov.wales/robots.txt
```

### 10.2 Rate Limiting

- Default delay: 1 second between requests
- Respect Retry-After headers
- Implement exponential backoff on 429 responses

### 10.3 Licensing

| Tool | License | Commercial Use |
|------|---------|----------------|
| Crawl4AI | Apache 2.0 | Permissive |
| Stagehand | MIT | Permissive |
| Skyvern | AGPL-3.0 | Restrictive (copyleft) |
| Patchright | MIT | Permissive |

---

## References

- Patchright: https://github.com/Kaliiiiiiiiii-Vinyzu/patchright
- Theyka Turnstile Solver: https://github.com/Theyka/Turnstile-Solver
- Skyvern: https://skyvern.com/docs
- Crawl4AI: https://docs.crawl4ai.com/
- Stagehand: https://github.com/browserbase/stagehand
- MCP Protocol: https://modelcontextprotocol.io/
- Gaois APIs: https://docs.gaois.ie/en/data/getting-started
