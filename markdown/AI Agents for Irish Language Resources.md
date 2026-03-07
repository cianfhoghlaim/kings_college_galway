# **Architecting the Neuro-Symbolic Gaeilge Engine: A Technical Blueprint for Agentic Knowledge Extraction and Preservation**

## **1\. The Imperative of Cognitive Preservation in Archival Science**

The preservation of cultural heritage has historically been a discipline of physical conservation—arresting the decay of vellum, paper, and ink. However, the digital era necessitates a transition from static preservation to dynamic, cognitive activation. This is particularly acute for low-resource languages like Irish (*Gaeilge*) and specialized domains such as historical educational materials. The challenge lies not merely in digitizing the artifact but in decoding its semantic structure, understanding its cultural context, and making it computationally accessible to modern reasoning engines.  
We present a comprehensive architectural blueprint for a sovereign AI system designed to ingest, structure, and query complex Irish language artifacts—specifically Leaving Certificate Mathematics papers and historical handwriting collections. This system transcends traditional Optical Character Recognition (OCR) and Retrieval-Augmented Generation (RAG) by implementing a **neuro-symbolic pipeline**. This architecture fuses the fluid, probabilistic reasoning of Large Multimodal Models (LMMs)—specifically **Z.ai's GLM-4.6 and GLM-4.6v**—with the rigorous, deterministic structure of Knowledge Graphs and strictly typed ontologies.  
By orchestrating **Agno** (formerly Phidata) as the high-performance agentic control plane, utilizing **Cocoindex** for incremental dataflow, and leveraging **Cognee** to construct a semantic graph within **PostgreSQL** and **LanceDB**, we establish a "Cognitive Federation." This system is governed by **BAML** (Boundary AI Markup Language) to ensure rigorous schema compliance and monitored via **Langfuse** and **Ragas** to maintain high fidelity to both the linguistic nuances of *Cló Gaelach* and the mathematical rigor of the State Examinations Commission.

## ---

**2\. Computational Linguistics of the *Cló Gaelach* and Educational Taxonomies**

Before dissecting the software architecture, it is essential to characterize the unique data modality of the target artifacts. The system must process two distinct but related streams: historical handwritten manuscripts (likely from the National Folklore Collection or *Dúchas*) and standardized educational texts (Leaving Certificate papers).

### **2.1 The Visual Complexity of Irish Handwriting**

The handwriting samples provided (Image 1 and Image 2\) represent a significant challenge for standard OCR engines. The script exhibits features of the *Cló Gaelach* (Gaelic type), characterized by unique orthographic conventions such as the *punctum delens* (a dot above a letter to indicate lenition, e.g., ḃ, ċ, ḋ) which corresponds to the modern 'h' suffix (bh, ch, dh).  
In Image 1, we observe a manuscript likely containing folklore or poetry ("Aimsir bhreagh"). The visual density, line variance, and ink bleed-through require a model capable of "Contextualized Late Interaction" rather than simple pixel-to-character mapping.1 Traditional OCR often misinterprets the *punctum delens* as noise or a tittle over an 'i'. A Vision-Language Model (VLM) like **GLM-4.6v**, however, processes the image as a semantic whole. It does not just "read" letters; it "perceives" the intent. When it sees a dot over a 'b' in the context of Irish syntax, it probabilistically infers the lenition, preserving the philological integrity of the text.  
Image 2 demonstrates the necessity of **Grounding** and **Object Detection**. The red bounding boxes indicate a pre-processing step where text regions are segmented. Our architecture must automate this segmentation. GLM-4.6v's native grounding capabilities allow it to return bounding box coordinates alongside the textual transcription.2 This enables the system to maintain a spatial index of the manuscript, allowing users to query not just *what* was written, but *where*—a critical feature for paleographers analyzing scribal patterns.

### **2.2 The Ontology of the Leaving Certificate**

The second data stream—Leaving Certificate Mathematics papers—presents a structural rather than orthographic challenge. As seen in the analysis of LC003GLP200IV.pdf 1, these documents obey a strict hierarchical ontology:

