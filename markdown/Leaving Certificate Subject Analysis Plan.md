# **Comprehensive Architectural Strategy for the Pan-Curricular Expansion of the Irish Leaving Certificate AI Tutoring System**

## **1\. Architectural Imperatives and the Universal Application of the Backend Strategy**

The rigorous analysis of the "Backend Strategy for Educational Tutoring System" 1 established a foundational blueprint for a bilingual, temporally aware, and pedagogically valid AI tutor for Mathematics. However, the Irish Senior Cycle curriculum is a vast and heterogeneous landscape comprising over 30 distinct subjects, ranging from the highly deterministic logic of Physics to the interpretive complexity of English Literature and the causal density of History. To transition from a pilot Mathematics system to a comprehensive national tutoring infrastructure, the architectural primitives identified in the research—FalkorDB, Cognee, BAML, Graphiti, and Cocoindex—must be radically extrapolated. This report serves as the definitive technical specification for this expansion, analyzing the curriculum structures, assessment logics, and data ingestion requirements for the full spectrum of Leaving Certificate subjects.

### **1.1 The Theoretical Framework: Pedagogical Content Knowledge (PCK) as a Graph**

The core insight of the initial research is that an educational knowledge graph must model Pedagogical Content Knowledge (PCK), not just raw information.1 PCK represents the intersection of content knowledge (what is taught) and pedagogical knowledge (how it is taught). In Mathematics, this was modeled through "Strands" and "Prerequisite" edges.1 When scaling to other subjects, this graph theory must evolve. We are no longer simply modeling logical derivation; we are modeling taxonomical hierarchies in Biology, causal networks in History, and thematic webs in English. The backend architecture must therefore support a polymorphic graph structure where the semantics of the edges change depending on the Subject domain of the nodes they connect.  
The "Strand" structure, identified in the Senior Cycle documentation 1, acts as the universalizing meta-structure. Whether the subject is Agricultural Science or Classical Studies, the National Council for Curriculum and Assessment (NCCA) organizes content into broad Strands. This allows the high-level ontology in FalkorDB to remain consistent: a root Subject node branches into Strand nodes, which branch into Topic nodes. However, the traversal logic—the algorithm used by the AI to move between nodes—must be customized for each domain. In Math, traversal is vertical (foundation to advanced). In Geography, traversal is often spatial (local to global). In History, it is temporal (cause to effect).

### **1.2 The "Ground Truth" Problem across Domains**

The research identifies the "Examination Paper" and its associated "Marking Scheme" as the ultimate source of truth.1 This is a critical architectural constraint. The State Examinations Commission (SEC) does not grade based on general correctness but on adherence to specific "Marking Scales" (e.g., Scale 10C).1 Expanding this to the humanities introduces a massive challenge: Subjectivity.  
In Mathematics, a "Scale 10C" (High/Mid/Low Partial Credit) is assigned based on definitive steps.1 In English, a similar scale is assigned based on "PCLM" (Purpose, Coherence, Language, Mechanics). The backend cannot simply look for keyword matches. It must implement a "Rubric-Based Evaluation Engine" within the BAML extraction layer. This engine requires not just the extraction of the marking scheme text, but the semantic vectorization of the *qualitative descriptors* provided by the Chief Examiner. When a Marking Scheme says "Reward independent thought," the system must translate "independent thought" into a vector embedding derived from a corpus of high-grade sample essays, allowing the AI to benchmark student work against a nebulous standard.

### **1.3 The Bilingual Mandate in a Multi-Subject Context**

The requirement for a bilingual system (T1/T2 schools) 1 becomes exponentially more complex outside of Mathematics. In Math, the translation is largely lexical (Triangle \= Triantán). In subjects like History or Business, the translation is conceptual and dialectal. The "Unified Concept Node" strategy proposed for Math—where a single node holds both English and Irish properties 1—must be rigorously tested against subjects where the language *is* the medium of analysis. For example, in the subject "History," a source document in Irish regarding the formation of the Gaelic League contains nuances that are lost in translation. The graph must therefore support "Dual-Source Nodes," where the original Irish text is preserved as a distinct entity from its English translation, allowing the AI to tutor T1 students using the original primary sources, preserving the "cló gaelach" or specific phrasing mentioned in the research.1

