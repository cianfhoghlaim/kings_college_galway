# **Backend Architecture Strategy for a Bilingual Temporal Knowledge Graph in Mathematics Education**

## **1\. Architectural Imperatives and Domain Analysis**

The design and implementation of a bilingual AI tutoring system for the Irish mathematics curriculum necessitates a backend architecture of significant complexity and nuance. The challenge is not merely technical but deeply rooted in the structural and pedagogical realities of the Irish education system. To build a system that is pedagogically valid and technically robust, one must first deconstruct the domain data provided by the National Council for Curriculum and Assessment (NCCA), the State Examinations Commission (SEC), and the Department of Education. This section analyzes the educational landscape to establish the functional requirements for the proposed technology stack: BAML, Cocoindex, Cognee, Graphiti, and FalkorDB.

### **1.1 The Structural Hierarchy of the Irish Mathematics Curriculum**

The primary data source for the system's ontology is curriculumonline.ie. The curriculum is not a monolithic entity but a stratified, hierarchical system that evolves over time. The backend must explicitly model these layers to function effectively as a tutor.

#### **1.1.1 The Senior Cycle Architecture**

The Senior Cycle (Leaving Certificate) mathematics syllabus is the terminal point of secondary education and the highest-stakes assessment in the system. According to the specific syllabus documentation, the course is designed as a 180-hour program of study.1 Crucially, the syllabus is divided into five distinct "strands" that form the high-level parent nodes of our proposed knowledge graph.  
**Table 1: Senior Cycle Strand Structure and Content Analysis**

| Strand ID | Strand Name | Key Content Areas | Ontological Implications |
| :---- | :---- | :---- | :---- |
| **Strand 1** | Statistics and Probability | Counting, concepts of probability, outcomes, statistical inference. | Requires nodes for stochastic processes and statistical data types. |
| **Strand 2** | Geometry and Trigonometry | Synthetic geometry, co-ordinate geometry, trigonometry, transformation geometry. | Needs spatial reasoning attributes and linkage to visual data processing. |
| **Strand 3** | Number | Number systems, arithmetic, complex numbers, length, area, volume. | Foundational nodes that map to all other strands; high interconnectivity. |
| **Strand 4** | Algebra | Expressions, equations, inequalities, calculus (differentiation/integration). | The logic engine of the graph; heavily referenced by Functions and Geometry. |
| **Strand 5** | Functions | Function concepts, graphing functions, exponential/logarithmic functions. | Directly dependent on Algebra; temporal links to calculus development. |

The syllabus explicitly states that "the strand structure of the syllabus should not be taken to imply that topics are to be studied in isolation".1 This directive is a critical architectural constraint. A hierarchical tree structure (Strand \-\> Topic) is insufficient; the backend requires a graph structure where lateral edges (e.g., , ) connect nodes across strands. For instance, "Complex Numbers" (Strand 3\) utilizes "Trigonometry" (Strand 2\) for polar form representation. The ontology managed by Cognee must enforce these cross-strand relationships to allow the AI tutor to diagnose cross-domain learning gaps.

#### **1.1.2 The Junior Cycle and Bridging Frameworks**

The Junior Cycle operates under a similar five-strand structure, designed to allow a smooth transition to the Senior Cycle.2 However, the ontology must account for the "Bridging Framework," a specific pedagogical tool designed to link Primary school mathematics to the Junior Cycle.2  
The architecture must support a "pathway analysis" capability. If a Senior Cycle student struggles with "Coordinate Geometry," the system must be able to traverse the graph backward:

1. **Current Node:** Senior Cycle Geometry (The Line/Circle).  
2. **Prerequisite Node:** Junior Cycle Coordinate Geometry (Slopes/Midpoints).  
3. **Foundational Node:** Primary School Shape and Space.3

The Common Introductory Course in the Junior Cycle ensures that all strands are engaged with to some extent in the first year.2 This creates a temporal dependency in the graph: specific nodes must be marked as "First Year Content," allowing Graphiti to sequence the learning path chronologically.

### **1.2 The Bilingual Requirement: Linguistic Duality**

The mandate for a bilingual system (Irish/English) introduces significant complexity. The curriculum is delivered in distinct educational contexts: T1 (Irish-medium) and T2 (English-medium) schools.4 While the mathematical concepts are universal, their lexical representation and the resources used to teach them are distinct.

#### **1.2.1 Conceptual Unity vs. Lexical Divergence**

The architecture must avoid creating two disconnected graphs (one English, one Irish). Instead, it must employ a "Unified Concept Node" strategy. A node representing "The Theorem of Pythagoras" must exist as a single entity in the FalkorDB store, with properties supporting both languages.

