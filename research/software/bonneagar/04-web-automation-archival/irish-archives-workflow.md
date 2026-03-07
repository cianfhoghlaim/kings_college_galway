# Irish Educational Archives Workflow

## Executive Summary

This document details specific workflows for archiving Irish educational and linguistic resources, including examinations.ie, ncca.ie, curriculumonline.ie, canuint.ie, and duchas.ie. The approach uses the Hunter-Gatherer-Operator pattern with domain-specific configurations.

---

## 1. Target Site Analysis

### 1.1 Site Classification

| Site | Type | Challenge | Tool Strategy |
|------|------|-----------|---------------|
| **examinations.ie** | Legacy Form | Session state, ViewState | Stagehand (Operator) |
| **ncca.ie** | Hierarchical Docs | Nested menus | Crawl4AI (Gatherer) |
| **curriculumonline.ie** | PDF Archive | Deep navigation | Navigation V2 Block |
| **canuint.ie** | Audio + Map | Spatial UI, audio assets | Hybrid (avoid canvas) |
| **duchas.ie** | Paginated Archive | Sequential traversal | Crawl4AI (Gatherer) |

### 1.2 Interaction Type Framework

| Type | Description | Example Sites |
|------|-------------|---------------|
| **Type A** | Hierarchical Drill-Down | ncca.ie, curriculumonline.ie |
| **Type B** | Complex Form Logic | examinations.ie |
| **Type C** | Spatial/Map Traversal | canuint.ie |
| **Type D** | Sequential/Paginated | duchas.ie |

---

## 2. examinations.ie: The Legacy Archive

### 2.1 Interface Analysis

The State Examinations Commission website represents a classic "Deep Web" legacy portal:
- ASP.NET framework with POST requests
- Session cookies and `__VIEWSTATE` parameters
- Multi-step form with dependent dropdowns

### 2.2 Workflow Design

```yaml
# examinations.ie workflow
name: irish_exam_harvester
tool: stagehand
type: Type_B_Form

steps:
  - action: navigate
    url: "https://www.examinations.ie/exammaterialarchive/"

  - action: wait
    selector: "#year-dropdown"

  - action: select
    target: "#year-dropdown"
    value: "{{ year }}"

  - action: wait
    description: "Wait for subject list to populate"
    timeout: 5000

  - action: select
    target: "#subject-dropdown"
    value: "{{ subject }}"

  - action: select
    target: "#level-dropdown"
    value: "{{ level }}"  # Higher, Ordinary, Foundation

  - action: click
    target: "#search-button"

  - action: extract
    instruction: "Extract all PDF download links from results"
    schema:
      papers:
        - filename: string
          url: string
          type: string  # Question Paper, Marking Scheme
```

### 2.3 Stagehand Implementation

```typescript
// examinations.ie scraper
const examArchive = async (year: number, subject: string, level: string) => {
  const stagehand = new Stagehand({
    localBrowserLaunchOptions: { cdpUrl: CDP_URL }
  });

  await stagehand.init();
  await page.goto("https://www.examinations.ie/exammaterialarchive/");

  // Form interaction with wait for dependent dropdowns
  await page.act(`Select '${year}' from the Year dropdown`);
  await page.waitForSelector("#subject-dropdown option:not([disabled])", { timeout: 5000 });

  await page.act(`Select '${subject}' from the Subject dropdown`);
  await page.act(`Select '${level}' from the Level dropdown`);
  await page.act("Click the Search button");

  // Extract results
  const papers = await page.extract({
    instruction: "Extract all exam paper download links with type and filename",
    schema: z.object({
      papers: z.array(z.object({
        filename: z.string(),
        url: z.string(),
        paperType: z.enum(["Question Paper", "Marking Scheme", "Audio"])
      }))
    })
  });

  return papers;
};

// Iterate through all subjects
const subjects = ["Mathematics", "English", "Irish", "Physics", "Chemistry"];
const years = [2024, 2023, 2022, 2021, 2020];
const levels = ["Higher", "Ordinary"];

for (const subject of subjects) {
  for (const year of years) {
    for (const level of levels) {
      const papers = await examArchive(year, subject, level);
      await downloadPapers(papers);
    }
  }
}
```

---

## 3. canuint.ie: Irish Dialect Audio Archive

### 3.1 Interface Analysis

