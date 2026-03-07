# AI/ML Pipeline for Irish Education Platform

## Executive Summary

This document details the machine learning architecture for processing 8,000+ pages of bilingual curriculum documents, including document understanding, model fine-tuning, and retrieval-augmented generation (RAG) for an Irish Leaving Certificate tutoring system.

---

## 1. Document Processing Pipeline

### 1.1 Tool Comparison

| Tool | LaTeX Extraction | Diagrams | Tables | Irish Support | Model Size |
|------|-----------------|----------|--------|---------------|------------|
| DeepSeek-OCR | Excellent (95%) | Good | Very Good | Unconfirmed | 3B |
| Qwen2.5-VL | Very Good | Excellent | Excellent | Likely (European) | 2B-235B |
| Qwen3-VL | Very Good | Excellent | Excellent | Native (119 langs) | Various |
| Granite-Docling | Good | Good | Excellent | Experimental | 258M |
| ColPali | N/A (retrieval) | Excellent | Good (visual) | Visual-based | 3B |

### 1.2 Recommended Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                 DOCUMENT INGESTION (CocoIndex)                  │
│  PDF Sources → Language Detection → Content Routing             │
│  ├── Text/Equations → DeepSeek-OCR → LaTeX extraction          │
│  ├── Diagrams → ColPali → Visual embeddings                     │
│  └── Tables → Granite-Docling → Structured extraction          │
│  ↓                                                              │
│  BAML Structured Extraction → Metadata + JSON                   │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 DeepSeek-OCR Capabilities

- **95% formula recognition accuracy**
- Vision-as-compression: 600-1000+ text tokens from 64-100 vision tokens
- Processing speed: ~2,500 tokens/second on A100 (~200,000 pages/day)
- MIT licensed (3B parameters)

### 1.4 ColPali: Visual Document Retrieval

Revolutionary approach bypassing OCR entirely:
- Multi-vector embeddings directly from page images
- PaliGemma-3B + ColBERT late-interaction
- **0.81 nDCG@5** vs 0.66 for traditional pipelines
- Ideal for geometry diagrams in exam papers

---

## 2. Fine-Tuning Strategy

### 2.1 Base Model Selection

**Qwen2.5-Math-7B-Instruct** (recommended):
- 85.3% on MATH benchmark with Tool-Integrated Reasoning
- Solves 21/30 AIME problems with reward model sampling
- Native multilingual support including Irish

**Alternatives**:
- DeepSeek-R1-Distill-Qwen-7B: Excellent reasoning via knowledge distillation
- DeepSeek-Math-V2: IMO gold-level (but too large for fine-tuning)

### 2.2 Unsloth Training Framework

**Advantages**:
- 2x faster training, 70% less VRAM
- 7-8B model with QLoRA 4-bit: **~6-7GB VRAM**
- Achievable on consumer RTX 3060+

**Critical Hyperparameters**:
| Parameter | Math Reasoning Value | Standard Value |
|-----------|---------------------|----------------|
| LoRA rank | 64-128 | 16-32 |
| Learning rate | 1e-5 to 5e-5 | 1e-4 to 5e-4 |
| Sequence length | 4096+ tokens | 2048 |

### 2.3 Training Data Format

```json
{
  "conversations": [
    {
      "role": "user",
      "content": "Leaving Certificate Higher Level, Paper 1:\nDifferentiate f(x) = (3x²+2)/(x-1) and find stationary points. (25 marks)"
    },
    {
      "role": "assistant",
      "content": "<think>Apply quotient rule, find where f'(x)=0...</think>\n\n**Step 1: Apply Quotient Rule** (5 marks)\n$$f'(x) = \\frac{6x(x-1) - (3x^2+2)(1)}{(x-1)^2}$$\n...\nFinal Answer: \\boxed{\\left(1 \\pm \\frac{\\sqrt{15}}{3}, y\\right)}"
    }
  ]
}
```

**Dataset Mixing**:
- 60-70% Leaving Certificate problems
- 20-30% general mathematics (GSM8K, MATH benchmark)
- Prevents catastrophic forgetting

---

## 3. Irish Language Integration

### 3.1 The Challenge

- Irish: <0.1% of web content (low-resource language)
- ~20% performance gap between English and Irish on identical questions
- Language fidelity: models produce valid Irish <80% of the time

