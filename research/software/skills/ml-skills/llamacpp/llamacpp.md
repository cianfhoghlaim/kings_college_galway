# llama.cpp Development Skill

## Description
Expert assistant for llama.cpp - the high-performance C/C++ LLM inference engine. This skill provides comprehensive support for building, deploying, optimizing, and integrating llama.cpp for local LLM inference across diverse hardware platforms.

## Skill Scope

### What This Skill Covers
- llama.cpp setup, building, and configuration
- GGUF format and model conversion from HuggingFace/PyTorch
- Quantization selection and optimization
- Performance tuning for CPU and GPU inference
- Server deployment and OpenAI-compatible API usage
- Python bindings (llama-cpp-python) integration
- Framework integration (LangChain, LlamaIndex)
- Memory optimization and hardware acceleration
- Troubleshooting inference issues

### When to Use This Skill
- Setting up llama.cpp for local LLM inference
- Converting models to GGUF format
- Optimizing inference performance
- Deploying llama.cpp as an API server
- Integrating llama.cpp with applications
- Selecting appropriate quantization methods
- Troubleshooting memory or performance issues
- Configuring GPU acceleration
- Building LLM-powered applications

## Core Concepts

### What is llama.cpp?
llama.cpp is an **open-source C/C++ inference engine** created by Georgi Gerganov in March 2023 for running LLMs locally with minimal setup and state-of-the-art performance. With 85,000+ GitHub stars, it's the de facto standard for local LLM inference.

**Key Characteristics:**
- Pure C/C++ with no external dependencies
- CPU-first design with extensive GPU support
- Aggressive quantization (1.5-bit to 8-bit)
- Memory-mapped execution for efficient loading
- Cross-platform (servers to edge devices)

### GGUF Format
**Generic GPT Unified Format** - custom file format designed for llama.cpp:
- Single-file design (tokenizer + weights + metadata)
- Key-value metadata structure (extensible)
- Memory-mapped friendly
- Efficient loading and execution

### Memory Architecture
- **Memory bandwidth-bound**: Performance limited by how fast weights load from RAM/VRAM, not compute
- **KV cache**: Stores key-value states, grows with context size
- **Quantization**: Reduces memory footprint and bandwidth requirements

## Common Tasks

### Task 1: Build llama.cpp with GPU Acceleration

**Context:** Set up llama.cpp from source with appropriate hardware acceleration.

**Steps:**

1. **Determine target backend**:
   - NVIDIA GPU → CUDA
   - AMD GPU → ROCm or Vulkan
   - Apple Silicon → Metal (default)
   - Intel GPU → Vulkan or SYCL
   - CPU only → No special flags

2. **Clone and build**:

**CPU Only:**
```bash
git clone https://github.com/ggml-org/llama.cpp
cd llama.cpp
cmake -B build
cmake --build build --config Release
```

**CUDA (NVIDIA):**
```bash
cmake -B build -DGGML_CUDA=ON
cmake --build build --config Release
```

**Metal (macOS - default):**
```bash
cmake -B build
cmake --build build --config Release
```

**Vulkan (Cross-platform):**
```bash
cmake -B build -DGGML_VULKAN=ON
cmake --build build --config Release
```

**ROCm (AMD):**
```bash
cmake -B build -DGGML_HIP=ON
cmake --build build --config Release
```

3. **Verify build**:
```bash
./build/bin/llama-cli --version
```

4. **Install (optional)**:
```bash
sudo cmake --install build --prefix /usr/local
```

**Best Practices:**
- Use CMake, not deprecated Makefile
- Build with Release configuration for performance
- Can enable multiple backends simultaneously
- Test with a small model first

### Task 2: Convert HuggingFace Model to GGUF

**Context:** Convert a model from HuggingFace to GGUF format for use with llama.cpp.

**Steps:**

1. **Download model from HuggingFace**:
```bash
# Using HuggingFace CLI (recommended)
pip install huggingface-hub
huggingface-cli download meta-llama/Llama-3.1-8B-Instruct --local-dir ./models/llama-3.1-8b

# Or use git
git clone https://huggingface.co/meta-llama/Llama-3.1-8B-Instruct ./models/llama-3.1-8b
```