Taisce Chanúintí na Gaeilge presents a hybrid interface:
- Interactive map (canvas-based - avoid)
- Text-based hierarchical lists (target)
- Audio assets linked to geographic locations

**Hierarchy:**
```
Province (Cúige)
└── Area (Limistéar)
    └── Locality/Townland
        └── Speaker → Audio Files
```

### 3.2 Navigation Strategy

**Critical:** Avoid the map canvas. Use the text list "TAIFEADTAÍ DE RÉIR LIMISTÉIR" (Recordings by Area).

```python
# canuint.ie crawler
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig
from crawl4ai.deep_crawling import BestFirstCrawlingStrategy

# Province pages
provinces = [
    ("ulster", "/en/province/ulster"),
    ("connacht", "/en/province/connacht"),
    ("munster", "/en/province/munster"),
    ("leinster", "/en/province/leinster")
]

strategy = BestFirstCrawlingStrategy(
    max_depth=3,
    max_pages=1000,
    scorer_config={
        "keywords": ["taifeadtaí", "recordings", "audio"],
        "weight": 0.85
    },
    # Filter to stay within recordings sections
    url_filter=lambda url: "/recordings/" in url or "/area/" in url
)

config = CrawlerRunConfig(
    deep_crawl_strategy=strategy,
    fit_markdown=True,
    excluded_tags=["nav", "footer", "aside"],  # Strip navigation
    css_selector="main.content"  # Focus on main content
)

async def crawl_dialect_archive():
    async with AsyncWebCrawler(config=BrowserConfig(cdp_url=CDP_URL)) as crawler:
        for province_name, province_url in provinces:
            result = await crawler.arun(
                url=f"https://www.canuint.ie{province_url}",
                config=config
            )

            # Parse audio links from markdown
            audio_links = extract_audio_urls(result.markdown)

            for audio in audio_links:
                await download_audio(
                    url=audio["url"],
                    metadata={
                        "province": province_name,
                        "area": audio["area"],
                        "speaker": audio["speaker"],
                        "word": audio["lemma"]
                    }
                )
```

### 3.3 Audio Extraction Schema

```python
from pydantic import BaseModel
from typing import Optional

class DialectRecording(BaseModel):
    """Schema for canuint.ie audio entries."""
    lemma: str  # Irish word
    audio_url: str
    speaker_name: str
    area: str  # Townland/locality
    county: str
    province: str
    year_recorded: Optional[int]

class DialectCorpus(BaseModel):
    """Collection of dialect recordings."""
    recordings: list[DialectRecording]
    province: str
    extraction_date: str
```

---

## 4. duchas.ie: Folklore Collection

### 4.1 Interface Analysis

The Schools' Collection (Bailiúchán na Scol) features:
- Volume-based organization (CBÉS 0001, CBÉS 0002...)
- Explicit pagination ("Page 1 / 225")
- Rich metadata (School, County, Transcription status)

### 4.2 Sequential Crawl Strategy

```python
# duchas.ie paginated crawler
async def crawl_duchas_collection(start_volume: int = 1, end_volume: int = 1000):
    """Crawl Dúchas Schools' Collection volumes."""

    results = []

    for volume_num in range(start_volume, end_volume + 1):
        volume_id = f"CBÉS{str(volume_num).zfill(4)}"
        volume_url = f"https://www.duchas.ie/en/cbes/{volume_id}"

        # Get volume metadata
        async with AsyncWebCrawler(config=BrowserConfig(cdp_url=CDP_URL)) as crawler:
            result = await crawler.arun(url=volume_url)

            metadata = await extract_volume_metadata(result.markdown)

            # Paginate through items
            page = 1
            while True:
                page_url = f"{volume_url}?page={page}"
                page_result = await crawler.arun(url=page_url)

                items = await extract_items(page_result.markdown)
                if not items:
                    break

                for item in items:
                    results.append({
                        "volume_id": volume_id,
                        "page": page,
                        "school": metadata["school"],
                        "county": metadata["county"],
                        "transcription_pct": metadata.get("transcription_pct"),
                        **item
                    })

                page += 1

    return results
```

### 4.3 Hidden Heritages Integration

The hiddenheritages.ai project links Irish and Scottish folklore with Aarne-Thompson classification:

