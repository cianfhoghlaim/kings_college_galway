# Agentic UI learning pipeline: a multi-agent architecture for automated web scraping and design system extraction

**The architecture combines Agno's A2A protocol for multi-agent orchestration, Browserbase for browser automation, Z.AI GLM 4.6V vision tools for UI analysis, Cognee for knowledge graph memory, BAML for structured outputs, and AG-UI for generative UI prototyping.** This system enables autonomous scraping, visual analysis, pattern learning, and UI recreation without pixel-perfect fidelity—instead capturing layout patterns, component hierarchies, and design tokens that evolve into an expert ontology. The pipeline creates domain-scoped knowledge graphs (educational, frontend, documentation) that improve over time through feedback loops and memory enhancement.

---

## The A2A protocol enables cross-agent communication with standardized message formats

Agno's A2A (Agent-to-Agent) protocol provides the foundation for multi-agent orchestration. Built on **JSON-RPC 2.0** over HTTP with Server-Sent Events for streaming, A2A enables agents to discover each other via Agent Cards at `/.well-known/agent.json` and communicate through standardized message schemas.

### Core message structure

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "method": "message/send",
  "params": {
    "message": {
      "messageId": "msg-123",
      "role": "user",
      "parts": [
        {"text": "Analyze this UI screenshot"},
        {"file": {"fileWithUri": "https://...", "mediaType": "image/png"}}
      ]
    }
  }
}
```

The protocol supports **seven task states** (submitted, working, completed, failed, cancelled, rejected, input-required) and three communication patterns: synchronous request/response, SSE streaming, and webhook-based push notifications. For this architecture, streaming is critical—it allows the Vision Agent to emit partial analysis results while Cognee progressively builds the knowledge graph.

### Specialized agent registration

Each agent exposes capabilities through an Agent Card:

```json
{
  "name": "Vision Analysis Agent",
  "skills": [
    {"id": "ui-analysis", "description": "Extract UI components from screenshots"},
    {"id": "pattern-detection", "description": "Identify design patterns and hierarchies"}
  ],
  "defaultInputModes": ["image/png", "application/json"],
  "defaultOutputModes": ["application/json"]
}
```

---

## System architecture: four specialized agents orchestrated by a coordinator team

The pipeline uses Agno's **coordinate** team mode where a team leader orchestrates agents sequentially based on task requirements. This differs from the simpler "route" mode (single agent) or "collaborate" mode (parallel execution) because UI learning requires ordered data flow.

### Agent specialization diagram

| Agent | Role | Tools | Outputs |
|-------|------|-------|---------|
| **Scraper Agent** | Browser control, navigation, screenshot capture | Browserbase SDK, Stagehand | Screenshots, HTML, DOM structure |
| **Vision Agent** | Screenshot analysis, component extraction | Z.AI GLM MCP tools | Structured UI data, BAML-typed components |
| **Memory Agent** | Knowledge graph management, ontology building | Cognee APIs | Graph updates, pattern associations |
| **UI Generator Agent** | Prototype recreation from learned patterns | AG-UI, component library | Generative UI specifications |

### Team configuration

```python
from agno.agent import Agent
from agno.team.team import Team
from agno.models.openai import OpenAIChat
from agno.tools.mcp import MCPTools

# Scraper Agent with Browserbase
scraper_agent = Agent(
    name="Scraper Agent",
    role="Capture screenshots and extract DOM from target URLs",
    model=OpenAIChat(id="gpt-4.1"),
    tools=[MCPTools(
        transport="stdio",
        command="npx",
        args=["@browserbasehq/mcp-server-browserbase"],
        env={"BROWSERBASE_API_KEY": "...", "BROWSERBASE_PROJECT_ID": "..."}
    )],
    instructions=["Navigate to URLs", "Capture full-page screenshots", "Extract DOM structure"]
)

