# Celtic Language AI/ML Resources

Comprehensive guide to AI/ML resources for Celtic languages on HuggingFace and related platforms.

**Languages Covered:** Irish (Gaeilge), Scottish Gaelic (Gàidhlig), Welsh (Cymraeg), Manx (Gaelg)

---

## Overview

| Language | LLMs | ASR Models | TTS Models | Translation | Datasets | Maturity |
|----------|------|------------|------------|-------------|----------|----------|
| **Irish** | 5+ | 7+ | 1 | 4+ | 10+ | High |
| **Scottish Gaelic** | 2+ | 0 (dev) | 0 | 4+ | 38+ | Medium |
| **Welsh** | 2 | 7+ | 1 | 2+ | 8+ | High |
| **Manx** | 0 | 0 | 0-1 | 4 | 2-3 | Low |

### Key Findings

- **Most Developed:** Irish and Welsh have mature ecosystems with dedicated LLMs and extensive speech technologies
- **Emerging:** Scottish Gaelic has strong dataset availability but limited dedicated models
- **Critical Gap:** Manx has minimal resources, primarily limited to translation models
- **Major Contributors:** DCU-NLP, ReliableAI (Irish), techiaith/BangorAI (Welsh), Helsinki-NLP (all languages)

---

## 1. Irish (Gaeilge) Resources

**ISO Codes:** ga (639-1), gle (639-2/3), Locale: ga-IE
**Speakers:** ~1.85 million (2022 census)

### Language Models

#### UCCIX (2024) - Most Advanced Irish LLM

| Model | URL | Details |
|-------|-----|---------|
| Base 13B | `ReliableAI/UCCIX-Llama2-13B` | First open-source Irish LLM, ~520M Irish tokens |
| Instruct 13B | `ReliableAI/UCCIX-Llama2-13B-Instruct` | Outperforms larger models by up to 12% |
| Llama 3.1 70B | `ReliableAI/UCCIX-Llama3.1-70B-Instruct-19122024` | Latest large-scale version |

**Demo:** https://aine.chat

#### gaBERT - Irish BERT
- **URL:** `DCU-NLP/bert-base-irish-cased-v1`
- **Training:** 7.9M Irish sentences
- **Status:** Best performing encoder model for Irish

#### gaELECTRA
- **URL:** `DCU-NLP/electra-base-irish-cased-generator-v1`
- **Training:** 7.9M Irish sentences

#### BERTreach
- **URL:** `jimregan/BERTreach`
- **Training:** 47M tokens, RoBERTa-based

### Speech Recognition (ASR) Models

#### Wav2Vec2 Models
| Model | URL |
|-------|-----|
| cpierse/wav2vec2-large-xlsr-53-irish | Large XLSR fine-tuned |
| Aditya3107/wav2vec2-large-xls-r-1b-ga-ie | 1B parameter version |
| kingabzpro/wav2vec2-large-xls-r-1b-Irish | Alternative 1B version |
| jimregan/wav2vec2-large-xlsr-irish-basic | Basic fine-tuned version |

#### Facebook MMS (Massively Multilingual Speech)
- `facebook/mms-1b-all` - 1162 languages, includes Irish
- `facebook/mms-1b-l1107` - Alternative version

### Text-to-Speech
- **Facebook MMS-TTS:** `facebook/mms-tts-gle`
- **Coverage:** 1107+ languages

### Translation Models

| Direction | Model |
|-----------|-------|
| English → Irish | `Helsinki-NLP/opus-mt-en-ga` |
| Irish → English | `Helsinki-NLP/opus-mt-ga-en` |
| Multilingual | `facebook/m2m100_418M`, `facebook/m2m100_1.2B` |
| Efficient | `alirezamsh/small100` (10K+ pairs) |

### Key Datasets

| Dataset | Description |
|---------|-------------|
| Irish-English Parallel Collection | `ReliableAI/Irish-English-Parallel-Collection` |
| CC-100 | 108M Irish tokens |
| OSCAR | Multilingual web corpus |
| CulturaX | 6.3T tokens, 167 languages |
| Common Voice | Multiple versions (9.0-19.0) |
| Tatoeba-Speech-Irish | `ymoslem/Tatoeba-Speech-Irish` |
| IrishQA | Question-answering dataset |

### Key Organizations
- **DCU-NLP** (Dublin City University) - gaBERT, gaELECTRA
- **ReliableAI/ReML-AI** - UCCIX project
- **Helsinki-NLP** - Translation models
- **Facebook AI/Meta** - MMS, M2M100

### Research Gaps
- No Whisper fine-tuned models for Irish
- Limited NER datasets
- No sentiment analysis datasets
- IrishQA not yet on HuggingFace

---

## 2. Welsh (Cymraeg) Resources