## **2\. Domain Analysis: The Experimental Sciences (Biology, Physics, Chemistry, Ag Science)**

The "Science Group" represents the closest adjacent domain to Mathematics, yet it introduces unique data modalities—specifically diagrams and taxonomies—that require a specialized configuration of the Cocoindex pipeline.

### **2.1 Physics: The Mathematical-Empirical Bridge**

Physics in the Leaving Certificate is effectively applied mathematics with an empirical layer. The syllabus relies heavily on the "Algebra" and "Functions" strands of the Math curriculum.1

#### **2.1.1 Cross-Graph Dependencies**

The primary architectural innovation for Physics is the implementation of **Cross-Graph Dependencies**. The PhysicsGraph cannot exist in isolation; it must query the MathsGraph.

* **Ontological Linkage:** When a student studies "Linear Motion" (Physics Strand: Mechanics), they rely on the concept of "Slope" (Math Strand: Coordinate Geometry).1  
* **Implementation:** The Cognee adapter must be configured to create "inter-graph edges." A node :Topic {name: "Velocity-Time Graphs", subject: "Physics"} must have a specific edge type :REQUIRES\_MATH\_CONCEPT pointing to :Topic {name: "The Line", subject: "Maths"}.  
* **Operational Logic:** When the AI detects a student failing a Physics question on velocity, it traverses this edge. If the student has a low mastery score on the linked Math node (stored in Graphiti), the tutor diagnoses the failure not as a *Physics* error but as a *Math* error, prompting a revision of the Math concept. This nuanced diagnosis is only possible through the rigorous interlinking of the two domain graphs.

#### **2.1.2 BAML Extraction for Scientific Notation**

The BAML extraction logic defined for Math questions 1 focuses on converting formulas to LaTeX. For Physics, this must be extended to handle **Dimensional Analysis**.

* **Schema Extension:** The ExtractQuestions function must be modified to identify "Units." A number in Physics is meaningless without its unit ($ms^{-2}$, $N$, $J$).  
* **BAML Code:**  
  Code snippet  
  class PhysicsValue {  
      magnitude: float  
      unit: string @description("SI Unit, derived from context if necessary")  
      dimension: string @description("e.g., Length, Time, Force")  
  }  
  class PhysicsQuestion {  
      // Inherits standard question fields  
      given\_values: PhysicsValue  
      required\_value\_dimension: string  
  }

  This structured extraction allows the backend to perform "Dimensional Consistency Checks" on generated answers, ensuring the AI never hallucinates a time value when a force is required.

### **2.2 Biology: The Taxonomical and Systemic Graph**

Biology differs fundamentally from Math and Physics. It is not derivation-based; it is system-based. The content is organized into hierarchical taxonomies (Kingdom \-\> Species) and complex interacting systems (Photosynthesis, Respiration).

#### **2.2.1 Modeling Biological Systems in FalkorDB**

The maths\_curriculum.owl ontology 1 is insufficient. We require a biology.owl that defines systemic relationships.

* **New Edge Types:** The graph must support flow-based edges.  
  * :Organelle {name: "Mitochondria"} \----\> :Process {name: "Respiration"}.  
  * :Process {name: "Respiration"} \----\> :Molecule {name: "ATP"}.  
* **Query Logic:** In Math, retrieval is often "Find similar questions." In Biology, retrieval is "Trace the pathway." If a student asks about "ATP," the system queries FalkorDB for all PRODUCES edges leading to ATP, effectively reconstructing the metabolic pathways dynamically.

#### **2.2.2 Visual Data Ingestion (Diagrams)**

Biology exams are visually dense. The "Layout Analysis" transformation in Cocoindex 1 must be upgraded with a specialized Computer Vision (CV) model trained on scientific diagrams.

