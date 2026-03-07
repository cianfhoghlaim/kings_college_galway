# Multimodal Document Intelligence & Heritage Digitization

This directory consolidates research on vision-language models (VLMs), OCR systems, and deployment strategies for document understanding, with special focus on Gaelic heritage preservation and mathematical content extraction.

## Overview

The research covers the complete pipeline from document ingestion to structured output, including:
- **Vision-Language Models**: Qwen2.5-VL, Qwen3-VL, DeepSeek-OCR, olmOCR, PaddleOCR-VL
- **Visual Document Retrieval**: ColPali for bypassing traditional OCR
- **Apple Silicon Optimization**: MLX framework, local deployment strategies
- **Heritage Digitization**: Dúchas.ie Schools' Collection, Gaelic manuscript alignment

## Documents in this Category

| Document | Focus | Key Technologies |
|----------|-------|------------------|
| `vlm-ocr-comparison.md` | Model architectures and capabilities | Qwen-VL, DeepSeek-OCR, Granite-Docling |
| `gaelic-heritage-pipeline.md` | Irish manuscript digitization | ColPali alignment, Unsloth fine-tuning |
| `apple-silicon-deployment.md` | Local inference on Mac | MLX, llama.cpp, Metal optimization |
| `orchestration-infrastructure.md` | Model serving and routing | LiteLLM, Llama-Swap, MCP, vLLM |

## Key Architectural Decisions

### 1. Model Selection by Task

| Task | Recommended Model | Rationale |
|------|------------------|-----------|
| Dense OCR (Text) | olmOCR-2-7B | Unit test trained, structural fidelity |
| Visual Reasoning | Qwen2.5-VL / Qwen3-VL | Dynamic resolution, multimodal understanding |
| Table Extraction | Granite-Docling (258M) | DocTags output, semantic structure |
| Math Content | DeepSeek-OCR | Mathematical reasoning priors |
| Gaelic Manuscripts | ColPali → Qwen2-VL | Weak supervision alignment |

### 2. Deployment Strategy

```
Hybrid Architecture (Mac/GPU):
├── MLX Backend (Native Apple Silicon)
│   ├── Granite-Docling (MLX) - <1s per page
│   ├── Qwen2.5-VL-7B-4bit - 50-70 t/s
│   └── olmOCR (MLX) - Fast OCR
├── llama.cpp Backend (Universal)
│   ├── GGUF quantized models
│   ├── Metal/CUDA acceleration
│   └── mmproj for VLMs
└── Docker Baseline (Comparison only)
    └── PaddleOCR-VL (CPU fallback)
```

### 3. ColPali for Weakly-Supervised Alignment

Revolutionary approach for heritage digitization:
1. **Bypass OCR**: Multi-vector embeddings directly from page images
2. **MaxSim Operator**: Implicit object detection via token-patch similarity
3. **Heatmap Generation**: Automatic bounding box derivation
4. **Fine-tuning Dataset**: Create aligned image-text pairs without manual annotation

## Source Files Consolidated

This category merges content from:
- `Aligning Gaelic Script for QwenVL Finetuning.md`
- `Finetuning Qwen3-VL for Gaelic OCR.md`
- `Handwriting Recognition and Dataset Creation.md`
- `LLM and OCR Deployment Research.md`
- `Local macOS MLX_MPS LLM Workflow.md`
- `Setting Up Local LLM Services on Mac.md`
- `Open-Source VLMs For PDF Extraction.md`

## Quick Reference

### Hardware Requirements

| Model | VRAM (4-bit) | Speed (M3 Max) |
|-------|-------------|----------------|
| Granite-Docling (258M) | ~1GB | <1s/page |
| olmOCR-2-7B | ~5GB | 2-3s/page |
| Qwen2.5-VL-7B | ~5GB | 50-70 t/s |
| Qwen2.5-VL-32B | ~19GB | 20-30 t/s |
| DeepSeek-OCR (3B) | ~6GB | 2-3s/page |

### Quantization Trade-offs

| Quant | Bits | Memory (70B) | Perplexity |
|-------|------|-------------|------------|
| F16/BF16 | 16 | ~140GB | Baseline |
| Q8_0 | 8 | ~75GB | Negligible |
| Q4_K_M | 4 | ~42GB | ~1-2% |
| IQ2_XXS | 2 | ~22GB | Significant |

### Gaelic Script Challenges

- **Punctum Delens**: Lenition dot above consonants (ḃ, ċ, ḋ)
- **Tironian Note (⁊)**: Symbol for "agus" (and)
- **Script Morphology**: Long 'r', long 's' (ſ), uncial 'd'
- **Dialectal Variations**: Connacht, Munster, Ulster spelling differences

### Cost Analysis (10,000 pages)

| Engine | Deployment | Cost |
|--------|-----------|------|
| Granite-Docling | Local Mac | ~$0.10 |
| Qwen2.5-VL | Local Mac | ~$0.50 |
| GLM-4.5v | Cloud API | ~$12.00 |