**ISO Codes:** cy (639-1), cym (639-2/3)
**Speakers:** ~884,300 (2021 census)

### Language Models

#### BangorAI/Mistral-7B-Cymraeg-Welsh-v2
- **Type:** Bilingual Welsh-English chat/instruct (7B parameters)
- **Base:** Mistral-7B, trained on MADLAD-400 dataset
- **Demo:** https://demo.bangor.ai

### Speech Recognition (7+ models)

All from techiaith (Bangor University):

| Model | WER | Notes |
|-------|-----|-------|
| `techiaith/wav2vec2-xlsr-ft-cy` | 6.04% (4.05% with KenLM) | Best performance |
| `techiaith/wav2vec2-base-cy` | - | 4000 hours training |
| `techiaith/wav2vec2-xlsr-53-ft-cy-en` | - | Bilingual |
| `techiaith/whisper-large-v3-ft-verbatim-cy-en` | 28.99% | Spontaneous speech |
| `techiaith/whisper-large-v3-ft-commonvoice-cy-en` | - | CommonVoice v18 |
| `techiaith/whisper-base-ft-verbatim-cy-en-cpp` | - | Mobile optimized |

### Text-to-Speech
- **Facebook MMS-TTS:** `facebook/mms-tts-cym`
- **Architecture:** VITS end-to-end TTS

### Translation
- `AndreasThinks/mistral-7b-english-welsh-translate` - Government documents
- `facebook/m2m100_418M` - 100 languages including Welsh

### Other NLP Models
- `techiaith/fullstop-welsh-punctuation-prediction` - ASR punctuation restoration

### Key Datasets
| Dataset | Size/Notes |
|---------|------------|
| CC-100 | 179M Welsh tokens |
| MADLAD-400 | 419 languages |
| openai/welsh-texts | Historical National Library docs |
| Common Voice | Audio + transcriptions |
| techiaith/commonvoice_16_1_en_cy | 50/50 Welsh-English |

### Key Organizations
- **techiaith** (Bangor University) - Primary Welsh AI developer
- **BangorAI** - 21 models on HuggingFace

### Collections
- Speech Recognition: `techiaith/speech-recognition-models-660552d87de27e9581013dcf`
- Datasets: `techiaith/speech-recognition-datasets-672df8ffb3f7da8ed8294ce2`

---

## 3. Scottish Gaelic Resources

**ISO Codes:** gd (639-1), gla (639-2/3)
**Speakers:** ~69,700 (2011 census)

### Language Models

#### benjamin/gpt2-wechsel-scottish-gaelic
- **Type:** GPT-2 using WECHSEL transfer learning
- **Performance:** 16.43 PPL, 64x more efficient than from scratch

#### wietsedv/xlm-roberta-base-ft-udpos28-gd
- **Purpose:** POS tagging using Universal Dependencies

### Translation Models

| Model | Performance |
|-------|-------------|
| `Helsinki-NLP/opus-mt-synthetic-en-gd` | ChrF: 51.10, COMET: 78.04 |
| `facebook/m2m100_418M` | 100 languages |
| `alirezamsh/small100` | 0.3B params, 4.3x faster |
| `facebook/nllb-200-3.3B` | 200 languages |

### Key Datasets (38+)

| Dataset | Size/Notes |
|---------|------------|
| CC-100 | 22M tokens |
| GlotCC-V1 | 18.8k rows |
| XLSum | 2.31k BBC articles |
| OPUS-100 | Multilingual parallel |
| FLORES-200 | Evaluation dataset |
| Common Voice | Mozilla Data Collective |

### Development Status (2024-2025)
- 2025 paper achieved **12.8% WER** (32% improvement over Whisper)
- **£225k Scottish Government funding** for development
- Speech-to-text API expected **Q4 2025**

### Key Organizations
- **EdinburghNLP** - Active research
- **University of Edinburgh** - Ongoing development

---

## 4. Manx (Gaelg) Resources

**ISO Codes:** gv (639-1), glv (639-2/3)
**Speakers:** ~1,800 (2021 census)
**Status:** Critically endangered

### Translation Models

| Model | Direction | BLEU | ChrF |
|-------|-----------|------|------|
| `Helsinki-NLP/opus-mt-en-gv` | EN→GV | 70.1 | 0.885 |
| `Helsinki-NLP/opus-mt-gv-en` | GV→EN | 38.9 | 0.668 |
| `Helsinki-NLP/opus-mt-en-cel` | EN→Celtic | 9.9 (Manx) | 0.454 |
| `Helsinki-NLP/opus-mt-cel-en` | Celtic→EN | 11.0 (Manx) | 0.297 |

### Language Identification
- `speechbrain/lang-id-voxlingua107-ecapa` - 6.7% error rate, Manx confirmed
- TalTechNLP VoxLingua107 models

