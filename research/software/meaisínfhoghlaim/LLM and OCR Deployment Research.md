# **Advanced Architectures for Document Intelligence on Apple Silicon: A Comprehensive Analysis of PaddleOCR v3, Docling, and Vision-Language Models**

## **1\. Introduction: The Paradigm Shift in Document Intelligence**

The field of Document Intelligence has historically been dominated by cascaded computer vision pipelines, typically characterized by a rigid sequence of operations: binarization, layout analysis, text line detection, and finally, optical character recognition (OCR). For decades, this heuristic-based approach served as the industrial standard, powering everything from invoice automation to archival digitization. However, the advent of the Transformer architecture and the subsequent rise of Large Language Models (LLMs) have precipitated a fundamental paradigm shift. We are no longer merely "recognizing characters"; we are now engineering systems capable of "visual understanding."  
This transition from recognition to understanding is epitomized by the emergence of Vision-Language Models (VLMs). Unlike traditional OCR engines that output a stream of disjointed text, VLMs ingest the entire document image as a visual token stream, projecting it into a high-dimensional semantic space where text, layout, and visual features are inextricably linked. This allows for the extraction of structured information—tables, charts, and logical relationships—with a fidelity that heuristic systems could never achieve.  
For the modern machine learning engineer or systems architect, this technological leap necessitates a complete re-evaluation of the deployment stack. This is particularly true for professionals operating within the Apple Silicon ecosystem (M1, M2, M3, and M4 chipsets). The unified memory architecture and potent Neural Engine of Apple’s silicon offer theoretical inference capabilities that rival dedicated discrete GPUs. Yet, the software ecosystem remains fractured. The industry-standard deployment vehicle—Docker containers running on Linux—introduces a virtualization layer that fundamentally clashes with Apple's Metal graphics API, creating a dichotomy between "easy deployment" and "hardware acceleration."  
This report provides an exhaustive technical analysis of this landscape, specifically tailored to the user's requirement to integrate **PaddleOCR v3**, **Docling (Granite-Docling)**, and **Qwen2.5-VL** into a cohesive workflow on a MacBook. We will dissect the internal architectures of these models, analyze the limitations of Docker on macOS, demystify the role of inference engines like vLLM and llama.cpp, and propose a hybrid-native architecture that maximizes the specific hardware advantages of Apple Silicon while maintaining the modularity of microservices.

## **2\. Theoretical Framework: VLM Architectures for Document Parsing**

To understand the comparative advantages of PaddleOCR-VL, Granite-Docling, and Qwen2.5-VL, one must first appreciate the architectural innovations that distinguish them from traditional OCR.

### **2.1 The Traditional OCR Pipeline vs. The VLM Approach**

Traditional systems, such as the earlier versions of PaddleOCR (v2) or Tesseract, operate on a "bottom-up" principle. A detection network (often based on DBNet or EAST) scans the image to identify bounding boxes containing text. These cropped regions are then fed into a recognition network (CRNN or SVTR) which transcribes the pixel data into string data. The structural relationship between these text boxes—whether they form a table, a paragraph, or a header—is reconstructed post-hoc using geometric heuristics. This approach is brittle; a slight misalignment in detection can shatter the logical structure of a table, and complex layouts like multi-column scientific papers often result in incoherent reading orders.  
The VLM approach, utilized by PaddleOCR-VL, Granite-Docling, and Qwen2.5-VL, is "top-down." The model perceives the image globally. The visual encoder transforms the pixel data into a sequence of embeddings. The language model decoder then autoregressively generates the text, inherently understanding the reading order and layout because it has been trained on millions of documents where the "next token" prediction depends on both the textual context and the 2D spatial position.

### **2.2 Dynamic Resolution and the NaViT Encoder**