* **English Resource:** "The square of the hypotenuse..."  
* **Irish Resource:** "An chearnóg ar an taobhagán...".5

The system must handle specific Irish language nuances found in the research material. For instance, older resources or primary school materials may use specific fonts (Cló Gaelach) or localized URL patterns.6 The data ingestion pipeline (Cocoindex) must be robust enough to normalize these variations into standard UTF-8 text while preserving the original linguistic integrity for display.

### **1.3 Statistical Context and System Scaling**

Data from gov.ie regarding education statistics provides the context necessary for system scaling and prioritization. The Department of Education publishes detailed projections of full-time enrolment and retention rates.7

* **Retention Rates:** High retention rates to the Leaving Certificate (approx 87.7%) 8 indicate that the system must be optimized for the Senior Cycle workload, as the vast majority of students progress to this level.  
* **Demographic Projections:** Reports on enrolment projections (2021-2040) 7 suggest a predictable load on the system. The architecture can leverage this data to pre-compute "Community Summaries" (a feature of Graphiti/GraphRAG) for the most heavily populated course levels, ensuring low-latency responses for the majority of users.

### **1.4 Assessment Architecture: The "Ground Truth"**

The ultimate source of truth for the tutoring system is not the textbook, but the examination paper. The State Examinations Commission (SEC) releases papers and, crucially, **Marking Schemes**.9 These marking schemes are highly structured, using codes like "Scale 10C" or "Scale 5B" to denote how marks are awarded.10  
Table 2: Marking Scheme Logic 11

| Scale Label | Categories | Description | Meaning for AI Backend |
| :---- | :---- | :---- | :---- |
| **A** | 2 | Correct / Incorrect | Binary classification of student answer. |
| **B** | 3 | Correct / Partial / Incorrect | Needs logic to identify "step" validity. |
| **C** | 4 | High/Mid/Low Partial Credit | Complex reasoning chain verification required. |
| **D** | 5 | Granular step marking | Deep step-by-step analysis required. |

The backend must extract these schemas using BAML and store them in the graph linked to the question. This allows the tutor not just to say "Wrong," but to say "You achieved Low Partial Credit because you identified the formula, but failed to substitute correctly," mirroring the exact logic of the state examiner.

## ---

**2\. Ontological Engineering with Cognee**

Cognee is the designated technology for managing the "Semantic Memory" of the system. While the raw data resides in FalkorDB, Cognee provides the structural enforcement layer, ensuring that the graph adheres to a rigorous educational ontology. This section details the design of the maths\_curriculum.owl ontology and its integration via Cognee.

### **2.1 Theoretical Basis: The Educational Knowledge Graph**

An educational knowledge graph differs from a generic informational graph. It must model **Pedagogical Content Knowledge (PCK)**—not just *what* the math is, but *how* it is taught and assessed. The ontology must capture three dimensions:

1. **Epistemological:** The structure of mathematics (Algebra relies on Arithmetic).  
2. **Curricular:** The rules of the NCCA (Strands, Levels, Learning Outcomes).  
3. **Assessment:** The rules of the SEC (Questions, Marking Schemes, Grades).

### **2.2 RDF/OWL Ontology Specification**

Cognee supports the ingestion of RDF/OWL files to define the graph schema.12 We will define a custom ontology http://www.mathstutor.ie/ontology/curriculum\# (prefix maths:).

#### **2.2.1 Core Class Hierarchy**

The ontology is rooted in the structure identified in the Domain Analysis.

Code snippet

@prefix maths: \<http://www.mathstutor.ie/ontology/curriculum\#\>.  
@prefix rdfs: \<http://www.w3.org/2000/01/rdf-schema\#\>.  
@prefix owl: \<http://www.w3.org/2002/07/owl\#\>.

\# Core Educational Entities  
maths:EducationalEntity a owl:Class.

\# The Syllabus Structure  
maths:Cycle a owl:Class ; rdfs:subClassOf maths:EducationalEntity.  
maths:JuniorCycle a owl:Class ; rdfs:subClassOf maths:Cycle.  
maths:SeniorCycle a owl:Class ; rdfs:subClassOf maths:Cycle.

maths:Strand a owl:Class ; rdfs:subClassOf maths:EducationalEntity.  
maths:Topic a owl:Class ; rdfs:subClassOf maths:EducationalEntity.  
maths:LearningOutcome a owl:Class ; rdfs:subClassOf maths:EducationalEntity.

