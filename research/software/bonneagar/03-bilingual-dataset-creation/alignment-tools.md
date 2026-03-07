# Text Alignment Tools for Irish-English

## Overview

Text alignment is the process of matching parallel segments (sentences, phrases, or terms) between source and target languages. This document covers tools and techniques for aligning Irish-English parallel content.

---

## 1. Alignment Tools

### 1.1 gaoisalign (Irish-Specific)

**Repository:** https://github.com/gaois/gaoisalign

| Property | Value |
|----------|-------|
| **Language** | Python |
| **License** | MIT |
| **Focus** | Irish-English alignment |
| **Maintained** | Gaois Research Group |

**Installation:**

```bash
git clone https://github.com/gaois/gaoisalign.git
cd gaoisalign
pip install -e .
```

**Usage:**

```python
from gaoisalign import align

# Align parallel texts
irish_text = "Is é seo an chéad abairt. Seo an dara habairt."
english_text = "This is the first sentence. This is the second sentence."

alignments = align(irish_text, english_text)
for pair in alignments:
    print(f"GA: {pair.source}")
    print(f"EN: {pair.target}")
```

### 1.2 hunalign

**Repository:** https://github.com/danielvarga/hunalign

| Property | Value |
|----------|-------|
| **Language** | C++ |
| **License** | LGPL |
| **Focus** | Language-agnostic sentence alignment |
| **Dictionary** | Optional bilingual dictionary |

**Installation:**

```bash
# Ubuntu/Debian
sudo apt-get install hunalign

# From source
git clone https://github.com/danielvarga/hunalign.git
cd hunalign/src
make
```

**Usage:**

```bash
# Basic alignment
hunalign dictionary.txt source.txt target.txt > aligned.txt

# Without dictionary
hunalign -text /dev/null source.txt target.txt > aligned.txt
```

**Python Wrapper:**

```python
import subprocess
from pathlib import Path
from typing import List, Tuple

def hunalign(
    source_file: Path,
    target_file: Path,
    dictionary: Path = None
) -> List[Tuple[str, str]]:
    """Run hunalign and parse results."""
    cmd = ["hunalign"]

    if dictionary:
        cmd.append(str(dictionary))
    else:
        cmd.extend(["-text", "/dev/null"])

    cmd.extend([str(source_file), str(target_file)])

    result = subprocess.run(cmd, capture_output=True, text=True)

    alignments = []
    for line in result.stdout.strip().split("\n"):
        parts = line.split("\t")
        if len(parts) >= 2:
            alignments.append((parts[0], parts[1]))

    return alignments
```

### 1.3 Bleualign

**Repository:** https://github.com/rsennrich/Bleualign

| Property | Value |
|----------|-------|
| **Language** | Python |
| **License** | LGPL |
| **Method** | Uses MT output for alignment |
| **Quality** | High for noisy parallel text |

**Installation:**

```bash
pip install bleualign
```

**Usage:**

```python
from bleualign.align import Aligner

aligner = Aligner(
    source_file="irish.txt",
    target_file="english.txt",
    source_translation="irish_translated.txt"  # MT output
)

alignments = aligner.align()
```

### 1.4 vecalign

**Repository:** https://github.com/thompsonb/vecalign

| Property | Value |
|----------|-------|
| **Language** | Python |
| **License** | Apache 2.0 |
| **Method** | Neural sentence embeddings |
| **Model** | LASER/LaBSE |

**Installation:**

```bash
pip install vecalign
```

**Usage:**

```python
from vecalign import align

# Uses sentence embeddings for alignment
alignments = align(
    source_sentences=["Irish sentence 1", "Irish sentence 2"],
    target_sentences=["English sentence 1", "English sentence 2"],
    embedding_model="laser"
)
```

---

## 2. Sentence Splitting

### 2.1 Irish Sentence Tokenizer