A critical innovation shared by the most advanced models in this study (PaddleOCR-VL and Qwen2.5-VL) is the handling of image resolution. Standard Vision Transformers (ViTs), such as the one used in the original CLIP model, require input images to be resized to a fixed square resolution, typically $224 \\times 224$ or $336 \\times 336$ pixels.  
For document processing, fixed-resolution resizing is catastrophic. A long receipts, a wide spreadsheet, or a high-density A4 academic paper contains high-frequency details (small fonts) that are obliterated when downsampled to a low-resolution square. Furthermore, the aspect ratio distortion introduces artifacts that confuse the model.  
The solution, adopted by PaddleOCR-VL and Qwen2.5-VL, is the **NaViT (Native Resolution Vision Transformer)** approach. Instead of resizing the image, the model divides the image into patches of fixed size (e.g., $14 \\times 14$ pixels) based on its original resolution. These patches are then packed into sequences. A "Patch Padding" or specialized attention mask is used to handle the variable sequence lengths within a batch. This allows the model to "read" a tall, thin receipt or a wide landscape chart with equal native fidelity, preserving the high-frequency edge information required to distinguish between a 'c' and an 'e' in 6-point font.

### **2.3 The Role of Instruction Tuning in Documents**

While the visual encoder handles perception, the utility of these models comes from instruction tuning. Granite-Docling and Qwen2.5-VL have been fine-tuned on massive datasets of "Document-QA" pairs. This means the models are not just trained to "transcribe text," but to "convert structure."  
Granite-Docling, for instance, is trained to output a specialized pseudo-code format known as **DocTags**. When it sees a table, it doesn't just output the words; it outputs \<table\_start\>\<row\>\<cell\>Data\</cell\>...\</row\>. This semantic awareness is injected into the model weights during the supervised fine-tuning (SFT) phase, effectively compressing the logic of a complex layout parser into the neural network itself.

## **3\. Deep Dive: PaddleOCR v3 and PaddleOCR-VL**

PaddleOCR has long been the gold standard for industrial OCR, particularly for CJK (Chinese, Japanese, Korean) languages. The release of Version 3.0 marks a significant strategic pivot from "lightweight and mobile-first" to "accurate and server-first."

### **3.1 PaddleOCR v3 Architecture**

The v3 framework is built upon PaddlePaddle 3.0, Baidu's deep learning framework which competes with PyTorch and TensorFlow. The v3 release introduces a "Unified Inference Interface," aiming to standardize how different modules (text detection, table recognition, layout analysis) interact.1

#### **3.1.1 PP-OCRv5**

The text recognition component, PP-OCRv5, introduces several key enhancements over v4 1:

* **Backbone Upgrade:** It utilizes PP-HGNetV2 for the detection model. HGNet (High-Performance GPU Net) is designed to maximize throughput on NVIDIA GPUs by optimizing kernel usage, reducing memory access costs compared to traditional ResNets.  
* **Recognition Strategy:** The recognition module uses SVTR (Scene Text Recognition), which combines local and global mixing of features. This is crucial for recognizing distinct characters in varied fonts and orientations.  
* **Data Augmentation:** The v5 models are trained with extensive data synthesis, specifically targeting "hard cases" mined from large-scale datasets using teacher models (like larger VLMs) to distill knowledge into the compact student model.

#### **3.1.2 PP-StructureV3**

This module is the backbone of document parsing. It moves beyond simple text to layout analysis. It employs a detection model to identify regions (Header, Footer, Table, Figure) and a separate recognition model for the content within those regions. Crucially, PP-StructureV3 excels at **Table Recognition**, converting raster table images into Excel-compatible structures with high accuracy, a task where standard LLMs often hallucinate row/column alignment.

### **3.2 PaddleOCR-VL: The Vision-Language Specialist**

PaddleOCR-VL is the most relevant component for the user's query regarding "deep research." It is a specialized VLM with approximately 0.9 Billion parameters.3  
**Architecture Details:**

* **Visual Encoder:** As mentioned, it uses a dynamic resolution encoder inspired by NaViT. This allows it to ingest documents at native DPI levels.  
* **LLM Backbone:** The language model is **ERNIE-4.5-0.3B**. ERNIE (Enhanced Representation through Knowledge Integration) is Baidu's answer to BERT/LLaMA. The 0.3B variant is extremely small, optimized for high throughput.  
* **Alignment:** The visual features are projected into the ERNIE embedding space using a lightweight MLP (Multi-Layer Perceptron) connector.

