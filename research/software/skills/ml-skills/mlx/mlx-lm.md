# MLX-LM Development Skill

You are an expert MLX-LM developer with deep knowledge of Apple Silicon optimization, LLM inference, model quantization, and fine-tuning. Use this skill when working with MLX-LM for running or fine-tuning large language models on Apple Silicon.

## When to Use This Skill

Activate this skill when:
- Working with MLX or MLX-LM on Apple Silicon (M1/M2/M3/M4)
- Running LLM inference locally on Mac
- Fine-tuning models with LoRA or QLoRA
- Converting Hugging Face models to MLX format
- Optimizing model performance for Apple Silicon
- Implementing local AI applications
- Building OpenAI-compatible API servers
- Troubleshooting memory or performance issues

## Core MLX-LM Knowledge

### What is MLX-LM?

MLX-LM is a Python package for running and fine-tuning large language models on Apple Silicon:
- **Apple Silicon Optimized** - Leverages M-series chip unified memory and Metal GPU
- **Efficient Quantization** - 4-bit/8-bit reduces memory by up to 75%
- **LoRA & QLoRA** - Fine-tune models on consumer hardware
- **Lazy Evaluation** - Graph optimization before execution
- **Hugging Face Integration** - Thousands of compatible models
- **OpenAI-Compatible** - Drop-in replacement for testing

**Key Performance:**
- 4-bit quantization: Llama-7B from 14GB → 4GB
- QLoRA: Fine-tune 7B models on 16GB RAM
- Sub-100ms latency with optimized models

### Hardware Requirements

**Minimum:**
- Apple Silicon (M1/M2/M3/M4)
- macOS 14.0+
- Python 3.11+
- 8GB unified memory

**Memory Guidelines:**
- **8GB RAM**: 3B models or 7B with 4-bit quantization
- **16GB RAM**: 7B-8B models comfortably
- **32GB+ RAM**: 13B-34B models

## Installation and Setup

```bash
# Install MLX-LM
pip install mlx-lm

# Verify installation
python -c "import mlx_lm; print(mlx_lm.__version__)"

# Install optional dependencies
pip install mlx-lm[server]  # For API server
```

## Core API Patterns

### Basic Model Loading and Generation

```python
from mlx_lm import load, generate

# Load quantized model (recommended)
model, tokenizer = load("mlx-community/Mistral-7B-Instruct-v0.3-4bit")

# Generate text
response = generate(
    model,
    tokenizer,
    prompt="Explain quantum computing in simple terms",
    max_tokens=200,
    temp=0.7,
    verbose=True
)
print(response)
```

### Streaming for Interactive Applications

```python
from mlx_lm import load, stream_generate

model, tokenizer = load("mlx-community/Llama-3.2-3B-Instruct-4bit")

prompt = "Write a story about AI"
for chunk in stream_generate(model, tokenizer, prompt, max_tokens=512):
    print(chunk.text, end="", flush=True)
print()
```

### Chat with Templates

```python
from mlx_lm import load, generate

model, tokenizer = load("mlx-community/Mistral-7B-Instruct-v0.3-4bit")

# Format messages
messages = [
    {"role": "system", "content": "You are a helpful assistant"},
    {"role": "user", "content": "What is machine learning?"}
]

# Apply chat template
prompt = tokenizer.apply_chat_template(
    messages,
    add_generation_prompt=True,
    tokenize=False
)

response = generate(model, tokenizer, prompt=prompt, max_tokens=200)
```

### Multi-Turn Conversations with Prompt Caching

```python
from mlx_lm import load, generate
from mlx_lm.models.cache import make_prompt_cache

model, tokenizer = load("mlx-community/Llama-3.2-3B-Instruct-4bit")
prompt_cache = make_prompt_cache(model)
conversation_history = []

def chat_turn(user_message):
    global conversation_history

    conversation_history.append({"role": "user", "content": user_message})

    prompt = tokenizer.apply_chat_template(
        conversation_history,
        add_generation_prompt=True
    )

    response = generate(
        model,
        tokenizer,
        prompt=prompt,
        max_tokens=200,
        prompt_cache=prompt_cache  # Reuses cached context
    )

    conversation_history.append({"role": "assistant", "content": response})
    return response

# Usage - cache makes subsequent turns faster
print(chat_turn("Hi, my name is Alice"))
print(chat_turn("What's my name?"))  # Uses cached context
```

## CLI Commands

### Generate Text

```bash
# Basic generation
mlx_lm.generate --model mlx-community/Mistral-7B-Instruct-v0.3-4bit \
  --prompt "Explain quantum computing" \
  --max-tokens 200 \
  --temp 0.7 \
  --verbose

# With advanced parameters
mlx_lm.generate \
  --model mlx-community/Llama-3.2-3B-Instruct-4bit \
  --prompt "Write a poem" \
  --max-tokens 200 \
  --temp 0.9 \
  --top-p 0.95 \
  --repetition-penalty 1.1 \
  --seed 42
```

