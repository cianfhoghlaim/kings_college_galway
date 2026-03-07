# Gaois API Reference

## Overview

The Gaois Research Group at Dublin City University maintains Ireland's most comprehensive digital Irish language resources. This document provides technical reference for accessing their APIs.

**Developer Hub:** https://www.gaois.ie/en/technology/developers/
**Documentation:** https://docs.gaois.ie/en/data/getting-started
**Contact:** gaois@dcu.ie

---

## 1. Authentication

### 1.1 Methods

Three authentication approaches are supported:

| Method | Example |
|--------|---------|
| **HTTP Header** | `X-Api-Key: <API_KEY>` |
| **Query Parameter** | `?apiKey=<API_KEY>` |
| **HTTP Basic Auth** | `https://API_KEY@www.logainm.ie/...` |

### 1.2 Configuration

```python
import os
from dataclasses import dataclass

@dataclass
class GaoisConfig:
    api_key: str = os.getenv("GAOIS_API_KEY", "")
    base_urls: dict = None

    def __post_init__(self):
        self.base_urls = {
            "logainm": "https://www.logainm.ie/api/v1.0",
            "duchas": "https://www.duchas.ie/api/v0.6"
        }

    @property
    def headers(self) -> dict:
        return {"X-Api-Key": self.api_key}
```

---

## 2. Logainm API v1.0 (Placenames)

### 2.1 Overview

| Property | Value |
|----------|-------|
| **Endpoint** | https://www.logainm.ie/api/v1.0 |
| **Documentation** | https://docs.gaois.ie/en/data/logainm/v1.0/api |
| **Status** | Production |
| **Content** | 100,000+ placenames |

### 2.2 Endpoints

```
GET /api/v1.0/placenames
GET /api/v1.0/placenames/{id}
GET /api/v1.0/search?q={query}
GET /api/v1.0/places/{id}/persons
```

### 2.3 Response Schema

```json
{
  "id": 37704,
  "nameGA": "Baile Hein",
  "nameEN": "Hayestown",
  "category": "townland",
  "coordinates": {
    "latitude": 53.5285,
    "longitude": -6.8542
  },
  "county": "Meath",
  "variants": [
    {"form": "Baile Héin", "language": "ga", "historical": false}
  ]
}
```

### 2.4 Usage Example

```python
import httpx

async def fetch_placenames(config: GaoisConfig, page: int = 1):
    """Fetch placenames with pagination."""
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{config.base_urls['logainm']}/placenames",
            headers=config.headers,
            params={"page": page, "per_page": 100}
        )
        response.raise_for_status()
        return response.json()
```

### 2.5 Key Features

- Search by Irish/English name
- Geographic filtering by county
- Biographical data links (connections to ainm.ie)
- Historical variants and etymology
- Pronunciation guides

---

## 3. Duchas API v0.6 (Folklore)

### 3.1 Overview

| Property | Value |
|----------|-------|
| **Endpoint** | https://www.duchas.ie/api/v0.6 |
| **Documentation** | https://docs.gaois.ie/en/data/duchas/v0.6/api |
| **GitHub** | https://github.com/gaois/DuchasAPI-docs |
| **Status** | Beta (v0.6) |

### 3.2 Collections

#### Main Manuscript Collection (CBE)
- 2,400 bound volumes
- Material collected since 1932
- Bilingual content (Irish & English)

#### Schools' Collection (CBES)
- **740,000 pages** of folklore
- Collected 1937-1939
- 5,000 primary schools

#### Photographic Collection (CBEG)
- **80,000 photographs**
- Visual documentation
- Bilingual metadata

### 3.3 Endpoints

```
GET /api/v0.6/collections
GET /api/v0.6/stories
GET /api/v0.6/stories/{id}
GET /api/v0.6/search?language=ga&county=Cork
```

### 3.4 Language Filtering

| Parameter | Values |
|-----------|--------|
| `language` | `ga` (Irish), `en` (English) |
| `county` | Any Irish county name |
| `topic` | Folklore classification |

~66% content is in Irish, ~33% in English.

### 3.5 Usage Example

```python
async def fetch_folklore_stories(
    config: GaoisConfig,
    language: str = "ga",
    county: str = None
):
    """Fetch folklore stories with filters."""
    params = {"language": language, "per_page": 100}
    if county:
        params["county"] = county

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{config.base_urls['duchas']}/stories",
            headers=config.headers,
            params=params
        )
        response.raise_for_status()
        return response.json()
```

---

## 4. Tearma.ie (Terminology)

### 4.1 Overview

| Property | Value |
|----------|-------|
| **Website** | https://www.tearma.ie/ |
| **API** | Not officially documented |
| **Content** | National terminology database |
| **Categories** | 40+ subject domains |

### 4.2 Content Categories

- Legal terminology
- Medical terms
- Technical vocabulary
- Sports terminology
- EU terminology
- COVID-19 terms

### 4.3 Access Strategy

```python
# Check for API availability
import httpx

async def check_tearma_api():
    """Probe for API endpoint."""
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get("https://www.tearma.ie/api/")
            return response.status_code == 200
        except:
            return False

# Fallback: scraping downloadable lists
TEARMA_DOWNLOADS = "https://www.tearma.ie/ioslodail/"
```

### 4.4 Export Options

- Downloadable lists: `/ioslodail/`
- Content syndication: `/breiseain/`
- Category browsing: `/dom/ga/`

---

## 5. Direct Downloads

### 5.1 Parallel Corpus (TMX)

| Property | Value |
|----------|-------|
| **URL** | https://www.gaois.ie/en/corpora/parallel/data |
| **Format** | TMX (Translation Memory eXchange) |
| **Size** | ~130.5 million words |

