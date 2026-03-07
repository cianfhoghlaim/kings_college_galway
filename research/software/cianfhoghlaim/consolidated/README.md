# Gaeilge Research - Organized Collection

This directory contains consolidated and reorganized research on Celtic language technology, data acquisition, and educational context, with a primary focus on Irish (Gaeilge).

## Overview

The research covers seven interconnected categories spanning AI/ML resources, data pipelines, bilingual corpora, geospatial analysis, education policy, document processing, and technical implementation for Celtic languages across the British Isles.

### Research Scope

| Language | ISO Code | Coverage |
|----------|----------|----------|
| **Irish (Gaeilge)** | ga/gle | Primary focus |
| **Scottish Gaelic** | gd/gla | Comparative |
| **Welsh (Cymraeg)** | cy/cym | Comparative |
| **Manx (Gaelg)** | gv/glv | Reference |

## Categories

### 01. Celtic Language AI Resources

HuggingFace models, datasets, and research for Celtic language AI.

| Document | Description |
|----------|-------------|
| `README.md` | Category overview and maturity matrix |
| `irish-nlp-resources.md` | UCCIX, gaBERT, ASR models |
| `scottish-gaelic-resources.md` | GPT-2 WECHSEL, XLSum |
| `welsh-resources.md` | Mistral-7B-Cymraeg, techiaith ASR |
| `unified-model-comparison.md` | Cross-language model comparison |
| `bilingual-ml-architecture.md` | **NEW** Technical architecture for bilingual Irish/English education system |

**Key Resources:**
- UCCIX-Llama2-13B (Irish LLM)
- gaBERT (Irish encoder)
- wav2vec2-xlsr-ft-cy (Welsh ASR, 4.05% WER)
- MMS-TTS (Irish/Welsh)

### 02. Celtic Data Acquisition

API access, web scraping strategies, and data pipeline implementation.

| Document | Description |
|----------|-------------|
| `README.md` | Acquisition methods overview |
| `gaois-api-reference.md` | Logainm, Duchas, Tearma APIs |
| `pan-celtic-scraping.md` | Skyvern/crawl4ai strategies |
| `acquisition-pipeline.md` | dlt implementation patterns |
| `eu-irish-datasets.md` | **NEW** EU-funded Irish language datasets |
| `bilingual-scraper-implementation.md` | **NEW** Comprehensive scraper guide for 5 educational websites |

**Key Sources:**
- Gaois APIs (200M+ words)
- Parallel Corpus TMX (130.5M words)
- Corpas.ie (240M+ words)

### 03. Bilingual Dataset Creation

Parallel corpus processing, TMX handling, and alignment tools.

| Document | Description |
|----------|-------------|
| `README.md` | Dataset creation overview |
| `tmx-processing.md` | TMX parsing and validation |
| `parallel-corpus-sources.md` | Source catalog by quality tier |
| `alignment-tools.md` | gaoisalign, hunalign, vecalign |
| `education-subject-inventory.md` | **NEW** Comprehensive inventory of subjects across 3 websites |

**Dataset Estimates:**
- Irish parallel: 118M+ words
- English parallel: 93M+ words
- Total items: 200K+

### 04. Geospatial Linguistics

Mapping Gaeltacht areas, schools, and census data.

| Document | Description |
|----------|-------------|
| `README.md` | Geospatial analysis overview |
| `duckdb-spatial.md` | DuckDB spatial queries |
| `maplibre-visualization.md` | Web mapping implementation |
| `data-sources.md` | Official boundary/census data |

**Technical Stack:**
- DuckDB + spatial extension
- MapLibre GL JS
- tippecanoe (vector tiles)
- dltHub (ingestion)

### 05. Education Policy Context

Celtic language education statistics and policy frameworks.

| Document | Description |
|----------|-------------|
| `README.md` | Policy overview |
| `enrollment-statistics.md` | Pupil numbers across jurisdictions |
| `policy-frameworks.md` | Cymraeg 2050, Identity Act, etc. |
| `teacher-supply.md` | Workforce crisis analysis |

**Key Metrics:**
- ~196,400 pupils in Celtic-medium education
- Wales: 21% in Welsh-medium
- NI: +50% growth in decade
- Teacher shortage: Critical across all jurisdictions

### 06. Document Processing

OCR, VLM, and PDF extraction for Celtic language historical documents.

| Document | Description |
|----------|-------------|
| `README.md` | Document processing overview |
| `Celtic Language OCR Resource Analysis.md` | OCR tools for Celtic scripts |
| `Open-Source VLMs For PDF Extraction.md` | VLM-based extraction strategies |

**Key Tools:**
- Tesseract with Irish/Welsh packs
- LLaVA/Donut for VLM
- PyMuPDF/pdfplumber

### 07. Technical Implementation

Pipeline architecture, anti-bot strategies, and data source management.

| Document | Description |
|----------|-------------|
| `README.md` | Implementation overview |
| `Managing Diverse Data Sources for Pipelines.md` | Multi-source pipeline patterns |
| `Open-Source Crawl4ai Anti-Bot Stack.md` | Anti-detection strategies |