```python
# Cross-reference with Hidden Heritages
async def enrich_with_folklore_types(duchas_items: list[dict]):
    """Add AT folktale type classification from Hidden Heritages."""

    # Hidden Heritages provides AT type metadata
    async with AsyncWebCrawler() as crawler:
        hh_result = await crawler.arun(
            url="https://www.hiddenheritages.ai/ga/s",
            config=CrawlerRunConfig(
                fit_markdown=True,
                extraction_strategy=JsonCssExtractionStrategy(
                    schema={
                        "stories": [{
                            "title": "h3.story-title",
                            "at_type": "span.at-classification",
                            "country": "span.country-tag"
                        }]
                    }
                )
            )
        )

        # Build lookup table
        at_lookup = {s["title"]: s["at_type"] for s in hh_result.extracted}

        # Enrich duchas items
        for item in duchas_items:
            if item["title"] in at_lookup:
                item["at_type"] = at_lookup[item["title"]]

        return duchas_items
```

---

## 5. Teanglann.ie: Pronunciation Database

### 5.1 URL Pattern Analysis

Teanglann uses predictable URL structures for audio:

| Dialect | Directory | Example |
|---------|-----------|---------|
| Ulster | `/CanU/` | `/CanU/abhainn.mp3` |
| Connacht | `/CanC/` | `/CanC/abhainn.mp3` |
| Munster | `/CanM/` | `/CanM/abhainn.mp3` |

### 5.2 Speculative Download Strategy

```python
import aiohttp
import urllib.parse

async def harvest_teanglann_audio(words: list[str]):
    """
    Download Teanglann audio using predictable URL patterns.
    """
    dialects = ["CanU", "CanC", "CanM"]
    base_url = "https://www.teanglann.ie"

    async with aiohttp.ClientSession() as session:
        for word in words:
            # URL encode for fada characters (á, é, í, ó, ú)
            encoded_word = urllib.parse.quote(word)

            for dialect in dialects:
                url = f"{base_url}/{dialect}/{encoded_word}.mp3"

                # HEAD request first to check existence
                async with session.head(url) as response:
                    if response.status == 200:
                        # Download the file
                        async with session.get(url) as audio_response:
                            content = await audio_response.read()

                            # Save with metadata
                            save_audio(
                                content=content,
                                word=word,
                                dialect=dialect,
                                checksum=hashlib.md5(content).hexdigest()
                            )
                    elif response.status == 404:
                        # Audio not available for this dialect
                        log.info(f"No {dialect} audio for: {word}")
```

### 5.3 Index Traversal

```python
# Crawl alphabetical index to build word list
async def build_word_index():
    """Build comprehensive word list from Teanglann index."""
    words = []

    async with AsyncWebCrawler() as crawler:
        for letter in "abcdefghilmnoprstu":  # Irish alphabet
            index_url = f"https://www.teanglann.ie/en/fuaim/_{letter}"

            result = await crawler.arun(url=index_url)

            # Extract word links
            soup = BeautifulSoup(result.html, "html.parser")
            word_links = soup.select("a[href*='/en/fuaim/']")

            for link in word_links:
                word = link.get_text().strip()
                if word and word != letter.upper():
                    words.append(word)

            # Check for sub-indices (e.g., ACH, ACU for 'a')
            sub_indices = soup.select(".sub-index a")
            for sub in sub_indices:
                sub_url = f"https://www.teanglann.ie{sub['href']}"
                sub_result = await crawler.arun(url=sub_url)
                # ... extract words from sub-index

    return words
```

---

## 6. sources.yaml Configuration

