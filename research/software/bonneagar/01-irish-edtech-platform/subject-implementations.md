# Subject-Specific Implementations

## Executive Summary

This document provides technical blueprints for implementing AI tutoring across all Leaving Certificate subjects, extending the core mathematics architecture to handle the diverse assessment models, data modalities, and pedagogical requirements of 30+ subjects.

---

## 1. Assessment Logic by Subject Group

| Subject Group | Ontology Model | Key Edge Types | Assessment Logic | Data Modality |
|--------------|----------------|----------------|------------------|---------------|
| **Mathematics** | Derivation Tree | :PREREQUISITE | Step-based (Scale 10C) | Text + Symbolic |
| **Sciences** | Taxonomy & System | :FLOWS_TO, :INTERACTS | Keyword/Hit-Count | Text + Diagram |
| **Humanities** | Causal & Spatial | :CAUSED, :LOCATED_AT | SRP Count / Argument | Text + Map + Image |
| **Languages** | Thematic Web | :EXPLORES, :TRANSLATES | Rubric (PCLM) | Text + Audio |
| **Business** | Transaction Graph | :DEBITS, :CREDITS | Exact Layout / Values | Text + Table |

---

## 2. Experimental Sciences

### 2.1 Physics: Mathematical-Empirical Bridge

**Cross-Graph Dependencies**:
- Physics concepts require Math prerequisites
- Example: "Velocity-Time Graphs" `:REQUIRES_MATH_CONCEPT` → "The Line"

```cypher
(:Topic {name: "Linear Motion", subject: "Physics"})
  -[:REQUIRES_MATH_CONCEPT]->
(:Topic {name: "Slope", subject: "Maths"})
```

**Diagnostic Logic**: When student fails Physics question, traverse edge to check Math mastery. Diagnose as Math error if prerequisite mastery is low.

**BAML Extension for Dimensional Analysis**:
```baml
class PhysicsValue {
  magnitude: float
  unit: string @description("SI Unit, derived from context")
  dimension: string @description("e.g., Length, Time, Force")
}

class PhysicsQuestion {
  given_values: PhysicsValue[]
  required_value_dimension: string
}
```

Enables "Dimensional Consistency Checks" on AI-generated answers.

### 2.2 Biology: Taxonomical and Systemic Graph

**New Edge Types**:
```cypher
(:Organelle {name: "Mitochondria"}) -[:PART_OF]-> (:Process {name: "Respiration"})
(:Process {name: "Respiration"}) -[:PRODUCES]-> (:Molecule {name: "ATP"})
```

**Query Logic**: "Trace the pathway" queries vs "Find similar" queries.

**Visual Data Ingestion**:
1. Diagram Segmentation: Identify line art regions
2. Multimodal Labeling: Generate textual descriptions
3. Embedding: Store descriptions alongside question text

### 2.3 Chemistry: Syntax of Matter

**Chemical Markup**:
- SMILES strings for organic molecules (e.g., Benzene: `C1=CC=CC=C1`)
- Enables substructure searching

**Family Structure**:
```cypher
(:Family {name: "Alcohols"}) -[:CONTAINS]-> (:Molecule {name: "Ethanol"})
(:Molecule {name: "Ethanol"}) -[:UNDERGOES]-> (:Reaction {name: "Oxidation"})
```

### 2.4 Agricultural Science: Applied Integration

**Project Support (25% of grade)**:
- Graphiti tracks project state over months
- Episodes: Draft introduction → Data results → Conclusion
- "Time Travel" allows critiquing conclusion based on earlier data

---

## 3. Humanities

### 3.1 History: Bi-Temporal Causal Graph

**Double Timeline**:
1. **Historical Time**: When event occurred (1916)
2. **Curriculum Time**: When topic added to syllabus

```cypher
(:Event {name: "Easter Rising", real_world_timestamp: "1916-04-24"})
```

**Causal Edge Types**:
- `:CONTRIBUTED_TO`
- `:TRIGGERED`
- `:LONG_TERM_CAUSE`

**Multiperspectivity**:
```cypher
(:Event {name: "Anglo-Irish Treaty"})
  <-[:PERSPECTIVE_ON]- (:Perspective {name: "Pro-Treaty"})
  <-[:PERSPECTIVE_ON]- (:Perspective {name: "Anti-Treaty"})
```

**Essay Evaluation**: Check if student references both perspectives.

