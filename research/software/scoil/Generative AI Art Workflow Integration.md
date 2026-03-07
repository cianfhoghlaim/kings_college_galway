# **Architecting Agentic Creative Workflows: Deep Research into Generative AI Integration for React Ecosystems**

## **Executive Summary**

This report presents a comprehensive architectural blueprint for integrating self-hosted generative AI into a professional React/TanStack Start/Shadcn development workflow. The primary objective is to autonomously generate high-fidelity "orthogonal retro pixel art" assets—specifically mirroring the aesthetic of the PostHog brand, such as interactive isometric maps—through an agentic coding interface orchestrated via the Model Context Protocol (MCP).  
The research addresses the user's specific transition from cloud-based Bria Fibo to self-hosted alternatives, with a critical evaluation of the Apple Silicon ecosystem. It contrasts the mature, graph-based capabilities of **InvokeAI** (running on PyTorch/MPS) against the emerging, high-performance **MLX** framework preferred by the user. While MLX offers superior raw throughput and memory efficiency on Mac, InvokeAI is identified as the pragmatic "workflow engine" due to its robust node graph architecture, which is essential for the multi-stage ControlNet pipelines required to enforce strict isometric geometry.  
The proposed solution establishes a "Human-in-the-Loop" development paradigm where the IDE acts as both the code editor and the creative director. By leveraging the **invokeai-mcp-server**, developers can instruct an AI agent to "generate a pixel-art British Isles map matching this SVG viewport," triggering a complex local generation graph that produces assets perfectly aligned with the frontend's coordinate system.

## **1\. The Agentic Asset Generation Paradigm**

The traditional software development lifecycle separates design and implementation into distinct phases, often mediated by asset handoffs and external tools like Figma or Blender. The "Agentic Asset Generation Paradigm" collapses this separation. In this new model, the asset generation engine is embedded directly into the Integrated Development Environment (IDE) context. The coding agent—whether Claude Code, Cursor, or a custom LLM orchestration—does not merely request code; it requests *assets* that it then immediately implements, styles, and binds to interaction logic.

### **1.1 The Shift from Rigid APIs to Fluid Protocols**

Historically, integrating AI required bespoke API wrappers (e.g., OpenAI SDK, Stability SDK) that were often brittle and limited to specific endpoints. The introduction of the **Model Context Protocol (MCP)** represents a fundamental shift. MCP acts as a universal "USB-C port" for AI models, allowing an LLM within an IDE to "see" local tools and "actuate" them without custom glue code for every new integration.1  
In the context of this specific workflow, MCP serves two distinct, critical functions:

1. **Deep Context Injection:** It allows the LLM to read the live state of the React application. The agent can "see" the exact pixel dimensions of a \<div\>, the color palette defined in tailwind.config.js, and the thematic requirements of the UI components (Shadcn tokens).1 This eliminates the "blind guess" nature of standard prompting.  
2. **Structured Tool Execution:** It enables the LLM to trigger complex generation graphs on the local InvokeAI server. Instead of a simple text prompt, the agent can construct sophisticated JSON payloads that define geometric constraints, ensuring the generated assets fit physically into the layout.1

### **1.2 Replacing Bria Fibo: The Move to Local Sovereignty**

The user's prior interest in Bria Fibo stemmed from its "JSON input" capability—a structured way to describe an image scene programmatically.3 This is a crucial insight: developers prefer *deterministic structure* over vague artistic prompting.  
Moving to a self-hosted model on Hugging Face and InvokeAI does not mean losing this structure; in fact, it enhances it. InvokeAI's core architecture is built around **Node Graphs**.5 Every image generation is the result of a Directed Acyclic Graph (DAG) execution.

* **Bria Fibo Approach:** Send a JSON description of the scene \-\> Receive Image.  
* **InvokeAI Graph Approach:** Send a JSON definition of the *pipeline* (Noise source \+ Latents \+ ControlNet inputs \+ VAE Decoder) \-\> Receive Image.

This shift grants the developer "God-mode" control over the generation process. We can replicate Bria's structured inputs by having the Agent construct these graph JSONs programmatically, ensuring that "orthogonal projection" is not just a suggestion in the prompt, but a mathematical constraint enforced by the pipeline nodes.

## **2\. The Inference Engine: Apple Silicon Strategy (MPS vs. MLX)**

