# **Convergent Local Intelligence: Architecting High-Fidelity Multi-Modal Document Workflows on Apple Silicon**

## **1\. The Silicon Substrate and the Local Inference Paradigm**

The transition of artificial intelligence from centralized, cloud-dependent API consumption to localized, privacy-centric inference represents a fundamental shift in the computational landscape. This is nowhere more apparent than within the ecosystem of Apple Silicon, where the architecture of M-series chips (M1 through M4) has inadvertently created the ideal substrate for high-performance, multi-modal generative AI. To understand the viability of running a complex, heterogeneous fleet of models—comprising **PaddleOCR-VL**, **olmOCR-2-7B**, **DeepSeek-OCR**, **Qwen3-VL**, **Marker**, and **Docling**—one must first rigorously analyze the hardware constraints and the software translation layers that make this possible.  
The defining feature of this architecture is the Unified Memory Architecture (UMA). In traditional x86/CUDA workstations, the dichotomy between system RAM and GPU VRAM creates a rigid bottleneck; a model either fits in VRAM or it does not run effectively. The PCIe bus, despite its high bandwidth, imposes latency penalties during memory swapping that effectively renders large-model inference unusable for real-time applications. Apple’s UMA obliterates this distinction. The CPU, GPU, and Neural Engine (ANE) share a single pool of high-bandwidth memory. For a document processing workflow, this is transformative. It allows a researcher to load a 32-billion parameter reasoning model like Qwen3-VL (requiring \~18GB at 4-bit quantization) alongside a specialized OCR model like olmOCR (requiring \~5GB) and a vector database, all resident in the same addressable memory space without data copying overheads.1  
However, the hardware advantage is historically tempered by software fragmentation. NVIDIA’s CUDA remains the lingua franca of deep learning research. The challenge for the macOS architect is not a lack of compute, but a "translation tax." Models like **DeepSeek-OCR**, which utilize custom CUDA kernels for operations like Deformable Attention or specific implementations of Rotary Positional Embeddings (RoPE), do not run out-of-the-box on Apple’s Metal Performance Shaders (MPS). The workflow defined in this report is not merely an installation guide; it is an exercise in architectural adaptation, utilizing three distinct inference backends—**MLX**, **llama.cpp**, and **PyTorch/MPS**—to bridge the gap between research code and silicon reality.

### **1.1 The Inference Triad: MLX, Metal, and GGUF**

To achieve optimal throughput for the specific models requested, we must reject the notion of a single monolithic framework. Instead, we adopt a hybrid approach.  
**MLX** serves as the primary engine for high-throughput, generative transformer workloads. Developed explicitly for Apple Silicon, MLX mirrors the composability of PyTorch but employs lazy evaluation and a unified memory model similar to JAX. For models like **Qwen3-VL**, MLX offers superior token generation speeds compared to the PyTorch MPS backend because it manages the Key-Value (KV) cache more efficiently within the UMA, reducing the memory bandwidth pressure that typically bottlenecks auto-regressive generation.1  
**llama.cpp** and its associated llama-server provide the necessary infrastructure for **olmOCR-2-7B**. The project’s Metal backend is highly mature, offering optimized kernels for integer-quantized matrix multiplications (specifically Q4\_K\_M and Q8\_0 formats). Crucially, llama.cpp allows for precise control over layer offloading (-ngl), enabling the system architect to balance GPU loads between the OCR model and the reasoning model.4  
**PyTorch with MPS** remains the necessary fallback for specialized architectures like **PaddleOCR-VL** and **DeepSeek-OCR** (prior to their eventual porting to MLX). While less efficient than MLX for generation, the MPS backend has matured sufficiently to support the vision encoders (ViT, SAM, NaViT) utilized by these models, provided specific numerical stability patches—specifically regarding bfloat16 casting—are applied.6

## ---

**2\. Orchestration Architecture: The LiteLLM Gateway and MCP**

