# **Architecting Unified Hybrid-Inference Gateways: A Comprehensive Analysis of Local-Cloud Interoperability for Multimodal AI**

## **1\. Executive Summary and Strategic Context**

The contemporary landscape of artificial intelligence infrastructure is characterized by a rapid bifurcation between centralized, ultra-large-scale proprietary models and increasingly capable, specialized open-weights models designed for local execution. Organizations and researchers are no longer choosing between "cloud" and "local" but are instead architecting hybrid substrates that leverage the latency and privacy benefits of edge computing alongside the raw reasoning power of cloud-based frontier models. This report provides an exhaustive architectural analysis and implementation strategy for constructing such a unified inference gateway.  
The proposed architecture aggregates diverse inference backends—specifically llama-swap wrapping llama-server for quantized GGUF inference, mlx-vlm for Apple Silicon-native optimization, and Zhipu AI’s (Z.AI) cloud API for high-fidelity multimodal fallback—into a single coherent control plane managed by litellm. We specifically target a high-complexity model suite: the vision-centric **Qwen3-VL**, the reasoning-heavy **DeepSeek-V3.1**, the natively multimodal **Gemma-3**, the massive **GPT-OSS** (120B), and the specialized **Granite-Docling**.  
To ensure this system is production-grade rather than merely experimental, the architecture enforces strict observability standards using **Langfuse** for trace analytics and **MLflow** for experiment registry and metric tracking. The analysis that follows details not just the configuration of these tools, but the underlying mechanical interplay between memory management, quantization strategies, and protocol normalization that makes such a "hot-swapping" environment viable.1

## **2\. Theoretical Framework: The Gateway Aggregation Pattern**

The core design philosophy employed here is the Gateway Aggregation Pattern. In a fragmented inference environment, client applications should not be aware of the underlying execution engine. Whether a prompt is served by a 4-bit quantized model residing on a local MacBook’s Unified Memory, an FP16 model on a CUDA cluster, or an API call to a Beijing-based datacenter, the client interface must remain the standard OpenAI Chat Completion protocol.

### **2.1 The Role of LiteLLM as the Unifying Control Plane**

At the apex of this stack sits **LiteLLM**, functioning as a stateless proxy and normalization engine. Its primary utility in this architecture extends beyond simple routing; it acts as the translation layer between the disparate dialects of inference providers. While llama-server and mlx-vlm both strive for OpenAI API compatibility, subtle deviations in parameter handling (e.g., stop token formats, image\_url processing, or "thinking" token exposition) often break standard clients.5  
LiteLLM addresses this by intercepting requests and normalizing them before dispatch. For the Z.AI integration specifically, LiteLLM handles the critical translation of standard audio/speech requests into the specific JSON payloads required by GLM-4-Voice and GLM-TTS, effectively treating a remote proprietary endpoint indistinguishably from a local text-to-speech engine.5 Furthermore, LiteLLM serves as the central injection point for observability. By hooking Langfuse and MLflow at the gateway level, we capture the "true" latency seen by the client, inclusive of network overhead and queue times, rather than just the inference time reported by the backend.3

### **2.2 Dynamic Resource Management via Llama-Swap**

For local inference involving massive models like **GPT-OSS (120B)** or **DeepSeek-V3.1**, distinct memory management challenges arise. Standard inference servers are persistent processes; they load model weights into RAM/VRAM at startup and retain them to minimize latency. However, loading a 120B parameter model even at Q4 quantization requires approximately 70-80 GB of memory.8 On a workstation with 96GB or 128GB of unified memory, running two such models simultaneously is impossible.  
**Llama-Swap** is introduced to resolve this resource contention through a "hot-swap" mechanism. It acts as a lightweight process manager that listens on a service port and inspects the incoming model parameter of HTTP requests. Unlike a standard load balancer, Llama-Swap maintains a mapping of model IDs to launch commands. When a request for gpt-oss arrives, Llama-Swap checks if the model is running. If not, and if system resources are constrained, it gracefully terminates the currently running model (e.g., qwen3-vl) and spawns the llama-server process for gpt-oss. This architecture transforms the inference station from a static server into a dynamic, on-demand compute utility, maximizing the utility of available hardware.1

