# HuggingFace OCR & Vision-Language Models - Complete Analysis

## Overview

This directory contains comprehensive documentation and code examples for modern OCR (Optical Character Recognition) and vision-language models from Hugging Face, with a focus on cutting-edge open-weight models and their integration patterns.

## Generated Analysis Documents

### 1. **ANALYSIS_SUMMARY.md** (20 KB)
**Comprehensive technical reference** covering:
- Complete model comparison table (9 models: Chandra, OlmOCR-2, PaddleOCR-VL, dots.ocr, DeepSeek-OCR, Granite-Docling, Nanonets-OCR2, Qwen3-VL, LightOnOCR)
- Model capabilities and output format specifications
- Integration frameworks: vLLM, Transformers, MLX, GGUF/llama.cpp
- Fine-tuning approaches: SFT, DPO, GRPO with hardware mapping
- Dataset strategies and benchmark comparisons
- Advanced patterns: visual document retrieval, document Q&A
- Code snippets and implementation details

**Use this for:** Deep technical understanding, architecture decisions, complete integration patterns

### 2. **QUICK_REFERENCE.md** (8.2 KB)
**Fast lookup guide** with:
- Models ranked by use case (quality, speed, multilingual, etc.)
- Decision tree for model selection
- Quick start integration examples (5 patterns)
- Performance benchmarks and cost analysis
- Quantization guide with device recommendations
- API integration examples (OpenAI-compatible, HF Hub, Batch)
- Troubleshooting table

**Use this for:** Quick lookup, rapid prototyping, decision-making

## Key Findings

### Top Performing Models

| Rank | Model | Quality | Size | Highlights |
|------|-------|---------|------|-----------|
| 1 | **Chandra** | 83.1 ± 0.9 | 9B | Best overall, 40+ languages |
| 2 | **OlmOCR-2** | 82.3 ± 1.1 | 8B | Optimized batch processing |
| 3 | **LightOnOCR-1B** | ~81 | 1B | 6× faster, incredible efficiency |
| 4 | **dots.ocr** | 79.1 ± 1.0 | 3B | Good balance, grounding support |
| 5 | **DeepSeek-OCR** | 75.4 ± 1.0 | 3B | 100 languages, general visual understanding |

### Integration Approaches

1. **Local GPU**: Transformers + Flash Attention
2. **Local Apple Silicon**: MLX-VLM
3. **Quantized Local**: GGUF + llama.cpp
4. **Cloud Serving**: vLLM (OpenAI-compatible API)
5. **Batch Processing**: HF Jobs + vLLM + streaming datasets

### Cost Efficiency

- **Open source (local)**: $0-10 per million pages (one-time)
- **Cloud H100**: ~$178 per million pages
- **Proprietary APIs**: $500-2000+ per million pages

## What's in This Directory

### Original Documents

1. **Supercharge your OCR Pipelines with Open Models.md**
   - Comprehensive OCR model landscape (2025)
   - Model comparison and evaluation
   - Local and remote deployment options
   - Document AI beyond OCR

2. **We Got Claude to Fine-Tune an Open Source LLM.md**
   - HF Skills integration with Claude
   - SFT, DPO, GRPO training methods
   - Hardware selection and cost mapping
   - GGUF conversion workflow

3. **Streaming datasets_ 100x More Efficient.md**
   - 100x improvement in data loading
   - Streaming from Hub vs. S3
   - Parquet prefetching and optimization
   - Large-scale training patterns (256 workers, 64×H100)

4. **gguf.md**
   - GGUF format overview
   - Quantization types
   - Loading GGUF models with Transformers

5. **transformers.md**
   - Transformers.js documentation
   - Browser-based model inference
   - Client-side vs. server-side examples

6. **convert_hf_to_gguf.py** (480+ KB)
   - Complete GGUF conversion utility
   - Supports Qwen (all versions), Llama, Mistral, Phi, etc.
   - Quantization handling
   - Multimodal model support

### Generated Reference Documents

1. **ANALYSIS_SUMMARY.md** - Complete technical reference
2. **QUICK_REFERENCE.md** - Fast lookup guide
3. **README_ANALYSIS.md** - This file

## Quick Start Examples

### Load and Infer
```python
from transformers import AutoProcessor, AutoModelForImageTextToText
from PIL import Image

model = AutoModelForImageTextToText.from_pretrained(
    "nanonets/Nanonets-OCR2-3B",
    device_map="auto",
    attn_implementation="flash_attention_2"
)
processor = AutoProcessor.from_pretrained("nanonets/Nanonets-OCR2-3B")

image = Image.open("document.png")
inputs = processor(image, return_tensors="pt").to(model.device)
outputs = model.generate(**inputs, max_new_tokens=15000)
text = processor.decode(outputs[0])
```

### Serve with vLLM
```bash
vllm serve nanonets/Nanonets-OCR2-3B
# Then use OpenAI client with base_url="http://localhost:8000/v1"
```

### Stream Large Datasets
```python
from datasets import load_dataset

# No disk space needed - streams from Hub
dataset = load_dataset("dataset-id", streaming=True)

# For high-throughput training with buffering
import pyarrow.dataset
fragment_scan_options = pyarrow.dataset.ParquetFragmentScanOptions(
    cache_options=pyarrow.CacheOptions(
        prefetch_limit=1,
        range_size_limit=128 << 20
    )
)
```

### GGUF Quantization
```python
from transformers import AutoModelForCausalLM, AutoTokenizer

model = AutoModelForCausalLM.from_pretrained(
    "model-id",
    gguf_file="model.Q4_K_M.gguf",
    dtype=torch.float32
)
# Q4_K_M recommended for most use cases
```

