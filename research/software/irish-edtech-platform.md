# Irish EdTech Platform: Consolidated Architecture Document

> **Consolidated from**: Celtic Education Policy Data, Platform Architecture, Data Architecture, Frontend Stack, AI/ML Pipeline, Subject Implementations, and BAML Schema specifications.

---

## Part 1: Pan-Celtic Education Policy Context

### 1.1 Demographic Overview

The mid-2020s educational landscape faces a profound demographic contraction intersecting with volatile fiscal conditions across the British Isles.

| Jurisdiction | Projected Child Pop. Decline (2025-2035) | Celtic Language Enrollment | Growth Trend |
|--------------|------------------------------------------|---------------------------|--------------|
| Northern Ireland | -15% | 7,414 pupils (IME) | Fast growth (+50%/decade) |
| Wales | -10% | 93,377 (21% of total) | Stable/static |
| Scotland | -8% | 5,066 GME pupils | Growing |
| England | -6% | N/A | N/A |
| Republic of Ireland | Variable | 66,318 (8% primary) | Stable/restricted |
| Isle of Man | Stable | ~69 primary | Stable |

**Key Insight**: Falling pupil numbers offer theoretical opportunity for increased per-pupil spending, but historical precedent shows resource consolidation instead.

### 1.2 Jurisdiction-Specific Analysis

#### Wales: Cymraeg 2050 Strategy

| Metric | Current State | 2031 Target | Gap |
|--------|---------------|-------------|-----|
| Welsh-medium schools | 405 | - | - |
| Welsh-medium enrollment | 21% | - | - |
| Primary teachers (Welsh) | 2,792 | 3,900 | +1,108 |
| Secondary teachers (Welsh) | 2,029 | 3,200 | +1,171 |
| ITE Secondary recruitment | 62% filled | 100% | -38% |
| Welsh subject recruitment | 15% of target | 100% | -85% |

**Critical Constraints**:
- Workforce-defined cap on expansion
- Geographic variance (Gwynedd/Anglesey normalized vs. 17 LAs English-dominant)
- "Bilingual advantage" empirically validated in attainment data

#### Scotland: Gaelic Medium Education

| Metric | Value | Notes |
|--------|-------|-------|
| GME Primary pupils | 3,781 | 9.8 per 1,000 nationally |
| GME Secondary pupils | 1,636 | 87% in 3 councils |
| Secondary immersion depth | 19% Gaelic-only | Subject drop-off issue |
| Population with Gaelic skills | 2.5% (130,161) | Slight increase via education |

**Key Pattern**: P7 GME pupils outperform national average in English literacy (+6 pp) and numeracy (+5 pp).

**Crisis Point**: Vernacular collapse in Western Isles despite 43% GME participation.

#### Northern Ireland: Irish Medium Education

| Metric | Value | Notes |
|--------|-------|-------|
| Total IME enrollment | 7,414 | +50% over decade |
| Schools | 30 standalone + 10 units | Fastest-growing sector |
| Nursery pipeline | 46 nurseries | Robust P1 feed |
| Temporary accommodation | 16 of 21 new schools | Infrastructure crisis |
| SEN prevalence | 32% vs 21.1% average | Under-resourced |
| Teacher workload | Higher than English-medium | Resource translation burden |

**Legislative Context**: Identity and Language (NI) Act 2022 placed statutory duty on DE to encourage/facilitate IME.

#### Republic of Ireland: Dual Context

| Context | Schools | Enrollment | Key Challenge |
|---------|---------|------------|---------------|
| Gaelscoileanna (outside Gaeltacht) | 153 primary | 48,684 primary | 13 counties with no secondary |
| Gaeltacht schools | 103 primary | - | Sociolinguistic collapse |
| Post-primary IME | 3.8% of students | 17,634 | Geographic deserts |

**Teacher Crisis**: 43% of Gaelscoileanna have long-term vacancies vs. 10% English-medium.

**Census 2022**: 2% drop in daily Irish speakers in Gaeltacht; only 60% youth usage in "strong" areas.

