# Welsh (Cymraeg) AI Resources

## Overview

**ISO Codes:** cy (639-1), cym (639-2/3), Locale: cy-GB
**Speakers:** ~884,300 (2021 census)
**Maturity Level:** High - Strong ASR ecosystem, active LLM development

---

## 1. Language Models

### 1.1 Mistral-7B-Cymraeg-Welsh-v2

The primary Welsh LLM, bilingual Welsh-English.

| Property | Value |
|----------|-------|
| **Model** | `BangorAI/Mistral-7B-Cymraeg-Welsh-v2` |
| **Parameters** | 7B |
| **Base** | Mistral-7B-v0.1 |
| **Training** | MADLAD-400 dataset, 2 epochs |
| **Fine-tuning** | yahma/alpaca-cleaned (Welsh + English) |

**HuggingFace:** https://huggingface.co/BangorAI/Mistral-7B-Cymraeg-Welsh-v2

**Demo:** https://demo.bangor.ai

**Usage:**
```python
from transformers import AutoModelForCausalLM, AutoTokenizer

model = AutoModelForCausalLM.from_pretrained("BangorAI/Mistral-7B-Cymraeg-Welsh-v2")
tokenizer = AutoTokenizer.from_pretrained("BangorAI/Mistral-7B-Cymraeg-Welsh-v2")

# Welsh system prompt for Welsh responses
system_prompt = "Rydych chi'n gynorthwyydd AI sy'n siarad Cymraeg."
```

### 1.2 Base Model

| Model | Purpose | URL |
|-------|---------|-----|
| **mistral-7b-cy-epoch-2** | Pre-training base | https://huggingface.co/BangorAI/mistral-7b-cy-epoch-2 |

---

## 2. Speech Recognition (ASR)

Welsh has the most comprehensive ASR ecosystem among Celtic languages, primarily from techiaith (Bangor University).

### 2.1 wav2vec2 Models

#### Primary Model: wav2vec2-xlsr-ft-cy

| Property | Value |
|----------|-------|
| **Model** | `techiaith/wav2vec2-xlsr-ft-cy` |
| **WER** | 6.04% (4.05% with KenLM) |
| **Base** | Facebook XLSR-53 |
| **Training** | Common Voice + OSCAR |

**HuggingFace:** https://huggingface.co/techiaith/wav2vec2-xlsr-ft-cy

**Usage:**
```python
from transformers import Wav2Vec2Processor, Wav2Vec2ForCTC
import torch

processor = Wav2Vec2Processor.from_pretrained("techiaith/wav2vec2-xlsr-ft-cy")
model = Wav2Vec2ForCTC.from_pretrained("techiaith/wav2vec2-xlsr-ft-cy")

# Process audio
inputs = processor(audio_array, sampling_rate=16000, return_tensors="pt")
with torch.no_grad():
    logits = model(**inputs).logits
predicted_ids = torch.argmax(logits, dim=-1)
transcription = processor.decode(predicted_ids[0])
```

#### Other wav2vec2 Models

| Model | Training | URL |
|-------|----------|-----|
| **wav2vec2-base-cy** | 4000 hours (25% Welsh) | https://huggingface.co/techiaith/wav2vec2-base-cy |
| **wav2vec2-xlsr-53-ft-cy-en** | Bilingual Welsh-English | https://huggingface.co/techiaith/wav2vec2-xlsr-53-ft-cy-en |

### 2.2 Whisper Models

| Model | Use Case | WER | URL |
|-------|----------|-----|-----|
| **whisper-large-v3-ft-verbatim-cy-en** | Spontaneous speech | 28.99% | https://huggingface.co/techiaith/whisper-large-v3-ft-verbatim-cy-en |
| **whisper-large-v3-ft-commonvoice-cy-en** | Read speech | - | https://huggingface.co/techiaith/whisper-large-v3-ft-commonvoice-cy-en |
| **whisper-base-ft-verbatim-cy-en-cpp** | Offline/mobile | - | https://huggingface.co/techiaith/whisper-base-ft-verbatim-cy-en-cpp |
| **whisper-large-v3-ft-verbatim-cy-en-ct2** | CTranslate2 optimized | - | https://huggingface.co/techiaith/whisper-large-v3-ft-verbatim-cy-en-ct2 |