Deployment Constraints (The "Deploy" Folder Analysis):  
The user referenced the deploy folder in the PaddleOCR repository. A granular analysis of this folder reveals the project's hardware assumptions.4

* **CUDA Dominance:** The Dockerfiles and compose.yaml configurations heavily favor NVIDIA. They utilize nvidia-docker runtimes and set environment variables like CUDA\_VISIBLE\_DEVICES.  
* **C++ Inference:** The deploy/cpp\_infer directory contains high-performance C++ source code using the Paddle Inference library. This library relies on mkldnn for Intel CPUs and cuDNN/TensorRT for NVIDIA GPUs.  
* **Missing Metal:** Crucially, the Paddle Inference library has **limited to no support for Apple Metal**. While PaddlePaddle supports MacOS via OpenBLAS (CPU), the optimized operations required for the VLM's dynamic attention mechanisms are likely not implemented for the MPS (Metal Performance Shaders) backend. This means running PaddleOCR-VL on a Mac, even natively, often defaults to CPU execution, which is significantly slower than the Neural Engine.

### **3.3 Limitations for the Mac User**

The "PaddleOCR-VL" promise of SOTA performance is contingent on having the right hardware. For a Mac user, the experience is compromised.

* **Docker:** Running the official Docker image on Mac forces CPU emulation. The 0.9B model, while small, still requires billions of floating-point operations per token. On a virtualized CPU, this results in latency of 10-30 seconds per page 6, rendering it unusable for real-time applications compared to native Metal execution.  
* **Dependency Hell:** Attempting to build PaddleOCR-VL from source on Mac to bypass Docker involves navigating complex dependency trees (protobuf versions, python versions) that often conflict with the system's clang compiler or arm64 architecture quirks.

## **4\. Deep Dive: Docling and Granite-Docling**

Docling, developed by IBM Research, represents a philosophy of "Document Conversion as a Service." It is not just an OCR engine; it is a pipeline designed to normalize unstructured data into a schema that RAG systems can consume.7

### **4.1 The Docling Ecosystem**

The core of Docling is its modular pipeline architecture.

* **Input Handling:** Docling accepts PDF, DOCX, PPTX, HTML, and images.  
* **Backend Selection:** It automatically routes documents. Digital PDFs might go through pypdfium2 for fast text extraction. Scanned PDFs are routed to the **VLM Pipeline**.  
* **The DoclingDocument:** The central data structure is the DoclingDocument object. This is a rich representation that stores not just text, but bounding boxes, hierarchical levels (Section 1, Section 1.1), table cells, and metadata. This object can be serialized losslessly to JSON or exported to Markdown.8

### **4.2 Granite-Docling-258M: The Efficient Expert**

The engine powering the VLM pipeline is **Granite-Docling-258M**.

* **Parameter Efficiency:** At 258 million parameters, it is nearly 4x smaller than PaddleOCR-VL. This makes it exceptionally fast and memory-efficient, fitting easily into the RAM of even the base model MacBook Air.9  
* **SigLIP2 Encoder:** It uses the SigLIP2 (Sigmoid Loss for Language Image Pre-training) encoder. SigLIP is known for better image-text alignment convergence than standard CLIP.  
* **Granite 165M Decoder:** The language model is a member of IBM's Granite family, specifically tuned for code and structured text.  
* **DocTags Training:** The model was trained to output specific XML-like tags (\<title\>, \<figure\>, \<table\>). This is a crucial differentiator. Generic VLMs like Qwen might simply output the text of a table line by line. Granite-Docling outputs the *structure* of the table, ensuring that when it is rendered to Markdown, the rows and columns are preserved.10

### **4.3 Native MLX Support: The Key Differentiator**

The user's query asks about taking advantage of MLX. Docling is the only tool in this set that supports MLX natively and effortlessly.  
The docling python library has optional dependencies. Installing pip install "docling\[mlx\]" pulls in the mlx and mlx-vlm libraries. When the pipeline is initialized on a Mac, Docling detects the architecture and automatically loads the Granite model weights into the Metal unified memory.11

* **Performance:** On an M3 Max, Granite-Docling via MLX can parse a page in under 1 second.13 This is an order of magnitude faster than running PaddleOCR-VL in Docker.