#### Isle of Man: Micro-Model Success

| Element | Status |
|---------|--------|
| Bunscoill Ghaelgagh | Fully maintained government school since 2020 |
| Enrollment | ~60-70 pupils |
| Total speakers produced | ~170 fluent (language declared extinct 2009) |
| Strategy target | 5,000 speakers by 2032 (double current) |

**Unique Features**: Island-wide enrollment (no catchment), transition to English-medium secondary with Manx as subject.

### 1.3 Universal Challenges

| Challenge | Wales | Scotland | N. Ireland | R. Ireland | Isle of Man |
|-----------|-------|----------|------------|------------|-------------|
| Teacher pipeline | Critical | Severe | Critical | Severe | Moderate |
| Infrastructure | Adequate | Adequate | Crisis | Restricted | Adequate |
| Secondary immersion | Diluted | Diluted | Developing | Diluted | N/A |
| Digital resources | Developing | Gaps | Severe gap | Gaps | Limited |
| Community vernacular | Strong (heartlands) | Perilous | N/A | Collapsing | Revitalizing |

### 1.4 Budget Allocations (2024/25)

| Jurisdiction | Allocation | Change |
|--------------|------------|--------|
| Wales (Education) | GBP 3.59bn | +7.4% |
| Isle of Man (DESC) | GBP 141m | +GBP 18m |
| Scotland (Gaelic Grant) | GBP 4.55m | +GBP 68k |
| N. Ireland (Education) | GBP 2.8bn | Real-terms cut |
| R. Ireland (Dictionary/Publishing) | EUR 1.5m | New investment |

---

## Part 2: Platform Architecture

### 2.1 System Overview

The Irish education system presents a **tripartite data landscape**:

```
┌─────────────────────────────────────────────────────────────────┐
│                    GOVERNANCE STRUCTURE                          │
├─────────────────────────────────────────────────────────────────┤
│  NCCA (curriculumonline.ie)    │ Pedagogical Intent             │
│  - Specifications              │ - Learning Outcomes            │
│  - Features of Quality         │ - Transverse Strands           │
├────────────────────────────────┼────────────────────────────────┤
│  SEC (examinations.ie)         │ Evidentiary Truth              │
│  - Exam Papers                 │ - Marking Schemes              │
│  - Chief Examiner Reports      │ - Conditional Logic            │
├────────────────────────────────┼────────────────────────────────┤
│  Dept of Education (gov.ie)    │ Temporal Governance            │
│  - Circular Letters            │ - SUPERSEDES relationships     │
│  - Policy Amendments           │ - Valid time tracking          │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Core Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| Document Ingestion | ColPali, DeepSeek-OCR, Granite-Docling | Multi-modal extraction |
| Knowledge Base | FalkorDB + Qdrant | Hybrid vector/graph storage |
| Temporal Reasoning | Graphiti | Bi-temporal data model |
| ETL Orchestration | CocoIndex | High-velocity pipelines |
| Structured Extraction | BAML | Type-safe LLM outputs |
| RAG Retrieval | BGE-M3 + ColPali | Dense + sparse + visual |
| Generation | Qwen2.5-Math-7B (fine-tuned) | Bilingual math reasoning |
| Frontend | TanStack Start + Cloudflare | Edge-native rendering |
| Interactive Compute | Marimo WASM | Browser-based Python |
| Heavy Compute | Self-hosted Coder | Container workspaces |

### 2.3 Curriculum Hierarchy Model

```
Subject (e.g., Mathematics)
├── Cycle (Junior/Senior)
│   ├── Strand (e.g., Algebra, Number)
│   │   ├── Topic (e.g., Equations)
│   │   │   └── Learning Outcome (atomic unit)
│   │   └── Assessment Items
│   │       ├── Exam Questions
│   │       └── Marking Schemes (Scales 10A-D)
│   └── Unifying Strands (transverse links)
└── Competency Links (Key Competencies)
```

### 2.4 Assessment Models by Subject Group

| Subject Group | Ontology Model | Key Edge Types | Assessment Logic | Data Modality |
|--------------|----------------|----------------|------------------|---------------|
| Mathematics | Derivation Tree | :PREREQUISITE, :ASSESSES | Step-based (Scale 10C) | Text + Symbolic |
| Sciences | Taxonomy & System | :FLOWS_TO, :INTERACTS | Keyword/Hit-Count | Text + Diagram |
| Humanities | Causal & Spatial | :CAUSED, :LOCATED_AT | SRP Count / Argument | Text + Map + Image |
| Languages | Thematic Web | :EXPLORES, :TRANSLATES | Rubric (PCLM) | Text + Audio |
| Business | Transaction Graph | :DEBITS, :CREDITS | Exact Layout / Values | Text + Table |

---

## Part 3: Data Architecture

### 3.1 Core Ontology

```turtle
@prefix edu: <http://www.irish-edtech.ie/ontology#>.