### 3.2 UCCIX Models (University College Cork)

**UCCIX-Llama2-13B-Instruct**:
- Trained on ~520M Irish tokens
- Vocabulary expansion for native Irish tokens
- Outperforms LLaMA 2-70B on Irish tasks by +12%

**UCCIX-Llama3.1-70B-Instruct** (December 2024):
- Latest architecture with improved Irish capabilities
- Useful as teacher model for distillation

### 3.3 GaBERT (DCU-NLP)

- Irish-specific BERT embeddings
- Trained on 7.9M Irish sentences
- +3.7 LAS improvement on dependency parsing
- Useful for preprocessing and classification

### 3.4 Recommended Multilingual Approach

1. Use **Qwen2.5-Math-7B** as base (native Irish support)
2. Merge UCCIX tokenizer additions if needed
3. Include bilingual training examples with Irish terminology
4. Validate outputs against Irish-BLiMP benchmark (1,020 minimal pairs)
5. UCCIX fallback for Irish-only responses

---

## 4. RAG Architecture

### 4.1 Embedding Models

**BGE-M3** (BAAI) - Primary:
- Three retrieval modes: dense, sparse, multi-vector
- 100+ languages, 8,192 token context
- Outperforms BM25 with learned sparse representations

**LaBSE** - Irish Supplement:
- 109 languages including Irish
- Superior performance on Irish classification tasks

### 4.2 Hybrid Retrieval Strategy

```
Query → Language Detection
    ↓
┌──────────────────────────────────────┐
│ BGE-M3 Dense + Sparse Embeddings     │
│ ColPali Visual Page Embeddings       │
│ Payload Filtering (year, topic, lang)│
└──────────────────────────────────────┘
    ↓
Reranking → Top-K Results
    ↓
Context Assembly for LLM
```

### 4.3 ColPali Integration

**ColQwen2.5-v0.2** (based on Qwen2.5-VL-3B):
- 29+ languages
- Eliminates OCR errors for equation-heavy pages
- Trade-off: 10-100x more vectors per document (1,024 patches/page)
- Use token pooling for storage efficiency

### 4.4 Vector Database: Qdrant

**Why Qdrant**:
- Advanced payload filtering for metadata
- Native multi-vector support for ColPali
- Hybrid sparse + dense search
- Highest RPS and lowest latency in benchmarks

### 4.5 Chunking Strategy for Math

Standard semantic chunking fails around equations. Use **semantic double-pass merging**:

1. First pass: Standard semantic chunking
2. Second pass: If chunks 1 and 3 similar but chunk 2 (equation) differs, merge all three

**Configuration**:
- Chunk size: 1000-2000 tokens
- Overlap: 200-500 tokens
- Separators: `["\n\n", "\n", ".", "$$", "\\["]`
- Never split inside LaTeX environments

---

## 5. BAML Schema Enforcement

### 5.1 Exam Paper Extraction Schema

```baml
class MathQuestion {
  number: string
  text: string @description("Full question in original language")
  text_irish: string?
  marks: int
  topic: "Algebra" | "Geometry" | "Calculus" | "Statistics"
  marking_criteria: MarkingCriterion[]
  requires_diagram: bool
}

function ExtractExamPaper(document: pdf) -> ParsedExam {
  client "anthropic/claude-sonnet-4-20250514"
  prompt #"
    Extract all questions from this Leaving Certificate exam paper.
    Identify marks, topics, and any diagrams required.
    {{ document }}
    {{ ctx.output_format }}
  "#
}
```

### 5.2 Benefits of BAML

- Type-safe clients for Python and TypeScript
- Compile-time verification of extraction schemas
- VSCode playground for parallel prompt testing
- Native multimodal support (PDFs, images, audio)

---

## 6. Deployment Architecture

### 6.1 Modal Serverless (Recommended)

| GPU | Price/Hour | VRAM | Best For |
|-----|-----------|------|----------|
| NVIDIA T4 | $0.59 | 16GB | Development/testing |
| NVIDIA L4 | $0.80 | 24GB | 7B models quantized |
| NVIDIA A10 | $1.10 | 24GB | 7B-13B production |
| NVIDIA A100 40GB | $2.10 | 40GB | 13B-70B models |

