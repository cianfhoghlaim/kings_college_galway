# Stealth Browser Infrastructure

## Executive Summary

Modern anti-bot systems like Cloudflare Turnstile, DataDome, and Akamai employ sophisticated fingerprinting techniques that easily identify standard automation tools. This document details the construction of a hardened browser infrastructure using Patchright, CDP architecture, and containerized deployment for reliable web automation.

---

## 1. The Detection Arms Race

### 1.1 Standard Automation Tool Leaks

Standard browser automation tools leak their identity through multiple vectors:

| Detection Vector | Method | Standard Tool Behavior |
|-----------------|--------|----------------------|
| **navigator.webdriver** | JavaScript property check | Returns `true` in automated browsers |
| **Runtime.enable Leak** | CDP command monitoring | Explicit call triggers detection |
| **Stack Traces** | Error analysis | Reveals automation library presence |
| **CDP Flags** | Command-line inspection | `--enable-automation` flag present |
| **Canvas/WebGL** | GPU fingerprinting | Inconsistent with claimed user agent |

### 1.2 Cloudflare Turnstile Detection Layers

| Layer | Detection Method | Difficulty |
|-------|-----------------|------------|
| **Network (TLS)** | JA3/JA4 fingerprinting | High |
| **Runtime** | `navigator.webdriver` | Medium |
| **Behavioral** | Mouse/keystroke entropy | High |
| **Canvas/WebGL** | GPU fingerprinting | Medium |

---

## 2. Patchright: The Hardened Browser Kernel

**Patchright** is a modified Playwright distribution that patches detection leaks at the binary and protocol level. Unlike stealth plugins that inject JavaScript to hide properties, Patchright modifies the browser's internal behavior.

### 2.1 Key Patches

1. **Runtime.enable Patch**: Re-architects script injection to avoid triggering detection
2. **Flag Sanitization**: Strips automation flags, adds user-like flags
3. **Console API Disabled**: Prevents debug output detection
4. **CDP Isolation**: Executes JavaScript in invisible isolated contexts

### 2.2 Docker Container Build

```dockerfile
# Base image: Playwright with Python (includes dependencies)
FROM mcr.microsoft.com/playwright/python:v1.49.0-jammy

# Install utilities and Xvfb for virtual display
RUN apt-get update && apt-get install -y \
    xvfb \
    socat \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

# Install Patchright Python package
RUN pip install patchright && patchright install chromium

# Create automation user
RUN useradd -m automation
USER automation
WORKDIR /home/automation

# Expose CDP port
EXPOSE 9222

# Entrypoint script
COPY start_browser.sh /start_browser.sh
ENTRYPOINT ["/bin/bash", "/start_browser.sh"]
```

### 2.3 Browser Launch Script

```bash
#!/bin/bash
# Start Xvfb for "headed" mode in headless container
# "Headed" mode is significantly stealthier than "Headless" mode
Xvfb :99 -screen 0 1920x1080x24 &
export DISPLAY=:99

# Locate Patchright Chromium Binary
BROWSER_BIN=$(python3 -c "import patchright; print(patchright.executable_path('chromium'))")

echo "Launching Patchright Chromium Listener on 0.0.0.0:9222..."

"$BROWSER_BIN" \
  --remote-debugging-port=9222 \
  --remote-debugging-address=0.0.0.0 \
  --user-data-dir=/home/automation/chrome_data \
  --no-first-run \
  --no-default-browser-check \
  --disable-blink-features=AutomationControlled \
  --disable-infobars \
  --start-maximized \
  --window-size=1920,1080
```

---

## 3. Shared CDP Gateway Architecture

The core innovation is centralizing the stateful browser while keeping automation tools stateless.

### 3.1 Architecture Diagram

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

### 3.2 Docker Compose Stack

```yaml
version: "3.8"

services:
  # Stealth Browser Hub
  browser-grid:
    build: ./browser-grid
    container_name: browser-hub
    shm_size: '2gb'  # Required for Chromium
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

  # Turnstile Solver Sidecar
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

  # MCP Gateway
  mcp-gateway:
    build: ./mcp-server
    container_name: mcp-gateway
    ports:
      - "3000:3000"
    environment:
      - CDP_URL=ws://browser-hub:9222
      - SOLVER_API_URL=http://solver-service:5000/turnstile
    volumes:
      - ./config/mcp_config.json:/etc/mcp/config.json
    networks:
      - scraping-mesh

networks:
  scraping-mesh:
    driver: bridge

volumes:
  browser_data:
```

### 3.3 Memory and Resource Management

| Resource | Requirement | Rationale |
|----------|-------------|-----------|
| **shm_size** | 2GB minimum | Chromium shared memory requirement |
| **SYS_ADMIN** | Capability | Required for sandbox operations |
| **User Data Volume** | Persistent | Session/cookie persistence across restarts |

---

## 4. Cloudflare Turnstile Mitigation

### 4.1 Theyka Solver Integration

The solver operates as an MCP tool, invoked when Turnstile is detected:

