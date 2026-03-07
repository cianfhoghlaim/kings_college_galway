# **The Anam Initiative: Architectural Blueprints for High-Fidelity Meteorological Particle Simulation in Real-Time Environments**

## **1\. Introduction: The Convergence of Myth and Meteorology**

In the rapidly evolving landscape of real-time rendering, environmental fidelity has transitioned from static backdrops to dynamic, data-driven simulations. Contemporary players demand worlds that possess an internal logic and a responsiveness that mimics the complexity of reality. This report presents a comprehensive technical architecture for the **Anam Initiative**, a system designed to simulate the "Dust of the Celtic World." This visual effect creates a tangible, red-hued atmospheric medium that flows according to real-world meteorological data, drawing aesthetic inspiration from the "Dust of Krypton" depicted in the *Absolute Superman* comic series.1  
The core objective is to translate the narrative weight and visual density of the Kryptonian dust—described as a "gritty," "crystalline" substance carrying cultural memory—into a Celtic context. Here, the particle system, termed *Anam* (Irish for "soul"), serves as a manifestation of the land's spiritual history carried by the wind.3 To achieve this, the system must bridge the gap between coarse, global-scale weather data (GRIB2/NetCDF) and the fine-grained, fluid motion required for a believable game experience.  
The implementation relies on a sophisticated synthesis of **SpacetimeDB** for real-time data streaming, **Vector Quantization** for bandwidth efficiency, and **Strong Interpolation** (specifically optimized Bicubic/Catmull-Rom splines) to upsample discrete weather grids into continuous, organic flow fields. This document serves as a definitive implementation guide for Technical Directors and Senior Graphics Programmers, providing the mathematical foundations, backend architecture, and engine-specific shader implementations for Unreal Engine 5, Unity 6, and Godot 4\.

## **2\. Aesthetic and Conceptual Framework**

### **2.1 Visual Deconstruction: The Dust of Krypton**

The "Dust of Krypton" provides the primary visual benchmark for this system. Analysis of the *Absolute Superman* source material reveals that this is not a standard smoke effect. It acts as a distinct state of matter—particulate yet fluid, opaque yet glowing.

* **Granularity and Specularity:** Unlike diffuse fog, the dust is composed of distinct, macroscopic particles. These particles catch light individually, suggesting a crystalline or metallic composition typical of planetary debris.  
* **Chromatic Identity:** The defining hue is a deep, saturated red, symbolizing both the red sun of Krypton and the blood of a dead civilization.4 This color must persist across lighting conditions, requiring a specific shading model (e.g., Subsurface Scattering or unlit emissive cores with lit shells).  
* **Intentionality of Movement:** The dust does not merely drift; it flows with "gravitas," suggesting a semi-sentient connection to the protagonist.1 It forms tendrils and rivers, wrapping around characters rather than simply dispersing.

### **2.2 The Anam: Meteorological Soul of the Celtic World**

To adapt this sci-fi aesthetic to a Celtic fantasy setting, we map the properties of the Kryptonian dust to the concept of **Anam**. In Irish mythology, the wind is not empty air; it is often a vehicle for spirits or the *Sídhe* (fairies).

* **Anam and the Soul Wind:** The word *Anam* means "soul." The concept of *Anam Cara* ("soul friend") implies a binding connection.3 The particles should therefore exhibit "flocking" or cohesive behaviors, representing souls moving in unison rather than independent noise.  
* **The Sídhe Gaoithe:** The "Fairy Wind" (*Sídhe Gaoithe*) is a sudden, localized blast of wind believed to signal the passing of the fairy host.5 This justifies sudden, high-velocity streams of red dust traversing the map, driven by real-world wind gusts.  
* **Anemological Personality:** Irish folklore distinguishes between many wind types, from the gentle *feothan* to the violent *scuab*.6 The Anam system must visualize these distinctions: a *feothan* might be represented by sparse, glittering golden particles, while a storm-driven *scuab* would manifest as a dense, choking wall of red dust.

## **3\. The Mathematics of Flow: Strong Interpolation**

