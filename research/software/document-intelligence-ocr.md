# Document Intelligence & OCR

Comprehensive guide to Vision-Language Models, OCR systems, and Gaelic heritage digitization pipelines.

---

## 1. VLM vs Traditional OCR

### 1.1 Architecture Comparison

| Approach | Processing | Output | Limitation |
|----------|-----------|--------|------------|
| **Traditional OCR** | Bottom-up: Detection → Recognition → Reconstruction | Disjointed text boxes | Loses structural relationships |
| **VLM Approach** | Top-down: Global visual understanding → Autoregressive generation | Semantically-aware text | Computationally heavier |

**Traditional Pipeline (PaddleOCR, Tesseract):**
1. Binarization
2. Layout analysis
3. Text line detection
4. Character recognition

**VLM Advantage:** Perceives image globally, understanding reading order and layout because "next token" prediction depends on both textual context and 2D spatial position.

### 1.2 Dynamic Resolution Innovation

**Problem with Fixed Resolution:**
- Standard ViTs resize to 224×224 or 336×336
- Long receipts, wide spreadsheets lose detail
- Aspect ratio distortion introduces artifacts

**NaViT Solution (Qwen-VL, PaddleOCR-VL):**
- Divide image into 14×14 patches at native resolution
- Patches packed into sequences with attention masks
- Preserves detail in small fonts and complex layouts

---

## 2. Model Comparison

### 2.1 Open-Source VLMs for Document Intelligence

| Model | Params | Primary Strength | LaTeX | Tables | Mac Support |
|-------|--------|------------------|-------|--------|-------------|
| **olmOCR-2-7B** | 7B | Dense OCR, structural fidelity | Excellent | Very Good | llama.cpp (GGUF) |
| **Qwen2.5-VL-7B** | 7B | Visual reasoning, dynamic resolution | Very Good | Excellent | MLX, llama.cpp |
| **Qwen3-VL-32B** | 32B | Deep reasoning ("Thinking") | Excellent | Excellent | MLX (4-bit) |
| **DeepSeek-OCR** | 3B | Mathematical reasoning, compression | Excellent (95%) | Good | PyTorch/MPS |
| **Granite-Docling** | 258M | Structural extraction (DocTags) | Good | Excellent | MLX native |
| **PaddleOCR-VL** | 0.9B | Multilingual, NaViT encoder | Very Good | SOTA | CPU fallback |
| **ColPali/ColQwen2** | 3B | Visual retrieval (no OCR) | N/A | Visual | Embeddings only |

### 2.2 Cloud Provider Comparison

| Provider | Free Tier | LaTeX | Tables | Limitation |
|----------|-----------|-------|--------|------------|
| **AWS Textract** | 1,000 pages/month (3 mo) | Natural language only | Good | Math flattening |
| **Google Document AI** | ~400-500 pages | Poor (∫ → J) | Good | Symbol misinterpretation |
| **Azure AI Vision** | 500 pages/month | No native | Good | Language specialist only |

---

## 3. Model Deep Dives

### 3.1 Qwen2.5-VL / Qwen3-VL

**Key Innovations:**
- **M-RoPE**: Multimodal Rotary Positional Embeddings - unified 1D text, 2D images, 3D video
- **Naive Dynamic Resolution**: Native aspect ratio without downscaling
- **Visual Reasoning**: Arithmetic verification on extracted content

**Speed on M-series Mac:**
- Qwen2.5-VL-7B (4-bit): 50-70 t/s
- Qwen3-VL-32B (4-bit): ~45 t/s with MLX

### 3.2 olmOCR-2-7B

Qwen2.5-VL fine-tune optimized for document transcription:
- Rigorous transcriber (no conversational filler)
- Accurate tables, LaTeX, reading order
- "Unit test trained" for structural fidelity

**Deployment:**
```bash
python -m llama_cpp.server \
  --model olmOCR-2-7B-Q4_K_M.gguf \
  --clip_model_path mmproj-olmOCR-2-7B-vision.gguf \
  --n_gpu_layers 99 \
  --n_ctx 8192 \
  --port 8081
```