### **2.3 Hardware-Specific Optimization via MLX-VLM**

While llama.cpp and GGUF formats offer broad compatibility, the architecture incorporates **MLX-VLM** to exploit the specific advantages of Apple Silicon. The MLX framework, developed by Apple Research, allows for direct access to the Unified Memory Architecture (UMA) without the overhead of translation layers often present in cross-platform libraries.  
For models like **Granite-Docling** and **Gemma-3**, MLX often provides "day-zero" support for specific architectural quirks—such as the "DocTags" used in Granite-Docling for structured document parsing or the native multimodal interleaving of Gemma-3—that may take weeks or months to stabilize in the GGUF ecosystem.12 The mlx-vlm server exposes these specialized capabilities via an OpenAI-compatible endpoint, allowing them to sit alongside GGUF models in the LiteLLM routing table.15

## ---

**3\. Deep Analysis of Target Models and Architectural Implications**

The selection of models for this research—Qwen3-VL, DeepSeek-V3.1, Gemma-3, GPT-OSS, and Granite-Docling—represents a deliberate cross-section of modern AI capabilities, ranging from reasoning and vision to massive-scale generalist knowledge. Each imposes unique infrastructure requirements.

### **3.1 DeepSeek-V3.1: The Mixture-of-Experts Reasoning Engine**

**DeepSeek-V3.1** represents a significant evolution in open-weights reasoning, utilizing a Mixture-of-Experts (MoE) architecture. Unlike dense models where every parameter is active for every token, MoE models route tokens to specific "expert" neural networks.  
Infrastructure Implications:  
The llama.cpp backend (utilized via Llama-Swap) is particularly adept at handling MoE architectures on consumer hardware. It implements "expert offloading," where the gating network and active experts can be dynamically loaded into VRAM while inactive experts reside in system RAM. This is crucial for DeepSeek-V3.1, which may have a massive total parameter count but a relatively small "active" parameter count during inference.17  
Thinking Mode Configuration:  
DeepSeek-V3.1 introduces a "thinking" capability similar to OpenAI's o1, where the model generates internal chain-of-thought tokens enclosed in specific delimiters (e.g., \<think\>...\</think\>) before producing the final answer.

* **Implication for Llama-Server:** The configuration must ensure that the context window is sufficiently large (e.g., 32k or 64k tokens) to accommodate this verbose internal reasoning process without truncating the final output.  
* **Implication for Observability:** In our Langfuse configuration, we must decide whether to expose these thinking tokens to the end-user or scrub them. The report recommends logging them for debugging in Langfuse but potentially suppressing them in the final client response if a polished user experience is desired.17

### **3.2 GPT-OSS (120B): Managing Massive Scale**

**GPT-OSS** acts as the heavyweight generalist in this stack. With 120 billion parameters, it is the most resource-intensive component.  
Infrastructure Implications:  
Even with 4-bit quantization (Q4\_K\_M), this model requires substantial memory bandwidth.

* **Sharding and Loading:** Llama-Swap's configuration for this model must utilize llama-server's memory-mapping (mmap) features effectively. We utilize the gpt-oss-120b-GGUF quantization which is optimized to fit on single high-memory nodes (like Mac Studio with 128GB RAM or dual H100s).  
* **Latency Management:** The startup time for loading a 70GB+ file into memory is non-trivial (often 10-30 seconds). Llama-Swap's health\_check\_timeout must be aggressively tuned (e.g., increased to 300 seconds) to prevent the gateway from timing out the request while the model loads.8

### **3.3 Qwen3-VL: High-Resolution Vision Integration**

**Qwen3-VL** is a vision-language model that excels at OCR and visual reasoning.  
Infrastructure Implications:  
Unlike standard LLMs, Qwen3-VL requires a separate vision encoder (projector) to process image inputs before they are fed into the language model.

