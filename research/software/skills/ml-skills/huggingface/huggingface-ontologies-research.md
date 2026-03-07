# Hugging Face Ontologies, Taxonomies, and Data Structures

## Research Overview

This document provides a comprehensive overview of Hugging Face's organizational structures, metadata schemas, data formats, and semantic relationships based on extensive research conducted in November 2025.

---

## 1. MODEL TAXONOMY

### 1.1 Task-Based Classification

Hugging Face organizes models by pipeline types across different modalities:

#### **NLP Tasks**
- **Text Classification** (`text-classification`)
  - Sentiment analysis
  - Document classification
  - Auto class: `AutoModelForSequenceClassification`
  
- **Token Classification** (`token-classification`)
  - Named Entity Recognition (NER)
  - Part-of-speech tagging
  - Auto class: `AutoModelForTokenClassification`
  
- **Text Generation** (`text-generation`)
  - Causal language modeling
  - Auto class: `AutoModelForCausalLM`
  
- **Question Answering** (`question-answering`)
  - Extractive QA
  - Auto class: `AutoModelForQuestionAnswering`
  
- **Masked Language Modeling** (`fill-mask`)
  - Auto class: `AutoModelForMaskedLM`
  
- **Translation** (`translation`)
  - Machine translation (e.g., Helsinki-NLP/opus-mt-{src}-{tgt})

#### **Computer Vision Tasks**
- **Image Classification** (`image-classification`)
  - Models: ResNet, EfficientNet, ViT
  
- **Object Detection** (`object-detection`)
  - Models: YOLO, Faster R-CNN
  
- **Image-to-Text** (`image-to-text`)
  - Image captioning
  - Optical Character Recognition (OCR)

#### **Audio Tasks**
- **Automatic Speech Recognition** (`automatic-speech-recognition`)
- **Audio Classification** (`audio-classification`)
- **Text-to-Speech** (`text-to-speech`)

#### **Multimodal Tasks**
- **Visual Question Answering** (`visual-question-answering`)
- **Image-Text Retrieval**
- **Multi-modal understanding**
  - Models: CLIP, BLIP, Perceiver IO, Macaw-LLM, Phi-4-multimodal

### 1.2 Architecture-Based Classification

#### **Three Main Transformer Architectures**

1. **Encoder-Only Models**
   - Characteristics: Bi-directional attention
   - Access: Can see all words in input sentence
   - Use cases: Classification, NER, question answering
   - Examples: BERT, RoBERTa, ALBERT, DistilBERT, ELECTRA
   - Config: `is_encoder_decoder: false`, no decoder components

2. **Decoder-Only Models**
   - Characteristics: Auto-regressive, unidirectional attention
   - Access: Can only see previous tokens
   - Use cases: Text generation, completion, few-shot learning
   - Examples: GPT-2, GPT-3, GPT-Neo, GPT-J
   - Config: `is_decoder: true`

3. **Encoder-Decoder Models**
   - Characteristics: Full transformer architecture
   - Access: Encoder has bi-directional, decoder has causal attention
   - Use cases: Translation, summarization, text-to-text tasks
   - Examples: T5, BART, mT5, FLAN-T5
   - Config: `is_encoder_decoder: true`, `add_cross_attention: true`

### 1.3 Auto Classes

Hugging Face provides Auto Classes that automatically infer the correct model architecture:

- **AutoModel**: Base model loading
- **AutoTokenizer**: Automatic tokenizer loading
- **AutoConfig**: Automatic configuration loading
- **Task-Specific Auto Classes**: 
  - `AutoModelForSequenceClassification`
  - `AutoModelForTokenClassification`
  - `AutoModelForCausalLM`
  - `AutoModelForMaskedLM`
  - `AutoModelForQuestionAnswering`
  - And 40+ more variants

**How they work:**
- Use `from_pretrained()` method with model name/path
- Automatically detect architecture from `config.json`
- Fall back to pattern matching on model name if needed
- Return correct model class instance for the task

---

## 2. DATASET TAXONOMY

### 2.1 Format Taxonomy

#### **Standard vs. Conversational**
- **Standard Format**: Plain text strings with varying columns by task
- **Conversational Format**: Dialogue/chat interactions between users and assistants

