# **Converging High-Fidelity Pre-Rendering and Database-Driven State: A Comprehensive Technical Blueprint for Next-Generation Indie Development**

## **1\. Executive Summary: The Architectural Synthesis of *Hades* and *BitCraft***

The contemporary independent game development landscape is defined by a divergence in technical philosophy. On one spectrum lies Supergiant Games, creators of the *Hades* franchise, who have perfected a **visual pipeline** that hybridizes high-fidelity 3D rendering with the responsive input latency of 2D sprites. This technique, often termed "pre-rendered isometric projection," allows for an aesthetic density—replete with fluid animations, dynamic lighting interactions, and sub-pixel anti-aliasing—that defies the computational limits of real-time 3D rendering on mid-range hardware. On the opposing spectrum lies Clockwork Labs, developers of *BitCraft Online*, who have pioneered a **backend infrastructure** centered on SpacetimeDB. This paradigm shifts the authoritative game state from a transient application server to a persistent relational database, effectively dissolving the boundary between "game server" and "database" to enable massive scalability and persistence without the traditional overhead of state synchronization.  
This report serves as a definitive technical manual for a Lead Architect or Technical Director tasked with synthesizing these two disparate yet complementary methodologies. The objective is to establish a development environment that leverages the visual "crunch" and fluidity of the *Hades* pipeline while resting upon the robust, serverless architecture of the *BitCraft* backend. We will rigorously analyze the implementation of these systems across three primary game engines—Unreal Engine 5, Unity 6, and Godot 4—providing a comparative analysis of asset generation pipelines, automation scripting, and backend integration strategies. Furthermore, to ensure this infrastructure remains adaptive, we detail the construction of an **Agentic Research Workflow**, utilizing Large Language Models (LLMs) and Python-based automation to autonomously gather, synthesize, and update technical intelligence from scattered sources such as GDC talks, developer logs, and source code repositories.  
The analysis indicates that while no single off-the-shelf engine perfectly encapsulates both workflows natively, a hybrid approach—specifically utilizing Unreal Engine for asset generation and Godot or Unity for the runtime client—offers the highest probability of replicating the specific "Supergiant feel" within an indie budget, provided the backend is offloaded to the SpacetimeDB ecosystem.1

## ---

**2\. Intelligence Gathering: Establishing an Automated Agentic Research Pipeline**

Before commencing infrastructure development, it is critical to establish a continuous stream of technical intelligence. The development methodologies of studios like Supergiant and Clockwork Labs are rarely documented in a single manual; rather, they are fragmented across Game Developers Conference (GDC) talks, obscure GitHub repositories, Discord server logs, and scattered blog posts. To manually aggregate this data is inefficient. Therefore, the first pillar of our environment is the creation of an **Agentic Research Pipeline**. This system utilizes Python-based orchestration frameworks to scrape, index, and query technical data autonomously.

### **2.1 Theoretical Architecture: The Research Agent**

The proposed architecture moves beyond simple web scraping into the realm of **Agentic AI**, where a Large Language Model (LLM) acts as a reasoning engine to determine *where* to look for information and *how* to extract it. We define this as a directed cyclic graph (DCG) using **LangGraph**, a library designed to build stateful, multi-actor applications with LLMs.4  
The agent operates on a "Plan-Execute-Verify" loop:

1. **Planner Node:** Deconstructs a high-level query (e.g., "How does Hades handle 3D-to-2D normal maps?") into sub-tasks (Search GDC Vault, Search Reddit r/gamedev, Analyze Supergiant Tech Blog).  
2. **Executor Node:** Utilizes specific tools (Scrapers, API clients) to fetch raw text.  
3. **Synthesizer Node:** Aggregates findings into a coherent technical summary, citing sources.

### **2.2 Tool Implementation: The "Tech Archaeologist" Stack**

To build this, the development environment requires a Python 3.10+ virtual environment equipped with langchain, langgraph, and specific scraping libraries.

#### **2.2.1 The GDC Vault Scraper**