For a developer working on Mac, the choice of inference engine dictates the speed, memory footprint, and feasibility of this workflow. The user expressed a specific preference for **GGUF** and **MLX**, necessitating a nuanced comparison with the standard **InvokeAI (MPS)** stack.

### **2.1 The Apple Silicon Landscape**

Apple Silicon (M1/M2/M3) utilizes a Unified Memory Architecture (UMA), which fundamentally changes how large models are loaded. Data does not need to be copied between CPU RAM and GPU VRAM; it exists in a single addressable space.

#### **2.1.1 InvokeAI and the MPS Backend**

InvokeAI is built on top of **PyTorch**. On macOS, PyTorch utilizes the **Metal Performance Shaders (MPS)** backend to accelerate tensor operations on the Mac's GPU.7

* **Pros:** It is a mature, battle-tested ecosystem. It supports the vast majority of the Stable Diffusion community assets (ControlNets, IP-Adapters, LoRAs, various VAEs) out of the box because they are all native PyTorch modules.8  
* **Cons:** It carries the overhead of the full PyTorch framework. While highly optimized, it does not fully exploit the unified memory architecture to the same extent as native frameworks, occasionally leading to slower "time-to-first-token" or image generation compared to bare-metal solutions.9

#### **2.1.2 The MLX Framework and GGUF**

**MLX** is an array framework designed by Apple Machine Learning Research specifically for Apple Silicon.10 It is designed to be familiar to NumPy/PyTorch users but executes directly on Apple's hardware with lazy evaluation and unified memory optimizations.

* **Pros:** It offers superior performance and memory efficiency for specific workloads. Benchmarks often show MLX outperforming MPS in training and high-throughput inference.9  
* **Cons:** It is a younger ecosystem. While DiffusionKit 12 allows running Stable Diffusion models on MLX, it lacks the rich, drag-and-drop node graph ecosystem of InvokeAI. Porting a complex workflow (e.g., "SDXL \+ Isometric LoRA \+ Depth ControlNet \+ Pixel VAE") to pure MLX currently requires writing custom Python scripts rather than configuring a graph.

**GGUF** is a file format optimized for quantization (running large models in less memory), primarily associated with the llama.cpp ecosystem.13 While stable-diffusion.cpp allows running diffusion models in GGUF format, integration with advanced features like ControlNet and regional prompting is significantly less developed than in the .safetensors/InvokeAI ecosystem.

### **2.2 The Strategic Decision: InvokeAI as the Workflow Engine**

While the user favors MLX, the **architectural recommendation** for this specific "Agentic Workflow" is to utilize **InvokeAI on MPS**.  
**Rationale:** The requirement is not just "generate an image efficiently" (where MLX shines), but "integrate deeply into a React workflow with complex geometric constraints" (where InvokeAI shines). InvokeAI's graph execution model 6 allows the Agent to manipulate the *logic* of generation. Replicating the specific "PostHog Orthogonal Pixel Art" style requires strict ControlNet guidance, specific LoRA chaining, and VAE swapping—features that are plug-and-play in InvokeAI but would require significant engineering effort to reimplement in a raw MLX script.  
**Compromise for the Power User:** If maximum performance is non-negotiable, the user can run a hybrid stack:

1. **Primary Workflow:** InvokeAI (MPS) for the complex, interactive asset generation.  
2. **Background Worker:** A standalone **DiffusionKit** (MLX) server 12 for batch generation of simpler textures or assets where ControlNet is not required, exposed via a custom lightweight MCP server.

However, for the primary "Interactive Map" use case, InvokeAI's node graph is the critical enabler.

## **3\. The Target Aesthetic: Deconstructing PostHog's Orthogonal Pixel Art**

To replicate the visual style seen in PostHog's marketing assets (e.g., the interactive British Isles map), we must deconstruct the aesthetic into reproducible technical constraints that the AI can understand. PostHog’s style is characterized by a specific blend of technical precision and retro whimsy.14

### **3.1 Visual Signatures**

1. **Orthogonal (Isometric) Projection:** Objects do not recede into the distance; parallel lines remain parallel. This is essential for tile-based interactive maps, as it allows sprites to be tiled seamlessly without perspective distortion.16  
2. **Pixel Art Dithering & Palette:** The images utilize limited color palettes, sharp edges, and intentional aliasing (dithering) to mimic the constraints of 16-bit hardware.17  
3. **"Hog" & Tech Themes:** The integration of whimsical elements (hedgehogs, retro computers, server racks) into technical diagrams is a core brand identifier.14

