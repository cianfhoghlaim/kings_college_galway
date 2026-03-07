# **Architecting the Sovereign AI Stack: A Comprehensive Analysis of Integrating Llama.cpp, MLX-VLM, Docling, Llama-Swap, and LiteLLM**

## **1\. Introduction: The Imperative of Composable Local AI Infrastructure**

The trajectory of artificial intelligence deployment is undergoing a fundamental bifurcation. While centralized, proprietary model providers continue to scale parameter counts into the trillions, a parallel and equally potent revolution is occurring at the edge. The capability to run high-fidelity Large Language Models (LLMs) and Vision-Language Models (VLMs) on consumer-grade hardware—specifically high-end workstations and Apple Silicon architecture—has transitioned from a novelty to a viable enterprise strategy. This shift is driven by three primary imperatives: data sovereignty, latency determinism, and cost predictability. However, achieving parity with cloud-based services requires more than simply executing a model inference binary. It demands a sophisticated orchestration layer capable of managing heterogeneous compute resources, normalizing diverse API schemas, and dynamically allocating memory constraints.  
This report presents a deep architectural analysis of a specific, high-performance local AI stack composed of **llama.cpp**, **MLX-VLM**, **Docling**, **Llama-Swap**, and **LiteLLM**. Unlike monolithic platforms that attempt to bundle these capabilities into opaque applications, this stack represents the "Unix philosophy" of AI deployment: small, sharp tools, loosely coupled, each optimized for a specific domain of the inference pipeline. We will explore how llama.cpp serves as the universal inference backend for quantized models, how MLX-VLM unlocks the specific matrix multiplication accelerators of Apple Silicon for vision tasks, how Docling provides the critical bridge between unstructured documents and structured context, and how Llama-Swap and LiteLLM provide the orchestration and gateway layers necessary to present this complex machinery as a unified, OpenAI-compatible API surface.  
The analysis that follows is technical and exhaustive. It presumes a professional familiarity with systems engineering, tensor operations, and containerized deployment. We will dissect configuration strategies, examine the nuances of quantization formats like GGUF, and investigate the interoperability challenges inherent in stitching together disparate open-source projects into a cohesive, production-grade system.

## **2\. The Universal Inference Backend: Llama.cpp and the GGUF Standard**

At the foundation of this local stack lies the inference engine, the component responsible for loading model weights into volatile memory and executing the forward pass operations that generate tokens. llama.cpp has established itself as the de facto standard for this layer, principally due to its rigorous focus on maximizing throughput on commodity hardware through aggressive optimization and the introduction of the GGUF file format.

### **2.1 The GGUF Architecture and Quantization Dynamics**

The efficacy of llama.cpp is inextricably linked to the GGUF (GPT-Generated Unified Format) standard. Unlike earlier formats that often separated model architecture definitions from weight tensors, GGUF encapsulates the entire model definition—including tokenizer vocabularies, architectural hyperparameters (e.g., RoPE scaling factors, head counts), and quantization tables—into a single, memory-mappable binary. This design choice is critical for the performance characteristics required in a local stack. By supporting mmap, llama.cpp allows the operating system to load model pages on demand, significantly reducing startup latency compared to loading raw PyTorch checkpoints.1  
The engine's primary innovation, however, is its handling of mixed-precision inference. Through its k-quants quantization methodology, llama.cpp allows models to be compressed with minimal perplexity degradation. For the system architect, choosing the correct quantization level is a trade-off between VRAM occupancy and reasoning capability.  
**Table 1: Quantization Trade-offs for Deployment**

| Quantization Type | Bit Depth | Use Case | Memory Impact (70B Model) | Perplexity Impact |
| :---- | :---- | :---- | :---- | :---- |
| **F16 / BF16** | 16-bit | Research benchmarks; exact reproducibility. | \~140 GB | Baseline |
| **Q8\_0** | 8-bit | High-precision production; virtually lossless. | \~75 GB | Negligible |
| **Q6\_K** | 6-bit | Balanced high-performance; "sweet spot" for reasoning. | \~55 GB | \< 0.1% increase |
| **Q4\_K\_M** | 4-bit | Standard efficient deployment; fits on dual 24GB GPUs. | \~42 GB | \~1-2% increase |
| **IQ4\_XS** | 4-bit (I-Quant) | Memory constrained setups; utilizes importance matrix. | \~39 GB | Variable |
| **IQ2\_XXS** | 2-bit | Extreme edge cases; significant reasoning loss. | \~22 GB | Significant |

