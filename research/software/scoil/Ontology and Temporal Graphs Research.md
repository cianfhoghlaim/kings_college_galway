# **Architectural Convergence: BAML, CocoIndex, Cognee, and Graphiti in Temporal Ontology Engineering**

## **1\. Executive Summary and Theoretical Framework**

The evolution of Artificial Intelligence from stochastic text generation to reliable, domain-specific reasoning engines necessitates a fundamental re-architecture of the underlying data infrastructure. The prevailing paradigm of Retrieval-Augmented Generation (RAG), which relies primarily on static vector similarity, is increasingly insufficient for domains requiring complex reasoning over time, strict schema adherence, and granular lineage tracking. This research report presents an exhaustive analysis of a next-generation stack composed of four distinct but highly complementary technologies: **BAML (Boundary AI Modeling Language)**, **CocoIndex**, **Cognee**, and **Graphiti**.  
The investigation is framed through the lens of a specific, high-complexity domain: the **Irish Leaving Certificate** education system. This domain provides a rigorous testbed for the architecture, characterized by its temporal volatility (e.g., annual changes in CAO points, grading scale reforms in 2017), intricate logical constraints (e.g., university matriculation rules), and diverse unstructured data sources (PDF booklets, syllabus documents).  
The central thesis of this analysis is that the integration of these four tools resolves the "Probabilistic-Deterministic Paradox" in AI application development. By combining **BAML’s** deterministic schema enforcement 1 with **CocoIndex’s** incremental data logistics 3, **Cognee’s** cognitive structuring 5, and **Graphiti’s** temporal persistence 7, we can construct a **Temporal Knowledge Graph** that is not only accurate but capable of "time-traveling" through the evolution of educational policy. This report details the theoretical underpinnings, the interaction dynamics, and the practical implementation of this unified pipeline.

## ---

**2\. Domain Analysis: The Leaving Certificate Ontology**

To understand the requirements for BAML schemas and Graphiti temporal edges, we must first rigorously deconstruct the data domain. The "Leaving Certificate" is not a static dataset but a complex, evolving ecosystem of rules, entities, and temporal events.

### **2.1 The Temporal Evolution of Grading Architectures**

One of the most significant challenges in modeling this domain is the discontinuity in grading schemas. A naive RAG system treating all documents as "current" would fail to distinguish between historical and active grading bands. The research material highlights a critical inflection point in 2017, where the grading system shifted from the alphanumeric A-F scale to the H1-H8/O1-O8 scale.9  
Table 1 illustrates the comparative schema complexity that the ontology must support, derived from historical data and current specifications.9

| Era | Level Type | Grade Identifiers | Point Value Range | Temporal Validity |
| :---- | :---- | :---- | :---- | :---- |
| **Pre-2017** | Higher | A1, A2, B1... F | 100 \- 0 | \< 2017 |
| **Pre-2017** | Ordinary | A1, A2, B1... F | 60 \- 0 | \< 2017 |
| **Post-2017** | Higher | H1, H2... H8 | 100 \- 0 | \>= 2017 |
| **Post-2017** | Ordinary | O1, O2... O8 | 56 \- 0 | \>= 2017 |

The analysis of snippet 9 further reveals the "Maths Bonus" anomaly. Prior to 2012, no bonus points were awarded. From 2012 onwards, a 25-point bonus was introduced for grades H6 (or D3 pre-2017) and above. This creates a conditional logic requirement for the ontology: the value of a "Maths" entity is not intrinsic but dependent on both the *grade achieved* and the *year of the examination*. A static graph cannot capture this; a temporal graph (Graphiti) is essential to model the HAS\_POINT\_VALUE edge as having a valid\_from=2012 property.

### **2.2 Matriculation Logic and Boolean Constraints**