### 3.3 DeepSeek-OCR

**Vision-as-Compression Architecture:**
- **DeepEncoder**: SAM-base (local) + CLIP-large (global)
- **Decoder**: DeepSeek-3B-MoE
- Compression: 1024×1024 → 256 vision tokens (10x reduction)

**MPS Compatibility:**
```python
DEVICE = "mps" if torch.backends.mps.is_available() else "cpu"
DTYPE = torch.bfloat16  # Avoids FP16 overflows

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
  </table>
</document>
```

- 258M parameters - extremely fast
- SigLIP2 encoder for image-text alignment
- Native MLX: `pip install "docling[mlx]"`

---

## 4. ColPali: Visual Document Retrieval

Bypasses OCR for retrieval tasks using late interaction.

### 4.1 Architecture

```
Manuscript Page → SigLIP Encoder → Patch Embeddings (32×32×128)
                                           ↓
Transcription Line → Gemma Encoder → Token Embeddings (L×128)
                                           ↓
                     MaxSim Interaction → Similarity Map
                                           ↓
                     Threshold + Contours → Bounding Box
```

### 4.2 MaxSim Localization

$$S(Q, D) = \sum_{j=1}^{N_{tokens}} \max_{i=1}^{N_{patches}} (t_j \cdot p_i)$$

For each token, find patch with highest similarity - that patch contains the visual representation.

### 4.3 Alignment for Training Data

ColPali enables weakly-supervised bounding box generation:
1. Input: Manuscript image + loose transcription
2. Forward Pass: Get patch and token embeddings
3. Interaction: Compute dot product tensor [L, 32, 32]
4. Aggregation: Sum across tokens → line-level heatmap
5. Bounding Box: Upscale, Otsu threshold, extract contours

---

## 5. Gaelic Heritage Pipeline

### 5.1 The Alignment Challenge

Historical collections (Dúchas.ie, RIA) contain:
- High-resolution manuscript images in one silo
- TEI-XML transcriptions in another silo
- Page-level metadata only (no coordinates)

**Typical XML:**
```xml
<pb n="117"/>
<p>
  <lb/>Tá sé páidre go bfuil cipic...
  <lb/>Deirfear gur b'iad na Lochlannaigh...
</p>
```

The `<lb/>` tags indicate line breaks but carry no spatial coordinates.

### 5.2 Gaelic Script Challenges

| Feature | Description | OCR Challenge |
|---------|-------------|---------------|
| **Script Morphology** | Long 'r' (r rotunda), long 's' (ſ) | Resembles other letters |
| **Punctum Delens** | Dot above for lenition (ḃ, ċ, ḋ) | Few pixels radically alter meaning |
| **Tironian Note (⁊)** | Symbol for "agus" (and) | Often fails tokenization |
| **Baseline Drift** | Handwriting violates lined paper | Adjacent lines touch |

### 5.3 ColPali Alignment Implementation

```python
from colpali_engine.models import ColQwen2Processor, ColQwen2

def generate_alignment_heatmap(image, text_query, model, processor):
    inputs = processor(text=[text_query], images=[image], return_tensors="pt")
    inputs = {k: v.to(model.device) for k, v in inputs.items()}

    with torch.no_grad():
        out = model(**inputs)
        patch_embeddings = out.visual_embeddings  # [1, 1024, 128]
        query_embeddings = out.text_embeddings    # [1, L, 128]

    interaction = torch.einsum("bnd,bmd->bnm", query_embeddings, patch_embeddings)
    heatmap_flat = interaction.sum(dim=1).squeeze()

    side = int(np.sqrt(heatmap_flat.shape[0]))
    return heatmap_flat.view(side, side).cpu().numpy()
```

### 5.4 Heatmap to Bounding Box