The introduction of "Importance Matrix" (I-Quant) quantizations further refines this by analyzing the activation magnitudes during a calibration pass, allocating more bits to weights that significantly impact the output and fewer to those that do not. For VLMs like Qwen3-VL or Gemma 3, using I-Quants can be particularly effective in fitting large context windows into limited VRAM, although it requires careful calibration to avoid degrading the visual encoder's alignment with the text embedding space.3

### **2.2 Server Architecture and Multimodal Pipelines**

While llama-cli offers a direct interface, the llama-server binary is the component that enables integration into a larger stack. It wraps the inference engine in a lightweight HTTP server that mimics the OpenAI API specifications. However, serving Vision-Language Models (VLMs) introduces significant complexity regarding the "Multimodal Projector" (mmproj).  
In pure text models, the input is a sequence of token IDs. In VLMs, the input includes image patches that must be encoded by a Vision Transformer (ViT) and then projected into the LLM's embedding space. llama.cpp decouples these components for flexibility. The main model GGUF contains the LLM weights, while a separate GGUF (the mmproj file) contains the vision encoder and projector weights.  
Configuration for Qwen3-VL-32B-Instruct:  
To serve a model like Qwen3-VL, the server invocation must explicitly link these components. The architecture of Qwen3-VL is notably sensitive to the resolution and aspect ratio of input images, which the projector handles via dynamic resolution scaling (M-ROPE).

Bash

\# High-Performance Server Invocation for Qwen3-VL  
llama-server \\  
  \-m /models/Qwen3-VL-32B-Instruct-Q4\_K\_M.gguf \\  \# The quantized LLM backend  
  \--mmproj /models/mmproj-Qwen3-VL-32B-Instruct-f16.gguf \\  \# The high-precision vision projector  
  \--port 8080 \\  
  \-ngl 99 \\          \# Offload all layers to GPU (Crucial for speed)  
  \-c 32768 \\         \# Context window (Visual tokens consume massive context)  
  \-fa \\              \# Enable Flash Attention (Reduces VRAM usage for self-attention)  
  \--ubatch-size 512  \# Physical batch size for prompt processing

It is imperative to maintain the mmproj file at a higher precision (typically F16) than the LLM. Quantizing the vision encoder often leads to catastrophic misalignment, where the model can "see" the image structure but fails to recognize fine details like text (OCR) or small objects, rendering the VLM useless for document parsing tasks.4  
The Gemma 3 Challenge:  
Google's Gemma 3 architecture introduces further nuance. Unlike previous iterations, Gemma 3 utilizes a Sliding Window Attention (SWA) mechanism to manage massive context windows (up to 128k tokens) efficiently. Support for Gemma 3 in llama.cpp was not immediate and required significant refactoring of the attention kernels. When deploying Gemma 3, specifically the multimodal variants, one must ensure the build version of llama.cpp supports the specific tensor operations used in its vision tower (SigLIP variants). Furthermore, the argument handling for mmproj differs slightly, often requiring specific flags to enable the sliding window cache to prevent OOM errors on consumer hardware.5 The "hybrid" quantization approaches (e.g., Q4\_K\_H) are often recommended for Gemma 3 27B to balance the high dynamic range of its layer activations against memory constraints.3

### **2.3 Hardware Acceleration Profiles**

llama.cpp is agnostic to the underlying hardware but requires specific compilation flags to unlock performance.

* **CUDA (NVIDIA):** Compiling with LLAMA\_CUDA=1 enables the cuBLAS backend. The \-ngl (n-gpu-layers) flag is the primary lever for performance. Setting this to 99 (or a number exceeding the total layers) ensures the entire model resides in VRAM, eliminating PCI-E bus latency.  
* **Metal (Apple Silicon):** Compiling with LLAMA\_METAL=1 targets Apple's Metal Performance Shaders (MPS). This allows the model to utilize the GPU cores on M-series chips. However, for certain batch processing tasks, the CPU backend on Apple Silicon (using the AMX matrix coprocessors) can sometimes outperform the GPU for prompt processing (prefill), leading to hybrid strategies where prompt ingestion happens on CPU and token generation on GPU.7  
* **Vulkan/ROCm:** For AMD and Intel GPUs, llama.cpp offers Vulkan and HIP implementations. While functional, these backends often lag behind CUDA in optimization for specific operators used in newer models like Qwen3 or Gemma 3\.9

