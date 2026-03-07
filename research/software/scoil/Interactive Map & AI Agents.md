# **Architectural Blueprint for "Celtic OS": A Spatial-Interactive Learning Environment for the British Isles**

## **1\. Introduction: The Paradigm of the Product Operating System**

The evolution of web interfaces has reached an inflection point where the traditional distinction between "websites" and "applications" has largely dissolved. However, a new dichotomy has emerged: the linear, page-based navigational model versus the spatial, object-oriented "desktop" metaphor. The user's request to replicate the frontend architecture of PostHog 1 signals a desire to move towards the latter—a **"Product OS"** paradigm. This report analyzes the architectural requirements for building **"Celtic OS"**, a comprehensive, interactive geospatial platform for the British Isles that integrates high-performance mapping of Celtic languages with generative AI educational agents.  
The core challenge of this undertaking lies in the convergence of three distinct technical domains: **Complex Window Management** (emulating a desktop OS in the browser), **High-Performance Geospatial Rendering** (visualizing dense census data for Wales, Cornwall, Scotland, Ireland, and the Isle of Man), and **Real-Time Generative AI** (managing stateful conversations with language tutors).  
By leveraging a stack comprised of **TanStack Start**, **Shadcn UI**, **Convex**, **DuckDB Spatial**, and **Lonboard** (via its underlying technologies), we can construct a system that transcends the limitations of standard dashboards. This report details the theoretical underpinnings, technical implementation strategies, and aesthetic considerations necessary to bring this vision to life, ensuring a user experience that is both intellectually rigorous and playfully engaging.

## ---

**2\. Deconstructing the PostHog "Product OS" Interface**

To replicate the "interactive theme" and "opening items" mechanic of PostHog, one must understand that PostHog does not function as a traditional Single Page Application (SPA) where the router swaps out the entire view container. Instead, it operates as a **Window Manager**.

### **2.1 The Philosophy of Spatial Multitasking**

PostHog’s design philosophy, termed "Product OS," addresses the fragmentation of developer tools.4 In a traditional workflow, a user might have one tab for analytics, another for feature flags, and a third for session replays. PostHog consolidates these into a single "desktop" where these tools coexist as floating windows.  
This spatial arrangement is crucial for the "Celtic OS" concept. When studying linguistic geography, context is paramount. A user examining the decline of Cornish speakers in *Penwith* 6 needs to simultaneously view the statistical breakdown (Data Window) and converse with an AI tutor about the specific dialectal variations (Chat Window). A linear website forces the user to navigate away from the map to read the text, breaking the cognitive link. The windowing system preserves this link by allowing the map to remain visible (perhaps maximized or tiled) while the auxiliary tools float above it.

### **2.2 The Mechanics of the Windowing System**

Research into PostHog’s frontend codebase and architecture documentation 2 reveals that the "desktop" effect is achieved through a global state management system that supersedes the browser's native windowing.  
**Core Components of the Architecture:**

* **The Desktop Canvas:** The root of the application is a fixed-viewport container that captures all pointer events. It disables native scrolling, treating the browser window as a static frame.  
* **The Window State Store:** A central store (likely implementing a flux-like pattern or using React Context) maintains an array of active Window objects. Each object contains critical metadata:  
  * id: Unique identifier (e.g., window-map-cornwall).  
  * component: The React component to render (e.g., \<InteractiveMap region="cornwall" /\>).  
  * geometry: The { x, y, width, height } values relative to the canvas.  
  * zIndex: An integer determining the stacking order.  
  * status: Enumerated state (OPEN, MINIMIZED, MAXIMIZED).  
* **The Router as a State Driver:** Crucially, the URL in PostHog often reflects the *active* window or a specific layout configuration. Navigating to /pricing typically mounts the Pricing component inside a new window frame rather than replacing the root view.2

Interaction Physics:  
The "playful" feel 3 is derived from the physics of the interaction. Windows snap to edges; they have momentum when dragged; minimizing them triggers an animation that sucks the window into a specific coordinate on the "Taskbar" or "Dock." Replicating this requires a physics-based animation library (like Framer Motion) integrated tightly with the drag-and-drop logic.

### **2.3 The "Retro-Brutalist" Aesthetic**

PostHog’s visual language is a hybrid of "pixel art" nostalgia and "brutalist" clarity.8 This is characterized by:

* **High Contrast Borders:** Elements often feature 2px–4px solid black borders, mimicking the stark delineations of early GUIs like Windows 3.1 or the classic Macintosh OS.  
* **Hard Shadows:** Shadows are not diffuse gaussian blurs but solid, offset blocks of color (often black or dark grey), creating a sense of depth without softness.  
* **Pixel Art Assets:** The mascot (the Hedgehog) and icons are rendered in pixel art, invoking a sense of fun and approachability that contrasts with the serious nature of the data (analytics).8 This is directly applicable to the "Celtic OS," where the subject matter (endangered languages) is serious, but the engagement model should be encouraging and gamified.

## ---

**3\. Frontend Architecture: The Window Manager Implementation**

Translating the PostHog paradigm into the specified stack (**TanStack Start**, **Shadcn UI**, **Convex**) requires a specific architectural approach. We are effectively building an Operating System shell within the browser.

### **3.1 The Shell Architecture with TanStack Start**

**TanStack Start** serves as the backbone for routing and server-side data fetching.10 While traditionally used for page-based routing, here it will function as the "bootloader" for the OS.  
The Root Layout (\_\_root.tsx):  
The root layout must provide the DesktopProvider context to the entire application. This context holds the state of the windows.

TypeScript

// Conceptual Structure for DesktopProvider  
type DesktopState \= {  
  windows: Record\<string, WindowInstance\>;  
  activeWindowId: string | null;  
  taskbarItems: string;  
  openWindow: (component: ReactNode, meta: WindowMeta) \=\> void;  
  closeWindow: (id: string) \=\> void;  
  minimizeWindow: (id: string) \=\> void;  
  bringToFront: (id: string) \=\> void;  
};

The Outlet from TanStack Router will primarily be used to handle deep links. If a user visits celticos.com/map/wales, the router should parse this URL and, upon mounting, immediately dispatch an openWindow action to the DesktopProvider with the \<WalesMap /\> component, effectively "booting" the OS with the map open.

### **3.2 Draggable Windows: Shadcn UI \+ dnd-kit**

**Shadcn UI** provides the visual primitives, specifically the Card component, which serves as the perfect base for a window frame.12 However, standard HTML elements are static. To make them draggable and interactive, we must integrate **dnd-kit**.13  
Why dnd-kit?  
Unlike older libraries like react-draggable, dnd-kit uses a sensor-based architecture.15 This is critical for complex windows containing interactive elements (like a map). We can define a MouseSensor and TouchSensor that only activate when the user drags the Header of the window. This prevents the user from accidentally dragging the window while trying to pan the map inside it.  
Component Composition:  
The WindowFrame component composes Shadcn primitives with drag logic:

1. **The Frame:** A Card component with custom Tailwind classes for the "retro" look (e.g., rounded-none, border-2, border-black).  
2. **The Handle:** The CardHeader acts as the title bar. It receives the attributes and listeners from dnd-kit's useDraggable hook.16  
3. **The Viewport:** The CardContent renders the child component (Map, Chat, etc.). Crucially, we must capture onMouseDown events on the content to trigger the bringToFront action, ensuring that clicking inside a window focuses it.

Minimization Logic:  
When a user clicks "Minimize" in the CardHeader:

1. The window's state is updated to minimized: true.  
2. CSS logic (via Tailwind) transitions the window's transform property to translate it towards the coordinates of its corresponding icon in the Taskbar.  
3. The window is unmounted or hidden (via display: none) to save resources, though for heavy components like maps, we might use visibility: hidden to preserve the WebGL context.

### **3.3 State Persistence with Convex**

A true OS remembers its state. If a user arranges their windows—Map on the left, Chat on the right—and reloads the page, the layout should persist.  
Convex is the ideal backend for this.11 We can define a desktops table in the Convex schema:

* userId: Identifier for the user.  
* layout: A JSON object storing the coordinates, dimensions, and open status of all windows.

**Synchronization Strategy:**

* **Debounced Mutation:** As the user drags a window, we don't spam the server. We use a debounced mutation that updates the layout record in Convex 500ms after the drag ends.  
* **Hydration:** On page load (TanStack Start loader), we fetch the layout from Convex. The DesktopProvider initializes with this layout, restoring the user's workspace instantly.

## ---

**4\. The Geospatial Engine: The "Binary Bridge" Architecture**

The requirement to visualize the British Isles with high granularity (Civil Parishes, LSOAs, Gaeltacht boundaries) creates a massive data challenge. Rendering thousands of polygons via standard DOM nodes or basic SVG is impossible. We must use WebGL, specifically **Deck.gl**, powered by **DuckDB WASM**.

### **4.1 The Role of Lonboard: Preprocessing, Not Runtime**