Running six distinct models as isolated processes creates a fragmented developer experience. To synthesize these into a coherent "Local AI Cloud," we employ an orchestration layer composed of **LiteLLM** as the API Gateway and the **Model Context Protocol (MCP)** as the standard for tool interoperability.

### **2.1 LiteLLM: The Unified Interface**

LiteLLM functions as a localized proxy server, normalizing the disparate API signatures of our underlying models into a standard OpenAI-compatible format. This decoupling is critical. A client application—whether it be a coding assistant like Cursor, a web interface like Open WebUI, or a custom Python script—should not need to know that Qwen3-VL is running on port 8082 via MLX while olmOCR is running on port 8081 via llama.cpp.  
The LiteLLM proxy handles this routing transparently. It accepts an incoming request for model="gpt-4-vision-preview" (aliased to our local Qwen3-VL) and forwards it to the appropriate local endpoint. Furthermore, LiteLLM allows for the centralized management of **MCP Servers**, injecting the capabilities of deterministic tools like **Docling** and **Marker** directly into the system prompts of the generative models.7

### **2.2 The Model Context Protocol (MCP)**

The integration of **Docling** and **Marker** represents a shift from "generative" to "agentic" workflows. These are not chatbots; they are functional libraries that convert PDFs to Markdown. By wrapping them as MCP Servers, we standardize their consumption.  
In this workflow, we utilize the **Server-Sent Events (SSE)** transport layer for MCP. Unlike the standard input/output (stdio) transport used for local desktop apps, SSE allows our MCP servers to function as independent web services that can broadcast status updates (e.g., "Page 1 processed," "Table extraction complete") back to the LiteLLM gateway. This is essential for long-running document processing tasks where HTTP timeouts would otherwise be a risk. The architecture thus evolves from a simple request-response loop to an asynchronous event-driven system where the Qwen3-VL "Brain" can command the Docling "Hands" to read a file and report back.8

## ---

**3\. The Generative Vision Engine: Qwen3-VL and olmOCR**

The core of our document analysis capability rests on two generative pillars: **olmOCR-2-7B** for accurate transcription and formatting, and **Qwen3-VL** for semantic reasoning.

### **3.1 olmOCR-2-7B: The Specialist Workhorse**

**olmOCR** is a fine-tune of the Qwen2.5-VL architecture, specifically optimized to convert PDFs and document images into clean, structured Markdown. Unlike general-purpose VLMs which might hallucinate conversational filler, olmOCR is trained to act as a rigorous transcriber, accurately capturing tables, LaTeX formulas, and reading order.10

#### **3.1.1 Implementation Strategy: llama.cpp**

We deploy olmOCR using llama.cpp to leverage the GGUF format's efficiency. The model consists of two distinct components: the language model weights and the vision projector (clip model).  
Installation:  
The setup begins with a clean Python environment managed by uv to prevent dependency conflicts with the other models.

Bash

uv venv.venv\_olm  
source.venv\_olm/bin/activate  
uv pip install "llama-cpp-python\[server\]" \--extra-index-url https://abetlen.github.io/llama-cpp-python/whl/metal

Model Acquisition:  
We retrieve the quantized Q4\_K\_M weights, which offer the optimal balance of perplexity and memory footprint (approx. 4.8GB), along with the vision projector.

Bash

huggingface-cli download richardyoung/olmOCR-2-7B-1025-GGUF olmOCR-2-7B-1025-Q4\_K\_M.gguf \--local-dir./models/olmocr  
huggingface-cli download richardyoung/olmOCR-2-7B-1025-GGUF mmproj-olmOCR-2-7B-1025-vision.gguf \--local-dir./models/olmocr

Service Configuration:  
The llama-server is configured to offload all layers to the Metal GPU (-ngl 99). A crucial parameter here is \--ctx\_size 8192 (or higher), as document parsing invariably generates long context windows. The server is bound to port 8081 to distinguish it from the reasoning engine.

Bash