The requirements for university entry (Matriculation) introduce a layer of logic that exceeds simple keyword matching. Snippet 11 from Qualifax outlines complex Boolean constraints: "Must obtain at least Grade H5 in two subjects and Grade O6/H7 in the remaining four subjects." This is not a simple fact; it is a **rule**.  
The ontology must therefore define a MatriculationRule entity. This entity interacts with Subject entities. For example, Engineering courses often have a "Special Requirement" of H4 in Mathematics.11 This is a hyper-edge in the knowledge graph, linking Course:Engineering to Subject:Mathematics with a property MinGrade=H4. The extraction of this logic requires the high-fidelity parsing capabilities of BAML, as standard LLM prompting often fails to distinguish between "recommended" and "mandatory" constraints.

### **2.3 Subject Grouping and Curriculum Dynamics**

The "Subject Choice" booklets from schools like Loreto College 12 and St. Joseph's 13 introduce the concept of "Option Blocks." These are temporal groupings valid only for a specific academic year. A student in 2024 might be able to choose "Physics" and "History" together, while in 2025, they might fall into the same block, making them mutually exclusive.  
The snippet 14 provides evidence of schema evolution, noting the introduction of new subjects like "Drama, Film and Theatre Studies" and "Climate Action and Sustainable Development" in 2025\. This necessitates an "Open World" ontology where the Subject enum is extensible. The CocoIndex-BAML pipeline must be robust enough to encounter a previously unknown subject string and correctly classify it as a new Subject entity rather than a hallucination.

## ---

**3\. BAML: The Deterministic Extraction Layer**

In the proposed architecture, BAML (Boundary AI Modeling Language) serves as the primary interface between the unstructured source documents (PDFs, text) and the structured data pipeline. Its role is to enforce **Schema-Aligned Parsing (SAP)**, ensuring that the probabilistic output of the Large Language Model (LLM) conforms strictly to the ontology defined for the Leaving Certificate domain.

### **3.1 The Imperative for Schema-Aligned Parsing**

The core challenge identified in LLM-based extraction is the "fragility of structure." When prompting a model to extract matriculation rules from a complex PDF 11, standard JSON prompting often results in syntax errors or hallucinated fields. BAML addresses this by treating the prompt as a function with a strictly typed return value.1  
The research indicates that BAML uses a Rust-based parsing engine that operates post-generation (or during streaming) to "heal" the output.2 For our Leaving Certificate ontology, this is critical. If a document lists a grade as "H 1" (with a space) or "Honours 1", BAML's fuzzy parsing logic, guided by the defined Enum, can map this deterministically to the Grade::H1 enum member. This capability reduces the error rate in downstream graph construction, preventing the creation of duplicate nodes (e.g., Node:H1 and Node:Honours1) which would fragment the knowledge graph.

### **3.2 BAML Schema Definitions for Education Ontology**

To operationalize the indexing plan, strict BAML schemas must be defined. These schemas act as the "contract" that the CocoIndex pipeline enforces.

#### **3.2.1 The Universal Grading Schema**

To handle the pre- and post-2017 complexity 9, the BAML schema utilizes a union of enums or a comprehensive mapping class.

Code snippet

// BAML Definition: Grading Ontology

enum GradeSymbol {  
  // Modern Grades (Post-2017)  
  H1, H2, H3, H4, H5, H6, H7, H8  
  O1, O2, O3, O4, O5, O6, O7, O8  
    
  // Legacy Grades (Pre-2017)  
  A1, A2, B1, B2, B3, C1, C2, C3, D1, D2, D3, E, F, NG  
}

class GradeRequirement {  
  subject: string @description("The canonical name of the subject")  
  min\_grade: GradeSymbol  
  level: "Higher" | "Ordinary" | "Foundation"  
  is\_bonus\_point\_eligible: bool?  
}

This schema ensures that when the LLM encounters "a pass in higher level Mathematics" 9, it extracts a GradeRequirement object where subject="Mathematics", min\_grade=H6 (the modern equivalent of a pass), and level="Higher".