**Research Study Report (RSR)**:
- Ingest primary source documents
- "Source Evaluation Engine" detects reliability and bias
- Specialized vector index for historiographical terms

### 3.2 Geography: Geospatial Knowledge Graph

**FalkorDB Geospatial Indexing**:
```cypher
(:CaseStudy {name: "Greater Dublin Area", lat: 53.3498, lng: -6.2603})
```

**Spatial Queries**: "Compare peripheral region (West) with core region (East)"

**SRP (Significant Relevant Point) Logic**:
- Marking: 2 marks per SRP
- BAML extracts SRPs from marking schemes
- Grading: Semantic Hit Count (sentence similarity to valid SRPs)

**OS Map Analysis**:
1. CV model identifies features (Post Offices, Contours)
2. Pixel coordinates → Grid coordinates mapping
3. Grid reference validation

### 3.3 Classical Studies

Uses History patterns with:
- Ancient timeline (BCE dates)
- Literary analysis edges from English

---

## 4. Languages

### 4.1 Gaeilge (Irish)

**Audio Processing Pipeline**:
```
Student Audio Recording
    ↓
Whisper (Irish dialect fine-tuned: Connacht, Munster, Ulster)
    ↓
Transcription Analysis:
  - Fluency (pauses, speech rate)
  - Vocabulary (Saibhreas) against "Rich Vocabulary" NodeSet
  - Grammar (Tuiseal Ginideach)
    ↓
Timestamped Error Feedback
```

**Dialectal Modeling**:
```cypher
(:Word {lemma: "Look"}) -[:HAS_FORM]-> (:Form {text: "Féach", dialect: "Standard"})
(:Word {lemma: "Look"}) -[:HAS_FORM]-> (:Form {text: "Amharc", dialect: "Ulster"})
```

Prevents "False Negative" grading for valid dialectal variations.

### 4.2 English

**PCLM Grading Architecture**:
| Component | Weight | Analysis Method |
|-----------|--------|-----------------|
| Purpose | 30% | Vector similarity (Essay ↔ Question) |
| Coherence | 30% | Discourse analysis (transitions, structure) |
| Language | 30% | Lexical diversity score |
| Mechanics | 10% | Spelling/grammar check |

**Comparative Study (Three Texts)**:
```cypher
(:Text {title: "Philadelphia, Here I Come!"}) -[:EXPLORES]-> (:Theme {name: "Isolation"})
(:Text {title: "Shawshank Redemption"}) -[:EXPLORES]-> (:Theme {name: "Hope"})
```

**Synthesis Engine**: Retrieve nodes from multiple texts, identify Contrast/Similarity edges.

### 4.3 Modern Foreign Languages (French, German, Spanish)

**Dynamic Question Generation**:
1. Scrape current news (e.g., Le Monde)
2. BAML extraction
3. Generate "Leaving Cert Style" questions from learned patterns

**Difficulty-Indexed Corpus**: Target-language texts indexed by reading level.

---

## 5. Business Group

### 5.1 Accounting: Double-Entry Graph

```cypher
(:Account {name: "Bank"})
(:Account {name: "Sales"})
// Transaction: Debit Bank, Credit Sales
(:Account {name: "Bank"}) -[:DEBIT {amount: 100}]-> (:Transaction {id: "TX001"})
(:Account {name: "Sales"}) -[:CREDIT {amount: 100}]-> (:Transaction {id: "TX001"})
```

**Balance Validation**: If Sum(Debits) != Sum(Credits), traverse graph to find error origin.

**Table Extraction**: BAML must preserve row/column structure for cell-by-cell grading.

### 5.2 Business: Structured Long Answer

**State-Explain-Example Pattern**:
```
"Delegation is assigning duties." → State (2 marks)
"This reduces manager workload." → Explain (2 marks)
"e.g., Manager asks supervisor to do roster." → Example (1 mark)
```

**Backend Logic**: Parse paragraph into three components, award marks per component.

### 5.3 Economics

Uses Accounting patterns with:
- Economic indicator tracking (temporal)
- Policy impact modeling (causal edges)

---

## 6. English/Irish Literature Data Model

### 6.1 Poet Rotation Matrix (English)

27 poets tracked across 23 years:
- **High-Frequency**: Dickinson, Yeats (recent clusters)
- **Odd-Year Cycle**: Hopkins (2021, 2019, 2017, 2013, 2011)
- **Dormant**: Larkin (last seen 2007), Montague (last seen 2007)