2. **Convert to GGUF (F16 precision)**:
```bash
python convert_hf_to_gguf.py ./models/llama-3.1-8b \
  --outtype f16 \
  --outfile ./models/llama-3.1-8b-f16.gguf
```

3. **Quantize to desired precision (recommended: Q4_K_M)**:
```bash
./build/bin/llama-quantize \
  ./models/llama-3.1-8b-f16.gguf \
  ./models/llama-3.1-8b-q4_k_m.gguf \
  Q4_K_M
```

4. **Test converted model**:
```bash
./build/bin/llama-cli -m ./models/llama-3.1-8b-q4_k_m.gguf \
  --prompt "Hello, how are you?" \
  -n 50
```

**Quantization Selection Guide:**
- **Q4_K_M**: Best balance (RECOMMENDED for most use cases)
- **Q5_K_M**: Excellent quality, slightly larger
- **Q8_0**: Highest quality quantized, ~95% of F16 size
- **Q3_K_M**: For constrained memory
- **Q2_K**: Extreme compression (noticeable quality loss)

**Available Types:** Q2_K, Q3_K_S, Q3_K_M, Q3_K_L, Q4_0, Q4_K_S, Q4_K_M, Q5_0, Q5_K_S, Q5_K_M, Q6_K, Q8_0

**Best Practices:**
- Always convert to F16 first, then quantize
- Prefer K-quants over legacy formats (Q4_K_M > Q4_0)
- Test quantized model quality before deployment
- Keep F16 version for re-quantization

### Task 3: Optimize Inference Performance

**Context:** Maximize inference speed for a given model and hardware configuration.

**Steps:**

1. **Memory Bandwidth Optimization (MOST CRITICAL)**:

**CPU Inference:**
- **Use dual-channel or quad-channel RAM** - single biggest factor
- Example: 8x16GB DDR4 is 2x faster than 4x32GB (more channels)
- Upgrade to faster RAM (DDR4-3200 or better)
- Can improve from 1.5 to 4+ tokens/s just from RAM configuration

**GPU Inference:**
- **Ensure model fits entirely in VRAM** - mixed CPU/GPU much slower
- Use quantization to fit in VRAM if needed
- Monitor with `nvidia-smi`, `rocm-smi`, or Activity Monitor (macOS)

2. **Thread Configuration**:

**CPU-Only:**
```bash
# Find physical cores (not hyperthreads)
lscpu | grep "Core(s) per socket"

# Use physical core count only
./llama-cli -m model.gguf --prompt "test" -t 8  # 8 physical cores
```

**Important:** SMT/hyperthreading HURTS LLM performance - use physical cores only!

**GPU-Accelerated:**
```bash
# Reduce threads to 2-4 when using GPU
./llama-cli -m model.gguf --prompt "test" -ngl 999 -t 4
```

**Apple Silicon:**
- Use p-core count only, never include e-cores
- Example: M1 Max (8 p-cores, 2 e-cores) → `-t 8`

3. **Enable Flash Attention**:
```bash
./llama-cli -m model.gguf --prompt "test" -fa -ngl 999
```
- 5-10x faster prompt processing
- ~20-30% memory reduction
- Negligible impact on generation speed
- **Always enable on supported models**

4. **GPU Layer Offloading**:
```bash
# Offload all layers to GPU
./llama-cli -m model.gguf -ngl 999 -fa

# Check if fully offloaded with --verbose
./llama-cli -m model.gguf -ngl 999 --verbose
```

**Verify full GPU offload:**
- Look for "llm_load_tensors: offloaded X/X layers to GPU"
- Monitor VRAM usage
- Partial offload significantly slower than full

5. **KV Cache Quantization** (if VRAM constrained):
```bash
./llama-cli -m model.gguf -ngl 999 -fa \
  --cache-type-k q8_0 \
  --cache-type-v q8_0
```
- Saves ~30-40% VRAM with Q8, ~50-60% with Q4
- Minimal quality impact with Q8
- Requires Flash Attention (`-fa`)