The GDC Vault is a primary source for Supergiant's technical talks (e.g., "The Art of Hades"). However, it is difficult to search. We implement a custom tool using BeautifulSoup and Selenium to index these talks.  
The scraping logic must handle the GDC Vault's session token authentication. A Python script utilizing mechanize or requests.Session can simulate the login process to access member-only content.6 Once authenticated, the agent iterates through the DOM elements of the browse pages, extracting metadata (Speaker, Company, Track) and, crucially, the transcript links if available.  
Python Implementation Strategy for GDC Extraction:  
The script initiates a session, retrieves the CSRF token, and posts login credentials. Upon success, it navigates to the "Programming" and "Visual Arts" tracks. For video content, the agent utilizes the youtube\_transcript\_api if the GDC talk is hosted on YouTube (common for older talks or "Free" vault access), or parses the closed-caption track from the proprietary player. This text is then chunked and stored in a local vector database (like FAISS or ChromaDB) for retrieval.7

#### **2.2.2 The Discord and Repository Miner**

For *BitCraft* and SpacetimeDB, the primary sources of truth are often the official Discord server and the GitHub issues/discussions pages. The Agent must be equipped with the GitHub API tool.  
GitHub Mining Logic:  
The agent targets repositories such as clockworklabs/SpacetimeDB and godot-rust/gdext. It pulls README.md, CONTRIBUTING.md, and highly commented issue threads. The analysis focuses on "breaking changes" and "architecture discussions," which often reveal the intent behind technical decisions (e.g., why SpacetimeDB chose Rust over Go).

* *Specific Target:* The agent monitors the spacetimedb-sdk folder in the repo to detect changes in the C\# or Rust bindings, alerting the technical director to updates that might break the game's netcode.2

### **2.3 Orchestrating with LangGraph**

We utilize **LangGraph** to wire these tools together. Unlike a linear chain, LangGraph allows the agent to "loop" back if it fails to find information.  
**State Definition:**

Python

from typing import TypedDict, List, Annotated  
import operator

class ResearchState(TypedDict):  
    query: str  
    sources\_found: List\[str\]  
    raw\_content: Annotated\[List\[str\], operator.add\]  
    final\_report: str  
    iteration\_count: int

**Graph Workflow:**

1. **Search Node:** Uses DuckDuckGo or Tavily API to find initial URLs related to "Supergiant Games Tech Art".  
2. **Filter Node:** The LLM evaluates URLs. If a URL is a generic marketing page, it is discarded. If it is a technical blog (e.g., theforge.dev), it is passed to the Scraper.  
3. **Scrape Node:** Uses FireCrawl (an AI-optimized scraper that turns websites into Markdown) to extract clean technical documentation.10  
4. **Critique Node:** The LLM reads the scraped content. If the content lacks specific implementation details (e.g., "it mentions normal maps but not the packing format"), it modifies the search query and loops back to the Search Node.5

This autonomous workflow ensures that as Supergiant or Clockwork Labs release new information, the studio's internal knowledge base is updated without manual intervention.

## ---

**3\. The Visual Paradigm: Deconstructing the *Hades* Pipeline**

Supergiant Games’ visual signature in *Hades* is the result of a "Hybrid" pipeline. It is crucial to understand that *Hades* is **not** a traditional 2D game drawn frame-by-frame, nor is it a standard 3D game. It is a 3D game baked into 2D assets. This section outlines the technical requirements to replicate this "Pre-Rendered" aesthetic.

### **3.1 The Theoretical Framework: Pre-Rendered Isometric Projection**

The decision to pre-render 3D models into 2D sprites is driven by three factors: performance, visual consistency, and "crispness."

1. **Performance:** A complex 3D character might consist of 50,000 polygons, 4 materials, and a skeletal rig with 100 bones. Updating this skeleton and shading these pixels every frame is expensive. In contrast, a 2D sprite is a single quad (2 triangles). By baking the character, the runtime cost becomes trivial, allowing for massive on-screen enemy counts.1  
2. **Visual Consistency:** Supergiant’s environments are hand-painted 2D. 3D models often "pop" against 2D backgrounds due to mismatched lighting and aliasing. By rendering the 3D model to a sprite, the developers can apply post-processing (like outlining or cel-shading) during the bake, ensuring the character blends perfectly with the background.13  
3. **Lighting Interaction:** To ensure the sprites don't look flat, *Hades* exports not just the color (Albedo), but also the **Normal Map** and **Material ID** map of the 3D model. This allows the 2D sprite to react to real-time 3D lights in the engine.14