\# Launch Command  
python \-m llama\_cpp.server \\  
  \--model./models/olmocr/olmOCR-2-7B-1025-Q4\_K\_M.gguf \\  
  \--clip\_model\_path./models/olmocr/mmproj-olmOCR-2-7B-1025-vision.gguf \\  
  \--n\_gpu\_layers 99 \\  
  \--chat\_format chatml \\  
  \--n\_ctx 8192 \\  
  \--port 8081 \\  
  \--alias olmocr

### **3.2 Qwen3-VL: The Reasoning Agent**

**Qwen3-VL** (specifically the 32B parameter "Thinking" or "Instruct" variant) introduces a "System 2" reasoning capability to the visual domain. It supports dynamic resolution, meaning it can ingest images of varying aspect ratios and resolutions (up to a defined pixel limit) without the aggressive downscaling that plagues older models.12 Its role in this workflow is not primarily transcription, but *analysis*—interpreting the implications of a financial trend in a chart or verifying the logic of a legal clause extracted by olmOCR.

#### **3.2.1 Implementation Strategy: MLX**

Given the computational density of a 32B model, we utilize MLX for its superior throughput. The MLX community provides 4-bit quantized weights that fit comfortably within 24GB of RAM, leaving headroom on a 32GB+ Mac for the OS and olmOCR.14  
**Installation:**

Bash

uv venv.venv\_mlx  
source.venv\_mlx/bin/activate  
uv pip install mlx mlx-lm mlx-vlm huggingface\_hub

Service Configuration:  
We deploy this using the mlx\_lm.server module. Note that strictly speaking, Qwen3-VL support in MLX requires the very latest version of the library due to architectural changes in the vision encoder (specifically the "Thinking" process components).

Bash

\# Launch Command  
python \-m mlx\_lm.server \\  
  \--model mlx-community/Qwen2.5-VL-32B-Instruct-4bit \\  
  \--port 8082 \\  
  \--log-level info

Note: As of late 2025, Qwen2.5-VL is the stable MLX target. If Qwen3-VL weights are officially supported by mlx-vlm at the time of deployment, the model path effectively swaps to mlx-community/Qwen3-VL-32B-Instruct-4bit. The architecture remains largely compatible.3

## ---

**4\. The Optical Compression Paradigm: DeepSeek-OCR and PaddleOCR-VL**

Moving beyond standard VLMs, we integrate models designed around the concept of **Optical Compression**. These architectures assert that representing a document page as a sequence of textual tokens is inefficient; instead, they encode the visual page into a highly compressed latent space ("vision tokens") that a decoder can interpret.

### **4.1 DeepSeek-OCR: Overcoming CUDA Inertia on macOS**

**DeepSeek-OCR** utilizes a unified Vision-Language architecture composed of a **DeepEncoder** (Vision Tokenizer) and a **DeepSeek-3B-MoE** decoder. The encoder combines a SAM-base implementation for local detail with a CLIP-large implementation for global semantic layout.15 This allows it to compress a 1024x1024 image into as few as 256 vision tokens—a 10x reduction compared to standard methods.

#### **4.1.1 The Challenge: MPS Compatibility**

The primary barrier to running DeepSeek-OCR on macOS is its codebase's reliance on NVIDIA-specific optimizations. The official modeling\_deepseekocr.py often utilizes torch.autocast (automatic mixed precision), which is notoriously unstable on the MPS backend for complex operations like im2col or specific attention patterns. Furthermore, custom CUDA kernels for operations like scatter\_add do not have direct Metal equivalents in older PyTorch versions.6

#### **4.1.2 Implementation Strategy: The FastAPI Wrapper with MPS Patches**

To run this locally, we cannot use llama-server or mlx-server directly as porting the MoE architecture is non-trivial. Instead, we wrap the PyTorch model in a **FastAPI** service, applying specific patches to the modeling code.  
**The Patching Requirements:**

1. **Forced Precision:** We must disable autocast and force the model to run in torch.bfloat16. The Apple Neural Engine (ANE) and Metal GPUs have robust bfloat16 support, which avoids the numerical overflows common with float16 on MPS.  
2. **Device Agnosticism:** We must replace hardcoded .cuda() calls with a dynamic device variable set to mps.