6. **Memory Locking**:
```bash
./llama-cli -m model.gguf --mlock -ngl 999 -fa
```
- Prevents OS swapping to disk
- Improves consistency
- May increase load time

**Performance Checklist:**
- [ ] Using dual/quad-channel RAM (CPU) or model fits in VRAM (GPU)
- [ ] Thread count = physical cores (CPU) or 2-4 (GPU)
- [ ] Flash Attention enabled (`-fa`)
- [ ] All layers offloaded to GPU (`-ngl 999`)
- [ ] KV cache quantized if VRAM limited
- [ ] Appropriate quantization level (Q4_K_M recommended)

**Expected Performance:**
- Apple M1 Max: 25-30 tokens/s (8B Q4_K_M)
- NVIDIA RTX 4090: 100-120 tokens/s (8B Q4_K_M)
- AMD 7950X CPU: 15-20 tokens/s (8B Q4_K_M, dual-channel RAM)

### Task 4: Deploy llama.cpp Server with OpenAI-Compatible API

**Context:** Set up llama.cpp as a production API server with OpenAI-compatible endpoints.

**Steps:**

1. **Basic server setup**:
```bash
./build/bin/llama-server \
  -m models/llama-3.1-8b-q4_k_m.gguf \
  --host 0.0.0.0 \
  --port 8080 \
  -ngl 999 \
  -fa \
  -c 8192
```

2. **Production configuration with security**:
```bash
./build/bin/llama-server \
  -m models/llama-3.1-8b-q4_k_m.gguf \
  --host 0.0.0.0 \
  --port 8080 \
  --api-key $(cat /run/secrets/llama_api_key) \
  -ngl 999 \
  -fa \
  -c 16384 \
  --cache-type-k q8_0 \
  --cache-type-v q8_0 \
  --parallel 4 \
  --cont-batching
```

**Key Parameters:**
- `--api-key`: Require authentication
- `--parallel`: Number of parallel slots
- `--cont-batching`: Enable continuous batching
- `--cache-type-k/v`: Quantize KV cache to save VRAM

3. **Test with Python OpenAI client**:
```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:8080/v1",
    api_key="your-api-key-here"
)

response = client.chat.completions.create(
    model="llama-3.1-8b",
    messages=[
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Explain quantum computing"}
    ],
    temperature=0.7,
    max_tokens=500
)

print(response.choices[0].message.content)
```

4. **Test streaming**:
```python
stream = client.chat.completions.create(
    model="llama-3.1-8b",
    messages=[{"role": "user", "content": "Tell me a story"}],
    stream=True
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

5. **Production deployment considerations**:

**Systemd Service** (Linux):
```ini
[Unit]
Description=llama.cpp Server
After=network.target

[Service]
Type=simple
User=llama
WorkingDirectory=/opt/llama.cpp
Environment="CUDA_VISIBLE_DEVICES=0"
ExecStart=/opt/llama.cpp/build/bin/llama-server \
  -m /opt/models/llama-3.1-8b-q4_k_m.gguf \
  --host 0.0.0.0 \
  --port 8080 \
  --api-key-file /etc/llama/api_key \
  -ngl 999 \
  -fa \
  -c 16384 \
  --parallel 4 \
  --cont-batching
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Reverse Proxy** (Nginx):
```nginx
upstream llama_backend {
    server 127.0.0.1:8080;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /etc/ssl/certs/cert.pem;
    ssl_certificate_key /etc/ssl/private/key.pem;

    location / {
        proxy_pass http://llama_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_buffering off;
    }
}
```

**Best Practices:**
- Use API key authentication
- Run behind reverse proxy with HTTPS
- Enable KV cache quantization to reduce VRAM
- Use `--parallel` for concurrent requests
- Monitor memory usage and set appropriate context size
- Implement rate limiting at reverse proxy level
- Use systemd or similar for auto-restart
- Monitor logs and metrics

### Task 5: Integrate llama.cpp with LangChain

**Context:** Use llama.cpp as the LLM backend for LangChain applications.

**Steps:**