```yaml
# Comprehensive Celtic Educational Sources Configuration
groups:
  - id: irish_educational_framework
    description: "Primary and Post-Primary Curriculum Specifications"
    targets:
      - url: "https://www.curriculumonline.ie/Primary/Curriculum-Areas/"
        name: "Irish Primary Curriculum"
        type: "Type_A_Hierarchical"
        depth: 2
        content_types: ["pdf", "html_toolkit"]
        priority: high

      - url: "https://ncca.ie/en/junior-cycle/subjects/"
        name: "NCCA Junior Cycle Subjects"
        type: "Type_A_Hierarchical"
        priority: high

  - id: examination_archives
    description: "State Examination Papers and Marking Schemes"
    targets:
      - url: "https://www.examinations.ie/exammaterialarchive/"
        name: "SEC Exam Archive"
        type: "Type_B_Form"
        inputs:
          years: [2024, 2023, 2022, 2021, 2020]
          subjects: ["Mathematics", "English", "Irish", "Physics", "Chemistry"]
          levels: ["Higher", "Ordinary", "Foundation"]
        priority: critical

  - id: celtic_audio_archives
    description: "Dialect and Pronunciation Archives"
    targets:
      - url: "https://www.canuint.ie/ga/"
        name: "Taisce Chanúintí na Gaeilge"
        type: "Type_C_Spatial"
        instruction: "Navigate via Text List only. Do not use Map Canvas."
        priority: high

      - url: "https://www.teanglann.ie/en/fuaim/"
        name: "Teanglann Pronunciation Database"
        type: "Type_A_Hierarchical"
        note: "Use speculative URL construction for audio files"
        priority: medium

  - id: folklore_manuscripts
    description: "Historical Folklore Collections"
    targets:
      - url: "https://www.duchas.ie/en/cbes"
        name: "Schools' Collection"
        type: "Type_D_Sequential"
        pagination_indicator: "Page number / "
        priority: medium

      - url: "https://www.hiddenheritages.ai/ga/s"
        name: "Hidden Heritages (Irish)"
        type: "Type_D_Sequential"
        filters: ["Éire"]
        priority: medium
```

---

## 7. Data Organization Schema

### 7.1 Directory Structure

```
/Irish_Educational_Archive/
├── /Examinations/
│   ├── /Mathematics/
│   │   ├── /2024/
│   │   │   ├── Higher_Question_Paper.pdf
│   │   │   ├── Higher_Marking_Scheme.pdf
│   │   │   └── metadata.json
│   │   └── /2023/
│   └── /Irish/
├── /Curriculum/
│   ├── /Primary/
│   │   ├── Mathematics_Specification.pdf
│   │   └── Language_Specification.pdf
│   └── /Junior_Cycle/
├── /Audio_Corpora/
│   ├── /Teanglann/
│   │   ├── /Ulster/
│   │   ├── /Connacht/
│   │   └── /Munster/
│   └── /Canuint/
│       ├── /Ulster/
│       │   └── /Donegal/
│       └── /Munster/
│           └── /Kerry/
└── /Folklore/
    ├── /Schools_Collection/
    │   └── /CBES_0001/
    └── /Hidden_Heritages/
```

### 7.2 Metadata Schema

```python
from pydantic import BaseModel
from datetime import date
from typing import Optional

class ExamPaper(BaseModel):
    subject: str
    year: int
    level: str  # Higher, Ordinary, Foundation
    paper_type: str  # Question Paper, Marking Scheme
    language: str  # English, Irish
    file_path: str
    source_url: str
    download_date: date
    checksum: str

class CurriculumDocument(BaseModel):
    name: str
    level: str  # Primary, Junior Cycle, Senior Cycle
    subject_area: str
    version: str  # Draft, Final
    effective_year: int
    file_path: str
    source_url: str

class AudioRecording(BaseModel):
    word: str
    dialect: str
    speaker: Optional[str]
    location: Optional[str]
    year_recorded: Optional[int]
    file_path: str
    source_url: str
    duration_ms: Optional[int]
    sample_rate: Optional[int]
```

---

## 8. Implementation Priorities

### Phase 1: Core Archives
1. Set up examinations.ie Stagehand workflow
2. Implement subject/year iteration logic
3. Configure download and metadata storage

### Phase 2: Curriculum Documents
1. Deploy Crawl4AI for ncca.ie/curriculumonline.ie
2. Implement hierarchical traversal
3. Extract PDF links and download

### Phase 3: Audio Corpora
1. Build Teanglann word index
2. Implement speculative audio download
3. Configure canuint.ie text-based navigation

### Phase 4: Folklore Integration
1. Set up duchas.ie paginated crawler
2. Implement Hidden Heritages cross-reference
3. Add AT type classification

---

## References

- examinations.ie: https://www.examinations.ie
- NCCA: https://ncca.ie
- Curriculum Online: https://www.curriculumonline.ie
- Canuint.ie: https://www.canuint.ie
- Teanglann.ie: https://www.teanglann.ie
- Dúchas.ie: https://www.duchas.ie
- Hidden Heritages: https://www.hiddenheritages.ai