### **3.2 Detailed Pipeline Architecture**

The replication of this pipeline requires a "Source" engine (for generating assets) and a "Runtime" engine (for playing the game).  
The Source Engine: Unreal Engine 5  
We select Unreal Engine 5 (UE5) as the source engine because of its superior rendering capabilities (Lumen, Nanite) and the robust Movie Render Queue (MRQ). Even if the final game runs in Godot or Unity, UE5 acts as the "virtual photography studio."  
**The Baking Process:**

1. **Animation:** Characters are animated in 3D (Maya/Blender) and imported into UE5.  
2. **Staging:** A specific "Baking Level" is created. This level contains a "Turntable" blueprint.  
3. **Capture:** The automation script rotates the character (or camera) through 8, 16, or 32 compass directions.  
4. **Render Passes:** The MRQ is configured to output:  
   * **Final Image (RGB):** The visual look.  
   * **World Normal (RGB):** Used for re-lighting the sprite in the runtime engine.  
   * **Opacity (A):** For masking.  
   * **Emissive (RGB):** For glow effects (crucial for *Hades* style neon aesthetics).

## ---

**4\. Implementation Strategy: Asset Generation in Unreal Engine 5**

Unreal Engine 5 offers the highest fidelity for the source assets. The goal is to automate the extraction of sprite sheets using Python.

### **4.1 Configuring Movie Render Queue (MRQ)**

The legacy "Scene Capture 2D" is insufficient because it lacks high-quality anti-aliasing. MRQ allows for "Spatial" and "Temporal" sampling, producing ultra-smooth sprites.  
We utilize the unreal.MoviePipelineQueueSubsystem to drive this process. The Python script must dynamically generate LevelSequence assets. A "Master Sequence" is created containing a Spawnable Actor (the character). The script iterates through the Content Browser, finds every AnimSequence associated with the character's Skeleton, and creates a render job for each.15

### **4.2 Python Automation Logic**

The automation script performs the following logic sequence:

1. **Load Registry:** Scan the AsstRegistry for all Skeletal Meshes and Animation Sequences.  
2. **Job Allocation:** For each animation, create a MoviePipelineExecutorJob.  
3. **Config Override:** Apply a specific MoviePipelineMasterConfig preset. This preset is critical. It must disable "Tone Curve" (to keep linear color space for proper compositing) and enable "Deferred Rendering" to access the G-Buffer (Normal, Depth, Base Color).15  
4. **Rotation Loop:** The script must modify the Level Sequence to rotate the character. Since MRQ renders linear time, the sequence must be structured such that frames 0-100 are "South," frames 101-200 are "South-East," etc. Alternatively, the script can submit 8 separate jobs, one for each camera angle, injecting a Python callback to rotate the actor between jobs.15

Code Snippet Context:  
Using unreal.MoviePipelineQueueSubsystem, we can gain access to allocate\_new\_job. We inject the LevelSequence path and the Map path. Crucially, we use set\_configuration to apply a pre-made preset that defines the .exr output format (Multilayer EXR is preferred to keep Normals and Color in sync).15

### **4.3 Handling the "Hades 2" Visual Evolution**

*Hades 2* appears to have moved closer to real-time 3D for some characters, evidenced by smoother turns.1 If targeting *Hades 2* fidelity, the pipeline changes slightly: instead of pre-rendering rotation, we might use the 3D model at runtime but use a **Toon Shader** with "stepped" animation to mimic the sprite look. However, for the pure *Hades 1* aesthetic, the pre-rendered pipeline remains superior for performance.

## ---

**5\. Implementation Strategy: Asset Generation in Unity 6**

Unity 6 (formerly 2023 LTS) offers a more integrated, though visually slightly less advanced, baking pipeline compared to UE5's Lumen. However, its C\# scripting interface is often more accessible for tool creation.

### **5.1 The Unity Recorder API**

Unity provides the **Unity Recorder** package, which is scriptable via the UnityEditor.Recorder namespace. The key class is RecorderControllerSettings.18  
**Workflow:**