### Datasets
- **OPUS Corpus** - Tatoeba includes Manx pairs
- **Helsinki-NLP/tatoeba** - Translated sentences

### Resource Summary

| Category | Status |
|----------|--------|
| Translation | Available (4 models) |
| Datasets | Partially Available (2-3) |
| Language ID | Available |
| ASR | Not Found |
| TTS | Unconfirmed (MMS) |
| BERT/Embeddings | Not Found |

### GitHub Resources
- `kscanne/gaelg` - Manx lexicon, UD corpus

### Recommendations for Manx
1. Use Helsinki-NLP OPUS-MT for translation
2. Use VoxLingua107 for language ID
3. Access OPUS/Tatoeba for training data
4. Consider cross-lingual transfer from Irish/Scottish Gaelic

---

## 5. Comparison & Selection Guide

### Technology Comparison

| Feature | Irish | Scottish Gaelic | Welsh | Manx |
|---------|-------|-----------------|-------|------|
| **Dedicated LLM** | UCCIX | GPT-2 | Mistral 7B | None |
| **BERT-style** | gaBERT | Multilingual | None | None |
| **ASR** | 7+ models | In development | 7+ models | None |
| **TTS** | MMS | None | MMS | Unconfirmed |
| **Translation** | Excellent | Good | Good | Basic |
| **Datasets** | 10+ | 38+ | 8+ | 2-3 |

### Data Availability (Text Tokens)

| Language | Tokens | Primary Sources |
|----------|--------|-----------------|
| Welsh | 179M+ | CC-100, OSCAR, MADLAD-400 |
| Irish | 108M+ | CC-100, OSCAR, CulturaX |
| Scottish Gaelic | 22M | CC-100, mC4, GlotCC |
| Manx | Unknown | OPUS Tatoeba (limited) |

### Selection Guide by Use Case

#### Text Generation
| Language | Recommended Model |
|----------|-------------------|
| Irish | UCCIX-Llama2-13B-Instruct |
| Welsh | BangorAI/Mistral-7B-Cymraeg-Welsh-v2 |
| Scottish Gaelic | benjamin/gpt2-wechsel-scottish-gaelic |
| Manx | Cross-lingual transfer from Irish |

#### Speech Recognition
| Language | Recommended Model |
|----------|-------------------|
| Irish | wav2vec2-large-xlsr-53-irish |
| Welsh | techiaith/wav2vec2-xlsr-ft-cy (4.05% WER) |
| Scottish Gaelic | Fine-tune Whisper (no public models yet) |
| Manx | Not available |

#### Translation
| Language | Recommended Model |
|----------|-------------------|
| Irish | Helsinki-NLP/opus-mt-en-ga |
| Welsh | AndreasThinks/mistral-7b-english-welsh-translate |
| Scottish Gaelic | Helsinki-NLP/opus-mt-synthetic-en-gd |
| Manx | Helsinki-NLP/opus-mt-en-gv |

### Common Research Gaps (All Languages)
- Named Entity Recognition (NER)
- Sentiment Analysis
- Question Answering (only Irish has IrishQA)
- Celtic GLUE-equivalent evaluation suite

---

## Appendix A: Agno Agent Implementation Blueprint

For building Irish language AI agents using the Agno framework.

### Agent Architecture

The neuro-symbolic pipeline fuses LMMs with Knowledge Graphs:
- **Agno** as the agentic control plane (~3μs agent instantiation)
- **BAML** for schema-enforced structured output
- **Cognee** for semantic graph construction
- **Cocoindex** for incremental ETL

### Agent Team Topology

```python
from agno.agent import Agent
from agno.models.openai.like import OpenAILike

# Irish Language Agent Configuration
irish_llm = OpenAILike(
    id="uccix-13b",
    api_key=os.getenv("UCCIX_API_KEY"),
    base_url="https://api.uccix.ie/v1/",
    temperature=0.1,
)

# The Orchestrator
chief_agent = Agent(
    name="ChiefExaminer",
    role="Orchestrator for Irish language processing",
    model=irish_llm,
    markdown=True,
    monitoring=True
)
```

### Vision MCP Integration

```python
from agno.tools.mcp import MCPTools

vision_mcp_tools = MCPTools(
    command="npx",
    args=["-y", "@vision/mcp-server"],
    env={"API_KEY": os.getenv("VISION_API_KEY")}
)

vision_specialist = Agent(
    name="Palaeographer",
    role="Expert in Irish handwriting and Cló Gaelach",
    model=irish_llm,
    tools=[vision_mcp_tools],
)
```

### BAML Schema for Irish Content