# Vision Agent with Z.AI GLM
vision_agent = Agent(
    name="Vision Agent",
    role="Analyze screenshots to extract UI components and patterns",
    model=OpenAIChat(id="gpt-4.1"),
    tools=[MCPTools(
        transport="stdio",
        command="npx",
        args=["-y", "@z_ai/mcp-server"],
        env={"Z_AI_API_KEY": "...", "Z_AI_MODE": "ZAI"}
    )],
    instructions=["Use ui_to_artifact for component extraction", "Identify layout patterns, not pixel values"]
)

# Memory Agent with Cognee
memory_agent = Agent(
    name="Memory Agent",
    role="Store patterns in domain-scoped knowledge graphs",
    model=OpenAIChat(id="gpt-4.1"),
    tools=[MCPTools(
        transport="http",
        url="http://localhost:8000/cognee-mcp"
    )],
    instructions=["Organize by domain (frontend, educational, documentation)", "Build ontology relationships"]
)

# Orchestration Team
ui_learning_team = Team(
    name="UI Learning Team",
    mode="coordinate",
    model=OpenAIChat(id="gpt-4.1"),
    members=[scraper_agent, vision_agent, memory_agent],
    instructions=["Process URLs sequentially through scrape→analyze→memorize pipeline"],
    send_team_context_to_members=True
)
```

---

## Browserbase provides the scraping infrastructure with stealth and AI integration

Browserbase's serverless browser platform handles anti-detection, CAPTCHA solving, and session persistence—essential for scraping production sites. The **Stagehand** framework adds AI-guided automation.

### Optimal screenshot capture workflow

```python
from browserbase import Browserbase
from playwright.sync_api import sync_playwright
import base64

bb = Browserbase(api_key=os.environ["BROWSERBASE_API_KEY"])

# Create stealth session with context persistence
session = bb.sessions.create(
    project_id=os.environ["BROWSERBASE_PROJECT_ID"],
    proxies=True,
    browser_settings={
        "advanced_stealth": True,
        "solve_captchas": True,
        "viewport": {"width": 1920, "height": 1080}
    }
)

with sync_playwright() as p:
    browser = p.chromium.connect_over_cdp(session.connect_url)
    page = browser.contexts()[0].pages()[0]
    page.goto("https://target-site.com", wait_until="networkidle")
    
    # CDP screenshot (faster than Playwright's built-in)
    client = browser.contexts()[0].new_cdp_session(page)
    screenshot_data = client.send("Page.captureScreenshot", {
        "format": "png",
        "fullPage": True
    })
    
    # Extract accessibility tree (preferred over raw HTML for AI analysis)
    a11y_tree = page.accessibility.snapshot()
    
    # Extract critical CSS
    critical_css = page.evaluate("""() => {
        return Array.from(document.styleSheets)
            .flatMap(sheet => Array.from(sheet.cssRules || []))
            .map(rule => rule.cssText)
            .join('\\n');
    }""")
```

The **Contexts API** persists authentication across sessions—critical for scraping authenticated dashboards or design systems behind login walls.

---

## Z.AI GLM vision tools extract semantic UI structure, not pixels

GLM 4.6V's native multimodal function calling allows screenshots to pass directly to tools without text conversion. The MCP tools provide specialized capabilities:

### Tool selection matrix for UI learning

| Tool | Use Case | Output Structure |
|------|----------|------------------|
| `ui_to_artifact` | Convert screenshot to component specs | HTML/CSS, component hierarchy, design tokens |
| `extract_text_from_screenshot` | Extract labels, headers, content | Structured text with position context |
| `ui_diff_check` | Compare design iterations | Visual diff report, layout drift detection |
| `understand_technical_diagram` | Parse architecture/flow diagrams | Component relationships, data flow |
| `analyze_data_visualization` | Read charts and dashboards | Data points, trends, metric summaries |

### Integration with BAML for structured extraction

The Vision Agent uses BAML schemas to enforce structured output:

```baml
enum ComponentType {
  Button @description("Interactive clickable element")
  Input @description("Text entry field")
  Card @description("Contained content block")
  Navigation @description("Menu or nav bar")
  Container @description("Layout wrapper")
  Modal @description("Overlay dialog")
  Table @description("Tabular data display")
}

