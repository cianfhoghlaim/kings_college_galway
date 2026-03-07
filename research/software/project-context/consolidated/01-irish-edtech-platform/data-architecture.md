# Data Architecture for Irish Education Platform

## Executive Summary

This document consolidates the data engineering strategy for building a semantic knowledge graph tailored to the Irish education system. The architecture utilizes **BAML** for structured extraction, **CocoIndex** for high-velocity ETL pipelines, **Cognee** for ontological enforcement, **Graphiti** for temporal reasoning, and **FalkorDB** for graph persistence.

---

## 1. The Tripartite Data Landscape

### 1.1 NCCA Domain: Pedagogical Intent
- **Source**: curriculumonline.ie, ncca.ie
- **Primary Unit**: Specification documents
- **Challenge**: Non-hierarchical structures (e.g., Junior Cycle Science has "Unifying Strands" operating transversely across "Contextual Strands")
- **BAML Requirement**: Extract semantic vectors from qualitative "Features of Quality" rubrics

### 1.2 SEC Domain: Evidentiary Truth
- **Source**: examinations.ie
- **Primary Unit**: Examination Papers, Marking Schemes, Chief Examiner Reports
- **Challenge**: Extreme granularity - marking schemes contain conditional logic ("deduct 1 mark for arithmetic slip")
- **BAML Requirement**: Parse conditional statements into executable validation rules

### 1.3 Department of Education Domain: Temporal Governance
- **Source**: gov.ie, circulars.gov.ie
- **Primary Unit**: Circular Letters
- **Challenge**: Temporal validity - policies supersede previous mandates
- **Graphiti Requirement**: Model SUPERSEDES relationships as temporal edges

---

## 2. Core Ontology Design

### 2.1 Entity Metamodel

```
EducationalNode (root)
├── CurriculumSpecification
├── PedagogicalUnit (Strand, Area of Practice)
├── LearningOutcome
├── AssessmentInstrument (Exam Question, CBA)
├── EvidenceLogic (Marking criteria)
└── PolicyDirective (Circular)
```

### 2.2 Key Relationship Types

| Edge Type | Semantics | Temporal |
|-----------|-----------|----------|
| `ASSESSES` | Question → LearningOutcome (weighted by similarity) | No |
| `DEFINES_QUALITY` | Rubric → PedagogicalUnit | No |
| `SUPERSEDES` | Circular → Circular | Yes (Graphiti) |
| `PREREQUISITE` | Topic → Topic | No |
| `EVIDENCES_DIFFICULTY` | ExaminerComment → LearningOutcome | Yes |

### 2.3 RDF/OWL Ontology (Cognee)

```turtle
@prefix maths: <http://www.mathstutor.ie/ontology/curriculum#>.

# Core Classes
maths:Cycle a owl:Class ; rdfs:subClassOf maths:EducationalEntity.
maths:Strand a owl:Class ; rdfs:subClassOf maths:EducationalEntity.
maths:Topic a owl:Class ; rdfs:subClassOf maths:EducationalEntity.
maths:LearningOutcome a owl:Class ; rdfs:subClassOf maths:EducationalEntity.

# Level Stratification (Higher includes Ordinary)
maths:validForLevel a owl:ObjectProperty ;
    rdfs:domain maths:LearningOutcome ;
    rdfs:range maths:Level.
maths:includesOutcome a owl:TransitiveProperty.
```

---

## 3. BAML Schema Specifications

### 3.1 Primary Curriculum (Integrated Structure)

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
```

### 3.2 Junior Cycle Science (Transverse Links)

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
```

### 3.3 Senior Cycle Marking Schemes (Logic Gates)

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
```

### 3.4 Qualitative Rubrics (Arts & Humanities)

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
```

### 3.5 Policy Circulars (Temporal Metadata)

```baml
enum CircularStatus {
  NewPolicy
  Amendment
  Repeal
  Clarification
}

class CircularMetadata {
  circular_id: string @description("e.g., '0003/2018'")
  title: string
  issue_date: string
  effective_date: string
  status: CircularStatus
  linked_circulars: LinkedCircular[]
  domains_affected: string[]
}
```

---

## 4. Temporal Dynamics with Graphiti

### 4.1 Bi-Temporal Data Model

Every edge tracks two time dimensions:
- **Valid Time** (`valid_at`, `invalid_at`): When the fact is true in the real world
- **Transaction Time** (`created_at`, `expired_at`): When the system recorded the fact

### 4.2 Syllabus Versioning Example

```cypher
// Topic valid from 1990-2015
(:Topic {name: "Matrices"}) -[:PART_OF {
  valid_at: "1990-01-01",
  invalid_at: "2015-01-01"
}]-> (:Curriculum {name: "Leaving Cert"})
```

**Query Logic**: Filter edges where `now()` falls within validity window. Use "Time Travel" for historical queries.

### 4.3 Student Mastery Tracking

```cypher
// Dynamic mastery with decay
(:Student) -[:HAS_MASTERY {
  valid_at: "2024-03-15",
  confidence: 0.85
}]-> (:Topic {name: "Complex Numbers"})
```