* **The Problem:** A BAML extractor reading text from a PDF will miss the unlabeled diagram of a heart which is the central component of the question.  
* **The Solution:** The Cocoindex pipeline must include a "Diagram Segmentation" step before BAML extraction.  
  1. **Detection:** Identify regions containing line art/diagrams.  
  2. **Labeling:** Use a Multimodal LLM to generate a textual description of the diagram (e.g., "Diagram of a vertical section of a human heart, labels A and B pointing to the Aorta and Ventricle").  
  3. Embedding: Embed this description alongside the question text in the Vector Index.  
     This ensures that when a student asks "What does the heart look like?", the system can retrieve questions that contain heart diagrams, even if the word "heart" is not in the question text.

### **2.3 Chemistry: The Syntax of Matter**

Chemistry requires a unique ingestion strategy due to its reliance on chemical syntax, which is distinct from mathematical LaTeX.

#### **2.3.1 Chemical Markup Language (CML) Integration**

The BAML extractor must be prompted to recognize and format chemical equations not just as LaTeX, but specifically using packages like mhchem or as SMILES strings for organic molecules.

* **Searchability:** Storing "Benzene" as a word is insufficient. Storing it as a SMILES string C1=CC=CC=C1 allows for substructure searching.  
* **Graph Structure:** The "Family" structure of Organic Chemistry (Alkanes, Alkenes, Alcohols) maps perfectly to the Strand \-\> Topic hierarchy.  
  * :Family {name: "Alcohols"} \----\> :Molecule {name: "Ethanol"}.  
  * :Molecule {name: "Ethanol"} \----\> :Reaction {name: "Oxidation"}.

### **2.4 Agricultural Science: The Applied Integration**

Agricultural Science is a hybrid subject, combining Biology, Chemistry, and Geography. It introduces the concept of the **"Project"** (Individual Investigative Study) which accounts for 25% of the marks.

* **Project Support:** The backend must support "Long-Form Context." Unlike the short context of an exam question, a student's project is a developing document.  
* **Graphiti Implementation:** The "Agentic Memory" 1 must track the state of the student's project over months.  
  * *Episode:* Student uploads draft introduction.  
  * *Episode:* Student uploads data results.  
  * Graphiti maintains the "Project State" node, allowing the AI to critique the "Conclusion" based on the "Data" uploaded weeks prior, a capability requiring the "Time Travel" feature 1 to reference previous versions of the work.

## **3\. Domain Analysis: The Humanities (History, Geography, Classical Studies)**

The Humanities introduce the challenge of "Unstructured Argumentation." The assessment logic shifts from "Correct/Incorrect" to "Coherent/Substantiated."

### **3.1 History: The Bi-Temporal Causal Graph**

History is the ultimate test case for the **Graphiti** module's bi-temporal capabilities.1

#### **3.1.1 The Double Timeline Paradox**

In Math, the "validity" of a theorem is eternal. In History, we must manage two distinct timelines:

1. **Historical Time (Valid Time):** The date the event occurred (e.g., 1916).  
2. **Curriculum Time (Transaction Time):** The date the topic was added to the syllabus or the date a specific interpretation became dominant.

Graphiti must be configured to index every :Event node with a real\_world\_timestamp. This allows the student to perform queries like "Show me all events in the 'Move toward War' strand between 1911 and 1914." Simultaneously, the curriculum\_validity timestamp tracks which Case Studies (e.g., The Montgomery Bus Boycott) are currently on the cycle for the 2025 exam, as these rotate periodically.

#### **3.1.2 Modeling Causality and Multiperspectivity**

The graph must enforce "Causal Chains."

* **Edge Logic:** :Event \----\> :Event is too simple. We need :CONTRIBUTED\_TO, :TRIGGERED, :LONG\_TERM\_CAUSE.  
* **Multiperspectivity:** History requires understanding different viewpoints. The graph should support "Perspective Nodes."  
  * :Event {name: "Anglo-Irish Treaty"}.  
  * :Perspective {name: "Pro-Treaty"} \----\> :Event.  
  * :Perspective {name: "Anti-Treaty"} \----\> :Event.  
  * When a student writes an essay, the BAML evaluator checks if the student has referenced *both* perspective nodes, fulfilling the marking scheme requirement for "balance."