# Root Entity
edu:EducationalNode a owl:Class.

# Entity Types
edu:CurriculumSpecification rdfs:subClassOf edu:EducationalNode.
edu:PedagogicalUnit rdfs:subClassOf edu:EducationalNode.
edu:LearningOutcome rdfs:subClassOf edu:EducationalNode.
edu:AssessmentInstrument rdfs:subClassOf edu:EducationalNode.
edu:EvidenceLogic rdfs:subClassOf edu:EducationalNode.
edu:PolicyDirective rdfs:subClassOf edu:EducationalNode.

# Key Properties
edu:validForLevel a owl:ObjectProperty ;
    rdfs:domain edu:LearningOutcome ;
    rdfs:range edu:Level.
edu:includesOutcome a owl:TransitiveProperty.
```

### 3.2 Edge Schema

| Edge Type | Semantics | Temporal | Example |
|-----------|-----------|----------|---------|
| `ASSESSES` | Question -> LearningOutcome | No | Weighted by similarity |
| `DEFINES_QUALITY` | Rubric -> PedagogicalUnit | No | CBA descriptors |
| `SUPERSEDES` | Circular -> Circular | Yes | Policy versioning |
| `PREREQUISITE` | Topic -> Topic | No | Concept dependencies |
| `EVIDENCES_DIFFICULTY` | ExaminerComment -> LO | Yes | Difficulty flagging |
| `REQUIRES_MATH_CONCEPT` | Physics Topic -> Math Topic | No | Cross-subject links |
| `HAS_FORM` | Word -> Form | No | Dialectal variations |

### 3.3 Bi-Temporal Data Model (Graphiti)

Every edge tracks two time dimensions:

```cypher
// Syllabus versioning
(:Topic {name: "Matrices"}) -[:PART_OF {
  valid_at: "1990-01-01",
  invalid_at: "2015-01-01",
  created_at: "2024-01-15"
}]-> (:Curriculum {name: "Leaving Cert"})

// Student mastery with decay
(:Student) -[:HAS_MASTERY {
  valid_at: "2024-03-15",
  confidence: 0.85
}]-> (:Topic {name: "Complex Numbers"})
```

**Query Logic**: Filter edges where `now()` falls within validity window. Use "Time Travel" for historical queries.

### 3.4 FalkorDB Schema

```cypher
// Vector index for similarity search
CALL db.idx.vector.createNodeIndex('Question', 'embedding', 'FLOAT32', 6, 'L2')

// Full-text index for keyword search
CALL db.idx.fulltext.createNodeIndex('Question', 'text')

// Constraint for data integrity
GRAPH.CONSTRAINT CREATE MathsGraph ON (q:Question) ASSERT q.id IS UNIQUE