* **Paper Level:** Year (2025), Level (Ordinary/Higher), Time limits.  
* **Section Level:** *Roinn A* (Concepts and Skills) vs. *Roinn B* (Contexts and Applications).  
* **Question Level:** Nested sub-parts (a)(i), (a)(ii), mark allocations (e.g., 30 marc), and dependencies (answers to part (i) often feed into part (ii)).  
* **Domain Level:** Mathematical notation (LaTeX), geometry diagrams, and bilingual instructions.

A standard RAG pipeline flattening this PDF into text chunks would destroy this structural integrity. Knowing that "Question 5" is in "Section A" and worth "30 marks" is semantic metadata that must be preserved. Furthermore, the *language* of the paper is Irish (*Gnáthleibhéal*, *Matamaitic*). The system must handle bilingual tokenization, ensuring that mathematical terms like "diall caighdeánach" (standard deviation) are mapped to their mathematical concepts in the Knowledge Graph, enabling cross-lingual querying.1  
The architecture must therefore support **multi-modal ingestion** (vision for handwriting/diagrams, text for exam questions) and **multi-structured output** (graph nodes for questions, vector embeddings for semantic search).

## ---

**3\. The Orchestration Layer: Agno and the Agentic Federation**

The central nervous system of our architecture is **Agno** (formerly Phidata). While frameworks like LangChain or LangGraph offer broad capabilities, Agno is selected for its minimalist, "pure Python" design philosophy and extreme performance characteristics—instantiating agents in approximately 3 microseconds, which is orders of magnitude faster than graph-based alternatives.4 This low overhead is decisive when spawning dynamic teams of agents to process multi-page documents in parallel.

### **3.1 The Agent Team Topology**

We implement a **Hierarchical Team Structure** within Agno to segregate duties. This prevents context pollution (where the math reasoning agent gets confused by handwriting instructions) and allows for specialized prompting.  
The team consists of three primary agent personas:

1. **The Chief Examiner (Orchestrator):** The entry point for all queries. It decomposes user requests ("Find all calculus questions from 2025 and transcribe this student's answer") into sub-tasks. It utilizes Agno's AgentTeam class with a delegate capability.6  
2. **The Palaeographer (Vision Specialist):** Equipped with the **Z.ai GLM-4.6v** model via a custom MCP client. Its sole responsibility is the visual interpretation of handwriting and exam diagrams. It is instructed to output strictly formatted transcriptions, preserving archaic orthography.  
3. **The Ontologist (Structure Specialist):** Uses **BAML** tools to parse raw text into strictly typed JSON objects adhering to the Leaving Certificate schema. It does not "think" creatively; it strictly enforces structure.

### **3.2 Custom Model Integration: Z.ai GLM-4.6**

Agno provides native support for OpenAI and Anthropic, but integration with **Z.ai's GLM-4.6** requires extending the OpenAILike model class. Z.ai's API structure is OpenAI-compatible, but the endpoints and specific parameters for the vision model (GLM-4.6v) require precise configuration.7  
We define a custom configuration to bridge Agno with the Z.ai inference engine. This setup ensures that we can leverage GLM-4.6's "Thinking Mode" for complex reasoning tasks (like solving the math problems to verify the marking scheme) while using the standard mode for rapid extraction.2

Python

from agno.agent import Agent  
from agno.models.openai.like import OpenAILike  
import os

\# Configuration for the Text/Reasoning Engine (GLM-4.6)  
\# We use a low temperature to ensure deterministic extraction of exam data.  
zhipu\_text\_model \= OpenAILike(  
    id="glm-4.6",  
    api\_key=os.getenv("ZHIPU\_API\_KEY"),  
    base\_url="https://open.bigmodel.cn/api/paas/v4/",  \# Official Z.ai endpoint  
    max\_tokens=4096,  
    temperature=0.1,  
    default\_headers={"Authorization": f"Bearer {os.getenv('ZHIPU\_API\_KEY')}"}  
)

\# Configuration for the Vision Engine (GLM-4.6v)  
\# This model handles the visual perception of handwriting and diagrams.  
zhipu\_vision\_model \= OpenAILike(  
    id="glm-4.6v",   
    api\_key=os.getenv("ZHIPU\_API\_KEY"),  
    base\_url="https://open.bigmodel.cn/api/paas/v4/",  
)

