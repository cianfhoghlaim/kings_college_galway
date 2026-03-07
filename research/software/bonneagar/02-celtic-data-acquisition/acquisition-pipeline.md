# Data Acquisition Pipeline Implementation

## Overview

This document provides technical implementation patterns for Celtic language data acquisition pipelines using dlt (data load tool), asyncio, and modern Python patterns.

---

## 1. Pipeline Architecture

### 1.1 Layer Model

```
+----------------------------------------------------------+
|                  DATA ACQUISITION LAYER                   |
+------------+------------+------------+-------------------+
|  GitHub    |  API       |  Direct    |  Web Scraping    |
|  Clone     |  Access    |  Download  |  (crawl4ai)      |
+-----+------+-----+------+-----+------+-----+------------+
      |            |            |            |
      v            v            v            v
+----------------------------------------------------------+
|                   PROCESSING LAYER                        |
+------------+------------+------------+-------------------+
|  Parse TMX |  Parse     |  Extract   |  Align Texts     |
|  to pairs  |  JSON      |  Markdown  |  (gaoisalign)    |
+-----+------+-----+------+-----+------+-----+------------+
      |            |            |            |
      v            v            v            v
+----------------------------------------------------------+
|                  NORMALIZATION LAYER                      |
|  - Standardize encoding (UTF-8)                          |
|  - Normalize Irish orthography (old -> modern)           |
|  - Clean HTML artifacts                                   |
|  - Tokenize sentences                                     |
|  - Align parallel segments                                |
+-----------------------------+----------------------------+
                              |
                              v
+----------------------------------------------------------+
|                     STORAGE LAYER                         |
+------------+------------+------------+-------------------+
|  DuckDB    |  JSONL     |  Parquet   |  HuggingFace     |
|  (spatial) |  (stream)  |  (column)  |  Datasets        |
+------------+------------+------------+-------------------+
```

### 1.2 Acquisition Priority Matrix

| Phase | Method | Sources | Priority |
|-------|--------|---------|----------|
| **1** | API Access | Logainm, Duchas | Highest |
| **2** | Direct Download | TMX, Corpas.ie | High |
| **3** | GitHub Clone | gaoisalign, sloinnte | Medium |
| **4** | Web Scraping | Tearma, ainm.ie | Lower |

---

## 2. dlt Integration Patterns

### 2.1 Basic dlt Source

```python
import dlt
from dlt.sources.helpers import requests

@dlt.source
def gaois_source(api_key: str = dlt.secrets.value):
    """Source for Gaois API endpoints."""

    @dlt.resource(write_disposition="replace")
    def placenames():
        """Fetch placenames from Logainm API."""
        base_url = "https://www.logainm.ie/api/v1.0"
        headers = {"X-Api-Key": api_key}

        page = 1
        while True:
            response = requests.get(
                f"{base_url}/placenames",
                headers=headers,
                params={"page": page, "per_page": 100}
            )
            data = response.json()

            if not data.get("results"):
                break

            yield from data["results"]
            page += 1

    @dlt.resource(write_disposition="replace")
    def folklore(language: str = "ga"):
        """Fetch folklore from Duchas API."""
        base_url = "https://www.duchas.ie/api/v0.6"
        headers = {"X-Api-Key": api_key}

        page = 1
        while True:
            response = requests.get(
                f"{base_url}/stories",
                headers=headers,
                params={"language": language, "page": page, "per_page": 100}
            )
            data = response.json()

            if not data.get("results"):
                break

            yield from data["results"]
            page += 1

    return placenames, folklore
```

### 2.2 Incremental Loading

```python
@dlt.resource(write_disposition="merge", primary_key="id")
def placenames_incremental(
    last_modified: dlt.sources.incremental[str] = dlt.sources.incremental(
        "updated_at",
        initial_value="2020-01-01"
    )
):
    """Incrementally load placenames since last update."""
    base_url = "https://www.logainm.ie/api/v1.0"

    response = requests.get(
        f"{base_url}/placenames",
        params={"modified_since": last_modified.last_value}
    )

    for item in response.json().get("results", []):
        yield item
```

