# EU Datasets and Resources for Irish Language (Gaeilge)

**Comprehensive Guide to European Union Bilingual Datasets**
**Date:** 2025-11-17
**Focus:** Irish Language Official Status and Bilingual Dataset Creation

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Irish Language Official Status in the EU](#irish-language-official-status-in-the-eu)
3. [Major EU Institutions with Irish Resources](#major-eu-institutions-with-irish-resources)
4. [Available Datasets and Resources](#available-datasets-and-resources)
5. [Creating Bilingual Datasets](#creating-bilingual-datasets)
6. [Technical Implementation Guide](#technical-implementation-guide)
7. [Dataset Quality and Characteristics](#dataset-quality-and-characteristics)
8. [Legal and Licensing Considerations](#legal-and-licensing-considerations)
9. [Research Applications](#research-applications)
10. [References and Further Resources](#references-and-further-resources)

---

## Executive Summary

Irish (Gaeilge) became the 24th official language of the European Union on 1 January 2007, and achieved full working language status on 1 January 2022. This unique position makes EU institutions a critical source of high-quality, professionally translated Irish-English parallel text data.

### Key Statistics

- **Official Language Status:** Full EU official and working language since 2022
- **Translation Volume:** Thousands of documents annually across all EU institutions
- **Domain Coverage:** Legal, administrative, technical, political, economic
- **Quality Level:** Professional human translations by EU translation services
- **Accessibility:** Most documents publicly available under open licenses

### Why EU Irish Datasets Matter

1. **Professional Quality:** All translations by qualified EU translators
2. **Domain Diversity:** Coverage across legal, technical, scientific, and administrative domains
3. **Standardized Terminology:** Consistent use of official Irish terminology
4. **Parallel Alignment:** Documents available in Irish and English (plus other EU languages)
5. **Legal Authority:** Official EU documents representing authoritative language usage
6. **Open Access:** Most materials available for research and AI training purposes

---

## Irish Language Official Status in the EU

### Timeline

- **2005:** Treaty of Accession recognizes Irish as an official language (limited working status)
- **1 January 2007:** Irish becomes 24th official EU language
- **2007-2021:** "Derogation period" - limited translation requirements
- **1 January 2022:** Full working language status - all EU legislation must be translated to Irish
- **2022-Present:** Full parity with other EU languages

### Legal Framework

**Treaty on European Union (TEU), Article 55(1):**
> "This Treaty is drawn up in a single original in the Bulgarian, Croatian, Czech, Danish, Dutch, English, Estonian, Finnish, French, **German, Greek, Hungarian, Irish**, Italian, Latvian, Lithuanian, Maltese, Polish, Portuguese, Romanian, Slovak, Slovenian, Spanish and Swedish languages..."

**Regulation No 1/1958 (as amended):**
Determines the languages to be used by the European Economic Community, with Irish added in 2007 and extended to full working status in 2022.

### Implications for Datasets

1. **Legal Requirement:** All EU regulations, directives, and official documents must be available in Irish
2. **Translation Infrastructure:** Dedicated Irish language units in EU institutions
3. **Terminology Management:** IATE (Interactive Terminology for Europe) includes comprehensive Irish terminology
4. **Quality Assurance:** Professional translation standards applied consistently
5. **Public Availability:** Most documents published on EUR-Lex and institutional portals

---

## Major EU Institutions with Irish Resources

### 1. EUR-Lex - EU Law Portal

**Website:** https://eur-lex.europa.eu

**Description:**
EUR-Lex provides free access to EU law and other public documents in all official languages, including Irish. It is the primary repository for all EU legal texts.

**Available Content:**
- EU treaties
- EU legislation (regulations, directives, decisions)
- Preparatory documents
- Case law (Court of Justice)
- International agreements
- Consolidated legislation
- National implementation measures

**Irish Language Coverage:**
- All binding legislation since 2007
- Full translation of all new legislation since 2022
- Searchable in Irish language
- Parallel text viewing available

**Dataset Potential:**
- **Volume:** Tens of thousands of documents
- **Domain:** Legal, regulatory
- **Format:** HTML, PDF, XHTML, XML (Formex)
- **Alignment:** Paragraph-level alignment available through CELEX numbers
- **License:** Public sector information reuse allowed under specific conditions

**Access Methods:**
- Web interface: https://eur-lex.europa.eu
- Web services: SOAP and REST APIs available
- SPARQL endpoint: https://publications.europa.eu/webapi/rdf/sparql
- Bulk download: Available through EU Publications Office

**Technical Details:**
```
CELEX Number Structure:
- 32023R1234 = Regulation 1234 from 2023
- Same CELEX across all language versions
- Enables perfect document alignment
```

### 2. European Parliament

**Website:** https://www.europarl.europa.eu

**Description:**
The European Parliament publishes extensive documentation in Irish, including debates, reports, resolutions, and parliamentary questions.

**Available Content:**
- Plenary debates (verbatim)
- Committee reports
- Parliamentary questions
- Resolutions
- Legislative documents
- Press releases
- Informational materials

**Irish Language Resources:**
- **Europarl Corpus:** One of the most widely used parallel corpora
- **Debates:** Transcripts of plenary sessions
- **Documents:** Reports, resolutions, working documents
- **Website Content:** Institutional information in Irish

**Dataset Potential:**
- **Volume:** Millions of words in parallel text
- **Domain:** Political, legislative, current affairs
- **Format:** XML, HTML, PDF
- **Update Frequency:** Daily for new proceedings
- **Temporal Coverage:** 2007-present

**Notable Resource: Europarl Parallel Corpus**
- One of the largest parallel corpora
- Versions 7-10 include Irish
- Used extensively in MT research
- Available through OPUS (see below)

### 3. European Commission

**Website:** https://ec.europa.eu

**Description:**
The Commission is the EU's executive arm and produces vast amounts of documentation in Irish.

**Available Content:**
- Legislative proposals
- Communications
- Reports and studies
- Press releases
- Policy documents
- Public consultations
- Funding program documentation

**Irish Language Services:**
- Directorate-General for Translation (DGT)
- Irish Language Unit
- Term coordination service
- Translation memory databases

**Dataset Potential:**
- **Volume:** Extensive (largest EU institution)
- **Domain:** All policy areas (environment, trade, digital, agriculture, etc.)
- **Quality:** Professional translation with QA
- **Terminology:** IATE database integration

### 4. DGT-Translation Memory (DGT-TM)

**Website:** https://joint-research-centre.ec.europa.eu/language-technology-resources/dgt-translation-memory_en

**Description:**
The European Commission's Directorate-General for Translation provides translation memories covering 24 EU languages.

**Dataset Details:**
- **Format:** TMX (Translation Memory eXchange)
- **Coverage:** All language pairs (Irish-English included)
- **Size:** Millions of sentence pairs
- **Quality:** Professional human translations
- **Domain:** European Commission documents
- **License:** CC-BY 4.0 (recent versions)

**Irish-English Statistics (approximate):**
- Sentence pairs: ~1-3 million (varies by release)
- Unique segments: High-quality aligned sentences
- Update frequency: Annual releases

**Access:**
- Direct download from JRC website
- Available through OPUS corpus
- TMX format enables easy processing

**Use Cases:**
- Training machine translation systems
- Terminology extraction
- Bilingual dictionary creation
- Translation quality assessment

### 5. IATE - Interactive Terminology for Europe

**Website:** https://iate.europa.eu

**Description:**
IATE is the EU's official terminology database, containing approximately 8.8 million terms in 24 languages.

**Irish Language Content:**
- ~200,000+ Irish language terms
- Technical terminology across all EU domains
- Context examples
- Definitions in Irish
- Cross-references to EU legislation

**Dataset Characteristics:**
- **Format:** Searchable database, XML/TBX export available
- **Domains:** All EU policy areas
- **Quality:** Validated by EU terminology experts
- **Reliability:** Official EU terminology
- **Updates:** Continuous

**Access Methods:**
- Web interface: Search and browse
- API access: Available for institutional users
- Downloads: Partial datasets available
- Integration: Can be integrated into CAT tools

**Dataset Potential:**
- Bilingual terminology database
- Domain-specific glossaries
- Technical vocabulary
- Context-rich examples

### 6. EU Open Data Portal

**Website:** https://data.europa.eu

**Description:**
The official portal for European data, providing access to datasets from EU institutions and bodies.

**Irish Language Datasets:**
- Statistical data with Irish labels
- Geospatial data
- Environmental data
- Economic indicators
- Social indicators

**Notable Features:**
- Metadata often available in Irish
- Some datasets fully translated
- RDF/linked data format
- SPARQL endpoint access

**Access:**
- Download datasets directly
- API access available
- CKAN-based platform
- Metadata in multiple languages

### 7. Translation Centre for the Bodies of the European Union (CdT)

**Website:** https://cdt.europa.eu

**Description:**
The Translation Centre provides translation services for EU agencies and bodies.

**Irish Language Services:**
- Translation of agency documents
- Pharmaceutical terminology
- Technical documentation
- Legal texts

**Dataset Potential:**
- Specialized domain translations
- Agency-specific terminology
- Technical/scientific content

### 8. Turas - An Caighdeán Oifigiúil (The Official Standard)

**Website:** https://www.turas.tv / https://www.tearma.ie

**Description:**
While primarily Irish government resources, these work closely with EU institutions to standardize Irish terminology.

**Resources:**
- **Tearma.ie:** National terminology database (includes EU terms)
- **Focal.ie:** Irish-English dictionary with EU legal terms
- **An Caighdeán Oifigiúil:** Official Irish language standard

**EU Connection:**
- Coordination with IATE
- EU terminology integration
- Official terminology source for EU Irish translations

### 9. European Court of Justice (CJEU)

**Website:** https://curia.europa.eu

**Description:**
Court judgments and legal documents in all EU languages.

**Irish Language Content:**
- Selected judgments in Irish
- Case law summaries
- Legal terminology
- Procedural documents

**Dataset Potential:**
- Legal domain specialization
- High-quality legal translations
- Structured legal reasoning
- Citations and references

### 10. Publications Office of the European Union

**Website:** https://op.europa.eu

**Description:**
The official publisher of EU institutions, providing access to all EU publications.

**Irish Language Publications:**
- Official Journal (OJ) in Irish
- Books and brochures
- Statistical publications (Eurostat)
- Research publications
- Educational materials

**Dataset Features:**
- Metadata in Irish
- Full-text publications
- Structured data
- Multiple format options (PDF, HTML, XML, ePub)

**Notable Resource: EU Vocabularies**
- **Website:** https://op.europa.eu/en/web/eu-vocabularies
- Controlled vocabularies in all EU languages including Irish
- EuroVoc thesaurus with Irish terms
- Authority tables
- RDF/SKOS format

---

## Available Datasets and Resources

### 1. OPUS Corpus Collection

**Website:** https://opus.nlpl.eu

**Description:**
OPUS (Open Parallel Corpus) is the largest collection of parallel corpora, including extensive Irish-English resources from EU sources.

**Irish-English EU Datasets Available:**

#### DGT-Translation Memory
- **Pairs:** ~1-3 million sentence pairs
- **Source:** European Commission
- **Domain:** Legal, administrative
- **Format:** Moses, TMX, XML
- **License:** CC-BY 4.0

#### Europarl (European Parliament Proceedings)
- **Version:** v7, v8, v9, v10
- **Pairs:** ~700,000+ sentence pairs (varies by version)
- **Source:** European Parliament debates
- **Domain:** Political, legislative
- **Temporal:** 2007-present
- **Format:** Moses, TMX, XML

#### JRC-Acquis
- **Pairs:** ~1 million+ sentence pairs
- **Source:** EU legislation (acquis communautaire)
- **Domain:** Legal
- **Format:** Moses, TMX, XML
- **Note:** Older dataset, primarily 2007-2012 material

#### EU Bookshop
- **Pairs:** Smaller dataset
- **Source:** EU publications
- **Domain:** Various (books, reports, brochures)

#### ELRC-CORDIS (Research Corpus)
- **Source:** EU research project descriptions
- **Domain:** Scientific, technical, research
- **Quality:** Professional translations

**Access Methods:**
```python
# Using OpusTools
from opustools import OpusRead

# Download DGT Irish-English
opus_reader = OpusRead(
    directory="dgt",
    source="en",
    target="ga",
    release="latest"
)

# Download Europarl Irish-English
opus_reader = OpusRead(
    directory="Europarl",
    source="en",
    target="ga",
    release="v10"
)
```

### 2. European Language Resource Coordination (ELRC-SHARE)

**Website:** https://elrc-share.eu

**Description:**
Repository of language resources for EU languages, with focus on under-resourced languages including Irish.

**Irish Language Resources:**
- EU institutional texts
- Public sector documents
- Monolingual and parallel corpora
- Terminology resources
- Language models and tools

**Notable Irish Datasets:**
- ELRC-CORDIS: Research project descriptions
- ELRC-EC_EUROPA: European Commission website content
- ELRC-EUR_LEX: Legal texts from EUR-Lex
- National datasets from Irish public sector

### 3. Common Crawl (EU Domains)

**Website:** https://commoncrawl.org

**Description:**
While not EU-specific, Common Crawl includes extensive scraping of EU websites with Irish content.

**Filtering for EU Irish Content:**
```python
# Example domains to filter for Irish EU content
eu_irish_domains = [
    "*.europa.eu/ga/",
    "*.europarl.europa.eu/ga/",
    "*.ec.europa.eu/*/ga",
    "eur-lex.europa.eu/*/ga/*"
]
```

**Datasets:**
- CC-100: Includes Irish segment (108M tokens)
- OSCAR: Irish corpus from CommonCrawl
- CulturaX: Multilingual dataset including Irish

### 4. European Parliament Proceedings Parallel Corpus (Europarl)

**Original Source:** https://www.statmt.org/europarl/
**OPUS Version:** https://opus.nlpl.eu/Europarl.php

**Details:**
- **Version 10:** Most recent, includes Irish
- **Time Period:** 2007-2012 for Irish (expanding)
- **Size:** ~700,000 sentence pairs (Irish-English)
- **Format:** Parallel text files, TMX, Moses format
- **Quality:** High - official parliamentary records
- **Use Cases:** MT training, political domain NLP, discourse analysis

**Statistics (Irish-English, Europarl v10):**
- Sentences: ~700,000
- Words (Irish): ~15-20 million
- Words (English): ~18-23 million
- Files: Aligned by date and session

### 5. JRC-Acquis Communautaire

**Source:** European Commission Joint Research Centre

**Description:**
Collection of legislative texts (acquis communautaire) in 22-24 languages.

**Irish Content:**
- EU legislation from 2007 onwards
- Aligned at sentence level
- Legal domain focus
- Consistent terminology

**Characteristics:**
- **Size:** ~1 million+ sentence pairs (Irish-English)
- **Domain:** Legal, regulatory
- **Quality:** Professional legal translations
- **Format:** TMX, XML, Moses
- **License:** Research-friendly

### 6. MultiUN and Other Multilingual Corpora

**Note:** While not EU-specific, UN parallel corpora sometimes include Irish translations of international documents that overlap with EU policy areas.

### 7. EU Data Portal Datasets

**Specific Irish-Language Datasets:**

#### Eurostat Data with Irish Labels
- Statistical indicators
- Economic data
- Social indicators
- Environmental metrics
- Metadata in Irish

#### Geospatial Data (INSPIRE Directive)
- Place names in Irish (where applicable)
- Administrative boundaries
- Environmental zones

#### Open Government Data
- Some EU agencies provide Irish metadata
- Cultural datasets (Europeana)

### 8. WikiMatrix and Wikipedia-Based Resources

**WikiMatrix:**
- Mined parallel sentences from Wikipedia
- Irish-English pairs
- EU-related articles often translated

**DBpedia (Irish):**
- Structured data from Irish Wikipedia
- EU institutions and concepts
- Linked data format

### 9. News Datasets

#### NewsCommentary
- Available through OPUS
- News translations including EU topics
- May contain Irish-English pairs

#### Global Voices
- Citizen journalism platform
- Some Irish translations
- EU news coverage

### 10. Research Project Datasets

#### UCCIX Project Resources
- **Organization:** ReliableAI / ReML-AI
- **Resources:** Irish-English Parallel Collection on HuggingFace
- **URL:** https://huggingface.co/datasets/ReliableAI/Irish-English-Parallel-Collection
- **Content:** May include EU documents among other sources

---

## Creating Bilingual Datasets

### Strategy 1: Web Scraping EU Websites

#### Target Websites

**EUR-Lex:**
```python
# Example URL pattern
base_url = "https://eur-lex.europa.eu/legal-content/EN-GA/TXT/"
celex_number = "32023R1234"  # Example CELEX number

# English version
url_en = f"https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:{celex_number}"

# Irish version
url_ga = f"https://eur-lex.europa.eu/legal-content/GA/TXT/?uri=CELEX:{celex_number}"
```

**European Parliament:**
```python
# Debate URLs
debate_id = "2023-10-17"
url_en = f"https://www.europarl.europa.eu/doceo/document/CRE-9-{debate_id}_EN.html"
url_ga = f"https://www.europarl.europa.eu/doceo/document/CRE-9-{debate_id}_GA.html"
```

#### Tools and Libraries

**Web Scraping:**
```python
import requests
from bs4 import BeautifulSoup
import trafilatura

# Using trafilatura for clean text extraction
def fetch_eu_document(url):
    downloaded = trafilatura.fetch_url(url)
    text = trafilatura.extract(downloaded)
    return text

# Scrape parallel documents
en_text = fetch_eu_document(url_en)
ga_text = fetch_eu_document(url_ga)
```

**Crawl4AI Integration:**
```python
from crawl4ai import WebCrawler

crawler = WebCrawler()

# Crawl EUR-Lex with Irish filter
result = crawler.run(
    url="https://eur-lex.europa.eu",
    word_count_threshold=10,
    extraction_strategy="LLMExtractionStrategy",
    chunking_strategy={"type": "semantic"}
)
```

### Strategy 2: Using OPUS API

**OpusTools Python Package:**
```python
from opustools import OpusRead, OpusGet

# Download DGT corpus
opus_get = OpusGet(
    source="en",
    target="ga",
    directory="DGT",
    release="latest"
)
opus_get.get_files()

# Read and process
opus_read = OpusRead(
    directory="DGT",
    source="en",
    target="ga",
    release="latest",
    write_mode="moses",
    write=["source.en", "target.ga"]
)
opus_read.printPairs()
```

**OPUS-API Direct Access:**
```python
import requests

# Get corpus information
corpus_info = requests.get(
    "https://opus.nlpl.eu/opusapi/?corpus=DGT&source=en&target=ga"
)

# Download TMX file
tmx_url = corpus_info.json()['corpora'][0]['url']
tmx_data = requests.get(tmx_url)
```

### Strategy 3: EUR-Lex Web Services

**SOAP API Example:**
```python
from zeep import Client

# EUR-Lex SOAP endpoint
wsdl = "https://eur-lex.europa.eu/EURLexWebService?wsdl"
client = Client(wsdl=wsdl)

# Search for documents in Irish
result = client.service.searchEURLex({
    'expertQuery': 'DD_LANGUE=GA',
    'page': 1,
    'pageSize': 100
})
```

**SPARQL Query Example:**
```sparql
PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>

SELECT ?work ?titleEN ?titleGA
WHERE {
  ?work cdm:work_has_expression ?exprEN, ?exprGA .
  ?exprEN cdm:expression_uses_language <http://publications.europa.eu/resource/authority/language/ENG> ;
          cdm:expression_title ?titleEN .
  ?exprGA cdm:expression_uses_language <http://publications.europa.eu/resource/authority/language/GLE> ;
          cdm:expression_title ?titleGA .
}
LIMIT 1000
```

### Strategy 4: Translation Memory Processing

**TMX File Processing:**
```python
from lxml import etree

def parse_tmx(tmx_file, source_lang="en", target_lang="ga"):
    """
    Parse TMX file and extract parallel segments
    """
    tree = etree.parse(tmx_file)
    root = tree.getroot()

    pairs = []

    for tu in root.findall('.//tu'):
        segments = {}
        for tuv in tu.findall('tuv'):
            lang = tuv.get('{http://www.w3.org/XML/1998/namespace}lang')
            seg = tuv.find('seg')
            if seg is not None and seg.text:
                segments[lang.lower()] = seg.text

        if source_lang in segments and target_lang in segments:
            pairs.append({
                'source': segments[source_lang],
                'target': segments[target_lang]
            })

    return pairs

# Usage
dgt_pairs = parse_tmx("DGT-TM-en-ga.tmx")
print(f"Extracted {len(dgt_pairs)} sentence pairs")
```

### Strategy 5: Document Alignment

**Sentence Alignment with Hunalign:**
```python
import subprocess

def align_documents(source_file, target_file, output_file):
    """
    Align parallel documents at sentence level using hunalign
    """
    cmd = [
        'hunalign',
        '-text',
        '-utf',
        'null.dic',  # No dictionary (use only length-based alignment)
        source_file,
        target_file,
        '-realign'
    ]

    with open(output_file, 'w') as out:
        subprocess.run(cmd, stdout=out)

    return output_file

# Align EUR-Lex documents
align_documents('document.en', 'document.ga', 'aligned.txt')
```

**Using NLTK for Paragraph Alignment:**
```python
import nltk
from nltk.tokenize import sent_tokenize

def paragraph_align(en_text, ga_text):
    """
    Simple paragraph-level alignment
    Assumes parallel structure
    """
    en_paragraphs = en_text.split('\n\n')
    ga_paragraphs = ga_text.split('\n\n')

    # Filter empty paragraphs
    en_paragraphs = [p.strip() for p in en_paragraphs if p.strip()]
    ga_paragraphs = [p.strip() for p in ga_paragraphs if p.strip()]

    # Align if same count
    if len(en_paragraphs) == len(ga_paragraphs):
        return list(zip(en_paragraphs, ga_paragraphs))
    else:
        # Use more sophisticated alignment (e.g., vecalign, bertalign)
        return None
```

### Strategy 6: Using HuggingFace Datasets

**Loading Existing Datasets:**
```python
from datasets import load_dataset

# Load Europarl from HuggingFace (if available)
# Note: May need to load via OPUS or convert yourself
dataset = load_dataset("opus_europarl", "en-ga")

# Or load from custom source
from datasets import Dataset

# Create dataset from scraped data
data = {
    'english': en_sentences,
    'irish': ga_sentences,
    'source': ['EUR-Lex'] * len(en_sentences)
}

dataset = Dataset.from_dict(data)

# Save to disk
dataset.save_to_disk('eu_irish_parallel')

# Push to HuggingFace Hub
dataset.push_to_hub("your-username/eu-irish-parallel")
```

### Strategy 7: Quality Control and Filtering

**Deduplication:**
```python
def deduplicate_pairs(pairs):
    """
    Remove duplicate sentence pairs
    """
    seen = set()
    unique_pairs = []

    for pair in pairs:
        key = (pair['source'], pair['target'])
        if key not in seen:
            seen.add(key)
            unique_pairs.append(pair)

    return unique_pairs
```

**Quality Filtering:**
```python
import langdetect
from langdetect import detect

def filter_quality(pairs, min_length=10, max_ratio=3.0):
    """
    Filter pairs based on quality criteria
    """
    filtered = []

    for pair in pairs:
        src = pair['source']
        tgt = pair['target']

        # Length checks
        if len(src) < min_length or len(tgt) < min_length:
            continue

        # Ratio check (avoid very unbalanced pairs)
        ratio = len(src) / len(tgt)
        if ratio > max_ratio or ratio < (1/max_ratio):
            continue

        # Language detection
        try:
            src_lang = detect(src)
            tgt_lang = detect(tgt)

            if src_lang != 'en' or tgt_lang != 'ga':
                continue
        except:
            continue

        filtered.append(pair)

    return filtered
```

**Alignment Quality Scoring:**
```python
from sentence_transformers import SentenceTransformer, util
from transformers import MarianMTModel, MarianTokenizer

def score_alignment_quality(pairs, model_name='stsb-xlm-r-multilingual'):
    """
    Score alignment quality using cross-lingual embeddings
    """
    model = SentenceTransformer(model_name)

    scored_pairs = []

    for pair in pairs:
        # Encode both sentences
        emb_src = model.encode(pair['source'], convert_to_tensor=True)
        emb_tgt = model.encode(pair['target'], convert_to_tensor=True)

        # Calculate cosine similarity
        similarity = util.cos_sim(emb_src, emb_tgt).item()

        scored_pairs.append({
            **pair,
            'alignment_score': similarity
        })

    return scored_pairs

# Filter by threshold
high_quality = [p for p in scored_pairs if p['alignment_score'] > 0.7]
```

### Strategy 8: Terminology Extraction

**Extract Terminology from IATE:**
```python
import requests
from bs4 import BeautifulSoup

def extract_iate_terms(domain=None):
    """
    Extract terminology from IATE
    Note: This is illustrative - actual implementation needs IATE API access
    """
    # IATE search API (requires authentication for bulk access)
    base_url = "https://iate.europa.eu/search"

    params = {
        'language': 'ga',
        'domain': domain  # e.g., 'law', 'economics'
    }

    # Implement actual API calls based on IATE documentation
    # This is a placeholder

    terms = []
    # Extract and structure terminology

    return terms
```

**Domain-Specific Glossary Creation:**
```python
def create_domain_glossary(parallel_corpus, domain='legal'):
    """
    Create bilingual glossary from parallel corpus
    """
    from collections import Counter
    import re

    # Extract noun phrases and technical terms
    # This is simplified - use actual NLP tools for better results

    en_terms = Counter()
    ga_terms = Counter()

    for pair in parallel_corpus:
        # Extract terms (simplified)
        en_candidates = re.findall(r'\b[A-Z][a-z]+(?:\s+[A-Z][a-z]+)*\b', pair['source'])
        ga_candidates = re.findall(r'\b[A-ZÁÉÍÓÚ][a-záéíóú]+(?:\s+[a-záéíóú]+)*\b', pair['target'])

        en_terms.update(en_candidates)
        ga_terms.update(ga_candidates)

    # Create alignment between frequent terms
    glossary = []
    # Implement term alignment logic

    return glossary
```

### Strategy 9: Creating Training Datasets

**Dataset Splits:**
```python
from sklearn.model_selection import train_test_split

def create_splits(pairs, train_ratio=0.9, val_ratio=0.05, test_ratio=0.05):
    """
    Create train/validation/test splits
    """
    # First split: train and temp
    train, temp = train_test_split(
        pairs,
        train_size=train_ratio,
        random_state=42
    )

    # Second split: validation and test
    val_size = val_ratio / (val_ratio + test_ratio)
    val, test = train_test_split(
        temp,
        train_size=val_size,
        random_state=42
    )

    return {
        'train': train,
        'validation': val,
        'test': test
    }

# Usage
splits = create_splits(filtered_pairs)
print(f"Train: {len(splits['train'])}")
print(f"Validation: {len(splits['validation'])}")
print(f"Test: {len(splits['test'])}")
```

**Export in Multiple Formats:**
```python
import json
import csv

def export_dataset(pairs, prefix='eu_irish'):
    """
    Export dataset in multiple formats
    """
    # Moses format (separate files)
    with open(f'{prefix}.en', 'w') as f_en, \
         open(f'{prefix}.ga', 'w') as f_ga:
        for pair in pairs:
            f_en.write(pair['source'] + '\n')
            f_ga.write(pair['target'] + '\n')

    # JSON format
    with open(f'{prefix}.json', 'w') as f:
        json.dump(pairs, f, ensure_ascii=False, indent=2)

    # CSV format
    with open(f'{prefix}.csv', 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['source', 'target'])
        writer.writeheader()
        writer.writerows(pairs)

    # HuggingFace Dataset
    from datasets import Dataset
    dataset = Dataset.from_dict({
        'translation': [
            {'en': p['source'], 'ga': p['target']}
            for p in pairs
        ]
    })
    dataset.save_to_disk(f'{prefix}_hf')
```

---

## Technical Implementation Guide

### Complete Pipeline Example

```python
#!/usr/bin/env python3
"""
EU Irish-English Bilingual Dataset Creation Pipeline
"""

import requests
from bs4 import BeautifulSoup
from lxml import etree
from datasets import Dataset
import langdetect
from tqdm import tqdm
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class EUIrishDatasetBuilder:
    """
    Build Irish-English parallel datasets from EU sources
    """

    def __init__(self):
        self.pairs = []

    def fetch_eurolex_documents(self, limit=100):
        """
        Fetch documents from EUR-Lex
        """
        logger.info("Fetching EUR-Lex documents...")

        # Example: Fetch regulations from 2023
        # In practice, use EUR-Lex API or SPARQL

        # Placeholder - implement actual EUR-Lex scraping
        celex_numbers = self._get_celex_numbers(limit)

        for celex in tqdm(celex_numbers):
            try:
                en_text = self._fetch_eurlex_text(celex, 'EN')
                ga_text = self._fetch_eurlex_text(celex, 'GA')

                if en_text and ga_text:
                    pairs = self._align_paragraphs(en_text, ga_text)
                    self.pairs.extend(pairs)
            except Exception as e:
                logger.error(f"Error processing {celex}: {e}")

        logger.info(f"Fetched {len(self.pairs)} pairs from EUR-Lex")

    def load_opus_datasets(self):
        """
        Load datasets from OPUS
        """
        logger.info("Loading OPUS datasets...")

        from opustools import OpusRead

        # DGT corpus
        dgt_reader = OpusRead(
            directory="DGT",
            source="en",
            target="ga",
            release="latest"
        )

        for src, tgt, meta in dgt_reader.get_all_pairs():
            self.pairs.append({
                'source': src,
                'target': tgt,
                'dataset': 'DGT',
                'metadata': meta
            })

        # Europarl corpus
        europarl_reader = OpusRead(
            directory="Europarl",
            source="en",
            target="ga",
            release="v10"
        )

        for src, tgt, meta in europarl_reader.get_all_pairs():
            self.pairs.append({
                'source': src,
                'target': tgt,
                'dataset': 'Europarl',
                'metadata': meta
            })

        logger.info(f"Loaded {len(self.pairs)} total pairs from OPUS")

    def filter_quality(self, min_length=10, max_ratio=3.0):
        """
        Apply quality filters
        """
        logger.info("Filtering for quality...")
        initial_count = len(self.pairs)

        filtered = []

        for pair in tqdm(self.pairs):
            src = pair['source']
            tgt = pair['target']

            # Length checks
            if len(src) < min_length or len(tgt) < min_length:
                continue

            # Ratio check
            ratio = len(src) / len(tgt) if len(tgt) > 0 else 0
            if ratio > max_ratio or ratio < (1/max_ratio):
                continue

            # Language detection
            try:
                if langdetect.detect(src) != 'en':
                    continue
                if langdetect.detect(tgt) != 'ga':
                    continue
            except:
                continue

            filtered.append(pair)

        self.pairs = filtered
        logger.info(f"Filtered from {initial_count} to {len(self.pairs)} pairs")

    def deduplicate(self):
        """
        Remove duplicates
        """
        logger.info("Deduplicating...")
        initial_count = len(self.pairs)

        seen = set()
        unique = []

        for pair in self.pairs:
            key = (pair['source'], pair['target'])
            if key not in seen:
                seen.add(key)
                unique.append(pair)

        self.pairs = unique
        logger.info(f"Removed {initial_count - len(self.pairs)} duplicates")

    def export(self, output_prefix='eu_irish_dataset'):
        """
        Export dataset in multiple formats
        """
        logger.info("Exporting dataset...")

        # Create HuggingFace Dataset
        dataset = Dataset.from_dict({
            'translation': [
                {'en': p['source'], 'ga': p['target']}
                for p in self.pairs
            ],
            'source_dataset': [p.get('dataset', 'unknown') for p in self.pairs]
        })

        # Save locally
        dataset.save_to_disk(f'{output_prefix}_hf')

        # Export Moses format
        with open(f'{output_prefix}.en', 'w') as f_en, \
             open(f'{output_prefix}.ga', 'w') as f_ga:
            for pair in self.pairs:
                f_en.write(pair['source'] + '\n')
                f_ga.write(pair['target'] + '\n')

        logger.info(f"Exported {len(self.pairs)} pairs to {output_prefix}")

        return dataset

    def _get_celex_numbers(self, limit):
        """Placeholder for CELEX number retrieval"""
        # Implement EUR-Lex API or SPARQL query
        return []

    def _fetch_eurlex_text(self, celex, lang):
        """Placeholder for EUR-Lex text retrieval"""
        # Implement actual fetching logic
        return None

    def _align_paragraphs(self, en_text, ga_text):
        """Simple paragraph alignment"""
        en_paras = [p.strip() for p in en_text.split('\n\n') if p.strip()]
        ga_paras = [p.strip() for p in ga_text.split('\n\n') if p.strip()]

        if len(en_paras) == len(ga_paras):
            return [
                {'source': en, 'target': ga}
                for en, ga in zip(en_paras, ga_paras)
            ]
        return []

# Usage
if __name__ == '__main__':
    builder = EUIrishDatasetBuilder()

    # Load from OPUS
    builder.load_opus_datasets()

    # Optionally fetch additional EUR-Lex documents
    # builder.fetch_eurolex_documents(limit=100)

    # Apply quality filters
    builder.filter_quality()

    # Deduplicate
    builder.deduplicate()

    # Export
    dataset = builder.export('eu_irish_parallel')

    print(f"\nDataset Statistics:")
    print(f"Total pairs: {len(builder.pairs)}")
    print(f"Source datasets: {set(p.get('dataset') for p in builder.pairs)}")
```

### Advanced Techniques

#### Using LanceDB for Dataset Management

```python
import lancedb
from sentence_transformers import SentenceTransformer

# Create LanceDB table for semantic search
db = lancedb.connect("eu_irish_db")

# Embed sentences
model = SentenceTransformer('paraphrase-multilingual-mpnet-base-v2')

data = []
for pair in pairs:
    en_embedding = model.encode(pair['source'])
    ga_embedding = model.encode(pair['target'])

    data.append({
        'english': pair['source'],
        'irish': pair['target'],
        'en_vector': en_embedding,
        'ga_vector': ga_embedding,
        'source': pair.get('dataset', 'unknown')
    })

# Create table
table = db.create_table("eu_irish_parallel", data=data)

# Semantic search
query = "environmental protection"
query_vector = model.encode(query)
results = table.search(query_vector).limit(10).to_pandas()
```

#### Using DuckDB for Analysis

```python
import duckdb

# Create database
con = duckdb.connect('eu_irish.db')

# Create table
con.execute("""
    CREATE TABLE parallel_corpus (
        id INTEGER PRIMARY KEY,
        english TEXT,
        irish TEXT,
        source_dataset VARCHAR,
        en_length INTEGER,
        ga_length INTEGER,
        length_ratio FLOAT
    )
""")

# Insert data
for i, pair in enumerate(pairs):
    en_len = len(pair['source'])
    ga_len = len(pair['target'])
    ratio = en_len / ga_len if ga_len > 0 else 0

    con.execute("""
        INSERT INTO parallel_corpus VALUES (?, ?, ?, ?, ?, ?, ?)
    """, (i, pair['source'], pair['target'], pair.get('dataset'),
          en_len, ga_len, ratio))

# Query for statistics
stats = con.execute("""
    SELECT
        source_dataset,
        COUNT(*) as pair_count,
        AVG(en_length) as avg_en_length,
        AVG(ga_length) as avg_ga_length,
        AVG(length_ratio) as avg_ratio
    FROM parallel_corpus
    GROUP BY source_dataset
""").fetchdf()

print(stats)
```

---

## Dataset Quality and Characteristics

### Quality Metrics

**Professional Translation Quality:**
- All EU translations performed by qualified translators
- Multi-stage review process
- Terminology coordination via IATE
- Legal validation for legislative texts

**Domain Coverage:**
- Legal: Regulations, directives, court judgments
- Political: Parliamentary debates, resolutions
- Administrative: Commission communications, reports
- Technical: Research project descriptions, standards
- Economic: Economic analysis, budget documents
- Environmental: Environmental policy, impact assessments

**Temporal Coverage:**
- 2007-present: All Irish language EU materials
- 2022-present: Full working language status (complete coverage)
- Historical: Some pre-2007 documents translated retroactively

### Dataset Statistics (Approximate)

**DGT Translation Memory:**
- Sentence pairs: 1-3 million (Irish-English)
- Unique segments: High percentage (low repetition)
- Domains: All EU policy areas
- Update: Annual releases

**Europarl:**
- Sentence pairs: ~700,000 (Irish-English, v10)
- Words (Irish): ~15-20 million
- Words (English): ~18-23 million
- Time period: 2007-present (expanding)

**JRC-Acquis:**
- Sentence pairs: ~1 million+ (Irish-English)
- Focus: Legal texts
- Vintage: Primarily 2007-2015

**EUR-Lex (full corpus):**
- Documents: 10,000+ with Irish translations
- Growing: ~1,000-2,000 new documents annually
- Comprehensive: All binding legislation

### Linguistic Characteristics

**Irish Language Variety:**
- **Caighdeán Oifigiúil** (Official Standard)
- Formal, written register
- Standardized terminology
- Modern neologisms for technical concepts

**Sentence Complexity:**
- Legal texts: Complex, formal structures
- Parliamentary debates: More natural, spoken-style (but edited)
- Administrative: Medium complexity
- Technical documents: Specialized vocabulary

**Terminology:**
- Highly specialized across domains
- Consistent use of official terms
- IATE integration ensures standardization
- Rich technical vocabulary development

---

## Legal and Licensing Considerations

### EUR-Lex and EU Publications

**Copyright Status:**
- EU institutions' documents are generally in the public domain for acts with legal force
- Reuse permitted under specific conditions
- Attribution typically required

**Commission Decision 2011/833/EU:**
Authorizes reuse of Commission documents, subject to conditions.

**General Principles:**
1. Free reuse for non-commercial and commercial purposes
2. Attribution required
3. No endorsement implied
4. Source must be acknowledged

### OPUS Corpus Licensing

**Typical Licenses:**
- DGT-TM: CC-BY 4.0 (recent versions)
- Europarl: Public domain / No copyright (EU institutional documents)
- JRC-Acquis: Research use permitted

### ELRC-SHARE Resources

**PSI Directive (Directive 2019/1024):**
- Promotes open data and reuse of public sector information
- EU member states must make public sector data available
- Irish government alignment with EU open data policies

### Best Practices

1. **Always check current license terms** for specific datasets
2. **Provide attribution** when required
3. **Cite sources** in research papers
4. **Respect terms of use** for web scraping
5. **Use official APIs** when available
6. **Document data provenance** in your datasets

**Recommended Citation Format:**
```
European Commission, Directorate-General for Translation (2023).
DGT Translation Memory [Dataset].
Retrieved from https://joint-research-centre.ec.europa.eu/language-technology-resources/dgt-translation-memory_en
License: CC-BY 4.0
```

---

## Research Applications

### Machine Translation

**Training MT Systems:**
- Use DGT-TM and Europarl for base training
- Domain adaptation with specialized EU texts
- Terminology integration from IATE
- Evaluation on EUR-Lex test sets

**Example Projects:**
- Helsinki-NLP OPUS-MT models (en-ga, ga-en)
- M2M100 multilingual models
- UCCIX project Irish-English translation

### Language Modeling

**Pre-training LLMs:**
- Irish language exposure in diverse domains
- Technical and formal register representation
- Terminology-rich content
- Bilingual learning signals

**Applications:**
- UCCIX: Irish LLM using EU data among other sources
- Domain-specific models (legal, political)
- Terminology-aware language models

### Terminology Extraction

**Bilingual Lexicon Induction:**
- Extract technical term pairs
- Build domain glossaries
- Terminology database creation
- Cross-lingual concept alignment

**Tools:**
- GIZA++ for word alignment
- FastAlign for efficient alignment
- Modern neural methods (BERTalign)

### Information Retrieval

**Cross-lingual IR:**
- Search Irish documents using English queries
- Multilingual EU policy search
- Legal information retrieval
- Document similarity across languages

**Datasets:**
- EUR-Lex as IR corpus
- Parliamentary debates for QA
- Legislation for legal IR

### Linguistic Research

**Corpus Linguistics:**
- Irish language usage patterns in formal domains
- Terminology development
- Translation universals
- Language change and modernization

**Computational Linguistics:**
- Syntax analysis (dependency parsing)
- Morphological richness (Irish inflection)
- Discourse coherence
- Translation quality assessment

### Named Entity Recognition (NER)

**Creating NER Datasets:**
- EU documents rich in named entities
- Organization names (EU institutions)
- Legal references (regulations, directives)
- Person names (politicians, officials)
- Location names (member states, cities)

**Annotation Projects:**
- Semi-automatic annotation using alignment
- Transfer learning from English NER
- Domain-specific entity types

### Educational Applications

**Language Learning:**
- Authentic Irish language materials
- Domain-specific vocabulary learning
- Professional translation examples
- Parallel reading resources

**Tools:**
- Bilingual dictionary enhancement
- Example sentence databases
- Context-aware translation tools

---

## References and Further Resources

### Official EU Resources

**EUR-Lex:**
- Website: https://eur-lex.europa.eu
- Documentation: https://eur-lex.europa.eu/content/help/eur-lex.html
- Web Services: https://eur-lex.europa.eu/content/help/data-reuse/webservice.html

**DGT Translation Memory:**
- Website: https://joint-research-centre.ec.europa.eu/language-technology-resources/dgt-translation-memory_en
- Download: Available from JRC website
- License: CC-BY 4.0 (recent versions)

**IATE:**
- Website: https://iate.europa.eu
- About: https://iate.europa.eu/about

**EU Open Data Portal:**
- Website: https://data.europa.eu
- API: https://data.europa.eu/api

**Publications Office:**
- Website: https://op.europa.eu
- EU Vocabularies: https://op.europa.eu/en/web/eu-vocabularies
- SPARQL Endpoint: https://publications.europa.eu/webapi/rdf/sparql

### Corpus Resources

**OPUS:**
- Website: https://opus.nlpl.eu
- GitHub: https://github.com/Helsinki-NLP/OPUS-MT
- OpusTools: https://github.com/Helsinki-NLP/OpusTools

**ELRC-SHARE:**
- Website: https://elrc-share.eu
- Documentation: https://elrc-share.eu/documentation

**Europarl Corpus:**
- Original: https://www.statmt.org/europarl/
- OPUS version: https://opus.nlpl.eu/Europarl.php

### Irish Language Resources

**Foras na Gaeilge (Irish Language Body):**
- Website: https://www.forasnagaeilge.ie
- Terminology: https://www.tearma.ie
- Dictionary: https://www.focal.ie

**An Caighdeán Oifigiúil:**
- Official standard for Irish spelling and grammar
- Used by EU translation services

**Dublin City University (DCU) NLP:**
- HuggingFace: https://huggingface.co/DCU-NLP
- Research: Irish NLP tools and models

### Research Papers

**Irish Language Models:**
- **UCCIX:** "UCCIX: Irish-eXcellence Large Language Model" (arXiv:2405.13010)
- **gaBERT:** "gaBERT - an Irish Language Model" (LREC 2022, arXiv:2107.12930)

**Parallel Corpora:**
- **DGT-TM:** JRC Technical Reports
- **Europarl:** "Europarl: A Parallel Corpus for Statistical Machine Translation" (Koehn, 2005)

**Machine Translation:**
- **OPUS-MT:** "OPUS-MT – Building open translation services for the World" (2020)
- **M2M100:** "Beyond English-Centric Multilingual Machine Translation" (2020)

### Tools and Libraries

**OpusTools:**
```bash
pip install opustools
```
- GitHub: https://github.com/Helsinki-NLP/OpusTools
- Documentation: https://opus.nlpl.eu/opustools/

**HuggingFace Datasets:**
```bash
pip install datasets
```
- Documentation: https://huggingface.co/docs/datasets

**LanceDB (Vector Database):**
```bash
pip install lancedb
```
- Website: https://lancedb.com
- Documentation: https://lancedb.github.io/lancedb/

**DuckDB (Analytics):**
```bash
pip install duckdb
```
- Website: https://duckdb.org
- Documentation: https://duckdb.org/docs/

**Web Scraping:**
```bash
pip install crawl4ai trafilatura beautifulsoup4
```

### Legal and Policy Documents

**Regulation No 1/1958** (as amended):
- Legal basis for EU language policy
- Available on EUR-Lex

**PSI Directive (2019/1024):**
- Open data and public sector information reuse
- EUR-Lex: https://eur-lex.europa.eu/eli/dir/2019/1024/oj

**Commission Decision 2011/833/EU:**
- Reuse of Commission documents
- EUR-Lex: https://eur-lex.europa.eu/eli/dec/2011/833/oj

### Community and Support

**OPUS Community:**
- Forum: https://groups.google.com/g/opus-users
- Issues: GitHub issues on relevant repositories

**HuggingFace Community:**
- Forums: https://discuss.huggingface.co
- Discord: HuggingFace Discord server

**Irish NLP Community:**
- Research groups at DCU, Trinity College Dublin
- UCCIX project community
- Language technology events in Ireland

---

## Quick Start Checklist

### Getting Started with EU Irish Datasets

- [ ] **Identify your use case** (MT, LLM training, terminology, NER, etc.)
- [ ] **Choose primary data source:**
  - [ ] OPUS (DGT-TM, Europarl) for ready-to-use parallel corpora
  - [ ] EUR-Lex for legal/regulatory focus
  - [ ] European Parliament for political/debate content
  - [ ] ELRC-SHARE for diverse public sector data
- [ ] **Install required tools:**
  - [ ] OpusTools for OPUS access
  - [ ] HuggingFace datasets library
  - [ ] Web scraping tools (if needed)
- [ ] **Download initial dataset:**
  - [ ] Start with DGT-TM (1-3M pairs, manageable size)
  - [ ] Or Europarl (~700k pairs, political domain)
- [ ] **Perform quality checks:**
  - [ ] Language detection verification
  - [ ] Length ratio filtering
  - [ ] Deduplication
  - [ ] Manual spot-checking
- [ ] **Prepare for your task:**
  - [ ] Split into train/val/test
  - [ ] Export in required format
  - [ ] Document data provenance
  - [ ] Check license compliance
- [ ] **Build and iterate:**
  - [ ] Start with baseline model/system
  - [ ] Evaluate performance
  - [ ] Add more data if needed
  - [ ] Consider domain adaptation

### Example First Project: MT Training Dataset

```bash
# 1. Install tools
pip install opustools datasets langdetect

# 2. Download DGT corpus
python -m opustools.opus_read -d DGT -s en -t ga -w moses -wm dgt_corpus

# 3. Load and process
python process_dgt.py  # Use code examples from this guide

# 4. Create HuggingFace dataset
python create_hf_dataset.py

# 5. Train model (e.g., with HuggingFace Transformers)
# Use Irish-English parallel data for Helsinki-NLP style model training
```

---

## Conclusion

The European Union's commitment to Irish as a full official and working language has created an unprecedented resource for Irish language technology development. The combination of:

1. **Legal mandate** for translation
2. **Professional quality** standards
3. **Domain diversity** across all EU policy areas
4. **Open access** to most materials
5. **Standardized terminology** through IATE
6. **Parallel text alignment** across 24 languages
7. **Continuous growth** as new legislation and documents are published

...makes EU sources an invaluable foundation for Irish NLP, machine translation, language modeling, and linguistic research.

Researchers and developers are encouraged to:
- Leverage existing parallel corpora (DGT, Europarl)
- Explore specialized domains (legal, technical, political)
- Contribute cleaned datasets back to the community
- Build on official terminology resources
- Respect licensing and attribution requirements
- Collaborate with Irish language technology initiatives

The resources documented in this guide represent only a portion of what's available. As Irish continues to develop in EU contexts and translation capacity expands, these datasets will grow richer and more comprehensive.

**For the latest updates and additions, monitor:**
- EUR-Lex new publications
- OPUS corpus updates
- ELRC-SHARE new datasets
- EU Open Data Portal
- Irish language technology research publications

---

**Document Version:** 1.0
**Last Updated:** 2025-11-17
**Maintained by:** Research Community
**Contributions Welcome:** Please submit corrections and additions

---

*This guide was created to support Irish language technology development and research using European Union resources. All information accurate as of publication date.*
