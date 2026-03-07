# **Semantic Indexing and Knowledge Graph Architecture for the Irish Education System: A Comprehensive Technical Implementation Report**

## **Executive Summary**

The digital transformation of national education systems represents one of the most significant data engineering challenges of the modern era. It is not merely a task of digitization—converting paper documents to PDF—but of *datafication*, where the underlying pedagogical logic, assessment criteria, and legal frameworks are extracted, structured, and interconnected. The Irish education system, with its tripartite governance structure involving the National Council for Curriculum and Assessment (NCCA), the State Examinations Commission (SEC), and the Department of Education, presents a particularly complex data landscape. This report serves as an exhaustive technical blueprint for the design and implementation of a Semantic Knowledge Graph tailored to this ecosystem, utilizing the advanced capabilities of **BAML (Better Also Make Libraries)** for structured extraction, **CocoIndex** for high-velocity ETL pipelines, and **Graphiti** for temporal reasoning.  
The core thesis of this research is that a "one-size-fits-all" indexing strategy is doomed to failure in the educational domain. The semantic structure of a primary school curriculum, which emphasizes integrated competencies and holistic development, is fundamentally different from the rigid, point-based logic of a Leaving Certificate Chemistry marking scheme.1 Furthermore, the temporal dimension of educational policy—where circular letters legally supersede previous mandates—requires a graph architecture capable of "time travel," allowing stakeholders to query the state of the system at any point in history.  
This report details the specific BAML schemas required to capture the nuances of every major subject area, from the "Unifying Strands" of Junior Cycle Science to the "Features of Quality" in Classroom-Based Assessments. It creates a unified ontology that links the *aspirational* curriculum (what should be taught) with the *enacted* curriculum (what is assessed) and the *regulatory* framework (how schools are managed), providing a queryable intelligence engine for policymakers, researchers, and educators.

## ---

**1\. Introduction: The Tripartite Data Landscape**

To architect an effective semantic index, one must first deconstruct the information hierarchy of the source domains. The Irish education system is not a monolith; it is a federation of three distinct data estates, each with its own ontology, publication cadence, and "truth" definitions.1

### **1.1 The NCCA Domain: Pedagogical Intent**

The National Council for Curriculum and Assessment (NCCA) operates curriculumonline.ie and ncca.ie. These domains represent the "Legislative Branch" of the curriculum—they define the laws of what must be taught. The primary unit of information here is the **Specification**. However, as our analysis of the Junior Cycle Science specification reveals, these documents are not simple hierarchical trees. They contain "Unifying Strands" like the *Nature of Science* that operate transversely across "Contextual Strands" like the *Physical World* or *Biological World*.1 A standard hierarchical index would fail to capture these lateral dependencies, treating them as siblings rather than intersecting dimensions.  
Furthermore, the NCCA domain houses the qualitative data of the system. The "Assessment Guidelines" do not provide numerical marks but rather descriptive rubrics—"Features of Quality." Phrases such as "In line with expectations" or "Exceptional" map to specific paragraphs of text that define student performance.1 The BAML schemas designed for this domain must therefore be capable of extracting *semantic vectors* from these qualitative descriptions, allowing for similarity-based retrieval rather than just keyword matching.

### **1.2 The SEC Domain: Evidentiary Truth**

The State Examinations Commission (examinations.ie) represents the "Judicial Branch." It interprets the NCCA's laws through the mechanism of the state exam. This domain is the repository of **Evidence**. It contains Examination Papers, Marking Schemes, and Chief Examiner Reports.  
The data engineering challenge here is extreme granularity. A Marking Scheme is not a prose document; it is a logic gate. As seen in the 2023 Chemistry Higher Level scheme, a single line item might contain a correct value ("0.000114"), a valid alternative format ("1.14 x 10^-4"), a mark allocation ("9 marks"), and a conditional penalty ("deduct 1 mark for arithmetic slip").1 Capturing this logic requires a BAML schema that can parse conditional statements and mathematical constraints, transforming them into executable validation rules within the knowledge graph.

### **1.3 The Department of Education Domain: Temporal Governance**