### **4.4 Docling-Serve Architecture**

docling-serve is a Python application (FastAPI) that wraps the library.

* **Configuration:** It is configured via environment variables (e.g., DOCLING\_SERVE\_ARTIFACTS\_PATH).  
* **No Native MLX in Docker:** The standard docling-serve Docker images are based on Linux (Debian/Ubuntu). They contain CUDA drivers. If the user runs docker run docling-serve on a Mac, the container runs in a Linux VM. This VM **cannot** access the Mac's Metal API. Therefore, docling-serve in Docker will run on the CPU.14  
* **The "vLLM" Confusion:** The user noted that Docling mentions vLLM. Docling *can* use vLLM as a remote backend. One can configure Docling to send the image to a separate server running vLLM (e.g., a Linux GPU server). However, running vLLM *itself* on a Mac is not the path to MLX acceleration; vLLM is optimized for NVIDIA.15

## **5\. Deep Dive: Qwen2.5-VL and GLM-4.5v**

These models represent the "General Purpose" end of the spectrum. They are not specialized solely for documents, but their massive scale and varied training data make them capable of reasoning tasks that smaller models cannot handle.

### **5.1 Qwen2.5-VL: The Reasoning Giant**

Qwen2.5-VL (7B parameters) is a significant step up in capability.16

* **M-RoPE (Multimodal Rotary Positional Embeddings):** This innovation allows the model to handle 1D text, 2D images, and 3D video sequences in a unified positional space.  
* **Visual Reasoning:** Unlike Granite or Paddle which primarily "extract," Qwen can "reason." You can ask Qwen, "Is the total on this invoice consistent with the line items?" and it can perform the arithmetic and logic verification.  
* **Mac Deployment:** Qwen2.5-VL has excellent support on Mac via **MLX-VLM** and **llama.cpp**. The mlx-vlm package provides a server implementation that mimics the OpenAI API. Running this native server allows Qwen to utilize the GPU, achieving speeds of 50-70 tokens per second on M-series chips.17

### **5.2 GLM-4.5v: The Cloud Benchmark**

GLM-4.5v is Zhipu AI's proprietary model.18

* **Architecture:** It uses a GLM (General Language Model) backbone with RLHF (Reinforcement Learning from Human Feedback) specifically tuned for agentic tasks.  
* **API Economics:** Access is strictly via API. While efficient for low volume, the latency (network round trip \+ server queue) sets a hard floor on performance (typically 2-5 seconds per request).  
* **Cost:** At $0.60 per million input tokens, it is affordable for reasoning tasks but expensive for bulk digitization (OCR) compared to the zero marginal cost of local models.19

## **6\. Engineering the Solution: The Docker vs. Native Conflict**

The user's central engineering challenge is the desire to use docker compose while leveraging mlx. This is a fundamental conflict in the current macOS virtualization stack.

### **6.1 The Docker Virtualization Barrier**

Docker Desktop on macOS uses a hypervisor (HyperKit, VPNKit, or the Apple Virtualization Framework) to run a Linux kernel.

* **Isolation:** This Linux kernel is isolated from the host hardware.  
* **GPU Passthrough:** While NVIDIA has engineered "NVIDIA Container Toolkit" to pass GPU access to containers on Linux hosts, no equivalent robust standard exists for passing the Apple Metal API into a Linux container.  
* **Result:** Any process inside a Docker container on Mac sees a generic virtual CPU. It cannot see the M-series GPU or Neural Engine.

### **6.2 The "vLLM" Red Herring**

The user noticed vLLM support in repositories and asked if this enables MLX.

* **vLLM Architecture:** vLLM is built around PagedAttention, a memory management technique optimizing the KV-cache for high throughput. Its kernels are written in CUDA (for NVIDIA) and HIP (for AMD).  
* **vLLM on Mac:** There is experimental CPU support for vLLM, and some very recent efforts to port kernels to Metal, but it is not the standard or performant way to run models on Mac.  
* **MLX Architecture:** MLX is Apple's own framework, designed from the ground up for Unified Memory. It does not use vLLM. It uses its own serving logic (mlx.server).  
* **Conclusion:** Seeing vLLM in a repo like Docling implies it can connect to a Linux GPU server running vLLM. It does not mean it uses vLLM to run fast on a Mac.