1. **Install dependencies**:
```bash
pip install langchain-community llama-cpp-python
```

2. **Basic LLM setup**:
```python
from langchain_community.llms import LlamaCpp
from langchain_core.prompts import PromptTemplate
from langchain.chains import LLMChain
import multiprocessing

# Initialize LLM
llm = LlamaCpp(
    model_path="models/llama-3.1-8b-q4_k_m.gguf",
    temperature=0.7,
    n_ctx=4096,
    n_gpu_layers=35,  # Adjust based on your GPU
    n_batch=512,
    n_threads=multiprocessing.cpu_count() - 1,  # Physical cores
    max_tokens=512,
    verbose=False
)

# Create prompt template
template = """Question: {question}

Answer: Let's think step by step."""

prompt = PromptTemplate(template=template, input_variables=["question"])

# Create chain
chain = LLMChain(llm=llm, prompt=prompt)

# Run
result = chain.invoke({"question": "What is machine learning?"})
print(result['text'])
```

3. **RAG (Retrieval-Augmented Generation) pattern**:
```python
from langchain_community.embeddings import LlamaCppEmbeddings
from langchain_community.vectorstores import FAISS
from langchain.chains import RetrievalQA
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import TextLoader

# Load documents
loader = TextLoader("documents/knowledge_base.txt")
documents = loader.load()

# Split into chunks
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50
)
texts = text_splitter.split_documents(documents)

# Create embeddings (use dedicated embedding model)
embeddings = LlamaCppEmbeddings(
    model_path="models/all-minilm-l6-v2-q4_k_m.gguf"
)

# Create vector store
vectorstore = FAISS.from_documents(texts, embeddings)

# Create RAG chain
qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=vectorstore.as_retriever(search_kwargs={"k": 5}),
    return_source_documents=True,
    chain_type="stuff"
)

# Query
result = qa_chain({"query": "What is llama.cpp?"})
print(result['result'])
print("\nSources:")
for doc in result['source_documents']:
    print(f"- {doc.metadata}")
```

4. **Chat with history**:
```python
from langchain_community.chat_models import ChatLlamaCpp
from langchain.memory import ConversationBufferMemory
from langchain.chains import ConversationChain

# Initialize chat model
chat_model = ChatLlamaCpp(
    model_path="models/llama-3.1-8b-q4_k_m.gguf",
    temperature=0.7,
    n_gpu_layers=35,
    n_ctx=4096,
    verbose=False
)

# Create memory
memory = ConversationBufferMemory()

# Create conversation chain
conversation = ConversationChain(
    llm=chat_model,
    memory=memory,
    verbose=True
)

# Chat
response1 = conversation.predict(input="Hi, I'm interested in quantum computing")
print(response1)

response2 = conversation.predict(input="Can you explain qubits?")
print(response2)

response3 = conversation.predict(input="What did I say I was interested in?")
print(response3)
```

5. **Agent with tools**:
```python
from langchain.agents import initialize_agent, Tool, AgentType
from langchain.tools import DuckDuckGoSearchRun

# Initialize search tool
search = DuckDuckGoSearchRun()

# Define tools
tools = [
    Tool(
        name="Search",
        func=search.run,
        description="Useful for searching the internet for current information"
    )
]

# Initialize agent
agent = initialize_agent(
    tools=tools,
    llm=llm,
    agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
    verbose=True
)

# Run agent
result = agent.run("What is the latest version of Python?")
print(result)
```

**Best Practices:**
- Use dedicated embedding models for RAG (not general LLMs)
- Set `n_gpu_layers` to offload as much as possible
- Use physical core count for `n_threads` (CPU only)
- Reduce `n_threads` to 2-4 when using GPU
- Monitor memory usage with large contexts
- Use streaming for better UX in chat applications
- Cache embeddings vector store for repeated use

### Task 6: Troubleshoot Common Issues

**Context:** Diagnose and fix common llama.cpp problems.

**Issue 1: Out of Memory Errors**

**Symptoms:**
- "Failed to allocate memory"
- "Out of memory"
- System freeze or crash