### 2.3 Transformer Pattern

```python
@dlt.transformer(data_from=placenames)
def normalize_placenames(items):
    """Normalize placename data for storage."""
    for item in items:
        yield {
            "id": item["id"],
            "name_irish": item.get("nameGA", ""),
            "name_english": item.get("nameEN", ""),
            "category": item.get("category", ""),
            "county": item.get("county", ""),
            "latitude": item.get("coordinates", {}).get("latitude"),
            "longitude": item.get("coordinates", {}).get("longitude"),
            "variants": item.get("variants", [])
        }
```

---

## 3. Async Collection Patterns

### 3.1 Rate-Limited Fetcher

```python
import asyncio
import aiohttp
from typing import AsyncIterator, Dict, List

class RateLimitedFetcher:
    """Fetch data with rate limiting and retry logic."""

    def __init__(
        self,
        api_key: str,
        requests_per_second: float = 1.0,
        max_retries: int = 3
    ):
        self.api_key = api_key
        self.delay = 1.0 / requests_per_second
        self.max_retries = max_retries
        self.semaphore = asyncio.Semaphore(5)

    async def fetch(
        self,
        session: aiohttp.ClientSession,
        url: str,
        params: dict = None
    ) -> Dict:
        """Fetch with retry and rate limiting."""
        async with self.semaphore:
            for attempt in range(self.max_retries):
                try:
                    async with session.get(
                        url,
                        headers={"X-Api-Key": self.api_key},
                        params=params
                    ) as response:
                        if response.status == 429:
                            await asyncio.sleep(2 ** attempt)
                            continue
                        response.raise_for_status()
                        return await response.json()
                except aiohttp.ClientError as e:
                    if attempt == self.max_retries - 1:
                        raise
                    await asyncio.sleep(2 ** attempt)

            await asyncio.sleep(self.delay)

    async def fetch_paginated(
        self,
        session: aiohttp.ClientSession,
        url: str,
        params: dict = None
    ) -> AsyncIterator[Dict]:
        """Fetch all pages from paginated endpoint."""
        params = params or {}
        page = 1

        while True:
            params["page"] = page
            params["per_page"] = 100

            data = await self.fetch(session, url, params)
            results = data.get("results", [])

            if not results:
                break

            for item in results:
                yield item

            page += 1
```

### 3.2 Parallel Collection

```python
async def collect_all_sources(api_key: str) -> Dict[str, List]:
    """Collect from all Gaois sources in parallel."""
    fetcher = RateLimitedFetcher(api_key)

    async with aiohttp.ClientSession() as session:
        # Define collection tasks
        tasks = {
            "placenames": collect_placenames(fetcher, session),
            "folklore_ga": collect_folklore(fetcher, session, "ga"),
            "folklore_en": collect_folklore(fetcher, session, "en")
        }

        # Run concurrently
        results = {}
        for name, coro in tasks.items():
            results[name] = [item async for item in coro]

        return results

async def collect_placenames(
    fetcher: RateLimitedFetcher,
    session: aiohttp.ClientSession
) -> AsyncIterator[Dict]:
    """Collect placenames."""
    url = "https://www.logainm.ie/api/v1.0/placenames"
    async for item in fetcher.fetch_paginated(session, url):
        yield item

async def collect_folklore(
    fetcher: RateLimitedFetcher,
    session: aiohttp.ClientSession,
    language: str
) -> AsyncIterator[Dict]:
    """Collect folklore by language."""
    url = "https://www.duchas.ie/api/v0.6/stories"
    async for item in fetcher.fetch_paginated(session, url, {"language": language}):
        yield item
```

---

## 4. TMX Processing Pipeline

### 4.1 TMX Parser

