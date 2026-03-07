# Vision-Language Models & OCR Systems Comparison

## Executive Summary

This document provides a comprehensive comparison of Vision-Language Models (VLMs) and OCR systems for document intelligence, covering architectures, capabilities, and use case recommendations.

---

## 1. Model Architecture Overview

### 1.1 Traditional OCR vs VLM Approach

| Approach | Processing | Output | Limitation |
|----------|-----------|--------|------------|
| **Traditional OCR** | Bottom-up: Detection → Recognition → Reconstruction | Disjointed text boxes | Loses structural relationships |
| **VLM Approach** | Top-down: Global visual understanding → Autoregressive generation | Semantically-aware text | Computationally heavier |

Traditional systems (PaddleOCR v2, Tesseract) use cascaded pipelines:
1. Binarization
2. Layout analysis
3. Text line detection
4. Character recognition

VLMs perceive the image globally, understanding reading order and layout because the "next token" prediction depends on both textual context and 2D spatial position.

### 1.2 Dynamic Resolution Innovation

A critical advancement is handling of variable image resolution:

**Problem with Fixed Resolution:**
- Standard ViTs resize to fixed squares (224×224 or 336×336)
- Long receipts, wide spreadsheets lose high-frequency details
- Aspect ratio distortion introduces artifacts

**NaViT Solution (Qwen-VL, PaddleOCR-VL):**
- Divide image into patches of fixed size (14×14 pixels) at native resolution
- Patches packed into sequences with specialized attention masks
- Preserves detail in small fonts and complex layouts

---

## 2. Model Comparison Matrix

### 2.1 Open-Source VLMs for Document Intelligence

| Model | Parameters | Primary Strength | LaTeX Extraction | Table Parsing | Mac Support |
|-------|------------|-----------------|------------------|---------------|-------------|
| **olmOCR-2-7B** | 7B | Dense OCR, structural fidelity | Excellent | Very Good | llama.cpp (GGUF) |
| **Qwen2.5-VL-7B** | 7B | Visual reasoning, dynamic resolution | Very Good | Excellent | MLX, llama.cpp |
| **Qwen3-VL-32B** | 32B | Deep reasoning ("Thinking") | Excellent | Excellent | MLX (4-bit) |
| **DeepSeek-OCR** | 3B | Mathematical reasoning, compression | Excellent (95%) | Good | PyTorch/MPS |
| **Granite-Docling** | 258M | Structural extraction (DocTags) | Good | Excellent | MLX native |
| **PaddleOCR-VL** | 0.9B | Multilingual, NaViT encoder | Very Good | SOTA | CPU fallback |
| **ColPali/ColQwen2** | 3B | Visual retrieval (no OCR) | N/A | Visual | Embeddings only |

### 2.2 Cloud Provider Comparison

| Provider | Free Tier | LaTeX Support | Table Support | Limitation |
|----------|-----------|---------------|---------------|------------|
| **AWS Textract** | 1,000 pages/month (3 mo) | Natural language only | Good | Mathematical flattening |
| **Google Document AI** | ~400-500 pages | Poor (∫ → J) | Good | Symbol misinterpretation |
| **Azure AI Vision** | 500 pages/month | No native LaTeX | Good | Language specialist only |

---

## 3. Architecture Deep Dives

### 3.1 Qwen2.5-VL / Qwen3-VL

**Key Innovations:**
- **M-RoPE (Multimodal Rotary Positional Embeddings)**: Unified handling of 1D text, 2D images, 3D video
- **Naive Dynamic Resolution**: Native aspect ratio processing without downscaling
- **Visual Reasoning**: Can perform arithmetic verification on extracted content

**Capabilities:**
```
Question: "Is the total on this invoice consistent with the line items?"
→ Qwen extracts data, performs calculation, returns verification
```

**Speed on M-series Mac:**
- Qwen2.5-VL-7B (4-bit): 50-70 t/s
- Qwen3-VL-32B (4-bit): ~45 t/s with MLX

### 3.2 olmOCR-2-7B