// Hybrid GraphRAG Query
CALL db.idx.vector.queryNodes('Question', 'embedding', $vec, 5)
YIELD node AS similar_question
MATCH (similar_question)-[:ASSESSES]->(topic:Topic)
RETURN similar_question.text, topic.definition
```

### 3.5 Bilingual Data Strategy

**Unified Concept Node**:
```json
{
  "concept_id": "PYTHAG_THEOREM",
  "name_en": "Theorem of Pythagoras",
  "name_ga": "Teoirim Pythagoras",
  "definition_en": "The square of the hypotenuse...",
  "definition_ga": "An chearnóg ar an taobhagán..."
}
```

**Dialect Handling**:
```cypher
(:Word {lemma: "Look"}) -[:HAS_FORM]-> (:Form {text: "Féach", dialect: "Standard"})
(:Word {lemma: "Look"}) -[:HAS_FORM]-> (:Form {text: "Amharc", dialect: "Ulster"})
```

---

## Part 4: Frontend Architecture

### 4.1 Edge-Native Philosophy

| Layer | Traditional | Proposed |
|-------|-------------|----------|
| Frontend | Vue.js / React (Node.js) | TanStack Start (Edge-rendered) |
| Compute | Bare Metal / VMs | Cloudflare Workers |
| State | Redis / MongoDB | Durable Objects |
| Runtime (Light) | MicroVMs per user | Marimo WebAssembly |
| Runtime (Heavy) | MicroVMs | Self-Hosted Coder |
| Transport | WebSocket/SSH tunnels | Durable Objects WebSockets |

### 4.2 Isomorphic Rendering Flow

```
Student Request (Rural Kerry)
    ↓
Cloudflare Edge (Dublin PoP)
    ↓
Stream HTML immediately (text, definitions, syllabus)
    ↓
Student starts reading
    ↓
JavaScript hydrates (WebGL simulations load)
    ↓
Full interactivity available
```

### 4.3 Bilingual Routing

```
/en/calculus/derivatives
/ga/calcalas/díorthaigh
```

- Middleware: Cloudflare Worker inspects `Accept-Language` header
- Streaming: Load only current language segment
- Terminology: KV store for glossary (`"Integer" -> "Slánuimhir"`)

### 4.4 Visualization Stack

| Subject Area | Technology | Use Case |
|--------------|------------|----------|
| Mathematics | MathBox.js, Mafs | Interactive graphs, complex number visualizations |
| Geography | DuckDB WASM + Deck.gl | Census SQL queries, choropleth maps |
| Chemistry | 3Dmol.js, R3F | Orbital clouds, virtual titrations |
| English | D3.js, Compromise.js | Character networks, sentiment analysis |
| History | Timeline.js | Bi-temporal event visualization |

### 4.5 Cost Profile

| Component | Monthly Cost |
|-----------|-------------|
| Cloudflare Workers/Pages | $5-20 |
| Durable Objects | Negligible |
| Self-hosted Coder | $10-20 |
| **Total** | ~$50/month |

---

## Part 5: AI/ML Pipeline

### 5.1 Document Processing

```
┌─────────────────────────────────────────────────────────────────┐
│                 DOCUMENT INGESTION (CocoIndex)                  │
│  PDF Sources -> Language Detection -> Content Routing           │
│  ├── Text/Equations -> DeepSeek-OCR -> LaTeX extraction        │
│  ├── Diagrams -> ColPali -> Visual embeddings                  │
│  └── Tables -> Granite-Docling -> Structured extraction        │
│  ↓                                                              │
│  BAML Structured Extraction -> Metadata + JSON                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Model Selection

| Tool | LaTeX | Diagrams | Tables | Irish | Size |
|------|-------|----------|--------|-------|------|
| DeepSeek-OCR | 95% | Good | Very Good | Unconfirmed | 3B |
| Qwen2.5-VL | Very Good | Excellent | Excellent | Likely | 2B-235B |
| Granite-Docling | Good | Good | Excellent | Experimental | 258M |
| ColPali | N/A (retrieval) | Excellent | Good | Visual-based | 3B |

**Fine-tuning Target**: Qwen2.5-Math-7B-Instruct
- 85.3% on MATH benchmark
- Solves 21/30 AIME problems
- Native multilingual support

### 5.3 Irish Language Integration