\# The Orchestrator Agent Definition  
chief\_examiner \= Agent(  
    name="ChiefExaminer",  
    role="Orchestrator of the digitization pipeline",  
    model=zhipu\_text\_model,  
    instructions=,  
    markdown=True,  
    show\_tool\_calls=True,  
    monitoring=True  \# Enables Agno's built-in monitoring  
)

### **3.3 The Vision MCP Integration**

To robustly handle the vision capabilities, we do not rely solely on the model's native multimodal input (which can be flaky with complex multi-turn conversations). Instead, we wrap the vision capability in a **Model Context Protocol (MCP)** server. This aligns with Agno's first-class support for MCP, allowing the agent to "use" vision as a tool.10  
We deploy a local MCP server (Node.js based, using @z\_ai/mcp-server) that exposes tools like analyze\_image and extract\_text\_from\_screenshot. The Agno agent connects to this server via stdio or SSE. This decoupling allows us to upgrade the vision logic (e.g., adding pre-processing filters to the images) without modifying the agent's core logic.

Python

from agno.tools.mcp import MCPTools

\# Initialize connection to the Z.ai Vision MCP Server  
\# We assume the server is installed via npm as per Z.ai docs \[11\]  
vision\_mcp\_tools \= MCPTools(  
    command="npx",   
    args=\["-y", "@z\_ai/mcp-server"\],  
    env={  
        "Z\_AI\_API\_KEY": os.getenv("ZHIPU\_API\_KEY"),  
        "Z\_AI\_MODE": "ZAI" \# Explicitly setting the platform mode  
    }  
)

\# The Vision Specialist Agent  
vision\_specialist \= Agent(  
    name="Palaeographer",  
    role="Expert in Irish handwriting and mathematical diagrams",  
    model=zhipu\_text\_model, \# The brain controlling the tools  
    tools=\[vision\_mcp\_tools\],   
    instructions=  
)

This configuration establishes the compute layer. The agents are stateless entities that process information. To manage the *flow* of this information from the source PDFs to the database, we turn to **Cocoindex**.

## ---

**4\. The Immutable Dataflow: Cocoindex ETL Pipeline**

In many RAG systems, data ingestion is a fragile, "fire-and-forget" script. If a parser fails or a PDF is updated, the entire corpus must often be re-indexed. For our system, which handles high-value archival documents, this is unacceptable. We utilize **Cocoindex** to establish a robust, incremental ETL (Extract, Transform, Load) pipeline.

### **4.1 Incremental State Management via Postgres**

Cocoindex operates on the principle of **Dataflow Programming**.12 It treats data transformations as a directed acyclic graph (DAG) where each step is memoized. Crucially, it uses **PostgreSQL** not just as a destination, but as an internal state store to track data lineage.  
When a new exam paper LC003GLP200IV.pdf is dropped into the source directory, Cocoindex calculates its content hash. If the file hasn't changed, the pipeline skips it. If only "Page 5" of the PDF is annotated, Cocoindex detects the delta and triggers the transformation logic *only* for that page. This is vital for cost control when using high-end models like GLM-4.6v.

### **4.2 The Pipeline Definition**

We define a Cocoindex Flow that orchestrates the movement of data. This flow integrates the Agno agents as custom transformation functions.

Python

import cocoindex  
from cocoindex.functions import SplitRecursively, SentenceTransformerEmbed  
from cocoindex.sources import LocalFile  
from cocoindex.targets import Postgres

@cocoindex.flow\_def(name="IrishMathsIngestion")  
def exam\_pipeline(flow\_builder: cocoindex.FlowBuilder, data\_scope: cocoindex.DataScope):  
    \# 1\. Source: Watch the repository of Exam PDFs  
    data\_scope\["documents"\] \= flow\_builder.add\_source(  
        LocalFile(path="./corpus/exams/2025", binary=True)  
    )  
      
    \# 2\. Transform: PDF Parsing & Image Conversion  
    \# We define a custom function that uses PyMuPDF to render pages as images  
    \# for the Vision Agent and extracts text for the Ontologist.  
    with data\_scope\["documents"\].row() as doc:  
        doc\["pages"\] \= doc\["content"\].transform(  
            cocoindex.functions.Custom(convert\_pdf\_to\_page\_objects)  
        )

    \# 3\. Transform: Agentic Extraction (The "Cognitive Step")  
    \# This is where we invoke the Agno Vision Specialist.  
    \# Note: This runs ONLY if the page image has changed.  
    with doc\["pages"\].row() as page:  
        page\["semantic\_data"\] \= page\["image"\].transform(  
            cocoindex.functions.Custom(invoke\_agno\_vision\_agent)  
        )  
          
        \# 4\. Transform: Vector Embedding  
        \# We generate embeddings for the text content to enable semantic search.  
        page\["embedding"\] \= page\["text\_content"\].transform(  
            cocoindex.functions.SentenceTransformerEmbed(  
                model="sentence-transformers/all-MiniLM-L6-v2"  
            )  
        )