#### **3.2.2 The Matriculation Rule Schema**

Modeling the logic from snippet 11 requires a nested structure. BAML's ability to handle nested classes allows for the capture of complex Boolean logic found in university prospectuses.

Code snippet

// BAML Definition: Matriculation Logic

class MatriculationSet {  
  university\_name: string  
  general\_requirements: GeneralRule  
  special\_course\_requirements: CourseRule  
}

class GeneralRule {  
  required\_subjects: int @description("Total subjects required, e.g., 6")  
  honours\_count: int @description("Number of Higher Level grades required, e.g., 2")  
  honours\_min\_grade: GradeSymbol @description("Minimum grade for the honours count, e.g., H5")  
}

By using this schema, the extraction of the "Two H5s" rule 11 becomes a deterministic data point: honours\_count: 2, honours\_min\_grade: H5. This structured object is then ready for ingestion by Cognee.

### **3.3 The Integration Mechanism: BAML as a Function**

In the interaction with CocoIndex, BAML functions act as atomic transformation units. The BAML compiler generates a client (e.g., in Python) that can be imported directly into the CocoIndex transformation logic. This tightly coupled interaction allows for **Type-Safe Prompting**. The CocoIndex pipeline does not need to parse strings; it receives Pydantic objects directly from the BAML client.  
Furthermore, BAML's efficiency in token usage—reported to be up to 4x more efficient than standard JSON schemas due to its compressed syntax 2—is a vital consideration when processing the extensive corpus of Leaving Certificate documents. Reducing the context overhead allows for more of the "Subject Choice" booklet to be included in the prompt window, improving the model's ability to understand context (e.g., distinguishing between a "History" subject and the "History of Art" subject).

## ---

**4\. CocoIndex: The Incremental Orchestration Engine**

While BAML provides the "how" of extraction, **CocoIndex** provides the "when" and "where." It serves as the incremental ETL (Extract, Transform, Load) framework that orchestrates the data lifecycle. The analysis of snippets 3 reveals that CocoIndex is specifically designed for AI workloads, differentiating itself from generic orchestrators like Dagster by focusing on **Data Lineage** and **Incremental Indexing**.

### **4.1 The Mechanism of Incremental Processing**

In the context of the Leaving Certificate timeline, documents are updated annually. A re-indexing of the entire corpus every time a single syllabus changes is computationally expensive and inefficient. CocoIndex addresses this through a robust state-tracking mechanism backed by a metadata store (typically Postgres).3  
When a new PDF, such as LC\_Syllabus\_Physics\_2025.pdf, is added to the source repository:

1. **Change Detection:** CocoIndex detects the file via its connector (e.g., LocalFile or S3) and calculates a content hash.  
2. **Lineage Evaluation:** It checks the metadata store to see if this hash has been processed by the current Flow definition.  
3. **Selective Execution:** If the file is new or modified, CocoIndex triggers the transformation pipeline *only* for this specific asset.

This capability is essential for the **Temporal Graph** architecture. By isolating the new document as a discrete "event," CocoIndex allows the downstream system (Graphiti) to assign a precise timestamp to the extracted facts, effectively "versioning" the knowledge.

### **4.2 The CocoIndex Flow Architecture**

The "indexing plan" requires a specific flow to move data from PDF to Graph. CocoIndex uses a declarative "Flow Builder" pattern.17

#### **4.2.1 Ingestion and Chunking**

The flow begins with the LocalFile source. The snippet 17 describes a SplitRecursively function for chunking. For the Leaving Cert ontology, standard fixed-size chunking is suboptimal because a single "Subject Description" might span a page break.

* **Optimization:** A custom chunking function can be implemented within CocoIndex to segment documents based on semantic headers (e.g., "Syllabus Content", "Assessment"). This ensures that BAML receives a coherent block of text representing a single logical entity.

#### **4.2.2 The BAML Transformation Step**