**Challenge**: Irish represents <0.1% of web content with ~20% performance gap vs. English.

**Solution Stack**:
1. **UCCIX-Llama2-13B-Instruct**: +12% over LLaMA 2-70B on Irish tasks
2. **GaBERT**: Irish-specific BERT embeddings (+3.7 LAS on dependency parsing)
3. **Qwen2.5-Math**: Native Irish support as base model
4. **Irish-BLiMP**: 1,020 minimal pairs for grammaticality validation

### 5.4 RAG Architecture

```
Query -> Language Detection
    ↓
┌──────────────────────────────────────┐
│ BGE-M3 Dense + Sparse Embeddings     │
│ ColPali Visual Page Embeddings       │
│ Payload Filtering (year, topic, lang)│
└──────────────────────────────────────┘
    ↓
Reranking -> Top-K Results
    ↓
Context Assembly for LLM
```

### 5.5 Training Data Format

```json
{
  "conversations": [
    {
      "role": "user",
      "content": "Leaving Certificate Higher Level, Paper 1:\nDifferentiate f(x) = (3x^2+2)/(x-1) and find stationary points. (25 marks)"
    },
    {
      "role": "assistant",
      "content": "<think>Apply quotient rule, find where f'(x)=0...</think>\n\n**Step 1: Apply Quotient Rule** (5 marks)\n$$f'(x) = \\frac{6x(x-1) - (3x^2+2)(1)}{(x-1)^2}$$\n...\nFinal Answer: \\boxed{\\left(1 \\pm \\frac{\\sqrt{15}}{3}, y\\right)}"
    }
  ]
}
```

**Dataset Mix**: 60-70% LC problems + 20-30% general math (prevents catastrophic forgetting)

### 5.6 Unsloth Hyperparameters

| Parameter | Math Reasoning | Standard |
|-----------|----------------|----------|
| LoRA rank | 64-128 | 16-32 |
| Learning rate | 1e-5 to 5e-5 | 1e-4 to 5e-4 |
| Sequence length | 4096+ tokens | 2048 |
| VRAM requirement | ~6-7GB (QLoRA 4-bit) | - |

---

## Part 6: Subject Implementations

### 6.1 Mathematics

**Graph Structure**:
```cypher
(:Topic {name: "Quadratic Equations"})
  -[:PREREQUISITE]->
(:Topic {name: "Factoring"})
  -[:PREREQUISITE]->
(:Topic {name: "Operations on Integers"})
```

**Marking Logic** (Scale 10C):
- 10 marks: Correct answer with full work
- 9-8 marks: Minor slip, correct method
- 7-5 marks: Partial solution
- 4-0 marks: Incorrect approach

### 6.2 Experimental Sciences

**Physics Cross-Graph Dependencies**:
```cypher
(:Topic {name: "Linear Motion", subject: "Physics"})
  -[:REQUIRES_MATH_CONCEPT]->
(:Topic {name: "Slope", subject: "Maths"})
```

**Biology Taxonomy**:
```cypher
(:Organelle {name: "Mitochondria"}) -[:PART_OF]-> (:Process {name: "Respiration"})
(:Process {name: "Respiration"}) -[:PRODUCES]-> (:Molecule {name: "ATP"})
```

**Chemistry SMILES Integration**:
```cypher
(:Family {name: "Alcohols"}) -[:CONTAINS]-> (:Molecule {smiles: "CCO", name: "Ethanol"})
(:Molecule {name: "Ethanol"}) -[:UNDERGOES]-> (:Reaction {name: "Oxidation"})
```

### 6.3 Humanities

**History Bi-Temporal Model**:
```cypher
(:Event {name: "Easter Rising", real_world_timestamp: "1916-04-24"})
  <-[:PERSPECTIVE_ON]- (:Perspective {name: "Pro-Treaty"})
  <-[:PERSPECTIVE_ON]- (:Perspective {name: "Anti-Treaty"})
```

