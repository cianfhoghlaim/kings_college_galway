# Scottish Gaelic AI Resources on HuggingFace
**Comprehensive Research Report**
*Generated: 2025-11-17*

---

## 1. LANGUAGE MODELS FOR SCOTTISH GAELIC

### 1.1 GPT-2 WECHSEL Scottish Gaelic
- **URL**: https://huggingface.co/benjamin/gpt2-wechsel-scottish-gaelic
- **Model Name**: `benjamin/gpt2-wechsel-scottish-gaelic`
- **Type**: Text Generation (Causal Language Model)
- **Architecture**: GPT-2 with WECHSEL transfer learning method
- **Description**: A GPT-2 model adapted for Scottish Gaelic using the WECHSEL (Effective initialization of subword embeddings for cross-lingual transfer) method. The English tokenizer was replaced with a Scottish Gaelic tokenizer, and embeddings were initialized using multilingual word embeddings.
- **Key Metrics**:
  - Perplexity: 16.43 (outperforms GPT-2 trained from scratch: 19.53)
  - Training efficiency: 64x less training effort than training from scratch
  - Downloads: 1 download last month
  - License: MIT
  - Framework: PyTorch + Transformers
- **Research**: Based on WECHSEL paper (Minixhofer et al., NAACL 2022)
- **Code**: https://github.com/CPJKU/wechsel

### 1.2 XLM-RoBERTa Base for Scottish Gaelic POS Tagging
- **URL**: https://huggingface.co/wietsedv/xlm-roberta-base-ft-udpos28-gd
- **Model Name**: `wietsedv/xlm-roberta-base-ft-udpos28-gd`
- **Type**: Token Classification (Part-of-Speech Tagging)
- **Architecture**: XLM-RoBERTa base fine-tuned on Universal Dependencies v2.8
- **Description**: Fine-tuned model for part-of-speech tagging in Scottish Gaelic using Universal Dependencies standards. Part of research examining cross-lingual transfer across 105+ languages.
- **Key Metrics**:
  - Training Data: Universal Dependencies v2.8 dataset
  - Updated: January 18, 2024
  - License: Apache 2.0
  - Community: 1 discussion thread
  - Framework: PyTorch + Transformers

### 1.3 Multilingual Models with Scottish Gaelic Support

#### mT5 (Multilingual T5)
- **URL**: https://huggingface.co/google/mt5-base, https://huggingface.co/google/mt5-large
- **Model Names**: `google/mt5-base`, `google/mt5-large`
- **Description**: Multilingual variant of T5 pre-trained on mC4 dataset covering 101 languages including Scottish Gaelic.
- **Note**: Requires fine-tuning before use on downstream tasks (cannot do translation out of the box)
- **Framework**: PyTorch + Transformers

---

## 2. DATASETS FOR SCOTTISH GAELIC

### 2.1 Text Corpora

#### CC-100 (Common Crawl 100)
- **URL**: https://huggingface.co/datasets/statmt/cc100
- **Dataset Name**: `statmt/cc100`
- **Description**: Monolingual dataset reconstructed from Common Crawl snapshots (January-December 2018). Contains web-crawled text for 100+ languages.
- **Scottish Gaelic Data**:
  - Language code: `gd`
  - Size: 22 million tokens
  - Source: Common Crawl web data processed through CC-Net toolkit
- **Format**: Documents separated by double newlines, paragraphs by single newlines
- **Use Case**: Pre-training language models (used for training XLM-R)

#### GlotCC-V1
- **URL**: https://huggingface.co/datasets/cis-lmu/GlotCC-V1
- **Dataset Name**: `cis-lmu/GlotCC-V1`
- **Description**: Large-scale multilingual web-crawled text corpus with quality indicators
- **Scottish Gaelic Data**:
  - Language code: `gla-Latn`
  - Size: 18.8k rows
  - Features: Language identification scores, script analysis, quality metrics
- **Source**: Web-crawled content with extensive filtering

#### mC4 (Multilingual C4)
- **URL**: https://huggingface.co/datasets/legacy-datasets/mc4
- **Dataset Name**: `legacy-datasets/mc4`
- **Description**: Multilingual variant of the Colossal Clean Crawled Corpus (C4)
- **Scottish Gaelic**: Listed among supported languages (language code: `gd`)
- **Note**: Used for pre-training mT5 models

### 2.2 Parallel Translation Datasets