### 2.3 Model Selection Guide

| Use Case | Recommended Model |
|----------|------------------|
| **Read/planned speech** | wav2vec2-xlsr-ft-cy |
| **Spontaneous speech** | whisper-large-v3-ft-verbatim-cy-en |
| **Offline/mobile** | whisper-base-ft-verbatim-cy-en-cpp |
| **Fast inference** | whisper-large-v3-ft-verbatim-cy-en-ct2 |
| **Bilingual audio** | wav2vec2-xlsr-53-ft-cy-en |

---

## 3. Text-to-Speech (TTS)

### 3.1 Facebook MMS-TTS Welsh

| Property | Value |
|----------|-------|
| **Model** | `facebook/mms-tts-cym` |
| **Architecture** | VITS |
| **Languages** | Part of 1107+ language coverage |

**HuggingFace:** https://huggingface.co/facebook/mms-tts-cym

**Usage:**
```python
from transformers import VitsModel, AutoTokenizer
import torch

model = VitsModel.from_pretrained("facebook/mms-tts-cym")
tokenizer = AutoTokenizer.from_pretrained("facebook/mms-tts-cym")

text = "Bore da, sut ydych chi heddiw?"
inputs = tokenizer(text, return_tensors="pt")

with torch.no_grad():
    output = model(**inputs).waveform

# output contains the synthesized audio waveform
```

---

## 4. Translation Models

### 4.1 Dedicated Welsh Translation

| Property | Value |
|----------|-------|
| **Model** | `AndreasThinks/mistral-7b-english-welsh-translate` |
| **Type** | Bidirectional English-Welsh |
| **Specialization** | Government documents |
| **Format** | Also available in GGUF |

**HuggingFace:** https://huggingface.co/AndreasThinks/mistral-7b-english-welsh-translate

**Usage:**
```python
# Use Alpaca instruction format
instruction = "Translate the text from English to Welsh."
input_text = "The meeting will take place tomorrow."
```

### 4.2 Multilingual Translation

| Model | Welsh Support | URL |
|-------|---------------|-----|
| **M2M100-418M** | Language code: `cy` | https://huggingface.co/facebook/m2m100_418M |
| **Helsinki-NLP OPUS-MT** | en-cy, cy-en pairs | https://huggingface.co/Helsinki-NLP |

---

## 5. Other NLP Models

### 5.1 Punctuation Prediction

| Property | Value |
|----------|-------|
| **Model** | `techiaith/fullstop-welsh-punctuation-prediction` |
| **Purpose** | Restore punctuation in ASR output |

**HuggingFace:** https://huggingface.co/techiaith/fullstop-welsh-punctuation-prediction

---

## 6. Datasets

### 6.1 Text Corpora

| Dataset | Size | Source | URL |
|---------|------|--------|-----|
| **CC-100** | 179M tokens | CommonCrawl | https://huggingface.co/datasets/statmt/cc100 |
| **OSCAR** | Multi-version | CommonCrawl | https://huggingface.co/datasets/oscar-corpus/OSCAR-2301 |
| **MADLAD-400** | 419 languages | CommonCrawl | https://huggingface.co/datasets/allenai/MADLAD-400 |
| **Welsh Texts** | Historical | National Library of Wales | https://huggingface.co/datasets/openai/welsh-texts |

**Loading CC-100 Welsh:**
```python
from datasets import load_dataset
welsh_data = load_dataset("statmt/cc100", "cy")
```

### 6.2 Speech Datasets

| Dataset | Content | URL |
|---------|---------|-----|
| **Common Voice (techiaith)** | 50/50 Welsh-English | https://huggingface.co/datasets/techiaith/commonvoice_16_1_en_cy |
| **CommonVoice 18** | Welsh + English | https://huggingface.co/datasets/techiaith/commonvoice_18_0_cy_en |
| **Banc Trawsgrifiadau Bangor** | 48+ hours spontaneous speech | techiaith collection |
| **Lleisiau Arfor** | Spontaneous speech | techiaith collection |

### 6.3 Collections

| Collection | Content | URL |
|------------|---------|-----|
| **Speech Recognition Datasets** | Training + evaluation | https://huggingface.co/collections/techiaith/speech-recognition-datasets-672df8ffb3f7da8ed8294ce2 |
| **Speech Recognition Models** | All ASR models | https://huggingface.co/collections/techiaith/speech-recognition-models-660552d87de27e9581013dcf |