**Geography SRP Logic**:
- Marking: 2 marks per Significant Relevant Point
- Grading: Semantic Hit Count (sentence similarity to valid SRPs)

### 6.4 Languages

**Irish Audio Pipeline**:
```
Student Audio Recording
    ↓
Whisper (Irish dialect fine-tuned: Connacht, Munster, Ulster)
    ↓
Transcription Analysis:
  - Fluency (pauses, speech rate)
  - Vocabulary (Saibhreas) against NodeSet
  - Grammar (Tuiseal Ginideach)
    ↓
Timestamped Error Feedback
```

**English PCLM Grading**:
| Component | Weight | Analysis Method |
|-----------|--------|-----------------|
| Purpose | 30% | Vector similarity (Essay <-> Question) |
| Coherence | 30% | Discourse analysis |
| Language | 30% | Lexical diversity score |
| Mechanics | 10% | Spelling/grammar check |

### 6.5 Business

**Accounting Double-Entry Graph**:
```cypher
(:Account {name: "Bank"}) -[:DEBIT {amount: 100}]-> (:Transaction {id: "TX001"})
(:Account {name: "Sales"}) -[:CREDIT {amount: 100}]-> (:Transaction {id: "TX001"})
```

**Validation**: If Sum(Debits) != Sum(Credits), traverse graph to find error origin.

### 6.6 Subject-Specific Tech Stack

| Subject | BAML Focus | Visualization | Special Requirements |
|---------|-----------|---------------|---------------------|
| Mathematics | LaTeX, Formulas | MathBox.js, Mafs | Step-based grading |
| Physics | Units, Dimensions | R3F simulations | Math cross-references |
| Chemistry | SMILES, Reactions | 3Dmol.js, MolStar | Equation balancing |
| Biology | Taxonomies, Diagrams | D3.js hierarchy | Diagram segmentation |
| History | Timelines, Causation | Timeline.js | Bi-temporal queries |
| Geography | SRPs, Maps | Deck.gl, MapLibre | Geospatial indexing |
| English | PCLM rubrics | D3.js networks | Sentiment analysis |
| Irish | Dialects, Audio | Waveform viz | Whisper fine-tuning |
| Accounting | Tables, Ledgers | React Tables | Double-entry validation |

---

## Part 7: BAML Schema Specifications

### 7.1 Primary Curriculum

```baml
enum PrimaryStage {
  Stage1_JuniorSeniorInfants
  Stage2_FirstSecondClass
  Stage3_ThirdFourthClass
  Stage4_FifthSixthClass
}

class CompetencyLink {
  competency_name: string @description("e.g., 'Being a Digital Learner'")
  context: string @description("How this outcome supports the competency")
}

class PrimaryLearningOutcome {
  id: string?
  text: string
  element: string @description("e.g., 'Communicating', 'Understanding'")
  progression_continuum: string?
  key_competencies: CompetencyLink[]
}

class PrimaryStrand {
  name: string @description("e.g., 'Number', 'Data and Chance'")
  description: string
  outcomes: PrimaryLearningOutcome[]
}

class PrimaryCurriculumArea {
  name: string @description("e.g., 'Mathematics', 'Language'")
  rationale: string
  strands: PrimaryStrand[]
  integration_links: string[] @description("Links to other areas")
}
```

### 7.2 Junior Cycle Science (Transverse Links)

```baml
class ScienceOutcome {
  id: string @description("e.g., 'CW4', 'NoS1'")
  strand_type: "Contextual" | "Unifying"
  strand_name: string
  text: string
  action_verb: string @description("Bloom's taxonomy verb")
  keywords: string[]
}

class TransverseLink {
  source_outcome_id: string
  target_nos_id: string @description("Nature of Science outcome ID")
  strength: "High" | "Medium" | "Low"
}

class JuniorCycleScienceSpec {
  unifying_strand: ScienceOutcome[]
  contextual_strands: ScienceOutcome[]
  inferred_links: TransverseLink[]
}
```

### 7.3 Senior Cycle Marking Schemes