#### XLSum Dataset
- **URL**: https://huggingface.co/datasets/csebuetnlp/xlsum
- **Dataset Name**: `csebuetnlp/xlsum`
- **Description**: Comprehensive dataset comprising 1.35 million professionally annotated article-summary pairs from BBC, covering 45 languages.
- **Scottish Gaelic Data**:
  - Configuration: `scottish_gaelic`
  - Size: 2.31k rows (evaluation set increased to 500 samples)
  - Splits: train, test, validation
  - Fields: split, id, url, title, summary, text
  - Source: BBC articles
  - Format: Parquet
- **Use Case**: Cross-lingual summarization tasks

#### Helsinki-NLP Tatoeba MT
- **URL**: https://huggingface.co/datasets/Helsinki-NLP/tatoeba_mt
- **Dataset Name**: `Helsinki-NLP/tatoeba_mt`
- **Description**: Multilingual translation benchmarks derived from user-contributed translations from Tatoeba.org. Covers hundreds of languages including Scottish Gaelic.
- **Scottish Gaelic**: Included in language inventory
- **Source**: Tatoeba.org community translations, compiled from OPUS
- **Use Case**: Machine translation evaluation and training

#### OPUS-100
- **URL**: https://huggingface.co/datasets/Helsinki-NLP/opus-100
- **Dataset Name**: `Helsinki-NLP/opus-100`
- **Description**: English-centric multilingual corpus covering 100 languages from OPUS
- **Scottish Gaelic**: May include Scottish Gaelic-English pairs
- **Source**: OPUS parallel corpus collection

#### FLORES-200
- **URL**: https://huggingface.co/datasets/facebook/flores
- **Dataset Name**: `facebook/flores`
- **Description**: Multilingual evaluation benchmark with parallel sentences across 200 languages.
- **Scottish Gaelic**:
  - Confirmed as included
  - Language code: likely `gla_Latn` or `gd_Latn`
  - Size: 3001 sentences total across all languages
  - Splits: dev (997), devtest (1012)
- **Use Case**: Translation model evaluation

#### FLORES+ (Enhanced)
- **URL**: https://huggingface.co/datasets/openlanguagedata/flores_plus
- **Dataset Name**: `openlanguagedata/flores_plus`
- **Description**: Enhanced version of FLORES dataset with synthetic data
- **Note**: Used for evaluating the OPUS-MT synthetic-en-gd model

### 2.3 Speech Datasets

#### Common Voice (Mozilla)
- **URL**: https://huggingface.co/datasets/mozilla-foundation/common_voice_13_0
- **Dataset Names**: `mozilla-foundation/common_voice_X_0` (versions 4.0, 9.0, 12.0, 13.0)
- **Description**: Multilingual speech recognition dataset with crowdsourced voice recordings
- **Scottish Gaelic**:
  - Language codes: `gd` or `gla`
  - Format: MP3 files with text transcriptions
  - Metadata: Age, sex, accent information
- **Important Note**: As of October 2025, Common Voice datasets are now exclusively available through Mozilla Data Collective at datacollective.mozillafoundation.org
- **Legacy Status**: Original HuggingFace datasets are deprecated

### 2.4 Linguistic Annotation Datasets

#### Universal Dependencies
- **URL**: https://huggingface.co/datasets/universal-dependencies/universal_dependencies
- **Dataset Name**: `universal-dependencies/universal_dependencies`
- **Description**: Cross-linguistically consistent treebank annotation for morphology and syntax
- **Scottish Gaelic Data**:
  - Language code: `gd`
  - Annotations: UPOS (universal POS tags), XPOS (language-specific POS), Feats (morphological features), Lemmas, dependency labels
- **Requirement**: Install conllu dependency (`pip install conllu`)
- **Use Case**: Training POS taggers, parsers, and syntactic analyzers

### 2.5 Complete Dataset List

According to HuggingFace's filter for Scottish Gaelic (language code: `gla`), there are **38 total datasets** available. Key additional datasets include:

- HuggingFaceFW/finepdfs
- HuggingFaceFW/fineweb-2
- CohereLabs/aya_collection_language_split
- mteb/tatoeba-bitext-mining
- Muennighoff/flores200
- cis-lmu/udhr-lid
- cis-lmu/Glot500
- pykeio/librivox-tracks
- lbourdois/language_tags
- lbourdois/panlex

