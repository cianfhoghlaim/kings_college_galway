# Pan-Celtic Web Scraping Strategy

## Overview

This document defines scraping strategies for Celtic language resources across Ireland, Scotland, Wales, and the Isle of Man using agentic browser automation.

---

## 1. Interaction Type Classification

### 1.1 Type Matrix

| Type | Pattern | Block Strategy | Example Sites |
|------|---------|----------------|---------------|
| **Type A** | Hierarchical drill-down | Navigation Block | ncca.ie, culturevannin.im |
| **Type B** | Complex form logic | Navigation V2 Block | examinations.ie, sqa.org.uk |
| **Type C** | Spatial/map traversal | Navigation V2 Block | canuint.ie, peoplescollection.wales |
| **Type D** | Sequential/paginated | Navigation V2 Block | duchas.ie, tobarandualchais.co.uk |

### 1.2 Block Selection Guide

- **Action Block:** Single, deterministic interaction (click, type)
- **Navigation Block:** Single goal with intermediate steps
- **Navigation V2 Block:** Multi-goal workflows with state management

---

## 2. Ireland - Primary Targets

### 2.1 ncca.ie (National Council for Curriculum and Assessment)

| Property | Value |
|----------|-------|
| **URL** | https://ncca.ie/en |
| **Type** | Type A - Hierarchical |
| **Content** | Curriculum specifications, PDFs |
| **Language Toggle** | /en/ ↔ /ga/ |

**Structure:**
- Early Childhood (Aistear)
- Primary Education
- Junior Cycle
- Senior Cycle

**Crawl Strategy:**
```python
async def scrape_ncca():
    targets = [
        "/en/primary/",
        "/en/junior-cycle/subjects/",
        "/en/senior-cycle/"
    ]

    for path in targets:
        result = await crawler.arun(
            url=f"https://ncca.ie{path}",
            extract_structured=True,
            css_selector=".content-area"
        )
```

### 2.2 examinations.ie (State Examinations Commission)

| Property | Value |
|----------|-------|
| **URL** | https://www.examinations.ie |
| **Type** | Type B - Complex Form |
| **Content** | Past papers, marking schemes |
| **Challenge** | Session state, ASP.NET __VIEWSTATE |

**Form Interaction:**
1. Year Selection dropdown
2. Examination Type (Junior/Leaving)
3. Subject dropdown (depends on year)
4. Level (Higher/Ordinary/Foundation)

**Skyvern Prompt:**
```
GOAL: Download Mathematics Higher Level papers for 2023-2024

INSTRUCTIONS:
1. Select '2023' from the Year dropdown
2. Wait for Subject list to update
3. Select 'Mathematics' from Subject
4. Select 'Higher Level' from Level
5. Click 'Search' or equivalent button
6. Download all PDF links on results page

GUARDRAILS:
- Wait for AJAX requests to complete
- Handle copyright acceptance modals
- Only click .pdf links
```

### 2.3 canuint.ie (Dialect Archive)

| Property | Value |
|----------|-------|
| **URL** | https://www.canuint.ie/ga/ |
| **Type** | Type C - Spatial |
| **Content** | Audio dialect recordings |
| **Language Toggle** | /en/ ↔ /ga/ |

**Geographic Structure:**
- Ulster: 6 dialect areas
- Connacht: 16 areas
- Leinster: 1 area
- Munster: 19 areas

**Crawl Strategy:**
```python
async def scrape_canuint():
    # Navigate via text list, NOT map canvas
    provinces = ["ulster", "connacht", "leinster", "munster"]

    for province in provinces:
        # Find text link section
        result = await crawler.arun(
            url="https://www.canuint.ie/ga/",
            css_selector=".recordings-by-area a",
            instruction="Click on province text links, not map"
        )

        # Extract audio URLs and word metadata
        for area_link in result.links:
            area_data = await crawler.arun(
                url=area_link,
                extract_structured=True,
                schema={
                    "word": "string",
                    "audio_url": "string",
                    "area": "string"
                }
            )
```

### 2.4 duchas.ie (Folklore Collection)

| Property | Value |
|----------|-------|
| **URL** | https://www.duchas.ie/en |
| **Type** | Type D - Sequential |
| **Recommendation** | Use Duchas API v0.6 instead |