#### **Format Types**
- **Prompt-only**: Contains only prompts
- **Preference**: Contains preference data for alignment
- **Instruction**: Instruction-response pairs

### 2.2 Task-Based Classification

- **Classification**: Categorical output variable
- **Regression**: Numerical output
- **Clustering**: Cluster assignment outputs
- **Language Identification**: 92+ languages supported (e.g., KDE4 dataset)
- **Translation**: Parallel corpora for translation tasks

### 2.3 Feature Types

Datasets have a tabular structure with typed features:

- **Text**: String data
- **Audio**: Audio waveforms and metadata
- **Image**: Image data with PIL/numpy format
- **ClassLabel**: Categorical labels with mappings
- **Sequence**: Lists of values
- **Translation**: Parallel text in multiple languages
- **Value**: Scalar numeric values

### 2.4 Dataset Splits

Standard split nomenclature:
- **train**: Training data
- **validation** (or **dev**): Validation data
- **test**: Test/evaluation data

**Split Operations:**
- `train_test_split()`: Create train/test splits with configurable ratios
- Supports both percentage and absolute sample counts
- Default shuffling (can be disabled)
- Streaming-compatible splitting for large datasets

---

## 3. METADATA SCHEMAS

### 3.1 Model Card Metadata (YAML)

Located at the top of `README.md` between `---` markers:

```yaml
---
language:
  - en
  - fr
  - multilingual
tags:
  - text-classification
  - sentiment-analysis
  - custom-tag
license: apache-2.0  # or mit, cc-by-4.0, etc.
datasets:
  - dataset-id-1
  - dataset-id-2
metrics:
  - accuracy
  - f1
  - bleu
  - rouge
base_model: bert-base-uncased
library_name: transformers  # or pytorch, tensorflow, jax
pipeline_tag: text-classification
model_type: bert  # Architecture identifier
widget:
  - text: "Example input"
inference: true
---
```

#### **Key Metadata Fields**

| Field | Description | Example Values |
|-------|-------------|----------------|
| `language` | Supported languages | `en`, `fr`, `multilingual` |
| `license` | Model license | `apache-2.0`, `mit`, `cc-by-4.0` |
| `tags` | Searchable keywords | `sentiment-analysis`, `ner` |
| `datasets` | Training datasets | Dataset Hub IDs |
| `metrics` | Evaluation metrics | `accuracy`, `f1`, `bleu` |
| `base_model` | Parent model if fine-tuned | Hub model ID |
| `library_name` | ML framework | `transformers`, `pytorch`, `tensorflow`, `jax` |
| `pipeline_tag` | Task type | `text-classification`, `token-classification` |
| `model_type` | Architecture type | `bert`, `gpt2`, `t5` |

### 3.2 Dataset Card Metadata (YAML)

Similar structure to model cards:

```yaml
---
language:
  - en
tags:
  - text-classification
  - sentiment
license: cc-by-4.0
size_categories:
  - 10K<n<100K
task_categories:
  - text-classification
  - token-classification
pretty_name: "Human-Readable Dataset Name"
---
```

#### **Size Categories**
- `n<1K`: Less than 1,000 samples
- `1K<n<10K`: 1,000 to 10,000 samples
- `10K<n<100K`: 10,000 to 100,000 samples
- `100K<n<1M`: 100,000 to 1 million samples
- `n>1M`: More than 1 million samples

### 3.3 Configuration Metadata (config.json)

Model configuration file containing architecture details:

```json
{
  "model_type": "bert",
  "architectures": ["BertForSequenceClassification"],
  "num_hidden_layers": 12,
  "num_attention_heads": 12,
  "hidden_size": 768,
  "vocab_size": 30522,
  "max_position_embeddings": 512,
  "is_encoder_decoder": false,
  "is_decoder": false,
  "add_cross_attention": false,
  "num_labels": 2,
  "id2label": {
    "0": "negative",
    "1": "positive"
  },
  "label2id": {
    "negative": 0,
    "positive": 1
  }
}
```