### **6.3 The Hybrid-Native Architecture**

To satisfy the user's requirements (Comparative Workflow \+ Speed \+ Cost \+ Mac Optimization), we must propose a **Hybrid Architecture**. We cannot use Docker for the *inference engines*, but we can use Docker for the *application logic* and database, while the inference engines run as "Native Services" on the host.

## **7\. Configuration and Implementation Guide**

This section provides the specific technical steps to implement the recommended solution.

### **7.1 Component 1: Native Docling Service (The "Fast" Parser)**

We will run docling-serve natively to unlock MLX.  
**Step 1: Setup Environment**

Bash

\# Create a dedicated directory  
mkdir docling-native  
cd docling-native

\# Create a virtual environment (using uv is recommended for speed)  
uv venv.venv \--python 3.11  
source.venv/bin/activate

\# Install docling with MLX support  
pip install "docling\[mlx\]" docling-serve

Step 2: Configure and Run  
By default, docling detects the hardware. With docling\[mlx\] installed on an ARM64 Mac, it prioritizes the MLX backend for the Granite model.

Bash

\# Set environment variables for the service  
export DOCLING\_SERVE\_PORT=5001  
export DOCLING\_SERVE\_HOST=0.0.0.0

\# Run the server  
docling-serve run

*Verification:* Monitor the logs. When the first request comes in, you should see initialization of the MLX backend, not the PyTorch CPU backend.

### **7.2 Component 2: Native Qwen2.5-VL Service (The "Smart" Reasoner)**

We will use mlx-vlm to serve Qwen.  
**Step 1: Installation**

Bash

\# In a separate terminal or same venv  
pip install mlx-vlm huggingface\_hub

Step 2: Serving  
We serve the 4-bit quantized version for maximum speed.

Bash

python \-m mlx\_vlm.server \--model mlx-community/Qwen2.5-VL-7B-Instruct-4bit \--port 8081

This creates an OpenAI-compatible API endpoint at http://localhost:8081/v1/chat/completions.

### **7.3 Component 3: Llama-Swap (The Router)**

The user mentioned llama-swap. This tool acts as a proxy, routing requests to different backends based on the model name. This is perfect for aggregating our native services.  
**Step 1: Configuration (config.yaml)**

YAML

listen: :8080  
models:  
  \- name: qwen-vl  
    \# Llama-swap usually spawns processes, but here we can use it   
    \# to proxy to our already running mlx server if configured as an upstream   
    \# OR we let llama-swap manage the llama-server process directly.  
    \# Given the user wants to use llama-swap, let's configure it to manage llama-server.  
    cmd: "llama-server \-m /path/to/Qwen2.5-VL-7B-Instruct-Q4\_K\_M.gguf \--port 8081 \--n-gpu-layers 99"  
      
  \- name: docling-parse  
    \# Docling isn't an LLM, so it might not fit llama-swap's chat completion proxy paradigm perfectly   
    \# unless we wrap it. For this report, we treat Docling as a separate endpoint.

*Correction:* llama-swap is designed to swap llama-server binaries. Since Qwen2.5-VL is now supported in llama.cpp, the user can simply use llama-swap to manage the Qwen instance alongside their text models.

### **7.4 Component 4: PaddleOCR (The Docker Baseline)**

Since we want to compare speed and cost, we run PaddleOCR in Docker to demonstrate the performance difference (and because compiling it natively is non-trivial).  
**docker-compose.yaml**

YAML

services:  
  paddle-ocr:  
    image: paddlepaddle/paddleocr-vl:latest  
    container\_name: paddle\_baseline  
    ports:  
      \- "8082:8080"  
    environment:  
      \- CUDA\_VISIBLE\_DEVICES="" \# Force CPU  
    deploy:  
      resources:  
        limits:  
          cpus: '4' \# Simulate a constrained environment

## **8\. Comparative Workflow: Speed and Cost Analysis**

The user wants a "comparative workflow." This implies a structured test. The following analysis projects the expected results based on the architectural constraints identified above.