**Recency Score**:
$$\text{Recency Score} = \sum \frac{1}{\text{CurrentYear} - \text{ExamYear}}$$

### 6.2 Stylistic Taxonomy

| Poet | Style Keywords |
|------|---------------|
| Bishop | Analytical, Rarely Emotional, Harsh Realities |
| Dickinson | Beautiful vs Horrific, Darker Aspects, Intrigue |
| Keats | Sensuous Beauty, Melancholy |
| Yeats | Intellectual vs Emotional, Tension, Real vs Ideal |
| Rich | Power and Powerlessness, Social Concerns |

### 6.3 Irish Prose (Character-Centric)

**Hurlamboc**:
- Focus: Character "Lisín"
- Themes: Control, Self-Deception
- Tags: `caithréim` (triumph), `i gceannas` (in control)

**Cáca Milis**:
- Focus: Paul (disability), Catherine (cruelty)
- Themes: Disability, Reader Response
- Question style: Argumentative ("nach dtuilleann mórán trua")

### 6.4 Irish Poetry (A/B/C Structure)

```json
{
  "poem": "Géibheann",
  "year": 2021,
  "parts": {
    "A": "Codarsnacht i saol an ainmhí",
    "B": "Teideal oiriúnach?",
    "C": "Saol agus saothar an fhile"
  }
}
```

**Part Types**:
- A: Thematic/Descriptive
- B: Technical/Emotional (Mothúchán)
- C: Biographical

---

## 7. Universal BAML Schema

### 7.1 Polymorphic Assessment Item

```baml
enum SubjectType {
  Math
  Science
  Language
  Humanities
  Business
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

class ImageAsset {
  url: string
  description: string @description("Alt-text from Vision Model")
  type: "Map" | "Diagram" | "Photo" | "Chart"
}
```

### 7.2 Coursework Brief Extraction

```baml
class ProjectBrief {
  subject: string
  year: int
  title: string
  constraints: Constraint[]
}

class Constraint {
  type: "word_count" | "source_count" | "section_required"
  value: string
  description: string
}
```

Validate student submission against constraints before semantic grading.

---

## 8. Cross-Subject Intelligence

### 8.1 Bridge Nodes (Common Concepts)

Concepts appearing in multiple subjects:
- "Statistics" (Math, Biology, Geography)
- "Energy" (Physics, Chemistry, Biology)
- "Causation" (History, Science, Business)

```cypher
(:Physics:Energy) -[:SAME_AS]-> (:Biology:Energy)
```

**Transfer Learning**: Mastery in Physics "Energy" implies higher probability of competence in Biology "Energy".

### 8.2 Cognitive Load Balancing

Track session duration and error rates per subject:
```cypher
(:Student) -[:STUDIES {
  duration_minutes: 40,
  error_rate: 0.35,
  cognitive_load: "HIGH"
}]-> (:Math:Topic)
```

Recommend switching subjects when error rates spike.

### 8.3 Spaced Repetition Priority

When multiple topics due for review:
1. Calculate exam weightings from syllabus
2. Prioritize by percentage contribution to grade
3. Example: Calculus (15% of Math) beats Genetics (5% of Biology)

---

## 9. Cocoindex Flow Router

```python
# Subject-specific processing flows
def route_document(doc):
    subject = classify_subject(doc)

    if subject == "Math":
        return Flow_Math  # OCR → BAML (LaTeX) → FalkorDB
    elif subject in ["Irish", "English", "French"]:
        return Flow_Language  # OCR → Audio Transcribe → BAML (PCLM)
    elif subject in ["Biology", "Geography"]:
        return Flow_Visual  # OCR → Layout Analysis → Vision Model → BAML
    else:
        return Flow_Standard
```

Ensures compute-heavy resources (Vision Models) only invoked when necessary.

---

## 10. Implementation Priorities

### Phase 1: Core Subjects
1. Mathematics (pilot)
2. English (high volume)
3. Irish (bilingual complexity)

### Phase 2: STEM Expansion
4. Physics (Math dependencies)
5. Chemistry (symbolic notation)
6. Biology (visual content)

### Phase 3: Humanities
7. History (temporal reasoning)
8. Geography (geospatial)

### Phase 4: Remaining Subjects
9. Business/Accounting
10. Modern Languages
11. Applied subjects

---

## 11. Subject-Specific Tech Stack Summary

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
