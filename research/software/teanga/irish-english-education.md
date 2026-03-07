# Technical Architecture for a Bilingual Irish/English Mathematics Education System

Building an AI tutoring system for Irish Leaving Certificate mathematics that processes **8,000+ pages** of bilingual curriculum documents requires careful orchestration of cutting-edge tools across document processing, fine-tuning, RAG, and deployment. The recommended architecture combines **Qwen2.5-VL** for multimodal understanding, **ColPali** for visual document retrieval, **BAML** for structured extraction, and **Qwen2.5-Math-7B** fine-tuned via **Unsloth**—deployable within days on Modal or consumer hardware.

---

## Document processing pipeline delivers 95% LaTeX extraction accuracy

The document ingestion layer must handle mathematical equations, geometric diagrams, tables from marking schemes, and bilingual Irish/English text. Five tools emerged as viable candidates, each with distinct strengths:

**DeepSeek-OCR** (3B parameters, MIT licensed) achieves approximately **95% formula recognition accuracy** and excels at converting mathematical content to LaTeX. Its revolutionary "vision-as-compression" technology recovers 600-1000+ text tokens from just 64-100 vision tokens, enabling processing speeds of ~2,500 tokens/second on A100 GPUs—roughly **200,000 pages per day**. However, Irish language support remains unconfirmed in official documentation.

**Qwen2.5-VL and Qwen3-VL** from Alibaba offer the most compelling multilingual capabilities, supporting **32 languages** including "most European languages." The models excel at document understanding benchmarks (DocVQA), handle tables and charts well, and produce structured JSON output—ideal for marking scheme extraction. Available in sizes from 2B to 235B parameters, the **7B variant** offers optimal balance for this use case. Qwen3 explicitly includes Irish among its 119 supported languages.

**Granite-Docling** from IBM provides a remarkably lightweight alternative at only **258M parameters**, purpose-built for document conversion with enhanced equation recognition and excellent table structure preservation. Its DocTags format captures all page elements with positional information, and it integrates directly with LangChain and LlamaIndex.

| Tool | LaTeX Extraction | Diagrams | Tables | Irish Support | Model Size |
|------|-----------------|----------|--------|---------------|------------|
| DeepSeek-OCR | Excellent (95%) | Good | Very Good | Unconfirmed | 3B |
| Qwen2.5-VL | Very Good | Excellent | Excellent | Likely (European) | 2B-235B |
| Granite-Docling | Good | Good | Excellent | Experimental | 258M |
| ColPali | N/A (retrieval) | Excellent | Good (visual) | Visual-based | 3B |
| Unstract | Depends on LLM | Depends | Good | Depends | Orchestration |

**ColPali** represents a paradigm shift—rather than OCR-based extraction, it creates **multi-vector embeddings directly from document page images** using PaliGemma-3B and ColBERT late-interaction mechanisms. This bypasses traditional text extraction entirely, achieving **0.81 nDCG@5** on the ViDoRe benchmark versus 0.66 for traditional pipelines. For exam papers with geometric diagrams, ColPali retrieves relevant pages visually, then Qwen2.5-VL extracts the actual content.

The recommended pipeline chains these tools: **ColPali** for visual retrieval → **Qwen2.5-VL-7B** or **DeepSeek-OCR** for content extraction → **Granite-Docling** for structured table processing → **BAML** for schema-enforced output.

---

## Fine-tuning Qwen2.5-Math-7B with Unsloth requires only 6-7GB VRAM

The mathematics tutoring model should be fine-tuned on Leaving Certificate exam papers paired with marking schemes. **Qwen2.5-Math-7B-Instruct** emerges as the optimal base model, achieving **85.3% on the MATH benchmark** with Tool-Integrated Reasoning and solving up to 21/30 AIME problems when combined with reward model sampling.

**Unsloth** (docs.unsloth.ai) provides 2x faster training with 70% less VRAM compared to standard HuggingFace approaches. For a 7-8B model using QLoRA 4-bit quantization, fine-tuning requires only **~6-7GB VRAM**—achievable on consumer RTX 3060 or higher. The framework supports all major math models including DeepSeek-R1 distillations, Qwen2.5-Math variants, and Phi-4 Reasoning.

**DeepSeek-Math-V2** (November 2025) achieves gold-level performance on IMO 2025 and near-perfect scores on Putnam 2024, but its massive size (based on V3.2-Exp-Base) makes it impractical for fine-tuning. Instead, **DeepSeek-R1-Distill-Qwen-7B** offers excellent reasoning capabilities at manageable scale through knowledge distillation.

