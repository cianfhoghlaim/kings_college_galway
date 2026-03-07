# Apple Silicon Deployment for Document Intelligence

## Executive Summary

This document details deployment strategies for running Vision-Language Models and OCR systems natively on Apple Silicon (M1-M4), leveraging Unified Memory Architecture for optimal performance without Docker virtualization overhead.

---

## 1. The Apple Silicon Advantage

### 1.1 Unified Memory Architecture (UMA)

Traditional x86/CUDA workstations have a fundamental bottleneck:
- System RAM and GPU VRAM are separate pools
- PCIe bus transfer imposes latency
- Models either fit in VRAM or don't run effectively

Apple's UMA obliterates this distinction:
- CPU, GPU, and Neural Engine share single memory pool
- High-bandwidth memory access without data copying
- Can load 32B model + specialized OCR model + vector database simultaneously

### 1.2 Hardware Specifications

| Chip | Unified Memory | GPU Cores | Neural Engine | Recommended For |
|------|---------------|-----------|---------------|-----------------|
| M1 | 8-16GB | 7-8 | 16-core | Granite-Docling only |
| M2 | 8-24GB | 8-10 | 16-core | + olmOCR (Q4) |
| M3 | 8-36GB | 10-18 | 16-core | + Qwen2.5-VL-7B |
| M3 Max | 36-128GB | 30-40 | 16-core | Full fleet deployment |
| M4 | 16-32GB | 10-20 | 16-core | + Qwen2.5-VL-7B |
| M4 Max | 36-128GB | 40+ | 16-core | Qwen3-VL-32B |

---

## 2. The Docker Virtualization Barrier

### 2.1 Why Docker Fails on Mac for Inference

Docker Desktop on macOS uses a hypervisor (HyperKit/Apple Virtualization Framework) to run a Linux kernel:

```
Mac Host
    └── HyperKit/AVF Hypervisor
        └── Linux VM (isolated)
            └── Docker Container
                └── Model (cannot see Metal)
```

**The Problem:**
- Linux kernel is isolated from host hardware
- No standard for passing Metal API into Linux containers
- Containers see generic virtual CPU only
- Cannot access M-series GPU or Neural Engine

**Result:** Running VLMs in Docker on Mac forces CPU execution - 20-45 seconds per page vs <1 second native.

### 2.2 The vLLM Misconception

vLLM is mentioned in many repositories (including Docling), leading to confusion:

| Framework | Optimization Target | Mac Support |
|-----------|--------------------|--------------|
| **vLLM** | PagedAttention for NVIDIA CUDA | CPU only (experimental) |
| **MLX** | Apple Silicon Unified Memory | Native, optimal |
| **llama.cpp** | Universal GGUF quantization | Metal backend (excellent) |

**Seeing vLLM in a repo = can connect to Linux GPU server, NOT that it runs fast on Mac.**

---

## 3. The Inference Triad: MLX, Metal, GGUF

### 3.1 MLX Framework

Apple's official ML framework designed for Unified Memory:
- Lazy evaluation like JAX
- Efficient KV-cache management in UMA
- Superior token generation vs PyTorch MPS

**Best for:** Generative transformer workloads (Qwen-VL, Granite-Docling)

```bash
pip install mlx mlx-lm mlx-vlm
```

### 3.2 llama.cpp with Metal Backend

Highly mature Metal backend for GGUF models:
- Optimized kernels for quantized matrix multiplication (Q4_K_M, Q8_0)
- Precise control over layer offloading (`-ngl`)
- Supports vision models via mmproj

**Best for:** olmOCR, Qwen2.5-VL (GGUF format)

```bash
pip install "llama-cpp-python[server]" --extra-index-url \
    https://abetlen.github.io/llama-cpp-python/whl/metal
```

### 3.3 PyTorch MPS Backend

Fallback for models not yet ported to MLX:
- Supports vision encoders (ViT, SAM, NaViT)
- Requires bfloat16 patches for stability

**Best for:** DeepSeek-OCR, PaddleOCR-VL

```python
DEVICE = "mps" if torch.backends.mps.is_available() else "cpu"
DTYPE = torch.bfloat16  # Critical for Metal stability
```

---

## 4. Native Service Configuration

### 4.1 Docling with MLX (The Fast Parser)

Docling is the only tool with native MLX support out of the box.

**Installation:**
```bash
mkdir docling-native && cd docling-native
uv venv .venv --python 3.11
source .venv/bin/activate
pip install "docling[mlx]" docling-serve
```

**Run Service:**
```bash
export DOCLING_SERVE_PORT=5001
export DOCLING_SERVE_HOST=0.0.0.0
docling-serve run
```

**Performance:** M3 Max: <1 second per page

### 4.2 olmOCR via llama.cpp

**Model Download:**
```bash
huggingface-cli download richardyoung/olmOCR-2-7B-1025-GGUF \
    olmOCR-2-7B-1025-Q4_K_M.gguf --local-dir ./models/olmocr

huggingface-cli download richardyoung/olmOCR-2-7B-1025-GGUF \
    mmproj-olmOCR-2-7B-1025-vision.gguf --local-dir ./models/olmocr
```

