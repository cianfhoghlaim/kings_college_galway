# Celtic Language AI - Unified Model Comparison

## Executive Summary

This document provides a cross-language comparison of AI/ML resources for Celtic languages, enabling informed technology selection for multilingual Celtic projects.

---

## 1. Maturity Comparison

### 1.1 Overall Resource Availability

| Language | LLMs | ASR | TTS | Translation | Datasets | Maturity |
|----------|------|-----|-----|-------------|----------|----------|
| **Irish (Gaeilge)** | 5+ | 7+ | 1 | 4+ | 10+ | High |
| **Welsh (Cymraeg)** | 2 | 7+ | 1 | 2+ | 8+ | High |
| **Scottish Gaelic** | 2+ | 0* | 0 | 4+ | 38+ | Medium |
| **Manx (Gaelg)** | 0 | 0 | 0-1 | 4 | 2-3 | Low |

*Scottish Gaelic ASR/TTS expected Q4 2025

### 1.2 Feature Matrix

| Feature | Irish | Scottish Gaelic | Welsh | Manx |
|---------|-------|-----------------|-------|------|
| **Dedicated LLM** | UCCIX (13B, 70B) | GPT-2 WECHSEL | Mistral 7B | None |
| **BERT-style** | gaBERT, gaELECTRA | XLM-R only | None | None |
| **Fine-tuned ASR** | 7+ models | In development | 7+ models | None |
| **TTS** | MMS | None | MMS | Unconfirmed |
| **Dedicated Translation** | OPUS-MT | OPUS-MT synthetic | Mistral translate | OPUS-MT |

---

## 2. Best-in-Class Models by Task

### 2.1 Language Models (LLMs)

| Language | Best Model | Parameters | URL |
|----------|------------|------------|-----|
| **Irish** | UCCIX-Llama2-13B-Instruct | 13B | https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct |
| **Irish (Large)** | UCCIX-Llama3.1-70B-Instruct | 70B | https://huggingface.co/ReliableAI/UCCIX-Llama3.1-70B-Instruct-19122024 |
| **Welsh** | Mistral-7B-Cymraeg-Welsh-v2 | 7B | https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2 |
| **Scottish Gaelic** | gpt2-wechsel-scottish-gaelic | 124M | https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic |
| **Manx** | None available | - | Use multilingual models |

### 2.2 Encoder Models

| Language | Best Model | Training Data | URL |
|----------|------------|---------------|-----|
| **Irish** | gaBERT | 7.9M sentences | https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1 |
| **Irish (Alternative)** | gaELECTRA | 7.9M sentences | https://huggingface.co/DCU-NLP/electra-base-irish-cased-generator-v1 |
| **Others** | XLM-RoBERTa | Multilingual | https://huggingface.co/FacebookAI/xlm-roberta-base |

### 2.3 Speech Recognition (ASR)

| Language | Best Model | WER | URL |
|----------|------------|-----|-----|
| **Welsh** | wav2vec2-xlsr-ft-cy | 4.05% (with KenLM) | https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy |
| **Irish** | wav2vec2-large-xlsr-53-irish | Not reported | https://huggingface.co/cpierse/wav2vec2-large-xlsr-53-irish |
| **Scottish Gaelic** | None public | 12.8% (research) | Expected Q4 2025 |
| **Manx** | None | - | - |

### 2.4 Text-to-Speech (TTS)

| Language | Best Model | Architecture | URL |
|----------|------------|--------------|-----|
| **Irish** | mms-tts-gle | VITS | https://huggingface.co/facebook/mms-tts-gle |
| **Welsh** | mms-tts-cym | VITS | https://huggingface.co/facebook/mms-tts-cym |
| **Scottish Gaelic** | None | - | Check MMS coverage |
| **Manx** | Unconfirmed | - | Check MMS coverage |

### 2.5 Translation