### **8.1 Benchmark Methodology**

We define a standard workload: **Digitizing a 10-page mixed-content PDF (Text \+ 2 Tables \+ 1 Chart).**  
**The Workflow Script (Conceptual Python):**

Python

\# 1\. Send to Docling (Native MLX)  
start \= time.time()  
requests.post("http://localhost:5001/v1/convert", files={'file': pdf})  
docling\_time \= time.time() \- start

\# 2\. Send to PaddleOCR (Docker CPU)  
start \= time.time()  
requests.post("http://localhost:8082/predict", json={'image': base64\_img})  
paddle\_time \= time.time() \- start

\# 3\. Send to Qwen2.5-VL (Native MLX via Llama-Swap/MLX-VLM)  
start \= time.time()  
client.chat.completions.create(model="qwen-vl", messages=\[...\])  
qwen\_time \= time.time() \- start

\# 4\. Send to GLM-4.5v (API)  
start \= time.time()  
zhipu\_client.chat.completions.create(model="glm-4.5v",...)  
glm\_time \= time.time() \- start

### **8.2 Projected Results and Analysis**

#### **8.2.1 Processing Speed (Latency)**

| Engine | Deployment | Accelerator | Est. Time (1 Page) | Insight |
| :---- | :---- | :---- | :---- | :---- |
| **Granite-Docling** | Native (MLX) | **Metal (GPU/ANE)** | **\< 1.0 sec** | **The Efficiency Winner.** Because it uses a small (258M) model directly on the hardware, it incurs virtually zero overhead. It is the only viable choice for real-time applications on Mac. |
| **Qwen2.5-VL (4-bit)** | Native (MLX) | Metal (GPU) | 3.0 \- 5.0 sec | Excellent for "reasoning." It is slower than Docling because the model is 25x larger (7B vs 0.25B), but MLX quantization keeps it interactive. |
| **GLM-4.5v** | API | Cloud GPU | 5.0 \- 10.0 sec | Network latency dominates. High variability based on internet connection and API congestion. |
| **PaddleOCR-VL** | Docker | **Virtual CPU** | 20.0 \- 45.0 sec | **The Bottleneck.** Running a 0.9B VLM on a virtualized CPU is computationally expensive. The lack of Metal passthrough makes this the slowest option by far. |

#### **8.2.2 Cost Effectiveness (10,000 Pages)**

| Engine | Hardware | Token Cost | Energy Cost | Total Cost |
| :---- | :---- | :---- | :---- | :---- |
| **Granite-Docling** | Local Mac | $0 | Negligible (\<$0.10) | **\~$0.10** |
| **Qwen2.5-VL** | Local Mac | $0 | Low (\<$0.50) | **\~$0.50** |
| **PaddleOCR-VL** | Local Mac | $0 | High (CPU grind) | **\~$1.00** |
| **GLM-4.5v** | Cloud API | \~$12.00 | N/A | **\~$12.00** |

**Insight:** For high-volume processing, local inference with Granite-Docling is essentially free. Using a commercial API like GLM-4.5v introduces a linear cost scaling that becomes prohibitive at archive scale (e.g., 1 million pages \= $1,200).

### **8.3 Feature Capability Matrix**

| Feature | Granite-Docling | PaddleOCR-VL | Qwen2.5-VL | GLM-4.5v |
| :---- | :---- | :---- | :---- | :---- |
| **Primary Output** | Structured Layout (DocTags) | Structured Layout (PP-Structure) | Conversational Text / JSON | Conversational Text |
| **Table Parsing** | **Excellent** (Preserves row/col) | **SOTA** (Optimized for tables) | Good (Reasoning based) | Very Good |
| **Layout Semantics** | **High** (Sections, Headers) | High (Reading Order) | Medium (Visual understanding) | Medium |
| **Reasoning** | Low (Extraction only) | Low (Extraction only) | **High** (Can answer logic questions) | **Very High** |
| **Deployment** | Simple (Python lib) | Complex (Docker/C++) | Simple (MLX/Llama.cpp) | Zero (API) |

## **9\. Recommendations and Strategic Roadmap**