* **The mmproj Argument:** Standard GGUF files for LLMs integrate embeddings and weights. For Qwen-VL models in llama.cpp, the vision projector is often distributed as a separate file (e.g., mmproj-model-f16.gguf). The Llama-Swap command template *must* explicitly include the \--mmproj flag pointing to this file. Failure to do so results in a text-only model that hallucinates when presented with image tokens.19  
* **Resolution handling:** Qwen3-VL supports dynamic resolution. The llama-server configuration should enable sufficiently high context limits to account for the variable number of image tokens generated by high-resolution inputs.21

### **3.4 Granite-Docling: Specialized Document Parsing**

**Granite-Docling** is not a general-purpose chat model but a specialized VLM designed to convert document layouts (PDFs, scans) into structured Markdown or JSON.  
**Infrastructure Implications:**

* **MLX Preference:** We deploy this on **MLX-VLM** rather than llama.cpp. The primary reason is the "DocTags" architecture. Granite-Docling uses a specific vocabulary of tags to denote document structure. The MLX implementation (ibm-granite/granite-docling-258M-mlx) includes optimized handling for these tags and the specific SigLIP2 vision encoder used by Granite, which ensures the structured output is generated correctly without the post-processing friction often encountered with generic GGUF loaders.12

### **3.5 Gemma-3: Native Multimodal Interleaving**

**Gemma-3** is Google's latest open model, featuring native multimodal capabilities trained from scratch rather than grafted on.  
Infrastructure Implications:  
We utilize MLX-VLM for Gemma-3 to leverage the unified memory architecture for handling interleaved image and text sequences. The mlx-vlm server supports the OpenAI chat format where content is an array of text and image objects. The server automatically handles the tokenization of images using Gemma-3's specialized SigLIP-based encoder, which is critical for maintaining the model's performance on tasks requiring fine-grained visual grounding.14

## ---

**4\. Cloud Integration Strategy: Z.AI (GLM-4.6v & GLM-TTS)**

While local models offer privacy and cost efficiency, a robust gateway requires a fallback to state-of-the-art cloud models for tasks exceeding local capabilities or requiring modalities not locally available (high-quality TTS).

### **4.1 GLM-4.6v Integration**

**GLM-4.6v** is Zhipu AI's flagship multimodal model with a 128k context window and native function calling.

* **Gateway Configuration:** In LiteLLM, this is configured as a standard OpenAI-compatible provider but using the zai provider route. This ensures that specific Zhipu API quirks (such as the specific structure of tool calls or video understanding inputs) are handled by LiteLLM's adapter layer.5

### **4.2 GLM-TTS (GLM-4-Voice) Integration**

**GLM-4-Voice** represents an end-to-end speech model that can generate speech with high emotional fidelity and controllability.

* **Audio/Speech Endpoint Mapping:** LiteLLM provides a bridge that exposes an OpenAI-compatible /v1/audio/speech endpoint. Behind the scenes, we configure LiteLLM to route these requests to Z.AI's specific voice synthesis endpoints. This allows the client application to use standard OpenAI client libraries (client.audio.speech.create) while the generation is fulfilled by the advanced GLM-4-Voice model.5 This abstraction is critical for maintaining code portability.

## ---

**5\. Observability Architecture: MLflow and Langfuse**

A "black box" inference system is a liability in research and production. We implement a dual-layer observability stack.

### **5.1 Langfuse: Trace-Level Debugging**

**Langfuse** serves as the operational telemetry layer.

* **Mechanism:** We utilize LiteLLM's success\_callback and failure\_callback hooks to asynchronously push trace data to Langfuse.  
* **Value Add:** This provides a waterfall view of every interaction. For a DeepSeek-V3.1 query, Langfuse visualizes the latency of the gateway, the queue time in Llama-Swap, and the token generation speed. Crucially, it captures the *cost*. By configuring custom pricing in LiteLLM for local models (e.g., $0.00/token), we can contrast the theoretical cost of local inference against the actual cost of Z.AI API calls in a unified dashboard.3

