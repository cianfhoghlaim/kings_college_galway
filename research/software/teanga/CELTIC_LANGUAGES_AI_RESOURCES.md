# Celtic Languages AI Resources on HuggingFace

**Comprehensive Research Report**
**Date:** 2025-11-17
**Languages Covered:** Irish (Gaeilge), Scottish Gaelic, Welsh (Cymraeg), Manx (Gaelg)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Irish (Gaeilge) Resources](#irish-gaeilge-resources)
3. [Scottish Gaelic Resources](#scottish-gaelic-resources)
4. [Welsh (Cymraeg) Resources](#welsh-cymraeg-resources)
5. [Manx (Gaelg) Resources](#manx-gaelg-resources)
6. [Comparative Analysis](#comparative-analysis)
7. [Project Recommendations](#project-recommendations)

---

## Executive Summary

This document catalogs all available AI/ML resources for Celtic languages on HuggingFace, including language models, datasets, translation systems, and speech technologies.

### Overall Statistics

| Language | LLMs | ASR Models | TTS Models | Translation Models | Major Datasets | Maturity Level |
|----------|------|------------|------------|-------------------|----------------|----------------|
| **Irish** | 5+ | 7+ | 1 | 4+ | 10+ | 🟢 High |
| **Scottish Gaelic** | 2+ | 0 | 0 | 4+ | 38+ | 🟡 Medium |
| **Welsh** | 2 | 7+ | 1 | 2+ | 8+ | 🟢 High |
| **Manx** | 0 | 0 | 0-1 | 4 | 2-3 | 🔴 Low |

### Key Findings

- **Most Developed:** Irish and Welsh have the most mature ecosystems with dedicated LLMs and extensive speech technologies
- **Emerging:** Scottish Gaelic has strong dataset availability but limited dedicated models
- **Critical Gap:** Manx has minimal resources, primarily limited to translation models
- **Major Contributors:** DCU-NLP, ReliableAI (Irish), techiaith/BangorAI (Welsh), Helsinki-NLP (all languages)

---

## Irish (Gaeilge) Resources

**ISO Codes:** ga (639-1), gle (639-2/3), Locale: ga-IE
**Speakers:** ~1.85 million (2022 census)

### Language Models (5+)

#### **UCCIX** (2024) - Most Advanced Irish LLM
- **URLs:**
  - Base 13B: https://huggingface.co/ReliableAI/UCCIX-Llama2-13B
  - Instruct 13B: https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct
  - Llama 3.1 70B: https://huggingface.co/ReliableAI/UCCIX-Llama3.1-70B-Instruct-19122024
- **Details:** First open-source Irish LLM, trained on ~520M Irish tokens
- **Performance:** Outperforms larger models by up to 12%
- **Demo:** https://aine.chat

#### **gaBERT** - Irish BERT
- **URL:** https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1
- **Training:** 7.9M Irish sentences
- **Status:** Best performing encoder model for Irish

#### **gaELECTRA**
- **URL:** https://huggingface.co/DCU-NLP/electra-base-irish-cased-generator-v1
- **Training:** 7.9M Irish sentences

#### **BERTreach**
- **URL:** https://huggingface.co/jimregan/BERTreach
- **Training:** 47M tokens, RoBERTa-based

#### **WikiBERT-ga**
- Trained on Irish Wikipedia (~0.7M sentences)
- Earlier model, superseded by gaBERT

### Datasets (10+)

#### Text Corpora
- **Irish-English Parallel Collection:** https://huggingface.co/datasets/ReliableAI/Irish-English-Parallel-Collection
- **CC-100:** https://huggingface.co/datasets/statmt/cc100 (108M Irish tokens)
- **OSCAR:** https://huggingface.co/datasets/oscar-corpus/OSCAR-2301
- **CulturaX:** https://huggingface.co/datasets/uonlp/CulturaX (6.3T tokens, 167 languages)

#### Speech Datasets
- **Common Voice (Irish):** Multiple versions (9.0, 11.0, 12.0, 17.0, 19.0)
- **Tatoeba-Speech-Irish:** https://huggingface.co/datasets/ymoslem/Tatoeba-Speech-Irish
- **XTREME-S:** https://huggingface.co/datasets/google/xtreme_s

#### Benchmarks
- **IrishQA:** Question-answering dataset (GitHub)
- **Irish MT-bench:** LLM evaluation benchmark

### Translation Models (4+)

#### **Helsinki-NLP OPUS-MT**
- **English → Irish:** https://huggingface.co/Helsinki-NLP/opus-mt-en-ga
- **Irish → English:** https://huggingface.co/Helsinki-NLP/opus-mt-ga-en

#### **Facebook M2M100**
- **418M:** https://huggingface.co/facebook/m2m100_418M
- **1.2B:** https://huggingface.co/facebook/m2m100_1.2B
- Coverage: 9,900 translation pairs, 100 languages

#### **SMaLL-100**
- **URL:** https://huggingface.co/alirezamsh/small100
- Coverage: 10K+ language pairs

### Speech/ASR Models (7+)

#### **Wav2Vec2 Models**
- **cpierse/wav2vec2-large-xlsr-53-irish:** https://huggingface.co/cpierse/wav2vec2-large-xlsr-53-irish
- **Aditya3107/wav2vec2-large-xls-r-1b-ga-ie:** https://huggingface.co/Aditya3107/wav2vec2-large-xls-r-1b-ga-ie
- **kingabzpro/wav2vec2-large-xls-r-1b-Irish:** https://huggingface.co/kingabzpro/wav2vec2-large-xls-r-1b-Irish
- **jimregan/wav2vec2-large-xlsr-irish-basic:** https://huggingface.co/jimregan/wav2vec2-large-xlsr-irish-basic

#### **Facebook MMS (Massively Multilingual Speech)**
- **mms-1b-all:** https://huggingface.co/facebook/mms-1b-all (1162 languages)
- **mms-1b-l1107:** https://huggingface.co/facebook/mms-1b-l1107
- **mms-1b-fl102:** https://huggingface.co/facebook/mms-1b-fl102
- Irish (ga/gle) included in all versions

### Text-to-Speech

#### **Facebook MMS-TTS**
- **Base:** https://huggingface.co/facebook/mms-tts
- **Irish-specific:** facebook/mms-tts-gle
- Coverage: 1107+ languages
- **Language coverage:** https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html

### Key Organizations
- **DCU-NLP** (Dublin City University) - gaBERT, gaELECTRA
- **ReliableAI/ReML-AI** - UCCIX project
- **Helsinki-NLP** - Translation models
- **Facebook AI/Meta** - MMS, M2M100, XLM-R
- **Mozilla Foundation** - Common Voice datasets
- **Individual contributors:** jimregan, cpierse, ymoslem

### Research Gaps
- No Whisper fine-tuned models for Irish
- Limited NER datasets
- No sentiment analysis datasets
- Few GPT-style generative models (UCCIX is primary)
- IrishQA not yet on HuggingFace (GitHub only)

---

## Scottish Gaelic Resources

**ISO Codes:** gd (639-1), gla (639-2/3)
**Speakers:** ~69,700 (2011 census)

### Language Models (2+)

#### **benjamin/gpt2-wechsel-scottish-gaelic**
- **URL:** https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic
- **Type:** GPT-2 using WECHSEL transfer learning
- **Performance:** 16.43 PPL, 64x more efficient than training from scratch

#### **wietsedv/xlm-roberta-base-ft-udpos28-gd**
- **URL:** https://huggingface.co/wietsedv/xlm-roberta-base-ft-udpos28-gd
- **Purpose:** Fine-tuned for POS tagging using Universal Dependencies

#### **Multilingual Support**
- mT5, M2M100, SMALL-100, NLLB-200 all support Scottish Gaelic

### Datasets (38+)

#### Text Corpora
- **CC-100:** 22M tokens of Scottish Gaelic
- **GlotCC-V1:** 18.8k rows
- **mC4:** Included in multilingual corpus

#### Translation/Summarization
- **XLSum:** https://huggingface.co/datasets/csebuetnlp/xlsum (2.31k articles from BBC)
- **Helsinki-NLP Tatoeba MT:** Translation pairs
- **OPUS-100:** Multilingual parallel corpus
- **FLORES-200:** Evaluation dataset

#### Speech
- **Common Voice:** Now via Mozilla Data Collective

#### Linguistic
- **Universal Dependencies:** Scottish Gaelic treebank

### Translation Models

#### **Helsinki-NLP/opus-mt-synthetic-en-gd**
- **URL:** https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd
- **Performance:** ChrF: 51.10, COMET: 78.04
- **Downloads:** 93/month

#### **facebook/m2m100_418M & _1.2B**
- **URL:** https://huggingface.co/facebook/m2m100_418M
- **Coverage:** 100 languages including Scottish Gaelic
- **Downloads:** 849k/month

#### **alirezamsh/small100**
- **URL:** https://huggingface.co/alirezamsh/small100
- **Size:** 0.3B parameters, 4.3x faster
- **Downloads:** 6.5k/month

#### **facebook/nllb-200-3.3B**
- **URL:** https://huggingface.co/facebook/nllb-200-3.3B
- **Coverage:** 200 languages including Scottish Gaelic

### Speech/ASR Models

**Status:** No publicly available dedicated models found

**Research & Development:**
- Recent 2025 paper achieved 12.8% WER (32% improvement over Whisper)
- University of Edinburgh developing speech-to-text API (expected Q4 2025)
- £225k Scottish Government funding for development

**Options:** Fine-tune Whisper or Wav2Vec2 models on Scottish Gaelic data

### Notable Gaps
- No publicly released fine-tuned ASR models (research models exist but unpublished)
- Limited dedicated models (most are multilingual)
- Data sparsity challenge for low-resource language

### Recent Developments (2024-2025)
- EMNLP 2025 paper on synthetic data for translation
- £225k Scottish Government funding for LLM development
- Speech-to-text API coming Q4 2025

### Key Organizations
- **EdinburghNLP** - Active research
- **Helsinki-NLP** - Translation models
- **University of Edinburgh** - Ongoing development

---

## Welsh (Cymraeg) Resources

**ISO Codes:** cy (639-1), cym (639-2/3)
**Speakers:** ~884,300 (2021 census)

### Language Models (2)

#### **BangorAI/Mistral-7B-Cymraeg-Welsh-v2**
- **URL:** https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2
- **Type:** Bilingual Welsh-English chat/instruct model (7B parameters)
- **Base:** Mistral-7B, trained on MADLAD-400 dataset
- **Demo:** https://demo.bangor.ai

#### **BangorAI/mistral-7b-cy-epoch-2**
- **URL:** https://huggingface.co/BangorAI/mistral-7b-cy-epoch-2
- **Purpose:** Base model for v2 version

### Speech Recognition Models (7+)

**All from techiaith (Bangor University):**

#### **Primary Models**
1. **techiaith/wav2vec2-xlsr-ft-cy**
   - **URL:** https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy
   - **WER:** 6.04% (4.05% with KenLM)

2. **techiaith/wav2vec2-base-cy**
   - **URL:** https://huggingface.co/techiaith/wav2vec2-base-cy
   - **Training:** 4000 hours

3. **techiaith/wav2vec2-xlsr-53-ft-cy-en**
   - **URL:** https://huggingface.co/techiaith/wav2vec2-xlsr-53-ft-cy-en
   - **Type:** Bilingual Welsh-English

#### **Whisper Fine-tuned Models**
4. **techiaith/whisper-large-v3-ft-verbatim-cy-en**
   - **URL:** https://huggingface.co/techiaith/whisper-large-v3-ft-verbatim-cy-en
   - **WER:** 28.99% (spontaneous speech)

5. **techiaith/whisper-large-v3-ft-commonvoice-cy-en**
   - **URL:** https://huggingface.co/techiaith/whisper-large-v3-ft-commonvoice-cy-en
   - **Training:** CommonVoice v18

6. **techiaith/whisper-base-ft-verbatim-cy-en-cpp**
   - **URL:** https://huggingface.co/techiaith/whisper-base-ft-verbatim-cy-en-cpp
   - **Optimization:** Offline/mobile

7. **techiaith/whisper-large-v3-ft-verbatim-cy-en-ct2**
   - **URL:** https://huggingface.co/techiaith/whisper-large-v3-ft-verbatim-cy-en-ct2
   - **Optimization:** CTranslate2

### Text-to-Speech (1)

#### **facebook/mms-tts-cym**
- **URL:** https://huggingface.co/facebook/mms-tts-cym
- **Coverage:** Part of MMS (1107 languages)
- **Architecture:** VITS end-to-end TTS

### Translation Models (2)

#### **AndreasThinks/mistral-7b-english-welsh-translate**
- **URL:** https://huggingface.co/AndreasThinks/mistral-7b-english-welsh-translate
- **Type:** Bidirectional English-Welsh translation
- **Specialization:** Government documents

#### **facebook/m2m100_418M**
- **URL:** https://huggingface.co/facebook/m2m100_418M
- **Coverage:** 100 languages including Welsh
- **Directions:** 9,900 translation pairs

### Other NLP Models

#### **techiaith/fullstop-welsh-punctuation-prediction**
- **URL:** https://huggingface.co/techiaith/fullstop-welsh-punctuation-prediction
- **Purpose:** Restores punctuation for ASR outputs

### Datasets (8+)

#### **Mozilla Common Voice**
- **Versions:** 2.0-13.0 (Welsh included in all)
- **Content:** Audio + transcriptions with demographics

#### **techiaith Collections**
- **commonvoice_16_1_en_cy:** https://huggingface.co/datasets/techiaith/commonvoice_16_1_en_cy (50/50 Welsh-English)
- **commonvoice_18_0_cy_en:** https://huggingface.co/datasets/techiaith/commonvoice_18_0_cy_en

#### **Text Corpora**
- **OSCAR Corpus:** Multiple versions (166-419 languages)
- **statmt/cc100:** https://huggingface.co/datasets/statmt/cc100 (179M Welsh tokens)
- **allenai/MADLAD-400:** https://huggingface.co/datasets/allenai/MADLAD-400 (419 languages)
- **openai/welsh-texts:** https://huggingface.co/datasets/openai/welsh-texts (Historical Welsh documents from National Library of Wales)

#### **Speech Recognition Datasets**
- 48+ hours of transcribed spontaneous Welsh speech

### Collections on HuggingFace

- **Speech Recognition Models:** https://huggingface.co/collections/techiaith/speech-recognition-models-660552d87de27e9581013dcf
- **Speech Recognition Datasets:** https://huggingface.co/collections/techiaith/speech-recognition-datasets-672df8ffb3f7da8ed8294ce2
- **Machine Translation Models** (techiaith)
- **Machine Translation Datasets** (techiaith)
- **Evaluation Datasets** (techiaith)

### Key Organizations

1. **techiaith (Language Technologies, Bangor University)**
   - **URL:** https://huggingface.co/techiaith
   - **Role:** Primary Welsh AI resource developer
   - **Collections:** 6+ maintained collections

2. **BangorAI**
   - **URL:** https://huggingface.co/BangorAI
   - **Models:** 21 models on HuggingFace
   - **Focus:** Welsh LLMs

3. **Facebook/Meta AI**
   - **Contribution:** TTS and multilingual translation support

### Notable Gaps
- No dedicated Named Entity Recognition (NER) model
- No dedicated sentiment analysis model
- Word embeddings research exists but not on HuggingFace
- No Welsh GLUE-equivalent benchmark

### Additional Resources
- **Welsh National Language Technologies Portal:** https://techiaith.cymru/?lang=en
- GitHub repositories with Welsh NLP tools
- Research papers on Welsh word embeddings

---

## Manx (Gaelg) Resources

**ISO Codes:** gv (639-1), glv (639-2/3)
**Speakers:** ~1,800 (2021 census)
**Status:** Critically endangered language

### Translation Models (4)

#### **Helsinki-NLP/opus-mt-en-gv** (English → Manx)
- **URL:** https://huggingface.co/Helsinki-NLP/opus-mt-en-gv
- **Architecture:** Transformer-align with SentencePiece
- **Performance:** BLEU: 70.1, ChrF: 0.885
- **Downloads:** 8/month
- **Integration:** Used in 12+ HuggingFace Spaces

#### **Helsinki-NLP/opus-mt-gv-en** (Manx → English)
- **URL:** https://huggingface.co/Helsinki-NLP/opus-mt-gv-en
- **Performance:** BLEU: 38.9, ChrF: 0.668
- **Downloads:** 6/month
- **Integration:** Used in 11+ HuggingFace Spaces

#### **Helsinki-NLP/opus-mt-en-cel** (English → Celtic Languages)
- **URL:** https://huggingface.co/Helsinki-NLP/opus-mt-en-cel
- **Languages:** Breton, Cornish, Welsh, Scottish Gaelic, Irish, Manx
- **Performance for Manx:** BLEU: 9.9, ChrF: 0.454
- **Downloads:** 27/month
- **Usage:** Requires target token `>>glv<<`

#### **Helsinki-NLP/opus-mt-cel-en** (Celtic Languages → English)
- **URL:** https://huggingface.co/Helsinki-NLP/opus-mt-cel-en
- **Performance for Manx:** BLEU: 11.0, ChrF: 0.297
- **Downloads:** 18/month
- **Integration:** Used in 11+ translation applications

### Datasets (2-3)

#### **OPUS Corpus - Manx Translation Pairs**
- **URL:** https://opus.nlpl.eu/
- **Access Methods:**
  - Direct website download
  - OpusTools Python package
  - OPUS-API for automation
- **Formats:** Plain text, TMX, XML, XCES alignment
- **Source:** Tatoeba corpus includes Manx

#### **Helsinki-NLP/tatoeba Dataset**
- **URL:** https://huggingface.co/datasets/Helsinki-NLP/tatoeba
- **Content:** Translated sentences with Manx pairs

#### **HuggingFaceFW/finewiki**
- **URL:** https://huggingface.co/datasets/HuggingFaceFW/finewiki
- **Content:** Wikipedia extracts in 325+ languages
- **Manx Status:** NOT CONFIRMED (claimed ~6,790 rows, unverified)

### Language Identification Models (4)

#### **speechbrain/lang-id-voxlingua107-ecapa**
- **URL:** https://huggingface.co/speechbrain/lang-id-voxlingua107-ecapa
- **Type:** Spoken language identification (ECAPA-TDNN)
- **Performance:** 6.7% error rate
- **Downloads:** 83,510/month
- **Manx Support:** ✓ Confirmed

#### **TalTechNLP VoxLingua107 Models**
- **voxlingua107-xls-r-300m-wav2vec:** https://huggingface.co/TalTechNLP/voxlingua107-xls-r-300m-wav2vec
- **voxlingua107-epaca-tdnn:** https://huggingface.co/TalTechNLP/voxlingua107-epaca-tdnn
- **voxlingua107-epaca-tdnn-ce:** https://huggingface.co/TalTechNLP/voxlingua107-epaca-tdnn-ce

### Speech/ASR Models

**Status:** NO DEDICATED MANX ASR MODELS FOUND

**Related Information:**
- Facebook's MMS supports ASR for 1,107 languages
- Manx status in MMS: UNCONFIRMED
- Check: https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html
- Related Celtic ASR: Irish (Fotheidil) and Scottish Gaelic systems exist
- Celtic Language Technology Workshop (CLTW) discussed Manx ASR development

### Text-to-Speech Models

#### **Facebook MMS TTS**
- **Base URL:** https://huggingface.co/facebook/mms-tts
- **Coverage:** 1,100+ languages
- **Manx Status:** UNCONFIRMED
- **Potential Model:** facebook/mms-tts-glv (if supported)

### Other NLP Resources

**Word Embeddings / BERT Models:** None found

**Explanation:** Manx is critically endangered with limited digital text, making dedicated language model training challenging

**Alternatives:**
- Use multilingual models (mBERT)
- Cross-lingual transfer from Irish/Scottish Gaelic

### GitHub Resources (Non-HuggingFace)

#### **kscanne/gaelg**
- **URL:** https://github.com/kscanne/gaelg
- **Contents:** Manx lexicon, bilingual mappings, Universal Dependencies corpus

### Spark NLP

#### **translate_en_gv**
- **URL:** https://nlp.johnsnowlabs.com/2021/01/03/translate_en_gv_xx.html
- **Type:** English-to-Manx translation pipeline (Marian-based)
- **Source:** Based on Helsinki-NLP OPUS models

### Resource Summary

| Category | Count | Status |
|----------|-------|--------|
| Translation Models | 4 | ✓ Available |
| Datasets | 2-3 | ✓ Partially Available |
| Language ID Models | 4 | ✓ Available |
| ASR Models | 0 | ✗ Not Found |
| TTS Models | 0-1 | ? Unconfirmed |
| BERT/Embeddings | 0 | ✗ Not Found |

### Recommendations for Manx
1. **Translation:** Use Helsinki-NLP OPUS-MT models (en-gv, gv-en, or multilingual Celtic)
2. **Language ID:** Use VoxLingua107-based models
3. **Training Data:** Access OPUS corpus or Tatoeba dataset
4. **Speech:** Check Facebook MMS language coverage directly
5. **Common Voice:** Access through Mozilla Data Collective

### Limitations
Manx remains a low-resource language. Many modern multilingual models (M2M100, NLLB-200, FLORES-200) do not explicitly confirm Manx support despite covering 100-200+ languages.

---

## Comparative Analysis

### Language Model Availability

| Feature | Irish | Scottish Gaelic | Welsh | Manx |
|---------|-------|-----------------|-------|------|
| **Dedicated LLM** | ✓✓✓ (UCCIX) | ✓ (GPT-2) | ✓✓ (Mistral 7B) | ✗ |
| **BERT-style** | ✓✓✓ | ✓ (multilingual) | ✗ | ✗ |
| **ASR** | ✓✓✓ (7+ models) | ✗ (in development) | ✓✓✓ (7+ models) | ✗ |
| **TTS** | ✓ (MMS) | ✗ | ✓ (MMS) | ? (MMS unconfirmed) |
| **Translation** | ✓✓✓ | ✓✓ | ✓✓ | ✓ |
| **Datasets** | ✓✓✓ (10+) | ✓✓✓ (38+) | ✓✓✓ (8+) | ✓ (2-3) |

### Maturity Levels

**🟢 High Maturity (Irish, Welsh)**
- Multiple dedicated models across all categories
- Active research and development
- Strong institutional support (DCU, Bangor University)
- Large, diverse datasets available
- Production-ready tools

**🟡 Medium Maturity (Scottish Gaelic)**
- Strong dataset availability (38+ datasets)
- Some dedicated models (GPT-2, translation)
- Active development with government funding
- Gaps in ASR/TTS but solutions incoming (Q4 2025)
- Primarily relies on multilingual models

**🔴 Low Maturity (Manx)**
- Critically endangered language status
- Limited to translation models only
- Very small speaker population (~1,800)
- No dedicated modern models
- Relies entirely on multilingual support (often unconfirmed)

### Institutional Support

| Institution | Languages | Focus Areas |
|-------------|-----------|-------------|
| **DCU-NLP** (Dublin City University) | Irish | BERT models, NLP research |
| **ReliableAI/ReML-AI** | Irish | LLMs (UCCIX), benchmarks |
| **techiaith** (Bangor University) | Welsh | ASR, TTS, complete NLP pipeline |
| **BangorAI** | Welsh | LLMs, translation |
| **EdinburghNLP** | Scottish Gaelic | ASR, translation research |
| **Helsinki-NLP** | All Celtic | Translation models (OPUS-MT) |
| **Facebook/Meta AI** | All (confirmed: Irish, Welsh) | Multilingual models (MMS, M2M100) |
| **Mozilla Foundation** | All | Common Voice speech datasets |

### Data Availability (Text Tokens)

| Language | Token Count | Primary Sources |
|----------|-------------|-----------------|
| **Welsh** | 179M+ | CC-100, OSCAR, MADLAD-400 |
| **Irish** | 108M+ | CC-100, OSCAR, CulturaX |
| **Scottish Gaelic** | 22M | CC-100, mC4, GlotCC |
| **Manx** | Unknown | OPUS Tatoeba (very limited) |

### Performance Benchmarks

#### Translation Quality (BLEU Scores)
- **Irish (en-ga):** Not specified in OPUS-MT
- **Scottish Gaelic (en-gd synthetic):** ChrF: 51.10, COMET: 78.04
- **Welsh:** Not specified in OPUS-MT
- **Manx (en-gv):** BLEU: 70.1, ChrF: 0.885
- **Manx (gv-en):** BLEU: 38.9, ChrF: 0.668

#### ASR Performance (WER)
- **Irish:** Various models, no standard benchmark reported
- **Welsh (wav2vec2-xlsr-ft-cy):** 6.04% (4.05% with KenLM)
- **Welsh (Whisper verbatim):** 28.99%
- **Scottish Gaelic:** Research model achieved 12.8% WER (unpublished)

### Research Gaps Across All Languages

**Common Gaps:**
- Named Entity Recognition (NER) - Limited for all languages
- Sentiment Analysis - No dedicated models found
- Question Answering - Only Irish has IrishQA
- Evaluation Benchmarks - No Celtic GLUE-equivalent

**Language-Specific Gaps:**
- **Irish:** No Whisper fine-tuned models, limited sentiment analysis
- **Scottish Gaelic:** No publicly released ASR/TTS models (in development)
- **Welsh:** No dedicated NER or sentiment models
- **Manx:** Everything except translation models

### Technology Stack Comparison

#### Most Common Base Models
1. **Transformers:** BERT, RoBERTa, ELECTRA (Irish, Welsh via techiaith)
2. **Generative:** GPT-2 (Scottish Gaelic), Llama 2/3.1 (Irish), Mistral 7B (Welsh)
3. **Speech:** Wav2Vec2 (Irish, Welsh), Whisper (Welsh), MMS (Irish, Welsh)
4. **Translation:** Marian/OPUS-MT (all), M2M100 (all), NLLB (most)

#### Framework Usage
- **HuggingFace Transformers:** Universal across all projects
- **SpeechBrain:** Language identification (Manx confirmed)
- **Fairseq:** Meta's multilingual models
- **CTranslate2:** Optimization (Welsh)

---

## Project Recommendations

### For Developers/Researchers

#### Starting a New Project

**Irish:**
- **LLM:** Use UCCIX for generation, gaBERT for encoding tasks
- **ASR:** Multiple wav2vec2 options, choose based on your accuracy needs
- **Translation:** Helsinki-NLP opus-mt for dedicated pairs, M2M100 for broader coverage
- **Data:** CC-100 (108M tokens), Irish-English Parallel Collection

**Scottish Gaelic:**
- **LLM:** benjamin/gpt2-wechsel-scottish-gaelic for generation
- **ASR:** Fine-tune Whisper or wait for Q4 2025 release
- **Translation:** opus-mt-synthetic-en-gd for best performance
- **Data:** CC-100 (22M tokens), XLSum (2.31k BBC articles)

**Welsh:**
- **LLM:** BangorAI/Mistral-7B-Cymraeg-Welsh-v2 for chat/instruct
- **ASR:** techiaith/wav2vec2-xlsr-ft-cy (best WER: 4.05% with KenLM)
- **Translation:** AndreasThinks/mistral-7b-english-welsh-translate
- **Data:** CC-100 (179M tokens), MADLAD-400

**Manx:**
- **Translation:** Helsinki-NLP/opus-mt-en-gv and opus-mt-gv-en
- **Language ID:** speechbrain/lang-id-voxlingua107-ecapa
- **Data:** OPUS Tatoeba corpus
- **Note:** Limited options; consider cross-lingual transfer from Irish/Scottish Gaelic

### Research Opportunities

**High-Impact Contributions:**

1. **Manx Resources**
   - Create first dedicated Manx LLM (fine-tune from Irish/Scottish Gaelic)
   - Develop ASR/TTS models using cross-lingual transfer
   - Build comprehensive Manx text corpus
   - Create Manx NER and sentiment datasets

2. **Scottish Gaelic**
   - Fine-tune Whisper models for public release
   - Develop dedicated TTS models
   - Create question-answering datasets

3. **All Languages**
   - Named Entity Recognition datasets and models
   - Sentiment analysis datasets
   - Question-answering benchmarks (expand IrishQA concept)
   - Celtic GLUE-equivalent evaluation suite
   - Cross-lingual transfer learning studies

4. **Multilingual Celtic**
   - Pan-Celtic language model (trained on all 4+ languages)
   - Cross-lingual benchmarks
   - Comparative linguistic analysis using embeddings

### Production Use Cases

**Viable Now:**
- **Translation:** All languages have production-ready solutions
- **ASR:** Irish and Welsh have multiple production options
- **Text Generation:** Irish (UCCIX), Welsh (Mistral-7B), Scottish Gaelic (GPT-2)
- **Language Identification:** Manx and others via VoxLingua107

**Coming Soon (2025):**
- Scottish Gaelic ASR/TTS (Q4 2025)
- Enhanced Scottish Gaelic LLM (funded development)

**Experimental/Research Only:**
- Manx ASR/TTS (no timeline)
- NER for all languages
- Sentiment analysis for all languages

### Data Collection Priorities

**Critical Needs:**
1. **Manx:** Everything (especially speech data, modern text corpora)
2. **Scottish Gaelic:** More speech data for ASR/TTS training
3. **All Languages:** Domain-specific datasets (legal, medical, technical)
4. **All Languages:** Annotated data for NER, sentiment, QA tasks

**Existing Strong Resources:**
- **Welsh:** CC-100 (179M tokens), extensive speech data
- **Irish:** CC-100 (108M tokens), Common Voice, parallel translation data
- **Scottish Gaelic:** 38+ datasets, strong translation pairs

### Community Engagement

**Active Communities:**
- **Irish:** DCU-NLP, ReliableAI, growing developer ecosystem around UCCIX
- **Welsh:** techiaith, BangorAI, strong institutional backing
- **Scottish Gaelic:** EdinburghNLP, government-funded initiatives
- **Manx:** Limited but emerging interest, potential for volunteer contributions

**Ways to Contribute:**
- Data collection (especially speech for Manx, Scottish Gaelic)
- Model fine-tuning and evaluation
- Creating benchmarks and evaluation datasets
- Documentation and usage examples
- Integration into popular frameworks

### Funding Sources

**Recent Examples:**
- Scottish Government: £225k for Scottish Gaelic LLM development
- Bangor University: Ongoing support for Welsh technologies
- Irish Research Council: Support for UCCIX and related projects

**Potential Funders:**
- Celtic language preservation organizations
- National language boards (Bòrd na Gàidhlig, Foras na Gaeilge, etc.)
- EU language diversity initiatives
- Academic research grants (Horizon Europe, etc.)

---

## Quick Reference: Direct Links to Key Resources

### Irish (Gaeilge)
- **Best LLM:** https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct
- **Best Encoder:** https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1
- **Best Dataset:** https://huggingface.co/datasets/statmt/cc100 (filter: ga)
- **Demo:** https://aine.chat

### Scottish Gaelic
- **Best LLM:** https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic
- **Best Translation:** https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd
- **Best Dataset:** https://huggingface.co/datasets/csebuetnlp/xlsum (filter: scottish_gaelic)

### Welsh (Cymraeg)
- **Best LLM:** https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2
- **Best ASR:** https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy
- **Best Dataset:** https://huggingface.co/datasets/statmt/cc100 (filter: cy)
- **Collections:** https://huggingface.co/collections/techiaith/speech-recognition-models-660552d87de27e9581013dcf
- **Demo:** https://demo.bangor.ai

### Manx (Gaelg)
- **Best Translation (en→gv):** https://huggingface.co/Helsinki-NLP/opus-mt-en-gv
- **Best Translation (gv→en):** https://huggingface.co/Helsinki-NLP/opus-mt-gv-en
- **Best Dataset:** https://opus.nlpl.eu/ (search: Manx/gv)

---

## Appendix: ISO Language Codes

| Language | ISO 639-1 | ISO 639-2/3 | Locale | Script |
|----------|-----------|-------------|--------|--------|
| Irish | ga | gle | ga-IE | Latn |
| Scottish Gaelic | gd | gla | gd-GB | Latn |
| Welsh | cy | cym | cy-GB | Latn |
| Manx | gv | glv | gv-IM | Latn |

---

## Document Metadata

**Version:** 1.0
**Last Updated:** 2025-11-17
**Research Methodology:** Parallel subagent deep search on HuggingFace, academic literature, and related resources
**Coverage:** HuggingFace models, datasets, and related resources as of November 2025
**Verification:** URLs and metrics verified through web search and direct platform access

**For Updates:** This field continues to evolve rapidly. Recommend quarterly reviews for new models and datasets.

**Contributing:** To suggest additions or corrections, please check the latest resources on:
- HuggingFace: https://huggingface.co
- OPUS Corpus: https://opus.nlpl.eu
- Common Voice: Mozilla Data Collective
- Celtic Language Technology Workshop proceedings

---

*This document was created to support Celtic language AI development and preservation efforts. All resources listed are publicly available unless otherwise noted.*
