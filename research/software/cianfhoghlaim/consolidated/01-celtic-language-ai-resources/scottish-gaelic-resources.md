# Scottish Gaelic AI Resources

## Overview

**ISO Codes:** gd (639-1), gla (639-2/3), Locale: gd-GB
**Speakers:** ~69,700 (2011 census)
**Maturity Level:** Medium - Strong datasets, limited dedicated models

---

## 1. Language Models

### 1.1 GPT-2 WECHSEL Scottish Gaelic

The primary dedicated Scottish Gaelic language model.

| Property | Value |
|----------|-------|
| **Model** | `benjamin/gpt2-wechsel-scottish-gaelic` |
| **Architecture** | GPT-2 with WECHSEL transfer learning |
| **Perplexity** | 16.43 (vs 19.53 from scratch) |
| **Efficiency** | 64x less training effort |
| **License** | MIT |

**HuggingFace:** https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic

**Usage:**
```python
from transformers import AutoModelForCausalLM, AutoTokenizer

tokenizer = AutoTokenizer.from_pretrained("benjamin/gpt2-wechsel-scottish-gaelic")
model = AutoModelForCausalLM.from_pretrained("benjamin/gpt2-wechsel-scottish-gaelic")

input_text = "Tha an latha"
input_ids = tokenizer(input_text, return_tensors="pt").input_ids
outputs = model.generate(input_ids, max_length=50, do_sample=True)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

**References:**
- WECHSEL Paper: https://aclanthology.org/2022.naacl-main.293/
- GitHub: https://github.com/CPJKU/wechsel

### 1.2 XLM-RoBERTa POS Tagging

| Property | Value |
|----------|-------|
| **Model** | `wietsedv/xlm-roberta-base-ft-udpos28-gd` |
| **Task** | Part-of-Speech Tagging |
| **Training** | Universal Dependencies v2.8 |
| **License** | Apache 2.0 |

**HuggingFace:** https://huggingface.co/wietsedv/xlm-roberta-base-ft-udpos28-gd

### 1.3 Multilingual Models with Scottish Gaelic Support

| Model | Parameters | Languages | Scottish Gaelic Support |
|-------|------------|-----------|------------------------|
| **mT5** | Various | 101 | Included via mC4 |
| **M2M100** | 418M/1.2B | 100 | Language code: `gd` |
| **NLLB-200** | 600M-3.3B | 200 | Language code: `gla_Latn` |
| **SMALL-100** | 0.3B | 101 | Included |

---

## 2. Datasets

### 2.1 Text Corpora

| Dataset | Size | Source | URL |
|---------|------|--------|-----|
| **CC-100** | 22M tokens | CommonCrawl | https://huggingface.co/datasets/statmt/cc100 |
| **GlotCC-V1** | 18.8k rows | Web crawl | https://huggingface.co/datasets/cis-lmu/GlotCC-V1 |
| **mC4** | Included | CommonCrawl | https://huggingface.co/datasets/legacy-datasets/mc4 |

**Loading CC-100:**
```python
from datasets import load_dataset
gd_data = load_dataset("statmt/cc100", "gd")
```

### 2.2 Summarization & Parallel Corpora

| Dataset | Content | Size | URL |
|---------|---------|------|-----|
| **XLSum** | BBC articles | 2.31k rows | https://huggingface.co/datasets/csebuetnlp/xlsum |
| **Tatoeba MT** | Translation pairs | Various | https://huggingface.co/datasets/Helsinki-NLP/tatoeba_mt |
| **OPUS-100** | Parallel corpus | Various | https://huggingface.co/datasets/Helsinki-NLP/opus-100 |
| **FLORES-200** | Evaluation | 3001 sentences | https://huggingface.co/datasets/facebook/flores |

**Loading XLSum:**
```python
from datasets import load_dataset
xlsum_gd = load_dataset("csebuetnlp/xlsum", "scottish_gaelic")
```

### 2.3 Linguistic Resources

| Dataset | Content | URL |
|---------|---------|-----|
| **Universal Dependencies** | Treebank annotation | https://huggingface.co/datasets/universal-dependencies/universal_dependencies |
| **ARCOSG** | Annotated Reference Corpus | University of Edinburgh DataShare |
| **Corpas na Gaidhlig** | 30M words | University of Glasgow |

### 2.4 Speech Datasets

| Dataset | Status | Notes |
|---------|--------|-------|
| **Common Voice** | Available via Mozilla Data Collective | Previously on HuggingFace |
| **DASG Audio Archive** | External | Cluas ri Claisneachd |

---

## 3. Translation Models

### 3.1 OPUS-MT Synthetic English-Scottish Gaelic

Best dedicated translation model for Scottish Gaelic.

| Property | Value |
|----------|-------|
| **Model** | `Helsinki-NLP/opus-mt-synthetic-en-gd` |
| **ChrF Score** | 51.10 |
| **COMET Score** | 78.04 |
| **Training** | GPT-4o forward-translated Europarl |
| **License** | CC-BY-4.0 |

**HuggingFace:** https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd

**Usage:**
```python
from transformers import MarianMTModel, MarianTokenizer

model_name = "Helsinki-NLP/opus-mt-synthetic-en-gd"
tokenizer = MarianTokenizer.from_pretrained(model_name)
model = MarianMTModel.from_pretrained(model_name)

