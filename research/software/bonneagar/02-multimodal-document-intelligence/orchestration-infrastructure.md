# Model Orchestration & Infrastructure

## Executive Summary

This document covers the orchestration layer for managing multiple VLM and OCR models, including LiteLLM gateway configuration, MCP (Model Context Protocol) integration, llama-swap routing, and vLLM serving strategies.

---

## 1. Orchestration Philosophy

### 1.1 The Multi-Model Reality

Document intelligence requires specialized models:
- **Structural extraction**: Granite-Docling (tables, layouts)
- **Dense OCR**: olmOCR (text transcription)
- **Visual reasoning**: Qwen-VL (diagram interpretation)
- **Mathematical extraction**: DeepSeek-OCR (LaTeX)

No single model excels at everything. Orchestration is essential.

### 1.2 Design Principles

1. **Unified Interface**: Clients shouldn't know which backend serves requests
2. **Smart Routing**: Route by content type to optimal model
3. **Resource Management**: Swap models to manage memory
4. **Tool Integration**: Deterministic tools (Docling, Marker) as MCP servers

---

## 2. LiteLLM Gateway

### 2.1 Purpose

LiteLLM functions as a localized proxy server:
- Normalizes disparate API signatures into OpenAI-compatible format
- Manages MCP servers for tool integration
- Enables centralized logging and rate limiting

### 2.2 Configuration

**config.yaml:**
```yaml
model_list:
  # Vision-Language Models
  - model_name: qwen-vl
    litellm_params:
      model: openai/qwen2.5-vl-32b-instruct
      api_base: "http://localhost:8082/v1"
      api_key: "sk-local-mlx"
      max_tokens: 4096

  - model_name: qwen-vl-7b
    litellm_params:
      model: openai/qwen2.5-vl-7b-instruct
      api_base: "http://localhost:8082/v1"
      api_key: "sk-local-mlx"

  # OCR Specialists
  - model_name: olmocr
    litellm_params:
      model: openai/olmocr
      api_base: "http://localhost:8081/v1"
      api_key: "sk-local-llama"

  - model_name: deepseek-ocr
    litellm_params:
      model: openai/deepseek-ocr
      api_base: "http://localhost:8083/v1"
      api_key: "sk-local-ds"

  # Cloud Fallbacks
  - model_name: glm-4.5v
    litellm_params:
      model: zhipu/glm-4.5v
      api_key: "os.environ/ZHIPU_API_KEY"

  # Text-Only Models
  - model_name: local-llama
    litellm_params:
      model: openai/llama-3.3-70b
      api_base: "http://localhost:8080/v1"
      api_key: "sk-local"

# MCP Tool Registration
mcp_servers:
  docling:
    command: "uvx"
    args: ["docling-mcp-server"]
    description: "PDF to Markdown conversion"

  marker:
    command: "python"
    args: ["marker_mcp_server.py"]
    env:
      TORCH_DEVICE: "mps"
    description: "Scientific PDF parsing"

# Router Configuration
router_settings:
  routing_strategy: "simple-shuffle"  # or "least-busy", "latency-based"
  num_retries: 3
  timeout: 300

  # Model aliases for fallback chains
  model_group_alias:
    vision-model:
      - qwen-vl
      - glm-4.5v  # Cloud fallback
    ocr-model:
      - olmocr
      - deepseek-ocr

general_settings:
  master_key: "sk-master-secret"
  database_url: "postgresql://localhost/litellm"
  alerting: ["slack"]
```

### 2.3 Launching

```bash
# Install
pip install litellm[proxy]

# Run with config
litellm --config config.yaml --port 4000

# Or with Docker (for non-ML components)
docker run -d \
  -p 4000:4000 \
  -v $(pwd)/config.yaml:/app/config.yaml \
  ghcr.io/berriai/litellm:main-latest \
  --config /app/config.yaml
```

### 2.4 Client Usage

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:4000/v1",
    api_key="sk-master-secret"
)