### **3.2 The Challenge: Diffusion vs. Pixel Precision**

Standard diffusion models (SD1.5, SDXL) operate in a continuous latent space. They naturally gravitate towards smooth gradients, anti-aliased edges, and "photorealistic" lighting—the exact opposite of pixel art. When prompted for "pixel art," raw models often produce "pixel art style" images that are actually blurry JPEGs of pixel art, rather than true pixel-perfect grids.  
The Solution Stack:  
To force the model into the "pixel grid," we must employ a specific chain of technologies:

* **Model:** **SDXL** is preferred over SD1.5 for its superior understanding of composition and "orthographic" concepts at high resolutions (1024x1024), which provides enough pixel density for the downscaling techniques used in pixel art generation.18  
* **LoRA (Low-Rank Adaptation):** Utilizing a specialized LoRA is non-negotiable.  
  * **"Pixel Art XL"**: This LoRA shifts the model's weights to bias towards sharp transitions and limited palettes.18  
  * **"Isometric Setting"**: This LoRA enforces the specific 30-degree camera angle required for isometric projection, preventing the model from generating standard "front-facing" or "top-down" views.16  
* **VAE (Variational Autoencoder):** Standard VAEs introduce blurring during the decoding step (Latent \-\> Pixel). A **"Pixelate VAE"** or a post-processing workflow (downscale by 8x, then upscale with Nearest Neighbor interpolation) is required to snap the generated gradients onto a fixed pixel grid.17

### **3.3 Geometric Control via ControlNet**

Prompting "isometric map of UK" alone will yield inconsistent geography—a "dream" of the UK rather than a map compatible with data. The geometry must be constrained to match the actual interactive surface.  
**The ControlNet Strategy:**

* **Input:** A high-contrast, black-and-white SVG of the British Isles, rendered by the React application itself (using d3-geo).  
* **ControlNet Model:** **ControlNet Depth** or **ControlNet Canny** for SDXL.19  
  * **Depth:** Interprets the white landmass against the black ocean as "closer," effectively extruding the map shape from the sea.  
  * **Canny:** Locks onto the exact coastline edges, ensuring the generated pixel art texture stays strictly within the bounds defined by the SVG.

This setup ensures that the generated "art" aligns perfectly with the "code" (the interactive SVG overlay).

## **4\. The Bridge: Model Context Protocol (MCP) Integration**

The core innovation in this workflow is the **"Agent-in-the-Loop"** capability provided by the Model Context Protocol (MCP). Instead of the developer switching contexts to a web UI, generating an asset, downloading it, and manually placing it in the project, the IDE's AI Agent orchestrates the entire process.

### **4.1 The MCP Architecture**

The MCP integration consists of three components working in concert:

1. **MCP Host:** The coding environment (e.g., **Claude Desktop**, **Cursor**, or a VS Code extension). This is the "brain" that reasons about the code.21  
2. **MCP Server:** A Python service (invokeai-mcp-server) that translates natural language or schema-based requests into InvokeAI Graph payloads.1  
3. **InvokeAI Instance:** The local backend performing the actual heavy lifting (inference).

### **4.2 The invokeai-mcp-server Capabilities**

The research identifies the invokeai-mcp-server as the existing implementation for this bridge.1 It exposes tools that map directly to the requirements:

* generate\_image: The primary tool. It accepts parameters like prompt, model\_key, lora\_key, controlnet\_args, and scheduler.  
* list\_models: Enables the agent to query which pixel art models and LoRAs are currently installed on the local instance.  
* get\_queue\_status: Allows the agent to poll for generation completion, handling the asynchronous nature of image generation.1

**Critical Insight:** The MCP server abstracts the complexity of the Graph API. Instead of the Agent needing to construct a 500-line JSON graph definition from scratch, it calls generate\_image(prompt="pixel art map...", style="isometric"). The MCP server's internal logic then constructs the necessary graph nodes (Noise \-\> Denoise \-\> VAE Decode) and submits them to InvokeAI.1

### **4.3 Customizing the MCP Server for Graph Execution**

While the standard generate\_image tool is sufficient for basic generation, the specific "PostHog" workflow requires advanced control (e.g., passing a specific base64 mask image to a ControlNet).  
The report recommends extending the MCP server with a specialized tool: execute\_graph.

