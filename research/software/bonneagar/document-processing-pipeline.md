# Document Processing Pipeline for Cryptocurrency Analytics

## Overview

This document describes the pipeline for processing cryptocurrency documents (whitepapers, audits, research reports) and extracting structured knowledge for the knowledge graph.

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Document Processing Pipeline                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │ Discovery│───>│Extraction│───>│Cognify   │───>│ Export   │      │
│  │ (Crawl4AI)│   │ (Marker) │    │(Cognee)  │    │(Graph+Vec)│     │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘      │
│       │               │               │               │              │
│       ▼               ▼               ▼               ▼              │
│  URLs, PDFs      Markdown +     Entities +      Memgraph +          │
│  HTML pages      Structure      Relations       LanceDB              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Stage 1: Document Discovery

### Crawl4AI Integration

```python
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig
from crawl4ai.extraction_strategy import LLMExtractionStrategy

async def discover_protocol_documents(protocol_name: str) -> list[dict]:
    """Discover documentation URLs for a protocol"""

    browser_config = BrowserConfig(
        headless=True,
        viewport_width=1280,
        viewport_height=720
    )

    crawler_config = CrawlerRunConfig(
        word_count_threshold=100,
        extraction_strategy=LLMExtractionStrategy(
            provider="openai/gpt-4o-mini",
            schema={
                "type": "object",
                "properties": {
                    "documents": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "title": {"type": "string"},
                                "url": {"type": "string"},
                                "doc_type": {"type": "string"},
                                "description": {"type": "string"}
                            }
                        }
                    }
                }
            },
            instruction=f"Find all documentation links for {protocol_name} including whitepapers, audits, risk reports, and technical documentation."
        )
    )

    async with AsyncWebCrawler(config=browser_config) as crawler:
        result = await crawler.arun(
            url=f"https://docs.{protocol_name.lower()}.fi/",
            config=crawler_config
        )
        return result.extracted_content
```

### Document Source Registry

From `crypto_sources.json`, document sources include:

| Source ID | Type | URL Pattern |
|-----------|------|-------------|
| `ethena_docs_reserve_fund_pdf` | PDF | docs.ethena.fi |
| `ethena_audits_pdf_index` | PDF | docs.ethena.fi/security/audits |
| `pendle_v2_whitepaper_pdf` | PDF | Pendle repository |
| `chaoslabs_aave_emode_risk_pdf` | PDF | Chaos Labs website |
| `21shares_pendle_onepager_pdf` | PDF | 21Shares research |

## Stage 2: Content Extraction

### PDF Extraction with Marker

Using Marker for high-quality PDF extraction (10x faster than Nougat, preserves LaTeX):

```python
from marker.converters.pdf import PdfConverter
from marker.models import create_model_dict

def extract_pdf_content(pdf_path: str) -> dict:
    """Extract structured content from PDF with math preservation"""

    models = create_model_dict()
    converter = PdfConverter(
        config={
            "preserve_latex": True,
            "extract_tables": True,
            "extract_images": True
        }
    )

    result = converter(pdf_path, models)

    return {
        "markdown": result.markdown,
        "metadata": result.metadata,
        "images": result.images,
        "tables": result.tables,
        "math_blocks": extract_math_blocks(result.markdown)
    }

def extract_math_blocks(markdown: str) -> list[dict]:
    """Extract LaTeX math expressions for preservation"""
    import re

    patterns = [
        (r'\$\$(.+?)\$\$', 'display'),
        (r'\$(.+?)\$', 'inline'),
        (r'\\begin\{equation\}(.+?)\\end\{equation\}', 'equation')
    ]

    blocks = []
    for pattern, math_type in patterns:
        for match in re.finditer(pattern, markdown, re.DOTALL):
            blocks.append({
                "content": match.group(1),
                "type": math_type,
                "position": match.start()
            })
    return blocks
```

### HTML Extraction for Web Pages