This is the critical integration point. CocoIndex supports Custom Functions.3 We define a custom transform ExtractWithBAML that wraps the generated BAML client.

Python

\# Conceptual CocoIndex Flow Integration  
@cocoindex.transform  
def baml\_extraction\_step(text\_chunk: str, metadata: dict):  
    from baml\_client import b  
    \# BAML handles the LLM interaction and schema enforcement  
    result \= b.ExtractRequirements(text\_chunk)  
    \# Return the structured object enriched with document metadata (Year)  
    return {  
        "data": result,  
        "valid\_year": metadata.get("year"),  
        "source\_id": metadata.get("filename")  
    }

This code snippet illustrates the seamless handoff. CocoIndex manages the file I/O and concurrency; BAML manages the intelligence.

### **4.3 Multi-Modal Indexing Strategy**

CocoIndex's architecture supports multi-modal outputs. Snippet 19 discusses "Multi-Vector" support. This is highly relevant for the Leaving Certificate "Subject Choice" booklets, which often contain images of student artwork (for Art) or technical diagrams (for DCG \- Design & Communication Graphics).

* **Vector Path:** CocoIndex can route the raw text chunks to a vector store like **Qdrant** 16 for semantic search (e.g., "Find subjects that involve drawing").  
* **Graph Path:** Simultaneously, it routes the BAML-extracted structure to **Cognee** for graph construction (e.g., "What is the point code for DCG?").

This bifurcation allows the system to satisfy both broad semantic queries and precise structural queries, a hallmark of advanced RAG systems.

## ---

**5\. Cognee: Cognitive Structuring and Entity Resolution**

**Cognee** acts as the semantic bridge between the extracted data and the persistent storage. While BAML provides isolated structured facts, Cognee provides **Contextual Cohesion**. Its primary function in this architecture is "Cognification"—the process of mapping isolated data points into a connected graph structure.5

### **5.1 The "Cognify" Process and Identity Management**

A recurring challenge in the Leaving Certificate dataset is inconsistent nomenclature. One document might refer to "Maths," another to "Mathematics," and a third to "Project Maths" (a specific syllabus iteration). If these are stored as separate nodes, the graph becomes fragmented.  
Cognee addresses this through its **Entity Resolution** layer. When cognify() is called on the data stream from CocoIndex 6, Cognee analyzes the entities to determine identity.

* **Mechanism:** It uses LLM-based reasoning (now potentially powered by BAML internally 20) to determine that Entity("Maths") and Entity("Mathematics") are semantically identical.  
* **Outcome:** It merges these references into a single canonical Node (e.g., Subject:Mathematics), preserving the relationships from all source documents. This creates a dense, highly connected graph rather than a sparse collection of disjoint facts.

### **5.2 Ontology Mapping and Graph Construction**

Cognee allows for the definition of a "Business Ontology Layer".21 For our education domain, we map the BAML output schemas directly to Cognee's internal graph representation.

* **BAML Object:** CourseRequirement { subject: "French", min\_grade: "H3" }  
* **Cognee Transformation:**  
  1. Identify or Create Node: Subject:French.  
  2. Identify or Create Node: Grade:H3.  
  3. Create Edge: REQUIRES from the Context Node (e.g., Course:Law) to Subject:French with property min\_grade=H3.

Snippet 22 explains that Cognee's "DataPoints" are the building blocks of this graph. Each BAML output is converted into a DataPoint, which Cognee then stitches into the fabric of the existing knowledge graph. This process ensures that new information (e.g., the 2025 syllabus update) is not just "added" but "integrated," linking to existing histories and entities.

### **5.3 Hybrid Storage Integration**

