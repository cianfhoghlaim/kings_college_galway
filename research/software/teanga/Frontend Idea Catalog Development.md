# **Automated Frontend Intelligence: A Multi-Modal Framework for Design Pattern Extraction**

## **1\. Introduction: The Era of Design Mining**

The contemporary web has transcended its origins as a static document delivery system to become a sophisticated application platform. Modern frontend engineering is characterized by intricate state management, physics-based animations, and complex geospatial visualizations. For frontend architects, design technologists, and product designers, the ability to systematically catalog and analyze these emerging patterns—what we term "frontend ideas"—is a critical competitive advantage. However, the transient and interactive nature of modern web interfaces renders traditional scraping methodologies obsolete. Standard DOM parsing tools cannot capture the "feel" of a spring animation, the logic of a windowing system, or the clustering behavior of an interactive map.  
This report establishes a comprehensive architectural framework for **Automated Design Mining**. By synthesizing cloud-based browser infrastructure, AI-driven automation agents, and structured prompt engineering languages, we define a pipeline capable of navigating complex Single Page Applications (SPAs), recording their behaviors, and extracting structured design intelligence. Specifically, we focus on three distinct architectural archetypes represented by **PostHog.com** (Application OS/Windowing), **HiddenHeritages.ai** (Narrative Geospatial Visualization), and **Canuint.ie** (Audio-Visual Synchronization).  
Our objective is to move beyond "View Source"—which is often obfuscated by minification and compilation—to "View Intent." We propose a multi-modal extraction methodology that combines the precision of code execution with the semantic understanding of Large Language Models (LLMs) to reverse-engineer design patterns into a queryable catalog.

## ---

**2\. The Modern Extraction Stack: Infrastructure and Intelligence**

To extract dynamic "frontend ideas" such as animations and interactive maps, the tooling stack must provide three capabilities: reliable execution context, intelligent agentic navigation, and structured data enforcement. We leverage **Browserbase**, **Stagehand**, and **BAML** to fulfill these roles.

### **2.1 Browserbase: Serverless Execution and Observability**

Browserbase functions as the foundational infrastructure layer, providing a serverless platform for managing headless browsers.1 Unlike traditional local Selenium grids or unmanaged EC2 instances, Browserbase offers specific features critical for high-fidelity design extraction.

#### **2.1.1 Infrastructure as Code for Browsers**

The extraction of animations requires a stable, reproducible rendering environment. Browserbase abstracts the complexity of managing Chromium instances, allowing for the instantiation of thousands of isolated browsers in milliseconds.2 This is crucial when building a catalog that may need to scan hundreds of pages across a site like HiddenHeritages.ai. The serverless nature ensures that the "observer effect"—where the act of scraping degrades performance—is minimized by offloading execution to the cloud.

#### **2.1.2 Stealth and Anti-Bot Evasion**

Modern frontends often sit behind sophisticated bot detection systems (e.g., Cloudflare, Akamai). A naive script attempting to scrape PostHog.com might be blocked before it can render the UI. Browserbase employs advanced stealth techniques, including fingerprint management, automated CAPTCHA solving, and residential proxy rotation.1 By mimicking human behavior patterns—such as randomized mouse movements and realistic keystroke delays—Browserbase ensures the extraction agent gains access to the fully hydrated application state required for analysis.

#### **2.1.3 Session Recording and The "rrweb" Paradigm**

A pivotal feature for our use case is **Session Recording**. Browserbase automatically captures the entire browser session. Crucially, this is not just a video stream but typically a reconstruction of the Document Object Model (DOM) history, often utilizing technologies like rrweb.4

* **Implication for Animation:** This allows us to "replay" an interaction frame-by-frame. We can inspect the exact state of the DOM at t=200ms of an animation, observing class changes and style mutations that are invisible to static snapshots.  
* **Implication for Maps:** For canvas-based maps (like those potentially on HiddenHeritages), DOM recording is insufficient. Here, we leverage Browserbase's capability to export actual video files or screencasts via the Chrome DevTools Protocol (CDP), enabling visual analysis by Multimodal LLMs.4

### **2.2 Stagehand: The AI-Native Agent**