```baml
class PenaltyRule {
  type: string @description("'Arithmetic Slip', 'Chemical Error'")
  deduction: float
  scope: string @description("'per occurrence', 'max -3'")
}

class MarkingPoint {
  correct_answer: string
  marks_awarded: int
  valid_alternatives: string[]
  mandatory_keywords: string[]
  examiner_notes: string?
}

class QuestionPartSchema {
  part_id: string @description("e.g., '(b)(ii)'")
  total_marks: int
  marking_points: MarkingPoint[]
  penalties: PenaltyRule[]
}

function ExtractMarkingScheme(text: string) -> QuestionPartSchema[] {
  client "anthropic/claude-sonnet-4-20250514"
  prompt #"
    Analyze the Marking Scheme segment.
    Extract the logic for awarding marks.

    CRITICAL: Identify 'Penalties' and 'Deductions'.
    Distinguish between a 'Slip' (minor error) and fundamental error.
    Look for lists of values separated by '/' which indicate alternatives.

    Text:
    {{ text }}
    {{ ctx.output_format }}
  "#
}
```

### 7.4 Qualitative Rubrics (Arts & Humanities)

```baml
enum AchievementLevel {
  Exceptional
  AboveExpectations
  InLineWithExpectations
  YetToMeetExpectations
}

class RubricDescriptor {
  level: AchievementLevel
  text: string @description("Full descriptive paragraph")
  key_qualities: string[] @description("'comprehensive analysis'")
  negative_indicators: string[] @description("'limited understanding'")
}

class AssessmentTask {
  name: string @description("e.g., 'CBA 1: The Past in My Place'")
  timing: string
  rubrics: RubricDescriptor[]
}
```

### 7.5 LCA/LCVP Vocational Pathways

```baml
class PortfolioItem {
  title: string @description("e.g., 'Curriculum Vitae', 'Career Investigation'")
  core_items: boolean @description("True if mandatory")
  optional_items: boolean @description("True if chosen from list")
  assessment_criteria: string[]
}

class LCVPModule {
  name: string
  learning_outcomes: LearningOutcome[]
  portfolio_requirements: PortfolioItem[]
}

class KeyAssignment {
  module: string @description("e.g., 'Social Education'")
  task_description: string
  evidence_required: string @description("'Logbook', 'Interview', 'Artifact'")
  credits_value: int
}
```

### 7.6 Policy Circulars

```baml
enum CircularStatus {
  NewPolicy
  Amendment
  Repeal
  Clarification
}

class LinkedCircular {
  id: string
  relationship: "Supersedes" | "Refers to" | "Amends"
}

class CircularMetadata {
  circular_id: string @description("e.g., '0003/2018'")
  title: string
  issue_date: string
  effective_date: string
  status: CircularStatus
  linked_circulars: LinkedCircular[]
  domains_affected: string[] @description("'Leadership', 'Special Needs', 'Curriculum'")
}

function ExtractCircularMeta(text: string) -> CircularMetadata {
  client "anthropic/claude-sonnet-4-20250514"
  prompt #"
    Analyze the Circular Letter.

    1. Extract the ID and Dates.
    2. CRITICAL: Find the 'Supersedes' or 'Rescinds' text.
       Extract the ID of the *old* circular being replaced.
    3. Identify the Domain. Is this about Staffing? Assessment?

    Text:
    {{ text }}
    {{ ctx.output_format }}
  "#
}
```

### 7.7 Universal Polymorphic Schema

```baml
enum SubjectType {
  Math
  Science
  Language
  Humanities
  Business
}

class ImageAsset {
  url: string
  description: string @description("Alt-text from Vision Model")
  type: "Map" | "Diagram" | "Photo" | "Chart"
}

class AssessmentItem {
  id: string
  year: int
  level: "Higher" | "Ordinary"
  subject: SubjectType
  strand_ref: string
  topic_tags: string[]

  // Polymorphic Content
  text_content: string?
  image_assets: ImageAsset[]?
  audio_assets: AudioAsset[]?
  table_data: TableData?

  marking_scheme_ref: string
}
```