**Collection Structure:**
- Main Collection (CBE): `/en/cbe/`
- Schools Collection (CBES): `/en/cbes/`
- Photographs (CBEG): `/en/cbeg/`

**Pagination:** "Page number / 225" format

---

## 3. Scotland - Primary Targets

### 3.1 SQA Past Papers

| Property | Value |
|----------|-------|
| **URL** | https://www.sqa.org.uk/pastpapers/findpastpaper.htm |
| **Type** | Type B - Form |
| **Content** | Examination papers |

**Subject List:**
- Gaidhlig (native speakers)
- Gaelic (Learners)
- Eachdraidh (History in Gaelic)
- Matamataig (Mathematics in Gaelic)

**Skyvern Prompt:**
```
GOAL: Download Gaidhlig Higher papers for 2022-2024

INSTRUCTIONS:
1. Locate 'Subject' dropdown
2. Select 'Gaidhlig' (if not found, try 'Gaelic (Learners)')
3. Select 'Higher' from Qualification Level
4. Click 'Go' button
5. Download Question Paper and Marking Instructions PDFs

GUARDRAILS:
- If "No results found", try 'National 5' level
- Accept any copyright terms modals
- Only download .pdf files

COMPLETION: At least 2 PDFs downloaded
```

### 3.2 Tobar an Dualchais

| Property | Value |
|----------|-------|
| **URL** | https://www.tobarandualchais.co.uk |
| **Type** | Type D - Faceted Search |
| **Content** | 50,000+ oral recordings |

**Filters:**
- Language: Gaelic, Scots, English
- Genre: Song, Story, Verse
- Geographic Area

**Crawl Strategy:**
```python
async def scrape_tobar():
    filters = {
        "language": "Gaelic",
        "genre": ["Song", "Story", "Verse"]
    }

    for genre in filters["genre"]:
        result = await crawler.arun(
            url="https://www.tobarandualchais.co.uk/search",
            params={"language": "Gaelic", "genre": genre},
            pagination=True
        )
```

### 3.3 Education Scotland

| Property | Value |
|----------|-------|
| **URL** | https://education.gov.scot |
| **Type** | Type A - Hierarchical |
| **Content** | Curriculum for Excellence |

**Key Documents:**
- Principles and Practice papers
- Experiences and Outcomes (Es and Os)
- CfE Level coding (e.g., LIT 1-01a)

---

## 4. Wales - Primary Targets

### 4.1 Hwb

| Property | Value |
|----------|-------|
| **URL** | https://hwb.gov.wales/curriculum-for-wales/ |
| **Type** | Type D - Sequential |
| **Challenge** | Heavy React/JavaScript |
| **Content** | Curriculum for Wales |

**Structure:**
- 6 Areas of Learning and Experience (AoLEs)
- Progression Steps (PS1 at age 5 to PS5 at age 16)
- "What Matters" statements

**Skyvern Prompt:**
```
GOAL: Extract Humanities Descriptions of Learning

INSTRUCTIONS:
1. Wait for page to fully load (no spinning icons)
2. Click "Areas of Learning and Experience"
3. Click "Humanities"
4. Find "Descriptions of learning" or "Statements of what matters"
5. Select "Progression Step 3"
6. Extract full text of learning descriptions
7. If "Download as PDF" button exists, click it

GUARDRAILS:
- Wait for dynamic content to load
- Ignore "Log in to Hwb" prompts (content is public)
- Do not enter any credentials
```

### 4.2 WJEC Past Papers

| Property | Value |
|----------|-------|
| **URL** | https://www.wjec.co.uk/home/past-papers |
| **Type** | Type B - Search Form |
| **Content** | Examination papers |

**Interface:** Text-input search (not dropdowns)

**Search Terms:**
- "Welsh Language"
- "Welsh Literature"
- "Cymraeg"
- "Welsh Second Language"

### 4.3 People's Collection Wales

| Property | Value |
|----------|-------|
| **URL** | https://www.peoplescollection.wales |
| **Type** | Type C - Spatial |
| **Content** | Heritage archive |

**Content Types:**
- Oral History
- Photos
- Documents
- Case Studies