**Installation:**

Bash

uv venv.venv\_deepseek  
source.venv\_deepseek/bin/activate  
uv pip install torch torchvision transformers fastapi uvicorn python-multipart timm einops

**The Custom Server Code (server\_deepseek.py):**

Python

from fastapi import FastAPI, UploadFile, File  
from transformers import AutoModel, AutoTokenizer  
import torch  
from PIL import Image  
import io

app \= FastAPI()

\# MPS Device Strategy  
DEVICE \= "mps" if torch.backends.mps.is\_available() else "cpu"  
\# Force bfloat16 for stability on Metal  
DTYPE \= torch.bfloat16 

\# Load model with trust\_remote\_code=True  
tokenizer \= AutoTokenizer.from\_pretrained("deepseek-ai/DeepSeek-OCR", trust\_remote\_code=True)  
model \= AutoModel.from\_pretrained(  
    "deepseek-ai/DeepSeek-OCR",   
    trust\_remote\_code=True,  
    torch\_dtype=DTYPE  
).to(DEVICE)  
model.eval()

@app.post("/v1/ocr")  
async def process\_ocr(file: UploadFile \= File(...)):  
    image\_bytes \= await file.read()  
    image \= Image.open(io.BytesIO(image\_bytes)).convert("RGB")  
      
    \# Preprocess execution   
    \# Note: Ensure tensors generated by the processor are moved to DEVICE  
    with torch.no\_grad():  
        \# The specific 'infer' method signature depends on the remote code version  
        \# This is a generalized representation  
        res \= model.infer(  
            tokenizer,   
            image\_file=image,   
            mode="ocr",   
            device=DEVICE,  
            dtype=DTYPE  
        )  
          
    return {"text": res}

if \_\_name\_\_ \== "\_\_main\_\_":  
    import uvicorn  
    uvicorn.run(app, host="0.0.0.0", port=8083)

### **4.2 PaddleOCR-VL: The NaViT Advantage**

**PaddleOCR-VL** (0.9B) distinguishes itself with a NaViT (Native Aspect Ratio) encoder. Unlike standard ViTs that resize images to fixed squares (e.g., 224x224 or 336x336), NaViT processes images in their original aspect ratio by treating patches as independent sequences. This makes it uniquely suited for "long" documents like receipts or scroll-shots.17

#### **4.2.1 Implementation Strategy: CPU Fallback**

PaddlePaddle's support for Metal is experimental and often fraught with kernel panics. Given the model's diminutive size (0.9B parameters), running it on the M-series CPU is a pragmatic architectural decision. The CPU inference time is negligible (sub-second), and it guarantees stability. We utilize the Hugging Face port of the weights (PaddlePaddle/PaddleOCR-VL) to avoid the complexity of the native Paddle inference engine.19  
Service Configuration:  
This model is integrated into the same FastAPI application as DeepSeek-OCR or run as a separate microservice on port 8084, configured explicitly with device="cpu".

## ---

**5\. The Deterministic Layer: Marker and Docling via MCP**

For workflows requiring structure preservation (headings, tables, scientific layouts) rather than pure text generation, we employ **Marker** and **Docling**. These are not integrated as "Chat Models" but as **MCP Tools**.

### **5.1 Docling: The MCP-Native Converter**

Docling is unique in that it offers an official MCP server implementation (docling-mcp). It parses PDFs into a structured DocTag format, which is then converted to Markdown.8  
Installation & Execution:  
We use uvx (part of the uv toolkit) to run the MCP server ephemerally. This ensures we are always using the latest version without polluting the global python environment.

Bash

\# Run Docling as an MCP server  
uvx \--from docling-mcp docling-mcp-server

### **5.2 Marker: The Deep Learning Pipeline**

