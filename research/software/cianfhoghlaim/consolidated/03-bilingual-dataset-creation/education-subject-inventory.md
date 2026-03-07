# Irish Education Subject Data Inventory

Comprehensive inventory of subject data availability across the three scraped education websites.

## Executive Summary

| Metric | Value |
|--------|-------|
| Total JSON Files | ~1,100 |
| Junior Cycle Subjects | 18 core + 16 short courses |
| Senior Cycle Subjects | 50+ |
| Years of Statistics | 2011-2024 (14 years) |
| Languages | English (EN) and Irish (GA) |

---

## Data Sources Overview

### 1. curriculumonline.ie (300 files)

**Strongest Coverage:** Junior Cycle (8 of 18 subjects well covered)

| Content Type | Count | Notes |
|--------------|-------|-------|
| English pages | 171 | Main curriculum content |
| Irish (GA) pages | 129 | Bilingual mirror |
| Subject pages | ~150 | Junior Cycle focus |
| Short courses | ~20 | Various JC short courses |
| Early Childhood | 14 | Aistear framework |
| Primary | 2 | Limited coverage |
| Senior Cycle | 0 content | Navigation only |

### 2. examinations.ie (498 JSON + 102 stats files)

**Strongest Coverage:** Examination logistics, statistics, circulars

| Content Type | Count | Notes |
|--------------|-------|-------|
| Parameterized pages | 117 | Bilingual (EN/IR) |
| PDF references | 338 | Chief Examiner Reports, circulars |
| Statistics files | 102 | CSV + PDF (2011-2024) |
| Exam info pages | ~60 | 2024-2026 exams |

### 3. ncca.ie (300 files)

**Strongest Coverage:** Senior Cycle curriculum development, policy

| Content Type | Count | Notes |
|--------------|-------|-------|
| English pages | 224 | Primary content |
| Irish (GA) pages | 76 | Partial mirror |
| Development groups | 28 | All boards and groups |
| SC developments | 40+ | Subject development status |
| Research/publications | ~30 | Reports and papers |

---

## Junior Cycle Subject Inventory

### Subjects with Strong Data (8 subjects)

| Subject | curriculumonline | examinations | ncca | EN | GA | CBAs |
|---------|-----------------|--------------|------|----|----|------|
| **Gaeilge** | 44 pages | Stats | Dev group | ✓ | ✓ | CBA-1, CBA-2 |
| **Business Studies** | 25 pages | Stats | Dev group | ✓ | ✓ | CBA-1, CBA-2 |
| **English** | 21 pages | Stats | Dev group | ✓ | ✓ | CBA-1, CBA-2 |
| **Geography** | 23 pages | Stats | Dev group | ✓ | ✓ | CBA-1, CBA-2 |
| **Engineering** | 24 pages | Stats | Dev group | ✓ | ✓ | CBA-1, CBA-2 |
| **Applied Technology** | 23 pages | Stats | - | ✓ | ✓ | CBA-1, CBA-2 |
| **Graphics** | 20 pages | Stats | Dev group | ✓ | ✓ | CBA-1, CBA-2 |
| **Classics** | 18 pages | Stats | - | ✓ | ✓ | CBA-1, CBA-2 |

### Subjects with Limited Data (10 subjects - needs expansion)

| Subject | curriculumonline | examinations | ncca | Notes |
|---------|-----------------|--------------|------|-------|
| History | Navigation only | Stats | JC Dev group | Needs scraping |
| Home Economics | Navigation only | Stats | Dev group | Needs scraping |
| Mathematics | 1 page | Stats | Dev group | Needs scraping |
| Modern Foreign Languages | Navigation only | Stats | JC MFL Dev group | Needs scraping |
| Music | Navigation only | Stats | Dev group | Needs scraping |
| Religious Education | Navigation only | Stats | - | Needs scraping |
| Science | Navigation only | Stats | - | Needs scraping |
| Visual Art | Navigation only | Stats | - | Needs scraping |
| Wood Technology | Navigation only | Stats | - | Needs scraping |
| Jewish Studies | Navigation only | Stats | - | Needs scraping |

### Junior Cycle Short Courses (16 available)

| Short Course | Pages | Level | Notes |
|--------------|-------|-------|-------|
| Coding | ~5 | Standard | Strong data |
| CSPE | ~5 | Standard | Civic education |
| Digital Media Literacy | ~3 | Standard | |
| Philosophy | ~3 | Standard | |
| Physical Education | ~5 | Standard | |
| SPHE | ~5 | Standard | |
| Chinese Language & Culture | ~3 | Standard | |
| Artistic Performance | ~2 | Standard | |
| CSI: Forensic Science | ~2 | Level 2 | L2LP |
| History | ~2 | Level 2 | L2LP |
| Enterprise in Animation | ~2 | Level 2 | L2LP |
| + 5 others | ~10 | Various | |