### **5.2 MLflow: Experimentation and Registry**

**MLflow** serves as the analytical and governance layer.

* **Mechanism:** Using mlflow.litellm.autolog(), we automatically capture input prompts, configuration parameters (temperature, top\_p), and generated outputs into the MLflow Tracking Server.  
* **Value Add:** This is essential for prompt engineering and model comparison. We can run a validation set of 100 complex queries against both **GPT-OSS** and **DeepSeek-V3.1**, and MLflow will provide a comparative view of metric performance (e.g., answer length, semantic similarity if evaluators are configured), creating a permanent artifact of the experiment.4

## ---

**6\. Implementation Blueprint: Folder Structure and Configurations**

To implement this architecture effectively, a rigorous directory structure is required to separate large binary weights from configuration files and runtime scripts.

### **6.1 Directory Hierarchy**

We define a root directory /opt/ai-gateway to house the entire stack.  
/opt/ai-gateway/  
├── configs/  
│ ├── litellm/  
│ │ └── proxy\_config.yaml \# Master Gateway Config  
│ ├── llama-swap/  
│ │ └── swap\_config.yaml \# Llama-Swap Routing Config  
│ └── mlx/  
│ └── launch\_settings.json \# MLX Server Env Settings  
├── models/  
│ ├── gguf/  
│ │ ├── qwen3-vl/  
│ │ │ ├── Qwen3-VL-Instruct-Q4\_K\_M.gguf  
│ │ │ └── mmproj-Qwen3-VL-Instruct-f16.gguf  
│ │ ├── deepseek-v3.1/  
│ │ │ └── DeepSeek-V3.1-Terminus-Q4\_K\_M.gguf  
│ │ └── gpt-oss/  
│ │ └── GPT-OSS-120B-Q3\_K\_M.gguf-split-a  
│ │ └── GPT-OSS-120B-Q3\_K\_M.gguf-split-b  
│ └── mlx/  
│ ├── granite-docling/ \# HF Snapshot  
│ └── gemma-3/ \# HF Snapshot  
├── logs/  
│ ├── litellm/  
│ ├── llama-swap/  
│ └── mlx/  
├── scripts/  
│ ├── install\_dependencies.sh  
│ ├── download\_models.sh  
│ ├── start\_backend\_mlx.sh  
│ ├── start\_backend\_swap.sh  
│ └── start\_gateway.sh  
└──.env \# Secrets (ZAI\_API\_KEY, MLFLOW\_TRACKING\_URI)

### **6.2 Llama-Swap Configuration (swap\_config.yaml)**

This configuration dictates how llama-swap manages the llama-server processes. Note the specific command templates for different model types.

YAML

\# /opt/ai-gateway/configs/llama-swap/swap\_config.yaml

\# Global Server Configuration  
host: "127.0.0.1"  
port: 8081                     \# Internal port for the swap proxy  
health\_check\_timeout: 300      \# 5 minutes: Essential for 120B model loading times

\# Model Definitions  
models:  
  \# \--- GPT-OSS (120B) \---  
  \# Strategy: Maximize memory mapping, aggressive offload  
  "gpt-oss-120b":  
    cmd: \>  
      /usr/local/bin/llama-server  
      \--model /opt/ai-gateway/models/gguf/gpt-oss/GPT-OSS-120B-Q3\_K\_M.gguf  
      \--port ${PORT}  
      \--ctx-size 8192  
      \--n-gpu-layers 99         \# Attempt full offload  
      \--threads 16              \# High CPU thread count for parts not on GPU  
      \--batch-size 512  
      \--flash-attn              \# Required for reasonable speed

  \# \--- DeepSeek-V3.1 (Reasoning/MoE) \---  
  \# Strategy: High context for "thinking" tokens  
  "deepseek-v3.1":  
    cmd: \>  
      /usr/local/bin/llama-server  
      \--model /opt/ai-gateway/models/gguf/deepseek-v3.1/DeepSeek-V3.1-Terminus-Q4\_K\_M.gguf  
      \--port ${PORT}  
      \--ctx-size 65536          \# 64k context for reasoning chains  
      \--n-gpu-layers 99  
      \--cache-type-k f16        \# Precision for Key cache to maintain reasoning quality  
      \--flash-attn

  \# \--- Qwen3-VL (Vision) \---  
  \# Strategy: Explicit vision projector (mmproj) loading  
  "qwen3-vl":  
    cmd: \>  
      /usr/local/bin/llama-server  
      \--model /opt/ai-gateway/models/gguf/qwen3-vl/Qwen3-VL-Instruct-Q4\_K\_M.gguf  
      \--mmproj /opt/ai-gateway/models/gguf/qwen3-vl/mmproj-Qwen3-VL-Instruct-f16.gguf  
      \--port ${PORT}  
      \--ctx-size 16384          \# Buffer for high-res image tokens  
      \--n-gpu-layers 99