class DesignToken {
  name string @description("Token name, e.g., 'color-primary'")
  value string @description("Token value, e.g., '#3B82F6'")
  category "color" | "spacing" | "typography" | "border" | "shadow"
}

class UIComponent {
  id string
  type ComponentType
  variant string? @description("e.g., 'primary', 'outline', 'ghost'")
  layout "flex" | "grid" | "stack" | "absolute" | null
  children UIComponent[]?
  design_tokens DesignToken[]?
  is_interactive bool
  accessibility_label string?
  @@dynamic  // Allow runtime extension for new component types
}

class ScreenAnalysis {
  screen_name string
  platform "web" | "ios" | "android"
  root_component UIComponent
  detected_patterns PatternMatch[]
  extracted_tokens DesignToken[]
}

function AnalyzeUIScreen(screenshot: image) -> ScreenAnalysis {
  client "zai/glm-4.6v"
  prompt #"
    {{ _.role("user") }}
    Analyze this UI screenshot. Focus on:
    1. Component hierarchy (not pixel positions)
    2. Layout patterns (flex, grid, stack)
    3. Design tokens (colors, spacing, typography)
    4. Reusable patterns (cards, nav bars, forms)
    
    Do NOT reproduce exact pixels. Extract semantic structure.
    
    {{ screenshot }}
    {{ ctx.output_format }}
  "#
}
```

---

## Cognee structures domain-scoped knowledge graphs with NodeSets

Cognee's **ECL (Extract, Cognify, Load)** pipeline transforms raw data into queryable knowledge graphs. For this architecture, **NodeSets** provide domain isolation—critical for maintaining separate graphs for educational content, frontend designs, and documentation.

### Domain-scoped memory architecture

```python
import cognee

# Educational sources domain
await cognee.add(
    educational_content,
    node_set=["educational", "programming", "react"]
)

# Frontend design patterns domain  
await cognee.add(
    ui_analysis_results,
    node_set=["frontend", "design-system", "dashboard"]
)

# Software documentation domain
await cognee.add(
    api_documentation,
    node_set=["documentation", "api", "backend"]
)

# Process all into knowledge graphs
await cognee.cognify(
    datasets=["ui_patterns"],
    ontology_file_path="ui_ontology.owl"  # Optional: enforce vocabulary
)

# Enhance with memory algorithms
await cognee.memify()  # Strengthens semantic associations
```

### Custom DataPoints for UI patterns

```python
from cognee.infrastructure.engine import DataPoint

class UIPattern(DataPoint):
    __tablename__ = "ui_pattern"
    pattern_name: str
    component_types: list[str]
    layout_strategy: str
    usage_context: str  # "dashboard", "form", "landing"
    source_urls: list[str]
    metadata: dict = {"index_fields": ["pattern_name", "usage_context"]}

class DesignSystemReference(DataPoint):
    __tablename__ = "design_system"
    system_name: str  # "Material", "Tailwind", "Custom"
    color_palette: dict
    spacing_scale: list[float]
    typography_scale: dict
    components: list["UIPattern"]
    metadata: dict = {"index_fields": ["system_name"]}
```

### Knowledge retrieval for UI generation

```python
from cognee import SearchType

# Graph-based search for related patterns
results = await cognee.search(
    query_text="dashboard card layout with data visualization",
    query_type=SearchType.GRAPH_COMPLETION,
    node_type=NodeSet,
    node_name=["frontend", "dashboard"]
)