**Spaced Repetition**: Analyze `valid_at` timestamps to implement forgetting curve optimization.

---

## 5. Pipeline Orchestration (CocoIndex)

### 5.1 Flow Strategy by Source Type

| Flow | Source | Frequency | BAML Strategy | Graphiti Action |
|------|--------|-----------|---------------|-----------------|
| CurriculumFlow | curriculumonline.ie | Annual | ExtractPrimaryFramework | Upsert (Stable) |
| EvidenceFlow | examinations.ie | Annual bursts | ExtractMarkingScheme | Append Episodes |
| PolicyFlow | gov.ie | Weekly | ExtractCircularMeta | Temporal Patching |

### 5.2 Custom FalkorDB Connector

```python
@cocoindex.op.target_connector(spec_cls=FalkorDBTargetSpec)
class FalkorDBConnector:
    @staticmethod
    def mutate(batch):
        client = FalkorDB(host=spec.host, port=spec.port)
        graph = client.select_graph(spec.graph_name)

        for item in batch:
            query = """
            MERGE (q:Question {id: $id})
            SET q.text = $text, q.embedding = $embedding
            """
            graph.query(query, params=item.dict())
```

### 5.3 Incremental Processing

CocoIndex's `FlowLiveUpdater` monitors source directories:
- Computes file hashes to detect changes
- Triggers flow only for changed files
- Enables near real-time updates during exam season

---

## 6. FalkorDB Schema and Indexing

### 6.1 Node Labels
- `Topic`: Abstract mathematical/curricular concepts
- `Question`: Specific assessment items
- `MarkingScheme`: Grading logic
- `Student`: User entities

### 6.2 Index Strategy

```cypher
// Vector index for similarity search
CALL db.idx.vector.createNodeIndex('Question', 'embedding', 'FLOAT32', 6, 'L2')

// Full-text index for keyword search
CALL db.idx.fulltext.createNodeIndex('Question', 'text')

// Constraint for data integrity
GRAPH.CONSTRAINT CREATE MathsGraph ON (q:Question) ASSERT q.id IS UNIQUE
```

### 6.3 Hybrid GraphRAG Query

```cypher
// Step 1: Vector similarity
CALL db.idx.vector.queryNodes('Question', 'embedding', $vec, 5)
YIELD node AS similar_question

// Step 2: Graph traversal
MATCH (similar_question)-[:ASSESSES]->(topic:Topic)

// Step 3: Aggregate context
RETURN similar_question.text, topic.definition
```

---

## 7. Cross-Subject Architecture

### 7.1 Unified Graph with Namespace Partitioning

All subjects in one graph enables interdisciplinary queries:
- `:History:Event` linked to `:Biology:Organism` (e.g., Famine → Potato Blight)
- Labels prefixed by subject for efficient filtering

### 7.2 Bridge Nodes (Curriculum Common Concepts)

Concepts appearing in multiple subjects:
- "Statistics" (Math, Biology, Geography)
- "Energy" (Physics, Chemistry, Biology)

Create `SAME_AS` edges or merge into super-nodes for transfer learning.

### 7.3 Subject-Specific Requirements

| Subject Group | Ontology Model | Key Edge Types | Assessment Logic |
|--------------|----------------|----------------|------------------|
| Mathematics | Derivation Tree | :PREREQUISITE | Step-based (Scale) |
| Sciences | Taxonomy & System | :FLOWS_TO, :INTERACTS | Keyword/Hit-Count |
| Humanities | Causal & Spatial | :CAUSED, :LOCATED_AT | SRP Count |
| Languages | Thematic Web | :EXPLORES, :TRANSLATES | Rubric (PCLM) |
| Business | Transaction Graph | :DEBITS, :CREDITS | Exact Layout |

---

## 8. Bilingual Data Strategy

### 8.1 Unified Concept Node

```json
{
  "concept_id": "PYTHAG_THEOREM",
  "name_en": "Theorem of Pythagoras",
  "name_ga": "Teoirim Pythagoras",
  "definition_en": "The square of the hypotenuse...",
  "definition_ga": "An chearnóg ar an taobhagán..."
}
```

### 8.2 Dialect Handling

```cypher
(:Word {lemma: "Look"}) -[:HAS_FORM]-> (:Form {text: "Féach", dialect: "Standard"})
(:Word {lemma: "Look"}) -[:HAS_FORM]-> (:Form {text: "Amharc", dialect: "Ulster"})
```

### 8.3 Translation Synonym Layer

```python
SYNONYM_MAP = {
    "emotion": ["mothúchán", "mothú"],
    "contrast": ["codarsnacht"],
    "life": ["saol"]
}
```

---

## 9. Implementation Roadmap

1. **Phase 1**: Define `.owl` ontology and `.baml` schemas (data contract)
2. **Phase 2**: Build CocoIndex flow for static curriculum PDFs
3. **Phase 3**: Process exam paper archive (Questions → Topics)
4. **Phase 4**: Activate Graphiti temporal layer
5. **Phase 5**: Deploy API for student queries and mastery tracking
