# Irish-English Bilingual Datasets: Comprehensive Technical Reference

**Version:** 1.0
**Last Updated:** 2025-12-26
**Consolidated from:** Gaois Research, Agentic Translation Workflows, Neuro-Symbolic Translation Training

---

## Table of Contents

1. [Gaois Parallel Corpus](#1-gaois-parallel-corpus)
2. [Source Inventory](#2-source-inventory)
3. [TMX Processing and Alignment](#3-tmx-processing-and-alignment)
4. [Dataset Formats](#4-dataset-formats)
5. [Translation Model Training](#5-translation-model-training)

---

## 1. Gaois Parallel Corpus

### 1.1 Overview

The Gaois Research Group at Dublin City University maintains Ireland's most comprehensive digital Irish language resources. The **Parallel English-Irish Corpus** represents the largest publicly available aligned bilingual dataset for Irish.

**Source:** https://www.gaois.ie/en/corpora/parallel/data
**Format:** TMX (Translation Memory eXchange)
**Method:** Direct download from Gaois servers

### 1.2 Dataset Specifications

| Metric | Value |
|--------|-------|
| **Total Size** | ~130.5 million words |
| **Irish Words** | 68.0 million words |
| **English Words** | 62.5 million words |
| **Alignment Level** | Sentence-level parallel alignment |

### 1.3 Content Types

The corpus contains the following document categories:

| Content Type | Description |
|--------------|-------------|
| **EU Legislation** | Regulations and Directives from the European Union |
| **Constitution of Ireland** | Bunreacht na hEireann (1937) |
| **Acts of the Oireachtas** | Irish parliamentary legislation (1922-2003+) |
| **Statutory Instruments** | Irish secondary legislation |
| **COVID-19 Terminology** | Pandemic-related bilingual terminology |

### 1.4 Quality Assessment

| Attribute | Rating | Notes |
|-----------|--------|-------|
| **Alignment Quality** | Excellent | Sentence-level professional alignment |
| **Domain Coverage** | Legal/Statutory | High-register formal language |
| **Completeness** | 100% | Fully aligned corpus |
| **License** | Open (verify specific terms) | Suitable for research |

### 1.5 Acquisition Strategy

```bash
# Direct download from Gaois website
wget https://www.gaois.ie/en/corpora/parallel/data

# TMX files can be parsed using Python libraries
pip install translate-toolkit
```

---

## 2. Source Inventory

### 2.1 Primary Data Sources

#### 2.1.1 Duchas.ie - National Folklore Collection

**Endpoint:** https://www.duchas.ie/api/v0.6
**Documentation:** https://docs.gaois.ie/en/data/duchas/v0.6/api
**Status:** Beta (v0.6), active development

**Dataset Specifications:**

| Collection | Description | Size |
|------------|-------------|------|
| **Main Manuscript Collection (CBE)** | Bound volumes since 1932 | 2,400 volumes |
| **Schools' Collection (CBES)** | Folklore from 1937-1939 | 740,000 pages |
| **Photographic Collection (CBEG)** | Visual documentation | 80,000+ photographs |

**Language Distribution:**
- ~66% content in Irish
- ~33% content in English
- Bilingual metadata throughout

**API Query Example:**
```python
GET /api/v0.6/stories?language=ga&county=Cork
```

#### 2.1.2 Logainm.ie - Placenames Database

**Endpoint:** https://www.logainm.ie/api/v1.0
**Documentation:** https://docs.gaois.ie/en/data/logainm/v1.0/api
**Status:** Production

**Dataset Specifications:**
- **100,000+ placenames** with bilingual entries
- Irish and English forms for all locations
- Geographic coordinates and historical variants
- Coverage: All 32 Irish counties (townlands, parishes, counties)

**Data Structure Example:**
```json
{
  "id": 37704,
  "nameGA": "Baile Hein",
  "nameEN": "Hayestown",
  "category": "townland",
  "coordinates": {...},
  "county": "Meath",
  "variants": [...]
}
```

#### 2.1.3 Tearma.ie - National Terminology Database

**Website:** https://www.tearma.ie/
**Download Path:** /ioslodail/

**Dataset Specifications:**
- National terminology database for Irish
- 40+ subject categories
- Hierarchical classification system
- Irish-English term pairs

**Content Categories:**

| Category | Examples |
|----------|----------|
| **Legal** | Mionnscribhinn (Affidavit) |
| **Medical** | Stoicaimeadracht (Stoichiometry) |
| **Technical** | Algartaim (Algorithms) |
| **EU Terminology** | Official translations |
| **COVID-19** | Pandemic terminology |

#### 2.1.4 Ainm.ie - Biographical Database

**URL:** https://www.ainm.ie/
**Integration:** Via Logainm API

**Dataset Specifications:**
- **1,785 biographies** of notable Irish speakers
- Date range: 1560 to present
- **1.3+ million words** of Irish text
- Source: *Beathaisnis* by Diarmuid Breathnach & Maire Ni Mhurchu

**Note:** Biographies are **Irish-only** (no English translations); metadata is bilingual.

#### 2.1.5 Corpas.ie - Irish Language Corpora

**URL:** https://www.corpas.ie/en/cng/

| Corpus | Size | Date Range |
|--------|------|------------|
| **National Corpus of Irish (CNG)** | 100 million words | 2000-2024 |
| **Corpus of Written Irish** | 131 million words | Various |
| **Corpus of Spoken Irish** | 9 million words | Transcriptions |
| **Historical Corpus** | 3,000+ texts | 1600-1926 |

### 2.2 GitHub Repositories

#### 2.2.1 gaoisalign - Text Alignment Tool

**Repository:** https://github.com/gaois/gaoisalign
**Language:** Python
**License:** MIT

```bash
git clone https://github.com/gaois/gaoisalign.git
cd gaoisalign
# Examine README.md and gaoisalign.py for usage
```

#### 2.2.2 Terminologue - Terminology Management System

**Repository:** https://github.com/gaois/terminologue
**Language:** JavaScript
**Stars:** 59
**License:** MIT

The software behind Tearma.ie - useful for database schema understanding.

#### 2.2.3 sloinnte - Irish Surnames Database

**Repository:** https://github.com/gaois/sloinnte
**Language:** XSLT
**License:** MIT

Contains Irish surname forms with English equivalents and linguistic metadata.

### 2.3 Supporting Repositories

| Repository | Language | Purpose |
|------------|----------|---------|
| **Gaois.Localizer** | C# | Multilingual web app framework |
| **GeoNames2Sql** | C# | Gazetteer data to SQL converter |
| **IrishSurnameIndex** | - | Surnames from Folklore Commission |
| **Gaois.QueryLogger** | C# | API logging utility |
| **documental** | CSS | Multilingual documentation platform |
| **screenful** | JavaScript | Database front-end framework |

### 2.4 Total Dataset Size Summary

| Source | Words (Irish) | Words (English) | Items | Method |
|--------|---------------|-----------------|-------|--------|
| **Parallel Corpus (TMX)** | 68M | 62.5M | 130M segments | Download |
| **Corpas.ie** | 240M | - | - | Download |
| **Duchas API** | ~50M | ~30M | 80,000+ | API |
| **Logainm API** | - | - | 100,000+ | API |
| **Ainm.ie** | 1.3M | - | 1,785 | Scrape |
| **Tearma.ie** | 100K+ | 100K+ | 10,000+ | API/Scrape |
| **Total Estimate** | **359M+** | **93M+** | **200K+ items** | Mixed |

---

## 3. TMX Processing and Alignment

### 3.1 TMX File Format

TMX (Translation Memory eXchange) is the standard format for parallel corpus data. The Gaois corpus uses TMX XML structure with aligned translation units.

**Structure Example:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<tmx version="1.4">
  <header creationtool="gaois" srclang="en" adminlang="en"/>
  <body>
    <tu>
      <tuv xml:lang="en">
        <seg>The Constitution of Ireland</seg>
      </tuv>
      <tuv xml:lang="ga">
        <seg>Bunreacht na hEireann</seg>
      </tuv>
    </tu>
  </body>
</tmx>
```

### 3.2 TMX Processing with Python

```python
#!/usr/bin/env python3
"""
TMX Processing Pipeline for Irish-English Parallel Corpus
"""

from translate.storage.tmx import tmxfile
import json
from pathlib import Path

def parse_tmx_file(tmx_path: str) -> list:
    """Parse TMX file and extract translation units."""
    with open(tmx_path, 'rb') as f:
        tmx = tmxfile(f)

    translation_units = []
    for unit in tmx.units:
        translation_units.append({
            'source': unit.source,
            'target': unit.target,
            'source_lang': 'en',
            'target_lang': 'ga'
        })

    return translation_units

def export_to_jsonl(units: list, output_path: str):
    """Export translation units to JSONL format."""
    with open(output_path, 'w', encoding='utf-8') as f:
        for unit in units:
            f.write(json.dumps(unit, ensure_ascii=False) + '\n')

# Usage
units = parse_tmx_file('gaois_parallel_corpus.tmx')
export_to_jsonl(units, 'irish_english_parallel.jsonl')
print(f"Exported {len(units)} translation units")
```

### 3.3 Alignment Tools

#### 3.3.1 gaoisalign (Gaois Native Tool)

The `gaoisalign` tool from the Gaois GitHub repository provides Irish-specific text alignment:

```python
# Installation
git clone https://github.com/gaois/gaoisalign.git
cd gaoisalign
pip install -r requirements.txt

# Usage (example)
from gaoisalign import align_texts

english_text = "The quick brown fox."
irish_text = "An sionnach donn tapaidh."

aligned = align_texts(english_text, irish_text)
```

#### 3.3.2 hunalign (Generic Sentence Alignment)

For fallback alignment when gaoisalign is insufficient:

```bash
# Installation
sudo apt-get install hunalign

# Usage
hunalign -text en-ga.dic english.txt irish.txt > aligned.txt
```

#### 3.3.3 NLTK for Tokenization

```python
import nltk
from nltk.tokenize import sent_tokenize, word_tokenize

nltk.download('punkt')

def tokenize_irish_text(text: str) -> list:
    """Tokenize Irish text into sentences and words."""
    sentences = sent_tokenize(text)
    tokenized = []
    for sent in sentences:
        tokens = word_tokenize(sent)
        tokenized.append(tokens)
    return tokenized
```

### 3.4 Data Processing Pipeline Architecture

```
+-------------------------------------------------------------+
|                  DATA ACQUISITION LAYER                      |
+--------------+--------------+--------------+-----------------+
|  GitHub      |  API Access  |  Direct DL   |  Web Scraping   |
|  Clone       |  (JSON)      |  (TMX/ZIP)   |  (crawl4ai)     |
+------+-------+------+-------+------+-------+------+----------+
       |              |              |              |
       v              v              v              v
+-------------------------------------------------------------+
|                    PROCESSING LAYER                          |
+--------------+--------------+--------------+-----------------+
|  Parse TMX   |  Parse JSON  |  Extract MD  |  Align Texts    |
|  to parallel |  responses   |  from HTML   |  (gaoisalign)   |
+------+-------+------+-------+------+-------+------+----------+
       |              |              |              |
       v              v              v              v
+-------------------------------------------------------------+
|                   NORMALIZATION LAYER                        |
|  - Standardize encoding (UTF-8)                              |
|  - Normalize Irish orthography (old -> modern)               |
|  - Clean HTML artifacts                                      |
|  - Tokenize sentences                                        |
|  - Align parallel segments                                   |
+--------------------------+----------------------------------+
                           |
                           v
+-------------------------------------------------------------+
|                      STORAGE LAYER                           |
+--------------+--------------+--------------+-----------------+
|  SQLite DB   |  JSON Lines  |  Parquet     |  HuggingFace    |
|  (metadata)  |  (streaming) |  (analytics) |  Datasets       |
+--------------+--------------+--------------+-----------------+
```

### 3.5 API Data Collection Example

```python
#!/usr/bin/env python3
"""
Gaois API Data Collector
Collects Irish-English bilingual data from Gaois APIs
"""

import asyncio
import aiohttp
import json
from typing import List, Dict
from pathlib import Path

class GaoisAPICollector:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.base_urls = {
            'logainm': 'https://www.logainm.ie/api/v1.0',
            'duchas': 'https://www.duchas.ie/api/v0.6'
        }
        self.headers = {'X-Api-Key': api_key}

    async def fetch_logainm_placenames(
        self,
        session: aiohttp.ClientSession
    ) -> List[Dict]:
        """Fetch all placenames from Logainm API"""
        url = f"{self.base_urls['logainm']}/placenames"
        placenames = []
        page = 1

        while True:
            async with session.get(
                f"{url}?page={page}&per_page=100",
                headers=self.headers
            ) as response:
                if response.status != 200:
                    break

                data = await response.json()
                if not data.get('results'):
                    break

                placenames.extend(data['results'])
                page += 1

                # Rate limiting
                await asyncio.sleep(0.5)

        return placenames

    async def fetch_duchas_stories(
        self,
        session: aiohttp.ClientSession
    ) -> List[Dict]:
        """Fetch folklore stories from Duchas API"""
        url = f"{self.base_urls['duchas']}/stories"
        stories = []

        for lang in ['ga', 'en']:
            async with session.get(
                f"{url}?language={lang}&per_page=100",
                headers=self.headers
            ) as response:
                if response.status == 200:
                    data = await response.json()
                    stories.extend(data.get('results', []))

        return stories

    async def collect_all_data(self) -> Dict[str, List]:
        """Main collection orchestrator"""
        async with aiohttp.ClientSession() as session:
            placenames, stories = await asyncio.gather(
                self.fetch_logainm_placenames(session),
                self.fetch_duchas_stories(session)
            )

            return {
                'placenames': placenames,
                'folklore': stories
            }

    def save_dataset(self, data: Dict, output_dir: Path):
        """Save collected data to disk"""
        output_dir.mkdir(exist_ok=True)

        for dataset_name, records in data.items():
            output_file = output_dir / f"{dataset_name}.jsonl"
            with output_file.open('w', encoding='utf-8') as f:
                for record in records:
                    f.write(json.dumps(record, ensure_ascii=False) + '\n')

            print(f"Saved {len(records)} records to {output_file}")

async def main():
    api_key = "YOUR_API_KEY_HERE"  # Get from gaois.ie

    collector = GaoisAPICollector(api_key)
    data = await collector.collect_all_data()
    collector.save_dataset(data, Path("./gaois_datasets"))

if __name__ == "__main__":
    asyncio.run(main())
```

---

## 4. Dataset Formats

### 4.1 JSON Lines (.jsonl)

The recommended streaming format for large parallel corpora:

```json
{"id": 1, "irish": "Baile Atha Cliath", "english": "Dublin", "source": "logainm", "metadata": {...}}
{"id": 2, "irish": "Bunreacht na hEireann", "english": "Constitution of Ireland", "source": "gaois", "domain": "legal"}
```

**Advantages:**
- Streaming-friendly (line-by-line processing)
- Easy parsing with standard JSON libraries
- Supports incremental updates

### 4.2 Parquet Format

Compressed columnar storage for analytics:

```python
import pandas as pd
import pyarrow.parquet as pq

# Convert to Parquet
df = pd.read_json('parallel_corpus.jsonl', lines=True)
df.to_parquet('irish_english_parallel.parquet', compression='snappy')

# Read Parquet
df = pd.read_parquet('irish_english_parallel.parquet')
print(f"Loaded {len(df)} records")
```

**Advantages:**
- Highly compressed (snappy, gzip, zstd)
- Columnar format enables fast queries
- Excellent for analytics pipelines

### 4.3 HuggingFace Datasets

The standard format for ML/LLM training:

```python
from datasets import Dataset, DatasetDict

# Load from JSONL
dataset = Dataset.from_json('parallel_corpus.jsonl')

# Or from Pandas DataFrame
dataset = Dataset.from_pandas(df)

# Create train/validation/test splits
dataset_dict = dataset.train_test_split(test_size=0.1)
dataset_dict = DatasetDict({
    'train': dataset_dict['train'],
    'validation': dataset_dict['test'].train_test_split(test_size=0.5)['train'],
    'test': dataset_dict['test'].train_test_split(test_size=0.5)['test']
})

# Push to HuggingFace Hub
dataset_dict.push_to_hub("your-username/irish-english-parallel")
```

**Dataset Card Template:**

```yaml
---
language:
  - ga
  - en
license: cc-by-4.0
task_categories:
  - translation
tags:
  - irish
  - gaeilge
  - parallel-corpus
  - bilingual
size_categories:
  - 100M<n<1B
---

# Irish-English Parallel Corpus

## Dataset Description

This dataset contains aligned Irish-English parallel text from the Gaois Research Group.

### Sources
- Gaois Parallel Corpus (130.5M words)
- Logainm placenames (100K+ entries)
- Duchas folklore collection
- Tearma terminology database

### Statistics
| Split | Examples |
|-------|----------|
| Train | X |
| Validation | X |
| Test | X |
```

### 4.4 TMX Format (Preserve Original)

Maintain TMX for CAT tool compatibility:

```python
from translate.storage.tmx import tmxfile, tmxunit

def create_tmx_file(parallel_data: list, output_path: str):
    """Create TMX file from parallel data."""
    tmx = tmxfile()
    tmx.settargetlanguage('ga')

    for pair in parallel_data:
        unit = tmxunit(pair['english'])
        unit.target = pair['irish']
        tmx.addunit(unit)

    with open(output_path, 'wb') as f:
        tmx.serialize(f)
```

---

## 5. Translation Model Training

### 5.1 Architecture Overview: T5Gemma-2 + Diffusion Refinement

The state-of-the-art approach for English-Irish translation combines:

1. **T5Gemma-2** (Encoder-Decoder) for initial drafting
2. **Gemini 3** for reasoning/critique
3. **Diffusion models** for refinement and visual fidelity

### 5.2 T5Gemma-2: The Linguistic Workhorse

#### 5.2.1 Architecture Advantages

T5Gemma-2 returns to the encoder-decoder architecture, which separates understanding (Encoder) from generation (Decoder):

| Feature | Benefit |
|---------|---------|
| **Encoder-Decoder Split** | Full source visibility before generation |
| **Tied Embeddings** | 10.5% parameter reduction |
| **Merged Attention** | Faster inference speeds |
| **140+ Languages** | Transfer learning for Irish |

#### 5.2.2 The "Deep Reading" Advantage

```
Source Text (English) --> [ENCODER] --> Full Bidirectional Representation
                                               |
                                               v
                          [DECODER] --> Irish Translation
```

The encoder creates a complete representation of the source before the decoder generates any output, enabling resolution of long-distance dependencies.

#### 5.2.3 Model Variants

| Model | Parameters | Use Case |
|-------|------------|----------|
| **T5Gemma-2-270M** | 270M | Edge deployment, mobile |
| **T5Gemma-2-1B** | 1B | Standard translation |
| **T5Gemma-2-4B** | 4B | High-quality drafting |

### 5.3 Agentic Translation Workflow

The recommended architecture uses a multi-agent system with specialized roles:

```
+-----------------------------------------------------------+
|                    ROOT ORCHESTRATOR                        |
+-----------------------------------------------------------+
         |                    |                    |
         v                    v                    v
+----------------+  +------------------+  +----------------+
| INGESTION      |  | DRAFTING LOOP    |  | COMPLIANCE     |
| AGENT          |  | (T5Gemma-2)      |  | AGENT          |
| (Gemini Flash) |  |                  |  | (BAML/Ontology)|
+----------------+  +------------------+  +----------------+
                           |
                           v
                  +------------------+
                  | CRITIC AGENT     |
                  | (Gemini 3 Pro)   |
                  | System 2 Reason  |
                  +------------------+
```

#### 5.3.1 Workflow Phases

**Phase 1: Ingestion (Gemini 3 Flash)**
- OCR for handwritten/archaic documents
- Layout analysis and structure extraction
- Context vector extraction (domain, dialect, register)

**Phase 2: Drafting (T5Gemma-2)**
- Generate initial translation respecting context
- Leverage multilingual transfer learning
- Served via Transformers v5 continuous batching

**Phase 3: Critique (Gemini 3 Pro)**
- System 2 reasoning verification
- Check semantic fidelity
- Verify grammatical mutations (seimhiu/uru)
- Dialectal consistency (Ulster/Connacht/Munster)

**Phase 4: Compliance (BAML + Ontology)**
- Neuro-symbolic truth anchoring
- Terminology enforcement from Tearma.ie
- Hard replacement of non-standard terms

### 5.4 Diffusion Model Refinement: InkSpire Architecture

For visual translation (documents, handwriting), the InkSpire diffusion architecture provides:

#### 5.4.1 Unified Latent Representation

Instead of separate encoders for style and content:

```
Style (Irish orthography) + Content (English semantics) + Noise
                              |
                              v
                    [SHARED LATENT SPACE]
                              |
                              v
                    [DIFFUSION TRANSFORMER]
                              |
                              v
                    Irish Document Output
```

#### 5.4.2 Multi-line Masked Infilling

Training objective for document translation:

```python
# Masked Conditional Flow Matching (MCFM)
# Loss = L_diff (velocity prediction vs true vector field)

# Reference (conditioning): English layout
# Target (masked): Irish text regions
# Model learns: flow from noise to Irish text conditioned on English context
```

#### 5.4.3 Rotated Aligned Position Encoding (R-APE)

Enables spatial alignment for:
- Chemical equations
- Mathematical notation
- Code indentation (Python)
- Complex table layouts

### 5.5 Technical Implementation Stack

```yaml
Primary Language: Python 3.9+

Core Libraries:
  - transformers: T5Gemma-2 model serving
  - torch: Deep learning framework
  - crawl4ai: Web scraping (LLM-ready)
  - aiohttp: Async HTTP requests

Data Processing:
  - pandas: Data manipulation
  - lxml: XML/HTML parsing
  - translate-toolkit: TMX file parsing
  - pyarrow: Parquet I/O

Alignment:
  - gaoisalign: Irish-English alignment
  - hunalign: Generic sentence alignment
  - nltk: Tokenization

Storage:
  - postgresql: Metadata + pgvector
  - lancedb: Vector storage
  - parquet: Columnar analytics

Orchestration:
  - agno: Agentic control plane
  - cocoindex: Incremental ETL
  - cognee: Knowledge graph
  - baml: Schema enforcement
```

### 5.6 Training Data Preparation

#### 5.6.1 Cocoindex Pipeline

```python
# Cocoindex flow for curriculum specifications
@flow
def curriculum_ingestion():
    # Source monitoring
    watch(Path("./specifications/*.pdf"))

    # Pre-processing
    pdf_to_images(dpi=300)
    extract_text_layer()

    # Cognitive step (Agno agents)
    layout_analysis(model="gemini-3-flash")
    ontology_mapping(schema="leaving_cert_ontology.baml")

    # Vector embedding
    generate_embeddings(model="all-MiniLM-L6-v2")

    # Persistence
    export_to_postgresql(with_pgvector=True)
```

#### 5.6.2 Cognee Knowledge Graph

Cross-lingual semantic mapping:

```
[Stoichiometry (EN)] --is_translation_of--> [Stocaimeadracht (GA)]
       |                                            |
   belongs_to                                   belongs_to
       |                                            |
       v                                            v
[Chemistry Strand 1]  <--is_translation_of--> [Snathe 1: Nadur an Abhair]
```

### 5.7 Serving Infrastructure: Transformers v5

#### 5.7.1 Continuous Batching

```python
# transformers serve for local T5Gemma-2
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
from transformers.serving import ModelServer

model = AutoModelForSeq2SeqLM.from_pretrained("google/t5gemma-2-4b")
tokenizer = AutoTokenizer.from_pretrained("google/t5gemma-2-4b")

server = ModelServer(
    model=model,
    tokenizer=tokenizer,
    continuous_batching=True,
    max_batch_size=32
)

server.start(port=8000)
```

**Performance Impact:**
- Up to **217% throughput increase** with continuous batching
- Eliminates network latency for agentic loops
- Enables parallel document processing

#### 5.7.2 Paged Attention

For 128K context windows:
- Non-contiguous KV-cache memory blocks
- Prevents OOM errors on long documents
- Essential for maintaining terminological consistency across 50+ page documents

---

## Appendices

### A. API Authentication

**Three methods for Gaois APIs:**

1. **HTTP Header:** `X-Api-Key: <API_KEY>`
2. **Query Parameter:** `?apiKey=<API_KEY>`
3. **HTTP Basic Auth:** `https://API_KEY@www.logainm.ie/...`

**Contact:** gaois@dcu.ie for API key requests

### B. Ethical Scraping Practices

1. **Respect robots.txt** - Check each domain
2. **Rate Limiting** - Max 1 request/second
3. **User-Agent** - Identify your scraper
4. **API First** - Always prefer official APIs
5. **Caching** - Store responses locally
6. **Attribution** - Credit Gaois, DCU

```bash
# robots.txt check
curl https://www.tearma.ie/robots.txt
curl https://www.logainm.ie/robots.txt
```

### C. Implementation Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| **Setup** | Week 1 | Register API keys, clone repos, set up environment |
| **API Collection** | Week 2-3 | Logainm, Duchas, TMX download |
| **Direct Downloads** | Week 3-4 | Corpas.ie lists, Tearma exports |
| **Web Scraping** | Week 4-6 | Ainm biographies, gap filling |
| **Processing** | Week 6-8 | Alignment, normalization, deduplication |
| **Publication** | Week 8-9 | HuggingFace export, dataset cards |

### D. Technical Contacts

- **Gaois Team:** gaois@dcu.ie
- **Developer Hub:** https://www.gaois.ie/en/technology/developers/
- **API Docs:** https://docs.gaois.ie/
- **GitHub:** https://github.com/gaois

---

**Document Version:** 1.0
**Sources Consolidated:**
- `/Users/cliste/dev/cianfhoghlaim/sruth/gaois/taighde/irish_bilingual_dataset_research.md`
- `/Users/cliste/dev/cianfhoghlaim/sruth/bun/taighde/Agentic Translation Workflow Technologies.md`
- `/Users/cliste/dev/cianfhoghlaim/sruth/bun/taighde/Neuro-Symbolic Translation Model Training.md`
