# Building a local/production LLM inference stack with llama.cpp, llama-swap, and Onyx

The core architecture uses **llama-server** (llama.cpp's HTTP server) for inference, **llama-swap** for multi-model hot-swapping, and **Onyx** for enterprise-grade RAG and knowledge management. This stack enables local-first development on an M4 Max with production deployment to Linux servers with NVIDIA GPUs, while supporting the Transformers v5 + Unsloth workflow for fine-tuning and GGUF export.

## Architectural overview and component relationships

The stack follows a layered proxy architecture: clients connect to Onyx (or llama-server's built-in UI) which routes requests through llama-swap, which manages multiple llama-server processes. Each component serves a distinct purpose—llama-server handles raw inference with OpenAI-compatible endpoints, llama-swap orchestrates model lifecycle and swapping, and Onyx provides document ingestion, RAG, and multi-user chat management.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Clients (Users, Applications, Agents)                               │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────────┐
│  Onyx UI (Port 3000)                                                │
│  • RAG/Knowledge Management  • Document Connectors (40+)            │
│  • Multi-user Auth           • Chat persistence (PostgreSQL)        │
└───────────────────────────┬─────────────────────────────────────────┘
                            │ OpenAI-compatible API
┌───────────────────────────▼─────────────────────────────────────────┐
│  llama-swap (Port 8080)                                             │
│  • Model routing by name     • TTL-based auto-unload                │
│  • Health check monitoring   • VRAM management via process lifecycle│
└───────┬───────────────┬───────────────┬─────────────────────────────┘
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│ llama-server │ │ llama-server│ │ llama-server│
│ (general)    │ │ (coder)     │ │ (math)      │
│ Port 9001    │ │ Port 9002   │ │ Port 9003   │
└──────────────┘ └─────────────┘ └─────────────┘
```

---

## llama-server deep dive: architecture and configuration

llama-server is a pure C/C++ HTTP server using httplib and nlohmann::json, designed for high-throughput LLM inference with a **slot-based architecture**. Each slot maintains its own KV cache, enabling concurrent request handling with continuous batching enabled by default.

### API endpoints supporting OpenAI compatibility

The server exposes both OpenAI-compatible and native llama.cpp endpoints:

| Endpoint | Purpose | Key Features |
|----------|---------|--------------|
| `/v1/chat/completions` | ChatML-formatted completion | Streaming, JSON schema constraints, tool calling |
| `/v1/completions` | Text completion (legacy) | Full llama.cpp parameter access |
| `/v1/embeddings` | Vector embeddings | Requires `--embedding` flag |
| `/v1/models` | List available models | OpenAI-compatible response format |
| `/completion` | Native endpoint | All sampling parameters (mirostat, grammar, etc.) |
| `/health` | Health check | Returns model loading status |
| `/metrics` | Prometheus metrics | Requires `--metrics` flag |

### Critical configuration flags for production

```bash
llama-server \
  -m /models/qwen2.5-7b-instruct.Q5_K_M.gguf \
  -c 8192 \                    # Context window (affects KV cache size)
  -np 8 \                      # Parallel slots for concurrent requests
  -ngl 999 \                   # GPU layers (-1 or 999 = all layers)
  -fa on \                     # Flash attention (memory efficiency)
  -b 2048 -ub 512 \            # Batch sizes (logical/physical)
  --host 0.0.0.0 --port 8080 \
  --api-key "sk-your-key" \    # Authentication
  --metrics \                  # Prometheus endpoint
  --no-slots \                 # Hide slot info in production
  --jinja \                    # Enable Jinja2 chat templates
  --mlock                      # Lock model in RAM
```

**Memory calculation** for KV cache: `n_ctx × (n_embd_head × n_head × 2) × type_size × n_parallel`. For 8 parallel slots with 1024 tokens each at f16 precision, expect **~500MB–1GB** additional VRAM beyond model weights.

### Built-in web UI capabilities

The new SvelteKit-based UI provides ChatGPT-like conversation with **file attachments** (PDF, images for vision models), **markdown rendering with LaTeX**, **conversation branching**, and **IndexedDB persistence**. Enable at `http://localhost:8080` by default; disable with `--no-webui`. Key limitation: **no server-side persistence**—conversations stored in browser only.

---

## llama-swap: multi-model hot-swapping architecture

llama-swap is a Go-based transparent proxy that intercepts OpenAI-compatible requests, extracts the `model` field, and routes to appropriate llama-server instances while managing process lifecycle.

### How request routing works

1. Client sends `POST /v1/chat/completions` with `"model": "math-specialist"`
2. llama-swap resolves model name (checking aliases)
3. If model isn't running, llama-swap starts the configured command
4. Waits for health check to pass (polls `/health` endpoint)
5. Proxies request to upstream llama-server
6. Returns response to client with streaming support

### Configuration YAML structure for specialist models

```yaml
# llama-swap-config.yaml
healthCheckTimeout: 180      # Seconds to wait for model startup
logLevel: info
startPort: 9000              # Auto-increment port macro

macros:
  "base-flags": >
    llama-server --port ${PORT} --flash-attn on --jinja --host 0.0.0.0

models:
  "general":
    cmd: |
      ${base-flags}
      -m /models/qwen2.5-7b-instruct.Q5_K_M.gguf
      -ngl 999 --ctx-size 8192
    aliases: ["gpt-4o-mini", "chat"]
    ttl: 300                 # Unload after 5 min inactivity

  "tables":                  # Table/structure specialist
    cmd: |
      ${base-flags}
      -m /models/table-transformer.Q4_K_M.gguf
      -ngl 999 --ctx-size 4096
    aliases: ["table-model", "structure"]
    ttl: 300

  "vision":                  # Visual reasoning
    cmd: |
      ${base-flags}
      -m /models/llava-1.6-mistral.Q5_K_M.gguf
      --mmproj /models/mmproj-model-f16.gguf
      -ngl 999 --ctx-size 4096
    aliases: ["llava", "image"]

  "math":                    # Mathematical reasoning
    cmd: |
      ${base-flags}
      -m /models/qwen2.5-math-7b.Q5_K_M.gguf
      -ngl 999 --ctx-size 8192
    aliases: ["math-model", "calculator"]

  "embeddings":              # Runs alongside LLMs
    cmd: |
      llama-server --port ${PORT}
      -m /models/nomic-embed-text-v1.5.Q8_0.gguf
      --embedding --ctx-size 8192

groups:
  "llm-group":
    swap: true               # Only one LLM at a time
    exclusive: true          # Unload other groups
    members: ["general", "tables", "vision", "math"]
  
  "embedding-group":
    swap: false              # Always keep loaded
    exclusive: false         # Coexist with LLMs
    persistent: true         # Cannot be unloaded by others
    members: ["embeddings"]

hooks:
  on_startup:
    preload: ["general", "embeddings"]
```

### VRAM management between model switches

llama-swap manages VRAM **indirectly through process lifecycle**: when swapping models, it sends SIGTERM to the current llama-server process (5-second graceful shutdown, then SIGKILL), releasing all GPU memory before starting the new model. The `groups` feature provides fine-grained control—`swap: true` ensures only one model from a group runs at a time, while `persistent: true` prevents other groups from unloading critical services like embedding models.

For **RTX 4070 (12GB)**, configure groups so only one 7B Q5_K_M model (~6GB VRAM including KV cache) loads at a time. The embedding model (Q8_0, ~1GB) can run persistently alongside.

---

## Onyx UI: enterprise RAG for local LLM backends

Onyx (formerly Danswer) is an open-source AI chat platform with advanced RAG capabilities, supporting **40+ document connectors** (Confluence, Slack, Google Drive, GitHub, etc.), hybrid search (BM-25 + semantic), and knowledge graphs.

### Architecture components

- **Frontend**: Next.js (TypeScript/React) on port 3000
- **Backend**: Python/FastAPI with SQLAlchemy ORM on port 8000
- **Database**: PostgreSQL (relational) + Vespa (vector search) + Redis (caching)
- **LLM Integration**: LiteLLM library for unified provider interface

### Connecting Onyx to llama-server/llama-swap

Configure via Admin Panel → Configuration → LLM Providers → "Add Custom LLM Provider":

```yaml
Provider Name: Local LLM Stack
Provider Type: custom (or openai for compatible APIs)
API Base: http://host.docker.internal:8080/v1  # llama-swap endpoint
Model Name: general                             # Must match llama-swap model
API Key: (optional, if llama-server requires auth)
```

Key environment variables in `.env`:
```bash
GEN_AI_API_ENDPOINT=http://llama-swap:8080/v1
GEN_AI_MODEL_VERSION=general
GEN_AI_TIMEOUT=120           # Increase for slower local models
```

### RAG pipeline and knowledge management

Onyx's RAG workflow: **Document Ingestion** → **Chunking** → **Embedding** (nomic-embed-text-v1) → **Vespa Indexing** → **Hybrid Query** (BM-25 + vector) → **Re-ranking** → **Context Injection** → **LLM Generation**. The system maintains **source-level permissions** from connected apps, enabling enterprise compliance.

### Onyx vs llama-server built-in UI comparison

| Feature | llama-server UI | Onyx |
|---------|-----------------|------|
| **Primary Use** | Quick testing, single-user | Enterprise, multi-user |
| **RAG/Knowledge** | None | Full pipeline with 40+ connectors |
| **Persistence** | Browser IndexedDB | Server-side PostgreSQL |
| **Auth** | API key only | SSO, OIDC, SAML (Enterprise) |
| **Document Handling** | File upload per chat | Persistent knowledge base |
| **Multi-model** | Manual | Configurable per assistant |
| **Resource Needs** | Minimal | Higher (Vespa, PostgreSQL, Redis) |

**Recommendation**: Use llama-server's built-in UI for development/testing; deploy Onyx for production with document management and multi-user requirements.

---

## Transformers v5 + Unsloth GGUF workflow

The new Transformers v5 GGUF interoperability enables loading quantized models directly into PyTorch for fine-tuning, then exporting back to GGUF for llama-server deployment.

### Loading GGUF in Transformers v5

```python
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch

model_id = "TheBloke/Qwen2.5-7B-Instruct-GGUF"
filename = "qwen2.5-7b-instruct.Q4_K_M.gguf"

# GGUF weights dequantized to fp32 for PyTorch compatibility
tokenizer = AutoTokenizer.from_pretrained(model_id, gguf_file=filename)
model = AutoModelForCausalLM.from_pretrained(
    model_id, 
    gguf_file=filename, 
    torch_dtype=torch.float16
)
```

### Complete fine-tuning workflow with Unsloth

Unsloth provides **2–5x faster training** and **70% less VRAM** through manual autograd optimization, Triton kernels, and custom gradient checkpointing.

```python
from unsloth import FastLanguageModel
from trl import SFTTrainer, SFTConfig

# Step 1: Load model with 4-bit quantization
model, tokenizer = FastLanguageModel.from_pretrained(
    model_name="unsloth/Qwen2.5-7B-Instruct",
    max_seq_length=4096,
    load_in_4bit=True,          # QLoRA - fits 7B in ~6.5GB VRAM
)

# Step 2: Add LoRA adapters
model = FastLanguageModel.get_peft_model(
    model,
    r=16,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                    "gate_proj", "up_proj", "down_proj"],
    lora_alpha=16,
    use_gradient_checkpointing="unsloth",  # Custom optimization
)

# Step 3: Fine-tune on bilingual Irish/English dataset
trainer = SFTTrainer(
    model=model,
    tokenizer=tokenizer,
    train_dataset=irish_english_dataset,
    args=SFTConfig(
        per_device_train_batch_size=2,
        gradient_accumulation_steps=4,
        max_seq_length=4096,
        output_dir="outputs",
    ),
)
trainer.train()

# Step 4: Export directly to GGUF
model.save_pretrained_gguf(
    "irish_model_gguf",
    tokenizer,
    quantization_method="q5_k_m"  # Recommended for 7B on RTX 4070
)
```

### Quantization strategies for your hardware

| Quant | Bits/Weight | 7B Size | 14B Size | PPL Δ | RTX 4070 | RTX 4090 |
|-------|-------------|---------|----------|-------|----------|----------|
| **Q4_K_M** | ~4.5 | 3.8GB | 7.6GB | +0.05 | ✅ 14B fits | ✅ All |
| **Q5_K_M** | ~5.5 | 4.3GB | 8.7GB | +0.04 | ✅ 7B optimal | ✅ All |
| **Q8_0** | 8 | 7GB | 14GB | +0.005 | ✅ 7B only | ✅ 14B fits |
| **F16** | 16 | 14GB | 28GB | baseline | ❌ | ⚠️ 7B only |

**Recommendations**: 
- **RTX 4070 (12GB)**: Q5_K_M for 7B models (best quality that fits), Q4_K_M for 14B
- **RTX 4090 (24GB)**: Q8_0 or Q6_K for maximum quality
- **M4 Max (128GB unified)**: Can run larger F16 models thanks to unified memory

### Tokenizer considerations for Irish/English bilingual content

Irish is an **extremely low-resource language** in most tokenizers. Key strategies:

1. **Use multilingual base models**: Qwen2.5 and Llama 3 have broader multilingual vocabularies
2. **Check token fertility**: Irish text shouldn't require 2–3x more tokens than English
3. **Consider vocabulary extension** if fine-tuning heavily on Irish data:

```python
# Test tokenizer efficiency
irish_text = "Dia dhuit, conas atá tú?"
english_text = "Hello, how are you?"
print(f"Irish tokens: {len(tokenizer.encode(irish_text))}")
print(f"English tokens: {len(tokenizer.encode(english_text))}")
# Ratio should be close to 1:1 for efficient inference
```

---

## Docker Compose orchestration for the complete stack

### Critical note for Apple Silicon development

**Docker Desktop on macOS cannot access Apple Silicon GPUs**—the Virtualization Framework provides no GPU passthrough. For M4 Max development:

1. **Run llama-server and llama-swap natively** using Metal backend
2. **Containerize only CPU-bound services** (Onyx, PostgreSQL, Redis)
3. **Connect via `host.docker.internal`** from containers to native services

### Production Docker Compose for Linux with NVIDIA GPUs

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ============ DATABASE LAYER ============
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-onyx}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-changeme}
      POSTGRES_DB: ${POSTGRES_DB:-onyx}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - llm-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U onyx"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    networks:
      - llm-network
    restart: unless-stopped

  # ============ LLM INFERENCE LAYER ============
  llama-swap:
    image: ghcr.io/mostlygeek/llama-swap:cuda
    volumes:
      - ./models:/models:ro
      - ./llama-swap-config.yaml:/app/config.yaml:ro
    ports:
      - "8080:8080"
    networks:
      - llm-network
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 180s
    restart: unless-stopped

  # ============ ONYX RAG PLATFORM ============
  onyx-api:
    image: onyxdotapp/onyx-backend:latest
    depends_on:
      postgres:
        condition: service_healthy
      llama-swap:
        condition: service_healthy
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_USER: ${POSTGRES_USER:-onyx}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-changeme}
      POSTGRES_DB: ${POSTGRES_DB:-onyx}
      REDIS_HOST: redis
      GEN_AI_API_ENDPOINT: http://llama-swap:8080/v1
      GEN_AI_MODEL_VERSION: general
      GEN_AI_TIMEOUT: 120
      AUTH_TYPE: ${AUTH_TYPE:-disabled}
    volumes:
      - onyx_storage:/app/storage
    networks:
      - llm-network
    restart: unless-stopped

  onyx-web:
    image: onyxdotapp/onyx-web-server:latest
    depends_on:
      - onyx-api
    environment:
      INTERNAL_URL: http://onyx-api:8000
    ports:
      - "3000:3000"
    networks:
      - llm-network
    restart: unless-stopped

  # ============ EMBEDDING MODEL (PERSISTENT) ============
  embedding-server:
    image: ghcr.io/ggml-org/llama.cpp:server-cuda
    environment:
      LLAMA_ARG_MODEL: /models/nomic-embed-text-v1.5.Q8_0.gguf
      LLAMA_ARG_EMBEDDING: "true"
      LLAMA_ARG_CTX_SIZE: 8192
      LLAMA_ARG_HOST: 0.0.0.0
      LLAMA_ARG_PORT: 8081
    volumes:
      - ./models:/models:ro
    networks:
      - llm-network
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              device_ids: ['0']
              capabilities: [gpu]
    restart: unless-stopped