1. **Scene Setup:** Create a scene with a Green Screen or transparent background camera.  
2. **Timeline Integration:** Unity's Timeline is the equivalent of Unreal's Sequencer. We create a Timeline instance that plays the Animation Clip.  
3. **Recorder Automation:** We write a custom EditorWindow tool. This tool takes a list of Prefabs and Animations. It enters Play Mode, iterates through the animations, and triggers the RecorderController to capture frames.18

### **5.2 Capturing Data Maps (Normals)**

Unity's Recorder captures the Game View. To capture Normals, we must use **Replacement Shaders**.

* **Technique:** The automation script performs a second pass. It calls camera.SetReplacementShader(normalShader, ""). This forces the camera to render the scene using a shader that outputs World Space Normals to the RGB channels instead of the material color.  
* **Result:** You get two image sequences: Idle\_Color\_001.png and Idle\_Normal\_001.png. These are combined in an external tool (like TexturePacker) or a custom Python script using PIL / OpenCV into a single sprite sheet.20

### **5.3 Comparison: Unity vs. Unreal for Baking**

* **Unity Pros:** Faster editor startup; C\# is easier for logic scripting than Unreal Python API; direct integration with 2D tools.  
* **Unity Cons:** Achieving the "Lumen" quality of global illumination (GI) in the bake requires configuring HDRP (High Definition Render Pipeline), which is heavier. Unreal's MRQ has better native support for "Render Passes" (Cryptomatte, Object IDs) without needing custom replacement shaders.17

## ---

**6\. Implementation Strategy: Asset Generation in Godot 4**

Godot 4 represents the "Indie Optimized" path. While it lacks the raw render power of UE5, its lightweight nature allows for a unique "Runtime Baking" workflow that neither Unity nor Unreal can easily match.

### **6.1 The SubViewport Technique**

In Godot 4, a SubViewport node can render a 3D scene. This viewport produces a ViewportTexture.

* **Runtime Advantage:** Instead of baking offline to PNGs, Godot developers can keep the 3D models in the project. At runtime (during a loading screen), the game can instantiate the 3D model into a SubViewport, play the animation, capture the texture frames into an Image, and construct a SpriteFrames resource in memory.22  
* **Benefit:** This allows for **character customization**. If the player changes armor, the game simply "re-bakes" the sprite sheet in 2 seconds during the load, rather than the developer having to bake 10,000 PNG combinations offline.

### **6.2 Automation via GDScript**

For offline baking (saving to disk), we use a @tool script.

1. **Setup:** A scene with SubViewportContainer \-\> SubViewport \-\> Node3D (Model).  
2. **Script:** The script iterates through the AnimationPlayer's animation list.  
3. **Capture:** It calls anim\_player.seek(time) then await RenderingServer.frame\_post\_draw.  
4. **Save:** It grabs viewport.get\_texture().get\_image() and calls save\_png().22

Normal Maps in Godot:  
Godot's SubViewport can be set to "Debug Draw: Normal". However, a robust solution involves using a second SubViewport with a WorldEnvironment that overrides the material to a "Normal Output" shader. This runs in parallel with the color viewport, ensuring perfectly synced frames.14

## ---

**7\. The Backend Paradigm: SpacetimeDB and Serverless Architecture**

Moving from the visual layer to the network layer, we analyze the infrastructure of *BitCraft Online*, built by Clockwork Labs. This represents a radical departure from traditional "Dedicated Server" architectures used in Unreal/Unity multiplayer games.

### **7.1 Theoretical Framework: The Database *Is* The Server**

In a standard architecture, you have:

1. **Game Server (Headless Unity/Unreal):** Simulates physics, holds state in RAM.  
2. **Database (Postgres/Redis):** Persists data when the server saves.  
* *Flaw:* Sync issues. If the server crashes between saves, progress is lost. Scaling requires complex sharding.

**SpacetimeDB** unifies these. It is a relational database that executes game logic (WebAssembly modules) directly within the database transaction.2

* **Reducers:** Game logic functions (e.g., "CraftItem") are **Reducers**. They are ACID transactions. When a client requests a craft, the DB runs the reducer. If it succeeds, the state is updated and persisted instantly.  
* **Subscription:** Clients do not poll. They subscribe to queries (e.g., SELECT \* FROM MapObjects WHERE distance(player, obj) \< view\_distance). The DB pushes updates to the client via WebSocket whenever the result of that query changes.