## **3\. Specialized Vision Intelligence: MLX-VLM and the Apple Advantage**

While llama.cpp strives for universality, the **MLX** framework represents specialization. Developed by Apple's machine learning research team, MLX is an array framework designed specifically for the Unified Memory Architecture (UMA) of Apple Silicon. For the local AI stack running on Mac hardware, segregating vision workloads to MLX-VLM often yields superior performance compared to forcing them through llama.cpp.

### **3.1 The Architecture of MLX and UMA**

The defining characteristic of MLX is its unified memory model. In traditional CUDA architectures, data must be explicitly copied between Host RAM and Device VRAM over the PCI-E bus—a significant bottleneck. On Apple Silicon, the CPU and GPU share the same memory address space. MLX exploits this by allowing arrays to live in shared memory without implicit copying. This is particularly advantageous for VLMs, where high-resolution image embeddings can be large.  
MLX also employs "lazy computation," where operations are only executed when the results are materialized. This allows for dynamic graph construction, making it highly adaptable to the variable-length sequences typical in document processing and chat interfaces.10

### **3.2 Granite Docling: A Native MLX Implementation**

**Granite Docling** represents a departure from general-purpose VLMs. It is a model engineered specifically for document conversion—transforming PDFs, scans, and slides into structured Markdown or JSON. Unlike a chat model that might hallucinate layout details, Granite Docling is trained to output **DocTags**, a specialized markup language that explicitly defines document topology (headers, tables, lists).  
Running Granite Docling via docling with the MLX backend leverages specific optimizations for the SigLIP vision encoder and the Granite language decoder. The integration is not merely about loading weights; it involves a specialized pipeline configuration.

Python

\# Configuring the Docling Pipeline for MLX Acceleration  
from docling.datamodel import vlm\_model\_specs  
from docling.datamodel.pipeline\_options import VlmPipelineOptions  
from docling.document\_converter import DocumentConverter, PdfFormatOption  
from docling.pipeline.vlm\_pipeline import VlmPipeline

\# Explicitly selecting the MLX backend spec for Granite  
pipeline\_options \= VlmPipelineOptions(  
    vlm\_options=vlm\_model\_specs.GRANITEDOCLING\_MLX  
)

\# Injecting the pipeline into the converter  
converter \= DocumentConverter(  
    format\_options={  
        InputFormat.PDF: PdfFormatOption(  
            pipeline\_cls=VlmPipeline,  
            pipeline\_options=pipeline\_options  
        )  
    }  
)

This configuration ensures that the heavy lifting of image encoding—splitting high-DPI document pages into patches and projecting them—is handled by the Neural Engine and GPU via MLX primitives, rather than generic PyTorch fallbacks.8

### **3.3 OlmOCR and the Case for Specialization**

**OlmOCR-2-7B** is another specialized VLM, fine-tuned on the Qwen2.5-VL architecture. Its primary differentiator is its training on a massive dataset of PDF pages paired with "verifiable unit tests." This means the model is penalized during training not just for text inaccuracy, but for failing to capture structural elements like reading order in multi-column layouts or correct table formatting.12  
Deploying OlmOCR within this stack presents a choice. While it can run via llama.cpp (as it is based on Qwen2.5), running the quantized MLX version (olmOCR-2-7B-1025-MLX-4bit) on a Mac allows for a dedicated "OCR Microservice." By isolating this heavy vision workload to an MLX process, the main llama-server (running on CUDA or Metal) is kept free for reasoning tasks.  
Serving MLX Models via API:  
To integrate these MLX-native models into the broader stack (which expects OpenAI-compatible APIs), we utilize mlx-openai-server. This lightweight Python server wraps the MLX model generation functions.

Bash

