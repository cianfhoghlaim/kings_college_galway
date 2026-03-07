# Frontend Stack for Irish Education Platform

## Executive Summary

This document details the edge-native, browser-first architecture for the Irish Leaving Certificate educational platform. The approach shifts computation from centralized servers to the client (WebAssembly) and network edge (Cloudflare), providing instant-start environments, reactive visualizations, and seamless bilingual support.

---

## 1. Architectural Philosophy: Edge-Native Shift

### 1.1 Traditional vs. Proposed Model

| Layer | Traditional (Server-Centric) | Proposed (Edge-Native) |
|-------|------------------------------|------------------------|
| Frontend | Vue.js / React (Node.js) | **TanStack Start** (Edge-rendered) |
| Compute | Bare Metal / Firecracker VMs | **Cloudflare Workers** |
| State | Redis / MongoDB | **Durable Objects** |
| Runtime (Light) | MicroVMs per user | **Marimo WebAssembly** |
| Runtime (Heavy) | MicroVMs | **Self-Hosted Coder** |
| Transport | WebSocket/SSH tunnels | **Durable Objects WebSockets** |

### 1.2 Cost-Performance Advantage

- **Marimo WASM**: Zero server cost for math/Python computation
- **Edge Rendering**: Sub-50ms TTFB from nearest Cloudflare PoP
- **Offline Capability**: Students in rural Ireland can work without connectivity
- **Browser Sandbox**: Isolation by design, no complex iptables configuration

---

## 2. TanStack Start: The Meta-Framework

### 2.1 Why TanStack Start

- **Full-Stack Type Safety**: TypeScript end-to-end
- **Server-Side Rendering with Streaming**: Progressive enhancement for slow connections
- **File-System Routing**: Matches curriculum hierarchy naturally
- **Server Functions**: API endpoints defined alongside UI code

### 2.2 Isomorphic Rendering for Education

```
Student Request (Rural Kerry)
    ↓
Cloudflare Edge (Dublin PoP)
    ↓
Stream HTML immediately (text, definitions, syllabus)
    ↓
Student starts reading
    ↓
JavaScript hydrates (WebGL simulations load)
    ↓
Full interactivity available
```

### 2.3 Type-Safe Syllabus Modeling

```typescript
// Syllabus schema enforced at build time
interface SyllabusNode {
  id: string;           // e.g., "CHEM_1.1"
  subject: Subject;
  strand: string;
  topic: string;
  learning_outcomes: LearningOutcome[];
}

interface Experiment {
  chemicals: Chemical[];
  safety_precautions: string[];
  procedure_steps: Step[];
}
```

Build fails if developer omits mandatory fields like `safety_precautions`.

---

## 3. Bilingual Routing and Localization

### 3.1 URL-Level Internationalization

```
/en/calculus/derivatives
/ga/calcalas/díorthaigh
```

### 3.2 Implementation Strategy

1. **Middleware Detection**: Cloudflare Worker inspects `Accept-Language` header
2. **Streaming Resources**: Load only current language segment (not entire JSON blob)
3. **Terminology Mapping**: KV store for glossary (`"Integer" → "Slánuimhir"`)

### 3.3 Real-Time Bilingual Toggling

Durable Objects broadcast language state changes:
```json
{ "action": "set_lang", "lang": "ga" }
```
All connected clients update UI labels and glossary terms instantly.

---

## 4. Marimo & WebAssembly: Browser-Based Computation

### 4.1 Why Marimo

- **Reactive Notebooks**: Change `x`, and `y = x²` updates instantly
- **No Hidden State**: Enforced dataflow graph (unlike Jupyter)
- **WebAssembly Export**: Full Python environment in static HTML/Wasm bundle

### 4.2 Pedagogical Advantages

**Complex Numbers Visualization**:
- Slider controls θ (argument) and r (modulus)
- Vector rotates on Argand Diagram in real-time
- Python code runs in browser via Pyodide
- Zero server cost

