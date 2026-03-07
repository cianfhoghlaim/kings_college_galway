# TMX File Processing

## Overview

TMX (Translation Memory eXchange) is the standard format for parallel corpora in the translation industry. The Gaois Parallel Corpus provides 130.5 million words in TMX format, making it the largest single source of Irish-English parallel text.

---

## 1. TMX Format Structure

### 1.1 Basic Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE tmx SYSTEM "tmx14.dtd">
<tmx version="1.4">
  <header
    creationtool="Gaois"
    creationtoolversion="1.0"
    datatype="plaintext"
    segtype="sentence"
    adminlang="en"
    srclang="ga"
    o-tmf="unknown">
  </header>
  <body>
    <tu tuid="1">
      <tuv xml:lang="ga">
        <seg>Is é seo an téacs Gaeilge.</seg>
      </tuv>
      <tuv xml:lang="en">
        <seg>This is the Irish text.</seg>
      </tuv>
    </tu>
  </body>
</tmx>
```

### 1.2 Key Elements

| Element | Description |
|---------|-------------|
| `<tmx>` | Root element with version |
| `<header>` | Metadata about the file |
| `<body>` | Contains translation units |
| `<tu>` | Translation unit (segment pair) |
| `<tuv>` | Translation unit variant (language) |
| `<seg>` | Segment text content |

### 1.3 Language Codes

| Code | Language | Usage |
|------|----------|-------|
| `ga` | Irish (Gaeilge) | ISO 639-1 |
| `en` | English | ISO 639-1 |
| `ga-IE` | Irish (Ireland) | BCP 47 |
| `en-IE` | English (Ireland) | BCP 47 |

---

## 2. Parsing Implementation

### 2.1 Basic Parser (xml.etree)

```python
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterator, Dict, Optional

def parse_tmx(filepath: Path) -> Iterator[Dict]:
    """
    Parse TMX file to extract parallel segments.

    Args:
        filepath: Path to TMX file

    Yields:
        Dict with id, irish, english fields
    """
    tree = ET.parse(filepath)
    root = tree.getroot()

    # Handle namespace if present
    ns = {"xml": "http://www.w3.org/XML/1998/namespace"}

    for tu in root.findall(".//tu"):
        segment = {
            "id": tu.get("tuid", ""),
            "changedate": tu.get("changedate", ""),
            "creationdate": tu.get("creationdate", "")
        }

        for tuv in tu.findall("tuv"):
            # Get language from xml:lang attribute
            lang = tuv.get("{http://www.w3.org/XML/1998/namespace}lang", "")

            seg = tuv.find("seg")
            if seg is not None and seg.text:
                text = seg.text.strip()

                if lang.startswith("ga"):
                    segment["irish"] = text
                elif lang.startswith("en"):
                    segment["english"] = text

        # Only yield if both languages present
        if "irish" in segment and "english" in segment:
            yield segment
```

### 2.2 Streaming Parser (Large Files)

```python
from xml.etree.ElementTree import iterparse
from typing import Iterator, Dict

def parse_tmx_streaming(filepath: Path) -> Iterator[Dict]:
    """
    Memory-efficient streaming parser for large TMX files.

    Args:
        filepath: Path to TMX file

    Yields:
        Dict with parallel segments
    """
    context = iterparse(str(filepath), events=("end",))

    current_tu = {}
    for event, elem in context:
        if elem.tag == "seg":
            # Get parent tuv for language
            pass  # Handle in tu processing

        elif elem.tag == "tuv":
            lang = elem.get("{http://www.w3.org/XML/1998/namespace}lang", "")
            seg = elem.find("seg")

            if seg is not None and seg.text:
                if lang.startswith("ga"):
                    current_tu["irish"] = seg.text.strip()
                elif lang.startswith("en"):
                    current_tu["english"] = seg.text.strip()

        elif elem.tag == "tu":
            if "irish" in current_tu and "english" in current_tu:
                current_tu["id"] = elem.get("tuid", "")
                yield current_tu.copy()

            current_tu = {}
            elem.clear()  # Free memory
```

### 2.3 Using translate-toolkit

```python
from translate.storage.tmx import tmxfile
from pathlib import Path
from typing import Iterator, Dict