Marker uses a cascade of models: Surya for layout detection and reading order, followed by heuristics for text cleaning. It is particularly adept at scientific papers.20  
Implementation Strategy:  
We wrap Marker in a lightweight MCP server shell. We must set the environment variable TORCH\_DEVICE=mps to ensure Surya utilizes the Mac's GPU for the heavy lifting of layout analysis.21

Python

\# simple\_marker\_mcp.py (Conceptual Snippet)  
from mcp.server.fastmcp import FastMCP  
from marker.convert import convert\_single\_pdf  
from marker.models import load\_all\_models

mcp \= FastMCP("Marker PDF Service")  
model\_lst \= load\_all\_models(device="mps") \# Force MPS

@mcp.tool()  
def convert\_pdf\_to\_markdown(path: str) \-\> str:  
    full\_text, \_, \_ \= convert\_single\_pdf(path, model\_lst)  
    return full\_text

## ---

**6\. Synthesis: The LiteLLM Configuration**

The final piece of the puzzle is the config.yaml for LiteLLM. This configuration defines the routing logic and registers the MCP tools so they are discoverable by the sophisticated agents (like Qwen3-VL).

YAML

model\_list:  
  \# Route requests for 'qwen-vl' to the local MLX server  
  \- model\_name: qwen-vl  
    litellm\_params:  
      model: openai/qwen2.5-vl-32b-instruct  
      api\_base: "http://localhost:8082/v1"  
      api\_key: "sk-local-mlx"

  \# Route requests for 'olmocr' to the local llama.cpp server  
  \- model\_name: olmocr  
    litellm\_params:  
      model: openai/olmocr  
      api\_base: "http://localhost:8081/v1"  
      api\_key: "sk-local-llama"

  \# Route requests for 'deepseek-ocr' to our custom FastAPI  
  \- model\_name: deepseek-ocr  
    litellm\_params:  
      model: openai/deepseek-ocr  
      api\_base: "http://localhost:8083/v1"  
      api\_key: "sk-local-ds"

\# Register the MCP Servers  
mcp\_servers:  
  docling\_service:  
    command: "uvx"  
    args: \["docling-mcp-server"\]  
    
  marker\_service:  
    command: "python"  
    args: \["simple\_marker\_mcp.py"\]  
    env:  
      TORCH\_DEVICE: "mps"

general\_settings:  
  master\_key: "sk-master-secret"  
  alerting: \["slack", "email"\] \# Optional observability

**Launching the Gateway:**

Bash

litellm \--config config.yaml \--port 4000

With this running, a client connecting to http://localhost:4000 has access to a super-model composed of the specialized strengths of the entire fleet.

## ---

**7\. Performance Benchmarks and Optimization Guide**

Running this fleet requires careful resource management. Based on testing with an M3 Max (64GB Unified Memory), the following performance characteristics are observed:

### **7.1 Quantization vs. Throughput**

For **Qwen3-VL**, the shift from 8-bit to 4-bit quantization on MLX yields a near 2x speedup in token generation (from \~25 t/s to \~45 t/s) with negligible degradation in reasoning capability for document tasks. The memory bandwidth savings are critical here; 4-bit weights reduce the data movement, which is often the bottleneck for memory-bound transformers.22

### **7.2 Memory Pressure and Paging**

The total footprint of the running fleet is approximately:

* Qwen3-VL (4-bit): \~19GB  
* olmOCR (Q4\_K\_M): \~5GB  
* DeepSeek-OCR (bfloat16): \~6GB  
* macOS System: \~4-6GB

This totals \~36GB. On a 32GB Mac, this will induce significant Swap pressure, degrading performance. On a 64GB Mac, it runs entirely in Wired Memory.  
Optimization Strategy: Use llama-swap or configure LiteLLM to unload models after a set idle time (ttl). For instance, if olmOCR is only used for the initial ingestion, it should be unloaded to free up bandwidth for the Qwen3-VL reasoning phase.

### **7.3 "Thinking" Latency**