**Browse all**: https://huggingface.co/datasets?language=language:gla

---

## 3. TRANSLATION MODELS INVOLVING SCOTTISH GAELIC

### 3.1 Dedicated Scottish Gaelic Translation Models

#### OPUS-MT Synthetic English-Scottish Gaelic
- **URL**: https://huggingface.co/Helsinki-NLP/opus-mt-synthetic-en-gd
- **Model Name**: `Helsinki-NLP/opus-mt-synthetic-en-gd`
- **Type**: Neural Machine Translation (English ↔ Scottish Gaelic)
- **Architecture**: Transformer-base (Marian MT)
- **Description**: Synthetic baseline model developed to supplement traditional datasets with high-quality LLM-generated translations for low-resource language pairs. Training data created by forward-translating English Europarl using GPT-4o.
- **Key Metrics**:
  - ChrF Score: 51.10
  - COMET Score: 78.04
  - Downloads: 93 last month
  - Evaluation: FLORES+ benchmark
  - License: CC-BY-4.0
  - Framework: PyTorch + MarianMT
- **Training Dataset**: openlanguagedata/flores_plus (synthetically generated)
- **Research**: "Scaling Low-Resource MT via Synthetic Data Generation with LLMs" (EMNLP 2025)
- **ArXiv**: 2505.14423

### 3.2 Multilingual Translation Models Supporting Scottish Gaelic

#### M2M100 (Many-to-Many 100)
- **URL**: https://huggingface.co/facebook/m2m100_418M, https://huggingface.co/facebook/m2m100_1.2B
- **Model Names**: `facebook/m2m100_418M` (418M parameters), `facebook/m2m100_1.2B` (1.2B parameters)
- **Type**: Multilingual encoder-decoder for translation
- **Architecture**: Transformer seq-to-seq
- **Description**: Can directly translate between 9,900 directions across 100 languages including Scottish Gaelic.
- **Scottish Gaelic**:
  - Language code: `gd`
  - Usage: Pass "gd" via `forced_bos_token_id` parameter
- **Key Metrics**:
  - Downloads: 849,430 last month (418M model)
  - Framework: PyTorch + Transformers
- **Note**: Ready to use out-of-the-box, no fine-tuning required