def parse_with_toolkit(filepath: Path) -> Iterator[Dict]:
    """
    Parse TMX using translate-toolkit library.

    Args:
        filepath: Path to TMX file

    Yields:
        Dict with source, target, id
    """
    with open(filepath, 'rb') as f:
        tmx = tmxfile(f)

        for unit in tmx.units:
            if unit.source and unit.target:
                yield {
                    "id": unit.getid(),
                    "source": unit.source,
                    "target": unit.target,
                    "notes": unit.getnotes()
                }
```

---

## 3. Validation

### 3.1 Segment Validation

```python
from dataclasses import dataclass
from typing import List, Optional

@dataclass
class ValidationResult:
    valid: bool
    errors: List[str]
    warnings: List[str]

def validate_segment(segment: Dict) -> ValidationResult:
    """Validate a single parallel segment."""
    errors = []
    warnings = []

    irish = segment.get("irish", "")
    english = segment.get("english", "")

    # Check for empty content
    if not irish:
        errors.append("Empty Irish segment")
    if not english:
        errors.append("Empty English segment")

    # Check length ratio
    if irish and english:
        ratio = len(irish) / len(english)
        if ratio < 0.3 or ratio > 3.0:
            warnings.append(f"Unusual length ratio: {ratio:.2f}")

    # Check for encoding issues
    try:
        irish.encode('utf-8')
        english.encode('utf-8')
    except UnicodeEncodeError:
        errors.append("Encoding error in segment")

    # Check for likely misalignment
    if irish == english:
        warnings.append("Identical source and target")

    return ValidationResult(
        valid=len(errors) == 0,
        errors=errors,
        warnings=warnings
    )
```

### 3.2 Language Detection

```python
from langdetect import detect, detect_langs
from typing import Tuple

def verify_languages(segment: Dict) -> Tuple[bool, float, float]:
    """
    Verify language of each segment.

    Returns:
        Tuple of (valid, irish_confidence, english_confidence)
    """
    irish = segment.get("irish", "")
    english = segment.get("english", "")

    irish_conf = 0.0
    english_conf = 0.0

    try:
        irish_langs = detect_langs(irish)
        for lang in irish_langs:
            if lang.lang == "ga":
                irish_conf = lang.prob
                break
    except:
        pass

    try:
        english_langs = detect_langs(english)
        for lang in english_langs:
            if lang.lang == "en":
                english_conf = lang.prob
                break
    except:
        pass

    # Accept if confidence > 0.5 or text too short for detection
    valid = (
        (irish_conf > 0.5 or len(irish) < 20) and
        (english_conf > 0.5 or len(english) < 20)
    )

    return valid, irish_conf, english_conf
```

---

## 4. Export Formats

### 4.1 JSONL Export

```python
import json
from pathlib import Path
from typing import Iterator, Dict

def export_jsonl(
    segments: Iterator[Dict],
    output_path: Path,
    include_metadata: bool = True
):
    """Export segments to JSON Lines format."""
    with output_path.open("w", encoding="utf-8") as f:
        for segment in segments:
            record = {
                "irish": segment["irish"],
                "english": segment["english"]
            }

            if include_metadata:
                record["id"] = segment.get("id", "")
                record["source"] = segment.get("source_file", "")

            f.write(json.dumps(record, ensure_ascii=False) + "\n")
```

### 4.2 Parquet Export

```python
import pyarrow as pa
import pyarrow.parquet as pq
from typing import List, Dict

def export_parquet(
    segments: List[Dict],
    output_path: Path,
    compression: str = "snappy"
):
    """Export segments to Parquet format."""
    schema = pa.schema([
        pa.field("id", pa.string()),
        pa.field("irish", pa.string()),
        pa.field("english", pa.string()),
        pa.field("source", pa.string())
    ])

    # Build arrays
    ids = [s.get("id", "") for s in segments]
    irish = [s.get("irish", "") for s in segments]
    english = [s.get("english", "") for s in segments]
    sources = [s.get("source_file", "") for s in segments]

    table = pa.table({
        "id": ids,
        "irish": irish,
        "english": english,
        "source": sources
    }, schema=schema)

    pq.write_table(table, output_path, compression=compression)