#### **3.1.3 The Research Study Report (RSR)**

Like Ag Science, History includes a research project (RSR). The backend needs a "Source Evaluation Engine."

* **Ingestion:** The Cocoindex pipeline must ingest primary source documents provided by the student.  
* **Evaluation:** The AI must evaluate the sources for "Reliability" and "Bias." This requires a specialized Vector Index trained on historiographical terms, capable of detecting "polemic language" or "propaganda techniques" in the text.

### **3.2 Geography: The Geospatial Knowledge Graph**

Geography is unique because it anchors knowledge in physical space.

#### **3.2.1 Geospatial Indexing in FalkorDB**

FalkorDB supports geospatial queries. We must leverage this.

* **Node Enrichment:** Every :CaseStudy node (e.g., "The Greater Dublin Area") must be enriched with Lat/Long coordinates or Polygon boundaries.  
* **Query Capability:** This allows the AI to answer "Compare the economic development of a peripheral region (West) with a core region (East)." The system identifies the regions based on spatial metadata and retrieves the relevant economic statistics.

#### **3.2.2 The Significant Relevant Point (SRP) Logic**

The Geography marking scheme is mechanically unique. It operates on the "SRP" system—typically 2 marks per Significant Relevant Point.1

* **BAML Extraction:** The extractor for Marking Schemes must be tuned to identify SRPs.  
  * *Input Text:* "Award 2 marks for stating that interaction of air masses causes rain."  
  * *Extracted Object:* SRP { content: "Air mass interaction causes rain", marks: 2 }.  
* **Grading Logic:** When grading a student essay, the backend does not look for holistic quality. It performs a "Semantic Hit Count." It segments the student's text into sentences, compares each sentence against the database of valid SRPs using vector similarity, and increments the score for each match. This mimics the exact mechanical grading process of a state examiner.

#### **3.2.3 OS Map and Aerial Photograph Analysis**

Every Geography exam includes an Ordnance Survey (OS) map and an aerial photo.

* **Grid Reference Logic:** The system must understand the coordinate system.  
* **Computer Vision:** The "Layout Analysis" 1 must extract the map. A specialized CV model must be trained to identify features (Post Offices, Antiquities, Contours).  
* **Integration:** A question asking for the "Grid Reference of the Post Office" requires the backend to:  
  1. Locate the Post Office in the image.  
  2. Map the pixel coordinates to the Grid coordinates.  
  3. Compare the student's answer (e.g., O 234 567\) with the calculated reference.

## **4\. Domain Analysis: The Languages (Gaeilge, English, Modern Foreign Languages)**

The Language subjects represent the shift from "Convergent Thinking" (one right answer) to "Divergent Thinking" (multiple valid interpretations).

### **4.1 Gaeilge (The Irish Language)**

The bilingual requirement 1 is central here. The subject is assessed across three domains: Oral (40%), Aural, and Written.

#### **4.1.1 The Oral Examination (An Scrúdú Cainte)**

The existing text-based architecture is insufficient. The backend must integrate an **Audio Processing Pipeline**.

* **Cocoindex Flow:**  
  1. **Ingest:** Student records a response (e.g., "Describe your local area").  
  2. **Transcribe:** Use a Whisper-model fine-tuned on Irish dialects (Connacht, Munster, Ulster).  
  3. **Analyze:**  
     * **Fluency:** Measure pauses and speech rate.  
     * **Vocabulary (Saibhreas):** Compare the transcript against a "Rich Vocabulary" NodeSet in FalkorDB.  
     * **Grammar:** Check for specific grammatical structures (e.g., the Tuiseal Ginideach).  
* **Feedback:** The system returns not just a grade, but specific timestamps in the audio where errors occurred.

#### **4.1.2 Dialectal Modeling**

The graph must explicitly model dialectal variations.