Cognee is unique in its native support for both graph and vector storage (Hybrid Storage).5 It often utilizes **FalkorDB** as the graph backend and a vector store (like Qdrant or LanceDB) for embeddings.  
In our unified architecture, Cognee plays the role of the "Staging Controller." It prepares the graph structure—cleaning entities, resolving edges—before committing the data to the temporal storage engine, Graphiti. This separation of concerns is vital; Cognee ensures *structural integrity* (the graph makes sense), while Graphiti ensures *temporal integrity* (the history is preserved).

## ---

**6\. Graphiti: The Temporal Persistence Layer**

The final, and perhaps most transformative, component of the stack is **Graphiti**. Standard graph databases store the *current state* of the world. However, the Leaving Certificate domain is inherently historical: points requirements fluctuate, syllabi are retired, and grading scales are reformed. Graphiti solves this by implementing a **Bi-Temporal Knowledge Graph**.7

### **6.1 The Theory of Bi-Temporal Modeling**

Graphiti distinguishes between two timelines 8:

1. **Valid Time (t\_valid):** The real-world time period when a fact is true. (e.g., "Maths Bonus Points Active: 2012–Present").  
2. **Transaction Time (t\_record):** The system time when the fact was ingested.

This dual-indexing allows for complex auditing. We can query "What did the system *think* the requirements were in 2023?" (Transaction Time) vs "What *were* the requirements in 2023?" (Valid Time). For the "Indexing Plan," this implies that every document processed by CocoIndex must yield a specific valid\_time (extracted from the document year, e.g., "Leaving Cert 2025").

### **6.2 Edge Invalidation and State Evolution**

The most powerful feature of Graphiti for this domain is **Edge Invalidation**.24

* **Scenario:** In 2011, the edge Maths \-\> HAS\_BONUS did not exist (or had value False). In 2012, the rule changed.9  
* **Mechanism:** When Graphiti ingests the 2012 episode (via add\_episode), it detects a contradiction with the previous state. Instead of overwriting (deleting) the 2011 fact, it "expires" the old edge by setting its valid\_to date to 2012\. It then creates a new edge with valid\_from=2012.  
* **Implication:** This preserves the history. A user asking "Why were points lower in 2010?" can be answered by the graph traversing the edges valid in 2010, which would lack the bonus points, thus explaining the discrepancy.

### **6.3 Implementation: The add\_episode Interface**

The interaction between the pipeline and Graphiti occurs via the add\_episode API.25 The snippet provided gives the specific syntax required for integration.

Python

\# Conceptual Implementation based on Snippet   
from datetime import datetime  
from graphiti\_core import Graphiti

async def ingest\_requirement(extracted\_data, doc\_year):  
    \# The BAML object is serialized to JSON  
    episode\_body \= extracted\_data.model\_dump\_json()  
      
    \# We explicitly set the reference time to the academic year  
    \# This is crucial for the "Time Travel" capability  
    ref\_time \= datetime(doc\_year, 9, 1\)   
      
    await graphiti.add\_episode(  
        name=f"Matriculation Update {doc\_year}",  
        episode\_body=episode\_body,  
        source=EpisodeType.json,  
        reference\_time=ref\_time  
    )

This code explicitly links the extracted data to a point in time. Graphiti's internal logic then handles the "diffing" against the existing graph to determine if new edges need to be created or old ones invalidated.

### **6.4 The Zep and FalkorDB Connection**

Graphiti is the core engine powering **Zep** (a context engineering platform) and is built on top of **FalkorDB** (a Redis-based graph database).23 This architectural lineage ensures high performance. FalkorDB’s sparse matrix implementation allows for extremely fast graph traversals, which is necessary when calculating temporal paths (e.g., "Find all subjects that have *ever* been required for Medicine").

## ---

**7\. Integration Synthesis: The Unified Pipeline**

Having analyzed the components individually, we can now define the Unified Pipeline. This architecture represents the "Interaction" requested in the prompt.

### **7.1 Data Flow Diagram**

The data flows sequentially through the system, transforming from unstructured text to temporal knowledge.