# Automatically routes to configured backend
response = client.chat.completions.create(
    model="qwen-vl",
    messages=[
        {
            "role": "user",
            "content": [
                {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}},
                {"type": "text", "text": "Extract all text from this document"}
            ]
        }
    ]
)
```

---

## 3. Model Context Protocol (MCP)

### 3.1 Why MCP?

MCP standardizes how AI models consume tools:
- **Docling**: Not a chatbot - it's a function that converts PDFs
- **Marker**: Another deterministic converter for scientific papers

By wrapping them as MCP servers, models can "command" these tools.

### 3.2 Transport Layers

| Transport | Use Case | Characteristics |
|-----------|----------|-----------------|
| **stdio** | Local desktop apps | Simple, synchronous |
| **SSE** | Web services | Async, supports streaming status |
| **HTTP** | REST API integration | Stateless, scalable |

**SSE is recommended** for document processing (long-running tasks).

### 3.3 Docling MCP Server

Official implementation available:

```bash
# Run directly
uvx docling-mcp-server

# Or install
pip install docling-mcp
```

**Capabilities:**
- `convert_document`: PDF → Markdown/JSON
- `extract_tables`: Get structured table data
- `get_metadata`: Document metadata extraction

### 3.4 Custom Marker MCP Server

```python
# marker_mcp_server.py
from mcp.server.fastmcp import FastMCP
from marker.convert import convert_single_pdf
from marker.models import load_all_models
import os

mcp = FastMCP("Marker PDF Service")

# Load models once at startup
device = os.environ.get("TORCH_DEVICE", "cpu")
model_lst = load_all_models(device=device)

@mcp.tool()
def convert_pdf_to_markdown(path: str) -> str:
    """Convert a PDF file to Markdown using Marker.

    Args:
        path: Absolute path to the PDF file

    Returns:
        Markdown text of the document
    """
    full_text, images, metadata = convert_single_pdf(path, model_lst)
    return full_text

@mcp.tool()
def convert_with_images(path: str, output_dir: str) -> dict:
    """Convert PDF and extract embedded images.

    Args:
        path: Absolute path to the PDF file
        output_dir: Directory to save extracted images

    Returns:
        Dictionary with 'text' and 'image_paths'
    """
    full_text, images, metadata = convert_single_pdf(path, model_lst)

    image_paths = []
    for i, img in enumerate(images):
        img_path = f"{output_dir}/image_{i}.png"
        img.save(img_path)
        image_paths.append(img_path)

    return {"text": full_text, "image_paths": image_paths}

if __name__ == "__main__":
    mcp.run()
```

### 3.5 MCP in LiteLLM

LiteLLM injects MCP tool capabilities into system prompts:

```python
# LiteLLM automatically adds tool definitions
response = client.chat.completions.create(
    model="qwen-vl",
    messages=[
        {"role": "system", "content": "You have access to document conversion tools."},
        {"role": "user", "content": "Please convert the PDF at /docs/paper.pdf to markdown"}
    ],
    tools=[
        {
            "type": "function",
            "function": {
                "name": "docling_convert_document",
                "description": "Convert PDF to markdown",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "path": {"type": "string", "description": "Path to PDF"}
                    },
                    "required": ["path"]
                }
            }
        }
    ]
)
```

---

## 4. Llama-Swap (Model Router)

### 4.1 Purpose

Llama-swap manages `llama-server` processes:
- Spawns/kills processes based on requests
- Implements time-to-live (TTL) for model unloading
- Routes requests by model name

### 4.2 Configuration

**llama-swap-config.yaml:**
```yaml
listen: :8080

# Health check endpoint
health_check:
  endpoint: /health
  interval: 30s

