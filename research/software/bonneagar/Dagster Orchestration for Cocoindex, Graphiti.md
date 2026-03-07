# **Architectural Blueprint for Mathematical Knowledge Extraction: A Modular Orchestration Strategy Using Dagster, Cocoindex, and Graphiti**

## **1\. Introduction: The Imperative for Semantic Intelligence in Educational Assessment**

The digitalization of educational assessment creates a profound data engineering challenge: the conversion of unstructured, mathematically dense artifacts—exam papers, marking schemes, and curriculum standards—into a structured, queryable knowledge base. This transformation is not merely a matter of optical character recognition (OCR); it is a semantic reconstruction task that requires preserving the rigorous logical structure of mathematical notation, the hierarchical nature of curriculum standards, and the temporal evolution of assessment criteria.  
This report outlines a comprehensive architectural strategy for building a high-fidelity data pipeline capable of synthesizing these disparate artifacts. The core objective is to establish an intelligence layer that supports advanced retrieval, automated reasoning, and longitudinal analysis of educational performance without incurring the technical debt of monolithic systems or the risks of proprietary vendor lock-in.  
The proposed architecture is founded on three pillars of modern data engineering:

1. **Asset-Based Orchestration via Dagster:** Shifting the paradigm from imperative task execution to declarative data management, ensuring that the state of every exam paper and curriculum document is observable, reproducible, and verifiable.1  
2. **Incremental Vector Indexing via Cocoindex:** Leveraging a high-velocity, flow-based transformation engine to generate semantic embeddings for mathematical content, enabling "find questions like this" capabilities that transcend simple keyword matching.2  
3. **Temporal Knowledge Graphing via Graphiti:** Constructing a dynamic graph of entities (Theorems, Topics, Questions) that respects the dimension of time, allowing the system to reason about how curriculum standards and assessment rigor evolve over years.4

By decoupling the extraction logic—specifically utilizing open-source tools like **Marker** to handle the nuances of LaTeX and mathematical typography—from the storage and indexing layers, organizations can achieve a robust system. This report provides an exhaustive analysis of the design decisions, implementation strategies, and theoretical underpinnings required to execute this vision.

## ---

**2\. Theoretical Framework and Design Philosophy**

The complexity of mathematical content, combined with the stringent requirements for accuracy in an educational context, necessitates a departure from traditional "script-based" ETL (Extract, Transform, Load) pipelines. Instead, we adopt a software engineering approach applied to data: the "Functional Core, Imperative Shell" pattern, orchestrated within a Software-Defined Asset framework.

### **2.1 The "Functional Core, Imperative Shell" Pattern in Data Pipelines**

To ensure modularity and testability—key requirements for avoiding vendor lock-in—the architecture rigorously separates business logic from I/O operations.

#### **2.1.1 The Functional Core: Pure Mathematical Logic**

The "Functional Core" consists of pure, deterministic functions that contain the domain logic.6 In the context of this pipeline, the core is responsible for:

* **Text Cleaning:** Identifying and normalizing LaTeX strings (e.g., standardizing \\frac{a}{b} vs a \\over b).  
* **Entity Extraction:** Parsing a raw text block to identify potential "Theorem" or "Definition" entities based on linguistic patterns, without yet calling an external database.  
* **Data Structuring:** Transforming unstructured Markdown into Pydantic objects that conform to the system's ontology.

Crucially, code in the functional core never connects to a database, never calls an external API, and never reads from a file system. It takes data as input and returns data as output. This isolation means that if the vector database (Cocoindex) or the graph database (Graphiti) changes, the core logic of how a mathematical question is parsed remains untouched.8

#### **2.1.2 The Imperative Shell: Dagster as the Nervous System**

The "Imperative Shell" is responsible for the side effects: reading files, sending data to APIs, and managing state. **Dagster** serves as this shell. It manages the sensors that detect new exam papers, handles the connections to the Postgres database for Cocoindex, and orchestrates the API calls to the Graphiti backend.9 By treating Dagster as the shell, we ensure that the orchestration logic focuses on *when* and *where* to run computations, while the core python modules focus on *how* to process the mathematics.

### **2.2 Asset-Based Orchestration vs. Task-Based Workflows**

Traditional orchestrators (like Airflow) focus on *tasks*: "Run the extraction script." If the script succeeds, the task is green. However, this says nothing about the quality or freshness of the data produced.  
Dagster's **Asset-Based Orchestration** inverts this relationship. We define the *data assets* we expect to exist: the raw\_exam\_pdf, the extracted\_markdown, the semantic\_embeddings, and the knowledge\_graph\_nodes.1