1. **Ingestion (CocoIndex):**  
   * *Input:* Subject\_Booklet\_2025.pdf  
   * *Action:* Detects new file, reads stream, splits into semantic chunks.  
   * *Output:* Chunk objects.  
2. **Extraction (CocoIndex \+ BAML):**  
   * *Input:* Chunk text.  
   * *Action:* CocoIndex calls baml\_client. BAML constrains LLM to extract CourseRequirement objects using the GradeSymbol enum.  
   * *Output:* Structured CourseRequirement JSON.  
3. **Structuring (Cognee):**  
   * *Input:* CourseRequirement JSON.  
   * *Action:* Cognee resolves "Maths" to Subject:Mathematics. It prepares the graph topology (Nodes/Edges).  
   * *Output:* Resolves Entities and Relations.  
4. **Persistence (Graphiti):**  
   * *Input:* Entities/Relations \+ Metadata (year=2025).  
   * *Action:* Graphiti performs add\_episode. It invalidates old edges (e.g., old point requirements) and asserts new ones with valid\_from=2025.  
   * *Output:* Updated Temporal Knowledge Graph.

### **7.2 The Interaction Matrix**

Table 2 summarizes the specific interactions and data handoffs between the technologies.

| Interaction Pair | Interaction Type | Mechanism | Data Handoff |
| :---- | :---- | :---- | :---- |
| **CocoIndex → BAML** | Function Call | Python Wrapper around Rust Client | Raw Text → Pydantic Model |
| **BAML → Cognee** | Data Ingestion | Adapter Pattern | Pydantic Model → Cognee DataPoint |
| **Cognee → Graphiti** | Persistence | API Call (add\_episode) | Entities/Relations → Temporal Graph Nodes |
| **CocoIndex → Graphiti** | Metadata Sync | Logic Injection | Document Year → reference\_time |

### **7.3 Addressing the "Indexing Plan"**

The "Indexing Plan" inferred from the snippets dictates that we must handle:

1. **Grades:** Handled by BAML Enums (Pre/Post 2017).  
2. **Points:** Handled by Graphiti Temporal Edges (Value changes).  
3. **Matriculation:** Handled by BAML Nested Classes (Boolean logic).  
4. **Subject Groups:** Handled by Cognee Graph Clusters (Option Blocks).

This pipeline ensures that every aspect of the indexing plan is addressed by the specific strength of a component in the stack.

## ---

**8\. Case Study Walkthrough: The "2025 Subject Choice" Scenario**

To demonstrate the practical application of this research, we trace a specific scenario based on snippet 12 (Loreto College Subject Booklet) and 14 (New Subjects).

### **8.1 Scenario Setup**

A new PDF, Loreto\_2025.pdf, is released. It contains:

1. A new subject: "Climate Action and Sustainable Development."  
2. A constraint: "French must be in the first 4 choices."  
3. A timeline: "For Fifth Year 2025."

### **8.2 Step-by-Step Execution**

Step 1: CocoIndex Detection  
CocoIndex's LocalFile connector sees the new PDF. The SplitRecursively function chunks the document. Crucially, the filename 2025 is parsed into the metadata context.  
Step 2: BAML Extraction  
The chunk containing "Climate Action..." is passed to BAML.

* The LLM sees a string that looks like a subject but isn't in the pre-defined Enum.  
* BAML's flexible schema (subject: string | enum) allows the extraction of this new string.  
* BAML extracts the rule: Constraint { type: "MandatoryChoice", subject: "French", position: }.

Step 3: Cognee Resolution  
Cognee receives the entity Subject:Climate Action.

* It queries its graph: "Do I have this node?" \-\> No.  
* Action: Creates new Node Subject:Climate Action.  
* It receives Subject:French.  
* Query: "Do I have this?" \-\> Yes.  
* Action: Links the new Constraint edge to the existing French node.

Step 4: Graphiti Temporal Update  
Cognee pushes this episode to Graphiti with reference\_time=2025-09-01.