* **Purpose:** To give the Agent "God-mode" access to the backend.  
* **Mechanism:** The Agent constructs a raw JSON object conforming to InvokeAI's Graph schema.5 This allows the Agent to build custom pipelines on the fly—for example, a "Hires Fix" pipeline that generates a low-res pixel image and then upscales it, or a pipeline that uses multiple ControlNets (Depth \+ Canny) simultaneously.  
* **Justification:** This capabilities mirrors the "Bria Fibo" structured input preference. The Agent becomes the "API Client," constructing the exact JSON structure needed to produce the desired result.

## **5\. Implementation Guide: The "Interactive British Isles" Case Study**

This section provides a detailed, step-by-step workflow for implementing the specific "PostHog-style Interactive Map" requested by the user.

### **5.1 Prerequisites and Environment Setup**

1. **InvokeAI Installation:** Install InvokeAI in a dedicated Python 3.11 environment (conda create \-n invokeai python=3.11) to ensure compatibility with Apple Silicon drivers.7  
2. **Model Loading:** Download sd\_xl\_base\_1.0.safetensors, pixel-art-xl.safetensors, isometric-setting-xl.safetensors, and controlnet-depth-sdxl-1.0.1  
3. **MCP Server Setup:** Install invokeai-mcp-server via pip. Configure the MCP Host (e.g., \~/.cursor/mcp.json) to point to this server:  
   JSON  
   {  
     "mcpServers": {  
       "invokeai": {  
         "command": "python",  
         "args": \["-m", "invokeai\_mcp\_server"\],  
         "env": { "INVOKEAI\_BASE\_URL": "http://127.0.0.1:9090" }  
       }  
     }  
   }

.21

### **5.2 Step 1: Generating the "Ground Truth" Mask**

The Agent cannot just "guess" the shape of the British Isles if the map needs to be interactive. We need a reliable source of truth.

* **Action:** The developer instructs the Agent: *"Create a temporary React component using d3-geo that renders the British Isles. Use an orthographic projection rotated to look isometric. Render the land as pure white and the sea as pure black."*  
* **Mechanism:** The Agent writes a script using **Puppeteer** (or a similar headless browser tool) to render this specific component to a hidden canvas and snapshot it as a PNG.24  
* **Output:** A base64-encoded string representing the "Control Image." This image represents the *exact* clickable area of the final map.

### **5.3 Step 2: The Agentic Generation Request**

With the Control Image in hand, the Agent constructs the request to InvokeAI via MCP.

* **Tool Call:** generate\_image  
* **Prompt:** "Orthogonal pixel art map of the British Isles, emerald green grass, retro video game style, 16-bit color palette, dithering, masterpiece. \[pixel art\], \[isometric setting\]"  
* **Negative Prompt:** "3d render, blurry, smooth, anti-aliased, perspective distortion, photography, text, labels"  
* **ControlNet Arguments:**  
  * image:  
  * model: "depth" (The white landmass is interpreted as "raised" geometry).  
  * weight: 1.0 (Strict adherence to the coastline).  
  * resize\_mode: "envelope" (Ensure the map fits strictly within the bounds).

### **5.4 Step 3: Asset Retrieval and Post-Processing**

The invokeai-mcp-server monitors the queue and returns the URL of the generated image once complete.

* **Refinement Loop:** The Agent displays the image in the chat. If the user says "Too much water," the Agent adjusts the prompt and re-runs the tool.  
* **Pixel-Perfect Scaling:** If the generated image is 1024x1024 but the React component is 512x512, simply resizing via CSS will cause blurring. The Agent can be instructed to use a "Pixel Perfect Downscale" node (custom node) or simply write a sharp CSS rule: image-rendering: pixelated;.

### **5.5 Step 4: Frontend Integration (Shadcn \+ D3)**

The final step is binding the generated visual to the interactive logic.

* **Layer 0 (Visuals):** The InvokeAI-generated PNG is placed as the backgroundImage of a div.  
* **Layer 1 (Interaction):** The original D3 SVG (from Step 1\) is overlaid on top. Crucially, its fill is set to transparent.  
* **Events:** Shadcn Tooltip components are triggered by the onMouseEnter events of the invisible SVG paths.  
  * *User Interaction:* The user hovers over "Ireland" on the pixel art map.  
  * *System Logic:* The mouse enters the invisible SVG path for Ireland.  
  * *Feedback:* A Shadcn tooltip appears: "Ireland: 5.0M Users." A semi-transparent white layer highlights the region, mimicking a "selection" effect over the pixel art.

