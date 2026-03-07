# Building an Interactive Learning Platform for Irish Mathematics

A hybrid architecture combining browser-based notebooks, edge orchestration, and remote development environments can deliver the flexibility needed for Leaving Certificate mathematics and computer science education. **Marimo's WebAssembly export enables instant, zero-installation interactivity** for lightweight exercises, while **Cloudflare Durable Objects provide globally-distributed session state**, and **Coder workspaces deliver full development environments** for advanced coursework—all orchestrated through a **TanStack Start frontend** deployed at the edge.

## Marimo WASM delivers instant interactivity with key constraints

Marimo notebooks run entirely in-browser via Pyodide, eliminating server infrastructure for basic exercises. The architecture consists of a Python kernel running in a WebWorker, a TypeScript frontend issuing commands, and an RPC bridge replacing traditional server communication. For Leaving Certificate mathematics, all essential packages are fully supported: **NumPy, SciPy, SymPy, Matplotlib**, and pandas work out of the box with automatic installation when imported.

Three embedding approaches serve different needs. **iframe embedding** with `sandbox="allow-scripts allow-same-origin"` provides the simplest integration while enabling localStorage persistence for student progress. **Marimo Islands** embed individual reactive cells directly into lesson pages—ideal for interactive tutorials where explanations and computations interweave. The **marimo-snippets NPM package** converts static code examples into live notebooks, perfect for documentation sites.

Technical constraints shape where WASM fits in the architecture:

- **Memory ceiling of 2GB** limits dataset sizes and complex simulations
- **Single-threaded execution** means no parallel processing (though adequate for educational workloads)
- **Persistent storage relies on localStorage** when embedding with `allow-same-origin`; without server integration, progress saving is limited to browser storage or manual export
- **Initial load times of several seconds** occur as Pyodide downloads (~tens of MB for the scientific stack)
- **Chrome delivers best performance** and compatibility for WASM execution

For an Irish educational context, marimo handles bilingual content naturally—markdown cells support Irish language text, LaTeX renders mathematical notation in either language, and UI elements can be labeled bilingually through standard internationalization patterns.

## Cloudflare Durable Objects enable stateful edge coordination

Durable Objects solve the critical challenge of maintaining user progress and coordinating sessions across a globally-distributed platform. Each DO combines compute with **strongly-consistent storage** (now up to 10GB via SQLite backend), with single-threaded execution guaranteeing serialized access—essential for preventing race conditions in progress tracking.

For WebSocket connections powering terminal sessions or collaborative features, the **WebSocket Hibernation API** dramatically reduces costs. Without hibernation, maintaining 100 WebSocket connections with periodic messages costs approximately **$139/month**; with hibernation, the same workload drops to roughly **$10/month**. The DO hibernates while connections remain live on Cloudflare's network, only waking to process messages:

```javascript
export class StudentSessionDO extends DurableObject {
  async fetch(request) {
    const [client, server] = Object.values(new WebSocketPair());
    this.ctx.acceptWebSocket(server);
    server.serializeAttachment({ studentId: "123", lessonId: "calculus-01" });
    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(ws, message) {
    const { studentId } = ws.deserializeAttachment();
    // Process terminal input, update progress
  }
}
```

**Orchestrating Coder workspaces from Workers** requires a hybrid pattern. Enable Smart Placement to run Workers geographically closer to your Coder infrastructure, reducing round-trip latency for API calls. Workers handle authentication, rate limiting, and session routing at the edge, then proxy workspace creation and terminal connections to self-hosted Coder servers. Authentication tokens flow through Workers as intermediaries—the Worker validates the student's JWT, then issues requests to Coder's API with service credentials stored as secrets.

Rate limiting for educational platforms works best with **per-user Durable Objects** rather than global limiters (which create bottlenecks). Each student's rate limit state lives in their session DO, enabling fair resource allocation without single-point contention.

## Coder workspaces provide full development environments

For advanced coursework requiring persistent file systems, IDE integration, or complex dependencies, Coder delivers enterprise-grade cloud development environments. The architecture separates **coderd** (control plane managing workspace lifecycle) from **provisionerd** (executing Terraform to create infrastructure) and the **Coder Agent** (running inside workspaces providing SSH, port forwarding, and health checks).