### **4.3 The Persistence Layer**

The pipeline terminates by exporting the enriched data to **PostgreSQL**. Cocoindex creates tables that map to our schema, handling the pgvector indexing automatically.14 This table (exam\_pages\_indexed) becomes the raw material for Cognee's Knowledge Graph.

Python

    \# 5\. Export: Structured Storage  
    \# We export the structured JSON (from BAML) and the Vectors to Postgres.  
    data\_scope.export(  
        "exam\_pages\_indexed",  
        Postgres(),  
        primary\_key\_fields=\["exam\_id", "page\_number"\],  
        vector\_indexes=  
    )

This pipeline ensures that our data lake (Postgres) is always in sync with our source files (PDFs), providing a pristine foundation for the next layer: the Knowledge Graph.

## ---

**5\. The Guardrails of Logic: BAML Ontology & Prompt Optimization**

Large Language Models are non-deterministic engines. When asked to "extract the exam questions," they might return JSON one time, Markdown the next, or translate Irish terms into English arbitrarily. To build a reliable database, we require **Strict Structured Output**. We utilize **BAML** (Boundary AI Markup Language) to enforce a rigorous schema on the output of our Agno agents.

### **5.1 Defining the Exam Ontology**

We create a .baml file that strictly defines the structure of an Irish Leaving Certificate Mathematics paper. This is not just a type hint; BAML compiles this into a parser that validates the LLM's raw token stream, performing "Schema-Aligned Parsing" (SAP) to correct errors on the fly.16  
The ontology reflects the hierarchical structure observed in the research snippets 1:

Code snippet

// exam\_structure.baml

enum ExamLevel {  
    Higher  
    Ordinary  
    Foundation  
}

enum SectionName {  
    Roinn\_A @description("Coincheapa agus Scileanna / Concepts and Skills")  
    Roinn\_B @description("Comhthéacsanna agus Feidhmeanna / Contexts and Applications")  
}

class QuestionPart {  
    label string @description("The part label, e.g., (a), (b)(i)")  
    text\_content string @description("The question text in Irish")  
    marks int? @description("Marks allocated to this specific part")  
}

class Question {  
    number int  
    topic string? @description("Inferred mathematical topic, e.g., 'Statistics', 'Algebra'")  
    parts QuestionPart  
    total\_marks int  
}

class ExamSection {  
    name SectionName  
    instructions string  
    questions Question  
}

class ExamPaper {  
    year int  
    subject string  
    level ExamLevel  
    sections ExamSection  
}

// The Extraction Function  
function ExtractExamData(exam\_text: string) \-\> ExamPaper {  
    client "zhipu/glm-4.6"  
    prompt \#"  
        Analyze the following text from a Leaving Certificate Math paper.  
        Extract the structured data strictly adhering to the schema.  
          
        CRITICAL RULES:  
        1\. Preserve all Irish text exactly (e.g., 'Gnáthleibhéal').  
        2\. Do not translate question content.  
        3\. Identify the section (Roinn A vs Roinn B) based on the headers.  
          
        Text:  
        {{ exam\_text }}  
          
        {{ ctx.output\_format }}  
    "\#  
}

### **5.2 Prompt Optimization with BoundaryML**

One of the key advantages of BAML is its tooling for prompt engineering. Using the **BoundaryML** VS Code extension, we can run "Prompt Optimization" experiments.18 We feed the system examples of difficult extractions (e.g., a complex calculus problem with mixed text and LaTeX) and BAML optimizes the instructions to minimize hallucinations.  
We specifically optimize for the preservation of the **Irish dialect**. We add assertions in BAML to fail the extraction if, for example, "Roinn A" is returned as "Section A."

