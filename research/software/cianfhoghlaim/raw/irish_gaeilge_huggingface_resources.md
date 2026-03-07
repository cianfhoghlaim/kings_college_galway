# Irish (Gaeilge) Language AI Resources on HuggingFace

Comprehensive research conducted on 2025-11-17

## Table of Contents
1. [Language Models](#language-models)
2. [Datasets](#datasets)
3. [Translation Models](#translation-models)
4. [Speech Recognition (ASR) Models](#speech-recognition-asr-models)
5. [Text-to-Speech (TTS) Models](#text-to-speech-tts-models)
6. [Other NLP Resources](#other-nlp-resources)

---

## Language Models

### 1. UCCIX - Irish-eXcellence Large Language Model (2024)

**HuggingFace URLs:**
- https://huggingface.co/ReliableAI/UCCIX-Llama2-13B (Pre-trained base)
- https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct (Instruction-tuned)
- https://huggingface.co/ReliableAI/UCCIX-Llama3.1-70B-Instruct-19122024 (Newer Llama 3.1 version)

**Description:**
- First-ever open-source Irish-based Large Language Model
- Irish-English bilingual model based on Llama 2-13B
- Vocabulary expanded to include native Irish tokens
- Continued pre-training on ~520M Irish tokens
- Outperforms much larger models on Irish language tasks with up to 12% performance improvement

**Key Metrics:**
- Base: Llama 2-13B parameters
- Training data: ~520M Irish tokens
- Published: May 2024

**Resources:**
- Paper: https://huggingface.co/papers/2405.13010
- GitHub: https://github.com/ReML-AI/UCCIX
- Live demo: https://aine.chat
- arXiv: https://arxiv.org/abs/2405.13010

### 2. gaBERT - Irish BERT Model

**HuggingFace URL:**
- https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1

**Description:**
- Monolingual BERT-base model for Irish language
- Encoder-based Transformer for feature extraction
- Provides better representations than multilingual BERT for downstream parsing tasks
- Designed for fine-tuning on downstream Irish language tasks

**Key Metrics:**
- Training data: 7.9M Irish sentences
- Architecture: BERT-base (cased)
- Organization: DCU-NLP (Dublin City University)

**Resources:**
- Paper: "gaBERT - an Irish Language Model" (LREC 2022)
- GitHub: https://github.com/jbrry/Irish-BERT
- arXiv: https://arxiv.org/abs/2107.12930

### 3. gaELECTRA - Irish ELECTRA Model

**HuggingFace URL:**
- https://huggingface.co/DCU-NLP/electra-base-irish-cased-generator-v1

**Description:**
- ELECTRA-based model for Irish language
- Performs slightly below gaBERT but better than mBERT and WikiBERT
- Designed for efficient training and fine-tuning

**Key Metrics:**
- Training data: 7.9M Irish sentences
- Architecture: ELECTRA-base
- Organization: DCU-NLP

**Resources:**
- Introduced alongside gaBERT in the same 2022 paper

### 4. BERTreach - Irish RoBERTa Model

**HuggingFace URL:**
- https://huggingface.co/jimregan/BERTreach

**Description:**
- Monolingual Irish RoBERTa model
- Designed for Fill-Mask tasks
- Compatible with PyTorch, JAX, and Transformers

**Key Metrics:**
- Training data: 47 million tokens
- Architecture: RoBERTa
- License: Apache-2.0

### 5. WikiBERT-ga

**Description:**
- Monolingual Irish BERT model trained on Irish Wikipedia
- Available since May 2020
- Performs slightly better than mBERT but underperforms compared to gaBERT

**Key Metrics:**
- Training data: ~0.7M sentences (Irish Wikipedia only)
- Note: Less comprehensive than gaBERT due to limited training data

---

## Datasets

### Text Datasets

#### 1. Irish-English Parallel Collection

**HuggingFace URL:**
- https://huggingface.co/datasets/ReliableAI/Irish-English-Parallel-Collection

**Description:**
- Parallel corpus for Irish-English translation
- Used at the start of continual pre-training to help LLMs draw connections between Irish and English
- Part of the UCCIX project

#### 2. Common Voice (Multiple Versions)

**HuggingFace URLs:**
- https://huggingface.co/datasets/mozilla-foundation/common_voice_9_0
- https://huggingface.co/datasets/mozilla-foundation/common_voice_11_0
- https://huggingface.co/datasets/mozilla-foundation/common_voice_12_0
- https://huggingface.co/datasets/fsicoli/common_voice_17_0
- https://huggingface.co/datasets/fsicoli/common_voice_19_0

**Description:**
- Mozilla's crowdsourced speech dataset
- Irish (ga) included in multiple versions
- Consists of unique MP3 files and corresponding text
- Includes demographic metadata (age, sex, accent)

**How to Load:**
```python
from datasets import load_dataset
cv = load_dataset("mozilla-foundation/common_voice_13_0", "ga")
```

#### 3. CC-100 (CommonCrawl)

**HuggingFace URL:**
- https://huggingface.co/datasets/statmt/cc100

**Description:**
- Large monolingual dataset for 100+ languages
- Recreates the dataset used for training XLM-R
- Intended for pretraining language models

**Key Metrics:**
- Irish (ga): 108M tokens
- Source: CommonCrawl web scraping

#### 4. OSCAR Dataset

**HuggingFace URLs:**
- https://huggingface.co/datasets/oscar-corpus/OSCAR-2301
- https://huggingface.co/datasets/oscar-corpus/OSCAR-2201
- https://huggingface.co/datasets/oscar-corpus/OSCAR-2109
- https://huggingface.co/datasets/oscar-corpus/colossal-oscar-1.0

**Description:**
- Open Super-large Crawled Aggregated coRpus
- Multilingual web-based corpus
- Special attention to low-resource languages including Irish
- Note: Currently gated access (requires manual approval)

**Key Metrics:**
- Multiple versions available (2021-2023)
- Based on CommonCrawl dumps

#### 5. CulturaX

**HuggingFace URL:**
- https://huggingface.co/datasets/uonlp/CulturaX

**Description:**
- Substantial multilingual dataset
- Covers 167 languages including Irish

**Key Metrics:**
- Total: 6.3 trillion tokens across 167 languages

#### 6. Tatoeba-Speech-Irish

**HuggingFace URL:**
- https://huggingface.co/datasets/ymoslem/Tatoeba-Speech-Irish

**Description:**
- Synthetic audio dataset created using Azure TTS
- Bilingual text from Tatoeba dataset
- Two sets: female and male voices

**Key Metrics:**
- 1,983 text segments
- 3,966 utterances total
- ~2 hours 39 minutes of speech

#### 7. Tatoeba MT (Translation Benchmark)

**HuggingFace URL:**
- https://huggingface.co/datasets/Helsinki-NLP/tatoeba_mt

**Description:**
- Multilingual machine translation benchmark
- User-contributed translations from Tatoeba.org
- Includes Irish language pairs

#### 8. IrishQA (Question Answering)

**Description:**
- Question-answering dataset for Irish
- Created as part of UCCIX project
- Enables open-book question answering benchmarking

**Availability:**
- Not currently on HuggingFace Datasets hub
- Check UCCIX GitHub repository: https://github.com/ReML-AI/UCCIX

#### 9. Irish MT-bench

**Description:**
- Irish version of MT-bench for LLM evaluation
- Part of UCCIX project benchmarking suite
- Enables rigorous evaluation of Irish LLM systems

**Availability:**
- Check UCCIX GitHub repository

#### 10. XTREME-S

**HuggingFace URL:**
- https://huggingface.co/datasets/google/xtreme_s

**Description:**
- Multilingual speech benchmark
- Covers 102 languages including Irish
- Various speech tasks

---

## Translation Models

### 1. Helsinki-NLP OPUS-MT Models

#### English to Irish
**HuggingFace URL:**
- https://huggingface.co/Helsinki-NLP/opus-mt-en-ga

**Description:**
- Neural machine translation model (English → Irish)
- Transformer-align architecture with normalization
- SentencePiece preprocessing
- Source: English (eng), Target: Irish (gle)

**Key Metrics:**
- Architecture: Transformer
- License: CC-BY 4.0
- Organization: Language Technology Research Group, University of Helsinki

#### Irish to English
**HuggingFace URL:**
- https://huggingface.co/Helsinki-NLP/opus-mt-ga-en

**Description:**
- Neural machine translation model (Irish → English)
- Part of OPUS-MT project
- Originally trained using Marian NMT framework

### 2. M2M100 - Multilingual Translation

**HuggingFace URLs:**
- https://huggingface.co/facebook/m2m100_418M
- https://huggingface.co/facebook/m2m100_1.2B

**Description:**
- Many-to-many multilingual translation model
- Directly translates between 9,900 directions of 100 languages
- Irish (ga) included as supported language

**Key Metrics:**
- Model sizes: 418M and 1.2B parameters
- 100 languages, 9,900 translation pairs
- Organization: Facebook AI

### 3. SMaLL-100

**HuggingFace URL:**
- https://huggingface.co/alirezamsh/small100

**Description:**
- Compact multilingual machine translation model
- Covers 10K+ language pairs including Irish (ga)

---

## Speech Recognition (ASR) Models

### 1. Wav2Vec2-Large-XLSR-53-Irish

**HuggingFace URL:**
- https://huggingface.co/cpierse/wav2vec2-large-xlsr-53-irish

**Description:**
- Fine-tuned version of facebook/wav2vec2-large-xlsr-53
- Trained on Common Voice Irish dataset
- Ready-to-use for Irish speech recognition

**Key Metrics:**
- Base model: wav2vec2-large-xlsr-53
- Training data: Common Voice (Irish/Gaelic)
- Sampling rate: 16kHz
- Creator: cpierse

**Usage:**
```python
from transformers import Wav2Vec2Processor, Wav2Vec2ForCTC
processor = Wav2Vec2Processor.from_pretrained("cpierse/wav2vec2-large-xlsr-53-irish")
model = Wav2Vec2ForCTC.from_pretrained("cpierse/wav2vec2-large-xlsr-53-irish")
```

### 2. Wav2Vec2-Large-XLS-R-1B-Irish

**HuggingFace URLs:**
- https://huggingface.co/Aditya3107/wav2vec2-large-xls-r-1b-ga-ie
- https://huggingface.co/kingabzpro/wav2vec2-large-xls-r-1b-Irish

**Description:**
- Fine-tuned XLS-R 1B model for Irish ASR
- Trained on Common Voice 8.0 and Living Irish audio dataset
- Language code: ga-IE

**Key Metrics:**
- Base model: XLS-R 1B parameters
- Training data: Common Voice 8.0 + Living Irish

### 3. Wav2Vec2-XLSR-Irish-Basic

**HuggingFace URL:**
- https://huggingface.co/jimregan/wav2vec2-large-xlsr-irish-basic

**Description:**
- Basic Irish ASR model from jimregan
- Based on wav2vec2-large-xlsr

### 4. Facebook MMS-1B (Massively Multilingual Speech)

**HuggingFace URLs:**
- https://huggingface.co/facebook/mms-1b-all (1162 languages)
- https://huggingface.co/facebook/mms-1b-l1107 (1107 languages)
- https://huggingface.co/facebook/mms-1b-fl102 (102 languages)

**Description:**
- Multilingual ASR based on Wav2Vec2 architecture
- Uses adapter models for 1000+ languages
- Irish (ga/gle) included

**Key Metrics:**
- Model size: 1 billion parameters
- Languages: 1100+ including Irish
- Organization: Facebook AI

**Usage:**
```python
# Specify target_lang="gle" or "ga" for Irish
# Use ignore_mismatched_sizes=True when loading adapters
```

---

## Text-to-Speech (TTS) Models

### 1. Facebook MMS-TTS (Massively Multilingual Speech)

**HuggingFace URL:**
- https://huggingface.co/facebook/mms-tts (general)
- https://huggingface.co/facebook/mms-tts-gle (Irish-specific, if available)

**Description:**
- Massively multilingual TTS model
- Supports 1107+ languages
- Irish (gle) likely included
- Same architecture as VITS
- Separate checkpoint for each language

**Key Metrics:**
- Languages: 1107+
- Available in Transformers library from v4.33+
- Organization: Facebook AI

**Notes:**
- To verify Irish support, check: https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html
- Access using model identifier: facebook/mms-tts-gle

---

## Other NLP Resources

### 1. Multilingual Models Supporting Irish

#### XLM-RoBERTa

**HuggingFace URL:**
- https://huggingface.co/FacebookAI/xlm-roberta-base
- https://huggingface.co/FacebookAI/xlm-roberta-large

**Description:**
- Multilingual RoBERTa pre-trained on 100 languages including Irish
- Scaled cross-lingual sentence encoder
- Can be fine-tuned for various Irish NLP tasks

**Key Metrics:**
- Training data: 2.5TB filtered CommonCrawl
- Languages: 100 (including Irish)
- Use cases: Classification, NER, QA across languages

#### LaBSE (Language-agnostic BERT Sentence Embedding)

**HuggingFace URL:**
- https://huggingface.co/setu4993/LaBSE

**Description:**
- Multilingual sentence encoder
- Trained to encode sentence meaning across 109 languages
- Irish included

**Key Metrics:**
- Languages: 109 including Irish

### 2. Fine-tuned Models from jimregan

**User Profile:**
- https://huggingface.co/jimregan

**Available Models:**
- jimregan/BERTreach (Irish RoBERTa)
- jimregan/bert-base-irish-cased-v1-finetuned-ner (NER)
- jimregan/electra-base-irish-cased-discriminator-v1-finetuned-ner (NER)
- jimregan/wav2vec2-large-xlsr-irish-basic (ASR)

### 3. Collections

#### Irish-English Speech Translation Datasets Collection

**HuggingFace URL:**
- https://huggingface.co/collections/ymoslem/irish-english-speech-translation-datasets-665dd9e8fbaa279db3474ca0

**Description:**
- Curated collection of Irish-English speech translation resources
- Multiple datasets for speech translation research

---

## Summary Statistics

### Models by Type:
- **Language Models (LLMs/BERT):** 5+ models
- **Translation Models:** 4+ models
- **Speech Recognition (ASR):** 7+ models
- **Text-to-Speech (TTS):** 1+ model family
- **Multilingual Models with Irish:** 3+ models

### Datasets by Type:
- **Text Datasets:** 9+ datasets
- **Speech Datasets:** 3+ datasets
- **Translation Datasets:** 2+ datasets
- **Benchmark Datasets:** 2+ datasets

### Key Organizations:
- **DCU-NLP** (Dublin City University)
- **ReliableAI / ReML-AI** (UCCIX project)
- **Helsinki-NLP** (University of Helsinki)
- **Facebook AI / Meta**
- **Mozilla Foundation**
- **Individual contributors:** jimregan, cpierse, ymoslem, and others

---

## Notable Findings

1. **UCCIX** (2024) represents the most significant recent development - the first open-source Irish-based LLM with strong performance

2. **Common Voice** datasets are the primary source for Irish speech data across multiple versions

3. **Helsinki-NLP** provides bidirectional translation models (en-ga and ga-en)

4. **Facebook's MMS** project provides extensive coverage with 1000+ language support including Irish for both ASR and TTS

5. **DCU-NLP** has been a major contributor to Irish language models (gaBERT, gaELECTRA)

6. **Limited NER resources** - Few dedicated Irish NER datasets found on HuggingFace

7. **No Whisper fine-tuned models** for Irish were found, though fine-tuning is technically feasible

8. **WikiBERT-ga** exists but underperforms compared to newer models like gaBERT

---

## Research Gaps & Opportunities

1. **Whisper fine-tuning:** No Irish fine-tuned Whisper models found (opportunity for contribution)
2. **NER datasets:** Limited named entity recognition datasets for Irish
3. **Sentiment analysis:** No dedicated Irish sentiment analysis datasets found
4. **IrishQA availability:** Not yet on HuggingFace datasets (only on GitHub)
5. **GPT-style models:** Limited generative models besides UCCIX

---

## ISO Language Codes for Irish

- **ISO 639-1:** ga
- **ISO 639-2/3:** gle (Irish Gaelic)
- **Locale codes:** ga-IE (Irish - Ireland)

---

## Additional Resources

- **UCCIX GitHub:** https://github.com/ReML-AI/UCCIX
- **Irish-BERT GitHub:** https://github.com/jbrry/Irish-BERT
- **OPUS-MT GitHub:** https://github.com/Helsinki-NLP/Opus-MT
- **MMS Language Coverage:** https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html

---

*Research compiled: 2025-11-17*
*Total resources catalogued: 40+ models, datasets, and tools*