```

### 4.3 HuggingFace Dataset

```python
from datasets import Dataset, DatasetDict
from typing import List, Dict

def export_huggingface(
    segments: List[Dict],
    dataset_name: str,
    push_to_hub: bool = False
):
    """Export to HuggingFace Datasets format."""
    dataset = Dataset.from_dict({
        "irish": [s["irish"] for s in segments],
        "english": [s["english"] for s in segments],
        "id": [s.get("id", "") for s in segments]
    })

    # Create train/validation/test splits
    splits = dataset.train_test_split(test_size=0.1)
    train_valid = splits["train"].train_test_split(test_size=0.1)

    dataset_dict = DatasetDict({
        "train": train_valid["train"],
        "validation": train_valid["test"],
        "test": splits["test"]
    })

    if push_to_hub:
        dataset_dict.push_to_hub(dataset_name)

    return dataset_dict
```

---

## 5. Complete Processing Pipeline

```python
#!/usr/bin/env python3
"""
Complete TMX Processing Pipeline
"""

import asyncio
from pathlib import Path
from typing import List, Dict, Iterator
from dataclasses import dataclass

@dataclass
class ProcessingStats:
    total_segments: int = 0
    valid_segments: int = 0
    invalid_segments: int = 0
    warnings: int = 0

class TMXProcessor:
    def __init__(self, input_dir: Path, output_dir: Path):
        self.input_dir = input_dir
        self.output_dir = output_dir
        self.stats = ProcessingStats()

    def process_file(self, filepath: Path) -> Iterator[Dict]:
        """Process single TMX file."""
        for segment in parse_tmx(filepath):
            self.stats.total_segments += 1

            # Validate
            result = validate_segment(segment)

            if result.valid:
                self.stats.valid_segments += 1
                segment["source_file"] = filepath.name
                yield segment
            else:
                self.stats.invalid_segments += 1

            self.stats.warnings += len(result.warnings)

    def process_all(self) -> List[Dict]:
        """Process all TMX files in directory."""
        all_segments = []

        for tmx_file in self.input_dir.glob("*.tmx"):
            print(f"Processing: {tmx_file.name}")
            segments = list(self.process_file(tmx_file))
            all_segments.extend(segments)

        return all_segments

    def export(self, segments: List[Dict]):
        """Export to all formats."""
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # JSONL
        export_jsonl(
            iter(segments),
            self.output_dir / "parallel.jsonl"
        )

        # Parquet
        export_parquet(
            segments,
            self.output_dir / "parallel.parquet"
        )

        print(f"Exported {len(segments)} segments")

    def run(self):
        """Run complete pipeline."""
        print("Starting TMX processing...")

        segments = self.process_all()

        print(f"Stats: {self.stats}")

        self.export(segments)

        print("Processing complete!")

def main():
    processor = TMXProcessor(
        input_dir=Path("./tmx_files"),
        output_dir=Path("./output")
    )
    processor.run()

if __name__ == "__main__":
    main()
```

---

## 6. Gaois TMX Sources

### 6.1 Download URLs

| Corpus | Content | URL |
|--------|---------|-----|
| **EU Legislation** | Regulations, Directives | https://www.gaois.ie/en/corpora/parallel/data |
| **Constitution** | Bunreacht na hÉireann | Included in above |
| **Acts of Oireachtas** | 1922-2003+ | Included in above |
| **Statutory Instruments** | Irish law | Included in above |

### 6.2 Acquisition Script

```bash
#!/bin/bash
# Download Gaois Parallel Corpus TMX files

OUTPUT_DIR="./tmx_files"
mkdir -p "$OUTPUT_DIR"

# Download from Gaois (check actual URLs)
wget -P "$OUTPUT_DIR" \
  "https://www.gaois.ie/en/corpora/parallel/data/eu_legislation.tmx" \
  "https://www.gaois.ie/en/corpora/parallel/data/constitution.tmx" \
  "https://www.gaois.ie/en/corpora/parallel/data/acts.tmx"

echo "Download complete"
```

---

## References

- TMX 1.4 Specification: https://www.gala-global.org/tmx-14b
- translate-toolkit: https://toolkit.translatehouse.org/
- Gaois Parallel Corpus: https://www.gaois.ie/en/corpora/parallel/