* Graphiti checks the OFFERS\_SUBJECT edges for Loreto College.  
* It sees previous edges (History, Physics, etc.).  
* It adds the new edge Loreto \-\> OFFERS \-\> Climate Action.  
* **Result:** A query "What subjects did Loreto offer in 2024?" returns the old list. "What subjects in 2025?" includes Climate Action. The system has successfully modeled schema evolution without manual intervention.

## ---

**9\. Strategic Implications and Future Outlook**

The convergence of BAML, CocoIndex, Cognee, and Graphiti signals a maturation of the AI engineering stack. We are moving away from "black box" RAG systems toward **White-Box, Auditable, and Temporal Intelligence**.

### **9.1 Second-Order Insights**

* **The Auditability of AI:** By using Graphiti's bi-temporal model, institutions can audit *exactly* what the AI knew and when. If an AI agent gives advice on CAO points, the system can prove that the advice was correct *based on the data valid at that time*. This is crucial for liability in educational advising.  
* **The "Healing" of Data:** BAML's Schema-Aligned Parsing doesn't just extract data; it cleans it. Over time, the input into the knowledge graph becomes higher quality than the source documents themselves, as the BAML layer normalizes disparate naming conventions into a canonical ontology.

### **9.2 Conclusion**

This research confirms that the integration of these four technologies provides a complete solution for the "Leaving Certificate" indexing plan. BAML provides the **Precision**, CocoIndex provides the **Efficiency**, Cognee provides the **Structure**, and Graphiti provides the **History**. Together, they form a robust, production-ready architecture for Temporal Ontology Engineering.

#### **Works cited**