Code snippet

test IrishPreservation {  
  functions  
  args {  
    exam\_text "Roinn A Coincheapa agus Scileanna 150 marc"  
  }  
  assert {  
    {{ this.sections.name \== "Roinn\_A" }}  
  }  
}

This BAML client is then generated as Python code and imported directly into our Agno agents as a tool, guaranteeing that the data flowing into our system is type-safe and valid.

## ---

**6\. Semantic Memory & Graph Construction: Cognee**

We now have structured data in Postgres (via Cocoindex/Agno/BAML). However, rows in a database do not capture *meaning*. A vector search for "Calculus" might miss a question that uses the term "Díorthú" (Derivation) if the embedding model is not perfectly bilingual. **Cognee** solves this by building a **Knowledge Graph** that explicitly links concepts.19

### **6.1 The GraphRAG Architecture**

We configure Cognee to use **PostgreSQL** as its relational backbone (linking it to the data Cocoindex ingested) and **LanceDB** as the vector store for graph nodes. The use of LanceDB is strategic here; its "Zero-Copy" architecture allows us to scale to millions of handwriting vectors without significant latency.1

### **6.2 The Cognify Pipeline**

The core operation in Cognee is cognify. This process takes the raw text chunks and uses an LLM (GLM-4.6) to extract **Entities** (e.g., "Pythagoras' Theorem", "Baile Átha an Rí") and **Relationships** (e.g., "Question 1" *tests\_knowledge\_of* "Statistics").  
We define a custom **Ontology** for Cognee to guide this extraction. Unlike BAML, which enforces the *document structure*, the Cognee ontology defines the *domain knowledge*.

Python

\# graph\_config.py  
import cognee  
import os

\# Configure Cognee to use our persistent stores  
os.environ \= "postgres"  
os.environ \= "cocoindex\_db" \# Connecting to the same DB as Cocoindex  
os.environ \= "lancedb"  
os.environ \= "./lancedb\_store"

async def generate\_knowledge\_graph():  
    \# 1\. Ingest Data: We pull the text we processed in the Cocoindex stage  
    \# In a production setup, we would write an adapter to read directly from the 'exam\_pages\_indexed' table.  
    \# For this outline, we simulate adding the content.  
      
    \# 2\. Cognify: This triggers the extraction of nodes and edges.  
    \# We pass a custom ontology file that defines the Irish Math Curriculum.  
    await cognee.cognify(  
        datasets=\["leaving\_cert\_2025"\],  
        ontology\_file\_path="ontologies/math\_curriculum.owl"   
    )

\# The Ontology (math\_curriculum.owl) would define:  
\# Class: Topic (e.g., Algebra, Geometry)  
\# Class: Question  
\# Property: assesses (Question \-\> Topic)  
\# Property: contains\_term (Question \-\> MathematicalTerm)

### **6.3 Graph-Enhanced Search**

Once "cognified," the data allows for powerful **GraphRAG** queries. An agent can answer: "How has the weight of Statistics questions changed since 2020?"

1. **Vector Search:** Finds questions semantically related to "Statistics".  
2. **Graph Traversal:** Cognee traverses the assesses edges to find all nodes linked to the Statistics topic node, ensuring high recall even if the word "Statistics" isn't explicitly mentioned in the question text (e.g., it only mentions "Standard Deviation").

This architecture provides the "Semantic Layer" that transforms isolated exam papers into a connected web of educational knowledge.

## ---

**7\. Observability and Evaluation: Langfuse and Ragas**

A complex multi-agent system requires rigorous monitoring. We employ **Langfuse** for operational observability (tracing execution paths) and **Ragas** for quality evaluation (measuring the accuracy of our preservation efforts).

### **7.1 Distributed Tracing with Langfuse**

We integrate Langfuse to trace the lifecycle of a request through the Agno agent team. This allows us to debug the "handoffs" between the Chief Examiner and the Vision Specialist.  
Configuration:  
Agno supports OpenTelemetry, which we pipe to Langfuse.21

Python

from agno.agent import Agent  
from langfuse.decorators import observe  
import os