## **6\. Advanced Topics: Customization and Future Proofing**

### **6.1 Bria Fibo Replacement: The "Graph JSON" Strategy**

The user specifically liked Bria Fibo's JSON input. We can replicate this by exposing the raw **InvokeAI Graph JSON** schema to the Agent.

* **Concept:** Instead of abstract parameters, the Agent generates the full node graph definition.  
* **Benefit:** This allows for "Regional Prompting." The Agent can define one text prompt for the "Land" region ("grassy fields, pixel art") and a different prompt for the "Ocean" region ("blue water, dithering pattern"), masking them using the same SVG paths used for the map.20 This replicates the structured, multi-element control of Bria Fibo entirely locally.

### **6.2 MLX: The "High Performance" Path**

If the user's requirement for **MLX** is absolute (e.g., for battery life on a MacBook Air), the workflow can be adapted.

* **DiffusionKit:** This library runs SDXL on MLX.12  
* **Implementation:** The developer would need to write a custom Python script that wraps DiffusionKit and exposes it as a simple MCP server.  
* **Trade-off:** This sacrifices the robust ControlNet and Graph features of InvokeAI. It is recommended only if the InvokeAI/MPS stack proves too resource-intensive.

## **7\. Conclusion**

This research confirms that integrating generative AI into a professional React workflow is not only feasible but highly advantageous when architected correctly. By moving from cloud APIs to a self-hosted **InvokeAI** instance on **Apple Silicon**, developers gain autonomy, privacy, and zero-cost experimentation.  
The **Model Context Protocol (MCP)** serves as the critical connective tissue, transforming the IDE from a passive text editor into an active creative studio. By following the "Interactive British Isles" blueprint—leveraging **SDXL**, **ControlNet Depth**, and **Orthogonal LoRAs**—developers can achieve the distinct "PostHog" aesthetic with programmatic precision.

| Component | Choice | Rationale |
| :---- | :---- | :---- |
| **Inference Engine** | **InvokeAI (MPS)** | Best-in-class Node/Graph architecture; robust ControlNet support; existing MCP server. |
| **Hardware** | **Apple Silicon** | Efficient local inference; zero marginal cost; high VRAM availability (Unified Memory). |
| **Protocol** | **MCP** | Standardized, secure agentic control; separates tool logic from agent reasoning. |
| **Model Stack** | **SDXL \+ Pixel LoRA** | High fidelity; native resolution suitable for modern displays; specific "retro" aesthetic training. |
| **Frontend** | **React/Shadcn/D3** | Robust ecosystem; perfect for overlaying precise interaction logic on static AI assets. |

This architecture enables a future where "assets" are not static files downloaded from a designer, but dynamic resources compiled alongside the code itself.

#### **Works cited**