```python
import re
from typing import List

def split_irish_sentences(text: str) -> List[str]:
    """
    Split Irish text into sentences.
    Handles common Irish abbreviations.
    """
    # Irish abbreviations that don't end sentences
    abbreviations = [
        r'Dr\.', r'Mr\.', r'Mrs\.', r'Ms\.',
        r'Uimh\.', r'lgh\.', r'féach',
        r'e\.g\.', r'i\.e\.',
        r'c\.', r'm\.sh\.'  # circa, mar shampla
    ]

    # Protect abbreviations
    protected = text
    for i, abbr in enumerate(abbreviations):
        protected = re.sub(abbr, f"<ABBR{i}>", protected)

    # Split on sentence boundaries
    sentences = re.split(r'(?<=[.!?])\s+', protected)

    # Restore abbreviations
    restored = []
    for sent in sentences:
        for i, abbr in enumerate(abbreviations):
            sent = sent.replace(f"<ABBR{i}>", abbr.replace('\\', ''))
        restored.append(sent.strip())

    return [s for s in restored if s]
```

### 2.2 Using spaCy

```python
import spacy

# Load Irish model (if available) or multilingual
try:
    nlp = spacy.load("ga_core_news_sm")
except:
    nlp = spacy.load("xx_sent_ud_sm")

def split_sentences_spacy(text: str) -> List[str]:
    """Split sentences using spaCy."""
    doc = nlp(text)
    return [sent.text.strip() for sent in doc.sents]
```

---

## 3. Alignment Quality Metrics

### 3.1 Length Ratio Check

```python
def length_ratio(source: str, target: str) -> float:
    """Calculate character length ratio."""
    if len(target) == 0:
        return float('inf')
    return len(source) / len(target)

def is_valid_alignment(source: str, target: str) -> bool:
    """Check if alignment is plausible based on length."""
    ratio = length_ratio(source, target)
    # Irish-English typically has ratio 0.8-1.3
    return 0.5 <= ratio <= 2.0
```

### 3.2 Alignment Score

```python
from sentence_transformers import SentenceTransformer
import numpy as np

model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')

def alignment_score(source: str, target: str) -> float:
    """Calculate semantic similarity score."""
    embeddings = model.encode([source, target])
    similarity = np.dot(embeddings[0], embeddings[1]) / (
        np.linalg.norm(embeddings[0]) * np.linalg.norm(embeddings[1])
    )
    return float(similarity)

def filter_by_score(
    alignments: List[Tuple[str, str]],
    threshold: float = 0.7
) -> List[Tuple[str, str]]:
    """Filter alignments by semantic similarity."""
    filtered = []
    for source, target in alignments:
        score = alignment_score(source, target)
        if score >= threshold:
            filtered.append((source, target, score))
    return filtered
```

---

## 4. Complete Alignment Pipeline

