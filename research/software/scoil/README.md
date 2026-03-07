# Celtic Education Scraping Agent

Agentic web scraping pipeline for multilingual Celtic education resources across the British Isles.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Observation Layer                             │
│  Browserbase CDP → Screenshots + DOM capture                     │
│  Firecrawl → Site mapping + batch scraping                      │
│  Crawl4AI → Markdown extraction + structured data               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Perception Layer                              │
│  Z.ai GLM 4.6v → Visual analysis + multilingual OCR             │
│  BAML → Schema-driven extraction                                │
│  Language Detection → ISO 639-1 tagging (ga/gd/cy/gv/kw/en)     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Cognition Layer                               │
│  Cognee → Knowledge graph persistence                           │
│  Memgraph → Curriculum relationship mapping                     │
│  LanceDB → Vector embeddings (BGE-M3 multilingual)              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Storage Layer                                 │
│  Cloudflare R2 → Raw PDFs, images                               │
│  DuckDB → Local analytical queries                              │
│  LanceDB Cloud → Production vector search                       │
└─────────────────────────────────────────────────────────────────┘
```

## Languages & Regions

| Language | ISO Code | Region | Primary Sources |
|----------|----------|--------|-----------------|
| Irish (Gaeilge) | ga | Ireland, NI | NCCA, SEC, CCEA |
| Scottish Gaelic (Gàidhlig) | gd | Scotland | SQA, Education Scotland, Stòrlann |
| Welsh (Cymraeg) | cy | Wales | WJEC, Qualifications Wales, Hwb |
| Manx (Gaelg) | gv | Isle of Man | DESC, Bunscoill Ghaelgagh |
| Cornish (Kernewek) | kw | Cornwall | Go Cornish, Kesva |
| English | en | All | All sources |

## Directory Structure

```
scraping_agent/
├── README.md                 # This file
├── agents/
│   ├── site_mapper.py       # Firecrawl site mapping
│   ├── content_extractor.py # Browserbase + Z.ai extraction
│   └── language_detector.py # FastText language detection
├── schemas/
│   ├── education.baml       # BAML extraction schemas
│   └── curriculum.baml      # Curriculum-specific schemas
├── sources/
│   ├── sample_data/
│   │   ├── ga/              # Irish samples
│   │   ├── gd/              # Scottish Gaelic samples
│   │   ├── cy/              # Welsh samples
│   │   ├── gv/              # Manx samples
│   │   ├── kw/              # Cornish samples
│   │   └── en/              # English samples
│   └── metadata/            # Source metadata JSONs
└── pipelines/
    └── dagster_assets.py    # Dagster orchestration
```

## Source Metadata Schema

All scraped content follows this JSON structure:

```json
{
  "source_id": "sqa_gaelic_n5",
  "region": "scotland",
  "language": {
    "primary": "gd",
    "secondary": ["en"]
  },
  "content": {
    "markdown": "...",
    "extracted_text": "..."
  },
  "metadata": {
    "title": {"gd": "...", "en": "..."},
    "url": "https://...",
    "og:locale": "gd_GB",
    "document_type": "specification",
    "education_level": "national_5",
    "subject": "gaidhlig",
    "scraped_at": "2025-12-18T...",
    "source_org": "SQA"
  },
  "provenance": {
    "scrapeId": "uuid",
    "tool": "firecrawl|browserbase|crawl4ai",
    "creditsUsed": 1
  }
}
```

## Pipeline Stages

### Stage 1: Site Mapping (Firecrawl)

```python
# Map all URLs for a source
from firecrawl import FirecrawlApp

app = FirecrawlApp(api_key=os.environ["FIRECRAWL_API_KEY"])

result = app.map_url(
    url="https://www.sqa.org.uk/sqa/45347.html",
    params={
        "includeSubdomains": True,
        "limit": 5000
    }
)
```

### Stage 2: Content Extraction (Crawl4AI + Browserbase)

```python
# Extract content with multilingual support
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig

async with AsyncWebCrawler(
    browser_config=BrowserConfig(headless=True)
) as crawler:
    result = await crawler.arun(
        url=url,
        config=CrawlerRunConfig(
            word_count_threshold=10,
            remove_overlay_elements=True,
            process_iframes=True
        )
    )
    markdown = result.markdown
```

### Stage 3: Visual Analysis (Z.ai GLM 4.6v)

For complex layouts, PDF tables, and handwritten content:

```python
# Via Browserbase CDP
from browserbase import Browserbase

bb = Browserbase(api_key=os.environ["BROWSERBASE_API_KEY"])
session = bb.sessions.create(project_id=os.environ["BROWSERBASE_PROJECT_ID"])

# Capture screenshot
screenshot = client.send("Page.captureScreenshot", {
    "format": "png",
    "fullPage": True
})

# Analyze with Z.ai
# Uses MCP server for visual understanding
```

### Stage 4: Language Detection

```python
import fasttext