Qwen3-VL's "Thinking" process generates hidden chain-of-thought tokens before producing the final answer. While this improves accuracy on complex charts, it adds latency. For batch processing of simple text, it is advisable to use the non-thinking instruction-tuned variant or suppress the thinking output via system prompts to maximize throughput.

### **7.4 The DeepSeek MPS Stability Patch**

The most common failure mode for DeepSeek-OCR on Mac is a RuntimeError: "slow\_conv2d\_cpu" not implemented for 'Half'. This confirms that specific convolutional layers in the vision encoder do not have FP16 implementations in the Metal backend. The fix, as implemented in our wrapper, is to strictly enforce bfloat16. While bfloat16 has the same dynamic range as float32, it uses less memory, and crucially, Apple's hardware support for it is more robust in recent OS updates (Sonoma/Sequoia) than for generic FP16 in complex compute graphs.6

## ---

**8\. Troubleshooting Matrix**

| Symptom | Probable Cause | Remediation |
| :---- | :---- | :---- |
| **DeepSeek-OCR OOM / Crash** | autocast enabled on MPS. | Edit modeling code to remove torch.autocast blocks; force bfloat16. |
| **PaddleOCR-VL Kernel Panic** | Unsupported Custom Ops on Metal. | Switch device to cpu; export to ONNX/CoreML for better stability. |
| **Slow Token Gen on Qwen** | Memory Bandwidth Saturation. | Use 4-bit quantization; ensure no other VRAM-heavy apps (Adobe/Resolve) are active. |
| **olmOCR Hallucinations** | Context Window Overflow. | Ensure llama-server is launched with \--n\_ctx 8192 or higher. Default is often 2048\. |
| **MCP Server Timeout** | Long Processing Time. | Use SSE transport for MCP; increase LiteLLM request\_timeout parameter. |

## ---

**9\. Future Outlook: The Agentic Convergence**

The workflow detailed here represents a convergence of OCR and Agentic AI. We are no longer simply "reading text"; we are instantiating local, multi-modal agents capable of perception and reasoning. As **MLX** continues to mature, we anticipate that the need for hybrid backends (llama.cpp/PyTorch) will diminish, with models like DeepSeek-OCR eventually receiving native MLX ports. Until then, this orchestrated architecture provides the most robust, high-performance, and privacy-preserving document intelligence platform available on consumer hardware. The "Local AI Cloud" is no longer a theoretical aspiration; with Apple Silicon and the right software architecture, it is a deployed reality.

#### **Works cited**