\# No "groups" defined to enforce exclusive execution (save VRAM)

**Table 1: Llama-Swap Parameter Logic**

| Parameter | Value Strategy | Justification |
| :---- | :---- | :---- |
| health\_check\_timeout | 300 | Large models (GPT-OSS 120B) take \>60s to load from SSD to RAM. Default timeout (60s) would cause premature failures.11 |
| \--mmproj | Path to file | **Critical for Qwen3-VL**. Without this, the server launches in text-only mode and fails on image input.19 |
| \--ctx-size | 65536 | **Critical for DeepSeek**. DeepSeek's "thinking" process consumes thousands of tokens before answering. Standard 4k windows are insufficient.17 |

### **6.3 MLX-VLM Server Script (start\_backend\_mlx.sh)**

Since mlx-vlm runs as a Python module, we wrap it in a shell script to manage environment variables and port binding.

Bash

\#\!/bin/bash  
\# /opt/ai-gateway/scripts/start\_backend\_mlx.sh

source /opt/ai-gateway/venv/bin/activate

\# Define Host/Port for the MLX Server  
export HOST="127.0.0.1"  
export PORT="8082"

echo "Initializing MLX-VLM Server on $HOST:$PORT..."  
echo "Serving models from: /opt/ai-gateway/models/mlx"

\# Launch the server module.   
\# Note: MLX-VLM server typically loads models dynamically based on the API request.  
\# We ensure the HuggingFace cache or local paths are accessible.  
python3 \-m mlx\_vlm.server \\  
    \--host $HOST \\  
    \--port $PORT \\  
    \--log-level INFO

### **6.4 LiteLLM Gateway Configuration (proxy\_config.yaml)**

This is the central nervous system. It routes requests to either localhost:8081 (Llama-Swap), localhost:8082 (MLX), or the public internet (Z.AI).

YAML

\# /opt/ai-gateway/configs/litellm/proxy\_config.yaml

general\_settings:  
  master\_key: sk-admin-gateway-key  
  alerting: \["slack"\]

litellm\_settings:  
  \# Dual Observability Pipeline  
  success\_callback: \["langfuse", "mlflow"\]  
  failure\_callback: \["langfuse", "mlflow"\]  
  \# Specific Z.AI settings to handle GLM quirks  
  json\_logs: true

environment\_variables:  
  \# Loaded from system env or.env file  
  ZAI\_API\_KEY: "os.environ/ZAI\_API\_KEY"  
  LANGFUSE\_PUBLIC\_KEY: "os.environ/LANGFUSE\_PUBLIC\_KEY"  
  LANGFUSE\_SECRET\_KEY: "os.environ/LANGFUSE\_SECRET\_KEY"  
  LANGFUSE\_HOST: "https://cloud.langfuse.com"  
  MLFLOW\_TRACKING\_URI: "http://localhost:5000"