\# Configure Langfuse  
os.environ \= "pk-lf..."  
os.environ \= "sk-lf..."

@observe(name="digitize\_exam\_paper")  
def run\_digitization\_task(pdf\_path):  
    \# This function wraps the agent execution  
    \# Langfuse will capture the input, the internal reasoning steps of GLM-4.6,  
    \# the tool calls to the Vision MCP, and the final BAML-structured output.  
    response \= chief\_examiner.run(f"Digitize the paper at {pdf\_path}")  
    return response

This tracing reveals bottlenecks. For instance, if the Vision Specialist takes 15 seconds to process a diagram, we will see that span in the Langfuse dashboard and can optimize the image resolution or prompt.

### **7.2 Quality Assurance with Ragas**

To ensure "Cultural Fidelity"—that we are accurately preserving the Irish language—we use **Ragas** to evaluate the pipeline.23 We define a "Golden Dataset" based on human-verified transcriptions of a subset of the folklore/exam papers.  
We define custom metrics relevant to our domain:

1. **Transcription Faithfulness:** Does the generated text match the ground truth Irish text (handling *síneadh fada* and lenition correctly)?  
2. **Ontological Accuracy:** Did BAML correctly classify "Question 6(a)" or did it hallucinate a "Question 6(c)"?

Python

from ragas import evaluate  
from ragas.metrics import faithfulness, context\_precision  
from datasets import Dataset

\# Define our Golden Dataset (derived from manual verification of snippets)  
eval\_data \= {  
    "question":,  
    "answer":,  
    "contexts": \[\["Page 4 content..."\], \["Image 1 visual tokens..."\]\],  
    "ground\_truth": \["129 mm", "Aimsir bhreagh..."\]  
}

dataset \= Dataset.from\_dict(eval\_data)

\# Run evaluation  
results \= evaluate(  
    dataset=dataset,  
    metrics=\[faithfulness, context\_precision\],  
    llm=zhipu\_text\_model \# We use GLM-4.6 as the judge to evaluate GLM-4.6v's work  
)

This feedback loop allows us to empirically measure the performance of our agents and refine the BAML schemas or Cognee ontologies accordingly.

## ---

**8\. Conclusion: A Sovereign Engine for Cultural Intelligence**

The architecture detailed in this report represents a sophisticated approach to the digital preservation of Irish cultural heritage. By moving beyond monolithic processing and embracing a **federated neuro-symbolic stack**, we achieve a system that is:

1. **Structurally Rigorous:** **BAML** ensures that the complex ontology of State Exams is respected, preventing the structural decay common in LLM outputs.  
2. **Semantically Deep:** **Cognee** and **LanceDB** transform static text into a living Knowledge Graph, enabling reasoning across time and topics.  
3. **Visually Acute:** The integration of **Z.ai GLM-4.6v** via **MCP** allows the system to perceive and index the nuance of *Cló Gaelach* handwriting and mathematical diagrams.  
4. **Operationally Efficient:** **Agno** and **Cocoindex** provide a lightweight, incremental runtime that minimizes compute costs while maximizing throughput.

This blueprint provides the technical foundation for a "National Digital Brain"—a system capable of not just archiving Ireland's educational and folklore history, but waking it up.

#### **Works cited**