**Key Configuration Fields:**
- `model_type`: Identifier for model architecture
- `architectures`: List of compatible model classes
- `is_encoder_decoder`: Boolean for encoder-decoder models
- `is_decoder`: Boolean for decoder-only models
- `num_labels`: Number of classification labels
- `id2label` / `label2id`: Label mappings for better inference widgets

---

## 4. FILE FORMATS AND DATA STRUCTURES

### 4.1 Model File Formats

#### **Weight Files**

1. **Safetensors** (`.safetensors`)
   - Modern, safe serialization format
   - No pickle security risks
   - Fast loading with memory mapping
   - Header + data structure
   - Preferred format on Hugging Face Hub

2. **PyTorch Binary** (`pytorch_model.bin`)
   - Traditional pickle-based format
   - Contains state_dict (weights and biases)
   - Security concerns with pickle
   - Being gradually replaced by safetensors

3. **ONNX** (`.onnx`)
   - Cross-framework format
   - Located in `onnx/` subfolder
   - Used by Transformers.js for browser inference

#### **Tokenizer Files**

1. **tokenizer.json**
   - Primary file for fast tokenizers
   - Complete tokenizer serialization
   - Used by PreTrainedTokenizerFast

2. **tokenizer_config.json**
   - Tokenizer configuration settings
   - Special token definitions

3. **special_tokens_map.json**
   - Maps special tokens (PAD, CLS, SEP, etc.)
   - Vestigial in newer fast tokenizers

4. **vocab.txt** or **vocab.json**
   - Vocabulary mappings
   - Token to ID mappings

5. **added_tokens.json**
   - Tokens added after training

### 4.2 Dataset File Formats

#### **Parquet Format** (Recommended)
- **Extension**: `.parquet`
- **Structure**: Columnar storage with row groups
- **Compression**: Per-column compression with optimal algorithms
- **Benefits**: 
  - Efficient compression
  - Rich typing
  - Fast read operations
  - Batched operations
- **Use case**: Large-scale dataset storage and distribution

#### **Arrow Format** (Internal)
- **Purpose**: Internal caching and processing
- **Structure**: Columnar memory layout
- **Benefits**:
  - Zero-copy reads
  - Memory-mapped on-disk cache
  - Fast column queries
  - No serialization overhead
- **Relationship**: Parquet files are loaded as Arrow internally

#### **Other Supported Formats**
- **CSV** (`.csv`): Simple tabular data
- **JSON/JSONL** (`.json`, `.jsonl`): Structured data, one object per line
- **Text** (`.txt`): Plain text files
- **Audio** (`.mp3`, `.wav`, `.flac`): Audio files
- **Images** (`.jpg`, `.png`, `.tiff`): Image files
- **Compressed**: `.zip`, `.gz`, `.zst`, `.bz2`, `.lz4`, `.xz`

#### **WebDataset Format**
- Large-scale dataset format
- Tar-based archive structure
- Used for massive datasets

---

## 5. REPOSITORY STRUCTURE

### 5.1 Naming Conventions

#### **Repository Paths**
- **User repositories**: `username/repository-name`
- **Organization repositories**: `organization-name/repository-name`
- **Case sensitivity**: Must match organization capitalization exactly

#### **Model Naming Patterns**
Common conventions (not standardized):
- **Implementation + Task**: `Llama-2-7b-chat-hf`
- **Base Model + Fine-tune**: `bert-base-uncased-finetuned-sst2`
- **Organization + Model**: `Helsinki-NLP/opus-mt-en-fr`
- **Size Indicator**: `t5-small`, `gpt2-medium`, `bert-large`

### 5.2 Model Repository Structure

```
model-repository/
├── README.md                    # Model card with YAML metadata
├── config.json                  # Model architecture configuration
├── pytorch_model.bin            # PyTorch weights (legacy)
├── model.safetensors           # Safetensors weights (preferred)
├── tokenizer.json              # Fast tokenizer definition
├── tokenizer_config.json       # Tokenizer configuration
├── special_tokens_map.json     # Special token mappings
├── vocab.txt                   # Vocabulary
├── .gitattributes             # Git LFS configuration
└── onnx/                      # ONNX format (optional)
    └── model.onnx
```

### 5.3 Dataset Repository Structure