---

## Senior Cycle Subject Inventory

### Subjects Under Active Development (40+)

Data from ncca.ie curriculum-developments section:

| Subject | Development Status | Draft Available | Consultation | Dev Group |
|---------|-------------------|-----------------|--------------|-----------|
| **Accounting** | Commenced | No | No | ✓ |
| **Agricultural Science** | In development | No | No | ✓ |
| **Applied Mathematics** | Existing | N/A | N/A | - |
| **Arabic** | Redeveloped | Yes | Closed | - |
| **Art** | Draft in progress | No | No | ✓ |
| **Biology** | Existing | N/A | N/A | - |
| **Business** | Envisioned | No | No | - |
| **Chemistry** | Existing | N/A | N/A | - |
| **Classical Languages** | In development | No | No | - |
| **Classical Studies** | Existing | N/A | N/A | - |
| **Climate Action & SD** | Envisioned | No | No | - |
| **Computer Science** | Draft completed | Yes | Open | ✓ |
| **Construction Studies** | Commenced | Yes | Open | ✓ |
| **Design & Comm Graphics** | Draft completed | Yes | Open | ✓ |
| **Drama, Film, Theatre** | Envisioned | No | No | - |
| **Economics** | Draft in progress | No | No | ✓ |
| **Engineering** | Commenced | No | No | ✓ |
| **English** | Commenced | No | No | ✓ |
| **Gaeilge** | In development | No | No | ✓ |
| **Geography** | Commenced | No | No | ✓ |
| **History** | Commenced | No | No | ✓ |
| **Home Economics** | Commenced | No | No | ✓ |
| **Mathematics** | Commenced | No | No | ✓ |
| **Modern Foreign Languages** | In development | No | No | ✓ |
| **Music** | Commenced | No | No | ✓ |
| **Physics** | Draft in progress | No | No | - |
| **Physics and Chemistry** | Existing | N/A | N/A | ✓ |
| **Politics and Society** | Existing | N/A | N/A | ✓ |
| **Technology** | In development | No | No | ✓ |
| **LC Physical Education** | Commenced | No | No | ✓ |
| **LCVP Link Modules** | Commenced | No | No | ✓ |
| **Transition Year** | In development | No | No | - |

### Modern Foreign Languages (8+)

| Language | LC Available | Statistics | Notes |
|----------|-------------|------------|-------|
| French | ✓ | ✓ | Traditional |
| German | ✓ | ✓ | Traditional |
| Spanish | ✓ | ✓ | Traditional |
| Italian | ✓ | ✓ | Traditional |
| Japanese | ✓ | ✓ | |
| Russian | ✓ | ✓ | |
| Arabic | ✓ (redeveloped) | ✓ | Recently updated |
| Mandarin Chinese | ✓ | ✓ | |
| Lithuanian | In development | - | New |
| Polish | In development | - | New |
| Portuguese | In development | - | New |

### LCA Modules (27 identified)

Located at /senior-cycle/lca/ on curriculumonline.ie:
- Active Leisure Studies, Agriculture/Horticulture, Childcare/Community Care
- Craft and Design, Dance, Drama, Engineering
- English and Communication, Gaeilge Chumarsáideach
- Graphics and Construction Studies, Hair and Beauty
- Hotel Catering and Tourism, ICT, Leisure and Recreation
- Mathematical Applications, Modern Languages, Music
- Office Administration, Religious Education, Science
- Sign Language, Social Education, Technology, Visual Art
- Vocational Preparation and Guidance

---

## Special Assessment Components by Subject

### Subjects with Oral Components

| Subject | Level | Oral % | Notes |
|---------|-------|--------|-------|
| Gaeilge | JC & LC | 40% | Mandatory |
| French | LC | 25% | Mandatory |
| German | LC | 25% | Mandatory |
| Spanish | LC | 25% | Mandatory |
| Italian | LC | 25% | Mandatory |
| Japanese | LC | 25% | Mandatory |
| Russian | LC | 25% | Mandatory |
| Arabic | LC | 25% | Mandatory |
| English | LC | 0% | No oral |

### Subjects with Practical/Lab Components

| Subject | Level | Practical % | Type |
|---------|-------|------------|------|
| Biology | LC | 0% | Mandatory experiments (no marks) |
| Chemistry | LC | 0% | Mandatory experiments (no marks) |
| Physics | LC | 0% | Mandatory experiments (no marks) |
| Agricultural Science | LC | 25% | Project |
| Construction Studies | LC | 50% | Project |
| Engineering | LC | 50% | Project |
| Technology | LC | 50% | Project |
| Home Economics | LC | 20% | Practical exam |
| Art | LC | 50% | Practical + written |
| Music | LC | 25% | Performance |
| PE (new) | LC | TBD | Physical activities |