### Interactive Chat

```bash
# Start chat REPL
mlx_lm.chat --model mlx-community/Mistral-7B-Instruct-v0.3-4bit
```

### Convert Models

```bash
# Convert with 4-bit quantization (recommended)
mlx_lm.convert --hf-path mistralai/Mistral-7B-Instruct-v0.3 -q

# Convert and upload to Hugging Face
mlx_lm.convert \
  --hf-path mistralai/Mistral-7B-Instruct-v0.3 \
  -q \
  --upload-repo mlx-community/my-mistral-4bit

# Custom quantization settings
mlx_lm.convert \
  --hf-path meta-llama/Llama-2-7b-hf \
  --mlx-path ./mlx_model \
  -q \
  --q-bits 8 \
  --q-group-size 128
```

## Fine-Tuning with LoRA/QLoRA

### Dataset Preparation

MLX-LM supports three formats (save as `train.jsonl` and `valid.jsonl`):

**1. Text Format:**
```json
{"text": "Question: What is machine learning?\nAnswer: Machine learning is..."}
{"text": "Question: Explain neural networks.\nAnswer: Neural networks are..."}
```

**2. Chat Format:**
```json
{
  "messages": [
    {"role": "system", "content": "You are a helpful assistant"},
    {"role": "user", "content": "What is AI?"},
    {"role": "assistant", "content": "AI stands for..."}
  ]
}
```

**3. Completions Format:**
```json
{"prompt": "Translate to French: Hello", "completion": "Bonjour"}
{"prompt": "Translate to French: Thank you", "completion": "Merci"}
```

### Basic LoRA Training

```bash
# LoRA on full precision model
mlx_lm.lora \
  --train \
  --model mistralai/Mistral-7B-Instruct-v0.2 \
  --data ./data \
  --batch-size 4 \
  --lora-layers 16 \
  --lora-rank 8 \
  --lora-alpha 16 \
  --iters 1000 \
  --val-batches 10 \
  --save-every 100 \
  --adapter-path ./adapters
```

### QLoRA Training (Recommended)

```bash
# QLoRA with 4-bit base model - more memory efficient
mlx_lm.lora \
  --train \
  --model mlx-community/Mistral-7B-Instruct-v0.2-4bit \
  --data ./data \
  --batch-size 8 \
  --lora-layers 16 \
  --lora-rank 16 \
  --lora-alpha 32 \
  --learning-rate 1e-5 \
  --iters 2000 \
  --val-batches 10 \
  --save-every 100 \
  --adapter-path ./adapters \
  --test
```

### LoRA Configuration Guidelines

**Rank (r):**
- Start with 8 or 16
- Increase to 32-64 for complex tasks
- Higher rank = more capacity but more memory

**Alpha:**
- Common: alpha = rank or alpha = 2 * rank
- Controls strength of fine-tuning

**Layers:**
- Apply to 8-16 layers
- More layers = more capacity but slower training

**Batch Size:**
- 16GB RAM: batch-size 2-4
- 32GB RAM: batch-size 4-8
- 64GB+ RAM: batch-size 8-16

### Fusing Adapters

```bash
# Merge LoRA with base model
mlx_lm.fuse \
  --model mistralai/Mistral-7B-Instruct-v0.3 \
  --adapter-path ./adapters \
  --save-path ./fused_model

# Fuse and de-quantize for export to other formats
mlx_lm.fuse \
  --model mlx-community/Mistral-7B-Instruct-v0.3-4bit \
  --adapter-path ./adapters \
  --save-path ./production_model \
  --de-quantize

# Fuse and upload
mlx_lm.fuse \
  --model mistralai/Mistral-7B-Instruct-v0.3 \
  --adapter-path ./adapters \
  --upload-repo myusername/my-finetuned-model
```

## Optimization Best Practices

### Always Use Quantized Models

```python
# ✅ GOOD: 4-bit quantization (4GB)
model, tokenizer = load("mlx-community/Mistral-7B-Instruct-v0.3-4bit")

# ❌ AVOID: Full precision when quantized available (14GB)
model, tokenizer = load("mistralai/Mistral-7B-Instruct-v0.3")
```

### Enable Streaming for Interactive Apps

```python
# ✅ GOOD: Streaming for better UX
for chunk in stream_generate(model, tokenizer, prompt, max_tokens=512):
    print(chunk.text, end="", flush=True)

# ❌ AVOID: Blocking generation
response = generate(model, tokenizer, prompt, max_tokens=512)
print(response)
```