## Model Selection Guide

**Choose by output format:**
- DocTag: Granite-Docling-258M (258M params, compact)
- HTML/Markdown: Chandra (9B, best quality) or OlmOCR-2 (8B, English)
- JSON: Chandra, Nanonets-OCR2
- Flexible: Qwen3-VL (9B, 32 languages)

**Choose by language support:**
- 100+ languages: PaddleOCR-VL (0.9B)
- 40+ languages: Chandra (9B)
- 32 languages: Qwen3-VL (9B)
- English only: OlmOCR-2 (8B)

**Choose by size constraint:**
- <1B: PaddleOCR-VL (0.9B) or LightOnOCR-1B (1B)
- <3B: dots.ocr (3B) or DeepSeek-OCR (3B)
- <5B: Nanonets-OCR2 (4B)
- Best quality: Chandra (9B)

## Advanced Capabilities

### Document Understanding Beyond OCR
- **Visual Document Retrieval**: ColPali models for PDF search
- **Document Q&A**: Qwen3-VL for direct document questions
- **Multimodal RAG**: Combine retrievers + VLMs
- **Layout Preservation**: DocTag format with Granite-Docling

### Fine-Tuning & Adaptation
- **SFT**: Domain-specific instruction tuning
- **DPO**: Preference alignment
- **GRPO**: Reinforcement learning for verifiable tasks
- **LoRA**: Efficient training for 7B+ models

### Infrastructure
- **Streaming**: Train on multi-TB datasets without downloading
- **Batch Processing**: HF Jobs for cost-effective scale
- **Quantization**: 4-8× compression with minimal quality loss
- **Multi-platform**: GPU, Apple Silicon, CPU, Browser (Transformers.js)

## Performance Metrics

### Quality (OlmOCR-Bench)
- Chandra: 83.1 ± 0.9 (best)
- OlmOCR-2: 82.3 ± 1.1
- LightOnOCR-1B: ~81 (estimated)
- dots.ocr: 79.1 ± 1.0
- DeepSeek-OCR: 75.4 ± 1.0

### Throughput
- LightOnOCR-1B: 493k pages/day (H100)
- DeepSeek-OCR: 200k+ pages/day (A100 40GB)
- Cost: ~$178 per million pages on H100

### Memory Efficiency
- PaddleOCR-VL: 4GB VRAM (0.9B)
- LightOnOCR-1B: 6GB VRAM
- Nanonets-OCR2: 12GB VRAM (4B)
- Chandra: 20GB VRAM (9B, no LoRA needed)

## Recommended Reading Order

1. **QUICK_REFERENCE.md** - Get overview and make initial decisions
2. **ANALYSIS_SUMMARY.md** - Deep dive into specific areas
3. **Original markdown files** - Technical details and explanations
4. **convert_hf_to_gguf.py** - Reference for model conversion

## Key Insights

1. **Modern OCR is VLM-based**: Fine-tuned vision-language models significantly outperform traditional OCR
2. **Output format matters**: Choose between DocTag (layout), HTML (structure), Markdown (LLM), JSON (data)
3. **Multilingual is standard**: Most models support 20-100+ languages natively
4. **Efficiency leaders**: LightOnOCR-1B and PaddleOCR-VL punch way above their weight class
5. **Local deployment ready**: GGUF quantization enables efficient on-device inference
6. **Cost-effective at scale**: Open models are 10-100x cheaper than proprietary APIs
7. **Streaming is critical**: 100x reduction in requests enables massive dataset training
8. **Fine-tuning is accessible**: SFT, DPO, GRPO are straightforward with HF Skills
9. **Platform diversity**: GPU, Apple Silicon, Cloud, Browser - all supported
10. **Document AI evolving**: Visual retrieval + VLM QA outperforms OCR → LLM chains

## Useful Resources

- HF Model Hub: https://huggingface.co/models?sort=trending&search=ocr
- vLLM Documentation: https://docs.vllm.ai
- MLX-VLM: https://github.com/Blaizzy/mlx-vlm
- GGUF Format: https://github.com/ggerganov/ggml
- HF Datasets: https://huggingface.co/docs/datasets
- HF Jobs: https://huggingface.co/docs/huggingface_hub/guides/jobs

## File Manifest

```
huggingface/
├── README_ANALYSIS.md                      (This file)
├── ANALYSIS_SUMMARY.md                     (20 KB, comprehensive reference)
├── QUICK_REFERENCE.md                      (8.2 KB, quick lookup)
├── Supercharge your OCR Pipelines...md     (31 KB, main OCR guide)
├── We Got Claude to Fine-Tune...md         (17 KB, fine-tuning guide)
├── Streaming datasets...md                 (12 KB, data efficiency)
├── gguf.md                                 (2 KB, GGUF format)
├── transformers.md                         (88 KB, Transformers.js docs)
└── convert_hf_to_gguf.py                   (480+ KB, conversion utility)
```

## Summary

This directory contains everything needed to understand and deploy modern OCR models:
- **9 production-ready models** with detailed comparisons
- **5 integration approaches** for different use cases
- **Complete code examples** for all major patterns
- **Benchmark data** for informed model selection
- **Cost analysis** for budget planning
- **Advanced techniques** for optimization and customization

Start with QUICK_REFERENCE.md for immediate answers, or ANALYSIS_SUMMARY.md for comprehensive understanding.

---

**Last Updated**: December 6, 2025
**Generated for**: HuggingFace examples directory analysis
**Models covered**: 9 production-ready OCR models, plus foundations (Qwen, Llama, etc.)
**Integration methods**: 5 major approaches (Transformers, vLLM, MLX, GGUF, Streaming)
