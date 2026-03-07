# **Architectural Blueprint for Autonomous Deep Research Agents: Replicating SaaS Capabilities with a Self-Hosted Google ADK, MCP, and Gemini 3 Stack**

## **Executive Summary**

The evolution of automated web interaction has transitioned from fragile, selector-based scripting to autonomous, vision-driven agency. While managed platforms like Browserbase, Firecrawl, and Stagehand offer robust "agent-as-a-service" capabilities, enterprise requirements for data privacy, cost control, and granular customizability are driving the development of sophisticated self-hosted stacks. This report articulates a comprehensive architectural blueprint for developing a proprietary deep research stack that rivals commercial alternatives.  
By leveraging the **Google Agent Development Kit (ADK)** as the orchestration layer, **Model Context Protocol (MCP)** as the universal connectivity standard, and **Gemini 3.0** (Flash and Pro) alongside **GLM-4.6v** as the cognitive engines, developers can construct a modular, scalable research system. This architecture integrates best-in-class open-source tools—**Crawl4AI** for high-speed extraction, **Skyvern** for visual navigation, **Patchright** for anti-bot stealth, and **Chrome DevTools** for precise state inspection—into a unified "Super-Browser" environment.  
The analysis provided herein details not only the implementation of these individual components but also the complex integration patterns required to make them operate as a cohesive unit. We explore advanced concepts such as "Shared State" middleware for synchronization between agent logic and user interfaces (Ag-UI), hybrid fallback patterns that prioritize low-cost local execution before defaulting to premium SaaS APIs, and the utilization of Gemini 3’s multimodal reasoning to overcome dynamic web obfuscation. This report serves as an exhaustive guide for architects seeking to build resilient, self-hosted deep research agents.

## ---

**1\. The Commercial Landscape: Benchmarking the "Gold Standard"**

To engineer a superior self-hosted stack, one must first deconstruct the capabilities of the leading SaaS providers. These platforms have defined the current state-of-the-art in agentic web interaction, establishing a feature set that our proprietary stack must emulate and eventually surpass.

### **1.1 Firecrawl: The Autonomous Research Agent**

Firecrawl has introduced a paradigm shift with its /agent endpoint, moving beyond simple HTML extraction to autonomous navigation.

* **Prompt-Driven Extraction:** The core value proposition is the ability to accept a high-level natural language prompt (e.g., "Find the pricing tier for enterprise users") and autonomously determine the navigational steps required to retrieve that data. This implies a recursive planning loop where the agent analyzes the current page, identifies relevant links, and decides whether to traverse deeper.1  
* **Deep Web Search:** Unlike traditional scrapers that require a seed URL, the /agent endpoint can perform open-ended research, likely leveraging a search engine API (like Google Search) to discover entry points before engaging in DOM-level navigation.  
* **Self-Correction:** The agent monitors its own progress. If a path yields no data (e.g., a 404 error or a paywall), it backtracks and attempts an alternative route. This resilience is a critical requirement for our self-hosted planner.

### **1.2 Stagehand: The Natural Language Browser Controller**

Stagehand, often integrated with Browserbase, focuses on the "Act" and "Observe" primitives.

* **browserbase\_stagehand\_act**: This tool abstracts the complexity of DOM interaction. Instead of page.click('\#submit-btn'), the agent issues the command act(action="click the login button"). The underlying system uses heuristic analysis or vision models to resolve "login button" to a specific coordinate or element handle.2  
* **browserbase\_stagehand\_observe**: This feature provides the agent with a semantic understanding of the page. It identifies actionable elements (forms, navigation bars, content areas) and presents them to the LLM, filtering out noise like tracking pixels and layout spacers.  
* **Session Management:** The ability to hand off sessions (browserbase\_session\_create) allows for long-running workflows where state (cookies, local storage) is preserved across distinct agent execution cycles.

### **1.3 Browserbase: The Infrastructure Layer**