While Browserbase provides the vehicle, Stagehand provides the driver. Stagehand is an AI-native browser automation framework that sits on top of Playwright, enhancing it with Large Language Model (LLM) capabilities.6

#### **2.2.1 The Failure of Selectors vs. The Promise of "Act"**

Traditional scraping relies on CSS selectors (e.g., div.hero \> button.primary). These are brittle; a minor update to PostHog's Tailwind configuration could break the scraper. Stagehand introduces the act primitive, which accepts natural language instructions.7

* **Mechanism:** When instructed to page.act("Click the 'minimize' button on the active window"), Stagehand analyzes the Accessibility Tree and the DOM structure to identify the element semantically.  
* **Relevance:** This allows our agent to interact with novel UI components—like PostHog's custom window controls—without us needing to reverse-engineer their specific class names beforehand. It enables *exploratory* extraction.

#### **2.2.2 The "Observe" Primitive for Discovery**

To build a catalog, we must first discover what exists. Stagehand's observe primitive returns a list of possible interactions on a page.7

* **Workflow:** On Canuint.ie, we can run observe to find all interactive regions on the map. Stagehand might return actions like "Click on the Cork dialect region" or "Play the audio sample." This automates the discovery of the "ideas" we wish to catalog.

#### **2.2.3 Self-Healing and Caching**

Stagehand implements caching mechanisms where it remembers the successful action path for a given prompt.8 If the UI changes, it "heals" by re-evaluating the page with the LLM. This ensures our frontend cataloging pipeline remains robust over months of operation, continuously monitoring target sites for design updates.

### **2.3 BAML: Structured Intelligence**

Extracting "ideas" involves subjective analysis. We need to convert the unstructured visual data of an animation into a structured JSON schema (e.g., Duration, Easing, Library). BAML (Better Architecture for Machine Learning) is a domain-specific language designed to enforce structured outputs from LLMs.10

#### **2.3.1 Prompt-as-Code**

BAML treats prompts as functions with typed arguments and return values. This allows us to define a strict schema for a "Frontend Idea."

* **Type Safety:** If an LLM analyzing a map interaction returns a malformed JSON object, BAML's parser intercepts this and can trigger a retry or repair the output.12 This is essential for building a database-ready catalog.  
* **Video Analysis Integration:** BAML supports multimodal inputs, including images and video.13 This is the linchpin of our strategy: we will pass video clips of interactions (recorded by Browserbase) to a Vision Language Model (VLM) like Gemini 1.5 Pro via BAML to generate text descriptions of the animation physics.

## ---

**3\. Analysis of Target Architectures**

To design an effective extraction strategy, we must first deconstruct the architectural DNA of our target websites. Each presents unique challenges for automation and recording.

### **3.1 PostHog.com: The Browser-Based Operating System**

Architectural Overview:  
PostHog.com is not a traditional website; it is a "Product OS" delivered via the web. The engineering handbook reveals that it runs on Gatsby but employs a custom "windowing system" built using React Context.15

* **Window Management:** The site mimics a desktop environment. Pages are rendered as draggable windows with state properties for position (x, y), size, zIndex, and minimized status.15  
* **Animation Engine:** The fluid motion of windows—opening, closing, minimizing—is driven by **Framer Motion**.15 This library uses physics-based springs rather than duration-based CSS transitions, giving the UI its distinct "snappy" feel.  
* **Interaction Model:** The UI supports complex mouse gestures: dragging windows, snapping to edges, and z-index manipulation (bringing windows to the front).

**Extraction Challenges:**

* **Non-Standard Navigation:** Standard "page loads" don't apply. Navigating from "Product Analytics" to "Session Replay" might just spawn a new React component (window) over the existing DOM rather than triggering a full page refresh. The extraction agent must understand this *single-page* persistence.  
* **State Trapping:** The state of the windows is held in React Context. Accessing this from outside requires injecting code to hook into React DevTools or inferring state from the DOM style attributes.

### **3.2 HiddenHeritages.ai: Narrative Geospatial Visualization**

Architectural Overview:  
The Decoding Hidden Heritages project is a data-heavy application visualizing over 5,500 folktales.16