```python
from pathlib import Path
from typing import Iterator, Dict
import xml.etree.ElementTree as ET

def parse_tmx(filepath: Path) -> Iterator[Dict]:
    """Parse TMX file to extract parallel segments."""
    tree = ET.parse(filepath)
    root = tree.getroot()

    for tu in root.findall(".//tu"):
        segment = {"id": tu.get("tuid", "")}

        for tuv in tu.findall("tuv"):
            lang = tuv.get("{http://www.w3.org/XML/1998/namespace}lang", "")
            seg = tuv.find("seg")

            if seg is not None and seg.text:
                if lang.startswith("ga"):
                    segment["irish"] = seg.text.strip()
                elif lang.startswith("en"):
                    segment["english"] = seg.text.strip()

        if "irish" in segment and "english" in segment:
            yield segment

def process_tmx_directory(tmx_dir: Path) -> Iterator[Dict]:
    """Process all TMX files in directory."""
    for tmx_file in tmx_dir.glob("*.tmx"):
        for segment in parse_tmx(tmx_file):
            segment["source_file"] = tmx_file.name
            yield segment
```

### 4.2 TMX to dlt Resource

```python
@dlt.resource(write_disposition="replace")
def parallel_corpus(tmx_directory: str):
    """Load parallel corpus from TMX files."""
    tmx_path = Path(tmx_directory)

    for segment in process_tmx_directory(tmx_path):
        yield {
            "id": segment.get("id", ""),
            "irish": segment.get("irish", ""),
            "english": segment.get("english", ""),
            "source": segment.get("source_file", ""),
            "domain": "legal"  # Gaois corpus is legal/legislative
        }
```

---

## 5. Storage Patterns

### 5.1 DuckDB Destination

```python
import dlt

def run_pipeline():
    """Run acquisition pipeline to DuckDB."""
    pipeline = dlt.pipeline(
        pipeline_name="celtic_data",
        destination="duckdb",
        dataset_name="gaois"
    )

    # Load data
    info = pipeline.run(gaois_source())
    print(f"Loaded: {info}")

    # Query results
    with pipeline.sql_client() as client:
        result = client.execute_sql(
            "SELECT COUNT(*) FROM placenames"
        )
        print(f"Placenames: {result[0][0]}")
```

### 5.2 JSONL Export

```python
import json
from pathlib import Path

def export_jsonl(data: List[Dict], output_path: Path):
    """Export data to JSONL format."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with output_path.open("w", encoding="utf-8") as f:
        for record in data:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")

def export_all(data: Dict[str, List], output_dir: Path):
    """Export all datasets to JSONL."""
    for name, records in data.items():
        output_path = output_dir / f"{name}.jsonl"
        export_jsonl(records, output_path)
        print(f"Exported {len(records)} records to {output_path}")
```

### 5.3 Parquet Export

```python
import pyarrow as pa
import pyarrow.parquet as pq
from pathlib import Path

def export_parquet(data: List[Dict], output_path: Path):
    """Export data to Parquet format."""
    if not data:
        return

    table = pa.Table.from_pylist(data)
    pq.write_table(
        table,
        output_path,
        compression="snappy"
    )

def export_partitioned(
    data: List[Dict],
    output_dir: Path,
    partition_cols: List[str]
):
    """Export with partitioning."""
    table = pa.Table.from_pylist(data)
    pq.write_to_dataset(
        table,
        output_dir,
        partition_cols=partition_cols,
        compression="snappy"
    )
```

---

## 6. Complete Pipeline Example