model\_list:  
  \# \==============================================  
  \# 1\. GGUF Backends (via Llama-Swap Port 8081\)  
  \# \==============================================  
  \- model\_name: gpt-oss  
    litellm\_params:  
      model: openai/gpt-oss-120b        \# Matches Llama-Swap ID  
      api\_base: http://127.0.0.1:8081/v1  
      api\_key: sk-local  
      timeout: 600                      \# Extended timeout for 120B

  \- model\_name: deepseek-reasoner  
    litellm\_params:  
      model: openai/deepseek-v3.1  
      api\_base: http://127.0.0.1:8081/v1  
      api\_key: sk-local

  \- model\_name: qwen-vision  
    litellm\_params:  
      model: openai/qwen3-vl  
      api\_base: http://127.0.0.1:8081/v1  
      api\_key: sk-local

  \# \==============================================  
  \# 2\. MLX Backends (via MLX Server Port 8082\)  
  \# \==============================================  
  \# Note: The 'model' param here maps to the HF path expected by MLX  
  \- model\_name: granite-docling  
    litellm\_params:  
      model: openai/ibm-granite/granite-docling-258M-mlx  
      api\_base: http://127.0.0.1:8082/v1  
      api\_key: sk-local

  \- model\_name: gemma-3  
    litellm\_params:  
      model: openai/google/gemma-3-27b-it-mlx  
      api\_base: http://127.0.0.1:8082/v1  
      api\_key: sk-local

  \# \==============================================  
  \# 3\. Cloud Backends (Z.AI)  
  \# \==============================================  
  \- model\_name: glm-4-plus  
    litellm\_params:  
      model: zai/glm-4.6v               \# Vision-capable cloud model  
      \# API Key auto-injected from env

  \- model\_name: zai-speech  
    litellm\_params:  
      model: zai/glm-4-voice            \# Maps to Zhipu Voice endpoint  
      \# LiteLLM handles the conversion from /audio/speech

## ---

**7\. Operational Workflow and Lifecycle Management**

### **7.1 The Request Lifecycle**

1. **Ingestion:** A client (e.g., a Python script using openai.Client) sends a request to http://localhost:4000/v1/chat/completions with model="gpt-oss".  
2. **Gateway Processing:** LiteLLM authenticates the master\_key, logs the incoming request to Langfuse (status: STARTED), and resolves the routing table. It identifies gpt-oss as a proxy to http://127.0.0.1:8081.  
3. **Swap Orchestration:** Llama-Swap receives the request. It checks active processes. If qwen3-vl is running, it issues a SIGTERM to it, waits for shutdown, and then executes the cmd defined for gpt-oss-120b.  
4. **Inference:** The llama-server process starts, loads the 70GB GGUF file (taking \~30s), and begins token generation.  
5. **Telemetry:** As tokens stream back, LiteLLM updates the trace. Upon completion, it logs the full interaction to MLflow and finalizes the Langfuse trace with token counts and calculated costs.

### **7.2 Handling Z.AI Audio Generation**

For audio generation using GLM-TTS, the workflow differs slightly.

* **Client Call:** The client calls client.audio.speech.create(model="zai-speech", input="Hello world").  
* **Translation:** LiteLLM recognizes the audio/speech endpoint and the zai provider. It constructs the specific JSON payload required by Zhipu's GLM-4-Voice API (https://open.bigmodel.cn/api/paas/v4/chat/completions with specific audio params).27  
* **Response:** Z.AI returns a base64 encoded audio blob or a URL. LiteLLM decodes this and streams the binary audio back to the client, preserving the OpenAI SDK experience.

### **7.3 Troubleshooting Common Failure Modes**

The "Mmproj" Mismatch: A frequent error with Qwen3-VL in Llama-Swap is the model loading but failing on image input. This is invariably due to the cmd string missing the \--mmproj flag. The architecture forces this flag in the swap\_config.yaml.  
The "Thinking" Timeout: DeepSeek-V3.1 in reasoning mode can generate thousands of hidden tokens. If the client (LiteLLM) has a default timeout (e.g., 60s), the request will fail before the first visible token is produced. The LiteLLM config explicitly sets timeout: 600 for these reasoning models to mitigate this.