### **7.2 Integrating SpacetimeDB with the Engines**

#### **7.2.1 Unity Integration (The Native Path)**

Since *BitCraft* is made in Unity, this is the most supported path.

1. **SDK:** Install the SpacetimeDB Unity SDK.  
2. **Module Definition:** Write the server module in C\# (or Rust). Define tables like struct Player { \[PrimaryKey\] u64 id; Vector3 pos; }.  
3. **Client Generation:** The spacetime CLI generates C\# client code. Player.FilterByPos(...) becomes available in Unity.  
4. **Synchronization:** The SpacetimeDBNetworkManager component handles the WebSocket connection. The developer simply registers callbacks: Player.OnInsert \+= SpawnPlayerAvatar;.25

#### **7.2.2 Unreal Engine 5 Integration**

Unreal support is experimental but viable via C++.

1. **C++ SDK:** The spacetimedb-sdk must be integrated as a third-party library in the Build.cs file of the Unreal project.  
2. **Blueprints:** To make this usable for designers (like in *Hades*), you must write a **C++ Wrapper** that exposes SpacetimeDB tables to Blueprints. You would create a USpacetimeManager class that handles the connection and broadcasts delegates (OnPlayerMoved) that Blueprints can bind to.  
3. **Data Types:** Unreal's FVector must be mapped to SpacetimeDB's coordinate schema manually in the wrapper.26

#### **7.2.3 Godot 4 Integration (The Rust/GDExtension Path)**

This is the high-performance choice for indie developers avoiding the overhead of C\#. Since SpacetimeDB is written in Rust, and Godot 4 has excellent Rust support via **GDExtension**, this integration is highly efficient.9  
**The Architecture:**

1. **GDExtension:** We do not use GDScript for the networking core. We create a Rust library using godot-rust/gdext.  
2. **Binding:** This Rust library imports the spacetimedb\_sdk crate.  
3. **Exposure:** The Rust code manages the SpacetimeDB connection and exposes high-level signals to Godot.  
   * *Rust:* \#\[signal\] fn on\_player\_update(id: i64, x: f32, y: f32);  
   * *Godot (GDScript):* network\_manager.connect("on\_player\_update", \_update\_sprite)  
4. **Advantage:** This bypasses the garbage collection overhead of C\# or GDScript for high-frequency network packets, aligning with the performance requirements of an MMO like *BitCraft*.

## ---

**8\. Comparative Analysis: Choosing the Right Stack**

To replicate the *Hades* \+ *BitCraft* target, we must select the engine that minimizes friction between the **Visual** requirement (Pre-rendered Sprites) and the **Backend** requirement (SpacetimeDB).

| Feature | Unreal Engine 5 | Unity 6 | Godot 4 |
| :---- | :---- | :---- | :---- |
| **Hades Visuals** | **Superior.** MRQ \+ Lumen creates the best source assets. Native Lua support (UnLua) allows replicating *Hades* scripting logic.28 | **Good.** Recorder works well. HDRP is powerful but complex to bake for sprites. C\# is standard. | **Efficient.** Runtime baking (SubViewport) allows unique mechanics. 2D engine is arguably better than Unity's for sprite handling. |
| **BitCraft Backend** | **Hard.** C++ SDK is verbose. requires significant boilerplate to expose to Blueprints. | **Native.** Official SDK support. "Happy Path" for SpacetimeDB. | **High Potential.** Rust GDExtension \+ SpacetimeDB Rust SDK is a "match made in heaven" for performance, but has a steep learning curve. |
| **Pipeline Cost** | **High.** Heavy editor, slow iteration for 2D. | **Medium.** Moderate iteration speed. | **Low.** Extremely fast iteration. Best for small teams. |

### **8.1 The Recommended "Hybrid" Pipeline Plan**

Based on the deep research, the optimal path for a small team aiming for this specific quality bar is:

1. **Asset Generation:** Use **Unreal Engine 5** strictly as a "Render Farm." Use Python scripts to batch-render high-fidelity characters to PNG/EXR sequences (Color \+ Normal \+ Emissive).  
2. **Runtime Engine:** Use **Godot 4**. Its 2D engine handles normal-mapped sprites natively and efficiently. It avoids the "overhead" of Unity/Unreal for pure 2D runtime logic.  
3. **Backend:** Use **SpacetimeDB** with **Rust GDExtension**. This provides the MMO scalability of *BitCraft* with the performance of native code, bound seamlessly to the Godot frontend.