A critical technical challenge in this initiative is the resolution mismatch. Global weather models (like NOAA's GFS or ECMWF) typically provide data on a grid with a resolution of 0.25 degrees (approx. 27km) or, at best, 1-3km for regional models.7 In a game world, a player moves meters per second. If we drive particles using simple linear interpolation (Lerp) across a 10km grid cell, the wind vector remains nearly constant for thousands of frames, leading to static, lifeless movement. When a particle crosses a grid boundary, it snaps to a new vector, creating jagged, robotic motion.  
To achieve the "organic" flow of the Anam, we must employ **Strong Interpolation**. This refers to higher-order reconstruction filters that preserve the continuity of both the value (C0) and its rate of change (C1).

### **3.1 Bicubic Interpolation and Catmull-Rom Splines**

Bicubic interpolation considers a 4x4 neighborhood of grid points (16 samples) surrounding the target position $P(u, v)$. This allows the surface to curve smoothly between data points, simulating the fluid dynamics of air without running a full Navier-Stokes simulation on the client.  
We utilize the **Catmull-Rom** spline formulation for the weights. Unlike B-Splines, which do not pass through their control points (resulting in a smoothed but inaccurate field), Catmull-Rom splines pass through the data points, ensuring that the game world's wind accurately reflects the real-world data at the grid nodes.8  
The weight function $w(x)$ for a Catmull-Rom spline is defined as:

$$w(x) \= \\begin{cases} \\frac{1}{2}(3|x|^3 \- 5|x|^2 \+ 2\) & \\text{if } |x| \< 1 \\\\ \\frac{1}{2}(-|x|^3 \+ 5|x|^2 \- 8|x| \+ 4\) & \\text{if } 1 \\le |x| \< 2 \\\\ 0 & \\text{otherwise} \\end{cases}$$

### **3.2 The Fast Third-Order Texture Filtering Optimization**

Evaluating the full bicubic summation requires 16 texture fetches per particle per frame. For a system with 1 million particles, this incurs a prohibitive memory bandwidth cost (16 million reads/frame).  
We implement an optimization first detailed in *GPU Gems 2* and refined for modern hardware.9 This technique exploits the GPU's fixed-function bilinear filtering hardware. By mathematically adjusting the texture coordinates, we can combine the contributions of a 2x2 block of texels into a single bilinear sample.  
**The Algorithm:**

1. **Coordinate Decomposition:** Given a fractional position $f$ within a grid cell, we compute the four Catmull-Rom weights $w\_0, w\_1, w\_2, w\_3$ for the neighboring grid points.  
2. **Weight Combination:** We group these weights into two pairs: $(w\_0, w\_1)$ and $(w\_2, w\_3)$.  
3. Offset Calculation: We calculate a new texture coordinate $h$ that lies between the grid centers. The bilinear filter performs the weighted average based on distance. By solving for $h$ such that the linear weight matches the ratio of the cubic weights, we can fetch the weighted sum of two grid points in a single tap.

   $$h\_0 \= \\text{coord}\_0 \+ \\frac{w\_1}{w\_0 \+ w\_1}$$  
   $$h\_1 \= \\text{coord}\_2 \+ \\frac{w\_3}{w\_2 \+ w\_3}$$  
4. **Sampling:** We perform samples at $h\_0$ and $h\_1$. In 2D, this logic expands to 4 samples (combining 4x4 texels) instead of 16\.

This reduction from 16 taps to 4 taps maintains mathematical equivalence to the full bicubic filter (within floating-point precision) but runs 4x faster on the GPU.10 This efficiency is the cornerstone of making the Anam system viable in real-time.

## **4\. Backend Architecture: SpacetimeDB and Data Streaming**

To drive the Anam with real-world patterns, we require a persistent, authoritative backend. **SpacetimeDB** is uniquely suited for this due to its ability to run complex data processing modules directly within the database transaction scope, minimizing latency and architectural complexity.11

### **4.1 Meteorological Data Ingestion (Rust Module)**

The ingestion pipeline begins with the acquisition of GRIB2 or NetCDF files from meteorological agencies. These binary formats are highly compressed and complex.  
SpacetimeDB Module Design:  
We develop a server-side module in Rust, leveraging its safety and performance for binary parsing.13

1. **Crate Selection:** We utilize the grib crate 14 for GRIB2 or netcdf 16 for NetCDF. The grib crate is particularly effective as it allows iterating over the complex "messages" within a GRIB file without decoding the entire dataset into memory at once.  
2. **Extraction Logic:** The module extracts the *U-component* (velocity East) and *V-component* (velocity North) of the wind at 10m altitude. It also extracts *Pressure Reduced to MSL* to drive particle density.  
3. **Coordinate Mapping:** The module maps the spherical Lat/Long coordinates of the weather data to the Cartesian coordinate system of the game world. This requires a projection step (e.g., Mercator or local tangent plane) handled within the Rust module logic.

### **4.2 Spatial Partitioning and Storage**

Storing a global wind field as individual rows (e.g., WindPoint { x, y, u, v }) in a database is inefficient for bulk retrieval. A high-resolution grid might contain millions of points.  
The Chunking Strategy:  
We partition the world into spatial Chunks (e.g., 32x32 or 64x64 grid cells). Each chunk is stored as a single row in SpacetimeDB containing a binary blob (BLOB).

Rust

\#\[spacetimedb::table(name \= wind\_chunks, public)\]  
pub struct WindChunk {  
    \#\[primary\_key\]  
    pub chunk\_id: u64, // Spatial Hash (Morton Code / Hilbert Curve)  
    pub timestamp: SpacetimeType::Timestamp,  
    pub width: u8,  
    pub height: u8,  
    pub data: Vec\<u8\>, // Quantized Vector Field (The BLOB)  
}

This hybrid approach leverages the relational nature of SpacetimeDB for querying (e.g., "Get all chunks near Player X") while using binary storage for the dense vector data, significantly reducing overhead.17

### **4.3 Vector Quantization: The 8-bit Packet**

Streaming raw 32-bit floating-point vectors (8 bytes per pixel) is bandwidth-intensive. We employ **Vector Quantization** to compress the data into a compact 32-bit integer (RGBA8) format.19  
**Quantization Algorithm:**

1. **Normalization:** We define a MAX\_WIND\_SPEED (e.g., 50 m/s). The raw $U$ and $V$ floats are clamped to $\[-50, 50\]$ and normalized to $$.  
2. **Packing (RGBA8):**  
   * **Channel R (8-bit):** Quantized U-Wind. $R \= (U\_{norm} \\times 255)$.  
   * **Channel G (8-bit):** Quantized V-Wind. $G \= (V\_{norm} \\times 255)$.  
   * **Channel B (8-bit):** Turbulence/Variance. Derived from the gust factor or local variance in the GRIB data.  
   * **Channel A (8-bit):** Soul Density. Derived from atmospheric pressure. Low pressure (storms) \= High Alpha (255); High pressure (calm) \= Low Alpha (50).

This 4-byte-per-pixel format is 50% smaller than even half-precision floats and can be directly loaded into GPU textures without CPU-side decompression.21

## **5\. Engine Implementation A: Unreal Engine 5 (Niagara)**

Unreal Engine 5 is the primary target for high-fidelity visualization. **Niagara** provides the necessary infrastructure for GPU-driven simulation and custom HLSL stages.

### **5.1 The Data Interface: Grid2D Collection**

We do not update particles using standard CPU logic. We use Niagara's **Grid2D Collection** 23 as a GPU-side lookup table.  
**Streaming Pipeline:**

1. **Client Subscription:** The UE5 client connects to SpacetimeDB and subscribes to SELECT \* FROM wind\_chunks WHERE chunk\_id IN (...).  
2. **Texture Reconstruction:** Upon receiving a WindChunk BLOB, the client writes the TArray\<uint8\> data directly to a UTexture2D. Using RHICmdList.UpdateTexture2D, this operation is performed efficiently on the render thread.25  
3. **Niagara Binding:** This texture is passed to the Niagara System via a **Texture Data Interface** (UNiagaraDataInterfaceTexture).26

### **5.2 Custom HLSL Simulation Stage**

Standard Niagara sampling functions use simple linear interpolation. To implement the Anam aesthetic, we must inject our optimized bicubic logic.  
We create a **Custom HLSL** module in the Particle Update stage.  
**Reference HLSL Implementation (Niagara):**

High-level shader language

// Custom HLSL Node: BicubicWindSample  
// Inputs: Texture (Texture Object), Sampler (SamplerState), UV (float2), TexSize (float2)  
// Outputs: Velocity (float3)

float2 samplePos \= UV \* TexSize;  
float2 texPos1 \= floor(samplePos \- 0.5) \+ 0.5;  
float2 f \= samplePos \- texPos1;

// Catmull-Rom Weights  
float2 w0 \= f \* (-0.5 \+ f \* (1.0 \- 0.5 \* f));  
float2 w1 \= 1.0 \+ f \* f \* (-2.5 \+ 1.5 \* f);  
float2 w2 \= f \* (0.5 \+ f \* (2.0 \- 1.5 \* f));  
float2 w3 \= f \* f \* (-0.5 \+ 0.5 \* f);

// 4-Tap Optimization Offsets  
float2 w12 \= w1 \+ w2;  
float2 offset12 \= w2 / (w1 \+ w2);

float2 texPos0 \= texPos1 \- 1;  
float2 texPos3 \= texPos1 \+ 2;  
float2 texPos12 \= texPos1 \+ offset12;

texPos0 /= TexSize;  
texPos3 /= TexSize;  
texPos12 /= TexSize;

float4 result \= float4(0,0,0,0);

// Sampling the 4 optimized bilinear taps  
result \+= Texture.SampleLevel(Sampler, float2(texPos12.x, texPos0.y), 0\) \* w12.x \* w0.y;  
result \+= Texture.SampleLevel(Sampler, float2(texPos0.x, texPos12.y), 0\) \* w0.x \* w12.y;  
result \+= Texture.SampleLevel(Sampler, float2(texPos12.x, texPos12.y), 0\) \* w12.x \* w12.y;  
result \+= Texture.SampleLevel(Sampler, float2(texPos3.x, texPos12.y), 0\) \* w3.x \* w12.y;  
result \+= Texture.SampleLevel(Sampler, float2(texPos12.x, texPos3.y), 0\) \* w12.x \* w3.y;

// Unpack \[0..1\] to \[-50..+50\]  
float2 windVec \= (result.rg \* 2.0 \- 1.0) \* 50.0;  
Velocity \= float3(windVec.x, windVec.y, 0); // Assuming texture is XY, World is XYZ

Note: This code snippet adapts the 9-tap/5-tap optimization strategies discussed in technical literature.10

### **5.3 Large World Coordinates (LWC)**

UE5 utilizes double-precision floats for world coordinates.27 However, textures and shaders typically operate in single precision. We must **rebase** the particle positions.

* **Method:** Subtract the ChunkOrigin (Double) from the ParticlePosition (Double) to get a LocalPosition (Float).  
* **UV Calculation:** $UV \= \\text{LocalPosition} / \\text{ChunkSize}$. This ensures precision is maintained even at the edges of the world map.

## **6\. Engine Implementation B: Unity 6 (VFX Graph)**

Unity's **VFX Graph** is highly capable of handling millions of particles. The integration strategy here relies on **Texture2DArray** to handle the temporal aspect of weather data (interpolating between current and forecast states).

### **6.1 Temporal Interpolation with Texture2DArray**

Weather data is not static; it evolves. A standard texture represents a single moment. By using a Texture2DArray 28, we can store a sequence of weather states in the array slices (Z-axis).

* **Slice 0:** Current Hour ($T\_0$).  
* **Slice 1:** Next Hour ($T\_1$).  
* Shader Logic: We sample both slices using the bicubic method and linearly interpolate the results based on the current game time fraction $\\alpha$.

  $$V\_{final} \= \\text{Lerp}(\\text{Bicubic}(Slice\_0), \\text{Bicubic}(Slice\_1), \\alpha)$$

### **6.2 Implementation Details**

* **Custom HLSL Block:** VFX Graph supports Custom HLSL Blocks.30 We encapsulate the bicubic sampling logic into a reusable block node: SampleAnamWind.  
* **Data Updating:** To prevent main-thread hitching when uploading new weather textures, we utilize Texture2D.LoadRawTextureData combined with NativeArray. This allows the byte data from SpacetimeDB to be memcopied safely and efficiently.31

## **7\. Engine Implementation C: Godot 4 (Compute Shaders)**

Godot 4 offers a lower-level, high-performance approach via its **RenderingDevice (RD)** API and direct GLSL Compute Shaders.33 This approach bypasses the overhead of a particle system abstraction, granting raw control over memory.

### **7.1 Storage Buffers vs. Textures**

While textures are standard for vector fields, Godot's compute pipeline allows for **Structured Storage Buffers (SSBOs)**.

* **Concept:** Instead of a texture, we can pass the wind grid as a linearized array of floats in an SSBO.  
* **Trade-off:** Textures have dedicated hardware caches and bilinear filtering units. SSBOs do not. Since we are implementing *custom* bicubic filtering that relies on bilinear lookups for performance (the 4-tap trick), we **must** use a Texture sampler in the compute shader. An SSBO implementation would require manual software bilinear filtering, which is slower.34

### **7.2 The Compute Pipeline**

1. **Dispatch:** The main script dispatches a compute shader with dimensions $\\lceil N\_{particles} / 64 \\rceil$.  
2. **Bindings:**  
   * Set 0, Binding 0: Particle Data Buffer (Position, Velocity, Life).  
   * Set 0, Binding 1: Wind Texture (UniformSampler2D).  
3. **Rendering:** To render the result without reading data back to the CPU (which is slow), we use a MultiMeshInstance3D. In Godot 4.x, linking compute buffers directly to MultiMesh buffers requires specific RID manipulation or the use of **ShaderGlobals** to pass the position data to the vertex shader.35

## **8\. Conclusion: The Living Map**

The **Anam Initiative** represents a paradigm shift in how environmental effects are integrated into game worlds. By moving beyond randomized noise and anchoring the visual experience in real-world meteorological data, we create a system that feels expansive and grounded.  
The key to the success of this system is the **strong interpolation**. The difference between a standard linear lookup and the proposed **Bicubic** solution is the difference between "gamey," grid-locked movement and the organic, flowing "rivers of soul" envisioned in the concept.  
Whether implemented in Unreal Engine 5 via Niagara, Unity via VFX Graph, or Godot via Compute Shaders, the architecture remains consistent:

1. **Source:** Real-world GRIB2 data.  
2. **Transport:** SpacetimeDB with 8-bit Vector Quantization.  
3. **Synthesis:** GPU-accelerated Bicubic Interpolation.  
4. **Visual:** Red, subsurface-scattered particulates representing the Celtic *Anam*.

This architecture ensures that the "Dust of the Celtic World" is not merely a visual effect, but a networked, data-driven entity that respects the physics of our world while visualizing the metaphysics of the soul.

## ---

**9\. Appendix: Technical Reference**

### **Table 1: Comparative Engine Architecture for Anam**

| Feature | Unreal Engine 5 | Unity 6 | Godot 4 |
| :---- | :---- | :---- | :---- |
| **Primary System** | Niagara Particle System | VFX Graph | Compute Shaders (GLSL) |
| **Wind Data Storage** | Grid2D Collection | Texture2DArray | Texture2D (UniformSampler) |
| **Interpolation Logic** | Custom HLSL Module | Custom HLSL Block | GLSL Function |
| **Data Injection** | UNiagaraDataInterfaceTexture | LoadRawTextureData / NativeArray | RenderingDevice.texture\_update |
| **Rendering Method** | Sprite Renderer (Subsurface) | Output Particle Quad | MultiMeshInstance3D |
| **Coordinate System** | LWC (Double Precision) | Floating Origin / Local Space | Floating Origin / Local Space |
| **Implementation Complexity** | Moderate (Visual Scripting \+ HLSL) | Moderate (Graph \+ HLSL) | High (C++ / GLSL required) |

### **Table 2: 8-Bit Vector Quantization Schema (RGBA8)**

This packing scheme allows a 4-component vector field to be stored in a standard 32-bit color texture, maximizing compatibility and minimizing bandwidth.

| Channel | Byte Offset | Data Type | Source Value | Mapped Range | Description |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **Red** | 0 | uint8 | U-Wind (East) | 0 \- 255 | Normalized velocity. 0 \= \-50m/s, 128 \= 0m/s, 255 \= \+50m/s. |
| **Green** | 1 | uint8 | V-Wind (North) | 0 \- 255 | Normalized velocity. Same mapping as U-Wind. |
| **Blue** | 2 | uint8 | Turbulence | 0 \- 255 | Variance/Gust factor. Used to drive curl noise magnitude in shader. |
| **Alpha** | 3 | uint8 | Pressure | 0 \- 255 | Inverse pressure. 0 \= High Pressure (Clear), 255 \= Low Pressure (Storm). |

### **Table 3: Irish Meteorological Vocabulary for Anam States**

Mapping the Celtic wind lore to specific particle behaviors.

| Irish Term | Meaning | Particle Behavior | Trigger Condition |
| :---- | :---- | :---- | :---- |
| **Feothan** | Gentle Breeze | Sparse, glittering gold particles. Low velocity. | Low Wind Speed (\< 5 m/s) |
| **Gaoth** | Wind | Steady flow of red particles. Laminar flow. | Medium Wind Speed (5-15 m/s) |
| **Scuab** | Sweeping Wind | Dense, opaque rivers of dark red dust. Turbulent. | High Wind Speed (\> 15 m/s) |
| **Sídhe Gaoithe** | Fairy Wind | Sudden, spiraling vortex of glowing particles. | Localized Gust / Event Trigger |
| **Rua** | Red / Storm | Deep crimson hue, high opacity, shadowing enabled. | Low Pressure System (\< 990 hPa) |

#### **Works cited**

1. It's literally made out of the dust of his home planet. So he's wearing the last remains of Krypton on his back. He carries his entire culture. Nobody knows what Krypton was or that it existed except for him." : r/DCcomics \- Reddit, accessed December 18, 2025, [https://www.reddit.com/r/DCcomics/comments/1fzsiyf/jason\_aaron\_talks\_absolute\_supermans\_cape\_its/](https://www.reddit.com/r/DCcomics/comments/1fzsiyf/jason_aaron_talks_absolute_supermans_cape_its/)  
2. Absolute Superman's Death of Krypton Might be Untouchable (Review) \- ComicBook.com, accessed December 18, 2025, [https://comicbook.com/comics/news/absolute-supermans-death-of-krypton-might-be-untouchable-review/](https://comicbook.com/comics/news/absolute-supermans-death-of-krypton-might-be-untouchable-review/)  
3. Anam Cara \- Wikipedia, accessed December 18, 2025, [https://en.wikipedia.org/wiki/Anam\_Cara](https://en.wikipedia.org/wiki/Anam_Cara)  
4. REVIEW: Absolute Superman \#3 \- The Aspiring Kryptonian, accessed December 18, 2025, [https://theaspiringkryptonian.com/2025/01/01/review-absolute-superman-3/](https://theaspiringkryptonian.com/2025/01/01/review-absolute-superman-3/)  
5. Weather in Irish folklore \- West Cork People, accessed December 18, 2025, [https://westcorkpeople.ie/columnists/weather-in-irish-folklore/](https://westcorkpeople.ie/columnists/weather-in-irish-folklore/)  
6. Some (60+) Irish Words and Phrases for Breeze, Wind, Gust, Squall, and Gale. Oh, and Zephyr\! \- Transparent Language Blog, accessed December 18, 2025, [https://blogs.transparent.com/irish/some-60-irish-words-and-phrases-for-breeze-wind-gust-squall-and-gale-oh-and-zephyr/](https://blogs.transparent.com/irish/some-60-irish-words-and-phrases-for-breeze-wind-gust-squall-and-gale-oh-and-zephyr/)  
7. GRIB1, GRIB2, NetCDF: What do I use? \- Geoscience Data Exchange (GDEX), accessed December 18, 2025, [https://gdex.ucar.edu/news/grib1-grib2-netcdf-what-do-i-use/](https://gdex.ucar.edu/news/grib1-grib2-netcdf-what-do-i-use/)  
8. GLSL Vertex shader bilinear sampling heightmap \- Stack Overflow, accessed December 18, 2025, [https://stackoverflow.com/questions/26130379/glsl-vertex-shader-bilinear-sampling-heightmap](https://stackoverflow.com/questions/26130379/glsl-vertex-shader-bilinear-sampling-heightmap)  
9. \[GLSL\] Simple, fast bicubic filtering shader function \- Shared Code \- JVM Gaming, accessed December 18, 2025, [https://jvm-gaming.org/t/glsl-simple-fast-bicubic-filtering-shader-function/52549](https://jvm-gaming.org/t/glsl-simple-fast-bicubic-filtering-shader-function/52549)  
10. An HLSL function for sampling a 2D texture with Catmull-Rom filtering, using 9 texture samples instead of 16 \- GitHub Gist, accessed December 18, 2025, [https://gist.github.com/TheRealMJP/c83b8c0f46b63f3a88a5986f4fa982b1](https://gist.github.com/TheRealMJP/c83b8c0f46b63f3a88a5986f4fa982b1)  
11. SpacetimeDB, accessed December 18, 2025, [https://spacetimedb.com/](https://spacetimedb.com/)  
12. Overview | SpacetimeDB docs, accessed December 18, 2025, [https://spacetimedb.com/docs/](https://spacetimedb.com/docs/)  
13. spacetimedb \- crates.io: Rust Package Registry, accessed December 18, 2025, [https://crates.io/crates/spacetimedb](https://crates.io/crates/spacetimedb)  
14. grib-build \- crates.io: Rust Package Registry, accessed December 18, 2025, [https://crates.io/crates/grib-build](https://crates.io/crates/grib-build)  
15. noritada/grib-rs: GRIB format parser for Rust \- GitHub, accessed December 18, 2025, [https://github.com/noritada/grib-rs](https://github.com/noritada/grib-rs)  
16. georust/netcdf: High-level netCDF bindings for Rust \- GitHub, accessed December 18, 2025, [https://github.com/georust/netcdf](https://github.com/georust/netcdf)  
17. Should I use BLOB or Tables for storing large data? \- Software Engineering Stack Exchange, accessed December 18, 2025, [https://softwareengineering.stackexchange.com/questions/284496/should-i-use-blob-or-tables-for-storing-large-data](https://softwareengineering.stackexchange.com/questions/284496/should-i-use-blob-or-tables-for-storing-large-data)  
18. At which point does storing a large amount of structured data as BLOB make sense?, accessed December 18, 2025, [https://stackoverflow.com/questions/69980695/at-which-point-does-storing-a-large-amount-of-structured-data-as-blob-make-sense](https://stackoverflow.com/questions/69980695/at-which-point-does-storing-a-large-amount-of-structured-data-as-blob-make-sense)  
19. What Is Vector Quantization? \- EDB, accessed December 18, 2025, [https://www.enterprisedb.com/blog/what-is-vector-quantization](https://www.enterprisedb.com/blog/what-is-vector-quantization)  
20. 8-bit Rotational Quantization: How to Compress Vectors by 4x and Improve the Speed-Quality Tradeoff of Vector Search | Weaviate, accessed December 18, 2025, [https://weaviate.io/blog/8-bit-rotational-quantization](https://weaviate.io/blog/8-bit-rotational-quantization)  
21. Packing floats into RGBA8 textures \- OpenGL \- Khronos Forums, accessed December 18, 2025, [https://community.khronos.org/t/packing-floats-into-rgba8-textures/55846](https://community.khronos.org/t/packing-floats-into-rgba8-textures/55846)  
22. Encode floating point data in a RGBA texture \- Stack Overflow, accessed December 18, 2025, [https://stackoverflow.com/questions/34963366/encode-floating-point-data-in-a-rgba-texture](https://stackoverflow.com/questions/34963366/encode-floating-point-data-in-a-rgba-texture)  
23. System Update Group Reference for Niagara Effects in Unreal Engine, accessed December 18, 2025, [https://dev.epicgames.com/documentation/en-us/unreal-engine/system-update-group-reference-for-niagara-effects-in-unreal-engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/system-update-group-reference-for-niagara-effects-in-unreal-engine)  
24. Niagara Grid 2D feels like a superpower\! Drawing Locations To a Render Target in Unreal 5.1 \- ZukoMedia \- Chris Zuko Official Site, accessed December 18, 2025, [https://www.zukomedia.com/articles/niagara-grid-2d-feels-like-a-superpower-drawing-locations-to-a-render-target-in-unreal-5-1](https://www.zukomedia.com/articles/niagara-grid-2d-feels-like-a-superpower-drawing-locations-to-a-render-target-in-unreal-5-1)  
25. Updating texture used in post processing with data on the GPU without copying to CPU and back. \- Rendering \- Unreal Engine Forum, accessed December 18, 2025, [https://forums.unrealengine.com/t/updating-texture-used-in-post-processing-with-data-on-the-gpu-without-copying-to-cpu-and-back/125298](https://forums.unrealengine.com/t/updating-texture-used-in-post-processing-with-data-on-the-gpu-without-copying-to-cpu-and-back/125298)  
26. How to pass a Texture to a .usf file through Niagara ? (UE5) \- Real Time VFX, accessed December 18, 2025, [https://realtimevfx.com/t/how-to-pass-a-texture-to-a-usf-file-through-niagara-ue5/26113](https://realtimevfx.com/t/how-to-pass-a-texture-to-a-usf-file-through-niagara-ue5/26113)  
27. Large World Coordinates in Unreal Engine 5 \- Epic Games Developers, accessed December 18, 2025, [https://dev.epicgames.com/documentation/en-us/unreal-engine/large-world-coordinates-in-unreal-engine-5](https://dev.epicgames.com/documentation/en-us/unreal-engine/large-world-coordinates-in-unreal-engine-5)  
28. Sample Texture2DArray | Visual Effect Graph | 10.2.2 \- Unity \- Manual, accessed December 18, 2025, [https://docs.unity3d.com/Packages/com.unity.visualeffectgraph@10.2/manual/Operator-SampleTexture2DArray.html](https://docs.unity3d.com/Packages/com.unity.visualeffectgraph@10.2/manual/Operator-SampleTexture2DArray.html)  
29. Sample a 2D texture array in a shader \- Unity \- Manual, accessed December 18, 2025, [https://docs.unity3d.com/6000.2/Documentation/Manual/class-Texture2DArray-use-in-shader.html](https://docs.unity3d.com/6000.2/Documentation/Manual/class-Texture2DArray-use-in-shader.html)  
30. Custom HLSL Block | Visual Effect Graph | 16.0.6 \- Unity \- Manual, accessed December 18, 2025, [https://docs.unity3d.com/Packages/com.unity.visualeffectgraph@16.0/manual/Block-CustomHLSL.html](https://docs.unity3d.com/Packages/com.unity.visualeffectgraph@16.0/manual/Block-CustomHLSL.html)  
31. Scripting API: Texture2D.LoadRawTextureData \- Unity \- Manual, accessed December 18, 2025, [https://docs.unity3d.com/ScriptReference/Texture2D.LoadRawTextureData.html](https://docs.unity3d.com/ScriptReference/Texture2D.LoadRawTextureData.html)  
32. Support native multi-platform non-blocking texture loading · Issue \#9 · KhronosGroup/UnityGLTF \- GitHub, accessed December 18, 2025, [https://github.com/KhronosGroup/UnityGLTF/issues/9](https://github.com/KhronosGroup/UnityGLTF/issues/9)  
33. Using compute shaders — Godot Engine (latest) documentation in English, accessed December 18, 2025, [https://docs.godotengine.org/en/latest/tutorials/shaders/compute\_shaders.html](https://docs.godotengine.org/en/latest/tutorials/shaders/compute_shaders.html)  
34. Add ability to pass compute buffers to vertex/fragment shaders · Issue \#6989 · godotengine/godot-proposals \- GitHub, accessed December 18, 2025, [https://github.com/godotengine/godot-proposals/issues/6989](https://github.com/godotengine/godot-proposals/issues/6989)  
35. Computer Shader Example \- Could it use Multimeshinstance? : r/godot \- Reddit, accessed December 18, 2025, [https://www.reddit.com/r/godot/comments/1kkxxwm/computer\_shader\_example\_could\_it\_use/](https://www.reddit.com/r/godot/comments/1kkxxwm/computer_shader_example_could_it_use/)