models:
  # Primary reasoning model
  - name: qwen-vl
    cmd: >
      llama-server
      -m /models/Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf
      --mmproj /models/qwen-vl-mmproj.gguf
      --port 8081
      --n-gpu-layers 99
      --ctx-size 8192
    ttl: 600  # Unload after 10 minutes idle

  # OCR specialist
  - name: olmocr
    cmd: >
      llama-server
      -m /models/olmOCR-2-7B-Q4_K_M.gguf
      --clip_model_path /models/mmproj-olmOCR-vision.gguf
      --port 8081
      --n-gpu-layers 99
      --ctx-size 8192
      --chat-format chatml
    ttl: 300  # Unload after 5 minutes

  # Text-only model
  - name: llama-70b
    cmd: >
      llama-server
      -m /models/Meta-Llama-3.3-70B-Instruct-Q4_K_M.gguf
      --port 8081
      --n-gpu-layers 60
      --ctx-size 32768
    ttl: 900  # Keep longer (reasoning tasks)

# Memory management
memory:
  max_loaded_models: 1  # Only one model at a time (memory constraint)
  preload: []  # No preloading
```

### 4.3 Usage

```bash
# Install
go install github.com/mostlygeek/llama-swap@latest

# Run
llama-swap --config llama-swap-config.yaml
```

**Client request:**
```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "olmocr",
    "messages": [{"role": "user", "content": "..."}]
  }'
```

Llama-swap:
1. Checks if `olmocr` is loaded
2. If not, spawns `llama-server` with olmocr config
3. If another model is loaded, kills it first (max_loaded_models: 1)
4. Proxies request to spawned server

---

## 5. vLLM Serving (GPU Servers)

### 5.1 When to Use vLLM

vLLM is optimal for:
- Linux servers with NVIDIA GPUs
- High-throughput batch processing
- Multi-GPU deployment

**Not for Mac** (CUDA-optimized).

### 5.2 Configuration

```bash
# Install
pip install vllm

# Serve Qwen2.5-VL
python -m vllm.entrypoints.openai.api_server \
    --model Qwen/Qwen2.5-VL-7B-Instruct \
    --trust-remote-code \
    --tensor-parallel-size 2 \
    --gpu-memory-utilization 0.9 \
    --max-model-len 32768 \
    --port 8000
```

### 5.3 Docker Compose for vLLM

```yaml
version: '3.8'
services:
  vllm-qwen:
    image: vllm/vllm-openai:latest
    runtime: nvidia
    environment:
      - NVIDIA_VISIBLE_DEVICES=all
    command: >
      --model Qwen/Qwen2.5-VL-7B-Instruct
      --trust-remote-code
      --tensor-parallel-size 2
    ports:
      - "8000:8000"
    volumes:
      - ~/.cache/huggingface:/root/.cache/huggingface
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 2
              capabilities: [gpu]
```

### 5.4 Integration with LiteLLM

Add vLLM backend to LiteLLM config:

```yaml
model_list:
  - model_name: qwen-vl-gpu
    litellm_params:
      model: openai/qwen2.5-vl-7b-instruct
      api_base: "http://gpu-server:8000/v1"
      api_key: "sk-vllm"
```

---

## 6. Quota-Aware Routing

### 6.1 Strategy

Maximize free-tier cloud usage before falling back to local:

```python
class QuotaRouter:
    def __init__(self):
        self.quotas = {
            'aws_textract': 1000,  # pages/month
            'azure_vision': 500,
            'google_docai': 400,
        }

    def route(self, page_content):
        """Route to optimal engine based on content and quotas."""

        content_type = self.classify_content(page_content)

        # Text-heavy: Use Azure (best for languages)
        if content_type == 'text' and self.quotas['azure_vision'] > 0:
            self.quotas['azure_vision'] -= 1
            return 'azure'

        # Tables: Use local Docling (free, structural)
        if content_type == 'table':
            return 'docling'

        # Math/Diagrams: Use local Qwen (free, reasoning)
        if content_type in ['math', 'diagram']:
            return 'qwen-vl'

        # Default: Local olmOCR
        return 'olmocr'

    def classify_content(self, page):
        """Classify page content type."""
        # Use lightweight classifier or heuristics
        if has_latex_markers(page):
            return 'math'
        if has_table_structure(page):
            return 'table'
        if has_diagram_elements(page):
            return 'diagram'
        return 'text'