#### **Simple Structure**
```
dataset-repository/
├── README.md           # Dataset card with YAML metadata
├── train.csv          # Training split
├── validation.csv     # Validation split
└── test.csv          # Test split
```

#### **Multi-file Structure**
```
dataset-repository/
├── README.md
├── train/
│   ├── data-00000.parquet
│   ├── data-00001.parquet
│   └── ...
├── validation/
│   └── data.parquet
└── test/
    └── data.parquet
```

#### **YAML Configuration**
For complex structures, define splits in README.md metadata:
```yaml
---
configs:
  - config_name: default
    data_files:
      train: "train/*.parquet"
      validation: "validation/*.parquet"
      test: "test/*.parquet"
---
```

### 5.4 Version Control with Git LFS

#### **How Hugging Face Uses Git LFS**
- **Purpose**: Manage large model and dataset files
- **Mechanism**: Stores large files on separate server
- **Pointers**: Small placeholder files in Git repository
- **Versioning**: Based on Git commits, tags, and branches

#### **Current Scale** (as of research date)
- 1.3 million models
- 450,000 datasets
- 680,000 spaces
- 12 PB stored in LFS (280M files)
- 7.3 TB stored in Git (non-LFS)

#### **Migration to Xet Storage**
- Hugging Face is migrating from Git LFS to Xet storage
- Reason: Git LFS limitations (5GB file size, 10GB repo size)
- Goal: Better handling of massive AI files at scale

---

## 6. TAG SYSTEMS AND CATEGORIZATION

### 6.1 Core Tag Types

#### **Pipeline Tag** (`pipeline_tag`)
Determines the ML task and inference widget:
- Automatically inferred from `config.json` for transformers
- Can be manually overridden in model card metadata
- Examples: `text-classification`, `image-to-text`, `translation`

#### **Library Name** (`library_name`)
Specifies the framework/library:
- `transformers`: Hugging Face Transformers
- `pytorch`: PyTorch
- `tensorflow`: TensorFlow
- `jax`: JAX/Flax
- `sklearn`: Scikit-learn
- `onnx`: ONNX Runtime
- And many others

#### **Language Tags**
- ISO language codes: `en`, `fr`, `de`, `zh`, `ja`, etc.
- `multilingual`: Supports multiple languages
- Used in language embeddings for models like XLM
- Maps to `lang2id` and `id2lang` in tokenizer

#### **License Tags**
Valid license identifiers:
- `apache-2.0`: Apache License 2.0
- `mit`: MIT License
- `cc-by-4.0`: Creative Commons Attribution 4.0
- `cc-by-nc-4.0`: CC Attribution Non-Commercial
- `openrail`: OpenRAIL licenses
- And many others

### 6.2 Task Categories

Comprehensive task categorization:
- `text-classification`
- `token-classification`
- `question-answering`
- `summarization`
- `translation`
- `conversational`
- `text-generation`
- `fill-mask`
- `sentence-similarity`
- `text-to-speech`
- `automatic-speech-recognition`
- `audio-classification`
- `image-classification`
- `object-detection`
- `image-segmentation`
- `image-to-text`
- `zero-shot-classification`
- `zero-shot-image-classification`
- `visual-question-answering`
- And 40+ more

### 6.3 Domain Tags

Custom domain categorization:
- Model-specific domains (e.g., medical, legal, financial)
- NVIDIA's multilingual-domain-classifier: 26 domain classes across 52 languages
- Free-form tags for specialized domains

---

## 7. EVALUATION METRICS

### 7.1 Generic Metrics

- **Accuracy**: Proportion of correct predictions
  ```python
  accuracy = correct_predictions / total_predictions
  ```

- **Precision**: True positives / (True positives + False positives)

- **Recall**: True positives / (True positives + False negatives)

- **F1 Score**: Harmonic mean of precision and recall
  ```python
  F1 = 2 * (precision * recall) / (precision + recall)
  ```

### 7.2 Task-Specific Metrics

#### **Machine Translation**
- **BLEU** (Bilingual Evaluation Understudy)
  - Evaluates machine-translated text quality
  - Compares to reference translations
  
