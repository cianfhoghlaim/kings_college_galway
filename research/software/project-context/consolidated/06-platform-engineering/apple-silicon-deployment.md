# Apple Silicon LLM Deployment

## Executive Summary

This document details deployment strategies for running local LLMs on Apple Silicon Macs, leveraging MLX for native Metal acceleration and llama.cpp for cross-platform compatibility. The patterns enable low-latency inference without cloud dependencies.

---

## 1. Hardware Capabilities

### 1.1 Apple Silicon Memory Architecture

| Feature | Benefit for LLM |
|---------|-----------------|
| **Unified Memory** | GPU/CPU share same RAM pool |
| **High Bandwidth** | 200-400 GB/s memory bandwidth |
| **Metal Acceleration** | Native GPU compute via MLX |
| **Efficiency Cores** | Background tasks don't impact inference |

### 1.2 Model Size Guidelines

| Mac Model | RAM | Recommended Model Size |
|-----------|-----|----------------------|
| M1/M2 (8GB) | 8GB | 7B Q4 |
| M1/M2 Pro (16GB) | 16GB | 13B Q4, 7B Q8 |
| M1/M2 Max (32GB) | 32GB | 34B Q4, 13B Q8 |
| M1/M2 Ultra (64GB+) | 64GB+ | 70B Q4, 34B Q8 |
| M3 Max (128GB) | 128GB | 70B Q8, 2x70B Q4 |

### 1.3 Quantization Impact

| Quantization | Memory Reduction | Quality Impact |
|--------------|------------------|----------------|
| **Q8** | 50% | Minimal |
| **Q6_K** | 60% | Very low |
| **Q4_K_M** | 75% | Low |
| **Q4_0** | 75% | Moderate |

---

## 2. MLX Framework

### 2.1 Installation

```bash
# Create virtual environment
python -m venv ~/.venvs/mlx-llm
source ~/.venvs/mlx-llm/bin/activate

# Install MLX and dependencies
pip install mlx mlx-lm transformers huggingface_hub
```

### 2.2 Model Download

```python
from huggingface_hub import snapshot_download

# Download quantized model
model_path = snapshot_download(
    repo_id="mlx-community/Qwen2.5-7B-Instruct-4bit",
    local_dir="./models/qwen2.5-7b-instruct-4bit"
)
```

### 2.3 Inference Script

```python
from mlx_lm import load, generate

# Load model and tokenizer
model, tokenizer = load("./models/qwen2.5-7b-instruct-4bit")

# Generate response
prompt = "Explain the Leaving Certificate points system."
response = generate(
    model,
    tokenizer,
    prompt=prompt,
    max_tokens=512,
    temp=0.7
)
print(response)
```

### 2.4 OpenAI-Compatible Server

```python
# mlx_server.py
from mlx_lm import load, generate
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()
model, tokenizer = load("./models/qwen2.5-7b-instruct-4bit")

class ChatRequest(BaseModel):
    model: str
    messages: list[dict]
    max_tokens: int = 512
    temperature: float = 0.7

@app.post("/v1/chat/completions")
async def chat_completions(request: ChatRequest):
    # Format messages
    prompt = tokenizer.apply_chat_template(
        request.messages,
        tokenize=False,
        add_generation_prompt=True
    )

    response = generate(
        model,
        tokenizer,
        prompt=prompt,
        max_tokens=request.max_tokens,
        temp=request.temperature
    )

    return {
        "id": "chatcmpl-xxx",
        "object": "chat.completion",
        "choices": [{
            "index": 0,
            "message": {
                "role": "assistant",
                "content": response
            },
            "finish_reason": "stop"
        }]
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8081)
```

---

## 3. llama.cpp / llama-server

### 3.1 Installation

```bash
# Clone and build
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp

# Build with Metal support (automatic on macOS)
make -j

# Or use cmake for more control
mkdir build && cd build
cmake .. -DGGML_METAL=ON
cmake --build . --config Release
```

### 3.2 Model Conversion