---

## 7. Key Organizations

### 7.1 techiaith (Bangor University)

**URL:** https://huggingface.co/techiaith

Primary Welsh AI resource developer - self-funded research unit.

**Focus Areas:**
- Speech Recognition
- Machine Translation
- Speech-to-Text
- Punctuation Prediction

**Portal:** https://techiaith.cymru/?lang=en

### 7.2 BangorAI

**URL:** https://huggingface.co/BangorAI

**Focus:** Welsh LLMs and bilingual models

**Models:** 21 models on HuggingFace

**Demo:** https://demo.bangor.ai

---

## 8. External Resources

### 8.1 GitHub Repositories

| Repository | Purpose | URL |
|------------|---------|-----|
| **docker-huggingface-stt-cy** | Welsh ASR Docker | https://github.com/techiaith/docker-huggingface-stt-cy |
| **spacy-wales-en-ner-model** | Wales-specific NER | https://github.com/techiaith/spacy-wales-en-ner-model |
| **lecsicon-cymraeg-bangor** | Welsh lexicon | https://github.com/techiaith/lecsicon-cymraeg-bangor |

### 8.2 Welsh Government Resources

| Resource | Purpose |
|----------|---------|
| **Cymraeg 2050** | Language strategy |
| **SENTimental** | Sentiment data collection tool |

---

## 9. Research Gaps & Opportunities

| Gap | Status | Priority |
|-----|--------|----------|
| **Named Entity Recognition** | External tools only | High |
| **Sentiment Analysis** | No dedicated model | High |
| **Word Embeddings** | Research exists, not on HF | Medium |
| **Evaluation Benchmarks** | No Welsh GLUE equivalent | Medium |

### 9.1 NER Alternatives

Welsh NER tools exist outside HuggingFace:
- Welsh Natural Language Toolkit (WNLT/WNLT2)
- Bangor University's NER tools (spaCy-based)
- National Library of Wales' 'Cymrie' tool

---

## 10. Integration Examples

### 10.1 Complete ASR Pipeline

```python
from transformers import Wav2Vec2Processor, Wav2Vec2ForCTC
import torch
import librosa

# Load model
processor = Wav2Vec2Processor.from_pretrained("techiaith/wav2vec2-xlsr-ft-cy")
model = Wav2Vec2ForCTC.from_pretrained("techiaith/wav2vec2-xlsr-ft-cy")

# Load and process audio
audio, sr = librosa.load("welsh_audio.wav", sr=16000)
inputs = processor(audio, sampling_rate=16000, return_tensors="pt")

# Transcribe
with torch.no_grad():
    logits = model(**inputs).logits
predicted_ids = torch.argmax(logits, dim=-1)
transcription = processor.decode(predicted_ids[0])

print(f"Transcription: {transcription}")
```

### 10.2 TTS Generation

```python
from transformers import VitsModel, AutoTokenizer
import soundfile as sf

model = VitsModel.from_pretrained("facebook/mms-tts-cym")
tokenizer = AutoTokenizer.from_pretrained("facebook/mms-tts-cym")

text = "Croeso i Gymru. Mae'n dda gen i gwrdd a chi."
inputs = tokenizer(text, return_tensors="pt")

with torch.no_grad():
    output = model(**inputs).waveform

# Save audio
sf.write("output.wav", output.squeeze().numpy(), 16000)
```

### 10.3 With dlt Pipeline

```python
import dlt
from transformers import pipeline

# Welsh speech-to-text pipeline
asr = pipeline("automatic-speech-recognition", model="techiaith/wav2vec2-xlsr-ft-cy")

@dlt.resource
def transcribe_welsh_audio(audio_paths: list[str]):
    for path in audio_paths:
        result = asr(path)
        yield {
            "audio_path": path,
            "transcription": result["text"]
        }
```

---

## References

- techiaith Portal: https://techiaith.cymru/?lang=en
- BangorAI Demo: https://demo.bangor.ai
- Welsh Word Embeddings Research: https://mdpi.com/2076-3417/11/15/6896/htm
- MMS Language Coverage: https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html