- **ROUGE** (Recall-Oriented Understudy for Gisting Evaluation)
  - Used for summarization and translation
  - Multiple variants (ROUGE-1, ROUGE-2, ROUGE-L)

#### **NLP Tasks**
- **Perplexity**: Language model quality
- **Exact Match**: For QA tasks
- **METEOR**: Machine translation evaluation
- **BERTScore**: Semantic similarity using BERT

#### **Computer Vision**
- **IoU** (Intersection over Union): Object detection
- **mAP** (Mean Average Precision): Object detection
- **FID** (Fréchet Inception Distance): Generative models

### 7.3 Metric Cards

Each metric has a metric card providing:
- Input structure and format
- Detailed explanations
- Limitations and considerations
- Appropriate use cases
- Example usage

---

## 8. SEMANTIC RELATIONSHIPS

### 8.1 Model Family Relationships

#### **Base Model → Fine-tuned Derivatives**
Tracked via `base_model` metadata field:
```yaml
---
base_model: bert-base-uncased
---
```

Examples of lineage:
- `bert-base-uncased` → `bert-base-uncased-finetuned-sst2`
- `gpt2` → `gpt2-medium` → `gpt2-large` → `gpt2-xl`
- `t5-small` → `t5-base` → `t5-large` → `t5-3b` → `t5-11b`

#### **Model Variants and Relationships**

1. **Size Variants**
   - Small → Base → Large → XL variants
   - Trade-off: Speed vs. performance

2. **Distilled Models**
   - BERT → DistilBERT (faster, smaller)
   - RoBERTa → DistilRoBERTa
   - Goal: Maintain performance with reduced size

3. **Quantized Versions**
   - FP32 → FP16 → INT8 → INT4
   - Reduced memory and faster inference

4. **Merged Models**
   - Combination of multiple base models
   - Documented in `base_model` field

### 8.2 Modality Classifications

#### **Unimodal Models**
- **Text-only**: BERT, GPT, T5
- **Vision-only**: ResNet, EfficientNet, ViT
- **Audio-only**: Wav2Vec2, Whisper

#### **Multimodal Models**
- **Vision-Language**: CLIP, BLIP, LLaVA
- **Audio-Text**: Whisper, Speech2Text
- **Video**: VideoMAE, TimeSformer
- **Multi-modal (Image+Audio+Text)**: Perceiver IO, Macaw-LLM, Phi-4-multimodal

**Fusion Strategies:**
1. Unimodal encoders process each modality
2. Fusion module combines encoded representations
3. Classification/generation network produces output

### 8.3 Language and Multilingual Relationships

#### **Language-Specific Models**
- English: `bert-base-uncased`, `roberta-base`
- French: `camembert-base`
- German: `bert-base-german-cased`
- Chinese: `bert-base-chinese`

#### **Multilingual Models**
- `bert-base-multilingual-cased`: 104 languages
- `xlm-roberta-base`: 100 languages
- `mT5`: Multilingual T5 variant

#### **Cross-lingual Models**
- XLM with language embeddings
- Language ID to embedding mappings via `lang2id`

### 8.4 Task Relationships

#### **Task Hierarchies**

```
Text Tasks
├── Classification
│   ├── Sequence Classification
│   └── Token Classification
├── Generation
│   ├── Causal LM
│   ├── Seq2Seq
│   └── Masked LM
└── Understanding
    ├── Question Answering
    └── Named Entity Recognition

Vision Tasks
├── Classification
│   ├── Image Classification
│   └── Video Classification
├── Detection
│   ├── Object Detection
│   └── Instance Segmentation
└── Generation
    ├── Image Generation
    └── Image-to-Image

Multimodal Tasks
├── Vision-Language
│   ├── Visual Question Answering
│   ├── Image Captioning
│   └── Image-Text Retrieval
└── Audio-Text
    ├── Speech Recognition
    └── Audio Captioning
```

---

## 9. INFERENCE AND WIDGETS

### 9.1 Widget Configuration

Inference widgets are automatically determined by:
1. **Primary**: `pipeline_tag` in model card metadata
2. **Fallback**: `config.json` architecture
3. **Override**: Manual specification in YAML

### 9.2 Widget Requirements

