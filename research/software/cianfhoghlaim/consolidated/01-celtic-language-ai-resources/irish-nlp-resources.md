# Irish (Gaeilge) Language AI Resources

## Overview

**ISO Codes:** ga (639-1), gle (639-2/3), Locale: ga-IE
**Speakers:** ~1.85 million (2022 census)
**Maturity Level:** High - Most developed Celtic language AI ecosystem

---

## 1. Language Models

### 1.1 UCCIX - Irish-eXcellence LLM (2024)

The first and most advanced open-source Irish LLM.

| Property | Value |
|----------|-------|
| **Base Model** | Llama 2-13B / Llama 3.1-70B |
| **Irish Tokens** | ~520M |
| **Performance** | Up to 12% improvement over larger models |

**HuggingFace Models:**
- **Pre-trained:** https://huggingface.co/ReliableAI/UCCIX-Llama2-13B
- **Instruction-tuned:** https://huggingface.co/ReliableAI/UCCIX-Llama2-13B-Instruct
- **Llama 3.1 70B:** https://huggingface.co/ReliableAI/UCCIX-Llama3.1-70B-Instruct-19122024

**Resources:**
- Live Demo: https://aine.chat
- Paper: https://arxiv.org/abs/2405.13010
- GitHub: https://github.com/ReML-AI/UCCIX

### 1.2 gaBERT - Irish BERT Model

Best performing encoder model for Irish NLP tasks.

| Property | Value |
|----------|-------|
| **Training Data** | 7.9M Irish sentences |
| **Architecture** | BERT-base (cased) |
| **Organization** | DCU-NLP |

**HuggingFace:** https://huggingface.co/DCU-NLP/bert-base-irish-cased-v1

**Usage:**
```python
from transformers import AutoModel, AutoTokenizer

tokenizer = AutoTokenizer.from_pretrained("DCU-NLP/bert-base-irish-cased-v1")
model = AutoModel.from_pretrained("DCU-NLP/bert-base-irish-cased-v1")
```

### 1.3 gaELECTRA

| Property | Value |
|----------|-------|
| **Training Data** | 7.9M Irish sentences |
| **Architecture** | ELECTRA-base |

**HuggingFace:** https://huggingface.co/DCU-NLP/electra-base-irish-cased-generator-v1

### 1.4 BERTreach - Irish RoBERTa

| Property | Value |
|----------|-------|
| **Training Data** | 47M tokens |
| **Architecture** | RoBERTa |
| **License** | Apache-2.0 |

**HuggingFace:** https://huggingface.co/jimregan/BERTreach

---

## 2. Datasets

### 2.1 Text Corpora

| Dataset | Size | Source | URL |
|---------|------|--------|-----|
| **CC-100** | 108M tokens | CommonCrawl | https://huggingface.co/datasets/statmt/cc100 |
| **OSCAR** | Multi-version | CommonCrawl | https://huggingface.co/datasets/oscar-corpus/OSCAR-2301 |
| **CulturaX** | 6.3T tokens (167 langs) | Mixed | https://huggingface.co/datasets/uonlp/CulturaX |
| **Irish-English Parallel** | Parallel corpus | UCCIX project | https://huggingface.co/datasets/ReliableAI/Irish-English-Parallel-Collection |

**Loading CC-100 Irish:**
```python
from datasets import load_dataset
irish_data = load_dataset("statmt/cc100", "ga")
```

### 2.2 Speech Datasets

| Dataset | Content | URL |
|---------|---------|-----|
| **Common Voice** | Crowdsourced speech + transcriptions | Multiple versions (9.0-19.0) |
| **Tatoeba-Speech-Irish** | Synthetic audio (2h 39m) | https://huggingface.co/datasets/ymoslem/Tatoeba-Speech-Irish |
| **XTREME-S** | Multilingual speech benchmark | https://huggingface.co/datasets/google/xtreme_s |

**Loading Common Voice:**
```python
from datasets import load_dataset
cv = load_dataset("mozilla-foundation/common_voice_13_0", "ga")
```

### 2.3 Benchmarks

| Benchmark | Type | Availability |
|-----------|------|--------------|
| **IrishQA** | Question Answering | GitHub (UCCIX repo) |
| **Irish MT-bench** | LLM Evaluation | GitHub (UCCIX repo) |

---

## 3. Translation Models

### 3.1 Helsinki-NLP OPUS-MT

| Direction | URL | License |
|-----------|-----|---------|
| **English → Irish** | https://huggingface.co/Helsinki-NLP/opus-mt-en-ga | CC-BY 4.0 |
| **Irish → English** | https://huggingface.co/Helsinki-NLP/opus-mt-ga-en | CC-BY 4.0 |

**Usage:**
```python
from transformers import MarianMTModel, MarianTokenizer

model_name = "Helsinki-NLP/opus-mt-en-ga"
tokenizer = MarianTokenizer.from_pretrained(model_name)
model = MarianMTModel.from_pretrained(model_name)

text = "Hello, how are you?"
translated = model.generate(**tokenizer(text, return_tensors="pt"))
print(tokenizer.decode(translated[0], skip_special_tokens=True))
```