1. Multimodal Irish Handwriting Generation Model.md  
2. GLM-4.6V \- Z.AI DEVELOPER DOCUMENT, accessed December 23, 2025, [https://docs.z.ai/guides/vlm/glm-4.6v](https://docs.z.ai/guides/vlm/glm-4.6v)  
3. zai-org/GLM-4.6V \- Hugging Face, accessed December 23, 2025, [https://huggingface.co/zai-org/GLM-4.6V](https://huggingface.co/zai-org/GLM-4.6V)  
4. agno-agi/agno: The multi-agent stack that runs in your cloud. Framework, runtime, and control plane. \- GitHub, accessed December 23, 2025, [https://github.com/agno-agi/agno](https://github.com/agno-agi/agno)  
5. Agno vs LangGraph: Best Framework to Build Multi-Agent Systems \- ZenML Blog, accessed December 23, 2025, [https://www.zenml.io/blog/agno-vs-langgraph](https://www.zenml.io/blog/agno-vs-langgraph)  
6. Team Examples \- Agno, accessed December 23, 2025, [https://docs.agno.com/examples/use-cases/teams/overview](https://docs.agno.com/examples/use-cases/teams/overview)  
7. Agno \- AI/ML API Documentation, accessed December 23, 2025, [https://docs.aimlapi.com/integrations/agno](https://docs.aimlapi.com/integrations/agno)  
8. OpenAI-compatible models \- Agno, accessed December 23, 2025, [https://docs.agno.com/integrations/models/openai-like](https://docs.agno.com/integrations/models/openai-like)  
9. GLM-4.5V \- Overview \- Z.AI DEVELOPER DOCUMENT, accessed December 23, 2025, [https://docs.z.ai/guides/vlm/glm-4.5v](https://docs.z.ai/guides/vlm/glm-4.5v)  
10. Model Context Protocol (MCP) \- Agno, accessed December 23, 2025, [https://docs.agno.com/basics/tools/mcp/overview](https://docs.agno.com/basics/tools/mcp/overview)  
11. cocoindex-io/cocoindex: Data transformation framework for AI. Ultra performant, with incremental processing. Star if you like it\! \- GitHub, accessed December 23, 2025, [https://github.com/cocoindex-io/cocoindex](https://github.com/cocoindex-io/cocoindex)  
12. Overview | CocoIndex, accessed December 23, 2025, [https://cocoindex.io/docs/](https://cocoindex.io/docs/)  
13. Transform Data From Structured Source in PostgreSQL \- CocoIndex, accessed December 23, 2025, [https://cocoindex.io/docs/examples/postgres\_source](https://cocoindex.io/docs/examples/postgres_source)  
14. PostgreSQL → PgVector with AI Embeddings: Build Production-Ready Semantic Search in 3 Steps : r/cocoindex \- Reddit, accessed December 23, 2025, [https://www.reddit.com/r/cocoindex/comments/1pf1gt3/postgresql\_pgvector\_with\_ai\_embeddings\_build/](https://www.reddit.com/r/cocoindex/comments/1pf1gt3/postgresql_pgvector_with_ai_embeddings_build/)  
15. Why BAML? | Boundary Documentation, accessed December 23, 2025, [https://docs.boundaryml.com/guide/introduction/why-baml](https://docs.boundaryml.com/guide/introduction/why-baml)  
16. The Prompting Language Every AI Engineer Should Know: A BAML Deep Dive, accessed December 23, 2025, [https://pub.towardsai.net/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive-6a4cd19a62db](https://pub.towardsai.net/the-prompting-language-every-ai-engineer-should-know-a-baml-deep-dive-6a4cd19a62db)  
17. Prompt Optimization (Beta) \- Boundary Documentation \- BAML, accessed December 23, 2025, [https://docs.boundaryml.com/guide/baml-advanced/prompt-optimization](https://docs.boundaryml.com/guide/baml-advanced/prompt-optimization)  
18. Introduction \- Cognee Documentation, accessed December 23, 2025, [https://docs.cognee.ai/getting-started/introduction](https://docs.cognee.ai/getting-started/introduction)  
19. Simplifying RAG for Developers: Cognee x LanceDB Integration, accessed December 23, 2025, [https://www.cognee.ai/blog/deep-dives/cognee-lancedb-simplifying-rag-for-developers](https://www.cognee.ai/blog/deep-dives/cognee-lancedb-simplifying-rag-for-developers)  
20. Observability for Agno with Langfuse, accessed December 23, 2025, [https://langfuse.com/integrations/frameworks/agno-agents](https://langfuse.com/integrations/frameworks/agno-agents)  
21. Langfuse \- Agno, accessed December 23, 2025, [https://docs.agno.com/integrations/observability/langfuse](https://docs.agno.com/integrations/observability/langfuse)  
22. Ragas \- Arize AX Docs, accessed December 23, 2025, [https://arize.com/docs/ax/integrations/evaluation-integrations/ragas](https://arize.com/docs/ax/integrations/evaluation-integrations/ragas)  
23. AG-UI Integration \- Ragas, accessed December 23, 2025, [https://docs.ragas.io/en/latest/howtos/integrations/\_ag\_ui/](https://docs.ragas.io/en/latest/howtos/integrations/_ag_ui/)