## **8\. Conclusion**

This report demonstrates that a unified, hybrid inference gateway is not only feasible but necessary for leveraging the full spectrum of modern AI capabilities. By combining the resource agility of **Llama-Swap**, the hardware-specific optimizations of **MLX-VLM**, and the cloud scalability of **Z.AI**, we create an infrastructure that is greater than the sum of its parts.  
The integration of **Langfuse** and **MLflow** elevates this system from a set of disjointed scripts to a managed platform, providing the visibility required to optimize costs (shifting load from cloud to local) and improve performance (tuning quantization and offloading). This architecture provides a blueprint for organizations to deploy privacy-preserving, high-performance AI services that remain adaptable to the rapidly evolving model landscape.  
**End of Report**

#### **Works cited**

1. llama-swap command \- github.com/LM4eu/llama-swap \- Go Packages, accessed December 10, 2025, [https://pkg.go.dev/github.com/LM4eu/llama-swap](https://pkg.go.dev/github.com/LM4eu/llama-swap)  
2. How to Run Multiple LLMs Locally Using Llama-Swap on a Single Server \- KDnuggets, accessed December 10, 2025, [https://www.kdnuggets.com/how-to-run-multiple-llms-locally-using-llama-swap-on-a-single-server](https://www.kdnuggets.com/how-to-run-multiple-llms-locally-using-llama-swap-on-a-single-server)  
3. Open Source Observability for LiteLLM Proxy \- Langfuse, accessed December 10, 2025, [https://langfuse.com/integrations/gateways/litellm](https://langfuse.com/integrations/gateways/litellm)  
4. MLflow \- OSS LLM Observability and Evaluation \- LiteLLM Docs, accessed December 10, 2025, [https://docs.litellm.ai/docs/observability/mlflow](https://docs.litellm.ai/docs/observability/mlflow)  
5. Z.AI (Zhipu AI) \- LiteLLM Docs, accessed December 10, 2025, [https://docs.litellm.ai/docs/providers/zai](https://docs.litellm.ai/docs/providers/zai)  
6. Callbacks \- LiteLLM Docs, accessed December 10, 2025, [https://docs.litellm.ai/docs/observability/callbacks](https://docs.litellm.ai/docs/observability/callbacks)  
7. audio/speech \- LiteLLM Docs, accessed December 10, 2025, [https://docs.litellm.ai/docs/text\_to\_speech](https://docs.litellm.ai/docs/text_to_speech)  
8. unsloth/gpt-oss-120b-GGUF Free Chat Online \- Skywork.ai, accessed December 10, 2025, [https://skywork.ai/blog/models/unsloth-gpt-oss-120b-gguf-free-chat-online-skywork-ai/](https://skywork.ai/blog/models/unsloth-gpt-oss-120b-gguf-free-chat-online-skywork-ai/)  
9. unsloth/gpt-oss-120b-GGUF \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/unsloth/gpt-oss-120b-GGUF](https://huggingface.co/unsloth/gpt-oss-120b-GGUF)  
10. mostlygeek/llama-swap: Reliable model swapping for any local OpenAI/Anthropic compatible server \- llama.cpp, vllm, etc \- GitHub, accessed December 10, 2025, [https://github.com/mostlygeek/llama-swap](https://github.com/mostlygeek/llama-swap)  
11. llama-swap command \- github.com/mostlygeek/llama-swap \- Go Packages, accessed December 10, 2025, [https://pkg.go.dev/github.com/mostlygeek/llama-swap](https://pkg.go.dev/github.com/mostlygeek/llama-swap)  
12. ibm-granite/granite-docling-258M-mlx \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/ibm-granite/granite-docling-258M-mlx](https://huggingface.co/ibm-granite/granite-docling-258M-mlx)  
13. IBM Granite-Docling: End-to-end document understanding with one tiny model, accessed December 10, 2025, [https://www.ibm.com/new/announcements/granite-docling-end-to-end-document-conversion](https://www.ibm.com/new/announcements/granite-docling-end-to-end-document-conversion)  
14. unsloth/gemma-3-27b-it-GGUF \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/unsloth/gemma-3-27b-it-GGUF](https://huggingface.co/unsloth/gemma-3-27b-it-GGUF)  
15. cubist38/mlx-openai-server: A high-performance API server that provides OpenAI-compatible endpoints for MLX models. Developed using Python and powered by the FastAPI framework, it provides an efficient, scalable, and user-friendly solution for running MLX-based vision and language models locally with an OpenAI-compatible \- GitHub, accessed December 10, 2025, [https://github.com/cubist38/mlx-openai-server](https://github.com/cubist38/mlx-openai-server)  
16. MLX-VLM is a package for inference and fine-tuning of Vision Language Models (VLMs) on your Mac using MLX. \- GitHub, accessed December 10, 2025, [https://github.com/Blaizzy/mlx-vlm](https://github.com/Blaizzy/mlx-vlm)  
17. unsloth/DeepSeek-V3.1-GGUF \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/unsloth/DeepSeek-V3.1-GGUF](https://huggingface.co/unsloth/DeepSeek-V3.1-GGUF)  
18. unsloth/DeepSeek-V3.1-Terminus \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/unsloth/DeepSeek-V3.1-Terminus](https://huggingface.co/unsloth/DeepSeek-V3.1-Terminus)  
19. Qwen 3 VL merged into llama.cpp\! : r/LocalLLaMA \- Reddit, accessed December 10, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1ok2lht/qwen\_3\_vl\_merged\_into\_llamacpp/](https://www.reddit.com/r/LocalLLaMA/comments/1ok2lht/qwen_3_vl_merged_into_llamacpp/)  
20. Feature Request: Qwen 2.5 VL \#11483 \- ggml-org/llama.cpp \- GitHub, accessed December 10, 2025, [https://github.com/ggml-org/llama.cpp/issues/11483](https://github.com/ggml-org/llama.cpp/issues/11483)  
21. Qwen/Qwen3-VL-30B-A3B-Instruct-GGUF \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/Qwen/Qwen3-VL-30B-A3B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen3-VL-30B-A3B-Instruct-GGUF)  
22. Granite Docling WebGPU: State-of-the-art document parsing 100% locally in your browser., accessed December 10, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1o0php3/granite\_docling\_webgpu\_stateoftheart\_document/](https://www.reddit.com/r/LocalLLaMA/comments/1o0php3/granite_docling_webgpu_stateoftheart_document/)  
23. unsloth/gemma-3-27b-it at main \- Hugging Face, accessed December 10, 2025, [https://huggingface.co/unsloth/gemma-3-27b-it/tree/main](https://huggingface.co/unsloth/gemma-3-27b-it/tree/main)  
24. Gemma-3n-E2B-it for on‑device LLM applications \- SecondState.io, accessed December 10, 2025, [https://www.secondstate.io/articles/gemma-3n-e2b/](https://www.secondstate.io/articles/gemma-3n-e2b/)  
25. GLM 4.6V \- API, Providers, Stats \- OpenRouter, accessed December 10, 2025, [https://openrouter.ai/z-ai/glm-4.6v](https://openrouter.ai/z-ai/glm-4.6v)  
26. GLM-4.6V \- Z.AI DEVELOPER DOCUMENT, accessed December 10, 2025, [https://docs.z.ai/guides/vlm/glm-4.6v](https://docs.z.ai/guides/vlm/glm-4.6v)  
27. GLM-4-Voice \- ZHIPU AI OPEN PLATFORM, accessed December 10, 2025, [https://open.bigmodel.cn/dev/api/rtav/GLM-4-Voice](https://open.bigmodel.cn/dev/api/rtav/GLM-4-Voice)  
28. Langfuse \- Logging LLM Input/Output \- LiteLLM Docs, accessed December 10, 2025, [https://docs.litellm.ai/docs/observability/langfuse\_integration](https://docs.litellm.ai/docs/observability/langfuse_integration)