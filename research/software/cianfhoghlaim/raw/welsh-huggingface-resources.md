# Welsh (Cymraeg) Language AI Resources on HuggingFace

**Comprehensive Research Report**
**Date:** November 17, 2025
**ISO Language Code:** cy (Welsh/Cymraeg)

---

## Table of Contents

1. [Language Models (LLMs)](#language-models-llms)
2. [Speech Recognition Models (ASR)](#speech-recognition-models-asr)
3. [Text-to-Speech Models (TTS)](#text-to-speech-models-tts)
4. [Translation Models](#translation-models)
5. [Other NLP Models](#other-nlp-models)
6. [Datasets](#datasets)
7. [Key Organizations](#key-organizations)
8. [Collections](#collections)
9. [Resources Not Available](#resources-not-available)

---

## Language Models (LLMs)

### 1. BangorAI/Mistral-7B-Cymraeg-Welsh-v2

**URL:** https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2

**Description:** A bilingual Mistral chat/instruct model trained in both English and Welsh languages.

**Key Details:**
- Based on BangorAI/mistral-7b-cy-epoch-2 (continual pre-training of Mistral-7B-v0.1)
- Pre-trained with Welsh data from allenai/MADLAD-400 dataset for 2 epochs
- Fine-tuned using yahma/alpaca-cleaned dataset in both Welsh and English for 2 epochs
- Supports bilingual applications with language-specific system prompts
- Online demo available at https://demo.bangor.ai

**Metrics:**
- 7B parameters
- Part of BangorAI's 21 models on HuggingFace

---

### 2. BangorAI/mistral-7b-cy-epoch-2

**URL:** https://huggingface.co/BangorAI/mistral-7b-cy-epoch-2

**Description:** Base continual pre-training model used to create the v2 version.

**Key Details:**
- Continual pre-training of Mistral-7B-v0.1 with Welsh data
- Trained on allenai/MADLAD-400 dataset for 2 epochs

---

## Speech Recognition Models (ASR)

### 1. techiaith/wav2vec2-xlsr-ft-cy

**URL:** https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy

**Description:** Welsh acoustic model for speech recognition based on wav2vec2 XLSR architecture.

**Key Details:**
- Fine-tuned version of Facebook/Meta AI's XLSR-53 pre-trained model
- Trained on Mozilla CommonVoice Welsh dataset and OSCAR corpus
- Suitable for read or planned Welsh language speech

**Metrics:**
- WER: 6.04% on Welsh Common Voice version 11 test set
- WER: 4.05% when assisted by KenLM language model
- WER: 14% on the Welsh Common Voice test set (older version)

---

### 2. techiaith/wav2vec2-base-cy

**URL:** https://huggingface.co/techiaith/wav2vec2-base-cy

**Description:** Base wav2vec2 model for Welsh speech recognition.

**Key Details:**
- Trained using 4000 hours of Welsh and English speech audio from YouTube
- Approximately 25% Welsh speech and 75% English language speech
- Latest version investigates fine-tuning Meta AI's pre-trained xls-r-1b models
- Transcribed using Whisper-based ASR models

**Metrics:**
- 4000 hours of training data

---

### 3. techiaith/wav2vec2-xlsr-53-ft-cy-en

**URL:** https://huggingface.co/techiaith/wav2vec2-xlsr-53-ft-cy-en

**Description:** Bilingual Welsh-English speech recognition model.

**Key Details:**
- Fine-tuned from Facebook's wav2vec2-large-xlsr-53
- Supports both Welsh and English transcription

---

### 4. techiaith/whisper-large-v3-ft-verbatim-cy-en

**URL:** https://huggingface.co/techiaith/whisper-large-v3-ft-verbatim-cy-en

**Description:** Fine-tuned Whisper model for verbatim transcription of spontaneous Welsh speech.

**Key Details:**
- Fine-tuned from openai/whisper-large-v3
- Trained on Banc Trawsgrifiadau Bangor (btb) transcriptions
- Trained on Lleisiau Arfor spontaneous speech dataset
- Includes Welsh Common Voice version 18 read speech recordings
- Suitable for verbatim transcribing of spontaneous or unplanned speech

**Metrics:**
- WER: 28.99 on Banc Trawsgrifiadau Bangor test set
- CER: 10.27 on Banc Trawsgrifiadau Bangor test set

---

### 5. techiaith/whisper-large-v3-ft-commonvoice-cy-en

**URL:** https://huggingface.co/techiaith/whisper-large-v3-ft-commonvoice-cy-en

**Description:** Fine-tuned Whisper model on CommonVoice dataset.

**Key Details:**
- Fine-tuned from openai/whisper-large-v3
- Trained on techiaith/commonvoice_18_0_cy_en dataset
- Uses both English and Welsh data for bilingual transcription

---

### 6. techiaith/whisper-base-ft-verbatim-cy-en-cpp

**URL:** https://huggingface.co/techiaith/whisper-base-ft-verbatim-cy-en-cpp

**Description:** Whisper model converted for use in whisper.cpp for offline transcription.

**Key Details:**
- Based on openai/whisper-base model
- Converted for whisper.cpp compatibility
- Provides high performance inference on desktops, laptops, and mobile devices
- Offers offline transcription option
- Fine-tuned with Welsh Common Voice version 18

---

### 7. techiaith/whisper-large-v3-ft-verbatim-cy-en-ct2

**URL:** https://huggingface.co/techiaith/whisper-large-v3-ft-verbatim-cy-en-ct2

**Description:** CTranslate2 optimized version of the Whisper model.

**Key Details:**
- Optimized for faster inference using CTranslate2
- Compatible with WhisperX

---

## Text-to-Speech Models (TTS)

### 1. facebook/mms-tts-cym

**URL:** https://huggingface.co/facebook/mms-tts-cym

**Description:** Welsh (cym) language text-to-speech model checkpoint from Facebook's Massively Multilingual Speech project.

**Key Details:**
- Part of Facebook's MMS project covering 1107 languages
- Uses VITS (Variational Inference with adversarial learning for end-to-end TTS) architecture
- Available in Transformers library from version 4.33 onwards
- End-to-end speech synthesis predicting waveform from text

**Usage:**
```python
from transformers import VitsModel, AutoTokenizer
import torch

model = VitsModel.from_pretrained("facebook/mms-tts-cym")
tokenizer = AutoTokenizer.from_pretrained("facebook/mms-tts-cym")

text = "some example text in the Welsh language"
inputs = tokenizer(text, return_tensors="pt")

with torch.no_grad():
    output = model(**inputs).waveform
```

---

## Translation Models

### 1. AndreasThinks/mistral-7b-english-welsh-translate

**URL:** https://huggingface.co/AndreasThinks/mistral-7b-english-welsh-translate

**Description:** Specialized English-Welsh translation model (bidirectional).

**Key Details:**
- Trained for English-Welsh translation in any direction
- Focus on government documents
- Uses Markdown formatting
- Uses Alpaca instruction prompt format for best results
- Also available in quantized GGUF format

**Usage:**
For highest quality translations, use the instruction:
"Translate the text from English to Welsh."

---

### 2. facebook/m2m100_418M

**URL:** https://huggingface.co/facebook/m2m100_418M

**Description:** Multilingual encoder-decoder model for many-to-many translation.

**Key Details:**
- Supports 100 languages including Welsh (cy)
- 9,900 translation directions
- Can directly translate between any supported language pair

**Metrics:**
- 418M parameters

---

## Other NLP Models

### 1. techiaith/fullstop-welsh-punctuation-prediction

**URL:** https://huggingface.co/techiaith/fullstop-welsh-punctuation-prediction

**Description:** Punctuation prediction model for Welsh language texts.

**Key Details:**
- Created to restore punctuation of transcribed speech recognition outputs
- Helps improve readability of ASR transcriptions

---

## Datasets

### 1. Mozilla Common Voice (Multiple Versions)

**URLs:**
- mozilla-foundation/common_voice_2_0
- mozilla-foundation/common_voice_3_0
- mozilla-foundation/common_voice_4_0
- mozilla-foundation/common_voice_6_0
- mozilla-foundation/common_voice_7_0
- mozilla-foundation/common_voice_8_0
- mozilla-foundation/common_voice_9_0
- mozilla-foundation/common_voice_10_0
- mozilla-foundation/common_voice_11_0
- mozilla-foundation/common_voice_13_0

**Description:** Welsh is included as one of the supported languages in multiple versions of Mozilla's Common Voice dataset.

**Key Details:**
- Contains MP3 audio files with corresponding text transcriptions
- Includes demographic metadata: age, sex, accent
- Dataset structure includes: path, sentence, accent, age, client_id, up_votes, down_votes, gender, locale, segment
- Welsh language code: "cy"

**Usage:**
```python
from datasets import load_dataset
ds = load_dataset("mozilla-foundation/common_voice_11_0", "cy", use_auth_token=True)
```

**Note:** As of October 2025, Mozilla Common Voice datasets are now exclusively available through Mozilla Data Collective.

---

### 2. techiaith/commonvoice_16_1_en_cy

**URL:** https://huggingface.co/datasets/techiaith/commonvoice_16_1_en_cy

**Description:** Balanced dataset containing 50/50 Welsh and English recordings from CommonVoice.

**Key Details:**
- Version 16.1 of Common Voice
- Bilingual dataset for Welsh-English training

---

### 3. techiaith/commonvoice_18_0_cy_en

**URL:** https://huggingface.co/datasets/techiaith/commonvoice_18_0_cy_en

**Description:** Common Voice version 18 dataset with Welsh and English data.

**Key Details:**
- Used for training Whisper models
- Includes both Welsh and English recordings

---

### 4. OSCAR Corpus (Multiple Versions)

**URLs:**
- oscar (OSCAR 2019)
- oscar-corpus/OSCAR-2109
- oscar-corpus/OSCAR-2201
- oscar-corpus/OSCAR-2301
- oscar-corpus/colossal-oscar-1.0

**Description:** Open Super-large Crawled ALMAnaCH coRpus - multilingual corpus from Common Crawl.

**Key Details:**
- Welsh (cy) is included in all versions
- 166-419 languages depending on version
- Document-level corpus
- Two versions: noisy (document-level LangID only) and clean (with filters)
- Requires gated access and HuggingFace login
- Licensed under ODC-BY with CommonCrawl terms of use
- Primary source for Welsh text corpus data

---

### 5. statmt/cc100

**URL:** https://huggingface.co/datasets/statmt/cc100

**Description:** CC-100 corpus recreating the dataset used for training XLM-R.

**Key Details:**
- Includes Welsh (cy) with 179M tokens
- Monolingual data for 100+ languages
- Mainly intended to pretrain language models and word representations
- Based on Common Crawl data

**Metrics:**
- 179 million tokens of Welsh

---

### 6. allenai/MADLAD-400

**URL:** https://huggingface.co/datasets/allenai/MADLAD-400

**Description:** Multilingual Audited Dataset: Low-resource And Document-level corpus.

**Key Details:**
- Document-level multilingual dataset based on Common Crawl
- Covers 419 languages including Welsh
- Uses all snapshots of CommonCrawl available as of August 1, 2022
- More multilingual, audited, filtered, and document-level compared to similar datasets
- Two versions: noisy (minimal filtering) and clean (with filters)
- Licensed under ODC-BY with CommonCrawl terms of use
- Used to train BangorAI Welsh models

**Metrics:**
- 419 languages total

---

### 7. openai/welsh-texts

**URL:** https://huggingface.co/datasets/openai/welsh-texts

**Description:** Historical Welsh language texts from the National Library of Wales.

**Key Details:**
- Printed and handwritten material from Welsh sources
- Mostly in the Welsh language
- Authorized by National Library of Wales and Welsh Government
- Available for public use including research, scholarship, and machine learning
- Contains variety of historical Welsh documents

---

### 8. Speech Recognition Datasets Collection

**URL:** https://huggingface.co/collections/techiaith/speech-recognition-datasets-672df8ffb3f7da8ed8294ce2

**Description:** Collection of speech recognition datasets curated by techiaith.

**Key Details:**
- Includes 48 hours of transcribed Welsh language spontaneous speech
- Contains Banc Trawsgrifiadau Bangor dataset
- Contains Lleisiau Arfor dataset
- Multiple evaluation datasets

---

## Key Organizations

### 1. techiaith (Language Technologies, Bangor University)

**URL:** https://huggingface.co/techiaith

**Description:** Self-funded research unit developing AI resources for Welsh language, Celtic languages, and multilingual situations.

**Focus Areas:**
- Speech Recognition
- Machine Translation
- Speech-to-Text
- Punctuation Prediction

**Collections Maintained:**
- Speech Recognition Models
- Machine Translation Models
- Speech Recognition Datasets
- Machine Translation Datasets
- Agile Cymru (Cymru-Breizh) Models
- Evaluation Datasets

**Technologies Used:**
- Whisper-based models
- wav2vec2-based models
- Kaldi-based models
- Utility models

---

### 2. BangorAI (Bangor AI)

**URL:** https://huggingface.co/BangorAI

**Description:** Organization focused on Welsh language AI development.

**Models Available:**
- 21 models on HuggingFace
- Focus on bilingual Welsh-English LLMs
- Experimental models for Welsh language generation

**Demo:** https://demo.bangor.ai

---

### 3. Facebook/Meta AI

**Contributions to Welsh:**
- facebook/mms-tts-cym (TTS)
- facebook/m2m100_418M (Translation)
- Pre-trained models: wav2vec2-large-xlsr-53, xls-r-1b (used for fine-tuning)

---

## Collections

### techiaith Collections

1. **Speech Recognition Models**
   - URL: https://huggingface.co/collections/techiaith/speech-recognition-models-660552d87de27e9581013dcf
   - Models for Welsh language and bilingual speech recognition
   - Includes Whisper, wav2vec2, and Kaldi-based models

2. **Speech Recognition Datasets**
   - URL: https://huggingface.co/collections/techiaith/speech-recognition-datasets-672df8ffb3f7da8ed8294ce2
   - Curated datasets for training and evaluation

3. **Machine Translation Models**
   - Collection of Welsh translation models
   - Uses Marian NMT framework
   - Specialized for Health/care and Legislation domains

---

## Resources Not Available

Based on comprehensive research, the following resources were NOT found as dedicated Welsh-specific models on HuggingFace:

### 1. Named Entity Recognition (NER)
- No dedicated Welsh NER model found on HuggingFace
- Welsh NER tools exist outside HuggingFace:
  - Welsh Natural Language Toolkit (WNLT/WNLT2)
  - Bangor University's NER tools (spaCy-based)
  - National Library of Wales' 'Cymrie' tool
- Challenge: English NER tools struggle with Welsh grammatical mutations

### 2. Sentiment Analysis
- No dedicated Welsh sentiment analysis model found on HuggingFace
- Welsh Government's SENTimental tool exists for data collection (not a model)
- Potential alternatives: multilingual sentiment models (not Welsh-specific)

### 3. Word Embeddings
- No dedicated Welsh word embeddings model on HuggingFace main repository
- Research exists: fastText embeddings for Welsh (92M word corpus)
- Recommendation from research: fastText skip-gram on WNLT-tokenized text
- Pre-trained fastText for 157 languages may include Welsh (not confirmed)

### 4. Evaluation Benchmarks
- No Welsh equivalent of GLUE or similar standardized evaluation suite found
- No structured Welsh language evaluation datasets on HuggingFace
- Resources exist but not as comprehensive benchmarks

### 5. Multilingual Model Coverage
- XLM-RoBERTa: 100 languages - Welsh inclusion not explicitly confirmed
- Most multilingual models don't list Welsh explicitly in documentation

---

## Additional Information

### Welsh Language Technology Portal

**URL:** https://techiaith.cymru/?lang=en

**Resources:**
- Information about HuggingFace models: https://techiaith.cymru/resources/hugging-face/?lang=en
- Speech Recognition: https://techiaith.cymru/speech/speech-recognition/?lang=en
- Machine Translation: https://techiaith.cymru/translation/machine-translation/?lang=en
- Data Resources: https://techiaith.cymru/resources/data/?lang=en

---

### GitHub Resources

1. **docker-huggingface-stt-cy**
   - URL: https://github.com/techiaith/docker-huggingface-stt-cy
   - Description: Speech Recognition for Welsh with HuggingFace
   - Docker containers for Welsh ASR

2. **docker-wav2vec2-xlsr-ft-cy**
   - URL: https://zenodo.org/records/5270295
   - Description: Speech recognition for Welsh with fine-tuned wav2vec2 XLSR and KenLM language models

3. **spacy-wales-en-ner-model**
   - URL: https://github.com/techiaith/spacy-wales-en-ner-model
   - Description: English NER model further trained on entities specific to Wales

4. **lecsicon-cymraeg-bangor**
   - URL: https://github.com/techiaith/lecsicon-cymraeg-bangor
   - Description: Comprehensive lexicon of Welsh-language wordforms

---

## Research Papers

1. **Creating Welsh Language Word Embeddings**
   - URL: https://mdpi.com/2076-3417/11/15/6896/htm
   - Description: Research on word2vec and fastText adaptations for Welsh
   - Corpus: 92,963,671 words from 11 sources

2. **Natural language processing for under-resourced languages**
   - Description: Developing a Welsh natural language toolkit
   - Reference: ScienceDirect publication

---

## Key Metrics Summary

| Resource Type | Count | Notes |
|--------------|-------|-------|
| LLMs | 2 | BangorAI Mistral models |
| ASR Models | 7+ | techiaith wav2vec2 and Whisper variants |
| TTS Models | 1 | Facebook MMS |
| Translation Models | 2 | 1 specialized, 1 multilingual |
| Other NLP Models | 1 | Punctuation prediction |
| Datasets | 8+ | Speech, text, and translation data |
| Organizations | 3 | techiaith, BangorAI, Facebook/Meta |
| Collections | 6+ | Various model and dataset collections |

---

## Recommendations

### For Developers:

1. **Speech Recognition:** Use techiaith's Whisper or wav2vec2 models depending on use case:
   - Spontaneous speech: whisper-large-v3-ft-verbatim-cy-en
   - Read speech: wav2vec2-xlsr-ft-cy
   - Offline/mobile: whisper-base-ft-verbatim-cy-en-cpp

2. **Language Generation:** BangorAI/Mistral-7B-Cymraeg-Welsh-v2 for bilingual chat/instruct

3. **Translation:** AndreasThinks/mistral-7b-english-welsh-translate for Welsh-English

4. **TTS:** facebook/mms-tts-cym for text-to-speech

5. **Training Data:**
   - Text: MADLAD-400, OSCAR, CC-100
   - Speech: Common Voice, techiaith datasets

### For Researchers:

1. **Gaps to Fill:**
   - Welsh-specific NER model
   - Welsh sentiment analysis model
   - Welsh evaluation benchmarks
   - Welsh word embedding models on HuggingFace

2. **Data Sources:**
   - Largest Welsh corpus: 92M+ words available
   - 179M tokens in CC-100
   - 48+ hours of transcribed spontaneous speech

---

## Conclusion

Welsh language AI resources on HuggingFace are primarily concentrated in:
- **Speech technologies** (ASR, TTS) - well developed
- **Translation** - moderately available
- **Language models** - emerging (BangorAI efforts)

Main contributors are techiaith (Bangor University) and BangorAI, with support from Facebook/Meta's multilingual initiatives.

**Strengths:**
- Excellent speech recognition resources
- Growing LLM support
- Active development community
- Multiple high-quality datasets

**Gaps:**
- Limited NER resources
- No sentiment analysis models
- Few evaluation benchmarks
- Limited traditional NLP task models

**Overall Assessment:** Welsh is better supported than many minority languages, particularly in speech technologies, thanks to dedicated efforts by Welsh academic institutions and government support.

---

**Report Compiled:** November 17, 2025
**Total Resources Documented:** 25+ models, 8+ datasets, 6+ collections
**Primary Sources:** HuggingFace Hub, techiaith, BangorAI, Welsh Government resources