* **Data Volume:** The core challenge here is density. Rendering thousands of points requires efficient clustering or vector tiling strategies.  
* **Tech Stack:** The backend likely involves Python (Django) or Node.js to serve the text-recognized folklore data.18 The frontend mapping technology is likely **Leaflet.js** or a similar tile-based engine, given the academic context and typical usage in digital humanities projects.17  
* **Search and Filter:** The site features advanced filtering (by date, tale type, gender) which updates the map state dynamically.20

**Extraction Challenges:**

* **Canvas vs. DOM:** If the map uses a Canvas renderer (common for high-density datasets), individual markers may not be DOM elements. This renders querySelector useless. We must rely on visual analysis (VLM) or network interception (intercepting GeoJSON) to extract data.  
* **Popups and Modals:** Information is hidden behind interactions. The agent must systematically click or hover markers to reveal the structured data (tale titles, summaries).

### **3.3 Canuint.ie: Audio-Visual Synchronization**

Architectural Overview:  
Canuint.ie is a repository of Irish dialects that emphasizes the connection between sound and place.21

* **Mapping Stack:** Explicitly cited as using **Leaflet.js**, **OpenStreetMap**, and **CARTO** basemaps.21  
* **Synchronization:** The critical feature is the "CTC forced aligner".21 This aligns audio playback with text transcripts and map highlighting. As the audio plays, specific regions on the map likely light up or animate.  
* **Libraries:** The site acknowledges the use of Leaflet.js for the mapping functionality.21

**Extraction Challenges:**

* **Temporal Sync:** The "idea" here is the *timing*. Extracting the map structure is easy; extracting the logic that links "Audio Second 0:05" to "Map Region B" is difficult. It requires an agent that can "watch" the map while "listening" (or monitoring the audio player state).

## ---

**4\. Methodology for Static Extraction (Structure & Layout)**

Before capturing motion, we must map the static terrain. This phase involves building a "Structural Skeleton" of the target sites, identifying the component hierarchy and library dependencies.

### **4.1 Automated Library Fingerprinting via Stagehand**

We configure Stagehand to perform a "Tech Stack Audit" on each target URL. This goes beyond reading package.json (which is inaccessible); it involves runtime inspection of the global scope and DOM attributes.

* **Mechanism:** We inject a script via stagehand.page.evaluate() to check for global variables known to specific libraries:  
  * window.L $\\rightarrow$ **Leaflet.js** (Confirmed for Canuint 21).  
  * window.mapboxgl $\\rightarrow$ **Mapbox GL JS**.  
  * window.\_\_REACT\_DEVTOOLS\_GLOBAL\_HOOK\_\_ $\\rightarrow$ **React** (Confirmed for PostHog 15).  
  * document.querySelector('\[class\*="framer-"\]') $\\rightarrow$ **Framer Motion** (Expected for PostHog 15).

**Table 1: Fingerprinting Strategy per Target**

| Target Site | Indicator to Watch | Inferred Technology | Extraction Goal |
| :---- | :---- | :---- | :---- |
| **PostHog.com** | class="...framer...", transform styles | Framer Motion, React | Window Component Structure |
| **Canuint.ie** | leaflet-container, leaflet-marker-icon | Leaflet.js, CARTO | Map Container & Layers |
| **HiddenHeritages** | canvas elements, Network calls to tiles | Custom WebGL / Leaflet | Geospatial Data Density |

### **4.2 Semantic Component Extraction with BAML**

Once the library is identified, we use BAML to extract the semantic structure. We define a BAML schema representing a UIComponent.

TypeScript

// BAML Schema Definition for Static Analysis  
class UIComponent {  
  name string @description("Descriptive name: 'Window Frame', 'Audio Player', 'Map View'")  
  library string @description("Identified library: 'Leaflet', 'Framer Motion', 'React'")  
  structure\_type string @description("Layout model: 'Absolute', 'Flex', 'Grid'")  
  is\_interactive bool @description("Does it respond to user input?")  
  css\_classes string @description("Relevant utility classes")  
}