A Qwen2.5-VL fine-tune optimized for document transcription:
- Trained as rigorous transcriber (no conversational filler)
- Accurate capture of tables, LaTeX formulas, reading order
- "Unit test trained" - optimized for structural fidelity

**Deployment:**
```bash
# GGUF format via llama.cpp
python -m llama_cpp.server \
  --model olmOCR-2-7B-Q4_K_M.gguf \
  --clip_model_path mmproj-olmOCR-2-7B-vision.gguf \
  --n_gpu_layers 99 \
  --n_ctx 8192 \
  --port 8081
```

### 3.3 DeepSeek-OCR

**Vision-as-Compression Architecture:**
- **DeepEncoder**: SAM-base (local detail) + CLIP-large (global semantic)
- **Decoder**: DeepSeek-3B-MoE
- Compression: 1024×1024 image → 256 vision tokens (10x reduction)

**Mathematical Reasoning:**
- Trained on ArXiv mathematical proofs
- Predicts correct LaTeX tokens even with blur
- "Knows" that `cos²θ` is likely in trigonometry context

**MPS Compatibility Patches:**
```python
# Force bfloat16 for Metal stability
DEVICE = "mps" if torch.backends.mps.is_available() else "cpu"
DTYPE = torch.bfloat16  # Avoids FP16 numerical overflows

model = AutoModel.from_pretrained(
    "deepseek-ai/DeepSeek-OCR",
    trust_remote_code=True,
    torch_dtype=DTYPE
).to(DEVICE)
```

### 3.4 Granite-Docling

**DocTags Output:**
```xml
<document>
  <title>Chapter 1: Introduction</title>
  <table>
    <row><cell>Data 1</cell><cell>Data 2</cell></row>
    <row><cell>Data 3</cell><cell>Data 4</cell></row>
  </table>
</document>
```

**Advantages:**
- 258M parameters - extremely fast and memory-efficient
- SigLIP2 encoder for better image-text alignment
- Trained specifically for structural preservation
- Native MLX support via `pip install "docling[mlx]"`

### 3.5 PaddleOCR-VL

**Architecture:**
- Visual Encoder: NaViT (native resolution)
- LLM Backbone: ERNIE-4.5-0.3B
- Alignment: MLP connector

**Strength:** Multilingual support, especially CJK languages

**Mac Limitation:** PaddlePaddle has limited Metal support; CPU fallback recommended for stability.

---

## 4. ColPali: Visual Document Retrieval

ColPali represents a paradigm shift - bypassing OCR entirely for retrieval tasks.

### 4.1 Late Interaction Architecture

Traditional dual encoders (CLIP) pool entire image into single vector, destroying spatial resolution. ColPali preserves granular representations:

**Vision Encoder (SigLIP-So400m):**
- Divides image into 32×32 grid = 1,024 patch embeddings
- Each patch is a vector in ℝ^128

**MaxSim Operator:**
$$S(Q, D) = \sum_{j=1}^{N_{tokens}} \max_{i=1}^{N_{patches}} (t_j \cdot p_i)$$

The `max` operation performs implicit object detection - finding which patch best matches each text token.

### 4.2 Alignment Pipeline

ColPali enables weakly-supervised training data generation:

1. **Input**: Manuscript image + loose transcription
2. **Forward Pass**: Get patch and token embeddings
3. **Interaction**: Compute dot product tensor [L, 32, 32]
4. **Aggregation**: Sum across tokens → line-level heatmap
5. **Bounding Box**: Upscale, threshold (Otsu), extract contours

**Result**: Automatic bounding box annotations without manual labeling.

---

## 5. Task-Specific Recommendations

### 5.1 By Document Type

| Document Type | Primary Model | Fallback | Rationale |
|---------------|---------------|----------|-----------|
| **Dense Text** | olmOCR-2-7B | Granite-Docling | Structural fidelity |
| **Mathematical Content** | DeepSeek-OCR | Qwen2.5-VL | Math reasoning priors |
| **Tables/Structured** | Granite-Docling | PaddleOCR-VL | DocTags preservation |
| **Diagrams/Charts** | Qwen3-VL | ColPali retrieval | Visual reasoning |
| **Historical Manuscripts** | ColPali → Qwen2-VL | - | Weak supervision alignment |
| **Multilingual** | PaddleOCR-VL | Azure API | CJK/European support |