```bash
# Convert HuggingFace model to GGUF
python convert_hf_to_gguf.py \
  ./models/Qwen2.5-7B-Instruct \
  --outfile ./models/qwen2.5-7b-instruct.gguf

# Quantize
./llama-quantize \
  ./models/qwen2.5-7b-instruct.gguf \
  ./models/qwen2.5-7b-instruct-Q4_K_M.gguf \
  Q4_K_M
```

### 3.3 Server Launch

```bash
# Basic server
./llama-server \
  -m ./models/qwen2.5-7b-instruct-Q4_K_M.gguf \
  --host 0.0.0.0 \
  --port 8081 \
  --n-gpu-layers 99 \
  --ctx-size 8192

# With multimodal (vision) support
./llama-server \
  -m ./models/Qwen2.5-VL-7B-Q4_K_M.gguf \
  --mmproj ./models/qwen2.5-vl-mmproj.gguf \
  --host 0.0.0.0 \
  --port 8081 \
  --n-gpu-layers 99
```

### 3.4 Server Options Reference

| Option | Purpose | Recommended |
|--------|---------|-------------|
| `--n-gpu-layers` | Layers on GPU | 99 (all) |
| `--ctx-size` | Context window | 8192 |
| `--batch-size` | Batch processing | 512 |
| `--threads` | CPU threads | Physical cores |
| `--flash-attn` | Flash attention | Enable if supported |

---

## 4. Vision Model Deployment (VLM)

### 4.1 Qwen2.5-VL with llama.cpp

```bash
# Download model and projector
huggingface-cli download Qwen/Qwen2.5-VL-7B-Instruct-GGUF \
  qwen2.5-vl-7b-instruct-q4_k_m.gguf \
  qwen2.5-vl-7b-instruct-mmproj.gguf \
  --local-dir ./models/qwen2.5-vl

# Launch server
./llama-server \
  -m ./models/qwen2.5-vl/qwen2.5-vl-7b-instruct-q4_k_m.gguf \
  --mmproj ./models/qwen2.5-vl/qwen2.5-vl-7b-instruct-mmproj.gguf \
  --host 0.0.0.0 \
  --port 8082 \
  --n-gpu-layers 99
```

### 4.2 Vision API Usage

```python
import httpx
import base64

def analyze_image(image_path: str, prompt: str) -> str:
    """Send image to VLM for analysis."""
    with open(image_path, "rb") as f:
        image_data = base64.b64encode(f.read()).decode()

    response = httpx.post(
        "http://localhost:8082/v1/chat/completions",
        json={
            "model": "qwen2.5-vl",
            "messages": [{
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{image_data}"
                        }
                    },
                    {
                        "type": "text",
                        "text": prompt
                    }
                ]
            }],
            "max_tokens": 1024
        }
    )

    return response.json()["choices"][0]["message"]["content"]

# Extract text from exam paper
result = analyze_image(
    "exam_paper_2024.jpg",
    "Extract all questions and their marks from this exam paper."
)
```

---

## 5. Service Management

### 5.1 launchd Configuration

```xml
<!-- ~/Library/LaunchAgents/com.local.llama-server.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.local.llama-server</string>

    <key>ProgramArguments</key>
    <array>
        <string>/usr/local/bin/llama-server</string>
        <string>-m</string>
        <string>/Users/username/models/qwen2.5-7b-instruct-Q4_K_M.gguf</string>
        <string>--host</string>
        <string>0.0.0.0</string>
        <string>--port</string>
        <string>8081</string>
        <string>--n-gpu-layers</string>
        <string>99</string>
    </array>

    <key>RunAtLoad</key>
    <true/>

    <key>KeepAlive</key>
    <true/>

    <key>StandardOutPath</key>
    <string>/tmp/llama-server.log</string>

    <key>StandardErrorPath</key>
    <string>/tmp/llama-server.err</string>
</dict>
</plist>
```

### 5.2 Service Control

```bash
# Load service
launchctl load ~/Library/LaunchAgents/com.local.llama-server.plist

# Unload service
launchctl unload ~/Library/LaunchAgents/com.local.llama-server.plist

# Check status
launchctl list | grep llama

# View logs
tail -f /tmp/llama-server.log
```

---

## 6. LiteLLM Integration