function AnalyzeStructure(html\_snippet: string) \-\> UIComponent {  
  client "openai/gpt-4o"  
  prompt \#"  
    Analyze this HTML snippet from a modern web application.  
    Identify the frontend pattern being used.  
      
    Snippet:  
    {{ html\_snippet }}  
  "\#  
}

**Execution Flow:**

1. **PostHog:** Stagehand navigates to the dashboard. observe identifies the main workspace. We extract the outer HTML of a "Window" component and pass it to AnalyzeStructure. The LLM identifies the nested div structure required for the resizing handles and title bar.  
2. **Canuint:** Stagehand targets the .leaflet-map-pane. The extraction reveals the hierarchy of *tile layers*, *overlay panes*, and *marker panes*, effectively documenting the Leaflet implementation strategy.

## ---

**5\. Methodology for Temporal Extraction (Recording Animations)**

This section addresses the user's specific requirement to "record animations." Capturing the *dynamics* of a UI requires a shift from DOM analysis to **Computer Vision**.

### **5.1 The "Stimulus-Response" Recording Pipeline**

We propose a pipeline that treats the browser as a black box: we provide a stimulus (input) and record the response (video), then use AI to analyze the physics of the motion.

#### **5.1.1 Step 1: Stimulus Injection via Stagehand**

We use Stagehand's act primitive to trigger the animation.

* **PostHog:** await stagehand.act("Drag the 'Insights' window to the center of the screen").  
* **HiddenHeritages:** await stagehand.act("Click on the 'Folktales' filter to update the map").

#### **5.1.2 Step 2: High-Fidelity Video Capture**

Browserbase's standard session replay is often DOM-based (rrweb). While efficient, rrweb can miss canvas-based animations (like WebGL maps) or subtle composite-layer rendering effects.4  
Solution: We explicitly configure Playwright (underlying Stagehand) to record a video file (.mp4 or .webm) of the viewport.

* **Configuration:**  
  TypeScript  
  const context \= await browser.newContext({  
    recordVideo: {  
      dir: 'videos/',  
      size: { width: 1280, height: 720 }  
    }  
  });

  This ensures we capture a pixel-perfect representation of the animation, including WebGL content on maps.22

#### **5.1.3 Step 3: Multimodal Analysis with Gemini 1.5 Pro**

We utilize **Gemini 1.5 Pro**, which possesses native video understanding capabilities.13 We pass the recorded video clip to Gemini via a BAML function to reverse-engineer the animation logic.  
**BAML Schema for Animation Physics:**

Code snippet

class AnimationPhysics {  
  trigger\_action string @description("The user action: 'Drag', 'Click', 'Hover'")  
  motion\_type string @description("Type: 'Spring', 'Linear', 'Ease-in-out'")  
  duration\_ms int @description("Estimated duration in milliseconds")  
  perceived\_stiffness string @description("For springs: 'High', 'Low', 'Bouncy'")  
  visual\_description string @description("Narrative description of the motion")  
}

function AnalyzeVideoPhysics(video\_url: string) \-\> AnimationPhysics {  
  client "google/gemini-1.5-pro"  
  prompt \#"  
    Watch this video of a UI interaction.   
    Focus on the movement of the window element between 0:02 and 0:05.  
    Analyze the animation physics. Does it oscillate before stopping (spring)?   
    Does it move at a constant speed (linear)?  
      
    Video: {{ video\_url }}  
  "\#  
}

### **5.2 Algorithmic Keyframe Extraction (The "Computed Style" Approach)**

For CSS-driven animations (likely found on *Canuint* for simple UI transitions), visual analysis might be overkill. We can use a lower-level extraction technique: **Property Polling**.

* **Technique:** We inject a JavaScript snippet into the browser using Stagehand that utilizes requestAnimationFrame to poll the getComputedStyle() of an element.24  
* **Data Capture:** We record the transform matrix and opacity values for 60 frames (1 second).  
  JavaScript  
  // Injected Script Concept  
  const data \=;  
  const start \= performance.now();  
  function poll() {  
    const style \= window.getComputedStyle(targetElement);  
    data.push({ t: performance.now() \- start, transform: style.transform });  
    if (performance.now() \- start \< 1000\) requestAnimationFrame(poll);  
  }  
  requestAnimationFrame(poll);