```python
def heatmap_to_bbox(heatmap, original_size, min_area=500):
    # Upscale to original resolution
    scale_h = original_size[0] / heatmap.shape[0]
    scale_w = original_size[1] / heatmap.shape[1]
    heatmap_full = zoom(heatmap, (scale_h, scale_w), order=3)

    # Normalize and binarize
    heatmap_norm = ((heatmap_full - heatmap_full.min()) /
                    (heatmap_full.max() - heatmap_full.min()) * 255).astype(np.uint8)
    _, binary = cv2.threshold(heatmap_norm, 0, 255,
                               cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    # Morphological dilation for handwriting
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 3))
    dilated = cv2.dilate(binary, kernel, iterations=2)

    # Find contours and select largest valid
    contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL,
                                    cv2.CHAIN_APPROX_SIMPLE)
    # ... filter by area and aspect ratio
```

---

## 6. Fine-Tuning Qwen2-VL with Unsloth

### 6.1 Why Unsloth?

| Benefit | Detail |
|---------|--------|
| Memory Efficiency | 4-bit QLoRA with minimal precision loss |
| Speed | 2x faster via Triton kernels |
| Vision Support | Native Qwen2-VL data collation |
| VRAM | 7-8B model: ~6-7GB (consumer RTX 3060+) |

### 6.2 Dataset Formatting

**Format A: Dense OCR (Crop-Based)**
```json
{
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "image", "image": "file:///path/to/crop_line_42.jpg"},
        {"type": "text", "text": "Transcribe this Gaelic manuscript line exactly."}
      ]
    },
    {
      "role": "assistant",
      "content": [
        {"type": "text", "text": "Agus do bhí an rí ⁊ na saighdiúirí..."}
      ]
    }
  ]
}
```

**Format B: Visual Grounding (Full Page)**
```json
{
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "image", "image": "file:///path/to/full_page.jpg"},
        {"type": "text", "text": "Find the location of the text 'Agus do bhí an rí'"}
      ]
    },
    {
      "role": "assistant",
      "content": [
        {"type": "text", "text": "<|box_start|>(300,450),(350,800)<|box_end|>"}
      ]
    }
  ]
}
```

### 6.3 Training Configuration

```python
from unsloth import FastVisionModel
from trl import SFTTrainer

model, tokenizer = FastVisionModel.from_pretrained(
    "unsloth/Qwen2-VL-7B-Instruct",
    load_in_4bit=True,
    max_seq_length=2048,
)

model = FastVisionModel.get_peft_model(
    model,
    finetune_vision_layers=True,   # Essential for domain adaptation
    finetune_language_layers=True,
    r=16,
    lora_alpha=16,
    lora_dropout=0.05,
    target_modules=[
        "q_proj", "k_proj", "v_proj", "o_proj",
        "gate_proj", "up_proj", "down_proj",
    ],
)

training_args = TrainingArguments(
    output_dir="./gaelic-qwen-vl",
    per_device_train_batch_size=2,
    gradient_accumulation_steps=4,
    learning_rate=2e-4,
    num_train_epochs=3,
    optim="adamw_8bit",
    bf16=True,
)
```

### 6.4 Critical Hyperparameters

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| `finetune_vision_layers` | True | Vision encoder never saw Gaelic script |
| `finetune_language_layers` | True | Learn Gaelic syntax and abbreviations |
| `lora_rank` | 16 | Balance plasticity vs memory |
| `max_seq_length` | 2048 | Visual tokens + transcription |
| `optim` | adamw_8bit | 75% optimizer memory reduction |

### 6.5 Handling Tironian Note (⁊)

```python
# Add Tironian note to tokenizer vocabulary
tokenizer.add_tokens(['⁊'])
model.resize_token_embeddings(len(tokenizer))

# Initialize embedding from 'agus' (semantic equivalent)
with torch.no_grad():
    agus_ids = tokenizer.encode('agus', add_special_tokens=False)
    agus_embedding = model.get_input_embeddings().weight[agus_ids].mean(dim=0)
    model.get_input_embeddings().weight[-1] = agus_embedding
```

---

## 7. Task-Specific Recommendations

### 7.1 By Document Type