model = fasttext.load_model('lid.176.bin')

def detect_language(text):
    predictions = model.predict(text, k=3)
    return {
        "primary": predictions[0][0].replace("__label__", ""),
        "confidence": predictions[1][0],
        "alternatives": [
            {"lang": p.replace("__label__", ""), "conf": c}
            for p, c in zip(predictions[0][1:], predictions[1][1:])
        ]
    }
```

### Stage 5: Vector Embedding (BGE-M3)

```python
import lancedb
from sentence_transformers import SentenceTransformer

# BGE-M3 supports 100+ languages including Celtic
model = SentenceTransformer('BAAI/bge-m3')

def embed_document(doc):
    return model.encode(doc["content"]["markdown"])

# Store in LanceDB
db = lancedb.connect("celtic_education")
table = db.create_table("documents", data=[
    {
        "vector": embed_document(doc),
        "source_id": doc["source_id"],
        "language": doc["language"]["primary"],
        "text": doc["content"]["markdown"][:1000]
    }
])
```

## BAML Extraction Schemas

Education content extraction uses typed BAML schemas:

```baml
// schemas/education.baml
class CurriculumSpecification {
    title BilingualText
    subject string
    level EducationLevel
    outcomes LearningOutcome[]
    assessment AssessmentCriteria?
}

class BilingualText {
    en string?
    ga string?
    gd string?
    cy string?
    gv string?
    kw string?
}

enum EducationLevel {
    PRIMARY
    JUNIOR_CYCLE
    SENIOR_CYCLE
    NATIONAL_3
    NATIONAL_4
    NATIONAL_5
    HIGHER
    ADVANCED_HIGHER
    GCSE
    A_LEVEL
}

class LearningOutcome {
    code string
    description BilingualText
    strand string?
}
```

## Configuration

Sources are defined in `config/sources_british_isles.yaml`:

```yaml
scotland:
  sqa:
    name: "Scottish Qualifications Authority"
    base_url: "https://www.sqa.org.uk"
    language_support: ["en", "gd"]
    endpoints:
      gaelic_hub:
        url: "/sqa/45347.html"
        document_types: ["qualification_spec"]
        priority: high
```

NLP resources are defined in `config/resources.yaml`:

```yaml
irish:
  language_models:
    - name: gaBERT
      huggingface: DCU-NLP/bert-base-irish-cased-v1
  corpora:
    - name: Corpas Náisiúnta na Gaeilge
      url: https://www.corpas.ie/ga/cng/
```

## ToS Compliance

Before scraping any source, review Terms of Service:

1. Check `robots.txt`
2. Review ToS for crawling permissions
3. Document in `sources/metadata/{source}_tos.json`
4. Mark sources as `allowed`, `conditional`, or `blocked`

Template:
```json
{
  "source": "SQA",
  "url": "https://www.sqa.org.uk",
  "robots_txt": "Allow",
  "crawling_allowed": true,
  "commercial_use": false,
  "attribution_required": true,
  "notes": "Educational use permitted"
}
```

## Running the Pipeline

### Prerequisites

```bash
# Required environment variables
export FIRECRAWL_API_KEY=...
export BROWSERBASE_API_KEY=...
export BROWSERBASE_PROJECT_ID=...
export Z_AI_API_KEY=...
export LANCEDB_URI=...
```

### Site Mapping (Day 1-2)

```bash
# Map all sources
python agents/site_mapper.py --config config/sources_british_isles.yaml

# Output: sources/metadata/site_maps/{region}/{source}.json
```

### Sample Collection (Day 3-4)

```bash
# Collect samples from each source
python agents/content_extractor.py \
  --mode sample \
  --samples-per-source 10 \
  --output sources/sample_data/
```

### Full Extraction (Day 5-7)

```bash
# Full batch extraction
python agents/content_extractor.py \
  --mode full \
  --config config/sources_british_isles.yaml \
  --output ../data/education/
```

## Monitoring (Datadog)

The pipeline emits metrics to Datadog:

- `scraping.pages_processed` - Pages scraped
- `scraping.credits_used` - API credits consumed
- `scraping.language_distribution` - Documents per language
- `scraping.errors` - Extraction errors

## Credits Budget

| Provider | Budget | Purpose |
|----------|--------|---------|
| Firecrawl | $20 | Site mapping |
| Browserbase | $50 | Visual capture |
| Modal | $100 | Batch processing |
| LanceDB Cloud | $100 | Vector storage |

## References

- [Crawl4AI Documentation](https://crawl4ai.com)
- [Firecrawl API](https://docs.firecrawl.dev)
- [Browserbase CDP](https://docs.browserbase.com)
- [BAML Documentation](https://docs.boundaryml.com)
- [DR-LIB CLARIN Resources](https://www.clarin.ac.uk/article/digital-resources-languages-ireland-and-britain)