* **Ontology:** :Word {lemma: "Look"} \----\> :Form {text: "Féach", dialect: "Standard"}.  
* **Ontology:** :Word {lemma: "Look"} \----\> :Form {text: "Amharc", dialect: "Ulster"}.  
* This ensures that if a student uses Ulster Irish, the system recognizes it as correct, preventing "False Negative" grading.

### **4.2 English: The Subjectivity Engine**

English requires the most sophisticated Semantic Analysis.

#### **4.2.1 PCLM Grading Architecture**

The Marking Scheme for English uses PCLM (Purpose, Coherence, Language, Mechanics).

* **Purpose (30%):** Did the student answer the question? (Vector similarity between Essay and Question).  
* **Coherence (30%):** Is the argument structured? (Discourse analysis: checking for transition words, paragraph structure).  
* **Language (30%):** Is the vocabulary varied? (Lexical diversity score).  
* Mechanics (10%): Spelling and grammar.  
  The backend must run four separate analysis routines on every essay submission and aggregate the results according to the weighted percentages defined in the Marking Scheme.1

#### **4.2.2 The Comparative Study**

A unique feature of Leaving Cert English is the "Comparative Study," where students compare three texts (e.g., a novel, a play, and a film) under a specific mode (e.g., "General Vision and Viewpoint").

* **Graph Structure:** The graph needs a "Cross-Text" layer.  
  * :Text {title: "Philadelphia, Here I Come\!"} \----\> :Theme {name: "Isolation"}.  
  * :Text {title: "The Shawshank Redemption"} \----\> :Theme {name: "Hope"}.  
* **Synthesis Engine:** The AI must be able to retrieve nodes from multiple texts simultaneously and identify "Contrast" or "Similarity" edges. The prompt to the LLM would be constructed by pulling the "Isolation" node from Text A and the "Hope" node from Text B and asking the model to synthesize a comparison based on the "General Vision and Viewpoint" criteria.

### **4.3 Modern Foreign Languages (French, German, Spanish)**

These follow the Gaeilge structure but with a stronger emphasis on "Reading Comprehension."

* **Contextual Retrieval:** The system needs a vast corpus of target-language texts (newspaper articles, literary excerpts) indexed by *difficulty level*.  
* **Dynamic generation:** Using the BAML extraction logic, the system can scrape current news (e.g., Le Monde), extract an article, and dynamically generate "Leaving Cert Style" questions (Find the synonym, Answer in English) based on the patterns learned from the 10-year archive of past papers stored in FalkorDB.

## **5\. Domain Analysis: The Business Group (Business, Accounting, Economics)**

These subjects require high precision and specific formatting (Balance Sheets, Ledgers).

### **5.1 Accounting: The Double-Entry Graph**

Accounting is, structurally, a graph problem. Every transaction is an edge between two accounts (Nodes).

* **Graph Logic:**  
  * :Account {name: "Bank"}.  
  * :Account {name: "Sales"}.  
  * Transaction: Debit Bank, Credit Sales.  
* **Error Detection:** The backend can model a student's answer as a set of graph transactions. If the graph does not "balance" (Sum of Debits\!= Sum of Credits), the system can traverse the graph to find the specific node (account) where the error originated.  
* **Table Extraction:** The "Layout Analysis" in Cocoindex 1 is critical here. Accounting questions are effectively massive tables. BAML must preserve the row/column structure perfectly to allow for cell-by-cell grading.

### **5.2 Business: The Structured Long Answer**

Business requires "Structured Answers" (State, Explain, Example).

* **Marking Scheme Logic:** "2 marks for stating, 2 for explaining, 1 for example."  
* **Segmentation:** The backend must parse the student's paragraph into these three components.  
  * *Sentence 1:* "Delegation is assigning duties." (State \- Match).  
  * *Sentence 2:* "This reduces manager workload." (Explain \- Match).  
  * *Sentence 3:* "e.g., A manager asks a supervisor to do the roster." (Example \- Match).  