```baml
enum ExamLevel {
    Higher
    Ordinary
    Foundation
}

class Question {
    number int
    topic string? @description("Mathematical topic in Irish")
    parts QuestionPart[]
    total_marks int
}

function ExtractExamData(exam_text: string) -> ExamPaper {
    client "uccix-13b"
    prompt #"
        Analyze the Irish language exam text.
        Preserve all Irish exactly (e.g., 'Gnáthleibhéal').
        Do not translate question content.
        {{ exam_text }}
        {{ ctx.output_format }}
    "#
}
```

### Observability with Langfuse

```python
from langfuse.decorators import observe

@observe(name="digitize_irish_document")
def run_digitization(pdf_path):
    response = chief_agent.run(f"Digitize: {pdf_path}")
    return response
```

---

## Appendix B: iOS Edge Deployment Guide

Deploying Irish LLMs on iPhone using Unsloth quantization.

### Architecture Constraints

| Device | RAM | Safe Model Size |
|--------|-----|-----------------|
| iPhone 14/15 | 6GB | ~2GB model |
| iPhone Pro | 8GB | ~3GB model |

**The 4-bit Solution:** Unsloth GGUF reduces footprint to ~0.7GB per 1B parameters.

### Recommended Model: Llama 3.2 3B

| Attribute | Value |
|-----------|-------|
| Size | ~1.9GB (Q4_K_M) |
| Context | 128K tokens |
| Unsloth Support | Native |

### Token Fertility Challenge

Irish morphology causes high token fertility in English-centric tokenizers:
- *deartháir* (brother) → 1 token (specialized) vs 3 tokens (English-centric)
- *bhean* (lenited woman) → fragmented if tokenizer doesn't recognize mutations

**Solution:** Use models with large vocabularies (Qwen 2.5: 151K, Llama 3.2: 128K).

### Unsloth Fine-Tuning Pipeline

```python
from unsloth import FastLanguageModel

model, tokenizer = FastLanguageModel.from_pretrained(
    model_name="unsloth/Llama-3.2-3B-Instruct-bnb-4bit",
    max_seq_length=8192,
    load_in_4bit=True,
)

# Add LoRA adapters for Irish
model = FastLanguageModel.get_peft_model(
    model,
    r=64,  # Higher rank for language adaptation
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                    "gate_proj", "up_proj", "down_proj"],
    lora_alpha=128,
    lora_dropout=0,
    use_gradient_checkpointing="unsloth",
)
```

### Dataset Engineering

| Tier | Source | Purpose |
|------|--------|---------|
| Raw Corpus | CulturaX (Irish) | Syntax & vocabulary |
| Domain | gaHealth | Medical Irish |
| Benchmark | IRLBench | Leaving Cert evaluation |
| Synthetic | OpenHermes localized | Instruction tuning |

### GGUF Export (Dynamic 2.0)

```python
model.save_pretrained_gguf(
    "Llama-3.2-3B-Irish-Instruct",
    tokenizer,
    quantization_method="q4_k_m"  # Best balance
)
```

### Swift Integration (AnyLanguageModel)

```swift
import AnyLanguageModel

class ModelController: ObservableObject {
    private var session: LanguageModelSession?

    func setupModel() {
        guard let path = Bundle.main.path(
            forResource: "Llama-3.2-3B-Irish-Instruct.Q4_K_M",
            ofType: "gguf"
        ) else { return }

        let model = LlamaLanguageModel(modelPath: path)
        self.session = LanguageModelSession(model: model)
    }

    func generate(prompt: String) async -> String {
        guard let session else { return "" }
        return try await session.respond(to: prompt).content
    }
}
```

### Package.swift Configuration

```swift
dependencies: [
    .package(
        url: "https://github.com/mattt/AnyLanguageModel.git",
        from: "0.5.0",
        traits: ["Llama"]  // Enable GGUF support
    )
]
```

### Benchmarking
- **IRLBench:** Leaving Cert reasoning in Irish
- **Irish-BLiMP:** Grammatical acceptability (minimal pairs)
- **gaHealth:** BLEU/COMET scores for medical translation

---

## Quick Reference Links

### Irish
- **Best LLM:** https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct
- **Best Encoder:** https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1
- **Demo:** https://aine.chat

### Welsh
- **Best LLM:** https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2
- **Best ASR:** https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy
- **Demo:** https://demo.bangor.ai

### Scottish Gaelic
- **Best LLM:** https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic
- **Best Translation:** https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd

### Manx
- **Best Translation:** https://huggingface.co/Helsinki-NLP/opus-mt-en-gv

---

## ISO Language Codes

| Language | ISO 639-1 | ISO 639-2/3 | Locale | Script |
|----------|-----------|-------------|--------|--------|
| Irish | ga | gle | ga-IE | Latn |
| Scottish Gaelic | gd | gla | gd-GB | Latn |
| Welsh | cy | cym | cy-GB | Latn |
| Manx | gv | glv | gv-IM | Latn |