```python
from crawl4ai import AsyncWebCrawler
from bs4 import BeautifulSoup

async def extract_html_content(url: str) -> dict:
    """Extract content from HTML documentation pages"""

    async with AsyncWebCrawler() as crawler:
        result = await crawler.arun(url=url)

        # Parse with BeautifulSoup for additional structure
        soup = BeautifulSoup(result.html, 'html.parser')

        return {
            "markdown": result.markdown,
            "title": soup.title.string if soup.title else None,
            "headings": extract_headings(soup),
            "links": extract_links(soup, url),
            "tables": extract_tables(soup),
            "code_blocks": result.code_blocks
        }

def extract_headings(soup) -> list[dict]:
    """Extract document structure from headings"""
    headings = []
    for tag in soup.find_all(['h1', 'h2', 'h3', 'h4']):
        headings.append({
            "level": int(tag.name[1]),
            "text": tag.get_text(strip=True),
            "id": tag.get('id')
        })
    return headings
```

## Stage 3: LLM-Based Structured Extraction

### BAML Schema for Crypto Documents

```baml
// baml_src/crypto_document.baml

class CryptoDocument {
  title string
  doc_type "whitepaper" | "audit" | "research" | "governance" | "risk_report"
  protocol string
  summary string @description("2-3 sentence summary of the document")

  // Extracted sections
  tokenomics Tokenomics?
  governance Governance?
  risks Risk[]
  mechanisms Mechanism[]
  audit_findings AuditFinding[]

  // Metadata
  published_date string?
  authors string[]
  version string?
}

class Tokenomics {
  token_symbol string
  total_supply string
  distribution Distribution[]
  vesting_schedule string?
  inflation_rate string?
}

class Distribution {
  category string @description("e.g., 'Team', 'Treasury', 'Public Sale'")
  percentage float
  vesting_period string?
}

class Governance {
  model "token_voting" | "multisig" | "council" | "hybrid"
  voting_token string?
  quorum_requirement string?
  timelock_period string?
  key_powers string[] @description("What can governance change?")
}

class Risk {
  category "smart_contract" | "market" | "regulatory" | "custody" | "oracle" | "governance" | "economic"
  severity "critical" | "high" | "medium" | "low"
  title string
  description string
  mitigation string?
  likelihood string?
}

class Mechanism {
  name string
  description string
  components string[]
  dependencies string[] @description("External protocols or oracles required")
}

class AuditFinding {
  auditor string
  severity "critical" | "high" | "medium" | "low" | "informational"
  title string
  description string
  location string? @description("Contract/function affected")
  status "resolved" | "acknowledged" | "disputed" | "open"
  recommendation string?
}

function ExtractCryptoDocument(content: string) -> CryptoDocument {
  client "anthropic/claude-sonnet-4-20250514"

  prompt #"
    Analyze this cryptocurrency/DeFi document and extract structured information.

    Document content:
    {{ content }}

    Extract all relevant information including:
    - Document type and basic metadata
    - Tokenomics details if present
    - Governance structure if described
    - Risk factors mentioned
    - Technical mechanisms explained
    - Audit findings if this is an audit report

    Be precise with numbers and percentages. Include direct quotes for important claims.
  "#
}
```

### Extraction Pipeline

```python
from baml_client import b
from baml_client.types import CryptoDocument

async def extract_structured_content(
    content: str,
    doc_type: str,
    source_url: str
) -> CryptoDocument:
    """Extract structured content using BAML"""

    # Truncate if too long (respect context limits)
    max_chars = 100000
    if len(content) > max_chars:
        content = truncate_intelligently(content, max_chars)

    # Run BAML extraction
    result = await b.ExtractCryptoDocument(content)

    # Add provenance
    result.source_url = source_url
    result.extraction_timestamp = datetime.now().isoformat()

    return result

def truncate_intelligently(content: str, max_chars: int) -> str:
    """Truncate while preserving document structure"""

    # Prioritize sections: summary, tokenomics, risks, findings
    priority_patterns = [
        r'(?i)(executive\s+summary|abstract|overview)',
        r'(?i)(tokenomics|token\s+distribution)',
        r'(?i)(risk|security)',
        r'(?i)(finding|issue|vulnerability)',
        r'(?i)(governance|voting)'
    ]

    # Keep matching sections, trim middle content
    # ... implementation details
    return truncated_content
```