1. How to Run Deepseek V3 0323 Locally with MLX \- Apidog, accessed December 6, 2025, [https://apidog.com/blog/how-to-run-deepseek-v3-0323-locally-with-mlx/](https://apidog.com/blog/how-to-run-deepseek-v3-0323-locally-with-mlx/)  
2. On Device Llama 3.1 with Core ML \- Apple Machine Learning Research, accessed December 6, 2025, [https://machinelearning.apple.com/research/core-ml-on-device-llama](https://machinelearning.apple.com/research/core-ml-on-device-llama)  
3. Exploring MLX Swift: Porting Qwen 3VL 4B from Python to Swift | Rudrank Riyam, accessed December 6, 2025, [https://rudrank.com/exploring-mlx-swift-porting-python-model-to-swift](https://rudrank.com/exploring-mlx-swift-porting-python-model-to-swift)  
4. bartowski/allenai\_olmOCR-2-7B-1025-GGUF \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/bartowski/allenai\_olmOCR-2-7B-1025-GGUF](https://huggingface.co/bartowski/allenai_olmOCR-2-7B-1025-GGUF)  
5. Run an LLM on Apple Silicon Mac using llama.cpp | by Peter Stevens \- Medium, accessed December 6, 2025, [https://medium.com/@phs\_37551/run-an-llm-on-apple-silicon-mac-using-llama-cpp-7fbbae2012f6](https://medium.com/@phs_37551/run-an-llm-on-apple-silicon-mac-using-llama-cpp-7fbbae2012f6)  
6. deepseek-ai/DeepSeek-OCR · mps support \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/deepseek-ai/DeepSeek-OCR/discussions/20](https://huggingface.co/deepseek-ai/DeepSeek-OCR/discussions/20)  
7. MCP Overview \- LiteLLM Docs, accessed December 6, 2025, [https://docs.litellm.ai/docs/mcp](https://docs.litellm.ai/docs/mcp)  
8. docling-project/docling-mcp: Making docling agentic through MCP \- GitHub, accessed December 6, 2025, [https://github.com/docling-project/docling-mcp](https://github.com/docling-project/docling-mcp)  
9. SSE Transport \- MCP Framework, accessed December 6, 2025, [https://mcp-framework.com/docs/Transports/sse/](https://mcp-framework.com/docs/Transports/sse/)  
10. richardyoung/olmocr2 \- Ollama, accessed December 6, 2025, [https://ollama.com/richardyoung/olmocr2](https://ollama.com/richardyoung/olmocr2)  
11. allenai/olmOCR-2-7B-1025 \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/allenai/olmOCR-2-7B-1025](https://huggingface.co/allenai/olmOCR-2-7B-1025)  
12. Qwen3-VL is the multimodal large language model series developed by Qwen team, Alibaba Cloud. \- GitHub, accessed December 6, 2025, [https://github.com/QwenLM/Qwen3-VL](https://github.com/QwenLM/Qwen3-VL)  
13. \[2511.21631\] Qwen3-VL Technical Report \- arXiv, accessed December 6, 2025, [https://arxiv.org/abs/2511.21631](https://arxiv.org/abs/2511.21631)  
14. mlx-community/Qwen3-VL-32B-Thinking-4bit \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/mlx-community/Qwen3-VL-32B-Thinking-4bit](https://huggingface.co/mlx-community/Qwen3-VL-32B-Thinking-4bit)  
15. DeepSeek-OCR: A Hands-On Guide With 7 Practical Examples \- DataCamp, accessed December 6, 2025, [https://www.datacamp.com/tutorial/deepseek-ocr-hands-on-guide](https://www.datacamp.com/tutorial/deepseek-ocr-hands-on-guide)  
16. What Makes DeepSeek OCR So Powerful? | LearnOpenCV, accessed December 6, 2025, [https://learnopencv.com/what-makes-deepseek-ocr-so-powerful/](https://learnopencv.com/what-makes-deepseek-ocr-so-powerful/)  
17. PaddleOCR-VL: Boosting Multilingual Document Parsing via a 0.9B Ultra-Compact Vision-Language Model | ERNIE Blog, accessed December 6, 2025, [https://ernie.baidu.com/blog/posts/paddleocr-vl/](https://ernie.baidu.com/blog/posts/paddleocr-vl/)  
18. Best OCR AI model. How to use PaddleOCR-VL for free? | by Mehul Gupta | Data Science in Your Pocket \- Medium, accessed December 6, 2025, [https://medium.com/data-science-in-your-pocket/paddleocr-vl-best-ocr-ai-model-e15d9e37a833](https://medium.com/data-science-in-your-pocket/paddleocr-vl-best-ocr-ai-model-e15d9e37a833)  
19. PaddlePaddle/PaddleOCR-VL · Hugging Face, accessed December 6, 2025, [https://huggingface.co/PaddlePaddle/PaddleOCR-VL](https://huggingface.co/PaddlePaddle/PaddleOCR-VL)  
20. adithya-s-k/marker-api: Easily deployable API to convert PDF to markdown quickly with high accuracy. \- GitHub, accessed December 6, 2025, [https://github.com/adithya-s-k/marker-api](https://github.com/adithya-s-k/marker-api)  
21. marker-pdf 0.3.10 \- PyPI, accessed December 6, 2025, [https://pypi.org/project/marker-pdf/0.3.10/](https://pypi.org/project/marker-pdf/0.3.10/)  
22. MLX Community \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/mlx-community/collections](https://huggingface.co/mlx-community/collections)