* **Analysis:** This generates a time-series dataset. We can plot this data to mathematically determine the easing curve (e.g., Cubic Bezier coefficients) without guessing. This provides the most accurate "technical" definition of the animation for our catalog.

## ---

**6\. Methodology for Spatial Extraction (Interactive Maps)**

Extracting "Interactive Maps" requires overcoming the "Canvas Black Box" problem. Maps often render as a single \<canvas\> element, hiding individual markers from the DOM.

### **6.1 Network Interception (The "Data Plane")**

The most reliable way to extract map data is not to look at the map, but to look at the *network*. When a user pans a map on HiddenHeritages.ai, the browser requests data tiles or GeoJSON.

* **HAR File Analysis:** We use Playwright's network interception capabilities to record a HAR (HTTP Archive) file during the session.5  
* **Targeting:** We filter traffic for:  
  * \*.geojson: Feature collections (markers, polygons).  
  * \*.pbf: Vector tiles (common in Mapbox/modern Leaflet).  
  * api/tales: Custom API endpoints serving the folklore data.  
* **Extraction:** By capturing the response bodies, we reconstruct the *data structure* of the map. For *Hidden Heritages*, this allows us to see exactly how 5,500 tales are structured (lat/long, title, metadata) without clicking every single marker.

### **6.2 Visual Interaction Analysis (The "Interaction Plane")**

To capture the *behavior* (clustering, popups), we return to Stagehand and BAML.

* **Cluster Analysis:** We instruct Stagehand to act("Zoom out until markers group together"). We record this visual transition. Gemini 1.5 Pro analyzes the video: "As the user zooms out, individual pins merge into colored circles with numbers indicating the count. The merge animation is a rapid fade-and-scale effect."  
* **Popup Logic:** We instruct Stagehand: act("Click on a map marker"). We then run extract on the newly appeared DOM element (the popup).  
  * *Requirement:* We must identify the "fly-to" behavior. Does the map automatically pan to center the popup? This is a key UX detail captured only through video analysis.

### **6.3 Case Specific: Canuint.ie Audio Sync**

For Canuint, the map interacts with audio. This is a state-synchronization pattern.

* **Polling Strategy:** We setup a Stagehand script that polls two states simultaneously:  
  1. **Audio State:** document.querySelector('audio').currentTime  
  2. **Map State:** document.querySelectorAll('.leaflet-interactive.active').length  
* **Correlation:** We log these values every 100ms. If we see the "active" class move from one map polygon to another as currentTime advances, we have successfully extracted the "Audio-Driven Map Traversal" pattern. The output of our catalog will describe this as a "Time-linked Geospatial State Machine."

## ---

**7\. Case Study Implementation Plans**

This section details the specific extraction workflow for each of the three target sites, applying the methodologies defined above.

### **7.1 Case Study: PostHog.com (The Windowing System)**

**Goal:** Catalog the "Product OS" window management and drag physics.  
**1\. Infrastructure Setup:**

* Initialize Browserbase session with stealth: true.  
* Connect Stagehand to the CDP endpoint.

**2\. Discovery Phase:**

* **Action:** Navigate to the PostHog public demo (if available) or the homepage where the "window" UI is simulated.  
* **Observation:** Use Stagehand to identify the container elements. Look for aria-label="Window" or classes matching AppWindow.15

**3\. Animation Extraction (The "Snap"):**

* **Action:** Instruct Stagehand: await page.act("Drag the window titled 'Trends' slowly towards the right edge of the screen").  
* **Recording:** Capture the video via Browserbase/Playwright.  
* **Analysis:** Send video to Gemini 1.5 Pro via BAML.  
  * *Prompt:* "Analyze the window movement. Does it stick to the edge? Is there a visual indicator (ghost outline) before it snaps?"  
* **Result:** Identification of the "Edge Snapping" pattern and the likely use of framer-motion's dragConstraints prop.

**4\. Component Extraction:**