**Launch Server:**
```bash
python -m llama_cpp.server \
    --model ./models/olmocr/olmOCR-2-7B-1025-Q4_K_M.gguf \
    --clip_model_path ./models/olmocr/mmproj-olmOCR-2-7B-1025-vision.gguf \
    --n_gpu_layers 99 \
    --chat_format chatml \
    --n_ctx 8192 \
    --port 8081 \
    --alias olmocr
```

**Critical:** `--n_ctx 8192` required for document parsing (default 2048 causes hallucinations)

### 4.3 Qwen2.5-VL via MLX

**Installation:**
```bash
uv venv .venv_mlx && source .venv_mlx/bin/activate
pip install mlx mlx-lm mlx-vlm huggingface_hub
```

**Launch Server:**
```bash
python -m mlx_lm.server \
    --model mlx-community/Qwen2.5-VL-7B-Instruct-4bit \
    --port 8082 \
    --log-level info
```

**For 32B model (64GB+ Mac):**
```bash
python -m mlx_lm.server \
    --model mlx-community/Qwen2.5-VL-32B-Instruct-4bit \
    --port 8082
```

### 4.4 DeepSeek-OCR via PyTorch/MPS

Custom FastAPI wrapper required due to CUDA dependencies:

**server_deepseek.py:**
```python
from fastapi import FastAPI, UploadFile, File
from transformers import AutoModel, AutoTokenizer
import torch
from PIL import Image
import io

app = FastAPI()

# MPS Device Strategy
DEVICE = "mps" if torch.backends.mps.is_available() else "cpu"
DTYPE = torch.bfloat16  # Force bfloat16 for Metal stability

# Load model
tokenizer = AutoTokenizer.from_pretrained(
    "deepseek-ai/DeepSeek-OCR",
    trust_remote_code=True
)
model = AutoModel.from_pretrained(
    "deepseek-ai/DeepSeek-OCR",
    trust_remote_code=True,
    torch_dtype=DTYPE
).to(DEVICE)
model.eval()

@app.post("/v1/ocr")
async def process_ocr(file: UploadFile = File(...)):
    image_bytes = await file.read()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

    with torch.no_grad():
        res = model.infer(
            tokenizer,
            image_file=image,
            mode="ocr",
            device=DEVICE,
            dtype=DTYPE
        )

    return {"text": res}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8083)
```

**Launch:**
```bash
pip install torch torchvision transformers fastapi uvicorn python-multipart timm einops
python server_deepseek.py
```

---

## 5. Orchestration Layer

### 5.1 LiteLLM Gateway

Normalizes disparate API signatures into OpenAI-compatible format:

**config.yaml:**
```yaml
model_list:
  # Qwen-VL via MLX
  - model_name: qwen-vl
    litellm_params:
      model: openai/qwen2.5-vl-32b-instruct
      api_base: "http://localhost:8082/v1"
      api_key: "sk-local-mlx"

  # olmOCR via llama.cpp
  - model_name: olmocr
    litellm_params:
      model: openai/olmocr
      api_base: "http://localhost:8081/v1"
      api_key: "sk-local-llama"

  # DeepSeek-OCR via custom FastAPI
  - model_name: deepseek-ocr
    litellm_params:
      model: openai/deepseek-ocr
      api_base: "http://localhost:8083/v1"
      api_key: "sk-local-ds"

# MCP Tool Servers
mcp_servers:
  docling_service:
    command: "uvx"
    args: ["docling-mcp-server"]

  marker_service:
    command: "python"
    args: ["simple_marker_mcp.py"]
    env:
      TORCH_DEVICE: "mps"

general_settings:
  master_key: "sk-master-secret"
```

**Launch:**
```bash
litellm --config config.yaml --port 4000
```

### 5.2 Llama-Swap (Model Router)

For swapping models to manage memory:

**config.yaml:**
```yaml
listen: :8080
models:
  - name: qwen-vl
    cmd: "llama-server -m /path/to/Qwen2.5-VL-7B-Q4_K_M.gguf --port 8081 --n-gpu-layers 99"

  - name: olmocr
    cmd: "llama-server -m /path/to/olmOCR-Q4_K_M.gguf --clip_model_path /path/to/mmproj.gguf --port 8081 --n-gpu-layers 99"
```

Use `ttl` (time-to-live) to unload idle models and free memory.

---

## 6. Memory Management

### 6.1 Fleet Memory Footprint

| Model | Format | Memory |
|-------|--------|--------|
| Qwen3-VL-32B (4-bit) | MLX | ~19GB |
| Qwen2.5-VL-7B (4-bit) | MLX | ~5GB |
| olmOCR-2-7B (Q4_K_M) | GGUF | ~5GB |
| DeepSeek-OCR (bfloat16) | PyTorch | ~6GB |
| Granite-Docling (MLX) | MLX | ~1GB |
| macOS System | - | ~4-6GB |

**Total for full fleet:** ~36-40GB

### 6.2 Memory Pressure Strategies