```

### 6.2 Cost Optimization

| Priority | Engine | Cost | Use For |
|----------|--------|------|---------|
| 1 | Local Docling | $0 | Tables, structure |
| 2 | Local Qwen-VL | $0 | Math, diagrams |
| 3 | Local olmOCR | $0 | Dense text |
| 4 | Azure (free tier) | $0 | Irish language validation |
| 5 | Cloud API | $$$ | Only if local fails |

---

## 7. Observability

### 7.1 LiteLLM Dashboard

```yaml
general_settings:
  database_url: "postgresql://localhost/litellm"
  ui_access_mode: "admin"
```

Access at `http://localhost:4000/ui`:
- Request/response logs
- Token usage tracking
- Latency metrics
- Error rates by model

### 7.2 Prometheus Metrics

```yaml
general_settings:
  prometheus_url: "http://prometheus:9090"
```

Key metrics:
- `litellm_requests_total{model="qwen-vl"}`
- `litellm_request_latency_seconds`
- `litellm_tokens_used{type="input|output"}`

### 7.3 Langfuse Integration

```yaml
callbacks:
  - langfuse

environment_variables:
  LANGFUSE_PUBLIC_KEY: "pk-..."
  LANGFUSE_SECRET_KEY: "sk-..."
  LANGFUSE_HOST: "https://cloud.langfuse.com"
```

---

## 8. Production Deployment

### 8.1 Systemd Services (Linux)

**/etc/systemd/system/litellm.service:**
```ini
[Unit]
Description=LiteLLM Proxy
After=network.target

[Service]
Type=simple
User=mlops
WorkingDirectory=/opt/litellm
ExecStart=/opt/litellm/venv/bin/litellm --config /opt/litellm/config.yaml --port 4000
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 8.2 Launchd Services (macOS)

**~/Library/LaunchAgents/com.local.docling.plist:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.local.docling</string>
    <key>ProgramArguments</key>
    <array>
        <string>/opt/homebrew/bin/docling-serve</string>
        <string>run</string>
    </array>
    <key>EnvironmentVariables</key>
    <dict>
        <key>DOCLING_SERVE_PORT</key>
        <string>5001</string>
    </dict>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
</dict>
</plist>
```

### 8.3 Health Checks

```python
# health_check.py
import httpx
import asyncio

SERVICES = {
    'litellm': 'http://localhost:4000/health',
    'docling': 'http://localhost:5001/health',
    'olmocr': 'http://localhost:8081/health',
    'qwen-vl': 'http://localhost:8082/health',
}

async def check_health():
    async with httpx.AsyncClient() as client:
        for name, url in SERVICES.items():
            try:
                resp = await client.get(url, timeout=5.0)
                status = "UP" if resp.status_code == 200 else "DOWN"
            except:
                status = "DOWN"
            print(f"{name}: {status}")

asyncio.run(check_health())
```

---

## 9. Architecture Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATIONS                                │
│        (Cursor, Open WebUI, Custom Scripts, Agents)             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   LITELLM GATEWAY                                │
│    Unified API | Routing | MCP Integration | Observability      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌───────────┬───────────┬───────────┬───────────┐
        ↓           ↓           ↓           ↓           ↓
┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐
│  LLAMA-   │ │   MLX     │ │  PYTORCH  │ │   MCP     │ │  CLOUD    │
│   SWAP    │ │  SERVER   │ │  FASTAPI  │ │ SERVERS   │ │   APIs    │
│ (GGUF)    │ │  (MLX)    │ │  (MPS)    │ │ (Tools)   │ │(Fallback) │
└───────────┘ └───────────┘ └───────────┘ └───────────┘ └───────────┘
     │             │             │             │             │
     └─────────────┴─────────────┴─────────────┴─────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        HARDWARE                                  │
│   Apple Silicon (UMA) | NVIDIA GPU (CUDA) | CPU (Fallback)      │
└─────────────────────────────────────────────────────────────────┘
```