### **8.2 Detailed Step-by-Step Setup Plan**

#### **Phase 1: Environment Setup**

* Install **Unreal Engine 5.4+** (Source Engine).  
* Install **Godot 4.3+** (.NET or Standard, Rust prefers Standard).  
* Install **Rust Toolchain** & **SpacetimeDB CLI**.  
* Initialize **LangGraph Research Agent** to monitor updates for "Godot Rust GDExtension" and "SpacetimeDB".

#### **Phase 2: The Visual Pipeline Setup (UE5 \-\> Godot)**

1. **In UE5:** Create BP\_BakerRig. Implement Python script render\_sprites.py using unreal.MoviePipelineQueueSubsystem.  
2. **Output:** Configure output to Project/Godot/assets/sprites/raw/.  
3. **In Godot:** Create an EditorPlugin import script. When new PNGs arrive in the raw folder, automatically convert them into AtlasTextures or SpriteFrames resources (.tres) with the Normal Maps linked to the CanvasTexture property.14

#### **Phase 3: The Backend Setup**

1. **Define Schema:** Write schema.spacetimedb (or Rust structs) defining Player, Inventory, WorldState.  
2. **Implement Logic:** Write Rust Reducers for movement and combat.  
3. **Bridge:** Compile the Rust GDExtension. Load it in Godot.  
4. **Sync:** In Godot, write a NetworkController.gd that instantiates the Rust class. Connect the on\_state\_update signals to the Sprite2D nodes managed by the visual pipeline.

## ---

**9\. Conclusion**

Replicating the development environments of Supergiant Games and Clockwork Labs requires a bifurcation of the traditional pipeline. One must accept that **3D is for asset generation**, not runtime display, and that **the database is for simulation**, not just storage.  
By leveraging Unreal Engine 5's Movie Render Queue for the visual fidelity of *Hades*, and integrating SpacetimeDB via Rust GDExtension in Godot for the scalability of *BitCraft*, an independent team can achieve a level of polish and scale that was previously the domain of massive studios. The glue holding this complex stack together is the **Agentic Research Pipeline**, ensuring that as these bleeding-edge tools evolve, the team's knowledge base evolves with them. This report provides the architectural blueprint to commence that journey.

## **10\. Appendix: Data Tables**

### **Table 1: Asset Pipeline Configuration Matrix**

| Configuration | Unreal Engine 5 (Source) | Unity 6 (Source) | Godot 4 (Source) |
| :---- | :---- | :---- | :---- |
| **Rendering Tech** | **Lumen / Path Tracer** | HDRP / Ray Tracing | Vulkan / SDFGI |
| **Automation API** | **Python (Native)** | C\# (Editor Scripting) | GDScript (@tool) |
| **Normal Map Baking** | **Native (Buffer Export)** | Replacement Shader Required | Viewport Shader Override |
| **Sprite Packing** | **Multilayer EXR (Native)** | PNG Sequence (Standard) | PNG Sequence (Standard) |
| **Best Use Case** | **High-Fidelity "Cinematic" Sprites** | Standard 3D-to-2D | Runtime / Dynamic Baking |

### **Table 2: SpacetimeDB Client Integration Comparison**

| Feature | Unity Integration | Godot Integration (Rust GDExtension) | Unreal Integration (C++) |
| :---- | :---- | :---- | :---- |
| **Language** | C\# | Rust | C++ |
| **Connection** | WebSocket (Managed) | WebSocket (Native) | WebSocket (Native) |
| **Code Gen** | Full C\# Classes | Full Rust Structs | Experimental / Manual |
| **Performance** | High (Garbage Collected) | **Maximum (No GC)** | Maximum (Manual Memory) |
| **Dev Friction** | **Low (Plug & Play)** | High (Compilation required) | High (Boilerplate heavy) |

#### **Works cited**