\# Assessment Entities  
maths:AssessmentItem a owl:Class.  
maths:ExamQuestion a owl:Class ; rdfs:subClassOf maths:AssessmentItem.  
maths:MarkingScheme a owl:Class ; rdfs:subClassOf maths:AssessmentItem.

#### **2.2.2 Modeling Level Stratification**

The research indicates that Ordinary Level is a subset of Higher Level.1 This is modeled via object properties and transitive relationships in OWL.

Code snippet

maths:Level a owl:Class.  
maths:Foundation a maths:Level.  
maths:Ordinary a maths:Level.  
maths:Higher a maths:Level.

maths:validForLevel a owl:ObjectProperty ;  
    rdfs:domain maths:LearningOutcome ;  
    rdfs:range maths:Level.

\# Inclusion Logic  
maths:includesOutcome a owl:TransitiveProperty.  
\# Rule: Higher Level includes all Ordinary Level outcomes

### **2.3 Cognee Integration and Graph Construction**

Cognee acts as the bridge between this static OWL definition and the dynamic data extracted from PDFs.

#### **2.3.1 Configuration of the Cognee Pipeline**

To enforce this ontology, the cognify function in Cognee is configured with the ontology path. This ensures that as data is processed, it is "grounded" against the defined classes.

Python

import cognee

async def build\_semantic\_graph():  
    \# Configure the hybrid adapter for FalkorDB \[13\]  
    from cognee.infrastructure.databases.vector.falkordb.FalkorDBAdapter import FalkorDBAdapter  
      
    \# Run the cognification process   
    await cognee.cognify(  
        datasets=\["leaving\_cert\_curriculum", "exam\_papers"\],  
        ontology\_file\_path="./ontologies/maths\_curriculum.owl",   
        include\_disjoint\_checks=True \# Ensure strict adherence to class boundaries  
    )

#### **2.3.2 NodeSet Management**

Cognee organizes data into NodeSets.12 In this architecture, we define NodeSets for each Strand.

* NodeSet("Algebra")  
* NodeSet("Geometry")  
  This partitioning allows for efficient sub-graph retrieval. When a student asks about "Algebra," the system retrieves the entire NodeSet("Algebra") from FalkorDB, which includes all related topics, theorems, and exam questions, without traversing unrelated nodes like "Statistics."

### **2.4 Hybrid Search: The FalkorDB Adapter**

Cognee's strength lies in its ability to fuse vector search with graph traversal.14 The cognee-community-hybrid-adapter-falkor is the critical component here.  
**Mechanism:**

1. **Ingestion:** When Cognee processes a chunk of text (e.g., a definition of a function), it generates an embedding.  
2. **Storage:** It stores the text as a Node in FalkorDB (:DataPoint) and the embedding in a vector index on that node.  
3. **Linking:** It creates edges to the ontological nodes (:Topic).  
   * (:DataPoint {text: "Definition of Injection..."}) \--\> (:Topic {name: "Injective Function"})

This allows for **Dual-Pathway Retrieval**:

* *Pathway A (Vector):* "Explain one-to-one functions." \-\> Finds the DataPoint via vector similarity.  
* *Pathway B (Graph):* "What topics are related to Injective Functions?" \-\> Traverses from the Topic node to find parent concepts (Functions) or related exam questions.

## ---

**3\. High-Fidelity Data Extraction via BAML**

The quality of the Knowledge Graph is entirely dependent on the quality of the data ingestion. The source materials—PDFs of exam papers and marking schemes—are unstructured and highly formatted. Standard extraction techniques fail to capture the semantic structure of a maths question (e.g., the difference between part (a) and part (b), or the specific layout of a marking scheme table). BAML (Boundary Abstract Markup Language) is selected for its ability to define rigid schemas for LLM extraction.16

### **3.1 BAML Strategy for Mathematical Content**

BAML allows us to treat the extraction process as a strictly typed function call. Instead of prompting an LLM with "Extract the questions," we define a BAML function ExtractExamPaper that returns a strictly typed ExamPaper object.

#### **3.1.1 The ExamQuestion BAML Schema**

We must define a BAML class that mirrors the hierarchical structure of Irish exam questions (Question \-\> Part \-\> Sub-part).18

Code snippet

// BAML Definition for Irish Math Questions

enum QuestionSection {  
    SectionA  
    SectionB  
}

class SubPart {  
    label: string @description("The label, e.g., (i), (ii)")  
    content: string @description("The text content. Preserve LaTeX for math.")  
    marks: int? @description("Marks allocated if explicitly stated")  
}