Browserbase provides the managed browser execution environment. Its key differentiator is "Stealth-as-a-Service."

* **Anti-Bot Evasion:** It automatically rotates proxies, solves CAPTCHAs, and manages browser fingerprints (User-Agent, Canvas noise, WebGL vendor strings) to appear as a legitimate human user.  
* **Debuggability:** It offers live view sessions and comprehensive logs (network HAR files, console logs), which are essential for diagnosing why an agent failed on a specific site.

### **1.4 Synthesis of Requirements for the Self-Hosted Stack**

Based on this analysis, our self-hosted stack must deliver:

1. **Stealth Execution:** A browser runtime that mimics human fingerprints (Target: Patchright).  
2. **Visual Reasoning:** The ability to "see" and interact with UI elements via natural language (Target: Skyvern \+ Z.ai Vision).  
3. **Semantic Extraction:** Converting raw HTML/screenshots into structured data (Target: Crawl4AI).  
4. **Autonomous Orchestration:** A planner that can loop, retry, and navigate (Target: Google ADK \+ Gemini 3).  
5. **Universal Interface:** A standardized way to connect these tools (Target: MCP).

## ---

**2\. The Cognitive Core: Google Agent Development Kit (ADK) & Gemini 3**

The "brain" of our autonomous research system is the Google Agent Development Kit (ADK), utilizing Python as the primary language. ADK moves beyond simple prompt chaining to establish a rigorous framework for agentic behavior, state management, and tool delegation.

### **2.1 ADK Architecture for Deep Research**

The ADK framework operates on a modular design philosophy that separates the "Reasoning Engine" from the "Capability Layer." For deep research, we utilize the LlmAgent primitive configured with specific directives.

* **The Agent Primitive:** An agent in ADK is defined by its instruction (system prompt), model (reasoning core), and tools. The instruction set for a research agent must be robust, including directives for:  
  * **Hypothesis Generation:** "Formulate search queries to find X."  
  * **Source Verification:** "Cross-reference data points from at least two domains."  
  * **Termination Conditions:** "Stop when 80% of the requested fields are populated."  
* **Tool Abstraction & MCP Integration:** ADK's McpToolset is the pivotal component. It allows the agent to treat local Python functions, remote HTTP endpoints, and Dockerized services as uniform "Tools." This abstraction enables the "Hybrid Fallback" pattern, where the agent sees two tools—scrape\_local and scrape\_remote—and learns to choose between them based on context.3

### **2.2 Gemini 3.0: The Reasoning Engine**

The choice of model dictates the agent's planning capability. Gemini 3.0 offers distinct advantages for this architecture.

* **Gemini 3.0 Flash (The "Vibe Coder"):** Optimized for speed and cost-efficiency, Flash is ideal for the inner loops of browsing—checking a URL, validating a selector, or reading a short snippet. It supports high-frequency interaction, essentially allowing the agent to "browse" at the speed of a human clicking through a site, but with the cognitive overhead of an LLM.5  
* **Gemini 3.0 Pro (The "Deep Thinker"):** For the overarching strategy, Gemini 3.0 Pro provides "PhD-level reasoning." It is deployed at the initialization phase to decompose the user query into a research plan and at the synthesis phase to compile thousands of extracted data points into a coherent report.7  
* **Thinking Level & Dynamic Reasoning:** ADK allows configuring the thinking\_level parameter. For complex navigational hurdles (e.g., a multi-step wizard), the agent can be switched to "High" thinking mode, engaging internal chain-of-thought processing to solve logic puzzles or complex authentication flows before committing to an action.8

### **2.3 Integrating GLM-4.6v for Specialized Vision**

