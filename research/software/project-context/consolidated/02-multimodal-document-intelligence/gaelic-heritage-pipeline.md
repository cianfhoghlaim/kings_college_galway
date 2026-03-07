# Gaelic Heritage Digitization Pipeline

## Executive Summary

This document details an automated pipeline for digitizing historical Gaelic manuscripts using ColPali for weakly-supervised alignment and Qwen2-VL fine-tuning via Unsloth. The approach addresses the fundamental challenge of creating training data without manual bounding box annotation.

---

## 1. The Alignment Challenge

### 1.1 The XML-Image Disconnect

Historical Gaelic collections (Dúchas.ie Schools' Collection, National Folklore Collection, Royal Irish Academy) contain:
- High-resolution manuscript images in one silo
- TEI-XML transcriptions in another silo
- Page-level metadata links only (no coordinate mapping)

**Typical XML Structure:**
```xml
<pb n="117"/>
<p>
  <lb/>Tá sé páidre go bfuil cipic...
  <lb/>Deirfear gur b'iad na Lochlannaigh...
</p>
```

The `<lb/>` tag indicates line breaks but carries no spatial coordinates (x, y, w, h). Without this information:
- Cannot crop images to train HTR models
- Cannot train grounding models to find text
- Manual annotation is economically infeasible

### 1.2 Visual Characteristics of Gaelic Manuscripts

The Schools' Collection (late 19th/early 20th century) presents specific challenges:

| Feature | Description | OCR Challenge |
|---------|-------------|---------------|
| **Script Morphology** | Long 'r' (r rotunda), long 's' (ſ), uncial 'd' | Resembles other letters to untrained eye |
| **Punctum Delens** | Dot above consonants for lenition (ḃ, ċ, ḋ) | Few pixels radically alter meaning |
| **Tironian Note (⁊)** | Symbol for "agus" (and) | Often fails to tokenize correctly |
| **Baseline Drift** | Handwriting violates lined paper | Adjacent lines touch/overlap |

---

## 2. ColPali for Weakly-Supervised Alignment

### 2.1 Why ColPali?

ColPali is trained with a **contrastive retrieval objective**, explicitly penalized if patch embeddings don't align with query tokens. This produces:
- "Sharper" alignment than generative attention
- Semantically rich patch embeddings
- Suitable for generating training data

### 2.2 Architecture Overview

```
Manuscript Page → SigLIP Encoder → Patch Embeddings (32×32×128)
                                           ↓
Transcription Line → Gemma Encoder → Token Embeddings (L×128)
                                           ↓
                     MaxSim Interaction → Similarity Map
                                           ↓
                     Threshold + Contours → Bounding Box
```

### 2.3 The MaxSim Localization Mechanism

$$S(Q, D) = \sum_{j=1}^{N_{tokens}} \max_{i=1}^{N_{patches}} (t_j \cdot p_i)$$

For each token in the transcription:
1. Calculate dot product with all 1,024 patches
2. Find patch with highest similarity
3. That patch contains the visual representation of the token

**Example**: For "Lochlannaigh" (Vikings), the max operator identifies the patch physically containing that word.

---

## 3. Alignment Pipeline Implementation

### 3.1 Step 1: Preprocessing

**Line-Level Alignment** is optimal:
- Word-level is computationally expensive and noisy
- Line-level provides sufficient context while being granular enough for HTR training

**Tironian Note Handling:**
```python
def prepare_query(xml_text):
    """
    Handle Tironian et (⁊) for ColPali alignment.

    Issue: Gemma tokenizer treats ⁊ as <unk>
    Solution: Replace with 'agus' for query, retain ⁊ in labels
    """
    query = xml_text.replace('⁊', 'agus')
    return query, xml_text  # (query, ground_truth)
```

### 3.2 Step 2: Similarity Map Generation

```python
import torch
import numpy as np
from colpali_engine.models import ColQwen2Processor, ColQwen2

def generate_alignment_heatmap(image, text_query, model, processor):
    """Generate spatial heatmap for text query on manuscript page."""

    # 1. Process Inputs
    inputs = processor(text=[text_query], images=[image], return_tensors="pt")
    inputs = {k: v.to(model.device) for k, v in inputs.items()}

    # 2. Forward Pass
    with torch.no_grad():
        out = model(**inputs)
        patch_embeddings = out.visual_embeddings  # [1, 1024, 128]
        query_embeddings = out.text_embeddings    # [1, L, 128]

    # 3. Compute Interaction
    interaction = torch.einsum("bnd,bmd->bnm", query_embeddings, patch_embeddings)

    # 4. Aggregate across tokens
    heatmap_flat = interaction.sum(dim=1).squeeze()  # [1024]

    # 5. Reshape to 2D grid
    side = int(np.sqrt(heatmap_flat.shape[0]))
    heatmap_2d = heatmap_flat.view(side, side).cpu().numpy()

    return heatmap_2d
```

### 3.3 Step 3: Heatmap to Bounding Box

```python
import cv2
from scipy.ndimage import zoom

def heatmap_to_bbox(heatmap, original_size, min_area=500):
    """Convert ColPali heatmap to bounding box coordinates."""

    # 1. Upscale to original resolution
    scale_h = original_size[0] / heatmap.shape[0]
    scale_w = original_size[1] / heatmap.shape[1]
    heatmap_full = zoom(heatmap, (scale_h, scale_w), order=3)

    # 2. Normalize to [0, 255]
    heatmap_norm = ((heatmap_full - heatmap_full.min()) /
                    (heatmap_full.max() - heatmap_full.min()) * 255).astype(np.uint8)

    # 3. Otsu's binarization
    _, binary = cv2.threshold(heatmap_norm, 0, 255,
                               cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    # 4. Morphological dilation for handwriting
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 3))
    dilated = cv2.dilate(binary, kernel, iterations=2)

    # 5. Find contours
    contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL,
                                    cv2.CHAIN_APPROX_SIMPLE)

    # 6. Select largest valid contour
    valid_boxes = []
    for cnt in contours:
        x, y, w, h = cv2.boundingRect(cnt)
        area = w * h
        aspect = w / h if h > 0 else 0

        # Sanity checks
        if area < min_area:
            continue
        if aspect < 1.5:  # Lines should be horizontal
            continue
        if w > original_size[1] * 0.5:  # Not covering >50% of page
            continue

        valid_boxes.append((x, y, w, h, area))

    if valid_boxes:
        # Return largest valid box
        return max(valid_boxes, key=lambda b: b[4])[:4]
    return None
```

---

## 4. Fine-Tuning Qwen2-VL with Unsloth

### 4.1 Why Qwen2-VL?

- State-of-the-art among open-weights for OCR
- Outperforms GPT-4o-mini on OCRBench and DocVQA
- Dynamic resolution preserves handwriting detail
- Native multilingual support including Irish

### 4.2 Why Unsloth?

| Benefit | Detail |
|---------|--------|
| Memory Efficiency | 4-bit QLoRA with minimal precision loss |
| Speed | 2x faster training via Triton kernels |
| Vision Support | Native Qwen2-VL data collation |
| VRAM | 7-8B model: ~6-7GB (consumer RTX 3060+) |

### 4.3 Dataset Formatting

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
        {"type": "image", "image": "file:///path/to/full_page_117.jpg"},
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

### 4.4 Training Configuration

```python
from unsloth import FastVisionModel
from unsloth import UnslothVisionDataCollator
from trl import SFTTrainer

# Load model with 4-bit quantization
model, tokenizer = FastVisionModel.from_pretrained(
    "unsloth/Qwen2-VL-7B-Instruct",
    load_in_4bit=True,
    max_seq_length=2048,
)

# Apply LoRA to BOTH vision and language
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

# Training arguments
training_args = TrainingArguments(
    output_dir="./gaelic-qwen-vl",
    per_device_train_batch_size=2,
    gradient_accumulation_steps=4,
    learning_rate=2e-4,
    num_train_epochs=3,
    optim="adamw_8bit",
    fp16=False,
    bf16=True,
)

# Train with vision-aware data collator
trainer = SFTTrainer(
    model=model,
    args=training_args,
    train_dataset=gaelic_dataset,
    data_collator=UnslothVisionDataCollator(model, tokenizer),
)
trainer.train()
```

### 4.5 Critical Hyperparameters

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| `finetune_vision_layers` | True | Vision encoder never saw Gaelic script |
| `finetune_language_layers` | True | Learn Gaelic syntax and abbreviations |
| `lora_rank` | 16 | Balance plasticity vs memory |
| `batch_size` | 2 | Fit in GPU with gradient accumulation |
| `gradient_accumulation_steps` | 4 | Effective batch size = 8 |
| `max_seq_length` | 2048 | Full page visual tokens + transcription |
| `optim` | adamw_8bit | 75% optimizer memory reduction |
| `learning_rate` | 2e-4 | Standard LoRA rate |

---

## 5. Handling Gaelic Script Specifics

### 5.1 Punctum Delens (Lenition Dots)

The dot above consonants (ḃ, ċ, ḋ, ḟ, ġ, ṁ, ṗ, ṡ, ṫ) is just a few pixels but changes pronunciation completely.

**Training Strategy:**
- Include explicit examples with/without dots
- Use high-resolution crops (preserve detail)
- Data augmentation: slight blur to test robustness

### 5.2 Tironian Note (⁊)

**Tokenization Issue:**
- Standard tokenizers split ⁊ (U+204A) into bytes or `<unk>`
- Causes alignment drift

**Solution:**
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

### 5.3 Dialectal Variations

Different dialects have different spellings:

| Standard | Ulster | Connacht | Munster |
|----------|--------|----------|---------|
| féach | amharc | féach | féach |
| ag | ag | ag | a' |

**Training Data Strategy:**
- Include examples from all three dialects
- Label dialect in metadata
- Don't penalize valid variations in evaluation

---

## 6. Evaluation Metrics

### 6.1 Character Error Rate (CER)

Critical for Gaelic because:
- Missed punctum delens = error
- Incorrect Tironian note expansion = error

```python
def calculate_cer(predictions, references):
    """Calculate Character Error Rate."""
    total_chars = 0
    total_errors = 0

    for pred, ref in zip(predictions, references):
        # Normalize Unicode (NFC)
        pred = unicodedata.normalize('NFC', pred)
        ref = unicodedata.normalize('NFC', ref)

        total_chars += len(ref)
        total_errors += editdistance.eval(pred, ref)

    return total_errors / total_chars
```

### 6.2 Word Error Rate (WER)

Measures semantic coherence at word level.

### 6.3 Grounding IoU

For the grounding task, measure Intersection over Union between predicted boxes and ColPali pseudo-ground-truth.

---

## 7. Common Issues and Solutions

### 7.1 Gibberish Output

**Symptom:** Model outputs repetitive tokens ("addCriterion") or internal control tokens.

**Cause:** Overfitting to formatting, loss of EOS token probability.

**Solution:**
```python
# Use SFT, not RL for initial adaptation
# Check chat template formatting
# Lower learning rate if persists
# Ensure aspect ratio crops are reasonable (not extreme)
```

### 7.2 Aspect Ratio Problems

**Symptom:** Extremely wide, short strips (3000px × 50px) cause confusion.

**Solution:**
```python
def pad_to_square(image, min_ratio=0.3):
    """Pad crops to more reasonable aspect ratio."""
    w, h = image.size
    ratio = min(w, h) / max(w, h)

    if ratio < min_ratio:
        # Add padding to shortest dimension
        target_size = max(w, h)
        padded = Image.new('RGB', (target_size, target_size), (255, 255, 255))
        padded.paste(image, ((target_size - w) // 2, (target_size - h) // 2))
        return padded
    return image
```

---

## 8. Pipeline Summary

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
│  4. Split train/val/test                                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    UNSLOTH FINE-TUNING                          │
│  1. Load Qwen2-VL with 4-bit quantization                       │
│  2. Apply LoRA to vision + language layers                      │
│  3. Train with UnslothVisionDataCollator                        │
│  4. Evaluate on held-out manuscripts                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DEPLOYMENT                                    │
│  1. Export to GGUF for llama.cpp                                │
│  2. Or serve via MLX on Apple Silicon                           │
│  3. Integrate with digital humanities workflow                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Impact and Applications

### 9.1 Democratizing HTR

This pipeline eliminates the manual annotation bottleneck:
- Traditional: PhD student manually annotates thousands of boxes
- ColPali: Automatic weak supervision from existing transcriptions

### 9.2 Transferability

The same pipeline applies to:
- Sanskrit palm leaves
- Arabic manuscripts
- Civil War letters
- Any collection with loose transcriptions

### 9.3 Material Understanding

Fine-tuning the vision encoder allows the model to:
- Learn iron-gall ink on vellum characteristics
- Ignore transparency/bleed-through
- "Read" the manuscript as a material object, not just shapes