### 5.2 By Hardware

| Hardware | Recommended Stack |
|----------|-------------------|
| **Mac M1/M2 (16GB)** | Granite-Docling (MLX), olmOCR (GGUF Q4) |
| **Mac M3/M4 (32GB+)** | + Qwen2.5-VL-7B (MLX 4-bit) |
| **Mac M3 Max (64GB+)** | + Qwen3-VL-32B (MLX 4-bit), full fleet |
| **NVIDIA RTX 3090** | Qwen2.5-VL-7B (vLLM), DeepSeek-OCR |
| **NVIDIA A100** | Qwen3-VL-72B, high-throughput batch |

---

## 6. Performance Benchmarks

### 6.1 Extraction Accuracy (Leaving Cert Math Paper)

| Engine | Integral Symbol | Fraction Structure | Diagram Understanding |
|--------|-----------------|-------------------|----------------------|
| Google Document AI | `Sok e 5x dx = 9` ❌ | Lost fraction bar ❌ | Labels only ❌ |
| AWS Textract | Natural language ⚠️ | Partial ⚠️ | Labels only ❌ |
| olmOCR | `\int_{0}^{k} e^{5x} dx = 9` ✓ | Preserved ✓ | N/A |
| Qwen3-VL | `\int_{0}^{k} e^{5x} dx = 9` ✓ | Preserved ✓ | Geometric relations ✓ |

### 6.2 Processing Speed (per page)

| Model | M3 Max (MLX) | Docker CPU | A100 (vLLM) |
|-------|-------------|------------|-------------|
| Granite-Docling | <1s | 3-5s | <0.5s |
| olmOCR-2-7B | 2-3s | 20-30s | <1s |
| Qwen2.5-VL-7B | 3-5s | 30-45s | 1-2s |
| DeepSeek-OCR | 2-3s | 15-25s | <1s |
| PaddleOCR-VL | N/A (CPU: 5s) | 10-30s | <1s |

---

## 7. Integration Patterns

### 7.1 Provider-Agnostic Router

```python
def route_page(page, quotas):
    """Route pages to optimal engine based on content and quotas."""

    if is_text_heavy(page) and quotas['azure'] > 0:
        return 'azure'  # High fidelity for text

    if is_tabular(page):
        return 'granite-docling'  # Local structural extraction

    if has_math_or_diagrams(page):
        return 'qwen-vl'  # Visual reasoning

    return 'olmocr'  # Default dense OCR
```

### 7.2 Bilingual Consensus Module

For multilingual documents (e.g., English/Irish exams):

```python
def verify_extraction(eng_result, irl_result):
    """Cross-validate mathematical constants across languages."""

    eng_latex = extract_latex(eng_result)
    irl_latex = extract_latex(irl_result)

    # Mathematical constants should match
    if eng_latex.coefficients != irl_latex.coefficients:
        return flag_for_review(eng_result, irl_result)

    return merge_results(eng_result, irl_result)
```

---

## 8. Cost Analysis (10,000 Pages)

| Engine | Deployment | Token Cost | Energy Cost | Total |
|--------|------------|------------|-------------|-------|
| Granite-Docling | Local Mac | $0 | ~$0.10 | **~$0.10** |
| Qwen2.5-VL | Local Mac | $0 | ~$0.50 | **~$0.50** |
| olmOCR | Local Mac | $0 | ~$0.30 | **~$0.30** |
| AWS Textract | Cloud | ~$15.00 | N/A | **~$15.00** |
| Google Doc AI | Cloud | ~$15.00 | N/A | **~$15.00** |
| GLM-4.5v | Cloud API | ~$12.00 | N/A | **~$12.00** |

**Conclusion**: Local inference with MLX-optimized models provides 100x cost reduction at comparable quality for document intelligence tasks.