* If the "Example" component is missing, the system awards 4/5 marks, citing the specific missing structural element.

## **6\. Advanced Data Ingestion: The BAML Specification**

The quality of the tutoring system is entirely dependent on the fidelity of the data extracted from the raw exam papers. The standard BAML schema for Math 1 must be diversified.

### **6.1 The Universal Assessment Item Schema**

We define a polymorphic BAML class structure that can handle any subject.

Code snippet

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
    strand\_ref: string  
    topic\_tags: string  
      
    // Polymorphic Content Fields  
    text\_content: string?  
    image\_assets: ImageAsset?  
    audio\_assets: AudioAsset?  
    table\_data: TableData?  
      
    // The "Ground Truth"  
    marking\_scheme\_ref: string  
}

class ImageAsset {  
    url: string  
    description: string @description("Detailed alt-text generated by Vision Model")  
    type: "Map" | "Diagram" | "Photo" | "Chart"  
}

class MarkingSchemeLogic {  
    scale\_label: string @description("e.g., Scale 10C or SRP")  
    criteria: string @description("The specific points required")  
    model\_answer: string  
}

### **6.2 Handling "The Project" (Coursework)**

Many subjects (History, Geog, Ag Science, Politics) have a coursework component (20%). The ingestion pipeline must handle "Briefs."

* **Brief Extraction:** The SEC releases a "Brief" each year (e.g., "Research a local historical event").  
* **Constraint Modeling:** BAML must extract the constraints: "Word count: 1500", "Must use 3 sources", "Must have an evaluation section."  
* **Validation:** These constraints are stored as properties on the Assignment node. The student's submission is validated against these properties *before* semantic grading begins.

## **7\. The Persistence Layer: FalkorDB Configuration for Scale**

Scaling from 1 subject to 30 requires a rethinking of the FalkorDB schema and indexing strategy.

### **7.1 Namespace Partitioning vs. Unified Graph**

Should all subjects live in one graph?

* **Decision:** A **Unified Graph** with **Namespace Partitioning** is superior.  
* **Reasoning:** Interdisciplinary links. A "Unified Graph" allows us to query the relationship between "The Famine" (History) and "Potato Blight" (Biology).  
* **Implementation:**  
  * Labels are prefixed: :History:Event, :Biology:Organism.  
  * Indices are partitioned: CALL db.idx.fulltext.createNodeIndex('History:Event', 'description').

### **7.2 The "Curriculum Common Node" Strategy**

To facilitate cross-subject intelligence, we introduce "Bridge Nodes."

* **Concept:** Identify concepts that appear in multiple subjects.  
  * "Statistics" (Math, Biology, Geography).  
  * "Energy" (Physics, Chemistry, Biology, Geog).  
* **Linkage:** We merge these into single "Super-Nodes" or create explicit SAME\_AS edges between :Physics:Energy and :Biology:Energy.  
* **Benefit:** If a student masters "Energy" in Physics, the system can infer a higher probability of competence in the "Energy" topic in Biology, adjusting the difficulty curve accordingly.

## **8\. Temporal Dynamics and Student Modeling (Graphiti)**

The "Agentic Memory" 1 becomes the student's "Digital Twin."

### **8.1 Cognitive Load Balancing**

With 7 subjects, the student's mastery graph is complex. Graphiti must track "Cognitive Fatigue."

* **Mechanism:**  
  1. Track session duration and error rates per subject.  
  2. If error rates spike in "Math" (High Load) after 40 minutes, the system recommends switching to "English" (Different Load).  
* **Edge Properties:** :Student \----\> :Math:Topic.

### **8.2 Spaced Repetition across the Curriculum**

The system must optimize the "Forgetting Curve" across 30 distinct strands (approx. 5 strands \* 6 subjects).

* **Algorithm:** The backend calculates the "Optimal Review Time" for every topic.  
* **Conflict Resolution:** If "Math:Calculus" and "Biology:Genetics" are both due for review, which takes precedence?  
* **Priority Logic:** The logic leverages the exam weightings. If Calculus is worth 15% of the Math grade and Genetics is 5% of Biology, Calculus wins. This weighting data is extracted from the Syllabus PDFs via BAML and stored as node properties.

