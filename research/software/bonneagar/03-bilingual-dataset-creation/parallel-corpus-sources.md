# Parallel Corpus Sources for Irish-English

## Overview

This document catalogs all known sources of Irish-English parallel text, organized by quality tier and domain.

---

## 1. Tier 1: Professional Translation Sources

### 1.1 Gaois Parallel Corpus

**Primary source for high-quality parallel text.**

| Property | Value |
|----------|-------|
| **URL** | https://www.gaois.ie/en/corpora/parallel/data |
| **Size** | 130.5M words |
| **Format** | TMX |
| **Alignment** | Sentence-level |
| **Quality** | Professional translation |

**Content Breakdown:**

| Domain | Irish Words | English Words |
|--------|-------------|---------------|
| EU Legislation | ~30M | ~28M |
| Acts of Oireachtas | ~25M | ~23M |
| Constitution | ~50K | ~45K |
| Statutory Instruments | ~13M | ~12M |

### 1.2 EUR-Lex (EU Official Journal)

| Property | Value |
|----------|-------|
| **URL** | https://eur-lex.europa.eu |
| **Languages** | 24 EU languages including Irish |
| **Format** | Various (HTML, PDF, XML) |
| **Access** | Public domain |

**Irish Coverage:**
- Regulations and Directives (since 2007)
- Official Journal of the EU
- Court judgments

### 1.3 Houses of the Oireachtas

| Property | Value |
|----------|-------|
| **URL** | https://www.oireachtas.ie |
| **Content** | Debates, legislation |
| **Format** | HTML, PDF |
| **Alignment** | Document-level (needs processing) |

**Resources:**
- Dáil/Seanad debates (bilingual sections)
- Legislation (Acts, SIs)
- Committee reports

---

## 2. Tier 2: Institutional Sources

### 2.1 Logainm API (Placenames)

| Property | Value |
|----------|-------|
| **URL** | https://www.logainm.ie/api/v1.0 |
| **Items** | 100,000+ |
| **Alignment** | Term-level (exact pairs) |
| **Quality** | Expert validated |

**Data Fields:**
- `nameGA`: Irish placename
- `nameEN`: English placename
- `variants`: Historical forms
- `coordinates`: Geographic location

### 2.2 Téarma (Terminology)

| Property | Value |
|----------|-------|
| **URL** | https://www.tearma.ie |
| **Domains** | 40+ subject areas |
| **Alignment** | Term-level (exact pairs) |
| **Quality** | Expert validated |

**Subject Categories:**
- Legal/Law
- Medicine/Health
- Science/Technology
- Business/Finance
- EU terminology
- COVID-19 terms

### 2.3 Ainm.ie (Biographies)

| Property | Value |
|----------|-------|
| **URL** | https://www.ainm.ie |
| **Items** | 1,785 biographies |
| **Words** | 1.3M+ Irish |
| **Parallel** | Metadata only (names, places) |

**Note:** Biographies are Irish-only. Parallel content limited to:
- Person names (Irish/English forms)
- Place names (via Logainm links)
- Dates and metadata

---

## 3. Tier 3: Folklore & Heritage

### 3.1 Dúchas API (Folklore Collection)

| Property | Value |
|----------|-------|
| **URL** | https://www.duchas.ie/api/v0.6 |
| **Items** | 80,000+ stories |
| **Irish Content** | ~66% |
| **English Content** | ~33% |

**Collections:**

| Collection | Pages | Content |
|------------|-------|---------|
| **CBE (Main)** | 2,400 volumes | Ethnography, folklore |
| **CBES (Schools)** | 740,000 pages | Local traditions |
| **CBEG (Photos)** | 80,000 images | Visual documentation |

**Alignment Status:**
- Metadata aligned (bilingual)
- Story text: ~10% parallel, ~90% monolingual
- Requires manual alignment for parallel use

### 3.2 Tobar an Dualchais (Scotland)

| Property | Value |
|----------|-------|
| **URL** | https://www.tobarandualchais.co.uk |
| **Items** | 50,000+ recordings |
| **Languages** | Scottish Gaelic, Scots, English |
| **Format** | Audio with transcripts |

**Use Case:** Cross-Celtic comparison, not Irish-English parallel.

---

## 4. Tier 4: Web-Scraped Sources

### 4.1 Government Websites

Bilingual Irish government sites with `/en/` and `/ga/` paths:

| Site | Content | Alignment |
|------|---------|-----------|
| **gov.ie** | Government services | Page-level |
| **rte.ie/gaeilge** | News articles | Some parallel |
| **ncca.ie** | Curriculum documents | PDF parallel |

### 4.2 Wikipedia

| Property | Value |
|----------|-------|
| **Irish Wikipedia** | ~56,000 articles |
| **English Wikipedia** | 6.7M+ articles |
| **Overlap** | ~10-15% with interlanguage links |

**Challenges:**
- Articles not direct translations
- Different scope and detail
- Requires sentence alignment

### 4.3 Tatoeba

| Property | Value |
|----------|-------|
| **URL** | https://tatoeba.org |
| **Irish Sentences** | ~3,000 |
| **Format** | TSV download |
| **License** | CC BY 2.0 |

---

## 5. Monolingual Sources (For Back-Translation)

### 5.1 Corpas.ie

| Corpus | Words | Content |
|--------|-------|---------|
| **CNG (National Corpus)** | 100M | 2000-2024 texts |
| **Written Irish** | 131M | Literature, journalism |
| **Spoken Irish** | 9M | Transcribed speech |
| **Historical** | 3,000+ texts | 1600-1926 |

**URL:** https://www.corpas.ie

### 5.2 Common Crawl (Irish)

| Property | Value |
|----------|-------|
| **CC-100 Irish** | 108M tokens |
| **OSCAR Irish** | Variable |
| **Quality** | Mixed (web text) |

---

## 6. Dataset Licensing

| Source | License | Commercial Use |
|--------|---------|----------------|
| **Gaois TMX** | Open Government | Verify |
| **EUR-Lex** | Public Domain | Yes |
| **Logainm** | Open Data | Yes |
| **Duchas** | Open Data | Yes |
| **Tatoeba** | CC BY 2.0 | Yes (with attribution) |
| **Wikipedia** | CC BY-SA | Yes (share-alike) |

---

## 7. Acquisition Priority

### Phase 1: High-Quality Parallel (Week 1-2)

1. Download Gaois TMX files
2. Set up Logainm API collection
3. Set up Duchas API collection
4. Download Tatoeba Irish-English pairs

### Phase 2: Institutional Content (Week 3-4)

1. Scrape Téarma terminology
2. Process EUR-Lex Irish content
3. Collect government bilingual pages

### Phase 3: Extended Coverage (Week 5-6)

1. Wikipedia article alignment
2. Process folklore for parallel sections
3. Back-translation of monolingual content

---

## 8. Quality Assessment Matrix

| Source | Alignment | Translation | Domain | Volume |
|--------|-----------|-------------|--------|--------|
| **Gaois TMX** | ★★★★★ | ★★★★★ | Legal | ★★★★★ |
| **Logainm** | ★★★★★ | ★★★★★ | Geographic | ★★★★☆ |
| **Téarma** | ★★★★★ | ★★★★★ | Technical | ★★★☆☆ |
| **Duchas** | ★★★☆☆ | ★★★★☆ | Cultural | ★★★★★ |
| **EUR-Lex** | ★★★★☆ | ★★★★★ | Legal | ★★★★☆ |
| **Wikipedia** | ★★☆☆☆ | ★★★☆☆ | General | ★★★☆☆ |
| **Tatoeba** | ★★★★★ | ★★★☆☆ | General | ★★☆☆☆ |

---

## 9. Combined Dataset Structure

```
celtic_parallel_corpus/
├── legal/
│   ├── gaois_legislation.parquet
│   ├── eurlex_irish.parquet
│   └── oireachtas_acts.parquet
├── geographic/
│   ├── logainm_placenames.parquet
│   └── geographic_entities.parquet
├── terminology/
│   ├── tearma_terms.parquet
│   └── domain_glossaries.parquet
├── cultural/
│   ├── duchas_parallel.parquet
│   └── folklore_aligned.parquet
├── general/
│   ├── tatoeba_pairs.parquet
│   └── wikipedia_aligned.parquet
└── metadata/
    ├── sources.json
    └── statistics.json
```

---

## References

- Gaois Corpora: https://www.gaois.ie/en/corpora/
- EUR-Lex: https://eur-lex.europa.eu
- Tatoeba: https://tatoeba.org/en/downloads
- CC-100: https://data.statmt.org/cc-100/