Workspace templates use Terraform, enabling precise control over educational environments:

```hcl
resource "coder_agent" "main" {
  os   = "linux"
  arch = "amd64"
  startup_script = <<-EOF
    pip install numpy scipy sympy matplotlib jupyter
    # Pre-configure Irish language locales
    sudo locale-gen ga_IE.UTF-8
  EOF
}

module "jupyter" {
  source   = "registry.coder.com/modules/jupyter/coder"
  agent_id = coder_agent.main.id
}
```

**Coder's REST API enables complete programmatic control**—create workspaces on-demand when students begin advanced labs, configure per-course quotas, and monitor usage. The `/workspaces/{id}/watch` endpoint provides real-time status updates via Server-Sent Events, enabling the frontend to show workspace readiness.

Compared to iximiuz Labs' Firecracker approach, Coder trades **sub-second boot times (~125ms for Firecracker) for richer features**: native VS Code and JetBrains integration, built-in persistent volumes, and Terraform's infrastructure flexibility. For educational use where workspaces run for hours and IDE experience matters, Coder's tradeoffs favor developer productivity over cold-start speed.

## TanStack Start orchestrates the frontend at the edge

TanStack Start, now in Release Candidate stage with official Cloudflare Workers support, provides the frontend framework. Its **client-first architecture with full-document SSR** suits educational platforms with heavy interactivity—quizzes, simulations, and embedded notebooks benefit from client-side execution, while course listings and marketing pages leverage server rendering for SEO.

Cloudflare deployment uses the new Vite plugin approach:

```typescript
// vite.config.ts
import { cloudflare } from '@cloudflare/vite-plugin'
import { tanstackStart } from '@tanstack/react-start/plugin/vite'

export default defineConfig({
  plugins: [
    cloudflare({ viteEnvironment: { name: 'ssr' } }),
    tanstackStart(),
  ],
})
```

**Server functions bridge edge and origin** seamlessly. Create authenticated endpoints that validate student sessions, query progress from Durable Objects, or proxy requests to Coder:

```typescript
export const createWorkspace = createServerFn({ method: 'POST' })
  .inputValidator(z.object({ templateId: z.string(), courseId: z.string() }))
  .handler(async ({ data }) => {
    const coderResponse = await fetch(`${env.CODER_URL}/api/v2/workspaces`, {
      headers: { 'Coder-Session-Token': env.CODER_TOKEN },
      body: JSON.stringify({ template_id: data.templateId })
    });
    return coderResponse.json();
  });
```

For bilingual Irish/English content, TanStack Router's type-safe search parameters elegantly handle language selection: `?lang=ga` persists through navigation, enabling deep-linkable bilingual states. Course content can load from language-specific files based on this parameter.

## Translating iximiuz Labs patterns to Cloudflare architecture

iximiuz Labs' Foreman/Conductor/Bender architecture offers valuable patterns adaptable to this hybrid approach. Their **separation of concerns** maps directly:

| iximiuz Component | Cloudflare Equivalent | Function |
|-------------------|----------------------|----------|
| Foreman | Cloudflare Workers + D1/KV | Orchestration, auth, content management |
| Conductor | Durable Objects (WebSocket) | Terminal sessions, real-time communication |
| Bender | Coder API | Workspace provisioning and management |
| Examiner | Custom agent in Coder workspace | Solution checking, task verification |

The **DAG-based task execution** for automatic solution checking translates well. Define exercise verification as YAML task graphs, execute them via a lightweight daemon inside Coder workspaces, and report completion through WebSocket to Durable Objects. gRPC provides reliable state synchronization between verification agents and the coordination layer—more robust than polling.

**Warm pools** apply differently: rather than pre-spawning Firecracker VMs, maintain pre-warmed Coder workspace images with all dependencies installed. Coder's template caching and prebuilt images achieve similar faster-startup goals.

## Recommended hybrid architecture

The optimal architecture layers browser, edge, and origin:

```
┌─────────────────── Browser Layer ───────────────────┐
│  TanStack Start SPA        Marimo WASM Notebooks   │
│  (Interactive UI)          (Lightweight exercises)  │
└───────────────────────────┬─────────────────────────┘
                            │
┌─────────────── Cloudflare Edge Layer ───────────────┐
│  Workers (API Gateway)    Durable Objects           │
│  - Auth/JWT validation    - User progress (SQLite)  │
│  - Rate limiting          - WebSocket sessions      │
│  - Coder orchestration    - Notebook state sync     │
│                                                     │
│  KV (Content cache)       D1 (Course metadata)     │
└───────────────────────────┬─────────────────────────┘
                            │ Smart Placement
┌─────────────── Self-Hosted Origin ──────────────────┐
│  Coder Control Plane      PostgreSQL                │
│  - Workspace lifecycle    - User data               │
│  - Template management    - Progress persistence    │
│                                                     │
│  Coder Workers (Kubernetes/Docker)                  │
│  - Student workspaces with Python/math tools        │
│  - JupyterLab, VS Code integration                 │
│  - Solution verification agents                     │
└─────────────────────────────────────────────────────┘
```

**Content routing by complexity**: Leaving Certificate Paper 1 (algebra, functions, calculus) works beautifully in Marimo WASM—students manipulate equations, visualize graphs, and solve problems without any server infrastructure. Paper 2 (geometry, statistics, probability) similarly fits the in-browser model. Computer science coursework requiring file persistence, package installation beyond Pyodide's capabilities, or full IDE features escalates to Coder workspaces.

## Answering key technical questions

**Marimo WASM constraints**: The 2GB memory limit and single-threaded execution suffice for educational mathematics—even intensive symbolic computation in SymPy runs comfortably. State persistence requires localStorage (limited to ~5MB per origin) or implementing custom sync to your backend via `fetch()` calls from notebook code. Packages with complex native dependencies unavailable in Pyodide (like TensorFlow) won't work, but the core scientific Python stack is complete.

**Durable Objects and WebSockets**: DO's Hibernation API handles terminal session multiplexing efficiently. Each student session gets its own DO with hibernating WebSocket connections; the 2KB `serializeAttachment` limit stores session metadata, while the SQLite backend stores progress data. For collaborative features (shared notebooks, study groups), create per-room DOs that broadcast messages to all connected participants.

**Workers proxying Coder**: Effective but requires careful timeout handling—workspace creation can take 30-60 seconds, exceeding Workers' default CPU limits. Use Workers to initiate workspace creation and return immediately with a tracking ID, then have clients poll a separate status endpoint or receive updates via WebSocket from a Durable Object monitoring the Coder API.

**Authentication patterns**: Implement OAuth (Google, GitHub, or Irish educational SSO) at the Workers layer. After authentication, issue short-lived JWTs validated at the edge. For Coder access, Workers exchange user JWTs for Coder API tokens stored as secrets, never exposing Coder credentials to clients.

**Bilingual content in notebooks**: Marimo markdown cells render both English and Irish text natively. For dynamic language switching, use `mo.ui.dropdown` to select language, then conditionally display content:

```python
lang = mo.ui.dropdown(["English", "Gaeilge"], value="English")
content = {"English": "Solve for x:", "Gaeilge": "Réitigh do x:"}
mo.md(content[lang.value])
```

## Implementation priorities for the Irish Leaving Certificate

Start with **Marimo WASM for immediate impact**—no infrastructure required. Export notebooks covering key syllabus topics (quadratics, trigonometry, differentiation) and host on Cloudflare Pages. This validates content approaches and gathers user feedback before investing in Coder infrastructure.

**Phase two adds progress tracking** via Durable Objects. Each student's DO stores completed exercises, quiz scores, and checkpoint states. The SQLite backend handles complex queries like "show all students struggling with integration" for teacher dashboards.

**Phase three introduces Coder workspaces** for computer science components—Python programming, data structures, and computational problem-solving requiring persistent environments.

Throughout, the TanStack Start frontend unifies the experience, embedding Marimo iframes for interactive content, managing authentication, and routing students between browser-based exercises and full development environments based on lesson requirements. The architecture scales from a single developer serving hundreds of students to institutional deployments serving thousands, with costs dominated by Coder infrastructure rather than edge compute.