### Training data structure for exam preparation

The optimal format uses ShareGPT/ChatML structure with explicit chain-of-thought reasoning and marking scheme alignment:

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

Critical hyperparameters for mathematical reasoning include higher LoRA rank (**r=64-128** vs typical 16-32), lower learning rates (**1e-5 to 5e-5**), and longer sequence lengths (4096+ tokens for multi-step solutions). Dataset mixing should combine 60-70% Leaving Certificate problems with 20-30% general mathematics (GSM8K, MATH benchmark samples) to prevent catastrophic forgetting.

---

## Irish language integration through UCCIX and Qwen3 native support

Irish presents unique challenges as a low-resource language with <0.1% of web content. Two paths enable bilingual support:

**UCCIX models** from University College Cork represent the state-of-the-art for Irish LLMs. The **UCCIX-Llama2-13B-Instruct** was trained on ~520M Irish tokens with vocabulary expansion to include native Irish tokens, outperforming LLaMA 2-70B on Irish tasks by up to 12%. The newer **UCCIX-Llama3.1-70B-Instruct** (December 2024) builds on LLaMA 3.1's improved architecture. These models can serve as teacher models for knowledge distillation or provide the expanded Irish tokenizer for fine-tuning other models.

**GaBERT** (DCU-NLP) offers Irish-specific BERT embeddings trained on 7.9M Irish sentences, useful for preprocessing and classification tasks. It outperforms multilingual BERT by +3.7 LAS on dependency parsing.

**Qwen3** explicitly lists Irish among its 119 supported languages, trained on 36 trillion tokens with Irish appearing alongside Welsh and Scottish Gaelic in its embedding space. This makes Qwen3-based models the most promising for native bilingual support without requiring extensive Irish-specific fine-tuning.

The **IRLBench benchmark** (May 2025) reveals a persistent ~20% performance gap between English and Irish on identical exam questions—best models achieve 55.8% Irish versus 76.2% English. Language fidelity remains problematic, with models producing valid Irish less than 80% of the time. Plan for Irish output verification and consider translation fallback strategies.

### Recommended multilingual approach

1. Use **Qwen2.5-Math-7B** as base (native Irish support)
2. Merge UCCIX tokenizer additions if Irish performance is insufficient
3. Include bilingual training examples with explicit Irish terminology
4. Validate outputs against Irish-BLiMP benchmark (1,020 minimal pairs)
5. Consider UCCIX as fallback generator for Irish-only responses

---

## RAG architecture combines ColPali visual retrieval with BGE-M3 embeddings

For 8,000+ curriculum pages, the retrieval system must handle mathematical notation, geometric diagrams, and bilingual content efficiently. **CocoIndex** provides the document indexing backbone with incremental processing—only re-computing affected portions when sources or logic change.

**BGE-M3** (BAAI) serves as the primary embedding model with three retrieval modes: dense semantic embeddings, learned sparse representations (outperforming BM25), and ColBERT-style multi-vector retrieval. It supports **100+ languages** with 8,192 token context length—critical for long mathematical documents. For optimal Irish support, combine with **LaBSE** embeddings which cover 109 languages including Irish and demonstrate superior performance on Irish classification tasks.

**ColPali** should operate alongside traditional embeddings for hybrid retrieval. ColQwen2.5-v0.2 (based on Qwen2.5-VL-3B) supports 29+ languages and eliminates OCR errors for equation-heavy pages. The tradeoff: ColPali produces 10-100x more vectors per document (1,024 patches per page), requiring token pooling for storage efficiency.

For the vector database, **Qdrant** (self-hosted or cloud) offers the best combination of features for this use case:
- Advanced payload filtering for metadata (exam year, topic, difficulty, language)
- Native multi-vector support for ColPali embeddings
- Hybrid search combining sparse and dense retrieval
- Highest RPS and lowest latency in benchmarks

### Chunking strategy for mathematical content

Standard semantic chunking fails around equations because mathematical notation creates semantic dissimilarity with surrounding explanatory text. The **semantic double-pass merging** algorithm addresses this:

1. First pass: Standard semantic chunking
2. Second pass: If chunks 1 and 3 are semantically similar but chunk 2 (equation) differs, merge all three

Configure chunk sizes of **1000-2000 tokens** with 200-500 overlap, using separators that respect LaTeX boundaries: `["\\n\\n", "\\n", ".", "$$", "\\["]`. Never split inside LaTeX environments.