The Department of Education (gov.ie, circulars.gov.ie) acts as the "Executive Branch." It manages the system through **Circular Letters**. The critical feature of this domain is **Temporal Validity**. Policies are not static; they evolve. Circular 0003/2018, regarding Leadership and Management, explicitly "supersedes" Circular 29/02.1  
If a knowledge graph ingests both documents as equal "facts," it creates a contradictory database. The architecture must utilize **Graphiti** to model the SUPERSEDES relationship as a temporal edge, effectively "expiring" the nodes associated with the old circular when the context time shifts past January 2018\. This allows for longitudinal analysis of policy shifts, such as the transition from "Post of Responsibility" to "Assistant Principal" roles.

## ---

**2\. Architectural Ontology and Design Principles**

Before defining the subject-specific schemas, we establish the core ontology that creates a unified fabric for the Knowledge Graph. This ontology utilizes **Polymorphism** to handle the diversity of educational entities while maintaining a shared query interface.

### **2.1 The Core Entity Metamodel**

We define a root entity type, EducationalNode, from which all specific data types inherit. This allows global queries (e.g., "Find all nodes related to 'Sustainability'") while enabling specific property extraction.

| Entity Type | Description | Source Domain | Key Attribute Example |
| :---- | :---- | :---- | :---- |
| **CurriculumSpecification** | The defining document for a subject or short course. | NCCA | level: Junior Cycle, subject: History |
| **PedagogicalUnit** | A structural division of learning. | NCCA | type: Strand, type: Area of Practice |
| **LearningOutcome** | The atomic unit of instruction. | NCCA | action\_verb: Investigate, id: PW1 |
| **AssessmentInstrument** | A specific test or task. | SEC / NCCA | type: Exam Question, type: CBA |
| **EvidenceLogic** | The rule for awarding marks or quality ratings. | SEC | penalty\_type: Slip, marks: 3 |
| **PolicyDirective** | An administrative rule or update. | Dept of Ed | circular\_id: 0003/2018, status: Active |

### **2.2 The Temporal Edge Schema**

The power of this architecture lies in the edges. Using Graphiti's schema capabilities, we define specific relationship types that carry semantic and temporal weight.

* **ASSESSES**: Connects an AssessmentInstrument (Question) to a LearningOutcome. This edge is weighted by *Semantic Similarity* (calculated via CocoIndex vector embedding). A strong edge (weight \> 0.85) implies the question directly tests the outcome.  
* **DEFINES\_QUALITY**: Connects a EvidenceLogic (Rubric) to a PedagogicalUnit (CBA). This edge carries the definitions of success.  
* **SUPERSEDES**: The temporal operator. A PolicyDirective (Circular) connects to a previous PolicyDirective via this edge. Graphiti uses this to determine *Node Visibility* at query time.  
* **EVIDENCES\_DIFFICULTY**: Connects a ChiefExaminerComment to a LearningOutcome. If the examiner notes that "Students struggled with organic synthesis," this edge is created, flagging the outcome as "High Difficulty" in the graph.1

## ---

**3\. The Primary Curriculum Framework: Integrated Schema Design**

The Primary Curriculum in Ireland is currently undergoing a systemic overhaul with the introduction of the *Primary Curriculum Framework*. Unlike the secondary system, which is compartmentalized into discrete subjects, the Primary Framework is **Integrated**. It organizes learning into broad "Curriculum Areas" and emphasizes "Key Competencies" that transcend specific lessons. A BAML schema for this domain must prioritize *connectivity* over *hierarchy*.

### **3.1 Schema: The Curriculum Area**

The framework identifies five key areas: *Language, Mathematics and Science and Technology Education (STEM), Wellbeing, Arts Education, and Social and Environmental Education*. The BAML extractor must identify the **Strands** and **Elements** within these areas while simultaneously extracting the cross-curricular links.

#### **BAML Definition: PrimaryCurriculumArea**

Code snippet

// BAML Definition for Primary Curriculum Structure

enum PrimaryStage {  
  Stage1\_JuniorSeniorInfants  
  Stage2\_FirstSecondClass  
  Stage3\_ThirdFourthClass  
  Stage4\_FifthSixthClass  
}

class CompetencyLink {  
  competency\_name: string @description("e.g., 'Being a Digital Learner', 'Being Creative'")  
  context: string @description("How this specific outcome supports the competency")  
}