**Content:**
- Irish: 68.0 million words
- English: 62.5 million words
- EU legislation, Constitution, Acts of Oireachtas

```python
from translate.tools import tmxfile

def parse_tmx(filepath: str):
    """Parse TMX file to extract parallel segments."""
    with open(filepath, 'rb') as f:
        tmx = tmxfile.tmxfile(f)
        for unit in tmx.units:
            yield {
                "source": unit.source,
                "target": unit.target,
                "id": unit.getid()
            }
```

### 5.2 Corpas.ie Word Lists

| Property | Value |
|----------|-------|
| **URL** | https://www.corpas.ie/en/extras/word-lists/ |
| **Format** | TAB-separated (ZIP) |
| **Content** | Word frequency lists |

**Corpora Available:**
- National Corpus of Irish (CNG): 100M words
- Corpus of Written Irish: 131M words
- Corpus of Spoken Irish: 9M words
- Historical Corpus: 3,000+ texts

---

## 6. GitHub Repositories

### 6.1 gaoisalign

| Property | Value |
|----------|-------|
| **Repository** | https://github.com/gaois/gaoisalign |
| **Language** | Python |
| **License** | MIT |
| **Purpose** | Text alignment for parallel corpora |

```bash
git clone https://github.com/gaois/gaoisalign.git
```

### 6.2 sloinnte (Surnames)

| Property | Value |
|----------|-------|
| **Repository** | https://github.com/gaois/sloinnte |
| **Language** | XSLT |
| **License** | MIT |
| **Purpose** | Irish surnames database |

### 6.3 Terminologue

| Property | Value |
|----------|-------|
| **Repository** | https://github.com/gaois/terminologue |
| **Language** | JavaScript |
| **Stars** | 59 |
| **Purpose** | Terminology management (powers Tearma.ie) |

---

## 7. Complete API Collector

```python
#!/usr/bin/env python3
"""
Gaois API Data Collector
Comprehensive collection from all Gaois APIs
"""

import asyncio
import aiohttp
import json
from pathlib import Path
from typing import AsyncIterator, Dict, List

class GaoisCollector:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.base_urls = {
            "logainm": "https://www.logainm.ie/api/v1.0",
            "duchas": "https://www.duchas.ie/api/v0.6"
        }
        self.headers = {"X-Api-Key": api_key}

    async def _fetch_paginated(
        self,
        session: aiohttp.ClientSession,
        url: str,
        params: dict = None
    ) -> AsyncIterator[Dict]:
        """Generic paginated fetch."""
        page = 1
        params = params or {}

        while True:
            params["page"] = page
            params["per_page"] = 100

            async with session.get(
                url,
                headers=self.headers,
                params=params
            ) as response:
                if response.status != 200:
                    break

                data = await response.json()
                results = data.get("results", [])

                if not results:
                    break

                for item in results:
                    yield item

                page += 1
                await asyncio.sleep(0.5)  # Rate limiting

    async def collect_placenames(self) -> List[Dict]:
        """Collect all placenames from Logainm."""
        async with aiohttp.ClientSession() as session:
            url = f"{self.base_urls['logainm']}/placenames"
            return [p async for p in self._fetch_paginated(session, url)]

    async def collect_folklore(
        self,
        language: str = None
    ) -> List[Dict]:
        """Collect folklore from Duchas."""
        async with aiohttp.ClientSession() as session:
            url = f"{self.base_urls['duchas']}/stories"
            params = {}
            if language:
                params["language"] = language
            return [s async for s in self._fetch_paginated(session, url, params)]

    async def collect_all(self) -> Dict[str, List]:
        """Collect from all sources."""
        placenames, folklore_ga, folklore_en = await asyncio.gather(
            self.collect_placenames(),
            self.collect_folklore("ga"),
            self.collect_folklore("en")
        )

        return {
            "placenames": placenames,
            "folklore_irish": folklore_ga,
            "folklore_english": folklore_en
        }

    def save(self, data: Dict, output_dir: Path):
        """Save collected data to JSONL files."""
        output_dir.mkdir(exist_ok=True)

        for name, records in data.items():
            filepath = output_dir / f"{name}.jsonl"
            with filepath.open("w", encoding="utf-8") as f:
                for record in records:
                    f.write(json.dumps(record, ensure_ascii=False) + "\n")
            print(f"Saved {len(records)} records to {filepath}")


async def main():
    import os

    api_key = os.getenv("GAOIS_API_KEY")
    if not api_key:
        print("Set GAOIS_API_KEY environment variable")
        return

    collector = GaoisCollector(api_key)
    data = await collector.collect_all()
    collector.save(data, Path("./gaois_data"))


if __name__ == "__main__":
    asyncio.run(main())
```

---

## 8. Dataset Size Estimates

| Source | Words (Irish) | Words (English) | Items | Method |
|--------|---------------|-----------------|-------|--------|
| **Parallel Corpus** | 68M | 62.5M | 130M segments | Download |
| **Corpas.ie** | 240M | - | - | Download |
| **Duchas API** | ~50M | ~30M | 80,000+ | API |
| **Logainm API** | - | - | 100,000+ | API |
| **Ainm.ie** | 1.3M | - | 1,785 | Scrape |
| **Tearma.ie** | 100K+ | 100K+ | 10,000+ | API/Scrape |
| **Total** | **359M+** | **93M+** | **200K+ items** | Mixed |

---

## References

- Gaois Developer Hub: https://www.gaois.ie/en/technology/developers/
- API Documentation: https://docs.gaois.ie/en/data/getting-started
- Duchas API Docs: https://github.com/gaois/DuchasAPI-docs
- Parallel Corpus: https://www.gaois.ie/en/corpora/parallel/data