---

## Deployment on Modal enables scale-to-zero with sub-second cold starts

**Modal** provides optimal serverless deployment for fine-tuned models with per-second GPU billing and automatic scaling. Key pricing for math tutoring workloads:

| GPU | Price/Hour | VRAM | Best For |
|-----|-----------|------|----------|
| NVIDIA T4 | $0.59 | 16GB | Development/testing |
| NVIDIA L4 | $0.80 | 24GB | 7B models quantized |
| NVIDIA A10 | $1.10 | 24GB | 7B-13B production |
| NVIDIA A100 40GB | $2.10 | 40GB | 13B-70B models |

Modal's Rust-based container stack achieves **<1 second cold starts**, critical for conversational tutoring where users expect immediate responses. Unsloth-trained models export directly to GGUF, vLLM, or native formats for deployment.

**Consumer hardware** remains viable for development and small-scale deployment. An **RTX 4090** (24GB, ~$1,800) runs 7B models at ~50 tokens/second with Q4_K_M quantization, or 13B models at 30-40 t/s. The RTX 3090 achieves similar performance at lower cost (~$1,500 used).

For inference engines, **vLLM** with PagedAttention provides 2-4x faster throughput than standard approaches and integrates well with Modal deployments. Implement **KV caching** (built into vLLM) plus **semantic response caching** for common math problems—research shows 50-90% GPU cost reduction with proper caching.

**Latency targets** for educational chatbots: Time-to-First-Token under **2 seconds**, token generation at **20-50 tokens/second minimum**. Studies show users lose patience after 3 seconds of waiting. Always use streaming responses.

---

## BAML enforces schema compliance for structured exam paper extraction

**BAML** (BoundaryML) is a domain-specific language for building reliable AI workflows with structured outputs, perfectly suited for extracting questions, marks, and topics from exam papers. Its Schema-Aligned Parsing works even without native tool-calling APIs, handling markdown in JSON and chain-of-thought reasoning.

```baml
class MathQuestion {
  number string
  text string @description("Full question in original language")  
  text_irish string?
  marks int
  topic "Algebra" | "Geometry" | "Calculus" | "Statistics"
  marking_criteria MarkingCriterion[]
  requires_diagram bool
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

BAML generates type-safe clients for Python and TypeScript, enabling compile-time verification of extraction schemas. The VSCode playground provides parallel test execution for iterating on extraction prompts. Native multimodal support handles PDFs, images, and audio inputs directly.

---

## Complete architecture recommendation

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

## Rapid prototyping roadmap achieves demo in 3 days

**Days 1-3 (Foundation):**
- Set up BAML project with exam paper schemas
- Create PDF extraction pipeline: PyMuPDF4LLM + BAML
- Initialize ChromaDB for vector storage (upgrade to Qdrant later)
- Build Streamlit chat interface
- Single exam paper end-to-end demo

**Week 1 (Core RAG):**
- Integrate LlamaIndex with vector store
- Implement topic-filtered retrieval
- Add step-by-step solution generation
- Basic Irish language support via Qwen3

**Week 2 (Enhancement):**
- Multi-modal diagram handling with ColPali
- Marking scheme integration for grading
- Practice test generation from topic pools
- Fine-tune Qwen2.5-Math-7B with Unsloth on collected data

**Weeks 3-4 (Production):**
- Deploy to Modal with autoscaling
- Implement response caching
- Bilingual output verification
- Evaluation against IRLBench

---

## Conclusion: Achievable innovation with open-source tools

This architecture leverages entirely open-source or commercially permissive models—Qwen (Apache 2.0), DeepSeek-OCR (MIT), BAML (Apache 2.0), Granite-Docling (MIT)—while addressing the unique challenges of mathematical notation, geometric diagrams, and Irish language support. 

The combination of **ColPali for visual retrieval** and **Qwen2.5-VL for content extraction** represents the cutting edge for document understanding, while **Unsloth-powered fine-tuning** of **Qwen2.5-Math-7B** enables domain adaptation at minimal cost (6-7GB VRAM). Irish language capabilities come from Qwen3's native support supplemented by UCCIX model techniques when higher accuracy is needed.

Total infrastructure cost for an MVP: **~$100-300/month** on Modal with free credits, or near-zero for development on consumer RTX hardware. The prototype-focused approach—BAML + LlamaIndex + Streamlit—enables functional demos within days, with full bilingual tutoring capability achievable in 2-4 weeks.