class QuestionPart {  
    label: string @description("The label, e.g., (a), (b)")  
    content: string  
    subparts: SubPart  
}

class ExamQuestion {  
    number: string @description("e.g., Question 1")  
    section: QuestionSection  
    parts: QuestionPart  
    topic\_tags: string @description("Relevant topics from the syllabus")  
    is\_higher\_level: bool  
}

function ExtractQuestions(text: string) \-\> ExamQuestion {  
    client GPT4o  
    prompt \#"  
        Extract all mathematical questions from the following text.  
        Ensure that mathematical formulas are converted to LaTeX format.  
        Preserve the hierarchy of parts (a, b) and sub-parts (i, ii).  
          
        Text:  
        {{ text }}  
    "\#  
}

This BAML code compiles into a Python client using Pydantic models.19 When the extraction runs, the Pydantic validation ensures that every extracted question adheres to this structure. If the LLM hallucinates a structure that doesn't fit, the validation fails, ensuring data integrity.

### **3.2 Extracting Marking Schemes**

The Marking Schemes are arguably more valuable than the questions themselves for a tutoring system. They contain the logic of assessment. The research snippets show that marking schemes use specific "Scales".10 We need a specialized BAML extractor for these.

Code snippet

class ScaleCriteria {  
    credit: "Low Partial" | "Mid Partial" | "High Partial" | "Full"  
    value: int  
    description: string @description("The specific requirement, e.g., 'Correct formula substituted'")  
}

class MarkingScale {  
    label: string @description("e.g., Scale 10C")  
    total\_marks: int  
    breakdown: int @description("e.g., ")  
    criteria: ScaleCriteria  
}

class QuestionScheme {  
    question\_ref: string  
    scales: MarkingScale  
    model\_solution: string @description("The worked solution provided in the scheme")  
}

By extracting the ScaleCriteria descriptions ("Work of merit," "One step correct"), the system can later evaluate a student's answer against these specific text descriptors using semantic similarity.

### **3.3 Bilingual Extraction Strategy**

BAML is language-agnostic but prompt-sensitive. For Irish language papers (T1 schools), we define parallel BAML functions that utilize Irish prompts to ensure better context understanding by the LLM.

Code snippet

function ExtractQuestionsGa(text: string) \-\> ExamQuestion {  
    client GPT4o  
    prompt \#"  
        Bain úsáid as an téacs seo a leanas ó pháipéar scrúdaithe Gaeilge.  
       ...  
    "\#  
}

Crucially, the return type is still ExamQuestion. This forces the Irish data into the exact same schema as the English data, facilitating the unified ontology in FalkorDB.

## ---

**4\. Temporal Dynamics and Episodic Memory with Graphiti**

The Irish education system is dynamic. Syllabi change (e.g., the rollout of "Project Maths" from 2010-2015), exam trends shift, and crucially, the student's own knowledge state evolves. A static database cannot capture this. Graphiti is integrated to provide **Bi-Temporal Knowledge Graph** capabilities.21

### **4.1 Theory of Bi-Temporal Data in Education**

Graphiti tracks two time dimensions for every edge in the graph:

1. **Valid Time (valid\_at, invalid\_at):** The real-world time period during which a fact is true.  
2. **Transaction Time (created\_at, expired\_at):** The time when the system recorded the fact.

#### **4.1.1 Syllabus Versioning**

The syllabus is not constant. Topics are added and removed. For example, "Matrices" might have been on the syllabus in 2005 but removed in 2015\.

* **Graph Edge:** (:Topic {name: "Matrices"}) \--\> (:Curriculum {name: "Leaving Cert"})  
* **Temporal Properties:**  
  * valid\_at: 1990-01-01  
  * invalid\_at: 2015-01-01

When the tutoring system queries the graph for "Current Topics," Graphiti automatically filters edges where now() falls within the validity window.22 This prevents the system from tutoring students on obsolete material. Conversely, if a student is practicing on "Past Papers" from 2010, the system can use Graphiti’s "Time Travel" feature to switch the graph context to 2010, making "Matrices" valid again for that specific session.

### **4.2 Episodic Memory: Exam Papers as Events**

Graphiti treats data ingestion as "Episodes".21 Each exam paper is treated as a distinct episode in the history of the curriculum.

* **Episode Entity:** ExamPaper\_2023\_Paper1  
* **Timestamp:** 2023-06-09  
* **Content:** The graph nodes (Questions) extracted from that paper.

This allows for longitudinal analysis. The system can answer queries like: "How has the frequency of Calculus questions changed over the last 10 years?" by aggregating data across episodes sorted by their valid\_at dates.