## Stage 4: Knowledge Graph Construction (Cognee)

### Cognee ECL Pipeline

```python
import cognee
from cognee.api.v1.cognify import cognify
from cognee.api.v1.add import add

async def build_document_knowledge_graph(
    documents: list[CryptoDocument]
) -> None:
    """Process documents through Cognee ECL pipeline"""

    # Configure backends
    cognee.config.set_graph_database(
        type="memgraph",
        host="localhost",
        port=7687
    )
    cognee.config.set_vector_database(
        type="lancedb",
        path="./data/vectors"
    )

    for doc in documents:
        # E: Extract - Add document content
        await add(
            data=doc.model_dump_json(),
            dataset_name=f"{doc.protocol}_docs"
        )

    # C: Cognify - Build knowledge graph
    await cognify()

    # Graph is now populated with:
    # - Entity nodes (Token, Protocol, Risk, Mechanism)
    # - Relationship edges (DESCRIBES, CONTAINS, MITIGATES)
    # - Vector embeddings in LanceDB for semantic search
```

### Custom Entity Extraction

```python
from cognee.modules.data.extraction import extract_entities_with_llm

async def extract_crypto_entities(content: str) -> list[dict]:
    """Extract crypto-specific entities"""

    entity_types = [
        "Token",
        "Protocol",
        "Exchange",
        "SmartContract",
        "Wallet",
        "Risk",
        "Mechanism",
        "Auditor"
    ]

    entities = await extract_entities_with_llm(
        content=content,
        entity_types=entity_types,
        llm_model="gpt-4o-mini"
    )

    # Post-process: normalize addresses, validate symbols
    for entity in entities:
        if entity["type"] == "Token":
            entity["symbol"] = entity["name"].upper()
        if entity["type"] == "SmartContract":
            entity["address"] = normalize_address(entity.get("address"))

    return entities
```

## Stage 5: Export to Graph & Vector Stores

### Memgraph Export

```python
from neo4j import GraphDatabase

def export_to_memgraph(
    documents: list[CryptoDocument],
    entities: list[dict],
    relationships: list[dict]
):
    """Export extracted knowledge to Memgraph"""

    driver = GraphDatabase.driver("bolt://localhost:7687")

    with driver.session() as session:
        # Create document nodes
        for doc in documents:
            session.run("""
                MERGE (d:Document {url: $url})
                SET d.title = $title,
                    d.doc_type = $doc_type,
                    d.protocol = $protocol,
                    d.summary = $summary,
                    d.published_date = $published_date
            """, **doc.model_dump())

        # Create entity nodes
        for entity in entities:
            session.run(f"""
                MERGE (e:{entity['type']} {{id: $id}})
                SET e += $properties
            """, id=entity['id'], properties=entity)

        # Create relationships
        for rel in relationships:
            session.run(f"""
                MATCH (a {{id: $from_id}})
                MATCH (b {{id: $to_id}})
                MERGE (a)-[r:{rel['type']}]->(b)
                SET r += $properties
            """, **rel)
```

### LanceDB Vector Export