For a widget to work:
- Task must be supported by inference providers
- Model must have proper metadata
- Label mappings (`id2label`, `label2id`) for better display
- Appropriate configuration files

### 9.3 Inference API

- Programmatic access via `huggingface_hub` library
- Automatic task detection from metadata
- Task-specific parameters
- Examples:
  - Zero-shot classification: Requires `candidate_labels`
  - Text generation: Accepts `max_length`, `temperature`

---

## 10. VALIDATION AND QUALITY

### 10.1 Metadata Validation

#### **Python API Validation**
```python
from huggingface_hub import ModelCard

card = ModelCard.load("model-name")
card.validate()  # Validates against Hub rules
```

#### **Automatic Validation**
- Called internally by `push_to_hub()`
- Requires internet access
- Checks against Hub validation logic

#### **Webhook-based Quality Review**
- Automatic metadata quality review for models and datasets
- Ensures compliance with best practices

### 10.2 Metadata UI

Interactive metadata editor:
1. Navigate to model/dataset page
2. Click "Edit model card"
3. Use UI to add/modify metadata tags
4. Suggests popular tags
5. Allows custom tags

---

## 11. INTEGRATION PATTERNS

### 11.1 Framework Interoperability

Hugging Face supports seamless framework switching:
- **PyTorch** → **TensorFlow**: Automatic conversion
- **PyTorch** → **JAX**: Direct compatibility
- **Any framework** → **ONNX**: Export for cross-platform

### 11.2 Hub Integration

Libraries with Hub integration:
- **Transformers**: Native integration
- **Diffusers**: Image generation models
- **Sentence Transformers**: Embedding models
- **Timm**: Vision models
- **spaCy**: NLP pipelines
- **SetFit**: Few-shot classification

### 11.3 Dataset Integration

- **datasets** library: Load any Hub dataset
- **Streaming mode**: Process without full download
- **Automatic caching**: Arrow-based local cache
- **Train/test splits**: Built-in splitting utilities

---

## 12. KEY TAKEAWAYS

### 12.1 Organizational Principles

1. **Task-Centric**: Models and datasets organized primarily by ML task
2. **Metadata-Driven**: YAML metadata enables discovery and automation
3. **Interoperable**: Framework-agnostic with automatic conversions
4. **Semantic**: Rich relationships between models, datasets, and tasks
5. **Versioned**: Git and Git LFS for complete version history

### 12.2 Discovery Mechanisms

- **Tags**: Free-form and structured tags for search
- **Filters**: Task, language, license, library, size
- **Relationships**: Base model tracking for lineage
- **Metrics**: Searchable evaluation metrics
- **Widgets**: Visual testing of model capabilities

### 12.3 Best Practices

1. **Metadata Completeness**: Fill all relevant YAML fields
2. **Label Mappings**: Add `id2label` for better inference widgets
3. **Base Model**: Specify `base_model` for fine-tuned models
4. **License**: Always specify license for legal clarity
5. **Documentation**: Complete model/dataset cards
6. **Format**: Use Parquet for datasets, Safetensors for models

---

## 13. REFERENCES

### Official Documentation
- **Hub Documentation**: https://huggingface.co/docs/hub
- **Transformers**: https://huggingface.co/docs/transformers
- **Datasets**: https://huggingface.co/docs/datasets
- **Evaluate**: https://huggingface.co/docs/evaluate
- **huggingface_hub**: https://huggingface.co/docs/huggingface_hub

### Task Pages
- **Tasks Overview**: https://huggingface.co/tasks
- **Model Tasks**: https://huggingface.co/docs/hub/models-tasks
- **Dataset Tasks**: https://huggingface.co/docs/hub/datasets-tasks

### Metadata Specifications
- **Model Cards**: https://huggingface.co/docs/hub/model-cards
- **Dataset Cards**: https://huggingface.co/docs/hub/datasets-cards
- **Repository Cards**: https://huggingface.co/docs/huggingface_hub/package_reference/cards

---

**Research Conducted**: November 17, 2025  
**Knowledge Base**: Hugging Face Hub, official documentation, and community resources  
**Coverage**: Models, Datasets, Tasks, Metadata, File Formats, and Semantic Relationships