**On 32GB Mac:**
- Significant swap pressure with full fleet
- Use llama-swap with TTL to unload idle models
- Prioritize: Docling (always) → olmOCR (ingestion) → Qwen (reasoning)

**On 64GB+ Mac:**
- Full fleet runs entirely in Wired Memory
- Can run simultaneously without swapping

### 6.3 Model Unloading Pattern

```python
async def process_document(pdf_path):
    # Phase 1: Structure extraction (Docling always loaded)
    structure = await docling_service.convert(pdf_path)

    # Phase 2: Dense OCR (load olmOCR, unload if not needed)
    text = await olmocr_service.transcribe(pdf_path)
    await model_manager.unload('olmocr', ttl=300)  # Unload after 5min

    # Phase 3: Reasoning (load Qwen)
    analysis = await qwen_service.analyze(text, structure)

    return analysis
```

---

## 7. Quantization Guide

### 7.1 GGUF Formats

| Format | Bits | Memory (7B) | Perplexity | Use Case |
|--------|------|-------------|------------|----------|
| F16 | 16 | ~14GB | Baseline | If memory allows |
| Q8_0 | 8 | ~8GB | Negligible loss | Quality priority |
| Q6_K | 6 | ~6GB | Very slight loss | Good balance |
| **Q4_K_M** | 4 | ~4.5GB | ~1-2% | **Recommended** |
| Q4_0 | 4 | ~4GB | ~2-3% | Max compression |
| IQ2_XXS | 2 | ~2GB | Significant | Not recommended |

### 7.2 MLX Quantization

```bash
# Convert HuggingFace model to MLX 4-bit
python -m mlx_lm.convert \
    --hf-path Qwen/Qwen2.5-VL-7B-Instruct \
    --mlx-path ./mlx-qwen-4bit \
    --quantize \
    --q-bits 4
```

### 7.3 Performance Impact

**Qwen2.5-VL-7B on M3 Max:**

| Quantization | Tokens/Second | Memory |
|--------------|---------------|--------|
| 8-bit | ~25 t/s | ~8GB |
| 4-bit | ~45-50 t/s | ~5GB |

4-bit provides ~2x speedup due to reduced memory bandwidth pressure.

---

## 8. Troubleshooting

### 8.1 Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| DeepSeek OOM/Crash | autocast on MPS | Remove torch.autocast, force bfloat16 |
| PaddleOCR Kernel Panic | Custom ops on Metal | Use CPU: `device="cpu"` |
| Slow Qwen generation | Memory bandwidth saturated | Use 4-bit; close Adobe/Resolve |
| olmOCR hallucinations | Context overflow | Launch with `--n_ctx 8192` or higher |
| MCP timeout | Long processing | Use SSE transport; increase timeout |

### 8.2 MPS Stability Patches

**The bfloat16 Fix:**
```python
# Common error: RuntimeError: "slow_conv2d_cpu" not implemented for 'Half'
# Cause: FP16 conv layers not implemented in Metal

# Solution: Force bfloat16 (better Apple hardware support)
model = model.to(torch.bfloat16)

# Or in model loading:
model = AutoModel.from_pretrained(
    model_id,
    torch_dtype=torch.bfloat16,  # Not torch.float16
    device_map="mps"
)
```

### 8.3 Memory Debugging

```bash
# Monitor unified memory usage
sudo powermetrics --samplers gpu_power -i 1000

# Check memory pressure
memory_pressure

# View per-process GPU usage (Xcode Instruments or Activity Monitor)
```

---

## 9. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT APPLICATION                          │
│              (Cursor, Open WebUI, Custom Script)                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   LITELLM GATEWAY (Port 4000)                   │
│              Unified OpenAI-Compatible Interface                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ DOCLING (MLX) │   │ OLMOCR (GGUF) │   │  QWEN-VL (MLX)│
│   Port 5001   │   │   Port 8081   │   │   Port 8082   │
│   <1s/page    │   │   2-3s/page   │   │   3-5s/page   │
│  Structural   │   │   Dense OCR   │   │   Reasoning   │
└───────────────┘   └───────────────┘   └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              APPLE SILICON UNIFIED MEMORY                        │
│                    (Metal GPU + ANE)                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. Performance Summary

### 10.1 Speed Comparison: Native vs Docker

| Model | Native (MLX/Metal) | Docker CPU | Speedup |
|-------|-------------------|------------|---------|
| Granite-Docling | <1s | 3-5s | 3-5x |
| olmOCR-2-7B | 2-3s | 20-30s | 10x |
| Qwen2.5-VL-7B | 3-5s | 30-45s | 10x |
| DeepSeek-OCR | 2-3s | 15-25s | 7x |

### 10.2 Cost for 10,000 Pages

| Deployment | Token Cost | Energy Cost | Total |
|------------|------------|-------------|-------|
| Native Mac (MLX) | $0 | ~$0.50 | **$0.50** |
| Docker CPU (Mac) | $0 | ~$5.00 | **$5.00** |
| Cloud API | ~$15+ | N/A | **$15+** |

**Conclusion:** Native deployment on Apple Silicon achieves 10x speedup at 30x lower cost compared to alternatives.