### 4.3 Integration with TanStack Start

```typescript
// Embed Marimo as iframe or web component
// Static assets served from Cloudflare R2

// Communication via PostMessage
window.parent.postMessage({
  type: "TASK_COMPLETE",
  taskId: "calc_deriv",
  payload: { result: 24 }
});
```

---

## 5. Cloudflare Edge Infrastructure

### 5.1 Workers for API Logic

- Replace monolithic Node.js "Foreman"
- Server functions execute at nearest PoP
- Sub-50ms latency for Dublin students

### 5.2 Durable Objects for State

```typescript
export class LabSession implements DurableObject {
  sessions: Map<string, WebSocket>;

  async handleSession(ws: WebSocket) {
    ws.accept();
    ws.addEventListener("message", async (msg) => {
      const event = JSON.parse(msg.data);
      if (event.type === "TASK_SUBMIT") {
        await this.gradeTask(event.payload);
      }
    });
  }
}
```

**Use Cases**:
- WebSocket termination for classroom collaboration
- Teacher broadcast to student Marimo instances
- Presence tracking and telemetry
- Code persistence with automatic resume

### 5.3 Edge-Native Authentication

- **Cloudflare Access** + custom JWT in Worker
- Tokens validated at edge (unauthenticated requests never reach backend)
- GitHub OAuth with profile storage

---

## 6. Heavy Compute: Self-Hosted Coder

### 6.1 When to Use Coder (vs. Marimo)

| Content Type | Solution | Cost |
|-------------|----------|------|
| Math visualization, basic Python | Marimo WASM | $0 |
| Web server hosting, databases | Coder | Server time |
| Embedded systems projects | Coder | Server time |

### 6.2 Coder Template for LCCS

```terraform
resource "coder_agent" "main" {
  arch = "amd64"
  os = "linux"
  startup_script = <<EOT
    # Irish language aliases
    echo "alias liosta='ls -la'" >> /home/coder/.bashrc
    python3 -m http.server 8080 &
  EOT
}

resource "docker_container" "workspace" {
  image = "ghcr.io/leaving-cert/lccs-env:latest"
}
```

### 6.3 Secure Integration

- **Cloudflare Tunnel**: Expose Coder workspaces without public ports
- **OIDC Token Hand-off**: Automatic student login
- **Iframe Embedding**: Coder IDE inside TanStack dashboard

---

## 7. Visualization Libraries

### 7.1 Geography: DuckDB + Deck.gl

**DuckDB Wasm** runs in web worker:
```sql
SELECT * FROM electoral_divisions
WHERE population_density < 10
AND dependency_ratio > 50
```

**Deck.gl** renders results via WebGPU:
- Choropleth maps with smooth color interpolation
- 3D building extrusion for urban analysis
- 60fps on student laptops

### 7.2 Chemistry: MathBox.js + R3F

**MathBox.js** for "3Blue1Brown" aesthetic:
- Render probability density functions (orbitals)
- Transition from Bohr model to Schrödinger model
- Glowing, translucent clouds via custom shaders

**React Three Fiber** for virtual lab:
- 3D burette and conical flask
- Fluid dynamics with Beer's Law shader
- pH-dependent color interpolation

### 7.3 Math: Mafs for Interactive Graphs

```jsx
<Mafs>
  <Coordinates.Cartesian />
  <Plot.OfX y={(x) => Math.sin(x)} />
</Mafs>
```

- Drag variables, see real-time graph updates
- Tight coupling with particle simulations

### 7.4 Geography Maps: MapLibre GL JS

- Vector tile rendering for OS-style topographic maps
- Terrain-RGB for 3D tilt (slope and aspect analysis)
- Essential for Geographical Investigation skills

---

## 8. Subject-Specific Implementations

### 8.1 Chemistry Visualizations