### Subjects with Project/Coursework

| Subject | Level | Project % | Description |
|---------|-------|-----------|-------------|
| Geography | LC | 20% | Geographical investigation |
| History | LC | 20% | Research study |
| Politics & Society | LC | 20% | Citizenship project |
| Computer Science | LC | 30% | Coding project |
| Design & Comm Graphics | LC | 40% | Design project |
| LCVP | LC | 60% | Portfolio + link modules |

### Junior Cycle CBAs

All JC subjects have two Classroom-Based Assessments:
- **CBA-1**: Typically in 2nd year
- **CBA-2**: Typically in 3rd year
- **Assessment Task**: Linked to CBA-2, contributes to final grade

---

## Statistics Data Availability (examinations.ie)

### Years with Complete Data

| Year | JC Stats | LC Stats | LCA Stats | Gender | County |
|------|----------|----------|-----------|--------|--------|
| 2024 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2023 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2022 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2021 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2020 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2019 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2018 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2017 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2016 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2015 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2014 | Partial | Partial | Partial | ✓ | ✓ |
| 2013 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2012 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 2011 | ✓ | ✓ | ✓ | ✓ | ✓ |

**Note:** 2014 data has some failed downloads (404 errors)

### Statistics File Types

- National results summary (CSV)
- Results by gender (CSV)
- Results by county (CSV)
- Results with >10 candidates (privacy filter)
- Grade distribution (PDF)

---

## Bilingual Coverage Analysis

### curriculumonline.ie

| Level | EN Pages | GA Pages | Coverage |
|-------|----------|----------|----------|
| Early Childhood | 7 | 7 | 100% |
| Primary | 1 | 1 | 100% |
| Junior Cycle | ~100 | ~80 | ~80% |
| Senior Cycle | 0 | 0 | N/A |

### ncca.ie

| Section | EN Pages | GA Pages | Coverage |
|---------|----------|----------|----------|
| Senior Cycle | ~150 | ~50 | ~33% |
| Junior Cycle | ~40 | ~15 | ~38% |
| Primary | ~20 | ~8 | ~40% |
| About | ~30 | ~3 | ~10% |

### examinations.ie

- Uses URL parameter `l=en` or `l=ir`
- Most pages have bilingual versions
- Statistics in English only

---

## Data Gaps and Expansion Priorities

### Priority 1: Critical Gaps

1. **Senior Cycle Subject Content** (curriculumonline.ie)
   - Only navigation pages exist
   - 50+ subjects need full scraping
   - Estimated: 300-500 additional pages

2. **Exam Papers and Marking Schemes** (examinations.ie)
   - References exist but PDFs not downloaded
   - Archive at /exammaterialarchive/
   - Estimated: 1000+ PDFs

3. **10 Junior Cycle Subjects** (curriculumonline.ie)
   - History, Mathematics, Science, etc.
   - Specification pages exist but not scraped

### Priority 2: Enhancement

1. **Statistics Files** (examinations.ie)
   - Directories exist but many files empty
   - Need targeted re-scraping

2. **Irish Language Versions** (all sites)
   - ~60-70% bilingual coverage
   - Complete GA versions needed

3. **Chief Examiner Reports** (examinations.ie)
   - Referenced 1999-2013
   - PDFs need downloading

### Priority 3: Completeness

1. **Primary Curriculum** (curriculumonline.ie)
   - Recently redeveloped (2025)
   - Limited current coverage

2. **Research Publications** (ncca.ie)
   - Many PDFs referenced
   - Need systematic download

---

## Subject Progression Mapping

### Junior Cycle → Senior Cycle Progressions

| JC Subject | LC Progression | Notes |
|------------|----------------|-------|
| Business Studies | Business, Accounting, Economics | Three pathways |
| Science | Biology, Chemistry, Physics | Specialization |
| Geography | Geography | Direct continuation |
| History | History | Direct continuation |
| Gaeilge | Gaeilge | T1→L1, T2→L2 streams |
| English | English | Direct continuation |
| Mathematics | Mathematics, Applied Maths | Two pathways |
| MFL | French, German, Spanish, etc. | Same languages |
| Graphics | Design & Comm Graphics | Renamed |
| Engineering | Engineering | Direct continuation |
| Home Economics | Home Economics | Direct continuation |
| Music | Music | Direct continuation |
| Art (Visual Art) | Art | Renamed |
| Wood Technology | Construction Studies, Technology | Two pathways |
| Coding (short course) | Computer Science | New subject |

---

## Recommended Next Steps

1. **Run Crawl4AI** with provided configurations to expand coverage
2. **Download PDFs** from examinations.ie archive
3. **Process with CocoIndex** pipeline to extract and embed
4. **Build BAML extraction** for structured data
5. **Create unified search** across all content
6. **Implement QwenVL** for PDF/image extraction