| Direction | Best Model | Performance | URL |
|-----------|------------|-------------|-----|
| **English → Irish** | opus-mt-en-ga | CC-BY 4.0 | https://huggingface.co/Helsinki-NLP/opus-mt-en-ga |
| **Irish → English** | opus-mt-ga-en | CC-BY 4.0 | https://huggingface.co/Helsinki-NLP/opus-mt-ga-en |
| **English → Welsh** | mistral-7b-english-welsh-translate | Gov docs | https://huggingface.co/AndreasThinks/mistral-7b-english-welsh-translate |
| **English → Scottish Gaelic** | opus-mt-synthetic-en-gd | ChrF: 51.10 | https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd |
| **English → Manx** | opus-mt-en-gv | BLEU: 70.1 | https://huggingface.co/Helsinki-NLP/opus-mt-en-gv |
| **Manx → English** | opus-mt-gv-en | BLEU: 38.9 | https://huggingface.co/Helsinki-NLP/opus-mt-gv-en |
| **All Celtic** | m2m100_418M | 100 languages | https://huggingface.co/facebook/m2m100_418M |

---

## 3. Data Availability

### 3.1 Text Corpora Size

| Language | CC-100 Tokens | Other Sources |
|----------|---------------|---------------|
| **Welsh** | 179M | MADLAD-400, OSCAR |
| **Irish** | 108M | CulturaX, OSCAR |
| **Scottish Gaelic** | 22M | GlotCC, mC4 |
| **Manx** | Unknown | OPUS Tatoeba (limited) |

### 3.2 Dataset Count on HuggingFace

| Language | Datasets | Notable |
|----------|----------|---------|
| **Scottish Gaelic** | 38+ | Highest count |
| **Irish** | 10+ | Parallel corpora |
| **Welsh** | 8+ | Speech focus |
| **Manx** | 2-3 | Translation only |

---

## 4. Multilingual Model Coverage

### 4.1 Models Supporting All Celtic Languages

| Model | Languages | Celtic Support | URL |
|-------|-----------|----------------|-----|
| **M2M100-418M** | 100 | ga, gd, cy, gv | https://huggingface.co/facebook/m2m100_418M |
| **SMALL-100** | 101 | ga, gd, cy, gv | https://huggingface.co/alirezamsh/small100 |
| **NLLB-200** | 200 | ga, gd, cy | https://huggingface.co/facebook/nllb-200-3.3B |
| **XLM-RoBERTa** | 100 | ga, gd, cy | https://huggingface.co/FacebookAI/xlm-roberta-base |
| **MMS-ASR** | 1162 | ga (confirmed) | https://huggingface.co/facebook/mms-1b-all |

### 4.2 Celtic-Specific Multilingual

| Model | Languages | Direction | URL |
|-------|-----------|-----------|-----|
| **opus-mt-en-cel** | 6 Celtic | en → Celtic | https://huggingface.co/Helsinki-NLP/opus-mt-en-cel |
| **opus-mt-cel-en** | 6 Celtic | Celtic → en | https://huggingface.co/Helsinki-NLP/opus-mt-cel-en |

---

## 5. Key Organizations

| Organization | Languages | Focus Areas |
|--------------|-----------|-------------|
| **DCU-NLP** | Irish | gaBERT, gaELECTRA, NLP research |
| **ReliableAI/ReML-AI** | Irish | UCCIX LLMs, benchmarks |
| **techiaith** | Welsh | ASR, TTS, complete NLP pipeline |
| **BangorAI** | Welsh | LLMs, translation |
| **EdinburghNLP** | Scottish Gaelic | ASR, translation research |
| **Helsinki-NLP** | All Celtic | OPUS-MT translation models |
| **Facebook/Meta AI** | All | MMS, M2M100, XLM-R |
| **Mozilla Foundation** | All | Common Voice datasets |

---

## 6. Research Gaps

### 6.1 Universal Gaps (All Languages)

| Gap | Status | Impact |
|-----|--------|--------|
| **NER** | Limited across all | High |
| **Sentiment Analysis** | No dedicated models | High |
| **Evaluation Benchmarks** | No Celtic GLUE | Medium |
| **Question Answering** | Irish only (IrishQA) | Medium |

### 6.2 Language-Specific Gaps

| Language | Critical Gaps |
|----------|---------------|
| **Irish** | Whisper fine-tuned, sentiment |
| **Scottish Gaelic** | Public ASR/TTS (in development) |
| **Welsh** | Dedicated NER, sentiment |
| **Manx** | Everything except translation |

---