* **Action:** await page.extract({ instruction: "Get the CSS values for the window shadow and border-radius", schema:... }).  
* **Result:** Precise design tokens (e.g., box-shadow: 0 20px 50px rgba(...)) to recreate the aesthetic.

### **7.2 Case Study: HiddenHeritages.ai (The Narrative Map)**

**Goal:** Catalog the handling of high-density narrative data on a map.  
**1\. Infrastructure Setup:**

* Browserbase session with extended timeout (maps can be heavy).

**2\. Data Reconnaissance:**

* **Action:** Navigate to the map view.  
* **Interception:** Enable Playwright Request Interception. Filter for XHR requests.  
* **Insight:** Capture the payload loading the 5,500 tales. Is it a single 5MB JSON file? Or tiled requests? (Likely tiled or clustered server-side given the volume).

**3\. Interaction Recording (Clustering):**

* **Action:** await page.act("Click on a cluster circle with a number").  
* **Recording:** Capture the "Spiderfy" or "Zoom-to-bounds" animation.  
* **Analysis:** Determine if the cluster explodes into a spiral (common Leaflet plugin) or zooms in to disperse markers.

**4\. Search Filter Extraction:**

* **Action:** await page.act("Drag the Date Range slider").  
* **Observation:** Monitor the map. Do markers filter instantly (client-side) or is there a loading spinner (server-side)?  
* **Result:** A catalog entry for "Real-time Geospatial Filtering."

### **7.3 Case Study: Canuint.ie (The Dialect Map)**

**Goal:** Catalog the synchronized audio-visual playback interface.  
**1\. Infrastructure Setup:**