* **Implicit Lineage:** Dagster automatically infers the dependency graph. If the extracted\_markdown asset depends on the raw\_exam\_pdf asset, Dagster knows that an update to the PDF necessitates a re-computation of the Markdown.  
* **Declarative Freshness:** Instead of scheduling a job at 3 AM, we define a freshness policy. The system constantly checks: "Is the vector index up to date with the latest exam papers?" If not, it triggers the necessary materializations.  
* **Partitioned State:** Educational data is naturally partitioned—by academic year, subject code, or exam board. Dagster’s native support for partitioning allows the pipeline to process a single exam paper (a partition) completely independently. This is critical for scalability; a failure in processing "Math Paper 2023" should not block the processing of "Math Paper 2024".10

### **2.3 Strategy for Avoiding Vendor Lock-In**

A core requirement of this research is modularity to prevent vendor lock-in. This is achieved through three strategic architectural decisions:

1. **Open Source Extraction Engines:** We reject proprietary extraction APIs (like Mathpix or Azure Document Intelligence) in favor of local, open-source models like **Marker**.11 This ensures that the capability to read the primary data source (PDFs) is owned by the organization, not rented.  
2. **Standardized Intermediate Formats:** The pipeline relies on universally readable formats for data interchange. Text is stored as Markdown with embedded LaTeX; structured data is passed as JSON. This prevents data from being trapped in a binary blob or a vendor-specific serialization format.  
3. **Abstraction via Pydantic:** Interactions with the knowledge graph are mediated through Pydantic models.13 We define a MathQuestion class in Python. Whether this object is eventually stored in Neo4j, FalkorDB, or a future graph database is an implementation detail handled by the connector layer, not the core logic.

## ---

**3\. The Data Plane: High-Fidelity Mathematical Extraction**

The foundation of the entire pipeline is the accurate extraction of content from PDF documents. In the domain of mathematics, this is non-trivial. Mathematical knowledge is encoded not just in alphanumeric text, but in spatial layout, specific distinct symbology, and two-dimensional structures (matrices, fractions, integrals).

### **3.1 The Challenge of Mathematical Typography**

A standard OCR tool might scan the equation $x \= \\frac{-b \\pm \\sqrt{b^2 \- 4ac}}{2a}$ and output "x \= \-b \+ Vb2 \- 4ac 2a", completely destroying the semantic meaning. The fraction bar, the square root scope, and the superscript are structural, not just textual.  
To build a "Mathematical Context," the extraction layer must recognize these visual cues and translate them into a semantic markup language, predominantly **LaTeX**. LaTeX allows the equation to be represented as x \= \\frac{-b \\pm \\sqrt{b^2 \- 4ac}}{2a}, a string that preserves the hierarchical relationships of the terms.

### **3.2 Comparative Analysis of Extraction Tools**

Deep research into the Python PDF ecosystem reveals a clear stratification of tools based on their ability to handle scientific notation.

| Tool | Primary Focus | Mathematical Fidelity | Performance (Speed) | License / Open Source | Verdict |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **PyPDF / PyPDF2** | PDF Manipulation (Split/Merge) | **None.** Extracts text stream only; destroys layout and formulas.14 | High | BSD (Open) | **Unsuitable.** |
| **PyMuPDF (Fitz)** | Rendering & Text Extraction | **Low.** Excellent for layout analysis (bounding boxes), but poor at formula recognition.14 | Very High | GNU GPL / Commercial | **Helper Only.** Use for metadata. |
| **Nougat** (Meta) | Academic/Scientific Papers | **High.** Transformer-based; converts PDF image directly to Markdown/LaTeX.15 | Low (Slow) | MIT (Open) | **Viable but Slow.** Good fallback. |
| **Mathpix** | STEM Content | **Very High.** Industry standard for math OCR. | High | Proprietary (API) | **Rejected.** Creates vendor lock-in.16 |
| **Marker** | Scientific Books/Papers | **High.** Deep learning pipeline for layout \+ OCR \+ LaTeX conversion.11 | High (\~10x Nougat) | GPL-3.0 (Open) | **Recommended.** Best balance. |

#### **3.2.1 Deep Dive: The Marker Advantage**

**Marker** represents the state-of-the-art for open-source scientific PDF extraction. Unlike **Nougat**, which uses a heavy end-to-end transformer model that can be slow and computationally expensive, Marker employs a pipeline approach:

1. **Layout Detection:** It first segments the page into text blocks, equations, tables, and images using object detection.  
2. **Selective OCR:** It applies OCR only to the text blocks.  
3. **Formula Recognition:** It uses a specialized model (often related to Nougat's architecture but optimized) specifically on the detected equation regions to generate LaTeX.12  
4. **Heuristics:** It applies post-processing heuristics to clean up headers, footers, and page numbers—artifacts that are notoriously problematic in exam papers.

Benchmarks indicate Marker is significantly faster (4x to 10x) than Nougat while maintaining comparable accuracy for mathematical content.11 This speed is critical when backfilling a decade's worth of exam papers. Furthermore, Marker natively outputs **Markdown**, which is the ideal input format for semantic chunking strategies (discussed in Section 5).

### **3.3 Handling Curriculum and Marking Schemes**

While exam papers are dense with equations, **Curriculum Standards** and **Marking Schemes** present different structural challenges.

* **Curriculum Documents:** These are deeply hierarchical. A specific learning objective (e.g., "Calculus \-\> Differentiation \-\> Chain Rule") is defined by its nesting depth. Marker's ability to output structured Markdown (using \#, \#\#, \#\#\# headers) allows the functional core to parse this hierarchy directly, reconstructing the tree structure of the curriculum.12  
* **Marking Schemes:** These are predominantly tabular. A row might contain "Question 1(a) | Answer: 5 | 2 Marks". Standard OCR often breaks tables into independent lines of text. Marker includes specific table recognition capabilities to output Markdown tables. However, for extremely complex, multi-page tables found in some marking schemes, the architecture allows for a "Strategy Pattern" in the extraction asset: if Marker's confidence is low, the system can fall back to a specialized table extraction library like gmft (Grid-Based PDF Table Extraction) or Table Transformer 15, wrapping this logic within the Dagster asset to maintain abstraction.

## ---

**4\. Orchestration Architecture: The Dagster Implementation**

Dagster is selected not merely as a task runner, but as a system for **Software-Defined Assets**. This section details how to implement the orchestration layer to handle the dynamic nature of file ingestion.

### **4.1 Dynamic Partitioning for File Ingestion**

In an educational data pipeline, the input dataset is never static. New exam papers are scanned, marking schemes are updated, and new curriculum documents are released. A static partitioning scheme (e.g., partitioning by "Day") is inefficient because files may arrive in irregular batches.  
We utilize **Dynamic Partitioning**, a powerful Dagster feature that allows the set of partitions (processing units) to be defined and grown at runtime.10  
We define a DynamicPartitionsDefinition named exam\_papers. The partition key will be the unique identifier of the file (e.g., the filename or a hash of the content).

Python

\# Conceptual Architecture for Dynamic Partitions  
from dagster import DynamicPartitionsDefinition, asset

\# Define the dynamic partition set.   
\# Initially empty, this will be populated by a Sensor.  
exam\_paper\_partitions \= DynamicPartitionsDefinition(name="exam\_papers")

@asset(partitions\_def=exam\_paper\_partitions)  
def raw\_pdf\_content(context):  
    """  
    Asset representing the binary content of a specific exam paper.  
    The partition\_key from the context determines which file to load.  
    """  
    partition\_key \= context.partition\_key  
    \# The imperative shell logic to read from the filesystem  
    file\_path \= resolve\_path(partition\_key)  
    with open(file\_path, "rb") as f:  
        return f.read()

This approach creates a discrete asset lineage for *every single exam paper*. If "Math\_Paper\_2023.pdf" fails to process due to a corrupted table, it appears as a failure for that specific partition. It does not block the pipeline for "Math\_Paper\_2024.pdf". This granularity is essential for debugging and backfilling.18

### **4.2 Event-Driven Architecture with Sensors**

To automate the population of these dynamic partitions, we employ **Dagster Sensors**.19 A sensor is a daemon that runs continuously, polling an external state (a local directory, an S3 bucket, or a Google Drive folder) to detect changes.  
The workflow for the new\_file\_sensor is as follows:

1. **Poll:** Check the source directory for PDF files.  
2. **Diff:** Compare the list of found files against the list of existing partitions in exam\_paper\_partitions.  
3. **Register:** For every new file found, explicitly add a new partition key to the exam\_paper\_partitions definition using context.instance.add\_dynamic\_partitions.20  
4. **Trigger:** Yield a RunRequest for the pipeline, specifically targeting the newly created partition key.

This ensures the pipeline is reactive. As soon as a file is dropped into the folder, the sensor wakes up, registers the new asset partition, and launches the processing job.

Python

@sensor(job=process\_exam\_job)  
def new\_exam\_sensor(context):  
    \# Imperative Shell: Interacting with the filesystem  
    current\_files \= list\_files\_in\_directory()  
      
    \# Check which partitions already exist  
    existing\_partitions \= context.instance.get\_dynamic\_partitions("exam\_papers")  
      
    new\_files \= \[f for f in current\_files if f not in existing\_partitions\]  
      
    if new\_files:  
        \# Register the new partitions in Dagster's state  
        context.instance.add\_dynamic\_partitions("exam\_papers", new\_files)  
          
        \# Request a run for each new file  
        for filename in new\_files:  
            yield RunRequest(  
                run\_key=filename, \# Idempotency key  
                partition\_key=filename \# The specific partition to materialize  
            )

### **4.3 The Asset Graph: Lineage and Dependencies**

The architecture visualizes the data flow as a graph of connected assets. This provides immediate observability into the state of the system.

1. **raw\_pdf\_file**: The binary input (Partitioned by filename).  
2. **extracted\_markdown**: The output of the **Marker** process. This asset depends on raw\_pdf\_file. Its computation involves running the Marker library on the binary input. The output is a .md string containing text and LaTeX equations.  
3. **semantic\_chunks**: A derived asset that splits the Markdown into semantic units (e.g., individual questions). This utilizes Cocoindex's splitting logic.  
4. **vector\_embeddings**: The numerical representation of the chunks, generated by Cocoindex.  
5. **knowledge\_graph\_episodes**: The structured entities and relationships extracted from the text, prepared for Graphiti ingestion.

By structuring the pipeline this way, we gain **Memoization**. If we change the embedding model (modifying the logic for vector\_embeddings), Dagster knows it does not need to re-run the expensive PDF-to-Markdown extraction (extracted\_markdown) because that asset has not changed. It only re-runs the downstream assets.

## ---

**5\. The Semantic Layer: Cocoindex Integration**

**Cocoindex** is chosen for the semantic layer because it is designed specifically as an *incremental* indexing framework, rather than just a passive vector database.2 It understands the concept of a "flow" of data.

### **5.1 Architecture: Library vs. Service Pattern**

Cocoindex usually operates as a standalone service with its own internal orchestration (cocoindex server). However, running "an orchestrator within an orchestrator" (Cocoindex inside Dagster) creates operational complexity and split-brain issues regarding state management.  
**Design Decision:** We will utilize Cocoindex primarily as a **Functional Library** within the Dagster assets.21 We will leverage its powerful Python API—specifically its text splitting and embedding functions—while letting Dagster manage the state and execution schedule.

### **5.2 Semantic Chunking with SplitRecursively**

Standard text splitters (like those in LangChain) often split by character count, which is disastrous for mathematics. Splitting a LaTeX equation $$\\int\_{a}^{b} f(x) dx$$ in the middle renders both halves semantically meaningless.  
Cocoindex provides a superior solution: SplitRecursively. This function leverages **Tree-sitter**, a parser generator that builds a concrete syntax tree for the source text.23

* We configure SplitRecursively with language="markdown".  
* Since the **Marker** extraction step produces valid Markdown, Tree-sitter understands the structure. It recognizes that a Code Block (which Marker uses for LaTeX blocks like $$...$$) is an atomic unit.  
* It respects header boundaries (\# Question 1), ensuring that a question and its sub-parts tend to stay together.

This "syntax-aware" chunking is vital for the mathematical context. It ensures that the vector embeddings generated in the next step represent coherent mathematical thoughts, not arbitrary text fragments.

### **5.3 Hybrid Embedding Strategy**

Embedding mathematical content is challenging because standard NLP models (like all-MiniLM-L6-v2) treat LaTeX variables ($x$, $y$, $\\alpha$) as generic tokens. They often fail to capture the *structural* similarity between $a^2+b^2=c^2$ and $x^2+y^2=z^2$.  
To mitigate this, the pipeline employs a **Hybrid Embedding Strategy** within the Cocoindex transform flow:

1. **Textual Embedding:** We use SentenceTransformerEmbed for the natural language component of the question (e.g., "Calculate the derivative of...").  
2. **Symbolic Preservation:** We do not rely solely on the vector for the math. The raw LaTeX string is preserved as metadata fields in the Cocoindex collect step.  
3. **Transform Flow:** We define a reusable @cocoindex.transform\_flow function that encapsulates this logic. This same function is used during *indexing* (to build the vector store) and during *querying* (to embed the user's search query), ensuring strictly consistent embedding geometry.22

Python

\# Reusable Transform Flow for Indexing AND Querying   
@cocoindex.transform\_flow()  
def text\_to\_embedding(text: cocoindex.DataSlice\[str\]):  
    return text.transform(  
        cocoindex.functions.SentenceTransformerEmbed(  
            model="sentence-transformers/all-MiniLM-L6-v2"  
        )  
    )

### **5.4 Incremental Updates via Custom Sources**

While we use Dagster for coarse-grained orchestration, Cocoindex shines at fine-grained incrementalism. If we process a "Marking Scheme" file that is 50 pages long, and only page 4 is corrected in a new version, we do not want to re-embed the other 49 pages.  
We can bridge this by using Cocoindex's **Postgres Source** or a **Custom Source**.25 The Dagster asset can write the extracted Markdown into a Postgres table (the "Staging Area"). Cocoindex is then configured to read from this table. Because Cocoindex tracks the modified\_time or offsets of the source data, it will automatically detect that only the row corresponding to "Page 4" has changed, and it will only re-compute the embeddings for that specific row. This hybrid approach uses Dagster for the macro-pipeline and Cocoindex for the micro-optimization of embedding costs.

## ---

**6\. The Knowledge Layer: Temporal Graph Construction with Graphiti**

While Cocoindex allows us to find *similar* questions, **Graphiti** allows us to find *related* concepts. It builds a structured Knowledge Graph (KG) that allows for reasoning. Graphiti is explicitly designed for **Temporal Knowledge Graphs**, making it the perfect tool for tracking how educational standards evolve over time.4

### **6.1 The "Episode" Abstraction**

Graphiti ingests data as "Episodes"—discrete events or documents.26 In our architecture, we map these to our domain artifacts:

* **Episode:** A single Exam Paper or Marking Scheme.  
* **Time:** The reference\_time of the episode is set to the exam date (e.g., "2023-06-15").

This temporal tagging is the key differentiator. It allows the system to construct a "Bi-Temporal" graph 27:

1. **Transaction Time:** When the data was added to the database.  
2. **Valid Time:** The real-world time the fact was true (the exam date).

This enables **Time Travel Queries**: *"Retrieve the definition of 'Matrix Multiplication' as it appeared in the 2015 curriculum versus the 2024 curriculum."* The graph will likely contain distinct nodes or evolving edges linked to these specific time windows, allowing the AI agent to discern the shift in pedagogical standards.

### **6.2 Ontology Engineering with Pydantic**

To prevent the graph from becoming a "soup" of disconnected nodes, we must enforce a schema. Graphiti allows the definition of custom entity and edge types using standard Python **Pydantic** models.13 This is how we inject domain expertise into the pipeline.  
We define a rigorous ontology for the mathematical domain:

Python

from pydantic import BaseModel, Field

\# Define Custom Entities  
class MathTheorem(BaseModel):  
    name: str \= Field(description="Name of the theorem, e.g., Pythagoras Theorem")  
    latex\_def: str \= Field(description="The mathematical definition in LaTeX")

class ExamTopic(BaseModel):  
    name: str \= Field(description="The curriculum topic, e.g., Differentiation")  
    code: str \= Field(description="The curriculum code, e.g., C1.2")

\# Define Graphiti Ingestion Logic  
await graphiti.add\_episode(  
    name="Math\_Paper\_2023",  
    episode\_body=extracted\_text,   
    source=EpisodeType.text,  
    reference\_time=exam\_date,  
    \# Enforce the ontology   
    entity\_types=   
)

By passing these models to add\_episode, we constrain the underlying LLM (which Graphiti uses for extraction) to look specifically for Theorems and Topics, ensuring high-quality, structured nodes.

### **6.3 Entity Resolution and Ground Truth**

A critical challenge is connecting the "Question" in the exam paper to the "Answer" in the marking scheme. These are separate documents.

* **Strategy:** We treat them as separate episodes.  
* **Resolution:** Graphiti employs LLM-based **Entity Resolution**.29 When it processes "Question 1" in the Marking Scheme, the LLM analyzes the context and recognizes it refers to the same entity as "Question 1" in the previously ingested Exam Paper episode. It merges or links these nodes.  
* **Enhancement:** We can explicitly model an edge type HAS\_ANSWER in our ontology. If the entity resolution is imperfect, we can implement a "Functional Core" post-processing step in Dagster that queries the graph for orphaned "Answers" and heuristically links them to "Questions" based on their ID (e.g., "Q1(a)") and temporal proximity.

### **6.4 Hybrid Retrieval Strategy**

For the end-user application (e.g., a "Math Tutor" bot), we utilize Graphiti's **Hybrid Search**.30 This combines:

1. **Semantic Search:** Using vectors to find conceptually similar nodes.  
2. **Keyword Search (BM25):** Essential for finding specific terms like "Eigenvector".  
3. **Graph Traversal:** Moving from a "Topic" node to all linked "Question" nodes.

This triangulation allows for complex queries: *"Find all geometry questions (Semantic) that involve 'circles' (Keyword) and appeared in exams linked to the '2023 Syllabus' (Graph Traversal)."*

## ---

**7\. Integrated Pipeline Data Flow**

The following describes the end-to-end flow of a single exam paper through the system.

### **7.1 Ingestion Phase**

1. **Sensor Activation:** The new\_exam\_sensor detects Math\_Exam\_2024.pdf in the monitored directory.  
2. **Partition Creation:** The sensor calls context.instance.add\_dynamic\_partitions to register "Math\_Exam\_2024".  
3. **Run Launch:** A RunRequest triggers the asset pipeline for this partition.

### **7.2 Extraction Phase (Asset: extracted\_markdown)**

1. **Execution:** The asset function executes. It retrieves the file path.  
2. **Functional Core Call:** It calls the extract\_math\_content(path) function.  
3. **Marker Pipeline:** Inside this function, the **Marker** library processes the PDF. It detects the layout, OCRs the text, converts formulas to LaTeX, and cleans headers/footers.  
4. **Materialization:** The resulting Markdown string is saved to object storage (S3/MinIO), and metadata (e.g., "LaTeX Equation Count: 45") is returned to Dagster.

### **7.3 Indexing Phase (Asset: vector\_index)**

1. **Input:** Reads the extracted\_markdown.  
2. **Chunking:** Calls Cocoindex.SplitRecursively to break the text into syntax-aware chunks.  
3. **Embedding:** Calls the text\_to\_embedding transform flow to generate vectors.  
4. **Storage:** Writes vectors and metadata to the Postgres vector store (via Cocoindex collector.export).

### **7.4 Knowledge Graph Phase (Asset: graph\_nodes)**

1. **Input:** Reads extracted\_markdown and metadata (Date).  
2. **Ontology Loading:** Loads the MathTheorem and ExamTopic Pydantic models.  
3. **Graphiti Ingestion:** Calls graphiti.add\_episode. The LLM extracts entities conforming to the Pydantic models.  
4. **Temporal Tagging:** The reference\_time is set to the exam date, placing the knowledge in the correct historical context.  
5. **Persistence:** Data is committed to the underlying graph database (Neo4j or FalkorDB).

## ---

**8\. Operational Strategy and Scalability**

### **8.1 Dockerized Deployment**

To ensure reproducibility and modularity, the entire stack should be deployed via Docker Compose or Kubernetes.

* **Service 1: Dagster Daemon & Webserver.**  
* **Service 2: Postgres** (Shared storage for Dagster metadata and Cocoindex vector store).  
* **Service 3: Neo4j / FalkorDB** (Storage for Graphiti).  
* **Service 4: GPU Worker.** The extraction (Marker) and embedding (Cocoindex) steps are compute-intensive. Dagster allows defining **Op Executors**. We can tag the extraction assets to run specifically on a worker node equipped with a GPU (e.g., NVIDIA A10G) to speed up the deep learning inference, while the sensor and graph logic run on a lightweight CPU node.

### **8.2 Database Independence**

The requirement to avoid vendor lock-in is satisfied at the storage layer:

* **Graphiti:** Abstracted via the Graphiti class. Swapping the backend from FalkorDB to Neo4j is a configuration change (changing the connection URI), not a code change.32  
* **Cocoindex:** Uses Postgres (with pgvector) as its default storage. Postgres is open-source, ubiquitous, and vendor-neutral.

### **8.3 Observability and Error Handling**

Dagster provides the operational "pane of glass."

* **Asset Checks:** We define data quality checks on the extracted\_markdown asset. Check: latex\_density \> 0.05. If a PDF yields no LaTeX, the check fails, alerting the engineer that the extraction likely failed (perhaps the PDF was a scanned image with no OCR text layer).  
* **Retry Policies:** Mathematical PDFs can be weird. We configure RetryPolicy(max\_retries=3) on the extraction asset to handle transient memory issues or LLM timeouts during graph extraction.

## **9\. Conclusion**

This architecture represents a rigorous, enterprise-grade approach to a complex unstructured data problem. By combining **Dagster's** robust state management with **Cocoindex's** incremental vectorization and **Graphiti's** temporal reasoning, we create a system that does more than just "read" exam papers—it "understands" them in the context of time and curriculum.  
The use of **Marker** ensures that the unique language of mathematics (LaTeX) is preserved, avoiding the semantic degradation common in generic OCR pipelines. The modular design, anchored by the "Functional Core" pattern and open standards (Markdown, JSON, Pydantic, Postgres), ensures that the organization owns its intelligence, free from proprietary API shackles. This is not just a search engine; it is a longitudinal analytical engine for educational data.

## **10\. Future Outlook & Recommendations**

* **Multimodal Expansion:** As models like GPT-4V improve, the pipeline can be upgraded to ingest diagrams and geometry figures. Cocoindex's support for multi-modal embeddings (e.g., ColPali) positions the architecture to handle this future requirement.33  
* **Feedback Loops:** Implementing a "Human-in-the-loop" asset in Dagster, where low-confidence extractions are flagged for manual review, would further enhance the integrity of the knowledge graph.  
* **Immediate Action:** Begin by implementing the "Extraction Asset" using Marker and verifying the LaTeX fidelity on a sample of 50 past papers. This is the foundational data asset upon which all downstream intelligence depends.

#### **Works cited**

1. Partitions in Data Pipelines \- Dagster, accessed December 4, 2025, [https://dagster.io/blog/partitioned-data-pipelines](https://dagster.io/blog/partitioned-data-pipelines)  
2. cocoindex \- PyPI, accessed December 4, 2025, [https://pypi.org/project/cocoindex/](https://pypi.org/project/cocoindex/)  
3. Overview | CocoIndex, accessed December 4, 2025, [https://cocoindex.io/docs/](https://cocoindex.io/docs/)  
4. Graphiti \- FalkorDB Docs, accessed December 4, 2025, [https://docs.falkordb.com/agentic-memory/graphiti.html](https://docs.falkordb.com/agentic-memory/graphiti.html)  
5. Graphiti: Knowledge Graph Memory for an Agentic World \- Neo4j, accessed December 4, 2025, [https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/](https://neo4j.com/blog/developer/graphiti-knowledge-graph-memory/)  
6. Functional core \- Imperative shell: dealing with logic dependant on conditional & expensive IO operations, accessed December 4, 2025, [https://stackoverflow.com/questions/79792099/functional-core-imperative-shell-dealing-with-logic-dependant-on-conditional](https://stackoverflow.com/questions/79792099/functional-core-imperative-shell-dealing-with-logic-dependant-on-conditional)  
7. Simplify Your Code: Functional Core, Imperative Shell \- Google Testing Blog, accessed December 4, 2025, [https://testing.googleblog.com/2025/10/simplify-your-code-functional-core.html](https://testing.googleblog.com/2025/10/simplify-your-code-functional-core.html)  
8. Simplify Your Code: Functional Core, Imperative Shell : r/programming \- Reddit, accessed December 4, 2025, [https://www.reddit.com/r/programming/comments/1od6z2h/simplify\_your\_code\_functional\_core\_imperative/](https://www.reddit.com/r/programming/comments/1od6z2h/simplify_your_code_functional_core_imperative/)  
9. Do you use the "Functional core, imperative shell" approach when writing code in all PLs?, accessed December 4, 2025, [https://www.reddit.com/r/ExperiencedDevs/comments/1hwtpj1/do\_you\_use\_the\_functional\_core\_imperative\_shell/](https://www.reddit.com/r/ExperiencedDevs/comments/1hwtpj1/do_you_use_the_functional_core_imperative_shell/)  
10. Partitioning assets | Dagster Docs, accessed December 4, 2025, [https://docs.dagster.io/guides/build/partitions-and-backfills/partitioning-assets](https://docs.dagster.io/guides/build/partitions-and-backfills/partitioning-assets)  
11. Marker — a new PDF converter suitable for RAG : r/llm\_updated \- Reddit, accessed December 4, 2025, [https://www.reddit.com/r/llm\_updated/comments/19dtd7z/marker\_a\_new\_pdf\_converter\_suitable\_for\_rag/](https://www.reddit.com/r/llm_updated/comments/19dtd7z/marker_a_new_pdf_converter_suitable_for_rag/)  
12. marker-pdf 0.3.2 \- PyPI, accessed December 4, 2025, [https://pypi.org/project/marker-pdf/0.3.2/](https://pypi.org/project/marker-pdf/0.3.2/)  
13. Custom Entity and Edge Types | Zep Documentation, accessed December 4, 2025, [https://help.getzep.com/graphiti/core-concepts/custom-entity-and-edge-types](https://help.getzep.com/graphiti/core-concepts/custom-entity-and-edge-types)  
14. I Tested 7 Python PDF Extractors So You Don't Have To (2025 Edition) \- Aman Kumar, accessed December 4, 2025, [https://onlyoneaman.medium.com/i-tested-7-python-pdf-extractors-so-you-dont-have-to-2025-edition-c88013922257](https://onlyoneaman.medium.com/i-tested-7-python-pdf-extractors-so-you-dont-have-to-2025-edition-c88013922257)  
15. A Comparative Study of PDF Parsing Tools Across Diverse Document Categories \- arXiv, accessed December 4, 2025, [https://arxiv.org/html/2410.09871v1](https://arxiv.org/html/2410.09871v1)  
16. Mathpix: Document conversion done right, accessed December 4, 2025, [https://mathpix.com/](https://mathpix.com/)  
17. Introducing Dynamic Definitions for Flexible Asset Partitioning \- Dagster, accessed December 4, 2025, [https://dagster.io/blog/dynamic-partitioning](https://dagster.io/blog/dynamic-partitioning)  
18. Launching a single RunRequest for a set of partitions from a schedule or sensor · dagster-io dagster · Discussion \#19457 \- GitHub, accessed December 4, 2025, [https://github.com/dagster-io/dagster/discussions/19457](https://github.com/dagster-io/dagster/discussions/19457)  
19. Sensors \- Dagster Docs, accessed December 4, 2025, [https://docs.dagster.io/guides/automate/sensors](https://docs.dagster.io/guides/automate/sensors)  
20. Get a DynamicPartitionsDefinition updated within a schedule definition before the RunRequest are fired \#20508 \- GitHub, accessed December 4, 2025, [https://github.com/dagster-io/dagster/discussions/20508](https://github.com/dagster-io/dagster/discussions/20508)  
21. Custom Functions | CocoIndex, accessed December 4, 2025, [https://cocoindex.io/docs/custom\_ops/custom\_functions](https://cocoindex.io/docs/custom_ops/custom_functions)  
22. CocoIndex Query Support, accessed December 4, 2025, [https://cocoindex.io/docs/query](https://cocoindex.io/docs/query)  
23. Functions | CocoIndex, accessed December 4, 2025, [https://cocoindex.io/docs/ops/functions](https://cocoindex.io/docs/ops/functions)  
24. Building Intelligent Codebase Indexing with CocoIndex: A Deep Dive into Semantic Code Search \- Medium, accessed December 4, 2025, [https://medium.com/@cocoindex.io/building-intelligent-codebase-indexing-with-cocoindex-a-deep-dive-into-semantic-code-search-e93ae28519c5](https://medium.com/@cocoindex.io/building-intelligent-codebase-indexing-with-cocoindex-a-deep-dive-into-semantic-code-search-e93ae28519c5)  
25. Incrementally Transform Structured \+ Unstructured Data from Postgres with AI \- CocoIndex, accessed December 4, 2025, [https://cocoindex.io/blogs/postgres-source](https://cocoindex.io/blogs/postgres-source)  
26. Adding Episodes \- Zep Documentation, accessed December 4, 2025, [https://help.getzep.com/graphiti/core-concepts/adding-episodes](https://help.getzep.com/graphiti/core-concepts/adding-episodes)  
27. Beyond Static Graphs: Engineering Evolving Relationships \- Zep, accessed December 4, 2025, [https://blog.getzep.com/beyond-static-knowledge-graphs/](https://blog.getzep.com/beyond-static-knowledge-graphs/)  
28. Graphiti (Knowledge Graph Agent Memory) Gets Custom Entity Types : r/LLMDevs \- Reddit, accessed December 4, 2025, [https://www.reddit.com/r/LLMDevs/comments/1j0ca03/graphiti\_knowledge\_graph\_agent\_memory\_gets\_custom/](https://www.reddit.com/r/LLMDevs/comments/1j0ca03/graphiti_knowledge_graph_agent_memory_gets_custom/)  
29. Building AI Agents with Knowledge Graph Memory: A Comprehensive Guide to Graphiti | by Saeed Hajebi | Medium, accessed December 4, 2025, [https://medium.com/@saeedhajebi/building-ai-agents-with-knowledge-graph-memory-a-comprehensive-guide-to-graphiti-3b77e6084dec](https://medium.com/@saeedhajebi/building-ai-agents-with-knowledge-graph-memory-a-comprehensive-guide-to-graphiti-3b77e6084dec)  
30. Searching the Graph \- Zep Documentation, accessed December 4, 2025, [https://help.getzep.com/v2/searching-the-graph](https://help.getzep.com/v2/searching-the-graph)  
31. Searching the Graph \- Zep Documentation, accessed December 4, 2025, [https://help.getzep.com/graphiti/working-with-data/searching](https://help.getzep.com/graphiti/working-with-data/searching)  
32. Graphiti MCP Server \- LobeHub, accessed December 4, 2025, [https://lobehub.com/mcp/getzep-graphiti-fastmcp](https://lobehub.com/mcp/getzep-graphiti-fastmcp)  
33. Featured Examples \- CocoIndex, accessed December 4, 2025, [https://cocoindex.io/docs/examples](https://cocoindex.io/docs/examples)