```python
from patchright.sync_api import sync_playwright

class TurnstileSolver:
    def __init__(self, cdp_url: str):
        self.cdp_url = cdp_url

    async def solve(self, page_context: str) -> str:
        """
        Connect to shared browser and solve Turnstile challenge.
        Returns clearance token on success.
        """
        with sync_playwright() as p:
            browser = p.chromium.connect_over_cdp(self.cdp_url)
            context = browser.contexts[0]
            page = context.pages[0]

            # Locate Turnstile iframe
            turnstile = page.frame_locator("iframe[src*='challenges.cloudflare.com']")

            # Execute human-like interaction
            checkbox = turnstile.locator("input[type='checkbox']")
            await self._human_click(checkbox)

            # Wait for clearance
            await page.wait_for_selector("[data-turnstile-response]", timeout=30000)

            return page.get_attribute("[data-turnstile-response]", "value")

    async def _human_click(self, element):
        """Simulate human-like mouse movement and click."""
        box = await element.bounding_box()
        # Add entropy to click position
        import random
        x = box['x'] + box['width'] * random.uniform(0.3, 0.7)
        y = box['y'] + box['height'] * random.uniform(0.3, 0.7)
        await element.page.mouse.move(x, y, steps=random.randint(10, 25))
        await asyncio.sleep(random.uniform(0.1, 0.3))
        await element.page.mouse.click(x, y)
```

### 4.2 CapSolver API Alternative

For high-volume scenarios where local solving is inconsistent:

```python
from crawl4ai import AsyncWebCrawler, CrawlerRunConfig

async def solve_with_capsolver(crawler: AsyncWebCrawler, page):
    """
    Hook into Crawl4AI to inject CapSolver token.
    """
    script = """
    (async () => {
        const response = await fetch('https://api.capsolver.com/createTask', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                clientKey: '%s',
                task: {
                    type: 'TurnstileTaskProxyLess',
                    websiteURL: window.location.href,
                    websiteKey: document.querySelector('[data-sitekey]').dataset.sitekey
                }
            })
        });
        const result = await response.json();
        // Force turnstile callback with token
        window.turnstile.execute(result.solution.token);
    })();
    """ % CAPSOLVER_API_KEY

    await page.evaluate(script)
```

---

## 5. Session Persistence and State Management

### 5.1 The "Active Tab" Strategy

When multiple tools share a browser, they must attach to the same context:

```python
from playwright.async_api import async_playwright

async def attach_to_shared_browser(cdp_url: str):
    """
    Connect to shared CDP gateway and attach to active context.
    """
    playwright = await async_playwright().start()
    browser = await playwright.chromium.connect_over_cdp(cdp_url)

    # Critical: Reuse existing context, don't create new one
    if not browser.contexts:
        context = await browser.new_context()
    else:
        context = browser.contexts[0]

    # Attach to active page
    if not context.pages:
        page = await context.new_page()
    else:
        page = context.pages[0]

    return browser, context, page
```

### 5.2 Concurrency Control

The MCP Gateway implements locking to prevent race conditions:

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

    @property
    def is_locked(self) -> bool:
        return self._lock.locked()

    @property
    def owner(self) -> str | None:
        return self._owner
```

---

## 6. Operational Resilience

### 6.1 VNC Debugging

Add VNC server for visual debugging:

```dockerfile
# Add to browser-grid Dockerfile
RUN apt-get install -y x11vnc

# In start_browser.sh
x11vnc -display :99 -forever -shared -rfbport 5900 &
```

### 6.2 Health Checks and Recovery

```yaml
# docker-compose health monitoring
healthcheck:
  test: |
    curl -sf http://localhost:9222/json/version || exit 1
    # Check for zombie processes
    pgrep -x chromium > /dev/null || exit 1
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 10s
```

### 6.3 Logging and Observability

```python
# MCP Gateway logging configuration
import structlog

logger = structlog.get_logger()

# Log all tool invocations
@app.middleware("http")
async def log_requests(request, call_next):
    logger.info(
        "mcp_tool_invoked",
        tool=request.headers.get("X-MCP-Tool"),
        target_url=request.json().get("url"),
        timestamp=datetime.utcnow().isoformat()
    )
    response = await call_next(request)
    return response
```

---

## 7. Implementation Priorities

### Phase 1: Core Infrastructure
1. Build Patchright Docker image with Xvfb
2. Configure CDP exposure on 0.0.0.0:9222
3. Test basic browser connectivity

### Phase 2: Solver Integration
1. Deploy Theyka solver as sidecar
2. Implement MCP tool wrapper
3. Test against Cloudflare-protected sites

### Phase 3: Production Hardening
1. Add VNC debugging capability
2. Implement browser lock mechanism
3. Configure health checks and auto-restart
4. Set up logging and alerting

---

## References

- Patchright GitHub: https://github.com/Kaliiiiiiiiii-Vinyzu/patchright
- Theyka Turnstile Solver: https://github.com/Theyka/Turnstile-Solver
- Playwright Docker: https://playwright.dev/python/docs/docker
- CDP Protocol: https://chromedevtools.github.io/devtools-protocol/