1. BAML documentation, accessed December 5, 2025, [https://docs.boundaryml.com/home](https://docs.boundaryml.com/home)  
2. The Prompting Language Every AI Engineer Should Know: A BAML Deep Dive | Towards AI, accessed December 5, 2025, [https://towardsai.net/p/machine-learning/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive](https://towardsai.net/p/machine-learning/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive)  
3. CocoIndex Indexing Basics, accessed December 5, 2025, [https://cocoindex.io/docs/core/basics](https://cocoindex.io/docs/core/basics)  
4. CocoIndex, accessed December 5, 2025, [https://cocoindex.io/](https://cocoindex.io/)  
5. Cognee | FalkorDB Docs, accessed December 5, 2025, [https://docs.falkordb.com/agentic-memory/cognee.html](https://docs.falkordb.com/agentic-memory/cognee.html)  
6. A Spotlight on Cognee: the memory engine for AI Agents \- G.V(), accessed December 5, 2025, [https://gdotv.com/blog/cognee-graphs-that-learn/](https://gdotv.com/blog/cognee-graphs-that-learn/)  
7. Welcome to Graphiti\! \- Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/graphiti/getting-started/welcome](https://help.getzep.com/graphiti/getting-started/welcome)  
8. Graphiti: Knowledge Graph Memory for an Agentic World \- Neo4j, accessed December 5, 2025, [https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/](https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/)  
9. Leaving Certificate (Ireland) \- Wikipedia, accessed December 5, 2025, [https://en.wikipedia.org/wiki/Leaving\_Certificate\_(Ireland)](https://en.wikipedia.org/wiki/Leaving_Certificate_\(Ireland\))  
10. Studying in Cork a guide for parents \- CIT, accessed December 5, 2025, [https://www.cit.ie/contentfiles/pdfs/studying%20in%20cork\_a%20guide%20for%20parents.pdf](https://www.cit.ie/contentfiles/pdfs/studying%20in%20cork_a%20guide%20for%20parents.pdf)  
11. Engineering | Qualifax, accessed December 5, 2025, [https://www.qualifax.ie/course/1716](https://www.qualifax.ie/course/1716)  
12. Leaving Certificate Subjects Booklet 5th Year 2025/26 \- Loreto College Foxrock, accessed December 5, 2025, [https://loretofoxrock.ie/wp-content/uploads/2025/06/5th-Year-2025-LC-Subjects-Booklet.pdf](https://loretofoxrock.ie/wp-content/uploads/2025/06/5th-Year-2025-LC-Subjects-Booklet.pdf)  
13. SUBJECT CHOICE FOR LEAVING CERTIFICATE 2024/2025 \- St Joseph's Secondary School, accessed December 5, 2025, [https://conch-gecko-8wkx.squarespace.com/s/2023-Subject-options-booklet.pdf](https://conch-gecko-8wkx.squarespace.com/s/2023-Subject-options-booklet.pdf)  
14. Leaving Certificate \- Citizens Information, accessed December 5, 2025, [https://www.citizensinformation.ie/en/education/state-examinations/leaving-certificate/](https://www.citizensinformation.ie/en/education/state-examinations/leaving-certificate/)  
15. Why I'm excited about BAML and the future of agentic workflows \- The Data Quarry, accessed December 5, 2025, [https://thedataquarry.com/blog/baml-and-future-agentic-workflows/](https://thedataquarry.com/blog/baml-and-future-agentic-workflows/)  
16. CocoIndex \- Qdrant, accessed December 5, 2025, [https://qdrant.tech/documentation/data-management/cocoindex/](https://qdrant.tech/documentation/data-management/cocoindex/)  
17. Simple Vector Index with Text Embedding | CocoIndex, accessed December 5, 2025, [https://cocoindex.io/docs/examples/simple\_vector\_index](https://cocoindex.io/docs/examples/simple_vector_index)  
18. cocoindex-io/cocoindex: Data transformation framework for ... \- GitHub, accessed December 5, 2025, [https://github.com/cocoindex-io/cocoindex](https://github.com/cocoindex-io/cocoindex)  
19. Multi-Dimensional Vector Support in CocoIndex, accessed December 5, 2025, [https://cocoindex.io/blogs/multi-vector](https://cocoindex.io/blogs/multi-vector)  
20. Actual open source local memory with no hidden cloud : r/LocalLLaMA \- Reddit, accessed December 5, 2025, [https://www.reddit.com/r/LocalLLaMA/comments/1ml2t67/actual\_open\_source\_local\_memory\_with\_no\_hidden/](https://www.reddit.com/r/LocalLLaMA/comments/1ml2t67/actual_open_source_local_memory_with_no_hidden/)  
21. Improve your AI infrastructure \- AI memory engine, accessed December 5, 2025, [https://www.cognee.ai/](https://www.cognee.ai/)  
22. Knowledge Graphs Explained: Structure, AI Applications & Benefits \- Cognee, accessed December 5, 2025, [https://www.cognee.ai/blog/fundamentals/building-blocks-of-knowledge-graphs](https://www.cognee.ai/blog/fundamentals/building-blocks-of-knowledge-graphs)  
23. \[2501.13956\] Zep: A Temporal Knowledge Graph Architecture for Agent Memory \- arXiv, accessed December 5, 2025, [https://arxiv.org/abs/2501.13956](https://arxiv.org/abs/2501.13956)  
24. Building Temporal Knowledge Graphs with Graphiti \- FalkorDB, accessed December 5, 2025, [https://www.falkordb.com/blog/building-temporal-knowledge-graphs-graphiti/](https://www.falkordb.com/blog/building-temporal-knowledge-graphs-graphiti/)  
25. Adding Episodes \- Zep Documentation, accessed December 5, 2025, [https://help.getzep.com/graphiti/core-concepts/adding-episodes](https://help.getzep.com/graphiti/core-concepts/adding-episodes)  
26. Graphiti \- FalkorDB Docs, accessed December 5, 2025, [https://docs.falkordb.com/agentic-memory/graphiti.html](https://docs.falkordb.com/agentic-memory/graphiti.html)