```python
#!/usr/bin/env python3
"""
Complete Irish-English Alignment Pipeline
"""

from pathlib import Path
from typing import List, Dict, Tuple
from dataclasses import dataclass
import json

@dataclass
class AlignedPair:
    source: str
    target: str
    score: float
    method: str

class IrishEnglishAligner:
    def __init__(self):
        self.min_score = 0.7
        self.min_length = 5
        self.max_ratio = 2.0

    def preprocess(self, text: str) -> str:
        """Clean and normalize text."""
        # Normalize whitespace
        text = ' '.join(text.split())
        # Normalize quotes
        text = text.replace('"', '"').replace('"', '"')
        return text.strip()

    def split_sentences(self, text: str, lang: str) -> List[str]:
        """Split text into sentences."""
        sentences = split_irish_sentences(text)
        return [s for s in sentences if len(s) >= self.min_length]

    def align_documents(
        self,
        irish_text: str,
        english_text: str
    ) -> List[AlignedPair]:
        """Align two parallel documents."""
        # Preprocess
        irish = self.preprocess(irish_text)
        english = self.preprocess(english_text)

        # Split sentences
        irish_sents = self.split_sentences(irish, "ga")
        english_sents = self.split_sentences(english, "en")

        # Align using hunalign
        alignments = self._hunalign(irish_sents, english_sents)

        # Score and filter
        results = []
        for ga, en in alignments:
            if not is_valid_alignment(ga, en):
                continue

            score = alignment_score(ga, en)
            if score >= self.min_score:
                results.append(AlignedPair(
                    source=ga,
                    target=en,
                    score=score,
                    method="hunalign+semantic"
                ))

        return results

    def _hunalign(
        self,
        source_sents: List[str],
        target_sents: List[str]
    ) -> List[Tuple[str, str]]:
        """Run hunalign on sentence lists."""
        import tempfile

        with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as sf:
            sf.write('\n'.join(source_sents))
            source_file = sf.name

        with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as tf:
            tf.write('\n'.join(target_sents))
            target_file = tf.name

        return hunalign(Path(source_file), Path(target_file))

    def export(
        self,
        alignments: List[AlignedPair],
        output_path: Path
    ):
        """Export alignments to JSONL."""
        with output_path.open('w', encoding='utf-8') as f:
            for pair in alignments:
                record = {
                    "irish": pair.source,
                    "english": pair.target,
                    "score": pair.score,
                    "method": pair.method
                }
                f.write(json.dumps(record, ensure_ascii=False) + '\n')

def main():
    aligner = IrishEnglishAligner()

    # Example usage
    irish = """
    Is é seo an chéad abairt sa téacs.
    Tá an dara habairt anseo.
    Seo an tríú habairt.
    """

    english = """
    This is the first sentence in the text.
    The second sentence is here.
    This is the third sentence.
    """

    alignments = aligner.align_documents(irish, english)

    for pair in alignments:
        print(f"GA: {pair.source}")
        print(f"EN: {pair.target}")
        print(f"Score: {pair.score:.3f}")
        print()

    aligner.export(alignments, Path("alignments.jsonl"))

if __name__ == "__main__":
    main()
```

---

## 5. Dictionary Resources

### 5.1 Irish-English Dictionary Format

For hunalign and similar tools:

```text
# Irish-English dictionary for alignment
# Format: irish_word @ english_word
agus @ and
an @ the
atá @ is
bhí @ was
bheith @ be
```

### 5.2 Building from Téarma

```python
import httpx
from pathlib import Path

async def build_dictionary_from_tearma(output_path: Path):
    """Build alignment dictionary from Téarma terminology."""
    # Note: This is a conceptual example
    # Actual implementation depends on Téarma API availability

    terms = []
    # Fetch terms from Téarma API or scrape

    with output_path.open('w', encoding='utf-8') as f:
        for term in terms:
            irish = term.get("ga", "")
            english = term.get("en", "")
            if irish and english:
                f.write(f"{irish} @ {english}\n")
```

---

## 6. Tool Comparison

| Tool | Speed | Quality | Irish Support | Dependencies |
|------|-------|---------|---------------|--------------|
| **gaoisalign** | Medium | High | Native | Python |
| **hunalign** | Fast | Good | Generic | C++ |
| **Bleualign** | Slow | High | Via MT | Python, MT |
| **vecalign** | Medium | High | Via embeddings | Python, LASER |

### Recommendation

1. **Start with gaoisalign** - Irish-specific, maintained by Gaois
2. **Fall back to hunalign** - Fast, good for large volumes
3. **Use vecalign for noisy data** - Better at handling mismatches
4. **Score all alignments** - Filter by semantic similarity

---

## References

- gaoisalign: https://github.com/gaois/gaoisalign
- hunalign: https://github.com/danielvarga/hunalign
- Bleualign: https://github.com/rsennrich/Bleualign
- vecalign: https://github.com/thompsonb/vecalign
- LASER embeddings: https://github.com/facebookresearch/LASER