### Use Prompt Caching for Conversations

```python
from mlx_lm.models.cache import make_prompt_cache

# ✅ GOOD: Cache for multi-turn conversations
cache = make_prompt_cache(model)
response1 = generate(model, tokenizer, prompt1, prompt_cache=cache)
response2 = generate(model, tokenizer, prompt2, prompt_cache=cache)

# ❌ AVOID: No caching (recomputes context)
response1 = generate(model, tokenizer, prompt1)
response2 = generate(model, tokenizer, prompt2)
```

### Memory Management

```python
import mlx.core as mx

# Clear cache periodically for long-running apps
mx.metal.clear_cache()

# Use rotating KV cache for long generations
response = generate(
    model,
    tokenizer,
    prompt=prompt,
    max_kv_size=512  # Limits cache growth
)

# Monitor memory with Activity Monitor
# Or: ps -o rss= -p <pid>
```

### Generation Parameters

```python
# Factual/deterministic (low temperature)
response = generate(
    model,
    tokenizer,
    prompt="What is the capital of France?",
    temp=0.2,
    top_p=0.9
)

# Creative/diverse (high temperature)
response = generate(
    model,
    tokenizer,
    prompt="Write a creative story",
    temp=0.9,
    top_p=0.95
)

# Reduce repetition
response = generate(
    model,
    tokenizer,
    prompt=prompt,
    repetition_penalty=1.1  # >1.0 reduces repetition
)
```

## OpenAI-Compatible API Server

### Start Server

```bash
# Basic server
mlx_lm.server --model mlx-community/Llama-3.2-3B-Instruct-4bit --port 8080

# Custom port and host
mlx_lm.server \
  --model mlx-community/Mistral-7B-Instruct-v0.3-4bit \
  --port 8000 \
  --host 0.0.0.0
```

### Use with OpenAI Client

```python
import openai

# Configure client for local MLX server
client = openai.OpenAI(
    base_url="http://localhost:8080/v1",
    api_key="not-needed"
)

# Chat completion
response = client.chat.completions.create(
    model="mlx-model",
    messages=[
        {"role": "system", "content": "You are a helpful coding assistant"},
        {"role": "user", "content": "Write a Python function for fibonacci"}
    ],
    temperature=0.7,
    max_tokens=300
)

print(response.choices[0].message.content)

# Streaming
stream = client.chat.completions.create(
    model="mlx-model",
    messages=[{"role": "user", "content": "Explain recursion"}],
    stream=True
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

## Integration Patterns

### LangChain Integration

```python
from langchain_community.chat_models.mlx import ChatMLX
from langchain_core.messages import HumanMessage, SystemMessage

# Initialize
chat = ChatMLX(
    model="mlx-community/Llama-3.2-3B-Instruct-4bit",
    temp=0.7
)

# Use with LangChain
messages = [
    SystemMessage(content="You are a helpful assistant"),
    HumanMessage(content="What is the capital of France?")
]

response = chat.invoke(messages)
print(response.content)
```

### Singleton Model Manager

```python
class MLXModelManager:
    _instance = None
    _models = {}

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def get_model(self, model_name):
        if model_name not in self._models:
            from mlx_lm import load
            model, tokenizer = load(model_name)
            self._models[model_name] = (model, tokenizer)
        return self._models[model_name]

# Usage
manager = MLXModelManager()
model, tokenizer = manager.get_model("mlx-community/Mistral-7B-Instruct-v0.3-4bit")
```

### Chatbot Class

```python
from mlx_lm import load, stream_generate
from mlx_lm.models.cache import make_prompt_cache

class Chatbot:
    def __init__(self, model_name):
        self.model, self.tokenizer = load(model_name)
        self.cache = make_prompt_cache(self.model)
        self.history = []

    def chat(self, user_message):
        self.history.append({"role": "user", "content": user_message})

        prompt = self.tokenizer.apply_chat_template(
            self.history,
            add_generation_prompt=True
        )

        response_text = ""
        for chunk in stream_generate(
            self.model,
            self.tokenizer,
            prompt,
            max_tokens=512,
            prompt_cache=self.cache
        ):
            print(chunk.text, end="", flush=True)
            response_text += chunk.text
        print()

        self.history.append({"role": "assistant", "content": response_text})
        return response_text

# Usage
bot = Chatbot("mlx-community/Llama-3.2-3B-Instruct-4bit")
bot.chat("Hello!")
bot.chat("Tell me a fun fact")
```

## Troubleshooting

### Out of Memory Errors

**Solutions:**
1. Use quantized models (4-bit recommended)
2. Limit KV cache size with `max_kv_size`
3. Clear cache periodically
4. Reduce batch size during training

```python
# Use 4-bit instead of full precision
model, tokenizer = load("mlx-community/Mistral-7B-Instruct-v0.3-4bit")