While Gemini 3 acts as the generalist orchestrator, the **GLM-4.6v** model (via Z.ai's GLM Coding Plan) provides specialized vision capabilities. GLM-4.6v excels in high-resolution visual grounding, making it the ideal candidate for the "Observe" phase of the loop.

* **Visual Debugging:** When the browser encounters a rendering error or a visual anti-bot challenge (e.g., "Click the images containing a traffic light"), GLM-4.6v can interpret the screenshot with higher fidelity than text-only models, providing the specific coordinates for interaction.9

## ---

**3\. The Connectivity Layer: Model Context Protocol (MCP)**

The Model Context Protocol (MCP) serves as the "universal bus" for our stack. It standardizes the interface between the ADK agent (Client) and the various capabilities (Servers), preventing vendor lock-in and simplifying the architecture.

### **3.1 MCP Architecture: Client-Server Topology**

In our design, the ADK agent acts as the **MCP Client**, while the various tools (Crawl4AI, Skyvern, Chrome DevTools, Z.ai) function as **MCP Servers**.

* **Transport Mechanisms:**  
  * **Stdio (Standard I/O):** Used for tools running directly on the host machine or simple local processes.  
  * **SSE (Server-Sent Events):** Critical for our Dockerized architecture. Services like Crawl4AI and Skyvern run in isolated containers and expose their MCP interfaces via HTTP/SSE. The ADK agent connects to these streams (e.g., http://crawl4ai:8000/sse), enabling real-time, bi-directional communication across container boundaries.10

### **3.2 Z.ai MCP Suite Integration**

The Z.ai suite provides a set of pre-built MCP servers that enhance the agent's sensory and analytical capabilities.

* **Vision MCP (vision-mcp-server):** This server wraps the GLM-4.6v capabilities. It exposes tools like ui\_to\_artifact (converting a UI screenshot into a code specification or description) and analyze\_data\_visualization (reading charts/graphs). This allows the agent to "read" visual data that Crawl4AI might miss in the HTML.9  
* **Search MCP (search-mcp-server):** Provides the webSearchPrime tool. This is essential for the planning phase, allowing the agent to discover target URLs before initiating the deep crawl. It acts as the "Top of Funnel" for the research process.11  
* **Reader MCP (reader-mcp-server):** specialized for converting cluttered web pages into clean, LLM-digestible text, similar to the "Reader Mode" in browsers but accessible via API.

### **3.3 Chrome DevTools MCP: The Diagnostic Layer**

The Chrome DevTools MCP server is unique; it doesn't just "do" things, it "inspects" things. It gives the agent access to the browser's internal state.

* **Tools Exposed:** list\_console\_messages, list\_network\_requests, take\_snapshot.  
* **Use Case:** If Crawl4AI returns an empty body, the agent uses DevTools MCP to inspect the network\_requests. If it sees a 403 Forbidden or a request to a known CAPTCHA provider (e.g., Turnstile), it infers a block and triggers the fallback logic. This "self-diagnostic" capability is what separates a fragile script from a robust agent.12

## ---

**4\. The Self-Hosted Infrastructure: Building the "Super-Browser"**

The heart of this project is the self-hosted browser infrastructure. To rival Browserbase, we must engineer a "Super-Browser" container that combines stealth, control, and accessibility.

### **4.1 The Core: Patchright Chromium**

Standard automated browsers (Puppeteer/Playwright) emit signals (e.g., navigator.webdriver \= true) that make them easily detectable. **Patchright** is a modified distribution of Playwright that patches these leaks at the binary and protocol level.

* **Stealth Features:** It removes Runtime.enable leaks, masks command-line flags, and mimics human interaction patterns (cursor jitter, typing latency).  
* **Dockerization Strategy:** We build a Docker image based on Patchright that launches a browser instance in "Server Mode." Crucially, it must expose the **Chrome DevTools Protocol (CDP)** port (typically 9222\) to the Docker network.  
  * *Configuration:* The container launch command must include \--remote-debugging-port=9222 and \--remote-debugging-address=0.0.0.0 to allow external containers (Skyvern, Crawl4AI) to connect to it.14

### **4.2 The Navigator: Skyvern**

Skyvern replaces fragile XPath/CSS selectors with computer vision. It takes a screenshot, overlays "Set-of-Marks" (bounding boxes with IDs) on interactive elements, and uses an LLM (Gemini 3 or GLM-4.6v via Z.ai Vision) to decide which element to interact with.

* **Connection to Patchright:** Instead of launching its own browser, Skyvern is configured to connect to the Patchright container via CDP (CDP\_URL=ws://patchright-browser:9222). This ensures that Skyvern navigates the *same* session that Crawl4AI will later extract from.17  
* **MCP Interface:** The Skyvern MCP exposes tools like navigate, click, type, and scroll. The ADK agent uses these to traverse complex UIs (e.g., multi-step forms, infinite scrolls).

### **4.3 The Extractor: Crawl4AI**

Crawl4AI is the high-performance data ingestion engine. It is optimized to convert raw HTML into clean Markdown, stripping away navigation bars, ads, and footers to conserve context window tokens.

* **Configuration:** Like Skyvern, Crawl4AI is configured to attach to the shared Patchright CDP session. This allows for a workflow where Skyvern navigates to a state (e.g., "Logged In Dashboard"), and Crawl4AI immediately extracts the content without needing to re-authenticate.19  
* **Extraction Strategies:** It supports LLM-based extraction (using Gemini Flash) to parse unstructured text into JSON objects based on a schema defined by the ADK agent.21

## ---

**5\. Architectural Implementation: Developing the Agent Stack**

This section details the specific logic and code structures required to implement the "Site Browser Agent" using Google ADK.

### **5.1 Defining the Agent Logic**

The agent is implemented as a Python class inheriting from ADK's LlmAgent. It is equipped with a McpToolset that aggregates tools from all our services.

Python

\# Conceptual implementation of the Research Agent  
from google.adk.agents import LlmAgent  
from google.adk.tools.mcp\_tool import McpToolset, SseServerParams  
from google.adk.models import Gemini3Flash

\# 1\. Connect to Self-Hosted MCPs via SSE  
crawl\_toolset \= McpToolset(  
    connection\_params=SseServerParams(url="http://crawl4ai-service:8000/sse")  
)  
skyvern\_toolset \= McpToolset(  
    connection\_params=SseServerParams(url="http://skyvern-service:8000/sse")  
)  
devtools\_toolset \= McpToolset(  
    connection\_params=SseServerParams(url="http://chrome-devtools:8000/sse")  
)  
vision\_toolset \= McpToolset(  
    connection\_params=SseServerParams(url="http://zai-vision:8000/sse")  
)

\# 2\. Define the Agent  
research\_agent \= LlmAgent(  
    model=Gemini3Flash(thinking\_level="high"),  
    name="DeepResearchBot",  
    instruction="""  
    You are an autonomous research agent. Your goal is to navigate websites,  
    extract data, and synthesize findings.  
      
    PHASE 1: PLANNING  
    \- Use 'webSearchPrime' (Z.ai) to find relevant URLs.  
    \- Filter URLs based on the user's research criteria.  
      
    PHASE 2: NAVIGATION & OBSERVATION  
    \- Use 'skyvern\_navigate' to load pages.  
    \- Use 'devtools\_snapshot' \+ 'ui\_to\_artifact' (Z.ai) to understand page structure if unknown.  
    \- Use 'skyvern\_interact' to click/type/scroll.  
      
    PHASE 3: EXTRACTION  
    \- Once the target data is visible, use 'crawl4ai\_extract' to get markdown.  
    \- Verify data quality. If poor, retry navigation.  
      
    PHASE 4: FALLBACK  
    \- If a 'BotDetectionError' or '403 Forbidden' occurs, invoke the 'firecrawl\_agent' tool.  
    """,  
    tools=\[crawl\_toolset, skyvern\_toolset, devtools\_toolset, vision\_toolset\]  
)

### **5.2 Replicating "Stagehand" Capabilities**

To mimic Stagehand's act() and observe() features, we build composite tools in ADK.

* **The observe() Composite:**  
  1. **Trigger:** Agent calls observe\_page().  
  2. **Action:** The tool triggers devtools.take\_screenshot.  
  3. **Processing:** The screenshot is sent to Z.ai's vision-mcp with the prompt: "Identify all interactive elements and return their coordinates and semantic purpose."  
  4. **Result:** The tool returns a JSON list of elements (e.g., {"id": 1, "type": "button", "text": "Login", "coords": }) to the agent's context.  
* **The act() Composite:**  
  1. **Trigger:** Agent calls act(instruction="Click the login button").  
  2. **Action:** The tool sends this instruction to Skyvern.  
  3. **Execution:** Skyvern uses its internal vision logic to find the element matching "login button" and executes a click on the shared Patchright browser.

## ---

**6\. The Hybrid Fallback Architecture: Balancing Cost and Reliability**

A purely self-hosted stack will inevitably face blocks on sophisticated sites. A robust enterprise architecture utilizes a "Tiered Execution" strategy to manage this risk while optimizing costs.

### **6.1 The Tiered Logic**

We implement a custom "Meta-Tool" in ADK called smart\_scrape that encapsulates this logic.

| Tier | Technology | Cost | Reliability | Trigger Condition |
| :---- | :---- | :---- | :---- | :---- |
| **Tier 1** | **Self-Hosted (Patchright \+ Crawl4AI)** | \~$0.001/run | Medium | Default starting point. |
| **Tier 2** | **Self-Hosted \+ Residential Proxy** | \~$0.05/run | High | Triggered if Tier 1 returns 403/Captcha. |
| **Tier 3** | **SaaS API (Firecrawl/Browserbase)** | \~$0.10+/run | Very High | Triggered if Tier 2 fails or complex JS renders incorrectly. |

### **6.2 Error Handling and Routing in ADK**

The ADK framework allows for sophisticated exception handling within tool definitions.

Python

def smart\_scrape(url: str, prompt: str) \-\> str:  
    """  
    Attempts to scrape a URL using a tiered fallback strategy.  
    """  
    \# Tier 1: Local Attempt  
    try:  
        result \= local\_crawl\_tool.invoke({"url": url})  
        if "captcha" in result.content.lower() or result.status\_code \== 403:  
            raise BotDetectionError("Detected anti-bot mechanism")  
        return result  
    except (BotDetectionError, TimeoutError) as e:  
        print(f"Tier 1 failed: {e}. Escalating to Tier 3 SaaS.")  
          
        \# Tier 3: SaaS Fallback (Skipping Tier 2 for brevity)  
        try:  
            \# Fallback to Firecrawl /agent endpoint for full autonomy  
            return firecrawl\_tool.invoke({"url": url, "prompt": prompt})  
        except Exception as saas\_error:  
            return f"CRITICAL FAILURE: All tiers failed. {saas\_error}"

This logic ensures that the agent is "Budget-Aware." It attempts the free route first but has the autonomy to spend budget on premium tools when necessary to fulfill the user's request.

## ---

**7\. Frontend Integration: The Ag-UI Shared State Pattern**

For deep research, users require visibility into the agent's progress. The Ag-UI "Shared State" pattern synchronizes the agent's internal state with a frontend interface.

### **7.1 Middleware Implementation**

We implement a custom ADK Middleware that intercepts the agent's thought process and tool execution events.

* **State Object:** A JSON structure held in a shared store (e.g., Redis) containing keys like current\_url, status (Planning/Browsing/Extracting), logs, and last\_screenshot.  
* **The Middleware Loop:**  
  1. **On Tool Start:** When Skyvern begins navigation, the middleware updates status="Navigating" and broadcasts this via WebSocket to the UI.  
  2. **On Screenshot:** When a screenshot is taken, it is base64 encoded and pushed to the last\_screenshot state, allowing the user to see a "Live View" of the agent's browser.  
  3. **Human-in-the-Loop (HITL):** If the agent confidence drops below a threshold (e.g., "I am unsure if this is the correct 'Pricing' page"), it sets status="Waiting\_Approval". The middleware pauses execution until a user clicks "Approve" in the UI, which updates the state and resumes the agent.22

## ---

**8\. Deployment & Docker Composition**

The complexity of this stack requires a rigorous Docker Compose configuration to ensure networking and resource sharing between the various services.

### **8.1 The Docker Compose Architecture**

The following docker-compose.yml structure defines the "Super-Browser" ecosystem.

YAML

version: '3.8'  
services:  
  \# 1\. The Super-Browser (Stealth Patchright)  
  patchright-browser:  
    image: dylangroos/patchright-mcp:latest  
    command: launch-server \--remote-debugging-port=9222 \--remote-debugging-address=0.0.0.0  
    ports:  
      \- "9222:9222" \# Exposed for local debugging  
    cap\_add:  
      \- SYS\_ADMIN \# Required for sandbox  
    shm\_size: '2gb' \# Prevent crashes on heavy pages  
    networks:  
      \- research-net

  \# 2\. Crawl4AI (Extraction Service)  
  crawl4ai-service:  
    image: unclecode/crawl4ai:latest  
    environment:  
      \# Connects to the shared browser container  
      \- CDP\_URL=ws://patchright-browser:9222/devtools/browser  
      \- MCP\_PORT=8001  
    depends\_on:  
      \- patchright-browser  
    networks:  
      \- research-net

  \# 3\. Skyvern (Navigation Service)  
  skyvern-service:  
    image: skyvern/skyvern:latest  
    environment:  
      \- BROWSER\_TYPE=cdp-connect  
      \- CDP\_URL=ws://patchright-browser:9222/devtools/browser  
      \- LLM\_PROVIDER=gemini  
      \- GEMINI\_API\_KEY=${GEMINI\_API\_KEY}  
    depends\_on:  
      \- patchright-browser  
    networks:  
      \- research-net

  \# 4\. Z.ai Vision MCP  
  zai-vision:  
    image: zai/vision-mcp:latest  
    environment:  
      \- Z\_AI\_API\_KEY=${Z\_AI\_API\_KEY}  
    networks:  
      \- research-net

  \# 5\. The ADK Agent (Orchestrator)  
  adk-agent:  
    build:./agent\_code  
    environment:  
      \- GOOGLE\_API\_KEY=${GOOGLE\_API\_KEY}  
      \# SSE Connections to all tools  
      \- MCP\_URLS=http://crawl4ai-service:8001/sse,http://skyvern-service:8002/sse,http://zai-vision:8003/sse  
    depends\_on:  
      \- crawl4ai-service  
      \- skyvern-service  
    networks:  
      \- research-net

networks:  
  research-net:  
    driver: bridge

### **8.2 Operational Considerations**

* **Data Locality:** By running these containers on the same Docker network (or Cloud Run VPC), we minimize latency. Heavy payloads (HTML, Screenshots) transfer instantly between the Browser and the Extractor without traversing the public internet.  
* **Scaling:** For production, the adk-agent and patchright-browser services can be scaled independently. A "Fleet" architecture would involve a load balancer distributing requests to a pool of patchright-browser instances, managed by Kubernetes or Google Cloud Run.

## ---

**9\. Future Outlook and Strategic Implications**

The transition to "Agentic Web" interactions is accelerating. As websites implement more sophisticated "Proof-of-Humanity" checks, the "Stealth" layer (Patchright) will require continuous updates. Decoupling it into its own container ensures that the agent logic remains stable even as the underlying browser infrastructure evolves.  
Furthermore, the integration of multimodal models like Gemini 3 and GLM-4.6v fundamentally changes the economics of extraction. We are moving away from maintaining thousands of fragile regex/XPath selectors towards a world where agents simply "look" at a page and understand it. This self-hosted stack represents a strategic investment in that future—providing the privacy of local execution, the power of enterprise SaaS, and the flexibility of open-source software.  
This architecture empowers developers to own their research destiny, reducing dependency on external vendors while maintaining the agility to adapt to the rapidly evolving digital landscape.

## ---

**10\. Appendix: Configuration Reference Tables**

### **10.1 Feature Comparison: Self-Hosted vs. SaaS**

| Feature | Self-Hosted Stack (Proposed) | SaaS (Firecrawl/Browserbase) |
| :---- | :---- | :---- |
| **Navigation** | Visual (Skyvern) \+ Stealth (Patchright) | Visual \+ Heuristic |
| **Extraction** | LLM-based (Crawl4AI \+ Gemini) | LLM-based |
| **Stealth** | Patchright (Local) \+ Proxies | Managed Fingerprinting |
| **Cost** | Compute \+ Tokens (\~$0.01/run) | Per Page (\~$0.05 \- $0.10/run) |
| **Privacy** | Data stays in VPC | Data traverses vendor cloud |
| **Customization** | Infinite (Full Code Access) | Limited via API |

### **10.2 Recommended Tool Configuration**

| Tool | Recommended Mode | Key Settings |
| :---- | :---- | :---- |
| **Crawl4AI** | Custom CDP | browser\_mode="custom", cdp\_url="ws://..." |
| **Skyvern** | CDP Connect | BROWSER\_TYPE="cdp-connect", LLM="gemini-flash" |
| **Patchright** | Server Mode | \--remote-debugging-address=0.0.0.0 |
| **ADK Agent** | High Thinking | thinking\_level="high", temperature=0.2 |

#### **Works cited**

1. Agent | Firecrawl \- Firecrawl Docs, accessed December 28, 2025, [https://docs.firecrawl.dev/features/agent](https://docs.firecrawl.dev/features/agent)  
2. Browserbase MCP Server Tools \- Stagehand, accessed December 28, 2025, [https://docs.stagehand.dev/v3/integrations/mcp/tools](https://docs.stagehand.dev/v3/integrations/mcp/tools)  
3. Hello World Agent — Build Your First AI Agent with Google ADK, accessed December 28, 2025, [https://medium.com/@raphael.mansuy/hello-world-agent-build-your-first-ai-agent-with-google-adk-026a36d9605b](https://medium.com/@raphael.mansuy/hello-world-agent-build-your-first-ai-agent-with-google-adk-026a36d9605b)  
4. google/adk-python: An open-source, code-first Python ... \- GitHub, accessed December 28, 2025, [https://github.com/google/adk-python](https://github.com/google/adk-python)  
5. Gemini 3 Flash for Enterprises | Google Cloud Blog, accessed December 28, 2025, [https://cloud.google.com/blog/products/ai-machine-learning/gemini-3-flash-for-enterprises](https://cloud.google.com/blog/products/ai-machine-learning/gemini-3-flash-for-enterprises)  
6. Gemini 3 Flash: frontier intelligence built for speed \- Google Blog, accessed December 28, 2025, [https://blog.google/products/gemini/gemini-3-flash/](https://blog.google/products/gemini/gemini-3-flash/)  
7. Google Gemini 3 Benchmarks (Explained) \- Vellum AI, accessed December 28, 2025, [https://www.vellum.ai/blog/google-gemini-3-benchmarks](https://www.vellum.ai/blog/google-gemini-3-benchmarks)  
8. Gemini 3: Google's Most Powerful LLM \- DataCamp, accessed December 28, 2025, [https://www.datacamp.com/blog/gemini-3](https://www.datacamp.com/blog/gemini-3)  
9. Vision MCP Server \- Overview \- Z.AI DEVELOPER DOCUMENT, accessed December 28, 2025, [https://docs.z.ai/devpack/mcp/vision-mcp-server](https://docs.z.ai/devpack/mcp/vision-mcp-server)  
10. Use Google ADK and MCP with an external server | Google Cloud Blog, accessed December 28, 2025, [https://cloud.google.com/blog/topics/developers-practitioners/use-google-adk-and-mcp-with-an-external-server](https://cloud.google.com/blog/topics/developers-practitioners/use-google-adk-and-mcp-with-an-external-server)  
11. Web Search MCP Server \- Overview \- Z.AI DEVELOPER DOCUMENT, accessed December 28, 2025, [https://docs.z.ai/devpack/mcp/search-mcp-server](https://docs.z.ai/devpack/mcp/search-mcp-server)  
12. How Google Chrome DevTools MCP Empowers AI Debugging with Real Time Performance Insights \- KailashPathak, accessed December 28, 2025, [https://kailash-pathak.medium.com/how-google-chrome-devtools-mcp-empowers-ai-debugging-with-real-time-performance-insights-8d2f58c1c4d4](https://kailash-pathak.medium.com/how-google-chrome-devtools-mcp-empowers-ai-debugging-with-real-time-performance-insights-8d2f58c1c4d4)  
13. ChromeDevTools/chrome-devtools-mcp \- GitHub, accessed December 28, 2025, [https://github.com/ChromeDevTools/chrome-devtools-mcp](https://github.com/ChromeDevTools/chrome-devtools-mcp)  
14. How to Scrape with Patchright and Avoid Detection \- ZenRows, accessed December 28, 2025, [https://www.zenrows.com/blog/patchright](https://www.zenrows.com/blog/patchright)  
15. dylangroos/patchright-mcp-lite: Patchright (Playwright patch) MCP server for lightweight models \- GitHub, accessed December 28, 2025, [https://github.com/dylangroos/patchright-mcp-lite](https://github.com/dylangroos/patchright-mcp-lite)  
16. Docker \- Playwright, accessed December 28, 2025, [https://playwright.dev/docs/docker](https://playwright.dev/docs/docker)  
17. Unlocking Browser Automation: A Deep Dive into the Official Skyvern MCP Server, accessed December 28, 2025, [https://skywork.ai/skypage/en/browser-automation-skyvern-mcp/1977611439104790528](https://skywork.ai/skypage/en/browser-automation-skyvern-mcp/1977611439104790528)  
18. Skyvern-AI/skyvern: Automate browser based workflows ... \- GitHub, accessed December 28, 2025, [https://github.com/Skyvern-AI/skyvern](https://github.com/Skyvern-AI/skyvern)  
19. Browser, Crawler & LLM Config \- Crawl4AI Documentation (v0.7.x), accessed December 28, 2025, [https://docs.crawl4ai.com/core/browser-crawler-config/](https://docs.crawl4ai.com/core/browser-crawler-config/)  
20. Self-Hosting Guide \- Crawl4AI Documentation (v0.7.x), accessed December 28, 2025, [https://docs.crawl4ai.com/core/self-hosting/](https://docs.crawl4ai.com/core/self-hosting/)  
21. Quick Start \- Crawl4AI Documentation (v0.7.x), accessed December 28, 2025, [https://docs.crawl4ai.com/core/quickstart/](https://docs.crawl4ai.com/core/quickstart/)  
22. Demo Viewer by CopilotKit \- AG-UI Dojo, accessed December 28, 2025, [https://dojo.ag-ui.com/adk-middleware/feature/shared\_state?openCopilot=true](https://dojo.ag-ui.com/adk-middleware/feature/shared_state?openCopilot=true)  
23. Delight users by combining ADK Agents with Fancy Frontends using AG-UI, accessed December 28, 2025, [https://developers.googleblog.com/delight-users-by-combining-adk-agents-with-fancy-frontends-using-ag-ui/](https://developers.googleblog.com/delight-users-by-combining-adk-agents-with-fancy-frontends-using-ag-ui/)