```python
import lancedb
from sentence_transformers import SentenceTransformer

def export_to_lancedb(documents: list[CryptoDocument]):
    """Export document embeddings to LanceDB"""

    model = SentenceTransformer('all-MiniLM-L6-v2')
    db = lancedb.connect("./data/vectors")

    # Prepare data with embeddings
    data = []
    for doc in documents:
        # Chunk document for better retrieval
        chunks = chunk_document(doc.summary + "\n" + doc.content)

        for i, chunk in enumerate(chunks):
            embedding = model.encode(chunk)
            data.append({
                "id": f"{doc.url}_{i}",
                "text": chunk,
                "vector": embedding,
                "doc_url": doc.url,
                "doc_type": doc.doc_type,
                "protocol": doc.protocol
            })

    # Upsert to LanceDB
    table = db.create_table("crypto_docs", data, mode="overwrite")

    # Create IVF-PQ index for fast search
    table.create_index(
        metric="L2",
        num_partitions=256,
        num_sub_vectors=96
    )
```

## Orchestration with Dagster

### Document Pipeline Assets

```python
from dagster import asset, AssetExecutionContext, MaterializeResult

@asset(
    description="Discover and index protocol documentation",
    compute_kind="crawl4ai",
    group_name="documents"
)
async def discover_documents(context: AssetExecutionContext) -> list[dict]:
    """Discover documentation URLs for all tracked protocols"""

    protocols = ["ethena", "aave", "pendle", "compound", "uniswap"]
    all_docs = []

    for protocol in protocols:
        docs = await discover_protocol_documents(protocol)
        all_docs.extend(docs)
        context.log.info(f"Found {len(docs)} documents for {protocol}")

    return all_docs

@asset(
    deps=["discover_documents"],
    description="Extract content from discovered documents",
    compute_kind="marker"
)
async def extract_documents(
    context: AssetExecutionContext,
    discover_documents: list[dict]
) -> list[dict]:
    """Extract content from PDFs and HTML pages"""

    extracted = []
    for doc in discover_documents:
        if doc["url"].endswith(".pdf"):
            content = extract_pdf_content(doc["url"])
        else:
            content = await extract_html_content(doc["url"])

        extracted.append({
            **doc,
            "content": content
        })

    return extracted

@asset(
    deps=["extract_documents"],
    description="Apply LLM extraction to get structured data",
    compute_kind="baml"
)
async def structure_documents(
    context: AssetExecutionContext,
    extract_documents: list[dict]
) -> list[CryptoDocument]:
    """Extract structured information using BAML"""

    structured = []
    for doc in extract_documents:
        result = await extract_structured_content(
            content=doc["content"]["markdown"],
            doc_type=doc["doc_type"],
            source_url=doc["url"]
        )
        structured.append(result)

    return structured

@asset(
    deps=["structure_documents"],
    description="Build knowledge graph from structured documents",
    compute_kind="cognee"
)
async def build_knowledge_graph(
    context: AssetExecutionContext,
    structure_documents: list[CryptoDocument]
) -> MaterializeResult:
    """Process through Cognee and export to Memgraph + LanceDB"""

    await build_document_knowledge_graph(structure_documents)
    export_to_lancedb(structure_documents)

    return MaterializeResult(
        metadata={
            "documents_processed": len(structure_documents),
            "graph_nodes": count_graph_nodes(),
            "vector_count": count_vectors()
        }
    )
```

## Quality Assurance

### Document Quality Checks

```python
from dagster import asset_check, AssetCheckResult

@asset_check(asset=structure_documents)
def check_extraction_quality(structure_documents: list[CryptoDocument]) -> AssetCheckResult:
    """Verify extraction quality meets thresholds"""

    issues = []
    for doc in structure_documents:
        quality = CryptoDocumentQuality().score(doc.model_dump())
        if quality < 0.6:
            issues.append(f"{doc.title}: quality score {quality:.2f}")

    return AssetCheckResult(
        passed=len(issues) == 0,
        severity="WARN",
        metadata={
            "low_quality_documents": issues,
            "total_documents": len(structure_documents)
        }
    )
```

## References

- Marker PDF extraction: https://github.com/VikParuchuri/marker
- Crawl4AI documentation: https://crawl4ai.com
- Cognee ECL pipeline: https://github.com/topoteretes/cognee
- BAML documentation: https://docs.boundaryml.com