**Advantages**:
- <1 second cold starts (Rust-based container stack)
- Per-second billing with scale-to-zero
- Direct Unsloth export to GGUF/vLLM

### 6.2 Consumer Hardware Option

**RTX 4090** (~$1,800):
- 7B models at ~50 tokens/second (Q4_K_M)
- 13B models at 30-40 t/s

**RTX 3090** (~$1,500 used):
- Similar performance at lower cost

### 6.3 Inference Optimization

**vLLM with PagedAttention**:
- 2-4x faster throughput
- Built-in KV caching

**Response Caching**:
- Semantic cache for common math problems
- 50-90% GPU cost reduction

**Latency Targets**:
- Time-to-First-Token: <2 seconds
- Token generation: 20-50 tokens/second minimum
- Always use streaming responses

---

## 7. Evaluation Framework

### 7.1 IRLBench (Irish Language)

- Reveals ~20% performance gap English vs Irish
- Best models: 55.8% Irish vs 76.2% English
- Use for Irish output validation

### 7.2 Irish-BLiMP Benchmark

- 1,020 minimal pairs for grammaticality
- Essential for validating Irish language generation

### 7.3 MLflow + Ragas Integration

```python
import mlflow
from ragas import evaluate

# Track experiments
with mlflow.start_run():
    mlflow.log_params({"model": "qwen2.5-math-7b", "lora_r": 64})

    # LLM-as-judge evaluation
    result = evaluate(
        dataset=leaving_cert_test_set,
        metrics=[faithfulness, answer_relevancy, context_precision]
    )
    mlflow.log_metrics(result)
```

---

## 8. Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                 DOCUMENT INGESTION (CocoIndex)                  │
│  PDF Sources → Language Detection → Content Routing             │
│  ├── Text/Equations → DeepSeek-OCR → LaTeX extraction          │
│  ├── Diagrams → ColPali → Visual embeddings                     │
│  └── Tables → Granite-Docling → Structured extraction          │
│  ↓                                                              │
│  BAML Structured Extraction → Metadata + JSON                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 KNOWLEDGE BASE (Qdrant)                         │
│  ├── text_chunks: BGE-M3 embeddings (dense + sparse)           │
│  ├── visual_pages: ColPali multi-vector embeddings             │
│  └── Payload filtering: {language, level, topic, year}         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 RAG RETRIEVAL (LlamaIndex)                      │
│  Query → Language detection → Hybrid search → Reranking        │
│  Return: Relevant questions + marking schemes + diagrams        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 GENERATION (Fine-tuned Model)                   │
│  Qwen2.5-Math-7B fine-tuned via Unsloth on LC exam data        │
│  BAML functions for step-by-step solutions, bilingual output   │
│  Deployment: Modal (serverless) or vLLM (self-hosted)          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Rapid Prototyping Roadmap

### Days 1-3 (Foundation)
- Set up BAML project with exam paper schemas
- PDF extraction: PyMuPDF4LLM + BAML
- ChromaDB for initial vector storage
- Streamlit chat interface
- Single exam paper end-to-end demo

### Week 1 (Core RAG)
- LlamaIndex integration
- Topic-filtered retrieval
- Step-by-step solution generation
- Basic Irish via Qwen3

### Week 2 (Enhancement)
- ColPali for diagram handling
- Marking scheme integration
- Practice test generation
- Fine-tune Qwen2.5-Math-7B with Unsloth

### Weeks 3-4 (Production)
- Deploy to Modal with autoscaling
- Response caching
- Bilingual output verification
- IRLBench evaluation

---

## 10. Cost Analysis

### MVP Infrastructure (~$100-300/month on Modal)

| Component | Cost |
|-----------|------|
| Modal compute (with free credits) | $100-200 |
| Qdrant Cloud (small tier) | $25 |
| Storage (R2/S3) | $10-20 |
| API calls (BAML extraction) | $50-100 |

### Development Hardware (One-Time)

| Option | Cost |
|--------|------|
| RTX 4090 | ~$1,800 |
| RTX 3090 (used) | ~$1,500 |
| M2 Max MacBook | ~$3,000 |

Near-zero ongoing cost for development on consumer hardware.
