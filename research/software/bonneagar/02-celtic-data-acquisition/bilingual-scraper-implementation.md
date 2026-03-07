# Bilingual Irish Educational Resources Scraper
## Technical Outline & Implementation Guide

**Document Version:** 2.0
**Date:** 2025-11-17
**Project:** Irish-English Bilingual Dataset Collection from Educational Websites
**Updated:** 2025-11-17 - Using Crawl4AI and Cocoindex

---

## Executive Summary

This document outlines a comprehensive approach to scraping bilingual educational content from five major Irish educational websites to create a high-quality Irish-English parallel corpus. The project leverages existing infrastructure including Crawl4AI (LLM-native web crawler), Cocoindex (knowledge graph and code indexing), and DLT (Data Load Tool) pipelines.

**Target Websites:**
1. NCCA.ie - National Council for Curriculum and Assessment
2. Oide.ie - Professional Development Service for Teachers
3. Scoilnet.ie - Digital Repository for Irish Education
4. Examinations.ie - State Examinations Commission
5. Curriculumonline.ie - Curriculum Resource Repository

---

## Table of Contents

1. [Website Analysis](#1-website-analysis)
2. [Technical Architecture](#2-technical-architecture)
3. [Data Schema Design](#3-data-schema-design)
4. [Scraping Strategy](#4-scraping-strategy)
5. [Bilingual Content Mapping](#5-bilingual-content-mapping)
6. [Implementation Plan](#6-implementation-plan)
7. [Quality Assurance](#7-quality-assurance)
8. [Storage & Export](#8-storage--export)
9. [Challenges & Solutions](#9-challenges--solutions)

---

## 1. Website Analysis

### 1.1 NCCA.ie - National Council for Curriculum and Assessment

**Purpose:** National curriculum development and assessment standards

**Content Types:**
- Curriculum specifications (Primary, Junior Cycle, Senior Cycle)
- Assessment guidelines
- Subject specifications and learning outcomes
- Policy documents and reports
- Teacher resources and exemplars

**Language Implementation:**
- URL Pattern:
  - English: `https://www.ncca.ie/en/`
  - Irish: `https://www.ncca.ie/ga/`
- Language switcher in navigation
- Parallel content structure
- Both languages available for most curriculum documents

**Key Content Sections:**
- `/en/primary/` - Primary curriculum
- `/en/junior-cycle/` - Junior cycle specifications
- `/en/senior-cycle/` - Senior cycle content
- `/en/early-childhood/` - Early years framework
- `/media/` - Downloadable PDF resources

**Data Value:**
- High-quality educational terminology
- Formal curriculum language
- Subject-specific vocabulary across all disciplines
- Assessment criteria and learning outcomes

**Scraping Complexity:** Medium
- Well-structured HTML
- Clear language separation
- May have PDF content requiring extraction
- Some dynamic content loading

---

### 1.2 Oide.ie - Professional Development Service for Teachers

**Purpose:** Teacher professional development and continuous learning

**Content Types:**
- Professional development courses
- Teaching methodologies
- Webinars and training materials
- Resource libraries
- Best practice guides

**Language Implementation:**
- URL Pattern: Likely uses `/ga/` prefix or language parameter
- Language toggle in header/footer
- Content primarily in English with Irish translations for key resources

**Key Content Sections:**
- Professional learning courses
- Subject-specific resources
- Digital learning tools
- Webinar archives
- Publications and research

**Data Value:**
- Pedagogical terminology
- Professional development vocabulary
- Teaching methodology descriptions
- Less formal than NCCA but still educational

**Scraping Complexity:** Medium-High
- May require authentication for some resources
- Video/multimedia content
- Dynamic course listings
- Possible AJAX-based content loading

---

### 1.3 Scoilnet.ie - Digital Repository for Irish Education

**Purpose:** Comprehensive educational resource repository for teachers and students

**Content Types:**
- Lesson plans and teaching resources
- Interactive learning tools
- Video content (RTÉ Learn, TG4 Foghlaim)
- Historical archives (Irish Newspaper Archives)
- Multimedia educational content
- Learning paths (curated resource collections)

**Language Implementation:**
- URL Pattern:
  - English: `https://www.scoilnet.ie/`
  - Irish: `https://www.scoilnet.ie/ga/`
- Clear language separation via `/ga/` prefix
- Locale variable: `var locale = 'en'` or `'ga'`
- Language toggle link in primary navigation

**Key Content Sections:**
- `/go-to-primary/` - Primary level resources
- `/go-to-post-primary/` - Post-primary resources
- `/learning-path/ref/[ID]/` - Curated learning collections
- `/scoilnet/news/` - News and updates
- `/scoilnet-sites/[level]/[tool-name]/` - Interactive tools

**Technical Infrastructure:**
- Solr search: `//search.scoilnet.ie/`
- API endpoint: `scoilnet-services/scoilnet-api/`
- Google Tag Manager integration
- Structured metadata likely available

**Data Value:**
- Broad vocabulary across all subjects
- Age-appropriate language (primary to post-primary)
- Informal educational content
- Rich multimedia metadata
- Lesson plan structures

**Scraping Complexity:** Medium
- Well-documented API potential
- Clear URL patterns
- Solr search integration for discovery
- Some resources may be third-party embeds
- JavaScript-rendered content

---

### 1.4 Examinations.ie - State Examinations Commission

**Purpose:** Official examination papers, marking schemes, and assessment information

**Content Types:**
- Past examination papers (Junior Certificate, Leaving Certificate)
- Marking schemes
- Chief examiner reports
- Examination timetables
- Candidate information
- Subject specifications

**Language Implementation:**
- URL Pattern: Likely `/en/` and `/ga/` separation
- Language switcher
- Full bilingual support (legal requirement for state exams)
- Irish language examination papers in both languages

**Key Content Sections:**
- Past papers archive (by year and subject)
- Marking schemes
- Statistics and reports
- Candidate resources
- Examination specifications

**Data Value:**
- Examination-level academic language
- Subject-specific terminology at highest level
- Formal assessment language
- Standardized vocabulary
- Parallel translations of identical content

**Scraping Complexity:** Low-Medium
- Primarily static PDF documents
- Well-structured archive
- Predictable URL patterns
- PDF extraction required
- High-value parallel content

---

### 1.5 Curriculumonline.ie - Curriculum Resource Repository

**Purpose:** Online curriculum repository and teaching resources

**Content Types:**
- Curriculum documents
- Teaching units and lesson sequences
- Assessment resources
- Subject-specific materials
- Cross-curricular resources

**Language Implementation:**
- URL Pattern: TBD (likely `/en/` and `/ga/`)
- Language toggle
- Structured curriculum content

**Key Content Sections:**
- Subject areas
- Level-specific resources (Primary/Post-Primary)
- Assessment tools
- Teacher guides

**Data Value:**
- Curriculum-aligned content
- Subject terminology
- Teaching instructions
- Assessment language

**Scraping Complexity:** Medium
- Structured content
- May have authentication requirements
- PDF resources
- Metadata-rich content

---

## 2. Technical Architecture

### 2.1 Technology Stack

**Core Scraping Technologies:**
- **Crawl4AI** - LLM-native async web crawler with JavaScript rendering
  - Dual extraction: CSS selectors + LLM-powered semantic extraction
  - Browser profiles for authentication
  - Deep crawling (BFS/DFS strategies)
  - Built-in markdown generation optimized for LLMs
  - Playwright-based for full JavaScript support
- **Cocoindex** - Knowledge graph and structured data extraction
  - LLM-based content transformation
  - Graph database integration (Neo4j, PostgreSQL)
  - Flow-based data processing
  - Tree-sitter for code parsing
  - Incremental processing support
- **PyPDF2/pdfplumber** - PDF text extraction for examination papers

**Data Pipeline:**
- **DLT (Data Load Tool)** - ETL pipeline orchestration
- **Python 3.10+** - Primary language
- **Pydantic** - Data validation and schema enforcement
- **Cocoindex FlowBuilder** - Declarative data transformation flows

**Storage & Database:**
- **LanceDB** - Vector database for semantic search (already in project)
- **DuckDB** - Analytics and data processing (already in project)
- **PostgreSQL** - Relational storage for metadata (via Cocoindex)
- **Neo4j** - Graph database for knowledge relationships (via Cocoindex)
- **Parquet** - Efficient columnar storage format

**LLM Integration:**
- **OpenAI GPT-4** - Content extraction and validation
- **Ollama (Llama 3.2)** - Local LLM for cost-effective extraction
- **UCCIX-Llama** - Irish language validation (from HuggingFace)
- **Helsinki-NLP translation models** - Quality checking
- **LaBSE** - Multilingual sentence embeddings for alignment

**Existing Project Components:**
- Agno framework for AI agents
- Crawl4AI integration (in `/infrastructure/compose/agno/cookbook/tools/`)
- Cocoindex examples (in `/data/examples/cocoindex/`)
- DLT pipelines (existing patterns in `/data-unified/pipelines/`)
- GitHub to R2 pipeline with Cocoindex indexing

### 2.2 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Orchestration Layer                      │
│            (DLT + Cocoindex FlowBuilder)                     │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Website     │     │  Language    │     │   Content    │
│  Crawler     │────▶│  Detector    │────▶│  Extractor   │
│  (Crawl4AI)  │     │  & Mapper    │     │ (Cocoindex)  │
│  + Deep BFS  │     │              │     │  + LLM       │
└──────────────┘     └──────────────┘     └──────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │  Bilingual   │
                     │  Alignment   │
                     │  Engine      │
                     │  (LaBSE)     │
                     └──────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Quality    │     │  Knowledge   │     │   Metadata   │
│  Validation  │     │   Graph      │     │  Extraction  │
│  (LLM-based) │     │  (Cocoindex) │     │  & Tagging   │
└──────────────┘     └──────────────┘     └──────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │   Storage    │
                     │   Layer      │
                     │ (Multi-DB)   │
                     └──────────────┘
                              │
        ┌─────────────────────┼─────────────────────┬──────────────┐
        ▼                     ▼                     ▼              ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐ ┌──────────┐
│   LanceDB    │     │   DuckDB     │     │   Parquet    │ │  Neo4j   │
│  (Vectors)   │     │  (Analytics) │     │   (Export)   │ │ (Graph)  │
└──────────────┘     └──────────────┘     └──────────────┘ └──────────┘
```

### 2.3 Component Responsibilities

**1. Website Crawler (Crawl4AI)**
- Discovers all pages using deep crawling (BFS/DFS strategies)
- Respects robots.txt and rate limits
- Full JavaScript rendering via Playwright
- Browser profile management for authentication
- CSS-based and LLM-based content extraction
- Generates LLM-ready markdown automatically
- Screenshot capture for visual analysis
- Detects language variations via HTML metadata

**2. Language Detector & Mapper**
- Identifies language of each page (Irish/English)
- Maps corresponding bilingual page pairs
- Extracts language metadata from HTML
- Validates language using LLM detection
- Uses LaBSE embeddings for cross-lingual similarity

**3. Content Extractor (Cocoindex)**
- Extracts structured content using LLM-based transformations
- Flow-based data processing with FlowBuilder
- Pydantic schema validation for structured extraction
- Handles PDF document processing (via separate tools)
- Preserves formatting and structure
- Extracts embedded resources metadata
- Maintains content hierarchy through graph relationships

**4. Bilingual Alignment Engine**
- Matches English and Irish content pairs
- Uses LaBSE multilingual embeddings for semantic similarity
- Paragraph-level alignment via vector cosine similarity
- Sentence-level alignment (where feasible)
- Confidence scoring based on embedding distances
- Handles partial translations gracefully

**5. Quality Validation**
- Language detection confirmation via langdetect
- Content quality scoring using LLM evaluation
- Translation quality estimation
- Duplicate detection via content hashing
- Completeness validation

**6. Knowledge Graph Generation (Cocoindex)**
- Builds relationship graphs between content entities
- Extracts terminology pairs and relationships
- Stores in Neo4j for graph querying
- Links bilingual content in knowledge graph
- Enables semantic navigation of curriculum

**7. Embeddings Generation**
- Generates vector embeddings using LaBSE
- Stores in LanceDB for semantic search
- Enables cross-lingual similarity search
- Supports bilingual RAG applications

**8. Storage Layer (Multi-Database)**
- DuckDB for analytics and SQL queries
- LanceDB for vector search
- Neo4j for knowledge graph (via Cocoindex)
- PostgreSQL for relational metadata (via Cocoindex)
- Parquet for efficient exports
- Raw data preservation in JSON/JSONL

---

## 3. Data Schema Design

### 3.1 Core Data Models

#### BilingualDocument

```python
from pydantic import BaseModel, HttpUrl, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum

class LanguageCode(str, Enum):
    IRISH = "ga"
    ENGLISH = "en"

class ContentType(str, Enum):
    WEBPAGE = "webpage"
    PDF = "pdf"
    VIDEO = "video"
    INTERACTIVE = "interactive"
    LESSON_PLAN = "lesson_plan"
    CURRICULUM_DOC = "curriculum_doc"
    EXAM_PAPER = "exam_paper"
    MARKING_SCHEME = "marking_scheme"

class EducationLevel(str, Enum):
    EARLY_CHILDHOOD = "early_childhood"
    PRIMARY = "primary"
    JUNIOR_CYCLE = "junior_cycle"
    SENIOR_CYCLE = "senior_cycle"
    PROFESSIONAL_DEV = "professional_dev"

class BilingualDocument(BaseModel):
    """Core model for bilingual document pairs"""

    # Identification
    id: str = Field(description="Unique document ID")
    source_website: str = Field(description="Source website domain")

    # English Version
    english_url: HttpUrl
    english_title: str
    english_content: str
    english_html: Optional[str] = None
    english_metadata: Dict[str, Any] = {}

    # Irish Version
    irish_url: HttpUrl
    irish_title: str
    irish_content: str
    irish_html: Optional[str] = None
    irish_metadata: Dict[str, Any] = {}

    # Content Classification
    content_type: ContentType
    education_level: Optional[EducationLevel] = None
    subject_area: Optional[str] = None
    keywords: List[str] = []

    # Alignment Quality
    alignment_confidence: float = Field(ge=0.0, le=1.0)
    alignment_method: str
    is_exact_translation: bool = False

    # Temporal Data
    scraped_at: datetime
    last_updated: Optional[datetime] = None
    published_date: Optional[datetime] = None

    # Quality Metrics
    irish_word_count: int
    english_word_count: int
    quality_score: Optional[float] = Field(None, ge=0.0, le=1.0)

    # Relationships
    related_documents: List[str] = []
    parent_document_id: Optional[str] = None

    class Config:
        json_schema_extra = {
            "example": {
                "id": "ncca-primary-maths-001",
                "source_website": "ncca.ie",
                "english_url": "https://www.ncca.ie/en/primary/mathematics",
                "english_title": "Primary Mathematics Curriculum",
                "english_content": "The primary mathematics curriculum...",
                "irish_url": "https://www.ncca.ie/ga/bunscoil/matamaitic",
                "irish_title": "Curaclam Matamaitice na Bunscoile",
                "irish_content": "Cuireann an curaclam matamaitice...",
                "content_type": "curriculum_doc",
                "education_level": "primary",
                "subject_area": "mathematics",
                "alignment_confidence": 0.95
            }
        }
```

#### ContentSegment

```python
class AlignmentType(str, Enum):
    EXACT = "exact"
    FUZZY = "fuzzy"
    PARTIAL = "partial"
    INFERRED = "inferred"

class ContentSegment(BaseModel):
    """Paragraph or sentence-level aligned content"""

    segment_id: str
    document_id: str

    # Segment Content
    english_text: str
    irish_text: str

    # Position in Document
    segment_index: int
    segment_type: str  # paragraph, heading, list_item, etc.

    # Alignment
    alignment_type: AlignmentType
    alignment_score: float = Field(ge=0.0, le=1.0)

    # Context
    preceding_context: Optional[str] = None
    following_context: Optional[str] = None

    # Linguistic Features
    english_length: int
    irish_length: int
    contains_technical_terms: bool = False
    terminology_pairs: Dict[str, str] = {}
```

#### TerminologyPair

```python
class TerminologyPair(BaseModel):
    """Subject-specific terminology translations"""

    term_id: str
    english_term: str
    irish_term: str

    # Context
    subject_area: str
    education_level: Optional[EducationLevel] = None
    definition_en: Optional[str] = None
    definition_ga: Optional[str] = None

    # Source
    source_documents: List[str]
    occurrence_count: int = 1

    # Validation
    validated: bool = False
    validation_source: Optional[str] = None
```

#### ScrapingMetadata

```python
class ScrapingStatus(str, Enum):
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    FAILED = "failed"
    PARTIAL = "partial"

class ScrapingMetadata(BaseModel):
    """Metadata about scraping operations"""

    scraping_id: str
    website: str
    start_time: datetime
    end_time: Optional[datetime] = None
    status: ScrapingStatus

    # Statistics
    total_pages_discovered: int = 0
    pages_scraped: int = 0
    pages_failed: int = 0
    bilingual_pairs_found: int = 0

    # Configuration
    scraper_version: str
    rate_limit: float
    respect_robots_txt: bool = True

    # Errors
    errors: List[Dict[str, Any]] = []
```

### 3.2 Database Schema Design

#### DuckDB Schema (Analytics)

```sql
-- Main bilingual documents table
CREATE TABLE bilingual_documents (
    id VARCHAR PRIMARY KEY,
    source_website VARCHAR NOT NULL,

    -- English content
    english_url VARCHAR NOT NULL,
    english_title VARCHAR,
    english_content TEXT,
    english_word_count INTEGER,

    -- Irish content
    irish_url VARCHAR NOT NULL,
    irish_title VARCHAR,
    irish_content TEXT,
    irish_word_count INTEGER,

    -- Classification
    content_type VARCHAR,
    education_level VARCHAR,
    subject_area VARCHAR,

    -- Quality & Alignment
    alignment_confidence FLOAT,
    alignment_method VARCHAR,
    quality_score FLOAT,

    -- Temporal
    scraped_at TIMESTAMP,
    published_date TIMESTAMP,

    -- Indexes
    INDEX idx_source_website (source_website),
    INDEX idx_content_type (content_type),
    INDEX idx_education_level (education_level),
    INDEX idx_subject_area (subject_area)
);

-- Content segments for fine-grained alignment
CREATE TABLE content_segments (
    segment_id VARCHAR PRIMARY KEY,
    document_id VARCHAR REFERENCES bilingual_documents(id),

    english_text TEXT,
    irish_text TEXT,

    segment_index INTEGER,
    segment_type VARCHAR,

    alignment_type VARCHAR,
    alignment_score FLOAT,

    english_length INTEGER,
    irish_length INTEGER,

    INDEX idx_document_id (document_id),
    INDEX idx_alignment_type (alignment_type)
);

-- Terminology pairs
CREATE TABLE terminology_pairs (
    term_id VARCHAR PRIMARY KEY,
    english_term VARCHAR,
    irish_term VARCHAR,
    subject_area VARCHAR,
    education_level VARCHAR,
    occurrence_count INTEGER,
    validated BOOLEAN,

    INDEX idx_subject_area (subject_area),
    INDEX idx_validated (validated)
);

-- Scraping metadata
CREATE TABLE scraping_runs (
    scraping_id VARCHAR PRIMARY KEY,
    website VARCHAR,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR,
    total_pages_discovered INTEGER,
    pages_scraped INTEGER,
    bilingual_pairs_found INTEGER,

    INDEX idx_website (website),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
);
```

#### LanceDB Schema (Vector Storage)

```python
# LanceDB tables for semantic search

# Document embeddings
document_embeddings_schema = {
    "id": "string",
    "document_id": "string",
    "language": "string",  # "en" or "ga"
    "content_type": "string",
    "subject_area": "string",
    "embedding": "vector(768)",  # or 1536 for OpenAI embeddings
    "text": "string",
    "metadata": "json"
}

# Segment embeddings for fine-grained search
segment_embeddings_schema = {
    "id": "string",
    "segment_id": "string",
    "document_id": "string",
    "language": "string",
    "embedding": "vector(768)",
    "text": "string",
    "metadata": "json"
}
```

### 3.3 Export Formats

#### 1. HuggingFace Dataset Format

```python
# Structure for upload to HuggingFace datasets
{
    "translation": {
        "en": "English text content...",
        "ga": "Irish text content..."
    },
    "metadata": {
        "source": "ncca.ie",
        "content_type": "curriculum_doc",
        "subject_area": "mathematics",
        "education_level": "primary",
        "url_en": "https://...",
        "url_ga": "https://...",
        "quality_score": 0.95
    }
}
```

#### 2. Parquet Export Schema

```python
# Columnar format for efficient processing
import pyarrow as pa

parquet_schema = pa.schema([
    ('id', pa.string()),
    ('source_website', pa.string()),
    ('english_text', pa.string()),
    ('irish_text', pa.string()),
    ('english_url', pa.string()),
    ('irish_url', pa.string()),
    ('content_type', pa.string()),
    ('education_level', pa.string()),
    ('subject_area', pa.string()),
    ('alignment_confidence', pa.float32()),
    ('quality_score', pa.float32()),
    ('english_word_count', pa.int32()),
    ('irish_word_count', pa.int32()),
    ('scraped_at', pa.timestamp('ms')),
    ('metadata', pa.string()),  # JSON string
])
```

#### 3. JSONL Format (Line-delimited JSON)

```jsonl
{"id": "ncca-001", "source": "ncca.ie", "en": "The curriculum...", "ga": "An curaclam..."}
{"id": "ncca-002", "source": "ncca.ie", "en": "Mathematics is...", "ga": "Is é an mhatamaitic..."}
```

#### 4. TMX Format (Translation Memory Exchange)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<tmx version="1.4">
  <header creationtool="IrishEducationScraper" datatype="html"/>
  <body>
    <tu tuid="ncca-001">
      <tuv xml:lang="en">
        <seg>The curriculum promotes mathematical thinking</seg>
      </tuv>
      <tuv xml:lang="ga">
        <seg>Cuireann an curaclam smaointeoireacht mhatamaiticiúil chun cinn</seg>
      </tuv>
    </tu>
  </body>
</tmx>
```

---

## 4. Scraping Strategy

### 4.1 Discovery Phase

**Objective:** Identify all scrapable pages and their language variants

**Process:**

1. **Sitemap Analysis**
   - Check for `/sitemap.xml` on each domain
   - Parse sitemap for URL lists
   - Identify language-specific sitemaps if available

2. **Robots.txt Compliance**
   - Parse `/robots.txt` for each website
   - Respect crawl-delay directives
   - Identify disallowed paths

3. **Homepage Crawling**
   - Start from homepage
   - Identify navigation structure
   - Map main content sections
   - Locate language switcher mechanism

4. **Language Pattern Detection**
   ```python
   language_patterns = {
       "ncca.ie": {"en": "/en/", "ga": "/ga/"},
       "scoilnet.ie": {"en": "/", "ga": "/ga/"},
       "examinations.ie": {"en": "/en/", "ga": "/ga/"},
       "oide.ie": {"en": "/", "ga": "/ga/"},
       "curriculumonline.ie": {"en": "/en/", "ga": "/ga/"}
   }
   ```

5. **URL Queue Building**
   - BFS (Breadth-First Search) crawling
   - Priority queue based on content type
   - Deduplication
   - Depth limiting (max 5-6 levels)

### 4.2 Content Extraction Strategy

**Per-Website Configuration:**

```python
from dataclasses import dataclass
from typing import Dict, List, Callable

@dataclass
class WebsiteConfig:
    domain: str
    language_patterns: Dict[str, str]
    rate_limit: float  # requests per second
    requires_js: bool
    pdf_extraction: bool
    selectors: Dict[str, str]
    custom_extractors: Dict[str, Callable]

website_configs = {
    "ncca.ie": WebsiteConfig(
        domain="ncca.ie",
        language_patterns={"en": "/en/", "ga": "/ga/"},
        rate_limit=1.0,  # 1 request per second
        requires_js=True,
        pdf_extraction=True,
        selectors={
            "title": "h1.page-title",
            "content": "div.main-content",
            "breadcrumbs": "nav.breadcrumb",
            "metadata": "div.document-metadata"
        },
        custom_extractors={
            "curriculum_specs": extract_ncca_curriculum,
            "learning_outcomes": extract_learning_outcomes
        }
    ),

    "scoilnet.ie": WebsiteConfig(
        domain="scoilnet.ie",
        language_patterns={"en": "/", "ga": "/ga/"},
        rate_limit=2.0,
        requires_js=True,
        pdf_extraction=False,
        selectors={
            "title": "h1.resource-title",
            "content": "div.resource-content",
            "resource_type": "span.resource-type",
            "level": "span.education-level"
        },
        custom_extractors={
            "learning_path": extract_learning_path,
            "interactive_tool": extract_interactive_content
        }
    ),

    "examinations.ie": WebsiteConfig(
        domain="examinations.ie",
        language_patterns={"en": "/en/", "ga": "/ga/"},
        rate_limit=1.0,
        requires_js=False,
        pdf_extraction=True,  # Critical for exam papers
        selectors={
            "title": "h1",
            "year": "span.exam-year",
            "subject": "span.subject",
            "level": "span.exam-level"
        },
        custom_extractors={
            "exam_paper": extract_exam_paper_metadata,
            "marking_scheme": extract_marking_scheme
        }
    ),

    "oide.ie": WebsiteConfig(
        domain="oide.ie",
        language_patterns={"en": "/", "ga": "/ga/"},
        rate_limit=1.5,
        requires_js=True,
        pdf_extraction=True,
        selectors={
            "title": "h1.course-title",
            "content": "div.course-content",
            "duration": "span.duration",
            "category": "span.category"
        },
        custom_extractors={
            "course_info": extract_course_metadata,
            "webinar": extract_webinar_info
        }
    ),

    "curriculumonline.ie": WebsiteConfig(
        domain="curriculumonline.ie",
        language_patterns={"en": "/en/", "ga": "/ga/"},
        rate_limit=1.0,
        requires_js=True,
        pdf_extraction=True,
        selectors={
            "title": "h1",
            "content": "div.curriculum-content",
            "subject": "span.subject-area"
        },
        custom_extractors={}
    )
}
```

### 4.3 Rate Limiting & Politeness

**Strategy:**

1. **Respectful Crawling**
   - Implement exponential backoff on errors
   - Respect rate limits (1-2 requests/second)
   - Random jitter between requests (0.5-1.5s)
   - User-Agent identification

2. **Caching**
   - Cache responses for 24 hours
   - ETags and Last-Modified headers
   - Conditional GET requests

3. **Error Handling**
   - Retry failed requests (max 3 attempts)
   - Circuit breaker pattern for persistent failures
   - Log all failures for manual review

4. **Session Management**
   - Persistent session cookies
   - Session refresh on timeout
   - Handle CSRF tokens if required

### 4.4 JavaScript Rendering with Crawl4AI

**For Sites Requiring JS (ncca.ie, scoilnet.ie, oide.ie):**

```python
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig
from crawl4ai.extraction import JsonCssExtractionStrategy

async def scrape_with_crawl4ai(
    url: str,
    css_selectors: dict = None,
    wait_for: str = None,
    js_code: str = None
):
    """
    Scrape JavaScript-heavy sites using Crawl4AI

    Args:
        url: Target URL
        css_selectors: Dict of CSS selectors for structured extraction
        wait_for: CSS selector to wait for before extraction
        js_code: Custom JavaScript to execute on page
    """

    # Configure browser
    browser_config = BrowserConfig(
        headless=True,
        user_agent="Mozilla/5.0 Educational Research Bot",
        viewport_size=(1920, 1080)
    )

    # Configure extraction strategy
    if css_selectors:
        extraction_strategy = JsonCssExtractionStrategy(
            extractions=[
                {"name": name, "css": selector, "type": "text"}
                for name, selector in css_selectors.items()
            ]
        )
    else:
        extraction_strategy = None

    # Configure crawler run
    config = CrawlerRunConfig(
        url=url,
        extraction_strategy=extraction_strategy,
        js_code=js_code,
        wait_for=wait_for if wait_for else "selector:body",
        delay=1.0,  # Rate limiting
        # Get clean markdown optimized for LLMs
        css_filters=["nav", "footer", ".advertisement", "#cookie-banner"]
    )

    # Execute crawl
    async with AsyncWebCrawler(config=browser_config) as crawler:
        result = await crawler.arun(config)

        return {
            "url": result.url,
            "markdown": result.markdown.fit_markdown,  # Clean markdown
            "raw_markdown": result.markdown.raw_markdown,  # Original
            "html": result.raw_html,
            "extracted_data": result.extracted_data,
            "metadata": result.metadata,
            "success": result.success
        }

# Example usage for NCCA curriculum page
async def scrape_ncca_curriculum(url: str, language: str = "en"):
    """Extract curriculum content from NCCA website"""

    css_selectors = {
        "title": "h1.page-title",
        "breadcrumbs": "nav.breadcrumb",
        "content": "div.main-content",
        "learning_outcomes": "div.learning-outcomes li",
        "subject_area": "span.subject-tag"
    }

    # Custom JS to expand collapsed sections
    js_code = """
        // Expand all collapsible sections
        document.querySelectorAll('.collapsible').forEach(el => {
            if (!el.classList.contains('active')) {
                el.click();
            }
        });

        // Wait for content to load
        await new Promise(r => setTimeout(r, 1000));
    """

    result = await scrape_with_crawl4ai(
        url=url,
        css_selectors=css_selectors,
        wait_for="selector:div.main-content",
        js_code=js_code
    )

    return result
```

### 4.5 PDF Extraction Strategy

**Critical for examinations.ie and document-heavy sites:**

```python
import pdfplumber
from typing import Dict, List

def extract_pdf_content(pdf_path: str) -> Dict[str, Any]:
    """Extract text and metadata from PDF"""

    with pdfplumber.open(pdf_path) as pdf:
        text_content = []
        metadata = pdf.metadata

        for page in pdf.pages:
            # Extract text
            text = page.extract_text()
            text_content.append(text)

            # Extract tables if present
            tables = page.extract_tables()

        return {
            "text": "\n\n".join(text_content),
            "metadata": metadata,
            "page_count": len(pdf.pages),
            "has_tables": bool(tables)
        }
```

---

## 5. Bilingual Content Mapping

### 5.1 URL-Based Mapping

**Strategy:** Detect corresponding bilingual pages through URL patterns

```python
import re
from urllib.parse import urlparse, urlunparse

def find_bilingual_pair(url: str, source_lang: str, target_lang: str) -> str:
    """
    Convert URL from one language to another

    Examples:
        https://www.ncca.ie/en/primary/mathematics
        -> https://www.ncca.ie/ga/bunscoil/matamaitic

        https://www.scoilnet.ie/go-to-primary/
        -> https://www.scoilnet.ie/ga/go-to-primary/
    """

    parsed = urlparse(url)
    path = parsed.path

    # Pattern 1: Direct language code replacement
    if f"/{source_lang}/" in path:
        new_path = path.replace(f"/{source_lang}/", f"/{target_lang}/")
        return urlunparse(parsed._replace(path=new_path))

    # Pattern 2: Add language prefix for Irish
    if source_lang == "en" and target_lang == "ga":
        if not path.startswith("/ga/"):
            new_path = f"/ga{path}"
            return urlunparse(parsed._replace(path=new_path))

    # Pattern 3: Remove language prefix for English
    if source_lang == "ga" and target_lang == "en":
        new_path = path.replace("/ga/", "/")
        return urlunparse(parsed._replace(path=new_path))

    return None

def verify_bilingual_pair(url_en: str, url_ga: str) -> float:
    """
    Verify that two URLs are likely bilingual pairs
    Returns confidence score 0.0-1.0
    """

    # Check URL structure similarity
    path_en = urlparse(url_en).path
    path_ga = urlparse(url_ga).path

    # Remove language codes for comparison
    path_en_clean = path_en.replace("/en/", "/").replace("/ga/", "/")
    path_ga_clean = path_ga.replace("/en/", "/").replace("/ga/", "/")

    # Calculate path similarity
    if path_en_clean == path_ga_clean:
        return 1.0

    # Check for slug translations (e.g., "primary" -> "bunscoil")
    slug_mapping = load_slug_translations()
    # ... implementation details

    return 0.5  # Partial confidence
```

### 5.2 Content-Based Alignment

**When URLs don't map directly:**

```python
from sentence_transformers import SentenceTransformer
import numpy as np

class BilingualAligner:
    def __init__(self):
        # Use multilingual sentence embedding model
        self.model = SentenceTransformer('LaBSE')  # Language-agnostic BERT

    def find_matching_content(
        self,
        source_content: str,
        candidate_contents: List[Dict[str, str]]
    ) -> List[tuple]:
        """
        Find matching bilingual content using semantic similarity

        Returns list of (candidate, similarity_score) tuples
        """

        # Embed source content
        source_embedding = self.model.encode(source_content)

        # Embed all candidates
        candidate_embeddings = self.model.encode(
            [c["content"] for c in candidate_contents]
        )

        # Calculate cosine similarities
        similarities = np.dot(candidate_embeddings, source_embedding) / (
            np.linalg.norm(candidate_embeddings, axis=1) *
            np.linalg.norm(source_embedding)
        )

        # Return sorted matches
        results = [
            (candidate_contents[i], similarities[i])
            for i in range(len(candidate_contents))
        ]
        results.sort(key=lambda x: x[1], reverse=True)

        return results
```

### 5.3 Paragraph-Level Alignment

**For documents with parallel structure:**

```python
from typing import List, Tuple
import difflib

def align_paragraphs(
    english_paragraphs: List[str],
    irish_paragraphs: List[str]
) -> List[Tuple[str, str, float]]:
    """
    Align paragraphs between English and Irish versions

    Returns list of (english_para, irish_para, confidence) tuples
    """

    alignments = []

    # If paragraph counts match, assume parallel structure
    if len(english_paragraphs) == len(irish_paragraphs):
        for en, ga in zip(english_paragraphs, irish_paragraphs):
            alignments.append((en, ga, 0.9))  # High confidence

    else:
        # Use dynamic programming alignment
        # Similar to sequence alignment in bioinformatics
        aligner = BilingualAligner()

        for en_para in english_paragraphs:
            matches = aligner.find_matching_content(
                en_para,
                [{"content": p} for p in irish_paragraphs]
            )

            if matches and matches[0][1] > 0.7:  # Similarity threshold
                best_match = matches[0]
                ga_para = best_match[0]["content"]
                confidence = best_match[1]
                alignments.append((en_para, ga_para, confidence))

    return alignments
```

### 5.4 HTML Structure Preservation

**Maintain document structure for context:**

```python
from bs4 import BeautifulSoup
from typing import Dict, Any

def extract_structured_content(html: str) -> Dict[str, Any]:
    """
    Extract content while preserving structure
    """

    soup = BeautifulSoup(html, 'html.parser')

    structure = {
        "title": "",
        "headings": [],
        "sections": [],
        "lists": [],
        "tables": []
    }

    # Extract title
    title = soup.find('h1')
    if title:
        structure["title"] = title.get_text(strip=True)

    # Extract hierarchical headings
    for level in range(1, 7):
        for heading in soup.find_all(f'h{level}'):
            structure["headings"].append({
                "level": level,
                "text": heading.get_text(strip=True),
                "id": heading.get('id', '')
            })

    # Extract sections
    for section in soup.find_all(['section', 'div'], class_=re.compile(r'section|content')):
        section_data = {
            "heading": "",
            "paragraphs": [],
            "lists": []
        }

        # Find section heading
        heading = section.find(['h2', 'h3', 'h4'])
        if heading:
            section_data["heading"] = heading.get_text(strip=True)

        # Extract paragraphs
        for para in section.find_all('p'):
            text = para.get_text(strip=True)
            if text:
                section_data["paragraphs"].append(text)

        # Extract lists
        for ul in section.find_all(['ul', 'ol']):
            list_items = [li.get_text(strip=True) for li in ul.find_all('li')]
            section_data["lists"].append({
                "type": ul.name,
                "items": list_items
            })

        if section_data["paragraphs"] or section_data["lists"]:
            structure["sections"].append(section_data)

    return structure
```

### 5.5 Terminology Extraction

**Extract domain-specific terminology:**

```python
from collections import defaultdict
import spacy

class TerminologyExtractor:
    def __init__(self):
        self.nlp_en = spacy.load("en_core_web_sm")
        # Note: Irish spacy model may not be available
        # Use custom rules or LLM-based extraction

    def extract_terminology_pairs(
        self,
        english_content: str,
        irish_content: str,
        alignments: List[Tuple[str, str, float]]
    ) -> List[TerminologyPair]:
        """
        Extract technical terms and their translations
        """

        terms = []

        for en_segment, ga_segment, confidence in alignments:
            # Extract English noun phrases
            doc_en = self.nlp_en(en_segment)
            en_terms = [
                chunk.text
                for chunk in doc_en.noun_chunks
                if len(chunk.text.split()) <= 4  # Max 4-word terms
            ]

            # Use LLM to find Irish equivalents
            for en_term in en_terms:
                ga_term = self.find_irish_equivalent(
                    en_term, ga_segment
                )

                if ga_term:
                    terms.append(TerminologyPair(
                        term_id=generate_id(en_term),
                        english_term=en_term,
                        irish_term=ga_term,
                        subject_area="",  # To be classified
                        occurrence_count=1
                    ))

        return terms

    def find_irish_equivalent(self, en_term: str, ga_segment: str) -> str:
        """Use LLM to find Irish translation of term"""

        # Use OpenAI or local model
        prompt = f"""
        Given the English term: "{en_term}"
        And the Irish text: "{ga_segment}"

        Identify the Irish equivalent of the English term if it appears in the Irish text.
        Return only the Irish term, or "NOT_FOUND" if not present.
        """

        # ... LLM call implementation
        return ""  # Placeholder
```

---

## 6. Implementation Plan

### 6.1 Project Structure

```
hackathon/
├── bilingual-scraper/
│   ├── __init__.py
│   ├── config/
│   │   ├── __init__.py
│   │   ├── websites.py          # Website configurations
│   │   ├── selectors.py         # CSS/XPath selectors
│   │   └── settings.py          # Global settings
│   ├── scrapers/
│   │   ├── __init__.py
│   │   ├── base.py              # Base scraper class
│   │   ├── ncca_scraper.py      # NCCA-specific scraper
│   │   ├── scoilnet_scraper.py  # Scoilnet-specific
│   │   ├── examinations_scraper.py
│   │   ├── oide_scraper.py
│   │   └── curriculum_scraper.py
│   ├── alignment/
│   │   ├── __init__.py
│   │   ├── url_mapper.py        # URL-based alignment
│   │   ├── content_aligner.py   # Semantic alignment
│   │   ├── paragraph_aligner.py # Fine-grained alignment
│   │   └── terminology.py       # Term extraction
│   ├── extractors/
│   │   ├── __init__.py
│   │   ├── html_extractor.py
│   │   ├── pdf_extractor.py
│   │   ├── structure_parser.py
│   │   └── metadata_extractor.py
│   ├── quality/
│   │   ├── __init__.py
│   │   ├── validators.py        # Content validation
│   │   ├── language_detector.py # Language detection
│   │   └── quality_scorer.py    # Quality metrics
│   ├── storage/
│   │   ├── __init__.py
│   │   ├── duckdb_manager.py
│   │   ├── lancedb_manager.py
│   │   ├── parquet_writer.py
│   │   └── huggingface_exporter.py
│   ├── pipelines/
│   │   ├── __init__.py
│   │   ├── discovery_pipeline.py
│   │   ├── scraping_pipeline.py
│   │   ├── alignment_pipeline.py
│   │   └── export_pipeline.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── schemas.py           # Pydantic models
│   │   └── database.py          # Database models
│   └── utils/
│       ├── __init__.py
│       ├── rate_limiter.py
│       ├── cache.py
│       ├── logging_config.py
│       └── helpers.py
├── data/
│   ├── raw/                     # Raw scraped data
│   ├── processed/               # Processed & aligned
│   ├── exports/                 # Final exports
│   └── cache/                   # Response cache
├── tests/
│   ├── unit/
│   ├── integration/
│   └── fixtures/
├── notebooks/
│   ├── exploration.ipynb
│   ├── quality_analysis.ipynb
│   └── statistics.ipynb
├── scripts/
│   ├── run_discovery.py
│   ├── run_scraping.py
│   ├── run_alignment.py
│   └── export_datasets.py
├── pyproject.toml
├── requirements.txt
└── README.md
```

### 6.2 Implementation Phases

#### Phase 1: Foundation (Week 1)

**Tasks:**
1. Set up project structure
2. Define data schemas (Pydantic models)
3. Configure database connections (DuckDB, LanceDB)
4. Implement base scraper class
5. Create rate limiting and caching utilities
6. Set up logging and monitoring

**Deliverables:**
- Project scaffold
- Database schemas
- Base infrastructure
- Unit tests for core utilities

#### Phase 2: Website Discovery (Week 1-2)

**Tasks:**
1. Implement sitemap parsers
2. Create robots.txt compliance checker
3. Build URL discovery crawler
4. Implement language pattern detection
5. Create URL queue manager
6. Test on one website (start with scoilnet.ie)

**Deliverables:**
- Discovery pipeline
- URL inventory for each website
- Language pattern mappings
- Discovery statistics

#### Phase 3: Content Extraction (Week 2-3)

**Tasks:**
1. Implement website-specific scrapers
2. Create HTML content extractors
3. Build PDF extraction pipeline
4. Implement JavaScript rendering
5. Create metadata extractors
6. Test extraction quality

**Deliverables:**
- 5 website-specific scrapers
- PDF extraction pipeline
- Raw content database
- Extraction quality metrics

#### Phase 4: Bilingual Alignment (Week 3-4)

**Tasks:**
1. Implement URL-based mapping
2. Build content-based semantic alignment
3. Create paragraph-level alignment
4. Implement terminology extraction
5. Add quality scoring
6. Manual validation of sample pairs

**Deliverables:**
- Bilingual document pairs
- Alignment confidence scores
- Terminology database
- Quality reports

#### Phase 5: Quality Assurance (Week 4-5)

**Tasks:**
1. Implement language detection validation
2. Create duplicate detection
3. Build quality scoring system
4. Manual review of samples
5. Error correction pipeline
6. Generate quality statistics

**Deliverables:**
- Quality validation pipeline
- Quality metrics dashboard
- Corrected dataset
- Quality assurance report

#### Phase 6: Export & Publishing (Week 5-6)

**Tasks:**
1. Implement export formats (Parquet, JSONL, TMX)
2. Create HuggingFace dataset preparation
3. Generate dataset documentation
4. Create data cards
5. Upload to HuggingFace
6. Create usage examples

**Deliverables:**
- Multiple export formats
- HuggingFace dataset
- Documentation
- Usage examples
- Public dataset release

### 6.3 DLT + Cocoindex Pipeline Implementation

**Integration with Crawl4AI and Cocoindex:**

```python
# bilingual-scraper/pipelines/scraping_pipeline.py

import dlt
import cocoindex
import asyncio
from typing import Iterator, Dict, Any
from dataclasses import dataclass
from crawl4ai import AsyncWebCrawler, BrowserConfig, CrawlerRunConfig
from crawl4ai.extraction import JsonCssExtractionStrategy, LLMExtractionStrategy
from pydantic import BaseModel

# Define structured extraction schemas
class CurriculumContent(BaseModel):
    """Pydantic model for curriculum content extraction"""
    title: str
    subject: str
    level: str  # primary, junior_cycle, senior_cycle
    learning_outcomes: list[str]
    key_concepts: list[str]
    content_summary: str

class BilingualPair(BaseModel):
    """Bilingual content pair"""
    english_url: str
    irish_url: str
    english_content: CurriculumContent
    irish_content: CurriculumContent

# Cocoindex flow for content extraction and knowledge graph building
@cocoindex.flow_def(name="BilingualEducationFlow")
def bilingual_education_flow(
    flow_builder: cocoindex.FlowBuilder,
    data_scope: cocoindex.DataScope
) -> None:
    """
    Cocoindex flow for extracting bilingual educational content
    and building knowledge graph relationships
    """

    # Add bilingual document collector
    bilingual_docs = data_scope.add_collector()
    terminology_pairs = data_scope.add_collector()
    content_relationships = data_scope.add_collector()

    # Assuming we have scraped content in data_scope["documents"]
    with data_scope["documents"].row() as doc:
        # Extract structured content using LLM
        doc["structured"] = doc["markdown"].transform(
            cocoindex.functions.ExtractByLlm(
                llm_spec=cocoindex.LlmSpec(
                    api_type=cocoindex.LlmApiType.OPENAI,
                    model="gpt-4o",
                    # Or use Ollama for cost savings:
                    # api_type=cocoindex.LlmApiType.OLLAMA,
                    # model="llama3.2",
                ),
                output_type=CurriculumContent,
                instruction="Extract curriculum content details including learning outcomes and key concepts."
            )
        )

        # Collect bilingual document
        bilingual_docs.collect(
            id=cocoindex.GeneratedField.UUID,
            source_website=doc["source"],
            english_url=doc["url_en"],
            irish_url=doc["url_ga"],
            english_content=doc["structured"]["en"],
            irish_content=doc["structured"]["ga"],
            content_type=doc["content_type"],
            subject_area=doc["subject"],
        )

        # Extract terminology pairs
        doc["terms"] = doc["markdown"].transform(
            cocoindex.functions.ExtractByLlm(
                llm_spec=cocoindex.LlmSpec(
                    api_type=cocoindex.LlmApiType.OPENAI,
                    model="gpt-4o-mini",  # Cheaper model for term extraction
                ),
                output_type=list[dict],
                instruction="""
                Extract Irish-English terminology pairs from this bilingual content.
                Return a list of dictionaries with 'english_term' and 'irish_term' keys.
                Focus on educational terminology and subject-specific vocabulary.
                """
            )
        )

        with doc["terms"].row() as term:
            terminology_pairs.collect(
                id=cocoindex.GeneratedField.UUID,
                english_term=term["english_term"],
                irish_term=term["irish_term"],
                document_id=doc["id"],
                subject_area=doc["subject"]
            )

    # Export to databases
    bilingual_docs.export(
        "bilingual_documents",
        cocoindex.targets.Postgres(
            connection=cocoindex.add_auth_entry(
                "PostgresConnection",
                cocoindex.targets.PostgresConnection(
                    host="localhost",
                    database="irish_education",
                    user="postgres",
                    password=os.getenv("POSTGRES_PASSWORD")
                )
            ),
            table_name="bilingual_documents"
        ),
        primary_key_fields=["id"]
    )

    terminology_pairs.export(
        "terminology_pairs",
        cocoindex.targets.Postgres(...),
        primary_key_fields=["id"]
    )

    # Export to Neo4j knowledge graph
    bilingual_docs.export(
        "doc_nodes",
        cocoindex.targets.Neo4j(
            connection=neo4j_conn_spec,
            mapping=cocoindex.targets.Nodes(label="EducationalResource")
        ),
        primary_key_fields=["id"]
    )


# DLT resource using Crawl4AI
@dlt.resource(
    write_disposition="merge",
    primary_key="id",
    table_name="scraped_pages"
)
async def crawl_bilingual_websites(
    websites: list[str] = None,
    max_pages_per_site: int = 100
) -> Iterator[Dict[str, Any]]:
    """DLT resource for crawling bilingual content with Crawl4AI"""

    if websites is None:
        websites = ["ncca.ie", "scoilnet.ie"]

    browser_config = BrowserConfig(
        headless=True,
        viewport_size=(1920, 1080)
    )

    async with AsyncWebCrawler(config=browser_config) as crawler:
        for website in websites:
            # Discover URLs (simplified - in practice use BFS deep crawl)
            base_urls_en = [
                f"https://www.{website}/en/primary/",
                f"https://www.{website}/en/junior-cycle/",
                f"https://www.{website}/en/senior-cycle/"
            ]

            for url_en in base_urls_en:
                # Determine Irish URL
                url_ga = url_en.replace("/en/", "/ga/")

                # Crawl English version
                config_en = CrawlerRunConfig(
                    url=url_en,
                    delay=1.0,
                    css_filters=["nav", "footer", ".cookie-banner"]
                )
                result_en = await crawler.arun(config_en)

                # Crawl Irish version
                config_ga = CrawlerRunConfig(
                    url=url_ga,
                    delay=1.0,
                    css_filters=["nav", "footer", ".cookie-banner"]
                )
                result_ga = await crawler.arun(config_ga)

                if result_en.success and result_ga.success:
                    yield {
                        "id": f"{website}_{hash(url_en)}",
                        "source": website,
                        "url_en": result_en.url,
                        "url_ga": result_ga.url,
                        "markdown_en": result_en.markdown.fit_markdown,
                        "markdown_ga": result_ga.markdown.fit_markdown,
                        "content_type": "curriculum",
                        "scraped_at": datetime.utcnow()
                    }

@dlt.source
def bilingual_education_source(
    websites: list[str] = None,
    max_pages_per_site: int = 100
):
    """DLT source for bilingual education content"""
    return crawl_bilingual_websites(
        websites=websites,
        max_pages_per_site=max_pages_per_site
    )

# Combined DLT + Cocoindex pipeline
if __name__ == "__main__":
    # Step 1: Crawl with DLT + Crawl4AI
    pipeline = dlt.pipeline(
        pipeline_name="bilingual_education_scraper",
        destination="duckdb",
        dataset_name="irish_education"
    )

    load_info = pipeline.run(
        bilingual_education_source(
            websites=["scoilnet.ie"],
            max_pages_per_site=50
        )
    )

    print("DLT Crawling complete:", load_info)

    # Step 2: Process with Cocoindex
    # Load scraped data and run through Cocoindex flow
    flow = cocoindex.Flow(name="BilingualEducationProcessor")
    # ... configure and run Cocoindex flow
```

### 6.4 Monitoring & Logging

```python
# bilingual-scraper/utils/logging_config.py

import logging
from datetime import datetime
from pathlib import Path

def setup_logging(log_dir: Path = None):
    """Configure comprehensive logging"""

    if log_dir is None:
        log_dir = Path("logs")
    log_dir.mkdir(exist_ok=True)

    # Create timestamped log file
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = log_dir / f"scraper_{timestamp}.log"

    # Configure root logger
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file),
            logging.StreamHandler()  # Also print to console
        ]
    )

    # Create specific loggers
    loggers = {
        "scraper": logging.getLogger("scraper"),
        "alignment": logging.getLogger("alignment"),
        "quality": logging.getLogger("quality"),
        "storage": logging.getLogger("storage")
    }

    return loggers

# Usage in scraper
logger = logging.getLogger("scraper.ncca")
logger.info(f"Starting scraping of {url}")
logger.warning(f"Failed to find Irish version for {url}")
logger.error(f"Scraping error: {error}", exc_info=True)
```

---

## 7. Quality Assurance

### 7.1 Language Detection Validation

```python
from langdetect import detect, DetectorFactory
from typing import Tuple

# Set seed for consistent results
DetectorFactory.seed = 0

def validate_language(text: str, expected_lang: str) -> Tuple[bool, str]:
    """
    Validate that text is in expected language

    Returns (is_valid, detected_language)
    """

    if not text or len(text.strip()) < 10:
        return False, "insufficient_text"

    try:
        detected = detect(text)

        # Map ISO codes
        lang_map = {"ga": "ga", "en": "en"}
        expected = lang_map.get(expected_lang)

        return detected == expected, detected

    except Exception as e:
        return False, f"error_{str(e)}"

def validate_bilingual_pair(doc: BilingualDocument) -> Dict[str, Any]:
    """Comprehensive validation of bilingual pair"""

    validation_results = {
        "valid": True,
        "issues": [],
        "warnings": []
    }

    # Language detection
    en_valid, en_detected = validate_language(doc.english_content, "en")
    ga_valid, ga_detected = validate_language(doc.irish_content, "ga")

    if not en_valid:
        validation_results["issues"].append(
            f"English content detected as {en_detected}"
        )
        validation_results["valid"] = False

    if not ga_valid:
        validation_results["issues"].append(
            f"Irish content detected as {ga_detected}"
        )
        validation_results["valid"] = False

    # Length ratio check (Irish typically 10-30% longer than English)
    length_ratio = doc.irish_word_count / max(doc.english_word_count, 1)
    if length_ratio < 0.5 or length_ratio > 2.0:
        validation_results["warnings"].append(
            f"Unusual length ratio: {length_ratio:.2f}"
        )

    # Content similarity check (should be similar but not identical)
    if doc.english_content.lower() == doc.irish_content.lower():
        validation_results["issues"].append("Identical content in both languages")
        validation_results["valid"] = False

    # Alignment confidence check
    if doc.alignment_confidence < 0.6:
        validation_results["warnings"].append(
            f"Low alignment confidence: {doc.alignment_confidence}"
        )

    return validation_results
```

### 7.2 Quality Scoring

```python
def calculate_quality_score(doc: BilingualDocument) -> float:
    """
    Calculate overall quality score (0.0-1.0)

    Factors:
    - Language detection accuracy
    - Content completeness
    - Alignment confidence
    - Structural similarity
    - Word count ratios
    """

    scores = []

    # Language detection (25%)
    en_valid, _ = validate_language(doc.english_content, "en")
    ga_valid, _ = validate_language(doc.irish_content, "ga")
    lang_score = (int(en_valid) + int(ga_valid)) / 2
    scores.append(lang_score * 0.25)

    # Content completeness (20%)
    min_words = 10
    en_complete = doc.english_word_count >= min_words
    ga_complete = doc.irish_word_count >= min_words
    completeness_score = (int(en_complete) + int(ga_complete)) / 2
    scores.append(completeness_score * 0.20)

    # Alignment confidence (30%)
    scores.append(doc.alignment_confidence * 0.30)

    # Length ratio (15%)
    length_ratio = doc.irish_word_count / max(doc.english_word_count, 1)
    # Ideal ratio is around 1.0-1.3 (Irish often slightly longer)
    if 0.8 <= length_ratio <= 1.5:
        ratio_score = 1.0
    elif 0.5 <= length_ratio <= 2.0:
        ratio_score = 0.7
    else:
        ratio_score = 0.3
    scores.append(ratio_score * 0.15)

    # Structural similarity (10%)
    # Compare heading counts, paragraph counts, etc.
    structure_score = compare_structure(doc)
    scores.append(structure_score * 0.10)

    return sum(scores)
```

### 7.3 Duplicate Detection

```python
from hashlib import md5
from typing import Set

class DuplicateDetector:
    def __init__(self):
        self.seen_hashes: Set[str] = set()
        self.seen_urls: Set[tuple] = set()

    def is_duplicate(self, doc: BilingualDocument) -> bool:
        """Check if document is duplicate"""

        # URL-based deduplication
        url_pair = (doc.english_url, doc.irish_url)
        if url_pair in self.seen_urls:
            return True

        # Content-based deduplication
        content_hash = self._hash_content(doc)
        if content_hash in self.seen_hashes:
            return True

        # Mark as seen
        self.seen_urls.add(url_pair)
        self.seen_hashes.add(content_hash)

        return False

    def _hash_content(self, doc: BilingualDocument) -> str:
        """Generate content hash"""

        # Normalize text
        en_normalized = " ".join(doc.english_content.lower().split())
        ga_normalized = " ".join(doc.irish_content.lower().split())

        # Create hash
        combined = f"{en_normalized}||{ga_normalized}"
        return md5(combined.encode()).hexdigest()
```

### 7.4 Manual Review Process

**Strategy for manual validation:**

1. **Sample Selection**
   - Random sample (10% of dataset)
   - Stratified by website and content type
   - Focus on low-confidence alignments
   - Edge cases and warnings

2. **Review Criteria**
   - Correct language detection
   - Accurate alignment
   - Content quality
   - Translation accuracy
   - Completeness

3. **Review Interface**
   - Web-based review tool
   - Side-by-side comparison
   - Rating system (1-5 stars)
   - Issue tagging
   - Comments

4. **Feedback Loop**
   - Adjust alignment algorithms
   - Update quality thresholds
   - Retrain models if needed
   - Filter low-quality pairs

---

## 8. Storage & Export

### 8.1 Multi-Database Strategy

**Why Multiple Databases:**

1. **DuckDB** - Analytics and querying
   - Fast aggregations
   - SQL interface
   - Efficient for analysis
   - Good for development

2. **LanceDB** - Vector embeddings
   - Semantic search
   - Similarity matching
   - Fast vector queries
   - ML/AI ready

3. **Parquet** - Long-term storage
   - Columnar format
   - Highly compressed
   - Cloud-compatible
   - Industry standard

4. **PostgreSQL** (Optional) - Production relational DB
   - ACID compliance
   - Complex queries
   - Concurrent access
   - Robust ecosystem

### 8.2 Storage Implementation

```python
# bilingual-scraper/storage/duckdb_manager.py

import duckdb
from pathlib import Path
from typing import List
from models.schemas import BilingualDocument

class DuckDBManager:
    def __init__(self, db_path: Path):
        self.db_path = db_path
        self.conn = duckdb.connect(str(db_path))
        self._create_tables()

    def _create_tables(self):
        """Create database schema"""

        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS bilingual_documents (
                id VARCHAR PRIMARY KEY,
                source_website VARCHAR NOT NULL,
                english_url VARCHAR NOT NULL,
                english_title VARCHAR,
                english_content TEXT,
                english_word_count INTEGER,
                irish_url VARCHAR NOT NULL,
                irish_title VARCHAR,
                irish_content TEXT,
                irish_word_count INTEGER,
                content_type VARCHAR,
                education_level VARCHAR,
                subject_area VARCHAR,
                alignment_confidence FLOAT,
                alignment_method VARCHAR,
                quality_score FLOAT,
                scraped_at TIMESTAMP,
                published_date TIMESTAMP
            )
        """)

        # Add indexes
        self.conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_source
            ON bilingual_documents(source_website)
        """)

        self.conn.execute("""
            CREATE INDEX IF NOT EXISTS idx_content_type
            ON bilingual_documents(content_type)
        """)

    def insert_document(self, doc: BilingualDocument):
        """Insert bilingual document"""

        self.conn.execute("""
            INSERT INTO bilingual_documents VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, [
            doc.id,
            doc.source_website,
            str(doc.english_url),
            doc.english_title,
            doc.english_content,
            doc.english_word_count,
            str(doc.irish_url),
            doc.irish_title,
            doc.irish_content,
            doc.irish_word_count,
            doc.content_type.value,
            doc.education_level.value if doc.education_level else None,
            doc.subject_area,
            doc.alignment_confidence,
            doc.alignment_method,
            doc.quality_score,
            doc.scraped_at,
            doc.published_date
        ])

    def get_statistics(self) -> dict:
        """Get dataset statistics"""

        stats = {}

        # Total documents
        stats["total_documents"] = self.conn.execute(
            "SELECT COUNT(*) FROM bilingual_documents"
        ).fetchone()[0]

        # By website
        stats["by_website"] = self.conn.execute("""
            SELECT source_website, COUNT(*) as count
            FROM bilingual_documents
            GROUP BY source_website
            ORDER BY count DESC
        """).fetchall()

        # By content type
        stats["by_content_type"] = self.conn.execute("""
            SELECT content_type, COUNT(*) as count
            FROM bilingual_documents
            GROUP BY content_type
            ORDER BY count DESC
        """).fetchall()

        # Quality distribution
        stats["avg_quality"] = self.conn.execute("""
            SELECT AVG(quality_score) FROM bilingual_documents
        """).fetchone()[0]

        return stats
```

```python
# bilingual-scraper/storage/lancedb_manager.py

import lancedb
from pathlib import Path
from sentence_transformers import SentenceTransformer
from models.schemas import BilingualDocument

class LanceDBManager:
    def __init__(self, db_path: Path):
        self.db = lancedb.connect(str(db_path))
        self.encoder = SentenceTransformer('LaBSE')  # Multilingual

        # Create tables if they don't exist
        self._init_tables()

    def _init_tables(self):
        """Initialize LanceDB tables"""

        try:
            self.doc_table = self.db.open_table("document_embeddings")
        except:
            # Create table with first record
            pass

    def add_document(self, doc: BilingualDocument):
        """Add document embeddings to LanceDB"""

        # Generate embeddings for both languages
        en_embedding = self.encoder.encode(doc.english_content)
        ga_embedding = self.encoder.encode(doc.irish_content)

        # Store English version
        en_record = {
            "id": f"{doc.id}_en",
            "document_id": doc.id,
            "language": "en",
            "content_type": doc.content_type.value,
            "subject_area": doc.subject_area or "",
            "embedding": en_embedding.tolist(),
            "text": doc.english_content[:1000],  # Store snippet
            "metadata": {
                "url": str(doc.english_url),
                "title": doc.english_title,
                "website": doc.source_website
            }
        }

        # Store Irish version
        ga_record = {
            "id": f"{doc.id}_ga",
            "document_id": doc.id,
            "language": "ga",
            "content_type": doc.content_type.value,
            "subject_area": doc.subject_area or "",
            "embedding": ga_embedding.tolist(),
            "text": doc.irish_content[:1000],
            "metadata": {
                "url": str(doc.irish_url),
                "title": doc.irish_title,
                "website": doc.source_website
            }
        }

        # Insert into table
        if not hasattr(self, 'doc_table'):
            self.doc_table = self.db.create_table(
                "document_embeddings",
                data=[en_record, ga_record]
            )
        else:
            self.doc_table.add([en_record, ga_record])

    def search_similar(
        self,
        query: str,
        language: str = "en",
        limit: int = 10
    ):
        """Search for similar documents"""

        query_embedding = self.encoder.encode(query)

        results = self.doc_table.search(query_embedding) \
            .where(f"language = '{language}'") \
            .limit(limit) \
            .to_list()

        return results
```

### 8.3 Export Formats

```python
# bilingual-scraper/storage/parquet_writer.py

import pyarrow as pa
import pyarrow.parquet as pq
from pathlib import Path
from typing import List
from models.schemas import BilingualDocument

class ParquetExporter:
    def __init__(self, output_dir: Path):
        self.output_dir = output_dir
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def export_documents(
        self,
        documents: List[BilingualDocument],
        filename: str = "bilingual_dataset.parquet"
    ):
        """Export documents to Parquet format"""

        # Convert to PyArrow table
        data = {
            "id": [doc.id for doc in documents],
            "source_website": [doc.source_website for doc in documents],
            "english_text": [doc.english_content for doc in documents],
            "irish_text": [doc.irish_content for doc in documents],
            "english_url": [str(doc.english_url) for doc in documents],
            "irish_url": [str(doc.irish_url) for doc in documents],
            "content_type": [doc.content_type.value for doc in documents],
            "education_level": [
                doc.education_level.value if doc.education_level else None
                for doc in documents
            ],
            "subject_area": [doc.subject_area for doc in documents],
            "alignment_confidence": [doc.alignment_confidence for doc in documents],
            "quality_score": [doc.quality_score for doc in documents],
            "english_word_count": [doc.english_word_count for doc in documents],
            "irish_word_count": [doc.irish_word_count for doc in documents],
            "scraped_at": [doc.scraped_at for doc in documents],
        }

        table = pa.Table.from_pydict(data)

        # Write to Parquet
        output_path = self.output_dir / filename
        pq.write_table(table, output_path, compression='snappy')

        print(f"Exported {len(documents)} documents to {output_path}")
        return output_path
```

```python
# bilingual-scraper/storage/huggingface_exporter.py

from datasets import Dataset, DatasetDict
from typing import List
from models.schemas import BilingualDocument

class HuggingFaceExporter:
    def prepare_dataset(
        self,
        documents: List[BilingualDocument],
        train_split: float = 0.9
    ) -> DatasetDict:
        """Prepare dataset for HuggingFace upload"""

        # Convert to HuggingFace format
        data = {
            "id": [],
            "translation": [],
            "metadata": []
        }

        for doc in documents:
            data["id"].append(doc.id)
            data["translation"].append({
                "en": doc.english_content,
                "ga": doc.irish_content
            })
            data["metadata"].append({
                "source": doc.source_website,
                "content_type": doc.content_type.value,
                "subject_area": doc.subject_area,
                "education_level": doc.education_level.value if doc.education_level else None,
                "url_en": str(doc.english_url),
                "url_ga": str(doc.irish_url),
                "quality_score": doc.quality_score,
                "alignment_confidence": doc.alignment_confidence
            })

        # Create dataset
        dataset = Dataset.from_dict(data)

        # Split into train/test
        split_dataset = dataset.train_test_split(
            test_size=1-train_split,
            seed=42
        )

        return DatasetDict({
            "train": split_dataset["train"],
            "test": split_dataset["test"]
        })

    def upload_to_hub(
        self,
        dataset: DatasetDict,
        repo_name: str,
        private: bool = False
    ):
        """Upload dataset to HuggingFace Hub"""

        dataset.push_to_hub(
            repo_name,
            private=private,
            token=os.getenv("HUGGINGFACE_TOKEN")
        )

        print(f"Uploaded dataset to: https://huggingface.co/datasets/{repo_name}")
```

---

## 9. Challenges & Solutions

### 9.1 Technical Challenges

#### Challenge 1: Bot Detection & 403 Errors

**Problem:** Websites blocking automated scraping (as seen with ncca.ie, oide.ie)

**Solutions:**
1. **Realistic User-Agent strings**
   ```python
   headers = {
       "User-Agent": "Mozilla/5.0 (Educational Research Bot; +http://yoursite.com/bot)"
   }
   ```

2. **Request Headers Rotation**
   - Rotate user agents
   - Include realistic headers (Accept, Accept-Language, Referer)
   - Session persistence

3. **Rate Limiting**
   - Slow down requests (1-2 per second)
   - Random delays between requests
   - Respect peak/off-peak hours

4. **IP Rotation** (if necessary)
   - Use proxy rotation
   - Residential proxies
   - Cloud provider rotation

5. **Manual Fallback**
   - Download sitemaps manually
   - Use official data exports if available
   - Contact website administrators for permission

#### Challenge 2: JavaScript-Heavy Sites

**Problem:** Content loaded dynamically via JavaScript

**Solutions:**
1. **Playwright/Puppeteer**
   - Full browser automation
   - Wait for network idle
   - Execute JavaScript

2. **API Discovery**
   - Inspect network requests
   - Find JSON API endpoints
   - Direct API calls (faster than rendering)

3. **Crawl4AI Deep Crawling**
   - BFS/DFS strategies for site discovery
   - Built-in JavaScript rendering via Playwright
   - LLM-optimized markdown generation
   - Cost-effective (no API costs for basic extraction)

#### Challenge 3: PDF Content Extraction

**Problem:** Critical content in PDF format (especially examinations.ie)

**Solutions:**
1. **PyPDF2/pdfplumber**
   - Good for text-based PDFs
   - Preserves layout

2. **OCR for Scanned PDFs**
   - Tesseract OCR
   - Google Cloud Vision API
   - Quality may vary

3. **Structured Extraction**
   - Detect sections (questions, marking schemes)
   - Parse tables
   - Maintain question numbering

#### Challenge 4: Imperfect URL Mapping

**Problem:** English and Irish URLs don't always follow patterns

**Solutions:**
1. **Heuristic-Based Mapping**
   - URL pattern detection
   - Slug translation dictionary
   - Path structure analysis

2. **Link Following**
   - Find language switcher links
   - Extract href from <link rel="alternate" hreflang="ga">
   - Follow canonical URLs

3. **Content-Based Matching**
   - Semantic similarity
   - Vector embeddings
   - LLM-based classification

4. **Manual Mapping**
   - Create exception rules
   - Maintain mapping database
   - Human verification

#### Challenge 5: Partial Translations

**Problem:** Not all content available in both languages

**Solutions:**
1. **Detection & Flagging**
   - Mark partial translations
   - Include completeness metric
   - Filter in final dataset

2. **Graceful Degradation**
   - Accept partial alignments
   - Lower confidence scores
   - Document gaps

3. **Community Translation**
   - Identify gaps
   - Request translations
   - Crowdsource if feasible

### 9.2 Data Quality Challenges

#### Challenge 1: Mixed Language Content

**Problem:** Pages containing both Irish and English

**Solutions:**
1. **Paragraph-Level Detection**
   - Detect language per paragraph
   - Separate mixed content
   - Align at finer granularity

2. **Language Span Tagging**
   - Tag spans with language
   - Preserve mixed content
   - Metadata annotation

#### Challenge 2: Low-Quality Translations

**Problem:** Machine-translated or poor-quality Irish content

**Solutions:**
1. **Quality Detection**
   - Use UCCIX model for validation
   - Translation quality metrics (BLEU, COMET)
   - Grammar checking

2. **Filtering**
   - Set quality thresholds
   - Flag suspicious content
   - Manual review

3. **Validation**
   - Cross-reference with dictionaries
   - Check terminology consistency
   - Native speaker review

#### Challenge 3: Terminology Inconsistency

**Problem:** Same term translated differently across documents

**Solutions:**
1. **Terminology Database**
   - Extract all term pairs
   - Count occurrences
   - Identify variations

2. **Standardization**
   - Choose canonical translations
   - Document alternatives
   - Apply consistently

3. **Context Preservation**
   - Store term context
   - Allow domain-specific variations
   - Metadata tagging

### 9.3 Legal & Ethical Challenges

#### Challenge 1: Copyright & Licensing

**Problem:** Unclear copyright status of educational materials

**Solutions:**
1. **Research Licenses**
   - Check terms of use
   - Look for open licenses
   - Educational fair use

2. **Attribution**
   - Properly attribute sources
   - Include URLs
   - Respect copyright

3. **Permission Requests**
   - Contact website administrators
   - Request explicit permission
   - Document responses

4. **Dataset Licensing**
   - Choose appropriate license (CC-BY, CC-BY-SA)
   - Respect source licenses
   - Legal review

#### Challenge 2: Data Privacy

**Problem:** Potential personal information in content

**Solutions:**
1. **PII Detection**
   - Screen for names, emails, addresses
   - Automated detection
   - Manual review

2. **Redaction**
   - Remove or anonymize PII
   - Document redactions
   - Preserve utility

#### Challenge 3: Robots.txt Compliance

**Problem:** Some content disallowed by robots.txt

**Solutions:**
1. **Strict Compliance**
   - Parse and respect robots.txt
   - Log disallowed URLs
   - Alternative sources

2. **Permission Requests**
   - Contact for research exemptions
   - Explain educational purpose
   - Negotiate access

---

## 10. Expected Outcomes

### 10.1 Dataset Statistics (Projected)

**Conservative Estimates:**

| Website | Pages | Bilingual Pairs | Word Count (Irish) | Quality Score |
|---------|-------|-----------------|-------------------|---------------|
| NCCA.ie | 500-1000 | 400-800 | 200K-400K | 0.85-0.95 |
| Scoilnet.ie | 2000-5000 | 1500-4000 | 500K-1.5M | 0.75-0.90 |
| Examinations.ie | 1000-2000 | 800-1600 | 1M-3M | 0.90-0.98 |
| Oide.ie | 500-1000 | 300-700 | 150K-350K | 0.80-0.92 |
| Curriculumonline.ie | 500-1000 | 400-800 | 200K-500K | 0.85-0.93 |
| **Total** | **4500-10000** | **3400-8000** | **2M-6M** | **0.80-0.95** |

### 10.2 Dataset Characteristics

**Content Diversity:**
- Primary education (grades 1-6)
- Secondary education (Junior Cycle, Senior Cycle)
- Subject areas: Mathematics, Irish, English, Science, History, Geography, etc.
- Professional development materials
- Assessment and examination content

**Language Features:**
- Formal educational register
- Technical terminology across domains
- Age-appropriate language levels
- Both instructional and descriptive text
- Question-answer formats

**Quality Metrics:**
- >95% correct language detection
- >80% high-confidence alignments
- Minimal duplication (<2%)
- Comprehensive metadata
- Validated terminology pairs

### 10.3 Use Cases

**1. Machine Translation Training**
- Irish-English translation models
- Domain adaptation for education
- Terminology-aware translation

**2. Language Model Training**
- Irish language model improvement
- Bilingual model training
- Fine-tuning existing models (UCCIX, gaBERT)

**3. Educational Applications**
- Language learning tools
- Bilingual dictionary creation
- Curriculum alignment tools

**4. Research Applications**
- Linguistic analysis
- Translation studies
- Educational content analysis

**5. NLP Tool Development**
- Irish NLP pipeline training
- Named entity recognition
- Text classification

---

## 11. Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|-----------------|
| Foundation Setup | 1 week | Project structure, schemas, infrastructure |
| Website Discovery | 1-2 weeks | URL inventory, language mappings |
| Content Extraction | 2-3 weeks | Scraped content, raw database |
| Bilingual Alignment | 1-2 weeks | Aligned pairs, terminology database |
| Quality Assurance | 1 week | Validated dataset, quality reports |
| Export & Publishing | 1 week | HuggingFace dataset, documentation |
| **Total** | **6-8 weeks** | **Production-ready bilingual dataset** |

---

## 12. Technology Requirements

### 12.1 Python Packages

```txt
# Core scraping
crawl4ai>=0.3.0
playwright>=1.40.0

# Knowledge graph and data extraction
cocoindex>=0.5.0

# PDF extraction
pypdf2>=3.0.0
pdfplumber>=0.10.0

# Data processing
dlt[duckdb]>=0.4.0
pydantic>=2.5.0
pyarrow>=14.0.0

# Database
duckdb>=0.9.0
lancedb>=0.3.0
neo4j>=5.0.0  # For Cocoindex knowledge graph
psycopg2-binary>=2.9.0  # For PostgreSQL via Cocoindex

# NLP & ML
sentence-transformers>=2.2.0
langdetect>=1.0.9
openai>=1.0.0
transformers>=4.30.0  # For HuggingFace models

# Utilities
httpx>=0.25.0
tenacity>=8.2.0
python-dotenv>=1.0.0
aiofiles>=23.0.0  # For async file operations
```

### 12.2 System Requirements

- Python 3.10+
- 8GB RAM minimum (16GB recommended for LLM processing)
- 50GB storage for raw data
- Stable internet connection
- **Optional: Docker** for Neo4j and PostgreSQL
- **Optional: Ollama** for local LLM inference (cost savings)

**API Keys & Credentials:**
- OpenAI API key (for LLM-based extraction)
  - GPT-4o for high-quality extraction
  - GPT-4o-mini for cost-effective term extraction
- HuggingFace token (for dataset upload and model access)
- Neo4j credentials (if using knowledge graph features)
- PostgreSQL credentials (if using Cocoindex relational storage)

---

## 13. Next Steps

### Immediate Actions:

1. **Review & Approve This Outline**
   - Validate approach
   - Identify concerns
   - Adjust priorities

2. **Set Up Development Environment**
   - Clone repository structure
   - Install dependencies
   - Configure API keys

3. **Start with Pilot Website**
   - Begin with scoilnet.ie (successful API analysis)
   - Test full pipeline
   - Validate approach

4. **Iterate & Expand**
   - Refine based on pilot results
   - Expand to other websites
   - Scale up operations

### Questions for Clarification:

1. **Scope:** Should we include all content or focus on specific subjects/levels?
2. **Timeline:** Is 6-8 weeks feasible for your project timeline?
3. **Resources:** Do you have API keys and compute resources available?
4. **Quality vs Quantity:** Prefer smaller high-quality dataset or larger mixed-quality?
5. **Licensing:** Any specific license requirements for the final dataset?

---

## Appendix A: References

- UCCIX Project: https://github.com/ReML-AI/UCCIX
- Irish BERT: https://github.com/jbrry/Irish-BERT
- Helsinki-NLP OPUS-MT: https://github.com/Helsinki-NLP/Opus-MT
- Crawl4AI Documentation: https://crawl4ai.com/
- Crawl4AI GitHub: https://github.com/unclecode/crawl4ai
- Cocoindex Documentation: https://cocoindex.io/docs/
- DLT Documentation: https://dlthub.com/docs/
- HuggingFace Datasets: https://huggingface.co/docs/datasets/

---

## Appendix B: Glossary

**Terms:**
- **Bilingual Pair**: Corresponding English and Irish versions of the same content
- **Alignment Confidence**: Score (0-1) indicating certainty of bilingual matching
- **Content Segment**: Paragraph or sentence-level text unit
- **Terminology Pair**: Subject-specific term and its translation
- **Quality Score**: Overall content quality metric (0-1)

---

**Document End**

*For questions or clarifications, please raise them before implementation begins.*