\# Launching an OpenAI-compatible server for OlmOCR via MLX  
python \-m mlx\_openai\_server \\  
  \--model-path mlx-community/olmOCR-2-7B-1025-MLX-4bit \\  
  \--model-type vlm \\  
  \--chat-template-file templates/olmocr\_instruct.jinja \\  
  \--port 8082

This effectively turns the specialized MLX model into a standardized endpoint that can be routed to by LiteLLM or Llama-Swap.10

## **4\. Intelligent Parsing and Data Ingestion: Docling**

In the context of a "Deep Research" AI stack, the ability to ingest information is as critical as the ability to reason about it. **Docling** serves as this ingestion layer. It is not a model itself, but a library that orchestrates models (like Granite Docling) to perform "Advanced PDF Understanding."

### **4.1 The DoclingDocument Object Model**

The output of a Docling conversion is not a simple string; it is a DoclingDocument object. This hierarchical representation captures the semantic structure of the file. It distinguishes between a "Section Header," a "Paragraph," a "Table Cell," and a "Figure Caption."  
This structural awareness is vital for RAG pipelines. When chunking a document for vector storage, naive splitting often breaks tables or separates headers from their content. Docling allows for "semantic chunking," respecting the boundaries defined by the document structure.14

### **4.2 The Docling-Serve API**

While Docling is primarily a Python library, the docling-serve project wraps this functionality into a REST API. This is essential for our decoupled stack. Instead of embedding the heavy Docling dependencies into every application, we run docling-serve as a standalone service.  
Gap Analysis: Tool Calling Integration  
A critical "missing link" in many deployments is connecting the chat-based LLM to this document processing service. docling-serve exposes endpoints like /convert, but LLMs speak in "Tool Calls."  
To bridge this, we must define a tool definition that the LLM understands. This definition describes the parse\_document function, which, when invoked by the LLM, triggers a piece of middleware code (likely in Python) that sends the actual HTTP request to docling-serve.  
**Table 2: Comparing Parsing Approaches in Local Stacks**

| Feature | Traditional OCR (Tesseract) | General VLM (Qwen/Gemma) | Specialized Pipeline (Docling) |
| :---- | :---- | :---- | :---- |
| **Input** | Image / PDF | Image / PDF | Image / PDF / DOCX / HTML |
| **Layout Analysis** | Heuristic / Rule-based | Implicit (learned) | Explicit (Model \+ Rules) |
| **Output Format** | Unstructured Text | Markdown / Text | Structured Object / DocTags |
| **Table Fidelity** | Poor | Variable (Hallucination risk) | High (Preserves topology) |
| **Latency** | Low | High | Medium |
| **Integration** | Library call | Chat Prompt | API / Tool Call |

By running docling-serve, we effectively create a "Document Processing Unit" (DPU) for our local cloud.16

## **5\. Dynamic Orchestration: Llama-Swap**

As we accumulate these specialized models—a general reasoner (Llama-3), a coder (Qwen-2.5-Coder), a vision expert (Gemma-3), and an OCR engine (OlmOCR)—we hit a hard physical limit: VRAM. A consumer workstation with 24GB or even 48GB of VRAM cannot hold all these models simultaneously. **Llama-Swap** provides the solution through dynamic orchestration.

### **5.1 The Proxy Architecture**

Llama-Swap functions as a Layer 7 transparent proxy. It binds to a specific port (e.g., 8081\) and listens for incoming HTTP requests compatible with the OpenAI API. When a request arrives, it inspects the JSON body for the model field.  
The logic follows a state-machine pattern:

1. **Intercept**: Request for model qwen-coder-32b received.  
2. **Check State**: Is qwen-coder-32b currently loaded?  
3. **Resource Management**: If another model (e.g., gemma-vision) is loaded and utilizing the GPU, Llama-Swap issues a SIGTERM to that process to free resources.  
4. **Provision**: It looks up the cmd definition for qwen-coder-32b in its config.yaml.  
5. **Launch**: It executes the shell command to start llama-server for the requested model, injecting a dynamically assigned port via the ${PORT} macro.  
6. **Health Check**: It polls the new server's health endpoint until it returns 200 OK.  
7. **Proxy**: It forwards the original request to the now-running backend.

### **5.2 Advanced Configuration Strategies**