networks:
  llm-network:
    driver: bridge

volumes:
  postgres_data:
  onyx_storage:
```

### Environment file structure

```bash
# .env
COMPOSE_PROJECT_NAME=llm-stack
POSTGRES_USER=onyx
POSTGRES_PASSWORD=secure_password_here
POSTGRES_DB=onyx
AUTH_TYPE=disabled
```

### Mise task configuration for CI/CD

```toml
# .mise.toml
[tools]
docker-compose = "latest"

[tasks.up]
run = "docker compose up -d"

[tasks.down]
run = "docker compose down"

[tasks.logs]
run = "docker compose logs -f llama-swap"

[tasks.health]
run = """
curl -s http://localhost:8080/health | jq
curl -s http://localhost:3000/api/health | jq
"""

[tasks.swap-model]
run = "curl -X POST http://localhost:8080/v1/chat/completions -H 'Content-Type: application/json' -d '{\"model\": \"$1\", \"messages\": [{\"role\": \"user\", \"content\": \"test\"}]}'"
```

---

## Addressing your specific technical questions

**1. How does llama-swap proxy requests to llama-server instances?**

llama-swap intercepts requests at the `/v1/*` endpoints, extracts the `model` field from the JSON body, resolves aliases, manages process lifecycle (start/stop llama-server instances), waits for health checks, then proxies the complete request including streaming responses via Server-Sent Events.

**2. Recommended Docker Compose setup?**

See the complete `docker-compose.yml` above. Key pattern: llama-swap manages all LLM model processes internally using the `cmd` field in config.yaml, while embedding models run as separate persistent containers. Onyx connects to llama-swap's single endpoint.

**3. How does Onyx connect to OpenAI-compatible APIs?**

Via Admin Panel → LLM Providers → Custom provider with `api_base: http://llama-swap:8080/v1`. Onyx uses LiteLLM internally which handles the OpenAI protocol. The `model` parameter in Onyx requests maps directly to llama-swap model names.

**4. Transformers v5 workflow?**

Load GGUF → dequantize to fp32 → fine-tune with Unsloth (QLoRA at 4-bit) → export via `model.save_pretrained_gguf()` with target quantization. This is a **one-way workflow**—you cannot "resume" from GGUF; each fine-tuning session starts fresh from a base model.

**5. Memory/VRAM management when swapping?**

llama-swap terminates processes completely between swaps (SIGTERM → 5s grace → SIGKILL), releasing all VRAM. No memory pooling or sharing—each model loads fresh. Use `groups` with `swap: true` to ensure only one large model runs at a time on constrained hardware.

**6. llama-server built-in UI vs Onyx?**

Built-in UI for development/testing (minimal resources, no persistence). Onyx for production (document RAG, multi-user, enterprise connectors, PostgreSQL persistence). They can run simultaneously—Onyx for knowledge work, built-in UI for quick model testing.

**7. Onyx RAG/knowledge management integration?**

Onyx indexes documents from connectors into Vespa (vector DB), performs hybrid BM-25 + semantic search on user queries, retrieves top chunks, injects them into the LLM prompt, and generates responses with citations. Works with any OpenAI-compatible backend including llama-server—no special integration required beyond the API endpoint configuration.

---

## Production deployment checklist

- [ ] Configure `--api-key` on llama-server for authentication
- [ ] Enable `--metrics` for Prometheus monitoring
- [ ] Disable `--slots` endpoint to prevent information leakage
- [ ] Set appropriate `ttl` values in llama-swap to free VRAM during idle periods
- [ ] Configure PostgreSQL backups for Onyx data
- [ ] Use nginx reverse proxy with `proxy_buffering off` for streaming
- [ ] Set memory limits in Docker Compose to prevent OOM
- [ ] Enable health checks with appropriate `start_period` for large model loading
- [ ] Test bilingual tokenizer efficiency before production deployment
- [ ] Validate GGUF quantization quality with perplexity benchmarks