**Diagnosis:**
```bash
# Check model size and memory requirements
ls -lh models/model.gguf

# Check available RAM
free -h  # Linux
vm_stat  # macOS

# Try loading with verbose output
./llama-cli -m models/model.gguf --verbose -n 1
```

**Solutions:**
1. Use more aggressive quantization:
```bash
# Instead of Q8_0, use Q4_K_M
./llama-quantize model-f16.gguf model-q4_k_m.gguf Q4_K_M
```

2. Reduce context size:
```bash
# Default 4096, try reducing to 2048
./llama-cli -m model.gguf -c 2048
```

3. Use KV cache quantization:
```bash
./llama-cli -m model.gguf -fa --cache-type-k q4_0 --cache-type-v q4_0
```

4. Disable memory mapping:
```bash
./llama-cli -m model.gguf --no-mmap
```

**Issue 2: Slow Inference**

**Symptoms:**
- Less than 1 token/second
- Long delays between tokens

**Diagnosis:**
```bash
# Check thread usage
htop  # Linux
Activity Monitor  # macOS

# Run with verbose to see performance metrics
./llama-cli -m model.gguf --prompt "test" -n 50 --verbose
```

**Solutions:**
1. Check memory bandwidth (MOST COMMON CAUSE):
```bash
# Verify RAM configuration
dmidecode -t memory | grep -i "configured clock"  # Linux
system_profiler SPMemoryDataType  # macOS

# Solution: Upgrade to dual-channel or faster RAM
```

2. Fix thread configuration:
```bash
# Use physical cores only, not hyperthreads
lscpu | grep "Core(s) per socket"

# Correct configuration (8 physical cores)
./llama-cli -m model.gguf -t 8
```

3. Enable Flash Attention:
```bash
./llama-cli -m model.gguf -fa
```

4. Use GPU if available:
```bash
# Offload all layers
./llama-cli -m model.gguf -ngl 999 -fa

# Verify full offload
./llama-cli -m model.gguf -ngl 999 --verbose | grep "offloaded"
```

**Issue 3: GPU Not Being Used**

**Symptoms:**
- `-ngl` flag has no effect
- GPU usage at 0% during inference
- Performance same as CPU

**Diagnosis:**
```bash
# Check build configuration
./llama-cli --version

# Check GPU availability
nvidia-smi  # NVIDIA
rocm-smi    # AMD
```

**Solutions:**
1. Rebuild with GPU support:
```bash
# CUDA
cmake -B build -DGGML_CUDA=ON
cmake --build build --config Release

# Vulkan
cmake -B build -DGGML_VULKAN=ON
cmake --build build --config Release

# ROCm
cmake -B build -DGGML_HIP=ON
cmake --build build --config Release
```

2. Verify GPU backend:
```bash
# Should show GPU backend in output
./llama-cli --version
```

3. Check VRAM capacity:
```bash
# Model must fit in VRAM for full offload
nvidia-smi --query-gpu=memory.total --format=csv
```

**Issue 4: Corrupted Model / Segfaults**

**Symptoms:**
- Segmentation fault
- Invalid GGUF file
- "Failed to load model"

**Diagnosis:**
```bash
# Check file integrity
md5sum model.gguf  # Compare with source

# Try loading with verbose
./llama-cli -m model.gguf --verbose
```

**Solutions:**
1. Re-download model (incomplete download):
```bash
# Delete and re-download
rm model.gguf
huggingface-cli download repo/model filename.gguf
```

2. Verify GGUF format:
```bash
# Check file header
file model.gguf  # Should show GGUF format
```

3. Reconvert from source:
```bash
# Reconvert from HuggingFace
python convert_hf_to_gguf.py ./hf_model --outtype f16
```

**Issue 5: Poor Quality Output**

**Symptoms:**
- Repetitive text
- Incoherent responses
- Generic answers

**Solutions:**
1. Check quantization level:
```bash
# Avoid very aggressive quantization (Q2_K)
# Use Q4_K_M or higher for better quality
```

2. Adjust sampling parameters:
```bash
./llama-cli -m model.gguf \
  --prompt "Your prompt" \
  --temp 0.7 \
  --top-p 0.9 \
  --repeat-penalty 1.1 \
  --top-k 40
```