# Get relationship insights
insights = await cognee.search(
    query_text="navigation patterns",
    query_type=SearchType.INSIGHTS
)
# Returns: [(NavBar, "contains", MenuItem), (MenuItem, "links_to", Page), ...]
```

---

## Building an expert ontology that improves through feedback loops

The ontology evolves through three mechanisms: explicit OWL definitions, learned relationships from scraping, and user feedback.

### Initial ontology structure (OWL/RDF)

```xml
<?xml version="1.0"?>
<rdf:RDF xmlns="http://uiontology.org#"
         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:owl="http://www.w3.org/2002/07/owl#">
         
  <owl:Class rdf:about="#UIComponent"/>
  <owl:Class rdf:about="#LayoutContainer">
    <rdfs:subClassOf rdf:resource="#UIComponent"/>
  </owl:Class>
  <owl:Class rdf:about="#InteractiveElement">
    <rdfs:subClassOf rdf:resource="#UIComponent"/>
  </owl:Class>
  
  <owl:ObjectProperty rdf:about="#containedIn">
    <rdfs:domain rdf:resource="#UIComponent"/>
    <rdfs:range rdf:resource="#LayoutContainer"/>
  </owl:ObjectProperty>
  
  <owl:ObjectProperty rdf:about="#usesPattern">
    <rdfs:domain rdf:resource="#Screen"/>
    <rdfs:range rdf:resource="#UIPattern"/>
  </owl:ObjectProperty>
</rdf:RDF>
```

### Feedback-driven learning

```python
# Search with interaction tracking
results = await cognee.search(
    query_type=SearchType.GRAPH_COMPLETION_COT,  # Chain-of-thought reasoning
    query_text="What card layouts work best for financial dashboards?",
    save_interaction=True
)

# User provides feedback
await cognee.search(
    query_type=SearchType.FEEDBACK,
    query_text="This pattern recommendation was excellent - the metric card grid works perfectly",
    last_k=1
)

# Periodic enhancement
await cognee.memify()  # Strengthens validated associations
```

### BAML dynamic types for ontology evolution

```python
from baml_client.type_builder import TypeBuilder

# Runtime schema extension as new patterns are learned
tb = TypeBuilder()

# Add newly discovered component types
for new_type in learned_component_types:
    tb.ComponentType.add_value(new_type)

# Extend UIComponent with new properties
tb.UIComponent.add_property("semantic_role", tb.string())
tb.UIComponent.add_property("accessibility_pattern", tb.string().optional())

# Use extended schema for future extractions
analysis = await b.AnalyzeUIScreen(screenshot, {"tb": tb})
```

---

## AG-UI delivers generative prototypes from learned patterns

AG-UI is a **communication protocol**, not a UI generator itself. It transports UI specifications between agents and frontends using **16+ event types** including `TOOL_CALL_*` for triggering UI components and `STATE_*` for synchronization.

### Extending the finance example pattern

The finance example demonstrates tool-based generative UI where agents emit structured data that frontends render:

```python
from dataclasses import dataclass
from agno.run.agent import CustomEvent
from agno.tools import tool

@dataclass
class UIPrototypeEvent(CustomEvent):
    """Custom event carrying UI prototype specification."""
    pattern_name: str
    components: list[dict]
    design_tokens: dict
    suggested_implementation: str  # "react", "vue", "html"

@tool()
async def generate_prototype(
    pattern_query: str,
    target_data: dict,
    style_preference: str = "modern"
):
    """Generate UI prototype from learned patterns."""
    # Query Cognee for matching patterns
    patterns = await cognee.search(
        query_text=pattern_query,
        query_type=SearchType.GRAPH_COMPLETION,
        node_name=["frontend"]
    )
    
    # Emit AG-UI custom event with prototype spec
    yield UIPrototypeEvent(
        pattern_name=patterns[0].pattern_name,
        components=instantiate_pattern(patterns[0], target_data),
        design_tokens=patterns[0].design_tokens,
        suggested_implementation="react"
    )
    
    return f"Generated prototype using {patterns[0].pattern_name} pattern"
```

### Frontend integration with AG-UI hooks

```typescript
// React component listening for prototype events
import { useCopilotAction } from "@copilotkit/react";

useCopilotAction({
  name: "generate_prototype",
  description: "Generate UI prototype from learned patterns",
  parameters: [
    { name: "pattern_query", type: "string" },
    { name: "target_data", type: "object" }
  ],
  render: ({ args, result }) => {
    if (!result) return <LoadingSpinner />;
    
    return (
      <UIPreview
        components={result.components}
        tokens={result.design_tokens}
        onEdit={(changes) => updatePrototype(changes)}
      />
    );
  }
});
```

### State synchronization for collaborative editing

```python
from ag_ui.core import StateSnapshotEvent, StateDeltaEvent