The user mentioned **Lonboard**.17 It is critical to clarify that Lonboard is a *Python* library designed for Jupyter notebooks. It cannot run directly in a React application. However, Lonboard is built on two foundational technologies: **GeoArrow** and **GeoParquet**.19  
The Architecture:  
We will use Lonboard in the Data Engineering Pipeline (offline), not the application runtime.

1. **Ingestion (Python/Lonboard):** We use Python to ingest the heavy shapefiles:  
   * Cornwall Civil Parishes.21  
   * Welsh LSOAs.23  
   * Isle of Man Sheadings/Parishes.24  
   * Irish Gaeltacht boundaries.26  
2. **Processing:** We join these geometries with the Census 2021 data (e.g., joining Cornish identity statistics to the parish polygons).28  
3. **Serialization:** We use Lonboard (or geopandas \+ pyarrow) to export this data as **GeoParquet** files.29  
   * *Why GeoParquet?* It is a binary, columnar format. A 50MB GeoJSON file might compress to 5MB in Parquet, and more importantly, it requires zero JSON parsing overhead in the browser.

### **4.2 The Browser Runtime: DuckDB WASM \+ Deck.gl**

To render this data in the "Celtic OS" web app, we build a bridge using **DuckDB WASM**.31

1. **Fetching:** When the "Map Window" opens, DuckDB WASM initializes in a web worker. It uses HTTP Range Requests to fetch *only* the necessary chunks of the GeoParquet file from the server/CDN.  
2. **Querying:** The application can run SQL queries directly against this file in the browser.  
   * *Example:* SELECT geometry, welsh\_speakers\_pct FROM 'wales\_census.parquet' WHERE welsh\_speakers\_pct \> 20  
3. **Zero-Copy Handoff:** DuckDB outputs the result as an **Apache Arrow** table.  
4. **Rendering:** We use the @geoarrow/deck.gl-layers library.33 This library accepts the Arrow table *directly*. The binary data flows from DuckDB \-\> Arrow \-\> Deck.gl \-\> GPU without ever being serialized to JSON or traversing the main JavaScript thread. This allows for the rendering of millions of points or polygons at 60FPS, maintaining the "fluid" feel of the OS.

## ---

**5\. Linguistic Geography: Data Layers of the British Isles**

To build a meaningful interactive map, we must understand the specific data landscapes of the Celtic nations. The "Celtic OS" will feature distinct layers for each nation, selectable via a "Start Menu" or desktop icons.

### **5.1 Cornwall (Kernow)**

**Data Source:** The 2021 Census for England and Wales provided a breakthrough for Cornish data. For the first time, "Cornish" was a write-in option that generated significant data.28

* **Identity vs. Language:** We have data for *Cornish Identity* (117,350 people) and *Cornish Language* skills (Main Language).  
* **Granularity:** The data is available at the **Civil Parish** level.22 There are 213 parishes.35  
* **Visualization Strategy:** The map should default to a Parish-level choropleth. Clicking a parish (e.g., *St Just*) should open a "Parish Stats" window showing the % of people identifying as Cornish vs. those speaking the language.

### **5.2 Wales (Cymru)**

**Data Source:** The 2021 Census provides the most granular data on Welsh language ability.36

* **Granularity:** Data is available at the **LSOA** (Lower Layer Super Output Area) level.23 LSOAs are much smaller than parishes, offering higher resolution.  
* **Trends:** The map can visualize the "Y Fro Gymraeg" (the Welsh-speaking heartland) in the west/north vs. the anglicized south-east.  
* **Comparison:** A "Time Slider" window could allow users to toggle between 2011 and 2021 data, visualizing the decline in traditional strongholds (like Carmarthenshire) and the rise in urban areas (like Cardiff).36

### **5.3 Isle of Man (Mannin)**

**Data Source:** The Isle of Man is a Crown Dependency and conducts its own census (2021).37

* **Granularity:** The administrative units are **Sheadings** (6) and **Parishes** (17).39  
* **Language Data:** The 2021 census recorded 2,223 people with knowledge of Manx.37 This is a small number, making a heatmap difficult.  
* **Visualization Strategy:** Instead of a heatmap, use **Point Clouds** or \*\* proportional symbols\*\* (circles) centered on the parishes (e.g., Rushen, Andreas) to represent speaker clusters.40

### **5.4 Scotland (Alba) and Ireland (Éire)**