3. Use appropriate chat format:
```python
# Python bindings
llm = Llama(
    model_path="model.gguf",
    chat_format="chatml"  # or "llama-2", "alpaca", etc.
)
```

4. Check context window:
```bash
# Ensure sufficient context
./llama-cli -m model.gguf -c 4096  # or higher
```

## API Usage Patterns

### CLI Patterns

**Basic Inference**:
```bash
./llama-cli -m model.gguf --prompt "Explain AI" -n 256 -ngl 999 -fa
```

**Interactive Mode**:
```bash
./llama-cli -m model.gguf --interactive --multiline-input -ngl 999 -fa
```

**Chat Format**:
```bash
./llama-cli -m model.gguf --chat-template chatml --interactive -ngl 999
```

**Embeddings**:
```bash
./llama-cli -m embedding-model.gguf --embedding --prompt "Text to embed"
```

**Server**:
```bash
./llama-server -m model.gguf --host 0.0.0.0 --port 8080 -ngl 999 -fa
```

### Python Patterns

**Direct Usage**:
```python
from llama_cpp import Llama

llm = Llama(
    model_path="model.gguf",
    n_gpu_layers=35,
    n_ctx=2048
)

output = llm("Your prompt", max_tokens=128)
```

**Streaming**:
```python
for token in llm("Your prompt", max_tokens=128, stream=True):
    print(token['choices'][0]['text'], end='', flush=True)
```

**Chat Completion**:
```python
messages = [
    {"role": "system", "content": "You are helpful."},
    {"role": "user", "content": "Hello!"}
]

response = llm.create_chat_completion(messages=messages)
```

**Function Calling**:
```python
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "Get current weather",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {"type": "string"}
                }
            }
        }
    }
]

response = llm.create_chat_completion(
    messages=messages,
    tools=tools,
    tool_choice="auto"
)
```

## Configuration Patterns

### Performance Tuning

**Maximum Performance (GPU)**:
```bash
./llama-cli -m model.gguf \
  -ngl 999 \           # All layers to GPU
  -fa \                # Flash Attention
  -t 4 \               # Reduce threads for GPU
  -c 8192 \            # Context size
  --cache-type-k q8_0 \
  --cache-type-v q8_0  # KV cache quantization
```

**Maximum Performance (CPU)**:
```bash
./llama-cli -m model.gguf \
  -t 8 \               # Physical cores only
  -fa \                # Flash Attention
  -c 4096 \            # Context size
  --mlock              # Lock memory
```

**Memory Constrained**:
```bash
./llama-cli -m model-q4_k_m.gguf \  # Aggressive quantization
  -c 2048 \            # Smaller context
  --cache-type-k q4_0 \
  --cache-type-v q4_0 \
  -ngl 20              # Partial offload
```

### Multi-GPU Configuration

**Tensor Parallelism**:
```bash
./llama-cli -m model.gguf \
  -ngl 999 \
  --tensor-split 0.6,0.4 \  # 60/40 split
  -sm row \                 # Row-wise split
  -fa
```

**Layer Assignment**:
```bash
./llama-cli -m model.gguf \
  -ngl 999 \
  --tensor-split 1.0,0.0 \  # All to first GPU
  -sm layer
```

## Best Practices

### Model Selection
- **4-8GB RAM**: 1B-3B models (Q4_K_M)
- **16GB RAM**: 7B-8B models (Q4_K_M)
- **24GB RAM/VRAM**: 8B models (Q8_0, F16) or 70B (Q4_K_M)
- **48GB RAM/VRAM**: 70B models (Q5_K_M)

### Quantization Guidelines
- **Production**: Q4_K_M (best balance)
- **High quality**: Q5_K_M or Q8_0
- **Constrained**: Q3_K_M
- **Always** prefer K-quants over legacy (Q4_K_M > Q4_0)

