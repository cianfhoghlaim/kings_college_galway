# Celtic Language AI Resources

This directory consolidates research on AI/ML resources for Celtic languages available on HuggingFace and related platforms, including language models, datasets, translation systems, and speech technologies.

## Overview

The Celtic language AI ecosystem spans four major languages with varying levels of maturity:

| Language | LLMs | ASR | TTS | Translation | Datasets | Maturity |
|----------|------|-----|-----|-------------|----------|----------|
| **Irish (Gaeilge)** | 5+ | 7+ | 1 | 4+ | 10+ | High |
| **Welsh (Cymraeg)** | 2 | 7+ | 1 | 2+ | 8+ | High |
| **Scottish Gaelic** | 2+ | 0* | 0 | 4+ | 38+ | Medium |
| **Manx (Gaelg)** | 0 | 0 | 0-1 | 4 | 2-3 | Low |

*Scottish Gaelic ASR/TTS expected Q4 2025

## Documents in this Category

| Document | Focus | Key Resources |
|----------|-------|---------------|
| `irish-nlp-resources.md` | Irish (Gaeilge) models and datasets | UCCIX, gaBERT, Common Voice |
| `scottish-gaelic-resources.md` | Scottish Gaelic resources | GPT-2 WECHSEL, XLSum |
| `welsh-resources.md` | Welsh (Cymraeg) resources | Mistral-7B-Cymraeg, techiaith ASR |
| `unified-model-comparison.md` | Cross-language analysis and recommendations | All languages |

## Key Organizations

| Organization | Languages | Focus Areas |
|--------------|-----------|-------------|
| **DCU-NLP** (Dublin City University) | Irish | gaBERT, gaELECTRA, NLP research |
| **ReliableAI/ReML-AI** | Irish | UCCIX LLMs, benchmarks |
| **techiaith** (Bangor University) | Welsh | ASR, TTS, complete NLP pipeline |
| **BangorAI** | Welsh | LLMs, translation |
| **EdinburghNLP** | Scottish Gaelic | ASR, translation research |
| **Helsinki-NLP** | All Celtic | OPUS-MT translation models |
| **Facebook/Meta AI** | All | MMS, M2M100, XLM-R |
| **Mozilla Foundation** | All | Common Voice datasets |

## Quick Reference: Best-in-Class Models

### Irish (Gaeilge)
- **LLM:** [UCCIX-Llama2-13B-Instruct](https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct) - First open-source Irish LLM
- **Encoder:** [gaBERT](https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1) - Best for NLP tasks
- **ASR:** [wav2vec2-large-xlsr-53-irish](https://huggingface.co/cpierse/wav2vec2-large-xlsr-53-irish)
- **Translation:** [opus-mt-en-ga](https://huggingface.co/Helsinki-NLP/opus-mt-en-ga)
- **Demo:** https://aine.chat

### Welsh (Cymraeg)
- **LLM:** [Mistral-7B-Cymraeg-Welsh-v2](https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2)
- **ASR:** [wav2vec2-xlsr-ft-cy](https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy) - 6.04% WER (4.05% with KenLM)
- **Collections:** https://huggingface.co/collections/techiaith/
- **Demo:** https://demo.bangor.ai

### Scottish Gaelic
- **LLM:** [gpt2-wechsel-scottish-gaelic](https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic)
- **Translation:** [opus-mt-synthetic-en-gd](https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd)
- **Dataset:** [XLSum](https://huggingface.co/datasets/csebuetnlp/xlsum) (2.31k BBC articles)

### Manx (Gaelg)
- **Translation:** [opus-mt-en-gv](https://huggingface.co/Helsinki-NLP/opus-mt-en-gv) (BLEU: 70.1)
- **Language ID:** [lang-id-voxlingua107-ecapa](https://huggingface.co/speechbrain/lang-id-voxlingua107-ecapa)
- **Dataset:** [OPUS Corpus](https://opus.nlpl.eu/) (Tatoeba)

## Data Availability

| Language | Text Tokens | Primary Sources |
|----------|-------------|-----------------|
| **Welsh** | 179M+ | CC-100, OSCAR, MADLAD-400 |
| **Irish** | 108M+ | CC-100, OSCAR, CulturaX |
| **Scottish Gaelic** | 22M | CC-100, mC4, GlotCC |
| **Manx** | Limited | OPUS Tatoeba |

## Research Gaps

### Universal Gaps
- Named Entity Recognition (NER) - Limited across all languages
- Sentiment Analysis - No dedicated models found
- Evaluation Benchmarks - No Celtic GLUE-equivalent

### Language-Specific Gaps
- **Irish:** No Whisper fine-tuned models
- **Scottish Gaelic:** No public ASR/TTS models (in development)
- **Welsh:** No dedicated NER model
- **Manx:** Everything except translation

## Cross-References

This category extends and connects to:
- **Category 02 (Multimodal Document Intelligence)** - Celtic OCR/VLM models
- **Category 03 (AI-Native Data Pipelines)** - Integration with dlt/Dagster pipelines
- **Main Research:** `../../organized/02-multimodal-document-intelligence/`

## ISO Language Codes

| Language | ISO 639-1 | ISO 639-2/3 | Locale | Script |
|----------|-----------|-------------|--------|--------|
| Irish | ga | gle | ga-IE | Latn |
| Scottish Gaelic | gd | gla | gd-GB | Latn |
| Welsh | cy | cym | cy-GB | Latn |
| Manx | gv | glv | gv-IM | Latn |

## Source Files Consolidated

- `CELTIC_LANGUAGES_AI_RESOURCES.md` - Cross-language comparison
- `irish_gaeilge_huggingface_resources.md` - Irish-specific resources
- `scottish_gaelic_huggingface_resources.md` - Scottish Gaelic resources
- `welsh-huggingface-resources.md` - Welsh resources