1. How the art style of the game Hades works, exactly? : r/gamedesign \- Reddit, accessed December 16, 2025, [https://www.reddit.com/r/gamedesign/comments/1co2rut/how\_the\_art\_style\_of\_the\_game\_hades\_works\_exactly/](https://www.reddit.com/r/gamedesign/comments/1co2rut/how_the_art_style_of_the_game_hades_works_exactly/)  
2. clockworklabs/SpacetimeDB: Multiplayer at the speed of light \- GitHub, accessed December 16, 2025, [https://github.com/clockworklabs/SpacetimeDB](https://github.com/clockworklabs/SpacetimeDB)  
3. SpacetimeDB, accessed December 16, 2025, [https://spacetimedb.com/](https://spacetimedb.com/)  
4. How to Build Your First AI Agent in 2025: Step-by-Step with Python & LangGraph, accessed December 16, 2025, [https://skywork.ai/blog/build-ai-agent-python-langgraph-step-by-step-2025/](https://skywork.ai/blog/build-ai-agent-python-langgraph-step-by-step-2025/)  
5. LangGraph 101: Let's Build A Deep Research Agent | Towards Data Science, accessed December 16, 2025, [https://towardsdatascience.com/langgraph-101-lets-build-a-deep-research-agent/](https://towardsdatascience.com/langgraph-101-lets-build-a-deep-research-agent/)  
6. Scraping the GDC Vault to find talks \- Professional \- Tech-Artists.Org, accessed December 16, 2025, [https://www.tech-artists.org/t/scraping-the-gdc-vault-to-find-talks/14943](https://www.tech-artists.org/t/scraping-the-gdc-vault-to-find-talks/14943)  
7. Agentic RAG: Step-by-Step Tutorial With Demo Project \- DataCamp, accessed December 16, 2025, [https://www.datacamp.com/tutorial/agentic-rag-tutorial](https://www.datacamp.com/tutorial/agentic-rag-tutorial)  
8. Agentic AI – LLM with Web Scraping – Beginner Bootcamp | Debabrata Pruseth, accessed December 16, 2025, [https://debabratapruseth.com/agentic-ai-llm-with-web-scraping-beginner-bootcamp/](https://debabratapruseth.com/agentic-ai-llm-with-web-scraping-beginner-bootcamp/)  
9. godot-rust/gdext: Rust bindings for Godot 4 \- GitHub, accessed December 16, 2025, [https://github.com/godot-rust/gdext](https://github.com/godot-rust/gdext)  
10. Web Scraping using Spider API | AutoGen 0.2 \- Microsoft Open Source, accessed December 16, 2025, [https://microsoft.github.io/autogen/0.2/docs/notebooks/agentchat\_webcrawling\_with\_spider/](https://microsoft.github.io/autogen/0.2/docs/notebooks/agentchat_webcrawling_with_spider/)  
11. Web Scrapping With AI\&LLM. Web scraping is the process of… | by DhanushKumar | Medium, accessed December 16, 2025, [https://medium.com/@danushidk507/web-scrapping-with-ai-llm-5c0b8f85cbfa](https://medium.com/@danushidk507/web-scrapping-with-ai-llm-5c0b8f85cbfa)  
12. 2d assets? :: Hades General Discussions \- Steam Community, accessed December 16, 2025, [https://steamcommunity.com/app/1145360/discussions/0/1738883810796606862/](https://steamcommunity.com/app/1145360/discussions/0/1738883810796606862/)  
13. Integrating 2D and 3D Game Art: Techniques for a Cohesive Visual Style, accessed December 16, 2025, [https://www.ixiegaming.com/blog/integrating-2d-and-3d-art/](https://www.ixiegaming.com/blog/integrating-2d-and-3d-art/)  
14. On turning 3D models into 2D sprites : r/godot \- Reddit, accessed December 16, 2025, [https://www.reddit.com/r/godot/comments/1mezwm9/on\_turning\_3d\_models\_into\_2d\_sprites/](https://www.reddit.com/r/godot/comments/1mezwm9/on_turning_3d_models_into_2d_sprites/)  
15. Unreal Rendering Workflow with Python Automation \- Tech Art Learning, accessed December 16, 2025, [https://www.xingyulei.com/post/ue-rendering-basic/index.html](https://www.xingyulei.com/post/ue-rendering-basic/index.html)  
16. Command Line Rendering With Unreal Engine Movie Render Queue | Community tutorial, accessed December 16, 2025, [https://dev.epicgames.com/community/learning/tutorials/nZ2e/command-line-rendering-with-unreal-engine-movie-render-queue](https://dev.epicgames.com/community/learning/tutorials/nZ2e/command-line-rendering-with-unreal-engine-movie-render-queue)  
17. Rendering High Quality Frames with Movie Render Queue in Unreal Engine \- Epic Games Developers, accessed December 16, 2025, [https://dev.epicgames.com/documentation/en-us/unreal-engine/rendering-high-quality-frames-with-movie-render-queue-in-unreal-engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/rendering-high-quality-frames-with-movie-render-queue-in-unreal-engine)  
18. Launch recordings from the command line | Recorder | 5.0.0 \- Unity \- Manual, accessed December 16, 2025, [https://docs.unity3d.com/Packages/com.unity.recorder@5.0/manual/CommandLineRecorder.html](https://docs.unity3d.com/Packages/com.unity.recorder@5.0/manual/CommandLineRecorder.html)  
19. Animation recorder: Unity's hidden feature | by Mohamed Hijazi | Bootcamp | Medium, accessed December 16, 2025, [http://medium.com/design-bootcamp/animation-recorder-unitys-hidden-feature-4153c0b8706f](http://medium.com/design-bootcamp/animation-recorder-unitys-hidden-feature-4153c0b8706f)  
20. 3D model to 2D sprite in Unity( Updated Tutorial) \- YouTube, accessed December 16, 2025, [https://www.youtube.com/watch?v=h\_ntDdUFUbo](https://www.youtube.com/watch?v=h_ntDdUFUbo)  
21. Turn 3D Models into 2D Sprites || Unity \- YouTube, accessed December 16, 2025, [https://www.youtube.com/watch?v=e9sb2\_ikFj4](https://www.youtube.com/watch?v=e9sb2_ikFj4)  
22. How can I bake 2D sprites in Godot at runtime? \- Game Development Stack Exchange, accessed December 16, 2025, [https://gamedev.stackexchange.com/questions/197017/how-can-i-bake-2d-sprites-in-godot-at-runtime](https://gamedev.stackexchange.com/questions/197017/how-can-i-bake-2d-sprites-in-godot-at-runtime)  
23. Hundreds of sprites Vs subviewport 3d for a 2d game \- Animation \- Godot Forum, accessed December 16, 2025, [https://forum.godotengine.org/t/hundreds-of-sprites-vs-subviewport-3d-for-a-2d-game/95402](https://forum.godotengine.org/t/hundreds-of-sprites-vs-subviewport-3d-for-a-2d-game/95402)  
24. Turn 3D Models & Particles Into Spritesheets in Godot (Fast & Automated) \- YouTube, accessed December 16, 2025, [https://www.youtube.com/watch?v=QA9VlEZZ-sw](https://www.youtube.com/watch?v=QA9VlEZZ-sw)  
25. 1 \- Setup | SpacetimeDB docs, accessed December 16, 2025, [https://spacetimedb.com/docs/unity/part-1](https://spacetimedb.com/docs/unity/part-1)  
26. 1 \- Setup | SpacetimeDB docs, accessed December 16, 2025, [https://spacetimedb.com/docs/unreal/part-1](https://spacetimedb.com/docs/unreal/part-1)  
27. killthejava / spacetimedb-gdchat · GitLab, accessed December 16, 2025, [https://gitlab.com/killthejava/spacetimedb-gdchat](https://gitlab.com/killthejava/spacetimedb-gdchat)  
28. Hades 2 is coded in LUA and you can edit the game files : r/pcgaming \- Reddit, accessed December 16, 2025, [https://www.reddit.com/r/pcgaming/comments/1cop25y/hades\_2\_is\_coded\_in\_lua\_and\_you\_can\_edit\_the\_game/](https://www.reddit.com/r/pcgaming/comments/1cop25y/hades_2_is_coded_in_lua_and_you_can_edit_the_game/)  
29. Tencent/sluaunreal: lua dev plugin for unreal engine 4 or 5 \- GitHub, accessed December 16, 2025, [https://github.com/Tencent/sluaunreal](https://github.com/Tencent/sluaunreal)