**Key Patterns:**
- DLT source configuration
- Rate limiting strategies
- Browser fingerprinting mitigation

## Quick Reference

### Best-in-Class Models

| Task | Language | Model |
|------|----------|-------|
| **LLM** | Irish | UCCIX-Llama2-13B-Instruct |
| **LLM** | Welsh | Mistral-7B-Cymraeg-Welsh-v2 |
| **Encoder** | Irish | gaBERT |
| **ASR** | Welsh | wav2vec2-xlsr-ft-cy |
| **ASR** | Irish | wav2vec2-large-xlsr-53-irish |
| **TTS** | Irish | facebook/mms-tts-gle |
| **Translation** | All | Helsinki-NLP/opus-mt-* |

### Primary Data Sources

| Source | Content | Method |
|--------|---------|--------|
| Gaois APIs | Placenames, folklore | API |
| Parallel Corpus | Legal translations | TMX download |
| Corpas.ie | Monolingual Irish | Download |
| Tearma.ie | Terminology | API/Scrape |

### Technical Stack

```yaml
Data Pipeline:
  - dltHub (ingestion)
  - DuckDB (storage/spatial)
  - Parquet (format)

Scraping:
  - crawl4ai (LLM-ready)
  - Skyvern (agentic)

Visualization:
  - MapLibre GL JS
  - tippecanoe (tiles)

ML/NLP:
  - HuggingFace Transformers
  - sentence-transformers
```

## Cross-References to Main Research

This gaeilge-specific research connects to the main research categories:

| Main Category | Connection |
|---------------|------------|
| **01. Celtic Language AI** | Uses models from main Category 01 |
| **02. Multimodal Intelligence** | OCR/VLM for historical documents |
| **03. AI-Native Pipelines** | dlt patterns from main Category 03 |
| **04. Stealth Browser Stack** | crawl4ai/Skyvern from main Category 04 |
| **05. Model Deployment** | Deployment patterns apply to Celtic models |

## Source File Mapping

Original files consolidated into this structure:

| Original File | Destination Category |
|---------------|---------------------|
| `irish_gaeilge_huggingface_resources.md` | 01 |
| `scottish_gaelic_huggingface_resources.md` | 01 |
| `welsh-huggingface-resources.md` | 01 |
| `CELTIC_LANGUAGES_AI_RESOURCES.md` | 01 |
| `irish_bilingual_dataset_research.md` | 02, 03 |
| `Celtic Data Scraping and Integration Plan.md` | 02 |
| `Crawl4ai Scraping and Site Analysis.md` | 02 |
| `gaeilge.md` | 04 |
| `Geospatial Data Analysis and DuckDB.md` | 04 |
| `British Isles Celtic Language Education Data.md` | 05 |
| `uk_education_datasets_analysis.md` | 05 |
| `british_isles_parallel_data_sources.md` | 05 |

## Getting Started

### For AI/ML Development

1. Start with `01-celtic-language-ai-resources/unified-model-comparison.md`
2. Review language-specific resources for your target language
3. Check dataset availability in `03-bilingual-dataset-creation/`

### For Data Collection

1. Review `02-celtic-data-acquisition/README.md` for priority order
2. Register for Gaois API key (gaois@dcu.ie)
3. Implement pipeline from `acquisition-pipeline.md`

### For Visualization

1. Download boundaries from `04-geospatial-linguistics/data-sources.md`
2. Set up DuckDB with spatial extension
3. Follow MapLibre patterns in `maplibre-visualization.md`

### For Policy Context

1. Read `05-education-policy-context/README.md` for overview
2. Check enrollment data for specific jurisdictions
3. Understand teacher supply constraints

## Repository Structure

```
organized/
├── README.md                          # This file
├── 01-celtic-language-ai-resources/
│   ├── README.md
│   ├── irish-nlp-resources.md
│   ├── scottish-gaelic-resources.md
│   ├── welsh-resources.md
│   └── unified-model-comparison.md
├── 02-celtic-data-acquisition/
│   ├── README.md
│   ├── gaois-api-reference.md
│   ├── pan-celtic-scraping.md
│   └── acquisition-pipeline.md
├── 03-bilingual-dataset-creation/
│   ├── README.md
│   ├── tmx-processing.md
│   ├── parallel-corpus-sources.md
│   └── alignment-tools.md
├── 04-geospatial-linguistics/
│   ├── README.md
│   ├── duckdb-spatial.md
│   ├── maplibre-visualization.md
│   └── data-sources.md
└── 05-education-policy-context/
    ├── README.md
    ├── enrollment-statistics.md
    ├── policy-frameworks.md
    └── teacher-supply.md
```

## Contributing

When adding new research:
1. Determine the appropriate category
2. Follow the established document structure
3. Include cross-references to related documents
4. Update this README if adding new documents

## References

- Gaois Research Group: https://www.gaois.ie
- HuggingFace: https://huggingface.co
- dltHub: https://dlthub.com
- DuckDB: https://duckdb.org
- MapLibre: https://maplibre.org