#### SMALL-100
- **URL**: https://huggingface.co/alirezamsh/small100
- **Model Name**: `alirezamsh/small100`
- **Type**: Compact multilingual translation model
- **Architecture**: Distilled version of M2M-100
- **Description**: Supports over 10K language pairs across 101 languages including "Gaelic; Scottish Gaelic (gd)".
- **Key Metrics**:
  - Size: 0.3B parameters (3.6x smaller than M2M-100's 1.2B)
  - Speed: 4.3x faster than M2M-100
  - Downloads: 6,541 last month
  - Community: 26 Spaces, 19 discussions, 2 finetunes
  - Benchmarks: FLORES-101, Tatoeba, TICO-19
  - Framework: PyTorch + Transformers (Safetensors format)
- **Performance**: Comparable to larger M2M-100 despite being much smaller

#### NLLB-200 (No Language Left Behind)
- **URL**: https://huggingface.co/facebook/nllb-200-3.3B, https://huggingface.co/facebook/nllb-200-distilled-600M
- **Model Names**: `facebook/nllb-200-3.3B`, `facebook/nllb-200-distilled-600M`
- **Type**: Multilingual translation covering 200 languages
- **Description**: State-of-the-art translation model for low-resource languages including Scottish Gaelic.
- **Scottish Gaelic**:
  - Language code: `gd` or `gla_Latn`
  - Enabled for Wikipedia Content Translation Tool
- **Key Metrics**:
  - Languages: 200 total
  - Input limit: 512 tokens
  - Status: Research model
  - Framework: PyTorch + Transformers
- **HuggingFace Spaces**: UNESCO/nllb, Narrativaai/NLLB-Translator
- **Research**: Meta AI Research - No Language Left Behind initiative

### 3.3 Other Multilingual Models

#### mBART-50
- **URL**: https://huggingface.co/facebook/mbart-large-50-many-to-many-mmt
- **Model Name**: `facebook/mbart-large-50-many-to-many-mmt`
- **Description**: Multilingual translation model covering 50 languages
- **Scottish Gaelic**: **NOT SUPPORTED** (Scottish Gaelic is not among the 50 languages)

---

## 4. SPEECH/ASR MODELS FOR SCOTTISH GAELIC

### 4.1 Research and Development Status

**Current State**: No publicly available dedicated ASR models for Scottish Gaelic were found on HuggingFace Hub.

### 4.2 Research Work

#### Recent ASR Research (2025)
- **Paper**: "A Practitioner's Guide to Building ASR Models for Low-Resource Languages: A Case Study on Scottish Gaelic" (Interspeech 2025)
- **Achievements**:
  - Best WER: 12.8% (32% relative improvement over fine-tuned Whisper)
  - Fine-tuned Whisper-Turbo baseline: 22.0% WER → 19.0% WER with further fine-tuning
  - Hybrid HMM approach: 54% better performance than previous models
- **Toolkit**: HuggingFace Transformers used for training
- **Models tested**: Whisper fine-tuning, self-supervised models
- **Note**: Models not yet published to HuggingFace Hub

#### Historical ASR Work
- **Toolkit**: Kaldi speech recognition toolkit
- **Performance**: Final WER of 26.30%
- **Team**: Dr. Will Lamb and team at University of Edinburgh
- **Output**: World's first Scottish Gaelic Speech Recognition System (available as webapp with University of Bangor)

### 4.3 Base Models for Fine-tuning

#### Whisper (OpenAI)
- **URL**: https://huggingface.co/openai/whisper-large-v3
- **Model Names**: Multiple sizes (tiny, base, small, medium, large, large-v3)
- **Description**: Multilingual ASR model that can be fine-tuned for Scottish Gaelic
- **Note**: All 11 pre-trained checkpoints available on HuggingFace Hub
- **Tutorial**: HuggingFace provides "Fine-Tune Whisper with 🤗 Transformers" blog post
- **Research**: Fine-tuned Whisper models for Scottish Gaelic achieved 19-22% WER in research

#### Wav2Vec2 XLSR
- **URL**: https://huggingface.co/facebook/wav2vec2-large-xlsr-53
- **Model Name**: `facebook/wav2vec2-large-xlsr-53`
- **Description**: Multilingual self-supervised model covering 53 languages
- **Note**: Can be fine-tuned for Scottish Gaelic (no pre-trained Scottish Gaelic model available)
- **Related**: Irish Gaelic model available: `cpierse/wav2vec2-large-xlsr-53-irish`

### 4.4 Ongoing Development

#### University of Edinburgh Initiative
- **Funding**: £225,000 from Scottish Government
- **Goal**: Production of large language model (similar to ChatGPT) for Scottish Gaelic
- **Speech-to-Text API**: Expected by Q4 2025
- **Data Sources**:
  - 30 million words from University of Glasgow's Corpas na Gàidhlig
  - Audio archive from DASG's Cluas ri Claisneachd
- **Challenge**: Data sparsity for under-resourced language

---

## 5. OTHER RELEVANT NLP RESOURCES

### 5.1 Browse Pages

#### Models by Language
- **URL**: https://huggingface.co/models?language=gd
- **Description**: HuggingFace filter page showing all models tagged with Scottish Gaelic
- **Count**: 669 models with "gd" filter applied (may include multilingual models)

#### Datasets by Language
- **URL**: https://huggingface.co/datasets?language=language:gla
- **Description**: HuggingFace filter page showing all datasets tagged with Scottish Gaelic
- **Count**: 38 datasets across 2 pages

### 5.2 Research Organizations on HuggingFace

#### EdinburghNLP
- **URL**: https://huggingface.co/EdinburghNLP
- **Organization**: Natural Language Processing Group at the University of Edinburgh
- **Focus**: Morphology, parsing, semantics, discourse, language generation, machine translation, speech technology
- **Notable Datasets**:
  - EdinburghNLP/xsum (summarization dataset)
  - edinburghcstr/edacc (Edinburgh International Accents of English - 40 hours, 25 accents, not Scottish Gaelic)

#### Helsinki-NLP
- **URL**: https://huggingface.co/Helsinki-NLP
- **Organization**: Language Technology Research Group at the University of Helsinki
- **Focus**: Multilingual NLP, machine translation, wide language coverage
- **Notable Resources**: OPUS-MT project, Tatoeba datasets

#### National Library of Scotland
- **URL**: https://huggingface.co/NationalLibraryOfScotland
- **Datasets**: Historical documents (e.g., encyclopaedia_britannica_illustrated)
- **Note**: No specific Scottish Gaelic resources found

### 5.3 Related Celtic Language Resources

For comparison and potential transfer learning:

#### Irish Gaelic Models
- **gaBERT**: BERT model for Irish Gaelic (research: arxiv.org/abs/2107.12930)
- **wav2vec2-large-xlsr-53-irish**: ASR model for Irish by cpierse
- **beirt-irish-translation**: Irish translation model by pbdevpros

#### Welsh Models
- **Whisper models**: Multiple fine-tuned Whisper models for Welsh available (e.g., techiaith/whisper-large-v3-ft-commonvoice-cy-en)
- **Note**: Welsh has significantly more resources than Scottish Gaelic on HuggingFace

### 5.4 Corpora and Text Resources (External)

While not on HuggingFace, these are important Scottish Gaelic NLP resources:

- **ARCOSG**: Annotated Reference Corpus of Scottish Gaelic (University of Edinburgh DataShare)
- **Corpas na Gàidhlig**: 30 million words (University of Glasgow)
- **DASG**: Digital Archive of Scottish Gaelic
- **Scottish Gaelic Wikipedia**: Potential training data source
- **Sketch Engine**: Scottish Gaelic text corpora and Wiki corpus

### 5.5 Language Codes

Scottish Gaelic is referenced with multiple codes across platforms:
- **ISO 639-1**: `gd`
- **ISO 639-3**: `gla`
- **With script**: `gla-Latn` or `gd-Latn`

### 5.6 Key Statistics

- **Speakers**: 69,700 speakers in Scotland (Celtic language)
- **Resource Level**: Low-resource language
- **Data Availability**: Significantly limited compared to major languages
- **Active Development**: Growing AI/NLP community with recent initiatives (2024-2025)

---

## SUMMARY STATISTICS

### Models Found
- **Dedicated Scottish Gaelic models**: 2 (GPT-2 WECHSEL, XLM-RoBERTa POS)
- **Multilingual models supporting Scottish Gaelic**: 5+ (M2M100, SMALL-100, NLLB-200, mT5)
- **Translation models**: 1 dedicated (OPUS-MT synthetic), 3+ multilingual
- **ASR models**: 0 publicly available (research models not yet released)

### Datasets Found
- **Text corpora**: 5+ (CC-100, GlotCC-V1, mC4, etc.)
- **Parallel translation**: 5+ (XLSum, Tatoeba, OPUS-100, FLORES-200, etc.)
- **Speech**: 1 (Common Voice - now via Mozilla Data Collective)
- **Linguistic annotation**: 1 (Universal Dependencies)
- **Total datasets on HuggingFace**: 38+

### Key Gaps
- No publicly available fine-tuned ASR models
- Limited number of dedicated Scottish Gaelic models
- Most resources are multilingual models that include Scottish Gaelic as one of many languages
- Data sparsity remains a significant challenge

### Recent Developments (2024-2025)
- £225,000 Scottish Government funding for LLM development
- Speech-to-text API expected Q4 2025
- EMNLP 2025 paper on synthetic data for Scottish Gaelic translation
- Ongoing ASR research achieving state-of-the-art results

---

## RECOMMENDATIONS

1. **For Text Generation**: Use `benjamin/gpt2-wechsel-scottish-gaelic` (ready to use)
2. **For Translation**: Use `Helsinki-NLP/opus-mt-synthetic-en-gd`, `facebook/m2m100_418M`, or `alirezamsh/small100`
3. **For POS Tagging**: Use `wietsedv/xlm-roberta-base-ft-udpos28-gd`
4. **For ASR Development**: Fine-tune Whisper or Wav2Vec2 models on Common Voice/local datasets
5. **For Training Data**: Use CC-100, GlotCC-V1, mC4, or XLSum datasets

---

## REFERENCES

- WECHSEL Paper: https://aclanthology.org/2022.naacl-main.293/
- Scottish Gaelic ASR Guide: https://arxiv.org/abs/2506.04915
- OPUS-MT Synthetic Paper: ArXiv 2505.14423
- NLLB-200: https://ai.meta.com/research/no-language-left-behind/
- University of Edinburgh Gaelic Initiative: https://www.ed.ac.uk/news/2023/ai-initiative-gives-gaelic-a-foothold-in-the-digit

---

*Report compiled through comprehensive web search of HuggingFace Hub and related resources.*
*All URLs verified as of November 17, 2025.*