---

## Part 8: Implementation Guide

### 8.1 CocoIndex Flow Strategy

| Flow | Source | Frequency | BAML Strategy | Graphiti Action |
|------|--------|-----------|---------------|-----------------|
| CurriculumFlow | curriculumonline.ie | Annual | ExtractPrimaryFramework | Upsert (Stable) |
| EvidenceFlow | examinations.ie | Annual bursts | ExtractMarkingScheme | Append Episodes |
| PolicyFlow | gov.ie | Weekly | ExtractCircularMeta | Temporal Patching |

### 8.2 Deployment Options

| Option | GPU | VRAM | Use Case | Cost |
|--------|-----|------|----------|------|
| Modal (T4) | NVIDIA T4 | 16GB | Development | $0.59/hr |
| Modal (A10) | NVIDIA A10 | 24GB | Production | $1.10/hr |
| RTX 4090 | Consumer | 24GB | Self-hosted | ~$1,800 one-time |
| RTX 3090 | Consumer | 24GB | Budget self-hosted | ~$1,500 used |

### 8.3 Implementation Phases

**Phase 1 (Weeks 1-4): Core Subjects**
- Mathematics pilot
- English (high volume)
- Irish (bilingual complexity)

**Phase 2 (Weeks 5-10): STEM Expansion**
- Physics (Math dependencies)
- Chemistry (symbolic notation)
- Biology (visual content)

**Phase 3 (Weeks 11-16): Humanities**
- History (temporal reasoning)
- Geography (geospatial)

**Phase 4 (Weeks 17-24): Completion**
- Business/Accounting
- Modern Languages
- Applied subjects
- WCAG 2.1 AA audit

### 8.4 Decision Framework

| Decision Point | Recommendation | Rationale |
|----------------|----------------|-----------|
| Base model | Qwen2.5-Math-7B | Native Irish, math-optimized |
| Fine-tuning | Unsloth + LoRA | 70% VRAM reduction |
| Vector DB | Qdrant | Multi-vector ColPali support |
| Graph DB | FalkorDB | Vector + full-text + Cypher |
| Frontend | TanStack Start | Type-safe, edge-rendered |
| WASM Compute | Marimo | Zero-cost browser Python |
| Heavy Compute | Coder | Only ~20% of syllabus needs |

---

## Appendix: Quick Reference Tables

### A.1 Celtic Language Education Stats (2024-25)

| Jurisdiction | Language | Primary | Secondary | % Total | Growth |
|--------------|----------|---------|-----------|---------|--------|
| Wales | Welsh | 93,377 | (included) | 21% | Stable |
| Scotland | Gaelic | 3,781 | 1,636 | ~1.7% | Growing |
| N. Ireland | Irish | ~5,113 | ~2,300 | 2.1% | Fast |
| R. Ireland | Irish | 48,684 | 17,634 | 8% (pri) | Stable |
| Isle of Man | Manx | ~69 | N/A | <1% | Stable |

### A.2 Teacher Supply Status

| Jurisdiction | Sector | Status | Key Metric |
|--------------|--------|--------|------------|
| Wales | Secondary | Critical | 15% target met (Welsh) |
| N. Ireland | Post-Primary | Critical | 50% specialist posts unfilled |
| R. Ireland | Primary (Gaelscoil) | Severe | 43% long-term vacancies |
| Scotland | Secondary (GME) | Moderate/Severe | e-Sgoil distance reliance |

### A.3 Infrastructure Costs (Monthly)

| Component | MVP | Production |
|-----------|-----|------------|
| Cloudflare | $5-20 | $50-100 |
| Modal compute | $100-200 | $500-1000 |
| Qdrant Cloud | $25 | $100 |
| Storage (R2/S3) | $10-20 | $50 |
| API calls (BAML) | $50-100 | $200-500 |
| **Total** | ~$200-350 | ~$900-1750 |

---

*Document generated from consolidated research files. Last updated: December 2025.*
