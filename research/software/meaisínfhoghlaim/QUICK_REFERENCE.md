# Quick Reference Guide: OCR Models & Integration

## Models at a Glance

### By Use Case

**Best Overall Quality**: Chandra (83.1) > OlmOCR-2 (82.3)
**Best Value**: LightOnOCR-1B (6× faster, similar quality)
**Best Multilingual**: PaddleOCR-VL (109 languages)
**Best for Documents**: Granite-Docling-258M (DocTag format, prompt-switching)
**Fastest**: LightOnOCR-1B (~493k pages/day on H100)
**Smallest**: PaddleOCR-VL (0.9B parameters)

### By Deployment Target

**Local Machine (GPU)**
- vLLM serving (all models)
- Transformers library (all models)
- Cost: One-time download

**Local Machine (CPU/Apple Silicon)**
- GGUF + llama.cpp (Qwen, Llama models)
- MLX-VLM (Apple Silicon optimized)
- Quantization: Q4_K_M recommended

**Cloud/Batch**
- Hugging Face Jobs + vLLM
- Inference Endpoints (managed)
- Cost: ~$180 per million pages

---

## Integration Quick Start

### 1. Load & Infer (Transformers)
```python
from transformers import AutoProcessor, AutoModelForImageTextToText
from PIL import Image

model = AutoModelForImageTextToText.from_pretrained(
    "nanonets/Nanonets-OCR2-3B",
    device_map="auto",
    attn_implementation="flash_attention_2"
)
processor = AutoProcessor.from_pretrained("nanonets/Nanonets-OCR2-3B")

image = Image.open("doc.png")
inputs = processor(image, return_tensors="pt").to(model.device)
outputs = model.generate(**inputs, max_new_tokens=15000)
text = processor.decode(outputs[0])
```

### 2. Serve via vLLM
```bash
vllm serve nanonets/Nanonets-OCR2-3B
```
Then use OpenAI client with `http://localhost:8000/v1`

### 3. MLX on Apple Silicon
```bash
pip install mlx-vlm
python -m mlx_vlm.generate --model ibm-granite/granite-docling-258M-mlx \
  --image doc.png --prompt "Extract text"
```

### 4. GGUF for Local Deployment
```python
from transformers import AutoModelForCausalLM, AutoTokenizer

model = AutoModelForCausalLM.from_pretrained(
    "model-id",
    gguf_file="model.Q4_K_M.gguf"
)
```
Then run with `llama-server -hf model-id:Q4_K_M`

### 5. Stream Large Datasets
```python
from datasets import load_dataset

dataset = load_dataset("dataset-id", streaming=True)
# No disk space needed, trains on-the-fly
```

---

## Decision Tree: Which Model?

```
Need output format?
├─ DocTag → Granite-Docling-258M (258M, EN/JA/AR/ZH)
├─ HTML → Chandra (9B, 40+ lang) OR OlmOCR-2 (8B, EN-only)
├─ Markdown → PaddleOCR-VL (0.9B, 109 lang) OR dots.ocr (3B)
└─ JSON → Chandra or Nanonets-OCR2 (4B)

Need multilingual?
├─ Yes (40+ lang) → Chandra (9B, 83.1 score)
├─ Yes (100 lang) → PaddleOCR-VL (0.9B)
├─ Yes (32 lang) → Qwen3-VL (9B, flexible)
└─ English-only OK → OlmOCR-2 (8B, 82.3 score)

Constrained on size?
├─ <1B → PaddleOCR-VL (0.9B) OR LightOnOCR-1B (1B)
├─ <3B → dots.ocr (3B) OR DeepSeek-OCR (3B)
├─ 3-5B → Nanonets-OCR2 (4B) OR LightOnOCR-1B (1B)
└─ Anything goes → Chandra (9B, best score)

Deployment?
├─ Local (GPU) → Any model, use Transformers
├─ Local (CPU/Apple) → GGUF quantized models
├─ Cloud/Batch → vLLM + HF Jobs
└─ Quick POC → Granite-Docling (258M, fastest)
```

---

## Performance Benchmarks

### Quality (OlmOCR-Bench Score)
```
Chandra:         83.1 ± 0.9  (9B)
OlmOCR-2:        82.3 ± 1.1  (8B)
dots.ocr:        79.1 ± 1.0  (3B)
DeepSeek-OCR:    75.4 ± 1.0  (3B)
LightOnOCR-1B:   ~81+ (estimated, 1B)
```

### Speed (Pages/Day)
```
LightOnOCR-1B:   ~493,000 (H100)
DeepSeek-OCR:    ~200,000+ (A100 40GB)
OlmOCR-2:        ~178 $/M pages
```

### Memory (VRAM Required)
```
PaddleOCR-VL:    ~4GB (0.9B)
dots.ocr:        ~8GB (3B)
Nanonets:        ~12GB (4B)
Chandra:         ~20GB (9B, no LoRA)
OlmOCR-2:        ~16GB (8B, with vLLM optimization)
```

---

## Quantization Guide

### GGUF Quantization Types