class PrimaryLearningOutcome {  
  id: string? @description("Code if available, e.g., 'LO-Math-1'")  
  text: string @description("The statement of learning")  
  element: string @description("The 'Element' of learning, e.g., 'Communicating', 'Understanding'")  
  progression\_continuum: string? @description("Reference to the continuum milestone")  
  key\_competencies: CompetencyLink  
}

class PrimaryStrand {  
  name: string @description("e.g., 'Number', 'Data and Chance'")  
  description: string  
  outcomes: PrimaryLearningOutcome  
}

class PrimaryCurriculumArea {  
  name: string @description("e.g., 'Mathematics', 'Language'")  
  rationale: string  
  strands: PrimaryStrand  
  integration\_links: string @description("Explicit text mentioning other areas, e.g., 'Links to Geography'")  
}

function ExtractPrimaryFramework(text: string) \-\> PrimaryCurriculumArea {  
  client "openai/gpt-4-turbo"  
  prompt \#"  
    Analyze the text from the Primary Curriculum Framework.  
    Extract the structural hierarchy.  
      
    CRITICAL: The Primary Framework uses a matrix of 'Strands' and 'Elements'.  
    Ensure that every Learning Outcome is tagged with its parent 'Strand' and its 'Element'.  
      
    Look for specific icons or text labels that indicate 'Key Competencies' and extract them as structured links.  
      
    Text:  
    {{ text }}  
  "\#  
}

### **3.2 The "Aistear" Integration (Infant Classes)**

For the infant classes (Junior and Senior Infants), the Primary Curriculum overlaps with the *Aistear* Early Childhood Curriculum Framework. Aistear is organized around four themes: *Well-being, Identity and Belonging, Communicating,* and *Exploring and Thinking*.  
The architecture must support a **Bridge Node**. We define an AistearTheme entity. When processing Primary Curriculum documents for Stage 1, the BAML extractor is prompted to identify alignment with these themes.  
**Graph Edge Logic:**

* PrimaryLearningOutcome(Stage 1\) \-\> SUPPORTS \-\> AistearTheme(Exploring and Thinking).  
* **Insight:** This allows for a "Pedagogical Continuity" query. A researcher can ask, "How does the Mathematics curriculum in First Class build upon the 'Exploring and Thinking' theme from the Infant classes?" The graph traversal provides the answer by following the SUPPORTS edges back to the shared Aistear node.

## ---

**4\. Junior Cycle Specifications: The Non-Linear Pedagogy**

The Junior Cycle (lower secondary) represents the most significant departure from traditional linear syllabi. The introduction of "Specifications" replaced the old "Syllabus" documents. These specifications are often non-sequential, relying on a network of Learning Outcomes (LOs) rather than a list of chapters. This requires a sophisticated BAML schema capable of detecting implicit relationships.

### **4.1 The Science Block: Unifying vs. Contextual Strands**

As highlighted in the domain analysis 1, the Junior Cycle Science specification uses a unique architecture. It features a "Unifying Strand" called *Nature of Science* (NoS) which is not taught in isolation but is embedded within the contextual strands (*Physical World, Biological World, Chemical World, Earth & Space*).

#### **BAML Definition: ScienceSpecification**

We need a schema that explicitly separates the Unifying Strand and creates "Transverse" links.

Code snippet

class ScienceOutcome {  
  id: string @description("e.g., 'CW4', 'PW2', 'NoS1'")  
  strand\_type: string @description("Contextual or Unifying")  
  strand\_name: string @description("e.g., 'Chemical World', 'Nature of Science'")  
  text: string  
  action\_verb: string @description("e.g., 'Investigate', 'Design', 'Evaluate'")  
  keywords: string  
}

class TransverseLink {  
  source\_outcome\_id: string @description("The ID of the Contextual outcome")  
  target\_nos\_id: string @description("The ID of the Nature of Science outcome it relies on")  
  strength: string @description("High/Medium/Low based on verb analysis")  
}

class JuniorCycleScienceSpec {  
  unifying\_strand: ScienceOutcome  
  contextual\_strands: ScienceOutcome  
  inferred\_links: TransverseLink  
}

