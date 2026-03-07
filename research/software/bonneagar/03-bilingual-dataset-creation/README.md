# Bilingual Dataset Creation

This directory contains research on creating high-quality Irish-English parallel corpora from multiple sources, with alignment tools and processing workflows.

## Overview

Creating bilingual datasets for Celtic languages requires combining multiple data sources with varying levels of quality and alignment. The primary goal is generating sentence-aligned parallel text suitable for machine translation training and evaluation.

### Dataset Size Estimates

| Source | Irish Words | English Words | Segments | Quality |
|--------|-------------|---------------|----------|---------|
| **Parallel Corpus (TMX)** | 68M | 62.5M | 130M | Excellent |
| **Duchas Folklore** | ~50M | ~30M | 80,000+ | Good |
| **Logainm Placenames** | - | - | 100,000+ | Excellent |
| **Tearma Terminology** | 100K+ | 100K+ | 10,000+ | Excellent |
| **Ainm Biographies** | 1.3M | - | 1,785 | Irish only |
| **Corpas.ie** | 240M | - | - | Monolingual |
| **Total Parallel** | **118M+** | **93M+** | **200K+ items** | Mixed |

## Documents in this Category

| Document | Focus | Key Topics |
|----------|-------|------------|
| `tmx-processing.md` | TMX file handling | Parsing, validation, export |
| `parallel-corpus-sources.md` | Source identification | Gaois, EU, government |
| `alignment-tools.md` | Text alignment | gaoisalign, hunalign |

## Primary Parallel Sources

### 1. Gaois Parallel Corpus (Highest Quality)

**URL:** https://www.gaois.ie/en/corpora/parallel/data

| Property | Value |
|----------|-------|
| **Format** | TMX (Translation Memory eXchange) |
| **Total Size** | ~130.5 million words |
| **Irish** | 68.0 million words |
| **English** | 62.5 million words |
| **Alignment** | Sentence-level |
| **License** | Open (verify specific terms) |

**Content Domains:**
- EU legislation (Regulations & Directives)
- Constitution of Ireland (1937)
- Acts of the Oireachtas (1922-2003+)
- Irish statutory instruments
- COVID-19 terminology

### 2. Duchas Folklore Collection

**API:** https://www.duchas.ie/api/v0.6

| Property | Value |
|----------|-------|
| **Irish Content** | ~66% |
| **English Content** | ~33% |
| **Items** | 80,000+ stories |
| **Alignment** | Metadata aligned, text requires processing |

**Collections:**
- Main Manuscript Collection (CBE): 2,400 volumes
- Schools' Collection (CBES): 740,000 pages
- Photographic Collection (CBEG): 80,000 photographs

### 3. Logainm Placenames

**API:** https://www.logainm.ie/api/v1.0

| Property | Value |
|----------|-------|
| **Entries** | 100,000+ |
| **Alignment** | Exact Irish-English pairs |
| **Metadata** | Geographic, historical variants |

### 4. Tearma Terminology

**URL:** https://www.tearma.ie/

| Property | Value |
|----------|-------|
| **Domains** | 40+ subject categories |
| **Alignment** | Term-level pairs |
| **Content** | Legal, medical, technical, EU |

## Data Quality Tiers

### Tier 1: Professional Translation (Highest)

- Gaois Parallel Corpus (TMX)
- EU official translations
- Government documents

**Characteristics:**
- Human translated
- Sentence-aligned
- Quality reviewed
- Consistent terminology

### Tier 2: Community/Editorial Content

- Duchas folklore (where aligned)
- Ainm.ie metadata
- Bilingual website content

**Characteristics:**
- Mixed translation quality
- Requires alignment
- May have style variations

### Tier 3: Machine-Assisted

- Back-translated content
- Web-scraped parallel pages
- Auto-aligned content

**Characteristics:**
- Requires quality filtering
- Higher noise ratio
- Useful for domain coverage

## Output Formats

### 1. JSON Lines (Streaming)

```json
{"id": 1, "irish": "Baile Átha Cliath", "english": "Dublin", "source": "logainm"}
{"id": 2, "irish": "Dia dhuit", "english": "Hello", "source": "tearma"}
```

### 2. TMX (Translation Memory)

```xml
<tu tuid="1">
  <tuv xml:lang="ga"><seg>Baile Átha Cliath</seg></tuv>
  <tuv xml:lang="en"><seg>Dublin</seg></tuv>
</tu>
```

### 3. Parquet (Analytics)

- Columnar storage
- Compressed (snappy/zstd)
- Schema-enforced
- Query-optimized

### 4. HuggingFace Datasets

```python
from datasets import Dataset
dataset = Dataset.from_dict({
    "irish": [...],
    "english": [...],
    "source": [...]
})
dataset.push_to_hub("gaois/irish-english-parallel")
```

## Processing Pipeline

```
Source Data
    |
    v
+-------------------+
|   Extraction      |  TMX parsing, API collection,
|                   |  web scraping
+--------+----------+
         |
         v
+-------------------+
|   Alignment       |  gaoisalign, hunalign,
|                   |  sentence splitting
+--------+----------+
         |
         v
+-------------------+
|   Normalization   |  UTF-8, orthography,
|                   |  deduplication
+--------+----------+
         |
         v
+-------------------+
|   Quality Filter  |  Length ratio, language ID,
|                   |  alignment score
+--------+----------+
         |
         v
+-------------------+
|   Export          |  JSONL, Parquet,
|                   |  HuggingFace
+-------------------+
```

## Quality Metrics

| Metric | Threshold | Purpose |
|--------|-----------|---------|
| **Length Ratio** | 0.5 - 2.0 | Filter misaligned |
| **Language ID Confidence** | >0.95 | Verify language |
| **Alignment Score** | >0.7 | Sentence correspondence |
| **Duplicate Ratio** | <5% | Deduplication check |

## Cross-References

- **Category 01 (Celtic Language AI)** - Models trained on this data
- **Category 02 (Data Acquisition)** - Collection pipelines
- Main research Category 03 (AI-Native Data Pipelines) - dlt patterns