---

## 5. Isle of Man - Primary Targets

### 5.1 Culture Vannin

| Property | Value |
|----------|-------|
| **URL** | https://www.culturevannin.im |
| **Type** | Type A - Hierarchical |
| **Content** | Manx language materials |

**Key Sections:**
- Watch & Listen (audio/video)
- Publications
- Learn Manx

### 5.2 LearnManx

| Property | Value |
|----------|-------|
| **URL** | https://learnmanx.com |
| **Type** | Interactive modules |
| **Content** | Dictionary, phrases |
| **Challenge** | App-based content |

---

## 6. crawl4ai Implementation

### 6.1 Basic Setup

```python
from crawl4ai import AsyncWebCrawler
from crawl4ai.extraction_strategy import LLMExtractionStrategy

async def create_crawler():
    return AsyncWebCrawler(
        verbose=True,
        # Stealth settings
        browser_type="chromium",
        headless=True
    )
```

### 6.2 Bilingual Page Scraping

```python
async def scrape_bilingual(base_url: str, path: str):
    """Scrape parallel Irish-English pages."""
    async with AsyncWebCrawler() as crawler:
        # English version
        en_result = await crawler.arun(
            url=f"{base_url}/en/{path}",
            extract_structured=True
        )

        # Irish version
        ga_result = await crawler.arun(
            url=f"{base_url}/ga/{path}",
            extract_structured=True
        )

        return {
            "path": path,
            "english": en_result.markdown,
            "irish": ga_result.markdown
        }
```

### 6.3 Rate-Limited Iteration

```python
import asyncio

async def scrape_with_rate_limit(urls: list, delay: float = 1.0):
    """Scrape URLs with rate limiting."""
    results = []

    async with AsyncWebCrawler() as crawler:
        for url in urls:
            result = await crawler.arun(url=url)
            results.append(result)
            await asyncio.sleep(delay)

    return results
```

---

## 7. Skyvern Configuration

### 7.1 sources.yaml

```yaml
groups:
  - id: irish_educational_framework
    description: "Irish curriculum and examination resources"
    targets:
      - url: "https://www.curriculumonline.ie/Primary/Curriculum-Areas/"
        name: "Irish Primary Curriculum"
        type: "Type_A_Hierarchical"
        content_types: ["pdf", "html"]
        priority: high

      - url: "https://ncca.ie/en/junior-cycle/subjects/"
        name: "NCCA Junior Cycle"
        type: "Type_A_Hierarchical"
        priority: high

  - id: scottish_qualifications
    description: "SQA examinations and curriculum"
    targets:
      - url: "https://www.sqa.org.uk/pastpapers/findpastpaper.htm"
        name: "SQA Past Papers"
        type: "Type_B_Form"
        inputs:
          subjects: ["Gaidhlig", "Gaelic (Learners)"]
          levels: ["National 5", "Higher", "Advanced Higher"]
        priority: medium

  - id: welsh_digital_learning
    description: "Hwb and WJEC resources"
    targets:
      - url: "https://hwb.gov.wales/curriculum-for-wales/"
        name: "Hwb Curriculum"
        type: "Type_D_Sequential"
        notes: "Heavy React. Use wait_for_network_idle"

  - id: celtic_audio_archives
    description: "Dialect and folklore archives"
    targets:
      - url: "https://www.canuint.ie/ga/"
        name: "Taisce Chanuinti"
        type: "Type_C_Spatial"
        instruction: "Use text list, not map canvas"

      - url: "https://www.tobarandualchais.co.uk/"
        name: "Tobar an Dualchais"
        type: "Type_D_Sequential"
        filters:
          language: "Gaelic"
```

---

## 8. robots.txt Compliance

Always check before scraping:

```bash
# Check robots.txt for each domain
curl https://www.tearma.ie/robots.txt
curl https://www.logainm.ie/robots.txt
curl https://www.sqa.org.uk/robots.txt
curl https://hwb.gov.wales/robots.txt
```

---

## References

- Skyvern Documentation: https://skyvern.com/docs/introduction
- crawl4ai GitHub: https://github.com/unclecode/crawl4ai
- Prompting Guide: https://skyvern.com/docs/getting-started/prompting-guide