| Document | Primary Model | Fallback | Rationale |
|----------|---------------|----------|-----------|
| **Dense Text** | olmOCR-2-7B | Granite-Docling | Structural fidelity |
| **Mathematical** | DeepSeek-OCR | Qwen2.5-VL | Math reasoning priors |
| **Tables/Structured** | Granite-Docling | PaddleOCR-VL | DocTags preservation |
| **Diagrams/Charts** | Qwen3-VL | ColPali retrieval | Visual reasoning |
| **Historical Manuscripts** | ColPali → Qwen2-VL | - | Weak supervision |
| **Multilingual** | PaddleOCR-VL | Azure API | CJK/European |

### 7.2 By Hardware

| Hardware | Recommended Stack |
|----------|-------------------|
| **Mac M1/M2 (16GB)** | Granite-Docling (MLX), olmOCR (GGUF Q4) |
| **Mac M3/M4 (32GB+)** | + Qwen2.5-VL-7B (MLX 4-bit) |
| **Mac M3 Max (64GB+)** | + Qwen3-VL-32B (MLX 4-bit), full fleet |
| **NVIDIA RTX 3090** | Qwen2.5-VL-7B (vLLM), DeepSeek-OCR |
| **NVIDIA A100** | Qwen3-VL-72B, high-throughput batch |

---

## 8. Evaluation Metrics

### 8.1 Character Error Rate (CER)

Critical for Gaelic - missed punctum delens or incorrect Tironian expansion = error.

```python
def calculate_cer(predictions, references):
    total_chars = 0
    total_errors = 0

    for pred, ref in zip(predictions, references):
        pred = unicodedata.normalize('NFC', pred)
        ref = unicodedata.normalize('NFC', ref)
        total_chars += len(ref)
        total_errors += editdistance.eval(pred, ref)

    return total_errors / total_chars
```

### 8.2 Grounding IoU

For bounding box task, measure Intersection over Union:

$$\text{IoU} = \frac{\text{Area of Overlap}}{\text{Area of Union}}$$

Threshold: IoU > 0.5 is correct detection.

---

## 9. Cost Analysis (10,000 Pages)

| Engine | Deployment | Token Cost | Total |
|--------|------------|------------|-------|
| Granite-Docling | Local Mac | $0 | **~$0.10** |
| Qwen2.5-VL | Local Mac | $0 | **~$0.50** |
| olmOCR | Local Mac | $0 | **~$0.30** |
| AWS Textract | Cloud | ~$15.00 | **~$15.00** |
| Google Doc AI | Cloud | ~$15.00 | **~$15.00** |

**Conclusion:** Local inference with MLX-optimized models provides 100x cost reduction.

---

## 10. Pipeline Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    DATA PREPARATION                              │
│  1. Parse TEI-XML transcriptions into line units                │
│  2. Handle Tironian notes (query substitution)                  │
│  3. Normalize Unicode (NFC)                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    COLPALI ALIGNMENT                             │
│  1. Generate similarity maps for each line                      │
│  2. Upscale heatmaps to original resolution                     │
│  3. Otsu threshold + morphological ops                          │
│  4. Extract bounding boxes with sanity checks                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DATASET CREATION                              │
│  1. Crop images using bounding boxes                            │
│  2. Format as JSONL (crop-based OCR + grounding)                │
│  3. Pad extreme aspect ratios                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    UNSLOTH FINE-TUNING                          │
│  1. Load Qwen2-VL with 4-bit quantization                       │
│  2. Apply LoRA to vision + language layers                      │
│  3. Train with UnslothVisionDataCollator                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DEPLOYMENT                                    │
│  1. Export to GGUF for llama.cpp                                │
│  2. Or serve via MLX on Apple Silicon                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## References

- ColPali: https://github.com/illuin-tech/colpali
- Qwen2-VL: https://huggingface.co/Qwen/Qwen2-VL-7B-Instruct
- Unsloth: https://github.com/unslothai/unsloth
- olmOCR: https://github.com/allenai/olmocr
- Granite-Docling: https://github.com/IBM/granite-docling
- Dúchas API: https://docs.gaois.ie/en/data/duchas/v0.6/api