### 3.2 Multilingual Translation

| Model | Parameters | Languages | URL |
|-------|------------|-----------|-----|
| **M2M100** | 418M / 1.2B | 100 (9,900 pairs) | https://huggingface.co/facebook/m2m100_418M |
| **SMaLL-100** | 0.3B | 10K+ pairs | https://huggingface.co/alirezamsh/small100 |

---

## 4. Speech Recognition (ASR)

### 4.1 Wav2Vec2 Models

| Model | Base | Training Data | URL |
|-------|------|---------------|-----|
| **wav2vec2-large-xlsr-53-irish** | XLSR-53 | Common Voice | https://huggingface.co/cpierse/wav2vec2-large-xlsr-53-irish |
| **wav2vec2-large-xls-r-1b-ga-ie** | XLS-R 1B | CV 8.0 + Living Irish | https://huggingface.co/Aditya3107/wav2vec2-large-xls-r-1b-ga-ie |
| **wav2vec2-large-xls-r-1b-Irish** | XLS-R 1B | Common Voice | https://huggingface.co/kingabzpro/wav2vec2-large-xls-r-1b-Irish |
| **wav2vec2-large-xlsr-irish-basic** | XLSR | Common Voice | https://huggingface.co/jimregan/wav2vec2-large-xlsr-irish-basic |

**Usage:**
```python
from transformers import Wav2Vec2Processor, Wav2Vec2ForCTC

processor = Wav2Vec2Processor.from_pretrained("cpierse/wav2vec2-large-xlsr-53-irish")
model = Wav2Vec2ForCTC.from_pretrained("cpierse/wav2vec2-large-xlsr-53-irish")
```

### 4.2 Facebook MMS (Massively Multilingual Speech)

| Model | Languages | Irish Code | URL |
|-------|-----------|------------|-----|
| **mms-1b-all** | 1162 | ga/gle | https://huggingface.co/facebook/mms-1b-all |
| **mms-1b-l1107** | 1107 | ga/gle | https://huggingface.co/facebook/mms-1b-l1107 |
| **mms-1b-fl102** | 102 | ga/gle | https://huggingface.co/facebook/mms-1b-fl102 |

**Usage:**
```python
from transformers import Wav2Vec2ForCTC, AutoProcessor

processor = AutoProcessor.from_pretrained("facebook/mms-1b-all")
model = Wav2Vec2ForCTC.from_pretrained("facebook/mms-1b-all", target_lang="gle", ignore_mismatched_sizes=True)
```

---

## 5. Text-to-Speech (TTS)

### 5.1 Facebook MMS-TTS

| Property | Value |
|----------|-------|
| **Languages** | 1107+ |
| **Architecture** | VITS |
| **Irish Model** | facebook/mms-tts-gle |

**HuggingFace:** https://huggingface.co/facebook/mms-tts

**Language Coverage:** https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html

---

## 6. Other NLP Resources

### 6.1 Multilingual Models Supporting Irish

| Model | Languages | URL |
|-------|-----------|-----|
| **XLM-RoBERTa** | 100 | https://huggingface.co/FacebookAI/xlm-roberta-base |
| **LaBSE** | 109 | https://huggingface.co/setu4993/LaBSE |

### 6.2 NER Models (jimregan)

- `jimregan/bert-base-irish-cased-v1-finetuned-ner`
- `jimregan/electra-base-irish-cased-discriminator-v1-finetuned-ner`

### 6.3 Collections

- **Irish-English Speech Translation:** https://huggingface.co/collections/ymoslem/irish-english-speech-translation-datasets-665dd9e8fbaa279db3474ca0

---

## 7. Research Gaps & Opportunities

| Gap | Status | Opportunity |
|-----|--------|-------------|
| **Whisper fine-tuning** | Not found | High-impact contribution |
| **NER datasets** | Limited | Create annotated corpus |
| **Sentiment analysis** | Not found | Build dataset from social media |
| **IrishQA on HuggingFace** | GitHub only | Upload to Hub |

---

## 8. Integration Examples

### 8.1 With dlt Pipeline

```python
import dlt
from transformers import pipeline

# Irish-English translation pipeline
translator = pipeline("translation", model="Helsinki-NLP/opus-mt-ga-en")

@dlt.resource
def translate_irish_documents(docs: list[str]):
    for doc in docs:
        translation = translator(doc)[0]['translation_text']
        yield {
            "original": doc,
            "translated": translation
        }
```

### 8.2 With BAML Schema

```baml
function TranslateToIrish(text: string) -> string {
  client OpenAI
  prompt #"
    Translate the following English text to Irish (Gaeilge).
    Use modern standard Irish (An Caighdeán Oifigiúil).

    Text: {{ text }}

    Irish translation:
  "#
}
```

---

## References

- UCCIX Paper: https://arxiv.org/abs/2405.13010
- gaBERT Paper: https://arxiv.org/abs/2107.12930
- Irish-BERT GitHub: https://github.com/jbrry/Irish-BERT
- MMS Language Coverage: https://dl.fbaipublicfiles.com/mms/misc/language_coverage_mms.html