* **Scotland:** Use **Council Areas** or **Data Zones** from the Scotland Census 2011 (and 2022 releases) to map Gaelic speakers, focusing on the *Eilean Siar* (Western Isles).41  
* **Ireland:** The **Gaeltacht** boundaries are legally defined.26 The map should distinctly highlight these zones (Category A, B, and C Gaeltacht regions) in a different color palette (e.g., emerald green vs. pale green) to show statutory protection vs. actual usage.42

## ---

**6\. The AI Educational Layer: Engineering Polyglot Agents**

The "Celtic OS" is not just a map; it is a learning environment. The user requested "AI educational agents." These will exist as "Applications" within the OS—chat windows that users can open to practice specific languages.

### **6.1 Agent Architecture with Convex and OpenAI**

**Convex** acts as the backend orchestrator for these agents.

* **Schema:** The messages table stores the conversation history, keyed by sessionId (representing the open chat window).43  
* **Action:** When a user types a message, a Convex **Action** is triggered. This action acts as a proxy to the OpenAI API.44  
* **Streaming:** To maintain the "interactive" feel, the response from OpenAI must be streamed. Convex supports streaming responses back to the client, allowing the text to appear character-by-character, mimicking a retro teletype or the "thinking" process of a 90s computer.45

### **6.2 Low-Resource Language Challenges & RAG**

A critical challenge is that Celtic languages (especially Manx and Cornish) are "low-resource" languages for Large Language Models (LLMs) like GPT-4.46 The models often hallucinate grammar or invent words.  
Solution: Retrieval Augmented Generation (RAG)  
We cannot rely on the model's raw training data. We must ground the agent in truth.

1. **Knowledge Base:** We scrape or ingest high-quality dictionaries (e.g., *Gerlyver Kernewek* for Cornish) and grammar guides into Convex.  
2. **Vector Search:** Convex has built-in vector search. When the user asks "How do I say 'Welcome' in Manx?", the system first searches the Manx Dictionary vector index.  
3. **Prompt Engineering:** The retrieved result (Failt erriu) is injected into the system prompt:"You are a Manx tutor. The user asked for a translation. Use this context: {'Welcome': 'Failt erriu'}. Do not invent words."  
   This ensures the agent provides accurate educational content while maintaining the persona of a helpful tutor.

### **6.3 Real-Time Voice Interaction**

The "interactive" requirement suggests voice. We can integrate **OpenAI's Realtime API** via WebRTC.48

* **WebRTC Handshake:** The Chat Window contains a "Call" button. Clicking it triggers a Convex action to generate an ephemeral session token from OpenAI.  
* **Browser Implementation:** The client uses the standard RTCPeerConnection API to connect directly to OpenAI's audio servers.  
* **Audio Visualizer:** To enhance the "OS" vibe, the audio stream is analyzed using the Web Audio API to render a retro waveform or spectrum analyzer (like Winamp) inside the Chat Window while the agent speaks.

## ---

**7\. Aesthetic Engineering: The "Retro-Futurist" Design System**

The visual language is the glue that holds the "Celtic OS" together. It must feel like a cohesive, gamified world.

### **7.1 The "PostHog" Look: Tailwind Implementation**

To achieve the PostHog aesthetic 3 using Tailwind CSS:

* **Colors:** Use a palette of "Paper White" (\#f3f4f6), "Ink Black" (\#1f2937), and "Highlighter Yellow" (\#facc15).  
* **Borders & Shadows:**

.retro-card {  
border: 2px solid black;  
box-shadow: 4px 4px 0px 0px black;  
border-radius: 0px; /\* Crucial for the brutalist look /  
}  
.retro-button:active {  
transform: translate(2px, 2px);  
box-shadow: 2px 2px 0px 0px black; / Button "press" effect \*/  
}  
\`\`\`

* **Fonts:** Integrating fonts is vital.  
  * *Headers:* **Press Start 2P** or **VT323** (Google Fonts) for window titles and "Start Menu" items.  
  * *Body:* **Space Mono** or **Inter** for readability in chat and stats.

### **7.2 Generating Playful Art Assets**

The user requested help generating assets similar to the PostHog image. This requires specific prompts for Generative AI tools (Midjourney v6 or DALL-E 3).  
Prompt Strategy: "Isometric Pixel Art"  
PostHog uses 8-bit/16-bit iconography.

* **Base Map Icons:***"Isometric pixel art icon of a small stone Celtic chapel on a green hill, 32-bit retro game style, white background, hard black outlines, vibrant emerald green and grey stone colors, no shadows, \--v 6.0"* 50  
* **AI Agent Avatars:**  
  * *Welsh Agent:* *"Pixel art portrait of a friendly red dragon wearing a professor's tweed jacket and glasses, 16-bit style, expressive face, solid yellow background \--v 6.0"*  
  * *Cornish Agent:* *"Pixel art portrait of a Cornish miner with a candle on his helmet, holding a pasty, smiling, retro RPG dialogue portrait style, 16-bit \--v 6.0"* 51  
* **UI Elements:**  
  * *"Computer operating system icons, folder, trash can, map file, chat bubble, 1-bit pixel art, black and white, dithering style \--v 6.0"* 50

These assets should be exported as PNGs with transparent backgrounds and used as:

1. **Desktop Icons:** Clickable items on the background canvas to open windows.  
2. **Deck.gl Markers:** Use IconLayer in Deck.gl to place the "Chapel" or "Castle" icons on the map at significant cultural sites (e.g., Tintagel, Caernarfon).

## ---

**8\. Technical Implementation Roadmap**

### **Phase 1: The Foundation (Weeks 1-2)**

* **Stack Init:** Initialize npm create tanstack-start@latest. Configure Tailwind and Shadcn.  
* **Window Core:** Build the DesktopProvider context and the draggable WindowFrame component using dnd-kit.  
* **Layout:** Implement the non-scrolling "Desktop" layout with a Taskbar component at the bottom.

### **Phase 2: The Geospatial Pipeline (Weeks 3-4)**

* **Data Ops:** Write Python scripts (using geopandas and lonboard) to download Census 2021 shapefiles and attribute data. Clean and join them.  
* **Conversion:** Export valid GeoParquet files for each nation.  
* **Map Integration:** Create the MapWindow component. Implement duckdb-wasm loading logic. Set up DeckGL with GeoArrowSolidPolygonLayer to render the Parquet data.

### **Phase 3: The Intelligence Layer (Weeks 5-6)**

* **Convex Backend:** Initialize Convex. Define schema for desktops and messages.  
* **Chat UI:** Build the ChatWindow using Shadcn ScrollArea and Input.  
* **Agent Logic:** Implement the Convex Action for OpenAI connectivity. Set up the RAG pipeline with a Manx/Cornish vocabulary vector store.

### **Phase 4: Polish & Assets (Weeks 7-8)**

* **Asset Gen:** Run the Midjourney prompts. Replace placeholder icons with the generated pixel art.  
* **Theming:** rigorous CSS pass to ensure all borders, shadows, and fonts match the "PostHog" retro-brutalist spec.  
* **Optimization:** Tune duckdb-wasm memory usage. Ensure window dragging is 60fps even with the map open (using React.memo and ensuring drag events don't trigger map re-renders).

## ---

**9\. Conclusion**

The proposed "Celtic OS" is more than a technical exercise; it is a novel approach to digital interaction. By treating the browser as an operating system, we solve the inherent usability problems of complex data dashboards. The user no longer has to choose between looking at the map and talking to the agent—they can do both, arranging their digital workspace to suit their learning style. The combination of **TanStack Start** for the application shell, **DuckDB/Deck.gl** for the heavy spatial lifting, and **Convex/OpenAI** for the intelligent layer provides a robust, scalable foundation for this ambitious interface. This architecture doesn't just mimic the "fun" of PostHog; it functionalizes it, turning whimsy into a powerful tool for the preservation and exploration of Celtic languages.

## **10\. Appendix: Data Tables**

### **Table 1: Administrative Units for Geospatial Visualization**

| Nation | Primary Unit (Fine) | Secondary Unit (Coarse) | Data Source | Notes |
| :---- | :---- | :---- | :---- | :---- |
| **Cornwall** | Civil Parish (213) | Output Areas (OA) | ONS Open Geography | "Cornish Identity" data available at Parish level.22 |
| **Wales** | LSOA (1,909) | MSOA / UA | StatsWales / ONS | Highest granularity for Welsh language skills.23 |
| **Isle of Man** | Parish (17) | Sheading (6) | Isle of Man Gov / OSM | Manx data requires careful manual mapping from census reports.37 |
| **Scotland** | Data Zone | Council Area | Scotland Census | Focus on "Eilean Siar" for Gaelic density.41 |
| **Ireland** | Electoral Division | Gaeltacht Boundaries | Údarás na Gaeltachta | Legal Gaeltacht boundaries differ from standard administrative borders.26 |

### **Table 2: Recommended Libraries for the "Window Manager"**

| Feature | Library | Reason for Selection |
| :---- | :---- | :---- |
| **Drag Physics** | @dnd-kit/core | Sensor API prevents conflict with Map interaction (e.g., only drag by header).13 |
| **UI Components** | shadcn/ui | Headless, accessible, and easily styled via Tailwind to look "Retro".12 |
| **Map Rendering** | deck.gl | Only library capable of rendering 100k+ polygons (Parishes/LSOAs) performantly via WebGL.33 |
| **Data Engine** | duckdb-wasm | Reads GeoParquet directly in browser; avoids JSON serialization bottleneck.31 |
| **State/Sync** | convex | Syncs window positions/chat in real-time; handles backend AI logic.11 |

#### **Works cited**

1. Self-host PostHog \- Docs, accessed December 15, 2025, [https://posthog.com/docs/self-host](https://posthog.com/docs/self-host)  
2. PostHog.com site architecture \- Handbook, accessed December 15, 2025, [https://posthog.com/handbook/engineering/posthog-com/technical-architecture](https://posthog.com/handbook/engineering/posthog-com/technical-architecture)  
3. PostHog taking a different approach : r/web\_design \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/web\_design/comments/1nf4vyj/posthog\_taking\_a\_different\_approach/](https://www.reddit.com/r/web_design/comments/1nf4vyj/posthog_taking_a_different_approach/)  
4. Welcome to PostHog\!, accessed December 15, 2025, [https://posthog.com/product-toolkit](https://posthog.com/product-toolkit)  
5. Product OS by PostHog – Developer tools for product engineers, accessed December 15, 2025, [https://posthog.com/product-os](https://posthog.com/product-os)  
6. Cornish people \- Wikipedia, accessed December 15, 2025, [https://en.wikipedia.org/wiki/Cornish\_people](https://en.wikipedia.org/wiki/Cornish_people)  
7. Project structure \- Handbook \- PostHog, accessed December 15, 2025, [https://posthog.com/handbook/engineering/project-structure](https://posthog.com/handbook/engineering/project-structure)  
8. How we designed the PostHog mascot, accessed December 15, 2025, [https://posthog.com/blog/drawing-hedgehogs](https://posthog.com/blog/drawing-hedgehogs)  
9. Icons for Apps- Development · Issue \#10733 \- GitHub, accessed December 15, 2025, [https://github.com/PostHog/posthog/issues/10733](https://github.com/PostHog/posthog/issues/10733)  
10. Installation \- Shadcn UI, accessed December 15, 2025, [https://ui.shadcn.com/docs/installation](https://ui.shadcn.com/docs/installation)  
11. TanStack Start Quickstart | Convex Developer Hub, accessed December 15, 2025, [https://docs.convex.dev/quickstart/tanstack-start](https://docs.convex.dev/quickstart/tanstack-start)  
12. Building Modern UI with React and shadcn/ui: Fast, Consistent, Elegant | by Dedi Kusniadi, accessed December 15, 2025, [https://medium.com/@dedikusniadi/building-modern-ui-with-react-and-shadcn-ui-fast-consistent-elegant-2280a7469f69](https://medium.com/@dedikusniadi/building-modern-ui-with-react-and-shadcn-ui-fast-consistent-elegant-2280a7469f69)  
13. How to implement drag and drop in React using dnd-kit \- DEV Community, accessed December 15, 2025, [https://dev.to/arshadayvid/how-to-implement-drag-and-drop-in-react-using-dnd-kit-204h](https://dev.to/arshadayvid/how-to-implement-drag-and-drop-in-react-using-dnd-kit-204h)  
14. \[OC\] I built a drag-and-drop library using shadcn/ui \+ dnd-kit : r/react \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/react/comments/1ogcl5s/oc\_i\_built\_a\_draganddrop\_library\_using\_shadcnui/](https://www.reddit.com/r/react/comments/1ogcl5s/oc_i_built_a_draganddrop_library_using_shadcnui/)  
15. How to create complex interactions with dnd-kit. A how-to that I hope is going to help some folks. \#809 \- GitHub, accessed December 15, 2025, [https://github.com/clauderic/dnd-kit/discussions/809](https://github.com/clauderic/dnd-kit/discussions/809)  
16. React Card Collapsible \- shadcn.io, accessed December 15, 2025, [https://www.shadcn.io/patterns/collapsible-card-1](https://www.shadcn.io/patterns/collapsible-card-1)  
17. Lonboard \- Overture Maps Documentation, accessed December 15, 2025, [https://docs.overturemaps.org/examples/lonboard/](https://docs.overturemaps.org/examples/lonboard/)  
18. lonboard \- PyPI, accessed December 15, 2025, [https://pypi.org/project/lonboard/](https://pypi.org/project/lonboard/)  
19. Launching Lonboard \- Development Seed, accessed December 15, 2025, [https://developmentseed.org/blog/2023-10-23-lonboard/](https://developmentseed.org/blog/2023-10-23-lonboard/)  
20. How it works? \- lonboard \- Development Seed, accessed December 15, 2025, [https://developmentseed.org/lonboard/latest/how-it-works/](https://developmentseed.org/lonboard/latest/how-it-works/)  
21. UK-GeoJSON, accessed December 15, 2025, [https://martinjc.github.io/UK-GeoJSON/](https://martinjc.github.io/UK-GeoJSON/)  
22. Open Geography Portal, accessed December 15, 2025, [https://geoportal.statistics.gov.uk/](https://geoportal.statistics.gov.uk/)  
23. Welsh Language Change in the percentage of people aged three years or older able to speak Welsh by LSOA 2011 to 2021 | DataMapWales, accessed December 15, 2025, [https://datamap.gov.wales/layers/geonode:welsh\_language\_change\_2021](https://datamap.gov.wales/layers/geonode:welsh_language_change_2021)  
24. File:IsleOfMan SheadingsAndParishes-en.svg \- Wikimedia Commons, accessed December 15, 2025, [https://commons.wikimedia.org/wiki/File:IsleOfMan\_SheadingsAndParishes-en.svg](https://commons.wikimedia.org/wiki/File:IsleOfMan_SheadingsAndParishes-en.svg)  
25. Isle of Man: OpenStreetMap basemap — download vector layers and GIS project for QGIS, ArcGIS Pro, ArcMap, MapInfo \- NextGIS Data, accessed December 15, 2025, [https://data.nextgis.com/en/region/IM/base/](https://data.nextgis.com/en/region/IM/base/)  
26. Gaeltacht \- Wikipedia, accessed December 15, 2025, [https://en.wikipedia.org/wiki/Gaeltacht](https://en.wikipedia.org/wiki/Gaeltacht)  
27. Gaeltacht Language Planning Area Boundaries Ungeneralised \- National Administrative Boundaries \- 2015 | Surveying Open Data Portal, accessed December 15, 2025, [https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-language-planning-area-boundaries-ungeneralised-national-administrative-boundaries-2015/about](https://data-osi.opendata.arcgis.com/datasets/osi::gaeltacht-language-planning-area-boundaries-ungeneralised-national-administrative-boundaries-2015/about)  
28. Cornish identity, England and Wales: Census 2021 \- Office for National Statistics, accessed December 15, 2025, [https://www.ons.gov.uk/peoplepopulationandcommunity/culturalidentity/ethnicity/articles/cornishidentityenglandandwales/census2021](https://www.ons.gov.uk/peoplepopulationandcommunity/culturalidentity/ethnicity/articles/cornishidentityenglandandwales/census2021)  
29. Optimal GeoParquet Partitioning Strategy | by Will Gislason | Center for Coastal Climate Resilience Visualizations Team | Medium, accessed December 15, 2025, [https://medium.com/center-for-coastal-climate-resilience-visualizatio/optimal-geoparquet-partitioning-strategy-33331874ef6c](https://medium.com/center-for-coastal-climate-resilience-visualizatio/optimal-geoparquet-partitioning-strategy-33331874ef6c)  
30. GeoArrow and GeoParquet in deck.gl \- Kyle Barron, accessed December 15, 2025, [https://kylebarron.dev/blog/geoarrow-and-geoparquet-in-deck-gl/](https://kylebarron.dev/blog/geoarrow-and-geoparquet-in-deck-gl/)  
31. From Legacy CSVs to Cloud-Native Geodata \- Development Seed, accessed December 15, 2025, [https://developmentseed.org/blog/2025-10-08-spatial-access-lab/](https://developmentseed.org/blog/2025-10-08-spatial-access-lab/)  
32. Spatial Extension \- DuckDB, accessed December 15, 2025, [https://duckdb.org/docs/stable/core\_extensions/spatial/overview](https://duckdb.org/docs/stable/core_extensions/spatial/overview)  
33. deck.gl layers for rendering GeoArrow data \- GitHub, accessed December 15, 2025, [https://github.com/geoarrow/deck.gl-layers](https://github.com/geoarrow/deck.gl-layers)  
34. @geoarrow/deck.gl-layers Polygon Example / Development Seed | Observable, accessed December 15, 2025, [https://observablehq.com/@developmentseed/geoarrow-deck-gl-polygon-example](https://observablehq.com/@developmentseed/geoarrow-deck-gl-polygon-example)  
35. Cornwall Council BI parish polygons 2021 \- Overview \- ArcGIS Online, accessed December 15, 2025, [https://www.arcgis.com/home/item.html?id=b33b7734a7914cbca3a9654141322d11](https://www.arcgis.com/home/item.html?id=b33b7734a7914cbca3a9654141322d11)  
36. 2021 Census \- Welsh Language Commissioner, accessed December 15, 2025, [https://www.welshlanguagecommissioner.wales/policy-and-research/the-position-of-the-welsh-language/2021-census](https://www.welshlanguagecommissioner.wales/policy-and-research/the-position-of-the-welsh-language/2021-census)  
37. Isle of Man Population 2023, accessed December 15, 2025, [https://populationdata.org.uk/isle-of-man-population/](https://populationdata.org.uk/isle-of-man-population/)  
38. 2021 Isle of Man Census Report Part I, accessed December 15, 2025, [https://www.gov.im/media/1375604/2021-01-27-census-report-part-i-final-2.pdf](https://www.gov.im/media/1375604/2021-01-27-census-report-part-i-final-2.pdf)  
39. Sheadings of the Isle of Man \- Statoids, accessed December 15, 2025, [https://statoids.com/uim.html](https://statoids.com/uim.html)  
40. Censuses of Manx Speakers, accessed December 15, 2025, [https://www.isle-of-man.com/manxnotebook/history/manks/census.htm](https://www.isle-of-man.com/manxnotebook/history/manks/census.htm)  
41. Gaelic language skills \- Census Maps, NRS, accessed December 15, 2025, [https://www.scotlandscensus.gov.uk/atlas/choropleth/ethnic\_group,\_national\_identity,\_language\_and\_religion/gaelic-language-skills/gael-lang-cat-p-2/understands-speaks-reads-or-writes-gaelic](https://www.scotlandscensus.gov.uk/atlas/choropleth/ethnic_group,_national_identity,_language_and_religion/gaelic-language-skills/gael-lang-cat-p-2/understands-speaks-reads-or-writes-gaelic)  
42. 1920s Gaeltacht (see text for explanation) | Download Scientific Diagram \- ResearchGate, accessed December 15, 2025, [https://www.researchgate.net/figure/s-Gaeltacht-see-text-for-explanation\_fig1\_233040197](https://www.researchgate.net/figure/s-Gaeltacht-see-text-for-explanation_fig1_233040197)  
43. Chat GPT Clone Starter \- Convex, accessed December 15, 2025, [https://www.convex.dev/templates/chat-gpt](https://www.convex.dev/templates/chat-gpt)  
44. ChatGPT clone with React Suspense and Streaming \- DEV Community, accessed December 15, 2025, [https://dev.to/fibonacid/chatgpt-clone-with-react-suspense-and-streaming-11me](https://dev.to/fibonacid/chatgpt-clone-with-react-suspense-and-streaming-11me)  
45. Conversation \- AI SDK, accessed December 15, 2025, [https://ai-sdk.dev/elements/components/conversation](https://ai-sdk.dev/elements/components/conversation)  
46. \[2405.13010\] UCCIX: Irish-eXcellence Large Language Model \- arXiv, accessed December 15, 2025, [https://arxiv.org/abs/2405.13010](https://arxiv.org/abs/2405.13010)  
47. Irish-BLiMP: A Linguistic Benchmark for Evaluating Human and Language Model Performance in a Low-Resource Setting \- arXiv, accessed December 15, 2025, [https://arxiv.org/html/2510.20957v1](https://arxiv.org/html/2510.20957v1)  
48. Realtime API with WebRTC \- OpenAI Platform, accessed December 15, 2025, [https://platform.openai.com/docs/guides/realtime-webrtc](https://platform.openai.com/docs/guides/realtime-webrtc)  
49. Voice agents | OpenAI API, accessed December 15, 2025, [https://platform.openai.com/docs/guides/voice-agents](https://platform.openai.com/docs/guides/voice-agents)  
50. 20 AI Pixel Art Generator Prompts for Midjourney \- Rareconnections, accessed December 15, 2025, [https://www.rareconnections.io/ai-pixel-art-generator-prompts](https://www.rareconnections.io/ai-pixel-art-generator-prompts)  
51. Animated Pixel Art Villages 2 (Prompts Included) : r/midjourney \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/midjourney/comments/1jhdul4/animated\_pixel\_art\_villages\_2\_prompts\_included/](https://www.reddit.com/r/midjourney/comments/1jhdul4/animated_pixel_art_villages_2_prompts_included/)