# Limit KV cache
response = generate(model, tokenizer, prompt, max_kv_size=512)

# Clear cache
import mlx.core as mx
mx.metal.clear_cache()
```

### trust_remote_code Error

```python
# Enable for models with custom code (e.g., Qwen)
model, tokenizer = load(
    "qwen/Qwen-7B",
    tokenizer_config={"trust_remote_code": True}
)

# Or via CLI
mlx_lm.generate --model qwen/Qwen-7B --trust-remote-code --prompt "..."
```

### Slow Performance

**Checklist:**
- ✅ Use 4-bit quantized models
- ✅ Enable prompt caching for conversations
- ✅ Use streaming for better perceived speed
- ✅ Monitor Activity Monitor for memory pressure
- ✅ Close other memory-intensive applications

### Fine-Tuning Crashes

**Common Causes:**
- Batch size too large for available memory
- Dataset format incorrect
- Missing validation data

**Solutions:**
```bash
# Reduce batch size
mlx_lm.lora --train --model ... --data ... --batch-size 2

# Ensure correct structure
# data/train.jsonl and data/valid.jsonl must exist

# Check dataset format matches one of three supported formats
```

## Supported Model Architectures

**Core Architectures:**
- Llama (Llama 2, Llama 3, Llama 3.1, Llama 3.2)
- Mistral (Mistral 7B, Small, Large)
- Mixtral (Mixture of Experts)
- Phi (Phi-2, Phi-3)
- Gemma (2B, 7B)
- Qwen (Qwen 2, Qwen 2.5)
- StableLM
- DeepSeek

**Compatibility:**
- Thousands of models on Hugging Face Hub
- mlx-community organization has 1000+ pre-converted models
- Most Mistral/Llama-style models work out of the box

## Popular Pre-Converted Models

```python
# Small models (3B) - Great for 8GB RAM
"mlx-community/Llama-3.2-3B-Instruct-4bit"
"mlx-community/Phi-3-mini-4k-instruct-4bit"

# Medium models (7-8B) - Ideal for 16GB RAM
"mlx-community/Mistral-7B-Instruct-v0.3-4bit"
"mlx-community/Llama-3.1-8B-Instruct-4bit"
"mlx-community/Qwen2.5-7B-Instruct-4bit"

# Large models (13B+) - Requires 32GB+ RAM
"mlx-community/Llama-2-13B-chat-4bit"
"mlx-community/Mixtral-8x7B-Instruct-v0.1-4bit"
```

## Key Reminders

1. **Always use quantized models** - 4-bit recommended for best memory/quality trade-off
2. **Enable streaming** - Better UX for interactive applications
3. **Use prompt caching** - Essential for multi-turn conversations
4. **Monitor memory** - Use Activity Monitor to watch for pressure
5. **Clear cache periodically** - For long-running applications
6. **Start conservative with LoRA** - rank=8 or 16, increase if needed
7. **Include validation data** - Always use --val-batches during training
8. **Save checkpoints** - Use --save-every to avoid losing progress
9. **Test with realistic prompts** - Before deploying fine-tuned models
10. **Use appropriate temperatures** - Low (0.2) for facts, high (0.9) for creativity

## Performance Expectations

**Quantization:**
- 4-bit: 75% memory reduction, minimal quality loss
- 8-bit: 50% memory reduction, better quality

**Speed (M3 Max):**
- Prompt processing: ~650 tokens/s
- Token generation: ~19 tokens/s

**Memory (Llama-7B):**
- Full precision: ~14 GB
- 4-bit quantized: ~4 GB
- QLoRA training: ~7 GB

## Resources

- **Documentation**: https://ml-explore.github.io/mlx/
- **GitHub**: https://github.com/ml-explore/mlx-lm
- **Models**: https://huggingface.co/mlx-community
- **Conversion Tool**: https://huggingface.co/spaces/mlx-community/mlx-my-repo
- **Examples**: https://github.com/ml-explore/mlx-examples

## Success Criteria

When working with MLX-LM, you've succeeded when:
- ✅ Using 4-bit quantized models by default
- ✅ Implementing streaming for interactive applications
- ✅ Enabling prompt caching for conversations
- ✅ Memory usage is appropriate for model size
- ✅ Fine-tuning includes validation and checkpointing
- ✅ Dataset format is correct and consistent
- ✅ Generation parameters match use case (temp, top-p)
- ✅ Code follows singleton pattern for model management
- ✅ Performance is optimized for Apple Silicon
- ✅ Error handling for OOM and other common issues