# Emit full state when pattern is loaded
async def load_pattern_state(pattern_id: str):
    pattern = await load_pattern(pattern_id)
    yield StateSnapshotEvent(
        snapshot={
            "currentPattern": pattern.model_dump(),
            "editHistory": [],
            "suggestions": []
        }
    )

# Emit delta when user modifies
async def apply_edit(edit: dict):
    yield StateDeltaEvent(
        delta=[
            {"op": "add", "path": "/editHistory/-", "value": edit},
            {"op": "replace", "path": "/currentPattern/components/0/variant", "value": edit["newVariant"]}
        ]
    )
```

---

## Complete data flow: scrape to prototype pipeline

The full pipeline orchestrates data through all components:

```
┌─────────────────────────────────────────────────────────────────┐
│  1. BROWSERBASE SCRAPER                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │ Navigate │───▶│Screenshot│───▶│  DOM +   │                  │
│  │   URL    │    │  (PNG)   │    │ A11y Tree│                  │
│  └──────────┘    └──────────┘    └──────────┘                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Z.AI GLM VISION ANALYSIS                                    │
│  ┌─────────────────┐    ┌─────────────────┐                    │
│  │ ui_to_artifact  │───▶│ BAML-structured │                    │
│  │ Component Detect│    │ ScreenAnalysis  │                    │
│  └─────────────────┘    └─────────────────┘                    │
│  ┌─────────────────┐    ┌─────────────────┐                    │
│  │ Design Token    │───▶│ DesignToken[]   │                    │
│  │ Extraction      │    │ (colors, spacing)                    │
│  └─────────────────┘    └─────────────────┘                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. COGNEE KNOWLEDGE GRAPH                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  NodeSet: "frontend"                                      │  │
│  │  ┌────────┐     uses_pattern     ┌────────────┐          │  │
│  │  │ Screen │─────────────────────▶│ CardGrid   │          │  │
│  │  └────────┘                      │ Pattern    │          │  │
│  │       │                          └────────────┘          │  │
│  │       │ contains                       │                 │  │
│  │       ▼                                │ has_tokens      │  │
│  │  ┌────────┐                           ▼                 │  │
│  │  │  Card  │────────────────────▶ DesignSystem           │  │
│  │  └────────┘   belongs_to                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. AG-UI PROTOTYPE GENERATION                                  │
│  ┌─────────────────┐    ┌─────────────────┐                    │
│  │ Pattern Query   │───▶│ Graph Search    │                    │
│  │ "dashboard card"│    │ (GRAPH_COMPLETION)                   │
│  └─────────────────┘    └─────────────────┘                    │
│           │                     │                              │
│           ▼                     ▼                              │
│  ┌─────────────────────────────────────────┐                   │
│  │   UIPrototypeEvent via AG-UI SSE        │                   │
│  │   → Frontend renders preview            │                   │
│  │   → User provides feedback              │                   │
│  │   → Cognee strengthens associations     │                   │
│  └─────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation: orchestrating the full workflow

### Workflow definition with Agno

```python
from typing import Iterator
from agno.workflow import Workflow
from agno.agent import Agent, RunResponse

class UILearningWorkflow(Workflow):
    scraper = Agent(name="Scraper", tools=[browserbase_mcp])
    vision = Agent(name="Vision", tools=[zai_mcp])
    memory = Agent(name="Memory", tools=[cognee_mcp])
    generator = Agent(name="Generator", output_schema=UIPrototype)
    
    def run(self, urls: list[str], domain: str) -> Iterator[RunResponse]:
        for url in urls:
            # Step 1: Scrape
            scrape_result = yield from self.scraper.run(
                f"Navigate to {url}, capture full-page screenshot and accessibility tree"
            )
            self.session_state["screenshot"] = scrape_result.screenshot
            self.session_state["a11y_tree"] = scrape_result.a11y_tree
            
            # Step 2: Analyze
            analysis = yield from self.vision.run(
                f"Analyze screenshot using ui_to_artifact. Extract components, layout patterns, design tokens.",
                images=[self.session_state["screenshot"]]
            )
            self.session_state["analysis"] = analysis
            
            # Step 3: Memorize
            yield from self.memory.run(
                f"Add to Cognee with node_set=['{domain}']. Create relationships between patterns.",
                data=self.session_state["analysis"]
            )
        
        # Step 4: Generate prototype from learned patterns
        prototype = yield from self.generator.run(
            f"Query patterns from domain '{domain}' and generate prototype specification"
        )
        
        yield RunResponse(content=prototype, run_id=self.run_id)
```