Based on the deep analysis of the architectures and the specific constraints of the user's hardware (MacBook), the following roadmap is recommended.

### **9.1 The "Native-First" Strategy**

Abandon the attempt to containerize the inference engines. The abstraction cost of Docker on macOS is too high for VLM workloads.

* **Action:** Run docling-serve and mlx-vlm directly on the host OS.  
* **Rationale:** This unlocks the Neural Engine and Metal GPU, transforming a 30-second task (Docker CPU) into a sub-second task (Native MLX).

### **9.2 The Routing Logic**

Use Docling as the default ingestion engine. It is the fastest and most cost-effective way to turn a PDF into Markdown.  
Use Qwen2.5-VL (via llama-swap or mlx-vlm) as an "Escalation" engine. If Docling fails to parse a specific chart, or if the user asks a question about the document ("What is the sentiment of this handwritten note?"), route that specific request to Qwen.

### **9.3 The Role of PaddleOCR**

Keep PaddleOCR-VL in a Docker container only as a **benchmark reference**. Do not use it in the production hot path on a Mac. Its dependency on CUDA or generic CPU kernels makes it uncompetitive on Apple Silicon compared to the highly optimized MLX implementations of Granite and Qwen.

### **9.4 Final Architecture Diagram (Conceptual)**

1. **User Request** \-\> **Llama-Swap (Port 8080\)**  
2. **Llama-Swap Routes:**  
   * /convert \-\> **Docling Serve** (Native Process, Port 5001\) \-\> **MLX** \-\> **Granite Model**  
   * /chat (Model: Qwen) \-\> **Llama-Server** (Native Process, Port 8081\) \-\> **Metal** \-\> **Qwen2.5-VL**  
   * /chat (Model: GLM) \-\> **Proxy** \-\> **Zhipu API**

This architecture satisfies all user requirements: it compares the models, utilizes llama-swap, leverages MLX where possible (Docling, Qwen), and integrates the cloud API (GLM), all while navigating the specific constraints of the Apple Silicon platform.

## **10\. Glossary of Technical Terms**

* **NaViT (Native Resolution Vision Transformer):** A technique where images are processed in their original aspect ratio by patching them dynamically, rather than resizing them to a fixed square. Used by PaddleOCR-VL.  
* **DocTags:** A set of special tokens (e.g., \<title\>, \<table\>) used by Granite-Docling to represent document structure in the text output.  
* **MLX:** Apple's array framework for machine learning on Apple Silicon, designed for unified memory efficiency.  
* **Metal Performance Shaders (MPS):** The graphics framework on macOS that allows PyTorch to utilize the GPU.  
* **vLLM:** A high-throughput serving engine for LLMs, primarily optimized for CUDA (NVIDIA) and ROCm (AMD), using PagedAttention.  
* **SigLIP:** A variation of the CLIP model using Sigmoid Loss, offering better image-text alignment convergence. Used by Granite-Docling.  
* **SOTA:** State of the Art.

---

*End of Report*

#### **Works cited**