```python
#!/usr/bin/env python3
"""
Complete Celtic Data Acquisition Pipeline
Collects from Gaois APIs, TMX files, and web scraping
"""

import asyncio
import dlt
from pathlib import Path
from dataclasses import dataclass
from typing import Optional

@dataclass
class PipelineConfig:
    api_key: str
    tmx_directory: Path
    output_directory: Path
    scrape_enabled: bool = False

class CelticDataPipeline:
    """Unified pipeline for Celtic language data acquisition."""

    def __init__(self, config: PipelineConfig):
        self.config = config
        self.pipeline = dlt.pipeline(
            pipeline_name="celtic_data",
            destination="duckdb",
            dataset_name="gaois"
        )

    def run_api_collection(self):
        """Collect from APIs."""
        source = gaois_source(self.config.api_key)
        info = self.pipeline.run(source)
        print(f"API collection: {info}")
        return info

    def run_tmx_processing(self):
        """Process TMX files."""
        source = parallel_corpus(str(self.config.tmx_directory))
        info = self.pipeline.run(source)
        print(f"TMX processing: {info}")
        return info

    async def run_scraping(self):
        """Run web scraping collection."""
        if not self.config.scrape_enabled:
            return None

        from crawl4ai import AsyncWebCrawler

        async with AsyncWebCrawler() as crawler:
            results = []

            # Scrape tearma.ie terminology
            result = await crawler.arun(
                url="https://www.tearma.ie/dom/ga/",
                extract_structured=True
            )
            results.append({"source": "tearma", "data": result.markdown})

            return results

    def export_results(self):
        """Export to multiple formats."""
        output_dir = self.config.output_directory
        output_dir.mkdir(parents=True, exist_ok=True)

        with self.pipeline.sql_client() as client:
            # Export placenames
            placenames = client.execute_sql("SELECT * FROM placenames")
            export_parquet(placenames, output_dir / "placenames.parquet")

            # Export folklore
            folklore = client.execute_sql("SELECT * FROM folklore")
            export_parquet(folklore, output_dir / "folklore.parquet")

    def run_all(self):
        """Run complete pipeline."""
        print("Starting Celtic data acquisition pipeline...")

        # Phase 1: API collection
        self.run_api_collection()

        # Phase 2: TMX processing
        if self.config.tmx_directory.exists():
            self.run_tmx_processing()

        # Phase 3: Web scraping (optional)
        if self.config.scrape_enabled:
            asyncio.run(self.run_scraping())

        # Phase 4: Export
        self.export_results()

        print("Pipeline complete!")

def main():
    import os

    config = PipelineConfig(
        api_key=os.getenv("GAOIS_API_KEY", ""),
        tmx_directory=Path("./tmx_files"),
        output_directory=Path("./output"),
        scrape_enabled=False
    )

    pipeline = CelticDataPipeline(config)
    pipeline.run_all()

if __name__ == "__main__":
    main()
```

---

## 7. Configuration

### 7.1 Environment Variables

```bash
# .env file
GAOIS_API_KEY=your_api_key_here
DLT_DESTINATION=duckdb
OUTPUT_DIR=./celtic_data
```

### 7.2 dlt secrets.toml

```toml
# .dlt/secrets.toml
[sources.gaois_source]
api_key = "your_api_key_here"

[destination.duckdb]
credentials = "celtic_data.duckdb"
```

---

## 8. Dataset Size Estimates

| Source | Words (Irish) | Words (English) | Items | Method |
|--------|---------------|-----------------|-------|--------|
| **Parallel Corpus** | 68M | 62.5M | 130M segments | TMX Download |
| **Corpas.ie** | 240M | - | - | Word lists |
| **Duchas API** | ~50M | ~30M | 80,000+ | API |
| **Logainm API** | - | - | 100,000+ | API |
| **Ainm.ie** | 1.3M | - | 1,785 | Scrape |
| **Tearma.ie** | 100K+ | 100K+ | 10,000+ | API/Scrape |
| **Total** | **359M+** | **93M+** | **200K+ items** | Mixed |

---

## References

- dlt Documentation: https://dlthub.com/docs
- Gaois Developer Hub: https://www.gaois.ie/en/technology/developers/
- crawl4ai: https://github.com/unclecode/crawl4ai
- DuckDB: https://duckdb.org/docs/