### Performance Guidelines
- **CPU**: Physical cores only, dual/quad-channel RAM
- **GPU**: Full offload (ngl=999), reduce threads (2-4)
- **Always**: Enable Flash Attention (`-fa`)
- **Memory**: Quantize KV cache if VRAM limited
- **Monitoring**: Use `--verbose` to verify configuration

### Security Guidelines
- Use API key authentication in production
- Run behind reverse proxy with HTTPS
- Implement rate limiting
- Monitor resource usage
- Use systemd or similar for auto-restart

## Integration Examples

### Docker Deployment
```dockerfile
FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    build-essential cmake git

WORKDIR /app
RUN git clone https://github.com/ggml-org/llama.cpp
WORKDIR /app/llama.cpp
RUN cmake -B build -DGGML_CUDA=ON && \
    cmake --build build --config Release

COPY models/ /app/models/

EXPOSE 8080

CMD ["./build/bin/llama-server", \
     "-m", "/app/models/model.gguf", \
     "--host", "0.0.0.0", \
     "--port", "8080", \
     "-ngl", "999", \
     "-fa"]
```

### FastAPI Integration
```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from llama_cpp import Llama
import os

app = FastAPI()

# Load model at startup
llm = Llama(
    model_path=os.getenv("MODEL_PATH", "models/model.gguf"),
    n_gpu_layers=int(os.getenv("GPU_LAYERS", 35)),
    n_ctx=int(os.getenv("CONTEXT_SIZE", 4096)),
    verbose=False
)

class PromptRequest(BaseModel):
    prompt: str
    max_tokens: int = 128
    temperature: float = 0.7

@app.post("/generate")
async def generate(request: PromptRequest):
    try:
        response = llm(
            request.prompt,
            max_tokens=request.max_tokens,
            temperature=request.temperature
        )
        return {"response": response['choices'][0]['text']}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health():
    return {"status": "healthy"}
```

## Troubleshooting Checklist

When experiencing issues, check:

**Build Issues:**
- [ ] Using CMake (not deprecated Makefile)
- [ ] Correct backend flags for hardware
- [ ] Build with Release configuration
- [ ] Dependencies installed (CUDA toolkit, etc.)

**Performance Issues:**
- [ ] Memory bandwidth (dual-channel RAM or full GPU offload)
- [ ] Thread count (physical cores only)
- [ ] Flash Attention enabled
- [ ] Full GPU offload (not partial)
- [ ] Appropriate quantization level

**Memory Issues:**
- [ ] Sufficient RAM for model size
- [ ] Context size not too large
- [ ] KV cache quantization if needed
- [ ] Memory locking if available

**Quality Issues:**
- [ ] Not over-quantized (Q4_K_M minimum)
- [ ] Appropriate sampling parameters
- [ ] Correct chat format/template
- [ ] Sufficient context window

**GPU Issues:**
- [ ] Built with GPU backend
- [ ] Model fits in VRAM
- [ ] Correct number of layers offloaded
- [ ] GPU drivers up to date

## Resources

### Official Documentation
- Repository: https://github.com/ggml-org/llama.cpp
- GGUF Spec: https://github.com/ggml-org/ggml/blob/master/docs/gguf.md
- Build Docs: docs/build.md
- Examples: examples/ directory

### Python Bindings
- Repository: https://github.com/abetlen/llama-cpp-python
- Documentation: https://llama-cpp-python.readthedocs.io/

### Community
- GitHub Discussions: Q&A and community support
- GitHub Issues: Bug reports and features
- Discord/Matrix: Real-time community support

### Model Sources
- HuggingFace: https://huggingface.co/models?library=gguf
- Search "GGUF" tag
- Popular quantizers: MaziyarPanahi, bartowski

## Skill Activation

This skill should be automatically activated when the user's query involves:
- llama.cpp setup, configuration, or deployment
- GGUF format and model conversion
- Local LLM inference optimization
- llama.cpp server deployment
- Performance tuning for LLM inference
- Integration with LangChain, LlamaIndex, or other frameworks
- Troubleshooting llama.cpp issues
- Quantization selection and optimization

---

*Last Updated: 2025-11-17*
*llama.cpp: Open-source C/C++ LLM inference engine*
*License: MIT*