function ExtractScienceSpec(text: string) \-\> JuniorCycleScienceSpec {  
  client "openai/gpt-4-turbo"  
  prompt \#"  
    Extract the Junior Cycle Science Specification.  
      
    Step 1: Identify the 'Nature of Science' outcomes. These are the Unifying Strand.  
    Step 2: Identify the Contextual outcomes (Physical, Biological, Chemical, Earth/Space).  
      
    Step 3: COMPLEX TASK. For each Contextual outcome, analyze the 'Action Verb'.   
    If the verb implies data analysis (e.g., 'analyze data'), link it to the relevant NoS outcome.  
    If the verb implies experimentation (e.g., 'investigate'), link it to NoS outcomes regarding investigation.  
      
    Text:  
    {{ text }}  
  "\#  
}

**Pedagogical Insight:** By extracting the action\_verb (Bloom's Taxonomy), we can analyze the "Cognitive Depth" of the curriculum. If an exam question asks students to "state" a fact, but the linked Learning Outcome requires them to "evaluate" it, the system can flag an **Alignment Gap**.

### **4.2 The Wellbeing Area: CSPE, SPHE, and PE**

Civil, Social and Political Education (CSPE) is a "Short Course" within the Wellbeing area. The snippet 1 notes that Strands (e.g., "Rights and Responsibilities") contain numbered outcomes (1.1, 1.2) but explicitly states that *no hierarchy* is implied. This is a crucial architectural constraint: the graph must model these as a **Flat Set**, not a dependency tree.

#### **BAML Definition: WellbeingShortCourse**

Code snippet

class WellbeingStrand {  
  name: string @description("e.g., 'Rights and Responsibilities'")  
  outcomes: LearningOutcome  
}

class ActionProject {  
  title: string  
  description: string  
  linked\_strands: string  
}

class CSPESpec {  
  strands: WellbeingStrand  
  action\_projects: ActionProject @description("Suggested or mandatory projects")  
}

Graphiti Logic:  
Short courses often rely on "Classroom-Based Assessments" (CBAs) rather than final exams. For CSPE, the "Action Record" is key. The graph connects ActionProject nodes to WellbeingStrand nodes via a REALIZES edge.

### **4.3 The Arts & Humanities: Qualitative Assessment**

Subjects like Visual Art, Music, History, and Geography rely heavily on qualitative assessment criteria. The NCCA publishes "Assessment Guidelines" containing "Features of Quality" rubrics.1 These are text-dense matrices describing performance levels: *Exceptional, Above Expectations, In Line with Expectations, Yet to Meet Expectations*.

#### **BAML Definition: QualitativeRubric**

This is one of the most high-value schemas. By vectorizing these descriptions, we enable semantic grading assistance.

Code snippet

enum AchievementLevel {  
  Exceptional  
  AboveExpectations  
  InLineWithExpectations  
  YetToMeetExpectations  
}

class RubricDescriptor {  
  level: AchievementLevel  
  text: string @description("The full descriptive paragraph")  
  key\_qualities: string @description("Extracted phrases: 'comprehensive analysis', 'limited understanding'")  
  negative\_indicators: string @description("Phrases indicating what is missing")  
}

class AssessmentTask {  
  name: string @description("e.g., 'CBA 1: The Past in My Place'")  
  timing: string  
  rubrics: RubricDescriptor  
}

Semantic Search Application:  
A teacher uploads a student's history essay. The system embeds the essay. It then calculates the Cosine Similarity between the essay vector and the RubricDescriptor vectors for that CBA.

* *Result:* "Closest match: In Line with Expectations (Similarity 0.89). Distance from Exceptional: 0.45."  
* *Feedback:* "To move towards 'Exceptional', the work needs more 'critical evaluation of sources' as defined in the descriptor."

## ---

**5\. Senior Cycle: High-Stakes Assessment and Evidence Modeling**

The Senior Cycle (Leaving Certificate) is the high-stakes terminal examination. The data architecture here must shift from "Pedagogical intent" to "Evidentiary precision." The primary data sources are the **Examination Papers** and the **Marking Schemes** from the SEC.1

### **5.1 The STEM Block: Logic-Gate Marking Schemes**

Chemistry, Physics, Biology, and Maths marking schemes are algorithmic. They define precise conditions for awarding marks and specific penalties for errors (e.g., "Slips"). As seen in the Chemistry example 1, a "Slip" might incur a \-1 penalty, while a "Chemical Error" might forfeit all marks for that section.

#### **BAML Definition: AdvancedMarkingLogic**

We require a schema that parses the text into a computable rule set.

Code snippet

class PenaltyRule {  
  type: string @description("e.g., 'Arithmetic Slip', 'Chemical Error', 'Unit Omission'")  
  deduction: float @description("The value to deduct, e.g., \-1, \-3")  
  scope: string @description("e.g., 'per occurrence', 'max \-3 for this part'")  
}

class MarkingPoint {  
  correct\_answer: string @description("The target value or phrase")  
  marks\_awarded: int  
  valid\_alternatives: string @description("Other acceptable answers listed in the scheme")  
  mandatory\_keywords: string @description("Words that MUST be present (often bolded)")  
  examiner\_notes: string? @description("Guidance like 'accept rounded values'")  
}

class QuestionPartSchema {  
  part\_id: string @description("e.g., '(b)(ii)'")  
  total\_marks: int  
  marking\_points: MarkingPoint  
  penalties: PenaltyRule  
}

function ExtractMarkingScheme(text: string) \-\> QuestionPartSchema {  
  client "openai/gpt-4-turbo"  
  prompt \#"  
    Analyze the Marking Scheme segment.  
    Extract the logic for awarding marks.  
      
    CRITICAL: Identify 'Penalties' and 'Deductions'.  
    Distinguish between a 'Slip' (minor error) and a fundamental error.  
      
    Look for lists of values separated by '/' which indicate alternatives.  
      
    Text:  
    {{ text }}  
  "\#  
}

Graph Integration:  
These QuestionPartSchema nodes are attached to the ExamQuestion nodes. This allows for detailed statistical queries:

* *Query:* "Show me all Chemistry questions from 2015-2023 where marks are explicitly deducted for 'significant figure' errors."  
* *Mechanism:* The Graphiti engine filters for PenaltyRule nodes where type contains "significant figure" or "sig fig".

### **5.2 The Humanities: Research and Essays**

Leaving Certificate History and Geography involve significant project work—the **Research Study Report (RSR)** and the **Geographical Investigation (GI)**. These account for 20% of the grade and are submitted prior to the exam. The schema must model the "Prescribed Topics" which change annually.

#### **BAML Definition: PrescribedTopicYearly**

Code snippet

class CaseStudy {  
  topic\_name: string @description("e.g., 'The Jarrow March'")  
  year\_valid: int @description("The exam year this applies to")  
  documents\_prescribed: string @description("Specific texts or sources students must study")  
}

class HistorySyllabusDeep {  
  core\_topics: string  
  case\_studies: CaseStudy  
  rsr\_rationale: string  
}

Temporal Note:  
Graphiti is essential here. The Case Study on "The Jarrow March" might be valid for the 2022 and 2023 cohorts but not 2024\. The valid\_year attribute allows the graph to serve the correct content to the student based on their exam year.

### **5.3 Languages: The Oral/Aural/Written Triad**

Leaving Certificate Irish (Gaeilge) and Modern Foreign Languages (French, German, Spanish) have a fragmented assessment structure:

1. **Oral Exam (approx. 25-40%)**  
2. **Aural (Listening) Exam**  
3. **Written Exam**

The BAML schema must separate these components to allow for targeted retrieval. A student struggling with the "Sraith Pictiúr" (Picture Series) in Irish needs resources specifically linked to the Oral component, not the written literature.

#### **BAML Definition: LanguageComponent**

Code snippet

enum ComponentType {  
  Oral  
  Aural  
  Written\_Comprehension  
  Written\_Production  
}

class LanguageSection {  
  type: ComponentType  
  name: string @description("e.g., 'An Triail', 'Sraith Pictiúr'")  
  marks: int  
  time\_allocation: string  
  content\_focus: string @description("Topics covered, e.g., 'Social Issues', 'Literary History'")  
}

## ---

**6\. The Vocational Pathways: LCA and LCVP Architecture**

The Irish system includes two alternative pathways: the **Leaving Certificate Applied (LCA)** and the **Leaving Certificate Vocational Programme (LCVP)**. These are often overlooked in digital systems but represent a significant student cohort. They rely heavily on **Portfolio Assessment**.

### **6.1 LCVP: The Link Modules**

LCVP students take standard subjects plus "Link Modules": *Preparation for the World of Work* and *Enterprise Education*. The assessment is primarily a **Portfolio of Coursework** (60%).

#### **BAML Definition: PortfolioSpecification**

Code snippet

class PortfolioItem {  
  title: string @description("e.g., 'Curriculum Vitae', 'Career Investigation'")  
  core\_items: boolean @description("True if mandatory")  
  optional\_items: boolean @description("True if chosen from a list")  
  assessment\_criteria: string @description("Specific requirements for valid submission")  
}

class LCVPModuel {  
  name: string  
  learning\_outcomes: LearningOutcome  
  portfolio\_requirements: PortfolioItem  
}

### **6.2 LCA: The Modular Credit System**

LCA operates on a credit system (Credits accumulated over 2 years). The BAML schema must capture the "Key Assignments" which are mandatory for credit linkage.

#### **BAML Definition: LCAKeyAssignment**

Code snippet

class KeyAssignment {  
  module: string @description("e.g., 'Social Education'")  
  task\_description: string  
  evidence\_required: string @description("e.g., 'Logbook', 'Interview', 'Artifact'")  
  credits\_value: int  
}

## ---

**7\. The Policy Layer: Temporal Governance and Circulars**

The Department of Education governs the entire system through **Circular Letters**. As identified in 1, these documents define the "Legal Reality" of schools. The architectural challenge is that a circular is a "Patch" applied to the system state.

### **7.1 The Circular BAML Schema**

We need to extract the metadata that drives the temporal graph.

Code snippet

enum CircularStatus {  
  NewPolicy  
  Amendment  
  Repeal  
  Clarification  
}

class LinkedCircular {  
  id: string  
  relationship: string @description("Supersedes, Refers to, Amends")  
}

class CircularMetadata {  
  circular\_id: string @description("e.g., '0003/2018'")  
  title: string  
  issue\_date: string  
  effective\_date: string  
  status: CircularStatus  
  linked\_circulars: LinkedCircular  
  domains\_affected: string @description("e.g., 'Leadership', 'Special Needs', 'Curriculum'")  
}

function ExtractCircularMeta(text: string) \-\> CircularMetadata {  
  client "openai/gpt-4-turbo"  
  prompt \#"  
    Analyze the Circular Letter.  
      
    1\. Extract the ID and Dates.  
    2\. CRITICAL: Find the 'Supersedes' or 'Rescinds' text.   
       Example: 'This circular supersedes Circular 29/02'.  
       Extract the ID of the \*old\* circular being replaced.  
         
    3\. Identify the Domain. Is this about Staffing? Assessment?  
      
    Text:  
    {{ text }}  
  "\#  
}

### **7.2 The Leadership & Management Framework (Graphiti Logic)**

Snippet 1 uses the transition from Circular 29/02 to 0003/2018 as a prime example.

* **Circular 29/02** defined "Special Duties Teachers".  
* **Circular 0003/2018** redefined these as "Assistant Principal II" and introduced a new grid of 4 domains (Leading Teaching and Learning, etc.).

**Graphiti Implementation:**

1. **Ingest 29/02 (Time: 2002):** Creates node Role: Special Duties Teacher.  
2. **Ingest 0003/2018 (Time: 2018):**  
   * Creates node Role: Assistant Principal II.  
   * Creates edge: Circular(0003/2018) \--SUPERSEDES--\> Circular(29/02).  
   * Creates edge: Role(Assistant Principal II) \--REPLACES--\> Role(Special Duties Teacher).  
3. **Query Execution:**  
   * User Query: "What are the duties of middle management?"  
   * Context 2010: Returns "Pastoral, Administrative..." (from 29/02).  
   * Context 2020: Returns "Leading Teaching and Learning..." (from 0003/2018).

This prevents the "Hallucination of Extinct Roles" which plagues standard RAG systems in legal/policy domains.

## ---

**8\. Technical Implementation: CocoIndex and Graphiti Pipelines**

The deployment of these schemas relies on a reactive data pipeline. We utilize **CocoIndex** to orchestrate the flow and **Graphiti** to manage the state.

### **8.1 The CocoIndex Flow Strategy**

We define three distinct flows based on the document velocity and structure.

| Flow Name | Source Type | Frequency | BAML Strategy | Graphiti Action |
| :---- | :---- | :---- | :---- | :---- |
| **CurriculumFlow** | curriculumonline.ie | Low (Annual) | ExtractScienceSpec, ExtractPrimaryFramework | Upsert Nodes (Stable) |
| **EvidenceFlow** | examinations.ie | High (Annual bursts) | ExtractMarkingScheme, ExtractExamQuestion | Append Episodes (Cumulative) |
| **PolicyFlow** | gov.ie | Ad-hoc (Weekly) | ExtractCircularMeta | Temporal Patching (State Change) |

#### **Sample CocoIndex Definition (Python Pseudo-code)**

Python

from cocoindex import FlowBuilder, Source, Transform  
from baml\_client import baml as b\_client

def build\_evidence\_pipeline():  
    \# Source: Local buffer of exam papers (downloaded via crawler)  
    exam\_source \= Source.LocalFile(path="./data/sec\_papers/\*.pdf")  
      
    \# Transform 1: Chunking  
    chunks \= exam\_source.apply(Transform.ChunkByRegex(pattern=r"Question \\d+"))  
      
    \# Transform 2: BAML Extraction (Marking Schemes)  
    \# Using the specific schema for Logic Gates  
    structured\_data \= chunks.apply(  
        lambda text: b\_client.ExtractMarkingScheme(text)  
    )  
      
    \# Transform 3: Vector Embedding  
    vectors \= structured\_data.apply(  
        lambda data: embed\_model.encode(data.question\_text)  
    )  
      
    \# Sink: Graphiti  
    \# We map the extracted 'Year' to the Graphiti 'reference\_time'  
    structured\_data.sink\_to\_graphiti(  
        edge\_map={"ASSESSES": "learning\_outcome\_id"},  
        time\_field="year"  
    )

### **8.2 Graphiti: The Semantic Reasoner**

Graphiti is not just a storage layer; it is the inference engine. It utilizes the edges created by CocoIndex to answer complex questions.  
**The "Alignment Gap" Algorithm:**

1. Select a Subject (e.g., Junior Cycle Science).  
2. Retrieve all LearningOutcome nodes.  
3. Traverse ASSESSES edges to count connected ExamQuestion nodes from the last 5 years.  
4. Identify LearningOutcome nodes with degree\_centrality \== 0 (Orphans).  
5. **Report:** "The following outcomes have not been assessed in the past 5 exam cycles: \[List\]."

**The "Predictive Marking" Assistant:**

1. User uploads a draft answer.  
2. System retrieves the MarkingPoint nodes for that question.  
3. System retrieves PenaltyRule nodes (e.g., "Deduct for units").  
4. System scans draft answer for "Units". If missing \-\> Suggest deduction.  
5. System compares answer text to valid\_alternatives. If match \-\> Suggest full marks.

## ---

**9\. Conclusion and Strategic Implications**

This report has outlined a comprehensive, "Polymorphic" architecture for the semantic indexing of the Irish education system. By acknowledging the structural differences between the *Integrated* Primary Curriculum, the *Non-Linear* Junior Cycle, and the *Evidentiary* Senior Cycle, we have designed a suite of BAML schemas that capture the true pedagogical reality of each domain.  
The integration of **Graphiti** provides the essential temporal dimension, solving the critical problem of policy evolution and supersession defined in the Department of Education's circulars. Meanwhile, the use of **CocoIndex** allows for the high-fidelity extraction of complex marking logic, transforming static PDF archives into a computational engine.

### **Implications for Stakeholders**

1. **For the NCCA:** The system provides immediate feedback on "Orphan Outcomes"—parts of the curriculum that are effectively invisible because they are never assessed.  
2. **For the SEC:** The schema highlights "Ambiguity Clusters"—questions where the marking scheme requires excessive "Examiner Notes" or alternative answers, indicating a poorly formulated question.  
3. **For Schools:** The "Leadership Framework" graph allows principals to navigate the complex web of changing circulars with legal precision, ensuring compliance with the latest governance mandates.

This architecture represents a shift from *managing documents* to *managing knowledge*, creating a digital infrastructure that is as dynamic and interconnected as the education system it serves.

#### **Works cited**

1. Copy%20of%20Semantic%20Indexing%20of%20Irish%20Education%20System.pdf.pdf