## **9\. Infrastructure and Deployment Strategy**

### **9.1 The Cocoindex Flow Orchestration**

We cannot run a single Cocoindex flow for all subjects. We need a "Subject Router."

* **Source:** Watcher monitors /data/raw.  
* **Router:** A Classifier detects the subject based on filename or content.  
* **Sub-Flows:**  
  * Flow\_Math: OCR \-\> BAML (LaTeX) \-\> FalkorDB.  
  * Flow\_Language: OCR \-\> Audio Transcribe \-\> BAML (PCLM) \-\> FalkorDB.  
  * Flow\_Visual: OCR \-\> Layout Analysis \-\> Vision Model \-\> BAML \-\> FalkorDB.  
    This ensures that compute-heavy resources (Vision Models) are only invoked when necessary.

### **9.2 The "Live" Update Cycle**

The research mentions "Live Updates".1 During the exam season (June), the SEC releases papers daily.

* **Real-Time Ingestion:** The Cocoindex FlowLiveUpdater 1 is critical. As soon as "Paper 1" is scanned and dropped into the bucket, it must be ingested, solved (by the AI), and indexed within minutes.  
* **Crowdsourced Corrections:** The graph should allow for "Flagging." If a student or teacher disputes a Marking Scheme interpretation, the system creates a :Dispute node linked to the question, which human reviewers can assess, updating the graph edge if necessary.

## **10\. Conclusion**

The expansion of the Irish Leaving Certificate AI Tutoring System from a Mathematics-only pilot to a pan-curricular infrastructure is a task of immense ontological and technical complexity. It requires moving beyond the relatively clean, derivation-based logic of Mathematics into the messy, subjective, and multimodal worlds of the Sciences, Humanities, and Languages.  
The proposed architecture handles this by:

1. **Generalizing the "Strand" Metamodel:** Using the NCCA's own structure as the root ontology.  
2. **Specializing the Extraction Layer:** Using polymorphic BAML schemas to handle everything from poems to balance sheets.  
3. **Enhancing the Graph Logic:** Introducing temporal, spatial, and causal edges to FalkorDB.  
4. **Deepening the Student Model:** Using Graphiti to track mastery and fatigue across the entire curriculum.

By rigorously applying these principles, the system can provide a "Unified Educational Theory" in code, guiding the student not just through one subject, but through the interconnected web of knowledge that constitutes the Senior Cycle.

## **Table 1: Summary of Technical Requirements by Subject Group**

| Feature | Mathematics | Experimental Sciences | Humanities (History/Geog) | Languages | Applied (Business/Eng) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **Ontology Model** | Derivation Tree | Taxonomy & System | Causal & Spatial Graph | Thematic & Semantic Web | Transaction & Structural |
| **BAML Focus** | LaTeX, Formulas | Diagrams, Taxonomies | SRPs, DBQs, Time | PCLM, Sentiment, Dialect | Tables, Briefs, CAD |
| **Key Edge Types** | :PREREQUISITE | :FLOWS\_TO, :INTERACTS | :CAUSED, :LOCATED\_AT | :EXPLORES, :TRANSLATES | :DEBITS, :ASSEMBLES |
| **Assessment Logic** | Step-Based (Scale) | Keyword/Hit-Count | SRP Count / Argument | Rubric (Subjective) | Exact Layout / Values |
| **Data Modality** | Text \+ Symbolic | Text \+ Diagram | Text \+ Map \+ Image | Text \+ Audio | Text \+ Table \+ Drawing |
| **Graphiti Usage** | Syllabus Versions | Scientific Updates | Historical Time \+ Syllabus | Literary Eras | Economic Cycles |
| **Cross-Subject** | Physics, Chem | Math, Ag Science | Politics, English | History, Classics | Math, Geography |

#### **Works cited**

1. Backend%20Strategy%20For%20Educational%20Tutoring%20System.pdf.pdf