* Browserbase session. *Note:* Audio playback in headless browsers can be tricky. We must ensure the \--autoplay-policy=no-user-gesture-required flag is passed to Chrome via Browserbase configuration to allow audio to "play" (even if we don't hear it, the events fire).

**2\. Sync Extraction:**

* **Action:** await page.act("Play the first dialect sample").  
* **Monitoring:** Inject the polling script (Section 5.2) to track the .leaflet-interactive element that gains the active class.  
* **Verification:** Confirm that the active class changes correspond to the timeupdate events on the \<audio\> tag.

**3\. Tech Stack Confirmation:**

* **Fingerprint:** Confirm presence of window.L (Leaflet) and the specific tile server URLs (CARTO).21

## ---

**8\. The Frontend Idea Catalog: Schema and Storage**

The ultimate output of this pipeline is a structured database. We use a Vector Database (like MongoDB Atlas Vector Search) to store these extractions, enabling semantic search (e.g., "Show me maps that sync with audio").

### **8.1 The "Frontend Idea" BAML Schema**

We define a comprehensive BAML schema to normalize data from all three sites.

TypeScript

// BAML Schema for a Frontend Idea Entry  
class FrontendIdea {  
  id string  
  title string @description("E.g., 'Physics-based Window Dragging'")  
  site\_origin string @description("E.g., 'PostHog.com'")  
    
  // Technical Implementation  
  libraries\_detected string @description("")  
  complexity\_score string @description("Low, Medium, High")  
    
  // Design Details  
  interaction\_pattern string @description("Description of the user behavior")  
  animation\_physics AnimationPhysics? @description("Extracted physics parameters")  
    
  // Assets  
  video\_demo\_url string @description("Link to the Browserbase session recording")  
  code\_snippet\_inference string? @description("LLM-generated pseudo-code for the pattern")  
}

### **8.2 The "Idea Generation" Pipeline**

1. **Ingest:** Stagehand \+ Browserbase scrape the site and record video.  
2. **Process:** BAML \+ Gemini 1.5 Pro analyze the video and DOM to populate the FrontendIdea schema.  
3. **Store:** The structured JSON is embedded (using an embedding model like text-embedding-3-small) and stored in the Vector DB.  
4. **Retrieval:** A frontend developer asks: "How does PostHog handle window minimizing?" The system performs a vector search and retrieves the specific catalog entry, complete with the video replay and inferred Framer Motion config.

## ---

**9\. Second-Order Insights and Implications**

This architecture signifies a fundamental shift in how we build software.

### **9.1 From "Open Source" to "Open Behavior"**

We are moving towards a regime of **Behavioral Indexing**. Just as GitHub indexed code, tools like Stagehand and Browserbase allow us to index *runtime experiences*. This commoditizes high-end UX patterns. If HiddenHeritages creates a novel map clustering interaction, this pipeline enables any developer to analyze, understand, and replicate the *physics* of that interaction within minutes, democratizing access to elite frontend design.

### **9.2 The "Agent-Accessible" Web**

The success of this pipeline depends on the Accessibility Tree. Stagehand relies on ARIA labels to navigate. This creates a powerful incentive: to be visible to AI agents (Agent Engine Optimization \- AEO), developers must build accessible sites. PostHog's use of standard windowing controls makes it easier for an agent to parse. Paradoxically, the rise of AI scrapers may drive a renaissance in web accessibility standards.

### **9.3 The End of "Black Box" UX**

Maps and Canvas elements have historically been opaque to scrapers. The introduction of Multimodal LLMs (Video-to-Text) effectively "solves" the Canvas extraction problem. We no longer need to parse the code drawing the map; we simply watch the map and describe it. This closes the final gap in web analysis, making every pixel on the screen indexable and queryable.

## ---

**10\. Conclusion**

The construction of a frontend idea catalog from complex sources like PostHog, HiddenHeritages, and Canuint requires a departure from static scraping. We have defined a robust pipeline leveraging **Browserbase** for scalable, stealthy execution and high-fidelity recording; **Stagehand** for intelligent, natural-language agent command; and **BAML** for enforcing strict data schemas on multimodal outputs.  
By treating the browser as a visual medium—recording interactions as video and analyzing them with Vision LLMs—we can extract the subtle, temporal nuances of animations and map interactions that previously eluded capture. This framework not only solves the immediate technical challenge but paves the way for a new discipline of **Automated Design Mining**, turning the entire web into a structured, searchable repository of user experience innovation.

| Component | Technology | Role in Cataloging |
| :---- | :---- | :---- |
| **Infrastructure** | Browserbase | Stealth execution, Session Recording (Video/DOM), CDP Access. |
| **Agent** | Stagehand | AI-driven Navigation (act), Discovery (observe), DOM Extraction. |
| **Schema** | BAML | Typed Prompt Engineering, Multimodal Analysis (Video \-\> JSON). |
| **Intelligence** | Gemini 1.5 Pro | analyzing video physics, map interactions, and "feel". |
| **Target: PostHog** | Framer Motion | Extracting window physics and desktop-metaphor interactions. |
| **Target: Canuint** | Leaflet \+ Audio | Extracting temporal synchronization logic. |
| **Target: HiddenHeritages** | WebGL/Canvas | Extracting clustering behavior and data density handling. |

This report provides the technical blueprint for transforming ephemeral web interactions into permanent design knowledge.

#### **Works cited**

1. Web Scraping \- Browserbase Documentation, accessed December 15, 2025, [https://docs.browserbase.com/use-cases/scraping-website](https://docs.browserbase.com/use-cases/scraping-website)  
2. Browserbase: A web browser for AI agents & applications, accessed December 15, 2025, [https://www.browserbase.com/](https://www.browserbase.com/)  
3. Top 10 web scraping tools in 2025: Complete developer guide \- Browserbase, accessed December 15, 2025, [https://www.browserbase.com/blog/best-web-scraping-tools](https://www.browserbase.com/blog/best-web-scraping-tools)  
4. Introducing Browser Session Replays for Web Agents | Kernel Blog, accessed December 15, 2025, [https://www.onkernel.com/blog/introducing-browser-session-replays](https://www.onkernel.com/blog/introducing-browser-session-replays)  
5. Session Replay \- Browserbase Documentation, accessed December 15, 2025, [https://docs.browserbase.com/features/session-replay](https://docs.browserbase.com/features/session-replay)  
6. Launching Stagehand v3, the best automation framework, accessed December 15, 2025, [https://www.browserbase.com/blog/stagehand-v3](https://www.browserbase.com/blog/stagehand-v3)  
7. Stagehand Docs, accessed December 15, 2025, [https://docs.stagehand.dev/](https://docs.stagehand.dev/)  
8. browserbase/stagehand: The AI Browser Automation Framework \- GitHub, accessed December 15, 2025, [https://github.com/browserbase/stagehand](https://github.com/browserbase/stagehand)  
9. Stagehand Review: Best AI Browser Automation Framework? \- Apidog, accessed December 15, 2025, [https://apidog.com/blog/stagehand/](https://apidog.com/blog/stagehand/)  
10. BAML documentation, accessed December 15, 2025, [https://docs.boundaryml.com/home](https://docs.boundaryml.com/home)  
11. BoundaryML/baml: The AI framework that adds the engineering to prompt engineering (Python/TS/Ruby/Java/C\#/Rust/Go compatible) \- GitHub, accessed December 15, 2025, [https://github.com/BoundaryML/baml](https://github.com/BoundaryML/baml)  
12. Every Way To Get Structured Output From LLMs | BAML Blog, accessed December 15, 2025, [https://boundaryml.com/blog/structured-output-from-llms](https://boundaryml.com/blog/structured-output-from-llms)  
13. Video | Boundary Documentation \- BAML, accessed December 15, 2025, [https://docs.boundaryml.com/ref/baml\_client/video](https://docs.boundaryml.com/ref/baml_client/video)  
14. Types \- Boundary Documentation \- BAML, accessed December 15, 2025, [https://docs.boundaryml.com/ref/baml/types](https://docs.boundaryml.com/ref/baml/types)  
15. PostHog.com site architecture \- Handbook, accessed December 15, 2025, [https://posthog.com/handbook/engineering/posthog-com/technical-architecture](https://posthog.com/handbook/engineering/posthog-com/technical-architecture)  
16. Decoding Hidden Heritages, accessed December 15, 2025, [https://www.hiddenheritages.ai/](https://www.hiddenheritages.ai/)  
17. wlamb – Gaelic Algorithmic Research Group \- Blogs, accessed December 15, 2025, [https://blogs.ed.ac.uk/garg/author/wlamb/](https://blogs.ed.ac.uk/garg/author/wlamb/)  
18. Tech stack \- Handbook \- PostHog, accessed December 15, 2025, [https://posthog.com/handbook/engineering/stack](https://posthog.com/handbook/engineering/stack)  
19. Lá Fhéile Colm Cille: explore the hidden folklore connections between Ireland and Scotland, accessed December 15, 2025, [https://www.gaois.ie/en/blog/colm-cille-decoding-hidden-heritages](https://www.gaois.ie/en/blog/colm-cille-decoding-hidden-heritages)  
20. How to use this website \- Hidden Heritages, accessed December 15, 2025, [https://www.hiddenheritages.ai/en/about/how](https://www.hiddenheritages.ai/en/about/how)  
21. Acknowledgments, accessed December 15, 2025, [https://www.canuint.ie/en/info/admin/acknowledgments/](https://www.canuint.ie/en/info/admin/acknowledgments/)  
22. Videos | Playwright, accessed December 15, 2025, [https://playwright.dev/docs/videos](https://playwright.dev/docs/videos)  
23. Video Analytics with Multi-modal LLMs | by Gerald Yong | Medium, accessed December 15, 2025, [https://medium.com/@geraldyong\_86312/video-analytics-with-multi-modal-llms-b03b6221f35e](https://medium.com/@geraldyong_86312/video-analytics-with-multi-modal-llms-b03b6221f35e)  
24. Using CSS animations \- MDN Web Docs, accessed December 15, 2025, [https://developer.mozilla.org/en-US/docs/Web/CSS/Guides/Animations/Using](https://developer.mozilla.org/en-US/docs/Web/CSS/Guides/Animations/Using)  
25. Window: getComputedStyle() method \- Web APIs | MDN, accessed December 15, 2025, [https://developer.mozilla.org/en-US/docs/Web/API/Window/getComputedStyle](https://developer.mozilla.org/en-US/docs/Web/API/Window/getComputedStyle)  
26. Browser | Playwright, accessed December 15, 2025, [https://playwright.dev/docs/api/class-browser](https://playwright.dev/docs/api/class-browser)