### **4.3 Agentic Memory: The Student Model**

The "User" in this system is not just a query source but a dynamic entity in the graph. Graphiti is designed for "Agentic Memory" 23, allowing the system to maintain a stateful representation of the student's knowledge.

#### **4.3.1 Dynamic Mastery Tracking**

We can model student mastery using temporal edges.

* **Event:** Student answers a question on "Complex Numbers" correctly.  
* **Graph Update:** Graphiti adds an edge: (:Student) \--\> (:Topic {name: "Complex Numbers"}).  
* **Validity:** This mastery is not permanent. We can set a valid\_at timestamp of now().

By analyzing the valid\_at timestamps of the \`\` edges, the system can implement **Spaced Repetition**. If the edge is 3 months old, the system infers that the "Mastery" might have decayed and prompts a revision question.

### **4.4 Custom Entity Definition via Pydantic**

Graphiti allows the definition of Custom Entity Types using Pydantic models.24 This ensures that the nodes created in the temporal graph are domain-specific.

Python

from pydantic import BaseModel, Field

class EducationalStandard(BaseModel):  
    code: str \= Field(description="The NCCA curriculum code")  
    description: str  
    year\_introduced: int

\# Registering the custom type with Graphiti   
graphiti.set\_ontology(entities={"Standard": EducationalStandard})

This deep integration ensures that Graphiti doesn't just store "nodes" but stores "Standards," "Questions," and "Students" with their specific attributes and constraints.

## ---

**5\. Pipeline Orchestration via Cocoindex**

While BAML handles extraction and Graphiti handles temporal logic, **Cocoindex** provides the orchestration layer that moves data through the system. Cocoindex employs a **Dataflow programming model** 25, treating data processing as a directed acyclic graph (DAG) of transformations. This is superior to script-based ETL for this use case because it supports incremental processing—critical when dealing with a constantly growing archive of exam papers.

### **5.1 The Maths Processing Flow**

We define a unified processing flow MathsTutorFlow that handles the ingestion of diverse data sources.  
**Table 3: Cocoindex Flow Stages**

| Stage | Operation | Tool/Method | Description |
| :---- | :---- | :---- | :---- |
| **Source** | LocalFile | Cocoindex Source | Watches directories for new PDFs (Syllabus, Exams). |
| **Transform 1** | Layout Analysis | Custom Function | Uses OCR to preserve spatial layout (vital for geometry questions). |
| **Transform 2** | Structuring | BAML Adapter | Calls the BAML client to convert text to Pydantic models. |
| **Transform 3** | Embedding | SentenceTransformer | Generates multilingual vectors for hybrid search. |
| **Transform 4** | Graph Construction | Custom Connector | Maps Pydantic models to Cypher queries for FalkorDB. |

### **5.2 Incremental Processing and Live Updates**

Cocoindex’s FlowLiveUpdater 27 allows the system to run in a "Live" mode. It monitors the source directories. If a new marking scheme is published by the SEC and dropped into the folder, Cocoindex detects the file change.

* **Mechanism:** It computes a hash of the file. If changed, it triggers the flow *only* for that file.  
* **Benefit:** The knowledge graph is updated in near real-time without requiring a complete re-indexing of the 10-year archive. This is essential for maintaining uptime during the exam season when new information (e.g., corrections) might be released.

### **5.3 Custom Target: The FalkorDB Connector**

Cocoindex does not have a native FalkorDB target out of the box, but it allows for Custom Targets.28 We must implement a TargetConnector that defines how the data flowing through the pipeline is written to the database.

#### **5.3.1 Implementation Specification**

The connector requires two components: a TargetSpec (configuration) and a TargetConnector (logic).

Python

import cocoindex.op  
from falkordb import FalkorDB

\# 1\. Define the Configuration \[28\]  
class FalkorDBTargetSpec(cocoindex.op.TargetSpec):  
    host: str \= "localhost"  
    port: int \= 6379  
    graph\_name: str \= "MathsGraph"

\# 2\. Define the Connector Logic \[29\]  
@cocoindex.op.target\_connector(spec\_cls=FalkorDBTargetSpec)  
class FalkorDBConnector:  
    @staticmethod  
    def mutate(batch):  
        \# Establish connection  
        client \= FalkorDB(host=spec.host, port=spec.port)  
        graph \= client.select\_graph(spec.graph\_name)  
          
        for item in batch:  
            \# Construct Cypher Query  
            \# Using parameterized queries for performance \[30\]  
            query \= """  
            MERGE (q:Question {id: $id})  
            SET q.text \= $text, q.embedding \= $embedding  
            """  
            graph.query(query, params=item.dict())

This connector ensures that the Pydantic models generated by BAML are efficiently serialized and upserted into FalkorDB using optimal Cypher patterns.

## ---

**6\. The Persistence Layer: FalkorDB**

FalkorDB is the unified persistence layer for the architecture. It is a high-performance graph database backed by Redis.31 Its selection is driven by its ability to support the hybrid requirements of the system: storing the complex graph structure maintained by Cognee and Graphiti, while providing the vector similarity search required for RAG.

### **6.1 Schema Design**

The schema in FalkorDB is the physical manifestation of the ontology defined in Section 2\.  
**Node Labels:**

* Topic: Represents abstract mathematical concepts.  
* Question: Represents specific assessment items.  
* MarkingScheme: Represents the grading logic.  
* Student: Represents the user.

**Edge Types:**

* \`\`: Connects a Question to a Topic.  
* \`\`: Connects Topic to Topic (e.g., Algebra \-\> Calculus).  
* \`\`: Connects Question to MarkingScheme.  
* \`\`: Connects Student to Question.

### **6.2 Indexing Strategy**

To support low-latency querying, we must deploy specific indexes using FalkorDB's command set.32

1. **Vector Index:** Created on the embedding property of Question nodes.  
   * GRAPH.QUERY MathsGraph "CALL db.idx.vector.createNodeIndex('Question', 'embedding', 'FLOAT32', 6, 'L2')"  
   * This enables the "Find similar questions" feature.  
2. **Full-Text Index:** Created on the text property of Topic and Question nodes.  
   * GRAPH.QUERY MathsGraph "CALL db.idx.fulltext.createNodeIndex('Question', 'text')"  
   * This enables keyword search (e.g., searching for specific terms like "Tetrahedron").  
3. **Constraint Enforcement:** We use GRAPH.CONSTRAINT CREATE 32 to ensure data integrity, for example, enforcing that every Question node must have a unique ID.

### **6.3 Hybrid Search Implementation**

The "GraphRAG" (Retrieval Augmented Generation) pattern is implemented directly in FalkorDB. When a student asks a question, the system performs a multi-step retrieval 33:

1. **Vector Step:** CALL db.idx.vector.queryNodes('Question', 'embedding', $vec, 5\) \-\> Retrieves the 5 most semantically similar questions.  
2. **Graph Step:** From those 5 questions, traverse the \`\` edges to find the related Topic nodes.  
3. **Context Assembly:** The system aggregates the text of the similar questions AND the definitions of the related topics to form the prompt for the LLM.

This approach ensures that the AI tutor's response is grounded not just in the text of similar questions, but in the theoretical framework of the curriculum.

## ---

**7\. Integration and Deployment Strategy**

The proposed architecture is a sophisticated assembly of specialized tools. The deployment strategy must ensure these components operate in harmony.

### **7.1 Infrastructure Requirements**

* **FalkorDB:** Deployed via Docker or Kubernetes (falkordb/falkordb:edge).33  
* **Pipeline Host:** A Python environment hosting the Cocoindex flow and Cognee/Graphiti logic.  
* **LLM Provider:** Integration with OpenAI (via BAML) for extraction and embedding generation.

### **7.2 Roadmap**

1. **Phase 1: Ontology & BAML:** Define the .owl file and .baml classes. This establishes the data contract.  
2. **Phase 2: Ingestion Pipeline:** Build the Cocoindex flow. Process the static curriculum PDFs first to populate the Topic nodes.  
3. **Phase 3: Assessment Ingestion:** Process the archive of Exam Papers and Marking Schemes. This populates the Question nodes and links them to Topics.  
4. **Phase 4: Temporal Layer:** Activate Graphiti. Define the temporal validity of syllabus topics (Project Maths rollout).  
5. **Phase 5: Student Interaction:** Deploy the API layer that allows students to query the system, triggering Agentic Memory updates in the graph.

### **7.3 Conclusion**

This research report outlines a comprehensive strategy for building a bilingual, temporally aware mathematics tutoring backend. By leveraging BAML for structured extraction, Cognee for ontological rigor, Graphiti for temporal logic, Cocoindex for pipeline orchestration, and FalkorDB for high-performance storage, the system addresses the specific, complex requirements of the Irish education system. It transforms the static archives of the State Examinations Commission into a dynamic, intelligent, and responsive educational engine.

#### **Works cited**

1. syllabus \- Curriculum Online, accessed December 3, 2025, [https://www.curriculumonline.ie/getmedia/f6f2e822-2b0c-461e-bcd4-dfcde6decc0c/SCSEC25\_Maths\_syllabus\_examination-2015\_English.pdf](https://www.curriculumonline.ie/getmedia/f6f2e822-2b0c-461e-bcd4-dfcde6decc0c/SCSEC25_Maths_syllabus_examination-2015_English.pdf)  
2. Junior Certificate Mathematics Syllabus \- Curriculum Online, accessed December 3, 2025, [https://www.curriculumonline.ie/getmedia/4f6cba68-ac41-485c-85a0-32ae6c3559a7/JCSEC18\_Maths\_Examination-in-2016.pdf](https://www.curriculumonline.ie/getmedia/4f6cba68-ac41-485c-85a0-32ae6c3559a7/JCSEC18_Maths_Examination-in-2016.pdf)  
3. Primary School Curriculum Curaclam na Bunscoile, accessed December 3, 2025, [https://www.curriculumonline.ie/getmedia/b59c3b53-f81f-4f04-9b09-533912674f00/1999\_Primary\_School\_Mathematics\_Curriculum.pdf](https://www.curriculumonline.ie/getmedia/b59c3b53-f81f-4f04-9b09-533912674f00/1999_Primary_School_Mathematics_Curriculum.pdf)  
4. Gaeilge | Curriculum Online, accessed December 3, 2025, [https://curriculumonline.ie/junior-cycle/junior-cycle-subjects/gaeilge/](https://curriculumonline.ie/junior-cycle/junior-cycle-subjects/gaeilge/)  
5. DRÉACHT-THREOIRLÍNTE DO MHÚINTEOIRÍ \- Curriculum Online, accessed December 3, 2025, [https://curriculumonline.ie/getmedia/6c884c53-66cb-45ad-bd55-e95418e74fb0/SCSEC20\_history\_guidelines\_gaeilge.pdf](https://curriculumonline.ie/getmedia/6c884c53-66cb-45ad-bd55-e95418e74fb0/SCSEC20_history_guidelines_gaeilge.pdf)  
6. Information and Communications Technology ICT in the Primary School Curriculum Guidelines for Teachers, accessed December 3, 2025, [https://www.curriculumonline.ie/getmedia/4adfbc22-f972-45a1-a0ba-d1864c69dff2/ICT-Guidelines-Primary-Teachers.pdf](https://www.curriculumonline.ie/getmedia/4adfbc22-f972-45a1-a0ba-d1864c69dff2/ICT-Guidelines-Primary-Teachers.pdf)  
7. Education statistics, accessed December 3, 2025, [https://www.gov.ie/en/department-of-education/publications/education-statistics/](https://www.gov.ie/en/department-of-education/publications/education-statistics/)  
8. Monitoring Ireland's Skills Supply 2012, accessed December 3, 2025, [https://www.skillsireland.ie/media/0s1fv4k3/egfsn25072012-monitoring-irelands-skills-supply-publication.pdf](https://www.skillsireland.ie/media/0s1fv4k3/egfsn25072012-monitoring-irelands-skills-supply-publication.pdf)  
9. Leaving Cert Maths FL \- educateplus, accessed December 3, 2025, [https://educateplus.ie/markingscheme/leaving-cert-maths-foundation-level](https://educateplus.ie/markingscheme/leaving-cert-maths-foundation-level)  
10. Coimisiún na Scrúduithe Stáit State Examinations Commission Leaving Certificate 2024 Marking Scheme Higher Level Mathematics \- Studyclix, accessed December 3, 2025, [https://blob-static.studyclix.ie/cms/media/de9d6f81-bbb7-4bef-8bf9-ee066b558b78.pdf](https://blob-static.studyclix.ie/cms/media/de9d6f81-bbb7-4bef-8bf9-ee066b558b78.pdf)  
11. Coimisiún na Scrúduithe Stáit State Examinations Commission Leaving Certificate 2024 Marking Scheme Ordinary Level Mathematic \- Studyclix, accessed December 3, 2025, [https://blob-static.studyclix.ie/cms/media/120910cb-95a8-4d3c-bc64-9135caeeaebf.pdf](https://blob-static.studyclix.ie/cms/media/120910cb-95a8-4d3c-bc64-9135caeeaebf.pdf)  
12. Ontologies \- Cognee Documentation, accessed December 3, 2025, [https://docs.cognee.ai/core-concepts/further-concepts/ontologies](https://docs.cognee.ai/core-concepts/further-concepts/ontologies)  
13. Cognee \- Qdrant, accessed December 3, 2025, [https://qdrant.tech/documentation/frameworks/cognee/](https://qdrant.tech/documentation/frameworks/cognee/)  
14. Graph Stores \- Cognee Documentation, accessed December 3, 2025, [https://docs.cognee.ai/setup-configuration/graph-stores](https://docs.cognee.ai/setup-configuration/graph-stores)  
15. BAML documentation, accessed December 3, 2025, [https://docs.boundaryml.com/home](https://docs.boundaryml.com/home)  
16. Your prompts are using 4x more tokens than you need | BAML Blog, accessed December 3, 2025, [https://boundaryml.com/blog/type-definition-prompting-baml](https://boundaryml.com/blog/type-definition-prompting-baml)  
17. Mathematics: Leaving Certificate Examination 2024 | PDF | Circle \- Scribd, accessed December 3, 2025, [https://www.scribd.com/document/851778111/9a6cfe2d-5e44-4d1c-a695-f5cf45926afa](https://www.scribd.com/document/851778111/9a6cfe2d-5e44-4d1c-a695-f5cf45926afa)  
18. generator \- Boundary Documentation \- BAML, accessed December 3, 2025, [https://docs.boundaryml.com/ref/baml/generator](https://docs.boundaryml.com/ref/baml/generator)  
19. Python \- Boundary Documentation \- BAML, accessed December 3, 2025, [https://docs.boundaryml.com/guide/installation-language/python](https://docs.boundaryml.com/guide/installation-language/python)  
20. Overview | Zep Documentation, accessed December 3, 2025, [https://help.getzep.com/graphiti/getting-started/overview](https://help.getzep.com/graphiti/getting-started/overview)  
21. Searching the Graph \- Zep Documentation, accessed December 3, 2025, [https://help.getzep.com/v2/searching-the-graph](https://help.getzep.com/v2/searching-the-graph)  
22. Zep: A Temporal Knowledge Graph Architecture for Agent Memory \- arXiv, accessed December 3, 2025, [https://arxiv.org/html/2501.13956v1](https://arxiv.org/html/2501.13956v1)  
23. Graphiti (Knowledge Graph Agent Memory) Gets Custom Entity Types : r/LLMDevs \- Reddit, accessed December 3, 2025, [https://www.reddit.com/r/LLMDevs/comments/1j0ca03/graphiti\_knowledge\_graph\_agent\_memory\_gets\_custom/](https://www.reddit.com/r/LLMDevs/comments/1j0ca03/graphiti_knowledge_graph_agent_memory_gets_custom/)  
24. CocoIndex Flow Definition, accessed December 3, 2025, [https://cocoindex.io/docs/core/flow\_def](https://cocoindex.io/docs/core/flow_def)  
25. Why AI ETL Needs Different Primitives: Lessons from Building CocoIndex in Rust, accessed December 3, 2025, [https://dev.to/badmonster0/why-ai-etl-needs-different-primitives-lessons-from-building-cocoindex-in-rust-5hh5](https://dev.to/badmonster0/why-ai-etl-needs-different-primitives-lessons-from-building-cocoindex-in-rust-5hh5)  
26. Operate a Flow | CocoIndex, accessed December 3, 2025, [https://cocoindex.io/docs/core/flow\_methods](https://cocoindex.io/docs/core/flow_methods)  
27. Bring your own building blocks: Export anywhere with Custom Targets \- CocoIndex, accessed December 3, 2025, [https://cocoindex.io/blogs/custom-targets](https://cocoindex.io/blogs/custom-targets)  
28. Custom Targets | CocoIndex, accessed December 3, 2025, [https://cocoindex.io/docs/custom\_ops/custom\_targets](https://cocoindex.io/docs/custom_ops/custom_targets)  
29. graphgeeks-lab/awesome-graph-universe: A curated list of resources for graph-related topics, including graph databases, analytics and science \- GitHub, accessed December 3, 2025, [https://github.com/graphgeeks-lab/awesome-graph-universe](https://github.com/graphgeeks-lab/awesome-graph-universe)  
30. GRAPH.CONSTRAINT CREATE \- FalkorDB Docs, accessed December 3, 2025, [https://docs.falkordb.com/commands/graph.constraint-create.html](https://docs.falkordb.com/commands/graph.constraint-create.html)  
31. GraphRAG Toolkit \- FalkorDB Docs, accessed December 3, 2025, [https://docs.falkordb.com/genai-tools/graphrag-toolkit.html](https://docs.falkordb.com/genai-tools/graphrag-toolkit.html)