The config.yaml for Llama-Swap is the control plane for the hardware. It requires precise definition to ensure stability.  
Port Management:  
Llama-Swap manages a pool of ports. The startPort directive defines the beginning of this range. When a model is spun up, it is assigned startPort \+ n. This allows multiple small models to theoretically run in parallel if groups are configured, though in a VRAM-constrained single-GPU setup, serial execution is the norm.18  
Argument Escaping:  
A common pitfall identified in the research is complex argument passing. When the cmd block in YAML involves JSON strings (e.g., for \--chat-template strings) or complex paths, standard YAML parsing can corrupt the command. Using the block scalar indicator | is mandatory to preserve newlines and spacing. Furthermore, if arguments contain colons or braces, they must be carefully quoted to prevent the shell from misinterpreting them.  
Timeout Tuning:  
The healthCheckTimeout is a critical parameter. Loading a 70B parameter model from a mechanical hard drive (HDD) or even a slow SATA SSD can take upwards of 30-60 seconds. If the timeout is set too low (default is often 120s, which is usually safe but can be tight for massive models), the proxy will return a 504 Gateway Timeout before the model is ready. For local stacks using NVMe drives, this is less of an issue, but for older hardware, increasing this value is essential.18  
**Example Heterogeneous Configuration:**

YAML

\# Llama-Swap Configuration for Mixed Backend  
healthCheckTimeout: 300  
startPort: 10000

models:  
  \# Llama.cpp Backend  
  qwen-coder:  
    cmd: |  
      /opt/llama.cpp/llama-server \\  
        \-m /models/Qwen2.5-Coder-32B-Instruct-Q4\_K\_M.gguf \\  
        \--port ${PORT} \\  
        \-ngl 99 \-c 16384 \--ctx-shift

  \# MLX Backend (via mlx-openai-server wrapper)  
  \# Note: Llama-swap can launch ANY executable that respects the port  
  olm-ocr:  
    cmd: |  
      python \-m mlx\_openai\_server \\  
        \--model-path mlx-community/olmOCR-2-7B-1025-MLX-4bit \\  
        \--port ${PORT} \\  
        \--model-type vlm

  \# Specialized Gemma 3 Setup  
  gemma-3:  
    cmd: |  
      /opt/llama.cpp/llama-server \\  
        \-m /models/gemma-3-27b-it-Q4\_K\_M.gguf \\  
        \--mmproj /models/gemma-3-mmproj-f16.gguf \\  
        \--port ${PORT} \\  
        \-ngl 99 \-c 8192

This configuration demonstrates Llama-Swap's ability to act as a unifying control plane for entirely different inference engines (llama.cpp binary vs Python mlx script).

## **6\. The API Gateway: LiteLLM**

If Llama-Swap provides the "hardware virtualization" (swapping models in and out), **LiteLLM** provides the "application virtualization." It decouples the client applications (chatbots, IDE plugins, agent frameworks) from the backend implementation details.

### **6.1 The Unified Interface Principle**