text = "Hello, how are you today?"
translated = model.generate(**tokenizer(text, return_tensors="pt"))
print(tokenizer.decode(translated[0], skip_special_tokens=True))
```

### 3.2 Multilingual Translation Models

| Model | Parameters | Downloads | URL |
|-------|------------|-----------|-----|
| **M2M100-418M** | 418M | 849k/month | https://huggingface.co/facebook/m2m100_418M |
| **M2M100-1.2B** | 1.2B | - | https://huggingface.co/facebook/m2m100_1.2B |
| **SMALL-100** | 0.3B | 6.5k/month | https://huggingface.co/alirezamsh/small100 |
| **NLLB-200-3.3B** | 3.3B | - | https://huggingface.co/facebook/nllb-200-3.3B |

**Using M2M100:**
```python
from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer

model = M2M100ForConditionalGeneration.from_pretrained("facebook/m2m100_418M")
tokenizer = M2M100Tokenizer.from_pretrained("facebook/m2m100_418M")

tokenizer.src_lang = "en"
encoded = tokenizer("Hello, this is a test.", return_tensors="pt")
generated_tokens = model.generate(
    **encoded,
    forced_bos_token_id=tokenizer.get_lang_id("gd")
)
print(tokenizer.decode(generated_tokens[0], skip_special_tokens=True))
```

---

## 4. Speech Recognition (ASR)

### 4.1 Current Status

**No publicly available dedicated ASR models found on HuggingFace.**

### 4.2 Research Progress

| Achievement | Performance | Source |
|------------|-------------|--------|
| **Best WER (2025)** | 12.8% | Interspeech 2025 paper |
| **Whisper-Turbo fine-tuned** | 19.0% WER | Research (unpublished) |
| **Historical Kaldi** | 26.30% WER | University of Edinburgh |

### 4.3 Upcoming Development

| Initiative | Timeline | Details |
|-----------|----------|---------|
| **Scottish Government Funded** | Q4 2025 | Speech-to-text API |
| **University of Edinburgh** | 2025 | £225,000 funding for LLM development |

**Data Sources for Future Development:**
- 30 million words from Corpas na Gaidhlig
- DASG's Cluas ri Claisneachd audio archive

### 4.4 Fine-Tuning Base Models

For ASR development, consider fine-tuning:

| Model | URL | Notes |
|-------|-----|-------|
| **Whisper Large-v3** | https://huggingface.co/openai/whisper-large-v3 | Best baseline |
| **Wav2Vec2-XLSR-53** | https://huggingface.co/facebook/wav2vec2-large-xlsr-53 | Cross-lingual |
| **MMS-1B** | https://huggingface.co/facebook/mms-1b-all | 1162 languages |

---

## 5. Key Organizations

| Organization | Focus | Resources |
|--------------|-------|-----------|
| **EdinburghNLP** | ASR, translation research | https://huggingface.co/EdinburghNLP |
| **Helsinki-NLP** | Translation models | OPUS-MT project |
| **University of Edinburgh** | LLM development | £225k government funding |
| **National Library of Scotland** | Historical documents | https://huggingface.co/NationalLibraryOfScotland |

---

## 6. External Resources

### 6.1 Non-HuggingFace Corpora

| Resource | Content | Access |
|----------|---------|--------|
| **ARCOSG** | Annotated Reference Corpus | Edinburgh DataShare |
| **Corpas na Gaidhlig** | 30M words | University of Glasgow |
| **DASG** | Digital Archive | https://dasg.ac.uk/ |
| **Scottish Gaelic Wikipedia** | General text | Wikipedia dump |
| **Sketch Engine** | Text corpora | Subscription required |

### 6.2 Browse Resources

- **Models:** https://huggingface.co/models?language=gd
- **Datasets:** https://huggingface.co/datasets?language=language:gla

---

## 7. Research Gaps & Opportunities

| Gap | Status | Priority |
|-----|--------|----------|
| **Fine-tuned ASR models** | In development | High |
| **TTS models** | Not available | High |
| **Dedicated BERT model** | Not available | Medium |
| **NER datasets** | Limited | Medium |
| **Question answering** | Not available | Medium |

---

## 8. Integration Examples

### 8.1 With dlt Pipeline

```python
import dlt
from transformers import pipeline

# Scottish Gaelic translation pipeline
translator = pipeline("translation", model="Helsinki-NLP/opus-mt-synthetic-en-gd")

@dlt.resource
def translate_to_scottish_gaelic(texts: list[str]):
    for text in texts:
        translation = translator(text)[0]['translation_text']
        yield {
            "original": text,
            "scottish_gaelic": translation
        }
```

### 8.2 Summarization with XLSum

```python
from datasets import load_dataset
from transformers import pipeline

# Load Scottish Gaelic summarization data
xlsum_gd = load_dataset("csebuetnlp/xlsum", "scottish_gaelic", split="train")

# Use for training or evaluation
for article in xlsum_gd:
    print(f"Title: {article['title']}")
    print(f"Summary: {article['summary']}")
    print(f"URL: {article['url']}")
```

---

## References

- WECHSEL Paper: https://aclanthology.org/2022.naacl-main.293/
- Scottish Gaelic ASR Guide (2025): https://arxiv.org/abs/2506.04915
- OPUS-MT Synthetic Paper: ArXiv 2505.14423
- University of Edinburgh Initiative: https://www.ed.ac.uk/news/2023/ai-initiative-gives-gaelic-a-foothold-in-the-digit