| Format | Size Impact | Speed | Quality | Use Case |
|--------|-------------|-------|---------|----------|
| **Q4_K_M** | ~40% | Fast | Good | Default choice |
| **Q5_K_M** | ~50% | Medium | Better | When VRAM available |
| **Q6_K** | ~60% | Slower | Excellent | Quality-critical |
| **fp16** | ~100% | Slowest | Perfect | No compression needed |

### By Device

| Device | Quantization | Max Model |
|--------|--------------|-----------|
| **Mobile** | Q4_K_M or Q3_K | 1-3B |
| **Laptop (8GB)** | Q4_K_M | 3-7B |
| **GPU (16GB)** | Q4_K_M | 13B |
| **GPU (24GB+)** | fp16 | 13-70B |

---

## Fine-Tuning Guide

### Quick Commands

```bash
# Supervised Fine-Tuning
Fine-tune Qwen3-0.6B on my-dataset for instruction following.

# Direct Preference Optimization
Run DPO on my-prefs-data to align the SFT model.

# Math Reasoning (GRPO)
Train a math model using GRPO on openai/gsm8k based on Qwen3-0.6B.
```

### Cost Examples (HF Skills)

```
Qwen3-0.6B on t4-small:        ~$0.30 (20 min test)
Qwen3-3B on t4-medium:         ~$5-10 (full training)
Llama-7B on a10g-large+LoRA:   ~$20-40 (full training)
```

### Post-Training to GGUF

```
Convert my-fine-tuned-model to GGUF with Q4_K_M quantization.
Push to my-org/model-gguf.
```

---

## API Integration Examples

### OpenAI-Compatible (vLLM)

```python
from openai import OpenAI

client = OpenAI(base_url="http://localhost:8000/v1")
response = client.chat.completions.create(
    model="nanonets/Nanonets-OCR2-3B",
    messages=[{
        "role": "user",
        "content": [
            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{img}"}},
            {"type": "text", "text": "Extract text from this document"}
        ]
    }],
    max_tokens=15000
)
```

### Hugging Face Hub

```python
from transformers import pipeline

ocr = pipeline(
    "image-to-text",
    model="nanonets/Nanonets-OCR2-3B",
    device=0
)
result = ocr("document.png")
```

### Batch Processing (HF Jobs)

```bash
hf jobs uv run --flavor l4x1 \
  https://huggingface.co/datasets/uv-scripts/ocr/raw/main/nanonets-ocr.py \
  input-dataset output-dataset
```

---

## Common Configurations

### High-Throughput Streaming (256 workers, 64×H100)

```python
import pyarrow
import pyarrow.dataset
from datasets import load_dataset

fragment_scan_options = pyarrow.dataset.ParquetFragmentScanOptions(
    cache_options=pyarrow.CacheOptions(
        prefetch_limit=1,
        range_size_limit=128 << 20  # 128MB chunks
    ),
)

ds = load_dataset(
    "FineVisionMax",
    streaming=True,
    fragment_scan_options=fragment_scan_options
)
# Results: 2× throughput vs. local SSD, zero disk space
```

### Optimal Inference Settings

```python
# Transformers + Flash Attention (recommended)
model = AutoModelForImageTextToText.from_pretrained(
    "model-id",
    torch_dtype=torch.float16,        # Use fp16 for speed
    device_map="auto",                # Automatic placement
    attn_implementation="flash_attention_2"  # Critical for speed
)

# Processor settings
processor = AutoProcessor.from_pretrained("model-id")
inputs = processor(image, return_tensors="pt").to(model.device)

# Generation settings
outputs = model.generate(
    **inputs,
    max_new_tokens=15000,
    do_sample=False,                  # Greedy decoding for consistency
    num_beams=1                       # No beam search for speed
)
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Out of memory | Use GGUF with Q4_K_M quantization |
| Slow inference | Enable flash_attention_2, use vLLM |
| Quality issues | Try larger model or fine-tune |
| Missing languages | Use PaddleOCR-VL (109 lang) |
| Need layout preservation | Use Granite-Docling or Chandra |
| GPU not found | Set `device_map="cpu"` or check torch.cuda |

---

## Useful Links

- **Model Hub**: https://huggingface.co/models?sort=trending&search=ocr
- **vLLM Docs**: https://docs.vllm.ai
- **MLX-VLM**: https://github.com/Blaizzy/mlx-vlm
- **GGUF Format**: https://github.com/ggerganov/ggml
- **HF Jobs**: https://huggingface.co/docs/huggingface_hub/guides/jobs
- **Datasets Streaming**: https://huggingface.co/docs/datasets/stream

---

## Key Metrics Summary

### Cost per Million Pages (rough estimates)
- **Open source local**: $0-10 (one-time VRAM cost)
- **H100 cloud**: ~$178 (OlmOCR-2)
- **Proprietary APIs**: $500-2000+

### Quality Leaders
1. **Chandra** (83.1) - Best overall
2. **OlmOCR-2** (82.3) - Best for English batch
3. **dots.ocr** (79.1) - Good balance

### Efficiency Leaders
1. **LightOnOCR-1B** - 6× faster than dots.ocr
2. **PaddleOCR-VL** - Lightest (0.9B)
3. **Granite-Docling** - Smallest with rich output (258M)