LiteLLM sits at the apex of the stack. It exposes a stable, immutable API endpoint (e.g., http://localhost:4000). Clients connect to this endpoint using the standard OpenAI SDK. LiteLLM then routes these requests based on its own internal routing table (config.yaml) to the appropriate backend—whether that is Llama-Swap, a direct docling-serve instance, or even a cloud fallback.  
This normalization is crucial because different backends often exhibit subtle API deviations. For instance, some local servers might return token usage stats in a slightly different JSON path, or handle stop sequences differently. LiteLLM smooths over these inconsistencies, ensuring that a client expecting standard OpenAI behavior never crashes due to backend quirks.19

### **6.2 Resilience: Fallbacks and Retries**

A key feature for local deployments is reliability. Local models can crash, run out of memory (OOM), or simply hang. LiteLLM enables a "Fallback" architecture.

YAML

\# LiteLLM Configuration with Fallbacks  
model\_list:  
  \- model\_name: coding-assistant  
    litellm\_params:  
      model: openai/qwen-coder  \# Points to Llama-Swap  
      api\_base: "http://localhost:8081/v1"  
      api\_key: "sk-local"  
      rpm: 100 \# Rate limit protection  
    fallback: "gpt-4o-mini" \# Cloud fallback

  \- model\_name: gpt-4o-mini  
    litellm\_params:  
      model: openai/gpt-4o-mini  
      api\_key: os.environ/OPENAI\_API\_KEY

In this setup, if llama-swap fails to load the Qwen model (perhaps due to a CUDA error), LiteLLM automatically reroutes the request to OpenAI's GPT-4o-mini. This ensures high availability for the user, even on unstable local hardware.20

### **6.3 Cost Tracking and Observability**

One often overlooked aspect of local AI is "cost" tracking—not in dollars, but in throughput and resource usage. LiteLLM provides built-in logging that tracks input/output tokens per request. By assigning a synthetic "cost" to local models (e.g., $0.00), or a real cost based on electricity/hardware amortization, organizations can visualize usage patterns. This data is invaluable for capacity planning—identifying which models are most heavily used and justifying hardware upgrades.21

### **6.4 Bridging the Gap: Tool Calling for Docling**

The integration of docling into this stack highlights a specific challenge: docling-serve is a document conversion API, not a chat API. LiteLLM does not "chat" with Docling. Instead, we configure Docling as a **Tool**.  
The primary LLM (e.g., Qwen-Coder via Llama-Swap) is provided with a function definition for parse\_document. When the user uploads a PDF, the LLM recognizes the need to parse it and issues a tool call. The client application (or an agentic framework like LangChain/AutoGen connected to LiteLLM) intercepts this call, executes the HTTP POST to docling-serve, gets the Markdown result, and appends it to the context.  
While LiteLLM can technically route *any* request, using it to proxy docling-serve directly as a chat model would be semantically incorrect. The correct architecture treats Docling as a *utility service* accessible via the LLM's tool-use capabilities.

## **7\. Synthesis: The Complete Local AI Workflow**

The true power of this stack emerges when the components interact. Consider a workflow where a user asks a local agent to "Analyze the financial table in this PDF invoice and write a Python script to visualize it."

1. **Ingestion:** The user's request and PDF path are sent to **LiteLLM**.  
2. **Routing:** LiteLLM forwards the request to the configured default model, e.g., qwen-coder.  
3. **Orchestration:** **Llama-Swap** receives the request. It sees qwen-coder is needed. It checks VRAM. If gemma-3 was running, it is terminated. llama-server is launched with qwen-coder GGUF.  
4. **Reasoning (Step 1):** Qwen-Coder loads. It analyzes the prompt: "Analyze PDF". It sees a registered tool parse\_document. It returns a structured tool call response: {"function": "parse\_document", "arguments": {"path": "invoice.pdf"}}.  
5. **Execution:** The client application executes this function. It sends the PDF to **Docling** (running via docling-serve).  
6. **Parsing:** Docling (potentially using **Granite Docling** via **MLX** for speed) extracts the table structure into high-fidelity Markdown.  
7. **Reasoning (Step 2):** The Markdown is fed back to Qwen-Coder (still loaded in Llama-Swap).  
8. **Generation:** Qwen-Coder writes the Python visualization script based on the structured data.

This entire loop occurs locally, with specialized models handling the vision/parsing and generalist models handling the reasoning/coding, all orchestrated dynamically to fit within consumer hardware constraints.

## **8\. Conclusion and Strategic Recommendations**

The combination of llama.cpp, MLX-VLM, Docling, Llama-Swap, and LiteLLM constitutes a "sovereign cloud" architecture. It provides the flexibility of microservices (swapping backends, specialized pipelines) with the privacy and cost benefits of local execution.  
**Strategic Recommendations for Implementation:**

1. **Hardware Segmentation:** On Apple Silicon, lean heavily into MLX for vision/OCR tasks (olmOCR, Granite Docling) to free up the llama.cpp backend for pure token generation. The UMA allows these to coexist more gracefully than on PC architectures.  
2. **Config Management:** Treat config.yaml files (for both Llama-Swap and LiteLLM) as infrastructure-as-code. Version control them. They define the behavior of your AI system.  
3. **Specialization over Generalization:** Do not rely on a single "do-it-all" VLM. A pipeline that uses Docling for ingestion and a text-optimized LLM for reasoning will consistently outperform a generic VLM trying to read dense text directly from image tokens.  
4. **Reliability Layering:** Use LiteLLM's fallback mechanisms. Even in a fully local stack, having a fallback to a smaller, faster model (e.g., Phi-3 or Gemma-2-2B) ensures the system remains responsive under load.

By adhering to this modular architecture, organizations can build AI systems that are not only powerful and private but also resilient and adaptable to the rapidly evolving landscape of open-source models.

## **9\. Technical Addenda: Configuration Reference**

### **9.1 Llama-Swap config.yaml (Heterogeneous Backend)**

YAML

healthCheckTimeout: 120  
startPort: 8081  
models:  
  \# Primary Coder \- Llama.cpp Backend  
  qwen-coder:  
    cmd: |  
      /usr/local/bin/llama-server \\  
        \-m /models/Qwen2.5-Coder-32B-Instruct-Q4\_K\_M.gguf \\  
        \--port ${PORT} \\  
        \-ngl 99 \\  
        \-c 16384 \\  
        \--ctx-shift

  \# Vision Specialist \- Llama.cpp Backend with Multi-modal Projector  
  gemma-vision:  
    cmd: |  
      /usr/local/bin/llama-server \\  
        \-m /models/gemma-3-27b-it-Q4\_K\_M.gguf \\  
        \--mmproj /models/gemma-3-mmproj-f16.gguf \\  
        \--port ${PORT} \\  
        \-ngl 99 \\  
        \-c 8192

  \# OCR Specialist \- MLX Backend (via wrapper script)  
  olm-ocr:  
    cmd: |  
      /scripts/launch\_mlx\_server.sh \\  
        \--model mlx-community/olmOCR-2-7B-1025-MLX-4bit \\  
        \--port ${PORT}

### **9.2 LiteLLM config.yaml (Gateway with Tools)**

YAML

model\_list:  
  \- model\_name: local-coder  
    litellm\_params:  
      model: openai/qwen-coder \# Routes to Llama-Swap  
      api\_base: "http://localhost:8081/v1"  
      api\_key: "sk-local"  
      timeout: 300 \# Allow time for Llama-Swap model loading

  \- model\_name: local-vision  
    litellm\_params:  
      model: openai/gemma-vision  
      api\_base: "http://localhost:8081/v1"  
      api\_key: "sk-local"

  \- model\_name: local-ocr  
    litellm\_params:  
      model: openai/olm-ocr  
      api\_base: "http://localhost:8081/v1"  
      api\_key: "sk-local"

#### **Works cited**

1. Switching from Ollama to llama-swap \+ llama.cpp on NixOS: the power user's choice | Bas Nijholt, accessed December 7, 2025, [https://www.nijho.lt/post/llama-nixos/](https://www.nijho.lt/post/llama-nixos/)  
2. ggml-org/llama.cpp: LLM inference in C/C++ \- GitHub, accessed December 7, 2025, [https://github.com/ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp)  
3. steampunque/gemma-3-27b-it-Hybrid-GGUF \- Hugging Face, accessed December 7, 2025, [https://huggingface.co/steampunque/gemma-3-27b-it-Hybrid-GGUF](https://huggingface.co/steampunque/gemma-3-27b-it-Hybrid-GGUF)  
4. Qwen/Qwen3-VL-32B-Instruct-GGUF \- Hugging Face, accessed December 7, 2025, [https://huggingface.co/Qwen/Qwen3-VL-32B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen3-VL-32B-Instruct-GGUF)  
5. Llama.cpp vs API \- Gemma 3 Context Window Performance : r/LocalLLaMA \- Reddit, accessed December 7, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1ljve2u/llamacpp\_vs\_api\_gemma\_3\_context\_window\_performance/](https://www.reddit.com/r/LocalLLaMA/comments/1ljve2u/llamacpp_vs_api_gemma_3_context_window_performance/)  
6. Sliding Window Attention support merged into llama.cpp, dramatically reducing the memory requirements for running Gemma 3 : r/LocalLLaMA \- Reddit, accessed December 7, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1kqye2t/sliding\_window\_attention\_support\_merged\_into/](https://www.reddit.com/r/LocalLLaMA/comments/1kqye2t/sliding_window_attention_support_merged_into/)  
7. How to build your own local AI stack on Linux with llama.cpp, llama-swap, LibreChat and more | by Imad Saddik \- Medium, accessed December 7, 2025, [https://medium.com/@imadsaddik/building-my-own-local-ai-stack-on-linux-with-llama-cpp-llama-swap-librechat-and-more-50ea464a2bf9](https://medium.com/@imadsaddik/building-my-own-local-ai-stack-on-linux-with-llama-cpp-llama-swap-librechat-and-more-50ea464a2bf9)  
8. VLM pipeline with GraniteDocling \- Docling \- GitHub Pages, accessed December 7, 2025, [https://docling-project.github.io/docling/examples/minimal\_vlm\_pipeline/](https://docling-project.github.io/docling/examples/minimal_vlm_pipeline/)  
9. How I Run Gemma 3 27B on an RX 7800 XT 16GB Locally\! : r/LocalLLaMA \- Reddit, accessed December 7, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1kjwi3w/how\_i\_run\_gemma\_3\_27b\_on\_an\_rx\_7800\_xt\_16gb/](https://www.reddit.com/r/LocalLLaMA/comments/1kjwi3w/how_i_run_gemma_3_27b_on_an_rx_7800_xt_16gb/)  
10. cubist38/mlx-openai-server: A high-performance API server that provides OpenAI-compatible endpoints for MLX models. Developed using Python and powered by the FastAPI framework, it provides an efficient, scalable, and user-friendly solution for running MLX-based vision and language models locally with an OpenAI-compatible \- GitHub, accessed December 7, 2025, [https://github.com/cubist38/mlx-openai-server](https://github.com/cubist38/mlx-openai-server)  
11. Vision models \- Docling \- GitHub Pages, accessed December 7, 2025, [https://docling-project.github.io/docling/usage/vision\_models/](https://docling-project.github.io/docling/usage/vision_models/)  
12. olmOCR 2 Unit Test Rewards for Document OCR \- arXiv, accessed December 7, 2025, [https://arxiv.org/html/2510.19817v1](https://arxiv.org/html/2510.19817v1)  
13. richardyoung/olmOCR-2-7B-1025-MLX-6bit \- Hugging Face, accessed December 7, 2025, [https://huggingface.co/richardyoung/olmOCR-2-7B-1025-MLX-6bit](https://huggingface.co/richardyoung/olmOCR-2-7B-1025-MLX-6bit)  
14. docling-sdk \- NPM, accessed December 7, 2025, [https://www.npmjs.com/package/docling-sdk?activeTab=readme](https://www.npmjs.com/package/docling-sdk?activeTab=readme)  
15. Introduction to Docling | Niklas Heidloff, accessed December 7, 2025, [https://heidloff.net/article/docling/](https://heidloff.net/article/docling/)  
16. docling-project/docling-serve: Running Docling as an API ... \- GitHub, accessed December 7, 2025, [https://github.com/docling-project/docling-serve](https://github.com/docling-project/docling-serve)  
17. Enhance Multi-Modal QA for Uploaded Documents with Docling File Parser and OpenAI-Compatible API \#14677 \- GitHub, accessed December 7, 2025, [https://github.com/open-webui/open-webui/discussions/14677](https://github.com/open-webui/open-webui/discussions/14677)  
18. config.example.yaml \- mostlygeek/llama-swap \- GitHub, accessed December 7, 2025, [https://github.com/mostlygeek/llama-swap/blob/main/config.example.yaml](https://github.com/mostlygeek/llama-swap/blob/main/config.example.yaml)  
19. Everything You Need to Know About LiteLLM Python SDK \- DEV Community, accessed December 7, 2025, [https://dev.to/yigit-konur/everything-you-need-to-know-about-litellm-python-sdk-3kfk](https://dev.to/yigit-konur/everything-you-need-to-know-about-litellm-python-sdk-3kfk)  
20. LiteLLM: A Guide With Practical Examples \- DataCamp, accessed December 7, 2025, [https://www.datacamp.com/tutorial/litellm](https://www.datacamp.com/tutorial/litellm)  
21. Azure Document Intelligence OCR \- LiteLLM, accessed December 7, 2025, [https://docs.litellm.ai/docs/providers/azure\_document\_intelligence](https://docs.litellm.ai/docs/providers/azure_document_intelligence)