| Syllabus Topic | Technology | Implementation |
|---------------|------------|----------------|
| Atomic Structure | MathBox.js | Orbital probability clouds |
| Bonding | 3Dmol.js | VSEPR shapes, bond angles |
| Equilibrium | R3F + Mafs | Particle sim + live Kc graph |
| Titration | R3F | Virtual lab with fluid shaders |

### 8.2 Geography Tools

| Syllabus Topic | Technology | Implementation |
|---------------|------------|----------------|
| Plate Tectonics | React-Globe.gl | GeoJSON plate boundaries |
| Landforms | Babylon.js | Procedural terrain erosion |
| Demographics | DuckDB + Deck.gl | Census SQL queries |
| Urban Zones | Deck.gl PolygonLayer | 3D land value extrusion |

### 8.3 English Digital Humanities

| Feature | Technology | Implementation |
|---------|------------|----------------|
| Sentiment Analysis | Compromise.js | "Mood Graph" per chapter |
| Character Networks | D3.js | Force-directed relationship graph |
| Annotation | TipTap/ProseMirror | Thematic tagging with persistence |

---

## 9. Pedagogical UX Patterns

### 9.1 Dual Coding (Mayer's Principles)

Text and simulation side-by-side:
```
┌──────────────────┬──────────────────┐
│  Syllabus Text   │   WebGL Canvas   │
│  (scrollable)    │   (synchronized) │
└──────────────────┴──────────────────┘
```

As student scrolls text about "Alkanes", 3D viewer morphs molecule to match.

### 9.2 Scaffolded Interactivity

1. **Observe**: Animation runs automatically
2. **Explore**: Controls unlock for manipulation
3. **Predict**: Simulation pauses, student must predict outcome
4. **Verify**: Simulation resumes, confirms or corrects

### 9.3 AI-Assisted Bilingual Feedback

Using Cloudflare Workers AI (Llama 3):
```
Prompt: "Analyze this Python code. Reply in Irish."
Response: "Maith thú! D'úsáid tú lúb 'for' i gceart..."
```

---

## 10. Progressive Web App Features

### 10.1 Offline Capability

- Service workers cache Marimo WASM bundles
- Students continue math lessons without internet
- Critical for rural Ireland with poor broadband

### 10.2 Virtual Field Notebook (Geography GI)

- PWA section works offline
- Input river width, velocity, bedload size in field
- Auto-sync generates scatter graphs when online

---

## 11. Security and Scalability

### 11.1 Attack Surface

| Architecture | Attack Surface |
|-------------|----------------|
| Server VMs | Root daemon, iptables misconfig |
| Marimo WASM | Student's browser sandbox only |
| Coder | Container escape (mitigated by gVisor) |

### 11.2 Scalability Profile

- **Frontend/WASM**: Infinite scale on Cloudflare CDN
- **Durable Objects**: Auto-distributed globally
- **Coder**: Only ~20% of syllabus needs heavy compute

### 11.3 Cost Analysis

| Component | Monthly Cost |
|-----------|-------------|
| Cloudflare Workers/Pages | $5-20 |
| Durable Objects | Negligible (text sync) |
| Self-hosted Coder | $10-20 |
| **Total** | ~$50/month |

---

## 12. Implementation Roadmap

### Phase 1: Core Framework (Weeks 1-4)
- Initialize TanStack Start project
- PostgreSQL + Drizzle ORM setup
- Implement type-safe syllabus schema

### Phase 2: Geospatial Engine (Weeks 5-10)
- ETL pipelines for CSO/Eurostat → GeoParquet
- DuckDB Wasm integration
- Deck.gl component library

### Phase 3: Science Simulations (Weeks 11-16)
- MathBox.js orbital visualizers
- React Three Fiber virtual lab
- Mafs dynamic graphing

### Phase 4: Content & Accessibility (Weeks 17-24)
- Populate Leaving Cert content
- WCAG 2.1 AA audit
- Keyboard navigation for WebGL