1. invokeai-mcp-server 1.0.1 on PyPI \- Libraries.io, accessed December 15, 2025, [https://libraries.io/pypi/invokeai-mcp-server](https://libraries.io/pypi/invokeai-mcp-server)  
2. What is Model Context Protocol (MCP)? A guide \- Google Cloud, accessed December 15, 2025, [https://cloud.google.com/discover/what-is-model-context-protocol](https://cloud.google.com/discover/what-is-model-context-protocol)  
3. Bria.ai | Generate AI Images at Scale, accessed December 15, 2025, [https://bria.ai/](https://bria.ai/)  
4. briaai/FIBO \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/briaai/FIBO](https://huggingface.co/briaai/FIBO)  
5. Workflows \- Design and Implementation \- Invoke, accessed December 15, 2025, [https://invoke-ai.github.io/InvokeAI/contributing/frontend/workflows/](https://invoke-ai.github.io/InvokeAI/contributing/frontend/workflows/)  
6. Workflow Editor Basics \- Invoke, accessed December 15, 2025, [https://invoke-ai.github.io/InvokeAI/nodes/NODES/](https://invoke-ai.github.io/InvokeAI/nodes/NODES/)  
7. Detailed Requirements \- Invoke, accessed December 15, 2025, [https://invoke-ai.github.io/InvokeAI/installation/requirements/](https://invoke-ai.github.io/InvokeAI/installation/requirements/)  
8. Supported Models \- Invoke Support Portal, accessed December 15, 2025, [https://support.invoke.ai/support/solutions/articles/151000170961-supported-models](https://support.invoke.ai/support/solutions/articles/151000170961-supported-models)  
9. PyTorch (MPS) is faster than MLX for training and inference for ResNets and Transformers (tested on 2 tasks) \#243 \- GitHub, accessed December 15, 2025, [https://github.com/ml-explore/mlx/issues/243](https://github.com/ml-explore/mlx/issues/243)  
10. Using MLX at Hugging Face, accessed December 15, 2025, [https://huggingface.co/docs/hub/mlx](https://huggingface.co/docs/hub/mlx)  
11. MLX vs MPS vs CUDA: a Benchmark | Towards Data Science, accessed December 15, 2025, [https://towardsdatascience.com/mlx-vs-mps-vs-cuda-a-benchmark-c5737ca6efc9/](https://towardsdatascience.com/mlx-vs-mps-vs-cuda-a-benchmark-c5737ca6efc9/)  
12. argmaxinc/DiffusionKit: On-device Image Generation for Apple Silicon \- GitHub, accessed December 15, 2025, [https://github.com/argmaxinc/DiffusionKit](https://github.com/argmaxinc/DiffusionKit)  
13. Models compatible with the GGUF library \- Hugging Face, accessed December 15, 2025, [https://huggingface.co/models?library=gguf\&sort=downloads](https://huggingface.co/models?library=gguf&sort=downloads)  
14. Logos, brand, hedgehogs \- Handbook \- PostHog, accessed December 15, 2025, [https://posthog.com/handbook/company/brand-assets](https://posthog.com/handbook/company/brand-assets)  
15. Art and branding request \- Handbook \- PostHog, accessed December 15, 2025, [https://posthog.com/handbook/brand/art-requests](https://posthog.com/handbook/brand/art-requests)  
16. Stylized Setting (Isometric) SDXL & SD1.5 \- SDXL | Stable Diffusion XL LoRA \- Civitai, accessed December 15, 2025, [https://civitai.com/models/118775/stylized-setting-isometric-sdxl-and-sd15](https://civitai.com/models/118775/stylized-setting-isometric-sdxl-and-sd15)  
17. Authentic Pixel Art with SDXL \- Civitai, accessed December 15, 2025, [https://civitai.com/articles/19902/authentic-pixel-art-with-sdxl](https://civitai.com/articles/19902/authentic-pixel-art-with-sdxl)  
18. Pixel Art XL \- v1.1 | Stable Diffusion XL LoRA \- Civitai, accessed December 15, 2025, [https://civitai.com/models/120096/pixel-art-xl](https://civitai.com/models/120096/pixel-art-xl)  
19. ComfyUI Depth ControlNet Usage Example, accessed December 15, 2025, [https://docs.comfy.org/tutorials/controlnet/depth-controlnet](https://docs.comfy.org/tutorials/controlnet/depth-controlnet)  
20. Control Layers \- Invoke Support Portal, accessed December 15, 2025, [https://support.invoke.ai/support/solutions/articles/151000105880-control-layers](https://support.invoke.ai/support/solutions/articles/151000105880-control-layers)  
21. Model Context Protocol (MCP) | Cursor Docs, accessed December 15, 2025, [https://cursor.com/docs/context/mcp](https://cursor.com/docs/context/mcp)  
22. Installing and running InvokeAI on macOS | by Russ Mckendrick \- Medium, accessed December 15, 2025, [https://russmckendrick.medium.com/installing-and-running-invokeai-on-macos-8b26e09d0b75](https://russmckendrick.medium.com/installing-and-running-invokeai-on-macos-8b26e09d0b75)  
23. Configuring Claude Code MCP Tools for Better Integration \- Clockwise, accessed December 15, 2025, [https://www.getclockwise.com/blog/claude-code-mcp-tools-integration](https://www.getclockwise.com/blog/claude-code-mcp-tools-integration)  
24. How to apply ControlNet only within an inpaint mask? : r/comfyui \- Reddit, accessed December 15, 2025, [https://www.reddit.com/r/comfyui/comments/1pkveyq/how\_to\_apply\_controlnet\_only\_within\_an\_inpaint/](https://www.reddit.com/r/comfyui/comments/1pkveyq/how_to_apply_controlnet_only_within_an_inpaint/)  
25. Is it possible to pass a React Component to puppeteer? \- Stack Overflow, accessed December 15, 2025, [https://stackoverflow.com/questions/48034767/is-it-possible-to-pass-a-react-component-to-puppeteer](https://stackoverflow.com/questions/48034767/is-it-possible-to-pass-a-react-component-to-puppeteer)