### AgentOS deployment with all interfaces

```python
from agno.os import AgentOS
from agno.os.interfaces.a2a import A2A
from agno.os.interfaces.agui import AGUI

workflow = UILearningWorkflow()

agent_os = AgentOS(
    description="UI Learning and Prototyping System",
    workflows=[workflow],
    interfaces=[
        A2A(agents=[workflow.scraper, workflow.vision, workflow.memory, workflow.generator]),
        AGUI(workflow=workflow)
    ]
)

if __name__ == "__main__":
    agent_os.serve(app="main:app", port=8000, reload=True)
```

---

## Configuration and environment setup

### Required environment variables

```bash
# Browserbase
BROWSERBASE_API_KEY=bb_live_...
BROWSERBASE_PROJECT_ID=proj_...

# Z.AI GLM
Z_AI_API_KEY=zai_...

# Cognee
COGNEE_API_KEY=cognee_...
GRAPH_DATABASE_PROVIDER=neo4j
GRAPH_DATABASE_URL=bolt://localhost:7687
VECTOR_DB_PROVIDER=qdrant
VECTOR_DB_URL=localhost:6333

# LLM (for Agno orchestration)
OPENAI_API_KEY=sk-...
```

### MCP server configuration

```json
{
  "mcpServers": {
    "browserbase": {
      "command": "npx",
      "args": ["@browserbasehq/mcp-server-browserbase"],
      "env": {
        "BROWSERBASE_API_KEY": "${BROWSERBASE_API_KEY}",
        "BROWSERBASE_PROJECT_ID": "${BROWSERBASE_PROJECT_ID}"
      }
    },
    "zai-vision": {
      "command": "npx", 
      "args": ["-y", "@z_ai/mcp-server"],
      "env": {
        "Z_AI_API_KEY": "${Z_AI_API_KEY}",
        "Z_AI_MODE": "ZAI"
      }
    },
    "web-reader": {
      "type": "http",
      "url": "https://api.z.ai/api/mcp/web_reader/mcp",
      "headers": {"Authorization": "Bearer ${Z_AI_API_KEY}"}
    }
  }
}
```

---

## Conclusion: key architectural decisions and tradeoffs

This architecture makes several deliberate choices that optimize for long-term learning over short-term accuracy:

**Semantic over pixel fidelity**. The Vision Agent extracts component types, layout strategies, and design tokens—not exact CSS values. This produces reusable patterns rather than brittle reproductions.

**Domain scoping via NodeSets**. Cognee's NodeSet mechanism isolates educational content from frontend designs from documentation. Queries can cross boundaries when needed (`query_text="what UI patterns are discussed in React tutorials?"`) but default to domain-specific retrieval.

**Feedback-driven ontology evolution**. The combination of explicit OWL definitions, BAML dynamic types, and Cognee's `memify()` creates an ontology that grows with usage. The `save_interaction=True` and `SearchType.FEEDBACK` patterns close the learning loop.

**Protocol-first integration**. A2A for agent communication, MCP for tool exposure, and AG-UI for frontend streaming provide clean boundaries between components. Any agent can be replaced without rewriting the pipeline.

The critical path for implementation is: **Browserbase session management → Z.AI MCP tool configuration → Cognee NodeSet schema → BAML output types → AG-UI event handlers**. Starting with the finance example's `YFinanceTools` pattern and replacing with the UI analysis tools provides a working baseline within a day.