## 7. Performance Benchmarks

### 7.1 ASR Performance (WER)

| Language | Model | WER | Notes |
|----------|-------|-----|-------|
| **Welsh** | wav2vec2-xlsr-ft-cy | 4.05% | With KenLM |
| **Welsh** | wav2vec2-xlsr-ft-cy | 6.04% | Without LM |
| **Welsh** | whisper-large-v3-ft-verbatim | 28.99% | Spontaneous |
| **Scottish Gaelic** | Research model | 12.8% | Unpublished |
| **Irish** | wav2vec2-large-xlsr-53-irish | - | Not reported |

### 7.2 Translation Performance

| Direction | Model | BLEU | ChrF |
|-----------|-------|------|------|
| **en → gv (Manx)** | opus-mt-en-gv | 70.1 | 0.885 |
| **gv → en (Manx)** | opus-mt-gv-en | 38.9 | 0.668 |
| **en → gd (Scottish)** | opus-mt-synthetic-en-gd | - | 51.10 |

---

## 8. Technology Selection Guide

### 8.1 By Use Case

| Use Case | Irish | Welsh | Scottish Gaelic | Manx |
|----------|-------|-------|-----------------|------|
| **Chatbot/Assistant** | UCCIX | Mistral-7B-Cymraeg | GPT-2 WECHSEL | M2M100 |
| **Document Analysis** | gaBERT | XLM-R | XLM-R | XLM-R |
| **Speech-to-Text** | wav2vec2-xlsr-irish | wav2vec2-xlsr-ft-cy | Fine-tune Whisper | None |
| **Text-to-Speech** | MMS-TTS | MMS-TTS | Check MMS | Check MMS |
| **Translation** | OPUS-MT | Mistral translate | OPUS-MT synthetic | OPUS-MT |

### 8.2 By Resource Constraints

| Constraint | Recommendation |
|------------|----------------|
| **Low compute** | SMALL-100 (translation), GPT-2 (generation) |
| **Medium compute** | wav2vec2 (ASR), M2M100-418M (translation) |
| **High compute** | UCCIX-70B (Irish), Whisper Large (ASR) |
| **Offline/Edge** | whisper-base-cpp (Welsh), GGUF models |

---

## 9. Quick Reference Links

### 9.1 Demos

| Language | Demo | URL |
|----------|------|-----|
| **Irish** | Aine Chat | https://aine.chat |
| **Welsh** | BangorAI Demo | https://demo.bangor.ai |

### 9.2 Collections

| Organization | Collection | URL |
|--------------|------------|-----|
| **techiaith** | ASR Models | https://huggingface.co/collections/techiaith/speech-recognition-models-660552d87de27e9581013dcf |
| **techiaith** | ASR Datasets | https://huggingface.co/collections/techiaith/speech-recognition-datasets-672df8ffb3f7da8ed8294ce2 |

### 9.3 Browse by Language

| Language | Models | Datasets |
|----------|--------|----------|
| **Irish** | https://huggingface.co/models?language=ga | https://huggingface.co/datasets?language=language:gle |
| **Scottish Gaelic** | https://huggingface.co/models?language=gd | https://huggingface.co/datasets?language=language:gla |
| **Welsh** | https://huggingface.co/models?language=cy | https://huggingface.co/datasets?language=language:cym |
| **Manx** | - | https://huggingface.co/datasets?language=language:glv |

---

## 10. ISO Language Codes Reference

| Language | ISO 639-1 | ISO 639-2/3 | Locale | Script |
|----------|-----------|-------------|--------|--------|
| Irish | ga | gle | ga-IE | Latn |
| Scottish Gaelic | gd | gla | gd-GB | Latn |
| Welsh | cy | cym | cy-GB | Latn |
| Manx | gv | glv | gv-IM | Latn |

---

## Cross-References

This document consolidates and cross-references:
- `irish-nlp-resources.md` - Detailed Irish resources
- `scottish-gaelic-resources.md` - Detailed Scottish Gaelic resources
- `welsh-resources.md` - Detailed Welsh resources
- Main research Category 02 (Multimodal Document Intelligence) - Celtic OCR/VLM
- Main research Category 03 (AI-Native Data Pipelines) - Integration patterns