### 6.1 Configuration for Local Models

```yaml
# litellm-config.yaml
model_list:
  # Local MLX model
  - model_name: qwen-local
    litellm_params:
      model: openai/qwen2.5-7b-instruct
      api_base: "http://localhost:8081/v1"
      api_key: "sk-local"

  # Local VLM
  - model_name: qwen-vision
    litellm_params:
      model: openai/qwen2.5-vl-7b
      api_base: "http://localhost:8082/v1"
      api_key: "sk-local"

  # Cloud fallback
  - model_name: gpt-4o
    litellm_params:
      model: openai/gpt-4o
      api_key: os.environ/OPENAI_API_KEY

router_settings:
  routing_strategy: "latency-based-routing"
  fallbacks:
    - qwen-local: [gpt-4o]
    - qwen-vision: [gpt-4o]
```

### 6.2 Docker with Host Network (Mac)

```yaml
# docker-compose.yaml
services:
  litellm:
    image: ghcr.io/berriai/litellm:main-latest
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      # Point to host machine's llama-server
      - LOCAL_LLM_BASE=http://host.docker.internal:8081/v1
    volumes:
      - ./litellm-config.yaml:/app/config.yaml
    ports:
      - "4000:4000"
```

---

## 7. Performance Optimization

### 7.1 Memory Management

```bash
# Monitor memory usage
while true; do
  memory_pressure
  sleep 5
done

# Clear disk cache if needed (helps with model loading)
sudo purge
```

### 7.2 Thermal Management

```bash
# Check thermal status
sudo powermetrics --samplers smc -i 1 -n 1 | grep -i temp

# Reduce thermal throttling:
# - Ensure good ventilation
# - Use laptop stand for airflow
# - Consider active cooling for sustained loads
```

### 7.3 Batch Processing

```python
# For bulk inference, use batching
from mlx_lm import load, generate

model, tokenizer = load("./models/qwen2.5-7b-instruct-4bit")

# Process in batches
documents = ["doc1...", "doc2...", "doc3..."]
batch_size = 4

results = []
for i in range(0, len(documents), batch_size):
    batch = documents[i:i+batch_size]
    # Process batch (model-specific batching may vary)
    for doc in batch:
        result = generate(model, tokenizer, prompt=doc, max_tokens=256)
        results.append(result)
```

---

## 8. Model Recommendations

### 8.1 General Purpose

| Model | Size | Use Case |
|-------|------|----------|
| Qwen2.5-7B-Instruct | 7B | General chat, coding |
| Mistral-7B-Instruct | 7B | Efficient reasoning |
| Llama-3.2-8B-Instruct | 8B | Latest capabilities |
| Qwen2.5-14B-Instruct | 14B | Higher quality (32GB RAM) |

### 8.2 Vision Models

| Model | Size | Use Case |
|-------|------|----------|
| Qwen2.5-VL-7B | 7B | Document analysis |
| LLaVA-v1.6-7B | 7B | General vision |
| Pixtral-12B | 12B | High-quality vision (32GB) |

### 8.3 Specialized

| Model | Size | Use Case |
|-------|------|----------|
| OLMo-7B | 7B | Research, transparency |
| olmOCR-7B | 7B | OCR-specific tasks |
| DeepSeek-Coder-7B | 7B | Code generation |

---

## 9. Implementation Priorities

### Phase 1: Basic Setup
1. Install llama.cpp with Metal support
2. Download Q4_K_M quantized model
3. Launch server and test

### Phase 2: Service Integration
1. Create launchd service
2. Configure LiteLLM routing
3. Test with application clients

### Phase 3: Multi-Model
1. Add vision model
2. Configure llama-swap for auto-switching
3. Optimize memory usage

### Phase 4: Production
1. Set up monitoring
2. Configure cloud fallbacks
3. Document model update procedures

---

## References

- MLX: https://github.com/ml-explore/mlx
- MLX-LM: https://github.com/ml-explore/mlx-examples/tree/main/llms
- llama.cpp: https://github.com/ggerganov/llama.cpp
- MLX Community Models: https://huggingface.co/mlx-community