1. Home \- PaddleOCR Documentation, accessed December 6, 2025, [http://www.paddleocr.ai/main/en/index.html](http://www.paddleocr.ai/main/en/index.html)  
2. (PDF) PaddleOCR 3.0 Technical Report \- ResearchGate, accessed December 6, 2025, [https://www.researchgate.net/publication/393511573\_PaddleOCR\_30\_Technical\_Report](https://www.researchgate.net/publication/393511573_PaddleOCR_30_Technical_Report)  
3. PaddleOCR-VL: Boosting Multilingual Document Parsing via a 0.9B Ultra-Compact Vision-Language Model \- arXiv, accessed December 6, 2025, [https://arxiv.org/html/2510.14528v1](https://arxiv.org/html/2510.14528v1)  
4. PaddleOCR/deploy/paddleocr\_vl\_docker/compose.yaml at main ..., accessed December 6, 2025, [https://github.com/PaddlePaddle/PaddleOCR/blob/main/deploy/paddleocr\_vl\_docker/compose.yaml](https://github.com/PaddlePaddle/PaddleOCR/blob/main/deploy/paddleocr_vl_docker/compose.yaml)  
5. PaddleOCR-VL Usage Tutorial, accessed December 6, 2025, [http://www.paddleocr.ai/main/en/version3.x/pipeline\_usage/PaddleOCR-VL.html](http://www.paddleocr.ai/main/en/version3.x/pipeline_usage/PaddleOCR-VL.html)  
6. PaddleOCR-VL, is better than private models : r/LocalLLaMA \- Reddit, accessed December 6, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1o866vl/paddleocrvl\_is\_better\_than\_private\_models/](https://www.reddit.com/r/LocalLLaMA/comments/1o866vl/paddleocrvl_is_better_than_private_models/)  
7. docling-project/docling-serve: Running Docling as an API service \- GitHub, accessed December 6, 2025, [https://github.com/docling-project/docling-serve](https://github.com/docling-project/docling-serve)  
8. Docling Technical Report \- arXiv, accessed December 6, 2025, [https://arxiv.org/html/2408.09869v4](https://arxiv.org/html/2408.09869v4)  
9. ibm-granite/granite-docling-258M \- Hugging Face, accessed December 6, 2025, [https://huggingface.co/ibm-granite/granite-docling-258M](https://huggingface.co/ibm-granite/granite-docling-258M)  
10. IBM Granite-Docling: Super Charge your RAG 2.0 Pipeline | by Vishal Mysore | Medium, accessed December 6, 2025, [https://medium.com/@visrow/ibm-granite-docling-super-charge-your-rag-2-0-pipeline-32ac102ffa40](https://medium.com/@visrow/ibm-granite-docling-super-charge-your-rag-2-0-pipeline-32ac102ffa40)  
11. Installation \- Docling \- GitHub Pages, accessed December 6, 2025, [https://docling-project.github.io/docling/getting\_started/installation/](https://docling-project.github.io/docling/getting_started/installation/)  
12. Quickstart \- Docling \- GitHub Pages, accessed December 6, 2025, [https://docling-project.github.io/docling/getting\_started/quickstart/](https://docling-project.github.io/docling/getting_started/quickstart/)  
13. Benchmarking small models at 4bit quants on Apple Silicon with mlx-lm \- Reddit, accessed December 6, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1o50mfy/benchmarking\_small\_models\_at\_4bit\_quants\_on\_apple/](https://www.reddit.com/r/LocalLLaMA/comments/1o50mfy/benchmarking_small_models_at_4bit_quants_on_apple/)  
14. \[Support\] Docling Serve – Convert Documents to Markdown/JSON \- Unraid Forums, accessed December 6, 2025, [https://forums.unraid.net/topic/193982-support-docling-serve-convert-documents-to-markdownjson/](https://forums.unraid.net/topic/193982-support-docling-serve-convert-documents-to-markdownjson/)  
15. Stupid question, but for production should I be using vLLM? \#2305 \- GitHub, accessed December 6, 2025, [https://github.com/docling-project/docling/discussions/2305](https://github.com/docling-project/docling/discussions/2305)  
16. \[2502.13923\] Qwen2.5-VL Technical Report \- arXiv, accessed December 6, 2025, [https://arxiv.org/abs/2502.13923](https://arxiv.org/abs/2502.13923)  
17. Tested local LLMs on a maxed out M4 Macbook Pro so you don't have to : r/ollama \- Reddit, accessed December 6, 2025, [https://www.reddit.com/r/ollama/comments/1j0by7r/tested\_local\_llms\_on\_a\_maxed\_out\_m4\_macbook\_pro/](https://www.reddit.com/r/ollama/comments/1j0by7r/tested_local_llms_on_a_maxed_out_m4_macbook_pro/)  
18. GLM-4.5V \- Z.AI DEVELOPER DOCUMENT, accessed December 6, 2025, [https://zhipu-32152247.mintlify.app/guides/vlm/glm-4.5v](https://zhipu-32152247.mintlify.app/guides/vlm/glm-4.5v)  
19. Pricing \- Z.AI DEVELOPER DOCUMENT, accessed December 6, 2025, [https://docs.z.ai/guides/overview/pricing](https://docs.z.ai/guides/overview/pricing)